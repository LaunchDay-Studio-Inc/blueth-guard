package com.blueth.guard.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueth.guard.battery.BatteryHealthAnalyzer
import com.blueth.guard.battery.DrainRanker
import com.blueth.guard.data.prefs.UserPreferences
import com.blueth.guard.data.repository.AppRepository
import com.blueth.guard.optimizer.StorageAnalyzer
import com.blueth.guard.privacy.PrivacyScorer
import com.blueth.guard.scanner.SecurityScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val isLoading: Boolean = true,

    // Overall health score (0-100, higher = better)
    val overallScore: Int = 0,
    val scoreLabel: String = "Unknown",

    // Security summary
    val lastScanTime: Long? = null,
    val totalAppsScanned: Int = 0,
    val riskyAppsCount: Int = 0,
    val trackersFound: Int = 0,
    val protectionEnabled: Boolean = false,

    // Privacy summary
    val privacyScore: Int = 0,
    val appsWithDangerousPermissions: Int = 0,
    val recentInstalls: Int = 0,

    // Battery summary
    val batteryLevel: Int = 0,
    val batteryHealth: String = "Unknown",
    val isCharging: Boolean = false,
    val temperature: Float = 0f,
    val topDrainer: String? = null,

    // Storage summary
    val totalStorage: Long = 0L,
    val usedStorage: Long = 0L,
    val cacheSize: Long = 0L,

    // Quick status flags
    val hasUnresolvedThreats: Boolean = false,
    val hasScheduledScans: Boolean = false,
    val installScanEnabled: Boolean = true,

    // Error tracking per module
    val securityError: Boolean = false,
    val privacyError: Boolean = false,
    val batteryError: Boolean = false,
    val storageError: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val securityScanner: SecurityScanner,
    private val batteryHealthAnalyzer: BatteryHealthAnalyzer,
    private val drainRanker: DrainRanker,
    private val privacyScorer: PrivacyScorer,
    private val storageAnalyzer: StorageAnalyzer,
    private val appRepository: AppRepository,
    private val userPreferences: UserPreferences,
    private val application: Application
) : ViewModel() {

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    init {
        loadDashboard()
    }

    fun refresh() {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _dashboardState.value = _dashboardState.value.copy(isLoading = true)

            var securityScore = 50 // default unknown
            var lastScanTime: Long? = null
            var totalAppsScanned = 0
            var riskyAppsCount = 0
            var trackersFound = 0
            var securityError = false

            var privacyScore = 50
            var appsWithDangerousPermissions = 0
            var privacyError = false

            var batteryLevel = 0
            var batteryHealth = "Unknown"
            var isCharging = false
            var temperature = 0f
            var topDrainer: String? = null
            var batteryHealthScore = 50
            var batteryError = false

            var totalStorage = 0L
            var usedStorage = 0L
            var cacheSize = 0L
            var storageError = false

            // Security
            try {
                val results = securityScanner.getLastScanResults()
                if (results.isNotEmpty()) {
                    totalAppsScanned = results.size
                    riskyAppsCount = results.count {
                        it.threatAssessment.riskLevel.name in listOf("HIGH", "CRITICAL")
                    }
                    trackersFound = results.sumOf { it.detectedTrackers.size }
                    lastScanTime = results.maxOfOrNull { it.scanTimestamp }
                    // Average threat score inverted: threatAssessment.overallScore is 0-100 where higher = more threat
                    val avgThreatScore = results.map { it.threatAssessment.overallScore }.average()
                    securityScore = (100 - avgThreatScore).toInt().coerceIn(0, 100)
                }
            } catch (_: Exception) {
                securityError = true
            }

            // Privacy
            try {
                val deviceScore = privacyScorer.calculateDeviceScore()
                privacyScore = deviceScore.overallScore.coerceIn(0, 100)
                appsWithDangerousPermissions = deviceScore.stats.highRiskApps
            } catch (_: Exception) {
                privacyError = true
            }

            // Battery
            try {
                val health = batteryHealthAnalyzer.analyze()
                batteryLevel = health.levelPercent
                batteryHealth = health.health
                isCharging = health.isCharging
                temperature = health.temperature
                batteryHealthScore = when (health.health) {
                    "Good" -> 100
                    "Overheat" -> 50
                    "Cold" -> 70
                    "Dead" -> 0
                    else -> 70
                }
            } catch (_: Exception) {
                batteryError = true
            }

            try {
                val drainers = drainRanker.rankByDrain()
                topDrainer = drainers.firstOrNull()?.appName
            } catch (_: Exception) {
                // Non-critical, ignore
            }

            // Storage
            try {
                val breakdown = storageAnalyzer.analyze()
                totalStorage = breakdown.totalBytes
                usedStorage = breakdown.usedBytes
                cacheSize = breakdown.cacheBytes
            } catch (_: Exception) {
                storageError = true
            }

            // Preferences
            val protectionEnabled = try {
                userPreferences.realTimeProtection.first()
            } catch (_: Exception) { false }

            val hasScheduledScans = try {
                userPreferences.scanScheduleEnabled.first()
            } catch (_: Exception) { false }

            val installScanEnabled = try {
                userPreferences.installScanEnabled.first()
            } catch (_: Exception) { true }

            // Compute overall score
            val storageScore = if (totalStorage > 0) {
                val freePercent = ((totalStorage - usedStorage).toFloat() / totalStorage * 100).toInt()
                (freePercent * 2).coerceIn(0, 100) // >50% free = 100
            } else 50

            val overallScore = (
                securityScore * 0.40 +
                privacyScore * 0.30 +
                batteryHealthScore * 0.15 +
                storageScore * 0.15
            ).toInt().coerceIn(0, 100)

            val scoreLabel = when {
                lastScanTime == null && !securityError -> "Run your first scan"
                overallScore >= 90 -> "Excellent"
                overallScore >= 75 -> "Good"
                overallScore >= 50 -> "Fair"
                overallScore >= 25 -> "Needs Attention"
                else -> "At Risk"
            }

            _dashboardState.value = DashboardState(
                isLoading = false,
                overallScore = overallScore,
                scoreLabel = scoreLabel,
                lastScanTime = lastScanTime,
                totalAppsScanned = totalAppsScanned,
                riskyAppsCount = riskyAppsCount,
                trackersFound = trackersFound,
                protectionEnabled = protectionEnabled,
                privacyScore = privacyScore,
                appsWithDangerousPermissions = appsWithDangerousPermissions,
                recentInstalls = 0,
                batteryLevel = batteryLevel,
                batteryHealth = batteryHealth,
                isCharging = isCharging,
                temperature = temperature,
                topDrainer = topDrainer,
                totalStorage = totalStorage,
                usedStorage = usedStorage,
                cacheSize = cacheSize,
                hasUnresolvedThreats = riskyAppsCount > 0,
                hasScheduledScans = hasScheduledScans,
                installScanEnabled = installScanEnabled,
                securityError = securityError,
                privacyError = privacyError,
                batteryError = batteryError,
                storageError = storageError
            )
        }
    }
}
