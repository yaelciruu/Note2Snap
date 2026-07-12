package com.note2snap.preprocessing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import kotlin.math.abs

/**
 * Prepares a raw whiteboard photo for downstream text/diagram detection.
 * Pipeline: decode (downsampled) -> grayscale -> deskew -> denoise ->
 * adaptive threshold (binarize) -> morphological cleanup. Output is a
 * binarized (black/white) bitmap ready for ConnectedComponentLabeler.
 * See the main guide's Steps 3-5 for the original design.
 */
class WhiteboardPreprocessor {

    private companion object {
        // Downscale target for very large camera photos, to keep OpenCV
        // operations fast without noticeably harming recognition quality.
        const val MAX_SOURCE_DIMENSION = 2048

        // Canny edge-detection thresholds used to find the whiteboard's
        // outline for deskewing.
        const val CANNY_LOWER_THRESHOLD = 50.0
        const val CANNY_UPPER_THRESHOLD = 150.0

        // A detected skew angle below this (in degrees) is treated as
        // "already straight" -- not worth the cost/risk of rotating.
        const val MIN_CORRECTABLE_SKEW_DEGREES = 0.5

        // Adaptive threshold block size (must be odd) and constant subtracted
        // from the mean, per OpenCV's adaptiveThreshold convention.
        const val ADAPTIVE_THRESHOLD_BLOCK_SIZE = 25
        const val ADAPTIVE_THRESHOLD_C = 10.0

        // Kernel size for the morphological "open" cleanup pass that removes
        // small speckle noise left over after binarization.
        const val MORPH_KERNEL_SIZE = 2.0
    }

    suspend fun process(imageFile: File): PreprocessingResult =
        withContext(Dispatchers.Default) {
            check(OpenCvInitializer.ensureInitialized()) {
                "OpenCV native library failed to load"
            }

            val sourceBitmap = decodeSampledBitmap(imageFile, maxDimension = MAX_SOURCE_DIMENSION)
            val sourceMat = Mat().also { Utils.bitmapToMat(sourceBitmap, it) }

            val grayMat = Mat()
            val deskewedMat: Mat
            val denoisedMat = Mat()
            val binaryMat = Mat()
            val cleanedMat = Mat()
            val morphKernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_ELLIPSE, Size(MORPH_KERNEL_SIZE, MORPH_KERNEL_SIZE)
            )

            try {
                if (sourceMat.channels() > 1) {
                    Imgproc.cvtColor(sourceMat, grayMat, Imgproc.COLOR_RGBA2GRAY)
                } else {
                    sourceMat.copyTo(grayMat)
                }

                deskewedMat = deskew(grayMat)

                Imgproc.GaussianBlur(deskewedMat, denoisedMat, Size(5.0, 5.0), 0.0)

                Imgproc.adaptiveThreshold(
                    denoisedMat,
                    binaryMat,
                    255.0,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY_INV,
                    ADAPTIVE_THRESHOLD_BLOCK_SIZE,
                    ADAPTIVE_THRESHOLD_C
                )

                Imgproc.morphologyEx(binaryMat, cleanedMat, Imgproc.MORPH_OPEN, morphKernel)

                val resultBitmap = createBitmap(cleanedMat.cols(), cleanedMat.rows())
                Utils.matToBitmap(cleanedMat, resultBitmap)

                PreprocessingResult(
                    binarizedBitmap = resultBitmap,
                    originalWidth = cleanedMat.cols(),
                    originalHeight = cleanedMat.rows()
                )
            } finally {
                // Native OpenCV memory isn't garbage-collected by the JVM --
                // release everything regardless of whether processing
                // succeeded or an exception was thrown partway through.
                listOf(sourceMat, grayMat, denoisedMat, binaryMat, cleanedMat, morphKernel)
                    .forEach { it.release() }
                sourceBitmap.recycle()
            }
        }

    private fun deskew(grayMat: Mat): Mat {
        val edges = Mat()
        Imgproc.Canny(grayMat, edges, CANNY_LOWER_THRESHOLD, CANNY_UPPER_THRESHOLD)

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            edges, contours, hierarchy,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )

        val largestContour = contours.maxByOrNull { Imgproc.contourArea(it) }

        edges.release()
        hierarchy.release()

        if (largestContour == null || contours.isEmpty()) {
            return grayMat.clone()
        }

        val points2f = MatOfPoint2f(*largestContour.toArray())
        val rotatedRect = Imgproc.minAreaRect(points2f)
        points2f.release()
        contours.forEach { it.release() }

        var angle = rotatedRect.angle
        if (angle < -45) angle += 90.0

        if (abs(angle) < MIN_CORRECTABLE_SKEW_DEGREES) return grayMat.clone()

        val rotationMatrix = Imgproc.getRotationMatrix2D(rotatedRect.center, angle, 1.0)
        val rotated = Mat()
        Imgproc.warpAffine(
            grayMat, rotated, rotationMatrix, grayMat.size(),
            Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE
        )
        rotationMatrix.release()

        return rotated
    }

    private fun decodeSampledBitmap(file: File, maxDimension: Int): Bitmap {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > maxDimension ||
            boundsOptions.outHeight / sampleSize > maxDimension
        ) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            ?: throw CorruptedImageException(
                "This image couldn't be read. It may be damaged or in an unsupported format."
            )
    }
}

class CorruptedImageException(message: String) : Exception(message)