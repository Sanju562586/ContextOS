package com.example.contextos.ui.capture

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.contextos.capture.ContextCaptureManager
import com.example.contextos.capture.ContextCaptureManagerImpl
import com.example.contextos.capture.modules.AppCaptureModule
import com.example.contextos.capture.modules.AppMetadata
import com.example.contextos.capture.modules.ClipboardCaptureModule
import com.example.contextos.capture.modules.DocumentCaptureModule
import com.example.contextos.capture.modules.NoteCaptureModule
import com.example.contextos.capture.modules.UrlCaptureModule
import com.example.contextos.domain.repository.SnapshotRepository
import com.example.contextos.domain.usecase.SaveSnapshotUseCase
import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CaptureContextViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSnapshotRepository
    private lateinit var captureManager: ContextCaptureManager
    private lateinit var appCaptureModule: AppCaptureModule
    private lateinit var documentCaptureModule: DocumentCaptureModule
    private lateinit var urlCaptureModule: UrlCaptureModule
    private lateinit var noteCaptureModule: NoteCaptureModule
    private lateinit var viewModel: CaptureContextViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        fakeRepository = FakeSnapshotRepository()
        appCaptureModule = AppCaptureModule(context)
        val clipboardCaptureModule = ClipboardCaptureModule(context)
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

        viewModel = CaptureContextViewModel(
            captureManager = captureManager,
            appCaptureModule = appCaptureModule,
            documentCaptureModule = documentCaptureModule,
            urlCaptureModule = urlCaptureModule,
            noteCaptureModule = noteCaptureModule,
            saveSnapshotUseCase = SaveSnapshotUseCase(fakeRepository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startCapture_emptyName_showsErrorMessage() = runTest(testDispatcher) {
        viewModel.onNameChanged("   ")
        viewModel.startCapture()

        val state = viewModel.uiState.value
        assertEquals(CaptureStep.INPUT_NAME, state.step)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun startCapture_validName_transitionsToReviewStep() = runTest(testDispatcher) {
        viewModel.onNameChanged("Project Alpha")
        viewModel.startCapture()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CaptureStep.REVIEW_ITEMS, state.step)
        assertEquals("Project Alpha", state.contextName)
    }

    @Test
    fun toggleAppSelection_addsAndRemovesApp() = runTest(testDispatcher) {
        val app = AppMetadata(packageName = "com.android.chrome", appName = "Chrome")
        viewModel.toggleAppSelection(app)

        var state = viewModel.uiState.value
        assertEquals(1, state.detectedApps.size)
        assertEquals("com.android.chrome", state.detectedApps.first().packageName)

        // Toggle again removes it
        viewModel.toggleAppSelection(app)
        state = viewModel.uiState.value
        assertEquals(0, state.detectedApps.size)
    }

    @Test
    fun addUrlAndNote_updatesUiState() = runTest(testDispatcher) {
        viewModel.addUrl("https://github.com/project-alpha", "Roadmap")
        viewModel.addNote("Review latency targets")

        val state = viewModel.uiState.value
        assertEquals(1, state.urls.size)
        assertEquals("https://github.com/project-alpha", state.urls.first().url)
        assertEquals(1, state.notes.size)
        assertEquals("Review latency targets", state.notes.first().content)
    }

    @Test
    fun confirmAndSave_savesSnapshotToRepositoryAndEmitsEvent() = runTest(testDispatcher) {
        val events = mutableListOf<CaptureEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.onNameChanged("Project Alpha")
        viewModel.onDescriptionChanged("Sprint planning workspace")
        viewModel.addUrl("https://github.com/project-alpha/roadmap", "Roadmap")
        viewModel.addNote("Sync with team at 10am")

        viewModel.confirmAndSave()
        testScheduler.advanceUntilIdle()

        val snapshots = fakeRepository.getSnapshots().first()
        assertEquals(1, snapshots.size)
        val saved = snapshots.first()
        assertEquals("Project Alpha", saved.name)
        assertEquals("Sprint planning workspace", saved.description)
        assertEquals(2, saved.items.size)

        assertEquals(1, events.size)
        assertTrue(events.first() is CaptureEvent.SnapshotSaved)
        job.cancel()
    }
}

private class FakeSnapshotRepository : SnapshotRepository {
    private val snapshots = MutableStateFlow<Map<String, ContextSnapshot>>(emptyMap())

    override fun getSnapshots(): Flow<List<ContextSnapshot>> = snapshots.map { it.values.toList() }
    override fun getSnapshotById(id: String): Flow<ContextSnapshot?> = snapshots.map { it[id] }

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
