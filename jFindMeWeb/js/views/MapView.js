import { GoogleMapView } from './GoogleMapView.js';
import { LeafletMapView } from './LeafletMapView.js';
import { AppConfig } from '../config/AppConfig.js';

/**
 * jFindMe Matrix - Master Unified Map Facade (OOPS Architecture)
 * Delegates all map rendering calls to GoogleMapView (Primary Engine) or LeafletMapView (Fallback Engine).
 */
export class MapView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.options = options;
        this.activeEngineType = AppConfig.MAP_DEFAULTS.ENGINE || "google";
        this.activeEngine = null;
        this._onDeviceClickCallback = options.onDeviceClick || null;
    }

    set onDeviceClickCallback(cb) {
        this._onDeviceClickCallback = cb;
        if (this.activeEngine) {
            this.activeEngine.onDeviceClickCallback = cb;
        }
    }

    get onDeviceClickCallback() {
        return this._onDeviceClickCallback;
    }

    /**
     * Initializes the appropriate map engine (Google Maps or Leaflet)
     * @param {number} centerLat 
     * @param {number} centerLng 
     * @param {number} zoom 
     * @param {string} theme 
     * @param {string} engineType 
     */
    async init(centerLat = 28.6139, centerLng = 77.2090, zoom = 13, theme = "dark_matrix", engineType = "leaflet") {
        this.activeEngineType = engineType;
        
        if (this.activeEngineType === "google") {
            try {
                this.activeEngine = new GoogleMapView(this.containerId, this.options);
                if (this._onDeviceClickCallback) this.activeEngine.onDeviceClickCallback = this._onDeviceClickCallback;
                await this.activeEngine.init(centerLat, centerLng, zoom, theme);
                console.log("[MAP_FACADE] Primary Google Maps V3 Engine Activated.");
                return;
            } catch (err) {
                console.warn("[MAP_FACADE] Google Maps SDK unavailable, engaging Leaflet fallback engine:", err);
                this.activeEngineType = "leaflet";
            }
        }

        // Leaflet Engine Fallback
        this.activeEngine = new LeafletMapView(this.containerId, this.options);
        if (this._onDeviceClickCallback) this.activeEngine.onDeviceClickCallback = this._onDeviceClickCallback;
        this.activeEngine.init(centerLat, centerLng, zoom, theme);
        console.log("[MAP_FACADE] Leaflet Fallback Engine Activated.");
    }

    setTheme(themeKey) {
        if (this.activeEngine) this.activeEngine.setTheme(themeKey);
    }

    toggleTraffic() {
        if (this.activeEngine && typeof this.activeEngine.toggleTrafficLayer === 'function') {
            return this.activeEngine.toggleTrafficLayer();
        }
        return false;
    }

    updateReferenceLocation(lat, lng, radiusKm = 10, showCircle = true) {
        if (this.activeEngine) this.activeEngine.updateReferenceLocation(lat, lng, radiusKm, showCircle);
    }

    renderDeviceMarkers(devices = [], showAll = true) {
        if (this.activeEngine) this.activeEngine.renderDeviceMarkers(devices, showAll);
    }

    renderSafeZones(safeZones = [], visible = true) {
        if (this.activeEngine) this.activeEngine.renderSafeZones(safeZones, visible);
    }

    renderHistoryTrail(breadcrumbs = []) {
        if (this.activeEngine) this.activeEngine.renderHistoryTrail(breadcrumbs);
    }

    clearHistoryTrail() {
        if (this.activeEngine) this.activeEngine.clearHistoryTrail();
    }

    flyToDevice(device, zoom = 13) {
        if (this.activeEngine) this.activeEngine.flyToDevice(device, zoom);
    }

    flyToReference(lat, lng, zoom = 13) {
        if (this.activeEngine) this.activeEngine.flyToReference(lat, lng, zoom);
    }

    fitAllBounds() {
        if (this.activeEngine) this.activeEngine.fitAllBounds();
    }

    setPickMode(enabled) {
        if (this.activeEngine) this.activeEngine.setPickMode(enabled);
    }

    switchTheme(theme) {
        if (this.activeEngine && typeof this.activeEngine.switchTheme === 'function') {
            this.activeEngine.switchTheme(theme);
        }
    }
}
