package com.example.contextos.ui.detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contextos.capture.modules.AppCaptureModule
import com.example.contextos.capture.modules.AppMetadata
import com.example.contextos.capture.modules.DocumentCaptureModule
import com.example.contextos.capture.modules.ImageCaptureModule
import com.example.contextos.capture.modules.NoteCaptureModule
import com.example.contextos.capture.modules.UrlCaptureModule
import com.example.contextos.domain.usecase.DeleteContextItemUseCase
import com.example.contextos.domain.usecase.GetSnapshotByIdUseCase
import com.example.contextos.domain.usecase.MarkSnapshotRestoredUseCase
import com.example.contextos.domain.usecase.SaveContextItemUseCase
import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextSnapshot
import com.example.contextos.restore.ContextRestoreManager
import com.example.contextos.restore.ContextRestoreProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ContextDetailUiState {
    data object Loading : ContextDetailUiState
    data class Success(val snapshot: ContextSnapshot) : ContextDetailUiState
    data object NotFound : ContextDetailUiState
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContextDetailViewModel @Inject constructor(
    private val getSnapshotByIdUseCase: GetSnapshotByIdUseCase,
    private val markSnapshotRestoredUseCase: MarkSnapshotRestoredUseCase,
    private val saveContextItemUseCase: SaveContextItemUseCase,
    private val deleteContextItemUseCase: DeleteContextItemUseCase,
    private val appCaptureModule: AppCaptureModule,
    private val documentCaptureModule: DocumentCaptureModule,
    private val urlCaptureModule: UrlCaptureModule,
    private val noteCaptureModule: NoteCaptureModule,
    private val imageCaptureModule: ImageCaptureModule,
    private val contextRestoreManager: ContextRestoreManager
) : ViewModel() {

    private val _snapshotId = MutableStateFlow<String?>(null)
    val currentSnapshotId: String? get() = _snapshotId.value

    private val _availableApps = MutableStateFlow<List<AppMetadata>>(emptyList())
    val availableApps: StateFlow<List<AppMetadata>> = _availableApps.asStateFlow()

    private val _restoreProgress = MutableStateFlow<ContextRestoreProgress?>(null)
    val restoreProgress: StateFlow<ContextRestoreProgress?> = _restoreProgress.asStateFlow()

    private var pendingCameraFilePath: String? = null

    val uiState: StateFlow<ContextDetailUiState> = _snapshotId
        .filterNotNull()
        .flatMapLatest { id ->
            getSnapshotByIdUseCase(id).map { snapshot ->
                if (snapshot != null) {
                    ContextDetailUiState.Success(snapshot)
                } else {
                    ContextDetailUiState.NotFound
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ContextDetailUiState.Loading
        )

    fun loadSnapshot(id: String) {
        _snapshotId.value = id
        loadAvailableApps()
    }

    fun loadAvailableApps() {
        viewModelScope.launch {
            _availableApps.value = appCaptureModule.getInstalledLauncherApps()
        }
    }

    fun restore(snapshot: ContextSnapshot) {
        viewModelScope.launch {
            contextRestoreManager.restoreContextFlow(snapshot).collect { progress ->
                _restoreProgress.value = progress
            }
        }
    }

    fun restore(snapshotId: String) {
        viewModelScope.launch {
            contextRestoreManager.restoreSnapshotByIdFlow(snapshotId).collect { progress ->
                _restoreProgress.value = progress
            }
        }
    }

    fun dismissRestore() {
        _restoreProgress.value = null
    }

    fun addApp(packageName: String, appName: String) {
        val snapshotId = _snapshotId.value ?: return
        viewModelScope.launch {
            val item = appCaptureModule.createAppItem(
                snapshotId = snapshotId,
                packageName = packageName,
                appName = appName,
                isPrimaryForSplitScreen = false
            )
            saveContextItemUseCase(item)
        }
    }

    fun addUrl(rawUrl: String, title: String = "") {
        val snapshotId = _snapshotId.value ?: return
        viewModelScope.launch {
            val item = urlCaptureModule.captureUrl(
                snapshotId = snapshotId,
                rawUrl = rawUrl,
                title = title
            )
            if (item != null) {
                saveContextItemUseCase(item)
            }
        }
    }

    fun addDocuments(uris: List<Uri>) {
        val snapshotId = _snapshotId.value ?: return
        viewModelScope.launch {
            val items = documentCaptureModule.captureDocuments(
                snapshotId = snapshotId,
                uris = uris
            )
            items.forEach { saveContextItemUseCase(it) }
        }
    }

    fun addNote(content: String) {
        val snapshotId = _snapshotId.value ?: return
        viewModelScope.launch {
            val item = noteCaptureModule.captureNote(
                snapshotId = snapshotId,
                content = content
            )
            if (item != null) {
                saveContextItemUseCase(item)
            }
        }
    }

    fun addImageFromUri(uri: Uri) {
        val snapshotId = _snapshotId.value ?: return
        viewModelScope.launch {
            val item = imageCaptureModule.captureImageFromUri(
                snapshotId = snapshotId,
                uri = uri
            )
            saveContextItemUseCase(item)
        }
    }

    fun prepareCameraPhotoCapture(): Uri {
        val (uri, filePath) = imageCaptureModule.createCameraImageUri()
        pendingCameraFilePath = filePath
        return uri
    }

    fun onCameraPhotoCaptured(success: Boolean) {
        val snapshotId = _snapshotId.value ?: return
        val filePath = pendingCameraFilePath ?: return
        pendingCameraFilePath = null
        if (success) {
            viewModelScope.launch {
                val item = imageCaptureModule.captureImageFromFile(
                    snapshotId = snapshotId,
                    filePath = filePath
                )
                saveContextItemUseCase(item)
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            deleteContextItemUseCase(itemId)
        }
    }
}
