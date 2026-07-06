package com.note2snap.recognition

import com.note2snap.ccl.Region

interface HandwritingRecognizer {
    suspend fun recognize(textRegions: List<Region>): List<RecognizedLine>
}