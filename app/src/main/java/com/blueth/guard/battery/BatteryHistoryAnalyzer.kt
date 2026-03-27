package com.blueth.guard.battery

import com.blueth.guard.data.local.BatterySnapshot
import javax.inject.Inject
import javax.inject.Singleton

data class BatteryHistoryReport(
    val estimatedChargeCycles: Float,
    val averageTemperature: Float,
    val maxTemperature: Float,
    val averageChargesPerDay: Float,
    val overnightChargingCount: Int,
    val degradationTrend: DegradationTrend?,
    val hasEnoughData: Boolean,
    val disclaimer: String?
)

enum class DegradationTrend {
    STABLE, SLIGHT_DECLINE, DECLINING, RAPID_DECLINE
}

@Singleton
class BatteryHistoryAnalyzer @Inject constructor() {

    fun analyze(snapshots: List<BatterySnapshot>): BatteryHistoryReport {
        if (snapshots.size < 2) {
            return BatteryHistoryReport(
                estimatedChargeCycles = 0f,
                averageTemperature = 0f,
                maxTemperature = 0f,
                averageChargesPerDay = 0f,
                overnightChargingCount = 0,
                degradationTrend = null,
                hasEnoughData = false,
                disclaimer = "Not enough battery data. Keep using the app to build history."
            )
        }

        val sorted = snapshots.sortedBy { it.timestamp }

        // Temperature stats
        val avgTemp = sorted.map { it.temperature }.average().toFloat()
        val maxTemp = sorted.maxOf { it.temperature }

        // Charge cycle estimation
        var chargeCycles = 0f
        var lastCharging = sorted.first().isCharging
        var cycleStartLevel = sorted.first().levelPercent
        for (snap in sorted) {
            if (lastCharging && !snap.isCharging) {
                // Transition from charging to discharging
                val charged = (snap.levelPercent - cycleStartLevel).coerceAtLeast(0)
                chargeCycles += charged / 100f
            }
            if (!lastCharging && snap.isCharging) {
                cycleStartLevel = snap.levelPercent
            }
            lastCharging = snap.isCharging
        }

        // Charges per day
        val timeSpanMs = sorted.last().timestamp - sorted.first().timestamp
        val days = (timeSpanMs / 86_400_000f).coerceAtLeast(1f)
        val chargeTransitions = sorted.windowed(2).count { (a, b) -> !a.isCharging && b.isCharging }
        val chargesPerDay = chargeTransitions / days

        // Overnight charging (charging between 10 PM and 6 AM)
        val overnightCount = sorted.count { snap ->
            snap.isCharging && run {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = snap.timestamp }
                val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                hour >= 22 || hour < 6
            }
        }

        // Degradation trend (need 7+ days)
        val degradationTrend = if (days >= 7) {
            val halfIdx = sorted.size / 2
            val firstAvg = sorted.take(halfIdx).map { it.healthScore }.average()
            val secondAvg = sorted.drop(halfIdx).map { it.healthScore }.average()
            val diff = firstAvg - secondAvg
            when {
                diff < 1 -> DegradationTrend.STABLE
                diff < 3 -> DegradationTrend.SLIGHT_DECLINE
                diff < 8 -> DegradationTrend.DECLINING
                else -> DegradationTrend.RAPID_DECLINE
            }
        } else null

        val disclaimer = if (days < 7)
            "Limited data (${days.toInt()} day(s)). Charge cycle estimates improve with more history."
        else null

        return BatteryHistoryReport(
            estimatedChargeCycles = chargeCycles,
            averageTemperature = avgTemp,
            maxTemperature = maxTemp,
            averageChargesPerDay = chargesPerDay,
            overnightChargingCount = overnightCount,
            degradationTrend = degradationTrend,
            hasEnoughData = snapshots.size >= 10,
            disclaimer = disclaimer
        )
    }
}
