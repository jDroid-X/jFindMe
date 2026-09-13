# jFindMe - Workflow Guide

## Development Workflow

### 1. Branch Strategy
```
main                  # Production-ready code
├── feat/feature-name # New features
├── fix/issue-name    # Bug fixes
└── refactor/module   # Code cleanup
```

### 2. Commit Convention
```bash
# Format: <type>(<scope>): <description>
feat(auth): add biometric login support
fix(service): prevent ANR in TrackingService
refactor(engine): optimize Haversine calculations
chore(deps): update Firebase BoM to 34.10.0
```

### 3. Code Review Checklist
- [ ] Follows MVVM architecture patterns
- [ ] Proper null safety (no `!!` unless intentional)
- [ ] Lifecycle-aware components
- [ ] Permission handling for sensitive operations
- [ ] Error boundaries and fallback UI
- [ ] Battery optimization considered
- [ ] Offline sync capability
- [ ] Security validation on inputs
- [ ] No hardcoded values (use resources)

### 4. Build Commands
```bash
# Debug build (for testing)
./gradlew assembleDebug

# Release build (auto-increments version)
./gradlew assembleRelease

# Clean build cache
./gradlew clean

# Run unit tests
./gradlew test

# Generate APK
./gradlew app:assembleDebug
```

### 5. Testing Strategy
| Test Type | Location | Tools |
|-----------|----------|-------|
| Unit Tests | `app/src/test/` | JUnit 4, MockK |
| Instrumented Tests | `app/src/androidTest/` | Espresso,@RunWith(AndroidJUnit4.class) |
| Integration Tests | Manual verification | Device testing |

### 6. Debugging Checklist
1. Check Logcat for "jFindMe" tagged logs
2. Verify Firebase Realtime Database rules
3. Confirm hardware identity is captured correctly
4. Test on multiple Android versions (26+)
5. Validate offline → online sync behavior
6. Monitor battery usage during location tracking

### 7. Agent Assignment Matrix

| Task Type | Primary Agent | Supporting Agents |
|-----------|---------------|-------------------|
| New feature design | Agent 1 (Architect) | Agent 2, 3 |
| Firebase integration | Agent 2 (Backend) | Agent 1, 5 |
| UI implementation | Agent 3 (UI/UX) | Agent 2 |
| Bug fix / Crash | Agent 5 (Crash Hunter) | Agent 4, 7 |
| Geofencing logic | Agent 6 (GIS) | Agent 2, 7 |
| Performance optimization | Agent 7 (Perf) | Agent 2, 5 |
| QA / Validation | Agent 4 (QA) | All agents |

### 8. Release Process
1. Update `version.properties` if needed
2. Run full test suite
3. Generate signed release APK
4. Upload to Firebase App Distribution
5. Update changelog in `UserManual.md`
6. Notify team via designated channel

### 9. Documentation Updates
When making changes, update:
- `ARCHITECTURE_ASSIGNMENTS.md` - If agent responsibilities change
- `FIREBASE_SCHEMA_ARCHITECT.md` - If database schema changes
- `AGENT_WORKFLOW_REFERENCE.md` - If workflow changes
- `ProgramMapping.md` - If dependencies change

### 10. Emergency Procedures
**If app crashes repeatedly:**
1. Collect crash logs from `crash_logs*.txt`
2. Analyze with Agent 5 (Crash Hunter)
3. Check foreground service lifecycle
4. Review Doze mode restrictions

**If data sync fails:**
1. Verify Firebase connection
2. Check offline queue in SQLite
3. Trigger manual sync via `BackgroundJobManager`
4. Review security rules in `firebase-database.rules.json`
