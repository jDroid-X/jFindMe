-- made by jDroid-X--
# SYNCHRONIZATION ROOT CAUSE ANALYSIS (RCA) AND REVERSE ENGINEERING ANALYSIS (REA)
## Fleet Record Consistency Between Local SQLite Database (`2177e4a0`) and Google Firebase Realtime Database

> **Document Version**: `2.0.0-ENTERPRISE-REA`  
> **Operational References**: `J_DROID_X_BRIEFING.md` | `GEMINI.md` | `PROJECT.md`  
> **Physical Hardware Host**: Device `2177e4a0` (Xiaomi Redmi Note 7 Pro `violet`, Android 10, SDK 29)  
> **Local Database SSOT**: Native `SQLiteOpenHelper` (`JFindMe_CitizenData.db` v84, 33.4 MB)  
> **Remote Database SSOT**: Google Firebase Realtime Database (`https://jfindme-3b15c-default-rtdb.asia-southeast1.firebasedatabase.app`)  
> **Primary Mesh Identity**: Group Circle `GRP_2T929153` | Host Node: `2T929153` (REDMI N7)  
> **Safety Invariants**: 🛡️ **Gate 1 Applied (Zero Telemetry Leakage for Child Node `4379A761` / Riddhi)** | **Prime Directives 1–5 Enforced**  

---

## 1. Executive Summary & Forensic Scope

### 1.1 Context & Mission Objective
During Milestone 1 fleet extraction across physical hardware host `2177e4a0` (Master Node `2T929153`) and the remote Google Firebase Realtime Database (RTDB), comprehensive state mapping identified critical synchronization discrepancies between the device-bound local SQLite database (`JFindMe_CitizenData.db`, schema version 84) and the cloud distributed state. While the primary positioning and telemetry ingress pipelines function in real-time, specific subsystem boundaries exhibit state freezing, type mismatches, race conditions, and unhandled network edge cases.

This document delivers an exhaustive, publication-grade Reverse Engineering Analysis (REA) and Root Cause Analysis (RCA). Every failure mechanism is traced from empirical database evidence directly to the exact file paths, class definitions, function signatures, and line numbers within the active `:jButterFly` canonical application module.

### 1.2 Fleet Node Roster & Investigated Entities
The analysis covers all six known fleet nodes participating in the mesh network:

| Canonical ID | Alias / Model | Hardware & OS Profile | Master Circle Role | Primary Synchronization Defect |
| :--- | :--- | :--- | :--- | :--- |
| **`2T929153`** | **REDMI N7** | Xiaomi Redmi Note 7 Pro (`violet`, Android 10) | ADMIN / Parent | Session state frozen at `OFFLINE` in SQLite; Orphaned Auth UID row in `device_vitals`. |
| **`2C811042`** | **REDMI 5PRO** | Xiaomi Redmi Note 5 Pro (`whyred`, Android 9) | Friend / Active Tracker | Vitals battery locked at `100%` in SQLite while cloud is `17%`; Pairings `TIMED_OUT`. |
| **`F8CAFC67`** | **PUSHPA** | Xiaomi Redmi Note 10 Pro (`sweet`, Android 13) | Spouse / Co-Guardian | Vitals battery locked at `100%` in SQLite while cloud is `47%`; Pairings `TIMED_OUT`. |
| **`2D113789`** | **INNOVANT** | Android Field Unit | Member / Colleague | Vitals battery locked at `100%` in SQLite; Heartbeat aged 4.10h evaluated inconsistently. |
| **`4379A761`** | **RIDDHI** | Android Handset (Protected Child Node) | Child / Daughter | Intra-cloud battery split-brain (`-1` vs `9%`); SQLite vitals locked at `100%`. |
| **`A7D5968D`** | **Tombstone** | Decommissioned Test Node | N/A (Change_Log only) | Legacy pairing stub triggers 12 phantom `(0.0, 0.0)` location ingestions into `sys_log`. |

---

## 2. Architectural Data Pipeline & State Synchronization Lifecycle

### 2.1 Dual-Engine Topology (Local SQLite SSOT vs Cloud RTDB SSOT)
jFindMe utilizes an OOPS-based dual-engine persistence architecture designed for mission-critical offline resilience:
1. **Local Persistent SSOT**: Managed by `DatabaseHelper.kt` (v84), persisting 24 physical tables and 13 compatibility views into `/data/data/com.jdroidx.jfindme/databases/JFindMe_CitizenData.db`. Local SQLite provides zero-latency cold-boot seeding for UI surfaces, ensuring immediate operational capability in GPS-denied or disconnected environments.
2. **Cloud Distributed SSOT**: Google Firebase Realtime Database acts as the multi-node synchronization backbone, propagating sub-second live GPS beacons, relationship contracts, peer vitals, and rendezvous points across the fleet.

```
+----------------------------------------------------------------------------------------------------+
|                                    PHYSICAL HARDWARE NODE (2177e4a0)                               |
|                                                                                                    |
|  +--------------------+         +------------------------+         +----------------------------+  |
|  |   GNSS Hardware    |         | Battery Fuel Gauge /   |         | Cellular / Wi-Fi Baseband  |  |
|  | (GPS / GLONASS)    |         | Power Manager Subsys   |         | Network Telephony Driver   |  |
|  +---------+----------+         +-----------+------------+         +--------------+-------------+  |
|            |                                |                                     |                |
|            +--------------------------------+-------------------------------------+                |
|                                             |                                                      |
|                                             v                                                      |
|                             [ TrackingService.kt Core Loop ]                                       |
|                                             |                                                      |
|                  +--------------------------+--------------------------+                           |
|                  | (Kinematic Delta >= 15m)                            | (Stationary < 15m)        |
|                  v                                                     v                           |
|      [ TelemetryFailoverRouter ]                         [ FirebaseComm.syncPresencePulse ]        |
|      +-- N1: Direct Cloud (Firebase)                               (28-Byte Lean Heartbeat)        |
|      +-- N2: Wi-Fi Mesh Handover                                       |                           |
|      +-- N3: SMS Beacon Failover                                       v                           |
|      +-- N4: SQLite FIFO Vault                            [ RTDB: /devices/{id}/device/live ]      |
|                  |                                        [ RTDB: .info/connected Presence  ]      |
|                  v                                                                                 |
|      [ OfflineSyncManager.syncLocation ]                                                           |
|                  |                                                                                 |
|         +--------+---------------------------------------+                                         |
|         | Write Local SSOT                               | Network Available?                      |
|         v                                                v                                         |
|   +----------------------------+             +---------------------------------------+             |
|   | SQLite: location_event     |             | YES: FirebaseComm.syncAnalyticalEvent |             |
|   | SQLite: pending_sync_queue |             | NO:  Enqueue In-Memory & SQLite Vault |             |
|   +----------------------------+             +---------------------------------------+             |
|                                                                                                    |
+----------------------------------------------------------------------------------------------------+
                                      |                       ^
                      Real-time Sockets / REST API            | Inbound Peer Data & Commands
                                      v                       |
+----------------------------------------------------------------------------------------------------+
|                                  GOOGLE FIREBASE REALTIME DATABASE                                 |
|                                                                                                    |
|  /devices/{agentId}/device/live                /relationships/{groupId}/members                    |
|  /devices/{agentId}/telemetry                  /relationships/{groupId}/devices                    |
|  /devices/{agentId}/profile                    /butterfly_points/{pointId}                         |
|  /devices/{agentId}/Identity                   /pairing_sessions/{sessionId}                       |
+----------------------------------------------------------------------------------------------------+
```

### 2.2 Telemetry Ingress & Failover Routing
The telemetry engine enforces a 4-tier cascading waterfall (`TelemetryFailoverRouter.kt:70-170`):
- **Tier N1 (Direct Cloud)**: Direct JSON payload dispatch to Firebase RTDB `/devices/{agentId}/device/live` via WebSocket. Measures round-trip ACK latency via `TelemetryClosedLoopSentinel.kt`.
- **Tier N2 (Wi-Fi P2P Handover)**: Local Wi-Fi Direct socket dispatch to nearby gateway devices.
- **Tier N3 (SMS Emergency Beacon)**: Compressed Base64 SMS beacon dispatched to guardian MSISDN when cellular IP data is completely unreachable.
- **Tier N4 (SQLite FIFO Vault)**: AES-256 encrypted persistence into SQLite table `location_event` and `pending_sync_queue`.

### 2.3 Master Data Synchronization Sentinel Lifecycle
Master data mutations (user profile updates, safe zone boundaries, circle associations) bypass the streaming telemetry pipeline and route through `MasterDataSyncSentinel.kt:60-140`. The sentinel logs mutations to local table `change_log`, queues offline changes into `sync_queue`, updates the cloud path `/devices/{agentId}/profile`, and fans out invalidation pings to `/devices/{peerId}/inbox/master_sync`.

### 2.4 UI Tri-Surface Data Binding Matrix

| UI Presentation Surface | Cold-Boot Local Seed | Real-time Cloud Socket Listeners | Health & Presence Evaluation Engine |
| :--- | :--- | :--- | :--- |
| **Live Map Canvas**<br>(`LiveMapDashboardActivity`<br>`MapActivity`) | `LocationRepository.seedInitialLocationsFromLocalDb()` queries `DatabaseHelper.getOfflineRoster()`. | `LocationRepository.updateTargetListeners()` attaches `ValueEventListener` to `/devices/{id}/device/live` and `/devices/{id}/profile`. | Kinematic vectors; avatar spiderwebbing via `MultiMarkerEngine.kt`; SOS priority override. |
| **Network Gateway**<br>(`NetworkGatewayActivity`) | Tab 0 queries local SQLite `getOfflineRoster()` via `GroupViewModel.refreshLocalRoster()`. | Tab 1 binds to `/pairing_requests` and `/pairing_sessions`. Per-card listeners bind to `/devices/{id}/device/live`. | `DeviceRagEngine.evaluate()`; displays route bearer (e.g. `N1_WIFI_FAST`) and pairing status. |
| **Friends & Family Hub**<br>(`FamilyFriendsActivity`) | Zero-latency initial list from `localDb.getOfflineRoster()`. | Binds `ValueEventListener` to `/relationships/{groupId}/members` and `/relationships/{groupId}/devices`. | **Strict Ward Sorting Invariant**: Child node (`4379A761`) is pinned to Position 0 via `isChildOrPrimeTarget`. |

---

## 3. Deep Root Cause Analysis & Reverse Engineering Analysis (Gaps 01 to 11)

### 3.1 GAP-01: Stale Peer Device Vitals Locked at 100% in SQLite
- **Empirical Observation**: In SQLite table `device_vitals` on device `2177e4a0`, all 4 peer devices (`4379A761`, `2C811042`, `2D113789`, `F8CAFC67`) have `battery_level = 100`, `network_type = 'UNKNOWN'`, and `signal_dbm = 0`. In Firebase RTDB, live batteries are: `4379A761` = 9% (`telemetry/battery_pct`), `2C811042` = 17%, `F8CAFC67` = 47%, `2D113789` = -1%.
- **Reverse Engineering Trace**:
  1. In `LocationRepository.kt` lines 304–313:
     ```kotlin
     // 🚀 Telemetry SSOT Backfill: Instantly synchronize DeviceVitalsTable in SQLite
     try {
         val effTs = loc.getLongTimestamp()
         val batt = loc.batteryLevel
         if (effTs > 0L) {
             db.updateDeviceVitalsTelemetry(canonicalStoreId, if (batt > 0) batt else 100, effTs)
             if (resolvedId != canonicalStoreId) {
                 db.updateDeviceVitalsTelemetry(resolvedId, if (batt > 0) batt else 100, effTs)
             }
         }
     } catch (e: Exception) { ... }
     ```
  2. `loc.batteryLevel` is parsed by Firebase RTDB using `UserLocation.kt:38-40`:
     ```kotlin
     @get:com.google.firebase.database.PropertyName("battery")
     @set:com.google.firebase.database.PropertyName("battery")
     var batteryLevel: Int = -1,
     ```
  3. When a remote device is plugged in or transmits battery under `/telemetry/battery_pct` (such as `4379A761` where `/device/live/battery = -1` and `/telemetry/battery_pct = 9`), `loc.batteryLevel` evaluates to `-1`.
  4. Line 309 executes `if (batt > 0) batt else 100`. Because `-1 <= 0`, this evaluates to `100`, forcing `100%` into SQLite `device_vitals`!
  5. The exact same defect is duplicated in `NetworkGatewayActivity.java` line 1765:
     ```java
     db.updateDeviceVitalsTelemetry(finalUid, batt > 0 ? batt : 100, ts, false, null);
     ```
  6. In `FamilyFriendsViewModel.kt` lines 466–469:
     ```kotlin
     val directBattery = safeInt(nodeSnap.child("battery"))
         ?: safeInt(nodeSnap.child("battery_pct"))
         ?: safeInt(telemetrySnap?.child("battery_pct"))
         ?: 100
     ```
     When reading `/relationships/{groupId}/members`, the member node does not contain battery or telemetry children, defaulting directly to `100` and persisting into `device_vitals` via `localDb.upsertDeviceVitals(canonicalAgentId, 100, ...)`.
- **Underlying Failure Mechanism**: Rigid ternary fallback (`batt > 0 ? batt : 100`) coupled with single-key deserialization (`battery` vs `battery_pct`) converts uncalibrated or AC-tethered states (`-1`) into artificial 100% full-battery states in SQLite.
- **File & Line References**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/repository/LocationRepository.kt:307-312`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/ui/group/NetworkGatewayActivity.java:1741-1765`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/ui/group/FamilyFriendsViewModel.kt:466-470, 575-585`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/local/DatabaseHelper.kt:4081`

---

### 3.2 GAP-02: User Identity Session State Frozen at `OFFLINE` in SQLite
- **Empirical Observation**: In SQLite `user_identity`, `st_session_state` is set to `'OFFLINE'` for all 5 devices, including host `2T929153`. In Firebase RTDB, `/devices/2T929153/device/live` reports `is_online: true` and `status: "ONLINE"`.
- **Reverse Engineering Trace**:
  1. In `UserIdentityTable.kt` line 68:
     ```kotlin
     $COL_SESSION_STATE TEXT DEFAULT 'OFFLINE',
     ```
     The column is defined with default value `'OFFLINE'`.
  2. In `FirebaseComm.kt` lines 1907–1965 (`setupPresenceSentinel`), the presence sentinel monitors `.info/connected`:
     ```kotlin
     connectedRef.addValueEventListener(object : ValueEventListener {
         override fun onDataChange(snapshot: DataSnapshot) {
             val connected = snapshot.getValue(Boolean::class.java) ?: false
             if (connected) {
                 liveRef.child("is_online").setValue(true)
                 liveRef.child("status").setValue("ONLINE")
                 // Missing: Never updates SQLite user_identity table locally!
             }
         }
     })
     ```
  3. No method in `UserIdentityTable.kt` or `DatabaseHelper.kt` updates `st_session_state` on connection changes.
  4. In `DatabaseHelper.kt` line 4083:
     ```kotlin
     'LIVE' as st_session_state
     ```
     `DatabaseHelper.getOfflineRoster()` hardcodes `'LIVE'` in the SQL projection, masking the underlying database discrepancy from UI views while leaving the physical table frozen at `'OFFLINE'`.
- **Underlying Failure Mechanism**: One-way presence publication (Android -> RTDB) lacking a local database write-back hook, accompanied by hardcoded projection masks in query helpers.
- **File & Line References**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/local/tables/UserIdentityTable.kt:32, 68`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/remote/FirebaseComm.kt:1907-1965`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/local/DatabaseHelper.kt:4083`

---

### 3.3 GAP-03: Butterfly Point Model Concurrency & `ConcurrentModificationException`
- **Empirical Observation**: Random fatal application crashes on the UI main thread during active Butterfly Point map rendering.
- **Reverse Engineering Trace**:
  1. In `ButterflyPointModel.kt` line 31:
     ```kotlin
     var devices: MutableMap<String, ButterflyDeviceEntry> = ConcurrentHashMap()
     ```
  2. When Firebase RTDB deserializes a `ButterflyPoint` via `snapshot.getValue(ButterflyPoint::class.java)` in `TrackingService.kt:840`, Firebase's internal `CustomClassMapper` creates a standard `java.util.HashMap`, completely replacing the `ConcurrentHashMap` instance.
  3. In `MapActivity.java` line 4128:
     ```java
     for (java.util.Map.Entry<String, com.jdroidx.jfindme.data.model.ButterflyDeviceEntry> entry : pt.getDevices().entrySet()) {
         // UI Main Thread rendering avatar markers and radar HUD
     }
     ```
  4. Concurrently, in `ButterflyPointEngine.kt` lines 573–632 (`startActivePointPeerSync`), incoming Firebase RTDB child events execute on an IO worker thread and modify the map:
     ```kotlin
     activeButterflyPoint?.devices?.get(devId)?.let { dEntry ->
         dEntry.status = status
         dEntry.distanceMeters = dist
     }
     ```
  5. Because the underlying collection was replaced with `java.util.HashMap`, iterating over `entrySet()` on the main thread while a background thread updates entries throws `java.util.ConcurrentModificationException`.
- **Underlying Failure Mechanism**: RTDB POJO deserialization clobbering thread-safe collection types with standard `HashMap`, exposed by direct iteration over mutable collections without defensive snapshot copying.
- **File & Line References**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/model/ButterflyPointModel.kt:31`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/ui/map/MapActivity.java:4128`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/ui/map/ButterflyPointDialog.java:400`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/engine/map/ButterflyPointEngine.kt:619-631`

---

### 3.4 GAP-04: RTDB Whole-Object Overwrite Clobbering Peer Status (`.setValue(cloudMap)`)
- **Empirical Observation**: Peer devices accept an invitation (`STATUS_ACCEPTED`), but the creator device's point updates revert the peer's status back to `STATUS_PENDING`. In cloud RTDB `/butterfly_points`, points periodically disappear or lose participant responses.
- **Reverse Engineering Trace**:
  1. In `ButterflyPointEngine.kt` lines 392–415, the creator constructs `cloudMap` containing all participants initialized with `STATUS_PENDING`:
     ```kotlin
     for (devId in targetDeviceIds) {
         if (devId.equals(creatorDevId, ignoreCase = true)) continue
         val cleanDevId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(devId)
         devicesMap[cleanDevId] = hashMapOf<String, Any>(
             "device_id" to devId,
             "status" to ButterflyPointDeviceMapTable.STATUS_PENDING,
             "distance_meters" to 0.0,
             "last_updated" to System.currentTimeMillis()
         )
     }
     cloudMap["devices"] = devicesMap
     ```
  2. At line 422:
     ```kotlin
     rtdb.child("butterfly_points").child(point.pointId).setValue(cloudMap)
     ```
  3. When a peer receives the push or event, it calls `ButterflyPointEngine.submitPeerResponse` (`ButterflyPointEngine.kt:519`):
     ```kotlin
     rtdb.child("butterfly_points").child(pointId).child("devices").child(deviceId).updateChildren(updates)
     ```
  4. If the creator dispatches an update, re-syncs, or experiences high network latency, line 422's `.setValue(cloudMap)` executes against the entire root of `/butterfly_points/{pointId}`, wiping all child nodes and replacing the peer's `STATUS_ACCEPTED` back with `STATUS_PENDING`!
- **Underlying Failure Mechanism**: Non-atomic whole-tree overwrite (`.setValue()`) clobbering fine-grained atomic child mutations (`.updateChildren()`).
- **File & Line References**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/engine/map/ButterflyPointEngine.kt:408-422, 511-523`

---

### 3.5 GAP-05: Schema Naming & Type Incompatibilities in Butterfly Rendezvous
- **Empirical Observation**: Distance to rendezvous point and SOS status fail to synchronize properly between SQLite and RTDB; deserialization drops values or throws mapping warnings.
- **Reverse Engineering Trace**:
  1. In `ButterflyPointDeviceMapTable.kt` lines 24 and 60:
     ```kotlin
     const val COL_DISTANCE_METERS = "distance_to_point_meters"
     ...
     $COL_DISTANCE_METERS REAL DEFAULT 0.0,
     $COL_IS_SOS INTEGER DEFAULT 0,
     ```
  2. In `ButterflyPointModel.kt` lines 46 and 54:
     ```kotlin
     var distanceMeters: Double = 0.0,
     var isSosActive: Boolean = false,
     ```
  3. In `ButterflyPointEngine.kt` lines 399 and 411:
     ```kotlin
     "distance_meters" to 0.0
     ```
  4. Notice the schema divergence:
     - SQLite column name: `distance_to_point_meters`
     - RTDB JSON key name: `distance_meters`
     - Model property name: `distanceMeters`
     - SQLite SOS type: `INTEGER` (`0` or `1`)
     - RTDB SOS type: `Boolean` (`true` or `false`)
- **Underlying Failure Mechanism**: Heterogeneous naming conventions across SQLite column definitions and Firebase JSON keys without property alias annotations (`@PropertyName`).
- **File & Line References**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/local/tables/ButterflyPointDeviceMapTable.kt:24, 29, 55, 60`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/model/ButterflyPointModel.kt:46, 54`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/engine/map/ButterflyPointEngine.kt:399, 411, 584, 589`

---

### 3.6 GAP-06: Dual-Queue Draining Desynchronization & Silent Auth Stalling
- **Empirical Observation**: On network reconnection after extended offline periods, location pings synchronize immediately, but safe zone, profile, and geofence mutations remain queued in SQLite indefinitely.
- **Reverse Engineering Trace**:
  1. The codebase divides offline storage into two separate queues:
     - `PendingSyncQueueTable.kt` (`pending_sync_queue`): Persists location fixes (`UserLocation`), drained by `OfflineSyncManager.processQueue()`.
     - `SyncQueueTable.kt` (`sync_queue`): Persists master data mutations (`MASTER_PROFILE`, `MASTER_SAFE_ZONE`), drained by `MasterDataSyncSentinel.flushPendingMasterSyncQueue()`.
  2. In `OfflineSyncManager.kt` lines 108–118:
     ```kotlin
     networkDebounceJob = CoroutineScope(Dispatchers.IO).launch {
         delay(3000L)
         if (isNetworkAvailable) {
             processQueue()
             reconcileMetadataWithCloud()
             MasterDataSyncSentinel.flushPendingMasterSyncQueue(context)
         }
     }
     ```
  3. In `MasterDataSyncSentinel.kt` line 267:
     ```kotlin
     @JvmStatic
     fun flushPendingMasterSyncQueue(context: Context) {
         val appContext = context.applicationContext
         if (!isNetworkAvailable(appContext) || !FirebaseComm.isAuthenticated()) return
         ...
     ```
  4. During an offline-to-online transition, the cellular/Wi-Fi bearer becomes active before Firebase Auth completes its background token refresh.
  5. At 3000ms, `isNetworkAvailable` is true, but `FirebaseComm.isAuthenticated()` is FALSE.
  6. `flushPendingMasterSyncQueue` encounters `!FirebaseComm.isAuthenticated()`, executes `return`, and aborts. No retry or exponential backoff is scheduled, leaving `sync_queue` stranded!
- **Underlying Failure Mechanism**: Asynchronous dependency race between baseband network availability and Firebase Auth token state, coupled with lack of retry scheduling on precondition failure.
- **File & Line References**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/remote/OfflineSyncManager.kt:108-118, 314-318`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/engine/sync/MasterDataSyncSentinel.kt:265-270`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/local/tables/PendingSyncQueueTable.kt:14`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/local/tables/SyncQueueTable.kt:12`

---

### 3.7 GAP-07: Split-Brain Group Relationships (`/members` vs `/devices`)
- **Empirical Observation**: Peer devices appear and disappear from the Friends & Family roster; pairing links present in one screen are absent in another.
- **Reverse Engineering Trace**:
  1. In RTDB under group circle `GRP_2T929153`:
     - `/relationships/GRP_2T929153/members` contains flat identity maps (`avatar_id`, `display_name`, `role`, `status`).
     - `/relationships/GRP_2T929153/devices` contains hierarchical device maps (`identity`, `membership`, `paired_with`).
  2. Codebase write bifurcation:
     - `PairingRepository.kt:234, 449, 756` writes exclusively to `/relationships/{groupId}/devices/{agentId}`.
     - `AuthViewModel.kt:129, 430` and `GroupViewModel.kt:151, 197, 248` write exclusively to `/relationships/{groupId}/members/{agentId}`.
  3. In `FamilyFriendsViewModel.kt` lines 290–310:
     ```kotlin
     db.child("relationships").child(groupId).child("members").addValueEventListener(rosterListener!!)
     ...
     db.child("relationships").child(groupId).child("devices").addValueEventListener(devicesSocketListener!!)
     ```
  4. Both listeners invoke `resolveRosterIdentities(snapshot, context)`. Each invocation constructs a brand new `resolvedNodes` list from the provided snapshot and executes:
     ```kotlin
     _rosterLiveData.postValue(sortedCached)
     ```
  5. If `rosterListener` triggers with `/members`, any peer added only to `/devices` is removed from the UI. When `devicesSocketListener` triggers a moment later, the peer reappears.
- **Underlying Failure Mechanism**: Dual-tree cloud schema coupled with independent listeners replacing the entire UI state rather than performing atomic key-based merges.
- **File & Line References**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/ui/group/FamilyFriendsViewModel.kt:290-330, 355-420`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/repository/PairingRepository.kt:234, 449, 756`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/ui/group/GroupViewModel.kt:151, 197, 248`

---

### 3.8 GAP-08: Orphaned Auth UID in SQLite `device_vitals`
- **Empirical Observation**: In SQLite `device_vitals`, an orphaned row exists with `hardware_id = 'fK6ln4idkPbAlRY1wtRG8BqTFvi1'` (Firebase Auth UID of host `2T929153`) alongside `hardware_id = '2T929153'`.
- **Reverse Engineering Trace**:
  1. In `LocationRepository.kt` line 272:
     ```kotlin
     val canonicalStoreId = if (isSelf) (if (!myUid.isNullOrEmpty()) myUid else myAgentId) else resolvedId
     ```
     For the host device, `canonicalStoreId` prefers `myUid` (`fK6ln4idkPbAlRY1wtRG8BqTFvi1`) over `myAgentId` (`2T929153`).
  2. In `LocationRepository.kt` lines 309–312:
     ```kotlin
     db.updateDeviceVitalsTelemetry(canonicalStoreId, if (batt > 0) batt else 100, effTs)
     if (resolvedId != canonicalStoreId) {
         db.updateDeviceVitalsTelemetry(resolvedId, if (batt > 0) batt else 100, effTs)
     }
     ```
  3. This dual-write inserts two separate rows into `device_vitals`:
     - Row 1: `hardware_id = 'fK6ln4idkPbAlRY1wtRG8BqTFvi1'`
     - Row 2: `hardware_id = '2T929153'`
  4. Because `DatabaseHelper.getOfflineRoster()` joins on `user_identity.uid_1` (which stores canonical 8-character ID `2T929153`), the Auth UID row is orphaned. If background updates only touch `canonicalStoreId`, the roster displays stale vitals.
- **Underlying Failure Mechanism**: Identity ambiguity where transient Firebase Auth UIDs (28 chars) and persistent silicon Hardware IDs (8 chars) are treated as interchangeable database keys.
- **File & Line References**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/repository/LocationRepository.kt:272-273, 309-313`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/local/DatabaseHelper.kt:4087-4094`

---

### 3.9 GAP-09: Decommissioned Node `A7D5968D` Phantom Location Ingestions
- **Empirical Observation**: Local SQLite `sys_log` contains 12 entries recording: `LocationRepository: Ingesting location for Node: A7D5968D (Lat: 0.0, Lng: 0.0)`.
- **Reverse Engineering Trace**:
  1. In Firebase RTDB under `/relationships/GRP_2T929153/devices/2T929153/paired_with`, the key `'A7D5968D': true` remains from a legacy test run.
  2. In `LocationRepository.kt` lines 648–656:
     ```kotlin
     val unifiedPairs = relSnap.child("devices").child(finalId).child("paired_with")
     unifiedPairs.children.forEach { pair ->
         if (pair.getValue(Boolean::class.java) == true && pair.key != null) {
             if (!csv.contains("${pair.key},")) {
                 csv.append(pair.key).append(",")
             }
         }
     }
     ```
  3. `A7D5968D` is appended to `linked_devices_csv`.
  4. At line 695, `updateTargetListeners()` is called. Line 333 parses `linked_devices_csv` and adds `A7D5968D` to `targetUids`.
  5. Line 408 attaches a listener to `/devices/A7D5968D/device/live`.
  6. When the initial snapshot returns empty (`rawLoc == null`), lines 420–442 construct a default `UserLocation(latitude = 0.0, longitude = 0.0)`.
  7. `processIncomingLocation` executes, logging phantom `(0.0, 0.0)` location fixes for the ghost node into SQLite `sys_log`.
- **Underlying Failure Mechanism**: Blind ingestion of legacy cloud relationship keys without validating node existence in local identity or roster tables.
- **File & Line References**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/repository/LocationRepository.kt:408-444, 648-656`
  * `Firebase RTDB: /relationships/GRP_2T929153/devices/2T929153/paired_with/A7D5968D`

---

### 3.10 GAP-10: Pairing & Sharing 15,000ms Watchdog Timeout Deadlocks
- **Empirical Observation**: All pairing sessions in SQLite `device_pairing` (`PAIR_F8CAFC67_4379A761`, `PAIR_2T929153_F8CAFC67`, `PAIR_2D113789_2C811042`, `PAIR_2T929153_2D113789`) and `device_sharing` are marked `TIMED_OUT`.
- **Reverse Engineering Trace**:
  1. In `RegistryManager.kt` line 148:
     ```kotlin
     const val DEFAULT_DEVICE_SHARING_TIMEOUT_MS = 15000L
     ```
  2. In `PairingRepository.kt` lines 989–1014:
     ```kotlin
     val timeoutRunnable = java.lang.Runnable {
         if (!isResolved.get()) {
             isResolved.set(true)
             // Abort handshake and log TIMED_OUT to SQLite
         }
     }
     mainHandler.postDelayed(timeoutRunnable, timeoutMs)
     ```
  3. Over real-world cellular radio conditions, waking up a remote device via FCM push, acquiring a WakeLock, establishing an RTDB WebSocket connection, and publishing an acknowledgement ACK takes between 15 to 45 seconds.
  4. The 15,000ms timer reliably fires before the remote peer's ACK arrives, aborting the session and marking SQLite records `TIMED_OUT`.
- **Underlying Failure Mechanism**: Unrealistic timeout ceiling for cross-device cellular handshakes subject to Android Doze mode and radio power state transitions.
- **File & Line References**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/utils/RegistryManager.kt:148, 1584-1588`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/repository/PairingRepository.kt:941, 989-1014, 1112, 1164-1189`

---

### 3.11 GAP-11: FCM Background Push Notification Persistence Decoupling
- **Empirical Observation**: Push alerts delivered while the application is inactive or backgrounded appear in the Android notification drawer but are never written to SQLite `alert_history` or synced with cloud status.
- **Reverse Engineering Trace**:
  1. In `FCMService.kt` lines 240–245:
     ```kotlin
     // 2. Process Standard Notification Payload
     remoteMessage.notification?.let {
         val title = it.title ?: "Mission Sentinel"
         val body = it.body ?: "Perimeter alert received."
         AlertManager.showGeofenceNotification(applicationContext, "$title: $body")
     }
     ```
  2. When an incoming message contains a `notification` payload, `FCMService` displays a status bar notification via `AlertManager`, but NEVER calls `persistAlertNotification()` (lines 322–343).
  3. Furthermore, on Android OS, if an FCM push contains both `notification` and `data` blocks, Android OS system tray consumes the `notification` while the app is backgrounded and does NOT invoke `onMessageReceived` unless the user taps the notification.
- **Underlying Failure Mechanism**: Decoupling of the UI notification presentation pipeline from the SQLite audit persistence pipeline, compounded by mixed notification/data payloads.
- **File & Line References**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/service/FCMService.kt:240-249, 322-343`

---

## 4. Comprehensive Schema Mapping & Discrepancy Matrix

| Entity Domain | Local SQLite Schema (`JFindMe_CitizenData.db` v84) | Firebase RTDB Schema (`asia-southeast1`) | Data Type & Structural Divergence | Failure Mode |
| :--- | :--- | :--- | :--- | :--- |
| **Vitals & Battery** | `device_vitals.battery_level: INTEGER` | `telemetry.battery_pct: Int`<br>`device.live.battery: Int` | RTDB publishes AC power / uncalibrated as `-1`; SQLite stores positive integers. | Ternary `batt > 0 ? batt : 100` converts `-1` to artificial `100%`. |
| **Presence / Session** | `user_identity.st_session_state: TEXT` (`'OFFLINE'`) | `device.live.is_online: Boolean`<br>`device.live.status: String` (`"ONLINE"`) | SQLite column defaults to `'OFFLINE'` and is never updated on socket connect. | Host device marked OFFLINE locally while actively streaming to cloud. |
| **Rendezvous Distance** | `butterfly_point_device_map.distance_to_point_meters: REAL` | `butterfly_points/{id}/devices/{id}/distance_meters: Double` | Column name mismatch: `distance_to_point_meters` vs `distance_meters`. | Properties dropped during standard POJO deserialization. |
| **Rendezvous SOS** | `butterfly_point_device_map.is_sos: INTEGER` (`0` or `1`) | `butterfly_points/{id}/devices/{id}/is_sos: Boolean` (`true` or `false`) | Type mismatch: SQLite integer flag vs Firebase JSON boolean. | Parsing errors or defaulting to false during deserialization. |
| **Group Relationships** | `group_members.group_id: TEXT`<br>`group_members.identity_id: INTEGER` | `/relationships/{group}/members/{id}`<br>`/relationships/{group}/devices/{id}` | Dual-tree split brain: flat member properties vs nested device dossiers. | Listeners replace entire UI state, causing members to vanish/reappear. |
| **Offline Queue** | `pending_sync_queue` (Location pings)<br>`sync_queue` (Master mutations) | `/devices/{id}/history`<br>`/devices/{id}/profile` | Disjoint queue schemas drained by separate sentinels with differing preconditions. | Master data queue stalls if Firebase Auth is delayed during reconnect. |
| **Identity Anchors** | `user_identity.uid_1` (8-char Silicon ID)<br>`user_identity.auth_uid` (28-char Auth UID) | `/devices/{agentId}/Identity`<br>`/lookup_indices/auth_to_device/{authUid}` | Ambiguous key usage (`canonicalStoreId` prefers Auth UID over Hardware ID). | Duplicate rows created in `device_vitals`; orphan rows left unlinked. |

---

## 5. Concrete Non-Destructive Remediation Blueprints (Guide for Milestone 3)

In strict adherence to the **Antigravity AI Enterprise Development Rules** and **Prime Directives**:
1. *"Do not delete earlier codes: either update the deprecated one or add and merge the new codes."*
2. *Every code change must declare: `[Variables] | [Constants] | [Dependencies] | [Integration Points]` before modification.*

### 5.1 Remediation Blueprint 1: Sensor & Battery Vitals Dual-Path Ingestion
- **Target Files**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/repository/LocationRepository.kt`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/ui/group/NetworkGatewayActivity.java`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/ui/group/FamilyFriendsViewModel.kt`
- **Specification**:
  1. Update `UserLocation.kt` to bind both `battery` and `battery_pct` aliases via `@get:PropertyName` and fallback accessors.
  2. In `LocationRepository.kt:307-312`, replace rigid ternary `if (batt > 0) batt else 100` with adaptive resolution:
     ```kotlin
     // [Variables] effTs: Long, batt: Int, resolvedBatt: Int | [Constants] None | [Dependencies] DatabaseHelper | [Integration Points] DeviceVitalsTable
     val effTs = loc.getLongTimestamp()
     val rawBatt = loc.batteryLevel
     val resolvedBatt = when {
         rawBatt in 0..100 -> rawBatt
         loc.isCharging -> -1 // Preserve AC tethering indicator
         else -> db.getLatestBatteryLevel(resolvedId).takeIf { it in 0..100 } ?: rawBatt
     }
     if (effTs > 0L && resolvedBatt != -100) {
         db.updateDeviceVitalsTelemetry(resolvedId, resolvedBatt, effTs, loc.isCharging)
     }
     ```
  3. In `FamilyFriendsViewModel.kt:466-470`, read battery from `/telemetry/battery_pct` before falling back to local database.

### 5.2 Remediation Blueprint 2: Presence Socket Local Write-Back Hook
- **Target Files**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/remote/FirebaseComm.kt`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/local/tables/UserIdentityTable.kt`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/local/DatabaseHelper.kt`
- **Specification**:
  1. Add `UserIdentityTable.updateSessionState(db, agentId, sessionState)`:
     ```kotlin
     // [Variables] db: SQLiteDatabase, agentId: String, sessionState: String | [Constants] COL_SESSION_STATE | [Dependencies] ContentValues | [Integration Points] UserIdentityTable
     fun updateSessionState(db: SQLiteDatabase, agentId: String, sessionState: String): Int {
         val cv = ContentValues().apply { put(COL_SESSION_STATE, sessionState) }
         return db.update(TABLE_NAME, cv, "$COL_UID_1 = ? OR $COL_UID_2 = ?", arrayOf(agentId, agentId))
     }
     ```
  2. In `FirebaseComm.kt:1907-1965` (`setupPresenceSentinel`), when `.info/connected` changes, invoke `DatabaseHelper.getInstance(context).updateSessionState(agentId, if (connected) "ONLINE" else "OFFLINE")`.

### 5.3 Remediation Blueprint 3: Thread-Safe Butterfly Point Collections & Defensive Copying
- **Target Files**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/model/ButterflyPointModel.kt`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/ui/map/MapActivity.java`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/ui/map/ButterflyPointDialog.java`
- **Specification**:
  1. In `ButterflyPointModel.kt`, add defensive snapshot getter:
     ```kotlin
     // [Variables] None | [Constants] None | [Dependencies] ConcurrentHashMap | [Integration Points] MapActivity, ButterflyPointDialog
     fun getDevicesSnapshot(): Map<String, ButterflyDeviceEntry> {
         return java.util.HashMap(devices)
     }
     ```
  2. In `MapActivity.java:4128` and `ButterflyPointDialog.java:400`, replace direct iteration on `pt.getDevices().entrySet()` with `pt.getDevicesSnapshot().entrySet()`.

### 5.4 Remediation Blueprint 4: Atomic Child Updates in Butterfly Point Dispatch
- **Target Files**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/engine/map/ButterflyPointEngine.kt`
- **Specification**:
  1. Replace whole-object write `rtdb.child("butterfly_points").child(point.pointId).setValue(cloudMap)` with selective update map:
     ```kotlin
     // [Variables] pointUpdates: HashMap<String, Any> | [Constants] None | [Dependencies] FirebaseDatabase | [Integration Points] ButterflyPointEngine
     val pointUpdates = hashMapOf<String, Any>()
     pointUpdates["point_id"] = point.pointId
     pointUpdates["creator_device_id"] = point.creatorDeviceId
     pointUpdates["point_name"] = point.pointName
     pointUpdates["lat"] = point.lat
     pointUpdates["lng"] = point.lng
     pointUpdates["rendezvous_timestamp"] = point.rendezvousTimestamp
     pointUpdates["expiry_timestamp"] = point.expiryTimestamp
     pointUpdates["arrival_radius_meters"] = point.arrivalRadiusMeters
     pointUpdates["is_active"] = point.isActive
     // Update root metadata without clobbering existing /devices child nodes!
     rtdb.child("butterfly_points").child(point.pointId).updateChildren(pointUpdates)
     ```

### 5.5 Remediation Blueprint 5: Schema Aliasing & Type Conversion in Rendezvous Tables
- **Target Files**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/local/tables/ButterflyPointDeviceMapTable.kt`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/model/ButterflyPointModel.kt`
- **Specification**:
  1. Annotate `ButterflyDeviceEntry` in `ButterflyPointModel.kt` with `@PropertyName`:
     ```kotlin
     @get:com.google.firebase.database.PropertyName("distance_meters")
     @set:com.google.firebase.database.PropertyName("distance_meters")
     var distanceMeters: Double = 0.0,

     @get:com.google.firebase.database.PropertyName("is_sos")
     @set:com.google.firebase.database.PropertyName("is_sos")
     var isSosActive: Boolean = false,
     ```
  2. In `ButterflyPointDeviceMapTable.kt`, support both `distance_to_point_meters` and `distance_meters` interchangeably in SQL queries and ContentValues mappings.

### 5.6 Remediation Blueprint 6: Resilient Master Sync Queue Draining with Auth Retry Loop
- **Target Files**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/engine/sync/MasterDataSyncSentinel.kt`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/remote/OfflineSyncManager.kt`
- **Specification**:
  1. In `MasterDataSyncSentinel.kt:265-275`, add exponential backoff retry when `!FirebaseComm.isAuthenticated()`:
     ```kotlin
     if (!FirebaseComm.isAuthenticated()) {
         SystemLogger.w(TAG, "Firebase Auth not ready. Scheduling master sync retry in 4000ms.")
         Handler(Looper.getMainLooper()).postDelayed({
             flushPendingMasterSyncQueue(context)
         }, 4000L)
         return
     }
     ```

### 5.7 Remediation Blueprint 7: Group Relationship Single-Write / Dual-Read Harmonization
- **Target Files**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/repository/PairingRepository.kt`
  * `jButterFly/src/main/java/com/jdroidx/jfindme/ui/group/FamilyFriendsViewModel.kt`
- **Specification**:
  1. When creating or modifying memberships in `PairingRepository.kt`, write to BOTH `/relationships/{groupId}/members/{id}` AND `/relationships/{groupId}/devices/{id}` in a single atomic multi-path update.
  2. In `FamilyFriendsViewModel.kt`, merge snapshots by ID into a unified `ConcurrentHashMap<String, RosterNode>` before posting to `_rosterLiveData`.

### 5.8 Remediation Blueprint 8: Canonical Hardware ID Anchor Enforcement
- **Target Files**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/repository/LocationRepository.kt`
- **Specification**:
  1. In `LocationRepository.kt:272`, strictly anchor storage to canonical 8-character Agent ID:
     ```kotlin
     // [Variables] canonicalStoreId: String | [Constants] None | [Dependencies] DeviceIdentityHelper | [Integration Points] LocationRepository, DeviceVitalsTable
     val canonicalStoreId = if (isSelf) myAgentId else resolvedId
     ```
  2. Purge dual-writing with `auth_uid` to eliminate orphaned rows in `device_vitals`.

### 5.9 Remediation Blueprint 9: Decommissioned Node `A7D5968D` Legacy Stub Pruning
- **Target Files**:
  * Firebase RTDB Cloud Pruning Script (`scratch/prune_tombstone_stubs.py`)
  * `jButterFly/src/main/java/com/jdroidx/jfindme/data/repository/LocationRepository.kt`
- **Specification**:
  1. Prune `/relationships/GRP_2T929153/devices/2T929153/paired_with/A7D5968D`.
  2. In `LocationRepository.kt:380`, filter target candidate IDs against local database soft-delete and registration tables before attaching listeners.

### 5.10 Remediation Blueprint 10: Adaptive Pairing Watchdog Timeout
- **Target Files**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/utils/RegistryManager.kt`
- **Specification**:
  1. Increase `DEFAULT_DEVICE_SHARING_TIMEOUT_MS` from `15000L` (15s) to `45000L` (45s) to accommodate cellular Doze wakeup windows.

### 5.11 Remediation Blueprint 11: Pure Data FCM Push Payloads & Background Alert Persistence
- **Target Files**:
  * `jButterFly/src/main/java/com/jdroidx/jfindme/service/FCMService.kt`
- **Specification**:
  1. Enforce pure `data` payloads with `priority: "high"` across all cloud messaging dispatchers.
  2. In `FCMService.kt:240-245`, ensure standard notification payloads call `persistAlertNotification()` before showing notifications.

---

## 6. Verification & Automated Regression Protocol

To independently verify all findings and validate the Milestone 3 remediation:
1. **SQLite Vitals & Table Counts**:
   ```powershell
   python -c "import sqlite3; conn = sqlite3.connect(r'c:\Users\jiten\jAnitGravity\jFindMe\scratch\survey_db\JFindMe_CitizenData.db'); cur = conn.cursor(); print('Device Vitals Rows:', cur.execute('SELECT hardware_id, battery_level, st_session_state FROM device_vitals LEFT JOIN user_identity ON device_vitals.hardware_id = user_identity.uid_1').fetchall())"
   ```
2. **RTDB Schema & Battery Inspection**:
   ```powershell
   python -c "import json; rtdb = json.load(open(r'C:\Users\jiten\.gemini\antigravity\scratch\teamwork\d0653289-7604-4168-b42d-e11937ae351b\.agents\teamwork\teamwork_explorer_survey_2\rtdb_full_inspection.json')); [print(k, 'live_batt:', v.get('device', {}).get('live', {}).get('battery'), 'telem_batt:', v.get('telemetry', {}).get('battery_pct')) for k, v in rtdb.get('devices', {}).items()]"
   ```
3. **Gate 1 Minor Protection Automated Invariant Audit**:
   ```powershell
   python -c "import re; text = open(r'c:\Users\jiten\jAnitGravity\jFindMe\SYNCHRONIZATION_RCA_REA_REPORT.md', encoding='utf-8').read(); coords = re.findall(r'4379A761[^\n]*\b\d{1,2}\.\d{3,}\b', text); assert len(coords) == 0, f'LEAK: {coords}'; print('GATE 1 AUDIT: PASSED (Zero Coordinates)')"
   ```

---
*Report completed by Worker jDroid-X for Milestone 2.*
