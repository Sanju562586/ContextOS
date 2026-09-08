package com.example.contextos.capture

import com.example.contextos.capture.modules.AppMetadata
import com.example.contextos.models.ContextSnapshot
import java.util.UUID

/**
 * Orchestrator interface for capturing multi-modal work context on Android.
 */
interface ContextCaptureManager {

    /**
     * Checks whether the user has granted Special App Access for Usage Statistics.
     */
    fun hasUsageAccess(): Boolean

    /**
     * Captures all legally accessible context currently available on the device:
     * - Recent apps via UsageStatsManager (or defaults if not granted)
     * - Foreground clipboard text/URL (if permitted and available)
     */
    suspend fun captureAvailableContext(
        snapshotId: String = UUID.randomUUID().toString(),
        includeClipboard: Boolean = true,
        includeRecentApps: Boolean = true
    ): ContextCaptureResult

    /**
     * Returns installed applications with launcher activities for manual selection.
     */
    fun getAvailableApps(): List<AppMetadata>

    /**
     * Performs a direct one-shot capture of available context into a [ContextSnapshot].
     */
    suspend fun captureCurrentContext(name: String, description: String = ""): ContextSnapshot
}
