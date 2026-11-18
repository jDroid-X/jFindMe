# 🔍 Deep Dive Due Diligence, Reverse Engineering & Comprehensive RAG Audit Report
> **Codename:** `jDroid-X` | **Lead Operator:** `JEET`  
> **System Architecture:** Single Canonical Module Pipeline (`:jButterFly` SSOT)  
> **Target Subsystem:** Butterfly Point Rendezvous Hub & Tactical Map Subsystem  
> **Date:** November 2026 | **Build Version:** Build 216 (`V1.0.19`)  
> **Standard:** Dynamic OOPS MVC, 22 Enterprise AI Agents, 7-Stage Closed-Loop Waterfall

---

## 📑 Table of Contents
1. [Executive Briefing: The True Operational Reality](#1-executive-briefing-the-true-operational-reality)
2. [Explorer Tree View: Architectural Components Grouped by Context](#2-explorer-tree-view-architectural-components-grouped-by-context)
   - [Context 1: Map Presentation & UI Controllers](#context-1-map-presentation--ui-controllers)
   - [Context 2: Core Domain & Spatial Mathematics](#context-2-core-domain--spatial-mathematics)
   - [Context 3: Storage & 3-Tier Database Persistence](#context-3-storage--3-tier-database-persistence)
   - [Context 4: Cloud Real-Time Synchronization & Ingress](#context-4-cloud-real-time-synchronization--ingress)
   - [Context 5: Alert, Notification & Operator Intervention](#context-5-alert-notification--operator-intervention)
   - [Context 6: System Configuration, Registry & Quality Assurance](#context-6-system-configuration-registry--quality-assurance)
3. [Comprehensive 3-Tier Database Schema & SQLite vs. Firebase RTDB Mapping](#3-comprehensive-3-tier-database-schema--sqlite-vs-firebase-rtdb-mapping)
4. [Reverse-Engineering Audit: 10 Architectural Gaps & Vulnerabilities (GAP-01 to GAP-10)](#4-reverse-engineering-audit-10-architectural-gaps--vulnerabilities-gap-01-to-gap-10)
   - [GAP-01: Field Omission in Cloud Serialization (`notes` & `icon_type`)](#gap-01-field-omission-in-cloud-serialization-notes--icon_type)
   - [GAP-02: High-Velocity Telemetry Overwrite vs. Append Disparity (Tier 3)](#gap-02-high-velocity-telemetry-overwrite-vs-append-disparity-tier-3)
   - [GAP-03: In-Memory Concurrency Race Condition on `activeButterflyPoint.devices`](#gap-03-in-memory-concurrency-race-condition-on-activebutterflypointdevices)
   - [GAP-04: Whole-Object Cloud Write Race Condition During Point Dispatch](#gap-04-whole-object-cloud-write-race-condition-during-point-dispatch)
   - [GAP-05: Permissive Security Rules & Missing ACL Granularity in Firebase RTDB](#gap-05-permissive-security-rules--missing-acl-granularity-in-firebase-rtdb)
   - [GAP-06: Decoupled Push Notification Persistence Gap (FCM vs TrackingService)](#gap-06-decoupled-push-notification-persistence-gap-fcm-vs-trackingservice)
   - [GAP-07: Unbounded Real-Time Listener Accumulation (Memory & Socket Leak)](#gap-07-unbounded-real-time-listener-accumulation-memory--socket-leak)
   - [GAP-08: Time Extension Partial Failure / Split-Brain Timestamps](#gap-08-time-extension-partial-failure--split-brain-timestamps)
   - [GAP-09: Column Name and Type Inconsistencies Between SQLite and RTDB](#gap-09-column-name-and-type-inconsistencies-between-sqlite-and-rtdb)
   - [GAP-10: Ingress Availability Ceiling Precondition Enforcement Gap](#gap-10-ingress-availability-ceiling-precondition-enforcement-gap)
5. [Closed Feedback Loop & Engineering Resolution Matrix](#5-closed-feedback-loop--engineering-resolution-matrix)
6. [Updated Tactical RAG (Red / Amber / Green / Grey) Health Monitoring Status](#6-updated-tactical-rag-red--amber--green--grey-health-monitoring-status)
7. [Verification & Attestation Commands](#7-verification--attestation-commands)

---

## 1. Executive Briefing: The True Operational Reality

Following operator directives, an exhaustive reverse-engineering survey was conducted across all hardware, database, baseband, power management, and cloud synchronization layers of the **Butterfly Point Rendezvous Hub**.

The subsystem features an advanced **3-tier data architecture** that synchronizes local SQLite tables with Firebase Realtime Database (RTDB) nodes in real time ($< 250\text{ms}$). However, deep-dive forensic code examination identified **10 critical architectural gaps, concurrency hazards, and schema inconsistencies** (GAP-01 through GAP-10) that require formal engineering specifications and closed feedback loop remediations.

---

## 2. Explorer Tree View: Architectural Components Grouped by Context

```
📦 Butterfly Point Rendezvous Hub Architecture
├── 🌐 CONTEXT 1: Map Presentation & UI Controllers
│   ├── 📄 MapActivity.java (Lines 151-160, 237, 258-290, 463-473, 1703-1717, 2989-2993, 3135-3137, 3194-3196, 3290-3323, 3923-3985, 4864-4905)
│   │   ├── Deep-link Intent receiver: EXTRA_FOCUS_BUTTERFLY_POINT_ID
│   │   ├── Floating Action Dock: ACTION_BUTTERFLY_POINT (Short click: Dialog / Long click: Ghost privacy toggle)
│   │   ├── Interactive Map Location Picker: onMapLocationPickedCallback
│   │   ├── Target Avatar Context Menu: OptionItem "🦋 Butterfly Point (Rendezvous & Proximity)"
│   │   ├── Overlay Lifecycle: focusOnButterflyPoint, restoreActiveButterflyPointOverlay, clearButterflyOverlay
│   │   └── Broadcast Observer: handleButterflyPointMasterDataUpdate
│   ├── 📄 ButterflyPointDialog.java (Lines 1-1323)
│   │   ├── Dynamic Dialog Management: show, showForTarget, refreshIfOpen
│   │   ├── Action Sheets: showNoActivePointsActionSheet, showActivePointsSheet
│   │   ├── Expandable Card View: createExpandablePointCard (RAG telemetry badges, ETA, distance, participant actions)
│   │   ├── Studio Mode: showCreateButterflyPointStudio (Location, Date/Time picker, 7-day availability ceiling, multi-device picker)
│   │   ├── Dispatch Execution: executeDispatchPoint
│   │   └── Operator HITL Controls: showExtendRendezvousDialog (+15m, +30m, +1h), showInviteMorePeersDialog
│   ├── 📄 UiArchitect.java
│   │   └── OptionItem, dialog card styling, frosted glass surface themes
│   └── 📄 IconTable.java
│       └── Small icon factories, avatar badges, marker asset bindings
│
├── ⚙️ CONTEXT 2: Core Domain & Spatial Mathematics
│   ├── 📄 ButterflyPointEngine.kt (Lines 1-1190)
│   │   ├── Mathematics: calculateHaversineDistance, calculateDistance, formatDistance, calculateEtaMinutes
│   │   ├── Status Engine: evaluateDeviceStatus (15m deadband hysteresis)
│   │   ├── Vector Mesh: renderButterflyOverlay (Azure center marker, emerald perimeter circle, cyan/amber/red vector lines)
│   │   ├── Bounds Computation: calculateMaxRadiusBounds (Dynamic camera framing encompassing point + all devices)
│   │   ├── Cloud Dispatch: dispatchButterflyPoint (SQLite + Firebase RTDB multi-node publish)
│   │   ├── Peer Response: respondToInvitation, submitPeerResponse
│   │   ├── Telemetry Evaluator: evaluateButterflyRag (Green/Amber/Red/Grey state machine)
│   │   ├── HITL Actions: nudgeParticipant, extendRendezvousTime, inviteAdditionalDevices, revokeDeviceInvitation, reInviteDevice
│   │   └── Real-Time Stream Processor: processLocalLocationUpdate (Location fix hook from TrackingService)
│   ├── 📄 SpatialMathUtil.kt (computeDistanceBetween, zero-allocation Haversine calculations)
│   └── 📄 DeviceRagEngine.kt (Enum RagStatus: GREEN, AMBER, RED, GREY)
│
├── 🗄️ CONTEXT 3: Storage & 3-Tier Database Persistence
│   ├── 📄 DatabaseHelper.kt (Lines 25, 588-589, 1023-1079, 2222-2228, 4527-4840)
│   │   ├── Schema Initialization & Migrations: V82 Master/Sub-Master migration, V83 Telemetry stream migration
│   │   ├── Data Access Objects: getActiveButterflyPoints, getButterflyPoint, getButterflyDevicesForPoint
│   │   ├── Schema Alignment: ensureButterflyDeviceMapColumns
│   │   └── Lifecycle Mutators: archiveButterflyPoint, extendButterflyPointTime
│   ├── 📄 ButterflyPointTable.kt (Tier 1: Master Entity)
│   │   ├── Table: butterfly_point
│   │   ├── Schema: point_id (PK), creator_device_id, creator_identity_id, point_name, address, lat, lng, geohash, rendezvous_timestamp, expiry_timestamp, arrival_radius_meters, notes, icon_type, is_active, is_archived, created_at, updated_at
│   │   └── Indexes: idx_butterfly_active (is_active, is_archived)
│   ├── 📄 ButterflyPointDeviceMapTable.kt (Tier 2: Sub-Master / Fact Roster)
│   │   ├── Table: butterfly_point_device_map
│   │   ├── Schema: id (PK AUTOINCREMENT), point_id (FK), device_id (FK), identity_id, status, rejection_reason, distance_to_point_meters, eta_seconds, last_telemetry_ts, arrival_timestamp, battery_pct, is_sos, rag_status, rag_reason, last_lat, last_lng, invited_by, created_at, updated_at
│   │   ├── Constraints: UNIQUE(point_id, device_id), FOREIGN KEY ON DELETE CASCADE
│   │   ├── Indexes: idx_butterfly_dev_status (point_id, status)
│   │   └── Status Invariants: PENDING, ACCEPTED, REJECTED, ON_MOVE, REACHED, TIMED_OUT, REVOKED
│   ├── 📄 ButterflyPointTelemetryTable.kt (Tier 3: Transactional Telemetry Stream)
│   │   ├── Table: butterfly_point_telemetry
│   │   ├── Schema: id (PK AUTOINCREMENT), point_id, device_id, latitude, longitude, speed_kmh, distance_meters, eta_seconds, battery_pct, is_sos, rag_status, rag_reason, timestamp
│   │   ├── Indexes: idx_bpt_point_dev (point_id, device_id, timestamp)
│   │   └── Housekeeping: logTelemetry, purgeExpiredTelemetry (7-day max retention window)
│   ├── 📄 ChangeLogTable.kt (Audit logging Category: Safehouse, Pairing_Devices)
│   └── 📄 AlertHistoryTable.kt (Audit logging AlertType: BUTTERFLY_RENDEZVOUS, BUTTERFLY_RESPONSE)
│
├── ☁️ CONTEXT 4: Cloud Real-Time Synchronization & Ingress
│   ├── 📄 Firebase RTDB Paths (database.rules.json Lines 86-90, 188-192)
│   │   ├── /butterfly_points/{pointId} (Global SSOT node)
│   │   ├── /butterfly_points/{pointId}/devices/{deviceId} (Participation roster)
│   │   ├── /butterfly_points/{pointId}/telemetry/{deviceId} (Latest telemetry broadcast)
│   │   └── /devices/{deviceId}/butterfly_invitations/{pointId} (Personal invitation inbox)
│   ├── 📄 TrackingService.kt (Lines 36-37, 766-979, 1041-1043, 1193-1195, 1510-1517)
│   │   ├── ChildEventListener on /devices/{localAgentId}/butterfly_invitations
│   │   │   ├── onChildAdded: Ingress validation, cold boot timeout guard, SQLite sync, notification dispatch
│   │   │   ├── onChildChanged: Time extension sync, archival/deactivation sync, UI notification
│   │   │   └── onChildRemoved: Local SQLite archival, peer sync detachment, overlay clear
│   │   ├── Location Hook: Calls ButterflyPointEngine.processLocalLocationUpdate on FusedLocation GPS fix
│   │   └── Lifecycle Teardown: stopAllActivePointPeerSyncs
│   ├── 📄 FCMService.kt (Lines 207-230)
│   │   └── RemoteMessage Handler: BUTTERFLY_INVITATION, BUTTERFLY_RESPONSE
│   ├── 📄 FirebaseComm.kt (Forensic timestamps, sanitizeAgentId, safeLong, getDeviceRef)
│   └── 📄 MasterDataSyncSentinel.kt (notifyMasterDataChanged with category BUTTERFLY_POINT)
│
├── 🚨 CONTEXT 5: Alert, Notification & Operator Intervention
│   ├── 📄 AlertManager.kt (Lines 368-651)
│   │   ├── Channel: butterfly_point_invitations (IMPORTANCE_HIGH)
│   │   ├── showButterflyInvitationNotification (Interactive Accept/Reject PendingIntents)
│   │   ├── showButterflyArrivalNotification (Peer arrival heads-up alert)
│   │   ├── showButterflyNudgeNotification (Organizer poke alert)
│   │   ├── showButterflyTimeExtensionNotification (Schedule change alert)
│   │   └── showButterflyQuorumNotification (100% assembly alert)
│   └── 📄 ButterflyPointActionReceiver.kt (Lines 1-79)
│       ├── BroadcastReceiver: ACTION_ACCEPT, ACTION_REJECT
│       ├── Quick Notification Response: Directly updates Sub-Master DB & RTDB without opening full UI
│       └── Immediate Feedback: Triggers processLocalLocationUpdate upon acceptance
│
└── 📋 CONTEXT 6: System Configuration, Registry & Quality Assurance
    ├── 📄 RegistryManager.kt (Lines 240-262, 580-689)
    │   ├── butterfly_arrival_radius_meters (Default: 50.0m)
    │   ├── butterfly_expiry_hours (Default: 4h)
    │   ├── butterfly_hysteresis_meters (Default: 15.0m)
    │   ├── butterfly_max_distance_km (Default: 500.0km)
    │   ├── butterfly_rag_amber_multiplier (Default: 2.0x)
    │   ├── butterfly_rag_red_multiplier (Default: 3.0x)
    │   ├── butterfly_invitation_timeout_hours (Default: 24h)
    │   ├── butterfly_overdue_grace_minutes (Default: 120m)
    │   ├── butterfly_foreground_sync_interval_sec (Default: 4s)
    │   └── butterfly_max_availability_days (Default: 7 days)
    ├── 📄 SettingsActivity.java (Admin configuration UI for dynamic registry overrides)
    └── 📄 EnterpriseUnitTestSuite.kt (Lines 326-507, 1036-1100, 1230-1305)
```

---

## 3. Comprehensive 3-Tier Database Schema & SQLite vs. Firebase RTDB Mapping

```
========================================================================================================================
TIER 1: MASTER ENTITY (Anchor Point SSOT)
------------------------------------------------------------------------------------------------------------------------
SQLite Table: butterfly_point                        Firebase RTDB Node: /butterfly_points/{pointId}
Field Name                 Type       Constraints    Key in RTDB Object       Type in Cloud     Sync Invariant
------------------------------------------------------------------------------------------------------------------------
point_id                   TEXT       PRIMARY KEY    point_id                 String            Bidirectional Match
creator_device_id          TEXT       NOT NULL       creator_device_id        String            Bidirectional Match
creator_identity_id        INTEGER    FK(users.id)   [OMITTED]                -                 Local SQLite Only
point_name                 TEXT       NOT NULL       point_name               String            Bidirectional Match
address                    TEXT       NULLABLE       address                  String            Bidirectional Match
lat                        REAL       NOT NULL       lat                      Double            Bidirectional Match
lng                        REAL       NOT NULL       lng                      Double            Bidirectional Match
geohash                    TEXT       NULLABLE       geohash                  String            Bidirectional Match
rendezvous_timestamp       INTEGER    NOT NULL       rendezvous_timestamp     Long (epoch ms)   Atomic Transaction
expiry_timestamp           INTEGER    NOT NULL       expiry_timestamp         Long (epoch ms)   Atomic Transaction
arrival_radius_meters      REAL       DEFAULT 50.0   arrival_radius_meters    Double            Bidirectional Match
notes                      TEXT       NULLABLE       [OMITTED IN CLOUD WRITE] -                 ⚠️ GAP-01: Serialization Drop
icon_type                  TEXT       DEFAULT 'STD'  [OMITTED IN CLOUD WRITE] -                 ⚠️ GAP-01: Serialization Drop
is_active                  INTEGER    DEFAULT 1      is_active                Boolean           Bidirectional Match
is_archived                INTEGER    DEFAULT 0      is_archived              Boolean           Bidirectional Match
created_at                 TEXT       NOT NULL       created_at               String (ISO-8601) Forensic Timestamp
updated_at                 TEXT       NOT NULL       updated_at               String (ISO-8601) Forensic Timestamp
-                          -          -              creator_name             String            Cloud Augmented
-                          -          -              devices                  Map<String, Obj>  Sub-Master Embedded Node

========================================================================================================================
TIER 2: SUB-MASTER / FACT ROSTER (Connected Devices Participation & State)
------------------------------------------------------------------------------------------------------------------------
SQLite Table: butterfly_point_device_map             Firebase RTDB: /butterfly_points/{pointId}/devices/{deviceId}
                                                     & /devices/{deviceId}/butterfly_invitations/{pointId}
Field Name                 Type       Constraints    Key in RTDB Object       Type in Cloud     Sync Invariant
------------------------------------------------------------------------------------------------------------------------
id                         INTEGER    PK AUTOINCR    [Implicit child key]     String (deviceId) Key Sanitized
point_id                   TEXT       FK CASCADE     [Parent path key]        String (pointId)  Path Scoped
device_id                  TEXT       FK CASCADE     device_id                String            Sanitized Node Key
identity_id                INTEGER    NULLABLE       [OMITTED]                -                 Local SQLite Only
status                     TEXT       DEFAULT PEND   status                   String            ACCEPTED, REJECTED, ON_MOVE,
                                                                                                REACHED, TIMED_OUT, REVOKED
rejection_reason           TEXT       NULLABLE       rejection_reason         String            Synced on REJECTED
distance_to_point_meters   REAL       DEFAULT 0.0    distance_meters          Double            ⚠️ GAP-09: Column Name Mismatch
eta_seconds                INTEGER    DEFAULT 0      eta_seconds              Int               Synced on GPS Fix
last_telemetry_ts          INTEGER    DEFAULT 0      last_updated             Long (epoch ms)   ⚠️ GAP-09: Field Name Mismatch
arrival_timestamp          INTEGER    DEFAULT 0      arrival_timestamp        Long (epoch ms)   Latched on REACHED
battery_pct                INTEGER    DEFAULT -1     battery_pct              Int (-1 to 100)   Device Hardware Telemetry
is_sos                     INTEGER    DEFAULT 0      is_sos                   Boolean           ⚠️ GAP-09: Type Mismatch (Int vs Bool)
rag_status                 TEXT       DEFAULT 'GREY' rag_status               String            GREEN, AMBER, RED, GREY
rag_reason                 TEXT       NULLABLE       rag_reason               String            Context-Aware RAG Reason
last_lat                   REAL       DEFAULT 0.0    last_lat                 Double            Live Coordinate Fix
last_lng                   REAL       DEFAULT 0.0    last_lng                 Double            Live Coordinate Fix
invited_by                 TEXT       NULLABLE       invited_by               String            Mesh Inviter Node ID
created_at                 TEXT       NOT NULL       [OMITTED IN DEVICE OBJ]  -                 Local SQLite Only
updated_at                 TEXT       NOT NULL       last_updated             Long (epoch ms)   Synced on State Transition

========================================================================================================================
TIER 3: TRANSACTIONAL TELEMETRY STREAM (Real-Time Proximity Breadcrumbs & Velocities)
------------------------------------------------------------------------------------------------------------------------
SQLite Table: butterfly_point_telemetry              Firebase RTDB: /butterfly_points/{pointId}/telemetry/{deviceId}
Field Name                 Type       Constraints    Key in RTDB Object       Type in Cloud     Sync Invariant
------------------------------------------------------------------------------------------------------------------------
id                         INTEGER    PK AUTOINCR    [N/A]                    -                 Local SQLite Sequence
point_id                   TEXT       NOT NULL       [Parent path key]        String (pointId)  Path Scoped
device_id                  TEXT       NOT NULL       device_id                String            Sanitized Node Key
latitude                   REAL       DEFAULT 0.0    last_lat                 Double            High-Frequency GPS Fix
longitude                  REAL       DEFAULT 0.0    last_lng                 Double            High-Frequency GPS Fix
speed_kmh                  REAL       DEFAULT 0.0    speed_kmh                Float             Dynamic Velocity
distance_meters            REAL       DEFAULT 0.0    distance_meters          Double            Haversine Distance
eta_seconds                INTEGER    DEFAULT 0      eta_seconds              Int               Dynamic ETA
battery_pct                INTEGER    DEFAULT -1     battery_pct              Int               Hardware Power Gauge
is_sos                     INTEGER    DEFAULT 0      is_sos                   Boolean           Emergency Distress Flag
rag_status                 TEXT       DEFAULT 'GREEN' rag_status              String            RAG State Badge
rag_reason                 TEXT       NULLABLE       rag_reason               String            RAG Rationale
timestamp                  INTEGER    NOT NULL       last_updated             Long (epoch ms)   Fix Timestamp
------------------------------------------------------------------------------------------------------------------------
Storage Model:             Chronological APPEND      Storage Model:           SINGLE OVERWRITE  ⚠️ GAP-02: SQLite stores
                           (insert per GPS fix)                               (setValue latest)  trail; RTDB overwrites.
========================================================================================================================
```

---

## 4. Reverse-Engineering Audit: 10 Architectural Gaps & Vulnerabilities (GAP-01 to GAP-10)

### GAP-01: Field Omission in Cloud Serialization (`notes` & `icon_type`)
* **File Location**: `app/src/main/java/com/jdroidx/jfindme/engine/map/ButterflyPointEngine.kt` (Lines 372–388) and `TrackingService.kt` (Lines 770–836).
* **Observed Code**:
  ```kotlin
  val cloudMap = hashMapOf<String, Any>(
      "point_id" to point.pointId,
      "creator_device_id" to point.creatorDeviceId,
      "creator_name" to point.creatorName,
      "point_name" to point.pointName,
      "address" to point.address,
      "lat" to point.lat,
      "lng" to point.lng,
      "geohash" to point.geohash,
      "rendezvous_timestamp" to point.rendezvousTimestamp,
      "expiry_timestamp" to point.expiryTimestamp,
      "arrival_radius_meters" to point.arrivalRadiusMeters,
      "is_active" to true,
      "is_archived" to false,
      "created_at" to FirebaseComm.getForensicTimestamp(),
      "updated_at" to FirebaseComm.getForensicTimestamp()
  )
  ```
* **Failure Condition**: `notes` and `icon_type` are defined in `ButterflyPointTable.kt` and populated in `ButterflyPointModel`, but omitted from `cloudMap`. Furthermore, `TrackingService.kt` does not read `notes` from the RTDB snapshot.
* **Operational Impact**: When Device A dispatches rendezvous notes (e.g., "Meet at Gate 3"), Device B receives null/empty notes. Device B's UI renders blank notes.
* **Severity**: MEDIUM (Data Loss).

---

### GAP-02: High-Velocity Telemetry Overwrite vs. Append Disparity (Tier 3)
* **File Location**: `ButterflyPointEngine.kt` (Line 1173) vs `ButterflyPointTelemetryTable.kt` (Line 84).
* **Observed Code**:
  - `ButterflyPointEngine.kt:1173`:
    `rtdb.child("butterfly_points").child(pt.pointId).child("telemetry").child(cleanMyAgentId).setValue(updateMap)`
  - `ButterflyPointTelemetryTable.kt:84`:
    `return db.insert(TABLE_NAME, null, values)`
* **Failure Condition**: In local SQLite, Tier 3 appends a complete chronological breadcrumb history. In Firebase RTDB, `.setValue()` overwrites the single node `/telemetry/{deviceId}` with only the latest fix.
* **Operational Impact**: Remote organizer cannot reconstruct breadcrumb route trails from RTDB without falling back to global device history.
* **Severity**: MEDIUM (Historical Audit Gap).

---

### GAP-03: In-Memory Concurrency Race Condition on `activeButterflyPoint.devices`
* **File Location**: `ButterflyPointModel.kt` (Line 30), `ButterflyPointEngine.kt` (Lines 607–618), `MapActivity.java` (Lines 3968–3975).
* **Observed Code**:
  - `ButterflyPointModel.kt:30`: `var devices: MutableMap<String, ButterflyDeviceEntry> = mutableMapOf()`
  - `ButterflyPointEngine.kt:607` mutates entries on a background Firebase listener thread:
    `activeButterflyPoint?.devices?.get(devId)?.let { dEntry -> dEntry.status = status; ... }`
  - `MapActivity.java:3968` iterates on the Android Main UI Thread during camera framing:
    `for (Map.Entry<String, ButterflyDeviceEntry> entry : pt.getDevices().entrySet()) { ... }`
* **Failure Condition**: `mutableMapOf()` instantiates a non-thread-safe `LinkedHashMap`. High-frequency peer status arrivals while the UI renders map overlays trigger `ConcurrentModificationException`.
* **Operational Impact**: Intermittent fatal application crash (`Fatal Exception: java.util.ConcurrentModificationException`) on map rendering.
* **Severity**: HIGH (Crash Hazard).

---

### GAP-04: Whole-Object Cloud Write Race Condition During Point Dispatch
* **File Location**: `ButterflyPointEngine.kt` (Line 420).
* **Observed Code**:
  `rtdb.child("butterfly_points").child(point.pointId).setValue(cloudMap)`
* **Failure Condition**: `setValue(cloudMap)` writes the entire point tree including the `devices` sub-map. If a peer accepts the invitation before the creator's initial write callback settles, writing the root object clobbers the peer's `ACCEPTED` state back to `PENDING`.
* **Operational Impact**: Peer response dropped; peer badge remains "Awaiting Response" even after accepting.
* **Severity**: MEDIUM (State Inconsistency).

---

### GAP-05: Permissive Security Rules & Missing ACL Granularity in Firebase RTDB
* **File Location**: `database.rules.json` (Lines 188–192).
* **Observed Code**:
  ```json
  "butterfly_points": {
    ".read": "auth != null",
    ".write": "auth != null",
    ".indexOn": ["creator_device_id", "is_active", "is_archived", "rendezvous_timestamp"]
  }
  ```
* **Failure Condition**: Any authenticated user can overwrite, corrupt, or delete any point under `/butterfly_points/` because the rule lacks child-level ACL checks.
* **Operational Impact**: Malicious or misconfigured client could delete active rendezvous points or overwrite peer telemetry.
* **Severity**: HIGH (Security & Data Integrity).

---

### GAP-06: Decoupled Push Notification Persistence Gap (FCM vs TrackingService)
* **File Location**: `FCMService.kt` (Lines 207–218).
* **Observed Code**:
  ```kotlin
  "BUTTERFLY_INVITATION" -> {
      AlertManager.showGeofenceNotification(applicationContext, "🦋 Butterfly Point: $body")
      val refreshIntent = Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply { ... }
      applicationContext.sendBroadcast(refreshIntent)
  }
  ```
* **Failure Condition**: `FCMService` displays a toast and broadcasts an intent, but does NOT fetch the point from RTDB or insert it into SQLite! If `TrackingService` is stopped or killed by Android battery optimization (Doze mode), the point is never persisted locally until `TrackingService` cold-starts.
* **Operational Impact**: User taps push notification; app opens to empty rendezvous dashboard.
* **Severity**: HIGH (Notification Ingress Hole).

---

### GAP-07: Unbounded Real-Time Listener Accumulation (Memory & Socket Leak)
* **File Location**: `ButterflyPointEngine.kt` (Lines 556, 674) and `clearButterflyOverlay()` (Lines 239–260).
* **Observed Code**:
  `activePointListeners[pointId] = listener`
  `clearButterflyOverlay()` resets markers and polylines, but does NOT call `stopActivePointPeerSync(pointId)`.
* **Failure Condition**: If an operator views multiple active points throughout the day, a new `ValueEventListener` is retained for each point in `activePointListeners`. `stopAllActivePointPeerSyncs()` is ONLY called in `TrackingService.onDestroy()`.
* **Operational Impact**: Unbounded memory growth, continuous socket listener processing, battery drain.
* **Severity**: MEDIUM (Resource Leak).

---

### GAP-08: Time Extension Partial Failure / Split-Brain Timestamps
* **File Location**: `DatabaseHelper.kt` (Lines 4784–4836).
* **Observed Code**:
  `extendButterflyPointTime` executes a `for` loop over `val devices = getButterflyDevicesForPoint(pointId)` and runs individual transactions on each peer inbox `/devices/{cleanId}/butterfly_invitations/{pointId}`.
* **Failure Condition**: If network drops mid-loop, some participant inboxes receive the extended timestamp while others do not.
* **Operational Impact**: Split-brain rendezvous schedule across participants; un-updated devices trigger false overdue alarms.
* **Severity**: MEDIUM (Distributed Consensus Failure).

---

### GAP-09: Column Name and Type Inconsistencies Between SQLite and RTDB
* **File Location**: `ButterflyPointDeviceMapTable.kt`, `ButterflyPointTelemetryTable.kt`, and `ButterflyPointEngine.kt`.
* **Inconsistencies Identified**:
  - Distance: SQLite Tier 2 uses `distance_to_point_meters`, while RTDB uses `distance_meters`.
  - SOS Flag: SQLite stores `is_sos` as INTEGER (`0` or `1`), while RTDB stores `is_sos` as Boolean (`true` or `false`).
  - Timestamp: SQLite uses `last_telemetry_ts`, while RTDB uses `last_updated`.
* **Operational Impact**: Requires fragile, ad-hoc manual type casts across deserialization code (`safeLong`, `as? Boolean`, `as? Double`), increasing regression risk.
* **Severity**: LOW (Schema Friction).

---

### GAP-10: Ingress Availability Ceiling Precondition Enforcement Gap
* **File Location**: `ButterflyPointEngine.kt` (Lines 265–438).
* **Observed Code**:
  `ButterflyPointDialog.java` checks 7-day availability in the UI, but `ButterflyPointEngine.dispatchButterflyPoint` does NOT validate the 7-day ceiling programmatically before writing to SQLite and RTDB.
* **Failure Condition**: An external mesh peer or API client could inject a point scheduled 30 days in the future, bypassing UI constraints.
* **Operational Impact**: Stale future points persist in the database until cleaned up by the 7-day query filter.
* **Severity**: LOW (Precondition Hygiene).

---

## 5. Closed Feedback Loop & Engineering Resolution Matrix

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                         CLOSED FEEDBACK LOOP SPECIFICATION MATRIX                                │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

| Gap ID | Identified Vulnerability | Root Cause Location | Closed Feedback Loop Architecture & Engineering Resolution |
|:---:|:---|:---|:---|
| **GAP-01** | `notes` & `icon_type` dropped | `ButterflyPointEngine.kt:372` | Include `"notes" to (point.notes ?: "")` and `"icon_type" to (point.iconType ?: "STD")` in `cloudMap`. In `TrackingService.kt:804`, parse both fields from the snapshot into SQLite `butterfly_point`. |
| **GAP-02** | Telemetry overwrite vs append | `ButterflyPointEngine.kt:1173` | Maintain dual cloud mode: write real-time latest fix to `/telemetry/{devId}` for $< 250\text{ms}$ HUD tracking, and write breadcrumb history to `/telemetry_history/{devId}/{timestamp}` on demand or post-mission. |
| **GAP-03** | Concurrency race on `devices` | `ButterflyPointModel.kt:30` | Refactor `ButterflyPointModel.devices` to `java.util.concurrent.ConcurrentHashMap<String, ButterflyDeviceEntry>` or wrap iterations in `synchronized(point.devices)` block. |
| **GAP-04** | Whole-object cloud write clobber | `ButterflyPointEngine.kt:420` | Refactor `dispatchButterflyPoint` to use Firebase fine-grained multi-path update (`updateChildren`), isolating root metadata from peer roster nodes. |
| **GAP-05** | Permissive RTDB security rules | `database.rules.json:188` | Update rules: allow creator write on root (`data.child('creator_device_id').val() == auth.uid`), scoped write on `/devices/{deviceId}` where `deviceId == auth.uid`, and scoped write on `/telemetry/{deviceId}` where `deviceId == auth.uid`. |
| **GAP-06** | Push notification decoupled | `FCMService.kt:207` | In `FCMService.onMessageReceived`: if `TrackingService` is not running, enqueue a one-shot Coroutine / WorkManager task to fetch `/butterfly_points/{pointId}` from RTDB and persist to SQLite immediately. |
| **GAP-07** | Listener accumulation leak | `ButterflyPointEngine.kt:556` | In `clearButterflyOverlay()` and `MapActivity.onPause/onDestroy`: invoke `stopActivePointPeerSync(activePointId)` to detach listeners when user leaves or switches active points. |
| **GAP-08** | Split-brain time extension | `DatabaseHelper.kt:4784` | Refactor `extendButterflyPointTime` to execute a single atomic multi-path RTDB update (`mapOf("/butterfly_points/$pId/rendezvous_timestamp" to newTs, ...)`) eliminating partial loop failures. |
| **GAP-09** | Column & type inconsistencies | `ButterflyPointEngine.kt` | Introduce an OOPS `ButterflyPointMapper` class to standardize field mappings and type conversions between SQLite columns and RTDB keys. |
| **GAP-10** | 7-day ceiling precondition | `ButterflyPointEngine.kt:265` | Add strict precondition check in `ButterflyPointEngine.dispatchButterflyPoint`: `require(point.rendezvousTimestamp <= System.currentTimeMillis() + (7 * 86400000L))` throwing `IllegalArgumentException` if violated. |

---

## 6. Updated Tactical RAG (Red / Amber / Green / Grey) Health Monitoring Status

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                   TACTICAL RAG ENGINE OPERATIONAL EVALUATION MATRIX                              │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

| Operational RAG State | Hex Color | Map Vector Line Color | State Evaluator Trigger Logic | Real-Time Operational Meaning |
|:---:|:---:|:---:|:---|:---|
| **🔴 RED** | `#EF4444` | Red (`#FF1744`) | `isSosActive == true` \|\| `status == REJECTED` \|\| `status == TIMED_OUT` \|\| `deltaTs > 24h` \|\| `batteryPct <= 5%` \|\| `now > rTs + 15m` | Participant in emergency distress, declined invitation, or severely overdue. |
| **🟡 AMBER** | `#F59E0B` | Amber (`#FFB300`) | `status == PENDING && now > rTs - 45m` \|\| `batteryPct in 6..20` \|\| `deltaTs > 30m` \|\| `arrivalEpoch > rTs + 5m` \|\| `distance > 500km` \|\| `lat==0 && lng==0` \|\| `stationary` | Participant en route but at risk of delay, battery exhaustion, or missing confirmation. |
| **🟢 GREEN** | `#10B981` | Cyan (`#00E5FF`) | `status == REACHED` \|\| (`status in (ACCEPTED, ON_MOVE)` && `battery > 20%` && `fresh telemetry` && `timely ETA`) | Participant on track or arrived at rendezvous perimeter. |
| **⚪ GREY** | `#6B7280` | Hidden | `status == REVOKED` \|\| (`status == PENDING` && `now <= rTs - 45m`) | Invitation issued well in advance; awaiting participant decision. |

---

## 7. Verification & Attestation Commands

### Dual-Module Parity Audit:
```powershell
$files = @(
    "data\local\tables\ButterflyPointTable.kt",
    "data\local\tables\ButterflyPointDeviceMapTable.kt",
    "data\local\tables\ButterflyPointTelemetryTable.kt",
    "engine\map\ButterflyPointEngine.kt",
    "ui\map\ButterflyPointDialog.java",
    "data\model\ButterflyPointModel.kt"
)
foreach ($f in $files) {
    $h1 = (Get-FileHash "app\src\main\java\com\jdroidx\jfindme\$f").Hash
    $h2 = (Get-FileHash "jButterFly\src\main\java\com\jdroidx\jfindme\$f").Hash
    if ($h1 -ne $h2) { throw "Parity violation in $f" }
}
Write-Output "100% SHA-256 Parity Verified Across All Subsystem Components"
```

### Canonical Unit Test Suite Execution:
```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.jdroidx.jfindme.EnterpriseUnitTestSuite"
```
- Command result: **BUILD SUCCESSFUL** (33 actionable tasks, 0 failures).
- Invalidation Condition: Any failure in `testButterflyPointDryRunRiddhiToPushpa` or `testButterfly3TierArchitectureDataExchangeGating`.
