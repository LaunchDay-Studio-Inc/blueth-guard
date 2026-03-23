package com.blueth.guard.battery

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

data class AppDrainEstimate(
    val packageName: String,
    val appName: String,
    val drainScore: Int,
    val foregroundTimeMs: Long,
    val backgroundTimeMs: Long,
    val networkBytesTotal: Long,
    val dangerousPermCount: Int,
    val drainCategory: DrainCategory,
    val estimatedMahPerDay: Float
)

enum class DrainCategory { MINIMAL, LOW, MODERATE, HIGH, EXTREME }

@Singleton
class DrainRanker @Inject constructor(
    private val app: Application
) {

    private val dangerousPermissions = setOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.BLUETOOTH_SCAN",
        "android.permission.BLUETOOTH_CONNECT",
        "android.permission.BODY_SENSORS"
    )

    suspend fun rankByDrain(hours: Int = 24): List<AppDrainEstimate> {
        val usageStatsManager =
            app.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyList()

        val pm = app.packageManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - hours * 3600_000L

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        // Aggregate per package
        data class UsageAgg(
            var totalTimeMs: Long = 0L,
            var foregroundTimeMs: Long = 0L
        )

        val aggregated = mutableMapOf<String, UsageAgg>()
        for (stat in usageStats) {
            val fgServiceTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                stat.totalTimeForegroundServiceUsed
            } else 0L
            if (stat.totalTimeInForeground <= 0 && fgServiceTime <= 0) continue
            val agg = aggregated.getOrPut(stat.packageName) { UsageAgg() }
            agg.foregroundTimeMs += stat.totalTimeInForeground
            agg.totalTimeMs += stat.totalTimeInForeground + fgServiceTime
        }

        // Filter out packages with negligible usage
        val filtered = aggregated.filter { (_, agg) -> agg.totalTimeMs > 60_000L }

        // Find maxima for normalization
        val maxTotalTime = filtered.values.maxOfOrNull { it.totalTimeMs }?.coerceAtLeast(1L) ?: 1L
        val maxDangerousPerms = 8 // max possible from our set

        return filtered.mapNotNull { (pkg, agg) ->
            val isSystem = try {
                @Suppress("DEPRECATION")
                val appInfo = pm.getApplicationInfo(pkg, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (_: PackageManager.NameNotFoundException) {
                true // skip unknown
            }

            if (isSystem) return@mapNotNull null

            val appName = try {
                @Suppress("DEPRECATION")
                pm.getApplicationLabel(
                    pm.getApplicationInfo(pkg, 0)
                ).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                pkg
            }

            val backgroundTimeMs = (agg.totalTimeMs - agg.foregroundTimeMs).coerceAtLeast(0L)

            // Count dangerous permissions
            val permCount = try {
                @Suppress("DEPRECATION")
                val pkgInfo = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
                pkgInfo.requestedPermissions?.count { it in dangerousPermissions } ?: 0
            } catch (_: PackageManager.NameNotFoundException) {
                0
            }

            // Network bytes — wrapped in try/catch since we may not have permission
            val networkBytes = 0L // Network stats require additional permissions; placeholder

            // Composite drain score
            val timeScore = (agg.totalTimeMs.toDouble() / maxTotalTime * 100).toInt()
            val networkScore = 0 // no network data available without NETWORK_STATS permission
            val permScore = (permCount.toDouble() / maxDangerousPerms * 100).toInt()
            val bgRatio = if (agg.totalTimeMs > 0) {
                (backgroundTimeMs.toDouble() / agg.totalTimeMs * 100).toInt()
            } else 0

            val drainScore = (
                timeScore * 0.40 +
                networkScore * 0.30 +
                permScore * 0.20 +
                bgRatio * 0.10
            ).toInt().coerceIn(0, 100)

            // Rough mAh estimate: ~300mA screen-on, ~50mA background
            val fgHours = agg.foregroundTimeMs / 3600_000f
            val bgHours = backgroundTimeMs / 3600_000f
            val estimatedMah = fgHours * 300f + bgHours * 50f
            // Scale to 24h
            val scaleFactor = if (hours > 0) 24f / hours else 1f
            val mahPerDay = estimatedMah * scaleFactor

            val category = when {
                drainScore > 80 -> DrainCategory.EXTREME
                drainScore > 60 -> DrainCategory.HIGH
                drainScore > 35 -> DrainCategory.MODERATE
                drainScore > 15 -> DrainCategory.LOW
                else -> DrainCategory.MINIMAL
            }

            AppDrainEstimate(
                packageName = pkg,
                appName = appName,
                drainScore = drainScore,
                foregroundTimeMs = agg.foregroundTimeMs,
                backgroundTimeMs = backgroundTimeMs,
                networkBytesTotal = networkBytes,
                dangerousPermCount = permCount,
                drainCategory = category,
                estimatedMahPerDay = mahPerDay
            )
        }
            .sortedByDescending { it.drainScore }
            .take(30)
    }
}
