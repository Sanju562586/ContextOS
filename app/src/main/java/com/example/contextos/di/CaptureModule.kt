package com.example.contextos.di

import com.example.contextos.capture.ContextCaptureManager
import com.example.contextos.capture.ContextCaptureManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureModule {

    @Binds
    @Singleton
    abstract fun bindContextCaptureManager(
        impl: ContextCaptureManagerImpl
    ): ContextCaptureManager
}
