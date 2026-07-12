package com.note2snap.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.note2snap.data.local.Note2SnapDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(private val appContext: Context) : ViewModel() {

    private val _storageUsedMb = MutableStateFlow(0.0)
    val storageUsedMb: StateFlow<Double> = _storageUsedMb

    fun refreshStorageUsage() {
        viewModelScope.launch {
            val sizeBytes = withContext(Dispatchers.IO) {
                val whiteboardsDir = File(appContext.filesDir, "whiteboards")
                val exportsDir = File(appContext.filesDir, "exports")
                (whiteboardsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }) +
                        (exportsDir.takeIf { it.exists() }?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L)
            }
            _storageUsedMb.value = sizeBytes / (1024.0 * 1024.0)
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            Note2SnapDatabase.getInstance(appContext).clearAllTables()
            File(appContext.filesDir, "whiteboards").deleteRecursively()
            File(appContext.filesDir, "exports").deleteRecursively()
            withContext(Dispatchers.Main) { refreshStorageUsage() }
        }
    }
}