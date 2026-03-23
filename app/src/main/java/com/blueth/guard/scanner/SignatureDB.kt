package com.blueth.guard.scanner

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class MalwareSignature(
    val packagePattern: String,
    val family: String,
    val severity: SignatureSeverity,
    val description: String
)

enum class SignatureSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class SignatureMatch(
    val signature: MalwareSignature,
    val matchType: MatchType
)

enum class MatchType { EXACT_PACKAGE, PATTERN_MATCH, CERTIFICATE_MATCH }

data class PackageSignatureInfo(
    val issuer: String,
    val subject: String,
    val serial: String,
    val sha256Fingerprint: String,
    val signatureScheme: String
)

@Singleton
class SignatureDB @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val exactPackages: HashSet<String> = hashSetOf(
        // === HiddenAds family ===
        "com.lovely.bouncycastle",
        "com.developer.flavourful.camera",
        "com.developer.flavor.camera.video",
        "com.developer.flavor.callscreen",
        "com.developer.flavor.recording",
        "com.developer.flavor.screenshot",
        "com.flavor.dark.wallpaper",
        "com.flavor.stamp.camera",
        "com.flavor.drawing.coloring",
        "com.flavor.photo.puzzle",

        // === Joker family ===
        "com.imagecompress.android",
        "com.contact.withme.texts",
        "com.hmvoice.friendsms",
        "com.relax.relaxation.attitudes",
        "com.cheery.message.sendsms",
        "com.peason.lovinglovemessage",
        "com.file.recovefiles",
        "com.LPlocker.lockapps",
        "com.remindme.alram",
        "com.training.memorygame",

        // === Harly family ===
        "com.binbin.flashlight",
        "com.launcher.overlay.live",
        "com.great.live.wallpaper",
        "com.chargeanim.battery.wallpaper",
        "com.amazing.ringtone.music",
        "com.neon.live.keyboard",

        // === Autolycos family ===
        "com.vlog.star.video.editor",
        "com.creative.3d.launcher",
        "com.funny.camera.faces",
        "com.gif.emoji.keyboard",
        "com.wow.beauty.camera",
        "com.freeglow.camera.beauty",
        "com.coco.camera.effect",
        "com.tezza.photo.editor",

        // === Anatsa/TeaBot banking trojan ===
        "com.az.cleanerapp.es",
        "com.securityprotect.antivirus",
        "com.document.reader.pro.pdf",
        "com.qrcode.barcode.scanner.reader",

        // === Godfather banking trojan ===
        "com.crypto.exchange.wallet",
        "com.banking.secure.login",

        // === SharkBot banking trojan ===
        "com.antivirus.supercleaner",
        "com.file.manager.fileexplorer",
        "com.mr.phone.cleaner.boost",
        "com.recovering.documents.files",

        // === Spyware ===
        "com.app.callrecorder.pro",
        "com.parentalcontrol.spyapp",
        "com.tracking.phone.spy",
        "com.spy.phone.tracker.gps"
    )

    private val patternSignatures: List<MalwareSignature> = listOf(
        MalwareSignature("com.hiddenads.", "HiddenAds", SignatureSeverity.HIGH, "HiddenAds adware family — hides icon, shows aggressive ads"),
        MalwareSignature("com.joker.", "Joker", SignatureSeverity.CRITICAL, "Joker malware — subscribes to premium services without consent"),
        MalwareSignature("com.fakeav.", "FakeAV", SignatureSeverity.HIGH, "Fake antivirus — displays false scan results to scare users"),
        MalwareSignature("com.fakecleaner.", "FakeCleaner", SignatureSeverity.MEDIUM, "Fake cleaner app — shows exaggerated junk findings"),
        MalwareSignature("com.adware.aggressive.", "AggressiveAds", SignatureSeverity.MEDIUM, "Aggressive adware — shows full-screen ads outside the app"),
        MalwareSignature("com.spy.tracker.", "Stalkerware", SignatureSeverity.CRITICAL, "Stalkerware — secretly monitors user activity"),
        MalwareSignature("com.fakevpn.", "FakeVPN", SignatureSeverity.HIGH, "Fake VPN — claims to provide VPN but harvests data"),
        MalwareSignature("com.fleeceware.", "Fleeceware", SignatureSeverity.MEDIUM, "Fleeceware — charges excessive subscription fees for basic functionality")
    )

    private val familyMap: Map<String, MalwareSignature> = buildMap {
        exactPackages.forEach { pkg ->
            val family = when {
                pkg.contains("hiddenads") || pkg.contains("flavor") || pkg.contains("lovely.bouncy") -> "HiddenAds"
                pkg.contains("joker") || pkg in setOf("com.imagecompress.android", "com.contact.withme.texts", "com.hmvoice.friendsms", "com.relax.relaxation.attitudes", "com.cheery.message.sendsms", "com.peason.lovinglovemessage", "com.file.recovefiles", "com.LPlocker.lockapps", "com.remindme.alram", "com.training.memorygame") -> "Joker"
                pkg.contains("harly") || pkg in setOf("com.binbin.flashlight", "com.launcher.overlay.live", "com.great.live.wallpaper", "com.chargeanim.battery.wallpaper", "com.amazing.ringtone.music", "com.neon.live.keyboard") -> "Harly"
                pkg.contains("autolycos") || pkg in setOf("com.vlog.star.video.editor", "com.creative.3d.launcher", "com.funny.camera.faces", "com.gif.emoji.keyboard", "com.wow.beauty.camera", "com.freeglow.camera.beauty", "com.coco.camera.effect", "com.tezza.photo.editor") -> "Autolycos"
                pkg.contains("anatsa") || pkg in setOf("com.az.cleanerapp.es", "com.securityprotect.antivirus", "com.document.reader.pro.pdf", "com.qrcode.barcode.scanner.reader") -> "Anatsa"
                pkg.contains("godfather") || pkg in setOf("com.crypto.exchange.wallet", "com.banking.secure.login") -> "Godfather"
                pkg.contains("sharkbot") || pkg in setOf("com.antivirus.supercleaner", "com.file.manager.fileexplorer", "com.mr.phone.cleaner.boost", "com.recovering.documents.files") -> "SharkBot"
                pkg.contains("spy") || pkg.contains("tracker") -> "Stalkerware"
                else -> "Unknown"
            }
            val severity = when (family) {
                "Joker", "Anatsa", "Godfather", "Stalkerware" -> SignatureSeverity.CRITICAL
                "SharkBot", "HiddenAds", "Harly", "Autolycos" -> SignatureSeverity.HIGH
                else -> SignatureSeverity.MEDIUM
            }
            put(pkg, MalwareSignature(pkg, family, severity, "$family malware family"))
        }
    }

    fun checkPackageName(packageName: String): SignatureMatch? {
        // Exact match
        familyMap[packageName]?.let {
            return SignatureMatch(it, MatchType.EXACT_PACKAGE)
        }
        // Pattern match
        for (sig in patternSignatures) {
            if (packageName.startsWith(sig.packagePattern)) {
                return SignatureMatch(sig, MatchType.PATTERN_MATCH)
            }
        }
        return null
    }

    fun checkCertificate(packageName: String, pm: PackageManager): SignatureMatch? {
        val fingerprint = getCertFingerprint(packageName, pm) ?: return null
        if (fingerprint in knownBadCertHashes) {
            return SignatureMatch(
                MalwareSignature(packageName, "Unknown", SignatureSeverity.CRITICAL, "Signing certificate matches known malware"),
                MatchType.CERTIFICATE_MATCH
            )
        }
        return null
    }

    fun getSignatureInfo(packageName: String): PackageSignatureInfo? {
        val pm = context.packageManager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = pkgInfo.signingInfo ?: return null
                val cert = if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners.firstOrNull()
                } else {
                    signingInfo.signingCertificateHistory?.firstOrNull()
                } ?: return null

                val certBytes = cert.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(certBytes)
                val fingerprint = digest.joinToString(":") { "%02X".format(it) }

                val x509 = java.security.cert.CertificateFactory.getInstance("X.509")
                    .generateCertificate(certBytes.inputStream()) as java.security.cert.X509Certificate
                PackageSignatureInfo(
                    issuer = x509.issuerDN.name,
                    subject = x509.subjectDN.name,
                    serial = x509.serialNumber.toString(16),
                    sha256Fingerprint = fingerprint,
                    signatureScheme = if (signingInfo.hasMultipleSigners()) "v2+" else "v1"
                )
            } else {
                @Suppress("DEPRECATION")
                val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                val sig = pkgInfo.signatures?.firstOrNull() ?: return null
                val certBytes = sig.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(certBytes)
                val fingerprint = digest.joinToString(":") { "%02X".format(it) }

                val x509 = java.security.cert.CertificateFactory.getInstance("X.509")
                    .generateCertificate(certBytes.inputStream()) as java.security.cert.X509Certificate
                PackageSignatureInfo(
                    issuer = x509.issuerDN.name,
                    subject = x509.subjectDN.name,
                    serial = x509.serialNumber.toString(16),
                    sha256Fingerprint = fingerprint,
                    signatureScheme = "v1"
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getCertFingerprint(packageName: String, pm: PackageManager): String? {
        return try {
            val certBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = pkgInfo.signingInfo ?: return null
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners.firstOrNull()?.toByteArray()
                } else {
                    signingInfo.signingCertificateHistory?.firstOrNull()?.toByteArray()
                }
            } else {
                @Suppress("DEPRECATION")
                val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                pkgInfo.signatures?.firstOrNull()?.toByteArray()
            } ?: return null
            val md = MessageDigest.getInstance("SHA-256")
            md.digest(certBytes).joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        // Placeholder for known-bad certificate SHA-256 hashes
        // In production, this would be populated with verified malicious cert hashes
        private val knownBadCertHashes = hashSetOf<String>()
    }
}
