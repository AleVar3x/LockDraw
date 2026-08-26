package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.model.BrushType
import com.example.data.model.DrawingPoint
import com.example.data.model.DrawingStroke
import com.example.data.model.PlacedSticker
import com.example.data.model.WallpaperTheme
import com.example.util.NotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections
import java.util.Date
import java.util.LinkedHashSet
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
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

    companion object {
        private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val TWO_WEEKS_MILLIS = 14L * 24 * 60 * 60 * 1000L // 2 weeks (1,209,600,000 ms)

        fun generateRandomRoomCode(length: Int = 16): String {
            return (1..length)
                .map { CODE_CHARS.random() }
                .joinToString("")
        }
    }

    val myDeviceId: String = prefs.getString("device_id", null) ?: run {
        val newId = "dev_" + UUID.randomUUID().toString().take(8)
        prefs.edit().putString("device_id", newId).apply()
        newId
    }

    // User's custom display name and partner nickname override
    private val _myName = MutableStateFlow(prefs.getString("my_name", "Io") ?: "Io")
    val myName: StateFlow<String> = _myName.asStateFlow()

    private val _partnerCustomName = MutableStateFlow(prefs.getString("partner_custom_name", "") ?: "")
    val partnerCustomName: StateFlow<String> = _partnerCustomName.asStateFlow()

    // Persisted Match Status (False on first launch until matched by user or partner)
    private val _isMatched = MutableStateFlow(prefs.getBoolean("is_matched", false))
    val isMatched: StateFlow<Boolean> = _isMatched.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.OFFLINE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    // 16-character alphanumeric room code generated on first launch if not already saved
    private val _currentRoomCode = MutableStateFlow(
        prefs.getString("saved_room_code", null) ?: run {
            val generated = generateRandomRoomCode(16)
            prefs.edit().putString("saved_room_code", generated).apply()
            generated
        }
    )
    val currentRoomCode: StateFlow<String> = _currentRoomCode.asStateFlow()

    private val _partnerPresence = MutableStateFlow(
        PartnerPresence(
            partnerName = prefs.getString("partner_custom_name", "")?.ifBlank { "Partner" } ?: "Partner"
        )
    )
    val partnerPresence: StateFlow<PartnerPresence> = _partnerPresence.asStateFlow()

    private val _incomingActions = MutableSharedFlow<SyncAction>(
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingActions: SharedFlow<SyncAction> = _incomingActions.asSharedFlow()

    // Deduplication of received action IDs (bounded to prevent memory bloat over long sessions)
    private val processedActionIds = Collections.synchronizedSet(LinkedHashSet<String>())
    private var lastSnapshotTimestamp: Long = System.currentTimeMillis()
    private var lastProcessedCanvasSignature: String? = null
    private val sessionStartTime: Long = System.currentTimeMillis()

    private var firestoreListener: ListenerRegistration? = null
    private var watchdogJob: Job? = null
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var simulationJob: Job? = null
    private var draftThrottleJob: Job? = null
    private var pendingDraftAction: SyncAction? = null

    private fun recordProcessedAction(actionId: String): Boolean {
        synchronized(processedActionIds) {
            if (processedActionIds.contains(actionId)) return false
            if (processedActionIds.size > 150) {
                val oldest = processedActionIds.firstOrNull()
                if (oldest != null) {
                    processedActionIds.remove(oldest)
                }
            }
            processedActionIds.add(actionId)
            return true
        }
    }

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    private var lastNotifiedJoinTimestamp: Long = prefs.getLong("last_notified_join_ts", 0L)

    private val firestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("PartnerSync", "Error getting Firestore instance", e)
            null
        }
    }

    init {
        val savedCode = _currentRoomCode.value
        connectToRoom(savedCode, markAsMatched = _isMatched.value)
        purgeStaleRoomsIfAny()
    }

    fun setMyName(name: String) {
        val clean = name.trim().ifBlank { "Io" }
        _myName.value = clean
        prefs.edit().putString("my_name", clean).apply()
        broadcastMyPresenceName(clean)
    }

    fun setPartnerCustomName(name: String) {
        val clean = name.trim()
        _partnerCustomName.value = clean
        prefs.edit().putString("partner_custom_name", clean).apply()
        if (clean.isNotBlank()) {
            _partnerPresence.value = _partnerPresence.value.copy(partnerName = clean)
        }
    }

    private fun broadcastMyPresenceName(name: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val db = firestore ?: return@launch
                val roomCode = _currentRoomCode.value
                val now = System.currentTimeMillis()
                db.collection("rooms").document(roomCode).set(
                    mapOf(
                        "presence_names" to mapOf(myDeviceId to name),
                        "updatedAt" to now,
                        "expiresAt" to (now + TWO_WEEKS_MILLIS),
                        "ttl_timestamp" to Timestamp(Date(now + TWO_WEEKS_MILLIS))
                    ),
                    SetOptions.merge()
                )
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    /**
     * Connect and pair with a room in Firebase Firestore.
     * When connecting, broadcast join event so the other device receives an instant notification
     * and automatically updates its pairing state as well.
     */
    fun connectToRoom(code: String, markAsMatched: Boolean = true) {
        val cleanCode = code.trim().uppercase().ifBlank { generateRandomRoomCode(16) }
        _currentRoomCode.value = cleanCode
        _isMatched.value = markAsMatched

        prefs.edit()
            .putString("saved_room_code", cleanCode)
            .putBoolean("is_matched", markAsMatched)
            .apply()

        disconnectFirestore()
        _connectionStatus.value = ConnectionStatus.CONNECTING

        startFirestoreListener(cleanCode)
        startHeartbeat()
        startWatchdog()

        // Broadcast join event to the room
        scope.launch(Dispatchers.IO) {
            try {
                val db = firestore ?: return@launch
                val now = System.currentTimeMillis()
                val joinData = hashMapOf<String, Any>(
                    "presence" to mapOf(myDeviceId to now),
                    "presence_names" to mapOf(myDeviceId to _myName.value),
                    "last_join_device_id" to myDeviceId,
                    "last_join_name" to _myName.value,
                    "last_join_timestamp" to now,
                    "updatedAt" to now,
                    "expiresAt" to (now + TWO_WEEKS_MILLIS),
                    "ttl_timestamp" to Timestamp(Date(now + TWO_WEEKS_MILLIS))
                )
                db.collection("rooms").document(cleanCode).set(joinData, SetOptions.merge())
            } catch (e: Exception) {
                Log.w("PartnerSync", "Failed to broadcast join event", e)
            }
        }
    }

    /**
     * Disconnect and unmatch explicitly.
     */
    fun unmatchAndDisconnect() {
        _isMatched.value = false
        prefs.edit().putBoolean("is_matched", false).apply()
        disconnectFirestore()
        _partnerPresence.value = _partnerPresence.value.copy(isOnline = false)
        _connectionStatus.value = ConnectionStatus.OFFLINE
    }

    fun reconnect() {
        val code = _currentRoomCode.value
        connectToRoom(code, markAsMatched = _isMatched.value)
    }

    /**
     * Proactively verifies that the Firestore listener and gRPC socket are alive.
     * Called when the app resumes, on user interaction, or by the background watchdog.
     */
    fun ensureActiveConnection(forceRefresh: Boolean = false) {
        val roomCode = _currentRoomCode.value
        val now = System.currentTimeMillis()
        val isStale = (now - lastSnapshotTimestamp) > 30_000

        if (firestoreListener == null || forceRefresh || isStale) {
            val db = firestore ?: return
            val docRef = db.collection("rooms").document(roomCode)
            docRef.get().addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    lastSnapshotTimestamp = System.currentTimeMillis()
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    handleDocumentSnapshot(snapshot)
                }
            }.addOnFailureListener { e ->
                Log.w("PartnerSync", "ensureActiveConnection probe failed, triggering reconnect: ${e.message}")
                scheduleListenerReconnect(roomCode)
            }

            if (firestoreListener == null) {
                startFirestoreListener(roomCode)
            }
        }
    }

    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(25_000)
                val now = System.currentTimeMillis()
                val elapsedSinceLastSnapshot = now - lastSnapshotTimestamp
                val roomCode = _currentRoomCode.value

                // If no updates received for >45s or listener was dropped, probe Firestore directly
                if (elapsedSinceLastSnapshot > 45_000 || firestoreListener == null) {
                    val db = firestore ?: continue
                    val docRef = db.collection("rooms").document(roomCode)
                    docRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot != null && snapshot.exists()) {
                            lastSnapshotTimestamp = System.currentTimeMillis()
                            _connectionStatus.value = ConnectionStatus.CONNECTED
                            handleDocumentSnapshot(snapshot)
                        }
                    }.addOnFailureListener { e ->
                        Log.w("PartnerSync", "Watchdog probe failed: ${e.message}. Re-establishing stream...")
                        disconnectFirestore()
                        startFirestoreListener(roomCode)
                    }
                }
            }
        }
    }

    private fun startFirestoreListener(roomCode: String) {
        val db = firestore
        if (db == null) {
            Log.w("PartnerSync", "Firestore instance not available")
            _connectionStatus.value = ConnectionStatus.OFFLINE
            _lastSyncError.value = "Firestore non inizializzato o non disponibile"
            return
        }

        try {
            val docRef = db.collection("rooms").document(roomCode)

            // 1. Instant one-shot query to sync immediately without waiting for listener attachment delay
            docRef.get().addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    lastSnapshotTimestamp = System.currentTimeMillis()
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    handleDocumentSnapshot(snapshot)
                }
            }

            // 2. Realtime continuous stream listener
            firestoreListener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val errMsg = error.message ?: error.code.name
                    Log.w("PartnerSync", "Firestore snapshot error: $errMsg")
                    _lastSyncError.value = "Errore sync: $errMsg"
                    _connectionStatus.value = ConnectionStatus.CONNECTING
                    scheduleListenerReconnect(roomCode)
                    return@addSnapshotListener
                }

                _lastSyncError.value = null
                reconnectJob?.cancel()

                if (snapshot != null && snapshot.exists()) {
                    lastSnapshotTimestamp = System.currentTimeMillis()
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    handleDocumentSnapshot(snapshot)
                } else if (snapshot != null && !snapshot.exists()) {
                    // Initialize empty room doc
                    lastSnapshotTimestamp = System.currentTimeMillis()
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    initEmptyRoom(docRef)
                }
            }
        } catch (e: Exception) {
            Log.e("PartnerSync", "Failed to attach Firestore snapshot listener", e)
            _lastSyncError.value = "Errore connessione: ${e.localizedMessage}"
            _connectionStatus.value = ConnectionStatus.OFFLINE
            scheduleListenerReconnect(roomCode)
        }
    }

    private fun scheduleListenerReconnect(roomCode: String) {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(3500)
            if (isActive && _currentRoomCode.value == roomCode) {
                Log.i("PartnerSync", "Attempting automatic reconnection to room: $roomCode")
                disconnectFirestore()
                startFirestoreListener(roomCode)
            }
        }
    }

    private fun initEmptyRoom(docRef: DocumentReference) {
        val now = System.currentTimeMillis()
        val data = hashMapOf<String, Any>(
            "createdAt" to now,
            "updatedAt" to now,
            "expiresAt" to (now + TWO_WEEKS_MILLIS),
            "ttl_timestamp" to Timestamp(Date(now + TWO_WEEKS_MILLIS)),
            "strokes_json" to "[]",
            "stickers_json" to "[]",
            "wallpaperTheme" to "FROSTED_GLASS",
            "presence" to mapOf(myDeviceId to now),
            "presence_names" to mapOf(myDeviceId to _myName.value),
            "last_join_device_id" to myDeviceId,
            "last_join_name" to _myName.value,
            "last_join_timestamp" to now
        )
        docRef.set(data, SetOptions.merge())
    }

    private fun handleDocumentSnapshot(snapshot: DocumentSnapshot) {
        try {
            // Condition Firestore: If record hasn't been updated for > 2 weeks, delete it
            val updatedAt = snapshot.getLong("updatedAt") ?: snapshot.getLong("createdAt") ?: 0L
            val now = System.currentTimeMillis()
            if (updatedAt > 0 && (now - updatedAt > TWO_WEEKS_MILLIS)) {
                Log.i("PartnerSync", "Room record ${snapshot.id} has not been updated for > 2 weeks. Deleting Firestore record.")
                scope.launch(Dispatchers.IO) {
                    try {
                        snapshot.reference.delete()
                    } catch (e: Exception) {
                        Log.w("PartnerSync", "Failed to delete expired room doc", e)
                    }
                }
                return
            }

            // 1. Check partner presence & name
            val presenceMap = snapshot.get("presence") as? Map<*, *>
            val namesMap = snapshot.get("presence_names") as? Map<*, *>
            var partnerOnline = false
            var latestPartnerActive = 0L
            var remotePartnerName: String? = null
            var hasRemotePartnerDevice = false

            if (presenceMap != null) {
                for ((devId, timeVal) in presenceMap) {
                    val idStr = devId?.toString() ?: ""
                    val timeLong = (timeVal as? Number)?.toLong() ?: 0L
                    if (idStr != myDeviceId && idStr.isNotBlank()) {
                        hasRemotePartnerDevice = true
                        if (now - timeLong < 35_000) {
                            partnerOnline = true
                            if (timeLong > latestPartnerActive) {
                                latestPartnerActive = timeLong
                            }
                        }
                    }
                }
            }

            if (namesMap != null) {
                for ((devId, nameVal) in namesMap) {
                    val idStr = devId?.toString() ?: ""
                    val nameStr = nameVal?.toString() ?: ""
                    if (idStr != myDeviceId && nameStr.isNotBlank()) {
                        remotePartnerName = nameStr
                        hasRemotePartnerDevice = true
                    }
                }
            }

            val customOverride = _partnerCustomName.value
            val resolvedPartnerName = when {
                customOverride.isNotBlank() -> customOverride
                !remotePartnerName.isNullOrBlank() -> remotePartnerName
                else -> _partnerPresence.value.partnerName.ifBlank { "Partner" }
            }

            _partnerPresence.value = _partnerPresence.value.copy(
                isOnline = partnerOnline,
                partnerName = resolvedPartnerName,
                lastActiveMillis = if (latestPartnerActive > 0) latestPartnerActive else _partnerPresence.value.lastActiveMillis
            )

            // Auto-match if another device is present in the room
            if (hasRemotePartnerDevice && !_isMatched.value) {
                _isMatched.value = true
                prefs.edit().putBoolean("is_matched", true).apply()
            }

            // Partner Join Notification detection
            val lastJoinDeviceId = snapshot.getString("last_join_device_id")
            val lastJoinTime = snapshot.getLong("last_join_timestamp") ?: 0L
            val roomCode = snapshot.id

            if (lastJoinDeviceId != null && lastJoinDeviceId != myDeviceId && lastJoinTime > 0) {
                if (lastJoinTime > lastNotifiedJoinTimestamp) {
                    lastNotifiedJoinTimestamp = lastJoinTime
                    prefs.edit().putLong("last_notified_join_ts", lastJoinTime).apply()
                    _isMatched.value = true
                    prefs.edit().putBoolean("is_matched", true).apply()
                    NotificationHelper.showPartnerConnectedNotification(context, resolvedPartnerName, roomCode)
                }
            }

            // 2. Check draft stroke
            val draftSender = snapshot.getString("draft_sender")
            val draftJson = snapshot.getString("draft_json")
            if (!draftJson.isNullOrBlank() && draftSender != myDeviceId) {
                val action = SyncActionSerializer.fromJson(draftJson)
                if (action != null) {
                    handleIncomingAction(action)
                }
            } else if (draftJson.isNullOrBlank() && _partnerPresence.value.isDrawing && draftSender != myDeviceId) {
                _partnerPresence.value = _partnerPresence.value.copy(isDrawing = false)
            }

            // 3. Check discrete lastAction
            val actionId = snapshot.getString("last_action_id")
            val actionSender = snapshot.getString("last_action_sender")
            val actionJson = snapshot.getString("last_action_json")
            val actionTime = snapshot.getLong("updatedAt") ?: 0L

            if (!actionId.isNullOrBlank() && actionSender != myDeviceId) {
                // If action occurred before this app session started (>15s ago), record it as seen to avoid stale notifications
                val isStaleHistoricalAction = actionTime > 0 && (sessionStartTime - actionTime > 15_000L)
                if (recordProcessedAction(actionId)) {
                    if (!isStaleHistoricalAction && !actionJson.isNullOrBlank()) {
                        val action = SyncActionSerializer.fromJson(actionJson)
                        if (action != null) {
                            handleIncomingAction(action)
                        }
                    }
                }
            }

            // 4. Handle Full Snapshot update (for full synchronization without repetitive heartbeat churn)
            val strokesJson = snapshot.getString("strokes_json")
            val stickersJson = snapshot.getString("stickers_json")
            val lastUpdateBy = snapshot.getString("updated_by")

            if (lastUpdateBy != myDeviceId && (!strokesJson.isNullOrBlank() || !stickersJson.isNullOrBlank())) {
                val themeName = snapshot.getString("wallpaperTheme") ?: "FROSTED_GLASS"
                val theme = try { WallpaperTheme.valueOf(themeName) } catch (e: Exception) { WallpaperTheme.FROSTED_GLASS }

                val currentSignature = "$lastUpdateBy:${strokesJson?.hashCode() ?: 0}:${stickersJson?.hashCode() ?: 0}:$themeName"
                if (currentSignature != lastProcessedCanvasSignature) {
                    lastProcessedCanvasSignature = currentSignature

                    val strokesList = if (!strokesJson.isNullOrBlank()) parseStrokesFromJson(strokesJson) else emptyList()
                    val stickersList = if (!stickersJson.isNullOrBlank()) parseStickersFromJson(stickersJson) else emptyList()

                    val fullSnapshot = SyncAction.FullSnapshot(
                        strokes = strokesList,
                        stickers = stickersList,
                        wallpaperTheme = theme,
                        authorId = lastUpdateBy ?: "partner"
                    )
                    handleIncomingAction(fullSnapshot)
                }
            }
        } catch (e: Exception) {
            Log.e("PartnerSync", "Error handling Firestore snapshot", e)
        }
    }

    /**
     * Purge rooms that have not been updated for 2 weeks.
     */
    private fun purgeStaleRoomsIfAny() {
        scope.launch(Dispatchers.IO) {
            try {
                val db = firestore ?: return@launch
                val twoWeeksAgo = System.currentTimeMillis() - TWO_WEEKS_MILLIS
                db.collection("rooms")
                    .whereLessThan("updatedAt", twoWeeksAgo)
                    .limit(10)
                    .get()
                    .addOnSuccessListener { snapshots ->
                        for (doc in snapshots.documents) {
                            Log.i("PartnerSync", "Cleaning up stale room doc ${doc.id} inactive for > 2 weeks")
                            doc.reference.delete()
                        }
                    }
            } catch (e: Exception) {
                // non-fatal cleanup check
            }
        }
    }


    private fun parseStrokesFromJson(jsonString: String): List<DrawingStroke> {
        val strokes = mutableListOf<DrawingStroke>()
        try {
            val arr = JSONArray(jsonString)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pts = parsePoints(obj)
                val mod = try { com.example.data.model.StrokeModifier.valueOf(obj.optString("modifier", "NONE")) } catch (e: Exception) { com.example.data.model.StrokeModifier.NONE }
                strokes.add(
                    DrawingStroke(
                        id = obj.getString("strokeId"),
                        points = pts,
                        colorArgb = obj.getInt("colorArgb"),
                        strokeWidth = obj.getDouble("strokeWidth").toFloat(),
                        brushType = try { BrushType.valueOf(obj.optString("brushType", "PEN")) } catch (e: Exception) { BrushType.PEN },
                        authorId = obj.optString("authorId", "partner"),
                        alpha = obj.optDouble("alpha", 1.0).toFloat(),
                        modifier = mod
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("PartnerSync", "Error parsing strokes JSON", e)
        }
        return strokes
    }

    private fun parseStickersFromJson(jsonString: String): List<PlacedSticker> {
        val stickers = mutableListOf<PlacedSticker>()
        try {
            val arr = JSONArray(jsonString)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                stickers.add(
                    PlacedSticker(
                        id = obj.getString("id"),
                        content = obj.getString("content"),
                        x = obj.getDouble("x").toFloat(),
                        y = obj.getDouble("y").toFloat(),
                        scale = obj.optDouble("scale", 1.0).toFloat(),
                        rotation = obj.optDouble("rotation", 0.0).toFloat(),
                        authorId = obj.optString("authorId", "partner")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("PartnerSync", "Error parsing stickers JSON", e)
        }
        return stickers
    }

    private fun parsePoints(json: JSONObject): List<DrawingPoint> {
        val pts = mutableListOf<DrawingPoint>()
        if (json.has("pts")) {
            val raw = json.optString("pts", "")
            if (raw.isNotEmpty()) {
                val tokens = raw.split(';')
                for (token in tokens) {
                    val parts = token.split(',')
                    if (parts.size >= 2) {
                        val x = (parts[0].toFloatOrNull() ?: 0f) / 1000f
                        val y = (parts[1].toFloatOrNull() ?: 0f) / 1000f
                        val p = if (parts.size >= 3) (parts[2].toFloatOrNull() ?: 100f) / 100f else 1.0f
                        pts.add(DrawingPoint(x, y, p))
                    }
                }
            }
        }
        return pts
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(12000)
                try {
                    val db = firestore ?: return@launch
                    val roomCode = _currentRoomCode.value
                    val now = System.currentTimeMillis()
                    db.collection("rooms").document(roomCode).set(
                        mapOf(
                            "presence" to mapOf(myDeviceId to now),
                            "presence_names" to mapOf(myDeviceId to _myName.value),
                            "updatedAt" to now,
                            "expiresAt" to (now + TWO_WEEKS_MILLIS),
                            "ttl_timestamp" to Timestamp(Date(now + TWO_WEEKS_MILLIS))
                        ),
                        SetOptions.merge()
                    )
                } catch (e: Exception) {
                    // Ignore non-fatal heartbeat errors
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

    /**
     * Broadcast an action to Firestore with 2-week TTL and expiration metadata.
     */
    fun broadcastAction(action: SyncAction) {
        val db = firestore ?: return
        val roomCode = _currentRoomCode.value
        val actionId = UUID.randomUUID().toString()
        recordProcessedAction(actionId)
        val now = System.currentTimeMillis()

        when (action) {
            is SyncAction.StrokeBegin, is SyncAction.StrokePoints -> {
                // Throttle live drafting points to keep database writes clean and efficient
                pendingDraftAction = action
                if (draftThrottleJob?.isActive != true) {
                    draftThrottleJob = scope.launch(Dispatchers.IO) {
                        delay(120)
                        val toSend = pendingDraftAction
                        pendingDraftAction = null
                        if (toSend != null) {
                            val json = SyncActionSerializer.toJson(toSend)
                            try {
                                db.collection("rooms").document(roomCode).set(
                                    mapOf(
                                        "draft_json" to json,
                                        "draft_sender" to myDeviceId
                                    ),
                                    SetOptions.merge()
                                )
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
                }
            }
            is SyncAction.StrokeFinished -> {
                draftThrottleJob?.cancel()
                pendingDraftAction = null
                val actionJson = SyncActionSerializer.toJson(action)
                scope.launch(Dispatchers.IO) {
                    try {
                        db.collection("rooms").document(roomCode).set(
                            mapOf(
                                "draft_json" to FieldValue.delete(),
                                "draft_sender" to FieldValue.delete(),
                                "last_action_id" to actionId,
                                "last_action_sender" to myDeviceId,
                                "last_action_json" to actionJson,
                                "updated_by" to myDeviceId,
                                "updatedAt" to now,
                                "expiresAt" to (now + TWO_WEEKS_MILLIS),
                                "ttl_timestamp" to Timestamp(Date(now + TWO_WEEKS_MILLIS))
                            ),
                            SetOptions.merge()
                        )
                    } catch (e: Exception) {
                        Log.e("PartnerSync", "Error updating finished stroke on Firestore", e)
                        scheduleListenerReconnect(roomCode)
                    }
                }
            }
            is SyncAction.FullSnapshot -> {
                scope.launch(Dispatchers.IO) {
                    try {
                        val strokesArr = JSONArray()
                        action.strokes.forEach { s ->
                            val sObj = JSONObject()
                            sObj.put("strokeId", s.id)
                            sObj.put("colorArgb", s.colorArgb)
                            sObj.put("strokeWidth", s.strokeWidth.toDouble())
                            sObj.put("brushType", s.brushType.name)
                            sObj.put("alpha", s.alpha.toDouble())
                            sObj.put("authorId", s.authorId)
                            val sb = StringBuilder()
                            s.points.forEachIndexed { idx, p ->
                                if (idx > 0) sb.append(';')
                                val ix = (p.x * 1000).toInt()
                                val iy = (p.y * 1000).toInt()
                                val ip = (p.pressure * 100).toInt()
                                sb.append(ix).append(',').append(iy).append(',').append(ip)
                            }
                            sObj.put("pts", sb.toString())
                            strokesArr.put(sObj)
                        }

                        val stArr = JSONArray()
                        action.stickers.forEach { st ->
                            val stObj = JSONObject()
                            stObj.put("id", st.id)
                            stObj.put("content", st.content)
                            stObj.put("x", st.x.toDouble())
                            stObj.put("y", st.y.toDouble())
                            stObj.put("scale", st.scale.toDouble())
                            stObj.put("rotation", st.rotation.toDouble())
                            stObj.put("authorId", st.authorId)
                            stArr.put(stObj)
                        }

                        val updates = hashMapOf<String, Any>(
                            "strokes_json" to strokesArr.toString(),
                            "stickers_json" to stArr.toString(),
                            "wallpaperTheme" to action.wallpaperTheme.name,
                            "updated_by" to myDeviceId,
                            "updatedAt" to now,
                            "expiresAt" to (now + TWO_WEEKS_MILLIS),
                            "ttl_timestamp" to Timestamp(Date(now + TWO_WEEKS_MILLIS)),
                            "draft_json" to FieldValue.delete(),
                            "draft_sender" to FieldValue.delete()
                        )
                        db.collection("rooms").document(roomCode).set(updates, SetOptions.merge())
                    } catch (e: Exception) {
                        Log.e("PartnerSync", "Error setting full snapshot on Firestore", e)
                        scheduleListenerReconnect(roomCode)
                    }
                }
            }
            is SyncAction.ClearAll -> {
                scope.launch(Dispatchers.IO) {
                    try {
                        val updates = hashMapOf<String, Any>(
                            "strokes_json" to "[]",
                            "stickers_json" to "[]",
                            "last_action_id" to actionId,
                            "last_action_sender" to myDeviceId,
                            "last_action_json" to SyncActionSerializer.toJson(action),
                            "updated_by" to myDeviceId,
                            "updatedAt" to now,
                            "expiresAt" to (now + TWO_WEEKS_MILLIS),
                            "ttl_timestamp" to Timestamp(Date(now + TWO_WEEKS_MILLIS)),
                            "draft_json" to FieldValue.delete(),
                            "draft_sender" to FieldValue.delete()
                        )
                        db.collection("rooms").document(roomCode).set(updates, SetOptions.merge())
                    } catch (e: Exception) {
                        Log.e("PartnerSync", "Error clearing canvas on Firestore", e)
                        scheduleListenerReconnect(roomCode)
                    }
                }
            }
            else -> {
                // HeartPing, Sticker updates, Cursor, etc.
                val actionJson = SyncActionSerializer.toJson(action)
                scope.launch(Dispatchers.IO) {
                    try {
                        db.collection("rooms").document(roomCode).set(
                            mapOf(
                                "last_action_id" to actionId,
                                "last_action_sender" to myDeviceId,
                                "last_action_json" to actionJson,
                                "updated_by" to myDeviceId,
                                "updatedAt" to now,
                                "expiresAt" to (now + TWO_WEEKS_MILLIS),
                                "ttl_timestamp" to Timestamp(Date(now + TWO_WEEKS_MILLIS))
                            ),
                            SetOptions.merge()
                        )
                    } catch (e: Exception) {
                        Log.e("PartnerSync", "Error broadcasting action to Firestore", e)
                    }
                }
            }
        }
    }

    /**
     * Unified single atomic write for finishing a stroke and saving canvas snapshot.
     * Prevents document write race conditions and halves cloud database requests.
     */
    fun broadcastCanvasChange(
        finishedStroke: DrawingStroke? = null,
        allStrokes: List<DrawingStroke>,
        allStickers: List<PlacedSticker>,
        wallpaperTheme: WallpaperTheme
    ) {
        val db = firestore ?: return
        val roomCode = _currentRoomCode.value
        val actionId = UUID.randomUUID().toString()
        recordProcessedAction(actionId)
        val now = System.currentTimeMillis()

        draftThrottleJob?.cancel()
        pendingDraftAction = null

        scope.launch(Dispatchers.IO) {
            try {
                val strokesArr = JSONArray()
                allStrokes.forEach { s ->
                    val sObj = JSONObject()
                    sObj.put("strokeId", s.id)
                    sObj.put("colorArgb", s.colorArgb)
                    sObj.put("strokeWidth", s.strokeWidth.toDouble())
                    sObj.put("brushType", s.brushType.name)
                    sObj.put("alpha", s.alpha.toDouble())
                    sObj.put("authorId", s.authorId)
                    val sb = StringBuilder()
                    s.points.forEachIndexed { idx, p ->
                        if (idx > 0) sb.append(';')
                        val ix = (p.x * 1000).toInt()
                        val iy = (p.y * 1000).toInt()
                        val ip = (p.pressure * 100).toInt()
                        sb.append(ix).append(',').append(iy).append(',').append(ip)
                    }
                    sObj.put("pts", sb.toString())
                    strokesArr.put(sObj)
                }

                val stArr = JSONArray()
                allStickers.forEach { st ->
                    val stObj = JSONObject()
                    stObj.put("id", st.id)
                    stObj.put("content", st.content)
                    stObj.put("x", st.x.toDouble())
                    stObj.put("y", st.y.toDouble())
                    stObj.put("scale", st.scale.toDouble())
                    stObj.put("rotation", st.rotation.toDouble())
                    stObj.put("authorId", st.authorId)
                    stArr.put(stObj)
                }

                val updates = hashMapOf<String, Any>(
                    "strokes_json" to strokesArr.toString(),
                    "stickers_json" to stArr.toString(),
                    "wallpaperTheme" to wallpaperTheme.name,
                    "updated_by" to myDeviceId,
                    "updatedAt" to now,
                    "expiresAt" to (now + TWO_WEEKS_MILLIS),
                    "ttl_timestamp" to Timestamp(Date(now + TWO_WEEKS_MILLIS)),
                    "draft_json" to FieldValue.delete(),
                    "draft_sender" to FieldValue.delete()
                )

                if (finishedStroke != null) {
                    updates["last_action_id"] = actionId
                    updates["last_action_sender"] = myDeviceId
                    updates["last_action_json"] = SyncActionSerializer.toJson(SyncAction.StrokeFinished(finishedStroke))
                }

                db.collection("rooms").document(roomCode).set(updates, SetOptions.merge())
            } catch (e: Exception) {
                Log.e("PartnerSync", "Error broadcasting canvas change to Firestore", e)
                scheduleListenerReconnect(roomCode)
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

    private fun disconnectFirestore() {
        try {
            watchdogJob?.cancel()
            watchdogJob = null
            reconnectJob?.cancel()
            reconnectJob = null
            firestoreListener?.remove()
            firestoreListener = null
        } catch (e: Exception) {
            // ignore
        }
    }

    fun cleanup() {
        watchdogJob?.cancel()
        simulationJob?.cancel()
        heartbeatJob?.cancel()
        draftThrottleJob?.cancel()
        reconnectJob?.cancel()
        disconnectFirestore()
    }
}

