package com.note2snap.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.note2snap.capture.CameraCaptureScreen
import com.note2snap.data.repository.NoteRepository
import com.note2snap.debug.CclDebugScreen
import com.note2snap.gallery.GalleryPickerScreen
import com.note2snap.noteviewer.NoteDetailScreen
import com.note2snap.noteviewer.NoteDetailViewModel
import com.note2snap.noteviewer.NoteListScreen
import com.note2snap.noteviewer.NoteListViewModel
import com.note2snap.noteviewer.ProcessingUiState
import com.note2snap.noteviewer.ProcessingViewModel
import com.note2snap.noteviewer.RepositoryViewModelFactory
import com.note2snap.settings.SettingsScreen
import com.note2snap.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import androidx.compose.runtime.mutableIntStateOf

private const val DEFAULT_NOTE_TITLE = "Whiteboard Note"

// Time thresholds (seconds) for the staged, approximate loading messages
// shown during processing. Not wired to real per-stage progress -- see
// the UI polish guide's Part C, Step 2 for the rationale.
private const val STAGE_1_END_SECONDS = 2
private const val STAGE_2_END_SECONDS = 4
private const val STAGE_3_END_SECONDS = 6

@Composable
fun Note2SnapNavHost(noteRepository: NoteRepository) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val factory = RepositoryViewModelFactory(noteRepository, context.applicationContext)

    NavHost(navController = navController, startDestination = NoteListDestination) {

        composable<NoteListDestination> {
            val viewModel: NoteListViewModel = viewModel(factory = factory)
            NoteListScreen(
                viewModel = viewModel,
                onNoteClick = { noteId ->
                    navController.navigate(NoteDetailDestination(noteId))
                },
                onNewCaptureClick = {
                    navController.navigate(CaptureChoiceDestination)
                },
                onDebugClick = {
                    navController.navigate(CclDebugDestination)
                },
                onSettingsClick = {
                    navController.navigate(SettingsDestination)
                }
            )
        }

        composable<CaptureChoiceDestination> {
            CaptureChoiceScreen(
                onCameraChosen = { navController.navigate(CameraCaptureDestination) },
                onGalleryChosen = { navController.navigate(GalleryPickerDestination) }
            )
        }

        composable<CameraCaptureDestination> {
            CameraCaptureScreen(
                onImageCaptured = { filePath ->
                    navController.navigate(ProcessingDestination(filePath)) {
                        popUpTo<NoteListDestination>()
                    }
                }
            )
        }

        composable<GalleryPickerDestination> {
            GalleryPickerScreen(
                onImageImported = { filePath ->
                    navController.navigate(ProcessingDestination(filePath)) {
                        popUpTo<NoteListDestination>()
                    }
                },
                onCancelled = { navController.popBackStack() }
            )
        }

        composable<ProcessingDestination> { backStackEntry ->
            val route: ProcessingDestination = backStackEntry.toRoute()
            val viewModel: ProcessingViewModel = viewModel(factory = factory)
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(route.imageFilePath) {
                viewModel.processWhiteboard(route.imageFilePath, title = DEFAULT_NOTE_TITLE)
            }

            Scaffold { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val state = uiState) {
                        is ProcessingUiState.Success -> {
                            LaunchedEffect(state.noteId) {
                                navController.navigate(NoteDetailDestination(state.noteId)) {
                                    popUpTo<NoteListDestination>()
                                }
                            }
                        }
                        is ProcessingUiState.Failure -> {
                            Text("Processing failed: ${state.message}")
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Go back")
                            }
                        }
                        else -> {
                            // Approximate, time-based staged loading messages -- not
                            // wired to real pipeline progress, just gives the user a
                            // sense that distinct steps are happening.
                            var elapsedSeconds by remember { mutableIntStateOf(0) }
                            LaunchedEffect(Unit) {
                                while (true) {
                                    delay(1.seconds)
                                    elapsedSeconds++
                                }
                            }

                            CircularProgressIndicator()
                            val stageMessage = when {
                                elapsedSeconds < STAGE_1_END_SECONDS -> "Preprocessing image…"
                                elapsedSeconds < STAGE_2_END_SECONDS -> "Detecting text and diagrams…"
                                elapsedSeconds < STAGE_3_END_SECONDS -> "Recognizing handwriting…"
                                else -> "Structuring your note…"
                            }
                            Text(stageMessage, modifier = Modifier.padding(top = 16.dp))
                        }
                    }
                }
            }
        }

        composable<NoteDetailDestination> { backStackEntry ->
            val route: NoteDetailDestination = backStackEntry.toRoute()
            val viewModel: NoteDetailViewModel = viewModel(factory = factory)
            NoteDetailScreen(
                noteId = route.noteId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<CclDebugDestination> {
            CclDebugScreen()
        }

        composable<SettingsDestination> {
            val viewModel = viewModel<SettingsViewModel> {
                SettingsViewModel(context.applicationContext)
            }
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun CaptureChoiceScreen(onCameraChosen: () -> Unit, onGalleryChosen: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onCameraChosen, modifier = Modifier.padding(bottom = 12.dp)) {
                Text("Take a photo")
            }
            Button(onClick = onGalleryChosen) {
                Text("Choose from gallery")
            }
        }
    }
}