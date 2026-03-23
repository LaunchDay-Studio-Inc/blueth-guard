package com.blueth.guard.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.blueth.guard.MainActivity
import com.blueth.guard.R
import com.blueth.guard.ui.theme.RiskHigh

class GuardWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GuardWidgetContent()
        }
    }
}

@Composable
private fun GuardWidgetContent() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
    val overallScore = prefs.getInt("overall_score", -1)
    val riskyApps = prefs.getInt("risky_apps", 0)
    val protectionEnabled = prefs.getBoolean("protection_enabled", false)

    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_notification),
                    contentDescription = "Shield",
                    modifier = GlanceModifier.size(24.dp)
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = if (overallScore >= 0) "$overallScore" else "—",
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    )
                )
            }

            Text(
                text = when {
                    overallScore < 0 -> "Not scanned"
                    overallScore >= 90 -> "Excellent"
                    overallScore >= 75 -> "Good"
                    overallScore >= 50 -> "Fair"
                    overallScore >= 25 -> "Needs Attention"
                    else -> "At Risk"
                },
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )

            Spacer(GlanceModifier.height(4.dp))

            if (riskyApps > 0) {
                Text(
                    text = "$riskyApps threats found",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(RiskHigh, RiskHigh)
                    )
                )
            } else if (overallScore >= 0) {
                Text(
                    text = if (protectionEnabled) "Protected" else "Protection off",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            androidx.glance.Button(
                text = "Open Guard",
                onClick = actionStartActivity<MainActivity>(),
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }
}
