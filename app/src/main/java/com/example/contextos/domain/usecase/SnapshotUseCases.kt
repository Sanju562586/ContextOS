package com.example.contextos.domain.usecase

import com.example.contextos.domain.repository.SnapshotRepository
import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextSnapshot
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSnapshotsUseCase @Inject constructor(
    private val repository: SnapshotRepository
) {
    operator fun invoke(): Flow<List<ContextSnapshot>> = repository.getSnapshots()
}

class GetSnapshotByIdUseCase @Inject constructor(
    private val repository: SnapshotRepository
) {
    operator fun invoke(id: String): Flow<ContextSnapshot?> = repository.getSnapshotById(id)
}

class SaveSnapshotUseCase @Inject constructor(
    private val repository: SnapshotRepository
) {
    suspend operator fun invoke(snapshot: ContextSnapshot) = repository.saveSnapshot(snapshot)
}

class DeleteSnapshotUseCase @Inject constructor(
    private val repository: SnapshotRepository
) {
    suspend operator fun invoke(id: String) = repository.deleteSnapshot(id)
}

class TogglePinSnapshotUseCase @Inject constructor(
    private val repository: SnapshotRepository
) {
    suspend operator fun invoke(id: String, isPinned: Boolean) = repository.togglePin(id, isPinned)
}

class MarkSnapshotResumedUseCase @Inject constructor(
    private val repository: SnapshotRepository
) {
    suspend operator fun invoke(id: String, timestamp: Long = System.currentTimeMillis()) =
        repository.markResumed(id, timestamp)
}

class MarkSnapshotRestoredUseCase @Inject constructor(
    private val repository: SnapshotRepository
) {
    suspend operator fun invoke(id: String, timestamp: Long = System.currentTimeMillis()) =
        repository.markRestored(id, timestamp)
}

class GetContextItemsUseCase @Inject constructor(
    private val repository: SnapshotRepository
) {
    operator fun invoke(snapshotId: String): Flow<List<ContextItem>> =
        repository.getItemsForSnapshot(snapshotId)
}

class SaveContextItemUseCase @Inject constructor(
    private val repository: SnapshotRepository
) {
    suspend operator fun invoke(item: ContextItem) = repository.saveContextItem(item)
}

class DeleteContextItemUseCase @Inject constructor(
    private val repository: SnapshotRepository
) {
    suspend operator fun invoke(itemId: String) = repository.deleteContextItem(itemId)
}

class SeedSampleDataUseCase @Inject constructor(
    private val repository: SnapshotRepository
) {
    suspend operator fun invoke() = repository.seedInitialData()
}
