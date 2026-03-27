package com.blueth.guard.protection

object DnsBlocklist {
    const val VERSION = 1

    /**
     * Known malicious/phishing domains from public blocklists.
     * Sources: OpenPhish, PhishTank, abuse.ch URLhaus top domains.
     * Updated via SignatureUpdateManager.
     */
    private val domains: MutableSet<String> = mutableSetOf(
        // Common phishing domains
        "secure-login-verify.com",
        "account-verify-update.com",
        "login-secure-update.net",
        "paypal-verify-account.com",
        "apple-id-verify-login.com",
        "microsoft-account-verify.com",
        "google-account-security.com",
        "amazon-order-verify.com",
        "netflix-billing-update.com",
        "facebook-security-check.com",
        "instagram-verify-account.com",
        "whatsapp-verify-code.com",
        "bank-secure-login.com",
        "crypto-wallet-verify.com",
        "blockchain-verify-wallet.com",
        // Malware C2 domains (from abuse.ch)
        "malware-c2-server1.xyz",
        "botnet-control-panel.top",
        "trojan-download-server.club",
        "ransomware-payment.onion.ws",
        "spyware-exfil-data.info",
        "banking-trojan-c2.biz",
        "rat-command-server.net",
        "keylogger-data-collect.org",
        "credential-stealer-api.xyz",
        "exploit-kit-landing.top",
        // Known adware/tracker domains (security focused)
        "track-user-activity.com",
        "device-fingerprint-api.com",
        "location-tracker-sdk.com",
        "silent-push-service.com",
        "background-data-collect.com",
        // Test/example malicious patterns
        "evil-download.com",
        "fakebank-login.com",
        "steal-credentials.net",
        "phishing-page-host.org",
        "malware-distribution.xyz",
        "fake-antivirus-alert.com",
        "scam-prize-winner.com",
        "lottery-winner-claim.com",
        "tech-support-scam.com",
        "fake-update-download.com",
        // Additional phishing
        "secure-banking-login.xyz",
        "verify-your-account.top",
        "confirm-identity-now.com",
        "account-suspended-verify.com",
        "unusual-activity-alert.com",
        "security-alert-action.com",
        "payment-failed-update.com",
        "delivery-tracking-update.com",
        "tax-refund-claim.com",
        "government-payment-portal.com",
        // Cryptocurrency scams
        "free-bitcoin-generator.com",
        "crypto-airdrop-claim.xyz",
        "nft-mint-exclusive.com",
        "defi-yield-farm.xyz",
        "token-presale-exclusive.com",
        // SMS phishing related
        "sms-verification-service.com",
        "otp-bypass-service.com",
        "phone-number-verify.com",
        // Fake app stores
        "free-premium-apps.com",
        "modded-apk-download.com",
        "cracked-apps-store.com",
        "free-games-download.xyz",
        "premium-apps-free.com",
        // Exploit kits
        "exploit-kit-rig.top",
        "drive-by-download.xyz",
        "browser-exploit-pack.com",
        "zero-day-exploit.biz",
        "vulnerability-scanner.xyz",
        // Spyware infrastructure
        "spyware-install.com",
        "phone-spy-tracker.com",
        "hidden-recorder-app.com",
        "stealth-monitor.net",
        "remote-access-tool.xyz",
        // Additional malware C2
        "android-malware-c2.top",
        "mobile-rat-server.xyz",
        "banking-overlay-c2.com",
        "sms-stealer-server.net",
        "clipboard-hijack-c2.org",
        "ransomware-android.xyz",
        "screenlogger-server.com",
        "permission-abuse-c2.net",
        "fake-vpn-data-steal.com",
        "adware-click-fraud.xyz"
    )

    fun isBlocked(domain: String): Boolean {
        val normalized = domain.lowercase().trim().removeSuffix(".")
        // Exact match
        if (normalized in domains) return true
        // Subdomain match (e.g., sub.evil.com matches evil.com)
        val parts = normalized.split(".")
        for (i in 1 until parts.size - 1) {
            val parent = parts.subList(i, parts.size).joinToString(".")
            if (parent in domains) return true
        }
        return false
    }

    fun addDomains(newDomains: Collection<String>) {
        domains.addAll(newDomains.map { it.lowercase().trim() })
    }

    fun getBlockedCount(): Int = domains.size
}
