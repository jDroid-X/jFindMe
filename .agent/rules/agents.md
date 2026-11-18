# Antigravity Enterprise AI Agent Protocol & Specialist Agent Registry (V5.0 Enterprise Taskforce)
## Codename: jDroid-X | Lead Operator: JEET

> **Mandatory Reference Pipeline**: `Workflow (.agent/rules/workflow.md)` ➔ `Rules (.agent/rules/rules.md)` ➔ `Agents (.agent/rules/agents.md)` ➔ `Skills (.agent/rules/skills.md)` ➔ `Branding (.agent/rules/branding.md)`

This registry defines the 22 Specialized Enterprise AI Agents organized across 5 Workforce Domains, collaborating under the Dynamic OOPS-based MVC/MVVM Closed-Loop Waterfall architecture to build, optimize, secure, and maintain `jFindMe` and `jButterFly`. Full specifications are recorded in `.agent/enterprise-22-agents/Agent-Registry.md`.

---

## 🏛️ Workforce Domains & 22-Agent Master Matrix

### Domain 1: Orchestrator & Governance (Agents 1 – 4)
- **Agent 1: Master Orchestrator & Hardware Identity Sentinel**:
  - SSOT Root Anchor (`ro.ril.oem.sno`, `ro.ril.oem.psno`, `Settings.Secure.ANDROID_ID`).
  - Zero hardcoded device identities. Router between modules and life-cycle state machines.
- **Agent 2: Requirements Analysis & Specification Architect**:
  - Formulates BRD/PRD specifications, anti-GIGO taxonomies, and the 6-layer Coordinate Tree `(ROOT⊃BR⊃EL⊃FE⊃BE⊃DB)`.
- **Agent 3: System Design & Enterprise HLD Architect**:
  - Enforces system-wide High-Level Design (HLD), clean Separation of Concerns (SoC), and dual-module parity (`app` ⟷ `jButterFly`).
- **Agent 4: Governance, Compliance & Safety Gatekeeper**:
  - Enforces the 4 Hard Gates (Minors, Irreversible ops, Live-fire targets, CSAM) and Red-Line Monitoring Invariants.

### Domain 2: Intelligence & Business (Agents 5 – 8)
- **Agent 5: Business Logic & Workflow Engine Architect**:
  - Reciprocal mesh pairing, 4-in-1 Device Sharing Hub logic (Device A ➔ Share Device B ➔ to Device C), Butterfly Point state machines (`ACTION_GHOST`).
- **Agent 6: GIS, Spatial & Geofencing Architect**:
  - Ray-Casting polygon containment, Haversine distance, 2D Kalman filter, and 50m proximity floating avatar panels.
- **Agent 7: Telemetry & Multi-Modal Streaming Specialist**:
  - Adaptive cadence (15s live movement vs 15m stationary heartbeat), baseband cellular tower triangulation (MCC/MNC/LAC/CID), and battery duty cycles.
- **Agent 8: Analytics, RAG Vitals & Reporting Engine**:
  - Centralized RAG vitals calculation (Emerald Green `< 15m`, Amber Orange `15m – 6h` / $\le 20\%$ batt, Rose Red `> 6h`, Crimson Pulsing SOS, Slate Gray Ghost).
  - Bounded output contract: $\le 100\text{ KB}$, max 10 sample slices.

### Domain 3: Technology & Architecture (Agents 9 – 13)
- **Agent 9: Dual-Engine Database & Backend Logic Architect**:
  - SQLite V81.2 Northwind 2003 schema (27 normalized tables + 13 compatibility views) ↔ Firebase RTDB closed loop.
  - Master Temporal Invariant: Descending chronological ordering (`ORDER BY timestamp DESC`).
- **Agent 10: Cloud Infrastructure & Firebase RTDB Sentinel**:
  - Cloud Realtime Database rules (`database.rules.json`), security validation, indexing (`lookup_indices`), and atomic multi-path commits.
- **Agent 11: Security, Cryptography & Hardware Anchor Specialist**:
  - Android Keystore, SELinux audit denial bypasses, device fingerprint sealing, and local SQLite cipher encryption.
- **Agent 12: Network Resilience & Failover Mesh Architect**:
  - 5-Tier Telemetry Failover Router (Cellular WebSocket ➔ Wi-Fi ➔ SMS Beacon ➔ Encrypted SQLite Queue ➔ Delayed Resync with Cloud ACK $< 5\text{s}$).
- **Agent 13: Clean Architecture, OOPS & Refactoring Sentinel**:
  - 20-Year Senior Developer dynamic OOPS standards, Ponytail 1,920-line ceiling, Strangler Fig pattern, zero code duplication, non-destructive code evolution.

### Domain 4: AI Engineering & Frontend UI/UX (Agents 14 – 17)
- **Agent 14: Frontend UI/UX & Lightweight Viewport Architect**:
  - L7 Enterprise UI Standard (`UiArchitect`): Two-point linear gradients, Glassmorphism, 16f-30f card radii, 50f capsule buttons with 25f elevation.
  - Mandatory fields marked with red asterisk `(*)` in label & hint via `Html.fromHtml`.
- **Agent 15: Map Viewport & Spatial Rendering Architect**:
  - Mapbox/Google Map viewport, `MultiMarkerEngine`, Lightweight Marker Standard (Pill + Avatar + Dot + Chevron, no heavy canvas glow layers), $\ge 250\text{ ms}$ render throttling.
- **Agent 16: Dynamic Theme & Brand Identity Specialist**:
  - 12 Dynamic Themes (`ThemeManager.THEMES`), real-time theme switching observable patterns, zero hardcoded colors, strict branding enforcement (`.agent/rules/branding.md`).
- **Agent 17: Accessibility, Navigation & Interaction Specialist**:
  - Universal 5-Stage scaling (LIBERAL, STANDARD, COMPACT, SLIM, ULTRA_SLIM), D-Pad/Enter key support (`IME_ACTION_DONE`), deterministic Tab sequences, predictive back navigation.

### Domain 5: Quality & Operations (Agents 18 – 22)
- **Agent 18: Integration, QA & Cross-Module Parity Verifier**:
  - 100% byte/line parity between `app` and `jButterFly` across all 14 twin pairs. Pre-change declarations `[Variables] | [Constants] | [Dependencies] | [Integration Points]`.
- **Agent 19: Crash & Silent Failure Hunter (Diagnostic Sentinel)**:
  - Doze mode & ForegroundService survival, Coroutine boundaries, null safety deserialization (`safeLong`, `safeInt`).
- **Agent 20: Performance, Memory & Battery Optimization Specialist**:
  - Battery longevity, $< 16\text{ms}$ SQLite queries, memory leak prevention, bitmap sampling (`REMOTE_HQ_`), rolling SQLite `VACUUM`.
- **Agent 21: Build Quarantine & Release Gatekeeper**:
  - Strict Build Quarantine Protocol: autonomous `./gradlew` execution strictly prohibited.
- **Agent 22: Self-Healing, Silent Update & In-Place Migration Engineer**:
  - Silent APK updates (`SilentUpdateWorker`), SHA-256 hash validation, dynamic PRAGMA migrations, factory reset sanitization.

---

## 🔒 Master Prime Directives & Invariant Protection
1. **Never delete earlier working code**: Deprecate (`@Deprecated`) or merge.
2. **Pre-Change Declaration**: `[Variables] | [Constants] | [Dependencies] | [Integration Points]`.
3. **Zero-Tolerance for Basic Errors**: No unresolved references, undefined parameters, or missing imports.
4. **Dynamic OOPS-based MVC Standard**: 20-Year Senior Developer quality; re-use code via call-by-reference.
5. **Program Mapping SSOT**: Maintain `.agent/enterprise-22-agents/PROGRAM-MAPPING.md` as the master relationship table.
6. **Red-Line Gate**: Ward Priority 0 (`4379A761` / Riddhi) and circle inclusion invariance require explicit confirmation:
   > **`Confirming - "Violating Prime Directive Rules"`**
