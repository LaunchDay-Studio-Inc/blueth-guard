package com.blueth.guard.privacy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InstallGuardReceiver : BroadcastReceiver() {

    @Inject
    lateinit var installGuard: InstallGuard

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        // Skip our own package
        if (packageName == context.packageName) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                        if (isReplacing) {
                            installGuard.onPackageUpdated(packageName)
                        } else {
                            installGuard.onPackageInstalled(packageName)
                        }
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                        if (!isReplacing) {
                            installGuard.onPackageRemoved(packageName)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
