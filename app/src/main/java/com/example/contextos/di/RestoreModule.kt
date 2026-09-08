package com.example.contextos.di

import com.example.contextos.restore.ContextRestoreManager
import com.example.contextos.restore.ContextRestoreManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RestoreModule {

    @Binds
    @Singleton
    abstract fun bindContextRestoreManager(
        impl: ContextRestoreManagerImpl
    ): ContextRestoreManager
}
