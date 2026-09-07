package com.example.contextos.voice

import kotlinx.coroutines.flow.StateFlow

interface VoiceCommandManager {
    val listeningState: StateFlow<VoiceState>
    fun startListening()
    fun stopListening()
}

sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class Processing(val spokenText: String) : VoiceState
    data class CommandRecognized(val action: String, val projectName: String) : VoiceState
    data class Error(val message: String) : VoiceState
}
