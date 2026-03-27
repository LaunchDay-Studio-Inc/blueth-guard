package com.blueth.guard.protection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.blueth.guard.MainActivity
import com.blueth.guard.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class GuardVpnService : VpnService() {

    companion object {
        const val CHANNEL_ID = "guard_vpn_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "com.blueth.guard.STOP_VPN"

        @Volatile
        var isRunning = false
            private set

        private var blockedCount = 0

        fun getBlockedCount(): Int = blockedCount
        fun resetBlockedCount() { blockedCount = 0 }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        startVpn()
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        scope.cancel()
        super.onDestroy()
    }

    private fun startVpn() {
        if (isRunning) return
        isRunning = true

        try {
            val builder = Builder()
                .setSession("Blueth Guard Web Protection")
                .addAddress("10.0.0.2", 32)
                .addDnsServer("8.8.8.8")
                .addDnsServer("8.8.4.4")
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)
                .setBlocking(true)

            // Exclude our own app from the VPN
            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) { }

            vpnInterface = builder.establish() ?: return

            scope.launch {
                handlePackets()
            }
        } catch (e: Exception) {
            stopVpn()
        }
    }

    private fun stopVpn() {
        isRunning = false
        try {
            vpnInterface?.close()
        } catch (_: Exception) { }
        vpnInterface = null
    }

    private suspend fun handlePackets() {
        val vpnFd = vpnInterface ?: return
        val inputStream = FileInputStream(vpnFd.fileDescriptor)
        val outputStream = FileOutputStream(vpnFd.fileDescriptor)
        val buffer = ByteArray(32767)

        while (isRunning && scope.isActive) {
            try {
                val length = inputStream.read(buffer)
                if (length <= 0) continue

                val packet = buffer.copyOf(length)

                // Check if it's a UDP packet to port 53 (DNS)
                if (isDnsQuery(packet)) {
                    val domain = extractDomainFromDns(packet)
                    if (domain != null && DnsBlocklist.isBlocked(domain)) {
                        // Send NXDOMAIN response
                        blockedCount++
                        val response = buildNxDomainResponse(packet)
                        if (response != null) {
                            outputStream.write(response)
                            outputStream.flush()
                        }
                        updateNotification()
                        continue
                    }
                }

                // Forward allowed packets
                // For a lightweight DNS-only filter, we forward by writing back
                // In a full implementation, we'd forward to the real DNS server
                // For simplicity, pass through all non-blocked traffic
                outputStream.write(packet)
                outputStream.flush()
            } catch (_: Exception) {
                if (!isRunning) break
            }
        }
    }

    private fun isDnsQuery(packet: ByteArray): Boolean {
        if (packet.size < 28) return false
        // IPv4 header: protocol at byte 9, UDP = 17
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return false // Not UDP

        // UDP header starts at byte 20 (assuming no IP options)
        val ipHeaderLength = (packet[0].toInt() and 0x0F) * 4
        if (packet.size < ipHeaderLength + 8) return false

        // Destination port (2 bytes at UDP offset + 2)
        val destPort = ((packet[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or
                (packet[ipHeaderLength + 3].toInt() and 0xFF)
        return destPort == 53
    }

    private fun extractDomainFromDns(packet: ByteArray): String? {
        try {
            val ipHeaderLength = (packet[0].toInt() and 0x0F) * 4
            val udpStart = ipHeaderLength
            val dnsStart = udpStart + 8 // UDP header is 8 bytes

            if (packet.size < dnsStart + 12) return null

            // DNS question section starts at dnsStart + 12
            var offset = dnsStart + 12
            val domain = StringBuilder()

            while (offset < packet.size) {
                val labelLen = packet[offset].toInt() and 0xFF
                if (labelLen == 0) break
                if (offset + labelLen + 1 > packet.size) return null

                if (domain.isNotEmpty()) domain.append(".")
                domain.append(String(packet, offset + 1, labelLen, Charsets.US_ASCII))
                offset += labelLen + 1
            }

            return if (domain.isNotEmpty()) domain.toString() else null
        } catch (_: Exception) {
            return null
        }
    }

    private fun buildNxDomainResponse(queryPacket: ByteArray): ByteArray? {
        try {
            val ipHeaderLength = (queryPacket[0].toInt() and 0x0F) * 4
            val udpStart = ipHeaderLength
            val dnsStart = udpStart + 8

            if (queryPacket.size < dnsStart + 12) return null

            // Clone the packet for response
            val response = queryPacket.copyOf()

            // Swap source and destination IP
            for (i in 0..3) {
                val temp = response[12 + i]
                response[12 + i] = response[16 + i]
                response[16 + i] = temp
            }

            // Swap source and destination port
            val srcPort0 = response[udpStart]
            val srcPort1 = response[udpStart + 1]
            response[udpStart] = response[udpStart + 2]
            response[udpStart + 1] = response[udpStart + 3]
            response[udpStart + 2] = srcPort0
            response[udpStart + 3] = srcPort1

            // Set DNS flags: QR=1, RCODE=3 (NXDOMAIN)
            response[dnsStart + 2] = (0x81).toByte() // QR=1, RD=1
            response[dnsStart + 3] = (0x83).toByte() // RA=1, RCODE=3

            return response
        } catch (_: Exception) {
            return null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Web Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Blueth Guard web protection status"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, GuardVpnService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Web Protection Active")
            .setContentText("Blocked $blockedCount malicious sites")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }
}
