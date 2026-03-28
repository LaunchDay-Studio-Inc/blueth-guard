package com.blueth.guard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallEventDao {

    @Insert
    suspend fun insert(event: InstallEvent)

    @Insert
    suspend fun insertAll(events: List<InstallEvent>)

    @Query("SELECT * FROM install_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<InstallEvent>>

    @Query("SELECT * FROM install_events WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getEventsForApp(packageName: String): Flow<List<InstallEvent>>

    @Query("SELECT COUNT(*) FROM install_events WHERE timestamp >= :since AND (action = 'INSTALLED' OR action = 'UPDATED')")
    fun getInstallCount(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM install_events WHERE timestamp >= :since AND installSource NOT IN ('com.android.vending', 'com.google.android.packageinstaller')")
    fun getSideloadCount(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM install_events")
    suspend fun getCount(): Int
}
