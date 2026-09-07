package com.example.contextos.restore

import com.example.contextos.models.ContextSnapshot

interface ContextRestoreManager {
    suspend fun restoreContext(snapshot: ContextSnapshot): RestoreResult
}

sealed interface RestoreResult {
    data class Success(val launchedAppsCount: Int, val openedLinksCount: Int, val openedDocsCount: Int) : RestoreResult
    data class Partial(val message: String, val launchedAppsCount: Int) : RestoreResult
    data class Failure(val throwable: Throwable) : RestoreResult
}
