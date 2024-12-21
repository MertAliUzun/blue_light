package com.example.blue_light

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.WindowManager
import android.widget.LinearLayout
import android.graphics.Color
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat

class BlueFilterService : Service() {
    private var windowManager: WindowManager? = null
    private var filterView: LinearLayout? = null
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "BlueFilterChannel"

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mavi Işık Filtresi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mavi ışık filtresinin çalışması için gerekli bildirim"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mavi Işık Filtresi Aktif")
            .setContentText("Filtre şu anda çalışıyor")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val intensity = intent?.getFloatExtra("intensity", 0.5f) ?: 0.5f
        showFilter(intensity)
        return START_STICKY
    }

    private fun showFilter(intensity: Float) {
        if (filterView == null) {
            filterView = LinearLayout(this)
            filterView?.setBackgroundColor(Color.argb((intensity * 50).toInt(), 255, 175, 0))

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else 
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            windowManager?.addView(filterView, params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (filterView != null) {
            windowManager?.removeView(filterView)
            filterView = null
        }
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? = null
} 