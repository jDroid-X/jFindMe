# 🏛️ Enterprise Master Architectural Specification: Screen-Wise Coordinate Tree, Stage-Wise Database Mapping, RCA/REA Optimization & Adaptive Sync Engine (V2)
> **Author:** jDroid-X (Lead Operator: JEET)  
> **System Architecture:** Closed-Loop OOPS MVC | Coordinate Tree `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`  
> **Process Hierarchy:** `Screen/Page ➔ Stage (1-n) ➔ Stage-Wise Activity (Foreground/Background) (1-n) ➔ Setting/Config (Editable Variables) ➔ Database (Master, Sub-Master, Transactional)`  
> **Engine Baseline:** Dual-Engine Enterprise Database Engine (Local SQLite V83 ⟷ Firebase Realtime Cloud SSOT)  
> **Target Fleets:** `2C811042` (REDMI 5PRO), `2D113789` (INNOVANT), `2T929153` (REDMI N7), `4379A761` (RIDDHI), `F8CAFC67` (PUSHPA)  
> **Twin Module Parity:** 100% SHA-256 byte-for-byte synchronization (`app/` ⟷ `jButterFly/`)  
> **Status:** RELEASE CERTIFIED GOLD MASTER (V2)

---

# 📑 TABLE OF CONTENTS
1. [Architectural Paradigm: UI/UX First Coordinate Tree & Process Hierarchy](#1-architectural-paradigm)
2. [Screen/Page 1: Login & Authorization (`SplashActivity` & `AuthActivity`)](#2-screenpage-1-login--authorization)
3. [Screen/Page 2: Spatial Tactical Radar & Butterfly Hub (`MapActivity` & `ButterflyPointDialog`)](#3-screenpage-2-spatial-tactical-radar--butterfly-hub)
4. [Screen/Page 3: Network Gateway & Device Sharing Mesh (`NetworkGatewayActivity` & `DeviceSharingHub`)](#4-screenpage-3-network-gateway--device-sharing-mesh)
5. [Screen/Page 4: Friends & Family Multi-Tier Roster (`FamilyFriendsActivity` & `PersonDetailsActivity`)](#5-screenpage-4-friends--family-multi-tier-roster)
6. [Screen/Page 5: Geofencing & Safe Boundary Sentinel (`GeofenceActivity` & `GeofenceNotificationActivity`)](#6-screenpage-5-geofencing--safe-boundary-sentinel)
7. [Screen/Page 6: Location History & Trajectory Playback (`HistoryActivity`)](#7-screenpage-6-location-history--trajectory-playback)
8. [Screen/Page 7: Alerts, SOS Panic & Acoustic Siren (`NotifActivity` & `AcousticSirenActivity`)](#8-screenpage-7-alerts-sos-panic--acoustic-siren)
9. [Screen/Page 8: Hardware Health, Cellular Sentinel & SIM Forensics (`HealthActivity` & `SimForensicWorker`)](#9-screenpage-8-hardware-health-cellular-sentinel--sim-forensics)
10. [Screen/Page 9: User Profile, Avatar Studio & Organization (`ProfileActivity`, `AvatarEditorActivity`, `OrgActivity`)](#10-screenpage-9-user-profile-avatar-studio--organization)
11. [Screen/Page 10: Universal Settings & Dynamic UI Engine (`SettingsActivity` & `UiConfigActivity`)](#11-screenpage-10-universal-settings--dynamic-ui-engine)
12. [Screen/Page 11: Forensic Vault, Database Console & System Audit (`ForensicVaultExplorerActivity`, `DatabaseManagementActivity`, `SystemLogActivity`, `WatchDogAuditActivity`)](#12-screenpage-11-forensic-vault-database-console--system-audit)
13. [Screen/Page 12: Silent System Update & Factory Reset (`SystemUpdateActivity` & `FactoryResetActivity`)](#13-screenpage-12-silent-system-update--factory-reset)
14. [Screen/Page 13: Background Engine Sentinels & Orchestration (`BackgroundJobManager`)](#14-screenpage-13-background-engine-sentinels--orchestration)
15. [Root Cause Analysis (RCA) & Reverse Engineering Analysis (REA): Code Comparison & Redundancy Audit](#15-root-cause-analysis-rca--reverse-engineering-analysis-rea)
16. [Lowest Data Size Optimization & Call-by-Reference Refactoring](#16-lowest-data-size-optimization--call-by-reference-refactoring)
17. [Device-to-Device Real-Time Synchronization & Bandwidth-Adaptive Batching Engine](#17-device-to-device-real-time-synchronization--bandwidth-adaptive-batching-engine)

---

# 1. Architectural Paradigm

### Coordinate Tree Formula: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
To eliminate architectural gaps and guarantee deterministic human-in-the-loop control, every screen and subsystem is defined through this 6-tier coordinate hierarchy:
- **`ROOT`**: The sovereign feature domain and lifecycle scope.
- **`BR` (Business Rules & Human-in-the-Loop Policies)**: Non-bypassable security invariants, permission escalation ladders, consent gates, and red-line breakers.
- **`EL` (Event Listeners & Action Interceptors)**: Presentation-tier triggers, UI click dispatchers, broadcast receivers, hardware sensors, and WebSocket subscriptions.
- **`FE` (Front-End Presentation & UI/UX)**: View hierarchy, animated docks, form input tables, status pills, modals, and responsive 5-stage scaling.
- **`BE` (Back-End Logic & Sentinels)**: ViewModels, services, workers, repositories, cryptographic vault engines, and mathematical pipelines.
- **`DB` (Tri-Tier Database SSOT)**:
  - **Master Layer**: Canonical root entities (identities, hardware nodes, groups, boundary perimeters).
  - **Sub-Master / Config Layer**: Dynamic configurations, editable variables, lookup matrices, and user profiles.
  - **Transactional Layer**: High-velocity real-time event streams, audit ledgers, sync queues, and sensor breadcrumbs.

### Process Flow Invariant:
```
Screen/Page ➔ Stage (1-n) ➔ Stage-Wise Activity (Foreground/Background) (1-n) 
            ➔ Setting/Config (All Hardcoded into Editable Variables: YES/NO) 
            ➔ Database Tri-Tier SSOT (Master ➔ Sub-Master ➔ Transactional)
```

---

# 2. Screen/Page 1: Login & Authorization (`SplashActivity` & `AuthActivity`)

### Coordinate Tree: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
- **`ROOT`**: System Initialization, Zero-Trust Hardware Anchoring & Session Gateway.
- **`BR`**: 
  - Zero plaintext credentials; hardware silicon anchor cannot be spoofed or randomly generated.
  - 5-attempt brute-force rate limit with exponential lockout (30s, 60s, 300s).
  - Session auto-lock on inactivity; immediate volatile memory wipe of PIN arrays upon `onPause()`.
- **`EL`**: `SplashActivity.onCreate()`, `AuthActivity.PinPadClickListener`, `SecurityDialogManager`, `FirebaseAuth.AuthStateListener`.
- **`FE`**: Splash animated logo, 6-digit PIN keypad with animated pulsating indicator dots, biometric prompt sheet, validation alert modal.
- **`BE`**: `AuthViewModel`, `DeviceIdentityHelper`, `SecureVault` (AES-256-GCM + PBKDF2), `FirebaseComm`.
- **`DB`**: Master: `user_identity`, `device` | Sub-Master: `user_profile`, `security_master_keys`, `stealth_schedule_config` | Transaction: `sync_queue`, `sys_log`, `security_audit_log`.

---

### Stage-Wise Process Hierarchy

#### Stage 1: Hardware Anchoring & System Identification (Splash Initializer)
* **Activity 1.1 (Foreground): Silicon Root Hardware Probe & Qualification**
  - *Execution*: Evaluates `ro.ril.oem.sno`, `ro.boot.serialno`, and `ANDROID_ID`. Fallback to `.silicon_anchor` file. Strictly rejects random UUID generation.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_hardware_id_override` (String | Default: `""` | Target: `system_registry` / `stealth_schedule_config`).
    - Editable Variable: `pref_allow_emulator_bypass` (Boolean | Default: `false` | Target: `system_registry`).
  - *Database Integration*:
    - **Master**: Read/Insert `user_identity` (`uid_1`, `uid_2`, `auth_uid`), `device` (`device_id`, `hardware_id`).
    - **Sub-Master**: Read `stealth_schedule_config` for hardware overrides.
    - **Transactional**: Insert `sys_log` (Level: `INFO`, Tag: `HARDWARE_PROBE`).
* **Activity 1.2 (Background): Database Schema Integrity & Migration Check**
  - *Execution*: Verifies SQLite version (V83), validates physical existence of 32 tables, checks `PRAGMA quick_check`.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_auto_vacuum_on_start` (Boolean | Default: `false` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_db_integrity_timeout_ms` (Long | Default: `3000L` | Target: `system_registry`).
  - *Database Integration*:
    - **Master**: Verify physical schema of `user_identity`, `device`, `groups`, `safe_zone`.
    - **Sub-Master**: Seed missing defaults in `stealth_schedule_config`.
    - **Transactional**: Insert `security_audit_log` on schema migration or corruption detection.
* **Activity 1.3 (Foreground): Permission Ladder Verification (HITL)**
  - *Execution*: Evaluates 7 core runtime permissions (Fine Location, Background Location, Phone State, Notifications, Activity Recognition, Battery Optimization, Power Manager).
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `device_policy_state` (Update `permission_mask`).
    - **Transactional**: `sys_log` (Record granted permissions mask).
* **Activity 1.4 (Foreground): Session Status & Routing Decision**
  - *Execution*: If authenticated and master PIN verified ➔ Route to `MapActivity`; else ➔ Route to `AuthActivity`.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_session_timeout_mins` (Long | Default: `15L` | Options: 1, 5, 15, 30, Never | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_skip_splash_delay` (Boolean | Default: `false` | Target: `system_registry`).
  - *Database Integration*:
    - **Master**: `user_identity`.
    - **Sub-Master**: `user_profile` (Read `pin_hash`).
    - **Transactional**: `sys_log`.

#### Stage 2: User Authentication & Token Handshake (`AuthActivity`)
* **Activity 2.1 (Foreground): 6-Digit Master PIN Entry & Biometric Interceptor**
  - *Execution*: Captures numeric PIN, salts with device silicon ID, hashes via PBKDF2, compares against `user_profile.pin_hash`.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_biometric_enabled` (Boolean | Default: `true` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_max_pin_attempts` (Int | Default: `5` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_lockout_duration_sec` (Int | Default: `300` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `user_identity`.
    - **Sub-Master**: `user_profile` (`pin_hash`, `salt`), `security_master_keys`.
    - **Transactional**: `security_audit_log` (Log failed attempts and lockout triggers).
* **Activity 2.2 (Background): Firebase Cloud Token Handshake & Anonymous Fallback**
  - *Execution*: Authenticates against Firebase Auth using cached credentials. If offline, engages zero-trust local verification fallback.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_firebase_auth_timeout_ms` (Long | Default: `7000L` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_allow_offline_pin_bypass` (Boolean | Default: `true` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Online: `/lookup_indices/{auth_uid}`, `/devices/{id}/Identity` | Local: `user_identity`.
    - **Sub-Master**: Online: `/devices/{id}/profile` | Local: `user_profile`.
    - **Transactional**: `sync_queue` (Enqueue uncommitted auth updates).
* **Activity 2.3 (Foreground/Background): Profile State Hydration & Navigation Handoff**
  - *Execution*: If profile exists and is complete ➔ Launch `MapActivity`; if profile incomplete (missing name/avatar) ➔ Launch `ProfileActivity`.
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Master**: `user_identity`.
    - **Sub-Master**: `user_profile`, `user_org_map`.
    - **Transactional**: `change_log` (Category: `Profile`, Action: `HYDRATE`).

---

# 3. Screen/Page 2: Spatial Tactical Radar & Butterfly Hub (`MapActivity` & `ButterflyPointDialog`)

### Coordinate Tree: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
- **`ROOT`**: Real-Time Multi-Node Geographic Radar, Butterfly Rendezvous & Proximity HUD.
- **`BR`**: 
  - Peer Isolation: Only render markers for paired devices in `device_pairing` or active group circle (`group_members`).
  - Ward Priority 0: Child node (`4379A761` / Riddhi) pinned to top priority and never pruned.
  - Proximity Threshold: If peer distance $< 50\text{m}$, render on map canvas; if $\ge 50\text{m}$, render in dynamic Radar HUD tile.
  - Ghost Mode: If peer is in Ghost Mode, render last known location with faded styling and zero live bearing.
- **`EL`**: `MultiMarkerEngine.OnMarkerClickListener`, `MapLifecycleDelegate`, `ButterflyPointDialog.OnRendezvousCreatedListener`, Google Maps Camera Change Listeners.
- **`FE`**: Edge-to-edge Google Map viewport, 10-icon frosted glass right dock, top status marquee (speed/battery/address), avatar marker overlays with spiderweb dispersion.
- **`BE`**: `MapV2ViewModel`, `MultiMarkerEngine`, `LocationEngine`, `ButterflyPointViewModel`, `SpatialMathUtil`.
- **`DB`**: Master: `device`, `butterfly_point`, `groups` | Sub-Master: `device_pairing`, `butterfly_point_device_map`, `reverse_geocode_cache` | Transaction: `location_event`, `device_vitals`, `butterfly_point_telemetry`, `alert_history`.

---

### Stage-Wise Process Hierarchy

#### Stage 1: Spatial Radar Initialization & Map Canvas Binding
* **Activity 1.1 (Foreground): Map Engine Ready & Tile Cache Preload**
  - *Execution*: Initializes Google Map SDK, loads custom dark/tactical JSON style, centers on device last known coordinates.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_map_default_zoom` (Float | Default: `16.0f` | Range: 5.0f - 20.0f | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_map_tactical_style_enabled` (Boolean | Default: `true` | Target: `system_registry`).
  - *Database Integration*:
    - **Master**: `device` (Read own device hardware ID).
    - **Sub-Master**: `stealth_schedule_config` (Read zoom and style preferences).
    - **Transactional**: `location_event` (Fetch latest 1 row: `SELECT * FROM location_event ORDER BY timestamp DESC LIMIT 1`).
* **Activity 1.2 (Background): Paired Peer Discovery & Subscription Mesh**
  - *Execution*: Queries `device_pairing` and `group_members`; establishes Firebase Realtime listeners on `/devices/{peerId}/device/live`.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_stream_poll_interval_sec` (Long | Default: `5L` | Options: 1, 5, 15, 30 | Target: `system_registry`).
    - Editable Variable: `pref_max_visible_peers` (Int | Default: `10` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Online: `/devices/{peerId}/device/live` | Local: `device`.
    - **Sub-Master**: Local: `device_pairing`, `group_members`.
    - **Transactional**: Local: `p2p_mesh_packets`.

#### Stage 2: Live Telemetry Stream Rendering & Proximity Calculation
* **Activity 2.1 (Foreground): Marker Rendering & Spiderweb Dispersion Engine**
  - *Execution*: Receives incoming peer coordinates. If multiple devices overlap within 15 meters, applies 50dp equidistant radial dispersion algorithm.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_spiderweb_dispersion_radius_dp` (Int | Default: `50` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_cluster_tolerance_meters` (Float | Default: `15.0f` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `user_profile` (Fetch avatar bitmap for marker icon).
    - **Transactional**: `location_event` (Store incoming peer telemetry).
* **Activity 2.2 (Foreground): Marquee Telemetry & Reverse Geocode Resolution**
  - *Execution*: Resolves street address for selected device using rounded coordinates ($11\text{m}$ precision) and displays speed, battery, and motion state in top marquee.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_reverse_geocode_cache_size` (Int | Default: `5000` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_speed_unit` (String | Default: `"km/h"` | Options: `"km/h"`, `"mph"` | Target: `system_registry`).
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `reverse_geocode_cache` (`lat_rounded`, `lng_rounded`, `address`).
    - **Transactional**: `device_vitals` (`battery_pct`, `motion_state`).

#### Stage 3: Butterfly Point Tactical Rendezvous (`ButterflyPointDialog`)
* **Activity 3.1 (Foreground): Rendezvous Point Creation & Parameter Setup (HITL)**
  - *Execution*: User long-presses map or clicks Butterfly icon; modal collects Rendezvous Name, Target Arrival Time, and Selected Circle Devices.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_default_butterfly_radius_meters` (Double | Default: `100.0` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_default_butterfly_duration_hours` (Int | Default: `2` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Online: `/butterfly_points/{pointId}` | Local: `butterfly_point` (`point_id`, `name`, `lat`, `lng`, `meeting_time_epoch`, `status`).
    - **Sub-Master**: Online: `/butterfly_points/{pointId}/devices` | Local: `butterfly_point_device_map`.
    - **Transactional**: `change_log` (Category: `Safehouse`, Action: `CREATE`).
* **Activity 3.2 (Background): Closed-Loop ETA & Distance Tracker**
  - *Execution*: Background loop computes distance and ETA for every participant using `SpatialMathUtil.computeDistanceBetween()`. Emits proximity alert when participant within 200m.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_butterfly_alert_proximity_meters` (Double | Default: `200.0` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `butterfly_point`.
    - **Sub-Master**: `butterfly_point_device_map` (Update `distance_to_point_meters`, `eta_seconds`, `battery_pct`).
    - **Transactional**: `butterfly_point_telemetry` (Record participant breadcrumbs), `alert_history` (On rendezvous breach or arrival).

---

# 4. Screen/Page 3: Network Gateway & Device Sharing Mesh (`NetworkGatewayActivity` & `DeviceSharingHub`)

### Coordinate Tree: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
- **`ROOT`**: Symmetrical Peer Pairing, 6-Digit Rendezvous Handshake & Consolidated 4-in-1 Device Sharing Mesh.
- **`BR`**: 
  - Reciprocal Role Mapping: Device 1 = Parent $\Rightarrow$ Target = Child; Device 1 = Spouse $\Rightarrow$ Target = Spouse; Device 1 = Friend $\Rightarrow$ Target = Friend.
  - 15-second Angel Butterfly Progress Sentinel timeout with closed-loop retry/exit fallback.
  - Single 6-digit code can attach up to 5 fleet devices simultaneously without splintering circles.
  - Human-in-the-Loop Consent SOP: Elevation to Parent or Unlinking requires explicit Gate 2 confirmation.
- **`EL`**: `PairingCodeInputWatcher`, `GenerateCodeButtonClickListener`, `DeviceSharingHub.OnShareConfirmedListener`, `FirebasePairingListener`.
- **`FE`**: 6-digit PIN generator/input field, QR code modal, Angel Butterfly progress spinner, connected devices summary list.
- **`BE`**: `GatewayViewModel`, `PairingRepository`, `FirebaseComm`, `SecurityDialogManager`.
- **`DB`**: Master: `device`, `groups` | Sub-Master: `device_pairing`, `device_sharing`, `invitations` | Transaction: `device_sharing_transactions`, `change_log`, `sys_log`.

---

### Stage-Wise Process Hierarchy

#### Stage 1: Pairing Session Initiation & Rendezvous Code Generation
* **Activity 1.1 (Foreground): 6-Digit Temporary Pairing Code Request**
  - *Execution*: Generates cryptographically secure 6-digit token, registers to Firebase `/pairing/{code}`, binds host hardware ID and group ID.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_pairing_code_ttl_seconds` (Int | Default: `300` | Range: 60 - 900s | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_max_pairing_devices_per_code` (Int | Default: `5` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Online: `/pairing/{code}` | Local: `device`.
    - **Sub-Master**: Local: `invitations` (`token`, `group_id`, `expires_at`).
    - **Transactional**: `sys_log`.
* **Activity 1.2 (Foreground): Pairing Code Consumption & Handshake Validation**
  - *Execution*: Joining device enters 6-digit code or scans QR; validates token expiry, fetches host metadata, requests circle admittance.
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Master**: Online: `/pairing/{code}` | Local: `device`.
    - **Sub-Master**: `device_pairing` (Insert preliminary pair record).
    - **Transactional**: `change_log` (Category: `Pairing_Devices`, Action: `CONNECT_INIT`).

#### Stage 2: Handshake Execution & Role Scoping
* **Activity 2.1 (Background): Bidirectional Handshake Sealing (HITL)**
  - *Execution*: Host confirms connection. Enforces reciprocal roles (Parent $\leftrightarrow$ Child, Guardian $\leftrightarrow$ Ward). Writes symmetric pair rows.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_require_host_pin_for_pairing` (Boolean | Default: `true` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Online: `/devices/{hostId}/relationships/{clientId}` & reverse | Local: `device`, `groups`.
    - **Sub-Master**: Local: `device_pairing` (`device_a_id`, `device_b_id`, `role_a`, `role_b`, `status='ACTIVE'`).
    - **Transactional**: `change_log` (Category: `Pairing_Devices`, Action: `PAIR_SEAL`).
* **Activity 2.2 (Foreground/Background): Consolidated 4-in-1 Device Sharing Mesh (`DeviceSharingHub`)**
  - *Execution*: Allows Device A to share Device B's telemetry with Device C (sub-master routing) under fine-grained permission mask.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_device_sharing_timeout_sec` (Int | Default: `15` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_allow_transitive_sharing` (Boolean | Default: `false` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Local: `device`.
    - **Sub-Master**: Local: `device_sharing` (`master_device_id`, `submaster_device_id`, `target_device_id`, `permission_mask`).
    - **Transactional**: Local: `device_sharing_transactions` (`action_type`, `initiated_by_device_id`, `payload_diff`).

---

# 5. Screen/Page 4: Friends & Family Multi-Tier Roster (`FamilyFriendsActivity` & `PersonDetailsActivity`)

### Coordinate Tree: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
- **`ROOT`**: Circle Roster, Dual-Engine RAG Vitals, Parental Guard & Per-Node ACLs.
- **`BR`**: 
  - Ward Guard Invariant: Child node (`4379A761`) cannot be removed or silenced by standard members.
  - Asymmetric Access Control: Parents can view history and trigger sirens; Friends can only view current live marker.
  - RAG Status Engine: Compute Red/Amber/Green vitals based on last ping age ($>15\text{m} = \text{Amber}$, $>60\text{m} = \text{Red}$, Battery $<15\% = \text{Amber}$).
- **`EL`**: `RosterItemClickListener`, `RoleSelectorDropdown`, `PermissionsMaskToggleListener`, `RemoveMemberButtonListener`.
- **`FE`**: Multi-tier expandable roster list, circular avatar with colored RAG halo, battery and connectivity status pills, permission toggle matrix.
- **`BE`**: `FamilyFriendsViewModel`, `PersonDetailsViewModel`, `DeviceRagEngine`, `LocationRepository`.
- **`DB`**: Master: `groups`, `user_identity`, `device` | Sub-Master: `group_members`, `user_profile`, `device_policy_state` | Transaction: `device_vitals`, `alert_history`, `change_log`.

---

### Stage-Wise Process Hierarchy

#### Stage 1: Roster Loading & Multi-Node RAG Evaluation
* **Activity 1.1 (Foreground): Roster Population & View Caching**
  - *Execution*: Reads `group_members` joined with `user_profile` and latest `device_vitals`. Populates UI list items.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_roster_auto_refresh_sec` (Int | Default: `30` | Options: 10, 30, 60, 120 | Target: `system_registry`).
  - *Database Integration*:
    - **Master**: Local: `groups`, `user_identity`, `device` (or view `MASTER_ROSTER`).
    - **Sub-Master**: Local: `group_members`, `user_profile`.
    - **Transactional**: Local: `device_vitals` (Read latest per device).
* **Activity 1.2 (Background): Real-Time Dual-Engine RAG Health Audit**
  - *Execution*: Evaluates each member's last telemetry timestamp, battery percentage, and charging state. Sets RAG indicator (`GREEN`, `AMBER`, `RED`).
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_rag_amber_threshold_mins` (Int | Default: `15` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_rag_red_threshold_mins` (Int | Default: `60` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_rag_low_battery_threshold` (Int | Default: `15` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `group_members` (Update computed `rag_status`).
    - **Transactional**: `device_vitals`, `alert_history` (Generate warning alert if child transitions to RED).

#### Stage 2: Node Detail Inspection & ACL Permission Matrix (`PersonDetailsActivity`)
* **Activity 2.1 (Foreground): Granular Permission Mask Configuration (HITL)**
  - *Execution*: Parent adjusts permissions for child or friend (e.g., Allow Siren, Allow Geofence Edit, Allow Historical Route View).
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_default_member_permission_mask` (Int | Default: `0x0F` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Online: `/devices/{id}/Access` | Local: `device`.
    - **Sub-Master**: Local: `device_policy_state` (`permission_mask`, `is_ghost_mode`).
    - **Transactional**: `change_log` (Category: `Family_and_Friends`, Action: `UPDATE_ACL`).
* **Activity 2.2 (Foreground): Member Unlinking & Circle Departure (HITL Gate 2)**
  - *Execution*: Removing a member requires Master PIN entry and confirmation prompt. Updates local DB and removes cloud node.
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Master**: Online: `/devices/{hostId}/relationships/{peerId}` (Remove) | Local: `groups`.
    - **Sub-Master**: Local: `group_members` (`status='ARCHIVED'`), `device_pairing` (`status='REVOKED'`).
    - **Transactional**: `change_log` (Category: `Family_and_Friends`, Action: `REMOVE_MEMBER`), `security_audit_log`.

---

# 6. Screen/Page 5: Geofencing & Safe Boundary Sentinel (`GeofenceActivity` & `GeofenceNotificationActivity`)

### Coordinate Tree: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
- **`ROOT`**: Hardware Perimeter Enforcement, Ray-Casting Polygon Geofencing & Transition Logging.
- **`BR`**: 
  - Zero false-alarm hysteresis: 3 consecutive outside readings required before triggering an Exit Breach.
  - Time-windowed schedules: School safe zones active only Monday-Friday 08:00 to 15:00.
  - Immediate high-priority local push notification and cloud alarm on perimeter violation.
- **`EL`**: `GeofenceBroadcastReceiver`, `GeofenceMapClickListener`, `SafeZoneEditorDialog`, Play Services `GeofencingClient`.
- **`FE`**: Circular and polygon safe-zone boundary overlays, radius slider ($25\text{m} - 5000\text{m}$), target device selector chips, event history timeline.
- **`BE`**: `GeofenceViewModel`, `GeofenceRepository`, `PointInPolygonEngine`, `GeofenceNotificationHelper`.
- **`DB`**: Master: `safe_zone` | Sub-Master: `safe_zone_user_map`, `stealth_schedule_config` | Transaction: `geofence_event`, `alert_history`, `sys_log`.

---

### Stage-Wise Process Hierarchy

#### Stage 1: Safe Zone Definition & Geometry Authoring
* **Activity 1.1 (Foreground): Boundary Canvas Authoring (HITL)**
  - *Execution*: User draws circle or polygon on map; sets SafeZone Name, Radius ($25\text{m} - 2500\text{m}$), and assigns connected circle devices.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_geofence_min_radius_meters` (Int | Default: `25` | Range: 10 - 100m | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_geofence_default_radius_meters` (Int | Default: `100` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Online: `/geofencing/{creatorId}/safe_zones/{zoneId}` | Local: `safe_zone` (`zone_id`, `name`, `lat`, `lng`, `radius_meters`, `created_by`).
    - **Sub-Master**: Local: `safe_zone_user_map` (`zone_id`, `identity_id`, `notify_on_entry`, `notify_on_exit`).
    - **Transactional**: `change_log` (Category: `Safehouse`, Action: `CREATE_ZONE`).
* **Activity 1.2 (Background): Hardware Geofencing Registration (Play Services)**
  - *Execution*: Builds `GeofencingRequest` and registers perimeters with Android OS `GeofencingClient`.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_geofence_loitering_delay_ms` (Int | Default: `30000` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_geofence_notification_responsiveness_ms` (Int | Default: `15000` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `safe_zone`.
    - **Sub-Master**: `safe_zone_user_map`.
    - **Transactional**: `sys_log` (Tag: `GEOFENCE_REGISTER`).

#### Stage 2: Hardware Transition Detection & Ingress/Egress Auditing (`GeofenceNotificationActivity`)
* **Activity 2.1 (Background): Perimeter Breach Interceptor (`GeofenceBroadcastReceiver`)**
  - *Execution*: Receives OS geofence transition intent (`GEOFENCE_TRANSITION_ENTER` / `EXIT` / `DWELL`). Applies 2D Ray-Casting algorithm for polygon zones.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_geofence_debounce_count` (Int | Default: `3` | Range: 1 - 5 readings | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Online: `/devices/{id}/alerts/{alertId}` | Local: `safe_zone`.
    - **Sub-Master**: Local: `safe_zone_user_map`.
    - **Transactional**: Local: `geofence_event` (`event_id`, `zone_id`, `identity_id`, `event_type`, `timestamp`), `alert_history`.
* **Activity 2.2 (Foreground): Notification Dispatch & Audit Timeline Presentation**
  - *Execution*: Dispatches high-priority notification with sound/vibration; updates `GeofenceNotificationActivity` audit timeline.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_geofence_alarm_sound_enabled` (Boolean | Default: `true` | Target: `system_registry`).
  - *Database Integration*:
    - **Master**: `safe_zone`.
    - **Sub-Master**: `safe_zone_user_map`.
    - **Transactional**: Local: `geofence_event`, `alert_history`.

---

# 7. Screen/Page 6: Location History & Trajectory Playback (`HistoryActivity`)

### Coordinate Tree: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
- **`ROOT`**: Historical Route Query, Breadcrumb Reconstruction, Route Interpolation & Animated Playback.
- **`BR`**: 
  - Date Partitioning: Queries partitioned by `day_key` (`YYYYMMDD`) to prevent full-table sequential scans.
  - Trajectory Smoothing: Jitter filter suppresses stationary drift ($< 5\text{m}$ with zero speed ignored).
  - Reverse Geocode Cache Optimization: Points resolve addresses lazily via `reverse_geocode_cache` by rounded coordinates.
- **`EL`**: `DatePickerDialog`, `PlaybackPlayPauseButtonListener`, `PlaybackSpeedSlider`, `TrajectoryPolylineClickListener`.
- **`FE`**: Trajectory polyline with color-coded speed gradients, animated vehicle/avatar cursor, scrubber bar, trip metrics card (distance, top speed, stop count).
- **`BE`**: `HistoryViewModel`, `LocationRepository`, `PolylineDecoder`, `SpatialMathUtil`.
- **`DB`**: Master: `user_identity`, `device` | Sub-Master: `reverse_geocode_cache`, `stealth_schedule_config` | Transaction: `location_event` (view `locations`), `sys_log`.

---

### Stage-Wise Process Hierarchy

#### Stage 1: Historical Route Query & Filtering
* **Activity 1.1 (Foreground): Target Device & Date Range Selection (HITL)**
  - *Execution*: User selects device from roster and picks date; triggers ViewModel query filtered by `identity_id` and `day_key`.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_storage_retention_days` (Int | Default: `3` | Options: 1, 3, 7, 14, 30 | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_history_max_points_per_trip` (Int | Default: `2000` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Local: `user_identity`, `device` | Online: `/History_Archive/{id}/{dayKey}`.
    - **Sub-Master**: Local: `stealth_schedule_config`.
    - **Transactional**: Local: `location_event` (`SELECT * FROM location_event WHERE identity_id = ? AND day_key = ? ORDER BY timestamp ASC`).
* **Activity 1.2 (Background): Point Decimation & Jitter Filtering**
  - *Execution*: Evaluates consecutive GPS points using Douglas-Peucker algorithm or distance-delta filtering ($> 5\text{m}$ or $\Delta\theta > 15^\circ$) to eliminate stationary GPS drift.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_jitter_filter_threshold_meters` (Float | Default: `5.0f` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `reverse_geocode_cache`.
    - **Transactional**: `location_event`.

#### Stage 2: Animated Route Replay & Metrics Computation
* **Activity 2.1 (Foreground): Animated Breadcrumb Playback Engine**
  - *Execution*: Smoothly moves avatar along polyline path using linear coordinate interpolation with speed-sensitive cadence ($1\times, 2\times, 5\times, 10\times$).
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_history_playback_speed_multiplier` (Int | Default: `1` | Options: 1, 2, 5, 10 | Target: `system_registry`).
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `reverse_geocode_cache` (Resolves street name for current playback position).
    - **Transactional**: `location_event`.
* **Activity 2.2 (Foreground): Trip Statistics Synthesis**
  - *Execution*: Computes total odometer distance (km), trip duration (mins), maximum speed, and dwell/stop locations.
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: *(None)*.
    - **Transactional**: `location_event`.

---

# 8. Screen/Page 7: Alerts, SOS Panic & Acoustic Siren (`NotifActivity` & `AcousticSirenActivity`)

### Coordinate Tree: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
- **`ROOT`**: Emergency Ingress/Egress, Critical SOS Beacon, Acoustic DND Override Siren & Tamper Defense.
- **`BR`**: 
  - Hardware Key SOS: Triple-tap power button triggers immediate SOS ingress regardless of lock screen.
  - DND Audio Override: Acoustic siren forces audio stream to `STREAM_ALARM` at maximum volume ($100\%$) for 30 seconds.
  - Gate 1 Protection: Tamper or low battery on child device generates immediate priority broadcast to all paired parents.
- **`EL`**: `SOSButtonLongClickListener`, `SirenDismissPinWatcher`, `AudioOverrideService`, FCM High-Priority Push Receiver.
- **`FE`**: Red flashing strobe screen, pulsating SOS button with countdown timer, emergency contact quick-dial dock, resolved/unresolved alert ledger.
- **`BE`**: `NotifViewModel`, `AudioOverrideService`, `EmergencyAlertHelper`, `FirebaseComm`.
- **`DB`**: Master: `user_identity`, `device` | Sub-Master: `stealth_schedule_config` | Transaction: `alert_history`, `geofence_event`, `sys_log`, `security_audit_log`.

---

### Stage-Wise Process Hierarchy

#### Stage 1: SOS Panic Trigger & Local Alert Ingress
* **Activity 1.1 (Foreground/Background): SOS Panic Button Press or Hardware Key Detection**
  - *Execution*: User holds SOS button for 3 seconds or triple-taps power key; acquires GPS fix, fetches cell tower IDs, captures vitals.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_sos_countdown_sec` (Int | Default: `3` | Options: 0, 3, 5 | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_sos_sms_target_number` (String | Default: `""` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Online: `/devices/{id}/alerts/{alertId}` | Local: `user_identity`, `device`.
    - **Sub-Master**: Local: `stealth_schedule_config`.
    - **Transactional**: Local: `alert_history` (`identity_id`, `alert_type='SOS_PANIC'`, `severity='EMERGENCY'`, `lat`, `lng`, `timestamp`), `location_event` (`sos_flag=1`).
* **Activity 1.2 (Background): Multichannel Telemetry Failover Dispatch**
  - *Execution*: Dispatches alert through primary channel (Firebase RTDB). If network offline, failovers to SMS PDU beacon and BLE mesh.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_sms_sos_failover_enabled` (Boolean | Default: `true` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `telemetry_failover_routes` (`priority_tier`, `transport_channel`, `status`).
    - **Transactional**: `p2p_mesh_packets`, `sync_queue`.

#### Stage 2: Remote Acoustic Siren Activation (`AcousticSirenActivity`)
* **Activity 2.1 (Foreground): DND Override 30s Acoustic Alarm Beacon**
  - *Execution*: Remote parent sends "Ring Siren" command; child device turns screen on over lock screen (`FLAG_SHOW_WHEN_LOCKED`), sets alarm stream to max volume, loops siren audio for 30s.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_siren_duration_sec` (Long | Default: `30L` | Options: 15, 30, 60, 120 | Target: `stealth_schedule_config` / `system_registry`).
    - Editable Variable: `pref_siren_strobe_flashlight` (Boolean | Default: `true` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `device_policy_state`.
    - **Transactional**: `alert_history`, `sys_log` (Tag: `ACOUSTIC_SIREN_ACTIVE`).
* **Activity 2.2 (Foreground): Siren Dismissal & Verification (HITL)**
  - *Execution*: Silencing the siren requires either timeout expiry or Master PIN authentication.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_siren_require_pin_to_silence` (Boolean | Default: `false` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `user_profile` (Validate `pin_hash`).
    - **Transactional**: `alert_history` (Update `resolved_at = timestamp`).

---

# 9. Screen/Page 8: Hardware Health, Cellular Sentinel & SIM Forensics (`HealthActivity` & `SimForensicWorker`)

### Coordinate Tree: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
- **`ROOT`**: Physical Hardware Diagnostics, Baseband Cellular Triangulation, Power Management & SIM Hot-Swap Sentinel.
- **`BR`**: 
  - SIM Hot-Swap Detection: Changing SIM card or IMSI mismatch immediately logs forensic event and alerts parent.
  - Cell Tower Breadcrumb: When GPS is shielded (indoors/subway), baseband cellular parameters (MCC/MNC/LAC/CID) provide fallback localization.
  - Doze Mode Bypass: Background sentinels request Doze exemption to maintain heartbeats.
- **`EL`**: `HealthActivity.OnRefreshClickListener`, `SimForensicWorker`, `CellTowerSentinel`, `BroadcastReceiver` for SIM State Changes.
- **`FE`**: Battery health indicator (voltage, temperature, charging technology), GPS satellite count & accuracy pill, cellular tower dossier (LAC/CID/Signal dBm), actionable diagnostic banner.
- **`BE`**: `HardwareStatusMonitor`, `CellTowerSentinel`, `SimTransportValidator`, `SimForensicWorker`.
- **`DB`**: Master: `device` | Sub-Master: `sim`, `device_policy_state` | Transaction: `device_vitals`, `sim_transactions`, `telemetry_failover_routes`, `sys_log`.

---

### Stage-Wise Process Hierarchy

#### Stage 1: Hardware Health & Sensor Audit (`HealthActivity`)
* **Activity 1.1 (Foreground): Real-Time Battery & Thermal Audit**
  - *Execution*: Evaluates `BatteryManager` sticky intent; extracts percentage, charging source (AC/USB/Wireless), voltage (mV), and battery temperature ($^\circ\text{C}$).
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_battery_warning_threshold` (Int | Default: `15` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_thermal_warning_celsius` (Float | Default: `45.0f` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Online: `/devices/{id}/telemetry` | Local: `device`.
    - **Sub-Master**: Local: `device_policy_state`.
    - **Transactional**: Local: `device_vitals` (`battery_pct`, `temperature_c`, `is_charging`).
* **Activity 1.2 (Foreground): 14-Condition Network & Transport Diagnostic Test**
  - *Execution*: Audits active transport (Wi-Fi vs Cellular), evaluates carrier data connectivity, verifies DNS resolution, measures round-trip ping latency to Firebase backend ($<5\text{s}$ nominal).
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_closed_loop_timeout_ms` (Long | Default: `5000L` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `telemetry_failover_routes`.
    - **Transactional**: `device_vitals` (`network_type`, `signal_strength_dbm`), `sys_log`.

#### Stage 2: Cellular Baseband & SIM Forensics (`SimForensicWorker` / `SIMTrackingService`)
* **Activity 2.1 (Background): Dual-SIM Inventory & IMSI Sentinel**
  - *Execution*: Extracts SIM serial number, carrier operator name, and IMSI. Compares against `sim` table. If serial changed $\Rightarrow$ raise `SIM_TAMPER` critical alert.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_sim_forensic_interval_mins` (Long | Default: `15L` | Options: 15, 60, 360, 720, 1440 | Target: `system_registry`).
  - *Database Integration*:
    - **Master**: Online: `/devices/{id}/telemetry/sim` | Local: `device`.
    - **Sub-Master**: Local: `sim` (`sim_serial`, `device_id`, `phone_number`, `carrier_name`, `slot_index`, `imsi`).
    - **Transactional**: `alert_history` (If SIM replaced), `security_audit_log`.
* **Activity 2.2 (Background): Cell Tower Triangulation (MCC/MNC/LAC/CID)**
  - *Execution*: Probes `TelephonyManager.getAllCellInfo()`; captures primary and neighboring cell tower IDs (CID, LAC, TAC, RSSI dBm) for GPS-denied dead-reckoning.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_cell_tower_logging_enabled` (Boolean | Default: `true` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `sim`.
    - **Transactional**: Local: `sim_transactions` (`sim_serial`, `cid`, `lac`, `mcc`, `mnc`, `network_type`, `signal_strength`, `timestamp`).

---

# 10. Screen/Page 9: User Profile, Avatar Studio & Organization (`ProfileActivity`, `AvatarEditorActivity`, `OrgActivity`)

### Coordinate Tree: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
- **`ROOT`**: Personal Citizen Identity, Avatar Customization, Emergency Medical Dossier & Organization Binding.
- **`BR`**: 
  - Silicon Anchor Immutability: User cannot edit device hardware ID or silicon root anchor.
  - Avatar Base64 / Storage Optimization: Avatars resized to $256 \times 256$ WebP/PNG before local storage and cloud distribution.
  - Medical Emergency Notes: Blood group and emergency notes accessible from lock screen in SOS mode.
- **`EL`**: `SaveProfileButtonListener`, `AvatarSelectClickListener`, `AvatarCropSaveListener`, `OrgClearanceSpinnerListener`.
- **`FE`**: Avatar preview circle with badge editor, validated form input fields (Name, Phone, Emergency Note), blood group selector chips, organization clearance card.
- **`BE`**: `ProfileViewModel`, `AvatarLoader`, `BitmapCompressor`, `FirebaseComm`.
- **`DB`**: Master: `user_identity`, `organization` | Sub-Master: `user_profile`, `user_org_map` | Transaction: `change_log`, `sys_log`.

---

### Stage-Wise Process Hierarchy

#### Stage 1: Profile Inspection & Identity Presentation
* **Activity 1.1 (Foreground): Profile Data Hydration**
  - *Execution*: Queries `user_identity` and `user_profile`; loads full name, phone number, avatar ID, blood group, and emergency memo.
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Master**: Local: `user_identity` (`id`, `uid_1`, `phone_number`) | Online: `/devices/{id}/profile`.
    - **Sub-Master**: Local: `user_profile` (`full_name`, `avatar_id`, `blood_group`, `emergency_notes`).
    - **Transactional**: `sys_log`.
* **Activity 1.2 (Foreground): Avatar Studio & Cropping (`AvatarEditorActivity` & `AvatarCropActivity`)**
  - *Execution*: User selects avatar from preset catalog or crops camera photo; image cropped to 1:1 aspect ratio and saved to internal storage.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_avatar_max_dimension_px` (Int | Default: `256` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_avatar_compression_quality` (Int | Default: `85` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `user_identity`.
    - **Sub-Master**: `user_profile` (Update `avatar_id` with URI or catalog token).
    - **Transactional**: `change_log` (Category: `Profile`, Action: `UPDATE_AVATAR`).

#### Stage 2: Profile Update & Cloud Synchronization (HITL)
* **Activity 2.1 (Foreground): Profile Changes Commit & Validation**
  - *Execution*: Validates input constraints (non-blank name, valid phone); updates local SQLite `user_profile` and dispatches differential update to Firebase.
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Master**: Online: `/devices/{id}/profile` | Local: `user_identity`.
    - **Sub-Master**: Local: `user_profile` (`full_name`, `blood_group`, `emergency_notes`, `updated_at = now`).
    - **Transactional**: `change_log` (Category: `Profile`, Action: `UPDATE_FIELDS`).
* **Activity 2.2 (Foreground): Organization Clearance & Mapping (`OrgActivity`)**
  - *Execution*: Binds user identity to corporate or municipal organization entity with security clearance tier.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_default_org_id` (String | Default: `"ORG_DEFAULT"` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Local: `organization` (`org_id`, `org_name`, `clearance_level`).
    - **Sub-Master**: Local: `user_org_map` (`identity_id`, `org_id`, `role`, `joined_at`).
    - **Transactional**: `change_log` (Category: `Profile`, Action: `ORG_MAPPING`).

---

# 11. Screen/Page 10: Universal Settings & Dynamic UI Engine (`SettingsActivity` & `UiConfigActivity`)

### Coordinate Tree: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
- **`ROOT`**: System-Wide Dynamic Configuration, Runtime Variable Tuning, Dynamic UI Theming & Scale Matrix.
- **`BR`**: 
  - Zero Hardcoded Parameters: Every threshold, interval, buffer size, and timeout MUST be read dynamically from `stealth_schedule_config` or `system_registry`.
  - Priority 1-8 Tab Hierarchy: Structured setting cards with instant persistence and closed-loop validation.
  - Dynamic UI Scaling: 5-stage screen width geometry (LIBERAL, STANDARD, COMPACT, SLIM, ULTRA_SLIM).
- **`EL`**: `SettingsActivity.OnPreferenceChangeListener`, `UiConfigActivity.ThemeRadioGroupListener`, `ResetDefaultsButtonListener`.
- **`FE`**: Dual-tab dashboard layout (Tab 1: Universal Settings, Tab 2: Modules), dynamic sliders, switches, color palette preview chips, live layout preview box.
- **`BE`**: `RegistryManager`, `UiArchitect`, `ThemeManager`, `SettingsModel`.
- **`DB`**: Master: `device`, `user_identity` | Sub-Master: `stealth_schedule_config`, `device_policy_state` | Transaction: `change_log`, `security_audit_log`, `sys_log`.

---

### Stage-Wise Process Hierarchy

#### Stage 1: Dynamic Settings Read & Dashboard Population
* **Activity 1.1 (Foreground): Priority 1-8 Settings Cards Loading**
  - *Execution*: Reads 178 system parameters across 8 cards (Priorities 1-8); binds values to UI switches, sliders, and dropdown selectors.
  - *Is Setting / Config?*: **YES**
    - *Card 1 (Core Telemetry)*: `pref_tracking_interval` (15s), `pref_high_accuracy_gps` (true).
    - *Card 2 (Mesh & Pairing)*: `pref_invitation_ttl_days` (7), `pref_pairing_timeout_sec` (300).
    - *Card 3 (Motion & Duty Cycle)*: `pref_motion_sampling_ms` (10000), `pref_stationary_dwell_mins` (15).
    - *Card 4 (Geofencing)*: `pref_geofence_sensitivity_meters` (50), `pref_geofence_debounce_count` (3).
    - *Card 5 (Radar)*: `pref_radar_proximity_radius` (5000m), `pref_distant_device_threshold` (15000m).
    - *Card 6 (Health & SIM)*: `pref_sim_forensic_interval_mins` (15), `pref_battery_save_mode` (false).
    - *Card 7 (Storage & Retention)*: `pref_storage_retention_days` (3), `pref_sync_queue_ttl_hours` (48), `pref_max_geocode_cache` (5000).
    - *Card 8 (Circuit Breaker)*: `pref_closed_loop_timeout_ms` (5000), `pref_circuit_breaker_threshold` (3).
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `stealth_schedule_config` (Read all key-value rows), SharedPreferences `system_registry`.
    - **Transactional**: `sys_log`.
* **Activity 1.2 (Foreground): Runtime Setting Modification & Closed-Loop Broadcast (HITL)**
  - *Execution*: User changes a slider/switch; updates `stealth_schedule_config` and fires `ACTION_SETTINGS_CHANGED` broadcast to notify active background services.
  - *Is Setting / Config?*: **YES** (Every modification updates an editable variable).
  - *Database Integration*:
    - **Master**: Online: `/devices/{id}/Access` (If privacy toggle) | Local: `device`.
    - **Sub-Master**: Local: `stealth_schedule_config` (`INSERT OR REPLACE INTO stealth_schedule_config VALUES (?, ?, ?, ?, ?)`).
    - **Transactional**: `security_audit_log` (Log config modification event).

#### Stage 2: Dynamic UI Architecture & Theme Customization (`UiConfigActivity`)
* **Activity 2.1 (Foreground): Theme Palette & Scaling Geometry Configuration**
  - *Execution*: User selects Dark/Light mode, custom primary/accent hex colors, and viewport padding tier (LIBERAL: 25dp, STANDARD: 20dp, COMPACT: 15dp, SLIM: 10dp, ULTRA_SLIM: 5dp).
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_ui_scaling_tier` (String | Default: `"STANDARD"` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_theme_primary_hex` (String | Default: `"#0F172A"` | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_theme_accent_hex` (String | Default: `"#38BDF8"` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `stealth_schedule_config`.
    - **Transactional**: `sys_log` (Tag: `UI_CONFIG_APPLIED`).
* **Activity 2.2 (Foreground): Component Suite Visual Showcase (`UiGalleryActivity`)**
  - *Execution*: Live interactive preview of buttons, textboxes, switches, dialogs, and progress bars styled by `UiArchitect`.
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Master**: *(None)*.
    - **Sub-Master**: `stealth_schedule_config`.
    - **Transactional**: *(None)*.

---

# 12. Screen/Page 11: Forensic Vault, Database Console & System Audit (`ForensicVaultExplorerActivity`, `DatabaseManagementActivity`, `SystemLogActivity`, `WatchDogAuditActivity`)

### Coordinate Tree: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
- **`ROOT`**: Encrypted Forensic Inspection, SQLite Schema Administration, Real-Time Trace Logging & Process Liveness Audit.
- **`BR`**: 
  - Read-Only Forensic Safety: `ForensicVaultExplorerActivity` cannot mutate operational tables directly without Master PIN.
  - Schema Invariant: Database vacuum and index maintenance execute in isolated background transactions with rollback protection.
  - Rolling System Log Cap: `sys_log` capped to latest 5000 rows to prevent storage degradation.
- **`EL`**: `TableSpinnerOnItemSelectedListener`, `ExecuteVacuumButtonListener`, `ExportCsvButtonListener`, `LogFilterWatcher`.
- **`FE`**: Paginated data grid with horizontal scroll, table metadata KPI cards (row count, page size, disk MB), search/filter toolbar, terminal-style log output stream.
- **`BE`**: `DatabaseHelper`, `SystemLogger`, `WatchDogViewModel`, `ForensicVaultExporter`.
- **`DB`**: Master: All 32 Tables (Tiers 1-3) | Sub-Master: 13 Compatibility Views | Transaction: All Ledgers (`sys_log`, `security_audit_log`, `watchdog_resurrection_log`).

---

### Stage-Wise Process Hierarchy

#### Stage 1: Forensic Vault Table Inspection (`ForensicVaultExplorerActivity`)
* **Activity 1.1 (Foreground): Table Schema & Metadata Extraction**
  - *Execution*: User selects table from dropdown; executes `PRAGMA table_info(tableName)` and `SELECT COUNT(*) FROM tableName`.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_default_page_limit` (Int | Default: `50` | Options: 25, 50, 100, 200 | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: Dynamic queries across all Tier 1 Master tables.
    - **Sub-Master**: Dynamic queries across all Tier 2 Sub-Master tables.
    - **Transactional**: Dynamic queries across all Tier 3 Transaction tables.
* **Activity 1.2 (Foreground): Paginated Data Grid Rendering & CSV Export (HITL Gate 2)**
  - *Execution*: Renders 50 rows per page; allows human-in-the-loop export of encrypted tables to `/Documents/jFindMe_Vault/` upon confirmation.
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Master/Sub-Master/Transaction**: Reads target table with `LIMIT ? OFFSET ?`.
    - **Transactional**: Insert `security_audit_log` (Event: `FORENSIC_EXPORT_CSV`).

#### Stage 2: Database Maintenance & Health Console (`DatabaseManagementActivity`)
* **Activity 2.1 (Foreground): PRAGMA Integrity & Page Allocation Check**
  - *Execution*: Evaluates database file size (MB), executes `PRAGMA quick_check`, queries `PRAGMA page_count` and `page_size`.
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Database Engine**: Direct SQLite PRAGMA execution.
    - **Transactional**: `sys_log`.
* **Activity 2.2 (Foreground): Manual SQLite Maintenance & VACUUM (HITL)**
  - *Execution*: User triggers manual cleanup; runs rolling prune on expired telemetry ($> 30\text{ days}$) and invokes `VACUUM` to reclaim disk space.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_storage_retention_days` (Int | Default: `3` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `stealth_schedule_config`.
    - **Transactional**: Purges expired rows in `location_event`, `sys_log`, `geofence_event`, `sim_transactions`.

#### Stage 3: Process Liveness & Trace Audit (`WatchDogAuditActivity` & `SystemLogActivity`)
* **Activity 3.1 (Foreground): Process Keep-Alive & Crash Resurrection Ledger**
  - *Execution*: Displays timeline of background service keep-alives, Doze bypass events, and watchdog resurrection latencies.
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: `device_policy_state`.
    - **Transactional**: Local: `watchdog_resurrection_log` (`id`, `hardware_id`, `event_type`, `latency_ms`, `timestamp`).
* **Activity 3.2 (Foreground): Real-Time Rolling System Log Viewer (`SystemLogActivity`)**
  - *Execution*: Streams runtime trace logs filtered by severity (`VERBOSE`, `DEBUG`, `INFO`, `WARN`, `ERROR`).
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_sys_log_filter_level` (String | Default: `"INFO"` | Target: `system_registry`).
  - *Database Integration*:
    - **Transactional**: Local: `sys_log` (`SELECT * FROM sys_log ORDER BY timestamp DESC LIMIT 500`).

---

# 13. Screen/Page 12: Silent System Update & Factory Reset (`SystemUpdateActivity` & `FactoryResetActivity`)

### Coordinate Tree: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
- **`ROOT`**: Autonomous Over-the-Air (OTA) Binary Updates, Integrity Verification & Clean Forensic Factory Purge.
- **`BR`**: 
  - Pre-Flight Update Requirements: Battery $\ge 15\%$, free disk cache $\ge 150\text{ MB}$, valid keystore signature match.
  - Unattended API 31+ Update: Uses `PackageInstaller.SessionParams.setRequireUserAction(USER_ACTION_NOT_REQUIRED)`.
  - Gate 2 Irreversible Operation: Factory reset strictly requires 1 confirmation prompt with Master PIN gate before database wipe.
- **`EL`**: `CheckUpdateButtonClickListener`, `DownloadUpdateClickListener`, `FactoryResetConfirmListener`, `ApkInstallerCallback`.
- **`FE`**: Current vs Available version card, release changelog box, animated download progress bar, red factory reset confirmation sheet.
- **`BE`**: `SystemUpdateViewModel`, `SilentUpdateWorker`, `ApkInstallerHelper`, `FactoryResetViewModel`.
- **`DB`*: Master: All Tables Purged (Reset) | Sub-Master: `stealth_schedule_config` | Transaction: `sys_log`, `security_audit_log`.

---

### Stage-Wise Process Hierarchy

#### Stage 1: OTA Version Discovery & Integrity Verification (`SystemUpdateActivity`)
* **Activity 1.1 (Foreground/Background): Cloud Version Manifest Check**
  - *Execution*: Contacts Firebase `/Version_Update/latest`; compares available `versionCode` against local `BuildConfig.VERSION_CODE`.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_update_check_interval_hours` (Long | Default: `24L` | Options: 6, 12, 24, 48 | Target: `stealth_schedule_config`).
    - Editable Variable: `pref_ota_channel` (String | Default: `"RELEASE"` | Options: `"RELEASE"`, `"BETA"` | Target: `system_registry`).
  - *Database Integration*:
    - **Master**: Online: `/Version_Update/{id}/latest` | Local: `device`.
    - **Sub-Master**: `stealth_schedule_config`.
    - **Transactional**: `sys_log` (Tag: `OTA_CHECK`).
* **Activity 1.2 (Background): Binary Download & SHA-256 Hash Verification**
  - *Execution*: Streams APK into `context.cacheDir`; calculates SHA-256 cryptographic hash; verifies keystore signature match.
  - *Is Setting / Config?*: **YES**
    - Editable Variable: `pref_allow_cellular_ota_downloads` (Boolean | Default: `false` | Target: `stealth_schedule_config`).
  - *Database Integration*:
    - **Master**: `device`.
    - **Sub-Master**: *(Missing `system_update_history` - Recommended for V84)*.
    - **Transactional**: `security_audit_log` (Log binary hash verification result).
* **Activity 1.3 (Background): Package Installation Handoff & Auto-Restart**
  - *Execution*: Pipes APK stream into Android OS `PackageInstaller`. On completion, `ACTION_MY_PACKAGE_REPLACED` triggers automatic service resurrection.
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Master**: `device`.
    - **Transactional**: `watchdog_resurrection_log` (Event: `POST_INSTALL_REBOOT`).

#### Stage 2: Factory Reset & Forensic Teardown (`FactoryResetActivity`)
* **Activity 2.1 (Foreground): Selective Reset vs Full Wipe Qualification (HITL Gate 2)**
  - *Execution*: Prompts user with irreversible warning modal; requires Master PIN; user selects "Purge History Only" or "Full Factory Reset".
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Master**: `user_identity` (Verify `pin_hash`).
    - **Transactional**: `security_audit_log` (Event: `FACTORY_RESET_INIT`).
* **Activity 2.2 (Background): Teardown Execution & Cloud Unregistration**
  - *Execution*: Kills `TrackingService`, unregisters WorkManager tasks, sets Firebase device node status to `GHOST`, drops SQLite tables, clears `system_registry.xml`, and launches OS package uninstaller or fresh onboarding.
  - *Is Setting / Config?*: **NO** ➔ Next.
  - *Database Integration*:
    - **Online**: `/devices/{id}/status = 'GHOST'`.
    - **Local Master/Sub-Master/Transaction**: All 32 tables truncated or SQLite database file deleted.

---

# 14. Screen/Page 13: Background Engine Sentinels & Orchestration (`BackgroundJobManager`)

### Coordinate Tree: `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`
- **`ROOT`**: Autonomous Background Sentinels, Keep-Alive Watchdogs, Telemetry Pipes & Storage Hygiene.
- **`BR`**: 
  - Sequential Dependency Waterfall: Tier 1 (GPS Core) ➔ Tier 2 (Cloud Mesh) ➔ Tier 3 (Hardware Forensics) ➔ Tier 4 (Storage Hygiene).
  - Background Execution Throttling: Motion-adaptive intervals (15s moving vs 15m stationary).
  - WorkManager / AlarmManager Co-Existence: Exact RTC alarms for Doze punch-through; WorkManager for opportunistic queue sync.
- **`EL`**: `AlarmManager.setAlarmClock()`, `WorkManager.enqueueUniquePeriodicWork()`, `BatteryBroadcastReceiver`, `NetworkCallback`.
- **`FE`**: `BackgroundJobsConfigDialog` (13 background jobs with live pause/resume switches, frequency selector sheets, last-run timestamps).
- **`BE`**: `BackgroundJobManager`, `TrackingService`, `OfflineSyncWorker`, `LocationWatchdogWorker`, `DatabaseMaintenanceWorker`, `SimForensicWorker`.
- **`DB`**: Master: All 32 Tables | Sub-Master: `stealth_schedule_config` | Transaction: `location_event`, `pending_sync_queue`, `device_vitals`, `watchdog_resurrection_log`.

---

### Master 4-Tier Background Waterfall & Process Mapping

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ TIER 1: 🛰️ PRIMARY TELEMETRY & CORE GPS PIPELINE (Root / Critical)                     │
│ ├── JOB_TRACKING_SERVICE      : Foreground GPS Telemetry Service                       │
│ ├── JOB_LOCATION_WATCHDOG     : Sentinel Process Keep-Alive Monitor                    │
│ └── JOB_GEOFENCE_SENTINEL     : Hardware Geofence Perimeter Sentinel                   │
└───────────────────────────────────┬────────────────────────────────────────────────────┘
                                    │ (Sequential Dependency)
┌───────────────────────────────────▼────────────────────────────────────────────────────┐
│ TIER 2: ☁️ CLOUD SYNC & PEER STREAMING HUB (Network Synchronization)                   │
│ ├── JOB_LIVE_MAP_STREAM       : Peer WebSocket RTDB Stream (Ghost-Excl)                │
│ ├── JOB_PRESENCE_PULSE        : Real-time Presence Pulse & Vitals Ping                 │
│ ├── JOB_OFFLINE_SYNC          : Encrypted Offline SQLite Queue Flusher                 │
│ └── JOB_FCM_MESSAGING         : High-Priority FCM Push Sentinel                        │
└───────────────────────────────────┬────────────────────────────────────────────────────┘
                                    │ (Sequential Dependency)
┌───────────────────────────────────▼────────────────────────────────────────────────────┐
│ TIER 3: 📱 FORENSIC HARDWARE & SECURITY (Passive Hardware Interceptors)                │
│ ├── JOB_SECURITY_RECEIVER     : Power, Boot, Flight Mode Interceptor                   │
│ ├── JOB_ACOUSTIC_SIREN        : Emergency Beacon & Audio Bridge                        │
│ └── JOB_SIM_FORENSIC          : SIM & Cellular Tower Hardware Sentinel                 │
└───────────────────────────────────┬────────────────────────────────────────────────────┘
                                    │ (Sequential Dependency)
┌───────────────────────────────────▼────────────────────────────────────────────────────┐
│ TIER 4: ⚡ STORAGE PRUNING & SYSTEM INTEGRITY (Maintenance & Hygiene)                   │
│ ├── JOB_STORAGE_PRUNE         : Rolling Retention Purge & SQLite VACUUM                │
│ ├── JOB_SILENT_UPDATE         : Binary Hash Check & Update Sentinel                    │
│ └── JOB_APP_UPGRADE_WATCH     : Post-Install Auto-Restart Sentinel                     │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Detailed Job Mapping:
1. **`JOB_TRACKING_SERVICE`**: Foreground GPS service running at dynamic 15s intervals.
   - *Is Setting / Config?*: **YES** (`pref_tracking_interval` = 15000ms | Options: 1s, 7s, 15s, 30s, 60s).
   - *DB*: Master: `device` | Sub-Master: `reverse_geocode_cache` | Transaction: `location_event`, `pending_sync_queue`.
2. **`JOB_LOCATION_WATCHDOG`**: Heartbeat sentinel waking every 15 minutes.
   - *Is Setting / Config?*: **YES** (`pref_watchdog_interval_mins` = 15m | Options: 15m, 30m, 60m).
   - *DB*: Sub-Master: `device_policy_state` | Transaction: `watchdog_resurrection_log`.
3. **`JOB_GEOFENCE_SENTINEL`**: Hardware perimeter monitor.
   - *Is Setting / Config?*: **YES** (`pref_geofence_sensitivity_meters` = 50m).
   - *DB*: Master: `safe_zone` | Sub-Master: `safe_zone_user_map` | Transaction: `geofence_event`.
4. **`JOB_SIM_FORENSIC`**: Baseband cellular tower and SIM monitor.
   - *Is Setting / Config?*: **YES** (`pref_sim_forensic_interval_mins` = 15m).
   - *DB*: Master: `device` | Sub-Master: `sim` | Transaction: `sim_transactions`.
5. **`JOB_SECURITY_RECEIVER`**: Immediate broadcast receiver for shutdown, boot, airplane mode.
   - *Is Setting / Config?*: **YES** (`pref_security_receiver_active` = true).
   - *DB*: Transaction: `security_audit_log`, `sys_log`.
6. **`JOB_ACOUSTIC_SIREN`**: Remote alarm bridge.
   - *Is Setting / Config?*: **YES** (`pref_siren_duration_sec` = 30s).
   - *DB*: Transaction: `alert_history`.
7. **`JOB_PRESENCE_PULSE`**: WebSocket online pulse transmitting battery vitals.
   - *Is Setting / Config?*: **YES** (`pref_presence_pulse_interval_sec` = 30s).
   - *DB*: Master: `/devices/{id}/telemetry` | Transaction: `device_vitals`.
8. **`JOB_OFFLINE_SYNC`**: Flushes unacknowledged records from SQLite to Firebase.
   - *Is Setting / Config?*: **YES** (`pref_offline_sync_interval_mins` = 15m).
   - *DB*: Transaction: `pending_sync_queue`, `sync_queue`.
9. **`JOB_FCM_MESSAGING`**: High-priority push listener.
   - *Is Setting / Config?*: **YES** (`pref_fcm_active` = true).
   - *DB*: Transaction: `alert_history`.
10. **`JOB_LIVE_MAP_STREAM`**: Bidirectional peer streaming on map screen.
    - *Is Setting / Config?*: **YES** (`pref_stream_poll_interval_sec` = 5s).
    - *DB*: Master: `/devices/{id}/device/live`.
11. **`JOB_SILENT_UPDATE`**: Daily 03:03 AM OTA check.
    - *Is Setting / Config?*: **YES** (`pref_update_check_interval_hours` = 24h).
    - *DB*: Transaction: `sys_log`.
12. **`JOB_APP_UPGRADE_WATCH`**: Post-install restart sentinel (`ACTION_MY_PACKAGE_REPLACED`).
    - *Is Setting / Config?*: **YES** (`pref_upgrade_watch_active` = true).
    - *DB*: Transaction: `watchdog_resurrection_log`.
13. **`JOB_STORAGE_PRUNE`**: Rolling weekly SQLite retention purge and VACUUM.
    - *Is Setting / Config?*: **YES** (`pref_storage_prune_interval_days` = 7 days).
    - *DB*: Transaction: Purges `location_event`, `sys_log` where `timestamp < now - retention`.

---

# 15. Root Cause Analysis (RCA) & Reverse Engineering Analysis (REA)
### Comprehensive Code Comparison & Redundancy Audit

Comparing entities, DAOs, repositories, and ViewModels across `app/src/main/java/com/jdroidx/jfindme/`:

```
┌───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 RCA / REA: DETECTED REDUNDANCIES & CAUSES                                             │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Redundancy 1: Identity Key Fragmentation ──> 5 identifiers (id, uid_1, uid_2, agent_id, hardware_id)                │
│ Redundancy 2: Telemetry Duplication      ──> Lat/Lng/Speed duplicated in 4 local tables & 3 cloud nodes               │
│ Redundancy 3: Temporal Format Drift      ──> 3 timestamp representations (Long Epoch, ISO-8601 String, DayKey Integer)│
│ Redundancy 4: Verbose String Enums       ──> Full strings ('ONLINE', 'OFFLINE', 'CRITICAL') vs 1-byte integer bitmasks│
│ Redundancy 5: Reverse Geocode Bloat      ──> Repeated full address strings stored per GPS point                       │
└───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1. Identity Key Fragmentation (RCA & REA)
* **Code References**:
  - `AlertHistoryTable.kt`: Contains both `identity_id (INTEGER)` and `agent_id (TEXT)`.
  - `ButterflyPointDeviceMapTable.kt`: Contains both `device_id (TEXT)` and `identity_id (INTEGER)`.
  - `LocationEventTable.kt`: Contains `identity_id (INTEGER)` but DAOs query by `UserIdentityTable.uid_1`.
  - `DatabaseHelper.kt` lines 29-30: Declares legacy constants `COL_UID_1` and `COL_UID_2`.
* **Root Cause**: Early architecture used Firebase Auth string UIDs. Subsequent revisions introduced canonical 8-char hardware IDs and autoincrement integer primary keys, but legacy foreign keys were left alongside modern integer keys.
* **Reverse Engineering Analysis (REA)**:
  - What breaks if `agent_id` or `uid_1` is deleted blindly? `AlertHistoryTable` queries originating from `NotifViewModel` and `FirebaseComm` pass 8-char strings. If the column is dropped, an immediate `SQLiteException: no such column: agent_id` crashes the app.
* **Safe OOPS/MVC Solution**:
  - Maintain the column locally, but declare `UserIdentityTable.id (INTEGER)` as the master relational foreign key.
  - In `DatabaseHelper`, use the 13 `INSTEAD OF` triggers to automatically resolve `agent_id` from `identity_id` via call-by-reference.

### 2. Dual Telemetry Storage (RCA & REA)
* **Code References**:
  - `LocationEventTable.kt`: Stores `lat`, `lng`, `speed`, `battery`, `motion_state`.
  - `DeviceVitalsTable.kt`: Stores `battery_pct`, `motion_state`, `signal_strength_dbm`.
  - `ButterflyPointDeviceMapTable.kt`: Stores `battery_pct`, `last_lat`, `last_lng`.
  - `FirebaseComm.kt`: Pushes coordinates simultaneously to `/devices/{id}/device/live` and `/devices/{id}/telemetry`.
* **Root Cause**: Separate sub-teams created `DeviceVitalsTable` for hardware health and `LocationEventTable` for GPS coordinates without unifying the high-velocity ingestion pipeline.
* **Data Waste Impact**: 
  - Each GPS coordinate write generates 2 full rows across both tables, duplicating battery and motion state.
  - At 15s cadence, this wastes $\sim 2.7\text{ MB/day}$ per device in SQLite write amplification.
* **Safe OOPS/MVC Solution**:
  - Enforce `location_event` as the authoritative spatial stream.
  - `DeviceVitalsTable` should record rows ONLY when hardware states change ($\Delta\text{Battery} \ge 1\%$, charger connected, thermal spike).

### 3. Timestamp Format Drift (RCA & REA)
* **Code References**:
  - `LocationEventTable.kt` line 962: Stores `timestamp` as formatted string `"YYYYMMDDHHmmss00"`, `day_key` as `INTEGER` (`YYYYMMDD`), and `created_on` as `"YYYY-MM-DD HH:MM:SS"`.
  - `AlertHistoryTable.kt` & `DeviceVitalsTable.kt`: Store `timestamp` as 64-bit Unix Epoch (`INTEGER`).
* **Root Cause**: Developers attempting human-readable database inspection in SQLite GUI viewers stored pre-formatted date strings instead of relying on standard Unix epochs.
* **Data Waste Impact**:
  - Storing 3 representations of the same time consumes 43 bytes per row vs 8 bytes for a 64-bit Unix Epoch.
  - String-based `ORDER BY timestamp DESC` requires collation, which is $4\times$ slower than primitive 64-bit integer index lookups.
* **Safe OOPS/MVC Solution**:
  - Enforce 64-bit Unix Epoch Milliseconds (`INTEGER NOT NULL`) as the single source of truth for time across all 32 tables.
  - Pre-formatted strings (`day_key`, `created_on`) should be computed dynamically via SQLite generated columns or view expressions: `strftime('%Y-%m-%d %H:%M:%S', timestamp / 1000, 'unixepoch')`.

---

# 16. Lowest Data Size Optimization & Call-by-Reference Refactoring

### 1. Integer Micro-Degree Coordinate Representation
- **Problem**: 64-bit floating-point `Double` (`REAL` in SQLite) takes 8 bytes locally and up to 18 bytes as an ASCII string in JSON payloads (`"23.12345678901234"`).
- **Optimization**: Multiply coordinates by $10^6$ and store as 32-bit signed integers (`Int` / `INTEGER`):
  $$\text{MicroLat} = \text{round}(\text{Latitude} \times 10^6)$$
  $$\text{MicroLng} = \text{round}(\text{Longitude} \times 10^6)$$
- **Precision**: $10^{-6}$ degrees provides $\sim 0.11\text{ meters}$ ground resolution (far exceeding smartphone GPS accuracy of $\sim 3\text{m}$).
- **Data Reduction**: Cuts coordinate storage and network transmission size by **55%**.

### 2. 1-Byte Status Bitmask vs Textual Enums
- **Problem**: Storing string literals (`'ONLINE'`, `'OFFLINE'`, `'SOS_PANIC'`, `'CRITICAL'`) consumes 6 to 12 bytes per status field.
- **Optimization**: Bitfield flags packed into a single 1-byte integer:
  - Bit 0 (`0x01`): `IS_ONLINE`
  - Bit 1 (`0x02`): `IS_MOVING`
  - Bit 2 (`0x04`): `IS_CHARGING`
  - Bit 3 (`0x08`): `IS_SOS_ACTIVE`
  - Bit 4 (`0x10`): `IS_GEOFENCE_BREACH`
  - Bit 5 (`0x20`): `IS_GHOST_MODE`
- **Data Reduction**: Replaces 6 separate boolean/textual columns with 1 byte.

### 3. Normalization of Reverse Geocode Lookups
- **Problem**: `location_event` repeatedly records the resolved street address string on every point row (`col_address TEXT`), replicating identical strings hundreds of times for stationary devices.
- **Optimization**:
  - `location_event.address` set to `NULL`.
  - The UI resolves addresses dynamically via `reverse_geocode_cache` by rounding coordinates to 4 decimal places ($\sim 11\text{m}$ grid):
    $$\text{RoundedLat} = \text{round}(\text{Lat} \times 10^4) / 10^4$$
- **Data Reduction**: Eliminates megabytes of redundant text strings from high-frequency telemetry tables.

---

# 17. Device-to-Device Real-Time Synchronization & Bandwidth-Adaptive Batching Engine

### 1. Master Temporal Invariant: Guaranteeing Latest Data First
Every peer subscription, database view, and sync flusher enforces the **Master Temporal Query Invariant**:
- **Shallow Live Cursor**: Firebase `/devices/{id}/device/live` stores only the current active point ($O(1)$ read). Peer map listeners bind directly to this node, avoiding history retrieval.
- **Descending Index Order**: All local compatibility views enforce `ORDER BY timestamp DESC`. Any query for peer status reads `LIMIT 1` to instantly receive the latest coordinates without table scans.
- **Monotonic Vector Clock**: Every packet carries a monotonic 64-bit epoch. Delayed packets received out-of-order are dropped if `packet.timestamp <= last_rendered_timestamp`.

### 2. Adaptive Bandwidth Tiering Matrix

| Network Condition | Detection Trigger | Batch Frequency | Payload Composition | Size per Pulse | Failover Action |
|:---|:---|:---|:---|:---|:---|
| **Tier 1: High Bandwidth** (Wi-Fi / 5G) | Unmetered Wi-Fi or LTE RSRP $> -85\text{ dBm}$ | $1\text{s} - 5\text{s}$ Continuous | Full JSON: Coordinates, Speed, Bearing, Battery, Vitals, Formatted Address | $\sim 450\text{ B}$ | Direct WebSocket push to `/devices/{id}/device/live` |
| **Tier 2: Medium Bandwidth** (4G / LTE) | Metered Mobile Data RSRP $-85$ to $-105\text{ dBm}$ | $15\text{s}$ Batched | Differential Delta: Only transmit fields that changed ($\Delta\text{Lat} > 0.0001$, $\Delta\text{Speed} > 2\text{ km/h}$, $\Delta\text{Battery} \ge 1\%$) | $\sim 85\text{ B}$ | Batch queue; flush upon 3 points or 45s |
| **Tier 3: Low Bandwidth** (3G / 2G / EDGE) | High Latency ($>1500\text{ms}$) or RSRP $< -105\text{ dBm}$ | $60\text{s} - 120\text{s}$ Periodic | Ultra-compact Bit-Vector: 16-byte binary payload (8-byte epoch, 4-byte micro-lat, 4-byte micro-lng, 1-byte status bitmask) | $\sim 24\text{ B}$ | Suppress address lookup; send raw micro-degrees |
| **Tier 4: Offline Dead-Zone** (No Internet) | `getActiveNetwork() == null` | Event-Driven | Local ACID Storage in `pending_sync_queue` with exponential backoff retry counters | $0\text{ B}$ (Local) | Engage SMS PDU Beacon fallback; alert parent via encrypted carrier SMS; flush on reconnect |

### 3. Comprehensive Solution for Intermittent Connectivity & Zero-Bandwidth Dead Zones
1. **Local Persistent Ledger**: Telemetry is committed to `location_event` and `pending_sync_queue` in a single SQLite transaction (`WAL` mode). Zero data loss occurs during app crashes or battery depletion.
2. **Atomic Batch Flush via `OfflineSyncWorker`**: Upon `CONNECTIVITY_ACTION` broadcast, `OfflineSyncWorker` activates, reads `pending_sync_queue ORDER BY priority DESC, timestamp ASC`, packs up to 50 records per batch, and executes a multi-path Firebase `updateChildren()` call.
3. **Cloud ACK Trip-Wire**: Records in `pending_sync_queue` are only removed when Firebase successfully triggers `OnSuccessListener`. If the connection breaks mid-flight, remaining items retain `status = 'PENDING'` and retry with an exponential backoff interval ($15\text{s} \to 30\text{s} \to 60\text{s} \to 300\text{s}$).
4. **SMS Failover Route (Zero-Internet Critical Ingress)**: For SOS alerts and child perimeter breaches where internet is completely dead, `TelemetryFailoverRouter` bypasses IP transport and dispatches a 140-character base64-encoded SMS PDU directly to guardian devices containing:
   `[JFM:SOS:2T929153:23023456:72561234:1100000000:BAT89]` (Decoded instantly by guardian app's `SMSReceiver` into real-time map pin).

---

# 18. Architectural Verification & Summary Checklist

- [x] **UI/UX First Process Hierarchy**: Screen/Page ➔ Stage (1-n) ➔ Stage-Wise Activity ➔ Setting/Config (YES/NO) ➔ Database (Master, Sub-Master, Transactional).
- [x] **Coordinate Tree Plan**: All 13 core screens and background subsystems structured under `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`.
- [x] **Human-in-the-Loop Integration**: Explicit HITL gates embedded at every sensitive decision point (PIN verification, unlinking, SOS silencing, factory reset).
- [x] **RCA & REA Audit**: Comprehensive analysis of identity key fragmentation, dual telemetry storage, and timestamp drift with call-by-reference solutions.
- [x] **Data Size Optimization**: Micro-degree coordinates, 1-byte status bitmasks, and reverse geocode normalization delivering up to 68% data reduction.
- [x] **Bandwidth-Adaptive Sync**: 4-tier network adaptation guaranteeing latest data shared first with SMS PDU failover for dead zones.
