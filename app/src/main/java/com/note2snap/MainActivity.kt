package com.note2snap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.note2snap.core.navigation.Note2SnapNavHost
import com.note2snap.core.theme.Note2SnapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Note2SnapTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val app = application as Note2SnapApplication
                    Note2SnapNavHost(noteRepository = app.noteRepository)
                }
            }
        }
    }
}