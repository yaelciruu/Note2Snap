package com.note2snap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.note2snap.ccl.ConnectedComponentLabeler
import com.note2snap.ccl.RegionType
import com.note2snap.core.theme.Note2SnapTheme
import com.note2snap.core.util.ImageStorage
import com.note2snap.preprocessing.WhiteboardPreprocessor
import com.note2snap.recognition.MockHandwritingRecognizer
import com.note2snap.structuring.NoteStructuringEngine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val imageStorage = ImageStorage(this)
        val existingCaptures = imageStorage.listCaptures()

        if (existingCaptures.isNotEmpty()) {
            val testFile = existingCaptures.first()
            lifecycleScope.launch {
                try {
                    val preprocessed = WhiteboardPreprocessor().process(testFile)
                    val regions = ConnectedComponentLabeler().label(preprocessed.binarizedBitmap)
                    val textRegions = regions.filter { it.type == RegionType.TEXT }
                    val nonTextRegions = regions.filter { it.type == RegionType.NON_TEXT }

                    val recognizedLines = MockHandwritingRecognizer().recognize(textRegions)

                    val structuredNote = NoteStructuringEngine().structure(
                        recognizedLines = recognizedLines,
                        nonTextRegions = nonTextRegions,
                        sourceImageWidth = preprocessed.originalWidth,
                        sourceImageHeight = preprocessed.originalHeight
                    )

                    android.util.Log.d(
                        "Note2SnapDebug",
                        "Structuring success: ${structuredNote.blocks.size} blocks"
                    )
                    structuredNote.blocks.forEach { block ->
                        android.util.Log.d(
                            "Note2SnapDebug",
                            "Block ${block.index}: ${block.elements.size} elements"
                        )
                        block.elements.forEach { element ->
                            android.util.Log.d(
                                "Note2SnapDebug",
                                "  - ${element.kind} at (${element.normalizedX}, ${element.normalizedY})"
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Note2SnapDebug", "Pipeline failed", e)
                }
            }
        } else {
            android.util.Log.d("Note2SnapDebug", "No existing captures found")
        }

        setContent {
            Note2SnapTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                }
            }
        }
    }
}