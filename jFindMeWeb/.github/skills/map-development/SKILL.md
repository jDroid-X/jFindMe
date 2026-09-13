---
name: map-development
description: "Develop and optimize Leaflet maps for GPS telemetry. Use for marker clustering, geofence drawing, smooth animations, coordinate math, and map performance tuning."
argument-hint: "Map development task..."
user-invocable: true
---

# Map Development Skill

## When to Use
- Implementing new map features (markers, polygons, polylines)
- Optimizing map performance for large fleets
- Debugging coordinate projection issues
- Adding geofence drawing/intersection logic
- Implementing smooth marker animations

## Procedures

### 1. Marker Management
```javascript
// Use cluster manager for 100+ markers
const clusterGroup = L.markerClusterGroup({
    maxClusterRadius: 50,
    spiderfyOnMaxZoom: true
});

// Single marker with custom icon
const marker = L.marker([lat, lng], {
    icon: customIcon,
    title: deviceName
}).bindPopup(deviceInfo);
```

### 2. Geofence Drawing
```javascript
// Create polygon from coordinates
const polygon = L.polygon(coordinates, {
    color: '#3b82f6',
    fillColor: '#3b82f6',
    fillOpacity: 0.2,
    weight: 2
});

// Point-in-polygon check
function isPointInPolygon(latlng, polygonCoords) {
    // Ray casting algorithm
}
```

### 3. Smooth Animation
```javascript
// Use requestAnimationFrame for interpolation
function animateMarker(marker, targetLat, targetLng, duration = 1000) {
    const start = Date.now();
    const startPos = marker.getLatLng();
    
    function update() {
        const elapsed = Date.now() - start;
        const progress = Math.min(elapsed / duration, 1);
        const eased = easeInOutCubic(progress);
        
        const lat = startPos.lat + (targetLat - startPos.lat) * eased;
        const lng = startPos.lng + (targetLng - startPos.lng) * eased;
        
        marker.setLatLng([lat, lng]);
        
        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }
    update();
}
```

### 4. Coordinate Utilities
```javascript
// Haversine distance (meters)
function haversineDistance(lat1, lon1, lat2, lon2) {
    const R = 6371e3; // Earth radius in meters
    const φ1 = lat1 * Math.PI / 180;
    const φ2 = lat2 * Math.PI / 180;
    const Δφ = (lat2 - lat1) * Math.PI / 180;
    const Δλ = (lon2 - lon1) * Math.PI / 180;

    const a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
              Math.cos(φ1) * Math.cos(φ2) *
              Math.sin(Δλ/2) * Math.sin(Δλ/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

    return R * c;
}

// Bearing calculation (degrees)
function calculateBearing(lat1, lng1, lat2, lng2) {
    const φ1 = lat1 * Math.PI / 180;
    const φ2 = lat2 * Math.PI / 180;
    const Δλ = (lon2 - lon1) * Math.PI / 180;

    const y = Math.sin(Δλ) * Math.cos(φ2);
    const x = Math.cos(φ1) * Math.sin(φ2) -
              Math.sin(φ1) * Math.cos(φ2) * Math.cos(Δλ);

    const bearing = Math.atan2(y, x) * 180 / Math.PI;
    return (bearing + 360) % 360;
}
```

## Performance Checklist
- [ ] Use L.marker instead of custom div icons when possible
- [ ] Enable `zIndexOffset` for proper layer ordering
- [ ] Batch DOM updates with `L.DomUtil.startTransform()`
- [ ] Throttle marker updates during fast movement
- [ ] Use Web Workers for heavy coordinate calculations

## References
- [Leaflet Documentation](https://leafletjs.com/reference.html)
- [Leaflet.markercluster](https://github.com/Leaflet/Leaflet.markercluster)
- [GeoUtils.js](../../js/core/GeoUtils.js)
