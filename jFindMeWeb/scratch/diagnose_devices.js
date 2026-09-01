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

async function runAudit() {
    console.log("=== 1. DEVICES TREE INSPECTION ===");
    const devSnap = await db.ref("devices").once("value");
    const devices = devSnap.val() || {};
    for (const [id, dev] of Object.entries(devices)) {
        const name = (dev.profile && (dev.profile.name || dev.profile.fullName)) || (dev.Identity && dev.Identity.owner_name) || id;
        console.log(`\nDevice Agent ID: [${id}] -> Name: [${name}]`);
        console.log(`  - dev.device.live:`, JSON.stringify(dev.device?.live || null));
        console.log(`  - dev.live:`, JSON.stringify(dev.live || null));
        console.log(`  - dev.history.latest:`, JSON.stringify(dev.history?.latest || null));
        console.log(`  - dev.telemetry:`, JSON.stringify(dev.telemetry || null));
        console.log(`  - dev.Identity:`, JSON.stringify(dev.Identity || null));
    }

    console.log("\n=== 2. ROOT HISTORY INSPECTION ===");
    const histSnap = await db.ref("History").once("value");
    const history = histSnap.val() || {};
    for (const [id, h] of Object.entries(history)) {
        console.log(`\nHistory Key: [${id}]`);
        if (h.latest) console.log(`  - latest:`, JSON.stringify(h.latest));
        if (h.history) {
            const entries = Object.entries(h.history);
            console.log(`  - history count: ${entries.length}`);
            if (entries.length > 0) {
                entries.sort((a,b) => (b[1].timestamp || b[0]) - (a[1].timestamp || a[0]));
                console.log(`  - most recent history item:`, JSON.stringify(entries[0][1]));
            }
        }
    }

    console.log("\n=== 3. ROOT USERLOCATIONS / LOCATIONS INSPECTION ===");
    const userLocsSnap = await db.ref("UserLocations").once("value").catch(() => null);
    console.log("UserLocations:", userLocsSnap ? Object.keys(userLocsSnap.val() || {}) : "None");
    
    const locsSnap = await db.ref("locations").once("value").catch(() => null);
    console.log("locations:", locsSnap ? Object.keys(locsSnap.val() || {}) : "None");

    process.exit(0);
}

runAudit().catch(e => {
    console.error(e);
    process.exit(1);
});
