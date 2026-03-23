package com.blueth.guard.protection

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.pm.ApplicationInfo
import android.view.accessibility.AccessibilityManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityWatcher @Inject constructor(
    private val app: Application
) {
    fun getAccessibilityServices(): List<AccessibilityServiceInfo> {
        val am = app.getSystemService(AccessibilityManager::class.java) ?: return emptyList()
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    }

    fun getSuspiciousServices(): List<AccessibilityServiceInfo> {
        val dangerousFlags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

        return getAccessibilityServices().filter { info ->
            val resolveInfo = info.resolveInfo ?: return@filter false
            val appInfo = resolveInfo.serviceInfo?.applicationInfo ?: return@filter false
            val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            !isSystem && (info.flags and dangerousFlags) != 0
        }
    }
}
