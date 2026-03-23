package com.blueth.guard.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.blueth.guard.optimizer.StorageBreakdown
import com.blueth.guard.ui.theme.BluePrimary
import com.blueth.guard.ui.theme.CyanSecondary
import com.blueth.guard.ui.viewmodel.AppListViewModel
import com.blueth.guard.ui.viewmodel.OptimizerViewModel

@Composable
fun OptimizerScreen(
    appListViewModel: AppListViewModel = hiltViewModel(),
    optimizerViewModel: OptimizerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val apps by appListViewModel.apps.collectAsState()
    val storageBreakdown by optimizerViewModel.storageBreakdown.collectAsState()

    // RAM info
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = remember {
        ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
    }
    val totalRam = memoryInfo.totalMem
    val availableRam = memoryInfo.availMem
    val usedRam = totalRam - availableRam
    val ramUsagePercent = usedRam.toFloat() / totalRam

    // Running services
    var runningServices by remember { mutableStateOf<List<ActivityManager.RunningServiceInfo>>(emptyList()) }
    LaunchedEffect(Unit) {
        @Suppress("DEPRECATION")
        runningServices = activityManager.getRunningServices(100)
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
                "Optimize",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // RAM Usage
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Memory,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "RAM Usage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { ramUsagePercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = if (ramUsagePercent > 0.8f) Color(0xFFF44336) else BluePrimary,
                        trackColor = MaterialTheme.colorScheme.surface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Used: ${Formatter.formatFileSize(context, usedRam)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Total: ${Formatter.formatFileSize(context, totalRam)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Storage Usage
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Storage,
                            contentDescription = null,
                            tint = CyanSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Storage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    storageBreakdown?.let { breakdown ->
                        LinearProgressIndicator(
                            progress = { breakdown.usedPercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = CyanSecondary,
                            trackColor = MaterialTheme.colorScheme.surface,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Used: ${Formatter.formatFileSize(context, breakdown.usedBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Total: ${Formatter.formatFileSize(context, breakdown.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        StorageBreakdownChart(breakdown, context)
                    }
                }
            }
        }

        // Cache cleanup
        item {
            val totalCache = apps.sumOf { it.cacheSize }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Total Cache",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Formatter.formatFileSize(context, totalCache),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (totalCache > 500_000_000) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { /* Cache cleaning requires system privileges */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Clean All Caches")
                    }
                }
            }
        }

        // Running services
        item {
            Text(
                "Running Services (${runningServices.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(runningServices.take(20)) { service ->
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
                            .background(if (service.foreground) Color(0xFF4CAF50) else Color(0xFFFF9800))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val serviceName = service.service.shortClassName.substringAfterLast(".")
                        Text(
                            serviceName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            service.service.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        if (service.foreground) "FG" else "BG",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (service.foreground) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun StorageBreakdownChart(breakdown: StorageBreakdown, context: Context) {
    Column {
        // Stacked horizontal bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            breakdown.categories.forEach { category ->
                if (category.bytes > 0 && breakdown.totalBytes > 0) {
                    val fraction = category.bytes.toFloat() / breakdown.totalBytes
                    Box(
                        modifier = Modifier
                            .weight(fraction.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(Color(category.color))
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Legend
        breakdown.categories.forEach { category ->
            if (category.bytes > 0) {
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(category.color))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        category.name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        Formatter.formatFileSize(context, category.bytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
