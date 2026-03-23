package com.blueth.guard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueth.guard.data.local.ScanHistoryDao
import com.blueth.guard.data.local.ScanHistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanHistoryViewModel @Inject constructor(
    private val scanHistoryDao: ScanHistoryDao
) : ViewModel() {

    private val _scanHistory = MutableStateFlow<List<ScanHistoryEntry>>(emptyList())
    val scanHistory: StateFlow<List<ScanHistoryEntry>> = _scanHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _scanHistory.value = scanHistoryDao.getRecent(50)
            _isLoading.value = false
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            scanHistoryDao.deleteOlderThan(System.currentTimeMillis() + 1)
            _scanHistory.value = emptyList()
        }
    }
}
