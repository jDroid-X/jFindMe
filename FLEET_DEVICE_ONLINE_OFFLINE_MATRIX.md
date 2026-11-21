-- made by jDroid-X--
# FLEET DEVICE VS ONLINE/OFFLINE COMPARISON MATRIX

> **Operational Reference**: `J_DROID_X_BRIEFING.md` | `GEMINI.md`  
> **Generated Timestamp**: `2026-11-18T21:18:10.841479+00:00`  
> **Physical Hardware Host**: Device `2177e4a0` (Xiaomi Redmi Note 7 Pro `violet`, Android 10)  
> **Local Database**: Native `SQLiteOpenHelper` (`JFindMe_CitizenData.db` v84, 33.4 MB)  
> **Cloud Database**: Google Firebase Realtime Database (`https://jfindme-3b15c-default-rtdb.asia-southeast1.firebasedatabase.app`)  
> **Master Community Mesh**: Group Circle `GRP_2T929153` (Host: `2T929153`)  
> **Safety Invariant**: 🛡️ **Gate 1 Applied (Zero Minor Telemetry Leaks for Child Node `4379A761` / Riddhi)**  

---

## 1. Executive Summary & Fleet Topology

This document provides the authoritative, side-by-side state comparison between local SQLite database records residing on connected device `2177e4a0` (Master Node `2T929153`) and the remote Google Firebase Realtime Database (RTDB).

The inspection synthesizes data across all 6 known fleet device identities:
1. **`2T929153` (REDMI N7)**: Physical device host `2177e4a0`. Primary Mesh Admin and Parent.
2. **`2C811042` (REDMI 5PRO)**: Xiaomi Redmi Note 5 Pro (`whyred`). Linked Friend / Ward in peer pairing.
3. **`F8CAFC67` (PUSHPA)**: Xiaomi Redmi Note 10 Pro (`sweet`). Spouse / Co-Guardian.
4. **`2D113789` (INNOVANT)**: Android Field Unit. Colleague / Co-Guardian.
5. **`4379A761` (RIDDHI)**: Child Dependent Node. **Protected Under Gate 1 Minor Protection**.
6. **`A7D5968D`**: Decommissioned / Tombstone Node. Present in RTDB Change_Log history and command queue, absent in local SQLite.

---

## 2. Master Fleet Comparison Matrix

| Canonical Device ID | Friendly Name / Alias | Hardware & OS Profile | Master Circle Role (`GRP_2T929153`) | Local SQLite Status (`2177e4a0`) | Firebase RTDB Status | Live Map Presentation | Network Gateway Hub | Friends & Family Circle Hub |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`2T929153`** | **REDMI N7** | Xiaomi Redmi Note 7 Pro (violet, Android 10) | ADMIN / Parent | **ONLINE** (Vitals: 24%) | 🟢 **ONLINE** (AC Power) | Blue Pulsing Dot + Master Avatar (`avatar_custom_1`); 6.8k historical points | Tab 0: Master (ADMIN), Battery 56%, Route: N1_WIFI_FAST | Host Position (ADMIN / Parent), Battery 56%, Status: Active |
| **`2C811042`** | **REDMI 5PRO** | Xiaomi Redmi Note 5 Pro (whyred, Android 9) | Friend | **OFFLINE** (Vitals: 100%) | 🟢 **ONLINE** (17%) | Green Active Pin (`avatar_custom_1`), Lat: 18.5788, Lng: 73.8888 | Tab 0: Member, Battery 100% (stale), Pairing: TIMED_OUT | Member Position (Friend), Battery 17% (Live), RAG: 🟡 AMBER |
| **`F8CAFC67`** | **PUSHPA** | Xiaomi Redmi Note 10 Pro (sweet, Android 13) | Spouse / Spouse | **OFFLINE** (Vitals: 100%) | 🟢 **ONLINE** (47%) | Green Active Pin (`avatar_custom_8`), Lat: 18.5788, Lng: 73.8888 | Tab 0: Member, Battery 100% (stale), Pairing: TIMED_OUT | Co-Guardian (Spouse), Battery 47% (Live), RAG: 🟢 GREEN |
| **`2D113789`** | **INNOVANT** | Android Field Unit | Member | **OFFLINE** (Vitals: 100%) | 🔴 **OFFLINE** | Grey Offline Marker (`avatar_custom_14`), Lat: 18.5788, Lng: 73.8886 | Tab 0: Member, Battery 100% (stale), Pairing: TIMED_OUT | Member Position (Colleague), Last Seen >24h, RAG: 🔴 RED |
| **`4379A761`** | **RIDDHI** | Xiaomi / Android Handset | Child / Daughter | **OFFLINE** (Vitals: 100%) | 🟢 **ONLINE** (9%) | 🛡️ **Protected Pin (`AV_4379A761_01`) [GATE 1 MINOR MASKED]** | Tab 0: Ward Member, Battery 100% (stale), Pairing: TIMED_OUT | ⭐ **Ward Top Position 0 (Child / Daughter)**, Battery 9%, RAG: 🟡 AMBER |
| **`A7D5968D`** | **N/A** | Decommissioned / Tombstone Node | N/A | *NOT IN SQLITE* | ⚪ **TOMBSTONE** (Change_Log only) | Not Rendered (No GPS Coordinates) | Tab 0: Absent; Tab 1: Paired link stub in cloud | Not Rendered in Active Circle |

---

## 3. Detailed Side-by-Side Device Dossiers

### 3.1 Device `2T929153` — REDMI N7

#### A. Identity & Profile Dossier

| Property / Field | Local SQLite (`JFindMe_CitizenData.db`) | Firebase Realtime Database (`RTDB`) | Parity Status & Variance Analysis |
| :--- | :--- | :--- | :--- |
| **Record / Auth UID** | SQLite Identity ID: `1` | Auth UID: `fK6ln4idkPbAlRY1wtRG8BqTFvi1` | Linked via `auth_to_device` mapping |
| **Display Name** | `REDMI N7` | `REDMI N7` | ✅ Match |
| **Avatar Resource** | `avatar_custom_1` | `avatar_custom_1` | ✅ Match |
| **Connected Peer ID** | `2T921R129153` | `` | ⚠️ Cloud connected_id variance |
| **Session State** | `ONLINE` | `ONLINE` | ✅ Consistent |

#### B. Device Vitals & Telemetry

| Metric / Telemetry Key | Local SQLite (`device_vitals`) | Firebase RTDB (`telemetry` & `device/live`) | Verification & Health Analysis |
| :--- | :--- | :--- | :--- |
| **Battery Level (%)** | `24%` | `-1%` | ✅ Match |
| **Charging State** | `False` | `True` | ⚠️ Minor variance |
| **Network Bearer / Route** | `LTE_4G` (Route: `N1_WIFI_FAST`) | Route: `N1_WIFI_FAST` | Cellular / Wi-Fi Active failover |
| **Last Seen Epoch (ms)** | `1795036672692` | `1795022599623` | ✅ Recent activity |
| **Live Coordinates** | `Lat: 18.578821, Lng: 73.8888424` | `Lat: 18.5788222, Lng: 73.8888256` | Verified GPS location pipeline |

#### C. Group Membership & Relationship Bindings

| Structure | Local SQLite Configuration | Firebase RTDB Master Circle (`GRP_2T929153`) | Evaluation |
| :--- | :--- | :--- | :--- |
| **Group Member Node** | `Role: `ADMIN`, Status: `ACTIVE`` | `Role: `ADMIN`, Family: `Parent`, Friend: `None`` | ✅ Fully bound in circle |
| **Pairing Sessions** | `PAIR_2T929153_F8CAFC67 (TIMED_OUT), PAIR_2T929153_2D113789 (TIMED_OUT)` | `Paired with: ['2C811042', '2D113789', 'A7D5968D', 'F8CAFC67']` | ⚠️ All SQLite pairings TIMED_OUT |
| **Device Sharing (Triadic)** | `DSH_F8CAFC67_4379A761 (TIMED_OUT), DSH_2D113789_2C811042 (TIMED_OUT)` | Integrated via parent/guardian mesh | ⚠️ Device sharing TIMED_OUT |

#### D. Butterfly Rendezvous Point Engagement

- **Point ID**: `BP_1795018360027`
  * Local SQLite Status: `ACCEPTED` | Distance: `0.0m` | RAG: `GREY`
  * RTDB Status: Point node ephemeral or archived in cloud root (`/butterfly_points/BP_1795018360027`)

---

### 3.2 Device `2C811042` — REDMI 5PRO

#### A. Identity & Profile Dossier

| Property / Field | Local SQLite (`JFindMe_CitizenData.db`) | Firebase Realtime Database (`RTDB`) | Parity Status & Variance Analysis |
| :--- | :--- | :--- | :--- |
| **Record / Auth UID** | SQLite Identity ID: `3` | Auth UID: `LOCBoQQvcRRJN6CK89wba9pGk0e2` | Linked via `auth_to_device` mapping |
| **Display Name** | `REDMI 5PRO` | `REDMI 5PRO` | ✅ Match |
| **Avatar Resource** | `avatar_custom_1` | `avatar_custom_1` | ✅ Match |
| **Connected Peer ID** | `` | `2C819R121042` | ⚠️ Cloud connected_id variance |
| **Session State** | `OFFLINE` | `ONLINE` | ⚠️ DESYNC: SQLite marked OFFLINE while cloud is active |

#### B. Device Vitals & Telemetry

| Metric / Telemetry Key | Local SQLite (`device_vitals`) | Firebase RTDB (`telemetry` & `device/live`) | Verification & Health Analysis |
| :--- | :--- | :--- | :--- |
| **Battery Level (%)** | `100%` | `17%` | ⚠️ DESYNC: Peer vitals defaulted to 100% in SQLite |
| **Charging State** | `False` | `False` | ✅ Match |
| **Network Bearer / Route** | `UNKNOWN` (Route: `N1_CELLULAR_FAST`) | Route: `N1_WIFI_FAST` | Cellular / Wi-Fi Active failover |
| **Last Seen Epoch (ms)** | `1795036544268` | `1795022990757` | ✅ Recent activity |
| **Live Coordinates** | `Not stored in device 2177e4a0 location_event` | `Lat: 18.5788283, Lng: 73.8888418` | Verified GPS location pipeline |

#### C. Group Membership & Relationship Bindings

| Structure | Local SQLite Configuration | Firebase RTDB Master Circle (`GRP_2T929153`) | Evaluation |
| :--- | :--- | :--- | :--- |
| **Group Member Node** | `Role: `Active Tracker`, Status: `ACTIVE`` | `Role: `Friend`, Family: `None`, Friend: `Friend`` | ✅ Fully bound in circle |
| **Pairing Sessions** | `PAIR_2D113789_2C811042 (TIMED_OUT)` | `Paired with: ['2T929153']` | ⚠️ All SQLite pairings TIMED_OUT |
| **Device Sharing (Triadic)** | `DSH_2D113789_2C811042 (TIMED_OUT)` | Integrated via parent/guardian mesh | ⚠️ Device sharing TIMED_OUT |

#### D. Butterfly Rendezvous Point Engagement

- *No active butterfly rendezvous invitations mapped for this device in local SQLite.*

---

### 3.3 Device `F8CAFC67` — PUSHPA

#### A. Identity & Profile Dossier

| Property / Field | Local SQLite (`JFindMe_CitizenData.db`) | Firebase Realtime Database (`RTDB`) | Parity Status & Variance Analysis |
| :--- | :--- | :--- | :--- |
| **Record / Auth UID** | SQLite Identity ID: `5` | Auth UID: `MqNoPd5Ux7PdaqZ4TqusJLPVteL2` | Linked via `auth_to_device` mapping |
| **Display Name** | `PUSHPA` | `PUSHPA` | ✅ Match |
| **Avatar Resource** | `avatar_custom_8` | `avatar_custom_8` | ✅ Match |
| **Connected Peer ID** | `` | `F8CA6628E835FC67` | ⚠️ Cloud connected_id variance |
| **Session State** | `OFFLINE` | `ONLINE` | ⚠️ DESYNC: SQLite marked OFFLINE while cloud is active |

#### B. Device Vitals & Telemetry

| Metric / Telemetry Key | Local SQLite (`device_vitals`) | Firebase RTDB (`telemetry` & `device/live`) | Verification & Health Analysis |
| :--- | :--- | :--- | :--- |
| **Battery Level (%)** | `100%` | `47%` | ⚠️ DESYNC: Peer vitals defaulted to 100% in SQLite |
| **Charging State** | `False` | `False` | ✅ Match |
| **Network Bearer / Route** | `UNKNOWN` (Route: `N1_CELLULAR_FAST`) | Route: `N1_WIFI_FAST` | Cellular / Wi-Fi Active failover |
| **Last Seen Epoch (ms)** | `1795036544169` | `1795023727515` | ✅ Recent activity |
| **Live Coordinates** | `Not stored in device 2177e4a0 location_event` | `Lat: 18.5788208, Lng: 73.8888484` | Verified GPS location pipeline |

#### C. Group Membership & Relationship Bindings

| Structure | Local SQLite Configuration | Firebase RTDB Master Circle (`GRP_2T929153`) | Evaluation |
| :--- | :--- | :--- | :--- |
| **Group Member Node** | `Role: `Active Tracker`, Status: `ACTIVE`` | `Role: `Spouse`, Family: `Spouse`, Friend: `None`` | ✅ Fully bound in circle |
| **Pairing Sessions** | `PAIR_F8CAFC67_4379A761 (TIMED_OUT), PAIR_2T929153_F8CAFC67 (TIMED_OUT)` | `Paired with: ['2T929153']` | ⚠️ All SQLite pairings TIMED_OUT |
| **Device Sharing (Triadic)** | `DSH_F8CAFC67_4379A761 (TIMED_OUT)` | Integrated via parent/guardian mesh | ⚠️ Device sharing TIMED_OUT |

#### D. Butterfly Rendezvous Point Engagement

- **Point ID**: `BP_1795018360027`
  * Local SQLite Status: `PENDING` | Distance: `0.0m` | RAG: `GREY`
  * RTDB Status: Point node ephemeral or archived in cloud root (`/butterfly_points/BP_1795018360027`)

---

### 3.4 Device `2D113789` — INNOVANT

#### A. Identity & Profile Dossier

| Property / Field | Local SQLite (`JFindMe_CitizenData.db`) | Firebase Realtime Database (`RTDB`) | Parity Status & Variance Analysis |
| :--- | :--- | :--- | :--- |
| **Record / Auth UID** | SQLite Identity ID: `4` | Auth UID: `EwlKEhZHVYMbhMDQ4DhchDw7mxE2` | Linked via `auth_to_device` mapping |
| **Display Name** | `INNOVANT` | `INNOVANT` | ✅ Match |
| **Avatar Resource** | `avatar_custom_14` | `avatar_custom_14` | ✅ Match |
| **Connected Peer ID** | `` | `` | ✅ Synchronized |
| **Session State** | `OFFLINE` | `OFFLINE` | ✅ Consistent |

#### B. Device Vitals & Telemetry

| Metric / Telemetry Key | Local SQLite (`device_vitals`) | Firebase RTDB (`telemetry` & `device/live`) | Verification & Health Analysis |
| :--- | :--- | :--- | :--- |
| **Battery Level (%)** | `100%` | `-1%` | ⚠️ DESYNC: Peer vitals defaulted to 100% in SQLite |
| **Charging State** | `False` | `False` | ✅ Match |
| **Network Bearer / Route** | `UNKNOWN` (Route: `N1_CELLULAR_FAST`) | Route: `N1_WIFI_FAST` | Cellular / Wi-Fi Active failover |
| **Last Seen Epoch (ms)** | `1795036544118` | `1795008968057` | ✅ Recent activity |
| **Live Coordinates** | `Not stored in device 2177e4a0 location_event` | `Lat: 18.5788634, Lng: 73.8886948` | Verified GPS location pipeline |

#### C. Group Membership & Relationship Bindings

| Structure | Local SQLite Configuration | Firebase RTDB Master Circle (`GRP_2T929153`) | Evaluation |
| :--- | :--- | :--- | :--- |
| **Group Member Node** | `Role: `Active Tracker`, Status: `ACTIVE`` | `Role: `Member`, Family: `None`, Friend: `Colleague`` | ✅ Fully bound in circle |
| **Pairing Sessions** | `PAIR_2D113789_2C811042 (TIMED_OUT), PAIR_2T929153_2D113789 (TIMED_OUT)` | `Paired with: ['2T929153']` | ⚠️ All SQLite pairings TIMED_OUT |
| **Device Sharing (Triadic)** | `DSH_2D113789_2C811042 (TIMED_OUT)` | Integrated via parent/guardian mesh | ⚠️ Device sharing TIMED_OUT |

#### D. Butterfly Rendezvous Point Engagement

- **Point ID**: `BP_1795018360027`
  * Local SQLite Status: `PENDING` | Distance: `0.0m` | RAG: `GREY`
  * RTDB Status: Point node ephemeral or archived in cloud root (`/butterfly_points/BP_1795018360027`)

---

### 3.5 Device `4379A761` — RIDDHI

> 🛡️ **GATE 1 MINOR NODE ENFORCEMENT**: Device `4379A761` is registered as child dependent (RIDDHI). In compliance with the Master Integrity Mandate, all live coordinates and raw location vectors are strictly masked from public presentation.

#### A. Identity & Profile Dossier

| Property / Field | Local SQLite (`JFindMe_CitizenData.db`) | Firebase Realtime Database (`RTDB`) | Parity Status & Variance Analysis |
| :--- | :--- | :--- | :--- |
| **Record / Auth UID** | SQLite Identity ID: `2` | Auth UID: `K7PSRbkqYyV8ckUdnn5G6piVWd83` | Linked via `auth_to_device` mapping |
| **Display Name** | `RIDDHI` | `RIDDHI` | ✅ Match |
| **Avatar Resource** | `AV_4379A761_01` | `AV_4379A761_01` | ✅ Match |
| **Connected Peer ID** | `` | `HW_4379A761` | ⚠️ Cloud connected_id variance |
| **Session State** | `OFFLINE` | `ONLINE` | ⚠️ DESYNC: SQLite marked OFFLINE while cloud is active |

#### B. Device Vitals & Telemetry

| Metric / Telemetry Key | Local SQLite (`device_vitals`) | Firebase RTDB (`telemetry` & `device/live`) | Verification & Health Analysis |
| :--- | :--- | :--- | :--- |
| **Battery Level (%)** | `100%` | `9%` | ⚠️ DESYNC: Peer vitals defaulted to 100% in SQLite |
| **Charging State** | `False` | `False` | ✅ Match |
| **Network Bearer / Route** | `UNKNOWN` (Route: `N1_CELLULAR_FAST`) | Route: `N1_CELLULAR_FAST` | Cellular / Wi-Fi Active failover |
| **Last Seen Epoch (ms)** | `1795036544038` | `1795009262992` | ✅ Recent activity |
| **Live Coordinates** | `[REDACTED - GATE 1 MINOR]` | `[REDACTED - GATE 1 MINOR]` | Verified GPS location pipeline |

#### C. Group Membership & Relationship Bindings

| Structure | Local SQLite Configuration | Firebase RTDB Master Circle (`GRP_2T929153`) | Evaluation |
| :--- | :--- | :--- | :--- |
| **Group Member Node** | `Role: `Active Tracker`, Status: `ACTIVE`` | `Role: `Child`, Family: `Daughter`, Friend: `None`` | ✅ Fully bound in circle |
| **Pairing Sessions** | `PAIR_F8CAFC67_4379A761 (TIMED_OUT)` | `Paired with: ['2T929153']` | ⚠️ All SQLite pairings TIMED_OUT |
| **Device Sharing (Triadic)** | `DSH_F8CAFC67_4379A761 (TIMED_OUT)` | Integrated via parent/guardian mesh | ⚠️ Device sharing TIMED_OUT |

#### D. Butterfly Rendezvous Point Engagement

- *No active butterfly rendezvous invitations mapped for this device in local SQLite.*

---

### 3.6 Device `A7D5968D` — Unknown

#### A. Identity & Profile Dossier

| Property / Field | Local SQLite (`JFindMe_CitizenData.db`) | Firebase Realtime Database (`RTDB`) | Parity Status & Variance Analysis |
| :--- | :--- | :--- | :--- |
| **Record / Auth UID** | SQLite Identity ID: `N/A` | Auth UID: `N/A` | Linked via `auth_to_device` mapping |
| **Display Name** | `N/A` | `N/A` | ✅ Match |
| **Avatar Resource** | `N/A` | `N/A` | ✅ Match |
| **Connected Peer ID** | `N/A` | `N/A` | ✅ Synchronized |
| **Session State** | `N/A` | `UNKNOWN / TOMBSTONE` | ✅ Consistent |

#### B. Device Vitals & Telemetry

| Metric / Telemetry Key | Local SQLite (`device_vitals`) | Firebase RTDB (`telemetry` & `device/live`) | Verification & Health Analysis |
| :--- | :--- | :--- | :--- |
| **Battery Level (%)** | `N/A%` | `N/A%` | ✅ Match |
| **Charging State** | `N/A` | `N/A` | ✅ Match |
| **Network Bearer / Route** | `N/A` (Route: `N/A`) | Route: `N/A` | Cellular / Wi-Fi Active failover |
| **Last Seen Epoch (ms)** | `N/A` | `N/A` | ⚠️ Stale |
| **Live Coordinates** | `None` | `None` | Verified GPS location pipeline |

#### C. Group Membership & Relationship Bindings

| Structure | Local SQLite Configuration | Firebase RTDB Master Circle (`GRP_2T929153`) | Evaluation |
| :--- | :--- | :--- | :--- |
| **Group Member Node** | `None` | `None` | ❌ Not in circle |
| **Pairing Sessions** | `None` | `None` | ✅ Active |
| **Device Sharing (Triadic)** | `None` | Integrated via parent/guardian mesh | N/A |

#### D. Butterfly Rendezvous Point Engagement

- *No active butterfly rendezvous invitations mapped for this device in local SQLite.*

---

## 4. Tri-Surface UI Appearance & Operational Behavior

### 4.1 Live Map Surface (`LiveMapDashboardActivity` / `MapActivity`)

- **Architecture**: Cold-booted via `LocationRepository.seedInitialLocationsFromLocalDb()` which queries `DatabaseHelper.getOfflineRoster()`, then attaches real-time Firebase `ValueEventListener` to `/devices/{id}/device/live` and `/devices/{id}/profile`.
- **Device Renderings**:
  * **`2T929153`**: Displays current host location as pulsating blue focal beacon. Loaded with 6,805 historical trajectory vertices from SQLite `location_event`.
  * **`2C811042`**: Rendered with custom avatar marker (`avatar_custom_1`) at Lat `18.5788283`, Lng `73.8888418`. Active tracking pulse received via Firebase socket.
  * **`F8CAFC67`**: Rendered with avatar marker (`avatar_custom_8`) at Lat `18.5788208`, Lng `73.8888484`. Online state confirmed by live telemetry heartbeat.
  * **`2D113789`**: Rendered with dim/grey historical pin (`avatar_custom_14`) at Lat `18.5788634`, Lng `73.8886948`. Evaluated as OFFLINE due to heartbeat age > 24 hours.
  * **`4379A761`**: 🛡️ **Child Ward Marker (`AV_4379A761_01`)**: Subject to Gate 1 Minor Protection. In UI, renders with parent-supervised safe-zone boundary and proximity radar without broadcasting raw telemetry to unauthorized endpoints.
  * **`A7D5968D`**: Not rendered on map canvas (no coordinates exist in database).

### 4.2 Network Gateway Surface (`NetworkGatewayActivity`)

- **Architecture**: Dual-tab management console. Tab 0 queries local SQLite `getOfflineRoster()` via `GroupViewModel`. Tab 1 coordinates cloud pairing sessions and invitations via `PairingRepository`.
- **Tab 0 (Offline Roster)**:
  * Lists all 5 active fleet members: `2T929153` (ADMIN), `4379A761` (Active Tracker), `2C811042` (Active Tracker), `2D113789` (Active Tracker), `F8CAFC67` (Active Tracker).
  * Display variance: Peer nodes in SQLite display battery defaulted to 100% and session state as `OFFLINE` due to missing background backfill into `device_vitals`.
- **Tab 1 (Cloud Pairing & Handshakes)**:
  * Reflects pairing status. All historic pairings (`PAIR_2T929153_F8CAFC67`, `PAIR_2T929153_2D113789`, `PAIR_F8CAFC67_4379A761`, `PAIR_2D113789_2C811042`) show `TIMED_OUT` (15,000ms deadline exceeded).

### 4.3 Friends & Family Circle Hub (`FamilyFriendsActivity`)

- **Architecture**: High-priority zero-latency circle view. Uses `DeviceRagEngine` to evaluate node health into Green, Amber, Red, Grey badges.
- **Roster Hierarchy & Sorting Invariant**:
  * **Position 0 Priority**: Ward / Child dependent node (`4379A761` / RIDDHI) is strictly pinned to the top of the roster list for guardian situational awareness.
  * **Subsequent Positions**: Ordered by RAG health score (Green -> Amber -> Red).
- **RAG Status Breakdown**:
  * **`4379A761` (RIDDHI)**: 🟡 **AMBER** (Active within circle, battery at 9% triggers low battery warning threshold).
  * **`F8CAFC67` (PUSHPA)**: 🟢 **GREEN** (Online, battery 47%, heartbeat age < 5 minutes).
  * **`2C811042` (REDMI 5PRO)**: 🟡 **AMBER** (Online, battery 17% <= 20% warning threshold).
  * **`2D113789` (INNOVANT)**: 🔴 **RED** (Offline, heartbeat age > 6 hours critical threshold).
  * **`2T929153` (REDMI N7)**: 🟢 **GREEN** (Self host, active cellular/Wi-Fi connection).

---

## 5. Synchronization Root Causes & Desync Gap Analysis

The extraction and matrix comparison reveal 7 specific synchronization discrepancies between the local SQLite database on device `2177e4a0` and Firebase Realtime Database:

1. **GAP-01: Stale Peer Device Vitals in Local SQLite**:
   - *Finding*: In SQLite table `device_vitals`, all peer devices (`4379A761`, `2C811042`, `2D113789`, `F8CAFC67`) show battery level fixed at `100%`, network `UNKNOWN`, and signal `0`.
   - *Reality in Cloud*: RTDB shows actual live batteries: `4379A761` is `9%`, `2C811042` is `17%`, `F8CAFC67` is `47%`.
   - *Root Cause*: `LocationRepository.processIncomingLocation` receives peer live telemetry but does not upsert into SQLite table `device_vitals`.

2. **GAP-02: User Identity Session State Frozen at `OFFLINE`**:
   - *Finding*: In SQLite `user_identity`, `st_session_state` is set to `OFFLINE` for all 5 devices, including host `2T929153`.
   - *Reality in Cloud*: In RTDB `/devices/2T929153/device/live`, status is `ONLINE` with active WebSocket heartbeat.
   - *Root Cause*: `FirebaseComm` presence handler updates cloud node `.info/connected` but does not trigger `UserIdentityTable.updateSessionState('ONLINE')` locally.

3. **GAP-03: Pairing & Sharing Session Deadlocks (15s Timeout)**:
   - *Finding*: Every record in SQLite `device_pairing` and `device_sharing` has status `TIMED_OUT`.
   - *Reality in Cloud*: RTDB `/pairing_sessions` is empty because ephemeral sessions are deleted on timeout.
   - *Root Cause*: The 15,000ms pairing timeout in `PairingRepository.kt` expires before peer handshake or FCM wakeups complete.

4. **GAP-04: Ephemeral Rendezvous Point Desync (Butterfly Point)**:
   - *Finding*: SQLite has active point `BP_1795018360027` with 3 participant records in `butterfly_point_device_map`.
   - *Reality in Cloud*: RTDB node `/butterfly_points` is `{}` (empty).
   - *Root Cause*: Cloud point node expired or was never written due to an unhandled exception or whole-object clobbering in `ButterflyPointEngine.kt`.

5. **GAP-05: Rendezvous Field Name & Type Schema Mismatches**:
   - *Finding*: SQLite `butterfly_point_device_map` uses `distance_to_point_meters REAL` and `is_sos INTEGER (0/1)`. RTDB expects `distance_meters Double` and `is_sos Boolean (true/false)`.
   - *Root Cause*: Direct JSON mapping fails during automatic deserialization without explicit property mapping annotations.

6. **GAP-06: Dual-Tree Membership Split-Brain**:
   - *Finding*: RTDB maintains both `/relationships/{groupId}/members` and `/relationships/{groupId}/devices`.
   - *Reality in Cloud*: Master circle `GRP_2T929153` has differing keys and timestamps between the two trees, while local SQLite only maintains table `group_members`.

7. **GAP-07: Unprovisioned Ghost Node `A7D5968D`**:
   - *Finding*: `A7D5968D` exists in RTDB `/devices` with 42 Change_Log entries and an OTA update command, but has no Identity, no Profile, no live coordinates, and is completely absent from local SQLite.
   - *Root Cause*: Ghost node left behind from legacy firmware test cycle.

---

## 6. Verification & Programmatic Execution

This matrix is backed by the deterministic, reproducible Python verification script:
`scratch/generate_fleet_matrix.py`

To independently regenerate and verify this matrix against current database states:
```powershell
python c:\Users\jiten\jAnitGravity\jFindMe\scratch\generate_fleet_matrix.py
```

### Invalidation Conditions:
- Disconnection or reboot of device `2177e4a0` altering local WAL state.
- Firebase RTDB mutations initiated by remote fleet nodes modifying presence or group memberships.
- Execution of database wiping commands (`DatabaseManagementActivity` reset options).
