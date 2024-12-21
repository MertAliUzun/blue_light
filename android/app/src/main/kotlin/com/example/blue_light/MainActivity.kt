package com.example.blue_light

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.example.blue_light_filter/filter"
    private var filterActive = false
    private var pendingIntensity: Float? = null
    
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
        private const val OVERLAY_PERMISSION_CODE = 1234
    }
    
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "toggleFilter" -> {
                    val intensity = call.argument<Double>("intensity")?.toFloat() ?: 0.5f
                    val isActive = call.argument<Boolean>("isActive") ?: false
                    
                    if (isActive) {
                        pendingIntensity = intensity
                        checkAndRequestPermissions { granted ->
                            if (granted) {
                                startFilter(intensity)
                                filterActive = true
                                result.success(true)
                            } else {
                                filterActive = false
                                result.success(false)
                            }
                        }
                    } else {
                        stopFilter()
                        filterActive = false
                        result.success(false)
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun checkAndRequestPermissions(callback: (Boolean) -> Unit) {
        var allPermissionsGranted = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            
            if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            allPermissionsGranted = false
            requestOverlayPermission()
        }

        if (allPermissionsGranted) {
            callback(true)
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            checkAllPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            checkAllPermissions()
        }
    }

    private fun checkAllPermissions() {
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasOverlayPermission && hasNotificationPermission) {
            pendingIntensity?.let { intensity ->
                startFilter(intensity)
                filterActive = true
                pendingIntensity = null
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // İzin ekranından döndükten sonra kontrol et
        if (!filterActive && Settings.canDrawOverlays(this)) {
            pendingIntensity?.let { intensity ->
                startFilter(intensity)
                filterActive = true
                pendingIntensity = null
            }
        }
    }

    private fun startFilter(intensity: Float) {
        if (Settings.canDrawOverlays(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, BlueFilterService::class.java).apply {
                    putExtra("intensity", intensity)
                })
            } else {
                startService(Intent(this, BlueFilterService::class.java).apply {
                    putExtra("intensity", intensity)
                })
            }
        }
    }

    private fun stopFilter() {
        stopService(Intent(this, BlueFilterService::class.java))
    }
} 