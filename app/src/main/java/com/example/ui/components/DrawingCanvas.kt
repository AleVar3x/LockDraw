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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
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
        targetValue = 1000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(15000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "dot_flow_val"
    )

    val myAlphaMultiplier = if (isMyDrawingsTransparent) 0.20f else 1.0f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("drawing_canvas_container")
    ) {
        val canvasWidth = if (constraints.maxWidth > 0) constraints.maxWidth.toFloat() else 1080f
        val canvasHeight = if (constraints.maxHeight > 0) constraints.maxHeight.toFloat() else 1920f

        // Drawing Gesture & Canvas Layer
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
            // Render completed strokes
            strokes.forEach { stroke ->
                val isMine = stroke.authorId != "partner"
                val alphaMult = if (isMine) myAlphaMultiplier else 1.0f
                renderStroke(stroke, size.width, size.height, neonPulse, waveAnimationPhase, dotFlowPhase, alphaMult)
            }

            // Render current user draft stroke
            currentDraft?.let { stroke ->
                renderStroke(stroke, size.width, size.height, neonPulse, waveAnimationPhase, dotFlowPhase, myAlphaMultiplier)
            }

            // Render partner draft stroke in real-time
            partnerDraft?.let { stroke ->
                renderStroke(stroke, size.width, size.height, neonPulse, waveAnimationPhase, dotFlowPhase, 1.0f)
            }
        }

        // Stickers Layer on top of Canvas
        stickers.forEach { sticker ->
            val isSelected = isInteractive && sticker.id == selectedStickerId
            val currentSticker by rememberUpdatedState(sticker)
            val stickerPxX = sticker.x * canvasWidth
            val stickerPxY = sticker.y * canvasHeight
            val isMine = sticker.authorId != "partner"
            val stickerAlpha = if (isMine) myAlphaMultiplier else 1.0f

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
                    .graphicsLayer { alpha = stickerAlpha }
                    .pointerInput(sticker.id) {
                        detectDragGestures(
                            onDragStart = {
                                onSelectSticker(currentSticker.id)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val safeW = if (canvasWidth > 0f) canvasWidth else 1f
                                val safeH = if (canvasHeight > 0f) canvasHeight else 1f
                                val dx = dragAmount.x / safeW
                                val dy = dragAmount.y / safeH
                                if (dx != 0f || dy != 0f) {
                                    if (onUpdateStickerDelta != null) {
                                        onUpdateStickerDelta(currentSticker.id, dx, dy)
                                    } else {
                                        val newX = (currentSticker.x + dx).coerceIn(0.02f, 0.98f)
                                        val newY = (currentSticker.y + dy).coerceIn(0.02f, 0.98f)
                                        onUpdateStickerPos(currentSticker.id, newX, newY)
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(sticker.id, isSelected) {
                        detectTransformGestures(panZoomLock = false) { _, _, zoom, rotation ->
                            if (zoom != 1f || rotation != 0f) {
                                onUpdateStickerTransform(currentSticker.id, zoom, rotation)
                            }
                        }
                    }
                    .clickable {
                        onSelectSticker(if (isSelected) null else currentSticker.id)
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
                    .graphicsLayer { alpha = stickerAlpha }
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
                            .border(1.5.dp, Color(0xFFFF2A6D), RoundedCornerShape(12.dp))
                            .background(Color(0x22FF2A6D), RoundedCornerShape(12.dp))
                    )
                    IconButton(
                        onClick = { onDeleteSticker(sticker.id) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(26.dp)
                            .background(Color(0xFFE53935), CircleShape)
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Elimina sticker",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
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
                    // Badge or love quote sticker
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
                    // Emoji / doodle sticker
                    Text(
                        text = sticker.content,
                        fontSize = 44.sp
                    )
                }
            }
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

    // Standard smooth interpolated path
    val path = Path()
    val first = stroke.points[0]
    path.moveTo(first.x * canvasW, first.y * canvasH)
    for (i in 1 until stroke.points.size) {
        val prev = stroke.points[i - 1]
        val current = stroke.points[i]
        val midX = ((prev.x + current.x) / 2f) * canvasW
        val midY = ((prev.y + current.y) / 2f) * canvasH
        path.quadraticTo(prev.x * canvasW, prev.y * canvasH, midX, midY)
    }
    val last = stroke.points.last()
    path.lineTo(last.x * canvasW, last.y * canvasH)

    val rainbowColors = listOf(
        Color(0xFFFF0055),
        Color(0xFFFF7700),
        Color(0xFFFFEE00),
        Color(0xFF00FF66),
        Color(0xFF00DDFF),
        Color(0xFF9900FF)
    )

    when (effectiveModifier) {
        StrokeModifier.DOT_FLOW -> {
            // Live flowing dots moving in inverted direction
            val dotWidth = (stroke.strokeWidth * 0.35f).coerceIn(2f, 8f)
            val gapWidth = stroke.strokeWidth * 1.8f
            // Inverted animation direction as requested: positive phase
            val dynamicPhase = (dotFlowPhase * (stroke.strokeWidth * 0.9f + 14f))

            // Soft glowing aura
            drawPath(
                path = path,
                color = baseColor.copy(alpha = (stroke.alpha * 0.45f).coerceIn(0.12f, 0.7f)),
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
                color = Color.White.copy(alpha = (stroke.alpha * 0.92f).coerceIn(0.35f, 1.0f)),
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
            // Animated flowing sine wave along stroke length
            val animatedWavyPath = Path()
            val firstPt = stroke.points[0]
            animatedWavyPath.moveTo(firstPt.x * canvasW, firstPt.y * canvasH)

            var accumulatedDist = 0f
            val frequency = 0.095f
            val amplitude = (stroke.strokeWidth * 0.88f).coerceIn(6f, 26f)

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
                        color = baseColor.copy(alpha = (stroke.alpha * 0.3f).coerceIn(0.08f, 0.5f)),
                        style = Stroke(width = stroke.strokeWidth * 3.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = animatedWavyPath,
                        color = baseColor.copy(alpha = (stroke.alpha * 0.7f).coerceIn(0.2f, 0.9f)),
                        style = Stroke(width = stroke.strokeWidth * 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = animatedWavyPath,
                        color = Color.White.copy(alpha = (stroke.alpha * 0.95f).coerceIn(0.4f, 1.0f)),
                        style = Stroke(width = (stroke.strokeWidth * 0.55f).coerceAtLeast(2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                BrushType.RAINBOW -> {
                    drawPath(
                        path = animatedWavyPath,
                        brush = Brush.linearGradient(colors = rainbowColors, start = Offset(0f, 0f), end = Offset(canvasW, canvasH)),
                        style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = animatedWavyPath,
                        color = Color.White.copy(alpha = (stroke.alpha * 0.85f).coerceIn(0.2f, 0.9f)),
                        style = Stroke(width = (stroke.strokeWidth * 0.35f).coerceAtLeast(1.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                BrushType.HIGHLIGHTER -> {
                    drawPath(
                        path = animatedWavyPath,
                        color = baseColor.copy(alpha = (stroke.alpha * 0.42f).coerceIn(0.1f, 0.65f)),
                        style = Stroke(width = stroke.strokeWidth * 2.2f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
                    )
                }
                BrushType.PENCIL -> {
                    drawPath(
                        path = animatedWavyPath,
                        color = baseColor.copy(alpha = (stroke.alpha * 0.75f).coerceIn(0.15f, 1f)),
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
                        color = baseColor.copy(alpha = (stroke.alpha * 0.45f).coerceIn(0.1f, 0.7f)),
                        style = Stroke(width = stroke.strokeWidth * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = animatedWavyPath,
                        color = baseColor,
                        style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = animatedWavyPath,
                        color = Color.White.copy(alpha = (stroke.alpha * 0.95f).coerceIn(0.3f, 1.0f)),
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
                    val outerAlpha = (stroke.alpha * 0.35f * neonPulse).coerceIn(0.08f, 0.7f)
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = outerAlpha),
                        style = Stroke(width = outerWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    val midWidth = stroke.strokeWidth * (2.4f * neonPulse)
                    val midAlpha = (stroke.alpha * 0.75f * neonPulse).coerceIn(0.2f, 0.95f)
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = midAlpha),
                        style = Stroke(width = midWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = (stroke.alpha * 0.98f).coerceIn(0.4f, 1.0f)),
                        style = Stroke(width = (stroke.strokeWidth * 0.7f).coerceAtLeast(3f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                BrushType.RAINBOW -> {
                    val pulseWidth = stroke.strokeWidth * (0.85f + 0.3f * neonPulse)
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(colors = rainbowColors, start = Offset(0f, 0f), end = Offset(canvasW, canvasH)),
                        style = Stroke(width = pulseWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = (stroke.alpha * 0.6f * neonPulse).coerceIn(0.1f, 0.8f)),
                        style = Stroke(width = pulseWidth * 0.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                BrushType.HIGHLIGHTER -> {
                    val pulseAlpha = (stroke.alpha * 0.45f * neonPulse).coerceIn(0.15f, 0.75f)
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = pulseAlpha),
                        style = Stroke(width = stroke.strokeWidth * (2.0f + 0.4f * neonPulse), cap = StrokeCap.Square, join = StrokeJoin.Miter)
                    )
                }
                BrushType.PENCIL -> {
                    val pulseAlpha = (stroke.alpha * 0.75f * neonPulse).coerceIn(0.2f, 1.0f)
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
                    val outerAlpha = (stroke.alpha * 0.4f * neonPulse).coerceIn(0.1f, 0.75f)
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
                        color = baseColor.copy(alpha = (stroke.alpha * 0.7f).coerceIn(0.1f, 1f)),
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
                        color = baseColor.copy(alpha = (stroke.alpha * 0.38f).coerceIn(0.1f, 0.6f)),
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
                        color = baseColor.copy(alpha = (stroke.alpha * 0.25f).coerceIn(0.05f, 0.4f)),
                        style = Stroke(width = stroke.strokeWidth * 4.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = (stroke.alpha * 0.6f).coerceIn(0.1f, 0.85f)),
                        style = Stroke(width = stroke.strokeWidth * 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = baseColor.copy(alpha = (stroke.alpha * 0.95f).coerceIn(0.2f, 1.0f)),
                        style = Stroke(width = stroke.strokeWidth * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = (stroke.alpha * 0.98f).coerceIn(0.3f, 1.0f)),
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
                            colors = rainbowColors,
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
