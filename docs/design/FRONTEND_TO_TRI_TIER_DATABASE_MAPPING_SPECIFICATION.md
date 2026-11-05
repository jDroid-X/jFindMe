# 🌳 Master Frontend-to-Database Mapping Specification (ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)
## Enterprise Tri-Tier Screen-by-Screen Dataflow & MLM Hierarchical Access Control
> **System:** `jFindMe` / `jButterFly` Enterprise Ecosystem  
> **Codename:** `jDroid-X` | **Operator:** `JEET` | **Version:** 5.1.0 (Enterprise Gold Master V80)  
> **Architecture Standard:** Strict OOPS MVC Coordinate Tree  
> **Database Scope:** Local SQLite V80 (`JFindMe_CitizenData.db`) & Online Firebase Cloud Matrix  
> **Taxonomy Standard:** Tri-Tier Database Hierarchy (Master DB • Sub-Master/Fact DB • Transactional DB)  
> **Relational Standard:** Northwind 2003 Access-Style Referential Integrity ($1 \rightarrow \infty$, 0 Orphan Tables)  
> **Governance Mandate:** MLM Device Pairing Network Gateway (Top-to-Bottom Permitted • Bottom-to-Top Gated • Zero Device Metadata Cross-Connections)

---

## 🏛️ Architectural Mandates: Device Metadata Independence & MLM Hierarchy

### 1. Zero Cross-Connection Invariant (Hardware Metadata Isolation)
Every physical device’s hardware metadata (`devices`, `sim`, `device_vitals`, `device_policy`, `watchdog_resurrection_ledger`) is **strictly independent and autonomous**.
- **No Direct Table Cross-Connections**: Device A never stores, joins, or mutates Device B's internal hardware tables.
- **Single Source of Truth (SSOT)**: The ONLY bridge between devices exists at the **Relational Identity & Pairing Layer** (`user_identity` $\leftrightarrow$ `device_pairing` / `group_members`).
- **Physical Partitioning**:
  - *Offline SQLite V80*: Partitioned strictly by silicon root key (`hardware_id`).
  - *Online Firebase Cloud Matrix*: Partitioned strictly under `/devices/{hardware_id}/` (Hardware Dossier) and `/locations/{agentId}/` (Telemetry Outbox).

### 2. Multi-Level Management (MLM) Asymmetric Visibility Protocol
Data sharing and spatial visibility between devices operate on a **Directed Acyclic Tree (MLM Hierarchy)**:
- **Hierarchy Levels**:
  - `Level 1 [ROOT_GUARDIAN / MASTER_ADMIN]`: `node_index = 1`, `role = 'ADMIN'`, `clearance = 'MASTER'`.
  - `Level 2 [SUPERVISOR / SPOUSE]`: `node_index = 2`, `role = 'GUARDIAN'`, `clearance = 'SUPERVISOR'`.
  - `Level 3+ [DEPENDENT_CHILD / FIELD_WARD]`: `node_index >= 3`, `role = 'DEPENDENT_CHILD'`, `clearance = 'WARD'`.
- **Top-to-Bottom Visibility ($\Downarrow$ Default Permitted)**:
  - Top devices can inspect telemetry, GNSS breadcrumbs, battery vitals, and safezone alerts of all subordinate devices assigned below them in the MLM tree branch.
- **Bottom-to-Top Visibility ($\Uparrow$ Strictly Prohibited Unless Explicitly Permitted)**:
  - Bottom devices CANNOT view superior/top device locations or vitals.
  - **Permission Gate (`allow_dependents`)**: Upstream visibility is unlocked ONLY if the top device explicitly enables `allow_dependents = true` (or bit `0x08` in `permissions_mask`). Otherwise, upstream markers are purged from the child map canvas.
- **Lateral / Cross-Branch Isolation**:
  - Sibling devices on parallel branches have ZERO visibility into each other unless explicitly bridged via a mutual Group Network Gateway.

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

---

## 📑 Core Screen Navigation Index

1. [Screen 01: Citizen Profile & Master Security PIN (`ProfileActivity`)](#screen-01-citizen-profile--master-security-pin-profileactivity)
2. [Screen 02: Device Pairing & Network Gateway (`NetworkGatewayActivity`)](#screen-02-device-pairing--network-gateway-networkgatewayactivity)
3. [Screen 03: Friends & Family Roster Governance (`FamilyFriendsActivity`)](#screen-03-friends--family-roster-governance-familyfriendsactivity)
4. [Screen 04: Live Map Tactical Radar & Real-Time Tracking (`LiveMapDashboardActivity` / `MapActivity`)](#screen-04-live-map-tactical-radar--real-time-tracking-livemapdashboardactivity--mapactivity)
5. [Screen 05: SafeZone & Geofence Boundary Matrix (`SafeZoneActivity`)](#screen-05-safezone--geofence-boundary-matrix-safezoneactivity)

---

### Screen 01: Citizen Profile & Master Security PIN (`ProfileActivity`)

```text
ROOT: PROFILE_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.profile.ProfileActivity)
│
├── 💼 [BR]₁: Citizen Identity Registration, MLM Level Initialization & Hardware Root Isolation
│    ├── 🖱️ [EL]₁․₁: Display Name & Nickname Entry Affordance
│    │    ├── 🖥️ [FE]₁․₁․₁: Input Field & Floating Label: "Full Name" & "Map Callout Nickname"
│    │    │    ├── (FR1.1): Full Name: 2-50 characters, letters, spaces, hyphens only.
│    │    │    │    ├── Pass Scenario: "Alexander Wright" -> Validated; green checkmark indicator.
│    │    │    │    └── Fail Scenario: "A" or "<script>" -> Inline red error: "Name must be 2-50 characters".
│    │    │    ├── (FR1.2): Nickname: 1-15 characters, alphanumeric only, rendered on map callout pill.
│    │    │    │    ├── Pass Scenario: "Alex" -> Validated; pill preview updates in real-time.
│    │    │    │    └── Fail Scenario: "Alex Wright The Great" -> Truncated/blocked at 15 chars.
│    │    │    └── ⚙️ [BE]₁․₁․₁․₁: ProfileSanitizer & Normalization Controller
│    │    │         ├── (BFR1.1): Strips HTML tags, trims whitespace, enforces Title Case.
│    │    │         │    ├── Pass Scenario: "  alexander  " -> Emits normalized "Alexander".
│    │    │         │    └── Fail Scenario: Null or empty -> Throws ValidationException("NAME_REQUIRED").
│    │    │         ├── 🗄️ [Master DB [Offline]]₁․₁․₁․₁․₁: `user_identity.uid_1` (PK) & `user_identity.full_name`
│    │    │         │    ├── 🗄️ [Master DB [Online]]₁․₁․₁․₁․₂: Firebase RTDB `/devices/{agentId}/Identity/full_name`
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₁․₁․₁․₁․₃: `user_profile.name` & `user_profile.nickname`
│    │    │         │         ├── 🗄️ [Sub Master/Fact DB [Online]]₁․₁․₁․₁․₄: Firebase RTDB `/devices/{agentId}/profile/nickname`
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₁․₁․₁․₁․₅: `sys_logs` (tag='PROFILE', msg='Name updated')
│    │    │         │              └── 🗄️ [Transactional DB [Online]]₁․₁․₁․₁․₆: Firebase RTDB `/devices/{agentId}/audit/profile_mutations/{ts}`
│    │
│    ├── 🖱️ [EL]₁․₂: MLM Node Identity & Asymmetric Role Display
│    │    ├── 🖥️ [FE]₁․₂․₁: Read-Only Hierarchy Badge: [Level 1: Root Guardian] or [Level 3: Dependent Child]
│    │    │    ├── (FR1.3): Renders current node clearance level derived from `node_index` and `role_type`.
│    │    │    │    ├── Pass Scenario: `node_index = 1` -> Badge displays "Root Master / Guardian" with gold shield.
│    │    │    │    └── Pass Scenario: `node_index = 3` -> Badge displays "Dependent Ward" with blue icon.
│    │    │    └── ⚙️ [BE]₁․₂․₁․₁: NodeHierarchyResolver (`DatabaseHelper.resolveNodeIndex`)
│    │    │         ├── (BFR1.2): Resolves node index without cross-connecting device hardware metadata.
│    │    │         ├── 🗄️ [Master DB [Offline]]₁․₂․₁․₁․₁: `user_identity.node_index` (MLM Level Anchor)
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₁․₂․₁․₁․₂: `user_org_map.clearance` & `user_org_map.role_name`
│    │    │         │         ├── 🗄️ [Sub Master/Fact DB [Online]]₁․₂․₁․₁․₃: Firebase RTDB `/devices/{agentId}/Identity/node_index`
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₁․₂․₁․₁․₄: `pending_sync_queue` (payload_type='NODE_INDEX_VERIFY')
│    │    │         │              └── 🗄️ [Transactional DB [Online]]₁․₂․₁․₁․₅: Firebase RTDB `/devices/{agentId}/sync_status`
│    │
│    ├── 🖱️ [EL]₁․₃: Master Parental Security PIN Affordance (Hardware Bound)
│    │    ├── 🖥️ [FE]₁․₃․₁: 4-Digit Secure PIN Entry Matrix & Confirmation Keypad
│    │    │    ├── (FR1.4): Exactly 4 numerical digits; rejects trivial sequences (e.g., "1234", "0000").
│    │    │    │    ├── Pass Scenario: "8492" -> Accepted; prompts for confirmation.
│    │    │    │    └── Fail Scenario: "1111" -> Warning: "PIN is too weak. Choose a non-trivial code".
│    │    │    └── ⚙️ [BE]₁․₃․₁․₁: Argon2id Cryptographic Hasher & Hardware Salt Generator
│    │    │         ├── (BFR1.3): Generates 16-byte salt; hashes via Argon2id. Bound strictly to local device.
│    │    │         ├── 🗄️ [Master DB [Offline]]₁․₃․₁․₁․₁: `user_identity.id` (Security Root)
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₁․₃․₁․₁․₂: `user_profile.master_pin_hash` & `user_profile.salt`
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₁․₃․₁․₁․₃: `security_evasion_audit_log` (evasion_type='PIN_ESTABLISHED')
│    │    │         │              └── 🗄️ [Transactional DB [Online]]₁․₃․₁․₁․₄: (ZERO CLOUD SYNC — PIN hash NEVER leaves device hardware)
```

---

### Screen 02: Device Pairing & Network Gateway (`NetworkGatewayActivity`)

```text
ROOT: NETWORK_GATEWAY_MASTER_PLAN (com.jdroidx.jfindme.ui.group.NetworkGatewayActivity)
│
├── 💼 [BR]₂: Hardware-Anchored Device Pairing, MLM Tree Lineage & Anti-Cross-Contamination
│    ├── 🖱️ [EL]₂․₁: 6-Digit Pair Code Input & Asymmetric Handshake Viewfinder
│    │    ├── 🖥️ [FE]₂․₁․₁: 6-Box Segmented Code Input & Camera Barcode Viewfinder
│    │    │    ├── (FR2.1): Auto-advances focus on digit entry; auto-submits upon 6th digit.
│    │    │    │    ├── Pass Scenario: Enters "839201" -> Triggers background pair handshake animation.
│    │    │    │    └── Fail Scenario: Enters "839A" -> Blocks non-numeric character.
│    │    │    ├── (FR2.2): Viewfinder detects JFindMe encrypted QR matrix (`jfindme://pair?id=...&code=...&mlm_level=...`).
│    │    │    │    ├── Pass Scenario: Scans QR -> Vibrates with haptic feedback (50ms); populates agent ID.
│    │    │    │    └── Fail Scenario: Scans standard URL -> Error toast: "Invalid JFindMe pairing QR".
│    │    │    └── ⚙️ [BE]₂․₁․₁․₁: MlmPairingEngine & Cryptographic Lineage Gateway
│    │    │         ├── (BFR2.1): Establishes MLM child edge: `parent_node_id = initiator_node`, `child_node_id = target_node`.
│    │    │         ├── (BFR2.2): Enforces Anti-Cyclic Check: Rejects pairing if target is already an ancestor (prevents A -> B -> A loops).
│    │    │         ├── (BFR2.3): Zero Device Cross-Connection: Preserves independent hardware records; pairs ONLY logical identity.
│    │    │         ├── 🗄️ [Master DB [Offline]]₂․₁․₁․₁․₁: `devices.hardware_id` (Host Device Master — Independent)
│    │    │         │    ├── 🗄️ [Master DB [Online]]₂․₁․₁․₁․₂: Firebase RTDB `/devices/{initiatorHwId}/Identity/auth_uid`
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₂․₁․₁․₁․₃: `sim.iccid` & `sim.carrier_name` (Isolated hardware slot)
│    │    │         │         ├── 🗄️ [Sub Master/Fact DB [Online]]₂․₁․₁․₁․₄: Firebase RTDB `/hw_mapping/{initiatorHwId}`
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₂․₁․₁․₁․₅: `invitations` (token, status='PENDING' ➔ 'ACTIVE')
│    │    │         │              └── 🗄️ [Transactional DB [Online]]₂․₁․₁․₁․₆: Firebase RTDB `/pairing_requests/{requestId}` (status='ACCEPTED')
│    │
│    ├── 🖱️ [EL]₂․₂: Top-to-Bottom Permission Grant Toggle ("Allow Dependents To Track Me")
│    │    ├── 🖥️ [FE]₂․₂․₁: Asymmetric Visibility Switch (Visible only on Level 1 / Level 2 Guardian Devices)
│    │    │    ├── (FR2.3): Toggling ON permits child nodes below this device to view guardian location on map.
│    │    │    │    ├── Pass Scenario: Switch ON -> Emits `allow_dependents = true`. Bottom-to-top gate OPEN.
│    │    │    │    └── Pass Scenario: Switch OFF -> Emits `allow_dependents = false`. Bottom-to-top gate LOCKED.
│    │    │    └── ⚙️ [BE]₂․₂․₁․₁: AsymmetricAclController (`RegistryManager.setAllowDependents`)
│    │    │         ├── (BFR2.4): Updates local preference and broadcasts ACL update flag to Firebase node.
│    │    │         ├── 🗄️ [Master DB [Offline]]₂․₂․₁․₁․₁: `user_identity.uid_1` (Guardian Anchor)
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₂․₂․₁․₁․₂: `group_members.permissions_mask` (Bit 0x08 = ALLOW_DEPENDENTS)
│    │    │         │         ├── 🗄️ [Sub Master/Fact DB [Online]]₂․₂․₁․₁․₃: Firebase RTDB `/devices/{guardianId}/device/allow_dependents`
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₂․₂․₁․₁․₄: `pending_sync_queue` (payload_type='ACL_UPDATE')
│    │    │         │              └── 🗄️ [Transactional DB [Online]]₂․₂․₁․₁․₅: Firebase RTDB `/relationships/{activeGroup}/signals/acl_refresh`
│    │
│    ├── 🖱️ [EL]₂․₃: Zombie Node De-Duplication & Independent Hardware Pruner
│    │    ├── 🖥️ [FE]₂․₃․₁: "Prune Duplicate Devices" Action Chip & Progress Bar
│    │    │    ├── (FR2.4): Emits confirmation dialog: "Remove stale instances of this device model?".
│    │    │    │    ├── Pass Scenario: Confirms -> Executes `pruneZombieNodes`; purges orphaned identity mappings.
│    │    │    └── ⚙️ [BE]₂․₃․₁․₁: Multi-Device Zombie Node Pruner (`DatabaseHelper.pruneZombieNodes`)
│    │    │         ├── (BFR2.5): Cleans orphaned `user_identity` records while leaving independent physical device metadata uncorrupted.
│    │    │         ├── 🗄️ [Master DB [Offline]]₂․₃․₁․₁․₁: `user_identity.uid_1` (Current Canonical Agent ID)
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₂․₃․₁․₁․₂: `devices.device_model` & `user_profile.phone`
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₂․₃․₁․₁․₃: `sys_logs` (tag='PRUNE', msg='Orphan node removed')
│    │    │         │              └── 🗄️ [Transactional DB [Online]]₂․₃․₁․₁․₄: Firebase RTDB `/devices/{orphanUid}` (`removeValue()`)
```

---

### Screen 03: Friends & Family Roster Governance (`FamilyFriendsActivity`)

```text
ROOT: FAMILY_FRIENDS_MASTER_PLAN (com.jdroidx.jfindme.ui.group.FamilyFriendsActivity)
│
├── 💼 [BR]₃: Circle Roster Hierarchy, MLM Asymmetric Filtering & Real-Time Presence
│    ├── 🖱️ [EL]₃․₁: Circle Category Filter Tabs (Family • Friends • Dependents • All)
│    │    ├── 🖥️ [FE]₃․₁․₁: Horizontal Scrollable Segmented Chip Group: [All (N)], [Guardians (N)], [Dependents (N)]
│    │    │    ├── (FR3.1): Selecting chip filters roster instantly via in-memory LiveData.
│    │    │    └── ⚙️ [BE]₃․₁․₁․₁: RosterCategoryClassifier (`FamilyFriendsViewModel.classifyCategory`)
│    │    │         ├── (BFR3.1): Classifies into `ROOT_GUARDIAN`, `SPOUSE`, `DEPENDENT_CHILD`, `FRIEND`.
│    │    │         ├── 🗄️ [Master DB [Offline]]₃․₁․₁․₁․₁: `groups.group_id` (PK) & `groups.group_name`
│    │    │         │    ├── 🗄️ [Master DB [Online]]₃․₁․₁․₁․₂: Firebase RTDB `/relationships/{activeGroup}/name`
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₃․₁․₁․₁․₃: `group_members.circle_type` & `group_members.role`
│    │    │         │         └── 🗄️ [Sub Master/Fact DB [Online]]₃․₁․₁․₁․₄: Firebase RTDB `/relationships/{activeGroup}/members/{agentId}`
│    │
│    ├── 🖱️ [EL]₃․₂: Member Roster Card & Dynamic Telemetry Pill (MLM Visibility Gated)
│    │    ├── 🖥️ [FE]₃․₂․₁: Card Component: 3D Avatar, Member Name, Nickname Pill, Battery Gauge, RAG Status Badge
│    │    │    ├── (FR3.2): Top-to-Bottom: Guardian sees child's live battery, online state, and telemetry.
│    │    │    │    ├── Pass Scenario: Guardian views Child -> Displays "Battery: 85%", "At School", "Active Now".
│    │    │    ├── (FR3.3): Bottom-to-Top: Child views Guardian:
│    │    │    │    ├── Pass Scenario (`allow_dependents = true`): Displays Guardian live presence.
│    │    │    │    └── Pass Scenario (`allow_dependents = false`): Masked: Displays "Location Hidden By Parent" / Pill hidden.
│    │    │    └── ⚙️ [BE]₃․₂․₁․₁: MlmVisibilityEvaluator (`FamilyFriendsViewModel.resolveRosterIdentities`)
│    │    │         ├── (BFR3.2): Evaluates asymmetric tree edge before binding telemetry data snapshot to card.
│    │    │         ├── 🗄️ [Master DB [Offline]]₃․₂․₁․₁․₁: `user_identity.uid_1` (Peer Agent ID)
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₃․₂․₁․₁․₂: `device_vitals.battery_level`, `device_vitals.motion_state`
│    │    │         │         ├── 🗄️ [Sub Master/Fact DB [Online]]₃․₂․₁․₁․₃: Firebase RTDB `/devices/{agentId}/telemetry`
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₃․₂․₁․₁․₄: `alert_history` (alert_type='SOS_PANIC', is_read=0)
│    │    │         │              └── 🗄️ [Transactional DB [Online]]₃․₂․₁․₁․₅: Firebase RTDB `/relationships/{groupId}/signals/sos`
│    │
│    ├── 🖱️ [EL]₃․₃: Permission Matrix Management Toggle (Tracking • Geofence • Remote Alert)
│    │    ├── 🖥️ [FE]₃․₃․₁: Switch Controls: [Can Track My Location], [Can Trigger SafeZone Alerts], [Is Admin]
│    │    │    ├── (FR3.4): Toggling switch calculates bitwise mask (`0x01` Track, `0x02` SafeZone, `0x04` Admin, `0x08` AllowDependents).
│    │    │    └── ⚙️ [BE]₃․₃․₁․₁: PermissionMaskBitwiseOperator
│    │    │         ├── 🗄️ [Master DB [Offline]]₃․₃․₁․₁․₁: `groups.group_id`
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₃․₃․₁․₁․₂: `group_members.permissions_mask`
│    │    │         │         ├── 🗄️ [Sub Master/Fact DB [Online]]₃․₃․₁․₁․₃: `/relationships/{grpId}/members/{agentId}/permissions`
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₃․₃․₁․₁․₄: `sys_logs` (tag='PERM_MUTATION', msg='Permissions updated')
│    │    │         │              └── 🗄️ [Transactional DB [Online]]₃․₃․₁․₁․₅: Firebase RTDB `/relationships/{grpId}/signals/refresh_roster`
```

---

### Screen 04: Live Map Tactical Radar & Real-Time Tracking (`LiveMapDashboardActivity` / `MapActivity`)

```text
ROOT: LIVE_MAP_DASHBOARD_MASTER_PLAN (com.jdroidx.jfindme.ui.map.LiveMapDashboardActivity)
│
├── 💼 [BR]₄: Tactical Radar Sweeping, MLM Asymmetric Spatial Filter & Zero Sensor Loss
│    ├── 🖱️ [EL]₄․₁: Fleet Summary KPI Badges (Total Assets • Active Live • Standby • SOS Panic)
│    │    ├── 🖥️ [FE]₄․₁․₁: 4 Top Counter Badges displaying real-time aggregation across all authorized child devices.
│    │    │    ├── (FR4.1): Aggregates only devices visible under current viewer's MLM branch.
│    │    │    └── ⚙️ [BE]₄․₁․₁․₁: FleetMetricsAggregator & MlmBranchFilter
│    │    │         ├── 🗄️ [Master DB [Offline]]₄․₁․₁․₁․₁: `devices.hardware_id` (Count of registered devices)
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₄․₁․₁․₁․₂: `device_vitals` joined with `devices`
│    │    │         │         ├── 🗄️ [Sub Master/Fact DB [Online]]₄․₁․₁․₁․₃: Firebase RTDB `/devices/{agentId}/device/live`
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₄․₁․₁․₁․₄: `alert_history` (WHERE alert_type='SOS_PANIC' AND resolved_at=0)
│    │    │         │              └── 🗄️ [Transactional DB [Online]]₄․₁․₁․₁․₅: Firebase RTDB `/devices/{agentId}/device/sos_active`
│    │
│    ├── 🖱️ [EL]₄․₂: Tactical Map Canvas & High-Frequency Multi-Marker Overlay (V80 Sensor Full)
│    │    ├── 🖥️ [FE]₄․₂․₁: Google Maps SDK V2 Surface with Custom 3D Avatar Markers, Accuracy Rings & Elevation Profile
│    │    │    ├── (FR4.2): Renders marker position with V80 Sensor Granularity (`accuracy` radius circle, `altitude` tag).
│    │    │    ├── (FR4.3): MLM Asymmetric Filter:
│    │    │    │    ├── Pass Scenario: Guardian views Child -> Child marker rendered at full precision.
│    │    │    │    ├── Pass Scenario: Child views Guardian (`allow_dependents = true`) -> Guardian marker rendered.
│    │    │    │    └── Pass Scenario: Child views Guardian (`allow_dependents = false`) -> Guardian marker SUPPRESSED / OMITTED.
│    │    │    └── ⚙️ [BE]₄․₂․₁․₁: SpatialProcessor & MlmMarkerGatekeeper
│    │    │         ├── (BFR4.1): Rejects GPS jitter (< 3m) and coordinates outside authorized MLM branch.
│    │    │         ├── 🗄️ [Master DB [Offline]]₄․₂․₁․₁․₁: `user_identity.uid_1` (Anchor ID)
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₄․₂․₁․₁․₂: `user_profile.nickname`, `user_profile.avatar_url`
│    │    │         │         ├── 🗄️ [Sub Master/Fact DB [Online]]₄․₂․₁․₁․₃: Firebase RTDB `/devices/{agentId}/device/live`
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₄․₂․₁․₁․₄: `location_event` (lat, lng, speed, bearing, accuracy, altitude, motion_state)
│    │    │         │              └── 🗄️ [Transactional DB [Online]]₄․₂․₁․₁․₅: Firebase RTDB `/History/{agentId}/history/{ts}`
│    │
│    ├── 🖱️ [EL]₄․₃: Emergency SOS Panic Broadcast Trigger Button (Priority Outbox)
│    │    ├── 🖥️ [FE]₄․₃․₁: Floating Action Button (Crimson, Dual-Action: 3-Second Hold or 3 Quick Taps)
│    │    │    ├── (FR4.4): Haptic countdown: 3 vibrations over 3s before SOS payload locks and dispatches.
│    │    │    └── ⚙️ [BE]₄․₃․₁․₁: TelemetryFailoverRouter & EmergencyBroadcastEngine
│    │    │         ├── (BFR4.2): Bypasses all MLM restrictions — SOS panic broadcasts UPWARD to all superior guardians immediately.
│    │    │         ├── 🗄️ [Master DB [Offline]]₄․₃․₁․₁․₁: `user_identity.uid_1` (Self Agent ID)
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₄․₃․₁․₁․₂: `device_vitals.is_stealth`
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₄․₃․₁․₁․₃: `alert_history` (alert_type='SOS_PANIC', timestamp=nowMs)
│    │    │         │              ├── 🗄️ [Transactional DB [Offline]]₄․₃․₁․₁․₄: `pending_sync_queue` (payload_type='SOS', is_sos=1)
│    │    │         │              └── 🗄️ [Transactional DB [Online]]₄․₃․₁․₁․₅: Firebase RTDB `/relationships/{activeGroup}/signals/sos`
```

---

### Screen 05: SafeZone & Geofence Boundary Matrix (`SafeZoneActivity`)

```text
ROOT: SAFEZONE_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.geofence.SafeZoneActivity)
│
├── 💼 [BR]₅: Spatial Geofencing, MLM Policy Inheritance & Dwell Detection
│    ├── 🖱️ [EL]₅․₁: Geofence Creation Map Tool & Circular/Polygon Radius Slider (Guardian Only)
│    │    ├── 🖥️ [FE]₅․₁․₁: Interactive Map with Draggable Center Pin & Radius Slider (50m to 5000m)
│    │    │    ├── (FR5.1): Guardian (Level 1/2) can create, edit, or delete safezones for any child in their branch.
│    │    │    ├── (FR5.2): Dependent Child (Level 3+) has read-only access (creation UI hidden/disabled).
│    │    │    │    ├── Pass Scenario: Guardian creates "School Zone" (200m) -> Deployed to child devices.
│    │    │    │    └── Fail Scenario: Child attempts to alter radius -> Blocked: "Parental authorization required".
│    │    │    └── ⚙️ [BE]₅․₁․₁․₁: GeofenceHelper & MlmZonePolicyEngine
│    │    │         ├── (BFR5.1): Validates creator node clearance $\le 2$ before committing zone geometry.
│    │    │         ├── 🗄️ [Master DB [Offline]]₅․₁․₁․₁․₁: `safe_zones.zone_id` (PK UUID) & `safe_zones.created_by`
│    │    │         │    ├── 🗄️ [Master DB [Online]]₅․₁․₁․₁․₂: Firebase RTDB `/Geofencing/{creatorId}/safe_zones/{zoneId}`
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₅․₁․₁․₁․₃: `safe_zones.radius_meters`, `safe_zones.is_dwell_alert_enabled`
│    │    │         │         ├── 🗄️ [Sub Master/Fact DB [Online]]₅․₁․₁․₁․₄: Firebase RTDB `/Geofencing/{creatorId}/safe_zones/{zoneId}/connected_devices`
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₅․₁․₁․₁․₅: `pending_sync_queue` (payload_type='SAFE_ZONE_ADD')
│    │    │         │              └── 🗄️ [Transactional DB [Online]]₅․₁․₁․₁․₆: Firebase RTDB `/Geofencing/{creatorId}/safe_zones/{zoneId}/notifications`
│    │
│    ├── 🖱️ [EL]₅․₂: SafeZone User Assignment & Schedule Picker
│    │    ├── 🖥️ [FE]₅․₂․₁: Checkbox List of Downstream Branch Members & Active Time Window: [08:00 AM - 04:00 PM]
│    │    │    ├── (FR5.3): Selecting a child member binds their `identity_id` to the zone schedule.
│    │    │    └── ⚙️ [BE]₅․₂․₁․₁: GeofenceScheduleValidator
│    │    │         ├── 🗄️ [Master DB [Offline]]₅․₂․₁․₁․₁: `safe_zones.zone_id`
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₅․₂․₁․₁․₂: `safe_zone_user_map` (zone_id, identity_id, alert_on_entry, alert_on_exit)
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₅․₂․₁․₁․₃: `sys_logs` (tag='ZONE_MAP', msg='User mapped to zone')
│    │
│    ├── 🖱️ [EL]₅․₃: Geofence Breach & Dwell Event History Feed (Upward Ingress)
│    │    ├── 🖥️ [FE]₅․₃․₁: Chronological Transition Event Cards: [Child Left School at 03:45 PM], [Dwelling > 15m]
│    │    │    ├── (FR5.4): Breach events automatically propagate UPWARD to parent guardian devices.
│    │    │    └── ⚙️ [BE]₅․₃․₁․₁: GeofenceBroadcastReceiver & MlmAlertDispatcher
│    │    │         ├── (BFR5.2): On exit/breach, dispatches push notification to all superior ancestors in the MLM tree.
│    │    │         ├── 🗄️ [Master DB [Offline]]₅․₃․₁․₁․₁: `safe_zones.zone_id` & `user_identity.id`
│    │    │         │    └── 🗄️ [Sub Master/Fact DB [Offline]]₅․₃․₁․₁․₂: `alert_history.alert_type`
│    │    │         │         └── 🗄️ [Transactional DB [Offline]]₅․₃․₁․₁․₃: `alert_history` (alert_type='GEOFENCE_EXIT', is_read=0)
│    │    │         │              └── 🗄️ [Transactional DB [Online]]₅․₃․₁․₁․₄: Firebase RTDB `/Geofencing/{creatorId}/safe_zones/{zoneId}/target_devices/{targetId}/notifications/{date}`
```

---

## 🔁 Section 6: Closed-Loop Dataflow & MLM Topology Matrix

```mermaid
flowchart TD
    subgraph MLM_TOPOLOGY ["Multi-Level Management (MLM) Hierarchy"]
        L1["Root Guardian (Node 1 / Admin)"]
        L2["Secondary Guardian (Node 2 / Supervisor)"]
        L3["Dependent Child (Node 3 / Ward)"]
        
        L1 -->|1. Top-to-Bottom Telemetry Inspection: ALLOWED| L2
        L2 -->|2. Top-to-Bottom Telemetry Inspection: ALLOWED| L3
        L3 -.->|3. Bottom-to-Top Location View: BLOCKED (allow_dependents=false)| L1
        L3 ==>|4. Bottom-to-Top Location View: UNLOCKED (allow_dependents=true)| L1
        L3 ==>|5. Emergency SOS Broadcast: BYPASSES GATES (Always Propagates UP)| L1
    end

    subgraph TRI_TIER_DATABASE ["Tri-Tier Normalized Storage (SQLite V80 & Firebase Cloud)"]
        subgraph MASTER_TIER ["Tier 1: Master Tables (Foundational Identity)"]
            T1_ID[("user_identity\n(AP, T64UI, node_index)")]
            T1_DEV[("devices\n(AP, hardware_id - ISOLATED)")]
            T1_GRP[("groups\n(AP, group_id)")]
            T1_ZONE[("safe_zones\n(AP, zone_id)")]
        end

        subgraph SUBMASTER_TIER ["Tier 2: Sub-Master / Dimensions (Junctions & Attributes)"]
            T2_PROF[("user_profile\n(1:1 with user_identity)")]
            T2_MEM[("group_members\n(permissions_mask, circle_type)")]
            T2_SIM[("sim\n(1:M with devices - ISOLATED)")]
            T2_POLICY[("device_policy\n(1:1 with devices - ISOLATED)")]
            T2_MAP[("safe_zone_user_map\n(zone_id, identity_id)")]
        end

        subgraph TRANSACTIONAL_TIER ["Tier 3: Transactional OLTP (Streams & Logs)"]
            T3_LOC[("location_event\n(lat, lng, accuracy, altitude, motion_state)")]
            T3_QUEUE[("pending_sync_queue\n(Indexed V80 Store-and-Forward Buffer)")]
            T3_VITALS[("device_vitals\n(battery, temp, ram, storage - ISOLATED)")]
            T3_ALERT[("alert_history\n(SOS, Geofence Breaches)")]
        end
    end

    L1 --- T1_ID
    L1 --- T1_DEV
    T1_ID -->|1 -> 1| T2_PROF
    T1_ID -->|1 -> ∞| T2_MEM
    T1_DEV -->|1 -> ∞| T2_SIM
    T1_DEV -->|1 -> 1| T2_POLICY
    T1_DEV -->|1 -> ∞| T3_VITALS
    T1_ID -->|1 -> ∞| T3_LOC
    T1_ID -->|1 -> ∞| T3_QUEUE
    T1_ID -->|1 -> ∞| T3_ALERT
```
