package com.blueth.guard.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.blueth.guard.data.model.AppInfo
import com.blueth.guard.ui.theme.RiskCritical
import com.blueth.guard.ui.theme.RiskHigh
import com.blueth.guard.ui.theme.RiskMedium
import com.blueth.guard.ui.theme.RiskSafe
import com.blueth.guard.ui.viewmodel.AppListViewModel

private val TRACKED_PERMISSIONS = listOf(
    Manifest.permission.CAMERA to "CAM",
    Manifest.permission.RECORD_AUDIO to "MIC",
    Manifest.permission.ACCESS_FINE_LOCATION to "LOC",
    Manifest.permission.READ_CONTACTS to "CON",
    Manifest.permission.READ_SMS to "SMS",
    Manifest.permission.CALL_PHONE to "CALL",
    Manifest.permission.READ_CALENDAR to "CAL",
    Manifest.permission.BODY_SENSORS to "SENS",
    Manifest.permission.READ_EXTERNAL_STORAGE to "STOR",
    Manifest.permission.INTERNET to "NET"
)

@Composable
fun PrivacyScreen(
    viewModel: AppListViewModel = hiltViewModel()
) {
    val apps by viewModel.apps.collectAsState()
    val userApps = remember(apps) { apps.filter { !it.isSystem && it.permissions.isNotEmpty() } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "Privacy",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Text(
                "Permission Heatmap",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Shows which permissions each app uses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Legend
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendItem("Granted", RiskCritical)
                LegendItem("Requested", RiskMedium)
                LegendItem("Not used", MaterialTheme.colorScheme.surface)
            }
        }

        // Permission heatmap header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                // App name column
                Box(modifier = Modifier.width(120.dp))

                // Permission columns
                TRACKED_PERMISSIONS.forEach { (_, shortName) ->
                    Box(
                        modifier = Modifier.width(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            shortName,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Heatmap rows
        items(userApps.take(50), key = { it.packageName }) { app ->
            PermissionHeatmapRow(app)
        }

        // Permission summary
        item {
            Spacer(Modifier.height(8.dp))
            PermissionSummaryCard(userApps)
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PermissionHeatmapRow(app: AppInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App name
        Text(
            app.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(120.dp)
        )

        // Permission cells
        TRACKED_PERMISSIONS.forEach { (permission, _) ->
            val hasPermission = app.permissions.contains(permission)
            val color = when {
                hasPermission && permission in listOf(
                    Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_SMS, Manifest.permission.ACCESS_FINE_LOCATION
                ) -> RiskHigh
                hasPermission -> RiskMedium
                else -> MaterialTheme.colorScheme.surface
            }

            Box(
                modifier = Modifier
                    .width(40.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun PermissionSummaryCard(apps: List<AppInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Permission Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            TRACKED_PERMISSIONS.forEach { (permission, shortName) ->
                val count = apps.count { permission in it.permissions }
                val percentage = if (apps.isNotEmpty()) (count * 100) / apps.size else 0
                val color = when {
                    percentage > 70 -> RiskCritical
                    percentage > 40 -> RiskMedium
                    else -> RiskSafe
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        shortName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(40.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percentage / 100f)
                                .height(8.dp)
                                .background(color)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$count apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(50.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
