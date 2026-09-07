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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
    onUpdateStickerDelta: ((String, Float, Float) -> Unit)? = null,
    onCanvasSizeChanged: ((Float, Float) -> Unit)? = null
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
            animation = androidx.compose.animation.core.tween(5000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "wave_phase_val"
    )
    val dotFlowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "dot_flow_val"
    )

    val myAlphaMultiplier = if (isMyDrawingsTransparent) 0.20f else 1.0f
    val effectiveMyName = myName.ifBlank { "Tu" }
    val effectivePartnerName = partnerPresence.partnerName.ifBlank { partnerName.ifBlank { "Partner" } }

    val currentOnStartDraw by rememberUpdatedState(onStartDraw)
    val currentOnContinueDraw by rememberUpdatedState(onContinueDraw)
    val currentOnFinishDraw by rememberUpdatedState(onFinishDraw)

    // Pinch-to-zoom & two-finger panning state
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("drawing_canvas_container")
    ) {
        val canvasWidth = if (constraints.maxWidth > 0) constraints.maxWidth.toFloat() else 1080f
        val canvasHeight = if (constraints.maxHeight > 0) constraints.maxHeight.toFloat() else 1920f

        LaunchedEffect(canvasWidth, canvasHeight) {
            onCanvasSizeChanged?.invoke(canvasWidth, canvasHeight)
        }

        // Maintain global chronological order across all users using createdAt
        val sortedStrokes = remember(strokes) { strokes.sortedBy { it.createdAt } }
        val sortedStickers = remember(stickers) { stickers.sortedBy { it.createdAt } }

        // Partition strokes into chronological render chunks so static strokes are rendered into cached
        // off-screen ImageBitmaps, while animated strokes (wave, pulsing, dot flow, sparkling) render dynamically on top/between.
        val strokeChunks = remember(sortedStrokes) {
            val chunks = mutableListOf<StrokeChunk>()
            var currentStatic = mutableListOf<DrawingStroke>()

            sortedStrokes.forEach { stroke ->
                if (isStrokeAnimated(stroke)) {
                    if (currentStatic.isNotEmpty()) {
                        chunks.add(StrokeChunk.StaticChunk(currentStatic.toList()))
                        currentStatic = mutableListOf()
                    }
                    chunks.add(StrokeChunk.AnimatedChunk(stroke))
                } else {
                    currentStatic.add(stroke)
                }
            }
            if (currentStatic.isNotEmpty()) {
                chunks.add(StrokeChunk.StaticChunk(currentStatic.toList()))
            }
            chunks
        }

        // Cache static stroke chunks into ImageBitmaps.
        // Static strokes (especially Spray Cans and Watercolors with tens of thousands of micro-particles)
        // are rasterized exactly ONCE off-screen into an ImageBitmap and blitted directly by the GPU,
        // reducing per-frame draw operations from ~30,000 to 1, guaranteeing butter-smooth 60/120 FPS.
        val cachedChunkBitmaps = remember(strokeChunks, canvasWidth, canvasHeight, myAlphaMultiplier, myDeviceId) {
            val map = mutableMapOf<Int, ImageBitmap>()
            if (canvasWidth > 0f && canvasHeight > 0f) {
                val w = canvasWidth.toInt().coerceAtLeast(1)
                val h = canvasHeight.toInt().coerceAtLeast(1)
                val drawScope = CanvasDrawScope()

                strokeChunks.forEachIndexed { index, chunk ->
                    if (chunk is StrokeChunk.StaticChunk) {
                        val bitmap = ImageBitmap(w, h)
                        val canvas = Canvas(bitmap)
                        drawScope.draw(
                            density = androidx.compose.ui.unit.Density(1f),
                            layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
                            canvas = canvas,
                            size = androidx.compose.ui.geometry.Size(canvasWidth, canvasHeight)
                        ) {
                            chunk.strokes.forEach { stroke ->
                                val isMyStroke = if (myDeviceId.isNotBlank()) stroke.authorId == myDeviceId else stroke.authorId != "partner"
                                val alphaMult = if (isMyStroke) myAlphaMultiplier else 1.0f
                                renderStroke(
                                    stroke = stroke,
                                    canvasW = canvasWidth,
                                    canvasH = canvasHeight,
                                    neonPulse = 1.0f,
                                    animatedWavePhase = 0f,
                                    dotFlowPhase = 0f,
                                    alphaMultiplier = alphaMult,
                                    showCursorIndicator = false
                                )
                            }
                        }
                        map[index] = bitmap
                    }
                }
            }
            map
        }

        // --- TRANSFORMABLE CANVAS CONTENT (Zoomed & Panned) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffset.x
                    translationY = panOffset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        ) {
            // --- 1. UNIFIED CHRONOLOGICAL DRAWING CANVAS ---
            Canvas(
                modifier = if (isInteractive) {
                    Modifier
                        .fillMaxSize()
                        .testTag("drawing_canvas")
                        .pointerInput(canvasWidth, canvasHeight, isInteractive) {
                            awaitEachGesture {
                                val firstDown = awaitFirstDown(requireUnconsumed = false)
                                var isDrawing = false
                                var isMultiTouchTransforming = false

                                val initialPressed = currentEvent.changes.filter { it.pressed }
                                if (initialPressed.size >= 2) {
                                    isMultiTouchTransforming = true
                                } else {
                                    val normX = ((firstDown.position.x - panOffset.x) / zoomScale / canvasWidth).coerceIn(0f, 1f)
                                    val normY = ((firstDown.position.y - panOffset.y) / zoomScale / canvasHeight).coerceIn(0f, 1f)
                                    currentOnStartDraw(normX, normY)
                                    isDrawing = true
                                    firstDown.consume()
                                }

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressed = event.changes.filter { it.pressed }
                                    if (pressed.isEmpty()) break

                                    if (pressed.size >= 2) {
                                        if (isDrawing) {
                                            currentOnFinishDraw()
                                            isDrawing = false
                                        }
                                        isMultiTouchTransforming = true

                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        val centroid = event.calculateCentroid(useCurrent = false)

                                        if (zoom != 1f || pan != Offset.Zero) {
                                            val oldScale = zoomScale
                                            val newScale = (oldScale * zoom).coerceIn(1.0f, 6.0f)
                                            val maxPanX = (newScale - 1f) * canvasWidth
                                            val maxPanY = (newScale - 1f) * canvasHeight

                                            val scaleRatio = newScale / oldScale
                                            val newPanX = if (newScale <= 1.001f) 0f else {
                                                (centroid.x - (centroid.x - panOffset.x) * scaleRatio + pan.x).coerceIn(-maxPanX, 0f)
                                            }
                                            val newPanY = if (newScale <= 1.001f) 0f else {
                                                (centroid.y - (centroid.y - panOffset.y) * scaleRatio + pan.y).coerceIn(-maxPanY, 0f)
                                            }

                                            zoomScale = newScale
                                            panOffset = if (newScale <= 1.001f) Offset.Zero else Offset(newPanX, newPanY)
                                        }

                                        event.changes.forEach { it.consume() }
                                    } else if (pressed.size == 1 && !isMultiTouchTransforming) {
                                        val change = pressed[0]
                                        val normX = ((change.position.x - panOffset.x) / zoomScale / canvasWidth).coerceIn(0f, 1f)
                                        val normY = ((change.position.y - panOffset.y) / zoomScale / canvasHeight).coerceIn(0f, 1f)

                                        if (!isDrawing) {
                                            currentOnStartDraw(normX, normY)
                                            isDrawing = true
                                        } else {
                                            currentOnContinueDraw(normX, normY)
                                        }
                                        change.consume()
                                    }
                                }

                                if (isDrawing) {
                                    currentOnFinishDraw()
                                }
                            }
                        }
                } else {
                    Modifier
                        .fillMaxSize()
                        .testTag("drawing_canvas")
                }
            ) {
                // High-performance layered rendering:
                // 1. Static strokes (e.g. spray can, watercolor, pen, pencil) are pre-rendered into cached off-screen
                //    ImageBitmaps and blitted onto the screen in a single GPU operation (0.1ms).
                // 2. Animated strokes (wave, pulsing, dot flow, sparkling) are rendered dynamically with live phases.
                strokeChunks.forEachIndexed { index, chunk ->
                    when (chunk) {
                        is StrokeChunk.StaticChunk -> {
                            val cachedBitmap = cachedChunkBitmaps[index]
                            if (cachedBitmap != null) {
                                drawImage(cachedBitmap)
                            } else {
                                // Fallback if bitmap is not ready
                                chunk.strokes.forEach { stroke ->
                                    val isMyStroke = if (myDeviceId.isNotBlank()) stroke.authorId == myDeviceId else stroke.authorId != "partner"
                                    val alphaMult = if (isMyStroke) myAlphaMultiplier else 1.0f
                                    renderStroke(stroke, size.width, size.height, neonPulse, waveAnimationPhase, dotFlowPhase, alphaMult)
                                }
                            }
                        }
                        is StrokeChunk.AnimatedChunk -> {
                            val stroke = chunk.stroke
                            val isMyStroke = if (myDeviceId.isNotBlank()) stroke.authorId == myDeviceId else stroke.authorId != "partner"
                            val alphaMult = if (isMyStroke) myAlphaMultiplier else 1.0f
                            renderStroke(stroke, size.width, size.height, neonPulse, waveAnimationPhase, dotFlowPhase, alphaMult)
                        }
                    }
                }

                // Real-time live in-progress drafts with active radial cursor indicator (rendered in real-time)
                partnerDraft?.let { stroke ->
                    renderStroke(
                        stroke = stroke,
                        canvasW = size.width,
                        canvasH = size.height,
                        neonPulse = neonPulse,
                        animatedWavePhase = waveAnimationPhase,
                        dotFlowPhase = dotFlowPhase,
                        alphaMultiplier = 1.0f,
                        showCursorIndicator = true
                    )
                }
                currentDraft?.let { stroke ->
                    renderStroke(
                        stroke = stroke,
                        canvasW = size.width,
                        canvasH = size.height,
                        neonPulse = neonPulse,
                        animatedWavePhase = waveAnimationPhase,
                        dotFlowPhase = dotFlowPhase,
                        alphaMultiplier = myAlphaMultiplier,
                        showCursorIndicator = true
                    )
                }
            }

            // --- 2. AUTHOR LABELS NEXT TO STROKES ---
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
        }

        // --- 4. PRECISION ZOOM & PAN HUD (Overlay on top of canvas) ---
        if (isInteractive && zoomScale > 1.05f) {
            Surface(
                color = Color(0xEE121124),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x667C4DFF)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .testTag("canvas_zoom_hud")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Zoom Out Button
                    IconButton(
                        onClick = {
                            val newScale = (zoomScale - 0.5f).coerceIn(1.0f, 6.0f)
                            if (newScale <= 1.05f) {
                                zoomScale = 1.0f
                                panOffset = Offset.Zero
                            } else {
                                val maxPanX = (newScale - 1f) * canvasWidth
                                val maxPanY = (newScale - 1f) * canvasHeight
                                zoomScale = newScale
                                panOffset = Offset(panOffset.x.coerceIn(-maxPanX, 0f), panOffset.y.coerceIn(-maxPanY, 0f))
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "Riduci zoom",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Zoom Percentage Badge
                    Surface(
                        color = Color(0xFF7C4DFF),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🔍 ${(zoomScale * 100).roundToInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Zoom In Button
                    IconButton(
                        onClick = {
                            val newScale = (zoomScale + 0.5f).coerceIn(1.0f, 6.0f)
                            val maxPanX = (newScale - 1f) * canvasWidth
                            val maxPanY = (newScale - 1f) * canvasHeight
                            zoomScale = newScale
                            panOffset = Offset(panOffset.x.coerceIn(-maxPanX, 0f), panOffset.y.coerceIn(-maxPanY, 0f))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Aumenta zoom",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Reset 1x Button
                    Surface(
                        color = Color(0x33FFFFFF),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable {
                                zoomScale = 1.0f
                                panOffset = Offset.Zero
                            }
                            .testTag("reset_zoom_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitScreen,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "1x Reset",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
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
            val partnerPxX = (partnerPresence.cursorX * canvasWidth * zoomScale + panOffset.x)
            val partnerPxY = (partnerPresence.cursorY * canvasHeight * zoomScale + panOffset.y)

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
            val rx = (reaction.x * canvasWidth * zoomScale + panOffset.x)
            val ry = (reaction.y * canvasHeight * zoomScale + panOffset.y)

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
 * Represents a chronological chunk of strokes: either a batch of static strokes pre-rendered
 * into an off-screen ImageBitmap, or a single animated stroke rendered dynamically per-frame.
 */
private sealed interface StrokeChunk {
    data class StaticChunk(val strokes: List<DrawingStroke>) : StrokeChunk
    data class AnimatedChunk(val stroke: DrawingStroke) : StrokeChunk
}

/**
 * Returns true if the stroke has an active real-time continuous movement animation
 * (such as sine wave oscillation, pulsating glow, animated dot flow, or sparkling twinkle).
 * Animated strokes are rendered dynamically every frame, while static strokes (like standard
 * spray cans, watercolor, pens, pencils, highlighters) are cached to an off-screen bitmap for 60/120fps fluid performance.
 */
private fun isStrokeAnimated(stroke: DrawingStroke): Boolean {
    val effectiveModifier = when {
        stroke.brushType == BrushType.WATERCOLOR && stroke.modifier == StrokeModifier.WAVE -> StrokeModifier.NONE
        stroke.brushType == BrushType.SPRAY && stroke.modifier == StrokeModifier.SPARKLING -> StrokeModifier.NONE
        stroke.modifier != StrokeModifier.NONE -> stroke.modifier
        stroke.brushType == BrushType.ANIMATED_WAVE -> StrokeModifier.WAVE
        stroke.brushType == BrushType.PULSING_NEON -> StrokeModifier.PULSING
        stroke.brushType == BrushType.PULSING_SPRAY -> StrokeModifier.DOT_FLOW
        stroke.brushType == BrushType.DOT_FLOW -> StrokeModifier.DOT_FLOW
        else -> StrokeModifier.NONE
    }

    return effectiveModifier == StrokeModifier.WAVE ||
           effectiveModifier == StrokeModifier.PULSING ||
           effectiveModifier == StrokeModifier.DOT_FLOW ||
           effectiveModifier == StrokeModifier.SPARKLING
}

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
    alphaMultiplier: Float = 1.0f,
    showCursorIndicator: Boolean = false
) {
    if (stroke.points.isEmpty()) return
    val scaledAlpha = (stroke.alpha * alphaMultiplier).coerceIn(0.01f, 1.0f)
    val effectiveStroke = if (alphaMultiplier < 0.99f) stroke.copy(alpha = scaledAlpha) else stroke
    val baseColor = Color(effectiveStroke.colorArgb).copy(alpha = scaledAlpha)

    if (effectiveStroke.points.size == 1) {
        val pt = effectiveStroke.points[0]
        val isSpray = effectiveStroke.brushType == BrushType.SPRAY
        val singleRadius = if (isSpray) (effectiveStroke.strokeWidth * 1.6f).coerceIn(12f, 75f) else effectiveStroke.strokeWidth / 2f
        drawCircle(
            color = baseColor,
            radius = singleRadius,
            center = Offset(pt.x * canvasW, pt.y * canvasH)
        )
        if (showCursorIndicator) {
            val cx = pt.x * canvasW
            val cy = pt.y * canvasH
            val radius = if (isSpray) (effectiveStroke.strokeWidth * 1.6f).coerceIn(12f, 75f) else (effectiveStroke.strokeWidth / 2f).coerceAtLeast(6f)
            drawCircle(
                color = baseColor.copy(alpha = (0.22f * alphaMultiplier).coerceIn(0.02f, 0.6f)),
                radius = radius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = (0.9f * alphaMultiplier).coerceIn(0.1f, 1f)),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 1.8f)
            )
            drawCircle(
                color = baseColor.copy(alpha = (0.95f * alphaMultiplier).coerceIn(0.1f, 1f)),
                radius = 2.5f,
                center = Offset(cx, cy)
            )
        }
        return
    }

    if (effectiveStroke.brushType == BrushType.ERASER) {
        val lastPt = effectiveStroke.points.lastOrNull()
        if (lastPt != null) {
            val cx = lastPt.x * canvasW
            val cy = lastPt.y * canvasH
            val radius = (effectiveStroke.strokeWidth / 450f).coerceIn(0.015f, 0.22f) * canvasW
            drawCircle(
                color = Color(0x3300E676).copy(alpha = (0.25f * alphaMultiplier).coerceIn(0.02f, 1f)),
                radius = radius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = (0.95f * alphaMultiplier).coerceIn(0.05f, 1f)),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2.2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = (0.8f * alphaMultiplier).coerceIn(0.05f, 1f)),
                radius = 3f,
                center = Offset(cx, cy)
            )
        }
        return
    }

    // Determine effective modifier & brush type
    val effectiveModifier = when {
        // Remove wave from watercolor and sparkling from spray
        stroke.brushType == BrushType.WATERCOLOR && stroke.modifier == StrokeModifier.WAVE -> StrokeModifier.NONE
        stroke.brushType == BrushType.SPRAY && stroke.modifier == StrokeModifier.SPARKLING -> StrokeModifier.NONE
        stroke.modifier != StrokeModifier.NONE -> stroke.modifier
        stroke.brushType == BrushType.ANIMATED_WAVE -> StrokeModifier.WAVE
        stroke.brushType == BrushType.PULSING_NEON -> StrokeModifier.PULSING
        stroke.brushType == BrushType.PULSING_SPRAY -> StrokeModifier.DOT_FLOW
        stroke.brushType == BrushType.DOT_FLOW -> StrokeModifier.DOT_FLOW
        else -> StrokeModifier.NONE
    }

    val effectiveBrush = when (stroke.brushType) {
        BrushType.ANIMATED_WAVE -> BrushType.PEN
        BrushType.PULSING_NEON -> BrushType.NEON
        BrushType.PULSING_SPRAY -> BrushType.SPRAY
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
        StrokeModifier.CALLIGRAPHY -> {
            if (effectiveBrush == BrushType.WATERCOLOR) {
                drawWatercolorStroke(
                    stroke = effectiveStroke,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    baseColor = baseColor,
                    scaledAlpha = scaledAlpha,
                    effectiveRainbowColors = effectiveRainbowColors
                )
            } else {
                drawCalligraphicStroke(
                    stroke = effectiveStroke,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    baseColor = baseColor,
                    scaledAlpha = scaledAlpha,
                    effectiveRainbowColors = effectiveRainbowColors
                )
            }
        }

        StrokeModifier.DOT_FLOW -> {
            if (effectiveBrush == BrushType.SPRAY) {
                // Live Spray Flow: aerosol particles that smoothly stream and flow along the stroke trajectory
                drawSprayStroke(
                    stroke = effectiveStroke,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    baseColor = baseColor,
                    scaledAlpha = scaledAlpha,
                    isPulsing = false,
                    isSparkling = false,
                    isFlow = true,
                    flowPhase = dotFlowPhase,
                    neonPulse = 1.0f
                )
            } else {
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
        }

        StrokeModifier.WAVE -> {
            // Animated flowing sine wave along smoothed stroke length
            val animatedWavyPath = Path()
            val wavyPointsList = ArrayList<DrawingPoint>()
            val smoothedPts = if (effectiveStroke.points.size > 2) smoothPoints(effectiveStroke.points) else effectiveStroke.points
            val firstPt = smoothedPts[0]
            animatedWavyPath.moveTo(firstPt.x * canvasW, firstPt.y * canvasH)
            wavyPointsList.add(DrawingPoint(firstPt.x, firstPt.y, firstPt.pressure, firstPt.timestamp))

            var accumulatedDist = 0f
            val frequency = if (effectiveBrush == BrushType.WATERCOLOR) 0.028f else 0.055f
            val amplitude = if (effectiveBrush == BrushType.WATERCOLOR) {
                (effectiveStroke.strokeWidth * 0.35f).coerceIn(3.0f, 9.0f)
            } else {
                (effectiveStroke.strokeWidth * 0.70f).coerceIn(5f, 20f)
            }

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

                    val interpPressure = p0.pressure + (p1.pressure - p0.pressure) * t
                    val interpTimestamp = (p0.timestamp + ((p1.timestamp - p0.timestamp) * t).toLong())
                    wavyPointsList.add(
                        DrawingPoint(
                            x = if (canvasW > 0f) ix / canvasW else 0f,
                            y = if (canvasH > 0f) iy / canvasH else 0f,
                            pressure = interpPressure,
                            timestamp = interpTimestamp
                        )
                    )
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
                BrushType.WATERCOLOR -> {
                    // Organic Watercolor rendering along wavy undulating trajectory
                    val wavyStroke = effectiveStroke.copy(
                        points = if (wavyPointsList.size >= 2) wavyPointsList else effectiveStroke.points
                    )
                    drawWatercolorStroke(
                        stroke = wavyStroke,
                        canvasW = canvasW,
                        canvasH = canvasH,
                        baseColor = baseColor,
                        scaledAlpha = scaledAlpha,
                        effectiveRainbowColors = effectiveRainbowColors
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
                BrushType.SPRAY -> {
                    drawSprayStroke(
                        stroke = effectiveStroke,
                        canvasW = canvasW,
                        canvasH = canvasH,
                        baseColor = baseColor,
                        scaledAlpha = scaledAlpha,
                        isPulsing = false,
                        neonPulse = 1.0f
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
                BrushType.WATERCOLOR -> {
                    val pulseScale = (0.75f + 0.25f * neonPulse).coerceIn(0.5f, 1.25f)
                    drawWatercolorStroke(
                        stroke = effectiveStroke.copy(strokeWidth = stroke.strokeWidth * pulseScale),
                        canvasW = canvasW,
                        canvasH = canvasH,
                        baseColor = baseColor,
                        scaledAlpha = (scaledAlpha * pulseScale).coerceIn(0.01f, 1.0f),
                        effectiveRainbowColors = effectiveRainbowColors
                    )
                }
                BrushType.SPRAY -> {
                    drawSprayStroke(
                        stroke = effectiveStroke,
                        canvasW = canvasW,
                        canvasH = canvasH,
                        baseColor = baseColor,
                        scaledAlpha = scaledAlpha,
                        isPulsing = true,
                        isSparkling = false,
                        neonPulse = neonPulse
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

        StrokeModifier.SPARKLING -> {
            // Live sparkling glint & glitter scatter animation
            when (effectiveBrush) {
                BrushType.SPRAY -> {
                    drawSprayStroke(
                        stroke = effectiveStroke,
                        canvasW = canvasW,
                        canvasH = canvasH,
                        baseColor = baseColor,
                        scaledAlpha = scaledAlpha,
                        isPulsing = false,
                        isSparkling = true,
                        neonPulse = neonPulse
                    )
                }
                else -> {
                    // Default sparkling stroke
                    drawPath(
                        path = path,
                        color = baseColor,
                        style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = (scaledAlpha * (0.7f + 0.3f * neonPulse)).coerceIn(0.01f, 1.0f)),
                        style = Stroke(width = (stroke.strokeWidth * 0.35f).coerceAtLeast(1.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }

        StrokeModifier.NONE -> {
            when (effectiveBrush) {
                BrushType.SPRAY -> {
                    drawSprayStroke(
                        stroke = effectiveStroke,
                        canvasW = canvasW,
                        canvasH = canvasH,
                        baseColor = baseColor,
                        scaledAlpha = scaledAlpha,
                        isPulsing = false,
                        neonPulse = 1.0f
                    )
                }
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
                BrushType.WATERCOLOR -> {
                    drawWatercolorStroke(
                        stroke = effectiveStroke,
                        canvasW = canvasW,
                        canvasH = canvasH,
                        baseColor = baseColor,
                        scaledAlpha = scaledAlpha,
                        effectiveRainbowColors = effectiveRainbowColors
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

    // Dynamic radial indicator at the active touch point for all brush types
    if (showCursorIndicator && effectiveStroke.brushType != BrushType.ERASER) {
        val lastPt = effectiveStroke.points.lastOrNull()
        if (lastPt != null) {
            val cx = lastPt.x * canvasW
            val cy = lastPt.y * canvasH
            val isSpray = effectiveStroke.brushType == BrushType.SPRAY
            val radius = if (isSpray) (effectiveStroke.strokeWidth * 1.6f).coerceIn(12f, 75f) else (effectiveStroke.strokeWidth / 2f).coerceAtLeast(6f)
            drawCircle(
                color = baseColor.copy(alpha = (0.22f * alphaMultiplier).coerceIn(0.02f, 0.6f)),
                radius = radius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = (0.9f * alphaMultiplier).coerceIn(0.1f, 1f)),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 1.8f)
            )
            drawCircle(
                color = baseColor.copy(alpha = (0.95f * alphaMultiplier).coerceIn(0.1f, 1f)),
                radius = 2.5f,
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * Natural, fluid Calligraphic Pen Stroke renderer.
 * Calculates drawing velocity between consecutive touch points:
 * - Slower, deliberate strokes produce richer, thicker lines.
 * - Faster, energetic strokes produce thinner, tapered flourishes.
 * Smooths width transitions and renders continuous anti-aliased round-capped Bezier segments.
 */
private fun DrawScope.drawCalligraphicStroke(
    stroke: DrawingStroke,
    canvasW: Float,
    canvasH: Float,
    baseColor: Color,
    scaledAlpha: Float,
    effectiveRainbowColors: List<Color> = emptyList()
) {
    val rawPts = stroke.points
    if (rawPts.isEmpty()) return
    if (rawPts.size == 1) {
        val pt = rawPts[0]
        drawCircle(
            color = baseColor,
            radius = stroke.strokeWidth / 2f,
            center = Offset(pt.x * canvasW, pt.y * canvasH)
        )
        return
    }

    val smoothedPts = if (rawPts.size > 2) smoothPoints(rawPts) else rawPts
    val n = smoothedPts.size
    if (n < 2) return

    val screenPts = smoothedPts.map { Offset(it.x * canvasW, it.y * canvasH) }
    val baseWidth = stroke.strokeWidth

    // 1. Calculate instantaneous velocity at each point (pixels per millisecond)
    val rawWidths = FloatArray(n)
    for (i in 0 until n) {
        val currPt = smoothedPts[i]
        val v = if (i == 0) {
            val nextPt = smoothedPts[1]
            val dist = kotlin.math.hypot(screenPts[1].x - screenPts[0].x, screenPts[1].y - screenPts[0].y)
            val dt = (nextPt.timestamp - currPt.timestamp).coerceIn(1L, 350L).toFloat()
            dist / dt
        } else {
            val prevPt = smoothedPts[i - 1]
            val dist = kotlin.math.hypot(screenPts[i].x - screenPts[i - 1].x, screenPts[i].y - screenPts[i - 1].y)
            val dt = (currPt.timestamp - prevPt.timestamp).coerceIn(1L, 350L).toFloat()
            dist / dt
        }

        // Calligraphic velocity-to-width mapping:
        // Slower stroke (v ~ 0 px/ms) -> factor up to 1.35x
        // Moderate stroke (v ~ 0.65 px/ms) -> factor ~ 0.85x
        // Fast stroke (v > 2.0 px/ms) -> factor down to 0.28x
        val speedFactor = (1.35f - 1.05f * (v / (v + 0.65f))).coerceIn(0.25f, 1.40f)
        val pressureFactor = (0.75f + 0.25f * currPt.pressure).coerceIn(0.6f, 1.3f)
        rawWidths[i] = (baseWidth * speedFactor * pressureFactor).coerceAtLeast(1.5f)
    }

    // 2. Smooth width transitions across adjacent points to eliminate sudden jumps
    val widths = FloatArray(n)
    widths[0] = rawWidths[0]
    for (i in 1 until n) {
        widths[i] = widths[i - 1] * 0.40f + rawWidths[i] * 0.60f
    }
    // Backward smoothing pass
    for (i in n - 2 downTo 0) {
        widths[i] = widths[i] * 0.70f + widths[i + 1] * 0.30f
    }
    // Natural tip tapering at the very start and finish
    if (n >= 3) {
        widths[0] = (widths[0] * 0.65f).coerceAtLeast(1.5f)
        widths[n - 1] = (widths[n - 1] * 0.40f).coerceAtLeast(1.2f)
    }

    // 3. Render continuous variable-width quadratic Bezier curves
    val isRainbow = stroke.brushType == BrushType.RAINBOW && effectiveRainbowColors.size >= 2
    val rainbowBrush = if (isRainbow) {
        Brush.linearGradient(
            colors = effectiveRainbowColors,
            start = Offset(0f, 0f),
            end = Offset(canvasW, canvasH)
        )
    } else null

    if (n == 2) {
        val p0 = screenPts[0]
        val p1 = screenPts[1]
        val steps = 8
        for (s in 0 until steps) {
            val t0 = s.toFloat() / steps
            val t1 = (s + 1).toFloat() / steps
            val sp0 = Offset(p0.x + (p1.x - p0.x) * t0, p0.y + (p1.y - p0.y) * t0)
            val sp1 = Offset(p0.x + (p1.x - p0.x) * t1, p0.y + (p1.y - p0.y) * t1)
            val w = widths[0] + (widths[1] - widths[0]) * ((t0 + t1) * 0.5f)
            if (rainbowBrush != null) {
                drawLine(
                    brush = rainbowBrush,
                    start = sp0,
                    end = sp1,
                    strokeWidth = w,
                    cap = StrokeCap.Round
                )
            } else {
                drawLine(
                    color = baseColor,
                    start = sp0,
                    end = sp1,
                    strokeWidth = w,
                    cap = StrokeCap.Round
                )
            }
        }
        return
    }

    // Connect quadratic Bezier segments between midpoints
    for (i in 0 until n - 1) {
        val p0 = screenPts[i]
        val p1 = screenPts[i + 1]
        val w0 = widths[i]
        val w1 = widths[i + 1]

        val startPt = if (i == 0) p0 else Offset((screenPts[i - 1].x + p0.x) * 0.5f, (screenPts[i - 1].y + p0.y) * 0.5f)
        val endPt = if (i == n - 2) p1 else Offset((p0.x + p1.x) * 0.5f, (p0.y + p1.y) * 0.5f)
        val ctrlPt = p0

        val segSteps = 8
        var prevSubPt = startPt
        for (s in 1..segSteps) {
            val t = s.toFloat() / segSteps
            val invT = 1f - t
            // Quadratic Bezier: B(t) = (1-t)^2 * start + 2*(1-t)*t * ctrl + t^2 * end
            val subX = invT * invT * startPt.x + 2f * invT * t * ctrlPt.x + t * t * endPt.x
            val subY = invT * invT * startPt.y + 2f * invT * t * ctrlPt.y + t * t * endPt.y
            val currSubPt = Offset(subX, subY)
            val curW = w0 + (w1 - w0) * t

            if (rainbowBrush != null) {
                drawLine(
                    brush = rainbowBrush,
                    start = prevSubPt,
                    end = currSubPt,
                    strokeWidth = curW,
                    cap = StrokeCap.Round
                )
            } else {
                drawLine(
                    color = baseColor,
                    start = prevSubPt,
                    end = currSubPt,
                    strokeWidth = curW,
                    cap = StrokeCap.Round
                )
            }
            prevSubPt = currSubPt
        }
    }
}

/**
 * Authentic Flat Watercolor Brush Stroke renderer matching real-world flat bristle watercolor swatches.
 * Inspired by professional watercolor brush flat-wash marks:
 * - Broad, luminous flat ribbon stroke with translucent layering (velature acquerello).
 * - Distinct feathery dry-brush bristle striations (setole e sfilacciature) extending outward at the start and tips.
 * - Granular watercolor pigment bleed & mottled wash pooling (pigmento concentrato che si accumula verso il fondo/fine del tratto).
 * - Subtle darker wet-edge pigment outlines (effetto bordo bagnato / coffee ring naturale).
 */
private fun DrawScope.drawWatercolorStroke(
    stroke: DrawingStroke,
    canvasW: Float,
    canvasH: Float,
    baseColor: Color,
    scaledAlpha: Float,
    effectiveRainbowColors: List<Color> = emptyList()
) {
    val rawPts = stroke.points
    if (rawPts.isEmpty()) return

    val isRainbow = stroke.brushType == BrushType.RAINBOW && effectiveRainbowColors.size >= 2
    val baseWidth = (stroke.strokeWidth * 1.35f).coerceAtLeast(14f)
    val halfWidth = baseWidth * 0.5f
    val strokeSeed = (stroke.id.hashCode().toLong() xor stroke.colorArgb.toLong())

    // 1. Single tap / dab: A flat brush impression with bristle tips
    if (rawPts.size == 1) {
        val center = Offset(rawPts[0].x * canvasW, rawPts[0].y * canvasH)
        val dabHeight = baseWidth * 1.35f

        // Soft background wash
        drawOval(
            color = baseColor.copy(alpha = (scaledAlpha * 0.28f).coerceIn(0.01f, 0.6f)),
            topLeft = Offset(center.x - halfWidth, center.y - dabHeight * 0.5f),
            size = androidx.compose.ui.geometry.Size(baseWidth, dabHeight)
        )
        // Bristle striation marks
        val numBristles = 9
        for (b in 0 until numBristles) {
            val frac = (b.toFloat() / (numBristles - 1)) - 0.5f
            val bristleX = center.x + frac * baseWidth * 0.85f
            val topH = (dabHeight * 0.55f) + fastSprayRandomFloat(strokeSeed, b) * (dabHeight * 0.35f)
            val bristleAlpha = (scaledAlpha * (0.25f + 0.35f * fastSprayRandomFloat(strokeSeed, b + 10))).coerceIn(0.02f, 0.85f)
            drawLine(
                color = baseColor.copy(alpha = bristleAlpha),
                start = Offset(bristleX, center.y + dabHeight * 0.4f),
                end = Offset(bristleX, center.y - topH),
                strokeWidth = (baseWidth / numBristles * 0.9f).coerceIn(1.8f, 5.5f),
                cap = StrokeCap.Round
            )
        }
        // Pigment sediment pooling at bottom
        drawCircle(
            color = baseColor.copy(alpha = (scaledAlpha * 0.45f).coerceIn(0.05f, 0.9f)),
            radius = halfWidth * 0.5f,
            center = Offset(center.x, center.y + dabHeight * 0.25f)
        )
        return
    }

    // 2. Continuous stroke: Full Flat Watercolor Brush with bristle striations, dry-brush tips & wet edges
    val smoothedPts = if (rawPts.size > 2) smoothPoints(rawPts) else rawPts
    val n = smoothedPts.size
    if (n < 2) return

    val screenPts = smoothedPts.map { Offset(it.x * canvasW, it.y * canvasH) }

    // Segment lengths and stroke trajectory
    val segLengths = FloatArray(n - 1)
    var totalLength = 0f
    for (i in 0 until n - 1) {
        val len = kotlin.math.hypot(screenPts[i + 1].x - screenPts[i].x, screenPts[i + 1].y - screenPts[i].y)
        segLengths[i] = len
        totalLength += len
    }

    if (totalLength <= 1f) {
        drawCircle(
            color = baseColor.copy(alpha = (scaledAlpha * 0.4f).coerceIn(0.01f, 1.0f)),
            radius = halfWidth,
            center = screenPts[0]
        )
        return
    }

    // Step spacing along spine
    val stepSpacing = (baseWidth * 0.16f).coerceIn(2.5f, 12f)
    var currentDist = 0f
    var segIdx = 0
    var distInSeg = 0f
    var sampleIdx = 0

    val leftBoundary = ArrayList<Offset>()
    val rightBoundary = ArrayList<Offset>()
    val spinePoints = ArrayList<Offset>()
    val normalList = ArrayList<Offset>()
    val dirList = ArrayList<Offset>()
    val lengthFracs = ArrayList<Float>()
    val widthsList = ArrayList<Float>()

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

        val progress = (currentDist / totalLength).coerceIn(0f, 1f)

        // Flat brush width profile: wide body, natural organic taper/flare
        val pt0 = smoothedPts[segIdx]
        val pt1 = smoothedPts[segIdx + 1]
        val pressure = (pt0.pressure + (pt1.pressure - pt0.pressure) * segFrac).coerceIn(0.65f, 1.35f)
        val sampleSeed = (strokeSeed xor (sampleIdx * 337L))
        val organicNoise = 0.92f + 0.16f * fastSprayRandomFloat(sampleSeed, 1)

        val curHalfW = halfWidth * pressure * organicNoise
        val leftPt = Offset(posX + normX * curHalfW, posY + normY * curHalfW)
        val rightPt = Offset(posX - normX * curHalfW, posY - normY * curHalfW)

        leftBoundary.add(leftPt)
        rightBoundary.add(rightPt)
        spinePoints.add(Offset(posX, posY))
        normalList.add(Offset(normX, normY))
        dirList.add(Offset(dirX, dirY))
        lengthFracs.add(progress)
        widthsList.add(curHalfW)

        currentDist += stepSpacing
        distInSeg += stepSpacing
        sampleIdx++
    }

    if (leftBoundary.isEmpty() || rightBoundary.isEmpty()) return

    // -------------------------------------------------------------------------------------
    // LAYER 1: Soft Translucent Base Watercolor Wash (Velatura di base fluida e luminosa)
    // -------------------------------------------------------------------------------------
    val bodyPolygon = Path()
    bodyPolygon.moveTo(leftBoundary[0].x, leftBoundary[0].y)
    for (i in 1 until leftBoundary.size) {
        val p = leftBoundary[i - 1]
        val c = leftBoundary[i]
        bodyPolygon.quadraticTo(p.x, p.y, (p.x + c.x) * 0.5f, (p.y + c.y) * 0.5f)
    }
    bodyPolygon.lineTo(leftBoundary.last().x, leftBoundary.last().y)
    bodyPolygon.lineTo(rightBoundary.last().x, rightBoundary.last().y)
    for (i in rightBoundary.size - 2 downTo 0) {
        val p = rightBoundary[i + 1]
        val c = rightBoundary[i]
        bodyPolygon.quadraticTo(p.x, p.y, (p.x + c.x) * 0.5f, (p.y + c.y) * 0.5f)
    }
    bodyPolygon.lineTo(rightBoundary[0].x, rightBoundary[0].y)
    bodyPolygon.close()

    // 1st Layer: Luminous wash
    drawPath(
        path = bodyPolygon,
        color = baseColor.copy(alpha = (scaledAlpha * 0.22f).coerceIn(0.01f, 0.45f))
    )
    // 2nd Layer: Slightly narrower, richer core for depth
    drawPath(
        path = bodyPolygon,
        color = baseColor.copy(alpha = (scaledAlpha * 0.14f).coerceIn(0.01f, 0.35f))
    )

    // -------------------------------------------------------------------------------------
    // LAYER 2: Flat Brush Bristle Striations & Bristle Spikes (Setole sfilacciate e striature)
    // As seen in the reference watercolor swatches, the start or top has distinct bristle fibers
    // -------------------------------------------------------------------------------------
    val numRibbons = 12
    val numSamples = spinePoints.size
    for (r in 0 until numRibbons) {
        val ribbonFrac = (r.toFloat() / (numRibbons - 1)) * 2f - 1f // -1f (left) to +1f (right)
        val ribbonSeed = strokeSeed xor (r * 1013L)
        val bristleWidth = (baseWidth / numRibbons * (0.85f + 0.4f * fastSprayRandomFloat(ribbonSeed, 1))).coerceIn(1.6f, 6.0f)
        val bristleAlpha = (scaledAlpha * (0.16f + 0.26f * fastSprayRandomFloat(ribbonSeed, 2))).coerceIn(0.02f, 0.70f)

        val bristlePath = Path()
        var hasMoved = false

        // Individual bristle length variance: some bristles extend further, some stop earlier (dry brush effect)
        val bristleStartFrac = (fastSprayRandomFloat(ribbonSeed, 3) * 0.12f)
        val bristleEndFrac = 1.0f - (fastSprayRandomFloat(ribbonSeed, 4) * 0.08f)

        for (i in 0 until numSamples) {
            val progress = lengthFracs[i]
            if (progress < bristleStartFrac || progress > bristleEndFrac) continue

            val center = spinePoints[i]
            val norm = normalList[i]
            val curW = widthsList[i]

            // Micro wave jitter for individual hair wiggle
            val hairJitter = (fastSprayRandomFloat(ribbonSeed xor (i * 37L), 5) - 0.5f) * (baseWidth * 0.08f)
            val bx = center.x + norm.x * (ribbonFrac * curW * 0.88f + hairJitter)
            val by = center.y + norm.y * (ribbonFrac * curW * 0.88f + hairJitter)

            if (!hasMoved) {
                bristlePath.moveTo(bx, by)
                hasMoved = true
            } else {
                bristlePath.lineTo(bx, by)
            }
        }

        if (hasMoved) {
            drawPath(
                path = bristlePath,
                color = baseColor.copy(alpha = bristleAlpha),
                style = Stroke(width = bristleWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    // -------------------------------------------------------------------------------------
    // LAYER 3: Feathery Bristle Tips at Stroke Start (Punte delle setole sfilacciate alla partenza)
    // Exactly matching the iconic crown of vertical bristle spikes at the top of the reference swatches
    // -------------------------------------------------------------------------------------
    if (numSamples >= 2) {
        val startCenter = spinePoints[0]
        val startNorm = normalList[0]
        val startDir = dirList[0] // direction pointing forward
        val startWidth = widthsList[0]

        // Bristle tips point backwards against the stroke movement (or forward depending on stroke origin)
        val tipCount = 14
        for (t in 0 until tipCount) {
            val tFrac = (t.toFloat() / (tipCount - 1)) * 2f - 1f // across width
            val tipSeed = strokeSeed xor (t * 2039L)

            // Feathery bristle tip length varies organically
            val spikeLength = baseWidth * (0.35f + 0.85f * fastSprayRandomFloat(tipSeed, 11))
            val lateralPos = tFrac * startWidth * 0.90f
            val basePtX = startCenter.x + startNorm.x * lateralPos
            val basePtY = startCenter.y + startNorm.y * lateralPos

            // Extend outward opposite to stroke direction
            val tipPtX = basePtX - startDir.x * spikeLength + (fastSprayRandomFloat(tipSeed, 12) - 0.5f) * (baseWidth * 0.15f)
            val tipPtY = basePtY - startDir.y * spikeLength + (fastSprayRandomFloat(tipSeed, 13) - 0.5f) * (baseWidth * 0.15f)

            val tipAlpha = (scaledAlpha * (0.28f + 0.40f * fastSprayRandomFloat(tipSeed, 14))).coerceIn(0.04f, 0.85f)
            val tipW = (baseWidth / tipCount * (0.75f + 0.5f * fastSprayRandomFloat(tipSeed, 15))).coerceIn(1.5f, 4.8f)

            drawLine(
                color = baseColor.copy(alpha = tipAlpha),
                start = Offset(basePtX, basePtY),
                end = Offset(tipPtX, tipPtY),
                strokeWidth = tipW,
                cap = StrokeCap.Round
            )
        }
    }

    // -------------------------------------------------------------------------------------
    // LAYER 4: Organic Pigment Pooling & Granulation (Concentrazione d'acqua e pigmento sul finale)
    // Water and pigment pool heavily towards the tail and edges creating natural watercolor blooms
    // -------------------------------------------------------------------------------------
    val poolStartIdx = (numSamples * 0.55f).toInt().coerceIn(0, numSamples - 1)
    for (i in poolStartIdx until numSamples step 2) {
        val progress = lengthFracs[i]
        val center = spinePoints[i]
        val rad = widthsList[i]
        val pSeed = strokeSeed xor (i * 883L)

        // Concentration increases towards the end of the stroke (puddle drying effect)
        val poolConcentration = ((progress - 0.55f) / 0.45f).coerceIn(0f, 1f)
        val poolAlpha = (scaledAlpha * (0.15f + 0.35f * poolConcentration * fastSprayRandomFloat(pSeed, 21))).coerceIn(0.02f, 0.70f)
        val poolRadius = rad * (0.40f + 0.50f * fastSprayRandomFloat(pSeed, 22))

        val offsetX = (fastSprayRandomFloat(pSeed, 23) - 0.5f) * rad * 0.65f
        val offsetY = (fastSprayRandomFloat(pSeed, 24) - 0.5f) * rad * 0.65f

        drawCircle(
            color = baseColor.copy(alpha = poolAlpha),
            radius = poolRadius,
            center = Offset(center.x + offsetX, center.y + offsetY)
        )
    }

    // -------------------------------------------------------------------------------------
    // LAYER 5: Delicate Wet-Edge Rim (Bordo d'acqua scuro e increspato / Coffee-ring effect)
    // Fine, organic outline where pigment collected along the outer wet meniscus
    // -------------------------------------------------------------------------------------
    val leftRimPath = Path()
    leftRimPath.moveTo(leftBoundary[0].x, leftBoundary[0].y)
    for (i in 1 until leftBoundary.size) {
        val p = leftBoundary[i - 1]
        val c = leftBoundary[i]
        leftRimPath.quadraticTo(p.x, p.y, (p.x + c.x) * 0.5f, (p.y + c.y) * 0.5f)
    }
    leftRimPath.lineTo(leftBoundary.last().x, leftBoundary.last().y)

    val rightRimPath = Path()
    rightRimPath.moveTo(rightBoundary[0].x, rightBoundary[0].y)
    for (i in 1 until rightBoundary.size) {
        val p = rightBoundary[i - 1]
        val c = rightBoundary[i]
        rightRimPath.quadraticTo(p.x, p.y, (p.x + c.x) * 0.5f, (p.y + c.y) * 0.5f)
    }
    rightRimPath.lineTo(rightBoundary.last().x, rightBoundary.last().y)

    val rimAlpha = (scaledAlpha * 0.65f).coerceIn(0.12f, 0.90f)
    val rimWidth = (baseWidth * 0.08f).coerceIn(1.2f, 3.2f)

    drawPath(
        path = leftRimPath,
        color = baseColor.copy(alpha = rimAlpha),
        style = Stroke(width = rimWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = rightRimPath,
        color = baseColor.copy(alpha = rimAlpha),
        style = Stroke(width = rimWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Soft finish at the final puddle rim
    if (rightBoundary.isNotEmpty() && leftBoundary.isNotEmpty()) {
        val endRimPath = Path()
        endRimPath.moveTo(leftBoundary.last().x, leftBoundary.last().y)
        endRimPath.lineTo(rightBoundary.last().x, rightBoundary.last().y)
        drawPath(
            path = endRimPath,
            color = baseColor.copy(alpha = rimAlpha),
            style = Stroke(width = rimWidth * 1.2f, cap = StrokeCap.Round)
        )
    }
}

/**
 * High-performance, allocation-free pseudo-random float generator for real-time rendering.
 */
private inline fun fastSprayRandomFloat(seed: Long, index: Int): Float {
    var x = seed xor (index.toLong() * 0x9E3779B97F4A7C15UL.toLong())
    x = (x xor (x ushr 30)) * 0xBF58476D1CE4E5B9UL.toLong()
    x = (x xor (x ushr 27)) * 0x94D049BB133111EBUL.toLong()
    x = x xor (x ushr 31)
    return ((x and 0x7FFFFFFF).toFloat()) / 0x7FFFFFFF.toFloat()
}

/**
 * Authentic ArtWorkout-style Aerosol Spray Can / Airbrush renderer.
 * Features:
 * - Multi-layer aerosol physics: ultra-fine Gaussian particulate vapor mist + high-density aerosol core + organic edge splatters.
 * - Dynamic velocity modulation: slow movements allow dense pigment concentration; fast gestures deposit airy, feathered micro-droplets.
 * - Organic spatter droplets with micro-highlight accents for true paint spray realism.
 */
private fun DrawScope.drawSprayStroke(
    stroke: DrawingStroke,
    canvasW: Float,
    canvasH: Float,
    baseColor: Color,
    scaledAlpha: Float,
    isPulsing: Boolean = false,
    isSparkling: Boolean = false,
    isFlow: Boolean = false,
    flowPhase: Float = 0f,
    neonPulse: Float = 1.0f
) {
    if (stroke.points.isEmpty()) return
    val pts = stroke.points
    val seed = stroke.id.hashCode().toLong()

    // Smooth trajectory for consistent aerodynamic aerosol dispersion
    val pathPts = if (pts.size > 2) smoothPoints(pts) else pts
    val screenPts = pathPts.map { Offset(it.x * canvasW, it.y * canvasH) }
    val n = screenPts.size

    val baseSprayRadius = (stroke.strokeWidth * 1.6f).coerceIn(12f, 75f)
    val effectiveSprayRadius = if (isPulsing) baseSprayRadius * (0.85f + 0.35f * neonPulse) else baseSprayRadius

    // Single touch point tap (burst spray)
    if (n <= 1) {
        val cx = pts[0].x * canvasW
        val cy = pts[0].y * canvasH

        // 1. Soft aerosol background vapor
        val mistSteps = 7
        for (m in 1..mistSteps) {
            val frac = m.toFloat() / mistSteps
            val mistR = effectiveSprayRadius * frac
            val mistA = (scaledAlpha * 0.13f * (1f - frac * 0.75f)).coerceIn(0.005f, 1f)
            drawCircle(
                color = baseColor.copy(alpha = mistA),
                radius = mistR,
                center = Offset(cx, cy)
            )
        }

        // 2. High-density aerosol micro-droplet burst
        val burstDots = (stroke.strokeWidth * 3.2f).toInt().coerceIn(40, 160)
        for (d in 0 until burstDots) {
            val r1 = fastSprayRandomFloat(seed, d * 4)
            val r2 = fastSprayRandomFloat(seed, d * 4 + 1)
            val r3 = fastSprayRandomFloat(seed, d * 4 + 2)
            val r4 = fastSprayRandomFloat(seed, d * 4 + 3)

            val angle = r1 * 6.2831855f
            // Box-Muller approx for natural Gaussian dropoff
            val gaussianR = (r2 * r3 + r4 * 0.5f) * 0.67f
            val dist = (gaussianR * effectiveSprayRadius).coerceAtMost(effectiveSprayRadius * 1.25f)

            val dx = cx + kotlin.math.cos(angle) * dist
            val dy = cy + kotlin.math.sin(angle) * dist

            val normalizedDist = (dist / effectiveSprayRadius).coerceIn(0f, 1.3f)
            val isCore = normalizedDist < 0.35f
            val isSplatter = r3 > 0.88f && normalizedDist > 0.65f

            val dotRadius = when {
                isSplatter -> (stroke.strokeWidth * 0.08f).coerceIn(1.3f, 3.2f) * (0.8f + r2 * 0.6f)
                isCore -> (stroke.strokeWidth * 0.06f).coerceIn(0.9f, 2.2f) * (0.9f + r1 * 0.4f)
                else -> (stroke.strokeWidth * 0.05f).coerceIn(0.7f, 1.8f) * (0.7f + r4 * 0.5f)
            }

            val dotAlpha = (scaledAlpha * (1f - normalizedDist * 0.65f) * (0.45f + r1 * 0.55f)).coerceIn(0.02f, 1f)
            drawCircle(
                color = baseColor.copy(alpha = dotAlpha),
                radius = dotRadius,
                center = Offset(dx, dy)
            )

            if (isCore && r4 > 0.85f) {
                drawCircle(
                    color = Color.White.copy(alpha = (dotAlpha * 0.5f).coerceIn(0.01f, 1f)),
                    radius = dotRadius * 0.5f,
                    center = Offset(dx, dy)
                )
            }
        }
        return
    }

    // Cumulative distances along trajectory
    val cumDist = FloatArray(n)
    cumDist[0] = 0f
    for (i in 1 until n) {
        val dx = screenPts[i].x - screenPts[i - 1].x
        val dy = screenPts[i].y - screenPts[i - 1].y
        cumDist[i] = cumDist[i - 1] + kotlin.math.hypot(dx, dy)
    }
    val totalLength = cumDist[n - 1]
    if (totalLength <= 1f) return

    // Dynamic velocity calculation for ArtWorkout feel:
    // Slower movements = tighter, saturated paint coverage.
    // Faster movements = wider, lighter atomized mist spray.
    val avgSpeed = (totalLength / n).coerceIn(2f, 40f)
    val speedFactor = ((avgSpeed - 2f) / 38f).coerceIn(0f, 1f)
    val dynamicRadius = effectiveSprayRadius * (0.9f + 0.35f * speedFactor)
    val coreSaturation = (1.0f - speedFactor * 0.35f).coerceIn(0.65f, 1.0f)

    if (isFlow) {
        // Continuous fluid spray flow along the path in stroke order (from start to end)
        val baseStepStride = (dynamicRadius * 0.45f).coerceAtLeast(6f)
        val baseClusterCount = (totalLength / baseStepStride).toInt().coerceIn(4, 130)

        // 1. Soft underlying atomized mist
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

            val baseDots = (stroke.strokeWidth * 0.85f).toInt().coerceIn(8, 22)
            for (bd in 0 until baseDots) {
                val r1 = fastSprayRandomFloat(seed, b * 31 + bd * 3)
                val r2 = fastSprayRandomFloat(seed, b * 31 + bd * 3 + 1)
                val r3 = fastSprayRandomFloat(seed, b * 31 + bd * 3 + 2)
                val angle = r1 * 6.2831855f
                val rFactor = (r2 * r3 + (r2 + r3) * 0.5f) * 0.5f
                val dotDist = rFactor * dynamicRadius * 0.90f
                val dx = bx + kotlin.math.cos(angle) * dotDist
                val dy = by + kotlin.math.sin(angle) * dotDist
                val bAlpha = (scaledAlpha * 0.32f * coreSaturation * (1f - rFactor * 0.5f)).coerceIn(0.01f, 1.0f)
                drawCircle(
                    color = baseColor.copy(alpha = bAlpha),
                    radius = (stroke.strokeWidth * 0.08f).coerceIn(0.9f, 2.4f),
                    center = Offset(dx, dy)
                )
            }
        }

        // 2. Flowing aerosol particles streaming forward along the stroke order
        val clusterSpacing = (dynamicRadius * 0.28f).coerceIn(3.5f, 14f)
        val numClusters = (totalLength / clusterSpacing).toInt().coerceAtLeast(8)
        val dotsPerCluster = (stroke.strokeWidth * 1.35f).toInt().coerceIn(16, 36)
        val edgeMargin = (totalLength * 0.05f).coerceIn(6f, 40f)

        for (c in 0 until numClusters) {
            val baseFraction = c.toFloat() / numClusters
            val flowPos = ((baseFraction + flowPhase) % 1.0f) * totalLength
            val edgeFade = minOf(
                1.0f,
                flowPos / edgeMargin,
                (totalLength - flowPos) / edgeMargin
            ).coerceIn(0f, 1f)

            var sIdx = 0
            while (sIdx < n - 2 && cumDist[sIdx + 1] < flowPos) sIdx++
            val sStart = cumDist[sIdx]
            val sEnd = cumDist[sIdx + 1]
            val segLen = (sEnd - sStart).coerceAtLeast(0.001f)
            val u = ((flowPos - sStart) / segLen).coerceIn(0f, 1f)

            val p0 = screenPts[sIdx]
            val p1 = screenPts[sIdx + 1]
            val cx = p0.x + (p1.x - p0.x) * u
            val cy = p0.y + (p1.y - p0.y) * u

            val dirX = (p1.x - p0.x) / segLen
            val dirY = (p1.y - p0.y) / segLen
            val normX = -dirY
            val normY = dirX

            for (d in 0 until dotsPerCluster) {
                val r1 = fastSprayRandomFloat(seed, c * 67 + d * 3)
                val r2 = fastSprayRandomFloat(seed, c * 67 + d * 3 + 1)
                val r3 = fastSprayRandomFloat(seed, c * 67 + d * 3 + 2)

                val rFactor = (r1 * r2 * 2f - 1.0f)
                val perpDist = rFactor * dynamicRadius
                val tangOffset = (r3 - 0.5f) * clusterSpacing * 0.85f

                val dotX = cx + normX * perpDist + dirX * tangOffset
                val dotY = cy + normY * perpDist + dirY * tangOffset

                val absR = kotlin.math.abs(rFactor)
                val isCore = absR < 0.32f
                val isSplatter = r3 > 0.86f && absR > 0.65f

                val dotRadius = when {
                    isSplatter -> (stroke.strokeWidth * 0.10f).coerceIn(1.3f, 3.0f)
                    isCore -> (stroke.strokeWidth * 0.08f).coerceIn(1.0f, 2.3f)
                    else -> (stroke.strokeWidth * 0.06f).coerceIn(0.7f, 1.8f)
                }

                val flowAlpha = (scaledAlpha * coreSaturation * (1f - absR * 0.45f) * edgeFade).coerceIn(0.01f, 1.0f)
                drawCircle(
                    color = baseColor.copy(alpha = flowAlpha),
                    radius = dotRadius,
                    center = Offset(dotX, dotY)
                )

                if (isCore && r2 > 0.85f && edgeFade > 0.4f) {
                    drawCircle(
                        color = Color.White.copy(alpha = (flowAlpha * 0.45f).coerceIn(0.01f, 1.0f)),
                        radius = dotRadius * 0.5f,
                        center = Offset(dotX, dotY)
                    )
                }
            }
        }
        return
    }

    // --- ARTWORKOUT AUTHENTIC SPRAY CAN RENDERER ---

    // 1. LAYER 1: Multi-Pass Soft Gaussian Aerosol Vapor Mist
    // Simulates the airborne atomized cloud around the spray nozzle
    val mistStride = (dynamicRadius * 0.40f).coerceAtLeast(5f)
    val mistClusterCount = (totalLength / mistStride).toInt().coerceIn(3, 140)
    for (m in 0..mistClusterCount) {
        val distAlong = (m.toFloat() / mistClusterCount) * totalLength
        var sIdx = 0
        while (sIdx < n - 2 && cumDist[sIdx + 1] < distAlong) sIdx++
        val sStart = cumDist[sIdx]
        val sEnd = cumDist[sIdx + 1]
        val segLen = (sEnd - sStart).coerceAtLeast(0.001f)
        val u = ((distAlong - sStart) / segLen).coerceIn(0f, 1f)
        val mx = screenPts[sIdx].x + (screenPts[sIdx + 1].x - screenPts[sIdx].x) * u
        val my = screenPts[sIdx].y + (screenPts[sIdx + 1].y - screenPts[sIdx].y) * u

        // Atmospheric feathered aerosol aura
        val auraAlpha = (scaledAlpha * 0.085f * coreSaturation).coerceIn(0.005f, 0.25f)
        drawCircle(
            color = baseColor.copy(alpha = auraAlpha),
            radius = dynamicRadius * 0.95f,
            center = Offset(mx, my)
        )
        drawCircle(
            color = baseColor.copy(alpha = auraAlpha * 1.5f),
            radius = dynamicRadius * 0.60f,
            center = Offset(mx, my)
        )

        // Ultra-fine micro-particulate vapor droplets
        val vaporDots = (stroke.strokeWidth * 0.90f).toInt().coerceIn(10, 26)
        for (vd in 0 until vaporDots) {
            val r1 = fastSprayRandomFloat(seed, m * 47 + vd * 3)
            val r2 = fastSprayRandomFloat(seed, m * 47 + vd * 3 + 1)
            val r3 = fastSprayRandomFloat(seed, m * 47 + vd * 3 + 2)
            val angle = r1 * 6.2831855f
            val rFactor = (r2 * r3 + (r2 + r3) * 0.5f) * 0.5f
            val dotDist = rFactor * dynamicRadius * 0.92f
            val dx = mx + kotlin.math.cos(angle) * dotDist
            val dy = my + kotlin.math.sin(angle) * dotDist
            val vAlpha = (scaledAlpha * 0.28f * coreSaturation * (1f - rFactor * 0.55f)).coerceIn(0.01f, 1.0f)
            drawCircle(
                color = baseColor.copy(alpha = vAlpha),
                radius = (stroke.strokeWidth * 0.06f).coerceIn(0.7f, 1.8f) * (0.8f + r1 * 0.4f),
                center = Offset(dx, dy)
            )
        }
    }

    // 2. LAYER 2: High-Density Aerosol Core with Organic Pressure Drops
    val clusterSpacing = (dynamicRadius * 0.24f).coerceIn(2.8f, 10f)
    val numClusters = (totalLength / clusterSpacing).toInt().coerceAtLeast(6)
    val dotsPerCluster = (stroke.strokeWidth * 1.45f).toInt().coerceIn(20, 44)

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

        // Neon / Pulsing aura integration
        if (isPulsing && c % 3 == 0) {
            drawCircle(
                color = baseColor.copy(alpha = (scaledAlpha * 0.12f * neonPulse).coerceIn(0.01f, 0.45f)),
                radius = dynamicRadius * 1.30f,
                center = Offset(cx, cy)
            )
        }

        for (d in 0 until dotsPerCluster) {
            val r1 = fastSprayRandomFloat(seed, c * 79 + d * 3)
            val r2 = fastSprayRandomFloat(seed, c * 79 + d * 3 + 1)
            val r3 = fastSprayRandomFloat(seed, c * 79 + d * 3 + 2)

            // Gaussian dropoff across perpendicular axis
            val rFactor = (r1 * r2 * 2f - 1.0f)
            val perpDist = rFactor * dynamicRadius
            val tangOffset = (r3 - 0.5f) * clusterSpacing * 0.90f

            val dotX = cx + normX * perpDist + dirX * tangOffset
            val dotY = cy + normY * perpDist + dirY * tangOffset

            val absR = kotlin.math.abs(rFactor)
            val isCore = absR < 0.35f
            val isMicroSplatter = r3 > 0.88f && absR > 0.60f
            val isAerosolDroplet = r2 > 0.84f

            val dotRadius = when {
                isMicroSplatter -> (stroke.strokeWidth * 0.11f).coerceIn(1.3f, 3.4f) * (0.85f + r1 * 0.4f)
                isCore -> (stroke.strokeWidth * 0.08f).coerceIn(1.0f, 2.4f) * (0.9f + r2 * 0.3f)
                else -> (stroke.strokeWidth * 0.06f).coerceIn(0.7f, 1.9f) * (0.8f + r3 * 0.4f)
            }

            val centerDensity = (1.0f - absR * 0.42f).coerceIn(0.28f, 1.0f)
            val dotAlpha = (scaledAlpha * coreSaturation * centerDensity * (0.70f + r1 * 0.30f)).coerceIn(0.02f, 1.0f)

            drawCircle(
                color = baseColor.copy(alpha = dotAlpha),
                radius = dotRadius,
                center = Offset(dotX, dotY)
            )

            // Micro-droplet gloss & wet spray highlight on core particles
            if (isCore && isAerosolDroplet) {
                val highlightAlpha = if (isPulsing) {
                    (dotAlpha * 0.90f * neonPulse).coerceIn(0.01f, 1.0f)
                } else {
                    (dotAlpha * 0.52f).coerceIn(0.01f, 1.0f)
                }
                drawCircle(
                    color = Color.White.copy(alpha = highlightAlpha),
                    radius = dotRadius * 0.50f,
                    center = Offset(dotX, dotY)
                )
            }
        }

        // Sparkling glitter starbursts if modifier is active
        if (isSparkling && (c % 3 == 0 || fastSprayRandomFloat(seed, c * 17 + 5) > 0.82f)) {
            val starPhase = (neonPulse + fastSprayRandomFloat(seed, c * 19 + 7)) % 1.0f
            val starAlpha = (scaledAlpha * (0.50f + 0.50f * kotlin.math.sin(starPhase * Math.PI.toFloat()))).coerceIn(0.01f, 1.0f)
            val starSize = (stroke.strokeWidth * 0.12f).coerceIn(1.8f, 4.2f) * (2.0f + 1.2f * starPhase)

            val sparkleCrossColor = Color(
                red = (baseColor.red * 0.55f + 0.45f).coerceIn(0f, 1f),
                green = (baseColor.green * 0.55f + 0.45f).coerceIn(0f, 1f),
                blue = (baseColor.blue * 0.55f + 0.45f).coerceIn(0f, 1f),
                alpha = starAlpha
            )

            drawLine(
                color = sparkleCrossColor,
                start = Offset(cx - starSize, cy),
                end = Offset(cx + starSize, cy),
                strokeWidth = 1.6f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = sparkleCrossColor,
                start = Offset(cx, cy - starSize),
                end = Offset(cx, cy + starSize),
                strokeWidth = 1.6f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color.White.copy(alpha = (starAlpha * 0.90f).coerceIn(0.01f, 1.0f)),
                radius = (stroke.strokeWidth * 0.08f).coerceIn(1.0f, 2.5f),
                center = Offset(cx, cy)
            )
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

