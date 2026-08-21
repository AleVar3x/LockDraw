package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LockscreenConfig
import com.example.data.model.WallpaperTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LockscreenBackground(
    config: LockscreenConfig,
    modifier: Modifier = Modifier
) {
    val gradientColors = config.wallpaperTheme.gradientColors.map { Color(it) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = gradientColors)
            )
            .testTag("lockscreen_wallpaper_background")
    )
}

@Composable
fun LockscreenOverlayWidgets(
    config: LockscreenConfig,
    roomCode: String,
    partnerIsOnline: Boolean,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.ITALIAN)
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now).replaceFirstChar { it.uppercase() }
            delay(1000)
        }
    }

    val contentColor = if (config.wallpaperTheme.isDark) Color.White else Color(0xFF1E1E24)
    val cardBg = if (config.wallpaperTheme.isDark) Color(0x33FFFFFF) else Color(0x44000000)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Status bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LockDraw",
                    color = contentColor.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkCell,
                        contentDescription = "Rete",
                        tint = contentColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wi-Fi",
                        tint = contentColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "87%",
                        color = contentColor.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Lock icon and Clock / Date
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloccato",
                    tint = contentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentTime.ifEmpty { "10:42" },
                    color = contentColor.copy(alpha = 0.95f),
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-2).sp
                )

                Text(
                    text = currentDate.ifEmpty { "Venerdì, 21 Agosto" },
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Partner Notification Card (Frosted Glass Card)
                Surface(
                    color = Color(0x1AFFFFFF),
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .testTag("partner_lockscreen_widget")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF4F378B),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0BCFF)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Partner",
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Schermo Condiviso",
                                    color = contentColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(
                                            if (partnerIsOnline) Color(0xFF4ADE80) else Color(0x66FFFFFF),
                                            CircleShape
                                        )
                                )
                            }
                            Text(
                                text = "Codice: $roomCode • Disegna per inviare",
                                color = contentColor.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Bottom Shortcuts (Flashlight & Camera) + Swipe handle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0x1AFFFFFF),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FlashlightOn,
                                contentDescription = "Torcia",
                                tint = contentColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Surface(
                        color = Color(0x1AFFFFFF),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Fotocamera",
                                tint = contentColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(4.dp)
                        .background(contentColor.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
