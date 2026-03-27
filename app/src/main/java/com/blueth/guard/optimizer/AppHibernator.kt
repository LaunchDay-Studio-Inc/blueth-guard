package com.blueth.guard.optimizer

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class HibernationResult(
    val appsHibernated: Int,
    val memoryFreedKb: Long,
    val message: String
)

@Singleton
class AppHibernator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val ESSENTIAL_PACKAGES = setOf(
            "com.android.phone",
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.android.camera",
            "com.android.camera2",
            "com.google.android.deskclock",
            "com.android.deskclock",
            "com.android.dialer",
            "com.google.android.dialer"
        )
    }

    private val activityManager: ActivityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    fun hibernateApp(packageName: String): Boolean {
        if (packageName == context.packageName) return false
        if (packageName in ESSENTIAL_PACKAGES) return false
        return try {
            activityManager.killBackgroundProcesses(packageName)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hibernateApps(packageNames: List<String>): HibernationResult {
        var hibernated = 0
        var memoryFreed = 0L
        for (pkg in packageNames) {
            if (pkg in ESSENTIAL_PACKAGES || pkg == context.packageName) continue
            try {
                // Estimate memory before killing
                val pids = activityManager.runningAppProcesses
                    ?.filter { it.processName.startsWith(pkg) }
                    ?.map { it.pid }
                    ?.toIntArray() ?: intArrayOf()
                val memInfos = if (pids.isNotEmpty()) activityManager.getProcessMemoryInfo(pids) else emptyArray()
                val memKb = memInfos.sumOf { it.totalPss.toLong() }

                activityManager.killBackgroundProcesses(pkg)
                hibernated++
                memoryFreed += memKb
            } catch (_: Exception) { }
        }
        return HibernationResult(
            appsHibernated = hibernated,
            memoryFreedKb = memoryFreed,
            message = if (hibernated > 0) "Hibernated $hibernated apps, freed ~${memoryFreed / 1024} MB"
            else "No apps to hibernate"
        )
    }

    fun isEssentialApp(packageName: String): Boolean {
        return packageName in ESSENTIAL_PACKAGES || packageName == context.packageName
    }

    fun openAppBatterySettings(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }
}
