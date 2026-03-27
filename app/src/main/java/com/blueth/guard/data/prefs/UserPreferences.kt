package com.blueth.guard.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { DARK, LIGHT, AMOLED, SYSTEM }
enum class ScanInterval { DAILY, WEEKLY, MONTHLY, MANUAL }

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val SCAN_SCHEDULE_ENABLED = booleanPreferencesKey("scan_schedule_enabled")
        val SCAN_INTERVAL = stringPreferencesKey("scan_interval")
        val REAL_TIME_PROTECTION = booleanPreferencesKey("real_time_protection")
        val INSTALL_SCAN_ENABLED = booleanPreferencesKey("install_scan_enabled")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SIGNATURE_DB_VERSION = intPreferencesKey("signature_db_version")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.entries[prefs[Keys.THEME_MODE] ?: 0]
    }

    val scanScheduleEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCAN_SCHEDULE_ENABLED] ?: false
    }

    val scanInterval: Flow<ScanInterval> = context.dataStore.data.map { prefs ->
        ScanInterval.valueOf(prefs[Keys.SCAN_INTERVAL] ?: ScanInterval.WEEKLY.name)
    }

    val realTimeProtection: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.REAL_TIME_PROTECTION] ?: false
    }

    val installScanEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.INSTALL_SCAN_ENABLED] ?: true
    }

    val notificationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATION_ENABLED] ?: true
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    val signatureDbVersion: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.SIGNATURE_DB_VERSION] ?: 0
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.ordinal }
    }

    suspend fun setScanScheduleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SCAN_SCHEDULE_ENABLED] = enabled }
    }

    suspend fun setScanInterval(interval: ScanInterval) {
        context.dataStore.edit { it[Keys.SCAN_INTERVAL] = interval.name }
    }

    suspend fun setRealTimeProtection(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REAL_TIME_PROTECTION] = enabled }
    }

    suspend fun setInstallScanEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INSTALL_SCAN_ENABLED] = enabled }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setSignatureDbVersion(version: Int) {
        context.dataStore.edit { it[Keys.SIGNATURE_DB_VERSION] = version }
    }
}
