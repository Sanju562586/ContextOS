package com.example.contextos.capture

import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextItemType
import com.example.contextos.models.ContextSnapshot
import java.util.UUID

/**
 * Result of a context capture operation across all modular providers.
 *
 * Encapsulates items legally discovered through supported Android APIs,
 * along with any platform permission notices or warnings.
 */
data class ContextCaptureResult(
    val snapshotId: String = UUID.randomUUID().toString(),
    val detectedApps: List<ContextItem> = emptyList(),
    val capturedClipboard: ContextItem? = null,
    val capturedUrls: List<ContextItem> = emptyList(),
    val selectedDocuments: List<ContextItem> = emptyList(),
    val notes: List<ContextItem> = emptyList(),
    val warnings: List<String> = emptyList(),
    val capturedAt: Long = System.currentTimeMillis()
) {
    /**
     * All captured items combined in display order.
     */
    val allItems: List<ContextItem>
        get() = (detectedApps + capturedUrls + selectedDocuments + notes + listOfNotNull(capturedClipboard))
            .sortedBy { it.displayOrder }

    /**
     * Converts this capture result into a persistable [ContextSnapshot].
     */
    fun toSnapshot(
        name: String,
        description: String = "",
        aiSummary: String? = null,
        isPinned: Boolean = false,
        colorHex: String = "#1E88E5",
        itemsToInclude: List<ContextItem> = allItems
    ): ContextSnapshot {
        val now = System.currentTimeMillis()
        val remappedItems = itemsToInclude.mapIndexed { index, item ->
            item.copy(
                snapshotId = snapshotId,
                displayOrder = index
            )
        }
        return ContextSnapshot(
            id = snapshotId,
            name = name.ifBlank { "Untitled Context" },
            description = description,
            aiSummary = aiSummary,
            createdAt = now,
            updatedAt = now,
            lastResumedAt = null,
            isPinned = isPinned,
            colorHex = colorHex,
            items = remappedItems,
            nextActions = emptyList()
        )
    }
}
