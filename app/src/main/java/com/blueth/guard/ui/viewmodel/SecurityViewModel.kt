package com.blueth.guard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueth.guard.data.local.ScanHistoryDao
import com.blueth.guard.data.local.ScanHistoryEntry
import com.blueth.guard.scanner.AppScanResult
import com.blueth.guard.scanner.DeepFileScanner
import com.blueth.guard.scanner.DeepScanProgress
import com.blueth.guard.scanner.DeepScanStats
import com.blueth.guard.scanner.DeviceAdminAppInfo
import com.blueth.guard.scanner.DeviceAdminChecker
import com.blueth.guard.scanner.FileScanResult
import com.blueth.guard.scanner.RiskLevel
import com.blueth.guard.scanner.ScanProgress
import com.blueth.guard.scanner.SecurityScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

enum class ScanState { IDLE, SCANNING, COMPLETE }
enum class DeepScanState { IDLE, SCANNING, COMPLETE }

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityScanner: SecurityScanner,
    private val deviceAdminChecker: DeviceAdminChecker,
    private val scanHistoryDao: ScanHistoryDao,
    private val deepFileScanner: DeepFileScanner
) : ViewModel() {

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    val scanProgress: StateFlow<ScanProgress?> = _scanProgress.asStateFlow()

    private val _scanResults = MutableStateFlow<List<AppScanResult>>(emptyList())
    val scanResults: StateFlow<List<AppScanResult>> = _scanResults.asStateFlow()

    private val _overallDeviceScore = MutableStateFlow(0)
    val overallDeviceScore: StateFlow<Int> = _overallDeviceScore.asStateFlow()

    private val _deviceAdmins = MutableStateFlow<List<DeviceAdminAppInfo>>(emptyList())
    val deviceAdmins: StateFlow<List<DeviceAdminAppInfo>> = _deviceAdmins.asStateFlow()

    // Deep file scan
    private val _deepScanState = MutableStateFlow(DeepScanState.IDLE)
    val deepScanState: StateFlow<DeepScanState> = _deepScanState.asStateFlow()

    private val _deepScanProgress = MutableStateFlow<DeepScanProgress?>(null)
    val deepScanProgress: StateFlow<DeepScanProgress?> = _deepScanProgress.asStateFlow()

    private val _deepScanResults = MutableStateFlow<List<FileScanResult>>(emptyList())
    val deepScanResults: StateFlow<List<FileScanResult>> = _deepScanResults.asStateFlow()

    private val _deepScanStats = MutableStateFlow<DeepScanStats?>(null)
    val deepScanStats: StateFlow<DeepScanStats?> = _deepScanStats.asStateFlow()

    fun startFullScan() {
        if (_scanState.value == ScanState.SCANNING) return

        viewModelScope.launch {
            _scanState.value = ScanState.SCANNING
            _scanResults.value = emptyList()
            val scanStartTime = System.currentTimeMillis()

            securityScanner.scanAll().collect { progress ->
                _scanProgress.value = progress
                _scanResults.value = progress.results
            }

            val results = securityScanner.getLastScanResults()
            _scanResults.value = results
            _overallDeviceScore.value = calculateDeviceScore(results)
            _deviceAdmins.value = deviceAdminChecker.getDeviceAdmins()
            _scanState.value = ScanState.COMPLETE

            // Save scan history
            val scanDuration = System.currentTimeMillis() - scanStartTime
            val flaggedResults = results.filter {
                it.threatAssessment.riskLevel != RiskLevel.SAFE
            }
            val flaggedJson = buildJsonArray {
                flaggedResults.forEach { result ->
                    add(buildJsonObject {
                        put("pkg", result.appInfo.packageName)
                        put("risk", result.threatAssessment.riskLevel.name)
                    })
                }
            }.toString()

            scanHistoryDao.insert(
                ScanHistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    totalAppsScanned = results.size,
                    threatsFound = results.count {
                        it.threatAssessment.riskLevel == RiskLevel.HIGH ||
                                it.threatAssessment.riskLevel == RiskLevel.CRITICAL
                    },
                    trackersFound = results.sumOf { it.detectedTrackers.size },
                    overallScore = (100 - _overallDeviceScore.value).coerceIn(0, 100),
                    flaggedApps = flaggedJson,
                    scanDurationMs = scanDuration
                )
            )
        }
    }

    private fun calculateDeviceScore(results: List<AppScanResult>): Int {
        if (results.isEmpty()) return 0
        val weightedSum = results.sumOf { result ->
            val weight = when (result.threatAssessment.riskLevel) {
                RiskLevel.CRITICAL -> 3.0
                RiskLevel.HIGH -> 2.0
                RiskLevel.MEDIUM -> 1.5
                RiskLevel.LOW -> 1.0
                RiskLevel.SAFE -> 0.5
            }
            (result.threatAssessment.overallScore * weight).toInt()
        }
        val totalWeight = results.sumOf { result ->
            when (result.threatAssessment.riskLevel) {
                RiskLevel.CRITICAL -> 3.0
                RiskLevel.HIGH -> 2.0
                RiskLevel.MEDIUM -> 1.5
                RiskLevel.LOW -> 1.0
                RiskLevel.SAFE -> 0.5
            }
        }
        return if (totalWeight > 0) (weightedSum / totalWeight).toInt() else 0
    }

    fun startDeepScan() {
        if (_deepScanState.value == DeepScanState.SCANNING) return
        viewModelScope.launch {
            _deepScanState.value = DeepScanState.SCANNING
            _deepScanResults.value = emptyList()
            _deepScanStats.value = null

            val (results, stats) = deepFileScanner.scan { progress ->
                _deepScanProgress.value = progress
            }

            _deepScanResults.value = results
            _deepScanStats.value = stats
            _deepScanState.value = DeepScanState.COMPLETE
        }
    }
}
