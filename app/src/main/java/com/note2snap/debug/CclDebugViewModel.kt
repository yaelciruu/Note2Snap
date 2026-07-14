package com.note2snap.debug

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.note2snap.ccl.ConnectedComponentLabeler
import com.note2snap.ccl.Region
import com.note2snap.preprocessing.WhiteboardPreprocessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class CclDebugState(
    val isLoading: Boolean = false,
    val binarizedBitmap: Bitmap? = null,
    val regions: List<Region> = emptyList(),
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val errorMessage: String? = null
)

class CclDebugViewModel : ViewModel() {
    private val _state = MutableStateFlow(CclDebugState())
    val state: StateFlow<CclDebugState> = _state

    private val preprocessor = WhiteboardPreprocessor()
    private val labeler = ConnectedComponentLabeler()

    fun runPipelineOn(imageFilePath: String) {
        _state.value = CclDebugState(isLoading = true)
        viewModelScope.launch {
            runCatching {
                val preprocessed = preprocessor.process(File(imageFilePath))
                val regions = labeler.label(preprocessed.binarizedBitmap)
                CclDebugState(
                    isLoading = false,
                    binarizedBitmap = preprocessed.binarizedBitmap,
                    regions = regions,
                    sourceWidth = preprocessed.originalWidth,
                    sourceHeight = preprocessed.originalHeight
                )
            }.onSuccess { newState ->
                _state.value = newState
            }.onFailure { throwable ->
                _state.value = CclDebugState(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Failed to process image"
                )
            }
        }
    }
}