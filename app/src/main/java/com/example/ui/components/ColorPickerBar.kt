package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 32 chromatic color pairs (64 colors total)
val CHROMATIC_COLOR_PAIRS_64: List<Pair<Color, Color>> = listOf(
    // 1. Reds & Corals
    Pair(Color(0xFFD50000), Color(0xFFFFCDD2)),
    Pair(Color(0xFFFF1744), Color(0xFFFF8A80)),
    Pair(Color(0xFFFF3D00), Color(0xFFFFAB91)),
    Pair(Color(0xFFFF6D00), Color(0xFFFFCCBC)),
    // 2. Oranges & Warm Golds
    Pair(Color(0xFFFF9100), Color(0xFFFFE0B2)),
    Pair(Color(0xFFFFC107), Color(0xFFFFE082)),
    Pair(Color(0xFFFFD600), Color(0xFFFFF59D)),
    Pair(Color(0xFFFFEA00), Color(0xFFFFF9C4)),
    // 3. Yellows & Limes
    Pair(Color(0xFFFFFF00), Color(0xFFFFFF8D)),
    Pair(Color(0xFFAEEA00), Color(0xFFF4FF81)),
    Pair(Color(0xFF76FF03), Color(0xFFCCFF90)),
    Pair(Color(0xFF64DD17), Color(0xFFDCEDC8)),
    // 4. Greens & Emeralds
    Pair(Color(0xFF00E676), Color(0xFFB9F6CA)),
    Pair(Color(0xFF00C853), Color(0xFFC8E6C9)),
    Pair(Color(0xFF2E7D32), Color(0xFF69F0AE)),
    Pair(Color(0xFF1B5E20), Color(0xFFA7FFEB)),
    // 5. Teals & Cyans
    Pair(Color(0xFF00897B), Color(0xFF80CBC4)),
    Pair(Color(0xFF00BFA5), Color(0xFFE0F7FA)),
    Pair(Color(0xFF1DE9B6), Color(0xFF84FFFF)),
    Pair(Color(0xFF00E5FF), Color(0xFFB2EBF2)),
    // 6. Blues & Indigos
    Pair(Color(0xFF00B0FF), Color(0xFF80D8FF)),
    Pair(Color(0xFF2979FF), Color(0xFF90CAF9)),
    Pair(Color(0xFF448AFF), Color(0xFFBBDEFB)),
    Pair(Color(0xFF3D5AFE), Color(0xFF82B1FF)),
    // 7. Violets, Purples & Magentas
    Pair(Color(0xFF651FFF), Color(0xFFB388FF)),
    Pair(Color(0xFF7C4DFF), Color(0xFFD1C4E9)),
    Pair(Color(0xFFAA00FF), Color(0xFFEA80FC)),
    Pair(Color(0xFFD500F9), Color(0xFFE1BEE7)),
    // 8. Pinks, Earthy & Monochromes
    Pair(Color(0xFFFF007F), Color(0xFFFF80AB)),
    Pair(Color(0xFFC2185B), Color(0xFFF8BBD0)),
    Pair(Color(0xFF5D4037), Color(0xFFD7CCC8)),
    Pair(Color(0xFF101014), Color(0xFFFFFFFF))
)

val PRESET_COLORS_ROW1: List<Color> = CHROMATIC_COLOR_PAIRS_64.map { it.first }
val PRESET_COLORS_ROW2: List<Color> = CHROMATIC_COLOR_PAIRS_64.map { it.second }
val ALL_PRESET_COLORS: List<Color> = PRESET_COLORS_ROW1 + PRESET_COLORS_ROW2

val DEFAULT_CUSTOM_PALETTE_COLORS = listOf(
    Color(0xFFFF2A6D), // Neon Pink
    Color(0xFFFF1744), // Crimson Red
    Color(0xFFFF6D00), // Radiant Orange
    Color(0xFFFFD600), // Golden Yellow
    Color(0xFF00E676), // Vivid Emerald Green
    Color(0xFF00E5FF), // Vivid Cyan
    Color(0xFF2979FF), // Electric Blue
    Color(0xFF7C4DFF), // Electric Violet
    Color(0xFFD500F9), // Hyper Magenta
    Color(0xFFFFFFFF), // Pure White
    Color(0xFF101014)  // Pitch Black
)

fun parseHexColor(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    return try {
        when (clean.length) {
            6 -> {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                Color(r, g, b)
            }
            8 -> {
                val a = clean.substring(0, 2).toInt(16)
                val r = clean.substring(2, 4).toInt(16)
                val g = clean.substring(4, 6).toInt(16)
                val b = clean.substring(6, 8).toInt(16)
                Color(r, g, b, a)
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

/**
 * Customizable Color Palette Bar with Quick Access Favorites,
 * Add Color (+), Long-press quick actions, Custom RGB/Hex Color Manager,
 * and Expandable Full Chromatic Spectrum.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColorPickerBar(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    customColors: List<Color> = DEFAULT_CUSTOM_PALETTE_COLORS,
    onAddCustomColor: ((Color) -> Unit)? = null,
    onRemoveCustomColor: ((Int) -> Unit)? = null,
    onUpdateCustomColor: ((Int, Color) -> Unit)? = null,
    onResetCustomPalette: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    var showFullSpectrum by remember { mutableStateOf(false) }
    var selectedColorForEditIndex by remember { mutableIntStateOf(-1) }
    var showSlotActionDialog by remember { mutableStateOf(false) }
    var clickedSlotIndex by remember { mutableIntStateOf(-1) }

    val scrollState = rememberScrollState()
    val spectrumScrollState = rememberScrollState()

    val effectiveCustomColors = if (customColors.isEmpty()) DEFAULT_CUSTOM_PALETTE_COLORS else customColors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expandable Full 64-Color Chromatic Spectrum
        AnimatedVisibility(
            visible = showFullSpectrum,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            Surface(
                color = Color(0xF2121324),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0x38FFFFFF)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Spettro Completo (64 Colori)",
                            color = Color(0xFFD0BCFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = Color(0x33FFFFFF),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { showFullSpectrum = false }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Chiudi Spettro",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // 2-row chromatic scrollable grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(spectrumScrollState)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CHROMATIC_COLOR_PAIRS_64.forEach { (colorTop, colorBottom) ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ColorSwatch(
                                    color = colorTop,
                                    isSelected = colorTop == selectedColor,
                                    onSelect = {
                                        onColorSelected(colorTop)
                                    }
                                )
                                ColorSwatch(
                                    color = colorBottom,
                                    isSelected = colorBottom == selectedColor,
                                    onSelect = {
                                        onColorSelected(colorBottom)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Customizable Quick Access Color Palette Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Action: Crea Colore Personalizzato (Recycled '+' Button)
            Surface(
                color = Color(0x3300E5FF),
                shape = CircleShape,
                modifier = Modifier
                    .size(34.dp)
                    .clickable {
                        selectedColorForEditIndex = -1
                        showCustomDialog = true
                    }
                    .testTag("open_color_manager_btn"),
                border = BorderStroke(1.2.dp, Color(0x8000E5FF)),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Crea Colore Personalizzato",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2. Action: Toggle Full 64-color Spectrum (Palette icon)
            Surface(
                color = if (showFullSpectrum) Color(0xFF5E35B1) else Color(0x2826283C),
                shape = CircleShape,
                modifier = Modifier
                    .size(34.dp)
                    .clickable { showFullSpectrum = !showFullSpectrum }
                    .testTag("toggle_full_spectrum_btn"),
                border = BorderStroke(1.2.dp, if (showFullSpectrum) Color(0xFFD0BCFF) else Color(0x44FFFFFF)),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Tutti i 64 Colori",
                        tint = if (showFullSpectrum) Color.White else Color(0xFFFFD54F),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Vertical divider separating toolbar actions from custom swatches
            Box(
                modifier = Modifier
                    .height(26.dp)
                    .width(1.dp)
                    .background(Color(0x33FFFFFF))
            )

            // 4. Quick Customizable Color Swatches
            effectiveCustomColors.forEachIndexed { index, color ->
                val isSelected = color.toArgb() == selectedColor.toArgb()

                CustomPaletteSwatch(
                    color = color,
                    isSelected = isSelected,
                    onSelect = { onColorSelected(color) },
                    onLongClick = {
                        clickedSlotIndex = index
                        showSlotActionDialog = true
                    }
                )
            }
        }
    }

    // Quick Slot Action Dialog (on long-press swatch)
    if (showSlotActionDialog && clickedSlotIndex in effectiveCustomColors.indices) {
        val slotColor = effectiveCustomColors[clickedSlotIndex]

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable { showSlotActionDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color(0xFF1B1C2E),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, Color(0x40D0BCFF)),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(slotColor)
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                        Column {
                            Text(
                                text = "Personalizza Colore",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = colorToHex(slotColor),
                                color = Color(0xFFB0AEC7),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Button: Sostituisci con colore attivo
                    Button(
                        onClick = {
                            onUpdateCustomColor?.invoke(clickedSlotIndex, selectedColor)
                            showSlotActionDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F378B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sostituisci con Colore Attuale", fontSize = 13.sp)
                    }

                    // Button: Modifica RGB / Hex
                    Button(
                        onClick = {
                            selectedColorForEditIndex = clickedSlotIndex
                            showSlotActionDialog = false
                            showCustomDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282944)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Modifica Valori RGB / Hex", fontSize = 13.sp)
                    }

                    // Button: Elimina da tavolozza
                    if (effectiveCustomColors.size > 1 && onRemoveCustomColor != null) {
                        Button(
                            onClick = {
                                onRemoveCustomColor(clickedSlotIndex)
                                showSlotActionDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF5252), contentColor = Color(0xFFFF8A80)),
                            border = BorderStroke(1.dp, Color(0x66FF5252)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rimuovi dalla Tavolozza", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    TextButton(onClick = { showSlotActionDialog = false }) {
                        Text("Chiudi", color = Color(0xFFB0AEC7))
                    }
                }
            }
        }
    }

    // Full Custom Color & Palette Customizer Dialog
    if (showCustomDialog) {
        val initialEditColor = if (selectedColorForEditIndex in effectiveCustomColors.indices) {
            effectiveCustomColors[selectedColorForEditIndex]
        } else {
            selectedColor
        }

        CustomColorOverlayDialog(
            initialColor = initialEditColor,
            isEditingSlot = selectedColorForEditIndex >= 0,
            onDismiss = { showCustomDialog = false },
            onConfirm = { color ->
                if (selectedColorForEditIndex in effectiveCustomColors.indices) {
                    onUpdateCustomColor?.invoke(selectedColorForEditIndex, color)
                }
                onColorSelected(color)
                showCustomDialog = false
            },
            onAddToPalette = { color ->
                onAddCustomColor?.invoke(color)
                onColorSelected(color)
                showCustomDialog = false
            },
            onResetPalette = onResetCustomPalette
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomPaletteSwatch(
    color: Color,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onLongClick: () -> Unit
) {
    val isLightColor = color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f > 0.65f

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onLongClick
            )
            .border(
                width = if (isSelected) 2.8.dp else 1.2.dp,
                color = if (isSelected) (if (isLightColor) Color(0xFF101014) else Color.White) else Color(0x38FFFFFF),
                shape = CircleShape
            )
            .testTag("custom_color_swatch_${color.toArgb()}"),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(
                        if (isLightColor) Color.Black else Color.White,
                        CircleShape
                    )
            )
        }
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
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) (if (isLightColor) Color(0xFF101014) else Color.White) else Color(0x33FFFFFF),
                shape = CircleShape
            )
            .testTag("color_swatch_${color.toArgb()}"),
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
    isEditingSlot: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
    onAddToPalette: ((Color) -> Unit)? = null,
    onResetPalette: (() -> Unit)? = null
) {
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }
    var hexText by remember(initialColor) { mutableStateOf(colorToHex(initialColor)) }
    var hexError by remember { mutableStateOf(false) }

    val currentColor = Color(red, green, blue)

    fun updateFromRgb(r: Float, g: Float, b: Float) {
        red = r
        green = g
        blue = b
        hexText = colorToHex(Color(r, g, b))
        hexError = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF161726),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, Color(0x40D0BCFF)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditingSlot) "Modifica Colore Tavolozza" else "Crea Colore Personalizzato",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )

                    if (onResetPalette != null) {
                        Surface(
                            color = Color(0x28FFFFFF),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable {
                                onResetPalette()
                                onDismiss()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Ripristina",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Ripristina",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFD54F)
                                )
                            }
                        }
                    }
                }

                // Color Preview Box and Hex Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(currentColor)
                            .border(2.dp, Color(0x80FFFFFF), RoundedCornerShape(16.dp))
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = hexText,
                            onValueChange = { input ->
                                hexText = input
                                val parsed = parseHexColor(input)
                                if (parsed != null) {
                                    red = parsed.red
                                    green = parsed.green
                                    blue = parsed.blue
                                    hexError = false
                                } else {
                                    hexError = input.isNotBlank() && !input.startsWith("#")
                                }
                            },
                            label = { Text("Codice HEX", fontSize = 11.sp) },
                            singleLine = true,
                            isError = hexError,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0x44FFFFFF)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Red Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "R", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                    Slider(
                        value = red,
                        onValueChange = { updateFromRgb(it, green, blue) },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5252), activeTrackColor = Color(0xFFFF5252)),
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "${(red * 255).toInt()}", color = Color.White, modifier = Modifier.width(32.dp), fontSize = 12.sp, textAlign = TextAlign.End)
                }

                // Green Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "G", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                    Slider(
                        value = green,
                        onValueChange = { updateFromRgb(red, it, blue) },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF4ADE80), activeTrackColor = Color(0xFF4ADE80)),
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "${(green * 255).toInt()}", color = Color.White, modifier = Modifier.width(32.dp), fontSize = 12.sp, textAlign = TextAlign.End)
                }

                // Blue Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "B", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                    Slider(
                        value = blue,
                        onValueChange = { updateFromRgb(red, green, it) },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF60A5FA), activeTrackColor = Color(0xFF60A5FA)),
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "${(blue * 255).toInt()}", color = Color.White, modifier = Modifier.width(32.dp), fontSize = 12.sp, textAlign = TextAlign.End)
                }

                // Quick Palette Presets grid
                Text(
                    text = "Oppure scegli un preset:",
                    fontSize = 11.sp,
                    color = Color(0xFFB0AEC7),
                    modifier = Modifier.align(Alignment.Start)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ALL_PRESET_COLORS.take(16).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(preset)
                                .clickable { updateFromRgb(preset.red, preset.green, preset.blue) }
                                .border(1.dp, Color(0x44FFFFFF), CircleShape)
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text("Annulla", color = Color(0xFFD0BCFF))
                    }

                    if (onAddToPalette != null && !isEditingSlot) {
                        Button(
                            onClick = { onAddToPalette(currentColor) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF382B57)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Salva in Tavolozza", fontSize = 12.sp, color = Color(0xFFD0BCFF))
                        }
                    }

                    Button(
                        onClick = { onConfirm(currentColor) },
                        colors = ButtonDefaults.buttonColors(containerColor = currentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        val isLight = red * 0.299f + green * 0.587f + blue * 0.114f > 0.65f
                        Text(
                            text = if (isEditingSlot) "Salva Modifica" else "Applica",
                            color = if (isLight) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
