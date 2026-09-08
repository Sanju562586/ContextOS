package com.example.contextos.data.mapper

import com.example.contextos.data.local.dao.SnapshotWithItems
import com.example.contextos.data.local.entity.ContextItemEntity
import com.example.contextos.data.local.entity.SnapshotEntity
import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextItemType
import com.example.contextos.models.ContextSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SnapshotMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun toDomain(relation: SnapshotWithItems): ContextSnapshot {
        return ContextSnapshot(
            id = relation.snapshot.id,
            name = relation.snapshot.name,
            description = relation.snapshot.description,
            aiSummary = relation.snapshot.aiSummary,
            createdAt = relation.snapshot.createdAt,
            updatedAt = relation.snapshot.updatedAt,
            lastResumedAt = relation.snapshot.lastResumedAt,
            isPinned = relation.snapshot.isPinned,
            colorHex = relation.snapshot.colorHex,
            items = relation.items.map { toDomain(it) },
            nextActions = relation.snapshot.nextActions
        )
    }

    fun toEntity(snapshot: ContextSnapshot): SnapshotEntity {
        return SnapshotEntity(
            id = snapshot.id,
            name = snapshot.name,
            description = snapshot.description,
            aiSummary = snapshot.aiSummary,
            createdAt = snapshot.createdAt,
            updatedAt = snapshot.updatedAt,
            lastResumedAt = snapshot.lastResumedAt,
            isPinned = snapshot.isPinned,
            colorHex = snapshot.colorHex,
            nextActions = snapshot.nextActions
        )
    }

    fun toDomain(entity: ContextItemEntity): ContextItem {
        val metaMap = if (!entity.metadataJson.isNullOrBlank()) {
            try {
                json.decodeFromString<Map<String, String>>(entity.metadataJson)
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }

        val itemType = try {
            ContextItemType.valueOf(entity.type)
        } catch (e: Exception) {
            ContextItemType.APP
        }

        return ContextItem(
            id = entity.id,
            snapshotId = entity.snapshotId,
            type = itemType,
            packageName = entity.packageName,
            appName = entity.appName,
            url = entity.url,
            contentUri = entity.contentUri,
            fileUri = entity.fileUri,
            noteContent = entity.noteContent,
            imageUri = entity.imageUri,
            metadata = metaMap,
            metadataJson = entity.metadataJson,
            displayOrder = entity.displayOrder,
            createdAt = entity.createdAt
        )
    }

    fun toEntity(item: ContextItem): ContextItemEntity {
        val jsonStr = when {
            item.metadataJson != null -> item.metadataJson
            item.metadata.isNotEmpty() -> json.encodeToString(item.metadata)
            else -> null
        }

        return ContextItemEntity(
            id = item.id,
            snapshotId = item.snapshotId,
            type = item.type.name,
            packageName = item.packageName,
            appName = item.appName,
            url = item.url,
            contentUri = item.contentUri,
            fileUri = item.fileUri,
            noteContent = item.noteContent,
            imageUri = item.imageUri,
            metadataJson = jsonStr,
            displayOrder = item.displayOrder,
            createdAt = item.createdAt
        )
    }
}

fun SnapshotWithItems.toDomain(): ContextSnapshot = SnapshotMapper.toDomain(this)
fun ContextSnapshot.toEntity(): SnapshotEntity = SnapshotMapper.toEntity(this)
fun ContextItemEntity.toDomain(): ContextItem = SnapshotMapper.toDomain(this)
fun ContextItem.toEntity(): ContextItemEntity = SnapshotMapper.toEntity(this)
