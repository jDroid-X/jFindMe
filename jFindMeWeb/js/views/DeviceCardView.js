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
            <div class="matrix-visiting-card glass-panel shadow-2xl p-3.5 rounded-2xl border border-white/20 text-white w-[280px] select-none font-sans bg-slate-950/95 backdrop-blur-2xl">
                <!-- Header: Avatar, Name & Live Status -->
                <div class="flex items-center gap-3 border-b border-white/10 pb-2.5 mb-2.5">
                    <div class="relative shrink-0">
                        <img src="${avatarUrl}" alt="${device.name}" 
                             class="w-10 h-10 rounded-full object-cover border-2 shadow-md bg-transparent" 
                             style="border-color: ${themeColor};"
                             onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                        <span class="absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-slate-900 ${device.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-500'}"></span>
                    </div>
                    <div class="flex-1 min-w-0">
                        <div class="flex items-center justify-between gap-1">
                            <h4 class="font-bold text-sm text-white truncate tracking-tight">${device.name}</h4>
                            <span class="text-[9px] font-mono px-1.5 py-0.5 rounded uppercase font-bold tracking-wider ${device.isOnline ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-slate-800 text-slate-400 border border-white/10'}">
                                ${device.isOnline ? 'LIVE' : 'OFF'}
                            </span>
                        </div>
                        <div class="text-[10px] text-slate-300 truncate flex items-center gap-1 mt-0.5 font-mono">
                            <span class="truncate font-semibold text-slate-200">${device.deviceName || device.model || device.agentId}</span>
                            <span>•</span>
                            <span class="text-sky-400 font-sans font-bold">${device.roleFamily !== 'None' ? device.roleFamily : device.role}</span>
                        </div>
                    </div>
                </div>

                <!-- Distance Radar Badge -->
                <div class="mb-2.5 flex items-center justify-between text-xs px-2.5 py-1.5 rounded-xl ${isWithin ? 'bg-sky-500/20 border border-sky-500/40 text-sky-300' : 'bg-amber-500/15 border border-amber-500/30 text-amber-300'}">
                    <span class="flex items-center gap-1 font-semibold text-[11px] truncate">
                        <span>${isWithin ? '🎯 10km Radius' : '📡 Radar Out'}</span>
                    </span>
                    <span class="font-mono font-bold text-xs">${device.getFormattedDistance()}</span>
                </div>

                <!-- Telemetry Grid -->
                <div class="grid grid-cols-2 gap-2 text-xs bg-black/50 rounded-xl p-2.5 border border-white/10 mb-2.5 font-mono">
                    <div class="flex flex-col">
                        <span class="text-[9px] text-slate-400 uppercase tracking-wider font-sans font-semibold">Battery</span>
                        <span class="font-bold text-xs flex items-center gap-1" style="color: ${batteryMeta.color}">
                            ${batteryMeta.icon} ${batteryMeta.text}
                        </span>
                    </div>
                    <div class="flex flex-col">
                        <span class="text-[9px] text-slate-400 uppercase tracking-wider font-sans font-semibold">Speed</span>
                        <span class="text-white font-bold text-xs">${device.getFormattedSpeed()}</span>
                    </div>
                    <div class="flex flex-col">
                        <span class="text-[9px] text-slate-400 uppercase tracking-wider font-sans font-semibold">Heading</span>
                        <span class="text-white font-bold text-xs">${bearing}° (${cardinal})</span>
                    </div>
                    <div class="flex flex-col">
                        <span class="text-[9px] text-slate-400 uppercase tracking-wider font-sans font-semibold">Last Pulse</span>
                        <span class="text-slate-200 font-bold text-xs">${device.getFormattedLastSeen()}</span>
                    </div>
                </div>

                <!-- Location Address & Date/Time Metadata -->
                <div class="bg-black/40 rounded-xl p-2.5 mb-2.5 border border-white/10 space-y-1.5">
                    <div class="flex items-center justify-between text-[10px] font-mono text-sky-400 font-bold border-b border-white/10 pb-1">
                        <span class="flex items-center gap-1">🕒 <span>${device.getFormattedDateTime()}</span></span>
                    </div>
                    <div class="text-[11px] text-slate-200 line-clamp-2 pt-0.5 leading-relaxed">
                        <span class="text-sky-400">📍 </span>${device.address || 'No street address registered'}
                    </div>
                </div>

                <!-- Quick Action Buttons -->
                <div class="flex items-center gap-2 pt-1.5 border-t border-white/10">
                    <button class="flex-1 py-2 px-2.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-bold text-xs transition-all flex items-center justify-center gap-1 shadow-lg shadow-sky-600/30"
                            onclick="window.MatrixApp.focusDevice('${device.agentId}')">
                        <span>🎯 Center</span>
                    </button>
                    <button class="py-2 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold border border-white/10 transition-all flex items-center justify-center gap-1"
                            onclick="window.MatrixApp.showHistory('${device.agentId}')">
                        <span>📍 Trail</span>
                    </button>
                    <button class="py-2 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold border border-white/10 transition-all flex items-center justify-center gap-1"
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
