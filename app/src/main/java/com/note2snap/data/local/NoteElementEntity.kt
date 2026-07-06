package com.note2snap.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_elements",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class NoteElementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val kind: String,               // "TEXT" or "DIAGRAM"
    val text: String?,              // null for diagrams
    val confidence: Float?,         // null for diagrams
    val diagramImagePath: String?,  // null for text
    val normalizedX: Float,
    val normalizedY: Float,
    val normalizedWidth: Float,
    val normalizedHeight: Float,
    val blockIndex: Int,
    val orderIndex: Int
)