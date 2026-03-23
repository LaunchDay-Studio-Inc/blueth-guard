# Blueth Guard — Development Plan

## Project Overview

**Blueth Guard** is an all-in-one Android security, optimizer, and privacy app.
Offline-first, open-source, no ads. Built by LaunchDay Studio.

Target: Replace 5 apps (antivirus + cleaner + privacy + battery + storage) with one.

---

## Open-Source Components to Integrate

### 1. LibreAV (projectmatris/antimalwareapp)
- License: GPL-3.0
- Stars: 286 | Language: Java | Forks: 43
- What: ML-based on-device malware detection
- We take: The TFLite model, feature extraction pipeline, APK analysis logic
- Adapt: Port to Kotlin, integrate into our scanner module, update ML model

### 2. AppManager (MuntashirAkon/AppManager)
- License: GPL-3.0
- Stars: 7,674 | Language: Java | Forks: 432
- What: Full-featured Android package manager
- We take: Package info extraction patterns, permission analysis, component inspection
- Adapt: Reference only (too monolithic to fork directly), rewrite relevant logic in Kotlin

### 3. NetGuard (M66B/NetGuard)
- License: GPL-3.0
- Stars: 3,522
- What: No-root Android firewall using VPN
- We take: Network traffic monitoring patterns, per-app traffic analysis
- Adapt: We only need the monitoring/logging part, not the blocking

### 4. Exodus Privacy Tracker Database
- Source: https://reports.exodus-privacy.eu.org/
- What: Database of known trackers embedded in Android apps
- We take: Tracker signature list (class names, network signatures)
- Adapt: Bundle offline, weekly delta updates

### 5. Hypatia (DivestOS)
- Source: https://divestos.org/pages/our_apps#hypatia (GitLab)
- License: GPL-3.0
- What: ClamAV-based offline malware scanner
- We take: ClamAV signature loading and matching patterns
- Adapt: Optional secondary scan engine alongside ML

---

## Architecture Decisions

### Language: Kotlin (100%)
- Modern Android standard
- Coroutines for async scanning
- No Java legacy code

### UI: Jetpack Compose + Material 3
- Single Activity architecture
- Bottom navigation with 5 tabs: Security | Optimize | Privacy | Battery | Settings
- Dark theme default (security app aesthetic)
- Blueth brand colors (blue/cyan gradient)

### Database: Room
- Scan results history
- App permission snapshots (diff over time)
- Battery drain logs
- Network traffic logs

### DI: Hilt
- Standard Android DI

### No Root Required
- All core features work without root
- Root-enhanced features (deeper cleanup, firewall rules) as bonus
- Clear UI indicator for root vs non-root mode

### Offline-First
- ML model bundled in APK (~5MB TFLite)
- Tracker DB bundled, delta updates when online
- No cloud dependencies for core scanning
- Optional: online hash lookup for zero-day detection

---

## Phase 1 — Foundation (Week 1-2)

### 1.1 Project Scaffold
- [x] Create Android project (Kotlin, Compose, Gradle KTS)
- [x] Set up Hilt DI
- [x] Set up Room database
- [x] Set up Navigation (bottom nav + nested graphs)
- [x] Set up CI (GitHub Actions — build + lint)
- [x] Material 3 theme with Blueth brand colors
- [x] App icon and splash screen

### 1.2 App List & Info
- [x] PackageManager queries to list all installed apps
- [x] Display: icon, name, package, install source, size, last updated
- [x] Sort/filter: by size, install date, source, permissions
- [x] Tap to expand: full permission list, components, signatures

### 1.3 Permission Auditor
- [x] Define risk scoring algorithm:
  - INTERNET + READ_SMS = HIGH
  - ACCESSIBILITY + INTERNET = CRITICAL
  - CAMERA + RECORD_AUDIO + INTERNET = HIGH
  - DEVICE_ADMIN + INTERNET = CRITICAL
  - etc.
- [x] Risk score badge per app (Safe / Low / Medium / High / Critical)
- [x] Permission breakdown screen with explanations
- [x] "What can this app do?" plain-English summary

### 1.4 Storage Analyzer
- [x] Scan internal storage by category (apps, media, cache, system, other)
- [x] Visual treemap or sunburst chart
- [x] Tap to drill into categories
- [x] Quick actions: clear cache, open app info

---

## Phase 2 — Security Engine (Week 3-4)

### 2.1 ML Malware Scanner
- [x] Port LibreAV's feature extraction (APK → feature vector)
  - Permissions used
  - API calls
  - Intents
  - Native libraries
  - Certificate info
- [x] Bundle TFLite model for on-device inference
- [x] Scan single APK → safe/suspicious/malware + confidence %
- [x] Full device scan (all installed apps)
- [x] Scan history with timestamps
- [x] Results: app name, risk level, reasons, recommended action

### 2.2 Tracker Detection
- [x] Bundle Exodus Privacy tracker database
- [x] Scan APK class names against tracker signatures
- [x] Display: tracker name, company, category, description
- [x] Summary: "This app contains 7 trackers from 4 companies"
- [x] Delta update mechanism (check for new tracker sigs weekly)

### 2.3 Sideload Tracker
- [x] Query installerPackageName for each app
- [x] Flag: Play Store / F-Droid / APK sideload / Unknown / ADB
- [x] Alert for apps installed from suspicious sources
- [x] "Installed from Telegram" type warnings

### 2.4 Device Admin Checker
- [x] List all device admin apps
- [x] Explain what device admin allows
- [x] One-tap revoke (opens system settings)

---

## Phase 3 — Optimizer (Week 5-6)

### 3.1 Smart Process Manager
- [x] List running processes with CPU and memory usage
- [x] Categorize: essential system / user app / background service / bloatware
- [x] Smart kill: only kills non-essential background processes
- [x] Do NOT kill: launcher, keyboard, active music, navigation, messaging
- [x] Kill history log

### 3.2 Cache Cleaner
- [x] Per-app cache size calculation
- [x] Sort by cache size (biggest first)
- [x] Individual app cache clear
- [x] One-tap clear all caches
- [x] Estimated space savings before clearing
- [x] Scheduled auto-clean option

### 3.3 Duplicate File Finder
- [x] Scan by: exact hash, similar images (perceptual hash), similar file names
- [x] Categories: photos, videos, downloads, documents
- [x] Side-by-side comparison view
- [x] Bulk select and delete
- [x] "Keep newest" / "Keep largest" quick actions

### 3.4 Bloatware Identifier
- [x] Database of known bloatware by manufacturer (Samsung, OnePlus, Xiaomi, etc.)
- [x] Safe-to-disable recommendations
- [x] One-tap disable (via ADB shell for rooted, instructions for non-root)
- [x] Crowdsourced bloatware database (users can report)

---

## Phase 4 — Privacy & Protection (Week 7-8)

### 4.1 Permission Heatmap
- [x] Matrix view: apps (rows) x permissions (columns)
- [x] Color-coded: green (safe) / yellow (moderate) / red (dangerous)
- [x] Tap cell for details
- [x] "Most dangerous apps" ranking
- [x] Permission timeline: what changed since last scan

### 4.2 Network Traffic Monitor
- [x] VPN-based packet capture (no root needed, like NetGuard)
- [x] Per-app data usage (upload / download)
- [x] Connection log: IP, hostname, port, timestamp
- [x] Flag connections to known ad/tracker domains
- [x] Optional: block connections (firewall mode)

### 4.3 Real-time Install Guard
- [x] BroadcastReceiver for PACKAGE_ADDED
- [x] Auto-scan new installs
- [x] Notification with scan result
- [x] "Block install" recommendation for flagged apps

### 4.4 Clipboard Monitor
- [x] Detect apps reading clipboard (Android 12+ native, polyfill for older)
- [x] Log clipboard access with app name and timestamp
- [x] Alert for suspicious clipboard reads

---

## Phase 5 — Battery Guardian (Week 7-8, parallel with Phase 4)

### 5.1 Wakelock Detector
- [x] List apps holding wakelocks
- [x] Duration and frequency tracking
- [x] "Top battery drainers" ranking
- [x] Historical trends (last 24h, 7d, 30d)

### 5.2 Background Service Monitor
- [x] List persistent background services
- [x] Categorize: essential / optional / wasteful
- [x] Estimated battery impact per service
- [x] Recommendations to disable

### 5.3 Battery Health Dashboard
- [x] Current battery stats (health, temperature, voltage, technology)
- [x] Charge cycle estimation
- [x] Charging speed monitor
- [x] Battery drain graph over time

---

## Phase 6 — Polish & Release (Week 9-10)

### 6.1 UI Polish
- [x] Animations and transitions
- [x] Onboarding flow (3 screens explaining features)
- [x] Widget: quick scan + battery + storage at a glance
- [x] Notification channels (scan results, alerts, recommendations)
- [x] Localization (English first, Arabic, Spanish, Portuguese, Hindi next)

### 6.2 Settings
- [x] Scan schedule (daily, weekly, on-install-only)
- [x] Notification preferences
- [x] Theme (dark, light, system)
- [x] Database update frequency
- [x] Export scan reports (PDF/JSON)
- [x] About + open source licenses

### 6.3 Distribution
- [x] F-Droid metadata (fastlane structure)
- [x] GitHub Releases (signed APK)
- [x] Play Store listing (title, description, screenshots, feature graphic)
- [x] blueth.online product page
- [x] APK direct download from website

### 6.4 Documentation
- [x] User guide
- [x] Privacy policy (we collect nothing)
- [x] FAQ
- [x] Contributing guide
- [x] Architecture docs for contributors

---

## Monetization Strategy

### Free (open-source, forever)
- Full security scanner
- Permission auditor
- Storage analyzer
- Cache cleaner
- Battery monitor
- Bloatware identifier

### Pro ($2.99 one-time, NOT subscription)
- Real-time protection (install guard, accessibility watcher)
- Scheduled auto-scans
- Network traffic monitor
- Duplicate file finder
- Custom scan rules
- Priority database updates
- Export reports
- No nag screens in free tier — just a "Pro" badge on locked features

### Revenue Model
- Play Store: $2.99 one-time
- Direct APK: Pay via blueth.online (Stripe)
- F-Droid: Free only (F-Droid policy)
- Estimated market: 10M+ users need this, 2% conversion = 200K * $2.99 = $597K potential

---

## Success Metrics

| Metric | Target (6 months) |
|--------|-------------------|
| GitHub stars | 1,000+ |
| F-Droid installs | 10,000+ |
| Play Store installs | 50,000+ |
| Pro conversions | 2% of installs |
| Play Store rating | 4.5+ |
| Malware detection rate | 95%+ (tested against VirusTotal dataset) |

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Google Play removal (antivirus policy) | Comply with Play Protect API, publish on F-Droid as backup |
| ML model accuracy | Train on updated datasets, A/B test with VirusTotal |
| Battery drain from monitoring | All monitoring opt-in, efficient polling, WorkManager |
| GPL license complexity | Clear attribution, no proprietary mixing, consult if needed |
| Scope creep | Strict phase gates, MVP first, polish later |

---

## Team

- **King Gin** — Product owner, architecture decisions, Codespace coding
- **AI Assistant** — Planning, research, prompt engineering, code review
- **Community** — Bug reports, translations, bloatware database contributions

---

*Last updated: 2026-03-23*
*Project: LaunchDay-Studio-Inc/blueth-guard*
