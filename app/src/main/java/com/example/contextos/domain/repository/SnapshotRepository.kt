package com.example.contextos.domain.repository

import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing ContextOS snapshots and context items.
 */
interface SnapshotRepository {
    fun getSnapshots(): Flow<List<ContextSnapshot>>
    fun getSnapshotById(id: String): Flow<ContextSnapshot?>
    suspend fun saveSnapshot(snapshot: ContextSnapshot)
    suspend fun deleteSnapshot(id: String)
    suspend fun markResumed(id: String, timestamp: Long = System.currentTimeMillis())
    suspend fun markRestored(id: String, timestamp: Long = System.currentTimeMillis())
    suspend fun togglePin(id: String, isPinned: Boolean)
    fun getItemsForSnapshot(snapshotId: String): Flow<List<ContextItem>>
    suspend fun saveContextItem(item: ContextItem)
    suspend fun deleteContextItem(itemId: String)
    suspend fun seedInitialData()
}
