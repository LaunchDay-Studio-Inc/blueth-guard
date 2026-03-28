package com.blueth.guard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.AppOpsManager
import android.app.Application
import com.blueth.guard.data.local.InstallEvent
import com.blueth.guard.data.local.NetworkUsageSummary
import com.blueth.guard.data.local.NetworkUsageTotals
import com.blueth.guard.data.local.PermissionEvent
import com.blueth.guard.data.model.AppInfo
import com.blueth.guard.data.repository.AppRepository
import com.blueth.guard.privacy.AppPrivacyProfile
import com.blueth.guard.privacy.AppPrivacyScore
import com.blueth.guard.privacy.ClipboardEvent
import com.blueth.guard.privacy.ClipboardGuard
import com.blueth.guard.privacy.ClipboardStatus
import com.blueth.guard.privacy.DevicePrivacyScore
import com.blueth.guard.privacy.InstallGuard
import com.blueth.guard.privacy.NetworkMonitor
import com.blueth.guard.privacy.PermissionDiffCalculator
import com.blueth.guard.privacy.PermissionMonitor
import com.blueth.guard.privacy.PrivacyRecommendation
import com.blueth.guard.privacy.PrivacyScorer
import com.blueth.guard.privacy.SuspiciousNetworkApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PrivacyTab { OVERVIEW, PERMISSIONS, NETWORK, CLIPBOARD, INSTALLS }

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val permissionMonitor: PermissionMonitor,
    private val networkMonitor: NetworkMonitor,
    private val clipboardGuard: ClipboardGuard,
    private val installGuard: InstallGuard,
    private val privacyScorer: PrivacyScorer,
    private val appRepository: AppRepository,
    private val permissionDiffCalculator: PermissionDiffCalculator,
    private val application: Application
) : ViewModel() {

    private val _privacyTab = MutableStateFlow(PrivacyTab.OVERVIEW)
    val privacyTab: StateFlow<PrivacyTab> = _privacyTab.asStateFlow()

    private val _devicePrivacyScore = MutableStateFlow<DevicePrivacyScore?>(null)
    val devicePrivacyScore: StateFlow<DevicePrivacyScore?> = _devicePrivacyScore.asStateFlow()

    private val _permissionEvents = MutableStateFlow<List<PermissionEvent>>(emptyList())
    val permissionEvents: StateFlow<List<PermissionEvent>> = _permissionEvents.asStateFlow()

    private val _topDataConsumers = MutableStateFlow<List<NetworkUsageSummary>>(emptyList())
    val topDataConsumers: StateFlow<List<NetworkUsageSummary>> = _topDataConsumers.asStateFlow()

    private val _networkTotals = MutableStateFlow<NetworkUsageTotals?>(null)
    val networkTotals: StateFlow<NetworkUsageTotals?> = _networkTotals.asStateFlow()

    private val _clipboardStatus = MutableStateFlow(ClipboardStatus(false, 0, 0, null))
    val clipboardStatus: StateFlow<ClipboardStatus> = _clipboardStatus.asStateFlow()

    private val _clipboardEvents = MutableStateFlow<List<ClipboardEvent>>(emptyList())
    val clipboardEvents: StateFlow<List<ClipboardEvent>> = _clipboardEvents.asStateFlow()

    private val _installEvents = MutableStateFlow<List<InstallEvent>>(emptyList())
    val installEvents: StateFlow<List<InstallEvent>> = _installEvents.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _permissionHeatmapApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val permissionHeatmapApps: StateFlow<List<AppInfo>> = _permissionHeatmapApps.asStateFlow()

    private val _appPrivacyScores = MutableStateFlow<List<AppPrivacyScore>>(emptyList())
    val appPrivacyScores: StateFlow<List<AppPrivacyScore>> = _appPrivacyScores.asStateFlow()

    private val _recommendations = MutableStateFlow<List<PrivacyRecommendation>>(emptyList())
    val recommendations: StateFlow<List<PrivacyRecommendation>> = _recommendations.asStateFlow()

    private val _dangerousApps = MutableStateFlow<List<AppPrivacyProfile>>(emptyList())
    val dangerousApps: StateFlow<List<AppPrivacyProfile>> = _dangerousApps.asStateFlow()

    private val _suspiciousApps = MutableStateFlow<List<SuspiciousNetworkApp>>(emptyList())
    val suspiciousApps: StateFlow<List<SuspiciousNetworkApp>> = _suspiciousApps.asStateFlow()

    private val _networkTimeRange = MutableStateFlow(24)
    val networkTimeRange: StateFlow<Int> = _networkTimeRange.asStateFlow()

    private val _permissionDiffs = MutableStateFlow<List<PermissionDiffCalculator.PermissionDiff>>(emptyList())
    val permissionDiffs: StateFlow<List<PermissionDiffCalculator.PermissionDiff>> = _permissionDiffs.asStateFlow()

    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    init {
        checkUsageAccess()
        loadAll()
    }

    private fun checkUsageAccess() {
        try {
            val appOps = application.getSystemService(android.content.Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    application.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    application.packageName
                )
            }
            _hasUsageAccess.value = mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            _hasUsageAccess.value = false
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load privacy scores
                launch(Dispatchers.IO) {
                    val score = privacyScorer.calculateDeviceScore()
                    _devicePrivacyScore.value = score
                    _appPrivacyScores.value = score.appScores
                    _recommendations.value = score.topRecommendations
                }

                // Load heatmap apps
                launch(Dispatchers.IO) {
                    val apps = appRepository.getInstalledApps()
                    _permissionHeatmapApps.value = apps.filter {
                        !it.isSystem && it.permissions.isNotEmpty()
                    }
                }

                // Load permission events
                launch(Dispatchers.IO) {
                    permissionMonitor.getRecentActivity(24).collect { events ->
                        _permissionEvents.value = events
                    }
                }

                // Load dangerous apps
                launch(Dispatchers.IO) {
                    _dangerousApps.value = permissionMonitor.getMostDangerousApps()
                }

                // Load network data
                launch(Dispatchers.IO) {
                    networkMonitor.getTopDataConsumers(_networkTimeRange.value).collect { consumers ->
                        _topDataConsumers.value = consumers
                        _suspiciousApps.value = networkMonitor.flagSuspiciousUsage(consumers)
                    }
                }
                launch(Dispatchers.IO) {
                    networkMonitor.getTotalUsage(_networkTimeRange.value).collect { totals ->
                        _networkTotals.value = totals
                    }
                }

                // Load install events
                launch(Dispatchers.IO) {
                    installGuard.seedInstalledApps()
                    installGuard.getInstallHistory(30).collect { events ->
                        _installEvents.value = events
                    }
                }

                // Load clipboard status
                launch {
                    _clipboardStatus.value = clipboardGuard.getClipboardStatus()
                    _clipboardEvents.value = clipboardGuard.getRecentClipboardEvents()
                }

                // Load permission diffs
                launch(Dispatchers.IO) {
                    _permissionDiffs.value = permissionDiffCalculator.calculateDiffs()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshPermissions() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            permissionMonitor.snapshotCurrentPermissions()
            _permissionEvents.value = permissionMonitor.getRecentActivity(24).first()
            _dangerousApps.value = permissionMonitor.getMostDangerousApps()
            _isLoading.value = false
        }
    }

    fun refreshNetworkStats() {
        checkUsageAccess()
        if (!_hasUsageAccess.value) {
            _topDataConsumers.value = emptyList()
            _networkTotals.value = null
            _suspiciousApps.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            // Clear stale data first
            _topDataConsumers.value = emptyList()
            _networkTotals.value = null
            networkMonitor.collectNetworkStats(_networkTimeRange.value)
            _topDataConsumers.value = networkMonitor.getTopDataConsumers(_networkTimeRange.value).first()
            _networkTotals.value = networkMonitor.getTotalUsage(_networkTimeRange.value).first()
            _suspiciousApps.value = networkMonitor.flagSuspiciousUsage(_topDataConsumers.value)
            _isLoading.value = false
        }
    }

    fun setNetworkTimeRange(hours: Int) {
        _networkTimeRange.value = hours
        refreshNetworkStats()
    }

    fun startClipboardMonitoring() {
        clipboardGuard.startMonitoring()
        updateClipboardState()
    }

    fun stopClipboardMonitoring() {
        clipboardGuard.stopMonitoring()
        updateClipboardState()
    }

    fun clearClipboardHistory() {
        clipboardGuard.clearHistory()
        updateClipboardState()
    }

    fun setTab(tab: PrivacyTab) {
        _privacyTab.value = tab
    }

    fun getAppsWithPermission(permission: String): List<AppInfo> {
        return _permissionHeatmapApps.value.filter { app ->
            app.permissions.contains(permission)
        }
    }

    fun getSideloadedApps(): List<AppInfo> {
        return _permissionHeatmapApps.value.filter { app ->
            app.installSource != "com.android.vending" &&
                app.installSource != "com.google.android.packageinstaller" &&
                app.installSource != "Unknown"
        }
    }

    fun getHighRiskApps(): List<AppPrivacyScore> {
        return _appPrivacyScores.value.filter { it.overallScore < 30 }
    }

    private fun updateClipboardState() {
        _clipboardStatus.value = clipboardGuard.getClipboardStatus()
        _clipboardEvents.value = clipboardGuard.getRecentClipboardEvents()
    }
}
