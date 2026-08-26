package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

data class PaywallFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconTint: Color
)

@Composable
fun PaywallDialog(
    onDismiss: () -> Unit,
    onUnlockSuccess: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_vip")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vip_scale"
    )

    val features = listOf(
        PaywallFeature(
            title = "Pennello Dot Flow",
            description = "Puntini animati che scorrono e viaggiano fluidamente nella direzione del tratto.",
            icon = Icons.Default.ScatterPlot,
            iconTint = Color(0xFFFFD54F)
        ),
        PaywallFeature(
            title = "Pennello Wave",
            description = "Tratti animati vivi che ondeggiano e fluiscono in tempo reale sulla tela di coppia.",
            icon = Icons.Default.Waves,
            iconTint = Color(0xFF00E5FF)
        ),
        PaywallFeature(
            title = "Pennello Penna Ondulata",
            description = "Tratti organici a onda sinusoidale per dediche, firme e disegni unici.",
            icon = Icons.Default.Gesture,
            iconTint = Color(0xFF80FFEA)
        ),
        PaywallFeature(
            title = "Pennello Pulsing",
            description = "Luce al neon viva che respira e pulsa visivamente sulla schermata.",
            icon = Icons.Default.AutoAwesome,
            iconTint = Color(0xFFFF2A6D)
        ),
        PaywallFeature(
            title = "Creatore Sticker Stile WhatsApp",
            description = "Crea adesivi personalizzati con testi, colori, emoji e disegni a mano.",
            icon = Icons.Default.ContentCut,
            iconTint = Color(0xFF25D366)
        ),
        PaywallFeature(
            title = "Esperienza VIP Completa",
            description = "Badge premium per entrambi, sincronizzazione prioritaria e nessun limite.",
            icon = Icons.Default.WorkspacePremium,
            iconTint = Color(0xFFFFD54F)
        )
    )

    // Fullscreen in-compose overlay - 100% crash-proof in Activity & Service Overlay Window
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent closing when clicking inside card */ }
                )
                .clip(RoundedCornerShape(28.dp))
                .testTag("paywall_dialog"),
            color = Color(0xFF141324),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF2A6D), Color(0xFF8E7CFF)))),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Close button & VIP Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0x33FFD54F),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0x88FFD54F))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "LOCKDRAW VIP PASS",
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Crown Hero Icon with breathing pulse
                Box(
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F),
                                    Color(0xFFFF9100),
                                    Color(0xFFFF2A6D)
                                )
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sblocca la Versione Completa",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Tutti i nuovi pennelli avanzati e il creatore sticker per te e il tuo partner.",
                    color = Color(0xFFD0BCFF),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Features list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    features.forEach { feature ->
                        Surface(
                            color = Color(0x18FFFFFF),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0x1FFFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(feature.iconTint.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = feature.icon,
                                        contentDescription = null,
                                        tint = feature.iconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = feature.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = feature.description,
                                        color = Color(0xFFB0AEC7),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Price Card
                Surface(
                    color = Color(0x2E4F378B),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, Color(0x88D0BCFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Offerta Speciale",
                                color = Color(0xFFFFD54F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Acquisto Una Tantum",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "4,99 €",
                            color = Color(0xFF80FFEA),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CTA Unlock Button
                Button(
                    onClick = onUnlockSuccess,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF2A6D),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("paywall_unlock_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sblocca Tutto a 4,99 €",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Restore purchase & Dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = onUnlockSuccess,
                        modifier = Modifier.testTag("restore_purchases_btn")
                    ) {
                        Text(
                            text = "Ripristina Acquisti",
                            color = Color(0xFFB0AEC7),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
