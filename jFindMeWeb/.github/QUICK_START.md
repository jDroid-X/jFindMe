# jButterfly - Quick Start Cheat Sheet

## 🚀 30-Second Launch

```powershell
# Terminal 1: Start Server
cd c:\Users\jiten\jAnitGravity\jFindMe\jFindMeWeb
.\jFireBase.bat

# Terminal 2: Open Browser (new tab)
Start http://127.0.0.1:3000
```

---

## 📱 Daily Operations Menu

### Primary Views (Click Sidebar Icons)

| Icon | Name | Purpose | Best For |
|------|------|---------|----------|
| 🗺️ | **Live Map Radar** | Real-time tracking | Main monitoring |
| 📊 | **Dashboard** | Fleet analytics | Morning briefing |
| 👥 | **Device User** | Manage devices | Adding/editing units |
| 🛡️ | **Geofencing** | Set perimeters | Security setup |
| 🦅 | **Database Architect** | Deep analytics | Weekly review |

### Quick Actions

| Action | How To |
|--------|--------|
| Focus device | Click marker or checkbox → "Focus" button |
| View history | Click device card → 🕐 History trail |
| Edit profile | Click device card → ✏️ Edit icon |
| Create geofence | Geofencing view → "Create New Zone" |
| Change settings | Settings gear icon (top-right) |
| Toggle theme | ☀️ Light/Dark button (top-right) |

---

## 🔍 Troubleshooting Quick Fixes

### Problem: Blank Map / No Devices
```
1. Check terminal for "[SUCCESS] Firebase Admin SDK Initialized"
2. Test: http://127.0.0.1:3000/api/discovery
3. If empty, add test device via Database Architect
```

### Problem: Auth Failed
```
1. Press Enter in password field (auto-login bypass)
2. Clear browser cache: Ctrl+Shift+Del
3. Check Master Key in js/core/Auth.js line 12
```

### Problem: Device Not Showing Location
```
1. Verify device has valid lat/lng in Firebase
2. Check timestamp is recent (< 15 minutes old)
3. Confirm isOnline = true in telemetry
```

### Problem: Geofence Not Triggering
```
1. Check zone is marked "Active" in SafeZoneModel
2. Verify device coordinates are within polygon
3. Look for errors in IN/OUT Alert Log (Pane 3)
```

---

## ⌨️ Keyboard Shortcuts (Planned)

| Shortcut | Action | Status |
|----------|--------|--------|
| `Ctrl+K` | Focus search box | ❌ TODO |
| `Alt+1` | Switch to Live Map | ❌ TODO |
| `Alt+2` | Switch to Dashboard | ❌ TODO |
| `Space` | Play/pause history | ❌ TODO |
| `Esc` | Close modal | ✅ Works |

---

## 📊 Understanding the Map

### Marker Colors by Status
- 🟢 **Green Dot** = Online & Moving (> 1.5 km/h)
- 🟡 **Yellow Dot** = Online but Idle
- ⚪ **Gray Dot** = Offline (> 15 min no update)
- 🔴 **Red Border** = Low Battery (< 20%)
- 🟣 **Purple Outline** = Mock GPS Detected

### Map Controls
| Control | Function |
|---------|----------|
| + / - buttons | Zoom in/out |
| Layer icon (top-left) | Change map type |
| Traffic toggle | Show live traffic |
| Coordinate display | Hover mouse over map |

---

## 🔧 Essential API Commands

```bash
# Test backend connectivity
curl http://127.0.0.1:3000/api/discovery

# Get all live devices
curl http://127.0.0.1:3000/api/devices-live

# Get all safe zones
curl http://127.0.0.1:3000/api/safe-zones

# Add test device (POST)
curl -X POST http://127.0.0.1:3000/api/devices \
  -H "Content-Type: application/json" \
  -d '{"agentId":"TEST001","profile":{"name":"Test Device"}}'
```

---

## 📂 File Structure Reference

```
jFindMeWeb/
├── index.html              # Main entry point
├── js/
│   ├── app.js              # Main orchestrator
│   ├── controllers/
│   │   ├── DeviceController.js    # Business logic
│   │   ├── MapController.js       # Map coordination
│   │   └── SafeZoneController.js  # Geofence logic
│   ├── models/
│   │   ├── DeviceModel.js         # Device entity
│   │   └── SafeZoneModel.js       # Zone entity
│   ├── views/
│   │   ├── GoogleMapView.js       # Map rendering
│   │   └── DashboardView.js       # Analytics UI
│   └── services/
│       └── FirebaseService.js     # API bridge
├── css/
│   └── variables.css        # Theme colors
└── Avatar/                  # Profile pictures
```

---

## 🎯 Quick Test Sequence

### Validate Full System Working
```
1. ✅ Backend running? Check terminal
2. ✅ Frontend loads? No console errors
3. ✅ Map shows? At least one marker
4. ✅ Device list? Sidebar has entries
5. ✅ Settings save? Change radius, reload page
6. ✅ Geofence works? Create zone, move device outside
```

---

## 📞 Emergency Contacts

| Issue | Contact |
|-------|---------|
| Firebase connection | Check `service-account.json` validity |
| Backend crashes | Restart with `.\jFireBase.bat` |
| Data corruption | Use scratch/cleanup_lookups.js |
| Map rendering bug | Check Leaflet version compatibility |

---

## 💡 Pro Tips

1. **Use Database Architect** to inspect raw Firebase data before debugging UI issues
2. **Terminal Logs** at bottom show all operations - filter by type (success/error/warning)
3. **Clear Terminal**: Click "CLEAR" button when logs get cluttered
4. **Export Settings**: Copy from localStorage for backup
5. **Multi-device testing**: Open same URL in incognito window for second operator view

---

*Last Updated: 2026-09-11*  
*Version: jFindMe Matrix V3*
