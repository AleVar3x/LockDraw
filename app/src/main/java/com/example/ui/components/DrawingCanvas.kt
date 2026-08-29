package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrushType
import com.example.data.model.DrawingPoint
import com.example.data.model.DrawingStroke
import com.example.data.model.PlacedSticker
import com.example.data.model.StrokeModifier
import com.example.data.repository.FloatingHeartReaction
import com.example.data.sync.PartnerPresence
import kotlin.math.roundToInt

@Composable
fun DrawingCanvas(
    strokes: List<DrawingStroke>,
    currentDraft: DrawingStroke?,
    partnerDraft: DrawingStroke?,
    stickers: List<PlacedSticker>,
    selectedStickerId: String?,
    partnerPresence: PartnerPresence,
    floatingReactions: List<FloatingHeartReaction> = emptyList(),
    isMyDrawingsTransparent: Boolean = false,
    myDeviceId: String = "",
    myName: String = "Tu",
    partnerName: String = "Partner",
    onStartDraw: (Float, Float) -> Unit,
    onContinueDraw: (Float, Float) -> Unit,
    onFinishDraw: () -> Unit,
    onSelectSticker: (String?) -> Unit,
    onUpdateStickerPos: (String, Float, Float) -> Unit,
    onUpdateStickerTransform: (String, Float, Float) -> Unit,
    onDeleteSticker: (String) -> Unit,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    onUpdateStickerDelta: ((String, Float, Float) -> Unit)? = null
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "neon_pulse_trans")
    val neonPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.35f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1100, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "neon_pulse_val"
    )
    val waveAnimationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1400, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "wave_phase_val"
    )
    val dotFlowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1400, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "dot_flow_val"
    )

    val myAlphaMultiplier = if (isMyDrawingsTransparent) 0.20f else 1.0f
    val effectiveMyName = myName.ifBlank { "Tu" }
    val effectivePartnerName = partnerPresence.partnerName.ifBlank { partnerName.ifBlank { "Partner" } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("drawing_canvas_container")
    ) {
        val canvasWidth = if (constraints.maxWidth > 0) constraints.maxWidth.toFloat() else 1080f
        val canvasHeight = if (constraints.maxHeight > 0) constraints.maxHeight.toFloat() else 1920f

        // Maintain global chronological order across all users using createdAt
        val sortedStrokes = remember(strokes) { strokes.sortedBy { it.createdAt } }
        val sortedStickers = remember(stickers) { stickers.sortedBy { it.createdAt } }

        // --- 1. UNIFIED CHRONOLOGICAL DRAWING CANVAS ---
        // Single unified canvas maintaining true progressive draw order across all layers
        Canvas(
            modifier = if (isInteractive) {
                Modifier
                    .fillMaxSize()
                    .testTag("drawing_canvas")
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val safeW = if (canvasWidth > 0f) canvasWidth else 1f
                                val safeH = if (canvasHeight > 0f) canvasHeight else 1f
                                val normX = (offset.x / safeW).coerceIn(0f, 1f)
                                val normY = (offset.y / safeH).coerceIn(0f, 1f)
                                onStartDraw(normX, normY)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val safeW = if (canvasWidth > 0f) canvasWidth else 1f
                                val safeH = if (canvasHeight > 0f) canvasHeight else 1f
                                val normX = (change.position.x / safeW).coerceIn(0f, 1f)
                                val normY = (change.position.y / safeH).coerceIn(0f, 1f)
                                onContinueDraw(normX, normY)
                            },
                            onDragEnd = {
                                onFinishDraw()
                            },
                            onDragCancel = {
                                onFinishDraw()
                            }
                        )
                    }
            } else {
                Modifier
                    .fillMaxSize()
                    .testTag("drawing_canvas")
            }
        ) {
            // Render all strokes strictly by creation timestamp with smooth curves
            sortedStrokes.forEach { stroke ->
                val isMyStroke = if (myDeviceId.isNotBlank()) stroke.authorId == myDeviceId else stroke.authorId != "partner"
                val alphaMult = if (isMyStroke) myAlphaMultiplier else 1.0f
                renderStroke(stroke, size.width, size.height, neonPulse, waveAnimationPhase, dotFlowPhase, alphaMult)
            }

            // Real-time live in-progress drafts
            partnerDraft?.let { stroke ->
                renderStroke(stroke, size.width, size.height, neonPulse, waveAnimationPhase, dotFlowPhase, 1.0f)
            }
            currentDraft?.let { stroke ->
                renderStroke(stroke, size.width, size.height, neonPulse, waveAnimationPhase, dotFlowPhase, myAlphaMultiplier)
            }
        }

        // --- 2. AUTHOR LABELS NEXT TO STROKES ---
        // Display author tag beside the end of strokes to identify who drew what
        // Apply label ONLY to the last stroke made by each author (non-cluttering & allows drawing over/under)
        val strokeAuthorBadges = remember(sortedStrokes, myDeviceId, effectiveMyName, effectivePartnerName) {
            val nonEraserStrokes = sortedStrokes.filter { it.brushType != BrushType.ERASER && it.points.isNotEmpty() }
            val myLast = nonEraserStrokes.lastOrNull { if (myDeviceId.isNotBlank()) it.authorId == myDeviceId else it.authorId != "partner" }
            val partnerLast = nonEraserStrokes.lastOrNull { if (myDeviceId.isNotBlank()) it.authorId != myDeviceId else it.authorId == "partner" }
            listOfNotNull(myLast, partnerLast).mapNotNull { s ->
                val lastPt = s.points.lastOrNull() ?: return@mapNotNull null
                val isMine = if (myDeviceId.isNotBlank()) s.authorId == myDeviceId else s.authorId != "partner"
                val name = if (isMine) effectiveMyName else effectivePartnerName
                StrokeBadgeInfo(
                    strokeId = s.id,
                    x = lastPt.x,
                    y = lastPt.y,
                    authorName = name,
                    isMine = isMine,
                    colorArgb = s.colorArgb
                )
            }
        }

        strokeAuthorBadges.forEach { badge ->
            val isMyStroke = badge.isMine
            val alphaMult = if (isMyStroke) myAlphaMultiplier else 1.0f
            val pxX = (badge.x * canvasWidth + 8f).coerceIn(8f, canvasWidth - 110f)
            val pxY = (badge.y * canvasHeight - 16f).coerceIn(8f, canvasHeight - 36f)

            Box(
                modifier = Modifier
                    .offset { IntOffset(pxX.roundToInt(), pxY.roundToInt()) }
                    .graphicsLayer { alpha = alphaMult * 0.9f }
            ) {
                Row(
                    modifier = Modifier
                        .background(Color(0xDD121324), RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            if (badge.isMine) Color(0x667C4DFF) else Color(0x66FF2A6D),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(badge.colorArgb))
                    )
                    Text(
                        text = badge.authorName,
                        color = if (badge.isMine) Color(0xFFD0BCFF) else Color(0xFFFF80AB),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Live Drawing Tag for Current User Draft
        currentDraft?.points?.lastOrNull()?.let { curPt ->
            if (currentDraft.brushType != BrushType.ERASER) {
                val livePxX = (curPt.x * canvasWidth + 14f).coerceIn(10f, canvasWidth - 90f)
                val livePxY = (curPt.y * canvasHeight - 20f).coerceIn(10f, canvasHeight - 40f)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(livePxX.roundToInt(), livePxY.roundToInt()) }
                        .graphicsLayer { alpha = 0.95f }
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xF07C4DFF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "✏️ $effectiveMyName",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Live Drawing Tag for Partner Draft
        partnerDraft?.points?.lastOrNull()?.let { pPt ->
            if (partnerDraft.brushType != BrushType.ERASER) {
                val livePxX = (pPt.x * canvasWidth + 14f).coerceIn(10f, canvasWidth - 90f)
                val livePxY = (pPt.y * canvasHeight - 20f).coerceIn(10f, canvasHeight - 40f)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(livePxX.roundToInt(), livePxY.roundToInt()) }
                        .graphicsLayer { alpha = 0.95f }
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xF0FF2A6D), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "✏️ $effectivePartnerName",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- 3. PLACED STICKERS (Rendered in chronological sequence) ---
        sortedStickers.forEach { sticker ->
            val isMySticker = if (myDeviceId.isNotBlank()) sticker.authorId == myDeviceId else sticker.authorId != "partner"
            val stickerAlpha = if (isMySticker) myAlphaMultiplier else 1.0f
            PlacedStickerItem(
                sticker = sticker,
                isSelected = isInteractive && sticker.id == selectedStickerId,
                isInteractive = isInteractive,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                alphaMultiplier = stickerAlpha,
                onSelectSticker = onSelectSticker,
                onUpdateStickerPos = onUpdateStickerPos,
                onUpdateStickerDelta = onUpdateStickerDelta,
                onUpdateStickerTransform = onUpdateStickerTransform,
                onDeleteSticker = onDeleteSticker
            )
        }

        // Real-time Partner Live Pointer / Cursor Indicator
        AnimatedVisibility(
            visible = partnerPresence.isOnline && partnerPresence.cursorX >= 0f && partnerPresence.cursorY >= 0f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            val partnerPxX = partnerPresence.cursorX * canvasWidth
            val partnerPxY = partnerPresence.cursorY * canvasHeight

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(partnerPxX.roundToInt(), partnerPxY.roundToInt())
                    }
                    .size(60.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Surface(
                    color = Color(0xFFFF007F),
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✏️ ${partnerPresence.partnerName.ifBlank { "Partner" }}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Floating Hearts / Reaction Bursts
        floatingReactions.forEach { reaction ->
            val rx = reaction.x * canvasWidth
            val ry = reaction.y * canvasHeight

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset((rx - 24.dp.toPx()).roundToInt(), (ry - 24.dp.toPx()).roundToInt())
                    }
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = reaction.emoji,
                    fontSize = 32.sp
                )
            }
        }
    }
}

private data class StrokeBadgeInfo(
    val strokeId: String,
    val x: Float,
    val y: Float,
    val authorName: String,
    val isMine: Boolean,
    val colorArgb: Int
)

/**
 * Weighted moving average filter to smooth raw touch points and eliminate jitter.
 */
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

/**
 * Builds a natural, continuous smooth Bezier path from points.
 */
private fun buildSmoothPath(points: List<DrawingPoint>, canvasW: Float, canvasH: Float): Path {
    val path = Path()
    if (points.isEmpty()) return path

    val smoothed = if (points.size > 2) smoothPoints(points) else points
    val first = smoothed[0]
    path.moveTo(first.x * canvasW, first.y * canvasH)

    if (smoothed.size == 2) {
        val second = smoothed[1]
        path.lineTo(second.x * canvasW, second.y * canvasH)
        return path
    }

    for (i in 1 until smoothed.size) {
        val prev = smoothed[i - 1]
        val curr = smoothed[i]
        val midX = ((prev.x + curr.x) / 2f) * canvasW
        val midY = ((prev.y + curr.y) / 2f) * canvasH
        path.quadraticTo(prev.x * canvasW, prev.y * canvasH, midX, midY)
    }
    val last = smoothed.last()
    path.lineTo(last.x * canvasW, last.y * canvasH)
    return path
}

private fun DrawScope.renderStroke(
    stroke: DrawingStroke,
    canvasW: Float,
    canvasH: Float,
    neonPulse: Float = 1.0f,
    animatedWavePhase: Float = 0f,
    dotFlowPhase: Float = 0f,
    alphaMultiplier: Float = 1.0f
) {
    if (stroke.points.isEmpty()) return
    val scaledAlpha = (stroke.alpha * alphaMultiplier).coerceIn(0.01f, 1.0f)
    val effectiveStroke = if (alphaMultiplier < 0.99f) stroke.copy(alpha = scaledAlpha) else stroke
    val baseColor = Color(effectiveStroke.colorArgb).copy(alpha = scaledAlpha)

    if (effectiveStroke.points.size == 1) {
        val pt = effectiveStroke.points[0]
        drawCircle(
            color = baseColor,
            radius = effectiveStroke.strokeWidth / 2f,
            center = Offset(pt.x * canvasW, pt.y * canvasH)
        )
        return
    }

    if (effectiveStroke.brushType == BrushType.ERASER) {
        val lastPt = effectiveStroke.points.lastOrNull()
        if (lastPt != null) {
            val cx = lastPt.x * canvasW
            val cy = lastPt.y * canvasH
            val radius = ((effectiveStroke.strokeWidth / 450f).coerceIn(0.035f, 0.16f) * canvasW).coerceAtLeast(18f)
            drawCircle(
                color = Color.White.copy(alpha = (0.35f * alphaMultiplier).coerceIn(0.02f, 1f)),
                radius = radius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFFFF2A6D).copy(alpha = (1.0f * alphaMultiplier).coerceIn(0.05f, 1f)),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f)
            )
        }
        return
    }

    // Determine effective modifier & brush type
    val effectiveModifier = when {
        stroke.modifier != StrokeModifier.NONE -> stroke.modifier
        stroke.brushType == BrushType.ANIMATED_WAVE -> StrokeModifier.WAVE
        stroke.brushType == BrushType.PULSING_NEON -> StrokeModifier.PULSING
        stroke.brushType == BrushType.DOT_FLOW -> StrokeModifier.DOT_FLOW
        else -> StrokeModifier.NONE
    }

    val effectiveBrush = when (stroke.brushType) {
        BrushType.ANIMATED_WAVE -> BrushType.PEN
        BrushType.PULSING_NEON -> BrushType.NEON
        BrushType.DOT_FLOW -> BrushType.DOTTED
        else -> stroke.brushType
    }

    // Standard smooth interpolated path with noise filter and continuous Bezier curve
    val path = buildSmoothPath(effectiveStroke.points, canvasW, canvasH)

    val rainbowColors = listOf(
        Color(0xFFFF0055),
        Color(0xFFFF7700),
        Color(0xFFFFEE00),
        Color(0xFF00FF66),
        Color(0xFF00DDFF),
        Color(0xFF9900FF)
    )
    val effectiveRainbowColors = rainbowColors.map { it.copy(alpha = it.alpha * scaledAlpha) }

    when (effectiveModifier) {
        StrokeModifier.DOT_FLOW -> {
            // Live flowing dots moving with same cycle rate as wave
            val dotWidth = (stroke.strokeWidth * 0.35f).coerceIn(2f, 8f)
            val gapWidth = stroke.strokeWidth * 1.8f
            val patternPeriod = dotWidth + gapWidth
            val dynamicPhase = dotFlowPhase * patternPeriod

            // Soft glowing aura
            drawPath(
                path = path,
                color = baseColor.copy(alpha = (scaledAlpha * 0.45f).coerceIn(0.01f, 1.0f)),
                style = Stroke(
                    width = stroke.strokeWidth * 1.75f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(dotWidth, gapWidth),
                        dynamicPhase
                    )
                )
            )

            // Primary vivid flowing dots
            drawPath(
                path = path,
                color = baseColor,
                style = Stroke(
                    width = stroke.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(dotWidth, gapWidth),
                        dynamicPhase
                    )
                )
            )

            // Crisp glowing core sparkle for each moving dot
            drawPath(
                path = path,
                color = Color.White.copy(alpha = (scaledAlpha * 0.90f).coerceIn(0.01f, 1.0f)),
                style = Stroke(
                    width = (stroke.strokeWidth * 0.42f).coerceAtLeast(1.5f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(dotWidth * 0.75f, gapWidth + dotWidth * 0.25f),
                        dynamicPhase
                    )
                )
            )
        }

        StrokeModifier.WAVE -> {
            // Animated flowing sine wave along smoothed stroke length
            val animatedWavyPath = Path()
            val smoothedPts = if (effectiveStroke.points.size > 2) smoothPoints(effectiveStroke.points) else effectiveStroke.points
            val firstPt = smoothedPts[0]
            animatedWavyPath.moveTo(firstPt.x * canvasW, firstPt.y * canvasH)

            var accumulatedDist = 0f
            val frequency = 0.095f
            val amplitude = (effectiveStroke.strokeWidth * 0.88f).coerceIn(6f, 26f)

            for (i in 1 until smoothedPts.size) {
                val p0 = smoothedPts[i - 1]
                val p1 = smoothedPts[i]
                val x0 = p0.x * canvasW
                val y0 = p0.y * canvasH
                val x1 = p1.x * canvasW
                val y1 = p1.y * canvasH

                val dx = x1 - x0
                val dy = y1 - y0
                val segLen = kotlin.math.hypot(dx, dy)
                if (segLen <= 0.001f) continue

                val steps = (segLen / 4f).toInt().coerceIn(1, 50)
                val perpX = -dy / segLen
                val perpY = dx / segLen

                for (s in 1..steps) {
                    val t = s.toFloat() / steps
                    val currentDist = accumulatedDist + segLen * t
                    val waveOffset = (kotlin.math.sin(currentDist * frequency - animatedWavePhase) * amplitude).toFloat()
                    val ix = x0 + dx * t + perpX * waveOffset
                    val iy = y0 + dy * t + perpY * waveOffset
                    animatedWavyPath.lineTo(ix, iy)
                }
                accumulatedDist += segLen
            }

            when (effectiveBrush) {
                BrushType.NEON -> {
                    drawPath(
                        path = animatedWavyPath,
                        color = baseColor.copy(alpha = (scaledAlpha * 0.30f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = stroke.strokeWidth * 3.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = animatedWavyPath,
                        color = baseColor.copy(alpha = (scaledAlpha * 0.70f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = stroke.strokeWidth * 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = animatedWavyPath,
                        color = Color.White.copy(alpha = (scaledAlpha * 0.95f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = (stroke.strokeWidth * 0.55f).coerceAtLeast(2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                BrushType.RAINBOW -> {
                    drawPath(
                        path = animatedWavyPath,
                        brush = Brush.linearGradient(colors = effectiveRainbowColors, start = Offset(0f, 0f), end = Offset(canvasW, canvasH)),
                        style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = animatedWavyPath,
                        color = Color.White.copy(alpha = (scaledAlpha * 0.85f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = (stroke.strokeWidth * 0.35f).coerceAtLeast(1.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                BrushType.HIGHLIGHTER -> {
                    drawPath(
                        path = animatedWavyPath,
                        color = baseColor.copy(alpha = (scaledAlpha * 0.42f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = stroke.strokeWidth * 2.2f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
                    )
                }
                BrushType.PENCIL -> {
                    drawPath(
                        path = animatedWavyPath,
                        color = baseColor.copy(alpha = (scaledAlpha * 0.75f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(
                            width = stroke.strokeWidth * 0.8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 4f), 0f)
                        )
                    )
                }
                else -> {
                    // PEN & Default
                    drawPath(
                        path = animatedWavyPath,
                        color = baseColor.copy(alpha = (scaledAlpha * 0.45f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = stroke.strokeWidth * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = animatedWavyPath,
                        color = baseColor,
                        style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = animatedWavyPath,
                        color = Color.White.copy(alpha = (scaledAlpha * 0.90f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = (stroke.strokeWidth * 0.38f).coerceAtLeast(1.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }

        StrokeModifier.PULSING -> {
            // Live pulsing animated breathing effect
            when (effectiveBrush) {
                BrushType.NEON -> {
                    val outerWidth = stroke.strokeWidth * (4.5f * neonPulse)
                    val outerAlpha = (scaledAlpha * 0.35f * neonPulse).coerceIn(0.01f, 1.0f)
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = outerAlpha),
                        style = Stroke(width = outerWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    val midWidth = stroke.strokeWidth * (2.4f * neonPulse)
                    val midAlpha = (scaledAlpha * 0.75f * neonPulse).coerceIn(0.01f, 1.0f)
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = midAlpha),
                        style = Stroke(width = midWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = (scaledAlpha * 0.95f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = (stroke.strokeWidth * 0.7f).coerceAtLeast(3f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                BrushType.RAINBOW -> {
                    val pulseWidth = stroke.strokeWidth * (0.85f + 0.3f * neonPulse)
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(colors = effectiveRainbowColors, start = Offset(0f, 0f), end = Offset(canvasW, canvasH)),
                        style = Stroke(width = pulseWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = (scaledAlpha * 0.6f * neonPulse).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = pulseWidth * 0.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                BrushType.HIGHLIGHTER -> {
                    val pulseAlpha = (scaledAlpha * 0.45f * neonPulse).coerceIn(0.01f, 1.0f)
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = pulseAlpha),
                        style = Stroke(width = stroke.strokeWidth * (2.0f + 0.4f * neonPulse), cap = StrokeCap.Square, join = StrokeJoin.Miter)
                    )
                }
                BrushType.PENCIL -> {
                    val pulseAlpha = (scaledAlpha * 0.75f * neonPulse).coerceIn(0.01f, 1.0f)
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = pulseAlpha),
                        style = Stroke(
                            width = stroke.strokeWidth * 0.85f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 4f), 0f)
                        )
                    )
                }
                else -> {
                    // PEN & Default
                    val outerWidth = stroke.strokeWidth * (2.4f * neonPulse)
                    val outerAlpha = (scaledAlpha * 0.4f * neonPulse).coerceIn(0.01f, 1.0f)
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = outerAlpha),
                        style = Stroke(width = outerWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = baseColor,
                        style = Stroke(width = stroke.strokeWidth * (0.9f + 0.2f * neonPulse), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }

        StrokeModifier.NONE -> {
            when (effectiveBrush) {
                BrushType.PEN -> {
                    drawPath(
                        path = path,
                        color = baseColor,
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                BrushType.PENCIL -> {
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = (scaledAlpha * 0.7f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(
                            width = stroke.strokeWidth * 0.8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 4f), 0f)
                        )
                    )
                }
                BrushType.HIGHLIGHTER -> {
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = (scaledAlpha * 0.38f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(
                            width = stroke.strokeWidth * 2.2f,
                            cap = StrokeCap.Square,
                            join = StrokeJoin.Miter
                        ),
                        blendMode = BlendMode.SrcOver
                    )
                }
                BrushType.NEON -> {
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = (scaledAlpha * 0.25f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = stroke.strokeWidth * 4.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = (scaledAlpha * 0.60f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = stroke.strokeWidth * 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = (scaledAlpha * 0.95f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = stroke.strokeWidth * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = (scaledAlpha * 0.90f).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = (stroke.strokeWidth * 0.55f).coerceAtLeast(2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                BrushType.WAVY -> {
                    val wavyPath = Path()
                    val firstPt = stroke.points[0]
                    wavyPath.moveTo(firstPt.x * canvasW, firstPt.y * canvasH)

                    var accumulatedDist = 0f
                    val frequency = 0.08f
                    val amplitude = (stroke.strokeWidth * 0.75f).coerceIn(4f, 22f)

                    for (i in 1 until stroke.points.size) {
                        val p0 = stroke.points[i - 1]
                        val p1 = stroke.points[i]
                        val x0 = p0.x * canvasW
                        val y0 = p0.y * canvasH
                        val x1 = p1.x * canvasW
                        val y1 = p1.y * canvasH

                        val dx = x1 - x0
                        val dy = y1 - y0
                        val segLen = kotlin.math.hypot(dx, dy)
                        if (segLen <= 0.001f) continue

                        val steps = (segLen / 5f).toInt().coerceIn(1, 40)
                        val perpX = -dy / segLen
                        val perpY = dx / segLen

                        for (s in 1..steps) {
                            val t = s.toFloat() / steps
                            val currentDist = accumulatedDist + segLen * t
                            val waveOffset = (kotlin.math.sin(currentDist * frequency) * amplitude).toFloat()
                            val ix = x0 + dx * t + perpX * waveOffset
                            val iy = y0 + dy * t + perpY * waveOffset
                            wavyPath.lineTo(ix, iy)
                        }
                        accumulatedDist += segLen
                    }

                    drawPath(
                        path = wavyPath,
                        color = baseColor,
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                BrushType.RAINBOW -> {
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = effectiveRainbowColors,
                            start = Offset(0f, 0f),
                            end = Offset(canvasW, canvasH)
                        ),
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                BrushType.DOTTED -> {
                    drawPath(
                        path = path,
                        color = baseColor,
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, stroke.strokeWidth * 1.6f), 0f)
                        )
                    )
                }
                else -> {
                    drawPath(
                        path = path,
                        color = baseColor,
                        style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun PlacedStickerItem(
    sticker: PlacedSticker,
    isSelected: Boolean,
    isInteractive: Boolean,
    canvasWidth: Float,
    canvasHeight: Float,
    alphaMultiplier: Float = 1.0f,
    onSelectSticker: (String?) -> Unit,
    onUpdateStickerPos: (String, Float, Float) -> Unit,
    onUpdateStickerDelta: ((String, Float, Float) -> Unit)?,
    onUpdateStickerTransform: (String, Float, Float) -> Unit,
    onDeleteSticker: (String) -> Unit
) {
    val currentSticker by rememberUpdatedState(sticker)
    val stickerPxX = sticker.x * canvasWidth
    val stickerPxY = sticker.y * canvasHeight

    val stickerModifier = if (isInteractive) {
        Modifier
            .offset {
                IntOffset(
                    (stickerPxX - 48.dp.toPx()).roundToInt(),
                    (stickerPxY - 48.dp.toPx()).roundToInt()
                )
            }
            .size(96.dp)
            .scale(sticker.scale)
            .rotate(sticker.rotation)
            .graphicsLayer { alpha = alphaMultiplier }
            .pointerInput(sticker.id) {
                detectTapGestures(
                    onTap = {
                        onSelectSticker(if (isSelected) null else currentSticker.id)
                    }
                )
            }
            .pointerInput(sticker.id) {
                detectTransformGestures(panZoomLock = false) { _, pan, zoom, rotation ->
                    onSelectSticker(currentSticker.id)
                    val safeW = if (canvasWidth > 0f) canvasWidth else 1f
                    val safeH = if (canvasHeight > 0f) canvasHeight else 1f
                    val dx = pan.x / safeW
                    val dy = pan.y / safeH
                    if (dx != 0f || dy != 0f) {
                        if (onUpdateStickerDelta != null) {
                            onUpdateStickerDelta(currentSticker.id, dx, dy)
                        } else {
                            val newX = (currentSticker.x + dx).coerceIn(0.02f, 0.98f)
                            val newY = (currentSticker.y + dy).coerceIn(0.02f, 0.98f)
                            onUpdateStickerPos(currentSticker.id, newX, newY)
                        }
                    }
                    if (zoom != 1f || rotation != 0f) {
                        onUpdateStickerTransform(currentSticker.id, zoom, rotation)
                    }
                }
            }
            .testTag("sticker_${sticker.id}")
    } else {
        Modifier
            .offset {
                IntOffset(
                    (stickerPxX - 48.dp.toPx()).roundToInt(),
                    (stickerPxY - 48.dp.toPx()).roundToInt()
                )
            }
            .size(96.dp)
            .scale(sticker.scale)
            .rotate(sticker.rotation)
            .graphicsLayer { alpha = alphaMultiplier }
            .testTag("sticker_${sticker.id}")
    }

    Box(
        modifier = stickerModifier,
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color(0xFFFF2A6D), RoundedCornerShape(12.dp))
                    .background(Color(0x22FF2A6D), RoundedCornerShape(12.dp))
            )
            // Delete button (Top End)
            IconButton(
                onClick = { onDeleteSticker(sticker.id) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .offset(x = 4.dp, y = (-4).dp)
                    .background(Color(0xFFE53935), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Elimina sticker",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Resize handle (Bottom End)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .offset(x = 4.dp, y = 4.dp)
                    .background(Color(0xFF7C4DFF), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
                    .pointerInput(sticker.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = (dragAmount.x + dragAmount.y) / 120f
                            val scaleDelta = 1f + delta
                            onUpdateStickerTransform(currentSticker.id, scaleDelta, 0f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = "Ridimensiona sticker",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Rotate handle (Bottom Start)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(28.dp)
                    .offset(x = (-4).dp, y = 4.dp)
                    .background(Color(0xFF00B0FF), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
                    .pointerInput(sticker.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val rotDelta = dragAmount.x * 0.8f
                            onUpdateStickerTransform(currentSticker.id, 1f, rotDelta)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = "Ruota sticker",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        if (sticker.content.startsWith("sticker_img:") || sticker.content.startsWith("file:") || sticker.content.startsWith("content:") || sticker.content.startsWith("data:image")) {
            val imageSource = sticker.content.removePrefix("sticker_img:")
            coil.compose.AsyncImage(
                model = imageSource,
                contentDescription = "Sticker Personalizzato",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else if (sticker.content.length > 4) {
            Surface(
                color = Color(0xDDFFFFFF),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.padding(6.dp)
            ) {
                Text(
                    text = sticker.content,
                    color = Color(0xFF1E1E1E),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        } else {
            Text(
                text = sticker.content,
                fontSize = 44.sp
            )
        }
    }
}

