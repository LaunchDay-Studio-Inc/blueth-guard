package com.blueth.guard.antitheft

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val provider: String,
    val timestamp: Long
)

@Singleton
class AntiTheftManager @Inject constructor(
    private val app: Application
) {
    private var alarmPlayer: MediaPlayer? = null
    private val alarmScope = CoroutineScope(Dispatchers.Main + Job())
    private var alarmTimerJob: Job? = null

    companion object {
        const val ALARM_DURATION_MS = 30_000L // 30 seconds
    }

    private val devicePolicyManager: DevicePolicyManager by lazy {
        app.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    private val adminComponent: ComponentName by lazy {
        ComponentName(app, GuardDeviceAdminReceiver::class.java)
    }

    fun isDeviceAdminEnabled(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    fun getDeviceAdminIntent(): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Blueth Guard needs device admin access for anti-theft features (lock, wipe)."
            )
        }
    }

    fun lockDevice(): Boolean {
        return try {
            if (isDeviceAdminEnabled()) {
                devicePolicyManager.lockNow()
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun wipeDevice(): Boolean {
        return try {
            if (isDeviceAdminEnabled()) {
                devicePolicyManager.wipeData(0)
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun startAlarm() {
        stopAlarm()
        try {
            val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0
            )

            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            alarmPlayer = MediaPlayer().apply {
                setDataSource(app, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            // Auto-stop after 30 seconds
            alarmTimerJob?.cancel()
            alarmTimerJob = alarmScope.launch {
                delay(ALARM_DURATION_MS)
                stopAlarm()
            }

            // Vibrate
            vibrate()
        } catch (_: Exception) { }
    }

    fun stopAlarm() {
        alarmTimerJob?.cancel()
        alarmTimerJob = null
        try {
            alarmPlayer?.stop()
            alarmPlayer?.release()
        } catch (_: Exception) { }
        alarmPlayer = null
    }

    fun isAlarmPlaying(): Boolean = alarmPlayer?.isPlaying == true

    @Suppress("DEPRECATION")
    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), 0)
                )
            } else {
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), 0)
                )
            }
        } catch (_: Exception) { }
    }

    @Suppress("MissingPermission")
    fun getLastKnownLocation(): DeviceLocation? {
        return try {
            val locationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            var bestLocation: android.location.Location? = null

            for (provider in providers) {
                try {
                    val location = locationManager.getLastKnownLocation(provider)
                    if (location != null) {
                        if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                            bestLocation = location
                        }
                    }
                } catch (_: Exception) { }
            }

            bestLocation?.let {
                DeviceLocation(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy,
                    provider = it.provider ?: "unknown",
                    timestamp = it.time
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun hasLocationPermission(): Boolean {
        return app.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED ||
                app.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
