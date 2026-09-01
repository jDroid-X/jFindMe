/**
 * jFindMe Matrix - SafeZone Entity Model (OOPS Architecture)
 * Encapsulates Geofencing Safe Zone properties, boundaries, active states, and connected devices.
 */
export class SafeZoneModel {
    constructor(data = {}) {
        this.zoneId = data.zoneId || `ZONE_${Date.now()}`;
        this.zoneName = data.zoneName || "Safe Perimeter";
        this.latitude = parseFloat(data.latitude || 0);
        this.longitude = parseFloat(data.longitude || 0);
        this.radius = parseFloat(data.radius || 100); // radius in meters
        this.isActive = Boolean(data.isActive !== false);
        this.alertsEnabled = Boolean(data.alertsEnabled);
        this.creatorId = data.creatorId || "";
        this.connectedDevices = Array.isArray(data.connectedDevices) ? data.connectedDevices : [];
    }

    /**
     * Checks if a given coordinate is within this safe zone
     * @param {number} lat 
     * @param {number} lng 
     * @returns {boolean}
     */
    contains(lat, lng) {
        if (!this.latitude || !this.longitude || !lat || !lng) return false;
        const R = 6371000; // Radius of Earth in meters
        const toRad = deg => (deg * Math.PI) / 180;
        const dLat = toRad(lat - this.latitude);
        const dLon = toRad(lng - this.longitude);
        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                  Math.cos(toRad(this.latitude)) * Math.cos(toRad(lat)) *
                  Math.sin(dLon / 2) * Math.sin(dLon / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        const distanceMeters = R * c;
        return distanceMeters <= this.radius;
    }

    /**
     * Resolves Theme Color for this Safe Zone
     * @returns {string} Hex color
     */
    getColor() {
        return this.isActive ? "#10B981" : "#64748B"; // Emerald Green or Slate Gray
    }
}
