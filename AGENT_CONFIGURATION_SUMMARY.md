# jFindMe - Agent Configuration Summary

## Files Created

### 1. Project Root Configuration
- `.prompt.md` - AI agent context prompt for Copilot
- `.agent.md` - Agent configuration with model settings
- `AGENT_WORKFLOW_REFERENCE.md` - Quick reference guide
- `WORKFLOW_GUIDE.md` - Detailed workflow documentation
- `QUICK_REFERENCE.md` - Cheat sheet for quick lookup

### 2. VS Code Configuration
- `.vscode/settings.json` - Project-specific VS Code settings
- `.vscode/extensions.md` - Recommended extensions list
- `.github/COPILOT_TASKS.md` - Copilot task templates
- `.gitignore.local` - Additional ignore rules

### 3. Claude Skills (Custom Agent Capabilities)
- `~/.claude/skills/firebase-integration/SKILL.md`
- `~/.claude/skills/geofencing-gis/SKILL.md`
- `~/.claude/skills/chaquopy-python-kotlin/SKILL.md`
- `~/.claude/skills/android-background-services/SKILL.md`
- `~/.claude/skills/android-mvvm-architecture/SKILL.md`

## Agent Role Assignments

| Agent | Name | Skill Triggered |
|-------|------|-----------------|
| 1 | Product Architect | Schema governance, Hardware identity |
| 2 | Backend Engineer | MVVM, Repositories, ViewModels |
| 3 | UI/UX Architect | Components, Navigation, Glassmorphism |
| 4 | QA Engineer | Testing, Integration, Validation |
| 5 | Crash Analyst | Stability, ANR prevention, Diagnostics |
| 6 | GIS Engineer | Geofencing, Spatial math, Haversine |
| 7 | Performance Engineer | Battery, Optimization, Indexing |

## Custom Skills Map

| Skill | Covers | Files Referenced |
|-------|--------|------------------|
| **firebase-integration** | RTDB, Auth, Security Rules, Sync | `FIREBASE_SCHEMA_ARCHITECT.md`, `firebase-database.rules.json` |
| **geofencing-gis** | Distance calc, Polygon bounds, SafeZones | `data_processor.py`, `ENGINE_HLD_DESIGN.md` |
| **chaquopy-python-kotlin** | Python interop, JSON bridge, Debugging | `app/src/main/python/data_processor.py` |
| **android-background-services** | ForegroundService, Doze mode, WorkManager | `service/TrackingService.kt`, `service/FCMService.kt` |
| **android-mvvm-architecture** | ViewModel, Repository, Lifecycle | `ui/`, `data/repository/`, `engine/EngineRegistry.kt` |

## Workflow Integration

### For New Features:
1. Check `ARCHITECTURE_ASSIGNMENTS.md` for current agent status
2. Review `ENTERPRISE_HLD_DESIGN.md` for system context
3. Follow branch naming convention: `feat/<feature-name>`
4. Commit with conventional commit format

### For Bug Fixes:
1. Check `FORENSIC_AUDIT_CLOSED_LOOP_WORKFLOW.md`
2. Analyze crash logs in `crash_logs*.txt`
3. Route to appropriate agent (primarily Agent 5 for crashes)
4. Validate with Agent 4 before merging

### For Architecture Changes:
1. Update `ProgramMapping.md` first
2. Get Agent 1 (Architect) approval
3. Document changes in relevant .md files
4. Run integration tests (Agent 4)

## Next Steps

1. **Review and customize** the `.prompt.md` for your specific needs
2. **Install recommended extensions** from `.vscode/extensions.md`
3. **Test the skills** by asking about Firebase, geofencing, or background services
4. **Update workflow** as the project evolves
5. **Add more skills** as new requirements emerge

---
*Agent configuration complete for jFindMe Android project*
*Created: 2026-09-12*
