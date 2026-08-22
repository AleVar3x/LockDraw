package com.example.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.example.data.model.BrushType
import com.example.data.model.DrawingPoint
import com.example.data.model.DrawingStroke
import com.example.data.model.LockscreenConfig
import com.example.data.model.PlacedSticker
import com.example.data.model.WallpaperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class WallpaperTarget {
    LOCKSCREEN,
    HOMESCREEN,
    BOTH
}

object WallpaperHelper {

    fun createDrawingBitmap(
        context: Context,
        strokes: List<DrawingStroke>,
        stickers: List<PlacedSticker>,
        config: LockscreenConfig,
        width: Int = 1080,
        height: Int = 2400,
        includeBackground: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Wallpaper Background
        if (includeBackground) {
            val colors = config.wallpaperTheme.gradientColors
            if (colors.size >= 2) {
                val intColors = colors.map { it.toInt() }.toIntArray()
                val shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intColors,
                    null,
                    Shader.TileMode.CLAMP
                )
                val bgPaint = Paint().apply {
                    this.shader = shader
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            } else {
                canvas.drawColor(android.graphics.Color.parseColor("#121212"))
            }
        } else {
            canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }

        // 2. Draw Strokes
        strokes.forEach { stroke ->
            drawStrokeOnCanvas(canvas, stroke, width, height)
        }

        // 3. Draw Stickers
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = width * 0.09f
            textAlign = Paint.Align.CENTER
        }

        stickers.forEach { sticker ->
            canvas.save()
            val sx = sticker.x * width
            val sy = sticker.y * height
            canvas.translate(sx, sy)
            canvas.rotate(sticker.rotation)
            canvas.scale(sticker.scale, sticker.scale)
            canvas.drawText(sticker.content, 0f, 0f, textPaint)
            canvas.restore()
        }

        return bitmap
    }

    private fun drawStrokeOnCanvas(canvas: Canvas, stroke: DrawingStroke, width: Int, height: Int) {
        if (stroke.points.isEmpty()) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.colorArgb
            strokeWidth = stroke.strokeWidth * (width / 400f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            alpha = (stroke.alpha * 255).toInt().coerceIn(0, 255)
        }

        when (stroke.brushType) {
            BrushType.ERASER -> {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            BrushType.HIGHLIGHTER -> {
                paint.strokeWidth = stroke.strokeWidth * (width / 240f)
                paint.alpha = (0.4f * 255).toInt()
            }
            BrushType.NEON -> {
                // Outer glow
                val glowPaint = Paint(paint).apply {
                    strokeWidth = stroke.strokeWidth * (width / 280f)
                    alpha = (0.5f * 255).toInt()
                }
                val path = createPath(stroke.points, width, height)
                canvas.drawPath(path, glowPaint)
            }
            BrushType.RAINBOW -> {
                val rainbowColors = intArrayOf(
                    android.graphics.Color.RED,
                    android.graphics.Color.YELLOW,
                    android.graphics.Color.GREEN,
                    android.graphics.Color.CYAN,
                    android.graphics.Color.MAGENTA
                )
                paint.shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    rainbowColors, null, Shader.TileMode.REPEAT
                )
            }
            else -> {}
        }

        val path = createPath(stroke.points, width, height)
        canvas.drawPath(path, paint)
    }

    private fun createPath(points: List<DrawingPoint>, width: Int, height: Int): Path {
        val path = Path()
        if (points.isEmpty()) return path

        path.moveTo(points[0].x * width, points[0].y * height)
        for (i in 1 until points.size) {
            val p = points[i]
            path.lineTo(p.x * width, p.y * height)
        }
        return path
    }

    suspend fun applyToDeviceWallpaper(
        context: Context,
        strokes: List<DrawingStroke>,
        stickers: List<PlacedSticker>,
        config: LockscreenConfig,
        target: WallpaperTarget = WallpaperTarget.LOCKSCREEN
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val displayMetrics = context.resources.displayMetrics
            val w = displayMetrics.widthPixels.coerceAtLeast(1080)
            val h = displayMetrics.heightPixels.coerceAtLeast(1920)

            val bitmap = createDrawingBitmap(
                context = context,
                strokes = strokes,
                stickers = stickers,
                config = config,
                width = w,
                height = h,
                includeBackground = true
            )

            val wallpaperManager = WallpaperManager.getInstance(context)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val flags = when (target) {
                        WallpaperTarget.LOCKSCREEN -> WallpaperManager.FLAG_LOCK
                        WallpaperTarget.HOMESCREEN -> WallpaperManager.FLAG_SYSTEM
                        WallpaperTarget.BOTH -> WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
                    }
                    wallpaperManager.setBitmap(bitmap, null, true, flags)
                } else {
                    wallpaperManager.setBitmap(bitmap)
                }
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }

            Log.d("WallpaperHelper", "Wallpaper successfully set to: $target")
            true
        } catch (e: Exception) {
            Log.e("WallpaperHelper", "Failed to apply wallpaper", e)
            false
        }
    }
}
