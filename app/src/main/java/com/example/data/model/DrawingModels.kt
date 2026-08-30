package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class StrokeModifier(val displayName: String, val isPremium: Boolean = false) {
    NONE("Normale", isPremium = false),
    WAVE("Wave", isPremium = true),
    PULSING("Pulsing", isPremium = true),
    SPARKLING("Sparkling", isPremium = true),
    DOT_FLOW("Flow", isPremium = true)
}

enum class BrushType(val displayName: String, val iconRes: String, val isPremium: Boolean = false) {
    PEN("Penna", "pen"),
    PENCIL("Matita", "pencil"),
    HIGHLIGHTER("Evidenziatore", "highlighter"),
    NEON("Neon Glow", "neon"),
    SPRAY("Bomboletta Spray", "spray", isPremium = true),
    RAINBOW("Arcobaleno", "rainbow"),
    DOTTED("Puntini", "dotted"),
    WAVY("Penna Ondulata", "wavy", isPremium = true),
    ANIMATED_WAVE("Wave", "animated_wave", isPremium = true),
    DOT_FLOW("Flow", "dot_flow", isPremium = true),
    PULSING_NEON("Neon Pulsing", "pulsing_neon", isPremium = true),
    PULSING_SPRAY("Spray Flow", "pulsing_spray", isPremium = true),
    ERASER("Gomma", "eraser");

    fun getSupportedModifiers(): List<StrokeModifier> {
        return when (this) {
            DOTTED -> listOf(StrokeModifier.NONE, StrokeModifier.DOT_FLOW)
            SPRAY -> listOf(
                StrokeModifier.NONE,
                StrokeModifier.DOT_FLOW,
                StrokeModifier.SPARKLING
            )
            PEN, PENCIL, HIGHLIGHTER, NEON, RAINBOW -> listOf(
                StrokeModifier.NONE,
                StrokeModifier.WAVE,
                StrokeModifier.PULSING
            )
            else -> listOf(StrokeModifier.NONE)
        }
    }
}

data class CustomStickerItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val emoji: String = "✨",
    val backgroundColorArgb: Long = 0xFF4F378B,
    val textColorArgb: Long = 0xFFFFFFFF,
    val isDoodle: Boolean = false,
    val doodlePoints: String? = null,
    val imageUri: String? = null,
    val imageBase64: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

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
    val authorId: String = "me", // "me" or deviceId or "partner"
    val alpha: Float = 1.0f,
    val modifier: StrokeModifier = StrokeModifier.NONE,
    val createdAt: Long = System.currentTimeMillis()
)

data class PlacedSticker(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String, // emoji or sticker identifier
    val x: Float, // percentage 0.0 - 1.0 of canvas width
    val y: Float, // percentage 0.0 - 1.0 of canvas height
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val authorId: String = "me",
    val createdAt: Long = System.currentTimeMillis()
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
