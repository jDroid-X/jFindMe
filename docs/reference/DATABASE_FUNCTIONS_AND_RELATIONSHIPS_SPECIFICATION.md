# 🗄️ JFindMe Citizen Data Engine (SQLite V79 Gold Master) — REA Schema Refabrication & SSOT Master Specification

> **Confidential & Proprietary — Antigravity AI Enterprise Development Standard**  
> **Codename:** `jDroid-X` | **Operator:** `JEET` | **Version:** 5.0.0 (Enterprise Gold Master)  
> **Database:** `JFindMe_CitizenData.db` (Version 79)  
> **Architecture:** Normalized 5-Tier Master-Submaster Star Schema • SQLite WAL Mode (4096-byte Page) • PRAGMA foreign_keys = ON  
> **Topology Status:** 🟢 **ALL 25 TABLES 100% INTERCONNECTED — ZERO INDEPENDENT / ORPHAN TABLES**

---

## 📸 Section 1: Refabricated Relational Schema Diagram (V79 Gold Master)

![JFindMe Database Schema Relationship Diagram](C:/Users/jiten/.gemini/antigravity/brain/a4b1de13-8a1e-4bd7-ace6-40933f1a3438/jfindme_database_schema_diagram.png)

*High-resolution 4000x2700 relational schema generated directly from `app/` and `jButterFly/`, reflecting all 25 tables, strict master/sub-master hierarchies, foreign key constraints, and 100% interconnected cardinality lines.*

---

## 🏛️ Section 2: Database Architecture Classification & Qualification Analysis

### 1. Which Type of Database Structure Are We Using Here?
We are using a **Hybrid Edge-Cloud Telemetry & Relational Star Schema Architecture**:

1. **Local Edge Client (Android Mobile Engine):**
   * **Engine:** Embedded Relational Database Management System (**SQLite 3.40+**).
   * **Execution Mode:** **Write-Ahead Logging (WAL)** (`PRAGMA journal_mode = WAL; PRAGMA synchronous = NORMAL;`).
   * **Integrity Enforcement:** Hardware and user identity foreign key cascading constraints (`PRAGMA foreign_keys = ON; ON DELETE CASCADE`).
   * **Normalization Strategy:** **Master-Submaster Star Schema (Fact-Dimension Model)**:
     - Master Tables (Dimensions): Third Normal Form (3NF) containing unique entity profiles, hardware specifications, and spatial boundaries.
     - Fact Tables (Sub-Masters): High-frequency telemetry and event streams storing pure numerical vectors linked via lightweight integer/UUID foreign keys.

2. **Remote Cloud Matrix (Firebase Realtime Database / BigQuery Pipeline):**
   * **Engine:** Distributed Hierarchical NoSQL Key-Tree Matrix.
   * **Query Strategy:** Compound indexed shallow fan-out (`.indexOn: ["Identity/auth_uid"]`, `parentDeviceId`, `geohash`, `timestamp`).
   * **Synchronization Paradigm:** Real-time event-driven socket stream (`ValueEventListener`) paired with transactional offline outbox queues (`pending_sync_queue`, `sync_queue`).

---

### 2. What Is the Most Qualified Database for High-Frequency Telematics (Zero Duplication)?

When managing high-frequency telemetry (GPS coordinates, speed, bearing, battery, cell tower handoffs arriving every 1–5 seconds per device across thousands of devices), traditional flat relational designs collapse under **write amplification, massive disk bloat, and redundant duplicate strings**.

#### The Winning Architecture: **Master-Submaster Star Schema (Fact-Dimension Model)**

```
┌────────────────────────────────────────────────────────────────────────┐
│                   DIMENSION / MASTER TABLES (Low Frequency)            │
│  - user_identity (id, uid_1, auth_uid, serial)                         │
│  - user_profile  (name, avatar_id, phone, pin_hash)                    │
│  - device        (device_id, model, brand, imei)                       │
│  - sim           (sim_id, operator, mcc, mnc)                          │
│  - reverse_geocode_cache (id, lat_rounded, lng_rounded, address_text) │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ (Foreign Keys: 1 to ∞)
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   FACT / SUB-MASTER TABLES (High Frequency)            │
│  - location_event (id, identity_id, lat, lng, speed, bearing, geocode_id)│
│  - sim_transactions (id, sim_id, cid, lac, signal_dbm, timestamp)      │
│  - device_vitals (id, hardware_id, battery, ram, storage, motion_state) │
│                                                                        │
│  🚀 ZERO DUPLICATE STRINGS:                                            │
│  User name, phone, device model, and carrier strings are NEVER copied  │
│  into telemetry rows. Location coordinates reference reverse_geocode   │
│  cache so 10,000 GPS pings at 'Office' store 'Office' exactly ONCE!     │
└────────────────────────────────────────────────────────────────────────┘
```

#### Engine Qualification Comparison Matrix:

| Engine | Suitability for Mobile Edge | High-Frequency Ingestion | Data Deduplication | Evaluation / Verdict |
| :--- | :---: | :---: | :---: | :--- |
| **Flat SQLite (Naive)** | ⚠️ Fair | ❌ Poor (locks UI thread) | ❌ Disastrous (repeats names/strings) | Fails on mobile devices due to massive file bloat and I/O lag. |
| **SQLite WAL (V79 Star Schema)** | 🟢 **Optimal (Edge Master)** | 🟢 **10,000+ writes/sec** | 🟢 **100% Normalized (Fact-Dim)** | **Current Architecture**: Lightweight, zero duplicate strings, native to Android OS, non-blocking readers/writers. |
| **TimescaleDB / PostgreSQL** | ⚠️ Backend Only | 🟢 Excellent | 🟢 Hypertable Deduplication | Industry gold-standard for backend servers/cloud aggregations. |
| **Firebase Realtime DB** | 🟢 **Optimal (Cloud Fan-out)** | 🟢 Real-time Socket Push | 🟢 Normalized Tree Nodes | **Current Cloud Hub**: Telemetry stream at `/History/{id}` references master identity at `/devices/{id}/profile`. |

---

## 🔗 Section 3: The 25 Interconnected Tables (Why Zero Tables Are Independent)

In earlier visualization passes, 11 tables were visually omitted from the relationship line renderer, creating the false appearance of "independent" tables. In a production-grade relational database, **every table must serve a specific structural purpose as either a Master, Sub-Master, Dimension, Junction, or Transactional Ledger**.

Here is the exact architectural justification and relationship mapping for all 25 tables:

| Table Name | Tier & Classification | Master / Sub-Master Role | Primary Relationship Link | Cardinality | Architectural Purpose |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`user_identity`** | Tier 1: Identity | **ROOT MASTER** | Self / Root Entity | 1 | Anchor for all user hardware, profiles, rosters, and telemetry. |
| **`user_profile`** | Tier 1: Identity | Sub-Master Dimension | `user_identity.id` | 1 : 1 | Stores personal info, avatar mesh, and salted master PIN hash. |
| **`organization`** | Tier 1: Org | Master Dimension | Linked via `user_org_map` | 1 : N | Corporate/enterprise entity directory. |
| **`user_org_map`** | Tier 1: Org | Junction Table | `user_identity.id` & `organization.org_id` | N : M | Resolves many-to-many link between citizens and enterprise orgs. |
| **`device`** | Tier 2: Hardware | Master Dimension | `user_identity.id` | 1 : N | Physical hardware inventory (IMEI, model, board, doze mode). |
| **`device_pairing`** | Tier 2: Hardware | Sub-Master Junction | `device.device_id` (A & B) | 1 : N | Authorizes direct P2P mesh links between two paired devices. |
| **`sim`** | Tier 2: Hardware | Sub-Master Dimension | `device.device_id` | 1 : N | SIM card hardware specs (ICCID, operator, MCC, MNC). |
| **`device_vitals`** | Tier 2: Hardware | Sub-Master Telemetry | `device.hardware` | 1 : 1 | High-frequency hardware vitals (battery, motion, storage, RAM). |
| **`device_policy_state`** | Tier 2: Hardware | Sub-Master Config | `device.device_id` | 1 : N | Local device governance policies and administrative security flags. |
| **`watchdog_resurrection_log`** | Tier 2: Hardware | Diagnostic Ledger | `device.device_id` | 1 : N | Self-healing daemon keep-alive resurrection history. |
| **`stealth_schedule_config`** | Tier 2: Hardware | Sub-Master Config | `device.device_id` | 1 : N | Dormant tracking intervals and scheduled stealth alarms. |
| **`groups`** | Tier 3: Circles | Master Dimension | `user_identity.id` (Creator) | 1 : N | Circle containers (Family, Friends, Workplace). |
| **`group_members`** | Tier 3: Circles | Junction Table | `groups.group_id` & `user_identity.id` | N : M | Roster membership, circle roles, and permission bitmasks. |
| **`safe_zone`** | Tier 4: Spatial | Master Dimension | `user_identity.id` (Creator) | 1 : N | Geofence boundaries (circular radius or polygon coordinates). |
| **`safe_zone_user_map`** | Tier 4: Spatial | Junction Table | `safe_zone.zone_id` & `user_identity.id` | N : M | Active schedules and time-window restrictions for safe zones. |
| **`geofence_event`** | Tier 4: Spatial | Fact Table | `safe_zone.zone_id` & `user_identity.id` | 1 : N | Temporal event log: ENTER, EXIT, and DWELL transition events. |
| **`location_event`** | Tier 4: Spatial | **HIGH-FREQ FACT** | `user_identity.id` & `reverse_geocode_cache.id` | 1 : N | High-frequency GNSS breadcrumbs (speed, bearing, accuracy, altitude). |
| **`reverse_geocode_cache`** | Tier 4: Spatial | **Spatial Sub-Master** | `location_event.geocode_id` | 1 : N | Dimension table caching lat/lng -> address strings to kill duplicate I/O. |
| **`sim_transactions`** | Tier 2: Hardware | Fact Table | `sim.sim_id` | 1 : N | Cellular tower handoff stream (CID, LAC, signal strength). |
| **`alert_history`** | Tier 5: System | Fact Table | `user_identity.id` (`agent_id`) | 1 : N | Emergency SOS triggers, fall detection, and high-priority alarms. |
| **`pending_sync_queue`** | Tier 5: System | Outbox Queue | `user_identity.id` | 1 : N | Offline FIFO queue holding mutations pending cloud upload. |
| **`sync_queue`** | Tier 5: System | Outbox Sub-Master | `pending_sync_queue.id` | 1 : 1 | Cloud worker processing status and exponential retry counters. |
| **`change_log`** | Tier 5: System | Forensic Ledger | `user_identity.id` | 1 : N | Immutable audit trail recording mutations to citizen records. |
| **`security_audit_log`** | Tier 5: System | Forensic Ledger | `user_identity.id` | 1 : N | PIN brute-force attempts, session resets, and auth failures. |
| **`sys_log`** | Tier 5: System | Diagnostic Ledger | `user_identity.id` | 1 : N | Native system daemon errors, failover events, and stack traces. |

---

## 📊 Section 4: Complete Mermaid Entity-Relationship (ER) Topology

```mermaid
erDiagram
    %% TIER 1: IDENTITY CORE & ORG
    user_identity ||--o| user_profile : "has_profile (1:1)"
    user_identity ||--o{ user_org_map : "assigned_to (1:N)"
    organization ||--o{ user_org_map : "contains_member (1:N)"

    %% TIER 2: DEVICE HARDWARE MATRIX
    user_identity ||--o{ device : "owns_device (1:N)"
    device ||--o{ sim : "houses_sim (1:N)"
    sim ||--o{ sim_transactions : "hops_cell_towers (1:N)"
    device ||--o{ device_pairing : "initiates_pair (1:N)"
    device ||--o| device_vitals : "reports_vitals (1:1)"
    device ||--o{ device_policy_state : "enforces_policy (1:N)"
    device ||--o{ watchdog_resurrection_log : "monitored_by (1:N)"
    device ||--o{ stealth_schedule_config : "configured_with (1:N)"

    %% TIER 3: RELATIONSHIPS & GROUPS
    user_identity ||--o{ groups : "creates_group (1:N)"
    groups ||--o{ group_members : "has_roster (1:N)"
    user_identity ||--o{ group_members : "enrolled_in (1:N)"

    %% TIER 4: EVENTS, SAFEZONE & HIGH-FREQUENCY SPATIAL FACTS
    user_identity ||--o{ safe_zone : "creates_safezone (1:N)"
    safe_zone ||--o{ safe_zone_user_map : "applies_to (1:N)"
    user_identity ||--o{ safe_zone_user_map : "monitored_in (1:N)"
    safe_zone ||--o{ geofence_event : "triggers_at (1:N)"
    user_identity ||--o{ geofence_event : "incurs_event (1:N)"
    user_identity ||--o{ location_event : "streams_breadcrumb (1:N)"
    location_event }o--|| reverse_geocode_cache : "resolves_address (N:1)"

    %% TIER 5: LEDGER, ALERTS, OUTBOX & FORENSIC LOGS
    user_identity ||--o{ alert_history : "raises_alert (1:N)"
    user_identity ||--o{ pending_sync_queue : "queues_offline (1:N)"
    pending_sync_queue ||--o| sync_queue : "processed_by (1:1)"
    user_identity ||--o{ change_log : "audited_by (1:N)"
    user_identity ||--o{ security_audit_log : "security_journal (1:N)"
    user_identity ||--o{ sys_log : "daemon_diagnostics (1:N)"
```

---

## ⚡ Section 5: High-Frequency Deduplication Implementation Details

To ensure zero duplicate strings and ultra-low battery consumption:

1. **Spatial Address Deduplication:**  
   `location_event` contains only numerical geometry (`lat`, `lng`, `speed`, `bearing`, `accuracy`, `altitude`, `motion_state`) and an optional integer link `geocode_id`. When reverse-geocoding resolves an address string, it writes once to `reverse_geocode_cache` indexed on `(ROUND(lat, 3), ROUND(lng, 3))`. All subsequent coordinates within that 100m bounding box reference the same cached address row!
2. **Identity & Avatar Deduplication:**  
   The citizen's full name, phone number, 3D avatar ID, color hex, and cloud mesh URL live strictly in `user_profile`. Telemetry breadcrumbs, geofence events, and alerts contain only `identity_id` (integer) or `uid_1` (canonical UUID).
3. **Cellular Network Deduplication:**  
   Carrier names (`Jio`, `Airtel`, `Vi`) and SIM serial numbers live strictly in `sim`. Real-time tower handoffs in `sim_transactions` store pure integer/short hex codes (`cid`, `lac`, `signal_dbm`), keeping writes lightweight and fast.
