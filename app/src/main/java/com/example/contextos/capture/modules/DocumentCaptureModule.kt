package com.example.contextos.capture.modules

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.contextos.models.ContextItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Capture module for user-selected documents.
 *
 * Uses the Android Storage Access Framework (SAF) to securely obtain persistable
 * content URI permissions and document metadata (name, MIME type, file size).
 */
@Singleton
class DocumentCaptureModule @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * Extracts metadata from a user-selected document [Uri] and takes persistable
     * read permissions if available.
     */
    fun captureDocument(
        snapshotId: String,
        uri: Uri,
        displayOrder: Int = 0
    ): ContextItem {
        val contentResolver = context.contentResolver

        // Attempt to persist URI permissions so documents remain accessible across reboots
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Some providers or temporary URIs don't support persistable permissions
        }

        val fallbackName = (uri.lastPathSegment ?: uri.path ?: "Document")
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .ifBlank { "Document" }
        var displayName = fallbackName
        var fileSizeBytes = 0L
        val mimeType = contentResolver.getType(uri) ?: "*/*"

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val queriedName = cursor.getString(nameIndex)
                        if (!queriedName.isNullOrBlank()) {
                            displayName = queriedName
                        }
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        fileSizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Keep fallback displayName
        }

        return ContextItem.createDocument(
            snapshotId = snapshotId,
            contentUri = uri.toString(),
            displayName = displayName,
            mimeType = mimeType,
            fileSizeBytes = fileSizeBytes,
            fileUri = uri.path,
            displayOrder = displayOrder
        )
    }

    /**
     * Batch processes multiple user-selected documents.
     */
    fun captureDocuments(
        snapshotId: String,
        uris: List<Uri>,
        startOrder: Int = 0
    ): List<ContextItem> {
        return uris.mapIndexed { index, uri ->
            captureDocument(snapshotId, uri, startOrder + index)
        }
    }
}
