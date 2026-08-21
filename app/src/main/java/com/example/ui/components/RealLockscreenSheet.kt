package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.WallpaperTarget
import com.example.viewmodel.DrawingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealLockscreenSheet(
    viewModel: DrawingViewModel,
    isFloatingActive: Boolean,
    autoUpdateWallpaper: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isApplying by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161726),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Lockscreen & Schermo Reale",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Disegna direttamente sul lockscreen o usa la bolla fluttuante in background",
                color = Color(0xFFD0BCFF).copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // 1. Floating Bubble Mode Toggle Card
            Surface(
                color = Color(0x1CFFFFFF),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = Color(0xFF4F378B),
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = Color(0xFFD0BCFF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Bolla Fluttuante su Schermo",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Disegna sopra Homescreen, Lockscreen e altre app",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = isFloatingActive,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (!viewModel.canDrawOverlays(context)) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                            Toast.makeText(
                                                context,
                                                "Abilita 'Mostra sopra altre app' per LockDraw",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else {
                                        viewModel.startFloatingService(context)
                                        Toast.makeText(context, "Bolla fluttuante attivata!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    viewModel.stopFloatingService(context)
                                    Toast.makeText(context, "Bolla fluttuante disattivata", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4F378B)
                            ),
                            modifier = Modifier.testTag("toggle_floating_service_switch")
                        )
                    }

                    if (!viewModel.canDrawOverlays(context)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Autorizza Permesso Overlay Schermo", fontSize = 12.sp, color = Color(0xFFD0BCFF))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Direct Wallpaper / Lockscreen Application Card
            Surface(
                color = Color(0x1CFFFFFF),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Imposta Disegno su Schermate Reali",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Applica istantaneamente il disegno attuale come sfondo di sistema",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Apply to Lockscreen
                        Button(
                            onClick = {
                                isApplying = true
                                viewModel.applyToRealLockscreen(context, WallpaperTarget.LOCKSCREEN) { success ->
                                    isApplying = false
                                    if (success) {
                                        Toast.makeText(context, "✨ Applicato a Lockscreen Reale!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Impossibile impostare sfondo", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("apply_real_lockscreen_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F378B)),
                            enabled = !isApplying
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD0BCFF))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lockscreen", fontSize = 12.sp, color = Color.White)
                        }

                        // Apply to Homescreen
                        Button(
                            onClick = {
                                isApplying = true
                                viewModel.applyToRealLockscreen(context, WallpaperTarget.HOMESCREEN) { success ->
                                    isApplying = false
                                    if (success) {
                                        Toast.makeText(context, "🏠 Applicato a Schermata Home!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Impossibile impostare sfondo", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("apply_real_homescreen_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                            enabled = !isApplying
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Home", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Apply to Both
                    Button(
                        onClick = {
                            isApplying = true
                            viewModel.applyToRealLockscreen(context, WallpaperTarget.BOTH) { success ->
                                isApplying = false
                                if (success) {
                                    Toast.makeText(context, "✨ Applicato a Lockscreen + Home!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Impossibile impostare sfondo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_real_both_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                        enabled = !isApplying
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD0BCFF))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Imposta su Entrambi (Lock + Home)", fontSize = 12.sp, color = Color(0xFFD0BCFF))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Auto-update Real Lockscreen Toggle
            Surface(
                color = Color(0x1CFFFFFF),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = Color(0x28FFFFFF),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Auto-aggiorna Lockscreen Reale",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Aggiorna lo sfondo del telefono appena il partner disegna",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = autoUpdateWallpaper,
                        onCheckedChange = { enabled ->
                            viewModel.setAutoUpdateRealWallpaper(enabled)
                            Toast.makeText(
                                context,
                                if (enabled) "Auto-aggiornamento lockscreen attivo" else "Auto-aggiornamento disattivato",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4F378B)
                        ),
                        modifier = Modifier.testTag("toggle_auto_update_wallpaper_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Homescreen Widget Guide Card
            Surface(
                color = Color(0x1CFFFFFF),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0x28FFFFFF),
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                tint = Color(0xFFC2E7FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Widget Schermata Home",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tieni premuto sulla Home del telefono > Widget > LockDraw per vedere i disegni in tempo reale.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
