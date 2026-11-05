# jFindMe - Quick Reference Card

## Architecture Overview
```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Agent 3)                    │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────┐  │
│  │  Auth   │ │  Group  │ │   Map   │ │  Settings   │  │
│  └────┬────┘ └────┬────┘ └────┬────┘ └──────┬──────┘  │
└───────┼───────────┼───────────┼─────────────┼─────────┘
        │           │           │             │
┌───────▼───────────▼───────────▼─────────────▼─────────┐
│              ViewModel Layer (Agent 2)                 │
│         StateFlow / LiveData / Result<T>               │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│               Repository Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌───────────────┐  │
│  │  Local DB   │  │   Firebase  │  │  Chaquopy Python│  │
│  │  (SQLite)   │  │   (RTDB)    │  │  (Haversine)  │  │
│  └─────────────┘  └─────────────┘  └───────────────┘  │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│              Engine Registry (Agent 6)                  │
│   Location | Sync | Alert | Map | Security              │
└─────────────────────────────────────────────────────────┘
```

## Key Technologies
| Category | Technology |
|----------|------------|
| Language | Kotlin (primary), Java (entry), Python (Chaquopy) |
| Architecture | MVVM + Engine Registry |
| Backend | Firebase Realtime DB, Auth, Analytics |
| Maps | Google Maps SDK + Utils |
| Location | FusedLocationProvider + Geofencing API |
| Database | SQLite (local) + Firebase (cloud) |
| Services | ForegroundService + WorkManager |
| Build | Gradle Kotlin DSL (AGP 8.7.3) |

## File Locations
```
app/src/main/java/com/jdroidx/jfindme/
├── data/
│   ├── local/       # SQLite helpers
│   ├── model/       # Data classes
│   ├── remote/      # Firebase repositories
│   └── repository/  # Unified repositories
├── engine/          # Core engines
│   ├── bridge/      # Python-Kotlin bridge
│   ├── location/    # Location engines
│   └── sync/        # Sync engines
├── service/         # Background services
├── ui/              # Activities & Fragments
├── utils/           # Helper classes
└── receiver/        # BroadcastReceivers

app/src/main/python/
└── data_processor.py  # GIS functions
```

## RAG Status Colors
| Status | Time Threshold | Color | Hex |
|--------|----------------|-------|-----|
| ACTIVE | < 15 minutes | 🟢 | #22C55E |
| INACTIVE | 15m - 6h | 🟡 | #F59E0B |
| INACTIVE | > 6 hours | 🔴 | #EF4444 |
| SOS | isSos=true | 🚨 | Pulsing |
| GHOST | Unlinked | ⚪ | #64748B |

## Agent Roles Quick Reference
| Agent | Role | Trigger When... |
|-------|------|-----------------|
| 1 | Product Architect | Schema changes, API contracts |
| 2 | Backend Engineer | Database ops, ViewModels, Repos |
| 3 | UI/UX Architect | Screens, components, navigation |
| 4 | QA Engineer | Testing, validation, quarantine |
| 5 | Crash Analyst | Crashes, ANRs, stability issues |
| 6 | GIS Engineer | Geofencing, spatial math, maps |
| 7 | Performance Eng. | Battery, optimization, queries |

## Critical Permissions
```xml
<!-- Location -->
ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION

<!-- Foreground Service -->
FOREGROUND_SERVICE, FOREGROUND_SERVICE_LOCATION, FOREGROUND_SERVICE_DATA_SYNC

<!-- Hardware -->
READ_PHONE_STATE, BLUETOOTH_CONNECT, CAMERA

<!-- Optimization -->
REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, WAKE_LOCK
```

## Common Commands
```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease

# Clean
./gradlew clean

# Test
./gradlew test
./gradlew connectedAndroidTest

# Dependencies
./gradlew app:dependencies
```

## Version Management
- `version.properties` controls semver
- `VERSION_CODE` auto-increments on release builds
- Current: 1.0.1 → versionCode: 2

## Security Notes
- Hardware serial is immutable SSOT
- Never overwrite with software UUID
- Use `keystore.properties` for signing
- ProGuard enabled for release builds

## Documentation
| Document | Purpose |
|----------|---------|
| `ARCHITECTURE_ASSIGNMENTS.md` | Agent roles & tasks |
| `ENTERPRISE_HLD_DESIGN.md` | System architecture |
| `FIREBASE_SCHEMA_ARCHITECT.md` | Database schema |
| `FORENSIC_AUDIT_CLOSED_LOOP_WORKFLOW.md` | Crash analysis |
| `AGENT_WORKFLOW_REFERENCE.md` | Quick reference |

---
*Last Updated: 2026-09-12*
