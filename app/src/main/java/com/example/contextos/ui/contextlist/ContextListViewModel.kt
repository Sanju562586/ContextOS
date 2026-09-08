package com.example.contextos.ui.contextlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contextos.data.sample.SampleData
import com.example.contextos.domain.usecase.DeleteSnapshotUseCase
import com.example.contextos.domain.usecase.GetSnapshotsUseCase
import com.example.contextos.domain.usecase.MarkSnapshotRestoredUseCase
import com.example.contextos.domain.usecase.SaveSnapshotUseCase
import com.example.contextos.domain.usecase.TogglePinSnapshotUseCase
import com.example.contextos.models.ContextSnapshot
import com.example.contextos.restore.ContextRestoreManager
import com.example.contextos.restore.ContextRestoreProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface ContextListUiState {
    data object Loading : ContextListUiState
    data class Success(val snapshots: List<ContextSnapshot>) : ContextListUiState
    data class Error(val message: String) : ContextListUiState
}

sealed interface ContextListEvent {
    data class ShowSnackbar(val message: String) : ContextListEvent
    data class LaunchRestore(val snapshot: ContextSnapshot) : ContextListEvent
}

@HiltViewModel
class ContextListViewModel @Inject constructor(
    getSnapshotsUseCase: GetSnapshotsUseCase,
    private val saveSnapshotUseCase: SaveSnapshotUseCase,
    private val deleteSnapshotUseCase: DeleteSnapshotUseCase,
    private val togglePinSnapshotUseCase: TogglePinSnapshotUseCase,
    private val markSnapshotRestoredUseCase: MarkSnapshotRestoredUseCase,
    private val contextRestoreManager: ContextRestoreManager
) : ViewModel() {

    private val _events = MutableSharedFlow<ContextListEvent>()
    val events: SharedFlow<ContextListEvent> = _events.asSharedFlow()

    private val _restoreProgress = MutableStateFlow<ContextRestoreProgress?>(null)
    val restoreProgress: StateFlow<ContextRestoreProgress?> = _restoreProgress.asStateFlow()

    val uiState: StateFlow<ContextListUiState> = getSnapshotsUseCase()
        .map<List<ContextSnapshot>, ContextListUiState> { snapshots ->
            ContextListUiState.Success(snapshots)
        }
        .catch { emit(ContextListUiState.Error(it.message ?: "Failed to load contexts")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ContextListUiState.Loading
        )

    fun onResumeClicked(snapshot: ContextSnapshot) {
        viewModelScope.launch {
            _events.emit(ContextListEvent.LaunchRestore(snapshot))
            contextRestoreManager.restoreContextFlow(snapshot).collect { progress ->
                _restoreProgress.value = progress
            }
        }
    }

    fun dismissRestore() {
        _restoreProgress.value = null
    }

    fun onPinToggled(snapshotId: String, isPinned: Boolean) {
        viewModelScope.launch {
            togglePinSnapshotUseCase(snapshotId, isPinned)
        }
    }

    fun onDeleteClicked(snapshotId: String) {
        viewModelScope.launch {
            deleteSnapshotUseCase(snapshotId)
            _events.emit(ContextListEvent.ShowSnackbar("Context snapshot removed"))
        }
    }

    fun seedSampleProjectAlpha() {
        viewModelScope.launch {
            val sample = SampleData.getProjectAlpha(snapshotId = UUID.randomUUID().toString())
            saveSnapshotUseCase(sample)
            _events.emit(ContextListEvent.ShowSnackbar("Created sample \"Project Alpha\""))
        }
    }

    fun seedSampleProjectBeta() {
        viewModelScope.launch {
            val sample = SampleData.getProjectBeta(snapshotId = UUID.randomUUID().toString())
            saveSnapshotUseCase(sample)
            _events.emit(ContextListEvent.ShowSnackbar("Created sample \"Project Beta\""))
        }
    }

    fun seedSampleWorkspaces() {
        viewModelScope.launch {
            saveSnapshotUseCase(SampleData.getProjectAlpha(snapshotId = UUID.randomUUID().toString()))
            saveSnapshotUseCase(SampleData.getProjectBeta(snapshotId = UUID.randomUUID().toString()))
            _events.emit(ContextListEvent.ShowSnackbar("Loaded sample contexts (Project Alpha & Beta)"))
        }
    }
}
