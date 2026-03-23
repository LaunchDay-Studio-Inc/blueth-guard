package com.blueth.guard.scanner

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceAdminAppInfo(
    val packageName: String,
    val appName: String,
    val description: String,
    val capabilities: List<String>,
    val isActive: Boolean
)

@Singleton
class DeviceAdminChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getDeviceAdmins(): List<DeviceAdminAppInfo> {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return emptyList()
        val pm = context.packageManager
        val activeAdmins = dpm.activeAdmins ?: return emptyList()

        return activeAdmins.mapNotNull { admin ->
            buildAdminInfo(admin, pm, dpm)
        }
    }

    private fun buildAdminInfo(
        admin: ComponentName,
        pm: PackageManager,
        dpm: DevicePolicyManager
    ): DeviceAdminAppInfo? {
        val packageName = admin.packageName
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }

        val isSystem = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        val capabilities = mutableListOf<String>()
        try {
            if (dpm.isAdminActive(admin)) {
                capabilities.add("Active device administrator")
            }
        } catch (_: SecurityException) {
            // Cannot query this admin
        }

        // Build capability list based on common device admin features
        capabilities.addAll(getCapabilities(packageName, isSystem))

        val description = if (isSystem) "System device administrator" else "Third-party device administrator"

        return DeviceAdminAppInfo(
            packageName = packageName,
            appName = appName,
            description = description,
            capabilities = capabilities,
            isActive = true
        )
    }

    private fun getCapabilities(packageName: String, isSystem: Boolean): List<String> {
        val caps = mutableListOf<String>()
        if (isSystem) {
            caps.add("Can manage device security policies")
        } else {
            caps.add("Can lock device")
            caps.add("Can wipe device data")
            caps.add("Can set password policies")
            caps.add("Can disable camera")
        }
        return caps
    }
}
