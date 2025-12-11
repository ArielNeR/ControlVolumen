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
    private var overlayView: View? = null
    private var isVisible = false

    private val channelId = "volume_overlay_channel"
    private val prefsName = "volume_prefs"
    private val keyTheme = "panel_theme"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showHiddenVolumeBar()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ya se inicializa en onCreate, START_STICKY para que se mantenga
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
            .setContentText("Toca para abrir ajustes de la app")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showHiddenVolumeBar() {
        if (overlayView != null) return

        overlayView = LayoutInflater.from(this).inflate(R.layout.floating_volume_control, null)
        val container = overlayView!!.findViewById<LinearLayout>(R.id.container)
        val frameButton = overlayView!!.findViewById<FrameLayout>(R.id.frameButton)
        val seekBar = overlayView!!.findViewById<SeekBar>(R.id.seekBarVolume)

        val params = WindowManager.LayoutParams(
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

        // Arriba a la derecha, parecido al comportamiento actual
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 0
        params.y = 100 // ajusta esta Y si quieres subir/bajar el control

        windowManager.addView(overlayView, params)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        // SeekBar mapeado 0–100% al volumen real del sistema
        seekBar.max = 100
        val percent = (currentVolume * 100) / maxVolume
        seekBar.progress = percent

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newVolume = (progress * maxVolume) / 100
                    val safeVolume = newVolume.coerceIn(0, maxVolume)
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        safeVolume,
                        0
                    )
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Aplicar tema visual al frame/seekbar
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val themeIndex = prefs.getInt(keyTheme, 0)
        applyThemeToFrame(frameButton, seekBar, themeIndex)

        // Iniciar oculto: movemos todo el container hacia la derecha
        container.post {
            container.translationX = container.width.toFloat() + 40f
            isVisible = false
        }

        // Gesto en el propio container, como en tu código original
        setupSwipeGesture(container)
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
            else -> { // Clásico / MIUI-like
                frame.setBackgroundResource(R.drawable.button_rounded)
                frame.alpha = 1.0f
            }
        }

        seekBar.progressDrawable = progressDrawable
        seekBar.thumb = thumbDrawable
    }

    /**
     * Gesto de mostrar/ocultar en el MISMO contenedor, similar a tu versión original:
     * - Deslizar derecha -> izquierda: mostrar (si está oculto).
     * - Deslizar izquierda -> derecha: ocultar (si está visible).
     */
    private fun setupSwipeGesture(container: View) {
        var initialX = 0f

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialX

                    // Panel oculto: mover hacia adentro al deslizar derecha->izquierda
                    if (!isVisible && deltaX < -50) {
                        showVolumeBarWithAnimation(container)
                        initialX = event.rawX
                    }
                    // Panel visible: ocultar al deslizar izquierda->derecha
                    else if (isVisible && deltaX > 50) {
                        hideVolumeBarWithAnimation(container)
                        initialX = event.rawX
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    private fun showVolumeBarWithAnimation(container: View) {
        container.animate()
            .translationX(0f)
            .setDuration(250)
            .withStartAction { container.visibility = View.VISIBLE }
            .start()
        isVisible = true
    }

    private fun hideVolumeBarWithAnimation(container: View) {
        container.animate()
            .translationX(container.width.toFloat() + 40f)
            .setDuration(250)
            .withEndAction {
                isVisible = false
            }
            .start()
    }

    override fun onDestroy() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}