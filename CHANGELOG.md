# Changelog

## [1.0.0-beta3] — 2026-03-28

### New Features
- **Deep File Scanner** — scans entire device storage for malware hashes, corrupted files, suspicious APKs, and double-extension tricks
- **Web Protection** — local VPN DNS filter blocks known phishing and malware domains without sending data off-device
- **Wi-Fi Security Checker** — detects open networks, WEP encryption, and privacy-hostile DNS
- **Anti-Theft Suite** — device lock, max-volume alarm, GPS location tracking, and emergency data wipe via Device Admin
- **App Hibernation** — force-stop background resource hogs on demand or automatically on screen-off / schedule
- **Battery History & Degradation Tracking** — 30-day charge cycle estimation, temperature trends, charging pattern analysis
- **Automated Power Profiles** — Normal, Power Saver, Ultra Saver modes auto-activate at configurable battery thresholds
- **Charge Guard** — smart alerts at 80% charge (configurable) and overnight charging detection
- **Transparent Deep Cleaning** — preview exactly what will be deleted before any cleaning action

### Bug Fixes & Accuracy
- Fixed battery service running time showing impossible durations (400,000+ hours)
- Fixed battery percentage using raw value instead of system-displayed value
- Fixed network tab showing fake/stale data when Usage Access permission not granted
- Fixed home button not responding to clicks when already on Home screen
- Battery health score now includes honest disclaimer about measurement limitations
- Cache cleaning now honestly explains Android's limitations instead of pretending to work

### Improvements
- Security scan results are now fully interactive with Uninstall, Force Stop, Permissions, and Disable actions
- Privacy recommendations now have prominent "Fix Now" and "Revoke Permission" action buttons
- Wakelock and drain items now have "Restrict Background" and "Kill All Critical" actions
- Optimizer Smart Boost now uses real killBackgroundProcesses instead of placeholder animation
- Added lint baseline for clean CI builds

## [1.0.0-beta1] — 2026-03-28

### Security Hardening
- Added network security config (cleartext traffic blocked)
- Added backup exclusion rules (scan data never backed up)
- Added anti-tampering / integrity checker (root, emulator, debuggable, repackage detection)
- Enhanced ProGuard rules (log stripping, class repackaging, obfuscation)
- Release APK signing via CI

### Accuracy
- Renamed "ML-based detection" to "heuristic threat analysis" (honest labeling — the engine uses weighted multi-factor scoring, not a TFLite model)

### Existing Features (from alpha)
- Security scanner with 200+ tracker detection (Exodus Privacy database)
- Permission auditor with risk scoring
- APK signature verification against known malware families
- Smart optimizer (cache, processes, duplicates, bloatware)
- Privacy dashboard with permission monitoring, network tracking, clipboard guard
- Battery guardian with wakelock detection and drain ranking
- Real-time protection service (new install scanning, USB debug alerts)
- Onboarding flow with permission setup
- Home screen widget (Glance)
- Scheduled scans via WorkManager
- Scan report export (JSON)
- AMOLED dark theme

## [1.0.0-alpha4] — 2026-03-23
- Widget, DataStore settings, notification channels, export, F-Droid prep

## [1.0.0-alpha3] — 2026-03-23
- Battery guardian, wakelock detection, drain ranking

## [1.0.0-alpha2] — 2026-03-23
- Privacy dashboard, Room database, permission monitor, network monitor

## [0.4.0-alpha] — 2026-03-23
- Initial privacy dashboard release
