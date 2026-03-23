package com.blueth.guard.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.blueth.guard.notification.NotificationHelper
import com.blueth.guard.scanner.SecurityScanner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.last

@HiltWorker
class ScheduledScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val securityScanner: SecurityScanner,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            var totalApps = 0
            var threatsFound = 0

            securityScanner.scanAll().collect { progress ->
                totalApps = progress.total
                threatsFound = progress.results.count {
                    it.threatAssessment.riskLevel.name in listOf("HIGH", "CRITICAL")
                }
            }

            notificationHelper.showScanComplete(totalApps, threatsFound)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
