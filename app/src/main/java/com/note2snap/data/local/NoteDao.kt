package com.note2snap.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Insert
    suspend fun insertElements(elements: List<NoteElementEntity>)

    @Transaction
    @Query("SELECT * FROM notes ORDER BY createdAtEpochMillis DESC")
    fun observeAllNotesWithElements(): Flow<List<NoteWithElements>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteWithElements(noteId: Long): NoteWithElements?

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: Long)
}