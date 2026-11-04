---
name: firebase-integration
description: "Design Firebase schemas, write security rules, implement real-time sync, and handle offline data for GPS telemetry. Use for RTDB/Firestore integration tasks."
argument-hint: "Firebase task..."
user-invocable: true
---

# Firebase Integration Skill

## When to Use
- Designing database schemas for devices/safe zones
- Writing Firebase Security Rules
- Implementing real-time location sync
- Setting up offline queue with retry logic
- Optimizing queries for fleet data

## Data Schema Patterns

### Devices Collection
```json
{
  "devices": {
    "device-id-123": {
      "name": "Alpha Unit",
      "ownerId": "user-456",
      "status": "online",
      "lastUpdate": 1700000000000,
      "location": {
        "lat": 28.6139,
        "lng": 77.2090,
        "accuracy": 5.2,
        "speed": 45.5,
        "heading": 142,
        "timestamp": 1700000000000
      },
      "metadata": {
        "battery": 89,
        "charging": false,
        "mockGps": false,
        "signalStrength": "strong"
      }
    }
  }
}
```

### Safe Zones Collection
```json
{
  "safeZones": {
    "zone-id-789": {
      "name": "Home",
      "type": "home",
      "ownerId": "user-456",
      "geometry": {
        "type": "Polygon",
        "coordinates": [[...]]
      },
      "assignedDevices": ["device-123", "device-456"],
      "notifications": {
        "onEntry": true,
        "onExit": true
      }
    }
  }
}
```

## Security Rules Template

### Basic Device Access
```json
{
  "rules": {
    "devices": {
      "$deviceId": {
        ".read": "auth != null && root.child('users').child(auth.uid).child('devices').hasChild($deviceId)",
        ".write": "auth != null && root.child('users').child(auth.uid).child('devices').hasChild($deviceId)",
        ".validate": "newData.hasChildren(['name', 'ownerId', 'status'])"
      }
    }
  }
}
```

### Safe Zone Rules
```json
{
  "safeZones": {
    "$zoneId": {
      ".read": "auth != null && $zoneId.matches(/^[a-zA-Z0-9_-]+$/)",
      ".write": "auth != null && newData.child('ownerId').val() == auth.uid",
      "geometry": {
        ".validate": "newData.isNumber() || newData.hasChildren(['type', 'coordinates'])"
      }
    }
  }
}
```

## Offline Queue Pattern
```javascript
class OfflineQueue {
  constructor(firebaseRef) {
    this.ref = firebaseRef;
    this.queue = [];
    this.isOnline = navigator.onLine;
    
    window.addEventListener('online', () => this.flush());
    window.addEventListener('offline', () => {});
  }
  
  async push(data) {
    if (this.isOnline) {
      await this.ref.push(data);
    } else {
      this.queue.push({ data, timestamp: Date.now() });
    }
  }
  
  async flush() {
    while (this.queue.length > 0) {
      const item = this.queue.shift();
      try {
        await this.ref.push(item.data);
      } catch (err) {
        this.queue.unshift(item); // Re-queue on failure
        break;
      }
    }
  }
}
```

## Query Optimization
- **Indexes**: Define composite indexes in `firebase.json`
- **Pagination**: Use `.limitToLast()` for history trails
- **Real-time**: Keep listener scope minimal (single device, not all devices)
- **Caching**: Use IndexedDB as secondary cache

## References
- [Firebase Security Rules](https://firebase.google.com/docs/database/security)
- [Realtime Database Structure](https://firebase.google.com/docs/database/web/structure-data)
- [Offline Capabilities](https://firebase.google.com/docs/database/web/offline-capabilities)
