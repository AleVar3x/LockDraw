package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WallpaperTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperBottomSheet(
    currentTheme: WallpaperTheme,
    isOverlayVisible: Boolean,
    onSelectTheme: (WallpaperTheme) -> Unit,
    onToggleOverlay: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161726),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .height(4.dp)
                    .fillMaxWidth(0.15f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Sfondo Lockscreen",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "I disegni vengono sovrapposti senza alterare lo sfondo",
                color = Color(0xFFD0BCFF).copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Toggle Lockscreen Clock & Widgets
            Surface(
                color = Color(0x1CFFFFFF),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isOverlayVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Widget Lockscreen",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Mostra Widget Orologio",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Orologio, data e notifiche lockscreen",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isOverlayVisible,
                        onCheckedChange = { onToggleOverlay() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4F378B)
                        ),
                        modifier = Modifier.testTag("toggle_lockscreen_widgets_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Temi Sfondo Disponibili",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Wallpaper Grids
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(240.dp)
            ) {
                items(WallpaperTheme.entries) { theme ->
                    val isSelected = theme == currentTheme
                    val colors = theme.gradientColors.map { Color(it) }

                    Box(
                        modifier = Modifier
                            .height(84.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.verticalGradient(colors))
                            .clickable {
                                onSelectTheme(theme)
                            }
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color(0x33FFFFFF),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(10.dp)
                            .testTag("wallpaper_theme_${theme.name}"),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        if (isSelected) {
                            Surface(
                                color = Color(0xFF4F378B),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0BCFF)),
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selezionato",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = theme.title,
                            color = if (theme.isDark) Color.White else Color(0xFF1E1E24),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
