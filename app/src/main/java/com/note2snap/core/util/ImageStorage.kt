package com.note2snap.core.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageStorage(private val context: Context) {

    private val whiteboardDir: File by lazy {
        File(context.filesDir, "whiteboards").apply { mkdirs() }
    }

    fun createCaptureFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(whiteboardDir, "WB_$timestamp.jpg")
    }

    fun toContentUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    suspend fun importFromUri(sourceUri: Uri): File = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val destination = File(whiteboardDir, "WB_$timestamp.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destination).use { output -> input.copyTo(output) }
        }
        destination
    }

    suspend fun saveBitmap(bitmap: Bitmap, filePrefix: String = "PROC"): File =
        withContext(Dispatchers.IO) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val destination = File(whiteboardDir, "${filePrefix}_$timestamp.png")
            FileOutputStream(destination).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            destination
        }

    fun listCaptures(): List<File> =
        whiteboardDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
}