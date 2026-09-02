import { GoogleMapsLoader } from '../services/GoogleMapsLoader.js';
import { MapConfig } from '../config/MapConfig.js';
import { DeviceCardView } from './DeviceCardView.js';
import { GeoUtils } from '../core/GeoUtils.js';

/**
 * jFindMe Matrix - Google Maps JavaScript API V3 Engine (OOPS Architecture)
 * Supports Google Maps vector tiles, Places Autocomplete, Traffic Layer,
 * Custom HTML OverlayView Markers, 10 km Radar Circles, Geofences, and Trails.
 */
export class GoogleMapView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.map = null;
        this.google = null;
        this.trafficLayer = null;
        this.isTrafficVisible = false;
        
        this.referenceMarker = null;
        this.radarCircle = null;
        this.customMarkers = new Map(); // agentId -> CustomOverlay
        this.safeZoneCircles = [];
        this.trailPolyline = null;
        this.trailCheckpoints = [];
        
        this.infoWindow = null;
        this.isPickMode = false;
        this.autocomplete = null;

        this.onDeviceClickCallback = options.onDeviceClick || null;
        this.onLocationPickedCallback = options.onLocationPicked || null;
        this.onPlaceSelectedCallback = options.onPlaceSelected || null;
    }

    /**
     * Initializes Google Maps SDK & Map Instance
     * @param {number} centerLat 
     * @param {number} centerLng 
     * @param {number} zoom 
     * @param {string} theme 
     */
    async init(centerLat = 28.6139, centerLng = 77.2090, zoom = 13, theme = "dark_matrix") {
        this.google = await GoogleMapsLoader.load();
        const container = document.getElementById(this.containerId);
        if (!container) return;

        const mapTypeId = (theme === "satellite" || theme === "hybrid") ? this.google.maps.MapTypeId.HYBRID : this.google.maps.MapTypeId.ROADMAP;

        const mapOptions = {
            center: { lat: centerLat, lng: centerLng },
            zoom: zoom,
            mapTypeId: mapTypeId,
            styles: (theme === "dark_matrix") ? MapConfig.GOOGLE_DARK_MATRIX_STYLE : null,
            disableDefaultUI: false,
            zoomControl: true,
            zoomControlOptions: { position: this.google.maps.ControlPosition.RIGHT_CENTER },
            mapTypeControl: true,
            mapTypeControlOptions: {
                position: this.google.maps.ControlPosition.TOP_LEFT,
                style: this.google.maps.MapTypeControlStyle.DROPDOWN_MENU
            },
            scaleControl: true,
            streetViewControl: true,
            fullscreenControl: false,
            gestureHandling: "greedy"
        };

        this.map = new this.google.maps.Map(container, mapOptions);
        this.trafficLayer = new this.google.maps.TrafficLayer();
        this.infoWindow = new this.google.maps.InfoWindow();

        // Cursor Coordinate Tracker
        this.map.addListener("mousemove", (e) => {
            const coordEl = document.getElementById('map-cursor-coords');
            if (coordEl && e.latLng) {
                coordEl.textContent = `${e.latLng.lat().toFixed(5)}°, ${e.latLng.lng().toFixed(5)}°`;
            }
        });

        // Click Listener for Pick on Map
        this.map.addListener("click", (e) => {
            if (this.isPickMode && this.onLocationPickedCallback && e.latLng) {
                this.onLocationPickedCallback(e.latLng.lat(), e.latLng.lng());
                this.setPickMode(false);
            }
        });

        // Initialize Places Autocomplete
        this.initPlacesAutocomplete();

        console.log(`[GOOGLE_MAP_VIEW] Google Maps V3 initialized at [${centerLat}, ${centerLng}].`);
    }

    /**
     * Initializes Google Places Autocomplete Input Box
     */
    initPlacesAutocomplete() {
        const input = document.getElementById('google-places-search-input');
        if (!input || !this.google || !this.google.maps.places) return;

        this.autocomplete = new this.google.maps.places.Autocomplete(input, {
            fields: ["geometry", "name", "formatted_address"]
        });

        this.autocomplete.bindTo("bounds", this.map);

        this.autocomplete.addListener("place_changed", () => {
            const place = this.autocomplete.getPlace();
            if (!place.geometry || !place.geometry.location) {
                return;
            }

            const lat = place.geometry.location.lat();
            const lng = place.geometry.location.lng();
            this.map.setCenter({ lat, lng });
            this.map.setZoom(14);

            if (this.onPlaceSelectedCallback) {
                this.onPlaceSelectedCallback(lat, lng, place.formatted_address || place.name);
            }
        });
    }

    /**
     * Toggles Google Live Traffic Layer
     */
    toggleTrafficLayer() {
        if (!this.map || !this.trafficLayer) return;
        this.isTrafficVisible = !this.isTrafficVisible;
        this.trafficLayer.setMap(this.isTrafficVisible ? this.map : null);
        return this.isTrafficVisible;
    }

    /**
     * Updates Reference Station Marker & 10 km Radar Circle
     * @param {number} lat 
     * @param {number} lng 
     * @param {number} radiusKm 
     * @param {boolean} showCircle 
     */
    updateReferenceLocation(lat, lng, radiusKm = 10, showCircle = true) {
        if (!this.map || !this.google || isNaN(lat) || isNaN(lng) || (lat === 0 && lng === 0)) return;

        const center = { lat, lng };

        // 1. Radar Circle Overlay (10 km default)
        const radiusMeters = radiusKm * 1000;
        if (!this.radarCircle) {
            this.radarCircle = new this.google.maps.Circle({
                map: showCircle ? this.map : null,
                center: center,
                radius: radiusMeters,
                strokeColor: "#00E5FF",
                strokeOpacity: 0.8,
                strokeWeight: 1.5,
                fillColor: "#00E5FF",
                fillOpacity: 0.06,
                clickable: false
            });
        } else {
            this.radarCircle.setCenter(center);
            this.radarCircle.setRadius(radiusMeters);
            this.radarCircle.setMap(showCircle ? this.map : null);
        }

        // 2. Reference Station Custom Pin
        if (!this.referenceMarker) {
            this.referenceMarker = new this.google.maps.Marker({
                position: center,
                map: this.map,
                zIndex: 1000,
                icon: {
                    path: this.google.maps.SymbolPath.CIRCLE,
                    scale: 12,
                    fillColor: "#0284c7",
                    fillOpacity: 1,
                    strokeColor: "#ffffff",
                    strokeWeight: 3
                },
                title: "Operator Station (You)"
            });
        } else {
            this.referenceMarker.setPosition(center);
        }
    }

    /**
     * Renders devices as custom HTML overlay pins with avatars
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices 
     * @param {boolean} showAll 
     */
    renderDeviceMarkers(devices = [], showAll = true) {
        if (!this.map || !this.google) return;

        const currentAgentIds = new Set();
        const OverlayView = this.google.maps.OverlayView;

        // 🛡️ Coordinate Dispersion Matrix (Anti-Overlap Physics matching Android MapEngine.kt)
        const displacedFleet = GeoUtils.applyAntiOverlapDisplacement(devices);

        for (const item of displacedFleet) {
            const { device, displayLat, displayLng } = item;
            if (!device || !device.hasValidLocation) continue;

            if (!showAll && !device.isWithinSearchRadius) {
                if (this.customMarkers.has(device.agentId)) {
                    this.customMarkers.get(device.agentId).setMap(null);
                    this.customMarkers.delete(device.agentId);
                }
                continue;
            }

            currentAgentIds.add(device.agentId);
            const pos = new this.google.maps.LatLng(displayLat, displayLng);
            const themeColor = device.getThemeColor();
            const avatarUrl = device.getAvatarUrl();
            const isWithin = device.isWithinSearchRadius;
            const bearing = Math.round(device.bearing || 0);

            if (this.customMarkers.has(device.agentId)) {
                // Update existing overlay
                const overlay = this.customMarkers.get(device.agentId);
                overlay.update(device, pos);
            } else {
                // Create custom Google Maps HTML Overlay
                const overlay = new GoogleCustomMarkerOverlay(
                    pos,
                    device,
                    this.map,
                    () => {
                        if (this.onDeviceClickCallback) {
                            this.onDeviceClickCallback(device);
                        }
                    }
                );
                this.customMarkers.set(device.agentId, overlay);
            }
        }

        // Clean up removed devices
        for (const [agentId, overlay] of this.customMarkers.entries()) {
            if (!currentAgentIds.has(agentId)) {
                overlay.setMap(null);
                this.customMarkers.delete(agentId);
            }
        }
    }

    /**
     * Renders Safe Zone Geofence Circles
     * @param {Array<import('../models/SafeZoneModel.js').SafeZoneModel>} safeZones 
     * @param {boolean} visible 
     */
    renderSafeZones(safeZones = [], visible = true) {
        if (!this.google || !this.map) return;

        // Clear existing
        this.safeZoneCircles.forEach(c => c.setMap(null));
        this.safeZoneCircles = [];

        if (!visible || !safeZones) return;

        safeZones.forEach(zone => {
            if (!zone.latitude || !zone.longitude) return;

            const color = (typeof zone.getColor === 'function') ? zone.getColor() : (zone.isActive !== false ? '#0284C7' : '#64748B');

            const circle = new this.google.maps.Circle({
                map: this.map,
                center: { lat: zone.latitude, lng: zone.longitude },
                radius: zone.radius || 100,
                strokeColor: color,
                strokeOpacity: 0.85,
                strokeWeight: 2,
                fillColor: color,
                fillOpacity: 0.12
            });

            this.safeZoneCircles.push(circle);
        });
    }

    /**
     * Renders historical route polyline breadcrumbs
     * @param {Array} breadcrumbs 
     */
    renderHistoryTrail(breadcrumbs = []) {
        if (!this.google || !this.map) return;
        this.clearHistoryTrail();

        if (!breadcrumbs || breadcrumbs.length === 0) return;

        const path = breadcrumbs.map(b => ({ lat: b.latitude, lng: b.longitude }));

        this.trailPolyline = new this.google.maps.Polyline({
            path: path,
            geodesic: true,
            strokeColor: "#00E5FF",
            strokeOpacity: 0.85,
            strokeWeight: 4,
            map: this.map
        });

        // Fit bounds
        const bounds = new this.google.maps.LatLngBounds();
        path.forEach(p => bounds.extend(p));
        this.map.fitBounds(bounds, { top: 60, bottom: 60, left: 60, right: 60 });
    }

    clearHistoryTrail() {
        if (this.trailPolyline) {
            this.trailPolyline.setMap(null);
            this.trailPolyline = null;
        }
    }

    flyToDevice(device, zoom = 13) {
        if (!this.map || !device) return;
        this.map.panTo({ lat: device.latitude, lng: device.longitude });
        this.map.setZoom(zoom);
    }

    flyToReference(lat, lng, zoom = 13) {
        if (!this.map) return;
        this.map.panTo({ lat, lng });
        this.map.setZoom(zoom);
    }

    fitAllBounds(minRadiusKm = 5.0) {
        if (!this.map || !this.google) return;
        const bounds = new this.google.maps.LatLngBounds();
        if (this.referenceMarker) bounds.extend(this.referenceMarker.getPosition());
        for (const overlay of this.customMarkers.values()) {
            bounds.extend(overlay.latLng);
        }
        if (!bounds.isEmpty()) {
            // Expand to at least 5 km radial window
            const center = bounds.getCenter();
            const bufferLat = minRadiusKm / 111.0;
            const bufferLng = minRadiusKm / (111.0 * Math.cos(center.lat() * Math.PI / 180.0));

            bounds.extend({ lat: center.lat() - bufferLat, lng: center.lng() - bufferLng });
            bounds.extend({ lat: center.lat() + bufferLat, lng: center.lng() + bufferLng });

            this.map.fitBounds(bounds, { top: 50, bottom: 50, left: 50, right: 50 });
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

/**
 * Custom Google Maps HTML OverlayView for Dynamic Avatar Pins & Visiting Card Tooltips
 */
class GoogleCustomMarkerOverlay {
    constructor(latLng, device, map, onClick) {
        this.latLng = latLng;
        this.device = device;
        this.onClick = onClick;
        this.div = null;
        this.tooltipDiv = null;
        
        this.overlay = new google.maps.OverlayView();
        this.overlay.onAdd = this.onAdd.bind(this);
        this.overlay.draw = this.draw.bind(this);
        this.overlay.onRemove = this.onRemove.bind(this);
        this.overlay.setMap(map);
    }

    onAdd() {
        this.div = document.createElement("div");
        this.div.className = "matrix-marker-wrapper in-radar online";
        this.div.style.position = "absolute";
        this.div.style.cursor = "pointer";
        this.div.style.zIndex = "500";

        this.renderHtml();

        // Visiting Card Tooltip on Hover
        this.div.addEventListener("mouseenter", () => {
            this.showTooltip();
        });
        this.div.addEventListener("mouseleave", () => {
            this.hideTooltip();
        });

        // Click handler
        this.div.addEventListener("click", (e) => {
            e.stopPropagation();
            if (this.onClick) this.onClick();
        });

        const panes = this.overlay.getPanes();
        panes.overlayMouseTarget.appendChild(this.div);
    }

    renderHtml() {
        if (!this.div) return;
        const themeColor = this.device.getThemeColor();
        const avatarUrl = this.device.getAvatarUrl();
        const bearing = Math.round(this.device.bearing || 0);

        this.div.innerHTML = `
            <div class="marker-callout-pill" style="border-color: ${themeColor};">
                <span class="marker-name-text">${this.device.name}</span>
            </div>
            <div class="marker-avatar-container" style="border-color: ${themeColor}; box-shadow: 0 0 12px ${themeColor}60;">
                <img src="${avatarUrl}" class="marker-avatar-img" alt="${this.device.name}"
                     onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><circle cx=%2250%22 cy=%2250%22 r=%2248%22 fill=%22%230f172a%22/><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2235%22>👤</text></svg>'">
                <span class="marker-status-dot ${this.device.isOnline ? 'online' : 'offline'}"></span>
                ${this.device.battery >= 0 ? `<span class="marker-battery-pill">${this.device.battery}%</span>` : ''}
            </div>
            ${this.device.speed > 0.8 ? `<div class="marker-bearing-arrow" style="transform: rotate(${bearing}deg); border-bottom-color: ${themeColor}"></div>` : ''}
            <div class="marker-pin-tip" style="border-top-color: ${themeColor};"></div>
        `;
    }

    draw() {
        if (!this.div || !this.overlay) return;
        const projection = this.overlay.getProjection();
        if (!projection) return;

        const point = projection.fromLatLngToDivPixel(this.latLng);
        if (point) {
            this.div.style.left = `${point.x - 24}px`;
            this.div.style.top = `${point.y - 60}px`;
        }
    }

    showTooltip() {
        this.hideTooltip();
        this.tooltipDiv = document.createElement("div");

        // 🛡️ Viewport Boundary Clamping: prevent card from hiding off browser borders
        const rect = this.div ? this.div.getBoundingClientRect() : { top: 300, left: 300, right: 300 };
        const isNearTop = rect.top < 220; // Needs 200px above
        const isNearRight = (window.innerWidth - rect.right) < 230;
        const isNearLeft = rect.left < 230;

        let positionClasses = "bottom-full left-1/2 -translate-x-1/2 mb-3";
        if (isNearTop) {
            positionClasses = "top-full left-1/2 -translate-x-1/2 mt-3"; // Flip below
        }
        if (isNearRight) {
            positionClasses = isNearTop ? "top-full right-0 mt-3" : "bottom-full right-0 mb-3";
        } else if (isNearLeft) {
            positionClasses = isNearTop ? "top-full left-0 mt-3" : "bottom-full left-0 mb-3";
        }

        this.tooltipDiv.className = `absolute ${positionClasses} z-[9999] pointer-events-auto select-none`;
        this.tooltipDiv.innerHTML = DeviceCardView.renderTooltipHtml(this.device);
        this.div.appendChild(this.tooltipDiv);
    }

    hideTooltip() {
        if (this.tooltipDiv) {
            this.tooltipDiv.remove();
            this.tooltipDiv = null;
        }
    }

    update(device, latLng) {
        this.device = device;
        this.latLng = latLng;
        this.renderHtml();
        this.draw();
    }

    onRemove() {
        if (this.div && this.div.parentNode) {
            this.div.parentNode.removeChild(this.div);
            this.div = null;
        }
    }

    setMap(map) {
        this.overlay.setMap(map);
    }
}
