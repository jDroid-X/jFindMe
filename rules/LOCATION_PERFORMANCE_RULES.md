# Location Performance & Accurate Data Display Rules
Source Framework: `C:\Users\jiten\jAnitGravity\.kit`

## 1. High Performance Telemetry
- **Adaptive Polling:** High-frequency location updates (1-3s) when speed > 5 km/h; throttle to low-frequency (30s) when stationary.
- **Marker Movement:** Update marker `setLatLng` directly via transform matrices; NEVER re-initialize or reload the map instance.
- **Data Batching:** Aggregate location history points before committing to database to minimize network overhead.

## 2. Accurate Data Display
- **Accuracy Radius:** Always render a translucent circle representing horizontal accuracy radius (`accuracy` in meters).
- **Sub-Pixel Interpolation:** Smoothly interpolate coordinates between GPS ticks to prevent marker jumping or stuttering.
- **Single Source of Truth:** Changes to Avatar, Nickname, or Safe Zone must cascade across all UI views and tables simultaneously.

## 3. Circuit Breakers & Fallback Handling
- If Firebase RTDB connection drops, seamlessly switch to IndexedDB/local cache and display an offline badge with timestamp.
- On GPS loss, indicate "Last Known Location" with timestamp rather than clearing the display.

## 4. Performance Optimization Rules
### Marker Management
- Cluster markers when count > 100 using Leaflet.markercluster
- Remove markers outside viewport during zoom/pan
- Use SVG icons instead of PNG for better scaling
- Recycle marker instances instead of creating new ones

### Coordinate Updates
- Throttle GPS updates to min 1 second interval
- Batch multiple position updates into single re-render
- Use requestAnimationFrame for smooth interpolation
- Skip render if delta < threshold (e.g., 5 meters)

### Memory Management
- Clear all event listeners on component destroy
- Use WeakRef for cache entries where possible
- Implement object pooling for frequently created objects
- Monitor with performance.memory API in dev mode

### Bundle Size
- Tree-shake unused Leaflet plugins
- Lazy-load heavy features (history playback, analytics)
- Code-split by route/view
- Target < 500KB gzipped total

### Testing Thresholds
| Metric | Warning | Critical |
|--------|---------|----------|
| FPS | <50 | <30 |
| Load Time | >5s | >10s |
| Memory Growth | >50MB/session | >100MB/session |
| Update Latency | >200ms | >500ms |

Benchmark at 100, 500, and 1000 concurrent markers. Measure with Chrome DevTools Performance tab.
