package com.blueth.guard.data.repository

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Process
import android.os.storage.StorageManager
import com.blueth.guard.data.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager

        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        packages.mapNotNull { packageInfo ->
            try {
                mapToAppInfo(pm, storageStatsManager, packageInfo)
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.name.lowercase() }
    }

    private fun mapToAppInfo(
        pm: PackageManager,
        storageStatsManager: StorageStatsManager?,
        packageInfo: PackageInfo
    ): AppInfo {
        val applicationInfo = packageInfo.applicationInfo ?: return AppInfo(
            name = packageInfo.packageName,
            packageName = packageInfo.packageName,
            icon = null,
            versionName = packageInfo.versionName ?: "",
            installSource = "",
            permissions = emptyList(),
            cacheSize = 0L,
            dataSize = 0L,
            apkSize = 0L,
            isSystem = false,
            firstInstallTime = packageInfo.firstInstallTime,
            lastUpdateTime = packageInfo.lastUpdateTime,
            targetSdkVersion = 0
        )

        val appName = pm.getApplicationLabel(applicationInfo).toString()
        val icon = try { pm.getApplicationIcon(applicationInfo) } catch (_: Exception) { null }

        val installSource = try {
            pm.getInstallSourceInfo(packageInfo.packageName).installingPackageName ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }

        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

        val isSystem = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0

        var cacheSize = 0L
        var dataSize = 0L
        var apkSize = 0L
        try {
            storageStatsManager?.let { ssm ->
                val storageUuid = StorageManager.UUID_DEFAULT
                val stats = ssm.queryStatsForPackage(
                    storageUuid,
                    packageInfo.packageName,
                    Process.myUserHandle()
                )
                cacheSize = stats.cacheBytes
                dataSize = stats.dataBytes
                apkSize = stats.appBytes
            }
        } catch (_: Exception) {
            // Storage stats may require PACKAGE_USAGE_STATS permission
        }

        return AppInfo(
            name = appName,
            packageName = packageInfo.packageName,
            icon = icon,
            versionName = packageInfo.versionName ?: "",
            installSource = installSource,
            permissions = permissions,
            cacheSize = cacheSize,
            dataSize = dataSize,
            apkSize = apkSize,
            isSystem = isSystem,
            firstInstallTime = packageInfo.firstInstallTime,
            lastUpdateTime = packageInfo.lastUpdateTime,
            targetSdkVersion = applicationInfo.targetSdkVersion
        )
    }
}
