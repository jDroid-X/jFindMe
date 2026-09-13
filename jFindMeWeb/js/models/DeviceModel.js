import { GeoUtils } from '../core/GeoUtils.js';
import { AvatarService } from '../services/AvatarService.js';

/**
 * jFindMe Matrix - Device Entity Model (OOPS Architecture)
 * Encapsulates device identity, location, hardware telemetry, and UI presentation states
 */
export class DeviceModel {
    constructor(data = {}) {
        this.raw = data.raw || data || {};
        const raw = this.raw;

        // ==========================================
        // TIER 1: MASTER DATA (Immutable Identity & Silicon Anchor)
        // ==========================================
        this.agentId = data.agentId || raw.agentId || raw.agent_id || "";
        this.deviceId = data.deviceId || raw.deviceId || raw.device_id || `DEV_${this.agentId}`;
        this.authUid = raw.Identity?.auth_uid || raw.auth_uid || raw.device?.live?.auth_uid || "";
        this.ownerUid = raw.Identity?.owner_uid || raw.owner_uid || raw.profile?.owner_uid || "";
        this.connectedId = raw.Identity?.connected_id || raw.connected_id || "";
        this.activeGroup = raw.Identity?.active_group || raw.active_group || raw.profile?.group || "Default Circle";

        // Hardware Silicon Specifications (Brand + Model + Board + Hardware + Device + IMEI 1/2)
        const hwProps = raw.Hardware || raw.Identity?.Hardware || raw.hardwareProperties || raw.device?.hardware || {};
        this.brand = hwProps.brand || raw.brand || "Android";
        this.model = hwProps.model || raw.model || data.model || data.deviceName || "Device";
        this.board = hwProps.board || raw.board || "universal";
        this.hardware = hwProps.hardware || raw.hardware || "qcom";
        this.device = hwProps.device || raw.device || "generic";
        this.imei1 = hwProps.imei1 || raw.imei1 || hwProps.imei || raw.imei || "Not Detected";
        this.imei2 = hwProps.imei2 || raw.imei2 || "Not Detected (Single SIM)";
        this.siliconHash = hwProps.siliconHash || raw.siliconHash || data.siliconHash || this._generateSiliconSignature();

        // ==========================================
        // TIER 2: SUB-MASTER DATA (Profile, Persona, Circles & ACL Access)
        // ==========================================
        const prof = raw.profile || raw.Profile || {};
        this.name = prof.name || data.name || raw.name || data.agentId || "Unknown Agent";
        this.deviceName = data.deviceName || prof.deviceName || this.model || "Android Device";
        this.hostName = prof.hostName || data.hostName || this.name;
        this.avatarId = prof.avatarId || data.avatarId || "avatar_custom_1";
        this.email = prof.email || data.email || "";
        this.phone = prof.phone || data.phone || "";
        this.dob = prof.dob || raw.dob || "";
        this.gender = prof.gender || raw.gender || "";
        this.role = prof.role || data.role || "MEMBER";
        this.roleFamily = prof.role_family || prof.roleFamily || data.role_family || "None";
        this.roleFriend = prof.role_friend || prof.roleFriend || data.role_friend || "None";
        this.roleTitle = prof.role_title || prof.roleTitle || data.roleTitle || this.role;
        this.department = prof.department || data.department || "General";
        this.emergencyNote = prof.emergencyNote || data.emergencyNote || "";
        this.safeZonesCount = parseInt(data.safeZonesCount || Object.keys(raw.safe_zones || raw.SafeZones || {}).length || 0);
        this.relationshipsCount = parseInt(data.relationshipsCount || Object.keys(raw.relationships || {}).length || 0);

        // Access Controls & Permissions (ACL)
        const acl = raw.acl || raw.Access || {};
        this.acl = {
            liveTracking: acl.acl_live_tracking !== 'DENIED',
            geofence: acl.acl_geofence !== 'DENIED',
            historyAudit: acl.acl_history_audit !== 'DENIED',
            ghostOverride: acl.acl_ghost_override === 'GRANTED',
            camera: acl.camera !== 'DENIED',
            gps: acl.gps !== 'DENIED',
            bluetooth: acl.bluetooth !== 'DENIED',
            foregroundService: acl.foreground_service !== 'DENIED'
        };

        // ==========================================
        // TIER 3: SUB-SUB-MASTER DATA (Live Telemetry & Diagnostics)
        // ==========================================
        const live = raw.device?.live || raw.live || {};
        this.latitude = parseFloat(live.latitude || data.latitude || raw.latitude || 0);
        this.longitude = parseFloat(live.longitude || data.longitude || raw.longitude || 0);
        this.hasValidLocation = Boolean(!isNaN(this.latitude) && !isNaN(this.longitude) && this.latitude !== 0 && this.longitude !== 0);
        this.speed = parseFloat(live.speed || data.speed || raw.speed || 0);
        this.bearing = parseFloat(live.bearing || data.bearing || raw.bearing || 0);
        this.accuracy = parseFloat(live.accuracy || data.accuracy || raw.accuracy || 0);
        this.altitude = parseFloat(live.altitude || data.altitude || raw.altitude || 0);
        this.address = live.address || data.address || raw.address || "";
        this.geohash = live.geohash || raw.geohash || "";

        const telem = raw.telemetry || raw.device?.telemetry || {};
        this.battery = parseInt(telem.battery != null ? telem.battery : (data.battery != null ? data.battery : -1));
        this.isCharging = Boolean(telem.isCharging != null ? telem.isCharging : data.isCharging);
        this.isMockGps = Boolean(telem.isMockGps != null ? telem.isMockGps : data.isMockGps);
        this.isAirplaneMode = Boolean(telem.isAirplaneMode != null ? telem.isAirplaneMode : data.isAirplaneMode);
        this.networkType = telem.networkType || data.networkType || "UNKNOWN";
        this.lastSeen = live.timestamp || telem.timestamp || data.lastSeen || data.lastUpdated || data.timestamp || 0;
        const epochInit = GeoUtils.parseTimestampToEpoch(this.lastSeen);
        this.timestamp = epochInit > 0 ? epochInit : (typeof data.timestamp === 'number' ? data.timestamp : 0);
        const elapsedInit = epochInit > 0 ? (Date.now() - epochInit) : Infinity;
        this.isOnline = Boolean(data.isOnline || (raw.st_session_state === "ONLINE") || (telem.session_state === "ONLINE") || (elapsedInit <= 15 * 60 * 1000));
        this.sessionState = telem.sessionState || data.sessionState || (this.isOnline ? "ONLINE" : "OFFLINE");
        this.oldDeviceIdArchive = raw.Old_Device_ID || {};

        // Dynamic Distance computation against reference station
        this.distanceFromRefKm = Infinity;
        this.isWithinSearchRadius = false;
    }

    /**
     * Generates a deterministic SHA-like silicon signature fallback from physical hardware fields
     */
    _generateSiliconSignature() {
        const rawKey = `${this.brand}|${this.model}|${this.board}|${this.hardware}|${this.device}`.toLowerCase();
        let hash = 0;
        for (let i = 0; i < rawKey.length; i++) {
            const char = rawKey.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash |= 0;
        }
        return 'SILICON_' + Math.abs(hash).toString(16).toUpperCase().padStart(8, '0');
    }

    /**
     * TIER 1: Master Data Representation
     * @returns {Object}
     */
    getMasterData() {
        return {
            tier: 1,
            tierName: "Master Data (Identity & Silicon Anchor)",
            agentId: this.agentId,
            deviceId: this.deviceId,
            authUid: this.authUid,
            ownerUid: this.ownerUid,
            connectedId: this.connectedId,
            activeGroup: this.activeGroup,
            hardware: {
                brand: this.brand,
                model: this.model,
                board: this.board,
                hardware: this.hardware,
                device: this.device,
                imei1: this.imei1,
                imei2: this.imei2,
                siliconHash: this.siliconHash
            }
        };
    }

    /**
     * TIER 2: Sub-Master Data Representation
     * @returns {Object}
     */
    getSubMasterData() {
        return {
            tier: 2,
            tierName: "Sub-Master Data (Profile, Persona & ACL)",
            profile: {
                name: this.name,
                deviceName: this.deviceName,
                hostName: this.hostName,
                avatarId: this.avatarId,
                avatarUrl: this.getAvatarUrl(),
                phone: this.phone,
                email: this.email,
                dob: this.dob,
                gender: this.gender,
                role: this.role,
                roleFamily: this.roleFamily,
                roleFriend: this.roleFriend,
                roleTitle: this.roleTitle,
                department: this.department,
                emergencyNote: this.emergencyNote
            },
            relationshipsCount: this.relationshipsCount,
            safeZonesCount: this.safeZonesCount,
            acl: this.acl,
            rawRelationships: this.raw.relationships || {},
            rawSafeZones: this.raw.safe_zones || this.raw.SafeZones || {}
        };
    }

    /**
     * TIER 3: Sub-Sub-Master Data Representation
     * @returns {Object}
     */
    getSubSubMasterData() {
        return {
            tier: 3,
            tierName: "Sub-Sub-Master Data (Telemetry & Diagnostics)",
            telemetry: {
                battery: this.battery,
                isCharging: this.isCharging,
                isMockGps: this.isMockGps,
                isAirplaneMode: this.isAirplaneMode,
                networkType: this.networkType,
                isOnline: this.isOnline,
                sessionState: this.sessionState,
                lastSeen: this.lastSeen,
                formattedLastSeen: this.getFormattedLastSeen(),
                formattedDateTime: this.getFormattedDateTime()
            },
            liveFix: {
                latitude: this.latitude,
                longitude: this.longitude,
                hasValidLocation: this.hasValidLocation,
                speed: this.speed,
                bearing: this.bearing,
                altitude: this.altitude,
                accuracy: this.accuracy,
                address: this.address,
                geohash: this.geohash,
                formattedCoords: this.getFormattedCoords(),
                formattedSpeed: this.getFormattedSpeed()
            },
            oldDeviceIdArchive: this.oldDeviceIdArchive
        };
    }

    /**
     * TIER 4: Transactional Data Representation
     * @returns {Object}
     */
    getTransactionalData() {
        const historyEntries = Object.entries(this.raw.history || {}).map(([k, v]) => ({ id: k, ...v }));
        const changeLogs = this.raw.Change_Log || this.raw.change_log || {};
        const alerts = this.raw.alerts || {};

        return {
            tier: 4,
            tierName: "Transactional Data (Time-Series & Forensic Logs)",
            historyPointsCount: historyEntries.length,
            history: historyEntries.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0)),
            changeLog: changeLogs,
            alerts: alerts,
            systemAudit: this.raw.system_audit || {}
        };
    }

    /**
     * Updates relative distance and proximity state based on operator's coordinates
     * @param {number} refLat 
     * @param {number} refLng 
     * @param {number} radiusKm 
     */
    updateProximity(refLat, refLng, radiusKm = 10) {
        if (this.hasValidLocation && refLat && refLng) {
            this.distanceFromRefKm = GeoUtils.calculateDistanceKm(refLat, refLng, this.latitude, this.longitude);
            this.isWithinSearchRadius = this.distanceFromRefKm <= radiusKm;
        } else {
            this.distanceFromRefKm = Infinity;
            this.isWithinSearchRadius = false;
        }
    }

    /**
     * Resolves Avatar URL
     * @returns {string}
     */
    getAvatarUrl() {
        return AvatarService.getAvatarUrl(this.avatarId, this.name);
    }

    /**
     * RAG Status Classifier:
     * - GREEN: Active (Online or last seen < 15m)
     * - ORANGE/AMBER: Inactive (Last seen between 15m and 6 hours ago)
     * - RED: Inactive (Last seen > 6 hours ago)
     * - GRAY: Uninstalled / Parked / Disconnected
     */
    getRagStatus() {
        if (this.sessionState === "UNINSTALLED" || this.sessionState === "PARKED" || this.sessionState === "DELETED") {
            return "UNINSTALLED";
        }
        const epochMs = GeoUtils.parseTimestampToEpoch(this.lastSeen);
        const now = Date.now();
        const elapsedMs = epochMs > 0 ? (now - epochMs) : Infinity;

        if (this.isOnline || elapsedMs <= 15 * 60 * 1000) {
            return "ACTIVE"; // Green
        }
        if (elapsedMs <= 6 * 60 * 60 * 1000) {
            return "INACTIVE_RECENT"; // Orange (< 6h)
        }
        return "INACTIVE_OLD"; // Red (> 6h)
    }

    getRagColor() {
        const status = this.getRagStatus();
        switch (status) {
            case "ACTIVE": return "#22C55E"; // Green
            case "INACTIVE_RECENT": return "#F59E0B"; // Orange
            case "INACTIVE_OLD": return "#EF4444"; // Red
            case "UNINSTALLED": return "#64748B"; // Gray
            default: return "#64748B";
        }
    }

    getRagBorderClass() {
        const status = this.getRagStatus();
        switch (status) {
            case "ACTIVE": return "border-emerald-500 shadow-emerald-500/20";
            case "INACTIVE_RECENT": return "border-amber-500 shadow-amber-500/20";
            case "INACTIVE_OLD": return "border-rose-500 shadow-rose-500/20";
            case "UNINSTALLED": return "border-slate-500 shadow-slate-500/20";
            default: return "border-slate-500";
        }
    }

    getRagBadgeClass() {
        const status = this.getRagStatus();
        switch (status) {
            case "ACTIVE": return "bg-emerald-500/20 text-emerald-400 border-emerald-500/30";
            case "INACTIVE_RECENT": return "bg-amber-500/20 text-amber-400 border-amber-500/30";
            case "INACTIVE_OLD": return "bg-rose-500/20 text-rose-400 border-rose-500/30";
            case "UNINSTALLED": return "bg-slate-800 text-slate-400 border-white/10";
            default: return "bg-slate-800 text-slate-400";
        }
    }

    getRagLabel() {
        const status = this.getRagStatus();
        switch (status) {
            case "ACTIVE": return "ACTIVE (LIVE)";
            case "INACTIVE_RECENT": return "INACTIVE (< 6H)";
            case "INACTIVE_OLD": return "INACTIVE (> 6H)";
            case "UNINSTALLED": return "UNINSTALLED";
            default: return "OFFLINE";
        }
    }

    /**
     * Resolves Theme Color for this device based on RAG status
     * @returns {string} Hex color
     */
    getThemeColor() {
        return this.getRagColor();
    }

    /**
     * Formatted string of distance
     * @returns {string}
     */
    getFormattedDistance() {
        return GeoUtils.formatDistance(this.distanceFromRefKm);
    }

    /**
     * Formatted relative time delta (e.g., "Live (Just now)", "2m ago")
     * @returns {string}
     */
    getFormattedLastSeen() {
        return GeoUtils.formatRelativeTime(this.lastSeen);
    }

    /**
     * Formatted relative time delta
     * @returns {string}
     */
    getFormattedTimeDelta() {
        return GeoUtils.formatRelativeTime(this.lastSeen);
    }

    /**
     * Formatted absolute Date & Time (e.g., "02 Sep 2026, 11:41:42 AM")
     * @returns {string}
     */
    getFormattedDateTime() {
        return GeoUtils.formatDateTime(this.lastSeen);
    }

    /**
     * Formatted speed
     * @returns {string}
     */
    getFormattedSpeed() {
        return GeoUtils.formatSpeed(this.speed);
    }

    /**
     * Formatted Coordinates
     * @returns {string}
     */
    getFormattedCoords() {
        return GeoUtils.formatCoords(this.latitude, this.longitude);
    }

    /**
     * Battery visual status class
     * @returns {{ text: string, color: string, icon: string }}
     */
    getBatteryMeta() {
        if (this.battery < 0) return { text: "N/A", color: "#64748B", icon: "🔋" };
        if (this.isCharging) return { text: `${this.battery}% ⚡`, color: "#10B981", icon: "⚡" };
        if (this.battery <= 15) return { text: `${this.battery}%`, color: "#EF4444", icon: "🪫" };
        if (this.battery <= 40) return { text: `${this.battery}%`, color: "#F59E0B", icon: "🔋" };
        return { text: `${this.battery}%`, color: "#10B981", icon: "🔋" };
    }

    /**
     * Returns standard epoch milliseconds from this device's timestamp or lastSeen
     * @returns {number}
     */
    getEpochTimestamp() {
        if (typeof this.timestamp === 'number' && this.timestamp > 0) return this.timestamp;
        const t = this.lastSeen || this.raw?.updated_at;
        const epoch = GeoUtils.parseTimestampToEpoch(t);
        if (epoch > 0) this.timestamp = epoch;
        return epoch || 0;
    }

    /**
     * Temporal Recency Tier:
     * - LIVE: < 1 minute (or isOnline)
     * - IDLE: 1m to 15m
     * - STALE: > 15m
     * @returns {{ tier: 'LIVE'|'IDLE'|'STALE', label: string, color: string, badgeClass: string, elapsedMs: number }}
     */
    getRecencyTier() {
        const epoch = this.getEpochTimestamp();
        const now = Date.now();
        const elapsedMs = epoch > 0 ? Math.max(0, now - epoch) : Infinity;

        if (this.isOnline || elapsedMs < 60 * 1000) {
            return {
                tier: 'LIVE',
                label: '🟢 LIVE (Now)',
                color: '#10b981',
                badgeClass: 'badge-status badge-success',
                elapsedMs
            };
        }
        if (elapsedMs <= 15 * 60 * 1000) {
            const mins = Math.max(1, Math.floor(elapsedMs / 60000));
            return {
                tier: 'IDLE',
                label: `🟡 IDLE (${mins}m ago)`,
                color: '#f59e0b',
                badgeClass: 'badge-status badge-warning',
                elapsedMs
            };
        }
        return {
            tier: 'STALE',
            label: '🔴 STALE (>15m)',
            color: '#ef4444',
            badgeClass: 'badge-status badge-danger',
            elapsedMs
        };
    }

    /**
     * Master Centralized Enterprise RAG Status Engine (Single Source of Truth across all screens):
     * - 🚨 EMERGENCY SOS: isSos == true (Pulsing Crimson #EF4444)
     * - 🟢 ACTIVE (LIVE): Telemetry < 15 minutes & Battery > 20% (Emerald Green #22C55E)
     * - 🟡 INACTIVE (< 6H): Telemetry 15m–6h OR Battery <= 20% (Amber Orange #F59E0B)
     * - 🔴 INACTIVE (> 6H): Telemetry > 6 hours (Rose Red #EF4444)
     * - ⚪ UNLINKED / GHOST: Uninstalled, ghost, parked, or deleted (Slate Gray #64748B)
     * @returns {{ status: string, label: string, isOnline: boolean, isMoving: boolean, color: string, badgeClass: string, dotClass: string, elapsedMs: number }}
     */
    getUnifiedStatus() {
        const epoch = this.getEpochTimestamp();
        const now = Date.now();
        const elapsedMs = epoch > 0 ? Math.max(0, now - epoch) : Infinity;
        const speed = parseFloat(this.speed || 0);

        // 🚨 1. EMERGENCY SOS (Pulsing Crimson)
        if (this.isSos || this.sessionState === "SOS") {
            return {
                status: 'EMERGENCY',
                label: 'EMERGENCY SOS',
                isOnline: true,
                isMoving: speed > 2,
                color: '#EF4444',
                badgeClass: 'badge-status badge-danger animate-pulse font-bold',
                dotClass: 'bg-rose-600 animate-ping',
                elapsedMs
            };
        }

        // ⚪ 2. UNLINKED / GHOST / UNINSTALLED (Slate Gray)
        if (this.sessionState === "UNINSTALLED" || this.sessionState === "PARKED" || this.sessionState === "DELETED" || this.sessionState === "GHOST") {
            return {
                status: 'UNLINKED',
                label: this.sessionState || 'UNLINKED',
                isOnline: false,
                isMoving: false,
                color: '#64748B',
                badgeClass: 'badge-status badge-secondary',
                dotClass: 'bg-slate-500',
                elapsedMs
            };
        }

        // 🟢 3. ACTIVE (LIVE) < 15 minutes & Battery > 20%
        if (elapsedMs <= 15 * 60 * 1000 && (this.battery < 0 || this.battery > 20)) {
            const isMoving = speed > 2;
            return {
                status: 'LIVE',
                label: isMoving ? 'LIVE (MOVING)' : 'LIVE (ACTIVE)',
                isOnline: true,
                isMoving,
                color: '#22C55E',
                badgeClass: 'badge-status badge-success',
                dotClass: 'bg-emerald-500 animate-pulse',
                elapsedMs
            };
        }

        // 🟡 4. INACTIVE (< 6H) or Low Battery <= 20%
        if (elapsedMs <= 6 * 60 * 60 * 1000 || (this.battery >= 0 && this.battery <= 20)) {
            let label = 'INACTIVE (<6H)';
            if (this.battery >= 0 && this.battery <= 20 && elapsedMs <= 15 * 60 * 1000) {
                label = `LOW BATT (${this.battery}%)`;
            } else if (elapsedMs <= 60 * 60 * 1000) {
                const mins = Math.max(1, Math.floor(elapsedMs / 60000));
                label = `IDLE (${mins}m)`;
            } else {
                const hrs = Math.floor(elapsedMs / 3600000);
                label = `INACTIVE (${hrs}h)`;
            }
            return {
                status: 'INACTIVE_RECENT',
                label,
                isOnline: elapsedMs <= 15 * 60 * 1000,
                isMoving: false,
                color: '#F59E0B',
                badgeClass: 'badge-status badge-warning',
                dotClass: 'bg-amber-400',
                elapsedMs
            };
        }

        // 🔴 5. INACTIVE (> 6H)
        return {
            status: 'INACTIVE_STALE',
            label: 'INACTIVE (>6H)',
            isOnline: false,
            isMoving: false,
            color: '#EF4444',
            badgeClass: 'badge-status badge-danger',
            dotClass: 'bg-rose-500',
            elapsedMs
        };
    }
}

