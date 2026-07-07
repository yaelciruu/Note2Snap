package com.note2snap.noteviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.note2snap.data.repository.NoteRepository
import com.note2snap.structuring.StructuredNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface NoteDetailUiState {
    data object Loading : NoteDetailUiState
    data class Loaded(val structuredNote: StructuredNote) : NoteDetailUiState
    data object NotFound : NoteDetailUiState
}

class NoteDetailViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<NoteDetailUiState>(NoteDetailUiState.Loading)
    val uiState: StateFlow<NoteDetailUiState> = _uiState

    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            _uiState.value = NoteDetailUiState.Loading
            val note = repository.getStructuredNote(noteId)
            _uiState.value = if (note != null) {
                NoteDetailUiState.Loaded(note)
            } else {
                NoteDetailUiState.NotFound
            }
        }
    }

    fun updateElementText(elementId: Long, newText: String, noteId: Long) {
        viewModelScope.launch {
            repository.updateElementText(elementId, newText)
            loadNote(noteId) // reload so the UI reflects the saved correction
        }
    }
}