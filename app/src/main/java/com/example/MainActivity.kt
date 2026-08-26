package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

  override fun onResume() {
    super.onResume()
    drawingViewModel.onAppResumed()
  }
}


