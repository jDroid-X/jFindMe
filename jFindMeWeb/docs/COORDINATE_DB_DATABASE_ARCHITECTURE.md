# 🗄️ Coordinate DB: Dual-Engine Database Architecture (Functional & Technical)
## jFindMe / jButterfly Enterprise Ecosystem & jFindMeWeb Subsystem
> **Enterprise Standard v5.0** | **Codename:** `jDroid-X` | **Operator:** `JEET`  
> **Repository Target:** `C:\Users\jiten\jAnitGravity\jFindMe\jFindMeWeb`  
> **Coordinate:** `DB` in `ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB`  
> **Methodology:** Dual-Engine Cloud-Edge Synchronization & 17 Normalized SQLite Tables

---

## 1. Dual-Engine Closed-Loop Architecture Overview

The `jFindMe` ecosystem utilizes a **Dual-Engine Database Architecture** providing an immutable local offline ledger at the mobile edge and a synchronized cloud real-time graph:

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                             DUAL-ENGINE DATABASE SYNCHRONIZATION PIPELINE                        │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                  ┌──────────────────────────────┴──────────────────────────────┐
                  ▼                                                             ▼
     [ EDGE LOCAL STORAGE: SQLite ]                               [ CLOUD STORAGE: Firebase RTDB ]
     File: `JFindMe_CitizenData.db`                                Project: `jfindme-3b15c`
     ├── Schema Version: 75 / 76                                   ├── Real-Time WebSocket Graph
     ├── 17 Normalized Relational Tables                           ├── NoSQL Hierarchical Trees
     ├── Zero External Dependencies                                ├── Server-Sent Events (SSE)
     └── Rolling Storage Hygiene & VACUUM                          └── Multi-Client Distribution
                  │                                                             │
                  └──────────────────────────────┬──────────────────────────────┘
                                                 │
                                                 ▼
                             [ ATOMIC 1-CLICK BI-DIRECTIONAL SYNC ]
                             File: `database-sync-settings.html`
                             ├── 178 Master Parameters Across 20 Modules
                             ├── Last-Write-Wins (LWW) Conflict Resolution
                             └── Transactional Rollback Protection
```

---

## 2. 17 Normalized SQLite Tables (Technical & Functional Specifications)

The edge database (`JFindMe_CitizenData.db`) contains 17 normalized relational tables:

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                              17 NORMALIZED SQLITE TABLES SPECIFICATION                           │
├─────┬────────────────────────┬───────────────────────────────────┬───────────────────────────────┤
│ #   │ Table Name             │ Primary Key / Indexes             │ Functional Description        │
├─────┼────────────────────────┼───────────────────────────────────┼───────────────────────────────┤
│ 01  │ `user_identity`        │ `id` (INTEGER PK AUTO), `email`   │ Operator credentials, tokens  │
│ 02  │ `user_profile`         │ `id` (PK), `user_id` (FK)         │ Display name, avatar, bio     │
│ 03  │ `devices`              │ `id` (PK), `device_serial` (UQ)   │ Silicon root descriptors, IMEI│
│ 04  │ `device_pairings`      │ `id` (PK), `pair_code`            │ Symmetrical pairing matrix    │
│ 05  │ `device_vitals`        │ `id` (PK), `device_id` (FK)       │ Battery, voltage, temp, route │
│ 06  │ `location_event`       │ `id` (PK), `device_id`, `ts`      │ Breadcrumb coordinates, speed │
│ 07  │ `geofence_events`      │ `id` (PK), `fence_id`, `ts`       │ Perimeter entry/exit logs     │
│ 08  │ `safe_zones`           │ `id` (PK), `zone_name`            │ Circle & Polygon boundaries   │
│ 09  │ `safe_zone_user_map`   │ `id` (PK), `zone_id`, `user_id`   │ User-to-SafeZone assignments  │
│ 10  │ `groups`               │ `id` (PK), `group_code` (UQ)      │ Multi-tier fleet circles      │
│ 11  │ `group_members`        │ `id` (PK), `group_id`, `user_id`  │ Group membership roster       │
│ 12  │ `organizations`        │ `id` (PK), `org_code` (UQ)        │ Top-level enterprise tenant   │
│ 13  │ `user_org_map`         │ `id` (PK), `user_id`, `org_id`    │ User organization tenancy     │
│ 14  │ `sim_transactions`     │ `id` (PK), `iccid`, `lac`, `cid`  │ SIM changes & cell tower log  │
│ 15  │ `sync_queue`           │ `id` (PK), `action_type`, `ts`    │ Offline outbound mutations    │
│ 16  │ `pending_sync_queue`   │ `id` (PK), `priority`, `retries`  │ Failed transmission retries   │
│ 17  │ `sys_log`              │ `id` (PK), `tag`, `level`, `ts`   │ Rolling forensic logcat buffer│
└─────┴────────────────────────┴───────────────────────────────────┴───────────────────────────────┘
```

---

## 3. Firebase Realtime Database Cloud SSOT Schema

The cloud repository (`https://jfindme-3b15c-default-rtdb.firebaseio.com/`) serves as the central synchronization hub:

```json
{
  "devices": {
    "<silicon_id>": {
      "alias": "Patrol Alpha",
      "battery_level": 88,
      "battery_status": "CHARGING",
      "imei": "864201048291024",
      "model": "Pixel 8 Pro",
      "phone": "+15550192834",
      "active_route": "CELLULAR_PRIMARY",
      "ack_rtt_ms": 78,
      "rag_status": "GREEN",
      "last_updated": 1798884920000
    }
  },
  "live-map": {
    "<silicon_id>": {
      "latitude": 37.7749,
      "longitude": -122.4194,
      "accuracy": 4.2,
      "altitude": 18.5,
      "speed": 12.4,
      "bearing": 184.2,
      "timestamp": 1798884920000
    }
  },
  "safe_zones": {
    "<zone_id>": {
      "name": "Headquarters Perimeter",
      "shape": "POLYGON",
      "vertices": [
        {"lat": 37.775, "lng": -122.420},
        {"lat": 37.776, "lng": -122.418},
        {"lat": 37.773, "lng": -122.419}
      ],
      "alert_on_entry": true,
      "alert_on_exit": true,
      "assigned_devices": ["<silicon_id>"]
    }
  },
  "alerts": {
    "sos": {
      "<alert_id>": {
        "device_id": "<silicon_id>",
        "latitude": 37.7749,
        "longitude": -122.4194,
        "acoustic_triggered": true,
        "acknowledged": false,
        "timestamp": 1798884920000
      }
    }
  },
  "sync_settings": {
    "module_01_battery": {
      "telemetry_interval_active_s": 15,
      "telemetry_interval_battery_saver_s": 300,
      "low_battery_threshold_pct": 20
    }
  }
}
```

---

## 4. 178 Master Parameter Bi-Directional Synchronization Matrix

The platform's 178 synchronization parameters are organized across 20 functional modules, visualized and managed via `database-sync-settings.html`:

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                         20-MODULE DUAL-ENGINE SYNCHRONIZATION BREAKDOWN                          │
├──────┬────────────────────────────────────────────┬─────────────┬────────────────────────────────┤
│ Mod# │ Functional Module Name                     │ Params Count│ Primary Database Mapping       │
├──────┼────────────────────────────────────────────┼─────────────┼────────────────────────────────┤
│ M01  │ Battery & Power Duty Cycles                │ 12 Params   │ `device_vitals` / `sync_set`   │
│ M02  │ GPS Telemetry & Sensor Duty Cycles         │ 12 Params   │ `location_event` / `live-map`  │
│ M03  │ Cellular Baseband & SIM Diagnostics        │ 12 Params   │ `sim_transactions`             │
│ M04  │ Network Gateway & Symmetrical Pairing      │ 10 Params   │ `device_pairings`              │
│ M05  │ Geofencing & Perimeter Defense             │ 12 Params   │ `safe_zones`, `geofence_events`│
│ M06  │ Acoustic Siren & Emergency SOS             │ 10 Params   │ `alerts/sos`                   │
│ M07  │ Historical Breadcrumb Trails & Replay      │ 10 Params   │ `location_event`               │
│ M08  │ Hardware Vitals & Closed-Loop ACK          │ 10 Params   │ `devices.ack_rtt_ms`           │
│ M09  │ WatchDog Heartbeat & Resurrection          │ 10 Params   │ `sys_log`                      │
│ M10  │ System Logcat & Forensic Terminal          │ 8 Params    │ `sys_log`                      │
│ M11  │ Privacy Matrix & Ghost Mode Locks          │ 8 Params    │ `user_identity`                │
│ M12  │ UiArchitect Theme & Sizing Matrix          │ 10 Params   │ LocalStorage & Cloud Theme     │
│ M13  │ 4-Tier Background Job Waterfall            │ 12 Params   │ WorkManager Job Registry       │
│ M14  │ Encrypted Forensic SQLite Vault            │ 8 Params    │ PRAGMA user_version / Cipher   │
│ M15  │ Cloud Realtime Database Mesh               │ 8 Params    │ `/.info/connected`             │
│ M16  │ Automated Silent APK Updater               │ 8 Params    │ `apk/version.properties`       │
│ M17  │ Factory Reset & Cryptographic Purge        │ 6 Params    │ Table TRUNCATE / Cloud Delete  │
│ M18  │ Tactical Radar & Map Optimization          │ 6 Params    │ Tile Cache & Marker Cluster    │
│ M19  │ Eagle Analytics Deep-Dive Cluster          │ 4 Params    │ D3 Analytics Aggregation       │
│ M20  │ Telemetry Failover Router & Breaker        │ 2 Params    │ Sentinel Circuit Breaker State │
├──────┴────────────────────────────────────────────┼─────────────┼────────────────────────────────┤
│ TOTAL MASTER PARAMETERS                           │ 178 Params  │ 100% Bi-Directional Coverage   │
└───────────────────────────────────────────────────┴─────────────┴────────────────────────────────┘
```

---

## 5. Atomic 1-Click Synchronization Protocol & Conflict Resolution

### 5.1 Reconciliation Flow:
1. **Hash Verification**: Web console requests MD5/SHA-256 fingerprint of current SQLite table state via API proxy.
2. **Diff Calculation**: Compares each parameter against the cloud node.
3. **Conflict Resolution Policy**:
   - **Default**: Last-Write-Wins (LWW) based on NTP-synchronized UTC millisecond timestamps.
   - **Operator Override**: Slide-out right drawer provides one-click "Force Push (Cloud -> Device)" or "Force Pull (Device -> Cloud)".
4. **Transactional Commit**: Updates are wrapped in an atomic SQLite `BEGIN TRANSACTION` ... `COMMIT` block on the device and a multi-path `.update()` in Firebase.
5. **Closed-Loop Audit**: `ACTION_PROFILE_SYNCED` broadcast fires, updating the green `SYNC_ACTIVE` pulse badge on Web Mission Control.

