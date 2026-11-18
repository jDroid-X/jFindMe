# System Update & Patch Center — Master Requirement Specification
## Coordinate Tree Architecture: ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB
### Enterprise L5 Lifecycle Standard (20-Year Senior Developer Quality)

```
ROOT: SYSTEM UPDATE & PATCH LIFECYCLE MANAGEMENT MASTER PLAN
│
├── 💼 [BR]₁: Version Audit & Integrity Verification
│    ├── 🖱️ [EL]₁․₁: Version Telemetry Hero Display
│    │    ├── 🖥️ [FE]₁․₁․₁: Installed Version Badge & Baseline Label
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₁․₁․₁․₁: Dynamically render `v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})`; hardcoded strings strictly prohibited.
│    │    │    │    ├── FR₁․₁․₁․₂: State badge color coding: Emerald Green (#00E676) for verified baseline, Amber (#FFB300) when upgrade pending.
│    │    │    │    └── Testing Scenarios:
│    │    │    │         ├── Pass: `VERSION_CODE` equals active running APK manifest code (e.g. 186).
│    │    │    │         └── Fail: Null, empty string, or stale hardcoded fallback (e.g. 115, 99) rendered on screen.
│    │    │    └── ⚙️ [BE]₁․₁․₁․₁: Dynamic Package Metadata Resolver (`SystemUpdateViewModel.initData`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₁․₁․₁․₁․₁: Query `packageManager.getPackageInfo()` dynamically on IO thread.
│    │    │         │    ├── BFR₁․₁․₁․₁․₂: Calculate installed package byte size via `sourceDir` file length.
│    │    │         │    └── Testing Scenarios:
│    │    │         │         ├── Pass: Resolves positive non-zero build code and human-readable byte size.
│    │    │         │         └── Fail: `NameNotFoundException` unhandled; fallback returns 0 or crashes UI.
│    │    │         ├── 🗄️ [Master DB [Offline]]₁․₁․₁․₁․₁: `system_registry.installed_version_code`
│    │    │         ├── 🗄️ [Master DB [Online]]₁․₁․₁․₁․₂: Firebase RTDB `/devices/{agentId}/version_metadata/installed_code`
│    │    │         ├── 🗄️ [Sub Master/Fact DB]₁․₁․₁․₁․₃: `version_history.installed_build_record`
│    │    │         └── 🗄️ [Transaction DB]₁․₁․₁․₁․₄: `sys_log.app_boot_version_audit`
│    │    └── 🖥️ [FE]₁․₁․₂: Silent Background Checking Toggle Affordance
│    │         ├── Validation & Conditional Rules:
│    │         │    ├── FR₁․₁․₂․₁: Synchronize checkbox with `RegistryManager.isSilentUpdateEnabled()`.
│    │         │    └── Testing Scenarios:
│    │         │         ├── Pass: Toggling state enqueues/cancels `SilentUpdateWorker` via WorkManager without blocking main thread.
│    │         │         └── Fail: Listener loop triggers recursion or duplicate scheduler workers.
│    │         └── ⚙️ [BE]₁․₁․₂․₁: WorkManager Schedule Enqueuer (`SystemUpdateScheduler.scheduleCheck`)
│    │              ├── 🗄️ [Master DB [Offline]]₁․₁․₂․₁․₁: `system_registry.silent_update_enabled`
│    │              └── 🗄️ [Transaction DB]₁․₁․₂․₁․₂: `sys_log.silent_scheduler_toggle`
│    │
│    └── 🖱️ [EL]₁․₂: Remote Release Availability Affordance
│         ├── 🖥️ [FE]₁․₂․₁: Update Status Summary Card & Differential Savings Pill
│         │    ├── Validation & Conditional Rules:
│         │    │    ├── FR₁․₂․₁․₁: When `remoteCode > currentCode`, display "NEW RELEASE READY" with remote version, code, and package size.
│         │    │    ├── FR₁․₂․₁․₂: If verified differential patch exists for this exact device build, highlight "🧬 Differential Patch (XX% Saved)".
│         │    │    ├── FR₁․₂․₁․₃: When `remoteCode == currentCode`, display "SYSTEM UP TO DATE" with last online verification timestamp.
│         │    │    ├── FR₁․₂․₁․₄: When `remoteCode < currentCode`, display "DEVELOPER TRACK (Installed Ahead of Public Release)".
│         │    │    └── Testing Scenarios:
│         │    │         ├── Pass: Accurately reflects comparison matrix without false downgrade warnings.
│         │    │         └── Fail: Inverted comparison reporting local build as obsolete when it is ahead.
│         │    └── ⚙️ [BE]₁․₂․₁․₁: Autonomous GitHub Release Scanner (`ApkInstallerHelper.scanGitHubForLatestRelease`)
│         │         ├── Logic & Business Rules:
│         │         │    ├── BFR₁․₂․₁․₁․₁: Queries `api.github.com/repos/jDroid-X/jButterfly/contents/jButterFly` directly without requiring Firebase authentication.
│         │         │    ├── BFR₁․₂․₁․₁․₂: Autonomously extracts highest build number from files matching `jButterFly_V<num>.apk` (e.g. `jButterFly_V190.apk`).
│         │         │    ├── BFR₁․₂․₁․₁․₃: Maps direct download stream directly to Git LFS media CDN (`media.githubusercontent.com/.../jButterFly_V<num>.apk`).
│         │         │    └── Testing Scenarios:
│         │         │         ├── Pass: Returns highest version code (e.g. 190) and working direct media URL returning HTTP 200 OK.
│         │         │         └── Fail: Rate-limiting triggers graceful fallback to canonical `jButterFly.apk`.
│         │         ├── 🗄️ [Master DB [Online]]₁․₂․₁․₁․₁: GitHub Repository `jDroid-X/jButterfly/jButterFly`
│         │         ├── 🗄️ [Master DB [Offline]]₁․₂․₁․₁․₂: `system_registry.latest_remote_version_code`
│         │         ├── 🗄️ [Sub Master/Fact DB]₁․₂․₁․₁․₃: `system_registry.latest_remote_package_size`
│         │         └── 🗄️ [Transaction DB]₁․₂․₁․₁․₄: `sys_log.version_check_event`
│         └── 🖥️ [FE]₁․₂․₂: Interactive Manual Scan Button ("🔄 CHECK FOR UPDATES")
│              ├── Validation & Conditional Rules:
│              │    ├── FR₁․₂․₂․₁: Shows indeterminate loading spinner (`AngelLoader`) and disables button during query.
│              │    └── Testing Scenarios:
│              │         ├── Pass: Re-enables button upon completion and updates `LAST_CHECK_TS`.
│              │         └── Fail: Button remains permanently disabled if network times out.
│              └── ⚙️ [BE]₁․₂․₂․₁: Asynchronous Scan Execution Hub (`SystemUpdateViewModel.executeUpdate`)
│                   ├── 🗄️ [Master DB [Offline]]₁․₂․₂․₁․₁: `system_registry.last_check_ts`
│                   └── 🗄️ [Transaction DB]₁․₂․₂․₁․₂: `sys_log.manual_scan_trigger`
│
├── 💼 [BR]₂: Binary Acquisition & Streaming (Download Engine)
│    ├── 🖱️ [EL]₂․₁: 1-Click Update Trigger Affordance
│    │    ├── 🖥️ [FE]₂․₁․₁: Primary "UPDATE NOW" Action Button
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₁․₁: Enabled only when `remoteCode > currentCode` or upon explicit user reinstall request.
│    │    │    │    ├── FR₂․₁․₁․₂: Automatically selects Smart Delta Differential Patch if present for device build; otherwise falls back to Full Standalone APK.
│    │    │    │    └── Testing Scenarios:
│    │    │    │         ├── Pass: Initiates download dialog with valid direct URL.
│    │    │    │         └── Fail: Initiates download with empty, null, or HTML folder URL.
│    │    │    └── ⚙️ [BE]₂․₁․₁․₁: Direct Binary URL Normalizer (`ApkInstallerHelper.resolveDirectDownloadUrl`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₂․₁․₁․₁․₁: Canonical repository mapping: Must resolve to `jDroid-X/jButterfly` media CDN (`https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jButterFly/jButterFly.apk`).
│    │    │         │    ├── BFR₂․₁․₁․₁․₂: Enforce exact case-sensitive path matching (`jButterFly` directory and `jButterFly.apk` binary) to prevent GitHub 404 responses.
│    │    │         │    ├── BFR₂․₁․₁․₁․₃: If Google Drive link provided, normalize file ID to direct binary download (`drive.usercontent.google.com/download?id=...&export=download&confirm=t`).
│    │    │         │    └── Testing Scenarios:
│    │    │         │         ├── Pass: Pre-flight HTTP HEAD returns status 200 with binary octet-stream MIME type.
│    │    │         │         └── Fail: URL returns HTTP 404 or Git LFS text pointer; triggers self-healing URL sanitizer.
│    │    │         ├── 🗄️ [Master DB [Offline]]₂․₁․₁․₁․₁: `system_registry.system_update_link`
│    │    │         ├── 🗄️ [Master DB [Offline]]₂․₁․₁․₁․₂: `system_registry.github_update_link`
│    │    │         └── 🗄️ [Transaction DB]₂․₁․₁․₁․₃: `sys_log.url_normalization_event`
│    │    └── 🖥️ [FE]₂․₁․₂: Optional Fallback Channels Button ("☁️ DRIVE MIRROR" / "🌐 BROWSER")
│    │         └── ⚙️ [BE]₂․₁․₂․₁: External Browser / Mirror Intent Launcher (`ApkInstallerHelper.openInWebBrowser`)
│    │              └── 🗄️ [Transaction DB]₂․₁․₂․₁․₁: `sys_log.external_browser_launch`
│    │
│    └── 🖱️ [EL]₂․₂: Interactive Progress & Bandwidth HUD
│         ├── 🖥️ [FE]₂․₂․₁: Glassmorphic Real-Time Progress Dialog
│         │    ├── Validation & Conditional Rules:
│         │    │    ├── FR₂․₂․₁․₁: Modal dialog non-cancelable by outside touch; cancelable only via explicit "CANCEL DOWNLOAD" button.
│         │    │    ├── FR₂․₂․₁․₂: Real-time progress bar: continuous 0% to 100% based on `contentLength`.
│         │    │    ├── FR₂․₂․₁․₃: Transferred bytes vs total bytes formatted dynamically (e.g. `12.4 MB / 65.2 MB`).
│         │    │    ├── FR₂․₂․₁․₄: Live throughput display smoothed across 500ms intervals (e.g. `3.8 MB/s`).
│         │    │    └── Testing Scenarios:
│         │    │         ├── Pass: Progress reaches 100% smoothly without UI freeze; hands off to installer.
│         │    │         └── Fail: Indeterminate state remains indefinitely; progress bar freezes or jumps backwards.
│         │    └── ⚙️ [BE]₂․₂․₁․₁: Buffered Binary Downloader (`ApkInstallerHelper.startDownloadStream`)
│         │         ├── Logic & Business Rules:
│         │         │    ├── BFR₂․₂․₁․₁․₁: Execute on `Dispatchers.IO` using 16 KB read buffer.
│         │         │    ├── BFR₂․₂․₁․₁․₂: Follow HTTP 301/302/307 redirects (maximum 10 hops).
│         │         │    ├── BFR₂․₂․₁․₁․₃: Enforce strict MIME-type guard: Abort immediately if `contentType.contains("text/html")`.
│         │         │    ├── BFR₂․₂․₁․₁․₄: Verify storage space: Require at least 150 MB free in cache before streaming.
│         │         │    ├── BFR₂․₂․₁․₁․₅: Stream cancellation: On cancel, close output stream and delete partial `.apk` immediately.
│         │         │    └── Testing Scenarios:
│         │         │         ├── Pass: Valid APK written to `cacheDir/update_jfindme.apk`.
│         │         │         └── Fail: Incomplete transfer leaves corrupted zero-byte file in cache.
│         │         ├── 🗄️ [Master DB [Offline]]₂․₂․₁․₁․₁: `system_registry.cached_apk_path`
│         │         ├── 🗄️ [Sub Master/Fact DB]₂․₂․₁․₁․₂: `system_registry.last_download_ts`
│         │         └── 🗄️ [Transaction DB]₂․₂․₁․₁․₃: `sys_log.download_stream_metrics`
│         └── 🖥️ [FE]₂․₂․₂: Differential Reconstruction Processor HUD (For Smart Delta)
│              └── ⚙️ [BE]₂․₂․₂․₁: Binary Bit-Diff Engine (`JDeltaPatcher.applyPatch`)
│                   ├── Logic & Business Rules:
│                   │    ├── BFR₂․₂․₂․₁․₁: Verify source `base.apk` SHA-256 matches delta baseline hash.
│                   │    ├── BFR₂․₂․₂․₁․₂: Apply differential reconstruction into `targetApk` using `RandomAccessFile`.
│                   │    ├── BFR₂․₂․₂․₁․₃: Verify reconstructed target APK SHA-256 against expected release hash.
│                   │    ├── BFR₂․₂․₂․₁․₄: If delta patch fails, automatically fall back to Full APK download stream.
│                   │    └── Testing Scenarios:
│                   │         ├── Pass: Reconstructed APK matches expected target hash 100%.
│                   │         └── Fail: Base hash mismatch; triggers transparent fallback to full binary.
│                   ├── 🗄️ [Sub Master/Fact DB]₂․₂․₂․₁․₁: `system_registry.patch_history`
│                   └── 🗄️ [Transaction DB]₂․₂․₂․₁․₂: `sys_log.jdelta_reconstruction_audit`
│
├── 💼 [BR]₃: Package Verification & Android OS Handshake (Installation Engine)
│    ├── 🖱️ [EL]₃․₁: Package Inspection & Security Pre-Flight
│    │    ├── 🖥️ [FE]₃․₁․₁: Cryptographic Verification Status Micro-HUD
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₃․₁․₁․₁: Display "Verifying package integrity & cryptographic seal..."
│    │    │    │    └── Testing Scenarios:
│    │    │    │         ├── Pass: Transitions directly to Android PackageInstaller.
│    │    │    │         └── Fail: Corrupt package triggers error dialog and purges cache.
│    │    │    └── ⚙️ [BE]₃․₁․₁․₁: Package Integrity & Signature Validator (`ApkInstallerHelper.isValidApkFile`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₃․₁․₁․₁․₁: Verify ZIP magic bytes (`0x50, 0x4B, 0x03, 0x04`).
│    │    │         │    ├── BFR₃․₁․₁․₁․₂: Verify package archive via `packageManager.getPackageArchiveInfo()`.
│    │    │         │    ├── BFR₃․₁․₁․₁․₃: Enforce package name match: `archive.packageName == context.packageName`.
│    │    │         │    ├── BFR₃․₁․₁․₁․₄: Enforce Version Invariant: `archive.versionCode >= installedCode` (Prevent downgrade block).
│    │    │         │    ├── BFR₃․₁․₁․₁․₅: Certificate inspection: Compare `signingInfo` / `signatures` (`MATCH` vs `MISMATCH`).
│    │    │         │    └── Testing Scenarios:
│    │    │         │         ├── Pass: Package is valid, matched, and non-downgrade.
│    │    │         │         └── Fail: Archive corrupted, package mismatch, or downgrade detected.
│    │    │         ├── 🗄️ [Master DB [Offline]]₃․₁․₁․₁․₁: `system_registry.last_verified_sha256`
│    │    │         └── 🗄️ [Transaction DB]₃․₁․₁․₁․₂: `sys_log.apk_verification_audit`
│    │    └── 🖥️ [FE]₃․₁․₂: Signature Mismatch / Clean Upgrade Guidance Dialog
│    │         ├── Validation & Conditional Rules:
│    │         │    ├── FR₃․₁․₂․₁: Shown ONLY if installed build is DEBUG while downloaded build is RELEASE (different signing keys).
│    │         │    ├── FR₃․₁․₂․₂: Offer "1-Click Clean Upgrade": Exports local settings to external migration backup, prompts removal of debug app, and launches new package.
│    │         │    └── Testing Scenarios:
│    │         │         ├── Pass: User cleanly transitions without "App not installed" OS error.
│    │         │         └── Fail: Aborts without corrupting local database.
│    │         └── ⚙️ [BE]₃․₁․₂․₁: State Migration & Backup Exporter (`ApkInstallerHelper.executeAppOverrideFlow`)
│    │              ├── 🗄️ [Master DB [Online]]₃․₁․₂․₁․₁: Firebase RTDB `/devices/{agentId}/migration_backup`
│    │              └── 🗄️ [Transaction DB]₃․₁․₂․₁․₂: `sys_log.migration_export_event`
│    │
│    └── 🖱️ [EL]₃․₂: Android OS PackageInstaller Dispatcher
│         ├── 🖥️ [FE]₃․₂․₁: System Handoff & Install Intent Dispatcher
│         │    ├── Validation & Conditional Rules:
│         │    │    ├── FR₃․₂․₁․₁: Android 8.0+ (API 26+) Unknown Sources Permission Check: If not granted, display guided permission request dialog routing to `ACTION_MANAGE_UNKNOWN_APP_SOURCES`.
│         │    │    ├── FR₃․₂․₁․₂: Secure FileProvider URI Generation: Grant explicit read URI permissions to package installer activities.
│         │    │    └── Testing Scenarios:
│         │    │         ├── Pass: Android native PackageInstaller opens showing app icon, permissions, and "Update" prompt.
│         │    │         └── Fail: SecurityException or FileUriExposedException thrown.
│         │    └── ⚙️ [BE]₃․₂․₁․₁: FileProvider & Intent Dispatch Engine (`ApkInstallerHelper.launchPackageInstaller`)
│         │         ├── Logic & Business Rules:
│         │         │    ├── BFR₃․₂․₁․₁․₁: Generate URI via `FileProvider.getUriForFile(context, "${packageName}.fileprovider", apkFile)`.
│         │         │    ├── BFR₃․₂․₁․₁․₂: Attach `FLAG_GRANT_READ_URI_PERMISSION` and `FLAG_ACTIVITY_NEW_TASK`.
│         │         │    ├── BFR₃․₂․₁․₁․₃: Query matching resolve info and grant permissions explicitly across Samsung/MIUI/AOSP packages.
│         │         │    ├── BFR₃․₂․₁․₁․₄: STRICT STATE POST-CONDITION: DO NOT mark `KEY_IS_UPDATED = true` here; defer until app restarts on new version code.
│         │         │    └── Testing Scenarios:
│         │         │         ├── Pass: Installer intent launches successfully with zero crash.
│         │         │         └── Fail: Unresolved Activity or missing FileProvider XML entry.
│         │         ├── 🗄️ [Master DB [Offline]]₃․₂․₁․₁․₁: `system_registry.is_updated`
│         │         ├── 🗄️ [Master DB [Online]]₃․₂․₁․₁․₂: Firebase RTDB `/devices/{agentId}/version_metadata/status`
│         │         ├── 🗄️ [Sub Master/Fact DB]₃․₂․₁․₁․₃: `system_registry.last_update_ts`
│         │         └── 🗄️ [Transaction DB]₃․₂․₁․₁․₄: `sys_log.installer_handoff_event`
│         └── 🖥️ [FE]₃․₂․₂: Post-Install Resume Observer (`SystemUpdateActivity.onResume`)
│              └── ⚙️ [BE]₃․₂․₂․₁: Pending Installation Resumer (`ApkInstallerHelper.resumePendingInstall`)
│                   └── 🗄️ [Transaction DB]₃․₂․₂․₁․₁: `sys_log.resume_install_audit`
│
└── 💼 [BR]₄: Domain Cleanliness & Anti-Clutter Boundary Governance
     ├── 🖱️ [EL]₄․₁: Subsystem Isolation & Purge Controls
     │    ├── 🖥️ [FE]₄․₁․₁: Single-Screen Consolidated Viewport (Zero Tabs)
     │    │    ├── Validation & Conditional Rules:
     │    │    │    ├── FR₄․₁․₁․₁: Completely remove two-tab navigation (`tabContentInstallUpdate` / `tabContentPatchLevel`); enforce unified 1-screen hierarchy.
     │    │    │    ├── FR₄․₁․₁․₂: Strict Purge of Domain Leakage: Eliminate Offline Balance Reconciliation (`reconcileYesterdayTodayBalance`) from this screen.
     │    │    │    ├── FR₄․₁․₁․₃: Strict Purge of Developer Daemons: Eliminate Local Sync Server Endpoint (`http://127.0.0.1:8085`).
     │    │    │    ├── FR₄․₁․₁․₄: Strict Purge of Background Schedulers: Eliminate Nightly Maintenance Window (02:39 AM timepicker).
     │    │    │    ├── FR₄․₁․₁․₅: Strict Purge of Cryptographic Debugger: Eliminate static 5-step pipeline stepper (`QUERY, FETCH, VERIFY, APPLY, AUDIT`).
     │    │    │    ├── FR₄․₁․₁․₆: Strict Purge of Fleet Patch Ledger: Eliminate historical build tables (e.g. `Build 61 ➔ Build 62`) and raw 64-character SHA strings.
     │    │    │    └── Testing Scenarios:
     │    │    │         ├── Pass: Activity loads in < 50ms with zero jank; only 3 clean visual cards (Hero, Update Status, Actions).
     │    │    │         └── Fail: Extraneous widgets or dead buttons remain on screen.
     │    │    └── ⚙️ [BE]₄․₁․₁․₁: Decoupled ViewModel Contract (`SystemUpdateViewModel`)
     │    │         ├── Logic & Business Rules:
     │    │         │    ├── BFR₄․₁․₁․₁․₁: Remove extraneous LiveData feeds (`pendingBalanceCount`, `localSyncEndpoint`, `maintenanceWindowTime`).
     │    │         │    ├── BFR₄․₁․₁․₁․₂: Enforce strictly ONE observer registration per LiveData stream with lifecycle awareness.
     │    │         │    ├── BFR₄․₁․₁․₁․₃: Eliminate duplicate observer registration for `showInstallPrompt`.
     │    │         │    └── Testing Scenarios:
     │    │         │         ├── Pass: Memory footprint reduced by > 40%; zero duplicate modal popups.
     │    │         │         └── Fail: Leaked background observers or memory leaks detected in LeakCanary.
     │    │         ├── 🗄️ [Master DB [Offline]]₄․₁․₁․₁․₁: `system_registry.clean_architecture_verified`
     │    │         └── 🗄️ [Transaction DB]₄․₁․₁․₁․₂: `sys_log.viewmodel_lifecycle_audit`
     │    └── 🖥️ [FE]₄․₁․₂: High-Clarity Action Controls (Scan, Update, Close)
     │         └── ⚙️ [BE]₄․₁․₂․₁: Unified Action Dispatcher (`SystemUpdateViewModel`)
     │              └── 🗄️ [Transaction DB]₄․₁․₂․₁․₁: `sys_log.ui_action_event`
```
