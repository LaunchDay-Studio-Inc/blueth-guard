package com.blueth.guard.battery

import android.app.Application
import com.blueth.guard.optimizer.AppHibernator
import javax.inject.Inject
import javax.inject.Singleton

enum class PowerProfile {
    NORMAL, POWER_SAVER, ULTRA_SAVER
}

data class PowerProfileConfig(
    val profile: PowerProfile,
    val label: String,
    val description: String,
    val autoTriggerPercent: Int
)

data class ProfileActivationResult(
    val profile: PowerProfile,
    val appsHibernated: Int,
    val memoryFreedMb: Long,
    val suggestions: List<String>
)

@Singleton
class PowerProfileManager @Inject constructor(
    private val app: Application,
    private val appHibernator: AppHibernator
) {
    val profiles = listOf(
        PowerProfileConfig(PowerProfile.NORMAL, "Normal", "No restrictions", 100),
        PowerProfileConfig(PowerProfile.POWER_SAVER, "Power Saver", "Hibernate non-essential apps", 30),
        PowerProfileConfig(PowerProfile.ULTRA_SAVER, "Ultra Saver", "Aggressive hibernation + suggestions", 15)
    )

    var activeProfile: PowerProfile = PowerProfile.NORMAL
        private set

    fun activateProfile(profile: PowerProfile, hibernateList: List<String> = emptyList()): ProfileActivationResult {
        activeProfile = profile

        val suggestions = mutableListOf<String>()
        var appsHibernated = 0
        var memoryFreedMb = 0L

        when (profile) {
            PowerProfile.NORMAL -> {
                // No action
            }
            PowerProfile.POWER_SAVER -> {
                if (hibernateList.isNotEmpty()) {
                    val result = appHibernator.hibernateApps(hibernateList)
                    appsHibernated = result.appsHibernated
                    memoryFreedMb = result.memoryFreedKb / 1024
                }
                suggestions.add("Consider reducing screen brightness")
                suggestions.add("Disable unused connectivity (Bluetooth, NFC)")
            }
            PowerProfile.ULTRA_SAVER -> {
                if (hibernateList.isNotEmpty()) {
                    val result = appHibernator.hibernateApps(hibernateList)
                    appsHibernated = result.appsHibernated
                    memoryFreedMb = result.memoryFreedKb / 1024
                }
                suggestions.add("Turn off Wi-Fi when not in use")
                suggestions.add("Turn off Bluetooth")
                suggestions.add("Turn off Location services")
                suggestions.add("Enable battery saver in system settings")
                suggestions.add("Reduce screen timeout to 30 seconds")
            }
        }

        return ProfileActivationResult(
            profile = profile,
            appsHibernated = appsHibernated,
            memoryFreedMb = memoryFreedMb,
            suggestions = suggestions
        )
    }

    fun shouldAutoActivate(batteryPercent: Int): PowerProfile? {
        return when {
            batteryPercent <= 15 && activeProfile != PowerProfile.ULTRA_SAVER -> PowerProfile.ULTRA_SAVER
            batteryPercent <= 30 && activeProfile == PowerProfile.NORMAL -> PowerProfile.POWER_SAVER
            else -> null
        }
    }
}
