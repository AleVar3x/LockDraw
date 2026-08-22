package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    floatingReactions: List<FloatingHeartReaction>,
    onStartDraw: (Float, Float) -> Unit,
    onContinueDraw: (Float, Float) -> Unit,
    onFinishDraw: () -> Unit,
    onSelectSticker: (String?) -> Unit,
    onUpdateStickerPos: (String, Float, Float) -> Unit,
    onUpdateStickerTransform: (String, Float, Float) -> Unit,
    onDeleteSticker: (String) -> Unit,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true
) {
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
                renderStroke(stroke, size.width, size.height)
            }

            // Render current user draft stroke
            currentDraft?.let { stroke ->
                renderStroke(stroke, size.width, size.height)
            }

            // Render partner draft stroke in real-time
            partnerDraft?.let { stroke ->
                renderStroke(stroke, size.width, size.height)
            }
        }

        // Stickers Layer on top of Canvas
        stickers.forEach { sticker ->
            val isSelected = isInteractive && sticker.id == selectedStickerId
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
                    .pointerInput(sticker.id) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            val safeW = if (canvasWidth > 0f) canvasWidth else 1f
                            val safeH = if (canvasHeight > 0f) canvasHeight else 1f
                            val newX = ((stickerPxX + pan.x) / safeW).coerceIn(0f, 1f)
                            val newY = ((stickerPxY + pan.y) / safeH).coerceIn(0f, 1f)
                            onUpdateStickerPos(sticker.id, newX, newY)
                            if (zoom != 1f || rotation != 0f) {
                                onUpdateStickerTransform(sticker.id, zoom, rotation)
                            }
                            onSelectSticker(sticker.id)
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

                if (sticker.content.length > 4) {
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

private fun DrawScope.renderStroke(stroke: DrawingStroke, canvasW: Float, canvasH: Float) {
    if (stroke.points.isEmpty()) return
    val baseColor = Color(stroke.colorArgb).copy(alpha = stroke.alpha)

    if (stroke.points.size == 1) {
        val pt = stroke.points[0]
        drawCircle(
            color = baseColor,
            radius = stroke.strokeWidth / 2f,
            center = Offset(pt.x * canvasW, pt.y * canvasH)
        )
        return
    }

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

    when (stroke.brushType) {
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
            // Neon outer wide diffuse aura
            drawPath(
                path = path,
                color = baseColor.copy(alpha = (stroke.alpha * 0.25f).coerceIn(0.05f, 0.4f)),
                style = Stroke(
                    width = stroke.strokeWidth * 4.2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            // Neon intense mid aura
            drawPath(
                path = path,
                color = baseColor.copy(alpha = (stroke.alpha * 0.6f).coerceIn(0.1f, 0.85f)),
                style = Stroke(
                    width = stroke.strokeWidth * 2.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            // Neon saturated edge beam
            drawPath(
                path = path,
                color = baseColor.copy(alpha = (stroke.alpha * 0.95f).coerceIn(0.2f, 1.0f)),
                style = Stroke(
                    width = stroke.strokeWidth * 1.3f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            // Neon pure brilliant laser core
            drawPath(
                path = path,
                color = Color.White.copy(alpha = (stroke.alpha * 0.98f).coerceIn(0.3f, 1.0f)),
                style = Stroke(
                    width = (stroke.strokeWidth * 0.55f).coerceAtLeast(2f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        BrushType.RAINBOW -> {
            val rainbowColors = listOf(
                Color(0xFFFF0055),
                Color(0xFFFF7700),
                Color(0xFFFFEE00),
                Color(0xFF00FF66),
                Color(0xFF00DDFF),
                Color(0xFF9900FF)
            )
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
        BrushType.ERASER -> {
            // Render active eraser cursor ring for real-time visual touch feedback
            val lastPt = stroke.points.lastOrNull()
            if (lastPt != null) {
                val cx = lastPt.x * canvasW
                val cy = lastPt.y * canvasH
                val radius = ((stroke.strokeWidth / 450f).coerceIn(0.035f, 0.16f) * canvasW).coerceAtLeast(18f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = radius,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFFFF2A6D),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.5f)
                )
            }
        }
    }
}
