package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class BrushType(val displayName: String, val iconRes: String) {
    PEN("Penna Inchiostro", "pen"),
    PENCIL("Matita Tratteggio", "pencil"),
    HIGHLIGHTER("Evidenziatore", "highlighter"),
    NEON("Neon Glow", "neon"),
    RAINBOW("Effetto Arcobaleno", "rainbow"),
    DOTTED("Pennello Puntinato", "dotted"),
    ERASER("Gomma", "eraser")
}

data class DrawingPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class DrawingStroke(
    val id: String = java.util.UUID.randomUUID().toString(),
    val points: List<DrawingPoint> = emptyList(),
    val colorArgb: Int = Color(0xFFFF2A6D).toArgb(),
    val strokeWidth: Float = 12f,
    val brushType: BrushType = BrushType.PEN,
    val authorId: String = "me", // "me" or "partner"
    val alpha: Float = 1.0f
)

data class PlacedSticker(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String, // emoji or sticker identifier
    val x: Float, // percentage 0.0 - 1.0 of canvas width
    val y: Float, // percentage 0.0 - 1.0 of canvas height
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val authorId: String = "me"
)

enum class WallpaperTheme(val title: String, val gradientColors: List<Long>, val isDark: Boolean) {
    FROSTED_GLASS("Vetro Satinato", listOf(0xFF1A1C2E, 0xFF2D1B24, 0xFF121212), true),
    DEEP_PURPLE("Notte Stellata", listOf(0xFF0F0C29, 0xFF302B63, 0xFF24243E), true),
    SUNSET_LOVE("Tramonto Romantico", listOf(0xFFFF512F, 0xFFDD2476), true),
    PASTEL_DREAM("Sogno Pastello", listOf(0xFFE0C3FC, 0xFF8EC5FC), false),
    NEON_CYBER("Cyber Love", listOf(0xFF000428, 0xFF004E92), true),
    ROSE_GOLD("Rosa Delicato", listOf(0xFFFFAFBD, 0xFFFFC3A0), false),
    DARK_OLED("OLED Minimal", listOf(0xFF121212, 0xFF1E1E1E), true),
    COUPLE_BEACH("Aura di Coppia", listOf(0xFFFF9A8B, 0xFFFF6A88, 0xFFFF99AC), false),
    FOREST_MIST("Smeraldo Mistico", listOf(0xFF134E5E, 0xFF71B280), true)
}

data class LockscreenConfig(
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showStatusIcons: Boolean = true,
    val showLockIcon: Boolean = true,
    val showPartnerNotification: Boolean = true,
    val wallpaperTheme: WallpaperTheme = WallpaperTheme.FROSTED_GLASS,
    val customWallpaperUri: String? = null,
    val customClockFormat: String = "HH:mm"
)
