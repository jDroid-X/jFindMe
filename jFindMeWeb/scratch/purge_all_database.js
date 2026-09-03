const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

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

async function purgeCompleteDatabase() {
    console.log("=================================================================");
    console.log("🚨 INITIATING COMPLETE ENTERPRISE DATABASE PURGE (STRATEGY A) 🚨");
    console.log("Target Database:", G_SERVICES.project_info.firebase_url);
    console.log("=================================================================");

    const nodesToPurge = [
        'devices',
        'History',
        'Geofencing',
        'relationships',
        'lookup_indices',
        'locations',
        'users',
        'groups',
        'archived_roster',
        'recycle_bin',
        'pairing_requests',
        'Version_Update',
        'system_logs',
        'telemetry_live'
    ];

    for (const node of nodesToPurge) {
        console.log(`🧹 Purging /${node}...`);
        await db.ref(node).remove();
        console.log(`✅ Cleared /${node}`);
    }

    // Set root to empty clean state
    console.log("🧹 Setting root to empty clean object...");
    await db.ref('/').set(null);

    console.log("=================================================================");
    console.log("✨ FIREBASE REALTIME DATABASE IS NOW 100% PRISTINE & BLANK ✨");
    console.log("=================================================================");

    // Verify
    const snap = await db.ref('/').once('value');
    console.log("Final Root Verification:", snap.val() === null ? "100% NULL (BLANK)" : JSON.stringify(snap.val()));

    process.exit(0);
}

purgeCompleteDatabase().catch(err => {
    console.error("FATAL ERROR DURING PURGE:", err);
    process.exit(1);
});
