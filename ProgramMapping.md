# ProgramMapping - Master Relationship Table (Enterprise Standard v4.1)

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
| | | | `SystemLogActivity` | `AuditLogger` | 7-Day HTML Audit Log Viewer |
| | | | `WatchDogAuditActivity`| `WatchDogViewModel` | 14-Table SQLite Forensic Verifier |
| | | | `FactoryResetActivity` | `FactoryResetViewModel`| Selective Purge & Cloud Recovery |
| | | | `AboutActivity` | `HelpSupportActivity`| System Documentation & Diagnostics |
| | **Location Engine (Ktx)**| `TrackingService` | `LocationEngine` | `FusedLocationClient` | Background GPS Telemetry Engine |
| | | `SIMTrackingService` | `SimForensicWorker` | `WorkManager` | Silent Hardware & SIM Audit Engine |
| | **Data Engine (Local/Remote)**| `DatabaseHelper` | `14 Normalized Tables`| `SQLiteOpenHelper` | Single Source of Truth Local DB |
| | | `FirebaseComm` | `LocationRepository` | `OfflineSyncManager` | Closed-loop Cloud Realtime Sync |
| | | `AvatarSyncEngine` | `AvatarLoader` | `CalloutGenerator` | One-Click Closed-Loop Avatar Propagation |

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
10. **Settings**: `SettingsActivity` (Theme, Unit & Map Configuration)
11. **Privacy Controls**: `PrivacyActivity` (Ghost Mode, Permissions Matrix)
12. **Help & Support**: `HelpSupportActivity` (FAQs, Documentation)
13. **About App**: `AboutActivity` (Architecture & Build Info)
14. **Logout**: Secure Account Sign-Out & Session Tear-Down

## Closed-Loop Feedback & Data Flow:
1. **User Profile / Avatar Sync**: `ProfileActivity` / `AvatarEditorActivity` -> `AvatarSyncEngine` -> Local DB (`user_profile`) -> Firebase RTDB (`devices/{agentId}/profile`) -> `NetworkGatewayActivity` & `MapActivity` -> Instant Closed-Loop Propagation.
2. **Device Pairing Flow**: `NetworkGatewayActivity` (QR Scan / 6-Digit PIN / 8-Digit Token) -> `GroupViewModel` -> `DatabaseHelper` (`device_pairings`, `MASTER_ROSTER`) -> `LocationRepository` (dynamic listener) -> `MapActivity` live track.
3. **Safe Zone Flow**: `GeofenceActivity` -> `GeofenceViewModel` -> `DatabaseHelper` (`safe_zones`, `safe_zone_user_map`) -> `GeofenceBroadcastReceiver` -> Notification & Siren.
4. **Navigation Flow**: Zero Dead-Ends; all drawer items map directly to functional activities with instant back-stack resolution.
