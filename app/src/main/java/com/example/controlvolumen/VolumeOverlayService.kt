package com.example.controlvolumen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt

class VolumeOverlayService : Service() {

    private lateinit var windowManager: WindowManager

    // Vista del trigger (zona de activación pequeña)
    private var triggerView: View? = null

    // Vista del panel de volumen
    private var volumePanelView: View? = null
    private var volumePanelParams: WindowManager.LayoutParams? = null

    private var isVisible = false

    private val channelId = "volume_overlay_channel"
    private val prefsName = "volume_prefs"
    private val keyTheme = "panel_theme"

    // Dimensiones del trigger (zona de activación)
    private val TRIGGER_WIDTH_DP = 20
    private val TRIGGER_HEIGHT_DP = 150

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupTriggerZone()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Control de volumen flotante",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Control de volumen activo")
            .setContentText("Desliza desde el borde derecho")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    /**
     * Crea una pequeña zona invisible en el borde derecho
     * que detecta el gesto de swipe para mostrar el panel
     */
    private fun setupTriggerZone() {
        // Crear vista trigger (solo detecta gestos)
        triggerView = View(this).apply {
            // Semi-transparente para debug, puedes ponerlo invisible
            setBackgroundColor(0x00000000) // Completamente transparente
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

        // Configurar gesto en el trigger
        var initialX = 0f
        triggerView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialX
                    // Deslizar de derecha a izquierda para mostrar
                    if (deltaX < -30 && !isVisible) {
                        showVolumePanel()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    /**
     * Muestra el panel de volumen
     */
    private fun showVolumePanel() {
        if (isVisible || volumePanelView != null) return

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

        // Configurar el SeekBar
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        seekBar.max = 100
        val percent = (currentVolume * 100) / maxVolume
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

        // Aplicar tema
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val themeIndex = prefs.getInt(keyTheme, 0)
        applyThemeToFrame(frameButton, seekBar, themeIndex)

        // Añadir vista con animación
        windowManager.addView(volumePanelView, volumePanelParams)

        // Animar entrada desde la derecha
        container.translationX = container.width.toFloat() + 100f
        container.post {
            container.animate()
                .translationX(0f)
                .setDuration(200)
                .start()
        }

        isVisible = true

        // Configurar gesto para ocultar
        setupHideGesture(container)
    }

    /**
     * Configura el gesto para ocultar el panel
     */
    private fun setupHideGesture(container: View) {
        var initialX = 0f
        var isDragging = false

        container.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    isDragging = false
                    // No consumir el evento para permitir que el SeekBar funcione
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialX
                    // Deslizar de izquierda a derecha para ocultar
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

    /**
     * Oculta y elimina el panel de volumen
     */
    private fun hideVolumePanel(container: View) {
        if (!isVisible) return

        container.animate()
            .translationX(container.width.toFloat() + 100f)
            .setDuration(200)
            .withEndAction {
                // Remover la vista completamente
                volumePanelView?.let {
                    try {
                        windowManager.removeView(it)
                    } catch (e: Exception) {
                        // Vista ya removida
                    }
                }
                volumePanelView = null
                volumePanelParams = null
                isVisible = false
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
            1 -> { // iOS-like
                frame.setBackgroundResource(R.drawable.button_rounded)
                frame.alpha = 0.96f
            }
            2 -> { // Spiderman
                frame.setBackgroundColor("#B71C1C".toColorInt())
                frame.alpha = 0.97f
            }
            else -> { // Clásico
                frame.setBackgroundResource(R.drawable.button_rounded)
                frame.alpha = 1.0f
            }
        }

        seekBar.progressDrawable = progressDrawable
        seekBar.thumb = thumbDrawable
    }

    override fun onDestroy() {
        triggerView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        volumePanelView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        triggerView = null
        volumePanelView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}