package com.example.contextos.di

import com.example.contextos.voice.DeterministicVoiceCommandProcessor
import com.example.contextos.voice.VoiceCommandManager
import com.example.contextos.voice.VoiceCommandManagerImpl
import com.example.contextos.voice.VoiceCommandProcessor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {

    @Binds
    @Singleton
    abstract fun bindVoiceCommandProcessor(
        impl: DeterministicVoiceCommandProcessor
    ): VoiceCommandProcessor

    @Binds
    @Singleton
    abstract fun bindVoiceCommandManager(
        impl: VoiceCommandManagerImpl
    ): VoiceCommandManager
}
