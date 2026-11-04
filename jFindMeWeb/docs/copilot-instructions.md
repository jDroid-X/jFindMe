---
name: copilot-instructions
description: "Project-wide coding standards and architecture guidelines for jButterfly GPS Telemetry"
---

# jButterfly / jFindMe Project Guidelines

## Architecture Standards

### MVC Closed-Loop Waterfall Pattern
All features must follow this 7-stage sequential flow:
1. **Initiate** — Define scope, requirements, and success criteria
2. **Requirements** — Specify functional and non-functional requirements
3. **Design** — Create class diagrams, data flow, and component boundaries
4. **Code** — Implement with proper separation of concerns
5. **Test** — Validate with unit tests, integration tests, and GPS simulation
6. **Deploy** — Package and verify zero asset link breaks
7. **Maintenance** — Monitor telemetry performance and update as needed

### Layer Responsibilities
| Layer | Files | Responsibility |
|-------|-------|----------------|
| Controllers | `js/controllers/*.js` | Business logic, user actions, orchestration |
| Models | `js/models/*.js` | Data storage, Firebase sync, validation |
| Services | `js/services/*.js` | External API integration (Firebase, Maps) |
| Views | `js/views/*.js` | DOM manipulation, UI rendering, animations |
| Components | `js/components/*.js` | Reusable UI widgets |

### Code Conventions
- **Private methods**: Prefix with `_` (e.g., `_calculateCenter()`)
- **Constants**: UPPER_SNAKE_CASE at module level
- **State management**: Use `Store.js` for shared state; never mutate directly
- **Async patterns**: Use async/await; reject with descriptive errors
- **JSDoc**: Document all public methods with `@returns`, `@param`, `@throws`

## Map Development Standards
- **Leaflet only**: Never mix Google Maps APIs
- **Marker optimization**: Cluster markers >100 units using Leaflet.markercluster
- **Projection math**: All coordinate calculations in `GeoUtils.js`
- **Smooth animations**: Use `L.DomUtil.setTransform()` with requestAnimationFrame
- **Geofence bounds**: Calculate properly using Haversine formula

## Firebase Integration Standards
- **Path structure**: Use flat collections: `devices/{deviceId}`, `safeZones/{zoneId}`
- **Security rules**: Validate ownership before write operations
- **Offline support**: Queue updates with exponential backoff retry
- **Real-time listeners**: Always remove listeners in cleanup to prevent memory leaks

## Git Workflow
- **Branch naming**: `feature/<module>-<description>` or `fix/<issue-description>`
- **Commit format**: `<type>(<scope>): <subject>`
  - Types: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `chore`
- **PR requirements**: Must include GPS test scenario and map render verification

## Performance Requirements
- **Target FPS**: 60fps on modern devices for map panning
- **Bundle size**: Keep under 500KB gzipped
- **Memory**: No more than 100MB for 500 concurrent markers
- **Battery**: Adaptive polling (movement-based throttling)

## Testing Requirements
- **Unit tests**: All utility functions (GeoUtils, validators)
- **Map tests**: Projection math, clustering, geofence intersection
- **Integration tests**: Firebase sync, offline queue, reconnection
- **GPS simulation**: Generate realistic trajectories for E2E testing
