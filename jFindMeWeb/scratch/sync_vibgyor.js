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

async function syncPushpaVibgyor() {
    console.log("Applying canonical Vibgyor sync for Pushpa (96F0E18F)...");
    const vibgyorLocation = {
        latitude: 18.559267848120406,
        longitude: 73.88436086475849,
        address: "Inside Vibgyor Safe Perimeter",
        current_safe_zone_id: "Vibgyor_1788163547752",
        current_safe_zone_name: "Vibgyor",
        current_safe_zone_state: "IN",
        last_geofence_transition_readable: "Sep 01, 07:51 AM",
        last_geofence_transition_ts: 1788229294842,
        updated_at: new Date().toISOString(),
        userName: "PUSHPA",
        avatar_id: "avatar_custom_2"
    };

    await db.ref("devices/96F0E18F/device/live").update(vibgyorLocation);
    console.log("Successfully synced Pushpa's live location to Vibgyor in devices/96F0E18F/device/live!");

    // Also dispatch test push notification to all other devices
    const devSnap = await db.ref("devices").once("value");
    const devices = devSnap.val() || {};
    const tokens = [];
    for (const [id, dev] of Object.entries(devices)) {
        if (id === "96F0E18F") continue;
        const token = (dev.Identity && dev.Identity.fcm_token) || (dev.device && dev.device.fcm_token);
        if (token && typeof token === "string" && token.length > 20 && !tokens.includes(token)) {
            tokens.push(token);
        }
    }

    if (tokens.length > 0) {
        const title = "🛡️ Safe Zone Entry Alert";
        const body = "PUSHPA has entered the Vibgyor Safe Zone.";
        const message = {
            tokens: tokens,
            notification: { title, body },
            data: {
                type: "GEOFENCE_ENTER",
                title: title,
                body: body,
                zone_name: "Vibgyor",
                avatar_name: "PUSHPA",
                timestamp: String(Date.now())
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

        const res = await admin.messaging().sendEachForMulticast(message);
        console.log(`Dispatched push to ${res.successCount}/${tokens.length} devices.`);
    }

    process.exit(0);
}

syncPushpaVibgyor();
