package com.example.contextos.models

import kotlinx.serialization.Serializable

@Serializable
data class ContextSnapshot(
    val id: String,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastRestoredAt: Long? = null,
    val isPinned: Boolean = false,
    val colorHex: String = "#4285F4",
    val apps: List<AppItem> = emptyList(),
    val links: List<LinkItem> = emptyList(),
    val documents: List<DocumentItem> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val images: List<ImageArtifact> = emptyList(),
    val aiSummary: AiSummary? = null
)

@Serializable
data class AppItem(
    val id: String,
    val snapshotId: String,
    val packageName: String,
    val appName: String,
    val launchIntentUri: String? = null,
    val displayOrder: Int = 0,
    val isPrimaryForSplitScreen: Boolean = false
)

@Serializable
data class LinkItem(
    val id: String,
    val snapshotId: String,
    val url: String,
    val title: String = "",
    val faviconUri: String? = null
)

@Serializable
data class DocumentItem(
    val id: String,
    val snapshotId: String,
    val contentUri: String,
    val displayName: String,
    val mimeType: String = "*/*",
    val fileSizeBytes: Long = 0L
)

@Serializable
data class NoteItem(
    val id: String,
    val snapshotId: String,
    val content: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ImageArtifact(
    val id: String,
    val snapshotId: String,
    val localFilePath: String,
    val ocrRawText: String = "",
    val capturedAt: Long = System.currentTimeMillis()
)

@Serializable
data class AiSummary(
    val id: String,
    val snapshotId: String,
    val executiveBrief: String,
    val nextActions: List<String> = emptyList(),
    val modelVersion: String = "gemini-nano",
    val generatedAt: Long = System.currentTimeMillis()
)
