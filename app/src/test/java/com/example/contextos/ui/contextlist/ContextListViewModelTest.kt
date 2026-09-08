package com.example.contextos.ui.contextlist

import com.example.contextos.domain.repository.SnapshotRepository
import com.example.contextos.domain.usecase.DeleteSnapshotUseCase
import com.example.contextos.domain.usecase.GetSnapshotsUseCase
import com.example.contextos.domain.usecase.MarkSnapshotRestoredUseCase
import com.example.contextos.domain.usecase.SaveSnapshotUseCase
import com.example.contextos.domain.usecase.TogglePinSnapshotUseCase
import com.example.contextos.models.ContextSnapshot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContextListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSnapshotRepository
    private lateinit var viewModel: ContextListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSnapshotRepository()
        viewModel = ContextListViewModel(
            getSnapshotsUseCase = GetSnapshotsUseCase(fakeRepository),
            saveSnapshotUseCase = SaveSnapshotUseCase(fakeRepository),
            deleteSnapshotUseCase = DeleteSnapshotUseCase(fakeRepository),
            togglePinSnapshotUseCase = TogglePinSnapshotUseCase(fakeRepository),
            markSnapshotRestoredUseCase = MarkSnapshotRestoredUseCase(fakeRepository),
            contextRestoreManager = FakeContextRestoreManager()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initiallyLoadsSuccessWithEmptyList() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ContextListUiState.Success)
        assertEquals(0, (state as ContextListUiState.Success).snapshots.size)
        collectJob.cancel()
    }

    @Test
    fun seedSampleProjectAlpha_addsProjectAlpha() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        viewModel.seedSampleProjectAlpha()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ContextListUiState.Success)
        val snapshots = (state as ContextListUiState.Success).snapshots
        assertEquals(1, snapshots.size)
        assertEquals("Project Alpha", snapshots.first().name)
        assertTrue(snapshots.first().isPinned)
        collectJob.cancel()
    }

    @Test
    fun seedSampleProjectBeta_addsProjectBeta() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        viewModel.seedSampleProjectBeta()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ContextListUiState.Success)
        val snapshots = (state as ContextListUiState.Success).snapshots
        assertEquals(1, snapshots.size)
        assertEquals("Project Beta", snapshots.first().name)
        collectJob.cancel()
    }

    @Test
    fun onDeleteClicked_removesSnapshot() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        viewModel.seedSampleProjectAlpha()
        testScheduler.advanceUntilIdle()

        val snapshotId = (viewModel.uiState.value as ContextListUiState.Success).snapshots.first().id
        viewModel.onDeleteClicked(snapshotId)
        testScheduler.advanceUntilIdle()

        val snapshots = (viewModel.uiState.value as ContextListUiState.Success).snapshots
        assertEquals(0, snapshots.size)
        collectJob.cancel()
    }

    @Test
    fun onResumeClicked_triggersRestoreQueue() = runTest(testDispatcher) {
        val collectJob1 = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        val collectJob2 = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.restoreProgress.collect()
        }

        viewModel.seedSampleProjectAlpha()
        testScheduler.advanceUntilIdle()

        val snapshot = (viewModel.uiState.value as ContextListUiState.Success).snapshots.first()
        viewModel.onResumeClicked(snapshot)
        testScheduler.advanceUntilIdle()

        val progress = viewModel.restoreProgress.value
        org.junit.Assert.assertNotNull(progress)
        org.junit.Assert.assertTrue(progress!!.isComplete)

        viewModel.dismissRestore()
        org.junit.Assert.assertNull(viewModel.restoreProgress.value)

        collectJob1.cancel()
        collectJob2.cancel()
    }
}

private class FakeContextRestoreManager : com.example.contextos.restore.ContextRestoreManager {
    override suspend fun restoreContext(snapshot: ContextSnapshot): com.example.contextos.restore.ContextRestoreProgress {
        return com.example.contextos.restore.ContextRestoreProgress(
            snapshotId = snapshot.id,
            snapshotName = snapshot.name,
            totalItems = snapshot.items.size,
            completedCount = snapshot.items.size,
            isComplete = true,
            statusMessage = "${snapshot.name} Restored",
            itemResults = emptyList()
        )
    }

    override fun restoreContextFlow(snapshot: ContextSnapshot): Flow<com.example.contextos.restore.ContextRestoreProgress> = kotlinx.coroutines.flow.flow {
        emit(
            com.example.contextos.restore.ContextRestoreProgress(
                snapshotId = snapshot.id,
                snapshotName = snapshot.name,
                totalItems = snapshot.items.size,
                completedCount = snapshot.items.size,
                isComplete = true,
                statusMessage = "${snapshot.name} Restored",
                itemResults = emptyList()
            )
        )
    }

    override fun restoreSnapshotByIdFlow(snapshotId: String): Flow<com.example.contextos.restore.ContextRestoreProgress> = kotlinx.coroutines.flow.flow {
        emit(
            com.example.contextos.restore.ContextRestoreProgress(
                snapshotId = snapshotId,
                snapshotName = "Context",
                totalItems = 0,
                completedCount = 0,
                isComplete = true,
                statusMessage = "Context Restored",
                itemResults = emptyList()
            )
        )
    }

    override suspend fun restoreItem(item: com.example.contextos.models.ContextItem): com.example.contextos.restore.ItemRestoreResult {
        return com.example.contextos.restore.ItemRestoreResult(
            item = item,
            status = com.example.contextos.restore.ItemRestoreStatus.SUCCESS,
            message = "Restored ${item.displayName}"
        )
    }
}

private class FakeSnapshotRepository : SnapshotRepository {
    private val snapshots = MutableStateFlow<Map<String, ContextSnapshot>>(emptyMap())

    override fun getSnapshots(): Flow<List<ContextSnapshot>> {
        return snapshots.map { it.values.toList() }
    }

    override fun getSnapshotById(id: String): Flow<ContextSnapshot?> {
        return snapshots.map { it[id] }
    }

    override suspend fun saveSnapshot(snapshot: ContextSnapshot) {
        snapshots.value = snapshots.value + (snapshot.id to snapshot)
    }

    override suspend fun deleteSnapshot(id: String) {
        snapshots.value = snapshots.value - id
    }

    override suspend fun markResumed(id: String, timestamp: Long) {
        val current = snapshots.value[id] ?: return
        snapshots.value = snapshots.value + (id to current.copy(lastResumedAt = timestamp))
    }

    override suspend fun markRestored(id: String, timestamp: Long) {
        markResumed(id, timestamp)
    }

    override suspend fun togglePin(id: String, isPinned: Boolean) {
        val current = snapshots.value[id] ?: return
        snapshots.value = snapshots.value + (id to current.copy(isPinned = isPinned))
    }

    override fun getItemsForSnapshot(snapshotId: String): Flow<List<com.example.contextos.models.ContextItem>> {
        return snapshots.map { it[snapshotId]?.items ?: emptyList() }
    }

    override suspend fun saveContextItem(item: com.example.contextos.models.ContextItem) {
        val snapshot = snapshots.value[item.snapshotId] ?: return
        val updatedItems = snapshot.items.filterNot { it.id == item.id } + item
        snapshots.value = snapshots.value + (snapshot.id to snapshot.copy(items = updatedItems))
    }

    override suspend fun deleteContextItem(itemId: String) {
        snapshots.value = snapshots.value.mapValues { (_, snapshot) ->
            snapshot.copy(items = snapshot.items.filterNot { it.id == itemId })
        }
    }

    override suspend fun seedInitialData() {
        // no-op for fake
    }
}
