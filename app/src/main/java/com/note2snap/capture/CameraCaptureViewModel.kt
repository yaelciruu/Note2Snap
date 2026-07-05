package com.note2snap.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface CaptureUiState {
    data object Idle : CaptureUiState
    data object Capturing : CaptureUiState
    data class Captured(val imageUri: Uri, val filePath: String) : CaptureUiState
    data class Error(val message: String) : CaptureUiState
}

class CameraCaptureViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState

    fun onCaptureStarted() {
        _uiState.value = CaptureUiState.Capturing
    }

    fun onCaptureSuccess(uri: Uri, filePath: String) {
        viewModelScope.launch {
            _uiState.value = CaptureUiState.Captured(uri, filePath)
        }
    }

    fun onCaptureError(message: String) {
        _uiState.value = CaptureUiState.Error(message)
    }

    fun reset() {
        _uiState.value = CaptureUiState.Idle
    }
}