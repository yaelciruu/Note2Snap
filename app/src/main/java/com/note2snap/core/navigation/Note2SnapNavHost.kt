package com.note2snap.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.note2snap.data.repository.NoteRepository

@Composable
fun Note2SnapNavHost(noteRepository: NoteRepository) {
    // Placeholder until Step 14 wires the full NavHost graph.
    Text("Note2Snap initializing…", modifier = Modifier.fillMaxSize())
}