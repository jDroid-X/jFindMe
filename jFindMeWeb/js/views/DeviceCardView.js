import { GeoUtils } from '../core/GeoUtils.js';
import { FeedbackDialog } from '../components/FeedbackDialog.js';

/**
 * jFindMe Matrix - Device Visiting Card & Dossier View (Dynamic OOPS Architecture)
 * Generates sleek glassmorphic Visiting Card tooltips on hover and comprehensive full-screen Dossier modals on click.
 * 100% Theme-Adaptive (Dark / Light) with high contrast tokens.
 */
export class DeviceCardView {
    /**
     * Generates HTML string for the Map Tooltip Visiting Card
     * @param {import('../models/DeviceModel.js').DeviceModel} device 
     * @returns {string} HTML markup
     */
    static renderTooltipHtml(device) {
        const themeColor = device.getThemeColor();
        const avatarUrl = device.getAvatarUrl();
        const batteryMeta = device.getBatteryMeta();
        const isWithin = device.isWithinSearchRadius;
        const bearing = Math.round(device.bearing || 0);
        const cardinal = GeoUtils.bearingToCardinal(bearing);

        return `
            <div class="matrix-visiting-card card-theme shadow-2xl p-3.5 rounded-2xl border text-primary-theme w-[280px] select-none font-sans">
                <!-- Header: Avatar, Name & Live Status -->
                <div class="flex items-center gap-3 border-b border-subtle-theme pb-2.5 mb-2.5">
                    <div class="relative shrink-0">
                        <img src="${avatarUrl}" alt="${device.name}" 
                             class="w-10 h-10 rounded-full object-cover border-2 shadow-md bg-transparent" 
                             style="border-color: ${themeColor};"
                             onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                        <span class="absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-canvas ${device.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-500'}"></span>
                    </div>
                    <div class="flex-1 min-w-0">
                        <div class="flex items-center justify-between gap-1">
                            <h4 class="font-bold text-sm text-primary-theme truncate tracking-tight">${device.name}</h4>
                            <span class="badge-status ${device.isOnline ? 'badge-success' : 'badge-danger'} text-[8px]">
                                ${device.isOnline ? 'LIVE' : 'OFF'}
                            </span>
                        </div>
                        <div class="text-[10px] text-secondary-theme truncate flex items-center gap-1 mt-0.5 font-mono">
                            <span class="truncate font-semibold">${device.deviceName || device.model || device.agentId}</span>
                            <span>•</span>
                            <span class="text-sky-400 font-sans font-bold">${device.roleFamily !== 'None' ? device.roleFamily : device.role}</span>
                        </div>
                    </div>
                </div>

                <!-- Distance Radar Badge -->
                <div class="mb-2.5 flex items-center justify-between text-xs px-2.5 py-1.5 rounded-xl ${isWithin ? 'bg-sky-500/15 border border-sky-500/30 text-sky-400' : 'bg-amber-500/15 border border-amber-500/30 text-amber-500'} font-medium">
                    <span class="flex items-center gap-1 text-[11px] truncate">
                        <span>${isWithin ? '🎯 10km Radius' : '📡 Radar Out'}</span>
                    </span>
                    <span class="font-mono font-bold text-xs">${device.getFormattedDistance()}</span>
                </div>

                <!-- Telemetry Grid -->
                <div class="grid grid-cols-2 gap-2 text-xs surface-theme rounded-xl p-2.5 border mb-2.5 font-mono">
                    <div class="flex flex-col">
                        <span class="text-[9px] text-muted-theme uppercase font-sans font-semibold">Battery</span>
                        <span class="font-bold text-xs flex items-center gap-1" style="color: ${batteryMeta.color}">
                            ${batteryMeta.icon} ${batteryMeta.text}
                        </span>
                    </div>
                    <div class="flex flex-col">
                        <span class="text-[9px] text-muted-theme uppercase font-sans font-semibold">Speed</span>
                        <span class="text-primary-theme font-bold text-xs">${device.getFormattedSpeed()}</span>
                    </div>
                    <div class="flex flex-col">
                        <span class="text-[9px] text-muted-theme uppercase font-sans font-semibold">Heading</span>
                        <span class="text-primary-theme font-bold text-xs">${bearing}° (${cardinal})</span>
                    </div>
                    <div class="flex flex-col">
                        <span class="text-[9px] text-muted-theme uppercase font-sans font-semibold">Last Pulse</span>
                        <span class="text-secondary-theme font-bold text-xs">${device.getFormattedLastSeen()}</span>
                    </div>
                </div>

                <!-- Location Address & Date/Time Metadata -->
                <div class="surface-theme rounded-xl p-2.5 mb-2.5 border space-y-1.5">
                    <div class="flex items-center justify-between text-[10px] font-mono text-sky-400 font-bold border-b border-subtle-theme pb-1">
                        <span class="flex items-center gap-1">🕒 <span>${device.getFormattedDateTime()}</span></span>
                    </div>
                    <div class="text-[11px] text-secondary-theme line-clamp-2 pt-0.5 leading-relaxed">
                        <span class="text-sky-400">📍 </span>${device.address || 'No street address registered'}
                    </div>
                </div>

                <!-- Quick Action Buttons -->
                <div class="flex items-center gap-2 pt-1.5 border-t border-subtle-theme">
                    <button class="flex-1 btn-action-primary text-xs"
                            onclick="window.MatrixApp && window.MatrixApp.focusDevice('${device.agentId}')">
                        <span>🎯 Center</span>
                    </button>
                    <button class="btn-action-secondary text-xs"
                            onclick="window.MatrixApp && window.MatrixApp.showHistory('${device.agentId}')">
                        <span>📍 Trail</span>
                    </button>
                    <button class="btn-action-secondary text-xs"
                            onclick="window.MatrixApp && window.MatrixApp.showDossier('${device.agentId}')">
                        <span>🔍 Dossier</span>
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Renders and displays the Fullscreen Detailed Device Dossier Modal
     * @param {import('../models/DeviceModel.js').DeviceModel} device 
     */
    static showDossierModal(device) {
        const existing = document.getElementById('device-dossier-modal');
        if (existing) existing.remove();

        const themeColor = device.getThemeColor();
        const avatarUrl = device.getAvatarUrl();
        const batteryMeta = device.getBatteryMeta();
        const bearing = Math.round(device.bearing || 0);
        const cardinal = GeoUtils.bearingToCardinal(bearing);

        const modal = document.createElement('div');
        modal.id = 'device-dossier-modal';
        modal.className = 'fixed inset-0 z-[1050] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-fade-in font-sans';

        modal.innerHTML = `
            <div class="card-theme border rounded-2xl w-full max-w-3xl shadow-2xl overflow-hidden text-primary-theme flex flex-col max-h-[92vh]">
                <!-- Modal Header -->
                <div class="p-5 border-b border-subtle-theme flex items-center justify-between surface-theme">
                    <div class="flex items-center gap-3.5 min-w-0">
                        <div class="relative shrink-0">
                            <img src="${avatarUrl}" alt="${device.name}"
                                 class="w-14 h-14 rounded-full object-cover border-2 shadow-md bg-transparent"
                                 style="border-color: ${themeColor};"
                                 onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                            <span class="absolute bottom-0 right-0 w-3.5 h-3.5 rounded-full border-2 border-canvas ${device.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-500'}"></span>
                        </div>
                        <div class="min-w-0">
                            <div class="flex items-center gap-2">
                                <h3 class="text-base font-extrabold tracking-tight text-primary-theme uppercase truncate">${device.name}</h3>
                                <span class="badge-status ${device.isOnline ? 'badge-success' : 'badge-danger'} text-[9px] shrink-0">
                                    ${device.sessionState}
                                </span>
                            </div>
                            <p class="text-xs text-secondary-theme font-mono mt-0.5 truncate">
                                AGENT: <span class="text-sky-400 font-bold">${device.agentId}</span> • HARDWARE: <span class="text-muted-theme">${device.deviceId}</span>
                            </p>
                        </div>
                    </div>
                    <div class="flex items-center gap-2 shrink-0">
                        <span class="badge-status badge-accent text-[9px] font-mono shrink-0">📊 READ-ONLY TELEMETRY</span>
                        <button id="btn-close-dossier" class="text-muted-theme hover:text-primary-theme p-1.5 rounded-lg text-lg font-mono">
                            ✕
                        </button>
                    </div>
                </div>

                <!-- Modal Body with Metric & Telemetry Panes -->
                <div class="p-5 overflow-y-auto space-y-4 flex-1 custom-scrollbar">
                    <!-- Top Metric Cards -->
                    <div class="grid grid-cols-2 sm:grid-cols-4 gap-2.5 font-mono">
                        <div class="surface-theme p-3 rounded-xl border">
                            <span class="text-[10px] text-muted-theme uppercase font-sans font-bold">Distance from Station</span>
                            <div class="text-sm font-extrabold text-sky-400 mt-1">${device.getFormattedDistance()}</div>
                            <span class="text-[9px] ${device.isWithinSearchRadius ? 'text-emerald-500' : 'text-amber-500'} font-bold">
                                ${device.isWithinSearchRadius ? 'Within Radar' : 'Outside Radar'}
                            </span>
                        </div>
                        <div class="surface-theme p-3 rounded-xl border">
                            <span class="text-[10px] text-muted-theme uppercase font-sans font-bold">Battery Health</span>
                            <div class="text-sm font-extrabold mt-1" style="color: ${batteryMeta.color}">${batteryMeta.text}</div>
                            <span class="text-[9px] text-muted-theme">${device.isCharging ? '⚡ Charging Active' : 'On Battery'}</span>
                        </div>
                        <div class="surface-theme p-3 rounded-xl border">
                            <span class="text-[10px] text-muted-theme uppercase font-sans font-bold">Speed & Heading</span>
                            <div class="text-sm font-extrabold text-primary-theme mt-1">${device.getFormattedSpeed()}</div>
                            <span class="text-[9px] text-muted-theme">${bearing}° (${cardinal})</span>
                        </div>
                        <div class="surface-theme p-3 rounded-xl border">
                            <span class="text-[10px] text-muted-theme uppercase font-sans font-bold">Network Stream</span>
                            <div class="text-sm font-extrabold text-emerald-500 mt-1">${device.networkType}</div>
                            <span class="text-[9px] text-muted-theme truncate block">${device.getFormattedLastSeen()}</span>
                        </div>
                    </div>

                    <!-- Dossier Information Sections -->
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                        <!-- Profile & Contact Dossier -->
                        <div class="surface-theme p-3.5 rounded-xl border space-y-2">
                            <h4 class="text-xs font-bold text-sky-400 uppercase tracking-wider flex items-center gap-1.5">
                                <span>👤 Identity & Contact Profile</span>
                            </h4>
                            <div class="space-y-1.5 text-xs font-mono">
                                <div class="flex justify-between py-1 border-b border-subtle-theme">
                                    <span class="text-muted-theme">Department / Org:</span>
                                    <span class="text-primary-theme font-medium">${device.department || 'Not Assigned'}</span>
                                </div>
                                <div class="flex justify-between py-1 border-b border-subtle-theme">
                                    <span class="text-muted-theme">Role / Relation:</span>
                                    <span class="text-sky-400 font-bold">${device.roleFamily !== 'None' ? device.roleFamily : device.role}</span>
                                </div>
                                <div class="flex justify-between py-1 border-b border-subtle-theme">
                                    <span class="text-muted-theme">Phone:</span>
                                    <span class="text-primary-theme">${device.phone || 'N/A'}</span>
                                </div>
                                <div class="flex justify-between py-1 border-b border-subtle-theme">
                                    <span class="text-muted-theme">Email:</span>
                                    <span class="text-primary-theme truncate max-w-[180px]">${device.email || 'N/A'}</span>
                                </div>
                                <div class="flex justify-between py-1">
                                    <span class="text-muted-theme">Emergency Note:</span>
                                    <span class="text-amber-500 font-sans font-medium truncate max-w-[180px]">${device.emergencyNote || 'None'}</span>
                                </div>
                            </div>
                        </div>

                        <!-- Spatial & Geodetic Telemetry -->
                        <div class="surface-theme p-3.5 rounded-xl border space-y-2">
                            <h4 class="text-xs font-bold text-sky-400 uppercase tracking-wider flex items-center gap-1.5">
                                <span>🛰️ Spatial & Diagnostics</span>
                            </h4>
                            <div class="space-y-1.5 text-xs font-mono">
                                <div class="flex justify-between py-1 border-b border-subtle-theme">
                                    <span class="text-muted-theme">Coordinates:</span>
                                    <span class="text-primary-theme">${device.getFormattedCoords()}</span>
                                </div>
                                <div class="flex justify-between py-1 border-b border-subtle-theme">
                                    <span class="text-muted-theme">Altitude / Accuracy:</span>
                                    <span class="text-primary-theme">${device.altitude.toFixed(1)}m / ±${device.accuracy.toFixed(1)}m</span>
                                </div>
                                <div class="flex justify-between py-1 border-b border-subtle-theme">
                                    <span class="text-muted-theme">Mock GPS Detected:</span>
                                    <span class="${device.isMockGps ? 'text-rose-500 font-bold' : 'text-emerald-500 font-bold'}">
                                        ${device.isMockGps ? 'YES ⚠️' : 'NO (Authentic)'}
                                    </span>
                                </div>
                                <div class="flex justify-between py-1 border-b border-subtle-theme">
                                    <span class="text-muted-theme">Airplane Mode:</span>
                                    <span class="${device.isAirplaneMode ? 'text-amber-500' : 'text-secondary-theme'}">
                                        ${device.isAirplaneMode ? 'ACTIVE' : 'INACTIVE'}
                                    </span>
                                </div>
                                <div class="flex justify-between py-1">
                                    <span class="text-muted-theme">Safe Zones:</span>
                                    <span class="badge-status badge-accent text-[8px]">${device.safeZonesCount} Zones</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Address Block -->
                    <div class="surface-theme p-3.5 rounded-xl border text-xs space-y-1.5">
                        <div class="flex items-center justify-between font-mono text-[10px] text-sky-400 font-bold border-b border-subtle-theme pb-1">
                            <span class="flex items-center gap-1.5">🕒 <span>Last Telemetry: ${device.getFormattedDateTime()}</span></span>
                            <span class="text-muted-theme font-normal">(${device.getFormattedTimeDelta()})</span>
                        </div>
                        <div>
                            <span class="text-[10px] text-muted-theme font-mono uppercase block mb-0.5">📍 Street Address</span>
                            <div class="text-secondary-theme font-medium">${device.address || 'No physical street address recorded.'}</div>
                        </div>
                    </div>

                    <!-- Interactive Security & Operational ACL Switches -->
                    <div class="surface-theme p-3.5 rounded-xl border text-xs space-y-2 font-sans">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-1">
                            <span class="text-xs font-bold text-sky-400 uppercase tracking-wider">🛡️ Operational ACL Controls</span>
                            <span class="badge-status badge-accent text-[8px]">CLOSED LOOP</span>
                        </div>
                        <div class="grid grid-cols-1 sm:grid-cols-3 gap-2">
                            <div class="flex items-center justify-between p-2 rounded-lg bg-card-subtle border border-subtle-theme">
                                <span class="text-[10px] font-bold text-primary-theme">Live Tracking</span>
                                <label class="switch-toggle switch-sm">
                                    <input type="checkbox" class="dossier-switch-acl" data-agent-id="${device.agentId}" data-flag="acl_live_tracking" ${device.raw?.acl?.acl_live_tracking === 'DENIED' ? '' : 'checked'}>
                                    <span class="switch-slider"></span>
                                </label>
                            </div>
                            <div class="flex items-center justify-between p-2 rounded-lg bg-card-subtle border border-subtle-theme">
                                <span class="text-[10px] font-bold text-primary-theme">Geofence Alerts</span>
                                <label class="switch-toggle switch-sm">
                                    <input type="checkbox" class="dossier-switch-acl" data-agent-id="${device.agentId}" data-flag="acl_geofence" ${device.raw?.acl?.acl_geofence === 'DENIED' ? '' : 'checked'}>
                                    <span class="switch-slider"></span>
                                </label>
                            </div>
                            <div class="flex items-center justify-between p-2 rounded-lg bg-card-subtle border border-subtle-theme">
                                <span class="text-[10px] font-bold text-primary-theme">Route History</span>
                                <label class="switch-toggle switch-sm">
                                    <input type="checkbox" class="dossier-switch-acl" data-agent-id="${device.agentId}" data-flag="acl_history_audit" ${device.raw?.acl?.acl_history_audit === 'DENIED' ? '' : 'checked'}>
                                    <span class="switch-slider"></span>
                                </label>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Footer Actions -->
                <div class="p-3.5 border-t border-subtle-theme surface-theme flex items-center justify-between">
                    <button id="btn-poke-dossier" class="btn-action-secondary text-xs">
                        <span>⚡ Poke / Ping Signal</span>
                    </button>

                    <div class="flex items-center gap-2">
                        <button id="btn-trail-dossier" class="btn-action-secondary text-xs">
                            <span>📍 History Trail</span>
                        </button>
                        <button id="btn-focus-dossier" class="btn-action-primary text-xs">
                            <span>🎯 Center On Map</span>
                        </button>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        const closeBtn = modal.querySelector('#btn-close-dossier');
        const editBtn = modal.querySelector('#btn-edit-from-dossier');
        const pokeBtn = modal.querySelector('#btn-poke-dossier');
        const trailBtn = modal.querySelector('#btn-trail-dossier');
        const focusBtn = modal.querySelector('#btn-focus-dossier');

        const handleClose = () => modal.remove();

        closeBtn.addEventListener('click', handleClose);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) handleClose();
        });

        editBtn?.addEventListener('click', () => {
            modal.remove();
            if (window.MatrixApp && window.MatrixApp.openEditProfile) {
                window.MatrixApp.openEditProfile(device.agentId);
            }
        });

        pokeBtn.addEventListener('click', async () => {
            try {
                const resData = await (window.fetchApi ? window.fetchApi('/api/poke-device', {
                    method: 'POST',
                    body: JSON.stringify({ agentId: device.agentId, pokedBy: 'Web_Dossier_Operator' })
                }) : fetch('/api/poke-device', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ agentId: device.agentId, pokedBy: 'Web_Dossier_Operator' })
                }).then(r => r.json()));

                if (resData && (resData.status === 'SUCCESS' || resData.status === 'POKED')) {
                    FeedbackDialog.showSlideNotification({
                        title: 'Telemetry Poke Dispatched',
                        message: `Pulse signal transmitted to ${device.name} (${device.agentId})`,
                        type: 'success',
                        timeout: 3500
                    });
                } else {
                    FeedbackDialog.showSlideNotification({
                        title: 'Poke Notice',
                        message: resData?.message || 'Signal transmitted',
                        type: 'info',
                        timeout: 3500
                    });
                }
            } catch (err) {
                FeedbackDialog.showSlideNotification({
                    title: 'Poke Dispatch Failed',
                    message: err.message,
                    type: 'danger',
                    timeout: 3500
                });
            }
        });

        // Dossier ACL Tactile Switch Handlers
        modal.querySelectorAll('.dossier-switch-acl').forEach(sw => {
            sw.addEventListener('change', async (e) => {
                const agentId = sw.getAttribute('data-agent-id');
                const flag = sw.getAttribute('data-flag');
                const val = sw.checked;
                try {
                    const res = await (window.fetchApi ? window.fetchApi('/api/device/toggle-acl', {
                        method: 'POST',
                        body: JSON.stringify({ agentId, flag, value: val })
                    }) : fetch('/api/device/toggle-acl', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ agentId, flag, value: val })
                    }).then(r => r.json()));

                    if (res && res.error) throw new Error(res.error || 'Failed to update ACL flag');
                    FeedbackDialog.showSlideNotification({
                        title: 'ACL Grant Updated',
                        message: `Flag [${flag}] set to ${val ? 'GRANTED' : 'DENIED'} for [${agentId}]`,
                        type: val ? 'success' : 'warning',
                        timeout: 3500
                    });
                } catch (err) {
                    FeedbackDialog.showSlideNotification({
                        title: 'ACL Update Error',
                        message: err.message,
                        type: 'danger',
                        timeout: 3500
                    });
                    sw.checked = !val;
                }
            });
        });

        trailBtn.addEventListener('click', () => {
            modal.remove();
            if (window.MatrixApp && window.MatrixApp.showHistory) {
                window.MatrixApp.showHistory(device.agentId);
            }
        });

        focusBtn.addEventListener('click', () => {
            modal.remove();
            if (window.MatrixApp && window.MatrixApp.focusDevice) {
                window.MatrixApp.focusDevice(device.agentId);
            }
        });
    }
}
