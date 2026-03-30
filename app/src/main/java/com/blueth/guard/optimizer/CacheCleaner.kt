package com.blueth.guard.optimizer

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.core.net.toUri
import android.os.storage.StorageManager
import android.provider.Settings
import com.blueth.guard.accessibility.AutomationTask
import com.blueth.guard.accessibility.GuardAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppCacheInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val cacheSize: Long,
    val dataSize: Long,
    val apkSize: Long,
    val isSystem: Boolean
)

sealed class ClearResult {
    data object Success : ClearResult()
    data object OpenedSettings : ClearResult()
    data object RequiresPermission : ClearResult()
    data class Failed(val reason: String) : ClearResult()
}

@Singleton
class CacheCleaner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getAppCaches(): List<AppCacheInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AppCacheInfo>()
        try {
            val storageStatsManager =
                context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)
            val userHandle = Process.myUserHandle()

            for (pkg in packages) {
                try {
                    val stats = storageStatsManager.queryStatsForPackage(
                        StorageManager.UUID_DEFAULT,
                        pkg.packageName,
                        userHandle
                    )
                    val appInfo = pm.getApplicationInfo(pkg.packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val icon = try {
                        pm.getApplicationIcon(appInfo)
                    } catch (_: Exception) {
                        null
                    }
                    val isSystem =
                        appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0

                    result.add(
                        AppCacheInfo(
                            packageName = pkg.packageName,
                            appName = appName,
                            icon = icon,
                            cacheSize = stats.cacheBytes,
                            dataSize = stats.dataBytes,
                            apkSize = stats.appBytes,
                            isSystem = isSystem
                        )
                    )
                } catch (_: PackageManager.NameNotFoundException) {
                    // Package was uninstalled between listing and querying
                } catch (_: SecurityException) {
                    // PACKAGE_USAGE_STATS not granted
                }
            }
        } catch (_: SecurityException) {
            // PACKAGE_USAGE_STATS permission not granted
        }
        result.sortedByDescending { it.cacheSize }
    }

    suspend fun getTotalCacheSize(): Long = withContext(Dispatchers.IO) {
        getAppCaches().sumOf { it.cacheSize }
    }

    fun clearAppCache(packageName: String): ClearResult {
        // Android does not allow clearing other apps' caches without
        // device owner or root privileges. Open the system app info page
        // so the user can manually clear the cache.
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ClearResult.OpenedSettings
        } catch (e: Exception) {
            ClearResult.Failed(e.message ?: "Failed to open app settings")
        }
    }

    fun clearAllCaches(): ClearResult {
        // Without root or device owner privileges, we cannot programmatically
        // clear other apps' caches. Open the storage settings page instead.
        return try {
            val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ClearResult.OpenedSettings
        } catch (e: Exception) {
            ClearResult.Failed(e.message ?: "Failed to open storage settings")
        }
    }

    fun clearOwnCache(): ClearResult {
        return try {
            val cacheDir = context.cacheDir
            val externalCacheDir = context.externalCacheDir
            var cleared = 0L
            cacheDir?.let { cleared += deleteDir(it) }
            externalCacheDir?.let { cleared += deleteDir(it) }
            ClearResult.Success
        } catch (e: Exception) {
            ClearResult.Failed(e.message ?: "Failed to clear own cache")
        }
    }

    private fun deleteDir(dir: java.io.File): Long {
        var freed = 0L
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                freed += if (file.isDirectory) deleteDir(file) else {
                    val size = file.length()
                    if (file.delete()) size else 0L
                }
            }
        }
        return freed
    }

    fun autoClearCache(packageName: String): ClearResult {
        val service = GuardAccessibilityService.instance
        if (service != null) {
            service.queueTask(AutomationTask.ClearCacheSingle(packageName))
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ClearResult.Success
        }
        return clearAppCache(packageName)
    }

    fun autoClearAllCaches(packages: List<String>, onProgress: (Int, Int) -> Unit): ClearResult {
        val service = GuardAccessibilityService.instance
        if (service != null && packages.isNotEmpty()) {
            service.queueTask(AutomationTask.ClearCache(
                packages = packages,
                onProgress = onProgress
            ))
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${packages.first()}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ClearResult.Success
        }
        return clearAllCaches()
    }
}
