package com.example.controlvolumen

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private val prefsName = "volume_prefs"
    private val keyTheme = "panel_theme"
    private val keyBoost = "volume_boost"
    private val keyDark = "dark_mode"

    private lateinit var prefs: SharedPreferences

    // Para el permiso de overlay
    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    checkNotificationPermissionAndStart()
                } else {
                    Toast.makeText(
                        this,
                        "Permiso de superposición no concedido",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    // Para el permiso de notificaciones (Android 13+)
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startOverlayService()
            } else {
                // Intentar iniciar de todos modos, la notificación simplemente no se mostrará
                Toast.makeText(
                    this,
                    "Sin permiso de notificaciones, el servicio podría no funcionar correctamente",
                    Toast.LENGTH_LONG
                ).show()
                startOverlayService()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val dark = prefs.getBoolean(keyDark, false)

        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStartOverlay = findViewById<Button>(R.id.btnStartOverlay)
        val spinnerTheme = findViewById<Spinner>(R.id.spinnerTheme)
        val seekBoost = findViewById<SeekBar>(R.id.seekBoost)
        val txtBoostValue = findViewById<TextView>(R.id.txtBoostValue)
        val switchDarkTheme = findViewById<SwitchMaterial>(R.id.switchDarkTheme)

        // Restaurar tema seleccionado
        val savedTheme = prefs.getInt(keyTheme, 0)
        spinnerTheme.setSelection(savedTheme)

        // Restaurar boost
        val savedBoost = prefs.getInt(keyBoost, 100)
        seekBoost.progress = savedBoost
        txtBoostValue.text = getString(R.string.boost_value, savedBoost)

        // Restaurar dark mode
        switchDarkTheme.isChecked = dark

        seekBoost.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val boost = if (progress < 50) 50 else progress
                txtBoostValue.text = getString(R.string.boost_value, boost)
                prefs.edit { putInt(keyBoost, boost) }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                prefs.edit { putInt(keyTheme, position) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(keyDark, isChecked) }
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        btnStartOverlay.setOnClickListener {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val uri = "package:$packageName".toUri()
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
                overlayPermissionLauncher.launch(intent)
            } else {
                checkNotificationPermissionAndStart()
            }
        } else {
            startOverlayService()
        }
    }

    private fun checkNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requiere permiso de notificaciones
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startOverlayService()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(
                        this,
                        "Se necesita permiso de notificaciones para el servicio",
                        Toast.LENGTH_LONG
                    ).show()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startOverlayService()
        }
    }

    private fun startOverlayService() {
        try {
            val intent = Intent(this, VolumeOverlayService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Servicio de volumen iniciado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error al iniciar servicio: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }
}