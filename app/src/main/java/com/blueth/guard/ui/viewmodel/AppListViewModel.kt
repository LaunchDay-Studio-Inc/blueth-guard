package com.blueth.guard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueth.guard.data.model.AppInfo
import com.blueth.guard.data.repository.AppRepository
import com.blueth.guard.scanner.PermissionRiskScorer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val riskScorer: PermissionRiskScorer
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val appList = appRepository.getInstalledApps().map { app ->
                app.copy(riskScore = riskScorer.scoreApp(app.permissions))
            }
            _apps.value = appList
            _isLoading.value = false
        }
    }

    fun getUserApps(): List<AppInfo> = _apps.value.filter { !it.isSystem }

    fun getSystemApps(): List<AppInfo> = _apps.value.filter { it.isSystem }

    fun getHighRiskApps(): List<AppInfo> = _apps.value.filter { it.riskScore > 60 }

    fun getTotalCacheSize(): Long = _apps.value.sumOf { it.cacheSize }
}
