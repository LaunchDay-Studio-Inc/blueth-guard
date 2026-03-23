package com.blueth.guard.battery

import android.app.Application
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

data class WakelockInfo(
    val packageName: String,
    val appName: String,
    val wakelockScore: Int,
    val foregroundTimeMs: Long,
    val wakeCount: Int,
    val lastWakeTimestamp: Long,
    val severity: WakelockSeverity
)

enum class WakelockSeverity { LOW, MEDIUM, HIGH, CRITICAL }

@Singleton
class WakelockDetector @Inject constructor(
    private val app: Application
) {

    private val excludedPackages = setOf(
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.android.systemui",
        "com.android.settings"
    )

    suspend fun analyzeWakelocks(hours: Int = 24): List<WakelockInfo> {
        val usageStatsManager =
            app.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyList()

        val endTime = System.currentTimeMillis()
        val startTime = endTime - hours * 3600_000L

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        data class Tracker(
            var foregroundStartMs: Long = 0L,
            var totalForegroundMs: Long = 0L,
            var wakeCount: Int = 0,
            var lastWakeTimestamp: Long = 0L
        )

        val trackers = mutableMapOf<String, Tracker>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue

            if (pkg in excludedPackages) continue

            val tracker = trackers.getOrPut(pkg) { Tracker() }

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    tracker.foregroundStartMs = event.timeStamp
                    tracker.wakeCount++
                    tracker.lastWakeTimestamp = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (tracker.foregroundStartMs > 0) {
                        tracker.totalForegroundMs += event.timeStamp - tracker.foregroundStartMs
                        tracker.foregroundStartMs = 0L
                    }
                }
            }
        }

        // Close any still-open foreground sessions
        for ((_, tracker) in trackers) {
            if (tracker.foregroundStartMs > 0) {
                tracker.totalForegroundMs += endTime - tracker.foregroundStartMs
                tracker.foregroundStartMs = 0L
            }
        }

        val maxForeground = trackers.values.maxOfOrNull { it.totalForegroundMs } ?: 1L

        val pm = app.packageManager

        return trackers
            .filter { (_, t) -> t.totalForegroundMs > 0 || t.wakeCount > 0 }
            .map { (pkg, t) ->
                val appName = try {
                    @Suppress("DEPRECATION")
                    pm.getApplicationLabel(
                        pm.getApplicationInfo(pkg, 0)
                    ).toString()
                } catch (_: PackageManager.NameNotFoundException) {
                    pkg
                }

                val score = ((t.totalForegroundMs.toDouble() / maxForeground) * 100).toInt()
                    .coerceIn(0, 100)

                val severity = when {
                    t.totalForegroundMs > 4 * 3600_000L || t.wakeCount > 100 -> WakelockSeverity.CRITICAL
                    t.totalForegroundMs > 2 * 3600_000L || t.wakeCount > 50 -> WakelockSeverity.HIGH
                    t.totalForegroundMs > 30 * 60_000L || t.wakeCount > 20 -> WakelockSeverity.MEDIUM
                    else -> WakelockSeverity.LOW
                }

                WakelockInfo(
                    packageName = pkg,
                    appName = appName,
                    wakelockScore = score,
                    foregroundTimeMs = t.totalForegroundMs,
                    wakeCount = t.wakeCount,
                    lastWakeTimestamp = t.lastWakeTimestamp,
                    severity = severity
                )
            }
            .sortedByDescending { it.wakelockScore }
    }
}
