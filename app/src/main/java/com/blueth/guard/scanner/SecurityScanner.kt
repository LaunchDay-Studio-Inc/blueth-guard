package com.blueth.guard.scanner

import android.content.Context
import com.blueth.guard.data.model.AppInfo
import com.blueth.guard.data.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

data class ScanProgress(
    val current: Int,
    val total: Int,
    val currentApp: String,
    val results: List<AppScanResult>
)

data class AppScanResult(
    val appInfo: AppInfo,
    val threatAssessment: ThreatAssessment,
    val permissionAudit: PermissionAudit,
    val detectedTrackers: List<DetectedTracker>,
    val signatureMatch: SignatureMatch?,
    val installSource: InstallSource,
    val scanTimestamp: Long
)

@Singleton
class SecurityScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val trackerDetector: TrackerDetector,
    private val signatureDB: SignatureDB,
    private val mlEngine: MLEngine,
    private val permissionAuditor: PermissionAuditor,
    private val permissionRiskScorer: PermissionRiskScorer,
    private val sideloadDetector: SideloadDetector
) {
    private var cachedResults: List<AppScanResult> = emptyList()

    fun scanAll(): Flow<ScanProgress> = flow {
        val apps = appRepository.getInstalledApps()
        val results = mutableListOf<AppScanResult>()

        apps.forEachIndexed { index, app ->
            emit(ScanProgress(index + 1, apps.size, app.name, results.toList()))
            val result = scanAppInternal(app)
            results.add(result)
        }

        cachedResults = results.toList()
        emit(ScanProgress(apps.size, apps.size, "", results.toList()))
    }.flowOn(Dispatchers.IO)

    fun scanApp(packageName: String): AppScanResult? {
        return cachedResults.find { it.appInfo.packageName == packageName }
    }

    fun getLastScanResults(): List<AppScanResult> = cachedResults

    private fun scanAppInternal(appInfo: AppInfo): AppScanResult {
        val pm = context.packageManager
        val trackers = trackerDetector.detectTrackers(appInfo.packageName, appInfo.permissions)
        val signatureMatch = signatureDB.checkPackageName(appInfo.packageName)
            ?: signatureDB.checkCertificate(appInfo.packageName, pm)
        val permissionScore = permissionRiskScorer.scoreApp(appInfo.permissions)
        val permissionAudit = permissionAuditor.auditApp(appInfo.packageName, appInfo.permissions)
        val installSource = sideloadDetector.getInstallSource(appInfo.packageName)
        val threatAssessment = mlEngine.analyzeApp(appInfo, trackers, signatureMatch, permissionScore)

        return AppScanResult(
            appInfo = appInfo,
            threatAssessment = threatAssessment,
            permissionAudit = permissionAudit,
            detectedTrackers = trackers,
            signatureMatch = signatureMatch,
            installSource = installSource,
            scanTimestamp = System.currentTimeMillis()
        )
    }
}
