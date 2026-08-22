package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

data class StickerCategory(val name: String, val items: List<String>)

val STICKER_CATEGORIES = listOf(
    StickerCategory(
        "Cuori & Amore",
        listOf("💖", "💕", "💌", "🌹", "💍", "🧸", "🍓", "🍫", "💋", "🕊️", "💐", "🎀", "💘", "💞", "❤️‍🔥", "💑")
    ),
    StickerCategory(
        "Effetti & Magia",
        listOf("✨", "🌟", "🌈", "💫", "🌙", "☀️", "🎨", "⚡", "🔥", "🦋", "🧁", "🍩", "🪄", "🍀", "🌸", "🌺")
    ),
    StickerCategory(
        "Espressioni Cute",
        listOf("🥰", "🥺", "😻", "😚", "🥑", "🐱", "🐶", "🍕", "🍦", "🍭", "🐼", "🐻", "🐰", "🐣", "🐸", "🌻")
    ),
    StickerCategory(
        "Messaggi",
        listOf(
            "Ti amo ❤️",
            "Mi manchi 🥺",
            "Buongiorno ☀️",
            "Buonanotte 🌙",
            "XOXO 💋",
            "Bacio 💋",
            "Sei speciale ✨",
            "Pensando a te",
            "Sempre insieme 💍",
            "Amore mio 💖"
        )
    )
)

@Composable
fun StickerBottomSheet(
    onDismiss: () -> Unit,
    onSelectSticker: (String) -> Unit
) {
    var selectedCategoryIndex by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = Color(0xF0141524),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x38FFFFFF)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Drag handle bar
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                        .height(4.dp)
                        .fillMaxWidth(0.12f)
                )

                Text(
                    text = "Aggiungi uno Sticker",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Category Selector Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(STICKER_CATEGORIES.indices.toList()) { idx ->
                        val cat = STICKER_CATEGORIES[idx]
                        val isSelected = idx == selectedCategoryIndex
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryIndex = idx },
                            label = { Text(cat.name, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4F378B),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0x18FFFFFF),
                                labelColor = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sticker Grid
                val currentStickers = STICKER_CATEGORIES[selectedCategoryIndex].items
                val isMessageCategory = selectedCategoryIndex == 3

                if (isMessageCategory) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(260.dp)
                    ) {
                        items(currentStickers) { stickerText ->
                            Surface(
                                color = Color(0x22FFFFFF),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSticker(stickerText)
                                    }
                                    .testTag("sticker_chip_$stickerText")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stickerText,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(260.dp)
                    ) {
                        items(currentStickers) { emoji ->
                            Surface(
                                color = Color(0x1CFFFFFF),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                                modifier = Modifier
                                    .clickable {
                                        onSelectSticker(emoji)
                                    }
                                    .testTag("sticker_emoji_$emoji")
                            ) {
                                Box(
                                    modifier = Modifier.padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = 34.sp
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
