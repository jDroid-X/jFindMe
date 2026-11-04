---
name: performance-profiling
description: "Profile and optimize performance for GPS telemetry dashboards. Use for CPU/memory profiling, map rendering benchmarks, battery drain analysis, and bundle optimization."
argument-hint: "Performance profiling task..."
user-invocable: true
---

# Performance Profiling Skill

## When to Use
- Map panning drops below 60fps
- High memory usage with many markers
- Battery drain concerns on mobile
- Slow initial page load
- Jittery marker animations

## Profiling Workflow

### 1. CPU Profiling
```bash
# Start dev server with profiling
node js/server.js --profile
```

Use Chrome DevTools:
- **Performance tab**: Record 30-second timeline during map interaction
- **Memory tab**: Take heap snapshots before/after marker operations
- **Profiler**: Identify hot functions in GeoUtils.js and MapController.js

### 2. Memory Leak Detection
```javascript
// Add to MapController cleanup
function trackMarkers(markers) {
  console.trace('Marker created');
  // Check for orphaned event listeners
}

// In Chrome DevTools:
// 1. Go to Memory tab
// 2. Take Heap Snapshot
// 3. Filter by "Detached" or "Closed"
// 4. Compare snapshots to find leaks
```

### 3. Bundle Analysis
```bash
# Install webpack-bundle-analyzer
npm install --save-dev webpack-bundle-analyzer

# Run analysis
npx webpack-bundle-analyzer stats.json
```

Check for:
- Duplicate dependencies
- Large third-party libraries
- Unused code paths

## Optimization Checklist

### Map Performance
- [ ] Use marker clustering for >100 units
- [ ] Implement viewport culling (only render visible markers)
- [ ] Batch DOM updates with `requestAnimationFrame`
- [ ] Use CSS transforms instead of top/left positioning
- [ ] Set `zIndexOffset` for proper layer ordering

### Memory Management
- [ ] Remove event listeners on component destroy
- [ ] Clear timeouts/intervals in cleanup
- [ ] Use WeakMap for cache where appropriate
- [ ] Avoid capturing large objects in closures

### Battery Optimization
- [ ] Adaptive polling: increase interval when stationary
- [ ] Throttle GPS updates (min 1 second apart)
- [ ] Pause map updates when tab is inactive
- [ ] Use `will-change` CSS property sparingly

## Benchmark Scripts

### Marker Update Benchmark
```javascript
// scripts/benchmark-markers.js
async function benchmarkMarkerUpdates(count, fpsTarget = 60) {
  const start = performance.now();
  let frames = 0;
  let lastTime = start;
  
  function frame() {
    const now = performance.now();
    const delta = now - lastTime;
    lastTime = now;
    frames++;
    
    // Simulate marker updates
    for (let i = 0; i < count; i++) {
      updateMarkerPosition(i);
    }
    
    if (now - start < 5000) {
      requestAnimationFrame(frame);
    } else {
      const avgFps = (frames / 5000) * 1000;
      console.log(`Avg FPS: ${avgFps.toFixed(2)}`);
      console.log(`Target met: ${avgFps >= fpsTarget}`);
    }
  }
  
  requestAnimationFrame(frame);
}
```

### Memory Usage Monitor
```javascript
// scripts/measure-memory.js
function logMemory() {
  if (performance.memory) {
    console.log('JS Heap:', (performance.memory.usedJSHeapSize / 1024 / 1024).toFixed(2) + ' MB');
    console.log('JS Limit:', (performance.memory.jsHeapSizeLimit / 1024 / 1024).toFixed(2) + ' MB');
  }
}

// Call every 5 seconds during testing
setInterval(logMemory, 5000);
```

## Performance Targets
| Metric | Target | Measurement |
|--------|--------|-------------|
| Map FPS | ≥60fps | DevTools Performance tab |
| Initial Load | <3s | DevTools Network tab |
| Memory Peak | <150MB | DevTools Memory tab |
| Battery Drain | <5%/hour | Mobile testing |
| Bundle Size | <500KB gzipped | Bundle analyzer |

## References
- [Chrome DevTools Performance Guide](https://developer.chrome.com/docs/devtools/performance/)
- [Web Vitals](https://web.dev/vitals/)
- [Leaflet Performance Tips](https://leafletjs.com/examples/perf/)
