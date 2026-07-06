package com.note2snap.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class NoteWithElements(
    @Embedded val note: NoteEntity,
    @Relation(parentColumn = "id", entityColumn = "noteId")
    val elements: List<NoteElementEntity>
)