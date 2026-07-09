package com.note2snap.preprocessing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

class WhiteboardPreprocessor {

    suspend fun process(imageFile: File): PreprocessingResult =
        withContext(Dispatchers.Default) {
            check(OpenCvInitializer.ensureInitialized()) {
                "OpenCV native library failed to load"
            }

            val sourceBitmap = decodeSampledBitmap(imageFile, maxDimension = 2048)
            val sourceMat = Mat().also { Utils.bitmapToMat(sourceBitmap, it) }

            val grayMat = Mat()
            Imgproc.cvtColor(sourceMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

            val deskewedMat = deskew(grayMat)

            val denoisedMat = Mat()
            Imgproc.GaussianBlur(deskewedMat, denoisedMat, Size(5.0, 5.0), 0.0)

            val binaryMat = Mat()
            Imgproc.adaptiveThreshold(
                denoisedMat,
                binaryMat,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                25,
                10.0
            )

            val cleanedMat = Mat()
            val morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(2.0, 2.0))
            Imgproc.morphologyEx(binaryMat, cleanedMat, Imgproc.MORPH_OPEN, morphKernel)

            val resultBitmap = Bitmap.createBitmap(
                cleanedMat.cols(),
                cleanedMat.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(cleanedMat, resultBitmap)

            val width = cleanedMat.cols()
            val height = cleanedMat.rows()

            listOf(sourceMat, grayMat, deskewedMat, denoisedMat, binaryMat, cleanedMat, morphKernel)
                .forEach { it.release() }
            sourceBitmap.recycle()

            PreprocessingResult(
                binarizedBitmap = resultBitmap,
                originalWidth = width,
                originalHeight = height
            )
        }

    private fun deskew(grayMat: Mat): Mat {
        val edges = Mat()
        Imgproc.Canny(grayMat, edges, 50.0, 150.0)

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

        if (abs(angle) < 0.5) return grayMat.clone()

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