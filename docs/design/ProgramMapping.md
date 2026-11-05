# ProgramMapping - Master Relationship Table (Enterprise Standard v4.2)

| Main Program (L1) | Sub Program (L2) | Sub Program (L3) | Sub Program (L4) | Sub Program (L5) | Responsibility |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **jFindMe App** | **UI Engine (Java)** | `BaseActivityJava` | `ThemeManager` | `ThemePalette` | Holistic UI Styling & 8 Themes |
| | | `SplashActivity` | `AuthCheck` | `SessionRouter` | Auto-login & Session Verification |
| | | `AuthActivity` | `LoginForm` | `AuthViewModel` | Secure Credential & Identity Gateway |
| | | `MapActivity` | `MultiMarkerEngine` | `MapViewModel` | Core Spatial Map Tracking & Primary View (Top 1) |
| | | `NetworkGatewayActivity`| `PairingMatrix` | `GroupViewModel` | 2x2 Cloud & P2P Symmetrical Pairing Matrix (Top 2) |
| | | `FamilyFriendsActivity` | `RosterTree` | `FamilyFriendsViewModel` | Guardians & Family Multi-tier Roster (Top 6) |
| | | `PersonDetailsActivity` | `ProfileEditor` | `PersonDetailsViewModel` | Relational Node Metadata & ACLs |
| | | `ManagementCluster`| `ProfileActivity` | `ProfileViewModel` | Identity, 3D Avatar & Dossier (Top 4) |
| | | | `GeofenceActivity` | `GeofenceViewModel` | Interactive Safe Zones & Boundaries (Top 5) |
| | | | `HistoryActivity` | `HistoryViewModel` | Traveled Route Breadcrumbs & Replay |
| | | | `NotifActivity` | `NotifViewModel` | Alert Config, SOS & Incoming Pokes |
| | | | `HealthActivity` | `HardwareStatus` | Battery, GPS, Network & Storage Stats |
| | | | `PrivacyActivity` | `SecurityEngine` | Permission Matrix & Ghost Mode Locks |
| | | | `SystemLogActivity` | `AuditLogger` | Rolling Realtime Runtime Trace Logger |
| | | | `WatchDogAuditActivity`| `WatchDogViewModel` | Process Liveness & SQLite Verifier |
| | | | `FactoryResetActivity` | `FactoryResetViewModel`| Selective Purge & Cloud Recovery |
| | **Control Hub (Dual-Tab)**| `SettingsActivity` | `UniversalSettingsTab` | `RegistryManager` | **Tab 1: Universal Settings** (Priorities 1-8, Parent/Child Cascade) |
| | | | `ScreenModulesTab` | `AllScreensRegistry` | **Tab 2: Screen Modules** (7 Grouped Navigation Modules) |
| | | | `UiConfigActivity` | `UiArchitect` | Master Visual Styling, Stages, Borders & Shadows |
| | | | `BackgroundJobsConfigDialog`| `BackgroundJobManager`| 12 Engine Workers, Periodic Cadence & Manual Execution |
| | **Location Engine (Ktx)**| `TrackingService` | `LocationEngine` | `ActivityRecognitionSentinel` | Hardware Motion Co-Processor & Battery Sentinel |
| | | `MultiMarkerEngine` | `RoadSmoothingEngine` | `SphericalUtil / Kalman` | 2D Kalman Road Snapping & Heading Azimuth |
| | | `SIMTrackingService` | `SimForensicWorker` | `WorkManager` | Silent Hardware & SIM Audit Engine |
| | **Data Engine (Local/Remote)**| `DatabaseHelper` | `17 Normalized Tables`| `SQLiteOpenHelper` | Single Source of Truth Local DB |
| | | `FirebaseComm` | `LocationRepository` | `OfflineSyncManager` | Closed-loop Cloud Realtime Sync |
| | | `AvatarSyncEngine` | `AvatarLoader` | `CalloutGenerator` | One-Click Closed-Loop Avatar Propagation |

## SettingsActivity Dual-Tab Architecture Taxonomy:

### Tab 1: 🌐 Universal Settings (Priority 1 to 8 Cascade):
1. **Header Actions**: Dynamic Theme Switcher (0-7 Cycle) & **Restore Recommended Defaults** (one-tap factory recovery of 18 engine thresholds with in-place label mutation).
2. **Card 1 (Priority 1 - Critical Security)**: Child / Ward Protection Mode (Switch with anti-desync zero-flicker PIN guard), Guardian Master PIN (SHA-256), Location Pause Window (Editable Custom: 5m-1440m), Pairing PIN Expiry TTL (Editable Custom: 1m-120m NIST), Master Purge Safety Password (`jbutterfly@123`).
3. **Card 2 (Priority 2 - Telemetry Core)**: Battery Optimizer & Ping Hub (Manual Stepper 90s+ or Presets: 1s, 7s, 15s, 30s, 60s), Hardware Motion Co-Processor (Parent Switch: Zero Still GPS), Motion Duty Cycles (Child Container: Still 300s, Vehicle 10s, Walk 20s), Minimum Displacement Filter (Editable Custom: 0m-1000m), Strict GNSS Accuracy Gate (`< 10m`).
4. **Card 3 (Priority 3 - Spatial Kinematics)**: Road Alignment & 2D Kalman Filter (Parent Switch), Kalman Measurement Noise $R=25$ & Heading EMA Factor $0.35$ (Child Dialog).
5. **Card 4 (Priority 4 - Geospatial Geofence)**: Perimeter Jitter Debounce Filter (Editable Custom: 0s-300s), Dormancy State Latch (Anti-Duplicate), Centroid GPS Snapping on Entry, Local Audio/Banner Alert, Cloud FCM Push Relay, Default Safe Zone Radius (Editable Custom: 10m-5000m).
6. **Card 5 (Priority 5 - Health Engine)**: Low Battery Threshold (Editable Custom: 5%-50%), Inactive Stale Threshold Amber (Editable Custom: 1m-1440m), Critical Offline Threshold Red (Editable Custom: 1h-168h), Distant Device Panel Threshold (Editable Custom: 20m-50000m).
7. **Card 6 (Priority 6 - Emergency Protocols)**: Siren Auto-Silence Duration (Editable Custom: 5s-600s), Continuous Haptic Vibration, DND Audio Override, SOS Confirmation Countdown (Editable Custom: 0s-60s), SOS Cooldown Window (Editable Custom: 5s-600s), SOS Ack Timeout (Editable Custom: 1m-180m), SOS Fan-Out Scope, Explicit Confirmation Switch, Master PIN Cancel Switch.
8. **Card 7 (Priority 7 - Resilience & Sentinels)**: Offline GPS Queue Capacity (Editable Custom: 50-5000 points), Deferred Sync on Wi-Fi Only Switch, 12 Background Jobs & Engine Sentinels Launcher (`BackgroundJobsConfigDialog`).
9. **Card 8 (Priority 8 - UX & Aesthetics)**: Distance Unit Metric (Metric km/h vs Imperial mph), Smart Avatar Clustering (10m Proximity), Global UI & Icon Architect (`UiConfigActivity`), Theme & Components Gallery (`UiGalleryActivity`).

### Tab 2: 📱 Screen Modules Directory (7 Grouped Modules):
1. **Module 1 (Tactical Live Maps)**: `MapActivity` (Primary Vector Map), `LiveMapDashboardActivity` (Tactical Radar HUD).
2. **Module 2 (Circle Mesh & Pairing)**: `NetworkGatewayActivity` (Mesh Gateway & QR), `FamilyFriendsActivity` (Family & Guardians Roster), `GroupActivity` (Circle Groups Partition), `PersonDetailsActivity` (Member Dossier & ACL).
3. **Module 3 (Identity, Avatar & Governance)**: `AuthActivity` (Authentication Gateway), `ProfileActivity` (Pupil Profile Architecture), `AvatarEditorActivity` (3D VRM Model Customizer), `AvatarCropActivity` (Biometric Photo Crop), `OrgActivity` (Enterprise Organization Domain).
4. **Module 4 (Geospatial & Safe Zones)**: `GeofenceActivity` (Safe Zone Manager), `GeofenceNotificationActivity` (Transition Audit Ledger), `HistoryActivity` (Historical Location Logs & Replay).
5. **Module 5 (Privacy, Health & Alerts)**: `PrivacyActivity` (Identity Security Protocol), `HealthActivity` (Device Health & Vitals), `NotifActivity` (Notification Routing & Triggers), `AcousticSirenActivity` (Emergency Acoustic Siren Beacon).
6. **Module 6 (Forensics, Audit & Database)**: `ForensicVaultExplorerActivity` (Encrypted Offline Vault), `WatchDogAuditActivity` (WatchDog Sentinel Audit), `DatabaseManagementActivity` (17 SQLite Tables Vacuum & Index), `FirebaseCloudMatrixActivity` (Realtime Database Health), `SystemLogActivity` (Rolling Event Traces).
7. **Module 7 (Maintenance, Update & Purge)**: `SystemUpdateActivity` (Silent APK Update), `FactoryResetActivity` (Purge Console & Teardown), `HelpSupportActivity` (Help & Support Center), `AboutActivity` (About jFindMe & Manifest).

## Navigation Drawer Standard Hierarchy:
1. **Live Map**: Real-Time Spatial Grid (Top / Default View)
2. **Pairing Devices**: `NetworkGatewayActivity` (Cloud PIN & P2P Token 2x2 Matrix)
3. **Dashboard**: Overview & Telemetry HUD
4. **Profile**: `ProfileActivity` (3D Avatar & Identity Dossier)
5. **Safe Zones**: `GeofenceActivity` (Interactive Boundary Geofences)
6. **Guardians / Family**: `FamilyFriendsActivity` (Family & Friends Dual Sub-tab)
7. **Location History**: `HistoryActivity` (Route Playback & Breadcrumbs)
8. **Notifications**: `NotifActivity` (Alerts, Geofence Ingress/Egress, SOS)
9. **Device Health**: `HealthActivity` (Battery, Hardware & GPS Telemetry)
10. **Settings**: `SettingsActivity` (Dual-Tab Control Hub: Universal Settings & Screen Modules)
11. **Privacy Controls**: `PrivacyActivity` (Ghost Mode, Permissions Matrix)
12. **Help & Support**: `HelpSupportActivity` (FAQs, Documentation)
13. **About App**: `AboutActivity` (Architecture & Build Info)
14. **Logout**: Secure Account Sign-Out & Session Tear-Down

## Closed-Loop Feedback & Data Flow:
1. **User Profile / Avatar Sync**: `ProfileActivity` / `AvatarEditorActivity` -> `AvatarSyncEngine` -> Local DB (`user_profile`) -> Firebase RTDB (`devices/{agentId}/profile`) -> `NetworkGatewayActivity` & `MapActivity` -> Instant Closed-Loop Propagation.
2. **Device Pairing Flow**: `NetworkGatewayActivity` (QR Scan / 6-Digit PIN / 8-Digit Token) -> `GroupViewModel` -> `DatabaseHelper` (`device_pairings`, `MASTER_ROSTER`) -> `LocationRepository` (dynamic listener) -> `MapActivity` live track.
3. **Safe Zone Flow**: `GeofenceActivity` -> `GeofenceViewModel` -> `DatabaseHelper` (`safe_zones`, `safe_zone_user_map`) -> `GeofenceBroadcastReceiver` -> Notification & Siren.
4. **Historical Trajectory Replay (Offline-First)**: `HistoryActivity` -> `HistoryViewModel` -> `LocationRepository` -> `DatabaseHelper.getRawHistoryPoints()` -> Instant zero-latency local playback, with closed-loop background merge when authenticated.
5. **Pre-Uninstall Teardown Protocol**: `FactoryResetActivity` -> `FactoryResetViewModel.executeFullSystemTeardown()` -> Halts foreground services, unregisters OS geofences, cancels scheduled workers, revokes FCM cloud tokens, and purges local SQLite tables cleanly.
6. **Navigation Flow**: Zero Dead-Ends; all drawer and settings items map directly to functional activities with instant back-stack resolution.
7. **Screen Settings & SSOT Matrix**: See [SCREEN_SETTINGS_AND_CONFIGURATIONS.md](SCREEN_SETTINGS_AND_CONFIGURATIONS.md) for the master mapping of all 32 screens, configurable parameters, and closed-loop cross-screen database synchronization.
8. **Device Location Tracking & Dual-Engine Telemetry Matrix**: See [DEVICE_LOCATION_TRACKING.md](DEVICE_LOCATION_TRACKING.md) for the master specification on SIM-First Background Telemetry, GPS Foreground-Only Restriction, Cross-OEM Whitelist Bridge (Top 20 Brands & Realme Deep-Dive), and Parent-Child Anti-Tamper (`PROVIDERS_CHANGED_ACTION` / `alert_gps_off`) Alert Protocols.
9. **Family Tracking Stealth & Anti-Evasion Coordinate Tree**: See [FAMILY_TRACKING_STEALTH_FAILOVER_COORDINATE_TREE.md](FAMILY_TRACKING_STEALTH_FAILOVER_COORDINATE_TREE.md) for the master coordinate tree (`ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB`) defining the "Alarm Buzz" Autonomous Scheduled Wakeup (`AlarmManager.setAlarmClock()`), SIM Tower Cellular Failover, 5-Tier Hardware Cascade (GPS ➔ Wi-Fi ➔ SIM Tower ➔ SMS ➔ SQLite), 60-Minute Default Dormant Battery Shield, and Smart Kid Anti-Evasion Hardening.
