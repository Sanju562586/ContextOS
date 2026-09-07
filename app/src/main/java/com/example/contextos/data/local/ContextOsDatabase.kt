package com.example.contextos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.contextos.data.local.dao.SnapshotDao
import com.example.contextos.data.local.entity.AiSummaryEntity
import com.example.contextos.data.local.entity.AppItemEntity
import com.example.contextos.data.local.entity.DocumentItemEntity
import com.example.contextos.data.local.entity.ImageArtifactEntity
import com.example.contextos.data.local.entity.LinkItemEntity
import com.example.contextos.data.local.entity.NoteItemEntity
import com.example.contextos.data.local.entity.SnapshotEntity

@Database(
    entities = [
        SnapshotEntity::class,
        AppItemEntity::class,
        DocumentItemEntity::class,
        LinkItemEntity::class,
        NoteItemEntity::class,
        ImageArtifactEntity::class,
        AiSummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ContextOsDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao

    companion object {
        const val DATABASE_NAME = "contextos.db"
    }
}
