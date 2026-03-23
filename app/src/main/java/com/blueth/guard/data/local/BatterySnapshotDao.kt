package com.blueth.guard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BatterySnapshotDao {

    @Insert
    suspend fun insert(snapshot: BatterySnapshot)

    @Query("SELECT * FROM battery_snapshots ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<BatterySnapshot>

    @Query("SELECT * FROM battery_snapshots WHERE timestamp BETWEEN :startMs AND :endMs ORDER BY timestamp DESC")
    suspend fun getInRange(startMs: Long, endMs: Long): List<BatterySnapshot>

    @Query("DELETE FROM battery_snapshots WHERE timestamp < :timestampMs")
    suspend fun deleteOlderThan(timestampMs: Long)
}
