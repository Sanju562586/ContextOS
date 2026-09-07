package com.example.contextos.ui.contextlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contextos.domain.usecase.DeleteSnapshotUseCase
import com.example.contextos.domain.usecase.GetSnapshotsUseCase
import com.example.contextos.domain.usecase.MarkSnapshotRestoredUseCase
import com.example.contextos.domain.usecase.SaveSnapshotUseCase
import com.example.contextos.domain.usecase.TogglePinSnapshotUseCase
import com.example.contextos.models.AiSummary
import com.example.contextos.models.AppItem
import com.example.contextos.models.ContextSnapshot
import com.example.contextos.models.DocumentItem
import com.example.contextos.models.ImageArtifact
import com.example.contextos.models.LinkItem
import com.example.contextos.models.NoteItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val markSnapshotRestoredUseCase: MarkSnapshotRestoredUseCase
) : ViewModel() {

    private val _events = MutableSharedFlow<ContextListEvent>()
    val events: SharedFlow<ContextListEvent> = _events.asSharedFlow()

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
            markSnapshotRestoredUseCase(snapshot.id)
            _events.emit(ContextListEvent.LaunchRestore(snapshot))
            _events.emit(ContextListEvent.ShowSnackbar("Restoring \"${snapshot.name}\"..."))
        }
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
            val snapshotId = UUID.randomUUID().toString()
            val sample = ContextSnapshot(
                id = snapshotId,
                name = "Project Alpha",
                description = "Sprint planning review, architecture diagrams & cloud cost spreadsheets",
                createdAt = System.currentTimeMillis(),
                isPinned = true,
                colorHex = "#1E88E5",
                apps = listOf(
                    AppItem(
                        id = UUID.randomUUID().toString(),
                        snapshotId = snapshotId,
                        packageName = "com.google.android.apps.docs",
                        appName = "Google Drive",
                        displayOrder = 0,
                        isPrimaryForSplitScreen = true
                    ),
                    AppItem(
                        id = UUID.randomUUID().toString(),
                        snapshotId = snapshotId,
                        packageName = "com.android.chrome",
                        appName = "Chrome",
                        displayOrder = 1,
                        isPrimaryForSplitScreen = true
                    )
                ),
                links = listOf(
                    LinkItem(
                        id = UUID.randomUUID().toString(),
                        snapshotId = snapshotId,
                        url = "https://github.com/project-alpha/roadmap",
                        title = "Sprint Roadmap & Milestones"
                    ),
                    LinkItem(
                        id = UUID.randomUUID().toString(),
                        snapshotId = snapshotId,
                        url = "https://console.cloud.google.com/billing",
                        title = "GCP Cloud Cost Overview"
                    )
                ),
                documents = listOf(
                    DocumentItem(
                        id = UUID.randomUUID().toString(),
                        snapshotId = snapshotId,
                        contentUri = "content://com.android.providers.downloads.documents/document/104",
                        displayName = "Alpha_Architecture_v2.pdf",
                        mimeType = "application/pdf",
                        fileSizeBytes = 4_200_000L
                    )
                ),
                notes = listOf(
                    NoteItem(
                        id = UUID.randomUUID().toString(),
                        snapshotId = snapshotId,
                        content = "Finalize multi-region database replication SLA before client sync."
                    ),
                    NoteItem(
                        id = UUID.randomUUID().toString(),
                        snapshotId = snapshotId,
                        content = "Review whiteboard sticky notes from 10am standup."
                    )
                ),
                images = listOf(
                    ImageArtifact(
                        id = UUID.randomUUID().toString(),
                        snapshotId = snapshotId,
                        localFilePath = "/data/user/0/com.example.contextos/files/standup_board_alpha.jpg",
                        ocrRawText = "Q4 Goals: 1. P99 latency < 200ms 2. Zero-downtime migration 3. SOC2 compliance"
                    )
                ),
                aiSummary = AiSummary(
                    id = UUID.randomUUID().toString(),
                    snapshotId = snapshotId,
                    executiveBrief = "Context focused on Project Alpha architecture review and cost estimation. Two open PR links, architecture PDF, and standup whiteboard notes loaded.",
                    nextActions = listOf(
                        "Review Q4 latency targets in whiteboard notes",
                        "Confirm billing alert threshold in Cloud Console",
                        "Review PDF section 4 on database replication"
                    ),
                    modelVersion = "gemini-nano-v1"
                )
            )
            saveSnapshotUseCase(sample)
            _events.emit(ContextListEvent.ShowSnackbar("Created sample \"Project Alpha\""))
        }
    }
}
