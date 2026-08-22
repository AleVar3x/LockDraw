package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BrushType
import com.example.data.model.DrawingStroke
import com.example.data.model.LockscreenConfig
import com.example.data.model.PlacedSticker
import com.example.data.model.WallpaperTheme
import com.example.data.repository.CanvasSnapshot
import com.example.data.repository.DrawingRepository
import com.example.data.repository.FloatingHeartReaction
import com.example.data.sync.ConnectionStatus
import com.example.data.sync.PartnerPresence
import com.example.data.sync.PartnerSyncManager
import com.example.service.FloatingDrawingService
import com.example.util.WallpaperTarget
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DrawingViewModel(application: Application) : AndroidViewModel(application) {

    val repository = DrawingRepository.getInstance(application)
    val syncManager: PartnerSyncManager = repository.syncManager

    // Drawing state
    val strokes: StateFlow<List<DrawingStroke>> = repository.strokes
    val currentDraftStroke: StateFlow<DrawingStroke?> = repository.currentDraftStroke
    val partnerDraftStroke: StateFlow<DrawingStroke?> = repository.partnerDraftStroke
    val placedStickers: StateFlow<List<PlacedSticker>> = repository.placedStickers
    val selectedStickerId: StateFlow<String?> = repository.selectedStickerId

    // Tooling state
    val selectedBrushType: StateFlow<BrushType> = repository.selectedBrushType
    val selectedColor: StateFlow<Color> = repository.selectedColor
    val strokeWidth: StateFlow<Float> = repository.strokeWidth
    val strokeAlpha: StateFlow<Float> = repository.strokeAlpha

    // Lockscreen & Floating state
    val lockscreenConfig: StateFlow<LockscreenConfig> = repository.lockscreenConfig
    val isLockscreenOverlayVisible: StateFlow<Boolean> = repository.isLockscreenOverlayVisible
    val isSimulatorOpen: StateFlow<Boolean> = repository.isSimulatorOpen
    val isFloatingServiceActive: StateFlow<Boolean> = repository.isFloatingServiceActive
    val autoUpdateRealWallpaper: StateFlow<Boolean> = repository.autoUpdateRealWallpaper

    val floatingReactions: StateFlow<List<FloatingHeartReaction>> = repository.floatingReactions
    val isMatched: StateFlow<Boolean> = repository.isMatched
    val connectionStatus: StateFlow<ConnectionStatus> = repository.connectionStatus
    val partnerPresence: StateFlow<PartnerPresence> = repository.partnerPresence
    val roomCode: StateFlow<String> = repository.roomCode
    val myName: StateFlow<String> = repository.myName
    val partnerCustomName: StateFlow<String> = repository.partnerCustomName

    fun setMyName(name: String) {
        repository.setMyName(name)
    }

    fun setPartnerCustomName(name: String) {
        repository.setPartnerCustomName(name)
    }

    fun generateNewRandomRoomCode() {
        val newCode = PartnerSyncManager.generateRandomRoomCode(16)
        repository.connectToRoomCode(newCode)
    }

    fun unmatchPartner() {
        repository.unmatchPartner()
    }

    fun startDrawing(normalizedX: Float, normalizedY: Float, pressure: Float = 1.0f) {
        repository.startDrawing(normalizedX, normalizedY, pressure)
    }

    fun continueDrawing(normalizedX: Float, normalizedY: Float, pressure: Float = 1.0f) {
        repository.continueDrawing(normalizedX, normalizedY, pressure)
    }

    fun finishDrawing() {
        repository.finishDrawing()
    }

    fun selectBrush(brushType: BrushType) {
        repository.selectBrush(brushType)
    }

    fun selectColor(color: Color) {
        repository.selectColor(color)
    }

    fun setStrokeWidth(width: Float) {
        repository.setStrokeWidth(width)
    }

    fun setStrokeAlpha(alpha: Float) {
        repository.setStrokeAlpha(alpha)
    }

    fun addSticker(emojiOrText: String, x: Float = 0.5f, y: Float = 0.45f) {
        repository.addSticker(emojiOrText, x, y)
    }

    fun updateStickerPosition(stickerId: String, newX: Float, newY: Float) {
        repository.updateStickerPosition(stickerId, newX, newY)
    }

    fun updateStickerTransform(stickerId: String, scaleDelta: Float, rotationDelta: Float) {
        repository.updateStickerTransform(stickerId, scaleDelta, rotationDelta)
    }

    fun selectSticker(id: String?) {
        repository.selectSticker(id)
    }

    fun deleteSticker(id: String) {
        repository.deleteSticker(id)
    }

    fun clearAllCanvas() {
        repository.clearAllCanvas()
    }

    fun undo() {
        repository.undo()
    }

    fun redo() {
        repository.redo()
    }

    fun setWallpaperTheme(theme: WallpaperTheme) {
        repository.setWallpaperTheme(theme)
    }

    fun setCustomWallpaperUri(uriString: String) {
        repository.setCustomWallpaperUri(uriString)
    }

    fun toggleLockscreenOverlay() {
        repository.toggleLockscreenOverlay()
    }

    fun setSimulatorOpen(open: Boolean) {
        repository.setSimulatorOpen(open)
    }

    fun sendHeartReaction(emoji: String = "💖", x: Float = 0.5f, y: Float = 0.5f) {
        repository.sendHeartReaction(emoji, x, y)
    }

    fun connectToRoomCode(newCode: String) {
        repository.connectToRoomCode(newCode)
    }

    fun triggerPartnerSimulatedDraw(type: String = "heart") {
        repository.triggerPartnerSimulatedDraw(type)
    }

    fun triggerPartnerSimulatedSticker() {
        repository.triggerPartnerSimulatedSticker()
    }

    fun setAutoUpdateRealWallpaper(enabled: Boolean) {
        repository.setAutoUpdateRealWallpaper(enabled)
    }

    fun applyToRealLockscreen(context: Context, target: WallpaperTarget = WallpaperTarget.LOCKSCREEN, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.applyToDeviceWallpaper(target)
            onResult(success)
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun startFloatingService(context: Context) {
        FloatingDrawingService.start(context)
        repository.setFloatingServiceActive(true)
    }

    fun stopFloatingService(context: Context) {
        FloatingDrawingService.stop(context)
        repository.setFloatingServiceActive(false)
    }

    fun toggleFloatingService(context: Context) {
        if (repository.isFloatingServiceActive.value) {
            stopFloatingService(context)
        } else {
            startFloatingService(context)
        }
    }
}
