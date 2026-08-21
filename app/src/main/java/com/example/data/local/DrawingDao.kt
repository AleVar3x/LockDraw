package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawingDao {
    @Query("SELECT * FROM drawing_sessions WHERE sessionCode = :code LIMIT 1")
    suspend fun getSession(code: String): DrawingSessionEntity?

    @Query("SELECT * FROM drawing_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<DrawingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: DrawingSessionEntity)

    @Query("SELECT * FROM saved_strokes WHERE sessionCode = :code ORDER BY orderIndex ASC")
    suspend fun getStrokesForSession(code: String): List<SavedStrokeEntity>

    @Query("SELECT * FROM saved_stickers WHERE sessionCode = :code ORDER BY orderIndex ASC")
    suspend fun getStickersForSession(code: String): List<SavedStickerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrokes(strokes: List<SavedStrokeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickers(stickers: List<SavedStickerEntity>)

    @Query("DELETE FROM saved_strokes WHERE sessionCode = :code")
    suspend fun clearStrokes(code: String)

    @Query("DELETE FROM saved_stickers WHERE sessionCode = :code")
    suspend fun clearStickers(code: String)

    @Transaction
    suspend fun saveFullSessionState(
        session: DrawingSessionEntity,
        strokes: List<SavedStrokeEntity>,
        stickers: List<SavedStickerEntity>
    ) {
        insertSession(session)
        clearStrokes(session.sessionCode)
        clearStickers(session.sessionCode)
        if (strokes.isNotEmpty()) insertStrokes(strokes)
        if (stickers.isNotEmpty()) insertStickers(stickers)
    }
}
