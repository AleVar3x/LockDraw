package com.example.data.repository

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.data.billing.PlayBillingManager
import com.example.data.local.DrawingDatabase
import com.example.data.local.DrawingSessionEntity
import com.example.data.local.SavedStickerEntity
import com.example.data.local.SavedStrokeEntity
import com.example.data.model.BrushType
import com.example.data.model.CustomStickerItem
import com.example.data.model.DrawingPoint
import com.example.data.model.DrawingStroke
import com.example.data.model.LockscreenConfig
import com.example.data.model.PlacedSticker
import com.example.data.model.WallpaperTheme
import com.example.data.sync.ConnectionStatus
import com.example.data.sync.PartnerPresence
import com.example.data.sync.PartnerSyncManager
import com.example.data.sync.SyncAction
import com.example.util.NotificationHelper
import com.example.util.WallpaperHelper
import com.example.widget.PartnerDrawingWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class FloatingHeartReaction(
    val id: String = UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    val emoji: String = "💖",
    val sender: String = "Partner",
    val createdAt: Long = System.currentTimeMillis()
)

data class CanvasSnapshot(
    val strokes: List<DrawingStroke>,
    val stickers: List<PlacedSticker>
)

class DrawingRepository private constructor(private val application: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val db = DrawingDatabase.getInstance(application)
    private val dao = db.drawingDao()
    private val prefs: SharedPreferences = application.getSharedPreferences("lockdraw_prefs", Context.MODE_PRIVATE)

    val syncManager = PartnerSyncManager(application, scope)
    val playBillingManager = PlayBillingManager(application) { unlocked ->
        unlockPremium(unlocked)
    }

    // VIP Pricing & Billing
    val formattedVipPrice: StateFlow<String> = playBillingManager.formattedPrice
    val billingStatus = playBillingManager.billingState

    fun launchBillingFlow(activity: Activity): Boolean {
        return playBillingManager.launchBillingFlow(activity)
    }

    fun restorePurchases(onComplete: (Boolean) -> Unit) {
        playBillingManager.restorePurchases(onComplete)
    }

    // Drawing state
    private val _strokes = MutableStateFlow<List<DrawingStroke>>(emptyList())
    val strokes: StateFlow<List<DrawingStroke>> = _strokes.asStateFlow()

    private val _currentDraftStroke = MutableStateFlow<DrawingStroke?>(null)
    val currentDraftStroke: StateFlow<DrawingStroke?> = _currentDraftStroke.asStateFlow()

    private val _partnerDraftStroke = MutableStateFlow<DrawingStroke?>(null)
    val partnerDraftStroke: StateFlow<DrawingStroke?> = _partnerDraftStroke.asStateFlow()

    private val _placedStickers = MutableStateFlow<List<PlacedSticker>>(emptyList())
    val placedStickers: StateFlow<List<PlacedSticker>> = _placedStickers.asStateFlow()

    private val _selectedStickerId = MutableStateFlow<String?>(null)
    val selectedStickerId: StateFlow<String?> = _selectedStickerId.asStateFlow()

    // History for Undo/Redo
    private val undoHistory = mutableListOf<CanvasSnapshot>()
    private val redoHistory = mutableListOf<CanvasSnapshot>()

    // Tooling state
    private val _selectedBrushType = MutableStateFlow(BrushType.PEN)
    val selectedBrushType: StateFlow<BrushType> = _selectedBrushType.asStateFlow()

    private val _selectedModifier = MutableStateFlow(com.example.data.model.StrokeModifier.NONE)
    val selectedModifier: StateFlow<com.example.data.model.StrokeModifier> = _selectedModifier.asStateFlow()

    private val _selectedColor = MutableStateFlow(Color(0xFFD0BCFF))
    val selectedColor: StateFlow<Color> = _selectedColor.asStateFlow()

    private val _strokeWidth = MutableStateFlow(14f)
    val strokeWidth: StateFlow<Float> = _strokeWidth.asStateFlow()

    private val _strokeAlpha = MutableStateFlow(1.0f)
    val strokeAlpha: StateFlow<Float> = _strokeAlpha.asStateFlow()

    // Premium & Paywall state (4.99€ VIP)
    private val _isPremiumUnlocked = MutableStateFlow(prefs.getBoolean("is_premium_unlocked", false))
    val isPremiumUnlocked: StateFlow<Boolean> = _isPremiumUnlocked.asStateFlow()

    // Partner Drawing Notifications state
    private val _partnerNotificationsEnabled = MutableStateFlow(prefs.getBoolean("partner_notifications_enabled", true))
    val partnerNotificationsEnabled: StateFlow<Boolean> = _partnerNotificationsEnabled.asStateFlow()

    // WhatsApp-style Custom Stickers state
    private val _customStickers = MutableStateFlow<List<CustomStickerItem>>(emptyList())
    val customStickers: StateFlow<List<CustomStickerItem>> = _customStickers.asStateFlow()

    // Lockscreen styling state
    private val _lockscreenConfig = MutableStateFlow(LockscreenConfig(wallpaperTheme = WallpaperTheme.FROSTED_GLASS))
    val lockscreenConfig: StateFlow<LockscreenConfig> = _lockscreenConfig.asStateFlow()

    // Floating background mode state
    private val _isFloatingServiceActive = MutableStateFlow(false)
    val isFloatingServiceActive: StateFlow<Boolean> = _isFloatingServiceActive.asStateFlow()

    // Partner reaction floating indicators
    private val _floatingReactions = MutableStateFlow<List<FloatingHeartReaction>>(emptyList())
    val floatingReactions: StateFlow<List<FloatingHeartReaction>> = _floatingReactions.asStateFlow()

    // My Drawings 20% Transparency Switch (ghost mode to use phone without deleting draft before partner sees)
    private val _isMyDrawingsTransparent = MutableStateFlow(false)
    val isMyDrawingsTransparent: StateFlow<Boolean> = _isMyDrawingsTransparent.asStateFlow()

    fun toggleMyDrawingsTransparency() {
        _isMyDrawingsTransparent.value = !_isMyDrawingsTransparent.value
    }

    fun setMyDrawingsTransparency(enabled: Boolean) {
        _isMyDrawingsTransparent.value = enabled
    }

    val isMatched: StateFlow<Boolean> = syncManager.isMatched
    val connectionStatus: StateFlow<ConnectionStatus> = syncManager.connectionStatus
    val partnerPresence: StateFlow<PartnerPresence> = syncManager.partnerPresence
    val roomCode: StateFlow<String> = syncManager.currentRoomCode
    val myName: StateFlow<String> = syncManager.myName
    val partnerCustomName: StateFlow<String> = syncManager.partnerCustomName

    fun setMyName(name: String) {
        syncManager.setMyName(name)
    }

    fun setPartnerCustomName(name: String) {
        syncManager.setPartnerCustomName(name)
    }

    fun unlockPremium(unlocked: Boolean = true) {
        _isPremiumUnlocked.value = unlocked
        prefs.edit().putBoolean("is_premium_unlocked", unlocked).apply()
    }

    fun setPartnerNotificationsEnabled(enabled: Boolean) {
        _partnerNotificationsEnabled.value = enabled
        prefs.edit().putBoolean("partner_notifications_enabled", enabled).apply()
    }

    fun addCustomSticker(sticker: CustomStickerItem) {
        val updated = listOf(sticker) + _customStickers.value.filter { it.id != sticker.id }
        _customStickers.value = updated
        saveCustomStickersToPrefs(updated)
    }

    fun importStickerFromUri(uri: Uri, context: Context, name: String = "Sticker WhatsApp"): String? {
        return try {
            val stickersDir = java.io.File(context.filesDir, "stickers").apply { mkdirs() }
            val targetFile = java.io.File(stickersDir, "sticker_wa_${System.currentTimeMillis()}.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val path = targetFile.absolutePath
            val customSticker = CustomStickerItem(
                title = name,
                emoji = "💬",
                imageUri = path,
                isDoodle = false
            )
            addCustomSticker(customSticker)
            path
        } catch (e: Exception) {
            Log.e("DrawingRepository", "Error importing sticker from uri", e)
            null
        }
    }

    fun importStickerFromText(text: String) {
        if (text.isNotBlank()) {
            val customSticker = CustomStickerItem(
                title = text.take(30),
                emoji = "💬",
                backgroundColorArgb = 0xFF25D366,
                textColorArgb = 0xFFFFFFFF,
                isDoodle = false
            )
            addCustomSticker(customSticker)
        }
    }

    fun deleteCustomSticker(id: String) {
        val updated = _customStickers.value.filter { it.id != id }
        _customStickers.value = updated
        saveCustomStickersToPrefs(updated)
    }

    private fun loadCustomStickersFromPrefs(): List<CustomStickerItem> {
        val raw = prefs.getString("custom_stickers_json", null) ?: return defaultCustomStickers()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<CustomStickerItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CustomStickerItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        emoji = obj.optString("emoji", "✨"),
                        backgroundColorArgb = obj.optLong("backgroundColorArgb", 0xFF4F378B),
                        textColorArgb = obj.optLong("textColorArgb", 0xFFFFFFFF),
                        isDoodle = obj.optBoolean("isDoodle", false),
                        doodlePoints = obj.optString("doodlePoints", null),
                        imageUri = obj.optString("imageUri", null).takeIf { !it.isNullOrBlank() },
                        imageBase64 = obj.optString("imageBase64", null).takeIf { !it.isNullOrBlank() },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            if (list.isEmpty()) defaultCustomStickers() else list
        } catch (e: Exception) {
            defaultCustomStickers()
        }
    }

    private fun saveCustomStickersToPrefs(list: List<CustomStickerItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("emoji", item.emoji)
                    put("backgroundColorArgb", item.backgroundColorArgb)
                    put("textColorArgb", item.textColorArgb)
                    put("isDoodle", item.isDoodle)
                    put("doodlePoints", item.doodlePoints ?: "")
                    put("imageUri", item.imageUri ?: "")
                    put("imageBase64", item.imageBase64 ?: "")
                    put("createdAt", item.createdAt)
                }
                arr.put(obj)
            }
            prefs.edit().putString("custom_stickers_json", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("DrawingRepository", "Error saving custom stickers", e)
        }
    }

    private fun defaultCustomStickers(): List<CustomStickerItem> {
        return listOf(
            CustomStickerItem(title = "Sei il mio cuore ❤️", emoji = "💖", backgroundColorArgb = 0xFFDD2476, textColorArgb = 0xFFFFFFFF),
            CustomStickerItem(title = "Ti penso tantissimo ✨", emoji = "🥺", backgroundColorArgb = 0xFF302B63, textColorArgb = 0xFFFFFFFF),
            CustomStickerItem(title = "Bacio della buonanotte 🌙", emoji = "💋", backgroundColorArgb = 0xFF1A1C2E, textColorArgb = 0xFFFFD54F)
        )
    }

    init {
        _customStickers.value = loadCustomStickersFromPrefs()
        listenToIncomingSyncActions()
        loadPersistedSession(syncManager.currentRoomCode.value)
    }

    fun setFloatingServiceActive(active: Boolean) {
        _isFloatingServiceActive.value = active
    }

    private var partnerStrokeReplayJob: Job? = null
    private var pendingReplayStroke: DrawingStroke? = null

    private fun replayPartnerStroke(stroke: DrawingStroke) {
        pendingReplayStroke?.let { prev ->
            if (_strokes.value.none { it.id == prev.id }) {
                _strokes.value = _strokes.value + prev
            }
        }
        partnerStrokeReplayJob?.cancel()
        pendingReplayStroke = stroke

        if (stroke.points.size <= 2) {
            _partnerDraftStroke.value = null
            if (_strokes.value.none { it.id == stroke.id }) {
                _strokes.value = _strokes.value + stroke
            }
            pendingReplayStroke = null
            persistCurrentState()
            handlePartnerUpdatedDrawing("ha appena disegnato sulla tua schermata! 🎨")
            return
        }

        partnerStrokeReplayJob = scope.launch(Dispatchers.Default) {
            val totalPoints = stroke.points.size
            val totalDurationMs = (totalPoints * 8L).coerceIn(160L, 420L)
            val frameIntervalMs = 16L
            val totalFrames = (totalDurationMs / frameIntervalMs).coerceAtLeast(4)
            val pointsPerFrame = (totalPoints.toFloat() / totalFrames).coerceAtLeast(1f)

            var currentPointCount = 1
            while (currentPointCount <= totalPoints && isActive) {
                val visiblePoints = stroke.points.take(currentPointCount)
                _partnerDraftStroke.value = stroke.copy(points = visiblePoints)
                delay(frameIntervalMs)
                currentPointCount = (currentPointCount + pointsPerFrame.toInt().coerceAtLeast(1)).coerceAtMost(totalPoints + 1)
            }

            _partnerDraftStroke.value = null
            if (_strokes.value.none { it.id == stroke.id }) {
                _strokes.value = _strokes.value + stroke
            }
            pendingReplayStroke = null
            persistCurrentState()
            handlePartnerUpdatedDrawing("ha appena disegnato sulla tua schermata! 🎨")
        }
    }

    private fun listenToIncomingSyncActions() {
        scope.launch {
            syncManager.incomingActions.collect { action ->
                when (action) {
                    is SyncAction.StrokeBegin -> {
                        val stroke = DrawingStroke(
                            id = action.strokeId,
                            points = listOf(DrawingPoint(action.x, action.y)),
                            colorArgb = action.colorArgb,
                            strokeWidth = action.strokeWidth,
                            brushType = action.brushType,
                            authorId = action.authorId,
                            alpha = action.alpha,
                            modifier = action.modifier
                        )
                        _partnerDraftStroke.value = stroke
                    }
                    is SyncAction.StrokePoints -> {
                        val current = _partnerDraftStroke.value
                        if (current != null && current.id == action.strokeId) {
                            _partnerDraftStroke.value = current.copy(points = action.points)
                        } else {
                            _partnerDraftStroke.value = DrawingStroke(
                                id = action.strokeId,
                                points = action.points,
                                authorId = action.authorId
                            )
                        }
                    }
                    is SyncAction.StrokeFinished -> {
                        if (action.stroke.brushType == BrushType.ERASER) {
                            _partnerDraftStroke.value = null
                            applyEraserPoints(action.stroke.points, action.stroke.strokeWidth)
                            persistCurrentState()
                            handlePartnerUpdatedDrawing(null)
                        } else if (action.stroke.authorId != syncManager.myDeviceId) {
                            replayPartnerStroke(action.stroke)
                        } else {
                            val existingIndex = _strokes.value.indexOfFirst { it.id == action.stroke.id }
                            if (existingIndex < 0) {
                                _strokes.value = _strokes.value + action.stroke
                                persistCurrentState()
                            }
                        }
                    }
                    is SyncAction.PlaceSticker -> {
                        if (_placedStickers.value.none { it.id == action.sticker.id }) {
                            _placedStickers.value = _placedStickers.value + action.sticker
                            persistCurrentState()
                            if (action.sticker.authorId != syncManager.myDeviceId && syncManager.isMatched.value) {
                                handlePartnerUpdatedDrawing("ha aggiunto un nuovo sticker sulla tela! ✨")
                            } else {
                                handlePartnerUpdatedDrawing(null)
                            }
                        }
                    }
                    is SyncAction.UpdateSticker -> {
                        _placedStickers.value = _placedStickers.value.map {
                            if (it.id == action.sticker.id) action.sticker else it
                        }
                        persistCurrentState()
                        handlePartnerUpdatedDrawing(null)
                    }
                    is SyncAction.RemoveSticker -> {
                        _placedStickers.value = _placedStickers.value.filter { it.id != action.stickerId }
                        persistCurrentState()
                        handlePartnerUpdatedDrawing(null)
                    }
                    is SyncAction.ClearUserLayer -> {
                        if (action.authorId == syncManager.myDeviceId) {
                            saveSnapshotForUndo()
                            _strokes.value = _strokes.value.filter { it.authorId != syncManager.myDeviceId }
                            _placedStickers.value = _placedStickers.value.filter { it.authorId != syncManager.myDeviceId }
                        } else {
                            // Partner cleared their layer
                            _strokes.value = _strokes.value.filter { it.authorId != action.authorId && it.authorId != "partner" }
                            _placedStickers.value = _placedStickers.value.filter { it.authorId != action.authorId && it.authorId != "partner" }
                        }
                        _partnerDraftStroke.value = null
                        persistCurrentState()
                        handlePartnerUpdatedDrawing(null)
                    }
                    is SyncAction.ClearAll -> {
                        saveSnapshotForUndo()
                        _strokes.value = emptyList()
                        _placedStickers.value = emptyList()
                        _partnerDraftStroke.value = null
                        persistCurrentState()
                        handlePartnerUpdatedDrawing(null)
                    }
                    is SyncAction.Undo -> {
                        val authorStrokes = _strokes.value.filter { it.authorId == action.authorId || (action.authorId != syncManager.myDeviceId && it.authorId == "partner") }
                        if (authorStrokes.isNotEmpty()) {
                            val lastId = authorStrokes.last().id
                            _strokes.value = _strokes.value.filter { it.id != lastId }
                            persistCurrentState()
                            handlePartnerUpdatedDrawing(null)
                        }
                    }
                    is SyncAction.HeartPing -> {
                        val reaction = FloatingHeartReaction(
                            x = action.x,
                            y = action.y,
                            emoji = action.emoji,
                            sender = action.sender
                        )
                        _floatingReactions.value = _floatingReactions.value + reaction
                        scope.launch {
                            kotlinx.coroutines.delay(3500)
                            _floatingReactions.value = _floatingReactions.value.filter { it.id != reaction.id }
                        }
                    }
                    is SyncAction.ChangeWallpaper -> {
                        _lockscreenConfig.value = _lockscreenConfig.value.copy(
                            wallpaperTheme = action.wallpaperTheme,
                            customWallpaperUri = action.customUri
                        )
                        persistCurrentState()
                        handlePartnerUpdatedDrawing(null)
                    }
                    is SyncAction.RequestSnapshot -> {
                        // Partner just joined and requested current canvas state
                        val myStrokes = _strokes.value.filter { it.authorId == syncManager.myDeviceId }
                        val myStickers = _placedStickers.value.filter { it.authorId == syncManager.myDeviceId }
                        if (myStrokes.isNotEmpty() || myStickers.isNotEmpty()) {
                            syncManager.broadcastCanvasChange(
                                finishedStroke = null,
                                myStrokes = myStrokes,
                                myStickers = myStickers,
                                wallpaperTheme = _lockscreenConfig.value.wallpaperTheme
                            )
                        }
                    }
                    is SyncAction.UserLayerSnapshot -> {
                        // Conflict-free multi-layer update:
                        // Replace only the remote author's layer while strictly keeping local user's layer intact!
                        val myDeviceId = syncManager.myDeviceId
                        val remoteAuthorId = action.authorId

                        val keptStrokes = _strokes.value.filter { it.authorId != remoteAuthorId && (remoteAuthorId != "partner" || it.authorId == myDeviceId) }
                        val keptStickers = _placedStickers.value.filter { it.authorId != remoteAuthorId && (remoteAuthorId != "partner" || it.authorId == myDeviceId) }

                        // Standardize incoming layer authorId
                        val sanitizedRemoteStrokes = action.strokes.map { if (it.authorId.isBlank()) it.copy(authorId = remoteAuthorId) else it }
                        val sanitizedRemoteStickers = action.stickers.map { if (it.authorId.isBlank()) it.copy(authorId = remoteAuthorId) else it }

                        _strokes.value = keptStrokes + sanitizedRemoteStrokes
                        _placedStickers.value = keptStickers + sanitizedRemoteStickers
                        action.wallpaperTheme?.let {
                            _lockscreenConfig.value = _lockscreenConfig.value.copy(wallpaperTheme = it)
                        }
                        persistCurrentState()
                        handlePartnerUpdatedDrawing(null)
                    }
                    else -> {}
                }
            }
        }
    }

    private var lastDrawingNotifiedTime: Long = 0L

    private fun handlePartnerUpdatedDrawing(detail: String? = null) {
        PartnerDrawingWidgetProvider.updateAllWidgets(application)

        if (detail != null && _partnerNotificationsEnabled.value) {
            val now = System.currentTimeMillis()
            if (now - lastDrawingNotifiedTime > 12_000L) { // Throttle at 12s interval
                lastDrawingNotifiedTime = now
                val resolvedPartnerName = syncManager.partnerPresence.value.partnerName.ifBlank { "Il tuo partner" }
                NotificationHelper.showPartnerDrawingNotification(
                    context = application,
                    partnerName = resolvedPartnerName,
                    roomCode = syncManager.currentRoomCode.value,
                    detailText = detail
                )
            }
        }
    }

    fun ensureActiveSync() {
        syncManager.ensureActiveConnection()
    }

    fun startDrawing(normalizedX: Float, normalizedY: Float, pressure: Float = 1.0f) {
        saveSnapshotForUndo()
        val startPoint = DrawingPoint(normalizedX, normalizedY, pressure)
        val stroke = DrawingStroke(
            id = UUID.randomUUID().toString(),
            points = listOf(startPoint),
            colorArgb = _selectedColor.value.toArgb(),
            strokeWidth = _strokeWidth.value,
            brushType = _selectedBrushType.value,
            authorId = syncManager.myDeviceId,
            alpha = _strokeAlpha.value,
            modifier = _selectedModifier.value
        )
        _currentDraftStroke.value = stroke

        if (stroke.brushType == BrushType.ERASER) {
            applyEraserPoint(normalizedX, normalizedY, _strokeWidth.value)
        }
    }

    fun continueDrawing(normalizedX: Float, normalizedY: Float, pressure: Float = 1.0f) {
        val current = _currentDraftStroke.value ?: return
        val lastPt = current.points.lastOrNull()
        if (lastPt != null) {
            val dx = normalizedX - lastPt.x
            val dy = normalizedY - lastPt.y
            // Discard negligible micro-jitter delta (< 1.5px on 1080p screen) for cleaner paths
            if (dx * dx + dy * dy < 0.000002f) {
                return
            }
        }
        val newPoint = DrawingPoint(normalizedX, normalizedY, pressure)
        val updatedPoints = current.points + newPoint
        val updatedStroke = current.copy(points = updatedPoints)
        _currentDraftStroke.value = updatedStroke

        if (current.brushType == BrushType.ERASER) {
            applyEraserPoint(normalizedX, normalizedY, _strokeWidth.value)
        }
    }

    fun finishDrawing() {
        val current = _currentDraftStroke.value ?: return
        val isEraser = current.brushType == BrushType.ERASER
        val finalStroke = current.copy(authorId = syncManager.myDeviceId)
        if (!isEraser) {
            _strokes.value = _strokes.value + finalStroke
        } else {
            applyEraserPoints(finalStroke.points, finalStroke.strokeWidth)
        }
        _currentDraftStroke.value = null

        val myStrokes = _strokes.value.filter { it.authorId == syncManager.myDeviceId }
        val myStickers = _placedStickers.value.filter { it.authorId == syncManager.myDeviceId }

        syncManager.broadcastCanvasChange(
            finishedStroke = if (!isEraser) finalStroke else null,
            myStrokes = myStrokes,
            myStickers = myStickers,
            wallpaperTheme = _lockscreenConfig.value.wallpaperTheme
        )
        redoHistory.clear()
        persistCurrentState()
        PartnerDrawingWidgetProvider.updateAllWidgets(application)
    }

    private fun distanceSqToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax
        val dy = by - ay
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-7f) {
            val dpx = px - ax
            val dpy = py - ay
            return dpx * dpx + dpy * dpy
        }
        val t = ((px - ax) * dx + (py - ay) * dy) / lenSq
        val clampedT = t.coerceIn(0f, 1f)
        val projX = ax + clampedT * dx
        val projY = ay + clampedT * dy
        val dpx = px - projX
        val dpy = py - projY
        return dpx * dpx + dpy * dpy
    }

    private fun applyEraserPoint(x: Float, y: Float, eraserStrokeWidth: Float) {
        val eraserRadius = (eraserStrokeWidth / 450f).coerceIn(0.035f, 0.16f)

        _strokes.value = _strokes.value.filterNot { stroke ->
            val strokeRadius = (stroke.strokeWidth / 900f).coerceIn(0.008f, 0.05f)
            val combinedRadius = eraserRadius + strokeRadius
            val combinedRadiusSq = combinedRadius * combinedRadius

            if (stroke.points.isEmpty()) {
                false
            } else if (stroke.points.size == 1) {
                val p = stroke.points[0]
                val dx = p.x - x
                val dy = p.y - y
                (dx * dx + dy * dy) <= combinedRadiusSq
            } else {
                var intersects = false
                for (i in 1 until stroke.points.size) {
                    val p1 = stroke.points[i - 1]
                    val p2 = stroke.points[i]
                    if (distanceSqToSegment(x, y, p1.x, p1.y, p2.x, p2.y) <= combinedRadiusSq) {
                        intersects = true
                        break
                    }
                }
                intersects
            }
        }

        val stickerHitRadius = eraserRadius + 0.06f
        val stickerHitRadiusSq = stickerHitRadius * stickerHitRadius
        _placedStickers.value = _placedStickers.value.filterNot { st ->
            val dx = st.x - x
            val dy = st.y - y
            (dx * dx + dy * dy) <= stickerHitRadiusSq
        }
    }

    private fun applyEraserPoints(points: List<DrawingPoint>, strokeWidth: Float) {
        if (points.isEmpty()) return
        points.forEach { pt ->
            applyEraserPoint(pt.x, pt.y, strokeWidth)
        }
    }

    fun selectBrush(brushType: BrushType) {
        _selectedBrushType.value = brushType
        val supported = brushType.getSupportedModifiers()
        if (_selectedModifier.value !in supported) {
            _selectedModifier.value = com.example.data.model.StrokeModifier.NONE
        }
    }

    fun selectModifier(modifier: com.example.data.model.StrokeModifier) {
        _selectedModifier.value = modifier
    }

    fun selectColor(color: Color) {
        _selectedColor.value = color
    }

    fun setStrokeWidth(width: Float) {
        _strokeWidth.value = width.coerceIn(2f, 70f)
    }

    fun setStrokeAlpha(alpha: Float) {
        _strokeAlpha.value = alpha.coerceIn(0.1f, 1.0f)
    }

    fun addSticker(emojiOrText: String, x: Float = 0.5f, y: Float = 0.45f) {
        saveSnapshotForUndo()
        val sticker = PlacedSticker(
            id = UUID.randomUUID().toString(),
            content = emojiOrText,
            x = x,
            y = y,
            scale = 1.0f,
            rotation = 0f,
            authorId = syncManager.myDeviceId
        )
        _placedStickers.value = _placedStickers.value + sticker
        _selectedStickerId.value = sticker.id

        val myStrokes = _strokes.value.filter { it.authorId == syncManager.myDeviceId }
        val myStickers = _placedStickers.value.filter { it.authorId == syncManager.myDeviceId }
        syncManager.broadcastCanvasChange(
            finishedStroke = null,
            myStrokes = myStrokes,
            myStickers = myStickers,
            wallpaperTheme = _lockscreenConfig.value.wallpaperTheme
        )
        persistCurrentState()
        PartnerDrawingWidgetProvider.updateAllWidgets(application)
    }

    private var stickerSyncJob: Job? = null

    private fun scheduleStickerSync() {
        stickerSyncJob?.cancel()
        stickerSyncJob = scope.launch {
            delay(150)
            val myStrokes = _strokes.value.filter { it.authorId == syncManager.myDeviceId }
            val myStickers = _placedStickers.value.filter { it.authorId == syncManager.myDeviceId }
            syncManager.broadcastCanvasChange(
                finishedStroke = null,
                myStrokes = myStrokes,
                myStickers = myStickers,
                wallpaperTheme = _lockscreenConfig.value.wallpaperTheme
            )
            persistCurrentState()
            PartnerDrawingWidgetProvider.updateAllWidgets(application)
        }
    }

    fun moveStickerDelta(stickerId: String, deltaX: Float, deltaY: Float) {
        val updated = _placedStickers.value.map { st ->
            if (st.id == stickerId) {
                st.copy(
                    x = (st.x + deltaX).coerceIn(0.04f, 0.96f),
                    y = (st.y + deltaY).coerceIn(0.04f, 0.96f)
                )
            } else st
        }
        _placedStickers.value = updated
        scheduleStickerSync()
    }

    fun updateStickerPosition(stickerId: String, newX: Float, newY: Float) {
        val updated = _placedStickers.value.map { st ->
            if (st.id == stickerId) st.copy(x = newX.coerceIn(0.04f, 0.96f), y = newY.coerceIn(0.04f, 0.96f)) else st
        }
        _placedStickers.value = updated
        scheduleStickerSync()
    }

    fun updateStickerTransform(stickerId: String, scaleDelta: Float, rotationDelta: Float) {
        val updated = _placedStickers.value.map { st ->
            if (st.id == stickerId) {
                st.copy(
                    scale = (st.scale * scaleDelta).coerceIn(0.4f, 3.5f),
                    rotation = (st.rotation + rotationDelta) % 360f
                )
            } else st
        }
        _placedStickers.value = updated
        scheduleStickerSync()
    }

    fun selectSticker(id: String?) {
        _selectedStickerId.value = id
    }

    fun deleteSticker(id: String) {
        saveSnapshotForUndo()
        _placedStickers.value = _placedStickers.value.filter { it.id != id }
        if (_selectedStickerId.value == id) _selectedStickerId.value = null
        val myStrokes = _strokes.value.filter { it.authorId == syncManager.myDeviceId }
        val myStickers = _placedStickers.value.filter { it.authorId == syncManager.myDeviceId }
        syncManager.broadcastCanvasChange(
            finishedStroke = null,
            myStrokes = myStrokes,
            myStickers = myStickers,
            wallpaperTheme = _lockscreenConfig.value.wallpaperTheme
        )
        persistCurrentState()
        PartnerDrawingWidgetProvider.updateAllWidgets(application)
    }

    fun clearAllCanvas() {
        if (_strokes.value.isEmpty() && _placedStickers.value.isEmpty()) return
        saveSnapshotForUndo()
        // Clear all strokes and stickers across all layers
        _strokes.value = emptyList()
        _placedStickers.value = emptyList()
        _selectedStickerId.value = null
        _partnerDraftStroke.value = null
        syncManager.broadcastAction(SyncAction.ClearAll)
        persistCurrentState()
        PartnerDrawingWidgetProvider.updateAllWidgets(application)
    }

    fun undo() {
        if (undoHistory.isNotEmpty()) {
            val lastState = undoHistory.removeAt(undoHistory.size - 1)
            redoHistory.add(CanvasSnapshot(_strokes.value, _placedStickers.value))
            _strokes.value = lastState.strokes
            _placedStickers.value = lastState.stickers
            val myStrokes = _strokes.value.filter { it.authorId == syncManager.myDeviceId }
            val myStickers = _placedStickers.value.filter { it.authorId == syncManager.myDeviceId }
            syncManager.broadcastCanvasChange(
                finishedStroke = null,
                myStrokes = myStrokes,
                myStickers = myStickers,
                wallpaperTheme = _lockscreenConfig.value.wallpaperTheme
            )
            persistCurrentState()
            PartnerDrawingWidgetProvider.updateAllWidgets(application)
        } else if (_strokes.value.isNotEmpty() || _placedStickers.value.isNotEmpty()) {
            redoHistory.add(CanvasSnapshot(_strokes.value, _placedStickers.value))
            val myStrokes = _strokes.value.filter { it.authorId == syncManager.myDeviceId }
            if (myStrokes.isNotEmpty()) {
                val lastId = myStrokes.last().id
                _strokes.value = _strokes.value.filter { it.id != lastId }
            } else if (_placedStickers.value.any { it.authorId == syncManager.myDeviceId }) {
                val myStickersList = _placedStickers.value.filter { it.authorId == syncManager.myDeviceId }
                val lastStickerId = myStickersList.last().id
                _placedStickers.value = _placedStickers.value.filter { it.id != lastStickerId }
            } else if (_strokes.value.isNotEmpty()) {
                _strokes.value = _strokes.value.dropLast(1)
            } else if (_placedStickers.value.isNotEmpty()) {
                _placedStickers.value = _placedStickers.value.dropLast(1)
            }
            val remainingMyStrokes = _strokes.value.filter { it.authorId == syncManager.myDeviceId }
            val remainingMyStickers = _placedStickers.value.filter { it.authorId == syncManager.myDeviceId }
            syncManager.broadcastCanvasChange(
                finishedStroke = null,
                myStrokes = remainingMyStrokes,
                myStickers = remainingMyStickers,
                wallpaperTheme = _lockscreenConfig.value.wallpaperTheme
            )
            persistCurrentState()
            PartnerDrawingWidgetProvider.updateAllWidgets(application)
        }
    }

    fun redo() {
        if (redoHistory.isNotEmpty()) {
            val nextState = redoHistory.removeAt(redoHistory.size - 1)
            undoHistory.add(CanvasSnapshot(_strokes.value, _placedStickers.value))
            _strokes.value = nextState.strokes
            _placedStickers.value = nextState.stickers
            persistCurrentState()
            PartnerDrawingWidgetProvider.updateAllWidgets(application)
        }
    }

    private fun saveSnapshotForUndo() {
        if (undoHistory.size > 20) undoHistory.removeAt(0)
        undoHistory.add(CanvasSnapshot(_strokes.value, _placedStickers.value))
    }

    fun setWallpaperTheme(theme: WallpaperTheme) {
        _lockscreenConfig.value = _lockscreenConfig.value.copy(
            wallpaperTheme = theme,
            customWallpaperUri = null
        )
        syncManager.broadcastAction(SyncAction.ChangeWallpaper(theme, null))
        persistCurrentState()
        PartnerDrawingWidgetProvider.updateAllWidgets(application)
    }

    fun setCustomWallpaperUri(uriString: String) {
        _lockscreenConfig.value = _lockscreenConfig.value.copy(
            customWallpaperUri = uriString
        )
        syncManager.broadcastAction(SyncAction.ChangeWallpaper(_lockscreenConfig.value.wallpaperTheme, uriString))
        persistCurrentState()
        PartnerDrawingWidgetProvider.updateAllWidgets(application)
    }

    fun sendHeartReaction(emoji: String = "💖", x: Float = 0.5f, y: Float = 0.5f) {
        val reaction = FloatingHeartReaction(
            x = x,
            y = y,
            emoji = emoji,
            sender = "Tu"
        )
        _floatingReactions.value = _floatingReactions.value + reaction
        syncManager.broadcastAction(
            SyncAction.HeartPing(
                x = x,
                y = y,
                emoji = emoji,
                sender = syncManager.myDeviceId
            )
        )
        scope.launch {
            kotlinx.coroutines.delay(3500)
            _floatingReactions.value = _floatingReactions.value.filter { it.id != reaction.id }
        }
    }

    fun connectToRoomCode(newCode: String) {
        val code = newCode.trim().uppercase()
        if (code.isBlank()) return
        syncManager.connectToRoom(code, markAsMatched = true)
        loadPersistedSession(code)
    }

    fun unmatchPartner() {
        syncManager.unmatchAndDisconnect()
    }

    fun triggerPartnerSimulatedDraw(type: String = "heart") {
        syncManager.triggerPartnerSimulatedDraw(type)
    }

    fun triggerPartnerSimulatedSticker() {
        syncManager.triggerPartnerSimulatedSticker()
    }

    private fun persistCurrentState() {
        val code = syncManager.currentRoomCode.value
        val config = _lockscreenConfig.value
        val strokesToSave = _strokes.value
        val stickersToSave = _placedStickers.value

        scope.launch(Dispatchers.IO) {
            try {
                val sessionEntity = DrawingSessionEntity(
                    sessionCode = code,
                    title = "LockDraw-$code",
                    wallpaperThemeName = config.wallpaperTheme.name,
                    customWallpaperUri = config.customWallpaperUri
                )

                val strokeEntities = strokesToSave.mapIndexed { idx, s ->
                    val ptsArr = JSONArray()
                    s.points.forEach { p ->
                        val obj = JSONObject()
                        obj.put("x", p.x.toDouble())
                        obj.put("y", p.y.toDouble())
                        obj.put("pressure", p.pressure.toDouble())
                        ptsArr.put(obj)
                    }
                    SavedStrokeEntity(
                        id = s.id,
                        sessionCode = code,
                        pointsJson = ptsArr.toString(),
                        colorArgb = s.colorArgb,
                        strokeWidth = s.strokeWidth,
                        brushTypeName = s.brushType.name,
                        alpha = s.alpha,
                        authorId = s.authorId,
                        orderIndex = idx.toLong(),
                        modifierName = s.modifier.name
                    )
                }

                val stickerEntities = stickersToSave.mapIndexed { idx, st ->
                    SavedStickerEntity(
                        id = st.id,
                        sessionCode = code,
                        content = st.content,
                        x = st.x,
                        y = st.y,
                        scale = st.scale,
                        rotation = st.rotation,
                        authorId = st.authorId,
                        orderIndex = idx.toLong()
                    )
                }

                dao.saveFullSessionState(sessionEntity, strokeEntities, stickerEntities)
            } catch (e: Exception) {
                // Ignore DB write errors gracefully
            }
        }
    }

    private fun loadPersistedSession(code: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val session = dao.getSession(code)
                if (session != null) {
                    val theme = try {
                        WallpaperTheme.valueOf(session.wallpaperThemeName)
                    } catch (e: Exception) {
                        WallpaperTheme.FROSTED_GLASS
                    }
                    _lockscreenConfig.value = _lockscreenConfig.value.copy(
                        wallpaperTheme = theme,
                        customWallpaperUri = session.customWallpaperUri
                    )

                    val savedStrokes = dao.getStrokesForSession(code)
                    val loadedStrokes = savedStrokes.map { ent ->
                        val pts = mutableListOf<DrawingPoint>()
                        try {
                            val arr = JSONArray(ent.pointsJson)
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                pts.add(
                                    DrawingPoint(
                                        x = o.getDouble("x").toFloat(),
                                        y = o.getDouble("y").toFloat(),
                                        pressure = o.optDouble("pressure", 1.0).toFloat()
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                        DrawingStroke(
                            id = ent.id,
                            points = pts,
                            colorArgb = ent.colorArgb,
                            strokeWidth = ent.strokeWidth,
                            brushType = try { BrushType.valueOf(ent.brushTypeName) } catch (e: Exception) { BrushType.PEN },
                            authorId = ent.authorId,
                            alpha = ent.alpha,
                            modifier = try { com.example.data.model.StrokeModifier.valueOf(ent.modifierName) } catch (e: Exception) { com.example.data.model.StrokeModifier.NONE }
                        )
                    }

                    val savedStickers = dao.getStickersForSession(code)
                    val loadedStickers = savedStickers.map { st ->
                        PlacedSticker(
                            id = st.id,
                            content = st.content,
                            x = st.x,
                            y = st.y,
                            scale = st.scale,
                            rotation = st.rotation,
                            authorId = st.authorId
                        )
                    }

                    _strokes.value = loadedStrokes
                    _placedStickers.value = loadedStickers
                }
            } catch (e: Exception) {
                // Ignore load error
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DrawingRepository? = null

        fun getInstance(application: Application): DrawingRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DrawingRepository(application)
                INSTANCE = instance
                instance
            }
        }
    }
}
