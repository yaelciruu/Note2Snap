package com.note2snap.noteviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.note2snap.data.repository.NoteRepository
import com.note2snap.data.repository.NoteSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteListViewModel(private val repository: NoteRepository) : ViewModel() {

    val notes: StateFlow<List<NoteSummary>> = repository.observeAllNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun deleteNote(noteId: Long) {
        viewModelScope.launch { repository.deleteNote(noteId) }
    }
}