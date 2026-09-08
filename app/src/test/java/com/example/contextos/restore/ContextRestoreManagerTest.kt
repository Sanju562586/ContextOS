package com.example.contextos.restore

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import com.example.contextos.domain.repository.SnapshotRepository
import com.example.contextos.domain.usecase.GetSnapshotByIdUseCase
import com.example.contextos.domain.usecase.MarkSnapshotRestoredUseCase
import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextItemType
import com.example.contextos.models.ContextSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class ContextRestoreManagerTest {

    private lateinit var context: Context
    private lateinit var fakeRepository: FakeSnapshotRepository
    private lateinit var restoreManager: ContextRestoreManagerImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeRepository = FakeSnapshotRepository()
        restoreManager = ContextRestoreManagerImpl(
            context = context,
            getSnapshotByIdUseCase = GetSnapshotByIdUseCase(fakeRepository),
            markSnapshotRestoredUseCase = MarkSnapshotRestoredUseCase(fakeRepository)
        ).apply {
            defaultItemDelayMillis = 0L // Fast execution in unit tests
        }
    }

    private fun registerLauncherApp(packageName: String, appLabel: String) {
        val packageManager = context.packageManager
        val shadowPm = Shadows.shadowOf(packageManager)
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                name = "$packageName.MainActivity"
                applicationInfo = ApplicationInfo().apply {
                    this.packageName = packageName
                    flags = ApplicationInfo.FLAG_INSTALLED
                }
            }
        }
        shadowPm.addResolveInfoForIntent(intent, resolveInfo)
    }

    @Test
    fun restoreContext_restoresAllSupportedItemTypesSequentially() = runTest {
        registerLauncherApp("com.android.chrome", "Chrome")

        val snapshotId = "snap-restore-1"
        val appItem = ContextItem.createApp(
            snapshotId = snapshotId,
            packageName = "com.android.chrome",
            appName = "Chrome"
        )
        val urlItem = ContextItem.createUrl(
            snapshotId = snapshotId,
            url = "https://github.com/project-alpha/roadmap",
            title = "Research URL"
        )
        val docItem = ContextItem.createDocument(
            snapshotId = snapshotId,
            contentUri = "content://downloads/report.docx",
            displayName = "report.docx",
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
        val noteItem = ContextItem.createNote(
            snapshotId = snapshotId,
            content = "Task Notes: Deliver MVP"
        )
        val imageItem = ContextItem.createImage(
            snapshotId = snapshotId,
            imageUri = "file:///data/cache/whiteboard.jpg",
            ocrRawText = "Sprint Goals: P99 latency < 200ms"
        )

        val snapshot = ContextSnapshot(
            id = snapshotId,
            name = "Project Alpha",
            description = "Sprint planning context",
            items = listOf(appItem, urlItem, docItem, noteItem, imageItem)
        )
        fakeRepository.saveSnapshot(snapshot)

        val progress = restoreManager.restoreContext(snapshot)

        assertTrue(progress.isComplete)
        assertEquals(5, progress.totalItems)
        assertEquals(5, progress.completedCount)
        assertEquals(5, progress.successCount)
        assertEquals(0, progress.failureCount)
        assertFalse(progress.hasErrors)

        // Verify messages match user requested format
        val messages = progress.itemResults.map { it.message }
        assertTrue(messages.any { it.contains("Opened Chrome") })
        assertTrue(messages.any { it.contains("Opened Research URL") })
        assertTrue(messages.any { it.contains("Opened report.docx") })
        assertTrue(messages.any { it.contains("Loaded") && it.contains("notes", ignoreCase = true) })
        assertTrue(messages.any { it.contains("Loaded whiteboard image") })

        // Verify in-app display of notes and images
        assertEquals(1, progress.restoredNotes.size)
        assertEquals("Task Notes: Deliver MVP", progress.restoredNotes.first().content)
        assertEquals(1, progress.restoredImages.size)
        assertEquals("Sprint Goals: P99 latency < 200ms", progress.restoredImages.first().ocrRawText)

        // Verify repository lastResumedAt timestamp was updated
        val updated = fakeRepository.getSnapshotDirect(snapshotId)
        assertNotNull(updated?.lastResumedAt)
    }

    @Test
    fun restoreContext_robustErrorHandling_continuesWhenAppNotInstalled() = runTest {
        val snapshotId = "snap-error-app"
        val uninstalledApp = ContextItem.createApp(
            snapshotId = snapshotId,
            packageName = "com.figma.android.uninstalled",
            appName = "Figma"
        )
        val noteItem = ContextItem.createNote(
            snapshotId = snapshotId,
            content = "Design review task"
        )
        val urlItem = ContextItem.createUrl(
            snapshotId = snapshotId,
            url = "https://figma.com/file/123",
            title = "Figma Web"
        )

        val snapshot = ContextSnapshot(
            id = snapshotId,
            name = "Project Beta",
            items = listOf(uninstalledApp, noteItem, urlItem)
        )
        fakeRepository.saveSnapshot(snapshot)

        val progress = restoreManager.restoreContext(snapshot)

        assertTrue(progress.isComplete)
        assertEquals(3, progress.totalItems)
        assertEquals(3, progress.completedCount)
        assertEquals(2, progress.successCount)
        assertEquals(1, progress.failureCount)
        assertTrue(progress.hasErrors)

        // App failed with clear error, but did not crash
        val appResult = progress.itemResults.first { it.item.type == ContextItemType.APP }
        assertEquals(ItemRestoreStatus.FAILED, appResult.status)
        assertTrue(appResult.message.contains("is not installed"))

        // Other items succeeded
        val noteResult = progress.itemResults.first { it.item.type == ContextItemType.NOTE }
        assertEquals(ItemRestoreStatus.SUCCESS, noteResult.status)

        val urlResult = progress.itemResults.first { it.item.type == ContextItemType.URL }
        assertEquals(ItemRestoreStatus.SUCCESS, urlResult.status)
    }

    @Test
    fun restoreContext_robustErrorHandling_handlesInvalidDocumentUri() = runTest {
        val snapshotId = "snap-doc-error"
        val badDoc = ContextItem(
            snapshotId = snapshotId,
            type = ContextItemType.DOCUMENT,
            contentUri = "",
            fileUri = null,
            metadata = mapOf("displayName" to "missing.pdf")
        )
        val note = ContextItem.createNote(
            snapshotId = snapshotId,
            content = "Keep going"
        )

        val snapshot = ContextSnapshot(
            id = snapshotId,
            name = "Doc Test",
            items = listOf(badDoc, note)
        )

        val progress = restoreManager.restoreContext(snapshot)

        assertTrue(progress.isComplete)
        assertEquals(2, progress.totalItems)
        assertEquals(1, progress.successCount)
        assertEquals(1, progress.failureCount)

        val docResult = progress.itemResults.first { it.item.type == ContextItemType.DOCUMENT }
        assertEquals(ItemRestoreStatus.FAILED, docResult.status)
        assertTrue(docResult.message.contains("no stored URI"))
    }

    @Test
    fun restoreSnapshotByIdFlow_emitsLiveProgressTransitions() = runTest {
        registerLauncherApp("com.android.chrome", "Chrome")

        val snapshotId = "snap-flow-test"
        val snapshot = ContextSnapshot(
            id = snapshotId,
            name = "Project Alpha",
            items = listOf(
                ContextItem.createNote(snapshotId, "Note 1"),
                ContextItem.createUrl(snapshotId, "https://google.com", "Google")
            )
        )
        fakeRepository.saveSnapshot(snapshot)

        val emissions = restoreManager.restoreSnapshotByIdFlow(snapshotId).toList()
        assertTrue(emissions.size >= 3)

        // Initial emission
        assertFalse(emissions.first().isComplete)
        assertEquals(0, emissions.first().completedCount)

        // Final emission
        val last = emissions.last()
        assertTrue(last.isComplete)
        assertEquals(2, last.totalItems)
        assertEquals(2, last.completedCount)
        assertEquals(2, last.successCount)
    }

    @Test
    fun restoreSnapshotByIdFlow_nonExistentSnapshot_reportsFailure() = runTest {
        val emissions = restoreManager.restoreSnapshotByIdFlow("non-existent-id").toList()
        assertEquals(1, emissions.size)
        val result = emissions.first()
        assertTrue(result.isComplete)
        assertTrue(result.statusMessage.contains("not found"))
    }
}

private class FakeSnapshotRepository : SnapshotRepository {
    private val snapshots = MutableStateFlow<Map<String, ContextSnapshot>>(emptyMap())

    fun getSnapshotDirect(id: String): ContextSnapshot? = snapshots.value[id]

    override fun getSnapshots(): Flow<List<ContextSnapshot>> =
        snapshots.map { it.values.toList() }

    override fun getSnapshotById(id: String): Flow<ContextSnapshot?> =
        snapshots.map { it[id] }

    override suspend fun saveSnapshot(snapshot: ContextSnapshot) {
        snapshots.value = snapshots.value + (snapshot.id to snapshot)
    }

    override suspend fun deleteSnapshot(id: String) {
        snapshots.value = snapshots.value - id
    }

    override suspend fun markResumed(id: String, timestamp: Long) {
        val cur = snapshots.value[id] ?: return
        snapshots.value = snapshots.value + (id to cur.copy(lastResumedAt = timestamp))
    }

    override suspend fun markRestored(id: String, timestamp: Long) = markResumed(id, timestamp)

    override suspend fun togglePin(id: String, isPinned: Boolean) {
        val cur = snapshots.value[id] ?: return
        snapshots.value = snapshots.value + (id to cur.copy(isPinned = isPinned))
    }

    override fun getItemsForSnapshot(snapshotId: String): Flow<List<ContextItem>> =
        snapshots.map { it[snapshotId]?.items ?: emptyList() }

    override suspend fun saveContextItem(item: ContextItem) {}
    override suspend fun deleteContextItem(itemId: String) {}
    override suspend fun seedInitialData() {}
}
