package com.blueth.guard.battery

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import javax.inject.Inject
import javax.inject.Singleton

data class BatteryHealth(
    val levelPercent: Int,
    val temperature: Float,
    val voltage: Float,
    val health: String,
    val technology: String,
    val isCharging: Boolean,
    val chargeSource: String,
    val healthScore: Int,
    val healthLabel: String,
    val healthDisclaimer: String?,
    val recommendations: List<String>,
    val chargeCounter: Int = 0,
    val energyCounter: Long = 0,
    val currentNow: Int = 0
)

@Singleton
class BatteryHealthAnalyzer @Inject constructor(
    private val app: Application
) {

    fun analyze(): BatteryHealth {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            app.registerReceiver(null, filter)
        }

        // Use BATTERY_PROPERTY_CAPACITY as primary (system's displayed percentage)
        val batteryManager = app.getSystemService(android.content.Context.BATTERY_SERVICE) as? BatteryManager
        val capacityPercent = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val rawPercent = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val levelPercent = if (capacityPercent > 0) capacityPercent else rawPercent

        val temperature =
            (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage =
            (batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000f
        val technology =
            batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

        val healthInt = batteryStatus?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN
        ) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN

        val health = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        val statusInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING ||
                statusInt == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargeSource = when {
            !isCharging -> "Not Charging"
            chargePlug == BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            chargePlug == BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Charging"
        }

        // Health score calculation
        var healthScore = 100
        if (healthInt != BatteryManager.BATTERY_HEALTH_GOOD) healthScore -= 30
        if (temperature > 40f) healthScore -= 15
        else if (temperature > 35f) healthScore -= 10
        if (voltage < 3.2f || voltage > 4.35f) healthScore -= 10

        // Charge cycle estimation from BatteryManager properties
        var hasChargeData = false
        if (batteryManager != null) {
            val chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            val energyCounter = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            if (chargeCounter > 0 && energyCounter > 0) {
                hasChargeData = true
                // Rough estimate: typical battery is ~4000mAh = 4000000 µAh
                // If chargeCounter is low relative to expected, battery may be degraded
                val chargeCounterMah = chargeCounter / 1000
                if (chargeCounterMah in 1..1500 && levelPercent > 50) {
                    // Low charge counter at high level suggests degradation
                    healthScore -= 10
                }
            }
        }

        healthScore = healthScore.coerceIn(0, 100)

        val healthLabel = when {
            healthScore >= 95 -> "Excellent"
            healthScore >= 80 -> "Good"
            healthScore >= 60 -> "Fair"
            healthScore >= 40 -> "Poor"
            else -> "Critical"
        }

        val healthDisclaimer = if (!hasChargeData) {
            "Health score based on current readings only. Battery age and wear cannot be measured without root access."
        } else null

        // Recommendations
        val recommendations = mutableListOf<String>()
        if (temperature > 42f) {
            recommendations.add("Battery is overheating! Close heavy apps and stop charging.")
        } else if (temperature > 38f) {
            recommendations.add("Your battery is warm. Avoid charging while using intensive apps.")
        }
        if (healthInt != BatteryManager.BATTERY_HEALTH_GOOD) {
            recommendations.add("Battery health is degraded. Consider getting it replaced.")
        }
        if (isCharging && chargeSource == "USB") {
            recommendations.add("USB charging is slower and can heat up more. Use AC when possible.")
        }
        if (levelPercent > 80 && isCharging) {
            recommendations.add("Charging above 80% accelerates battery wear. Consider unplugging.")
        }

        // Read additional battery properties
        val chargeCounter = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0
        val energyCounter = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER) ?: 0L
        val currentNow = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0

        return BatteryHealth(
            levelPercent = levelPercent,
            temperature = temperature,
            voltage = voltage,
            health = health,
            technology = technology,
            isCharging = isCharging,
            chargeSource = chargeSource,
            healthScore = healthScore,
            healthLabel = healthLabel,
            healthDisclaimer = healthDisclaimer,
            recommendations = recommendations,
            chargeCounter = chargeCounter,
            energyCounter = energyCounter,
            currentNow = currentNow
        )
    }
}
