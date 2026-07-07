package com.note2snap.data.repository

data class NoteSummary(
    val id: Long,
    val title: String,
    val createdAtEpochMillis: Long,
    val sourceImagePath: String
)