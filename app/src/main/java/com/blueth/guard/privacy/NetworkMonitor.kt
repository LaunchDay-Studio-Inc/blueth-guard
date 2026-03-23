package com.blueth.guard.privacy

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import com.blueth.guard.data.local.NetworkEventDao
import com.blueth.guard.data.local.NetworkUsageEntry
import com.blueth.guard.data.local.NetworkUsageSummary
import com.blueth.guard.data.local.NetworkUsageTotals
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class SuspiciousNetworkApp(
    val packageName: String,
    val appName: String,
    val reason: String,
    val dataUsed: Long
)

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: NetworkEventDao
) {

    suspend fun collectNetworkStats(intervalHours: Int = 24) = withContext(Dispatchers.IO) {
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return@withContext

        val pm = context.packageManager
        val now = System.currentTimeMillis()
        val startTime = now - intervalHours * 3600_000L
        val packages = pm.getInstalledPackages(0)
        val entries = mutableListOf<NetworkUsageEntry>()

        for (pkgInfo in packages) {
            val uid = try {
                pkgInfo.applicationInfo?.uid ?: continue
            } catch (_: Exception) {
                continue
            }

            val appName = try {
                pkgInfo.applicationInfo?.let { pm.getApplicationLabel(it).toString() }
                    ?: pkgInfo.packageName
            } catch (_: Exception) {
                pkgInfo.packageName
            }

            // Query WiFi usage
            val wifiEntry = queryUidUsage(nsm, uid, ConnectivityManager.TYPE_WIFI, startTime, now)
            if (wifiEntry != null && (wifiEntry.first > 0 || wifiEntry.second > 0)) {
                entries.add(
                    NetworkUsageEntry(
                        packageName = pkgInfo.packageName,
                        appName = appName,
                        txBytes = wifiEntry.first,
                        rxBytes = wifiEntry.second,
                        txPackets = wifiEntry.third,
                        rxPackets = wifiEntry.fourth,
                        networkType = "WIFI",
                        timestamp = now,
                        periodStartMs = startTime,
                        periodEndMs = now
                    )
                )
            }

            // Query Mobile usage
            val mobileEntry = queryUidUsage(nsm, uid, ConnectivityManager.TYPE_MOBILE, startTime, now)
            if (mobileEntry != null && (mobileEntry.first > 0 || mobileEntry.second > 0)) {
                entries.add(
                    NetworkUsageEntry(
                        packageName = pkgInfo.packageName,
                        appName = appName,
                        txBytes = mobileEntry.first,
                        rxBytes = mobileEntry.second,
                        txPackets = mobileEntry.third,
                        rxPackets = mobileEntry.fourth,
                        networkType = "MOBILE",
                        timestamp = now,
                        periodStartMs = startTime,
                        periodEndMs = now
                    )
                )
            }
        }

        if (entries.isNotEmpty()) {
            dao.insertAll(entries)
        }
    }

    @Suppress("DEPRECATION")
    private fun queryUidUsage(
        nsm: NetworkStatsManager,
        uid: Int,
        networkType: Int,
        start: Long,
        end: Long
    ): Quadruple? {
        return try {
            val stats = nsm.queryDetailsForUid(networkType, null, start, end, uid)
            var totalTx = 0L
            var totalRx = 0L
            var totalTxPackets = 0L
            var totalRxPackets = 0L
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                totalTx += bucket.txBytes
                totalRx += bucket.rxBytes
                totalTxPackets += bucket.txPackets
                totalRxPackets += bucket.rxPackets
            }
            stats.close()
            Quadruple(totalTx, totalRx, totalTxPackets, totalRxPackets)
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun getTopDataConsumers(hours: Int = 24): Flow<List<NetworkUsageSummary>> {
        val since = System.currentTimeMillis() - hours * 3600_000L
        return dao.getTopDataUsers(since, 15)
    }

    fun getAppNetworkHistory(packageName: String): Flow<List<NetworkUsageEntry>> =
        dao.getUsageForApp(packageName)

    fun getTotalUsage(hours: Int = 24): Flow<NetworkUsageTotals> {
        val since = System.currentTimeMillis() - hours * 3600_000L
        return dao.getTotalUsage(since)
    }

    fun flagSuspiciousUsage(entries: List<NetworkUsageSummary>): List<SuspiciousNetworkApp> {
        val suspicious = mutableListOf<SuspiciousNetworkApp>()
        for (entry in entries) {
            val totalData = entry.totalTxBytes + entry.totalRxBytes
            if (entry.totalTxBytes > entry.totalRxBytes * 3 && entry.totalTxBytes > 1_000_000) {
                suspicious.add(
                    SuspiciousNetworkApp(
                        packageName = entry.packageName,
                        appName = entry.appName,
                        reason = "Unusually high upload ratio (${formatBytes(entry.totalTxBytes)} sent vs ${formatBytes(entry.totalRxBytes)} received)",
                        dataUsed = totalData
                    )
                )
            }
            if (totalData > 500_000_000) {
                suspicious.add(
                    SuspiciousNetworkApp(
                        packageName = entry.packageName,
                        appName = entry.appName,
                        reason = "Very high data usage: ${formatBytes(totalData)}",
                        dataUsed = totalData
                    )
                )
            }
        }
        return suspicious
    }

    private data class Quadruple(
        val first: Long,
        val second: Long,
        val third: Long,
        val fourth: Long
    )

    companion object {
        fun formatBytes(bytes: Long): String = when {
            bytes >= 1_073_741_824 -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
            bytes >= 1_024 -> String.format(Locale.US, "%.1f KB", bytes / 1_024.0)
            else -> "$bytes B"
        }
    }
}
