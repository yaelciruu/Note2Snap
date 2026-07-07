package com.note2snap.noteviewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.note2snap.structuring.ElementKind
import com.note2snap.structuring.StructuredElement
import com.note2snap.structuring.StructuredNote

@Composable
fun NoteCanvas(
    structuredNote: StructuredNote,
    onElementEdited: ((elementId: Long, newText: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var editingElement by remember { mutableStateOf<StructuredElement?>(null) }

    val aspectRatio = structuredNote.sourceImageWidth.toFloat() /
            structuredNote.sourceImageHeight.toFloat().coerceAtLeast(1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val canvasWidthPx = constraints.maxWidth.toFloat()
        val canvasHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        structuredNote.allElementsInReadingOrder.forEach { element ->
            val xDp = with(density) { (element.normalizedX * canvasWidthPx).toDp() }
            val yDp = with(density) { (element.normalizedY * canvasHeightPx).toDp() }
            val widthDp = with(density) { (element.normalizedWidth * canvasWidthPx).toDp() }
            val heightDp = with(density) { (element.normalizedHeight * canvasHeightPx).toDp() }

            when (element.kind) {
                ElementKind.TEXT -> {
                    Text(
                        text = element.text.orEmpty(),
                        modifier = Modifier
                            .offset(x = xDp, y = yDp)
                            .size(
                                width = widthDp.coerceAtLeastDp(24.dp),
                                height = heightDp.coerceAtLeastDp(16.dp)
                            )
                            .let { base ->
                                if (onElementEdited != null) {
                                    base.clickable { editingElement = element }
                                } else base
                            },
                        color = if ((element.confidence ?: 1f) < 0.4f) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                ElementKind.DIAGRAM -> {
                    element.diagramBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Preserved diagram",
                            modifier = Modifier
                                .offset(x = xDp, y = yDp)
                                .size(width = widthDp, height = heightDp)
                        )
                    }
                }
            }
        }
    }

    val elementBeingEdited = editingElement
    if (elementBeingEdited != null && onElementEdited != null) {
        EditTextDialog(
            initialText = elementBeingEdited.text.orEmpty(),
            onConfirm = { newText ->
                onElementEdited(elementBeingEdited.elementId, newText)
                editingElement = null
            },
            onDismiss = { editingElement = null }
        )
    }
}

@Composable
private fun EditTextDialog(
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Correct this line") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun Dp.coerceAtLeastDp(min: Dp) = if (this < min) min else this