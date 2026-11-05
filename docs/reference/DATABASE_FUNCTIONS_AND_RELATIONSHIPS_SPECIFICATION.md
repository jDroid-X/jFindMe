# 🗄️ JFindMe Citizen Data Engine (SQLite V80 Gold Master) — REA Schema Refabrication & SSOT Master Specification

> **Confidential & Proprietary — Antigravity AI Enterprise Development Standard**  
> **Codename:** `jDroid-X` | **Operator:** `JEET` | **Version:** 5.1.0 (Enterprise Gold Master V80)  
> **Database:** `JFindMe_CitizenData.db` (Version 80)  
> **Architecture:** Tri-Tier Enterprise Schema (Master • Sub-Master • Transactional OLTP) • SQLite WAL Mode • PRAGMA foreign_keys = ON  
> **Status:** 🟢 **25 TABLES 100% INTERCONNECTED — COMPLETE TRI-TIER TAXONOMY (MASTER • SUB-MASTER • TRANSACTIONAL)**  
> **Relational Standard:** Northwind 2003 Access-Style Referential Integrity ($1 \rightarrow \infty$, 0 Orphan Tables)  
> **Governance:** MLM Device Pairing Network Gateway (Top-to-Bottom Permitted • Bottom-to-Top Gated • Zero Device Metadata Cross-Connections)

---

## 📸 Section 1: Refabricated Relational Schema Diagrams (V80 Gold Master)

### 1.1 Authentic Northwind 2003 Relational Model ($1 \rightarrow \infty$ Cardinality Matrix)
![Northwind 2003 Relational Schema Diagram](C:/Users/jiten/.gemini/antigravity/brain/a4b1de13-8a1e-4bd7-ace6-40933f1a3438/jfindme_northwind_relational_diagram.png)

*Ultra-high-resolution 4400x2700 Microsoft Access 2003 style relational schema diagram. Every entity is modeled with deep navy headers, field type abbreviations (`AP`, `T64UI`, `LII`, `DtI`), record counts, and strict orthogonal connector vectors with explicit `1` (one) and `∞` (many) endpoints. Zero orphan tables.*

### 1.2 Tri-Tier Star Schema Topology
![JFindMe Database Schema Relationship Diagram](C:/Users/jiten/.gemini/antigravity/brain/a4b1de13-8a1e-4bd7-ace6-40933f1a3438/jfindme_database_schema_diagram.png)

*High-resolution 4000x2700 structural star-schema diagram detailing Master Anchors, Dimension Sub-Masters, and High-Velocity Transactional Streams.*

---

## 🏛️ Section 2: Zero Cross-Connection Invariant & Hardware Metadata Isolation

### Strict Rule: Device Metadata Independence
Every physical device’s hardware metadata is **strictly independent and autonomous**:
- **No Direct Table Cross-Connections**: Device A never stores, joins, or mutates Device B's internal hardware tables (`devices`, `sim`, `device_vitals`, `device_policy`, `watchdog_resurrection_ledger`).
- **Single Source of Truth (SSOT)**: The ONLY link between physical devices is through the **Relational Identity & Pairing Gateway** (`user_identity` $\longleftrightarrow$ `device_pairing` / `group_members`).
- **Storage Partitioning**:
  - *Local SQLite V80*: Partitioned strictly by silicon root key (`hardware_id`).
  - *Cloud Firebase Matrix*: Partitioned strictly under `/devices/{hardware_id}/` (Hardware Dossier) and `/locations/{agentId}/` (Telemetry Outbox).

---

## 🌳 Section 3: MLM (Multi-Level Management) Asymmetric Hierarchy Protocol

Sharing and data ingestion across the **Device Pairing Network Gateway** are governed by a **Directed Acyclic Tree (MLM Hierarchy)**:

```
                  ┌──────────────────────────────────────────────┐
                  │ LEVEL 1: ROOT GUARDIAN (Node 1)              │
                  │ Device A: Master Admin (allow_dependents=F)  │
                  └──────────────────────┬───────────────────────┘
                                         │
                 ┌───────────────────────┴───────────────────────┐
                 │  Top-to-Bottom Stream (ALLOWED)               │
                 │  Bottom-to-Top Stream (BLOCKED: Gate Closed)  │
                 ▼                                               ▼
   ┌───────────────────────────┐                   ┌───────────────────────────┐
   │ LEVEL 2: GUARDIAN (Node 2)│                   │ LEVEL 2: GUARDIAN (Node 3)│
   │ Device B: Secondary Admin │                   │ Device C: Secondary Admin │
   └─────────────┬─────────────┘                   └─────────────┬─────────────┘
                 │                                               │
                 ▼ (Top-to-Bottom: ALLOWED)                      ▼ (Top-to-Bottom: ALLOWED)
   ┌───────────────────────────┐                   ┌───────────────────────────┐
   │ LEVEL 3: CHILD (Node 4)   │                   │ LEVEL 3: CHILD (Node 5)   │
   │ Device D: Dependent Ward  │                   │ Device E: Dependent Ward  │
   └───────────────────────────┘                   └───────────────────────────┘
                 ▲                                               ▲
                 └─────────── NO CROSS-BRANCH SNOOPING ──────────┘
                              (Zero Lateral Connection)
```

1. **Top-to-Bottom Visibility ($\Downarrow$ Default Permitted)**:
   - Superior nodes (Level 1 Root Guardian / Level 2 Supervisor) can inspect live GNSS telemetry, battery vitals, safezone dwell alerts, and historical breadcrumbs of all subordinate nodes in their branch.
2. **Bottom-to-Top Visibility ($\Uparrow$ Strictly Prohibited Unless Permitted)**:
   - Subordinate nodes (Level 3+ Dependent Child / Ward) **cannot** see the guardian's live location or vitals.
   - **Permission Gate (`allow_dependents`)**: Upstream visibility is unlocked **only** when the top device flips `allow_dependents = true` (or bit `0x08` in `group_members.permissions_mask`). If `false`, the guardian's avatar marker is completely omitted from the child's map canvas.
3. **Cross-Branch / Lateral Isolation**:
   - Nodes on parallel branches cannot inspect each other unless explicitly linked through a mutual Group Gateway with administrative permissions.
4. **Emergency SOS Bypass**:
   - SOS panic broadcasts bypass the downstream filter and propagate **upward to all superior guardians immediately**.

---

## ⚡ Section 4: Enterprise Tri-Tier Database Taxonomy (V80 Master Schema)

```
┌────────────────────────────────────────────────────────────────────────┐
│                        TIER 1: MASTER TABLES                           │
│   Immutable root entities, canonical anchors, and directory records    │
│   (Low write frequency, high read cache, persistent life-cycle)       │
│   • user_identity   • devices   • groups   • safe_zones                │
│   • security_master_keys                                               │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (1 to 1 / 1 to N Foreign Keys)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     TIER 2: SUB-MASTER TABLES                          │
│   Entity attributes, configurations, dimension caches, and junctions  │
│   (Moderate write frequency, stateful attributes, lookup matrices)     │
│   • user_profile    • sim      • device_vitals  • device_policy        │
│   • group_members   • user_org_map • safe_zone_user_map                │
│   • stealth_schedule_config    • reverse_geocode_cache                 │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Foreign Key Anchors)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     TIER 3: TRANSACTIONAL TABLES                       │
│   High-frequency events, state-machines, outbox queues, & audit logs   │
│   (Very high write frequency, ACID transaction boundaries, FIFO queues)│
│   • location_event         • pending_sync_queue (V80 Sensor Queue)     │
│   • alert_history          • watchdog_resurrection_ledger              │
│   • security_evasion_audit_log • telemetry_failover_routes             │
│   • sys_logs               • p2p_mesh_packets   • invitations          │
│   • sim_transactions                                                   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Section 5: Master vs. Sub-Master vs. Transactional Mapping (25 Tables)

| Tier | Table Name | Type | Master Anchor (FK) | Cardinality | Primary Function & Invariant |
| :---: | :--- | :--- | :--- | :---: | :--- |
| **1** | **`user_identity`** | **MASTER** | Root Entity Anchor | 1 | Master citizen identity (UUID, auth UID, `node_index`, silicon root). |
| **1** | **`devices`** | **MASTER** | Independent Root | 1 | Master hardware registry (`hardware_id`, model, OS, app version). **Isolated.** |
| **1** | **`groups`** | **MASTER** | `user_identity.id` | 1 : N | Master circle containers (Family, Friends, Workplace). |
| **1** | **`safe_zones`** | **MASTER** | `user_identity.id` | 1 : N | Master geofence boundary coordinates, radius, and polygons. |
| **1** | **`security_master_keys`** | **MASTER** | `user_identity.id` | 1 : N | Cryptographic key store for device-level digital signatures. |
| **2** | **`user_profile`** | **SUB-MASTER** | `user_identity.id` | 1 : 1 | Profile attributes, avatar URL, nickname, Argon2id salted PIN hash. |
| **2** | **`user_org_map`** | **SUB-MASTER** | `user_identity.id` | 1 : N | Junction table resolving user clearance level and role type. |
| **2** | **`group_members`** | **SUB-MASTER** | `groups.id` & `user_identity.id` | N : M | Circle membership, roles, and bitwise `permissions_mask`. |
| **2** | **`safe_zone_user_map`**| **SUB-MASTER** | `safe_zones.id` & `user_identity.id`| N : M | Junction resolving time-window schedules for safe zones. |
| **2** | **`sim`** | **SUB-MASTER** | `devices.id` | 1 : N | Cellular slots (ICCID, carrier, serving CID). **Bound to single device.** |
| **2** | **`device_policy`** | **SUB-MASTER** | `devices.id` | 1 : 1 | Device governance policies (Kiosk lock, mock location ban). **Isolated.** |
| **2** | **`stealth_schedule_config`**| **SUB-MASTER** | `user_identity.id` | 1 : N | Stealth schedule configuration (quiet hours, tracking duty cycle). |
| **2** | **`reverse_geocode_cache`** | **SUB-MASTER** | `location_event.id` | 1 : N | Spatial coordinate-to-address text lookup dimension. |
| **3** | **`location_event`** | **TRANSACTIONAL**| `user_identity.id` | 1 : N | High-frequency GNSS breadcrumbs (`lat`, `lng`, `speed`, `bearing`, `accuracy`, `altitude`, `motion_state`). |
| **3** | **`pending_sync_queue`** | **TRANSACTIONAL**| `user_identity.uid_1` | 1 : N | **V80 Zero-Loss Offline Outbox**: Stores `accuracy`, `altitude`, `motion_state`. Indexed via `idx_pending_sync_status_ts`. |
| **3** | **`device_vitals`** | **TRANSACTIONAL**| `devices.hardware_id` | 1 : N | Real-time battery, thermal, RAM, and storage diagnostics stream. **Isolated.** |
| **3** | **`alert_history`** | **TRANSACTIONAL**| `user_identity.id` | 1 : N | SOS panic alarms and geofence breach incident transactions. |
| **3** | **`watchdog_resurrection_ledger`**| **TRANSACTIONAL**| `devices.hardware_id` | 1 : N | Daemon service survival and resurrection transactions. **Isolated.** |
| **3** | **`security_evasion_audit_log`** | **TRANSACTIONAL**| `user_identity.id` | 1 : N | Root detection, mock GPS, and security evasion audit trail. |
| **3** | **`telemetry_failover_routes`** | **TRANSACTIONAL**| `user_identity.id` | 1 : N | 5-tier failover circuit breaker route health logs. |
| **3** | **`sys_logs`** | **TRANSACTIONAL**| `user_identity.id` | 1 : N | Forensic system diagnostic logs and exception traces. |
| **3** | **`p2p_mesh_packets`** | **TRANSACTIONAL**| `user_identity.id` | 1 : N | Offline BLE/Wi-Fi Direct mesh hop packets. |
| **3** | **`invitations`** | **TRANSACTIONAL**| `groups.id` & `user_identity.id` | 1 : N | Asymmetric pairing invitations and QR handshake tokens. |
| **3** | **`sim_transactions`** | **TRANSACTIONAL**| `sim.id` | 1 : N | Cellular tower handoff stream (`CID`, `LAC`, `MCC`, `MNC`, signal). |
| **3** | **`geofence_events`** | **TRANSACTIONAL**| `safe_zones.id` | 1 : N | Geofence transition stream (`ENTER`, `DWELL`, `EXIT`). |

---

## ⚡ Section 6: V80 Telemetry Zero-Loss Sync Pipeline

```mermaid
flowchart TD
    subgraph SENSOR_CAPTURE ["1. Sensor Capture (GNSS / Barometer / IMU)"]
        A["Hardware Fix: lat, lng, speed, bearing, accuracy, altitude, motion_state"]
    end

    subgraph GATEWAY ["2. Connectivity Router"]
        A --> B{"Network Available?"}
        B -->|YES| C["Firebase Realtime Matrix (Direct Sync)"]
        B -->|NO| D["OfflineSyncManager.syncLocation()"]
    end

    subgraph OFFLINE_BUFFER ["3. SQLite V80 Store-and-Forward Vault"]
        D --> E["pending_sync_queue (V80 Schema)"]
        E -->|Compound Index: idx_pending_sync_status_ts| F[("FIFO Queue (100% Sensor Data Retained)")]
    end

    subgraph FLUSH ["4. Reconnection & Flush"]
        G["Network Reconnected"] --> H["processQueue() & reconcileYesterdayTodayBalance()"]
        F -->|O(log N) Batch Drain| H
        H -->|Zero Sensor Loss| C
        H -->|ACK Verified| I["DELETE FROM pending_sync_queue WHERE id = ?"]
    end
```
