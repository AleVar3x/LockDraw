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
            BrushType.SPRAY, BrushType.PULSING_SPRAY -> {
                drawSprayOnCanvas(canvas, stroke, width, height)
                return
            }
            BrushType.ERASER -> {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            BrushType.HIGHLIGHTER -> {
                paint.strokeWidth = stroke.strokeWidth * (width / 240f)
                paint.alpha = (0.4f * 255).toInt()
            }
            BrushType.WATERCOLOR -> {
                drawWatercolorOnCanvas(canvas, stroke, width, height)
                return
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

    private fun drawSprayOnCanvas(canvas: Canvas, stroke: DrawingStroke, width: Int, height: Int) {
        val pts = stroke.points
        if (pts.isEmpty()) return

        val baseRadius = (stroke.strokeWidth * (width / 400f) * 1.5f).coerceIn(12f, 65f)
        val seed = stroke.id.hashCode().toLong()
        val random = java.util.Random(seed)

        val pathPts = if (pts.size > 2) smoothPoints(pts) else pts
        val screenPts = pathPts.map { android.graphics.PointF(it.x * width, it.y * height) }
        val n = screenPts.size

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.colorArgb
            style = Paint.Style.FILL
        }

        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }

        val baseAlpha = (stroke.alpha * 255).toInt().coerceIn(0, 255)
        val dotSizeBase = (stroke.strokeWidth * (width / 400f) * 0.12f).coerceIn(1.2f, 3.2f)

        if (n <= 1) {
            val cx = pts[0].x * width
            val cy = pts[0].y * height
            paint.alpha = (baseAlpha * 0.75f).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx, cy, baseRadius * 0.85f, paint)
            return
        }

        val cumDist = FloatArray(n)
        cumDist[0] = 0f
        for (i in 1 until n) {
            val dx = screenPts[i].x - screenPts[i - 1].x
            val dy = screenPts[i].y - screenPts[i - 1].y
            cumDist[i] = cumDist[i - 1] + kotlin.math.hypot(dx, dy)
        }
        val totalLength = cumDist[n - 1]
        if (totalLength <= 1f) return

        // 1. Soft underlying mist layer
        val baseStepStride = (baseRadius * 0.40f).coerceAtLeast(5f)
        val baseClusterCount = (totalLength / baseStepStride).toInt().coerceIn(3, 140)
        for (b in 0..baseClusterCount) {
            val distAlong = (b.toFloat() / baseClusterCount) * totalLength
            var sIdx = 0
            while (sIdx < n - 2 && cumDist[sIdx + 1] < distAlong) sIdx++
            val sStart = cumDist[sIdx]
            val sEnd = cumDist[sIdx + 1]
            val segLen = (sEnd - sStart).coerceAtLeast(0.001f)
            val u = ((distAlong - sStart) / segLen).coerceIn(0f, 1f)
            val bx = screenPts[sIdx].x + (screenPts[sIdx + 1].x - screenPts[sIdx].x) * u
            val by = screenPts[sIdx].y + (screenPts[sIdx + 1].y - screenPts[sIdx].y) * u

            val baseDots = (stroke.strokeWidth * 0.75f).toInt().coerceIn(8, 18)
            for (bd in 0 until baseDots) {
                val angle = random.nextFloat() * 6.2831855f
                val rFactor = (random.nextFloat() + random.nextFloat()) * 0.5f
                val dotDist = rFactor * baseRadius * 0.90f
                val dx = bx + kotlin.math.cos(angle) * dotDist
                val dy = by + kotlin.math.sin(angle) * dotDist
                val bAlpha = (baseAlpha * 0.35f * (1f - rFactor * 0.45f)).toInt().coerceIn(0, 255)
                paint.alpha = bAlpha
                canvas.drawCircle(dx, dy, dotSizeBase * 1.0f, paint)
            }
        }

        // 2. High-density core aerosol droplets
        val clusterSpacing = (baseRadius * 0.28f).coerceIn(3f, 12f)
        val numClusters = (totalLength / clusterSpacing).toInt().coerceAtLeast(6)
        val dotsPerCluster = (stroke.strokeWidth * 1.25f).toInt().coerceIn(16, 34)

        for (c in 0 until numClusters) {
            val distAlong = (c.toFloat() / numClusters) * totalLength
            var sIdx = 0
            while (sIdx < n - 2 && cumDist[sIdx + 1] < distAlong) sIdx++
            val sStart = cumDist[sIdx]
            val sEnd = cumDist[sIdx + 1]
            val segLen = (sEnd - sStart).coerceAtLeast(0.001f)
            val u = ((distAlong - sStart) / segLen).coerceIn(0f, 1f)

            val p0 = screenPts[sIdx]
            val p1 = screenPts[sIdx + 1]
            val cx = p0.x + (p1.x - p0.x) * u
            val cy = p0.y + (p1.y - p0.y) * u

            val dirX = (p1.x - p0.x) / segLen
            val dirY = (p1.y - p0.y) / segLen
            val normX = -dirY
            val normY = dirX

            for (d in 0 until dotsPerCluster) {
                val rFactor = (random.nextFloat() + random.nextFloat()) - 1.0f
                val perpDist = rFactor * baseRadius
                val tangOffset = (random.nextFloat() - 0.5f) * clusterSpacing * 0.85f

                val dotX = cx + normX * perpDist + dirX * tangOffset
                val dotY = cy + normY * perpDist + dirY * tangOffset

                val isLargeDot = random.nextFloat() > 0.82f
                val dotRadius = if (isLargeDot) dotSizeBase * 1.35f else dotSizeBase * (0.85f + 0.35f * (1f - kotlin.math.abs(rFactor)))
                val centerDensity = (1.0f - kotlin.math.abs(rFactor) * 0.38f).coerceIn(0.30f, 1.0f)
                val dotAlpha = (baseAlpha * 0.85f * centerDensity).toInt().coerceIn(0, 255)

                paint.alpha = dotAlpha
                canvas.drawCircle(dotX, dotY, dotRadius, paint)

                if (isLargeDot && kotlin.math.abs(rFactor) < 0.32f) {
                    highlightPaint.alpha = (dotAlpha * 0.45f).toInt().coerceIn(0, 255)
                    canvas.drawCircle(dotX, dotY, dotRadius * 0.55f, highlightPaint)
                }
            }
        }
    }

    private fun drawWatercolorOnCanvas(canvas: Canvas, stroke: DrawingStroke, width: Int, height: Int) {
        val pts = stroke.points
        if (pts.isEmpty()) return

        val baseWidth = stroke.strokeWidth * (width / 400f).coerceAtLeast(1f)
        val baseRadius = (baseWidth * 1.15f).coerceAtLeast(4f)
        val baseAlpha = (stroke.alpha * 255).toInt().coerceIn(0, 255)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.colorArgb
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.colorArgb
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val random = java.util.Random(stroke.id.hashCode().toLong())

        // 1. Single touch: full watercolor splatter dab with dark rim and radial droplets
        if (pts.size == 1) {
            val cx = pts[0].x * width
            val cy = pts[0].y * height

            val numLobes = 12
            val stainPath = android.graphics.Path()
            for (i in 0 until numLobes) {
                val angle = (i.toFloat() / numLobes) * 2f * Math.PI.toFloat()
                val rVar = 0.55f + 0.65f * random.nextFloat()
                val lobeR = baseRadius * rVar
                val px = cx + kotlin.math.cos(angle).toFloat() * lobeR
                val py = cy + kotlin.math.sin(angle).toFloat() * lobeR
                if (i == 0) stainPath.moveTo(px, py) else stainPath.lineTo(px, py)
            }
            stainPath.close()

            // Soft interior wash
            fillPaint.alpha = (baseAlpha * 0.18f).toInt().coerceIn(1, 255)
            canvas.drawPath(stainPath, fillPaint)

            // Mottled puddles
            for (b in 0 until 5) {
                val bAngle = random.nextFloat() * 2f * Math.PI.toFloat()
                val bDist = baseRadius * 0.40f * random.nextFloat()
                val bx = cx + kotlin.math.cos(bAngle).toFloat() * bDist
                val by = cy + kotlin.math.sin(bAngle).toFloat() * bDist
                val bRad = baseRadius * (0.35f + 0.35f * random.nextFloat())
                fillPaint.alpha = (baseAlpha * 0.14f).toInt().coerceIn(1, 255)
                canvas.drawCircle(bx, by, bRad, fillPaint)
            }

            // Dark wet-edge rim
            strokePaint.strokeWidth = (baseWidth * 0.18f).coerceIn(1.8f, 5.5f)
            strokePaint.alpha = (baseAlpha * 0.85f).toInt().coerceIn(1, 255)
            canvas.drawPath(stainPath, strokePaint)

            // Radial splatters
            val numSplatters = 20 + random.nextInt(15)
            for (s in 0 until numSplatters) {
                val sAngle = random.nextFloat() * 2f * Math.PI.toFloat()
                val sDist = baseRadius * if (s % 4 == 0) (1.8f + 2.0f * random.nextFloat()) else (1.1f + 0.8f * random.nextFloat())
                val sx = cx + kotlin.math.cos(sAngle).toFloat() * sDist
                val sy = cy + kotlin.math.sin(sAngle).toFloat() * sDist
                val sRadius = if (s % 5 == 0) (baseWidth * 0.14f + 1.8f * random.nextFloat()).coerceIn(1.8f, 5.5f)
                else (baseWidth * 0.08f + 1.2f * random.nextFloat()).coerceIn(1.0f, 3.2f)

                fillPaint.alpha = (baseAlpha * 0.90f).toInt().coerceIn(1, 255)
                canvas.drawCircle(sx, sy, sRadius, fillPaint)
            }
            return
        }

        val smoothedPts = if (pts.size > 2) smoothPoints(pts) else pts
        val n = smoothedPts.size
        val screenPts = smoothedPts.map { android.graphics.PointF(it.x * width, it.y * height) }

        val segLengths = FloatArray(n - 1)
        var totalLength = 0f
        for (i in 0 until n - 1) {
            val len = kotlin.math.hypot(screenPts[i + 1].x - screenPts[i].x, screenPts[i + 1].y - screenPts[i].y)
            segLengths[i] = len
            totalLength += len
        }
        if (totalLength <= 0.5f) return

        val stepSpacing = (baseRadius * 0.28f).coerceIn(2.5f, 15f)
        var currentDist = 0f
        var segIdx = 0
        var distInSeg = 0f

        val leftBoundary = ArrayList<android.graphics.PointF>()
        val rightBoundary = ArrayList<android.graphics.PointF>()
        val centerSamples = ArrayList<android.graphics.PointF>()
        val radiiSamples = ArrayList<Float>()

        while (currentDist <= totalLength) {
            while (segIdx < n - 2 && distInSeg > segLengths[segIdx]) {
                distInSeg -= segLengths[segIdx]
                segIdx++
            }
            val p0 = screenPts[segIdx]
            val p1 = screenPts[segIdx + 1]
            val segLen = segLengths[segIdx]
            val segFrac = if (segLen > 0.001f) (distInSeg / segLen).coerceIn(0f, 1f) else 0f

            val posX = p0.x + (p1.x - p0.x) * segFrac
            val posY = p0.y + (p1.y - p0.y) * segFrac

            val dirX = if (segLen > 0.001f) (p1.x - p0.x) / segLen else 1f
            val dirY = if (segLen > 0.001f) (p1.y - p0.y) / segLen else 0f
            val normX = -dirY
            val normY = dirX

            val activeRadius = baseRadius * (0.85f + 0.30f * random.nextFloat())
            val leftDist = activeRadius * (0.75f + 0.55f * random.nextFloat())
            val rightDist = activeRadius * (0.75f + 0.55f * random.nextFloat())

            val lp = android.graphics.PointF(posX + normX * leftDist, posY + normY * leftDist)
            val rp = android.graphics.PointF(posX - normX * rightDist, posY - normY * rightDist)

            leftBoundary.add(lp)
            rightBoundary.add(rp)
            centerSamples.add(android.graphics.PointF(posX, posY))
            radiiSamples.add(activeRadius)

            // Dynamic splatters
            if (random.nextFloat() > 0.40f) {
                val isLeft = random.nextBoolean()
                val sideSign = if (isLeft) 1f else -1f
                val sideDist = if (isLeft) leftDist else rightDist
                val spDist = sideDist + activeRadius * (0.35f + 1.75f * random.nextFloat())
                val spX = posX + normX * (sideSign * spDist)
                val spY = posY + normY * (sideSign * spDist)
                val spRad = if (random.nextFloat() > 0.8f) (baseWidth * 0.12f + 1.8f * random.nextFloat()).coerceIn(1.8f, 5.0f)
                else (baseWidth * 0.07f + 1.1f * random.nextFloat()).coerceIn(0.9f, 2.8f)

                fillPaint.alpha = (baseAlpha * 0.88f).toInt().coerceIn(1, 255)
                canvas.drawCircle(spX, spY, spRad, fillPaint)
            }

            currentDist += stepSpacing
            distInSeg += stepSpacing
        }

        if (leftBoundary.isEmpty() || rightBoundary.isEmpty()) return

        // Interior wash polygon
        val washPath = android.graphics.Path()
        washPath.moveTo(leftBoundary[0].x, leftBoundary[0].y)
        for (i in 1 until leftBoundary.size) {
            val prev = leftBoundary[i - 1]
            val curr = leftBoundary[i]
            washPath.quadTo(prev.x, prev.y, (prev.x + curr.x) * 0.5f, (prev.y + curr.y) * 0.5f)
        }
        washPath.lineTo(leftBoundary.last().x, leftBoundary.last().y)
        washPath.lineTo(rightBoundary.last().x, rightBoundary.last().y)
        for (i in rightBoundary.size - 2 downTo 0) {
            val prev = rightBoundary[i + 1]
            val curr = rightBoundary[i]
            washPath.quadTo(prev.x, prev.y, (prev.x + curr.x) * 0.5f, (prev.y + curr.y) * 0.5f)
        }
        washPath.lineTo(rightBoundary[0].x, rightBoundary[0].y)
        washPath.close()

        fillPaint.alpha = (baseAlpha * 0.16f).toInt().coerceIn(1, 255)
        canvas.drawPath(washPath, fillPaint)

        // Mottled puddles inside
        for (i in 0 until centerSamples.size step 2) {
            val c = centerSamples[i]
            val r = radiiSamples[i]
            fillPaint.alpha = (baseAlpha * 0.09f).toInt().coerceIn(1, 255)
            canvas.drawCircle(c.x, c.y, r * 0.5f, fillPaint)
        }

        // Dark wet-edge rim
        strokePaint.strokeWidth = (baseWidth * 0.18f).coerceIn(1.8f, 5.0f)
        strokePaint.alpha = (baseAlpha * 0.85f).toInt().coerceIn(1, 255)

        val leftRim = android.graphics.Path()
        leftRim.moveTo(leftBoundary[0].x, leftBoundary[0].y)
        for (i in 1 until leftBoundary.size) {
            val prev = leftBoundary[i - 1]
            val curr = leftBoundary[i]
            leftRim.quadTo(prev.x, prev.y, (prev.x + curr.x) * 0.5f, (prev.y + curr.y) * 0.5f)
        }
        leftRim.lineTo(leftBoundary.last().x, leftBoundary.last().y)
        canvas.drawPath(leftRim, strokePaint)

        val rightRim = android.graphics.Path()
        rightRim.moveTo(rightBoundary[0].x, rightBoundary[0].y)
        for (i in 1 until rightBoundary.size) {
            val prev = rightBoundary[i - 1]
            val curr = rightBoundary[i]
            rightRim.quadTo(prev.x, prev.y, (prev.x + curr.x) * 0.5f, (prev.y + curr.y) * 0.5f)
        }
        rightRim.lineTo(rightBoundary.last().x, rightBoundary.last().y)
        canvas.drawPath(rightRim, strokePaint)
    }
}
