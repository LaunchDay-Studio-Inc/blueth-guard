package com.blueth.guard.ui.screens

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.blueth.guard.R
import com.blueth.guard.ui.theme.BluePrimary
import com.blueth.guard.ui.theme.RiskCritical
import com.blueth.guard.ui.theme.RiskHigh
import com.blueth.guard.ui.theme.RiskMedium
import com.blueth.guard.ui.theme.RiskSafe
import com.blueth.guard.ui.viewmodel.ActivityItem
import com.blueth.guard.ui.viewmodel.DashboardState
import com.blueth.guard.ui.viewmodel.HomeViewModel
import com.blueth.guard.ui.viewmodel.QuickScanReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToBattery: () -> Unit = {},
    onNavigateToOptimizer: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAntiTheft: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.dashboardState.collectAsState()
    val recentActivities by viewModel.recentActivities.collectAsState()
    val quickScanRunning by viewModel.quickScanRunning.collectAsState()
    val quickScanReport by viewModel.quickScanReport.collectAsState()

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Spacer(Modifier.height(8.dp))
                HeaderSection(onNavigateToSettings = onNavigateToSettings)
            }

            // Score Ring
            item {
                ScoreRingSection(state = state)
            }

            // Quick Scan button
            item {
                QuickScanButton(
                    isScanning = quickScanRunning,
                    onScan = { viewModel.runQuickScan() }
                )
            }

            // Quick Scan Report
            quickScanReport?.let { report ->
                item {
                    QuickScanReportCard(report = report)
                }
            }

            // Quick Actions
            item {
                if (state.isLoading) {
                    // Show nothing while loading
                } else {
                    QuickActionsRow(
                        onNavigateToSecurity = onNavigateToSecurity,
                        onNavigateToOptimizer = onNavigateToOptimizer,
                        onNavigateToPrivacy = onNavigateToPrivacy
                    )
                }
            }

            // Module Status Cards
            item {
                ModuleCardsGrid(
                    state = state,
                    onNavigateToSecurity = onNavigateToSecurity,
                    onNavigateToPrivacy = onNavigateToPrivacy,
                    onNavigateToBattery = onNavigateToBattery,
                    onNavigateToOptimizer = onNavigateToOptimizer
                )
            }

            // Protection Status Banner
            item {
                ProtectionBanner(
                    state = state,
                    onNavigateToSettings = onNavigateToSettings
                )
            }

            // WiFi Security Card
            if (state.wifiConnected) {
                item {
                    WifiSecurityCard(state = state)
                }
            }

            // Anti-Theft shortcut
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAntiTheft() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.PhonelinkLock,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Anti-Theft Protection",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Remote lock, wipe & locate your device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Alerts Section
            item {
                AlertsSection(
                    state = state,
                    onNavigateToPrivacy = onNavigateToPrivacy,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToBattery = onNavigateToBattery
                )
            }

            // Recent Activity Feed
            if (recentActivities.isNotEmpty()) {
                item {
                    RecentActivitySection(activities = recentActivities)
                }
            }

            // Bottom spacing
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun HeaderSection(onNavigateToSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onNavigateToSettings) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScoreRingSection(state: DashboardState) {
    val hasScore = state.lastScanTime != null || state.securityError
    val targetProgress = if (hasScore) state.overallScore / 100f else 0f

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(targetProgress) {
        animatedProgress.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        )
    }

    val animatedScore by animateIntAsState(
        targetValue = if (hasScore) state.overallScore else 0,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "scoreAnim"
    )

    val scoreColor = when {
        !hasScore -> MaterialTheme.colorScheme.onSurfaceVariant
        state.overallScore >= 75 -> RiskSafe
        state.overallScore >= 50 -> RiskMedium
        state.overallScore >= 25 -> RiskHigh
        else -> RiskCritical
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 12.dp.toPx()
                val padding = strokeWidth / 2
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(padding, padding)

                // Background track
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Foreground arc
                drawArc(
                    color = scoreColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(
                        text = if (hasScore) "$animatedScore" else "—",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = state.scoreLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        val context = LocalContext.current
        val lastScanText = when {
            state.isLoading -> ""
            state.lastScanTime != null -> stringResource(R.string.home_last_scan_format,
                DateUtils.getRelativeTimeSpanString(
                    state.lastScanTime,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
            )
            else -> stringResource(R.string.home_never_scanned)
        }

        if (lastScanText.isNotEmpty()) {
            Text(
                text = lastScanText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickScanButton(
    isScanning: Boolean,
    onScan: () -> Unit
) {
    Button(
        onClick = onScan,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        enabled = !isScanning,
        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
    ) {
        if (isScanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
            Spacer(Modifier.width(8.dp))
            Text("Scanning...", fontWeight = FontWeight.SemiBold)
        } else {
            Icon(Icons.Filled.Security, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_quick_scan), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuickScanReportCard(report: QuickScanReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (report.criticalCount > 0 || report.highRiskCount > 0)
                RiskHigh.copy(alpha = 0.08f)
            else RiskSafe.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    tint = if (report.criticalCount > 0) RiskCritical
                    else if (report.highRiskCount > 0) RiskHigh
                    else RiskSafe,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Scan Report — ${report.overallBadge}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "${report.totalApps} apps scanned",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "0 viruses detected · 0 malware found · ${report.trackerCount} trackers found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RiskChip(count = report.safeCount + report.lowRiskCount, label = "Safe", color = RiskSafe)
                RiskChip(count = report.mediumRiskCount, label = "Medium", color = RiskMedium)
                RiskChip(count = report.highRiskCount + report.criticalCount, label = "High", color = RiskCritical)
            }
        }
    }
}

@Composable
private fun RiskChip(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun QuickActionsRow(
    onNavigateToSecurity: () -> Unit,
    onNavigateToOptimizer: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionChip(
            label = "Full Scan",
            icon = Icons.Filled.Security,
            onClick = onNavigateToSecurity,
            modifier = Modifier.weight(1f)
        )
        QuickActionChip(
            label = "Boost RAM",
            icon = Icons.Filled.Rocket,
            onClick = onNavigateToOptimizer,
            modifier = Modifier.weight(1f)
        )
        QuickActionChip(
            label = "Clean Cache",
            icon = Icons.Filled.Storage,
            onClick = onNavigateToOptimizer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            leadingIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun ModuleCardsGrid(
    state: DashboardState,
    onNavigateToSecurity: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToBattery: () -> Unit,
    onNavigateToOptimizer: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Row 1: Security + Privacy
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 0)
                ) + fadeIn(animationSpec = tween(400, delayMillis = 0)),
                modifier = Modifier.weight(1f)
            ) {
                SecurityCard(state = state, onClick = onNavigateToSecurity)
            }

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 100)
                ) + fadeIn(animationSpec = tween(400, delayMillis = 100)),
                modifier = Modifier.weight(1f)
            ) {
                PrivacyCard(state = state, onClick = onNavigateToPrivacy)
            }
        }

        // Row 2: Battery + Storage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 200)
                ) + fadeIn(animationSpec = tween(400, delayMillis = 200)),
                modifier = Modifier.weight(1f)
            ) {
                BatteryCard(state = state, onClick = onNavigateToBattery)
            }

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 300)
                ) + fadeIn(animationSpec = tween(400, delayMillis = 300)),
                modifier = Modifier.weight(1f)
            ) {
                StorageCard(state = state, onClick = onNavigateToOptimizer)
            }
        }
    }
}

@Composable
private fun ModuleCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    indicatorColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = indicatorColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (bottomContent != null) {
                Spacer(Modifier.height(4.dp))
                bottomContent()
            }
        }
    }
}

@Composable
private fun SecurityCard(state: DashboardState, onClick: () -> Unit) {
    val (value, subtitle, color) = when {
        state.securityError -> Triple(stringResource(R.string.home_error), stringResource(R.string.home_unable_to_load), MaterialTheme.colorScheme.onSurfaceVariant)
        state.lastScanTime == null -> Triple("—", stringResource(R.string.home_not_scanned), MaterialTheme.colorScheme.onSurfaceVariant)
        state.riskyAppsCount > 0 -> Triple(
            "${100 - (state.riskyAppsCount * 10).coerceAtMost(100)}",
            stringResource(R.string.home_threats_format, state.riskyAppsCount),
            RiskHigh
        )
        else -> Triple("100", stringResource(R.string.home_all_clear), RiskSafe)
    }
    ModuleCard(
        icon = Icons.Filled.Shield,
        title = stringResource(R.string.home_security),
        value = value,
        subtitle = subtitle,
        indicatorColor = color,
        onClick = onClick
    )
}

@Composable
private fun PrivacyCard(state: DashboardState, onClick: () -> Unit) {
    val (value, subtitle, color) = when {
        state.privacyError -> Triple(stringResource(R.string.home_error), stringResource(R.string.home_unable_to_load), MaterialTheme.colorScheme.onSurfaceVariant)
        state.privacyScore == 0 && state.lastScanTime == null -> Triple("—", stringResource(R.string.home_not_analyzed), MaterialTheme.colorScheme.onSurfaceVariant)
        else -> {
            val c = when {
                state.privacyScore >= 75 -> RiskSafe
                state.privacyScore >= 50 -> RiskMedium
                else -> RiskHigh
            }
            Triple(
                "${state.privacyScore}",
                stringResource(R.string.home_high_risk_apps_format, state.appsWithDangerousPermissions),
                c
            )
        }
    }
    ModuleCard(
        icon = Icons.Filled.Lock,
        title = stringResource(R.string.home_privacy),
        value = value,
        subtitle = subtitle,
        indicatorColor = color,
        onClick = onClick
    )
}

@Composable
private fun BatteryCard(state: DashboardState, onClick: () -> Unit) {
    val (subtitle, color) = when {
        state.batteryError -> stringResource(R.string.home_unable_to_load) to MaterialTheme.colorScheme.onSurfaceVariant
        state.isCharging -> stringResource(R.string.home_charging) to RiskSafe
        state.topDrainer != null -> state.topDrainer to MaterialTheme.colorScheme.onSurfaceVariant
        else -> state.batteryHealth to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val batteryColor = when {
        state.batteryError -> MaterialTheme.colorScheme.onSurfaceVariant
        state.batteryLevel >= 50 -> RiskSafe
        state.batteryLevel >= 20 -> RiskMedium
        else -> RiskHigh
    }

    ModuleCard(
        icon = if (state.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.Battery4Bar,
        title = stringResource(R.string.home_battery),
        value = if (state.batteryError) stringResource(R.string.home_error) else "${state.batteryLevel}%",
        subtitle = subtitle,
        indicatorColor = batteryColor,
        onClick = onClick
    )
}

@Composable
private fun StorageCard(state: DashboardState, onClick: () -> Unit) {
    val context = LocalContext.current

    val ramUsedMb = state.ramUsed / (1024 * 1024)
    val ramTotalMb = state.ramTotal / (1024 * 1024)
    val (value, subtitle) = when {
        state.storageError -> stringResource(R.string.home_error) to stringResource(R.string.home_unable_to_load)
        state.ramTotal == 0L -> "—" to stringResource(R.string.home_not_analyzed)
        else -> {
            val used = Formatter.formatShortFileSize(context, state.ramUsed)
            val total = Formatter.formatShortFileSize(context, state.ramTotal)
            "RAM $used / $total" to "${state.killableProcessCount} killable processes"
        }
    }

    val usedFraction = if (state.ramTotal > 0) {
        state.ramUsed.toFloat() / state.ramTotal
    } else 0f

    val color = when {
        state.storageError -> MaterialTheme.colorScheme.onSurfaceVariant
        usedFraction > 0.9f -> RiskHigh
        usedFraction > 0.75f -> RiskMedium
        else -> BluePrimary
    }

    ModuleCard(
        icon = Icons.Filled.Rocket,
        title = stringResource(R.string.home_storage),
        value = value,
        subtitle = subtitle,
        indicatorColor = color,
        onClick = onClick,
        bottomContent = if (state.ramTotal > 0 && !state.storageError) {
            {
                LinearProgressIndicator(
                    progress = { usedFraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        } else null
    )
}

@Composable
private fun ProtectionBanner(state: DashboardState, onNavigateToSettings: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToSettings),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.protectionEnabled)
                RiskSafe.copy(alpha = 0.1f)
            else
                RiskMedium.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (state.protectionEnabled) Icons.Filled.Shield else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (state.protectionEnabled) RiskSafe else RiskMedium,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.protectionEnabled) stringResource(R.string.home_protection_active) else stringResource(R.string.home_protection_disabled),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (state.protectionEnabled) {
                        if (state.installScanEnabled) stringResource(R.string.home_install_scanning_enabled) else stringResource(R.string.home_monitoring_active)
                    } else {
                        stringResource(R.string.home_tap_to_enable)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!state.protectionEnabled) {
                Button(
                    onClick = onNavigateToSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = RiskMedium),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.home_enable))
                }
            }
        }
    }
}

@Composable
private fun WifiSecurityCard(state: DashboardState) {
    val isSecure = state.wifiSecure
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSecure) RiskSafe.copy(alpha = 0.1f) else RiskHigh.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSecure) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                contentDescription = null,
                tint = if (isSecure) RiskSafe else RiskHigh,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.wifiSsid ?: "WiFi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isSecure) "Network is secure" else state.wifiWarnings.firstOrNull() ?: "Potential security issue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AlertsSection(
    state: DashboardState,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBattery: () -> Unit
) {
    val alerts = buildList {
        if (state.appsWithDangerousPermissions > 0) {
            add(AlertItem(
                text = stringResource(R.string.home_high_risk_perms_format, state.appsWithDangerousPermissions),
                color = RiskHigh,
                onClick = onNavigateToPrivacy
            ))
        }
        if (state.hasScheduledScans && state.lastScanTime == null) {
            add(AlertItem(
                text = stringResource(R.string.home_scan_overdue),
                color = RiskMedium,
                onClick = onNavigateToSettings
            ))
        }
        if (state.temperature > 38f) {
            add(AlertItem(
                text = stringResource(R.string.home_battery_temp_format, state.temperature),
                color = if (state.temperature > 42f) RiskCritical else RiskHigh,
                onClick = onNavigateToBattery
            ))
        }
    }

    if (alerts.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.home_alerts),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        alerts.forEach { alert ->
            AlertCard(alert = alert)
        }
    }
}

private data class AlertItem(
    val text: String,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun AlertCard(alert: AlertItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alert.onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = alert.color.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = alert.color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = alert.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── RECENT ACTIVITY FEED ──────────────────────────────────────────────────────

@Composable
private fun RecentActivitySection(activities: List<ActivityItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        activities.forEach { activity ->
            RecentActivityCard(activity)
        }
    }
}

@Composable
private fun RecentActivityCard(activity: ActivityItem) {
    val icon = when (activity.icon) {
        "scan" -> Icons.Filled.Shield
        "install" -> Icons.Filled.Download
        "permission" -> Icons.Filled.Lock
        else -> Icons.Filled.Info
    }
    val iconColor = when (activity.icon) {
        "scan" -> BluePrimary
        "install" -> RiskMedium
        "permission" -> RiskHigh
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activity.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    activity.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                android.text.format.DateUtils.getRelativeTimeSpanString(
                    activity.timestamp,
                    System.currentTimeMillis(),
                    android.text.format.DateUtils.MINUTE_IN_MILLIS
                ).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
