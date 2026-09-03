package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.model.BrushType

/**
 * TactileFeedbackHelper delivers brush-specific haptic vibrations
 * to make drawing, sticker placement, and UI navigation feel responsive, organic, and tactile.
 * Zero audio / sound overhead.
 */
object TactileFeedbackHelper {

    private const val TAG = "TactileFeedbackHelper"

    private var vibrator: Vibrator? = null

    // Rate limiter for continuous strokes
    private var lastStrokeVibrateTime: Long = 0L

    @Volatile
    private var isInitialized = false

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext ?: context

        Thread {
            try {
                vibrator = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                        vibratorManager?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Vibrator service not accessible", e)
                    null
                }
                isInitialized = true
            } catch (e: Throwable) {
                Log.e(TAG, "Error in TactileFeedback initialization", e)
            }
        }.apply {
            name = "TactileFeedback-Init"
            priority = Thread.MIN_PRIORITY
            start()
        }
    }

    // ==========================================
    // DRAWING INTERACTION VIBRATIONS
    // ==========================================

    /**
     * Triggered on initial touch down of a drawing stroke.
     */
    fun onStrokeStart(
        brushType: BrushType,
        hapticsEnabled: Boolean = true
    ) {
        if (hapticsEnabled) {
            vibrateForBrush(brushType, isInitialTouch = true)
        }
    }

    /**
     * Triggered periodically during dragging / drawing strokes.
     */
    fun onStrokeMove(
        brushType: BrushType,
        hapticsEnabled: Boolean = true
    ) {
        val now = System.currentTimeMillis()
        // Rate-limit continuous haptics to every ~110ms
        if (hapticsEnabled && (now - lastStrokeVibrateTime > 110)) {
            lastStrokeVibrateTime = now
            vibrateForBrush(brushType, isInitialTouch = false)
        }
    }

    /**
     * Triggered when a sticker or emoji is placed or stamped on the canvas.
     */
    fun onStickerPlaced(hapticsEnabled: Boolean = true) {
        if (hapticsEnabled) {
            vibrateClick(strength = 2) // Crisp pop click
        }
    }

    /**
     * Triggered when switching brush, modifier, or tool.
     */
    fun onToolSelected(hapticsEnabled: Boolean = true) {
        if (hapticsEnabled) {
            vibrateTick(light = true)
        }
    }

    /**
     * Triggered when selecting a color swatch.
     */
    fun onColorSelected(hapticsEnabled: Boolean = true) {
        if (hapticsEnabled) {
            vibrateTick(light = true)
        }
    }

    /**
     * Triggered on Undo or Redo actions.
     */
    fun onUndoRedo(hapticsEnabled: Boolean = true) {
        if (hapticsEnabled) {
            vibrateTick(light = false)
        }
    }

    /**
     * Triggered on Clear Canvas action.
     */
    fun onClearCanvas(hapticsEnabled: Boolean = true) {
        if (hapticsEnabled) {
            vibrateBurst(durationMs = 35, amplitude = 120)
        }
    }

    // ==========================================
    // HAPTIC VIBRATION PATTERNS PER BRUSH
    // ==========================================

    private fun vibrateForBrush(brushType: BrushType, isInitialTouch: Boolean) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        try {
            when (brushType) {
                BrushType.PEN, BrushType.HIGHLIGHTER, BrushType.WATERCOLOR -> {
                    // Crisp light micro-tick
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createOneShot(10L, 40))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(10L)
                    }
                }

                BrushType.PENCIL -> {
                    // Textured subtle scratch (short 8ms light tick)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createOneShot(8L, 35))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(8L)
                    }
                }

                BrushType.SPRAY, BrushType.PULSING_SPRAY -> {
                    // Fine rapid aerosol spray nozzle pulse
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val timings = if (isInitialTouch) longArrayOf(0, 12, 10, 12) else longArrayOf(0, 8, 8, 8)
                        val amplitudes = if (isInitialTouch) intArrayOf(0, 80, 0, 70) else intArrayOf(0, 45, 0, 40)
                        vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(14L)
                    }
                }

                BrushType.NEON, BrushType.PULSING_NEON -> {
                    // Smooth electric hum / rhythmic dual pulse
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val timings = longArrayOf(0, 16, 20, 16)
                        val amplitudes = intArrayOf(0, 60, 0, 85)
                        vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(20L)
                    }
                }

                BrushType.RAINBOW, BrushType.ANIMATED_WAVE -> {
                    // Flowing dynamic pulse
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val timings = longArrayOf(0, 12, 15, 18)
                        val amplitudes = intArrayOf(0, 50, 0, 75)
                        vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(18L)
                    }
                }

                BrushType.DOTTED, BrushType.DOT_FLOW -> {
                    // Staccato micro-tap
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createOneShot(6L, 50))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(6L)
                    }
                }

                BrushType.ERASER -> {
                    // Scrubbing friction vibration
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val timings = longArrayOf(0, 14, 12, 14)
                        val amplitudes = intArrayOf(0, 65, 0, 65)
                        vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(16L)
                    }
                }

                BrushType.WAVY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createOneShot(14L, 55))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(14L)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering brush vibration", e)
        }
    }

    private fun vibrateTick(light: Boolean = true) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(if (light) 8L else 14L, if (light) 40 else 70))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(if (light) 8L else 14L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating tick", e)
        }
    }

    private fun vibrateClick(strength: Int = 1) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(if (strength == 1) 15L else 24L, if (strength == 1) 75 else 120))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(if (strength == 1) 15L else 24L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating click", e)
        }
    }

    private fun vibrateBurst(durationMs: Long, amplitude: Int) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating burst", e)
        }
    }
}
