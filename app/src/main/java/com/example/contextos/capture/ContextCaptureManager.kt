package com.example.contextos.capture

import com.example.contextos.models.ContextSnapshot

interface ContextCaptureManager {
    suspend fun captureCurrentContext(name: String, description: String = ""): ContextSnapshot
}
