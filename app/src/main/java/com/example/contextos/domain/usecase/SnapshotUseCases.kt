package com.example.contextos.domain.usecase

import com.example.contextos.domain.repository.SnapshotRepository
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

class MarkSnapshotRestoredUseCase @Inject constructor(
    private val repository: SnapshotRepository
) {
    suspend operator fun invoke(id: String) = repository.markRestored(id)
}
