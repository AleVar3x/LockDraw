package com.example.viewmodel

import android.app.Activity
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
    val selectedModifier: StateFlow<com.example.data.model.StrokeModifier> = repository.selectedModifier
    val selectedColor: StateFlow<Color> = repository.selectedColor
    val strokeWidth: StateFlow<Float> = repository.strokeWidth
    val strokeAlpha: StateFlow<Float> = repository.strokeAlpha

    // VIP Premium & Paywall state
    val isPremiumUnlocked: StateFlow<Boolean> = repository.isPremiumUnlocked
    val formattedVipPrice: StateFlow<String> = repository.formattedVipPrice
    val billingStatus = repository.billingStatus
    val partnerNotificationsEnabled: StateFlow<Boolean> = repository.partnerNotificationsEnabled
    val customStickers: StateFlow<List<com.example.data.model.CustomStickerItem>> = repository.customStickers

    // Floating state
    val lockscreenConfig: StateFlow<LockscreenConfig> = repository.lockscreenConfig
    val isFloatingServiceActive: StateFlow<Boolean> = repository.isFloatingServiceActive

    val floatingReactions: StateFlow<List<FloatingHeartReaction>> = repository.floatingReactions
    val isMyDrawingsTransparent: StateFlow<Boolean> = repository.isMyDrawingsTransparent
    val isMatched: StateFlow<Boolean> = repository.isMatched
    val connectionStatus: StateFlow<ConnectionStatus> = repository.connectionStatus
    val lastSyncError: StateFlow<String?> = syncManager.lastSyncError
    val partnerPresence: StateFlow<PartnerPresence> = repository.partnerPresence
    val roomCode: StateFlow<String> = repository.roomCode
    val myName: StateFlow<String> = repository.myName
    val partnerCustomName: StateFlow<String> = repository.partnerCustomName

    fun launchBillingFlow(activity: Activity): Boolean {
        return repository.launchBillingFlow(activity)
    }

    fun restorePurchases(onComplete: (Boolean) -> Unit) {
        repository.restorePurchases(onComplete)
    }

    fun unlockPremium(unlocked: Boolean = true) {
        repository.unlockPremium(unlocked)
    }

    fun setPartnerNotificationsEnabled(enabled: Boolean) {
        repository.setPartnerNotificationsEnabled(enabled)
    }

    fun addCustomSticker(sticker: com.example.data.model.CustomStickerItem) {
        repository.addCustomSticker(sticker)
    }

    fun deleteCustomSticker(id: String) {
        repository.deleteCustomSticker(id)
    }

    fun reconnectSync() {
        syncManager.reconnect()
    }

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

    fun toggleMyDrawingsTransparency() {
        repository.toggleMyDrawingsTransparency()
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

    fun selectModifier(modifier: com.example.data.model.StrokeModifier) {
        repository.selectModifier(modifier)
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

    fun importStickerFromUri(uri: Uri, context: Context, name: String = "WhatsApp Sticker"): String? {
        return repository.importStickerFromUri(uri, context, name)
    }

    fun importStickerFromText(text: String) {
        repository.importStickerFromText(text)
    }

    fun updateStickerPosition(stickerId: String, newX: Float, newY: Float) {
        repository.updateStickerPosition(stickerId, newX, newY)
    }

    fun moveStickerDelta(stickerId: String, deltaX: Float, deltaY: Float) {
        repository.moveStickerDelta(stickerId, deltaX, deltaY)
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

    fun onAppResumed() {
        repository.ensureActiveSync()
    }

    fun toggleFloatingService(context: Context) {
        if (repository.isFloatingServiceActive.value) {
            stopFloatingService(context)
        } else {
            startFloatingService(context)
        }
    }
}
