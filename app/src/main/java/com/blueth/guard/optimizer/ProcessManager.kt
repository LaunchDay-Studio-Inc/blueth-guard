package com.blueth.guard.optimizer

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Debug
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ProcessCategory {
    ESSENTIAL_SYSTEM,
    USER_ACTIVE,
    BACKGROUND_SERVICE,
    CACHED,
    KILLABLE
}

data class RunningProcess(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val serviceName: String,
    val isForeground: Boolean,
    val processCategory: ProcessCategory,
    val importance: Int,
    val memoryUsageKb: Long,
    val uptime: Long
)

data class KillResult(
    val success: Boolean,
    val processesKilled: Int,
    val estimatedMemoryFreedKb: Long,
    val message: String
)

@Singleton
class ProcessManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val PROTECTED_PREFIXES = listOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.google.android.gms",
            "com.google.android.inputmethod",
            "com.android.phone",
            "com.android.settings",
            "com.android.bluetooth",
            "com.android.nfc",
            "com.android.providers",
            "android",
            "com.android.server",
            "system"
        )
    }

    private val activityManager: ActivityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    suspend fun getRunningProcesses(): List<RunningProcess> = withContext(Dispatchers.IO) {
        val result = mutableListOf<RunningProcess>()
        val pm = context.packageManager
        val runningProcesses = activityManager.runningAppProcesses ?: return@withContext emptyList()

        val pids = runningProcesses.map { it.pid }.toIntArray()
        val memInfos = activityManager.getProcessMemoryInfo(pids)

        for ((index, processInfo) in runningProcesses.withIndex()) {
            val packageName = processInfo.processName.split(":").first()
            val category = categorizeProcess(processInfo, packageName)
            val isForeground = processInfo.importance <=
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE

            val appName: String
            val icon: Drawable?
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                appName = pm.getApplicationLabel(appInfo).toString()
                icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
            } catch (_: PackageManager.NameNotFoundException) {
                continue
            }

            val memoryKb = if (index < memInfos.size) {
                memInfos[index].totalPss.toLong()
            } else {
                0L
            }

            result.add(
                RunningProcess(
                    packageName = packageName,
                    appName = appName,
                    icon = icon,
                    serviceName = processInfo.processName,
                    isForeground = isForeground,
                    processCategory = category,
                    importance = processInfo.importance,
                    memoryUsageKb = memoryKb,
                    uptime = 0L // Exact per-process uptime is not available via public API
                )
            )
        }

        result.sortedByDescending { it.memoryUsageKb }
    }

    fun categorizeProcess(
        processInfo: ActivityManager.RunningAppProcessInfo,
        packageName: String
    ): ProcessCategory {
        // Essential system processes
        if (isProtectedPackage(packageName)) {
            return ProcessCategory.ESSENTIAL_SYSTEM
        }

        // Our own app
        if (packageName == context.packageName) {
            return ProcessCategory.ESSENTIAL_SYSTEM
        }

        return when {
            processInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ->
                ProcessCategory.USER_ACTIVE
            processInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE ->
                ProcessCategory.USER_ACTIVE
            processInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE ->
                ProcessCategory.USER_ACTIVE
            processInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE ->
                ProcessCategory.BACKGROUND_SERVICE
            processInfo.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED ->
                ProcessCategory.CACHED
            else -> ProcessCategory.KILLABLE
        }
    }

    suspend fun getSmartKillList(): List<RunningProcess> = withContext(Dispatchers.IO) {
        val isMusicActive = audioManager.isMusicActive
        getRunningProcesses().filter { process ->
            (process.processCategory == ProcessCategory.KILLABLE ||
                    process.processCategory == ProcessCategory.CACHED) &&
                    !isProtectedPackage(process.packageName) &&
                    process.packageName != context.packageName &&
                    !(isMusicActive && isMediaApp(process.packageName))
        }.sortedByDescending { it.memoryUsageKb }
    }

    fun killProcess(packageName: String): KillResult {
        if (isProtectedPackage(packageName)) {
            return KillResult(
                success = false,
                processesKilled = 0,
                estimatedMemoryFreedKb = 0,
                message = "Cannot kill essential system process"
            )
        }
        if (packageName == context.packageName) {
            return KillResult(
                success = false,
                processesKilled = 0,
                estimatedMemoryFreedKb = 0,
                message = "Cannot kill Blueth Guard"
            )
        }

        return try {
            activityManager.killBackgroundProcesses(packageName)
            KillResult(
                success = true,
                processesKilled = 1,
                estimatedMemoryFreedKb = 0,
                message = "Process killed"
            )
        } catch (e: Exception) {
            KillResult(
                success = false,
                processesKilled = 0,
                estimatedMemoryFreedKb = 0,
                message = e.message ?: "Failed to kill process"
            )
        }
    }

    suspend fun killAllKillable(): KillResult = withContext(Dispatchers.IO) {
        val killList = getSmartKillList()
        var killed = 0
        var totalMemory = 0L

        for (process in killList) {
            try {
                totalMemory += process.memoryUsageKb
                activityManager.killBackgroundProcesses(process.packageName)
                killed++
            } catch (_: Exception) {
                // Continue killing others
            }
        }

        KillResult(
            success = killed > 0,
            processesKilled = killed,
            estimatedMemoryFreedKb = totalMemory,
            message = if (killed > 0) "Killed $killed processes, freed ~${totalMemory / 1024} MB"
            else "No killable processes found"
        )
    }

    private fun isProtectedPackage(packageName: String): Boolean {
        return PROTECTED_PREFIXES.any { prefix ->
            packageName == prefix || packageName.startsWith("$prefix.")
        }
    }

    private fun isMediaApp(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val intent = android.content.Intent("android.intent.action.MUSIC_PLAYER")
            val musicApps = pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }
            packageName in musicApps
        } catch (_: Exception) {
            false
        }
    }

    fun getRamInfo(): Pair<Long, Long> {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return Pair(memInfo.totalMem, memInfo.availMem)
    }
}
