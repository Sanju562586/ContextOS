package com.example.contextos.ui.detail

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.contextos.capture.modules.AppCaptureModule
import com.example.contextos.capture.modules.DocumentCaptureModule
import com.example.contextos.capture.modules.ImageCaptureModule
import com.example.contextos.capture.modules.NoteCaptureModule
import com.example.contextos.capture.modules.UrlCaptureModule
import com.example.contextos.domain.repository.SnapshotRepository
import com.example.contextos.domain.usecase.DeleteContextItemUseCase
import com.example.contextos.domain.usecase.GetSnapshotByIdUseCase
import com.example.contextos.domain.usecase.MarkSnapshotRestoredUseCase
import com.example.contextos.domain.usecase.SaveContextItemUseCase
import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextItemType
import com.example.contextos.models.ContextSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ContextDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var fakeRepository: FakeSnapshotRepository
    private lateinit var appCaptureModule: AppCaptureModule
    private lateinit var documentCaptureModule: DocumentCaptureModule
    private lateinit var urlCaptureModule: UrlCaptureModule
    private lateinit var noteCaptureModule: NoteCaptureModule
    private lateinit var imageCaptureModule: ImageCaptureModule
    private lateinit var viewModel: ContextDetailViewModel

    private val testSnapshotId = "snapshot-project-alpha-001"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        fakeRepository = FakeSnapshotRepository()

        // Seed an empty Project Alpha snapshot
        val alpha = ContextSnapshot(
            id = testSnapshotId,
            name = "Project Alpha",
            description = "Architecture review and roadmap",
            items = emptyList()
        )
        fakeRepository.seed(listOf(alpha))

        appCaptureModule = AppCaptureModule(context)
        documentCaptureModule = DocumentCaptureModule(context)
        urlCaptureModule = UrlCaptureModule()
        noteCaptureModule = NoteCaptureModule()
        imageCaptureModule = ImageCaptureModule(context)
        val restoreManager = com.example.contextos.restore.ContextRestoreManagerImpl(
            context = context,
            getSnapshotByIdUseCase = GetSnapshotByIdUseCase(fakeRepository),
            markSnapshotRestoredUseCase = MarkSnapshotRestoredUseCase(fakeRepository)
        ).apply { defaultItemDelayMillis = 0L }

        viewModel = ContextDetailViewModel(
            getSnapshotByIdUseCase = GetSnapshotByIdUseCase(fakeRepository),
            markSnapshotRestoredUseCase = MarkSnapshotRestoredUseCase(fakeRepository),
            saveContextItemUseCase = SaveContextItemUseCase(fakeRepository),
            deleteContextItemUseCase = DeleteContextItemUseCase(fakeRepository),
            appCaptureModule = appCaptureModule,
            documentCaptureModule = documentCaptureModule,
            urlCaptureModule = urlCaptureModule,
            noteCaptureModule = noteCaptureModule,
            imageCaptureModule = imageCaptureModule,
            contextRestoreManager = restoreManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadSnapshot_emitsSuccessWithSnapshotDetails() = runTest(testDispatcher) {
        val observedStates = mutableListOf<ContextDetailUiState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { observedStates.add(it) }
        }

        viewModel.loadSnapshot(testSnapshotId)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ContextDetailUiState.Success)
        val success = state as ContextDetailUiState.Success
        assertEquals("Project Alpha", success.snapshot.name)
        assertEquals(0, success.snapshot.items.size)

        job.cancel()
    }

    @Test
    fun addApp_createsAndPersistsAppItem() = runTest(testDispatcher) {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.loadSnapshot(testSnapshotId)
        testScheduler.advanceUntilIdle()

        viewModel.addApp(packageName = "com.android.chrome", appName = "Chrome")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as ContextDetailUiState.Success
        assertEquals(1, state.snapshot.items.size)
        val app = state.snapshot.items.first()
        assertEquals(ContextItemType.APP, app.type)
        assertEquals("Chrome", app.appName)
        assertEquals("com.android.chrome", app.packageName)

        job.cancel()
    }

    @Test
    fun addUrl_normalizesAndPersistsUrlItem() = runTest(testDispatcher) {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.loadSnapshot(testSnapshotId)
        testScheduler.advanceUntilIdle()

        viewModel.addUrl("github.com/project-alpha/roadmap", "Research URL")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as ContextDetailUiState.Success
        assertEquals(1, state.snapshot.items.size)
        val urlItem = state.snapshot.items.first()
        assertEquals(ContextItemType.URL, urlItem.type)
        assertEquals("https://github.com/project-alpha/roadmap", urlItem.url)
        assertEquals("Research URL", urlItem.title)

        job.cancel()
    }

    @Test
    fun addDocuments_extractsMetadataAndPersistsItem() = runTest(testDispatcher) {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.loadSnapshot(testSnapshotId)
        testScheduler.advanceUntilIdle()

        // Create a temporary file for doc URI
        val testDoc = File(context.cacheDir, "report.docx").apply {
            writeText("Project Alpha Architecture Specification")
        }
        val docUri = Uri.fromFile(testDoc)

        viewModel.addDocuments(listOf(docUri))
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as ContextDetailUiState.Success
        assertEquals(1, state.snapshot.items.size)
        val docItem = state.snapshot.items.first()
        assertEquals(ContextItemType.DOCUMENT, docItem.type)
        assertEquals("report.docx", docItem.displayName)

        job.cancel()
    }

    @Test
    fun addNote_persistsNoteItem() = runTest(testDispatcher) {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.loadSnapshot(testSnapshotId)
        testScheduler.advanceUntilIdle()

        viewModel.addNote("Task Notes: Verify multi-region failover latency")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as ContextDetailUiState.Success
        assertEquals(1, state.snapshot.items.size)
        val note = state.snapshot.items.first()
        assertEquals(ContextItemType.NOTE, note.type)
        assertEquals("Task Notes: Verify multi-region failover latency", note.content)

        job.cancel()
    }

    @Test
    fun addImageFromUri_persistsGalleryImage() = runTest(testDispatcher) {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.loadSnapshot(testSnapshotId)
        testScheduler.advanceUntilIdle()

        val galleryUri = Uri.parse("content://media/external/images/media/100")
        viewModel.addImageFromUri(galleryUri)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as ContextDetailUiState.Success
        assertEquals(1, state.snapshot.items.size)
        val img = state.snapshot.items.first()
        assertEquals(ContextItemType.IMAGE, img.type)
        assertEquals(galleryUri.toString(), img.imageUri)

        job.cancel()
    }

    @Test
    fun cameraCaptureFlow_preparesUriAndSavesPhotoFile() = runTest(testDispatcher) {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.loadSnapshot(testSnapshotId)
        testScheduler.advanceUntilIdle()

        val cameraUri = viewModel.prepareCameraPhotoCapture()
        assertNotNull(cameraUri)

        // Simulate camera activity completing successfully
        viewModel.onCameraPhotoCaptured(success = true)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as ContextDetailUiState.Success
        assertEquals(1, state.snapshot.items.size)
        val capturedImg = state.snapshot.items.first()
        assertEquals(ContextItemType.IMAGE, capturedImg.type)
        assertNotNull(capturedImg.imageUri)
        assertTrue(capturedImg.imageUri!!.startsWith("file://"))

        job.cancel()
    }

    @Test
    fun deleteItem_removesItemFromSnapshot() = runTest(testDispatcher) {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.loadSnapshot(testSnapshotId)
        testScheduler.advanceUntilIdle()

        viewModel.addApp("com.android.chrome", "Chrome")
        viewModel.addNote("Quick note")
        testScheduler.advanceUntilIdle()

        var state = viewModel.uiState.value as ContextDetailUiState.Success
        assertEquals(2, state.snapshot.items.size)

        val appItem = state.snapshot.items.first { it.type == ContextItemType.APP }
        viewModel.deleteItem(appItem.id)
        testScheduler.advanceUntilIdle()

        state = viewModel.uiState.value as ContextDetailUiState.Success
        assertEquals(1, state.snapshot.items.size)
        assertEquals(ContextItemType.NOTE, state.snapshot.items.first().type)

        job.cancel()
    }

    @Test
    fun allFiveItemTypes_unifiedWorkspaceListDisplaysAllCorrectly() = runTest(testDispatcher) {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.loadSnapshot(testSnapshotId)
        testScheduler.advanceUntilIdle()

        // 1. Add App
        viewModel.addApp("com.android.chrome", "Chrome")

        // 2. Add URL
        viewModel.addUrl("https://github.com/project-alpha/research", "Research URL")

        // 3. Add Document
        val docFile = File(context.cacheDir, "report.docx").apply { writeText("Report Content") }
        viewModel.addDocuments(listOf(Uri.fromFile(docFile)))

        // 4. Add Note
        viewModel.addNote("Task Notes")

        // 5. Add Image
        val imageFile = File(context.cacheDir, "whiteboard.jpg").apply { writeText("Image binary mock") }
        viewModel.addImageFromUri(Uri.fromFile(imageFile))

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as ContextDetailUiState.Success
        val snapshot = state.snapshot

        assertEquals("Project Alpha", snapshot.name)
        assertEquals(5, snapshot.items.size)

        val apps = snapshot.items.filter { it.type == ContextItemType.APP }
        val urls = snapshot.items.filter { it.type == ContextItemType.URL }
        val docs = snapshot.items.filter { it.type == ContextItemType.DOCUMENT }
        val notes = snapshot.items.filter { it.type == ContextItemType.NOTE }
        val images = snapshot.items.filter { it.type == ContextItemType.IMAGE }

        assertEquals(1, apps.size)
        assertEquals("Chrome", apps.first().appName)

        assertEquals(1, urls.size)
        assertEquals("Research URL", urls.first().title)

        assertEquals(1, docs.size)
        assertEquals("report.docx", docs.first().displayName)

        assertEquals(1, notes.size)
        assertEquals("Task Notes", notes.first().content)

        assertEquals(1, images.size)
        assertNotNull(images.first().imageUri)

        job.cancel()
    }

    @Test
    fun restore_triggersRestorationQueueAndUpdatesProgress() = runTest(testDispatcher) {
        val job1 = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        val job2 = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.restoreProgress.collect {}
        }

        viewModel.loadSnapshot(testSnapshotId)
        viewModel.addNote("Task Notes")
        viewModel.addUrl("https://example.com", "Example")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as ContextDetailUiState.Success
        viewModel.restore(state.snapshot)
        testScheduler.advanceUntilIdle()

        val progress = viewModel.restoreProgress.value
        assertNotNull(progress)
        assertTrue(progress!!.isComplete)
        assertEquals(2, progress.totalItems)
        assertEquals(2, progress.completedCount)
        assertEquals(2, progress.successCount)

        viewModel.dismissRestore()
        org.junit.Assert.assertNull(viewModel.restoreProgress.value)

        job1.cancel()
        job2.cancel()
    }
}

/**
 * Fake implementation of [SnapshotRepository] with reactive flows that propagate item additions and deletions.
 */
private class FakeSnapshotRepository : SnapshotRepository {
    private val snapshots = MutableStateFlow<Map<String, ContextSnapshot>>(emptyMap())

    fun seed(initial: List<ContextSnapshot>) {
        snapshots.value = initial.associateBy { it.id }
    }

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

    override suspend fun saveContextItem(item: ContextItem) {
        val currentSnapshot = snapshots.value[item.snapshotId] ?: return
        val existingIndex = currentSnapshot.items.indexOfFirst { it.id == item.id }
        val updatedItems = if (existingIndex != -1) {
            currentSnapshot.items.toMutableList().apply { set(existingIndex, item) }
        } else {
            currentSnapshot.items + item
        }
        snapshots.value = snapshots.value + (item.snapshotId to currentSnapshot.copy(items = updatedItems))
    }

    override suspend fun deleteContextItem(itemId: String) {
        for ((snapId, snap) in snapshots.value) {
            if (snap.items.any { it.id == itemId }) {
                val updatedItems = snap.items.filterNot { it.id == itemId }
                snapshots.value = snapshots.value + (snapId to snap.copy(items = updatedItems))
                break
            }
        }
    }

    override suspend fun seedInitialData() {}
}
