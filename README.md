<p align="center">
  <img src="assets/banner.png" alt="Blueth Guard" width="600" />
</p>

<h1 align="center">Blueth Guard</h1>
<p align="center">
  <strong>All-in-one Android security, optimizer & privacy guardian.</strong><br>
  Offline-first. Open-source. No ads. No BS.
</p>

<p align="center">
  <a href="https://blueth.online/products/guard"><img src="https://img.shields.io/badge/Download-APK-00C853?style=for-the-badge&logo=android&logoColor=white" alt="Download" /></a>
  <a href="https://github.com/LaunchDay-Studio-Inc/blueth-guard/releases/latest"><img src="https://img.shields.io/github/v/release/LaunchDay-Studio-Inc/blueth-guard?style=flat-square&label=Version" alt="Version" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square" alt="License" /></a>
  <a href="https://discord.gg/bJDGXc4DvW"><img src="https://img.shields.io/badge/Discord-Join-5865F2?style=flat-square&logo=discord&logoColor=white" alt="Discord" /></a>
  <a href="https://github.com/LaunchDay-Studio-Inc/blueth-guard/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/LaunchDay-Studio-Inc/blueth-guard/build.yml?style=flat-square&label=Build" alt="Build" /></a>
</p>

---

## Download & Install

<table>
<tr>
<td width="80" align="center">📱</td>
<td>

**[⬇️ Download from blueth.online](https://blueth.online/products/guard)**

Or grab the APK directly from [GitHub Releases](https://github.com/LaunchDay-Studio-Inc/blueth-guard/releases/latest).

1. Download `blueth-guard-x.x.x.apk`
2. Open the APK on your Android device
3. Allow "Install from unknown sources" if prompted
4. Done — no account, no sign-up, no data collected

**Requirements:** Android 8.0+ (API 26) | ~15MB | No root needed

</td>
</tr>
</table>

> **Beta** — Core features are stable but expect rough edges. [Report bugs here.](https://github.com/LaunchDay-Studio-Inc/blueth-guard/issues)

---

## The Problem

Every "cleaner" and "antivirus" app on the Play Store is loaded with ads, requires sketchy permissions, phones home constantly, and does nothing useful. Fake scans, placebo "boost" buttons, battery drain worse than what they claim to fix.

**Blueth Guard is different.** One app that actually does what all those apps pretend to do — and nothing else. Fully offline. Zero data collection. Open source.

---

## Features

### Security Scanner
- **Heuristic threat analysis** — multi-factor scoring combining permissions, trackers, signatures, and metadata
- **APK signature verification** — checks against known malware family databases
- **Permission auditor** — flags dangerous permission combinations with risk scoring
- **Tracker detection** — identifies 200+ embedded trackers via Exodus Privacy database
- **Sideload tracker** — shows which apps were installed outside Play Store
- **Device admin checker** — reveals apps with admin access

### Smart Optimizer
- **Intelligent process kill** — only kills actual resource hogs, not everything blindly
- **Per-app cache cleaner** — with size breakdown and one-tap clear
- **Storage analyzer** — visual breakdown of what's consuming space
- **Duplicate file finder** — MD5-based, scoped storage aware
- **Bloatware identifier** — 100+ entries across Samsung, OnePlus, Xiaomi, Huawei, Google, carriers

### Privacy Dashboard
- **Privacy score** — 4-factor scoring (permissions 40%, trackers 20%, network 20%, source 20%)
- **Permission monitor** — snapshot diffing and timeline tracking
- **Network monitor** — per-app data usage with suspicious activity flagging
- **Clipboard guard** — detects apps reading clipboard silently (memory-only, never persisted)
- **Install guard** — auto-scans new installs and notifies you

### Battery Guardian
- **Wakelock detector** — which apps keep your phone awake
- **Background service monitor** — shows persistent services draining battery
- **Battery drain ranking** — with actual mAh estimates
- **Battery health** — temperature and cycle monitoring

### Real-time Protection
- New app install scanner with notification
- USB debug detection alert
- Unknown sources warning
- Accessibility service monitor
- App integrity verification (root, emulator, tampering detection)

### Security Hardening
- Cleartext traffic blocked via network security config
- Backup data excluded from cloud and ADB backups
- Release builds are minified, obfuscated, and signed
- Anti-tampering checks on every protection cycle

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 100% |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Hilt DI |
| Database | Room (privacy events, scan history) |
| Scanner | Heuristic multi-factor engine + signature DB |
| Tracker DB | Exodus Privacy list (offline, bundled) |
| Background | WorkManager + Foreground Service |
| Widget | Glance (Material 3) |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 16 (API 36) |

---

## Building from Source

```bash
git clone https://github.com/LaunchDay-Studio-Inc/blueth-guard.git
cd blueth-guard
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/
```

---

## Permissions

| Permission | Why |
|---|---|
| `QUERY_ALL_PACKAGES` | List installed apps for security scanning |
| `INTERNET` | Optional malware signature database updates |
| `ACCESS_NETWORK_STATE` | Check connectivity before database updates |
| `REQUEST_DELETE_PACKAGES` | Let users uninstall detected bloatware |
| `PACKAGE_USAGE_STATS` | Monitor app usage for battery and network analysis |
| `FOREGROUND_SERVICE` | Real-time protection background service |
| `KILL_BACKGROUND_PROCESSES` | Smart process optimizer |
| `POST_NOTIFICATIONS` | Security alerts and scan result notifications |

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

Contributions welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) first.

---

## License

GPL-3.0 — see [LICENSE](LICENSE)

---

## Links

- [blueth.online](https://blueth.online) — Website
- [Download Blueth Guard](https://blueth.online/products/guard) — Landing page
- [Discord](https://discord.gg/bJDGXc4DvW) — Community
- [Issues](https://github.com/LaunchDay-Studio-Inc/blueth-guard/issues) — Bug reports
- [Changelog](CHANGELOG.md) — Version history

---

<p align="center">
  <strong>Made by <a href="https://launchdaystudio.com">LaunchDay Studio</a></strong><br>
  <em>Security should be free. Privacy shouldn't cost extra.</em>
</p>
