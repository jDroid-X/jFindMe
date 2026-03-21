# ProgramMapping - Master Relationship Table (Enterprise Standard v3.0)

| Main Program (L1) | Sub Program (L2) | Sub Program (L3) | Sub Program (L4) | Sub Program (L5) | Responsibility |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **jFindMe App** | **UI Engine (Java)** | `BaseActivityJava` | `ThemeManager` | `ThemePalette` | Holistic UI Styling & 7 Themes |
| | | `AuthActivity` | `LoginForm` | `LoginLogic` | Java-based Programmatic Login |
| | | `MapActivity` | `RadarScanner` | `AvatarMovement` | Pixel-level Map UI (70:30 split) |
| | | `GroupActivity` | `PairingForm` | `LinkLogic` | Device Pairing & Group Mgmt |
| | | `ManagementCluster`| `ProfileActivity` | `PrivacyActivity` | User & Security Management |
| | | | `HistoryActivity` | `GeofenceActivity`| Data & Spatial Interaction |
| | | | `HealthActivity` | `NotifActivity` | System & Alert Status |
| | **Location Engine (Ktx)**| `AdaptiveTracker` | `PermissionFlow`| `FusedLocProvider` | Precise GPS & Sensor Fusion |
| | **Map Engine (Ktx)** | `MapController` | `MarkerClusterer` | `MarkerInterpolator` | High-Perf Rendering & Smoothing |
| | **Data Engine (Py)** | `SpatialProcessor` | `GeohashEngine` | `CleanEngine` | Python-based DB & Geo Queries |
| | **System Layer** | `SystemLogger` | `AlertManager` | `SecurityEngine` | Enterprise Safety & Logging |

## Current Sprint Objectives (Waterfall Execution):
1. **Redo activities**: Ensure all UI Cluster activities follow strict programmatic rules.
2. **Landscape Rule**: Implement 70:30 split with toggle icons in all new activities.
3. **Data Integrity**: Route all spatial/status queries through `data_processor.py`.
4. **Zero Dead-Ends**: Link all Navigation Drawer items to actual functional sub-programs.
5. **Security Agent**: Enforce Root/Mock detection at activity launch level.
