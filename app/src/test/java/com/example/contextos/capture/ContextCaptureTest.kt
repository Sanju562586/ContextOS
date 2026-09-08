package com.example.contextos.capture

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.contextos.capture.modules.AppCaptureModule
import com.example.contextos.capture.modules.AppMetadata
import com.example.contextos.capture.modules.ClipboardCaptureModule
import com.example.contextos.capture.modules.DocumentCaptureModule
import com.example.contextos.capture.modules.NoteCaptureModule
import com.example.contextos.capture.modules.UrlCaptureModule
import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextItemType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class ContextCaptureTest {

    private lateinit var context: Context
    private lateinit var appCaptureModule: AppCaptureModule
    private lateinit var clipboardCaptureModule: ClipboardCaptureModule
    private lateinit var documentCaptureModule: DocumentCaptureModule
    private lateinit var urlCaptureModule: UrlCaptureModule
    private lateinit var noteCaptureModule: NoteCaptureModule
    private lateinit var captureManager: ContextCaptureManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        appCaptureModule = AppCaptureModule(context)
        clipboardCaptureModule = ClipboardCaptureModule(context)
        documentCaptureModule = DocumentCaptureModule(context)
        urlCaptureModule = UrlCaptureModule()
        noteCaptureModule = NoteCaptureModule()

        captureManager = ContextCaptureManagerImpl(
            appCaptureModule = appCaptureModule,
            clipboardCaptureModule = clipboardCaptureModule,
            documentCaptureModule = documentCaptureModule,
            urlCaptureModule = urlCaptureModule,
            noteCaptureModule = noteCaptureModule
        )
    }

    @Test
    fun urlCaptureModule_normalizesUrlsAndExtractsHost() {
        val item1 = urlCaptureModule.captureUrl(
            snapshotId = "snap-1",
            rawUrl = "github.com/project-alpha/repo",
            title = ""
        )
        assertNotNull(item1)
        assertEquals("https://github.com/project-alpha/repo", item1?.url)
        assertEquals("github.com", item1?.title)

        val item2 = urlCaptureModule.captureUrl(
            snapshotId = "snap-1",
            rawUrl = "https://figma.com/file/123",
            title = "Design System"
        )
        assertNotNull(item2)
        assertEquals("https://figma.com/file/123", item2?.url)
        assertEquals("Design System", item2?.title)

        val blankItem = urlCaptureModule.captureUrl(
            snapshotId = "snap-1",
            rawUrl = "   "
        )
        assertNull(blankItem)
    }

    @Test
    fun noteCaptureModule_createsNotesAndTrimsWhitespace() {
        val note = noteCaptureModule.captureNote(
            snapshotId = "snap-2",
            content = "  Review latency before deploying to prod  "
        )
        assertNotNull(note)
        assertEquals("Review latency before deploying to prod", note?.content)
        assertEquals(ContextItemType.NOTE, note?.type)

        val emptyNote = noteCaptureModule.captureNote(
            snapshotId = "snap-2",
            content = ""
        )
        assertNull(emptyNote)
    }

    @Test
    fun appCaptureModule_createAppItemWithSplitScreenFlag() {
        val appItem = appCaptureModule.createAppItem(
            snapshotId = "snap-3",
            packageName = "com.android.chrome",
            appName = "Chrome",
            isPrimaryForSplitScreen = true
        )
        assertEquals("com.android.chrome", appItem.packageName)
        assertEquals("Chrome", appItem.appName)
        assertTrue(appItem.isPrimaryForSplitScreen)
        assertEquals(ContextItemType.APP, appItem.type)
    }

    @Test
    fun contextCaptureResult_toSnapshot_producesCompleteSnapshot() {
        val snapshotId = UUID.randomUUID().toString()
        val app = ContextItem.createApp(
            snapshotId = snapshotId,
            packageName = "com.google.android.apps.docs",
            appName = "Google Drive"
        )
        val url = ContextItem.createUrl(
            snapshotId = snapshotId,
            url = "https://console.cloud.google.com",
            title = "Google Cloud"
        )
        val note = ContextItem.createNote(
            snapshotId = snapshotId,
            content = "Standup notes"
        )

        val result = ContextCaptureResult(
            snapshotId = snapshotId,
            detectedApps = listOf(app),
            capturedUrls = listOf(url),
            notes = listOf(note)
        )

        val snapshot = result.toSnapshot(
            name = "Project Alpha",
            description = "Sprint context review"
        )

        assertEquals(snapshotId, snapshot.id)
        assertEquals("Project Alpha", snapshot.name)
        assertEquals("Sprint context review", snapshot.description)
        assertEquals(3, snapshot.items.size)
        assertEquals(1, snapshot.apps.size)
        assertEquals(1, snapshot.links.size)
        assertEquals(1, snapshot.notes.size)
    }

    @Test
    fun captureManager_captureCurrentContext_createsValidSnapshot() = runTest {
        val snapshot = captureManager.captureCurrentContext(
            name = "Project Alpha",
            description = "Automated test capture"
        )

        assertEquals("Project Alpha", snapshot.name)
        assertEquals("Automated test capture", snapshot.description)
        assertNotNull(snapshot.id)
        assertTrue(snapshot.createdAt > 0L)
    }

    @Test
    fun captureManager_warnsWhenUsageAccessNotGranted() = runTest {
        val result = captureManager.captureAvailableContext(
            snapshotId = "snap-warn",
            includeClipboard = false,
            includeRecentApps = true
        )

        if (!captureManager.hasUsageAccess()) {
            assertTrue(result.warnings.isNotEmpty())
            assertTrue(result.warnings.first().contains("Usage access"))
        }
    }
}
