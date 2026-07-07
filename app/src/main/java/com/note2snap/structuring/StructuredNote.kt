package com.note2snap.structuring

import android.graphics.Bitmap

enum class ElementKind { TEXT, DIAGRAM }

data class StructuredElement(
    val elementId: Long = 0,
    val kind: ElementKind,
    val text: String?,
    val confidence: Float?,
    val diagramBitmap: Bitmap?,
    val normalizedX: Float,
    val normalizedY: Float,
    val normalizedWidth: Float,
    val normalizedHeight: Float,
    val blockIndex: Int
)

data class NoteBlock(
    val index: Int,
    val elements: List<StructuredElement>
)

data class StructuredNote(
    val blocks: List<NoteBlock>,
    val sourceImageWidth: Int,
    val sourceImageHeight: Int
) {
    val allElementsInReadingOrder: List<StructuredElement>
        get() = blocks.flatMap { it.elements }
}