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

        // Parse polygon boundary points
        this.polygonPoints = [];
        const rawPoly = data.polygonPoints || data.polygon_points || data.polygonPointsJson;
        if (typeof rawPoly === 'string' && rawPoly.trim().startsWith('[')) {
            try {
                this.polygonPoints = JSON.parse(rawPoly);
            } catch (e) {
                this.polygonPoints = [];
            }
        } else if (Array.isArray(rawPoly)) {
            this.polygonPoints = rawPoly;
        }
        this.dwellMinutes = parseInt(data.dwellMinutes || data.dwell_minutes || 0, 10);
    }

    /**
     * Checks if this zone is configured with a polygon boundary
     * @returns {boolean}
     */
    get isPolygon() {
        return Array.isArray(this.polygonPoints) && this.polygonPoints.length >= 3;
    }

    /**
     * Checks if a given coordinate is within this safe zone (Polygon Ray-Casting or Circle Haversine)
     * @param {number} lat 
     * @param {number} lng 
     * @returns {boolean}
     */
    contains(lat, lng) {
        if (!lat || !lng) return false;

        // 1. Polygon Ray-Casting containment
        if (this.isPolygon) {
            let inside = false;
            const pts = this.polygonPoints;
            const n = pts.length;
            let j = n - 1;
            for (let i = 0; i < n; i++) {
                const piLat = parseFloat(pts[i].lat ?? pts[i].latitude ?? 0);
                const piLng = parseFloat(pts[i].lng ?? pts[i].longitude ?? 0);
                const pjLat = parseFloat(pts[j].lat ?? pts[j].latitude ?? 0);
                const pjLng = parseFloat(pts[j].lng ?? pts[j].longitude ?? 0);

                if ((piLng > lng) !== (pjLng > lng) &&
                    (lat < (pjLat - piLat) * (lng - piLng) / (pjLng - piLng) + piLat)) {
                    inside = !inside;
                }
                j = i;
            }
            return inside;
        }

        // 2. Circular Haversine containment
        if (!this.latitude || !this.longitude) return false;
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
