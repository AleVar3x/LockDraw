package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.data.model.CustomStickerItem
import java.io.File

@Composable
fun CustomStickerCreatorDialog(
    onDismiss: () -> Unit,
    onCreateSticker: (CustomStickerItem) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: WhatsApp / Immagine, 1: Testo & Badge, 2: Disegna a Mano
    var stickerText by remember { mutableStateOf("Amore Mio ❤️") }
    var selectedEmoji by remember { mutableStateOf("💖") }
    var selectedBgColor by remember { mutableStateOf(0xFFFF2A6D.toInt()) }
    var isDarkText by remember { mutableStateOf(false) }

    // State for imported / pasted WhatsApp sticker image
    var importedImagePath by remember { mutableStateOf<String?>(null) }
    var stickerName by remember { mutableStateOf("Sticker WhatsApp") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Doodle Points state for mini-drawing canvas
    val doodleLines = remember { mutableStateListOf<List<Offset>>() }
    var currentDoodleLine by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // File picker launcher for picking WhatsApp stickers or images from gallery/storage
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stickersDir = File(context.filesDir, "stickers").apply { mkdirs() }
                val targetFile = File(stickersDir, "sticker_imported_${System.currentTimeMillis()}.png")
                context.contentResolver.openInputStream(it)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                importedImagePath = targetFile.absolutePath
                statusMessage = "Sticker caricato con successo! 🎉"
            } catch (e: Exception) {
                statusMessage = "Errore durante il caricamento dell'immagine"
            }
        }
    }

    fun pasteFromClipboard() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val item = clip.getItemAt(0)
                val uri = item.uri
                if (uri != null) {
                    val stickersDir = File(context.filesDir, "stickers").apply { mkdirs() }
                    val targetFile = File(stickersDir, "sticker_clipboard_${System.currentTimeMillis()}.png")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    importedImagePath = targetFile.absolutePath
                    statusMessage = "Sticker incollato con successo da WhatsApp / Appunti! ✨"
                    return
                }
                val text = item.text?.toString()
                if (!text.isNullOrBlank()) {
                    stickerText = text
                    if (selectedTab != 1) selectedTab = 1
                    statusMessage = "Testo/Emoji incollato dagli appunti! 💬"
                    return
                }
            }
            statusMessage = "Nessun elemento negli appunti. Copia uno sticker su WhatsApp o usa 'Scegli File'!"
        } catch (e: Exception) {
            statusMessage = "Errore lettura appunti: ${e.localizedMessage}"
        }
    }

    val quickEmojis = listOf("💖", "💕", "💋", "🥺", "🥰", "💌", "🌹", "🧸", "✨", "🔥", "😻", "💍", "🥑", "🌻")
    val bgColors = listOf(
        0xFFFF2A6D.toInt(), // Neon Rose
        0xFFDD2476.toInt(), // Sunset Pink
        0xFF9C27B0.toInt(), // Violet
        0xFF673AB7.toInt(), // Deep Purple
        0xFF3F51B5.toInt(), // Indigo
        0xFF00E5FF.toInt(), // Cyber Cyan
        0xFF00C853.toInt(), // Emerald Green
        0xFFFF9100.toInt(), // Bright Orange
        0xFF1A1C2E.toInt(), // Dark Glass
        0xFFFFFFFF.toInt()  // Pure White Sticker
    )

    // Fullscreen in-compose overlay - 100% crash-proof in both Activity & Service Overlay Window
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent closing when clicking card inside */ }
                )
                .clip(RoundedCornerShape(28.dp))
                .testTag("custom_sticker_creator_dialog"),
            color = Color(0xFF141324),
            border = BorderStroke(1.2.dp, Color(0x38FFFFFF)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0x3325D366),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ContentCut,
                                    contentDescription = null,
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sticker & WhatsApp Maker",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Copia, incolla e disegna per la tela di coppia",
                                color = Color(0xFF9E9DB5),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab selector: WhatsApp / Immagine vs Text vs Doodle
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0x18FFFFFF),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color(0xFF25D366),
                                height = 3.dp
                            )
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("WhatsApp / File", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Testo & Emoji", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Disegno a Mano", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Preview Card with WhatsApp-like Sticker Border and Shadow
                Text(
                    text = "Anteprima Sticker",
                    color = Color(0xFFB0AEC7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF262347), Color(0xFF131220))
                            )
                        )
                        .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedTab == 0) {
                        // WhatsApp Image Sticker Preview
                        if (importedImagePath != null) {
                            Surface(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .size(110.dp)
                                    .padding(4.dp)
                            ) {
                                AsyncImage(
                                    model = importedImagePath,
                                    contentDescription = "Anteprima WhatsApp Sticker",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color(0x88FFFFFF),
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Incolla o seleziona uno sticker WhatsApp",
                                    color = Color(0x99FFFFFF),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else if (selectedTab == 1) {
                        // WhatsApp style text sticker badge preview
                        Surface(
                            color = Color(selectedBgColor),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(2.5.dp, Color.White),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .padding(12.dp)
                                .shadow(6.dp, RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (selectedEmoji.isNotBlank()) {
                                    Text(
                                        text = selectedEmoji,
                                        fontSize = 24.sp
                                    )
                                }
                                Text(
                                    text = stickerText.ifBlank { "Il tuo sticker" },
                                    color = if (selectedBgColor == 0xFFFFFFFF.toInt() || isDarkText) Color(0xFF1E1E1E) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    } else {
                        // Doodle drawing preview with white sticker outline
                        Surface(
                            color = Color(selectedBgColor),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(3.dp, Color.White),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(100.dp)
                                .padding(4.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeCol = if (selectedBgColor == 0xFFFFFFFF.toInt() || isDarkText) Color(0xFF1E1E1E) else Color.White
                                for (line in doodleLines) {
                                    if (line.size > 1) {
                                        val path = Path().apply {
                                            moveTo(line[0].x, line[0].y)
                                            for (pt in line.drop(1)) {
                                                lineTo(pt.x, pt.y)
                                            }
                                        }
                                        drawPath(path, strokeCol, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                    }
                                }
                                if (currentDoodleLine.size > 1) {
                                    val path = Path().apply {
                                        moveTo(currentDoodleLine[0].x, currentDoodleLine[0].y)
                                        for (pt in currentDoodleLine.drop(1)) {
                                            lineTo(pt.x, pt.y)
                                        }
                                    }
                                    drawPath(path, strokeCol, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                }
                            }
                        }
                    }
                }

                // Feedback message
                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = msg,
                        color = Color(0xFF80FFEA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // WhatsApp Sticker Copy & Paste Actions
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { pasteFromClipboard() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("paste_whatsapp_sticker_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Incolla da WhatsApp / Appunti 📋",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFD0BCFF)
                            ),
                            border = BorderStroke(1.2.dp, Color(0xFFD0BCFF)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("pick_sticker_file_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scegli Sticker da Galleria / File 🖼️",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedTextField(
                            value = stickerName,
                            onValueChange = { if (it.length <= 30) stickerName = it },
                            label = { Text("Nome Sticker (opzionale)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0x44FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF100F1C),
                                unfocusedContainerColor = Color(0xFF100F1C)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (selectedTab == 1) {
                    // Text Input
                    OutlinedTextField(
                        value = stickerText,
                        onValueChange = { if (it.length <= 35) stickerText = it },
                        label = { Text("Testo dello Sticker") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0x44FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF100F1C),
                            unfocusedContainerColor = Color(0xFF100F1C)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sticker_text_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Emojis
                    Text(
                        text = "Scegli un'icona / emoji",
                        color = Color(0xFFB0AEC7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickEmojis) { emoji ->
                            val isSelected = selectedEmoji == emoji
                            Surface(
                                color = if (isSelected) Color(0xFF4F378B) else Color(0x1CFFFFFF),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) BorderStroke(1.5.dp, Color(0xFFD0BCFF)) else BorderStroke(1.dp, Color(0x22FFFFFF)),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { selectedEmoji = emoji }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Interactive Mini Doodle Canvas
                    Text(
                        text = "Disegna il tuo simbolo o scarabocchio:",
                        color = Color(0xFFB0AEC7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF12111E))
                            .border(1.2.dp, Color(0x38FFFFFF), RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentDoodleLine = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        currentDoodleLine = currentDoodleLine + change.position
                                    },
                                    onDragEnd = {
                                        if (currentDoodleLine.isNotEmpty()) {
                                            doodleLines.add(currentDoodleLine)
                                            currentDoodleLine = emptyList()
                                        }
                                    },
                                    onDragCancel = {
                                        currentDoodleLine = emptyList()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val lineCol = if (selectedBgColor == 0xFFFFFFFF.toInt() || isDarkText) Color(0xFFD0BCFF) else Color(0xFFFF2A6D)
                            for (line in doodleLines) {
                                if (line.size > 1) {
                                    val path = Path().apply {
                                        moveTo(line[0].x, line[0].y)
                                        for (pt in line.drop(1)) {
                                            lineTo(pt.x, pt.y)
                                        }
                                    }
                                    drawPath(path, lineCol, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                }
                            }
                            if (currentDoodleLine.size > 1) {
                                val path = Path().apply {
                                    moveTo(currentDoodleLine[0].x, currentDoodleLine[0].y)
                                    for (pt in currentDoodleLine.drop(1)) {
                                        lineTo(pt.x, pt.y)
                                    }
                                }
                                drawPath(path, lineCol, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                        }

                        if (doodleLines.isEmpty() && currentDoodleLine.isEmpty()) {
                            Text(
                                text = "Tocca e disegna con il dito qui ✨",
                                color = Color(0x66FFFFFF),
                                fontSize = 13.sp
                            )
                        }

                        // Clear doodle button
                        if (doodleLines.isNotEmpty()) {
                            IconButton(
                                onClick = { doodleLines.clear() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Pulisci",
                                    tint = Color(0xFFFF6E40)
                                )
                            }
                        }
                    }
                }

                if (selectedTab != 0) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Background Color Palette
                    Text(
                        text = "Colore di sfondo dello sticker",
                        color = Color(0xFFB0AEC7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(bgColors) { colorArgb ->
                            val isSelected = selectedBgColor == colorArgb
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorArgb))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color(0x44FFFFFF),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedBgColor = colorArgb },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (colorArgb == 0xFFFFFFFF.toInt()) Color.Black else Color.White)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Create CTA Button
                Button(
                    onClick = {
                        val customSticker = when (selectedTab) {
                            0 -> {
                                CustomStickerItem(
                                    title = stickerName.ifBlank { "Sticker WhatsApp" },
                                    emoji = "💬",
                                    imageUri = importedImagePath,
                                    isDoodle = false
                                )
                            }
                            1 -> {
                                CustomStickerItem(
                                    title = stickerText.ifBlank { "Sticker" },
                                    emoji = selectedEmoji,
                                    backgroundColorArgb = (selectedBgColor.toLong() and 0xFFFFFFFFL),
                                    textColorArgb = if (selectedBgColor == 0xFFFFFFFF.toInt() || isDarkText) 0xFF1E1E1EL else 0xFFFFFFFFL,
                                    isDoodle = false
                                )
                            }
                            else -> {
                                CustomStickerItem(
                                    title = "Disegno ✨",
                                    emoji = "✏️",
                                    backgroundColorArgb = (selectedBgColor.toLong() and 0xFFFFFFFFL),
                                    textColorArgb = if (selectedBgColor == 0xFFFFFFFF.toInt() || isDarkText) 0xFF1E1E1EL else 0xFFFFFFFFL,
                                    isDoodle = true
                                )
                            }
                        }
                        onCreateSticker(customSticker)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("create_sticker_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Salva & Incolla sulla Tela",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
