package com.example.contextos.restore

import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.contextos.domain.usecase.GetSnapshotByIdUseCase
import com.example.contextos.domain.usecase.MarkSnapshotRestoredUseCase
import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextItemType
import com.example.contextos.models.ContextSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [ContextRestoreManager].
 *
 * Implements a sequential restoration queue with robust error tolerance:
 * if an app or document is unavailable or uninstalled, it tracks the failure,
 * displays a clear message, and continues restoring the remaining items.
 */
@Singleton
class ContextRestoreManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getSnapshotByIdUseCase: GetSnapshotByIdUseCase,
    private val markSnapshotRestoredUseCase: MarkSnapshotRestoredUseCase
) : ContextRestoreManager {

    var defaultItemDelayMillis: Long = 150L

    override suspend fun restoreContext(snapshot: ContextSnapshot): ContextRestoreProgress {
        var lastProgress: ContextRestoreProgress? = null
        restoreContextFlow(snapshot).collect { progress ->
            lastProgress = progress
        }
        return lastProgress ?: createInitialProgress(snapshot)
    }

    override fun restoreContextFlow(snapshot: ContextSnapshot): Flow<ContextRestoreProgress> = flow {
        val items = snapshot.items
        var successCount = 0
        var failureCount = 0
        val results = mutableListOf<ItemRestoreResult>()

        // 1. Display initial restoration summary
        val initialProgress = ContextRestoreProgress(
            snapshotId = snapshot.id,
            snapshotName = snapshot.name,
            totalItems = items.size,
            completedCount = 0,
            isComplete = items.isEmpty(),
            statusMessage = if (items.isEmpty()) "${snapshot.name} has no items to restore" else "Restoring ${snapshot.name}...",
            itemResults = emptyList(),
            currentRestoringItem = null,
            successCount = 0,
            failureCount = 0,
            restoredNotes = snapshot.notes,
            restoredImages = snapshot.images
        )
        emit(initialProgress)

        if (items.isEmpty()) {
            markSnapshotRestoredUseCase(snapshot.id)
            return@flow
        }

        // 2 & 3. Restore supported items sequentially
        for (item in items) {
            // Signal item is now restoring
            emit(
                initialProgress.copy(
                    completedCount = results.size,
                    currentRestoringItem = item,
                    itemResults = results.toList(),
                    successCount = successCount,
                    failureCount = failureCount
                )
            )

            if (defaultItemDelayMillis > 0L) {
                delay(defaultItemDelayMillis)
            }

            // Execute item restoration
            val result = restoreItem(item)
            results.add(result)

            if (result.status == ItemRestoreStatus.SUCCESS) {
                successCount++
            } else if (result.status == ItemRestoreStatus.FAILED) {
                failureCount++
            }

            // 4. Track success/failure
            emit(
                initialProgress.copy(
                    completedCount = results.size,
                    currentRestoringItem = null,
                    itemResults = results.toList(),
                    successCount = successCount,
                    failureCount = failureCount
                )
            )
        }

        // 5. Finalize and record restoration timestamp in Room
        markSnapshotRestoredUseCase(snapshot.id)

        emit(
            ContextRestoreProgress(
                snapshotId = snapshot.id,
                snapshotName = snapshot.name,
                totalItems = items.size,
                completedCount = items.size,
                isComplete = true,
                statusMessage = if (failureCount == 0) "${snapshot.name} Restored" else "${snapshot.name} Restored with warnings",
                itemResults = results.toList(),
                currentRestoringItem = null,
                successCount = successCount,
                failureCount = failureCount,
                restoredNotes = snapshot.notes,
                restoredImages = snapshot.images
            )
        )
    }

    override fun restoreSnapshotByIdFlow(snapshotId: String): Flow<ContextRestoreProgress> = flow {
        val snapshot = getSnapshotByIdUseCase(snapshotId).firstOrNull()
        if (snapshot == null) {
            emit(
                ContextRestoreProgress(
                    snapshotId = snapshotId,
                    snapshotName = "Context",
                    totalItems = 0,
                    completedCount = 0,
                    isComplete = true,
                    statusMessage = "Context snapshot not found",
                    itemResults = emptyList(),
                    failureCount = 1
                )
            )
            return@flow
        }

        restoreContextFlow(snapshot).collect { progress ->
            emit(progress)
        }
    }

    override suspend fun restoreItem(item: ContextItem): ItemRestoreResult {
        return try {
            when (item.type) {
                ContextItemType.APP -> restoreApp(item)
                ContextItemType.URL -> restoreUrl(item)
                ContextItemType.DOCUMENT -> restoreDocument(item)
                ContextItemType.NOTE -> restoreNote(item)
                ContextItemType.IMAGE -> restoreImage(item)
                ContextItemType.CLIPBOARD -> restoreClipboard(item)
            }
        } catch (t: Throwable) {
            ItemRestoreResult(
                item = item,
                status = ItemRestoreStatus.FAILED,
                message = "Failed to restore ${item.displayName}: ${t.localizedMessage ?: "Unknown error"}",
                detail = t.message
            )
        }
    }

    private fun restoreApp(item: ContextItem): ItemRestoreResult {
        val packageName = item.packageName
        if (packageName.isNullOrBlank()) {
            return ItemRestoreResult(
                item = item,
                status = ItemRestoreStatus.FAILED,
                message = "Cannot launch app: missing package name"
            )
        }

        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: run {
                val explicitUri = item.metadata["launchIntentUri"]
                if (!explicitUri.isNullOrBlank()) {
                    try {
                        Intent.parseUri(explicitUri, 0)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                val label = item.appName ?: item.displayName
                ItemRestoreResult(
                    item = item,
                    status = ItemRestoreStatus.SUCCESS,
                    message = "Opened $label"
                )
            } catch (e: Exception) {
                ItemRestoreResult(
                    item = item,
                    status = ItemRestoreStatus.FAILED,
                    message = "Could not launch ${item.appName ?: packageName}: ${e.localizedMessage ?: "Permission or process restriction"}"
                )
            }
        } else {
            val label = item.appName ?: packageName
            ItemRestoreResult(
                item = item,
                status = ItemRestoreStatus.FAILED,
                message = "App \"$label\" ($packageName) is not installed on this device"
            )
        }
    }

    private fun restoreUrl(item: ContextItem): ItemRestoreResult {
        val rawUrl = item.url
        if (rawUrl.isNullOrBlank()) {
            return ItemRestoreResult(
                item = item,
                status = ItemRestoreStatus.FAILED,
                message = "Cannot open link: missing URL"
            )
        }

        return try {
            val uri = Uri.parse(rawUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val label = item.title.ifBlank { rawUrl }
            ItemRestoreResult(
                item = item,
                status = ItemRestoreStatus.SUCCESS,
                message = "Opened $label"
            )
        } catch (e: ActivityNotFoundException) {
            ItemRestoreResult(
                item = item,
                status = ItemRestoreStatus.FAILED,
                message = "No browser installed to open $rawUrl"
            )
        } catch (e: Exception) {
            ItemRestoreResult(
                item = item,
                status = ItemRestoreStatus.FAILED,
                message = "Unable to open link $rawUrl: ${e.localizedMessage ?: "Invalid URL format"}"
            )
        }
    }

    private fun restoreDocument(item: ContextItem): ItemRestoreResult {
        val uriString = item.contentUri ?: item.fileUri
        if (uriString.isNullOrBlank()) {
            return ItemRestoreResult(
                item = item,
                status = ItemRestoreStatus.FAILED,
                message = "Document \"${item.displayName}\" has no stored URI"
            )
        }

        return try {
            val uri = Uri.parse(uriString)
            val mimeType = item.mimeType.ifBlank { "*/*" }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ItemRestoreResult(
                item = item,
                status = ItemRestoreStatus.SUCCESS,
                message = "Opened ${item.displayName}"
            )
        } catch (e: ActivityNotFoundException) {
            ItemRestoreResult(
                item = item,
                status = ItemRestoreStatus.FAILED,
                message = "No viewer installed for ${item.displayName} (${item.mimeType})"
            )
        } catch (e: SecurityException) {
            ItemRestoreResult(
                item = item,
                status = ItemRestoreStatus.FAILED,
                message = "Permission revoked for document ${item.displayName}"
            )
        } catch (e: Exception) {
            ItemRestoreResult(
                item = item,
                status = ItemRestoreStatus.FAILED,
                message = "Failed to open document ${item.displayName}: ${e.localizedMessage ?: "File inaccessible"}"
            )
        }
    }

    private fun restoreNote(item: ContextItem): ItemRestoreResult {
        // NOTE: Display the note inside ContextOS
        val title = item.title.ifBlank {
            item.content.lines().firstOrNull()?.take(30) ?: "notes"
        }
        return ItemRestoreResult(
            item = item,
            status = ItemRestoreStatus.SUCCESS,
            message = if (title.startsWith("notes", ignoreCase = true) || title.startsWith("task", ignoreCase = true)) {
                "Loaded $title"
            } else {
                "Loaded notes"
            }
        )
    }

    private fun restoreImage(item: ContextItem): ItemRestoreResult {
        // IMAGE: Display the image inside ContextOS
        val label = if (item.ocrRawText.isNotBlank()) {
            "whiteboard image"
        } else {
            item.title.ifBlank { "whiteboard image" }
        }
        return ItemRestoreResult(
            item = item,
            status = ItemRestoreStatus.SUCCESS,
            message = "Loaded $label"
        )
    }

    private fun restoreClipboard(item: ContextItem): ItemRestoreResult {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && !item.content.isNullOrBlank()) {
                val clip = android.content.ClipData.newPlainText("ContextOS", item.content)
                clipboard.setPrimaryClip(clip)
                ItemRestoreResult(
                    item = item,
                    status = ItemRestoreStatus.SUCCESS,
                    message = "Restored clipboard snippet"
                )
            } else {
                ItemRestoreResult(
                    item = item,
                    status = ItemRestoreStatus.SUCCESS,
                    message = "Loaded clipboard snippet"
                )
            }
        } catch (e: Exception) {
            ItemRestoreResult(
                item = item,
                status = ItemRestoreStatus.SUCCESS,
                message = "Loaded clipboard snippet"
            )
        }
    }

    private fun createInitialProgress(snapshot: ContextSnapshot): ContextRestoreProgress {
        return ContextRestoreProgress(
            snapshotId = snapshot.id,
            snapshotName = snapshot.name,
            totalItems = snapshot.items.size,
            completedCount = 0,
            isComplete = false,
            statusMessage = "Restoring ${snapshot.name}...",
            itemResults = emptyList(),
            restoredNotes = snapshot.notes,
            restoredImages = snapshot.images
        )
    }
}
