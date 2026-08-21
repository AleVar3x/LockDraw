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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WallpaperTheme
import com.example.data.sync.ConnectionStatus
import com.example.util.QrCodeView
import com.example.util.WallpaperTarget
import com.example.viewmodel.DrawingViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun MainDrawingScreen(
    viewModel: DrawingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val roomCode by viewModel.roomCode.collectAsState()
    val isMatched by viewModel.isMatched.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val partnerPresence by viewModel.partnerPresence.collectAsState()
    val strokes by viewModel.strokes.collectAsState()
    val placedStickers by viewModel.placedStickers.collectAsState()
    val isFloatingActive by viewModel.isFloatingServiceActive.collectAsState()
    val autoUpdateWallpaper by viewModel.autoUpdateRealWallpaper.collectAsState()
    val lockscreenConfig by viewModel.lockscreenConfig.collectAsState()

    var partnerCodeInput by remember { mutableStateOf("") }
    var showQrDialog by remember { mutableStateOf(false) }
    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(viewModel.canDrawOverlays(context)) }
    var isApplyingWallpaper by remember { mutableStateOf(false) }

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

                        // Generate New Random Code Button (Only available when NOT matched)
                        OutlinedButton(
                            onClick = {
                                val prefixes = listOf("LOVE", "HEART", "ROSE", "SOUL", "COUPLE", "STAR", "HONEY")
                                val newCode = "${prefixes.random()}-${Random.nextInt(100, 999)}"
                                viewModel.connectToRoomCode(newCode)
                                Toast.makeText(context, "Nuovo codice generato: $newCode", Toast.LENGTH_SHORT).show()
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
                            Text("Genera Nuovo Codice Stanza", fontSize = 13.sp)
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
                                    text = "Inserisci il codice fornito dall'altro smartphone",
                                    color = Color(0xFF9E9DB5),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Input Text Field
                        OutlinedTextField(
                            value = partnerCodeInput,
                            onValueChange = { partnerCodeInput = it.uppercase() },
                            placeholder = { Text("Es. LOVE-779", color = Color(0x66FFFFFF)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
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

            // 4. CARD: Stato Lavagna Condivisa & Sincronizzazione in Tempo Reale
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lavagna Condivisa in Tempo Reale",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        // Status Chip
                        Surface(
                            color = if (partnerPresence.isOnline) Color(0xFF2E7D32).copy(alpha = 0.3f) else Color(0xFFF57C00).copy(alpha = 0.2f),
                            shape = CircleShape,
                            border = BorderStroke(
                                1.dp,
                                if (partnerPresence.isOnline) Color(0xFF4ADE80) else Color(0xFFFFB74D)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (partnerPresence.isOnline) Color(0xFF4ADE80) else Color(0xFFFFB74D),
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = if (partnerPresence.isOnline) "🟢 Partner Online" else "🟠 In Attesa...",
                                    color = if (partnerPresence.isOnline) Color(0xFF4ADE80) else Color(0xFFFFB74D),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Canvas stats pill
                    Surface(
                        color = Color(0xFF100F1C),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${strokes.size}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Tratti", color = Color(0xFF8E8CA7), fontSize = 11.sp)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0x33FFFFFF)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${placedStickers.size}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Sticker", color = Color(0xFF8E8CA7), fontSize = 11.sp)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0x33FFFFFF)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = roomCode, color = Color(0xFFD0BCFF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Stanza", color = Color(0xFF8E8CA7), fontSize = 11.sp)
                            }
                        }
                    }

                    // Test Real-Time actions (heart, partner simulated stroke, sticker)
                    Text(
                        text = "Testa la sincronizzazione in tempo reale:",
                        color = Color(0xFF8E8CA7),
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Send Heart Ping
                        OutlinedButton(
                            onClick = {
                                viewModel.sendHeartReaction("💖", 0.5f, 0.5f)
                                Toast.makeText(context, "Cuore inviato al partner!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x44FF4081)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF80AB)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💖 Invia Cuore", fontSize = 11.sp)
                        }

                        // Simulated Partner Stroke
                        OutlinedButton(
                            onClick = {
                                viewModel.triggerPartnerSimulatedDraw("heart")
                                Toast.makeText(context, "Disegno partner avviato in tempo reale!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x44D0BCFF)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("✨ Tratto Partner", fontSize = 11.sp)
                        }
                    }

                    // Clear Canvas button
                    OutlinedButton(
                        onClick = {
                            viewModel.clearAllCanvas()
                            Toast.makeText(context, "Lavagna condivisa cancellata", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0x33FF5252)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancella Lavagna Condivisa per Entrambi", fontSize = 12.sp)
                    }
                }
            }

            // 5. CARD: Impostazioni Sfondo & Lockscreen Reale del Telefono
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
                            color = Color(0xFF2A3942),
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Wallpaper,
                                    contentDescription = null,
                                    tint = Color(0xFF80CBC4),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sfondo Lockscreen Reale",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Imposta i disegni come sfondo del tuo telefono",
                                color = Color(0xFF9E9DB5),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Auto update switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aggiorna Sfondo Lockscreen Reale",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Applica automaticamente i nuovi disegni del partner",
                                color = Color(0xFF7E7C98),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = autoUpdateWallpaper,
                            onCheckedChange = { viewModel.setAutoUpdateRealWallpaper(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6750A4)
                            )
                        )
                    }

                    // Manual Apply to Real Lockscreen Button
                    Button(
                        onClick = {
                            isApplyingWallpaper = true
                            viewModel.applyToRealLockscreen(context, WallpaperTarget.LOCKSCREEN) { success ->
                                isApplyingWallpaper = false
                                if (success) {
                                    Toast.makeText(context, "Sfondo applicato al Lockscreen con successo!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Impossibile impostare lo sfondo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isApplyingWallpaper
                    ) {
                        if (isApplyingWallpaper) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Applicazione in corso...", fontSize = 13.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Imposta Subito sul Lockscreen Reale", fontSize = 13.sp)
                        }
                    }
                }
            }

            // 6. Big Prominent Action: Disegna su Schermo Subito (Bolla Fluttuante)
            Button(
                onClick = {
                    if (hasOverlayPermission) {
                        if (!isFloatingActive) {
                            viewModel.startFloatingService(context)
                        }
                        Toast.makeText(context, "Tocca la bolla fluttuante per iniziare a disegnare!", Toast.LENGTH_LONG).show()
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
    }
}
