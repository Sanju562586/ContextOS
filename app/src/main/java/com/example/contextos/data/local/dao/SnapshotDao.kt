package com.example.contextos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.contextos.data.local.entity.ContextItemEntity
import com.example.contextos.data.local.entity.SnapshotEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Context Snapshots and their relations.
 */
@Dao
interface SnapshotDao {

    @Transaction
    @Query("SELECT * FROM snapshots ORDER BY isPinned DESC, updatedAt DESC, createdAt DESC")
    fun getSnapshotsWithItems(): Flow<List<SnapshotWithItems>>

    @Transaction
    @Query("SELECT * FROM snapshots WHERE id = :id")
    fun getSnapshotWithItemsById(id: String): Flow<SnapshotWithItems?>

    @Query("SELECT * FROM snapshots WHERE id = :id")
    suspend fun getSnapshotEntityById(id: String): SnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(snapshots: List<SnapshotEntity>)

    @Update
    suspend fun updateSnapshot(snapshot: SnapshotEntity)

    @Query("DELETE FROM snapshots WHERE id = :id")
    suspend fun deleteSnapshotById(id: String): Int

    @Query("DELETE FROM snapshots")
    suspend fun deleteAllSnapshots(): Int

    @Query("UPDATE snapshots SET lastResumedAt = :timestamp, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateLastResumed(id: String, timestamp: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE snapshots SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePinned(id: String, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContextItems(items: List<ContextItemEntity>)

    @Query("DELETE FROM context_items WHERE snapshotId = :snapshotId")
    suspend fun deleteContextItemsBySnapshotId(snapshotId: String): Int

    @Transaction
    suspend fun upsertFullSnapshot(
        snapshot: SnapshotEntity,
        items: List<ContextItemEntity>
    ) {
        insertSnapshot(snapshot)
        deleteContextItemsBySnapshotId(snapshot.id)
        if (items.isNotEmpty()) {
            insertContextItems(items)
        }
    }
}
