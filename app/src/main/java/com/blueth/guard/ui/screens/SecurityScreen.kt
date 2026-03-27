package com.blueth.guard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.blueth.guard.R
import com.blueth.guard.scanner.AppScanResult
import com.blueth.guard.scanner.DeviceAdminAppInfo
import com.blueth.guard.scanner.RiskLevel
import com.blueth.guard.scanner.FileSeverity
import com.blueth.guard.ui.theme.RiskCritical
import com.blueth.guard.ui.theme.RiskHigh
import com.blueth.guard.ui.theme.RiskLow
import com.blueth.guard.ui.theme.RiskMedium
import com.blueth.guard.ui.theme.RiskSafe
import com.blueth.guard.ui.viewmodel.DeepScanState
import com.blueth.guard.ui.viewmodel.ScanState
import com.blueth.guard.ui.viewmodel.SecurityViewModel

@Composable
fun SecurityScreen(
    onNavigateToScanHistory: () -> Unit = {},
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val scanState by viewModel.scanState.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val scanResults by viewModel.scanResults.collectAsState()
    val overallScore by viewModel.overallDeviceScore.collectAsState()
    val deviceAdmins by viewModel.deviceAdmins.collectAsState()
    val deepScanState by viewModel.deepScanState.collectAsState()
    val deepScanProgress by viewModel.deepScanProgress.collectAsState()
    val deepScanResults by viewModel.deepScanResults.collectAsState()
    val deepScanStats by viewModel.deepScanStats.collectAsState()

    val isScanning = scanState == ScanState.SCANNING
    val scanComplete = scanState == ScanState.COMPLETE

    var selectedApp by remember { mutableStateOf<AppScanResult?>(null) }

    val totalApps = scanProgress?.total ?: 0
    val currentProgress = scanProgress?.current ?: 0
    val currentAppName = scanProgress?.currentApp ?: ""

    // App detail bottom sheet
    selectedApp?.let { app ->
        AppDetailBottomSheet(
            result = app,
            onDismiss = { selectedApp = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.security_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Scan button
        item {
            ScanButton(
                isScanning = isScanning,
                scanComplete = scanComplete,
                scanProgress = currentProgress,
                totalApps = totalApps,
                currentAppName = currentAppName,
                onScanClick = { viewModel.startFullScan() }
            )
        }

        // Progress bar during scan
        if (isScanning && totalApps > 0) {
            item {
                Column {
                    LinearProgressIndicator(
                        progress = { currentProgress.toFloat() / totalApps },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.security_scanning_format, currentAppName, currentProgress, totalApps),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Results
        if (scanComplete) {
            val safeApps = scanResults.filter { it.threatAssessment.riskLevel == RiskLevel.SAFE }
            val lowApps = scanResults.filter { it.threatAssessment.riskLevel == RiskLevel.LOW }
            val mediumApps = scanResults.filter { it.threatAssessment.riskLevel == RiskLevel.MEDIUM }
            val riskyApps = scanResults.filter {
                it.threatAssessment.riskLevel == RiskLevel.HIGH || it.threatAssessment.riskLevel == RiskLevel.CRITICAL
            }
            val totalTrackers = scanResults.sumOf { it.detectedTrackers.size }

            // Device score
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    DeviceScoreCard(overallScore)
                }
            }

            // Summary cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryChip(
                        modifier = Modifier.weight(1f),
                        count = safeApps.size + lowApps.size,
                        label = stringResource(R.string.security_safe),
                        color = RiskSafe
                    )
                    SummaryChip(
                        modifier = Modifier.weight(1f),
                        count = mediumApps.size,
                        label = stringResource(R.string.security_attention),
                        color = RiskMedium
                    )
                    SummaryChip(
                        modifier = Modifier.weight(1f),
                        count = riskyApps.size,
                        label = stringResource(R.string.security_risky),
                        color = RiskCritical
                    )
                }
            }

            // Tracker count
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.TrackChanges,
                            contentDescription = "Trackers",
                            tint = RiskMedium,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.security_trackers_format, totalTrackers),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Device admin warning
            val nonSystemAdmins = deviceAdmins.filter { admin ->
                !admin.description.contains("System")
            }
            if (nonSystemAdmins.isNotEmpty()) {
                item {
                    DeviceAdminWarningCard(nonSystemAdmins)
                }
            }

            // All apps sorted by risk
            val sortedResults = scanResults.sortedByDescending { it.threatAssessment.overallScore }

            if (sortedResults.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.security_all_scanned_apps),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(sortedResults, key = { it.appInfo.packageName }) { result ->
                    ScanResultCard(result, onClick = { selectedApp = result }, onLongClick = { selectedApp = result })
                }
            }
            // View Scan History button
            item {
                OutlinedButton(
                    onClick = onNavigateToScanHistory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.security_view_scan_history))
                }
            }
        }

        // Deep File Scan section
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Deep File Scanner",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Scan storage for malware, suspicious files, and corruption",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            val isDeepScanning = deepScanState == DeepScanState.SCANNING
            Button(
                onClick = { viewModel.startDeepScan() },
                enabled = !isDeepScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isDeepScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    val prog = deepScanProgress
                    Text(if (prog != null) "Scanning ${prog.scannedFiles}/${prog.totalFiles}..." else "Scanning...")
                } else {
                    Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (deepScanState == DeepScanState.COMPLETE) "Re-scan Storage" else "Scan Storage")
                }
            }
        }

        // Deep scan results
        if (deepScanState == DeepScanState.COMPLETE) {
            deepScanStats?.let { stats ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Scan Complete", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("${stats.filesScanned} files scanned in ${stats.scanDurationMs / 1000}s",
                                style = MaterialTheme.typography.bodySmall)
                            Text("${stats.threatsFound} threats, ${stats.corruptedFound} corrupted files found",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (stats.threatsFound > 0) RiskCritical else RiskSafe)
                        }
                    }
                }
            }

            if (deepScanResults.isNotEmpty()) {
                items(deepScanResults) { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sevColor = when (result.severity) {
                                FileSeverity.CRITICAL -> RiskCritical
                                FileSeverity.HIGH -> RiskHigh
                                FileSeverity.MEDIUM -> RiskMedium
                                FileSeverity.LOW -> RiskLow
                                else -> RiskSafe
                            }
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = sevColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    result.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    result.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ScanButton(
    isScanning: Boolean,
    scanComplete: Boolean,
    scanProgress: Int,
    totalApps: Int,
    currentAppName: String,
    onScanClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(isScanning, scanProgress, totalApps) {
        if (isScanning && totalApps > 0) {
            progressAnim.animateTo(
                scanProgress.toFloat() / totalApps,
                animationSpec = tween(100)
            )
        } else if (!isScanning) {
            progressAnim.snapTo(0f)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            if (isScanning) {
                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .rotate(rotation)
                ) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, Color(0xFF2196F3))
                        ),
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Canvas(modifier = Modifier.size(180.dp)) {
                drawArc(
                    color = Color(0xFF1E2438),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx())
                )
                if (isScanning || scanComplete) {
                    val sweep = if (scanComplete) 360f else progressAnim.value * 360f
                    val progressColor = if (scanComplete) Color(0xFF4CAF50) else Color(0xFF2196F3)
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Button(
                onClick = onScanClick,
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primary
                ),
                enabled = !isScanning
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = "Start security scan",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isScanning) stringResource(R.string.security_scanning) else if (scanComplete) stringResource(R.string.security_rescan) else stringResource(R.string.security_scan),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceScoreCard(score: Int) {
    val scoreColor = when {
        score <= 20 -> RiskSafe
        score <= 40 -> RiskLow
        score <= 60 -> RiskMedium
        score <= 80 -> RiskHigh
        else -> RiskCritical
    }
    // Invert for display: lower threat score = higher device security
    val securityScore = (100 - score).coerceIn(0, 100)
    val displayColor = when {
        securityScore >= 80 -> RiskSafe
        securityScore >= 60 -> RiskLow
        securityScore >= 40 -> RiskMedium
        securityScore >= 20 -> RiskHigh
        else -> RiskCritical
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.security_device_score),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "$securityScore",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = displayColor,
                fontSize = 64.sp
            )
            Text(
                when {
                    securityScore >= 80 -> stringResource(R.string.security_well_protected)
                    securityScore >= 60 -> stringResource(R.string.security_mostly_secure)
                    securityScore >= 40 -> stringResource(R.string.security_some_attention)
                    else -> stringResource(R.string.security_issues_detected)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryChip(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.titleLarge,
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
}

@Composable
private fun DeviceAdminWarningCard(admins: List<DeviceAdminAppInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RiskHigh.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.AdminPanelSettings,
                contentDescription = "Device admin warning",
                tint = RiskHigh,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.security_device_admin_warning),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = RiskHigh
                )
                admins.forEach { admin ->
                    Text(
                        stringResource(R.string.security_admin_privileges_format, admin.appName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScanResultCard(result: AppScanResult, onClick: () -> Unit = {}, onLongClick: () -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    val riskColor = when (result.threatAssessment.riskLevel) {
        RiskLevel.CRITICAL -> RiskCritical
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> RiskMedium
        RiskLevel.LOW -> RiskLow
        RiskLevel.SAFE -> RiskSafe
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Risk badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(riskColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        result.threatAssessment.overallScore.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = riskColor
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        result.appInfo.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row {
                        Text(
                            result.threatAssessment.riskLevel.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = riskColor
                        )
                        if (result.detectedTrackers.isNotEmpty()) {
                            Text(
                                " · ${result.detectedTrackers.size} trackers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded details
            if (expanded) {
                Spacer(Modifier.height(12.dp))

                // Threat reasons
                if (result.threatAssessment.reasons.isNotEmpty()) {
                    Text(
                        stringResource(R.string.security_threat_analysis),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    result.threatAssessment.reasons.forEach { reason ->
                        Text(
                            "• ${reason.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Trackers
                if (result.detectedTrackers.isNotEmpty()) {
                    Text(
                        stringResource(R.string.security_trackers_count_format, result.detectedTrackers.size),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    result.detectedTrackers.forEach { tracker ->
                        Text(
                            "• ${tracker.signature.name} (${tracker.signature.category.label})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Permission audit summary
                Text(
                    stringResource(R.string.security_permissions),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    result.permissionAudit.plainEnglishSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Install source
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.security_source_format, result.installSource.displayName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
