import { MapView } from '../views/MapView.js';
import { DeviceListView } from '../views/DeviceListView.js';
import { jButterflyHubView } from '../views/jButterflyHubView.js';
import { DeviceCardView } from '../views/DeviceCardView.js';
import { DeviceEditModalView } from '../views/DeviceEditModalView.js';
import { SafeZoneCreateModalView } from '../views/SafeZoneCreateModalView.js';
import { HistoryTrailView } from '../views/HistoryTrailView.js';
import { AlertBannerView } from '../views/AlertBannerView.js';
import { DeviceController } from './DeviceController.js';
import { SafeZoneController } from './SafeZoneController.js';
import { SettingsController } from './SettingsController.js';

/**
 * jFindMe Matrix - Master Map & Radar Orchestrator (jButterfly Architecture)
 * Full Closed-Loop Controller managing live devices, jButterfly Hub UI, safe zones, alerts,
 * route trail playback, tactical vector search, traffic layer, and user calibration.
 */
export class MapController {
    constructor(mapContainerId = 'map-canvas', sidebarContainerId = 'device-sidebar') {
        this.mapContainerId = mapContainerId;
        this.sidebarContainerId = sidebarContainerId;
        this.activeSelectedDevice = null;
        
        // 1. Settings Controller
        this.settingsController = new SettingsController({
            onSettingsUpdated: (settings) => this.handleSettingsUpdated(settings),
            onPickLocationMode: () => this.enablePickMode()
        });

        // 2. SafeZone Controller
        this.safeZoneController = new SafeZoneController();

        // 3. Device Controller
        this.deviceController = new DeviceController(this.settingsController.settings);

        // 4. Map View Facade (Google Maps Primary + Leaflet Fallback)
        this.mapView = new MapView(this.mapContainerId, {
            onDeviceClick: (device) => this.handleDeviceSelected(device),
            onLocationPicked: (lat, lng) => this.handleLocationPicked(lat, lng),
            onPlaceSelected: (lat, lng, address) => this.handlePlaceSelected(lat, lng, address)
        });

        // 5. Device List Sidebar View
        this.deviceListView = new DeviceListView(this.sidebarContainerId, {
            onSelectDevice: (device) => this.handleDeviceSelected(device)
        });

        // 6. History Trail Playback View
        this.historyTrailView = new HistoryTrailView(this.mapView);

        // 7. Alert Banner View
        this.alertBannerView = new AlertBannerView('map-alert-banner-container', {
            onFocusDevice: (agentId) => {
                const dev = this.deviceController.getDeviceById(agentId);
                if (dev) this.handleDeviceSelected(dev);
            }
        });

        // 8. Device Edit Profile Modal View
        this.deviceEditModalView = new DeviceEditModalView({
            onSave: async (agentId, payload) => {
                await this.deviceController.updateDeviceProfile(agentId, payload);
                if (window.log) {
                    window.log(`PROFILE_SYNC: Profile for [${payload.name}] synchronized back to Firebase.`, "success");
                }
            }
        });

        // 9. Safe Zone Creator Modal View
        this.safeZoneCreateModalView = new SafeZoneCreateModalView({
            onSave: async (payload) => {
                await this.safeZoneController.createSafeZone(payload);
                if (window.log) {
                    window.log(`SAFE_ZONE_SYNC: Safe Zone [${payload.zoneName}] saved and synchronized.`, "success");
                }
            }
        });

        this.pollTimer = null;
        this.isInitialized = false;
    }

    /**
     * Bootstraps the Live Map Radar Module
     */
    async init() {
        const settings = this.settingsController.settings;

        // 1. Initialize Map Engine (Google Maps or Leaflet)
        await this.mapView.init(
            settings.referenceLat,
            settings.referenceLng,
            15,
            settings.mapTileTheme,
            settings.mapEngine || "leaflet"
        );

        // 2. Render Reference Location & 10 km Radar Circle
        this.mapView.updateReferenceLocation(
            settings.referenceLat,
            settings.referenceLng,
            settings.searchRadiusKm,
            settings.showRadiusCircle
        );

        // 3. Subscribe Safe Zone updates
        this.safeZoneController.subscribe((safeZones) => {
            this.mapView.renderSafeZones(safeZones, true);
        });

        this.selectedDeviceIds = null; // null means ALL devices selected

        // 4. Subscribe Device Controller updates to MapView, GoogleFindHubView, and AlertBannerView
        this.deviceController.subscribe((devices, radiusKm) => {
            this.allDevicesCache = devices;
            if (!this.selectedDeviceIds) {
                this.selectedDeviceIds = devices.map(d => d.agentId);
            }

            const visibleDevices = devices.filter(d => this.selectedDeviceIds.includes(d.agentId));
            this.mapView.renderDeviceMarkers(visibleDevices, settings.showAllDevicesIfOutsideRadius);
            
            if (!this.activeSelectedDevice && devices.length > 0) {
                this.activeSelectedDevice = devices.find(d => d.hasValidLocation) || devices[0];
            } else if (this.activeSelectedDevice) {
                this.activeSelectedDevice = devices.find(d => d.agentId === this.activeSelectedDevice.agentId) || this.activeSelectedDevice;
            }

            const sidebarEl = document.getElementById(this.sidebarContainerId);
            if (sidebarEl && this.activeSelectedDevice) {
                sidebarEl.innerHTML = jButterflyHubView.renderHubCardHtml(this.activeSelectedDevice, devices, this.selectedDeviceIds);
            }

            this.alertBannerView.update(devices);
        });

        // 5. Initial Data Fetch
        await Promise.all([
            this.deviceController.fetchLiveDevices(),
            this.safeZoneController.fetchSafeZones()
        ]);

        // Auto-frame all live devices
        setTimeout(() => {
            this.mapView.fitAllBounds();
        }, 300);

        // 6. Start Polling Engine
        this.startPolling();

        this.isInitialized = true;
        console.log("[MAP_CTRL] Master Map Radar Suite fully engaged.");
    }

    /**
     * Starts background polling engine based on refresh setting
     */
    startPolling() {
        this.stopPolling();
        const intervalSec = this.settingsController.settings.refreshIntervalSec;
        if (intervalSec > 0) {
            this.pollTimer = setInterval(async () => {
                await Promise.all([
                    this.deviceController.fetchLiveDevices(),
                    this.safeZoneController.fetchSafeZones()
                ]);
            }, intervalSec * 1000);
            console.log(`[MAP_CTRL] Live stream active (every ${intervalSec}s).`);
        }
    }

    /**
     * Stops polling
     */
    stopPolling() {
        if (this.pollTimer) {
            clearInterval(this.pollTimer);
            this.pollTimer = null;
        }
    }

    /**
     * Handles device selection from sidebar or marker click
     * @param {import('../models/DeviceModel.js').DeviceModel} device 
     */
    handleDeviceSelected(device) {
        if (!device) return;
        this.activeSelectedDevice = device;
        const sidebarEl = document.getElementById(this.sidebarContainerId);
        if (sidebarEl) {
            sidebarEl.innerHTML = jButterflyHubView.renderHubCardHtml(device, this.deviceController.devices, this.selectedDeviceIds);
        }
        this.mapView.flyToDevice(device, 13);
    }

    /**
     * Toggles device selection for multi-marker map display
     * @param {string} agentId 
     */
    toggleDeviceSelection(agentId) {
        if (!this.allDevicesCache) return;
        const allIds = this.allDevicesCache.map(d => d.agentId);
        if (!this.selectedDeviceIds) this.selectedDeviceIds = [...allIds];

        if (this.selectedDeviceIds.includes(agentId)) {
            // Prevent deselecting all (keep at least 1)
            if (this.selectedDeviceIds.length > 1) {
                this.selectedDeviceIds = this.selectedDeviceIds.filter(id => id !== agentId);
            }
        } else {
            this.selectedDeviceIds.push(agentId);
        }

        const visibleDevices = this.allDevicesCache.filter(d => this.selectedDeviceIds.includes(d.agentId));
        this.mapView.renderDeviceMarkers(visibleDevices);

        const sidebarEl = document.getElementById(this.sidebarContainerId);
        if (sidebarEl && this.activeSelectedDevice) {
            sidebarEl.innerHTML = jButterflyHubView.renderHubCardHtml(this.activeSelectedDevice, this.allDevicesCache, this.selectedDeviceIds);
        }
    }

    /**
     * Selects all devices for map marker display
     */
    selectAllDevices() {
        if (!this.allDevicesCache) return;
        const allIds = this.allDevicesCache.map(d => d.agentId);
        if (this.selectedDeviceIds && this.selectedDeviceIds.length === allIds.length) {
            // Toggle to only active selected device
            this.selectedDeviceIds = this.activeSelectedDevice ? [this.activeSelectedDevice.agentId] : [allIds[0]];
        } else {
            this.selectedDeviceIds = [...allIds];
        }

        const visibleDevices = this.allDevicesCache.filter(d => this.selectedDeviceIds.includes(d.agentId));
        this.mapView.renderDeviceMarkers(visibleDevices);

        const sidebarEl = document.getElementById(this.sidebarContainerId);
        if (sidebarEl && this.activeSelectedDevice) {
            sidebarEl.innerHTML = jButterflyHubView.renderHubCardHtml(this.activeSelectedDevice, this.allDevicesCache, this.selectedDeviceIds);
        }
    }

    /**
     * Opens Detailed Dossier Modal
     * @param {string} agentId 
     */
    showDeviceDossier(agentId) {
        const device = this.deviceController.getDeviceById(agentId);
        if (device) {
            DeviceCardView.showDossierModal(device);
        }
    }

    /**
     * Opens Edit Profile Modal
     * @param {string} agentId 
     */
    openEditProfileModal(agentId) {
        const device = this.deviceController.getDeviceById(agentId);
        if (device) {
            this.deviceEditModalView.show(device);
        }
    }

    /**
     * Opens Create Safe Zone Modal
     */
    openCreateSafeZoneModal() {
        const s = this.settingsController.settings;
        this.safeZoneCreateModalView.show(this.deviceController.devices, s.referenceLat, s.referenceLng);
    }

    /**
     * Toggles Google Live Traffic Layer
     */
    toggleTraffic() {
        const isVisible = this.mapView.toggleTraffic();
        if (window.log) {
            window.log(`TRAFFIC_LAYER: Live traffic overlay ${isVisible ? 'ENABLED' : 'DISABLED'}.`, "system");
        }
        return isVisible;
    }

    /**
     * Handles Poke Action
     * @param {string} agentId 
     */
    async pokeDevice(agentId) {
        const device = this.deviceController.getDeviceById(agentId);
        if (!device) return;
        try {
            await this.deviceController.pokeDevice(agentId, "Web_Commander");
            alert(`⚡ Poke signal transmitted to ${device.name} (${agentId})!`);
            if (window.log) {
                window.log(`POKE_SENT: Telemetry poke signal dispatched to [${device.name}].`, "success");
            }
        } catch (e) {
            alert(`Failed to transmit poke signal: ${e.message}`);
        }
    }

    /**
     * Handles Settings Updated event
     * @param {import('../models/SettingsModel.js').SettingsModel} settings 
     */
    handleSettingsUpdated(settings) {
        // If engine changed, reinitialize map
        if (this.mapView.activeEngineType !== settings.mapEngine) {
            this.mapView.init(settings.referenceLat, settings.referenceLng, 13, settings.mapTileTheme, settings.mapEngine);
        } else {
            this.mapView.setTheme(settings.mapTileTheme);
            this.mapView.updateReferenceLocation(
                settings.referenceLat,
                settings.referenceLng,
                settings.searchRadiusKm,
                settings.showRadiusCircle
            );
        }

        // Recalculate Proximities and Re-render
        this.deviceController.recalculateProximities();
        this.deviceController.notify();

        // Restart polling with new interval
        this.startPolling();

        if (window.log) {
            window.log(`RADAR_SETTINGS: Station calibrated to [${settings.referenceLat.toFixed(4)}, ${settings.referenceLng.toFixed(4)}] with ${settings.searchRadiusKm}km Radius.`, "success");
        }
    }

    /**
     * Handles Google Places Place Selected
     */
    handlePlaceSelected(lat, lng, address) {
        this.settingsController.settings.referenceLat = lat;
        this.settingsController.settings.referenceLng = lng;
        this.settingsController.settings.referenceAddress = address;
        this.settingsController.settings.save();

        this.mapView.updateReferenceLocation(
            lat,
            lng,
            this.settingsController.settings.searchRadiusKm,
            this.settingsController.settings.showRadiusCircle
        );

        this.deviceController.recalculateProximities();
        this.deviceController.notify();

        if (window.log) {
            window.log(`PLACE_SEARCH: Radar Station centered at [${address}].`, "success");
        }
    }

    /**
     * Enables Map Coordinate Picker mode
     */
    enablePickMode() {
        this.mapView.setPickMode(true);
        if (window.log) {
            window.log("RADAR_CALIBRATION: Click anywhere on the map to set reference station.", "warning");
        }
    }

    /**
     * Handles Location Picked from Map
     * @param {number} lat 
     * @param {number} lng 
     */
    handleLocationPicked(lat, lng) {
        this.settingsController.setReferenceFromMap(lat, lng);
        this.mapView.setPickMode(false);
    }

    /**
     * Shows Breadcrumb Route History Trail for a device
     * @param {string} agentId 
     */
    async showDeviceHistoryTrail(agentId) {
        const device = this.deviceController.getDeviceById(agentId);
        if (!device) return;

        if (window.log) {
            window.log(`HISTORY_PROBE: Loading historical trail for ${device.name} (${agentId})...`, "system");
        }

        const breadcrumbs = await this.deviceController.fetchDeviceHistory(agentId, 100);
        if (breadcrumbs.length > 0) {
            this.historyTrailView.showTrail(device.name, breadcrumbs);
            if (window.log) {
                window.log(`TRAIL_SYNC: Loaded ${breadcrumbs.length} historical breadcrumb vectors for ${device.name}.`, "success");
            }
        } else {
            alert(`No historical route breadcrumbs found for ${device.name}.`);
        }
    }

    toggleFullscreen() {
        const elem = document.getElementById('map-workspace');
        if (!elem) return;
        if (!document.fullscreenElement) {
            elem.requestFullscreen().catch(() => null);
        } else {
            document.exitFullscreen().catch(() => null);
        }
    }

    focusStation() {
        const s = this.settingsController.settings;
        this.mapView.flyToReference(s.referenceLat, s.referenceLng, 14);
    }

    fitAll() {
        this.mapView.fitAllBounds();
    }

    destroy() {
        this.stopPolling();
        this.historyTrailView.clear();
        this.deviceEditModalView.close();
        this.safeZoneCreateModalView.close();
    }
}
