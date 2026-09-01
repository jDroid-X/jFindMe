import { MapConfig } from '../config/MapConfig.js';
import { DeviceCardView } from './DeviceCardView.js';
import { jButterflyHubView } from './jButterflyHubView.js';
import { GeoUtils } from '../core/GeoUtils.js';

/**
 * jFindMe Matrix - Leaflet.js Tactical Map View (Secondary / Fallback Engine)
 */
export class LeafletMapView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.map = null;
        this.tileLayers = {};
        this.currentTileLayer = null;
        this.radarCircle = null;
        this.referenceMarker = null;
        this.deviceMarkers = new Map();
        this.safeZonesLayerGroup = null;
        this.trailLayerGroup = null;
        this.isPickMode = false;
        
        this.onDeviceClickCallback = options.onDeviceClick || null;
        this.onLocationPickedCallback = options.onLocationPicked || null;
    }

    init(centerLat = 28.6139, centerLng = 77.2090, zoom = 13, theme = "dark_matrix") {
        const container = document.getElementById(this.containerId);
        if (!container) return;

        if (this.map) {
            this.map.remove();
            this.map = null;
        }

        this.map = L.map(this.containerId, {
            center: [centerLat, centerLng],
            zoom: zoom,
            zoomControl: false,
            attributionControl: false
        });

        L.control.zoom({ position: 'topright' }).addTo(this.map);
        L.control.scale({ imperial: false, position: 'bottomleft' }).addTo(this.map);

        for (const [key, provider] of Object.entries(MapConfig.TILE_PROVIDERS)) {
            this.tileLayers[key] = L.tileLayer(provider.url, {
                maxZoom: provider.maxZoom || 22,
                maxNativeZoom: provider.maxNativeZoom || 19,
                subdomains: provider.subdomains || '0123',
                attribution: provider.attribution
            });
        }

        const availableThemes = Object.keys(this.tileLayers);
        const activeTheme = this.tileLayers[theme] ? theme : (availableThemes[0] || "google_clean");
        if (this.tileLayers[activeTheme]) {
            this.currentTileLayer = this.tileLayers[activeTheme].addTo(this.map);
        }

        this.trailLayerGroup = L.layerGroup().addTo(this.map);
        this.safeZonesLayerGroup = L.layerGroup().addTo(this.map);

        this.map.on('mousemove', (e) => {
            const coordEl = document.getElementById('map-cursor-coords');
            if (coordEl) coordEl.textContent = `${e.latlng.lat.toFixed(5)}°, ${e.latlng.lng.toFixed(5)}°`;
        });

        this.map.on('click', (e) => {
            if (this.isPickMode && this.onLocationPickedCallback) {
                this.onLocationPickedCallback(e.latlng.lat, e.latlng.lng);
                this.setPickMode(false);
            }
        });

        // Ensure Leaflet map canvas immediately matches exact container window size
        setTimeout(() => {
            if (this.map) this.map.invalidateSize();
        }, 150);

        window.addEventListener('resize', () => {
            if (this.map) this.map.invalidateSize();
        });
    }

    setTheme(themeKey) {
        if (!this.map || !this.tileLayers[themeKey]) return;
        if (this.currentTileLayer) this.map.removeLayer(this.currentTileLayer);
        this.currentTileLayer = this.tileLayers[themeKey].addTo(this.map);
    }

    updateReferenceLocation(lat, lng, radiusKm = 10, showCircle = true) {
        if (!this.map || isNaN(lat) || isNaN(lng)) return;
        const center = [lat, lng];

        if (!this.referenceMarker) {
            const refIcon = L.divIcon({
                className: 'jbutterfly-station-beacon-container',
                html: `
                    <div class="relative flex items-center justify-center w-8 h-8 pointer-events-none">
                        <div class="w-8 h-8 rounded-full bg-blue-500/25 animate-ping absolute"></div>
                        <div class="w-3.5 h-3.5 rounded-full bg-blue-600 border-2 border-white shadow-md relative"></div>
                    </div>
                `,
                iconSize: [32, 32],
                iconAnchor: [16, 16]
            });
            this.referenceMarker = L.marker(center, { icon: refIcon, zIndexOffset: 200 }).addTo(this.map);
        } else {
            this.referenceMarker.setLatLng(center);
        }

        const radiusMeters = radiusKm * 1000;
        if (!this.radarCircle) {
            this.radarCircle = L.circle(center, {
                radius: radiusMeters,
                color: '#00E5FF',
                weight: 1.5,
                opacity: 0.8,
                fillColor: '#00E5FF',
                fillOpacity: 0.06,
                dashArray: '6, 8'
            }).addTo(this.map);
        } else {
            this.radarCircle.setLatLng(center);
            this.radarCircle.setRadius(radiusMeters);
        }

        if (!showCircle && this.map.hasLayer(this.radarCircle)) {
            this.map.removeLayer(this.radarCircle);
        } else if (showCircle && !this.map.hasLayer(this.radarCircle)) {
            this.radarCircle.addTo(this.map);
        }
    }

    renderDeviceMarkers(devices = [], showAll = true) {
        if (!this.map) return;
        const currentAgentIds = new Set();

        // 🛡️ Coordinate Dispersion Matrix (Anti-Overlap Physics matching Android MapEngine.kt)
        const displacedFleet = GeoUtils.applyAntiOverlapDisplacement(devices);

        for (const item of displacedFleet) {
            const { device, displayLat, displayLng } = item;
            if (!device || !device.hasValidLocation) continue;
            if (!showAll && !device.isWithinSearchRadius) {
                if (this.deviceMarkers.has(device.agentId)) {
                    this.map.removeLayer(this.deviceMarkers.get(device.agentId));
                    this.deviceMarkers.delete(device.agentId);
                }
                continue;
            }

            currentAgentIds.add(device.agentId);
            const latLng = [displayLat, displayLng];
            const themeColor = device.getThemeColor();
            const avatarUrl = device.getAvatarUrl();
            const isWithin = device.isWithinSearchRadius;
            const bearing = Math.round(device.bearing || 0);

            const customIcon = L.divIcon({
                className: 'jbutterfly-leaflet-marker',
                html: jButterflyHubView.renderTacticalPinHtml(device),
                iconSize: [120, 80],
                iconAnchor: [60, 75]
            });

            if (this.deviceMarkers.has(device.agentId)) {
                const existing = this.deviceMarkers.get(device.agentId);
                existing.setLatLng(latLng);
                existing.setIcon(customIcon);
                existing.setTooltipContent(DeviceCardView.renderTooltipHtml(device));
            } else {
                const marker = L.marker(latLng, { icon: customIcon }).addTo(this.map);
                marker.bindTooltip(DeviceCardView.renderTooltipHtml(device), {
                    direction: 'auto',
                    offset: [0, 0],
                    opacity: 1,
                    className: 'matrix-custom-leaflet-tooltip'
                });
                marker.on('click', () => {
                    if (this.onDeviceClickCallback) this.onDeviceClickCallback(device);
                });
                this.deviceMarkers.set(device.agentId, marker);
            }
        }

        // 🕸️ Render Spiderweb Anchor Lines & Centroid Dots for Dispersed Clusters
        if (!this.spiderLinesGroup) {
            this.spiderLinesGroup = L.layerGroup().addTo(this.map);
        }
        this.spiderLinesGroup.clearLayers();

        const drawnCentroids = new Set();
        for (const item of displacedFleet) {
            if (item.isDisplaced && item.centroidLat && item.centroidLng) {
                // Connecting line from pin to centroid
                L.polyline([
                    [item.displayLat, item.displayLng],
                    [item.centroidLat, item.centroidLng]
                ], {
                    color: item.device.getThemeColor ? item.device.getThemeColor() : '#00E5FF',
                    weight: 1.2,
                    dashArray: '3, 4',
                    opacity: 0.55
                }).addTo(this.spiderLinesGroup);

                // Draw centroid anchor beacon once per cluster
                const centroidKey = `${item.centroidLat.toFixed(5)}_${item.centroidLng.toFixed(5)}`;
                if (!drawnCentroids.has(centroidKey)) {
                    drawnCentroids.add(centroidKey);
                    L.circleMarker([item.centroidLat, item.centroidLng], {
                        radius: 4,
                        color: '#00E5FF',
                        weight: 1.5,
                        fillColor: '#00E5FF',
                        fillOpacity: 0.35
                    }).addTo(this.spiderLinesGroup);
                }
            }
        }

        for (const [agentId, marker] of this.deviceMarkers.entries()) {
            if (!currentAgentIds.has(agentId)) {
                this.map.removeLayer(marker);
                this.deviceMarkers.delete(agentId);
            }
        }
    }

    renderSafeZones(safeZones = [], visible = true) {
        if (!this.safeZonesLayerGroup) return;
        this.safeZonesLayerGroup.clearLayers();
        if (!visible || !safeZones) return;

        safeZones.forEach(zone => {
            if (!zone.latitude || !zone.longitude) return;
            L.circle([zone.latitude, zone.longitude], {
                radius: zone.radius || 100,
                color: zone.getColor(),
                weight: 1.5,
                opacity: 0.8,
                fillColor: zone.getColor(),
                fillOpacity: 0.08,
                dashArray: '5, 5'
            }).addTo(this.safeZonesLayerGroup);
        });
    }

    renderHistoryTrail(breadcrumbs = []) {
        if (!this.trailLayerGroup) return;
        this.trailLayerGroup.clearLayers();
        if (!breadcrumbs || breadcrumbs.length === 0) return;

        const latLngs = breadcrumbs.map(b => [b.latitude, b.longitude]);
        const line = L.polyline(latLngs, {
            color: '#00E5FF',
            weight: 4,
            opacity: 0.85,
            dashArray: '8, 8'
        }).addTo(this.trailLayerGroup);
        this.map.fitBounds(line.getBounds(), { padding: [60, 60] });
    }

    clearHistoryTrail() {
        if (this.trailLayerGroup) this.trailLayerGroup.clearLayers();
    }

    flyToDevice(device, zoom = 13) {
        if (!this.map || !device || !device.hasValidLocation) return;
        // Default zoom 13 guarantees at least 5km radial perspective around target
        this.map.flyTo([device.latitude, device.longitude], zoom, { duration: 1.2 });
    }

    flyToReference(lat, lng, zoom = 13) {
        if (this.map) this.map.flyTo([lat, lng], zoom, { duration: 1.2 });
    }

    fitAllBounds(minRadiusKm = 5.0) {
        if (!this.map) return;
        const group = [];
        if (this.referenceMarker) group.push(this.referenceMarker.getLatLng());
        for (const marker of this.deviceMarkers.values()) group.push(marker.getLatLng());
        
        if (group.length > 0) {
            const rawBounds = L.latLngBounds(group);
            const center = rawBounds.getCenter();
            
            // Ensure at least 5km radial margin around the entire fleet
            const bufferLat = minRadiusKm / 111.0;
            const bufferLng = minRadiusKm / (111.0 * Math.cos(center.lat * Math.PI / 180.0));
            
            const south = Math.min(rawBounds.getSouth(), center.lat - bufferLat);
            const north = Math.max(rawBounds.getNorth(), center.lat + bufferLat);
            const west = Math.min(rawBounds.getWest(), center.lng - bufferLng);
            const east = Math.max(rawBounds.getEast(), center.lng + bufferLng);
            
            this.map.fitBounds(L.latLngBounds([[south, west], [north, east]]), { padding: [50, 50] });
        }
    }

    setPickMode(enabled) {
        this.isPickMode = enabled;
        const container = document.getElementById(this.containerId);
        if (container) {
            if (enabled) container.classList.add('cursor-crosshair');
            else container.classList.remove('cursor-crosshair');
        }
    }
}
