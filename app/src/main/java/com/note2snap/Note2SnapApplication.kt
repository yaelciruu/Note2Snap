package com.note2snap

import android.app.Application
import com.note2snap.data.local.Note2SnapDatabase
import com.note2snap.data.repository.NoteRepository
import com.note2snap.data.repository.NoteRepositoryImpl

class Note2SnapApplication : Application() {
    lateinit var noteRepository: NoteRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Note2SnapDatabase.getInstance(this)
        noteRepository = NoteRepositoryImpl(
            noteDao = database.noteDao(),
            appContext = applicationContext
        )
    }
}