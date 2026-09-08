package com.example.contextos.capture.modules

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.contextos.models.ContextItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Capture module for user-selected images (gallery/SAF) and camera photo captures.
 */
@Singleton
class ImageCaptureModule @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * Prepares a temporary image file in the app's cache directory and creates
     * a secure [Uri] via [FileProvider] for camera intent capture.
     */
    fun createCameraImageUri(): Pair<Uri, String> {
        val imageDir = File(context.cacheDir, "images").apply {
            if (!exists()) mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val photoFile = File(imageDir, "IMG_${timestamp}_${UUID.randomUUID().toString().take(6)}.jpg")
        val contentUri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
        } catch (e: Exception) {
            Uri.fromFile(photoFile)
        }
        return Pair(contentUri, photoFile.absolutePath)
    }

    /**
     * Captures an image selected from the gallery / storage picker via [Uri].
     */
    fun captureImageFromUri(
        snapshotId: String,
        uri: Uri,
        ocrRawText: String = "",
        displayOrder: Int = 0
    ): ContextItem {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Persistable permission might not be granted by all external pickers
        }

        return ContextItem.createImage(
            snapshotId = snapshotId,
            imageUri = uri.toString(),
            ocrRawText = ocrRawText,
            fileUri = uri.path,
            displayOrder = displayOrder
        )
    }

    /**
     * Captures an image from a local file path (e.g. captured by camera).
     */
    fun captureImageFromFile(
        snapshotId: String,
        filePath: String,
        ocrRawText: String = "",
        displayOrder: Int = 0
    ): ContextItem {
        return ContextItem.createImage(
            snapshotId = snapshotId,
            imageUri = "file://$filePath",
            ocrRawText = ocrRawText,
            fileUri = filePath,
            displayOrder = displayOrder
        )
    }
}
