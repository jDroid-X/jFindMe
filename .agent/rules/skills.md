# jFindMe Domain Skills & Capabilities (V5.0 Enterprise Taskforce)

> **Mandatory Reference Pipeline**: `Workflow (.agent/rules/workflow.md)` ➔ `Rules (.agent/rules/rules.md)` ➔ `Agents (.agent/rules/agents.md)` ➔ `Skills (.agent/rules/skills.md)` ➔ `Branding (.agent/rules/branding.md)`

This file defines the technical capabilities, tools, and specialized skill sets mapped to the 22 Specialized Enterprise AI Agents for `jFindMe` and `jButterFly`.

---

## 🛠️ Universal Core Skills (Enforced Across ALL Agents)
- **Agent Concurrency Control**: Adherence to $\le 4$ active agent ceiling and single workflow execution.
- **Pre-Change Declaration Skill**: Mandatory declaration of `[Variables] | [Constants] | [Dependencies] | [Integration Points]` before any file edit.
- **Dual-Module Parity Skill**: 100% byte/line synchronization between `app/` and `jButterFly/`.
- **Build Quarantine Compliance**: Strict decoupling of code generation/auditing from build tasks (`./gradlew` strictly quarantined).
- **Fast-Exit Context Guard**: Zero speculative repo crawling when target screen/module is absent in cold context.

---

## 🎯 22-Agent Domain Skill Matrix

### Domain 1: Orchestration & Governance
- **Agent 1 (Orchestrator & Hardware Identity)**:
  - Hardware Root Probing: `Settings.Secure.ANDROID_ID`, `ro.ril.oem.sno`, SELinux thread-safe caching.
  - Multi-agent orchestration, pipeline routing, and session state verification.
- **Agent 2 (Requirements & Specifications)**:
  - 6-Layer Coordinate Tree mapping `(ROOT⊃BR⊃EL⊃FE⊃BE⊃DB)`.
  - Anti-GIGO taxonomy enforcement and screen functional requirements authoring.
- **Agent 3 (System Design & HLD)**:
  - MVVM clean architecture enforcement, interface boundary modeling, and cross-module contract drafting.
- **Agent 4 (Governance & Safety Gatekeeper)**:
  - 4 Hard Gates policy enforcement, RBAC circle boundary validation, and Red-Line Ward Priority 0 invariant monitoring.

### Domain 2: Intelligence & Business
- **Agent 5 (Business Logic & Mesh Workflow)**:
  - Symmetrical pairing handshake, 4-in-1 Device Sharing state machine, and Butterfly Point (`ACTION_GHOST`) coordination.
- **Agent 6 (GIS, Spatial & Geofencing)**:
  - Ray-Casting polygonal containment math, Haversine spherical distance, 2D Kalman trajectory smoothing, and deduplicated index creation (`idx_safe_zones_dedupe`).
- **Agent 7 (Telemetry & Multi-Modal Streaming)**:
  - Adaptive telemetry duty cycles ($15\text{s}$ live vs $15\text{m}$ stationary), Cellular Baseband Sentinel (MCC/MNC/LAC/CID triangulation), and acoustic DND override beacon.
- **Agent 8 (Analytics, RAG Vitals & Reporting)**:
  - Centralized RAG vitals calculation (Emerald Green, Amber Orange, Rose Red, Pulsing SOS, Slate Gray), and bounded output schema compliance ($\le 100\text{ KB}$).

### Domain 3: Technology & Architecture
- **Agent 9 (Dual-Engine Database & Backend)**:
  - SQLite V81.2 Northwind 2003 schema governance (27 normalized tables + 13 compatibility views), and Master Temporal Invariant (`ORDER BY timestamp DESC`).
- **Agent 10 (Cloud Infrastructure & Firebase Sentinel)**:
  - Firebase Realtime Database rule enforcement (`database.rules.json`), path security, atomic multi-path commits, and index optimization.
- **Agent 11 (Security & Cryptography Specialist)**:
  - Android Keystore integration, AES-256 cipher encryption for local SQLite databases, and hardware serial hash protection.
- **Agent 12 (Network Resilience & Failover Mesh)**:
  - 5-Tier Telemetry Failover Router (Cellular ➔ Wi-Fi ➔ SMS ➔ SQLite Offline Queue), and bidirectional cloud ACK latency audits ($< 5\text{s}$).
- **Agent 13 (Clean Architecture, OOPS & Refactoring)**:
  - Ponytail 96% ceiling enforcement ($\le 1,920$ lines per file), Strangler Fig adapter patterns, and non-destructive code deprecation (`@Deprecated`).

### Domain 4: AI Engineering & Frontend UI/UX
- **Agent 14 (Frontend UI/UX & Lightweight Viewport)**:
  - L7 Enterprise UI Standard (`UiArchitect`): Two-point linear gradients, Glassmorphism (15% overlay), 16f-30f card radii, and 50f capsule buttons with 25f elevation.
  - Mandatory red asterisk `(*)` formatting via `Html.fromHtml`.
- **Agent 15 (Map Viewport & Spatial Rendering)**:
  - Clutter-free Lightweight Marker Standard (Pill + Avatar + Dot + Chevron), GPU rendering optimization, and $\ge 250\text{ ms}$ render cycle throttling.
- **Agent 16 (Dynamic Theme & Brand Identity)**:
  - 12 Dynamic Themes (`ThemeManager.THEMES`), real-time theme sync, zero hardcoded colors, and brand identity enforcement (`.agent/rules/branding.md`).
- **Agent 17 (Accessibility, Navigation & Interaction)**:
  - Universal 5-Stage scaling (LIBERAL, STANDARD, COMPACT, SLIM, ULTRA_SLIM), D-Pad/Enter key support (`IME_ACTION_DONE`), and deterministic Tab sequence.

### Domain 5: Quality & Operations
- **Agent 18 (Integration, QA & Cross-Module Parity)**:
  - Line-by-line parity verification across all 14 twin pairs (`app` $\leftrightarrow$ `jButterFly`), and zero unresolved reference enforcement.
- **Agent 19 (Crash & Silent Failure Hunter)**:
  - Doze mode & ForegroundService survival audits, Coroutine exception boundary containment, and type-safe deserialization (`safeLong`, `safeInt`).
- **Agent 20 (Performance, Memory & Battery Optimization)**:
  - Sub-16ms SQLite query tuning, avatar memory caching (`REMOTE_HQ_`), and rolling background SQLite `VACUUM`.
- **Agent 21 (Build Quarantine & Release Gatekeeper)**:
  - Enforces the strict Build Quarantine Protocol: autonomous `./gradlew` execution quarantined; code generation decoupled from compilation.
  - Keystore signing verification (X.509 certificate compatibility audit, SHA-256 binary validation).
- **Agent 22 (Self-Healing, Silent Update & In-Place Migration)**:
  - Silent APK update pipeline (`SilentUpdateWorker`), SHA-256 integrity checks, and self-healing schema PRAGMA updates.
  - Hardware & Software Condition Engine (`.agent/rules/lifecycle_rules.md`): Battery $\ge 15\%$ guard, storage $\ge 150\text{ MB}$ quota, Android API 31+ unattended installer sessions, and pre-uninstall sentinel teardown.
