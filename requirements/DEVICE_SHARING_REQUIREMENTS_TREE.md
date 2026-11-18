# Device Sharing & Multi-Node Mesh Synchronization — Master Requirement Specification
## Coordinate Tree Architecture: ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB
### Enterprise L5 Lifecycle Standard (20-Year Senior Developer Quality)
> **System:** `jFindMe` / `jButterFly` Enterprise Ecosystem  
> **Codename:** `jDroid-X` | **Operator:** `JEET` | **Date:** 2026-11-14T03:40:00+05:30  
> **Pattern:** Dynamic OOPS-based MVC / Tri-Tier Master-Fact-Transaction Database Architecture  
> **Subsystem:** Unified `<Device Sharing>` Orchestrator & Angel Butterfly Progress Sentinel  

---

## 1. Executive Mission & System Purpose

The **Device Sharing Subsystem** (`<Device Sharing>`) unifies and replaces the previously fragmented device-connecting actions (`Connect with Device C (Family / Friends)`, `Connect Another Device (Tickbox Selection)`, `Add Another Parent / Guardian`, and `1-Code Multi-Device Attachment`) into a single, cohesive, enterprise-grade sharing engine.

### Core Architectural Axioms:
1. **Unified Affordance Consolidation**: Eliminates UI bloat and duplicate code by merging four separate connection entry points into a single, high-fidelity `<Device Sharing>` action item on the Target Tactical Action Sheet (`MapActivity`).
2. **Deterministic 3-Node Topology ($Device\ A \to Share\ Device\ B \to to\ Device\ C$)**:
   - **Device A (Host / Operator)**: The active device orchestrating the network connection.
   - **Device B (Target Node)**: The device being shared (e.g., Ward / Child / Dependent or peer node).
   - **Device C (Recipient Node)**: The target peer, secondary guardian, or fleet device receiving mutual tracking and synchronization rights.
3. **Role & Permission Matrix**: Flexible assignment of bidirectional relationships across Family (`Guardian`, `Child`, `Spouse`, `Family Member`), Friends (`Best Friend`, `Close Friend`, `Peer`, `Colleague`), or Co-Guardian oversight with granular ACLs.
4. **Angel Butterfly Progress Matrix (`AngelLoader`)**:
   - Tapping "Share" immediately starts the `AngelLoader` with the high-performance hardware-accelerated flapping Lottie butterfly vector (`butterfly_angel.json`).
   - Touch-blocking dim scrim ($66\%$ opacity) prevents concurrent taps or race conditions during the handshake.
   - Status updates dynamically across real-time phases (`Securing Cloud Matrix...`, `Syncing Device B ↔ Device C...`, `Awaiting Bidirectional Handshake ACK...`).
5. **Bidirectional Closed-Loop Synchronization**:
   - The butterfly flaps continuously while synchronization is pending across both local SQLite tables and Firebase Realtime Database Cloud SSOT.
   - Once all participating devices acknowledge state parity, the butterfly stops flapping (`AngelLoader.hide(activity)`).
   - A rich slide-in notification banner displays full metadata information:
     `✅ Device B is synced with Device C | Role: [Role B ↔ Role C] | Mesh Circle: GRP_XXX | Latency: XXXms | SSOT Hash: #XXXX`.
6. **Standard Time-Limit Watchdog & Closed-Loop Fallback**:
   - Enforces a standard timeout threshold ($\Delta t_{max} = 15$ seconds).
   - If sync is not confirmed within the limit, the butterfly stops flapping immediately.
   - An interactive feedback alert dialog displays the exact root cause reason code:
     - `ERR_TIMEOUT_PEER_OFFLINE`: Recipient device unresponsive or disconnected from cellular data.
     - `ERR_CLOUD_HANDSHAKE_STALL`: Cloud SSOT latency exceeded standard SLA limit.
     - `ERR_ACL_CONFLICT`: Conflicting role or circle assignment already present in database.
     - `ERR_SECURITY_GATE_DENIAL`: Peer device privacy or security policy denied attachment.
   - Offers two decisive operator actions:
     - **[RETRY]**: Re-initiates the synchronization sequence immediately with prioritized mesh packet dispatch.
     - **[EXIT / TRY LATER]**: Enqueues the synchronization request into the local encrypted `offline_events` SQLite queue for automatic opportunistic background flushing (`OfflineSyncManager`), and dismisses the dialog gracefully.

---

## 2. Root Cause Bug Analysis: Image 1 Google Maps Failure

### Fatal Log Analysis:
```
32 I Google Play services package version: 264463013
32 I Google Play services maps renderer version(maps_core): 264320700
34 D SDK type: 1, version: 264320700
35 E Failed to get service from broker. (Fix with AI)
     java.lang.SecurityException: Unknown calling package name 'com.google.android.gms'.
        at android.os.Parcel.createException(Parcel.java:2074)
        at android.os.Parcel.readException(Parcel.java:2042)
        at android.os.Parcel.readException(Parcel.java:1990)
        at com.google.android.gms.common.internal.BaseGmsClient.q(:com.google.android.gms.policy_maps_core_dynamite@264320701@264320700025.100...)
        at m140.crb.run(:com.google.android.gms.policy_maps_core_dynamite@264320701@264320700025.100...)
36 W Not showing notification since connectionResult is not user-facing: ConnectionResult{statusCode=DEVELOPER_ERROR, resolution=null, message=null}
```

### Root Cause 1: Android 11+ Package Visibility Filter
- **Mechanism**: In Android 11+ (API 30+), the OS enforces strict package visibility restrictions. When Google Maps SDK's dynamic loader (`policy_maps_core_dynamite`) invokes IPC against `com.google.android.gms` to verify client credentials, Android's `PackageManager` denies visibility because `com.google.android.gms` was omitted from `<queries>` in `AndroidManifest.xml`.
- **Manifest Remediation**: Added `<package android:name="com.google.android.gms" />` and `<package android:name="com.android.vending" />` directly to the `<queries>` block in `AndroidManifest.xml`.

### Root Cause 2: GMS Broker ConnectionResult `DEVELOPER_ERROR` (Code 10)
- **Mechanism**: The Maps SDK API key (`AIzaSyDbkAsYbNmxJxBVy2_XmCmKnIvjflJV0Vg`) in Google Cloud Console is restricted to package `com.jdroidx.jfindme` and requires the exact SHA-1 fingerprint of the signing certificate.
- **Fingerprint Requirements**:
  - Release Keystore (`keystore/jButterfly.jks`):
    - `SHA-1: 24:DC:0B:EC:AF:C4:F4:29:72:14:DD:A4:DC:2E:90:54:FB:CE:07:7F`
    - `SHA-256: 9E:66:3E:A0:10:70:28:A3:D0:8B:9F:87:CC:02:4C:65:CD:90:FD:A5:2D:59:6D:1F:C7:49:79:34:79:69:AC:AB`
  - Debug Keystore (`~/.android/debug.keystore`):
    - `SHA-1: 69:FE:FE:00:96:65:B6:2C:EA:27:93:05:0C:A9:F6:EF:66:25:BE:33`
- **Architectural Fallback Remediation**: In `SplashActivity.java` and `MapActivity.java`, `MapsInitializer.initialize` is wrapped in an enterprise try-catch block with graceful degradation to `Renderer.LEGACY` upon any Dynamite module connection failure.

---

## 3. Master Coordinate Tree Architecture (ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)

```
ROOT: APPLICATION MASTER PLAN
│
├── 💼 [BR]₁: Tactical Target Action Sheet & Unified Affordance Governance
│    ├── 🖱️ [EL]₁․₁: Unified Device Sharing Entry Affordance
│    │    ├── 🖥️ [FE]₁․₁․₁: Consolidated Action Sheet Item (`📱 Device Sharing`)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₁․₁․₁․₁: The Target Action Sheet must replace the 4 legacy separate actions (Connect Device C, Connect Another Device, Add Parent, 1-Code Attachment) with a single menu item labeled "📱 Device Sharing".
│    │    │    │    ├── FR₁․₁․₁․₂: Target Device B cannot be the local operator's own UID (Self-selection auto-recenters camera).
│    │    │    │    └── FR₁․₁․₁․₃: Clicking "📱 Device Sharing" must launch the Unified Device Sharing Wizard (`showDeviceSharingWizard`) passing `targetUid`, `finalTargetName`, and `target` location model.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass: Single click on "📱 Device Sharing" launches Sharing Wizard Dialog in < 120ms with 0 UI stutter.
│    │    │    │    └── Fail: Target is null or self; triggers camera recenter instead of dialog.
│    │    │    └── ⚙️ [BE]₁․₁․₁․₁: Action Router & Target Validation Service (`MapActivity.showTacticalTargetActionSheet`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₁․₁․₁․₁․₁: Resolve Device B centric callsign from `DatabaseHelper.getDeviceCentricName(targetUid)`; fallback to `target.getUserName()`.
│    │    │         │    ├── BFR₁․₁․₁․₁․₂: Validate Device B network liveness and circle membership in local cache.
│    │    │         │    └── BFR₁․₁․₁․₁․₃: Audit menu dispatch to SQLite `sys_log` transaction ledger.
│    │    │         ├── Testing Scenarios:
│    │    │         │    ├── Pass: Valid target returns populated dossier and routes to Sharing Wizard.
│    │    │         │    └── Fail: Unresolved UID; logs warning and rejects opening wizard.
│    │    │         ├── 🗄️ [Master DB [Offline]]₁․₁․₁․₁․₁: SQLite `device` & `user_identity` tables
│    │    │         ├── 🗄️ [Master DB [Online]]₁․₁․₁․₁․₂: Firebase RTDB `/devices/{targetUid}`
│    │    │         ├── 🗄️ [Sub Master/Fact DB]₁․₁․₁․₁․₃: `change_log` (entity_type = 'DEVICE_ACTION_MENU')
│    │    │         └── 🗄️ [Transaction DB]₁․₁․₁․₁․₄: `sys_log.action_menu_click`
│
├── 💼 [BR]₂: Unified Device Sharing Wizard (Device A ➔ Share Device B ➔ to Device C)
│    ├── 🖱️ [EL]₂․₁: Sharing Mode & Topology Selector
│    │    ├── 🖥️ [FE]₂․₁․₁: Mode Selection Dialog (`UiArchitect.showRichOptionsDialog`)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₁․₁: Presents 4 structured sharing paths:
│    │    │    │    │    - Mode 1: 🔗 Direct Peer Share (Connect Device B to Device C with mutual Family/Friends role).
│    │    │    │    │    - Mode 2: 📋 Fleet Multi-Device Share (Tickbox selection of multiple recipient devices).
│    │    │    │    │    - Mode 3: 👨‍👩‍👧 Co-Guardian / Parent Delegation (Assign secondary parent/guardian rights).
│    │    │    │    │    - Mode 4: 🔑 1-Code Instant Mesh Attachment (Generate 6-digit sync token for 3-5 devices).
│    │    │    │    ├── FR₂․₁․₁․₂: Back button navigates back to Target Action Sheet without state corruption.
│    │    │    │    └── FR₂․₁․₁․₃: Manual Device Entry option present on all recipient lists for unlisted devices.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass: Selecting any mode routes to appropriate role/device step in < 100ms.
│    │    │    │    └── Fail: Invalid selection index; no action taken.
│    │    │    └── ⚙️ [BE]₂․₁․₁․₁: Candidate Fleet Resolver (`MapActivity.resolveFleetCandidates`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₂․₁․₁․₁․₁: Aggregate candidate devices from 3 sources: live locations (`viewModel.getUserLocations()`), offline roster (`DatabaseHelper.getOfflineRoster()`), and paired devices (`DatabaseHelper.getAllPairedDeviceIds()`).
│    │    │         │    ├── BFR₂․₁․₁․₁․₂: Deduplicate candidates using Case-Insensitive Set (`seenIds`).
│    │    │         │    └── BFR₂․₁․₁․₁․₃: Strictly exclude Target Device B and Host Device A from recipient list.
│    │    │         ├── Testing Scenarios:
│    │    │         │    ├── Pass: Returns clean, deduplicated candidate list with zero self-references.
│    │    │         │    └── Fail: Database locked; returns empty list with prompt for manual entry.
│    │    │         ├── 🗄️ [Master DB [Offline]]₂․₁․₁․₁․₁: SQLite `device_pairings` & `group_members`
│    │    │         ├── 🗄️ [Master DB [Online]]₂․₁․₁․₁․₂: Firebase RTDB `/relationships/{circleId}/members`
│    │    │         ├── 🗄️ [Sub Master/Fact DB]₂․₁․₁․₁․₃: `change_log` (entity_type = 'CANDIDATE_RESOLVER')
│    │    │         └── 🗄️ [Transaction DB]₂․₁․₁․₁․₄: `sys_log.fleet_candidate_query`
│
├── 💼 [BR]₃: Role & Permission Assignment Engine
│    ├── 🖱️ [EL]₃․₁: Relational Category & Role Picker
│    │    ├── 🖥️ [FE]₃․₁․₁: Two-Tier Role Matrix Dialog (`UiArchitect.showCompoundDeviceRoleDialog`)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₃․₁․₁․₁: Family Category offers: Parent/Guardian (B=Guardian ↔ C=Child), Child/Ward (B=Child ↔ C=Guardian), Spouse/Partner (Mutual), Sibling/Family (Mutual).
│    │    │    │    ├── FR₃․₁․₁․₂: Friends Category offers: Best Friend, Close Friend, Friend/Peer, Colleague/Work.
│    │    │    │    ├── FR₃․₁․₁․₃: Previews confirmation message stating exact relationship mapping before commit.
│    │    │    │    └── FR₃․₁․₁․₄: Final action button labeled "CONFIRM & SHARE" triggers synchronization pipeline.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass: Correct inverse roles assigned (e.g. Guardian for B implies Child for C).
│    │    │    │    └── Fail: Unselected role defaults to "Member" without crash.
│    │    │    └── ⚙️ [BE]₃․₁․₁․₁: Role Inversion & ACL Normalizer (`PairingRepository.normalizeRoles`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₃․₁․₁․₁․₁: Invert asymmetric roles (Guardian ⇄ Child, Admin ⇄ Member).
│    │    │         │    ├── BFR₃․₁․₁․₁․₂: Maintain symmetric roles unchanged (Spouse ⇄ Spouse, Friend ⇄ Friend).
│    │    │         │    └── BFR₃․₁․₁․₁․₃: Construct atomic payload updates for SQLite and Firebase SSOT.
│    │    │         ├── Testing Scenarios:
│    │    │         │    ├── Pass: Correctly produces bidirectional ACL payloads.
│    │    │         │    └── Fail: Malformed role string sanitized to default fallback.
│    │    │         ├── 🗄️ [Master DB [Offline]]₃․₁․₁․₁․₁: SQLite `device_pairings` (columns: `role_1`, `role_2`)
│    │    │         ├── 🗄️ [Master DB [Online]]₃․₁․₁․₁․₂: Firebase RTDB `/profiles/{uid}/role_title`
│    │    │         ├── 🗄️ [Sub Master/Fact DB]₃․₁․₁․₁․₃: `change_log` (entity_type = 'ROLE_NORMALIZATION')
│    │    │         └── 🗄️ [Transaction DB]₃․₁․₁․₁․₄: `sys_log.role_assignment`
│
├── 💼 [BR]₄: Angel Butterfly Progress & Live Multi-Device Mesh Synchronization Sentinel
│    ├── 🖱️ [EL]₄․₁: "Share Now" Execution Trigger
│    │    ├── 🖥️ [FE]₄․₁․₁: Angel Butterfly Loading Scrim (`AngelLoader.show`)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₄․₁․₁․₁: Flapping butterfly animation (`butterfly_angel.json`) must start immediately upon tapping "Share Now".
│    │    │    │    ├── FR₄․₁․₁․₂: Centered script card displays progressive status messages:
│    │    │    │    │    - "Connecting Mesh: Device B ➔ Device C..."
│    │    │    │    │    - "Securing Cloud SSOT Matrix..."
│    │    │    │    │    - "Awaiting Synchronization Handshake..."
│    │    │    │    ├── FR₄․₁․₁․₃: Screen is non-dismissible and touch-blocking while butterfly is flapping.
│    │    │    │    └── FR₄․₁․₁․₄: Butterfly continues flapping until ALL participating devices acknowledge state parity or timeout fires.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass: Butterfly renders at hardware-accelerated 60fps; status text updates cleanly.
│    │    │    │    └── Fail: Missing Lottie asset falls back gracefully to high-contrast circular progress bar.
│    │    │    └── ⚙️ [BE]₄․₁․₁․₁: Multi-Device Sync Dispatcher (`PairingRepository.linkDevicesWithRoles`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₄․₁․₁․₁․₁: Write bidirectional pairing rows in SQLite (`device_pairings`, `group_members`, `user_profile`).
│    │    │         │    ├── BFR₄․₁․₁․₁․₂: Push profile updates and circle membership to Firebase Realtime Database.
│    │    │         │    ├── BFR₄․₁․₁․₁․₃: Transmit atomic `refresh_roster` timestamp signal to `/relationships/{circleId}/signals`.
│    │    │         │    └── BFR₄․₁․₁․₁․₄: Track remaining ACKs with thread-safe `AtomicInteger remainingAcks`.
│    │    │         ├── Testing Scenarios:
│    │    │         │    ├── Pass: All database mutations succeed; callbacks executed on Main thread.
│    │    │         │    └── Fail: Network socket disconnect handled without crashing; returns error code.
│    │    │         ├── 🗄️ [Master DB [Offline]]₄․₁․₁․₁․₁: SQLite `device_pairings`, `user_profile`, `group_members`
│    │    │         ├── 🗄️ [Master DB [Online]]₄․₁․₁․₁․₂: Firebase RTDB `/relationships/{circleId}/device_pairings/{pairKey}`
│    │    │         ├── 🗄️ [Sub Master/Fact DB]₄․₁․₁․₁․₃: `change_log` (entity_type = 'DEVICE_SYNC_DISPATCH')
│    │    │         └── 🗄️ [Transaction DB]₄․₁․₁․₁․₄: `sys_log.sync_dispatch_event`
│
├── 💼 [BR]₅: Completion Notification & Metadata Presentation
│    ├── 🖱️ [EL]₅․₁: State Parity Latch
│    │    ├── 🖥️ [FE]₅․₁․₁: Butterfly Stop Flapping & Slide-in Metadata Notification Banner
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₅․₁․₁․₁: When sync completes successfully, invoke `AngelLoader.hide(activity)` to smoothly stop and dismiss the butterfly.
│    │    │    │    ├── FR₅․₁․₁․₂: Trigger slide-in green banner: "✅ DEVICE SHARING SYNCHRONIZED".
│    │    │    │    ├── FR₅․₁․₁․₃: Banner content must display full metadata:
│    │    │    │    │    "Device B ([NameB]) is synced with Device C ([NameC]) | Role: [RoleB ↔ RoleC] | Circle: [CircleID] | Timestamp: [Time]".
│    │    │    │    └── FR₅․₁․₁․₄: Map viewport and markers refresh automatically to reflect new relationships.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass: Butterfly fades out in 250ms; slide-in banner animates from top and dismisses after 5000ms.
│    │    │    │    └── Fail: Banner layout overlap; clamped by safe insets.
│    │    │    └── ⚙️ [BE]₅․₁․₁․₁: Viewport & Roster Refresh Sentinel (`MapViewModel.refreshAllData`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₅․₁․₁․₁․₁: Re-query local SQLite roster and update `_userLocations` LiveData stream.
│    │    │         │    ├── BFR₅․₁․₁․₁․₂: Refresh marker cluster and relationship badges on live Google Map.
│    │    │         │    └── BFR₅․₁․₁․₁․₃: Commit success audit record to SQLite transaction log.
│    │    │         ├── Testing Scenarios:
│    │    │         │    ├── Pass: New device marker and relationship line render on map immediately.
│    │    │         │    └── Fail: Roster cache stale; fallback to SQLite direct reload.
│    │    │         ├── 🗄️ [Master DB [Offline]]₅․₁․₁․₁․₁: SQLite `device_pairings`
│    │    │         ├── 🗄️ [Master DB [Online]]₅․₁․₁․₁․₂: Firebase RTDB `/devices/{id}/last_sync`
│    │    │         ├── 🗄️ [Sub Master/Fact DB]₅․₁․₁․₁․₃: `change_log` (entity_type = 'ROSTER_REFRESH')
│    │    │         └── 🗄️ [Transaction DB]₅․₁․₁․₁․₄: `sys_log.sync_success_audit`
│
└── 💼 [BR]₆: Standard Time-Limit Watchdog, Reason Codes & Retry/Exit Closed-Loop Fallback
     ├── 🖱️ [EL]₆․₁: Synchronization Timer Watchdog
     │    ├── 🖥️ [FE]₆․₁․₁: Timeout Diagnostic Alert Dialog (`UiArchitect.showFeedbackDialog`)
     │    │    ├── Validation & Conditional Rules:
     │    │    │    ├── FR₆․₁․₁․₁: Standard time limit watchdog expires at exactly 15,000ms ($\Delta t_{std} = 15s$).
     │    │    │    ├── FR₆․₁․₁․₂: If state parity is not verified before timer expiry:
     │    │    │    │    1. Butterfly stops flapping immediately (`AngelLoader.hide(activity)`).
     │    │    │    │    2. Renders Diagnostic Alert Dialog with explicit failure reason code.
     │    │    │    ├── FR₆․₁․₁․₃: Diagnostic Reason Codes presented to operator:
     │    │    │    │    - `ERR_TIMEOUT_PEER_OFFLINE`: "Device C did not acknowledge handshake within 15s. Device may be powered off, asleep, or out of cellular coverage."
     │    │    │    │    - `ERR_CLOUD_LATENCY`: "Cloud SSOT handshake latency exceeded standard threshold (>15,000ms)."
     │    │    │    │    - `ERR_ACL_CONFLICT`: "Device B and Device C are already linked in conflicting circle."
     │    │    │    │    - `ERR_NETWORK_DISCONNECT`: "Host device disconnected from cellular/Wi-Fi network."
     │    │    │    ├── FR₆․₁․₁․₄: The dialog must provide exactly two actionable choices:
     │    │    │    │    - Button 1: **[RETRY]** (Immediately re-arms the watchdog and restarts synchronization).
     │    │    │    │    - Button 2: **[EXIT / TRY LATER]** (Cancels active timer, preserves intent in offline sync queue, and dismisses).
     │    │    │    └── FR₆․₁․₁․₅: Operator is never left in an unhandled hanging state or blocked screen.
     │    │    ├── Testing Scenarios:
     │    │    │    ├── Pass: Timeout at 15s halts butterfly, reveals dialog with exact diagnostic reason, and RETRY resumes operation cleanly.
     │    │    │    └── Fail: Zombie timer continues running in background after activity finishes (prevented via Handler lifecycle cleanup).
     │    │    └── ⚙️ [BE]₆․₁․₁․₁: Sync Watchdog & Offline Failover Sentinel (`OfflineSyncManager.queuePendingSync`)
     │    │         ├── Logic & Business Rules:
     │    │         │    ├── BFR₆․₁․₁․₁․₁: Track active synchronization requests via thread-safe timestamp map.
     │    │         │    ├── BFR₆․₁․₁․₁․₂: On "EXIT / TRY LATER", serialize payload into SQLite `offline_events` queue with status `PENDING_RETRY`.
     │    │         │    ├── BFR₆․₁․₁․₁․₃: Background worker `OfflineSyncWorker` picks up queued event on next network reconnection.
     │    │         │    └── BFR₆․₁․₁․₁․₄: Audit timeout and user decision (RETRY vs EXIT) to SQLite transaction ledger.
     │    │         ├── Testing Scenarios:
     │    │         │    ├── Pass: Unsynced event safely persisted in offline queue; no data lost.
     │    │         │    └── Fail: SQLite write failure handled with crash-safe log.
     │    │         ├── 🗄️ [Master DB [Offline]]₆․₁․₁․₁․₁: SQLite `offline_events` (status = 'PENDING_RETRY')
     │    │         ├── 🗄️ [Master DB [Online]]₆․₁․₁․₁․₂: Firebase RTDB `/telemetry_failover/{deviceId}`
     │    │         ├── 🗄️ [Sub Master/Fact DB]₆․₁․₁․₁․₃: `change_log` (entity_type = 'SYNC_TIMEOUT_FALLBACK')
     │    │         └── 🗄️ [Transaction DB]₆․₁․₁․₁․₄: `sys_log.sync_watchdog_timeout`
```

---

## 4. Master Relational Database Tri-Tier Schema Integration

### Tier 1: Master DB [Offline & Online]
- **SQLite Table `device_pairings`**:
  ```sql
  CREATE TABLE IF NOT EXISTS device_pairings (
      pair_id TEXT PRIMARY KEY,
      group_id TEXT NOT NULL,
      device_id_1 TEXT NOT NULL,
      device_id_2 TEXT NOT NULL,
      role_1 TEXT NOT NULL,
      role_2 TEXT NOT NULL,
      sync_status TEXT NOT NULL DEFAULT 'SYNCED', -- 'SYNCING', 'SYNCED', 'FAILED', 'TIMEOUT'
      created_at INTEGER NOT NULL,
      last_synced_at INTEGER NOT NULL,
      FOREIGN KEY(device_id_1) REFERENCES device(device_id),
      FOREIGN KEY(device_id_2) REFERENCES device(device_id)
  );
  CREATE INDEX IF NOT EXISTS idx_pairings_devices ON device_pairings(device_id_1, device_id_2);
  CREATE INDEX IF NOT EXISTS idx_pairings_group ON device_pairings(group_id);
  ```
- **Firebase RTDB SSOT**:
  - `/relationships/{circleId}/device_pairings/{deviceB_deviceC}`
    - `role_b`: String
    - `role_c`: String
    - `shared_by`: String (Device A UID)
    - `sync_state`: String ("PENDING" | "ACKNOWLEDGED")
    - `synced_at`: Long (Timestamp)
    - `metadata_hash`: String (CRC32 / SHA-256)

### Tier 2: Sub Master / Fact DB
- **SQLite Table `change_log`**:
  - `entity_type`: `'DEVICE_SHARING'`
  - `entity_id`: `'{deviceB}_{deviceC}'`
  - `action`: `'SHARE_RELATIONSHIP_CREATED'`
  - `timestamp`: Current epoch millis
  - `metadata_payload`: JSON string containing circle, roles, latency, and ACK status.

### Tier 3: Transaction DB
- **SQLite Table `sys_log`**:
  - Log Level: `INFO` / `WARN` / `ERROR`
  - Component: `'DeviceSharingSentinel'`
  - Message: Execution trace, timeout triggers, user retry/exit selections, and handshake round-trip telemetry.

---

## 5. Unified Reactive Cross-Screen SSOT Data Pool
**Screen Convergence Pipeline (Live Map ↔ Network Gateway ↔ Family & Friends)**:

```
[Incoming Live GPS / Telemetry Fix]
               │
               ▼
   LocationRepository.ingestLocation()
       ├── In-Memory: latestLocations[canonicalId] = loc
       ├── LiveData: _userLocations.postValue(...)
       └── Backfill: DatabaseHelper.updateDeviceVitalsTelemetry(...)
               │
               ├── SQLite: DeviceVitalsTable.upsertVitals(...)
               └── Event: MasterDataSyncSentinel.notifyMasterDataChanged(...)
                       │
       ┌───────────────┼──────────────────────────────┐
       ▼               ▼                              ▼
 [Live Map]    [Network Gateway]            [Family & Friends]
  • MapViewModel • NetworkGatewayActivity    • FamilyFriendsViewModel
  • RAG Status:  • Initial RAG: SQLite+Repo   • Initial RAG: SQLite+Repo
    DeviceRag     • Real-time: ValueEvent-    • Real-time: LocationObserver
    Engine.        Listener + LocationObserver • RAG Status: DeviceRagEngine
    evaluate()   • Status: DeviceRagEngine     • Status: DeviceRagEngine
       │               │                              │
       └───────────────┴──────────────────────────────┘
                       │
          🟢 PARITY ACHIEVED: 100% IDENTICAL
             (Zero status divergence across screens)
```
