# JButterfly / JFindMe - Agent & Skill Implementation Guide

## ✅ Implemented

### Custom Agents (`.github/agents/`)
| Agent | Purpose | Invocation |
|-------|---------|------------|
| `map-gis-specialist.agent.md` | Leaflet maps, geofences, coordinate math | Map-related tasks |
| `firebase-data-engineer.agent.md` | Schema design, security rules, sync | Database tasks |
| `telemetry-dashboard-designer.agent.md` | HUD UI, glassmorphism, themes | UI/UX tasks |
| `gps-telemetry-validator.agent.md` | GPS validation, simulation, QA | Testing tasks |

### Skills (`.github/skills/`)
| Skill | Use When |
|-------|----------|
| `map-development/SKILL.md` | Marker clustering, animations, projection math |
| `firebase-integration/SKILL.md` | Security rules, offline queue, query patterns |
| `performance-profiling/SKILL.md` | FPS profiling, memory leaks, bundle analysis |

### Prompts (`.github/prompts/`)
| Prompt | Input | Purpose |
|--------|-------|---------|
| `debug-geofence.prompt.md` | deviceId, zoneType | Geofence boundary testing |
| `audit-map-performance.prompt.md` | markerCount | Performance benchmarking |
| `review-firebase-schema.prompt.md` | collectionName | Schema optimization |

### File Instructions (`.github/instructions/`)
| Instruction | Applies To | Focus |
|-------------|------------|-------|
| `map-view.instructions.md` | MapController, *MapView*.js | Marker management, event cleanup |
| `firebase-service.instructions.md` | FirebaseService, models/*.js | Connection management, offline support |
| `uix-component.instructions.md` | views/*.js, components/*.js | Component structure, accessibility |

---

## 🎯 Recommended Additional Agents (from .kit framework)

Based on `AGENTS_FRAMEWORK_KIT.md`, consider implementing these from the 8-agent suite:

```
LAYER 1: ARCHITECTURE
├── Master Orchestrator & Enterprise Architect  [P0 - if complex refactors]
└── Spatial & Unified Database Architect        [P1 - multi-database sync]

LAYER 2: REALTIME ENGINE
├── Realtime Sync & Backend Specialist          [P1 - WebSocket/RTDB]
└── GIS & Mapping Specialist                    [P2 - vector tiles]

LAYER 3: UI/UX
├── UI/UX & Telemetry HUD Designer              [IMPLEMENTED]
└── Performance & Battery Optimizer             [P2 - adaptive polling]

LAYER 4: SECURITY
├── Security & Device Health Auditor            [P2 - mock GPS detection]
└── QA & E2E Validation Engineer                [P3 - GPS trail sim]
```

---

## 🚀 How to Use

### Invoke Agents in Chat
```
@map-gis-specialist Fix the marker clustering issue with 200+ devices
@firebase-data-engineer Write security rules for safeZones collection
@gps-telemetry-validator Validate this GPS payload format
```

### Run Skills as Slash Commands
```
/map-development Implement smooth marker interpolation
/firebase-integration Design schema for device history
/performance-profiling Profile map rendering at 500 markers
```

### Use Prompts for Focused Tasks
```
/debug-geofence device-123 home
/audit-map-performance 200
/review-firebase-schema devices
```

---

## 📋 Next Steps

1. **Test the agents**: Use `@map-gis-specialist` to review existing map code
2. **Run the skill**: Execute `/map-development` to generate a new feature
3. **Create more prompts**: Add domain-specific prompts as needed
4. **Extend file instructions**: Add more `applyTo` patterns for other modules

---

## 🔗 References
- [.kit AGENTS_FRAMEWORK_KIT.md](../../AGENTS_FRAMEWORK_KIT.md)
- [VS Code Agent Docs](https://code.visualstudio.com/docs/copilot/customization/custom-agents)
- [Skill Creation Guide](./references/skill-creation.md)
