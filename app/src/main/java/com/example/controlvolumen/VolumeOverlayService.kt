package com.example.controlvolumen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt

class VolumeOverlayService : Service() {

    companion object {
        private const val TAG = "VolumeOverlayService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "volume_overlay_channel"
    }

    private lateinit var windowManager: WindowManager

    private var triggerView: View? = null
    private var volumePanelView: View? = null
    private var volumePanelParams: WindowManager.LayoutParams? = null

    private var isVisible = false

    private val prefsName = "volume_prefs"
    private val keyTheme = "panel_theme"

    private val TRIGGER_WIDTH_DP = 20
    private val TRIGGER_HEIGHT_DP = 150

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")

        try {
            createNotificationChannel()
            startForegroundWithType()
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            setupTriggerZone()
            Log.d(TAG, "Service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            stopSelf()
        }
    }

    private fun startForegroundWithType() {
        val notification = buildNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10-13
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
                )
            } else {
                // Android 9 y anteriores
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground: ${e.message}", e)
            // Fallback
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Control de volumen flotante",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación para el control de volumen flotante"
                setShowBadge(false)
            }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Control de volumen activo")
            .setContentText("Desliza desde el borde derecho")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupTriggerZone() {
        try {
            triggerView = View(this).apply {
                setBackgroundColor(0x00000000)
            }

            val triggerParams = WindowManager.LayoutParams(
                dpToPx(TRIGGER_WIDTH_DP),
                dpToPx(TRIGGER_HEIGHT_DP),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            triggerParams.gravity = Gravity.TOP or Gravity.END
            triggerParams.x = 0
            triggerParams.y = 200

            windowManager.addView(triggerView, triggerParams)

            var initialX = 0f
            triggerView?.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = event.rawX
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialX
                        if (deltaX < -30 && !isVisible) {
                            showVolumePanel()
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                    else -> false
                }
            }

            Log.d(TAG, "Trigger zone setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up trigger zone: ${e.message}", e)
        }
    }

    private fun showVolumePanel() {
        if (isVisible || volumePanelView != null) return

        try {
            volumePanelView = LayoutInflater.from(this)
                .inflate(R.layout.floating_volume_control, null)

            val container = volumePanelView!!.findViewById<LinearLayout>(R.id.container)
            val frameButton = volumePanelView!!.findViewById<FrameLayout>(R.id.frameButton)
            val seekBar = volumePanelView!!.findViewById<SeekBar>(R.id.seekBarVolume)

            volumePanelParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            volumePanelParams!!.gravity = Gravity.TOP or Gravity.END
            volumePanelParams!!.x = 0
            volumePanelParams!!.y = 100

            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            seekBar.max = 100
            val percent = if (maxVolume > 0) (currentVolume * 100) / maxVolume else 0
            seekBar.progress = percent

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val newVolume = (progress * maxVolume) / 100
                        val safeVolume = newVolume.coerceIn(0, maxVolume)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, safeVolume, 0)
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

            val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
            val themeIndex = prefs.getInt(keyTheme, 0)
            applyThemeToFrame(frameButton, seekBar, themeIndex)

            windowManager.addView(volumePanelView, volumePanelParams)

            container.translationX = container.width.toFloat() + 100f
            container.post {
                container.animate()
                    .translationX(0f)
                    .setDuration(200)
                    .start()
            }

            isVisible = true
            setupHideGesture(container)

            Log.d(TAG, "Volume panel shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing volume panel: ${e.message}", e)
        }
    }

    private fun setupHideGesture(container: View) {
        var initialX = 0f
        var isDragging = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    isDragging = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialX
                    if (deltaX > 50 && isVisible) {
                        isDragging = true
                        hideVolumePanel(container)
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging
                }
                else -> false
            }
        }
    }

    private fun hideVolumePanel(container: View) {
        if (!isVisible) return

        container.animate()
            .translationX(container.width.toFloat() + 100f)
            .setDuration(200)
            .withEndAction {
                volumePanelView?.let {
                    try {
                        windowManager.removeView(it)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing volume panel: ${e.message}")
                    }
                }
                volumePanelView = null
                volumePanelParams = null
                isVisible = false
                Log.d(TAG, "Volume panel hidden")
            }
            .start()
    }

    private fun applyThemeToFrame(frame: FrameLayout, seekBar: SeekBar, themeIndex: Int) {
        val progressDrawable = ResourcesCompat.getDrawable(
            resources,
            R.drawable.seekbar_progress,
            null
        )
        val thumbDrawable = ResourcesCompat.getDrawable(
            resources,
            R.drawable.seekbar_thumb,
            null
        )

        when (themeIndex) {
            1 -> {
                frame.setBackgroundResource(R.drawable.button_rounded)
                frame.alpha = 0.96f
            }
            2 -> {
                frame.setBackgroundColor("#B71C1C".toColorInt())
                frame.alpha = 0.97f
            }
            else -> {
                frame.setBackgroundResource(R.drawable.button_rounded)
                frame.alpha = 1.0f
            }
        }

        seekBar.progressDrawable = progressDrawable
        seekBar.thumb = thumbDrawable
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        try {
            triggerView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing trigger: ${e.message}")
        }
        try {
            volumePanelView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing panel: ${e.message}")
        }
        triggerView = null
        volumePanelView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}