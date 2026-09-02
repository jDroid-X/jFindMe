import { FeedbackDialog } from '../components/FeedbackDialog.js';

/**
 * jFindMe Matrix - Safe Zone Geofence Creator Modal View (Dynamic OOPS Architecture)
 * Interactive dialog allowing admins to create geofences, calibrate radius (50m - 5000m),
 * name zones, assign devices, and save atomically to Firebase Geofencing.
 * 100% Theme-Adaptive (Dark / Light) with coordinate bounds validation and feedback toasts.
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
        modal.className = 'fixed inset-0 z-[1100] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-fade-in font-sans';

        modal.innerHTML = `
            <div class="card-theme border rounded-2xl w-full max-w-xl shadow-2xl overflow-hidden text-primary-theme flex flex-col max-h-[90vh]">
                <!-- Header -->
                <div class="p-4 border-b border-subtle-theme flex items-center justify-between surface-theme">
                    <div class="flex items-center gap-3">
                        <div class="w-9 h-9 rounded-xl bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center text-emerald-500 text-base font-bold">
                            🛡️
                        </div>
                        <div>
                            <h3 class="text-sm font-extrabold tracking-tight text-primary-theme uppercase">Create Safe Zone Geofence</h3>
                            <p class="text-[11px] text-secondary-theme">Establish Perimeter Bounds for Monitored Fleet</p>
                        </div>
                    </div>
                    <button id="btn-close-sz" class="text-muted-theme hover:text-primary-theme p-1.5 rounded-lg text-lg font-mono">
                        ✕
                    </button>
                </div>

                <!-- Body -->
                <div class="p-5 overflow-y-auto space-y-4 flex-1 custom-scrollbar">
                    <div id="sz-error-box" class="hidden p-2.5 rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-500 text-xs font-mono font-medium"></div>

                    <!-- Zone Name -->
                    <div>
                        <label class="text-[11px] font-bold uppercase tracking-wider text-secondary-theme block mb-1">Zone Perimeter Name *</label>
                        <input type="text" id="sz-name" placeholder="e.g. Home Perimeter, Office HQ, School Campus" 
                               class="input-theme w-full p-2 text-xs font-medium">
                    </div>

                    <!-- Coordinates -->
                    <div class="grid grid-cols-2 gap-3 surface-theme p-3 rounded-xl border">
                        <div>
                            <label class="text-[10px] text-muted-theme uppercase font-mono block mb-1">Center Latitude (-90 to 90)</label>
                            <input type="number" step="any" id="sz-lat" value="${defaultLat}" 
                                   class="input-theme w-full p-2 text-xs font-mono">
                        </div>
                        <div>
                            <label class="text-[10px] text-muted-theme uppercase font-mono block mb-1">Center Longitude (-180 to 180)</label>
                            <input type="number" step="any" id="sz-lng" value="${defaultLng}" 
                                   class="input-theme w-full p-2 text-xs font-mono">
                        </div>
                    </div>

                    <!-- Radius Slider (50m to 5000m) -->
                    <div class="space-y-2 surface-theme p-3 rounded-xl border">
                        <div class="flex items-center justify-between">
                            <label class="text-[11px] font-bold uppercase tracking-wider text-emerald-500">Perimeter Radius</label>
                            <span id="sz-radius-display" class="badge-status badge-success text-[10px]">
                                150 meters
                            </span>
                        </div>
                        <input type="range" id="sz-radius-slider" min="50" max="5000" step="25" value="150"
                               class="w-full h-2 bg-canvas rounded-lg appearance-none cursor-pointer accent-emerald-500">
                        <div class="flex justify-between text-[9px] text-muted-theme font-mono">
                            <span>50m (Tight)</span>
                            <span>1000m (Campus)</span>
                            <span>5000m (District)</span>
                        </div>
                    </div>

                    <!-- Assign Connected Devices -->
                    <div class="space-y-1.5">
                        <label class="text-[11px] font-bold uppercase tracking-wider text-secondary-theme block">Assign Tracked Devices</label>
                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-36 overflow-y-auto custom-scrollbar p-2 surface-theme rounded-xl border">
                            ${devices.map(d => `
                                <label class="flex items-center gap-2 p-2 rounded-lg bg-card-subtle hover:bg-card-hover cursor-pointer border border-subtle-theme">
                                    <input type="checkbox" class="sz-device-checkbox accent-emerald-500 rounded" value="${d.agentId}" checked>
                                    <span class="text-xs font-bold text-primary-theme truncate">${d.name}</span>
                                    <span class="text-[10px] font-mono text-muted-theme ml-auto">${d.agentId}</span>
                                </label>
                            `).join('')}
                        </div>
                    </div>
                </div>

                <!-- Footer -->
                <div class="p-3.5 border-t border-subtle-theme surface-theme flex items-center justify-end gap-2">
                    <button id="btn-cancel-sz" class="btn-action-secondary text-xs">
                        Cancel
                    </button>
                    <button id="btn-save-sz" class="btn-action-primary text-xs">
                        <span>Establish Safe Zone</span>
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

            if (isNaN(lat) || lat < -90 || lat > 90) {
                errorBox.textContent = "Validation Error: Latitude must be between -90 and 90 degrees.";
                errorBox.classList.remove('hidden');
                return;
            }

            if (isNaN(lng) || lng < -180 || lng > 180) {
                errorBox.textContent = "Validation Error: Longitude must be between -180 and 180 degrees.";
                errorBox.classList.remove('hidden');
                return;
            }

            if (isNaN(radius) || radius < 50 || radius > 5000) {
                errorBox.textContent = "Validation Error: Radius must be between 50 and 5000 meters.";
                errorBox.classList.remove('hidden');
                return;
            }

            errorBox.classList.add('hidden');
            saveBtn.disabled = true;
            saveBtn.innerHTML = `<span>⏳ Establishing...</span>`;

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
                    FeedbackDialog.showToast(`Safe Zone [${name}] established successfully!`, "success");
                } catch (err) {
                    errorBox.textContent = `Save Error: ${err.message}`;
                    errorBox.classList.remove('hidden');
                    saveBtn.disabled = false;
                    saveBtn.innerHTML = `<span>Establish Safe Zone</span>`;
                    FeedbackDialog.showToast(`Failed to create safe zone: ${err.message}`, "error");
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
