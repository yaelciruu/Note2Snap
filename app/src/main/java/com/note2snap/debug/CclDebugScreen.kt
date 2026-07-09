package com.note2snap.debug

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.note2snap.ccl.RegionType
import com.note2snap.core.util.ImageStorage
import kotlinx.coroutines.launch

/**
 * Developer-only debug screen. Not part of the normal user flow — wire a
 * temporary entry point to it (e.g. a debug-build-only button) while tuning
 * thresholds, then remove or gate it behind BuildConfig.DEBUG before release.
 */
@Composable
fun CclDebugScreen(viewModel: CclDebugViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val imageStorage = remember { ImageStorage(context) }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val file = imageStorage.importFromUri(uri)
                viewModel.runPipelineOn(file.absolutePath)
            }
        }
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(onClick = {
                pickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }) {
                Text("Pick whiteboard photo to test")
            }

            Text(
                "Green = TEXT region   Red = NON_TEXT (diagram) region",
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }

            state.binarizedBitmap?.let { bitmap ->
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val canvasWidthPx = constraints.maxWidth.toFloat()
                    val scale = canvasWidthPx / state.sourceWidth.toFloat()

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        state.regions.forEach { region ->
                            val color = if (region.type == RegionType.TEXT) Color.Green else Color.Red
                            drawRect(
                                color = color,
                                topLeft = Offset(
                                    region.boundingBox.left * scale,
                                    region.boundingBox.top * scale
                                ),
                                size = Size(
                                    region.boundingBox.width() * scale,
                                    region.boundingBox.height() * scale
                                ),
                                style = Stroke(width = 3f)
                            )
                        }
                    }
                }

                Text("Total regions: ${state.regions.size}")
                Text("TEXT: ${state.regions.count { it.type == RegionType.TEXT }}")
                Text("NON_TEXT: ${state.regions.count { it.type == RegionType.NON_TEXT }}")
            }
        }
    }
}