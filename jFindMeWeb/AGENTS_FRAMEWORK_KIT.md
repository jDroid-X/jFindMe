# jFindMe - Optimized Multi-Agent, Skills & Workflow Governance
Source Framework: `C:\Users\jiten\jAnitGravity\.kit`

## 🏛️ 1. ARCHITECTURAL OVERVIEW
For **jFindMe** (Live GPS Telemetry, Family & Friends Circles, Geofences/Safe Zones, History Playback, and Device Health), high performance and accurate data display require specialized agents across **4 operational layers** (8 dedicated agent personas), powered by **6 core workflow rules** and **8 targeted skills** from `.kit`.

---

## 🤖 2. RECOMMENDED AGENTS SUITE (8 SPECIALIZED AGENTS)

```
========================================================================================
LAYER 1: ARCHITECTURE & SPATIAL DATABASE
----------------------------------------------------------------------------------------
[Agent 1: Master Orchestrator & Enterprise Architect]
  • Source: .kit/.agent/agents/architect.md + orchestrator.md
  • Role: Enforces Dynamic OOPS-based MVC, 7-Stage Waterfall, and ProgramMapping.md anchor.
  • Output: Clean class boundaries, zero code duplication, unified state flow.

[Agent 2: Spatial & Unified Database Architect]
  • Source: .kit/.agent/agents/database-architect.md + .kit/.agent/gis/gis-spatial-data-engineer.md
  • Role: Normalizes schemas across Firebase RTDB, Firestore & IndexedDB/LocalCache.
  • Output: 1-click cascading updates (Avatar, Name, SafeZone, Relations) with zero sync lag.

========================================================================================
LAYER 2: REALTIME ENGINE & GIS TELEMETRY
----------------------------------------------------------------------------------------
[Agent 3: Realtime Sync & Backend Specialist]
  • Source: .kit/.agent/agents/backend-specialist.md + fullstack-sync-engineer.md
  • Role: Sub-second GPS payload streaming, WebSocket/RTDB handlers, connection recovery.
  • Output: Reliable offline-first queue, device heartbeat monitoring, state reconciliation.

[Agent 4: GIS & Mapping Specialist]
  • Source: .kit/.agent/gis/gis-web-gis-developer.md + .kit/.agent/agents/mobile-developer.md
  • Role: Coordinate projection, sub-pixel marker interpolation, vector tile caching.
  • Output: Jitter-free live marker movement, smooth history polylines, fast geofence math.

========================================================================================
LAYER 3: UI/UX TELEMETRY & PERFORMANCE OPTIMIZATION
----------------------------------------------------------------------------------------
[Agent 5: UI/UX & Telemetry HUD Designer]
  • Source: .kit/.agent/workflows/ui-ux-pro-max.md + .kit/.agent/gis/gis-cartography-designer.md
  • Role: Vertically compact glassmorphism HUD, speed/accuracy/battery indicators.
  • Output: Sleek dark/light theme, full standard menu drawer, responsive 60fps telemetry.

[Agent 6: Performance & Battery Optimizer]
  • Source: .kit/.agent/agents/performance-optimizer.md + .kit/.agent/skills/11_Opt/Perf/
  • Role: Adaptive GPS polling (movement-based throttling), DOM virtualization, memory audit.
  • Output: Minimal CPU/battery drain, zero frame-drop map panning, lean bundle footprint.

========================================================================================
LAYER 4: ENTERPRISE SECURITY & E2E VALIDATION
----------------------------------------------------------------------------------------
[Agent 7: Security & Device Health Auditor]
  • Source: .kit/.agent/agents/security-auditor.md + penetration-tester.md
  • Role: Mock GPS detection, root/jailbreak defense, developer mode warnings, AES-256 tokens.
  • Output: Tamper-proof telemetry streams, hardened Firebase Security Rules.

[Agent 8: QA & E2E Validation Engineer]
  • Source: .kit/.agent/agents/test-engineer.md + .kit/.agent/gis/gis-qa-engineer.md
  • Role: Automated GPS trail simulation, pairing verification, closed-loop log assertions.
  • Output: Strict assertions (MAP_READY, LOCATION_STREAM_ACTIVE, PAIRING_SUCCESS).
========================================================================================
```

---

## 📜 3. RECOMMENDED WORKFLOW RULES FROM `.kit`

| Workflow Rule | Location in `.kit` | Purpose in jFindMe |
| :--- | :--- | :--- |
| **MVC Closed-Loop Waterfall** | `.kit/.agent/workflows/mvc-closed-loop-waterfall.md` | 7 sequential stages (Initiate -> Reqs -> Design -> Code -> Test -> Deploy -> Maintenance) with zero dead ends. |
| **Operational Governance** | `.kit/.agent/rules/GEMINI.md` | Waterfall encapsulation tiers (P0-P5), Ponytail Decision Ladder, Circuit Breakers (max 2 retries). |
| **UI/UX Pro Max Standard** | `.kit/.agent/workflows/ui-ux-pro-max.md` | Compact visual design, micro-animations, glassmorphism telemetry HUD, and menu drawer standards. |
| **Token Audit & Context Hygiene** | `.kit/.agent/rules/TOKEN_AUDIT_OPTIMIZATION.md` | 7 Pillars (Memory, Tools, Model, Hooks, Subagents, Cron, Cache) to optimize LLM performance and cost. |
| **Branch Management & Convergence** | `.kit/.agent/AGENTS.md` | Guarantees every conditional branch (e.g., GPS loss, offline mode) merges into a valid fallback or graceful alert. |
| **Zero Hard-Refresh Telemetry** | Master AGENTS Governance | Continuous backend health checks on local dev server (port 12247 / 3000) with zero broken asset links. |

---

## 🧰 4. RECOMMENDED SPECIALIZED SKILLS FROM `.kit`

1. **`gis-web-gis-developer`** (`.kit/.agent/gis/`): High-performance vector rendering, Leaflet/Google Maps integration.
2. **`gis-spatial-data-engineer`** (`.kit/.agent/gis/`): Spatial indexing, geohash calculation, proximity clustering.
3. **`performance-optimizer` & `performance-profiling`** (`.kit/.agent/skills/11_Opt/Perf/`): GPU-accelerated rendering, memory leak prevention.
4. **`fixing-motion-performance`** (`.kit/.agent/skills/11_Opt/Perf/`): Sub-pixel transform animations for fluid vehicle/personnel tracking.
5. **`database-design`** (`.kit/.agent/skills/04_DB/`): Cascading 1-to-many relationship management for Avatar/SafeZone synchronization.
6. **`ui-ux-pro-max`** (`.kit/.agent/skills/07_UI/`): Glassmorphism telemetry overlays, high-contrast dark mode maps.
7. **`vulnerability-scanner`** (`.kit/.agent/security/`): Anti-spoofing algorithms for mock GPS detection.
8. **`webapp-testing`** (`.kit/.agent/skills/10_Test/`): End-to-end telemetry playback tests and geofence entry/exit validation.
