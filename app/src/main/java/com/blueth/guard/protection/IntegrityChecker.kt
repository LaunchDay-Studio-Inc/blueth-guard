package com.blueth.guard.protection

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

data class IntegrityStatus(
    val isDebuggable: Boolean,
    val isEmulator: Boolean,
    val isRooted: Boolean,
    val installerTampered: Boolean,
    val severity: String
)

@Singleton
class IntegrityChecker @Inject constructor(
    private val app: Application
) {
    fun check(): IntegrityStatus {
        val debuggable = isDebuggable()
        val emulator = isEmulator()
        val rooted = isRooted()
        val tampered = isInstallerTampered()

        val severity = when {
            debuggable || tampered -> "Critical"
            rooted -> "High"
            emulator -> "Medium"
            else -> "None"
        }

        return IntegrityStatus(debuggable, emulator, rooted, tampered, severity)
    }

    private fun isDebuggable(): Boolean {
        return try {
            val ai = app.packageManager.getApplicationInfo(app.packageName, 0)
            (ai.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Exception) { false }
    }

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.contains("generic") ||
                Build.FINGERPRINT.contains("sdk_gphone") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.PRODUCT.contains("sdk") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu")
    }

    private fun isRooted(): Boolean {
        val paths = listOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
            "/su/bin/su"
        )
        return paths.any { java.io.File(it).exists() } ||
                try { Runtime.getRuntime().exec("which su").inputStream.bufferedReader().readLine() != null }
                catch (_: Exception) { false }
    }

    private fun isInstallerTampered(): Boolean {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                app.packageManager.getInstallSourceInfo(app.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                app.packageManager.getInstallerPackageName(app.packageName)
            }
            // Allowed installers: Play Store, F-Droid, sideload (null), ADB (shell)
            val allowed = setOf(null, "com.android.vending", "org.fdroid.fdroid", "com.android.shell", "com.google.android.packageinstaller")
            installer !in allowed
        } catch (_: Exception) { false }
    }
}
