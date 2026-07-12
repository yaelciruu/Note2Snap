package com.note2snap.structuring

import com.note2snap.ccl.Region
import com.note2snap.ccl.RegionType
import com.note2snap.recognition.RecognizedLine

class NoteStructuringEngine {

    private companion object {
        const val BLOCK_GAP_RATIO = 0.035f
    }

    fun structure(
        recognizedLines: List<RecognizedLine>,
        nonTextRegions: List<Region>,
        sourceImageWidth: Int,
        sourceImageHeight: Int
    ): StructuredNote {
        require(sourceImageWidth > 0 && sourceImageHeight > 0) {
            "Source image dimensions must be positive to normalize element positions"
        }

        val textElements = recognizedLines.map { line ->
            StructuredElement(
                kind = ElementKind.TEXT,
                text = line.text,
                confidence = line.confidence,
                diagramBitmap = null,
                normalizedX = line.boundingBox.left / sourceImageWidth.toFloat(),
                normalizedY = line.boundingBox.top / sourceImageHeight.toFloat(),
                normalizedWidth = line.boundingBox.width() / sourceImageWidth.toFloat(),
                normalizedHeight = line.boundingBox.height() / sourceImageHeight.toFloat(),
                blockIndex = -1
            )
        }

        val diagramElements = nonTextRegions
            .filter { it.type == RegionType.NON_TEXT }
            .map { region ->
                StructuredElement(
                    kind = ElementKind.DIAGRAM,
                    text = null,
                    confidence = null,
                    diagramBitmap = region.croppedBitmap,
                    normalizedX = region.boundingBox.left / sourceImageWidth.toFloat(),
                    normalizedY = region.boundingBox.top / sourceImageHeight.toFloat(),
                    normalizedWidth = region.boundingBox.width() / sourceImageWidth.toFloat(),
                    normalizedHeight = region.boundingBox.height() / sourceImageHeight.toFloat(),
                    blockIndex = -1
                )
            }

        val readingOrder = (textElements + diagramElements).sortedWith(
            compareBy({ it.normalizedY }, { it.normalizedX })
        )

        val blocks = segmentIntoBlocks(readingOrder)

        return StructuredNote(
            blocks = blocks,
            sourceImageWidth = sourceImageWidth,
            sourceImageHeight = sourceImageHeight
        )
    }

    private fun segmentIntoBlocks(readingOrder: List<StructuredElement>): List<NoteBlock> {
        if (readingOrder.isEmpty()) return emptyList()

        val blocks = mutableListOf<MutableList<StructuredElement>>(mutableListOf())
        var previousBottom = readingOrder.first().normalizedY

        for (element in readingOrder) {
            val gap = element.normalizedY - previousBottom
            if (gap > BLOCK_GAP_RATIO && blocks.last().isNotEmpty()) {
                blocks.add(mutableListOf())
            }
            blocks.last().add(element)
            previousBottom = maxOf(previousBottom, element.normalizedY + element.normalizedHeight)
        }

        return blocks.mapIndexed { index, elements ->
            NoteBlock(
                index = index,
                elements = elements.map { it.copy(blockIndex = index) }
            )
        }
    }
}