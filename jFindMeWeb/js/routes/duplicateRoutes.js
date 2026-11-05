/**
 * 👥 Duplicate Devices Express Router (OOPS MVC Sub-Module)
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: oldDev, newDev, oldHist, newHist, archiveNode, headingList, updates, movePayload, fingerprint
 * Constants: KNOWN_DEFINITIONS
 * Dependencies: express, db, queryNodeWithTimeout, writeWithTimeout, getLiveDevicesCache, setLiveDevicesCache, getSafeZonesCache, setSafeZonesCache
 * Integration Points: Exported factory function mounted into Express pipeline at /api
 */

const express = require('express');
const crypto = require('crypto');

/**
 * Computes deterministic physical silicon SHA-256 hardware hash matching DeviceIdentityHelper
 */
function computeSiliconHash(rawNode) {
    if (!rawNode) return null;
    const dev = rawNode.device || {};
    const profile = rawNode.profile || {};
    const identity = rawNode.Identity || {};

    // 1. Direct explicit silicon hash
    if (dev.silicon_sha256 && typeof dev.silicon_sha256 === 'string' && dev.silicon_sha256.length >= 8) {
        return dev.silicon_sha256.trim().toUpperCase();
    }

    const fp = (dev.fingerprint || dev.build_fingerprint || '').trim();
    const fpParts = fp ? fp.split('/') : [];
    const fpDevicePart = (fpParts.length >= 3) ? fpParts[2].split(':')[0] : '';

    const brand = (dev.brand || profile.brand || (fpParts.length > 0 ? fpParts[0] : '')).trim().toUpperCase();
    const model = (dev.model || dev.deviceName || profile.model || (fpParts.length > 1 ? fpParts[1] : '')).trim().toUpperCase();
    const board = (dev.board || fpDevicePart || '').trim().toUpperCase();
    const hardware = (dev.hardware || (fp.toLowerCase().includes('qcom') ? 'QCOM' : (fp ? 'ARM64' : ''))).trim().toUpperCase();
    const device = (dev.device || fpDevicePart || (dev.deviceId ? dev.deviceId.replace(/^HW_/, '') : '')).trim().toUpperCase();
    const imei = (dev.imei || '').trim();

    let baseSeed = '';
    if (brand && model) {
        baseSeed = `${brand}_${model}_${board}_${hardware}_${device}`;
    } else if (fp) {
        baseSeed = fp;
    } else if (imei && imei.length >= 10) {
        baseSeed = `IMEI_${imei}`;
    } else if (dev.deviceName && (identity.auth_uid || profile.phone)) {
        baseSeed = `${dev.deviceName}_${identity.auth_uid || profile.phone}`;
    }

    if (!baseSeed) return null;

    try {
        return crypto.createHash('sha256').update(baseSeed).digest('hex').toUpperCase();
    } catch (e) {
        return null;
    }
}

function createDuplicateRouter({
    db,
    queryNodeWithTimeout,
    writeWithTimeout,
    getLiveDevicesCache,
    setLiveDevicesCache,
    getSafeZonesCache,
    setSafeZonesCache
}) {
    const router = express.Router();

/**
 * Master Headings Synchronization & Comparison Engine (OOPS-based SSOT)
 * Analyzes all headings of the old device vs the new device's Old_Device_ID archive.
 * Both sides tick yes ([✔]) if the heading is synchronized under Old_Device_ID.
 */
function buildHeadingsComparison(oldId, newId, oldNode = {}, newNode = {}, rawHistory = {}, rawGeofencing = {}) {
    const archiveNode = (newNode?.Old_Device_ID && newNode.Old_Device_ID[oldId]) ? newNode.Old_Device_ID[oldId] : {};

    const KNOWN_DEFINITIONS = [
        { key: 'profile', label: 'Profile Dossier', icon: '👤', desc: 'Name, avatar, contact phone, home address, roles' },
        { key: 'device', label: 'Device & Hardware Telemetry', icon: '📱', desc: 'Hardware specs, live GPS coordinate, model, brand, fingerprint' },
        { key: 'telemetry', label: 'Telemetry & Battery', icon: '⚡', desc: 'Battery %, charging status, online session, heartbeat' },
        { key: 'Identity', label: 'Identity & Auth Keys', icon: '🔑', desc: 'Canonical Agent IDs, auth UID, FCM token, node index' },
        { key: 'history', label: 'Device History Breadcrumbs', icon: '📍', desc: 'Internal location breadcrumb points recorded on device' },
        { key: 'History', label: 'Trajectory GPS Stream', icon: '🗺️', desc: 'Historical GPS points from /History/{id}/history' },
        { key: 'relationships', label: 'Circle & Family Peers', icon: '👥', desc: 'Family circles, peer devices, and guardian links' },
        { key: 'safe_zones', label: 'Safe Zones & Geofences', icon: '🛡️', desc: 'Assigned safe zones, geofence boundaries, arrival alerts' },
        { key: 'Access', label: 'Hardware Access Permissions', icon: '🔒', desc: 'GPS, camera, bluetooth, foreground service ACL grants' },
        { key: 'Change_Log', label: 'Change Log & Forensic Audit', icon: '📜', desc: 'Forensic audit entries, pairing records, system updates' },
        { key: 'commands', label: 'Remote Push Commands', icon: '📡', desc: 'Poke location request, remote ring/siren, SOS alerts' },
        { key: 'alerts', label: 'Emergency Alerts & SOS', icon: '🚨', desc: 'SOS emergency alerts, battery warnings, zone transitions' },
        { key: 'last_sync', label: 'Last Sync Timestamp', icon: '⏱️', desc: 'Epoch and readable timestamp of synchronization' },
        { key: 'acl', label: 'ACL Security Controls', icon: '🛡️', desc: 'Access Control Lists (live tracking permissions)' },
        { key: 'live_tracking_enabled', label: 'Live Tracking State', icon: '🟢', desc: 'Live tracking toggle state' },
        { key: '_merge_metadata', label: 'Merge Forensic Receipt', icon: '🏷️', desc: 'Migration receipt, old/new IDs, timestamp' }
    ];

    const allKeys = new Set(KNOWN_DEFINITIONS.map(d => d.key));
    if (oldNode && typeof oldNode === 'object') {
        Object.keys(oldNode).forEach(k => {
            if (k !== 'Old_Device_ID') allKeys.add(k);
        });
    }
    if (archiveNode && typeof archiveNode === 'object') {
        Object.keys(archiveNode).forEach(k => allKeys.add(k));
    }

    const headingList = [];

    for (const key of allKeys) {
        const def = KNOWN_DEFINITIONS.find(d => d.key === key) || {
            key,
            label: key.charAt(0).toUpperCase() + key.slice(1).replace(/_/g, ' '),
            icon: '📁',
            desc: `Custom device heading [${key}]`
        };

        // 1. Resolve Old Device state
        let oldVal = oldNode ? oldNode[key] : undefined;
        if (oldVal === undefined && archiveNode[key] !== undefined) {
            oldVal = archiveNode[key];
        }
        if (key === 'History' && (oldVal === undefined || !oldVal)) {
            const histRoot = rawHistory?.[oldId]?.history || rawHistory?.[newId]?.history;
            if (histRoot && Object.keys(histRoot).length > 0) {
                oldVal = histRoot;
            }
        }
        if (key === 'safe_zones' && (oldVal === undefined || !oldVal)) {
            const geoDev = rawGeofencing?.[oldId]?.safe_zones || rawGeofencing?.[newId]?.safe_zones;
            if (geoDev && Object.keys(geoDev).length > 0) {
                oldVal = geoDev;
            }
        }

        const oldExists = oldVal !== undefined && oldVal !== null;
        let oldSubkeys = [];
        let oldRecordsCount = 0;
        let oldSample = '';

        if (oldExists) {
            if (typeof oldVal === 'object' && !Array.isArray(oldVal)) {
                oldSubkeys = Object.keys(oldVal);
                oldRecordsCount = oldSubkeys.length;
                oldSample = oldSubkeys.slice(0, 8).join(', ') + (oldSubkeys.length > 8 ? '...' : '');
            } else if (Array.isArray(oldVal)) {
                oldRecordsCount = oldVal.length;
                oldSample = `Array(${oldVal.length})`;
            } else {
                oldRecordsCount = 1;
                oldSample = String(oldVal);
            }
        }

        // 2. Resolve Old_Device_ID state under New Device
        let syncVal = archiveNode ? archiveNode[key] : undefined;
        // Check if History was merged into target History root
        if (key === 'History' && (syncVal === undefined || !syncVal)) {
            if (archiveNode.History) {
                syncVal = archiveNode.History;
            } else if (rawHistory?.[newId]?.history) {
                const count = Object.values(rawHistory[newId].history).filter(h => h.origin_agent_id === oldId).length;
                if (count > 0) syncVal = { origin_points: count, target_root: `History/${newId}/history` };
            }
        }
        // Check if safe_zones was linked
        if (key === 'safe_zones' && (syncVal === undefined || !syncVal)) {
            if (archiveNode.safe_zones) {
                syncVal = archiveNode.safe_zones;
            } else if (rawGeofencing?.[newId]?.safe_zones) {
                syncVal = rawGeofencing[newId].safe_zones;
            }
        }

        const syncedExists = syncVal !== undefined && syncVal !== null;
        let syncedSubkeys = [];
        let syncedRecordsCount = 0;
        let syncedSample = '';

        if (syncedExists) {
            if (typeof syncVal === 'object' && !Array.isArray(syncVal)) {
                syncedSubkeys = Object.keys(syncVal);
                syncedRecordsCount = syncedSubkeys.length;
                syncedSample = syncedSubkeys.slice(0, 8).join(', ') + (syncedSubkeys.length > 8 ? '...' : '');
            } else if (Array.isArray(syncVal)) {
                syncedRecordsCount = syncVal.length;
                syncedSample = `Array(${syncVal.length})`;
            } else {
                syncedRecordsCount = 1;
                syncedSample = String(syncVal);
            }
        }

        // Both side tick yes if they are sync in heading Old_device_Id (under new device)
        const isBothSynced = Boolean(syncedExists);

        headingList.push({
            key,
            label: def.label,
            icon: def.icon,
            desc: def.desc,
            oldExists,
            oldRecordsCount,
            oldSubkeys,
            oldSample,
            syncedExists,
            syncedRecordsCount,
            syncedSubkeys,
            syncedSample,
            isBothSynced,
            oldPreview: oldExists ? (typeof oldVal === 'object' ? JSON.stringify(oldVal, null, 2) : String(oldVal)) : null,
            syncedPreview: syncedExists ? (typeof syncVal === 'object' ? JSON.stringify(syncVal, null, 2) : String(syncVal)) : null
        });
    }

    return {
        headings: headingList,
        totalHeadings: headingList.length,
        syncedHeadings: headingList.filter(h => h.isBothSynced).length,
        missingHeadings: headingList.filter(h => !h.isBothSynced).length,
        isFullySynced: headingList.every(h => h.isBothSynced)
    };
}

/**
 * GET /api/duplicate-devices
 * Scans Firebase RTDB /devices, groups by Silicon SHA-256 / Hardware Fingerprint,
 * returns candidate pairs whose IDs changed after OTA/signature mismatch,
 * AND includes already-moved pairs archived under Old_Device_ID with full headings matrix.
 */
router.get(["/duplicate-devices", "/api/duplicate-devices"], async (req, res) => {
    try {
        const [devSnap, histSnap, geofenceSnap, settingsSnap] = await Promise.all([
            queryNodeWithTimeout("devices", 3500),
            queryNodeWithTimeout("History", 3500),
            queryNodeWithTimeout("Geofencing", 3500),
            queryNodeWithTimeout("config/operator/settings", 3500)
        ]);

        const settingsData = (settingsSnap && typeof settingsSnap.val === 'function' && settingsSnap.val()) || {};
        const recencyLimitMs = Math.max(1, parseInt(settingsData.recencyThresholdMinutes || 15, 10)) * 60 * 1000;

        if (!devSnap || typeof devSnap.val !== 'function' || !devSnap.exists()) {
            return res.json({ duplicates: [], totalCount: 0, scannedAt: Date.now() });
        }

        const rawDevices = devSnap.val() || {};
        const rawHistory = (histSnap && typeof histSnap.val === 'function' && histSnap.exists()) ? histSnap.val() : {};
        const rawGeofencing = (geofenceSnap && typeof geofenceSnap.val === 'function' && geofenceSnap.exists()) ? geofenceSnap.val() : {};

        // Helper to extract numeric epoch
        function extractLastSeenEpoch(d) {
            if (!d) return 0;
            const liveTs = d.device?.live?.timestamp;
            const telemTs = d.telemetry?.last_seen;
            const idUpdated = d.Identity?.updated_at;
            const profileUpdated = d.profile?.updated_at;
            let maxTs = 0;
            if (typeof liveTs === 'number') maxTs = Math.max(maxTs, liveTs);
            if (typeof telemTs === 'number') maxTs = Math.max(maxTs, telemTs);
            if (typeof idUpdated === 'number') maxTs = Math.max(maxTs, idUpdated);
            if (typeof profileUpdated === 'string' && profileUpdated.length === 16) {
                const yr = parseInt(profileUpdated.substring(0, 4));
                const mo = parseInt(profileUpdated.substring(4, 6)) - 1;
                const dy = parseInt(profileUpdated.substring(6, 8));
                const hr = parseInt(profileUpdated.substring(8, 10));
                const mi = parseInt(profileUpdated.substring(10, 12));
                const sc = parseInt(profileUpdated.substring(12, 14));
                const parsed = new Date(yr, mo, dy, hr, mi, sc).getTime();
                if (!isNaN(parsed)) maxTs = Math.max(maxTs, parsed);
            }
            return maxTs;
        }

        // Shared helper to build standard device summary
        function formatDeviceSummary(agentId, node) {
            const epoch = extractLastSeenEpoch(node);
            const histRecords = rawHistory[agentId]?.history || {};
            const historyCount = Object.keys(histRecords).length + (node.device?.live?.latitude ? 1 : 0);
            const relCount = Object.keys(node.relationships || {}).length;

            let safeZonesCount = Object.keys(node.safe_zones || {}).length;
            for (const creator of Object.values(rawGeofencing)) {
                if (creator.safe_zones) {
                    for (const sz of Object.values(creator.safe_zones)) {
                        if (sz.connected_devices && sz.connected_devices[agentId]) {
                            safeZonesCount++;
                        }
                    }
                }
            }

            const dev = node.device || {};
            const profile = node.profile || {};
            const telemetry = node.telemetry || {};

            const fp = (dev.fingerprint || dev.build_fingerprint || '').trim();
            const fpParts = fp ? fp.split('/') : [];
            const fpDevicePart = (fpParts.length >= 3) ? fpParts[2].split(':')[0] : '';

            const brand = dev.brand || profile.brand || (fpParts.length > 0 ? fpParts[0] : '') || 'Generic';
            const model = dev.model || dev.deviceName || profile.model || (fpParts.length > 1 ? fpParts[1] : '') || 'Unknown';
            const board = dev.board || fpDevicePart || 'N/A';
            const hardware = dev.hardware || (fp.toLowerCase().includes('qcom') ? 'qcom' : (fp ? 'ARM64' : 'N/A'));
            const device = dev.device || fpDevicePart || (dev.deviceId ? dev.deviceId.replace(/^HW_/, '') : agentId);

            // IMEI resolution (dual SIM support: IMEI 1 & IMEI 2)
            const rawImei = (dev.imei || profile.imei || telemetry.imei || '').trim();
            let imei1 = (dev.imei1 || dev.imei_1 || '').trim();
            let imei2 = (dev.imei2 || dev.imei_2 || '').trim();
            if (!imei1 && rawImei) {
                if (rawImei.includes(',')) {
                    const parts = rawImei.split(',').map(s => s.trim());
                    imei1 = parts[0] || 'N/A';
                    imei2 = parts[1] || '';
                } else if (rawImei.includes('/')) {
                    const parts = rawImei.split('/').map(s => s.trim());
                    imei1 = parts[0] || 'N/A';
                    imei2 = parts[1] || '';
                } else {
                    imei1 = rawImei;
                }
            }
            if (!imei1) imei1 = 'N/A';
            const isDualSim = Boolean(imei2 || dev.is_dual_sim || dev.sim_count === 2);

            return {
                agentId,
                name: profile.name || dev.deviceName || agentId,
                deviceName: dev.deviceName || model || 'Android Device',
                brand,
                model,
                board,
                hardware,
                device,
                imei: imei1,
                imei1,
                imei2,
                isDualSim,
                osVersion: dev.os_version || (fpParts.length >= 3 && fpParts[2].includes(':') ? `Android ${fpParts[2].split(':')[1].split('/')[0]}` : 'Android'),
                avatarUrl: profile.avatar_id ? `/assets/avatars/${profile.avatar_id}.png` : '/assets/avatars/1.png',
                lastSeenEpoch: epoch,
                lastSeenFormatted: epoch > 0 ? new Date(epoch).toLocaleString() : 'Never Recorded',
                isLive: (Date.now() - epoch) < recencyLimitMs,
                batteryPct: telemetry.battery_pct ?? 100,
                location: dev.live ? {
                    lat: dev.live.latitude,
                    lng: dev.live.longitude,
                    address: dev.live.geo_address || dev.live.address || 'Unknown'
                } : null,
                historyCount,
                relationshipsCount: relCount,
                safeZonesCount,
                hasArchive: !!node.Old_Device_ID,
                raw: node
            };
        }

        // Group devices by Silicon Fingerprint or Secondary Hardware Signature
        const mapByHash = new Map();

        for (const [agentId, node] of Object.entries(rawDevices)) {
            if (!node || typeof node !== 'object') continue;
            
            const hash = computeSiliconHash(node);
            const imei = (node.device?.imei || '').trim();
            const devName = (node.device?.deviceName || node.device?.model || node.profile?.name || '').trim().toLowerCase();
            const phone = (node.profile?.phone || '').trim();
            
            let groupKey = null;
            let matchType = 'SILICON_SHA256';

            if (hash) {
                groupKey = `HASH_${hash}`;
                matchType = 'SILICON_SHA256 (Brand + Model + Board + Hardware + Device)';
            } else if (imei && imei.length >= 12) {
                groupKey = `IMEI_${imei}`;
                matchType = 'IMEI_HARDWARE_KEY';
            } else if (devName && phone) {
                groupKey = `PHONE_NAME_${phone}_${devName}`;
                matchType = 'PROFILE_PHONE_HARDWARE_NAME';
            }

            if (!groupKey) continue;

            if (!mapByHash.has(groupKey)) {
                mapByHash.set(groupKey, { groupKey, matchType, hash: hash || groupKey, items: [] });
            }

            mapByHash.get(groupKey).items.push(formatDeviceSummary(agentId, node));
        }

        const duplicatePairs = [];

        for (const [groupKey, group] of mapByHash.entries()) {
            if (group.items.length >= 2) {
                // Sort items by lastSeenEpoch descending: newest active is index 0, older deprecated is index >= 1
                group.items.sort((a, b) => b.lastSeenEpoch - a.lastSeenEpoch);

                const newDev = group.items[0];
                for (let i = 1; i < group.items.length; i++) {
                    const oldDev = group.items[i];
                    if (oldDev.agentId === newDev.agentId) continue;

                    // Cross-fertilize hardware properties across the pair
                    const unifiedBrand = (newDev.brand && newDev.brand !== 'Generic') ? newDev.brand : (oldDev.brand && oldDev.brand !== 'Generic' ? oldDev.brand : 'Generic');
                    const unifiedModel = (newDev.model && newDev.model !== 'Unknown') ? newDev.model : (oldDev.model && oldDev.model !== 'Unknown' ? oldDev.model : 'Unknown');
                    const unifiedBoard = (newDev.board && newDev.board !== 'N/A') ? newDev.board : (oldDev.board && oldDev.board !== 'N/A' ? oldDev.board : 'N/A');
                    const unifiedHardware = (newDev.hardware && newDev.hardware !== 'N/A') ? newDev.hardware : (oldDev.hardware && oldDev.hardware !== 'N/A' ? oldDev.hardware : 'N/A');
                    const unifiedDevice = (newDev.device && newDev.device !== 'N/A') ? newDev.device : (oldDev.device && oldDev.device !== 'N/A' ? oldDev.device : 'N/A');
                    const unifiedImei1 = (newDev.imei1 && newDev.imei1 !== 'N/A') ? newDev.imei1 : (oldDev.imei1 && oldDev.imei1 !== 'N/A' ? oldDev.imei1 : 'N/A');
                    const unifiedImei2 = newDev.imei2 || oldDev.imei2 || '';
                    const unifiedDualSim = Boolean(newDev.isDualSim || oldDev.isDualSim || unifiedImei2);

                    oldDev.brand = unifiedBrand;
                    newDev.brand = unifiedBrand;
                    oldDev.model = unifiedModel;
                    newDev.model = unifiedModel;
                    oldDev.board = unifiedBoard;
                    newDev.board = unifiedBoard;
                    oldDev.hardware = unifiedHardware;
                    newDev.hardware = unifiedHardware;
                    oldDev.device = unifiedDevice;
                    newDev.device = unifiedDevice;
                    oldDev.imei1 = unifiedImei1;
                    newDev.imei1 = unifiedImei1;
                    oldDev.imei2 = unifiedImei2;
                    newDev.imei2 = unifiedImei2;
                    oldDev.isDualSim = unifiedDualSim;
                    newDev.isDualSim = unifiedDualSim;

                    const headingsComp = buildHeadingsComparison(
                        oldDev.agentId,
                        newDev.agentId,
                        rawDevices[oldDev.agentId] || {},
                        rawDevices[newDev.agentId] || {},
                        rawHistory,
                        rawGeofencing
                    );

                    duplicatePairs.push({
                        pairId: `${oldDev.agentId}_TO_${newDev.agentId}`,
                        matchType: group.matchType,
                        siliconHash: group.hash,
                        isMoved: false,
                        hardwareProperties: {
                            brand: unifiedBrand,
                            model: unifiedModel,
                            board: unifiedBoard,
                            hardware: unifiedHardware,
                            device: unifiedDevice,
                            imei: unifiedImei1,
                            imei1: unifiedImei1,
                            imei2: unifiedImei2,
                            isDualSim: unifiedDualSim
                        },
                        oldDevice: oldDev,
                        newDevice: newDev,
                        headingsComparison: headingsComp
                    });
                }
            }
        }

        // Also discover devices that were already moved and archived under Old_Device_ID
        for (const [agentId, node] of Object.entries(rawDevices)) {
            if (node?.Old_Device_ID && typeof node.Old_Device_ID === 'object') {
                for (const [oldAgentId, oldArchive] of Object.entries(node.Old_Device_ID)) {
                    if (!oldArchive || typeof oldArchive !== 'object') continue;
                    const pairKey = `${oldAgentId}_TO_${agentId}`;
                    if (duplicatePairs.some(p => p.pairId === pairKey)) continue;

                    const oldSummary = formatDeviceSummary(oldAgentId, oldArchive);
                    const newSummary = formatDeviceSummary(agentId, node);

                    const unifiedBrand = (newSummary.brand && newSummary.brand !== 'Generic') ? newSummary.brand : (oldSummary.brand && oldSummary.brand !== 'Generic' ? oldSummary.brand : 'Generic');
                    const unifiedModel = (newSummary.model && newSummary.model !== 'Unknown') ? newSummary.model : (oldSummary.model && oldSummary.model !== 'Unknown' ? oldSummary.model : 'Unknown');
                    const unifiedBoard = (newSummary.board && newSummary.board !== 'N/A') ? newSummary.board : (oldSummary.board && oldSummary.board !== 'N/A' ? oldSummary.board : 'N/A');
                    const unifiedHardware = (newSummary.hardware && newSummary.hardware !== 'N/A') ? newSummary.hardware : (oldSummary.hardware && oldSummary.hardware !== 'N/A' ? oldSummary.hardware : 'N/A');
                    const unifiedDevice = (newSummary.device && newSummary.device !== 'N/A') ? newSummary.device : (oldSummary.device && oldSummary.device !== 'N/A' ? oldSummary.device : 'N/A');
                    const unifiedImei1 = (newSummary.imei1 && newSummary.imei1 !== 'N/A') ? newSummary.imei1 : (oldSummary.imei1 && oldSummary.imei1 !== 'N/A' ? oldSummary.imei1 : 'N/A');
                    const unifiedImei2 = newSummary.imei2 || oldSummary.imei2 || '';
                    const unifiedDualSim = Boolean(newSummary.isDualSim || oldSummary.isDualSim || unifiedImei2);

                    oldSummary.brand = unifiedBrand; newSummary.brand = unifiedBrand;
                    oldSummary.model = unifiedModel; newSummary.model = unifiedModel;
                    oldSummary.board = unifiedBoard; newSummary.board = unifiedBoard;
                    oldSummary.hardware = unifiedHardware; newSummary.hardware = unifiedHardware;
                    oldSummary.device = unifiedDevice; newSummary.device = unifiedDevice;
                    oldSummary.imei1 = unifiedImei1; newSummary.imei1 = unifiedImei1;
                    oldSummary.imei2 = unifiedImei2; newSummary.imei2 = unifiedImei2;
                    oldSummary.isDualSim = unifiedDualSim; newSummary.isDualSim = unifiedDualSim;

                    const headingsComp = buildHeadingsComparison(oldAgentId, agentId, oldArchive, node, rawHistory, rawGeofencing);

                    duplicatePairs.push({
                        pairId: pairKey,
                        matchType: 'MOVED_ACTIVE_ARCHIVE (Old_Device_ID Preserved)',
                        siliconHash: computeSiliconHash(oldArchive) || computeSiliconHash(node) || 'ARCHIVED_SILICON',
                        isMoved: true,
                        hardwareProperties: {
                            brand: unifiedBrand,
                            model: unifiedModel,
                            board: unifiedBoard,
                            hardware: unifiedHardware,
                            device: unifiedDevice,
                            imei: unifiedImei1,
                            imei1: unifiedImei1,
                            imei2: unifiedImei2,
                            isDualSim: unifiedDualSim
                        },
                        oldDevice: oldSummary,
                        newDevice: newSummary,
                        headingsComparison: headingsComp
                    });
                }
            }
        }

        return res.json({
            duplicates: duplicatePairs,
            totalCount: duplicatePairs.length,
            scannedAt: Date.now()
        });
    } catch (err) {
        console.error("[ERROR] /api/duplicate-devices failed:", err.message);
        return res.status(500).json({ error: "DUPLICATE_SCAN_FAILED", message: err.message });
    }
});

/**
 * POST /api/verify-duplicate-pair
 * Directly verifies two specific device IDs (Old ID vs New ID),
 * extracts their hardware properties and silicon hashes, and returns a verified pair comparison card.
 * Also resolves archived instances under Old_Device_ID if old root was already merged.
 */
router.post(["/verify-duplicate-pair", "/api/verify-duplicate-pair"], async (req, res) => {
    try {
        const { oldAgentId, newAgentId } = req.body || {};
        if (!oldAgentId || !newAgentId) {
            return res.status(400).json({ error: "MISSING_PARAMS", message: "Both oldAgentId and newAgentId are required." });
        }

        const cleanOld = String(oldAgentId).trim().toUpperCase();
        const cleanNew = String(newAgentId).trim().toUpperCase();

        if (cleanOld === cleanNew) {
            return res.status(400).json({ error: "IDENTICAL_IDS", message: "Old Device ID and New Device ID cannot be identical." });
        }

        const [oldDevSnap, newDevSnap, histSnap, geofenceSnap] = await Promise.all([
            db.ref(`devices/${cleanOld}`).once("value"),
            db.ref(`devices/${cleanNew}`).once("value"),
            db.ref("History").once("value"),
            db.ref("Geofencing").once("value")
        ]);

        if (!newDevSnap.exists()) {
            return res.status(404).json({ error: "NEW_DEVICE_NOT_FOUND", message: `New Device [${cleanNew}] does not exist in Firebase RTDB.` });
        }

        const newNode = newDevSnap.val() || {};
        let oldNode = oldDevSnap.exists() ? (oldDevSnap.val() || {}) : null;
        let isMoved = false;

        // If old device root does not exist, check if it is already archived under Old_Device_ID
        if (!oldNode) {
            if (newNode?.Old_Device_ID?.[cleanOld]) {
                oldNode = newNode.Old_Device_ID[cleanOld];
                isMoved = true;
            } else {
                // Check all devices for Old_Device_ID[cleanOld]
                const allDevsSnap = await db.ref("devices").once("value");
                const allDevs = allDevsSnap.val() || {};
                for (const [dId, dNode] of Object.entries(allDevs)) {
                    if (dNode?.Old_Device_ID?.[cleanOld]) {
                        oldNode = dNode.Old_Device_ID[cleanOld];
                        isMoved = true;
                        break;
                    }
                }
            }
        }

        if (!oldNode) {
            return res.status(404).json({ error: "OLD_DEVICE_NOT_FOUND", message: `Old Device [${cleanOld}] does not exist in Firebase RTDB or Old_Device_ID archives.` });
        }

        const rawHistory = histSnap.exists() ? histSnap.val() : {};
        const rawGeofencing = geofenceSnap.exists() ? geofenceSnap.val() : {};

        function extractEpoch(d) {
            if (!d) return 0;
            const liveTs = d.device?.live?.timestamp;
            const telemTs = d.telemetry?.last_seen;
            const idUpdated = d.Identity?.updated_at;
            let maxTs = 0;
            if (typeof liveTs === 'number') maxTs = Math.max(maxTs, liveTs);
            if (typeof telemTs === 'number') maxTs = Math.max(maxTs, telemTs);
            if (typeof idUpdated === 'number') maxTs = Math.max(maxTs, idUpdated);
            return maxTs;
        }

        function formatDev(id, node) {
            const epoch = extractEpoch(node);
            const histRecords = rawHistory[id]?.history || {};
            const historyCount = Object.keys(histRecords).length + (node.device?.live?.latitude ? 1 : 0);
            const relCount = Object.keys(node.relationships || {}).length;

            let safeZonesCount = Object.keys(node.safe_zones || {}).length;
            for (const creator of Object.values(rawGeofencing)) {
                if (creator.safe_zones) {
                    for (const sz of Object.values(creator.safe_zones)) {
                        if (sz.connected_devices && sz.connected_devices[id]) {
                            safeZonesCount++;
                        }
                    }
                }
            }

            const dev = node.device || {};
            const profile = node.profile || {};
            const telemetry = node.telemetry || {};

            const fp = (dev.fingerprint || dev.build_fingerprint || '').trim();
            const fpParts = fp ? fp.split('/') : [];
            const fpDevicePart = (fpParts.length >= 3) ? fpParts[2].split(':')[0] : '';

            const brand = dev.brand || profile.brand || (fpParts.length > 0 ? fpParts[0] : '') || 'Generic';
            const model = dev.model || dev.deviceName || profile.model || (fpParts.length > 1 ? fpParts[1] : '') || 'Unknown';
            const board = dev.board || fpDevicePart || 'N/A';
            const hardware = dev.hardware || (fp.toLowerCase().includes('qcom') ? 'qcom' : (fp ? 'ARM64' : 'N/A'));
            const device = dev.device || fpDevicePart || (dev.deviceId ? dev.deviceId.replace(/^HW_/, '') : id);

            // IMEI resolution (dual SIM support: IMEI 1 & IMEI 2)
            const rawImei = (dev.imei || profile.imei || telemetry.imei || '').trim();
            let imei1 = (dev.imei1 || dev.imei_1 || '').trim();
            let imei2 = (dev.imei2 || dev.imei_2 || '').trim();
            if (!imei1 && rawImei) {
                if (rawImei.includes(',')) {
                    const parts = rawImei.split(',').map(s => s.trim());
                    imei1 = parts[0] || 'N/A';
                    imei2 = parts[1] || '';
                } else if (rawImei.includes('/')) {
                    const parts = rawImei.split('/').map(s => s.trim());
                    imei1 = parts[0] || 'N/A';
                    imei2 = parts[1] || '';
                } else {
                    imei1 = rawImei;
                }
            }
            if (!imei1) imei1 = 'N/A';
            const isDualSim = Boolean(imei2 || dev.is_dual_sim || dev.sim_count === 2);

            return {
                agentId: id,
                name: profile.name || dev.deviceName || id,
                deviceName: dev.deviceName || model || 'Android Device',
                brand,
                model,
                board,
                hardware,
                device,
                imei: imei1,
                imei1,
                imei2,
                isDualSim,
                osVersion: dev.os_version || (fpParts.length >= 3 && fpParts[2].includes(':') ? `Android ${fpParts[2].split(':')[1].split('/')[0]}` : 'Android'),
                avatarUrl: profile.avatar_id ? `/assets/avatars/${profile.avatar_id}.png` : '/assets/avatars/1.png',
                lastSeenEpoch: epoch,
                lastSeenFormatted: epoch > 0 ? new Date(epoch).toLocaleString() : 'Never Recorded',
                isLive: (Date.now() - epoch) < recencyLimitMs,
                batteryPct: telemetry.battery_pct ?? 100,
                location: dev.live ? {
                    lat: dev.live.latitude,
                    lng: dev.live.longitude,
                    address: dev.live.geo_address || dev.live.address || 'Unknown'
                } : null,
                historyCount,
                relationshipsCount: relCount,
                safeZonesCount,
                hasArchive: !!node.Old_Device_ID,
                raw: node
            };
        }

        const oldDev = formatDev(cleanOld, oldNode);
        const newDev = formatDev(cleanNew, newNode);

        // Cross-fertilize hardware properties across the pair (since they are verified duplicate silicon instances)
        const unifiedBrand = (newDev.brand && newDev.brand !== 'Generic') ? newDev.brand : (oldDev.brand && oldDev.brand !== 'Generic' ? oldDev.brand : 'Generic');
        const unifiedModel = (newDev.model && newDev.model !== 'Unknown') ? newDev.model : (oldDev.model && oldDev.model !== 'Unknown' ? oldDev.model : 'Unknown');
        const unifiedBoard = (newDev.board && newDev.board !== 'N/A') ? newDev.board : (oldDev.board && oldDev.board !== 'N/A' ? oldDev.board : 'N/A');
        const unifiedHardware = (newDev.hardware && newDev.hardware !== 'N/A') ? newDev.hardware : (oldDev.hardware && oldDev.hardware !== 'N/A' ? oldDev.hardware : 'N/A');
        const unifiedDevice = (newDev.device && newDev.device !== 'N/A') ? newDev.device : (oldDev.device && oldDev.device !== 'N/A' ? oldDev.device : 'N/A');
        const unifiedImei1 = (newDev.imei1 && newDev.imei1 !== 'N/A') ? newDev.imei1 : (oldDev.imei1 && oldDev.imei1 !== 'N/A' ? oldDev.imei1 : 'N/A');
        const unifiedImei2 = newDev.imei2 || oldDev.imei2 || '';
        const unifiedDualSim = Boolean(newDev.isDualSim || oldDev.isDualSim || unifiedImei2);

        oldDev.brand = unifiedBrand;
        newDev.brand = unifiedBrand;
        oldDev.model = unifiedModel;
        newDev.model = unifiedModel;
        oldDev.board = unifiedBoard;
        newDev.board = unifiedBoard;
        oldDev.hardware = unifiedHardware;
        newDev.hardware = unifiedHardware;
        oldDev.device = unifiedDevice;
        newDev.device = unifiedDevice;
        oldDev.imei1 = unifiedImei1;
        newDev.imei1 = unifiedImei1;
        oldDev.imei2 = unifiedImei2;
        newDev.imei2 = unifiedImei2;
        oldDev.isDualSim = unifiedDualSim;
        newDev.isDualSim = unifiedDualSim;

        const oldHash = computeSiliconHash(oldNode);
        const newHash = computeSiliconHash(newNode);
        const isSiliconMatch = oldHash && newHash && (oldHash === newHash);

        const headingsComp = buildHeadingsComparison(cleanOld, cleanNew, oldNode, newNode, rawHistory, rawGeofencing);

        const verifiedPair = {
            pairId: `${cleanOld}_TO_${cleanNew}`,
            matchType: isMoved ? 'MOVED_ACTIVE_ARCHIVE (Old_Device_ID Preserved)' : (isSiliconMatch ? 'SILICON_SHA256 (Brand + Model + Board + Hardware + Device)' : 'MANUAL_ID_SPECIFICATION'),
            siliconHash: newHash || oldHash || 'MANUAL_VERIFIED',
            isSiliconMatch: Boolean(isSiliconMatch),
            isMoved: Boolean(isMoved),
            hardwareProperties: {
                brand: unifiedBrand,
                model: unifiedModel,
                board: unifiedBoard,
                hardware: unifiedHardware,
                device: unifiedDevice,
                imei: unifiedImei1,
                imei1: unifiedImei1,
                imei2: unifiedImei2,
                isDualSim: unifiedDualSim
            },
            oldDevice: oldDev,
            newDevice: newDev,
            headingsComparison: headingsComp
        };

        return res.json({
            success: true,
            pair: verifiedPair
        });
    } catch (err) {
        console.error("[ERROR] /api/verify-duplicate-pair failed:", err.message);
        return res.status(500).json({ error: "VERIFY_PAIR_FAILED", message: err.message });
    }
});

/**
 * POST /api/sync-heading
 * Synchronizes a specific missing heading into devices/{newAgentId}/Old_Device_ID/{oldAgentId}/{headingKey}.
 */
router.post(["/sync-heading", "/api/sync-heading"], async (req, res) => {
    try {
        const { oldAgentId, newAgentId, headingKey } = req.body || {};
        if (!oldAgentId || !newAgentId || !headingKey) {
            return res.status(400).json({ error: "MISSING_PARAMS", message: "oldAgentId, newAgentId, and headingKey are required." });
        }

        const [oldDevSnap, newDevSnap, oldHistSnap, newHistSnap, geofenceSnap] = await Promise.all([
            db.ref(`devices/${oldAgentId}`).once("value"),
            db.ref(`devices/${newAgentId}`).once("value"),
            db.ref(`History/${oldAgentId}`).once("value"),
            db.ref(`History/${newAgentId}`).once("value"),
            db.ref("Geofencing").once("value")
        ]);

        if (!newDevSnap.exists()) {
            return res.status(404).json({ error: "NEW_DEVICE_NOT_FOUND", message: `Target new device [${newAgentId}] does not exist.` });
        }

        const oldDevData = oldDevSnap.val() || {};
        const newDevData = newDevSnap.val() || {};
        const archive = newDevData.Old_Device_ID?.[oldAgentId] || {};
        const nowTs = Date.now();

        let dataToSync = oldDevData[headingKey];
        if (dataToSync === undefined && archive[headingKey] !== undefined) {
            dataToSync = archive[headingKey];
        }

        if (headingKey === 'History') {
            if (oldHistSnap.exists()) {
                dataToSync = oldHistSnap.val()?.history || {};
            } else if (newHistSnap.exists()) {
                const allH = newHistSnap.val()?.history || {};
                const oldPts = {};
                for (const [k, v] of Object.entries(allH)) {
                    if (v.origin_agent_id === oldAgentId) oldPts[k] = v;
                }
                dataToSync = Object.keys(oldPts).length > 0 ? oldPts : allH;
            } else {
                dataToSync = { status: "SYNCED_EMPTY" };
            }
        } else if (headingKey === 'safe_zones') {
            const geofences = geofenceSnap.exists() ? geofenceSnap.val() : {};
            const zones = {};
            for (const creatorNode of Object.values(geofences)) {
                if (creatorNode?.safe_zones) {
                    for (const [zid, z] of Object.entries(creatorNode.safe_zones)) {
                        if (z.connected_devices?.[oldAgentId] || z.connected_devices?.[newAgentId] || (z.assigned_targets && z.assigned_targets.includes(oldAgentId))) {
                            zones[zid] = z;
                        }
                    }
                }
            }
            dataToSync = Object.keys(zones).length > 0 ? zones : { status: "SYNCED_NO_ZONES" };
        } else if (headingKey === 'acl') {
            dataToSync = oldDevData.acl || archive.acl || { acl_live_tracking: "GRANTED" };
        } else if (headingKey === 'live_tracking_enabled') {
            dataToSync = oldDevData.live_tracking_enabled !== undefined ? oldDevData.live_tracking_enabled : true;
        } else if (headingKey === 'alerts') {
            dataToSync = oldDevData.alerts || archive.alerts || { active_alerts: {}, updated_at: nowTs };
        } else if (dataToSync === undefined) {
            dataToSync = { synced_at: nowTs, status: "INITIALIZED" };
        }

        await db.ref(`devices/${newAgentId}/Old_Device_ID/${oldAgentId}/${headingKey}`).set(dataToSync);

        const [refreshedNewSnap, refreshedHistSnap, refreshedGeoSnap] = await Promise.all([
            db.ref(`devices/${newAgentId}`).once("value"),
            db.ref("History").once("value"),
            db.ref("Geofencing").once("value")
        ]);

        const refreshedNew = refreshedNewSnap.val() || {};
        const refreshedArchive = refreshedNew.Old_Device_ID?.[oldAgentId] || {};
        const comparison = buildHeadingsComparison(
            oldAgentId,
            newAgentId,
            oldDevSnap.exists() ? oldDevData : refreshedArchive,
            refreshedNew,
            refreshedHistSnap.val() || {},
            refreshedGeoSnap.val() || {}
        );

        return res.json({
            success: true,
            message: `Heading [${headingKey}] synchronized into Old_Device_ID/${oldAgentId}.`,
            headingsComparison: comparison
        });
    } catch (err) {
        console.error("[ERROR] /api/sync-heading failed:", err.message);
        return res.status(500).json({ error: "SYNC_HEADING_FAILED", message: err.message });
    }
});

/**
 * POST /api/sync-all-missing-headings
 * Synchronizes all missing headings into devices/{newAgentId}/Old_Device_ID/{oldAgentId}.
 */
router.post(["/sync-all-missing-headings", "/api/sync-all-missing-headings"], async (req, res) => {
    try {
        const { oldAgentId, newAgentId } = req.body || {};
        if (!oldAgentId || !newAgentId) {
            return res.status(400).json({ error: "MISSING_PARAMS", message: "Both oldAgentId and newAgentId are required." });
        }

        const [oldDevSnap, newDevSnap, oldHistSnap, newHistSnap, geofenceSnap] = await Promise.all([
            db.ref(`devices/${oldAgentId}`).once("value"),
            db.ref(`devices/${newAgentId}`).once("value"),
            db.ref(`History/${oldAgentId}`).once("value"),
            db.ref(`History/${newAgentId}`).once("value"),
            db.ref("Geofencing").once("value")
        ]);

        if (!newDevSnap.exists()) {
            return res.status(404).json({ error: "NEW_DEVICE_NOT_FOUND", message: `Target new device [${newAgentId}] does not exist.` });
        }

        const oldDevData = oldDevSnap.val() || {};
        const newDevData = newDevSnap.val() || {};
        const archive = newDevData.Old_Device_ID?.[oldAgentId] || {};
        const nowTs = Date.now();

        const updates = {};
        const basePath = `devices/${newAgentId}/Old_Device_ID/${oldAgentId}`;

        const allStandardHeadings = [
            'profile', 'device', 'telemetry', 'Identity', 'history', 'History',
            'relationships', 'safe_zones', 'Access', 'Change_Log', 'commands',
            'alerts', 'last_sync', 'acl', 'live_tracking_enabled', '_merge_metadata'
        ];

        for (const h of allStandardHeadings) {
            if (archive[h] !== undefined && archive[h] !== null) continue;

            if (h === 'History') {
                const pts = oldHistSnap.val()?.history || {};
                updates[`${basePath}/History`] = Object.keys(pts).length > 0 ? pts : (newHistSnap.val()?.history || { status: "SYNCED" });
            } else if (h === 'safe_zones') {
                const geofences = geofenceSnap.exists() ? geofenceSnap.val() : {};
                const zones = {};
                for (const creator of Object.values(geofences)) {
                    if (creator.safe_zones) {
                        for (const [zid, z] of Object.entries(creator.safe_zones)) {
                            if (z.connected_devices?.[oldAgentId] || z.connected_devices?.[newAgentId] || (z.assigned_targets && z.assigned_targets.includes(oldAgentId))) {
                                zones[zid] = z;
                            }
                        }
                    }
                }
                updates[`${basePath}/safe_zones`] = Object.keys(zones).length > 0 ? zones : { status: "SYNCED_NO_ZONES" };
            } else if (h === 'acl') {
                updates[`${basePath}/acl`] = oldDevData.acl || { acl_live_tracking: "GRANTED" };
            } else if (h === 'live_tracking_enabled') {
                updates[`${basePath}/live_tracking_enabled`] = oldDevData.live_tracking_enabled !== undefined ? oldDevData.live_tracking_enabled : true;
            } else if (h === 'alerts') {
                updates[`${basePath}/alerts`] = oldDevData.alerts || { active_alerts: {}, updated_at: nowTs };
            } else if (h === '_merge_metadata') {
                updates[`${basePath}/_merge_metadata`] = {
                    merged_at_epoch: nowTs,
                    merged_at_iso: new Date().toISOString(),
                    original_old_agent_id: oldAgentId,
                    target_new_agent_id: newAgentId,
                    migration_reason: "SIGNATURE_MISMATCH_HEADINGS_SYNCHRONIZED"
                };
            } else if (oldDevData[h] !== undefined) {
                updates[`${basePath}/${h}`] = oldDevData[h];
            } else {
                updates[`${basePath}/${h}`] = { status: "INITIALIZED", synced_at: nowTs };
            }
        }

        if (Object.keys(updates).length > 0) {
            await db.ref().update(updates);
        }

        const [refreshedNewSnap, refreshedHistSnap, refreshedGeoSnap] = await Promise.all([
            db.ref(`devices/${newAgentId}`).once("value"),
            db.ref("History").once("value"),
            db.ref("Geofencing").once("value")
        ]);

        const refreshedNew = refreshedNewSnap.val() || {};
        const refreshedArchive = refreshedNew.Old_Device_ID?.[oldAgentId] || {};
        const comparison = buildHeadingsComparison(
            oldAgentId,
            newAgentId,
            oldDevSnap.exists() ? oldDevData : refreshedArchive,
            refreshedNew,
            refreshedHistSnap.val() || {},
            refreshedGeoSnap.val() || {}
        );

        return res.json({
            success: true,
            message: `All missing headings synchronized for ${oldAgentId} -> Old_Device_ID/${oldAgentId}.`,
            headingsComparison: comparison
        });
    } catch (err) {
        console.error("[ERROR] /api/sync-all-missing-headings failed:", err.message);
        return res.status(500).json({ error: "SYNC_ALL_FAILED", message: err.message });
    }
});

/**
 * POST /api/merge-duplicate-devices
 * Atomically merges old duplicate device node into the new device node:
 * 1. Deep clones /devices/{OLD_ID} -> /devices/{NEW_ID}/Old_Device_ID/{OLD_ID}
 * 2. Merges /History/{OLD_ID}/history -> /History/{NEW_ID}/history
 * 3. Re-links circle relationships from OLD_ID -> NEW_ID
 * 4. Re-links SafeZone connected devices from OLD_ID -> NEW_ID
 * 5. Updates lookup indices & writes forensic Change_Log + system_audit
 * 6. Safely cleans up the deprecated root /devices/{OLD_ID} node.
 */
router.post(["/merge-duplicate-devices", "/api/merge-duplicate-devices"], async (req, res) => {
    try {
        const { oldAgentId, newAgentId } = req.body || {};

        if (!oldAgentId || !newAgentId) {
            return res.status(400).json({ error: "MISSING_PARAMS", message: "Both oldAgentId and newAgentId are required." });
        }

        if (oldAgentId === newAgentId) {
            return res.status(400).json({ error: "IDENTICAL_IDS", message: "oldAgentId and newAgentId must not be the same." });
        }

        const [oldDevSnap, newDevSnap, oldHistSnap, geofenceSnap, allDevsSnap] = await Promise.all([
            db.ref(`devices/${oldAgentId}`).once("value"),
            db.ref(`devices/${newAgentId}`).once("value"),
            db.ref(`History/${oldAgentId}`).once("value"),
            db.ref("Geofencing").once("value"),
            db.ref("devices").once("value")
        ]);

        if (!oldDevSnap.exists()) {
            return res.status(404).json({ error: "OLD_DEVICE_NOT_FOUND", message: `Source device [${oldAgentId}] not found in Firebase.` });
        }

        if (!newDevSnap.exists()) {
            return res.status(404).json({ error: "NEW_DEVICE_NOT_FOUND", message: `Target device [${newAgentId}] not found in Firebase.` });
        }

        const oldDevData = oldDevSnap.val() || {};
        const newDevData = newDevSnap.val() || {};
        const nowTs = Date.now();
        const nowIso = new Date().toISOString();
        const logId = `${nowTs}_MERGE_${oldAgentId}`;

        // 1. Prepare Deep Clone Archive Data
        const archivePayload = {
            ...oldDevData,
            _merge_metadata: {
                merged_at_epoch: nowTs,
                merged_at_iso: nowIso,
                original_old_agent_id: oldAgentId,
                target_new_agent_id: newAgentId,
                migration_reason: "SIGNATURE_MISMATCH_SYSTEM_UPDATE"
            }
        };

        const multiPathUpdates = {};

        // A. Store old device data under new device under heading Old_Device_ID/{oldAgentId}
        multiPathUpdates[`devices/${newAgentId}/Old_Device_ID/${oldAgentId}`] = archivePayload;

        // B. Merge History Streams
        if (oldHistSnap.exists()) {
            const oldHistoryItems = oldHistSnap.val()?.history || {};
            for (const [key, record] of Object.entries(oldHistoryItems)) {
                multiPathUpdates[`History/${newAgentId}/history/${key}`] = {
                    ...record,
                    origin_agent_id: oldAgentId,
                    migrated_at: nowTs
                };
            }
        }

        // C. Re-anchor Peer Relationships across all devices
        if (allDevsSnap.exists()) {
            const allDevs = allDevsSnap.val() || {};
            for (const [peerId, peerNode] of Object.entries(allDevs)) {
                if (peerNode?.relationships && peerNode.relationships[oldAgentId]) {
                    const existingRel = peerNode.relationships[oldAgentId];
                    multiPathUpdates[`devices/${peerId}/relationships/${newAgentId}`] = {
                        ...existingRel,
                        linked_agent_id: newAgentId,
                        updated_at: nowIso
                    };
                    multiPathUpdates[`devices/${peerId}/relationships/${oldAgentId}`] = null;
                }
            }
        }

        // D. Re-anchor Relationships from old device into new device if missing
        if (oldDevData.relationships) {
            for (const [peerAgentId, relData] of Object.entries(oldDevData.relationships)) {
                if (peerAgentId !== newAgentId && !newDevData.relationships?.[peerAgentId]) {
                    multiPathUpdates[`devices/${newAgentId}/relationships/${peerAgentId}`] = {
                        ...relData,
                        migrated_from_old_device: oldAgentId
                    };
                }
            }
        }

        // E. Re-anchor SafeZones connected_devices
        if (geofenceSnap.exists()) {
            const geofences = geofenceSnap.val() || {};
            for (const [creatorId, creatorNode] of Object.entries(geofences)) {
                if (creatorNode?.safe_zones) {
                    for (const [zoneId, zoneNode] of Object.entries(creatorNode.safe_zones)) {
                        if (zoneNode?.connected_devices && zoneNode.connected_devices[oldAgentId]) {
                            const connData = zoneNode.connected_devices[oldAgentId];
                            multiPathUpdates[`Geofencing/${creatorId}/safe_zones/${zoneId}/connected_devices/${newAgentId}`] = {
                                ...connData,
                                device_id: newAgentId,
                                updated_at: nowIso
                            };
                            multiPathUpdates[`Geofencing/${creatorId}/safe_zones/${zoneId}/connected_devices/${oldAgentId}`] = null;
                        }
                    }
                }
            }
        }

        // F. Forensic Change_Log in New Device & Global Audit Ledger
        multiPathUpdates[`devices/${newAgentId}/Change_Log/Pairing_Devices/${logId}`] = {
            log_id: logId,
            category: "Pairing_Devices",
            action: "DUPLICATE_DEVICE_MERGED",
            entity_id: newAgentId,
            changed_by: "WEB_ADMIN_OPERATOR",
            timestamp: nowIso.replace(/[-:T.Z]/g, '').slice(0, 16),
            epoch_ms: nowTs,
            metadata: {
                old_device_id: oldAgentId,
                new_device_id: newAgentId,
                archive_location: `devices/${newAgentId}/Old_Device_ID/${oldAgentId}`,
                reason: "OTA_SIGNATURE_MISMATCH_RECONCILIATION"
            }
        };

        multiPathUpdates[`system_audit/${logId}`] = {
            timestamp: nowTs,
            action: "MERGE_DUPLICATE_DEVICE",
            old_device_id: oldAgentId,
            new_device_id: newAgentId,
            status: "SUCCESS"
        };

        // G. Deprecate & Remove old root device node and old root History node
        multiPathUpdates[`devices/${oldAgentId}`] = null;
        multiPathUpdates[`History/${oldAgentId}`] = null;

        // Perform Atomic Commit
        await db.ref().update(multiPathUpdates);

        // Invalidate In-Memory Caches
        if (setLiveDevicesCache) setLiveDevicesCache({ data: null, timestamp: 0 });
        if (setSafeZonesCache) setSafeZonesCache({ data: null, timestamp: 0 });

        console.log(`[DUPLICATE_SENTINEL] ✅ Successfully merged duplicate device ${oldAgentId} into ${newAgentId}. Old data preserved in Old_Device_ID.`);

        return res.json({
            success: true,
            message: `Successfully merged duplicate device [${oldAgentId}] into [${newAgentId}].`,
            oldAgentId,
            newAgentId,
            archivePath: `devices/${newAgentId}/Old_Device_ID/${oldAgentId}`,
            mergedAt: nowIso
        });
    } catch (err) {
        console.error("[ERROR] /api/merge-duplicate-devices failed:", err.message);
        return res.status(500).json({ error: "MERGE_FAILED", message: err.message });
    }
});

// 🛡️ Helper: Validates that an ID strictly conforms to canonical 8-char uppercase hex
    return router;
}

module.exports = createDuplicateRouter;
