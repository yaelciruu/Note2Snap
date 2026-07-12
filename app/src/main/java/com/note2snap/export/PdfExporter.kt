package com.note2snap.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import com.note2snap.structuring.ElementKind
import com.note2snap.structuring.StructuredNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Exports a StructuredNote to a PDF file, reusing the same normalized
 * coordinate positioning NoteCanvas uses for on-screen rendering -- text
 * and diagrams land in the same relative positions in the PDF as they do
 * in the app.
 */
class PdfExporter(private val context: Context) {

    private companion object {
        const val PAGE_WIDTH_POINTS = 595 // A4 width at 72 DPI
        const val PAGE_HEIGHT_POINTS = 842 // A4 height at 72 DPI
        const val MARGIN = 24
    }

    suspend fun exportToPdf(structuredNote: StructuredNote, title: String): File =
        withContext(Dispatchers.Default) {
            val document = PdfDocument()

            val contentWidth = PAGE_WIDTH_POINTS - (MARGIN * 2)
            val contentHeight = (contentWidth * structuredNote.sourceImageHeight) /
                    structuredNote.sourceImageWidth.coerceAtLeast(1)

            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_POINTS, PAGE_HEIGHT_POINTS, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                textSize = 16f
                isFakeBoldText = true
            }
            canvas.drawText(title, MARGIN.toFloat(), MARGIN.toFloat() + 16f, titlePaint)

            val contentTop = MARGIN + 40 // leave room for the title
            val textPaint = Paint().apply { textSize = 10f }

            for (element in structuredNote.allElementsInReadingOrder) {
                val x = MARGIN + (element.normalizedX * contentWidth)
                val y = contentTop + (element.normalizedY * contentHeight)

                when (element.kind) {
                    ElementKind.TEXT -> {
                        canvas.drawText(element.text.orEmpty(), x, y + 10f, textPaint)
                    }
                    ElementKind.DIAGRAM -> {
                        element.diagramBitmap?.let { bitmap ->
                            val width = (element.normalizedWidth * contentWidth).toInt().coerceAtLeast(1)
                            val height = (element.normalizedHeight * contentHeight).toInt().coerceAtLeast(1)
                            val scaledBitmap = bitmap.scale(width, height)
                            canvas.drawBitmap(scaledBitmap, x, y, null)
                        }
                    }
                }
            }

            document.finishPage(page)

            val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
            val outputFile = File(exportsDir, "${title.replace(" ", "_")}.pdf")

            withContext(Dispatchers.IO) {
                FileOutputStream(outputFile).use { output -> document.writeTo(output) }
            }
            document.close()

            outputFile
        }

    fun getShareableUri(pdfFile: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
}