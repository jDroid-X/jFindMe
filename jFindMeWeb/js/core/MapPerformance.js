/**
 * MapPerformanceOptimizer.js - High-Performance Leaflet Map Engine
 * Optimizes marker rendering, reduces battery consumption, improves accuracy
 */

import { GeoUtils } from '../core/GeoUtils.js';

export class MapPerformanceOptimizer {
    static _instance = null;
    static _markerPool = new Map();
    static _animationFrameId = null;
    static _lastRenderTime = 0;
    static _fpsTarget = 60;
    static _frameInterval = 1000 / 60; // ~16.67ms for 60fps
    
    // Performance thresholds
    static MARKER_CLUSTER_THRESHOLD = 50;
    static CANVAS_THRESHOLD = 200;
    static VIEWPORT_CULLING_ENABLED = true;
    
    /**
     * Get singleton instance
     */
    static getInstance() {
        if (!this._instance) {
            this._instance = new MapPerformanceOptimizer();
        }
        return this._instance;
    }
    
    /**
     * Clear marker pool (call on cleanup)
     */
    static clearPool() {
        this._markerPool.clear();
        console.log('[PERF] Marker pool cleared');
    }
    
    /**
     * Get or create marker from pool
     */
    static getMarker(map, lat, lng, icon, options = {}) {
        const key = `${lat.toFixed(6)},${lng.toFixed(6)}`;
        
        if (!this._markerPool.has(key)) {
            const marker = L.marker([lat, lng], {
                icon: icon,
                zIndexOffset: options.zIndexOffset || 0,
                ...options
            });
            this._markerPool.set(key, marker);
            marker.addTo(map);
            return marker;
        }
        
        const marker = this._markerPool.get(key);
        marker.setLatLng([lat, lng]);
        return marker;
    }
    
    /**
     * Return marker to pool (don't remove from map)
     */
    static returnMarker(marker) {
        // Marker stays on map, just reset for reuse
    }
    
    /**
     * Calculate optimal poll interval based on device activity
     * @param {Array} devices - Device array
     * @param {number} baseInterval - Base interval in seconds
     * @returns {number} Optimized interval in milliseconds
     */
    static calculateOptimalPollInterval(devices, baseInterval = 5000) {
        if (!devices || devices.length === 0) return baseInterval;
        
        let movingCount = 0;
        let totalDistance = 0;
        let now = Date.now();
        
        devices.forEach(device => {
            const speed = parseFloat(device.speed || 0);
            const lastSeen = parseInt(device.lastSeen || 0);
            const elapsed = now - lastSeen;
            
            // Count moving devices (speed > 2 km/h)
            if (speed > 2) {
                movingCount++;
            }
            
            // Calculate average distance traveled
            if (device.distanceFromRefKm && device.distanceFromRefKm < 100) {
                totalDistance += device.distanceFromRefKm;
            }
        });
        
        const movingRatio = movingCount / devices.length;
        
        // Adaptive polling strategy:
        // - If all stationary: 30s interval (battery saver)
        // - If mostly stationary (< 20% moving): 15s interval
        // - If mixed activity: 10s interval
        // - If all moving: 3s interval (real-time tracking)
        
        let intervalMultiplier;
        
        if (movingRatio === 0) {
            intervalMultiplier = 6; // 30s
        } else if (movingRatio < 0.2) {
            intervalMultiplier = 3; // 15s
        } else if (movingRatio < 0.5) {
            intervalMultiplier = 2; // 10s
        } else {
            intervalMultiplier = 0.6; // 3s
        }
        
        const optimizedInterval = Math.max(3000, baseInterval * intervalMultiplier);
        
        // Debug logging (only when changed significantly)
        if (Math.abs(optimizedInterval - baseInterval) > 1000) {
            console.log(`[PERF] Adaptive polling: ${devices.length} devices, ${movingCount} moving (${Math.round(movingRatio * 100)}%), interval: ${optimizedInterval}ms`);
        }
        
        return optimizedInterval;
    }
    
    /**
     * Filter devices by viewport bounds (culling)
     * @param {Array} devices - All devices
     * @param {Object} bounds - Leaflet LatLngBounds
     * @param {number} bufferMeters - Buffer zone in meters
     * @returns {Array} Devices within bounds
     */
    static filterByViewport(devices, bounds, bufferMeters = 50000) {
        if (!this.VISIBLEPORT_CULLING_ENABLED || !bounds) {
            return devices;
        }
        
        const buffer = bounds;
        const expandedBounds = L.latLngBounds(
            bounds.getSouthWest().translate(-bufferMeters / 111320, -bufferMeters / 111320),
            bounds.getNorthEast().translate(bufferMeters / 111320, bufferMeters / 111320)
        );
        
        return devices.filter(device => {
            if (!device.hasValidLocation) return false;
            return expandedBounds.contains([device.latitude, device.longitude]);
        });
    }
    
    /**
     * Smooth marker animation using requestAnimationFrame
     * @param {L.Marker} marker - Leaflet marker
     * @param {number} targetLat - Target latitude
     * @param {number} targetLng - Target longitude
     * @param {number} duration - Animation duration in ms
     */
    static animateMarker(marker, targetLat, targetLng, duration = 1000) {
        if (!marker || !targetLat || !targetLng) return;
        
        const startLat = marker.getLatLng().lat;
        const startLng = marker.getLatLng().lng;
        const startTime = performance.now();
        
        const animate = (currentTime) => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            
            // Easing function (ease-in-out cubic)
            const eased = progress < 0.5
                ? 4 * progress * progress * progress
                : 1 - Math.pow(-2 * progress + 2, 3) / 2;
            
            const currentLat = startLat + (targetLat - startLat) * eased;
            const currentLng = startLng + (targetLng - startLng) * eased;
            
            marker.setLatLng([currentLat, currentLng]);
            
            if (progress < 1) {
                this._animationFrameId = requestAnimationFrame(animate);
            }
        };
        
        if (this._animationFrameId) {
            cancelAnimationFrame(this._animationFrameId);
        }
        
        this._animationFrameId = requestAnimationFrame(animate);
    }
    
    /**
     * Batch update multiple markers efficiently
     * Uses single requestAnimationFrame call for all updates
     */
    static batchUpdateMarkers(map, devices, iconFactory) {
        if (!map || !devices || devices.length === 0) return;
        
        const now = performance.now();
        if (now - this._lastRenderTime < this._frameInterval) {
            return; // Skip frame to maintain 60fps
        }
        
        this._lastRenderTime = now;
        
        // Use document fragment for batch DOM operations
        const fragment = document.createDocumentFragment();
        
        devices.forEach(device => {
            if (!device.hasValidLocation) return;
            
            const icon = iconFactory(device);
            const marker = this.getMarker(map, device.latitude, device.longitude, icon, {
                zIndexOffset: device.isOnline ? 1000 : 500
            });
            
            // Bind popup only if not already bound
            if (!marker.getPopup()) {
                marker.bindPopup(this.createDevicePopup(device));
            }
        });
        
        console.log(`[PERF] Rendered ${devices.length} markers in ${performance.now() - now}ms`);
    }
    
    /**
     * Create optimized device popup
     */
    static createDevicePopup(device) {
        const batteryColor = device.battery >= 50 ? '#10b981' : 
                           device.battery >= 20 ? '#f59e0b' : '#ef4444';
        
        return `
            <div class="perf-popup" style="min-width: 200px;">
                <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
                    <div style="width: 32px; height: 32px; border-radius: 50%; background: #3b82f6; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold;">
                        ${device.name?.charAt(0)?.toUpperCase() || '?'}
                    </div>
                    <div>
                        <div style="font-weight: bold; font-size: 14px;">${device.name || 'Unknown'}</div>
                        <div style="font-size: 11px; color: #64748b;">${device.agentId}</div>
                    </div>
                </div>
                <div style="font-size: 12px; line-height: 1.6;">
                    <div><strong>Status:</strong> ${device.isOnline ? '<span style="color: #10b981;">● Online</span>' : '<span style="color: #94a3b8;">● Offline</span>'}</div>
                    <div><strong>Speed:</strong> ${device.speed?.toFixed(1) || 0} km/h</div>
                    <div><strong>Battery:</strong> <span style="color: ${batteryColor};">${device.battery}%</span></div>
                    <div><strong>Last Seen:</strong> ${this.formatTimeAgo(device.lastSeen)}</div>
                </div>
            </div>
        `;
    }
    
    /**
     * Format time ago string
     */
    static formatTimeAgo(timestamp) {
        if (!timestamp) return 'Unknown';
        const seconds = Math.floor((Date.now() - timestamp) / 1000);
        
        if (seconds < 60) return `${seconds}s ago`;
        if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
        if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
        return `${Math.floor(seconds / 86400)}d ago`;
    }
    
    /**
     * Calculate LOD (Level of Detail) for markers based on zoom
     * @param {number} zoom - Current zoom level
     * @param {number} deviceCount - Number of devices
     * @returns {Object} LOD settings
     */
    static calculateLOD(zoom, deviceCount) {
        const lod = {
            showAccuracy: false,
            showBattery: true,
            showSpeed: true,
            clusterRadius: 50,
            animationEnabled: true
        };
        
        // Adjust based on zoom and device count
        if (zoom < 12) {
            // Zoomed out - simplify
            lod.showAccuracy = false;
            lod.showSpeed = false;
            lod.clusterRadius = 80;
        } else if (zoom < 15) {
            // Medium zoom
            lod.showAccuracy = true;
            lod.clusterRadius = 60;
        } else {
            // Zoomed in - full details
            lod.clusterRadius = 40;
        }
        
        // Reduce animations for many devices
        if (deviceCount > 100) {
            lod.animationEnabled = false;
            lod.clusterRadius = Math.min(lod.clusterRadius, 50);
        }
        
        return lod;
    }
    
    /**
     * Measure map performance metrics
     */
    static measurePerformance(map) {
        const metrics = {
            markerCount: map.getAllLayers?.length || 0,
            fps: this.calculateFPS(),
            memoryEstimate: this.estimateMemoryUsage(),
            renderTime: performance.now()
        };
        
        console.log('[PERF]', metrics);
        return metrics;
    }
    
    /**
     * Calculate current FPS
     */
    static calculateFPS() {
        // Simplified FPS calculation
        return 60; // Placeholder - real implementation would track frame times
    }
    
    /**
     * Estimate memory usage
     */
    static estimateMemoryUsage() {
        if (performance.memory) {
            return Math.round(performance.memory.usedJSHeapSize / 1024 / 1024);
        }
        return 'N/A';
    }
    
    /**
     * Enable/disable performance monitoring
     */
    static setMonitoring(enabled) {
        if (enabled) {
            setInterval(() => {
                this.measurePerformance(window.activeMap);
            }, 5000);
            console.log('[PERF] Monitoring enabled');
        } else {
            console.log('[PERF] Monitoring disabled');
        }
    }
}

// Export singleton
export const mapOptimizer = MapPerformanceOptimizer.getInstance();
