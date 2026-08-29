package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CustomStickerItem

data class StickerCategory(
    val name: String,
    val items: List<String>,
    val isWhatsApp: Boolean = false
)

val EMOJI_SMILEYS = listOf(
    "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
    "😘", "😗", "😚", "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐", "🤨", "😐",
    "😑", "😶", "😏", "😒", "🙄", "😬", "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢",
    "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "🥸", "😎", "🤓", "🧐", "😕", "😟", "🙁",
    "☹️", "😮", "😯", "😲", "😳", "🥺", "😦", "😧", "😨", "😰", "😥", "😢", "😭", "😱", "😖", "😣",
    "😞", "😓", "😩", "😫", "🥱", "😤", "😡", "😠", "🤬", "😈", "👿", "💀", "☠️", "💩", "🤡", "👹",
    "👺", "👻", "👽", "👾", "🤖"
)

val EMOJI_HEARTS = listOf(
    "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖",
    "💘", "💝", "💟", "💌", "💋", "💍", "💎", "💐", "🌹", "🥀", "🌺", "🌸", "🕊️", "🧸", "🍫", "🍓",
    "❤️‍🔥", "❤️‍🩹", "💑", "💏", "👩‍❤️‍👨", "👨‍❤️‍👨", "👩‍❤️‍👩", "🎀", "🪄", "🫀", "🫂", "✨", "💫", "👑"
)

val EMOJI_GESTURES = listOf(
    "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉",
    "👆", "🖕", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏",
    "✍️", "💅", "🤳", "💪", "🧠", "👀", "👁️", "👅", "👄", "👂", "👃"
)

val EMOJI_ANIMALS = listOf(
    "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔",
    "🐧", "🐦", "🐤", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞",
    "🐜", "🐢", "🐍", "🐙", "🦑", "🦐", "🦀", "🐡", "🐠", "🐟", "🐬", "🐳", "🐋", "🦈", "🐊", "🐅",
    "🐆", "🦓", "🦍", "🦧", "🐘", "🦛", "🦏", "🐪", "🐫", "🦒", "🦘", "🌸", "💮", "🏵️", "🌹", "🥀",
    "🌺", "🌻", "🌼", "🌷", "🌱", "🌲", "🌳", "🌴", "🌵", "🌾", "🌿", "🍀", "🍁", "🍂", "🍃"
)

val EMOJI_FOOD = listOf(
    "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥",
    "🥝", "🍅", "🥑", "🥦", "🥬", "🥒", "🌶️", "🌽", "🥕", "🧄", "🧅", "🥔", "🍠", "🥐", "🥯", "🍞",
    "🥖", "🥨", "🧀", "🥚", "🍳", "🧈", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🌭", "🍔", "🍟", "🍕",
    "🥪", "🥙", "🌮", "🌯", "🥗", "🥘", "🍲", "🍝", "🍜", "🍣", "🍱", "🥟", "🍤", "🍙", "🍚", "🍧",
    "🍨", "🍦", "🥧", "🧁", "🍰", "🎂", "🍮", "🍭", "🍬", "🍫", "🍿", "🍩", "🍪", "☕", "🍵", "🧃",
    "🥤", "🧋", "🍺", "🍻", "🥂", "🍷", "🥃", "🍸", "🍹"
)

val EMOJI_ACTIVITIES = listOf(
    "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱", "🪀", "🏓", "🏸", "🏒", "🥅", "⛳",
    "🪁", "🏹", "🎣", "🤿", "🥊", "🥋", "🛹", "🛼", "🛷", "⛸️", "🎨", "🎬", "🎤", "🎧", "🎼", "🎹",
    "🥁", "🎷", "🎺", "🎸", "🪕", "🎻", "🎲", "🎯", "🎳", "🎮", "🎰", "🧩", "🚗", "🚀", "✈️", "⛵",
    "🛸", "⏰", "📱", "💻", "💡", "🎁", "🎈", "🎉", "🎊", "🪄", "👑", "🏆", "🥇", "🥈", "🥉"
)

val EMOJI_SYMBOLS = listOf(
    "✨", "🌟", "💫", "⭐", "🌠", "⚡", "💥", "🔥", "🌈", "☀️", "🌤️", "⛅", "☁️", "🌧️", "⛈️", "❄️",
    "🌙", "🌛", "💤", "💯", "💢", "💬", "💭", "🗯️", "👁️‍🗨️", "🔱", "⚜️", "🔰", "⭕", "✅", "☑️", "✔️",
    "❌", "✖️", "➕", "➖", "➗", "❓", "❗", "🔴", "🟠", "🟡", "🟢", "🔵", "🟣", "⚫", "⚪", "🟤"
)

val QUICK_MESSAGES = listOf(
    "Ti amo ❤️",
    "Mi manchi 🥺",
    "Buongiorno ☀️",
    "Buonanotte 🌙",
    "XOXO 💋",
    "Bacio 💋",
    "Sei speciale ✨",
    "Pensando a te",
    "Sempre insieme 💍",
    "Amore mio 💖",
    "Sorridi 😊",
    "Sei unico 🌟"
)

val STICKER_CATEGORIES = listOf(
    StickerCategory("💬 Sticker WhatsApp", emptyList(), isWhatsApp = true),
    StickerCategory("😀 Faccine", EMOJI_SMILEYS),
    StickerCategory("💖 Cuori & Amore", EMOJI_HEARTS),
    StickerCategory("✌️ Mani & Gesti", EMOJI_GESTURES),
    StickerCategory("🐶 Animali & Natura", EMOJI_ANIMALS),
    StickerCategory("🍕 Cibo & Dolci", EMOJI_FOOD),
    StickerCategory("⚡ Attività & Oggetti", EMOJI_ACTIVITIES),
    StickerCategory("🌟 Simboli & Meteo", EMOJI_SYMBOLS),
    StickerCategory("💌 Messaggi Rapidi", QUICK_MESSAGES)
)

fun openWhatsAppDirectly(context: Context) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
            ?: context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Impossibile aprire WhatsApp", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun StickerBottomSheet(
    isPremiumUnlocked: Boolean = false,
    customStickers: List<CustomStickerItem> = emptyList(),
    onDismiss: () -> Unit,
    onSelectSticker: (String) -> Unit,
    onDeleteCustomSticker: (String) -> Unit = {},
    onImportStickerUri: ((Uri) -> Unit)? = null,
    onOpenPaywall: () -> Unit = {}
) {
    val context = LocalContext.current
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
                        text = "Sticker & Emoji",
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
                                if (cat.isWhatsApp && !isPremiumUnlocked) {
                                    onOpenPaywall()
                                } else {
                                    selectedCategoryIndex = idx
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cat.name, fontSize = 13.sp)
                                    if (cat.isWhatsApp && !isPremiumUnlocked) {
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
                                selectedContainerColor = if (cat.isWhatsApp) Color(0xFF25D366) else Color(0xFF7C4DFF),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0x18FFFFFF),
                                labelColor = Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val currentCategory = STICKER_CATEGORIES.getOrNull(selectedCategoryIndex) ?: STICKER_CATEGORIES[0]

                if (currentCategory.isWhatsApp) {
                    // --- SEZIONE STICKER WHATSAPP CON PROCEDURA & LINK DIRETTO ---
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            // 1. WhatsApp Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { openWhatsAppDirectly(context) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF25D366),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(44.dp)
                                        .testTag("open_whatsapp_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Apri WhatsApp",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                                type = "image/*"
                                                addCategory(Intent.CATEGORY_OPENABLE)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Scegli uno sticker dalla galleria", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    border = BorderStroke(1.dp, Color(0x6625D366)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80E8A8)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sfoglia file",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        item {
                            // 2. Procedura Guidata Importazione da WhatsApp
                            Surface(
                                color = Color(0x1F25D366),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0x4025D366)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Come importare sticker da WhatsApp:",
                                            color = Color(0xFF80E8A8),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Text(
                                        text = "1️⃣ Tocca 'Apri WhatsApp' sopra e vai in una chat.\n" +
                                               "2️⃣ Tieni premuto sullo sticker e tocca 'Condividi' o 'Invia'.\n" +
                                               "3️⃣ Seleziona LockDraw: lo sticker verrà salvato all'istante qui!",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        if (customStickers.isEmpty()) {
                            item {
                                Surface(
                                    color = Color(0x12FFFFFF),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0x18FFFFFF)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Nessuno sticker WhatsApp salvato ancora",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Condividi qualsiasi sticker da WhatsApp a LockDraw per vederlo apparire qui in tempo reale!",
                                            color = Color(0x99FFFFFF),
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                Text(
                                    text = "I tuoi Sticker WhatsApp salvati (${customStickers.size}):",
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Chunks of 3 stickers per row
                            val stickerChunks = customStickers.chunked(3)
                            items(stickerChunks) { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { sticker ->
                                        Surface(
                                            color = Color(0x28FFFFFF),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.5.dp, Color(0x6625D366)),
                                            shadowElevation = 4.dp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    if (!sticker.imageUri.isNullOrBlank()) {
                                                        onSelectSticker("sticker_img:${sticker.imageUri}")
                                                    } else {
                                                        val label = if (sticker.emoji.isNotBlank()) "${sticker.emoji} ${sticker.title}" else sticker.title
                                                        onSelectSticker(label)
                                                    }
                                                }
                                                .testTag("whatsapp_sticker_${sticker.id}")
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(84.dp)
                                                    .padding(6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!sticker.imageUri.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = sticker.imageUri,
                                                        contentDescription = sticker.title,
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(2.dp)
                                                    )
                                                } else {
                                                    Text(
                                                        text = sticker.title,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { onDeleteCustomSticker(sticker.id) },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(22.dp)
                                                        .background(Color(0xBB000000), CircleShape)
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
                                    }
                                    // Filler spacing if row has fewer than 3 items
                                    if (rowItems.size < 3) {
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (currentCategory.name.contains("Messaggi")) {
                    // Categoria Messaggi Rapidi
                    val currentStickers = currentCategory.items
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        items(currentStickers) { stickerText ->
                            Surface(
                                color = Color(0x22FFFFFF),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSticker(stickerText) }
                                    .testTag("sticker_msg_$stickerText")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
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
                    // Categorie EMOJI con TUTTE LE EMOJI DISPONIBILI
                    val currentEmojis = currentCategory.items
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        items(currentEmojis) { emoji ->
                            Surface(
                                color = Color(0x18FFFFFF),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                modifier = Modifier
                                    .clickable { onSelectSticker(emoji) }
                                    .testTag("sticker_emoji_$emoji")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = 28.sp
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
