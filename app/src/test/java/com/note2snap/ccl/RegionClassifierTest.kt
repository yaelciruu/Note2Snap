package com.note2snap.ccl

import android.graphics.Rect
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)

class RegionClassifierTest {

    @Test
    fun `small sparse component classifies as text`() {
        val boundingBox = Rect(0, 0, 20, 30) // 20x30 = 600 px^2
        val strokePixelCount = 150 // extent = 0.25, well under MAX_TEXT_EXTENT
        val result = RegionClassifier.classify(boundingBox, strokePixelCount)
        assertEquals(RegionType.TEXT, result)
    }

    @Test
    fun `large filled square component classifies as non-text`() {
        val boundingBox = Rect(0, 0, 40, 40) // 1600 px^2
        val strokePixelCount = 1400 // extent ~0.875, well over MAX_TEXT_EXTENT
        val result = RegionClassifier.classify(boundingBox, strokePixelCount)
        assertEquals(RegionType.NON_TEXT, result)
    }

    @Test
    fun `extremely tiny component is discarded as noise`() {
        val boundingBox = Rect(0, 0, 2, 2) // 4 px^2, below MIN_GLYPH_PIXELS
        val strokePixelCount = 4
        val result = RegionClassifier.classify(boundingBox, strokePixelCount)
        assertNull(result)
    }

    @Test
    fun `wide low component with low extent classifies as text`() {
        val boundingBox = Rect(0, 0, 200, 25) // aspect ratio 8:1
        val strokePixelCount = 1000 // extent = 0.2
        val result = RegionClassifier.classify(boundingBox, strokePixelCount)
        assertEquals(RegionType.NON_TEXT, result)
    }

    @Test
    fun `small ambiguous blob is discarded, not defaulted to text`() {
        // Below MIN_DIAGRAM_AREA and above MAX_TEXT_EXTENT -- fails both checks.
        // Per the Part B hardening update, this now returns null (discarded)
        // rather than defaulting to TEXT, since forcing an ambiguous non-text-shaped
        // blob into TEXT caused real diagrams to be misclassified during CCL tuning.
        val boundingBox = Rect(0, 0, 25, 25) // 625 px^2, below MIN_DIAGRAM_AREA (900)
        val strokePixelCount = 500 // extent = 0.8, exceeds MAX_TEXT_EXTENT
        val result = RegionClassifier.classify(boundingBox, strokePixelCount)
        assertNull(result)
    }
}