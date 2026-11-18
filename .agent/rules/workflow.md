# jFindMe Master Waterfall MVVM Workflow (V5.0 Enterprise Standard)

> **Mandatory Reference Pipeline**: `Workflow (.agent/rules/workflow.md)` ➔ `Rules (.agent/rules/rules.md)` ➔ `Agents (.agent/rules/agents.md)` ➔ `Skills (.agent/rules/skills.md)` ➔ `Branding (.agent/rules/branding.md)` ➔ `Lifecycle (.agent/rules/lifecycle_rules.md)`

---

## 🧭 Master 6-Step Reference Pipeline

When ANY change, patch, bug fix, or feature request is received, every agent and developer MUST traverse this pipeline in exact order:

```
┌────────────────────────────────────────────────────────────────────────┐
│ STEP 1: WORKFLOW (.agent/rules/workflow.md)                            │
│ Check Concurrency (< 4 agents), Select Lane (1, 2, 3), 7-Stage Sequence│
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ STEP 2: RULES (.agent/rules/rules.md)                                  │
│ 1. Verify Universal Invariants (Pre-declaration, Parity, SSOT, 4 Gates)│
│ 2. Select Screen-Dependent Subset (Subsets A - H)                      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ STEP 3: AGENTS (.agent/rules/agents.md)                                │
│ Assign primary and supporting agents from the 22-Agent Workforce       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ STEP 4: SKILLS (.agent/rules/skills.md)                                │
│ Activate specialized technical capabilities; execute with zero stubs   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ STEP 5: BRANDING (.agent/rules/branding.md)                            │
│ Apply 12 Dynamic Themes, L7 UI Architect, RAG Colors, Lightweight Map  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ STEP 6: LIFECYCLE (.agent/rules/lifecycle_rules.md)                    │
│ Hardware/Software Pre-Flight, Unattended Upgrade, Pre-Uninstall Teardown│
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🚦 Adaptive 3-Lane Matrix

Every incoming task is categorized into one of three execution lanes to balance rigor with velocity:

### Lane 1: Patch Lane (Targeted Fix / Defect Remediation)
- **Scope**: Hotfixes, typo corrections, null checks, localized layout tweaks ($\le 2$ files).
- **Protocol**:
  1. Check warm context or `git status -s` (Fast-Exit Guard).
  2. Pre-change declaration: `[Variables] | [Constants] | [Dependencies] | [Integration Points]`.
  3. Apply non-destructive fix (`@Deprecated` or merge).
  4. Sync twin module file immediately (`app` ⟷ `jButterFly`).
  5. Verify cross-references with zero unresolved symbols.

### Lane 2: Feature Lane (Component / Screen Addition)
- **Scope**: New screens, new dialogs, new view models, new background jobs.
- **Protocol**:
  1. Define 6-Layer Coordinate Tree `(ROOT⊃BR⊃EL⊃FE⊃BE⊃DB)`.
  2. Apply Dynamic OOPS MVC/MVVM pattern.
  3. Ensure Ponytail Line Ceiling ($\le 1,920$ lines per file).
  4. Bind to `ThemeManager` 12 themes & L7 aesthetics (`UiArchitect`).
  5. Enforce dual-module twin creation and database temporal ordering (`ORDER BY ... DESC`).

### Lane 3: Enterprise Lane (Subsystem / Schema Overhaul)
- **Scope**: Database schema migrations (SQLite / Firebase), mesh protocol refactoring, authentication restructuring.
- **Protocol**:
  1. Full traversal through the 7-Stage Closed-Loop Waterfall Sequence.
  2. Multi-agent coordination (Agents 1 – 4 Orchestration review).
  3. Anti-GIGO verification & backward compatibility ledger.
  4. Red-Line confirmation gate if monitoring hierarchy or circle scoping is affected.

---

## 🔄 Master 7-Stage Closed-Loop Waterfall Sequence

```
[Stage 1: Initiate & Plan]
         │
         ▼
[Stage 2: Requirements Analysis]
         │
         ▼
[Stage 3: System Design]
         │
         ▼
[Stage 4: Implementation (OOPS MVC)]
         │
         ▼
[Stage 5: Testing & Parity Verification]
         │
         ▼
[Stage 6: Deployment & Build Quarantine]
         │
         ▼
[Stage 7: Maintenance & Closed-Loop Feedback]
```

### Stage 1: Initiate & Plan (Concurrency & Target Gate)
- **Concurrency Guard**: Check `active_agents < 4`. Queue tasks if saturated.
- **Single Workflow Lock**: Attach to existing workflows; prevent duplicate runs on identical `task_id`.
- **Target Resolution & Fast-Exit**: If no target screen/module is specified and context is cold ($\le 3$ turns), check `git status -s`. If no files are modified, **STOP & FAST-EXIT**: do NOT crawl the repository speculatively. Invert untouched screens to `[N/A]`.

### Stage 2: Requirements Analysis (The "Anti-GIGO Gate")
- **Specification Check**: Map requirements to `docs/design/` and `requirements/` specs.
- **Coordinate Tree Formulation**: Explicitly write out `(ROOT⊃BR⊃EL⊃FE⊃BE⊃DB)`:
  - **ROOT**: Subsystem Context
  - **BR**: Business Rules & Role Permissions
  - **EL**: Event Listeners & State Triggers
  - **FE**: Frontend UI/UX Elements
  - **BE**: Backend Repositories & ViewModels
  - **DB**: Dual Storage SSOT (SQLite Tables & Views ↔ Firebase RTDB)

### Stage 3: System Design (OOPS MVVM Architecture)
- **Architectural Mapping**: Verify dependencies in `.agent/enterprise-22-agents/PROGRAM-MAPPING.md`.
- **Design Tokens**: Select theme palette (Themes 0–11) and L7 UI components (Glassmorphism, 16f–30f radii, 50f capsule buttons, 8f–25f elevation).
- **Temporal Query Guarantee**: Ensure all database reads plan for descending ordering (`ORDER BY timestamp DESC`).

### Stage 4: Implementation (Modular OOPS Build)
- **Pre-Change Declaration Mandatory**:
  $$\text{[Variables]} \quad\vert\quad \text{[Constants]} \quad\vert\quad \text{[Dependencies]} \quad\vert\quad \text{[Integration Points]}$$
- **Ponytail 96% Rule**: If a class or file reaches $\ge 1,920$ lines, decompose via Strangler Fig pattern or call-by-reference modular controllers.
- **Dual-Module Synchronization**: Every line written to `app/src/main/...` must be mirrored in `jButterFly/src/main/...`.
- **Non-Destructive Code Evolution**: Never delete earlier working code; deprecate gracefully (`@Deprecated`) or merge.

### Stage 5: Testing & Parity Verification
- **Dual-Module Parity Audit**: Line-by-line verification across all 14 twin pairs.
- **Basic Error Zero-Tolerance**: Unresolved references, missing parameters, and missing imports are strictly blocked.
- **RAG Status Engine Test**: Verify 15m (Green), 15m–6h (Amber), >6h (Red), SOS (Crimson), Ghost (Gray).
- **Navigation & Lifecycle**: Verify D-Pad, Enter action, Tab sequence, and predictive back callback.

### Stage 6: Deployment & Build Quarantine
- **Strict Build Quarantine Protocol**: **NEVER execute Gradle builds (`./gradlew`, `./gradlew.bat installDebug`) autonomously.**
- Code modifications are decoupled from build tasks until explicitly commanded by operator `JEET`.
- Package hash calculation (`SHA-256`) and manifest identity review via Agent 21.
- **Hardware & Software Pre-Flight Evaluation**: Evaluate battery ($\ge 15\%$), free storage ($\ge 150\text{ MB}$), and keystore certificate match per `.agent/rules/lifecycle_rules.md`.

### Stage 7: Maintenance & Closed-Loop Feedback
- **Self-Healing Watchdog**: Monitor SQLite integrity and process resurrection.
- **Rolling Maintenance**: Periodic SQLite `VACUUM` and log compression via `BackgroundJobManager`.
- **Pre-Uninstall Teardown Guard**: Stop background sentinels, flush offline queues to cloud, offer forensic SQLite dump, before triggering native `ACTION_DELETE` package intent.
- **Feedback Isolation**: Post-task feedback triggers must NOT spawn uncontrolled recurring workflows.
