/**
 * jFindMe Matrix - Geo & Math Utilities (Enterprise SSOT)
 * Responsibility: Spatial computations, Haversine formula, bearings, and string formatters
 */
export class GeoUtils {
    /**
     * Earth radius in kilometers
     */
    static EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculates great-circle distance between two points using the Haversine formula
     * @param {number} lat1 
     * @param {number} lon1 
     * @param {number} lat2 
     * @param {number} lon2 
     * @returns {number} Distance in kilometers
     */
    static calculateDistanceKm(lat1, lon1, lat2, lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return Infinity;
        if (isNaN(lat1) || isNaN(lon1) || isNaN(lat2) || isNaN(lon2)) return Infinity;
        if (lat1 === 0 && lon1 === 0) return Infinity;
        if (lat2 === 0 && lon2 === 0) return Infinity;
        if (lat1 === lat2 && lon1 === lon2) return 0;

        const toRad = deg => (deg * Math.PI) / 180;
        const dLat = toRad(lat2 - lat1);
        const dLon = toRad(lon2 - lon1);

        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                  Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                  Math.sin(dLon / 2) * Math.sin(dLon / 2);

        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return this.EARTH_RADIUS_KM * c;
    }

    /**
     * Checks if a point is within a radius of the center point
     * @param {number} centerLat 
     * @param {number} centerLng 
     * @param {number} pointLat 
     * @param {number} pointLng 
     * @param {number} radiusKm 
     * @returns {boolean}
     */
    static isWithinRadius(centerLat, centerLng, pointLat, pointLng, radiusKm = 10) {
        if (!centerLat || !centerLng || !pointLat || !pointLng) return false;
        const dist = this.calculateDistanceKm(centerLat, centerLng, pointLat, pointLng);
        return dist <= radiusKm;
    }

    /**
     * Calculates the forward azimuth / compass bearing from point 1 to point 2 (0-360 deg)
     * @param {number} lat1 
     * @param {number} lon1 
     * @param {number} lat2 
     * @param {number} lon2 
     * @returns {number} Bearing in degrees
     */
    static calculateBearing(lat1, lon1, lat2, lon2) {
        const toRad = deg => (deg * Math.PI) / 180;
        const toDeg = rad => (rad * 180) / Math.PI;

        const y = Math.sin(toRad(lon2 - lon1)) * Math.cos(toRad(lat2));
        const x = Math.cos(toRad(lat1)) * Math.sin(toRad(lat2)) -
                  Math.sin(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.cos(toRad(lon2 - lon1));

        const brng = toDeg(Math.atan2(y, x));
        return (brng + 360) % 360;
    }

    /**
     * Converts bearing degrees into compass cardinal direction (e.g. N, NE, E, SE, S, SW, W, NW)
     * @param {number} bearing 
     * @returns {string}
     */
    static bearingToCardinal(bearing) {
        const cardinals = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW'];
        const index = Math.round(((bearing % 360) / 22.5)) % 16;
        return cardinals[index];
    }

    /**
     * Formats distance cleanly (e.g., "450 m" or "3.4 km")
     * @param {number} distKm 
     * @returns {string}
     */
    static formatDistance(distKm) {
        if (distKm == null || distKm === Infinity || isNaN(distKm)) return "N/A";
        if (distKm < 1.0) {
            return `${Math.round(distKm * 1000)} m`;
        }
        return `${distKm.toFixed(1)} km`;
    }

    /**
     * Formats speed from m/s or km/h
     * @param {number} speedMps 
     * @returns {string}
     */
    static formatSpeed(speedMps) {
        if (!speedMps || speedMps < 0.2) return "Stationary";
        const kmh = speedMps * 3.6;
        return `${kmh.toFixed(1)} km/h`;
    }

    /**
     * Parses diverse timestamp representations (14/16-char forensic string YYYYMMDDHHmmssSS, ISO string, or numeric epoch ms)
     * @param {string|number} raw 
     * @returns {number} Epoch milliseconds
     */
    static parseTimestampToEpoch(raw) {
        if (!raw) return 0;
        if (typeof raw === 'number') {
            if (raw > 1e14) return Math.floor(raw / 1000);
            if (raw < 1e11 && raw > 1e9) return raw * 1000;
            return raw;
        }
        const str = String(raw).trim();
        if (/^\d{14,16}$/.test(str)) {
            // Forensic Timestamp format: YYYYMMDDHHmmss or YYYYMMDDHHmmssSS
            const y = parseInt(str.substring(0, 4), 10);
            const m = parseInt(str.substring(4, 6), 10) - 1;
            const d = parseInt(str.substring(6, 8), 10);
            const hh = parseInt(str.substring(8, 10), 10);
            const mm = parseInt(str.substring(10, 12), 10);
            const ss = parseInt(str.substring(12, 14), 10);
            const rem = str.substring(14);
            const ms = rem ? parseInt(rem.padEnd(3, '0').slice(0, 3), 10) : 0;
            const date = new Date(y, m, d, hh, mm, ss, ms);
            return date.getTime();
        }
        const parsed = Date.parse(str);
        if (!isNaN(parsed)) return parsed;
        const num = Number(str);
        if (!isNaN(num) && num > 0) return this.parseTimestampToEpoch(num);
        return 0;
    }

    /**
     * Formats timestamp to readable Date & Time (e.g. "02 Sep 2026, 11:41:42 AM")
     * @param {string|number} raw 
     * @returns {string}
     */
    static formatDateTime(raw) {
        const epoch = this.parseTimestampToEpoch(raw);
        if (!epoch) return "No timestamp recorded";
        const date = new Date(epoch);
        const day = String(date.getDate()).padStart(2, '0');
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        const month = months[date.getMonth()];
        const year = date.getFullYear();
        let hours = date.getHours();
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        const ampm = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12 || 12;
        const formattedHours = String(hours).padStart(2, '0');
        return `${day} ${month} ${year}, ${formattedHours}:${minutes}:${seconds} ${ampm}`;
    }

    /**
     * Formats relative time elapsed ("Just now", "2m ago", "1h ago", "Yesterday")
     * @param {number|string} timestamp 
     * @returns {string}
     */
    static formatRelativeTime(timestamp) {
        if (!timestamp) return "Never";
        const ts = this.parseTimestampToEpoch(timestamp);
        if (!ts) return "Never";

        const diffMs = Date.now() - ts;
        if (diffMs < 0) return "Live (Just now)";
        const diffSec = Math.floor(diffMs / 1000);
        if (diffSec < 30) return "Live (Just now)";
        if (diffSec < 60) return `${diffSec}s ago`;
        const diffMin = Math.floor(diffSec / 60);
        if (diffMin < 60) return `${diffMin}m ago`;
        const diffHours = Math.floor(diffMin / 60);
        if (diffHours < 24) return `${diffHours}h ago`;
        const diffDays = Math.floor(diffHours / 24);
        return `${diffDays}d ago`;
    }

    /**
     * Enterprise Anti-Overlap Multi-Marker Engine (Matching MultiMarkerEngine.kt)
     * Implements precise Geometric & Spiderweb Formations:
     * - 1 Device:  Exact GPS Point (•)
     * - 2 Devices: Symmetrical Line (—)
     * - 3 Devices: Equilateral Triangle with Top Apex (Δ)
     * - 4 Devices: Diamond / Square (◇)
     * - 5 Devices: Regular Pentagon (⬟)
     * - 6 Devices: Regular Hexagon (⬡)
     * - 7-8 Devices: Regular Equidistant N-gon Circle (◯)
     * - >8 Devices: Multi-ring Archimedean Spiderweb Spiral
     * 
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices
     * @param {number|Object} optionsOrThreshold Collision threshold or config object { map, zoom, spacingPx, collisionThresholdPx }
     * @returns {Array<{ device: import('../models/DeviceModel.js').DeviceModel, displayLat: number, displayLng: number, isDisplaced: boolean, clusterSize: number, indexInCluster: number, centroidLat: number, centroidLng: number }>}
     */
    static applyAntiOverlapDisplacement(devices = [], optionsOrThreshold = {}) {
        const validDevices = devices.filter(d => d && d.hasValidLocation);
        const nonLocated = devices.filter(d => !d || !d.hasValidLocation);

        if (validDevices.length === 0) {
            return nonLocated.map(d => ({
                device: d,
                displayLat: d ? d.latitude : 0,
                displayLng: d ? d.longitude : 0,
                isDisplaced: false,
                clusterSize: 1,
                indexInCluster: 0,
                centroidLat: 0,
                centroidLng: 0
            }));
        }

        const options = (typeof optionsOrThreshold === 'number') 
            ? { collisionThresholdMeters: optionsOrThreshold } 
            : (optionsOrThreshold || {});

        const map = options.map || null;
        const currentZoom = options.zoom || (map && typeof map.getZoom === 'function' ? map.getZoom() : 14);
        const targetSpacingPx = options.spacingPx || 65.0; // 65px center-to-center
        const collisionThresholdPx = options.collisionThresholdPx || 60.0;

        // Leaflet screen layerPoint projection support
        const hasLeafletProjection = map && typeof map.latLngToLayerPoint === 'function' && typeof map.layerPointToLatLng === 'function';

        // 1. Convert to screen points or calculate zoom-adaptive collision distance
        const candidateMap = new Map();
        for (const dev of validDevices) {
            let pt = null;
            if (hasLeafletProjection) {
                try {
                    pt = map.latLngToLayerPoint([dev.latitude, dev.longitude]);
                } catch (e) {
                    pt = null;
                }
            }
            candidateMap.set(dev.agentId, {
                device: dev,
                point: pt,
                lat: dev.latitude,
                lng: dev.longitude
            });
        }

        // 2. Transitive Connected Component Clustering (BFS matching Android MultiMarkerEngine.kt)
        const clusters = [];
        const visited = new Set();

        for (let i = 0; i < validDevices.length; i++) {
            const root = validDevices[i];
            if (visited.has(root.agentId)) continue;

            const currentCluster = [];
            const queue = [root];
            visited.add(root.agentId);

            while (queue.length > 0) {
                const curr = queue.shift();
                currentCluster.push(curr);
                const currData = candidateMap.get(curr.agentId);

                for (const neighbor of validDevices) {
                    if (visited.has(neighbor.agentId)) continue;
                    const neighData = candidateMap.get(neighbor.agentId);

                    let isColliding = false;
                    // Check exact co-location first
                    if (Math.abs(curr.latitude - neighbor.latitude) < 0.00002 && Math.abs(curr.longitude - neighbor.longitude) < 0.00002) {
                        isColliding = true;
                    } else if (hasLeafletProjection && currData.point && neighData.point) {
                        const dx = currData.point.x - neighData.point.x;
                        const dy = currData.point.y - neighData.point.y;
                        const distPx = Math.sqrt(dx * dx + dy * dy);
                        isColliding = distPx <= collisionThresholdPx;
                    } else {
                        // Zoom-adaptive geographic threshold
                        const metersPerPixel = 156543.03392 * Math.cos(curr.latitude * Math.PI / 180.0) / Math.pow(2, currentZoom);
                        const effectiveThresholdMeters = Math.max(30.0, collisionThresholdPx * metersPerPixel);
                        const distMeters = this.calculateDistanceKm(
                            curr.latitude, curr.longitude,
                            neighbor.latitude, neighbor.longitude
                        ) * 1000.0;
                        isColliding = distMeters <= effectiveThresholdMeters;
                    }

                    if (isColliding) {
                        visited.add(neighbor.agentId);
                        queue.push(neighbor);
                    }
                }
            }
            clusters.push(currentCluster);
        }

        // 3. Apply Spiderweb / Screen-Space Equidistant Geometric Progression across each cluster
        const results = [];

        for (const cluster of clusters) {
            const count = cluster.length;

            if (count === 1) {
                const dev = cluster[0];
                results.push({
                    device: dev,
                    displayLat: dev.latitude,
                    displayLng: dev.longitude,
                    isDisplaced: false,
                    clusterSize: 1,
                    indexInCluster: 0,
                    centroidLat: dev.latitude,
                    centroidLng: dev.longitude
                });
                continue;
            }

            // True centroid calculation
            const centroidLat = cluster.reduce((sum, d) => sum + d.latitude, 0) / count;
            const centroidLng = cluster.reduce((sum, d) => sum + d.longitude, 0) / count;

            if (hasLeafletProjection) {
                // SCREEN-SPACE SPIDERWEB PROJECTION (Exact Leaflet Layer Points)
                const clusterPoints = cluster.map(d => candidateMap.get(d.agentId).point || map.latLngToLayerPoint([d.latitude, d.longitude]));
                const centroidX = clusterPoints.reduce((sum, p) => sum + p.x, 0) / count;
                const centroidY = clusterPoints.reduce((sum, p) => sum + p.y, 0) / count;

                // Dispersion radius formula matching Android MultiMarkerEngine.kt
                let dispersionRadiusPx = 48.0;
                if (count === 2) {
                    dispersionRadiusPx = 38.0;
                } else if (count === 3) {
                    dispersionRadiusPx = 46.0;
                } else if (count === 4) {
                    dispersionRadiusPx = 54.0;
                } else {
                    const sinVal = Math.sin(Math.PI / Math.min(count, 8));
                    dispersionRadiusPx = Math.max(48.0, (targetSpacingPx / 2.0) / (sinVal > 0.01 ? sinVal : 1.0));
                }

                const baseAngle = (count === 2) ? 0.0 : -Math.PI / 2.0;

                for (let idx = 0; idx < count; idx++) {
                    const dev = cluster[idx];
                    let angle, radius;

                    if (count > 8) {
                        // Multi-ring Archimedean spiderweb spiral for dense clusters
                        const ring = Math.floor(idx / 8);
                        const ringIdx = idx % 8;
                        const ringCapacity = Math.min(8, count - ring * 8);
                        angle = -Math.PI / 2.0 + (2.0 * Math.PI * ringIdx / ringCapacity);
                        radius = dispersionRadiusPx * (1.0 + ring * 0.65);
                    } else {
                        angle = baseAngle + (2.0 * Math.PI * idx / count);
                        radius = dispersionRadiusPx;
                    }

                    const offsetX = centroidX + radius * Math.cos(angle);
                    const offsetY = centroidY + radius * Math.sin(angle);
                    const displaced = map.layerPointToLatLng([offsetX, offsetY]);

                    results.push({
                        device: dev,
                        displayLat: displaced.lat,
                        displayLng: displaced.lng,
                        isDisplaced: true,
                        clusterSize: count,
                        indexInCluster: idx,
                        centroidLat,
                        centroidLng
                    });
                }

            } else {
                // ZOOM-ADAPTIVE GEOGRAPHIC SPIDERWEB PROJECTION
                const metersPerPixel = 156543.03392 * Math.cos(centroidLat * Math.PI / 180.0) / Math.pow(2, currentZoom);
                const targetSpacingMeters = Math.max(25.0, targetSpacingPx * metersPerPixel);
                const sinVal = Math.sin(Math.PI / Math.min(count, 8));
                const dispersionRadiusMeters = (targetSpacingMeters / 2.0) / (sinVal > 0.01 ? sinVal : 1.0);

                const baseAngle = (count === 2) ? 0.0 : -Math.PI / 2.0;

                for (let idx = 0; idx < count; idx++) {
                    const dev = cluster[idx];
                    let angle, radiusMeters;

                    if (count > 8) {
                        const ring = Math.floor(idx / 8);
                        const ringIdx = idx % 8;
                        const ringCapacity = Math.min(8, count - ring * 8);
                        angle = -Math.PI / 2.0 + (2.0 * Math.PI * ringIdx / ringCapacity);
                        radiusMeters = dispersionRadiusMeters * (1.0 + ring * 0.65);
                    } else {
                        angle = baseAngle + (2.0 * Math.PI * idx / count);
                        radiusMeters = dispersionRadiusMeters;
                    }

                    const deltaLat = (radiusMeters / 111111.0) * Math.sin(angle);
                    const deltaLng = (radiusMeters / (111111.0 * Math.cos(centroidLat * Math.PI / 180.0))) * Math.cos(angle);

                    results.push({
                        device: dev,
                        displayLat: centroidLat + deltaLat,
                        displayLng: centroidLng + deltaLng,
                        isDisplaced: true,
                        clusterSize: count,
                        indexInCluster: idx,
                        centroidLat,
                        centroidLng
                    });
                }
            }
        }

        // Add non-located devices
        for (const dev of nonLocated) {
            results.push({
                device: dev,
                displayLat: dev ? dev.latitude : 0,
                displayLng: dev ? dev.longitude : 0,
                isDisplaced: false,
                clusterSize: 1,
                indexInCluster: 0,
                centroidLat: 0,
                centroidLng: 0
            });
        }

        return results;
    }

    /**
     * Formats latitude and longitude for display
     * @param {number} lat 
     * @param {number} lng 
     * @returns {string}
     */
    static formatCoords(lat, lng) {
        if (!lat && !lng) return "Unknown";
        return `${lat.toFixed(5)}°, ${lng.toFixed(5)}°`;
    }

    /**
     * Asynchronously reverse-geocodes coordinates to street address
     * @param {number} lat 
     * @param {number} lng 
     * @returns {Promise<string>}
     */
    static async reverseGeocode(lat, lng) {
        if (!lat || !lng || isNaN(lat) || isNaN(lng)) return "Unknown Location";
        try {
            const host = window.location.hostname || '127.0.0.1';
            const base = window.API_BASE_URL || (window.location.port === '3000' ? window.location.origin : `http://${host}:3000`);
            const res = await fetch(`${base}/api/reverse-geocode?lat=${lat}&lng=${lng}`);
            if (res.ok) {
                const data = await res.json();
                return data.address || `${lat.toFixed(5)}°, ${lng.toFixed(5)}°`;
            }
            return `${lat.toFixed(5)}°, ${lng.toFixed(5)}°`;
        } catch (e) {
            return `${lat.toFixed(5)}°, ${lng.toFixed(5)}°`;
        }
    }
}
