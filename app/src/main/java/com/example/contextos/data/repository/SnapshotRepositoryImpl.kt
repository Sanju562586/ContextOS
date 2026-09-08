package com.example.contextos.data.repository

import com.example.contextos.data.local.dao.ContextItemDao
import com.example.contextos.data.local.dao.SnapshotDao
import com.example.contextos.data.mapper.toDomain
import com.example.contextos.data.mapper.toEntity
import com.example.contextos.data.sample.SampleData
import com.example.contextos.domain.repository.SnapshotRepository
import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotRepositoryImpl @Inject constructor(
    private val snapshotDao: SnapshotDao,
    private val contextItemDao: ContextItemDao
) : SnapshotRepository {

    override fun getSnapshots(): Flow<List<ContextSnapshot>> {
        return snapshotDao.getSnapshotsWithItems().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getSnapshotById(id: String): Flow<ContextSnapshot?> {
        return snapshotDao.getSnapshotWithItemsById(id).map { it?.toDomain() }
    }

    override suspend fun saveSnapshot(snapshot: ContextSnapshot) {
        val snapshotEntity = snapshot.toEntity()
        val itemEntities = snapshot.items.map { it.toEntity() }
        snapshotDao.upsertFullSnapshot(snapshotEntity, itemEntities)
    }

    override suspend fun deleteSnapshot(id: String) {
        snapshotDao.deleteSnapshotById(id)
    }

    override suspend fun markResumed(id: String, timestamp: Long) {
        snapshotDao.updateLastResumed(id, timestamp)
    }

    override suspend fun markRestored(id: String, timestamp: Long) {
        markResumed(id, timestamp)
    }

    override suspend fun togglePin(id: String, isPinned: Boolean) {
        snapshotDao.updatePinned(id, isPinned)
    }

    override fun getItemsForSnapshot(snapshotId: String): Flow<List<ContextItem>> {
        return contextItemDao.getItemsForSnapshot(snapshotId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveContextItem(item: ContextItem) {
        contextItemDao.insertItem(item.toEntity())
    }

    override suspend fun deleteContextItem(itemId: String) {
        contextItemDao.deleteItemById(itemId)
    }

    override suspend fun seedInitialData() {
        val existing = getSnapshots().firstOrNull()
        if (existing.isNullOrEmpty()) {
            for (sample in SampleData.getAllSampleSnapshots()) {
                saveSnapshot(sample)
            }
        }
    }
}
