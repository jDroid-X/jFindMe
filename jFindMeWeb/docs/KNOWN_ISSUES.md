# jButterfly - Known Issues & Gap Analysis

## 🔴 CRITICAL ISSUES

### 1. Authentication Security Vulnerability
**File:** `js/core/Auth.js:12`
```javascript
this.MASTER_KEY_ID = 'Jeet@123Meet';  // HARDCODED PASSWORD
```
**Impact:** Anyone with this key has full admin access
**Fix Required:** Implement Firebase Auth with email/password or SSO
**Priority:** P0 - Fix Immediately

### 2. No Device Registration Flow
**Issue:** Devices must be manually added to Firebase
**User Experience:** Confusing for new operators
**Fix Required:** Create "Add Device" wizard with:
- QR code scanning (from companion app)
- Manual ID entry
- Invitation flow

### 3. Settings Not Persisted Server-Side
**Issue:** All settings stored in localStorage only
**Risk:** Cache clear = reset configuration
**Fix Required:** Sync settings to Firebase `/config/operator` node

---

## 🟡 HIGH PRIORITY GAPS

### 4. No Mobile Responsive Design
**Affected Files:**
- `css/layout.css`
- `js/views/jButterflyHubView.js`

**Issues:**
- Selection matrix overflows on < 375px screens
- Touch targets too small (8px minimum recommended)
- Sidebar doesn't collapse properly

**Fix Required:**
```css
@media (max-width: 375px) {
  .matrix-selection-panel { width: 100%; position: fixed; }
  .leaflet-device-card { width: 90%; }
}
```

### 5. No Offline Capability
**Current Behavior:** App breaks when Firebase disconnects
**Fix Required:** Implement IndexedDB cache with:
- Background sync queue
- Stale-while-revalidate pattern
- Visual offline indicator (currently shows SYNC_ACTIVE always)

### 6. Error Recovery Documentation Missing
**Missing:** Error code reference guide
**Required Pages:**
- 401 Unauthorized flows
- 403 Permission denied handling
- 500 Server error recovery
- Network timeout graceful degradation

---

## 🟢 MEDIUM PRIORITY IMPROVEMENTS

### 7. Keyboard Shortcuts Not Implemented
**Benefit:** Faster operations for power users
**Proposed Shortcuts:**
| Key | Action |
|-----|--------|
| `Ctrl+K` | Focus search |
| `Alt+1-5` | Switch views |
| `Space` | Play/pause history |
| `Escape` | Close modal |
| `Ctrl+S` | Save settings |

### 8. No Data Export Functionality
**Use Cases:**
- Monthly reporting
- Backup before migration
- Audit compliance

**Required:**
- Export devices to CSV/JSON
- Export geofence boundaries
- Export alert history

### 9. Limited Theme Customization
**Current:** Only dark/light toggle
**Enhanced:**
- Custom color accents
- Map style presets (Satellite, Terrain, Hybrid)
- Typography scaling for accessibility

---

## 🔵 LOW PRIORITY / NICE TO HAVE

### 10. No Multi-User Role System
**Current:** Single-operator model
**Future:**
- Admin (full access)
- Supervisor (read-only + alerts)
- Viewer (map only)

### 11. No Push Notification Support
**Missing:** Browser notification on geofence breach
**Required:** Service Worker + Firebase Cloud Messaging

### 12. No Device Health Dashboard
**Currently Missing:**
- Last seen statistics
- Battery trend analysis
- Signal strength history
- Mock GPS detection rate

---

## 📊 DATA INTEGRITY ISSUES

### 13. Timestamp Format Inconsistency
**Problem:** Some devices send epoch ms, some ISO strings
**Fix Required:** Standardize in DeviceModel constructor
```javascript
// GeoUtils.parseTimestampToEpoch() should handle all formats
```

### 14. Duplicate Device Handling
**Issue:** Same agentId can exist multiple times
**Fix Required:** Deduplication logic in DeviceController.fetchLiveDevices()

### 15. No Data Retention Policy
**Risk:** Database grows unbounded
**Fix Required:** 
- Automatic cleanup of history > 30 days
- Archive old data to cold storage
- Configurable retention periods

---

## 🐛 BUG REPORTS

### 16. Map Centering Race Condition
**Symptom:** Map centers on wrong location after quick navigation
**Trigger:** Click dashboard → Live Map within 500ms
**Fix:** Debounce window.showLiveMap() calls

### 17. Marker Flickering on Fast Updates
**Symptom:** Markers blink when GPS updates > 1/sec
**Fix:** Implement requestAnimationFrame throttling

### 18. Settings Modal Closes on Enter
**Bug:** Pressing Enter in input triggers save unexpectedly
**Fix:** Add keydown handler to prevent default on form submit

---

## 📝 DOCUMENTATION GAPS

### 19. No API Reference
**Missing:** `/api/*` endpoint documentation
**Required:**
- Request/response schemas
- Error codes
- Rate limits

### 20. No Deployment Guide
**Missing:** Production deployment instructions
**Required:**
- SSL/TLS setup
- Reverse proxy config (nginx)
- CI/CD pipeline
- Monitoring setup

---

## 🔒 SECURITY CONCERNS

### 21. CORS Misconfiguration Risk
**Current:** `app.use(cors())` allows all origins
**Fix:** Restrict to known domains
```javascript
app.use(cors({ origin: ['https://yourdomain.com'] }));
```

### 22. No Rate Limiting
**Risk:** API abuse / DoS
**Fix:** Implement express-rate-limit

### 23. Secrets in Source Code
**Files:**
- `service-account.json` (should be env variable)
- `google-services.json` (should be downloaded per-env)
- `.gitignore` should exclude these

---

## ✅ RECOMMENDED IMPLEMENTATION ORDER

### Week 1: Critical Fixes
1. [ ] Replace hardcoded auth with Firebase Auth
2. [ ] Add settings persistence to Firebase
3. [ ] Fix mobile responsiveness

### Week 2: Core Features
4. [ ] Device registration wizard
5. [ ] Offline capability with IndexedDB
6. [ ] Error recovery documentation

### Week 3: Enhanced UX
7. [ ] Keyboard shortcuts
8. [ ] Data export functionality
9. [ ] Theme customization

### Week 4: Scale & Security
10. [ ] Multi-user RBAC
11. [ ] Push notifications
12. [ ] Security hardening (CORS, rate limiting)

---

## 📞 SUPPORT RESOURCES

### Internal Documentation
- [AGENTS_FRAMEWORK_KIT.md](../AGENTS_FRAMEWORK_KIT.md) - Architecture overview
- [BUSINESS_ANALYTICS_TREE_ARCHITECTURE.md](../BUSINESS_ANALYTICS_TREE_ARCHITECTURE.md) - Business logic
- [.github/WORKFLOW_GUIDE.md](./WORKFLOW_GUIDE.md) - Operational procedures

### Debug Tools
```javascript
// Global debug helpers
window.debug = {
  devices: () => console.table(window.MatrixApp?.cachedDevices),
  settings: () => console.log(localStorage.getItem('jfindme_matrix_settings')),
  sync: () => console.log('Sync status:', document.getElementById('sync-indicator'))
};
```

---

*Document Version: 1.0*  
*Last Reviewed: 2026-09-11*
