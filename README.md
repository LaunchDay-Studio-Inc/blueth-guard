<p align="center">
  <img src="assets/banner.png" alt="Blueth Guard" width="600" />
</p>

<h1 align="center">Blueth Guard</h1>
<p align="center">
  <strong>Free. Open-source. No ads. No tracking. No BS.</strong><br>
  The security app your phone actually deserves.
</p>

<p align="center">
  <a href="https://blueth.online/products/guard"><img src="https://img.shields.io/badge/⬇_Download_APK-blueth.online-00C853?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" /></a>
  &nbsp;
  <a href="https://play.google.com/store/apps/details?id=com.blueth.guard"><img src="https://img.shields.io/badge/Google_Play-Internal_Testing-414141?style=for-the-badge&logo=google-play&logoColor=white" alt="Play Store" /></a>
</p>

<p align="center">
  <a href="https://github.com/LaunchDay-Studio-Inc/blueth-guard/releases/latest"><img src="https://img.shields.io/github/v/release/LaunchDay-Studio-Inc/blueth-guard?style=flat-square&label=Version" alt="Version" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square" alt="License" /></a>
  <a href="https://discord.gg/bJDGXc4DvW"><img src="https://img.shields.io/badge/Discord-Community-5865F2?style=flat-square&logo=discord&logoColor=white" alt="Discord" /></a>
  <a href="https://github.com/LaunchDay-Studio-Inc/blueth-guard/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/LaunchDay-Studio-Inc/blueth-guard/build.yml?style=flat-square&label=Build" alt="Build" /></a>
</p>

---

## Why Blueth Guard?

Let's be real. Every "antivirus" and "cleaner" app on the Play Store is the same story:

- Fake scans that "find" threats to scare you into paying
- Full-screen ads every 30 seconds
- "Boost" buttons that do literally nothing
- Premium subscriptions for features that should be free
- They collect more of your data than the malware they claim to block

**We got tired of it.** So we built Blueth Guard — one app that actually does what all those apps pretend to do. No ads. No subscriptions. No data collection. Just real protection.

Everything runs locally on your device. Nothing is sent anywhere. Ever. Check the source code yourself — it's right here.

---

## Download

### Direct APK (Recommended)
Download the latest APK directly — no Play Store account needed:

**[⬇️ Download from blueth.online](https://blueth.online/products/guard)**

Or grab it from [GitHub Releases](https://github.com/LaunchDay-Studio-Inc/blueth-guard/releases/latest).

1. Download `blueth-guard-x.x.x.apk`
2. Tap to install (allow "Unknown sources" if prompted)
3. Open and follow the setup — takes 30 seconds
4. That's it. No account. No sign-up. No email.

### Google Play Store (Internal Testing)
Currently in internal testing. Join the test track:

**[🧪 Join Internal Testing on Google Play](https://play.google.com/store/apps/details?id=com.blueth.guard)**

The Play Store version is identical to the APK — same code, same features, same privacy.

### Requirements
- Android 8.0+ (API 26)
- ~15 MB install size
- No root needed
- No internet required (works fully offline)

> **Beta** — Core features are stable. Rough edges exist. [Report bugs here.](https://github.com/LaunchDay-Studio-Inc/blueth-guard/issues)

---

## What It Actually Does

### 🛡️ Security Scanner
Not a fake progress bar — a real multi-factor threat engine.

- **Deep File Scanner** — scans your entire storage for malware, corrupted files, suspicious APKs, and double-extension tricks (`.jpg.exe`)
- **App Threat Analysis** — multi-factor scoring combining permissions, embedded trackers, APK signatures, and behavioral metadata
- **Malware Signature Database** — bundled + auto-updating hash database checked against every file and APK
- **Tracker Detection** — identifies 200+ embedded advertising and analytics trackers via the Exodus Privacy database
- **Permission Auditor** — flags dangerous permission combinations with plain-English risk explanations
- **Sideload Detector** — shows which apps were installed outside the Play Store
- **Device Admin Checker** — reveals apps with hidden device administrator access
- **Scheduled Scans** — daily, weekly, or monthly automatic scans via WorkManager

### 🌐 Web Protection
Local DNS-level blocking — no remote servers, no data leaves your device.

- **Phishing & Malware Blocking** — local VPN intercepts DNS queries and blocks known malicious domains
- **Wi-Fi Security Check** — detects open networks, WEP encryption, and privacy-hostile DNS servers
- **Zero Data Collection** — the VPN tunnel is local-only. No traffic is proxied, logged, or sent anywhere.
- **Real-Time Stats** — see how many malicious sites were blocked today

### 🔒 Anti-Theft
Your phone. Your data. Keep it that way.

- **Remote Lock** — instantly lock your device with one tap
- **Sound Alarm** — blast a max-volume alarm even on silent mode (30 seconds, impossible to miss)
- **Location Tracking** — shows last known GPS location with a Google Maps link
- **Emergency Wipe** — nuclear option behind double confirmation — wipes all data if your device is stolen
- **Device Admin** — secure lock and wipe via Android's official Device Admin API

### 📊 Privacy Dashboard
Know exactly what your apps are doing behind your back.

- **Privacy Score** — 4-factor scoring (permissions 40%, trackers 20%, network 20%, install source 20%)
- **Permission Monitor** — tracks permission changes over time with snapshot diffing
- **Network Monitor** — per-app data usage with suspicious activity flagging (upload > 3x download = red flag)
- **Clipboard Guard** — detects apps silently reading your clipboard (never logged, memory-only)
- **Install Guard** — automatically scans every new app install and alerts you immediately
- **Actionable Recommendations** — every privacy issue has a "Fix Now" button that takes you straight to the right settings page

### ⚡ Smart Optimizer
Actual optimization. Not a fake "boost" animation.

- **App Hibernation** — force-stop background resource hogs on demand or automatically on screen-off / schedule
- **Process Manager** — kills actual resource hogs, not everything blindly. Shows real RAM freed.
- **Cache Cleaner** — per-app cache breakdown with honest behavior (Android won't let third-party apps clear other apps' caches — we tell you that upfront and open the right settings page)
- **Storage Analyzer** — visual breakdown of what's consuming your space
- **Duplicate Finder** — MD5-based file deduplication, scoped storage aware
- **Bloatware Identifier** — 100+ known bloatware entries across Samsung, OnePlus, Xiaomi, Huawei, Google, and carriers
- **Transparent Cleaning** — you see exactly what will be deleted before anything is removed

### 🔋 Battery Guardian
Real battery intelligence, not a percentage with a green circle.

- **Battery Health Tracking** — temperature, voltage, charge cycles, and degradation trends over time
- **Wakelock Detector** — which apps keep your phone awake and drain your battery
- **Background Service Monitor** — shows persistent services with accurate running times
- **Drain Ranking** — per-app mAh estimates based on actual usage data
- **Charge Guard** — alerts you at 80% (configurable) to unplug and preserve battery longevity. Detects overnight charging.
- **Automated Power Profiles** — Normal, Power Saver, and Ultra Saver modes that auto-activate at configurable battery thresholds
- **Battery History** — 30-day snapshot history with charging pattern analysis
- **Honest Disclaimers** — we tell you what Android can and can't measure. No fake "battery health 100%" claims.

### 🛡️ Real-Time Protection
Always watching. Never draining.

- **New App Scanner** — auto-scans every app install in real-time
- **USB Debug Detection** — alerts when developer options or USB debugging is enabled
- **Unknown Sources Warning** — notifies when sideloading is enabled
- **Accessibility Monitor** — flags apps with suspicious accessibility service access
- **App Integrity Checks** — root, emulator, and tampering detection
- **Low Resource Design** — the background service uses minimal CPU and battery

---

## Privacy Promise

```
We collect:        Nothing.
We send:           Nothing.
We track:          Nothing.
We sell:           Nothing.
Ads:               None.
Analytics:         None.
Firebase:          None.
Third-party SDKs:  None that collect data.
```

Everything runs on your device. The app works fully offline. Network permission exists only for optional malware signature database updates — and you can deny it.

Read the full [Privacy Policy](PRIVACY_POLICY.md). It's short because there's nothing to disclose.

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 100% |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Hilt DI |
| Database | Room (privacy events, battery snapshots, scan history) |
| Scanner | Heuristic multi-factor engine + signature DB |
| Web Protection | Local VPN DNS filter (no remote server) |
| Tracker DB | Exodus Privacy list (offline, bundled) |
| Background | WorkManager + Foreground Service |
| Widget | Glance (Material 3) |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 16 (API 36) |

---

## Permissions — And Why

Every permission has a reason. No permission is used to collect your data.

| Permission | Why |
|---|---|
| `QUERY_ALL_PACKAGES` | List installed apps for security scanning |
| `INTERNET` | Optional malware signature database updates (works without it) |
| `ACCESS_NETWORK_STATE` | Check connectivity before signature updates |
| `REQUEST_DELETE_PACKAGES` | Let you uninstall detected bloatware |
| `PACKAGE_USAGE_STATS` | Monitor app usage for battery and network analysis |
| `FOREGROUND_SERVICE` | Real-time protection background service |
| `KILL_BACKGROUND_PROCESSES` | Smart process optimizer and app hibernation |
| `POST_NOTIFICATIONS` | Security alerts and scan results |
| `MANAGE_EXTERNAL_STORAGE` | Deep file scanner (scan all directories for threats) |
| `READ_EXTERNAL_STORAGE` | File scanning on older Android versions |
| `ACCESS_FINE_LOCATION` | Anti-theft device location tracking |
| `ACCESS_COARSE_LOCATION` | Fallback location for anti-theft |
| `ACCESS_WIFI_STATE` | Wi-Fi security analysis |
| `VIBRATE` | Anti-theft alarm |
| `BIND_VPN_SERVICE` | Local DNS-based web protection (no remote server) |

---

## Building from Source

```bash
git clone https://github.com/LaunchDay-Studio-Inc/blueth-guard.git
cd blueth-guard
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/
```

For signed release builds:
```bash
./gradlew assembleRelease   # → signed APK (direct install)
./gradlew bundleRelease     # → signed AAB (Play Store upload)
```

---

## Open Source Credits

| Project | Usage | License |
|---------|-------|---------|
| [Exodus Privacy](https://exodus-privacy.eu.org/) | Tracker detection database | GPL-3.0 |
| [LibreAV](https://github.com/projectmatris/antimalwareapp) | Malware detection patterns | GPL-3.0 |
| [AppManager](https://github.com/MuntashirAkon/AppManager) | Package management patterns | GPL-3.0 |
| [NetGuard](https://github.com/M66B/NetGuard) | Network monitoring patterns | GPL-3.0 |
| [Hypatia](https://divestos.org/pages/our_apps#hypatia) | ClamAV scanning patterns | GPL-3.0 |

---

## Contributing

Contributions welcome! Read [CONTRIBUTING.md](CONTRIBUTING.md) first.

Found a bug? [Open an issue.](https://github.com/LaunchDay-Studio-Inc/blueth-guard/issues)

---

## License

GPL-3.0 — see [LICENSE](LICENSE)

---

## Links

- **[blueth.online](https://blueth.online)** — Website
- **[Download](https://blueth.online/products/guard)** — Get the APK
- **[Discord](https://discord.gg/bJDGXc4DvW)** — Community & support
- **[Issues](https://github.com/LaunchDay-Studio-Inc/blueth-guard/issues)** — Bug reports
- **[Changelog](CHANGELOG.md)** — Version history
- **[Privacy Policy](PRIVACY_POLICY.md)** — Read it, it's short

---

<p align="center">
  <strong>Made by <a href="https://launchdaystudio.com">LaunchDay Studio</a></strong><br>
  <em>Security should be free. Privacy shouldn't cost extra.</em><br><br>
  If this app helped you, ⭐ the repo and tell a friend. That's all we ask.
</p>
