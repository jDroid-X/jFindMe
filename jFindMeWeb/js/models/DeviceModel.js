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
        this.lastSeen = data.lastSeen || 0;
        this.isOnline = Boolean(data.isOnline);
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
}
