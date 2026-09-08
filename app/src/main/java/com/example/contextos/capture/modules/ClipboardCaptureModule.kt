package com.example.contextos.capture.modules

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import com.example.contextos.models.ContextItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Capture module for device clipboard content.
 *
 * =========================================================================================
 * ANDROID OS ARCHITECTURAL RESTRICTIONS & LEGAL COMPLIANCE
 * =========================================================================================
 * Starting in Android 10 (API level 29), Google introduced strict privacy controls for the
 * clipboard:
 *
 * 1. FOREGROUND & WINDOW FOCUS RESTRICTION:
 *    - Applications cannot observe or read the clipboard from a background service or worker.
 *    - ClipboardManager.getPrimaryClip() returns null or throws SecurityException unless:
 *        a) The calling app is currently in the foreground AND holds window focus, OR
 *        b) The calling app is the default Input Method Editor (IME).
 *
 * 2. BEST LEGAL ALTERNATIVE IMPLEMENTED HERE:
 *    - Capture is only invoked when the ContextOS capture UI has active window focus.
 *    - All calls are wrapped in robust exception handling (catching SecurityException).
 *    - Text snippets and copied URLs are parsed safely, giving users full visibility and control
 *      to review or omit clipboard items before saving.
 * =========================================================================================
 */
@Singleton
class ClipboardCaptureModule @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * Reads the current clipboard text if available, non-empty, and legally accessible.
     */
    fun captureCurrentClipboard(snapshotId: String, displayOrder: Int = 0): ContextItem? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null

        try {
            if (!clipboard.hasPrimaryClip()) return null

            val description = clipboard.primaryClipDescription ?: return null
            if (!description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
                !description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                return null
            }

            val clipData = clipboard.primaryClip ?: return null
            if (clipData.itemCount <= 0) return null

            val item = clipData.getItemAt(0) ?: return null
            val text = item.text?.toString()?.trim() ?: return null

            if (text.isBlank()) return null

            // Detect if the clipboard content is a web URL
            val isUrl = text.startsWith("http://", ignoreCase = true) ||
                    text.startsWith("https://", ignoreCase = true) ||
                    (text.contains(".") && !text.contains(" ") && (text.startsWith("www.", ignoreCase = true) || text.endsWith(".com") || text.endsWith(".org") || text.endsWith(".io")))

            return if (isUrl) {
                val normalizedUrl = if (!text.startsWith("http://") && !text.startsWith("https://")) {
                    "https://$text"
                } else text
                ContextItem.createUrl(
                    snapshotId = snapshotId,
                    url = normalizedUrl,
                    title = "Copied Link: $text",
                    displayOrder = displayOrder
                )
            } else {
                ContextItem.createClipboard(
                    snapshotId = snapshotId,
                    content = text,
                    mimeType = "text/plain",
                    displayOrder = displayOrder
                )
            }
        } catch (e: SecurityException) {
            // Android 10+ focus or permission restriction
            return null
        } catch (e: Exception) {
            return null
        }
    }
}
