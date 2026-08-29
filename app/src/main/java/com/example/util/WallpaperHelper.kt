package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import com.example.data.model.BrushType
import com.example.data.model.DrawingPoint
import com.example.data.model.DrawingStroke
import com.example.data.model.LockscreenConfig
import com.example.data.model.PlacedSticker
import com.example.data.model.WallpaperTheme

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
                val path = createSmoothPath(stroke.points, width, height)
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

        val path = createSmoothPath(stroke.points, width, height)
        canvas.drawPath(path, paint)
    }

    private fun smoothPoints(rawPoints: List<DrawingPoint>): List<DrawingPoint> {
        if (rawPoints.size <= 2) return rawPoints
        val smoothed = ArrayList<DrawingPoint>(rawPoints.size)
        smoothed.add(rawPoints.first())
        for (i in 1 until rawPoints.size - 1) {
            val p0 = rawPoints[i - 1]
            val p1 = rawPoints[i]
            val p2 = rawPoints[i + 1]
            val sx = 0.25f * p0.x + 0.50f * p1.x + 0.25f * p2.x
            val sy = 0.25f * p0.y + 0.50f * p1.y + 0.25f * p2.y
            val sp = 0.25f * p0.pressure + 0.50f * p1.pressure + 0.25f * p2.pressure
            smoothed.add(DrawingPoint(sx, sy, sp, p1.timestamp))
        }
        smoothed.add(rawPoints.last())
        return smoothed
    }

    private fun createSmoothPath(points: List<DrawingPoint>, width: Int, height: Int): Path {
        val path = Path()
        if (points.isEmpty()) return path

        val smoothed = if (points.size > 2) smoothPoints(points) else points
        val first = smoothed[0]
        path.moveTo(first.x * width, first.y * height)

        if (smoothed.size == 2) {
            val second = smoothed[1]
            path.lineTo(second.x * width, second.y * height)
            return path
        }

        for (i in 1 until smoothed.size) {
            val prev = smoothed[i - 1]
            val curr = smoothed[i]
            val midX = ((prev.x + curr.x) / 2f) * width
            val midY = ((prev.y + curr.y) / 2f) * height
            path.quadTo(prev.x * width, prev.y * height, midX, midY)
        }
        val last = smoothed.last()
        path.lineTo(last.x * width, last.y * height)
        return path
    }
}
