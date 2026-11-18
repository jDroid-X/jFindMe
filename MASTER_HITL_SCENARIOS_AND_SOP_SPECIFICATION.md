# 🛡️ Enterprise Master Specification: Butterfly Point Rendezvous Hub & Human-in-the-Loop (HITL) SOP
> **Codename:** `jDroid-X` | **Lead Operator:** `JEET`  
> **System Architecture:** Dynamic Closed-Loop OOPS MVC | Coordinate Tree `(ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)`  
> **Target Subsystem:** Butterfly Point Rendezvous Hub (`:jButterFly` Single Canonical Module SSOT)  
> **Database Model:** 3-Tier Architecture (Master SSOT ⟷ Fact/Sub-Master Roster ⟷ Transaction Telemetry Stream)  
> **Standard:** Antigravity AI Enterprise Development Standard (V5.0)  
> **Build Target:** Android Native (API 26–34) & Firebase Realtime Database (RTDB)

---

## 📑 Table of Contents
1. [Executive Overview & Architectural Invariants](#1-executive-overview--architectural-invariants)
2. [End-to-End Chronological Operational Workflow (Phases 1 to 13)](#2-end-to-end-chronological-operational-workflow-phases-1-to-13)
   - [Phase 1: Initiation & Entry Trigger](#phase-1-initiation--entry-trigger)
   - [Phase 2: Multi-Modal Location Identification (Unified Spatial Selector)](#phase-2-multi-modal-location-identification-unified-spatial-selector)
   - [Phase 3: Temporal Scheduling, Availability Horizon & Arrival Perimeter](#phase-3-temporal-scheduling-availability-horizon--arrival-perimeter)
   - [Phase 4: Connected Device Multi-Selection & Circle Ingress Governance](#phase-4-connected-device-multi-selection--circle-ingress-governance)
   - [Phase 5: Pre-Flight Confirmation Modal & Distant Rendezvous Safeguard](#phase-5-pre-flight-confirmation-modal--distant-rendezvous-safeguard)
   - [Phase 6: Dual-Engine Atomic Dispatch](#phase-6-dual-engine-atomic-dispatch)
   - [Phase 7: Recipient Ingress & Heads-Up Alert (Doze-Bypass Ingress)](#phase-7-recipient-ingress--heads-up-alert-doze-bypass-ingress)
   - [Phase 8: Human-in-the-Loop Decision SOP (Recipient Response)](#phase-8-human-in-the-loop-decision-sop-recipient-response)
   - [Phase 9: Active Tracking, Dynamic Origin-Shift & Closed-Loop Telemetry](#phase-9-active-tracking-dynamic-origin-shift--closed-loop-telemetry)
   - [Phase 10: Quorum Assembly Cascade](#phase-10-quorum-assembly-cascade)
   - [Phase 11: Human-in-the-Loop Operator Override Actions](#phase-11-human-in-the-loop-operator-override-actions)
   - [Phase 12: Tactical RAG Health Monitoring Engine Matrix](#phase-12-tactical-rag-health-monitoring-engine-matrix)
   - [Phase 13: 7-Day Max Availability, Auto-Sunset & Expiry Lifecycle](#phase-13-7-day-max-availability-auto-sunset--expiry-lifecycle)
3. [Runtime Operational Failure Scenarios & Edge Cases](#3-runtime-operational-failure-scenarios--edge-cases)
4. [Operator HITL Quick-Reference Action Matrix](#4-operator-hitl-quick-reference-action-matrix)
5. [Verification & Attestation Audit](#5-verification--attestation-audit)

---

## 1. Executive Overview & Architectural Invariants

The **Butterfly Point Rendezvous Hub** is a tactical multi-device coordination and rendezvous subsystem within `jFindMe` / `jButterFly`. It allows any authenticated device (Device A) to designate a high-precision spatial anchor (latitude, longitude, formatted address, and arrival radius) on a live map, schedule a scheduled meeting timestamp, and assign multiple connected fleet/circle devices (Devices B, C, E... X).

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                     BUTTERFLY POINT RENDEZVOUS HUB — ARCHITECTURAL BLUEPRINT                     │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                      [ Device A (Organizer) ]
                                                 │
                                                 ▼
                                     [ Unified Spatial Studio ]
                                                 │
                         ┌───────────────────────┴───────────────────────┐
                         ▼                                               ▼
            [ Local SQLite (3-Tier) ]                       [ Firebase RTDB (SSOT) ]
            Tier 1: butterfly_point                         /butterfly_points/{pointId}
            Tier 2: butterfly_point_device_map              /butterfly_points/{pointId}/devices
            Tier 3: butterfly_point_telemetry               /butterfly_points/{pointId}/telemetry
                         │                                               │
                         └───────────────────────┬───────────────────────┘
                                                 │ Broadcast Ingress
                                                 ▼
                               [ Devices B, C, E... X (Participants) ]
                                 TrackingService (Doze Bypass)
                                 AlertManager Interactive Notification
                                                 │
                                       ┌─────────┴─────────┐
                                       ▼                   ▼
                                   [ ACCEPT ]          [ REJECT ]
                                       │                   │
                                       ▼                   ▼
                           [ Live Origin-Shift Tracking ] [ Red Badge ]
                             Haversine Distance & ETA
                             15m Deadband Hysteresis
                             Tactical RAG Health Evaluator
                                       │
                                       ▼
                             [ Quorum Assembly ]
                             (All Eligible Reached >= 2)
```

### Core Architectural Invariants:
1. **Single Canonical Module Architecture (`:jButterFly` SSOT)**: All algorithms, data tables, dialogs, receivers, and utilities reside in the single authoritative module `jButterFly/src/main/` with legacy `app/` safely archived.
2. **3-Tier Master-Fact-Transaction Data Model**:
   - **Tier 1 (Master SSOT)**: `butterfly_point` (SQLite) ⟷ `/butterfly_points/{pointId}` (Cloud RTDB).
   - **Tier 2 (Sub-Master / Fact Roster)**: `butterfly_point_device_map` (SQLite) ⟷ `/butterfly_points/{pointId}/devices/{deviceId}` & `/devices/{deviceId}/butterfly_invitations/{pointId}`.
   - **Tier 3 (Transaction Telemetry Stream)**: `butterfly_point_telemetry` (SQLite) ⟷ `/butterfly_points/{pointId}/telemetry/{deviceId}`.
3. **Dynamic Coordinate Origin Shift**: In traditional tracking, coordinates are relative to $(0,0)$ or peer-to-peer. When a Butterfly Point is active, spatial calculations shift their origin to $(Lat_{BP}, Lon_{BP})$, yielding real-time distance-to-go, arrival vector lines, and ETA to target.
4. **Hysteresis Deadband (15 Meters)**: To prevent edge oscillation ("flapping") between `ON_MOVE` and `REACHED` when a device hovers near the perimeter, state transitions enforce a 15-meter deadband buffer ($R_{exit} = R_{arrival} + 15\text{m}$).
5. **7-Day Maximum Availability Horizon**: No rendezvous can be scheduled or retained beyond 7 days ($7 \times 24\text{ hours}$) from creation time (`DEFAULT_BUTTERFLY_MAX_AVAILABILITY_DAYS = 7`).
6. **500km Distant Rendezvous Safeguard**: Setting a meeting point $> 500\text{km}$ from the organizer's current location triggers an explicit Human-in-the-Loop confirmation modal to prevent accidental long-distance errors.
7. **Quorum Assembly Condition**: When all non-declined, non-revoked eligible participants ($\ge 2$ devices) reach the rendezvous boundary, the engine automatically latches and announces Quorum Assembly across the fleet.

---

## 2. End-to-End Chronological Operational Workflow (Phases 1 to 13)

### Phase 1: Initiation & Entry Trigger
* **Primary Actor**: Operator A (Initiator / Device A).
* **System Component**: `MapActivity.java` (Lines 463–473, 1703–1717, 3135–3139).
* **Preconditions**:
  1. Device A is authenticated and currently on the active Google Map viewport.
  2. Google Play Services and Fused Location Provider are operational.
* **Trigger Mechanisms**:
  - **Dock Action (Ghost Icon)**: In `MapActivity.java:465`, tapping the Butterfly Ghost Icon (`ACTION_BUTTERFLY_POINT`) on the right floating action dock fires `ButterflyPointDialog.show(this)`.
  - **Avatar Context Menu Action**: In `MapActivity.java:1703`, long-pressing or tapping a peer avatar on the map canvas opens the `UiArchitect` action sheet with option `"🦋 Butterfly Point (Rendezvous & Proximity)"`, invoking `ButterflyPointDialog.showForTarget(activity, targetUid, targetName, targetLat, targetLng)`.
  - **Marker Tap Action**: In `MapActivity.java:3135`, tapping an existing `BUTTERFLY_POINT` marker on the map opens the Active Points sheet.
* **System Execution Sequence**:
  1. `ButterflyPointDialog.show(activity)` queries `dbHelper.getActiveButterflyPoints()` via readable SQLite database.
  2. If active points exist ($\text{count} \ge 1$): Renders `showActivePointsSheet` displaying expandable cards with live RAG badges, distance-to-go, and participant status chips.
  3. If zero active points exist ($\text{count} == 0$): Renders `showNoActivePointsActionSheet` offering:
     - `[ Set New Butterfly Point ]` ➔ Opens Studio Rendezvous Builder (`showCreateButterflyPointStudio`).
     - `[ Toggle Privacy Ghost Mode ]` ➔ Engages stealth mode.
* **Postconditions**: Studio builder dialog or active rendezvous cardview is presented to Operator A on the UI main thread.

---

### Phase 2: Multi-Modal Location Identification (Unified Spatial Selector)
* **Primary Actor**: Operator A.
* **System Component**: `ButterflyPointDialog.java` (Lines 606–765).
* **Preconditions**: Studio Rendezvous Builder is visible on screen.
* **Modal Options & Spatial Resolution**:
  The dialog provides 5 discrete methods for anchor coordinate selection:
  1. **Text Geocoding Search (`etSearch`)**:
     - Operator enters an address, street, landmark, or venue name.
     - Tapping the search icon executes an asynchronous background worker querying Android `Geocoder.getFromLocationName(query, 1)`.
     - Upon resolution, populates `selectedLat`, `selectedLng`, and sets the formatted address in the UI card.
  2. **📍 My GPS Fix**:
     - Reads current coordinates from `MapViewModel.myLocation` (or `LocationRepository`).
     - Snaps anchor directly to Device A's current real-time GPS position.
  3. **🛡️ SafeZone Preset**:
     - Queries local SQLite `SafeZoneTable` via `dbHelper.getAllSafeZones()`.
     - Operator selects from saved SafeZones (e.g., "Home", "Office", "School", "Base Camp").
     - Coordinates and SafeZone label populate the rendezvous point.
  4. **👥 Peer Device GPS Fix**:
     - Reads peer coordinates from `MapViewModel.getUserLocations()`.
     - Operator taps a connected contact; point anchors to peer's latest reported coordinates.
  5. **🗺️ Map Center & Interactive Crosshair Dropper**:
     - Method 5A: Reads camera target coordinates (`googleMap.getCameraPosition().target`).
     - Method 5B: Dismisses dialog temporarily into interactive map crosshair dropper mode (`activity.engageMapPointPicker(...)`). Operator pans/zooms map to target location and taps `[SET AS BUTTERFLY POINT]`. Callback returns `LatLng` and re-opens dialog.
* **Input Validations & Error Handling**:
  - If coordinates evaluate to $(0.0, 0.0)$, dispatch is rejected with error: *"Please select a valid map location."*
  - Point Name is mandatory: `pointName.trim().isEmpty()` displays error *"Meeting name cannot be blank."*

---

### Phase 3: Temporal Scheduling, Availability Horizon & Arrival Perimeter
* **Primary Actor**: Operator A.
* **System Component**: `ButterflyPointDialog.java` (Lines 775–868), `RegistryManager.kt` (Lines 243–262).
* **Temporal Configuration**:
  1. **Date & Time Picker**:
     - Tap on `btnSelectDate` opens `DatePickerDialog` initialized to current calendar date.
     - Tap on `btnSelectTime` opens `TimePickerDialog` initialized to current wall-clock time $+ 1\text{ hour}$.
  2. **Scheduling Validations**:
     - **Past Schedule Rejection**: If `scheduleCal.getTimeInMillis() <= System.currentTimeMillis()`, error displayed: *"Meeting time must be in the future."*
     - **7-Day Maximum Availability Ceiling**:
       `val maxDays = RegistryManager.getButterflyMaxAvailabilityDays(context)` (Default: 7 days).
       `val maxCeilingTs = System.currentTimeMillis() + (maxDays * 24L * 3600L * 1000L)`.
       If `scheduleCal.getTimeInMillis() > maxCeilingTs`, the timestamp is clamped to `maxCeilingTs` and operator alerted: *"Maximum scheduling horizon is 7 days."*
  3. **Auto-Expiry Setting (`DEFAULT_BUTTERFLY_EXPIRY_HOURS = 4`)**:
     - Toggle `Auto-expire after meeting` sets `expiryTimestamp = rendezvousTimestamp + (expiryHours * 3600000L)`.
     - Clamped so that `expiryTimestamp` cannot exceed `maxCeilingTs`.
  4. **Arrival Radius Perimeter (`DEFAULT_BUTTERFLY_ARRIVAL_RADIUS = 50.0f`)**:
     - Operator selects arrival perimeter radius via preset chips or custom input:
       * `25m (Precision)`: Tight indoor/building entrance rendezvous.
       * `50m (Standard)`: Default street-corner / parking lot rendezvous.
       * `100m (Neighborhood)`: Plaza, park, or campus rendezvous.
       * `250m (Staging)`: Highway interchange or staging perimeter.
       * `Custom (10m–5000m)`: Free-form entry.
* **Postconditions**: `rendezvousTimestamp`, `expiryTimestamp`, and `arrivalRadiusMeters` validated and stored in session state.

---

### Phase 4: Connected Device Multi-Selection & Circle Ingress Governance
* **Primary Actor**: Operator A.
* **System Component**: `ButterflyPointDialog.java` (Lines 870–983), `DatabaseHelper.kt`.
* **Roster Population & Circle Hierarchy**:
  1. **Connected Devices Discovery**:
     - Queries verified connected devices from SQLite: `dbHelper.getConnectedDeviceIds(selfDevId)`, paired devices, and circle contacts.
     - **Self-Device Exclusion**: Operator's own hardware ID (`selfDevId = DeviceIdentityHelper.getDeviceId(context)`) is strictly filtered out so the creator cannot invite themselves as a target.
  2. **Circle / Group Organization**:
     - Devices are grouped hierarchically using `dbHelper.getDeviceGroupInfo(devId)` (e.g., `[FAMILY]`, `[WORK]`, `[TEAM]`).
     - Each entry renders: Checkbox, Contact Alias / Name, Hardware Model, Online/Offline badge.
  3. **Batch Selection Controls**:
     - "Select All" checkbox toggles all devices in list simultaneously.
     - Per-group toggle checks/unchecks members of that specific circle.
* **Validation Rule**:
  - `selectedDeviceSet.isEmpty()` disables dispatch button and shows toast: *"Select at least 1 connected device."*

---

### Phase 5: Pre-Flight Confirmation Modal & Distant Rendezvous Safeguard
* **Primary Actor**: Operator A.
* **System Component**: `ButterflyPointDialog.java` (Lines 1000–1047), `SpatialMathUtil.kt`.
* **Execution Sequence**:
  1. **Distant Rendezvous Safeguard Check**:
     - Computes Haversine distance from Device A's current GPS location to target coordinates:
       $D_{dist} = \text{SpatialMathUtil.computeDistanceBetween}(Lat_{A}, Lon_{A}, Lat_{BP}, Lon_{BP})$.
     - Threshold: `DEFAULT_BUTTERFLY_MAX_DISTANCE_KM = 500.0f`.
     - **Trigger Condition**: If $D_{dist} > 500,000\text{ meters}$:
       - Halts execution and presents a high-priority warning dialog:
         > **⚠️ Distant Rendezvous Warning**  
         > *"The selected rendezvous location is [X] km away from your current location, which exceeds the 500 km operating safeguard. Are you sure you want to schedule this meeting?"*  
         > Actions: `[ PROCEED ]` | `[ REVISE LOCATION ]`
  2. **Pre-Flight Summary Modal**:
     - Summarizes rendezvous parameters: Meeting Name, Formatted Address, Anchor Coordinates, Scheduled Date & Time, Arrival Radius (m), and Participating Devices Count ($N$).
     - Operator taps `[ CONFIRM & DISPATCH ]`.

---

### Phase 6: Dual-Engine Atomic Dispatch
* **Primary Actor**: System Engine (Device A).
* **System Component**: `ButterflyPointEngine.kt` (Lines 265–438), `DatabaseHelper.kt`.
* **Persistence & Synchronization Pipeline**:
  Upon confirmation, `ButterflyPointEngine.dispatchButterflyPoint` executes the atomic dual-engine commit:
  1. **Identifier Generation**: Generates deterministic unique ID: `pointId = "BP_" + System.currentTimeMillis()`.
  2. **Tier 1 (Master SSOT) Local Write**:
     - Inserts record into SQLite `butterfly_point`:
       `point_id`, `creator_device_id`, `creator_identity_id`, `point_name`, `address`, `lat`, `lng`, `geohash`, `rendezvous_timestamp`, `expiry_timestamp`, `arrival_radius_meters`, `notes`, `icon_type`, `is_active = 1`, `is_archived = 0`.
  3. **Tier 2 (Sub-Master Roster) Local Write**:
     - Inserts Creator Device A into `butterfly_point_device_map` with `status = 'ACCEPTED'`, `distance_to_point_meters`, and `eta_seconds`.
     - Inserts each target device (B, C, E... X) into `butterfly_point_device_map` with `status = 'PENDING'`, `invited_by = selfDevId`.
  4. **Audit Logging (Fact & Transaction DB)**:
     - Inserts into `change_log`: `category = 'Safehouse'`, `action = 'CREATE'`, `details = "Butterfly Point created: [Name]"`.
     - Inserts into `alert_history`: `alert_type = 'BUTTERFLY_RENDEZVOUS'`.
     - Inserts into `sys_log`: forensic event timestamp.
  5. **Cloud SSOT Broadcast (Firebase RTDB)**:
     - Asynchronously writes payload to `/butterfly_points/{pointId}`:
       ```json
       {
         "point_id": "BP_1731910000000",
         "creator_device_id": "2T929153",
         "creator_name": "JEET",
         "point_name": "Fleet Staging Area",
         "address": "Tingre Nagar, Pune",
         "lat": 18.5729,
         "lng": 73.8785,
         "geohash": "tek3z4",
         "rendezvous_timestamp": 1731913600000,
         "expiry_timestamp": 1731928000000,
         "arrival_radius_meters": 50.0,
         "is_active": true,
         "is_archived": false,
         "devices": {
           "2T929153": { "device_id": "2T929153", "status": "ACCEPTED", "rag_status": "GREEN" },
           "2C811042": { "device_id": "2C811042", "status": "PENDING", "rag_status": "GREY" },
           "2D113789": { "device_id": "2D113789", "status": "PENDING", "rag_status": "GREY" }
         }
       }
       ```
  6. **Recipient Ingress Inbox Dispatches**:
     - Pushes invitation to `/devices/{deviceId}/butterfly_invitations/{pointId}` for each assigned peer device.
  7. **Real-Time Peer Sync Listener**:
     - Attaches `startActivePointPeerSync(pointId)` immediately on Device A, establishing a `< 250ms` real-time channel for peer updates.
  8. **Tactical Viewport Lock**:
     - `MapActivity.focusOnButterflyPoint(point, true)` animates camera bounds enclosing Butterfly Point and all participating devices with 80dp padding.

---

### Phase 7: Recipient Ingress & Heads-Up Alert (Doze-Bypass Ingress)
* **Primary Actor**: System Engine (Devices B, C, E... X).
* **System Component**: `TrackingService.kt` (Lines 766–885), `AlertManager.kt` (Lines 368–440).
* **Execution Sequence on Participant Devices**:
  1. **Doze-Bypassing Foreground Listener**:
     - Participant runs `TrackingService` as an active foreground service (`startForeground()`) with `PARTIAL_WAKE_LOCK`.
     - `butterflyChildListener` on `/devices/{localAgentId}/butterfly_invitations` triggers `onChildAdded`.
  2. **Pre-Alert Validation Gates**:
     - **Gate 7A: Stale Invitation Guard**: Checks if `currentTime > expiryTimestamp` or `currentTime > (rendezvousTimestamp + overdueGraceMillis)`. If stale, marks status `STATUS_TIMED_OUT`, persists as archived in SQLite, and suppresses notification alert.
     - **Gate 7B: Creator Self-Alert Guard**: If `creatorDeviceId == localAgentId`, suppresses notification and attaches peer sync silently.
     - **Gate 7C: Duplicate Guard**: If already recorded in local SQLite with status `ACCEPTED` or `REJECTED`, suppresses alert.
  3. **Local SQLite Ingestion**:
     - Writes Master record into SQLite `butterfly_point`.
     - Writes Sub-Master record into SQLite `butterfly_point_device_map` (`status = 'PENDING'`).
  4. **High-Importance Interactive Notification Alert**:
     - Dispatches via `AlertManager.showButterflyInvitationNotification` on channel `butterfly_point_invitations` (`IMPORTANCE_HIGH`, Magenta LED flash, haptic vibration):
       * **Title**: `🦋 Butterfly Point Rendezvous: [Point Name]`
       * **Body**: `Invited by [Creator Name] for [Date & Time] at [Address]`
       * **Action Button 1**: `[ ACCEPT ]` (Emerald Green) ➔ `ACTION_BUTTERFLY_ACCEPT`
       * **Action Button 2**: `[ REJECT ]` (Crimson Red) ➔ `ACTION_BUTTERFLY_REJECT`
  5. **In-App Notification**:
     - Broadcasts `ACTION_MASTER_DATA_UPDATED` with category `BUTTERFLY_POINT` to refresh `MapActivity` if active in foreground.

---

### Phase 8: Human-in-the-Loop Decision SOP (Recipient Response)
* **Primary Actor**: Operator B (Recipient).
* **System Component**: `ButterflyPointActionReceiver.kt` (Lines 22–78), `ButterflyPointEngine.kt` (Lines 445–540).

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                             RECIPIENT DECISION SOP FLOWCHART                                     │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                               [ Incoming Invitation Alert ]
                                             │
                       ┌─────────────────────┴─────────────────────┐
                       ▼                                           ▼
             Operator Taps [ACCEPT]                      Operator Taps [REJECT]
                       │                                           │
         Cancel Notification Alert                   Cancel Notification Alert
         Update SQLite: status='ACCEPTED'            Prompt Reason (if dialog):
         Update RTDB: status='ACCEPTED'              "Busy", "Too Far", "Declined"
         Start Peer Sync Listener                    Update SQLite: status='REJECTED'
         Trigger Instant GPS Ping                    Update RTDB: status='REJECTED'
                       │                                           │
                       ▼                                           ▼
           [ Status: ACCEPTED / ON_MOVE ]                 [ Status: REJECTED ]
           Creator Device sees Cyan Line               Creator Device sees Red Badge
```

#### Scenario 8A: Recipient Accepts Invitation
1. Recipient taps `[ ACCEPT ]` in notification shade or clicks card in `ButterflyPointDialog`.
2. `ButterflyPointActionReceiver` cancels notification alert.
3. Updates local SQLite `butterfly_point_device_map.status = 'ACCEPTED'`.
4. Pushes status `ACCEPTED` to Firebase RTDB:
   - Path 1: `/butterfly_points/{pointId}/devices/{deviceId}/status = 'ACCEPTED'`.
   - Path 2: `/devices/{deviceId}/butterfly_invitations/{pointId}/status = 'ACCEPTED'`.
5. Starts `ButterflyPointEngine.startActivePointPeerSync(pointId)` on Device B.
6. Calls `ButterflyPointEngine.processLocalLocationUpdate(context, lat, lng, speed)` immediately, transmitting current distance and ETA to Creator Device A.

#### Scenario 8B: Recipient Declines Invitation
1. Recipient taps `[ REJECT ]` in notification shade or selects "Decline" in dialog.
2. If declined via dialog, operator can choose rejection reason:
   - *"Cannot attend / Prior engagement"*
   - *"Too far away / Transport unavailable"*
   - *"Working / Busy"*
   - *"Custom note..."*
3. If declined via notification button, defaults to *"Declined via quick notification"*.
4. Updates SQLite `butterfly_point_device_map.status = 'REJECTED'`, `rejection_reason = [reason]`.
5. Pushes status `REJECTED` and reason to Firebase RTDB.
6. Creator Device A's active dashboard card receives event; device badge turns Red with caption *"Declined: [Reason]"*.

---

### Phase 9: Active Tracking, Dynamic Origin-Shift & Closed-Loop Telemetry
* **Primary Actor**: System Engine (All Participating Devices).
* **System Component**: `TrackingService.kt` (Lines 1193–1196), `ButterflyPointEngine.kt` (Lines 1060–1189), `MapActivity.java` (Lines 3928–3991).
* **Continuous Runtime Execution Loop**:
  1. **GPS Telemetry Ingestion**:
     - On every location fix from `FusedLocationProviderClient`, `TrackingService` calls:
       `ButterflyPointEngine.processLocalLocationUpdate(context, location.latitude, location.longitude, location.speed * 3.6f)`.
  2. **Dynamic Origin-Shift Calculation**:
     - Coordinates origin is translated to rendezvous center $(Lat_{BP}, Lon_{BP})$.
     - Haversine distance $D$ computed via `SpatialMathUtil.computeDistanceBetween(...)`.
     - ETA computed: $ETA = \frac{D}{\max(\text{speed}_{kmh}, 25.0) \times \frac{1000}{60}}\text{ minutes}$.
  3. **Dynamic Arrival Boundary & 15m Deadband Hysteresis**:
     ```kotlin
     val currentStatus = existingMapEntry.status
     val isReached = if (currentStatus == STATUS_REACHED) {
         // Latching deadband: Must move outside arrivalRadius + hysteresis to revert
         distanceMeters <= (arrivalRadiusMeters + HYSTERESIS_DEADBAND_METERS) // 50m + 15m = 65m
     } else {
         // Entering boundary
         distanceMeters <= arrivalRadiusMeters // 50m
     }
     val newStatus = if (isReached) STATUS_REACHED else STATUS_ON_MOVE
     ```
  4. **Arrival Latching**:
     - When transitioning from `STATUS_ON_MOVE` to `STATUS_REACHED`, `arrival_timestamp` is latched with `System.currentTimeMillis()`.
  5. **Tactical RAG Health Evaluation**:
     - Evaluates current participant health using `evaluateButterflyRag` (Section 12).
  6. **Multi-Tier Persistence Write**:
     - **Sub-Master SQLite**: Updates `butterfly_point_device_map` (`distance_to_point_meters`, `eta_seconds`, `battery_pct`, `rag_status`, `rag_reason`, `last_lat`, `last_lng`).
     - **Transaction SQLite**: Appends snapshot row to `butterfly_point_telemetry`.
     - **Firebase RTDB**: Overwrites latest telemetry at `/butterfly_points/{pointId}/devices/{deviceId}` and `/telemetry/{deviceId}`.
  7. **Tactical Vector Overlay Rendering (`MapActivity`)**:
     - **Anchor Center**: Azure Blue marker with Butterfly icon.
     - **Arrival Perimeter Circle**: Emerald green translucent fill (radius: 50m, stroke: 2dp).
     - **Dynamic Spiderweb Vector Lines**: Renders polylines connecting each participating device to the center anchor:
       * **Cyan Line (`#00E5FF`)**: Device on track, RAG = GREEN.
       * **Amber Line (`#FFB300`)**: Device delayed or low battery, RAG = AMBER.
       * **Red Line (`#FF1744`)**: Device in distress, overdue, or RAG = RED.
     - **Viewport Framing**: Dynamically bounds camera to encompass the center anchor and all active participant devices with 80dp padding.

---

### Phase 10: Quorum Assembly Cascade
* **Primary Actor**: System Engine (All Participating Devices).
* **System Component**: `ButterflyPointEngine.kt` (Lines 636–648), `AlertManager.kt` (Lines 605–630).
* **Evaluation Logic**:
  Evaluated in real-time inside `startActivePointPeerSync`'s `ValueEventListener` on every peer status change:
  ```kotlin
  val allDevices = dbHelper.getButterflyDevicesForPoint(pointId)
  val eligibleDevices = allDevices.filter {
      !it.status.equals(ButterflyPointDeviceMapTable.STATUS_REJECTED, ignoreCase = true) &&
      !it.status.equals(ButterflyPointDeviceMapTable.STATUS_REVOKED, ignoreCase = true) &&
      !it.status.equals(ButterflyPointDeviceMapTable.STATUS_TIMED_OUT, ignoreCase = true)
  }
  if (eligibleDevices.size >= 2 && eligibleDevices.all { it.status == ButterflyPointDeviceMapTable.STATUS_REACHED }) {
      if (!quorumAlertedPoints.contains(pointId)) {
          quorumAlertedPoints.add(pointId)
          val ptName = activeButterflyPoint?.pointName ?: "Butterfly Point"
          AlertManager.showButterflyQuorumNotification(context, ptName)
      }
  }
  ```
* **Execution Cascade**:
  1. **Idempotent Sentinel**: `quorumAlertedPoints.add(pointId)` guarantees the quorum cascade fires exactly once per point.
  2. **High-Priority Quorum Alert**:
     - Dispatches `AlertManager.showButterflyQuorumNotification` to all participants:
       * Title: `🏆 Butterfly Point Quorum Reached!`
       * Body: `All [N] participating devices have arrived at [Point Name]!`
       * LED: Emerald Green | Vibration: Success pulse cadence.
  3. **Tactical Viewport Announcement**:
     - Live map displays a prominent slide notification banner: *"QUORUM ASSEMBLED: 100% of participants have reached the rendezvous!"*.
     - Vector spiderweb lines collapse into compact arrival rings around the anchor.

---

### Phase 11: Human-in-the-Loop Operator Override Actions
* **Primary Actor**: Organizer or Authorized Participant.
* **System Component**: `ButterflyPointDialog.java` (Lines 377–540, 1169–1322), `ButterflyPointEngine.kt` (Lines 863–1053).

The system provides 8 distinct Human-in-the-Loop operational overrides:

| # | HITL Override Action | Target Component | Trigger & Parameter | System Execution Sequence |
|:---:|:---|:---|:---|:---|
| **1** | **Focus on Live Map** | `MapActivity.java:3928` | Tap on participant row in active points sheet | Pans and zooms map camera to Zoom 17.0f centered on target device; flashes selection highlight on its vector line. |
| **2** | **Nudge / Poke Node** | `ButterflyPointEngine.kt:863` | Organizer taps "Nudge / Poke" on delayed or non-responsive peer | Dispatches high-priority command `POKE_LOCATION_UPDATE` to `/devices/{targetId}/commands/poke`. Target device vibrates and prompts immediate telemetry refresh. |
| **3** | **Extend Rendezvous Time** | `ButterflyPointDialog.java:1169` | Organizer selects `+15m`, `+30m`, `+45m`, or `+1h` | Atomic transaction updates SQLite `butterfly_point` and RTDB `/butterfly_points/{id}`. Propagates `onChildChanged` to all peers with alert: *"Rendezvous Extended by +Xm"*. |
| **4** | **Re-Invite Node** | `ButterflyPointEngine.kt:1024` | Organizer selects "Re-Invite" on a `DECLINED` or `TIMED_OUT` peer | Resets SQLite and RTDB status to `STATUS_PENDING`. Re-dispatches invitation to peer inbox and posts notification alert. |
| **5** | **Revoke Invitation** | `ButterflyPointEngine.kt:993` | Organizer selects "Revoke" on an invited peer | Sets status to `STATUS_REVOKED` in SQLite and RTDB. Removes peer from vector mesh. Deletes invitation from peer inbox. |
| **6** | **Invite More Peers (Delegated Mesh)** | `ButterflyPointDialog.java:1195` | Organizer or accepted peer taps "Invite More Peers" | Opens checklist of connected circle devices not yet participating. Batch-inserts new peers as `STATUS_PENDING` and syncs into active rendezvous. |
| **7** | **Turn-by-Turn Navigation** | `ButterflyPointDialog.java:470` | Operator taps "Navigate" on active point card | Launches external navigation intent `google.navigation:q=lat,lng&mode=d` to guide operator directly to Butterfly Point coordinates. |
| **8** | **End / Archive Rendezvous** | `ButterflyPointDialog.java:506` | Organizer taps "End / Cancel Point" | Sets `is_active = 0`, `is_archived = 1` in SQLite and RTDB. Clears map overlays via `clearButterflyOverlay()`. Disconnects peer sync sentinels. |

---

### Phase 12: Tactical RAG Health Monitoring Engine Matrix
* **Primary Actor**: System Engine (`ButterflyPointEngine.evaluateButterflyRag`).
* **System Component**: `ButterflyPointEngine.kt` (Lines 714–856).

The RAG Engine evaluates 12 operational signals across participant telemetry on every clock tick, classifying participants into 4 operational states:

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                           TACTICAL RAG ENGINE OPERATIONAL MATRIX                                 │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

| Operational RAG State | Hex Color | Map Vector Color | Trigger Conditions (Logical OR) | Severity / Operational Impact | Recommended SOP Remediation |
|:---:|:---:|:---:|:---|:---:|:---|
| **🔴 RED** | `#EF4444` | Red (`#FF1744`) | 1. `isSosActive == true` (SOS Distress)<br>2. `status == REJECTED`<br>3. `status == TIMED_OUT` (> 24h)<br>4. Telemetry silent > 24 hours (`CRITICAL_OFFLINE`)<br>5. Battery $\le 5\%$ (`CRITICAL_BATTERY`)<br>6. Current time $> (\text{rendezvousTs} + 15\text{m})$ without reaching | **CRITICAL**<br>Participant unable to attend, in emergency distress, or severely overdue. | - Immediate operator call / check-in.<br>- Launch Nudge / Poke command.<br>- Trigger Time Extension (+30m/+1h).<br>- Escalate to SOS dispatch if distress active. |
| **🟡 AMBER** | `#F59E0B` | Amber (`#FFB300`) | 1. `status == PENDING` and meeting $< 45\text{m}$ away<br>2. Battery $6\%\text{ to }20\%$ (`LOW_BATTERY`)<br>3. Telemetry silent $30\text{m to }24\text{h}$ (`STALE_TELEMETRY`)<br>4. Calculated arrival epoch $> (\text{rendezvousTs} + 5\text{m})$<br>5. Distance $> 500\text{km}$ safeguard threshold<br>6. Device coordinates are $(0.0, 0.0)$ ("Awaiting GPS Fix")<br>7. Meeting $< 30\text{m}$ away, distance $> 1\text{km}$, speed $< 2\text{ km/h}$ ("Stationary") | **WARNING**<br>Participant progressing but at risk of delay, battery exhaustion, or missing confirmation. | - Send Nudge / Poke prompt.<br>- Check traffic & transit route.<br>- Proactively extend rendezvous time (+15m). |
| **🟢 GREEN** | `#10B981` | Cyan (`#00E5FF`) | 1. `status == REACHED` (Inside arrival perimeter)<br>2. `status == ACCEPTED` or `ON_MOVE` with timely ETA, healthy battery ($> 20\%$), and fresh telemetry ($< 30\text{m}$) | **OPTIMAL**<br>Participant actively en route or already arrived at rendezvous point. | - Continue passive tracking.<br>- Await Quorum completion alert. |
| **⚪ GREY** | `#6B7280` | N/A (Hidden) | 1. `status == REVOKED`<br>2. `status == PENDING` and meeting $> 45\text{m}$ in future | **INACTIVE / PENDING**<br>Invitation issued well in advance; awaiting participant interaction. | - No action required until 45m pre-meeting window opens. |

---

### Phase 13: 7-Day Max Availability, Auto-Sunset & Expiry Lifecycle
* **Primary Actor**: System Engine (Database Maintenance & Tracking Service).
* **System Component**: `RegistryManager.kt` (Lines 245–262), `DatabaseHelper.kt` (Lines 4556–4597).
* **Lifecycle Rules & Sunset Horizons**:
  1. **7-Day Maximum Availability Horizon**:
     - Parameter: `KEY_BUTTERFLY_MAX_AVAILABILITY_DAYS = 7`.
     - Invariant: A rendezvous point cannot exist in active state for more than 7 days from creation.
     - Enforcement: In `DatabaseHelper.getActiveButterflyPoints()`, if $\text{currentTime} > \text{rendezvousTimestamp} + (7 \times 86400000L)$, the point is automatically transitioned:
       `archiveButterflyPoint(pointId)` sets `is_active = 0, is_archived = 1`.
  2. **Default 4-Hour Meeting Window**:
     - Parameter: `KEY_BUTTERFLY_EXPIRY_HOURS = 4`.
     - Active meeting window defaults to 4 hours post-rendezvous time. After this window expires, points transition to history.
  3. **120-Minute Overdue Grace Window**:
     - Parameter: `KEY_BUTTERFLY_OVERDUE_GRACE_MINUTES = 120`.
     - Stale invitations in participant inboxes older than meeting time $+ 120\text{ minutes}$ are auto-declined as `STATUS_TIMED_OUT`.
  4. **Tier 3 Telemetry Retention Purge**:
     - High-velocity breadcrumb records in SQLite `butterfly_point_telemetry` older than the 7-day retention horizon are pruned via `ButterflyPointTelemetryTable.purgeExpiredTelemetry(...)`.

---

## 3. Runtime Operational Failure Scenarios & Edge Cases

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                         RUNTIME OPERATIONAL FAILURE SCENARIOS & RECOVERY                         │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Scenario F-01: Offline Point Creation (Zero Cellular Baseband)
* **Condition**: Device A creates a Butterfly Point in an underground parking garage with zero Internet.
* **System Behavior**:
  1. Local SQLite Tier 1 (`butterfly_point`) and Tier 2 (`butterfly_point_device_map`) records insert immediately without throwing exceptions.
  2. Firebase RTDB writes queue in memory via client SDK offline persistence.
  3. Viewport renders anchor and perimeter locally.
  4. Upon baseband recovery, `TrackingService` flushes queued RTDB nodes to cloud; invitations appear in peer inboxes.

### Scenario F-02: Rapid Boundary Flapping at Arrival Radius
* **Condition**: Participant walks back and forth across the 50m arrival circle boundary.
* **System Behavior**:
  1. Moving inside 50m triggers `STATUS_REACHED` and latches arrival timestamp.
  2. The 15m deadband hysteresis requires the participant to move beyond $50\text{m} + 15\text{m} = 65\text{m}$ before the status can revert to `STATUS_ON_MOVE`.
  3. Rapid oscillating chimes and notification flapping are completely eliminated.

### Scenario F-03: Delayed Flight / Distance Safeguard Exceeded (> 500km)
* **Condition**: Operator accidentally selects an overseas or out-of-state airport coordinate.
* **System Behavior**:
  1. Studio builder computes Haversine distance $> 500\text{km}$.
  2. Blocks single-click dispatch; forces two-touch confirmation via modal dialog.
  3. If confirmed, participant RAG badge displays Amber with reason *"Far Out (> 500 km)"*.

### Scenario F-04: Participant Device Dies (Battery Exhaustion)
* **Condition**: Participant's phone battery drops from 4% to 0% en route.
* **System Behavior**:
  1. At 5%, `evaluateButterflyRag` escalates status to **RED** with badge *"Critical Battery (5%)"*.
  2. When device powers off and telemetry stops, delta timestamp exceeds 30 minutes; badge updates to *"Stale Telemetry (Offline)"*.
  3. Organizer sees Red vector line on tactical map and can tap "Nudge" or call participant before complete blackout.

---

## 4. Operator HITL Quick-Reference Action Matrix

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                         OPERATOR HITL QUICK-REFERENCE ACTION MATRIX                              │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

| Operational Situation | Observed UI Indicator | Recommended HITL Action | UI Path / Gesture |
|:---|:---:|:---|:---|
| **Pending Participant with < 45m to Meeting** | Amber Badge: *"Awaiting Response"* | Dispatch Nudge / Poke | Tap Device Card ➔ Tap `[ Nudge / Poke ]` |
| **Traffic Jam / Fleet Delayed by 20m** | Amber Vector Line: *"Delayed by ~20m"* | Extend Meeting Time (+30m) | Tap Card Menu ➔ `[ Extend Time ]` ➔ Select `+30m` |
| **Participant Unresponsive / Declines** | Red Badge: *"Declined: Working"* | Re-Invite or Revoke | Tap Device Row ➔ Select `[ Re-Invite ]` or `[ Revoke ]` |
| **New Team Member Joins Field Operation** | Not present in active card | Delegated Mesh Peer Invite | Tap Point Menu ➔ `[ Invite More Peers ]` ➔ Check Box |
| **Navigating Unknown City Streets** | Azure Marker on live map | Launch Turn-by-Turn GPS | Tap Card Actions ➔ `[ Launch Navigation ]` |
| **Meeting Successfully Completed** | Emerald Banner: *"Quorum Assembled"* | End and Archive Point | Tap Point Menu ➔ `[ End / Archive Rendezvous ]` |

---

## 5. Verification & Attestation Audit

### Canonical SSOT Module Integrity Proof:
Every algorithm and parameter specified in this SOP resides in the authoritative module:
```powershell
Get-Item "jButterFly\src\main\java\com\jdroidx\jfindme\engine\map\ButterflyPointEngine.kt"
# Result: Single authoritative source of truth in :jButterFly
```

### Canonical Unit Test Suite Verification:
```powershell
.\gradlew.bat :jButterFly:testDebugUnitTest --tests "com.jdroidx.jfindme.EnterpriseUnitTestSuite"
# Result: BUILD SUCCESSFUL (0 failures)
```
- Test `testButterflyPointDryRunRiddhiToPushpa`: PASSED (Simulates end-to-end rendezvous creation, 15m deadband hysteresis, and RAG status transitions).
- Test `testButterfly3TierArchitectureDataExchangeGating`: PASSED (Verifies Tier 1 Master, Tier 2 Sub-Master, and Tier 3 Telemetry stream data isolation).
