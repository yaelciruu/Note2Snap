package com.note2snap

import android.app.Application
import com.note2snap.data.repository.NoteRepository

class Note2SnapApplication : Application() {
    lateinit var noteRepository: NoteRepository
        private set

    override fun onCreate() {
        super.onCreate()
        // TODO: wire real Room database + repository in Step 9/10
        noteRepository = object : NoteRepository {}
    }
}