# Implementation Plan 02: System Feature Hardening & Closed-Loop Architecture Gap Fixes

## 1. Overview
A systematic audit and architectural hardening of the jFindMe platform focusing on:
- **Map SafeZone Boundary Visualization**: Connecting `MapViewModel` and `MapActivity` with `DatabaseHelper` and Firebase RTDB (`safe_zones`) to render active geofences dynamically.
- **Closed-Loop Avatar & Profile Propagation**: Wiring real-time callbacks via `AvatarSyncEngine` to refresh map markers, callouts, and Drawer headers in 1-click.
- **Lifecycle Synchronization (`onResume`)**: Injecting lifecycle reload routines in `MapActivity.java` and `FamilyFriendsActivity.java`.
- **Target Node Identity Binding**: Resolving target node avatar rendering in `PersonDetailsActivity.java`.
- **Navigation Drawer Hierarchy (Rule 7 Standard)**: Aligning navigation targets to prevent dead routes.

---

## 2. Technical Scope & Changes

### A. SQLite & Data Access Layer (`DatabaseHelper.kt`)
- Added `getActiveSafeZones(): List<SafeZoneModel>` method querying active, non-archived boundaries for map rendering.

### B. Live Map Tracking Engine (`MapViewModel.kt`, `MapActivity.java`)
- `MapViewModel.kt`: Implemented `loadSafeZones()` pulling local SQLite safe zones and cloud Firebase `safe_zones`, publishing updates via `safeZones` LiveData.
- `MapActivity.java`: Added observer for `viewModel.safeZones` to dynamically draw themed boundary circles (`CircleOptions`) on Google Map.
- Registered with `AvatarSyncEngine.registerClosedLoopCallback` to trigger marker bitmap invalidation and nav header refresh instantly on avatar changes.
- Enhanced `onResume` to refresh user nickname, avatar, and active safe zones.

### C. Family & Friends Roster Management (`FamilyFriendsActivity.java`, `PersonDetailsActivity.java`)
- `FamilyFriendsActivity.java`: Fixed `onResume` to re-sync local/cloud roster data when returning from node edits or device pairing.
- `PersonDetailsActivity.java`: Fixed header avatar binding to display the target node's avatar via `AvatarLoader.bindAvatar(...)` when viewing dependent/peer cards.

### D. Multi-Device Sync Engine (`AvatarSyncEngine.kt`)
- Added `@JvmStatic` and `@JvmSuppressWildcards` on `registerClosedLoopCallback` and `unregisterClosedLoopCallback` for seamless Java/Kotlin interop.

---

## 3. Verification & Quality Gates
- **Build Status**: Verified with `./gradlew.bat compileDebugSources` — **BUILD SUCCESSFUL (0 errors)**.
