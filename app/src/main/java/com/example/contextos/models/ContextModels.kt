package com.example.contextos.models

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Supported types for a [ContextItem].
 */
@Serializable
enum class ContextItemType {
    APP,
    URL,
    DOCUMENT,
    NOTE,
    IMAGE,
    CLIPBOARD
}

/**
 * Domain model representing a captured item within a context.
 * Stores only metadata necessary for restoration and display.
 */
@Serializable
data class ContextItem(
    val id: String = UUID.randomUUID().toString(),
    val snapshotId: String,
    val type: ContextItemType,
    val packageName: String? = null,
    val appName: String? = null,
    val url: String? = null,
    val contentUri: String? = null,
    val fileUri: String? = null,
    val noteContent: String? = null,
    val imageUri: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val metadataJson: String? = null,
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Convenience accessors
    val title: String
        get() = metadata["title"] ?: appName ?: metadata["displayName"] ?: url ?: "Item"

    val displayName: String
        get() = metadata["displayName"] ?: appName ?: title

    val isPrimaryForSplitScreen: Boolean
        get() = metadata["isPrimaryForSplitScreen"]?.toBoolean() ?: false

    val mimeType: String
        get() = metadata["mimeType"] ?: "*/*"

    val fileSizeBytes: Long
        get() = metadata["fileSizeBytes"]?.toLongOrNull() ?: 0L

    val ocrRawText: String
        get() = metadata["ocrRawText"] ?: ""

    val isCompleted: Boolean
        get() = metadata["isCompleted"]?.toBoolean() ?: false

    val content: String
        get() = noteContent ?: ""

    companion object {
        fun createApp(
            snapshotId: String,
            packageName: String,
            appName: String,
            launchIntentUri: String? = null,
            isPrimaryForSplitScreen: Boolean = false,
            displayOrder: Int = 0,
            id: String = UUID.randomUUID().toString()
        ): ContextItem {
            val meta = mutableMapOf<String, String>()
            if (launchIntentUri != null) meta["launchIntentUri"] = launchIntentUri
            if (isPrimaryForSplitScreen) meta["isPrimaryForSplitScreen"] = "true"
            return ContextItem(
                id = id,
                snapshotId = snapshotId,
                type = ContextItemType.APP,
                packageName = packageName,
                appName = appName,
                metadata = meta,
                displayOrder = displayOrder
            )
        }

        fun createUrl(
            snapshotId: String,
            url: String,
            title: String = "",
            faviconUri: String? = null,
            displayOrder: Int = 0,
            id: String = UUID.randomUUID().toString()
        ): ContextItem {
            val meta = mutableMapOf<String, String>()
            if (title.isNotBlank()) meta["title"] = title
            if (faviconUri != null) meta["faviconUri"] = faviconUri
            return ContextItem(
                id = id,
                snapshotId = snapshotId,
                type = ContextItemType.URL,
                url = url,
                metadata = meta,
                displayOrder = displayOrder
            )
        }

        fun createDocument(
            snapshotId: String,
            contentUri: String,
            displayName: String,
            mimeType: String = "*/*",
            fileSizeBytes: Long = 0L,
            fileUri: String? = null,
            displayOrder: Int = 0,
            id: String = UUID.randomUUID().toString()
        ): ContextItem {
            val meta = mutableMapOf(
                "displayName" to displayName,
                "mimeType" to mimeType,
                "fileSizeBytes" to fileSizeBytes.toString()
            )
            return ContextItem(
                id = id,
                snapshotId = snapshotId,
                type = ContextItemType.DOCUMENT,
                contentUri = contentUri,
                fileUri = fileUri,
                metadata = meta,
                displayOrder = displayOrder
            )
        }

        fun createNote(
            snapshotId: String,
            content: String,
            isCompleted: Boolean = false,
            displayOrder: Int = 0,
            id: String = UUID.randomUUID().toString()
        ): ContextItem {
            val meta = mutableMapOf("isCompleted" to isCompleted.toString())
            return ContextItem(
                id = id,
                snapshotId = snapshotId,
                type = ContextItemType.NOTE,
                noteContent = content,
                metadata = meta,
                displayOrder = displayOrder
            )
        }

        fun createImage(
            snapshotId: String,
            imageUri: String,
            ocrRawText: String = "",
            fileUri: String? = null,
            displayOrder: Int = 0,
            id: String = UUID.randomUUID().toString()
        ): ContextItem {
            val meta = mutableMapOf<String, String>()
            if (ocrRawText.isNotBlank()) meta["ocrRawText"] = ocrRawText
            return ContextItem(
                id = id,
                snapshotId = snapshotId,
                type = ContextItemType.IMAGE,
                imageUri = imageUri,
                fileUri = fileUri ?: imageUri,
                metadata = meta,
                displayOrder = displayOrder
            )
        }

        fun createClipboard(
            snapshotId: String,
            content: String,
            mimeType: String = "text/plain",
            displayOrder: Int = 0,
            id: String = UUID.randomUUID().toString()
        ): ContextItem {
            val meta = mutableMapOf("mimeType" to mimeType)
            return ContextItem(
                id = id,
                snapshotId = snapshotId,
                type = ContextItemType.CLIPBOARD,
                noteContent = content,
                metadata = meta,
                displayOrder = displayOrder
            )
        }
    }
}

/**
 * Domain model representing a full context snapshot.
 */
@Serializable
data class ContextSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val aiSummary: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastResumedAt: Long? = null,
    val isPinned: Boolean = false,
    val colorHex: String = "#1E88E5",
    val items: List<ContextItem> = emptyList(),
    val nextActions: List<String> = emptyList()
) {
    // Backwards-compatibility aliases and grouped item helpers
    val lastRestoredAt: Long?
        get() = lastResumedAt

    val apps: List<ContextItem>
        get() = items.filter { it.type == ContextItemType.APP }

    val links: List<ContextItem>
        get() = items.filter { it.type == ContextItemType.URL }

    val documents: List<ContextItem>
        get() = items.filter { it.type == ContextItemType.DOCUMENT }

    val notes: List<ContextItem>
        get() = items.filter { it.type == ContextItemType.NOTE }

    val images: List<ContextItem>
        get() = items.filter { it.type == ContextItemType.IMAGE }

    val clipboards: List<ContextItem>
        get() = items.filter { it.type == ContextItemType.CLIPBOARD }
}

/**
 * Structured AI summary representation for AI engine integrations.
 */
@Serializable
data class AiSummary(
    val id: String = UUID.randomUUID().toString(),
    val snapshotId: String = "",
    val executiveBrief: String = "",
    val nextActions: List<String> = emptyList(),
    val modelVersion: String = "gemini-nano",
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Image artifact representation for camera and OCR engine integrations.
 */
@Serializable
data class ImageArtifact(
    val id: String = UUID.randomUUID().toString(),
    val snapshotId: String = "",
    val localFilePath: String = "",
    val ocrRawText: String = "",
    val capturedAt: Long = System.currentTimeMillis()
)
