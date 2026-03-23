package com.blueth.guard.privacy

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.blueth.guard.data.local.PermissionEvent
import com.blueth.guard.data.local.PermissionEventDao
import com.blueth.guard.data.local.PermissionSnapshot
import com.blueth.guard.data.local.PermissionSnapshotDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppPrivacyProfile(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val grantedDangerous: Int,
    val totalDangerous: Int,
    val privacyScore: Int
)

data class DangerousPermissionGroup(
    val group: String,
    val displayName: String,
    val description: String,
    val permissions: List<String>
)

@Singleton
class PermissionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionEventDao: PermissionEventDao,
    private val permissionSnapshotDao: PermissionSnapshotDao
) {

    companion object {
        private val DANGEROUS_PERMISSIONS = setOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        private fun getPermissionGroup(permission: String): String = when {
            permission.contains("CAMERA") -> "CAMERA"
            permission.contains("RECORD_AUDIO") || permission.contains("MICROPHONE") -> "MICROPHONE"
            permission.contains("LOCATION") -> "LOCATION"
            permission.contains("CONTACTS") -> "CONTACTS"
            permission.contains("SMS") || permission.contains("MMS") -> "SMS"
            permission.contains("PHONE") || permission.contains("CALL") -> "PHONE"
            permission.contains("CALENDAR") -> "CALENDAR"
            permission.contains("SENSOR") -> "SENSORS"
            permission.contains("STORAGE") || permission.contains("MEDIA") -> "STORAGE"
            permission.contains("BLUETOOTH") || permission.contains("NEARBY") -> "NEARBY_DEVICES"
            else -> "OTHER"
        }
    }

    suspend fun snapshotCurrentPermissions() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val now = System.currentTimeMillis()

        for (pkgInfo in packages) {
            val packageName = pkgInfo.packageName
            val requestedPermissions = pkgInfo.requestedPermissions ?: continue
            val requestedPermissionsFlags = pkgInfo.requestedPermissionsFlags ?: continue

            val grantedPermissions = mutableListOf<String>()
            for (i in requestedPermissions.indices) {
                if (requestedPermissionsFlags[i] and PackageManager.GET_PERMISSIONS != 0) {
                    val perm = requestedPermissions[i]
                    if (perm in DANGEROUS_PERMISSIONS) {
                        grantedPermissions.add(perm)
                    }
                }
            }

            val appName = try {
                pkgInfo.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: packageName
            } catch (_: Exception) {
                packageName
            }

            val previousSnapshot = permissionSnapshotDao.getSnapshot(packageName)
            val previouslyGranted = previousSnapshot?.grantedPermissions
                ?.split("|||")
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: emptySet()
            val currentGranted = grantedPermissions.toSet()

            // Newly granted permissions
            for (perm in currentGranted - previouslyGranted) {
                permissionEventDao.insert(
                    PermissionEvent(
                        packageName = packageName,
                        appName = appName,
                        permission = perm,
                        permissionGroup = getPermissionGroup(perm),
                        timestamp = now,
                        isGranted = true
                    )
                )
            }

            // Newly revoked permissions
            for (perm in previouslyGranted - currentGranted) {
                permissionEventDao.insert(
                    PermissionEvent(
                        packageName = packageName,
                        appName = appName,
                        permission = perm,
                        permissionGroup = getPermissionGroup(perm),
                        timestamp = now,
                        isGranted = false
                    )
                )
            }

            // Update snapshot
            permissionSnapshotDao.upsert(
                PermissionSnapshot(
                    packageName = packageName,
                    grantedPermissions = currentGranted.joinToString("|||"),
                    snapshotTimestamp = now
                )
            )
        }
    }

    fun getPermissionTimeline(packageName: String): Flow<List<PermissionEvent>> =
        permissionEventDao.getEventsForApp(packageName)

    fun getRecentActivity(hours: Int = 24): Flow<List<PermissionEvent>> {
        val since = System.currentTimeMillis() - hours * 3600_000L
        return permissionEventDao.getRecentEvents(since)
    }

    suspend fun getMostDangerousApps(): List<AppPrivacyProfile> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val profiles = mutableListOf<AppPrivacyProfile>()

        for (pkgInfo in packages) {
            val appInfo = pkgInfo.applicationInfo ?: continue
            if (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0) continue

            val requestedPermissions = pkgInfo.requestedPermissions ?: continue
            val requestedPermissionsFlags = pkgInfo.requestedPermissionsFlags ?: continue

            val dangerousRequested = requestedPermissions.filter { it in DANGEROUS_PERMISSIONS }
            if (dangerousRequested.isEmpty()) continue

            var grantedCount = 0
            for (i in requestedPermissions.indices) {
                if (requestedPermissions[i] in DANGEROUS_PERMISSIONS &&
                    requestedPermissionsFlags[i] and PackageManager.GET_PERMISSIONS != 0
                ) {
                    grantedCount++
                }
            }

            val score = if (dangerousRequested.isNotEmpty()) {
                100 - (grantedCount * 100 / dangerousRequested.size)
            } else 100

            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkgInfo.packageName
            }

            val icon = try {
                pm.getApplicationIcon(appInfo)
            } catch (_: Exception) {
                null
            }

            profiles.add(
                AppPrivacyProfile(
                    packageName = pkgInfo.packageName,
                    appName = appName,
                    icon = icon,
                    grantedDangerous = grantedCount,
                    totalDangerous = dangerousRequested.size,
                    privacyScore = score
                )
            )
        }

        profiles.sortedByDescending { it.grantedDangerous }
    }

    fun getDangerousPermissionGroups(): List<DangerousPermissionGroup> = listOf(
        DangerousPermissionGroup(
            "CAMERA", "Camera", "Take photos and record video",
            listOf(Manifest.permission.CAMERA)
        ),
        DangerousPermissionGroup(
            "MICROPHONE", "Microphone", "Record audio",
            listOf(Manifest.permission.RECORD_AUDIO)
        ),
        DangerousPermissionGroup(
            "LOCATION", "Location", "Access your precise or approximate location",
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        ),
        DangerousPermissionGroup(
            "CONTACTS", "Contacts", "Read or modify your contacts",
            listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
        ),
        DangerousPermissionGroup(
            "SMS", "SMS", "Read, send, and receive text messages",
            listOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS)
        ),
        DangerousPermissionGroup(
            "PHONE", "Phone", "Make calls and read call log",
            listOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG)
        ),
        DangerousPermissionGroup(
            "CALENDAR", "Calendar", "Read or modify calendar events",
            listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        ),
        DangerousPermissionGroup(
            "SENSORS", "Body Sensors", "Access body sensors like heart rate",
            listOf(Manifest.permission.BODY_SENSORS)
        ),
        DangerousPermissionGroup(
            "STORAGE", "Storage", "Access files on your device",
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ),
        DangerousPermissionGroup(
            "NEARBY_DEVICES", "Nearby Devices", "Find and connect to nearby devices",
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        )
    )
}
