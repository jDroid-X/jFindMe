# Program Mapping - Master Relationship Table (Enterprise Standard v5.0)
# Antigravity Enterprise AI Agent Protocol (V3.0 Enterprise Taskforce)

## Workforce Domains & Agent Registry
1. **Workforce Domain 1: Orchestrator & Governance**
   - Agent 1: Product Architect & Hardware Identity Sentinel (SSOT Silicon Root Key: `ro.ril.oem.sno`, `ro.boot.serialno`, persistent external storage anchor `.silicon_anchor`, dynamic Settings override with Zero hardcoded device strings).
2. **Workforce Domain 2: Technology & Architecture**
   - Agent 2: Dual-Engine Database & Backend Logic Architect (SQLite Schema V78 (Granular ACL permissions_mask across 17 Normalized Tables) ↔ Firebase Realtime Cloud SSOT, real-time closed-loop `ACTION_PROFILE_SYNCED` lifecycle broadcast).
   - Agent 6: GIS, Spatial & Geofencing Architect (2D Kalman filter, Haversine, Ray-Casting Polygon SafeZones).
3. **Workforce Domain 3: AI Engineering & Frontend UI/UX**
   - Agent 3: Frontend UI/UX & Lightweight Viewport Architect (UiArchitect L7 Standard: Buttons, Validated Textboxes, Switches, Toggles, Flippers, Dropdowns, Multi-choice Dialogs, Slide-in Notifications).
4. **Workforce Domain 4: Quality & Reliability Operations**
   - Agent 4: Integration, QA & Build Quarantine Agent (Build Quarantine Protocol: autonomous `./gradlew` strictly quarantined).
   - Agent 5: Crash & Silent Failure Hunter (Foreground service lifecycle, Doze mode bypass, Coroutine boundary containment).
5. **Workforce Domain 5: Operations & Performance**
   - Agent 7: Performance & Battery Optimization Agent (Dynamic telemetry intervals, adaptive motion duty-cycles, rolling storage hygiene & SQLite VACUUM).

---

## Master Screen & Component Relationship Table

| Layer (L1-L5) | Screen / Component | Architecture | Agent Owner | Dependencies | Integration Points & Relational SSOT | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **L1 Root** | `SplashActivity` | MVC Router | Agent 1 | `AuthCheck`, `SessionRouter` | Auto-login & Identity Verification | Active SSOT |
| **L1 Auth** | `AuthActivity` | MVVM | Agent 1, Agent 2 | `AuthViewModel`, `ValidationEngine` | Firebase Auth ↔ `user_identity` Table | Active SSOT |
| **L2 Spatial**| `MapActivity` | MVVM / SSOT | Agent 3, Agent 6, Agent 2 | `MapViewModel`, `MultiMarkerEngine`, `LocationEngine` | `/locations/{id}`, `/devices/{id}`, `LocationEventTable` | Active SSOT |
| **L2 Spatial**| `LiveMapDashboardActivity` | Quarantined Shell | Agent 3, Agent 4 | Canonical `MapActivity` | Quarantined to `legacy/archived_tools/android_shells/`; callers route to MapActivity | Quarantined Legacy |
| **L2 Spatial**| `MapActivity2` | Quarantined Shell | Agent 3, Agent 4 | Canonical `MapActivity` | Quarantined to `legacy/archived_tools/android_shells/`; manifest cleaned | Quarantined Legacy |
| **L2 Mesh** | `NetworkGatewayActivity` | MVVM | Agent 1, Agent 2 | `GroupViewModel`, `PairingRepository` | 2x2 Cloud & P2P Symmetrical Pairing Matrix (`device_pairings`) | Active SSOT |
| **L2 Mesh** | `GroupActivity` | Quarantined Shell | Agent 1, Agent 4 | Canonical `NetworkGatewayActivity` | Quarantined to `legacy/archived_tools/android_shells/`; SettingsActivity L759 routes to NetworkGatewayActivity | Quarantined Legacy |
| **L2 Mesh** | `FamilyFriendsActivity` | MVVM | Agent 3, Agent 2 | `FamilyFriendsViewModel`, `RosterTree` | Guardians & Family Multi-tier Roster (`MASTER_ROSTER`) | Active SSOT |
| **L2 Mesh** | `PersonDetailsActivity` | MVVM | Agent 3, Agent 1, Agent 2 | `PersonDetailsViewModel`, `ValidationEngine` | Relational Node Metadata & ACLs (`user_profile`, `user_identity`) | Active SSOT |
| **L3 Boundary**| `GeofenceActivity` | MVVM | Agent 6, Agent 3 | `GeofenceViewModel`, `GeofenceHelper` | Safe Zones (`safe_zones`, `safe_zone_user_map`) | Active SSOT |
| **L3 Boundary**| `GeofenceNotificationActivity`| MVC | Agent 6, Agent 5 | `GeofenceBroadcastReceiver` | Transition Audit Ledger (`geofence_events`) | Active SSOT |
| **L3 Trace** | `HistoryActivity` | MVVM | Agent 5, Agent 6 | `HistoryViewModel`, `LocationRepository` | Historical Trajectory Replay (`location_event`) | Active SSOT |
| **L3 Alert** | `NotifActivity` | MVVM | Agent 5, Agent 2 | `NotifViewModel`, `AlertManager` | Alerts, SOS Ingress/Egress (`alerts`) | Active SSOT |
| **L3 Alert** | `AcousticSirenActivity` | MVC | Agent 5, Agent 7 | `AudioOverrideService` | DND Override 30s Acoustic Alarm Beacon | Active SSOT |
| **L3 Health** | `HealthActivity` | MVC | Agent 7, Agent 1 | `HardwareStatusMonitor`, `CellTowerSentinel`, `SimTransportValidator` | Real-time Battery, GPS, Baseband Cellular Tower (MCC/MNC/LAC/CID), Closed-Loop Latency & Actionable Human-in-The-Loop Fix Banner | Active SSOT |
| **L3 Forensic**| `SimForensicWorker` / `CellTowerSentinel` | MVC Engine | Agent 1, Agent 6 | `TelephonyManager`, `CellInfo` | Cellular Tower (MCC/MNC/LAC/CID) Triangulation & `sim_transactions` | Active SSOT |
| **L3 Transport**| `SimTransportValidator` | Diagnostic Sentinel | Agent 4, Agent 5 | `ConnectivityManager`, `PowerManager` | 14-Condition Mobile Data, Doze & Carrier Blocking Audit | Active SSOT |
| **L3 Failover** | `TelemetryFailoverRouter` | Failover Switch | Agent 1, Agent 2, Agent 4 | `FirebaseComm`, `OfflineSyncManager` | 5-Tier Interchangeable Mesh (Cellular -> Wi-Fi -> SMS -> SQLite) | Active SSOT |
| **L3 Feedback** | `TelemetryClosedLoopSentinel` | Closed-Loop Engine | Agent 2, Agent 4, Agent 5 | `FirebaseDatabase`, `DatabaseHelper` | Bidirectional Cloud ACK, RTT Latency (<5s) & Trip-Wire Failover | Active SSOT |
| **L3 Privacy**| `PrivacyActivity` | MVC | Agent 1, Agent 5 | `SecurityEngine`, `PrivacyEngine` | Permission Matrix & Ghost Mode Locks (`system_registry`) | Active SSOT |
| **L4 Control**| `SettingsActivity` | Dual-Tab MVVM | Agent 3, Agent 7 | `RegistryManager`, `AllScreensRegistry` | Tab 1: Universal Settings (Priorities 1-8, Priority 7C: Closed-Loop ACK Timeout, Circuit Breaker Failure Threshold, SOS SMS Target), Tab 2: Modules | Active SSOT |
| **L4 Control**| `UiConfigActivity` | MVC | Agent 3 | `UiArchitect`, `ThemeManager`, `AngelLoad` | Dynamic Theme Palette, 5-Stage Sizing & Border Geometry | Active SSOT |
| **L4 Control**| `UiGalleryActivity` | MVC | Agent 3 | `UiArchitect` | Component Suite Visual Showcase | Active SSOT |
| **L4 Engine** | `BackgroundJobsConfigDialog`| 4-Tier Waterfall | Agent 7, Agent 2 | `BackgroundJobManager` | 13 Engine Workers, Periodic Cadence & Manual Execution | Active SSOT |
| **L4 Vault** | `ForensicVaultExplorerActivity`| MVC | Agent 5, Agent 2 | `DatabaseHelper` | Encrypted SQLite Forensic Inspector (17 Tables) | Active SSOT |
| **L4 Audit** | `WatchDogAuditActivity`| MVVM | Agent 5 | `WatchDogViewModel` | Process Liveness & SQLite Integrity Verifier | Active SSOT |
| **L4 Admin** | `DatabaseManagementActivity` | MVC | Agent 2, Agent 4 | `DatabaseHelper` | SQLite Schema Inspector & Table Maintenance Console | Active SSOT |
| **L4 Admin** | `FirebaseCloudMatrixActivity` | MVC | Agent 1, Agent 2 | `FirebaseComm` | Cloud Realtime Database Mesh Inspector | Active SSOT |
| **L4 Admin** | `SystemLogActivity` | MVC | Agent 5, Agent 7 | `SystemLogger` | Rolling Realtime Runtime Trace Logger (`sys_log`) | Active SSOT |
| **L5 Maint** | `SystemUpdateActivity` | MVVM | Agent 4, Agent 5 | `SilentUpdateWorker`, `ApkInstallerHelper`| Self-Healing Silent APK Updater & Hash Verifier | Active SSOT |
| **L5 Maint** | `FactoryResetActivity` | MVVM | Agent 4, Agent 1 | `FactoryResetViewModel` | Selective Purge & Cloud Recovery Console | Active SSOT |
| **L5 Support**| `HelpSupportActivity` | MVC | Agent 3 | `UserManual` | Technical Documentation & Diagnostics Gateway | Active SSOT |
| **L5 Support**| `AboutActivity` | MVC | Agent 1, Agent 3 | `BuildConfig` | Application Manifest & Build Identity Dossier | Active SSOT |

---

## Web Mission Control & Forensic Gateway Relationship Table

| Layer | Subsystem / File | Port / Protocol | Architecture | Role & Integration Points | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **W1 Gateway** | `jFindMeWeb/js/server.js` | Port 3000 (REST) | Express Proxy | Firebase Admin SDK, OTA broadcast, RTDB REST endpoints | Active SSOT |
| **W1 Gateway** | `jFindMeWeb/js/static_server.js` | Port 8080 (HTTP) | Reverse Proxy | Serves static assets, reverse-proxies `/api/*` to Port 3000 | Active SSOT |
| **W2 Shell** | `jFindMeWeb/index.html` | Port 8080 (SPA) | Tactical Radar | Main web workspace, Leaflet/Google Maps radar, audit ledger | Active SSOT |
| **W2 Orchestrator**| `jFindMeWeb/js/app.js` | ES6 Module | Master Controller | State dispatch, routing, telemetry subscriptions, terminal | Active SSOT |
| **W2 Matrix** | `jFindMeWeb/database-sync-settings.html` | Standalone HTML | 20-Screen Sync | 178 Master Parameter Dual-Engine Synchronization Console | Active SSOT |
| **W3 Models** | `DeviceModel.js`, `SettingsModel.js`, `SafeZoneModel.js` | MVC Model | Data Entities | Hardware silicon ID binding, boundary polygons, telemetry | Active SSOT |
| **W3 Controllers**| `DeviceController.js`, `MapController.js`, `SafeZoneController.js` | MVC Controller | Action Handlers | Device filtering, interrogation, map event dispatching | Active SSOT |
| **W3 Services** | `FirebaseService.js` (FBBridge), `AvatarService.js`, `SanitizerService.js` | Service Layer | Network & IO | SSE event streaming, XSS sanitization, avatar asset mapping | Active SSOT |
| **W3 Views** | `DashboardView.js`, `DeviceCardView.js`, `DeviceUserView.js`, `GeofencingView.js`, `MapView.js` | MVC View | Visual Renderers | Real-time map pins, KPI metrics, device dossier with Active Route & Baseband RSSI, SafeZones | Active SSOT |
| **W4 Release** | `apk/sync_service.py` | Port 8085 (Daemon)| Python Daemon | Automated version code increments, SHA-256 hash, GitHub push | Active SSOT |

---

## 4-Tier Chronological Background Jobs Waterfall

```
┌────────────────────────────────────────────────────────────────────────┐
│ TIER 1: 🛰️ PRIMARY TELEMETRY & CORE GPS PIPELINE (Root / Critical)     │
│ ├── JOB_TRACKING_SERVICE      : Foreground GPS Telemetry Service       │
│ ├── JOB_LOCATION_WATCHDOG     : Sentinel Process Keep-Alive Monitor    │
│ └── JOB_GEOFENCE_SENTINEL     : Hardware Geofence Perimeter Sentinel   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Sequential Dependency)
┌───────────────────────────────────▼────────────────────────────────────┐
│ TIER 2: ☁️ CLOUD SYNC & PEER STREAMING HUB (Network Synchronization)   │
│ ├── JOB_LIVE_MAP_STREAM       : Peer WebSocket RTDB Stream (Ghost-Excl)│
│ ├── JOB_PRESENCE_PULSE        : Real-time Presence Pulse & Vitals Ping │
│ ├── JOB_OFFLINE_SYNC          : Encrypted Offline SQLite Queue Flusher │
│ └── JOB_FCM_MESSAGING         : High-Priority FCM Push Sentinel        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Sequential Dependency)
┌───────────────────────────────────▼────────────────────────────────────┐
│ TIER 3: 📱 FORENSIC HARDWARE & SECURITY (Passive Hardware Interceptors)│
│ ├── JOB_SECURITY_RECEIVER     : Power, Boot, Flight Mode Interceptor   │
│ ├── JOB_ACOUSTIC_SIREN        : Emergency Beacon & Audio Bridge        │
│ └── JOB_SIM_FORENSIC          : SIM & Cellular Tower Hardware Sentinel │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Sequential Dependency)
┌───────────────────────────────────▼────────────────────────────────────┐
│ TIER 4: ⚡ STORAGE PRUNING & SYSTEM INTEGRITY (Maintenance & Hygiene)   │
│ ├── JOB_STORAGE_PRUNE         : Rolling Retention Purge & SQLite VACUUM│
│ ├── JOB_SILENT_UPDATE         : Binary Hash Check & Update Sentinel    │
│ └── JOB_APP_UPGRADE_WATCH     : Post-Install Auto-Restart Sentinel     │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Quarantined Legacy & Scratch Inventory (Prime Directive 1 Compliance)

All obsolete prototypes, temporary test files, and historical binaries have been safely relocated to `legacy/` rather than deleted, guaranteeing 100% auditability without polluting the active production tree:

1. **`legacy/archived_tools/`**:
   - `android_shells/`: Quarantined redirect shells (`MapActivity2.java`, `LiveMapDashboardActivity.java`, `GroupActivity.java`). Callers modernized to canonical `MapActivity` and `NetworkGatewayActivity`.
   - `diagnostics/`: 12 one-time diagnostic node scripts (`purge_all_database.js`, `cleanup_lookups.js`, `cleanup_database_gaps.js`, `check_fcm.js`, `sync_vibgyor.js`, `inspect_devices.js`, `inspect_versions.js`, `check_all_roots.js`, `check_device_geofences.js`, `check_device_timestamps.js`, `diagnose_devices.js`).
   - `unity_avatar_prototypes/`: 7 C# Unity avatar scripts (`AvatarAnimationController.cs`, `AvatarController.cs`, `AvatarLoader.cs`, `AvatarLoadingPipeline.cs`, `AvatarMapPositioner.cs`, `AvatarProfileRenderer.cs`, `ModularAvatarAssembler.cs`).
   - `html_sync/`: Obsolete v1 `jButterflyGithubSync.html` (superseded by `jButterflyGithubSync_v2.html`).
   - `launchers/`: Obsolete `jFindMeWeb/launcher.hta` and hardcoded ADB test script `scripts/auto_reinstall_device.bat`.
   - `root_engine_duplicate/`: Contains orphaned `MarkerClusterer.kt` duplicate from root.
   - `web_enterprise_orphans/`: Contains unimported `js/enterprise/` (`App.js`, `DataGrid.js`, `SchemaView.js`, `Store.js`).
   - `web_prototypes/`: Contains dead prototype island (`MainController.js`, `DataRenderer.js`, `ModalUI.js`, `DatabaseProvider.js`, `OptimizedLeafletMap.js`, `MapPerformance.js`, `server-settings-api.js`, `GoogleFindHubView.js`).
   - `download.py`, `find_img.py`, `inspect_gdrive.py`, `drive_file.html`, `process_avatars.py`: One-off asset scraping scripts.
   - `_temp_script_0.js`: Extracted APK scratch script.
   - `watchdog_dog.png`: Unused root image.

2. **`legacy/archived_scratch/`**:
   - `root_avatar_backup/`: Quarantined root `Avatar/` (51 duplicate loose PNG/JPG/WebP files, preserved under Chesterton's Fence; web serves from `jFindMeWeb/assets/avatars/`).
   - `web_scratch/`: Quarantined `jFindMeWeb/scratch/` (including 19.65 MB enterprise repair backup snapshot & 32 scratch scripts).
   - `root_scratch/`: Quarantined `scratch/jbutterfly/` (~285 MB old APKs) & `scratch/backup_db/` (2.7 MB DB).

3. **`legacy/jButterFly/`**:
   - Complete historical reference project tree preserved under Chesterton's Fence.
