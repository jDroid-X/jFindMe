# Enterprise Lifecycle, Installation, Uninstallation & Upgrade Rulebook (V5.0)
## jFindMe & jButterFly Real-Time Tracking & Family Mesh Platform
### Standard Operating Procedures (SOP): Human-In-The-Loop (HITL) Scenario Matrix

> **Mandatory Reference Pipeline**: `Workflow (.agent/rules/workflow.md)` ➔ `Rules (.agent/rules/rules.md)` ➔ `Agents (.agent/rules/agents.md)` ➔ `Skills (.agent/rules/skills.md)` ➔ `Branding (.agent/rules/branding.md)` ➔ `Lifecycle (.agent/rules/lifecycle_rules.md)`

---

## 🏛️ 1. Executive Summary & Core Architectural Principle

In enterprise Android tracking and telemetry systems (`jFindMe` / `jButterFly`), installation, in-place upgrades, rollback, and uninstallation operate across strict Linux sandboxes, hardware sensors, and Android operating system security layers.

This Rulebook defines the **Tri-Phase Lifecycle State Machine**, the **Hardware/Software Condition Verification Engine**, and **7 Scenario-Based Standard Operating Procedures (SOPs)** with deterministic **Human-In-The-Loop (HITL)** validation gates arranged in precise sequential order for maximum velocity and zero data loss.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        TRI-PHASE LIFECYCLE STATE MACHINE                               │
└────────────────────────────────────────────────────────────────────────────────────────┘

  [ PHASE 1: FRESH INSTALLATION (Day 0) ]
        ├── Hardware : Silicon Root Anchor (ro.ril.oem.sno / Settings.Secure.ANDROID_ID)
        ├── Storage  : Sandboxed Linux UID (/data/user/0/com.jdroidx.jfindme/)
        ├── Database : Seed SQLite V81.2 (27 Normalized Tables + 13 Compatibility Views)
        └── Security : Guided 7-Stage Permission Escalation Ladder (HITL SOP-01)

  [ PHASE 2: IN-PLACE PACKAGE UPGRADE (Continuous Zero-Loss Delivery) ]
        ├── Pre-Flight : Battery >= 15%, Storage >= 150 MB buffer, Network Quality
        ├── Software   : Keystore Signature MATCH, versionCode Monotonicity (>=)
        ├── Execution  : API 31+ Unattended (USER_ACTION_NOT_REQUIRED) vs 1-Tap Fallback
        └── OS Revive  : ACTION_MY_PACKAGE_REPLACED, SQLite PRAGMA migrate, Service restart

  [ PHASE 3: PRE-UNINSTALL & TEARDOWN (Clean Forensic Exit) ]
        ├── Sentinel   : Kill TrackingService, WorkManager, AlarmReceivers (Zero leaks)
        ├── Cloud Flush: Push offline telemetry queue & set node status to GHOST
        ├── Forensic   : Human-In-The-Loop export of 27 tables to /Documents/jFindMe_Vault/
        ├── SQLite Wipe: Gate 2 confirmed purge of local tables and credentials
        └── OS Handoff : Launch native Intent.ACTION_DELETE package uninstaller
```

---

## 🔬 2. Hardware Condition Verification & Action Matrix

Before initiating any installation, silent update, or background sync cycle, the system MUST evaluate and verify the physical hardware environment:

| Hardware Factor | Evaluation Metric | Threshold / Condition | Verification Logic | Action Required if Condition Fails |
|:---|:---|:---|:---|:---|
| **Silicon Identity** | Silicon Serial Anchor | `ro.ril.oem.sno`, `ro.ril.oem.psno`, or `ANDROID_ID` | `DeviceIdentityHelper.getRawAndroidId(context)` | If null/corrupt, fallback to thread-safe cached `.silicon_anchor`; **strictly reject random UUID generation**. |
| **Battery Level** | Power & Thermal State | $\ge 15\%$ (or AC Charger connected) | `BatteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)` | Abort silent background download/install; defer until plugged in or charged. Alert user with non-intrusive notification if manual. |
| **Storage Space** | Free Disk Cache | $\ge 150\text{ MB}$ free in `context.cacheDir` | `StatFs(context.cacheDir.path).availableBytes` | Abort download; execute rolling cache purge (`cacheDir.deleteRecursively()`); log `INSUFFICIENT_STORAGE_ERROR`. |
| **Network Interface** | Transport & Metering | Wi-Fi / Unmetered or High-Speed LTE/5G | `ConnectivityManager.getNetworkCapabilities()` | If Metered Cellular with low signal, defer Full APK download; prioritize Smart Delta Bit-Patch ($\le 5\text{ MB}$). |
| **Baseband Modem** | SIM Card & Carrier Signal | SIM Ready (`SIM_STATE_READY`), Cell Tower active | `TelephonyManager.getSimState()`, `getAllCellInfo()` | If SIM missing or dead zone, engage SMS fallback beacon & Wi-Fi Mesh failover; preserve offline GPS buffer. |
| **Display Form Factor**| Viewport Geometry | Universal 5-Stage Scaling (`dp` width) | `resources.configuration.screenWidthDp` | Dynamically apply LIBERAL (25dp), STANDARD (20dp), COMPACT (15dp), SLIM (10dp), or ULTRA_SLIM (5dp). |

---

## 💻 3. Software & Android OS Condition Verification Matrix

Operating system API levels and security policies dictate how installation and upgrade processes execute:

| Software Layer | Target Condition | Evaluation Rule | Action Path if Met | Fallback Action if Unmet |
|:---|:---|:---|:---|:---|
| **Android OS API** | Android 12+ (API 31+) | `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` | Use `PackageInstaller.SessionParams.setRequireUserAction(USER_ACTION_NOT_REQUIRED)` for **100% Unattended Silent Upgrade**. | Launch 1-tap interactive system prompt (`STATUS_PENDING_USER_ACTION`). |
| **Unknown Sources** | Install Unknown Apps | `context.packageManager.canRequestPackageInstalls()` | Stream APK directly into `PackageInstaller` pipeline with zero user friction. | Display guided modal dialog routing to `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES`. |
| **Keystore Signature** | X.509 Signing Match | `packageManager.getPackageArchiveInfo()` signatures match installed app | Proceed with In-Place Upgrade (Preserves Linux UID, SQLite data, and granted permissions). | **SIGNATURE MISMATCH GATE**: Show Clean Upgrade Dialog $\to$ Export settings to external vault $\to$ Prompt uninstall of debug build $\to$ Install release build. |
| **Version Monotonicity**| Version Code Order | `downloadedCode >= installedCode` | Allow package replacement and schedule `SQLiteOpenHelper.onUpgrade()`. | **DOWNGRADE BLOCKED**: Android OS rejects lower version codes; display `DEVELOPER TRACK` warning; abort installation. |
| **Runtime Permissions**| Core Permission Set | Location, Phone, Notif, Doze Exemption | Validate `ContextCompat.checkSelfPermission` for all mandatory permissions. | Intercept with Non-Cancelable Installation Guide Popup (`UiArchitect.showValidationDialog`). |
| **Doze Mode Whitelist** | Battery Optimization | `PowerManager.isIgnoringBatteryOptimizations()` | Background `TrackingService` and `SilentUpdateWorker` execute without OS throttling. | Prompt user with `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. |

---

## 📋 4. Standard Operating Procedures (SOP): Human-In-The-Loop Scenario Matrix

---

### SOP-01: Zero-State Day-0 Onboarding & Guided Permission Escalation (HITL)
**Trigger**: Fresh installation, first launch of `SplashActivity` / `AuthActivity`.  
**Objective**: Establish immutable silicon hardware anchor, seed local database, and escalate permissions sequentially without intimidating the user.

```
[Step 1: Hardware Qualification] ➔ [Step 2: Silicon Identity Sealing] ➔ [Step 3: Permission Escalation Ladder]
        ➔ [Step 4: OEM Background Protection] ➔ [Step 5: Database Seeding] ➔ [Step 6: Mesh Enrollment]
```

1. **Step 1: Hardware & OS Qualification**:
   - Verify Android API $\ge 26$ (Android 8.0 Oreo). If $< 26$, display non-supported OS dialog and abort.
   - Verify minimum internal storage $\ge 300\text{ MB}$ (allows for 27 SQLite tables and telemetry caching).
2. **Step 2: Silicon Root Anchoring & Anti-Spoofing**:
   - Resolve physical hardware serial via `DeviceIdentityHelper.getRawAndroidId()`.
   - Write immutable anchor to `system_registry.xml` and secure external mirror `.silicon_anchor`.
   - Strictly prohibit generating ephemeral software UUIDs.
3. **Step 3: Human-In-The-Loop Permission Escalation Ladder (Strict Sequential Order)**:
   - **Gate 3.1: Foreground Location**: Prompt for `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` with educational modal explaining live map tracking.
   - **Gate 3.2: Background Location**: On Android 10+ (API 29+), launch guided sheet explaining "Allow all the time" requirement for safe zones and SOS beacon, then route to system permission dialog.
   - **Gate 3.3: Baseband Cellular Access**: Request `READ_PHONE_STATE` for cell tower triangulation (MCC/MNC/LAC/CID) fallback during GPS dead-zones.
   - **Gate 3.4: Notifications**: On Android 13+ (API 33+), request `POST_NOTIFICATIONS` for foreground service status dot.
   - **Gate 3.5: Physical Activity**: Request `ACTIVITY_RECOGNITION` for adaptive motion throttling (15s motion vs 15m stationary).
4. **Step 4: OEM Battery Optimization & Doze Mode Exemption**:
   - Check `PowerManager.isIgnoringBatteryOptimizations()`.
   - If restricted, display gentle one-click card routing to `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
   - If OEM is Xiaomi/Samsung/Huawei (detected via `Build.MANUFACTURER`), surface OEM-specific Auto-Start checklist (SOP-07).
5. **Step 5: Database Baseline Seeding**:
   - Initialize SQLite Schema V81.2 (27 normalized tables + 13 zero-loss compatibility views).
   - Seed default role definitions, theme token (Theme 0: Coral Sea Teal), and latest-data-first query triggers.
6. **Step 6: Firebase Cloud Mesh Enrollment**:
   - Register node in Firebase Realtime Database at `/devices/{agentId}/profile` and `/devices/{agentId}/device/live`.
   - Generate initial 6-digit mesh invitation code for circle discovery.

---

### SOP-02: Zero-Downtime Unattended Background Upgrade (Automated Over-The-Air)
**Trigger**: Daily 03:03 AM `SilentUpdateWorker` execution or Firebase Cloud Push `/commands/system_update`.  
**Objective**: Autonomous in-place update without interrupting active location telemetry or presenting user friction.

```
[Step 1: Silent Pre-Flight] ➔ [Step 2: Buffered Streaming] ➔ [Step 3: Cryptographic Verification]
        ➔ [Step 4: Unattended Commit (API 31+)] ➔ [Step 5: Post-Upgrade Revive]
```

1. **Step 1: Silent Pre-Flight Checks**:
   - Verify Battery $\ge 15\%$ or device connected to charger.
   - Verify free storage in `cacheDir` $\ge 150\text{ MB}$.
   - Verify network transport: If metered cellular and signal weak, defer download; if unmetered Wi-Fi, proceed immediately.
2. **Step 2: Buffered Binary Streaming & SHA-256 On-the-Fly**:
   - Stream from canonical GitHub CDN (`jDroid-X/jButterfly`) using 16 KB read buffer on `Dispatchers.IO`.
   - Enforce MIME guard: If content type contains `text/html` (e.g. login gate or 404), abort immediately.
   - Compute SHA-256 checksum dynamically as bytes stream into `cacheDir/update_jfindme.apk`.
3. **Step 3: Cryptographic & Compatibility Pre-Validation**:
   - Verify ZIP magic bytes (`0x50, 0x4B, 0x03, 0x04`).
   - Call `packageManager.getPackageArchiveInfo()`.
   - Enforce package name equality: `archive.packageName == "com.jdroidx.jfindme"`.
   - Verify X.509 signing certificates match installed keystore.
   - Enforce version code monotonicity: `archive.versionCode > currentInstalledCode`.
4. **Step 4: Unattended PackageInstaller Commit (Android 12+ / API 31+)**:
   - Open `PackageInstaller.Session` with `USER_ACTION_NOT_REQUIRED`.
   - Stream APK into system pipe; commit session with `PackageInstallStatusReceiver` pending intent.
   - (Fallback for API $\le 30$: If unattended is unavailable, defer install until device screen turns on, then surface 1-tap notification).
5. **Step 5: Post-Upgrade Revive (`ACTION_MY_PACKAGE_REPLACED`)**:
   - Intercepted by `AppUpgradeReceiver.kt`.
   - Trigger SQLite `DatabaseHelper.onUpgrade()`: Execute dynamic `PRAGMA table_info` checks and append any missing columns non-destructively. Zero data wipe.
   - Immediately revive `TrackingService`, `BackgroundJobManager`, and alarm sentinels.
   - Push `SUCCESS_INSTALLED` with new `versionCode` to Firebase `/devices/{agentId}/version_metadata/status`.
   - Purge downloaded APK from cache directory.

---

### SOP-03: Interactive Human-In-The-Loop Manual Upgrade via Settings Console
**Trigger**: Operator taps **Settings ➔ 17. System Update ➔ "🔄 CHECK FOR UPDATES"**.  
**Objective**: Transparent, user-controlled update with live feedback, bandwidth HUD, and verified installation handoff.

```
[Step 1: Manual Scan Trigger] ➔ [Step 2: Differential Analysis] ➔ [Step 3: HITL Permission Check]
        ➔ [Step 4: Live Bandwidth HUD] ➔ [Step 5: Installation Handoff]
```

1. **Step 1: Manual Scan Execution**:
   - Display `AngelLoader` spinning indicator on the Check button.
   - Scan GitHub Release API and Firebase RTDB concurrently on `Dispatchers.IO`.
2. **Step 2: Differential Version & Patch Analysis**:
   - If `remoteCode > currentCode`: Render **"NEW RELEASE READY"** hero card with release notes, package size, and "Smart Delta" badge if a bit-diff patch is available for this build.
   - If `remoteCode == currentCode`: Render **"SYSTEM UP TO DATE"** with verified green checkmark and timestamp.
   - If `remoteCode < currentCode`: Render **"DEVELOPER TRACK"** warning (Installed version is ahead of public release; downgrade blocked).
3. **Step 3: Human-In-The-Loop Permission Interceptor**:
   - When user taps **"🚀 UPDATE NOW"**, check `packageManager.canRequestPackageInstalls()`.
   - If false, display guided dialog: *"To update jFindMe seamlessly, please enable 'Allow from this source' on the next screen."* Route directly to `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES`.
4. **Step 4: Glassmorphic Real-Time Bandwidth HUD**:
   - Display non-cancelable modal progress card with:
     - Smooth 0% to 100% progress bar.
     - Live transferred vs total size (e.g. `14.2 MB / 68.5 MB`).
     - Real-time throughput smoothed at 500ms intervals (e.g. `4.2 MB/s`).
     - Explicit **"CANCEL DOWNLOAD"** button (which terminates the stream and purges partial files).
5. **Step 5: Installation Intent & Resume Observer**:
   - If API 31+ and permission granted: Commit silent session; UI shows *"Installing update in-place..."*
   - If API $\le 30$: Launch native PackageInstaller intent via `FileProvider.getUriForFile()`.
   - On `onResume()`, detect if `versionCode` has increased; if yes, display celebration toast and refresh version badge.

---

### SOP-04: Cross-Keystore Signature Mismatch Recovery Bridge (HITL Migration)
**Trigger**: Attempted upgrade where downloaded APK signature does not match installed signature (e.g. migrating from a `debug.keystore` dev build to a production `release.keystore` build).  
**Objective**: Prevent Android OS `INSTALL_FAILED_UPDATE_INCOMPATIBLE` error while guaranteeing **Zero Data Loss** through an automated backup bridge.

```
[Step 1: Signature Pre-Check] ➔ [Step 2: HITL Guidance Dialog] ➔ [Step 3: Zero-Touch Vault Export]
        ➔ [Step 4: Guided Uninstall/Reinstall] ➔ [Step 5: Automatic Post-Boot Restore]
```

1. **Step 1: Signature Mismatch Interception**:
   - In `ApkInstallerHelper.isValidApkFile()`, compare X.509 certificate hashes of the downloaded APK against the running app.
   - If hashes differ, **HALT installation handoff immediately**. Never allow Android OS to trigger a blind install failure.
2. **Step 2: Human-In-The-Loop Guidance Modal**:
   - Display high-clarity dialog:
     > **"Cryptographic Signature Mismatch Detected"**  
     > *"The new release is signed with a Production Release Key, while your device is running a Development Debug Build. Android security prevents direct overwriting. We have prepared an automated 1-Click Backup & Migrate flow so you lose zero data."*
   - Present two buttons: **"1-CLICK BACKUP & MIGRATE"** and **"CANCEL"**.
3. **Step 3: Zero-Touch Local & Cloud Backup Bridge**:
   - User approves: System dumps all 27 SQLite tables and `system_registry.xml` to an encrypted vault file:
     `/Documents/jFindMe_Vault/jfindme_migration_<agentId>_<timestamp>.bak`
   - Simultaneously upload migration snapshot to Firebase Cloud at `/devices/{agentId}/migration_backup`.
   - Store target APK in external public downloads folder: `/Download/jFindMe_Release_Latest.apk`.
4. **Step 4: Guided Two-Stage Handshake**:
   - Step 4.1: Launch native uninstall prompt for the debug build with user message: *"Uninstalling old debug build..."*
   - Step 4.2: Launch installer intent for `/Download/jFindMe_Release_Latest.apk`.
5. **Step 5: Automatic Post-Boot Restore (Day 0 Recovery)**:
   - On first launch of the freshly installed release build, `SplashActivity` scans `/Documents/jFindMe_Vault/`.
   - Validates that the backup hardware Silicon ID matches the current device's hardware ID.
   - Automatically re-imports all 27 SQLite tables, circle pairings, and credentials.
   - Surface success banner: *"Welcome to Production! All circle data, history, and settings restored successfully."*

---

### SOP-05: Rollback, Failure Containment & 3-Strike Circuit Breaker
**Trigger**: Network drops mid-download, corrupted byte transfer, insufficient storage during extraction, or OS installer rejection.  
**Objective**: Contain faults, clean temporary files, protect system stability, and prevent battery drain from infinite retry loops.

```
[Step 1: Fault Detection & Classification] ➔ [Step 2: Immediate Cache Eviction] 
        ➔ [Step 3: 3-Strike Circuit Breaker] ➔ [Step 4: Actionable HITL Notification]
```

1. **Step 1: Fault Detection & Classification**:
   - Identify failure type: `NETWORK_TIMEOUT`, `STORAGE_FULL`, `HASH_MISMATCH`, `SIGNATURE_MISMATCH`, or `OS_REJECTED`.
2. **Step 2: Immediate Cache Eviction & Lock Release**:
   - Delete any partial or corrupted `.apk` in `cacheDir`. Never leave orphan zero-byte binaries on disk.
   - Release all file locks and background wake locks.
   - Reset `system_registry.is_updating = false`.
3. **Step 3: 3-Strike Exponential Backoff Circuit Breaker**:
   - Increment `system_registry.update_fail_count`.
   - If `fail_count < 3`: Schedule next retry via `WorkManager` with exponential backoff (1h, 4h, 8h).
   - If `fail_count >= 3`: **TRIP CIRCUIT BREAKER**.
     - Pause automatic update scanning for 48 hours.
     - Log `CIRCUIT_BREAKER_TRIPPED` event to Firebase `/devices/{agentId}/sys_logs`.
4. **Step 4: Actionable Human-In-The-Loop Notification**:
   - Surface a low-priority notification with clear troubleshooting advice:
     - If Storage Full: *"jFindMe update postponed: Needs 150 MB free space. Tap to clean cache."*
     - If Network Drop: *"jFindMe update postponed: Waiting for stable Wi-Fi connection."*

---

### SOP-06: Pre-Uninstall Teardown & Factory Reset (Gate 2 Protected Clean Exit)
**Trigger**: User or Admin navigates to **Settings ➔ Factory Reset & Uninstall Console** (`FactoryResetActivity.java`).  
**Objective**: Perform a clean, forensic-grade uninstallation ensuring zero background process leakage, complete cloud detachment, and optional local data export.

```
[Step 1: Forensic Matrix Audit] ➔ [Step 2: Gate 2 Operator Confirmation] ➔ [Step 3: Cloud Telemetry Flush]
        ➔ [Step 4: Vault Dump Export] ➔ [Step 5: Sentinel Teardown] ➔ [Step 6: SQLite Wipe] ➔ [Step 7: OS Uninstall]
```

1. **Step 1: Forensic Matrix Audit & Data Inspection**:
   - Display all 27 SQLite tables with real-time row counts and sample records.
   - Allow user to inspect and filter data before deciding to purge.
2. **Step 2: Gate 2 Mandatory Confirmation Prompt**:
   - Require explicit operator confirmation line before proceeding:
     > **`Confirming - "Violating Prime Directive Rules"`** (if active ward circle affected) or **`Confirm: Purge and Uninstall`**.
3. **Step 3: Mandatory Cloud Telemetry Flush**:
   - Force immediate upload of any pending offline location records, emergency SOS logs, and geofence events to Firebase RTDB.
   - Update node status in Firebase: Set `/devices/{agentId}/device/live/status = "⚪ GHOST"` and `is_active = false`.
   - Unbind bidirectional pairing relationships in `/relationships/`.
4. **Step 4: Human-In-The-Loop Forensic Vault Export**:
   - Offer user: *"Do you want to save a forensic text dump of all 27 tables before wiping?"*
   - If approved: Writes `/Documents/jFindMe_Vault/JFINDME_FORENSIC_DUMP_<TIMESTAMP>.txt`.
5. **Step 5: Sentinel Service Teardown**:
   - Call `stopService(Intent(this, TrackingService::class.java))`.
   - Cancel all `WorkManager` scheduled tasks (`SilentUpdateWorker`, `SimForensicWorker`).
   - Unregister all sticky alarm receivers (`WatchdogAlarmReceiver`, `SecurityReceiver`).
   - Release all acquired wake locks (`PowerManager.WakeLock`).
6. **Step 6: Local SQLite Database & Registry Purge**:
   - Execute `DatabaseHelper.clearAllTables()` across all 27 normalized tables.
   - Wipe `system_registry.xml`, clear app private cache, delete `.silicon_anchor`.
7. **Step 7: Operating System Uninstallation Handoff**:
   - Dispatch Android OS uninstallation intent:
     ```java
     Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
     uninstallIntent.setData(Uri.parse("package:" + getPackageName()));
     uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     startActivity(uninstallIntent);
     finishAffinity();
     ```

---

### SOP-07: OEM Battery Aggression & Background Kill Prevention
**Trigger**: Device manufacturer identified as Xiaomi/Redmi, Samsung, Huawei, Oppo, Vivo, or OnePlus.  
**Objective**: Prevent OEM proprietary battery managers from terminating background tracking sentinels.

```
[Step 1: OEM Telephony Identification] ➔ [Step 2: Auto-Start Deep-Link] ➔ [Step 3: Sticky Foreground Notification]
```

1. **Step 1: OEM Telephony & Manufacturer Detection**:
   - Read `Build.MANUFACTURER.toLowerCase()`.
2. **Step 2: Targeted OEM Deep-Link Navigation**:
   - **Xiaomi (MIUI / HyperOS)**: Route to `com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity`.
   - **Samsung (OneUI)**: Route to `com.samsung.android.lool/com.samsung.android.sm.battery.ui.BatteryActivity` (Add to "Never sleeping apps").
   - **Huawei (EMUI)**: Route to `com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity`.
   - **Oppo / Vivo (ColorOS / Funtouch)**: Route to Startup Manager and Background High Power Consumption list.
3. **Step 3: Sticky Low-Priority Foreground Notification**:
   - Bind `TrackingService` to a continuous, non-dismissible notification channel (`CHANNEL_ID_TRACKING`) with `FOREGROUND_SERVICE_TYPE_LOCATION`.
   - Render live strategy badge (`🛰️ GPS` or `📡 Cell Tower`) to guarantee Android OS `system_server` does not demote the process to cached low-memory status.

---

## 🔒 5. Master Invariants & Safety Guardrails

1. **Never Brick on Downgrade**:
   - Downgrade attempts (`remoteCode < currentCode`) are blocked with a clear "Developer Track" advisory.
2. **Never Brick on Signature Mismatch**:
   - Mismatched certificates trigger the automated Backup & Migration Bridge (SOP-04). Direct package handoff is blocked.
3. **Atomic File Streaming**:
   - Downloaded APKs are written to `.tmp` files and only renamed to `.apk` once SHA-256 and ZIP magic bytes pass 100%.
4. **Gate 2 Confirmation on Purge**:
   - Deleting databases or uninstalling requires one confirmation line from operator **JEET**.
5. **Ward Top Position 0 Invariance**:
   - Lifecycle upgrades must preserve child/ward priority sorting (`4379A761` / Riddhi) across SQLite migrations without exception.
