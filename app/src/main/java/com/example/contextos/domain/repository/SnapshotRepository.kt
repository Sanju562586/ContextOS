package com.example.contextos.domain.repository

import com.example.contextos.models.ContextSnapshot
import kotlinx.coroutines.flow.Flow

interface SnapshotRepository {
    fun getSnapshots(): Flow<List<ContextSnapshot>>
    fun getSnapshotById(id: String): Flow<ContextSnapshot?>
    suspend fun saveSnapshot(snapshot: ContextSnapshot)
    suspend fun deleteSnapshot(id: String)
    suspend fun markRestored(id: String, timestamp: Long = System.currentTimeMillis())
    suspend fun togglePin(id: String, isPinned: Boolean)
}
