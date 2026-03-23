package com.blueth.guard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.blueth.guard.privacy.InstallAction

@Entity(tableName = "install_events")
data class InstallEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val action: InstallAction,
    val timestamp: Long,
    val riskScore: Int?,
    val installSource: String?,
    val scanSummary: String?
)
