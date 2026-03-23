package com.blueth.guard.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class SourceType {
    PLAY_STORE, FDROID, AMAZON, SAMSUNG, HUAWEI, XIAOMI, OPPO,
    APTOIDE, SIDELOAD, ADB, SYSTEM, UNKNOWN
}

data class InstallSource(
    val sourcePackage: String?,
    val sourceType: SourceType,
    val displayName: String,
    val riskLevel: RiskLevel
)

@Singleton
class SideloadDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getInstallSource(packageName: String): InstallSource {
        val pm = context.packageManager

        val installerPackage = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
        } catch (_: Exception) {
            null
        }

        val isSystem = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        return mapInstallerToSource(installerPackage, isSystem)
    }

    private fun mapInstallerToSource(installer: String?, isSystem: Boolean): InstallSource = when {
        installer == "com.android.vending" || installer == "com.google.android.packageinstaller" ->
            InstallSource(installer, SourceType.PLAY_STORE, "Google Play Store", RiskLevel.SAFE)
        installer == "org.fdroid.fdroid" || installer == "org.fdroid.basic" ->
            InstallSource(installer, SourceType.FDROID, "F-Droid", RiskLevel.SAFE)
        installer == "com.amazon.venezia" ->
            InstallSource(installer, SourceType.AMAZON, "Amazon Appstore", RiskLevel.LOW)
        installer == "com.sec.android.app.samsungapps" ->
            InstallSource(installer, SourceType.SAMSUNG, "Samsung Galaxy Store", RiskLevel.SAFE)
        installer == "com.huawei.appmarket" ->
            InstallSource(installer, SourceType.HUAWEI, "Huawei AppGallery", RiskLevel.LOW)
        installer == "com.xiaomi.market" || installer == "com.xiaomi.mipicks" ->
            InstallSource(installer, SourceType.XIAOMI, "Xiaomi GetApps", RiskLevel.LOW)
        installer == "com.oppo.market" || installer == "com.heytap.market" ->
            InstallSource(installer, SourceType.OPPO, "OPPO App Market", RiskLevel.LOW)
        installer == "com.aptoide.pt" ->
            InstallSource(installer, SourceType.APTOIDE, "Aptoide", RiskLevel.MEDIUM)
        installer == "com.android.shell" ->
            InstallSource(installer, SourceType.ADB, "ADB Install", RiskLevel.MEDIUM)
        installer == null && isSystem ->
            InstallSource(null, SourceType.SYSTEM, "System", RiskLevel.SAFE)
        installer == null ->
            InstallSource(null, SourceType.SIDELOAD, "APK Sideload", RiskLevel.HIGH)
        else ->
            InstallSource(installer, SourceType.UNKNOWN, "Unknown ($installer)", RiskLevel.MEDIUM)
    }
}
