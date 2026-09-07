package com.example.contextos.data.local.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.example.contextos.data.local.entity.AiSummaryEntity
import com.example.contextos.data.local.entity.AppItemEntity
import com.example.contextos.data.local.entity.DocumentItemEntity
import com.example.contextos.data.local.entity.ImageArtifactEntity
import com.example.contextos.data.local.entity.LinkItemEntity
import com.example.contextos.data.local.entity.NoteItemEntity
import com.example.contextos.data.local.entity.SnapshotEntity

data class SnapshotWithDetails(
    @Embedded val snapshot: SnapshotEntity,
    @Relation(parentColumn = "id", entityColumn = "snapshotId")
    val apps: List<AppItemEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "snapshotId")
    val links: List<LinkItemEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "snapshotId")
    val documents: List<DocumentItemEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "snapshotId")
    val notes: List<NoteItemEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "snapshotId")
    val images: List<ImageArtifactEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "snapshotId")
    val aiSummary: AiSummaryEntity? = null
)
