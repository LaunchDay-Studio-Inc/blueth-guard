package com.blueth.guard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.blueth.guard.data.model.AppInfo
import com.blueth.guard.scanner.PermissionRiskScorer
import com.blueth.guard.ui.theme.RiskCritical
import com.blueth.guard.ui.theme.RiskHigh
import com.blueth.guard.ui.theme.RiskLow
import com.blueth.guard.ui.theme.RiskMedium
import com.blueth.guard.ui.theme.RiskSafe
import com.blueth.guard.ui.viewmodel.AppListViewModel
import kotlinx.coroutines.delay

@Composable
fun SecurityScreen(
    viewModel: AppListViewModel = hiltViewModel()
) {
    val apps by viewModel.apps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var isScanning by remember { mutableStateOf(false) }
    var scanComplete by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "Security",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Scan button
        item {
            ScanButton(
                isScanning = isScanning,
                scanComplete = scanComplete,
                scanProgress = scanProgress,
                totalApps = apps.size,
                onScanClick = {
                    if (!isScanning) {
                        isScanning = true
                        scanComplete = false
                        scanProgress = 0
                    }
                }
            )

            if (isScanning) {
                LaunchedEffect(Unit) {
                    for (i in 1..apps.size.coerceAtLeast(1)) {
                        delay(50)
                        scanProgress = i
                    }
                    delay(500)
                    isScanning = false
                    scanComplete = true
                }
            }
        }

        // Scan results
        if (scanComplete && !isLoading) {
            val highRiskApps = apps.filter { it.riskScore > 60 }
            val mediumRiskApps = apps.filter { it.riskScore in 41..60 }
            val lowRiskApps = apps.filter { it.riskScore in 21..40 }
            val safeApps = apps.filter { it.riskScore <= 20 }

            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    RiskSummaryCard(
                        total = apps.size,
                        critical = highRiskApps.size,
                        medium = mediumRiskApps.size,
                        low = lowRiskApps.size,
                        safe = safeApps.size
                    )
                }
            }

            if (highRiskApps.isNotEmpty()) {
                item {
                    Text(
                        "High Risk Apps",
                        style = MaterialTheme.typography.titleMedium,
                        color = RiskHigh,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(highRiskApps.take(10), key = { it.packageName }) { app ->
                    FlaggedAppCard(app)
                }
            }

            if (mediumRiskApps.isNotEmpty()) {
                item {
                    Text(
                        "Medium Risk Apps",
                        style = MaterialTheme.typography.titleMedium,
                        color = RiskMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(mediumRiskApps.take(10), key = { it.packageName }) { app ->
                    FlaggedAppCard(app)
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
            // Outer ring animation
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

            // Progress ring
            Canvas(modifier = Modifier.size(180.dp)) {
                // Background ring
                drawArc(
                    color = Color(0xFF1E2438),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx())
                )
                // Progress
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

            // Center button
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
                        contentDescription = "Scan",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isScanning) "Scanning..." else if (scanComplete) "Rescan" else "Scan",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        if (isScanning) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Scanning $scanProgress / $totalApps apps...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RiskSummaryCard(
    total: Int,
    critical: Int,
    medium: Int,
    low: Int,
    safe: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Scan Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "$total apps scanned",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            RiskRow("Critical / High", critical, RiskCritical)
            RiskRow("Medium", medium, RiskMedium)
            RiskRow("Low", low, RiskLow)
            RiskRow("Safe", safe, RiskSafe)
        }
    }
}

@Composable
private fun RiskRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FlaggedAppCard(app: AppInfo) {
    val riskColor = when {
        app.riskScore > 80 -> RiskCritical
        app.riskScore > 60 -> RiskHigh
        app.riskScore > 40 -> RiskMedium
        app.riskScore > 20 -> RiskLow
        else -> RiskSafe
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = riskColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${app.permissions.size} permissions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    app.riskScore.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = riskColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "risk",
                    style = MaterialTheme.typography.labelSmall,
                    color = riskColor
                )
            }
        }
    }
}
