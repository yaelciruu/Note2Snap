package com.note2snap.ccl

import android.graphics.Bitmap
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect as CvRect
import org.opencv.imgproc.Imgproc
import androidx.core.graphics.createBitmap

class ConnectedComponentLabeler {

    companion object {
        // A merged "text" box bigger than this fraction of the whole image is
        // more likely a diagram (e.g. a whiteboard sketch) than a real paragraph
        // of handwriting -- see Part B, Step 2 of the hardening guide.
        private const val MAX_MERGED_TEXT_AREA_RATIO = 0.08

        // Upper bound on total regions per photo, checked after merging (not on
        // raw pre-merge fragments). Tune based on real photos; a typical
        // whiteboard has well under 100 -- see Part C, Step 3 of the hardening guide.
        private const val MAX_REGIONS_PER_IMAGE = 300

        // How close two text fragments can be (in pixels) and still get merged
        // into one line/paragraph. Tuned empirically against real whiteboard
        // photos during CCL debug-screen testing: 28 (original) under-merged
        // some lines, 15 over-fragmented words; 22 was the best balance found.
        private const val HORIZONTAL_GAP_THRESHOLD = 22
    }

    suspend fun label(binarizedBitmap: Bitmap): List<Region> =
        withContext(Dispatchers.Default) {
            val srcMat = Mat().also { Utils.bitmapToMat(binarizedBitmap, it) }
            val grayMat = Mat()
            if (srcMat.channels() > 1) {
                Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY)
            } else {
                srcMat.copyTo(grayMat)
            }

            val labels = Mat()
            val stats = Mat()
            val centroids = Mat()
            val componentCount = Imgproc.connectedComponentsWithStats(
                grayMat, labels, stats, centroids, 8, CvType.CV_32S
            )

            val rawRegions = mutableListOf<Region>()

            for (label in 1 until componentCount) {
                val x = stats.get(label, Imgproc.CC_STAT_LEFT)[0].toInt()
                val y = stats.get(label, Imgproc.CC_STAT_TOP)[0].toInt()
                val w = stats.get(label, Imgproc.CC_STAT_WIDTH)[0].toInt()
                val h = stats.get(label, Imgproc.CC_STAT_HEIGHT)[0].toInt()
                val area = stats.get(label, Imgproc.CC_STAT_AREA)[0].toInt()

                val boundingBox = Rect(x, y, x + w, y + h)
                val type = RegionClassifier.classify(boundingBox, area) ?: continue

                val cropMat = Mat(grayMat, CvRect(x, y, w, h))
                val cropBitmap = createBitmap(w, h, Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(cropMat, cropBitmap)
                cropMat.release()

                rawRegions += Region(
                    id = label,
                    boundingBox = boundingBox,
                    type = type,
                    pixelArea = area,
                    croppedBitmap = cropBitmap
                )
            }

            srcMat.release(); grayMat.release(); labels.release(); stats.release(); centroids.release()

            mergeAdjacentTextRegions(rawRegions, binarizedBitmap)
        }

    private fun mergeAdjacentTextRegions(regions: List<Region>, sourceBitmap: Bitmap): List<Region> {
        val nonText = regions.filter { it.type == RegionType.NON_TEXT }
        val textRegions = regions.filter { it.type == RegionType.TEXT }
            .sortedWith(compareBy({ it.boundingBox.top }, { it.boundingBox.left }))

        val merged = mutableListOf<Rect>()
        val used = BooleanArray(textRegions.size)

        for (i in textRegions.indices) {
            if (used[i]) continue
            var currentBox = Rect(textRegions[i].boundingBox)
            used[i] = true

            var expanded = true
            while (expanded) {
                expanded = false
                for (j in textRegions.indices) {
                    if (used[j]) continue
                    val candidate = textRegions[j].boundingBox

                    val verticallyAligned = candidate.top < currentBox.bottom &&
                            candidate.bottom > currentBox.top
                    val horizontallyClose =
                        candidate.left - currentBox.right in -HORIZONTAL_GAP_THRESHOLD..HORIZONTAL_GAP_THRESHOLD ||
                                currentBox.left - candidate.right in -HORIZONTAL_GAP_THRESHOLD..HORIZONTAL_GAP_THRESHOLD

                    if (verticallyAligned && horizontallyClose) {
                        currentBox = Rect(
                            minOf(currentBox.left, candidate.left),
                            minOf(currentBox.top, candidate.top),
                            maxOf(currentBox.right, candidate.right),
                            maxOf(currentBox.bottom, candidate.bottom)
                        )
                        used[j] = true
                        expanded = true
                    }
                }
            }
            merged += currentBox
        }

        val imageArea = sourceBitmap.width * sourceBitmap.height
        val maxMergedTextArea = imageArea * MAX_MERGED_TEXT_AREA_RATIO

        val mergedTextRegions = merged.mapIndexed { index, box ->
            val cropped = Bitmap.createBitmap(
                sourceBitmap,
                box.left.coerceIn(0, sourceBitmap.width - 1),
                box.top.coerceIn(0, sourceBitmap.height - 1),
                box.width().coerceAtMost(sourceBitmap.width - box.left),
                box.height().coerceAtMost(sourceBitmap.height - box.top)
            )
            val boxArea = box.width() * box.height()
            val finalType = if (boxArea > maxMergedTextArea) RegionType.NON_TEXT else RegionType.TEXT
            Region(
                id = 100_000 + index,
                boundingBox = box,
                type = finalType,
                pixelArea = boxArea,
                croppedBitmap = cropped
            )
        }

        val allRegions = nonText + mergedTextRegions
        if (allRegions.size > MAX_REGIONS_PER_IMAGE) {
            throw TooManyRegionsException(
                "This photo produced an unusually high number of regions (${allRegions.size}), " +
                        "which likely means the image is too noisy to process reliably. Try " +
                        "retaking it with better lighting or a cleaner whiteboard."
            )
        }

        return allRegions.sortedWith(
            compareBy({ it.boundingBox.top }, { it.boundingBox.left })
        )
    }
}

class TooManyRegionsException(message: String) : Exception(message)