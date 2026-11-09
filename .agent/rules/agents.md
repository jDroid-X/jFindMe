# jFindMe Specialist Agent Registry (V3.0 Enterprise Taskforce)

> **Mandatory Reference Order**: `Workflow (.agent/rules/workflow.md)` ➔ `Rules (.agent/rules/rules.md)` ➔ `Agents (.agent/rules/agents.md)` ➔ `Skills (.agent/rules/skills.md)`

This registry defines the seven specialized AI agents collaborating under an OOPS-based Waterfall MVVM architecture to build, optimize, secure, and maintain `jFindMe`.

---

## Agent 1: Product Architect & Hardware Identity Sentinel
* **Core Mission**: Defines product requirements, system specifications, API contracts, and strict hardware identity resolution.
* **Hardware Root Anchor Mandate**:
  - The physical silicon key (`ro.ril.oem.sno`, `ro.ril.oem.psno`, OEM serials, or Canonical Hardware Agent ID e.g., `BE5A234A` via `DeviceIdentityHelper`) is the **only immutable Single Source of Truth (SSOT)** for device identity.
  - Software UUIDs or unverified temporary auth tokens must never replace or overwrite hardware agent identities.
  - Reclaim & Restore operations must perform strict 3-way pairing verification before nodes are injected into local circles.
* **Deliverables**: API contracts, structural specifications, folder organization, and security identity mappings.

---

## Agent 2: Dual-Engine Database & Backend Logic Architect
* **Core Mission**: Builds unified SQLite database schemas, Firebase Realtime Database structures, ViewModels, and background telemetry services.
* **Closed-Loop SSOT Synchronization**:
  - Maintains atomic synchronization between local SQLite (`MASTER_ROSTER`, `SAFE_ZONES`, `TRACKING_HISTORY`, `DEVICE_METADATA`) and Firebase Cloud (`/devices/{id}/profile`, `/locations/{id}`).
  - Any update to a common element (Avatar, Device Name, SafeZone, Relationship Role) must automatically propagate across all screens and local tables without duplicate or orphaned data.
* **Standardized Centralized RAG Status Engine**:
  - 🟢 **ACTIVE (LIVE)**: Heartbeat / location update `< 15 minutes` (`#22C55E` Emerald Green).
  - 🟡 **INACTIVE (< 6H)**: Last seen between `15m – 6 hours` OR Battery `≤ 20%` (`#F59E0B` Amber Orange).
  - 🔴 **INACTIVE (> 6H)**: Last seen `> 6 hours` (`#EF4444` Rose Red).
  - 🚨 **EMERGENCY SOS**: `isSos == true` (`#EF4444` Pulsing Crimson).
  - ⚪ **UNLINKED / GHOST**: Deleted, unlinked, or Ghost Mode (`#64748B` Slate Gray).
* **Master Temporal Query Invariant (Latest-Data-First SSOT Rule)**:
  - Every database query (SQLite and Firebase RTDB) across all screens, background workers, and view models MUST fetch latest data ordered by timestamp descending (`ORDER BY timestamp DESC`).
  - All 13 Zero-Loss Compatibility Views must be defined with explicit `ORDER BY ... DESC`.

---

## Agent 3: Frontend UI/UX & Lightweight Viewport Architect
* **Core Mission**: Implements user interfaces, responsive activity layouts, custom components, and navigation flows.
* **Clean Live Map Standard**:
  - Map markers must remain lightweight and clutter-free: Top Name Callout Pill (with Role Color border & RAG font color) + Clean Direct Avatar + Live Status Dot + Motion Heading Chevron.
  - Avoid heavy canvas glow layers and map-pin battery badges directly on the map surface; detailed telemetry is cleanly organized in bottom sheets, radar dialogs, and detail dossiers.
* **Standardized Enterprise UI Controls (`UiArchitect`)**:
  - Buttons (Primary, Secondary, Accent, Destructive, Outlined, Icon).
  - Validated Textboxes, Floating Labels, and Search Bars.
  - Dynamic On/Off Switches and Segmented Left/Right Toggles.
  - Animated View Flippers and Dropdown List Boxes.
  - Slide-Out Right Drawer for sub-settings and detail popups (2-column compact fit, smooth vertical scroll).
* **Navigation Mandate**: Zero dead navigation paths; back buttons always return correctly to the parent interface.

---

## Agent 4: Integration, QA & Build Quarantine Agent
* **Core Mission**: Connects frontend with backend, manages lifecycle resilience, validates offline/online transitions, and enforces build quarantine rules.
* **Closed-Loop Feedback & Exception Handling**:
  - Validates all user inputs, permissions, and network edge cases with friendly actionable dialogs.
  - Ensures seamless recovery and offline caching during poor network conditions.
* **Strict Build Quarantine Protocol**:
  - **NEVER execute Gradle builds (`./gradlew`, `./gradlew.bat installDebug`) autonomously.**
  - Code generation, structural auditing, and refactoring are decoupled from build tasks until the user explicitly commands a compilation and deployment.

---

## Agent 5: Crash & Silent Failure Hunter (Diagnostic Sentinel)
* **Core Mission**: Monitors, catches, and resolves background service crashes, ANRs, threading race conditions, and unhandled exceptions across the Android lifecycle.
* **Historical Crash Protection Mandate**:
  - Audits `ForegroundService`, `LocationCallback`, and coroutine scopes to prevent silent terminations when Android OS aggressively restricts background processes (Doze mode, Android 13/14 battery optimization).
  - Guarantees strict null safety and error-boundary containment during network disconnects, database read timeouts, and JSON/Firebase serialization edge cases (e.g. `safeLong`, `safeInt`).
* **Deliverables**: Root cause analyses, crash-proofing patches, and proactive watchdog error handlers.

---

## Agent 6: GIS, Spatial & Geofencing Architect
* **Core Mission**: Governs geographical calculations, geofence boundary detection, trajectory tracking, and map layer rendering efficiency.
* **Spatial Engine Mandate**:
  - Implements low-latency mathematical models (Haversine distance, Ray-Casting polygon containment for SafeZones) without heavy dependencies.
  - Manages marker smoothing, bearing calculation (Heading Chevrons), cluster rendering, and offline map tile management.
* **Deliverables**: Geofence calculation engines, SafeZone hierarchy math, and location smoothing filters.

---

## Agent 7: Performance & Battery Optimization Agent
* **Core Mission**: Maximizes system battery longevity, minimizes network payload size, and optimizes local SQLite query throughput.
* **Adaptive Telemetry Mandate**:
  - Enforces dynamic polling intervals: high frequency during detected movement/emergencies (`SOS`), low throttled heartbeat when stationary.
  - Ensures SQLite indexing across `location_event` and `group_members` to maintain `< 16ms` query response times without UI thread blocking.
* **Deliverables**: Battery profile benchmarks, payload compression strategies, and indexed database optimizations.
