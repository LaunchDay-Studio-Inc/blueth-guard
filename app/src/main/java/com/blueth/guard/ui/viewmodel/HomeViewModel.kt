package com.blueth.guard.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueth.guard.battery.BatteryHealthAnalyzer
import com.blueth.guard.battery.DrainRanker
import com.blueth.guard.data.local.InstallEventDao
import com.blueth.guard.data.local.PermissionEventDao
import com.blueth.guard.data.local.ScanHistoryDao
import com.blueth.guard.data.prefs.UserPreferences
import com.blueth.guard.data.repository.AppRepository
import com.blueth.guard.optimizer.ProcessManager
import com.blueth.guard.optimizer.StorageAnalyzer
import com.blueth.guard.privacy.PrivacyScorer
import com.blueth.guard.protection.WifiSecurityChecker
import com.blueth.guard.scanner.SecurityScanner
import com.blueth.guard.scanner.RiskLevel
import com.blueth.guard.widget.WidgetDataSync
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

    // RAM summary
    val ramTotal: Long = 0L,
    val ramUsed: Long = 0L,
    val killableProcessCount: Int = 0,

    // Quick status flags
    val hasUnresolvedThreats: Boolean = false,
    val hasScheduledScans: Boolean = false,
    val installScanEnabled: Boolean = true,

    // Error tracking per module
    val securityError: Boolean = false,
    val privacyError: Boolean = false,
    val batteryError: Boolean = false,
    val storageError: Boolean = false,

    // WiFi security
    val wifiConnected: Boolean = false,
    val wifiSsid: String? = null,
    val wifiSecure: Boolean = true,
    val wifiWarnings: List<String> = emptyList()
)

data class ActivityItem(
    val icon: String,  // emoji or icon type key
    val title: String,
    val subtitle: String,
    val timestamp: Long
)

data class QuickScanReport(
    val totalApps: Int = 0,
    val virusCount: Int = 0,
    val malwareCount: Int = 0,
    val trackerCount: Int = 0,
    val safeCount: Int = 0,
    val lowRiskCount: Int = 0,
    val mediumRiskCount: Int = 0,
    val highRiskCount: Int = 0,
    val criticalCount: Int = 0,
    val overallBadge: String = "Excellent",
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val securityScanner: SecurityScanner,
    private val batteryHealthAnalyzer: BatteryHealthAnalyzer,
    private val drainRanker: DrainRanker,
    private val privacyScorer: PrivacyScorer,
    private val storageAnalyzer: StorageAnalyzer,
    private val processManager: ProcessManager,
    private val wifiSecurityChecker: WifiSecurityChecker,
    private val appRepository: AppRepository,
    private val userPreferences: UserPreferences,
    private val widgetDataSync: WidgetDataSync,
    private val application: Application,
    private val scanHistoryDao: ScanHistoryDao,
    private val installEventDao: InstallEventDao,
    private val permissionEventDao: PermissionEventDao
) : ViewModel() {

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    private val _recentActivities = MutableStateFlow<List<ActivityItem>>(emptyList())
    val recentActivities: StateFlow<List<ActivityItem>> = _recentActivities.asStateFlow()

    private val _quickScanRunning = MutableStateFlow(false)
    val quickScanRunning: StateFlow<Boolean> = _quickScanRunning.asStateFlow()

    private val _quickScanReport = MutableStateFlow<QuickScanReport?>(null)
    val quickScanReport: StateFlow<QuickScanReport?> = _quickScanReport.asStateFlow()

    init {
        loadDashboard()
        loadRecentActivities()
    }

    fun refresh() {
        loadDashboard()
        loadRecentActivities()
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

            // RAM
            var ramTotal = 0L
            var ramUsed = 0L
            var killableProcessCount = 0
            try {
                val (total, available) = processManager.getRamInfo()
                ramTotal = total
                ramUsed = total - available
                killableProcessCount = processManager.getSmartKillList().size
            } catch (_: Exception) { }

            // WiFi Security
            var wifiConnected = false
            var wifiSsid: String? = null
            var wifiSecure = true
            var wifiWarnings = emptyList<String>()
            try {
                val wifiResult = wifiSecurityChecker.check()
                wifiConnected = wifiResult.isConnected
                wifiSsid = wifiResult.ssid
                wifiWarnings = wifiResult.warnings
                wifiSecure = wifiWarnings.isEmpty()
            } catch (_: Exception) { }

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
                ramTotal = ramTotal,
                ramUsed = ramUsed,
                killableProcessCount = killableProcessCount,
                hasUnresolvedThreats = riskyAppsCount > 0,
                hasScheduledScans = hasScheduledScans,
                installScanEnabled = installScanEnabled,
                securityError = securityError,
                privacyError = privacyError,
                batteryError = batteryError,
                storageError = storageError,
                wifiConnected = wifiConnected,
                wifiSsid = wifiSsid,
                wifiSecure = wifiSecure,
                wifiWarnings = wifiWarnings
            )

            // Sync widget data
            try {
                widgetDataSync.updateWidgetData(
                    overallScore = overallScore,
                    lastScanTime = lastScanTime ?: 0L,
                    riskyApps = riskyAppsCount,
                    protectionEnabled = protectionEnabled
                )
            } catch (_: Exception) {
                // Widget sync is non-critical
            }
        }
    }

    private fun loadRecentActivities() {
        viewModelScope.launch {
            val activities = mutableListOf<ActivityItem>()

            try {
                val recentScans = scanHistoryDao.getRecent(5)
                recentScans.forEach { scan ->
                    activities.add(
                        ActivityItem(
                            icon = "scan",
                            title = "Security Scan",
                            subtitle = "${scan.totalAppsScanned} apps scanned, ${scan.threatsFound} threats",
                            timestamp = scan.timestamp
                        )
                    )
                }
            } catch (_: Exception) {}

            try {
                installEventDao.getRecentEvents(10).first().forEach { event ->
                    activities.add(
                        ActivityItem(
                            icon = "install",
                            title = "${event.action.name.lowercase().replaceFirstChar { it.uppercase() }}: ${event.appName}",
                            subtitle = event.packageName,
                            timestamp = event.timestamp
                        )
                    )
                }
            } catch (_: Exception) {}

            try {
                val since = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                permissionEventDao.getRecentEvents(since).first().take(10).forEach { event ->
                    val action = if (event.isGranted) "granted" else "revoked"
                    activities.add(
                        ActivityItem(
                            icon = "permission",
                            title = "Permission $action",
                            subtitle = "${event.appName}: ${event.permission.substringAfterLast('.')}",
                            timestamp = event.timestamp
                        )
                    )
                }
            } catch (_: Exception) {}

            _recentActivities.value = activities.sortedByDescending { it.timestamp }.take(15)
        }
    }

    fun runQuickScan() {
        if (_quickScanRunning.value) return
        viewModelScope.launch {
            _quickScanRunning.value = true
            _quickScanReport.value = null
            try {
                securityScanner.scanAll().collect { /* consume progress */ }
                val results = securityScanner.getLastScanResults()
                val safeCount = results.count { it.threatAssessment.riskLevel == RiskLevel.SAFE }
                val lowCount = results.count { it.threatAssessment.riskLevel == RiskLevel.LOW }
                val medCount = results.count { it.threatAssessment.riskLevel == RiskLevel.MEDIUM }
                val highCount = results.count { it.threatAssessment.riskLevel == RiskLevel.HIGH }
                val critCount = results.count { it.threatAssessment.riskLevel == RiskLevel.CRITICAL }
                val trackerCount = results.sumOf { it.detectedTrackers.size }
                val badge = when {
                    critCount > 0 -> "At Risk"
                    highCount > 0 -> "Fair"
                    medCount > 0 -> "Good"
                    else -> "Excellent"
                }
                _quickScanReport.value = QuickScanReport(
                    totalApps = results.size,
                    virusCount = 0,
                    malwareCount = critCount,
                    trackerCount = trackerCount,
                    safeCount = safeCount,
                    lowRiskCount = lowCount,
                    mediumRiskCount = medCount,
                    highRiskCount = highCount,
                    criticalCount = critCount,
                    overallBadge = badge,
                    timestamp = System.currentTimeMillis()
                )
                // Refresh dashboard after scan
                loadDashboard()
            } catch (_: Exception) {
                _quickScanReport.value = null
            } finally {
                _quickScanRunning.value = false
            }
        }
    }
}
