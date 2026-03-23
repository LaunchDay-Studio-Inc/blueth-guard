package com.blueth.guard.optimizer

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class BloatwareCategory {
    CARRIER,
    MANUFACTURER,
    SOCIAL_MEDIA_PREINSTALL,
    SHOPPING_PREINSTALL,
    GAME_PREINSTALL,
    UTILITY_DUPLICATE,
    OTHER
}

enum class BloatwareAction {
    SAFE_TO_DISABLE,
    SAFE_TO_UNINSTALL,
    CAUTION,
    DO_NOT_TOUCH
}

enum class DisableMethod {
    OPENED_SETTINGS,
    REQUIRES_ADB,
    REQUIRES_ROOT,
    ALREADY_DISABLED
}

data class BloatwareApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val manufacturer: String,
    val category: BloatwareCategory,
    val recommendation: BloatwareAction,
    val description: String,
    val isDisabled: Boolean,
    val isInstalled: Boolean
)

data class DisableResult(
    val success: Boolean,
    val method: DisableMethod,
    val instruction: String?
)

private data class BloatwareEntry(
    val packageName: String,
    val manufacturer: String,
    val category: BloatwareCategory,
    val recommendation: BloatwareAction,
    val description: String,
    val universal: Boolean = false
)

@Singleton
class BloatwareIdentifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val deviceManufacturer: String by lazy {
        Build.MANUFACTURER.lowercase()
    }

    private val bloatwareDatabase: List<BloatwareEntry> by lazy { buildBloatwareDatabase() }

    suspend fun identifyBloatware(): List<BloatwareApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedPackages = pm.getInstalledPackages(0).map { it.packageName }.toSet()
        val result = mutableListOf<BloatwareApp>()

        for (entry in bloatwareDatabase) {
            // Filter by manufacturer relevance or universal
            if (!entry.universal && !isRelevantManufacturer(entry.manufacturer)) continue

            // Check if matching packages are installed
            val matchingPackages = if (entry.packageName.endsWith("*")) {
                val prefix = entry.packageName.removeSuffix("*")
                installedPackages.filter { it.startsWith(prefix) }
            } else {
                if (entry.packageName in installedPackages) listOf(entry.packageName) else emptyList()
            }

            for (pkg in matchingPackages) {
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
                    val isDisabled = !appInfo.enabled

                    result.add(
                        BloatwareApp(
                            packageName = pkg,
                            appName = appName,
                            icon = icon,
                            manufacturer = entry.manufacturer,
                            category = entry.category,
                            recommendation = entry.recommendation,
                            description = entry.description,
                            isDisabled = isDisabled,
                            isInstalled = true
                        )
                    )
                } catch (_: PackageManager.NameNotFoundException) {
                    // Package not found
                }
            }
        }

        result.sortedBy { it.appName }
    }

    suspend fun getBloatwareCount(): Int = identifyBloatware().size

    suspend fun getDisabledCount(): Int = identifyBloatware().count { it.isDisabled }

    suspend fun getTotalBloatwareSize(): Long = withContext(Dispatchers.IO) {
        var total = 0L
        try {
            val storageStatsManager =
                context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val bloatware = identifyBloatware()
            val userHandle = Process.myUserHandle()

            for (app in bloatware) {
                try {
                    val stats = storageStatsManager.queryStatsForPackage(
                        StorageManager.UUID_DEFAULT,
                        app.packageName,
                        userHandle
                    )
                    total += stats.appBytes + stats.dataBytes
                } catch (_: Exception) { }
            }
        } catch (_: SecurityException) { }
        total
    }

    fun disableApp(packageName: String): DisableResult {
        // Check if already disabled
        try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            if (!appInfo.enabled) {
                return DisableResult(
                    success = true,
                    method = DisableMethod.ALREADY_DISABLED,
                    instruction = null
                )
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return DisableResult(
                success = false,
                method = DisableMethod.REQUIRES_ADB,
                instruction = null
            )
        }

        // Try to open app info settings
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            DisableResult(
                success = true,
                method = DisableMethod.OPENED_SETTINGS,
                instruction = "You can also disable via ADB:\nadb shell pm disable-user --user 0 $packageName"
            )
        } catch (_: Exception) {
            DisableResult(
                success = false,
                method = DisableMethod.REQUIRES_ADB,
                instruction = "adb shell pm disable-user --user 0 $packageName"
            )
        }
    }

    fun getDetectedManufacturer(): String {
        return Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    }

    private fun isRelevantManufacturer(manufacturer: String): Boolean {
        return deviceManufacturer.contains(manufacturer.lowercase())
    }

    @Suppress("LongMethod")
    private fun buildBloatwareDatabase(): List<BloatwareEntry> = listOf(
        // ===== Samsung =====
        BloatwareEntry("com.samsung.android.app.sbrowseredge", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Samsung Internet Browser — duplicate if you use Chrome or another browser"),
        BloatwareEntry("com.sec.android.app.sbrowser", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Samsung Browser — older browser package"),
        BloatwareEntry("com.samsung.android.bixby.agent", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Bixby Voice Assistant"),
        BloatwareEntry("com.samsung.android.bixby.service", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Bixby background service"),
        BloatwareEntry("com.samsung.android.visionintelligence", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Bixby Vision image recognition"),
        BloatwareEntry("com.samsung.android.bixby.wakeup", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Bixby wake-up listener"),
        BloatwareEntry("com.samsung.android.spay", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.CAUTION, "Samsung Pay — disable only if you don't use it"),
        BloatwareEntry("com.samsung.android.samsungpass", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.CAUTION, "Samsung Pass password manager"),
        BloatwareEntry("com.sec.android.app.shealth", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Samsung Health fitness tracker"),
        BloatwareEntry("com.samsung.android.app.notes", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Samsung Notes"),
        BloatwareEntry("com.samsung.android.voc", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Samsung Members community app"),
        BloatwareEntry("com.samsung.android.app.spage", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Samsung Free news and entertainment feed"),
        BloatwareEntry("com.sec.android.app.samsungapps", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.CAUTION, "Galaxy Store — Samsung's app store"),
        BloatwareEntry("com.samsung.android.aremoji", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "AR Emoji avatar creator"),
        BloatwareEntry("com.samsung.android.app.camera.sticker.facearavatar.preload", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "AR Emoji stickers"),
        BloatwareEntry("com.samsung.android.kidsinstaller", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Samsung Kids launcher"),
        BloatwareEntry("com.samsung.android.app.watchmanagerstub", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Galaxy Wearable stub — safe if no Samsung watch"),
        BloatwareEntry("com.samsung.android.oneconnect", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.CAUTION, "SmartThings — disable if you have no Samsung IoT devices"),
        BloatwareEntry("com.samsung.android.game.gamehome", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Game Launcher"),
        BloatwareEntry("com.samsung.android.game.gametools", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Game Tools overlay"),
        BloatwareEntry("com.samsung.android.mobileservice", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.CAUTION, "Samsung Experience Service"),
        BloatwareEntry("com.samsung.android.globalgoals", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Samsung Global Goals charity app"),
        BloatwareEntry("com.samsung.android.ardrawing", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "AR Doodle drawing app"),
        BloatwareEntry("com.samsung.android.app.routines", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Bixby Routines automation"),
        BloatwareEntry("com.samsung.android.app.reminder", "Samsung", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Samsung Reminder"),

        // ===== OnePlus / OPPO / Realme =====
        BloatwareEntry("com.oneplus.community", "OnePlus", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "OnePlus Community forums app"),
        BloatwareEntry("com.oneplus.care", "OnePlus", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "OnePlus Care support app"),
        BloatwareEntry("com.oneplus.store", "OnePlus", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "OnePlus Store shopping app"),
        BloatwareEntry("com.heytap.cloud", "OnePlus", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "HeyTap Cloud backup service"),
        BloatwareEntry("com.oplus.orelax", "OnePlus", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "O Relax white noise app"),
        BloatwareEntry("com.oneplus.gamespace", "OnePlus", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Game Space gaming mode tool"),
        BloatwareEntry("com.coloros.browser", "OnePlus", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "ColorOS Browser — duplicate browser"),
        BloatwareEntry("com.heytap.browser", "OnePlus", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "HeyTap Browser — duplicate browser"),
        BloatwareEntry("com.coloros.oshare", "OnePlus", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "OPPO Share file transfer"),
        BloatwareEntry("com.oneplus.membership", "OnePlus", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "OnePlus membership rewards"),
        BloatwareEntry("com.oplus.games", "OnePlus", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Oplus Games center"),

        // ===== Xiaomi / MIUI / Redmi / POCO =====
        BloatwareEntry("com.mi.globalbrowser", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Mi Browser — duplicate browser"),
        BloatwareEntry("com.miui.videoplayer", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Mi Video player"),
        BloatwareEntry("com.miui.player", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Mi Music player"),
        BloatwareEntry("com.mipay.wallet", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Mi Pay mobile wallet"),
        BloatwareEntry("com.xiaomi.mipicks", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "GetApps — Xiaomi app store"),
        BloatwareEntry("com.miui.msa.global", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "MSA ad service — shows system ads"),
        BloatwareEntry("com.miui.analytics", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "MIUI Analytics data collection"),
        BloatwareEntry("com.xiaomi.midrop", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "ShareMe / Mi Drop file sharing"),
        BloatwareEntry("com.mi.android.globalFileexplorer", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Mi File Manager"),
        BloatwareEntry("com.miui.cloudservice", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.CAUTION, "Mi Cloud backup — caution if using cloud sync"),
        BloatwareEntry("com.xiaomi.payment", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Xiaomi Payment service"),
        BloatwareEntry("com.miui.yellowpage", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Mi Yellow Pages caller ID"),
        BloatwareEntry("com.miui.gallery", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.CAUTION, "MIUI Gallery — may be default gallery app"),
        BloatwareEntry("com.mi.health", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Mi Health fitness app"),
        BloatwareEntry("com.xiaomi.scanner", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Mi Scanner QR code reader"),
        BloatwareEntry("com.miui.compass", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "MIUI Compass"),
        BloatwareEntry("com.miui.bugreport", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "MIUI Bug Report tool"),
        BloatwareEntry("com.xiaomi.glgm", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Xiaomi Games"),
        BloatwareEntry("com.miui.personalassistant", "Xiaomi", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "App Vault left panel feed"),

        // ===== Huawei =====
        BloatwareEntry("com.huawei.browser", "Huawei", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Huawei Browser — duplicate browser"),
        BloatwareEntry("com.huawei.himovie.overseas", "Huawei", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Huawei Video streaming"),
        BloatwareEntry("com.huawei.music", "Huawei", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Huawei Music player"),
        BloatwareEntry("com.huawei.hiskytone", "Huawei", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Huawei Themes store"),
        BloatwareEntry("com.huawei.hicare", "Huawei", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "HiCare support and diagnostics"),
        BloatwareEntry("com.huawei.appmarket", "Huawei", BloatwareCategory.MANUFACTURER, BloatwareAction.CAUTION, "Huawei AppGallery — app store"),
        BloatwareEntry("com.huawei.hivision", "Huawei", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "HiVision AI image recognition"),
        BloatwareEntry("com.huawei.maps.app", "Huawei", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Petal Maps navigation"),
        BloatwareEntry("com.huawei.search", "Huawei", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Petal Search"),
        BloatwareEntry("com.huawei.wallet", "Huawei", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Huawei Wallet"),
        BloatwareEntry("com.huawei.tips", "Huawei", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Huawei Tips and tricks app"),
        BloatwareEntry("com.huawei.gameassistant", "Huawei", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Huawei Game Center"),

        // ===== Google (potentially unwanted, user choice) =====
        BloatwareEntry("com.google.android.videos", "Google", BloatwareCategory.UTILITY_DUPLICATE, BloatwareAction.SAFE_TO_DISABLE, "Google TV — video marketplace", universal = true),
        BloatwareEntry("com.google.android.apps.youtube.music", "Google", BloatwareCategory.UTILITY_DUPLICATE, BloatwareAction.SAFE_TO_DISABLE, "YouTube Music — disable if you use another music app", universal = true),
        BloatwareEntry("com.google.android.apps.magazines", "Google", BloatwareCategory.UTILITY_DUPLICATE, BloatwareAction.SAFE_TO_DISABLE, "Google News — news aggregator", universal = true),
        BloatwareEntry("com.google.android.apps.subscriptions.red", "Google", BloatwareCategory.UTILITY_DUPLICATE, BloatwareAction.SAFE_TO_DISABLE, "Google One — cloud storage subscription", universal = true),
        BloatwareEntry("com.google.android.apps.tycho", "Google", BloatwareCategory.UTILITY_DUPLICATE, BloatwareAction.SAFE_TO_DISABLE, "Google Fi — carrier app if not a Fi subscriber", universal = true),
        BloatwareEntry("com.google.android.apps.chromecast.app", "Google", BloatwareCategory.UTILITY_DUPLICATE, BloatwareAction.CAUTION, "Google Home — disable if no Google smart devices", universal = true),
        BloatwareEntry("com.google.android.apps.wellbeing", "Google", BloatwareCategory.UTILITY_DUPLICATE, BloatwareAction.CAUTION, "Digital Wellbeing — screen time tracker", universal = true),
        BloatwareEntry("com.google.android.apps.podcasts", "Google", BloatwareCategory.UTILITY_DUPLICATE, BloatwareAction.SAFE_TO_DISABLE, "Google Podcasts — podcast player", universal = true),
        BloatwareEntry("com.google.android.apps.tachyon", "Google", BloatwareCategory.UTILITY_DUPLICATE, BloatwareAction.SAFE_TO_DISABLE, "Google Meet — video calling app", universal = true),
        BloatwareEntry("com.google.ar.core", "Google", BloatwareCategory.UTILITY_DUPLICATE, BloatwareAction.CAUTION, "Google AR Core — needed by AR apps", universal = true),

        // ===== Carrier: T-Mobile =====
        BloatwareEntry("com.tmobile.pr.mytmobile", "T-Mobile", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "T-Mobile account app", universal = true),
        BloatwareEntry("com.tmobile.pr.adapt", "T-Mobile", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "T-Mobile Device Unlock", universal = true),
        BloatwareEntry("com.tmobile.services", "T-Mobile", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "T-Mobile services background app", universal = true),
        BloatwareEntry("com.tmobile.grsupdater", "T-Mobile", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "T-Mobile GRS Updater", universal = true),
        BloatwareEntry("com.tmobile.contextawareness", "T-Mobile", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "T-Mobile Context Awareness", universal = true),

        // ===== Carrier: Sprint =====
        BloatwareEntry("com.sprint.ce.updater", "Sprint", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "Sprint system updater", universal = true),
        BloatwareEntry("com.sprint.ms.smf.services", "Sprint", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "Sprint background services", universal = true),
        BloatwareEntry("com.sprint.dsa", "Sprint", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "Sprint DSA analytics", universal = true),

        // ===== Carrier: AT&T =====
        BloatwareEntry("com.att.myatt", "AT&T", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "AT&T account management app", universal = true),
        BloatwareEntry("com.att.tv", "AT&T", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "AT&T TV streaming app", universal = true),
        BloatwareEntry("com.att.callprotect", "AT&T", BloatwareCategory.CARRIER, BloatwareAction.CAUTION, "AT&T Call Protect spam blocker", universal = true),
        BloatwareEntry("com.att.android.attsmartwifi", "AT&T", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "AT&T Smart Wi-Fi manager", universal = true),
        BloatwareEntry("com.att.dh", "AT&T", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "AT&T Device Help", universal = true),

        // ===== Carrier: Verizon =====
        BloatwareEntry("com.verizon.mips.services", "Verizon", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "Verizon background services", universal = true),
        BloatwareEntry("com.verizon.llkagent", "Verizon", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "Verizon LLK Agent", universal = true),
        BloatwareEntry("com.vzw.hss.myverizon", "Verizon", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "My Verizon account app", universal = true),
        BloatwareEntry("com.verizon.messaging.vzmsgs", "Verizon", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "Verizon Messages+ — duplicate SMS app", universal = true),
        BloatwareEntry("com.mobitv.client.tv", "Verizon", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "Verizon TV Go", universal = true),

        // ===== Carrier: Vodafone =====
        BloatwareEntry("com.vodafone.myvodafone", "Vodafone", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "My Vodafone account app", universal = true),
        BloatwareEntry("com.vodafone.securenet", "Vodafone", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "Vodafone Secure Net", universal = true),

        // ===== Carrier: Orange =====
        BloatwareEntry("com.orange.myorange", "Orange", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "My Orange account app", universal = true),
        BloatwareEntry("com.orange.update", "Orange", BloatwareCategory.CARRIER, BloatwareAction.SAFE_TO_DISABLE, "Orange system updater", universal = true),

        // ===== Facebook preinstalls =====
        BloatwareEntry("com.facebook.system", "Facebook", BloatwareCategory.SOCIAL_MEDIA_PREINSTALL, BloatwareAction.SAFE_TO_DISABLE, "Facebook App Installer — silently reinstalls Facebook", universal = true),
        BloatwareEntry("com.facebook.appmanager", "Facebook", BloatwareCategory.SOCIAL_MEDIA_PREINSTALL, BloatwareAction.SAFE_TO_DISABLE, "Facebook App Manager — manages Facebook updates", universal = true),
        BloatwareEntry("com.facebook.services", "Facebook", BloatwareCategory.SOCIAL_MEDIA_PREINSTALL, BloatwareAction.SAFE_TO_DISABLE, "Facebook Services — background data collection", universal = true),
        BloatwareEntry("com.facebook.katana", "Facebook", BloatwareCategory.SOCIAL_MEDIA_PREINSTALL, BloatwareAction.SAFE_TO_UNINSTALL, "Facebook — preinstalled social media", universal = true),
        BloatwareEntry("com.instagram.android", "Facebook", BloatwareCategory.SOCIAL_MEDIA_PREINSTALL, BloatwareAction.SAFE_TO_UNINSTALL, "Instagram — preinstalled if bundled", universal = true),

        // ===== Social media preinstalls =====
        BloatwareEntry("com.linkedin.android", "LinkedIn", BloatwareCategory.SOCIAL_MEDIA_PREINSTALL, BloatwareAction.SAFE_TO_UNINSTALL, "LinkedIn — preinstalled professional network", universal = true),
        BloatwareEntry("com.zhiliaoapp.musically", "TikTok", BloatwareCategory.SOCIAL_MEDIA_PREINSTALL, BloatwareAction.SAFE_TO_UNINSTALL, "TikTok — preinstalled short video app", universal = true),
        BloatwareEntry("com.twitter.android", "Twitter", BloatwareCategory.SOCIAL_MEDIA_PREINSTALL, BloatwareAction.SAFE_TO_UNINSTALL, "X (Twitter) — preinstalled social media", universal = true),
        BloatwareEntry("com.snapchat.android", "Snapchat", BloatwareCategory.SOCIAL_MEDIA_PREINSTALL, BloatwareAction.SAFE_TO_UNINSTALL, "Snapchat — preinstalled messaging app", universal = true),

        // ===== Streaming / media preinstalls =====
        BloatwareEntry("com.netflix.partner.activation", "Netflix", BloatwareCategory.OTHER, BloatwareAction.SAFE_TO_DISABLE, "Netflix stub — preinstalled activation stub", universal = true),
        BloatwareEntry("com.netflix.mediaclient", "Netflix", BloatwareCategory.OTHER, BloatwareAction.SAFE_TO_UNINSTALL, "Netflix — preinstalled streaming app", universal = true),
        BloatwareEntry("com.spotify.music", "Spotify", BloatwareCategory.OTHER, BloatwareAction.SAFE_TO_UNINSTALL, "Spotify — preinstalled music streaming", universal = true),

        // ===== Shopping preinstalls =====
        BloatwareEntry("com.amazon.mShop.android.shopping", "Amazon", BloatwareCategory.SHOPPING_PREINSTALL, BloatwareAction.SAFE_TO_UNINSTALL, "Amazon Shopping — preinstalled shopping app", universal = true),
        BloatwareEntry("com.amazon.appmanager", "Amazon", BloatwareCategory.SHOPPING_PREINSTALL, BloatwareAction.SAFE_TO_DISABLE, "Amazon App Manager", universal = true),
        BloatwareEntry("com.ebay.mobile", "eBay", BloatwareCategory.SHOPPING_PREINSTALL, BloatwareAction.SAFE_TO_UNINSTALL, "eBay — preinstalled shopping app", universal = true),
        BloatwareEntry("com.booking", "Booking", BloatwareCategory.SHOPPING_PREINSTALL, BloatwareAction.SAFE_TO_UNINSTALL, "Booking.com — preinstalled travel app", universal = true),

        // ===== Game preinstalls =====
        BloatwareEntry("com.king.candycrushsaga", "King", BloatwareCategory.GAME_PREINSTALL, BloatwareAction.SAFE_TO_UNINSTALL, "Candy Crush Saga — preinstalled game", universal = true),
        BloatwareEntry("com.king.candycrushsodasaga", "King", BloatwareCategory.GAME_PREINSTALL, BloatwareAction.SAFE_TO_UNINSTALL, "Candy Crush Soda Saga — preinstalled game", universal = true),
        BloatwareEntry("com.king.farmheroessaga", "King", BloatwareCategory.GAME_PREINSTALL, BloatwareAction.SAFE_TO_UNINSTALL, "Farm Heroes Saga — preinstalled game", universal = true),
        BloatwareEntry("com.microsoft.office.officehubrow", "Microsoft", BloatwareCategory.UTILITY_DUPLICATE, BloatwareAction.SAFE_TO_UNINSTALL, "Microsoft 365 — preinstalled office hub", universal = true),
        BloatwareEntry("com.microsoft.skydrive", "Microsoft", BloatwareCategory.UTILITY_DUPLICATE, BloatwareAction.SAFE_TO_UNINSTALL, "OneDrive — preinstalled cloud storage", universal = true),
        BloatwareEntry("com.microsoft.office.outlook", "Microsoft", BloatwareCategory.UTILITY_DUPLICATE, BloatwareAction.SAFE_TO_UNINSTALL, "Outlook — preinstalled email app", universal = true),

        // ===== LG =====
        BloatwareEntry("com.lge.qmemoplus", "LG", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "LG QMemo+ note app"),
        BloatwareEntry("com.lge.music", "LG", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "LG Music player"),
        BloatwareEntry("com.lge.sizechangable.weather", "LG", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "LG Weather widget"),

        // ===== Sony =====
        BloatwareEntry("com.sonymobile.sketch", "Sony", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Sony Sketch drawing app"),
        BloatwareEntry("com.sonymobile.music", "Sony", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Sony Music player"),
        BloatwareEntry("com.sonymobile.getmore.client", "Sony", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Sony Xperia Lounge promotional app"),

        // ===== Motorola =====
        BloatwareEntry("com.motorola.help", "Motorola", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Moto Help support app"),
        BloatwareEntry("com.motorola.demo", "Motorola", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Moto retail demo app"),
        BloatwareEntry("com.motorola.tipsapp", "Motorola", BloatwareCategory.MANUFACTURER, BloatwareAction.SAFE_TO_DISABLE, "Moto Tips and suggestions")
    )
}
