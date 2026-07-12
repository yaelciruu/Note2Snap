package com.note2snap.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBackClick: () -> Unit) {
    val storageUsedMb by viewModel.storageUsedMb.collectAsState()
    var showConfirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshStorageUsage() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Privacy", style = MaterialTheme.typography.titleMedium)
            Text(
                "Note2Snap works entirely offline. Your whiteboard photos and notes " +
                        "are stored only on this device, in app-private storage. Nothing is " +
                        "uploaded, no account or login is required, and no data leaves your phone.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            HorizontalDivider()

            Text("Storage", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Text(
                "Used: ${"%.1f".format(storageUsedMb)} MB",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            Button(onClick = { showConfirmClear = true }) {
                Text("Delete all notes and photos")
            }
        }
    }

    if (showConfirmClear) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("Delete everything?") },
            text = { Text("This permanently deletes all notes, photos, and exports. This cannot be undone.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.clearAllData()
                    showConfirmClear = false
                }) { Text("Delete") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showConfirmClear = false }) { Text("Cancel") }
            }
        )
    }
}