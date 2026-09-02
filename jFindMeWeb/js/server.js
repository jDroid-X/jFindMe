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
    if (LIVE_DEVICES_CACHE.data && (now - LIVE_DEVICES_CACHE.timestamp) < CACHE_TTL_MS) {
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
            if (!creatorNode || !creatorNode.safe_zones) continue;
            for (const [zoneId, zone] of Object.entries(creatorNode.safe_zones)) {
                if (zone && zone.lat && zone.lng) {
                    safeZoneCoords.set(zone.zone_id || zoneId, {
                        lat: parseFloat(zone.lat),
                        lng: parseFloat(zone.lng),
                        name: zone.zone_name || "Safe Zone"
                    });
                }
            }
        }

        // Merge Auth UID nodes (long 28-char keys) into canonical 8-character device nodes
        for (const [key, node] of Object.entries(devicesData)) {
            if (!node || typeof node !== "object") continue;
            if (key.length > 16) {
                const targetAgentId = authToDevice[key];
                if (targetAgentId && devicesData[targetAgentId]) {
                    const canonical = devicesData[targetAgentId];
                    if (node.device && node.device.live) {
                        canonical.device = canonical.device || {};
                        canonical.device.live = Object.assign({}, canonical.device.live || {}, node.device.live);
                    }
                }
            }
        }

        const devicesList = [];

        for (const [agentId, node] of Object.entries(devicesData)) {
            if (!node || typeof node !== "object") continue;
            if (agentId.length > 16) continue;

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

            // 2. Safe Zone Fallback: ONLY if GPS coordinates are 0 and device is in safe zone
            if ((lat === 0 || lng === 0) && liveLoc.current_safe_zone_state === "IN" && liveLoc.current_safe_zone_id) {
                const zoneInfo = safeZoneCoords.get(liveLoc.current_safe_zone_id);
                if (zoneInfo) {
                    lat = zoneInfo.lat;
                    lng = zoneInfo.lng;
                    liveLoc.address = `Inside ${zoneInfo.name} Safe Perimeter`;
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

        // 1. Extract from Geofencing/{creatorId}/safe_zones (Primary SSOT for Geofencing & Connected Devices)
        for (const [rawCreatorId, creatorNode] of Object.entries(geofenceData)) {
            if (!creatorNode || !creatorNode.safe_zones) continue;
            const canonicalCreator = resolveCanonicalDevId(rawCreatorId);
            for (const [zoneId, zone] of Object.entries(creatorNode.safe_zones)) {
                if (!zone || typeof zone !== "object" || seenZoneIds.has(zoneId)) continue;
                seenZoneIds.add(zoneId);

                const connMap = zone.connected_devices || {};
                const connList = Object.keys(connMap).map(k => resolveCanonicalDevId(k)).filter((v, i, a) => a.indexOf(v) === i);

                zonesList.push({
                    zoneId: zone.zone_id || zoneId,
                    zoneName: zone.zone_name || zone.name || "Safe Perimeter",
                    latitude: parseFloat(zone.lat || zone.latitude || 0),
                    longitude: parseFloat(zone.lng || zone.longitude || 0),
                    radius: parseFloat(zone.radius || zone.radius_meters || 100),
                    address: zone.address || "",
                    assignedName: zone.assigned_targets || zone.assignedName || "",
                    creatorId: canonicalCreator,
                    isActive: zone.is_active !== false && zone.isZoneActive !== false,
                    alertsEnabled: Boolean(zone.alerts_enabled || zone.alertsEnabled),
                    history: zone.history || {},
                    connectedDevices: connList
                });
            }
        }

        // 2. Extract from relationships/{groupId}/geofences
        for (const [groupId, groupNode] of Object.entries(relData)) {
            if (!groupNode || !groupNode.geofences) continue;
            for (const [zoneId, zone] of Object.entries(groupNode.geofences)) {
                if (!zone || typeof zone !== "object" || seenZoneIds.has(zoneId)) continue;
                seenZoneIds.add(zoneId);

                const connMap = zone.connected_devices || {};
                const connList = Object.keys(connMap).map(k => resolveCanonicalDevId(k)).filter((v, i, a) => a.indexOf(v) === i);

                zonesList.push({
                    zoneId: zone.zone_id || zoneId,
                    zoneName: zone.zone_name || zone.name || "Safe Perimeter",
                    latitude: parseFloat(zone.lat || zone.latitude || 0),
                    longitude: parseFloat(zone.lng || zone.longitude || 0),
                    radius: parseFloat(zone.radius || zone.radius_meters || 100),
                    address: zone.address || "",
                    assignedName: zone.assignedName || zone.assigned_targets || "",
                    creatorId: resolveCanonicalDevId(zone.created_by || zone.creator_uid || groupId),
                    isActive: zone.isZoneActive !== false && zone.is_active !== false,
                    alertsEnabled: Boolean(zone.alertsEnabled || zone.alerts_enabled),
                    history: zone.history || {},
                    connectedDevices: connList
                });
            }
        }

        // 3. Extract from devices/{agentId}/safe_zones
        for (const [agentId, deviceNode] of Object.entries(devicesData)) {
            if (!deviceNode || !deviceNode.safe_zones) continue;
            for (const [zoneId, zone] of Object.entries(deviceNode.safe_zones)) {
                if (!zone || typeof zone !== "object" || seenZoneIds.has(zoneId)) continue;
                seenZoneIds.add(zoneId);

                const connMap = zone.connected_devices || {};
                const connList = Object.keys(connMap).map(k => resolveCanonicalDevId(k)).filter((v, i, a) => a.indexOf(v) === i);

                zonesList.push({
                    zoneId: zone.zone_id || zoneId,
                    zoneName: zone.zone_name || zone.name || "Safe Perimeter",
                    latitude: parseFloat(zone.lat || zone.latitude || 0),
                    longitude: parseFloat(zone.lng || zone.longitude || 0),
                    radius: parseFloat(zone.radius || zone.radius_meters || 100),
                    address: zone.address || "",
                    assignedName: zone.assigned_targets || zone.assignedName || "",
                    creatorId: resolveCanonicalDevId(agentId),
                    isActive: zone.is_active !== false,
                    alertsEnabled: Boolean(zone.alerts_enabled),
                    history: zone.history || {},
                    connectedDevices: connList
                });
            }
        }

        // Enrich every safe zone history ledger with group notifications
        zonesList.forEach(z => {
            const zoneNameLower = (z.zoneName || "").toLowerCase();
            const zoneIdLower = (z.zoneId || "").toLowerCase();
            
            for (const [groupId, groupNode] of Object.entries(relData)) {
                if (!groupNode || !groupNode.notifications) continue;
                for (const [nId, n] of Object.entries(groupNode.notifications)) {
                    const notifZoneName = (n.zone_name || "").toLowerCase();
                    const notifZoneId = (n.zone_id || "").toLowerCase();
                    if (notifZoneName === zoneNameLower || notifZoneId === zoneIdLower || (zoneNameLower.length > 3 && notifZoneName.includes(zoneNameLower))) {
                        z.history[nId] = {
                            event_id: nId,
                            datetime: n.datetime || n.readable_time || (n.timestamp ? new Date(n.timestamp).toLocaleString() : "Recorded Alert"),
                            status: n.status || n.transition || n.event_type || "TRANSITION",
                            target_name: n.avatar_name || n.target_name || "Device",
                            target_uid: resolveCanonicalDevId(n.target_uid || n.creator_uid),
                            zone_name: z.zoneName,
                            timestamp: n.timestamp || Date.now()
                        };
                    }
                }
            }
        });

        const validZones = zonesList.filter(z => z.latitude !== 0 && z.longitude !== 0);
        const zonePayload = { status: "SUCCESS", count: validZones.length, safeZones: validZones, timestamp: now };
        SAFE_ZONES_CACHE = { data: zonePayload, timestamp: now };
        res.json(zonePayload);
    } catch (err) {
        console.error("[ERROR] /api/safe-zones failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Create & Save Safe Zone Endpoint (Atomic Multi-Node Write)
app.post("/api/save-safe-zone", async (req, res) => {
    const { zoneId, zoneName, latitude, longitude, radius, isActive, alertsEnabled, connectedDevices, creatorId } = req.body;
    if (!zoneId || !zoneName || !latitude || !longitude) {
        return res.status(400).json({ error: "MISSING_REQUIRED_SAFE_ZONE_FIELDS" });
    }

    try {
        const creator = creatorId || "WEB_ADMIN_HOST";
        console.log(`[SAFE_ZONES] Saving Safe Zone [${zoneName}] (${zoneId})...`);

        const connMap = {};
        if (Array.isArray(connectedDevices)) {
            connectedDevices.forEach(dId => {
                connMap[dId] = {
                    device_id: dId,
                    role: "TARGET",
                    assigned_at: Date.now()
                };
            });
        }

        const zonePayload = {
            zone_id: zoneId,
            zone_name: zoneName,
            lat: parseFloat(latitude),
            lng: parseFloat(longitude),
            radius: parseFloat(radius || 100),
            is_active: isActive !== false,
            alerts_enabled: Boolean(alertsEnabled),
            creator_uid: creator,
            created_on: new Date().toISOString(),
            connected_devices: connMap
        };

        // 1. Write to Geofencing/{creator}/safe_zones/{zoneId}
        await db.ref(`Geofencing/${creator}/safe_zones/${zoneId}`).set(zonePayload);

        // 2. Multi-cast write to each connected device's safe_zones node
        if (Array.isArray(connectedDevices)) {
            for (const targetId of connectedDevices) {
                await db.ref(`devices/${targetId}/safe_zones/${zoneId}`).set({
                    zone_id: zoneId,
                    zone_name: zoneName,
                    lat: parseFloat(latitude),
                    lng: parseFloat(longitude),
                    radius: parseFloat(radius || 100),
                    is_active: isActive !== false,
                    alerts_enabled: Boolean(alertsEnabled)
                }).catch(() => null);
            }
        }

        // 3. Log into system audit
        await db.ref(`system_audit/${Date.now()}_ZONE_CREATE`).set({
            timestamp: Date.now(),
            admin_uid: "WEB_ADMIN",
            action: "CREATE_SAFE_ZONE",
            target_path: `Geofencing/${creator}/safe_zones/${zoneId}`,
            zone_name: zoneName
        }).catch(() => null);

        console.log(`[SAFE_ZONES] Success: Geofence [${zoneName}] stored and synced.`);
        res.json({ status: "SUCCESS", zoneId, zone: zonePayload });
    } catch (err) {
        console.error("[ERROR] /api/save-safe-zone failed:", err.message);
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
    let { agentId, profileData } = req.body;
    if (!agentId || !profileData) {
        return res.status(400).json({ error: "MISSING_AGENT_ID_OR_PROFILE_DATA" });
    }

    try {
        agentId = await resolveCanonicalAgentId(agentId);
        console.log(`[PROFILE_UPDATE] Updating profile for Agent [${agentId}]...`);
        const profileRef = db.ref(`devices/${agentId}/profile`);
        const currentSnap = await profileRef.once("value");
        const previousState = currentSnap.val() || {};

        const updatedProfile = {
            ...previousState,
            ...profileData,
            fullName: profileData.name || previousState.fullName || previousState.name,
            name: profileData.name || previousState.name || previousState.fullName,
            userName: profileData.name || previousState.userName || previousState.name,
            updated_at: new Date().toISOString().replace(/[-:T.]/g, "").slice(0, 16)
        };

        // 1. Write atomic profile update
        await profileRef.update(updatedProfile);

        // 2. Closed-Loop: Synchronize Identity & Live Node
        const newName = updatedProfile.name;
        const newAvatar = updatedProfile.avatar_id;
        
        await db.ref(`devices/${agentId}/Identity`).update({
            owner_name: newName,
            id_full_name: newName,
            ...(newAvatar ? { avatar_id: newAvatar } : {})
        }).catch(() => null);

        await db.ref(`devices/${agentId}/device/live`).update({
            userName: newName,
            ...(newAvatar ? { avatar_id: newAvatar } : {})
        }).catch(() => null);

        // 3. Closed-Loop: Synchronize Geofencing Root
        await db.ref(`Geofencing/${agentId}`).update({
            device_name: newName
        }).catch(() => null);

        // 4. Closed-Loop: Propagate Name & Avatar across all peer relationships
        const allDevicesSnap = await db.ref("devices").once("value").catch(() => null);
        const allDevices = (allDevicesSnap && allDevicesSnap.val()) || {};
        
        for (const [peerId, peerNode] of Object.entries(allDevices)) {
            if (peerId === agentId || !peerNode) continue;
            
            // Peer relationship link
            if (peerNode.relationships && peerNode.relationships[agentId]) {
                await db.ref(`devices/${peerId}/relationships/${agentId}`).update({
                    name: newName
                }).catch(() => null);
            }

            // Peer safe zone connected devices
            if (peerNode.safe_zones) {
                for (const zoneId of Object.keys(peerNode.safe_zones)) {
                    if (peerNode.safe_zones[zoneId].connected_devices && peerNode.safe_zones[zoneId].connected_devices[agentId]) {
                        await db.ref(`devices/${peerId}/safe_zones/${zoneId}/connected_devices/${agentId}`).update({
                            device_name: newName,
                            ...(newAvatar ? { avatar_id: newAvatar } : {})
                        }).catch(() => null);
                    }
                }
            }
        }

        // 5. Closed-Loop: Propagate across top-level Geofencing tree
        const geofencingSnap = await db.ref("Geofencing").once("value").catch(() => null);
        const geofencingData = (geofencingSnap && geofencingSnap.val()) || {};
        for (const [creatorId, creatorNode] of Object.entries(geofencingData)) {
            if (!creatorNode || !creatorNode.safe_zones) continue;
            for (const [zoneId, zone] of Object.entries(creatorNode.safe_zones)) {
                if (zone.connected_devices && zone.connected_devices[agentId]) {
                    await db.ref(`Geofencing/${creatorId}/safe_zones/${zoneId}/connected_devices/${agentId}`).update({
                        device_name: newName,
                        ...(newAvatar ? { avatar_id: newAvatar } : {})
                    }).catch(() => null);
                }
            }
        }

        // 6. Record Forensic Audit in Change_Log
        const logId = `${Date.now()}_WEB_${agentId.slice(0, 4)}`;
        const logRef = db.ref(`devices/${agentId}/Change_Log/Profile/${logId}`);
        await logRef.set({
            log_id: logId,
            category: "Profile",
            action: "PROFILE_WEB_MUTATION",
            entity_id: agentId,
            changed_by: "Web_Matrix_Admin",
            timestamp: new Date().toISOString(),
            epoch_ms: Date.now(),
            previous_state: previousState,
            new_state: updatedProfile
        }).catch(() => null);

        console.log(`[PROFILE_UPDATE] Success: Closed-loop profile synchronized for ${agentId} (${newName}).`);
        res.json({ status: "SUCCESS", agentId, profile: updatedProfile });
    } catch (err) {
        console.error("[ERROR] /api/update-profile failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Dynamic Reverse Geocoding Proxy with In-Memory Cache
const GEO_CACHE = new Map();
app.get("/api/reverse-geocode", async (req, res) => {
    const lat = parseFloat(req.query.lat);
    const lng = parseFloat(req.query.lng);

    if (isNaN(lat) || isNaN(lng) || (lat === 0 && lng === 0)) {
        return res.status(400).json({ error: "INVALID_COORDINATES" });
    }

    const key = `${lat.toFixed(4)},${lng.toFixed(4)}`;
    if (GEO_CACHE.has(key)) {
        return res.json({ status: "CACHED", address: GEO_CACHE.get(key) });
    }

    try {
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
                    const displayName = parsed.display_name || "Unknown Spatial Location";
                    // Cache up to 1000 items
                    if (GEO_CACHE.size > 1000) GEO_CACHE.clear();
                    GEO_CACHE.set(key, displayName);
                    res.json({ status: "SUCCESS", address: displayName });
                } catch (e) {
                    res.json({ status: "FALLBACK", address: `${lat.toFixed(5)}°, ${lng.toFixed(5)}°` });
                }
            });
        }).on('error', () => {
            res.json({ status: "FALLBACK", address: `${lat.toFixed(5)}°, ${lng.toFixed(5)}°` });
        });
    } catch (err) {
        res.json({ status: "FALLBACK", address: `${lat.toFixed(5)}°, ${lng.toFixed(5)}°` });
    }
});

// Poke / Ping Telemetry Trigger Endpoint
app.post("/api/poke-device", async (req, res) => {
    let { agentId, pokedBy } = req.body;
    if (!agentId) return res.status(400).json({ error: "MISSING_AGENT_ID" });

    try {
        agentId = await resolveCanonicalAgentId(agentId);
        await db.ref(`devices/${agentId}/telemetry/poke`).set({
            isPoked: true,
            pokedBy: pokedBy || "Web_Matrix_Commander",
            timestamp: Date.now()
        });
        res.json({ status: "POKED", agentId, timestamp: Date.now() });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
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
            // Assume path is 'collection/docId'
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

const PORT = process.env.PORT || 3000;
const HOST = process.env.HOST || "127.0.0.1";
app.listen(PORT, HOST, () => {
    console.log(`==========================================`);
    console.log(`   jFindMe FORENSIC PROXY LIVE            `);
    console.log(`   Endpoint: http://${HOST}:${PORT}       `);
    console.log(`==========================================`);
    initRealtimeSentinelBridge();
});
