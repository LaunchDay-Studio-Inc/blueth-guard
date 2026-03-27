package com.blueth.guard.battery

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.blueth.guard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargeGuard @Inject constructor(
    private val app: Application
) {
    companion object {
        const val CHANNEL_ID = "charge_guard_channel"
        const val NOTIFICATION_ID_CHARGE = 3001
        const val NOTIFICATION_ID_FULL = 3002
        const val DEFAULT_THRESHOLD = 80
    }

    private var isMonitoring = false
    private var threshold = DEFAULT_THRESHOLD
    private var hasNotifiedThreshold = false
    private var chargingStartTime = 0L
    private var receiver: BroadcastReceiver? = null

    fun start(alertThreshold: Int = DEFAULT_THRESHOLD) {
        if (isMonitoring) return
        isMonitoring = true
        threshold = alertThreshold
        hasNotifiedThreshold = false
        createNotificationChannel()

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                handleBatteryChanged(intent)
            }
        }
        app.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    fun stop() {
        isMonitoring = false
        try {
            receiver?.let { app.unregisterReceiver(it) }
        } catch (_: Exception) { }
        receiver = null
    }

    fun setThreshold(percent: Int) {
        threshold = percent
        hasNotifiedThreshold = false
    }

    fun isActive(): Boolean = isMonitoring

    private fun handleBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val levelPercent = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        if (isCharging && chargingStartTime == 0L) {
            chargingStartTime = System.currentTimeMillis()
        } else if (!isCharging) {
            chargingStartTime = 0L
            hasNotifiedThreshold = false
        }

        // Threshold alert
        if (isCharging && levelPercent >= threshold && !hasNotifiedThreshold) {
            hasNotifiedThreshold = true
            showNotification(
                NOTIFICATION_ID_CHARGE,
                "Battery at $levelPercent%",
                "Unplug to preserve battery life. Charging above $threshold% accelerates battery wear."
            )
        }

        // Full charge + prolonged alert
        if (isCharging && levelPercent >= 100 && chargingStartTime > 0) {
            val chargingDuration = System.currentTimeMillis() - chargingStartTime
            if (chargingDuration > 30 * 60_000) { // 30 minutes
                showNotification(
                    NOTIFICATION_ID_FULL,
                    "Fully charged — unplug recommended",
                    "Prolonged charging accelerates battery wear. Your phone has been charging for ${chargingDuration / 60_000} minutes."
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Charge Guard",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Charge management alerts"
            }
            val nm = app.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun showNotification(id: Int, title: String, text: String) {
        val nm = app.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(app, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(id, notification)
    }
}
