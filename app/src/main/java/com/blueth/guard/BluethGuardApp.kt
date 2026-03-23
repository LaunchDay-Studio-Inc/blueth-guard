package com.blueth.guard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BluethGuardApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannel(
                "security_alerts",
                "Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for malware detection and security threats"
            },
            NotificationChannel(
                "install_guard",
                "Install Guard",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when new apps are installed"
            },
            NotificationChannel(
                "battery_warnings",
                "Battery Warnings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts about battery-draining apps"
            },
            NotificationChannel(
                "scan_results",
                "Scan Results",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Results from scheduled security scans"
            },
            NotificationChannel(
                "protection_service",
                "Protection Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Ongoing notification for real-time protection"
            }
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannels(channels)
    }
}
