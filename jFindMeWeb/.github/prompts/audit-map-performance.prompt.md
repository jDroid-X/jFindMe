---
name: audit-map-performance
argument-hint: "<marker-count>"
description: "Audit map rendering performance for N concurrent markers. Measures FPS, memory usage, and identifies bottlenecks in the Leaflet implementation."
---

# Map Performance Audit Prompt

Use this prompt when experiencing lag, high CPU, or memory issues with the map view.

## Input Parameter
- `markerCount`: Number of concurrent markers to test with

## Audit Procedure

### 1. Baseline Measurement
```javascript
// Open browser console and run:
console.group('Map Performance Audit');
console.log('Marker Count:', markerCount);

// Measure initial load time
const startTime = performance.now();
initMap();
const loadTime = performance.now() - startTime;
console.log(`Initial Load: ${loadTime.toFixed(2)}ms`);
```

### 2. FPS Monitoring
```javascript
// Real-time FPS counter
let frameCount = 0;
let lastTime = performance.now();

function measureFPS() {
  const now = performance.now();
  frameCount++;
  
  if (now - lastTime >= 1000) {
    const fps = (frameCount * 1000) / (now - lastTime);
    console.log(`FPS: ${fps.toFixed(2)}`);
    frameCount = 0;
    lastTime = now;
  }
  
  requestAnimationFrame(measureFPS);
}
measureFPS();
```

### 3. Memory Snapshot
```javascript
// Take heap snapshot
console.profile('Memory Profile');
// Perform map interactions (pan, zoom, add markers)
console.profileEnd('Memory Profile');

// Check for leaks
const before = performance.memory?.usedJSHeapSize || 0;
// ... trigger GC if possible ...
const after = performance.memory?.usedJSHeapSize || 0;
console.log(`Memory Delta: ${(after - before) / 1024 / 1024.toFixed(2)} MB`);
```

### 4. Marker Update Benchmark
```javascript
// Simulate 1000 location updates
const updates = Array.from({length: markerCount}, (_, i) => ({
  id: `device-${i}`,
  lat: 28.61 + Math.random() * 0.01,
  lng: 77.20 + Math.random() * 0.01
}));

const updateStart = performance.now();
updates.forEach(update => {
  updateMarkerPosition(update.id, update.lat, update.lng);
});
const updateEnd = performance.now();
console.log(`Batch Update Time: ${updateEnd - updateStart}ms`);
```

## Performance Thresholds

| Metric | Warning | Critical |
|--------|---------|----------|
| FPS | <50 | <30 |
| Load Time | >5s | >10s |
| Memory Growth | >50MB/session | >100MB/session |
| Update Latency | >200ms | >500ms |

## Remediation Steps

### If FPS Low:
1. Enable marker clustering
2. Implement viewport culling
3. Reduce marker icon complexity

### If Memory High:
1. Clear unused marker references
2. Remove event listeners
3. Check for closure captures

### If Update Latency High:
1. Batch DOM updates
2. Use requestAnimationFrame
3. Implement throttling

## Output Format
Return audit report:
```markdown
## Map Performance Audit
- Markers Tested: {markerCount}
- Avg FPS: {value}
- Load Time: {value}ms
- Memory Usage: {value}MB
- Bottlenecks: [...]
- Recommendations: [...]
```
