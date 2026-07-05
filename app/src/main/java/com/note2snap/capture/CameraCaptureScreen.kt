package com.note2snap.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.note2snap.core.util.ImageStorage
import kotlinx.coroutines.launch

@Composable
fun CameraCaptureScreen(
    onImageCaptured: (filePath: String) -> Unit,
    viewModel: CameraCaptureViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (hasCameraPermission) {
                CameraPreviewAndCapture(
                    onCaptureStarted = viewModel::onCaptureStarted,
                    onCaptured = { path -> onImageCaptured(path) },
                    onError = viewModel::onCaptureError
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Camera permission is required to photograph a whiteboard.",
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }

            if (uiState is CaptureUiState.Capturing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun CameraPreviewAndCapture(
    onCaptureStarted: () -> Unit,
    onCaptured: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val imageStorage = remember { ImageStorage(context) }
    val coroutineScope = rememberCoroutineScope()

    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewUseCase = remember {
        Preview.Builder().build().apply {
            setSurfaceProvider { request -> surfaceRequest = request }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        val cameraProvider =
            androidx.camera.lifecycle.ProcessCameraProvider.awaitInstance(context)
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            previewUseCase,
            imageCapture
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        surfaceRequest?.let { request ->
            CameraXViewfinder(
                surfaceRequest = request,
                modifier = Modifier.fillMaxSize()
            )
        }

        FloatingActionButton(
            onClick = {
                onCaptureStarted()
                val destinationFile = imageStorage.createCaptureFile()
                val outputOptions = ImageCapture.OutputFileOptions.Builder(destinationFile).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            coroutineScope.launch { onCaptured(destinationFile.absolutePath) }
                        }
                        override fun onError(exception: ImageCaptureException) {
                            onError(exception.message ?: "Capture failed")
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        ) {
            Icon(Icons.Filled.Camera, contentDescription = "Capture whiteboard")
        }
    }
}