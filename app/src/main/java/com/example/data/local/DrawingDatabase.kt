package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DrawingSessionEntity::class,
        SavedStrokeEntity::class,
        SavedStickerEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class DrawingDatabase : RoomDatabase() {
    abstract fun drawingDao(): DrawingDao

    companion object {
        @Volatile
        private var INSTANCE: DrawingDatabase? = null

        fun getInstance(context: Context): DrawingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DrawingDatabase::class.java,
                    "lockdraw_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
