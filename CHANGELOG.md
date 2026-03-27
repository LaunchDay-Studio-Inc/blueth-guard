# Changelog

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
