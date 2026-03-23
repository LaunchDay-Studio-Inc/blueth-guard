package com.blueth.guard.privacy

import android.content.Context
import android.content.pm.PackageManager
import com.blueth.guard.data.local.PermissionSnapshot
import com.blueth.guard.data.local.PermissionSnapshotDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionDiffCalculator @Inject constructor(
    private val permissionSnapshotDao: PermissionSnapshotDao,
    @ApplicationContext private val context: Context
) {
    data class PermissionDiff(
        val packageName: String,
        val appName: String,
        val addedPermissions: List<String>,
        val removedPermissions: List<String>,
        val timestamp: Long
    )

    suspend fun calculateDiffs(): List<PermissionDiff> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val diffs = mutableListOf<PermissionDiff>()

        for (pkgInfo in packages) {
            val packageName = pkgInfo.packageName
            val requestedPermissions = pkgInfo.requestedPermissions ?: continue
            val requestedPermissionsFlags = pkgInfo.requestedPermissionsFlags ?: continue

            val currentGranted = mutableSetOf<String>()
            for (i in requestedPermissions.indices) {
                if (requestedPermissionsFlags[i] and PackageManager.GET_PERMISSIONS != 0) {
                    currentGranted.add(requestedPermissions[i])
                }
            }

            val snapshot = permissionSnapshotDao.getSnapshot(packageName) ?: continue
            val previousGranted = snapshot.grantedPermissions
                .split("|||")
                .filter { it.isNotBlank() }
                .toSet()

            val added = currentGranted - previousGranted
            val removed = previousGranted - currentGranted

            if (added.isNotEmpty() || removed.isNotEmpty()) {
                val appName = try {
                    pkgInfo.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: packageName
                } catch (_: Exception) {
                    packageName
                }
                diffs.add(
                    PermissionDiff(
                        packageName = packageName,
                        appName = appName,
                        addedPermissions = added.toList(),
                        removedPermissions = removed.toList(),
                        timestamp = snapshot.snapshotTimestamp
                    )
                )
            }
        }
        return diffs
    }
}
