<p align="center">
  <img src="assets/banner.png" alt="Blueth Guard" width="600" />
</p>

<h1 align="center">🛡️ Blueth Guard</h1>
<p align="center">
  <strong>All-in-one Android security, optimizer & privacy guardian.</strong><br>
  Offline-first. Open-source. No ads. No BS.
</p>

<p align="center">
  <a href="https://github.com/LaunchDay-Studio-Inc/blueth-guard/releases/latest"><img src="https://img.shields.io/github/v/release/LaunchDay-Studio-Inc/blueth-guard?style=flat-square&label=Download" alt="Download" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square" alt="License" /></a>
  <a href="https://discord.gg/bJDGXc4DvW"><img src="https://img.shields.io/badge/Discord-Join-5865F2?style=flat-square&logo=discord&logoColor=white" alt="Discord" /></a>
  <a href="https://github.com/LaunchDay-Studio-Inc/blueth-guard/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/LaunchDay-Studio-Inc/blueth-guard/build.yml?style=flat-square&label=Build" alt="Build" /></a>
</p>

---

## 📥 Download & Install

<table>
<tr>
<td width="80" align="center">📱</td>
<td>

**[⬇️ Download Latest APK](https://github.com/LaunchDay-Studio-Inc/blueth-guard/releases/latest)**

1. Download `blueth-guard-x.x.x.apk` from the latest release
2. Open the APK on your Android device
3. If prompted, allow "Install from unknown sources" for your browser/file manager
4. Done — no account, no sign-up, no permissions asked until you need them

**Requirements:** Android 8.0+ (API 26) • ~15MB • No root needed

</td>
</tr>
</table>

> **🔨 Early Alpha** — The app is under active development. Core features work, but expect rough edges. [Report bugs here.](https://github.com/LaunchDay-Studio-Inc/blueth-guard/issues)

---

## The Problem

Every "cleaner" and "antivirus" app on the Play Store is:
- 🗑️ Loaded with ads and upsells
- 🔓 Requires sketchy permissions (accessibility, device admin)
- 📡 Phones home constantly
- 🤡 Does nothing useful (fake scans, placebo "boost" buttons)
- 🔋 Drains more battery than it saves

**Blueth Guard is different.** One app that actually does what all those apps pretend to do — and nothing else.

## Features

### 🔍 Security Scanner (offline)
- **ML-based malware detection** — on-device machine learning, no cloud needed
- **APK hash verification** — checks against known malware signature databases
- **Permission auditor** — flags apps with dangerous permission combos
- **Device admin checker** — shows which apps have admin access
- **Sideload tracker** — shows which apps came from outside Play Store
- **Tracker detection** — identifies 200+ embedded trackers using the Exodus Privacy database

### 🧹 Smart Optimizer
- **Intelligent background kill** — only kills actual resource hogs, not everything blindly
- **Per-app cache cleaner** — with size breakdown and one-tap clear
- **Storage analyzer** — visual breakdown of what's eating your space
- **Duplicate file finder** — MD5-based, scoped storage aware
- **Bloatware identifier** — 100+ entries across Samsung, OnePlus, Xiaomi, Huawei, Google, carriers

### 🔒 Privacy Dashboard
- **Privacy score** — 4-factor scoring (permissions 40%, trackers 20%, network 20%, source 20%)
- **Permission monitor** — snapshot diffing and timeline tracking
- **Network monitor** — per-app data usage via NetworkStatsManager, suspicious flagging
- **Clipboard guard** — detects apps reading clipboard silently (memory-only, never persisted)
- **Install guard** — auto-scans new installs and notifies you

### ⚡ Battery Guardian
- **Wakelock detector** — which apps keep your phone awake
- **Background service monitor** — shows persistent services draining battery
- **Battery drain ranking** — with actual mAh estimates

### 🛡️ Real-time Protection
- New app install scanner with notification
- USB debug detection alert
- Unknown sources warning
- Accessibility service monitor

## Screenshots

> *Coming soon — app is in active development*

## Development Status

| Phase | What | Status |
|-------|------|--------|
| **1 — Foundation** | Project scaffold, theme, navigation, CI | ✅ Complete |
| **2 — Security** | Scanner, tracker detection, sideload detector, device admin | ✅ Complete |
| **3 — Optimizer** | Cache, processes, duplicates, bloatware, storage | ✅ Complete |
| **4 — Privacy** | Room DB, permission monitor, network monitor, clipboard, install guard, privacy scorer | ✅ Complete |
| **5 — Battery** | Wakelock deep scan, service monitor, drain ranking, battery health | 🔜 Next |
| **6 — Polish** | UI animations, onboarding, F-Droid, Play Store | ⬜ Planned |

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 100% |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Hilt DI |
| Database | Room (privacy events, scan history) |
| AV Engine | On-device ML (TFLite) + signature DB |
| Tracker DB | Exodus Privacy list (offline) |
| Min SDK | Android 8.0+ (API 26) |
| Target SDK | Android 15 (API 35) |

## Building from Source

```bash
git clone https://github.com/LaunchDay-Studio-Inc/blueth-guard.git
cd blueth-guard
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/
```

## Architecture

```
blueth-guard/
├── app/src/main/java/com/blueth/guard/
│   ├── scanner/          # Security scanning engine (8 files)
│   │   ├── MLEngine.kt, SignatureDB.kt, SecurityScanner.kt
│   │   ├── PermissionAuditor.kt, PermissionRiskScorer.kt
│   │   ├── TrackerDetector.kt, SideloadDetector.kt
│   │   └── DeviceAdminChecker.kt
│   ├── optimizer/        # System optimization (5 files)
│   │   ├── CacheCleaner.kt, ProcessManager.kt
│   │   ├── DuplicateFinder.kt, StorageAnalyzer.kt
│   │   └── BloatwareIdentifier.kt
│   ├── privacy/          # Privacy monitoring (8 files)
│   │   ├── PermissionMonitor.kt, PermissionHeatmap.kt
│   │   ├── NetworkMonitor.kt, ClipboardGuard.kt
│   │   ├── PrivacyScorer.kt, InstallGuardPrivacy.kt
│   │   ├── InstallGuardReceiver.kt, InstallAction.kt
│   ├── battery/          # Battery analysis (3 files)
│   │   ├── WakelockDetector.kt, ServiceMonitor.kt
│   │   └── DrainRanker.kt
│   ├── protection/       # Real-time protection (3 files)
│   │   ├── InstallGuard.kt, DebugDetector.kt
│   │   └── AccessibilityWatcher.kt
│   ├── data/             # Room DB + models (12 files)
│   │   ├── local/        # Database, DAOs, entities
│   │   ├── model/        # Data models
│   │   └── repository/   # App data repository
│   ├── ui/               # Compose UI (13 files)
│   │   ├── screens/      # 5 main screens
│   │   ├── viewmodel/    # 4 ViewModels
│   │   ├── navigation/   # Bottom nav
│   │   └── theme/        # Colors, typography
│   └── di/               # Hilt modules
└── 55 Kotlin files total
```

## Open Source Credits

| Project | What we use | License |
|---------|-------------|---------|
| [LibreAV](https://github.com/projectmatris/antimalwareapp) | ML malware detection engine | GPL-3.0 |
| [AppManager](https://github.com/MuntashirAkon/AppManager) | Package management patterns | GPL-3.0 |
| [NetGuard](https://github.com/M66B/NetGuard) | Network monitoring patterns | GPL-3.0 |
| [Exodus Privacy](https://exodus-privacy.eu.org/) | Tracker detection database | GPL-3.0 |
| [Hypatia (DivestOS)](https://divestos.org/pages/our_apps#hypatia) | ClamAV scanning patterns | GPL-3.0 |

## Contributing

Contributions welcome! Read [CONTRIBUTING.md](CONTRIBUTING.md) first.

## License

```
Copyright (C) 2026 LaunchDay Studio Inc.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

## Links

- 🌐 [blueth.online](https://blueth.online)
- 💬 [Discord](https://discord.gg/bJDGXc4DvW)
- 🐛 [Report Issues](https://github.com/LaunchDay-Studio-Inc/blueth-guard/issues)
- 📋 [Development Plan](PLAN.md)

---

<p align="center">
  <strong>Made with 🛡️ by <a href="https://launchdaystudio.com">LaunchDay Studio</a></strong><br>
  <em>Security should be free. Privacy shouldn't cost extra.</em>
</p>
