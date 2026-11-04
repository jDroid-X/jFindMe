import { SettingsModel } from '../models/SettingsModel.js';
import { MapSettingsView } from '../views/MapSettingsView.js';

/**
 * jFindMe Matrix - Settings Controller (OOPS Architecture)
 * Coordinates user preferences, Geolocation calibration, and MapSettingsView
 */
export class SettingsController {
    constructor(options = {}) {
        this.settings = new SettingsModel();
        this.onSettingsUpdated = options.onSettingsUpdated || null;
        this.onPickLocationMode = options.onPickLocationMode || null;

        this.view = new MapSettingsView({
            onSave: (updated) => this.handleSave(updated),
            onPickOnMap: () => this.handlePickOnMap(),
            onAutoDetectGps: () => this.detectBrowserGps()
        });
    }

    /**
     * Opens the Settings modal
     */
    openSettingsModal() {
        this.view.show(this.settings);
    }

    /**
     * Auto-detects browser GPS coordinates using HTML5 Geolocation API
     * @returns {Promise<{latitude: number, longitude: number}>}
     */
    async detectBrowserGps() {
        return new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
                return reject(new Error("Geolocation is not supported by your browser."));
            }

            navigator.geolocation.getCurrentPosition(
                (pos) => {
                    const coords = {
                        latitude: pos.coords.latitude,
                        longitude: pos.coords.longitude
                    };
                    this.settings.setReferenceLocation(coords.latitude, coords.longitude, "Browser GPS Station");
                    resolve(coords);
                },
                (err) => {
                    console.warn("[GPS] Geolocation query failed:", err.message);
                    reject(err);
                },
                { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
            );
        });
    }

    /**
     * Handles manual coordinate selection from map click
     * @param {number} lat 
     * @param {number} lng 
     */
    setReferenceFromMap(lat, lng) {
        this.settings.setReferenceLocation(lat, lng, "Map Calibrated Station");
        if (this.onSettingsUpdated) {
            this.onSettingsUpdated(this.settings);
        }
    }

    handleSave(updated) {
        this.settings = updated;
        if (this.onSettingsUpdated) {
            this.onSettingsUpdated(this.settings);
        }
    }

    handlePickOnMap() {
        if (this.onPickLocationMode) {
            this.onPickLocationMode();
        }
    }
}
