/**
 * jFindMe Matrix - Settings Entity Model
 * Responsibility: Persistent web settings, reference location, radius calibration, and map preferences
 */
export class SettingsModel {
    static STORAGE_KEY = "jfindme_matrix_settings";

    constructor() {
        this.referenceLat = 18.5788; // Centered on Live Device Fleet
        this.referenceLng = 73.8887;
        this.referenceAddress = "Operator Station";
        this.useBrowserLocation = false;
        this.searchRadiusKm = 10.0; // 10 km default requested by user
        this.mapEngine = "leaflet"; // 'leaflet' default (instant clean map, zero API key issues)
        this.mapTileTheme = "google_clean"; // Clean high-DPI street map with zero watermark
        this.refreshIntervalSec = 5;
        this.showRadiusCircle = true;
        this.showAllDevicesIfOutsideRadius = true; // Still show them dimmed on the map if outside 10km
        this.autoFitBounds = false;

        this.load();
    }

    /**
     * Loads settings from localStorage
     */
    load() {
        try {
            const raw = localStorage.getItem(SettingsModel.STORAGE_KEY);
            if (raw) {
                const parsed = JSON.parse(raw);
                if (parsed.referenceLat != null) this.referenceLat = parseFloat(parsed.referenceLat);
                if (parsed.referenceLng != null) this.referenceLng = parseFloat(parsed.referenceLng);
                if (parsed.referenceAddress != null) this.referenceAddress = parsed.referenceAddress;
                if (parsed.useBrowserLocation != null) this.useBrowserLocation = Boolean(parsed.useBrowserLocation);
                if (parsed.searchRadiusKm != null) this.searchRadiusKm = parseFloat(parsed.searchRadiusKm);
                if (parsed.mapEngine != null) this.mapEngine = parsed.mapEngine;
                if (parsed.mapTileTheme != null) this.mapTileTheme = parsed.mapTileTheme;
                if (parsed.refreshIntervalSec != null) this.refreshIntervalSec = parseInt(parsed.refreshIntervalSec);
                if (parsed.showRadiusCircle != null) this.showRadiusCircle = Boolean(parsed.showRadiusCircle);
                if (parsed.showAllDevicesIfOutsideRadius != null) this.showAllDevicesIfOutsideRadius = Boolean(parsed.showAllDevicesIfOutsideRadius);
            }
        } catch (e) {
            console.warn("[SETTINGS] Failed to load local settings:", e);
        }
    }

    /**
     * Saves settings to localStorage
     */
    save() {
        try {
            const payload = {
                referenceLat: this.referenceLat,
                referenceLng: this.referenceLng,
                referenceAddress: this.referenceAddress,
                useBrowserLocation: this.useBrowserLocation,
                searchRadiusKm: this.searchRadiusKm,
                mapEngine: this.mapEngine,
                mapTileTheme: this.mapTileTheme,
                refreshIntervalSec: this.refreshIntervalSec,
                showRadiusCircle: this.showRadiusCircle,
                showAllDevicesIfOutsideRadius: this.showAllDevicesIfOutsideRadius
            };
            localStorage.setItem(SettingsModel.STORAGE_KEY, JSON.stringify(payload));
        } catch (e) {
            console.error("[SETTINGS] Failed to save settings:", e);
        }
    }

    /**
     * Updates reference coordinates
     * @param {number} lat 
     * @param {number} lng 
     * @param {string} address 
     */
    setReferenceLocation(lat, lng, address = "") {
        this.referenceLat = parseFloat(lat);
        this.referenceLng = parseFloat(lng);
        if (address) this.referenceAddress = address;
        this.save();
    }

    /**
     * Updates search radius in km
     * @param {number} radiusKm 
     */
    setSearchRadius(radiusKm) {
        this.searchRadiusKm = parseFloat(radiusKm);
        this.save();
    }
}
