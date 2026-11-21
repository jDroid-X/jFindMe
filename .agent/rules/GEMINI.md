# Master Operational Router (GEMINI.md V5.0)
## Codename: jDroid-X | Lead Operator: JEET

> **Mandatory Reference Pipeline**: `Workflow (.agent/rules/workflow.md)` ➔ `Rules (.agent/rules/rules.md)` ➔ `Agents (.agent/rules/agents.md)` ➔ `Skills (.agent/rules/skills.md)` ➔ `Branding (.agent/rules/branding.md)` ➔ `Lifecycle (.agent/rules/lifecycle_rules.md)`

---

## 🧭 Operational Routing Logic

Every prompt, task, patch, or command issued by **JEET** is routed through this operational decision matrix:

```
                      [ Incoming Operator Request ]
                                   │
                                   ▼
                   [ Check The 4 Hard Gates & Red-Line ]
                                   │
         ┌─────────────────────────┴─────────────────────────┐
         │                                                   │
    [Violation?]                                         [Passed]
         │                                                   │
         ▼                                                   ▼
[Trigger Red-Line Confirmation                    [Select Adaptive Lane]
 or Immediate Lockout]                             ├─ Lane 1: Patch
                                                   ├─ Lane 2: Feature
                                                   └─ Lane 3: Enterprise
                                                             │
                                                             ▼
                                             [Execute 6-Step Pipeline]
                                             1. Workflow (.agent/rules/workflow.md)
                                             2. Rules (.agent/rules/rules.md)
                                             3. Agents (.agent/rules/agents.md)
                                             4. Skills (.agent/rules/skills.md)
                                             5. Branding (.agent/rules/branding.md)
                                             6. Lifecycle (.agent/rules/lifecycle_rules.md)
                                                             │
                                                             ▼
                                             [Pre-Change Declaration]
                                             [Variables] | [Constants] |
                                             [Dependencies] | [Integrations]
                                                             │
                                                             ▼
                                             [Execute Non-Destructive Code Mod]
                                                             │
                                                             ▼
                                             [Enforce Dual-Module Parity]
                                             app/src/main ⟷ jButterFly/src/main
                                                             │
                                                             ▼
                                             [Post-Change Verification & Audit]
```

---

## 🚦 Lane Routing Matrix

| Trigger Scenario | Assigned Lane | Traversal Depth | Gatekeeper Agents |
|:---|:---|:---|:---|
| Bug fix, typo, null-pointer, single file tweak | **Lane 1 (Patch)** | Fast-Exit check, Pre-declaration, Dual sync | Agent 18 (QA), Agent 19 (Crash) |
| New screen, component, dialog, or background job | **Lane 2 (Feature)** | Coordinate Tree `(ROOT⊃BR⊃EL⊃FE⊃BE⊃DB)`, L7 UI | Agent 2 (Specs), Agent 14 (UI), Agent 16 (Theme) |
| Schema change, mesh rewrite, identity refactor | **Lane 3 (Enterprise)**| Full 7-Stage Closed-Loop Waterfall | Agent 1 (Orchestrator), Agent 3 (HLD), Agent 9 (DB) |

---

## 🛡️ Router Safety Invariants & Hard Gates
1. **Gate 1 (Minors Protection)**: Zero telemetry leaks of child nodes (`4379A761` / Riddhi).
2. **Gate 2 (Irreversible Ops)**: Delete, overwrite, drop, push require 1 confirmation prompt.
3. **Gate 3 (Live-Fire Target)**: Prohibit blind external socket bursts against live targets.
4. **Gate 4 (CSAM)**: System locked.
5. **Red-Line Gate**: If monitoring priority (Ward Top Position 0) or circle scoping is affected, immediately halt:
   > **`Confirming - "Violating Prime Directive Rules"`**
6. **Sovereign Web Workspace Invariant**: All website, HTML, CSS, client-side JS, web dashboard, and web server development MUST strictly reference and operate within `C:\Users\jiten\jAnitGravity\jFindMeWeb\`. Never create web/HTML presentation files inside the Android project root (`jFindMe/`).

---

## 📦 Master References
- **Constitution**: `.agent/rules/rules.md`
- **Workflow**: `.agent/rules/workflow.md`
- **Specialist Agents**: `.agent/rules/agents.md` & `.agent/enterprise-22-agents/Agent-Registry.md`
- **Domain Skills**: `.agent/rules/skills.md`
- **Brand Identity**: `.agent/rules/branding.md`
- **Lifecycle Rulebook**: `.agent/rules/lifecycle_rules.md`
- **Briefing Master**: `.agent/rules/J_DROID_X_BRIEFING.md`
- **Program Mapping**: `.agent/enterprise-22-agents/PROGRAM-MAPPING.md`
- **Sovereign Web Workspace**: `C:\Users\jiten\jAnitGravity\jFindMeWeb\`
