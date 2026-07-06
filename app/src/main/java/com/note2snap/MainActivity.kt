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
                    android.util.Log.d(
                        "Note2SnapDebug",
                        "Preprocessing success: ${preprocessed.originalWidth}x${preprocessed.originalHeight}"
                    )

                    val regions = ConnectedComponentLabeler().label(preprocessed.binarizedBitmap)
                    val textCount = regions.count { it.type == RegionType.TEXT }
                    val diagramCount = regions.count { it.type == RegionType.NON_TEXT }

                    android.util.Log.d(
                        "Note2SnapDebug",
                        "CCL success: ${regions.size} total regions ($textCount text, $diagramCount diagrams)"
                    )
                    regions.forEach { region ->
                        android.util.Log.d(
                            "Note2SnapDebug",
                            "Region ${region.id}: ${region.type}, box=${region.boundingBox}, area=${region.pixelArea}"
                        )
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