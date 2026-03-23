package com.blueth.guard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val totalAppsScanned: Int,
    val threatsFound: Int,
    val trackersFound: Int,
    val overallScore: Int,
    val flaggedApps: String,
    val scanDurationMs: Long
)
