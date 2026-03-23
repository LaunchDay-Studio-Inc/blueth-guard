package com.blueth.guard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScanHistoryDao {

    @Insert
    suspend fun insert(entry: ScanHistoryEntry)

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<ScanHistoryEntry>

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): ScanHistoryEntry?

    @Query("SELECT COUNT(*) FROM scan_history")
    suspend fun getCount(): Int

    @Query("DELETE FROM scan_history WHERE timestamp < :timestampMs")
    suspend fun deleteOlderThan(timestampMs: Long)
}
