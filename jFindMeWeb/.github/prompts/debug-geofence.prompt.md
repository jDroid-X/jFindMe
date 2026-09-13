---
name: debug-geofence
argument-hint: "<device-id> <zone-type>"
description: "Debug geofence entry/exit issues for a specific device and zone type. Validates polygon intersection, border cases, and notification triggers."
---

# Debug Geofence Prompt

Use this prompt when geofence notifications are not triggering correctly or boundary detection is inconsistent.

## Input Parameters
- `deviceId`: The device ID to test
- `zoneType`: Type of safe zone (home, school, work, custom)

## Diagnostic Steps

### 1. Verify Device Location
```javascript
// Check current device coordinates
const device = await db.collection('devices').doc(deviceId).get();
console.log('Device Location:', device.data().location);
```

### 2. Test Point-in-Polygon
```javascript
// Import GeoUtils
import { isPointInPolygon } from '../core/GeoUtils.js';

const zone = await db.collection('safeZones').where('type', '==', zoneType).first();
const point = [device.location.lat, device.location.lng];
const polygon = zone.geometry.coordinates[0];

const inside = isPointInPolygon(point, polygon);
console.log('Inside zone:', inside);
```

### 3. Check Notification State
```javascript
// Verify notification flags
console.log('Zone Notifications:', {
  entry: zone.notifications?.onEntry,
  exit: zone.notifications?.onExit
});

// Check recent events
const recentEvents = await db.collection('events')
  .where('deviceId', '==', deviceId)
  .where('zoneId', '==', zone.id)
  .orderBy('timestamp', 'desc')
  .limit(10)
  .get();
```

### 4. Simulate Border Crossing
```javascript
// Test with coordinates near boundary
const boundaryTests = [
  { lat: 28.6139, lng: 77.2090, expected: 'inside' },
  { lat: 28.6145, lng: 77.2085, expected: 'outside' },
  { lat: 28.6142, lng: 77.2088, expected: 'boundary' }
];

boundaryTests.forEach(test => {
  const result = isPointInPolygon([test.lat, test.lng], polygon);
  console.log(`${test.expected}: ${result ? 'INSIDE' : 'OUTSIDE'}`);
});
```

## Common Issues & Fixes

| Issue | Cause | Fix |
|-------|-------|-----|
| Notification fires twice | Overshoot detection | Add debouncing (500ms) |
| Never triggers on entry | Polygon winding order | Ensure counter-clockwise |
| Flickering at boundary | Coordinate precision | Add 1m buffer zone |
| Memory leak | Uncleared timers | Cleanup in destroy() |

## Output Format
Return a structured report:
```markdown
## Geofence Debug Report
- Device: {deviceId}
- Zone: {zoneType}
- Status: PASS/FAIL
- Issues Found: [...]
- Recommendations: [...]
```
