import { MapConfig } from '../config/MapConfig.js';
import { FeedbackDialog } from '../components/FeedbackDialog.js';

/**
 * jFindMe Matrix - Web Settings View (Dynamic OOPS Architecture)
 * Provides comprehensive configuration for Reference Location, Search Radius (10 km default),
 * Map Themes, Geolocation Auto-Detection, and Refresh Rates.
 * 100% Theme-Adaptive (Dark / Light) with feedback toasts.
 */
export class MapSettingsView {
    constructor(options = {}) {
        this.onSave = options.onSave || null;
        this.onPickOnMap = options.onPickOnMap || null;
        this.onAutoDetectGps = options.onAutoDetectGps || null;
        this.modalElement = null;
    }

    /**
     * Shows the Web Settings Modal
     * @param {import('../models/SettingsModel.js').SettingsModel} settings 
     */
    show(settings) {
        this.close(); // Clean up if already open

        const modal = document.createElement('div');
        modal.id = 'map-settings-modal';
        modal.className = 'fixed inset-0 z-[1000] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-fade-in font-sans';
        
        modal.innerHTML = `
            <div class="card-theme border rounded-2xl w-full max-w-sm shadow-2xl overflow-hidden text-primary-theme flex flex-col max-h-[90vh]">
                <!-- Modal Header -->
                <div class="p-3.5 border-b border-subtle-theme flex items-center justify-between surface-theme">
                    <div class="flex items-center gap-2.5">
                        <div class="w-8 h-8 rounded-lg bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-sky-400 text-sm font-bold">
                            ⚙️
                        </div>
                        <div>
                            <h3 class="text-xs font-extrabold tracking-tight text-primary-theme uppercase">Web Radar Settings</h3>
                            <p class="text-[10px] text-secondary-theme">Calibrate Reference Station & Proximity</p>
                        </div>
                    </div>
                    <button id="btn-close-settings" class="text-muted-theme hover:text-primary-theme p-1 rounded-lg text-sm font-mono">
                        ✕
                    </button>
                </div>

                <!-- Modal Body -->
                <div class="p-4 overflow-y-auto space-y-3.5 flex-1 custom-scrollbar">
                    <!-- SECTION 1: REFERENCE LOCATION -->
                    <div class="space-y-2.5 surface-theme p-3 rounded-xl border">
                        <div class="flex items-center justify-between">
                            <label class="text-xs font-bold uppercase tracking-wider text-sky-400 flex items-center gap-1.5">
                                <span>📍 Reference Station</span>
                            </label>
                            <button id="btn-auto-gps" class="btn-action-primary text-[10px] py-1 px-2.5">
                                <span>📡 Auto-Detect GPS</span>
                            </button>
                        </div>

                        <div class="grid grid-cols-2 gap-2">
                            <div>
                                <label class="text-[10px] text-muted-theme uppercase font-mono block mb-1">Latitude</label>
                                <input type="number" step="any" id="setting-lat" value="${settings.referenceLat}" 
                                       class="input-theme w-full p-2 text-xs font-mono">
                            </div>
                            <div>
                                <label class="text-[10px] text-muted-theme uppercase font-mono block mb-1">Longitude</label>
                                <input type="number" step="any" id="setting-lng" value="${settings.referenceLng}" 
                                       class="input-theme w-full p-2 text-xs font-mono">
                            </div>
                        </div>

                        <div class="flex items-center justify-between pt-1">
                            <span class="text-[10px] text-muted-theme">Or pinpoint directly:</span>
                            <button id="btn-pick-map" class="btn-action-secondary text-[10px] py-1 px-2">
                                <span>🎯 Pick On Map</span>
                            </button>
                        </div>
                    </div>

                    <!-- SECTION 2: SEARCH RADIUS CALIBRATION -->
                    <div class="space-y-2.5 surface-theme p-3 rounded-xl border">
                        <div class="flex items-center justify-between">
                            <label class="text-xs font-bold uppercase tracking-wider text-sky-400">
                                🎯 Search Radius
                            </label>
                            <span id="radius-value-display" class="badge-status badge-accent text-[10px]">
                                ${settings.searchRadiusKm} km
                            </span>
                        </div>

                        <input type="range" id="setting-radius-slider" min="5" max="100" step="1" value="${Math.max(5, settings.searchRadiusKm)}"
                               class="w-full h-2 bg-canvas rounded-lg appearance-none cursor-pointer accent-sky-500">

                        <!-- Preset Quick Buttons -->
                        <div class="flex items-center gap-1.5 pt-0.5">
                            <span class="text-[9px] text-muted-theme font-mono uppercase">Presets:</span>
                            <div class="flex items-center gap-1">
                                ${[5, 10, 15, 25, 50, 100].map(r => `
                                    <button type="button" class="preset-radius-btn text-[9px] font-mono px-1.5 py-0.5 rounded border border-subtle-theme ${settings.searchRadiusKm === r ? 'bg-sky-500 text-white font-bold' : 'bg-card-subtle text-secondary-theme hover:bg-card-hover'}" 
                                            data-radius="${r}">
                                        ${r}km
                                    </button>
                                `).join('')}
                            </div>
                        </div>

                        <div class="flex items-center gap-2 pt-1 border-t border-subtle-theme text-xs text-secondary-theme">
                            <input type="checkbox" id="setting-show-circle" ${settings.showRadiusCircle ? 'checked' : ''} class="accent-sky-500 rounded">
                            <label for="setting-show-circle" class="text-[11px]">Display Radial Perimeter Circle on Map</label>
                        </div>
                    </div>

                    <!-- SECTION 3: MAP ENGINE, THEME & REFRESH -->
                    <div class="grid grid-cols-1 sm:grid-cols-3 gap-2 surface-theme p-3 rounded-xl border">
                        <div>
                            <label class="text-[10px] font-bold uppercase tracking-wider text-muted-theme block mb-1">Map Engine</label>
                            <select id="setting-engine" class="input-theme w-full p-1.5 text-xs">
                                <option value="google" ${settings.mapEngine === 'google' ? 'selected' : ''}>Google Maps (Vector)</option>
                                <option value="leaflet" ${settings.mapEngine === 'leaflet' ? 'selected' : ''}>Leaflet / OSM Core</option>
                            </select>
                        </div>

                        <div>
                            <label class="text-[10px] font-bold uppercase tracking-wider text-muted-theme block mb-1">Map Theme</label>
                            <select id="setting-theme" class="input-theme w-full p-1.5 text-xs">
                                ${Object.entries(MapConfig.TILE_PROVIDERS).map(([key, prov]) => `
                                    <option value="${key}" ${settings.mapTileTheme === key ? 'selected' : ''}>${prov.name}</option>
                                `).join('')}
                            </select>
                        </div>

                        <div>
                            <label class="text-[10px] font-bold uppercase tracking-wider text-muted-theme block mb-1">Live Refresh</label>
                            <select id="setting-refresh" class="input-theme w-full p-1.5 text-xs">
                                <option value="3" ${settings.refreshIntervalSec === 3 ? 'selected' : ''}>Every 3s</option>
                                <option value="5" ${settings.refreshIntervalSec === 5 ? 'selected' : ''}>Every 5s (Optimal)</option>
                                <option value="10" ${settings.refreshIntervalSec === 10 ? 'selected' : ''}>Every 10s</option>
                                <option value="30" ${settings.refreshIntervalSec === 30 ? 'selected' : ''}>Every 30s</option>
                                <option value="0" ${settings.refreshIntervalSec === 0 ? 'selected' : ''}>Manual</option>
                            </select>
                        </div>
                    </div>
                </div>

                <!-- Modal Footer -->
                <div class="p-3 border-t border-subtle-theme surface-theme flex items-center justify-end gap-2">
                    <button id="btn-cancel-settings" class="btn-action-secondary text-xs">
                        Cancel
                    </button>
                    <button id="btn-save-settings" class="btn-action-primary text-xs">
                        Apply & Save
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
        this.modalElement = modal;

        this.bindEvents(modal, settings);
    }

    /**
     * Binds events for modal interactions
     * @param {HTMLElement} modal 
     * @param {import('../models/SettingsModel.js').SettingsModel} settings 
     */
    bindEvents(modal, settings) {
        const closeBtn = modal.querySelector('#btn-close-settings');
        const cancelBtn = modal.querySelector('#btn-cancel-settings');
        const saveBtn = modal.querySelector('#btn-save-settings');
        const autoGpsBtn = modal.querySelector('#btn-auto-gps');
        const pickMapBtn = modal.querySelector('#btn-pick-map');
        const radiusSlider = modal.querySelector('#setting-radius-slider');
        const radiusDisplay = modal.querySelector('#radius-value-display');

        // Close Handlers
        const handleClose = () => this.close();
        closeBtn.addEventListener('click', handleClose);
        cancelBtn.addEventListener('click', handleClose);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) handleClose();
        });

        // Radius Slider Drag
        radiusSlider.addEventListener('input', (e) => {
            radiusDisplay.textContent = `${e.target.value} km`;
        });

        // Preset Buttons
        modal.querySelectorAll('.preset-radius-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const val = btn.dataset.radius;
                radiusSlider.value = val;
                radiusDisplay.textContent = `${val} km`;
                modal.querySelectorAll('.preset-radius-btn').forEach(b => {
                    b.classList.remove('bg-sky-500', 'text-white', 'font-bold');
                    b.classList.add('bg-card-subtle', 'text-secondary-theme');
                });
                btn.classList.add('bg-sky-500', 'text-white', 'font-bold');
                btn.classList.remove('bg-card-subtle', 'text-secondary-theme');
            });
        });

        // Auto Detect GPS
        autoGpsBtn.addEventListener('click', async () => {
            autoGpsBtn.innerHTML = `<span>⏳ Detecting...</span>`;
            autoGpsBtn.disabled = true;
            if (this.onAutoDetectGps) {
                try {
                    const coords = await this.onAutoDetectGps();
                    if (coords) {
                        modal.querySelector('#setting-lat').value = coords.latitude;
                        modal.querySelector('#setting-lng').value = coords.longitude;
                        autoGpsBtn.innerHTML = `<span>✅ Acquired</span>`;
                        FeedbackDialog.showToast("GPS position acquired from browser!", "success");
                    }
                } catch (err) {
                    FeedbackDialog.showToast("GPS Notice: " + err.message, "warning");
                    autoGpsBtn.innerHTML = `<span>📡 Auto-Detect GPS</span>`;
                } finally {
                    autoGpsBtn.disabled = false;
                }
            }
        });

        // Pick On Map
        pickMapBtn.addEventListener('click', () => {
            this.close();
            if (this.onPickOnMap) {
                this.onPickOnMap();
                FeedbackDialog.showToast("Click anywhere on the map to set reference station.", "info");
            }
        });

        // Save Settings
        saveBtn.addEventListener('click', () => {
            const lat = parseFloat(modal.querySelector('#setting-lat').value);
            const lng = parseFloat(modal.querySelector('#setting-lng').value);
            const radius = parseFloat(radiusSlider.value);
            const engine = modal.querySelector('#setting-engine').value;
            const theme = modal.querySelector('#setting-theme').value;
            const refresh = parseInt(modal.querySelector('#setting-refresh').value, 10);
            const showCircle = modal.querySelector('#setting-show-circle').checked;

            if (isNaN(lat) || isNaN(lng)) {
                FeedbackDialog.showToast("Invalid reference coordinates.", "error");
                return;
            }

            settings.referenceLat = lat;
            settings.referenceLng = lng;
            settings.searchRadiusKm = radius;
            settings.mapEngine = engine;
            settings.mapTileTheme = theme;
            settings.refreshIntervalSec = refresh;
            settings.showRadiusCircle = showCircle;
            settings.save();

            if (this.onSave) {
                this.onSave(settings);
            }

            this.close();
            FeedbackDialog.showToast("Radar settings applied!", "success");
        });
    }

    close() {
        if (this.modalElement) {
            this.modalElement.remove();
            this.modalElement = null;
        }
    }
}
