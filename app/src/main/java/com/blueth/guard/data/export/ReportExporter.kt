package com.blueth.guard.data.export

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.blueth.guard.BuildConfig
import com.blueth.guard.scanner.SecurityScanner
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ScanReport(
    val timestamp: Long,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
    val totalAppsScanned: Int,
    val threatsFound: Int,
    val trackersFound: Int,
    val flaggedApps: List<FlaggedApp>
)

@Serializable
data class FlaggedApp(
    val name: String,
    val packageName: String,
    val riskLevel: String,
    val score: Int,
    val reasons: List<String>,
    val trackerCount: Int
)

@Singleton
class ReportExporter @Inject constructor(
    private val app: Application,
    private val securityScanner: SecurityScanner
) {
    private val json = Json { prettyPrint = true }

    fun generateReport(): ScanReport {
        val results = securityScanner.getLastScanResults()

        val flaggedApps = results
            .filter { it.threatAssessment.riskLevel.name in listOf("MEDIUM", "HIGH", "CRITICAL") }
            .map { result ->
                FlaggedApp(
                    name = result.appInfo.name,
                    packageName = result.appInfo.packageName,
                    riskLevel = result.threatAssessment.riskLevel.name,
                    score = result.threatAssessment.overallScore,
                    reasons = result.threatAssessment.reasons.map { it.description },
                    trackerCount = result.detectedTrackers.size
                )
            }

        return ScanReport(
            timestamp = System.currentTimeMillis(),
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = Build.VERSION.RELEASE,
            appVersion = BuildConfig.VERSION_NAME,
            totalAppsScanned = results.size,
            threatsFound = flaggedApps.size,
            trackersFound = results.sumOf { it.detectedTrackers.size },
            flaggedApps = flaggedApps
        )
    }

    fun exportToJson(report: ScanReport): String {
        return json.encodeToString(report)
    }

    fun shareReport(context: Context, report: ScanReport) {
        val jsonString = exportToJson(report)
        val reportsDir = File(app.cacheDir, "reports").apply { mkdirs() }
        val reportFile = File(reportsDir, "blueth-guard-report-${report.timestamp}.json")
        reportFile.writeText(jsonString)

        val uri = FileProvider.getUriForFile(
            app,
            "${app.packageName}.fileprovider",
            reportFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Blueth Guard Scan Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Scan Report"))
    }
}
