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

async function checkAllRoots() {
    const snap = await db.ref("/").once("value");
    const val = snap.val() || {};
    console.log("Root Keys in Firebase RTDB:", Object.keys(val));
    
    for (const key of Object.keys(val)) {
        if (typeof val[key] === 'object' && val[key] !== null) {
            console.log(`Key [${key}] subkeys:`, Object.keys(val[key]).slice(0, 10));
        }
    }
    
    // Also check if any device has custom location node
    if (val.devices) {
        for (const [dId, dev] of Object.entries(val.devices)) {
            console.log(`\nDevice ${dId} keys:`, Object.keys(dev));
        }
    }
    process.exit(0);
}

checkAllRoots();
