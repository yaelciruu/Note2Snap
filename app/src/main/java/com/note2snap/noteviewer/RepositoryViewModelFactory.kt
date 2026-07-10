package com.note2snap.noteviewer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.note2snap.data.repository.NoteRepository

class RepositoryViewModelFactory(
    private val noteRepository: NoteRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        when (modelClass) {
            ProcessingViewModel::class.java -> ProcessingViewModel(noteRepository) as T
            NoteListViewModel::class.java -> NoteListViewModel(noteRepository) as T
            NoteDetailViewModel::class.java -> NoteDetailViewModel(noteRepository, appContext) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
}