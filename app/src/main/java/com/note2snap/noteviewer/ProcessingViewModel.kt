package com.note2snap.noteviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.note2snap.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ProcessingUiState {
    data object Idle : ProcessingUiState
    data object Running : ProcessingUiState
    data class Success(val noteId: Long) : ProcessingUiState
    data class Failure(val message: String) : ProcessingUiState
}

class ProcessingViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ProcessingUiState>(ProcessingUiState.Idle)
    val uiState: StateFlow<ProcessingUiState> = _uiState

    fun processWhiteboard(imageFilePath: String, title: String) {
        _uiState.value = ProcessingUiState.Running
        viewModelScope.launch {
            runCatching { repository.processAndSaveWhiteboard(imageFilePath, title) }
                .onSuccess { noteId -> _uiState.value = ProcessingUiState.Success(noteId) }
                .onFailure { throwable ->
                    _uiState.value = ProcessingUiState.Failure(
                        throwable.message ?: "Failed to process whiteboard image"
                    )
                }
        }
    }
}