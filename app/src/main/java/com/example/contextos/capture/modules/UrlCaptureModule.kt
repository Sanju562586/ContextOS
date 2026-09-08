package com.example.contextos.capture.modules

import com.example.contextos.models.ContextItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Capture module for user-provided or auto-detected URLs.
 */
@Singleton
class UrlCaptureModule @Inject constructor() {

    fun captureUrl(
        snapshotId: String,
        rawUrl: String,
        title: String = "",
        displayOrder: Int = 0
    ): ContextItem? {
        val sanitized = rawUrl.trim()
        if (sanitized.isBlank()) return null

        val normalized = if (!sanitized.startsWith("http://", ignoreCase = true) &&
            !sanitized.startsWith("https://", ignoreCase = true)) {
            "https://$sanitized"
        } else {
            sanitized
        }

        val displayTitle = title.ifBlank {
            try {
                val host = java.net.URI(normalized).host
                if (!host.isNullOrBlank()) host.removePrefix("www.") else normalized
            } catch (e: Exception) {
                normalized
            }
        }

        return ContextItem.createUrl(
            snapshotId = snapshotId,
            url = normalized,
            title = displayTitle,
            displayOrder = displayOrder
        )
    }
}
