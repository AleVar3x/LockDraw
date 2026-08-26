package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DrawingStroke
import com.example.data.model.LockscreenConfig
import com.example.data.model.PlacedSticker
import com.example.data.repository.FloatingHeartReaction
import com.example.data.sync.PartnerPresence

@Composable
fun LockscreenSimulatorDialog(
    config: LockscreenConfig,
    roomCode: String,
    strokes: List<DrawingStroke>,
    stickers: List<PlacedSticker>,
    partnerPresence: PartnerPresence,
    floatingReactions: List<FloatingHeartReaction>,
    isMyDrawingsTransparent: Boolean = false,
    onDismiss: () -> Unit
) {
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("lockscreen_simulator_fullscreen")
        ) {
            // 1. Wallpaper background
            LockscreenBackground(
                config = config,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Lockscreen Clock, Status Bar, Partner Notification
            LockscreenOverlayWidgets(
                config = config,
                roomCode = roomCode,
                partnerIsOnline = partnerPresence.isOnline,
                visible = true,
                modifier = Modifier.fillMaxSize()
            )

            // 3. Drawing Canvas overlay (read-only in simulator preview)
            DrawingCanvas(
                strokes = strokes,
                currentDraft = null,
                partnerDraft = null,
                stickers = stickers,
                selectedStickerId = null,
                partnerPresence = partnerPresence,
                floatingReactions = floatingReactions,
                isMyDrawingsTransparent = isMyDrawingsTransparent,
                onStartDraw = { _, _ -> },
                onContinueDraw = { _, _ -> },
                onFinishDraw = {},
                onSelectSticker = {},
                onUpdateStickerPos = { _, _, _ -> },
                onUpdateStickerTransform = { _, _, _ -> },
                onDeleteSticker = {},
                modifier = Modifier.fillMaxSize()
            )

            // Close Simulator / Unlock button
            Surface(
                color = Color(0x1EFFFFFF),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2EFFFFFF)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 16.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_simulator_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi Simulatore",
                        tint = Color.White
                    )
                }
            }

            // Bottom Unlock Prompt
            Surface(
                color = Color(0x22FFFFFF),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2EFFFFFF)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .clickable { onDismiss() }
            ) {
                Text(
                    text = "🔒 Tocca per sbloccare e tornare a disegnare",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}
