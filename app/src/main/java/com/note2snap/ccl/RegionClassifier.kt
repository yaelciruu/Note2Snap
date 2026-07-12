package com.note2snap.ccl

import android.graphics.Rect
object RegionClassifier {
    private const val MIN_GLYPH_PIXELS = 12
    private const val MAX_TEXT_EXTENT = 0.55
    private const val MAX_TEXT_ASPECT_RATIO = 6.0
    private const val MIN_DIAGRAM_AREA = 900
    private const val MAX_DIAGRAM_ASPECT_RATIO = 8.0

    fun classify(boundingBox: Rect, strokePixelCount: Int): RegionType? {
        val bboxArea = boundingBox.width() * boundingBox.height()
        if (bboxArea < MIN_GLYPH_PIXELS) return null

        val extent = strokePixelCount.toDouble() / bboxArea.toDouble()
        val aspectRatio = maxOf(
            boundingBox.width().toDouble() / boundingBox.height().coerceAtLeast(1),
            boundingBox.height().toDouble() / boundingBox.width().coerceAtLeast(1)
        )

        val looksLikeText = extent <= MAX_TEXT_EXTENT && aspectRatio <= MAX_TEXT_ASPECT_RATIO
        val isLargeEnoughForDiagram = bboxArea >= MIN_DIAGRAM_AREA && aspectRatio <= MAX_DIAGRAM_ASPECT_RATIO

        return when {
            looksLikeText -> RegionType.TEXT
            isLargeEnoughForDiagram -> RegionType.NON_TEXT
            else -> null
        }
    }
}