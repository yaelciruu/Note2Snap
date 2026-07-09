package com.note2snap.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object NoteListDestination

@Serializable
data object CaptureChoiceDestination

@Serializable
data object CameraCaptureDestination

@Serializable
data object GalleryPickerDestination

@Serializable
data class ProcessingDestination(val imageFilePath: String)

@Serializable
data class NoteDetailDestination(val noteId: Long)

@Serializable
data object CclDebugDestination