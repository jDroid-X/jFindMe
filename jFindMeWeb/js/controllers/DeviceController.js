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

    /**
     * Broadcasts updated device state to subscribers
     */
    notify() {
        for (const cb of this.subscribers) {
            try {
                cb(this.devices, this.settings.searchRadiusKm);
            } catch (err) {
                console.error("[DEVICE_CTRL] Subscriber error:", err);
            }
        }
    }

    /**
     * Sorts device array by Latest Timestamp on Top (Reverse-Chronological)
     * Primary: Latest Timestamp (descending)
     * Secondary: Active Session State (Online before Offline if timestamps tie)
     * @param {Array<DeviceModel>} devicesList
     * @returns {Array<DeviceModel>}
     */
    static sortByLatestFirst(devicesList) {
        if (!Array.isArray(devicesList)) return [];
        return [...devicesList].sort((a, b) => {
            const timeA = typeof a.getEpochTimestamp === 'function' ? a.getEpochTimestamp() : (Number(a.timestamp) || 0);
            const timeB = typeof b.getEpochTimestamp === 'function' ? b.getEpochTimestamp() : (Number(b.timestamp) || 0);
            if (timeB !== timeA) {
                return timeB - timeA; // Descending: Newest on Top
            }
            return (b.isOnline ? 1 : 0) - (a.isOnline ? 1 : 0);
        });
    }

    /**
     * Filters devices by Time Window:
     * - 'live': Active in last 15 minutes (or currently online)
     * - 'today': Active since 00:00:00 of current day
     * - '24h': Active within past 24 hours
     * - 'all': All fleet devices
     * @param {Array<DeviceModel>} devicesList
     * @param {'live'|'today'|'24h'|'all'} windowType
     * @returns {Array<DeviceModel>}
     */
    static filterByTimeWindow(devicesList, windowType = 'all') {
        if (!Array.isArray(devicesList)) return [];
        const now = Date.now();

        switch (windowType) {
            case 'live':
                return devicesList.filter(d => {
                    if (d.isOnline) return true;
                    const epoch = typeof d.getEpochTimestamp === 'function' ? d.getEpochTimestamp() : 0;
                    return epoch > 0 && (now - epoch) <= 15 * 60 * 1000;
                });
            case 'today': {
                const startOfToday = new Date().setHours(0, 0, 0, 0);
                return devicesList.filter(d => {
                    const epoch = typeof d.getEpochTimestamp === 'function' ? d.getEpochTimestamp() : 0;
                    return epoch >= startOfToday;
                });
            }
            case '24h':
                return devicesList.filter(d => {
                    const epoch = typeof d.getEpochTimestamp === 'function' ? d.getEpochTimestamp() : 0;
                    return epoch > 0 && (now - epoch) <= 24 * 60 * 60 * 1000;
                });
            case 'all':
            default:
                return devicesList;
        }
    }

    /**
     * Calculates the geometric center (centroid) of all devices with valid GPS coordinates
     * @param {Array<DeviceModel>} devicesList 
     * @returns {{ lat: number, lng: number }|null}
     */
    static calculateFleetCentroid(devicesList) {
        if (!Array.isArray(devicesList)) return null;
        const valid = devicesList.filter(d => d.hasValidLocation && d.latitude && d.longitude);
        if (valid.length === 0) return null;
        const avgLat = valid.reduce((acc, d) => acc + d.latitude, 0) / valid.length;
        const avgLng = valid.reduce((acc, d) => acc + d.longitude, 0) / valid.length;
        return { lat: avgLat, lng: avgLng };
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
                const mapped = res.devices.map(d => new DeviceModel(d));
                this.devices = DeviceController.sortByLatestFirst(mapped);
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
     * @param {number|Object} optionsOrLimit 
     * @returns {Promise<Array>}
     */
    async fetchDeviceHistory(agentId, optionsOrLimit = 100) {
        try {
            let queryParams = `agentId=${encodeURIComponent(agentId)}`;
            if (typeof optionsOrLimit === 'number') {
                queryParams += `&limit=${optionsOrLimit}`;
            } else if (typeof optionsOrLimit === 'object' && optionsOrLimit !== null) {
                if (optionsOrLimit.limit) queryParams += `&limit=${optionsOrLimit.limit}`;
                if (optionsOrLimit.startDate) queryParams += `&startDate=${encodeURIComponent(optionsOrLimit.startDate)}`;
                if (optionsOrLimit.endDate) queryParams += `&endDate=${encodeURIComponent(optionsOrLimit.endDate)}`;
                if (optionsOrLimit.startTime) queryParams += `&startTime=${encodeURIComponent(optionsOrLimit.startTime)}`;
                if (optionsOrLimit.endTime) queryParams += `&endTime=${encodeURIComponent(optionsOrLimit.endTime)}`;
                if (optionsOrLimit.deadband !== undefined) queryParams += `&deadband=${Boolean(optionsOrLimit.deadband)}`;
                if (optionsOrLimit.order) queryParams += `&order=${encodeURIComponent(optionsOrLimit.order)}`;
            }

            const res = await FBBridge.fetchRaw(`/api/device-history?${queryParams}`);
            if (res && res.breadcrumbs) {
                return res.breadcrumbs;
            }
            return [];
        } catch (err) {
            console.error(`[DEVICE_CTRL] History fetch error for ${agentId}:`, err);
            return [];
        }
    }

    /**
     * Discovers all distinct dates containing recorded telemetry for a device
     * @param {string} agentId 
     * @returns {Promise<Array<{date: string, count: number, firstTs: number, lastTs: number}>>}
     */
    async fetchHistoricalDates(agentId) {
        try {
            const res = await FBBridge.fetchRaw(`/api/history-dates?agentId=${encodeURIComponent(agentId)}`);
            if (res && res.dates) {
                return res.dates;
            }
            return [];
        } catch (err) {
            console.error(`[DEVICE_CTRL] Historical dates fetch error for ${agentId}:`, err);
            return [];
        }
    }
}
