package com.blueth.guard.ui.screens

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.blueth.guard.data.local.InstallEvent
import com.blueth.guard.data.local.NetworkUsageSummary
import com.blueth.guard.data.local.PermissionEvent
import com.blueth.guard.data.model.AppInfo
import com.blueth.guard.privacy.AppPrivacyProfile
import com.blueth.guard.privacy.AppPrivacyScore
import com.blueth.guard.privacy.ClipboardEvent
import com.blueth.guard.privacy.ClipboardStatus
import com.blueth.guard.privacy.DevicePrivacyScore
import com.blueth.guard.privacy.InstallAction
import com.blueth.guard.privacy.NetworkMonitor
import com.blueth.guard.privacy.PrivacyRecommendation
import com.blueth.guard.privacy.RecommendationSeverity
import com.blueth.guard.privacy.SuspiciousNetworkApp
import com.blueth.guard.ui.theme.BluePrimary
import com.blueth.guard.ui.theme.CyanSecondary
import com.blueth.guard.ui.theme.RiskCritical
import com.blueth.guard.ui.theme.RiskHigh
import com.blueth.guard.ui.theme.RiskLow
import com.blueth.guard.ui.theme.RiskMedium
import com.blueth.guard.ui.theme.RiskSafe
import com.blueth.guard.ui.viewmodel.PrivacyTab
import com.blueth.guard.ui.viewmodel.PrivacyViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    viewModel: PrivacyViewModel = hiltViewModel()
) {
    val currentTab by viewModel.privacyTab.collectAsState()
    val deviceScore by viewModel.devicePrivacyScore.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Privacy",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            // Tab Row
            PrivacyTabRow(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) },
                permissionEventCount = viewModel.permissionEvents.collectAsState().value.size,
                installEventCount = viewModel.installEvents.collectAsState().value.size
            )

            // Tab Content
            Crossfade(targetState = currentTab, label = "privacy_tab") { tab ->
                when (tab) {
                    PrivacyTab.OVERVIEW -> OverviewTab(viewModel)
                    PrivacyTab.PERMISSIONS -> PermissionsTab(viewModel)
                    PrivacyTab.NETWORK -> NetworkTab(viewModel)
                    PrivacyTab.CLIPBOARD -> ClipboardTab(viewModel)
                    PrivacyTab.INSTALLS -> InstallsTab(viewModel)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PrivacyTabRow(
    currentTab: PrivacyTab,
    onTabSelected: (PrivacyTab) -> Unit,
    permissionEventCount: Int,
    installEventCount: Int
) {
    val tabs = PrivacyTab.entries
    val tabLabels = listOf("Overview", "Permissions", "Network", "Clipboard", "Installs")

    ScrollableTabRow(
        selectedTabIndex = tabs.indexOf(currentTab),
        containerColor = MaterialTheme.colorScheme.surface,
        edgePadding = 16.dp
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tabLabels[index])
                        val badgeCount = when (tab) {
                            PrivacyTab.PERMISSIONS -> permissionEventCount
                            PrivacyTab.INSTALLS -> installEventCount
                            else -> 0
                        }
                        if (badgeCount > 0) {
                            Spacer(Modifier.width(4.dp))
                            Badge(
                                containerColor = BluePrimary
                            ) {
                                Text("$badgeCount", fontSize = 10.sp)
                            }
                        }
                    }
                }
            )
        }
    }
}

// ===== OVERVIEW TAB =====

@Composable
private fun OverviewTab(viewModel: PrivacyViewModel) {
    val deviceScore by viewModel.devicePrivacyScore.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val appScores by viewModel.appPrivacyScores.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Privacy Score Circle
        item {
            PrivacyScoreCircle(score = deviceScore?.overallScore ?: 0)
        }

        // Quick Stats Grid
        item {
            deviceScore?.stats?.let { stats ->
                QuickStatsGrid(stats)
            }
        }

        // Recommendations
        if (recommendations.isNotEmpty()) {
            item {
                Text(
                    "Top Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(recommendations.take(5)) { rec ->
                RecommendationCard(rec, context)
            }
        }

        // Most Intrusive Apps
        if (appScores.isNotEmpty()) {
            item {
                Text(
                    "Most Intrusive Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(appScores.take(5)) { score ->
                IntrusiveAppRow(score)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PrivacyScoreCircle(score: Int) {
    val color = when {
        score >= 80 -> RiskSafe
        score >= 50 -> RiskMedium
        score >= 30 -> RiskHigh
        else -> RiskCritical
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.size(120.dp),
                    color = color,
                    strokeWidth = 10.dp,
                    trackColor = MaterialTheme.colorScheme.surface,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$score",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        "/100",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Your Privacy Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickStatsGrid(stats: com.blueth.guard.privacy.PrivacyStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Camera,
                label = "Camera Access",
                value = "${stats.appsWithCamera}",
                color = if (stats.appsWithCamera > 10) RiskHigh else CyanSecondary
            )
            QuickStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.LocationOn,
                label = "Location Access",
                value = "${stats.appsWithLocation}",
                color = if (stats.appsWithLocation > 15) RiskHigh else CyanSecondary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.GetApp,
                label = "Sideloaded",
                value = "${stats.sideloadedApps}",
                color = if (stats.sideloadedApps > 0) RiskMedium else RiskSafe
            )
            QuickStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.GppBad,
                label = "High Risk",
                value = "${stats.highRiskApps}",
                color = if (stats.highRiskApps > 0) RiskCritical else RiskSafe
            )
        }
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RecommendationCard(rec: PrivacyRecommendation, context: android.content.Context) {
    val severityColor = when (rec.severity) {
        RecommendationSeverity.CRITICAL -> RiskCritical
        RecommendationSeverity.HIGH -> RiskHigh
        RecommendationSeverity.MEDIUM -> RiskMedium
        RecommendationSeverity.LOW -> RiskLow
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = "Warning",
                tint = severityColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rec.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(rec.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (rec.targetPackage != null) {
                IconButton(onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.parse("package:${rec.targetPackage}")
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) { }
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = "Details", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun IntrusiveAppRow(score: AppPrivacyScore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.PhonelinkLock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(score.appName, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(score.packageName, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            ScoreBadge(score.overallScore)
        }
    }
}

@Composable
private fun ScoreBadge(score: Int) {
    val color = when {
        score >= 80 -> RiskSafe
        score >= 50 -> RiskMedium
        score >= 30 -> RiskHigh
        else -> RiskCritical
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("$score", style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold, color = color)
    }
}

// ===== PERMISSIONS TAB =====

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PermissionsTab(viewModel: PrivacyViewModel) {
    val apps by viewModel.permissionHeatmapApps.collectAsState()
    val permissionEvents by viewModel.permissionEvents.collectAsState()
    val dangerousApps by viewModel.dangerousApps.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var filterMode by remember { mutableStateOf("all") }
    val sheetState = rememberModalBottomSheetState()

    if (selectedApp != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedApp = null },
            sheetState = sheetState
        ) {
            PermissionDetailSheet(selectedApp!!)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Filter chips
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterMode == "all",
                    onClick = { filterMode = "all" },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = filterMode == "dangerous",
                    onClick = { filterMode = "dangerous" },
                    label = { Text("Dangerous Only") }
                )
                FilterChip(
                    selected = filterMode == "recent",
                    onClick = { filterMode = "recent" },
                    label = { Text("Recently Changed") }
                )
            }
        }

        // Heatmap
        item {
            Text("Permission Heatmap", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Tap a row to see permission details",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Legend
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem("Granted", RiskCritical)
                LegendItem("Requested", RiskMedium)
                LegendItem("Not used", MaterialTheme.colorScheme.surface)
            }
        }

        // Heatmap header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.width(120.dp))
                TRACKED_PERMISSIONS.forEach { (_, shortName) ->
                    Box(
                        modifier = Modifier.width(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(shortName, style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // Heatmap rows
        val filteredApps = when (filterMode) {
            "dangerous" -> apps.filter { app ->
                app.permissions.any { perm ->
                    perm in listOf(
                        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_SMS, Manifest.permission.ACCESS_FINE_LOCATION
                    )
                }
            }
            else -> apps
        }

        items(filteredApps.take(50), key = { it.packageName }) { app ->
            PermissionHeatmapRow(app, onClick = { selectedApp = app })
        }

        // Scan button
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.refreshPermissions() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scan Permissions")
            }
        }

        // Permission Timeline
        item {
            Spacer(Modifier.height(8.dp))
            Text("Permission Timeline", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
        }

        if (permissionEvents.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Security,
                    message = "No permission changes detected yet. Tap Scan to check."
                )
            }
        } else {
            items(permissionEvents.take(20)) { event ->
                PermissionEventRow(event)
            }
        }

        // Most dangerous apps
        if (dangerousApps.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Most Dangerous Apps", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }
            items(dangerousApps.take(10)) { profile ->
                DangerousAppRow(profile)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PermissionHeatmapRow(app: AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(app.name, style = MaterialTheme.typography.bodySmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(120.dp))
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
private fun PermissionDetailSheet(app: AppInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(app.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(app.packageName, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        Text("Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        TRACKED_PERMISSIONS.forEach { (permission, shortName) ->
            val has = permission in app.permissions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (has) RiskHigh else RiskSafe)
                )
                Spacer(Modifier.width(8.dp))
                Text("$shortName — ${permission.substringAfterLast(".")}",
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text(if (has) "Granted" else "Not used",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (has) RiskHigh else RiskSafe)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PermissionEventRow(event: PermissionEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (event.isGranted) RiskHigh else RiskSafe)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    event.permission.substringAfterLast("."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (event.isGranted) "Granted" else "Revoked",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (event.isGranted) RiskHigh else RiskSafe
                )
                Text(
                    formatTimestamp(event.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DangerousAppRow(profile: AppPrivacyProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint = RiskHigh,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.appName, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${profile.grantedDangerous}/${profile.totalDangerous} dangerous permissions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ScoreBadge(profile.privacyScore)
        }
    }
}

// ===== NETWORK TAB =====

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NetworkTab(viewModel: PrivacyViewModel) {
    val topConsumers by viewModel.topDataConsumers.collectAsState()
    val totals by viewModel.networkTotals.collectAsState()
    val suspiciousApps by viewModel.suspiciousApps.collectAsState()
    val timeRange by viewModel.networkTimeRange.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Time range filter
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = timeRange == 24,
                    onClick = { viewModel.setNetworkTimeRange(24) },
                    label = { Text("24h") }
                )
                FilterChip(
                    selected = timeRange == 168,
                    onClick = { viewModel.setNetworkTimeRange(168) },
                    label = { Text("7d") }
                )
                FilterChip(
                    selected = timeRange == 720,
                    onClick = { viewModel.setNetworkTimeRange(720) },
                    label = { Text("30d") }
                )
            }
        }

        // Total usage card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Data Usage", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    totals?.let { t ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.CloudUpload, contentDescription = null,
                                    tint = BluePrimary, modifier = Modifier.size(24.dp))
                                Text(NetworkMonitor.formatBytes(t.totalTx),
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Uploaded", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.NetworkCheck, contentDescription = null,
                                    tint = CyanSecondary, modifier = Modifier.size(24.dp))
                                Text(NetworkMonitor.formatBytes(t.totalRx),
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Downloaded", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("${t.appCount} apps used network",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center)
                    } ?: run {
                        EmptyStateContent(
                            icon = Icons.Filled.NetworkCheck,
                            message = "Grant Usage Access to see network stats"
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                } catch (_: Exception) { }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Usage Access")
                        }
                    }
                }
            }
        }

        // Refresh button
        item {
            Button(
                onClick = { viewModel.refreshNetworkStats() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Refresh Network Stats")
            }
        }

        // Suspicious activity
        if (suspiciousApps.isNotEmpty()) {
            item {
                Text("Suspicious Activity", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = RiskHigh)
            }
            items(suspiciousApps) { app ->
                SuspiciousAppCard(app)
            }
        }

        // Top data consumers
        item {
            Text("Top Data Consumers", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
        }

        if (topConsumers.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.NetworkCheck,
                    message = "No network data collected yet. Tap Refresh to collect."
                )
            }
        } else {
            items(topConsumers) { consumer ->
                NetworkConsumerRow(consumer, topConsumers.firstOrNull()?.let {
                    it.totalTxBytes + it.totalRxBytes
                } ?: 1L)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SuspiciousAppCard(app: SuspiciousNetworkApp) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RiskHigh.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = RiskHigh,
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold)
                Text(app.reason, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(NetworkMonitor.formatBytes(app.dataUsed),
                    style = MaterialTheme.typography.labelSmall, color = RiskHigh)
            }
        }
    }
}

@Composable
private fun NetworkConsumerRow(consumer: NetworkUsageSummary, maxTotal: Long) {
    val total = consumer.totalTxBytes + consumer.totalRxBytes
    val progress = if (maxTotal > 0) total.toFloat() / maxTotal else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(consumer.appName, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                Text(NetworkMonitor.formatBytes(total),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .background(BluePrimary)
                )
            }
            Spacer(Modifier.height(4.dp))
            Row {
                Text("↑ ${NetworkMonitor.formatBytes(consumer.totalTxBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Text("↓ ${NetworkMonitor.formatBytes(consumer.totalRxBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ===== CLIPBOARD TAB =====

@Composable
private fun ClipboardTab(viewModel: PrivacyViewModel) {
    val status by viewModel.clipboardStatus.collectAsState()
    val events by viewModel.clipboardEvents.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Monitoring toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = null,
                        tint = if (status.isMonitoring) RiskSafe else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clipboard Monitoring",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (status.isMonitoring) "Active — monitoring clipboard changes"
                            else "Inactive — tap to start monitoring",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = status.isMonitoring,
                        onCheckedChange = { enabled ->
                            if (enabled) viewModel.startClipboardMonitoring()
                            else viewModel.stopClipboardMonitoring()
                        }
                    )
                }
            }
        }

        // Status card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${status.eventCount}",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Events", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${status.sensitiveCount}",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            color = if (status.sensitiveCount > 0) RiskHigh else MaterialTheme.colorScheme.onSurface)
                        Text("Sensitive", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Recent events
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Events", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (events.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearClipboardHistory() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (events.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.ContentPaste,
                    message = if (status.isMonitoring) "No clipboard events captured yet."
                    else "Start monitoring to capture clipboard events."
                )
            }
        } else {
            items(events) { event ->
                ClipboardEventRow(event)
            }
        }

        // Privacy notice
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BluePrimary.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null,
                        tint = BluePrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Clipboard data is stored in memory only and never saved to disk.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BluePrimary
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ClipboardEventRow(event: ClipboardEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isSensitive)
                RiskHigh.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (event.isSensitive) {
                Icon(Icons.Filled.Warning, contentDescription = "Sensitive",
                    tint = RiskHigh, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(event.contentPreview, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(formatTimestamp(event.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                event.clipType.name,
                style = MaterialTheme.typography.labelSmall,
                color = CyanSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyanSecondary.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

// ===== INSTALLS TAB =====

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InstallsTab(viewModel: PrivacyViewModel) {
    val events by viewModel.installEvents.collectAsState()
    var filterMode by remember { mutableStateOf("all") }

    val filteredEvents = when (filterMode) {
        "installs" -> events.filter { it.action == InstallAction.INSTALLED }
        "updates" -> events.filter { it.action == InstallAction.UPDATED }
        "uninstalls" -> events.filter { it.action == InstallAction.UNINSTALLED }
        "sideloaded" -> events.filter {
            it.installSource != null &&
                it.installSource != "com.android.vending" &&
                it.installSource != "com.google.android.packageinstaller"
        }
        else -> events
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Summary card
        item {
            val installs = events.count { it.action == InstallAction.INSTALLED }
            val updates = events.count { it.action == InstallAction.UPDATED }
            val uninstalls = events.count { it.action == InstallAction.UNINSTALLED }
            val sideloaded = events.count {
                it.installSource != null &&
                    it.installSource != "com.android.vending" &&
                    it.installSource != "com.google.android.packageinstaller" &&
                    it.action != InstallAction.UNINSTALLED
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Install Activity (30 days)",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$installs", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold, color = RiskSafe)
                            Text("Installs", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$updates", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold, color = BluePrimary)
                            Text("Updates", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$uninstalls", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            Text("Removed", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (sideloaded > 0) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null,
                                tint = RiskMedium, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("$sideloaded sideloaded apps",
                                style = MaterialTheme.typography.bodySmall, color = RiskMedium)
                        }
                    }
                }
            }
        }

        // Filter chips
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All", "installs" to "Installs", "updates" to "Updates",
                    "uninstalls" to "Uninstalls", "sideloaded" to "Sideloaded").forEach { (key, label) ->
                    FilterChip(
                        selected = filterMode == key,
                        onClick = { filterMode = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (filteredEvents.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.GetApp,
                    message = "No install events recorded yet."
                )
            }
        } else {
            items(filteredEvents) { event ->
                InstallEventRow(event)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun InstallEventRow(event: InstallEvent) {
    val isHighRisk = event.riskScore != null && event.riskScore > 60
    val isSideloaded = event.installSource != null &&
        event.installSource != "com.android.vending" &&
        event.installSource != "com.google.android.packageinstaller"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighRisk) RiskHigh.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action badge
            val (actionLabel, actionColor) = when (event.action) {
                InstallAction.INSTALLED -> "Installed" to RiskSafe
                InstallAction.UPDATED -> "Updated" to BluePrimary
                InstallAction.UNINSTALLED -> "Removed" to MaterialTheme.colorScheme.onSurfaceVariant
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(event.appName, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = actionColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(actionColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    if (isSideloaded) {
                        Spacer(Modifier.width(4.dp))
                        Text("Sideloaded", style = MaterialTheme.typography.labelSmall,
                            color = RiskMedium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(RiskMedium.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    event.riskScore?.let { score ->
                        Spacer(Modifier.width(4.dp))
                        ScoreBadge(100 - score) // Invert: security risk -> privacy score
                    }
                }
            }
            Text(formatTimestamp(event.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ===== SHARED COMPONENTS =====

@Composable
private fun EmptyStateCard(icon: ImageVector, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        EmptyStateContent(icon, message)
    }
}

@Composable
private fun EmptyStateContent(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
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

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
