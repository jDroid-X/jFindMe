import { SafeZoneModel } from '../models/SafeZoneModel.js';
import { FBBridge } from '../services/FirebaseService.js';

/**
 * jFindMe Matrix - SafeZone Business Logic Controller (OOPS Architecture)
 * Manages fetching, creation, caching, and state synchronization for Geofencing Safe Zones.
 */
export class SafeZoneController {
    constructor() {
        this.safeZones = []; // Array<SafeZoneModel>
        this.subscribers = [];
        this.isLoading = false;
    }

    /**
     * Subscribes to safe zone state updates
     * @param {Function} callback 
     * @returns {Function} Unsubscribe function
     */
    subscribe(callback) {
        this.subscribers.push(callback);
        
        // Return unsubscribe function
        return () => {
            const index = this.subscribers.indexOf(callback);
            if (index > -1) {
                this.subscribers.splice(index, 1);
            }
        };
    }

    notify() {
        for (const cb of this.subscribers) {
            try {
                cb(this.safeZones);
            } catch (err) {
                console.error("[SAFE_ZONE_CTRL] Subscriber error:", err);
            }
        }
    }

    /**
     * Fetches all safe zones from Firebase backend proxy
     * @returns {Promise<Array<SafeZoneModel>>}
     */
    async fetchSafeZones() {
        this.isLoading = true;
        try {
            const res = await FBBridge.fetchRaw('/api/safe-zones');
            if (res && res.safeZones && Array.isArray(res.safeZones)) {
                this.safeZones = res.safeZones.map(z => new SafeZoneModel(z));
                this.notify();
                return this.safeZones;
            }
            return [];
        } catch (err) {
            console.error("[SAFE_ZONE_CTRL] Fetch error:", err);
            return [];
        } finally {
            this.isLoading = false;
        }
    }

    /**
     * Creates and saves a new Safe Zone to Firebase
     * @param {object} payload 
     * @returns {Promise<object>}
     */
    async createSafeZone(payload) {
        try {
            const res = await FBBridge.fetchRaw('/api/save-safe-zone', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            if (res && res.status === 'SUCCESS') {
                await this.fetchSafeZones();
                return res;
            }
            throw new Error(res.error || "SAVE_SAFE_ZONE_FAILED");
        } catch (err) {
            console.error("[SAFE_ZONE_CTRL] Create safe zone error:", err);
            throw err;
        }
    }
}
