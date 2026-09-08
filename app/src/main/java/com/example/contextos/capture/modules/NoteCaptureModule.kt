package com.example.contextos.capture.modules

import com.example.contextos.models.ContextItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Capture module for user-provided scratchpad notes and tasks.
 */
@Singleton
class NoteCaptureModule @Inject constructor() {

    fun captureNote(
        snapshotId: String,
        content: String,
        isCompleted: Boolean = false,
        displayOrder: Int = 0
    ): ContextItem? {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return null

        return ContextItem.createNote(
            snapshotId = snapshotId,
            content = trimmed,
            isCompleted = isCompleted,
            displayOrder = displayOrder
        )
    }
}
