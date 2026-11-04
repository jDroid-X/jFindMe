# 👑 Coordinate Tree Re-Evaluation: Enterprise Admin Control Panel & MLM Multi-Tier Fleet Governance
## `jFindMeWeb` Master Administrative Architecture & Zero-Cross-Connection REA
> **Antigravity AI Enterprise Standard v5.0** | **Codename:** `jDroid-X` | **Operator:** `JEET`  
> **Repository Target:** `C:\Users\jiten\jAnitGravity\jFindMe\jFindMeWeb`  
> **Core Architecture:** `ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB` Structured Coordinate Tree  
> **Domain Focus:** Multi-Tier MLM Grouping, Zero Data Leakage, Device-Dependent Telemetry, Remote Metadata Override & Version Distribution

---

## 1. 🌐 The Executive Paradigm: Centralized Admin Mission Control

`jFindMeWeb` is not merely a passive tracking dashboard; it is the **authoritative, centralized Web-based Administrative Control Panel** governing all Android edge devices connected through the `jButterFly` mobile application via the Firebase Realtime Database cloud pipeline.

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                             CENTRALIZED ADMIN CONTROL & MLM GOVERNANCE PIPELINE                  │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                  ┌──────────────────────────────┴──────────────────────────────┐
                  ▼                                                             ▼
     [ ENTERPRISE CLOUD BRIDGE ]                                   [ MLM MULTI-TIER TREE ISOLATION ]
     Firebase RTDB (jfindme-3b15c)                                 Sponsor -> Circle -> Device Nodes
     ├── Exclusive Cloud Ingress/Egress                            ├── Strict Device-Dependent Data Only
     ├── Remote Metadata Mutation Engine                           ├── Zero Cross-Connection Leakage
     └── Realtime Bi-directional Broadcast                         └── Automated Rogue-Link Quarantining
                  │                                                             │
                  └──────────────────────────────┬──────────────────────────────┘
                                                 │
                                                 ▼
     ┌────────────────────────────────────────────────────────────────────────────────────────┐
     │                             ADMINISTRATIVE POWER CAPABILITIES                          │
     ├────────────────────────────────────────────────────────────────────────────────────────┤
     │ 1. Device Lifecycle: Activate / Deactivate / Cold-Storage Archive                      │
     │ 2. Remote Metadata & Sensor Override: Edit wrong data, correct corrupted parameters   │
     │ 3. Automated OTA Version Distribution: Push latest release to lower-version units      │
     │ 4. Macro-to-Micro Statistical Analytics: "Eagle to Ant" top-to-bottom drilldown      │
     │ 5. Silicon Anti-Collision Sentinel: Detect and purge duplicate hardware collisions    │
     └────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 🛡️ Multi-Level Marketing (MLM) Process & Anti-Cross-Connection Governance

In an MLM / Multi-Tier hierarchical organization, devices are grouped into structured levels:
$$\text{Enterprise / Tenant} \longrightarrow \text{Regions / Squadrons} \longrightarrow \text{Sponsors / Guardians} \longrightarrow \text{Leaf Device Units}$$

### The 4 Non-Negotiable Governance Directives:
1. **Strict Device-Dependent Telemetry**: Every telemetry packet (latitude, longitude, speed, battery, cell tower LAC/CID) is strictly bound to its verified hardware silicon ID (`ro.ril.oem.sno` + IMEI hash). No data can ever be shared across disparate branches.
2. **Zero Cross-Connection Tolerance**: A device can only communicate with and receive telemetry from its direct authenticated parent, sponsor, or authorized downline circle members. Any cross-tree data leakage or shared session token is flagged as an integrity breach.
3. **Data Poisoning & Misconfiguration Remediation**: The Admin Control Panel has the absolute authority to interrogate any device, detect erroneous metadata (wrong phone number, invalid circle ID, corrupted reporting interval), and push authoritative corrections down to the device.
4. **Automated Fleet Version Uniformity**: The admin console continuously compares each device's reported `version_code` against `masterRelease` (`version.properties`). Any device or entire MLM group running a lower version is flagged in amber and queued for an Over-The-Air (OTA) binary delta push.

---

## 3. 🔍 Page-by-Page / Screen-by-Screen REA in the Coordinate Tree (`ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB`)

Below is the exhaustive, screen-by-screen re-evaluation of every view in `jFindMeWeb` from the perspective of the Admin Control Panel, MLM multi-tier grouping, cross-connection prevention, and remote management.

---

### 🖥️ Screen W01: Master Web Admin Shell (`index.html` & `js/app.js`)
- **[ROOT]**: Application container at `index.html` bootstrapping `js/app.js`. Mounts global sidebar, top navigation, `#workspace-root`, and `#terminal`.
- **[BR - Business Rules]**:
  - Provides zero-trust administrative authentication and real-time connectivity status to Firebase RTDB (`/.info/connected`).
  - Implements global error trapping (`window.onerror`) ensuring administrative oversight never terminates even when an edge node transmits corrupted telemetry.
- **[EL - Elements]**: Google-style jButterfly brand masthead, navigation items with color-coded status badges, real-time connectivity beacon (`SYNC_ACTIVE`), responsive mobile sidebar overlay, forensic audit terminal.
- **[FE - Frontend Behavior]**: Single-page view router preserving client state and active Firebase listeners across view transitions. Live terminal stream auto-scrolling with filter tabs (ALL, SUCCESS, WARNING, ERROR).
- **[BE - Backend Engine]**: Reverse proxy communication through `static_server.js` (Port 8080) to `server.js` (Port 3000) using Firebase Admin SDK.
- **[DB - Database Mapping]**: Listens to Firebase RTDB `/.info/connected` and routes incoming events to `Store.js`.
- **[Admin & MLM Functionality]**: Provides the primary operational cockpit to monitor system-wide socket health, switch between administrative views, and clear forensic fault registers.

---

### 🖥️ Screen W02: Technical Operations Dashboard (`js/views/DashboardView.js`)
- **[ROOT]**: File location `js/views/DashboardView.js` (67,292 bytes).
- **[BR - Business Rules]**:
  - Computes top-level enterprise KPIs aggregating all MLM branches: Total Registered Units, Active Online Units, Stale Units, and Active Geofenced Circles.
  - Enforces hierarchical aggregation: metrics must accurately summarize downline device states without leaking individual sensitive PII in macro views.
- **[EL - Elements]**: 4 KPI telemetry cards with glowing drop-shadows, Quick Tactical Action Launchers (Open Matrix, Register Device, Audit Ledgers), Live Movement Event Stream, Search & Filter Bar.
- **[FE - Frontend Behavior]**: Animated numerical counters, live ticker updates as devices report new coordinates, click-through triggers navigating to individual device dossiers or Live Radar.
- **[BE - Backend Engine]**: `TelemetryAggregator.computeDashboardMetrics` querying `/devices` and `/locations`.
- **[DB - Database Mapping]**: Aggregates over `devices`, `location_event`, and `safe_zones`.
- **[Admin & MLM Functionality]**: Top-level macro monitoring. Identifies system-wide anomalies (e.g., sudden spikes in inactive units indicating a network carrier failure or an unauthorized branch disconnection).

---

### 🖥️ Screen W03: Interactive Tactical Map Radar (`js/views/MapView.js`, `LeafletMapView.js`, `GoogleMapView.js`)
- **[ROOT]**: View orchestrator `MapView.js` delegating to `LeafletMapView.js` or `GoogleMapView.js`.
- **[BR - Business Rules]**:
  - Visualizes spatial locations strictly partitioned by authorized MLM circles and squadrons.
  - Detects speed anomalies ($> 250\text{ km/h}$) and flags potential mock GPS spoofing before rendering map pins.
  - If a device is found transmitting data under two different branch trees, the radar highlights its marker with a crimson warning border.
- **[EL - Elements]**: Leaflet/Google Maps canvas, custom SVG markers with pulsing concentric rings (Emerald for Active, Amber for Stale, Crimson for SOS/Tamper), Cluster Count bubbles, Tile Provider Switcher, Recenter FAB.
- **[FE - Frontend Behavior]**: Smooth marker interpolation along vector paths, rich popup cards on marker click displaying battery voltage, speed, cellular signal dBm, and parent group name.
- **[BE - Backend Engine]**: Subscribes to Firebase RTDB `/live-map` SSE stream; reverse-geocodes coordinates to physical postal addresses.
- **[DB - Database Mapping]**: Cloud `/live-map/{silicon_id}` and `/devices/{silicon_id}`.
- **[Admin & MLM Functionality]**: Real-time visual auditing. Allows the admin to click on any node, inspect its immediate parent circle, verify that its spatial breadcrumbs match its assigned operational zone, and trigger a remote "Poke" to test device liveness.

---

### 🖥️ Screen W04: Fleet Device Registry Matrix Table (`js/views/DeviceListView.js`)
- **[ROOT]**: File location `js/views/DeviceListView.js` (18,754 bytes).
- **[BR - Business Rules]**:
  - Enforces device inventory governance: lists all physical units with serial descriptors, hardware models, and reporting statuses.
  - Provides administrative status categorization: Active, Stale ($> 15\text{m}$ silence), and Archived.
  - Implements multi-tier search across Device Alias, Silicon ID, Hardware Model, and Assigned MLM Circle.
- **[EL - Elements]**: Real-time debounced search input, status filter dropdown, multi-column sortable table, status badge chips, action buttons (Inspect Dossier, Edit Configuration, Purge/Archive).
- **[FE - Frontend Behavior]**: Instantaneous client-side filtering, column header sorting (Ascending/Descending), paginated views (50 rows per page) to eliminate browser rendering bottlenecks.
- **[BE - Backend Engine]**: `DeviceController.getFilteredDevices()`.
- **[DB - Database Mapping]**: Maps to SQLite `devices` table and Firebase RTDB `/devices/`.
- **[Admin & MLM Functionality]**: Administrative triage. Detects orphaned devices (devices without an assigned parent sponsor or circle) and provides a 1-click bridge to edit and reassign them.

---

### 🖥️ Screen W05: MLM Hierarchical Relationship Studio (`js/views/DeviceUserView.js`)
- **[ROOT]**: Core enterprise file `js/views/DeviceUserView.js` (173,997 bytes, 2,692 lines).
- **[BR - Business Rules]**:
  - The central command center for **MLM Multi-Tier Grouping and Relationship Management**.
  - Enforces `groupBy: "none" | "group" | "role" | "status"`, dynamically partitioning the fleet into hierarchical sponsor trees and circle squads.
  - **Zero Cross-Connection Enforcer**: Ensures that user-to-device mappings in `safe_zone_user_map` and `group_members` have valid foreign keys and never link to unrelated third-party groups.
  - **Device Lifecycle Manager**: Implements full **Activation, Deactivation, and Cold-Storage Archive** (`loadArchivedDevices()`, `restoreArchivedDevice()`).
  - **Version Disparity Auditor**: Compares device `version_code` against `masterRelease` via `loadMasterRelease()` and flags outdated units for OTA delta upgrades.
- **[EL - Elements]**: Dual-pane master-detail view, Grouping Segmented Toggle, Date/Time Horizon Filter, Device User Cards with role emblems, Cold-Storage Archive Toggle, Master Release Version Banner, Restore Device Pills.
- **[FE - Frontend Behavior]**: Interactive tree expansion, reactive state updates when profile metadata is modified, slide notifications confirming cloud commits.
- **[BE - Backend Engine]**: REST endpoints `/api/update-profile`, `/api/archived-devices`, `/api/device/restore-archive`, `/api/system-release`.
- **[DB - Database Mapping]**: Interacts with `users`, `devices`, `groups`, `group_members`, and Firebase `/devices/{id}/config`.
- **[Admin & MLM Functionality]**: Absolute administrative authority. Admins can change a device's assigned sponsor, update phone numbers, deactivate lost units, restore archived hardware, and initiate version upgrades.

---

### 🖥️ Screen W06: Device Metadata & Control Inspector (`js/views/DeviceEditModalView.js`)
- **[ROOT]**: File location `js/views/DeviceEditModalView.js` (34,901 bytes).
- **[BR - Business Rules]**:
  - Allows admins to correct erroneous, corrupted, or outdated metadata on any connected mobile unit.
  - Validates all input fields: E.164 phone format, alphanumeric alias restrictions, and reporting cadence boundaries ($1\text{s} \le \text{cadence} \le 3600\text{s}$).
  - Broadcasts the updated configuration directly to the mobile device via Firebase RTDB.
- **[EL - Elements]**: Modal backdrop with glassmorphic blur, Device Alias input, Assigned Phone input, Role selector dropdown (Admin, Supervisor, Field Agent, Asset), Cadence slider, Save Changes button.
- **[FE - Frontend Behavior]**: Form dirty-state tracking, regex validation micro-badges (Green check for valid, Red warning for invalid), loading spinner during cloud commit.
- **[BE - Backend Engine]**: REST `POST /api/update-profile` and Firebase Admin SDK `.update()`.
- **[DB - Database Mapping]**: Commits updates to Firebase `/devices/{id}/config` and mirrors to SQLite `user_profile` / `devices`.
- **[Admin & MLM Functionality]**: Data correction. If an agent's phone number was entered incorrectly or their circle role changed, the admin corrects it here; the mobile app detects the change via its Firebase listener and updates its local SQLite DB automatically.

---

### 🖥️ Screen W07: Device De-Registration & Cold-Storage Archive (`js/views/DeviceDeleteModalView.js`)
- **[ROOT]**: File location `js/views/DeviceDeleteModalView.js` (32,360 bytes).
- **[BR - Business Rules]**:
  - Governs device de-registration, deactivation, and archiving.
  - Implements the "Cold-Storage Archive" protocol: moves the device out of the active fleet without deleting historical forensic breadcrumbs, allowing future restoration.
  - If "Permanent Purge" is selected, cascades deletion across all group memberships to prevent orphaned reference pointers.
- **[EL - Elements]**: Crimson warning banner, Hardware Identifiers Summary, "Archive to Cold Storage" vs "Permanent Cryptographic Purge" Radio Buttons, Confirmation Text Input challenge ("CONFIRM_DELETE"), Action Buttons.
- **[FE - Frontend Behavior]**: Prevents accidental clicks by disabling the delete action until the exact confirmation text is typed; renders animated deletion feedback.
- **[BE - Backend Engine]**: REST `POST /api/devices/:id/archive` or `DELETE /api/devices/:id`.
- **[DB - Database Mapping]**: Updates `devices.status = 'ARCHIVED'` or cascades deletes across `devices`, `location_event`, and `/devices/{id}`.
- **[Admin & MLM Functionality]**: Lifecycle decommissioning. Protects the MLM network integrity by removing compromised, stolen, or decommissioned devices so they can no longer report or receive fleet data.

---

### 🖥️ Screen W08: Anti-Cross-Connection & Collision Remediation (`js/views/DupDeviceView.js`)
- **[ROOT]**: File location `js/views/DupDeviceView.js` (65,172 bytes, 1,029 lines).
- **[BR - Business Rules]**:
  - **The Primary Anti-Cross-Connection Sentinel**.
  - Scans the database for duplicate device collisions where two different agent IDs share the same immutable **Physical Silicon SHA-256 Hardware Fingerprint** (Brand + Model + Board + Hardware + Device + IMEI).
  - Identifies devices that fragmented post-OTA update or re-installation, creating phantom cross-connections.
  - Implements atomic multi-tier merging: consolidates historical telemetry breadcrumbs under the canonical active device and purges the duplicate stub.
- **[EL - Elements]**: Top Header Bar with Silicon Sentinel badge, Collision Pair Comparison Cards (Old Device vs New Device), Hardware Difference Table, "Atomic Merge" Action Pill, "Purge Rogue Node" Button.
- **[FE - Frontend Behavior]**: Side-by-side visual diff highlighting matching hardware hashes versus divergent timestamps; one-click merge execution with slide notification feedback.
- **[BE - Backend Engine]**: REST `/api/duplicate-devices` and `/api/merge-devices`.
- **[DB - Database Mapping]**: Re-points foreign keys in `location_event` and `geofence_events` to the canonical primary ID, then deletes the duplicate record from `devices`.
- **[Admin & MLM Functionality]**: Data integrity sanitization. Guarantees that no single physical phone exists as two separate nodes in different MLM groups, eliminating data poisoning and duplicate billing.

---

### 🖥️ Screen W09: Macro-to-Micro "Eagle to Ant" Analytics (`js/views/EagleAnalyticsTreeView.js`)
- **[ROOT]**: File location `js/views/EagleAnalyticsTreeView.js` (32,248 bytes) and `controllers/D3TreeRenderer.js`.
- **[BR - Business Rules]**:
  - Implements the **"Eagle flying at high altitude to Ant on the ground"** top-to-bottom drilldown analytical model:
    $$\text{Global Enterprise} \longrightarrow \text{Regions / Circles} \longrightarrow \text{SafeZones} \longrightarrow \text{Device Unit} \longrightarrow \text{Micro GPS Ticks ("The Ant")}$$
  - Statistical anomaly detection: identifies devices exhibiting abnormal idling, speeding outliers, or mock location injections.
- **[EL - Elements]**: Top Eagle-Eye Telemetry Bar, Speed Unit Toggle (km/h vs mph), Metric Badges (Total Units, Moving, Idle, Integrity Alerts), D3.js Collapsible Hierarchical Tree Canvas, Micro Sensor Inspector Drawer.
- **[FE - Frontend Behavior]**: Interactive D3 collapsible nodes with smooth spring physics; clicking an enterprise node expands its subsidiary circles; clicking a leaf device expands its recent GPS tick breadcrumbs with speed and altitude gauges.
- **[BE - Backend Engine]**: `EagleAnalyticsController` aggregating cross-sectional fleet statistics.
- **[DB - Database Mapping]**: Multi-table join across `organizations`, `groups`, `devices`, and `location_event`.
- **[Admin & MLM Functionality]**: Deep technical forensics. Allows an admin to drill down from a 10,000-device enterprise view straight to the physical sensor data of an individual phone in seconds to identify misconfigured or misbehaving units.

---

### 🖥️ Screen W10: Historical Breadcrumb Scrubber (`js/views/HistoryTrailView.js` & `ReplayEngine.js`)
- **[ROOT]**: File location `js/views/HistoryTrailView.js` (10,960 bytes) and `controllers/ReplayEngine.js`.
- **[BR - Business Rules]**:
  - Enforces strict device-dependent historical auditing: queries and renders breadcrumb points strictly bound to the selected device ID.
  - Eliminates coordinate jumps and multi-path noise using temporal speed validation ($v = \Delta d / \Delta t$).
- **[EL - Elements]**: Target Device Dropdown, Date Range Pickers, Interactive Time Slider (Scrubber), Play/Pause Controls, Speed Multiplier Buttons (1x, 2x, 5x, 10x), Speedometer Gauge.
- **[FE - Frontend Behavior]**: Vehicle marker glides along polyline trajectory in synchronization with slider; color codes polyline segments based on speed thresholds (Emerald for normal, Amber for moderate, Red for speeding).
- **[BE - Backend Engine]**: `HistoryRepository.fetchBreadcrumbs(agentId, startTime, endTime)`.
- **[DB - Database Mapping]**: Queries SQLite `location_event` table and Firebase `/locations/{id}`.
- **[Admin & MLM Functionality]**: Verification of movement truth. Admins can verify whether an agent genuinely visited a designated checkpoint or if their device reported false locations.

---

### 🖥️ Screen W11: Geofencing Boundary Builder (`js/views/GeofencingView.js` & `SafeZoneCreateModalView.js`)
- **[ROOT]**: File location `js/views/GeofencingView.js` (39,074 bytes) and `SafeZoneCreateModalView.js`.
- **[BR - Business Rules]**:
  - SafeZones and operational perimeters are assigned strictly to designated MLM groups and authorized devices.
  - Prevents perimeter leakage: a SafeZone created for Group A is invisible to and cannot trigger alerts for Group B.
  - Calculates mathematical polygon containment via the Ray-Casting algorithm.
- **[EL - Elements]**: Leaflet Draw Toolbar (Draw Polygon, Draw Circle, Reshape Handles), Boundary Metadata Drawer, Assigned Devices Multi-Select Checkboxes, Trigger Rule Checkboxes (Enter, Exit, Dwell), Save Zone Button.
- **[FE - Frontend Behavior]**: Interactive vertex manipulation with live area and perimeter calculations; polygon shape validation (minimum 3 non-intersecting vertices).
- **[BE - Backend Engine]**: `SafeZoneController.saveBoundary()` and Firebase RTDB `/safe_zones` sync.
- **[DB - Database Mapping]**: Maps to SQLite `safe_zones` and `safe_zone_user_map` tables.
- **[Admin & MLM Functionality]**: Perimeter control. Allows admins to establish operational boundaries for specific squads and receive instant breach notifications.

---

### 🖥️ Screen W12: Global Emergency SOS & Tamper Alert Banner (`js/views/AlertBannerView.js`)
- **[ROOT]**: File location `js/views/AlertBannerView.js` (5,487 bytes).
- **[BR - Business Rules]**:
  - Listens to high-priority emergency events: hardware SOS button presses, power-off disconnects, and unauthorized SIM swaps.
  - Overrides all active views with a persistent visual and acoustic alert until acknowledged by the administrator.
- **[EL - Elements]**: Top-anchored Crimson Floating Banner, Pulsing Alarm Emblem, Target Device Details (Name, Phone, Coordinates, Battery), "Center on Radar" Button, "Acknowledge Alert" Button.
- **[FE - Frontend Behavior]**: High-frequency CSS box-shadow pulsing, automatic playback of `assets/sounds/sos_beacon.mp3`, immediate radar map centering on click.
- **[BE - Backend Engine]**: Firebase RTDB listener on `/alerts/sos`.
- **[DB - Database Mapping]**: Cloud `/alerts/sos/{id}` and SQLite `alerts` table.
- **[Admin & MLM Functionality]**: Immediate life-safety intervention. Alerts the admin instantly if a field agent triggers an SOS or if a phone is tampered with.

---

### 🖥️ Screen W13: 178-Parameter Dual-Engine Sync Matrix (`database-sync-settings.html`)
- **[ROOT]**: File location `database-sync-settings.html` (373,846 bytes, 7,087 lines).
- **[BR - Business Rules]**:
  - The master configuration engine of the entire ecosystem.
  - Controls **178 parameters across 20 modules**, governing every facet of mobile device behavior: GPS duty cycles, battery saver thresholds, baseband cell tower scanning, RTT timeouts, and OTA version updates.
  - **Over-The-Air (OTA) Version Push**: Module 16 allows admins to broadcast new APK releases (`version.properties`) to lower-version mobile units.
  - **Remote Device Control**: Any parameter modified here is pushed to Firebase and synchronized to mobile devices automatically.
- **[EL - Elements]**: 20 Module Selection Cards, Filter Chips (All, Modified, Synced, Conflict), 178 Parameter Specification Cards, Slide-Out Right Drawer (2-Column Smart-Fit Layout), 1-Click Bi-directional Sync Buttons.
- **[FE - Frontend Behavior]**: Instant search filtering, slide-out drawer inspection, live value comparison between local edge SQLite and cloud Firebase RTDB, visual mismatch highlighting.
- **[BE - Backend Engine]**: Node.js API Proxy (`js/server.js`) interacting with Firebase Admin SDK and SQLite bridge endpoints.
- **[DB - Database Mapping]**: Bridges all 17 SQLite tables (`JFindMe_CitizenData.db`) to Firebase RTDB root collections.
- **[Admin & MLM Functionality]**: Full-fleet remote reconfiguration. If an entire MLM squad needs to switch from a 15-second tracking interval to a 5-second emergency mode, the admin updates Module 2 here and broadcasts it fleet-wide in seconds.

---

### 🖥️ Screen W14: Coordinate Tree & MindMap Explorer (`js/views/CoordinateTreeMindMapView.js`)
- **[ROOT]**: File location `js/views/CoordinateTreeMindMapView.js` (78,464 bytes, 1,187 lines).
- **[BR - Business Rules]**:
  - Provides a self-documenting, interactive visualization of the entire ecosystem architecture (`ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB`) across all 32 Android Native Screens and 11 Web Screens.
- **[EL - Elements]**: Topbar Control Strip with Platform Toggle (Android vs Web), Search Textbox, Domain Filter Dropdown, 3D Card Flipper, Node Detail Drawer, Export Spec Button.
- **[FE - Frontend Behavior]**: Interactive tree navigation, search filtering matching screen names, business rules, elements, or database tables; 3D card flip revealing technical specifications.
- **[BE - Backend Engine]**: Client-side hierarchical data model (`buildCoordinateDataset()`).
- **[DB - Database Mapping]**: Reflects all 17 SQLite tables and Firebase RTDB endpoints.
- **[Admin & MLM Functionality]**: Architectural governance and onboarding. Allows developers and senior administrators to verify screen dependencies, functional requirements (FR), and test pass/fail criteria.

---

## 4. 📊 Technical Summary Table: Screen-by-Screen MLM & Admin Capabilities

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                     ADMIN CONTROL PANEL SCREEN-BY-SCREEN CAPABILITIES MATRIX                     │
├──────┬────────────────────────────┬─────────────────────────────┬────────────────────────────────┤
│ ID   │ Screen Name                │ Primary Admin Capability    │ Anti-Cross-Connection Role     │
├──────┼────────────────────────────┼─────────────────────────────┼────────────────────────────────┤
│ W01  │ `index.html`               │ Master Shell & Socket Audit │ Global Zero-Trust Auth Guard   │
│ W02  │ `DashboardView.js`         │ Macro Enterprise KPI Deck   │ Tree-Partitioned Metrics       │
│ W03  │ `MapView.js` (Radar)       │ Tactical Geospatial Tracking│ Multi-Tenant Visual Isolation  │
│ W04  │ `DeviceListView.js`        │ Device Inventory & Triage   │ Orphaned Node Quarantine       │
│ W05  │ `DeviceUserView.js`        │ MLM Grouping & Lifecycle    │ Activate / Archive / Lineage   │
│ W06  │ `DeviceEditModalView.js`   │ Remote Metadata Correction  │ Fixes Corrupted Circle Data    │
│ W07  │ `DeviceDeleteModalView.js` │ Cold-Storage & Secure Purge │ Cascading Reference Cleanup    │
│ W08  │ `DupDeviceView.js`         │ Silicon Hardware Sentinel   │ Merges Collisions, Kills Bleed │
│ W09  │ `EagleAnalyticsTreeView.js`│ Top-to-Bottom Tree Analytics│ Detects Rogue Branch Links     │
│ W10  │ `HistoryTrailView.js`      │ Chronological Flight Scrubber│ Device-Dependent Breadcrumbs  │
│ W11  │ `GeofencingView.js`        │ Perimeter Security Studio   │ Group-Specific SafeZone Scope  │
│ W12  │ `AlertBannerView.js`       │ SOS & Tamper Dispatcher     │ Hardware Disconnect Alerts     │
│ W13  │ `database-sync-settings`   │ 178-Param Config & OTA Push │ Fleet-Wide Firmware Uniformity │
│ W14  │ `CoordinateTreeMindMap`    │ Full Architecture Inspector │ System SSOT Verification       │
└──────┴────────────────────────────┴─────────────────────────────┴────────────────────────────────┘
```

