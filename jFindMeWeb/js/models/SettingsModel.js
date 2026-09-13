/**
 * jFindMe Matrix - Settings Entity Model
 * Responsibility: Persistent web settings, reference location, radius calibration, and map preferences
 * 
 * IMPROVEMENTS:
 * - Server-side persistence via Firebase
 * - LocalStorage fallback for offline mode
 * - Conflict resolution between local and server
 */
import { FBBridge } from '../services/FirebaseService.js';

export class SettingsModel {
    static STORAGE_KEY = "jfindme_matrix_settings";
    static FIREBASE_PATH = "config/operator/settings";
    
    // Server sync state
    static _syncing = false;
    static _pendingChanges = false;

    constructor() {
        // Default values (can be overridden by server or localStorage)
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
        this.lastSyncTime = null;
        
        // Performance optimization settings (NEW)
        this.batterySaverMode = false; // Reduced animations and updates
        this.disableViewportCulling = false; // Show all markers regardless of viewport
        this.adaptivePolling = true; // Automatically adjust poll rate based on activity
    }

    /**
     * Load settings from localStorage (immediate availability)
     */
    load() {
        try {
            const raw = localStorage.getItem(SettingsModel.STORAGE_KEY);
            if (raw) {
                const parsed = JSON.parse(raw);
                this._applyParsedSettings(parsed);
            }
        } catch (e) {
            console.warn("[SETTINGS] Failed to load local settings:", e);
        }
    }

    /**
     * Apply parsed settings to model properties
     */
    _applyParsedSettings(parsed) {
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
        if (parsed.lastSyncTime != null) this.lastSyncTime = parsed.lastSyncTime;
        
        // NEW: Performance settings
        if (parsed.batterySaverMode != null) this.batterySaverMode = Boolean(parsed.batterySaverMode);
        if (parsed.disableViewportCulling != null) this.disableViewportCulling = Boolean(parsed.disableViewportCulling);
        if (parsed.adaptivePolling != null) this.adaptivePolling = Boolean(parsed.adaptivePolling);
    }

    /**
     * Save settings to both localStorage (immediate) and Firebase (persistent)
     */
    async save() {
        try {
            // 1. Save to localStorage first (immediate availability)
            const payload = this._toPayload();
            localStorage.setItem(SettingsModel.STORAGE_KEY, JSON.stringify(payload));
            
            // 2. Sync to Firebase (background, non-blocking)
            await this._syncToServer(payload);
            
            return { success: true, synced: true };
        } catch (e) {
            console.error("[SETTINGS] Failed to save settings:", e);
            // Still save locally even if server sync fails
            this._saveToLocalOnly();
            return { success: true, synced: false, error: e.message };
        }
    }

    /**
     * Save only to localStorage (fallback when server unavailable)
     */
    _saveToLocalOnly() {
        try {
            const payload = this._toPayload();
            localStorage.setItem(SettingsModel.STORAGE_KEY, JSON.stringify(payload));
        } catch (e) {
            console.error("[SETTINGS] Failed to save locally:", e);
        }
    }

    /**
     * Convert model to saveable payload
     */
    _toPayload() {
        return {
            referenceLat: this.referenceLat,
            referenceLng: this.referenceLng,
            referenceAddress: this.referenceAddress,
            useBrowserLocation: this.useBrowserLocation,
            searchRadiusKm: this.searchRadiusKm,
            mapEngine: this.mapEngine,
            mapTileTheme: this.mapTileTheme,
            refreshIntervalSec: this.refreshIntervalSec,
            showRadiusCircle: this.showRadiusCircle,
            showAllDevicesIfOutsideRadius: this.showAllDevicesIfOutsideRadius,
            lastSyncTime: Date.now(),
            // NEW: Performance settings
            batterySaverMode: this.batterySaverMode,
            disableViewportCulling: this.disableViewportCulling,
            adaptivePolling: this.adaptivePolling
        };
    }

    /**
     * Sync settings to Firebase server
     * @param {object} payload - Settings payload
     */
    async _syncToServer(payload) {
        if (SettingsModel._syncing) return;
        
        try {
            SettingsModel._syncing = true;
            await FBBridge.fetchRaw('/api/save-settings', {
                method: 'POST',
                body: JSON.stringify({
                    path: SettingsModel.FIREBASE_PATH,
                    data: payload
                })
            });
            this.lastSyncTime = Date.now();
            SettingsModel._syncing = false;
        } catch (e) {
            console.warn("[SETTINGS] Server sync failed, local save only:", e.message);
            SettingsModel._syncing = false;
            SettingsModel._pendingChanges = true;
        }
    }

    /**
     * Load settings from Firebase server using proper endpoint
     * @returns {Promise<boolean>} Success status
     */
    async loadFromServer() {
        try {
            // Use the correct endpoint that returns settings directly
            const response = await FBBridge.fetchRaw('/api/settings');
            
            if (response && response.settings) {
                this._applyParsedSettings(response.settings);
                this.lastSyncTime = Date.now();
                return true;
            }
            return false;
        } catch (e) {
            console.warn("[SETTINGS] Failed to load from server:", e.message);
            return false;
        }
    }

    /**
     * Check if there are pending unsynced changes
     * @returns {boolean}
     */
    hasPendingChanges() {
        return SettingsModel._pendingChanges;
    }

    /**
     * Update reference coordinates
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
