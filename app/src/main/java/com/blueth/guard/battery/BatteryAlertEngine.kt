package com.blueth.guard.battery

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryAlertEngine @Inject constructor(
    private val application: Application
) {
    data class BatteryAlert(
        val type: AlertType,
        val title: String,
        val description: String,
        val suggestion: String,
        val severity: Severity,
        val actionLabel: String? = null,
        val actionIntent: Intent? = null
    )

    enum class AlertType { HIGH_TEMP, POOR_HEALTH, EXCESSIVE_DRAIN, WAKELOCK_ABUSE, LOW_BATTERY }
    enum class Severity { INFO, WARNING, CRITICAL }

    fun generateAlerts(): List<BatteryAlert> {
        val alerts = mutableListOf<BatteryAlert>()

        val batteryIntent = application.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Check battery temperature
        val temp = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        if (temp > 40f) {
            alerts.add(
                BatteryAlert(
                    type = AlertType.HIGH_TEMP,
                    title = "Battery Temperature High",
                    description = "Your battery is at ${temp}°C — above the safe threshold of 40°C.",
                    suggestion = "Close heavy apps, remove phone case, avoid charging while using.",
                    severity = if (temp > 45f) Severity.CRITICAL else Severity.WARNING
                )
            )
        }

        // Check battery health
        val healthInt = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN
        ) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        if (healthInt != BatteryManager.BATTERY_HEALTH_GOOD && healthInt != BatteryManager.BATTERY_HEALTH_UNKNOWN) {
            alerts.add(
                BatteryAlert(
                    type = AlertType.POOR_HEALTH,
                    title = "Battery Health Degraded",
                    description = "Your battery health is below optimal. Performance may be affected.",
                    suggestion = "Consider having the battery replaced by an authorized service center.",
                    severity = Severity.WARNING
                )
            )
        }

        // Check battery level
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (scale > 0) (level * 100) / scale else 0
        if (pct in 1..15) {
            alerts.add(
                BatteryAlert(
                    type = AlertType.LOW_BATTERY,
                    title = "Low Battery",
                    description = "Battery is at $pct% — plug in soon.",
                    suggestion = "Enable battery saver mode to extend remaining time.",
                    severity = if (pct <= 5) Severity.CRITICAL else Severity.WARNING,
                    actionLabel = "Battery Saver",
                    actionIntent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            )
        }

        return alerts
    }
}
