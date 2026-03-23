package com.blueth.guard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_usage")
data class NetworkUsageEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val txBytes: Long,
    val rxBytes: Long,
    val txPackets: Long,
    val rxPackets: Long,
    val networkType: String,
    val timestamp: Long,
    val periodStartMs: Long,
    val periodEndMs: Long
)

data class NetworkUsageSummary(
    val packageName: String,
    val appName: String,
    val totalTxBytes: Long,
    val totalRxBytes: Long
)

data class NetworkUsageTotals(
    val totalTx: Long,
    val totalRx: Long,
    val appCount: Int
)
