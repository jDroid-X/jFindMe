import { GeoUtils } from '../core/GeoUtils.js';

/**
 * jButterflyHubView - Tactical Map Radar Multi-Device Workspace (OOPS Architecture)
 * 1. Left Side: Map Selection Matrix (Searchable Multi-Device Checkbox Roster with Avatars & RAG Status)
 * 2. Right Side: Leaflet Selected Device Telemetry Card (with Avatar, Live Unified Status, ACL stream switch, and Actions)
 * Vertically Ultra-Compressed Smart Fit.
 */
export class jButterflyHubView {
    /**
     * Renders Left Panel: Map Selection Matrix (20% Slimmer & Ultra Compact)
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} allDevices 
     * @param {Array<string>} selectedIds 
     * @param {string} focusedAgentId 
     * @returns {string} HTML string
     */
    static renderSelectionMatrixHtml(allDevices = [], selectedIds = null, focusedAgentId = null) {
        if (!allDevices || allDevices.length === 0) return '';

        const allSelected = !selectedIds || selectedIds.length === allDevices.length;
        const isDeviceSelected = (d) => !selectedIds || selectedIds.includes(d.agentId);

        return `
            <div class="matrix-selection-panel w-[210px] sm:w-[225px] max-h-[calc(100vh-100px)] select-none pointer-events-auto font-sans animate-fade-in flex flex-col bg-white/96 dark:bg-slate-950/92 backdrop-blur-xl rounded-2xl border border-slate-200 dark:border-white/10 shadow-2xl overflow-hidden text-slate-800 dark:text-slate-100">
                <!-- Header -->
                <div class="px-2.5 py-1.5 border-b border-slate-200 dark:border-white/10 flex items-center justify-between bg-slate-50 dark:bg-slate-900/80">
                    <div class="flex items-center gap-1 min-w-0">
                        <span class="text-[11px]">📡</span>
                        <span class="text-[10px] font-bold text-sky-600 dark:text-sky-400 uppercase tracking-wider truncate">Selection Matrix</span>
                    </div>
                    <button class="px-1.5 py-0.2 rounded bg-sky-500/10 dark:bg-sky-600/30 text-sky-700 dark:text-sky-300 text-[8px] font-mono hover:bg-sky-500 hover:text-white transition-all font-semibold border border-sky-300 dark:border-sky-500/40"
                            onclick="window.MatrixApp.selectAllDevices()">
                        ${allSelected ? '✓ All' : 'Select All'}
                    </button>
                </div>

                <!-- Roster List (Theme Adaptive & Vertically Ultra-Compressed) -->
                <div class="p-1 space-y-0.5 overflow-y-auto custom-scrollbar max-h-[calc(100vh-160px)] font-mono text-[11px]">
                    ${allDevices.map(d => {
                        const selected = isDeviceSelected(d);
                        const isFocused = d.agentId === focusedAgentId;
                        const unified = typeof d.getUnifiedStatus === 'function' ? d.getUnifiedStatus() : { status: d.isOnline ? 'LIVE' : 'OFFLINE', dotClass: d.isOnline ? 'bg-emerald-400' : 'bg-slate-500' };
                        const avatarUrl = typeof d.getAvatarUrl === 'function' ? d.getAvatarUrl() : '/Avatar/1.png';
                        const themeColor = typeof d.getThemeColor === 'function' ? d.getThemeColor() : '#00E5FF';

                        return `
                            <div class="flex items-center justify-between p-1 rounded-xl border transition-all cursor-pointer ${isFocused ? 'ring-1 ring-sky-500 bg-sky-50 dark:bg-sky-950/70 border-sky-400 dark:border-sky-500/50 shadow-sm' : (selected ? 'bg-slate-50/80 dark:bg-slate-900/70 border-slate-200 dark:border-white/10 hover:bg-slate-100 dark:hover:bg-slate-800/80' : 'bg-slate-100/40 dark:bg-black/40 border-slate-200/40 dark:border-white/5 opacity-60 hover:opacity-90')}"
                                 onclick="window.MatrixApp.focusDevice('${d.agentId}')">
                                <div class="flex items-center gap-1.5 min-w-0 flex-1">
                                    <input type="checkbox" ${selected ? 'checked' : ''} class="rounded text-sky-500 bg-white dark:bg-slate-900 border-slate-300 dark:border-white/20 w-3 h-3 cursor-pointer shrink-0"
                                           onclick="event.stopPropagation(); window.MatrixApp.toggleDeviceSelection('${d.agentId}')">
                                    
                                    <!-- Device Avatar Thumbnail -->
                                    <div class="relative shrink-0 w-5 h-5 rounded-full overflow-hidden border shadow-sm" style="border-color: ${themeColor}">
                                        <img src="${avatarUrl}" class="w-full h-full object-cover" alt="${d.name}"
                                             onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                                        <span class="absolute bottom-0 right-0 w-1.5 h-1.5 rounded-full ${unified.dotClass}"></span>
                                    </div>

                                    <div class="min-w-0 flex-1">
                                        <div class="flex items-center gap-1">
                                            <span class="text-[10px] font-bold text-slate-900 dark:text-slate-100 truncate leading-tight font-sans">${d.name}</span>
                                            <span class="w-1.5 h-1.5 rounded-full ${unified.dotClass} shrink-0" title="${unified.label}"></span>
                                        </div>
                                        <div class="text-[8px] text-slate-500 dark:text-slate-400 truncate">${d.agentId} • ${d.battery >= 0 ? `${d.battery}%` : '--'}</div>
                                    </div>
                                </div>

                                <div class="flex items-center gap-0.5 shrink-0 ml-1">
                                    <span class="${unified.badgeClass} text-[7px] font-bold font-mono px-1 py-0.2 uppercase">
                                        ${unified.status === 'LIVE' ? 'LIVE' : (unified.status === 'EMERGENCY' ? 'SOS' : (unified.status === 'INACTIVE_RECENT' ? 'IDLE' : 'OFFLINE'))}
                                    </span>
                                    <button class="text-[7.5px] font-mono px-1 py-0.2 rounded border bg-slate-100 dark:bg-slate-800/80 hover:bg-sky-500 hover:text-white text-slate-700 dark:text-slate-300 border-slate-200 dark:border-white/10 transition-all"
                                            onclick="event.stopPropagation(); window.MatrixApp.focusDevice('${d.agentId}')"
                                            title="Focus ${d.name} on Radar">
                                        Focus
                                    </button>
                                </div>
                            </div>
                        `;
                    }).join('')}
                </div>
            </div>
        `;
    }

    /**
     * Renders Right Panel: Leaflet Selected Device Telemetry Card (Readable Fonts & Top Address Layout)
     * @param {import('../models/DeviceModel.js').DeviceModel} device 
     * @returns {string} HTML string
     */
    static renderDeviceDetailCardHtml(device) {
        if (!device) return '';

        const battery = device.battery >= 0 ? `${device.battery}%` : 'N/A';
        const wifiName = (device.networkType && device.networkType !== 'UNKNOWN') ? device.networkType : (device.isOnline ? 'Cellular / WiFi' : 'Disconnected');
        const lastSeen = device.getFormattedLastSeen();
        const avatarUrl = typeof device.getAvatarUrl === 'function' ? device.getAvatarUrl() : '/Avatar/1.png';
        const themeColor = typeof device.getThemeColor === 'function' ? device.getThemeColor() : '#00E5FF';
        const unified = typeof device.getUnifiedStatus === 'function' ? device.getUnifiedStatus() : { status: device.isOnline ? 'LIVE' : 'OFFLINE', badgeClass: device.isOnline ? 'badge-success' : 'badge-danger', dotClass: device.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400', label: device.isOnline ? 'LIVE' : 'OFFLINE' };

        return `
            <div class="leaflet-device-card w-[240px] sm:w-[260px] max-h-[calc(100vh-100px)] select-none pointer-events-auto font-sans animate-fade-in flex flex-col bg-white/96 dark:bg-slate-950/92 backdrop-blur-xl rounded-2xl border border-slate-200 dark:border-white/10 shadow-2xl overflow-y-auto custom-scrollbar p-2.5 space-y-2 text-slate-800 dark:text-slate-100">
                
                <!-- 1. Top Row: Avatar + Identity + Status + Quick Controls -->
                <div class="flex items-start justify-between gap-2 border-b border-slate-100 dark:border-slate-800/80 pb-2">
                    <div class="flex items-center gap-2 min-w-0">
                        <!-- Avatar -->
                        <div class="relative shrink-0 w-9 h-9 rounded-full p-0.5 border-2 shadow-md" style="border-color: ${themeColor}">
                            <img src="${avatarUrl}" class="w-full h-full rounded-full object-cover bg-slate-900" alt="${device.name}"
                                 onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                            <span class="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full border-2 border-white dark:border-slate-950 ${unified.dotClass}"></span>
                        </div>

                        <!-- Name & Subtitle -->
                        <div class="min-w-0 flex-1">
                            <div class="flex items-center gap-1">
                                <h3 class="text-xs font-black text-slate-900 dark:text-white tracking-tight truncate leading-tight">${device.name}</h3>
                            </div>
                            <div class="text-[10px] text-slate-500 dark:text-slate-400 font-mono truncate mt-0.5">
                                <span class="text-sky-600 dark:text-sky-400 font-bold">${device.agentId}</span> • ${device.deviceName || device.model || 'Android'}
                            </div>
                            <div class="text-[9px] text-slate-400 dark:text-slate-500 font-medium truncate mt-0.5">
                                ⏱️ ${lastSeen}
                            </div>
                        </div>
                    </div>

                    <!-- Top Action Badges -->
                    <div class="flex flex-col items-end gap-1 shrink-0">
                        <span class="${unified.badgeClass} text-[8.5px] font-bold font-mono px-1.5 py-0.5">
                            ${unified.label}
                        </span>
                        <div class="flex items-center gap-1 text-slate-400">
                            <button class="p-1 rounded hover:bg-slate-100 dark:hover:bg-slate-800 text-xs" title="Device Dossier" onclick="window.MatrixApp.showDossier('${device.agentId}')">📄</button>
                            <button class="p-1 rounded hover:bg-slate-100 dark:hover:bg-slate-800 text-xs" title="Refresh Location" onclick="window.MatrixApp.refreshNow()">🔄</button>
                        </div>
                    </div>
                </div>

                <!-- 2. Geocoded Street Address (Moved ABOVE Proximity Grid) -->
                ${device.address ? `
                <div class="bg-slate-100 dark:bg-slate-900/80 rounded-xl p-2 text-[10px] text-slate-700 dark:text-slate-200 border border-slate-200 dark:border-slate-800 flex items-start gap-1.5 font-sans shadow-sm">
                    <span class="text-xs text-sky-500 shrink-0">📍</span>
                    <div class="line-clamp-2 leading-snug font-medium">${device.address}</div>
                </div>
                ` : `
                <div class="bg-slate-100 dark:bg-slate-900/80 rounded-xl p-1.5 text-[9px] text-slate-500 dark:text-slate-400 border border-slate-200 dark:border-slate-800 flex items-center gap-1 font-sans">
                    <span class="text-xs text-slate-400 shrink-0">📍</span>
                    <span class="truncate font-mono">${device.latitude ? `${device.latitude.toFixed(4)}°, ${device.longitude.toFixed(4)}°` : 'Live Satellite Position'}</span>
                </div>
                `}

                <!-- 3. Telemetry Grid (Readable Proximity, Velocity, Battery, Precision) -->
                <div class="grid grid-cols-2 gap-1.5 font-mono text-[10px] bg-slate-50 dark:bg-slate-900/60 p-2 rounded-xl border border-slate-100 dark:border-slate-800">
                    <div>
                        <span class="text-slate-400 block text-[8.5px] uppercase font-semibold leading-none mb-0.5">Proximity</span>
                        <span class="font-bold text-sky-600 dark:text-sky-400 truncate block text-[11px]">${device.getFormattedDistance ? device.getFormattedDistance() : '--'}</span>
                    </div>
                    <div>
                        <span class="text-slate-400 block text-[8.5px] uppercase font-semibold leading-none mb-0.5">Velocity</span>
                        <span class="font-bold text-amber-600 dark:text-amber-400 truncate block text-[11px]">${device.getFormattedSpeed ? device.getFormattedSpeed() : '0.0 km/h'}</span>
                    </div>
                    <div>
                        <span class="text-slate-400 block text-[8.5px] uppercase font-semibold leading-none mb-0.5">Battery</span>
                        <span class="font-bold ${device.battery <= 20 ? 'text-rose-500' : 'text-emerald-500'} truncate block text-[11px]">🔋 ${battery} ${device.isCharging ? '⚡' : ''}</span>
                    </div>
                    <div>
                        <span class="text-slate-400 block text-[8.5px] uppercase font-semibold leading-none mb-0.5">GPS Precision</span>
                        <span class="font-bold text-slate-700 dark:text-slate-300 truncate block text-[11px]">±${device.accuracy ? device.accuracy.toFixed(1) : '5.0'}m</span>
                    </div>
                </div>

                <!-- 4. Live GPS Stream Toggle -->
                <div class="flex items-center justify-between px-2.5 py-1.5 rounded-xl bg-slate-50 dark:bg-slate-900/40 border border-slate-100 dark:border-slate-800 text-xs">
                    <div class="flex items-center gap-1.5">
                        <span class="text-xs">🛰️</span>
                        <span class="text-[10.5px] font-bold text-slate-700 dark:text-slate-300">Live GPS Stream</span>
                    </div>
                    <label class="switch-toggle switch-sm" style="transform: scale(0.9); transform-origin: right center;" title="Toggle Live GPS Stream">
                        <input type="checkbox" class="hub-switch-stream" data-agent-id="${device.agentId}" ${device.raw?.acl?.acl_live_tracking === 'DENIED' ? '' : 'checked'}>
                        <span class="switch-slider"></span>
                    </label>
                </div>

                <!-- 5. Quick Action Buttons Strip -->
                <div class="grid grid-cols-3 gap-1.5 pt-0.5 font-sans">
                    <button class="flex flex-col items-center justify-center p-1.5 rounded-xl bg-slate-100 dark:bg-slate-900 hover:bg-sky-50 dark:hover:bg-sky-950/50 text-slate-700 dark:text-slate-200 hover:text-sky-600 dark:hover:text-sky-400 border border-slate-200 dark:border-slate-800 transition-all text-center"
                            onclick="window.MatrixApp.pokeDevice('${device.agentId}')" title="Play Ring Sound">
                        <span class="text-sm">🔊</span>
                        <span class="text-[9.5px] font-bold leading-tight mt-0.5">Ring</span>
                    </button>

                    <button class="flex flex-col items-center justify-center p-1.5 rounded-xl bg-slate-100 dark:bg-slate-900 hover:bg-emerald-50 dark:hover:bg-emerald-950/50 text-slate-700 dark:text-slate-200 hover:text-emerald-600 dark:hover:text-emerald-400 border border-slate-200 dark:border-slate-800 transition-all text-center"
                            onclick="window.MatrixApp.showHistory('${device.agentId}')" title="View History Route Trail">
                        <span class="text-sm">🗺️</span>
                        <span class="text-[9.5px] font-bold leading-tight mt-0.5">Trail</span>
                    </button>

                    <button class="flex flex-col items-center justify-center p-1.5 rounded-xl bg-slate-100 dark:bg-slate-900 hover:bg-amber-50 dark:hover:bg-amber-950/50 text-slate-700 dark:text-slate-200 hover:text-amber-600 dark:hover:text-amber-400 border border-slate-200 dark:border-slate-800 transition-all text-center"
                            onclick="window.MatrixApp.showDossier('${device.agentId}')" title="Lock & Device Dossier">
                        <span class="text-sm">📄</span>
                        <span class="text-[9.5px] font-bold leading-tight mt-0.5">Dossier</span>
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Backward-compatible unified layout
     */
    static renderHubCardHtml(device, allDevices = [], selectedIds = null) {
        return `
            <div class="flex items-start justify-between w-full select-none pointer-events-none px-4">
                <!-- LEFT: Map Selection Matrix -->
                <div class="pointer-events-auto">
                    ${allDevices.length > 0 ? this.renderSelectionMatrixHtml(allDevices, selectedIds, device?.agentId) : ''}
                </div>
                
                <!-- RIGHT: Leaflet Device Telemetry Card with Avatar -->
                <div class="pointer-events-auto">
                    ${device ? this.renderDeviceDetailCardHtml(device) : ''}
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
