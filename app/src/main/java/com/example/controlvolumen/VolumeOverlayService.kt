package com.example.controlvolumen

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.View.OnClickListener
import android.widget.SeekBar
import android.widget.Toast

class VolumeOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isVisible = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showHiddenVolumeBar()
        return START_STICKY
    }

    private fun showHiddenVolumeBar() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.floating_volume_control, null)
        overlayView?.visibility = View.GONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.START or Gravity.TOP
        params.x = 0
        params.y = 500

        windowManager.addView(overlayView, params)

        val seekBar = overlayView?.findViewById<SeekBar>(R.id.seekBarVolume)
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        seekBar?.max = maxVolume
        seekBar?.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        overlayView?.findViewById<View>(R.id.btnClose)?.setOnClickListener {
            overlayView?.visibility = View.GONE
        }

        overlayView?.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val initialX = event.rawX

                overlayView?.setOnTouchListener { _, motionEvent ->
                    if (motionEvent.action == MotionEvent.ACTION_MOVE) {
                        val deltaX = motionEvent.rawX - initialX

                        if (deltaX > 50 && !isVisible) {
                            showVolumeBarWithAnimation()
                        } else if (deltaX < -50 && isVisible) {
                            hideVolumeBarWithAnimation()
                        }
                    }
                    true
                }
            }
            true
        }
    }

    private fun showVolumeBarWithAnimation() {
        overlayView?.visibility = View.VISIBLE
        overlayView?.animate()?.translationX(0f)?.setDuration(300)
        isVisible = true
    }

    private fun hideVolumeBarWithAnimation() {
        overlayView?.animate()?.translationX(-overlayView!!.width.toFloat())?.setDuration(300)?.withEndAction {
            overlayView?.visibility = View.GONE
            isVisible = false
        }
    }

    override fun onDestroy() {
        overlayView?.let { windowManager.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}