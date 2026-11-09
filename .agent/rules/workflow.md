# jFindMe Waterfall MVVM Workflow

This master workflow governs all development tasks, ensuring a systematic, non-destructive approach from forensic analysis to final audit.

---

## 🧭 Master 4-Step Reference Protocol: Workflow ➔ Rules ➔ Agents ➔ Skills

When ANY change, patch, bug fix, or feature request is received, every agent and developer MUST traverse this pipeline in exact order:

```
┌────────────────────────────────────────────────────────────────────────┐
│ STEP 1: WORKFLOW (.agent/rules/workflow.md)                            │
│ Check Concurrency (< 4 agents), Single Workflow lock, Waterfall Phase  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ STEP 2: RULES (.agent/rules/rules.md)                                  │
│ 1. Check TIER 1 Universal Invariants (Pre-declaration, Parity, SSOT)   │
│ 2. Identify TIER 2 Screen-Dependent Subset (Subsets A - H)             │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ STEP 3: AGENTS (.agent/rules/agents.md)                                │
│ Assign task to the designated Specialist Agent (Agent 1 - 7)          │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ STEP 4: SKILLS (.agent/rules/skills.md)                                │
│ Activate required technical capability & execute with zero stubs       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Phase 0: Agent Dispatch & Concurrency Control (Global Guard)

- **Rule 1 (Single Workflow)**: Only ONE workflow instance allowed per project. If running, attach to it.
- **Rule 2 (Pre-Spawn Guard)**: Before spawning, check if `active_agents >= 4`. If true, queue task immediately.
- **Rule 3 (Duplicate Execution Block)**: Do NOT start another instance if the same `task_id` is already running.
- **Rule 4 (Task Merge Rule)**: If tasks share the same input and stage, merge them into one execution.
- **Rule 5 (Controlled Parallelism)**: Parallel execution is ONLY allowed for Model vs Controller vs View. All other tasks execute sequentially.

## Phase 1: Forensic Analysis (The "Look-Ahead")

- **Step 1**: Identify the target module or feature requested.
- **Step 2**: Audit current implementation for existing logic, technical debt, and brownfield file sizes ($\ge 1,920$ lines).
- **Step 3**: Identify all dependencies (Classes, Sub-routines, APIs).
- **Step 4**: **Pre-Change Declaration Mandatory**:
  $$\text{[Variables]} \quad\vert\quad \text{[Constants]} \quad\vert\quad \text{[Dependencies]} \quad\vert\quad \text{[Integration Points]}$$
- **Step 5**: **Verification Rule**: Check for missing links, caller references, or integration gaps across BOTH modules (`app` and `jButterFly`) before proceeding.

## Phase 2: Structural Planning (The "Design-Wait")

- **Step 1**: Define the Scenario and Sub-Scenarios.
- **Step 2**: Check Tier 1 Universal Rules and select the exact Tier 2 Screen-Dependent Subset in `.agent/rules/rules.md`.
- **Step 3**: Design the View-ViewModel-Repository structure (Waterfall MVVM).
- **Trigger**: Move to Solutioning ONLY after the plan is approved. (Do NOT create uncoordinated sub-branches during initial execution).

## Phase 3: Solutioning (The "Modular-Build")

- **Step 1**: Develop modular, reusable functions/Classes (Ponytail ceiling: $\le 1,920$ lines).
- **Step 2**: Implement the "Closed-Loop" data binding and bi-directional SQLite/Firebase synchronization.
- **Step 3**: Apply L7 UI Architect styles (Two-point Linear Gradients, Glassmorphism, 16f-30f Radii, Capsule Buttons).
- **Rule**: No duplicate code. Re-use existing components where possible via call-by-reference.

## Phase 4: Implementation & Audit (The "Zero-Gap")

- **Step 1**: Integrate components and establish Firebase sync hooks with type-safe deserializers (`safeLong`, `safeInt`).
- **Step 2**: Execute Tab-Sequence, Enter-Action, and Predictive Back navigation validation.
- **Step 3**: Perform Cross-Device usability and performance audit (Map rendering throttled $\ge 250\text{ ms}$).
- **Step 4**: **Strict Build Quarantine Protocol**: Decouple all code modifications from build tasks. NEVER execute `./gradlew` autonomously.
- **Step 5**: Trigger a Feedback Loop ONLY after main workflow completes (Feedback must NOT auto-trigger workflows).

## Final Validation Protocol

1. **Module Parity**: All 14 twin file pairs between `app/` and `jButterFly/` verified 100% synchronized?
2. **Consistency**: UI, local SQLite, and Firebase Cloud SSOT perfectly synchronized?
3. **Accessibility**: D-Pad, Marquee text, and Back-navigation functional?
4. **Security**: Mandatory fields marked with red asterisk, RBAC checked, and hardware root anchor immutable?
5. **Performance**: Zero UI blocking during IO operations; markers lightweight and GPU-optimized?
