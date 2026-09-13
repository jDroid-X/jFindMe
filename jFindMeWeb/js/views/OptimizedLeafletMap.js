/**
 * OptimizedLeafletMap.js - High-Performance Map Engine
 * Optimized for: Performance, Accuracy, Battery Efficiency
 */

import { GeoUtils } from '../core/GeoUtils.js';
import { MapPerformanceOptimizer } from '../core/MapPerformance.js';

export class OptimizedLeafletMap {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.map = null;
        this.options = options;
        
        // Performance settings
        this.performance = {
            maxMarkers: 500,
            clusterThreshold: 50,
            viewportCulling: true,
            animationFrameEnabled: true,
            batterySaverMode: false,
            adaptivePolling: true
        };
        
        // Layer groups
        this.deviceLayerGroup = null;
        this.clusterGroup = null;
        this.safeZoneLayerGroup = null;
        this.trailLayerGroup = null;
        this.referenceLayerGroup = null;
        
        // Markers cache
        this.deviceMarkers = new Map();
        this.markerOptions = {};
        
        // Animation frame handling
        this.animationFrameId = null;
        this.lastUpdateTimestamp = 0;
        this.minUpdateInterval = 1000 / 30; // Cap at 30fps for updates
        
        // Viewport bounds tracking
        this.currentBounds = null;
        this.boundsChangeDebounce = null;
        
        // On-device callbacks
        this.onDeviceClickCallback = options.onDeviceClick || null;
    }
    
    /**
     * Initialize the optimized map
     */
    async init(centerLat = 28.6139, centerLng = 77.2090, zoom = 13, theme = 'dark_matrix') {
        const container = document.getElementById(this.containerId);
        if (!container) {
            console.error('[OPTIMIZED_MAP] Container not found:', this.containerId);
            return;
        }
        
        // Remove existing map if present
        if (this.map) {
            this.map.remove();
            this.map = null;
        }
        
        // Create map with performance optimizations
        this.map = L.map(container, {
            center: [centerLat, centerLng],
            zoom: zoom,
            zoomControl: false,
            attributionControl: false,
            preferCanvas: true, // Use canvas rendering for better performance
            zoomAnimation: true,
            markerZoomAnimation: true
        });
        
        // Add zoom control
        L.control.zoom({ position: 'topleft' }).addTo(this.map);
        L.control.scale({ imperial: false, position: 'bottomleft' }).addTo(this.map);
        
        // Initialize layer groups
        this.deviceLayerGroup = L.layerGroup().addTo(this.map);
        this.safeZoneLayerGroup = L.layerGroup().addTo(this.map);
        this.trailLayerGroup = L.layerGroup().addTo(this.map);
        this.referenceLayerGroup = L.layerGroup().addTo(this.map);
        
        // Initialize marker cluster group (key for performance!)
        this.clusterGroup = L.markerClusterGroup({
            maxClusterRadius: 50,
            singletonParentClusterCap: true,
            spiderfyOnMaxZoom: true,
            showCoverageOnHover: false,
            zoomToBoundsOnClick: true,
            iconCreateFunction: (cluster) => {
                const count = cluster.getChildCount();
                let size = 'small';
                if (count > 10) size = 'medium';
                if (count > 50) size = 'large';
                
                return L.divIcon({
                    html: `<div><span>${count}</span></div>`,
                    className: `marker-cluster marker-cluster-${size}`,
                    iconSize: L.point(40, 40)
                });
            }
        });
        
        // Add cluster group to map
        this.clusterGroup.addTo(this.map);
        
        // Setup event listeners for viewport culling
        this.setupViewportListeners();
        
        // Ensure map fits container
        setTimeout(() => {
            if (this.map) this.map.invalidateSize();
        }, 100);
        
        console.log('[OPTIMIZED_MAP] Initialized with performance optimizations');
        return this;
    }
    
    /**
     * Setup viewport change listeners for culling
     */
    setupViewportListeners() {
        // Debounced bounds change handler
        const handleBoundsChange = () => {
            if (this.boundsChangeDebounce) clearTimeout(this.boundsChangeDebounce);
            this.boundsChangeDebounce = setTimeout(() => {
                this.currentBounds = this.map.getBounds();
                this.onViewportChange?.(this.currentBounds);
            }, 150);
        };
        
        this.map.on('moveend', handleBoundsChange);
        this.map.on('zoomend', handleBoundsChange);
        
        // Initial bounds
        this.currentBounds = this.map.getBounds();
    }
    
    /**
     * Set callback for viewport changes
     */
    onViewportChange(callback) {
        this.onViewportChange = callback;
    }
    
    /**
     * Render device markers with clustering and culling
     */
    renderDeviceMarkers(devices = [], options = {}) {
        if (!this.map || !devices || devices.length === 0) return;
        
        const startTime = performance.now();
        
        // Apply performance optimizations
        const showAll = options.showAll !== false;
        const useClustering = devices.length > this.performance.clusterThreshold;
        
        // Clear existing markers from cluster group
        this.clusterGroup.clearLayers();
        
        // Filter and prepare markers
        const validDevices = devices.filter(d => {
            if (!d.hasValidLocation) return false;
            if (!showAll && !d.isWithinSearchRadius) return false;
            
            // Viewport culling
            if (this.performance.viewportCulling && this.currentBounds) {
                return this.currentBounds.contains([d.latitude, d.longitude]);
            }
            return true;
        });
        
        // Limit max markers for performance
        const displayDevices = validDevices.slice(0, this.performance.maxMarkers);
        
        // Create markers
        displayDevices.forEach(device => {
            const marker = this.createOptimizedMarker(device);
            
            if (useClustering) {
                this.clusterGroup.addLayer(marker);
            } else {
                marker.addTo(this.deviceLayerGroup);
            }
            
            this.deviceMarkers.set(device.agentId, marker);
        });
        
        // Remove markers for devices no longer visible
        for (const [agentId, marker] of this.deviceMarkers.entries()) {
            const device = displayDevices.find(d => d.agentId === agentId);
            if (!device) {
                if (useClustering) {
                    this.clusterGroup.removeLayer(marker);
                } else {
                    this.deviceLayerGroup.removeLayer(marker);
                }
                this.deviceMarkers.delete(agentId);
            }
        }
        
        // Add cluster group to map if using clustering
        if (useClustering && !this.map.hasLayer(this.clusterGroup)) {
            this.clusterGroup.addTo(this.map);
        }
        
        const endTime = performance.now();
        console.log(`[OPTIMIZED_MAP] Rendered ${displayDevices.length} markers in ${(endTime - startTime).toFixed(2)}ms`);
    }
    
    /**
     * Create optimized marker for device
     */
    createOptimizedMarker(device) {
        const batteryColor = device.battery >= 50 ? '#10b981' : 
                           device.battery >= 20 ? '#f59e0b' : '#ef4444';
        const statusColor = device.isOnline ? '#10b981' : '#94a3b8';
        
        // Create simple, optimized SVG icon
        const svgIcon = L.divIcon({
            className: 'optimized-device-marker',
            html: `
                <div style="position: relative; width: 24px; height: 24px;">
                    <!-- Status indicator dot -->
                    <div style="position: absolute; top: 0; right: 0; width: 8px; height: 8px; border-radius: 50%; background: ${statusColor}; border: 2px solid white;"></div>
                    <!-- Battery indicator -->
                    <div style="position: absolute; bottom: -2px; right: -2px; width: 10px; height: 6px; background: ${batteryColor}; border: 1px solid white; border-radius: 1px;"></div>
                    <!-- Main marker -->
                    <div style="width: 20px; height: 20px; border-radius: 50%; background: ${device.getThemeColor?.() || '#3b82f6'}; border: 2px solid white; box-shadow: 0 2px 4px rgba(0,0,0,0.3);"></div>
                </div>
            `,
            iconSize: [24, 24],
            iconAnchor: [12, 12],
            popupAnchor: [0, -12]
        });
        
        const marker = L.marker([device.latitude, device.longitude], {
            icon: svgIcon,
            zIndexOffset: device.isOnline ? 1000 : 500
        });
        
        // Add click handler
        marker.on('click', () => {
            if (this.onDeviceClickCallback) {
                this.onDeviceClickCallback(device);
            }
        });
        
        // Add tooltip with essential info only
        if (device.name) {
            marker.bindTooltip(
                `<strong>${device.name}</strong><br>
                 <span style="color: ${statusColor}">● ${device.isOnline ? 'ONLINE' : 'OFFLINE'}</span> | 
                 ${device.speed?.toFixed(0) || 0} km/h`,
                { direction: 'top', offset: [0, -10] }
            );
        }
        
        return marker;
    }
    
    /**
     * Update reference location (operator station)
     */
    updateReferenceLocation(lat, lng, radiusKm = 10, showCircle = true) {
        if (!this.map || isNaN(lat) || isNaN(lng)) return;
        
        const center = [lat, lng];
        
        // Reference marker
        if (!this.referenceMarker) {
            const refIcon = L.divIcon({
                className: 'reference-station-marker',
                html: `
                    <div style="position: relative;">
                        <div style="width: 16px; height: 16px; border-radius: 50%; background: #3b82f6; border: 3px solid white; box-shadow: 0 2px 8px rgba(59,130,246,0.5);"></div>
                        <div style="position: absolute; top: -4px; left: -4px; width: 24px; height: 24px; border-radius: 50%; border: 2px solid #3b82f6; animation: pulse 2s infinite;"></div>
                    </div>
                `,
                iconSize: [24, 24],
                iconAnchor: [12, 12]
            });
            
            this.referenceMarker = L.marker(center, { 
                icon: refIcon, 
                zIndexOffset: 2000 
            }).addTo(this.referenceLayerGroup);
        } else {
            this.referenceMarker.setLatLng(center);
        }
        
        // Radar circle
        if (showCircle && radiusKm > 0) {
            const radiusMeters = radiusKm * 1000;
            
            if (!this.radarCircle) {
                this.radarCircle = L.circle(center, {
                    radius: radiusMeters,
                    color: '#00E5FF',
                    weight: 1.5,
                    opacity: 0.6,
                    fillColor: '#00E5FF',
                    fillOpacity: 0.05,
                    dashArray: '6, 8'
                }).addTo(this.referenceLayerGroup);
            } else {
                this.radarCircle.setLatLng(center);
                this.radarCircle.setRadius(radiusMeters);
            }
        } else if (this.radarCircle) {
            this.referenceLayerGroup.removeLayer(this.radarCircle);
            this.radarCircle = null;
        }
    }
    
    /**
     * Render safe zones with optimized polygons
     */
    renderSafeZones(safeZones = [], visible = true) {
        if (!this.safeZoneLayerGroup) return;
        
        this.safeZoneLayerGroup.clearLayers();
        if (!visible || !safeZones) return;
        
        safeZones.forEach(zone => {
            if (!zone.latitude || !zone.longitude) return;
            
            const color = zone.getColor?.() || (zone.isActive !== false ? '#10b981' : '#64748b');
            
            // Circle zone
            const circle = L.circle([zone.latitude, zone.longitude], {
                radius: zone.radius || 100,
                color: color,
                weight: 2,
                opacity: 0.8,
                fillColor: color,
                fillOpacity: 0.1
            }).addTo(this.safeZoneLayerGroup);
            
            circle.bindTooltip(`<b>🛡️ ${zone.zoneName}</b><br>Radius: ${zone.radius || 100}m`);
        });
    }
    
    /**
     * Render history trail with optimized polyline
     */
    renderHistoryTrail(breadcrumbs = []) {
        if (!this.trailLayerGroup || !breadcrumbs || breadcrumbs.length === 0) return;
        
        this.trailLayerGroup.clearLayers();
        
        // Create polyline from breadcrumbs
        const latlngs = breadcrumbs
            .filter(b => b.latitude && b.longitude)
            .map(b => [b.latitude, b.longitude]);
        
        if (latlngs.length > 1) {
            const polyline = L.polyline(latlngs, {
                color: '#3b82f6',
                weight: 3,
                opacity: 0.8,
                lineJoin: 'round'
            }).addTo(this.trailLayerGroup);
            
            // Add trail markers (sparse for performance)
            const step = Math.max(1, Math.floor(latlngs.length / 10));
            latlngs.forEach((latlng, idx) => {
                if (idx % step === 0) {
                    L.circleMarker(latlng, {
                        radius: 4,
                        color: '#3b82f6',
                        fillColor: '#fff',
                        fillOpacity: 1,
                        weight: 2
                    }).addTo(this.trailLayerGroup);
                }
            });
        }
    }
    
    /**
     * Clear history trail
     */
    clearHistoryTrail() {
        if (this.trailLayerGroup) {
            this.trailLayerGroup.clearLayers();
        }
    }
    
    /**
     * Fly to device location
     */
    flyToDevice(device, zoom = 14) {
        if (!this.map || !device?.hasValidLocation) return;
        
        this.map.flyTo([device.latitude, device.longitude], zoom, {
            duration: 1.5,
            easeLinearity: 0.25
        });
    }
    
    /**
     * Fit map to all device bounds
     */
    fitAllBounds(devices = []) {
        if (!this.map || !devices || devices.length === 0) return;
        
        const validDevices = devices.filter(d => d.hasValidLocation);
        if (validDevices.length === 0) return;
        
        const bounds = L.latLngBounds(validDevices.map(d => [d.latitude, d.longitude]));
        this.map.fitBounds(bounds, { padding: [50, 50] });
    }
    
    /**
     * Toggle traffic layer (if supported)
     */
    toggleTrafficLayer() {
        // Traffic layer would need Google Maps or specific tile provider
        console.warn('[OPTIMIZED_MAP] Traffic layer not supported in Leaflet mode');
        return false;
    }
    
    /**
     * Set pick mode for clicking map locations
     */
    setPickMode(enabled) {
        this.isPickMode = enabled;
        
        if (enabled) {
            this.map.getContainer().style.cursor = 'crosshair';
            this.map.on('click', this.handlePickClick);
        } else {
            this.map.getContainer().style.cursor = '';
            this.map.off('click', this.handlePickClick);
        }
    }
    
    handlePickClick = (e) => {
        if (this.isPickMode && this.onLocationPickedCallback) {
            this.onLocationPickedCallback(e.latlng.lat, e.latlng.lng);
            this.setPickMode(false);
        }
    };
    
    /**
     * Switch map theme
     */
    switchTheme(theme) {
        if (!this.map || !this.tileLayers?.[theme]) return;
        
        if (this.currentTileLayer) {
            this.map.removeLayer(this.currentTileLayer);
        }
        
        this.currentTileLayer = this.tileLayers[theme].addTo(this.map);
    }
    
    /**
     * Set tile layers
     */
    setTileLayers(tileLayers) {
        this.tileLayers = tileLayers;
    }
    
    /**
     * Enable/disable battery saver mode
     */
    setBatterySaverMode(enabled) {
        this.performance.batterySaverMode = enabled;
        
        if (enabled) {
            // Reduce map interactions for battery saving
            this.performance.viewportCulling = true;
            this.performance.animationFrameEnabled = false;
            console.log('[OPTIMIZED_MAP] Battery saver mode enabled');
        } else {
            this.performance.animationFrameEnabled = true;
            console.log('[OPTIMIZED_MAP] Battery saver mode disabled');
        }
    }
    
    /**
     * Enable/disable adaptive polling
     */
    setAdaptivePolling(enabled) {
        this.performance.adaptivePolling = enabled;
    }
    
    /**
     * Get current performance metrics
     */
    getPerformanceMetrics() {
        return {
            markerCount: this.deviceMarkers.size,
            clusterCount: this.clusterGroup?.getLayers()?.length || 0,
            isUsingClustering: this.clusterGroup?.getLayers().length > 0,
            batterySaverMode: this.performance.batterySaverMode,
            viewportCulling: this.performance.viewportCulling
        };
    }
    
    /**
     * Cleanup and destroy map
     */
    destroy() {
        if (this.map) {
            this.map.remove();
            this.map = null;
        }
        
        this.deviceMarkers.clear();
        this.clusterGroup = null;
        this.animationFrameId = null;
        
        console.log('[OPTIMIZED_MAP] Cleaned up');
    }
}
