package com.note2snap.recognition

class CrnnCtcDecoder(private val charset: List<Char>) {
    fun decode(frameClassIndices: IntArray, frameConfidences: FloatArray): Pair<String, Float> {
        val builder = StringBuilder()
        val confidences = mutableListOf<Float>()
        var previousClass = -1

        for (i in frameClassIndices.indices) {
            val classIndex = frameClassIndices[i]
            val isBlank = classIndex == 0

            if (!isBlank && classIndex != previousClass) {
                if (classIndex < charset.size) {
                    builder.append(charset[classIndex])
                    confidences += frameConfidences[i]
                }
            }
            previousClass = classIndex
        }

        val averageConfidence = if (confidences.isEmpty()) 0f else confidences.average().toFloat()
        return builder.toString() to averageConfidence
    }
}