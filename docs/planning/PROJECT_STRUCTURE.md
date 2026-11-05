# jFindMe - Project Structure Map

## Root Level
```
jFindMe/
├── .agents/rules/          # Agent protocols and rules
├── .github/                # GitHub integration files
├── .vscode/                # VS Code settings and prompts
├── app/                    # Main Android module
├── docs/                   # Documentation assets
├── engine/                 # Engine documentation
├── requirement/            # Requirements tracking
├── requirements/           # Additional requirements
├── scratch/                # Temporary work files
└── tmp/                    # Temp files
```

## App Module Structure
```
app/
├── src/main/
│   ├── java/com/jdroidx/jfindme/
│   │   ├── data/
│   │   │   ├── local/        # SQLite helpers and DAOs
│   │   │   ├── model/        # Data classes (LocationData, UserProfile, etc.)
│   │   │   ├── remote/       # Firebase repositories
│   │   │   └── repository/   # Unified repository interface
│   │   ├── engine/
│   │   │   ├── alert/        # SOS and alert engines
│   │   │   ├── avatar/       # Avatar sync engine
│   │   │   ├── bridge/       # Python-Kotlin bridge
│   │   │   ├── data/         # Data processing engines
│   │   │   ├── group/        # Group management engine
│   │   │   ├── location/     # Location tracking engine
│   │   │   ├── map/          # Map rendering engine
│   │   │   ├── security/     # Security engine
│   │   │   ├── sync/         # Sync engine
│   │   │   └── EngineRegistry.kt  # Service locator singleton
│   │   ├── receiver/         # BroadcastReceivers
│   │   ├── service/          # Background services
│   │   ├── ui/
│   │   │   ├── alert/        # Alert dialogs and UI
│   │   │   ├── auth/         # Login/Register flows
│   │   │   ├── base/         # Base Activity/Fragment classes
│   │   │   ├── group/        # Group management screens
│   │   │   ├── management/   # Admin management UI
│   │   │   ├── map/          # Map views
│   │   │   └── settings/     # Settings screens
│   │   ├── utils/            # Helper utilities
│   │   └── JFindMeApplication.java  # Application entry point
│   ├── python/
│   │   └── data_processor.py  # Python GIS functions
│   ├── res/
│   │   ├── drawable/         # Icons and backgrounds
│   │   ├── layout/           # XML layouts
│   │   ├── mipmap-*/         # App icons
│   │   ├── values/           # Strings, colors, themes
│   │   └── xml/              # Preferences, network config
│   └── AndroidManifest.xml
│
├── build.gradle.kts          # Module build config
└── google-services.json      # Firebase config (gitignored)
```

## Key Configuration Files
```
build.gradle.kts          # Kotlin DSL build file (root)
app/build.gradle.kts      # App module build config
gradle/libs.versions.toml # Version catalog
keystore.properties       # Signing config (gitignored)
version.properties        # Auto-incrementing version
local.properties          # SDK paths (gitignored)
```

## Documentation Files
```
ARCHITECTURE_ASSIGNMENTS.md      # Agent role definitions
ENTERPRISE_HLD_DESIGN.md         # System architecture
FIREBASE_SCHEMA_ARCHITECT.md     # Database schema
FORENSIC_AUDIT_CLOSED_LOOP_WORKFLOW.md  # Crash analysis
GLOBAL_UI_ARCHITECT_RULES.md     # UI standards
GLOBAL_UI_ARCHITECT_WORKFLOW.md  # UI workflow
ProgramMapping.md                # Dependency mapping
DEVICE_CONNECTIVITY_RULEBOOK.md  # Hardware identity rules
NavigationFlowDiagram.md         # Navigation structure
UserManual.md                    # End-user documentation
```

## Firebase Configuration
```
firebase.json              # Firebase CLI config
firebase_database.rules.json  # RTDB security rules
firestore.rules            # Firestore rules (if used)
database.rules.json        # Legacy rules backup
```
