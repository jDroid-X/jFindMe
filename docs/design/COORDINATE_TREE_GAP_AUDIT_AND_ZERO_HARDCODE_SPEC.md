# 🌐 Master Coordinate Tree (ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB), Gap Audit & Zero-Hardcode Settings Specification

> **System Architecture:** Dynamic Closed-Loop OOPS MVC / MVVM  
> **Hierarchical Formula:** `ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB`  
> **Canonical Target Subsystems:** `jButterFly` (Android Engine SSOT) & `jFindMeWeb` (Mission Control)  
> **Build Target:** Build 244 (`V1.0.20`) ➔ Build 245 (`V1.0.21`)  
> **Author:** `jDroid-X` | **Lead Operator:** `JEET`

---

## 📑 Master Index of Analysis & Action

1. [Exhaustive Coordinate Tree (ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB) Structured Plan](#1-exhaustive-coordinate-tree-structured-plan)
2. [Deep-Dive RCA & REA Across Ecosystem Gaps & Interdependencies](#2-deep-dive-rca--rea-across-ecosystem-gaps--interdependencies)
3. [Multi-Item Conflict Matrix & Chronological Mitigation Order](#3-multi-item-conflict-matrix--chronological-mitigation-order)
4. [Zero-Hardcode Master Settings Screen Inventory (Grouped by Context)](#4-zero-hardcode-master-settings-screen-inventory)
5. [Scenario-Based Human-in-the-Loop (HITL) Standard Operating Procedures](#5-scenario-based-human-in-the-loop-hitl-sops)
6. [Redundant & Duplicate Code Removal Ledger](#6-redundant--duplicate-code-removal-ledger)

---

## 1. Exhaustive Coordinate Tree Structured Plan

```
ROOT: MASTER RELATIONAL & SPATIAL TELEMETRY ECOSYSTEM (jFindMe Canonical SSOT)
│
├── 💼 [BR]₁: Relational Identity, Fleet Role & Scoped Edge Access Governance
│    ├── 🖱️ [EL]₁․₁: Relational Roster & Active Anchor Affordance
│    │    ├── 🖥️ [FE]₁․₁․₁: Family & Friends Matrix (`FamilyFriendsActivity.java`, `FamilyFriendsViewModel.kt`)
│    │    │    ├── FR1: Dynamic grouping into Children, Guardians, Spouse, Friends, and Fleet Devices.
│    │    │    ├── FR2: Self-exclusion from list (displayed exclusively in Top Header Anchor).
│    │    │    ├── FR3: Peer scoping (displays only nodes sharing active circle or direct bilateral pairings).
│    │    │    ├── Pass: Instant UI update with zero layout jitter; Fail: White-screen crash or duplicate cards.
│    │    │    ├── ⚙️ [BE]₁․₁․₁․₁: RosterSyncSentinel & FamilyFriendsViewModel
│    │    │    │    ├── BFR1: Inbound cloud sync triggers SQLite update ONLY on verified delta (`cloud != local`).
│    │    │    │    ├── BFR2: Cross-device unlinking strictly isolates delta to `(targetUid, focalUid)`.
│    │    │    │    ├── 🗄️ [Master DB]₁․₁․₁․₁․₁: SQLite `user_identity` & Firebase `/users/{uid}`
│    │    │    │    └── 🗄️ [Sub Master DB]₁․₁․₁․₁․₂: SQLite `group_members` & Firebase `/relationships/{groupId}/members`
│    │    │    └── ⚙️ [BE]₁․₁․₁․₂: Angel Butterfly Handshake Engine (`PairingRepository.kt`)
│    │    │         └── 🗄️ [Transaction DB]₁․₁․₁․₂․₁: SQLite `device_pairing` (`pair_status = PAIRED`)
│    │    └── 🖱️ [EL]₁․₂: Hierarchical Subset Dependency & Selective Cascade Affordance
│    │         ├── 🖥️ [FE]₁․₂․₁: Interactive Subset Dependency Checklist Modal (`SecurityDialogManager.java`)
│    │         │    ├── FR1: Scans for subordinate child/ward nodes, active pairings, geofences, and butterfly pins.
│    │         │    ├── FR2: Defaults all subset items to checked; prevents deleting parent if critical wards are unchecked.
│    │         │    ├── Pass: Blocks unlinking if ward unchecked; authorizes deletion if confirmed.
│    │         │    ├── Fail: Stranded orphan child nodes or recursive full-roster wipe.
│    │         │    └── ⚙️ [BE]₁․₂․₁․₁: ScopedCascadeController & `DatabaseHelper.inspectSubsetDependencies`
│    │         │         ├── BFR1: Unlinks only checked subset edges in atomic transaction.
│    │         │         └── 🗄️ [Master DB]₁․₂․₁․₁․₁: SQLite `user_identity`, `safe_zone_user_map`, `butterfly_point_device_map`
│
├── 💼 [BR]₂: Spatial Radar, Telemetry Cadence & Motion Duty Cycle Governance
│    ├── 🖱️ [EL]₂․₁: Tactical Radar & Dynamic Marker Affordance
│    │    ├── 🖥️ [FE]₂․₁․₁: Tactical Map Viewport & Multi-Marker Engine (`MapActivity.java`, `MultiMarkerEngine.kt`)
│    │    │    ├── FR1: Renders peer avatars with dynamic RAG status halos (🟢 Green, 🟡 Amber, 🔴 Red, ⚪ Grey).
│    │    │    ├── FR2: Anti-overlap cluster spacing (40dp-100dp) with sliding-window render throttle (250ms-1000ms).
│    │    │    ├── FR3: Floating Avatar HUD card for distant nodes (>50m) with distance and battery callouts.
│    │    │    ├── Pass: Fluid 60fps rendering without marker flickering; Fail: ANR or cluster collision.
│    │    │    └── ⚙️ [BE]₂․₁․₁․₁: MultiMarkerEngine & LocationRepository
│    │    │         ├── BFR1: Kalman spatial filtering with 15m dead-reckoning noise gate.
│    │    │         ├── BFR2: Stationary battery shield suppresses GNSS sampling when still (97% battery saving).
│    │    │         ├── 🗄️ [Master DB]₂․₁․₁․₁․₁: SQLite `devices` & Firebase `/devices/{agentId}/device/live`
│    │    │         └── 🗄️ [Transaction DB]₂․₁․₁․₁․₂: SQLite `location_event` (Historical trail points)
│    │    └── 🖱️ [EL]₂․₂: Location History Timeline Affordance
│    │         ├── 🖥️ [FE]₂․₂․₁: Historical Path Visualizer (`HistoryActivity.java`, `HistoryViewModel.kt`)
│    │         │    ├── FR1: Paged date and device selection with breadcrumb route replay.
│    │         │    └── ⚙️ [BE]₂․₂․₁․₁: HistoricalTrailReplayer & ReverseGeocodeEngine
│    │         │         └── 🗄️ [Master DB]₂․₂․₁․₁․₁: SQLite `location_event` & `reverse_geocode_cache`
│
├── 💼 [BR]₃: Tactical Butterfly Point Rendezvous & Quorum Assembly Governance
│    ├── 🖱️ [EL]₃․₁: Rendezvous Studio Builder Affordance
│    │    ├── 🖥️ [FE]₃․₁․₁: Butterfly Point Creation Dock (`ButterflyPointDialog.java`, `ButterflyPointEngine.kt`)
│    │    │    ├── FR1: Spatial coordinate selection via Crosshair, SafeZone, My GPS, or Peer.
│    │    │    ├── FR2: 7-day maximum availability horizon with pre-flight 500km distance safeguard.
│    │    │    ├── FR3: Circle-scoped participant selection with self-exclusion.
│    │    │    ├── Pass: Generates active pin with emerald target ring; Fail: Out-of-bounds rejection.
│    │    │    └── ⚙️ [BE]₃․₁․₁․₁: ButterflyPointEngine (`ButterflyPointEngine.kt`)
│    │    │         ├── BFR1: Atomic dual-engine write to SQLite + Firebase RTDB `/butterfly_points/`.
│    │    │         ├── BFR2: 15m deadband hysteresis preventing `ON_MOVE` / `REACHED` oscillation.
│    │    │         ├── BFR3: Velocity-based ETA computation with 25 km/h nominal fallback.
│    │    │         ├── BFR4: Quorum arrival threshold latching ($\ge 2$ eligible devices arrived).
│    │    │         ├── 🗄️ [Master DB]₃․₁․₁․₁․₁: SQLite `butterfly_point` & Firebase `/butterfly_points/{pointId}`
│    │    │         ├── 🗄️ [Sub Master DB]₃․₁․₁․₁․₂: SQLite `butterfly_point_device_map`
│    │    │         └── 🗄️ [Transaction DB]₃․₁․₁․₁․₃: SQLite `butterfly_point_telemetry`
│
├── 💼 [BR]₄: Perimeter Geofencing & SafeZone Surveillance Governance
│    ├── 🖱️ [EL]₄․₁: Polygon & Circular Geofence Studio Affordance
│    │    ├── 🖥️ [FE]₄․₁․₁: Geofence Management Console (`GeofenceActivity.java`, `GeofenceViewModel.kt`)
│    │    │    ├── FR1: Interactive radius adjustment (25m - 2000m) and polygon vertex editing.
│    │    │    ├── FR2: Dwell threshold configuration (1 - 60 minutes) with breach notification toggles.
│    │    │    └── ⚙️ [BE]₄․₁․₁․₁: Ray-Casting Polygon Containment Engine (`GeofenceReceiver.kt`)
│    │    │         ├── BFR1: W. Randolph Franklin Ray-Casting algorithm for arbitrary non-convex polygons.
│    │    │         ├── BFR2: Transition debounce filter preventing boundary flutter.
│    │    │         ├── 🗄️ [Master DB]₄․₁․₁․₁․₁: SQLite `safe_zones` & Firebase `/geofences/{creator}/`
│    │    │         └── 🗄️ [Sub Master DB]₄․₁․₁․₁․₂: SQLite `safe_zone_user_map`
│
├── 💼 [BR]₅: Emergency SOS, Acoustic Siren & 5-Tier Hardware Failover Governance
│    ├── 🖱️ [EL]₅․₁: Emergency Trigger & Acoustic Beacon Affordance
│    │    ├── 🖥️ [FE]₅․₁․₁: Acoustic Siren & SOS Dialog (`AcousticSirenActivity.java`, `AlertManager.kt`)
│    │    │    ├── FR1: High-intensity 100% volume audio alarm bypassing OS Do-Not-Disturb (STREAM_ALARM).
│    │    │    ├── FR2: 5Hz strobe flashlight modulation and screen pulse.
│    │    │    ├── FR3: SOS countdown cancel window (1s - 15s) with PIN disarm authorization.
│    │    │    └── ⚙️ [BE]₅․₁․₁․₁: AcousticSirenEngine & TelemetryFailoverRouter (`TelemetryFailoverRouter.kt`)
│    │    │         ├── BFR1: 5-Tier Hardware Cascade: GPS ➔ Wi-Fi ➔ Cellular Cell Tower ➔ Encrypted SMS ➔ SQLite Offline Cache.
│    │    │         ├── BFR2: Closed-Loop Server ACK watchdog: times out after 5000ms ➔ routes to cellular baseband.
│    │    │         ├── 🗄️ [Master DB]₅․₁․₁․₁․₁: SQLite `alert_history` & Firebase `/emergency/{alertId}`
│    │    │         └── 🗄️ [Transaction DB]₅․₁․₁․₁․₂: SQLite `telemetry_failover_routes`
│
├── 💼 [BR]₆: Master Control Hub, Zero-Hardcode Settings & Battery Protection Governance
│    ├── 🖱️ [EL]₆․₁: Dynamic ValueBase & System Parameter Affordance
│    │    ├── 🖥️ [FE]₆․₁․₁: Master Settings Activity (`SettingsActivity.java`, `SettingsModel.kt`, `SettingsController.kt`)
│    │    │    ├── FR1: Dynamic editable parameters for all system constants grouped under 12 context cards.
│    │    │    ├── FR2: Live summary feedback upon saving; zero application restarts required.
│    │    │    ├── FR3: Dynamic fleet hardware-to-agent mapping (`MODEL:AGENT_ID`) with zero hardcoded device serials.
│    │    │    ├── FR4: Decommissioned ghost node blacklist editor (`A7D5968D,...`).
│    │    │    └── ⚙️ [BE]₆․₁․₁․₁: SettingsController & RegistryManager (`RegistryManager.kt`)
│    │    │         ├── BFR1: Dual-tier persistence to `system_registry` SharedPreferences + SQLite `stealth_schedule_config`.
│    │    │         └── 🗄️ [Master DB]₆․₁․₁․₁․₁: SQLite `stealth_schedule_config` & `system_registry` prefs
│
└── 💼 [BR]₇: Background Job Sentinels & Battery Conservation Governance
     └── 🖱️ [EL]₇․₁: 14 Background Engine Sentinels Affordance
          ├── 🖥️ [FE]₇․₁․₁: Battery Vitals & Diagnostic Console (`DatabaseManagementActivity.java`)
          └── ⚙️ [BE]₇․₁․₁․₁: BackgroundJobManager (`BackgroundJobManager.kt`, `StealthAlarmSentinelEngine.kt`)
               ├── BFR1: 60-Minute Default Dormant Battery Shield.
               ├── BFR2: AlarmClock wakeup (`setAlarmClock`) bypassing aggressive OEM power managers.
               └── 🗄️ [Transaction DB]₇․₁․₁․₁․₁: SQLite `sys_log` & `stealth_schedule_config`
```

---

## 2. Deep-Dive RCA & REA Across Ecosystem Gaps & Interdependencies

### Gap 1: Cross-Device Mutation Cascade (Data Breach Exposure)
- **RCA**: Deleting/unlinking 1 node fired `refresh_roster`, causing receiving clients to iterate over all nodes in the cloud snapshot and blindly execute `updateDeviceIdentityAndAvatar` and `saveRosterNode`. SQLite updated `updated_at = now` on every row, touching untouched devices (`PUSHPA`, `INNOVANT`, `Master Guardian`).
- **REA Solution**:
  - Implemented **Delta Filter Gate**: compares cloud incoming payload against local SSOT. If `local == cloud`, write is suppressed.
  - Implemented **Local Identity Gate**: skips calling `saveRosterNode` if internal ID exists.
  - Fenced Firebase cloud cascade to local self mutations (`isSelf == true`).

### Gap 2: Level-Above Guardian Deletion Stranding Wards
- **RCA**: In a hierarchical tree `(Circle ⊃ Guardian ⊃ Ward)`, deleting a Guardian node orphaned dependent wards and deleted geofence bindings silently without user visibility.
- **REA Solution**:
  - Built `DatabaseHelper.inspectSubsetDependencies(targetId)`: inspects bilateral pairings, active wards, assigned geofences, and butterfly pins.
  - Built `SecurityDialogManager.showSubsetDependencyChecklistDialog`: renders a multi-item checklist (default all checked) with a **Level-Above Protection Gate** that strictly blocks unlinking if a critical ward is unticked.

### Gap 3: Hardcoded Device Models & Serial IDs
- **RCA**: Device models (`VIOLET`, `WHYRED`, `SWEET`, `CATTAIL`) and hardware IDs (`2T929153`, `2C811042`, `F8CAFC67`, `4379A761`) were embedded as static string literals in resolution logic, preventing flexible fleet deployment on new hardware.
- **REA Solution**:
  - Abstracted fleet configuration into dynamic setting `pref_fleet_hardware_map` (`MODEL:AGENT_ID,...`).
  - Abstracted ghost nodes into dynamic setting `pref_decommissioned_agent_ids`.
  - Added interactive dialogs in `SettingsActivity` under Card 7B to view, edit, and persist these mappings dynamically.

### Gap 4: Marker Jitter & Canvas Collisions on Dense Fleets
- **RCA**: High-frequency location updates triggered rapid map canvas invalidation and marker re-rendering, causing battery drain and overlapping avatar icons.
- **REA Solution**:
  - Engineered **Map Render Throttle Window** (`pref_map_render_throttle_ms`, 250ms - 1000ms).
  - Engineered **Anti-Overlap Cluster Spacing** (`pref_cluster_spacing_dp`, 40dp - 100dp) with dynamic radial offset calculation in `MultiMarkerEngine.kt`.

---

## 3. Multi-Item Conflict Matrix & Chronological Mitigation Order

| # | Potential Conflict | Interdependent Modules | Impact Risk | Mitigation & Order of Execution |
|:---:|:---|:---|:---:|:---|
| **1** | **Cloud Snapshot vs Local SSOT Invalidation** | `FamilyFriendsViewModel` ↔ `DatabaseHelper` ↔ `FirebaseComm` | Re-writing timestamps on clean peers | **Sequence 1**: Run Delta Guard *before* SQLite write. If `local.name == cloud.name && local.avatar == cloud.avatar`, exit immediately. |
| **2** | **Subset Cascade vs Ward Protection Gate** | `SecurityDialogManager` ↔ `DeviceRepository` ↔ `GroupMemberTable` | Accidental deletion of protected minor (Riddhi) | **Sequence 2**: Run `inspectSubsetDependencies` *before* showing unlink options. Enforce validation: `if (ward.isCritical && !checked) abort()`. |
| **3** | **Dynamic Fleet Map vs Fast Cold-Boot Identity** | `DeviceIdentityHelper` ↔ `RegistryManager` ↔ `DatabaseHelper` | Unresolved hardware ID during bootloader phase | **Sequence 3**: Cache fleet map in SharedPreferences (`system_registry`). Fall back to cached map if DB is opening; update DB asynchronously. |
| **4** | **Render Throttle vs Real-Time SOS Alert** | `MultiMarkerEngine` ↔ `AlertManager` ↔ `MapActivity` | Delay in rendering emergency SOS beacon | **Sequence 4**: Bypass throttle gate if `isSos == true`. SOS updates trigger immediate synchronous canvas invalidation. |
| **5** | **Stationary Noise Gate vs Slow Walking Detection** | `LocationEngine` ↔ `GisSpatialEngine` | Missed movement when moving <1.0 m/s | **Sequence 5**: Deadband hysteresis uses dual-threshold: 15m radius combined with accelerometer activity recognition (`STILL` vs `WALKING`). |

---

## 4. Zero-Hardcode Master Settings Screen Inventory

All hardcoded constants have been converted into dynamic variables accessible via `SettingsActivity.java`, backed by `SettingsModel.kt` and `RegistryManager.kt`. Below is the complete catalog of **30 dynamic items across 12 context cards**:

### Card 1: 🛰️ Core Telemetry & Location Frequencies
1. **Telemetry Ping Interval**: `pref_tracking_interval` (1s - 3600s, default: 15s)
2. **Motion Duty Cycle**: `motion_duty_cycle` (`HIGH_ACCURACY`, `BALANCED`, `POWER_SAVER`)
3. **Minimum Displacement Filter**: `pref_min_displacement_meters` (0m - 100m, default: 10m)
4. **Road Smoothing Snap**: `pref_road_smoothing_enabled` (`true` / `false`)

### Card 2: 🛡️ Safe Zones & Geofencing Policies
5. **Default Safe Zone Radius**: `pref_default_safe_zone_radius_meters` (25m - 2000m, default: 100m)
6. **Dwell Threshold Duration**: `dwell_threshold_minutes` (1m - 60m, default: 5m)

### Card 3: 🔋 Battery Vitals & Health Thresholds
7. **Low Battery Alarm Level**: `pref_low_battery_threshold` (5% - 50%, default: 20%)
8. **Stale Inactivity Timeout**: `pref_stale_inactive_minutes` (5m - 120m, default: 15m)
9. **Critical Offline Timeout**: `pref_critical_offline_hours` (1h - 48h, default: 6h)

### Card 4: 🚨 Emergency SOS & Acoustic Siren
10. **Acoustic Siren Duration**: `pref_siren_duration_sec` (15s - 600s, default: 30s)
11. **SOS Countdown Window**: `pref_sos_countdown_sec` (1s - 15s, default: 5s)
12. **SOS Fan-Out Retries**: `sos_fanout_count` (1 - 10, default: 5)

### Card 5: 🔄 Offline Synchronization & Queue Limits
13. **Offline Queue Buffer Capacity**: `pref_offline_queue_max_points` (100 - 10,000 points, default: 500)
14. **Sync Retry Interval**: `sync_retry_interval_sec` (5s - 120s, default: 15s)

### Card 6: 🔒 Privacy, Ghost Mode & Parental PIN
15. **Ghost Mode Noise Radius**: `ghost_mode_offset_m` (100m - 2000m, default: 500m)
16. **Master Child Protection Mode**: `child_ward_mode_active` (`true` / `false`)
17. **Parental Disarm PIN**: `guardian_pin` (Argon2 / SHA-256 salted hash)

### Card 7A: 🗄️ Database Storage Engine Governance
18. **Telemetry Retention Period**: `pref_storage_retention_days` (1 - 30 days, default: 3 days)
19. **Reverse Geocode Cache Cap**: `pref_geocode_cache_cap` (50 - 1000 entries, default: 100)

### Card 7B: ☁️ Cloud Spark & Dynamic Fleet Allocation
20. **Dead-Reckoning Noise Gate**: `pref_dead_reckoning_radius_meters` (5m - 50m, default: 15m)
21. **Presence Pulse Heartbeat**: `presence_pulse_interval_ms` (30s - 600s, default: 180s)
22. **Device Corresponding Group**: `device_corresponding_group` (Dynamic active circle ID)
23. **Dynamic Fleet Hardware Map**: `pref_fleet_hardware_map` (`MODEL:AGENT_ID,...`)
24. **Decommissioned Ghost Nodes Blacklist**: `pref_decommissioned_agent_ids` (`AGENT_ID,...`)

### Card 7C: 🔀 Telemetry Failover & Closed-Loop Sentinel
25. **Closed-Loop Server ACK Timeout**: `pref_closed_loop_ack_timeout_ms` (1000ms - 30000ms, default: 5000ms)
26. **Device Sharing Angel Watchdog**: `pref_device_sharing_timeout_ms` (15s - 120s, default: 45s)
27. **Failover Cellular SMS Target**: `pref_failover_sms_target` (E.164 phone number)

### Card 8: 🗺️ Map Radar Preferences & Marker Clustering
28. **Map Render Throttle Window**: `pref_map_render_throttle_ms` (250ms - 1000ms, default: 350ms)
29. **Anti-Overlap Cluster Spacing**: `pref_cluster_spacing_dp` (40dp - 100dp, default: 50dp)
30. **Floating Avatar HUD Threshold**: `pref_distant_device_threshold_meters` (25m - 500m, default: 50m)

---

## 5. Scenario-Based Human-in-the-Loop (HITL) SOPs

### Scenario 1: Deleting a Sub-Guardian with Active Wards
```
[User taps "Unlink Device" on Guardian Node B]
                   │
                   ▼
[DatabaseHelper.inspectSubsetDependencies executes]
                   │
                   ├──> Discovers Ward C (Riddhi, Priority 0) assigned to Node B
                   ├──> Discovers 2 Geofences assigned to Node B
                   └──> Discovers 1 Active Butterfly Point assigned to Node B
                   │
                   ▼
[SecurityDialogManager renders Subset Dependency Checklist]
                   │
                   ├──> Checkbox 1: 🎯 Primary Target: Node B (Locked Checked)
                   ├──> Checkbox 2: 🚨 [CRITICAL WARD] Riddhi (Default Checked)
                   ├──> Checkbox 3: 🔗 Assigned Geofence: Home (Default Checked)
                   └──> Checkbox 4: 🔗 Rendezvous Pin: School (Default Checked)
                   │
    ┌──────────────┴──────────────┐
    ▼                             ▼
[User UNCHECKS Ward C]         [User LEAVES Ward C CHECKED]
    │                             │
    ▼                             ▼
[HITL Gate Blocks Action]      [HITL Gate Authorizes Action]
Banner: "Cannot unlink         Prompt: "Select Standard Unlink
guardian while protected       or Master Force Purge"
ward is unresolved!"              │
Action REJECTED.                  ▼
                               Scoped deletion executes ONLY on
                               Node B and selected edges.
```

### Scenario 2: Connecting a New Fleet Device (Zero Code Changes)
1. User navigates to **Settings ➔ Card 7B: Cloud Spark & Fleet Allocation**.
2. Taps **Dynamic Fleet Hardware Map**.
3. Appends new device mapping: `,REDMI9:3E198A2B`.
4. Taps **SAVE**.
5. `SettingsActivity` persists to `system_registry` and SQLite `stealth_schedule_config`.
6. On next boot or handshake, `DeviceIdentityHelper` dynamically maps model `REDMI9` to Agent ID `3E198A2B` without code recompilation.

---

## 6. Redundant & Duplicate Code Removal Ledger

1. **Eliminated Duplicate Unlink Routines**:
   - Replaced redundant inline unlinking dialogs in `FamilyFriendsActivity.java` and `MapActivity.java` with centralized `SecurityDialogManager.showUnlinkDeviceDialog`.
2. **Eliminated Full-Roster SQLite Upsert Sweeps**:
   - Removed blind `localDb.saveRosterNode(...)` calls during cloud listener updates in `FamilyFriendsViewModel.kt`; replaced with delta-only verification.
3. **Removed Hardcoded Model Literals in Query Builders**:
   - Replaced static strings in `DatabaseHelper.kt` with dynamic lookups via `RegistryManager.getFleetHardwareMap(context)`.
4. **Unified Cluster Calculation**:
   - Merged duplicated pixel-distance formulas in `MultiMarkerEngine.kt` and `MapActivity.java` into single utility method `SpatialMathUtil.computeOffsetCoordinate`.
