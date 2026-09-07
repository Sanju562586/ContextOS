package com.example.contextos.camera

import com.example.contextos.models.ImageArtifact

interface CameraCaptureEngine {
    suspend fun processCapturedImage(filePath: String): ImageArtifact
}
