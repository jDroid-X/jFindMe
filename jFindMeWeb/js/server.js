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

app.use(cors());
app.use(express.json());

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

// Atomic Profile & Avatar Update with Forensic Ledger (Enforces Web Read-Only Policy)
app.post("/api/update-profile", async (req, res) => {
    return res.status(403).json({
        error: "READ_ONLY_MODE",
        message: "Web Portal operates in Read-Only Analytics Mode. Profiles must be updated on the mobile device."
    });
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
app.listen(PORT, HOST, () => {
    console.log(`==========================================`);
    console.log(`   jFindMe FORENSIC PROXY LIVE            `);
    console.log(`   Endpoint: http://${HOST}:${PORT}       `);
    console.log(`==========================================`);
    initRealtimeSentinelBridge();
    initBackgroundMaintenanceAgent();
});
