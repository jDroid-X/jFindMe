# jFindMe - AI Agent Onboarding Checklist

## For New Developers/AI Agents

### Step 1: Project Familiarization
- [ ] Read `ARCHITECTURE_ASSIGNMENTS.md` - Understand agent roles
- [ ] Read `ENTERPRISE_HLD_DESIGN.md` - System architecture overview
- [ ] Read `AGENT_WORKFLOW_REFERENCE.md` - Quick reference guide
- [ ] Read `QUICK_REFERENCE.md` - Cheat sheet

### Step 2: Code Structure
- [ ] Explore `app/src/main/java/com/jdroidx/jfindme/` directory
- [ ] Review `engine/EngineRegistry.kt` for service dependencies
- [ ] Check `data/repository/` for data flow patterns
- [ ] Examine `ui/` for navigation structure

### Step 3: Key Documents
- [ ] `FIREBASE_SCHEMA_ARCHITECT.md` - Database schema
- [ ] `FORENSIC_AUDIT_CLOSED_LOOP_WORKFLOW.md` - Crash analysis
- [ ] `ProgramMapping.md` - Dependency map
- [ ] `DEVICE_CONNECTIVITY_RULEBOOK.md` - Hardware identity

### Step 4: Build Setup
- [ ] Open project in Android Studio
- [ ] Sync Gradle dependencies
- [ ] Verify `local.properties` points to Android SDK
- [ ] Run debug build: `./gradlew assembleDebug`

### Step 5: Agent Configuration
- [ ] Review `.vscode/.prompt.md` for context
- [ ] Check `.agents/rules/rules4agents.md` for protocols
- [ ] Read relevant SKILL.md files in `~/.claude/skills/`

### Step 6: Testing
- [ ] Install debug APK on test device
- [ ] Verify location permissions granted
- [ ] Test foreground service starts correctly
- [ ] Check Firebase connection in debug mode

---

## Critical Reminders

⚠️ **NEVER run gradle builds autonomously** - Only user can trigger builds  
⚠️ **NEVER delete existing code** - Update or merge instead  
⚠️ **Hardware ID is immutable** - Physical serial is SSOT  
⚠️ **Always check ProgramMapping.md** - Before making changes  
