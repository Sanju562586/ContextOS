package com.example.contextos.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contextos.domain.usecase.GetSnapshotByIdUseCase
import com.example.contextos.domain.usecase.MarkSnapshotRestoredUseCase
import com.example.contextos.models.ContextSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val markSnapshotRestoredUseCase: MarkSnapshotRestoredUseCase
) : ViewModel() {

    private val _snapshotId = MutableStateFlow<String?>(null)

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
    }

    fun restore(snapshotId: String) {
        viewModelScope.launch {
            markSnapshotRestoredUseCase(snapshotId)
        }
    }
}
