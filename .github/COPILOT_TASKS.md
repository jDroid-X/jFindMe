# Copilot Tasks for jFindMe

## Common Development Tasks

### Task 1: Add New Location Field
```prompt
Add a new field to LocationData model and update Firebase synchronization in LocationRepository.kt
```

### Task 2: Create New Fragment
```prompt
Create a new fragment following MVVM pattern with navigation setup in NavGraph.xml
```

### Task 3: Implement Geofence Alert
```prompt
Implement geofence enter/exit detection using ray-casting algorithm from data_processor.py
```

### Task 4: Fix Crash in Service
```prompt
Analyze and fix crash in TrackingService. Check for null pointers and lifecycle issues
```

### Task 5: Update Firebase Security Rules
```prompt
Update firebase-database.rules.json to add new permission for [feature]
```

### Task 6: Optimize Battery Usage
```prompt
Review and optimize location polling intervals in TrackingService for battery savings
```

### Task 7: Add Offline Sync
```prompt
Implement offline queue for [entity] using SQLite and sync on reconnect
```

### Task 8: Create Reusable UI Component
```prompt
Create a reusable card component following Glassmorphism design with RAG status indicator
```

## Quick Commands

| Command | Description |
|---------|-------------|
| `/android` | Android-specific code generation |
| `/firebase` | Firebase integration help |
| `/geofence` | GIS and geofencing implementation |
| `/crash` | Crash analysis and fix |
| `/architecture` | Architecture guidance |

## Code Review Checklist

When reviewing changes, check for:
- [ ] Proper null safety checks
- [ ] Lifecycle-aware components
- [ ] Permission handling
- [ ] Error boundaries
- [ ] Memory leak prevention
- [ ] Battery optimization
- [ ] Offline support
- [ ] Security validation
