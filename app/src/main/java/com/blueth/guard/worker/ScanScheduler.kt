package com.blueth.guard.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.blueth.guard.data.prefs.ScanInterval
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val WORK_NAME = "blueth_guard_scheduled_scan"
    }

    fun schedulePeriodicScan(interval: ScanInterval) {
        val repeatInterval = when (interval) {
            ScanInterval.DAILY -> 1L
            ScanInterval.WEEKLY -> 7L
            ScanInterval.MONTHLY -> 30L
            ScanInterval.MANUAL -> return
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ScheduledScanWorker>(
            repeatInterval, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelScheduledScan() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
