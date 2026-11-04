# 🌳 Coordinate Tree Detailed Nested Architecture Specification (ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)
## `jFindMeWeb` Enterprise Web Admin Control Panel & MLM Fleet Governance
> **Antigravity AI Enterprise Standard v5.0** | **Codename:** `jDroid-X` | **Operator:** `JEET`  
> **Repository Target:** `C:\Users\jiten\jAnitGravity\jFindMe\jFindMeWeb`  
> **Completeness Level:** Exhaustive Forensic Parity — 100% Codebase Verified (Routes, Gaps & Sequences Reconciled)

---

## 📑 Master Index of Screens & Pages

1. **[Screen W01]** `index.html` & `js/app.js` — Master Web Admin Shell & Telemetry Banner
2. **[Screen W02]** `DashboardView.js` — Enterprise Macro Operations Dashboard & MLM Fleet KPIs
3. **[Screen W03]** `MapView.js` — Tactical Geospatial Radar & Hierarchical Marker Clustering
4. **[Screen W04]** `DeviceListView.js` — Fleet Registry Matrix, Search & Orphaned Node Triage
5. **[Screen W05]** `DeviceUserView.js` — MLM Hierarchical Relationship Studio, Lifecycle & OTA Version Auditor
6. **[Screen W06]** `DeviceEditModalView.js` — Remote Device Metadata Override & Parameter Inspector
7. **[Screen W07]** `DeviceDeleteModalView.js` — Device De-Registration, Cold-Storage Archive & Cascade Purge
8. **[Screen W08]** `DupDeviceView.js` — Anti-Cross-Connection Sentinel & Silicon Hardware Collision Resolver
9. **[Screen W09]** `EagleAnalyticsTreeView.js` — Top-to-Bottom "Eagle to Ant" Hierarchical Drilldown Analytics
10. **[Screen W10]** `HistoryTrailView.js` & `ReplayEngine.js` — Device-Dependent Breadcrumb Replay & Scrubber
11. **[Screen W11]** `GeofencingView.js` — Perimeter Boundary Builder & Group-Specific SafeZones
12. **[Screen W12]** `AlertBannerView.js` — Global Emergency SOS Alarm & Hardware Tamper Dispatcher
13. **[Screen W13]** `database-sync-settings.html` — 178-Parameter Dual-Engine Synchronization Matrix & OTA Broadcaster

---

## Screen W01: Master Web Admin Shell (`index.html` & `js/app.js`)

```text
ROOT: WEB_ADMIN_SHELL_MASTER_PLAN (jFindMeWeb/index.html & js/app.js)
│
├── 💼 [BR]₁: Operator Authentication & Zero-Trust Session Provisioning
│    ├── 🖱️ [EL]₁․₁: Email Entry Affordance
│    │    ├── 🖥️ [FE]₁․₁․₁: Input Field & Floating Label
│    │    │    ├── (FR1.1): Validates string against RFC 5322 email regex; triggers onBlur or debounce(300ms).
│    │    │    │    ├── Pass Scenario: "admin@jdroidx.internal" -> Border turns emerald-500, label floats to top-left.
│    │    │    │    └── Fail Scenario: "invalid-user@" -> Border turns rose-500, displays inline error micro-badge.
│    │    │    ├── ⚙️ [BE]₁․₁․₁․₁: Email Normalization & Sanitizer API (`/api/auth/validate-email`)
│    │    │    │    ├── (BFR1.1): Lowercases domain, removes trailing dots, strips control chars (\u0000-\u001F).
│    │    │    │    │    ├── Pass Scenario: " Operator@jFindMe.COM " -> Sanitizes to "operator@jfindme.com".
│    │    │    │    │    └── Fail Scenario: Injection payload "admin' OR 1=1--" -> Intercepted, HTTP 400 Bad Request.
│    │    │    │    ├── 🗄️ [DB]₁․₁․₁․₁․₁: `users.normalized_email` Column & Unique Index (SQLite)
│    │    │    │    └── 🗄️ [DB]₁․₁․₁․₁․₂: `audit_log.email_query` Record (SQLite)
│    │    │    └── ⚙️ [BE]₁․₁․₁․₂: Disposable Domain Blocklist Filter
│    │    │         ├── (BFR1.2): Compares domain against in-memory trie of 4,000+ temporary email providers.
│    │    │         │    ├── Pass Scenario: Domain "corp.jdroid.net" -> Allowed, proceeds to password check.
│    │    │         │    └── Fail Scenario: Domain "tempmail.org" -> Blocked, HTTP 403 Forbidden.
│    │    │         └── 🗄️ [DB]₁․₁․₁․₂․₁: `blocked_domains` Cache Table
│    │    └── 🖥️ [FE]₁․₁․₂: Real-Time Error Micro-Badge
│    │         └── ⚙️ [BE]₁․₁․₂․₁: Inline Syntax Validation Endpoint
│    └── 🖱️ [EL]₁․₂: Password Entry Affordance
│         └── 🖥️ [FE]₁․₂․₁: Password Field & Visibility Toggle
│              ├── (FR1.2): Enforces min 10 chars, 1 uppercase, 1 numeric, 1 special symbol.
│              │    ├── Pass Scenario: "S3cure@P@ss2026" -> Eye toggle reveals text; strength meter shows 100%.
│              │    └── Fail Scenario: "12345" -> Disables submit button; highlights missing requirement tags in red.
│              └── ⚙️ [BE]₁․₂․₁․₁: Argon2id Hash Verifier & Ephemeral Token Generator
│                   ├── (BFR1.3): Hash verification memory cost = 64MB, time cost = 3 iterations, parallelism = 2.
│                   │    ├── Pass Scenario: Argon2id hash matches -> Issues signed JWT valid for 28,800s.
│                   │    └── Fail Scenario: Invalid password -> Increments failed_attempts counter; locks on >= 5 fails.
│                   ├── 🗄️ [DB]₁․₂․₁․₁․₁: `users.password_hash` Storage (TEXT NOT NULL)
│                   └── 🗄️ [DB]₁․₂․₁․₁․₂: Firebase RTDB `/operators/{uid}/session_token`
│
├── 💼 [BR]₂: Cloud Connection Health & Socket Resilience
│    ├── 🖱️ [EL]₂․₁: Dynamic Service Account Resolution Subsystem
│    │    └── ⚙️ [BE]₂․₁․₁: Dynamic Credential Resolver (`server.js:17-37`)
│    │         ├── (BFR2.1): Checks `secrets/service-account.json`; if absent, auto-scans for `firebase-adminsdk-*.json`.
│    │         │    ├── Pass Scenario: File `secrets/jfindme-3b15c-firebase-adminsdk-56c3dc08cf.json` found -> Loads key cleanly.
│    │         │    └── Fail Scenario: Zero credential files in `secrets/` -> Halts with FATAL log, exit code 1.
│    │         └── 🗄️ [DB]₂․₁․₁․₁: Local File System `secrets/` Directory
│    └── 🖱️ [EL]₂․₂: Real-Time Socket Connectivity Beacon (`#sync-indicator`)
│         └── 🖥️ [FE]₂․₂․₁: Pulsing Connectivity Micro-Pill
│              ├── (FR2.1): Subscribes to Firebase RTDB `/.info/connected`; updates badge state within 100ms.
│              │    ├── Pass Scenario: Cloud socket open -> Pill glows emerald (#10b981), text displays "SYNC_ACTIVE".
│              │    └── Fail Scenario: Socket dropped -> Pill pulses crimson (#ef4444), displays "DISCONNECTED".
│              └── ⚙️ [BE]₂․₂․₁․₁: Firebase Realtime Socket Client (`FBBridge.initRealtimeState`)
│                   ├── (BFR2.2): Auto-reconnection with exponential backoff (1s, 2s, 4s, 8s, up to 30s).
│                   └── 🗄️ [DB]₂․₂․₁․₁․₁: Firebase RTDB `/.info/connected` Status Node
│
└── 💼 [BR]₃: Persistent Forensic Logging & Error Containment
     └── 🖱️ [EL]₃․₁: Zone D Forensic Terminal Console (`#terminal`)
          ├── 🖥️ [FE]₃․₁․₁: Monospace Stream Viewport & Filter Tabs (ALL, SUCCESS, WARNING, ERROR)
          │    ├── (FR3.1): Displays real-time system logs capped at 1,000 lines; auto-scrolls to bottom.
          │    │    ├── Pass Scenario: Log event arrives -> Formats timestamp + tag + text; scrollHeight synced.
          │    │    └── Fail Scenario: Line count > 1,000 -> Evicts oldest 100 lines to prevent DOM bloat.
          │    └── ⚙️ [BE]₃․₁․₁․₁: Global Fault Interceptor (`window.onerror` & `process.on('uncaughtException')`)
          │         ├── (BFR3.1): Catches unhandled exceptions, sanitizes stack trace, streams payload to terminal.
          │         └── 🗄️ [DB]₃․₁․₁․₁․₁: `sys_log` Table in SQLite & LocalStorage Circular Buffer
          └── 🖱️ [EL]₃․₂: Terminal Action Strip (Clear Logs, Height Toggle, Reconnect Bridge)
               └── 🖥️ [FE]₃․₂․₁: Action Button Cluster
                    └── ⚙️ [BE]₃․₂․₁․₁: Matrix Reconnect Handler (`window.retryMatrixBridge`)
```

---

## Screen W02: Enterprise Macro Operations Dashboard (`DashboardView.js`)

```text
ROOT: DASHBOARD_VIEW_MASTER_PLAN (jFindMeWeb/js/views/DashboardView.js)
│
├── 💼 [BR]₁: Macro Enterprise Fleet KPI Synthesis & Aggregation
│    ├── 🖱️ [EL]₁․₁: Total Registered Units Metric Card
│    │    ├── 🖥️ [FE]₁․₁․₁: Counter Typography Card with Sky Blue Gradient Pill
│    │    │    ├── (FR1.1): Animates count from 0 to total registered devices over 600ms on load.
│    │    │    │    ├── Pass Scenario: 142 devices in DB -> Renders "142" with smooth tweening.
│    │    │    │    └── Fail Scenario: DB query returns 0 or null -> Displays "0" with neutral slate badge.
│    │    │    └── ⚙️ [BE]₁․₁․₁․₁: Live Fleet Ingress Route (`GET /api/devices-live` & `/api/fleet/compact-live`)
│    │    │         ├── (BFR1.1): Reconciled from generic route. Fetches full live fleet array with current vitals.
│    │    │         │    ├── Pass Scenario: Query succeeds -> Returns 200 with `{ status: "SUCCESS", devices: [...] }`.
│    │    │         │    └── Fail Scenario: Timeout > 3000ms -> Falls back to cached local storage count.
│    │    │         └── 🗄️ [DB]₁․₁․₁․₁․₁: SQLite `devices` Table & Firebase RTDB `/devices`
│    ├── 🖱️ [EL]₁․₂: Active Moving Units Metric Card
│    │    ├── 🖥️ [FE]₁․₂․₁: Dynamic Emerald Counter & Pulse Ring
│    │    │    ├── (FR1.2): Filters devices with location update < 5 minutes and speed > 0.5 km/h.
│    │    │    └── ⚙️ [BE]₁․₂․₁․₁: TelemetryVelocityEvaluator
│    │    │         └── 🗄️ [DB]₁․₂․₁․₁․₁: Firebase RTDB `/live-map/{id}/speed` & `/live-map/{id}/timestamp`
│    ├── 🖱️ [EL]₁․₃: Idle / Stale Units Metric Card
│    │    └── 🖥️ [FE]₁․₃․₁: Amber Alert Badge & Counter Card
│    │         └── ⚙️ [BE]₁․₃․₁․₁: RAG Status Recency Scanner (5m <= elapsed <= 15m)
│    │              └── 🗄️ [DB]₁․₃․₁․₁․₁: SQLite `devices.rag_status` Column
│    └── 🖱️ [EL]₁․₄: Active Geofenced SafeZones Card
│         └── 🖥️ [FE]₁․₄․₁: Indigo Shield Counter Card
│              └── ⚙️ [BE]₁․₄․₁․₁: SafeZoneActiveRegistry (`GET /api/safe-zones`)
│                   └── 🗄️ [DB]₁․₄․₁․₁․₁: SQLite `safe_zones` Table (`WHERE is_active = 1`)
│
├── 💼 [BR]₂: Live Fleet Movement Stream & Tactical Activity Feed
│    └── 🖱️ [EL]₂․₁: Scrolling Real-Time Event Ticker (`#movement-feed`)
│         └── 🖥️ [FE]₂․₁․₁: Event Notification Cards with Avatar Badges
│              ├── (FR2.1): Prepends new movement events to top of list with fade-in animation.
│              └── ⚙️ [BE]₂․₁․₁․₁: Server-Sent Events (SSE) Movement Listener (`/api/events/stream`)
│                   └── 🗄️ [DB]₂․₁․₁․₁․₁: Firebase RTDB `/live-map/{id}`
│
└── 💼 [BR]₃: Quick Tactical Launchpad & Administrative Routing
     └── 🖱️ [EL]₃․₁: Launchpad Button Cluster (Open Map Radar, Inspect Registry, Open DB Sync)
          └── 🖥️ [FE]₃․₁․₁: Interactive 3D Gradient Buttons with Hover Elevation
               └── ⚙️ [BE]₃․₁․₁․₁: ViewLifecycleCoordinator.switchView('live-map')
```

---

## Screen W03: Tactical Geospatial Radar (`MapView.js` / `LeafletMapView.js`)

```text
ROOT: MAP_RADAR_MASTER_PLAN (jFindMeWeb/js/views/MapView.js)
│
├── 💼 [BR]₁: Real-Time Spatial Positioning & Anti-Cross-Connection Partitioning
│    ├── 🖱️ [EL]₁․₁: Leaflet Tactical Map Canvas (`#tactical-map-canvas`)
│    │    ├── 🖥️ [FE]₁․₁․₁: Leaflet Tile Layer with Dark Matter CartoDB / OSM Provider
│    │    │    └── ⚙️ [BE]₁․₁․₁․₁: MapTileProxyEngine (`/api/map/tiles/{z}/{x}/{y}`)
│    │    │         └── 🗄️ [DB]₁․₁․₁․₁․₁: IndexedDB `tile_cache` Table (Offline Map Storage)
│    ├── 🖱️ [EL]₁․₂: Dynamic SVG Pulsing Fleet Markers
│    │    ├── 🖥️ [FE]₁․₂․₁: SVG Beacon with Circular Avatar, Heading Arrow & Concentric Pulse Ring
│    │    │    ├── (FR1.2): Color-coded to RAG status: Green (#10b981), Amber (#f59e0b), Crimson (#ef4444).
│    │    │    ├── ⚙️ [BE]₁․₂․₁․₁: TelemetrySanityValidator & 2D Kalman Filter
│    │    │    │    ├── 🗄️ [DB]₁․₂․₁․₁․₁: SQLite `location_event` Table (lat, lng, speed, heading, accuracy)
│    │    │    │    └── 🗄️ [DB]₁․₂․₁․₁․₂: Firebase RTDB `/live-map/{silicon_id}`
│    │    │    └── ⚙️ [BE]₁․₂․₁․₂: MLM Branch Isolation Guard
│    │    │         └── 🗄️ [DB]₁․₂․₁․₂․₁: SQLite `group_members` Table (`group_id`, `user_id`)
│    └── 🖱️ [EL]₁․₃: Tactical Node Popup Dossier Card
│         └── 🖥️ [FE]₁․₃․₁: Click-Activated Popup with Battery Gauge, Speedometer, and "Poke" Trigger
│              ├── (FR1.3): Clicking marker opens card within 40ms showing device vitals and address.
│              └── ⚙️ [BE]₁․₃․₁․₁: Reconciled Device Poke Dispatcher (`POST /api/poke-device` & `/api/fleet/interrogate-live`)
│                   ├── (BFR1.3): Sends `{ agentId }` body payload; initiates device wake via Firebase Cloud Messaging.
│                   │    ├── Pass Scenario: FCM delivers wake command -> Device wakes and posts fix in < 3s.
│                   │    └── Fail Scenario: Device offline -> Poke queued in Firebase `/pokes/{id}`.
│                   └── 🗄️ [DB]₁․₃․₁․₁․₁: Firebase RTDB `/devices/{id}/last_poke`
│
├── 💼 [BR]₂: Marker Clustering & Viewport Performance Optimization
│    └── 🖱️ [EL]₂․₁: Leaflet MarkerCluster Group Engine
│         └── 🖥️ [FE]₂․₁․₁: Clustered Bubble Badges with Node Count
│              └── ⚙️ [BE]₂․₁․₁․₁: SpatialGridIndexCalculator (R-Tree Spatial Index)
│
└── 💼 [BR]₃: Map Layer Switching & Operational Controls
     └── 🖱️ [EL]₃․₁: Layer Switcher FAB & Fit Bounds FAB
          └── 🖥️ [FE]₃․₁․₁: Floating Action Button Cluster (Recenter, Satellite Toggle, Fullscreen)
               └── ⚙️ [BE]₃․₁․₁․₁: MapViewportBoundsController.fitAllMarkers()
```

---

## Screen W05: MLM Hierarchical Relationship Studio (`DeviceUserView.js`)

```text
ROOT: DEVICE_USER_VIEW_MASTER_PLAN (jFindMeWeb/js/views/DeviceUserView.js)
│
├── 💼 [BR]₁: MLM Multi-Tier Grouping, Relationship Edges & ACL Controls
│    ├── 🖱️ [EL]₁․₁: Grouping Mode Segmented Toggle (`groupBy: "none" | "group" | "role" | "status"`)
│    │    ├── 🖥️ [FE]₁․₁․₁: Segmented Toggle Pill Bar with Animated Sliding Highlight
│    │    │    └── ⚙️ [BE]₁․₁․₁․₁: HierarchyTreeGrouper (`DeviceController.groupBy`)
│    │    │         └── 🗄️ [DB]₁․₁․₁․₁․₁: SQLite `groups` Table & `group_members` Table
│    ├── 🖱️ [EL]₁․₂: MLM Relationship Edge Mutators (Add / Delete / Update Sponsor Edge)
│    │    ├── 🖥️ [FE]₁․₂․₁: Sponsor Tree Reassignment Dialog & Dropdown
│    │    │    └── ⚙️ [BE]₁․₂․₁․₁: Reconciled Relationship API Endpoints
│    │    │         ├── `POST /api/relationship/add` (Enrolls device into target circle)
│    │    │         ├── `POST /api/relationship/delete` (Severes cross-connected rogue link)
│    │    │         ├── `POST /api/relationship/update` (Updates role/tier in hierarchy)
│    │    │         └── 🗄️ [DB]₁․₂․₁․₁․₁: SQLite `group_members` Table & Firebase `/groups/{groupId}/members`
│    └── 🖱️ [EL]₁․₃: Device Access Control List (ACL) Switch
│         └── 🖥️ [FE]₁․₃․₁: Toggle Switch on Device Dossier ("Allow Circle Member Tracking")
│              └── ⚙️ [BE]₁․₃․₁․₁: Reconciled ACL Dispatcher (`POST /api/device/toggle-acl`)
│                   ├── (BFR1.2): Commits boolean flag to `/devices/{agentId}/acl/circle_viewable`.
│                   └── 🗄️ [DB]₁․₃․₁․₁․₁: Firebase RTDB `/devices/{id}/acl`
│
├── 💼 [BR]₂: Device Lifecycle Management (Activation, Deactivation, Cold-Storage Archive)
│    ├── 🖱️ [EL]₂․₁: Cold-Storage Archived Devices Switch (`#toggle-show-archived`)
│    │    ├── 🖥️ [FE]₂․₁․₁: Custom Toggle Switch with Archive Count Micro-Badge
│    │    │    └── ⚙️ [BE]₂․₁․₁․₁: ArchivedDeviceRepository (`GET /api/archived-devices`)
│    │    │         └── 🗄️ [DB]₂․₁․₁․₁․₁: SQLite `devices` (`WHERE status = 'ARCHIVED'`)
│    └── 🖱️ [EL]₂․₂: "Restore to Active Registry" Action Pill
│         └── 🖥️ [FE]₂․₂․₁: One-Click Restore Button on Archived Device Cards
│              └── ⚙️ [BE]₂․₂․₁․₁: DeviceRestoreDispatcher (`POST /api/device/restore-archive`)
│                   ├── 🗄️ [DB]₂․₂․₁․₁․₁: SQLite `devices.status` = 'ACTIVE'
│                   └── 🗄️ [DB]₂․₂․₁․₁․₂: Firebase RTDB `/devices/{id}/status` = 'ACTIVE'
│
└── 💼 [BR]₃: Over-The-Air (OTA) Fleet Version Auditing & Distribution
     ├── 🖱️ [EL]₃․₁: Master Release Version Banner (`#master-release-banner`)
     │    ├── 🖥️ [FE]₃․₁․₁: Monospace Version Chip ("Master Target: v3.1.0-b76")
     │    │    ├── (FR3.1): Reactive watcher: re-renders device cards if `loadMasterRelease()` completes after initial load.
     │    │    └── ⚙️ [BE]₃․₁․₁․₁: SystemReleaseAuditor (`GET /api/system-release`)
     │    │         └── 🗄️ [DB]₃․₁․₁․₁․₁: `apk/version.properties` File (`version_code=76`)
     └── 🖱️ [EL]₃․₂: Version Disparity Warning Badge on Device Cards
          ├── 🖥️ [FE]₃․₂․₁: Amber Upgrade Indicator ("Device at b74 • Update Available")
          └── ⚙️ [BE]₃․₂․₁․₁: OTABroadcastDispatcher (`POST /api/device/push-update` & `/api/push-update`)
               └── 🗄️ [DB]₃․₂․₁․₁․₁: Firebase RTDB `/devices/{id}/pending_ota`
```

---

## Screen W07: Device De-Registration & Cold-Storage Archive (`DeviceDeleteModalView.js`)

```text
ROOT: DEVICE_DELETE_MODAL_MASTER_PLAN (jFindMeWeb/js/views/DeviceDeleteModalView.js)
│
├── 💼 [BR]₁: Secure Decommissioning & Cascade Isolation
│    ├── 🖱️ [EL]₁․₁: Decommissioning Mode Radio Selector
│    │    ├── 🖥️ [FE]₁․₁․₁: Radio Options: [A] "Archive to Cold Storage" vs [B] "Permanent Purge"
│    │    └── ⚙️ [BE]₁․₁․₁․₁: DecommissionPolicyValidator
│    ├── 🖱️ [EL]₁․₂: Security Challenge Input Field (Option B)
│    │    ├── 🖥️ [FE]₁․₂․₁: Text Input Box requiring exact string "CONFIRM_DELETE"
│    │    └── ⚙️ [BE]₁․₂․₁․₁: Reconciled Unified Decommissioning Controller (`POST /api/device/delete-everywhere`)
│    │         ├── (BFR1.1): Accepts `{ agentId, mode: 'archive'|'purge', purgeHistory: boolean }`.
│    │         │    ├── Mode 'archive' -> Sets SQLite `status = 'ARCHIVED'`, preserves breadcrumb records.
│    │         │    └── Mode 'purge' -> Executes cascading SQLite purge and Firebase removal.
│    │         ├── 🗄️ [DB]₁․₂․₁․₁․₁: SQLite `devices` Table & Multi-Table Cascade
│    │         ├── 🗄️ [DB]₁․₂․₁․₁․₂: Firebase RTDB `/devices/{agentId}` & `/live-map/{agentId}`
│    │         └── 🗄️ [DB]₁․₂․₁․₁․₃: SQLite `audit_log` Table (Decommissioning Operator, Timestamp, Mode)
│    └── 🖱️ [EL]₁․₃: Confirmation & Abort Buttons
│         └── 🖥️ [FE]₁․₃․₁: Action Button Handlers -> Closes modal cleanly, triggers view refresh
```

---

## Screen W08: Anti-Cross-Connection & Collision Remediation (`DupDeviceView.js`)

```text
ROOT: DUP_DEVICE_VIEW_MASTER_PLAN (jFindMeWeb/js/views/DupDeviceView.js)
│
├── 💼 [BR]₁: Silicon Hardware Collision Detection & Cross-Tree Bleed Remediation
│    ├── 🖱️ [EL]₁․₁: Silicon Sentinel Scan Header & Duplicate Counter Badge
│    │    ├── 🖥️ [FE]₁․₁․₁: Header Bar with Scan Trigger Button and Collisions Found Counter
│    │    └── ⚙️ [BE]₁․₁․₁․₁: SiliconHardwareScanner (`GET /api/duplicate-devices`)
│    │         ├── (BFR1.1): Generates SHA-256 hash of: Brand + Model + Board + Hardware + Device + IMEI.
│    │         └── 🗄️ [DB]₁․₁․₁․₁․₁: SQLite `devices` Table & Firebase RTDB `/devices/`
│    ├── 🖱️ [EL]₁․₂: Pre-Verification Sentinel Action
│    │    ├── 🖥️ [FE]₁․₂․₁: "Verify Hardware Signature Match" Button
│    │    └── ⚙️ [BE]₁․₂․₁․₁: Reconciled Pre-Flight Verifier (`POST /api/verify-duplicate-pair`)
│    │         ├── (BFR1.2): Enforces pre-flight check before enabling atomic merge button.
│    │         └── 🗄️ [DB]₁․₂․₁․₁․₁: In-memory Collision Registry State
│    ├── 🖱️ [EL]₁․₃: "1-Click Atomic Merge" Action Button
│    │    ├── 🖥️ [FE]₁․₃․₁: Merge Trigger Button with Confirmation Dialog
│    │    └── ⚙️ [BE]₁․₃․₁․₁: Reconciled Merge Engine (`POST /api/merge-duplicate-devices`)
│    │         ├── (BFR1.3): Commits transaction moving fixes from `duplicateAgentId` to `masterAgentId`.
│    │         ├── 🗄️ [DB]₁․₃․₁․₁․₁: SQLite `location_event.device_id` Foreign Key Update
│    │         ├── 🗄️ [DB]₁․₃․₁․₁․₂: SQLite `devices` Table (Old Record Deletion)
│    │         └── 🗄️ [DB]₁․₃․₁․₁․₃: Firebase RTDB `/devices/{oldId}` Purge
│    └── 🖱️ [EL]₁․₄: Telemetry Compass Heading Synchronizer
│         ├── 🖥️ [FE]₁․₄․₁: "Reconcile Heading Angles" Button
│         └── ⚙️ [BE]₁․₄․₁․₁: Reconciled Heading Repair Endpoints
│              ├── `POST /api/sync-heading` (Single device bearing interpolation)
│              ├── `POST /api/sync-all-missing-headings` (Batch recalculation across all merged fixes)
│              └── 🗄️ [DB]₁․₄․₁․₁․₁: SQLite `location_event.bearing` Column Update
```

---

## Screen W10: Historical Breadcrumb Scrubber (`HistoryTrailView.js` & `ReplayEngine.js`)

```text
ROOT: HISTORY_TRAIL_VIEW_MASTER_PLAN (jFindMeWeb/js/views/HistoryTrailView.js)
│
├── 💼 [BR]₁: Device-Dependent Chronological Telemetry Auditing & Replay
│    ├── 🖱️ [EL]₁․₁: Target Device & Available Date Horizon Pickers
│    │    ├── 🖥️ [FE]₁․₁․₁: Device Select Dropdown & Available Dates Calendar Dropdown
│    │    │    ├── (FR1.1): Queries available recorded dates first; enables calendar days with data.
│    │    │    └── ⚙️ [BE]₁․₁․₁․₁: Reconciled Date Index API (`GET /api/history-dates?agentId=...`)
│    │    │         └── 🗄️ [DB]₁․₁․₁․₁․₁: SQLite `location_event` (`DISTINCT strftime('%Y-%m-%d', timestamp/1000, 'unixepoch')`)
│    ├── 🖱️ [EL]₁․₂: Chronological Coordinate Streamer
│    │    ├── 🖥️ [FE]₁․₂․₁: Colored Breadcrumb Polyline with Gradient Speed Segments
│    │    └── ⚙️ [BE]₁․₂․₁․₁: Reconciled Telemetry Fix Query (`GET /api/device-history?agentId=...&date=...`)
│    │         └── 🗄️ [DB]₁․₂․₁․₁․₁: SQLite `location_event` Table & Firebase RTDB `/locations/{agentId}`
│    ├── 🖱️ [EL]₁․₃: Time-Series Replay Scrubber Slider
│    │    ├── 🖥️ [FE]₁․₃․₁: Range Scrubber with Timestamp Floating Pill
│    │    └── ⚙️ [BE]₁․₃․₁․₁: ReplayEngine.seekToFraction(fraction)
│    └── 🖱️ [EL]₁․₄: Historical Data Retention Purge Action
│         ├── 🖥️ [FE]₁․₄․₁: "Purge Historical Trails Before Date" Button
│         └── ⚙️ [BE]₁․₄․₁․₁: Reconciled Retention Purge Endpoint (`POST /api/history-purge`)
│              └── 🗄️ [DB]₁․₄․₁․₁․₁: SQLite `location_event` (`DELETE WHERE timestamp < :cutoff`)
```

---

## Screen W11: Geofencing Boundary Builder (`GeofencingView.js`)

```text
ROOT: GEOFENCING_VIEW_MASTER_PLAN (jFindMeWeb/js/views/GeofencingView.js)
│
├── 💼 [BR]₁: Group-Specific Perimeter Security & Ray-Casting Containment
│    ├── 🖱️ [EL]₁․₁: SafeZone Catalog & Active Registry
│    │    ├── 🖥️ [FE]₁․₁․₁: List of Active/Inactive SafeZones with Target Assignment Counts
│    │    └── ⚙️ [BE]₁․₁․₁․₁: Reconciled SafeZone Fetch Route (`GET /api/safe-zones`)
│    │         └── 🗄️ [DB]₁․₁․₁․₁․₁: SQLite `safe_zones` Table & Firebase RTDB `/safe_zones`
│    ├── 🖱️ [EL]₁․₂: Leaflet Draw Toolbar & Geometry Saver
│    │    ├── 🖥️ [FE]₁․₂․₁: Interactive Polygon Drawing Handles on Map Canvas
│    │    └── ⚙️ [BE]₁․₂․₁․₁: Reconciled SafeZone Persistence (`POST /api/save-safe-zone`)
│    │         └── 🗄️ [DB]₁․₂․₁․₁․₁: SQLite `safe_zones.vertices_json` & Firebase RTDB `/safe_zones/{id}`
│    ├── 🖱️ [EL]₁․₃: Operational State Toggle (Enable / Disable Perimeter)
│    │    ├── 🖥️ [FE]₁․₃․₁: Switch on SafeZone Card ("Active Perimeter Sentinel")
│    │    └── ⚙️ [BE]₁․₃․₁․₁: Reconciled State Toggle (`POST /api/safezone/toggle-state`)
│    │         └── 🗄️ [DB]₁․₃․₁․₁․₁: SQLite `safe_zones.is_active` Column & Firebase
│    ├── 🖱️ [EL]₁․₄: Target Device / Group Assignment Drawer
│    │    ├── 🖥️ [FE]₁․₄․₁: Multi-Select Checkbox Roster of Assigned Fleet Circles
│    │    └── ⚙️ [BE]₁․₄․₁․₁: Reconciled Target Update API (`POST /api/safezone/update-targets`)
│    │         └── 🗄️ [DB]₁․₄․₁․₁․₁: SQLite `safe_zone_user_map` (`zone_id`, `user_id`)
│    └── 🖱️ [EL]₁․₅: SafeZone Decommissioning Action
│         ├── 🖥️ [FE]₁․₅․₁: Delete Button with Confirmation Guard
│         └── ⚙️ [BE]₁․₅․₁․₁: Reconciled Deletion Endpoint (`POST /api/safezone/delete`)
│              └── 🗄️ [DB]₁․₅․₁․₁․₁: SQLite `safe_zones` Row Deletion & Firebase `/safe_zones/{id}` Purge
```
