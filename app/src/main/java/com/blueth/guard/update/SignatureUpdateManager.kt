package com.blueth.guard.update

import android.content.Context
import android.util.Log
import com.blueth.guard.data.prefs.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignatureUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cacheFile = File(context.filesDir, "signature-db-v2.json")

    private val updateUrl = "https://raw.githubusercontent.com/LaunchDay-Studio-Inc/blueth-guard/main/signature-db/latest.json"

    data class UpdateResult(
        val success: Boolean,
        val newVersion: Int?,
        val previousVersion: Int?,
        val message: String
    )

    suspend fun checkAndUpdate(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val currentVersion = userPreferences.signatureDbVersion.first()

            val connection = URL(updateUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"

            if (connection.responseCode != 200) {
                return@withContext UpdateResult(false, null, currentVersion, "Server returned ${connection.responseCode}")
            }

            val responseBody = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val manifest = json.decodeFromString<SignatureUpdateManifest>(responseBody)

            if (manifest.version <= currentVersion) {
                return@withContext UpdateResult(true, manifest.version, currentVersion, "Already up to date")
            }

            // Save to cache
            cacheFile.writeText(responseBody)
            userPreferences.setSignatureDbVersion(manifest.version)

            UpdateResult(
                success = true,
                newVersion = manifest.version,
                previousVersion = currentVersion,
                message = "Updated from v$currentVersion to v${manifest.version}"
            )
        } catch (e: Exception) {
            Log.w("SignatureUpdate", "Update failed: ${e.message}")
            UpdateResult(false, null, null, "Update failed: ${e.message}")
        }
    }

    fun getCachedManifest(): SignatureUpdateManifest? {
        return try {
            if (cacheFile.exists()) {
                json.decodeFromString<SignatureUpdateManifest>(cacheFile.readText())
            } else null
        } catch (_: Exception) { null }
    }

    fun getLocalVersion(): Int {
        return try {
            if (cacheFile.exists()) {
                json.decodeFromString<SignatureUpdateManifest>(cacheFile.readText()).version
            } else 0
        } catch (_: Exception) { 0 }
    }
}
