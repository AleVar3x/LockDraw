package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.CustomStickerItem

data class StickerCategory(val name: String, val items: List<String>, val isCustom: Boolean = false)

val STICKER_CATEGORIES = listOf(
    StickerCategory("✂️ Personalizzati", emptyList(), isCustom = true),
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
    isPremiumUnlocked: Boolean = false,
    customStickers: List<CustomStickerItem> = emptyList(),
    onDismiss: () -> Unit,
    onSelectSticker: (String) -> Unit,
    onOpenCustomStickerCreator: () -> Unit = {},
    onDeleteCustomSticker: (String) -> Unit = {},
    onOpenPaywall: () -> Unit = {}
) {
    var selectedCategoryIndex by remember { mutableStateOf(if (isPremiumUnlocked) 0 else 1) }

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
            border = BorderStroke(1.2.dp, Color(0x38FFFFFF)),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aggiungi uno Sticker",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (!isPremiumUnlocked) {
                        Surface(
                            color = Color(0x33FFD54F),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x66FFD54F)),
                            modifier = Modifier.clickable { onOpenPaywall() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "VIP 4,99€",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                            onClick = {
                                if (cat.isCustom && !isPremiumUnlocked) {
                                    onOpenPaywall()
                                } else {
                                    selectedCategoryIndex = idx
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cat.name, fontSize = 13.sp)
                                    if (cat.isCustom && !isPremiumUnlocked) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "VIP",
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            },
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

                val currentCategory = STICKER_CATEGORIES[selectedCategoryIndex]

                if (currentCategory.isCustom) {
                    // WhatsApp-style Custom Stickers Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        // Create button
                        Button(
                            onClick = {
                                if (isPremiumUnlocked) {
                                    onOpenCustomStickerCreator()
                                } else {
                                    onOpenPaywall()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("create_new_custom_sticker_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Crea Nuovo Sticker (Stile WhatsApp)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (customStickers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nessuno sticker creato ancora.\nTocca il pulsante verde sopra per iniziare!",
                                    color = Color(0x88FFFFFF),
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(customStickers) { sticker ->
                                    if (!sticker.imageUri.isNullOrBlank()) {
                                        Surface(
                                            color = Color(0x28FFFFFF),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(2.dp, Color.White),
                                            shadowElevation = 4.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onSelectSticker("sticker_img:${sticker.imageUri}")
                                                }
                                                .testTag("custom_sticker_${sticker.id}")
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(80.dp)
                                                    .padding(6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AsyncImage(
                                                    model = sticker.imageUri,
                                                    contentDescription = sticker.title,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(2.dp)
                                                )
                                                IconButton(
                                                    onClick = { onDeleteCustomSticker(sticker.id) },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(22.dp)
                                                        .background(Color(0x99000000), CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Elimina",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Surface(
                                            color = Color(sticker.backgroundColorArgb.toInt()),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(2.dp, Color.White),
                                            shadowElevation = 4.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val stickerLabel = if (sticker.emoji.isNotBlank()) "${sticker.emoji} ${sticker.title}" else sticker.title
                                                    onSelectSticker(stickerLabel)
                                                }
                                                .testTag("custom_sticker_${sticker.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (sticker.emoji.isNotBlank()) {
                                                        Text(sticker.emoji, fontSize = 18.sp)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                    }
                                                    Text(
                                                        text = sticker.title,
                                                        color = Color(sticker.textColorArgb.toInt()),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 2
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { onDeleteCustomSticker(sticker.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Elimina",
                                                        tint = Color.White.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedCategoryIndex == 4) {
                    // Message Category
                    val currentStickers = currentCategory.items
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
                                border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSticker(stickerText) }
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
                    // Emoji Categories
                    val currentStickers = currentCategory.items
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
                                border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                                modifier = Modifier
                                    .clickable { onSelectSticker(emoji) }
                                    .testTag("sticker_emoji_$emoji")
                            ) {
                                Box(
                                    modifier = Modifier.padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 34.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
