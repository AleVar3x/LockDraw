package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.model.BrushType
import com.example.data.model.DrawingPoint
import com.example.data.model.DrawingStroke
import com.example.data.model.PlacedSticker
import com.example.data.model.WallpaperTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

enum class ConnectionStatus {
    OFFLINE,
    CONNECTING,
    CONNECTED,
    SIMULATING_PARTNER
}

data class PartnerPresence(
    val isOnline: Boolean = false,
    val isDrawing: Boolean = false,
    val partnerName: String = "Partner",
    val cursorX: Float = -1f,
    val cursorY: Float = -1f,
    val lastActiveMillis: Long = System.currentTimeMillis(),
    val activeReaction: String? = null
)

class PartnerSyncManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val prefs = context.getSharedPreferences("lockdraw_sync_prefs", Context.MODE_PRIVATE)

    val myDeviceId: String = prefs.getString("device_id", null) ?: run {
        val newId = "dev_" + UUID.randomUUID().toString().take(8)
        prefs.edit().putString("device_id", newId).apply()
        newId
    }

    // Persisted Match Status
    private val _isMatched = MutableStateFlow(prefs.getBoolean("is_matched", false))
    val isMatched: StateFlow<Boolean> = _isMatched.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.OFFLINE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _currentRoomCode = MutableStateFlow(prefs.getString("saved_room_code", "LOVE-779") ?: "LOVE-779")
    val currentRoomCode: StateFlow<String> = _currentRoomCode.asStateFlow()

    private val _partnerPresence = MutableStateFlow(PartnerPresence())
    val partnerPresence: StateFlow<PartnerPresence> = _partnerPresence.asStateFlow()

    private val _incomingActions = MutableSharedFlow<SyncAction>(
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingActions: SharedFlow<SyncAction> = _incomingActions.asSharedFlow()

    // Deduplication of received action IDs (to avoid replaying self actions or duplicates)
    private val processedActionIds = ConcurrentHashMap.newKeySet<String>()

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep WebSocket open indefinitely
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var simulationJob: Job? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    init {
        // Automatically connect to saved room code on launch
        val savedCode = _currentRoomCode.value
        connectToRoom(savedCode, markAsMatched = _isMatched.value)
    }

    /**
     * Connect and pair with a room.
     */
    fun connectToRoom(code: String, markAsMatched: Boolean = true) {
        val cleanCode = code.trim().uppercase().ifBlank { "LOVE-779" }
        _currentRoomCode.value = cleanCode
        _isMatched.value = markAsMatched

        prefs.edit()
            .putString("saved_room_code", cleanCode)
            .putBoolean("is_matched", markAsMatched)
            .apply()

        disconnectWebSocket()
        _connectionStatus.value = ConnectionStatus.CONNECTING

        scope.launch(Dispatchers.IO) {
            // First fetch missed messages/snapshot via HTTP history
            fetchHistoryAndCatchUp(cleanCode)
            // Connect live WebSocket stream
            startWebSocketConnection(cleanCode)
            startHeartbeat()
        }
    }

    /**
     * Disconnect and unmatch explicitly (only when user taps "Scollega Partner").
     */
    fun unmatchAndDisconnect() {
        _isMatched.value = false
        prefs.edit().putBoolean("is_matched", false).apply()
        disconnectWebSocket()
        _partnerPresence.value = PartnerPresence(isOnline = false)
        _connectionStatus.value = ConnectionStatus.OFFLINE
    }

    /**
     * Fetch recent messages from ntfy topic JSON stream (catch-up if app was closed).
     */
    private fun fetchHistoryAndCatchUp(roomCode: String) {
        val topic = getTopicForRoom(roomCode)
        // ntfy.sh JSON stream endpoint with poll=1 & since=12h retrieves missed events
        val pollUrl = "https://ntfy.sh/$topic/json?poll=1&since=12h"
        try {
            val request = Request.Builder().url(pollUrl).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    bodyString.lineSequence().forEach { line ->
                        if (line.isNotBlank()) {
                            try {
                                val ntfyMsg = JSONObject(line)
                                if (ntfyMsg.optString("event") == "message") {
                                    val innerMessage = ntfyMsg.optString("message")
                                    val msgId = ntfyMsg.optString("id")
                                    if (innerMessage.isNotBlank() && (msgId.isBlank() || processedActionIds.add(msgId))) {
                                        val action = SyncActionSerializer.fromJson(innerMessage)
                                        if (action != null && !isActionFromSelf(action)) {
                                            handleIncomingAction(action)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Skip individual corrupt lines
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("PartnerSync", "Catch-up poll error (non-fatal): ${e.message}")
        }
    }

    private fun getTopicForRoom(roomCode: String): String {
        return "lockdraw_sync_${roomCode.lowercase().replace("[^a-z0-9]".toRegex(), "")}"
    }

    private fun startWebSocketConnection(roomCode: String) {
        disconnectWebSocket()
        val topic = getTopicForRoom(roomCode)
        // Connect to ntfy WebSocket with since=all to ensure no events are dropped while connecting
        val wsUrl = "wss://ntfy.sh/$topic/ws"
        Log.d("PartnerSync", "Connecting to live topic: $wsUrl (Device: $myDeviceId)")

        try {
            val request = Request.Builder().url(wsUrl).build()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    _partnerPresence.value = _partnerPresence.value.copy(isOnline = true)
                    Log.d("PartnerSync", "Live WebSocket connected on topic: $topic")

                    // Announce presence & ask for initial board snapshot from partner
                    broadcastAction(SyncAction.RequestSnapshot(requesterId = myDeviceId))
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    try {
                        val ntfyMsg = JSONObject(text)
                        val event = ntfyMsg.optString("event")
                        if (event == "message") {
                            val innerMessage = ntfyMsg.optString("message")
                            val msgId = ntfyMsg.optString("id")
                            if (innerMessage.isNotBlank()) {
                                if (msgId.isNotBlank() && !processedActionIds.add(msgId)) {
                                    return // Already processed
                                }
                                val action = SyncActionSerializer.fromJson(innerMessage)
                                if (action != null && !isActionFromSelf(action)) {
                                    scope.launch(Dispatchers.Main.immediate) {
                                        handleIncomingAction(action)
                                    }
                                }
                            }
                        } else if (event == "keepalive") {
                            _partnerPresence.value = _partnerPresence.value.copy(isOnline = true)
                        }
                    } catch (e: Exception) {
                        Log.e("PartnerSync", "Parse error for WS msg: $text", e)
                    }
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Log.d("PartnerSync", "WebSocket closed: $reason")
                    _connectionStatus.value = ConnectionStatus.OFFLINE
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.w("PartnerSync", "WebSocket failure, will reconnect: ${t.message}")
                    _connectionStatus.value = ConnectionStatus.CONNECTING
                    scheduleReconnect(roomCode)
                }
            })
        } catch (e: Exception) {
            Log.e("PartnerSync", "Failed to initialize WebSocket", e)
            scheduleReconnect(roomCode)
        }
    }

    private fun isActionFromSelf(action: SyncAction): Boolean {
        return when (action) {
            is SyncAction.StrokeBegin -> action.authorId == myDeviceId
            is SyncAction.StrokePoints -> action.authorId == myDeviceId
            is SyncAction.StrokeFinished -> action.stroke.authorId == myDeviceId
            is SyncAction.PlaceSticker -> action.sticker.authorId == myDeviceId
            is SyncAction.UpdateSticker -> action.sticker.authorId == myDeviceId
            is SyncAction.RemoveSticker -> action.authorId == myDeviceId
            is SyncAction.Undo -> action.authorId == myDeviceId
            is SyncAction.HeartPing -> action.sender == myDeviceId
            is SyncAction.CursorMove -> action.sender == myDeviceId
            is SyncAction.RequestSnapshot -> action.requesterId == myDeviceId
            is SyncAction.FullSnapshot -> action.authorId == myDeviceId
            is SyncAction.ClearAll, is SyncAction.ChangeWallpaper -> false
        }
    }

    private fun scheduleReconnect(roomCode: String) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(2500)
            if (isActive && _connectionStatus.value != ConnectionStatus.CONNECTED) {
                startWebSocketConnection(roomCode)
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(15000)
                if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                    _partnerPresence.value = _partnerPresence.value.copy(
                        isOnline = true,
                        lastActiveMillis = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    private fun handleIncomingAction(action: SyncAction) {
        when (action) {
            is SyncAction.CursorMove -> {
                _partnerPresence.value = _partnerPresence.value.copy(
                    isOnline = true,
                    cursorX = action.x,
                    cursorY = action.y,
                    lastActiveMillis = System.currentTimeMillis()
                )
            }
            is SyncAction.HeartPing -> {
                _partnerPresence.value = _partnerPresence.value.copy(
                    isOnline = true,
                    activeReaction = action.emoji,
                    lastActiveMillis = System.currentTimeMillis()
                )
                scope.launch {
                    delay(3000)
                    _partnerPresence.value = _partnerPresence.value.copy(activeReaction = null)
                }
            }
            is SyncAction.StrokeBegin, is SyncAction.StrokePoints -> {
                _partnerPresence.value = _partnerPresence.value.copy(
                    isOnline = true,
                    isDrawing = true,
                    lastActiveMillis = System.currentTimeMillis()
                )
            }
            is SyncAction.StrokeFinished -> {
                _partnerPresence.value = _partnerPresence.value.copy(
                    isOnline = true,
                    isDrawing = false,
                    lastActiveMillis = System.currentTimeMillis()
                )
            }
            is SyncAction.FullSnapshot, is SyncAction.PlaceSticker -> {
                _partnerPresence.value = _partnerPresence.value.copy(
                    isOnline = true,
                    lastActiveMillis = System.currentTimeMillis()
                )
            }
            else -> {}
        }
        _incomingActions.tryEmit(action)
    }

    fun broadcastAction(action: SyncAction) {
        val topic = getTopicForRoom(_currentRoomCode.value)
        val postUrl = "https://ntfy.sh/$topic"

        scope.launch(Dispatchers.IO) {
            try {
                val json = SyncActionSerializer.toJson(action)
                val body = json.toRequestBody("text/plain; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(postUrl)
                    .post(body)
                    // High priority for instant push
                    .header("Priority", "high")
                    .header("Title", "LockDraw")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w("PartnerSync", "HTTP broadcast response code: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("PartnerSync", "Failed to broadcast action", e)
            }
        }
    }

    /**
     * Interactive Partner Simulation: allows user or tester to test real-time partner drawing.
     */
    fun triggerPartnerSimulatedDraw(type: String = "heart") {
        simulationJob?.cancel()
        simulationJob = scope.launch(Dispatchers.Default) {
            _partnerPresence.value = _partnerPresence.value.copy(
                isDrawing = true,
                isOnline = true,
                lastActiveMillis = System.currentTimeMillis()
            )

            val strokeId = "partner_sim_${System.currentTimeMillis()}"
            val color = when (type) {
                "neon" -> 0xFF00F0FF.toInt()
                "rainbow" -> 0xFFFF007F.toInt()
                "pencil" -> 0xFFFFD700.toInt()
                else -> 0xFFFF2A6D.toInt()
            }
            val brush = when (type) {
                "neon" -> BrushType.NEON
                "rainbow" -> BrushType.RAINBOW
                "pencil" -> BrushType.PENCIL
                else -> BrushType.PEN
            }

            val strokePoints = mutableListOf<DrawingPoint>()
            val steps = 28
            val centerX = 0.5f + (Random.nextFloat() * 0.1f - 0.05f)
            val centerY = 0.45f + (Random.nextFloat() * 0.1f - 0.05f)
            val scale = 0.015f

            // Start Stroke
            val firstPoint = DrawingPoint(x = centerX, y = centerY - 8 * scale, pressure = 1.0f)
            strokePoints.add(firstPoint)
            _incomingActions.emit(
                SyncAction.StrokeBegin(
                    strokeId = strokeId,
                    x = firstPoint.x,
                    y = firstPoint.y,
                    colorArgb = color,
                    strokeWidth = 14f,
                    brushType = brush,
                    alpha = 1.0f,
                    authorId = "partner"
                )
            )

            // Generate parametric heart curve
            for (i in 0..steps) {
                val t = (i.toFloat() / steps) * (2 * Math.PI)
                val hx = centerX + (16 * Math.pow(Math.sin(t), 3.0)).toFloat() * scale
                val hy = centerY - (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)).toFloat() * scale

                val point = DrawingPoint(x = hx, y = hy, pressure = 1.0f)
                strokePoints.add(point)

                _partnerPresence.value = _partnerPresence.value.copy(
                    cursorX = hx,
                    cursorY = hy,
                    isDrawing = true
                )

                _incomingActions.emit(
                    SyncAction.StrokePoints(
                        strokeId = strokeId,
                        points = strokePoints.toList(),
                        authorId = "partner"
                    )
                )
                delay(30)
            }

            val finishedStroke = DrawingStroke(
                id = strokeId,
                points = strokePoints,
                colorArgb = color,
                strokeWidth = 14f,
                brushType = brush,
                authorId = "partner",
                alpha = 1.0f
            )
            _incomingActions.emit(SyncAction.StrokeFinished(finishedStroke))

            delay(150)
            _incomingActions.emit(
                SyncAction.HeartPing(
                    x = centerX,
                    y = centerY,
                    emoji = "💖",
                    sender = "Partner"
                )
            )

            _partnerPresence.value = _partnerPresence.value.copy(
                isDrawing = false,
                cursorX = -1f,
                cursorY = -1f
            )
        }
    }

    fun triggerPartnerSimulatedSticker() {
        scope.launch(Dispatchers.Default) {
            val emojis = listOf("💖", "💌", "🥰", "✨", "🌸", "🧸", "🌹", "🍓")
            val selected = emojis.random()
            val sticker = PlacedSticker(
                id = "partner_stk_${System.currentTimeMillis()}",
                content = selected,
                x = 0.25f + Random.nextFloat() * 0.5f,
                y = 0.25f + Random.nextFloat() * 0.5f,
                scale = 1.2f,
                rotation = (Random.nextFloat() * 20f) - 10f,
                authorId = "partner"
            )
            _incomingActions.emit(SyncAction.PlaceSticker(sticker))
            _incomingActions.emit(
                SyncAction.HeartPing(
                    x = sticker.x,
                    y = sticker.y,
                    emoji = selected,
                    sender = "Partner"
                )
            )
        }
    }

    private fun disconnectWebSocket() {
        try {
            webSocket?.close(1000, "Room change")
            webSocket = null
        } catch (e: Exception) {
            // ignore
        }
    }

    fun cleanup() {
        simulationJob?.cancel()
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        disconnectWebSocket()
    }
}
