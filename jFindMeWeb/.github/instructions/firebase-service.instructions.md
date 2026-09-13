---
applyTo: "**/js/services/FirebaseService.js", "**/js/models/*.js"
description: "Best practices for Firebase integration and data model implementation in jButterfly"
---

# Firebase Service File Instructions

Apply to all Firebase service and model files.

## Connection Management

### Singleton Pattern
```javascript
// GOOD: Single Firebase instance
class FirebaseService {
  static _instance = null;
  static getInstance() {
    if (!FirebaseService._instance) {
      FirebaseService._instance = new FirebaseService();
    }
    return FirebaseService._instance;
  }
  
  constructor() {
    if (FirebaseService._instance) {
      return FirebaseService._instance;
    }
    // Initialize Firebase...
  }
}
```

### Offline Support
```javascript
// Implement offline queue
class OfflineQueue {
  constructor(dbRef) {
    this.db = dbRef;
    this.queue = [];
    this.processing = false;
    
    this._setupListeners();
  }
  
  _setupListeners() {
    window.addEventListener('online', () => this.flush());
    window.addEventListener('offline', () => {});
  }
  
  async push(operation) {
    if (navigator.onLine) {
      await this._execute(operation);
    } else {
      this.queue.push(operation);
    }
  }
  
  async flush() {
    if (this.processing || this.queue.length === 0) return;
    
    this.processing = true;
    while (this.queue.length > 0) {
      const op = this.queue.shift();
      try {
        await this._execute(op);
      } catch (err) {
        this.queue.unshift(op); // Re-queue on failure
        break;
      }
    }
    this.processing = false;
  }
}
```

## Query Optimization

### Compound Indexes
```javascript
// Define in firebase.json
{
  "database": {
    "indexes": [
      {
        "collectionGroup": "locations",
        "queryScope": "COLLECTION_GROUP",
        "fields": [
          { "fieldPath": "deviceId", "order": "ASCENDING" },
          { "fieldPath": "timestamp", "order": "DESCENDING" }
        ]
      }
    ]
  }
}
```

### Pagination
```javascript
// Implement cursor-based pagination
async function getHistory(deviceId, lastTimestamp = null, limit = 50) {
  let query = db.collection('locations')
    .where('deviceId', '==', deviceId)
    .orderBy('timestamp', 'desc')
    .limit(limit);
  
  if (lastTimestamp) {
    query = query.startAfter(lastTimestamp);
  }
  
  return query.get();
}
```

## Security Patterns

### Owner Verification
```javascript
// Validate ownership before writes
async function updateDevice(deviceId, updates, userId) {
  const device = await db.collection('devices').doc(deviceId).get();
  
  if (device.data().ownerId !== userId) {
    throw new Error('Unauthorized device access');
  }
  
  return db.collection('devices').doc(deviceId).update(updates);
}
```

### Data Sanitization
```javascript
// Sanitize incoming data
function sanitizeLocationData(rawData) {
  return {
    lat: Number(rawData.lat)?.toFixed(6),
    lng: Number(rawData.lng)?.toFixed(6),
    accuracy: Number(rawData.accuracy),
    speed: Number(rawData.speed),
    heading: Number(rawData.heading),
    timestamp: Date.now(),
    // Strip sensitive fields
  };
}
```

## Testing Requirements
- [ ] Test online/offline transitions
- [ ] Verify security rule enforcement
- [ ] Check query performance with large datasets
- [ ] Validate offline queue persistence
- [ ] Test concurrent update handling
