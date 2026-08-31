const express = require("express");
const cors = require("cors");
const admin = require("firebase-admin");
const fs = require("fs");
const path = require("path");

console.log("[SYSTEM] Initializing jFindMe Forensic Proxy...");

// --- PATH VALIDATION (DYNAMIC RESOLVER) ---
let SA_PATH = path.join(__dirname, "../service-account.json");
const GS_PATH = path.join(__dirname, "../google-services.json");

// Auto-Detect specific service account if generic is missing
if (!fs.existsSync(SA_PATH)) {
    const root = path.join(__dirname, "../");
    const files = fs.readdirSync(root);
    const saFile = files.find(f => f.includes("firebase-adminsdk") && f.endsWith(".json"));
    if (saFile) {
        SA_PATH = path.join(root, saFile);
        console.log(`[INFO] Identity Resolver: Using forensic credential [${saFile}]`);
    }
}

if (!fs.existsSync(SA_PATH)) {
    console.error(`[FATAL] Missing service-account.json at ${SA_PATH}`);
    process.exit(1);
}
if (!fs.existsSync(GS_PATH)) {
    console.error(`[FATAL] Missing google-services.json at ${GS_PATH}`);
    process.exit(1);
}

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

app.use(cors());
app.use(express.json());

// Log every request
app.use((req, res, next) => {
    console.log(`[NETWORK] ${new Date().toLocaleTimeString()} ${req.method} ${req.url}`);
    next();
});

app.get("/api/ping", (req, res) => res.json({ status: "ALIVE", project: PROJECT_ID }));

app.get("/api/discovery", async (req, res) => {
    console.log("[DEBUG] Multi-Cloud Discovery Probe Initiated...");
    try {
        // 1. Define ENTERPRISE_SCHEMA_REGISTRY (Unified 4-Node Cloud Architecture)
        const SCHEMA_REGISTRY = {
            rtdb: ["devices", "lookup_indices", "pairing_requests", "History"],
            firestore: ["users", "avatars", "sessions", "cells", "users_metadata"]
        };

        // 2. Fetch RTDB Root Nodes (Shallow Live Probe)
        const https = require("https");
        const baseUrl = DATABASE_URL.endsWith("/") ? DATABASE_URL.slice(0, -1) : DATABASE_URL;
        const rtdbUrl = `${baseUrl}/.json?shallow=true`;
        
        let liveRtdbNodes = [];
        await new Promise((resolve) => {
            https.get(rtdbUrl, (resp) => {
                let data = '';
                resp.on('data', (chunk) => { data += chunk; });
                resp.on('end', () => {
                    try {
                        const parsed = JSON.parse(data) || {};
                        liveRtdbNodes = Object.keys(parsed).filter(k => k !== 'null');
                        resolve();
                    } catch (e) { resolve(); }
                });
            }).on('error', () => resolve());
        });

        // 3. Fetch Firestore Collections (Live Probe)
        let liveFirestoreDocs = [];
        try {
            const collections = await fs_db.listCollections();
            liveFirestoreDocs = collections.map(c => c.id);
        } catch (e) {
            console.warn("[WARN] Firestore Discovery Restricted:", e.message);
        }

        // 4. MERGE LIVE DATA WITH SCHEMA REGISTRY (Ensure Published Structure is ALWAYS visible)
        const finalRtdb = [...new Set([...SCHEMA_REGISTRY.rtdb, ...liveRtdbNodes])];
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
            // Note: firestore.collection().count() requires Firebase Admin v11.1.0+ and pays for indexing
            // For simplicity and compatibility, we'll try a basic approach or return estimated
            const snapshot = await fs_db.collection(node).select().get();
            res.json({ node, total_records: snapshot.size });
        } else {
            const https = require("https");
            const baseUrl = DATABASE_URL.endsWith("/") ? DATABASE_URL.slice(0, -1) : DATABASE_URL;
            const nodePath = node.startsWith("/") ? node : `/${node}`;
            const url = `${baseUrl}${nodePath}.json?shallow=true`;
            
            https.get(url, (resp) => {
                let data = '';
                resp.on('data', (chunk) => { data += chunk; });
                resp.on('end', () => {
                    try {
                        const parsed = JSON.parse(data) || {};
                        const count = Object.keys(parsed).length;
                        res.json({ node, total_records: count });
                    } catch (parseErr) {
                        res.status(500).json({ error: "STATS_PARSE_FAILURE" });
                    }
                });
            }).on('error', (httpErr) => {
                res.status(500).json({ error: httpErr.message });
            });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
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
            // Assume path is 'collection/docId'
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

const PORT = process.env.PORT || 3000;
const HOST = process.env.HOST || "127.0.0.1";
app.listen(PORT, HOST, () => {
    console.log(`==========================================`);
    console.log(`   jFindMe FORENSIC PROXY LIVE            `);
    console.log(`   Endpoint: http://${HOST}:${PORT}       `);
    console.log(`==========================================`);
});
