<p align="center">
  <a href="https://blueth.online"><img src="app/src/main/ic_launcher-playstore.png" alt="Blueth Guard" width="160" /></a>
</p>

<h1 align="center"><a href="https://blueth.online">Blueth Guard</a></h1>

<p align="center">
  <strong>Free. Open-source. No ads. No tracking. No BS.</strong><br>
  The security app your phone actually deserves.<br>
  <a href="https://blueth.online">blueth.online</a>
</p>

<br>

<p align="center">
  <a href="https://github.com/LaunchDay-Studio-Inc/blueth-guard/releases/latest"><img src="https://img.shields.io/github/v/release/LaunchDay-Studio-Inc/blueth-guard?include_prereleases&style=for-the-badge&logo=github&logoColor=white&label=Version&color=0A1628" alt="Version" /></a>
  &nbsp;
  <a href="https://github.com/LaunchDay-Studio-Inc/blueth-guard/releases/latest"><img src="https://img.shields.io/badge/⬇_Download_APK-Latest_Release-00C853?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" /></a>
  &nbsp;
  <a href="https://play.google.com/store/apps/details?id=com.blueth.guard"><img src="https://img.shields.io/badge/Google_Play-Internal_Testing-414141?style=for-the-badge&logo=google-play&logoColor=white" alt="Play Store" /></a>
</p>

<p align="center">
  <a href="https://github.com/LaunchDay-Studio-Inc/blueth-guard/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/LaunchDay-Studio-Inc/blueth-guard/build.yml?style=flat-square&label=Build&logo=githubactions&logoColor=white" alt="Build" /></a>
  &nbsp;
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square" alt="License" /></a>
  &nbsp;
  <a href="https://discord.gg/bJDGXc4DvW"><img src="https://img.shields.io/badge/Discord-Community-5865F2?style=flat-square&logo=discord&logoColor=white" alt="Discord" /></a>
  &nbsp;
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 8.0+" />
  &nbsp;
  <img src="https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
</p>

<br>

---

<br>

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

<table>
<tr>
<td width="50%" align="center">

### Direct APK (Recommended)

Download the latest APK directly — no Play Store account needed:

[![Download APK](https://img.shields.io/badge/⬇_Download_APK-Latest_Release-00C853?style=for-the-badge&logo=android&logoColor=white)](https://github.com/LaunchDay-Studio-Inc/blueth-guard/releases/latest)

Or from **[blueth.online](https://blueth.online/products/guard)**

</td>
<td width="50%" align="center">

### Google Play (Internal Testing)

Currently in internal testing. Join the test track:

[![Google Play](https://img.shields.io/badge/Google_Play-Join_Testing-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.blueth.guard)

Same code, same features, same privacy.

</td>
</tr>
</table>

<details>
<summary><strong>Installation Instructions</strong></summary>

<br>

1. Download `blueth-guard-x.x.x.apk` from [GitHub Releases](https://github.com/LaunchDay-Studio-Inc/blueth-guard/releases/latest)
2. Tap to install (allow "Unknown sources" if prompted)
3. Open and follow the setup — takes 30 seconds
4. That's it. No account. No sign-up. No email.

</details>

### Requirements
- Android 8.0+ (API 26)
- ~15 MB install size
- No root needed
- No internet required (works fully offline)

> **v1.3.0-beta** — Core features are stable. Rough edges exist. See [CHANGELOG](CHANGELOG.md) for what's new. [Report bugs here.](https://github.com/LaunchDay-Studio-Inc/blueth-guard/issues)

### What's New in v1.3.0-beta

- **AccessibilityService** — real Force Stop and Cache Clear automation via Android Settings UI
- **Onboarding Accessibility page** — guided setup for enabling accessibility permission
- **DNS blocklist** expanded from ~90 to 2,500+ domains across 20+ threat categories
- **Tracker database** expanded to 200+ signatures across 7 categories
- **VPN packet passthrough fixed** — DNS-only routing eliminates infinite packet loop
- **Anti-theft alarm** now auto-stops after 30 seconds instead of looping indefinitely
- **Remote signature database** populated with real malware packages, patterns, and tracker entries

Full details: [CHANGELOG.md](CHANGELOG.md)

<br>

---

<br>

## What It Actually Does

### 🛡️ Security Scanner
Not a fake progress bar — a real multi-factor threat engine.

- **Deep File Scanner** — scans your entire storage for malware, corrupted files, suspicious APKs, double-extension tricks (`.jpg.exe`), large unused files (>100 MB), stale files (>6 months), and leftover data from uninstalled apps
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
- **Sound Alarm** — blast a max-volume alarm even on silent mode (auto-stops after 30 seconds)
- **Location Tracking** — shows last known GPS location with a Google Maps link
- **Emergency Wipe** — nuclear option behind double confirmation — wipes all data if your device is stolen
- **Device Admin** — secure lock and wipe via Android's official Device Admin API

### 📊 Privacy Dashboard
Know exactly what your apps are doing behind your back.

- **Privacy Score** — 4-factor scoring (permissions 40%, trackers 20%, network 20%, install source 20%) — tap any stat card to see matching apps
- **Permission Monitor** — tracks permission changes over time with snapshot diffing
- **Network Monitor** — per-app data usage with suspicious activity flagging (upload > 3x download = red flag) — tap any app for upload/download detail and quick settings access
- **Clipboard Guard** — detects apps silently reading your clipboard (never logged, memory-only)
- **Install Guard** — automatically scans every new app install and alerts you immediately
- **Actionable Recommendations** — every privacy issue has a "Fix Now" button that takes you straight to the right settings page

### ⚡ Smart Optimizer
Actual optimization. Not a fake "boost" animation.

- **App Hibernation** — force-stop background resource hogs on demand or automatically on screen-off / schedule
- **Process Manager** — kills actual resource hogs, not everything blindly. Uses AccessibilityService for real Force Stop via Settings. Shows real RAM freed with before/after comparison.
- **Cache Cleaner** — per-app cache breakdown with honest behavior. Uses AccessibilityService for real cache clearing via Settings. Clears Blueth Guard's own cache directly; for other apps, explains Android's limitations and opens the right settings page
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

<br>

## Privacy Promise

> **Everything runs on your device. Nothing is sent anywhere. Ever.**

| | |
|:--|:--|
| **We collect** | Nothing |
| **We send** | Nothing |
| **We track** | Nothing |
| **We sell** | Nothing |
| **Ads** | None |
| **Analytics** | None |
| **Firebase** | None |
| **Third-party SDKs** | None that collect data |

Everything runs on your device. The app works fully offline. Network permission exists only for optional malware signature database updates — and you can deny it.

Read the full [Privacy Policy](PRIVACY_POLICY.md). It's short because there's nothing to disclose.

<br>

---

<br>

## Tech Stack

| Component | Technology |
| :-- | :-- |
| Language | Kotlin 2.1.10 (100%) |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Hilt DI (Dagger 2.56.1) |
| Build | AGP 8.9.3, KSP 2.1.10 |
| Database | Room (privacy events, battery snapshots, scan history) |
| Scanner | Heuristic multi-factor engine + signature DB |
| Web Protection | Local VPN DNS filter (no remote server) |
| Tracker DB | Exodus Privacy list (offline, bundled) |
| Background | WorkManager + Foreground Service |
| Widget | Glance (Material 3) |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 16 (API 36) |

<br>

---

<br>

## Permissions — And Why

Every permission has a reason. No permission is used to collect your data.

<details>
<summary><strong>View all permissions</strong></summary>

<br>

| Permission | Why |
| :-- | :-- |
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
| `BIND_ACCESSIBILITY_SERVICE` | Automate Force Stop and Cache Clear via Settings UI |

</details>

<br>

---

<br>

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

<br>

---

<br>

## Open Source Credits

| Project | Usage | License |
| :-- | :-- | :-- |
| [Exodus Privacy](https://exodus-privacy.eu.org/) | Tracker detection database | GPL-3.0 |
| [LibreAV](https://github.com/projectmatris/antimalwareapp) | Malware detection patterns | GPL-3.0 |
| [AppManager](https://github.com/MuntashirAkon/AppManager) | Package management patterns | GPL-3.0 |
| [NetGuard](https://github.com/M66B/NetGuard) | Network monitoring patterns | GPL-3.0 |
| [Hypatia](https://divestos.org/pages/our_apps#hypatia) | ClamAV scanning patterns | GPL-3.0 |

<br>

---

<br>

## Contributing

Contributions welcome! Read [CONTRIBUTING.md](CONTRIBUTING.md) first.

Found a bug? [Open an issue.](https://github.com/LaunchDay-Studio-Inc/blueth-guard/issues)

<br>

---

## License

GPL-3.0 — see [LICENSE](LICENSE)

<br>

---

<br>

<p align="center">
  <a href="https://blueth.online"><img src="https://img.shields.io/badge/Website-blueth.online-0A1628?style=for-the-badge&logoColor=white" alt="Website" /></a>
  &nbsp;
  <a href="https://github.com/LaunchDay-Studio-Inc/blueth-guard/releases/latest"><img src="https://img.shields.io/badge/⬇_Download-APK-00C853?style=for-the-badge&logo=android&logoColor=white" alt="Download" /></a>
  &nbsp;
  <a href="https://discord.gg/bJDGXc4DvW"><img src="https://img.shields.io/badge/Discord-Join-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Discord" /></a>
  &nbsp;
  <a href="https://github.com/LaunchDay-Studio-Inc/blueth-guard/issues"><img src="https://img.shields.io/badge/Bugs-Report-FF6D00?style=for-the-badge&logo=github&logoColor=white" alt="Issues" /></a>
  &nbsp;
  <a href="CHANGELOG.md"><img src="https://img.shields.io/badge/Changelog-History-607D8B?style=for-the-badge" alt="Changelog" /></a>
</p>

<br>

<p align="center">
  <strong>Made by <a href="https://launchdaystudio.com">LaunchDay Studio</a></strong><br>
  <em>Security should be free. Privacy shouldn't cost extra.</em><br><br>
  If this app helped you, star the repo and tell a friend. That's all we ask.
</p>
