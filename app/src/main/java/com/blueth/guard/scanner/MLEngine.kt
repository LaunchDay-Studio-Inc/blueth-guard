package com.blueth.guard.scanner

import com.blueth.guard.data.model.AppInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

enum class RiskLevel(val label: String) {
    SAFE("Safe"),
    LOW("Low Risk"),
    MEDIUM("Medium Risk"),
    HIGH("High Risk"),
    CRITICAL("Critical");

    companion object {
        fun fromScore(score: Int): RiskLevel = when {
            score <= 20 -> SAFE
            score <= 40 -> LOW
            score <= 60 -> MEDIUM
            score <= 80 -> HIGH
            else -> CRITICAL
        }
    }
}

enum class ScanMethod { HEURISTIC, ML_TFLITE }

data class ThreatReason(
    val category: String,
    val description: String,
    val score: Int
)

data class ThreatAssessment(
    val overallScore: Int,
    val riskLevel: RiskLevel,
    val reasons: List<ThreatReason>,
    val confidence: Float,
    val scanMethod: ScanMethod
)

@Singleton
class MLEngine @Inject constructor() {

    fun analyzeApp(
        appInfo: AppInfo,
        trackers: List<DetectedTracker>,
        signatureMatch: SignatureMatch?,
        permissionScore: Int
    ): ThreatAssessment {
        val reasons = mutableListOf<ThreatReason>()

        // 1. Permission risk score: 30% weight
        val permissionComponent = (permissionScore * 0.30).toInt()
        if (permissionScore > 0) {
            reasons.add(ThreatReason("Permissions", "Permission risk score: $permissionScore", permissionScore))
        }

        // 2. Tracker count and category: 20% weight
        val trackerRaw = calculateTrackerScore(trackers)
        val trackerComponent = (trackerRaw * 0.20).toInt()
        if (trackers.isNotEmpty()) {
            val adCount = trackers.count { it.signature.category == TrackerCategory.ADVERTISING }
            reasons.add(ThreatReason("Trackers", "${trackers.size} trackers detected ($adCount advertising)", trackerRaw))
        }

        // 3. Install source: 15% weight
        val installSourceRaw = calculateInstallSourceScore(appInfo.installSource)
        val installSourceComponent = (installSourceRaw * 0.15).toInt()
        if (installSourceRaw > 0) {
            reasons.add(ThreatReason("Install Source", "Installed from: ${appInfo.installSource}", installSourceRaw))
        }

        // 4. Signature check: 20% weight
        val signatureRaw = calculateSignatureScore(signatureMatch)
        val signatureComponent = (signatureRaw * 0.20).toInt()
        if (signatureMatch != null) {
            reasons.add(ThreatReason("Malware Signature", "Matches known malware: ${signatureMatch.signature.family}", signatureRaw))
        }

        // 5. App metadata heuristics: 15% weight
        val metadataRaw = calculateMetadataScore(appInfo, reasons)
        val metadataComponent = (metadataRaw * 0.15).toInt()

        val overallScore = (permissionComponent + trackerComponent + installSourceComponent +
                signatureComponent + metadataComponent).coerceIn(0, 100)

        val confidence = if (signatureMatch != null) 0.95f
        else if (trackers.isNotEmpty()) 0.7f
        else 0.5f

        return ThreatAssessment(
            overallScore = overallScore,
            riskLevel = RiskLevel.fromScore(overallScore),
            reasons = reasons,
            confidence = confidence,
            scanMethod = ScanMethod.HEURISTIC
        )
    }

    private fun calculateTrackerScore(trackers: List<DetectedTracker>): Int {
        val baseScore = when {
            trackers.size >= 8 -> 40
            trackers.size in 4..7 -> 25
            trackers.size in 1..3 -> 10
            else -> 0
        }
        val adBonus = trackers.count { it.signature.category == TrackerCategory.ADVERTISING } * 3
        return (baseScore + adBonus).coerceAtMost(60)
    }

    private fun calculateInstallSourceScore(installSource: String): Int = when {
        installSource == "com.android.vending" -> 0
        installSource == "org.fdroid.fdroid" -> 0
        installSource == "com.amazon.venezia" -> 5
        installSource == "com.sec.android.app.samsungapps" -> 3
        installSource == "com.huawei.appmarket" -> 5
        installSource == "com.android.shell" -> 20
        installSource == "Unknown" || installSource.isEmpty() -> 30
        else -> 15
    }

    private fun calculateSignatureScore(match: SignatureMatch?): Int = when (match?.matchType) {
        null -> 0
        MatchType.PATTERN_MATCH -> 60
        MatchType.EXACT_PACKAGE -> 100
        MatchType.CERTIFICATE_MATCH -> 100
    }

    private fun calculateMetadataScore(appInfo: AppInfo, reasons: MutableList<ThreatReason>): Int {
        var score = 0

        if (appInfo.targetSdkVersion < 28) {
            score += 15
            reasons.add(ThreatReason("Metadata", "Targets old SDK version ${appInfo.targetSdkVersion}", 15))
        }

        if (appInfo.icon == null) {
            score += 10
            reasons.add(ThreatReason("Metadata", "App has no icon", 10))
        }

        if (hasHighEntropy(appInfo.packageName)) {
            score += 20
            reasons.add(ThreatReason("Metadata", "Package name appears randomly generated", 20))
        }

        val hasDeviceAdmin = appInfo.permissions.any {
            it == "android.permission.BIND_DEVICE_ADMIN"
        }
        if (hasDeviceAdmin) {
            score += 25
            reasons.add(ThreatReason("Metadata", "Requests device admin privileges", 25))
        }

        val hasAccessibility = appInfo.permissions.any {
            it == "android.permission.BIND_ACCESSIBILITY_SERVICE"
        }
        if (hasAccessibility) {
            score += 25
            reasons.add(ThreatReason("Metadata", "Uses accessibility service", 25))
        }

        if (looksLikeSystemApp(appInfo) && !appInfo.isSystem) {
            score += 15
            reasons.add(ThreatReason("Metadata", "Name mimics system app but is not a system app", 15))
        }

        return score.coerceAtMost(100)
    }

    private fun hasHighEntropy(packageName: String): Boolean {
        val parts = packageName.split(".")
        val lastPart = parts.lastOrNull() ?: return false
        if (lastPart.length < 6) return false
        val charFreq = lastPart.lowercase().groupBy { it }.mapValues { it.value.size }
        val len = lastPart.length.toDouble()
        val entropy = -charFreq.values.sumOf { count ->
            val p = count / len
            p * ln(p) / ln(2.0)
        }
        return entropy > 3.5 && lastPart.any { it.isDigit() } && !lastPart.contains(Regex("[aeiou]{2,}"))
    }

    private fun looksLikeSystemApp(appInfo: AppInfo): Boolean {
        val nameLower = appInfo.name.lowercase()
        val suspiciousWords = listOf("system", "android", "google", "settings", "update", "framework")
        return suspiciousWords.any { it in nameLower }
    }
}
