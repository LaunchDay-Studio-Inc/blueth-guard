package com.blueth.guard.protection

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import com.blueth.guard.data.prefs.UserPreferences
import com.blueth.guard.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProtectionService : Service() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var debugDetector: DebugDetector

    @Inject
    lateinit var accessibilityWatcher: AccessibilityWatcher

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var integrityChecker: IntegrityChecker

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var packageReceiver: BroadcastReceiver? = null
    private var lastDebugStatus: DebugStatus? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NotificationHelper.NOTIFICATION_ID_PROTECTION_SERVICE,
            notificationHelper.createProtectionServiceNotification()
        )

        registerPackageReceiver()
        startPeriodicChecks()

        return START_STICKY
    }

    private fun registerPackageReceiver() {
        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
                    val packageName = intent.data?.schemeSpecificPart ?: return
                    val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    if (!isReplacing) {
                        notificationHelper.showInstallScanResult(
                            appName = packageName,
                            isSafe = true,
                            riskLevel = "Scanning..."
                        )
                    }
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
    }

    private fun startPeriodicChecks() {
        serviceScope.launch {
            while (true) {
                // Check if still enabled
                val enabled = userPreferences.realTimeProtection.first()
                if (!enabled) {
                    stopSelf()
                    return@launch
                }

                // Check debug status
                val debugStatus = debugDetector.checkAll()
                if (lastDebugStatus != null && debugStatus != lastDebugStatus) {
                    if (debugStatus.usbDebuggingEnabled && lastDebugStatus?.usbDebuggingEnabled == false) {
                        notificationHelper.showSecurityAlert(
                            "USB Debugging Enabled",
                            "USB debugging was just enabled on your device",
                            null
                        )
                    }
                }
                lastDebugStatus = debugStatus

                // Check app integrity
                val integrity = integrityChecker.check()
                if (integrity.isDebuggable || integrity.installerTampered) {
                    notificationHelper.showSecurityAlert(
                        "App Integrity Warning",
                        "Potential tampering detected: ${integrity.severity} severity",
                        null
                    )
                }

                // Check suspicious accessibility services
                val suspicious = accessibilityWatcher.getSuspiciousServices()
                if (suspicious.isNotEmpty()) {
                    notificationHelper.showSecurityAlert(
                        "Suspicious Accessibility Service",
                        "${suspicious.size} suspicious accessibility service(s) detected",
                        suspicious.firstOrNull()?.resolveInfo?.serviceInfo?.packageName
                    )
                }

                delay(30 * 60 * 1000L) // 30 minutes
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        packageReceiver?.let { unregisterReceiver(it) }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ProtectionService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProtectionService::class.java))
        }
    }
}
