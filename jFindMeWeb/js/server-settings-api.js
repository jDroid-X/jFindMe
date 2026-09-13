/**
 * Settings API Endpoint Handler - Server-side Implementation
 * Add this to js/server.js to enable settings synchronization
 */

// ============================================================
// SETTINGS SYNC ENDPOINT (Add to server.js)
// ============================================================

app.post("/api/save-settings", async (req, res) => {
    try {
        const { path, data } = req.body;
        
        if (!path || !data) {
            return res.status(400).json({ error: "INVALID_PAYLOAD", message: "Path and data required" });
        }
        
        // Save to Firebase Realtime Database
        await db.ref(path).set(data);
        
        res.json({ 
            status: "SUCCESS", 
            message: "Settings saved to server",
            timestamp: Date.now()
        });
        
    } catch (err) {
        console.error("[SETTINGS_API] Error saving settings:", err.message);
        res.status(500).json({ 
            error: "SAVE_FAILED", 
            message: err.message 
        });
    }
});

app.get("/api/settings", async (req, res) => {
    try {
        const snapshot = await db.ref("config/operator/settings").once("value");
        const data = snapshot.val();
        
        res.json({
            status: "SUCCESS",
            settings: data || {}
        });
        
    } catch (err) {
        console.error("[SETTINGS_API] Error fetching settings:", err.message);
        res.status(500).json({ 
            error: "FETCH_FAILED", 
            message: err.message 
        });
    }
});

// ============================================================
// DEVICE REGISTRATION ENDPOINT (Add to server.js)
// ============================================================

app.post("/api/devices", async (req, res) => {
    try {
        const deviceData = req.body;
        
        if (!deviceData.agentId) {
            return res.status(400).json({ error: "MISSING_AGENT_ID" });
        }
        
        // Check if device already exists
        const existing = await db.ref(`devices/${deviceData.agentId}`).once("value");
        
        if (existing.exists()) {
            return res.status(409).json({ 
                error: "DEVICE_EXISTS", 
                message: "Device with this ID already exists" 
            });
        }
        
        // Add created timestamp
        deviceData.createdAt = Date.now();
        deviceData.updatedAt = Date.now();
        
        // Save to Firebase
        await db.ref(`devices/${deviceData.agentId}`).set(deviceData);
        
        res.json({
            status: "SUCCESS",
            agentId: deviceData.agentId,
            message: `Device "${deviceData.profile?.name}" registered successfully`
        });
        
    } catch (err) {
        console.error("[DEVICE_API] Error registering device:", err.message);
        res.status(500).json({ 
            error: "REGISTER_FAILED", 
            message: err.message 
        });
    }
});

// ============================================================
// CIRCLES ENDPOINT (Add to server.js)
// ============================================================

app.get("/api/circles", async (req, res) => {
    try {
        const snapshot = await db.ref("config/circles").once("value");
        const circles = snapshot.val() || [
            { id: "family", name: "Family Circle" },
            { id: "operations", name: "Operations" },
            { id: "field", name: "Field Service" }
        ];
        
        res.json({ circles });
        
    } catch (err) {
        console.error("[CIRCLES_API] Error fetching circles:", err.message);
        res.json({ circles: [] }); // Fallback
    }
});

module.exports = {
    registerSettingsEndpoints,
    registerDeviceEndpoints,
    registerCircleEndpoints
};
