package com.blueth.guard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_snapshots")
data class BatterySnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val levelPercent: Int,
    val temperature: Float,
    val voltage: Float,
    val isCharging: Boolean,
    val healthScore: Int
)
