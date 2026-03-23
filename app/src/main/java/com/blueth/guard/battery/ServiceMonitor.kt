package com.blueth.guard.battery

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

data class RunningServiceInfo(
    val packageName: String,
    val appName: String,
    val serviceClassName: String,
    val isForeground: Boolean,
    val isSystem: Boolean,
    val runningSinceMs: Long,
    val runningDurationMs: Long,
    val processName: String,
    val drainImpact: DrainImpact
)

enum class DrainImpact { LOW, MEDIUM, HIGH }

@Singleton
class ServiceMonitor @Inject constructor(
    private val app: Application
) {

    /**
     * Note: ActivityManager.getRunningServices() has been limited since Android O (API 26)
     * for third-party apps. It mainly returns the caller's own services plus a few others.
     * For richer data in a future phase, consider using UsageStatsManager or dumpsys.
     */
    suspend fun getRunningServices(): List<RunningServiceInfo> {
        val am = app.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return emptyList()
        val pm = app.packageManager
        val now = System.currentTimeMillis()

        @Suppress("DEPRECATION")
        val services = am.getRunningServices(Int.MAX_VALUE)

        return services.mapNotNull { info ->
            val pkg = info.service.packageName
            val serviceClass = info.service.className

            val appName = try {
                @Suppress("DEPRECATION")
                pm.getApplicationLabel(
                    pm.getApplicationInfo(pkg, 0)
                ).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                pkg
            }

            val isSystem = try {
                @Suppress("DEPRECATION")
                val appInfo = pm.getApplicationInfo(pkg, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }

            val runningDuration = now - info.activeSince
            val isForeground = info.foreground

            val drainImpact = when {
                !isSystem && !isForeground && runningDuration > 3600_000L -> DrainImpact.HIGH
                !isSystem && runningDuration > 900_000L -> DrainImpact.MEDIUM
                else -> DrainImpact.LOW
            }

            RunningServiceInfo(
                packageName = pkg,
                appName = appName,
                serviceClassName = serviceClass,
                isForeground = isForeground,
                isSystem = isSystem,
                runningSinceMs = info.activeSince,
                runningDurationMs = runningDuration,
                processName = info.process,
                drainImpact = drainImpact
            )
        }.sortedWith(
            compareBy<RunningServiceInfo> { it.drainImpact.ordinal }
                .reversed()
                .thenByDescending { it.runningDurationMs }
        )
    }
}
