package com.example.contextos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a saved context snapshot.
 */
@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val aiSummary: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastResumedAt: Long?,
    val isPinned: Boolean = false,
    val colorHex: String = "#1E88E5",
    val nextActions: List<String> = emptyList()
)

/**
 * Room entity representing an individual item within a context snapshot.
 * Maintains a Foreign Key with CASCADE delete to [SnapshotEntity].
 */
@Entity(
    tableName = "context_items",
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["snapshotId"]),
        Index(value = ["type"]),
        Index(value = ["snapshotId", "displayOrder"])
    ]
)
data class ContextItemEntity(
    @PrimaryKey
    val id: String,
    val snapshotId: String,
    val type: String,
    val packageName: String? = null,
    val appName: String? = null,
    val url: String? = null,
    val contentUri: String? = null,
    val fileUri: String? = null,
    val noteContent: String? = null,
    val imageUri: String? = null,
    val metadataJson: String? = null,
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
