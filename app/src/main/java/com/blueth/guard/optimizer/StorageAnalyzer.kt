package com.blueth.guard.optimizer

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class StorageBreakdown(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val appBytes: Long,
    val cacheBytes: Long,
    val mediaBytes: Long,
    val systemBytes: Long,
    val otherBytes: Long
) {
    val usedPercentage: Float get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f

    val categories: List<StorageCategory>
        get() = listOf(
            StorageCategory("Apps", appBytes, 0xFF2196F3),
            StorageCategory("Media", mediaBytes, 0xFF9C27B0),
            StorageCategory("Cache", cacheBytes, 0xFFFF9800),
            StorageCategory("System", systemBytes, 0xFF607D8B),
            StorageCategory("Other", otherBytes, 0xFF795548),
            StorageCategory("Free", freeBytes, 0xFF4CAF50)
        )
}

data class StorageCategory(
    val name: String,
    val bytes: Long,
    val color: Long
)

@Singleton
class StorageAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun analyze(): StorageBreakdown = withContext(Dispatchers.IO) {
        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalBytes = statFs.totalBytes
        val freeBytes = statFs.availableBytes
        val usedBytes = totalBytes - freeBytes

        var totalAppBytes = 0L
        var totalCacheBytes = 0L

        try {
            val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val storageUuid = StorageManager.UUID_DEFAULT
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)

            for (pkg in packages) {
                try {
                    val stats = storageStatsManager.queryStatsForPackage(
                        storageUuid,
                        pkg.packageName,
                        android.os.Process.myUserHandle()
                    )
                    totalAppBytes += stats.appBytes + stats.dataBytes
                    totalCacheBytes += stats.cacheBytes
                } catch (_: Exception) {
                    // Some packages may not be accessible
                }
            }
        } catch (_: Exception) {
            // StorageStats may require PACKAGE_USAGE_STATS permission
        }

        // Estimate system and media
        val externalStatFs = StatFs(Environment.getExternalStorageDirectory().path)
        val externalTotal = externalStatFs.totalBytes
        val externalFree = externalStatFs.availableBytes
        val externalUsed = externalTotal - externalFree

        val mediaBytes = maxOf(0L, externalUsed - totalAppBytes - totalCacheBytes)
        val systemBytes = maxOf(0L, usedBytes - totalAppBytes - totalCacheBytes - mediaBytes)
        val otherBytes = maxOf(0L, usedBytes - totalAppBytes - totalCacheBytes - mediaBytes - systemBytes)

        StorageBreakdown(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            freeBytes = freeBytes,
            appBytes = totalAppBytes,
            cacheBytes = totalCacheBytes,
            mediaBytes = mediaBytes,
            systemBytes = systemBytes,
            otherBytes = otherBytes
        )
    }
}
