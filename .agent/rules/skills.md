# jFindMe Domain Skills & Capabilities (V3.0 Enterprise Taskforce)

> **Mandatory Reference Order**: `Workflow (.agent/rules/workflow.md)` ➔ `Rules (.agent/rules/rules.md)` ➔ `Agents (.agent/rules/agents.md)` ➔ `Skills (.agent/rules/skills.md)`

This file maps technical capabilities and tools to the specialist agents to ensure optimized performance and architectural alignment.

---

## Core Technical Skills (Required for all Agents)
- **Agent Concurrency Management**: Adherence to the 4-agent maximum and single-workflow guardrails.
- **Pre-Change Declaration Skill**: Mandatory declaration of `[Variables] | [Constants] | [Dependencies] | [Integration Points]`.
- **Module Parity Skill**: Mandatory dual-sync between `app/` and `jButterFly/` with zero file drift.
- **Build Quarantine Compliance**: Decoupling code modifications from builds; zero autonomous `./gradlew` execution.

---

## Specialist Skill Matrix

### Agent 1: Product Architect & Hardware Identity Sentinel
- **Hardware Root Probing**: Secure resolution via `Settings.Secure.ANDROID_ID`, SELinux bypass, and thread-safe caching.
- **Domain Modeling**: ERD design, `DeviceConnectivityRulebook` matrix, and API contract specification.
- **Topology Scoping**: RBAC definition and multi-device circle relationship hierarchies.

### Agent 2: Dual-Engine Database & Backend Logic Architect
- **MVVM Implementation**: Clean Separation of Concerns (SoC) between ViewModels, Repositories, and Views.
- **Master Temporal Querying**: Enforcing `ORDER BY ... DESC` on all SQLite queries and 13 compatibility views.
- **Dual-Engine Syncing**: Bi-directional SQLite $\leftrightarrow$ Firebase Realtime Database atomic mutations.
- **RAG Status Resolution**: Centralized status calculation based on 15m / 6h / SOS thresholds.

### Agent 3: Frontend UI/UX & Lightweight Viewport Architect
- **Aesthetic Engineering (`UiArchitect`)**: Implementing two-point linear gradients, Glassmorphism, 16f-30f corner radii, and capsule buttons.
- **Accessibility & Navigation**: Tab-sequence, D-Pad support, predictive back callback, and Marquee text.
- **Responsive Viewport Fitting**: Dynamic 5-stage screen scaling matrix (Liberal, Standard, Compact, Slim, Ultra-Slim).
- **Lightweight Marker Optimization**: Clutter-free callout pills, direct avatar loading, and heading chevrons.

### Agent 4: Integration, QA & Build Quarantine Agent
- **Parity Auditing**: Line-by-line verification across all 14 file pairs (`app` $\leftrightarrow$ `jButterFly`).
- **Forensic Troubleshooting**: Resolving fatal deserialization, type-mismatch errors (`safeLong`, `safeInt`), and broken dependencies.
- **Closed-Loop Feedback**: Ensuring all offline-online fallbacks and preference stores are bidirectionally closed.

### Agent 5: Crash & Silent Failure Hunter (Diagnostic Sentinel)
- **Doze Mode & Lifecycle Hardening**: Coroutine scope supervision and foreground service survival across Android 13/14.
- **Exception Boundary Containment**: Catching silent network drops, database lock contention, and serialization anomalies.
- **Self-Healing Schema Auditing**: `PRAGMA table_info` schema verification and dynamic table column additions.

### Agent 6: GIS, Spatial & Geofencing Architect
- **Spatial Calculations**: Haversine distance, marker bearing smoothing, and ray-casting polygon containment.
- **Geofence Deduplication**: Unique SQLite index creation (`idx_safe_zones_dedupe`) and low-latency boundary evaluation.
- **Tactical Viewport Control**: 50m proximity filtering for distant peer carousels and smooth camera pan targeting.

### Agent 7: Performance & Battery Optimization Agent
- **Adaptive Telemetry Engine**: Dynamic polling throttle ($15\text{s}$ live vs $15\text{m}$ stationary).
- **Map Render Throttling**: Debouncing marker layout cycles to $\ge 250\text{ ms}$ on the Android MainLooper.
- **Payload & Memory Compression**: Avatar bitmap scaling, isolated cache keying (`REMOTE_HQ_{devId}_{avId}`), and bounded sample slicing ($\le 100\text{ KB}$).
