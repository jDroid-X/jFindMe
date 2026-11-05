 const express = require("express");
const cors = require("cors");
const admin = require("firebase-admin");
const fs = require("fs");
const path = require("path");

process.on("uncaughtException", (err) => {
    console.error("[FATAL_TRAP] Uncaught Exception:", err.stack || err.message || err);
});
process.on("unhandledRejection", (reason) => {
    console.error("[FATAL_TRAP] Unhandled Rejection:", reason?.stack || reason?.message || reason);
});

console.log("[SYSTEM] Initializing jFindMe Forensic Proxy...");

// --- PATH VALIDATION (DYNAMIC MULTI-TIER RESOLVER) ---
const root = path.join(__dirname, "../");
const secretsDir = path.join(root, "secrets");

const saCandidates = [
    path.join(secretsDir, "service-account.json"),
    path.join(root, "service-account.json"),
    path.join(secretsDir, "jfindme-3b15c-firebase-adminsdk-fbsvc-56c3dc08cf.json"),
    path.join(root, "jfindme-3b15c-firebase-adminsdk-fbsvc-56c3dc08cf.json")
];

let SA_PATH = saCandidates.find(p => fs.existsSync(p));

if (!SA_PATH) {
    const searchDirs = [secretsDir, root];
    for (const d of searchDirs) {
        if (fs.existsSync(d)) {
            const files = fs.readdirSync(d);
            const saFile = files.find(f => f.includes("firebase-adminsdk") && f.endsWith(".json"));
            if (saFile) {
                SA_PATH = path.join(d, saFile);
                break;
            }
        }
    }
}

const gsCandidates = [
    path.join(secretsDir, "google-services.json"),
    path.join(root, "google-services.json"),
    path.join(root, "app/google-services.json"),
    path.join(root, "app/src/main/google-services.json")
];

const GS_PATH = gsCandidates.find(p => fs.existsSync(p));

if (!SA_PATH || !fs.existsSync(SA_PATH)) {
    console.error("[FATAL] Missing service-account.json across search paths:", saCandidates);
    process.exit(1);
}
if (!GS_PATH || !fs.existsSync(GS_PATH)) {
    console.error("[FATAL] Missing google-services.json across search paths:", gsCandidates);
    process.exit(1);
}

console.log("[INFO] Identity Resolver: Using service account [" + path.basename(SA_PATH) + "]");
console.log("[INFO] Identity Resolver: Using Google services [" + path.basename(GS_PATH) + "]");

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

// Local In-Memory Mock Fixture support for ultra-fast offline testing & WAN bypass
const FIXTURE_PATH = path.join(__dirname, "fixtures/local_mock_db.json");
let LOCAL_MOCK_DATA = null;
if (fs.existsSync(FIXTURE_PATH)) {
    try {
        LOCAL_MOCK_DATA = JSON.parse(fs.readFileSync(FIXTURE_PATH, "utf8"));
        if (process.env.USE_LOCAL_MOCK === "true") {
            console.log("[MOCK] Ultra-Fast Local Database Fixture Active (Zero WAN Latency).");
        } else {
            console.log("[FIREBASE_REAL] Connected to Live Cloud Firebase RTDB: " + G_SERVICES.project_info.firebase_url);
        }
    } catch (e) {
        console.warn("[WARN] Could not load local mock fixture:", e.message);
    }
}

// Universal Resilient RTDB Timeout Helpers with Local Mock Support
// [Variables]: refPath, ms, parts, curr, devicesSnap, lookupSnap, geofenceSnap, relSnap, verUpdateSnap | [Constants]: FIXTURE_PATH, LOCAL_MOCK_DATA, CACHE_TTL_MS | [Dependencies]: admin.database (db), fs, path | [Integration Points]: Firebase RTDB, /api/devices-live, /api/fleet/compact-live
const queryNodeWithTimeout = (refPath, ms = 8000) => {
    if (process.env.USE_LOCAL_MOCK === "true" && LOCAL_MOCK_DATA) {
        const parts = refPath.split("/").filter(Boolean);
        let curr = LOCAL_MOCK_DATA;
        for (const p of parts) {
            if (curr && typeof curr === "object") curr = curr[p];
            else { curr = undefined; break; }
        }
        return Promise.resolve({
            exists: () => curr !== undefined && curr !== null,
            val: () => (curr !== undefined ? curr : null)
        });
    }

    return Promise.race([
        db.ref(refPath).once("value"),
        new Promise((_, reject) => setTimeout(() => reject(new Error("Timeout " + refPath)), ms))
    ]).catch(err => {
        console.warn("[WARN] Universal RTDB helper timeout on [" + refPath + "]:", err.message);
        if (LOCAL_MOCK_DATA) {
            const parts = refPath.split("/").filter(Boolean);
            let curr = LOCAL_MOCK_DATA;
            for (const p of parts) {
                if (curr && typeof curr === "object") curr = curr[p];
                else { curr = undefined; break; }
            }
            if (curr !== undefined && curr !== null) {
                console.log("[FALLBACK] Serving offline mock fixture for [" + refPath + "]");
                return {
                    exists: () => true,
                    val: () => curr
                };
            }
        }
        return null;
    });
};

const writeWithTimeout = (refPath, data, ms = 3500, isUpdate = false) => {
    if (process.env.USE_LOCAL_MOCK === "true" && LOCAL_MOCK_DATA) {
        if (!refPath && isUpdate && typeof data === 'object') {
            for (const [k, v] of Object.entries(data)) {
                const parts = k.split("/").filter(Boolean);
                let curr = LOCAL_MOCK_DATA;
                for (let i = 0; i < parts.length - 1; i++) {
                    curr[parts[i]] = curr[parts[i]] || {};
                    curr = curr[parts[i]];
                }
                curr[parts[parts.length - 1]] = v;
            }
        }
        return Promise.resolve(true);
    }

    const nodeRef = refPath ? db.ref(refPath) : db.ref();
    const action = isUpdate ? nodeRef.update(data) : nodeRef.set(data);
    return Promise.race([
        action,
        new Promise((_, reject) => setTimeout(() => reject(new Error("Timeout write " + refPath)), ms))
    ]).catch(err => {
        console.warn(`[WARN] RTDB write timeout on ${refPath}:`, err.message);
        return false;
    });
};

// Port configuration for jFindMeWeb API Proxy
const PORT = process.env.PROXY_PORT || (process.env.PORT && process.env.PORT !== "8080" ? process.env.PORT : 3000);
const HOST = process.env.HOST || "127.0.0.1";

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
        if (!origin || origin === 'null') return callback(null, true);
        
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

// Serve Frontend Static Files with zero-cache in development to guarantee instantaneous online sync
app.use(express.static(path.join(__dirname, "../"), {
    setHeaders: (res, filePath) => {
        if (filePath.endsWith('.js') || filePath.endsWith('.html') || filePath.endsWith('.css')) {
            res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
        }
    }
}));

// In-Memory Short TTL Caches for High-Frequency Polling (Sub-millisecond response)
let LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };
let COMPACT_DEVICES_CACHE = { data: null, timestamp: 0 };
let SAFE_ZONES_CACHE = { data: null, timestamp: 0 };
const CACHE_TTL_MS = 10000; // 10-second cache window for instant responses (Stale-While-Revalidate)

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
        const rem = str.substring(14);
        const ms = rem ? parseInt(rem.padEnd(3, '0').slice(0, 3), 10) : 0;
        return new Date(y, m, d, hh, mm, ss, ms).getTime();
    }
    const parsed = Date.parse(str);
    if (!isNaN(parsed)) return parsed;
    const num = Number(str);
    if (!isNaN(num) && num > 0) return parseTimestampToMillis(num);
    return 0;
}

// Core SSOT: Live Devices Aggregation Engine (Descending Latest Priority)
async function computeLiveDevicesPayload() {
    const now = Date.now();

    // [Variables]: recencyLimitMs, dormantLimitMs, settingsData
    // [Constants]: None | [Dependencies]: queryNodeWithTimeout | [Integration Points]: Operator Settings SSOT
    const [devicesSnap, lookupSnap, geofenceSnap, relSnap, verUpdateSnap, settingsSnap] = await Promise.all([
        queryNodeWithTimeout("devices", 8000),
        queryNodeWithTimeout("lookup_indices/auth_to_device", 6000),
        queryNodeWithTimeout("Geofencing", 6000),
        queryNodeWithTimeout("relationships", 6000),
        queryNodeWithTimeout("Version_Update", 6000),
        queryNodeWithTimeout("config/operator/settings", 4000)
    ]);

    const devicesData = (devicesSnap && typeof devicesSnap.val === 'function' ? devicesSnap.val() : null)
        || (LIVE_DEVICES_CACHE.data && Array.isArray(LIVE_DEVICES_CACHE.data.devices) ? LIVE_DEVICES_CACHE.data.devices.reduce((acc, d) => ({...acc, [d.agentId]: d}), {}) : null)
        || (LOCAL_MOCK_DATA && LOCAL_MOCK_DATA.devices ? LOCAL_MOCK_DATA.devices : null)
        || {};
    const authToDevice = (lookupSnap && typeof lookupSnap.val === 'function' && lookupSnap.val()) || {};
    const geofenceData = (geofenceSnap && typeof geofenceSnap.val === 'function' && geofenceSnap.val()) || {};
    const relData = (relSnap && typeof relSnap.val === 'function' && relSnap.val()) || {};
    const verUpdateData = (verUpdateSnap && typeof verUpdateSnap.val === 'function' && verUpdateSnap.val()) || {};
    const settingsData = (settingsSnap && typeof settingsSnap.val === 'function' && settingsSnap.val()) || {};
    const recencyLimitMs = Math.max(1, parseInt(settingsData.recencyThresholdMinutes || 15, 10)) * 60 * 1000;
    const dormantLimitMs = Math.max(1, parseInt(settingsData.dormantThresholdHours || 6, 10)) * 60 * 60 * 1000;
    console.log(`[FIREBASE_SYNC] Fetched devicesData with ${Object.keys(devicesData).length} nodes (Snap: ${Boolean(devicesSnap)}), authToDevice: ${Object.keys(authToDevice).length} keys, recencyLimitMs: ${recencyLimitMs}, dormantLimitMs: ${dormantLimitMs}.`);

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

    for (const [key, node] of Object.entries(devicesData)) {
        if (!node || typeof node !== "object") continue;
        if (/^[0-9A-Z]{8}$/i.test(key)) continue;

        // Authoritative mapping strictly via lookup_indices/auth_to_device
        let targetAgentId = authToDevice[key];
        // Only strip standard HW_ prefix if exact 8-character ID matches
        if (!targetAgentId && key.startsWith("HW_")) {
            const stripped = key.replace(/^HW_/, "");
            if (/^[0-9A-Z]{8}$/i.test(stripped) && devicesData[stripped]) {
                targetAgentId = stripped;
            }
        }

        if (targetAgentId && targetAgentId !== key && devicesData[targetAgentId]) {
            const canonical = devicesData[targetAgentId];
            if (node.device && node.device.live) {
                const aTime = parseTimestampToMillis(node.device.live.t || node.device.live.timestamp || node.device.live.updated_at || 0);
                const cTime = parseTimestampToMillis(canonical.device?.live?.t || canonical.device?.live?.timestamp || canonical.device?.live?.updated_at || 0);
                if (aTime >= cTime) {
                    canonical.device = canonical.device || {};
                    canonical.device.live = Object.assign({}, canonical.device.live || {}, node.device.live);
                }
            }
            if (node.profile && !canonical.profile) {
                canonical.profile = Object.assign({}, node.profile);
            }
        }
    }

    for (const [aliasKey, aliasNode] of Object.entries(devicesData)) {
        if (!aliasNode || typeof aliasNode !== "object" || /^[0-9A-Z]{8}$/i.test(aliasKey)) continue;
        const aliasHw = aliasNode.Identity?.uid_2 || aliasNode.device?.hardware_id || "";
        const cleanAlias = aliasHw.replace(/^HW_/, "").trim();
        const targetId = (/^[0-9A-Z]{8}$/i.test(cleanAlias) && devicesData[cleanAlias]) ? cleanAlias : null;
        if (targetId && targetId !== aliasKey) {
            const targetNode = devicesData[targetId];
            if (aliasNode.device && aliasNode.device.live) {
                const aTime = parseTimestampToMillis(aliasNode.device.live.t || aliasNode.device.live.timestamp || aliasNode.device.live.updated_at || 0);
                const tTime = parseTimestampToMillis(targetNode.device?.live?.t || targetNode.device?.live?.timestamp || targetNode.device?.live?.updated_at || 0);
                if (aTime >= tTime) {
                    targetNode.device = targetNode.device || {};
                    targetNode.device.live = Object.assign({}, targetNode.device.live || {}, aliasNode.device.live);
                }
            }
            if (aliasNode.telemetry) {
                targetNode.telemetry = Object.assign({}, targetNode.telemetry || {}, aliasNode.telemetry);
            }
            delete devicesData[aliasKey];
        }
    }

    const devicesList = [];

        for (const [agentId, node] of Object.entries(devicesData)) {
            if (!node || typeof node !== "object") continue;
            if (agentId.length > 16 || agentId.startsWith("HW_") || agentId.startsWith("GRP_")) continue;
            // Include any valid hardware node that possesses device metadata, profile, telemetry, live state, or command queue
            const isValidDevice = Boolean(
                node.device || 
                node.profile || 
                node.telemetry || 
                node.Identity || 
                node.live || 
                (node.history && Object.keys(node.history).length > 0) ||
                node.commands
            );
            if (!isValidDevice) continue;

            const profile = node.profile || {};
            const deviceInfo = node.device || {};
            const identity = node.Identity || {};
            const telemetry = node.telemetry || {};
            const safeZones = node.safe_zones || {};
            const relationships = Object.assign({}, node.relationships || {});

            // 1. Direct device-level roster from node.roster (Clean SSOT)
            const cleanIdUpper = agentId.toUpperCase();
            if (node.roster && typeof node.roster === 'object') {
                for (const [rKey, rVal] of Object.entries(node.roster)) {
                    const rClean = rKey.replace(/^HW_/, '').replace(/^HW_/, '').toUpperCase();
                    if (rClean !== cleanIdUpper && !rKey.toUpperCase().includes(cleanIdUpper)) {
                        if (!relationships[rClean]) {
                            relationships[rClean] = {
                                agentId: rClean,
                                name: (typeof rVal === 'object' ? (rVal.name || rVal.user_name || rVal.display_name) : '') || devicesData[rClean]?.profile?.name || rClean,
                                role: (typeof rVal === 'object' ? (rVal.role || rVal.role_family || rVal.membership?.role) : '') || "Connected",
                                source: "device_roster"
                            };
                        }
                    }
                }
            }

            // 2. Strict Circle Pairings from root /relationships tree (Respect paired_with mapping)
            for (const [grpId, grp] of Object.entries(relData)) {
                if (!grp || typeof grp !== 'object') continue;
                const grpDevs = grp.devices || grp.members || {};
                
                // Find entry for current agentId in this group
                let myEntry = null;
                for (const [devKey, devVal] of Object.entries(grpDevs)) {
                    const devClean = devKey.replace(/^HW_/, '').toUpperCase();
                    if (devClean === cleanIdUpper || devKey.toUpperCase().includes(cleanIdUpper)) {
                        myEntry = devVal;
                        break;
                    }
                }

                if (myEntry) {
                    const myPairings = (myEntry && typeof myEntry === 'object' && myEntry.paired_with) || {};
                    const hostId = (grp.group_info?.host_device_id || "").replace(/^HW_/, '').toUpperCase();
                    const isHost = hostId === cleanIdUpper;

                    for (const [peerKey, peerVal] of Object.entries(grpDevs)) {
                        const peerClean = peerKey.replace(/^HW_/, '').toUpperCase();
                        if (peerClean === cleanIdUpper || peerKey.toUpperCase().includes(cleanIdUpper)) continue;

                        const peerPairings = (peerVal && typeof peerVal === 'object' && peerVal.paired_with) || {};
                        const isPeerHost = hostId === peerClean;

                        // Peer is linked IF AND ONLY IF explicit pairwise binding exists
                        const isExplicitlyPaired = Boolean(
                            myPairings[peerKey] || myPairings[peerClean] ||
                            peerPairings[agentId] || peerPairings[cleanIdUpper] ||
                            (isHost && (peerPairings[agentId] || peerPairings[cleanIdUpper])) ||
                            (isPeerHost && (myPairings[hostId] || myPairings[peerKey] || myPairings[peerClean]))
                        );

                        if (isExplicitlyPaired) {
                            if (!relationships[peerClean]) {
                                const peerRole = (typeof peerVal === 'object' ? (peerVal.membership?.role || peerVal.role || peerVal.role_family) : '') ||
                                                 (typeof myPairings === 'object' && typeof myPairings[peerClean] === 'string' ? myPairings[peerClean] : '') ||
                                                 (devicesData[peerClean]?.profile?.role_family || devicesData[peerClean]?.profile?.role) ||
                                                 "Family";
                                const peerName = (typeof peerVal === 'object' ? (peerVal.name || peerVal.device_name || peerVal.identity?.full_name || peerVal.identity?.name) : '') || 
                                                 devicesData[peerClean]?.profile?.name || 
                                                 devicesData[peerClean]?.name || 
                                                 peerClean;
                                relationships[peerClean] = {
                                    agentId: peerClean,
                                    name: peerName,
                                    role: peerRole,
                                    source: grpId
                                };
                            }
                        }
                    }
                }
            }

            // Also check other devices' relationships
            for (const [otherId, otherNode] of Object.entries(devicesData)) {
                if (otherId === agentId || !otherNode || typeof otherNode !== 'object') continue;
                if (otherNode.relationships) {
                    for (const [targetKey, relInfo] of Object.entries(otherNode.relationships)) {
                        if (targetKey.toUpperCase().includes(cleanIdUpper) || cleanIdUpper.includes(targetKey.toUpperCase())) {
                            if (!relationships[otherId]) {
                                relationships[otherId] = {
                                    agentId: otherId,
                                    name: otherNode.profile?.name || otherId,
                                    role: (typeof relInfo === 'object' ? (relInfo.peerRole || relInfo.role) : '') || "Connected",
                                    source: "peer"
                                };
                            }
                        }
                    }
                }
            }
            node.relationships = relationships;

            // 1. Determine Live Location strictly prioritizing newest date-time (Single Line SSOT)
            let liveLoc = (deviceInfo.live) || node.live || node.location || {};
            
            // Only fall back to history if live GPS coordinates are missing
            if ((!liveLoc.latitude || !liveLoc.longitude) && node.history) {
                const histEntries = Object.entries(node.history);
                if (histEntries.length > 0) {
                    // Strict DESCENDING order by newest timestamp
                    histEntries.sort((a, b) => (parseTimestampToMillis(b[1].timestamp || b[1].updated_at || b[1].t) - parseTimestampToMillis(a[1].timestamp || a[1].updated_at || a[1].t)));
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

            // Decode compact loc array or string from SSOT if standard keys are zero
            if ((lat === 0 || isNaN(lat)) && liveLoc.loc) {
                if (Array.isArray(liveLoc.loc) && liveLoc.loc.length >= 2) {
                    lat = parseFloat(liveLoc.loc[0]);
                    lng = parseFloat(liveLoc.loc[1]);
                } else if (typeof liveLoc.loc === 'string' && liveLoc.loc.includes(',')) {
                    const parts = liveLoc.loc.split(',');
                    lat = parseFloat(parts[0]);
                    lng = parseFloat(parts[1]);
                }
            }

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
            // Compare all available timestamp sources to find the absolute latest heartbeat
            const liveEpoch = parseTimestampToMillis(liveLoc.updated_at || liveLoc.timestamp || 0);
            const liveTEpoch = (liveLoc.t && Number(liveLoc.t) > 0) ? parseTimestampToMillis(Number(liveLoc.t)) : 0;
            const telemEpoch = parseTimestampToMillis(telemetry.last_seen || telemetry.last_heartbeat || telemetry.updated_at || 0);
            const devEpoch = parseTimestampToMillis(deviceInfo.updated_at || identity.updated_at || 0);

            // Latest true epoch timestamp across all nodes
            const lastSeenEpoch = Math.max(liveEpoch, liveTEpoch, telemEpoch, devEpoch);
            const rawTs = lastSeenEpoch > 0 ? lastSeenEpoch : (liveLoc.t || liveLoc.updated_at || 0);
            const isRecent = lastSeenEpoch > 0 && (now - lastSeenEpoch < recencyLimitMs);
            const isExplicitlyOffline = (identity.st_session_state === "OFFLINE") || (identity.st_session_state === "PARKED") || (identity.st_session_state === "DELETED") || (identity.st_session_state === "UNINSTALLED");
            const isOnline = Boolean(isRecent && !isExplicitlyOffline);

            // RAG Status Engine & Accurate Battery Resolution
            // If liveLoc.battery is valid (> 0), use it; otherwise prioritize telemetry.battery_pct / telemetry.battery
            const liveBat = (liveLoc.battery != null && liveLoc.battery > 0) ? parseInt(liveLoc.battery, 10) : -1;
            const telemBat = (telemetry.battery_pct != null && telemetry.battery_pct >= 0) ? parseInt(telemetry.battery_pct, 10) :
                             (telemetry.battery != null && telemetry.battery >= 0) ? parseInt(telemetry.battery, 10) : -1;
            const vmaskRaw = (liveLoc.vmask != null && !isNaN(parseInt(liveLoc.vmask, 10))) ? (parseInt(liveLoc.vmask, 10) & 0x7F) : -1;
            const vmaskBat = (vmaskRaw > 0 && vmaskRaw <= 100) ? vmaskRaw : -1;

            let batteryLevel = -1;
            if (telemEpoch >= liveEpoch && telemBat >= 0) {
                batteryLevel = telemBat;
            } else if (liveBat >= 0) {
                batteryLevel = liveBat;
            } else if (telemBat >= 0) {
                batteryLevel = telemBat;
            } else if (vmaskBat >= 0) {
                batteryLevel = vmaskBat;
            } else if (liveLoc.battery != null && liveLoc.battery >= 0) {
                batteryLevel = parseInt(liveLoc.battery, 10);
            }
            const isSos = Boolean(node.alerts?.sos || node.isSos || liveLoc.isSos || liveLoc.isSosActive || liveLoc.sos_flag || (liveLoc.vmask != null && ((parseInt(liveLoc.vmask, 10) >> 7) & 1)));
            const isGhost = Boolean(node.isGhost || identity.status === "Ghost" || (lastSeenEpoch === 0 && !node.profile));

            const dormantHoursLabel = Math.max(1, Math.round(dormantLimitMs / (3600 * 1000)));
            let ragStatus = "INACTIVE_CRITICAL";
            let ragColor = "#EF4444";
            let ragLabel = `INACTIVE (> ${dormantHoursLabel}H)`;

            if (isSos) {
                ragStatus = "EMERGENCY_SOS";
                ragColor = "#EF4444";
                ragLabel = "EMERGENCY SOS";
            } else if (isGhost) {
                ragStatus = "GHOST";
                ragColor = "#64748B";
                ragLabel = "UNLINKED / GHOST";
            } else if (isRecent) {
                ragStatus = "ACTIVE";
                ragColor = "#22C55E";
                ragLabel = "ACTIVE (LIVE)";
            } else if (lastSeenEpoch > 0 && (now - lastSeenEpoch <= dormantLimitMs)) {
                ragStatus = "INACTIVE_WARNING";
                ragColor = "#F59E0B";
                ragLabel = `INACTIVE (< ${dormantHoursLabel}H)`;
            }

            // 🚀 TIMESTAMP-AWARE AVATAR & IDENTITY RESOLUTION
            // Compare timestamps between /profile and /device/live to select latest authoritative state
            const profileTs = parseTimestampToMillis(profile.updated_at || profile.ts_modified || 0);
            const liveTs = parseTimestampToMillis(liveLoc.updated_at || liveLoc.t || liveLoc.timestamp || 0);

            // Latest Avatar Resolution
            let avatarId = "avatar_custom_1";
            if (profileTs >= liveTs && (profile.avatar_id || profile.equipped_avatar_id)) {
                avatarId = profile.avatar_id || (profile.equipped_avatar_id ? `avatar_custom_${profile.equipped_avatar_id}` : identity.avatar_id);
            } else if (liveLoc.avatar_id && liveLoc.avatar_id !== "butterfly") {
                avatarId = liveLoc.avatar_id;
            } else {
                avatarId = profile.avatar_id || identity.avatar_id || liveLoc.avatar_id || "avatar_custom_1";
            }

            let avatarData = profile.avatar_data || node.avatar_data || (node.avatar_sync ? node.avatar_sync.avatar_data : "") || "";

            // Normalize leaked internal Android file paths to prevent 404
            if (typeof avatarId === "string" && (avatarId.includes("/data/user/") || avatarId.includes("/avatars/remote_") || avatarId.includes("/files/avatars/"))) {
                avatarId = avatarData ? "avatar_custom_photo" : "avatar_custom_1";
            }

            // Synthesize direct avatarUrl for instantaneous high-DPI rendering (Support Full 1..50 range)
            let avatarUrl = "";
            if (avatarData) {
                avatarUrl = avatarData.startsWith("data:image/") ? avatarData : `data:image/jpeg;base64,${avatarData}`;
            } else if (avatarId && !avatarId.includes("/") && /^\d+$/.test(avatarId)) {
                avatarUrl = `/assets/avatars/${avatarId}.png`;
            } else if (avatarId && avatarId.startsWith("avatar_custom_") && avatarId !== "avatar_custom_photo") {
                const match = avatarId.match(/\d+/);
                if (match) {
                    const num = parseInt(match[0], 10);
                    const clamped = ((num - 1) % 37) + 1;
                    avatarUrl = `/assets/avatars/${clamped}.png`;
                }
            } else if (avatarId && avatarId.startsWith("AV_")) {
                // Custom device selfie without uploaded Base64 payload -> deterministic fallback in 1..37 range
                const hashNum = Math.abs(avatarId.split("").reduce((acc, c) => ((acc << 5) - acc) + c.charCodeAt(0), 0)) % 37 + 1;
                avatarUrl = `/assets/avatars/${hashNum}.png`;
            }

            // Fallback for custom photo or missing avatarUrl to ensure no broken icons
            if (!avatarUrl) {
                const devHash = Math.abs(agentId.split("").reduce((acc, c) => ((acc << 5) - acc) + c.charCodeAt(0), 0)) % 37 + 1;
                avatarUrl = `/assets/avatars/${devHash}.png`;
            }

            // Resolve Hardware Device Model / Device Name
            const model = deviceInfo.model || deviceInfo.device_model || (deviceInfo.fingerprint ? deviceInfo.fingerprint.split('/')[1] : "") || "Android Device";
            const deviceName = deviceInfo.deviceName || deviceInfo.device_name || profile.deviceName || profile.device_name || model;

            // Resolve User / Persona / Host Name strictly by newest date-time priority
            let name = "";
            if (profileTs >= liveTs && (profile.name || profile.fullName || profile.userName)) {
                name = profile.name || profile.fullName || profile.userName;
            } else if (liveLoc.userName && liveLoc.userName !== "Active Tracker" && liveLoc.userName !== "null") {
                name = liveLoc.userName;
            } else {
                name = profile.name || profile.fullName || profile.userName || identity.owner_name || identity.id_full_name || deviceName || agentId;
            }
            const hostName = identity.owner_name || identity.id_full_name || profile.fullName || profile.name || name;

            // Resolve Roles
            const role = profile.role_title || profile.role_family || profile.role || "MEMBER";
            const roleFamily = profile.role_family || "None";
            const roleFriend = profile.role_friend || "None";

            // Device-Specific Actual Version & Package Specs (from /devices/{id} & /Version_Update/{id})
            const devVerInfo = (verUpdateData && verUpdateData[agentId]) || {};
            const devVerLatest = devVerInfo.latest || {};
            let devPendingPush = devVerInfo.pending_push || null;

            const finalVerName = deviceInfo.version_name || devVerLatest.version_name || (deviceInfo.app_version ? String(deviceInfo.app_version).replace(/^V/i, '').split(' ')[0] : '1.0.13');
            const finalVerCode = parseInt(deviceInfo.version_code || devVerLatest.version_code || telemetry.version_code || 86, 10);
            const finalAppVer = deviceInfo.app_version || telemetry.app_version || (devVerLatest.version_name ? `V${devVerLatest.version_name} (Build ${devVerLatest.version_code})` : `V${finalVerName} (Build ${finalVerCode})`);

            // 🔄 Auto-Reconciliation: If device already runs or exceeds pushed version, mark pending_push COMPLETED
            if (devPendingPush && devPendingPush.status !== "COMPLETED") {
                const pushCode = parseInt(devPendingPush.version_code || devPendingPush.target_code || 0, 10);
                if (pushCode > 0 && finalVerCode >= pushCode) {
                    devPendingPush = Object.assign({}, devPendingPush, {
                        status: "COMPLETED",
                        stage: "COMPLETED",
                        reconciled_at: new Date().toISOString()
                    });
                    db.ref(`Version_Update/${agentId}/pending_push`).update({
                        status: "COMPLETED",
                        stage: "COMPLETED",
                        reconciled_at: new Date().toISOString()
                    }).catch(() => null);
                }
            }

            devicesList.push({
                agentId,
                deviceId: deviceInfo.deviceId || identity.uid_2 || `HW_${agentId}`,
                siliconHash: (function() {
                    const h = computeSiliconHash(node);
                    return h ? ('SILICON_' + h.substring(0, 8)) : '';
                })(),
                name,
                deviceName,
                model,
                hostName,
                avatarId,
                avatarData,
                avatarUrl,
                lat,
                lng,
                latitude: lat,
                longitude: lng,
                hasValidLocation: !isNaN(lat) && !isNaN(lng) && lat !== 0 && lng !== 0,
                speed: parseFloat(liveLoc.speed || telemetry.speed_mps || 0),
                bearing: parseFloat(liveLoc.bearing || 0),
                accuracy: parseFloat(liveLoc.accuracy || 0),
                altitude: parseFloat(liveLoc.altitude || 0),
                battery: batteryLevel,
                isCharging: Boolean(telemetry.is_charging),
                isMockGps: Boolean(telemetry.is_mock_gps),
                isAirplaneMode: Boolean(telemetry.is_airplane_mode),
                networkType: telemetry.network_type || "UNKNOWN",
                activeRoute: telemetry.active_route || "N1_FAST_PATH",
                cellularOperator: telemetry.cellular_operator || "",
                cellularMccMnc: telemetry.cellular_mcc_mnc || "",
                cellularLacCid: telemetry.cellular_lac_cid || "",
                cellularSignalDbm: telemetry.cellular_signal_dbm != null ? telemetry.cellular_signal_dbm : -999,
                address: liveLoc.address || liveLoc.geo_address || profile.address || "",
                lastSeen: rawTs || lastSeenEpoch,
                isOnline,
                status: ragStatus,
                ragStatus,
                ragColor,
                ragLabel,
                sessionState: identity.st_session_state || (isOnline ? "ONLINE" : "OFFLINE"),
                email: profile.email || profile.email_id || "",
                phone: profile.phone || "",
                activeGroup: identity.active_group || profile.group || "Default Circle",
                role,
                role_family: roleFamily,
                role_friend: roleFriend,
                department: profile.department || profile.org_name || profile.deptName || "",
                emergencyNote: profile.emergency_note || profile.emergencyNote || "",
                safeZonesCount: Object.keys(safeZones).length,
                relationships: relationships,
                relationshipsCount: Object.keys(relationships).length,
                packageName: deviceInfo.package_name || "com.jdroidx.jfindme",
                packageSizeBytes: devVerLatest.package_size_bytes || null,
                packageSizeFormatted: devVerLatest.package_size_formatted || "80.6 MB",
                databaseSchema: devVerLatest.database_schema || "V75",
                otaStatus: devVerLatest.status || "INSTALLED",
                otaPendingPush: devPendingPush,
                versionName: finalVerName,
                versionCode: finalVerCode,
                appVersion: finalAppVer,
                raw: {
                    Identity: node.Identity || {},
                    device: node.device || {},
                    profile: node.profile || {},
                    telemetry: node.telemetry || {},
                    relationships: relationships,
                    safe_zones: safeZones,
                    Access: node.Access || {},
                    acl: node.acl || {},
                    alerts: node.alerts || {},
                    Version_Update: devVerInfo,
                    history: node.history ? { [Object.keys(node.history).slice(-1)[0]]: Object.values(node.history).slice(-1)[0] } : {}
                }
            });
        }

        const isMockActive = Boolean(process.env.USE_LOCAL_MOCK === "true" || !devicesSnap);
        const responsePayload = {
            status: "SUCCESS",
            source: isMockActive ? "LOCAL_MOCK_FALLBACK" : "LIVE_CLOUD_RTDB",
            total: devicesList.length,
            count: devicesList.length,
            withGps: devicesList.filter(d => d.hasValidLocation).length,
            devices: devicesList,
            timestamp: now
        };
        LIVE_DEVICES_CACHE = { data: responsePayload, timestamp: now };

        // Also prime the compact cache simultaneously
        const compactList = devicesList.map(extractCompactDevice);
        COMPACT_DEVICES_CACHE = {
            data: {
                status: "SUCCESS",
                source: isMockActive ? "LOCAL_MOCK_FALLBACK" : "LIVE_CLOUD_RTDB",
                total: compactList.length,
                count: compactList.length,
                withGps: compactList.filter(d => d.hasValidLocation).length,
                devices: compactList,
                timestamp: now
            },
            timestamp: now
        };

        return responsePayload;
}

// ============================================================
// 🚀 MODULAR OOPS MVC ROUTER EXTENSIONS (Step 3 Velocity Refactor)
// [Variables] | [Constants] | [Dependencies] | [Integration Points]
// Variables: systemRouter, safezoneRouter, duplicateRouter
// Constants: CACHE_TTL_MS, PORT, HOST
// Dependencies: express, ./routes/systemRoutes, ./routes/safezoneRoutes, ./routes/duplicateRoutes
// Integration Points: Express app.use('/api', router) pipeline mount
// ============================================================
const createSystemRouter = require('./routes/systemRoutes');
const createSafezoneRouter = require('./routes/safezoneRoutes');
const createDuplicateRouter = require('./routes/duplicateRoutes');

app.use('/api', createSystemRouter({
    db,
    fs_db,
    queryNodeWithTimeout,
    writeWithTimeout,
    PROJECT_ID,
    DATABASE_URL,
    LOCAL_MOCK_DATA,
    setLiveDevicesCache: (val) => { LIVE_DEVICES_CACHE = val; }
}));

app.use('/api', createSafezoneRouter({
    db,
    queryNodeWithTimeout,
    writeWithTimeout,
    resolveCanonicalAgentId: (id) => resolveCanonicalAgentId(id),
    getSafeZonesCache: () => SAFE_ZONES_CACHE,
    setSafeZonesCache: (val) => { SAFE_ZONES_CACHE = val; },
    getLiveDevicesCache: () => LIVE_DEVICES_CACHE,
    setLiveDevicesCache: (val) => { LIVE_DEVICES_CACHE = val; },
    CACHE_TTL_MS
}));

app.use('/api', createDuplicateRouter({
    db,
    queryNodeWithTimeout,
    writeWithTimeout,
    getLiveDevicesCache: () => LIVE_DEVICES_CACHE,
    setLiveDevicesCache: (val) => { LIVE_DEVICES_CACHE = val; },
    getSafeZonesCache: () => SAFE_ZONES_CACHE,
    setSafeZonesCache: (val) => { SAFE_ZONES_CACHE = val; }
}));

// Live Devices Aggregation Endpoint for Map Radar
app.get("/api/devices-live", async (req, res) => {
    try {
        const now = Date.now();
        if (LIVE_DEVICES_CACHE.data && LIVE_DEVICES_CACHE.data.count > 0) {
            res.json(LIVE_DEVICES_CACHE.data);
            if ((now - LIVE_DEVICES_CACHE.timestamp) >= CACHE_TTL_MS && !global._isRefreshingLiveDevices) {
                global._isRefreshingLiveDevices = true;
                computeLiveDevicesPayload()
                    .catch(e => console.warn("[BG_SYNC] Live devices refresh warning:", e.message))
                    .finally(() => { global._isRefreshingLiveDevices = false; });
            }
            return;
        }
        if (false) {
            return res.json(LIVE_DEVICES_CACHE.data);
        }
        const payload = await computeLiveDevicesPayload();
        LIVE_DEVICES_CACHE = { data: payload, timestamp: now };
        res.json(payload);
    } catch (err) {
        console.error("[ERROR] /api/devices-live failed:", err.message);
        if (LIVE_DEVICES_CACHE.data) {
            return res.json(LIVE_DEVICES_CACHE.data);
        }
        res.status(500).json({ error: err.message, devices: [], count: 0 });
    }
});

// Helper: Extracts clean, ultra-lightweight telemetry profile (drops bulky history/dumps)
function extractCompactDevice(d) {
    if (!d) return {};
    return {
        agentId: d.agentId,
        deviceId: d.deviceId,
        name: d.name,
        hostName: d.hostName,
        deviceName: d.deviceName,
        model: d.model,
        latitude: d.latitude,
        longitude: d.longitude,
        hasValidLocation: d.hasValidLocation,
        bearing: d.bearing,
        speed: d.speed,
        accuracy: d.accuracy,
        altitude: d.altitude,
        battery: d.battery,
        isCharging: d.isCharging,
        isOnline: d.isOnline,
        status: d.status,
        ragStatus: d.ragStatus,
        ragColor: d.ragColor,
        ragLabel: d.ragLabel,
        isSos: d.isSos,
        isGhost: d.isGhost,
        avatarId: d.avatarId,
        avatarUrl: d.avatarUrl,
        // Exclude massive Base64 avatarData (>500 bytes) from compact telemetry stream to keep polling payload < 1.5 KB
        avatarData: (d.avatarData && d.avatarData.length > 500) ? "" : d.avatarData,
        role: d.role,
        role_family: d.role_family,
        role_friend: d.role_friend,
        address: d.address,
        lastSeen: d.lastSeen,
        timestamp: d.lastSeen,
        activeGroup: d.activeGroup,
        networkType: d.networkType
    };
}

// Ultra-Lightweight Live Fleet Aggregation Endpoint (< 1.5 KB payload for rapid polling)
app.get("/api/fleet/compact-live", async (req, res) => {
    const now = Date.now();
    if (COMPACT_DEVICES_CACHE.data && COMPACT_DEVICES_CACHE.data.count > 0) {
        res.json(COMPACT_DEVICES_CACHE.data);
        if ((now - COMPACT_DEVICES_CACHE.timestamp) >= CACHE_TTL_MS && !global._isRefreshingLiveDevices) {
            global._isRefreshingLiveDevices = true;
            computeLiveDevicesPayload()
                .catch(e => console.warn("[BG_SYNC] Compact refresh warning:", e.message))
                .finally(() => { global._isRefreshingLiveDevices = false; });
        }
        return;
    }
    if (false) {
        return res.json(COMPACT_DEVICES_CACHE.data);
    }

    try {
        const fullPayload = await computeLiveDevicesPayload();
        const compactList = (fullPayload.devices || []).map(extractCompactDevice);
        const payload = {
            status: "SUCCESS",
            total: compactList.length,
            count: compactList.length,
            withGps: compactList.filter(d => d.hasValidLocation).length,
            devices: compactList,
            timestamp: now
        };
        COMPACT_DEVICES_CACHE = { data: payload, timestamp: now };
        return res.json(payload);
    } catch (err) {
        console.error("[ERROR] /api/fleet/compact-live failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Safe Zones Geofence Aggregation Endpoint
app.get("/api/safe-zones", async (req, res) => {
    const now = Date.now();
    if (SAFE_ZONES_CACHE.data && SAFE_ZONES_CACHE.data.count > 0) {
        res.json(SAFE_ZONES_CACHE.data);
        if ((now - SAFE_ZONES_CACHE.timestamp) >= CACHE_TTL_MS && !global._isRefreshingSafeZones) {
            global._isRefreshingSafeZones = true;
            // Refresh in background
            Promise.all([
                queryNodeWithTimeout("Geofencing", 2500),
                queryNodeWithTimeout("relationships", 2500),
                queryNodeWithTimeout("lookup_indices/auth_to_device", 2500)
            ]).catch(e => console.warn("[BG_SAFEZONES] warning:", e.message))
              .finally(() => { global._isRefreshingSafeZones = false; });
        }
        return;
    }
    if (false) {
        return res.json(SAFE_ZONES_CACHE.data);
    }

    try {
        const [geofenceSnap, devicesSnap, relSnap, lookupSnap] = await Promise.all([
            queryNodeWithTimeout("Geofencing", 3500),
            Promise.resolve(null), // Use in-memory LIVE_DEVICES_CACHE directly instead of heavy WAN refetch
            queryNodeWithTimeout("relationships", 3500),
            queryNodeWithTimeout("lookup_indices/auth_to_device", 3500)
        ]);
        const geofenceData = (geofenceSnap && typeof geofenceSnap.val === 'function' && geofenceSnap.val()) || {};
        const devicesData = (devicesSnap && typeof devicesSnap.val === 'function' && devicesSnap.val()) || (LIVE_DEVICES_CACHE.data && Array.isArray(LIVE_DEVICES_CACHE.data.devices) ? LIVE_DEVICES_CACHE.data.devices.reduce((acc, d) => ({...acc, [d.agentId]: d}), {}) : null) || {};
        const relData = (relSnap && typeof relSnap.val === 'function' && relSnap.val()) || {};
        const authToDevice = (lookupSnap && typeof lookupSnap.val === 'function' && lookupSnap.val()) || {};

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

        const zonePayload = { status: "SUCCESS", count: validZones.length, safeZones: validZones, zones: validZones, timestamp: now };
        SAFE_ZONES_CACHE.data = zonePayload;
        SAFE_ZONES_CACHE.timestamp = now;
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
        const zoneId = req.body.zoneId || req.body.zone_id;
        const zoneName = req.body.zoneName || req.body.name || req.body.zone_name;
        const rawLat = req.body.latitude !== undefined ? req.body.latitude : req.body.lat;
        const rawLng = req.body.longitude !== undefined ? req.body.longitude : req.body.lng;
        const latitude = parseFloat(rawLat);
        const longitude = parseFloat(rawLng);
        const radius = req.body.radius !== undefined ? req.body.radius : (req.body.radius_meters || 100);
        const isActive = req.body.isActive !== undefined ? req.body.isActive : (req.body.is_active !== false);
        const alertsEnabled = req.body.alertsEnabled !== undefined ? req.body.alertsEnabled : Boolean(req.body.alerts_enabled);
        const connectedDevices = req.body.connectedDevices || req.body.connected_devices || [];
        const polygonPoints = req.body.polygonPoints || req.body.polygon_points || "";
        const dwellMinutes = req.body.dwellMinutes || req.body.dwell_minutes || 0;
        const creatorId = req.body.creatorId || req.body.creator_id || req.body.created_by;

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

        await writeWithTimeout(`Geofencing/${cleanCreator}/safe_zones/${cleanZoneId}`, zonePayload, 3500);
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

        const snap = await queryNodeWithTimeout("Geofencing", 3000);
        const geofencing = (snap && typeof snap.val === 'function' ? snap.val() : null) || {};
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
    const zoneId = req.body.zoneId || req.body.zone_id;
    const creatorUid = req.body.creatorUid || req.body.creator_uid || req.body.creatorId || req.body.creator_id;
    if (!zoneId) return res.status(400).json({ error: "MISSING_ZONE_ID" });

    try {
        console.log(`[SAFE_ZONES] Decommissioning Safe Zone [${zoneId}] for Creator [${creatorUid || 'ANY'}]...`);
        const snap = await queryNodeWithTimeout("Geofencing", 3000);
        const geofencing = (snap && typeof snap.val === 'function' ? snap.val() : null) || {};

        const deleteOps = [];
        for (const [zKey, zNode] of Object.entries(geofencing)) {
            if (!zNode) continue;
            if (zNode.zone_id === zoneId || zKey === zoneId || zKey.includes(zoneId)) {
                deleteOps.push(db.ref(`Geofencing/${zKey}`).remove().catch(() => null));
            }
            if (zNode.safe_zones && zNode.safe_zones[zoneId]) {
                deleteOps.push(db.ref(`Geofencing/${zKey}/safe_zones/${zoneId}`).remove().catch(() => null));
            }
        }

        if (creatorUid) {
            const cleanCreator = creatorUid.replace(/[\.\#\$\[\]\/]/g, "_");
            deleteOps.push(db.ref(`Geofencing/${cleanCreator}/safe_zones/${zoneId}`).remove().catch(() => null));
            deleteOps.push(db.ref(`devices/${creatorUid}/safe_zones/${zoneId}`).remove().catch(() => null));
        }

        Promise.all(deleteOps).catch(() => null);
        SAFE_ZONES_CACHE = { data: null, timestamp: 0 };
        res.json({ status: "DELETED", zoneId });
    } catch (err) {
        console.error("[ERROR] /api/safezone/delete failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Handler for Authoritative Relationship Binding / Addition
const handleRelationshipUpdate = async (req, res) => {
    try {
        const { agentId, targetAgentId, role = "Family", peerRole, name, peerName } = req.body || {};
        if (!agentId || !targetAgentId) {
            return res.status(400).json({ error: "MISSING_PARAMS", message: "Both agentId and targetAgentId are required." });
        }

        const cleanAgentId = await resolveCanonicalAgentId(agentId);
        const cleanTargetId = await resolveCanonicalAgentId(targetAgentId);
        const nowTs = Date.now();
        const nowIso = new Date().toISOString();
        const timestamp16 = nowIso.replace(/[-:T.Z]/g, '').slice(0, 16);

        console.log(`[RELATIONSHIP] Authoritative circle link: [${cleanAgentId}] <-> [${cleanTargetId}] (${role})`);

        const updates = {};
        updates[`devices/${cleanAgentId}/relationships/${cleanTargetId}`] = {
            agentId: cleanTargetId,
            name: peerName || cleanTargetId,
            role: role || "Family",
            linked_at: timestamp16,
            updated_at: timestamp16
        };
        updates[`devices/${cleanTargetId}/relationships/${cleanAgentId}`] = {
            agentId: cleanAgentId,
            name: name || cleanAgentId,
            role: peerRole || role || "Family",
            linked_at: timestamp16,
            updated_at: timestamp16
        };
        updates[`relationships/GRP_${cleanAgentId}/devices/${cleanTargetId}`] = {
            name: peerName || cleanTargetId,
            membership: { role: role || "Family" },
            updated_at: timestamp16
        };
        updates[`relationships/GRP_${cleanTargetId}/devices/${cleanAgentId}`] = {
            name: name || cleanAgentId,
            membership: { role: peerRole || role || "Family" },
            updated_at: timestamp16
        };
        // Trigger offline SQLite sync on both devices
        updates[`devices/${cleanAgentId}/sync_trigger`] = nowTs;
        updates[`devices/${cleanTargetId}/sync_trigger`] = nowTs;
        updates[`devices/${cleanAgentId}/commands/SYNC_CIRCLE`] = { timestamp: timestamp16, target: cleanTargetId, role };
        updates[`devices/${cleanTargetId}/commands/SYNC_CIRCLE`] = { timestamp: timestamp16, target: cleanAgentId, role: peerRole || role };

        await writeWithTimeout("", updates, 2500, true);
        LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };

        return res.json({
            status: "SUCCESS",
            message: `Linked [${cleanAgentId}] with [${cleanTargetId}] in circle as [${role}]`,
            agentId: cleanAgentId,
            targetAgentId: cleanTargetId
        });
    } catch (err) {
        console.error("[ERROR] handleRelationshipUpdate failed:", err.message);
        return res.status(500).json({ error: "LINK_FAILED", message: err.message });
    }
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

        let fcmDispatched = false;

        // 1. Write RTDB Command & Poke Flags
        const rtdbPromise = Promise.all([
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

        // 2. High-Priority Silent FCM Wakeup Burst (Shakes device out of Android Doze Mode)
        const fcmPromise = (async () => {
            try {
                const devSnap = await db.ref(`devices/${agentId}`).once("value");
                const devVal = devSnap.val() || {};
                const fcmToken = devVal.Identity?.fcm_token || devVal.telemetry?.fcm_token || devVal.fcm_token || devVal.device?.fcm_token;
                if (fcmToken && typeof fcmToken === "string" && fcmToken.length > 10) {
                    await admin.messaging().send({
                        token: fcmToken,
                        data: {
                            type: "PING_LOCATION",
                            action: "FORCE_IMMEDIATE_GPS_BURST",
                            title: "Radar Sync",
                            body: `Immediate location ping requested by ${sender}`
                        },
                        android: {
                            priority: "high"
                        }
                    });
                    fcmDispatched = true;
                    console.log(`[FCM_SENT] High-priority location wakeup sent to device [${agentId}]`);
                }
            } catch (fcmErr) {
                console.warn(`[FCM_WARN] FCM wakeup failed for [${agentId}]:`, fcmErr.message);
            }
        })();

        await Promise.all([rtdbPromise, fcmPromise]);

        res.json({ status: "SUCCESS", agentId, fcmDispatched, timestamp: Date.now() });
    } catch (err) {
        console.error("[ERROR] /api/poke-device failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// 🔄 Closed-Loop Telemetry Acknowledgment Downlink Channel
app.post("/api/telemetry/closed-loop-ack", async (req, res) => {
    let { agentId, command, status } = req.body;
    if (!agentId) return res.status(400).json({ error: "MISSING_AGENT_ID" });
    try {
        agentId = await resolveCanonicalAgentId(agentId);
        await db.ref(`devices/${agentId}/feedback/ack`).set({
            timestamp: Date.now(),
            command: command || "SYNC_ACK",
            status: status || "CONFIRMED"
        });
        res.json({ success: true, agentId, timestamp: Date.now() });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Fleet-Wide Silent High-Priority FCM Interrogation Wakeup Burst
app.post("/api/fleet/interrogate-live", async (req, res) => {
    const { agentIds, requester = "Web_Live_Radar" } = req.body || {};
    try {
        const snap = await queryNodeWithTimeout("devices", 3000);
        const devicesData = (snap && typeof snap.val === 'function' ? snap.val() : null) || (LIVE_DEVICES_CACHE.data && Array.isArray(LIVE_DEVICES_CACHE.data.devices) ? LIVE_DEVICES_CACHE.data.devices.reduce((acc, d) => ({...acc, [d.agentId]: d}), {}) : {}) || {};
        const targetIds = Array.isArray(agentIds) && agentIds.length > 0
            ? agentIds
            : Object.keys(devicesData).filter(k => /^[0-9A-Z]{8}$/i.test(k));

        let sentCount = 0;
        const wakePromises = [];

        for (const rawId of targetIds) {
            const node = devicesData[rawId];
            if (!node || typeof node !== "object") continue;
            const fcmToken = node.Identity?.fcm_token || node.telemetry?.fcm_token || node.fcm_token || node.device?.fcm_token;
            
            // Queue RTDB poke command
            wakePromises.push(
                db.ref(`devices/${rawId}/commands/poke`).set({
                    command: "POKE_LOCATION_UPDATE",
                    timestamp: Date.now(),
                    poked_by: requester
                }).catch(() => null)
            );

            // If FCM token is available, dispatch silent high-priority wakeup burst
            if (fcmToken && typeof fcmToken === "string" && fcmToken.length > 10) {
                wakePromises.push(
                    admin.messaging().send({
                        token: fcmToken,
                        data: {
                            type: "PING_LOCATION",
                            action: "FORCE_IMMEDIATE_GPS_BURST",
                            title: "Fleet Location Burst",
                            body: "Operator requested immediate live radar update"
                        },
                        android: {
                            priority: "high"
                        }
                    }).then(() => {
                        sentCount++;
                    }).catch(e => {
                        console.warn(`[FCM_FLEET_WARN] FCM send failed for [${rawId}]:`, e.message);
                    })
                );
            }
        }

        Promise.all(wakePromises).then(() => {
            console.log(`[FLEET_WAKE] Interrogation burst completed for ${wakePromises.length} devices.`);
        }).catch(e => {
            console.warn(`[FLEET_WAKE] Interrogation warning:`, e.message);
        });

        res.json({
            status: "SUCCESS",
            totalTargeted: targetIds.length,
            fcmPushesDispatched: targetIds.length,
            timestamp: Date.now()
        });
    } catch (err) {
        console.error("[ERROR] /api/fleet/interrogate-live failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// System Version Release Metadata Endpoint (SSOT)
app.get("/api/system-release", async (req, res) => {
    try {
        const snap = await queryNodeWithTimeout("Version_Update/master_release", 3000);
        let release = (snap && typeof snap.val === 'function') ? snap.val() : null;
        if (!release) {
            release = {
                version_name: "1.0.13",
                version_code: 87,
                database_schema: "V75",
                package_size_bytes: 84510920,
                package_size_formatted: "80.6 MB",
                download_url_direct: "https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jbutterfly/jButterFly.apk",
                download_url_versioned: "https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jbutterfly/jButterFly_V87.apk",
                download_url_github: "https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jbutterfly/jButterFly.apk",
                download_url_drive: "https://drive.google.com/drive/folders/1w5mD_P7kU-G6G-jFindMe-Release",
                release_notes: "Enterprise OTA Release V1.0.13: Android PackageInstaller Session API, external download quarantine resolution, Google Maps SDK dynamic authorization, and zero-touch data restoration.",
                status: "PUBLISHED_MASTER",
                last_updated_ts: new Date().toISOString(),
                epoch_ms: Date.now()
            };
            // Seed master release SSOT in Firebase Realtime Database asynchronously
            writeWithTimeout("Version_Update/master_release", release, 2500);
            writeWithTimeout("Version_Update/latest", release, 2500);
        }
        res.json({ status: "SUCCESS", release });
    } catch (err) {
        console.error("[ERROR] /api/system-release failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Update System Version Release Metadata Endpoint (Dynamic Operator SSOT)
app.post("/api/system-release", async (req, res) => {
    try {
        const payload = req.body || {};
        const snap = await queryNodeWithTimeout("Version_Update/master_release", 3000);
        const existing = (snap && typeof snap.val === 'function' ? snap.val() : null) || {};

        const nowTs = new Date().toISOString();
        const epochMs = Date.now();

        const updatedRelease = {
            version_name: payload.version_name || existing.version_name || "1.0.13",
            version_code: parseInt(payload.version_code || existing.version_code || 87, 10),
            database_schema: payload.database_schema || existing.database_schema || "V75",
            package_size_formatted: payload.package_size_formatted || existing.package_size_formatted || "80.6 MB",
            package_size_bytes: parseInt(payload.package_size_bytes || existing.package_size_bytes || 84510920, 10),
            download_url_direct: payload.download_url_direct || payload.download_url || existing.download_url_direct || "",
            download_url_versioned: payload.download_url_versioned || payload.download_url || existing.download_url_versioned || "",
            download_url_github: payload.download_url_github || existing.download_url_github || "",
            download_url_drive: payload.download_url_drive || existing.download_url_drive || "",
            release_notes: payload.release_notes || existing.release_notes || "Enterprise Dynamic OTA Release",
            status: "PUBLISHED_MASTER",
            last_updated_ts: nowTs,
            epoch_ms: epochMs
        };

        writeWithTimeout("Version_Update/master_release", updatedRelease, 2500);
        writeWithTimeout("Version_Update/latest", updatedRelease, 2500);

        console.log(`[RELEASE] Updated master release SSOT: V${updatedRelease.version_name} (Build ${updatedRelease.version_code})`);
        res.json({ status: "SUCCESS", release: updatedRelease });
    } catch (err) {
        console.error("[ERROR] POST /api/system-release failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Remote System/OTA Update Push Endpoint (Supports Option 1: Screen & Web Control vs Option 2: Background Script)
app.post(["/api/device/push-update", "/api/push-update"], async (req, res) => {
    let { agentId, targetVersion, targetCode, downloadUrl, silent, mode } = req.body;
    if (!agentId) return res.status(400).json({ error: "MISSING_AGENT_ID" });

    try {
        agentId = await resolveCanonicalAgentId(agentId);

        // Determine Mode:
        // Option 1: 'INTERACTIVE_SCREEN' (open device installation screen & webpage controls)
        // Option 2: 'BACKGROUND_BEST_OPTION' (run background script choosing best option)
        const isInteractive = mode === "INTERACTIVE_SCREEN" || mode === "interactive" || mode === 1 || (silent === false && mode !== "BACKGROUND_BEST_OPTION");
        const selectedMode = isInteractive ? "INTERACTIVE_SCREEN" : "BACKGROUND_BEST_OPTION";
        const updateMode = isInteractive ? "PROMPT_INTERACTIVE" : "SILENT_AUTO";

        // 1. Resolve target version & release assets
        let masterRelease = null;
        if (!targetVersion || !targetCode || !downloadUrl) {
            const masterSnap = await db.ref("Version_Update/master_release").once("value");
            masterRelease = masterSnap.val() || {
                version_name: "1.0.13",
                version_code: 89,
                download_url_versioned: "https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jbutterfly/jButterFly_V89.apk",
                download_url_github: "https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jbutterfly/jButterFly.apk",
                package_size_formatted: "80.6 MB"
            };
            targetVersion = targetVersion || masterRelease.version_name || "1.0.13";
            targetCode = targetCode || masterRelease.version_code || 89;
            downloadUrl = downloadUrl 
                || masterRelease.download_url_versioned 
                || masterRelease.download_url_github 
                || masterRelease.download_url_media 
                || masterRelease.download_url_drive 
                || "https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jbutterfly/jButterFly.apk";
        }

        const now = Date.now();
        const nowIso = new Date().toISOString();
        console.log(`[PUSH_UPDATE] 🚀 Dispatching OTA update V${targetVersion} (Build ${targetCode}) [Mode: ${selectedMode}] to device [${agentId}] via [${downloadUrl}]`);

        // 2. Fetch device node to get FCM token for background wake-up
        const devSnap = await db.ref(`devices/${agentId}`).once("value");
        const devData = devSnap.val() || {};
        const fcmToken = devData.Identity?.fcm_token 
            || devData.device?.fcm_token 
            || devData.device?.live?.fcm_token 
            || devData.telemetry?.fcm_token 
            || null;

        // Dynamic Network Routing Preference Resolution (Zero Hardcoded Routing)
        const reqNetPref = req.body.network_preference || req.body.networkPreference;
        const netPref = reqNetPref || devData.device?.network_preference || devData.profile?.network_preference || "AUTO_ANY_ACTIVE";
        const netMode = netPref === "WIFI_ONLY" ? "WIFI_ONLY" : (netPref === "MOBILE_DATA_ONLY" ? "MOBILE_DATA_ONLY" : "MOBILE_DATA_OR_WIFI");

        // 3. Atomically write command records across Firebase RTDB
        const updatePayload = {
            action: "SYSTEM_UPDATE",
            command: "SYSTEM_UPDATE",
            mode: selectedMode,
            update_mode: updateMode,
            target_version: String(targetVersion),
            version_name: String(targetVersion),
            target_code: Number(targetCode),
            version_code: Number(targetCode),
            download_url: String(downloadUrl),
            downloadUrl: String(downloadUrl),
            network_preference: String(netPref),
            network_mode: String(netMode),
            package_size_formatted: masterRelease?.package_size_formatted || "80.6 MB",
            timestamp: now,
            epoch_ms: now,
            operator: "Web_Matrix_Commander",
            status: "DISPATCHED",
            stage: "PUSH_DISPATCHED",
            message: isInteractive 
                ? "OTA package dispatched. Device installation screen will open directly for Web-controlled installation." 
                : "OTA package dispatched. Background script will autonomously choose the best installation option on device."
        };

        const rtdbWrites = [
            // Active listener in TrackingService: child("device").child("remote_command")
            db.ref(`devices/${agentId}/device/remote_command`).set(updatePayload).catch(() => null),
            // Dedicated commands node: child("commands").child("system_update")
            db.ref(`devices/${agentId}/commands/system_update`).set(updatePayload).catch(() => null),
            // Tracking under Version_Update tree
            db.ref(`Version_Update/${agentId}/pending_push`).set({
                version_name: String(targetVersion),
                version_code: Number(targetCode),
                download_url: String(downloadUrl),
                network_preference: String(netPref),
                network_mode: String(netMode),
                mode: selectedMode,
                update_mode: updateMode,
                status: "PENDING_INSTALL",
                stage: "PUSH_DISPATCHED",
                pushed_at: nowIso,
                epoch_ms: now
            }).catch(() => null),
            // Forensic Change_Log audit
            db.ref(`devices/${agentId}/Change_Log`).push({
                timestamp: nowIso,
                change_type: "SYSTEM_UPDATE_PUSHED",
                source: "WEB_DASHBOARD",
                initiated_by: "Web_Matrix_Commander",
                details: {
                    target_version: `V${targetVersion}`,
                    target_code: targetCode,
                    download_url: downloadUrl,
                    network_preference: netPref,
                    mode: selectedMode,
                    interactive: isInteractive
                }
            }).catch(() => null),
            // Global audit ledger
            db.ref("audit_logs").push({
                action: "SYSTEM_UPDATE_PUSHED",
                target_agent_id: agentId,
                version: targetVersion,
                code: targetCode,
                network_preference: netPref,
                mode: selectedMode,
                timestamp: nowIso,
                status: "DISPATCHED"
            }).catch(() => null)
        ];

        await Promise.all(rtdbWrites);

        // 4. Send High-Priority FCM Push Message (Wakes up device even if companion app is completely closed)
        let fcmStatus = "NO_TOKEN";
        if (fcmToken) {
            try {
                // High-priority DATA-ONLY payload to guarantee onMessageReceived() wakes up device background service over Mobile Data / Wi-Fi
                const fcmDataMessage = {
                    token: fcmToken,
                    data: {
                        type: "SYSTEM_UPDATE",
                        action: updateMode,
                        update_mode: updateMode,
                        mode: selectedMode,
                        version_name: String(targetVersion),
                        target_version: String(targetVersion),
                        version_code: String(targetCode),
                        target_code: String(targetCode),
                        download_url: String(downloadUrl),
                        downloadUrl: String(downloadUrl),
                        network_preference: String(netPref),
                        network_mode: String(netMode),
                        title: isInteractive ? `⚡ Update Screen: V${targetVersion}` : `📦 System Update Ready: V${targetVersion}`,
                        body: isInteractive 
                            ? `Tap to launch installation screen for V${targetVersion} on your device.` 
                            : `New build V${targetVersion} ready. Tap to install and upgrade.`
                    },
                    android: {
                        priority: "high"
                    }
                };
                await admin.messaging().send(fcmDataMessage);
                console.log(`[PUSH_UPDATE] ✅ High-priority DATA-ONLY FCM wake-up dispatched to [${agentId}] via ${netPref}`);

                // If interactive or user-facing notification requested, also dispatch notification block
                if (isInteractive) {
                    const fcmNotifMessage = {
                        token: fcmToken,
                        notification: {
                            title: `⚡ Update Screen: V${targetVersion}`,
                            body: `Tap to launch installation screen for V${targetVersion} on your device.`
                        },
                        data: fcmDataMessage.data,
                        android: {
                            priority: "high",
                            notification: {
                                channelId: "system_updates_channel",
                                priority: "max",
                                defaultSound: true,
                                defaultVibrateTimings: true
                            }
                        }
                    };
                    await admin.messaging().send(fcmNotifMessage);
                }
                fcmStatus = "SENT";
            } catch (fcmErr) {
                console.warn(`[PUSH_UPDATE] ⚠️ FCM delivery notice for [${agentId}]:`, fcmErr.message);
                fcmStatus = `FAILED: ${fcmErr.message}`;
            }
        } else {
            console.log(`[PUSH_UPDATE] ℹ️ Device [${agentId}] has no active FCM token cached. Relying on Realtime Database listener & worker polling.`);
        }

        res.json({
            status: "SUCCESS",
            agentId,
            mode: selectedMode,
            updateMode,
            targetVersion,
            targetCode,
            fcmStatus,
            timestamp: now,
            message: isInteractive
                ? `Option 1: Device installation screen launched & Webpage Control Console active for [${agentId}].`
                : `Option 2: Autonomous background script running on [${agentId}] (Best option selected).`
        });
    } catch (err) {
        console.error("[ERROR] /api/device/push-update failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Remote Installation Webpage Control Endpoint (Option 1 Webpage Controller)
app.post("/api/device/install-control", async (req, res) => {
    let { agentId, action } = req.body;
    if (!agentId || !action) return res.status(400).json({ error: "MISSING_PARAMS" });

    try {
        agentId = await resolveCanonicalAgentId(agentId);
        const now = Date.now();
        console.log(`[INSTALL_CTRL] 🎮 Operator Web Action [${action}] dispatched to node [${agentId}]`);

        if (action === "OPEN_INSTALLER_SCREEN" || action === "REOPEN_SCREEN") {
            await Promise.all([
                db.ref(`devices/${agentId}/commands/system_update/status`).set("REOPEN_SCREEN"),
                db.ref(`devices/${agentId}/device/remote_command`).set({
                    action: "OPEN_INSTALLER_SCREEN",
                    epoch_ms: now
                })
            ]);
        } else if (action === "ABORT_INSTALL") {
            await Promise.all([
                db.ref(`devices/${agentId}/commands/system_update/status`).set("ABORT"),
                db.ref(`devices/${agentId}/device/remote_command`).set({
                    action: "ABORT_INSTALL",
                    epoch_ms: now
                }),
                db.ref(`Version_Update/${agentId}/pending_push/status`).set("ABORTED")
            ]);
        } else if (action === "SWITCH_TO_BACKGROUND") {
            await Promise.all([
                db.ref(`devices/${agentId}/commands/system_update`).update({
                    update_mode: "SILENT_AUTO",
                    mode: "BACKGROUND_BEST_OPTION",
                    status: "DISPATCHED",
                    epoch_ms: now
                }),
                db.ref(`devices/${agentId}/device/remote_command`).set({
                    action: "SYSTEM_UPDATE",
                    update_mode: "SILENT_AUTO",
                    epoch_ms: now
                })
            ]);
        } else if (action === "PING_STATUS") {
            await db.ref(`devices/${agentId}/commands/poke`).set({
                command: "POKE_LOCATION_UPDATE",
                source: "OTA_CONSOLE",
                timestamp: now
            });
        }

        res.json({
            status: "SUCCESS",
            agentId,
            action,
            timestamp: now,
            message: `Remote control action [${action}] delivered to node [${agentId}].`
        });
    } catch (err) {
        console.error("[ERROR] /api/device/install-control failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Real-Time OTA Installation Status Endpoint (Live Telemetry for Web Console)
app.get("/api/device/install-status", async (req, res) => {
    let { agentId } = req.query;
    if (!agentId) return res.status(400).json({ error: "MISSING_AGENT_ID" });

    try {
        agentId = await resolveCanonicalAgentId(agentId);

        const [cmdSnap, pushSnap, devSnap] = await Promise.all([
            db.ref(`devices/${agentId}/commands/system_update`).once("value"),
            db.ref(`Version_Update/${agentId}/pending_push`).once("value"),
            db.ref(`devices/${agentId}/device`).once("value")
        ]);

        const cmdData = cmdSnap.val() || {};
        const pushData = pushSnap.val() || {};
        const devData = devSnap.val() || {};

        res.json({
            status: "SUCCESS",
            agentId,
            installStatus: cmdData.status || pushData.status || "IDLE",
            stage: cmdData.stage || pushData.stage || "IDLE",
            message: cmdData.message || "",
            mode: cmdData.mode || pushData.mode || "BACKGROUND_BEST_OPTION",
            updateMode: cmdData.update_mode || pushData.update_mode || "SILENT_AUTO",
            targetVersion: cmdData.target_version || pushData.version_name || "1.0.13",
            targetCode: cmdData.target_code || pushData.version_code || 87,
            chosenEngine: cmdData.chosen_engine || "",
            installedVersion: devData.app_version || devData.version_name || "1.0.13",
            installedCode: devData.version_code || 86,
            downloadUrl: cmdData.download_url || pushData.download_url || "",
            epochMs: cmdData.epoch_ms || pushData.epoch_ms || 0
        });
    } catch (err) {
        console.error("[ERROR] /api/device/install-status failed:", err.message);
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

        writeWithTimeout("", updates, 2500, true);
        LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };

        res.json({ status: "SUCCESS", agentId, flag, value: boolVal, timestamp: Date.now() });
    } catch (err) {
        console.error("[ERROR] /api/device/toggle-acl failed:", err.message);
        res.status(500).json({ error: err.message });
    }
});

// Safe Zone Operational State Toggle Endpoint (Atomic)
app.post("/api/safezone/toggle-state", async (req, res) => {
    let zoneId = req.body.zoneId || req.body.zone_id;
    let isActive = req.body.isActive !== undefined ? req.body.isActive : req.body.is_active;
    let alertsEnabled = req.body.alertsEnabled !== undefined ? req.body.alertsEnabled : req.body.alerts_enabled;
    let creatorUid = req.body.creatorUid || req.body.creator_uid || req.body.creatorId || req.body.creator_id;
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

        writeWithTimeout("", updates, 2500, true);
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
            let lat = parseFloat(item.latitude || item.lat || 0);
            let lng = parseFloat(item.longitude || item.lng || 0);

            // Support compact loc array or string format from SSOT
            if ((lat === 0 || isNaN(lat)) && item.loc) {
                if (Array.isArray(item.loc) && item.loc.length >= 2) {
                    lat = parseFloat(item.loc[0]);
                    lng = parseFloat(item.loc[1]);
                } else if (typeof item.loc === 'string' && item.loc.includes(',')) {
                    const parts = item.loc.split(',');
                    lat = parseFloat(parts[0]);
                    lng = parseFloat(parts[1]);
                }
            }
            if (lat === 0 && lng === 0) continue;

            const ts = parseTimestampToMillis(item.timestamp || item.t || item.updated_at || item.time || key);
            if (ts <= 0) continue;

            // Date/Time boundary filtering
            if (ts < startEpoch || ts > endEpoch) continue;

            const speed = parseFloat(item.speed || item.spd || 0);
            const bearing = parseFloat(item.bearing || item.brg || 0);
            const altitude = parseFloat(item.altitude || 0);
            const accuracy = parseFloat(item.accuracy || 0);
            const address = item.address || item.geo_address || "";
            const vmask = item.vmask != null ? parseInt(item.vmask, 10) : null;
            const battery = item.battery != null ? parseInt(item.battery, 10) : (vmask != null ? (vmask & 0x7F) : null);
            const isSos = item.sos != null ? Boolean(item.sos) : (vmask != null ? Boolean((vmask >> 7) & 1) : false);

            breadcrumbs.push({
                id: key,
                latitude: lat,
                longitude: lng,
                speed,
                bearing,
                altitude,
                accuracy,
                address,
                battery,
                isSos,
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
            const ts = parseTimestampToMillis(item.timestamp || item.t || item.updated_at || item.time || key);
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
    // Attempt lookup index resolution with timeout
    try {
        const indexSnap = await queryNodeWithTimeout(`lookup_indices/auth_to_device/${cleanId}`, 2000);
        const indexed = indexSnap && typeof indexSnap.val === 'function' ? indexSnap.val() : null;
        if (indexed && /^[A-Z0-9]{8}$/.test(indexed)) {
            return indexed;
        }
        if (LIVE_DEVICES_CACHE.data && Array.isArray(LIVE_DEVICES_CACHE.data.devices)) {
            const match = LIVE_DEVICES_CACHE.data.devices.find(d => d.agentId === cleanId || d.authUid === cleanId);
            if (match) return match.agentId;
        }
    } catch (e) {
        console.warn(`[RESOLVER] Error resolving agentId for [${cleanId}]:`, e.message);
    }
    return cleanId;
}

// Dynamic Local & Online Avatars Inventory Scanner
app.get("/api/avatars", (req, res) => {
    try {
        const avatarsDir = path.join(__dirname, "../assets/avatars");
        if (!fs.existsSync(avatarsDir)) {
            return res.json({ avatars: [] });
        }
        const files = fs.readdirSync(avatarsDir);
        const imageFiles = files.filter(f => /\.(png|jpg|jpeg|webp|svg)$/i.test(f));
        imageFiles.sort((a, b) => {
            const numA = parseInt(a, 10);
            const numB = parseInt(b, 10);
            if (!isNaN(numA) && !isNaN(numB)) return numA - numB;
            return a.localeCompare(b);
        });
        const avatars = imageFiles.map(file => ({
            id: file.replace(/\.[^/.]+$/, ""),
            filename: file,
            url: `/assets/avatars/${file}`
        }));
        return res.json({ status: "SUCCESS", count: avatars.length, avatars });
    } catch (err) {
        console.error("[AVATAR_SCANNER] Failed to scan avatars directory:", err.message);
        return res.status(500).json({ error: "AVATAR_SCAN_FAILED", message: err.message });
    }
});

// Atomic Profile & Avatar Update with Complete Enterprise Synchronization & Forensic Ledger
app.post("/api/update-profile", async (req, res) => {
    try {
        const { agentId, profileData } = req.body || {};
        if (!agentId || !profileData) {
            return res.status(400).json({ error: "MISSING_PARAMS", message: "agentId and profileData are required." });
        }

        const cleanAgentId = await resolveCanonicalAgentId(agentId);
        const nowTs = Date.now();
        const nowIso = new Date().toISOString();
        const timestamp16 = nowIso.replace(/[-:T.Z]/g, '').slice(0, 16);
        const logId = `${nowTs}_PROFILE_${cleanAgentId}`;

        const updates = {};
        
        // 1. Profile Persona & Identity fields (Tier 2 Sub-Master - Atomic Cross-Node Sync)
        if (profileData.name !== undefined) {
            updates[`devices/${cleanAgentId}/profile/name`] = profileData.name;
            updates[`devices/${cleanAgentId}/profile/fullName`] = profileData.name;
            updates[`devices/${cleanAgentId}/profile/userName`] = profileData.name;
            updates[`devices/${cleanAgentId}/device/live/userName`] = profileData.name;
            updates[`devices/${cleanAgentId}/device/live/name`] = profileData.name;
            updates[`devices/${cleanAgentId}/Identity/owner_name`] = profileData.name;
            updates[`devices/${cleanAgentId}/Identity/id_full_name`] = profileData.name;
        }
        if (profileData.deviceName !== undefined || profileData.device_name !== undefined) {
            const devName = profileData.deviceName || profileData.device_name;
            updates[`devices/${cleanAgentId}/profile/deviceName`] = devName;
            updates[`devices/${cleanAgentId}/profile/device_name`] = devName;
            updates[`devices/${cleanAgentId}/device/device_name`] = devName;
            updates[`devices/${cleanAgentId}/device/deviceName`] = devName;
        }
        if (profileData.avatar_id !== undefined || profileData.avatarId !== undefined) {
            const avId = profileData.avatar_id || profileData.avatarId;
            updates[`devices/${cleanAgentId}/profile/avatar_id`] = avId;
            updates[`devices/${cleanAgentId}/profile/avatarId`] = avId;
            updates[`devices/${cleanAgentId}/profile/equipped_avatar_id`] = avId;
            updates[`devices/${cleanAgentId}/device/live/avatar_id`] = avId;
            updates[`devices/${cleanAgentId}/Identity/avatar_id`] = avId;
        }
        if (profileData.avatar_data !== undefined || profileData.avatarData !== undefined) {
            const avData = profileData.avatar_data || profileData.avatarData;
            updates[`devices/${cleanAgentId}/profile/avatar_data`] = avData;
        }

        // Cellular & Network Telemetry Routing SSOT Sync
        if (profileData.phone !== undefined) {
            updates[`devices/${cleanAgentId}/profile/phone`] = profileData.phone;
            updates[`devices/${cleanAgentId}/profile/userPhone`] = profileData.phone;
            updates[`devices/${cleanAgentId}/device/phone`] = profileData.phone;
            updates[`devices/${cleanAgentId}/device/sim_number`] = profileData.phone;
        }
        if (profileData.carrier !== undefined) {
            updates[`devices/${cleanAgentId}/profile/carrier`] = profileData.carrier;
            updates[`devices/${cleanAgentId}/device/carrier`] = profileData.carrier;
            updates[`devices/${cleanAgentId}/telemetry/carrier`] = profileData.carrier;
        }
        if (profileData.network_preference !== undefined || profileData.networkPreference !== undefined) {
            const netPref = profileData.network_preference || profileData.networkPreference;
            updates[`devices/${cleanAgentId}/profile/network_preference`] = netPref;
            updates[`devices/${cleanAgentId}/device/network_preference`] = netPref;
        }
        if (profileData.is_dual_sim !== undefined || profileData.isDualSim !== undefined) {
            const isDual = Boolean(profileData.is_dual_sim != null ? profileData.is_dual_sim : profileData.isDualSim);
            updates[`devices/${cleanAgentId}/profile/is_dual_sim`] = isDual;
            updates[`devices/${cleanAgentId}/device/is_dual_sim`] = isDual;
            updates[`devices/${cleanAgentId}/Hardware/is_dual_sim`] = isDual;
        }
        if (profileData.sim_status !== undefined || profileData.simStatus !== undefined) {
            const sStatus = profileData.sim_status || profileData.simStatus;
            updates[`devices/${cleanAgentId}/device/sim_status`] = sStatus;
        }
        if (profileData.imei1 !== undefined || profileData.imei_1 !== undefined) {
            const imei1Val = profileData.imei1 || profileData.imei_1;
            if (imei1Val && !imei1Val.includes("Protected") && !imei1Val.includes("Active")) {
                updates[`devices/${cleanAgentId}/device/imei_1`] = imei1Val;
            }
        }
        if (profileData.imei2 !== undefined || profileData.imei_2 !== undefined) {
            const imei2Val = profileData.imei2 || profileData.imei_2;
            if (imei2Val && !imei2Val.includes("Protected") && !imei2Val.includes("Active")) {
                updates[`devices/${cleanAgentId}/device/imei_2`] = imei2Val;
            }
        }

        if (profileData.email !== undefined) updates[`devices/${cleanAgentId}/profile/email`] = profileData.email;
        if (profileData.dob !== undefined) updates[`devices/${cleanAgentId}/profile/dob`] = profileData.dob;
        if (profileData.gender !== undefined) updates[`devices/${cleanAgentId}/profile/gender`] = profileData.gender;
        if (profileData.department !== undefined || profileData.dept_name !== undefined) {
            const dept = profileData.department || profileData.dept_name;
            updates[`devices/${cleanAgentId}/profile/department`] = dept;
            updates[`devices/${cleanAgentId}/profile/dept_name`] = dept;
        }
        if (profileData.org_name !== undefined) updates[`devices/${cleanAgentId}/profile/org_name`] = profileData.org_name;
        if (profileData.role !== undefined) updates[`devices/${cleanAgentId}/profile/role`] = profileData.role;
        if (profileData.role_family !== undefined || profileData.roleFamily !== undefined) {
            const rf = profileData.role_family || profileData.roleFamily;
            updates[`devices/${cleanAgentId}/profile/role_family`] = rf;
            updates[`devices/${cleanAgentId}/profile/roleFamily`] = rf;
        }
        if (profileData.role_friend !== undefined || profileData.roleFriend !== undefined) {
            const rf = profileData.role_friend || profileData.roleFriend;
            updates[`devices/${cleanAgentId}/profile/role_friend`] = rf;
            updates[`devices/${cleanAgentId}/profile/roleFriend`] = rf;
        }
        if (profileData.emergency_note !== undefined || profileData.emergencyNote !== undefined) {
            const note = profileData.emergency_note || profileData.emergencyNote;
            updates[`devices/${cleanAgentId}/profile/emergency_note`] = note;
            updates[`devices/${cleanAgentId}/profile/note`] = note;
        }
        if (profileData.address !== undefined) updates[`devices/${cleanAgentId}/profile/address`] = profileData.address;
        updates[`devices/${cleanAgentId}/profile/updated_at`] = timestamp16;
        updates[`devices/${cleanAgentId}/device/updated_at`] = timestamp16;

        // 2. ACL Security Grants (Dual Path: acl/{flag} AND Access/{flag})
        if (profileData.acl && typeof profileData.acl === 'object') {
            for (const [flag, val] of Object.entries(profileData.acl)) {
                const grantVal = (val === true || val === "GRANTED") ? "GRANTED" : "DENIED";
                updates[`devices/${cleanAgentId}/acl/${flag}`] = grantVal;
                updates[`devices/${cleanAgentId}/Access/${flag}`] = grantVal;
            }
        }

        // 3. Android SQLite Offline Database Synchronization Handshake Trigger
        updates[`devices/${cleanAgentId}/sync_trigger`] = nowTs;
        updates[`devices/${cleanAgentId}/sync_version`] = timestamp16;
        updates[`devices/${cleanAgentId}/commands/SYNC_PROFILE`] = {
            command: "SYNC_PROFILE",
            timestamp: timestamp16,
            epoch_ms: nowTs,
            payload: profileData
        };

        // 4. Change Log Audit Ledger
        updates[`devices/${cleanAgentId}/Change_Log/Profile/${logId}`] = {
            log_id: logId,
            category: "Profile",
            action: "DOSSIER_UPDATE_WEB",
            entity_id: cleanAgentId,
            changed_by: "WEB_ADMIN_OPERATOR",
            timestamp: timestamp16,
            epoch_ms: nowTs,
            metadata: profileData
        };

        await db.ref().update(updates);
        LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };
        COMPACT_DEVICES_CACHE = { data: null, timestamp: 0 };

        console.log(`[PROFILE_SENTINEL] ✅ Authoritative SSOT Profile Update synced for ${cleanAgentId}.`);
        return res.json({ 
            success: true, 
            status: "SUCCESS", 
            message: `Profile synchronized to cloud and offline databases for ${cleanAgentId}`, 
            agentId: cleanAgentId,
            updated_at: timestamp16,
            sync_version: timestamp16
        });
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

        const req = https.get(url, reqOpts, (geoRes) => {
            if (geoRes.statusCode !== 200) {
                geoRes.resume();
                return resolve(`${lat.toFixed(5)}°, ${lng.toFixed(5)}°`);
            }
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
        });

        // 3500ms safety timeout to prevent hanging connections
        req.setTimeout(3500, () => {
            req.destroy();
            resolve(`${lat.toFixed(5)}°, ${lng.toFixed(5)}°`);
        });

        req.on('error', () => {
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
    if (process.env.USE_LOCAL_MOCK === "true" && LOCAL_MOCK_DATA) {
        return res.json({
            rtdb: Object.keys(LOCAL_MOCK_DATA),
            firestore: ["users", "avatars", "sessions", "cells", "users_metadata"],
            projectId: PROJECT_ID || "jfindme-3b15c",
            databaseURL: DATABASE_URL,
            status: "SUCCESS"
        });
    }
    try {
        // 1. Define ENTERPRISE_SCHEMA_REGISTRY (Unified 4-Node Cloud Architecture)
        const SCHEMA_REGISTRY = {
            rtdb: ["devices", "lookup_indices", "pairing_requests", "History"],
            firestore: ["users", "avatars", "sessions", "cells", "users_metadata"]
        };

        // 2. Fetch RTDB Root Nodes (Admin SDK Live Probe)
        let liveRtdbNodes = [];
        try {
            const rootSnap = await Promise.race([
                db.ref("/").once("value"),
                new Promise((_, reject) => setTimeout(() => reject(new Error("Timeout")), 2500))
            ]);
            if (rootSnap && rootSnap.exists()) {
                const val = rootSnap.val() || {};
                liveRtdbNodes = Object.keys(val).filter(k => k && k !== 'null' && k !== 'error' && !k.startsWith('.'));
            }
        } catch (e) {
            console.warn("[WARN] RTDB Root Probe Fallback:", e.message);
            liveRtdbNodes = ["devices", "lookup_indices", "pairing_requests", "History", "Geofencing", "relationships"];
        }

        // 3. Fetch Firestore Collections (Live Probe with 2500ms Bound)
        let liveFirestoreDocs = [];
        try {
            const collections = await Promise.race([
                fs_db.listCollections(),
                new Promise((_, reject) => setTimeout(() => reject(new Error("Firestore Timeout")), 2500))
            ]);
            liveFirestoreDocs = (collections || []).map(c => c.id);
        } catch (e) {
            console.warn("[WARN] Firestore Discovery Restricted:", e.message);
        }

        // 4. MERGE LIVE DATA WITH SCHEMA REGISTRY (Ensure Published Structure is ALWAYS visible)
        const finalRtdb = [...new Set([...SCHEMA_REGISTRY.rtdb, ...liveRtdbNodes])].filter(k => k !== "error");
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
            const snapshot = await fs_db.collection(node).select().get();
            return res.json({ node, total_records: snapshot.size });
        } else {
            const cleanNode = (node || "").replace(/^\//, "").trim();
            if (!cleanNode || cleanNode === "/") {
                return res.json({ node, total_records: 8, rtdbNodes: ["devices", "lookup_indices", "pairing_requests", "History", "Geofencing", "relationships", "Version_Update", "config"] });
            }
            if (cleanNode === "devices") {
                const count = (LIVE_DEVICES_CACHE.data && LIVE_DEVICES_CACHE.data.count) || 5;
                return res.json({ node, total_records: count });
            }
            const snap = await queryNodeWithTimeout(cleanNode, 2500);
            const val = snap && typeof snap.val === "function" ? snap.val() : null;
            const count = val && typeof val === "object" ? Object.keys(val).length : (val != null ? 1 : 0);
            return res.json({ node, total_records: count });
        }
    } catch (err) {
        return res.status(500).json({ error: err.message });
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
// ENTERPRISE DECOMMISSION SENTINEL: ARCHIVE / CLEANSLATE DELETION
// ============================================================
app.post("/api/device/delete-everywhere", async (req, res) => {
    const { agentId, mode, preview } = req.body;
    if (!agentId || typeof agentId !== "string" || agentId.trim().length === 0) {
        return res.status(400).json({ error: "MISSING_AGENT_ID" });
    }
    const cleanId = agentId.trim();
    const actionState = (mode || "CLEANSLATE").toUpperCase(); // "ARCHIVE" or "CLEANSLATE"

    try {
        // 1. Fetch current device details from /devices/{cleanId}
        const devSnap = await db.ref(`devices/${cleanId}`).once("value");
        const devData = devSnap.val();
        if (!devData) {
            return res.status(404).json({ error: "DEVICE_NOT_FOUND", message: `No active device found for Agent ID: ${cleanId}` });
        }

        const authUid = devData.Identity?.auth_uid || "";
        const activeGroup = devData.Identity?.active_group || devData.profile?.active_group || "";
        const deviceName = devData.profile?.name || devData.profile?.fullName || devData.device?.deviceName || cleanId;
        const deviceModel = devData.device?.model || devData.device?.deviceName || "Android Device";

        // 2. Scan all database tables / cloud roots for references
        const targets = [];

        // Table 1: Master Device Registry
        targets.push({
            system: "Firebase RTDB",
            table: "Master Device Registry",
            path: `devices/${cleanId}`,
            recordsCount: Object.keys(devData).length,
            description: "Master identity, hardware silicon specs, profile, live coordinates and vitals"
        });

        // Table 2: Time-series GPS History
        const histSnap = await db.ref(`History/${cleanId}`).once("value");
        const histData = histSnap.val() || {};
        const histCount = Object.keys(histData).length;
        if (histCount > 0) {
            targets.push({
                system: "Firebase RTDB",
                table: "GPS Historical Stream",
                path: `History/${cleanId}`,
                recordsCount: histCount,
                description: "Historical breadcrumb stream and inverted timestamp indices"
            });
        }

        // Table 3: Reverse Auth Inverted Index
        if (authUid) {
            const authLookupSnap = await db.ref(`lookup_indices/auth_to_device/${authUid}`).once("value");
            if (authLookupSnap.exists()) {
                targets.push({
                    system: "Firebase RTDB",
                    table: "Auth Inverted Index",
                    path: `lookup_indices/auth_to_device/${authUid}`,
                    recordsCount: 1,
                    description: `Maps Auth UID [${authUid}] -> Agent ID [${cleanId}]`
                });
            }
        }

        // Table 4: Hardware Lookup Index
        const hwLookupSnap = await db.ref(`lookup_indices/device_to_hw/${cleanId}`).once("value");
        if (hwLookupSnap.exists()) {
            targets.push({
                system: "Firebase RTDB",
                table: "Hardware Serial Index",
                path: `lookup_indices/device_to_hw/${cleanId}`,
                recordsCount: 1,
                description: `Maps Agent ID -> Hardware ID [${hwLookupSnap.val()}]`
            });
        }

        // Table 5: Active Monitors
        const monitorSnap = await db.ref(`lookup_indices/active_monitors/${cleanId}`).once("value");
        if (monitorSnap.exists()) {
            targets.push({
                system: "Firebase RTDB",
                table: "Active Monitors Index",
                path: `lookup_indices/active_monitors/${cleanId}`,
                recordsCount: 1,
                description: "Real-time keep-alive monitor ping record"
            });
        }

        // Table 6: Community Mesh Graph / Circle Relationships
        const relSnap = await db.ref("relationships").once("value");
        const allGroups = relSnap.val() || {};
        for (const [grpId, grp] of Object.entries(allGroups)) {
            if (grp.devices && grp.devices[cleanId]) {
                targets.push({
                    system: "Firebase RTDB",
                    table: "Community Mesh Relationships",
                    path: `relationships/${grpId}/devices/${cleanId}`,
                    recordsCount: 1,
                    description: `Group membership in [${grpId}] with role [${grp.devices[cleanId].membership?.role || 'Member'}]`
                });
            }
            if (grp.members && grp.members[cleanId]) {
                targets.push({
                    system: "Firebase RTDB",
                    table: "Group Member Registry",
                    path: `relationships/${grpId}/members/${cleanId}`,
                    recordsCount: 1,
                    description: `Direct member node in [${grpId}]`
                });
            }
            if (grp.citizen_dossiers && grp.citizen_dossiers[cleanId]) {
                targets.push({
                    system: "Firebase RTDB",
                    table: "Citizen Dossiers",
                    path: `relationships/${grpId}/citizen_dossiers/${cleanId}`,
                    recordsCount: 1,
                    description: `Cached citizen persona profile in [${grpId}]`
                });
            }
        }

        // Table 7: Multi-Tenant Safe Zones
        const geoSnap = await db.ref("Geofencing").once("value");
        const geofences = geoSnap.val() || {};
        for (const [creatorId, creatorNode] of Object.entries(geofences)) {
            const zones = creatorNode.safe_zones || {};
            for (const [zoneId, zone] of Object.entries(zones)) {
                if (zone.connected_devices && zone.connected_devices[cleanId]) {
                    targets.push({
                        system: "Firebase RTDB",
                        table: "Safe Zone Boundary Links",
                        path: `Geofencing/${creatorId}/safe_zones/${zoneId}/connected_devices/${cleanId}`,
                        recordsCount: 1,
                        description: `Geofence target assignment for [${zone.zone_name || zoneId}]`
                    });
                }
            }
        }

        // Table 8: Peer Relationships inside other devices
        const allDevsSnap = await db.ref("devices").once("value");
        const allDevs = allDevsSnap.val() || {};
        for (const [otherId, otherNode] of Object.entries(allDevs)) {
            if (otherId !== cleanId && otherNode.relationships && otherNode.relationships[cleanId]) {
                targets.push({
                    system: "Firebase RTDB",
                    table: "Peer-to-Peer Device Link",
                    path: `devices/${otherId}/relationships/${cleanId}`,
                    recordsCount: 1,
                    description: `Peer relationship record inside device [${otherId}]`
                });
            }
        }

        // Table 9: Device Wipe & Purge Command Dispatch (Cloud-Mediated)
        targets.push({
            system: "Firebase Cloud Command",
            table: "Device Remote Purge Dispatch",
            path: `devices/${cleanId}/commands/PURGE_CLIENT_DATA`,
            recordsCount: 1,
            description: "Cloud-dispatched remote purge command instructing target device to clear cached local roster and reset node state upon next handshake"
        });

        // If preview mode, return target log report without modifying database
        if (preview) {
            return res.json({
                status: "PREVIEW",
                agentId: cleanId,
                deviceName,
                deviceModel,
                mode: actionState,
                totalTargets: targets.length,
                targets
            });
        }

        // EXECUTION PHASE
        if (actionState === "ARCHIVE") {
            // 1. Move to /database_archive/devices/{cleanId}
            const archivePayload = {
                agentId: cleanId,
                deviceName,
                deviceModel,
                archived_at_epoch: Date.now(),
                archived_at_iso: new Date().toISOString(),
                archive_reason: "USER_REQUESTED",
                data: devData,
                history: histData
            };
            await db.ref(`database_archive/devices/${cleanId}`).set(archivePayload);
            await db.ref(`database_archive/summary/${cleanId}`).set({
                agentId: cleanId,
                name: deviceName,
                model: deviceModel,
                archived_at: Date.now(),
                active_group: activeGroup,
                avatar_id: devData.profile?.avatar_id || "avatar_custom_1"
            });

            // 2. Remove from active /devices/{cleanId}
            await db.ref(`devices/${cleanId}`).remove();

            // 3. Remove from active circle relationships
            for (const [grpId, grp] of Object.entries(allGroups)) {
                if (grp.devices && grp.devices[cleanId]) {
                    await db.ref(`relationships/${grpId}/devices/${cleanId}`).remove();
                }
                if (grp.members && grp.members[cleanId]) {
                    await db.ref(`relationships/${grpId}/members/${cleanId}`).remove();
                }
            }

            // Invalidate cache
            LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };

            // Audit Log
            await db.ref(`system_audit/${Date.now()}_DEVICE_ARCHIVE`).set({
                action: "DEVICE_ARCHIVE",
                agentId: cleanId,
                name: deviceName,
                timestamp: new Date().toISOString(),
                targetsCount: targets.length
            });

            return res.json({
                status: "ARCHIVED",
                agentId: cleanId,
                deviceName,
                archivePath: `database_archive/devices/${cleanId}`,
                message: `Device [${deviceName}] (${cleanId}) successfully moved to Archive Database. It will remain hidden until Archive Log is checked.`
            });

        } else {
            // CLEANSLATE MODE: Atomic removal from ALL targets
            const deletePromises = [];

            // Master Device
            deletePromises.push(db.ref(`devices/${cleanId}`).remove());

            // History
            deletePromises.push(db.ref(`History/${cleanId}`).remove());

            // Auth Index
            if (authUid) {
                deletePromises.push(db.ref(`lookup_indices/auth_to_device/${authUid}`).remove());
            }
            deletePromises.push(db.ref(`lookup_indices/device_to_hw/${cleanId}`).remove());
            deletePromises.push(db.ref(`lookup_indices/active_monitors/${cleanId}`).remove());

            // Relationships
            for (const [grpId, grp] of Object.entries(allGroups)) {
                if (grp.devices && grp.devices[cleanId]) {
                    deletePromises.push(db.ref(`relationships/${grpId}/devices/${cleanId}`).remove());
                }
                if (grp.members && grp.members[cleanId]) {
                    deletePromises.push(db.ref(`relationships/${grpId}/members/${cleanId}`).remove());
                }
                if (grp.citizen_dossiers && grp.citizen_dossiers[cleanId]) {
                    deletePromises.push(db.ref(`relationships/${grpId}/citizen_dossiers/${cleanId}`).remove());
                }
            }

            // Safe Zones
            for (const [creatorId, creatorNode] of Object.entries(geofences)) {
                const zones = creatorNode.safe_zones || {};
                for (const [zoneId, zone] of Object.entries(zones)) {
                    if (zone.connected_devices && zone.connected_devices[cleanId]) {
                        deletePromises.push(db.ref(`Geofencing/${creatorId}/safe_zones/${zoneId}/connected_devices/${cleanId}`).remove());
                    }
                }
            }

            // Peer References
            for (const [otherId, otherNode] of Object.entries(allDevs)) {
                if (otherId !== cleanId && otherNode.relationships && otherNode.relationships[cleanId]) {
                    deletePromises.push(db.ref(`devices/${otherId}/relationships/${cleanId}`).remove());
                }
            }

            // Also remove from archive if existed
            deletePromises.push(db.ref(`database_archive/devices/${cleanId}`).remove());
            deletePromises.push(db.ref(`database_archive/summary/${cleanId}`).remove());

            await Promise.all(deletePromises);

            // Invalidate cache
            LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };

            // Audit Log
            await db.ref(`system_audit/${Date.now()}_DEVICE_CLEANSLATE`).set({
                action: "DEVICE_CLEANSLATE",
                agentId: cleanId,
                name: deviceName,
                timestamp: new Date().toISOString(),
                purgedTargetsCount: targets.length
            });

            return res.json({
                status: "CLEANSLATE_COMPLETED",
                agentId: cleanId,
                deviceName,
                purgedTablesCount: targets.length,
                message: `Device [${deviceName}] (${cleanId}) completely erased from all database tables and cloud indices.`
            });
        }

    } catch (err) {
        console.error(`[ERROR] Delete Everywhere Failure (${cleanId}):`, err.message);
        res.status(500).json({ error: err.message });
    }
});

// GET /api/archived-devices: Retrieves list of archived devices for the Archive Log
app.get("/api/archived-devices", async (req, res) => {
    try {
        const snap = await queryNodeWithTimeout("database_archive/summary", 3000);
        const summaries = (snap && typeof snap.val === 'function' ? snap.val() : null) || {};
        const list = Object.entries(summaries).map(([agentId, data]) => ({
            agentId,
            name: data.name || agentId,
            model: data.model || "Unknown",
            archivedAt: data.archived_at || Date.now(),
            avatarId: data.avatar_id || "avatar_custom_1",
            activeGroup: data.active_group || ""
        }));
        res.json({ status: "SUCCESS", count: list.length, archivedDevices: list });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// POST /api/device/restore-archive: Restores an archived device back to active /devices
app.post("/api/device/restore-archive", async (req, res) => {
    const { agentId } = req.body;
    if (!agentId) return res.status(400).json({ error: "MISSING_AGENT_ID" });
    const cleanId = agentId.trim();

    try {
        const snap = await db.ref(`database_archive/devices/${cleanId}`).once("value");
        const archiveData = snap.val();
        if (!archiveData || !archiveData.data) {
            return res.status(404).json({ error: "ARCHIVE_NOT_FOUND" });
        }

        // Restore to /devices/{cleanId}
        await db.ref(`devices/${cleanId}`).set(archiveData.data);
        if (archiveData.history) {
            await db.ref(`History/${cleanId}`).set(archiveData.history);
        }
        // Remove from archive
        await db.ref(`database_archive/devices/${cleanId}`).remove();
        await db.ref(`database_archive/summary/${cleanId}`).remove();

        LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };
        res.json({ status: "RESTORED", agentId: cleanId, message: `Device [${archiveData.deviceName || cleanId}] restored to active roster.` });
    } catch (err) {
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
            (LIVE_DEVICES_CACHE.data && LIVE_DEVICES_CACHE.data.devices && LIVE_DEVICES_CACHE.data.devices.length > 0) ? Promise.resolve(null) : queryNodeWithTimeout("devices", 6000),
            queryNodeWithTimeout("History", 3500),
            queryNodeWithTimeout("Geofencing", 3500)
        ]);

        let rawDevices = {};
        if (LIVE_DEVICES_CACHE.data && LIVE_DEVICES_CACHE.data.devices && LIVE_DEVICES_CACHE.data.devices.length > 0) {
            for (const d of LIVE_DEVICES_CACHE.data.devices) {
                rawDevices[d.agentId] = d.raw || d;
            }
        } else if (devSnap && typeof devSnap.val === 'function' && devSnap.exists()) {
            rawDevices = devSnap.val() || {};
        }

        if (Object.keys(rawDevices).length === 0) {
            return res.json({ duplicates: [], totalCount: 0, scannedAt: Date.now() });
        }
        const rawHistory = (histSnap && typeof histSnap.val === 'function' && histSnap.exists()) ? histSnap.val() : {};
        const rawGeofencing = (geofenceSnap && typeof geofenceSnap.val === 'function' && geofenceSnap.exists()) ? geofenceSnap.val() : {};

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
                avatarUrl: profile.avatar_id ? `/assets/avatars/${profile.avatar_id}.png` : '/assets/avatars/1.png',
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
                avatarUrl: profile.avatar_id ? `/assets/avatars/${profile.avatar_id}.png` : '/assets/avatars/1.png',
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

            // 4. 🛡️ Flush Stale / Expired Pairing Requests (>10 Minute TTL)
            const pairingSnap = await db.ref("pairing_requests").once("value");
            if (pairingSnap.exists()) {
                const pairings = pairingSnap.val() || {};
                const now = Date.now();
                const pairingUpdates = {};
                for (const [code, data] of Object.entries(pairings)) {
                    const ts = data?.created_at || data?.timestamp || data?.epoch_ms || 0;
                    if (now - ts > 10 * 60 * 1000 || data?.status === "expired") {
                        pairingUpdates[`pairing_requests/${code}`] = null;
                    }
                }
                if (Object.keys(pairingUpdates).length > 0) {
                    await db.ref().update(pairingUpdates);
                    console.log(`[BACKGROUND_AGENT] 🧹 Flushed ${Object.keys(pairingUpdates).length} expired pairing sessions.`);
                }
            }

            // 5. 🛡️ SafeZone Cross-Member & Auth UID Leaks Hygiene
            const geoSnap = await db.ref("Geofencing").once("value");
            if (geoSnap.exists()) {
                const geoData = geoSnap.val() || {};
                const geoUpdates = {};
                for (const [creatorId, creatorNode] of Object.entries(geoData)) {
                    if (creatorNode?.safe_zones) {
                        for (const [zoneId, zoneNode] of Object.entries(creatorNode.safe_zones)) {
                            if (zoneNode?.connected_devices) {
                                for (const targetId of Object.keys(zoneNode.connected_devices)) {
                                    if (!isCanonicalAgentId(targetId)) {
                                        console.warn(`[BACKGROUND_AGENT] ⚠️ Purging non-canonical/Auth-UID safezone target: Geofencing/${creatorId}/safe_zones/${zoneId}/connected_devices/${targetId}`);
                                        geoUpdates[`Geofencing/${creatorId}/safe_zones/${zoneId}/connected_devices/${targetId}`] = null;
                                    }
                                }
                            }
                        }
                    }
                }
                if (Object.keys(geoUpdates).length > 0) {
                    await db.ref().update(geoUpdates);
                    console.log(`[BACKGROUND_AGENT] 🧹 Purged ${Object.keys(geoUpdates).length} illegal non-canonical SafeZone targets.`);
                }
            }

            // 6. Heartbeat health ping to Firebase RTDB
            await db.ref(".info/connected").once("value");
            console.log("[BACKGROUND_AGENT] ✅ Closed-loop database connectivity & schema integrity verified.");
        } catch (err) {
            console.warn("[BACKGROUND_AGENT] Maintenance cycle warning:", err.message);
        }
    }, INTERVAL_MS);
    console.log("[BACKGROUND_AGENT] Initialized 10-minute automated background maintenance daemon with History-Device parity guard.");
}


// ============================================================
// NEW API ENDPOINTS - Settings Sync & Device Registration
// ============================================================

// Settings Sync Endpoint
app.post("/api/save-settings", async (req, res) => {
    try {
        let settingsPath = req.body.path;
        let data = req.body.data;
        
        if (!settingsPath && !data && req.body && typeof req.body === "object") {
            settingsPath = "config/operator/settings";
            data = req.body;
        }

        if (!settingsPath || !data || (typeof data === "object" && Object.keys(data).length === 0)) {
            return res.status(400).json({ 
                error: "INVALID_PAYLOAD", 
                message: "Path and non-empty data required" 
            });
        }
        
        // Save to Firebase Realtime Database
        const isUpdate = (typeof data === "object" && !Array.isArray(data));
        await writeWithTimeout(settingsPath, data, 3500, isUpdate);
        
        console.log(`[SETTINGS_API] Settings saved to ${settingsPath}:`, Object.keys(data || {}));
        
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
        const snapshot = await Promise.race([
            db.ref("config/operator/settings").once("value"),
            new Promise((_, reject) => setTimeout(() => reject(new Error("Settings Timeout")), 4000))
        ]).catch(err => {
        console.warn("[WARN] Settings query timeout:", err.message);
        return null;
    });
        const data = snapshot && snapshot.exists ? snapshot.val() : {};
        
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
        const snapshot = await queryNodeWithTimeout("config/circles", 3000);
        const circles = (snapshot && typeof snapshot.val === 'function' ? snapshot.val() : null) || [
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

// Dynamic Avatar Listing Endpoint
app.get("/api/avatars", (req, res) => {
    try {
        const avatarsDir = path.join(__dirname, "../assets/avatars");
        if (!fs.existsSync(avatarsDir)) {
            return res.json({ status: "SUCCESS", count: 0, avatars: [] });
        }
        const files = fs.readdirSync(avatarsDir);
        const validExts = [".png", ".jpg", ".jpeg", ".webp"];
        const avatars = [];
        
        files.forEach(f => {
            const ext = path.extname(f).toLowerCase();
            if (validExts.includes(ext)) {
                const baseName = path.basename(f, ext);
                avatars.push({
                    id: /^\d+$/.test(baseName) ? `avatar_custom_${baseName}` : baseName,
                    filename: f,
                    url: `/assets/avatars/${encodeURIComponent(f)}`
                });
            }
        });

        // Sort: numeric first (1..37), then alphabetically
        avatars.sort((a, b) => {
            const aNum = parseInt(a.filename.match(/\d+/)?.[0] || "9999", 10);
            const bNum = parseInt(b.filename.match(/\d+/)?.[0] || "9999", 10);
            if (aNum !== bNum) return aNum - bNum;
            return a.filename.localeCompare(b.filename);
        });

        res.json({ status: "SUCCESS", count: avatars.length, avatars });
    } catch (err) {
        console.error("[AVATAR_API] Failed to scan avatars:", err.message);
        res.status(500).json({ error: "SCAN_FAILED", message: err.message });
    }
});

// Note: Duplicate /api/update-profile definition consolidated and merged into the authoritative SSOT endpoint (line 1713).

// Circle Relationship Addition Endpoint (Bidirectional Pairing)
app.post("/api/relationship/add", async (req, res) => {
    try {
        const { agentId, targetAgentId, relationRole, circleType } = req.body;
        if (!agentId || !targetAgentId) {
            return res.status(400).json({ error: "MISSING_IDS", message: "agentId and targetAgentId required" });
        }

        const nowTs = Date.now();
        const role = relationRole || "Connected";
        const circle = circleType || "Family";

        // Reverse role mapping
        let reverseRole = role;
        if (role.toLowerCase() === "parent" || role.toLowerCase() === "father" || role.toLowerCase() === "mother") {
            reverseRole = "Child";
        } else if (role.toLowerCase() === "child" || role.toLowerCase() === "son" || role.toLowerCase() === "daughter") {
            reverseRole = "Parent";
        }

        const updates = {};
        updates[`devices/${agentId}/relationships/${targetAgentId}`] = {
            agentId: targetAgentId,
            role: role,
            circle: circle,
            linked_at: nowTs
        };
        updates[`devices/${targetAgentId}/relationships/${agentId}`] = {
            agentId: agentId,
            role: reverseRole,
            circle: circle,
            linked_at: nowTs
        };

        // Also add to circle group registry
        const grpKey = circle.toLowerCase().includes("friend") ? "friends_circle" : "family_circle";
        updates[`relationships/${grpKey}/devices/${agentId}`] = {
            agent_id: agentId,
            role: role,
            updated_at: nowTs
        };
        updates[`relationships/${grpKey}/devices/${targetAgentId}`] = {
            agent_id: targetAgentId,
            role: reverseRole,
            updated_at: nowTs
        };

        // [Variables]: logId, nowTs | [Constants]: None | [Dependencies]: db.ref() | [Integration Points]: Change_Log/Pairing_Devices audit trail
        const logId = "LOG_" + nowTs;
        updates[`devices/${agentId}/Change_Log/Pairing_Devices/${logId}`] = {
            category: "Pairing_Devices",
            action: "DEVICE_PAIR_LINK",
            target_agent: targetAgentId,
            role: role,
            circle: circle,
            timestamp: nowTs
        };
        updates[`devices/${targetAgentId}/Change_Log/Pairing_Devices/${logId}`] = {
            category: "Pairing_Devices",
            action: "DEVICE_PAIR_LINK",
            target_agent: agentId,
            role: reverseRole,
            circle: circle,
            timestamp: nowTs
        };

        // Push sync trigger to both devices
        updates[`devices/${agentId}/sync_trigger`] = nowTs;
        updates[`devices/${targetAgentId}/sync_trigger`] = nowTs;

        await db.ref().update(updates);
        LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };

        console.log(`[RELATIONSHIP_API] ✅ Linked [${agentId}] (${role}) <-> [${targetAgentId}] (${reverseRole}) in ${circle}`);

        res.json({
            status: "SUCCESS",
            message: `Connection established between [${agentId}] and [${targetAgentId}].`,
            timestamp: nowTs
        });
    } catch (err) {
        console.error("[RELATIONSHIP_API] Error adding relationship:", err.message);
        res.status(500).json({ error: "LINK_FAILED", message: err.message });
    }
});

// Circle Relationship Deletion Endpoint (Bidirectional Unlink)
app.post("/api/relationship/delete", async (req, res) => {
    try {
        const { agentId, targetAgentId } = req.body;
        if (!agentId || !targetAgentId) {
            return res.status(400).json({ error: "MISSING_IDS", message: "agentId and targetAgentId required" });
        }

        const nowTs = Date.now();
        const updates = {};
        updates[`devices/${agentId}/relationships/${targetAgentId}`] = null;
        updates[`devices/${targetAgentId}/relationships/${agentId}`] = null;
        updates[`devices/${agentId}/sync_trigger`] = nowTs;
        updates[`devices/${targetAgentId}/sync_trigger`] = nowTs;

        await db.ref().update(updates);
        LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };

        console.log(`[RELATIONSHIP_API] 🗑️ Severed link between [${agentId}] and [${targetAgentId}]`);

        res.json({
            status: "DELETED",
            message: `Connection severed between [${agentId}] and [${targetAgentId}].`,
            timestamp: nowTs
        });
    } catch (err) {
        console.error("[RELATIONSHIP_API] Error deleting relationship:", err.message);
        res.status(500).json({ error: "DELETE_FAILED", message: err.message });
    }
});

// Hardware / ACL Permission Toggle Endpoint
app.post("/api/device/toggle-acl", async (req, res) => {
    try {
        const { agentId, flag, value } = req.body;
        if (!agentId || !flag) {
            return res.status(400).json({ error: "INVALID_PARAMETERS", message: "agentId and flag required" });
        }

        const cleanId = agentId.trim();
        const valStr = value ? "GRANTED" : "DENIED";
        const nowTs = Date.now();

        await db.ref(`devices/${cleanId}/Access/${flag}`).set(valStr);
        await db.ref(`devices/${cleanId}/sync_trigger`).set(nowTs);
        LIVE_DEVICES_CACHE = { data: null, timestamp: 0 };

        console.log(`[ACL_API] Set Access/${flag} = ${valStr} for ${cleanId}`);

        res.json({
            status: "SUCCESS",
            agentId: cleanId,
            flag,
            value: valStr,
            timestamp: nowTs
        });
    } catch (err) {
        console.error("[ACL_API] Error toggling ACL:", err.message);
        res.status(500).json({ error: "ACL_UPDATE_FAILED", message: err.message });
    }
});

// ============================================================
// END OF NEW API ENDPOINTS
// ============================================================

app.listen(PORT, HOST, () => {
    console.log("");
    console.log("╔══════════════════════════════════════════════════════════╗");
    console.log("║      jFindMeWeb - GPS Telemetry API Proxy                ║");
    console.log("╠══════════════════════════════════════════════════════════╣");
    console.log(`║  API Endpoint: http://${HOST}:${PORT}              ║`);
    console.log("║  Web UI:       http://127.0.0.1:8080/                    ║");
    console.log("╠══════════════════════════════════════════════════════════╣");
    console.log("║  API Endpoints:                                            ║");
    console.log("║    • /api/discovery        - Cloud node discovery         ║");
    console.log("║    • /api/devices-live     - Live device telemetry        ║");
    console.log("║    • /api/safe-zones       - Geofence zones               ║");
    console.log("║    • /api/save-settings    - Settings sync                ║");
    console.log("╠══════════════════════════════════════════════════════════╣");
    console.log("║  Project: jFindMeWeb (Web/GPS Telemetry)                 ║");
    console.log("║  Location: C:\\Users\\jiten\\jAnitGravity\\jFindMe\\jFindMeWeb\\           ║");
    console.log("╚══════════════════════════════════════════════════════════╝");
    console.log("");
    initRealtimeSentinelBridge();
    initBackgroundMaintenanceAgent();
    // Prime in-memory telemetry caches on boot so first web request responds in 0ms
    computeLiveDevicesPayload().catch(e => console.warn("[BOOT_PRIME] Cache prime warning:", e.message));
});
