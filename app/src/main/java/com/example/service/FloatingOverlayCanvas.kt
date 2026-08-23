package com.example.service

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import com.example.data.model.BrushType
import com.example.data.repository.DrawingRepository
import com.example.ui.components.BrushPaletteBar
import com.example.ui.components.ColorPickerBar
import com.example.ui.components.DrawingCanvas
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
    val strokes by repository.strokes.collectAsState()
    val currentDraft by repository.currentDraftStroke.collectAsState()
    val partnerDraft by repository.partnerDraftStroke.collectAsState()
    val placedStickers by repository.placedStickers.collectAsState()
    val selectedStickerId by repository.selectedStickerId.collectAsState()
    val selectedBrush by repository.selectedBrushType.collectAsState()
    val selectedColor by repository.selectedColor.collectAsState()
    val strokeWidth by repository.strokeWidth.collectAsState()
    val strokeAlpha by repository.strokeAlpha.collectAsState()
    val partnerPresence by repository.partnerPresence.collectAsState()
    val floatingReactions by repository.floatingReactions.collectAsState()

    var showStickersSheet by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }

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
            onStartDraw = { x, y -> repository.startDrawing(x, y) },
            onContinueDraw = { x, y -> repository.continueDrawing(x, y) },
            onFinishDraw = { repository.finishDrawing() },
            onSelectSticker = { repository.selectSticker(it) },
            onUpdateStickerPos = { id, x, y -> repository.updateStickerPosition(id, x, y) },
            onUpdateStickerTransform = { id, scale, rot -> repository.updateStickerTransform(id, scale, rot) },
            onDeleteSticker = { repository.deleteSticker(it) },
            modifier = Modifier.fillMaxSize(),
            isInteractive = true
        )

        // 2. Floating Toggle/Close Bubble positioned right on screen where the user put it
        Box(
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
        }

        // 3. Bottom Frosted Glass Dock with Color Picker & Brushes (Dark-tinted for high contrast)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Color bar
            if (selectedBrush != BrushType.ERASER) {
                Surface(
                    color = Color(0xE612131F),
                    shape = RoundedCornerShape(32.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x38FFFFFF)),
                    shadowElevation = 12.dp,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    ColorPickerBar(
                        selectedColor = selectedColor,
                        onColorSelected = { repository.selectColor(it) }
                    )
                }
            }

            // Brush Palette Bar
            BrushPaletteBar(
                selectedBrush = selectedBrush,
                strokeWidth = strokeWidth,
                strokeAlpha = strokeAlpha,
                currentColor = selectedColor,
                onSelectBrush = { repository.selectBrush(it) },
                onSelectStrokeWidth = { repository.setStrokeWidth(it) },
                onSelectStrokeAlpha = { repository.setStrokeAlpha(it) },
                onOpenStickers = { showStickersSheet = true },
                onClearCanvas = { showClearConfirmation = true },
                onUndo = { repository.undo() },
                onRedo = { repository.redo() },
                onMinimize = { onMinimizeToBubble() }
            )
        }

        // Stickers Bottom Sheet Overlay
        if (showStickersSheet) {
            StickerBottomSheet(
                onDismiss = { showStickersSheet = false },
                onSelectSticker = { emojiOrText ->
                    repository.addSticker(emojiOrText)
                    showStickersSheet = false
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
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0x44FF5252)),
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
