package com.example.contextos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.contextos.data.local.entity.AiSummaryEntity
import com.example.contextos.data.local.entity.AppItemEntity
import com.example.contextos.data.local.entity.DocumentItemEntity
import com.example.contextos.data.local.entity.ImageArtifactEntity
import com.example.contextos.data.local.entity.LinkItemEntity
import com.example.contextos.data.local.entity.NoteItemEntity
import com.example.contextos.data.local.entity.SnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {

    @Transaction
    @Query("SELECT * FROM snapshots ORDER BY isPinned DESC, createdAt DESC")
    fun getSnapshotsWithDetails(): Flow<List<SnapshotWithDetails>>

    @Transaction
    @Query("SELECT * FROM snapshots WHERE id = :id")
    fun getSnapshotWithDetailsById(id: String): Flow<SnapshotWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<LinkItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(docs: List<DocumentItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<ImageArtifactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiSummary(summary: AiSummaryEntity)

    @Query("DELETE FROM app_items WHERE snapshotId = :snapshotId")
    suspend fun deleteAppsBySnapshotId(snapshotId: String)

    @Query("DELETE FROM link_items WHERE snapshotId = :snapshotId")
    suspend fun deleteLinksBySnapshotId(snapshotId: String)

    @Query("DELETE FROM document_items WHERE snapshotId = :snapshotId")
    suspend fun deleteDocumentsBySnapshotId(snapshotId: String)

    @Query("DELETE FROM note_items WHERE snapshotId = :snapshotId")
    suspend fun deleteNotesBySnapshotId(snapshotId: String)

    @Query("DELETE FROM image_artifacts WHERE snapshotId = :snapshotId")
    suspend fun deleteImagesBySnapshotId(snapshotId: String)

    @Query("DELETE FROM ai_summaries WHERE snapshotId = :snapshotId")
    suspend fun deleteAiSummaryBySnapshotId(snapshotId: String)

    @Query("DELETE FROM snapshots WHERE id = :id")
    suspend fun deleteSnapshotById(id: String)

    @Query("UPDATE snapshots SET lastRestoredAt = :timestamp WHERE id = :id")
    suspend fun updateLastRestored(id: String, timestamp: Long)

    @Query("UPDATE snapshots SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(id: String, isPinned: Boolean)

    @Transaction
    suspend fun upsertFullSnapshot(
        snapshot: SnapshotEntity,
        apps: List<AppItemEntity>,
        links: List<LinkItemEntity>,
        documents: List<DocumentItemEntity>,
        notes: List<NoteItemEntity>,
        images: List<ImageArtifactEntity>,
        aiSummary: AiSummaryEntity?
    ) {
        insertSnapshot(snapshot)
        deleteAppsBySnapshotId(snapshot.id)
        deleteLinksBySnapshotId(snapshot.id)
        deleteDocumentsBySnapshotId(snapshot.id)
        deleteNotesBySnapshotId(snapshot.id)
        deleteImagesBySnapshotId(snapshot.id)
        deleteAiSummaryBySnapshotId(snapshot.id)

        if (apps.isNotEmpty()) insertApps(apps)
        if (links.isNotEmpty()) insertLinks(links)
        if (documents.isNotEmpty()) insertDocuments(documents)
        if (notes.isNotEmpty()) insertNotes(notes)
        if (images.isNotEmpty()) insertImages(images)
        if (aiSummary != null) insertAiSummary(aiSummary)
    }
}
