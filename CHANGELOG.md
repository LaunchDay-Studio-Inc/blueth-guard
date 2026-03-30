# Changelog

## [1.3.0-beta] — 2026-03-30

### New
- **AccessibilityService** — real Force Stop and Cache Clear automation via Android Settings UI
- **Onboarding Accessibility page** — new step 6 of 7 guiding users to enable accessibility permission
- **Accessibility prompt dialog** — Optimizer shows prompt when accessibility is not enabled before One-Tap Optimize

### Improved
- **DNS blocklist** expanded from ~90 to 430+ real malicious domains across 20+ threat categories
- **Tracker database** expanded from 93 to 170+ signatures across 7 categories
- **Remote signature database** (signature-db/latest.json) populated with 20 malware packages, 10 patterns, and 10 tracker entries
- **ProcessManager** now uses AccessibilityService for genuine Force Stop via Settings
- **CacheCleaner** now uses AccessibilityService for genuine Cache Clear via Settings

### Fixed
- **VPN packet passthrough** — replaced broken 0.0.0.0/0 route with DNS-only routing (8.8.8.8, 8.8.4.4) to prevent infinite packet loop
- **DNS forwarding** — VPN now properly forwards allowed DNS queries via protected DatagramSocket and rebuilds response packets
- **Anti-theft alarm** — alarm now auto-stops after 30 seconds instead of looping indefinitely

## [1.2.0-beta] — 2026-03-29

### New
- **Quick Scan on Home** — inline quick scan button with mini-report showing virus/malware/tracker counts and risk breakdown
- **WiFi Security card** on Home dashboard showing current network security status
- **Anti-Theft shortcut** on Home screen for easy access to remote lock/wipe/locate
- **All Files Access** and **Location** onboarding pages (6 pages total, up from 4)
- **Install Guard seeding** — pre-populates install history with currently installed apps on first launch
- **RAM Boost before/after** — Smart Boost result card now shows RAM available before and after boost
- **Own cache clearing** — CacheCleaner can now clear Blueth Guard's own cache directly

### Improved
- **Deep Scan** now detects large files (>100MB), old unused files (>50MB, >6 months), and leftover app data from uninstalled apps
- **Deep Scan** expanded to scan multiple roots including SD card, with tabbed results (Threats/Space Hogs/Old Files/App Leftovers)
- **Deep Scan** requires MANAGE_EXTERNAL_STORAGE permission with guided dialog
- **Privacy QuickStats** boxes are now clickable — tap to see matching app list in a bottom sheet with direct link to system app settings
- **Network consumers** are now clickable — tap to see upload/download breakdown and open app settings
- **Cache Clean All** now shows confirmation dialog explaining Android limitations before opening settings
- **Processes tab** shows Usage Access permission prompt when not granted
- **Adaptive icon** fully rebuilt — transparent foreground layer, distinct circular round icon, monochrome layer for Android 13+ themed icons

### Fixed
- Network data deduplication — old entries deleted before inserting new data to prevent accumulation
- Home dashboard now shows RAM usage with Rocket icon instead of storage info in the optimizer card

## [1.1.0-beta] — 2026-03-28

### New
- **Official Blueth Guard icon** — custom shield icon replaces placeholder, applied across all densities and Play Store listing
- Version bump to 1.1.0-beta (versionCode 9)

### Changed
- Switched from adaptive vector icon to high-quality raster PNG icon at all mipmap densities
- Removed ic_launcher_foreground.xml and ic_launcher_background.xml vector drawables (replaced by PNG)

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
- Security scanner with 170+ tracker detection (Exodus Privacy database)
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
