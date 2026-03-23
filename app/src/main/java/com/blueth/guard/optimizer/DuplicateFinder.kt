package com.blueth.guard.optimizer

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

enum class FileCategory {
    PHOTO, VIDEO, AUDIO, DOCUMENT, DOWNLOAD, OTHER;

    companion object {
        private val PHOTO_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "gif")
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "3gp", "webm")
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav", "flac", "ogg", "aac", "wma")
        private val DOCUMENT_EXTENSIONS = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv")

        fun fromExtension(extension: String, isInDownloads: Boolean): FileCategory {
            val ext = extension.lowercase()
            return when {
                ext in PHOTO_EXTENSIONS -> PHOTO
                ext in VIDEO_EXTENSIONS -> VIDEO
                ext in AUDIO_EXTENSIONS -> AUDIO
                ext in DOCUMENT_EXTENSIONS -> DOCUMENT
                isInDownloads -> DOWNLOAD
                else -> OTHER
            }
        }
    }
}

data class DuplicateFile(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?
)

data class DuplicateGroup(
    val hash: String,
    val files: List<DuplicateFile>,
    val totalWastedBytes: Long,
    val category: FileCategory
)

data class DuplicateScanProgress(
    val scannedFiles: Int,
    val totalFiles: Int,
    val currentDir: String,
    val foundGroups: Int
)

data class DeleteResult(
    val deleted: Int,
    val failed: Int,
    val freedBytes: Long
)

@Singleton
class DuplicateFinder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val MAX_FILE_SIZE = 500L * 1024 * 1024 // 500 MB
        private const val MIN_FILE_SIZE = 1024L // 1 KB
        private const val HASH_BUFFER_SIZE = 8192 // 8 KB
    }

    private var cachedResults: List<DuplicateGroup> = emptyList()

    private val scanDirectories: List<File>
        get() = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        ).filter { it.exists() && it.canRead() }

    private val downloadsPath: String by lazy {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .absolutePath
    }

    fun scanForDuplicates(
        categories: Set<FileCategory> = FileCategory.entries.toSet()
    ): Flow<DuplicateScanProgress> = flow {
        val allFiles = mutableListOf<File>()

        // Collect all scannable files
        for (dir in scanDirectories) {
            collectFiles(dir, allFiles)
        }

        val totalFiles = allFiles.size
        emit(DuplicateScanProgress(0, totalFiles, "Grouping by size...", 0))

        // Group files by size (optimization: only hash files that share a size)
        val sizeGroups = allFiles.groupBy { it.length() }
            .filter { it.value.size > 1 }

        val filesToHash = sizeGroups.values.flatten()
        val hashGroups = mutableMapOf<String, MutableList<DuplicateFile>>()
        var scanned = 0

        for (file in filesToHash) {
            val coroutineContext = currentCoroutineContext()
            if (!coroutineContext.isActive) break

            scanned++
            if (scanned % 10 == 0 || scanned == filesToHash.size) {
                emit(
                    DuplicateScanProgress(
                        scannedFiles = scanned,
                        totalFiles = filesToHash.size,
                        currentDir = file.parentFile?.name ?: "",
                        foundGroups = hashGroups.count { it.value.size > 1 }
                    )
                )
            }

            val hash = computeMd5(file) ?: continue
            val ext = file.extension
            val isInDownloads = file.absolutePath.startsWith(downloadsPath)
            val category = FileCategory.fromExtension(ext, isInDownloads)

            if (category !in categories) continue

            val dupFile = DuplicateFile(
                path = file.absolutePath,
                name = file.name,
                size = file.length(),
                lastModified = file.lastModified(),
                mimeType = getMimeType(ext)
            )
            hashGroups.getOrPut(hash) { mutableListOf() }.add(dupFile)
        }

        // Build duplicate groups (only groups with > 1 file)
        cachedResults = hashGroups.filter { it.value.size > 1 }.map { (hash, files) ->
            val fileSize = files.first().size
            val ext = files.first().name.substringAfterLast(".", "")
            val isInDownloads = files.first().path.startsWith(downloadsPath)

            DuplicateGroup(
                hash = hash,
                files = files.sortedByDescending { it.lastModified },
                totalWastedBytes = fileSize * (files.size - 1),
                category = FileCategory.fromExtension(ext, isInDownloads)
            )
        }.sortedByDescending { it.totalWastedBytes }

        emit(
            DuplicateScanProgress(
                scannedFiles = filesToHash.size,
                totalFiles = filesToHash.size,
                currentDir = "Complete",
                foundGroups = cachedResults.size
            )
        )
    }.flowOn(Dispatchers.IO)

    fun getScanResults(): List<DuplicateGroup> = cachedResults

    fun getTotalWastedSpace(): Long = cachedResults.sumOf { it.totalWastedBytes }

    fun deleteFile(path: String): Boolean {
        return try {
            val file = File(path)
            if (!file.exists()) return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Try MediaStore deletion for scoped storage
                val deleted = deleteViaMediaStore(path)
                if (deleted) return true
            }
            // Fallback to direct deletion
            file.delete()
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun deleteFiles(paths: List<String>): DeleteResult {
        var deleted = 0
        var failed = 0
        var freedBytes = 0L

        for (path in paths) {
            val file = File(path)
            val size = if (file.exists()) file.length() else 0L

            if (deleteFile(path)) {
                deleted++
                freedBytes += size
            } else {
                failed++
            }
        }

        return DeleteResult(deleted = deleted, failed = failed, freedBytes = freedBytes)
    }

    private fun collectFiles(dir: File, result: MutableList<File>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                // Skip Android/data and hidden directories
                if (file.name == "Android" || file.name.startsWith(".")) continue
                collectFiles(file, result)
            } else if (file.isFile && file.length() in MIN_FILE_SIZE..MAX_FILE_SIZE && file.canRead()) {
                result.add(file)
            }
        }
    }

    private fun computeMd5(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(HASH_BUFFER_SIZE)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun deleteViaMediaStore(path: String): Boolean {
        return try {
            val contentResolver: ContentResolver = context.contentResolver
            val uri = MediaStore.Files.getContentUri("external")
            val deleted = contentResolver.delete(
                uri,
                "${MediaStore.MediaColumns.DATA} = ?",
                arrayOf(path)
            )
            deleted > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun getMimeType(extension: String): String? {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            else -> null
        }
    }
}
