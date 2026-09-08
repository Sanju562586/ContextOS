package com.example.contextos.di

import android.content.Context
import androidx.room.Room
import com.example.contextos.data.local.ContextOsDatabase
import com.example.contextos.data.local.dao.ContextItemDao
import com.example.contextos.data.local.dao.SnapshotDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideContextOsDatabase(
        @ApplicationContext context: Context
    ): ContextOsDatabase {
        return Room.databaseBuilder(
            context,
            ContextOsDatabase::class.java,
            ContextOsDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }

    @Provides
    fun provideSnapshotDao(database: ContextOsDatabase): SnapshotDao {
        return database.snapshotDao()
    }

    @Provides
    fun provideContextItemDao(database: ContextOsDatabase): ContextItemDao {
        return database.contextItemDao()
    }
}
