package com.note2snap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.note2snap.core.theme.Note2SnapTheme
import com.note2snap.core.util.ImageStorage
import com.note2snap.preprocessing.WhiteboardPreprocessor
import kotlinx.coroutines.launch
import java.io.File

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
                    val result = WhiteboardPreprocessor().process(testFile)
                    android.util.Log.d(
                        "Note2SnapDebug",
                        "Preprocessing success: ${result.originalWidth}x${result.originalHeight}"
                    )
                    val savedPath = imageStorage.saveBitmap(result.binarizedBitmap, "PROC")
                    android.util.Log.d("Note2SnapDebug", "Saved to: ${savedPath.absolutePath}")
                } catch (e: Exception) {
                    android.util.Log.e("Note2SnapDebug", "Preprocessing failed", e)
                }
            }
        } else {
            android.util.Log.d("Note2SnapDebug", "No existing captures found — capture or import an image first")
        }

        setContent {
            Note2SnapTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                }
            }
        }
    }
}