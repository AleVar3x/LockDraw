package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BrushType
import com.example.data.model.StrokeModifier

data class BrushToolItem(
    val type: BrushType,
    val label: String,
    val icon: ImageVector? = null,
    val iconResId: Int? = null,
    val isPremium: Boolean = false
)

// Primary clean base brush tools with custom eraser and watercolor icons
val PRIMARY_BRUSH_TOOLS = listOf(
    BrushToolItem(BrushType.ERASER, "Gomma", iconResId = R.drawable.ic_eraser),
    BrushToolItem(BrushType.PEN, "Penna", icon = Icons.Default.Edit),
    BrushToolItem(BrushType.WATERCOLOR, "Acquerello", iconResId = R.drawable.ic_watercolor_brush),
    BrushToolItem(BrushType.PENCIL, "Matita", icon = Icons.Default.Create),
    BrushToolItem(BrushType.HIGHLIGHTER, "Evidenziatore", icon = Icons.Default.Highlight),
    BrushToolItem(BrushType.NEON, "Neon Glow", icon = Icons.Default.AutoAwesome),
    BrushToolItem(BrushType.SPRAY, "Spray", icon = Icons.Default.BlurOn, isPremium = true),
    BrushToolItem(BrushType.RAINBOW, "Arcobaleno", icon = Icons.Default.InvertColors),
    BrushToolItem(BrushType.DOTTED, "Puntini", icon = Icons.Default.MoreHoriz)
)

private fun getModifierIcon(modifier: StrokeModifier): ImageVector {
    return when (modifier) {
        StrokeModifier.NONE -> Icons.Default.Edit
        StrokeModifier.CALLIGRAPHY -> Icons.Default.Gesture
        StrokeModifier.WAVE -> Icons.Default.Waves
        StrokeModifier.PULSING -> Icons.Default.Favorite
        StrokeModifier.SPARKLING -> Icons.Default.AutoAwesome
        StrokeModifier.DOT_FLOW -> Icons.Default.ScatterPlot
    }
}

@Composable
fun BrushPaletteBar(
    selectedBrush: BrushType,
    selectedModifier: StrokeModifier = StrokeModifier.NONE,
    strokeWidth: Float,
    strokeAlpha: Float,
    currentColor: Color,
    isPremiumUnlocked: Boolean = false,
    onSelectBrush: (BrushType) -> Unit,
    onSelectModifier: (StrokeModifier) -> Unit = {},
    onSelectStrokeWidth: (Float) -> Unit,
    onSelectStrokeAlpha: (Float) -> Unit,
    onOpenStickers: () -> Unit,
    onOpenPaywall: () -> Unit = {},
    onClearCanvas: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onMinimize: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showSliders by remember { mutableStateOf(false) }
    val supportedModifiers = selectedBrush.getSupportedModifiers()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expandable Slider Controls for Stroke Width & Opacity or Eraser Size
        AnimatedVisibility(
            visible = showSliders,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                color = Color(0xE6141522),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (selectedBrush == BrushType.ERASER) Color(0x6600E676) else Color(0x40D0BCFF)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (selectedBrush == BrushType.ERASER) {
                        // Dedicated Eraser Size Slider & Presets
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Dimensione Gomma: ${strokeWidth.toInt()}px",
                                    color = Color(0xFFB9F6CA),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Cancellazione selettiva in tempo reale",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                            // Live Eraser Circle Preview
                            Box(
                                modifier = Modifier
                                    .size(strokeWidth.coerceIn(12f, 44f).dp)
                                    .clip(CircleShape)
                                    .background(Color(0x3300E676))
                                    .border(2.dp, Color(0xFF00E676), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = strokeWidth,
                            onValueChange = onSelectStrokeWidth,
                            valueRange = 8f..80f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E676),
                                activeTrackColor = Color(0xFF00E676),
                                inactiveTrackColor = Color(0x3300E676)
                            ),
                            modifier = Modifier.testTag("eraser_size_slider")
                        )

                        // Quick Eraser Size Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Pair(12f, "Fine (12px)"),
                                Pair(28f, "Medio (28px)"),
                                Pair(48f, "Grande (48px)"),
                                Pair(75f, "Max (75px)")
                            ).forEach { (sizeVal, label) ->
                                val isCurSize = (strokeWidth - sizeVal).let { it >= -3f && it <= 3f }
                                Surface(
                                    color = if (isCurSize) Color(0xFF00C853) else Color(0x2200E676),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isCurSize) Color(0xFFB9F6CA) else Color(0x4400E676)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectStrokeWidth(sizeVal) }
                                        .testTag("eraser_preset_${sizeVal.toInt()}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isCurSize) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurSize) Color.White else Color(0xFFB9F6CA)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard Brush Size Slider
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
        }

        // Upper Control Bar: Tune button alongside the Stroke Style modifiers / Eraser presets
        Surface(
            color = Color(0xF2161729),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.2.dp, if (selectedBrush == BrushType.ERASER) Color(0x6600E676) else Color(0x44D0BCFF)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .padding(bottom = 6.dp)
                .testTag("modifier_submenu_bar")
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tune / Sliders toggle button without text
                Surface(
                    color = if (showSliders) (if (selectedBrush == BrushType.ERASER) Color(0xFF00C853) else Color(0xFF5E35B1)) else Color(0x3325273C),
                    shape = RoundedCornerShape(14.dp),
                    border = if (showSliders) {
                        BorderStroke(1.5.dp, if (selectedBrush == BrushType.ERASER) Color(0xFFB9F6CA) else Color(0xFFD0BCFF))
                    } else {
                        BorderStroke(1.dp, Color(0x26FFFFFF))
                    },
                    modifier = Modifier
                        .clickable { showSliders = !showSliders }
                        .testTag("toggle_sliders_btn")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Regola Tratto o Gomma",
                            tint = if (showSliders) Color.White else (if (selectedBrush == BrushType.ERASER) Color(0xFF69F0AE) else Color(0xFFD0BCFF)),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // If Eraser is active, display quick size presets in the top bar directly
                if (selectedBrush == BrushType.ERASER) {
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(1.dp)
                            .background(Color(0x33FFFFFF))
                    )

                    listOf(
                        Pair(12f, "12px"),
                        Pair(28f, "28px"),
                        Pair(48f, "48px"),
                        Pair(75f, "75px")
                    ).forEach { (sizeVal, label) ->
                        val isCurSize = (strokeWidth - sizeVal).let { it >= -3f && it <= 3f }
                        Surface(
                            color = if (isCurSize) Color(0xFF00C853) else Color(0x3325273C),
                            shape = RoundedCornerShape(14.dp),
                            border = if (isCurSize) {
                                BorderStroke(1.5.dp, Color(0xFFB9F6CA))
                            } else {
                                BorderStroke(1.dp, Color(0x26FFFFFF))
                            },
                            modifier = Modifier
                                .clickable { onSelectStrokeWidth(sizeVal) }
                                .testTag("quick_eraser_size_${sizeVal.toInt()}")
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isCurSize) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurSize) Color.White else Color(0xFFB9F6CA),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // If the active brush supports multiple modifiers, display style icons
                if (supportedModifiers.size > 1 && selectedBrush != BrushType.ERASER) {
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(1.dp)
                            .background(Color(0x33FFFFFF))
                    )

                    supportedModifiers.forEach { mod ->
                        val isModSelected = selectedModifier == mod
                        val isModLocked = mod.isPremium && !isPremiumUnlocked

                        Surface(
                            color = if (isModSelected) Color(0xFF5E35B1) else Color(0x3325273C),
                            shape = RoundedCornerShape(14.dp),
                            border = if (isModSelected) {
                                BorderStroke(1.5.dp, Color(0xFFD0BCFF))
                            } else {
                                BorderStroke(1.dp, Color(0x26FFFFFF))
                            },
                            modifier = Modifier
                                .clickable {
                                    if (isModLocked) {
                                        onOpenPaywall()
                                    } else {
                                        onSelectModifier(mod)
                                    }
                                }
                                .testTag("modifier_btn_${mod.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = getModifierIcon(mod),
                                    contentDescription = mod.displayName,
                                    tint = if (isModSelected) Color.White else if (isModLocked) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                                if (isModLocked) {
                                    Surface(
                                        color = Color(0xFFFFD54F),
                                        shape = CircleShape,
                                        modifier = Modifier.size(12.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "VIP",
                                                tint = Color(0xFF1A1C2E),
                                                modifier = Modifier.size(7.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Main Bottom Frosted Glass Dock Toolbar
        Surface(
            color = Color(0xE612131F),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.2.dp, Color(0x38FFFFFF)),
            shadowElevation = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. FIXED Leftmost "Abbassa" (Minimize) button: remains sticky on the left on scroll
                if (onMinimize != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onMinimize() }
                            .padding(horizontal = 2.dp)
                    ) {
                        Surface(
                            color = Color(0xFF00B0FF),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("minimize_palette_btn"),
                            border = BorderStroke(1.5.dp, Color(0x80FFFFFF)),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Abbassa Tavolozza",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Abbassa",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF80D8FF)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Fixed vertical divider
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(Color(0x33FFFFFF))
                    )

                    Spacer(modifier = Modifier.width(6.dp))
                }

                // 2. SCROLLABLE Tools Row
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Brush Tools (Penna, Matita, Evidenziatore, Neon, Arcobaleno, Puntini, Gomma)
                    PRIMARY_BRUSH_TOOLS.forEach { tool ->
                        val isSelected = selectedBrush == tool.type
                        val isLocked = tool.isPremium && !isPremiumUnlocked
                        val isEraser = tool.type == BrushType.ERASER

                        // Styling for regular tools vs. highlighted green Gomma
                        val buttonBg = when {
                            isEraser && isSelected -> Color(0xFF00C853)
                            isEraser -> Color(0x3300E676)
                            isSelected -> Color(0xFF4F378B)
                            else -> Color(0x2826283C)
                        }
                        val buttonBorder = when {
                            isEraser && isSelected -> BorderStroke(1.8.dp, Color(0xFFB9F6CA))
                            isEraser -> BorderStroke(1.2.dp, Color(0x6600E676))
                            isSelected -> BorderStroke(1.5.dp, Color(0xFFD0BCFF))
                            else -> BorderStroke(1.dp, Color(0x26FFFFFF))
                        }
                        val iconColor = when {
                            isEraser && isSelected -> Color.White
                            isEraser -> Color(0xFF69F0AE)
                            isSelected -> Color.White
                            isLocked -> Color(0xFFFFD54F)
                            else -> Color.White.copy(alpha = 0.75f)
                        }
                        val labelColor = when {
                            isEraser && isSelected -> Color(0xFFB9F6CA)
                            isEraser -> Color(0xFF69F0AE)
                            isSelected -> Color(0xFFD0BCFF)
                            isLocked -> Color(0xFFFFD54F)
                            else -> Color.White.copy(alpha = 0.75f)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                if (isLocked) {
                                    onOpenPaywall()
                                } else {
                                    onSelectBrush(tool.type)
                                }
                            }
                        ) {
                            Box {
                                Surface(
                                    color = buttonBg,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .size(46.dp)
                                        .testTag("brush_btn_${tool.type.name}"),
                                    border = buttonBorder
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (tool.iconResId != null) {
                                            Icon(
                                                painter = painterResource(id = tool.iconResId),
                                                contentDescription = tool.label,
                                                tint = iconColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else if (tool.icon != null) {
                                            Icon(
                                                imageVector = tool.icon,
                                                contentDescription = tool.label,
                                                tint = iconColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                // VIP Badge on top right of locked items
                                if (isLocked) {
                                    Surface(
                                        color = Color(0xFFFFD54F),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.TopEnd)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "VIP",
                                                tint = Color(0xFF1A1C2E),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tool.label,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected || isEraser) FontWeight.Bold else FontWeight.Medium,
                                color = labelColor
                            )
                        }
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
                            border = BorderStroke(1.dp, Color(0x26FFFFFF))
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

                    // Undo (Annulla)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onUndo() }
                    ) {
                        Surface(
                            color = Color(0x2826283C),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("undo_btn"),
                            border = BorderStroke(1.dp, Color(0x26FFFFFF))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Annulla",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Annulla",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    // Redo (Ripristina)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onRedo() }
                    ) {
                        Surface(
                            color = Color(0x2826283C),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("redo_btn"),
                            border = BorderStroke(1.dp, Color(0x26FFFFFF))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = "Ripristina",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Ripristina",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                // 3. FIXED Rightmost "Cancella Tutto" button: remains sticky on the right on scroll
                Spacer(modifier = Modifier.width(6.dp))

                // Fixed vertical divider
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(Color(0x33FFFFFF))
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onClearCanvas() }
                        .padding(horizontal = 2.dp)
                ) {
                    Surface(
                        color = Color(0xFFC62828),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("clear_all_btn"),
                        border = BorderStroke(1.5.dp, Color(0xFFFF8A80)),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Cancella Tutto da Tutti i Layer",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cancella",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8A80)
                    )
                }
            }
        }
    }
}
