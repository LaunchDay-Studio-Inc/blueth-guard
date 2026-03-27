package com.blueth.guard.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.blueth.guard.R
import com.blueth.guard.battery.AppDrainEstimate
import com.blueth.guard.battery.BatteryAlertEngine
import com.blueth.guard.battery.DrainCategory
import com.blueth.guard.battery.DrainImpact
import com.blueth.guard.battery.RunningServiceInfo
import com.blueth.guard.battery.WakelockInfo
import com.blueth.guard.battery.WakelockSeverity
import com.blueth.guard.battery.BatteryHistoryReport
import com.blueth.guard.battery.PowerProfile
import com.blueth.guard.battery.PowerProfileConfig
import com.blueth.guard.ui.theme.BluePrimary
import com.blueth.guard.ui.theme.CyanSecondary
import com.blueth.guard.ui.theme.RiskCritical
import com.blueth.guard.ui.theme.RiskHigh
import com.blueth.guard.ui.theme.RiskMedium
import com.blueth.guard.ui.theme.RiskSafe
import com.blueth.guard.ui.viewmodel.BatteryTab
import com.blueth.guard.ui.viewmodel.BatteryViewModel

@Composable
fun BatteryScreen(
    viewModel: BatteryViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val batteryHealth by viewModel.batteryHealth.collectAsState()
    val wakelocks by viewModel.wakelocks.collectAsState()
    val services by viewModel.services.collectAsState()
    val drainEstimates by viewModel.drainEstimates.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val lastRefresh by viewModel.lastRefreshTimestamp.collectAsState()
    val batteryAlerts by viewModel.batteryAlerts.collectAsState()
    val batteryHistory by viewModel.batteryHistory.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val profileConfigs by viewModel.profileConfigs.collectAsState()
    val historyReport by viewModel.historyReport.collectAsState()
    val chargeGuardActive by viewModel.chargeGuardActive.collectAsState()

    val tabs = BatteryTab.entries
    val tabTitles = listOf(
        stringResource(R.string.battery_tab_overview),
        stringResource(R.string.battery_tab_wakelocks),
        stringResource(R.string.battery_tab_services),
        stringResource(R.string.battery_tab_drain),
        "Profiles"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Battery",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        }

        // Tab row
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(selectedTab),
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            contentColor = BluePrimary
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = { Text(tabTitles[index]) }
                )
            }
        }

        // Loading state
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BluePrimary)
            }
        } else {
            when (selectedTab) {
                BatteryTab.OVERVIEW -> OverviewTab(batteryHealth, lastRefresh, batteryAlerts, batteryHistory)
                BatteryTab.WAKELOCKS -> WakelocksTab(wakelocks)
                BatteryTab.SERVICES -> ServicesTab(services)
                BatteryTab.DRAIN -> DrainTab(drainEstimates)
                BatteryTab.PROFILES -> ProfilesTab(
                    profileConfigs, activeProfile, chargeGuardActive,
                    historyReport,
                    onActivateProfile = { viewModel.activateProfile(it) },
                    onToggleChargeGuard = { viewModel.toggleChargeGuard(it) }
                )
            }
        }
    }
}

// ── OVERVIEW TAB ──────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(
    health: com.blueth.guard.battery.BatteryHealth?,
    lastRefreshTimestamp: Long,
    batteryAlerts: List<BatteryAlertEngine.BatteryAlert>,
    batteryHistory: List<com.blueth.guard.data.local.BatterySnapshot>
) {
    if (health == null) return

    val batteryColor = when {
        health.levelPercent <= 15 -> RiskCritical
        health.levelPercent <= 30 -> RiskMedium
        else -> RiskSafe
    }

    val animatedLevel by animateFloatAsState(
        targetValue = health.levelPercent / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "battery_level"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Battery Alerts
        if (batteryAlerts.isNotEmpty()) {
            item {
                BatteryAlertsSection(alerts = batteryAlerts)
            }
        }

        // Battery level circle
        item {
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
                            progress = { animatedLevel },
                            modifier = Modifier.size(120.dp),
                            color = batteryColor,
                            trackColor = MaterialTheme.colorScheme.surface,
                            strokeWidth = 10.dp,
                        )
                        Text(
                            "${health.levelPercent}%",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = batteryColor
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Health score badge
                    val scoreColor = when {
                        health.healthScore >= 80 -> RiskSafe
                        health.healthScore >= 50 -> RiskMedium
                        else -> RiskCritical
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(scoreColor.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Health: ${health.healthLabel} (${health.healthScore}/100)",
                            style = MaterialTheme.typography.labelMedium,
                            color = scoreColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        health.chargeSource,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    health.healthDisclaimer?.let { disclaimer ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            disclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2x2 detail grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BatteryDetailCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Favorite,
                    label = "Health",
                    value = health.health,
                    color = if (health.health == "Good") RiskSafe else RiskMedium
                )
                BatteryDetailCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.DeviceThermostat,
                    label = "Temperature",
                    value = "${health.temperature}°C",
                    color = if (health.temperature < 40) CyanSecondary else RiskCritical
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BatteryDetailCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.FlashOn,
                    label = "Voltage",
                    value = "${health.voltage}V",
                    color = BluePrimary
                )
                BatteryDetailCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.BatteryChargingFull,
                    label = "Technology",
                    value = health.technology,
                    color = CyanSecondary
                )
            }
        }

        // Battery History Graph
        if (batteryHistory.size >= 2) {
            item {
                Text(
                    "Battery History (24h)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                BatteryHistoryChart(
                    history = batteryHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        // Recommendations
        if (health.recommendations.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.battery_recommendation_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(health.recommendations) { rec ->
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
                                .background(RiskMedium)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            rec,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Last refreshed
        if (lastRefreshTimestamp > 0) {
            item {
                Text(
                    stringResource(R.string.battery_last_refreshed) + " " +
                            DateUtils.getRelativeTimeSpanString(lastRefreshTimestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── BATTERY HISTORY CHART ─────────────────────────────────────────────────────

@Composable
private fun BatteryHistoryChart(
    history: List<com.blueth.guard.data.local.BatterySnapshot>,
    modifier: Modifier = Modifier
) {
    val lineColor = BluePrimary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val chargingColor = RiskSafe.copy(alpha = 0.15f)

    val sorted = remember(history) { history.sortedBy { it.timestamp } }
    val minTime = sorted.first().timestamp
    val maxTime = sorted.last().timestamp
    val timeRange = (maxTime - minTime).coerceAtLeast(1L)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 36.dp, end = 16.dp, top = 16.dp, bottom = 28.dp)
        ) {
            val w = size.width
            val h = size.height

            // Horizontal grid lines at 0%, 25%, 50%, 75%, 100%
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            for (i in 0..4) {
                val y = h - (i / 4f) * h
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    pathEffect = dashEffect,
                    strokeWidth = 1f
                )
            }

            // Charging segments background
            for (i in 0 until sorted.size - 1) {
                if (sorted[i].isCharging) {
                    val x1 = ((sorted[i].timestamp - minTime).toFloat() / timeRange) * w
                    val x2 = ((sorted[i + 1].timestamp - minTime).toFloat() / timeRange) * w
                    drawRect(
                        color = chargingColor,
                        topLeft = Offset(x1, 0f),
                        size = androidx.compose.ui.geometry.Size(x2 - x1, h)
                    )
                }
            }

            // Battery level line path
            val path = Path()
            sorted.forEachIndexed { index, snapshot ->
                val x = ((snapshot.timestamp - minTime).toFloat() / timeRange) * w
                val y = h - (snapshot.levelPercent / 100f) * h
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(
                    width = 3f,
                    cap = StrokeCap.Round
                )
            )

            // Data point dots
            sorted.forEach { snapshot ->
                val x = ((snapshot.timestamp - minTime).toFloat() / timeRange) * w
                val y = h - (snapshot.levelPercent / 100f) * h
                drawCircle(
                    color = lineColor,
                    radius = 4f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

// ── WAKELOCKS TAB ─────────────────────────────────────────────────────────────

@Composable
private fun WakelocksTab(wakelocks: List<WakelockInfo>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text(
                stringResource(R.string.battery_wakelock_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Analysis period: last 24 hours",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (wakelocks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        stringResource(R.string.battery_wakelock_empty),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(wakelocks, key = { it.packageName }) { wl ->
            WakelockItem(wl)
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun WakelockItem(wl: WakelockInfo) {
    val context = LocalContext.current
    val severityColor = when (wl.severity) {
        WakelockSeverity.LOW -> RiskSafe
        WakelockSeverity.MEDIUM -> RiskMedium
        WakelockSeverity.HIGH -> RiskHigh
        WakelockSeverity.CRITICAL -> RiskCritical
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        wl.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        wl.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SeverityBadge(wl.severity.name, severityColor)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Screen: ${formatDuration(wl.foregroundTimeMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Wakes: ${wl.wakeCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (wl.lastWakeTimestamp > 0) {
                    Text(
                        DateUtils.getRelativeTimeSpanString(wl.lastWakeTimestamp).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (wl.severity == WakelockSeverity.HIGH || wl.severity == WakelockSeverity.CRITICAL) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        ).apply {
                            data = android.net.Uri.fromParts("package", wl.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restrict Background", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ── SERVICES TAB ──────────────────────────────────────────────────────────────

@Composable
private fun ServicesTab(services: List<RunningServiceInfo>) {
    val userServices = services.filter { !it.isSystem }
    val systemServices = services.filter { it.isSystem }
    var showSystem by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text(
                stringResource(R.string.battery_services_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${services.size} services running",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (services.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        stringResource(R.string.battery_services_empty),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // User apps section
        if (userServices.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.battery_services_user),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            }
            items(userServices, key = { it.serviceClassName }) { svc ->
                ServiceItem(svc)
            }
        }

        // System section (collapsible)
        if (systemServices.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.battery_services_system) + " (${systemServices.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { showSystem = !showSystem }
                )
            }
            if (showSystem) {
                items(systemServices, key = { it.serviceClassName }) { svc ->
                    ServiceItem(svc)
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ServiceItem(svc: RunningServiceInfo) {
    val impactColor = when (svc.drainImpact) {
        DrainImpact.LOW -> RiskSafe
        DrainImpact.MEDIUM -> RiskMedium
        DrainImpact.HIGH -> RiskCritical
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Impact dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(impactColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        svc.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (svc.isForeground) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyanSecondary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "FG",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    svc.serviceClassName.substringAfterLast('.'),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Running: ${formatDuration(svc.runningDurationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── DRAIN TAB ─────────────────────────────────────────────────────────────────

@Composable
private fun DrainTab(drainEstimates: List<AppDrainEstimate>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text(
                stringResource(R.string.battery_drain_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Based on usage, network, and permissions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (drainEstimates.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        stringResource(R.string.battery_drain_empty),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(drainEstimates, key = { it.packageName }) { est ->
            DrainItem(est)
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun DrainItem(est: AppDrainEstimate) {
    val categoryColor = when (est.drainCategory) {
        DrainCategory.MINIMAL -> RiskSafe
        DrainCategory.LOW -> RiskSafe
        DrainCategory.MODERATE -> RiskMedium
        DrainCategory.HIGH -> RiskHigh
        DrainCategory.EXTREME -> RiskCritical
    }

    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        est.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SeverityBadge(est.drainCategory.name, categoryColor)
            }
            Spacer(Modifier.height(8.dp))

            // Drain score bar
            LinearProgressIndicator(
                progress = { est.drainScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = categoryColor,
                trackColor = MaterialTheme.colorScheme.surface,
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "~${"%.0f".format(est.estimatedMahPerDay)} mAh/day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "Screen: ${formatDuration(est.foregroundTimeMs)} | " +
                        "Network: ${android.text.format.Formatter.formatFileSize(context, est.networkBytesTotal)} | " +
                        "Perms: ${est.dangerousPermCount} risky",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (est.drainCategory == DrainCategory.HIGH || est.drainCategory == DrainCategory.EXTREME) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        ).apply {
                            data = android.net.Uri.fromParts("package", est.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restrict Background", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ── SHARED COMPONENTS ─────────────────────────────────────────────────────────

@Composable
private fun SeverityBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun BatteryDetailCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms < 0 || ms > 2_592_000_000L) return "Since boot" // > 30 days
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}

@Composable
private fun BatteryAlertsSection(alerts: List<BatteryAlertEngine.BatteryAlert>) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        alerts.forEach { alert ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (alert.severity) {
                        BatteryAlertEngine.Severity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                        BatteryAlertEngine.Severity.WARNING -> RiskMedium.copy(alpha = 0.15f)
                        BatteryAlertEngine.Severity.INFO -> MaterialTheme.colorScheme.primaryContainer
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (alert.severity) {
                                BatteryAlertEngine.Severity.CRITICAL -> Icons.Filled.Warning
                                BatteryAlertEngine.Severity.WARNING -> Icons.Filled.BatteryAlert
                                BatteryAlertEngine.Severity.INFO -> Icons.Filled.Info
                            },
                            contentDescription = null,
                            tint = when (alert.severity) {
                                BatteryAlertEngine.Severity.CRITICAL -> RiskCritical
                                BatteryAlertEngine.Severity.WARNING -> RiskMedium
                                BatteryAlertEngine.Severity.INFO -> BluePrimary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            alert.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(alert.description, style = MaterialTheme.typography.bodySmall)
                    Text(
                        alert.suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    alert.actionLabel?.let { label ->
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { alert.actionIntent?.let { context.startActivity(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}

// ── PROFILES TAB ──────────────────────────────────────────────────────────────

@Composable
private fun ProfilesTab(
    profileConfigs: List<PowerProfileConfig>,
    activeProfile: PowerProfile,
    chargeGuardActive: Boolean,
    historyReport: BatteryHistoryReport?,
    onActivateProfile: (PowerProfile) -> Unit,
    onToggleChargeGuard: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Power Profiles
        item {
            Text("Power Profiles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(profileConfigs.size) { index ->
            val config = profileConfigs[index]
            val isActive = config.profile == activeProfile
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActivateProfile(config.profile) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            config.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isActive) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        config.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Charge Guard
        item {
            Spacer(Modifier.height(8.dp))
            Text("Charge Guard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alert at 80%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                "Get notified to unplug when battery reaches 80% to preserve battery longevity",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = chargeGuardActive,
                            onCheckedChange = onToggleChargeGuard
                        )
                    }
                }
            }
        }

        // Battery History Report
        historyReport?.let { report ->
            item {
                Spacer(Modifier.height(8.dp))
                Text("Battery History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (report.hasEnoughData) {
                            Text("Est. charge cycles: ${"%.1f".format(report.estimatedChargeCycles)}",
                                style = MaterialTheme.typography.bodyMedium)
                            Text("Avg temperature: ${"%.1f".format(report.averageTemperature)}°C",
                                style = MaterialTheme.typography.bodyMedium)
                            Text("Max temperature: ${"%.1f".format(report.maxTemperature)}°C",
                                style = MaterialTheme.typography.bodyMedium)
                            Text("Overnight charges: ${report.overnightChargingCount}",
                                style = MaterialTheme.typography.bodyMedium)
                            report.degradationTrend?.let { trend ->
                                Text(
                                    "Degradation trend: ${trend.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when (trend) {
                                        com.blueth.guard.battery.DegradationTrend.STABLE -> RiskSafe
                                        com.blueth.guard.battery.DegradationTrend.SLIGHT_DECLINE -> RiskMedium
                                        com.blueth.guard.battery.DegradationTrend.DECLINING -> RiskHigh
                                        com.blueth.guard.battery.DegradationTrend.RAPID_DECLINE -> RiskCritical
                                    }
                                )
                            }
                        } else {
                            Text(
                                "Not enough data yet — keep using the app to build battery history",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        report.disclaimer?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}
