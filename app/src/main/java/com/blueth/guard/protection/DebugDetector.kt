package com.blueth.guard.protection

import android.app.Application
import android.provider.Settings
import javax.inject.Inject
import javax.inject.Singleton

data class DebugStatus(
    val usbDebuggingEnabled: Boolean,
    val developerOptionsEnabled: Boolean,
    val severity: String
)

@Singleton
class DebugDetector @Inject constructor(
    private val app: Application
) {
    fun isUsbDebuggingEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(
                app.contentResolver,
                Settings.Secure.ADB_ENABLED, 0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    fun isDeveloperOptionsEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(
                app.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    fun checkAll(): DebugStatus {
        val usb = isUsbDebuggingEnabled()
        val dev = isDeveloperOptionsEnabled()
        val severity = when {
            usb && dev -> "High"
            usb || dev -> "Medium"
            else -> "Low"
        }
        return DebugStatus(usb, dev, severity)
    }
}
