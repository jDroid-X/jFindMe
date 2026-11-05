# Implementation Plan 01: Enterprise Deep-Dive Architecture & Human-in-the-Loop Hardening

## 1. Overview
A comprehensive audit and systematic architectural hardening across all layers of the jFindMe platform:
- **Frontend Layer**: Android Native Java/Kotlin UI with responsive ceramic/mineral theming, WebGL 3D avatar viewport, and HTML5 Web Admin Dashboard.
- **Backend & Service Layer**: Android FusedLocation Telemetry Service (`TrackingService`), Geofence Broadcast Engine (`GeofenceBroadcastReceiver`), P2P/Cloud Pairing Managers (`FirebasePairingManager`, `LocalPairingManager`), and Node.js Matrix Proxy (`server.js`).
- **Database Layer**: SQLite 5-Tier Normalized Relational Schema (`DatabaseHelper.kt`) and Firebase Realtime / Firestore Clean Hub Matrix (`database.rules.json`).
- **Closed-Loop Feedback**: Converging every open UI branch, network exception, and database mutation to an authoritative single source of truth (SSOT).

---

## 2. Human-in-the-Loop UI/UX Hardening & Safeguards

### A. Geofence & Boundary Management (`GeofenceActivity.java`, `GeofenceBroadcastReceiver.kt`)
- **Interactive Validation**: Replaced silent execution halts and `NumberFormatException` crashes with custom dialogs (`showForensicDialog`) prompting for missing zone names, coordinate pins, or non-numeric radius inputs.
- **Radius Bounds**: Clamped radius inputs between `50m` and `50,000m` with automatic fallback to `100m`.
- **Database Normalization**: Fixed table query mismatch (`"safe_zones"` vs `SafeZoneTable.TABLE_NAME` `"safe_zone"`) and column lookup (`"alerts_active"` vs `SafeZoneTable.COL_IS_ALERTS_ENABLED`).
- **Composite ID Resolution**: Enabled `doesSafeZoneExist` and `isGeofenceAlertEnabled` to handle composite `zoneName___targetUid` IDs.

### B. Relational State & Member Dossiers (`PersonDetailsActivity.java`, `PersonDetailsViewModel.kt`)
- **Role Retention**: Retained active `role_family` and `role_friend` variables dynamically when updating emergency contacts or residences, preventing relationship resets to `"None"`.
- **Dual-Tier Unlinking**: Integrated `unlinkDevice` routines in `FamilyFriendsViewModel.kt` to purge nodes from both local SQLite and Firebase `relationships/$groupId/members` and `citizen_dossiers`.

### C. History & Replay System (`HistoryViewModel.kt`, `HistoryActivity.java`)
- **Emergency Cloud Fallback**: Fixed `fetchTrackingTargets` so unpopulated local databases (0 or 1 target) trigger an emergency network scavenge against `FirebaseComm.devicesRef` rather than remaining stuck in an infinite loading state.
- **Reverse Geocoding**: Integrated late-binding geocoding in `loadHistorySessions` to asynchronously resolve human-readable street addresses for historical breadcrumb coordinates.

### D. Forensic Vault & Storage Safety (`FactoryResetActivity.java`, `ForensicVaultExplorerActivity.java`)
- **Scoped Storage Protection**: Migrated system dump export from deprecated public external storage to `getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)` with `filesDir` fallback and `mkdirs()` guards.
- **Case-Insensitive Table Mapping**: Normalized table names (`PROFILE_TABLE`, `MASTER_ROSTER`, `safe_zone`, `history_table`, `sys_log`) to match live Firebase nodes (`devices`, `relationships`, `safe_zones`, `history`, `system_audit`).

### E. Avatar & Cross-App Identity Sync (`AvatarCropActivity.java`, `AvatarEditorActivity.java`, `ProfileActivity.java`)
- **Instant Cache Purge**: Injected `AvatarLoader.clearCache()` on crop saves, profile edits, and 3D avatar equipping to eliminate stale bitmap caches across the app.

---

## 3. Verification & Quality Gates
1. **Compilation**: `./gradlew.bat compileDebugSources --no-daemon` — Confirmed 0 errors.
2. **Database Integrity**: Verified foreign key cascading and SQLite table definitions.
3. **Security & Rules**: Configured comprehensive read/write security rules in `database.rules.json`.
