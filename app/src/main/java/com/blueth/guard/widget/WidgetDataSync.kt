package com.blueth.guard.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetDataSync @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun updateWidgetData(
        overallScore: Int,
        lastScanTime: Long,
        riskyApps: Int,
        protectionEnabled: Boolean
    ) {
        context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
            .edit()
            .putInt("overall_score", overallScore)
            .putLong("last_scan_time", lastScanTime)
            .putInt("risky_apps", riskyApps)
            .putBoolean("protection_enabled", protectionEnabled)
            .apply()

        withContext(Dispatchers.Main) {
            try {
                val widgetManager = GlanceAppWidgetManager(context)
                val glanceIds = widgetManager.getGlanceIds(GuardWidget::class.java)
                glanceIds.forEach { glanceId ->
                    GuardWidget().update(context, glanceId)
                }
            } catch (_: Exception) {
                // Widget may not be placed on home screen
            }
        }
    }
}
