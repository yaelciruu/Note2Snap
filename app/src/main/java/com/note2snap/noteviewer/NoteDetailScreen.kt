package com.note2snap.noteviewer

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val PDF_MIME_TYPE = "application/pdf"
private const val SHARE_CHOOSER_TITLE = "Share PDF"

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    viewModel: NoteDetailViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(noteId) { viewModel.loadNote(noteId) }

    // Once a PDF export succeeds, immediately hand it off to the system share
    // sheet, then reset export state back to Idle so this doesn't refire.
    LaunchedEffect(exportState) {
        val state = exportState
        if (state is ExportUiState.Success) {
            val uri = viewModel.getShareableUri(state.pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = PDF_MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, SHARE_CHOOSER_TITLE))
            viewModel.resetExportState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note Detail") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is NoteDetailUiState.Loaded) {
                        if (exportState is ExportUiState.Exporting) {
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                        } else {
                            IconButton(onClick = { viewModel.exportToPdf("Note_$noteId") }) {
                                Icon(Icons.Filled.Share, contentDescription = "Export as PDF")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is NoteDetailUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
            is NoteDetailUiState.NotFound -> {
                Text("Note not found.", modifier = Modifier.padding(padding).padding(24.dp))
            }
            is NoteDetailUiState.Loaded -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    val currentExportState = exportState
                    if (currentExportState is ExportUiState.Failure) {
                        Text(
                            "Export failed: ${currentExportState.message}",
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    NoteCanvas(
                        structuredNote = state.structuredNote,
                        onElementEdited = { elementId, newText ->
                            viewModel.updateElementText(elementId, newText, noteId)
                        }
                    )
                }
            }
        }
    }
}