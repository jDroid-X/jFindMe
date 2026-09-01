import { MapConfig } from '../config/MapConfig.js';

/**
 * jFindMe Matrix - Web Settings View (OOPS Architecture)
 * Provides comprehensive configuration for Reference Location, Search Radius (10 km default),
 * Map Themes, Geolocation Auto-Detection, and Refresh Rates.
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
        modal.className = 'fixed inset-0 z-[1000] flex items-center justify-center bg-black/80 backdrop-blur-md p-4 animate-fade-in font-sans';
        
        modal.innerHTML = `
            <div class="bg-slate-900 border border-white/10 rounded-2xl w-full max-w-sm shadow-2xl overflow-hidden text-white flex flex-col max-h-[90vh]">
                <!-- Modal Header -->
                <div class="p-3.5 border-b border-white/10 flex items-center justify-between bg-slate-950/60">
                    <div class="flex items-center gap-2.5">
                        <div class="w-8 h-8 rounded-lg bg-sky-500/20 border border-sky-500/30 flex items-center justify-center text-sky-400 text-sm">
                            ⚙️
                        </div>
                        <div>
                            <h3 class="text-xs font-black tracking-tight text-white uppercase">Web Radar Settings</h3>
                            <p class="text-[10px] text-slate-400">Calibrate Reference Station & Proximity</p>
                        </div>
                    </div>
                    <button id="btn-close-settings" class="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-white/5 transition-all text-sm font-mono">
                        ✕
                    </button>
                </div>

                <!-- Modal Body -->
                <div class="p-4 overflow-y-auto space-y-4 flex-1 custom-scrollbar">
                    <!-- SECTION 1: REFERENCE LOCATION -->
                    <div class="space-y-3 bg-black/30 p-4 rounded-xl border border-white/5">
                        <div class="flex items-center justify-between">
                            <label class="text-xs font-bold uppercase tracking-wider text-sky-400 flex items-center gap-1.5">
                                <span>📍 Reference Station (Your Location)</span>
                            </label>
                            <button id="btn-auto-gps" class="text-[11px] font-bold px-2.5 py-1 rounded-lg bg-sky-600 hover:bg-sky-500 text-white transition-all flex items-center gap-1 shadow-md shadow-sky-600/20">
                                <span>📡 Auto-Detect Browser GPS</span>
                            </button>
                        </div>

                        <div class="grid grid-cols-2 gap-3">
                            <div>
                                <label class="text-[10px] text-slate-400 uppercase font-mono block mb-1">Latitude</label>
                                <input type="number" step="any" id="setting-lat" value="${settings.referenceLat}" 
                                       class="w-full bg-slate-950 border border-white/10 rounded-lg p-2.5 text-xs text-white font-mono focus:border-sky-500 focus:outline-none">
                            </div>
                            <div>
                                <label class="text-[10px] text-slate-400 uppercase font-mono block mb-1">Longitude</label>
                                <input type="number" step="any" id="setting-lng" value="${settings.referenceLng}" 
                                       class="w-full bg-slate-950 border border-white/10 rounded-lg p-2.5 text-xs text-white font-mono focus:border-sky-500 focus:outline-none">
                            </div>
                        </div>

                        <div class="flex items-center justify-between pt-1">
                            <span class="text-[11px] text-slate-400">Or pinpoint directly on the live map:</span>
                            <button id="btn-pick-map" class="text-[11px] font-medium px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-white/10 transition-all flex items-center gap-1">
                                <span>🎯 Pick Location On Map</span>
                            </button>
                        </div>
                    </div>

                    <!-- SECTION 2: SEARCH RADIUS CALIBRATION -->
                    <div class="space-y-3 bg-black/30 p-4 rounded-xl border border-white/5">
                        <div class="flex items-center justify-between">
                            <label class="text-xs font-bold uppercase tracking-wider text-sky-400">
                                🎯 Radar Search Radius
                            </label>
                            <span id="radius-value-display" class="text-sm font-mono font-black text-sky-300 bg-sky-500/20 px-2.5 py-0.5 rounded-lg border border-sky-500/30">
                                ${settings.searchRadiusKm} km
                            </span>
                        </div>

                        <input type="range" id="setting-radius-slider" min="5" max="100" step="1" value="${Math.max(5, settings.searchRadiusKm)}"
                               class="w-full h-2 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-sky-400">

                        <!-- Preset Quick Buttons -->
                        <div class="flex items-center gap-2 pt-1">
                            <span class="text-[10px] text-slate-400 font-mono uppercase">Presets:</span>
                            <div class="flex items-center gap-1.5">
                                ${[5, 10, 15, 25, 50, 100].map(r => `
                                    <button type="button" class="preset-radius-btn text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 hover:bg-sky-600 text-slate-300 hover:text-white transition-all ${settings.searchRadiusKm === r ? 'bg-sky-600 text-white font-bold' : ''}" 
                                            data-radius="${r}">
                                        ${r}km
                                    </button>
                                `).join('')}
                            </div>
                        </div>

                        <div class="flex items-center gap-2 pt-2 border-t border-white/5 text-xs text-slate-300">
                            <input type="checkbox" id="setting-show-circle" ${settings.showRadiusCircle ? 'checked' : ''} class="accent-sky-400 rounded">
                            <label for="setting-show-circle">Display Glowing Radial Perimeter Circle on Map</label>
                        </div>
                    </div>

                    <!-- SECTION 3: MAP ENGINE, THEME & REFRESH -->
                    <div class="grid grid-cols-3 gap-3 bg-black/30 p-4 rounded-xl border border-white/5">
                        <div>
                            <label class="text-[11px] font-bold uppercase tracking-wider text-slate-300 block mb-2">Map Engine</label>
                            <select id="setting-engine" class="w-full bg-slate-950 border border-white/10 rounded-lg p-2.5 text-xs text-white focus:border-sky-500 focus:outline-none">
                                <option value="google" ${settings.mapEngine === 'google' ? 'selected' : ''}>jButterfly Tactical Vector</option>
                                <option value="leaflet" ${settings.mapEngine === 'leaflet' ? 'selected' : ''}>Leaflet / OSM Core</option>
                            </select>
                        </div>

                        <div>
                            <label class="text-[11px] font-bold uppercase tracking-wider text-slate-300 block mb-2">Map Theme</label>
                            <select id="setting-theme" class="w-full bg-slate-950 border border-white/10 rounded-lg p-2.5 text-xs text-white focus:border-sky-500 focus:outline-none">
                                ${Object.entries(MapConfig.TILE_PROVIDERS).map(([key, prov]) => `
                                    <option value="${key}" ${settings.mapTileTheme === key ? 'selected' : ''}>${prov.name}</option>
                                `).join('')}
                            </select>
                        </div>

                        <div>
                            <label class="text-[11px] font-bold uppercase tracking-wider text-slate-300 block mb-2">Live Refresh</label>
                            <select id="setting-refresh" class="w-full bg-slate-950 border border-white/10 rounded-lg p-2.5 text-xs text-white focus:border-sky-500 focus:outline-none">
                                <option value="3" ${settings.refreshIntervalSec === 3 ? 'selected' : ''}>Every 3s (Ultra)</option>
                                <option value="5" ${settings.refreshIntervalSec === 5 ? 'selected' : ''}>Every 5s (Recommended)</option>
                                <option value="10" ${settings.refreshIntervalSec === 10 ? 'selected' : ''}>Every 10s</option>
                                <option value="30" ${settings.refreshIntervalSec === 30 ? 'selected' : ''}>Every 30s</option>
                                <option value="0" ${settings.refreshIntervalSec === 0 ? 'selected' : ''}>Manual Only</option>
                            </select>
                        </div>
                    </div>
                </div>

                <!-- Modal Footer -->
                <div class="p-4 border-t border-white/10 bg-slate-950 flex items-center justify-end gap-3">
                    <button id="btn-cancel-settings" class="px-4 py-2 rounded-xl text-xs font-bold text-slate-400 hover:text-white hover:bg-white/5 transition-all">
                        Cancel
                    </button>
                    <button id="btn-save-settings" class="px-6 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-black uppercase tracking-wider transition-all shadow-lg shadow-sky-600/30">
                        Apply & Save Settings
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
                modal.querySelectorAll('.preset-radius-btn').forEach(b => b.classList.remove('bg-sky-600', 'text-white', 'font-bold'));
                btn.classList.add('bg-sky-600', 'text-white', 'font-bold');
            });
        });

        // Auto Detect GPS
        autoGpsBtn.addEventListener('click', async () => {
            autoGpsBtn.innerHTML = `<span>⏳ Detecting GPS...</span>`;
            autoGpsBtn.disabled = true;
            if (this.onAutoDetectGps) {
                try {
                    const coords = await this.onAutoDetectGps();
                    if (coords) {
                        modal.querySelector('#setting-lat').value = coords.latitude;
                        modal.querySelector('#setting-lng').value = coords.longitude;
                        autoGpsBtn.innerHTML = `<span>✅ Location Acquired</span>`;
                    }
                } catch (err) {
                    alert("Unable to retrieve browser GPS location: " + err.message);
                    autoGpsBtn.innerHTML = `<span>📡 Auto-Detect Browser GPS</span>`;
                } finally {
                    autoGpsBtn.disabled = false;
                }
            }
        });

        // Pick on Map
        pickMapBtn.addEventListener('click', () => {
            this.close();
            if (this.onPickOnMap) this.onPickOnMap();
        });

        // Save Settings
        saveBtn.addEventListener('click', () => {
            const newLat = parseFloat(modal.querySelector('#setting-lat').value);
            const newLng = parseFloat(modal.querySelector('#setting-lng').value);
            const newRadius = parseFloat(radiusSlider.value);
            const newEngine = modal.querySelector('#setting-engine').value;
            const newTheme = modal.querySelector('#setting-theme').value;
            const newRefresh = parseInt(modal.querySelector('#setting-refresh').value);
            const newShowCircle = modal.querySelector('#setting-show-circle').checked;

            if (isNaN(newLat) || isNaN(newLng)) {
                alert("Please specify valid latitude and longitude values.");
                return;
            }

            settings.referenceLat = newLat;
            settings.referenceLng = newLng;
            settings.searchRadiusKm = newRadius;
            settings.mapEngine = newEngine;
            settings.mapTileTheme = newTheme;
            settings.refreshIntervalSec = newRefresh;
            settings.showRadiusCircle = newShowCircle;
            settings.save();

            if (this.onSave) this.onSave(settings);
            this.close();
        });
    }

    /**
     * Closes and cleans up the modal
     */
    close() {
        if (this.modalElement) {
            this.modalElement.remove();
            this.modalElement = null;
        }
    }
}
