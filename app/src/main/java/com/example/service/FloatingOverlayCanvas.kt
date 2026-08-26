package com.example.service

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrushType
import com.example.data.repository.DrawingRepository
import com.example.ui.components.BrushPaletteBar
import com.example.ui.components.ColorPickerBar
import com.example.ui.components.CustomStickerCreatorDialog
import com.example.ui.components.DrawingCanvas
import com.example.ui.components.PaywallDialog
import com.example.ui.components.StickerBottomSheet

@Composable
fun FloatingOverlayCanvas(
    repository: DrawingRepository,
    initialBubbleX: Int = 30,
    initialBubbleY: Int = 350,
    onUpdateBubblePos: (Int, Int) -> Unit = { _, _ -> },
    onMinimizeToBubble: () -> Unit,
    onCloseService: () -> Unit
) {
    val context = LocalContext.current
    val strokes by repository.strokes.collectAsState()
    val currentDraft by repository.currentDraftStroke.collectAsState()
    val partnerDraft by repository.partnerDraftStroke.collectAsState()
    val placedStickers by repository.placedStickers.collectAsState()
    val selectedStickerId by repository.selectedStickerId.collectAsState()
    val selectedBrush by repository.selectedBrushType.collectAsState()
    val selectedModifier by repository.selectedModifier.collectAsState()
    val selectedColor by repository.selectedColor.collectAsState()
    val strokeWidth by repository.strokeWidth.collectAsState()
    val strokeAlpha by repository.strokeAlpha.collectAsState()
    val partnerPresence by repository.partnerPresence.collectAsState()
    val floatingReactions by repository.floatingReactions.collectAsState()
    val isPremiumUnlocked by repository.isPremiumUnlocked.collectAsState()
    val customStickers by repository.customStickers.collectAsState()
    val isMyDrawingsTransparent by repository.isMyDrawingsTransparent.collectAsState()

    var showStickersSheet by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showPaywallDialog by remember { mutableStateOf(false) }
    var showCustomStickerCreator by remember { mutableStateOf(false) }
    var isPaletteCollapsed by remember { mutableStateOf(false) }

    var bubbleX by remember { mutableStateOf(initialBubbleX.toFloat()) }
    var bubbleY by remember { mutableStateOf(initialBubbleY.toFloat()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x01000000))
    ) {
        // 1. Transparent Drawing Canvas directly over the real device screen
        DrawingCanvas(
            strokes = strokes,
            currentDraft = currentDraft,
            partnerDraft = partnerDraft,
            stickers = placedStickers,
            selectedStickerId = selectedStickerId,
            partnerPresence = partnerPresence,
            floatingReactions = floatingReactions,
            isMyDrawingsTransparent = isMyDrawingsTransparent,
            onStartDraw = { x, y ->
                repository.startDrawing(x, y)
            },
            onContinueDraw = { x, y -> repository.continueDrawing(x, y) },
            onFinishDraw = { repository.finishDrawing() },
            onSelectSticker = { repository.selectSticker(it) },
            onUpdateStickerPos = { id, x, y -> repository.updateStickerPosition(id, x, y) },
            onUpdateStickerDelta = { id, dx, dy -> repository.moveStickerDelta(id, dx, dy) },
            onUpdateStickerTransform = { id, scale, rot -> repository.updateStickerTransform(id, scale, rot) },
            onDeleteSticker = { repository.deleteSticker(it) },
            modifier = Modifier.fillMaxSize(),
            isInteractive = true
        )

        // 2. Floating Controls (Floating Bubble + My Drawings 10%/100% Alpha Switch)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .offset { IntOffset(bubbleX.toInt(), bubbleY.toInt()) }
        ) {
            FloatingBubbleView(
                repository = repository,
                isExpanded = true,
                onMove = { dx, dy ->
                    bubbleX = (bubbleX + dx).coerceAtLeast(0f)
                    bubbleY = (bubbleY + dy).coerceAtLeast(0f)
                    onUpdateBubblePos(bubbleX.toInt(), bubbleY.toInt())
                },
                onToggle = {
                    onMinimizeToBubble()
                }
            )

            // Switch on/off: Trasparenza 20% / 100% per i tuoi disegni (solo icona occhio)
            Surface(
                color = if (isMyDrawingsTransparent) Color(0xDD311B92) else Color(0x66101424),
                shape = CircleShape,
                border = BorderStroke(
                    1.4.dp,
                    if (isMyDrawingsTransparent) Color(0xFFFF4081) else Color(0x6600E5FF)
                ),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        repository.toggleMyDrawingsTransparency()
                    }
                    .testTag("my_drawings_transparency_toggle")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMyDrawingsTransparent) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isMyDrawingsTransparent) "Trasparenza 20% attiva" else "Opacità 100%",
                        tint = if (isMyDrawingsTransparent) Color(0xFFFF80AB) else Color(0xFFE0F7FA),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // 3. Collapsible Palette & Swipe-up Handle at bottom screen
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Collapsed slim handle: allows drawing completely down to the bottom of the screen
            AnimatedVisibility(
                visible = isPaletteCollapsed,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Surface(
                    color = Color(0xE6141524),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.2.dp, Color(0x66D0BCFF)),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -8f) {
                                    isPaletteCollapsed = false
                                }
                            }
                        }
                        .clickable { isPaletteCollapsed = false }
                        .testTag("collapsed_palette_handle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Swipe up per aprire la tavolozza",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Swipe up per aprire tavolozza",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        // Live active color & brush indicator preview
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(selectedColor)
                                .border(1.dp, Color(0x99FFFFFF), CircleShape)
                        )
                    }
                }
            }

            // Expanded Full Dock with Color Picker & Brushes (supports swipe-down to dismiss)
            AnimatedVisibility(
                visible = !isPaletteCollapsed,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount > 15f) {
                                    isPaletteCollapsed = true
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Color bar (if not eraser)
                    if (selectedBrush != BrushType.ERASER) {
                        Surface(
                            color = Color(0xE612131F),
                            shape = RoundedCornerShape(32.dp),
                            border = BorderStroke(1.2.dp, Color(0x38FFFFFF)),
                            shadowElevation = 12.dp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            ColorPickerBar(
                                selectedColor = selectedColor,
                                onColorSelected = {
                                    repository.selectColor(it)
                                    isPaletteCollapsed = true
                                }
                            )
                        }
                    }

                    // Brush Palette Bar
                    BrushPaletteBar(
                        selectedBrush = selectedBrush,
                        selectedModifier = selectedModifier,
                        strokeWidth = strokeWidth,
                        strokeAlpha = strokeAlpha,
                        currentColor = selectedColor,
                        isPremiumUnlocked = isPremiumUnlocked,
                        onSelectBrush = {
                            repository.selectBrush(it)
                            // If brush has no extra modifiers, auto-slide down to draw immediately
                            if (it.getSupportedModifiers().size <= 1) {
                                isPaletteCollapsed = true
                            }
                        },
                        onSelectModifier = {
                            repository.selectModifier(it)
                            isPaletteCollapsed = true
                        },
                        onSelectStrokeWidth = { repository.setStrokeWidth(it) },
                        onSelectStrokeAlpha = { repository.setStrokeAlpha(it) },
                        onOpenStickers = { showStickersSheet = true },
                        onClearCanvas = { showClearConfirmation = true },
                        onUndo = { repository.undo() },
                        onRedo = { repository.redo() },
                        onOpenPaywall = { showPaywallDialog = true },
                        onMinimize = { isPaletteCollapsed = true }
                    )
                }
            }
        }

        // Stickers Bottom Sheet Overlay
        if (showStickersSheet) {
            StickerBottomSheet(
                isPremiumUnlocked = isPremiumUnlocked,
                customStickers = customStickers,
                onDismiss = { showStickersSheet = false },
                onSelectSticker = { emojiOrText ->
                    repository.addSticker(emojiOrText)
                    showStickersSheet = false
                },
                onOpenCustomStickerCreator = {
                    showStickersSheet = false
                    showCustomStickerCreator = true
                },
                onDeleteCustomSticker = { repository.deleteCustomSticker(it) },
                onOpenPaywall = {
                    showStickersSheet = false
                    showPaywallDialog = true
                }
            )
        }

        // Custom Sticker Creator Dialog
        if (showCustomStickerCreator) {
            CustomStickerCreatorDialog(
                onDismiss = { showCustomStickerCreator = false },
                onCreateSticker = { createdSticker ->
                    repository.addCustomSticker(createdSticker)
                    showCustomStickerCreator = false
                }
            )
        }

        // Paywall VIP Dialog
        if (showPaywallDialog) {
            PaywallDialog(
                onDismiss = { showPaywallDialog = false },
                onUnlockSuccess = {
                    repository.unlockPremium(true)
                    showPaywallDialog = false
                    android.widget.Toast.makeText(context, "LockDraw VIP sbloccato a 4,99 €! 🎉", android.widget.Toast.LENGTH_LONG).show()
                }
            )
        }

        // Clear Canvas Confirmation Overlay
        if (showClearConfirmation) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable { showClearConfirmation = false },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF1E1D30),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, Color(0x44FF5252)),
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(38.dp)
                        )

                        Text(
                            text = "Cancellare la lavagna?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )

                        Text(
                            text = "Verranno rimossi tutti i tratti e gli sticker dallo schermo condiviso.",
                            fontSize = 13.sp,
                            color = Color(0xFFB0AEC7),
                            lineHeight = 18.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { showClearConfirmation = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Annulla", color = Color(0xFFB0AEC7))
                            }

                            Button(
                                onClick = {
                                    repository.clearAllCanvas()
                                    showClearConfirmation = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text("Cancella Tutto", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
