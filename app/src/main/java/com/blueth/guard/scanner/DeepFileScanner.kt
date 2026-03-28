package com.blueth.guard.scanner

import android.app.Application
import android.content.pm.PackageManager
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

enum class FileThreatType {
    MALWARE_HASH, SUSPICIOUS_APK, DOUBLE_EXTENSION, CORRUPTED, SUSPICIOUS_SCRIPT, CLEAN
}

enum class FileSeverity {
    CRITICAL, HIGH, MEDIUM, LOW, INFO
}

data class FileScanResult(
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val threat: FileThreatType,
    val severity: FileSeverity,
    val description: String
)

data class LargeFileInfo(
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long
)

data class LeftoverAppData(
    val packageName: String,
    val folderPath: String,
    val folderSize: Long
)

data class DeepScanProgress(
    val scannedFiles: Int,
    val totalFiles: Int,
    val currentPath: String,
    val elapsedMs: Long = 0L
)

data class DeepScanStats(
    val filesScanned: Int,
    val threatsFound: Int,
    val corruptedFound: Int,
    val scanDurationMs: Long
)

data class DeepScanResult(
    val threats: List<FileScanResult>,
    val stats: DeepScanStats,
    val largeFiles: List<LargeFileInfo>,
    val oldFiles: List<LargeFileInfo>,
    val leftovers: List<LeftoverAppData>
)

@Singleton
class DeepFileScanner @Inject constructor(
    private val app: Application
) {
    companion object {
        private val SKIP_DIRS = setOf("/proc", "/sys", "/dev", "/data/app")
        private val DOUBLE_EXT_PATTERN = Regex(".*\\.(jpg|jpeg|png|gif|pdf|doc|docx|xls|xlsx|mp3|mp4)\\.(exe|apk|js|bat|cmd|ps1|vbs|sh)$", RegexOption.IGNORE_CASE)
        private val SUSPICIOUS_SCRIPT_EXTS = setOf(".sh", ".bat", ".cmd", ".ps1", ".vbs")
        private val KNOWN_APP_DIRS = setOf("/data/app", "/system/app", "/system/priv-app", "/vendor/app")
        private const val SINGLE_FILE_TIMEOUT_MS = 2000L
        private const val TOTAL_SCAN_TIMEOUT_MS = 600_000L // 10 minutes
        private const val LARGE_FILE_THRESHOLD = 100L * 1024 * 1024 // 100MB
        private const val OLD_FILE_THRESHOLD = 50L * 1024 * 1024 // 50MB
        private const val OLD_FILE_AGE_MS = 180L * 24 * 3600_000 // 6 months

        // File magic bytes
        private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte())
        private val ZIP_MAGIC = byteArrayOf(0x50.toByte(), 0x4B.toByte())
        private val APK_MAGIC = ZIP_MAGIC // APKs are ZIP files
    }

    suspend fun scan(
        onProgress: (DeepScanProgress) -> Unit = {}
    ): DeepScanResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<FileScanResult>()
        val largeFiles = mutableListOf<LargeFileInfo>()
        val oldFiles = mutableListOf<LargeFileInfo>()
        val hashDb = FileHashDatabase.hashes

        // Build scan roots
        val scanRoots = mutableListOf<File>()
        scanRoots.add(Environment.getExternalStorageDirectory())
        // Try app-specific data dirs
        app.filesDir.parentFile?.let { scanRoots.add(it) }
        // Try SD card
        val storageDir = File("/storage")
        if (storageDir.exists() && storageDir.canRead()) {
            storageDir.listFiles()?.filter { it.isDirectory && it.name != "emulated" && it.name != "self" }
                ?.forEach { scanRoots.add(it) }
        }

        val allFiles = mutableListOf<File>()

        // Collect files from all roots
        for (root in scanRoots) {
            try {
                root.walkTopDown()
                    .maxDepth(20)
                    .onEnter { dir ->
                        val path = dir.absolutePath
                        !SKIP_DIRS.any { path.startsWith(it) }
                    }
                    .filter { it.isFile }
                    .take(500_000) // Increased safety limit
                    .forEach { allFiles.add(it) }
            } catch (_: Exception) { }
        }

        val totalFiles = allFiles.size
        var scannedCount = 0
        var threatsFound = 0
        var corruptedFound = 0
        val now = System.currentTimeMillis()

        val scanResult = withTimeoutOrNull(TOTAL_SCAN_TIMEOUT_MS) {
            for (file in allFiles) {
                val fileResult = withTimeoutOrNull(SINGLE_FILE_TIMEOUT_MS) {
                    scanSingleFile(file, hashDb)
                }

                fileResult?.let { result ->
                    if (result.threat != FileThreatType.CLEAN) {
                        results.add(result)
                        if (result.threat == FileThreatType.CORRUPTED) corruptedFound++
                        else threatsFound++
                    }
                }

                // Large file detection (> 100MB)
                val fileSize = file.length()
                if (fileSize > LARGE_FILE_THRESHOLD) {
                    largeFiles.add(LargeFileInfo(file.absolutePath, file.name, fileSize, file.lastModified()))
                }

                // Old file detection (> 50MB and not modified in 6 months)
                if (fileSize > OLD_FILE_THRESHOLD && (now - file.lastModified()) > OLD_FILE_AGE_MS) {
                    oldFiles.add(LargeFileInfo(file.absolutePath, file.name, fileSize, file.lastModified()))
                }

                scannedCount++
                if (scannedCount % 100 == 0 || scannedCount == totalFiles) {
                    onProgress(DeepScanProgress(
                        scannedFiles = scannedCount,
                        totalFiles = totalFiles,
                        currentPath = file.absolutePath,
                        elapsedMs = System.currentTimeMillis() - startTime
                    ))
                }
            }
        }

        // Leftover app data detection
        val leftovers = detectLeftoverAppData()

        val stats = DeepScanStats(
            filesScanned = scannedCount,
            threatsFound = threatsFound,
            corruptedFound = corruptedFound,
            scanDurationMs = System.currentTimeMillis() - startTime
        )

        DeepScanResult(
            threats = results,
            stats = stats,
            largeFiles = largeFiles.sortedByDescending { it.fileSize },
            oldFiles = oldFiles.sortedByDescending { it.fileSize },
            leftovers = leftovers
        )
    }

    private fun detectLeftoverAppData(): List<LeftoverAppData> {
        val leftovers = mutableListOf<LeftoverAppData>()
        val pm = app.packageManager
        val installedPackages = pm.getInstalledPackages(0).map { it.packageName }.toSet()

        // Check /Android/data/ for folders from uninstalled apps
        val androidDataDir = File(Environment.getExternalStorageDirectory(), "Android/data")
        if (androidDataDir.exists() && androidDataDir.canRead()) {
            androidDataDir.listFiles()?.forEach { dir ->
                if (dir.isDirectory && dir.name !in installedPackages) {
                    val size = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    if (size > 0) {
                        leftovers.add(LeftoverAppData(dir.name, dir.absolutePath, size))
                    }
                }
            }
        }

        return leftovers.sortedByDescending { it.folderSize }
    }

    private fun scanSingleFile(file: File, hashDb: Set<String>): FileScanResult {
        val path = file.absolutePath
        val name = file.name
        val size = file.length()

        // Check double extensions
        if (DOUBLE_EXT_PATTERN.matches(name)) {
            return FileScanResult(path, name, size, FileThreatType.DOUBLE_EXTENSION, FileSeverity.HIGH,
                "Suspicious double extension detected: $name")
        }

        // Check suspicious scripts outside known app dirs
        val ext = name.substringAfterLast('.', "").lowercase()
        if (".$ext" in SUSPICIOUS_SCRIPT_EXTS && !KNOWN_APP_DIRS.any { path.startsWith(it) }) {
            return FileScanResult(path, name, size, FileThreatType.SUSPICIOUS_SCRIPT, FileSeverity.MEDIUM,
                "Script file found outside app directories: $name")
        }

        // Check sideloaded APKs
        if (ext == "apk" && !KNOWN_APP_DIRS.any { path.startsWith(it) }) {
            return FileScanResult(path, name, size, FileThreatType.SUSPICIOUS_APK, FileSeverity.MEDIUM,
                "APK file found outside system directories: $name")
        }

        // Check zero-byte files (corrupted)
        if (size == 0L && file.parentFile?.listFiles()?.any { it.length() > 0 } == true) {
            return FileScanResult(path, name, size, FileThreatType.CORRUPTED, FileSeverity.LOW,
                "Zero-byte file in non-empty directory")
        }

        // Check file headers for corruption
        if (size > 0) {
            val corruptionResult = checkFileCorruption(file, ext, path, name, size)
            if (corruptionResult != null) return corruptionResult
        }

        // Check malware hash (only for small-ish files to avoid memory issues)
        if (size in 1..50_000_000) {
            try {
                val hash = sha256(file)
                if (hash in hashDb) {
                    return FileScanResult(path, name, size, FileThreatType.MALWARE_HASH, FileSeverity.CRITICAL,
                        "File matches known malware hash")
                }
            } catch (_: Exception) { }
        }

        return FileScanResult(path, name, size, FileThreatType.CLEAN, FileSeverity.INFO, "")
    }

    private fun checkFileCorruption(file: File, ext: String, path: String, name: String, size: Long): FileScanResult? {
        try {
            val header = ByteArray(12)
            FileInputStream(file).use { fis ->
                val read = fis.read(header)
                if (read < 4) return null
            }

            when (ext) {
                "jpg", "jpeg" -> {
                    if (!header.startsWith(JPEG_MAGIC)) {
                        return FileScanResult(path, name, size, FileThreatType.CORRUPTED, FileSeverity.LOW,
                            "JPEG file has invalid header")
                    }
                }
                "png" -> {
                    if (!header.startsWith(PNG_MAGIC)) {
                        return FileScanResult(path, name, size, FileThreatType.CORRUPTED, FileSeverity.LOW,
                            "PNG file has invalid header")
                    }
                }
                "mp4", "m4a", "m4v" -> {
                    // Check for 'ftyp' box at offset 4
                    val ftyp = String(header, 4, 4, Charsets.US_ASCII)
                    if (ftyp != "ftyp" && size > 1024) {
                        return FileScanResult(path, name, size, FileThreatType.CORRUPTED, FileSeverity.LOW,
                            "MP4 file missing ftyp header")
                    }
                }
                "apk", "zip" -> {
                    if (!header.startsWith(ZIP_MAGIC)) {
                        return FileScanResult(path, name, size, FileThreatType.CORRUPTED, FileSeverity.LOW,
                            "ZIP/APK file has invalid header")
                    }
                }
            }
        } catch (_: Exception) { }
        return null
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (this.size < prefix.size) return false
        for (i in prefix.indices) {
            if (this[i] != prefix[i]) return false
        }
        return true
    }
}
