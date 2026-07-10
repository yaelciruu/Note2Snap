package com.note2snap.noteviewer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.note2snap.data.repository.NoteRepository
import com.note2snap.export.PdfExporter
import com.note2snap.structuring.StructuredNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface NoteDetailUiState {
    data object Loading : NoteDetailUiState
    data class Loaded(val structuredNote: StructuredNote) : NoteDetailUiState
    data object NotFound : NoteDetailUiState
}

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data object Exporting : ExportUiState
    data class Success(val pdfFile: java.io.File) : ExportUiState
    data class Failure(val message: String) : ExportUiState
}

class NoteDetailViewModel(
    private val repository: NoteRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<NoteDetailUiState>(NoteDetailUiState.Loading)
    val uiState: StateFlow<NoteDetailUiState> = _uiState

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState

    private val pdfExporter = PdfExporter(appContext)

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

    fun exportToPdf(title: String) {
        val currentState = _uiState.value
        if (currentState !is NoteDetailUiState.Loaded) return

        viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting
            try {
                val pdfFile = pdfExporter.exportToPdf(currentState.structuredNote, title)
                _exportState.value = ExportUiState.Success(pdfFile)
            } catch (e: Exception) {
                _exportState.value = ExportUiState.Failure(e.message ?: "Export failed")
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportUiState.Idle
    }
}