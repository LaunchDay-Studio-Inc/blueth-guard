package com.blueth.guard.ui.screens

import android.content.Context
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.blueth.guard.R
import com.blueth.guard.optimizer.AppCacheInfo
import com.blueth.guard.optimizer.BloatwareAction
import com.blueth.guard.optimizer.BloatwareApp
import com.blueth.guard.optimizer.BloatwareCategory
import com.blueth.guard.optimizer.DuplicateGroup
import com.blueth.guard.optimizer.ProcessCategory
import com.blueth.guard.optimizer.RunningProcess
import com.blueth.guard.optimizer.StorageBreakdown
import com.blueth.guard.ui.theme.BluePrimary
import com.blueth.guard.ui.theme.CyanSecondary
import com.blueth.guard.ui.theme.RiskCritical
import com.blueth.guard.ui.theme.RiskHigh
import com.blueth.guard.ui.theme.RiskMedium
import com.blueth.guard.ui.theme.RiskSafe
import com.blueth.guard.ui.viewmodel.OptimizerTab
import com.blueth.guard.ui.viewmodel.OptimizerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizerScreen(
    optimizerViewModel: OptimizerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentTab by optimizerViewModel.optimizerTab.collectAsState()
    val snackbarMessage by optimizerViewModel.snackbarMessage.collectAsState()

    // Observe snackbar messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            optimizerViewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            Text(
                stringResource(R.string.optimizer_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            // Overview cards
            OverviewCards(optimizerViewModel, context)

            Spacer(Modifier.height(8.dp))

            // Tab row
            OptimizerTabRow(optimizerViewModel, currentTab)

            // Tab content
            Crossfade(targetState = currentTab, label = "optimizer_tab") { tab ->
                when (tab) {
                    OptimizerTab.OVERVIEW -> OverviewTab(optimizerViewModel, context)
                    OptimizerTab.CACHE -> CacheTab(optimizerViewModel, context)
                    OptimizerTab.PROCESSES -> ProcessesTab(optimizerViewModel, context)
                    OptimizerTab.DUPLICATES -> DuplicatesTab(optimizerViewModel, context)
                    OptimizerTab.BLOATWARE -> BloatwareTab(optimizerViewModel, context)
                }
            }
        }
    }
}

// ========== Overview Cards ==========

@Composable
private fun OverviewCards(viewModel: OptimizerViewModel, context: Context) {
    val ramTotal by viewModel.ramTotal.collectAsState()
    val ramAvailable by viewModel.ramAvailable.collectAsState()
    val storageBreakdown by viewModel.storageBreakdown.collectAsState()
    val totalCacheSize by viewModel.totalCacheSize.collectAsState()
    val killableProcesses by viewModel.killableProcesses.collectAsState()

    val usedRam = ramTotal - ramAvailable
    val ramPercent = if (ramTotal > 0) usedRam.toFloat() / ramTotal else 0f

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // RAM card
            OverviewMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Memory,
                iconColor = BluePrimary,
                title = stringResource(R.string.optimizer_ram),
                value = stringResource(R.string.optimizer_free_format, Formatter.formatFileSize(context, ramAvailable)),
                progress = ramPercent,
                progressColor = if (ramPercent > 0.8f) RiskCritical else BluePrimary,
                buttonText = stringResource(R.string.optimizer_boost),
                onButtonClick = { viewModel.killAllKillable() }
            )

            // Storage card
            val storageProgress = storageBreakdown?.usedPercentage ?: 0f
            OverviewMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Storage,
                iconColor = CyanSecondary,
                title = stringResource(R.string.optimizer_storage),
                value = stringResource(
                    R.string.optimizer_free_format,
                    Formatter.formatFileSize(context, storageBreakdown?.freeBytes ?: 0)
                ),
                progress = storageProgress,
                progressColor = if (storageProgress > 0.9f) RiskCritical else CyanSecondary
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cache card
            OverviewMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CleaningServices,
                iconColor = RiskMedium,
                title = stringResource(R.string.optimizer_tab_cache),
                value = Formatter.formatFileSize(context, totalCacheSize),
                buttonText = stringResource(R.string.optimizer_clean),
                onButtonClick = { viewModel.clearAllCaches() }
            )

            // Processes card
            val killableCount = killableProcesses.size
            OverviewMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Speed,
                iconColor = if (killableCount > 5) RiskHigh else RiskSafe,
                title = stringResource(R.string.optimizer_tab_processes),
                value = stringResource(R.string.optimizer_killable_count, killableCount.toString()),
                buttonText = stringResource(R.string.optimizer_boost),
                onButtonClick = { viewModel.killAllKillable() }
            )
        }
    }
}

@Composable
private fun OverviewMiniCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    progress: Float? = null,
    progressColor: Color = BluePrimary,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))

            progress?.let {
                LinearProgressIndicator(
                    progress = { it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surface,
                )
                Spacer(Modifier.height(4.dp))
            }

            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (buttonText != null && onButtonClick != null) {
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onButtonClick,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Text(buttonText, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ========== Tab Row ==========

@Composable
private fun OptimizerTabRow(viewModel: OptimizerViewModel, currentTab: OptimizerTab) {
    val appCaches by viewModel.appCaches.collectAsState()
    val killableProcesses by viewModel.killableProcesses.collectAsState()
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val bloatwareCount by viewModel.bloatwareCount.collectAsState()

    val tabs = listOf(
        OptimizerTab.OVERVIEW to Pair(stringResource(R.string.optimizer_tab_overview), 0),
        OptimizerTab.CACHE to Pair(stringResource(R.string.optimizer_tab_cache), appCaches.count { it.cacheSize > 1_048_576 }),
        OptimizerTab.PROCESSES to Pair(stringResource(R.string.optimizer_tab_processes), killableProcesses.size),
        OptimizerTab.DUPLICATES to Pair(stringResource(R.string.optimizer_tab_duplicates), duplicateGroups.size),
        OptimizerTab.BLOATWARE to Pair(stringResource(R.string.optimizer_tab_bloatware), bloatwareCount)
    )

    ScrollableTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == currentTab }.coerceAtLeast(0),
        containerColor = MaterialTheme.colorScheme.surface,
        edgePadding = 16.dp
    ) {
        tabs.forEach { (tab, labelAndCount) ->
            val (label, count) = labelAndCount
            Tab(
                selected = currentTab == tab,
                onClick = { viewModel.setTab(tab) },
                text = {
                    if (count > 0) {
                        BadgedBox(badge = {
                            Badge { Text("$count") }
                        }) {
                            Text(label)
                        }
                    } else {
                        Text(label)
                    }
                }
            )
        }
    }
}

// ========== OVERVIEW Tab ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewTab(viewModel: OptimizerViewModel, context: Context) {
    val storageBreakdown by viewModel.storageBreakdown.collectAsState()
    val appCaches by viewModel.appCaches.collectAsState()
    val ramTotal by viewModel.ramTotal.collectAsState()
    val ramAvailable by viewModel.ramAvailable.collectAsState()
    val killableProcesses by viewModel.killableProcesses.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.loadAll()
            isRefreshing = false
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Storage breakdown chart
            item {
                storageBreakdown?.let { breakdown ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.optimizer_storage_breakdown),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                            StorageBreakdownChart(breakdown, context)
                        }
                    }
                }
            }

            // Quick stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.optimizer_quick_stats),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        QuickStatRow(stringResource(R.string.optimizer_total_apps), "${appCaches.size}")
                        QuickStatRow(
                            stringResource(R.string.optimizer_total_cache),
                            Formatter.formatFileSize(context, appCaches.sumOf { it.cacheSize })
                        )
                        QuickStatRow(
                            stringResource(R.string.optimizer_ram_used),
                            Formatter.formatFileSize(context, ramTotal - ramAvailable)
                        )
                        QuickStatRow(
                            stringResource(R.string.optimizer_killable_processes),
                            "${killableProcesses.size}"
                        )
                    }
                }
            }

            // One-tap optimize
            item {
                Button(
                    onClick = {
                        viewModel.killAllKillable()
                        viewModel.clearAllCaches()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Filled.Rocket, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.optimizer_one_tap_optimize),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun QuickStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

// ========== CACHE Tab ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CacheTab(viewModel: OptimizerViewModel, context: Context) {
    val appCaches by viewModel.appCaches.collectAsState()
    val totalCacheSize by viewModel.totalCacheSize.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    val filteredCaches = appCaches.filter { it.cacheSize > 1_048_576 } // > 1MB

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.refreshCaches()
            isRefreshing = false
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Total cache header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (totalCacheSize > 500_000_000)
                            RiskMedium.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.optimizer_total_cache),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            Formatter.formatFileSize(context, totalCacheSize),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalCacheSize > 500_000_000) RiskMedium else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.clearAllCaches() }) {
                            Icon(Icons.Filled.CleaningServices, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.optimizer_clean_all))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.optimizer_cache_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (filteredCaches.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.optimizer_no_large_caches),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(filteredCaches, key = { it.packageName }) { cache ->
                CacheAppRow(cache, context) { viewModel.clearAppCache(cache.packageName) }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun CacheAppRow(cache: AppCacheInfo, context: Context, onClear: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClear),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BluePrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    cache.appName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    cache.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    Formatter.formatFileSize(context, cache.cacheSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClear) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.optimizer_open_app_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ========== PROCESSES Tab ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcessesTab(viewModel: OptimizerViewModel, context: Context) {
    val runningProcesses by viewModel.runningProcesses.collectAsState()
    val killableProcesses by viewModel.killableProcesses.collectAsState()
    val killableMemoryKb by viewModel.killableMemoryKb.collectAsState()
    val ramTotal by viewModel.ramTotal.collectAsState()
    val ramAvailable by viewModel.ramAvailable.collectAsState()
    val lastKillResult by viewModel.lastKillResult.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    val usedRam = ramTotal - ramAvailable
    val ramPercent = if (ramTotal > 0) usedRam.toFloat() / ramTotal else 0f

    val essentialProcesses = runningProcesses.filter {
        it.processCategory == ProcessCategory.ESSENTIAL_SYSTEM || it.processCategory == ProcessCategory.USER_ACTIVE
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.refreshProcesses()
            isRefreshing = false
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // RAM usage bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Memory, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.optimizer_ram),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ramPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (ramPercent > 0.8f) RiskCritical else BluePrimary,
                            trackColor = MaterialTheme.colorScheme.surface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${Formatter.formatFileSize(context, usedRam)} used",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${Formatter.formatFileSize(context, ramTotal)} total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Smart Boost button
            item {
                Button(
                    onClick = { viewModel.killAllKillable() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Filled.FlashOn, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.optimizer_smart_boost))
                }
            }

            // Kill result card
            lastKillResult?.let { result ->
                item {
                    AnimatedVisibility(visible = true, enter = fadeIn()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = RiskSafe.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = RiskSafe)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.optimizer_freed_memory,
                                            Formatter.formatFileSize(context, result.estimatedMemoryFreedKb * 1024)),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = RiskSafe
                                    )
                                    Text(
                                        stringResource(R.string.optimizer_processes_killed, result.processesKilled.toString()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.clearKillResult() }) {
                                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.optimizer_dismiss))
                                }
                            }
                        }
                    }
                }
            }

            // Killable processes section
            if (killableProcesses.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.optimizer_can_be_freed, killableProcesses.size.toString()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = RiskHigh,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(killableProcesses, key = { "${it.packageName}_${it.serviceName}" }) { process ->
                    ProcessRow(process, context, RiskHigh) { viewModel.killProcess(process.packageName) }
                }
            }

            // Essential processes section
            if (essentialProcesses.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.optimizer_essential, essentialProcesses.size.toString()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = RiskSafe,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(essentialProcesses.take(15), key = { "ess_${it.packageName}_${it.serviceName}" }) { process ->
                    ProcessRow(process, context, RiskSafe, onKill = null)
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun ProcessRow(
    process: RunningProcess,
    context: Context,
    accentColor: Color,
    onKill: (() -> Unit)?
) {
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
                    .background(accentColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    process.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    Text(
                        Formatter.formatFileSize(context, process.memoryUsageKb * 1024),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    CategoryBadge(process.processCategory)
                }
            }
            if (onKill != null) {
                OutlinedButton(
                    onClick = onKill,
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Text(stringResource(R.string.optimizer_stop), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: ProcessCategory) {
    val (text, color) = when (category) {
        ProcessCategory.ESSENTIAL_SYSTEM -> "System" to RiskSafe
        ProcessCategory.USER_ACTIVE -> "Active" to BluePrimary
        ProcessCategory.BACKGROUND_SERVICE -> "Background" to RiskMedium
        ProcessCategory.CACHED -> "Cached" to RiskHigh
        ProcessCategory.KILLABLE -> "Killable" to RiskCritical
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// ========== DUPLICATES Tab ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuplicatesTab(viewModel: OptimizerViewModel, context: Context) {
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val scanProgress by viewModel.duplicateScanProgress.collectAsState()
    val isScanning by viewModel.isDuplicateScanning.collectAsState()
    val totalWasted by viewModel.totalWastedSpace.collectAsState()

    val selectedFiles = remember { mutableStateListOf<String>() }

    Scaffold(
        floatingActionButton = {
            if (selectedFiles.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        viewModel.deleteFiles(selectedFiles.toList())
                        selectedFiles.clear()
                    },
                    containerColor = RiskCritical
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.optimizer_delete_selected), tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            if (!isScanning && duplicateGroups.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.optimizer_scan_for_duplicates_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.startDuplicateScan() }) {
                            Icon(Icons.Filled.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.optimizer_scan_for_duplicates))
                        }
                    }
                }
            }

            // Scanning progress
            if (isScanning) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.optimizer_scanning),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            scanProgress?.let { progress ->
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = {
                                        if (progress.totalFiles > 0) progress.scannedFiles.toFloat() / progress.totalFiles
                                        else 0f
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${progress.scannedFiles}/${progress.totalFiles} files · ${progress.currentDir}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    stringResource(R.string.optimizer_found_groups, progress.foundGroups.toString()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Results
            if (!isScanning && duplicateGroups.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RiskMedium.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(R.string.optimizer_wasted_space),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                Formatter.formatFileSize(context, totalWasted),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = RiskMedium
                            )
                            Text(
                                stringResource(R.string.optimizer_duplicate_groups, duplicateGroups.size.toString()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(duplicateGroups, key = { it.hash }) { group ->
                    DuplicateGroupCard(group, context, selectedFiles) { path ->
                        if (path in selectedFiles) selectedFiles.remove(path) else selectedFiles.add(path)
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) } // FAB space
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    context: Context,
    selectedFiles: List<String>,
    onToggleFile: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.files.first().name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${group.files.size} copies · ${Formatter.formatFileSize(context, group.totalWastedBytes)} wasted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CategoryChip(group.category.name)
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                group.files.forEachIndexed { index, file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (index > 0) { // Don't select the first (newest) file
                            Checkbox(
                                checked = file.path in selectedFiles,
                                onCheckedChange = { onToggleFile(file.path) }
                            )
                        } else {
                            Spacer(Modifier.width(48.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                file.path.substringAfterLast("/"),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                file.path.substringBeforeLast("/"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            Formatter.formatFileSize(context, file.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = CyanSecondary,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CyanSecondary.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// ========== BLOATWARE Tab ==========

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BloatwareTab(viewModel: OptimizerViewModel, context: Context) {
    val bloatwareApps by viewModel.bloatwareApps.collectAsState()
    var selectedFilter by remember { mutableStateOf<BloatwareCategory?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val filteredApps = if (selectedFilter != null) {
        bloatwareApps.filter { it.category == selectedFilter }
    } else {
        bloatwareApps
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.loadAll()
            isRefreshing = false
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Manufacturer badge
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = BluePrimary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(R.string.optimizer_device_detected, viewModel.getDetectedManufacturer()),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.optimizer_bloatware_found, bloatwareApps.size.toString()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Filter chips
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text(stringResource(R.string.optimizer_filter_all)) }
                    )
                    FilterChip(
                        selected = selectedFilter == BloatwareCategory.CARRIER,
                        onClick = { selectedFilter = if (selectedFilter == BloatwareCategory.CARRIER) null else BloatwareCategory.CARRIER },
                        label = { Text(stringResource(R.string.optimizer_filter_carrier)) }
                    )
                    FilterChip(
                        selected = selectedFilter == BloatwareCategory.MANUFACTURER,
                        onClick = { selectedFilter = if (selectedFilter == BloatwareCategory.MANUFACTURER) null else BloatwareCategory.MANUFACTURER },
                        label = { Text(stringResource(R.string.optimizer_filter_manufacturer)) }
                    )
                    FilterChip(
                        selected = selectedFilter == BloatwareCategory.SOCIAL_MEDIA_PREINSTALL || selectedFilter == BloatwareCategory.SHOPPING_PREINSTALL || selectedFilter == BloatwareCategory.GAME_PREINSTALL,
                        onClick = { selectedFilter = if (selectedFilter == BloatwareCategory.SOCIAL_MEDIA_PREINSTALL) null else BloatwareCategory.SOCIAL_MEDIA_PREINSTALL },
                        label = { Text(stringResource(R.string.optimizer_filter_preinstalled)) }
                    )
                }
            }

            if (filteredApps.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.optimizer_no_bloatware),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(filteredApps, key = { it.packageName }) { app ->
                BloatwareAppRow(app, context) { viewModel.disableBloatware(app.packageName) }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun BloatwareAppRow(
    app: BloatwareApp,
    context: Context,
    onAction: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BluePrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        app.appName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (app.isDisabled) TextDecoration.LineThrough else null
                    )
                    Row {
                        BloatwareActionBadge(app.recommendation)
                        if (app.isDisabled) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.optimizer_disabled),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    app.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedButton(onClick = onAction) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.optimizer_open_settings))
                    }
                }
            }
        }
    }
}

@Composable
private fun BloatwareActionBadge(action: BloatwareAction) {
    val (text, color) = when (action) {
        BloatwareAction.SAFE_TO_DISABLE -> "Safe to disable" to RiskSafe
        BloatwareAction.SAFE_TO_UNINSTALL -> "Safe to uninstall" to RiskSafe
        BloatwareAction.CAUTION -> "Caution" to RiskMedium
        BloatwareAction.DO_NOT_TOUCH -> "Don't touch" to RiskCritical
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// ========== Storage Chart (preserved from original) ==========

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
