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

async function inspectDeviceHistories() {
    const ids = ['497FA75E', '96F0E18F', 'BE5A234A', '2B75E1E9', '1EACAA0E', 'VhpBXedjJLREwATSMs2syhEJfGj1'];
    for (const id of ids) {
        const dSnap = await db.ref(`devices/${id}`).once("value");
        const dev = dSnap.val() || {};
        console.log(`\n=================== DEVICE ${id} ===================`);
        console.log(`device subnode:`, JSON.stringify(dev.device));
        console.log(`history subnode:`, JSON.stringify(dev.history));
        console.log(`last_sync:`, JSON.stringify(dev.last_sync));
    }
    process.exit(0);
}

inspectDeviceHistories();
