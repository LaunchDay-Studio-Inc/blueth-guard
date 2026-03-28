package com.blueth.guard.protection

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

data class WifiSecurityResult(
    val isConnected: Boolean,
    val ssid: String?,
    val securityType: WifiSecurityType,
    val warnings: List<String>,
    val dnsServers: List<String>
)

enum class WifiSecurityType {
    OPEN, WEP, WPA, WPA2, WPA3, UNKNOWN, NOT_CONNECTED
}

@Singleton
class WifiSecurityChecker @Inject constructor(
    private val app: Application
) {
    @Suppress("DEPRECATION")
    fun check(): WifiSecurityResult {
        val wifiManager = app.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as? WifiManager
        val connectivityManager = app.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (wifiManager == null || connectivityManager == null) {
            return WifiSecurityResult(false, null, WifiSecurityType.NOT_CONNECTED, emptyList(), emptyList())
        }

        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        if (!isWifi) {
            return WifiSecurityResult(false, null, WifiSecurityType.NOT_CONNECTED, emptyList(), emptyList())
        }

        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo?.ssid?.removeSurrounding("\"") ?: "Unknown"

        val securityType = detectSecurityType(wifiManager)

        val dnsServers = mutableListOf<String>()
        val dhcpInfo = wifiManager.dhcpInfo
        if (dhcpInfo != null) {
            if (dhcpInfo.dns1 != 0) dnsServers.add(intToIp(dhcpInfo.dns1))
            if (dhcpInfo.dns2 != 0) dnsServers.add(intToIp(dhcpInfo.dns2))
        }

        val warnings = mutableListOf<String>()
        when (securityType) {
            WifiSecurityType.OPEN -> warnings.add("You're on an open Wi-Fi network. Your traffic may be visible to others.")
            WifiSecurityType.WEP -> warnings.add("WEP encryption is insecure and can be cracked in minutes. Use WPA2/WPA3 instead.")
            else -> {}
        }

        val privacyHostileDns = setOf("114.114.114.114", "119.29.29.29")
        dnsServers.forEach { dns ->
            if (dns in privacyHostileDns) {
                warnings.add("DNS server $dns may log your queries. Consider using a privacy-focused DNS.")
            }
        }

        return WifiSecurityResult(
            isConnected = true,
            ssid = ssid,
            securityType = securityType,
            warnings = warnings,
            dnsServers = dnsServers
        )
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun detectSecurityType(wifiManager: WifiManager): WifiSecurityType {
        if (!hasWifiScanPermission()) {
            return WifiSecurityType.UNKNOWN
        }

        return try {
            val scanResults = wifiManager.scanResults
            val currentSsid = wifiManager.connectionInfo?.ssid?.removeSurrounding("\"")

            val currentNetwork = scanResults.firstOrNull {
                it.SSID == currentSsid
            } ?: return WifiSecurityType.UNKNOWN

            val capabilities = currentNetwork.capabilities
            when {
                capabilities.contains("WPA3") -> WifiSecurityType.WPA3
                capabilities.contains("WPA2") -> WifiSecurityType.WPA2
                capabilities.contains("WPA") -> WifiSecurityType.WPA
                capabilities.contains("WEP") -> WifiSecurityType.WEP
                capabilities.contains("ESS") && !capabilities.contains("WPA") && !capabilities.contains("WEP") -> WifiSecurityType.OPEN
                else -> WifiSecurityType.UNKNOWN
            }
        } catch (_: Exception) {
            WifiSecurityType.UNKNOWN
        }
    }

    private fun hasWifiScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                app,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                app,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
    }
}
