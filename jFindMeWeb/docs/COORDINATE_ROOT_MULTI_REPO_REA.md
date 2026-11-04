# 🌐 Coordinate ROOT: System Architecture & Multi-Repository REA Topology
## jFindMe / jButterfly Enterprise Ecosystem & jFindMeWeb Subsystem
> **Enterprise Standard v5.0** | **Codename:** `jDroid-X` | **Operator:** `JEET`  
> **Repository Target:** `C:\Users\jiten\jAnitGravity\jFindMe\jFindMeWeb`  
> **Coordinate:** `ROOT` in `ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB`  
> **Methodology:** Dynamic OOPS-based MVC Architecture & Reverse Engineering Analysis (REA)

---

## 1. Enterprise System Landscape & High-Level Context

The **jFindMe Enterprise Ecosystem** is a multi-tier spatial intelligence, real-time telemetry, SafeZone perimeter defense, baseband cellular triangulation, and forensic device synchronization platform. The ecosystem operates across two distinct primary execution environments interconnected through real-time cloud data pipelines:

1. **Native Android Edge Client (`jButterFly`)**: High-resilience Android native client (Kotlin/Java) operating with zero third-party telemetry bloatware, running 24/7 background foreground services (`TrackingService`, `SIMTrackingService`), an offline SQLite ledger (17 normalized tables), and local Kalman filter stabilization.
2. **Web Mission Control (`jFindMeWeb`)**: High-performance tactical tracking and governance console (ES6 Modular MVC, Node.js Express Proxy, Leaflet/Google Maps radar matrix) supporting live spatial tracking, historical breadcrumb playback, 20-module/178-parameter dual-engine synchronization, and deep forensic auditing.
3. **Continuous Release & Packaging Bridge (`apk/sync_service.py`, `tools/`)**: Background automation daemon continuously managing SHA-256 certificate hashing, version incrementation, and binary delta distribution (`jdelta_generator.py`).

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB COORDINATE TREE                     │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                  ┌──────────────────────────────┼──────────────────────────────┐
                  ▼                              ▼                              ▼
         [ Coordinate BR ]              [ Coordinate EL ]              [ Coordinate FE ]
       Business Rules & Domain        Element UI Design System       Frontend Presentation
       ├── 22 Enterprise Agents       ├── UiArchitect L7 Standards   ├── 11 Web Screens (W01-W11)
       ├── Silicon Root Hardware SSOT ├── 5-Stage Universal Sizing   ├── ES6 Modular MVC Engine
       ├── Centralized RAG Status     ├── Dynamic Theme Palettes     ├── Responsive Viewport Grid
       └── 5-Tier Failover Mesh       └── Slide-Out Right Drawer     └── High-DPI Canvas Rendering
                  │                              │                              │
                  └──────────────────────────────┼──────────────────────────────┘
                                                 │
                  ┌──────────────────────────────┴──────────────────────────────┐
                  ▼                                                             ▼
         [ Coordinate BE ]                                             [ Coordinate DB ]
       Backend & Operating Engines                                   Dual-Engine Storage
       ├── Dual-Server Node.js Engine (Port 3000/8080)               ├── SQLite Edge DB (17 Tables)
       ├── Firebase Admin SDK Realtime Proxy                         ├── Firebase RTDB Cloud SSOT
       ├── SSE Telemetry Event Bridge                                ├── 178-Parameter Sync Matrix
       └── Diagnostic Sentinel & Sanitizer                           └── Bi-directional 1-Click Sync
```

---

## 2. Multi-Repository Topology Overview

```
C:\Users\jiten\jAnitGravity\jFindMe\
├── .agent/                      <-- Enterprise Agent Registry & Protocol Mapping
│   ├── enterprise-22-agents/    <-- Program Mapping v5.0 master relationship tables
│   └── .agents/rules/           <-- rules4agents.md & taskforce behavioral guidelines
├── apk/                         <-- Production APK output & sync_service.py release daemon
├── app/                         <-- Legacy module wrapper redirecting to jButterFly
├── docs/                        <-- Multi-repository Enterprise documentation suite
│   ├── design/                  <-- High-level designs, screen settings, telemetry rulebooks
│   ├── history/                 <-- Audit reports, threading validation, RAG reports
│   ├── planning/                <-- Requirements specifications and roadmaps
│   └── reference/               <-- Architecture guides and UI blueprints
├── firebase/                    <-- Cloud security rules & schema configurations
├── jButterFly/                  <-- Core Android production native application
├── jFindMeWeb/                  <-- Web Mission Control, tactical radar, sync console (Target)
├── legacy/                      <-- Quarantined historical code under Chesterton's Fence
├── logs/                        <-- Crash buffer dumps, compile checks, and runtime traces
├── scratch/                     <-- Scratch scripts, test assets, temporary working files
└── tools/                       <-- Binary diff utilities (jdelta_generator.py)
```

---

## 3. Deep Folder-by-Folder REA for `jFindMeWeb`

Below is the exhaustive, folder-by-folder reverse engineering analysis (REA) detailing every file, class, responsibility, dependency, and data flow within `jFindMeWeb`:

### 3.1 Root Files
- **`index.html`**: Master Web Mission Control Single Page Application (SPA). Mounts Google-style jButterfly branding, responsive left sidebar navigation, topbar status indicators (`SYNC_ACTIVE`, `project-id-label`), Zone C main workspace (`#workspace-root`), and Zone D persistent forensic audit terminal (`#terminal`).
- **`database-sync-settings.html`**: Dual-Engine Database Synchronization Matrix (7,087 lines). Contains the 178-parameter bi-directional synchronization console organized into 20 functional modules, complete with a slide-out right drawer (2-column smart-fit layout), filter chips, and live SQLite-to-Firebase sync execution.
- **`launcher.html`**: Desktop and offline browser launcher. Handles cross-origin security bypasses, automated port detection (8080 vs 3000), and protocol registration redirects.
- **`launch.bat`**: Windows quick-launch script. Bootstraps both Node.js servers concurrently (`static_server.js` on port 8080 and `server.js` on port 3000).
- **`package.json` & `package-lock.json`**: NPM project definition declaring dependencies: `express`, `cors`, `firebase-admin`, and runtime configurations.

### 3.2 `.github/` Directory
- **`.github/agents/`**:
  - `firebase-data-engineer.agent.md`: Domain instructions for real-time database schema management, rule enforcement, and indexing.
  - `gps-telemetry-validator.agent.md`: Telemetry validation rules (speed anomaly detection, Kalman filtering thresholds).
  - `map-gis-specialist.agent.md`: GIS rendering, spatial queries, Leaflet/Google Maps integration policies.
  - `telemetry-dashboard-designer.agent.md`: UI/UX design tokens, glassmorphism, responsive viewport compliance.
- **`.github/instructions/`**:
  - `firebase-service.instructions.md`: Rules for integrating `FBBridge` and handling connection retries.
  - `map-view.instructions.md`: Map lifecycle, marker clustering, polyline interpolation specifications.
  - `uix-component.instructions.md`: UiArchitect atomic control rules (Switches, Segmented Toggles, Flippers).
- **`.github/prompts/`**:
  - `audit-map-performance.prompt.md`: Diagnostic prompts for map frame-rate evaluation.
  - `debug-geofence.prompt.md`: Ray-Casting algorithm verification prompts.
  - `review-firebase-schema.prompt.md`: Firebase RTDB schema alignment check prompts.
- **`.github/skills/`**:
  - `firebase-integration/`: Reusable scripts and guidelines for cloud database connectivity.
  - `map-development/`: Tactical GIS and spatial math helper assets.
  - `performance-profiling/`: Web runtime memory profiling and leak detection scripts.

### 3.3 `assets/` Directory
- **`assets/avatars/`**: Contains numbered avatar assets (`1.png` to `20.png`, `avatar-1.png` to `avatar-5.png`, `default-avatar.svg`) for user profiles, map pin icons, and roster representations.
- **`assets/icons/`**: SVG glyphs and navigation icons (map pins, battery status icons, satellite lock indicators, SOS beacon emblems).
- **`assets/sounds/`**: Acoustic notification audio assets (`sos_beacon.mp3`, `alert_chime.wav`) used for emergency dispatch audio alerts.

### 3.4 `config/` Directory
- Contains application deployment variables, environment port mappings, and operational flag definitions.

### 3.5 `css/` Directory
- **`css/variables.css`**: Master Design Tokens. Defines CSS Custom Properties for theme colors (Dark/Light), typography (Plus Jakarta Sans, JetBrains Mono), spacing scale (4px grid), and border radii (`--radius-sm: 6px`, `--radius-md: 10px`, `--radius-lg: 16px`, `--radius-xl: 20px`).
- **`css/enterprise.css`**: Global enterprise styling. Implements dark glassmorphism (`backdrop-filter: blur(18px)`), custom scrollbars, modal backdrops, typography hierarchies, card hover physics, and badge styles.
- **`css/map.css`**: Tactical GIS radar styling. Contains Leaflet map container positioning, custom SVG marker animations (crimson pulse for SOS, emerald glow for live devices), cluster bubble counters, and popup cards.
- **`css/mobile-responsive.css`**: Universal responsive viewport layout. Defines media queries across breakpoints (320px, 640px, 768px, 1024px, 1280px), collapsible mobile drawer transitions, and touch-optimized touch targets (min 44px).

### 3.6 `docs/` Directory
- Houses the operational documentation, deployment guides, architectural blueprints, RAG status reports, and system validation ledgers.

### 3.7 `js/` Directory (Core Application Logic)

#### `js/components/`
- **`DataGrid.js`**: High-performance virtualized data table component. Renders large tabular records with column sorting, real-time filtering, row pagination, and inline action buttons.
- **`DataExport.js`**: Client-side data serialization engine. Exports tabular data, device telemetry, and audit logs to CSV, JSON, and printable formats.
- **`DeviceRegistrationWizard.js`**: Step-by-step multi-stage onboarding modal wizard for enrolling new silicon nodes, configuring hardware profiles, and assigning initial SafeZones.
- **`FeedbackDialog.js`**: Accessible modal dialog system for operator confirmations, critical purge verification, and error alerts.
- **`JsonEditor.js`**: Interactive JSON inspector and live mutation tree with syntax highlighting and schema validation.
- **`KeyboardShortcuts.js`**: Global accelerator manager binding keyboard shortcuts (e.g., `M` for Live Map, `D` for Dashboard, `S` for Sync Matrix, `/` for search).
- **`SchemaView.js` & `SchemaView1.js`**: Interactive schema visualization components mapping SQLite tables and Firebase RTDB tree paths.
- **`UIControls.js`**: UiArchitect L7 Atomic UI Component Suite. Implements custom accessible controls:
  - Toggle Switch (`createSwitch`)
  - Segmented Multi-Button Toggle (`createSegmentedToggle`)
  - 3D Interactive Card Flipper (`create3DFlipper`)
  - Filterable ListBox Dropdown (`createListBoxDropdown`)
  - Multi-Select Choice Dialog (`createMultiChoiceDialog`)
  - Slide-In Notification Toast Banner (`createSlideNotification`)

#### `js/config/`
- **`AppConfig.js`**: Global client configuration object (polling frequencies, default coordinates, API base URLs).
- **`firebase-config.js`**: Public Firebase project metadata (Project ID: `jfindme-3b15c`, databaseURL).
- **`MapConfig.js`**: GIS map defaults (default zoom level 13, initial center latitude/longitude, OpenStreetMap tile URLs, satellite layer providers).

#### `js/controllers/`
- **`MapController.js`**: Primary GIS tactical controller. Orchestrates Leaflet/Google map instances, marker lifecycle, live telemetry stream bindings, GPS polyline breadcrumb rendering, and SafeZone polygon overlays.
- **`DeviceController.js`**: Fleet management controller. Handles device filtering, status calculation (Active, Stale, Inactive), device alias updates, and device deletion cascades.
- **`SafeZoneController.js`**: Perimeter boundary controller. Manages interactive drawing tools, polygon vertex editing, boundary persistence, and breach alert dispatch.
- **`SettingsController.js`**: Client-side settings coordinator. Manages radar search radius, telemetry refresh cadence, and UI display options.
- **`WebMissionControlFacades.js`**: Architectural facade unifying access to disparate sub-controllers for clean decoupling.
- **`D3TreeRenderer.js`**: D3.js visualization engine rendering collapsible hierarchical tree diagrams for Eagle Analytics and organizational fleet structures.
- **`DupTableRenderer.js`**: Specialized table renderer highlighting duplicate hardware IMEI/phone collisions and presenting merge/purge options.
- **`ReplayEngine.js`**: Chronological breadcrumb replay engine. Animates vehicle marker movements along historical polyline paths with speed playback controls (1x, 2x, 5x, 10x).

#### `js/core/`
- **`Store.js`**: Centralized reactive state store following the unidirectional data flow pattern. Emits state change events to subscribed UI views.
- **`Auth.js`**: Authentication manager handling session tokens, operator permissions, and zero-trust credentials.
- **`GeoUtils.js`**: Mathematical GIS utility suite implementing Haversine distance formula, bearing calculation, Ray-Casting algorithm for point-in-polygon tests, and coordinate formatting.
- **`TelemetryValidator.js`**: Forensic telemetry integrity guard. Validates latitude/longitude bounds (-90 to +90, -180 to +180), flags impossible speed vectors (> 250 km/h), and validates battery voltages.
- **`ErrorRecovery.js`**: Resilience sentinel implementing circuit breakers, exponential backoff retries, and network reconnection handlers.
- **`OfflineQueue.js`**: LocalStorage-backed transaction buffer storing unsynced mutations during network drops and automatically draining them upon reconnection.
- **`ThemeManager.js`**: UI theme coordinator toggling between Dark Mode (`.dark`) and Light Mode (`.light-theme`), persisting preferences to `localStorage`.

#### `js/models/`
- **`DeviceModel.js`**: Core domain entity representing a fleet device. Encapsulates hardware descriptors (IMEI, silicon root key, phone, model), telemetry (lat, lng, speed, heading, altitude, battery, signal dBm, cell tower LAC/CID), and RAG status.
- **`SafeZoneModel.js`**: Domain entity representing a geofence. Stores boundary type (Circle vs Polygon), radius/vertices, associated user IDs, and trigger rules (Enter, Exit, Dwell).
- **`SettingsModel.js`**: Configuration entity encapsulating 178 system parameters across 20 functional modules.

#### `js/services/`
- **`FirebaseService.js` (`FBBridge`)**: Enterprise communication bridge connecting to Firebase Realtime Database and the local Express proxy. Provides real-time event listeners (`onValue`), REST CRUD operations, and fallback polling.
- **`AvatarService.js`**: Profile image resolver mapping user IDs and styles to local SVG/PNG assets or generating dynamic initials badges.
- **`SanitizerService.js`**: DOMPurify-grade HTML sanitizer preventing XSS injections in operator inputs, device aliases, and log messages.
- **`GoogleMapsLoader.js`**: Asynchronous loader script injecting Google Maps JavaScript SDK when Google Maps radar mode is selected.

#### `js/views/`
- **`MapView.js`**: Container view managing map canvas switching between Leaflet and Google Maps.
- **`LeafletMapView.js`**: Concrete Leaflet.js tactical map view with animated markers, cluster groups, and polyline trails.
- **`GoogleMapView.js`**: Concrete Google Maps tactical map view with styled vector tiles and marker overlays.
- **`DashboardView.js`**: Fleet KPI metrics dashboard rendering active node counts, alert tickers, battery health distribution charts, and recent movement feeds.
- **`DeviceListView.js`**: Fleet roster table view with real-time text search, status filters, and action buttons.
- **`DeviceCardView.js`**: Detailed modal inspector card displaying individual device dossiers, baseband cellular vitals, and remote action triggers (Poke, SOS siren).
- **`DeviceEditModalView.js`**: Modal form for updating device aliases, phone numbers, and reporting cadences.
- **`DeviceDeleteModalView.js`**: High-risk confirmation dialog for de-registering devices and optionally purging historical telemetry breadcrumbs.
- **`DeviceUserView.js`**: Comprehensive user-device mapping and profile administration view.
- **`DupDeviceView.js`**: Duplicate device collision remediation interface.
- **`GeofencingView.js`**: Geofence management view providing interactive drawing tools and perimeter rule catalogs.
- **`SafeZoneCreateModalView.js`**: Dedicated modal dialog for configuring new SafeZone parameters.
- **`HistoryTrailView.js`**: Historical trajectory playback view with time-series scrubber and speed analytics gauge.
- **`EagleAnalyticsTreeView.js`**: Forensic intelligence view presenting collapsible hierarchical tree graphs of device relationships, organizations, and movement clusters.
- **`CoordinateTreeMindMapView.js`**: Interactive MindMap and Coordinate Tree Explorer visualizing the entire system specification (`ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB`) across all 32 Android screens and 11 Web screens.
- **`AlertBannerView.js`**: Global emergency alert banner rendering high-priority SOS acoustic alerts and database synchronization warnings.
- **`MapSettingsView.js`**: Fly-out drawer for tuning map display settings, search radius, and layer toggles.
- **`jButterflyHubView.js`**: Unified navigation hub linking all mission control sub-modules.

#### Root JS Engines
- **`js/app.js`**: Master Client Application Orchestrator (1,512 lines). Boots the MVC framework, registers global navigation functions (`window.showLiveMap`, `window.showDashboard`, etc.), mounts components to `#workspace-root`, binds Firebase real-time listeners, and routes forensic terminal logs.
- **`js/server.js`**: Enterprise Node.js Express API & Proxy Server (4,638 lines) running on Port 3000. Initializes Firebase Admin SDK using service account credentials, exposes REST endpoints (`/api/devices`, `/api/locations`, `/api/geofences`, `/api/sync-settings`, `/api/poke`), and streams Server-Sent Events (SSE).
- **`js/static_server.js`**: Lightweight HTTP Static File Server (Port 8080). Serves static HTML/CSS/JS assets and reverse-proxies API calls to Port 3000.

### 3.8 `references/` Directory
- Contains reference schemas, architecture diagrams, and protocol documentation.

### 3.9 `rules/` Directory
- **`rules/LOCATION_PERFORMANCE_RULES.md`**: Mission-critical GIS execution guidelines detailing GPS polling intervals, battery duty cycles, and network payload throttling.

### 3.10 `scripts/` Directory
- **`scripts/audit_active_devices.js`**: Diagnostic script querying Firebase RTDB to inspect currently active devices.
- **`scripts/inspect_avatars.js`**: Verification script checking consistency and file existence of avatar assets.
- **`scripts/inspect_chosen_5.js`**: Script inspecting specific fleet test nodes.
- **`scripts/validate-system.js`**: Automated pre-flight health check validating service account credentials, database connections, and port availability.
- **`scripts/verify_services.js`**: Verifies running status of background daemons.
- **`scripts/launch_server.bat`**: Server launch utility batch script.
- **`scripts/register_protocol.bat`**: Windows custom URL protocol handler registration script.
- **`scripts/deployment/`**:
  - `firebase-setup.js`: Automated Firebase security rules deployer and schema bootstrapper.
  - `jFireBase.bat`: Automated Node.js server launcher for Firebase proxying.
  - `run-integration-tests.js`: End-to-end integration test runner validating API endpoints and data synchronization.

### 3.11 `secrets/` Directory
- **`secrets/service-account.json` & `jfindme-3b15c-firebase-adminsdk-*.json`**: Google Cloud Service Account private key credentials for Firebase Admin SDK authentication.
- **`secrets/google-services.json`**: Client-side Firebase project configuration file.

### 3.12 `workflows/` Directory
- **`workflows/mvc-closed-loop-waterfall.md`**: Master engineering workflow document detailing the 7-Stage Closed-Loop Waterfall Sequence.
- **`workflows/ui-ux-pro-max.md`**: UI/UX design specifications, typography standards, color contrasts, and responsive layout rules.

---

## 4. Multi-Repository Integration Flow

```
[ Android Edge Device (jButterFly) ]
        │  ▲
        │  │ (Cellular LTE / Wi-Fi / SMS)
        ▼  │
[ Firebase Realtime Database (Cloud SSOT) ]
        │  ▲
        │  │ (Admin SDK WebSocket & REST)
        ▼  │
[ Node.js Express Proxy (js/server.js:3000) ]
        │  ▲
        │  │ (HTTP Reverse Proxy & SSE)
        ▼  │
[ Tactical Mission Control Browser (js/app.js:8080) ]
```

This multi-repository topology guarantees strict zero-loss telemetry persistence, sub-second radar rendering, and atomic bi-directional database synchronization across mobile and web environments.

