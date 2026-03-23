package com.blueth.guard.scanner

import android.Manifest
import android.annotation.SuppressLint
import javax.inject.Inject
import javax.inject.Singleton

enum class PermissionCategory(val label: String) {
    LOCATION("Location"),
    CAMERA("Camera"),
    MICROPHONE("Microphone"),
    CONTACTS("Contacts"),
    SMS("SMS"),
    PHONE("Phone"),
    STORAGE("Storage"),
    SENSORS("Sensors"),
    CALENDAR("Calendar"),
    OTHER("Other")
}

data class PermissionDetail(
    val permission: String,
    val category: PermissionCategory,
    val riskLevel: RiskLevel,
    val plainDescription: String,
    val isGranted: Boolean
)

data class PermissionCombo(
    val permissions: Set<String>,
    val description: String,
    val riskLevel: RiskLevel
)

data class PermissionAudit(
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val dangerousPermissions: List<PermissionDetail>,
    val criticalCombos: List<PermissionCombo>,
    val plainEnglishSummary: String
)

@Singleton
class PermissionAuditor @Inject constructor(
    private val riskScorer: PermissionRiskScorer
) {
    fun auditApp(packageName: String, permissions: List<String>): PermissionAudit {
        val score = riskScorer.scoreApp(permissions)
        val riskLevel = RiskLevel.fromScore(score)
        val permSet = permissions.toSet()

        val dangerousPerms = permissions.mapNotNull { perm ->
            permissionDetailsMap[perm]?.copy(isGranted = true)
        }

        val combos = mutableListOf<PermissionCombo>()

        // Critical combos
        val criticalComboDefs = listOf(
            setOf("android.permission.BIND_ACCESSIBILITY_SERVICE", Manifest.permission.INTERNET)
                    to "Can monitor all screen content and send it over the internet",
            setOf("android.permission.BIND_DEVICE_ADMIN", Manifest.permission.INTERNET)
                    to "Can remotely control your device",
            setOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.INTERNET)
                    to "Can read your text messages, send new ones, and transmit them online"
        )
        for ((combo, desc) in criticalComboDefs) {
            if (permSet.containsAll(combo)) {
                combos.add(PermissionCombo(combo, desc, RiskLevel.CRITICAL))
            }
        }

        // High combos
        val highComboDefs = listOf(
            setOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET)
                    to "Can record video and audio and send it over the internet",
            setOf(Manifest.permission.READ_SMS, Manifest.permission.INTERNET)
                    to "Can read your text messages and send them online",
            setOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.INTERNET)
                    to "Can read your call history and send it online",
            setOf(Manifest.permission.READ_CONTACTS, Manifest.permission.INTERNET, Manifest.permission.CAMERA)
                    to "Can access your contacts and camera with internet access"
        )
        for ((combo, desc) in highComboDefs) {
            if (permSet.containsAll(combo)) {
                combos.add(PermissionCombo(combo, desc, RiskLevel.HIGH))
            }
        }

        // Medium combos
        val medComboDefs = listOf(
            setOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET)
                    to "Can track your precise location and send it online",
            setOf(Manifest.permission.READ_CONTACTS, Manifest.permission.INTERNET)
                    to "Can read your contacts and send them online",
            setOf(Manifest.permission.READ_CALENDAR, Manifest.permission.INTERNET)
                    to "Can read your calendar and send events online"
        )
        for ((combo, desc) in medComboDefs) {
            if (permSet.containsAll(combo)) {
                combos.add(PermissionCombo(combo, desc, RiskLevel.MEDIUM))
            }
        }

        val summary = buildSummary(dangerousPerms, combos)

        return PermissionAudit(
            riskScore = score,
            riskLevel = riskLevel,
            dangerousPermissions = dangerousPerms,
            criticalCombos = combos,
            plainEnglishSummary = summary
        )
    }

    private fun buildSummary(
        perms: List<PermissionDetail>,
        combos: List<PermissionCombo>
    ): String {
        if (perms.isEmpty()) return "This app requests no dangerous permissions."

        val capabilities = mutableListOf<String>()
        val categories = perms.map { it.category }.toSet()

        if (PermissionCategory.CAMERA in categories) capabilities.add("access your camera")
        if (PermissionCategory.MICROPHONE in categories) capabilities.add("record audio")
        if (PermissionCategory.LOCATION in categories) capabilities.add("track your location")
        if (PermissionCategory.CONTACTS in categories) capabilities.add("read your contacts")
        if (PermissionCategory.SMS in categories) capabilities.add("read your messages")
        if (PermissionCategory.PHONE in categories) capabilities.add("access phone features")
        if (PermissionCategory.STORAGE in categories) capabilities.add("access your files")
        if (PermissionCategory.CALENDAR in categories) capabilities.add("read your calendar")
        if (PermissionCategory.SENSORS in categories) capabilities.add("access body sensors")

        val base = "This app can ${capabilities.joinToString(", ")}"
        val comboWarning = if (combos.isNotEmpty()) {
            ". Warning: ${combos.first().description}"
        } else ""

        return "$base$comboWarning."
    }

    companion object {
        @SuppressLint("InlinedApi")
        private val permissionDetailsMap: Map<String, PermissionDetail> = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to PermissionDetail(
                Manifest.permission.ACCESS_FINE_LOCATION, PermissionCategory.LOCATION, RiskLevel.HIGH,
                "Can access your precise GPS location", false
            ),
            Manifest.permission.ACCESS_COARSE_LOCATION to PermissionDetail(
                Manifest.permission.ACCESS_COARSE_LOCATION, PermissionCategory.LOCATION, RiskLevel.MEDIUM,
                "Can access your approximate location", false
            ),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION to PermissionDetail(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION, PermissionCategory.LOCATION, RiskLevel.HIGH,
                "Can access your location in the background", false
            ),
            Manifest.permission.CAMERA to PermissionDetail(
                Manifest.permission.CAMERA, PermissionCategory.CAMERA, RiskLevel.HIGH,
                "Can take photos and record video", false
            ),
            Manifest.permission.RECORD_AUDIO to PermissionDetail(
                Manifest.permission.RECORD_AUDIO, PermissionCategory.MICROPHONE, RiskLevel.HIGH,
                "Can record audio using the microphone", false
            ),
            Manifest.permission.READ_CONTACTS to PermissionDetail(
                Manifest.permission.READ_CONTACTS, PermissionCategory.CONTACTS, RiskLevel.MEDIUM,
                "Can read your contact list", false
            ),
            Manifest.permission.WRITE_CONTACTS to PermissionDetail(
                Manifest.permission.WRITE_CONTACTS, PermissionCategory.CONTACTS, RiskLevel.MEDIUM,
                "Can modify your contacts", false
            ),
            Manifest.permission.GET_ACCOUNTS to PermissionDetail(
                Manifest.permission.GET_ACCOUNTS, PermissionCategory.CONTACTS, RiskLevel.LOW,
                "Can see accounts on the device", false
            ),
            Manifest.permission.READ_SMS to PermissionDetail(
                Manifest.permission.READ_SMS, PermissionCategory.SMS, RiskLevel.CRITICAL,
                "Can read your text messages including verification codes", false
            ),
            Manifest.permission.SEND_SMS to PermissionDetail(
                Manifest.permission.SEND_SMS, PermissionCategory.SMS, RiskLevel.CRITICAL,
                "Can send text messages on your behalf", false
            ),
            Manifest.permission.RECEIVE_SMS to PermissionDetail(
                Manifest.permission.RECEIVE_SMS, PermissionCategory.SMS, RiskLevel.HIGH,
                "Can intercept incoming text messages", false
            ),
            Manifest.permission.RECEIVE_MMS to PermissionDetail(
                Manifest.permission.RECEIVE_MMS, PermissionCategory.SMS, RiskLevel.MEDIUM,
                "Can receive MMS messages", false
            ),
            Manifest.permission.READ_PHONE_STATE to PermissionDetail(
                Manifest.permission.READ_PHONE_STATE, PermissionCategory.PHONE, RiskLevel.MEDIUM,
                "Can read phone status and identity", false
            ),
            Manifest.permission.READ_PHONE_NUMBERS to PermissionDetail(
                Manifest.permission.READ_PHONE_NUMBERS, PermissionCategory.PHONE, RiskLevel.MEDIUM,
                "Can read your phone number", false
            ),
            Manifest.permission.CALL_PHONE to PermissionDetail(
                Manifest.permission.CALL_PHONE, PermissionCategory.PHONE, RiskLevel.HIGH,
                "Can make phone calls without your interaction", false
            ),
            Manifest.permission.READ_CALL_LOG to PermissionDetail(
                Manifest.permission.READ_CALL_LOG, PermissionCategory.PHONE, RiskLevel.HIGH,
                "Can read your call history", false
            ),
            Manifest.permission.WRITE_CALL_LOG to PermissionDetail(
                Manifest.permission.WRITE_CALL_LOG, PermissionCategory.PHONE, RiskLevel.MEDIUM,
                "Can modify your call log", false
            ),
            Manifest.permission.ANSWER_PHONE_CALLS to PermissionDetail(
                Manifest.permission.ANSWER_PHONE_CALLS, PermissionCategory.PHONE, RiskLevel.MEDIUM,
                "Can answer incoming phone calls", false
            ),
            Manifest.permission.ADD_VOICEMAIL to PermissionDetail(
                Manifest.permission.ADD_VOICEMAIL, PermissionCategory.PHONE, RiskLevel.LOW,
                "Can add voicemail messages", false
            ),
            Manifest.permission.USE_SIP to PermissionDetail(
                Manifest.permission.USE_SIP, PermissionCategory.PHONE, RiskLevel.LOW,
                "Can use SIP for internet calls", false
            ),
            Manifest.permission.READ_EXTERNAL_STORAGE to PermissionDetail(
                Manifest.permission.READ_EXTERNAL_STORAGE, PermissionCategory.STORAGE, RiskLevel.MEDIUM,
                "Can read files on your device storage", false
            ),
            Manifest.permission.WRITE_EXTERNAL_STORAGE to PermissionDetail(
                Manifest.permission.WRITE_EXTERNAL_STORAGE, PermissionCategory.STORAGE, RiskLevel.MEDIUM,
                "Can write files to your device storage", false
            ),
            Manifest.permission.READ_MEDIA_IMAGES to PermissionDetail(
                Manifest.permission.READ_MEDIA_IMAGES, PermissionCategory.STORAGE, RiskLevel.MEDIUM,
                "Can read your photos", false
            ),
            Manifest.permission.READ_MEDIA_VIDEO to PermissionDetail(
                Manifest.permission.READ_MEDIA_VIDEO, PermissionCategory.STORAGE, RiskLevel.MEDIUM,
                "Can read your videos", false
            ),
            Manifest.permission.READ_MEDIA_AUDIO to PermissionDetail(
                Manifest.permission.READ_MEDIA_AUDIO, PermissionCategory.STORAGE, RiskLevel.MEDIUM,
                "Can read your audio files", false
            ),
            Manifest.permission.BODY_SENSORS to PermissionDetail(
                Manifest.permission.BODY_SENSORS, PermissionCategory.SENSORS, RiskLevel.MEDIUM,
                "Can access body sensor data like heart rate", false
            ),
            Manifest.permission.ACTIVITY_RECOGNITION to PermissionDetail(
                Manifest.permission.ACTIVITY_RECOGNITION, PermissionCategory.SENSORS, RiskLevel.LOW,
                "Can detect your physical activity", false
            ),
            Manifest.permission.READ_CALENDAR to PermissionDetail(
                Manifest.permission.READ_CALENDAR, PermissionCategory.CALENDAR, RiskLevel.MEDIUM,
                "Can read your calendar events", false
            ),
            Manifest.permission.WRITE_CALENDAR to PermissionDetail(
                Manifest.permission.WRITE_CALENDAR, PermissionCategory.CALENDAR, RiskLevel.MEDIUM,
                "Can add or modify calendar events", false
            ),
            Manifest.permission.POST_NOTIFICATIONS to PermissionDetail(
                Manifest.permission.POST_NOTIFICATIONS, PermissionCategory.OTHER, RiskLevel.LOW,
                "Can send you notifications", false
            ),
            Manifest.permission.NEARBY_WIFI_DEVICES to PermissionDetail(
                Manifest.permission.NEARBY_WIFI_DEVICES, PermissionCategory.OTHER, RiskLevel.MEDIUM,
                "Can discover nearby Wi-Fi devices", false
            ),
            Manifest.permission.BLUETOOTH_CONNECT to PermissionDetail(
                Manifest.permission.BLUETOOTH_CONNECT, PermissionCategory.OTHER, RiskLevel.LOW,
                "Can connect to paired Bluetooth devices", false
            ),
            Manifest.permission.BLUETOOTH_SCAN to PermissionDetail(
                Manifest.permission.BLUETOOTH_SCAN, PermissionCategory.OTHER, RiskLevel.MEDIUM,
                "Can discover nearby Bluetooth devices", false
            )
        )
    }
}
