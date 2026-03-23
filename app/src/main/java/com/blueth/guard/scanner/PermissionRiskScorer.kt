package com.blueth.guard.scanner

import android.Manifest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRiskScorer @Inject constructor() {

    companion object {
        // Permission constants not in Manifest.permission
        private const val BIND_ACCESSIBILITY_SERVICE = "android.permission.BIND_ACCESSIBILITY_SERVICE"
        private const val BIND_DEVICE_ADMIN = "android.permission.BIND_DEVICE_ADMIN"

        private val CRITICAL_COMBOS = listOf(
            setOf(BIND_ACCESSIBILITY_SERVICE, Manifest.permission.INTERNET),
            setOf(BIND_DEVICE_ADMIN, Manifest.permission.INTERNET),
            setOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.INTERNET),
        )

        private val HIGH_COMBOS = listOf(
            setOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET),
            setOf(Manifest.permission.READ_SMS, Manifest.permission.INTERNET),
            setOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.INTERNET),
            setOf(Manifest.permission.READ_CONTACTS, Manifest.permission.INTERNET, Manifest.permission.CAMERA),
        )

        private val MEDIUM_COMBOS = listOf(
            setOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET),
            setOf(Manifest.permission.READ_CONTACTS, Manifest.permission.INTERNET),
            setOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET),
            setOf(Manifest.permission.READ_CALENDAR, Manifest.permission.INTERNET),
        )

        private val DANGEROUS_PERMISSIONS = setOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BODY_SENSORS,
            BIND_ACCESSIBILITY_SERVICE,
            BIND_DEVICE_ADMIN,
        )
    }

    fun scoreApp(permissions: List<String>): Int {
        val permSet = permissions.toSet()
        var score = 0

        // Check critical combos
        for (combo in CRITICAL_COMBOS) {
            if (permSet.containsAll(combo)) {
                score = maxOf(score, 85)
            }
        }

        // Check high combos
        for (combo in HIGH_COMBOS) {
            if (permSet.containsAll(combo)) {
                score = maxOf(score, 65)
            }
        }

        // Check medium combos
        for (combo in MEDIUM_COMBOS) {
            if (permSet.containsAll(combo)) {
                score = maxOf(score, 45)
            }
        }

        // Count individual dangerous permissions
        val dangerousCount = permSet.count { it in DANGEROUS_PERMISSIONS }
        val dangerousScore = when {
            dangerousCount >= 8 -> 50
            dangerousCount >= 5 -> 35
            dangerousCount >= 3 -> 25
            dangerousCount >= 1 -> 15
            else -> 0
        }
        score = maxOf(score, dangerousScore)

        return score.coerceIn(0, 100)
    }

    fun getRiskLevel(score: Int): RiskLevel = when {
        score <= 20 -> RiskLevel.SAFE
        score <= 40 -> RiskLevel.LOW
        score <= 60 -> RiskLevel.MEDIUM
        score <= 80 -> RiskLevel.HIGH
        else -> RiskLevel.CRITICAL
    }

    enum class RiskLevel(val label: String) {
        SAFE("Safe"),
        LOW("Low Risk"),
        MEDIUM("Medium Risk"),
        HIGH("High Risk"),
        CRITICAL("Critical")
    }
}
