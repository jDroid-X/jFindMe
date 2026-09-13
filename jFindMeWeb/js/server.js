const express = require("express");
const cors = require("cors");
const admin = require("firebase-admin");
const fs = require("fs");
const path = require("path");

process.on("uncaughtException", (err) => {
    console.error("[FATAL_TRAP] Uncaught Exception:", err.message);
});
process.on("unhandledRejection", (reason) => {
    console.error("[FATAL_TRAP] Unhandled Rejection:", reason);
});

console.log("[SYSTEM] Initializing jFindMe Forensic Proxy...");

// --- PATH VALIDATION (DYNAMIC RESOLVER) ---
let SA_PATH = path.join(__dirname, "../service-account.json");
const GS_PATH = path.join(__dirname, "../google-services.json");

// Auto-Detect specific service account if generic is missing
if (!fs.existsSync(SA_PATH)) {
    const root = path.join(__dirname, "../");
    const files = fs.readdirSync(root);
    const saFile = files.find(f => f.includes("firebase-adminsdk") && f.endsWith(".json"));
    if (saFile) {
        SA_PATH = path.join(root, saFile);
        console.log(`[INFO] Identity Resolver: Using forensic credential [${saFile}]`);
    }
}

if (!fs.existsSync(SA_PATH)) {
    console.error(`[FATAL] Missing service-account.json at ${SA_PATH}`);
    process.exit(1);
}
if (!fs.existsSync(GS_PATH)) {
    console.error(`[FATAL] Missing google-services.json at ${GS_PATH}`);
    process.exit(1);
}

const G_SERVICE_ACCOUNT = JSON.parse(fs.readFileSync(SA_PATH, "utf8"));
const G_SERVICES = JSON.parse(fs.readFileSync(GS_PATH, "utf8"));

const DATABASE_URL = G_SERVICES.project_info.firebase_url;
const PROJECT_ID = G_SERVICES.project_info.project_id;

console.log(`[INFO] Targeting Project: ${PROJECT_ID}`);
console.log(`[INFO] Database URL: ${DATABASE_URL}`);

try {
    admin.initializeApp({
      credential: admin.credential.cert(G_SERVICE_ACCOUNT),
      databaseURL: DATABASE_URL
    });
    console.log("[SUCCESS] Firebase Admin SDK Initialized.");
} catch (e) {
    console.error("[FATAL] SDK Initialization Failed:", e.message);
    process.exit(1);
}

const db = admin.database();
const fs_db = admin.firestore();
const app = express();

// Internal Event Bus for Forensic Matrix Monitoring
function broadcastMatrixEvent(eventName, data) {
    if (process.env.DEBUG_EVENTS) {
        console.log(`[MATRIX_EVENT] [${eventName}]`, data);
    }
}

// CORS Configuration - Restrict in production
const allowedOrigins = process.env.NODE_ENV === 'production' 
    ? (process.env.ALLOWED_ORIGINS || '').split(',').filter(Boolean)
    : ['http://127.0.0.1:3000', 'http://localhost:3000', 'http://127.0.0.1:8080'];

app.use(cors({
    origin: function(origin, callback) {
        // Allow requests with no origin (like mobile apps or curl)
        if (!origin) return callback(null, true);
        
        if (allowedOrigins.indexOf(origin) !== -1) {
            callback(null, true);
        } else {
            callback(new Error('Not allowed by CORS'));
        }
    },
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization']
}));
app.use(express.json());
app.use((err, req, res, next) => {
    if (err instanceof SyntaxError && err.status === 400 && 'body' in err) {
        return res.status(400).json({ error: "INVALID_JSON_BODY", message: err.message });
    }
    next(err);
});

// Log every request
app.use((req, res, next) => {
    console.log(`[NETWORK] ${new Date().toLocaleTimeString()} ${req.method} ${req.url}`);
    next();
});

// Serve Avatar static files
const AVATAR_ROOT_1 = path.join(__dirname, "../../Avatar");
const AVATAR_ROOT_2 = path.join(__dirname, "../Avatar");
if (fs.existsSync(AVATAR_ROOT_1)) {
    app.use("/Avatar", express.static(AVATAR_ROOT_1));
    app.use("/avatars", express.static(AVATAR_ROOT_1));
} else if (fs.existsSync(AVATAR_ROOT_2)) {
    app.use("/Avatar", express.static(AVATAR_ROOT_2));
    app.use("/avatars", express.static(AVATAR_ROOT_2));
}

// Serve Frontend Static Files
app.use(express.static(path.join(__dirname, "../")));

// In-Memory Short TTL Caches for High-Frequency Polling (Sub-millisecond response)
let LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };
let SAFE_ZONES_CACHE = { data: null, timestamp: 0 };
const CACHE_TTL_MS = 2000; // 2-second cache window for instant responses

function parseTimestampToMillis(raw) {
    if (!raw) return 0;
    if (typeof raw === 'number') {
        if (raw > 1e14) return Math.floor(raw / 1000);
        if (raw < 1e11 && raw > 1e9) return raw * 1000;
        return raw;
    }
    const str = String(raw).trim();
    if (/^\d{14,16}$/.test(str)) {
        const y = parseInt(str.substring(0, 4), 10);
        const m = parseInt(str.substring(4, 6), 10) - 1;
        const d = parseInt(str.substring(6, 8), 10);
        const hh = parseInt(str.substring(8, 10), 10);
        const mm = parseInt(str.substring(10, 12), 10);
        const ss = parseInt(str.substring(12, 14), 10);
        return new Date(y, m, d, hh, mm, ss).getTime();
    }
    const parsed = Date.parse(str);
    if (!isNaN(parsed)) return parsed;
    const num = Number(str);
    if (!isNaN(num) && num > 0) return parseTimestampToMillis(num);
    return 0;
}

// Live Devices Aggregation Endpoint for Map Radar
app.get("/api/devices-live", async (req, res) => {
    const now = Date.now();
    // Return cached response instantly if within 2-second window
    if (LIVE_DEVICES_CACHE.data && LIVE_DEVICES_CACHE.data.count > 0 && (now - LIVE_DEVICES_CACHE.timestamp) < CACHE_TTL_MS) {
        return res.json(LIVE_DEVICES_CACHE.data);
    }

    try {
        const [devicesSnap, lookupSnap, geofenceSnap] = await Promise.all([
            db.ref("devices").once("value"),
            db.ref("lookup_indices/auth_to_device").once("value").catch(() => null),
            db.ref("Geofencing").once("value").catch(() => null)
        ]);

        const devicesData = devicesSnap.val() || {};
        const authToDevice = (lookupSnap && lookupSnap.val()) || {};
        const geofenceData = (geofenceSnap && geofenceSnap.val()) || {};

        // Build a map of all Safe Zone coordinates for quick fallback resolution
        const safeZoneCoords = new Map();
        for (const [creatorId, creatorNode] of Object.entries(geofenceData)) {
            if (!creatorNode) continue;
            const zonesMap = creatorNode.safe_zones || (creatorNode.latitude || creatorNode.lat ? { [creatorId]: creatorNode } : creatorNode);
            for (const [zoneId, zone] of Object.entries(zonesMap)) {
                if (zone && (zone.lat || zone.latitude) && (zone.lng || zone.longitude)) {
                    const zLat = parseFloat(zone.lat || zone.latitude);
                    const zLng = parseFloat(zone.lng || zone.longitude);
                    const zName = zone.zone_name || zone.name || "Safe Zone";
                    safeZoneCoords.set(zone.zone_id || zoneId, { lat: zLat, lng: zLng, name: zName });
                    if (zone.zone_name) safeZoneCoords.set(zone.zone_name.toLowerCase(), { lat: zLat, lng: zLng, name: zName });
                }
            }
        }

        // Merge Auth UID / Hardware Key nodes into canonical 8-character device nodes (First 4 + Last 4)
        for (const [key, node] of Object.entries(devicesData)) {
            if (!node || typeof node !== "object") continue;
            let targetAgentId = authToDevice[key];
            if (!targetAgentId && (key.startsWith("HW_") || key.length > 8)) {
                const cleanKey = key.replace("HW_", "").replace(/[^A-Za-z0-9]/g, "").toUpperCase();
                if (cleanKey.length >= 8) {
                    targetAgentId = cleanKey.slice(0, 4) + cleanKey.slice(-4);
                }
            }
            if (targetAgentId && targetAgentId !== key && devicesData[targetAgentId]) {
                const canonical = devicesData[targetAgentId];
                if (node.device && node.device.live) {
                    canonical.device = canonical.device || {};
                    canonical.device.live = Object.assign({}, canonical.device.live || {}, node.device.live);
                }
                if (node.profile) {
                    canonical.profile = Object.assign({}, canonical.profile || {}, node.profile);
                }
            }
        }

        const devicesList = [];

        for (const [agentId, node] of Object.entries(devicesData)) {
            if (!node || typeof node !== "object") continue;
            if (agentId.length > 16 || agentId.startsWith("HW_")) continue;

            const profile = node.profile || {};
            const deviceInfo = node.device || {};
            const identity = node.Identity || {};
            const telemetry = node.telemetry || {};
            const safeZones = node.safe_zones || {};
            const relationships = node.relationships || {};

            // 1. Determine Live Location from node.device.live, node.live, node.location, or node.history
            let liveLoc = (deviceInfo.live) || node.live || node.location || {};
            if ((!liveLoc.latitude || !liveLoc.longitude) && node.history) {
                const histEntries = Object.entries(node.history);
                if (histEntries.length > 0) {
                    histEntries.sort((a, b) => (parseTimestampToMillis(b[1].timestamp || b[1].updated_at) - parseTimestampToMillis(a[1].timestamp || a[1].updated_at)));
                    const latestItem = histEntries[0][1];
                    liveLoc = {
                        latitude: latestItem.lat || latestItem.latitude,
                        longitude: latestItem.lng || latestItem.longitude,
                        speed: latestItem.speed,
                        address: latestItem.address || latestItem.geo_address,
                        timestamp: latestItem.timestamp || latestItem.updated_at
                    };
                }
            }

            let lat = parseFloat(liveLoc.latitude || liveLoc.lat || deviceInfo.latitude || node.latitude || 0);
            let lng = parseFloat(liveLoc.longitude || liveLoc.lng || deviceInfo.longitude || node.longitude || 0);

            // 2. Safe Zone Fallback: If GPS coordinates are 0 and device is in safe zone or matching name
            if (lat === 0 || lng === 0) {
                const zoneKey = liveLoc.current_safe_zone_id || liveLoc.current_safe_zone_name || (liveLoc.safe_zone || "").toLowerCase();
                const zoneInfo = safeZoneCoords.get(zoneKey) || safeZoneCoords.get(String(zoneKey).toLowerCase());
                if (zoneInfo) {
                    lat = zoneInfo.lat;
                    lng = zoneInfo.lng;
                    liveLoc.address = liveLoc.address || `Inside ${zoneInfo.name} Safe Perimeter`;
                }
            }

            // Determine Online State & Accurate Elapsed Time
            const rawTs = liveLoc.updated_at || liveLoc.timestamp || telemetry.last_seen || deviceInfo.updated_at || identity.updated_at || 0;
            const lastSeenEpoch = parseTimestampToMillis(rawTs);
            const isRecent = lastSeenEpoch > 0 && (now - lastSeenEpoch < 15 * 60 * 1000);
            const isOnline = (identity.st_session_state === "ONLINE") || isRecent;

            // Resolve Avatar ID
            const avatarId = profile.avatar_id || identity.avatar_id || "avatar_custom_1";

            // Resolve Hardware Device Model / Device Name
            const model = deviceInfo.model || deviceInfo.device_model || (deviceInfo.fingerprint ? deviceInfo.fingerprint.split('/')[1] : "") || "Android Device";
            const deviceName = deviceInfo.deviceName || model;

            // Resolve User / Persona / Host Name
            const name = profile.name || profile.fullName || profile.userName || identity.owner_name || identity.id_full_name || liveLoc.userName || deviceName || agentId;
            const hostName = identity.owner_name || identity.id_full_name || profile.fullName || profile.name || name;

            // Resolve Roles
            const role = profile.role_title || profile.role_family || profile.role || "MEMBER";
            const roleFamily = profile.role_family || "None";
            const roleFriend = profile.role_friend || "None";

            devicesList.push({
                agentId,
                deviceId: deviceInfo.deviceId || identity.uid_2 || `HW_${agentId}`,
                name,
                deviceName,
                model,
                hostName,
                avatarId,
                latitude: lat,
                longitude: lng,
                hasValidLocation: !isNaN(lat) && !isNaN(lng) && lat !== 0 && lng !== 0,
                speed: parseFloat(liveLoc.speed || telemetry.speed_mps || 0),
                bearing: parseFloat(liveLoc.bearing || 0),
                accuracy: parseFloat(liveLoc.accuracy || 0),
                altitude: parseFloat(liveLoc.altitude || 0),
                battery: parseInt(telemetry.battery_pct != null ? telemetry.battery_pct : (liveLoc.battery != null ? liveLoc.battery : -1)),
                isCharging: Boolean(telemetry.is_charging),
                isMockGps: Boolean(telemetry.is_mock_gps),
                isAirplaneMode: Boolean(telemetry.is_airplane_mode),
                networkType: telemetry.network_type || "UNKNOWN",
                address: liveLoc.address || liveLoc.geo_address || profile.address || "",
                lastSeen: rawTs || lastSeenEpoch,
                isOnline,
                sessionState: identity.st_session_state || (isOnline ? "ONLINE" : "OFFLINE"),
                email: profile.email || profile.email_id || "",
                phone: profile.phone || "",
                role,
                role_family: roleFamily,
                role_friend: roleFriend,
                department: profile.department || profile.org_name || profile.deptName || "",
                emergencyNote: profile.emergency_note || profile.emergencyNote || "",
                safeZonesCount: Object.keys(safeZones).length,
                relationshipsCount: Object.keys(relationships).length,
                raw: node
            });
        }

        const responsePayload = {
            status: "SUCCESS",
            total: devicesList.length,
            count: devicesList.length,
            withGps: devicesList.filter(d => d.hasValidLocation).length,
            devices: devicesList,
            timestamp: now
        };
        LIVE_DEVICES_CACHE = { data: responsePayload, timestamp: now };
        res.json(responsePayload);
    } catch (err) {
        console.error("[ERROR] /api/devices-live failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Safe Zones Geofence Aggregation Endpoint
app.get("/api/safe-zones", async (req, res) => {
    const now = Date.now();
    if (SAFE_ZONES_CACHE.data && (now - SAFE_ZONES_CACHE.timestamp) < CACHE_TTL_MS) {
        return res.json(SAFE_ZONES_CACHE.data);
    }

    try {
        const [geofenceSnap, devicesSnap, relSnap, lookupSnap] = await Promise.all([
            db.ref("Geofencing").once("value").catch(() => null),
            db.ref("devices").once("value").catch(() => null),
            db.ref("relationships").once("value").catch(() => null),
            db.ref("lookup_indices/auth_to_device").once("value").catch(() => null)
        ]);
        const geofenceData = (geofenceSnap && geofenceSnap.val()) || {};
        const devicesData = (devicesSnap && devicesSnap.val()) || {};
        const relData = (relSnap && relSnap.val()) || {};
        const authToDevice = (lookupSnap && lookupSnap.val()) || {};

        const zonesList = [];
        const seenZoneIds = new Set();

        const resolveCanonicalDevId = (rawKey) => {
            if (!rawKey) return rawKey;
            return authToDevice[rawKey] || rawKey;
        };

        // Helper to process and normalize a single safe zone payload
        const processZoneNode = (zKey, zone, creatorHint = "") => {
            if (!zone || typeof zone !== "object") return;
            const zoneId = zone.zone_id || zKey;
            if (seenZoneIds.has(zoneId)) return;
            seenZoneIds.add(zoneId);

            const connMap = zone.connected_devices || {};
            const connList = Object.keys(connMap).map(k => resolveCanonicalDevId(k)).filter((v, i, a) => a.indexOf(v) === i);

            // Extract nested devices node
            const devMap = zone.devices || zone.connected_devices || {};
            if (devMap && typeof devMap === 'object') {
                Object.keys(devMap).forEach(dKey => {
                    const canonicalDev = resolveCanonicalDevId(dKey);
                    if (!connList.includes(canonicalDev)) connList.push(canonicalDev);
                });
            }

            // Extract nested alert_notices and date-grouped notifications under connected devices
            const historyObj = zone.history || {};
            if (devMap && typeof devMap === 'object') {
                for (const [dKey, devNode] of Object.entries(devMap)) {
                    if (!devNode || typeof devNode !== 'object') continue;
                    
                    // 1. Check alert_notices node
                    if (devNode.alert_notices && typeof devNode.alert_notices === 'object') {
                        for (const [aId, alertNotice] of Object.entries(devNode.alert_notices)) {
                            historyObj[aId] = {
                                event_id: aId,
                                datetime: alertNotice.readable_time || (alertNotice.timestamp ? new Date(alertNotice.timestamp).toLocaleString() : "Recorded Alert"),
                                status: alertNotice.event_type || "TRANSITION",
                                target_name: devNode.device_name || alertNotice.target_name || dKey,
                                target_uid: resolveCanonicalDevId(alertNotice.agent_id || dKey),
                                zone_name: zone.zone_name || zone.name || "Safe Perimeter",
                                timestamp: alertNotice.timestamp || Date.now()
                            };
                        }
                    }

                    // 2. Check Android notifications date-grouped tree (FirebaseComm.kt standard)
                    if (devNode.notifications && typeof devNode.notifications === 'object') {
                        for (const [dateStr, notifGroup] of Object.entries(devNode.notifications)) {
                            if (notifGroup && typeof notifGroup === 'object') {
                                for (const [aId, notifItem] of Object.entries(notifGroup)) {
                                    historyObj[aId] = {
                                        event_id: aId,
                                        datetime: notifItem.readable_time || (notifItem.timestamp ? new Date(notifItem.timestamp).toLocaleString() : "Recorded Alert"),
                                        status: notifItem.event_type || "TRANSITION",
                                        target_name: devNode.device_name || notifItem.target_name || dKey,
                                        target_uid: resolveCanonicalDevId(notifItem.agent_id || dKey),
                                        zone_name: zone.zone_name || zone.name || "Safe Perimeter",
                                        timestamp: notifItem.timestamp || Date.now()
                                    };
                                }
                            }
                        }
                    }
                }
            }

            zonesList.push({
                zoneId: zoneId,
                zoneName: zone.zone_name || zone.name || "Safe Perimeter",
                latitude: parseFloat(zone.latitude || zone.lat || 0),
                longitude: parseFloat(zone.longitude || zone.lng || 0),
                radius: parseFloat(zone.radius || zone.radius_meters || 100),
                polygonPoints: zone.polygon_points || zone.polygonPoints || zone.polygonPointsJson || "",
                dwellMinutes: parseInt(zone.dwell_minutes || zone.dwellMinutes || 0, 10),
                address: zone.address || "",
                assignedName: zone.assigned_targets || zone.assignedName || "",
                creatorId: resolveCanonicalDevId(zone.creator_id || creatorHint || zone.created_by || "UNKNOWN_CREATOR"),
                createdAt: zone.created_at || zone.timestamp || zone.createdAt || Date.now(),
                isActive: zone.is_active !== false && zone.isZoneActive !== false,
                alertsEnabled: Boolean(zone.alerts_enabled || zone.alertsEnabled),
                history: historyObj,
                connectedDevices: connList,
                devicesTree: devMap
            });
        };

        // 1. Extract from Geofencing tree (Handles BOTH root /Geofencing/{zoneId} AND /Geofencing/{creatorId}/safe_zones/{zoneId})
        for (const [key, node] of Object.entries(geofenceData)) {
            if (!node || typeof node !== "object") continue;
            
            // Check if node is creator container with safe_zones sub-tree
            if (node.safe_zones && typeof node.safe_zones === 'object') {
                for (const [zId, zNode] of Object.entries(node.safe_zones)) {
                    processZoneNode(zId, zNode, key);
                }
            } else {
                // Root-level zone
                processZoneNode(key, node, node.creator_id || node.created_by || "");
            }
        }

        // 2. Extract from relationships tree (/relationships/{groupId}/geofences/{zoneId})
        for (const [groupId, groupNode] of Object.entries(relData)) {
            if (!groupNode || typeof groupNode !== "object") continue;
            const relFences = groupNode.geofences || {};
            for (const [zId, zNode] of Object.entries(relFences)) {
                processZoneNode(zId, zNode, groupId);
            }
        }

        // 3. Extract from devices tree (/devices/{agentId}/safe_zones/{zoneId})
        for (const [dKey, devNode] of Object.entries(devicesData)) {
            if (!devNode || typeof devNode !== "object") continue;
            const devZones = devNode.safe_zones || {};
            for (const [zId, zNode] of Object.entries(devZones)) {
                processZoneNode(zId, zNode, dKey);
            }
        }

        // Filter valid coordinates
        const validZones = zonesList.filter(z => (z.latitude !== 0 || z.longitude !== 0 || (z.polygonPoints && z.polygonPoints.length > 0)));

        SAFE_ZONES_CACHE.data = { status: "SUCCESS", count: validZones.length, safeZones: validZones, timestamp: now };
        SAFE_ZONES_CACHE.timestamp = now;

        const zonePayload = { status: "SUCCESS", count: validZones.length, safeZones: validZones, timestamp: now };
        broadcastMatrixEvent("matrix_refresh", { entity: "safe_zones", timestamp: now, count: validZones.length });
        return res.json(zonePayload);

    } catch (error) {
        console.error("[SAFE_ZONES] Error aggregating safe zones:", error);
        return res.status(500).json({ error: "INTERNAL_ERROR", message: error.message });
    }
});

// Safe Zone Creation Endpoint (Authoritative Web Admin Engine)
app.post("/api/save-safe-zone", async (req, res) => {
    try {
        const { zoneId, zoneName, latitude, longitude, radius, isActive, alertsEnabled, connectedDevices, polygonPoints, dwellMinutes, creatorId } = req.body;
        if (!zoneName || isNaN(latitude) || isNaN(longitude)) {
            return res.status(400).json({ error: "INVALID_PARAMETERS", message: "Zone name, latitude and longitude are required." });
        }

        const cleanZoneId = zoneId || `ZONE_${Date.now()}`;
        const hostCreator = creatorId || "WEB_DISPATCHER";
        const cleanCreator = hostCreator.replace(/[\.\#\$\[\]\/]/g, "_");

        const zonePayload = {
            zone_id: cleanZoneId,
            zone_name: zoneName,
            lat: parseFloat(latitude),
            lng: parseFloat(longitude),
            radius: parseFloat(radius || 100),
            is_active: isActive !== false,
            alerts_enabled: Boolean(alertsEnabled),
            polygon_points: polygonPoints || "",
            dwell_minutes: parseInt(dwellMinutes || 0, 10),
            created_on: new Date().toISOString(),
            updated_at: Date.now(),
            created_by: cleanCreator,
            connected_devices: {}
        };

        if (Array.isArray(connectedDevices)) {
            connectedDevices.forEach(dId => {
                if (dId) {
                    const cleanDId = dId.replace(/[\.\#\$\[\]\/]/g, "_");
                    zonePayload.connected_devices[cleanDId] = {
                        assigned_at: Date.now(),
                        device_name: dId,
                        status: "ACTIVE"
                    };
                }
            });
        }

        await db.ref(`Geofencing/${cleanCreator}/safe_zones/${cleanZoneId}`).set(zonePayload);
        SAFE_ZONES_CACHE.timestamp = 0; // Invalidate cache

        return res.json({
            status: "SUCCESS",
            message: `Safe Zone [${zoneName}] established successfully.`,
            zone: zonePayload
        });
    } catch (err) {
        console.error("[SAFE_ZONES] Error saving safe zone from web:", err);
        return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
    }
});

// Update SafeZone Assigned Targets Endpoint (Consolidated)
app.post("/api/safezone/update-targets", async (req, res) => {
    const { creatorUid, zoneId, assignedTargets, connectedDevices } = req.body;
    if (!zoneId) return res.status(400).json({ error: "MISSING_ZONE_ID" });

    try {
        console.log(`[SAFE_ZONES] Updating targets for zone [${zoneId}] to: "${assignedTargets}"`);

        const snap = await db.ref(`Geofencing`).once('value');
        const geofencing = snap.val() || {};
        let updated = false;

        for (const [zKey, zNode] of Object.entries(geofencing)) {
            if (zNode && (zNode.zone_id === zoneId || zKey === zoneId || zKey.includes(zoneId))) {
                await db.ref(`Geofencing/${zKey}`).update({
                    assigned_targets: assignedTargets || "Connected Devices",
                    connected_devices: Array.isArray(connectedDevices) ? connectedDevices : (zNode.connected_devices || []),
                    updated_at: Date.now()
                });
                updated = true;
            }
        }

        if (creatorUid) {
            await db.ref(`devices/${creatorUid}/safe_zones/${zoneId}`).update({
                assigned_targets: assignedTargets || "Connected Devices",
                updated_at: Date.now()
            }).catch(() => null);
        }

        SAFE_ZONES_CACHE = { data: null, timestamp: 0 };
        res.json({ status: "SUCCESS", zoneId, assignedTargets });
    } catch (err) {
        console.error("[ERROR] /api/safezone/update-targets failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Decommission / Delete SafeZone Endpoint (Consolidated)
app.post("/api/safezone/delete", async (req, res) => {
    const { creatorUid, zoneId } = req.body;
    if (!zoneId) return res.status(400).json({ error: "MISSING_ZONE_ID" });

    try {
        console.log(`[SAFE_ZONES] Decommissioning Safe Zone [${zoneId}] for Creator [${creatorUid || 'ANY'}]...`);
        const snap = await db.ref(`Geofencing`).once('value');
        const geofencing = snap.val() || {};

        for (const [zKey, zNode] of Object.entries(geofencing)) {
            if (zNode && (zNode.zone_id === zoneId || zKey === zoneId || zKey.includes(zoneId))) {
                await db.ref(`Geofencing/${zKey}`).remove();
            }
        }

        if (creatorUid) {
            await db.ref(`devices/${creatorUid}/safe_zones/${zoneId}`).remove().catch(() => null);
        }

        SAFE_ZONES_CACHE = { data: null, timestamp: 0 };
        res.json({ status: "DELETED", zoneId });
    } catch (err) {
        console.error("[ERROR] /api/safezone/delete failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Helper Handler for Relationship Binding / Addition (Enforces Web Read-Only Policy)
const handleRelationshipUpdate = async (req, res) => {
    return res.status(403).json({
        error: "READ_ONLY_MODE",
        message: "Web Portal is configured as a Read-Only Analytics Engine. Relationships must be established authoritatively via mobile QR pairing."
    });
};

// Bind / Update Device Relationship Endpoint (supports both /add and /update aliases)
app.post("/api/relationship/update", handleRelationshipUpdate);
app.post("/api/relationship/add", handleRelationshipUpdate);

// Sever / Delete Device Relationship Endpoint (Consolidated)
app.post("/api/relationship/delete", async (req, res) => {
    const agentId = req.body.agentId || req.body.sourceId;
    const targetAgentId = req.body.targetAgentId || req.body.targetId;

    if (!agentId || !targetAgentId) {
        return res.status(400).json({ error: "MISSING_AGENT_IDS", message: "Both agentId and targetAgentId are required." });
    }

    try {
        const cleanAgentId = await resolveCanonicalAgentId(agentId);
        const cleanTargetId = await resolveCanonicalAgentId(targetAgentId);
        console.log(`[RELATIONSHIP] Severing connection: [${cleanAgentId}] <-> [${cleanTargetId}]`);

        await Promise.all([
            db.ref(`devices/${cleanAgentId}/relationships/${cleanTargetId}`).remove().catch(() => null),
            db.ref(`devices/${cleanTargetId}/relationships/${cleanAgentId}`).remove().catch(() => null),
            db.ref(`relationships/GRP_${cleanAgentId}/devices/${cleanTargetId}`).remove().catch(() => null),
            db.ref(`relationships/GRP_${cleanTargetId}/devices/${cleanAgentId}`).remove().catch(() => null),
            db.ref(`relationships/GRP_${cleanAgentId}/members/${cleanTargetId}`).remove().catch(() => null),
            db.ref(`relationships/GRP_${cleanTargetId}/members/${cleanAgentId}`).remove().catch(() => null),
            db.ref(`relationships/GRP_${cleanAgentId}/direct_pairs/${cleanTargetId}`).remove().catch(() => null),
            db.ref(`relationships/GRP_${cleanTargetId}/direct_pairs/${cleanAgentId}`).remove().catch(() => null),
            db.ref(`relationships/GRP_${cleanAgentId}/citizen_dossiers/${cleanTargetId}`).remove().catch(() => null),
            db.ref(`relationships/GRP_${cleanTargetId}/citizen_dossiers/${cleanAgentId}`).remove().catch(() => null)
        ]);

        LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };
        res.json({ status: "SUCCESS", agentId: cleanAgentId, targetAgentId: cleanTargetId });
    } catch (err) {
        console.error("[ERROR] /api/relationship/delete failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Device Telemetry Poke Endpoint (Consolidated)
app.post("/api/poke-device", async (req, res) => {
    let { agentId, pokedBy } = req.body;
    if (!agentId) return res.status(400).json({ error: "MISSING_AGENT_ID" });

    try {
        agentId = await resolveCanonicalAgentId(agentId);
        const sender = pokedBy || "Web_Matrix_Commander";
        console.log(`[POKE] Telemetry poke signal dispatched to [${agentId}] from [${sender}]`);

        await Promise.all([
            db.ref(`devices/${agentId}/commands/poke`).set({
                command: "POKE_LOCATION_UPDATE",
                timestamp: Date.now(),
                poked_by: sender
            }).catch(() => null),
            db.ref(`devices/${agentId}/telemetry/poke`).set({
                isPoked: true,
                pokedBy: sender,
                timestamp: Date.now()
            }).catch(() => null)
        ]);

        res.json({ status: "SUCCESS", agentId, timestamp: Date.now() });
    } catch (err) {
        console.error("[ERROR] /api/poke-device failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Device ACL Security Switch Toggle Endpoint (Atomic)
app.post("/api/device/toggle-acl", async (req, res) => {
    let { agentId, flag, value } = req.body;
    if (!agentId || !flag) {
        return res.status(400).json({ error: "MISSING_PARAMETERS", message: "Both agentId and flag are required." });
    }

    try {
        agentId = await resolveCanonicalAgentId(agentId);
        const boolVal = Boolean(value);
        console.log(`[ACL_TOGGLE] Setting [${flag} = ${boolVal}] on [${agentId}]`);

        const updates = {};
        updates[`devices/${agentId}/acl/${flag}`] = boolVal ? "GRANTED" : "DENIED";

        if (flag === "acl_live_tracking") {
            updates[`devices/${agentId}/live_tracking_enabled`] = boolVal;
        } else if (flag === "acl_ghost_override") {
            updates[`devices/${agentId}/ghost_mode`] = !boolVal;
        }

        await db.ref().update(updates);
        LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };

        res.json({ status: "SUCCESS", agentId, flag, value: boolVal, timestamp: Date.now() });
    } catch (err) {
        console.error("[ERROR] /api/device/toggle-acl failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Safe Zone Operational State Toggle Endpoint (Atomic)
app.post("/api/safezone/toggle-state", async (req, res) => {
    let { zoneId, isActive, alertsEnabled, creatorUid } = req.body;
    if (!zoneId) {
        return res.status(400).json({ error: "MISSING_ZONE_ID" });
    }

    try {
        console.log(`[SAFEZONE_TOGGLE] Updating state for zone [${zoneId}]`);
        const updates = {};

        if (isActive !== undefined) {
            updates[`Geofencing/${zoneId}/isActive`] = Boolean(isActive);
            updates[`Geofencing/${zoneId}/status`] = isActive ? "ACTIVE" : "DISABLED";
        }
        if (alertsEnabled !== undefined) {
            updates[`Geofencing/${zoneId}/alertsEnabled`] = Boolean(alertsEnabled);
        }
        updates[`Geofencing/${zoneId}/updated_at`] = Date.now();

        if (creatorUid) {
            const cleanUid = await resolveCanonicalAgentId(creatorUid);
            if (isActive !== undefined) {
                updates[`devices/${cleanUid}/safe_zones/${zoneId}/isActive`] = Boolean(isActive);
            }
            if (alertsEnabled !== undefined) {
                updates[`devices/${cleanUid}/safe_zones/${zoneId}/alertsEnabled`] = Boolean(alertsEnabled);
            }
        }

        await db.ref().update(updates);
        LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };

        res.json({ status: "SUCCESS", zoneId, isActive, alertsEnabled, timestamp: Date.now() });
    } catch (err) {
        console.error("[ERROR] /api/safezone/toggle-state failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});


// Helper Haversine distance calculator in meters
function haversineDistMeters(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// Device Historical Trail Endpoint with Date/Time Filtering & Deadbanding
app.get("/api/device-history", async (req, res) => {
    let agentId = req.query.agentId || req.query.id;
    const limit = parseInt(req.query.limit || 500, 10);
    const startDate = req.query.startDate; // "YYYY-MM-DD"
    const endDate = req.query.endDate;     // "YYYY-MM-DD"
    const startTime = req.query.startTime; // "HH:mm"
    const endTime = req.query.endTime;     // "HH:mm"
    const applyDeadband = req.query.deadband === "true" || req.query.deadband === "1";
    const order = String(req.query.order || "asc").toLowerCase();

    if (!agentId) return res.status(400).json({ error: "MISSING_AGENT_ID" });

    try {
        agentId = await resolveCanonicalAgentId(agentId);
        
        // Fetch historical telemetry from both History tree and devices/{agentId}/history
        const [histSnap1, histSnap2] = await Promise.all([
            db.ref(`History/${agentId}/history`).limitToLast(1000).once("value").catch(() => null),
            db.ref(`devices/${agentId}/history`).limitToLast(1000).once("value").catch(() => null)
        ]);

        const rawEntries = {};
        if (histSnap1 && histSnap1.val()) Object.assign(rawEntries, histSnap1.val());
        if (histSnap2 && histSnap2.val()) Object.assign(rawEntries, histSnap2.val());

        // Parse date-time filters to epoch boundaries if supplied
        let startEpoch = 0;
        let endEpoch = Infinity;

        if (startDate) {
            const timePart = startTime ? `${startTime}:00` : "00:00:00";
            const parsedStart = new Date(`${startDate}T${timePart}`).getTime();
            if (!isNaN(parsedStart)) startEpoch = parsedStart;
        }
        if (endDate) {
            const timePart = endTime ? `${endTime}:59` : "23:59:59";
            const parsedEnd = new Date(`${endDate}T${timePart}`).getTime();
            if (!isNaN(parsedEnd)) endEpoch = parsedEnd;
        }

        let breadcrumbs = [];
        for (const [key, item] of Object.entries(rawEntries)) {
            if (!item || typeof item !== 'object') continue;
            const lat = parseFloat(item.latitude || item.lat || 0);
            const lng = parseFloat(item.longitude || item.lng || 0);
            if (lat === 0 && lng === 0) continue;

            const ts = parseTimestampToMillis(item.timestamp || item.updated_at || item.time || key);
            if (ts <= 0) continue;

            // Date/Time boundary filtering
            if (ts < startEpoch || ts > endEpoch) continue;

            const speed = parseFloat(item.speed || 0);
            const bearing = parseFloat(item.bearing || 0);
            const altitude = parseFloat(item.altitude || 0);
            const accuracy = parseFloat(item.accuracy || 0);
            const address = item.address || item.geo_address || "";

            breadcrumbs.push({
                id: key,
                latitude: lat,
                longitude: lng,
                speed,
                bearing,
                altitude,
                accuracy,
                address,
                timestamp: ts,
                readableTime: new Date(ts).toLocaleString()
            });
        }

        // Chronological sort ascending for processing
        breadcrumbs.sort((a, b) => a.timestamp - b.timestamp);

        // Apply Layer 4 Deadband Noise Filtering (Stationary Suppression)
        if (applyDeadband && breadcrumbs.length > 1) {
            const filtered = [breadcrumbs[0]];
            for (let i = 1; i < breadcrumbs.length; i++) {
                const prev = filtered[filtered.length - 1];
                const curr = breadcrumbs[i];
                const distM = haversineDistMeters(prev.latitude, prev.longitude, curr.latitude, curr.longitude);
                const speedKmh = curr.speed > 50 ? curr.speed : curr.speed * 3.6;

                // Keep point only if user moved > 15m or moving speed > 1.5 km/h, or if it is the latest point
                if (distM >= 15 || speedKmh >= 1.5 || i === breadcrumbs.length - 1) {
                    filtered.push(curr);
                }
            }
            breadcrumbs = filtered;
        }

        // Apply limit
        if (breadcrumbs.length > limit) {
            breadcrumbs = breadcrumbs.slice(-limit);
        }

        // Final sort according to requested order
        if (order === "desc") {
            breadcrumbs.reverse();
        }

        res.json({
            status: "SUCCESS",
            agentId,
            count: breadcrumbs.length,
            startDate: startDate || null,
            endDate: endDate || null,
            deadbandApplied: applyDeadband,
            breadcrumbs
        });
    } catch (err) {
        console.error(`[ERROR] /api/device-history for ${agentId} failed:`, err.message);
        res.status(500).json({ error: err.message, breadcrumbs: [] });
    }
});

// Endpoint: Distinct Historical Dates Discovery for Device
app.get("/api/history-dates", async (req, res) => {
    let agentId = req.query.agentId || req.query.id;
    if (!agentId) return res.status(400).json({ error: "MISSING_AGENT_ID" });

    try {
        agentId = await resolveCanonicalAgentId(agentId);

        const [histSnap1, histSnap2] = await Promise.all([
            db.ref(`History/${agentId}/history`).once("value").catch(() => null),
            db.ref(`devices/${agentId}/history`).once("value").catch(() => null)
        ]);

        const rawEntries = {};
        if (histSnap1 && histSnap1.val()) Object.assign(rawEntries, histSnap1.val());
        if (histSnap2 && histSnap2.val()) Object.assign(rawEntries, histSnap2.val());

        const dateMap = new Map();
        for (const [key, item] of Object.entries(rawEntries)) {
            if (!item || typeof item !== 'object') continue;
            const ts = parseTimestampToMillis(item.timestamp || item.updated_at || item.time || key);
            if (ts <= 0) continue;

            const d = new Date(ts);
            const dateStr = d.toISOString().slice(0, 10); // "YYYY-MM-DD"
            
            const existing = dateMap.get(dateStr) || { date: dateStr, count: 0, firstTs: ts, lastTs: ts };
            existing.count += 1;
            if (ts < existing.firstTs) existing.firstTs = ts;
            if (ts > existing.lastTs) existing.lastTs = ts;
            dateMap.set(dateStr, existing);
        }

        // Sort descending (latest date on top)
        const dates = Array.from(dateMap.values()).sort((a, b) => b.date.localeCompare(a.date));

        res.json({
            status: "SUCCESS",
            agentId,
            totalDates: dates.length,
            dates
        });
    } catch (err) {
        console.error(`[ERROR] /api/history-dates failed:`, err.message);
        res.status(500).json({ error: err.message, dates: [] });
    }
});

// Endpoint: 5-Layer Telemetry Validation & Tiered Ingest
app.post("/api/telemetry-validate-ingest", async (req, res) => {
    const { ping, saveToDb = false } = req.body;
    if (!ping) return res.status(400).json({ error: "MISSING_TELEMETRY_PAYLOAD" });

    try {
        const agentId = await resolveCanonicalAgentId(ping.agentId || ping.deviceId);
        
        // Layer 1: Security & Identity
        if (!agentId || agentId.length < 3) {
            return res.status(400).json({ valid: false, layer: 1, error: "LAYER1_ERR: Missing or invalid AgentID." });
        }
        if (ping.isMockGps === true || ping.is_mock_gps === true) {
            return res.status(403).json({ valid: false, layer: 1, error: "LAYER1_ERR: Mock GPS detected. Quarantined." });
        }

        // Layer 2: Temporal Monotonicity & Clock Drift
        let ts = parseTimestampToMillis(ping.timestamp || ping.time || Date.now());
        const now = Date.now();
        if (ts > now + 300000) {
            return res.status(400).json({ valid: false, layer: 2, error: "LAYER2_ERR: Future timestamp drift rejected." });
        }

        // Layer 3: Spatial Kinematics Sanity
        const lat = parseFloat(ping.latitude ?? ping.lat ?? 0);
        const lng = parseFloat(ping.longitude ?? ping.lng ?? 0);
        if (isNaN(lat) || isNaN(lng) || (lat === 0 && lng === 0) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return res.status(400).json({ valid: false, layer: 3, error: "LAYER3_ERR: Invalid spatial coordinates." });
        }

        // Layer 4: Redundancy & Stationary Deadband Check
        const devSnap = await db.ref(`devices/${agentId}/device/live`).once("value");
        const livePrev = devSnap.val() || {};
        const prevLat = parseFloat(livePrev.latitude || 0);
        const prevLng = parseFloat(livePrev.longitude || 0);

        let isStationary = false;
        if (prevLat !== 0 && prevLng !== 0) {
            const distM = haversineDistMeters(prevLat, prevLng, lat, lng);
            const speedKmh = parseFloat(ping.speed || 0) * (ping.speed > 50 ? 1 : 3.6);
            if (distM < 15 && speedKmh < 1.5) {
                isStationary = true;
            }
        }

        // Layer 5: State & Tiered Storage Update
        if (saveToDb) {
            // Update HOT store (1-doc live state)
            const liveUpdate = {
                latitude: lat,
                longitude: lng,
                speed: parseFloat(ping.speed || 0),
                bearing: parseFloat(ping.bearing || 0),
                altitude: parseFloat(ping.altitude || 0),
                accuracy: parseFloat(ping.accuracy || 0),
                timestamp: ts,
                updated_at: new Date(ts).toISOString()
            };
            await db.ref(`devices/${agentId}/device/live`).update(liveUpdate);

            // If not stationary, append to WARM history
            if (!isStationary) {
                const dateKey = new Date(ts).toISOString().slice(0, 10);
                await db.ref(`History/${agentId}/history/${ts}`).set({
                    ...liveUpdate,
                    date: dateKey
                });
            }
        }

        res.json({
            valid: true,
            agentId,
            isStationary,
            action: isStationary ? "HOT_STATE_UPDATED_ONLY (Stationary Noise Suppressed)" : "HOT_AND_WARM_PERSISTED",
            timestamp: ts
        });
    } catch (err) {
        console.error("[ERROR] /api/telemetry-validate-ingest failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Endpoint: Prune Old Telemetry History (Retention Policy)
app.post("/api/history-purge", async (req, res) => {
    const { agentId, beforeDate, daysOlderThan } = req.body;
    if (!agentId) return res.status(400).json({ error: "MISSING_AGENT_ID" });

    try {
        const cleanAgentId = await resolveCanonicalAgentId(agentId);
        let cutoffEpoch = 0;

        if (beforeDate) {
            cutoffEpoch = new Date(`${beforeDate}T00:00:00`).getTime();
        } else if (daysOlderThan) {
            cutoffEpoch = Date.now() - (parseInt(daysOlderThan, 10) * 86400000);
        } else {
            return res.status(400).json({ error: "SPECIFY_BEFORE_DATE_OR_DAYS" });
        }

        const histSnap = await db.ref(`History/${cleanAgentId}/history`).once("value");
        const histData = histSnap.val() || {};
        let purgedCount = 0;

        const updates = {};
        for (const [key, item] of Object.entries(histData)) {
            const ts = parseTimestampToMillis(item.timestamp || item.time || key);
            if (ts > 0 && ts < cutoffEpoch) {
                updates[key] = null;
                purgedCount++;
            }
        }

        if (purgedCount > 0) {
            await db.ref(`History/${cleanAgentId}/history`).update(updates);
        }

        res.json({
            status: "SUCCESS",
            agentId: cleanAgentId,
            cutoffDate: new Date(cutoffEpoch).toISOString(),
            purgedCount
        });
    } catch (err) {
        console.error("[ERROR] /api/history-purge failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// 🛡️ Helper: Resolves any raw ID (e.g. 28-char Auth UID) to canonical 8-character Agent ID
async function resolveCanonicalAgentId(rawId) {
    if (!rawId) return rawId;
    const cleanId = String(rawId).trim();
    if (/^[A-Z0-9]{8}$/.test(cleanId)) {
        return cleanId;
    }
    // Attempt lookup index resolution
    try {
        const indexSnap = await db.ref(`lookup_indices/auth_to_device/${cleanId}`).once("value");
        const indexed = indexSnap.val();
        if (indexed && /^[A-Z0-9]{8}$/.test(indexed)) {
            return indexed;
        }
        // Attempt search under Identity/auth_uid
        const querySnap = await db.ref("devices").orderByChild("Identity/auth_uid").equalTo(cleanId).limitToFirst(1).once("value");
        if (querySnap.exists()) {
            const matchedKey = Object.keys(querySnap.val())[0];
            if (matchedKey && /^[A-Z0-9]{8}$/.test(matchedKey)) {
                return matchedKey;
            }
        }
    } catch (e) {
        console.warn(`[RESOLVER] Error resolving agentId for [${cleanId}]:`, e.message);
    }
    return cleanId;
}

// Atomic Profile & Avatar Update with Forensic Ledger
app.post("/api/update-profile", async (req, res) => {
    try {
        const { agentId, profileData } = req.body || {};
        if (!agentId || !profileData) {
            return res.status(400).json({ error: "MISSING_PARAMS", message: "agentId and profileData are required." });
        }

        const nowTs = Date.now();
        const nowIso = new Date().toISOString();
        const timestamp16 = nowIso.replace(/[-:T.Z]/g, '').slice(0, 16);
        const logId = `${nowTs}_PROFILE_${agentId}`;

        const updates = {};
        
        // Profile fields
        if (profileData.name !== undefined) updates[`devices/${agentId}/profile/name`] = profileData.name;
        if (profileData.avatar_id !== undefined) updates[`devices/${agentId}/profile/avatar_id`] = profileData.avatar_id;
        if (profileData.phone !== undefined) updates[`devices/${agentId}/profile/phone`] = profileData.phone;
        if (profileData.department !== undefined) updates[`devices/${agentId}/profile/department`] = profileData.department;
        if (profileData.org_name !== undefined) updates[`devices/${agentId}/profile/org_name`] = profileData.org_name;
        if (profileData.emergency_note !== undefined) updates[`devices/${agentId}/profile/emergency_note`] = profileData.emergency_note;
        if (profileData.address !== undefined) updates[`devices/${agentId}/profile/address`] = profileData.address;
        updates[`devices/${agentId}/profile/updated_at`] = timestamp16;

        // Change Log entry
        updates[`devices/${agentId}/Change_Log/Profile/${logId}`] = {
            log_id: logId,
            category: "Profile",
            action: "DOSSIER_UPDATE_WEB",
            entity_id: agentId,
            changed_by: "WEB_ADMIN_OPERATOR",
            timestamp: timestamp16,
            epoch_ms: nowTs,
            metadata: profileData
        };

        await db.ref().update(updates);
        LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };

        console.log(`[PROFILE_SENTINEL] ✅ Successfully updated profile for ${agentId}.`);
        return res.json({ success: true, message: `Profile updated for ${agentId}`, updated_at: timestamp16 });
    } catch (err) {
        console.error("[ERROR] /api/update-profile failed:", err.message);
        return res.status(500).json({ error: "UPDATE_PROFILE_FAILED", message: err.message });
    }
});

// Dynamic Reverse Geocoding Helper with In-Memory Cache (Unified SSOT)
const GEO_CACHE = new Map();
async function reverseGeocodeLookup(lat, lng) {
    if (isNaN(lat) || isNaN(lng) || (lat === 0 && lng === 0)) {
        return `${Number(lat).toFixed(5)}°, ${Number(lng).toFixed(5)}°`;
    }

    const key = `${lat.toFixed(4)},${lng.toFixed(4)}`;
    if (GEO_CACHE.has(key)) {
        return GEO_CACHE.get(key);
    }

    return new Promise((resolve) => {
        const https = require("https");
        const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`;
        const reqOpts = {
            headers: {
                'User-Agent': 'jFindMe-Enterprise-Radar/2.0 (admin@jfindme.internal)'
            }
        };

        https.get(url, reqOpts, (geoRes) => {
            let data = '';
            geoRes.on('data', chunk => data += chunk);
            geoRes.on('end', () => {
                try {
                    const parsed = JSON.parse(data);
                    const displayName = parsed.display_name || `${lat.toFixed(5)}°, ${lng.toFixed(5)}°`;
                    if (GEO_CACHE.size > 1000) GEO_CACHE.clear();
                    GEO_CACHE.set(key, displayName);
                    resolve(displayName);
                } catch (e) {
                    resolve(`${lat.toFixed(5)}°, ${lng.toFixed(5)}°`);
                }
            });
        }).on('error', () => {
            resolve(`${lat.toFixed(5)}°, ${lng.toFixed(5)}°`);
        });
    });
}

app.get("/api/reverse-geocode", async (req, res) => {
    const lat = parseFloat(req.query.lat);
    const lng = parseFloat(req.query.lng);

    if (isNaN(lat) || isNaN(lng) || (lat === 0 && lng === 0)) {
        return res.status(400).json({ error: "INVALID_COORDINATES" });
    }

    const key = `${lat.toFixed(4)},${lng.toFixed(4)}`;
    const isCached = GEO_CACHE.has(key);
    const address = await reverseGeocodeLookup(lat, lng);
    res.json({ status: isCached ? "CACHED" : "SUCCESS", address });
});


app.get("/api/ping", (req, res) => res.json({ status: "ALIVE", project: PROJECT_ID }));

app.get("/api/discovery", async (req, res) => {
    console.log("[DEBUG] Multi-Cloud Discovery Probe Initiated...");
    try {
        // 1. Define ENTERPRISE_SCHEMA_REGISTRY (Unified 4-Node Cloud Architecture)
        const SCHEMA_REGISTRY = {
            rtdb: ["devices", "lookup_indices", "pairing_requests", "History"],
            firestore: ["users", "avatars", "sessions", "cells", "users_metadata"]
        };

        // 2. Fetch RTDB Root Nodes (Shallow Live Probe)
        const https = require("https");
        const baseUrl = DATABASE_URL.endsWith("/") ? DATABASE_URL.slice(0, -1) : DATABASE_URL;
        const rtdbUrl = `${baseUrl}/.json?shallow=true`;
        
        let liveRtdbNodes = [];
        await new Promise((resolve) => {
            https.get(rtdbUrl, (resp) => {
                let data = '';
                resp.on('data', (chunk) => { data += chunk; });
                resp.on('end', () => {
                    try {
                        const parsed = JSON.parse(data) || {};
                        liveRtdbNodes = Object.keys(parsed).filter(k => k !== 'null');
                        resolve();
                    } catch (e) { resolve(); }
                });
            }).on('error', () => resolve());
        });

        // 3. Fetch Firestore Collections (Live Probe)
        let liveFirestoreDocs = [];
        try {
            const collections = await fs_db.listCollections();
            liveFirestoreDocs = collections.map(c => c.id);
        } catch (e) {
            console.warn("[WARN] Firestore Discovery Restricted:", e.message);
        }

        // 4. MERGE LIVE DATA WITH SCHEMA REGISTRY (Ensure Published Structure is ALWAYS visible)
        const finalRtdb = [...new Set([...SCHEMA_REGISTRY.rtdb, ...liveRtdbNodes])];
        const finalFirestore = [...new Set([...SCHEMA_REGISTRY.firestore, ...liveFirestoreDocs])];

        console.log(`[DEBUG] Discovery Success. Published Nodes: [RTDB: ${finalRtdb.length} | FS: ${finalFirestore.length}]`);
        
        res.json({
            rtdb: finalRtdb,
            firestore: finalFirestore,
            projectId: PROJECT_ID,
            databaseURL: DATABASE_URL
        });
    } catch (err) {
        console.error("[ERROR] Discovery Failure:", err.message);
        res.status(500).json({ error: err.message });
    }
});

app.get("/api/data", async (req, res) => {
    const node = req.query.path || "/";
    const limit = parseInt(req.query.limit) || 2000;
    const startAt = req.query.startAt;
    const type = req.query.type || "rtdb"; // Default to RTDB
    
    const startTime = Date.now();
    console.log(`[NETWORK] SYNC_START: Source [${type}] | Node [${node}] | Limit: ${limit}`);
    
    try {
        if (type === "firestore") {
            try {
                let query = fs_db.collection(node);
                if (startAt) {
                    // For Firestore, we assume startAt is a document ID
                    const doc = await fs_db.collection(node).doc(startAt).get();
                    if (doc.exists) query = query.startAfter(doc);
                }
                const snapshot = await query.limit(limit).get();
                const data = {};
                snapshot.forEach(doc => { data[doc.id] = doc.data(); });
                
                console.log(`[NETWORK] FS_SYNC_COMPLETE: Collection [${node}] | Size: ${snapshot.size}`);
                return res.json(data);
            } catch (fsErr) {
                console.warn(`[WARN] Firestore query for [${node}] failed:`, fsErr.message);
                return res.json({});
            }
        } else {
            let query = db.ref(node).orderByKey();
            if (startAt) query = query.startAt(startAt);
            
            const snapshot = await query.limitToFirst(limit).once("value");
            const data = snapshot.val() || {};
            const duration = Date.now() - startTime;
            
            console.log(`[NETWORK] RTDB_SYNC_COMPLETE: Node [${node}] | Size: ${Object.keys(data).length} | Latency: ${duration}ms`);
            res.json(data);
        }
    } catch (err) {
        console.error(`[ERROR] Data Fetch Failure (${node}):`, err.message);
        res.status(500).json({ error: err.message });
    }
});

app.get("/api/stats", async (req, res) => {
    const node = req.query.path || "/";
    const type = req.query.type || "rtdb";
    console.log(`[DEBUG] Audit Stats Probe: ${node} [${type}]`);
    
    try {
        if (type === "firestore") {
            // Note: firestore.collection().count() requires Firebase Admin v11.1.0+ and pays for indexing
            // For simplicity and compatibility, we'll try a basic approach or return estimated
            const snapshot = await fs_db.collection(node).select().get();
            res.json({ node, total_records: snapshot.size });
        } else {
            const https = require("https");
            const baseUrl = DATABASE_URL.endsWith("/") ? DATABASE_URL.slice(0, -1) : DATABASE_URL;
            const nodePath = node.startsWith("/") ? node : `/${node}`;
            const url = `${baseUrl}${nodePath}.json?shallow=true`;
            
            https.get(url, (resp) => {
                let data = '';
                resp.on('data', (chunk) => { data += chunk; });
                resp.on('end', () => {
                    try {
                        const parsed = JSON.parse(data) || {};
                        const count = Object.keys(parsed).length;
                        res.json({ node, total_records: count });
                    } catch (parseErr) {
                        res.status(500).json({ error: "STATS_PARSE_FAILURE" });
                    }
                });
            }).on('error', (httpErr) => {
                res.status(500).json({ error: httpErr.message });
            });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ============================================================
// UPDATE / SET / DELETE RECORD ENDPOINT
// ============================================================
app.post("/api/update", async (req, res) => {
    const { path: targetPath, data, type } = req.body;
    const sourceType = type || "rtdb";
    console.log(`[ACTION] UPDATE_REQUEST: Source [${sourceType}] | Path [${targetPath}]`);

    try {
        if (!targetPath) {
            return res.status(400).json({ error: "MISSING_PATH" });
        }

        if (sourceType === "firestore") {
            const parts = targetPath.replace(/^\/+/, "").split("/").filter(Boolean);
            if (parts.length < 2) throw new Error("INVALID_FIRESTORE_PATH: Expected collection/docId");
            const docRef = fs_db.collection(parts[0]).doc(parts[1]);
            
            if (data === null) {
                await docRef.delete();
                console.log(`[SUCCESS] Firestore Doc deleted: ${targetPath}`);
            } else {
                await docRef.set(data, { merge: true });
                console.log(`[SUCCESS] Firestore Doc updated: ${targetPath}`);
            }
        } else {
            const sanitizedPath = targetPath.replace(/^\/+/, "");
            if (data === null) {
                await db.ref(sanitizedPath).remove();
                console.log(`[SUCCESS] RTDB Node removed: ${sanitizedPath}`);
            } else {
                if (typeof data === "object" && data !== null && !Array.isArray(data)) {
                    await db.ref(sanitizedPath).update(data);
                } else {
                    await db.ref(sanitizedPath).set(data);
                }
                console.log(`[SUCCESS] RTDB Node updated: ${sanitizedPath}`);
            }
        }
        res.json({ status: "SUCCESS", path: targetPath });
    } catch (err) {
        console.error(`[ERROR] Update Failure (${targetPath}):`, err.message);
        res.status(500).json({ error: err.message });
    }
});

app.post("/api/delete", async (req, res) => {
    const { path, type } = req.body;
    const sourceType = type || "rtdb";
    console.log(`[ACTION] DECOMMISSION_REQUEST: Source [${sourceType}] | Path [${path}]`);
    
    try {
        if (sourceType === "firestore") {
            const parts = path.split("/").filter(Boolean);
            if (parts.length < 2) throw new Error("INVALID_FIRESTORE_PATH");
            await fs_db.collection(parts[0]).doc(parts[1]).delete();
        } else {
            await db.ref(path).remove();
        }
        console.log(`[SUCCESS] Node purged: ${path}`);
        res.json({ status: "DECOMMISSIONED", path });
    } catch (err) {
        console.error(`[ERROR] Decommission Failure (${path}):`, err.message);
        res.status(500).json({ error: err.message });
    }
});


// ============================================================
// NUCLEAR PURGE: Complete Erasure of an Entire Root-Level Node
// ============================================================
app.post("/api/purge-node", async (req, res) => {
    const { node, type } = req.body;
    const sourceType = type || "rtdb";

    if (!node || node === "/" || node.trim() === "") {
        console.error("[BLOCKED] Attempted root-level database wipe — DENIED.");
        return res.status(400).json({ error: "ROOT_WIPE_BLOCKED: Cannot purge the entire database root." });
    }

    const sanitizedNode = node.replace(/^\/+/, ""); // Strip leading slashes
    console.log(`\n${"=".repeat(60)}`);
    console.log(`[NUCLEAR_PURGE] INITIATED for node: /${sanitizedNode} [${sourceType}]`);
    console.log(`${"=".repeat(60)}`);

    try {
        let childCount = 0;
        let totalRecords = 0;

        if (sourceType === "firestore") {
            const snapshot = await fs_db.collection(sanitizedNode).get();
            childCount = snapshot.size;
            totalRecords = snapshot.size;

            // Batch Delete
            const batch = fs_db.batch();
            snapshot.forEach(doc => batch.delete(doc.ref));
            await batch.commit();
        } else {
            // RTDB Census
            const snapshot = await db.ref(sanitizedNode).once("value");
            const data = snapshot.val();
            childCount = data ? Object.keys(data).length : 0;
            if (data) {
                Object.values(data).forEach(child => {
                    totalRecords += (child && typeof child === "object") ? Object.keys(child).length : 1;
                });
            }
            // RTDB Purge
            await db.ref(sanitizedNode).remove();
        }

        console.log(`[SUCCESS] /${sanitizedNode} — FULLY PURGED.`);
        console.log(`${"=".repeat(60)}\n`);

        res.json({
            status: "PURGED",
            node: sanitizedNode,
            purgedChildren: childCount,
            purgedRecordsEstimate: totalRecords,
            timestamp: new Date().toISOString()
        });
    } catch (err) {
        console.error(`[FATAL] Purge Failure (${sanitizedNode}):`, err.message);
        res.status(500).json({ error: err.message });
    }
});

// ============================================================
// OOPS MVC DEVICE AUDIT & DIAGNOSTIC ENDPOINT
// ============================================================
const handleDeviceAudit = async (req, res) => {
    try {
        const target = req.params.agentId;
        const devSnap = await db.ref("devices").once("value");
        const devices = devSnap.val() || {};

        const results = [];
        const processDev = async (id, dev) => {
            const histSnap = await db.ref(`History/${id}`).once("value");
            const hist = histSnap.val() || {};
            const monitorSnap = await db.ref(`lookup_indices/active_monitors/${id}`).once("value");

            results.push({
                agentId: id,
                name: dev.profile?.name || dev.Identity?.owner_name || dev.device?.live?.userName || id,
                model: dev.device?.model || "Generic",
                telemetry: {
                    status: dev.telemetry?.status || "UNKNOWN",
                    battery_pct: dev.telemetry?.battery_pct ?? -1,
                    is_charging: dev.telemetry?.is_charging ?? false,
                    last_seen: dev.telemetry?.last_seen || null,
                    last_heartbeat: dev.telemetry?.last_heartbeat || null,
                    monitor_heartbeat: monitorSnap.val() || null
                },
                live: {
                    latitude: dev.device?.live?.latitude || 0,
                    longitude: dev.device?.live?.longitude || 0,
                    address: dev.device?.live?.address || "N/A",
                    updated_at: dev.device?.live?.updated_at || null,
                    safe_zone_state: dev.device?.live?.current_safe_zone_state || "OUT",
                    safe_zone_name: dev.device?.live?.current_safe_zone_name || "None"
                },
                safe_zones: Object.keys(dev.safe_zones || {}).map(k => ({
                    zoneId: k,
                    name: dev.safe_zones[k].zone_name || k,
                    state: dev.safe_zones[k].current_state || "UNKNOWN",
                    alerts: dev.safe_zones[k].alerts_enabled ?? true
                })),
                archive_count: hist.history ? Object.keys(hist.history).length : 0
            });
        };

        if (target && devices[target]) {
            await processDev(target, devices[target]);
        } else {
            for (const [id, dev] of Object.entries(devices)) {
                if (id.length <= 16) {
                    await processDev(id, dev);
                }
            }
        }

        res.json({ success: true, timestamp: new Date().toISOString(), count: results.length, devices: results });
    } catch (err) {
        res.status(500).json({ success: false, error: err.message });
    }
};

app.get("/api/device-audit", handleDeviceAudit);
app.get("/api/device-audit/:agentId", handleDeviceAudit);
// REAL-TIME GEOFENCE FCM PUSH DISPATCHER & CANONICAL BRIDGE
// ============================================================
function initRealtimeSentinelBridge() {
    console.log("[SENTINEL_BRIDGE] Initializing Geofence Notification & Push Dispatcher...");

    // 1. Listen for new geofence notifications across all groups
    db.ref("relationships").on("child_added", (groupSnap) => {
        const groupId = groupSnap.key;
        if (!groupId) return;

        db.ref(`relationships/${groupId}/notifications`).limitToLast(1).on("child_added", async (notifSnap) => {
            const notif = notifSnap.val();
            if (!notif || !notif.timestamp) return;

            // Only process recent notifications (< 5 mins old)
            if (Date.now() - Number(notif.timestamp) > 5 * 60 * 1000) return;

            const avatarName = notif.avatar_name || notif.target_name || "Device";
            const zoneName = notif.zone_name || "Safe Zone";
            const status = notif.status || notif.transition || "ENTERED";
            const senderUid = notif.uid || notif.target_uid;

            console.log(`[SENTINEL_BRIDGE] 🚨 Geofence Transition Detected: ${avatarName} ${status} [${zoneName}]`);

            try {
                // Fetch all registered devices to broadcast push notification
                const devSnap = await db.ref("devices").once("value");
                const devices = devSnap.val() || {};
                const tokens = [];

                for (const [id, dev] of Object.entries(devices)) {
                    // Skip sender device
                    if (id === senderUid || (dev.Identity && dev.Identity.auth_uid === senderUid)) continue;

                    const token = (dev.Identity && dev.Identity.fcm_token) || (dev.device && dev.device.fcm_token);
                    if (token && typeof token === "string" && token.length > 20 && !tokens.includes(token)) {
                        tokens.push(token);
                    }
                }

                if (tokens.length > 0) {
                    const title = `🛡️ Safe Zone ${status === 'ENTERED' ? 'Entry' : 'Exit'} Alert`;
                    const body = `${avatarName} has ${status === 'ENTERED' ? 'entered' : 'exited'} the ${zoneName} Safe Zone.`;

                    const message = {
                        tokens: tokens,
                        notification: { title, body },
                        data: {
                            type: status === "ENTERED" || status === "IN" ? "GEOFENCE_ENTER" : "GEOFENCE_EXIT",
                            title: title,
                            body: body,
                            zone_name: zoneName,
                            avatar_name: avatarName,
                            timestamp: String(notif.timestamp)
                        },
                        android: {
                            priority: "high",
                            notification: {
                                channelId: "geofence_channel",
                                priority: "high",
                                sound: "default"
                            }
                        }
                    };

                    const fcmRes = await admin.messaging().sendEachForMulticast(message);
                    console.log(`[FCM_DISPATCHER] 📲 Push Notification sent to ${fcmRes.successCount}/${tokens.length} connected devices.`);
                }
            } catch (fcmErr) {
                console.error("[FCM_DISPATCHER] Error dispatching push notification:", fcmErr.message);
            }
        });
    });

    // 2. Real-time Canonical Bridge: Mirror Auth UID device/live updates to 8-character device ID
    db.ref("devices").on("child_changed", async (snap) => {
        const key = snap.key;
        if (!key || key.length <= 16) return; // Only process long Auth UIDs

        try {
            const lookupSnap = await db.ref(`lookup_indices/auth_to_device/${key}`).once("value");
            const targetAgentId = lookupSnap.val();
            if (!targetAgentId) return;

            const authNodeData = snap.val() || {};
            const liveData = (authNodeData.device && authNodeData.device.live) || authNodeData.live;
            if (!liveData) return;

            console.log(`[CANONICAL_BRIDGE] Syncing Auth UID [${key}] -> Canonical Device [${targetAgentId}]...`);

            // If entering safe zone, resolve coordinates
            if (liveData.current_safe_zone_state === "IN" && liveData.current_safe_zone_id) {
                const zoneSnap = await db.ref(`Geofencing`).once("value");
                const geofences = zoneSnap.val() || {};
                for (const creator of Object.values(geofences)) {
                    if (creator.safe_zones && creator.safe_zones[liveData.current_safe_zone_id]) {
                        const z = creator.safe_zones[liveData.current_safe_zone_id];
                        if (z.lat && z.lng) {
                            liveData.latitude = parseFloat(z.lat);
                            liveData.longitude = parseFloat(z.lng);
                            liveData.address = `Inside ${z.zone_name || 'Safe Zone'} Safe Perimeter`;
                        }
                        break;
                    }
                }
            }

            await db.ref(`devices/${targetAgentId}/device/live`).update(liveData);
            console.log(`[CANONICAL_BRIDGE] ✅ Successfully updated devices/${targetAgentId}/device/live.`);
        } catch (bridgeErr) {
            console.error("[CANONICAL_BRIDGE] Sync failed:", bridgeErr.message);
        }
    });
}

// =========================================================================
// 🔄 DUPLICATE DEVICE SENTINEL: SILICON HARDWARE FINGERPRINT & MERGE ENGINE
// =========================================================================

/**
 * Computes deterministic physical silicon SHA-256 hardware hash matching DeviceIdentityHelper
 */
function computeSiliconHash(rawNode) {
    if (!rawNode) return null;
    const dev = rawNode.device || {};
    const profile = rawNode.profile || {};
    const identity = rawNode.Identity || {};

    // 1. Direct explicit silicon hash
    if (dev.silicon_sha256 && typeof dev.silicon_sha256 === 'string' && dev.silicon_sha256.length >= 8) {
        return dev.silicon_sha256.trim().toUpperCase();
    }

    const fp = (dev.fingerprint || dev.build_fingerprint || '').trim();
    const fpParts = fp ? fp.split('/') : [];
    const fpDevicePart = (fpParts.length >= 3) ? fpParts[2].split(':')[0] : '';

    const brand = (dev.brand || profile.brand || (fpParts.length > 0 ? fpParts[0] : '')).trim().toUpperCase();
    const model = (dev.model || dev.deviceName || profile.model || (fpParts.length > 1 ? fpParts[1] : '')).trim().toUpperCase();
    const board = (dev.board || fpDevicePart || '').trim().toUpperCase();
    const hardware = (dev.hardware || (fp.toLowerCase().includes('qcom') ? 'QCOM' : (fp ? 'ARM64' : ''))).trim().toUpperCase();
    const device = (dev.device || fpDevicePart || (dev.deviceId ? dev.deviceId.replace(/^HW_/, '') : '')).trim().toUpperCase();
    const imei = (dev.imei || '').trim();

    let baseSeed = '';
    if (brand && model) {
        baseSeed = `${brand}_${model}_${board}_${hardware}_${device}`;
    } else if (fp) {
        baseSeed = fp;
    } else if (imei && imei.length >= 10) {
        baseSeed = `IMEI_${imei}`;
    } else if (dev.deviceName && (identity.auth_uid || profile.phone)) {
        baseSeed = `${dev.deviceName}_${identity.auth_uid || profile.phone}`;
    }

    if (!baseSeed) return null;

    try {
        return crypto.createHash('sha256').update(baseSeed).digest('hex').toUpperCase();
    } catch (e) {
        return null;
    }
}

/**
 * Master Headings Synchronization & Comparison Engine (OOPS-based SSOT)
 * Analyzes all headings of the old device vs the new device's Old_Device_ID archive.
 * Both sides tick yes ([✔]) if the heading is synchronized under Old_Device_ID.
 */
function buildHeadingsComparison(oldId, newId, oldNode = {}, newNode = {}, rawHistory = {}, rawGeofencing = {}) {
    const archiveNode = (newNode?.Old_Device_ID && newNode.Old_Device_ID[oldId]) ? newNode.Old_Device_ID[oldId] : {};

    const KNOWN_DEFINITIONS = [
        { key: 'profile', label: 'Profile Dossier', icon: '👤', desc: 'Name, avatar, contact phone, home address, roles' },
        { key: 'device', label: 'Device & Hardware Telemetry', icon: '📱', desc: 'Hardware specs, live GPS coordinate, model, brand, fingerprint' },
        { key: 'telemetry', label: 'Telemetry & Battery', icon: '⚡', desc: 'Battery %, charging status, online session, heartbeat' },
        { key: 'Identity', label: 'Identity & Auth Keys', icon: '🔑', desc: 'Canonical Agent IDs, auth UID, FCM token, node index' },
        { key: 'history', label: 'Device History Breadcrumbs', icon: '📍', desc: 'Internal location breadcrumb points recorded on device' },
        { key: 'History', label: 'Trajectory GPS Stream', icon: '🗺️', desc: 'Historical GPS points from /History/{id}/history' },
        { key: 'relationships', label: 'Circle & Family Peers', icon: '👥', desc: 'Family circles, peer devices, and guardian links' },
        { key: 'safe_zones', label: 'Safe Zones & Geofences', icon: '🛡️', desc: 'Assigned safe zones, geofence boundaries, arrival alerts' },
        { key: 'Access', label: 'Hardware Access Permissions', icon: '🔒', desc: 'GPS, camera, bluetooth, foreground service ACL grants' },
        { key: 'Change_Log', label: 'Change Log & Forensic Audit', icon: '📜', desc: 'Forensic audit entries, pairing records, system updates' },
        { key: 'commands', label: 'Remote Push Commands', icon: '📡', desc: 'Poke location request, remote ring/siren, SOS alerts' },
        { key: 'alerts', label: 'Emergency Alerts & SOS', icon: '🚨', desc: 'SOS emergency alerts, battery warnings, zone transitions' },
        { key: 'last_sync', label: 'Last Sync Timestamp', icon: '⏱️', desc: 'Epoch and readable timestamp of synchronization' },
        { key: 'acl', label: 'ACL Security Controls', icon: '🛡️', desc: 'Access Control Lists (live tracking permissions)' },
        { key: 'live_tracking_enabled', label: 'Live Tracking State', icon: '🟢', desc: 'Live tracking toggle state' },
        { key: '_merge_metadata', label: 'Merge Forensic Receipt', icon: '🏷️', desc: 'Migration receipt, old/new IDs, timestamp' }
    ];

    const allKeys = new Set(KNOWN_DEFINITIONS.map(d => d.key));
    if (oldNode && typeof oldNode === 'object') {
        Object.keys(oldNode).forEach(k => {
            if (k !== 'Old_Device_ID') allKeys.add(k);
        });
    }
    if (archiveNode && typeof archiveNode === 'object') {
        Object.keys(archiveNode).forEach(k => allKeys.add(k));
    }

    const headingList = [];

    for (const key of allKeys) {
        const def = KNOWN_DEFINITIONS.find(d => d.key === key) || {
            key,
            label: key.charAt(0).toUpperCase() + key.slice(1).replace(/_/g, ' '),
            icon: '📁',
            desc: `Custom device heading [${key}]`
        };

        // 1. Resolve Old Device state
        let oldVal = oldNode ? oldNode[key] : undefined;
        if (oldVal === undefined && archiveNode[key] !== undefined) {
            oldVal = archiveNode[key];
        }
        if (key === 'History' && (oldVal === undefined || !oldVal)) {
            const histRoot = rawHistory?.[oldId]?.history || rawHistory?.[newId]?.history;
            if (histRoot && Object.keys(histRoot).length > 0) {
                oldVal = histRoot;
            }
        }
        if (key === 'safe_zones' && (oldVal === undefined || !oldVal)) {
            const geoDev = rawGeofencing?.[oldId]?.safe_zones || rawGeofencing?.[newId]?.safe_zones;
            if (geoDev && Object.keys(geoDev).length > 0) {
                oldVal = geoDev;
            }
        }

        const oldExists = oldVal !== undefined && oldVal !== null;
        let oldSubkeys = [];
        let oldRecordsCount = 0;
        let oldSample = '';

        if (oldExists) {
            if (typeof oldVal === 'object' && !Array.isArray(oldVal)) {
                oldSubkeys = Object.keys(oldVal);
                oldRecordsCount = oldSubkeys.length;
                oldSample = oldSubkeys.slice(0, 8).join(', ') + (oldSubkeys.length > 8 ? '...' : '');
            } else if (Array.isArray(oldVal)) {
                oldRecordsCount = oldVal.length;
                oldSample = `Array(${oldVal.length})`;
            } else {
                oldRecordsCount = 1;
                oldSample = String(oldVal);
            }
        }

        // 2. Resolve Old_Device_ID state under New Device
        let syncVal = archiveNode ? archiveNode[key] : undefined;
        // Check if History was merged into target History root
        if (key === 'History' && (syncVal === undefined || !syncVal)) {
            if (archiveNode.History) {
                syncVal = archiveNode.History;
            } else if (rawHistory?.[newId]?.history) {
                const count = Object.values(rawHistory[newId].history).filter(h => h.origin_agent_id === oldId).length;
                if (count > 0) syncVal = { origin_points: count, target_root: `History/${newId}/history` };
            }
        }
        // Check if safe_zones was linked
        if (key === 'safe_zones' && (syncVal === undefined || !syncVal)) {
            if (archiveNode.safe_zones) {
                syncVal = archiveNode.safe_zones;
            } else if (rawGeofencing?.[newId]?.safe_zones) {
                syncVal = rawGeofencing[newId].safe_zones;
            }
        }

        const syncedExists = syncVal !== undefined && syncVal !== null;
        let syncedSubkeys = [];
        let syncedRecordsCount = 0;
        let syncedSample = '';

        if (syncedExists) {
            if (typeof syncVal === 'object' && !Array.isArray(syncVal)) {
                syncedSubkeys = Object.keys(syncVal);
                syncedRecordsCount = syncedSubkeys.length;
                syncedSample = syncedSubkeys.slice(0, 8).join(', ') + (syncedSubkeys.length > 8 ? '...' : '');
            } else if (Array.isArray(syncVal)) {
                syncedRecordsCount = syncVal.length;
                syncedSample = `Array(${syncVal.length})`;
            } else {
                syncedRecordsCount = 1;
                syncedSample = String(syncVal);
            }
        }

        // Both side tick yes if they are sync in heading Old_device_Id (under new device)
        const isBothSynced = Boolean(syncedExists);

        headingList.push({
            key,
            label: def.label,
            icon: def.icon,
            desc: def.desc,
            oldExists,
            oldRecordsCount,
            oldSubkeys,
            oldSample,
            syncedExists,
            syncedRecordsCount,
            syncedSubkeys,
            syncedSample,
            isBothSynced,
            oldPreview: oldExists ? (typeof oldVal === 'object' ? JSON.stringify(oldVal, null, 2) : String(oldVal)) : null,
            syncedPreview: syncedExists ? (typeof syncVal === 'object' ? JSON.stringify(syncVal, null, 2) : String(syncVal)) : null
        });
    }

    return {
        headings: headingList,
        totalHeadings: headingList.length,
        syncedHeadings: headingList.filter(h => h.isBothSynced).length,
        missingHeadings: headingList.filter(h => !h.isBothSynced).length,
        isFullySynced: headingList.every(h => h.isBothSynced)
    };
}

/**
 * GET /api/duplicate-devices
 * Scans Firebase RTDB /devices, groups by Silicon SHA-256 / Hardware Fingerprint,
 * returns candidate pairs whose IDs changed after OTA/signature mismatch,
 * AND includes already-moved pairs archived under Old_Device_ID with full headings matrix.
 */
app.get("/api/duplicate-devices", async (req, res) => {
    try {
        const [devSnap, histSnap, geofenceSnap] = await Promise.all([
            db.ref("devices").once("value"),
            db.ref("History").once("value"),
            db.ref("Geofencing").once("value")
        ]);

        if (!devSnap.exists()) {
            return res.json({ duplicates: [], totalCount: 0, scannedAt: Date.now() });
        }

        const rawDevices = devSnap.val() || {};
        const rawHistory = histSnap.exists() ? histSnap.val() : {};
        const rawGeofencing = geofenceSnap.exists() ? geofenceSnap.val() : {};

        // Helper to extract numeric epoch
        function extractLastSeenEpoch(d) {
            if (!d) return 0;
            const liveTs = d.device?.live?.timestamp;
            const telemTs = d.telemetry?.last_seen;
            const idUpdated = d.Identity?.updated_at;
            const profileUpdated = d.profile?.updated_at;
            let maxTs = 0;
            if (typeof liveTs === 'number') maxTs = Math.max(maxTs, liveTs);
            if (typeof telemTs === 'number') maxTs = Math.max(maxTs, telemTs);
            if (typeof idUpdated === 'number') maxTs = Math.max(maxTs, idUpdated);
            if (typeof profileUpdated === 'string' && profileUpdated.length === 16) {
                const yr = parseInt(profileUpdated.substring(0, 4));
                const mo = parseInt(profileUpdated.substring(4, 6)) - 1;
                const dy = parseInt(profileUpdated.substring(6, 8));
                const hr = parseInt(profileUpdated.substring(8, 10));
                const mi = parseInt(profileUpdated.substring(10, 12));
                const sc = parseInt(profileUpdated.substring(12, 14));
                const parsed = new Date(yr, mo, dy, hr, mi, sc).getTime();
                if (!isNaN(parsed)) maxTs = Math.max(maxTs, parsed);
            }
            return maxTs;
        }

        // Shared helper to build standard device summary
        function formatDeviceSummary(agentId, node) {
            const epoch = extractLastSeenEpoch(node);
            const histRecords = rawHistory[agentId]?.history || {};
            const historyCount = Object.keys(histRecords).length + (node.device?.live?.latitude ? 1 : 0);
            const relCount = Object.keys(node.relationships || {}).length;

            let safeZonesCount = Object.keys(node.safe_zones || {}).length;
            for (const creator of Object.values(rawGeofencing)) {
                if (creator.safe_zones) {
                    for (const sz of Object.values(creator.safe_zones)) {
                        if (sz.connected_devices && sz.connected_devices[agentId]) {
                            safeZonesCount++;
                        }
                    }
                }
            }

            const dev = node.device || {};
            const profile = node.profile || {};
            const telemetry = node.telemetry || {};

            const fp = (dev.fingerprint || dev.build_fingerprint || '').trim();
            const fpParts = fp ? fp.split('/') : [];
            const fpDevicePart = (fpParts.length >= 3) ? fpParts[2].split(':')[0] : '';

            const brand = dev.brand || profile.brand || (fpParts.length > 0 ? fpParts[0] : '') || 'Generic';
            const model = dev.model || dev.deviceName || profile.model || (fpParts.length > 1 ? fpParts[1] : '') || 'Unknown';
            const board = dev.board || fpDevicePart || 'N/A';
            const hardware = dev.hardware || (fp.toLowerCase().includes('qcom') ? 'qcom' : (fp ? 'ARM64' : 'N/A'));
            const device = dev.device || fpDevicePart || (dev.deviceId ? dev.deviceId.replace(/^HW_/, '') : agentId);

            // IMEI resolution (dual SIM support: IMEI 1 & IMEI 2)
            const rawImei = (dev.imei || profile.imei || telemetry.imei || '').trim();
            let imei1 = (dev.imei1 || dev.imei_1 || '').trim();
            let imei2 = (dev.imei2 || dev.imei_2 || '').trim();
            if (!imei1 && rawImei) {
                if (rawImei.includes(',')) {
                    const parts = rawImei.split(',').map(s => s.trim());
                    imei1 = parts[0] || 'N/A';
                    imei2 = parts[1] || '';
                } else if (rawImei.includes('/')) {
                    const parts = rawImei.split('/').map(s => s.trim());
                    imei1 = parts[0] || 'N/A';
                    imei2 = parts[1] || '';
                } else {
                    imei1 = rawImei;
                }
            }
            if (!imei1) imei1 = 'N/A';
            const isDualSim = Boolean(imei2 || dev.is_dual_sim || dev.sim_count === 2);

            return {
                agentId,
                name: profile.name || dev.deviceName || agentId,
                deviceName: dev.deviceName || model || 'Android Device',
                brand,
                model,
                board,
                hardware,
                device,
                imei: imei1,
                imei1,
                imei2,
                isDualSim,
                osVersion: dev.os_version || (fpParts.length >= 3 && fpParts[2].includes(':') ? `Android ${fpParts[2].split(':')[1].split('/')[0]}` : 'Android'),
                avatarUrl: profile.avatar_id ? `/Avatar/${profile.avatar_id}.png` : '/Avatar/001.png',
                lastSeenEpoch: epoch,
                lastSeenFormatted: epoch > 0 ? new Date(epoch).toLocaleString() : 'Never Recorded',
                isLive: (Date.now() - epoch) < (15 * 60 * 1000),
                batteryPct: telemetry.battery_pct ?? 100,
                location: dev.live ? {
                    lat: dev.live.latitude,
                    lng: dev.live.longitude,
                    address: dev.live.geo_address || dev.live.address || 'Unknown'
                } : null,
                historyCount,
                relationshipsCount: relCount,
                safeZonesCount,
                hasArchive: !!node.Old_Device_ID,
                raw: node
            };
        }

        // Group devices by Silicon Fingerprint or Secondary Hardware Signature
        const mapByHash = new Map();

        for (const [agentId, node] of Object.entries(rawDevices)) {
            if (!node || typeof node !== 'object') continue;
            
            const hash = computeSiliconHash(node);
            const imei = (node.device?.imei || '').trim();
            const devName = (node.device?.deviceName || node.device?.model || node.profile?.name || '').trim().toLowerCase();
            const phone = (node.profile?.phone || '').trim();
            
            let groupKey = null;
            let matchType = 'SILICON_SHA256';

            if (hash) {
                groupKey = `HASH_${hash}`;
                matchType = 'SILICON_SHA256 (Brand + Model + Board + Hardware + Device)';
            } else if (imei && imei.length >= 12) {
                groupKey = `IMEI_${imei}`;
                matchType = 'IMEI_HARDWARE_KEY';
            } else if (devName && phone) {
                groupKey = `PHONE_NAME_${phone}_${devName}`;
                matchType = 'PROFILE_PHONE_HARDWARE_NAME';
            }

            if (!groupKey) continue;

            if (!mapByHash.has(groupKey)) {
                mapByHash.set(groupKey, { groupKey, matchType, hash: hash || groupKey, items: [] });
            }

            mapByHash.get(groupKey).items.push(formatDeviceSummary(agentId, node));
        }

        const duplicatePairs = [];

        for (const [groupKey, group] of mapByHash.entries()) {
            if (group.items.length >= 2) {
                // Sort items by lastSeenEpoch descending: newest active is index 0, older deprecated is index >= 1
                group.items.sort((a, b) => b.lastSeenEpoch - a.lastSeenEpoch);

                const newDev = group.items[0];
                for (let i = 1; i < group.items.length; i++) {
                    const oldDev = group.items[i];
                    if (oldDev.agentId === newDev.agentId) continue;

                    // Cross-fertilize hardware properties across the pair
                    const unifiedBrand = (newDev.brand && newDev.brand !== 'Generic') ? newDev.brand : (oldDev.brand && oldDev.brand !== 'Generic' ? oldDev.brand : 'Generic');
                    const unifiedModel = (newDev.model && newDev.model !== 'Unknown') ? newDev.model : (oldDev.model && oldDev.model !== 'Unknown' ? oldDev.model : 'Unknown');
                    const unifiedBoard = (newDev.board && newDev.board !== 'N/A') ? newDev.board : (oldDev.board && oldDev.board !== 'N/A' ? oldDev.board : 'N/A');
                    const unifiedHardware = (newDev.hardware && newDev.hardware !== 'N/A') ? newDev.hardware : (oldDev.hardware && oldDev.hardware !== 'N/A' ? oldDev.hardware : 'N/A');
                    const unifiedDevice = (newDev.device && newDev.device !== 'N/A') ? newDev.device : (oldDev.device && oldDev.device !== 'N/A' ? oldDev.device : 'N/A');
                    const unifiedImei1 = (newDev.imei1 && newDev.imei1 !== 'N/A') ? newDev.imei1 : (oldDev.imei1 && oldDev.imei1 !== 'N/A' ? oldDev.imei1 : 'N/A');
                    const unifiedImei2 = newDev.imei2 || oldDev.imei2 || '';
                    const unifiedDualSim = Boolean(newDev.isDualSim || oldDev.isDualSim || unifiedImei2);

                    oldDev.brand = unifiedBrand;
                    newDev.brand = unifiedBrand;
                    oldDev.model = unifiedModel;
                    newDev.model = unifiedModel;
                    oldDev.board = unifiedBoard;
                    newDev.board = unifiedBoard;
                    oldDev.hardware = unifiedHardware;
                    newDev.hardware = unifiedHardware;
                    oldDev.device = unifiedDevice;
                    newDev.device = unifiedDevice;
                    oldDev.imei1 = unifiedImei1;
                    newDev.imei1 = unifiedImei1;
                    oldDev.imei2 = unifiedImei2;
                    newDev.imei2 = unifiedImei2;
                    oldDev.isDualSim = unifiedDualSim;
                    newDev.isDualSim = unifiedDualSim;

                    const headingsComp = buildHeadingsComparison(
                        oldDev.agentId,
                        newDev.agentId,
                        rawDevices[oldDev.agentId] || {},
                        rawDevices[newDev.agentId] || {},
                        rawHistory,
                        rawGeofencing
                    );

                    duplicatePairs.push({
                        pairId: `${oldDev.agentId}_TO_${newDev.agentId}`,
                        matchType: group.matchType,
                        siliconHash: group.hash,
                        isMoved: false,
                        hardwareProperties: {
                            brand: unifiedBrand,
                            model: unifiedModel,
                            board: unifiedBoard,
                            hardware: unifiedHardware,
                            device: unifiedDevice,
                            imei: unifiedImei1,
                            imei1: unifiedImei1,
                            imei2: unifiedImei2,
                            isDualSim: unifiedDualSim
                        },
                        oldDevice: oldDev,
                        newDevice: newDev,
                        headingsComparison: headingsComp
                    });
                }
            }
        }

        // Also discover devices that were already moved and archived under Old_Device_ID
        for (const [agentId, node] of Object.entries(rawDevices)) {
            if (node?.Old_Device_ID && typeof node.Old_Device_ID === 'object') {
                for (const [oldAgentId, oldArchive] of Object.entries(node.Old_Device_ID)) {
                    if (!oldArchive || typeof oldArchive !== 'object') continue;
                    const pairKey = `${oldAgentId}_TO_${agentId}`;
                    if (duplicatePairs.some(p => p.pairId === pairKey)) continue;

                    const oldSummary = formatDeviceSummary(oldAgentId, oldArchive);
                    const newSummary = formatDeviceSummary(agentId, node);

                    const unifiedBrand = (newSummary.brand && newSummary.brand !== 'Generic') ? newSummary.brand : (oldSummary.brand && oldSummary.brand !== 'Generic' ? oldSummary.brand : 'Generic');
                    const unifiedModel = (newSummary.model && newSummary.model !== 'Unknown') ? newSummary.model : (oldSummary.model && oldSummary.model !== 'Unknown' ? oldSummary.model : 'Unknown');
                    const unifiedBoard = (newSummary.board && newSummary.board !== 'N/A') ? newSummary.board : (oldSummary.board && oldSummary.board !== 'N/A' ? oldSummary.board : 'N/A');
                    const unifiedHardware = (newSummary.hardware && newSummary.hardware !== 'N/A') ? newSummary.hardware : (oldSummary.hardware && oldSummary.hardware !== 'N/A' ? oldSummary.hardware : 'N/A');
                    const unifiedDevice = (newSummary.device && newSummary.device !== 'N/A') ? newSummary.device : (oldSummary.device && oldSummary.device !== 'N/A' ? oldSummary.device : 'N/A');
                    const unifiedImei1 = (newSummary.imei1 && newSummary.imei1 !== 'N/A') ? newSummary.imei1 : (oldSummary.imei1 && oldSummary.imei1 !== 'N/A' ? oldSummary.imei1 : 'N/A');
                    const unifiedImei2 = newSummary.imei2 || oldSummary.imei2 || '';
                    const unifiedDualSim = Boolean(newSummary.isDualSim || oldSummary.isDualSim || unifiedImei2);

                    oldSummary.brand = unifiedBrand; newSummary.brand = unifiedBrand;
                    oldSummary.model = unifiedModel; newSummary.model = unifiedModel;
                    oldSummary.board = unifiedBoard; newSummary.board = unifiedBoard;
                    oldSummary.hardware = unifiedHardware; newSummary.hardware = unifiedHardware;
                    oldSummary.device = unifiedDevice; newSummary.device = unifiedDevice;
                    oldSummary.imei1 = unifiedImei1; newSummary.imei1 = unifiedImei1;
                    oldSummary.imei2 = unifiedImei2; newSummary.imei2 = unifiedImei2;
                    oldSummary.isDualSim = unifiedDualSim; newSummary.isDualSim = unifiedDualSim;

                    const headingsComp = buildHeadingsComparison(oldAgentId, agentId, oldArchive, node, rawHistory, rawGeofencing);

                    duplicatePairs.push({
                        pairId: pairKey,
                        matchType: 'MOVED_ACTIVE_ARCHIVE (Old_Device_ID Preserved)',
                        siliconHash: computeSiliconHash(oldArchive) || computeSiliconHash(node) || 'ARCHIVED_SILICON',
                        isMoved: true,
                        hardwareProperties: {
                            brand: unifiedBrand,
                            model: unifiedModel,
                            board: unifiedBoard,
                            hardware: unifiedHardware,
                            device: unifiedDevice,
                            imei: unifiedImei1,
                            imei1: unifiedImei1,
                            imei2: unifiedImei2,
                            isDualSim: unifiedDualSim
                        },
                        oldDevice: oldSummary,
                        newDevice: newSummary,
                        headingsComparison: headingsComp
                    });
                }
            }
        }

        return res.json({
            duplicates: duplicatePairs,
            totalCount: duplicatePairs.length,
            scannedAt: Date.now()
        });
    } catch (err) {
        console.error("[ERROR] /api/duplicate-devices failed:", err.message);
        return res.status(500).json({ error: "DUPLICATE_SCAN_FAILED", message: err.message });
    }
});

/**
 * POST /api/verify-duplicate-pair
 * Directly verifies two specific device IDs (Old ID vs New ID),
 * extracts their hardware properties and silicon hashes, and returns a verified pair comparison card.
 * Also resolves archived instances under Old_Device_ID if old root was already merged.
 */
app.post("/api/verify-duplicate-pair", async (req, res) => {
    try {
        const { oldAgentId, newAgentId } = req.body || {};
        if (!oldAgentId || !newAgentId) {
            return res.status(400).json({ error: "MISSING_PARAMS", message: "Both oldAgentId and newAgentId are required." });
        }

        const cleanOld = String(oldAgentId).trim().toUpperCase();
        const cleanNew = String(newAgentId).trim().toUpperCase();

        if (cleanOld === cleanNew) {
            return res.status(400).json({ error: "IDENTICAL_IDS", message: "Old Device ID and New Device ID cannot be identical." });
        }

        const [oldDevSnap, newDevSnap, histSnap, geofenceSnap] = await Promise.all([
            db.ref(`devices/${cleanOld}`).once("value"),
            db.ref(`devices/${cleanNew}`).once("value"),
            db.ref("History").once("value"),
            db.ref("Geofencing").once("value")
        ]);

        if (!newDevSnap.exists()) {
            return res.status(404).json({ error: "NEW_DEVICE_NOT_FOUND", message: `New Device [${cleanNew}] does not exist in Firebase RTDB.` });
        }

        const newNode = newDevSnap.val() || {};
        let oldNode = oldDevSnap.exists() ? (oldDevSnap.val() || {}) : null;
        let isMoved = false;

        // If old device root does not exist, check if it is already archived under Old_Device_ID
        if (!oldNode) {
            if (newNode?.Old_Device_ID?.[cleanOld]) {
                oldNode = newNode.Old_Device_ID[cleanOld];
                isMoved = true;
            } else {
                // Check all devices for Old_Device_ID[cleanOld]
                const allDevsSnap = await db.ref("devices").once("value");
                const allDevs = allDevsSnap.val() || {};
                for (const [dId, dNode] of Object.entries(allDevs)) {
                    if (dNode?.Old_Device_ID?.[cleanOld]) {
                        oldNode = dNode.Old_Device_ID[cleanOld];
                        isMoved = true;
                        break;
                    }
                }
            }
        }

        if (!oldNode) {
            return res.status(404).json({ error: "OLD_DEVICE_NOT_FOUND", message: `Old Device [${cleanOld}] does not exist in Firebase RTDB or Old_Device_ID archives.` });
        }

        const rawHistory = histSnap.exists() ? histSnap.val() : {};
        const rawGeofencing = geofenceSnap.exists() ? geofenceSnap.val() : {};

        function extractEpoch(d) {
            if (!d) return 0;
            const liveTs = d.device?.live?.timestamp;
            const telemTs = d.telemetry?.last_seen;
            const idUpdated = d.Identity?.updated_at;
            let maxTs = 0;
            if (typeof liveTs === 'number') maxTs = Math.max(maxTs, liveTs);
            if (typeof telemTs === 'number') maxTs = Math.max(maxTs, telemTs);
            if (typeof idUpdated === 'number') maxTs = Math.max(maxTs, idUpdated);
            return maxTs;
        }

        function formatDev(id, node) {
            const epoch = extractEpoch(node);
            const histRecords = rawHistory[id]?.history || {};
            const historyCount = Object.keys(histRecords).length + (node.device?.live?.latitude ? 1 : 0);
            const relCount = Object.keys(node.relationships || {}).length;

            let safeZonesCount = Object.keys(node.safe_zones || {}).length;
            for (const creator of Object.values(rawGeofencing)) {
                if (creator.safe_zones) {
                    for (const sz of Object.values(creator.safe_zones)) {
                        if (sz.connected_devices && sz.connected_devices[id]) {
                            safeZonesCount++;
                        }
                    }
                }
            }

            const dev = node.device || {};
            const profile = node.profile || {};
            const telemetry = node.telemetry || {};

            const fp = (dev.fingerprint || dev.build_fingerprint || '').trim();
            const fpParts = fp ? fp.split('/') : [];
            const fpDevicePart = (fpParts.length >= 3) ? fpParts[2].split(':')[0] : '';

            const brand = dev.brand || profile.brand || (fpParts.length > 0 ? fpParts[0] : '') || 'Generic';
            const model = dev.model || dev.deviceName || profile.model || (fpParts.length > 1 ? fpParts[1] : '') || 'Unknown';
            const board = dev.board || fpDevicePart || 'N/A';
            const hardware = dev.hardware || (fp.toLowerCase().includes('qcom') ? 'qcom' : (fp ? 'ARM64' : 'N/A'));
            const device = dev.device || fpDevicePart || (dev.deviceId ? dev.deviceId.replace(/^HW_/, '') : id);

            // IMEI resolution (dual SIM support: IMEI 1 & IMEI 2)
            const rawImei = (dev.imei || profile.imei || telemetry.imei || '').trim();
            let imei1 = (dev.imei1 || dev.imei_1 || '').trim();
            let imei2 = (dev.imei2 || dev.imei_2 || '').trim();
            if (!imei1 && rawImei) {
                if (rawImei.includes(',')) {
                    const parts = rawImei.split(',').map(s => s.trim());
                    imei1 = parts[0] || 'N/A';
                    imei2 = parts[1] || '';
                } else if (rawImei.includes('/')) {
                    const parts = rawImei.split('/').map(s => s.trim());
                    imei1 = parts[0] || 'N/A';
                    imei2 = parts[1] || '';
                } else {
                    imei1 = rawImei;
                }
            }
            if (!imei1) imei1 = 'N/A';
            const isDualSim = Boolean(imei2 || dev.is_dual_sim || dev.sim_count === 2);

            return {
                agentId: id,
                name: profile.name || dev.deviceName || id,
                deviceName: dev.deviceName || model || 'Android Device',
                brand,
                model,
                board,
                hardware,
                device,
                imei: imei1,
                imei1,
                imei2,
                isDualSim,
                osVersion: dev.os_version || (fpParts.length >= 3 && fpParts[2].includes(':') ? `Android ${fpParts[2].split(':')[1].split('/')[0]}` : 'Android'),
                avatarUrl: profile.avatar_id ? `/Avatar/${profile.avatar_id}.png` : '/Avatar/001.png',
                lastSeenEpoch: epoch,
                lastSeenFormatted: epoch > 0 ? new Date(epoch).toLocaleString() : 'Never Recorded',
                isLive: (Date.now() - epoch) < (15 * 60 * 1000),
                batteryPct: telemetry.battery_pct ?? 100,
                location: dev.live ? {
                    lat: dev.live.latitude,
                    lng: dev.live.longitude,
                    address: dev.live.geo_address || dev.live.address || 'Unknown'
                } : null,
                historyCount,
                relationshipsCount: relCount,
                safeZonesCount,
                hasArchive: !!node.Old_Device_ID,
                raw: node
            };
        }

        const oldDev = formatDev(cleanOld, oldNode);
        const newDev = formatDev(cleanNew, newNode);

        // Cross-fertilize hardware properties across the pair (since they are verified duplicate silicon instances)
        const unifiedBrand = (newDev.brand && newDev.brand !== 'Generic') ? newDev.brand : (oldDev.brand && oldDev.brand !== 'Generic' ? oldDev.brand : 'Generic');
        const unifiedModel = (newDev.model && newDev.model !== 'Unknown') ? newDev.model : (oldDev.model && oldDev.model !== 'Unknown' ? oldDev.model : 'Unknown');
        const unifiedBoard = (newDev.board && newDev.board !== 'N/A') ? newDev.board : (oldDev.board && oldDev.board !== 'N/A' ? oldDev.board : 'N/A');
        const unifiedHardware = (newDev.hardware && newDev.hardware !== 'N/A') ? newDev.hardware : (oldDev.hardware && oldDev.hardware !== 'N/A' ? oldDev.hardware : 'N/A');
        const unifiedDevice = (newDev.device && newDev.device !== 'N/A') ? newDev.device : (oldDev.device && oldDev.device !== 'N/A' ? oldDev.device : 'N/A');
        const unifiedImei1 = (newDev.imei1 && newDev.imei1 !== 'N/A') ? newDev.imei1 : (oldDev.imei1 && oldDev.imei1 !== 'N/A' ? oldDev.imei1 : 'N/A');
        const unifiedImei2 = newDev.imei2 || oldDev.imei2 || '';
        const unifiedDualSim = Boolean(newDev.isDualSim || oldDev.isDualSim || unifiedImei2);

        oldDev.brand = unifiedBrand;
        newDev.brand = unifiedBrand;
        oldDev.model = unifiedModel;
        newDev.model = unifiedModel;
        oldDev.board = unifiedBoard;
        newDev.board = unifiedBoard;
        oldDev.hardware = unifiedHardware;
        newDev.hardware = unifiedHardware;
        oldDev.device = unifiedDevice;
        newDev.device = unifiedDevice;
        oldDev.imei1 = unifiedImei1;
        newDev.imei1 = unifiedImei1;
        oldDev.imei2 = unifiedImei2;
        newDev.imei2 = unifiedImei2;
        oldDev.isDualSim = unifiedDualSim;
        newDev.isDualSim = unifiedDualSim;

        const oldHash = computeSiliconHash(oldNode);
        const newHash = computeSiliconHash(newNode);
        const isSiliconMatch = oldHash && newHash && (oldHash === newHash);

        const headingsComp = buildHeadingsComparison(cleanOld, cleanNew, oldNode, newNode, rawHistory, rawGeofencing);

        const verifiedPair = {
            pairId: `${cleanOld}_TO_${cleanNew}`,
            matchType: isMoved ? 'MOVED_ACTIVE_ARCHIVE (Old_Device_ID Preserved)' : (isSiliconMatch ? 'SILICON_SHA256 (Brand + Model + Board + Hardware + Device)' : 'MANUAL_ID_SPECIFICATION'),
            siliconHash: newHash || oldHash || 'MANUAL_VERIFIED',
            isSiliconMatch: Boolean(isSiliconMatch),
            isMoved: Boolean(isMoved),
            hardwareProperties: {
                brand: unifiedBrand,
                model: unifiedModel,
                board: unifiedBoard,
                hardware: unifiedHardware,
                device: unifiedDevice,
                imei: unifiedImei1,
                imei1: unifiedImei1,
                imei2: unifiedImei2,
                isDualSim: unifiedDualSim
            },
            oldDevice: oldDev,
            newDevice: newDev,
            headingsComparison: headingsComp
        };

        return res.json({
            success: true,
            pair: verifiedPair
        });
    } catch (err) {
        console.error("[ERROR] /api/verify-duplicate-pair failed:", err.message);
        return res.status(500).json({ error: "VERIFY_PAIR_FAILED", message: err.message });
    }
});

/**
 * POST /api/sync-heading
 * Synchronizes a specific missing heading into devices/{newAgentId}/Old_Device_ID/{oldAgentId}/{headingKey}.
 */
app.post("/api/sync-heading", async (req, res) => {
    try {
        const { oldAgentId, newAgentId, headingKey } = req.body || {};
        if (!oldAgentId || !newAgentId || !headingKey) {
            return res.status(400).json({ error: "MISSING_PARAMS", message: "oldAgentId, newAgentId, and headingKey are required." });
        }

        const [oldDevSnap, newDevSnap, oldHistSnap, newHistSnap, geofenceSnap] = await Promise.all([
            db.ref(`devices/${oldAgentId}`).once("value"),
            db.ref(`devices/${newAgentId}`).once("value"),
            db.ref(`History/${oldAgentId}`).once("value"),
            db.ref(`History/${newAgentId}`).once("value"),
            db.ref("Geofencing").once("value")
        ]);

        if (!newDevSnap.exists()) {
            return res.status(404).json({ error: "NEW_DEVICE_NOT_FOUND", message: `Target new device [${newAgentId}] does not exist.` });
        }

        const oldDevData = oldDevSnap.val() || {};
        const newDevData = newDevSnap.val() || {};
        const archive = newDevData.Old_Device_ID?.[oldAgentId] || {};
        const nowTs = Date.now();

        let dataToSync = oldDevData[headingKey];
        if (dataToSync === undefined && archive[headingKey] !== undefined) {
            dataToSync = archive[headingKey];
        }

        if (headingKey === 'History') {
            if (oldHistSnap.exists()) {
                dataToSync = oldHistSnap.val()?.history || {};
            } else if (newHistSnap.exists()) {
                const allH = newHistSnap.val()?.history || {};
                const oldPts = {};
                for (const [k, v] of Object.entries(allH)) {
                    if (v.origin_agent_id === oldAgentId) oldPts[k] = v;
                }
                dataToSync = Object.keys(oldPts).length > 0 ? oldPts : allH;
            } else {
                dataToSync = { status: "SYNCED_EMPTY" };
            }
        } else if (headingKey === 'safe_zones') {
            const geofences = geofenceSnap.exists() ? geofenceSnap.val() : {};
            const zones = {};
            for (const creatorNode of Object.values(geofences)) {
                if (creatorNode?.safe_zones) {
                    for (const [zid, z] of Object.entries(creatorNode.safe_zones)) {
                        if (z.connected_devices?.[oldAgentId] || z.connected_devices?.[newAgentId] || (z.assigned_targets && z.assigned_targets.includes(oldAgentId))) {
                            zones[zid] = z;
                        }
                    }
                }
            }
            dataToSync = Object.keys(zones).length > 0 ? zones : { status: "SYNCED_NO_ZONES" };
        } else if (headingKey === 'acl') {
            dataToSync = oldDevData.acl || archive.acl || { acl_live_tracking: "GRANTED" };
        } else if (headingKey === 'live_tracking_enabled') {
            dataToSync = oldDevData.live_tracking_enabled !== undefined ? oldDevData.live_tracking_enabled : true;
        } else if (headingKey === 'alerts') {
            dataToSync = oldDevData.alerts || archive.alerts || { active_alerts: {}, updated_at: nowTs };
        } else if (dataToSync === undefined) {
            dataToSync = { synced_at: nowTs, status: "INITIALIZED" };
        }

        await db.ref(`devices/${newAgentId}/Old_Device_ID/${oldAgentId}/${headingKey}`).set(dataToSync);

        const [refreshedNewSnap, refreshedHistSnap, refreshedGeoSnap] = await Promise.all([
            db.ref(`devices/${newAgentId}`).once("value"),
            db.ref("History").once("value"),
            db.ref("Geofencing").once("value")
        ]);

        const refreshedNew = refreshedNewSnap.val() || {};
        const refreshedArchive = refreshedNew.Old_Device_ID?.[oldAgentId] || {};
        const comparison = buildHeadingsComparison(
            oldAgentId,
            newAgentId,
            oldDevSnap.exists() ? oldDevData : refreshedArchive,
            refreshedNew,
            refreshedHistSnap.val() || {},
            refreshedGeoSnap.val() || {}
        );

        return res.json({
            success: true,
            message: `Heading [${headingKey}] synchronized into Old_Device_ID/${oldAgentId}.`,
            headingsComparison: comparison
        });
    } catch (err) {
        console.error("[ERROR] /api/sync-heading failed:", err.message);
        return res.status(500).json({ error: "SYNC_HEADING_FAILED", message: err.message });
    }
});

/**
 * POST /api/sync-all-missing-headings
 * Synchronizes all missing headings into devices/{newAgentId}/Old_Device_ID/{oldAgentId}.
 */
app.post("/api/sync-all-missing-headings", async (req, res) => {
    try {
        const { oldAgentId, newAgentId } = req.body || {};
        if (!oldAgentId || !newAgentId) {
            return res.status(400).json({ error: "MISSING_PARAMS", message: "Both oldAgentId and newAgentId are required." });
        }

        const [oldDevSnap, newDevSnap, oldHistSnap, newHistSnap, geofenceSnap] = await Promise.all([
            db.ref(`devices/${oldAgentId}`).once("value"),
            db.ref(`devices/${newAgentId}`).once("value"),
            db.ref(`History/${oldAgentId}`).once("value"),
            db.ref(`History/${newAgentId}`).once("value"),
            db.ref("Geofencing").once("value")
        ]);

        if (!newDevSnap.exists()) {
            return res.status(404).json({ error: "NEW_DEVICE_NOT_FOUND", message: `Target new device [${newAgentId}] does not exist.` });
        }

        const oldDevData = oldDevSnap.val() || {};
        const newDevData = newDevSnap.val() || {};
        const archive = newDevData.Old_Device_ID?.[oldAgentId] || {};
        const nowTs = Date.now();

        const updates = {};
        const basePath = `devices/${newAgentId}/Old_Device_ID/${oldAgentId}`;

        const allStandardHeadings = [
            'profile', 'device', 'telemetry', 'Identity', 'history', 'History',
            'relationships', 'safe_zones', 'Access', 'Change_Log', 'commands',
            'alerts', 'last_sync', 'acl', 'live_tracking_enabled', '_merge_metadata'
        ];

        for (const h of allStandardHeadings) {
            if (archive[h] !== undefined && archive[h] !== null) continue;

            if (h === 'History') {
                const pts = oldHistSnap.val()?.history || {};
                updates[`${basePath}/History`] = Object.keys(pts).length > 0 ? pts : (newHistSnap.val()?.history || { status: "SYNCED" });
            } else if (h === 'safe_zones') {
                const geofences = geofenceSnap.exists() ? geofenceSnap.val() : {};
                const zones = {};
                for (const creator of Object.values(geofences)) {
                    if (creator.safe_zones) {
                        for (const [zid, z] of Object.entries(creator.safe_zones)) {
                            if (z.connected_devices?.[oldAgentId] || z.connected_devices?.[newAgentId] || (z.assigned_targets && z.assigned_targets.includes(oldAgentId))) {
                                zones[zid] = z;
                            }
                        }
                    }
                }
                updates[`${basePath}/safe_zones`] = Object.keys(zones).length > 0 ? zones : { status: "SYNCED_NO_ZONES" };
            } else if (h === 'acl') {
                updates[`${basePath}/acl`] = oldDevData.acl || { acl_live_tracking: "GRANTED" };
            } else if (h === 'live_tracking_enabled') {
                updates[`${basePath}/live_tracking_enabled`] = oldDevData.live_tracking_enabled !== undefined ? oldDevData.live_tracking_enabled : true;
            } else if (h === 'alerts') {
                updates[`${basePath}/alerts`] = oldDevData.alerts || { active_alerts: {}, updated_at: nowTs };
            } else if (h === '_merge_metadata') {
                updates[`${basePath}/_merge_metadata`] = {
                    merged_at_epoch: nowTs,
                    merged_at_iso: new Date().toISOString(),
                    original_old_agent_id: oldAgentId,
                    target_new_agent_id: newAgentId,
                    migration_reason: "SIGNATURE_MISMATCH_HEADINGS_SYNCHRONIZED"
                };
            } else if (oldDevData[h] !== undefined) {
                updates[`${basePath}/${h}`] = oldDevData[h];
            } else {
                updates[`${basePath}/${h}`] = { status: "INITIALIZED", synced_at: nowTs };
            }
        }

        if (Object.keys(updates).length > 0) {
            await db.ref().update(updates);
        }

        const [refreshedNewSnap, refreshedHistSnap, refreshedGeoSnap] = await Promise.all([
            db.ref(`devices/${newAgentId}`).once("value"),
            db.ref("History").once("value"),
            db.ref("Geofencing").once("value")
        ]);

        const refreshedNew = refreshedNewSnap.val() || {};
        const refreshedArchive = refreshedNew.Old_Device_ID?.[oldAgentId] || {};
        const comparison = buildHeadingsComparison(
            oldAgentId,
            newAgentId,
            oldDevSnap.exists() ? oldDevData : refreshedArchive,
            refreshedNew,
            refreshedHistSnap.val() || {},
            refreshedGeoSnap.val() || {}
        );

        return res.json({
            success: true,
            message: `All missing headings synchronized for ${oldAgentId} -> Old_Device_ID/${oldAgentId}.`,
            headingsComparison: comparison
        });
    } catch (err) {
        console.error("[ERROR] /api/sync-all-missing-headings failed:", err.message);
        return res.status(500).json({ error: "SYNC_ALL_FAILED", message: err.message });
    }
});

/**
 * POST /api/merge-duplicate-devices
 * Atomically merges old duplicate device node into the new device node:
 * 1. Deep clones /devices/{OLD_ID} -> /devices/{NEW_ID}/Old_Device_ID/{OLD_ID}
 * 2. Merges /History/{OLD_ID}/history -> /History/{NEW_ID}/history
 * 3. Re-links circle relationships from OLD_ID -> NEW_ID
 * 4. Re-links SafeZone connected devices from OLD_ID -> NEW_ID
 * 5. Updates lookup indices & writes forensic Change_Log + system_audit
 * 6. Safely cleans up the deprecated root /devices/{OLD_ID} node.
 */
app.post("/api/merge-duplicate-devices", async (req, res) => {
    try {
        const { oldAgentId, newAgentId } = req.body || {};

        if (!oldAgentId || !newAgentId) {
            return res.status(400).json({ error: "MISSING_PARAMS", message: "Both oldAgentId and newAgentId are required." });
        }

        if (oldAgentId === newAgentId) {
            return res.status(400).json({ error: "IDENTICAL_IDS", message: "oldAgentId and newAgentId must not be the same." });
        }

        const [oldDevSnap, newDevSnap, oldHistSnap, geofenceSnap, allDevsSnap] = await Promise.all([
            db.ref(`devices/${oldAgentId}`).once("value"),
            db.ref(`devices/${newAgentId}`).once("value"),
            db.ref(`History/${oldAgentId}`).once("value"),
            db.ref("Geofencing").once("value"),
            db.ref("devices").once("value")
        ]);

        if (!oldDevSnap.exists()) {
            return res.status(404).json({ error: "OLD_DEVICE_NOT_FOUND", message: `Source device [${oldAgentId}] not found in Firebase.` });
        }

        if (!newDevSnap.exists()) {
            return res.status(404).json({ error: "NEW_DEVICE_NOT_FOUND", message: `Target device [${newAgentId}] not found in Firebase.` });
        }

        const oldDevData = oldDevSnap.val() || {};
        const newDevData = newDevSnap.val() || {};
        const nowTs = Date.now();
        const nowIso = new Date().toISOString();
        const logId = `${nowTs}_MERGE_${oldAgentId}`;

        // 1. Prepare Deep Clone Archive Data
        const archivePayload = {
            ...oldDevData,
            _merge_metadata: {
                merged_at_epoch: nowTs,
                merged_at_iso: nowIso,
                original_old_agent_id: oldAgentId,
                target_new_agent_id: newAgentId,
                migration_reason: "SIGNATURE_MISMATCH_SYSTEM_UPDATE"
            }
        };

        const multiPathUpdates = {};

        // A. Store old device data under new device under heading Old_Device_ID/{oldAgentId}
        multiPathUpdates[`devices/${newAgentId}/Old_Device_ID/${oldAgentId}`] = archivePayload;

        // B. Merge History Streams
        if (oldHistSnap.exists()) {
            const oldHistoryItems = oldHistSnap.val()?.history || {};
            for (const [key, record] of Object.entries(oldHistoryItems)) {
                multiPathUpdates[`History/${newAgentId}/history/${key}`] = {
                    ...record,
                    origin_agent_id: oldAgentId,
                    migrated_at: nowTs
                };
            }
        }

        // C. Re-anchor Peer Relationships across all devices
        if (allDevsSnap.exists()) {
            const allDevs = allDevsSnap.val() || {};
            for (const [peerId, peerNode] of Object.entries(allDevs)) {
                if (peerNode?.relationships && peerNode.relationships[oldAgentId]) {
                    const existingRel = peerNode.relationships[oldAgentId];
                    multiPathUpdates[`devices/${peerId}/relationships/${newAgentId}`] = {
                        ...existingRel,
                        linked_agent_id: newAgentId,
                        updated_at: nowIso
                    };
                    multiPathUpdates[`devices/${peerId}/relationships/${oldAgentId}`] = null;
                }
            }
        }

        // D. Re-anchor Relationships from old device into new device if missing
        if (oldDevData.relationships) {
            for (const [peerAgentId, relData] of Object.entries(oldDevData.relationships)) {
                if (peerAgentId !== newAgentId && !newDevData.relationships?.[peerAgentId]) {
                    multiPathUpdates[`devices/${newAgentId}/relationships/${peerAgentId}`] = {
                        ...relData,
                        migrated_from_old_device: oldAgentId
                    };
                }
            }
        }

        // E. Re-anchor SafeZones connected_devices
        if (geofenceSnap.exists()) {
            const geofences = geofenceSnap.val() || {};
            for (const [creatorId, creatorNode] of Object.entries(geofences)) {
                if (creatorNode?.safe_zones) {
                    for (const [zoneId, zoneNode] of Object.entries(creatorNode.safe_zones)) {
                        if (zoneNode?.connected_devices && zoneNode.connected_devices[oldAgentId]) {
                            const connData = zoneNode.connected_devices[oldAgentId];
                            multiPathUpdates[`Geofencing/${creatorId}/safe_zones/${zoneId}/connected_devices/${newAgentId}`] = {
                                ...connData,
                                device_id: newAgentId,
                                updated_at: nowIso
                            };
                            multiPathUpdates[`Geofencing/${creatorId}/safe_zones/${zoneId}/connected_devices/${oldAgentId}`] = null;
                        }
                    }
                }
            }
        }

        // F. Forensic Change_Log in New Device & Global Audit Ledger
        multiPathUpdates[`devices/${newAgentId}/Change_Log/Pairing_Devices/${logId}`] = {
            log_id: logId,
            category: "Pairing_Devices",
            action: "DUPLICATE_DEVICE_MERGED",
            entity_id: newAgentId,
            changed_by: "WEB_ADMIN_OPERATOR",
            timestamp: nowIso.replace(/[-:T.Z]/g, '').slice(0, 16),
            epoch_ms: nowTs,
            metadata: {
                old_device_id: oldAgentId,
                new_device_id: newAgentId,
                archive_location: `devices/${newAgentId}/Old_Device_ID/${oldAgentId}`,
                reason: "OTA_SIGNATURE_MISMATCH_RECONCILIATION"
            }
        };

        multiPathUpdates[`system_audit/${logId}`] = {
            timestamp: nowTs,
            action: "MERGE_DUPLICATE_DEVICE",
            old_device_id: oldAgentId,
            new_device_id: newAgentId,
            status: "SUCCESS"
        };

        // G. Deprecate & Remove old root device node and old root History node
        multiPathUpdates[`devices/${oldAgentId}`] = null;
        multiPathUpdates[`History/${oldAgentId}`] = null;

        // Perform Atomic Commit
        await db.ref().update(multiPathUpdates);

        // Invalidate In-Memory Caches
        LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };
        SAFE_ZONES_CACHE = { data: null, timestamp: 0 };

        console.log(`[DUPLICATE_SENTINEL] ✅ Successfully merged duplicate device ${oldAgentId} into ${newAgentId}. Old data preserved in Old_Device_ID.`);

        return res.json({
            success: true,
            message: `Successfully merged duplicate device [${oldAgentId}] into [${newAgentId}].`,
            oldAgentId,
            newAgentId,
            archivePath: `devices/${newAgentId}/Old_Device_ID/${oldAgentId}`,
            mergedAt: nowIso
        });
    } catch (err) {
        console.error("[ERROR] /api/merge-duplicate-devices failed:", err.message);
        return res.status(500).json({ error: "MERGE_FAILED", message: err.message });
    }
});

// 🛡️ Helper: Validates that an ID strictly conforms to canonical 8-char uppercase hex
function isCanonicalAgentId(id) {
    return typeof id === "string" && /^[A-Z0-9]{8}$/.test(id.trim());
}

// 🤖 Background Maintenance Agent: Runs periodic database integrity and cache hygiene
function initBackgroundMaintenanceAgent() {
    const INTERVAL_MS = 10 * 60 * 1000; // 10 minutes
    setInterval(async () => {
        try {
            console.log("[BACKGROUND_AGENT] Executing scheduled maintenance & forensic cache optimization...");
            
            // 1. Flush stale in-memory reverse-geocoding cache if bloated
            if (GEO_CACHE.size > 500) {
                GEO_CACHE.clear();
                console.log("[BACKGROUND_AGENT] Reverse geocoding cache pruned.");
            }

            // 2. Refresh live device and safe zone cache
            LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };
            SAFE_ZONES_CACHE = { data: null, timestamp: 0 };

            // 3. 🛡️ Canonical Schema Guard: Purge rogue keys under /devices/ and unregistered history under /History/
            const [devSnap, histSnap] = await Promise.all([
                db.ref("devices").once("value"),
                db.ref("History").once("value")
            ]);
            if (devSnap.exists()) {
                const registeredDevices = new Set(Object.keys(devSnap.val() || {}));
                // A. Purge rogue non-canonical keys under /devices/
                for (const k of Array.from(registeredDevices)) {
                    if (!isCanonicalAgentId(k)) {
                        console.warn(`[BACKGROUND_AGENT] ⚠️ Purging rogue non-canonical key under /devices/: ${k}`);
                        await db.ref(`devices/${k}`).remove().catch(() => null);
                        registeredDevices.delete(k);
                    }
                }
                // B. Sync History: Guarantee ONLY actively registered devices have records in /History/
                if (histSnap.exists()) {
                    const histKeys = Object.keys(histSnap.val() || {});
                    for (const hk of histKeys) {
                        if (!registeredDevices.has(hk)) {
                            console.warn(`[BACKGROUND_AGENT] ⚠️ Purging orphaned history for unregistered device: History/${hk}`);
                            await db.ref(`History/${hk}`).remove().catch(() => null);
                        }
                    }
                }
            }

            // 4. Heartbeat health ping to Firebase RTDB
            await db.ref(".info/connected").once("value");
            console.log("[BACKGROUND_AGENT] ✅ Closed-loop database connectivity & History-Device parity verified.");
        } catch (err) {
            console.warn("[BACKGROUND_AGENT] Maintenance cycle warning:", err.message);
        }
    }, INTERVAL_MS);
    console.log("[BACKGROUND_AGENT] Initialized 10-minute automated background maintenance daemon with History-Device parity guard.");
}

const PORT = process.env.PROXY_PORT || (process.env.PORT && process.env.PORT !== "8080" ? process.env.PORT : 3000);
const HOST = process.env.HOST || "127.0.0.1";

// ============================================================
// NEW API ENDPOINTS - Settings Sync & Device Registration
// ============================================================

// Settings Sync Endpoint
app.post("/api/save-settings", async (req, res) => {
    try {
        const { path: settingsPath, data } = req.body;
        
        if (!settingsPath || !data) {
            return res.status(400).json({ 
                error: "INVALID_PAYLOAD", 
                message: "Path and data required" 
            });
        }
        
        // Save to Firebase Realtime Database
        await db.ref(settingsPath).set(data);
        
        console.log(`[SETTINGS_API] Settings saved to ${settingsPath}`);
        
        res.json({ 
            status: "SUCCESS", 
            message: "Settings saved to server",
            timestamp: Date.now()
        });
        
    } catch (err) {
        console.error("[SETTINGS_API] Error saving settings:", err.message);
        res.status(500).json({ 
            error: "SAVE_FAILED", 
            message: err.message 
        });
    }
});

// Settings Load Endpoint
app.get("/api/settings", async (req, res) => {
    try {
        const snapshot = await db.ref("config/operator/settings").once("value");
        const data = snapshot.val();
        
        res.json({
            status: "SUCCESS",
            settings: data || {}
        });
        
    } catch (err) {
        console.error("[SETTINGS_API] Error fetching settings:", err.message);
        res.status(500).json({ 
            error: "FETCH_FAILED", 
            message: err.message 
        });
    }
});

// Device Registration Endpoint
app.post("/api/devices", async (req, res) => {
    try {
        const deviceData = req.body;
        
        if (!deviceData.agentId) {
            return res.status(400).json({ 
                error: "MISSING_AGENT_ID",
                message: "Agent ID is required"
            });
        }
        
        // Check if device already exists
        const existing = await db.ref(`devices/${deviceData.agentId}`).once("value");
        
        if (existing.exists()) {
            return res.status(409).json({ 
                error: "DEVICE_EXISTS", 
                message: `Device with ID '${deviceData.agentId}' already exists`
            });
        }
        
        // Add timestamps
        const now = Date.now();
        deviceData.createdAt = now;
        deviceData.updatedAt = now;
        
        // Ensure identity structure exists
        if (!deviceData.Identity) {
            deviceData.Identity = {};
        }
        deviceData.Identity.created_at = now;
        
        // Save to Firebase
        await db.ref(`devices/${deviceData.agentId}`).set(deviceData);
        
        console.log(`[DEVICE_API] Device registered: ${deviceData.agentId} (${deviceData.profile?.name || 'Unnamed'})`);
        
        res.json({
            status: "SUCCESS",
            agentId: deviceData.agentId,
            message: `Device "${deviceData.profile?.name || deviceData.agentId}" registered successfully`,
            timestamp: now
        });
        
    } catch (err) {
        console.error("[DEVICE_API] Error registering device:", err.message);
        res.status(500).json({ 
            error: "REGISTER_FAILED", 
            message: err.message 
        });
    }
});

// Circles/Groups Endpoint
app.get("/api/circles", async (req, res) => {
    try {
        const snapshot = await db.ref("config/circles").once("value");
        const circles = snapshot.val() || [
            { id: "family", name: "Family Circle" },
            { id: "operations", name: "Operations" },
            { id: "field", name: "Field Service" },
            { id: "default", name: "Default Circle" }
        ];
        
        res.json({ circles });
        
    } catch (err) {
        console.error("[CIRCLES_API] Error fetching circles:", err.message);
        // Return default circles on error
        res.json({ 
            circles: [
                { id: "family", name: "Family Circle" },
                { id: "operations", name: "Operations" },
                { id: "field", name: "Field Service" }
            ] 
        });
    }
});

// ============================================================
// END OF NEW API ENDPOINTS
// ============================================================

app.listen(PORT, HOST, () => {
    console.log(`==========================================`);
    console.log(`   jFindMe FORENSIC PROXY LIVE            `);
    console.log(`   Endpoint: http://${HOST}:${PORT}       `);
    console.log(`   Settings API: /api/save-settings       `);
    console.log(`   Devices API:  /api/devices             `);
    console.log(`   Circles API:  /api/circles             `);
    console.log(`==========================================`);
    initRealtimeSentinelBridge();
    initBackgroundMaintenanceAgent();
});
