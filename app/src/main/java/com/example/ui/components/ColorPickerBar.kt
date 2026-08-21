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
    Color(0xFFD0BCFF), // Frosted Lavender
    Color(0xFFFFB4AB), // Frosted Coral
    Color(0xFFC2E7FF), // Frosted Sky
    Color(0xFFB4E495), // Frosted Mint
    Color(0xFFFEE285), // Frosted Butter
    Color(0xFFFFFFFF), // Pure White
    Color(0xFFFF2A6D), // Neon Pink
    Color(0xFFFF9100), // Bright Amber
    Color(0xFF00E5FF), // Cyan Glow
    Color(0xFF7C4DFF), // Violet
    Color(0xFF4F378B), // Deep Plum
    Color(0xFF1E1E24)  // Dark Ink
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
                            .background(if (color == Color.White || color == Color(0xFFFEE285) || color == Color(0xFFC2E7FF)) Color.Black else Color.White, CircleShape)
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
        CustomColorDialog(
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
fun CustomColorDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }

    val currentColor = Color(red, green, blue)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161726),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.85f),
        title = {
            Text(
                text = "Tavolozza Colori Personalizzata",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Color Preview Box
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(currentColor)
                        .border(2.dp, Color(0x66FFFFFF), RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

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
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentColor) },
                colors = ButtonDefaults.buttonColors(containerColor = currentColor)
            ) {
                Text(
                    text = "Applica Colore",
                    color = if (red + green + blue > 1.8f) Color.Black else Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = Color(0xFFD0BCFF))
            }
        }
    )
}
