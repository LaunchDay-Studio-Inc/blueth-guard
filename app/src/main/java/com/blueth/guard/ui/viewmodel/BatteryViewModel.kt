package com.blueth.guard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueth.guard.battery.AppDrainEstimate
import com.blueth.guard.battery.BatteryAlertEngine
import com.blueth.guard.battery.BatteryHealth
import com.blueth.guard.battery.BatteryHealthAnalyzer
import com.blueth.guard.battery.DrainRanker
import com.blueth.guard.battery.RunningServiceInfo
import com.blueth.guard.battery.ServiceMonitor
import com.blueth.guard.battery.WakelockDetector
import com.blueth.guard.battery.WakelockInfo
import com.blueth.guard.data.local.BatterySnapshot
import com.blueth.guard.data.local.BatterySnapshotDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BatteryTab { OVERVIEW, WAKELOCKS, SERVICES, DRAIN }

@HiltViewModel
class BatteryViewModel @Inject constructor(
    private val wakelockDetector: WakelockDetector,
    private val serviceMonitor: ServiceMonitor,
    private val drainRanker: DrainRanker,
    private val batteryHealthAnalyzer: BatteryHealthAnalyzer,
    private val batterySnapshotDao: BatterySnapshotDao,
    private val batteryAlertEngine: BatteryAlertEngine
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(BatteryTab.OVERVIEW)
    val selectedTab: StateFlow<BatteryTab> = _selectedTab.asStateFlow()

    private val _batteryHealth = MutableStateFlow<BatteryHealth?>(null)
    val batteryHealth: StateFlow<BatteryHealth?> = _batteryHealth.asStateFlow()

    private val _wakelocks = MutableStateFlow<List<WakelockInfo>>(emptyList())
    val wakelocks: StateFlow<List<WakelockInfo>> = _wakelocks.asStateFlow()

    private val _services = MutableStateFlow<List<RunningServiceInfo>>(emptyList())
    val services: StateFlow<List<RunningServiceInfo>> = _services.asStateFlow()

    private val _drainEstimates = MutableStateFlow<List<AppDrainEstimate>>(emptyList())
    val drainEstimates: StateFlow<List<AppDrainEstimate>> = _drainEstimates.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastRefreshTimestamp = MutableStateFlow(0L)
    val lastRefreshTimestamp: StateFlow<Long> = _lastRefreshTimestamp.asStateFlow()

    private val _batteryAlerts = MutableStateFlow<List<BatteryAlertEngine.BatteryAlert>>(emptyList())
    val batteryAlerts: StateFlow<List<BatteryAlertEngine.BatteryAlert>> = _batteryAlerts.asStateFlow()

    private val _batteryHistory = MutableStateFlow<List<BatterySnapshot>>(emptyList())
    val batteryHistory: StateFlow<List<BatterySnapshot>> = _batteryHistory.asStateFlow()

    init {
        refresh()
        saveSnapshot()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val healthDeferred = async(Dispatchers.IO) { batteryHealthAnalyzer.analyze() }
                val wakelocksDeferred = async(Dispatchers.IO) { wakelockDetector.analyzeWakelocks() }
                val servicesDeferred = async(Dispatchers.IO) { serviceMonitor.getRunningServices() }
                val drainDeferred = async(Dispatchers.IO) { drainRanker.rankByDrain() }

                _batteryHealth.value = healthDeferred.await()
                _wakelocks.value = wakelocksDeferred.await()
                _services.value = servicesDeferred.await()
                _drainEstimates.value = drainDeferred.await()
                _batteryAlerts.value = batteryAlertEngine.generateAlerts()
                _lastRefreshTimestamp.value = System.currentTimeMillis()

                // Load battery history (last 24 hours)
                val now = System.currentTimeMillis()
                _batteryHistory.value = batterySnapshotDao.getInRange(now - 86_400_000, now)

                // Cleanup old snapshots (older than 7 days)
                batterySnapshotDao.deleteOlderThan(now - 7 * 86_400_000)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectTab(tab: BatteryTab) {
        _selectedTab.value = tab
    }

    fun saveSnapshot() {
        viewModelScope.launch(Dispatchers.IO) {
            val health = batteryHealthAnalyzer.analyze()
            batterySnapshotDao.insert(
                BatterySnapshot(
                    timestamp = System.currentTimeMillis(),
                    levelPercent = health.levelPercent,
                    temperature = health.temperature,
                    voltage = health.voltage,
                    isCharging = health.isCharging,
                    healthScore = health.healthScore
                )
            )
        }
    }
}
