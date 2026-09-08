package com.example.contextos.ui.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contextos.capture.ContextCaptureManager
import com.example.contextos.capture.ContextCaptureResult
import com.example.contextos.capture.modules.AppCaptureModule
import com.example.contextos.capture.modules.AppMetadata
import com.example.contextos.capture.modules.DocumentCaptureModule
import com.example.contextos.capture.modules.NoteCaptureModule
import com.example.contextos.capture.modules.UrlCaptureModule
import com.example.contextos.domain.usecase.SaveSnapshotUseCase
import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextItemType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class CaptureStep {
    INPUT_NAME,
    REVIEW_ITEMS,
    SAVING
}

data class CaptureUiState(
    val step: CaptureStep = CaptureStep.INPUT_NAME,
    val snapshotId: String = UUID.randomUUID().toString(),
    val contextName: String = "",
    val contextDescription: String = "",
    val isCapturing: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val detectedApps: List<ContextItem> = emptyList(),
    val capturedClipboard: ContextItem? = null,
    val urls: List<ContextItem> = emptyList(),
    val documents: List<ContextItem> = emptyList(),
    val notes: List<ContextItem> = emptyList(),
    val availableApps: List<AppMetadata> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errorMessage: String? = null
) {
    val allItems: List<ContextItem>
        get() = detectedApps + urls + documents + notes + listOfNotNull(capturedClipboard)
}

sealed interface CaptureEvent {
    data class SnapshotSaved(val snapshotId: String, val name: String) : CaptureEvent
    data class ShowToast(val message: String) : CaptureEvent
}

@HiltViewModel
class CaptureContextViewModel @Inject constructor(
    private val captureManager: ContextCaptureManager,
    private val appCaptureModule: AppCaptureModule,
    private val documentCaptureModule: DocumentCaptureModule,
    private val urlCaptureModule: UrlCaptureModule,
    private val noteCaptureModule: NoteCaptureModule,
    private val saveSnapshotUseCase: SaveSnapshotUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CaptureEvent>()
    val events: SharedFlow<CaptureEvent> = _events.asSharedFlow()

    init {
        checkPermissionsAndLoadApps()
    }

    fun checkPermissionsAndLoadApps() {
        val hasAccess = captureManager.hasUsageAccess()
        val installedApps = captureManager.getAvailableApps()
        _uiState.update {
            it.copy(
                hasUsageAccess = hasAccess,
                availableApps = installedApps
            )
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(contextName = name, errorMessage = null) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(contextDescription = description) }
    }

    fun startCapture() {
        val currentName = _uiState.value.contextName.trim()
        if (currentName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a context name (e.g. Project Alpha)") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true, errorMessage = null) }

            val snapshotId = _uiState.value.snapshotId
            val result: ContextCaptureResult = captureManager.captureAvailableContext(
                snapshotId = snapshotId,
                includeClipboard = true,
                includeRecentApps = true
            )

            _uiState.update { state ->
                state.copy(
                    isCapturing = false,
                    step = CaptureStep.REVIEW_ITEMS,
                    detectedApps = result.detectedApps,
                    capturedClipboard = result.capturedClipboard,
                    warnings = result.warnings
                )
            }
        }
    }

    fun toggleAppSelection(app: AppMetadata) {
        _uiState.update { state ->
            val existing = state.detectedApps.firstOrNull { it.packageName == app.packageName }
            val updatedApps = if (existing != null) {
                state.detectedApps - existing
            } else {
                val isSplitScreen = state.detectedApps.size < 2
                val newItem = appCaptureModule.createAppItem(
                    snapshotId = state.snapshotId,
                    packageName = app.packageName,
                    appName = app.appName,
                    isPrimaryForSplitScreen = isSplitScreen,
                    displayOrder = state.detectedApps.size
                )
                state.detectedApps + newItem
            }
            state.copy(detectedApps = updatedApps)
        }
    }

    fun removeDetectedApp(packageName: String) {
        _uiState.update { state ->
            state.copy(detectedApps = state.detectedApps.filterNot { it.packageName == packageName })
        }
    }

    fun dismissClipboard() {
        _uiState.update { it.copy(capturedClipboard = null) }
    }

    fun addUrl(url: String, title: String = "") {
        val item = urlCaptureModule.captureUrl(
            snapshotId = _uiState.value.snapshotId,
            rawUrl = url,
            title = title,
            displayOrder = _uiState.value.urls.size
        )
        if (item != null) {
            _uiState.update { it.copy(urls = it.urls + item) }
        }
    }

    fun removeUrl(itemId: String) {
        _uiState.update { state ->
            state.copy(urls = state.urls.filterNot { it.id == itemId })
        }
    }

    fun addDocuments(uris: List<Uri>) {
        val newDocs = documentCaptureModule.captureDocuments(
            snapshotId = _uiState.value.snapshotId,
            uris = uris,
            startOrder = _uiState.value.documents.size
        )
        _uiState.update { it.copy(documents = it.documents + newDocs) }
    }

    fun removeDocument(itemId: String) {
        _uiState.update { state ->
            state.copy(documents = state.documents.filterNot { it.id == itemId })
        }
    }

    fun addNote(content: String) {
        val item = noteCaptureModule.captureNote(
            snapshotId = _uiState.value.snapshotId,
            content = content,
            displayOrder = _uiState.value.notes.size
        )
        if (item != null) {
            _uiState.update { it.copy(notes = it.notes + item) }
        }
    }

    fun removeNote(itemId: String) {
        _uiState.update { state ->
            state.copy(notes = state.notes.filterNot { it.id == itemId })
        }
    }

    fun confirmAndSave() {
        val state = _uiState.value
        val name = state.contextName.trim().ifBlank { "Untitled Context" }

        viewModelScope.launch {
            _uiState.update { it.copy(step = CaptureStep.SAVING) }

            val captureResult = ContextCaptureResult(
                snapshotId = state.snapshotId,
                detectedApps = state.detectedApps,
                capturedClipboard = state.capturedClipboard,
                capturedUrls = state.urls,
                selectedDocuments = state.documents,
                notes = state.notes,
                warnings = state.warnings
            )

            val snapshot = captureResult.toSnapshot(
                name = name,
                description = state.contextDescription.trim(),
                itemsToInclude = state.allItems
            )

            saveSnapshotUseCase(snapshot)

            _events.emit(CaptureEvent.SnapshotSaved(snapshot.id, snapshot.name))
        }
    }
}
