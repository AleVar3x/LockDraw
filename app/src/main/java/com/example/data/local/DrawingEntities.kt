package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drawing_sessions")
data class DrawingSessionEntity(
    @PrimaryKey val sessionCode: String,
    val title: String,
    val wallpaperThemeName: String,
    val customWallpaperUri: String?,
    val updatedAt: Long = System.currentTimeMillis(),
    val hasUnsyncedChanges: Boolean = false,
    val boardVersion: Long = 0L
)

@Entity(tableName = "saved_strokes")
data class SavedStrokeEntity(
    @PrimaryKey val id: String,
    val sessionCode: String,
    val pointsJson: String,
    val colorArgb: Int,
    val strokeWidth: Float,
    val brushTypeName: String,
    val alpha: Float,
    val authorId: String,
    val orderIndex: Long,
    val modifierName: String = "NONE"
)

@Entity(tableName = "saved_stickers")
data class SavedStickerEntity(
    @PrimaryKey val id: String,
    val sessionCode: String,
    val content: String,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float,
    val authorId: String,
    val orderIndex: Long
)
