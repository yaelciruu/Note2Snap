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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as Note2SnapApplication
        val imageStorage = ImageStorage(this)
        val existingCaptures = imageStorage.listCaptures()

        if (existingCaptures.isNotEmpty()) {
            val testFile = existingCaptures.first()
            lifecycleScope.launch {
                try {
                    val noteId = app.noteRepository.processAndSaveWhiteboard(
                        testFile.absolutePath,
                        "Test Note"
                    )
                    android.util.Log.d("Note2SnapDebug", "Saved note with id: $noteId")

                    val structuredNote = app.noteRepository.getStructuredNote(noteId)
                    android.util.Log.d(
                        "Note2SnapDebug",
                        "Retrieved note: ${structuredNote?.blocks?.size} blocks"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("Note2SnapDebug", "Repository test failed", e)
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