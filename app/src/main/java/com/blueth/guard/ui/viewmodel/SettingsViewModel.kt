package com.blueth.guard.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueth.guard.data.export.ReportExporter
import com.blueth.guard.data.prefs.ScanInterval
import com.blueth.guard.data.prefs.ThemeMode
import com.blueth.guard.data.prefs.UserPreferences
import com.blueth.guard.protection.ProtectionService
import com.blueth.guard.update.SignatureUpdateManager
import com.blueth.guard.worker.ScanScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val scanScheduler: ScanScheduler,
    private val reportExporter: ReportExporter,
    private val signatureUpdateManager: SignatureUpdateManager
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)

    val scanScheduleEnabled: StateFlow<Boolean> = userPreferences.scanScheduleEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val scanInterval: StateFlow<ScanInterval> = userPreferences.scanInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanInterval.WEEKLY)

    val realTimeProtection: StateFlow<Boolean> = userPreferences.realTimeProtection
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val installScanEnabled: StateFlow<Boolean> = userPreferences.installScanEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationEnabled: StateFlow<Boolean> = userPreferences.notificationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _updateStatus = MutableStateFlow<String?>(null)
    val updateStatus: StateFlow<String?> = _updateStatus.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    fun checkForSignatureUpdates() {
        viewModelScope.launch {
            _isUpdating.value = true
            val result = signatureUpdateManager.checkAndUpdate()
            _updateStatus.value = result.message
            _isUpdating.value = false
        }
    }

    fun clearUpdateStatus() {
        _updateStatus.value = null
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }

    fun setScanScheduleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setScanScheduleEnabled(enabled)
            if (enabled) {
                scanScheduler.schedulePeriodicScan(scanInterval.value)
            } else {
                scanScheduler.cancelScheduledScan()
            }
        }
    }

    fun setScanInterval(interval: ScanInterval) {
        viewModelScope.launch {
            userPreferences.setScanInterval(interval)
            if (scanScheduleEnabled.value) {
                scanScheduler.schedulePeriodicScan(interval)
            }
        }
    }

    fun setRealTimeProtection(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            userPreferences.setRealTimeProtection(enabled)
            if (enabled) {
                ProtectionService.start(context)
            } else {
                ProtectionService.stop(context)
            }
        }
    }

    fun setInstallScanEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setInstallScanEnabled(enabled) }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setNotificationEnabled(enabled) }
    }

    fun shareReport(context: Context) {
        val report = reportExporter.generateReport()
        reportExporter.shareReport(context, report)
    }
}
