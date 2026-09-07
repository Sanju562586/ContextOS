package com.example.contextos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val createdAt: Long,
    val lastRestoredAt: Long?,
    val isPinned: Boolean,
    val colorHex: String
)

@Entity(
    tableName = "app_items",
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["snapshotId"])]
)
data class AppItemEntity(
    @PrimaryKey
    val id: String,
    val snapshotId: String,
    val packageName: String,
    val appName: String,
    val launchIntentUri: String?,
    val displayOrder: Int,
    val isPrimaryForSplitScreen: Boolean
)

@Entity(
    tableName = "link_items",
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["snapshotId"])]
)
data class LinkItemEntity(
    @PrimaryKey
    val id: String,
    val snapshotId: String,
    val url: String,
    val title: String,
    val faviconUri: String?
)

@Entity(
    tableName = "document_items",
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["snapshotId"])]
)
data class DocumentItemEntity(
    @PrimaryKey
    val id: String,
    val snapshotId: String,
    val contentUri: String,
    val displayName: String,
    val mimeType: String,
    val fileSizeBytes: Long
)

@Entity(
    tableName = "note_items",
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["snapshotId"])]
)
data class NoteItemEntity(
    @PrimaryKey
    val id: String,
    val snapshotId: String,
    val content: String,
    val isCompleted: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "image_artifacts",
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["snapshotId"])]
)
data class ImageArtifactEntity(
    @PrimaryKey
    val id: String,
    val snapshotId: String,
    val localFilePath: String,
    val ocrRawText: String,
    val capturedAt: Long
)

@Entity(
    tableName = "ai_summaries",
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["snapshotId"])]
)
data class AiSummaryEntity(
    @PrimaryKey
    val id: String,
    val snapshotId: String,
    val executiveBrief: String,
    val nextActions: List<String>,
    val modelVersion: String,
    val generatedAt: Long
)
