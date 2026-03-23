package com.blueth.guard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueth.guard.optimizer.AppCacheInfo
import com.blueth.guard.optimizer.BloatwareApp
import com.blueth.guard.optimizer.BloatwareIdentifier
import com.blueth.guard.optimizer.CacheCleaner
import com.blueth.guard.optimizer.ClearResult
import com.blueth.guard.optimizer.DeleteResult
import com.blueth.guard.optimizer.DuplicateFinder
import com.blueth.guard.optimizer.DuplicateGroup
import com.blueth.guard.optimizer.DuplicateScanProgress
import com.blueth.guard.optimizer.KillResult
import com.blueth.guard.optimizer.ProcessManager
import com.blueth.guard.optimizer.RunningProcess
import com.blueth.guard.optimizer.StorageAnalyzer
import com.blueth.guard.optimizer.StorageBreakdown
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OptimizerTab {
    OVERVIEW, CACHE, PROCESSES, DUPLICATES, BLOATWARE
}

@HiltViewModel
class OptimizerViewModel @Inject constructor(
    private val storageAnalyzer: StorageAnalyzer,
    private val cacheCleaner: CacheCleaner,
    private val processManager: ProcessManager,
    private val duplicateFinder: DuplicateFinder,
    private val bloatwareIdentifier: BloatwareIdentifier
) : ViewModel() {

    private val _storageBreakdown = MutableStateFlow<StorageBreakdown?>(null)
    val storageBreakdown: StateFlow<StorageBreakdown?> = _storageBreakdown.asStateFlow()

    private val _appCaches = MutableStateFlow<List<AppCacheInfo>>(emptyList())
    val appCaches: StateFlow<List<AppCacheInfo>> = _appCaches.asStateFlow()

    private val _totalCacheSize = MutableStateFlow(0L)
    val totalCacheSize: StateFlow<Long> = _totalCacheSize.asStateFlow()

    private val _runningProcesses = MutableStateFlow<List<RunningProcess>>(emptyList())
    val runningProcesses: StateFlow<List<RunningProcess>> = _runningProcesses.asStateFlow()

    private val _killableProcesses = MutableStateFlow<List<RunningProcess>>(emptyList())
    val killableProcesses: StateFlow<List<RunningProcess>> = _killableProcesses.asStateFlow()

    private val _killableMemoryKb = MutableStateFlow(0L)
    val killableMemoryKb: StateFlow<Long> = _killableMemoryKb.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    private val _duplicateScanProgress = MutableStateFlow<DuplicateScanProgress?>(null)
    val duplicateScanProgress: StateFlow<DuplicateScanProgress?> = _duplicateScanProgress.asStateFlow()

    private val _isDuplicateScanning = MutableStateFlow(false)
    val isDuplicateScanning: StateFlow<Boolean> = _isDuplicateScanning.asStateFlow()

    private val _totalWastedSpace = MutableStateFlow(0L)
    val totalWastedSpace: StateFlow<Long> = _totalWastedSpace.asStateFlow()

    private val _bloatwareApps = MutableStateFlow<List<BloatwareApp>>(emptyList())
    val bloatwareApps: StateFlow<List<BloatwareApp>> = _bloatwareApps.asStateFlow()

    private val _bloatwareCount = MutableStateFlow(0)
    val bloatwareCount: StateFlow<Int> = _bloatwareCount.asStateFlow()

    private val _optimizerTab = MutableStateFlow(OptimizerTab.OVERVIEW)
    val optimizerTab: StateFlow<OptimizerTab> = _optimizerTab.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _lastKillResult = MutableStateFlow<KillResult?>(null)
    val lastKillResult: StateFlow<KillResult?> = _lastKillResult.asStateFlow()

    private val _ramTotal = MutableStateFlow(0L)
    val ramTotal: StateFlow<Long> = _ramTotal.asStateFlow()

    private val _ramAvailable = MutableStateFlow(0L)
    val ramAvailable: StateFlow<Long> = _ramAvailable.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            // Load RAM info
            val (total, available) = processManager.getRamInfo()
            _ramTotal.value = total
            _ramAvailable.value = available

            // Load storage, caches, processes, bloatware in parallel
            launch { loadStorage() }
            launch { refreshCaches() }
            launch { refreshProcesses() }
            launch { loadBloatware() }
        }
    }

    private suspend fun loadStorage() {
        _storageBreakdown.value = storageAnalyzer.analyze()
    }

    fun refreshProcesses() {
        viewModelScope.launch {
            val (total, available) = processManager.getRamInfo()
            _ramTotal.value = total
            _ramAvailable.value = available

            val processes = processManager.getRunningProcesses()
            _runningProcesses.value = processes

            val killable = processManager.getSmartKillList()
            _killableProcesses.value = killable
            _killableMemoryKb.value = killable.sumOf { it.memoryUsageKb }
        }
    }

    fun refreshCaches() {
        viewModelScope.launch {
            val caches = cacheCleaner.getAppCaches()
            _appCaches.value = caches
            _totalCacheSize.value = caches.sumOf { it.cacheSize }
        }
    }

    fun clearAppCache(packageName: String) {
        val result = cacheCleaner.clearAppCache(packageName)
        val message = when (result) {
            is ClearResult.OpenedSettings -> "Opened settings — clear cache manually"
            is ClearResult.RequiresPermission -> "Permission required"
            is ClearResult.Success -> "Cache cleared"
            is ClearResult.Failed -> "Failed: ${result.reason}"
        }
        _snackbarMessage.value = message
    }

    fun clearAllCaches() {
        val result = cacheCleaner.clearAllCaches()
        val message = when (result) {
            is ClearResult.OpenedSettings -> "Opened storage settings — clear caches manually"
            is ClearResult.RequiresPermission -> "Permission required"
            is ClearResult.Success -> "All caches cleared"
            is ClearResult.Failed -> "Failed: ${result.reason}"
        }
        _snackbarMessage.value = message
    }

    fun killProcess(packageName: String) {
        val result = processManager.killProcess(packageName)
        _snackbarMessage.value = result.message
        if (result.success) {
            refreshProcesses()
        }
    }

    fun killAllKillable() {
        viewModelScope.launch {
            val result = processManager.killAllKillable()
            _lastKillResult.value = result
            _snackbarMessage.value = result.message
            refreshProcesses()
        }
    }

    fun startDuplicateScan() {
        if (_isDuplicateScanning.value) return
        viewModelScope.launch {
            _isDuplicateScanning.value = true
            _duplicateGroups.value = emptyList()
            _duplicateScanProgress.value = null

            duplicateFinder.scanForDuplicates().collect { progress ->
                _duplicateScanProgress.value = progress
            }

            val results = duplicateFinder.getScanResults()
            _duplicateGroups.value = results
            _totalWastedSpace.value = duplicateFinder.getTotalWastedSpace()
            _isDuplicateScanning.value = false
        }
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            val success = duplicateFinder.deleteFile(path)
            _snackbarMessage.value = if (success) "File deleted" else "Failed to delete file"
            // Refresh results
            _duplicateGroups.value = duplicateFinder.getScanResults()
            _totalWastedSpace.value = duplicateFinder.getTotalWastedSpace()
        }
    }

    fun deleteFiles(paths: List<String>) {
        viewModelScope.launch {
            val result: DeleteResult = duplicateFinder.deleteFiles(paths)
            _snackbarMessage.value = "Deleted ${result.deleted} files, freed ${formatSize(result.freedBytes)}"
            _duplicateGroups.value = duplicateFinder.getScanResults()
            _totalWastedSpace.value = duplicateFinder.getTotalWastedSpace()
        }
    }

    fun disableBloatware(packageName: String) {
        val result = bloatwareIdentifier.disableApp(packageName)
        _snackbarMessage.value = when (result.method) {
            com.blueth.guard.optimizer.DisableMethod.OPENED_SETTINGS -> "Opened app settings"
            com.blueth.guard.optimizer.DisableMethod.REQUIRES_ADB -> "Requires ADB: ${result.instruction}"
            com.blueth.guard.optimizer.DisableMethod.REQUIRES_ROOT -> "Requires root access"
            com.blueth.guard.optimizer.DisableMethod.ALREADY_DISABLED -> "Already disabled"
        }
        viewModelScope.launch { loadBloatware() }
    }

    fun setTab(tab: OptimizerTab) {
        _optimizerTab.value = tab
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun clearKillResult() {
        _lastKillResult.value = null
    }

    fun getDetectedManufacturer(): String = bloatwareIdentifier.getDetectedManufacturer()

    private suspend fun loadBloatware() {
        val apps = bloatwareIdentifier.identifyBloatware()
        _bloatwareApps.value = apps
        _bloatwareCount.value = apps.size
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
