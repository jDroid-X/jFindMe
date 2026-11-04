---
applyTo: "**/js/views/**MapView*.js", "**/js/controllers/MapController.js", "**/js/services/GoogleMapsLoader.js"
description: "Guidelines for map-related JavaScript files in the jButterfly telemetry system"
---

# Map View File Instructions

Apply to all Leaflet map implementations and controller files.

## Code Style

### Marker Management
```javascript
// GOOD: Track and clean up markers
class MapManager {
  constructor() {
    this.markers = new Map();
  }
  
  addMarker(deviceId, lat, lng, options = {}) {
    const marker = L.marker([lat, lng], options);
    this.markers.set(deviceId, marker);
    return marker;
  }
  
  removeMarker(deviceId) {
    const marker = this.markers.get(deviceId);
    if (marker) {
      marker.remove();
      this.markers.delete(deviceId);
    }
  }
}

// BAD: No cleanup, potential memory leaks
function addMarker(lat, lng) {
  L.marker([lat, lng]).addTo(map); // No reference stored
}
```

### Event Listener Management
```javascript
// GOOD: Store and remove listeners
class MapController {
  init() {
    this._clickHandler = this._onClick.bind(this);
    this.map.on('click', this._clickHandler);
  }
  
  destroy() {
    this.map.off('click', this._clickHandler);
  }
}

// BAD: Leaked listeners
map.on('click', function(e) { ... }); // Anonymous, can't remove
```

## Coordinate Handling
Always use GeoUtils.js for coordinate operations:
```javascript
import { haversineDistance, calculateBearing } from '../../core/GeoUtils.js';

// Calculate distance
const distance = haversineDistance(lat1, lng1, lat2, lng2);

// Calculate bearing
const bearing = calculateBearing(lat1, lng1, lat2, lng2);
```

## Animation Best Practices
```javascript
// Use requestAnimationFrame for smooth updates
function smoothUpdate(marker, targetLat, targetLng) {
  const start = Date.now();
  const startPos = marker.getLatLng();
  
  function animate() {
    const elapsed = Date.now() - start;
    const progress = Math.min(elapsed / 1000, 1);
    
    // Easing function
    const eased = progress < 0.5 
      ? 2 * progress * progress 
      : 1 - Math.pow(-2 * progress + 2, 2) / 2;
    
    const lat = startPos.lat + (targetLat - startPos.lat) * eased;
    const lng = startPos.lng + (targetLng - startPos.lng) * eased;
    
    marker.setLatLng([lat, lng]);
    
    if (progress < 1) {
      requestAnimationFrame(animate);
    }
  }
  
  requestAnimationFrame(animate);
}
```

## Error Handling
```javascript
// Always validate coordinates before using
function updateMarkerPosition(markerId, lat, lng) {
  if (typeof lat !== 'number' || typeof lng !== 'number') {
    console.error(`Invalid coordinates for ${markerId}: ${lat}, ${lng}`);
    return;
  }
  
  if (!isFinite(lat) || !isFinite(lng)) {
    console.error(`Non-finite coordinates for ${markerId}`);
    return;
  }
  
  // Proceed with update...
}
```

## Testing Checklist
- [ ] Test with 0 markers
- [ ] Test with 1000+ markers
- [ ] Test rapid coordinate updates
- [ ] Test network disconnection/reconnection
- [ ] Test browser tab visibility changes
