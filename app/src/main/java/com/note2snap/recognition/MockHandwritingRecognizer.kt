package com.note2snap.recognition

import com.note2snap.ccl.Region

class MockHandwritingRecognizer : HandwritingRecognizer {
    override suspend fun recognize(textRegions: List<Region>): List<RecognizedLine> =
        textRegions.mapIndexed { index, region ->
            RecognizedLine(
                sourceRegionId = region.id,
                boundingBox = region.boundingBox,
                text = "Recognized line ${index + 1}",
                confidence = 0.0f
            )
        }
}