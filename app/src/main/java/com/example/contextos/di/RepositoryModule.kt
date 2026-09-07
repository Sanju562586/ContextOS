package com.example.contextos.di

import com.example.contextos.data.repository.SnapshotRepositoryImpl
import com.example.contextos.domain.repository.SnapshotRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSnapshotRepository(
        impl: SnapshotRepositoryImpl
    ): SnapshotRepository
}
