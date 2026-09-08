package com.example.contextos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.contextos.data.local.dao.ContextItemDao
import com.example.contextos.data.local.dao.SnapshotDao
import com.example.contextos.data.local.entity.ContextItemEntity
import com.example.contextos.data.local.entity.SnapshotEntity

@Database(
    entities = [
        SnapshotEntity::class,
        ContextItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ContextOsDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
    abstract fun contextItemDao(): ContextItemDao

    companion object {
        const val DATABASE_NAME = "contextos.db"
    }
}
