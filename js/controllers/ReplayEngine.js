/**
 * ⏱️ ReplayEngine & AnalyticsEngine: Temporal Breadcrumb & Historical Route Controller.
 * [Variables]: fraction, points, activeIndex, playbackSpeed
 * [Constants]: MIN_FRACTION = 0.0, MAX_FRACTION = 1.0
 * [Dependencies]: GeoUtils.js
 * [Integration Points]: HistoryTrailView.js, MapView.js
 */
export const ReplayEngine = {
    fraction: 0.0,
    isPlaying: false,

    seekToFraction(fraction) {
        this.fraction = Math.max(0.0, Math.min(1.0, fraction));
        console.log(`[ReplayEngine] Seeking temporal playback to ${(this.fraction * 100).toFixed(1)}%`);
        if (typeof window !== 'undefined' && window.dispatchEvent) {
            window.dispatchEvent(new CustomEvent('replay_fraction_changed', { detail: { fraction: this.fraction } }));
        }
        return this.fraction;
    }
};

export const AnalyticsEngine = {
    calculateRouteStats(points = []) {
        if (!points || points.length === 0) {
            return { totalDistanceMeters: 0, averageSpeedKmh: 0, durationSec: 0, waypointCount: 0 };
        }
        let totalDistance = 0;
        let totalSpeed = 0;
        for (let i = 0; i < points.length; i++) {
            totalSpeed += (points[i].speed || 0);
            if (i > 0 && points[i].lat && points[i-1].lat) {
                // Approximate distance
                const dLat = (points[i].lat - points[i-1].lat) * 111000;
                const dLng = (points[i].lng - points[i-1].lng) * 111000;
                totalDistance += Math.sqrt(dLat * dLat + dLng * dLng);
            }
        }
        return {
            totalDistanceMeters: Math.round(totalDistance),
            averageSpeedKmh: Math.round((totalSpeed / points.length) * 3.6),
            durationSec: points.length > 1 ? Math.round((points[points.length - 1].timestamp - points[0].timestamp) / 1000) : 0,
            waypointCount: points.length
        };
    }
};

if (typeof window !== 'undefined') {
    window.ReplayEngine = ReplayEngine;
    window.AnalyticsEngine = AnalyticsEngine;
}

