package com.blueth.guard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkEventDao {

    @Insert
    suspend fun insertAll(entries: List<NetworkUsageEntry>)

    @Query("SELECT * FROM network_usage WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getUsageForApp(packageName: String): Flow<List<NetworkUsageEntry>>

    @Query(
        "SELECT packageName, appName, SUM(txBytes) AS totalTxBytes, SUM(rxBytes) AS totalRxBytes " +
        "FROM network_usage WHERE timestamp >= :since " +
        "GROUP BY packageName ORDER BY (SUM(txBytes) + SUM(rxBytes)) DESC LIMIT :limit"
    )
    fun getTopDataUsers(since: Long, limit: Int): Flow<List<NetworkUsageSummary>>

    @Query(
        "SELECT COALESCE(SUM(txBytes), 0) AS totalTx, COALESCE(SUM(rxBytes), 0) AS totalRx, " +
        "COUNT(DISTINCT packageName) AS appCount FROM network_usage WHERE timestamp >= :since"
    )
    fun getTotalUsage(since: Long): Flow<NetworkUsageTotals>

    @Query("DELETE FROM network_usage WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
