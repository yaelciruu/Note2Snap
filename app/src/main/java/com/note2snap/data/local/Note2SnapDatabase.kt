package com.note2snap.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class, NoteElementEntity::class],
    version = 1,
    exportSchema = true
)
abstract class Note2SnapDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: Note2SnapDatabase? = null

        fun getInstance(context: Context): Note2SnapDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    Note2SnapDatabase::class.java,
                    "note2snap.db"
                ).build().also { INSTANCE = it }
            }
    }
}