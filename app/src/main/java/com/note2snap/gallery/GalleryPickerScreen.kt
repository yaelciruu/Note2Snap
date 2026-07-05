package com.note2snap.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.note2snap.core.util.ImageStorage
import kotlinx.coroutines.launch

@Composable
fun GalleryPickerScreen(
    onImageImported: (filePath: String) -> Unit,
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    val imageStorage = remember { ImageStorage(context) }
    val scope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            onCancelled()
            return@rememberLauncherForActivityResult
        }
        isImporting = true
        scope.launch {
            val savedFile = imageStorage.importFromUri(uri)
            isImporting = false
            onImageImported(savedFile.absolutePath)
        }
    }

    LaunchedEffect(Unit) {
        pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (isImporting) "Importing image…" else "Opening gallery…")
            Button(onClick = onCancelled, modifier = Modifier.padding(top = 16.dp)) {
                Text("Cancel")
            }
        }
    }
}