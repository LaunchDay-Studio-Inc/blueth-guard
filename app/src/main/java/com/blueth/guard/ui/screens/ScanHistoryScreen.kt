package com.blueth.guard.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.blueth.guard.R
import com.blueth.guard.data.local.ScanHistoryEntry
import com.blueth.guard.ui.theme.BluePrimary
import com.blueth.guard.ui.theme.RiskCritical
import com.blueth.guard.ui.theme.RiskMedium
import com.blueth.guard.ui.theme.RiskSafe
import com.blueth.guard.ui.viewmodel.ScanHistoryViewModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScanHistoryViewModel = hiltViewModel()
) {
    val entries by viewModel.scanHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Filled.DeleteSweep, stringResource(R.string.scan_history_clear))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BluePrimary)
            }
        } else if (entries.isEmpty()) {
            EmptyHistoryContent(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                itemsIndexed(entries) { index, entry ->
                    TimelineItem(
                        entry = entry,
                        isFirst = index == 0,
                        isLast = index == entries.lastIndex
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyHistoryContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.scan_history_empty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.scan_history_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TimelineItem(
    entry: ScanHistoryEntry,
    isFirst: Boolean,
    isLast: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    val dotColor = when {
        entry.overallScore >= 75 -> RiskSafe
        entry.overallScore >= 50 -> RiskMedium
        else -> RiskCritical
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline visual (left side)
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Top connector line
                if (!isFirst) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                }

                // Dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                // Bottom connector line
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f, fill = true)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Card (right side)
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
                .animateContentSize()
                .clickable { expanded = !expanded },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Date/time
                val dateFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
                Text(
                    dateFormat.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                // Score and stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Score
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${entry.overallScore}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = dotColor
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "/100",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Duration
                    Text(
                        "${entry.scanDurationMs / 1000}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatLabel("Apps", entry.totalAppsScanned.toString())
                    StatLabel(stringResource(R.string.scan_history_threats), entry.threatsFound.toString())
                    StatLabel(stringResource(R.string.scan_history_trackers), entry.trackersFound.toString())
                }

                // Expanded: flagged apps
                if (expanded) {
                    Spacer(Modifier.height(12.dp))
                    val flaggedApps = parseFlaggedApps(entry.flaggedApps)
                    if (flaggedApps.isNotEmpty()) {
                        Text(
                            stringResource(R.string.scan_history_flagged_apps),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        flaggedApps.take(10).forEach { (pkg, risk) ->
                            val riskColor = when (risk) {
                                "CRITICAL" -> RiskCritical
                                "HIGH" -> RiskCritical
                                "MEDIUM" -> RiskMedium
                                "LOW" -> RiskSafe
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(riskColor)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    pkg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    risk,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = riskColor
                                )
                            }
                        }
                        if (flaggedApps.size > 10) {
                            Text(
                                stringResource(R.string.scan_history_more_format, flaggedApps.size - 10),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.scan_history_all_passed),
                            style = MaterialTheme.typography.bodySmall,
                            color = RiskSafe
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatLabel(label: String, value: String) {
    Column {
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun parseFlaggedApps(json: String): List<Pair<String, String>> {
    return try {
        val array = Json.parseToJsonElement(json).jsonArray
        array.map { element ->
            val obj = element.jsonObject
            val pkg = obj["pkg"]?.jsonPrimitive?.content ?: ""
            val risk = obj["risk"]?.jsonPrimitive?.content ?: ""
            pkg to risk
        }
    } catch (_: Exception) {
        emptyList()
    }
}
