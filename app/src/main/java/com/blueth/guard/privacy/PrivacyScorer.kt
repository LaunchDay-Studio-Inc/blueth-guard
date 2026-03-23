package com.blueth.guard.privacy

import android.Manifest
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import com.blueth.guard.data.repository.AppRepository
import com.blueth.guard.scanner.TrackerDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppPrivacyScore(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val overallScore: Int,
    val permissionScore: Int,
    val trackerScore: Int,
    val networkScore: Int,
    val installSourceScore: Int,
    val recommendations: List<PrivacyRecommendation>
)

data class PrivacyRecommendation(
    val title: String,
    val description: String,
    val actionType: RecommendationAction,
    val targetPackage: String?,
    val severity: RecommendationSeverity
)

enum class RecommendationAction {
    REVOKE_PERMISSION, UNINSTALL_APP, RESTRICT_BACKGROUND_DATA,
    OPEN_APP_SETTINGS, REVIEW_PERMISSIONS, ENABLE_SETTING, INFORMATIONAL
}

enum class RecommendationSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class DevicePrivacyScore(
    val overallScore: Int,
    val appScores: List<AppPrivacyScore>,
    val topRecommendations: List<PrivacyRecommendation>,
    val stats: PrivacyStats
)

data class PrivacyStats(
    val totalApps: Int,
    val appsWithCamera: Int,
    val appsWithMicrophone: Int,
    val appsWithLocation: Int,
    val appsWithContacts: Int,
    val sideloadedApps: Int,
    val highRiskApps: Int
)

@Singleton
class PrivacyScorer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val trackerDetector: TrackerDetector
) {
    companion object {
        private val PERMISSION_DEDUCTIONS = mapOf(
            Manifest.permission.CAMERA to 8,
            Manifest.permission.RECORD_AUDIO to 8,
            Manifest.permission.ACCESS_FINE_LOCATION to 10,
            Manifest.permission.ACCESS_COARSE_LOCATION to 5,
            Manifest.permission.READ_CONTACTS to 6,
            Manifest.permission.WRITE_CONTACTS to 4,
            Manifest.permission.READ_SMS to 8,
            Manifest.permission.SEND_SMS to 6,
            Manifest.permission.READ_PHONE_STATE to 5,
            Manifest.permission.CALL_PHONE to 5,
            Manifest.permission.READ_CALENDAR to 4,
            Manifest.permission.WRITE_CALENDAR to 3,
            Manifest.permission.BODY_SENSORS to 3,
            Manifest.permission.READ_EXTERNAL_STORAGE to 3,
            Manifest.permission.WRITE_EXTERNAL_STORAGE to 3
        )

        private val PLAY_STORE_PACKAGES = setOf(
            "com.android.vending",
            "com.google.android.packageinstaller"
        )

        private val FDROID_PACKAGES = setOf("org.fdroid.fdroid", "org.fdroid.fdroid.privileged")
        private val AMAZON_PACKAGES = setOf("com.amazon.venezia")
        private val SAMSUNG_PACKAGES = setOf("com.sec.android.app.samsungapps")
    }

    suspend fun calculateDeviceScore(): DevicePrivacyScore = withContext(Dispatchers.IO) {
        val apps = appRepository.getInstalledApps()
        val userApps = apps.filter { !it.isSystem }

        val appScores = userApps.map { app ->
            scoreApp(app.packageName, app.name, app.icon, app.permissions, app.installSource)
        }

        val allRecommendations = mutableListOf<PrivacyRecommendation>()
        appScores.forEach { allRecommendations.addAll(it.recommendations) }

        // System-level checks
        allRecommendations.addAll(checkSystemSettings())

        // App-level aggregate checks
        val appsWithLocation = userApps.count { app ->
            app.permissions.any { it.contains("LOCATION") }
        }
        if (appsWithLocation > 20) {
            allRecommendations.add(
                PrivacyRecommendation(
                    title = "Too many apps have location access",
                    description = "$appsWithLocation apps can access your location. Review and revoke unnecessary access.",
                    actionType = RecommendationAction.INFORMATIONAL,
                    targetPackage = null,
                    severity = RecommendationSeverity.MEDIUM
                )
            )
        }

        val averageScore = if (appScores.isNotEmpty()) {
            appScores.map { it.overallScore }.average().toInt()
        } else 100

        val stats = PrivacyStats(
            totalApps = userApps.size,
            appsWithCamera = userApps.count { Manifest.permission.CAMERA in it.permissions },
            appsWithMicrophone = userApps.count { Manifest.permission.RECORD_AUDIO in it.permissions },
            appsWithLocation = appsWithLocation,
            appsWithContacts = userApps.count { Manifest.permission.READ_CONTACTS in it.permissions },
            sideloadedApps = userApps.count { it.installSource !in PLAY_STORE_PACKAGES },
            highRiskApps = appScores.count { it.overallScore < 40 }
        )

        val topRecommendations = allRecommendations
            .sortedByDescending { it.severity.ordinal }
            .take(10)

        DevicePrivacyScore(
            overallScore = averageScore,
            appScores = appScores.sortedBy { it.overallScore },
            topRecommendations = topRecommendations,
            stats = stats
        )
    }

    private fun scoreApp(
        packageName: String,
        appName: String,
        icon: Drawable?,
        permissions: List<String>,
        installSource: String
    ): AppPrivacyScore {
        val permissionScore = calculatePermissionScore(permissions)
        val trackerScore = calculateTrackerScore(packageName, permissions)
        val networkScore = calculateNetworkScore(permissions, installSource)
        val installSourceScore = calculateInstallSourceScore(installSource)

        val overall = (
            permissionScore * 0.40 +
            trackerScore * 0.20 +
            networkScore * 0.20 +
            installSourceScore * 0.20
        ).toInt().coerceIn(0, 100)

        val recommendations = generateAppRecommendations(packageName, appName, permissions, installSource)

        return AppPrivacyScore(
            packageName = packageName,
            appName = appName,
            icon = icon,
            overallScore = overall,
            permissionScore = permissionScore,
            trackerScore = trackerScore,
            networkScore = networkScore,
            installSourceScore = installSourceScore,
            recommendations = recommendations
        )
    }

    private fun calculatePermissionScore(permissions: List<String>): Int {
        var score = 100
        for (perm in permissions) {
            score -= PERMISSION_DEDUCTIONS[perm] ?: 0
        }
        return score.coerceIn(0, 100)
    }

    private fun calculateTrackerScore(packageName: String, permissions: List<String>): Int {
        val trackers = trackerDetector.detectTrackers(packageName, permissions)
        return when (trackers.size) {
            0 -> 100
            in 1..2 -> 80
            in 3..5 -> 60
            in 6..10 -> 40
            else -> 20
        }
    }

    private fun calculateNetworkScore(permissions: List<String>, installSource: String): Int {
        val hasInternet = Manifest.permission.INTERNET in permissions
        return when {
            !hasInternet -> 100
            installSource in PLAY_STORE_PACKAGES -> 80
            else -> 40
        }
    }

    private fun calculateInstallSourceScore(installSource: String): Int = when {
        installSource in PLAY_STORE_PACKAGES -> 100
        installSource in FDROID_PACKAGES -> 95
        installSource in AMAZON_PACKAGES || installSource in SAMSUNG_PACKAGES -> 80
        installSource == "Unknown" -> 20
        else -> 30
    }

    private fun generateAppRecommendations(
        packageName: String,
        appName: String,
        permissions: List<String>,
        installSource: String
    ): List<PrivacyRecommendation> {
        val recs = mutableListOf<PrivacyRecommendation>()

        val hasCamera = Manifest.permission.CAMERA in permissions
        val hasMic = Manifest.permission.RECORD_AUDIO in permissions
        val hasLocation = permissions.any { it.contains("LOCATION") }
        val hasSms = Manifest.permission.READ_SMS in permissions
        val hasInternet = Manifest.permission.INTERNET in permissions

        if (hasCamera && hasMic && hasLocation) {
            recs.add(
                PrivacyRecommendation(
                    title = "Review $appName permissions",
                    description = "$appName has access to camera, microphone, and location.",
                    actionType = RecommendationAction.REVIEW_PERMISSIONS,
                    targetPackage = packageName,
                    severity = RecommendationSeverity.HIGH
                )
            )
        }

        if (installSource !in PLAY_STORE_PACKAGES && permissions.size > 10) {
            recs.add(
                PrivacyRecommendation(
                    title = "Consider uninstalling $appName",
                    description = "$appName was sideloaded and has ${permissions.size} permissions.",
                    actionType = RecommendationAction.UNINSTALL_APP,
                    targetPackage = packageName,
                    severity = RecommendationSeverity.HIGH
                )
            )
        }

        if (hasSms && hasInternet) {
            recs.add(
                PrivacyRecommendation(
                    title = "Warning: $appName can read SMS",
                    description = "$appName can read SMS and access the internet — potential security risk.",
                    actionType = RecommendationAction.REVIEW_PERMISSIONS,
                    targetPackage = packageName,
                    severity = RecommendationSeverity.CRITICAL
                )
            )
        }

        return recs
    }

    private fun checkSystemSettings(): List<PrivacyRecommendation> {
        val recs = mutableListOf<PrivacyRecommendation>()

        try {
            val devOptions = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            )
            if (devOptions == 1) {
                recs.add(
                    PrivacyRecommendation(
                        title = "Developer options enabled",
                        description = "Developer options are enabled. Consider disabling them for better security.",
                        actionType = RecommendationAction.ENABLE_SETTING,
                        targetPackage = null,
                        severity = RecommendationSeverity.MEDIUM
                    )
                )
            }

            val adb = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            )
            if (adb == 1) {
                recs.add(
                    PrivacyRecommendation(
                        title = "USB debugging enabled",
                        description = "USB debugging allows access to your device over a computer connection.",
                        actionType = RecommendationAction.ENABLE_SETTING,
                        targetPackage = null,
                        severity = RecommendationSeverity.HIGH
                    )
                )
            }
        } catch (_: Exception) {
            // Settings may not be accessible
        }

        return recs
    }
}
