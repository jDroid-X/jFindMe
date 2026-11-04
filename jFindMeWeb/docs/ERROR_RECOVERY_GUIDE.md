# jButterfly - Error Recovery & Troubleshooting Guide

## 🚨 Error Codes Reference

| Code | Message | Cause | Recovery Action |
|------|---------|-------|-----------------|
| AUTH_001 | Invalid Credentials | Wrong master key | Verify key in Auth.js line 12 |
| AUTH_002 | Session Expired | 8-hour timeout | Auto-logout, re-login required |
| NET_001 | Network Error | No internet/Firebase down | Check backend server, retry |
| NET_002 | Timeout Error | Slow response (>30s) | Check network, retry |
| DB_001 | Query Failed | Invalid Firebase path | Verify collection exists |
| DB_002 | Permission Denied | No access to resource | Check security rules |
| MAP_001 | Render Failed | Leaflet/Google Maps error | Check browser console |
| MAP_002 | Invalid Coordinates | Lat/Lng out of range | Validate device data |

---

## 🔧 Common Issues & Fixes

### Issue 1: Blank Map After Login
**Symptoms:**
- Login successful but no devices visible
- Map shows gray tiles only

**Diagnosis:**
```javascript
// Check console (F12) for:
console.log(window.MatrixApp?.cachedDevices);
console.log(fetch('/api/devices-live'));
```

**Fix:**
1. Verify backend running: `http://127.0.0.1:3000/api/discovery`
2. Check Firebase has device data:
   ```bash
   curl http://127.0.0.1:3000/api/devices-live
   ```
3. If empty, add test device via Database Architect view

---

### Issue 2: Settings Not Persisting
**Symptoms:**
- Refresh page resets reference location
- Radius returns to 10km default

**Diagnosis:**
```javascript
console.log(localStorage.getItem('jfindme_matrix_settings'));
```

**Fix:**
1. Check console for save errors
2. Verify Firebase `/config/operator/settings` path exists
3. If server sync fails, local storage should still work
4. Clear cache and retry

---

### Issue 3: Geofence Alerts Not Triggering
**Symptoms:**
- Device crosses zone boundary
- No notification appears

**Diagnosis:**
1. Check "IN/OUT Alert Log" pane (Pane 3)
2. Verify device coordinates vs zone boundary
3. Check zone is marked "Active"

**Fix:**
```javascript
// Test point-in-polygon
import { GeoUtils } from './core/GeoUtils.js';
const inside = GeoUtils.isPointInPolygon([lat, lng], polygonCoords);
console.log('Inside zone:', inside);
```

---

### Issue 4: Marker Flickering on Map
**Symptoms:**
- Markers blink or jump
- Performance drops below 30fps

**Fix:**
1. Enable marker clustering for >100 devices
2. Throttle updates to 5-second intervals
3. Use requestAnimationFrame for smooth movement

```javascript
// In MapController, add throttle
this.deviceUpdateTimer = setInterval(() => {
    this.mapView.renderDeviceMarkers(this.devices);
}, 5000);
```

---

### Issue 5: Offline Mode Not Working
**Symptoms:**
- App breaks when network disconnects
- No queued updates visible

**Fix:**
1. Check IndexedDB initialized:
   ```javascript
   console.log(indexedDB.databases());
   ```
2. Verify offline queue has pending records
3. Test reconnection after restoring network

---

## 📱 Mobile-Specific Issues

### Issue 6: Sidebar Overflows on Small Screens
**Symptoms:**
- Menu items cut off on phones
- Touch targets too small

**Fix:**
1. Mobile menu button should appear (< 576px)
2. Check CSS media queries in `css/mobile-responsive.css`
3. Ensure touch targets are ≥ 44px

---

### Issue 7: Map Touch Events Broken
**Symptoms:**
- Cannot tap markers on mobile
- Dragging map doesn't work

**Fix:**
```css
/* Add to mobile-responsive.css */
.leaflet-tap-target {
    touch-action: manipulation;
}

/* Increase touch target size */
.leaflet-marker-icon {
    min-width: 44px;
    min-height: 44px;
}
```

---

## 🔐 Security Issues

### Issue 8: Hardcoded Credentials Detected
**Location:** `js/core/Auth.js:12`

**Fix Applied:**
```javascript
// BEFORE (INSECURE):
this.MASTER_KEY_ID = 'Jeet@123Meet';

// AFTER (IMPROVED):
this.MASTER_KEY_ID = window.ENV?.MASTER_KEY_ID 
    || localStorage.getItem('matrix_master_key') 
    || 'CHANGE_ME_DEFAULT_KEY';
```

**Action Required:**
1. Change `CHANGE_ME_DEFAULT_KEY` before production
2. Use environment variables in production
3. Never commit actual credentials to git

---

### Issue 9: CORS Errors in Production
**Symptoms:**
- Frontend loads but API calls fail
- Console shows CORS policy errors

**Fix:**
```javascript
// In server.js, restrict origins:
app.use(cors({
    origin: ['https://yourdomain.com', 'https://admin.yourdomain.com'],
    credentials: true
}));
```

---

## 🗄️ Database Issues

### Issue 10: Duplicate Devices Appearing
**Symptoms:**
- Same device shows multiple times
- Inconsistent data across views

**Fix:**
1. Check deduplication logic in `DeviceController.fetchLiveDevices()`
2. Ensure agentId uniqueness in database
3. Run cleanup script:
   ```bash
   node scratch/cleanup_lookups.js
   ```

---

### Issue 11: Timestamp Format Mismatch
**Symptoms:**
- Some devices show "Unknown" last seen
- Time sorting incorrect

**Fix:**
Standardize all timestamps to epoch milliseconds:
```javascript
// GeoUtils.parseTimestampToEpoch() handles:
// - Epoch ms: 1700000000000
// - ISO string: "2024-01-15T10:30:00Z"
// - Date object
```

---

## 🎯 Quick Diagnostic Commands

### Browser Console Checks
```javascript
// 1. Check authentication status
console.log('Auth:', Auth.isAuthenticated());

// 2. Check cached devices
console.log('Devices:', window.MatrixApp?.cachedDevices?.length);

// 3. Check settings
console.log('Settings:', localStorage.getItem('jfindme_matrix_settings'));

// 4. Check offline queue
OfflineQueue.getStats().then(console.log);

// 5. Check error logs
console.log(ErrorRecovery.getStats());

// 6. Test API connection
fetch('/api/discovery').then(r => r.json()).then(console.log);
```

### Terminal Checks
```bash
# Test backend health
curl http://127.0.0.1:3000/api/discovery

# Test devices endpoint
curl http://127.0.0.1:3000/api/devices-live

# Test safe zones
curl http://127.0.0.1:3000/api/safe-zones

# Check server logs
tail -f logs/server.log
```

---

## 🔄 Recovery Procedures

### Full System Reset
```javascript
// WARNING: Clears all local data
localStorage.clear();
indexedDB.deleteDatabase('jfindme_offline_queue');
location.reload();
```

### Database Repair
```bash
# Backup current data
firebase database:get / > backup_$(date +%Y%m%d).json

# Purge corrupted data (USE WITH CAUTION)
node scratch/purge_all_database.js

# Restore from backup
firebase database:set / < backup.json
```

### Server Restart Procedure
```bash
# Stop server (Ctrl+C)
# Clear cache
rm -rf node_modules/.cache

# Restart
npm install
node js/server.js
```

---

## 📊 Monitoring Dashboard

### Key Metrics to Watch
| Metric | Normal Range | Alert Threshold |
|--------|-------------|-----------------|
| Device Count | >0 | =0 for 5 min |
| Update Frequency | 3-10s | >30s |
| Error Rate | <1% | >5% |
| Offline Queue | 0-10 | >100 |
| Memory Usage | <100MB | >200MB |

### Log Analysis
```javascript
// Export error logs for analysis
const logs = ErrorRecovery.getStats();
DataExport.exportAlertsToCSV(
    logs.recent.map(e => ({
        timestamp: e.timestamp,
        type: e.type,
        context: e.context
    }))
);
```

---

## 🆘 Emergency Contacts

| Issue Type | Contact | Response Time |
|------------|---------|---------------|
| Critical Down | DevOps | Immediate |
| Data Loss | DBA | 15 minutes |
| Security Breach | Security Team | 5 minutes |
| Performance Degradation | Dev Team | 30 minutes |

---

*Document Version: 1.0*  
*Last Updated: 2026-09-11*
