package com.example.contextos.data.repository

import com.example.contextos.data.local.dao.SnapshotDao
import com.example.contextos.data.local.dao.SnapshotWithDetails
import com.example.contextos.data.local.entity.AiSummaryEntity
import com.example.contextos.data.local.entity.AppItemEntity
import com.example.contextos.data.local.entity.DocumentItemEntity
import com.example.contextos.data.local.entity.ImageArtifactEntity
import com.example.contextos.data.local.entity.LinkItemEntity
import com.example.contextos.data.local.entity.NoteItemEntity
import com.example.contextos.data.local.entity.SnapshotEntity
import com.example.contextos.domain.repository.SnapshotRepository
import com.example.contextos.models.AiSummary
import com.example.contextos.models.AppItem
import com.example.contextos.models.ContextSnapshot
import com.example.contextos.models.DocumentItem
import com.example.contextos.models.ImageArtifact
import com.example.contextos.models.LinkItem
import com.example.contextos.models.NoteItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotRepositoryImpl @Inject constructor(
    private val snapshotDao: SnapshotDao
) : SnapshotRepository {

    override fun getSnapshots(): Flow<List<ContextSnapshot>> {
        return snapshotDao.getSnapshotsWithDetails().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getSnapshotById(id: String): Flow<ContextSnapshot?> {
        return snapshotDao.getSnapshotWithDetailsById(id).map { it?.toDomain() }
    }

    override suspend fun saveSnapshot(snapshot: ContextSnapshot) {
        val snapshotEntity = snapshot.toEntity()
        val appEntities = snapshot.apps.map { it.toEntity() }
        val linkEntities = snapshot.links.map { it.toEntity() }
        val docEntities = snapshot.documents.map { it.toEntity() }
        val noteEntities = snapshot.notes.map { it.toEntity() }
        val imageEntities = snapshot.images.map { it.toEntity() }
        val aiSummaryEntity = snapshot.aiSummary?.toEntity()

        snapshotDao.upsertFullSnapshot(
            snapshot = snapshotEntity,
            apps = appEntities,
            links = linkEntities,
            documents = docEntities,
            notes = noteEntities,
            images = imageEntities,
            aiSummary = aiSummaryEntity
        )
    }

    override suspend fun deleteSnapshot(id: String) {
        snapshotDao.deleteSnapshotById(id)
    }

    override suspend fun markRestored(id: String, timestamp: Long) {
        snapshotDao.updateLastRestored(id, timestamp)
    }

    override suspend fun togglePin(id: String, isPinned: Boolean) {
        snapshotDao.updatePinned(id, isPinned)
    }

    // Mapping Functions
    private fun SnapshotWithDetails.toDomain(): ContextSnapshot {
        return ContextSnapshot(
            id = snapshot.id,
            name = snapshot.name,
            description = snapshot.description,
            createdAt = snapshot.createdAt,
            lastRestoredAt = snapshot.lastRestoredAt,
            isPinned = snapshot.isPinned,
            colorHex = snapshot.colorHex,
            apps = apps.map { it.toDomain() },
            links = links.map { it.toDomain() },
            documents = documents.map { it.toDomain() },
            notes = notes.map { it.toDomain() },
            images = images.map { it.toDomain() },
            aiSummary = aiSummary?.toDomain()
        )
    }

    private fun ContextSnapshot.toEntity(): SnapshotEntity {
        return SnapshotEntity(
            id = id,
            name = name,
            description = description,
            createdAt = createdAt,
            lastRestoredAt = lastRestoredAt,
            isPinned = isPinned,
            colorHex = colorHex
        )
    }

    private fun AppItemEntity.toDomain() = AppItem(
        id = id,
        snapshotId = snapshotId,
        packageName = packageName,
        appName = appName,
        launchIntentUri = launchIntentUri,
        displayOrder = displayOrder,
        isPrimaryForSplitScreen = isPrimaryForSplitScreen
    )

    private fun AppItem.toEntity() = AppItemEntity(
        id = id,
        snapshotId = snapshotId,
        packageName = packageName,
        appName = appName,
        launchIntentUri = launchIntentUri,
        displayOrder = displayOrder,
        isPrimaryForSplitScreen = isPrimaryForSplitScreen
    )

    private fun LinkItemEntity.toDomain() = LinkItem(
        id = id,
        snapshotId = snapshotId,
        url = url,
        title = title,
        faviconUri = faviconUri
    )

    private fun LinkItem.toEntity() = LinkItemEntity(
        id = id,
        snapshotId = snapshotId,
        url = url,
        title = title,
        faviconUri = faviconUri
    )

    private fun DocumentItemEntity.toDomain() = DocumentItem(
        id = id,
        snapshotId = snapshotId,
        contentUri = contentUri,
        displayName = displayName,
        mimeType = mimeType,
        fileSizeBytes = fileSizeBytes
    )

    private fun DocumentItem.toEntity() = DocumentItemEntity(
        id = id,
        snapshotId = snapshotId,
        contentUri = contentUri,
        displayName = displayName,
        mimeType = mimeType,
        fileSizeBytes = fileSizeBytes
    )

    private fun NoteItemEntity.toDomain() = NoteItem(
        id = id,
        snapshotId = snapshotId,
        content = content,
        isCompleted = isCompleted,
        createdAt = createdAt
    )

    private fun NoteItem.toEntity() = NoteItemEntity(
        id = id,
        snapshotId = snapshotId,
        content = content,
        isCompleted = isCompleted,
        createdAt = createdAt
    )

    private fun ImageArtifactEntity.toDomain() = ImageArtifact(
        id = id,
        snapshotId = snapshotId,
        localFilePath = localFilePath,
        ocrRawText = ocrRawText,
        capturedAt = capturedAt
    )

    private fun ImageArtifact.toEntity() = ImageArtifactEntity(
        id = id,
        snapshotId = snapshotId,
        localFilePath = localFilePath,
        ocrRawText = ocrRawText,
        capturedAt = capturedAt
    )

    private fun AiSummaryEntity.toDomain() = AiSummary(
        id = id,
        snapshotId = snapshotId,
        executiveBrief = executiveBrief,
        nextActions = nextActions,
        modelVersion = modelVersion,
        generatedAt = generatedAt
    )

    private fun AiSummary.toEntity() = AiSummaryEntity(
        id = id,
        snapshotId = snapshotId,
        executiveBrief = executiveBrief,
        nextActions = nextActions,
        modelVersion = modelVersion,
        generatedAt = generatedAt
    )
}
