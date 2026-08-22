package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrushType

data class BrushToolItem(
    val type: BrushType,
    val label: String,
    val icon: ImageVector
)

val BRUSH_TOOLS = listOf(
    BrushToolItem(BrushType.PEN, "Penna", Icons.Default.Edit),
    BrushToolItem(BrushType.PENCIL, "Matita", Icons.Default.Create),
    BrushToolItem(BrushType.HIGHLIGHTER, "Evidenziatore", Icons.Default.Highlight),
    BrushToolItem(BrushType.NEON, "Neon Glow", Icons.Default.AutoAwesome),
    BrushToolItem(BrushType.RAINBOW, "Arcobaleno", Icons.Default.InvertColors),
    BrushToolItem(BrushType.DOTTED, "Puntini", Icons.Default.MoreHoriz),
    BrushToolItem(BrushType.ERASER, "Gomma", Icons.Default.CleaningServices)
)

@Composable
fun BrushPaletteBar(
    selectedBrush: BrushType,
    strokeWidth: Float,
    strokeAlpha: Float,
    currentColor: Color,
    onSelectBrush: (BrushType) -> Unit,
    onSelectStrokeWidth: (Float) -> Unit,
    onSelectStrokeAlpha: (Float) -> Unit,
    onOpenStickers: () -> Unit,
    onClearCanvas: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSliders by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Expandable Slider Controls for Stroke Width & Opacity
        AnimatedVisibility(visible = showSliders) {
            Surface(
                color = Color(0xE6141522),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40D0BCFF)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Size Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tratto: ${strokeWidth.toInt()}px",
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        // Live dot preview
                        Box(
                            modifier = Modifier
                                .size(strokeWidth.coerceIn(4f, 32f).dp)
                                .clip(CircleShape)
                                .background(currentColor.copy(alpha = strokeAlpha))
                                .border(1.dp, Color(0x66FFFFFF), CircleShape)
                        )
                    }
                    Slider(
                        value = strokeWidth,
                        onValueChange = onSelectStrokeWidth,
                        valueRange = 4f..60f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD0BCFF),
                            activeTrackColor = Color(0xFFD0BCFF),
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.testTag("stroke_width_slider")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Opacity Slider
                    Text(
                        text = "Opacità: ${(strokeAlpha * 100).toInt()}%",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = strokeAlpha,
                        onValueChange = onSelectStrokeAlpha,
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFC2E7FF),
                            activeTrackColor = Color(0xFFC2E7FF),
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.testTag("stroke_opacity_slider")
                    )
                }
            }
        }

        // Main Bottom Frosted Glass Dock Toolbar
        Surface(
            color = Color(0xE612131F),
            shape = RoundedCornerShape(32.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x38FFFFFF)),
            shadowElevation = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Brush Tools
                BRUSH_TOOLS.forEach { tool ->
                    val isSelected = selectedBrush == tool.type
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSelectBrush(tool.type) }
                    ) {
                        Surface(
                            color = if (isSelected) Color(0xFF4F378B) else Color(0x2826283C),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("brush_btn_${tool.type.name}"),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD0BCFF)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = tool.label,
                                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tool.label,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFFD0BCFF) else Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(Color(0x33FFFFFF))
                )

                // Adjust Sliders toggle button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showSliders = !showSliders }
                ) {
                    Surface(
                        color = if (showSliders) Color(0xFF4F378B) else Color(0x2826283C),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("toggle_sliders_btn"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "Tratto",
                                tint = if (showSliders) Color(0xFFD0BCFF) else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Regola", fontSize = 9.sp, color = Color.White.copy(alpha = 0.75f))
                }

                // Sticker Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onOpenStickers() }
                ) {
                    Surface(
                        color = Color(0x2826283C),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("open_stickers_btn"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EmojiEmotions,
                                contentDescription = "Sticker",
                                tint = Color(0xFFFEE285),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Sticker", fontSize = 9.sp, color = Color.White.copy(alpha = 0.75f))
                }

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(Color(0x33FFFFFF))
                )

                // Undo
                IconButton(
                    onClick = onUndo,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("undo_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Annulla",
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }

                // Redo
                IconButton(
                    onClick = onRedo,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("redo_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Ripristina",
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }

                // Clear (Cancella tutto)
                Surface(
                    color = Color(0x40FF5252),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FF5252)),
                    modifier = Modifier
                        .size(46.dp)
                        .clickable { onClearCanvas() }
                        .testTag("clear_all_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Cancella Tutto",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
