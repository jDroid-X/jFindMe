import { GeoUtils } from '../core/GeoUtils.js';

/**
 * jFindMe Matrix - Device Visiting Card & Dossier View (OOPS Architecture)
 * Generates sleek glassmorphic Visiting Card tooltips on hover and comprehensive full-screen Dossier modals on click.
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
            <div class="matrix-visiting-card glass-panel shadow-xl p-2.5 rounded-xl border border-white/10 text-white w-[210px] select-none font-sans">
                <!-- Header: Avatar, Name & Live Status -->
                <div class="flex items-center gap-2 border-b border-white/10 pb-2 mb-2">
                    <div class="relative shrink-0">
                        <img src="${avatarUrl}" alt="${device.name}" 
                             class="w-8 h-8 rounded-full object-cover border shadow bg-transparent" 
                             style="border-color: ${themeColor};"
                             onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                        <span class="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full border border-slate-900 ${device.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-500'}"></span>
                    </div>
                    <div class="flex-1 min-w-0">
                        <div class="flex items-center justify-between gap-1">
                            <h4 class="font-bold text-xs text-white truncate tracking-tight">${device.name}</h4>
                            <span class="text-[7px] font-mono px-1 py-0.2 rounded uppercase font-bold tracking-wider ${device.isOnline ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-slate-500/20 text-slate-400 border border-slate-500/30'}">
                                ${device.isOnline ? 'LIVE' : 'OFF'}
                            </span>
                        </div>
                        <div class="text-[8px] text-slate-400 truncate flex items-center gap-1 mt-0.5 font-mono">
                            <span class="truncate">${device.deviceName || device.model || device.agentId}</span>
                            <span>•</span>
                            <span class="text-sky-400 font-sans font-bold">${device.roleFamily !== 'None' ? device.roleFamily : device.role}</span>
                        </div>
                    </div>
                </div>

                <!-- Distance Radar Badge -->
                <div class="mb-2 flex items-center justify-between text-[10px] px-2 py-1 rounded-lg ${isWithin ? 'bg-sky-500/15 border border-sky-500/30 text-sky-300' : 'bg-amber-500/10 border border-amber-500/20 text-amber-300'}">
                    <span class="flex items-center gap-1 font-medium text-[9px] truncate">
                        <span>${isWithin ? '🎯 10km Zone' : '📡 Radar'}</span>
                    </span>
                    <span class="font-mono font-bold text-[10px]">${device.getFormattedDistance()}</span>
                </div>

                <!-- Telemetry Grid -->
                <div class="grid grid-cols-2 gap-1.5 text-[9px] bg-black/40 rounded-lg p-2 border border-white/5 mb-2 font-mono">
                    <div class="flex flex-col">
                        <span class="text-[7px] text-slate-400 uppercase tracking-wider font-sans">Battery</span>
                        <span class="font-bold flex items-center gap-0.5" style="color: ${batteryMeta.color}">
                            ${batteryMeta.icon} ${batteryMeta.text}
                        </span>
                    </div>
                    <div class="flex flex-col">
                        <span class="text-[7px] text-slate-400 uppercase tracking-wider font-sans">Speed</span>
                        <span class="text-white font-bold">${device.getFormattedSpeed()}</span>
                    </div>
                    <div class="flex flex-col">
                        <span class="text-[7px] text-slate-400 uppercase tracking-wider font-sans">Heading</span>
                        <span class="text-white font-bold">${bearing}° (${cardinal})</span>
                    </div>
                    <div class="flex flex-col">
                        <span class="text-[9px] text-slate-400 uppercase tracking-wider font-sans">Last Seen</span>
                        <span class="text-slate-300">${device.getFormattedLastSeen()}</span>
                    </div>
                </div>

                <!-- Location Address & Date/Time Metadata -->
                <div class="bg-black/30 rounded-lg p-2 mb-2 border border-white/5 space-y-1">
                    <div class="flex items-center justify-between text-[8px] font-mono text-sky-400 font-bold border-b border-white/5 pb-1">
                        <span class="flex items-center gap-1">🕒 <span>${device.getFormattedDateTime()}</span></span>
                        <span class="text-slate-400 font-normal">${device.getFormattedTimeDelta()}</span>
                    </div>
                    <div class="text-[9px] text-slate-300 line-clamp-2 pt-0.5">
                        <span class="text-slate-400">📍 </span>${device.address || 'No street address registered'}
                    </div>
                </div>

                <!-- Quick Action Buttons -->
                <div class="flex items-center gap-1.5 pt-1 border-t border-white/10">
                    <button class="flex-1 py-1.5 px-2 rounded-lg bg-sky-600 hover:bg-sky-500 text-white font-bold text-[11px] transition-all flex items-center justify-center gap-1 shadow-lg shadow-sky-600/20"
                            onclick="window.MatrixApp.focusDevice('${device.agentId}')">
                        <span>🎯 Center</span>
                    </button>
                    <button class="py-1.5 px-2.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-[11px] font-medium border border-white/10 transition-all flex items-center justify-center gap-1"
                            onclick="window.MatrixApp.showHistory('${device.agentId}')">
                        <span>📍 Trail</span>
                    </button>
                    <button class="py-1.5 px-2.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-[11px] font-medium border border-white/10 transition-all flex items-center justify-center gap-1"
                            onclick="window.MatrixApp.showDossier('${device.agentId}')">
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
        modal.className = 'fixed inset-0 z-[1050] flex items-center justify-center bg-black/85 backdrop-blur-md p-4 animate-fade-in font-sans';

        modal.innerHTML = `
            <div class="bg-slate-900 border border-white/10 rounded-2xl w-full max-w-3xl shadow-2xl overflow-hidden text-white flex flex-col max-h-[92vh]">
                <!-- Modal Header -->
                <div class="p-6 border-b border-white/10 flex items-center justify-between bg-slate-950/80">
                    <div class="flex items-center gap-4">
                        <div class="relative">
                            <img src="${avatarUrl}" alt="${device.name}"
                                 class="w-16 h-16 rounded-full object-cover border-2 shadow-xl bg-transparent"
                                 style="border-color: ${themeColor};"
                                 onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                            <span class="absolute bottom-0 right-0 w-4 h-4 rounded-full border-2 border-slate-900 ${device.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-500'}"></span>
                        </div>
                        <div>
                            <div class="flex items-center gap-2">
                                <h3 class="text-xl font-black tracking-tight text-white uppercase">${device.name}</h3>
                                <span class="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold uppercase tracking-wider ${device.isOnline ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-slate-500/20 text-slate-400 border border-slate-500/30'}">
                                    ${device.sessionState}
                                </span>
                            </div>
                            <p class="text-xs text-slate-400 font-mono mt-0.5">AGENT: <span class="text-sky-400">${device.agentId}</span> • HARDWARE: <span class="text-white/60">${device.deviceId}</span></p>
                        </div>
                    </div>
                    <div class="flex items-center gap-2">
                        <button id="btn-edit-from-dossier" class="px-3.5 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold transition-all shadow-md flex items-center gap-1.5">
                            <span>✏️ Edit Profile</span>
                        </button>
                        <button id="btn-close-dossier" class="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-white/5 transition-all text-xl font-mono">
                            ✕
                        </button>
                    </div>
                </div>

                <!-- Modal Body with Tabs -->
                <div class="p-6 overflow-y-auto space-y-6 flex-1 custom-scrollbar">
                    <!-- Top Metric Cards -->
                    <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 font-mono">
                        <div class="bg-black/40 p-3.5 rounded-xl border border-white/5">
                            <span class="text-[10px] text-slate-400 uppercase font-sans">Distance from Station</span>
                            <div class="text-base font-black text-sky-400 mt-1">${device.getFormattedDistance()}</div>
                            <span class="text-[9px] ${device.isWithinSearchRadius ? 'text-emerald-400' : 'text-amber-400'}">${device.isWithinSearchRadius ? 'Within Radar' : 'Outside Radar'}</span>
                        </div>
                        <div class="bg-black/40 p-3.5 rounded-xl border border-white/5">
                            <span class="text-[10px] text-slate-400 uppercase font-sans">Battery Health</span>
                            <div class="text-base font-black mt-1" style="color: ${batteryMeta.color}">${batteryMeta.text}</div>
                            <span class="text-[9px] text-slate-400">${device.isCharging ? '⚡ Charging Active' : 'On Battery'}</span>
                        </div>
                        <div class="bg-black/40 p-3.5 rounded-xl border border-white/5">
                            <span class="text-[10px] text-slate-400 uppercase font-sans">Speed & Heading</span>
                            <div class="text-base font-black text-white mt-1">${device.getFormattedSpeed()}</div>
                            <span class="text-[9px] text-slate-400">${bearing}° (${cardinal})</span>
                        </div>
                        <div class="bg-black/40 p-3.5 rounded-xl border border-white/5">
                            <span class="text-[10px] text-slate-400 uppercase font-sans">Network Stream</span>
                            <div class="text-base font-black text-emerald-400 mt-1">${device.networkType}</div>
                            <span class="text-[9px] text-slate-400">${device.getFormattedLastSeen()}</span>
                        </div>
                    </div>

                    <!-- Dossier Information Sections -->
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <!-- Profile & Contact Dossier -->
                        <div class="bg-black/30 p-4 rounded-xl border border-white/5 space-y-3">
                            <h4 class="text-xs font-black text-sky-400 uppercase tracking-wider flex items-center gap-1.5">
                                <span>👤 Identity & Contact Profile</span>
                            </h4>
                            <div class="space-y-2 text-xs font-mono">
                                <div class="flex justify-between py-1 border-b border-white/5">
                                    <span class="text-slate-400">Department / Org:</span>
                                    <span class="text-white">${device.department || 'Not Assigned'}</span>
                                </div>
                                <div class="flex justify-between py-1 border-b border-white/5">
                                    <span class="text-slate-400">Role / Relation:</span>
                                    <span class="text-sky-300 font-bold">${device.roleFamily !== 'None' ? device.roleFamily : device.role}</span>
                                </div>
                                <div class="flex justify-between py-1 border-b border-white/5">
                                    <span class="text-slate-400">Phone:</span>
                                    <span class="text-white">${device.phone || 'N/A'}</span>
                                </div>
                                <div class="flex justify-between py-1 border-b border-white/5">
                                    <span class="text-slate-400">Email:</span>
                                    <span class="text-white">${device.email || 'N/A'}</span>
                                </div>
                                <div class="flex justify-between py-1">
                                    <span class="text-slate-400">Emergency Note:</span>
                                    <span class="text-amber-300 font-sans">${device.emergencyNote || 'None'}</span>
                                </div>
                            </div>
                        </div>

                        <!-- Spatial & Geodetic Telemetry -->
                        <div class="bg-black/30 p-4 rounded-xl border border-white/5 space-y-3">
                            <h4 class="text-xs font-black text-sky-400 uppercase tracking-wider flex items-center gap-1.5">
                                <span>🛰️ Spatial & Hardware Diagnostics</span>
                            </h4>
                            <div class="space-y-2 text-xs font-mono">
                                <div class="flex justify-between py-1 border-b border-white/5">
                                    <span class="text-slate-400">Coordinates:</span>
                                    <span class="text-white">${device.getFormattedCoords()}</span>
                                </div>
                                <div class="flex justify-between py-1 border-b border-white/5">
                                    <span class="text-slate-400">Altitude / Accuracy:</span>
                                    <span class="text-white">${device.altitude.toFixed(1)}m / ±${device.accuracy.toFixed(1)}m</span>
                                </div>
                                <div class="flex justify-between py-1 border-b border-white/5">
                                    <span class="text-slate-400">Mock GPS Detected:</span>
                                    <span class="${device.isMockGps ? 'text-red-400 font-bold' : 'text-emerald-400'}">${device.isMockGps ? 'YES ⚠️' : 'NO (Authentic)'}</span>
                                </div>
                                <div class="flex justify-between py-1 border-b border-white/5">
                                    <span class="text-slate-400">Airplane Mode:</span>
                                    <span class="${device.isAirplaneMode ? 'text-amber-400' : 'text-slate-300'}">${device.isAirplaneMode ? 'ACTIVE' : 'INACTIVE'}</span>
                                </div>
                                <div class="flex justify-between py-1">
                                    <span class="text-slate-400">Safe Zones Assigned:</span>
                                    <span class="text-sky-400 font-bold">${device.safeZonesCount} Zones</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Address Block & Latest Metadata -->
                    <div class="bg-black/30 p-4 rounded-xl border border-white/5 text-xs space-y-2">
                        <div class="flex items-center justify-between font-mono text-[11px] text-sky-400 font-bold border-b border-white/5 pb-2">
                            <span class="flex items-center gap-1.5">🕒 <span>Last Telemetry: ${device.getFormattedDateTime()}</span></span>
                            <span class="text-slate-400 font-normal">(${device.getFormattedTimeDelta()})</span>
                        </div>
                        <div>
                            <span class="text-[10px] text-slate-400 font-mono uppercase block mb-1">📍 Reverse-Geocoded Street Address</span>
                            <div class="text-slate-200 font-medium">${device.address || 'No physical street address recorded.'}</div>
                        </div>
                    </div>
                </div>

                <!-- Footer Actions -->
                <div class="p-4 border-t border-white/10 bg-slate-950 flex items-center justify-between">
                    <button class="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold transition-all flex items-center gap-1.5"
                            onclick="window.MatrixApp && window.MatrixApp.pokeDevice('${device.agentId}')">
                        <span>⚡ Send Poke / Ping Signal</span>
                    </button>

                    <div class="flex items-center gap-2">
                        <button class="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold transition-all"
                                onclick="window.MatrixApp && window.MatrixApp.showHistory('${device.agentId}'); document.getElementById('device-dossier-modal')?.remove();">
                            <span>📍 View History Trail</span>
                        </button>
                        <button class="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-black uppercase tracking-wider transition-all shadow-lg shadow-sky-600/30"
                                onclick="window.MatrixApp && window.MatrixApp.focusDevice('${device.agentId}'); document.getElementById('device-dossier-modal')?.remove();">
                            <span>🎯 Center On Map</span>
                        </button>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        const closeBtn = modal.querySelector('#btn-close-dossier');
        const editBtn = modal.querySelector('#btn-edit-from-dossier');
        const handleClose = () => modal.remove();

        closeBtn.addEventListener('click', handleClose);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) handleClose();
        });

        editBtn.addEventListener('click', () => {
            modal.remove();
            if (window.MatrixApp && window.MatrixApp.openEditProfile) {
                window.MatrixApp.openEditProfile(device.agentId);
            }
        });
    }
}
