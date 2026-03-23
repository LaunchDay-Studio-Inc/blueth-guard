package com.blueth.guard.notification

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.blueth.guard.MainActivity
import com.blueth.guard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    private val app: Application
) {
    companion object {
        const val NOTIFICATION_ID_SECURITY_ALERT = 1001
        const val NOTIFICATION_ID_INSTALL_SCAN = 1002
        const val NOTIFICATION_ID_BATTERY_WARNING = 1003
        const val NOTIFICATION_ID_SCAN_COMPLETE = 1004
        const val NOTIFICATION_ID_PROTECTION_SERVICE = 1005
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            app, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showSecurityAlert(title: String, message: String, appName: String?): Notification {
        val notification = NotificationCompat.Builder(app, "security_alerts")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(if (appName != null) "$message — $appName" else message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createPendingIntent())
            .setAutoCancel(true)
            .build()

        val nm = app.getSystemService(android.app.NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID_SECURITY_ALERT, notification)
        return notification
    }

    fun showInstallScanResult(appName: String, isSafe: Boolean, riskLevel: String): Notification {
        val title = if (isSafe) "$appName is safe" else "$appName — $riskLevel risk"
        val icon = if (isSafe) R.drawable.ic_notification else R.drawable.ic_notification
        val notification = NotificationCompat.Builder(app, "install_guard")
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText("Tap for details")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(createPendingIntent())
            .setAutoCancel(true)
            .build()

        val nm = app.getSystemService(android.app.NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID_INSTALL_SCAN, notification)
        return notification
    }

    fun showBatteryWarning(title: String, message: String): Notification {
        val notification = NotificationCompat.Builder(app, "battery_warnings")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(createPendingIntent())
            .setAutoCancel(true)
            .build()

        val nm = app.getSystemService(android.app.NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID_BATTERY_WARNING, notification)
        return notification
    }

    fun showScanComplete(appsScanned: Int, threatsFound: Int): Notification {
        val title = "Scan Complete"
        val message = "$appsScanned apps scanned — $threatsFound threats found"
        val notification = NotificationCompat.Builder(app, "scan_results")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(createPendingIntent())
            .setAutoCancel(true)
            .build()

        val nm = app.getSystemService(android.app.NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID_SCAN_COMPLETE, notification)
        return notification
    }

    fun createProtectionServiceNotification(): Notification {
        return NotificationCompat.Builder(app, "protection_service")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Blueth Guard")
            .setContentText("Real-time protection is active")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(createPendingIntent())
            .setOngoing(true)
            .build()
    }
}
