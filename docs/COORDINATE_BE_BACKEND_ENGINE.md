# ⚙️ Coordinate BE: Backend Logic & Operating Engine Layer (Functional & Technical)
## jFindMe / jButterfly Enterprise Ecosystem & jFindMeWeb Subsystem
> **Enterprise Standard v5.0** | **Codename:** `jDroid-X` | **Operator:** `JEET`  
> **Repository Target:** `C:\Users\jiten\jAnitGravity\jFindMe\jFindMeWeb`  
> **Coordinate:** `BE` in `ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB`  
> **Methodology:** Dual-Server Node.js Microservices & Android Foreground Engine Integration

---

## 1. Dual-Server Node.js Architecture & Process Topology

The Web Mission Control backend operates as a decoupled dual-server Node.js microservice architecture to isolate static asset delivery from stateful real-time data streaming:

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                               WEB MISSION CONTROL BACKEND TOPOLOGY                               │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                  ┌──────────────────────────────┴──────────────────────────────┐
                  ▼                                                             ▼
       [ PORT 8080: Static File Server ]                             [ PORT 3000: Forensic API Proxy ]
       File: `js/static_server.js`                                   File: `js/server.js`
       ├── Serves HTML/CSS/JS Assets                                 ├── Firebase Admin SDK Engine
       ├── Reverse-Proxies `/api/*` -> Port 3000                     ├── REST Endpoints (/api/devices, etc.)
       └── Handles MIME Types & Compression                          ├── Server-Sent Events (SSE) Stream
                  │                                                             │
                  └──────────────────────────────┬──────────────────────────────┘
                                                 ▼
                             [ FIREBASE REALTIME DATABASE CLOUD ]
                               Project ID: `jfindme-3b15c`
```

### 1.1 `js/server.js`: Enterprise Node.js Express API & Proxy Server
- **Port**: 3000 (configurable via `PROXY_PORT` or `PORT`).
- **Initialization**: Automatically auto-detects Google Cloud Service Account credentials (`secrets/service-account.json` or `secrets/jfindme-3b15c-firebase-adminsdk-*.json`) and client settings (`secrets/google-services.json`).
- **Fatal Trap Anchors**: Implements `uncaughtException` and `unhandledRejection` guards preventing server termination from network drops.
- **Key REST API Endpoints**:
  - `GET /api/health`: Returns server status, uptime, and Firebase connection state.
  - `GET /api/devices`: Returns the array of registered fleet nodes with current vitals.
  - `POST /api/devices/:id/config`: Updates node configuration (alias, reporting cadence).
  - `DELETE /api/devices/:id`: De-registers a device and optionally cascades history purges.
  - `GET /api/locations`: Returns real-time or historical breadcrumb coordinates.
  - `GET /api/geofences`: Fetches active SafeZones and perimeter polygon coordinates.
  - `POST /api/geofences`: Creates or mutates a SafeZone definition.
  - `POST /api/poke/:id`: Sends a high-priority push poke to wake an edge device.
  - `GET /api/sync-settings`: Returns the 178 system parameters across 20 modules.
  - `POST /api/sync-settings`: Commits parameter updates to the cloud SSOT.
  - `GET /api/events/stream`: Server-Sent Events (SSE) real-time streaming endpoint for live telemetry updates.

### 1.2 `js/static_server.js`: Lightweight HTTP Reverse Proxy
- **Port**: 8080.
- **Responsibilities**:
  - Delivers `index.html`, `database-sync-settings.html`, `launcher.html`, stylesheets, and JavaScript ES6 modules.
  - Intercepts requests matching `/api/*` and transparently reverse-proxies them to `http://127.0.0.1:3000`.
  - Injects CORS headers allowing local development without security cross-origin violations.

---

## 2. Client-Side Operating Engine Hierarchy

Within the browser environment (`jFindMeWeb/js/`), specialized single-responsibility engines coordinate application workflows:

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                            CLIENT-SIDE OPERATING ENGINE PIPELINE                                 │
├──────────────────────┬───────────────────────────────┬───────────────────────────────────────────┤
│ Engine Module        │ Primary Responsibilities      │ Failure Recovery & Performance            │
├──────────────────────┼───────────────────────────────┼───────────────────────────────────────────┤
│ `FirebaseService.js` │ Real-time SSE / WebSocket     │ Auto-reconnect with exponential backoff   │
│ (`FBBridge`)         │ synchronization with cloud    │ (1s, 2s, 4s, up to 30s max interval)     │
├──────────────────────┼───────────────────────────────┼───────────────────────────────────────────┤
│ `OfflineQueue.js`    │ Local mutation buffer for     │ IndexedDB / localStorage persistence;     │
│                      │ offline operation             │ flushes automatically upon online event   │
├──────────────────────┼───────────────────────────────┼───────────────────────────────────────────┤
│ `TelemetryValidator` │ Ingress data sanity guard     │ Discards coordinates with speed > 250km/h │
│                      │                               │ or invalid latitude/longitude ranges      │
├──────────────────────┼───────────────────────────────┼───────────────────────────────────────────┤
│ `SanitizerService.js`│ XSS prevention & text clean   │ Strips `<script>`, `onerror`, `onload`    │
│                      │ for aliases and terminal logs │ before DOM injection via innerHTML        │
├──────────────────────┼───────────────────────────────┼───────────────────────────────────────────┤
│ `GeoUtils.js`        │ Spatial calculations          │ Haversine, Bearing, Ray-Casting in-poly   │
├──────────────────────┼───────────────────────────────┼───────────────────────────────────────────┤
│ `ReplayEngine.js`    │ Chronological trajectory      │ Interpolates coordinates along polyline;  │
│                      │ playback                      │ supports 1x, 2x, 5x, 10x speed multipliers│
└──────────────────────┴───────────────────────────────┴───────────────────────────────────────────┘
```

---

## 3. Edge Android Operating Engine Integration (`jButterFly`)

`jFindMeWeb` functions as the mission control console for the native Android client (`jButterFly`). The web backend coordinates directly with the edge operating engines:

### 3.1 Foreground Services:
1. **`TrackingService.kt`**: High-priority Android Foreground Service holding a partial `WakeLock`. Acquires GNSS fixes via `FusedLocationProviderClient` and posts to `/live-map/` every 15 seconds (active) or 1 second (high speed).
2. **`SIMTrackingService.kt`**: Telephony monitoring service tracking dual-SIM changes, cellular tower handovers (MCC, MNC, LAC, CID), and signal strength (dBm).
3. **`FCMService.kt`**: High-priority Firebase Cloud Messaging receiver handling remote poke requests, emergency acoustic beacon dispatch, and configuration sync commands.

### 3.2 4-Tier Background Job Waterfall (13 Engine Workers):
```
┌────────────────────────────────────────────────────────────────────────┐
│ TIER 1: 🛰️ PRIMARY TELEMETRY & CORE GPS PIPELINE (Root / Critical)     │
│ ├── JOB_TRACKING_SERVICE      : Foreground GPS Telemetry Service       │
│ ├── JOB_LOCATION_WATCHDOG     : Sentinel Process Keep-Alive Monitor    │
│ └── JOB_GEOFENCE_SENTINEL     : Hardware Geofence Perimeter Sentinel   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Sequential Dependency)
┌───────────────────────────────────▼────────────────────────────────────┐
│ TIER 2: ☁️ CLOUD SYNC & PEER STREAMING HUB (Network Synchronization)   │
│ ├── JOB_LIVE_MAP_STREAM       : Peer WebSocket RTDB Stream             │
│ ├── JOB_PRESENCE_PULSE        : Real-time Presence Pulse & Vitals Ping │
│ ├── JOB_OFFLINE_SYNC          : Encrypted Offline SQLite Queue Flusher │
│ └── JOB_FCM_MESSAGING         : High-Priority FCM Push Sentinel        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Sequential Dependency)
┌───────────────────────────────────▼────────────────────────────────────┐
│ TIER 3: 📱 FORENSIC HARDWARE & SECURITY (Passive Hardware Interceptors)│
│ ├── JOB_SECURITY_RECEIVER     : Power, Boot, Flight Mode Interceptor   │
│ ├── JOB_ACOUSTIC_SIREN        : Emergency Beacon & Audio Bridge        │
│ └── JOB_SIM_FORENSIC          : SIM & Cellular Tower Hardware Sentinel │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Sequential Dependency)
┌───────────────────────────────────▼────────────────────────────────────┐
│ TIER 4: 🧹 STORAGE HYGIENE & SYSTEM MAINTENANCE (Deep Maintenance)     │
│ ├── JOB_STORAGE_HYGIENE       : Rolling Purge & SQLite VACUUM          │
│ ├── JOB_AUTO_UPDATE           : Self-Healing Binary Delta Patcher      │
│ └── JOB_DIAGNOSTIC_HEALTH     : Battery & Baseband Health Sentinel     │
└────────────────────────────────────────────────────────────────────────┘
```

### 3.3 14-Condition `SimTransportValidator`:
Audits network transport viability across 14 edge device conditions:
1. Airplane Mode Enabled
2. SIM State Absent
3. Cellular Data Disabled
4. Roaming Restricted
5. Captive Portal Detected
6. VPN Tunnel Interception
7. Metered Network Throttling
8. Battery Saver Restricting Background Data
9. Doze Mode Deep Sleep Active
10. App Standby Bucket Rare/Restricted
11. Missing Background Location Permission (`ACCESS_BACKGROUND_LOCATION`)
12. Location Services Globally Disabled
13. Wi-Fi Connected without Internet Route
14. Baseband Modem Radio Off / Emergency Calls Only

---

## 4. Release Daemon & Delta Patcher (`apk/sync_service.py`)

- **Daemon Port**: 8085.
- **Responsibilities**:
  - Automatically monitors compiled APK releases in `apk/`.
  - Calculates SHA-256 certificate fingerprints.
  - Automatically increments version codes in `version.properties`.
  - Generates binary delta patches via `tools/jdelta_generator.py` for bandwidth-efficient OTA distribution.
  - Pushes verified releases to the cloud deployment repository.

