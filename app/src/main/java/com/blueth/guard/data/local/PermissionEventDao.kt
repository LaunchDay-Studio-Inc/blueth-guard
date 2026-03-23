package com.blueth.guard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PermissionEventDao {

    @Insert
    suspend fun insert(event: PermissionEvent)

    @Query("SELECT * FROM permission_events WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getEventsForApp(packageName: String): Flow<List<PermissionEvent>>

    @Query("SELECT * FROM permission_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getRecentEvents(since: Long): Flow<List<PermissionEvent>>

    @Query("SELECT * FROM permission_events WHERE permission = :permission ORDER BY timestamp DESC")
    fun getEventsByPermission(permission: String): Flow<List<PermissionEvent>>

    @Query("SELECT COUNT(*) FROM permission_events")
    fun getEventCount(): Flow<Int>

    @Query("DELETE FROM permission_events WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
