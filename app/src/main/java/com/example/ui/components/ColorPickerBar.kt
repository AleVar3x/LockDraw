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

val PRESET_COLORS = listOf(
    Color(0xFFFF0055), // Hot Neon Pink
    Color(0xFF00F5FF), // Vivid Neon Cyan
    Color(0xFF00FF66), // Laser Lime Green
    Color(0xFFFFEA00), // Electric Neon Yellow
    Color(0xFFFF6D00), // Radiant Orange
    Color(0xFFD500F9), // Hyper Magenta
    Color(0xFF00E5FF), // Bright Celeste
    Color(0xFF7C4DFF), // Electric Violet
    Color(0xFFFF1744), // Fluorescent Red
    Color(0xFF00E676), // Mint Emerald
    Color(0xFFFFFFFF), // Pure Brilliant White
    Color(0xFF101014)  // Pitch Black
)

@Composable
fun ColorPickerBar(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PRESET_COLORS.forEach { color ->
            val isSelected = color == selectedColor
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onColorSelected(color) }
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) Color.White else Color(0x33FFFFFF),
                        shape = CircleShape
                    )
                    .testTag("color_swatch_${color.value}"),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (color == Color.White || color == Color(0xFFFFEA00) || color == Color(0xFF00F5FF)) Color.Black else Color.White, CircleShape)
                    )
                }
            }
        }

        // Custom color picker button (Glass style)
        Surface(
            color = Color(0xCC1A1B28),
            shape = CircleShape,
            modifier = Modifier
                .size(34.dp)
                .clickable { showCustomDialog = true }
                .testTag("custom_color_picker_btn"),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x66FFFFFF))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = "Colori Personalizzati",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
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
