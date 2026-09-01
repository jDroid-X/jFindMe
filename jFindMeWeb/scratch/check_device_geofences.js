const admin = require("firebase-admin");
const path = require("path");
const fs = require("fs");

let SA_PATH = path.join(__dirname, "../service-account.json");
if (!fs.existsSync(SA_PATH)) {
    const files = fs.readdirSync(path.join(__dirname, "../"));
    const saFile = files.find(f => f.includes("firebase-adminsdk") && f.endsWith(".json"));
    if (saFile) SA_PATH = path.join(__dirname, "../", saFile);
}
const GS_PATH = path.join(__dirname, "../google-services.json");
const G_SERVICE_ACCOUNT = JSON.parse(fs.readFileSync(SA_PATH, "utf8"));
const G_SERVICES = JSON.parse(fs.readFileSync(GS_PATH, "utf8"));

if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(G_SERVICE_ACCOUNT),
        databaseURL: G_SERVICES.project_info.firebase_url
    });
}

const db = admin.database();

function parseTimeRange(start, end) {
    const formatH = (n) => {
        const s = String(n).padStart(4, '0');
        return `${s.slice(0, 2)}:${s.slice(2, 4)}`;
    };
    return `${formatH(start || 0)} - ${formatH(end || 2359)}`;
}

function parseDays(activeDays) {
    if (!activeDays) return "All Days";
    const dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
    return String(activeDays).split(',').map(d => dayNames[parseInt(d.trim())] || d).join(', ');
}

class DeviceGeofenceAuditor {
    constructor() {
        this.db = db;
    }

    async auditDevice(agentId) {
        const snap = await this.db.ref(`devices/${agentId}`).once("value");
        const dev = snap.val() || {};
        const name = dev.profile?.name || dev.Identity?.owner_name || dev.device?.live?.userName || agentId;

        console.log(`\n${"=".repeat(65)}`);
        console.log(`   GEOFENCE MATRIX AUDIT: [${agentId}] — ${name}`);
        console.log(`${"=".repeat(65)}`);

        // 1. Current Live Safe Zone State
        const liveState = dev.device?.live?.current_safe_zone_state || "OUT / UNKNOWN";
        const liveZoneName = dev.device?.live?.current_safe_zone_name || "None";
        const liveZoneId = dev.device?.live?.current_safe_zone_id || "None";
        const transitionTime = dev.device?.live?.last_geofence_transition_readable || "N/A";
        console.log(`📡 Live Perimeter Status: [${liveState}] in "${liveZoneName}" (${liveZoneId})`);
        console.log(`   Last Transition Recorded: ${transitionTime}`);

        // 2. Assigned Safe Zones Sub-Tree
        const safeZones = dev.safe_zones || {};
        const zoneKeys = Object.keys(safeZones);
        console.log(`\n🛡️ Assigned Safe Zones (${zoneKeys.length}):`);
        if (zoneKeys.length === 0) {
            console.log("   (No safe zones directly assigned under devices/" + agentId + "/safe_zones)");
        } else {
            for (const [zId, zone] of Object.entries(safeZones)) {
                const zName = zone.zone_name || zone.name || zId;
                const active = zone.is_active !== false ? "🟢 ACTIVE" : "🔴 DISABLED";
                const alerts = zone.alerts_enabled ? "🔔 ON" : "🔕 MUTED";
                const schedule = parseTimeRange(zone.startTime, zone.endTime);
                const days = parseDays(zone.activeDays);
                const center = `${zone.lat || 0}°, ${zone.lng || 0}°`;
                const rad = `${zone.radius || 100}m`;
                const currState = zone.current_state || "UNKNOWN";

                console.log(`  • [${zName}] (${zId}) — ${active} | Alerts: ${alerts}`);
                console.log(`    - Location: ${center} (Radius: ${rad})`);
                console.log(`    - Schedule: ${schedule} on [${days}]`);
                console.log(`    - Current Device State: [${currState}]`);
                if (zone.address) console.log(`    - Address: ${zone.address}`);
            }
        }

        // 3. Pending Sync Requests
        const syncReqs = dev.Identity?.geofence_sync_requests;
        if (syncReqs) {
            console.log(`\n📬 Geofence Sync Handshakes:`);
            for (const [reqId, req] of Object.entries(syncReqs)) {
                console.log(`  • Request [${reqId}]: Zone "${req.zone_name}" -> Status: [${req.status}]`);
            }
        }
    }

    async auditGlobalGeofencingTree() {
        console.log(`\n${"=".repeat(65)}`);
        console.log(`   GLOBAL GEOFENCING HIERARCHY (/Geofencing)`);
        console.log(`${"=".repeat(65)}`);

        const gSnap = await this.db.ref("Geofencing").once("value");
        const gData = gSnap.val() || {};

        for (const [creatorId, cData] of Object.entries(gData)) {
            console.log(`\n👑 Master Creator: [${creatorId}]`);
            if (!cData.safe_zones) {
                console.log("   (No safe zones defined under this creator)");
                continue;
            }

            for (const [zId, z] of Object.entries(cData.safe_zones)) {
                const connDevs = z.connected_devices ? Object.keys(z.connected_devices) : [];
                console.log(`  📍 Zone: [${z.zone_name || zId}] (${zId})`);
                console.log(`     - Coordinates: ${z.lat}°, ${z.lng}° (Radius: ${z.radius || 100}m)`);
                console.log(`     - Target Connected Devices (${connDevs.length}): [ ${connDevs.join(", ")} ]`);
            }
        }
    }

    async run(targetId) {
        if (targetId) {
            await this.auditDevice(targetId);
        } else {
            const snap = await this.db.ref("devices").once("value");
            const devices = snap.val() || {};
            for (const id of Object.keys(devices)) {
                if (id.length <= 16) {
                    await this.auditDevice(id);
                }
            }
            await this.auditGlobalGeofencingTree();
        }
        process.exit(0);
    }
}

const auditor = new DeviceGeofenceAuditor();
auditor.run(process.argv[2]);
