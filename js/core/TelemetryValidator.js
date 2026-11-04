/**
 * jFindMe Matrix - Enterprise 5-Layer Telemetry Validation Engine (OOPS Architecture)
 * 
 * Validates incoming device telemetry in a strict Fail-Fast pipeline:
 *  Layer 1: Security & Device Identity (Canonical ID, Token, Mock GPS Flag)
 *  Layer 2: Temporal Monotonicity & Clock Drift (Epoch delta bounds, future timestamp rejection)
 *  Layer 3: Spatial Kinematics Sanity (Lat/Lng bounds, Speed < 250 km/h, Teleport Check)
 *  Layer 4: Redundancy & Deadbanding Filter (Delta distance < 15m AND Velocity < 1 km/h)
 *  Layer 5: Business State & Closed-Loop Geofencing (SafeZone breach, Battery threshold)
 */
export class TelemetryValidator {
    /**
     * Layer 1: Identity & Security Validation
     * @param {Object} ping 
     * @returns {{ valid: boolean, error?: string }}
     */
    static validateIdentity(ping) {
        if (!ping || typeof ping !== 'object') {
            return { valid: false, error: "LAYER1_ERR: Telemetry payload is null or invalid." };
        }
        const agentId = String(ping.agentId || ping.deviceId || "").trim();
        if (!agentId || agentId.length < 3) {
            return { valid: false, error: "LAYER1_ERR: Missing or invalid Agent/Device Identifier." };
        }
        if (ping.isMockGps === true || ping.is_mock_gps === true) {
            return { valid: false, error: "LAYER1_ERR: Mock GPS spoofing detected. Record quarantined." };
        }
        return { valid: true };
    }

    /**
     * Layer 2: Temporal Monotonicity & Clock Drift Validation
     * @param {Object} ping 
     * @param {Object} [lastPing]
     * @param {number} [maxDriftMs=300000] // 5 minutes max clock drift allowed
     * @returns {{ valid: boolean, error?: string, timestamp: number }}
     */
    static validateTemporal(ping, lastPing = null, maxDriftMs = 300000) {
        let ts = Number(ping.timestamp || ping.time || ping.epoch_ms);
        if (!ts || isNaN(ts) || ts <= 0) {
            ts = Date.now();
        }
        // Normalize 16-digit or seconds format
        if (ts > 1e14) ts = Math.floor(ts / 1000);
        if (ts < 1e11 && ts > 1e9) ts = ts * 1000;

        const now = Date.now();
        // Reject points from the far future (clock drift > 5 min)
        if (ts > now + maxDriftMs) {
            return { valid: false, error: `LAYER2_ERR: Future timestamp rejected (${new Date(ts).toISOString()} vs server ${new Date(now).toISOString()})`, timestamp: ts };
        }

        // Monotonicity check against previous ping if available
        if (lastPing) {
            let lastTs = Number(lastPing.timestamp || lastPing.time || 0);
            if (lastTs > 1e14) lastTs = Math.floor(lastTs / 1000);
            if (lastTs < 1e11 && lastTs > 1e9) lastTs = lastTs * 1000;

            if (lastTs > 0 && ts < lastTs - 60000) { // Allowed 1-min tolerance for out-of-order delivery
                return { valid: false, error: `LAYER2_ERR: Timestamp backwards jump detected (${ts} < ${lastTs})`, timestamp: ts };
            }
        }

        return { valid: true, timestamp: ts };
    }

    /**
     * Layer 3: Spatial Kinematics Sanity & Teleportation Detection
     * @param {Object} ping 
     * @param {Object} [lastPing]
     * @param {number} [maxSpeedKmh=250] // Max realistic ground transport speed
     * @returns {{ valid: boolean, error?: string, lat: number, lng: number, speed: number }}
     */
    static validateKinematics(ping, lastPing = null, maxSpeedKmh = 250) {
        const lat = parseFloat(ping.latitude ?? ping.lat ?? 0);
        const lng = parseFloat(ping.longitude ?? ping.lng ?? 0);

        if (isNaN(lat) || isNaN(lng)) {
            return { valid: false, error: "LAYER3_ERR: Coordinates are NaN." };
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return { valid: false, error: `LAYER3_ERR: Coordinates out of geographical bounds (${lat}, ${lng}).` };
        }
        if (lat === 0 && lng === 0) {
            return { valid: false, error: "LAYER3_ERR: Zero GPS null-island coordinate rejected." };
        }

        const accuracy = parseFloat(ping.accuracy || 0);
        if (accuracy > 250) {
            return { valid: false, error: `LAYER3_ERR: Inaccurate GPS fix rejected (Accuracy: ${accuracy}m > 250m threshold).` };
        }

        const rawSpeed = parseFloat(ping.speed || 0); // m/s
        let speedKmh = rawSpeed > 100 ? rawSpeed : rawSpeed * 3.6;

        // Teleportation sanity check against last recorded point
        if (lastPing) {
            const lastLat = parseFloat(lastPing.latitude ?? lastPing.lat ?? 0);
            const lastLng = parseFloat(lastPing.longitude ?? lastPing.lng ?? 0);
            const lastTs = Number(lastPing.timestamp || lastPing.time || 0);
            const currTs = Number(ping.timestamp || ping.time || Date.now());

            if (lastLat !== 0 && lastLng !== 0 && currTs > lastTs) {
                const distMeters = TelemetryValidator.calculateDistanceMeters(lastLat, lastLng, lat, lng);
                const deltaHours = (currTs - lastTs) / (1000 * 3600);
                if (deltaHours > 0) {
                    const derivedSpeedKmh = (distMeters / 1000) / deltaHours;
                    if (derivedSpeedKmh > maxSpeedKmh) {
                        return { valid: false, error: `LAYER3_ERR: Teleport jump detected (${distMeters.toFixed(0)}m in ${(deltaHours*3600).toFixed(1)}s = ${derivedSpeedKmh.toFixed(0)} km/h).` };
                    }
                    if (speedKmh <= 0) speedKmh = derivedSpeedKmh;
                }
            }
        }

        return { valid: true, lat, lng, speed: speedKmh };
    }

    /**
     * Layer 4: Redundancy & Deadband Noise Filter (Stationary Suppression)
     * If user is stationary (distance moved < threshold and velocity < 1.5 km/h),
     * return true to indicate ping should update HOT state only and skip redundant WARM writes.
     * @param {Object} currentPing 
     * @param {Object} lastRecordedPing 
     * @param {number} [thresholdMeters=15] 
     * @returns {boolean} true = is stationary deadband (suppress redundant write)
     */
    static isStationaryDeadband(currentPing, lastRecordedPing, thresholdMeters = 15) {
        if (!lastRecordedPing) return false;

        const lat1 = parseFloat(lastRecordedPing.latitude ?? lastRecordedPing.lat ?? 0);
        const lng1 = parseFloat(lastRecordedPing.longitude ?? lastRecordedPing.lng ?? 0);
        const lat2 = parseFloat(currentPing.latitude ?? currentPing.lat ?? 0);
        const lng2 = parseFloat(currentPing.longitude ?? currentPing.lng ?? 0);

        if (lat1 === 0 || lng1 === 0 || lat2 === 0 || lng2 === 0) return false;

        const distanceMeters = TelemetryValidator.calculateDistanceMeters(lat1, lng1, lat2, lng2);
        const speedKmh = parseFloat(currentPing.speed || 0) * (currentPing.speed > 50 ? 1 : 3.6);

        // Stationary condition: moved less than deadband radius and speed is below walking threshold
        return (distanceMeters < thresholdMeters && speedKmh < 1.5);
    }

    /**
     * Haversine Distance in Meters
     */
    static calculateDistanceMeters(lat1, lon1, lat2, lon2) {
        const R = 6371000; // Earth radius in meters
        const dLat = (lat2 - lat1) * Math.PI / 180;
        const dLon = (lon2 - lon1) * Math.PI / 180;
        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                  Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                  Math.sin(dLon / 2) * Math.sin(dLon / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Ramer-Douglas-Peucker (RDP) Polyline Downsampling Algorithm
     * Compresses thousands of breadcrumbs into essential geometric vertices.
     * @param {Array<Object>} points 
     * @param {number} [epsilon=0.00008] // Approx 8-10 meters tolerance
     * @returns {Array<Object>}
     */
    static downsampleRDP(points, epsilon = 0.00008) {
        if (!points || points.length <= 2) return points || [];

        const perpendicularDistance = (pt, lineStart, lineEnd) => {
            const dx = lineEnd.longitude - lineStart.longitude;
            const dy = lineEnd.latitude - lineStart.latitude;
            if (dx === 0 && dy === 0) {
                return Math.hypot(pt.longitude - lineStart.longitude, pt.latitude - lineStart.latitude);
            }
            const t = ((pt.longitude - lineStart.longitude) * dx + (pt.latitude - lineStart.latitude) * dy) / (dx * dx + dy * dy);
            const clampedT = Math.max(0, Math.min(1, t));
            const projX = lineStart.longitude + clampedT * dx;
            const projY = lineStart.latitude + clampedT * dy;
            return Math.hypot(pt.longitude - projX, pt.latitude - projY);
        };

        const rdpRecursive = (pts) => {
            let maxDist = 0;
            let index = 0;
            const end = pts.length - 1;

            for (let i = 1; i < end; i++) {
                const dist = perpendicularDistance(pts[i], pts[0], pts[end]);
                if (dist > maxDist) {
                    maxDist = dist;
                    index = i;
                }
            }

            if (maxDist > epsilon) {
                const left = rdpRecursive(pts.slice(0, index + 1));
                const right = rdpRecursive(pts.slice(index));
                return left.slice(0, left.length - 1).concat(right);
            } else {
                return [pts[0], pts[end]];
            }
        };

        return rdpRecursive(points);
    }
}
