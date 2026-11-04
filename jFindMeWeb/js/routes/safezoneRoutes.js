/**
 * 🛡️ SafeZone Express Router (OOPS MVC Sub-Module)
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: req, res, updates, deleteOps, geofenceData, zonesList, seenZoneIds
 * Constants: CACHE_TTL_MS, R (Haversine Earth radius)
 * Dependencies: express, db, queryNodeWithTimeout, writeWithTimeout, resolveCanonicalAgentId, SAFE_ZONES_CACHE, LIVE_DEVICES_CACHE
 * Integration Points: Exported factory function mounted into Express pipeline at /api
 */

const express = require('express');

function createSafezoneRouter({
    db,
    queryNodeWithTimeout,
    writeWithTimeout,
    resolveCanonicalAgentId,
    getSafeZonesCache,
    setSafeZonesCache,
    getLiveDevicesCache,
    setLiveDevicesCache,
    CACHE_TTL_MS
}) {
    const router = express.Router();

    // GET /api/safe-zones: Authoritative Safe Zones Aggregator
    router.get('/safe-zones', async (req, res) => {
        const now = Date.now();
        const safeZonesCache = getSafeZonesCache ? getSafeZonesCache() : null;
        if (safeZonesCache && safeZonesCache.data && (now - safeZonesCache.timestamp) < CACHE_TTL_MS) {
            return res.json(safeZonesCache.data);
        }

        try {
            const liveDevicesCache = getLiveDevicesCache ? getLiveDevicesCache() : null;
            const [geofenceSnap, devicesSnap, relSnap, lookupSnap] = await Promise.all([
                queryNodeWithTimeout("Geofencing", 3500),
                queryNodeWithTimeout("devices", 3500),
                queryNodeWithTimeout("relationships", 3500),
                queryNodeWithTimeout("lookup_indices/auth_to_device", 3500)
            ]);
            const geofenceData = (geofenceSnap && typeof geofenceSnap.val === 'function' && geofenceSnap.val()) || {};
            const devicesData = (devicesSnap && typeof devicesSnap.val === 'function' && devicesSnap.val()) || 
                (liveDevicesCache && liveDevicesCache.data && Array.isArray(liveDevicesCache.data.devices) 
                    ? liveDevicesCache.data.devices.reduce((acc, d) => ({...acc, [d.agentId]: d}), {}) 
                    : null) || {};
            const relData = (relSnap && typeof relSnap.val === 'function' && relSnap.val()) || {};
            const authToDevice = (lookupSnap && typeof lookupSnap.val === 'function' && lookupSnap.val()) || {};

            const zonesList = [];
            const seenZoneIds = new Set();

            const resolveCanonicalDevId = (rawKey) => {
                if (!rawKey) return rawKey;
                return authToDevice[rawKey] || rawKey;
            };

            const parseZone = (key, zoneObj, parentCreator) => {
                if (!zoneObj || typeof zoneObj !== 'object') return;
                const rawZoneId = zoneObj.zone_id || zoneObj.id || key;
                if (!rawZoneId) return;

                const lat = parseFloat(zoneObj.lat || zoneObj.latitude || 0);
                const lng = parseFloat(zoneObj.lng || zoneObj.longitude || 0);
                if (isNaN(lat) || isNaN(lng) || (lat === 0 && lng === 0)) return;

                const uniqueKey = `${parentCreator || 'ROOT'}_${rawZoneId}`;
                if (seenZoneIds.has(uniqueKey)) return;
                seenZoneIds.add(uniqueKey);

                let connectedArr = [];
                if (Array.isArray(zoneObj.connected_devices)) {
                    connectedArr = zoneObj.connected_devices;
                } else if (zoneObj.connected_devices && typeof zoneObj.connected_devices === 'object') {
                    connectedArr = Object.keys(zoneObj.connected_devices);
                }

                const resolvedCreator = resolveCanonicalDevId(zoneObj.created_by || parentCreator || "SYSTEM");
                const creatorDevice = devicesData[resolvedCreator] || {};
                const creatorName = creatorDevice.name || creatorDevice.personalInfo?.name || resolvedCreator;

                zonesList.push({
                    zoneId: rawZoneId,
                    zone_id: rawZoneId,
                    zoneName: zoneObj.zone_name || zoneObj.name || `Zone ${rawZoneId}`,
                    name: zoneObj.zone_name || zoneObj.name || `Zone ${rawZoneId}`,
                    latitude: lat,
                    longitude: lng,
                    lat: lat,
                    lng: lng,
                    radius: parseFloat(zoneObj.radius || 100),
                    isActive: zoneObj.is_active !== undefined ? zoneObj.is_active : (zoneObj.isActive !== undefined ? zoneObj.isActive : true),
                    alertsEnabled: zoneObj.alerts_enabled !== undefined ? zoneObj.alerts_enabled : (zoneObj.alertsEnabled !== undefined ? zoneObj.alertsEnabled : true),
                    polygonPoints: zoneObj.polygon_points || zoneObj.polygonPoints || "",
                    dwellMinutes: parseInt(zoneObj.dwell_minutes || zoneObj.dwellMinutes || 0, 10),
                    creatorId: resolvedCreator,
                    creatorName: creatorName,
                    connectedDevices: connectedArr,
                    assignedTargets: zoneObj.assigned_targets || "Connected Devices",
                    createdOn: zoneObj.created_on || zoneObj.created_at || new Date().toISOString(),
                    updatedAt: zoneObj.updated_at || Date.now()
                });
            };

            // Parse Root and Per-Device Geofences
            for (const [k, v] of Object.entries(geofenceData)) {
                if (v && v.safe_zones && typeof v.safe_zones === 'object') {
                    for (const [subK, subV] of Object.entries(v.safe_zones)) {
                        parseZone(subK, subV, k);
                    }
                } else if (v && (v.lat || v.latitude)) {
                    parseZone(k, v, "ROOT");
                }
            }

            // Parse embedded safe_zones inside devices
            for (const [devKey, devVal] of Object.entries(devicesData)) {
                if (devVal && devVal.safe_zones && typeof devVal.safe_zones === 'object') {
                    for (const [subK, subV] of Object.entries(devVal.safe_zones)) {
                        parseZone(subK, subV, devKey);
                    }
                }
            }

            const responsePayload = {
                status: "SUCCESS",
                total: zonesList.length,
                zones: zonesList,
                cachedAt: Date.now()
            };

            if (setSafeZonesCache) {
                setSafeZonesCache({ data: responsePayload, timestamp: Date.now() });
            }

            res.json(responsePayload);
        } catch (err) {
            console.error("[SAFE_ZONES] Error aggregating safe zones:", err);
            res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
        }
    });

    // POST /api/save-safe-zone: Safe Zone Creation Endpoint
    router.post('/save-safe-zone', async (req, res) => {
        try {
            const zoneId = req.body.zoneId || req.body.zone_id;
            const zoneName = req.body.zoneName || req.body.name || req.body.zone_name;
            const rawLat = req.body.latitude !== undefined ? req.body.latitude : req.body.lat;
            const rawLng = req.body.longitude !== undefined ? req.body.longitude : req.body.lng;
            const latitude = parseFloat(rawLat);
            const longitude = parseFloat(rawLng);
            const radius = req.body.radius !== undefined ? req.body.radius : (req.body.radius_meters || 100);
            const isActive = req.body.isActive !== undefined ? req.body.isActive : (req.body.is_active !== false);
            const alertsEnabled = req.body.alertsEnabled !== undefined ? req.body.alertsEnabled : Boolean(req.body.alerts_enabled);
            const connectedDevices = req.body.connectedDevices || req.body.connected_devices || [];
            const polygonPoints = req.body.polygonPoints || req.body.polygon_points || "";
            const dwellMinutes = req.body.dwellMinutes || req.body.dwell_minutes || 0;
            const creatorId = req.body.creatorId || req.body.creator_id || req.body.created_by;

            if (!zoneName || isNaN(latitude) || isNaN(longitude)) {
                return res.status(400).json({ error: "INVALID_PARAMETERS", message: "Zone name, latitude and longitude are required." });
            }

            const cleanZoneId = zoneId || `ZONE_${Date.now()}`;
            const hostCreator = creatorId || "WEB_DISPATCHER";
            const cleanCreator = hostCreator.replace(/[\.\#\$\[\]\/]/g, "_");

            const zonePayload = {
                zone_id: cleanZoneId,
                zone_name: zoneName,
                lat: parseFloat(latitude),
                lng: parseFloat(longitude),
                radius: parseFloat(radius || 100),
                is_active: isActive !== false,
                alerts_enabled: Boolean(alertsEnabled),
                polygon_points: polygonPoints || "",
                dwell_minutes: parseInt(dwellMinutes || 0, 10),
                created_on: new Date().toISOString(),
                updated_at: Date.now(),
                created_by: cleanCreator,
                connected_devices: {}
            };

            if (Array.isArray(connectedDevices)) {
                connectedDevices.forEach(dId => {
                    if (dId) {
                        const cleanDId = dId.replace(/[\.\#\$\[\]\/]/g, "_");
                        zonePayload.connected_devices[cleanDId] = {
                            assigned_at: Date.now(),
                            device_name: dId,
                            status: "ACTIVE"
                        };
                    }
                });
            }

            await writeWithTimeout(`Geofencing/${cleanCreator}/safe_zones/${cleanZoneId}`, zonePayload, 3500);
            if (setSafeZonesCache) setSafeZonesCache({ data: null, timestamp: 0 });

            return res.json({
                status: "SUCCESS",
                message: `Safe Zone [${zoneName}] established successfully.`,
                zone: zonePayload
            });
        } catch (err) {
            console.error("[SAFE_ZONES] Error saving safe zone from web:", err);
            return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
        }
    });

    // POST /api/safezone/update-targets: Update Assigned Targets Endpoint
    router.post('/safezone/update-targets', async (req, res) => {
        const { creatorUid, zoneId, assignedTargets, connectedDevices } = req.body;
        if (!zoneId) return res.status(400).json({ error: "MISSING_ZONE_ID" });

        try {
            console.log(`[SAFE_ZONES] Updating targets for zone [${zoneId}] to: "${assignedTargets}"`);
            const snap = await queryNodeWithTimeout("Geofencing", 3000);
            const geofencing = (snap && typeof snap.val === 'function' ? snap.val() : null) || {};

            for (const [zKey, zNode] of Object.entries(geofencing)) {
                if (zNode && (zNode.zone_id === zoneId || zKey === zoneId || zKey.includes(zoneId))) {
                    await db.ref(`Geofencing/${zKey}`).update({
                        assigned_targets: assignedTargets || "Connected Devices",
                        connected_devices: Array.isArray(connectedDevices) ? connectedDevices : (zNode.connected_devices || []),
                        updated_at: Date.now()
                    });
                }
            }

            if (creatorUid) {
                await db.ref(`devices/${creatorUid}/safe_zones/${zoneId}`).update({
                    assigned_targets: assignedTargets || "Connected Devices",
                    updated_at: Date.now()
                }).catch(() => null);
            }

            if (setSafeZonesCache) setSafeZonesCache({ data: null, timestamp: 0 });
            res.json({ status: "SUCCESS", zoneId, assignedTargets });
        } catch (err) {
            console.error("[ERROR] /api/safezone/update-targets failed:", err.message);
            res.status(500).json({ error: err.message });
        }
    });

    // POST /api/safezone/delete: Decommission / Delete SafeZone Endpoint
    router.post('/safezone/delete', async (req, res) => {
        const zoneId = req.body.zoneId || req.body.zone_id;
        const creatorUid = req.body.creatorUid || req.body.creator_uid || req.body.creatorId || req.body.creator_id;
        if (!zoneId) return res.status(400).json({ error: "MISSING_ZONE_ID" });

        try {
            console.log(`[SAFE_ZONES] Decommissioning Safe Zone [${zoneId}] for Creator [${creatorUid || 'ANY'}]...`);
            const snap = await queryNodeWithTimeout("Geofencing", 3000);
            const geofencing = (snap && typeof snap.val === 'function' ? snap.val() : null) || {};

            const deleteOps = [];
            for (const [zKey, zNode] of Object.entries(geofencing)) {
                if (!zNode) continue;
                if (zNode.zone_id === zoneId || zKey === zoneId || zKey.includes(zoneId)) {
                    deleteOps.push(db.ref(`Geofencing/${zKey}`).remove().catch(() => null));
                }
                if (zNode.safe_zones && zNode.safe_zones[zoneId]) {
                    deleteOps.push(db.ref(`Geofencing/${zKey}/safe_zones/${zoneId}`).remove().catch(() => null));
                }
            }

            if (creatorUid) {
                const cleanCreator = creatorUid.replace(/[\.\#\$\[\]\/]/g, "_");
                deleteOps.push(db.ref(`Geofencing/${cleanCreator}/safe_zones/${zoneId}`).remove().catch(() => null));
                deleteOps.push(db.ref(`devices/${creatorUid}/safe_zones/${zoneId}`).remove().catch(() => null));
            }

            Promise.all(deleteOps).catch(() => null);
            if (setSafeZonesCache) setSafeZonesCache({ data: null, timestamp: 0 });
            res.json({ status: "DELETED", zoneId });
        } catch (err) {
            console.error("[ERROR] /api/safezone/delete failed:", err.message);
            res.status(500).json({ error: err.message });
        }
    });

    // POST /api/safezone/toggle-state: Safe Zone Operational State Toggle Endpoint
    router.post('/safezone/toggle-state', async (req, res) => {
        let zoneId = req.body.zoneId || req.body.zone_id;
        let isActive = req.body.isActive !== undefined ? req.body.isActive : req.body.is_active;
        let alertsEnabled = req.body.alertsEnabled !== undefined ? req.body.alertsEnabled : req.body.alerts_enabled;
        let creatorUid = req.body.creatorUid || req.body.creator_uid || req.body.creatorId || req.body.creator_id;
        if (!zoneId) {
            return res.status(400).json({ error: "MISSING_ZONE_ID" });
        }

        try {
            console.log(`[SAFEZONE_TOGGLE] Updating state for zone [${zoneId}]`);
            const updates = {};

            if (isActive !== undefined) {
                updates[`Geofencing/${zoneId}/isActive`] = Boolean(isActive);
                updates[`Geofencing/${zoneId}/status`] = isActive ? "ACTIVE" : "DISABLED";
            }
            if (alertsEnabled !== undefined) {
                updates[`Geofencing/${zoneId}/alertsEnabled`] = Boolean(alertsEnabled);
            }
            updates[`Geofencing/${zoneId}/updated_at`] = Date.now();

            if (creatorUid && resolveCanonicalAgentId) {
                const cleanUid = await resolveCanonicalAgentId(creatorUid);
                if (isActive !== undefined) {
                    updates[`devices/${cleanUid}/safe_zones/${zoneId}/isActive`] = Boolean(isActive);
                }
                if (alertsEnabled !== undefined) {
                    updates[`devices/${cleanUid}/safe_zones/${zoneId}/alertsEnabled`] = Boolean(alertsEnabled);
                }
            }

            writeWithTimeout("", updates, 2500, true);
            if (setLiveDevicesCache) setLiveDevicesCache({ data: null, timestamp: 0 });

            res.json({ status: "SUCCESS", zoneId, isActive, alertsEnabled, timestamp: Date.now() });
        } catch (err) {
            console.error("[ERROR] /api/safezone/toggle-state failed:", err.message);
            res.status(500).json({ error: err.message });
        }
    });

    return router;
}

module.exports = createSafezoneRouter;

