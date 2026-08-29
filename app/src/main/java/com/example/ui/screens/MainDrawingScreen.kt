package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrushType
import com.example.data.model.WallpaperTheme
import com.example.data.sync.ConnectionStatus
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.ui.components.PaywallDialog
import com.example.ui.components.DrawingCanvas
import com.example.ui.components.StickerBottomSheet
import com.example.ui.components.openWhatsAppDirectly
import com.example.util.QrCodeView
import com.example.viewmodel.DrawingViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun MainDrawingScreen(
    viewModel: DrawingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val roomCode by viewModel.roomCode.collectAsState()
    val isMatched by viewModel.isMatched.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val lastSyncError by viewModel.lastSyncError.collectAsState()
    val partnerPresence by viewModel.partnerPresence.collectAsState()
    val myName by viewModel.myName.collectAsState()
    val partnerCustomName by viewModel.partnerCustomName.collectAsState()
    val strokes by viewModel.strokes.collectAsState()
    val currentDraftStroke by viewModel.currentDraftStroke.collectAsState()
    val partnerDraftStroke by viewModel.partnerDraftStroke.collectAsState()
    val placedStickers by viewModel.placedStickers.collectAsState()
    val selectedStickerId by viewModel.selectedStickerId.collectAsState()
    val floatingReactions by viewModel.floatingReactions.collectAsState()
    val isFloatingActive by viewModel.isFloatingServiceActive.collectAsState()
    val lockscreenConfig by viewModel.lockscreenConfig.collectAsState()

    val isPremiumUnlocked by viewModel.isPremiumUnlocked.collectAsState()
    val partnerNotificationsEnabled by viewModel.partnerNotificationsEnabled.collectAsState()
    val customStickers by viewModel.customStickers.collectAsState()

    val selectedBrushType by viewModel.selectedBrushType.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val strokeWidth by viewModel.strokeWidth.collectAsState()

    var partnerCodeInput by remember { mutableStateOf("") }
    var myNameInput by remember(myName) { mutableStateOf(myName) }
    var partnerNameInput by remember(partnerCustomName) { mutableStateOf(partnerCustomName) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }
    var showPaywallDialog by remember { mutableStateOf(false) }
    var showStickersSheet by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(viewModel.canDrawOverlays(context)) }

    val drawingColors = remember {
        listOf(
            Color(0xFFFF2A6D), // Neon Pink
            Color(0xFF05D9E8), // Neon Cyan
            Color(0xFFFFD700), // Yellow Gold
            Color(0xFF00FF66), // Neon Green
            Color(0xFFB15EFF), // Neon Purple
            Color(0xFFFF8C00), // Orange
            Color(0xFFFFFFFF), // White
            Color(0xFF1F1F1F)  // Dark
        )
    }

    val quickStickers = remember {
        listOf("💖", "💌", "✨", "🧸", "💋", "🐱", "🌹", "🔥", "🎀", "⭐")
    }

    // Launcher for overlay permission settings
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = viewModel.canDrawOverlays(context)
        hasOverlayPermission = granted
        if (granted && !isFloatingActive) {
            viewModel.startFloatingService(context)
        }
    }

    // Automatically start floating bubble service on launch
    LaunchedEffect(Unit) {
        if (viewModel.canDrawOverlays(context)) {
            hasOverlayPermission = true
            if (!isFloatingActive) {
                viewModel.startFloatingService(context)
            }
        } else {
            hasOverlayPermission = false
        }
    }

    // Disconnect Confirmation Dialog
    if (showDisconnectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmDialog = false },
            title = {
                Text(
                    text = "Scollegare dal Partner?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Scollegandoti dalla stanza $roomCode, non riceverai più i disegni in tempo reale finché non effettui nuovamente il match.",
                    color = Color(0xFFD0BCFF)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unmatchPartner()
                        showDisconnectConfirmDialog = false
                        Toast.makeText(context, "Scollegato dalla stanza", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Scollega Stanza", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmDialog = false }) {
                    Text("Annulla", color = Color(0xFFB0AEC7))
                }
            },
            containerColor = Color(0xFF1E1D30)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0E17),
                        Color(0xFF141324),
                        Color(0xFF1C1936)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. App Header & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LockDraw",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (isMatched) Color(0xFF2E7D32) else Color(0xFF4F378B),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isMatched) "MATCHED" else "LIVE SYNC",
                                color = if (isMatched) Color(0xFFA5D6A7) else Color(0xFFD0BCFF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "Lavagna condivisa su schermo per coppie",
                        color = Color(0xFFB0AEC7),
                        fontSize = 13.sp
                    )
                }

                // Quick Floating Bubble status badge
                Surface(
                    color = if (isFloatingActive) Color(0xFF2E7D32).copy(alpha = 0.25f) else Color(0xFFE53935).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isFloatingActive) Color(0xFF4ADE80) else Color(0xFFFF8A80)
                    ),
                    modifier = Modifier.clickable {
                        if (hasOverlayPermission) {
                            viewModel.toggleFloatingService(context)
                        } else {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            overlayPermissionLauncher.launch(intent)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isFloatingActive) Color(0xFF4ADE80) else Color(0xFFFF8A80),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isFloatingActive) "Bolla Attiva" else "Bolla Off",
                            color = if (isFloatingActive) Color(0xFF4ADE80) else Color(0xFFFF8A80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Permission Banner if overlay is missing
            if (!hasOverlayPermission) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xD9381E24)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.2.dp, Color(0xFFFF5252)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = null,
                                tint = Color(0xFFFF8A80)
                            )
                            Text(
                                text = "Permesso Sovrapposizione Schermo",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "Per disegnare direttamente sopra il lockscreen e qualsiasi app, abilita il permesso di sovrapposizione.",
                            color = Color(0xFFE2D0D3),
                            fontSize = 13.sp
                        )
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                overlayPermissionLauncher.launch(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Abilita Permesso Disegno Fluttuante", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Sync Status & Error Card (if any sync error or offline state occurs)
            if (lastSyncError != null || connectionStatus == ConnectionStatus.OFFLINE) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xD92E1A1A)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF8A80)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stato Sincronizzazione Firebase",
                                color = Color(0xFFFF8A80),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = lastSyncError ?: "Disconnesso da Firebase. Verifica la connessione di rete.",
                                color = Color(0xFFF3E5E8),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.reconnectSync()
                                Toast.makeText(context, "Tentativo di riconnessione a Firebase...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Riprova", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. MATCH STATUS & ROOM CARD
            if (isMatched) {
                // ACTIVE MATCH CARD (Lock state - cannot generate new codes unless unlinked)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xD9142820)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF4ADE80)),
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFF1B5E20),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF81C784),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Dispositivi Abbinati",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Stanza salvata & attiva permanentemente",
                                        color = Color(0xFFA5D6A7),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // QR toggle
                            IconButton(
                                onClick = { showQrDialog = !showQrDialog },
                                modifier = Modifier
                                    .background(Color(0x20FFFFFF), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "Mostra QR",
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Room code banner
                        Surface(
                            color = Color(0xFF0C1914),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0x664ADE80)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "STANZA ABBINATA",
                                        color = Color(0xFF81C784),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = roomCode,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = Color(0x28FFFFFF),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("LockDraw Code", roomCode))
                                                Toast.makeText(context, "Codice $roomCode copiato!", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copia codice",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // QR Code Card View (Collapsible)
                        AnimatedVisibility(visible = showQrDialog) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0C1914), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Scansiona con l'altro smartphone",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box(
                                    modifier = Modifier.size(170.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    QrCodeView(
                                        data = roomCode,
                                        modifier = Modifier.size(160.dp),
                                        backgroundColor = Color.White,
                                        codeColor = Color(0xFF0C1914)
                                    )
                                }
                                Text(
                                    text = roomCode,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // Disconnect Partner Button (Only way to unlink and generate a new code)
                        Button(
                            onClick = { showDisconnectConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x33FF5252),
                                contentColor = Color(0xFFFF8A80)
                            ),
                            border = BorderStroke(1.dp, Color(0x66FF5252)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("disconnect_partner_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scollega Partner dalla Stanza",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                // NOT MATCHED YET: Provide Code + Connect with Partner options
                // 3a. CARD: Il Tuo Codice di Match (Fornisci questo codice al Partner)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xD9181729)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.2.dp, Color(0x38FFFFFF)),
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFF382B57),
                                    shape = CircleShape,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = Color(0xFFFF80AB),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Il Tuo Codice di Match",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Fornisci questo codice all'altro smartphone",
                                        color = Color(0xFF9E9DB5),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // QR Code toggle button
                            IconButton(
                                onClick = { showQrDialog = !showQrDialog },
                                modifier = Modifier
                                    .background(Color(0x20FFFFFF), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "Mostra QR Code",
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Big Room Code Display Box
                        Surface(
                            color = Color(0xFF100F1C),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color(0x4D8E7CFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "CODICE STANZA",
                                        color = Color(0xFF8E7CFF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = roomCode,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Copy Code Button
                                    Surface(
                                        color = Color(0x28FFFFFF),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("LockDraw Code", roomCode))
                                                Toast.makeText(context, "Codice $roomCode copiato negli appunti!", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copia codice",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Share Code Button
                                    Surface(
                                        color = Color(0xFF4F378B),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable {
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, "Disegna con me su LockDraw! Unisciti con il codice: $roomCode")
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Condividi codice LockDraw"))
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Condividi codice",
                                                tint = Color(0xFFD0BCFF),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // QR Code Card View (Collapsible)
                        AnimatedVisibility(visible = showQrDialog) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF100F1C), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Scansiona con l'altro smartphone",
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box(
                                    modifier = Modifier.size(170.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    QrCodeView(
                                        data = roomCode,
                                        modifier = Modifier.size(160.dp),
                                        backgroundColor = Color.White,
                                        codeColor = Color(0xFF141324)
                                    )
                                }
                                Text(
                                    text = roomCode,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // Generate New Random 16-character Code Button (Only available when NOT matched)
                        OutlinedButton(
                            onClick = {
                                viewModel.generateNewRandomRoomCode()
                                Toast.makeText(context, "Nuovo codice 16 caratteri generato!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF)),
                            border = BorderStroke(1.dp, Color(0x4D8E7CFF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Genera Nuovo Codice Stanza (16 car.)", fontSize = 13.sp)
                        }
                    }
                }

                // 3b. CARD: Collega con Smartphone Partner (Immetti il Codice)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xD9181729)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.2.dp, Color(0x38FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF1E3A5F),
                                shape = CircleShape,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = Color(0xFF82B1FF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Collega al Partner",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Inserisci il codice di 16 caratteri del partner",
                                    color = Color(0xFF9E9DB5),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Input Text Field
                        OutlinedTextField(
                            value = partnerCodeInput,
                            onValueChange = { partnerCodeInput = it.uppercase() },
                            placeholder = { Text("Es. 8K2M9PX4Y7Q1L3R5", color = Color(0x66FFFFFF)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    if (partnerCodeInput.isNotBlank()) {
                                        viewModel.connectToRoomCode(partnerCodeInput)
                                        partnerCodeInput = ""
                                        Toast.makeText(context, "Match effettuato!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ),
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
                                .testTag("partner_code_input")
                        )

                        // Connect CTA Button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                if (partnerCodeInput.isNotBlank()) {
                                    val connectedTo = partnerCodeInput
                                    viewModel.connectToRoomCode(partnerCodeInput)
                                    partnerCodeInput = ""
                                    Toast.makeText(context, "Match effettuato con successo a $connectedTo!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Inserisci un codice valido", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6750A4),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("connect_partner_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fai il Match & Unisciti alla Lavagna",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 4. IMPOSTAZIONE NOME OPERATORE & IDENTITÀ PARTNER
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xD9161726)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.2.dp, Color(0x28FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF4A148C),
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = Color(0xFFE1BEE7),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Nome Operatore & Partner",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Ogni partner imposta il proprio nome, sincronizzato via Firebase",
                                color = Color(0xFF9E9DB5),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Il tuo nome operatore
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Il tuo nome (visibile al partner)",
                            color = Color(0xFFD0BCFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        OutlinedTextField(
                            value = myNameInput,
                            onValueChange = {
                                myNameInput = it
                                viewModel.setMyName(it)
                            },
                            placeholder = { Text("Es. Giulia / Marco", color = Color(0x55FFFFFF)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF100F1C),
                                unfocusedContainerColor = Color(0xFF100F1C)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("my_name_input")
                        )
                    }

                    // Scheda Partner (Legge il nome impostato dal partner su Firebase)
                    Surface(
                        color = Color(0xFF1C1A2E),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x3380DEEA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Nome del Partner:",
                                    color = Color(0xFF80DEEA),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Surface(
                                    color = if (partnerPresence.isOnline) Color(0x2200E676) else Color(0x229E9E9E),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (partnerPresence.isOnline) Color(0xFF00E676) else Color(0x449E9E9E))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (partnerPresence.isOnline) Color(0xFF00E676) else Color(0xFF9E9E9E))
                                        )
                                        Text(
                                            text = if (partnerPresence.isOnline) "Online" else "Non in linea",
                                            color = if (partnerPresence.isOnline) Color(0xFF00E676) else Color(0xFF9E9E9E),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color(0x3300E5FF),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "👤 ${partnerPresence.partnerName.ifBlank { "In attesa che il partner imposti il nome..." }}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Il nome del partner viene impostato direttamente dal suo smartphone e sincronizzato in automatico su Firebase.",
                                color = Color(0xFF9E9DB5),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // 5. IMPOSTAZIONI VIP & NOTIFICHE PARTNER
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremiumUnlocked) Color(0xD91E1B38) else Color(0xD92A1D28)
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    1.2.dp,
                    if (isPremiumUnlocked) Color(0x66FFD54F) else Color(0x66FF2A6D)
                ),
                modifier = Modifier.fillMaxWidth().testTag("vip_settings_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isPremiumUnlocked) Color(0xFFFFD54F) else Color(0xFFFF2A6D),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = if (isPremiumUnlocked) Color(0xFF141324) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isPremiumUnlocked) "LockDraw VIP (Attivo)" else "LockDraw VIP (4,99 €)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (isPremiumUnlocked) "Tutti i pennelli e sticker sbloccati a vita" else "Sblocca pennelli avanzati e sticker personalizzati",
                                    color = if (isPremiumUnlocked) Color(0xFFFFD54F) else Color(0xFFD0BCFF),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (!isPremiumUnlocked) {
                        Surface(
                            color = Color(0x28FF2A6D),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x40FF2A6D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "✨ Include: Pennello Ondulato, Neon Glow Pulsante, Importazione Sticker WhatsApp illimitata & Collezione Emoji completa",
                                    color = Color(0xFFFFE082),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                Button(
                                    onClick = { showPaywallDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF2A6D),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("open_paywall_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Acquista Versione Completa (4,99 €)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0x334ADE80),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x664ADE80)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VIP Pass a vita attivo per questa app",
                                    color = Color(0xFF81C784),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Notifica Disegno Partner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF100F1C), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Notifiche disegno partner",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Ricevi un avviso quando il partner disegna",
                                    color = Color(0xFF9E9DB5),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = partnerNotificationsEnabled,
                            onCheckedChange = { viewModel.setPartnerNotificationsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF7C4DFF),
                                uncheckedThumbColor = Color(0xFFB0AEC7),
                                uncheckedTrackColor = Color(0xFF282638)
                            ),
                            modifier = Modifier.testTag("partner_notifications_switch")
                        )
                    }
                }
            }

            // 6. GESTIONE STICKER WHATSAPP & COLLEZIONE EMOJI
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xD9161E28)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.2.dp, Color(0x5525D366)),
                modifier = Modifier.fillMaxWidth().testTag("whatsapp_stickers_settings_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF25D366),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Sticker WhatsApp & Emoji",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${customStickers.size} sticker WhatsApp salvati",
                                    color = Color(0xFF80E8A8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Procedura Importazione WhatsApp
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
                                    text = "Come importare da WhatsApp:",
                                    color = Color(0xFF80E8A8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                text = "1. Tocca 'Apri WhatsApp' qui sotto ed entra in una chat.\n" +
                                       "2. Tieni premuto sullo sticker e tocca 'Condividi'.\n" +
                                       "3. Scegli LockDraw: lo sticker verrà salvato all'istante!",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("main_screen_open_whatsapp_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Apri WhatsApp",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { showStickersSheet = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C4DFF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                                .testTag("open_all_stickers_sheet_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEmotions,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Emoji & Sticker",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Big Prominent Action: Disegna su Schermo Subito (Bolla Fluttuante)
            Button(
                onClick = {
                    if (hasOverlayPermission) {
                        if (!isFloatingActive) {
                            viewModel.startFloatingService(context)
                        }
                        Toast.makeText(context, "Tocca la bolla fluttuante per iniziare a disegnare su qualsiasi schermata!", Toast.LENGTH_LONG).show()
                    } else {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        overlayPermissionLauncher.launch(intent)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C4DFF),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("launch_floating_drawing_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Draw,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Apri Disegno su Schermo (Bolla)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Paywall VIP Dialog
        if (showPaywallDialog) {
            PaywallDialog(
                onDismiss = { showPaywallDialog = false },
                onUnlockSuccess = {
                    viewModel.unlockPremium(true)
                    showPaywallDialog = false
                    Toast.makeText(context, "LockDraw VIP sbloccato a 4,99 €! 🎉", Toast.LENGTH_LONG).show()
                }
            )
        }

        // Sticker & Emoji Sheet Overlay
        if (showStickersSheet) {
            StickerBottomSheet(
                isPremiumUnlocked = isPremiumUnlocked,
                customStickers = customStickers,
                onDismiss = { showStickersSheet = false },
                onSelectSticker = { emojiOrText ->
                    viewModel.addSticker(emojiOrText)
                    showStickersSheet = false
                    Toast.makeText(context, "Sticker aggiunto alla lavagna!", Toast.LENGTH_SHORT).show()
                },
                onDeleteCustomSticker = { viewModel.deleteCustomSticker(it) },
                onImportStickerUri = { uri ->
                    val path = viewModel.importStickerFromUri(uri, context, "Sticker WhatsApp")
                    if (path != null) {
                        Toast.makeText(context, "Sticker salvato con successo!", Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenPaywall = {
                    showStickersSheet = false
                    showPaywallDialog = true
                }
            )
        }
    }
}
