package com.example.contextos.ai

import com.example.contextos.models.AiSummary
import com.example.contextos.models.ContextSnapshot

interface AiSummaryEngine {
    suspend fun generateContextSummary(snapshot: ContextSnapshot): AiSummary
}
