/**
 * jFindMe Matrix - Safe Zone Geofence Creator Modal View (OOPS Architecture)
 * Interactive dialog allowing admins to create geofences, calibrate radius (50m - 5000m),
 * name zones, assign devices, and save atomically to Firebase Geofencing.
 */
export class SafeZoneCreateModalView {
    constructor(options = {}) {
        this.onSave = options.onSave || null;
        this.onPickCenter = options.onPickCenter || null;
        this.modalElement = null;
    }

    /**
     * Shows the Safe Zone Creator Modal
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices 
     * @param {number} defaultLat 
     * @param {number} defaultLng 
     */
    show(devices = [], defaultLat = 28.6139, defaultLng = 77.2090) {
        this.close();

        const modal = document.createElement('div');
        modal.id = 'safezone-create-modal';
        modal.className = 'fixed inset-0 z-[1100] flex items-center justify-center bg-black/85 backdrop-blur-md p-4 animate-fade-in font-sans';

        modal.innerHTML = `
            <div class="bg-slate-900 border border-white/10 rounded-2xl w-full max-w-xl shadow-2xl overflow-hidden text-white flex flex-col max-h-[90vh]">
                <!-- Header -->
                <div class="p-5 border-b border-white/10 flex items-center justify-between bg-slate-950/80">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400 text-lg">
                            🛡️
                        </div>
                        <div>
                            <h3 class="text-base font-black tracking-tight text-white uppercase">Create Safe Zone Geofence</h3>
                            <p class="text-xs text-slate-400">Establish Geofence Perimeter for Tracked Devices</p>
                        </div>
                    </div>
                    <button id="btn-close-sz" class="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-white/5 transition-all text-xl font-mono">
                        ✕
                    </button>
                </div>

                <!-- Body -->
                <div class="p-6 overflow-y-auto space-y-5 flex-1 custom-scrollbar">
                    <div id="sz-error-box" class="hidden p-3 rounded-lg bg-red-950/80 border border-red-500/50 text-red-200 text-xs font-mono"></div>

                    <!-- Zone Name -->
                    <div>
                        <label class="text-[11px] font-bold uppercase tracking-wider text-slate-300 block mb-1">Zone Perimeter Name *</label>
                        <input type="text" id="sz-name" placeholder="e.g. Home Perimeter, School Zone, Office HQ" 
                               class="w-full bg-slate-950 border border-white/10 rounded-lg p-2.5 text-xs text-white focus:border-emerald-500 focus:outline-none font-medium">
                    </div>

                    <!-- Coordinates & Pinpoint -->
                    <div class="grid grid-cols-2 gap-3 bg-black/40 p-3.5 rounded-xl border border-white/5">
                        <div>
                            <label class="text-[10px] text-slate-400 uppercase font-mono block mb-1">Center Latitude</label>
                            <input type="number" step="any" id="sz-lat" value="${defaultLat}" 
                                   class="w-full bg-slate-950 border border-white/10 rounded-lg p-2 text-xs text-white font-mono focus:border-emerald-500 focus:outline-none">
                        </div>
                        <div>
                            <label class="text-[10px] text-slate-400 uppercase font-mono block mb-1">Center Longitude</label>
                            <input type="number" step="any" id="sz-lng" value="${defaultLng}" 
                                   class="w-full bg-slate-950 border border-white/10 rounded-lg p-2 text-xs text-white font-mono focus:border-emerald-500 focus:outline-none">
                        </div>
                    </div>

                    <!-- Radius Slider (50m to 5000m) -->
                    <div class="space-y-2 bg-black/40 p-3.5 rounded-xl border border-white/5">
                        <div class="flex items-center justify-between">
                            <label class="text-[11px] font-bold uppercase tracking-wider text-emerald-400">Geofence Radius</label>
                            <span id="sz-radius-display" class="text-xs font-mono font-bold text-emerald-300 bg-emerald-500/20 px-2 py-0.5 rounded border border-emerald-500/30">
                                150 meters
                            </span>
                        </div>
                        <input type="range" id="sz-radius-slider" min="50" max="5000" step="25" value="150"
                               class="w-full h-2 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-emerald-400">
                    </div>

                    <!-- Assign Connected Devices -->
                    <div class="space-y-2">
                        <label class="text-[11px] font-bold uppercase tracking-wider text-slate-300 block">Assign Devices to Zone</label>
                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-36 overflow-y-auto custom-scrollbar p-2 bg-black/40 rounded-xl border border-white/5">
                            ${devices.map(d => `
                                <label class="flex items-center gap-2 p-2 rounded-lg bg-slate-950 hover:bg-slate-900 cursor-pointer border border-white/5">
                                    <input type="checkbox" class="sz-device-checkbox accent-emerald-400" value="${d.agentId}" checked>
                                    <span class="text-xs font-bold truncate">${d.name}</span>
                                    <span class="text-[10px] font-mono text-slate-500 ml-auto">${d.agentId}</span>
                                </label>
                            `).join('')}
                        </div>
                    </div>
                </div>

                <!-- Footer -->
                <div class="p-4 border-t border-white/10 bg-slate-950 flex items-center justify-end gap-3">
                    <button id="btn-cancel-sz" class="px-4 py-2 rounded-xl text-xs font-bold text-slate-400 hover:text-white hover:bg-white/5 transition-all">
                        Cancel
                    </button>
                    <button id="btn-save-sz" class="px-6 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-black uppercase tracking-wider transition-all shadow-lg shadow-emerald-600/30 flex items-center gap-1.5">
                        <span>Save Safe Zone</span>
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
        this.modalElement = modal;

        this.bindEvents(modal);
    }

    bindEvents(modal) {
        const closeBtn = modal.querySelector('#btn-close-sz');
        const cancelBtn = modal.querySelector('#btn-cancel-sz');
        const saveBtn = modal.querySelector('#btn-save-sz');
        const radiusSlider = modal.querySelector('#sz-radius-slider');
        const radiusDisplay = modal.querySelector('#sz-radius-display');
        const errorBox = modal.querySelector('#sz-error-box');

        const handleClose = () => this.close();
        closeBtn.addEventListener('click', handleClose);
        cancelBtn.addEventListener('click', handleClose);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) handleClose();
        });

        radiusSlider.addEventListener('input', (e) => {
            radiusDisplay.textContent = `${e.target.value} meters`;
        });

        saveBtn.addEventListener('click', async () => {
            const name = modal.querySelector('#sz-name').value.trim();
            const lat = parseFloat(modal.querySelector('#sz-lat').value);
            const lng = parseFloat(modal.querySelector('#sz-lng').value);
            const radius = parseFloat(radiusSlider.value);

            const selectedDeviceIds = Array.from(modal.querySelectorAll('.sz-device-checkbox:checked')).map(cb => cb.value);

            if (!name) {
                errorBox.textContent = "Validation Error: Please provide a Safe Zone Name.";
                errorBox.classList.remove('hidden');
                return;
            }

            if (isNaN(lat) || isNaN(lng) || (lat === 0 && lng === 0)) {
                errorBox.textContent = "Validation Error: Please provide valid Center Latitude and Longitude.";
                errorBox.classList.remove('hidden');
                return;
            }

            errorBox.classList.add('hidden');
            saveBtn.disabled = true;
            saveBtn.innerHTML = `<span>⏳ Saving...</span>`;

            const zoneId = `ZONE_${Date.now()}`;
            const payload = {
                zoneId,
                zoneName: name,
                latitude: lat,
                longitude: lng,
                radius: radius,
                isActive: true,
                alertsEnabled: true,
                connectedDevices: selectedDeviceIds
            };

            if (this.onSave) {
                try {
                    await this.onSave(payload);
                    this.close();
                } catch (err) {
                    errorBox.textContent = `Save Error: ${err.message}`;
                    errorBox.classList.remove('hidden');
                    saveBtn.disabled = false;
                    saveBtn.innerHTML = `<span>Save Safe Zone</span>`;
                }
            }
        });
    }

    close() {
        if (this.modalElement) {
            this.modalElement.remove();
            this.modalElement = null;
        }
    }
}
