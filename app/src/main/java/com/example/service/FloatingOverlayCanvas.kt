package com.example.service

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrushType
import com.example.data.repository.DrawingRepository
import com.example.ui.components.BrushPaletteBar
import com.example.ui.components.ColorPickerBar
import com.example.ui.components.DrawingCanvas
import com.example.ui.components.StickerBottomSheet
import com.example.ui.components.WallpaperBottomSheet
import com.example.util.WallpaperTarget
import kotlinx.coroutines.launch

@Composable
fun FloatingOverlayCanvas(
    repository: DrawingRepository,
    onMinimizeToBubble: () -> Unit,
    onCloseService: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
    val roomCode by repository.roomCode.collectAsState()
    val floatingReactions by repository.floatingReactions.collectAsState()
    val lockscreenConfig by repository.lockscreenConfig.collectAsState()

    var showStickersSheet by remember { mutableStateOf(false) }
    var showWallpaperSheet by remember { mutableStateOf(false) }

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

        // 2. Bottom Frosted Glass Dock with Color Picker & Brushes (Dark-tinted for high contrast)
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
                onOpenWallpapers = { showWallpaperSheet = true },
                onClearCanvas = { repository.clearAllCanvas() },
                onUndo = { repository.undo() },
                onRedo = { repository.redo() }
            )
        }

        // Stickers Bottom Sheet
        if (showStickersSheet) {
            StickerBottomSheet(
                onDismiss = { showStickersSheet = false },
                onSelectSticker = { emojiOrText ->
                    repository.addSticker(emojiOrText)
                    showStickersSheet = false
                }
            )
        }

        // Wallpaper / Lockscreen Options Sheet
        if (showWallpaperSheet) {
            WallpaperBottomSheet(
                currentTheme = lockscreenConfig.wallpaperTheme,
                isOverlayVisible = true,
                onSelectTheme = { theme ->
                    repository.setWallpaperTheme(theme)
                    showWallpaperSheet = false
                },
                onToggleOverlay = { repository.toggleLockscreenOverlay() },
                onDismiss = { showWallpaperSheet = false }
            )
        }
    }
}
