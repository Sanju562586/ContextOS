package com.example.contextos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.contextos.data.local.entity.ContextItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for individual ContextItems.
 */
@Dao
interface ContextItemDao {

    @Query("SELECT * FROM context_items WHERE snapshotId = :snapshotId ORDER BY displayOrder ASC, createdAt ASC")
    fun getItemsForSnapshot(snapshotId: String): Flow<List<ContextItemEntity>>

    @Query("SELECT * FROM context_items WHERE snapshotId = :snapshotId AND type = :type ORDER BY displayOrder ASC")
    fun getItemsByType(snapshotId: String, type: String): Flow<List<ContextItemEntity>>

    @Query("SELECT * FROM context_items WHERE id = :id")
    suspend fun getItemById(id: String): ContextItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ContextItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ContextItemEntity>)

    @Update
    suspend fun updateItem(item: ContextItemEntity)

    @Query("DELETE FROM context_items WHERE id = :id")
    suspend fun deleteItemById(id: String): Int

    @Query("DELETE FROM context_items WHERE snapshotId = :snapshotId")
    suspend fun deleteItemsBySnapshotId(snapshotId: String): Int
}
