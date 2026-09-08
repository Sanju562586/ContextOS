package com.example.contextos.data.local.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.example.contextos.data.local.entity.ContextItemEntity
import com.example.contextos.data.local.entity.SnapshotEntity

/**
 * Room relation class bundling a [SnapshotEntity] with all its associated [ContextItemEntity] records.
 */
data class SnapshotWithItems(
    @Embedded val snapshot: SnapshotEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "snapshotId"
    )
    val items: List<ContextItemEntity> = emptyList()
)
