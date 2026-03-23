package com.blueth.guard.privacy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.blueth.guard.MainActivity
import com.blueth.guard.R
import com.blueth.guard.data.local.InstallEvent
import com.blueth.guard.data.local.InstallEventDao
import com.blueth.guard.scanner.SecurityScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallGuard @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityScanner: SecurityScanner,
    private val dao: InstallEventDao
) {

    companion object {
        const val CHANNEL_ID = "install_guard"
        const val CHANNEL_NAME = "Install Guard"
        private const val NOTIFICATION_ID_BASE = 5000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when new apps are installed or updated"
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    suspend fun onPackageInstalled(packageName: String) = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }

        val installSource = getInstallSource(packageName)
        val scanResult = securityScanner.scanApp(packageName)
        val riskScore = scanResult?.threatAssessment?.overallScore
        val scanSummary = scanResult?.threatAssessment?.riskLevel?.label

        val event = InstallEvent(
            packageName = packageName,
            appName = appName,
            action = InstallAction.INSTALLED,
            timestamp = System.currentTimeMillis(),
            riskScore = riskScore,
            installSource = installSource,
            scanSummary = scanSummary
        )
        dao.insert(event)

        postInstallNotification(appName, riskScore, scanSummary, isUpdate = false)
    }

    suspend fun onPackageUpdated(packageName: String) = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }

        val installSource = getInstallSource(packageName)
        val scanResult = securityScanner.scanApp(packageName)
        val riskScore = scanResult?.threatAssessment?.overallScore
        val scanSummary = scanResult?.threatAssessment?.riskLevel?.label

        val event = InstallEvent(
            packageName = packageName,
            appName = appName,
            action = InstallAction.UPDATED,
            timestamp = System.currentTimeMillis(),
            riskScore = riskScore,
            installSource = installSource,
            scanSummary = scanSummary
        )
        dao.insert(event)

        postInstallNotification(appName, riskScore, scanSummary, isUpdate = true)
    }

    suspend fun onPackageRemoved(packageName: String) = withContext(Dispatchers.IO) {
        val event = InstallEvent(
            packageName = packageName,
            appName = packageName,
            action = InstallAction.UNINSTALLED,
            timestamp = System.currentTimeMillis(),
            riskScore = null,
            installSource = null,
            scanSummary = null
        )
        dao.insert(event)
    }

    fun getInstallHistory(days: Int = 30): Flow<List<InstallEvent>> {
        val limit = days * 10 // reasonable limit
        return dao.getRecentEvents(limit)
    }

    fun getSideloadedApps(): Flow<List<InstallEvent>> {
        val since = System.currentTimeMillis() - 365L * 24 * 3600_000
        return dao.getRecentEvents(1000)
    }

    private fun getInstallSource(packageName: String): String {
        val pm = context.packageManager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName ?: "Unknown"
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName) ?: "Unknown"
            }
        } catch (_: Exception) {
            "Unknown"
        }
    }

    private fun postInstallNotification(
        appName: String,
        riskScore: Int?,
        scanSummary: String?,
        isUpdate: Boolean
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val riskLabel = when {
            riskScore == null -> "Unknown"
            riskScore <= 20 -> "Safe"
            riskScore <= 40 -> "Low"
            riskScore <= 60 -> "Medium"
            riskScore <= 80 -> "High"
            else -> "Critical"
        }

        val action = if (isUpdate) "updated" else "installed"
        val title = "$appName $action — Risk: $riskLabel"
        val importance = if (riskScore != null && riskScore > 60) {
            NotificationManager.IMPORTANCE_HIGH
        } else {
            NotificationManager.IMPORTANCE_DEFAULT
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(scanSummary ?: "Tap to view details in Blueth Guard")
            .setPriority(
                if (importance == NotificationManager.IMPORTANCE_HIGH)
                    NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_BASE + appName.hashCode(), notification)
    }
}
