package com.note2snap.recognition

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.note2snap.ccl.Region

class CrnnTfLiteRecognizer(context: Context) : HandwritingRecognizer {

    private companion object {
        const val MODEL_ASSET_PATH = "crnn_model.tflite"
        const val MODEL_INPUT_HEIGHT = 32
        const val MODEL_INPUT_WIDTH = 256
    }

    private val decoder = CrnnCtcDecoder(charset = buildTrainedCharset())

    private val interpreter: Interpreter by lazy {
        val assetFileDescriptor = context.assets.openFd(MODEL_ASSET_PATH)
        val inputStream = java.io.FileInputStream(assetFileDescriptor.fileDescriptor)
        val modelBuffer = inputStream.channel.map(
            java.nio.channels.FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
        Interpreter(modelBuffer, Interpreter.Options().apply { setNumThreads(4) })
    }

    override suspend fun recognize(textRegions: List<Region>): List<RecognizedLine> =
        withContext(Dispatchers.Default) {
            textRegions.map { region ->
                val inputBuffer = preprocessForModel(region.croppedBitmap)
                val (classIndices, confidences) = runInference(inputBuffer)
                val (text, confidence) = decoder.decode(classIndices, confidences)
                RecognizedLine(
                    sourceRegionId = region.id,
                    boundingBox = region.boundingBox,
                    text = text,
                    confidence = confidence
                )
            }
        }

    private fun preprocessForModel(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_WIDTH, MODEL_INPUT_HEIGHT, true)
        val buffer = ByteBuffer.allocateDirect(4 * MODEL_INPUT_WIDTH * MODEL_INPUT_HEIGHT)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(MODEL_INPUT_WIDTH * MODEL_INPUT_HEIGHT)
        resized.getPixels(pixels, 0, MODEL_INPUT_WIDTH, 0, 0, MODEL_INPUT_WIDTH, MODEL_INPUT_HEIGHT)
        for (pixel in pixels) {
            val gray = ((pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)) / 3
            buffer.putFloat(gray / 255f)
        }
        buffer.rewind()
        return buffer
    }

    private fun runInference(inputBuffer: ByteBuffer): Pair<IntArray, FloatArray> {
        // TODO(integration): replace with the exact output tensor shape/decoding
        // logic from the trained CRNN export (time-steps x num-classes softmax).
        error(
            "CrnnTfLiteRecognizer.runInference is a placeholder. Wire this to the " +
                    "actual CRNN model output tensor before use."
        )
    }

    private fun buildTrainedCharset(): List<Char> =
        // TODO(integration): replace with the exact ordered charset used at training time.
        listOf('\u0000') + ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf(' ', '.', ',')
}