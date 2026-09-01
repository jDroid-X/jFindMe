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
const fs_db = admin.firestore();

async function checkFcmStatus() {
    console.log("=== CHECKING FCM_QUEUE IN FIRESTORE ===");
    try {
        const queueSnap = await fs_db.collection("fcm_queue").get();
        console.log(`Found ${queueSnap.size} pending docs in fcm_queue:`);
        queueSnap.forEach(doc => {
            console.log(`Doc ID: [${doc.id}] ->`, JSON.stringify(doc.data()));
        });
    } catch (e) {
        console.log("Error querying fcm_queue:", e.message);
    }

    console.log("\n=== CHECKING FCM TOKENS ACROSS DEVICES ===");
    const devSnap = await db.ref("devices").once("value");
    const devices = devSnap.val() || {};
    for (const [id, dev] of Object.entries(devices)) {
        const name = (dev.profile && dev.profile.name) || (dev.Identity && dev.Identity.owner_name) || id;
        const fcmToken = (dev.Identity && dev.Identity.fcm_token) || (dev.device && dev.device.fcm_token) || null;
        console.log(`Device [${id}] (${name}): Token = ${fcmToken ? `${fcmToken.slice(0, 20)}...` : 'NONE'}`);
    }
    process.exit(0);
}

checkFcmStatus();
