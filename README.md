<p align="center">
  <img src="assets/banner.png" alt="Blueth Guard" width="600" />
</p>

<h1 align="center">🛡️ Blueth Guard</h1>
<p align="center">
  <strong>All-in-one Android security, optimizer & privacy guardian.</strong><br>
  Offline-first. Open-source. No ads. No BS.
</p>

<p align="center">
  <a href="https://github.com/LaunchDay-Studio-Inc/blueth-guard/releases"><img src="https://img.shields.io/github/v/release/LaunchDay-Studio-Inc/blueth-guard?style=flat-square" alt="Release" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square" alt="License" /></a>
  <a href="https://discord.gg/bJDGXc4DvW"><img src="https://img.shields.io/discord/1234567890?label=Discord&style=flat-square&color=5865F2" alt="Discord" /></a>
</p>

---

## The Problem

Every "cleaner" and "antivirus" app on the Play Store is:
- 🗑️ Loaded with ads and upsells
- 🔓 Requires sketchy permissions (accessibility, device admin)
- 📡 Phones home constantly
- 🤡 Does nothing useful (fake scans, placebo "boost" buttons)
- 🔋 Drains more battery than it saves

**Blueth Guard is different.** One app that actually does what all those apps pretend to do.

## Features

### 🔍 Security Scanner (offline)
- **ML-based malware detection** — on-device machine learning, no cloud needed
- **APK hash verification** — checks against known malware signature databases
- **Permission auditor** — flags apps with dangerous permission combos
- **Device admin checker** — shows which apps have admin access
- **Sideload tracker** — shows which apps came from outside Play Store
- **Tracker detection** — identifies embedded trackers using the Exodus Privacy database

### 🧹 Smart Optimizer
- **Intelligent background kill** — only kills actual resource hogs, not everything blindly
- **Per-app cache cleaner** — with size breakdown and one-tap clear
- **Storage analyzer** — visual treemap of what's eating your space
- **Duplicate file finder** — photos, downloads, media

### 🔒 Privacy Dashboard
- **Permission heatmap** — one screen showing every dangerous permission and who has it
- **Network monitor** — which apps are sending data and where
- **Clipboard monitor** — detects apps reading clipboard silently
- **Recent access log** — camera, mic, location access history

### ⚡ Battery Guardian
- **Wakelock detector** — which apps keep your phone awake
- **Background service monitor** — shows persistent services draining battery
- **Battery drain ranking** — with actual mAh estimates
- **Bloatware identifier** — pre-installed apps you can safely disable

### 🛡️ Real-time Protection (optional)
- New app install scanner
- USB debug detection alert
- Unknown sources warning
- Accessibility service monitor

## Screenshots

> *Coming soon — app is in early development*

## Architecture

```
blueth-guard/
├── app/                          # Main Android application
│   ├── src/main/
│   │   ├── java/.../bluethguard/
│   │   │   ├── scanner/          # Malware detection engine
│   │   │   │   ├── MLEngine.kt           # ML-based APK analysis
│   │   │   │   ├── SignatureDB.kt         # Offline signature database
│   │   │   │   ├── PermissionAuditor.kt   # Permission risk scoring
│   │   │   │   └── TrackerDetector.kt     # Exodus Privacy integration
│   │   │   ├── optimizer/        # System optimization
│   │   │   │   ├── ProcessManager.kt      # Smart background kill
│   │   │   │   ├── CacheCleaner.kt        # Per-app cache management
│   │   │   │   ├── StorageAnalyzer.kt     # Space usage treemap
│   │   │   │   └── DuplicateFinder.kt     # File deduplication
│   │   │   ├── privacy/          # Privacy monitoring
│   │   │   │   ├── PermissionHeatmap.kt   # Visual permission overview
│   │   │   │   ├── NetworkMonitor.kt      # Traffic analysis
│   │   │   │   └── ClipboardGuard.kt      # Clipboard access detection
│   │   │   ├── battery/          # Battery optimization
│   │   │   │   ├── WakelockDetector.kt    # Wakelock analysis
│   │   │   │   ├── ServiceMonitor.kt      # Background service tracking
│   │   │   │   └── DrainRanker.kt         # Battery usage ranking
│   │   │   ├── protection/       # Real-time protection
│   │   │   │   ├── InstallGuard.kt        # New install scanner
│   │   │   │   ├── DebugDetector.kt       # USB debug alerts
│   │   │   │   └── AccessibilityWatcher.kt
│   │   │   ├── ui/               # Jetpack Compose UI
│   │   │   │   ├── theme/
│   │   │   │   ├── screens/
│   │   │   │   ├── components/
│   │   │   │   └── navigation/
│   │   │   └── data/             # Local database & preferences
│   │   │       ├── db/
│   │   │       └── prefs/
│   │   └── res/
│   └── build.gradle.kts
├── scanner-engine/               # Standalone scanner module (reusable)
├── tracker-db/                   # Exodus Privacy tracker database
├── docs/                         # Documentation
└── build.gradle.kts
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| AV Engine | On-device ML (TFLite) + signature DB |
| Tracker DB | Exodus Privacy list (offline, weekly updates) |
| Database | Room |
| DI | Hilt |
| Min SDK | Android 8.0+ (API 26) |
| Target SDK | Android 15 (API 35) |

## Building from Source

```bash
git clone https://github.com/LaunchDay-Studio-Inc/blueth-guard.git
cd blueth-guard
./gradlew assembleDebug
```

## Roadmap

See [PLAN.md](PLAN.md) for the full development plan.

### Phase 1 — Foundation (Week 1-2)
- [ ] Project scaffold (Compose, Hilt, Room, Navigation)
- [ ] App list with permission analysis
- [ ] Basic permission auditor
- [ ] Storage analyzer

### Phase 2 — Security Engine (Week 3-4)
- [ ] ML malware scanner (port LibreAV engine)
- [ ] Tracker detection (Exodus Privacy DB)
- [ ] Sideload source tracker
- [ ] Device admin checker

### Phase 3 — Optimizer (Week 5-6)
- [ ] Smart process manager
- [ ] Cache cleaner
- [ ] Duplicate file finder
- [ ] Battery drain analyzer

### Phase 4 — Privacy & Protection (Week 7-8)
- [ ] Permission heatmap dashboard
- [ ] Network traffic monitor
- [ ] Real-time install guard
- [ ] Clipboard monitor

### Phase 5 — Polish & Release (Week 9-10)
- [ ] UI polish and animations
- [ ] F-Droid metadata
- [ ] Play Store listing
- [ ] Documentation

## Open Source Credits

Blueth Guard builds on the shoulders of these excellent open-source projects:

| Project | What we use | License |
|---------|-------------|---------|
| [LibreAV (antimalwareapp)](https://github.com/projectmatris/antimalwareapp) | ML malware detection engine | GPL-3.0 |
| [AppManager](https://github.com/MuntashirAkon/AppManager) | Package management patterns | GPL-3.0 |
| [NetGuard](https://github.com/M66B/NetGuard) | Network monitoring patterns | GPL-3.0 |
| [Exodus Privacy](https://exodus-privacy.eu.org/) | Tracker detection database | GPL-3.0 |
| [Hypatia (DivestOS)](https://divestos.org/pages/our_apps#hypatia) | ClamAV scanning patterns | GPL-3.0 |

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting PRs.

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
- 🐛 [Issues](https://github.com/LaunchDay-Studio-Inc/blueth-guard/issues)

---

<p align="center">
  <strong>Made with 🛡️ by <a href="https://launchdaystudio.com">LaunchDay Studio</a></strong><br>
  <em>Security should be free. Privacy shouldn't cost extra.</em>
</p>
