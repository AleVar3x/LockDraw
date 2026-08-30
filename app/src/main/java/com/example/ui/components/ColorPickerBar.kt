package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 32 chromatic color pairs (64 colors total): Starts from Red and transitions to Orange -> Yellow -> Lime -> Green -> Teal -> Cyan -> Blue -> Indigo -> Violet -> Magenta -> Pink -> Earthy -> White & Black
// Row 1 features vivid/deep hues, Row 2 features matching soft/pastel/toned shades
val CHROMATIC_COLOR_PAIRS_64: List<Pair<Color, Color>> = listOf(
    // 1. Reds & Corals
    Pair(Color(0xFFD50000), Color(0xFFFFCDD2)), // Crimson Deep Red / Soft Candy Red
    Pair(Color(0xFFFF1744), Color(0xFFFF8A80)), // Fluorescent Red / Soft Coral Pink
    Pair(Color(0xFFFF3D00), Color(0xFFFFAB91)), // Sunset Orange-Red / Soft Coral Salmon
    Pair(Color(0xFFFF6D00), Color(0xFFFFCCBC)), // Radiant Deep Orange / Pastel Apricot
    // 2. Oranges & Warm Golds
    Pair(Color(0xFFFF9100), Color(0xFFFFE0B2)), // Vivid Tangelo Orange / Pastel Peach
    Pair(Color(0xFFFFC107), Color(0xFFFFE082)), // Amber Warm Gold / Warm Pastel Honey
    Pair(Color(0xFFFFD600), Color(0xFFFFF59D)), // Golden Yellow / Soft Light Yellow
    Pair(Color(0xFFFFEA00), Color(0xFFFFF9C4)), // Neon Sun Yellow / Soft Cream Lemon
    // 3. Yellows & Limes
    Pair(Color(0xFFFFFF00), Color(0xFFFFFF8D)), // Pure Bright Yellow / Light Canary
    Pair(Color(0xFFAEEA00), Color(0xFFF4FF81)), // Vibrant Olive Lime / Light Electric Yellow-Green
    Pair(Color(0xFF76FF03), Color(0xFFCCFF90)), // Electric Laser Lime / Light Lime Pastel
    Pair(Color(0xFF64DD17), Color(0xFFDCEDC8)), // Vivid Green Lime / Pastel Pale Lime
    // 4. Greens & Emeralds
    Pair(Color(0xFF00E676), Color(0xFFB9F6CA)), // Mint Emerald Green / Pastel Mint Green
    Pair(Color(0xFF00C853), Color(0xFFC8E6C9)), // Vivid Leaf Green / Soft Spring Green
    Pair(Color(0xFF2E7D32), Color(0xFF69F0AE)), // Forest Emerald / Light Neon Emerald
    Pair(Color(0xFF1B5E20), Color(0xFFA7FFEB)), // Deep Dark Green / Pastel Seafoam
    // 5. Teals & Cyans
    Pair(Color(0xFF00897B), Color(0xFF80CBC4)), // Pine Teal / Soft Aquamarine
    Pair(Color(0xFF00BFA5), Color(0xFFE0F7FA)), // Deep Mint Aqua / Frosted Ice Mint
    Pair(Color(0xFF1DE9B6), Color(0xFF84FFFF)), // Bright Turquoise / Ice Aqua
    Pair(Color(0xFF00E5FF), Color(0xFFB2EBF2)), // Vivid Cyan / Pastel Aqua
    // 6. Blues & Indigos
    Pair(Color(0xFF00B0FF), Color(0xFF80D8FF)), // Bright Azure / Sky Blue Ice
    Pair(Color(0xFF2979FF), Color(0xFF90CAF9)), // Electric Dodger Blue / Ice Blue
    Pair(Color(0xFF448AFF), Color(0xFFBBDEFB)), // Vivid Blue / Baby Sky Blue
    Pair(Color(0xFF3D5AFE), Color(0xFF82B1FF)), // Royal Indigo / Soft Cerulean
    // 7. Violets, Purples & Magentas
    Pair(Color(0xFF651FFF), Color(0xFFB388FF)), // Deep Indigo Purple / Light Periwinkle
    Pair(Color(0xFF7C4DFF), Color(0xFFD1C4E9)), // Electric Violet / Pastel Lilac
    Pair(Color(0xFFAA00FF), Color(0xFFEA80FC)), // Vivid Violet / Pastel Orchid
    Pair(Color(0xFFD500F9), Color(0xFFE1BEE7)), // Hyper Magenta / Soft Lavender
    // 8. Pinks, Earthy & Monochromes
    Pair(Color(0xFFFF007F), Color(0xFFFF80AB)), // Electric Neon Pink / Pastel Bubblegum
    Pair(Color(0xFFC2185B), Color(0xFFF8BBD0)), // Deep Raspberry / Soft Rose Pink
    Pair(Color(0xFF5D4037), Color(0xFFD7CCC8)), // Deep Espresso Brown / Milk Tea Sand
    Pair(Color(0xFF101014), Color(0xFFFFFFFF))  // Pitch Black / Pure Brilliant White
)

val PRESET_COLORS_ROW1: List<Color> = CHROMATIC_COLOR_PAIRS_64.map { it.first }
val PRESET_COLORS_ROW2: List<Color> = CHROMATIC_COLOR_PAIRS_64.map { it.second }
val ALL_PRESET_COLORS: List<Color> = PRESET_COLORS_ROW1 + PRESET_COLORS_ROW2

@Composable
fun ColorPickerBar(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // 2-row 64-color chromatic spectrum starting from Red to White
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom Color Picker Button Column (Leading controls)
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1: Custom Palette / Color Wheel icon
            Surface(
                color = Color(0xCC1A1B28),
                shape = CircleShape,
                modifier = Modifier
                    .size(31.dp)
                    .clickable { showCustomDialog = true }
                    .testTag("custom_color_picker_btn"),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x66FFFFFF))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = "Colori Personalizzati",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Row 2: RGB Custom Slider shortcut
            Surface(
                color = Color(0x33FFFFFF),
                shape = CircleShape,
                modifier = Modifier
                    .size(31.dp)
                    .clickable { showCustomDialog = true }
                    .testTag("custom_rgb_picker_btn"),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "RGB",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 32 Chromatic Pairs (64 colors total): Top Row (Vivid/Deep) and Bottom Row (Pastel/Complementary)
        CHROMATIC_COLOR_PAIRS_64.forEach { (colorTop, colorBottom) ->
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ColorSwatch(
                    color = colorTop,
                    isSelected = colorTop == selectedColor,
                    onSelect = { onColorSelected(colorTop) }
                )
                ColorSwatch(
                    color = colorBottom,
                    isSelected = colorBottom == selectedColor,
                    onSelect = { onColorSelected(colorBottom) }
                )
            }
        }
    }

    if (showCustomDialog) {
        CustomColorOverlayDialog(
            initialColor = selectedColor,
            onDismiss = { showCustomDialog = false },
            onConfirm = { color ->
                onColorSelected(color)
                showCustomDialog = false
            }
        )
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val isLightColor = color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f > 0.65f

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) (if (isLightColor) Color(0xFF101014) else Color.White) else Color(0x33FFFFFF),
                shape = CircleShape
            )
            .testTag("color_swatch_${color.value}"),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (isLightColor) Color.Black else Color.White,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
fun CustomColorOverlayDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }

    val currentColor = Color(red, green, blue)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF161726),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0x40D0BCFF)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tavolozza Colori Personalizzata",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                // Color Preview Box
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(currentColor)
                        .border(2.dp, Color(0x66FFFFFF), RoundedCornerShape(16.dp))
                )

                // Red Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "R", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                    Slider(
                        value = red,
                        onValueChange = { red = it },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5252), activeTrackColor = Color(0xFFFF5252)),
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "${(red * 255).toInt()}", color = Color.White, modifier = Modifier.width(36.dp), fontSize = 12.sp)
                }

                // Green Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "G", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                    Slider(
                        value = green,
                        onValueChange = { green = it },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF4ADE80), activeTrackColor = Color(0xFF4ADE80)),
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "${(green * 255).toInt()}", color = Color.White, modifier = Modifier.width(36.dp), fontSize = 12.sp)
                }

                // Blue Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "B", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                    Slider(
                        value = blue,
                        onValueChange = { blue = it },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF60A5FA), activeTrackColor = Color(0xFF60A5FA)),
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "${(blue * 255).toInt()}", color = Color.White, modifier = Modifier.width(36.dp), fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annulla", color = Color(0xFFD0BCFF))
                    }

                    Button(
                        onClick = { onConfirm(currentColor) },
                        colors = ButtonDefaults.buttonColors(containerColor = currentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text(
                            text = "Applica Colore",
                            color = if (red + green + blue > 1.8f) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
