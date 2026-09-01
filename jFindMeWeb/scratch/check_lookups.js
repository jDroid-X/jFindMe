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

async function checkLookups() {
    const snap = await db.ref("lookup_indices").once("value");
    console.log("lookup_indices:", JSON.stringify(snap.val(), null, 2));
    process.exit(0);
}

checkLookups();
