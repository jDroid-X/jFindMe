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

function parseCustomTimestamp(ts) {
    if (!ts) return "N/A";
    if (typeof ts === 'number') {
        return new Date(ts).toLocaleString('en-IN', { timeZone: 'Asia/Kolkata' });
    }
    const str = String(ts);
    if (/^\d{16}$/.test(str)) {
        // Format: YYYYMMDDHHMMSSCC e.g. 2026090107543793
        const y = str.slice(0, 4);
        const m = str.slice(4, 6);
        const d = str.slice(6, 8);
        const h = str.slice(8, 10);
        const min = str.slice(10, 12);
        const s = str.slice(12, 14);
        return `${d}/${m}/${y} at ${h}:${min}:${s} (IST)`;
    }
    const parsed = new Date(str);
    if (!isNaN(parsed.getTime())) {
        return parsed.toLocaleString('en-IN', { timeZone: 'Asia/Kolkata' });
    }
    return str;
}

async function auditDevice(agentId) {
    const dSnap = await db.ref(`devices/${agentId}`).once("value");
    const dev = dSnap.val() || {};

    const histSnap = await db.ref(`History/${agentId}`).once("value");
    const hist = histSnap.val() || {};

    const monitorSnap = await db.ref(`lookup_indices/active_monitors/${agentId}`).once("value");
    const monitorTs = monitorSnap.val();

    const name = dev.profile?.name || dev.Identity?.owner_name || dev.device?.live?.userName || agentId;

    console.log(`\n${"=".repeat(60)}`);
    console.log(`   DEVICE AUDIT: [${agentId}] — ${name}   `);
    console.log(`${"=".repeat(60)}`);

    console.log("1. TELEMETRY & HEARTBEAT:");
    console.log("  - Telemetry last_seen:", dev.telemetry?.last_seen, "->", parseCustomTimestamp(dev.telemetry?.last_seen));
    console.log("  - Telemetry last_heartbeat:", dev.telemetry?.last_heartbeat, "->", parseCustomTimestamp(dev.telemetry?.last_heartbeat));
    console.log("  - Telemetry updated_at:", dev.telemetry?.updated_at, "->", parseCustomTimestamp(dev.telemetry?.updated_at));
    console.log("  - Telemetry status:", dev.telemetry?.status, `(Battery: ${dev.telemetry?.battery_pct ?? -1}% | Charging: ${dev.telemetry?.is_charging ?? false})`);
    console.log("  - Active Monitor Heartbeat:", monitorTs, "->", parseCustomTimestamp(monitorTs));

    console.log("\n2. LIVE LOCATION & GPS:");
    console.log("  - Live Location updated_at:", dev.device?.live?.updated_at, "->", parseCustomTimestamp(dev.device?.live?.updated_at));
    console.log("  - Live Coordinates:", `Lat: ${dev.device?.live?.latitude}, Lng: ${dev.device?.live?.longitude}`);
    console.log("  - Live Address:", dev.device?.live?.address || dev.device?.Address || "N/A");
    if (dev.device?.live?.current_safe_zone_name) {
        console.log("  - Safe Zone State:", `[${dev.device?.live?.current_safe_zone_state}] inside ${dev.device?.live?.current_safe_zone_name} (${parseCustomTimestamp(dev.device?.live?.last_geofence_transition_ts)})`);
    }

    console.log("\n3. ROOT TIME-SERIES ARCHIVE (History/" + agentId + "):");
    if (hist.history) {
        const entries = Object.entries(hist.history);
        console.log("  - Total Historical Breadcrumbs Logged:", entries.length);
        entries.sort((a,b) => (b[1].timestamp || b[0]) - (a[1].timestamp || a[0]));
        const latest = entries[0][1];
        console.log("  - Most Recent Archive Record:", latest.timestamp, "->", parseCustomTimestamp(latest.timestamp));
        console.log("  - Coordinates:", `${latest.latitude}, ${latest.longitude}`);
        console.log("  - Address:", latest.address);
    } else {
        console.log("  - No archive records.");
    }
}

async function run() {
    const target = process.argv[2];
    if (target) {
        await auditDevice(target);
    } else {
        const snap = await db.ref("devices").once("value");
        const devices = snap.val() || {};
        for (const id of Object.keys(devices)) {
            if (id.length <= 16) {
                await auditDevice(id);
            }
        }
    }
    process.exit(0);
}

run();
