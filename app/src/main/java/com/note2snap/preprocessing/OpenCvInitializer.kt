package com.note2snap.preprocessing

import org.opencv.android.OpenCVLoader

object OpenCvInitializer {
    @Volatile
    private var initialized = false

    fun ensureInitialized(): Boolean {
        if (!initialized) {
            initialized = OpenCVLoader.initLocal()
        }
        return initialized
    }
}