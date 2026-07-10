package com.note2snap.structuring

import android.graphics.Rect
import com.note2snap.ccl.Region
import com.note2snap.ccl.RegionType
import com.note2snap.recognition.RecognizedLine
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NoteStructuringEngineTest {

    private val engine = NoteStructuringEngine()

    @Test
    fun `two close lines land in the same block`() {
        val sourceHeight = 1000
        val lines = listOf(
            recognizedLineAt(top = 100, bottom = 130, text = "Line 1"),
            recognizedLineAt(top = 135, bottom = 165, text = "Line 2") // gap = 5px = 0.005 of height
        )

        val result = engine.structure(lines, emptyList(), sourceImageWidth = 800, sourceImageHeight = sourceHeight)

        assertEquals(1, result.blocks.size)
        assertEquals(2, result.blocks.first().elements.size)
    }

    @Test
    fun `two widely separated lines land in different blocks`() {
        val sourceHeight = 1000
        val lines = listOf(
            recognizedLineAt(top = 100, bottom = 130, text = "Topic A"),
            recognizedLineAt(top = 300, bottom = 330, text = "Topic B") // gap = 170px = 0.17 of height
        )

        val result = engine.structure(lines, emptyList(), sourceImageWidth = 800, sourceImageHeight = sourceHeight)

        assertEquals(2, result.blocks.size)
    }

    @Test
    fun `diagram and text preserve reading order top to bottom`() {
        val diagramRegion = Region(
            id = 1,
            boundingBox = Rect(50, 400, 250, 600),
            type = RegionType.NON_TEXT,
            pixelArea = 40000,
            croppedBitmap = fakeBitmap()
        )
        val lines = listOf(
            recognizedLineAt(top = 50, bottom = 80, text = "Title above diagram")
        )

        val result = engine.structure(lines, listOf(diagramRegion), sourceImageWidth = 800, sourceImageHeight = 1000)

        val orderedKinds = result.allElementsInReadingOrder.map { it.kind }
        assertEquals(listOf(ElementKind.TEXT, ElementKind.DIAGRAM), orderedKinds)
    }

    @Test
    fun `diagram bitmap passes through unchanged`() {
        val originalBitmap = fakeBitmap()
        val diagramRegion = Region(
            id = 1,
            boundingBox = Rect(0, 0, 100, 100),
            type = RegionType.NON_TEXT,
            pixelArea = 10000,
            croppedBitmap = originalBitmap
        )

        val result = engine.structure(emptyList(), listOf(diagramRegion), sourceImageWidth = 800, sourceImageHeight = 1000)

        assertEquals(originalBitmap, result.allElementsInReadingOrder.first().diagramBitmap)
    }

    // --- Test helpers ---

    private fun recognizedLineAt(top: Int, bottom: Int, text: String) = RecognizedLine(
        sourceRegionId = top, // arbitrary unique id for the test
        boundingBox = Rect(50, top, 400, bottom),
        text = text,
        confidence = 0.9f
    )

    private fun fakeBitmap(): android.graphics.Bitmap =
        android.graphics.Bitmap.createBitmap(10, 10, android.graphics.Bitmap.Config.ARGB_8888)
}