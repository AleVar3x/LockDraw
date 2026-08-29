package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.screens.MainDrawingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.NotificationHelper
import com.example.viewmodel.DrawingViewModel

class MainActivity : ComponentActivity() {
  private val drawingViewModel: DrawingViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    NotificationHelper.createNotificationChannels(this)

    handleIncomingShareIntent(intent)

    setContent {
      MyApplicationTheme {
        val notifPermissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }

        LaunchedEffect(Unit) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
              notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
          }
        }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          MainDrawingScreen(
            viewModel = drawingViewModel,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIncomingShareIntent(intent)
  }

  private fun handleIncomingShareIntent(intent: Intent) {
    val action = intent.action
    val type = intent.type

    if (Intent.ACTION_SEND == action && type != null) {
      if (type.startsWith("image/")) {
        val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
          @Suppress("DEPRECATION")
          intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        imageUri?.let { uri ->
          val importedPath = drawingViewModel.importStickerFromUri(uri, this, "Sticker WhatsApp")
          if (importedPath != null) {
            Toast.makeText(this, "Sticker salvato con successo nella galleria Sticker! ✨", Toast.LENGTH_LONG).show()
          } else {
            Toast.makeText(this, "Impossibile salvare lo sticker.", Toast.LENGTH_SHORT).show()
          }
        }
      } else if (type.startsWith("text/")) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!sharedText.isNullOrBlank()) {
          drawingViewModel.importStickerFromText(sharedText)
          Toast.makeText(this, "Testo salvato come sticker! ✨", Toast.LENGTH_SHORT).show()
        }
      }
    } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null && type.startsWith("image/")) {
      val imageUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
      } else {
        @Suppress("DEPRECATION")
        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
      }
      var count = 0
      imageUris?.forEach { uri ->
        if (drawingViewModel.importStickerFromUri(uri, this, "Sticker WhatsApp ${count + 1}") != null) {
          count++
        }
      }
      if (count > 0) {
        Toast.makeText(this, "$count sticker salvati nella galleria Sticker! ✨", Toast.LENGTH_LONG).show()
      }
    }
  }

  override fun onResume() {
    super.onResume()
    drawingViewModel.onAppResumed()
  }
}


