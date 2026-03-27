package com.blueth.guard.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.blueth.guard.update.SignatureUpdateManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SignatureUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateManager: SignatureUpdateManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val result = updateManager.checkAndUpdate()
        return if (result.success) Result.success() else Result.retry()
    }
}
