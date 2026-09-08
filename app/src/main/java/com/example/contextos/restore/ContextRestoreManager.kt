package com.example.contextos.restore

import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Status of an individual item in the restoration queue.
 */
enum class ItemRestoreStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    SKIPPED
}

/**
 * Result of restoring an individual [ContextItem].
 */
data class ItemRestoreResult(
    val item: ContextItem,
    val status: ItemRestoreStatus,
    val message: String,
    val detail: String? = null,
    val isFatal: Boolean = false
)

/**
 * Live snapshot of context restoration queue execution.
 */
data class ContextRestoreProgress(
    val snapshotId: String,
    val snapshotName: String,
    val totalItems: Int,
    val completedCount: Int,
    val isComplete: Boolean,
    val statusMessage: String,
    val itemResults: List<ItemRestoreResult>,
    val currentRestoringItem: ContextItem? = null,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val restoredNotes: List<ContextItem> = emptyList(),
    val restoredImages: List<ContextItem> = emptyList()
) {
    val progressFraction: Float
        get() = if (totalItems <= 0) 1f else (completedCount.toFloat() / totalItems).coerceIn(0f, 1f)

    val hasErrors: Boolean
        get() = failureCount > 0
}

/**
 * High-level restoration manager interface for restoring a saved [ContextSnapshot].
 */
interface ContextRestoreManager {

    /**
     * Executes the restoration queue for [snapshot] and returns the final progress result.
     */
    suspend fun restoreContext(snapshot: ContextSnapshot): ContextRestoreProgress

    /**
     * Executes the restoration queue for [snapshot], sequentially emitting progress updates.
     */
    fun restoreContextFlow(snapshot: ContextSnapshot): Flow<ContextRestoreProgress>

    /**
     * Retrieves the snapshot by [snapshotId] and sequentially emits restoration progress updates.
     */
    fun restoreSnapshotByIdFlow(snapshotId: String): Flow<ContextRestoreProgress>

    /**
     * Restores an individual [ContextItem] as much as Android APIs allow.
     */
    suspend fun restoreItem(item: ContextItem): ItemRestoreResult
}
