package com.fpsboostpro.app.core.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.fpsboostpro.app.MainActivity
import com.fpsboostpro.app.R

/**
 * Draws a small always-on-top overlay (CPU/RAM/battery-temp readout) using
 * SYSTEM_ALERT_WINDOW. This is the one honest, non-root way to show live
 * stats over another running game.
 *
 * Started only when the user explicitly enables "Gaming Overlay" in
 * Settings AND has granted the draw-over-other-apps permission — never
 * automatically.
 */
class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: TextView? = null

    companion object {
        private const val CHANNEL_ID = "fps_boost_overlay"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        addOverlay()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun addOverlay() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 64
        }

        val view = TextView(this).apply {
            setBackgroundColor(0xAA000000.toInt())
            setTextColor(0xFF00E5FF.toInt())
            setPadding(20, 12, 20, 12)
            textSize = 11f
            text = "FPS Boost Pro"
        }
        overlayView = view

        try {
            wm.addView(view, params)
        } catch (e: Exception) {
            // Permission not granted or view already attached; fail silently,
            // this only runs when the user opted in from Settings.
            stopSelf()
        }
    }

    /** Called by external monitors to refresh the overlay text. */
    fun updateText(text: String) {
        overlayView?.text = text
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayView = null
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Gaming overlay active")
            .setSmallIcon(R.drawable.ic_bolt_small)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }
}
