import { GeoUtils } from '../core/GeoUtils.js';
import { AvatarService } from '../services/AvatarService.js';

/**
 * jFindMe Matrix - Device Entity Model (OOPS Architecture)
 * Encapsulates device identity, location, hardware telemetry, and UI presentation states
 */
export class DeviceModel {
    constructor(data = {}) {
        this.agentId = data.agentId || "";
        this.deviceId = data.deviceId || `DEV_${this.agentId}`;
        this.name = data.name || data.agentId || "Unknown Agent";
        this.deviceName = data.deviceName || data.model || "Android Device";
        this.model = data.model || data.deviceName || "";
        this.hostName = data.hostName || this.name;
        this.avatarId = data.avatarId || "avatar_custom_1";
        
        // Geospatial state
        this.latitude = parseFloat(data.latitude || 0);
        this.longitude = parseFloat(data.longitude || 0);
        this.hasValidLocation = Boolean(data.hasValidLocation && !isNaN(this.latitude) && !isNaN(this.longitude) && this.latitude !== 0 && this.longitude !== 0);
        this.speed = parseFloat(data.speed || 0);
        this.bearing = parseFloat(data.bearing || 0);
        this.accuracy = parseFloat(data.accuracy || 0);
        this.altitude = parseFloat(data.altitude || 0);
        this.address = data.address || "";

        // Telemetry & Hardware
        this.battery = parseInt(data.battery != null ? data.battery : -1);
        this.isCharging = Boolean(data.isCharging);
        this.isMockGps = Boolean(data.isMockGps);
        this.isAirplaneMode = Boolean(data.isAirplaneMode);
        this.networkType = data.networkType || "UNKNOWN";
        this.lastSeen = data.lastSeen || data.lastUpdated || data.timestamp || 0;
        const epochInit = GeoUtils.parseTimestampToEpoch(this.lastSeen);
        this.timestamp = epochInit > 0 ? epochInit : (typeof data.timestamp === 'number' ? data.timestamp : 0);
        const elapsedInit = epochInit > 0 ? (Date.now() - epochInit) : Infinity;
        this.isOnline = Boolean(data.isOnline || (data.st_session_state === "ONLINE") || (elapsedInit <= 15 * 60 * 1000));
        this.sessionState = data.sessionState || (this.isOnline ? "ONLINE" : "OFFLINE");

        // Profile & Organization
        this.email = data.email || "";
        this.phone = data.phone || "";
        this.role = data.role || "MEMBER";
        this.roleFamily = data.role_family || "None";
        this.roleFriend = data.role_friend || "None";
        this.department = data.department || "";
        this.emergencyNote = data.emergencyNote || "";
        this.safeZonesCount = parseInt(data.safeZonesCount || 0);
        this.relationshipsCount = parseInt(data.relationshipsCount || 0);
        
        // Dynamic Distance computation (calculated against current reference location)
        this.distanceFromRefKm = Infinity;
        this.isWithinSearchRadius = false;
        
        this.raw = data.raw || {};
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

