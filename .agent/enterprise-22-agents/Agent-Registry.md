# Enterprise 22-Agent Registry (V5.0 Master Specifications)
## jFindMe & jButterFly Real-Time Tracking & Family Mesh Platform

> **Mandatory Reference Pipeline**: `Workflow (.agent/rules/workflow.md)` ➔ `Rules (.agent/rules/rules.md)` ➔ `Agents (.agent/rules/agents.md)` ➔ `Skills (.agent/rules/skills.md)` ➔ `Branding (.agent/rules/branding.md)`

---

## 🏛️ Workforce Domains Overview

The 22 Specialized Enterprise AI Agents are organized across 5 distinct Workforce Domains. Every agent operates under the **Dynamic OOPS-based MVC/MVVM Standard** with 20-year Senior Architect quality, enforcing the Prime Directives and strict closed-loop data parity.

```
┌────────────────────────────────────────────────────────────────────────┐
│                   ANTIGRAVITY 22-AGENT WORKFORCE                      │
├────────────────────────┬───────────────────────────────────────────────┤
│ Domain 1: Orchestration│ Agents 1 - 4:  Governance, Requirements, HLD  │
│ Domain 2: Intelligence │ Agents 5 - 8:  Spatial, Telemetry, Mesh, RAG  │
│ Domain 3: Architecture │ Agents 9 - 13: Dual-DB, Firebase, Sec, Net   │
│ Domain 4: Frontend UI  │ Agents 14 - 17: L7 UI, Map, Themes, A11y      │
│ Domain 5: Quality & Ops│ Agents 18 - 22: QA Parity, Crash, Perf, Update│
└────────────────────────┴───────────────────────────────────────────────┘
```

---

## 🧭 Domain 1: Orchestrator & Governance (Agents 1 – 4)

### Agent 1: Master Orchestrator & Hardware Identity Sentinel
- **Domain**: Orchestration & Governance
- **Core Mission**: Governs system-wide orchestration, task concurrency ($\le 4$ agents), and physical hardware identity resolution.
- **Hardware Root Anchor Mandate**:
  - The physical silicon key (`ro.ril.oem.sno`, `ro.ril.oem.psno`, OEM serials, or Canonical Hardware Agent ID e.g., `BE5A234A` via `DeviceIdentityHelper`) is the **only immutable Single Source of Truth (SSOT)** for device identity.
  - Software UUIDs or temporary auth tokens must never replace or overwrite hardware agent identities.
- **Integration Points**: `SplashActivity`, `DeviceIdentityHelper`, `SessionRouter`, `AuthActivity`.

### Agent 2: Requirements Analysis & Specification Architect
- **Domain**: Orchestration & Governance
- **Core Mission**: Formulates functional requirements, BRD/PRD specifications, anti-GIGO taxonomies, and the 6-layer Coordinate Tree `(ROOT⊃BR⊃EL⊃FE⊃BE⊃DB)`.
- **Primary Deliverables**: Screen-level requirement trees, database sync settings, and coordinate tree contracts across `docs/design/` and `requirements/`.
- **Integration Points**: `COORDINATE_TREE_ALL_SCREENS_SPECIFICATION.md`, `PROGRAM-MAPPING.md`.

### Agent 3: System Design & Enterprise HLD Architect
- **Domain**: Orchestration & Governance
- **Core Mission**: Enforces system-wide High-Level Design (HLD), clean Separation of Concerns (SoC), MVVM architecture, and dual-module parity between `app` and `jButterFly`.
- **Primary Deliverables**: Enterprise architectural diagrams, cross-module contracts, and module interface boundaries.
- **Integration Points**: `ENTERPRISE_HLD_DESIGN.md`, `ARCHITECTURE_ASSIGNMENTS.md`.

### Agent 4: Governance, Compliance & Safety Gatekeeper
- **Domain**: Orchestration & Governance
- **Core Mission**: Enforces the **4 Hard Gates** and protective invariants:
  - **Gate 1**: Minors protection (Zero tolerance for privacy leaks or unauthorized external exposure).
  - **Gate 2**: Irreversible operations (Delete, overwrite, deploy, drop require explicit one-line confirmation).
  - **Gate 3**: Live-fire target guard (Stop before live execution against unverified targets).
  - **Gate 4**: CSAM (Strict system lockdown).
  - **Red-Line Gate**: Ward Top-Position 0 monitoring priority (`4379A761` / Riddhi) and circle inclusion invariance.
- **Integration Points**: `PrivacyActivity`, `SecurityEngine`, `Security_and_Identity_Specification.md`.

---

## 🌐 Domain 2: Intelligence & Business (Agents 5 – 8)

### Agent 5: Business Logic & Workflow Engine Architect
- **Domain**: Intelligence & Business
- **Core Mission**: Manages reciprocal mesh pairing workflows, 4-in-1 Device Sharing Hub logic (Device A ➔ Share Device B ➔ to Device C), and Butterfly Point state machines (`ACTION_GHOST`).
- **Primary Deliverables**: Symmetric pairing state trees, relationship lifecycle controllers, and invitation handshake protocols.
- **Integration Points**: `DeviceSharingHub`, `ButterflyPointHub`, `NetworkGatewayActivity`, `PairingRepository`.

### Agent 6: GIS, Spatial & Geofencing Architect
- **Domain**: Intelligence & Business
- **Core Mission**: Governs geographical math, low-latency geofence polygon containment (Ray-Casting), Haversine distance, and 2D Kalman smoothing filters.
- **Spatial Mandates**:
  - Deduped geofence index (`idx_safe_zones_dedupe`).
  - Proximity detection ($> 50\text{ m}$ floating panel separation, $< 5\text{ m}$ safe zone threshold).
- **Integration Points**: `GeofenceActivity`, `GeofenceNotificationActivity`, `GeofenceHelper`, `SpatialEngine`.

### Agent 7: Telemetry & Multi-Modal Streaming Specialist
- **Domain**: Intelligence & Business
- **Core Mission**: Regulates multi-modal telemetry ingestion: GPS coordinates, baseband cellular towers (MCC/MNC/LAC/CID triangulation), battery levels, and Wi-Fi SSID streams.
- **Adaptive Cadence**:
  - Live movement / SOS: Dynamic polling ($15\text{s}$).
  - Stationary: Throttled heartbeat ($15\text{m}$).
- **Integration Points**: `TrackingService`, `CellTowerSentinel`, `SimForensicWorker`, `LocationRepository`.

### Agent 8: Analytics, RAG Vitals & Reporting Engine
- **Domain**: Intelligence & Business
- **Core Mission**: Calculates real-time RAG health vitals (Green `< 15m`, Amber `15m – 6h` / $\le 20\%$ batt, Red `> 6h`, Crimson SOS, Gray Ghost).
- **Bounded Output Standard**:
  - Telemetry audits and diagnostic logs must enforce bounded output schemas ($\le 100\text{ KB}$, max 10 sample slices). Never dump unbounded telemetry payloads.
- **Integration Points**: `DeviceRagEngine`, `FamilyFriendsViewModel`, `HealthActivity`, `RAG_CRITICAL_VALIDATION_REPORT.md`.

---

## ⚙️ Domain 3: Technology & Architecture (Agents 9 – 13)

### Agent 9: Dual-Engine Database & Backend Logic Architect
- **Domain**: Technology & Architecture
- **Core Mission**: Maintains bidirectional SSOT between local SQLite (V81.2 Northwind 2003 schema, 27 normalized tables + 13 compatibility views) and Firebase RTDB.
- **Master Temporal Query Invariant**:
  - All database queries and compatibility views MUST enforce descending order (`ORDER BY timestamp DESC`).
- **Integration Points**: `DatabaseHelper`, `DatabaseManagementActivity`, `ForensicVaultExplorerActivity`.

### Agent 10: Cloud Infrastructure & Firebase RTDB Sentinel
- **Domain**: Technology & Architecture
- **Core Mission**: Manages Firebase Realtime Database rules (`database.rules.json`), security validation, indexing (`lookup_indices`), and atomic multi-path commits.
- **Integration Points**: `FirebaseComm`, `FirebaseCloudMatrixActivity`, `FindMe online structure.txt`.

### Agent 11: Security, Cryptography & Hardware Anchor Specialist
- **Domain**: Technology & Architecture
- **Core Mission**: Manages Android Keystore, SELinux audit denial bypasses, device fingerprint sealing, and local SQLite cipher encryption.
- **Integration Points**: `keystore.properties`, `DeviceIdentityHelper`, `PrivacyEngine`.

### Agent 12: Network Resilience & Failover Mesh Architect
- **Domain**: Technology & Architecture
- **Core Mission**: Implements the 5-Tier Telemetry Failover Router:
  1. Primary Cellular WebSocket
  2. Wi-Fi Local Socket
  3. SMS Beacon Fallback
  4. Encrypted SQLite Offline Queue
  5. Delayed Resynchronization on reconnect with cloud ACK ($< 5\text{s}$).
- **Integration Points**: `TelemetryFailoverRouter`, `TelemetryClosedLoopSentinel`, `SimTransportValidator`.

### Agent 13: Clean Architecture, OOPS & Refactoring Sentinel
- **Domain**: Technology & Architecture
- **Core Mission**: Enforces 20-Year Senior Developer dynamic OOPS standards:
  - **Ponytail 96% Rule**: Any file $\ge 1,920$ lines must branch via Strangler Fig pattern or call-by-reference modular controllers.
  - **Zero Duplicate Code**: Call-by-reference re-use of singletons and utility engines.
  - **Prime Directive 1**: Deprecate (`@Deprecated`) or merge; never delete working code outright.
- **Integration Points**: `BaseActivityJava`, `ThemeManager`, `All brownfield ViewModels/Controllers`.

---

## 🎨 Domain 4: AI Engineering & Frontend UI/UX (Agents 14 – 17)

### Agent 14: Frontend UI/UX & Lightweight Viewport Architect
- **Domain**: AI Engineering & Frontend UI/UX
- **Core Mission**: Implements the L7 Enterprise UI Standard (`UiArchitect`):
  - Two-point linear gradients, Glassmorphism (15% white overlay), 16f-30f card radii, 50f capsule buttons with 25f elevation.
  - Mandatory fields highlighted with red asterisk `(*)` in label and hint via `Html.fromHtml`.
- **Integration Points**: `UiArchitect.java`, `UiConfigActivity`, `UiGalleryActivity`.

### Agent 15: Map Viewport & Spatial Rendering Architect
- **Domain**: AI Engineering & Frontend UI/UX
- **Core Mission**: Governs the Google Map viewport, `MultiMarkerEngine`, and camera transitions.
- **Lightweight Marker Rule**:
  - Clutter-free callout pills + direct avatars + status dots + chevrons.
  - Render cycles throttled $\ge 250\text{ ms}$ on MainLooper. Zero heavy canvas glow layers.
- **Integration Points**: `MapActivity.java`, `MultiMarkerEngine.java`, `activity_map.xml`.

### Agent 16: Dynamic Theme & Brand Identity Specialist
- **Domain**: AI Engineering & Frontend UI/UX
- **Core Mission**: Maintains the 12 dynamic themes (`ThemeManager.THEMES`), real-time theme switching observable patterns, and eliminates hardcoded colors.
- **Brand Identity Mandate**: Strictly enforce `.agent/rules/branding.md` across all activities.
- **Integration Points**: `ThemeManager.java`, `SettingsActivity.java`, `BaseActivityJava.java`.

### Agent 17: Accessibility, Navigation & Interaction Specialist
- **Domain**: AI Engineering & Frontend UI/UX
- **Core Mission**: Ensures Universal 5-Stage scaling (LIBERAL, STANDARD, COMPACT, SLIM, ULTRA_SLIM), D-Pad/Enter key support (`IME_ACTION_DONE`), deterministic Tab sequences, and zero-dead-end back navigation.
- **Integration Points**: `OnBackInvokedCallback`, `ScrollView` stage roots, `UserManual.md`.

---

## 🛡️ Domain 5: Quality & Operations (Agents 18 – 22)

### Agent 18: Integration, QA & Cross-Module Parity Verifier
- **Domain**: Quality & Operations
- **Core Mission**: Validates 100% byte/line parity between `app` and `jButterFly` across all 14 twin pairs. Enforces pre-change declarations `[Variables] | [Constants] | [Dependencies] | [Integration Points]`.
- **Integration Points**: `FINAL_INTEGRATION_VALIDATION_REPORT.md`, `docs/history/`.

### Agent 19: Crash & Silent Failure Hunter (Diagnostic Sentinel)
- **Domain**: Quality & Operations
- **Core Mission**: Proactively catches background crashes, ANRs, Doze mode terminations, and JSON/Firebase deserialization bugs via `safeLong`/`safeInt` boundaries.
- **Integration Points**: `WatchDogAuditActivity`, `WatchDogViewModel`, `SystemLogActivity`.

### Agent 20: Performance, Memory & Battery Optimization Specialist
- **Domain**: Quality & Operations
- **Core Mission**: Optimizes battery longevity, ensures SQLite query execution $< 16\text{ms}$, manages avatar memory caching (`REMOTE_HQ_`), and executes rolling SQLite `VACUUM`.
- **Integration Points**: `HealthActivity`, `BackgroundJobsConfigDialog`, `BackgroundJobManager`.

### Agent 21: Build Quarantine & Release Gatekeeper
- **Domain**: Quality & Operations
- **Core Mission**: Enforces the **Build Quarantine Protocol**:
  - **Autonomous `./gradlew` execution is strictly prohibited.**
  - Code changes remain decoupled from build tasks until explicitly authorized by operator JEET.
- **Integration Points**: `gradlew`, `build.gradle.kts`, `version.properties`.

### Agent 22: Self-Healing, Silent Update & In-Place Migration Engineer
- **Domain**: Quality & Operations
- **Core Mission**: Oversees seamless APK updates (`SilentUpdateWorker`), SHA-256 hash validation, database schema dynamic PRAGMA migrations, and factory reset sanitization.
- **Integration Points**: `SystemUpdateActivity`, `SilentUpdateWorker`, `SYSTEM_UPDATE_REQUIREMENTS_TREE.md`, `FactoryResetActivity`.

---

## 📋 Master 22-Agent Assignment Matrix to jFindMe Screens

| Screen / Component | Primary Agent Owner | Supporting Agents | SSOT Integration |
|:---|:---|:---|:---|
| `SplashActivity` | **Agent 1** | Agent 11, Agent 17 | `DeviceIdentityHelper` ➔ Hardware Serial Root |
| `AuthActivity` | **Agent 1** | Agent 9, Agent 14 | Firebase Auth ↔ `user_identity` |
| `MapActivity` | **Agent 15** | Agent 6, Agent 7, Agent 9 | `/locations/{id}`, `LocationEventTable` |
| `ButterflyPointHub` | **Agent 5** | Agent 6, Agent 9, Agent 14 | `/butterfly_points/{id}`, `butterfly_point` |
| `DeviceSharingHub` | **Agent 5** | Agent 1, Agent 9, Agent 12 | `/relationships/{grp}/device_pairings` |
| `NetworkGatewayActivity` | **Agent 5** | Agent 1, Agent 9, Agent 12 | 2x2 Pairing Matrix (`device_pairings`) |
| `FamilyFriendsActivity` | **Agent 8** | Agent 9, Agent 14, Agent 16 | `MASTER_ROSTER`, `device_vitals` |
| `PersonDetailsActivity` | **Agent 2** | Agent 1, Agent 14 | `user_profile`, `user_identity` |
| `GeofenceActivity` | **Agent 6** | Agent 14, Agent 15 | `safe_zones`, `/safe_zones/{grp}` |
| `GeofenceNotificationActivity` | **Agent 6** | Agent 9, Agent 19 | `geofence_events` |
| `HistoryActivity` | **Agent 6** | Agent 8, Agent 9, Agent 19 | `tracking_history`, `location_event` |
| `NotifActivity` / `alert` | **Agent 8** | Agent 9, Agent 19 | `alerts`, SOS Ingress/Egress |
| `AcousticSirenActivity` | **Agent 7** | Agent 19, Agent 20 | DND Override 30s Acoustic Alarm Beacon |
| `HealthActivity` | **Agent 7** | Agent 1, Agent 8, Agent 12 | Battery, GPS, MCC/MNC/LAC/CID, Cell Tower |
| `PrivacyActivity` | **Agent 4** | Agent 1, Agent 11 | `system_registry`, Ghost Mode Locks |
| `SettingsActivity` | **Agent 16** | Agent 2, Agent 14, Agent 20 | `ThemeManager`, `settings_preferences` |
| `UiConfigActivity` | **Agent 16** | Agent 14, Agent 17 | Dynamic 12 Themes & 5-Stage Sizing |
| `UiGalleryActivity` | **Agent 14** | Agent 16, Agent 17 | L7 Component Showcase |
| `BackgroundJobsConfigDialog` | **Agent 20** | Agent 9, Agent 19 | 13 Engine Workers, BackgroundJobManager |
| `ForensicVaultExplorerActivity`| **Agent 9** | Agent 11, Agent 19 | 27 Normalized Tables + 13 Compatibility Views |
| `WatchDogAuditActivity` | **Agent 19** | Agent 9, Agent 20 | Process Liveness & SQLite Integrity |
| `DatabaseManagementActivity` | **Agent 9** | Agent 18, Agent 20 | SQLite Schema & PRAGMA Verification |
| `FirebaseCloudMatrixActivity` | **Agent 10**| Agent 1, Agent 9 | Cloud Realtime Database Mesh |
| `SystemLogActivity` | **Agent 19** | Agent 8, Agent 20 | Rolling Realtime Runtime Trace (`sys_log`) |
| `SystemUpdateActivity` | **Agent 22** | Agent 18, Agent 21 | Silent APK Updater & Hash Verifier |
| `FactoryResetActivity` | **Agent 22** | Agent 1, Agent 4 | Selective Purge & Cloud Recovery |
| `HelpSupportActivity` | **Agent 17** | Agent 14, Agent 2 | User Manual & Diagnostics Gateway |
| `AboutActivity` | **Agent 1** | Agent 14, Agent 18 | Application Manifest & Build Identity |
