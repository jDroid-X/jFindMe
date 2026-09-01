import { GeoUtils } from '../core/GeoUtils.js';

/**
 * jFindMe / jButterfly Hub View (Material 3 Single-Layer Floating Card)
 * Enterprise closed-loop component providing 3-column wrapping multi-device combo selector
 * and rich vertical telemetry matrix.
 */
export class jButterflyHubView {
    /**
     * Renders the jButterfly Hub floating left sidebar card
     * @param {import('../models/DeviceModel.js').DeviceModel} device 
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} allDevices 
     * @returns {string} HTML string
     */
    static renderHubCardHtml(device, allDevices = []) {
        if (!device) return '';

        const battery = device.battery >= 0 ? `${device.battery}%` : '58%';
        const wifiName = device.networkType !== 'UNKNOWN' ? device.networkType : 'jNokia5G';
        const lastSeen = device.getFormattedLastSeen();

        return `
            <div class="jbutterfly-card-wrapper w-[305px] min-h-[500px] max-h-[calc(100vh-80px)] overflow-y-auto custom-scrollbar select-none pointer-events-auto font-sans animate-fade-in flex flex-col">
                <!-- Multi-Device 3-Column Wrapping Combo Grid -->
                ${allDevices.length > 1 ? `
                <div class="grid grid-cols-3 gap-1.5 mb-2.5 p-1 bg-slate-950/80 backdrop-blur-md rounded-2xl border border-white/10 shadow-lg">
                    ${allDevices.map(d => `
                        <button class="px-2 py-2 rounded-xl text-[11.5px] font-semibold flex items-center justify-center gap-1 transition-all shadow-sm truncate ${d.agentId === device.agentId ? 'bg-sky-600 text-white font-bold shadow-md ring-2 ring-sky-400/40' : 'bg-slate-900/90 text-slate-300 hover:bg-slate-800 hover:text-white border border-white/5'}"
                                onclick="window.MatrixApp.focusDevice('${d.agentId}')"
                                title="${d.name} (${d.agentId})">
                            <span class="w-2 h-2 rounded-full ${d.isOnline ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'} shrink-0"></span>
                            <span class="truncate">${d.name}</span>
                        </button>
                    `).join('')}
                </div>
                ` : ''}

                <!-- Main Clean Expanded Material 3 Card (3x Height Window) -->
                <div class="bg-white/98 backdrop-blur-xl rounded-3xl shadow-2xl border border-slate-100 p-4 space-y-3.5 text-slate-800 flex-1 flex flex-col justify-between">
                    <!-- Header: Device Silhouette + Info + Controls -->
                    <div class="flex items-start justify-between">
                        <div class="flex items-center gap-3 min-w-0">
                            <!-- Phone Silhouette Icon -->
                            <div class="w-12 h-18 rounded-2xl bg-slate-900 border-2 border-slate-700 p-1.5 shadow-md flex flex-col items-center justify-between shrink-0 relative overflow-hidden">
                                <div class="w-4 h-1 bg-slate-700 rounded-full"></div>
                                <div class="w-full h-9 bg-slate-800 rounded flex items-center justify-center text-white/40 text-sm">
                                    📱
                                </div>
                                <div class="w-2 h-2 rounded-full border border-slate-600"></div>
                            </div>

                            <!-- Name & Telemetry -->
                            <div class="min-w-0 flex-1">
                                <h2 class="text-base font-extrabold text-slate-900 tracking-tight truncate leading-tight">${device.name}</h2>
                                <p class="text-xs text-slate-500 font-medium truncate">${device.deviceName || device.model || 'Android'} • <span class="font-mono text-sky-600 font-bold">${device.agentId}</span></p>
                                <p class="text-[10px] text-slate-400 font-medium mt-0.5">${lastSeen}</p>
                                
                                <div class="flex items-center gap-2 text-xs text-slate-600 mt-1 font-medium">
                                    <span class="flex items-center gap-1 truncate">
                                        <svg class="w-3.5 h-3.5 text-slate-500 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                            <path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z"/>
                                        </svg>
                                        <span class="truncate max-w-[85px]">${wifiName}</span>
                                    </span>
                                    <span>•</span>
                                    <span class="flex items-center gap-1 shrink-0 font-bold text-slate-800">
                                        <span>🔋</span>
                                        <span>${battery}</span>
                                    </span>
                                </div>
                            </div>
                        </div>

                        <!-- Top Right Settings & Refresh Icons -->
                        <div class="flex items-center gap-1 text-slate-400 shrink-0">
                            <button class="p-1.5 rounded-full hover:bg-slate-100 hover:text-slate-700 transition-all text-sm" title="Device Settings" onclick="window.MatrixApp.openEditProfile('${device.agentId}')">
                                ⚙️
                            </button>
                            <button class="p-1.5 rounded-full hover:bg-slate-100 hover:text-slate-700 transition-all text-sm" title="Refresh Location" onclick="window.MatrixApp.refreshNow()">
                                🔄
                            </button>
                        </div>
                    </div>

                    <!-- Telemetry Distance & Coordinates Matrix Card -->
                    <div class="bg-slate-50 rounded-2xl p-3 border border-slate-100 space-y-1.5 font-mono text-xs">
                        <div class="flex items-center justify-between">
                            <span class="text-slate-500 font-sans">Proximity:</span>
                            <span class="font-bold ${device.isWithinSearchRadius ? 'text-sky-600' : 'text-amber-600'}">
                                ${device.isWithinSearchRadius ? '🎯' : '📡'} ${device.getFormattedDistance()}
                            </span>
                        </div>
                        <div class="flex items-center justify-between text-slate-500">
                            <span class="font-sans">Velocity:</span>
                            <span class="font-bold text-slate-700">${device.getFormattedSpeed()}</span>
                        </div>
                        <div class="flex items-center justify-between text-slate-500">
                            <span class="font-sans">GPS Fix:</span>
                            <span class="text-slate-700 truncate max-w-[165px]">${device.latitude ? `${device.latitude.toFixed(4)}°, ${device.longitude.toFixed(4)}°` : 'No Signal'}</span>
                        </div>
                    </div>

                    <!-- Action List (Expanded Style) -->
                    <div class="space-y-1.5 pt-1 border-t border-slate-100 flex-1">
                        <!-- Play Sound Button -->
                        <button class="w-full flex items-center gap-3 p-2.5 rounded-2xl hover:bg-slate-50 transition-all text-left text-slate-700 hover:text-slate-900 group"
                                onclick="window.MatrixApp.pokeDevice('${device.agentId}')">
                            <div class="w-8 h-8 rounded-xl bg-slate-100 text-slate-700 flex items-center justify-center text-sm group-hover:bg-sky-100 group-hover:text-sky-700 transition-colors shrink-0">
                                🔊
                            </div>
                            <div class="flex-1 min-w-0">
                                <div class="text-xs font-bold leading-tight">Play sound</div>
                                <div class="text-[10px] text-slate-400 truncate">Ring at max volume for 5m</div>
                            </div>
                        </button>

                        <!-- Mark as Lost / Dossier Button -->
                        <button class="w-full flex items-center gap-3 p-2.5 rounded-2xl hover:bg-slate-50 transition-all text-left text-slate-700 hover:text-slate-900 group"
                                onclick="window.MatrixApp.showDossier('${device.agentId}')">
                            <div class="w-8 h-8 rounded-xl bg-slate-100 text-slate-700 flex items-center justify-center text-sm group-hover:bg-amber-100 group-hover:text-amber-700 transition-colors shrink-0">
                                🔒
                            </div>
                            <div class="flex-1 min-w-0">
                                <div class="text-xs font-bold leading-tight">Device Dossier</div>
                                <div class="text-[10px] text-slate-400 truncate">Lock & forensic profile</div>
                            </div>
                        </button>

                        <!-- Location History Button -->
                        <button class="w-full flex items-center gap-3 p-2.5 rounded-2xl hover:bg-slate-50 transition-all text-left text-slate-700 hover:text-slate-900 group"
                                onclick="window.MatrixApp.showHistory('${device.agentId}')">
                            <div class="w-8 h-8 rounded-xl bg-slate-100 text-slate-700 flex items-center justify-center text-sm group-hover:bg-emerald-100 group-hover:text-emerald-700 transition-colors shrink-0">
                                📍
                            </div>
                            <div class="flex-1 min-w-0">
                                <div class="text-xs font-bold leading-tight">Location history</div>
                                <div class="text-[10px] text-slate-400 truncate">Route playback trail</div>
                            </div>
                        </button>
                    </div>

                    <!-- Geocoded Street Address -->
                    ${device.address ? `
                    <div class="bg-slate-50 rounded-2xl p-2.5 text-xs text-slate-600 border border-slate-100 flex items-start gap-2">
                        <span class="text-slate-400 text-sm shrink-0">📍</span>
                        <div class="line-clamp-2 leading-tight">${device.address}</div>
                    </div>
                    ` : ''}
                </div>
            </div>
        `;
    }

    /**
     * Generates jButterfly tactical circular pin HTML with glowing halo, device name, and avatar
     * @param {import('../models/DeviceModel.js').DeviceModel} device 
     * @returns {string} HTML markup
     */
    static renderTacticalPinHtml(device) {
        if (!device) return '';
        const themeColor = device.getThemeColor ? device.getThemeColor() : '#00E5FF';
        const avatarUrl = device.getAvatarUrl ? device.getAvatarUrl() : 'data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>';
        const isWithin = device.isWithinSearchRadius !== undefined ? device.isWithinSearchRadius : true;
        const bearing = Math.round(device.bearing || 0);

        return `
            <div class="matrix-marker-wrapper ${isWithin ? 'in-radar' : 'out-radar'} ${device.isOnline ? 'online' : 'offline'} flex flex-col items-center select-none cursor-pointer">
                <!-- Device Name Callout Pill -->
                <div class="marker-callout-pill" style="border: 1.5px solid ${themeColor}; background: rgba(8, 12, 22, 0.95); backdrop-filter: blur(8px); border-radius: 9999px; padding: 2px 8px; font-size: 10px; font-weight: 800; color: #ffffff; white-space: nowrap; box-shadow: 0 4px 12px rgba(0,0,0,0.6); margin-bottom: 2px; pointer-events: none; max-width: 120px; overflow: hidden; text-overflow: ellipsis;">
                    <span class="marker-name-text">${device.name}</span>
                </div>

                <!-- Avatar Container with Theme Glow (Transparent Background) -->
                <div class="marker-avatar-container relative w-10 h-10 rounded-full bg-transparent p-0 shadow-lg transition-transform hover:scale-110" 
                     style="border: 2.5px solid ${themeColor}; box-shadow: 0 0 14px ${themeColor}60;">
                    <img src="${avatarUrl}" class="w-full h-full rounded-full object-cover bg-transparent" alt="${device.name}"
                         onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                    
                    <!-- Live Online Pulsar Dot -->
                    <span class="absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full border-2 border-slate-950 ${device.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-500'}"></span>
                    
                    <!-- Battery Pill Badge -->
                    ${device.battery >= 0 ? `<span class="absolute -top-1.5 -right-2 px-1 py-0.2 rounded-full bg-slate-950/90 text-white text-[8px] font-mono font-bold border border-white/20">${device.battery}%</span>` : ''}
                </div>

                <!-- Direction Bearing Indicator -->
                ${device.speed > 0.8 ? `<div class="w-0 h-0 border-l-[4px] border-l-transparent border-r-[4px] border-r-transparent border-b-[6px] mt-0.5" style="transform: rotate(${bearing}deg); border-bottom-color: ${themeColor};"></div>` : ''}

                <!-- Pin Anchor Point -->
                <div class="w-0 h-0 border-l-[4px] border-l-transparent border-r-[4px] border-r-transparent border-t-[5px]" style="border-top-color: ${themeColor}; margin-top: -1px;"></div>
            </div>
        `;
    }
}

// SSOT Alias for backwards compatibility
export const GoogleFindHubView = jButterflyHubView;
