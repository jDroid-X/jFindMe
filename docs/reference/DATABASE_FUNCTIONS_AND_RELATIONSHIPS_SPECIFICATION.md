# 🗄️ JFindMe Citizen Data Engine (SQLite V79 Gold Master) — REA Schema Refabrication & SSOT Master Specification

> **Confidential & Proprietary — Antigravity AI Enterprise Development Standard**  
> **Codename:** `jDroid-X` | **Operator:** `JEET` | **Version:** 5.0.0 (Enterprise Gold Master)  
> **Database:** `JFindMe_CitizenData.db` (Version 79)  
> **Architecture:** Tri-Tier Enterprise Schema (Master • Sub-Master • Transactional OLTP) • SQLite WAL Mode • PRAGMA foreign_keys = ON  
> **Status:** 🟢 **25 TABLES 100% INTERCONNECTED — COMPLETE TRI-TIER TAXONOMY (MASTER • SUB-MASTER • TRANSACTIONAL)**

---

## 📸 Section 1: Refabricated Relational Schema Diagram (V79 Gold Master)

![JFindMe Database Schema Relationship Diagram](C:/Users/jiten/.gemini/antigravity/brain/a4b1de13-8a1e-4bd7-ace6-40933f1a3438/jfindme_database_schema_diagram.png)

*High-resolution 4000x2700 relational schema generated directly from `app/` and `jButterFly/`, reflecting all 25 tables, strict master/sub-master hierarchies, foreign key constraints, and 100% interconnected cardinality lines.*

---

## 🏛️ Section 2: Enterprise Tri-Tier Database Taxonomy

In enterprise operational databases (OLTP - Online Transaction Processing), tables cannot be naively grouped into just two buckets. A production-grade system enforces a strict **Tri-Tier Taxonomy**:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        TIER 1: MASTER TABLES                           │
│   Immutable root entities, canonical anchors, and directory records    │
│   (Low write frequency, high read cache, persistent life-cycle)       │
│   • user_identity   • device   • organization   • groups   • safe_zone │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (1 to 1 / 1 to N Foreign Keys)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     TIER 2: SUB-MASTER TABLES                          │
│   Entity attributes, configurations, dimension caches, and junctions  │
│   (Moderate write frequency, stateful attributes, lookup matrices)     │
│   • user_profile    • sim      • device_vitals  • device_policy_state  │
│   • group_members   • user_org_map • safe_zone_user_map                │
│   • reverse_geocode_cache      • stealth_schedule_config               │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Foreign Key Anchors)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     TIER 3: TRANSACTIONAL TABLES                       │
│   High-frequency events, state-machines, outbox queues, & audit logs   │
│   (Very high write frequency, ACID transaction boundaries, FIFO queues)│
│   • location_event         • geofence_event     • alert_history        │
│   • sim_transactions       • device_pairing     • pending_sync_queue   │
│   • sync_queue             • change_log         • security_audit_log   │
│   • watchdog_resurrection_log                                          │
└────────────────────────────────────────────────────────────────────────┘
```

---

## ⚡ Section 3: Deep Dive — The 10 Transactional Tables (OLTP Layer)

### What Makes a Table "Transactional"?
A **Transactional Table** records real-world state transitions, asynchronous message queues, or high-velocity operational events governed by strict **ACID boundaries** (`db.beginTransaction()` -> `db.setTransactionSuccessful()` -> `db.endTransaction()`). They exhibit:
1. **Append-Heavy / Event-Driven Ingestion:** New records arrive continuously based on sensor, user, or network triggers.
2. **State-Machine Lifecycle:** Rows transition through deterministic states (e.g. `PENDING` -> `PROCESSING` -> `ACKNOWLEDGED` / `FAILED`).
3. **Transactional Outbox Semantics:** Ensures offline-first resilience so no event is lost when cellular connection drops.
4. **Forensic Non-Repudiation:** Append-only audit logs capturing who, what, when, and delta state changes.

### Complete Transactional Ledger Matrix:

| Transactional Table | Trigger Event / Source | Operational Role | Transaction Lifecycle / States | ACID Rollback Boundary | Retention & Pruning Policy |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`pending_sync_queue`** | Offline CRUD mutation (Location, Roster, SOS) | **Transactional Outbox FIFO Queue** | `PENDING` ➔ `IN_FLIGHT` ➔ `COMMITTED` / `RETRY_FAIL` | Rollback on SQLite insert failure; ensures local DB and outbox write atomically. | Pruned immediately upon successful cloud HTTP/Socket ACK. |
| **`sync_queue`** | Background sync worker scheduler | **Sync Worker State Machine** | `PENDING` ➔ `PROCESSING` ➔ `SUCCESS` / `MAX_RETRY_EXCEEDED` | Atomic status updates; prevents concurrent workers from re-processing the same batch. | Retained for 24 hours for worker retry diagnostics. |
| **`device_pairing`** | QR code scan / Invite code exchange | **P2P Handshake State Machine** | `INVITED` ➔ `PENDING` ➔ `VERIFIED` ➔ `ACCEPTED` / `REJECTED` | Atomic swap of Device A & Device B pairing tokens; rollback on validation failure. | Persistent relationship link until explicit unpair. |
| **`sim_transactions`** | Telephony base station cell tower handoff | **Cellular Mast Handoff Ledger** | Append-only event (`CID`, `LAC`, `MCC`, `MNC`, `SIGNAL_DBM`) | Commits on cell tower change detected by `CellTowerSentinel`. | Rolling 7-day circular buffer (max 10,000 hops). |
| **`location_event`** | Fused GNSS / Network / Barometer ping | **High-Frequency Telemetry Stream** | Append-only breadcrumb (`lat`, `lng`, `speed`, `bearing`, `accuracy`, `motion`) | Atomic insert per location fix; references `user_identity` and `reverse_geocode_cache`. | 7-day rolling window on device; archived to cloud. |
| **`geofence_event`** | Spatial polygon / radius boundary crossing | **Spatial Boundary Event Log** | `ENTER` ➔ `DWELL` ➔ `EXIT` | Atomic trigger on transition event; writes event and updates `safe_zone_user_map`. | 30-day forensic retention. |
| **`alert_history`** | Panic button press, fall detection, battery critical | **Emergency Incident Ledger** | `RAISED` ➔ `BROADCASTED` ➔ `ACKNOWLEDGED` ➔ `RESOLVED` | Atomic priority insert; triggers failover SMS/Firebase pipeline. | Permanent forensic storage; soft-delete on archive. |
| **`change_log`** | Any mutation on Citizen Profile or Identity | **Forensic Entity Delta Audit Trail** | Append-only JSON delta (`COL_CHANGED_BY`, `DETAILS_JSON`) | Executes inside the same transaction as the parent entity update; rolls back if parent fails. | Permanent legal/compliance forensic log. |
| **`security_audit_log`** | Master PIN entry, biometric check, tamper alarm | **Security & Cryptographic Gate Log** | Append-only event (`INFO`, `WARNING`, `CRITICAL`) | Immediate atomic commit; isolated from user-space rollbacks to capture attack vectors. | 90-day security audit window. |
| **`watchdog_resurrection_log`** | Android OS kills service; JobScheduler revives daemon | **Daemon Self-Healing Audit Log** | Append-only trigger (`TRIGGER_ACTION`, `SUCCESS`) | Writes on service `onStartCommand` resurrection to monitor Doze kills. | Rolling 30-day health diagnostic log. |

---

## 🔗 Section 4: Master vs. Sub-Master vs. Transactional Mapping

| Tier | Table Name | Type | Master Anchor (FK) | Cardinality | Primary Function |
| :---: | :--- | :--- | :--- | :---: | :--- |
| **1** | **`user_identity`** | **MASTER** | Root Entity Anchor | 1 | Master citizen identity (UUID, auth UID, silicon root). |
| **1** | **`device`** | **MASTER** | `user_identity.id` | 1 : N | Master hardware registry (IMEI, model, board, doze mode). |
| **1** | **`organization`** | **MASTER** | Root Org Directory | 1 | Enterprise/corporate directory anchor. |
| **1** | **`groups`** | **MASTER** | `user_identity.id` | 1 : N | Master circle containers (Family, Friends, Workplace). |
| **1** | **`safe_zone`** | **MASTER** | `user_identity.id` | 1 : N | Master geofence boundary coordinates and polygons. |
| **2** | **`user_profile`** | **SUB-MASTER** | `user_identity.id` | 1 : 1 | Profile attributes, avatar mesh ID, salted PIN hash. |
| **2** | **`user_org_map`** | **SUB-MASTER** | `user_identity.id` & `org_id` | N : M | Junction table resolving user-to-organization assignments. |
| **2** | **`sim`** | **SUB-MASTER** | `device.device_id` | 1 : N | SIM card hardware attributes (ICCID, operator, slot). |
| **2** | **`device_vitals`** | **SUB-MASTER** | `device.hardware` | 1 : 1 | Latest telemetry state (battery, motion classifier, RAM). |
| **2** | **`device_policy_state`** | **SUB-MASTER** | `device.device_id` | 1 : N | Host device security policies and administrative controls. |
| **2** | **`stealth_schedule_config`** | **SUB-MASTER** | `device.device_id` | 1 : N | Dormant tracking timeframes and stealth intervals. |
| **2** | **`group_members`** | **SUB-MASTER** | `groups.group_id` & `user_id` | N : M | Roster membership, circle roles, and permission masks. |
| **2** | **`safe_zone_user_map`** | **SUB-MASTER** | `safe_zone.zone_id` & `user_id` | N : M | Junction resolving time-window schedules for safe zones. |
| **2** | **`reverse_geocode_cache`** | **SUB-MASTER** | `location_event.geocode_id` | 1 : N | Spatial dimension lookup caching coordinates to address text. |
| **3** | **`device_pairing`** | **TRANSACTIONAL** | `device.device_id` (A & B) | 1 : N | P2P handshake transaction ledger (`PENDING` -> `ACCEPTED`). |
| **3** | **`sim_transactions`** | **TRANSACTIONAL** | `sim.sim_id` | 1 : N | Cell tower handoff events stream (`CID`, `LAC`, signal). |
| **3** | **`location_event`** | **TRANSACTIONAL** | `user_identity.id` | 1 : N | High-frequency GPS/telemetry breadcrumb transactions. |
| **3** | **`geofence_event`** | **TRANSACTIONAL** | `safe_zone.zone_id` | 1 : N | Boundary crossing transitions (`ENTER`, `DWELL`, `EXIT`). |
| **3** | **`alert_history`** | **TRANSACTIONAL** | `user_identity.uid_1` | 1 : N | SOS panic alarms and emergency alert transactions. |
| **3** | **`pending_sync_queue`** | **TRANSACTIONAL** | `user_identity.id` | 1 : N | Transactional Outbox holding offline mutations. |
| **3** | **`sync_queue`** | **TRANSACTIONAL** | `pending_sync_queue.id` | 1 : 1 | Worker execution and retry state machine. |
| **3** | **`change_log`** | **TRANSACTIONAL** | `user_identity.id` | 1 : N | Forensic entity delta audit log (INSERT/UPDATE/DELETE). |
| **3** | **`security_audit_log`** | **TRANSACTIONAL** | `user_identity.id` | 1 : N | Authentication, tamper, and security gate events. |
| **3** | **`watchdog_resurrection_log`** | **TRANSACTIONAL** | `device.device_id` | 1 : N | Daemon service survival and resurrection transactions. |

---

## ⚡ Section 5: Why Transactional Decoupling Eliminates Duplicate Data

In flat databases, an emergency alert or a location ping repeats the user's name, phone, device model, and address in every row. Under our **Tri-Tier Architecture**:

1. **Transactional Slimness:** A `location_event` or `alert_history` transaction contains **zero text strings** for user profile or hardware. It contains only `identity_id` (integer) and `timestamp`.
2. **Address Deduplication via Spatial Sub-Master:** `location_event` links to `reverse_geocode_cache`. When a user remains at a desk for 8 hours, generating 5,000 location pings, the address text `"Tower B, Tech Park, Electronic City"` is stored **exactly once** in `reverse_geocode_cache`. All 5,000 transactions point to that single integer ID.
3. **Atomic Outbox Processing:** When an offline action occurs (e.g. creating a safe zone), the database executes an atomic transaction inserting the `safe_zone` record and the `pending_sync_queue` transaction together. If the database crashes mid-way, both rollback cleanly, preventing orphan unsynced records.
