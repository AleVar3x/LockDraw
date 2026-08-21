package com.example.service

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.DrawingRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.util.WallpaperTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingDrawingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var repository: DrawingRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var bubbleView: ComposeView? = null
    private var overlayCanvasView: ComposeView? = null
    private var bubbleLifecycleOwner: OverlayLifecycleOwner? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null

    private var persistentDrawingView: ComposeView? = null
    private var persistentDrawingLifecycleOwner: OverlayLifecycleOwner? = null

    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var overlayParams: WindowManager.LayoutParams
    private lateinit var persistentDrawingParams: WindowManager.LayoutParams

    private var isOverlayExpanded = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        repository = DrawingRepository.getInstance(application)
        repository.setFloatingServiceActive(true)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("LockDraw attivo sullo schermo"))

        initLayoutParams()
        showPersistentDrawingLayer()
        showBubble()
    }

    private fun initLayoutParams() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. Bubble Layout Params - Always on top, draggable, frosted glass
        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 350
        }

        // 2. Interactive Fullscreen Canvas Overlay Params (When drawing mode is ACTIVE)
        val overlayFlags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            overlayFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        // 3. Non-touchable persistent drawing layer (When drawing mode is INACTIVE)
        // This keeps strokes/stickers on screen while making all touches pass directly through to phone UI
        val passiveFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        persistentDrawingParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            passiveFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
    }

    private fun showPersistentDrawingLayer() {
        if (persistentDrawingView != null) return

        persistentDrawingLifecycleOwner = OverlayLifecycleOwner().apply {
            onCreate()
            onResume()
        }

        persistentDrawingView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            persistentDrawingLifecycleOwner?.attachToView(this)

            setContent {
                MyApplicationTheme {
                    val strokes by repository.strokes.collectAsState()
                    val partnerDraft by repository.partnerDraftStroke.collectAsState()
                    val placedStickers by repository.placedStickers.collectAsState()
                    val partnerPresence by repository.partnerPresence.collectAsState()
                    val floatingReactions by repository.floatingReactions.collectAsState()

                    com.example.ui.components.DrawingCanvas(
                        strokes = strokes,
                        currentDraft = null,
                        partnerDraft = partnerDraft,
                        stickers = placedStickers,
                        selectedStickerId = null,
                        partnerPresence = partnerPresence,
                        floatingReactions = floatingReactions,
                        onStartDraw = { _, _ -> },
                        onContinueDraw = { _, _ -> },
                        onFinishDraw = {},
                        onSelectSticker = {},
                        onUpdateStickerPos = { _, _, _ -> },
                        onUpdateStickerTransform = { _, _, _ -> },
                        onDeleteSticker = {},
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        isInteractive = false
                    )
                }
            }
        }

        try {
            windowManager.addView(persistentDrawingView, persistentDrawingParams)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun removePersistentDrawingLayer() {
        persistentDrawingView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // ignore
            }
        }
        persistentDrawingLifecycleOwner?.onDestroy()
        persistentDrawingLifecycleOwner = null
        persistentDrawingView = null
    }

    private fun showBubble() {
        if (bubbleView != null) {
            // Already added, just ensure its composition reflects current isOverlayExpanded
            return
        }

        bubbleLifecycleOwner = OverlayLifecycleOwner().apply {
            onCreate()
            onResume()
        }

        bubbleView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            bubbleLifecycleOwner?.attachToView(this)

            setContent {
                MyApplicationTheme {
                    FloatingBubbleView(
                        repository = repository,
                        isExpanded = isOverlayExpanded,
                        onToggle = {
                            if (isOverlayExpanded) {
                                minimizeToBubble()
                            } else {
                                expandOverlayCanvas()
                            }
                        }
                    )
                }
            }

            // Drag touch handling
            setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isDragging = false

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = bubbleParams.x
                            initialY = bubbleParams.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isDragging = false
                            return false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()
                            if (abs(dx) > 10 || abs(dy) > 10) {
                                isDragging = true
                                bubbleParams.x = initialX + dx
                                bubbleParams.y = initialY + dy
                                try {
                                    windowManager.updateViewLayout(bubbleView, bubbleParams)
                                } catch (e: Exception) {
                                    // ignore
                                }
                                return true
                            }
                        }
                        MotionEvent.ACTION_UP -> {
                            if (isDragging) {
                                return true
                            }
                        }
                    }
                    return false
                }
            })
        }

        try {
            windowManager.addView(bubbleView, bubbleParams)
        } catch (e: Exception) {
            // Permission or WindowManager error
        }
    }

    private fun expandOverlayCanvas() {
        if (overlayCanvasView != null) return

        // Hide passive drawing layer while interactive drawing canvas is active
        removePersistentDrawingLayer()

        overlayLifecycleOwner = OverlayLifecycleOwner().apply {
            onCreate()
            onResume()
        }

        overlayCanvasView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            overlayLifecycleOwner?.attachToView(this)

            setContent {
                MyApplicationTheme {
                    FloatingOverlayCanvas(
                        repository = repository,
                        onMinimizeToBubble = {
                            minimizeToBubble()
                        },
                        onCloseService = {
                            stopSelf()
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(overlayCanvasView, overlayParams)
            isOverlayExpanded = true
            // Bring bubble to top so it's always accessible to toggle drawing off
            bubbleView?.let {
                try {
                    windowManager.removeView(it)
                    windowManager.addView(it, bubbleParams)
                } catch (e: Exception) {
                    // ignore
                }
            }
        } catch (e: Exception) {
            showBubble()
        }
    }

    private fun minimizeToBubble() {
        removeOverlayCanvas()
        isOverlayExpanded = false
        // Restore persistent non-touchable drawing layer
        showPersistentDrawingLayer()
        // Ensure bubble is on top
        bubbleView?.let {
            try {
                windowManager.removeView(it)
                windowManager.addView(it, bubbleParams)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun removeBubble() {
        bubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // ignore
            }
        }
        bubbleLifecycleOwner?.onDestroy()
        bubbleLifecycleOwner = null
        bubbleView = null
    }

    private fun removeOverlayCanvas() {
        overlayCanvasView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // ignore
            }
        }
        overlayLifecycleOwner?.onDestroy()
        overlayLifecycleOwner = null
        overlayCanvasView = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_EXPAND_CANVAS -> {
                expandOverlayCanvas()
            }
            ACTION_APPLY_LOCKSCREEN -> {
                serviceScope.launch {
                    repository.applyToDeviceWallpaper(WallpaperTarget.LOCKSCREEN)
                }
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LockDraw Disegno Fluttuante",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifica persistente per il disegno fluttuante su lockscreen e homescreen"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pMainIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val expandIntent = Intent(this, FloatingDrawingService::class.java).apply {
            action = ACTION_EXPAND_CANVAS
        }
        val pExpandIntent = PendingIntent.getService(
            this, 1, expandIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val lockIntent = Intent(this, FloatingDrawingService::class.java).apply {
            action = ACTION_APPLY_LOCKSCREEN
        }
        val pLockIntent = PendingIntent.getService(
            this, 2, lockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingDrawingService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pStopIntent = PendingIntent.getService(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LockDraw • Disegno Fluttuante Attivo")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pMainIntent)
            .addAction(R.mipmap.ic_launcher, "Disegna Subito", pExpandIntent)
            .addAction(R.mipmap.ic_launcher, "Imposta Lockscreen", pLockIntent)
            .addAction(R.mipmap.ic_launcher, "Chiudi", pStopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.setFloatingServiceActive(false)
        removePersistentDrawingLayer()
        removeBubble()
        removeOverlayCanvas()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "lockdraw_floating_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_EXPAND_CANVAS = "com.example.lockdraw.EXPAND_CANVAS"
        const val ACTION_APPLY_LOCKSCREEN = "com.example.lockdraw.APPLY_LOCKSCREEN"
        const val ACTION_STOP_SERVICE = "com.example.lockdraw.STOP_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, FloatingDrawingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingDrawingService::class.java)
            context.stopService(intent)
        }
    }
}
