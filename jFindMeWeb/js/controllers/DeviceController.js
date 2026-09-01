import { DeviceModel } from '../models/DeviceModel.js';
import { GeoUtils } from '../core/GeoUtils.js';
import { FBBridge } from '../services/FirebaseService.js';

/**
 * jFindMe Matrix - Device Business Logic Controller (OOPS Architecture)
 * Manages device data fetching, proximity math, profile updates, poke triggers, and state dispatching.
 */
export class DeviceController {
    constructor(settings) {
        this.settings = settings;
        this.devices = []; // Array<DeviceModel>
        this.selectedDevice = null;
        this.subscribers = [];
        this.isLoading = false;
        this.lastFetchTime = 0;
    }

    /**
     * Subscribes to device list state changes
     * @param {Function} callback 
     */
    subscribe(callback) {
        this.subscribers.push(callback);
    }

    /**
     * Broadcasts updated device state to subscribers
     */
    notify() {
        for (const cb of this.subscribers) {
            cb(this.devices, this.settings.searchRadiusKm);
        }
    }

    /**
     * Fetches all registered devices and their live locations from Firebase Bridge
     * @returns {Promise<Array<DeviceModel>>}
     */
    async fetchLiveDevices() {
        this.isLoading = true;
        try {
            const res = await FBBridge.fetchRaw('/api/devices-live');
            if (res && res.devices && Array.isArray(res.devices)) {
                this.devices = res.devices.map(d => new DeviceModel(d));
                this.recalculateProximities();
                this.lastFetchTime = Date.now();
                this.notify();

                // Background reverse-geocode any device missing a readable address
                this.resolveMissingAddresses();

                return this.devices;
            }
            return [];
        } catch (err) {
            console.error("[DEVICE_CTRL] Fetch Live Devices Error:", err);
            return [];
        } finally {
            this.isLoading = false;
        }
    }

    /**
     * Background resolution of reverse-geocoded street addresses
     */
    async resolveMissingAddresses() {
        for (const device of this.devices) {
            if (device.hasValidLocation && !device.address) {
                GeoUtils.reverseGeocode(device.latitude, device.longitude).then(addr => {
                    if (addr && addr !== "Unknown Location") {
                        device.address = addr;
                        this.notify();
                    }
                }).catch(() => null);
            }
        }
    }

    /**
     * Recalculates distances for all devices relative to operator station
     */
    recalculateProximities() {
        const refLat = this.settings.referenceLat;
        const refLng = this.settings.referenceLng;
        const radiusKm = this.settings.searchRadiusKm;

        for (const device of this.devices) {
            device.updateProximity(refLat, refLng, radiusKm);
        }
    }

    /**
     * Finds a device by agent ID
     * @param {string} agentId 
     * @returns {DeviceModel|null}
     */
    getDeviceById(agentId) {
        return this.devices.find(d => d.agentId === agentId) || null;
    }

    /**
     * Atomically updates a device profile in Firebase RTDB
     * @param {string} agentId 
     * @param {object} profileData 
     * @returns {Promise<object>}
     */
    async updateDeviceProfile(agentId, profileData) {
        try {
            const res = await FBBridge.fetchRaw('/api/update-profile', {
                method: 'POST',
                body: JSON.stringify({ agentId, profileData })
            });

            if (res && res.status === 'SUCCESS') {
                const device = this.getDeviceById(agentId);
                if (device) {
                    if (profileData.name) device.name = profileData.name;
                    if (profileData.avatar_id) device.avatarId = profileData.avatar_id;
                    if (profileData.department) device.department = profileData.department;
                    if (profileData.phone) device.phone = profileData.phone;
                    if (profileData.email) device.email = profileData.email;
                    if (profileData.emergency_note) device.emergencyNote = profileData.emergency_note;
                    this.notify();
                }
                return res;
            }
            throw new Error(res.error || "PROFILE_UPDATE_FAILED");
        } catch (err) {
            console.error(`[DEVICE_CTRL] Profile update failed for ${agentId}:`, err);
            throw err;
        }
    }

    /**
     * Sends a Poke / Ping telemetry trigger to a target device
     * @param {string} agentId 
     * @param {string} pokedBy 
     * @returns {Promise<object>}
     */
    async pokeDevice(agentId, pokedBy = "Web_Matrix_Commander") {
        try {
            const res = await FBBridge.fetchRaw('/api/poke-device', {
                method: 'POST',
                body: JSON.stringify({ agentId, pokedBy })
            });
            return res;
        } catch (err) {
            console.error(`[DEVICE_CTRL] Poke trigger failed for ${agentId}:`, err);
            throw err;
        }
    }

    /**
     * Fetches historical breadcrumbs for a device
     * @param {string} agentId 
     * @param {number} limit 
     * @returns {Promise<Array>}
     */
    async fetchDeviceHistory(agentId, limit = 100) {
        try {
            const res = await FBBridge.fetchRaw(`/api/device-history?agentId=${encodeURIComponent(agentId)}&limit=${limit}`);
            if (res && res.breadcrumbs) {
                return res.breadcrumbs;
            }
            return [];
        } catch (err) {
            console.error(`[DEVICE_CTRL] History fetch error for ${agentId}:`, err);
            return [];
        }
    }
}
