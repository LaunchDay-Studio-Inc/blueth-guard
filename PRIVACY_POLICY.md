# Privacy Policy — Blueth Guard

**Effective Date:** March 23, 2026  
**Last Updated:** March 23, 2026  
**Developer:** LaunchDay Studio Inc.

## Summary

Blueth Guard collects **no user data**. Period.

## Data Collection

Blueth Guard does not collect, store, transmit, or share any personal information. Specifically:

- **No analytics** — We do not use Google Analytics, Firebase Analytics, or any other analytics service.
- **No telemetry** — No usage data, crash reports, or behavioral data is sent anywhere.
- **No cloud services** — All scanning, analysis, and processing happens entirely on your device.
- **No personal information** — We do not collect names, email addresses, phone numbers, device identifiers, or any other personal data.
- **No advertising** — There are no ads in the app, and no advertising identifiers are collected.
- **No third-party SDKs** that collect data — The app contains no tracking SDKs.

## Data Processing

All data processing occurs locally on your device:

- **Security scans** are performed using on-device ML models and bundled signature databases.
- **Permission audits** use Android's PackageManager API locally.
- **Tracker detection** uses a bundled database from Exodus Privacy, matched locally.
- **Network monitoring** uses Android's NetworkStatsManager API locally.
- **Battery analysis** uses Android's system APIs locally.
- **Scan reports** are generated locally and only shared if you explicitly choose to export them.

## Network Access

Blueth Guard requests the `INTERNET` permission for one purpose only:

- **Optional signature database updates** — When you choose to update the malware signature database, the app downloads the latest signatures. No user data is transmitted during this process.

If you never update the database, the app never makes any network requests.

## Permissions

Blueth Guard requests the following permissions, all used exclusively for their stated purpose:

| Permission | Purpose |
|---|---|
| `QUERY_ALL_PACKAGES` | List installed apps for scanning |
| `INTERNET` | Optional signature database updates |
| `ACCESS_NETWORK_STATE` | Check connectivity before updates |
| `REQUEST_DELETE_PACKAGES` | Allow user to uninstall bloatware |
| `PACKAGE_USAGE_STATS` | Monitor app usage for battery analysis |
| `FOREGROUND_SERVICE` | Real-time protection service |
| `KILL_BACKGROUND_PROCESSES` | Smart process optimizer |
| `POST_NOTIFICATIONS` | Security and scan result alerts |
| `READ_EXTERNAL_STORAGE` | Storage analysis (Android 12 and below) |

## Open Source

Blueth Guard is open source under the GPL-3.0 license. You can inspect every line of code to verify our privacy practices:

- **Source code:** [github.com/LaunchDay-Studio-Inc/blueth-guard](https://github.com/LaunchDay-Studio-Inc/blueth-guard)

## Children's Privacy

Blueth Guard does not knowingly collect any data from anyone, including children under 13.

## Changes to This Policy

If we ever change this policy, we will update this document in the repository and increment the app version. Since we collect no data, changes are unlikely.

## Contact

If you have questions about this privacy policy:

- **Email:** info@launchdaystudio.com
- **GitHub Issues:** [github.com/LaunchDay-Studio-Inc/blueth-guard/issues](https://github.com/LaunchDay-Studio-Inc/blueth-guard/issues)
