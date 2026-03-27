package com.blueth.guard.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.blueth.guard.antitheft.AntiTheftManager
import com.blueth.guard.ui.theme.RiskCritical
import com.blueth.guard.ui.theme.RiskSafe
import com.blueth.guard.ui.viewmodel.AntiTheftViewModel

@Composable
fun AntiTheftScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AntiTheftViewModel = hiltViewModel()
) {
    val antiTheftManager = viewModel.antiTheftManager
    val context = LocalContext.current
    var showWipeDialog by remember { mutableStateOf(false) }
    var showWipeConfirm2 by remember { mutableStateOf(false) }
    var isAlarmOn by remember { mutableStateOf(antiTheftManager.isAlarmPlaying()) }
    val isAdmin = remember { mutableStateOf(antiTheftManager.isDeviceAdminEnabled()) }
    var location by remember { mutableStateOf(antiTheftManager.getLastKnownLocation()) }

    DisposableEffect(Unit) {
        onDispose {
            // Don't stop alarm on screen leave — it should keep going
        }
    }

    // Wipe confirmation dialogs
    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = RiskCritical, modifier = Modifier.size(48.dp)) },
            title = { Text("Emergency Wipe", fontWeight = FontWeight.Bold, color = RiskCritical) },
            text = { Text("This will PERMANENTLY DELETE ALL DATA on this device. This action CANNOT be undone. Are you absolutely sure?") },
            confirmButton = {
                Button(
                    onClick = { showWipeDialog = false; showWipeConfirm2 = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskCritical)
                ) { Text("Yes, I understand") }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showWipeConfirm2) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm2 = false },
            icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = RiskCritical, modifier = Modifier.size(48.dp)) },
            title = { Text("FINAL CONFIRMATION", fontWeight = FontWeight.Bold, color = RiskCritical) },
            text = { Text("ALL DATA WILL BE ERASED. This is your last chance to cancel. The device will be factory reset.") },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeConfirm2 = false
                        antiTheftManager.wipeDevice()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskCritical)
                ) { Text("WIPE DEVICE") }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm2 = false }) { Text("Cancel") }
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Anti-Theft",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Device Admin Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAdmin.value) RiskSafe.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        tint = if (isAdmin.value) RiskSafe else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Device Admin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isAdmin.value) "Enabled — Lock and Wipe available"
                            else "Disabled — Enable for anti-theft features",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isAdmin.value) {
                        Button(onClick = {
                            context.startActivity(antiTheftManager.getDeviceAdminIntent())
                        }) { Text("Enable") }
                    }
                }
            }
        }

        // Sound Alarm
        item {
            ActionCard(
                icon = Icons.Filled.VolumeUp,
                title = "Sound Alarm",
                description = if (isAlarmOn) "Alarm is playing — tap to stop"
                else "Play a loud alarm at max volume for 30 seconds",
                buttonText = if (isAlarmOn) "Stop Alarm" else "Sound Alarm",
                buttonColor = if (isAlarmOn) RiskCritical else MaterialTheme.colorScheme.primary,
                onClick = {
                    if (isAlarmOn) {
                        antiTheftManager.stopAlarm()
                        isAlarmOn = false
                    } else {
                        antiTheftManager.startAlarm()
                        isAlarmOn = true
                    }
                }
            )
        }

        // Lock Device
        item {
            ActionCard(
                icon = Icons.Filled.Lock,
                title = "Lock Device",
                description = "Locks the screen immediately",
                buttonText = "Lock Now",
                enabled = isAdmin.value,
                disabledText = "Enable Device Admin first",
                onClick = { antiTheftManager.lockDevice() }
            )
        }

        // Show Location
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Show Location", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("Last known GPS coordinates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    if (location != null) {
                        Text("Latitude: ${location!!.latitude}", style = MaterialTheme.typography.bodyMedium)
                        Text("Longitude: ${location!!.longitude}", style = MaterialTheme.typography.bodyMedium)
                        Text("Accuracy: ±${location!!.accuracy.toInt()}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Provider: ${location!!.provider}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            val uri = Uri.parse("geo:${location!!.latitude},${location!!.longitude}?q=${location!!.latitude},${location!!.longitude}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Open in Maps")
                        }
                    } else {
                        Text(
                            if (antiTheftManager.hasLocationPermission())
                                "No location data available. Try again later."
                            else "Location permission not granted.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        location = antiTheftManager.getLastKnownLocation()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Refresh Location")
                    }
                }
            }
        }

        // Emergency Wipe
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RiskCritical.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null,
                            tint = RiskCritical, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Emergency Wipe", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = RiskCritical)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "WARNING: This will permanently erase ALL data on this device. " +
                                "This cannot be undone. Only use in emergencies when your device is stolen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RiskCritical
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showWipeDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RiskCritical),
                        enabled = isAdmin.value
                    ) {
                        Text(if (isAdmin.value) "Emergency Wipe" else "Enable Device Admin first")
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    buttonColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    disabledText: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = buttonColor, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                enabled = enabled
            ) {
                Text(if (enabled) buttonText else (disabledText ?: buttonText))
            }
        }
    }
}
