# 🏛️ Master Architectural Specification: Database Structure, Versions & Dual-Engine SSOT (V1)
> **Author:** jDroid-X (Lead Operator: JEET)  
> **System Architecture:** Dual-Engine Enterprise Database Engine (Local SQLite V83 ⟷ Firebase Realtime Cloud SSOT)  
> **Target Fleets:** `2C811042` (REDMI 5PRO), `2D113789` (INNOVANT), `2T929153` (REDMI N7), `4379A761` (RIDDHI), `F8CAFC67` (PUSHPA)  
> **Twin Module Parity:** 100% SHA-256 byte-for-byte synchronization (`app/` ⟷ `jButterFly/`)  
> **Status:** ARCHIVED FOUNDATION SPECIFICATION (V1)

---

## 1. Executive Summary & Engine Baseline

The system operates on an enterprise dual-engine architecture:
- **Local Engine**: SQLite Database `JFindMe_CitizenData.db` running at **Schema Version 83** across 32 physical tables, 13 latest-data compatibility views, and bidirectional `INSTEAD OF` triggers.
- **Online Cloud SSOT**: Firebase Realtime Database structured under canonical `/devices/{agentId}/` nodes with secondary indexes `/lookup_indices/`, `/pairing/`, `/butterfly_points/`, and `/Version_Update/`.

---

## 2. 7-Stage Lifecycle Pipeline Matrix

```
========================================================================================================================
                                     7-STAGE LIFECYCLE PIPELINE MATRIX
========================================================================================================================
Stage 1: Hardware Anchoring & Zero-State Onboarding  (Splash, Auth, System Boot)
Stage 2: Spatial Mesh & Multi-Node Tactical Radar    (Map, Live Dashboard, Butterfly Hub, Device Sharing)
Stage 3: Boundary Guard, Telemetry & Ingress/Egress  (Geofence, SafeZone Map, History Replay, Notif/SOS, Siren)
Stage 4: Hardware Forensics, Baseband & Telephony    (Health, SIM Sentinel, Cellular Triangulation, Power/Flight)
Stage 5: Enterprise Engine, Sentinels & Background   (13 Background Workers, WorkManager, AlarmManager, Sync)
Stage 6: Security, Vault & Cloud Matrix Governance   (Forensic Vault, Database Mgmt, Firebase Cloud Matrix, Logs)
Stage 7: System Lifecycle, OTA & Factory Purge       (System Update, Factory Reset, Profile, Settings, Org)
========================================================================================================================
```

---

## 3. Screen, Activity & Background Job Database Mapping Table

| Stage | Activity / Screen / Background Job | Database Type | Master Table(s) | Sub-Master / Dimension Table(s) | Transaction / Event Table(s) | Relationship Fields & Join Type |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Stage 1: Hardware Anchoring & Auth** | `SplashActivity` | **Local**: SQLite<br>**Online**: Firebase | `user_identity`<br>`device`<br>`/devices/{id}/Identity` | `user_profile`<br>`device_policy_state`<br>`/devices/{id}/profile` | `sys_log`<br>`security_audit_log` | `user_identity.id = device.identity_id` (`INNER JOIN`)<br>`user_identity.id = user_profile.identity_id` (`LEFT JOIN`) |
| **Stage 1: Hardware Anchoring & Auth** | `AuthActivity` | **Local**: SQLite<br>**Online**: Firebase | `user_identity`<br>`/lookup_indices/{uid}` | `user_profile`<br>`security_master_keys` | `sync_queue`<br>`sys_log` | `user_identity.auth_uid = lookup_indices.auth_uid` (`1:1 Map`)<br>`user_identity.id = security_master_keys.identity_id` (`INNER JOIN`) |
| **Stage 2: Spatial Mesh & Tactical Radar** | `MapActivity` (`MapV2ViewModel`) | **Local**: SQLite<br>**Online**: Firebase | `device`<br>`user_identity`<br>`/devices/{id}` | `device_pairing`<br>`group_members`<br>`/devices/{id}/relationships` | `location_event`<br>`device_vitals`<br>`/devices/{id}/device/live` | `device.identity_id = location_event.identity_id` (`LEFT JOIN`)<br>`device_pairing.device_b_id = device.hardware_id` (`INNER JOIN`) |
| **Stage 2: Spatial Mesh & Tactical Radar** | `ButterflyPointHub` (`ButterflyPointDialog`) | **Local**: SQLite<br>**Online**: Firebase | `butterfly_point`<br>`/butterfly_points/{pointId}` | `butterfly_point_device_map`<br>`safe_zone` | `butterfly_point_telemetry`<br>`alert_history` | `butterfly_point.point_id = butterfly_point_device_map.point_id` (`INNER JOIN`)<br>`butterfly_point_device_map.device_id = device.hardware_id` (`LEFT JOIN`) |
| **Stage 2: Spatial Mesh & Tactical Radar** | `DeviceSharingHub` (Consolidated 4-in-1 Mesh) | **Local**: SQLite<br>**Online**: Firebase | `device`<br>`groups`<br>`/devices/{id}/relationships` | `device_sharing`<br>`device_pairing` | `device_sharing_transactions`<br>`change_log` | `device_sharing.sharing_id = device_sharing_transactions.sharing_id` (`INNER JOIN`)<br>`device_sharing.master_device_id = device.device_id` (`INNER JOIN`) |
| **Stage 2: Spatial Mesh & Tactical Radar** | `NetworkGatewayActivity` (`GatewayViewModel`) | **Local**: SQLite<br>**Online**: Firebase | `device`<br>`user_identity`<br>`/pairing/{code}` | `device_pairing`<br>`invitations` | `change_log`<br>`p2p_mesh_packets` | `device_pairing.device_a_id = device.hardware_id` (`INNER JOIN`)<br>`invitations.group_id = groups.group_id` (`LEFT JOIN`) |
| **Stage 2: Spatial Mesh & Tactical Radar** | `FamilyFriendsActivity` (`FamilyFriendsViewModel`) | **Local**: SQLite<br>**Online**: Firebase | `groups`<br>`user_identity`<br>`/devices/{id}/profile` | `group_members`<br>`user_profile`<br>`device_policy_state` | `device_vitals`<br>`alert_history` | `groups.group_id = group_members.group_id` (`INNER JOIN`)<br>`group_members.identity_id = user_identity.id` (`INNER JOIN`)<br>`user_identity.id = user_profile.identity_id` (`LEFT JOIN`) |
| **Stage 2: Spatial Mesh & Tactical Radar** | `PersonDetailsActivity` | **Local**: SQLite<br>**Online**: Firebase | `user_identity`<br>`device`<br>`/devices/{id}` | `user_profile`<br>`device_policy_state`<br>`/devices/{id}/Access` | `location_event`<br>`device_vitals` | `user_identity.id = user_profile.identity_id` (`INNER JOIN`)<br>`device.device_id = device_policy_state.device_id` (`LEFT JOIN`) |
| **Stage 3: Boundary Guard & Ingress/Egress** | `GeofenceActivity` (`GeofenceViewModel`) | **Local**: SQLite<br>**Online**: Firebase | `safe_zone`<br>`/geofencing/{creatorId}` | `safe_zone_user_map`<br>`group_members` | `geofence_event`<br>`alert_history` | `safe_zone.zone_id = safe_zone_user_map.zone_id` (`INNER JOIN`)<br>`safe_zone_user_map.identity_id = user_identity.id` (`INNER JOIN`) |
| **Stage 3: Boundary Guard & Ingress/Egress** | `GeofenceNotificationActivity` | **Local**: SQLite | `safe_zone` | `safe_zone_user_map` | `geofence_event`<br>`alert_history` | `geofence_event.zone_id = safe_zone.zone_id` (`INNER JOIN`)<br>`geofence_event.identity_id = user_identity.id` (`LEFT JOIN`) |
| **Stage 3: Boundary Guard & Ingress/Egress** | `HistoryActivity` (`HistoryViewModel`) | **Local**: SQLite<br>**Online**: Firebase | `user_identity`<br>`device`<br>`/History_Archive/{id}` | `reverse_geocode_cache` | `location_event`<br>`butterfly_point_telemetry` | `user_identity.id = location_event.identity_id` (`INNER JOIN`)<br>`location_event (lat_round, lng_round) = reverse_geocode_cache (lat_round, lng_round)` (`LEFT OUTER`) |
| **Stage 3: Boundary Guard & Ingress/Egress** | `NotifActivity` (`NotifViewModel`) | **Local**: SQLite<br>**Online**: Firebase | `user_identity`<br>`/devices/{id}/alerts` | `safe_zone`<br>`group_members` | `alert_history`<br>`geofence_event` | `alert_history.identity_id = user_identity.id` (`LEFT JOIN`)<br>`alert_history.zone_id = safe_zone.zone_id` (`LEFT JOIN`) |
| **Stage 3: Boundary Guard & Ingress/Egress** | `AcousticSirenActivity` | **Local**: SQLite | `device` | *(Missing dedicated table)* | `alert_history`<br>`sys_log` | Writes trigger record to `alert_history` with `alert_type='SOS_PANIC'` |
| **Stage 4: Hardware Forensics & Telephony** | `HealthActivity` | **Local**: SQLite<br>**Online**: Firebase | `device`<br>`/devices/{id}/telemetry` | `sim`<br>`device_policy_state` | `device_vitals`<br>`sim_transactions` | `device.device_id = sim.device_id` (`INNER JOIN`)<br>`device.hardware_id = device_vitals.hardware_id` (`LEFT JOIN`) |
| **Stage 4: Hardware Forensics & Telephony** | `SimForensicWorker` / `SIMTrackingService` | **Local**: SQLite | `device` | `sim` | `sim_transactions`<br>`telemetry_failover_routes` | `sim.sim_serial = sim_transactions.sim_serial` (`INNER JOIN`) |
| **Stage 5: Enterprise Engine & Background Jobs** | `TrackingService` (`JOB_TRACKING_SERVICE`) | **Local**: SQLite<br>**Online**: Firebase | `user_identity`<br>`device`<br>`/devices/{id}/device/live` | `reverse_geocode_cache`<br>`device_policy_state` | `location_event`<br>`pending_sync_queue`<br>`device_vitals` | Inserts to `location_event` & queues payload to `pending_sync_queue` if unacknowledged |
| **Stage 5: Enterprise Engine & Background Jobs** | `LocationWatchdogWorker` (`JOB_LOCATION_WATCHDOG`) | **Local**: SQLite | `device` | `device_policy_state` | `watchdog_resurrection_log`<br>`sys_log` | Logs keep-alive heartbeat and restart recovery metrics to `watchdog_resurrection_log` |
| **Stage 5: Enterprise Engine & Background Jobs** | `OfflineSyncWorker` (`JOB_OFFLINE_SYNC`) | **Local**: SQLite<br>**Online**: Firebase | All Master Tables<br>`/devices/{id}/` | All Sub-Master Tables | `pending_sync_queue`<br>`sync_queue`<br>`change_log` | Reads `pending_sync_queue ORDER BY priority DESC, timestamp ASC`; commits to Firebase and deletes from queue on cloud ACK |
| **Stage 5: Enterprise Engine & Background Jobs** | `DatabaseMaintenanceWorker` (`JOB_STORAGE_PRUNE`) | **Local**: SQLite | `stealth_schedule_config` | `reverse_geocode_cache` | `location_event`<br>`sys_log`<br>`geofence_event`<br>`sim_transactions` | Deletes records where `timestamp < (now - retention_days * 86400000)` and invokes `VACUUM` |
| **Stage 5: Enterprise Engine & Background Jobs** | `FCMService` (`JOB_FCM_MESSAGING`) | **Local**: SQLite | `user_identity` | `group_members` | `alert_history`<br>`p2p_mesh_packets`<br>`sys_log` | Parses high-priority push payload, inserts alert, awakens `TrackingService` |
| **Stage 6: Security, Vault & Governance** | `ForensicVaultExplorerActivity` | **Local**: SQLite | All 32 Tables (Tiers 1-3) | 13 Compatibility Views | All Audit Ledgers | Dynamic `PRAGMA table_info()` and paginated `SELECT * FROM table LIMIT 50 OFFSET n` |
| **Stage 6: Security, Vault & Governance** | `DatabaseManagementActivity` | **Local**: SQLite | `stealth_schedule_config` | All 32 Tables | `sys_log`<br>`security_audit_log` | Executes database integrity checks (`PRAGMA quick_check`), index rebuilding, and manual `VACUUM` |
| **Stage 6: Security, Vault & Governance** | `FirebaseCloudMatrixActivity` | **Online**: Firebase | Root `/` | `/devices/`<br>`/lookup_indices/` | `/Change_Log/`<br>`/Version_Update/` | Direct tree traversal inspecting cloud parity and sync latency |
| **Stage 6: Security, Vault & Governance** | `SystemLogActivity` | **Local**: SQLite | *(None)* | *(None)* | `sys_log` (or view `sys_logs`) | Reads `sys_log ORDER BY timestamp DESC LIMIT 500` |
| **Stage 6: Security, Vault & Governance** | `WatchDogAuditActivity` | **Local**: SQLite | `device` | `device_policy_state` | `watchdog_resurrection_log` | `device.hardware_id = watchdog_resurrection_log.hardware_id` (`LEFT JOIN`) |
| **Stage 7: System Lifecycle, OTA & Settings** | `ProfileActivity` / `OrgActivity` | **Local**: SQLite<br>**Online**: Firebase | `user_identity`<br>`organization`<br>`/devices/{id}/profile` | `user_profile`<br>`user_org_map` | `change_log` | `user_identity.id = user_profile.identity_id` (`INNER JOIN`)<br>`user_identity.id = user_org_map.identity_id` (`LEFT JOIN`) |
| **Stage 7: System Lifecycle, OTA & Settings** | `SettingsActivity` | **Local**: SQLite<br>**Online**: Firebase | `user_identity`<br>`device`<br>`/devices/{id}/Access` | `stealth_schedule_config`<br>`device_policy_state` | `change_log`<br>`security_audit_log` | Dynamic key-value reads from `stealth_schedule_config` + Android SharedPreferences (`system_registry`) |
| **Stage 7: System Lifecycle, OTA & Settings** | `SystemUpdateActivity` (`SilentUpdateWorker`) | **Local**: SQLite<br>**Online**: Firebase | `device`<br>`/Version_Update/{id}` | *(Missing update table)* | `sys_log`<br>`security_audit_log` | Compares installed `versionCode` against `/Version_Update/{id}/latest` |
| **Stage 7: System Lifecycle, OTA & Settings** | `FactoryResetActivity` | **Local**: SQLite<br>**Online**: Firebase | All Tables Purged | All Tables Purged | All Tables Purged | Executes confirmed truncation/drop, deletes SQLite database file, clears SharedPreferences, resets cloud node to `GHOST` |

---

## 4. Gap Analysis & Missing Database Connections

1. **`AcousticSirenActivity`**: Lacks dedicated `siren_execution_audit` table.
2. **`UiConfigActivity` / Dynamic Theming**: Stored in SharedPreferences; recommends consolidating into `stealth_schedule_config`.
3. **`SystemUpdateActivity`**: Lacks `system_update_history` table for update-loop protection.
4. **`AvatarEditorActivity` / `AvatarCropActivity`**: Bitmaps stored loose; recommends normalizing via `user_profile.avatar_id` + storage pruning.
5. **`HealthActivity`**: Ephemeral diagnostic runs; recommended to enrich `device_vitals` with TTFF and network diagnostics.

---

## 5. RCA / REA Redundancy Diagnostics

- **Identity Key Fragmentation**: 5 identifiers (`id`, `uid_1`, `uid_2`, `agent_id`, `hardware_id`) causing index bloat.
- **Dual Telemetry Storage**: Coordinates and vitals stored in multiple tables simultaneously.
- **Timestamp Format Drift**: Mixed String formatting and Unix Epoch.
- **Verbose String Enums**: Multi-byte string literals instead of 1-byte integer bitmasks.
- **Reverse Geocode Bloat**: Redundant street strings stored per telemetry point.

---

## 6. Bandwidth-Adaptive Batching Engine

- **Tier 1 (High Bandwidth / Wi-Fi / 5G)**: 1s-5s continuous live streaming.
- **Tier 2 (Medium Bandwidth / 4G / LTE)**: 15s batched differential deltas.
- **Tier 3 (Low Bandwidth / 3G / 2G)**: 60s-120s compact bit-vectors (16 bytes).
- **Tier 4 (Offline Dead-Zone)**: Encrypted `pending_sync_queue` + SMS PDU Fallback Beacon.
