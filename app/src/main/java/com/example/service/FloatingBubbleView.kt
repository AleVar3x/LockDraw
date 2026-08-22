package com.example.service

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.repository.DrawingRepository

@Composable
fun FloatingBubbleView(
    repository: DrawingRepository,
    isExpanded: Boolean = false,
    onMove: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    onToggle: () -> Unit
) {
    val partnerPresence by repository.partnerPresence.collectAsState()
    val partnerDraft by repository.partnerDraftStroke.collectAsState()

    var hasDragged by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        color = Color(0x66101424),
        shape = CircleShape,
        border = BorderStroke(
            1.2.dp,
            if (isExpanded) Color(0xCCFF5252) else Color(0x6600E5FF)
        ),
        shadowElevation = 6.dp,
        modifier = Modifier
            .size(48.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { hasDragged = false },
                    onDragEnd = { /* no-op */ },
                    onDragCancel = { /* no-op */ },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        hasDragged = true
                        onMove(dragAmount.x, dragAmount.y)
                    }
                )
            }
            .clickable {
                if (!hasDragged) {
                    onToggle()
                }
            }
            .testTag("floating_bubble_btn")
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    Brush.radialGradient(
                        colors = if (isExpanded) {
                            listOf(Color(0x80D50000), Color(0x5012131F))
                        } else {
                            listOf(Color(0x7000E5FF), Color(0x4012131F))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Default.Close
                } else if (partnerDraft != null) {
                    Icons.Default.Favorite
                } else {
                    Icons.Default.Draw
                },
                contentDescription = if (isExpanded) "Chiudi modalità disegno" else "Disegna su schermo",
                tint = if (isExpanded) {
                    Color(0xFFFF8A80)
                } else if (partnerDraft != null) {
                    Color(0xFFFF4081)
                } else {
                    Color(0xFFE0F7FA)
                },
                modifier = Modifier
                    .size(24.dp)
                    .scale(if (!isExpanded && partnerDraft != null) pulseScale else 1.0f)
            )

            // Online partner indicator dot
            if (!isExpanded) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-3).dp, y = 3.dp)
                        .background(
                            if (partnerPresence.isOnline) Color(0xFF00E676) else Color(0x66FFFFFF),
                            CircleShape
                        )
                        .padding(1.dp)
                )
            }
        }
    }
}
