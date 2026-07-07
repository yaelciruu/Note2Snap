package com.note2snap.data.repository

import com.note2snap.structuring.StructuredNote
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    suspend fun processAndSaveWhiteboard(imageFilePath: String, title: String): Long
    fun observeAllNotes(): Flow<List<NoteSummary>>
    suspend fun getStructuredNote(noteId: Long): StructuredNote?
    suspend fun deleteNote(noteId: Long)
}