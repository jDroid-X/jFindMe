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

        // Eagle's Eye Macro Metrics Aggregate
        let movingCount = 0;
        let idleCount = 0;
        let offlineCount = 0;
        let totalBat = 0;
        let validBatCount = 0;
        let mockGpsCount = 0;

        allDevices.forEach(d => {
            const speed = Number(d.speed || d.spd || 0);
            if (d.isOnline) {
                if (speed > 1.5) movingCount++;
                else idleCount++;
            } else {
                offlineCount++;
            }
            if (d.battery >= 0) {
                totalBat += d.battery;
                validBatCount++;
            }
            if (d.isMockGps || d.isMock) mockGpsCount++;
        });

        const avgBattery = validBatCount > 0 ? Math.round(totalBat / validBatCount) : '--';

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

                <!-- Eagle's Eye Macro KPI Bar (Fleet Telemetry Summary) -->
                <div class="grid grid-cols-3 gap-0.5 px-2 py-1 bg-slate-100/70 dark:bg-slate-900/90 border-b border-slate-200 dark:border-white/10 text-[9px] font-mono select-none">
                    <div class="text-center">
                        <span class="text-emerald-500 font-bold">${movingCount}</span>
                        <span class="text-slate-400 block text-[7px] uppercase tracking-tighter">Moving</span>
                    </div>
                    <div class="text-center">
                        <span class="text-amber-500 font-bold">${idleCount}</span>
                        <span class="text-slate-400 block text-[7px] uppercase tracking-tighter">Idle</span>
                    </div>
                    <div class="text-center">
                        <span class="${avgBattery !== '--' && avgBattery < 20 ? 'text-rose-400' : 'text-sky-400'} font-bold">${avgBattery}%</span>
                        <span class="text-slate-400 block text-[7px] uppercase tracking-tighter">Avg Bat</span>
                    </div>
                </div>
                ${mockGpsCount > 0 ? `
                <div class="px-2 py-0.5 bg-rose-500/20 border-b border-rose-500/40 text-rose-400 text-[8px] font-mono font-bold flex items-center justify-between">
                    <span>🚨 INTEGRITY ALERT</span>
                    <span>${mockGpsCount} MOCK GPS</span>
                </div>
                ` : ''}

                <!-- Roster List (Theme Adaptive & Vertically Ultra-Compressed) -->
                <div class="p-1 space-y-0.5 overflow-y-auto custom-scrollbar max-h-[calc(100vh-190px)] font-mono text-[11px]">
                    ${allDevices.map(d => {
                        const selected = isDeviceSelected(d);
                        const isFocused = d.agentId === focusedAgentId;
                        const unified = typeof d.getUnifiedStatus === 'function' ? d.getUnifiedStatus() : { status: d.isOnline ? 'LIVE' : 'OFFLINE', dotClass: d.isOnline ? 'bg-emerald-400' : 'bg-slate-500' };
                        const avatarUrl = typeof d.getAvatarUrl === 'function' ? d.getAvatarUrl() : '/assets/avatars/1.png';
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
                                        <div class="text-[8px] text-slate-500 dark:text-slate-400 truncate flex items-center gap-1">
                                            <span>${d.agentId}</span>
                                            <span>•</span>
                                            <span class="text-emerald-600 dark:text-emerald-400 font-semibold font-sans">📍 ${d.getFormattedDistance ? d.getFormattedDistance() : '--'}</span>
                                            <span>•</span>
                                            <span>${d.battery >= 0 ? `${d.battery}%` : '--'}</span>
                                        </div>
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

                <!-- 🗺️ Standardized Centralized RAG Status Legend Footer -->
                <div class="px-2.5 py-1.5 bg-slate-100/90 dark:bg-slate-900/95 border-t border-slate-200 dark:border-white/10 select-none text-[8.5px] font-mono">
                    <div class="text-[8px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1 flex items-center justify-between">
                        <span>MAP COLOR CODING (RAG)</span>
                        <span class="text-[7.5px] text-sky-500 font-sans">SSOT V3.0</span>
                    </div>
                    <div class="grid grid-cols-2 gap-x-1.5 gap-y-0.5 text-slate-700 dark:text-slate-300">
                        <div class="flex items-center gap-1 truncate" title="Heartbeat / update < 15 minutes">
                            <span class="w-2 h-2 rounded-full bg-emerald-500 shrink-0"></span>
                            <span class="truncate">🟢 LIVE (&lt;15m)</span>
                        </div>
                        <div class="flex items-center gap-1 truncate" title="Last seen 15m - 6h OR Battery ≤ 20%">
                            <span class="w-2 h-2 rounded-full bg-amber-500 shrink-0"></span>
                            <span class="truncate">🟡 IDLE (&lt;6h/Low)</span>
                        </div>
                        <div class="flex items-center gap-1 truncate" title="Last seen > 6 hours">
                            <span class="w-2 h-2 rounded-full bg-rose-500 shrink-0"></span>
                            <span class="truncate">🔴 OFFLINE (&gt;6h)</span>
                        </div>
                        <div class="flex items-center gap-1 truncate" title="Active Emergency SOS alert">
                            <span class="w-2 h-2 rounded-full bg-rose-600 animate-ping shrink-0"></span>
                            <span class="truncate text-rose-500 font-bold">🚨 SOS ALERT</span>
                        </div>
                        <div class="col-span-2 flex items-center gap-1 pt-0.5 border-t border-slate-200/60 dark:border-white/5 truncate" title="Ghost Mode / Unlinked / Frozen privacy mode">
                            <span class="w-2 h-2 rounded-full bg-slate-500 border border-dashed border-slate-300 shrink-0"></span>
                            <span class="truncate text-slate-500 dark:text-slate-400">👻 GHOST / UNLINKED</span>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Renders Right Panel: Leaflet Selected Device Telemetry Card (Collapsible Design)
     * @param {import('../models/DeviceModel.js').DeviceModel} device 
     * @returns {string} HTML string
     */
    static renderDeviceDetailCardHtml(device) {
        if (!device) return '';

        const battery = device.battery >= 0 ? `${device.battery}%` : 'N/A';
        const wifiName = (device.networkType && device.networkType !== 'UNKNOWN') ? device.networkType : (device.isOnline ? 'Cellular / WiFi' : 'Disconnected');
        const lastSeen = device.getFormattedLastSeen();
        const avatarUrl = typeof device.getAvatarUrl === 'function' ? device.getAvatarUrl() : '/assets/avatars/1.png';
        const themeColor = typeof device.getThemeColor === 'function' ? device.getThemeColor() : '#00E5FF';
        const distance = device.getFormattedDistance ? device.getFormattedDistance() : '--';
        const unified = typeof device.getUnifiedStatus === 'function' ? device.getUnifiedStatus() : { status: device.isOnline ? 'LIVE' : 'OFFLINE', badgeClass: device.isOnline ? 'badge-success' : 'badge-danger', dotClass: device.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400', label: device.isOnline ? 'LIVE' : 'OFFLINE' };

        return `
            <div class="leaflet-device-card w-[220px] sm:w-[240px] max-h-[calc(100vh-100px)] select-text pointer-events-auto font-sans animate-fade-in flex flex-col bg-white/96 dark:bg-slate-950/92 backdrop-blur-xl rounded-2xl border border-slate-200 dark:border-white/10 shadow-2xl overflow-hidden text-slate-800 dark:text-slate-100">
                
                <!-- Collapsible Header: Always Visible (Avatar + Name + Distance + Toggle) -->
                <div class="p-2.5 border-b border-slate-100 dark:border-slate-800/80 bg-gradient-to-r from-slate-50 to-transparent dark:from-slate-900/60 dark:to-transparent">
                    <div class="flex items-center gap-2.5">
                        <!-- Avatar -->
                        <div class="relative shrink-0 w-10 h-10 rounded-full p-0.5 border-2 shadow-lg" style="border-color: ${themeColor}">
                            <img src="${avatarUrl}" class="w-full h-full rounded-full object-cover bg-slate-900" alt="${device.name}"
                                 onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                            <span class="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full border-2 border-white dark:border-slate-950 ${unified.dotClass}"></span>
                        </div>

                        <!-- Name & Info -->
                        <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-1.5">
                                <h3 class="text-xs font-black text-slate-900 dark:text-white tracking-tight truncate">${device.name}</h3>
                                <span class="${unified.badgeClass} text-[8px] font-bold font-mono px-1 py-0.5 shrink-0">${unified.label}</span>
                            </div>
                            <div class="text-[9px] text-slate-500 dark:text-slate-400 font-mono truncate mt-0.5">
                                <span class="text-sky-600 dark:text-sky-400 font-bold">${device.agentId}</span> • ${lastSeen}
                            </div>
                            <div class="flex items-center gap-1 mt-0.5">
                                <span class="text-[9px] font-bold text-emerald-600 dark:text-emerald-400">📍 ${distance}</span>
                                <span class="text-[8px] text-slate-400">from base</span>
                            </div>
                        </div>

                        <!-- Expand/Collapse Toggle -->
                        <button id="detail-toggle-${device.agentId}" 
                                class="toggle-btn w-7 h-7 rounded-full bg-slate-100 dark:bg-slate-800 hover:bg-sky-500 hover:text-white border border-slate-200 dark:border-slate-700 flex items-center justify-center transition-all text-sm font-bold"
                                onclick="window.toggleDeviceDetails('${device.agentId}')">
                            ▶
                        </button>
                    </div>

                    <!-- Quick Actions Row (Always Visible) -->
                    <div class="flex items-center gap-1 mt-2 pt-2 border-t border-slate-100 dark:border-slate-800/60">
                        <button class="action-btn flex-1 py-1 rounded-lg bg-slate-100 dark:bg-slate-800/80 hover:bg-sky-50 dark:hover:bg-sky-950/50 text-[9px] font-bold text-slate-600 dark:text-slate-300 hover:text-sky-600 dark:hover:text-sky-400 transition-all"
                                onclick="window.MatrixApp.showDossier('${device.agentId}')" title="Device Dossier">
                            📄 Profile
                        </button>
                        <button class="action-btn flex-1 py-1 rounded-lg bg-slate-100 dark:bg-slate-800/80 hover:bg-emerald-50 dark:hover:bg-emerald-950/50 text-[9px] font-bold text-slate-600 dark:text-slate-300 hover:text-emerald-600 dark:hover:text-emerald-400 transition-all"
                                onclick="window.MatrixApp.showHistory('${device.agentId}')" title="View History Trail">
                            🗺️ Trail
                        </button>
                        <button class="action-btn py-1 rounded-lg bg-slate-100 dark:bg-slate-800/80 hover:bg-rose-50 dark:hover:bg-rose-950/50 text-[9px] font-bold text-slate-600 dark:text-slate-300 hover:text-rose-600 dark:hover:text-rose-400 transition-all"
                                onclick="window.MatrixApp.closeDeviceCard()" title="Close">
                            ✕
                        </button>
                    </div>
                </div>

                <!-- Collapsible Details Section (Hidden by default) -->
                <div id="detail-content-${device.agentId}" class="detail-content hidden">
                    <!-- Divider -->
                    <div class="h-px bg-gradient-to-r from-transparent via-slate-200 dark:via-slate-700 to-transparent my-2"></div>

                    <!-- Geocoded Address -->
                    ${device.address ? `
                    <div class="px-3 py-2 bg-slate-50 dark:bg-slate-900/60 border-b border-slate-100 dark:border-slate-800">
                        <div class="flex items-start gap-1.5">
                            <span class="text-xs text-sky-500 shrink-0">📍</span>
                            <div class="text-[9px] text-slate-600 dark:text-slate-300 line-clamp-2 leading-snug font-medium">${device.address}</div>
                        </div>
                    </div>
                    ` : ''}

                    <!-- Telemetry Grid -->
                    <div class="grid grid-cols-2 gap-1.5 p-3 bg-slate-50/50 dark:bg-slate-900/40 border-b border-slate-100 dark:border-slate-800">
                        <div class="text-center p-1.5 rounded-lg bg-white dark:bg-slate-800/80 border border-slate-100 dark:border-slate-700">
                            <div class="text-[8px] text-slate-400 uppercase font-semibold">Battery</div>
                            <div class="text-xs font-bold ${device.battery <= 20 ? 'text-rose-500' : 'text-emerald-500'}">${battery} ${device.isCharging ? '⚡' : ''}</div>
                        </div>
                        <div class="text-center p-1.5 rounded-lg bg-white dark:bg-slate-800/80 border border-slate-100 dark:border-slate-700">
                            <div class="text-[8px] text-slate-400 uppercase font-semibold">Speed</div>
                            <div class="text-xs font-bold text-amber-600 dark:text-amber-400">${device.getFormattedSpeed ? device.getFormattedSpeed() : '0 km/h'}</div>
                        </div>
                        <div class="text-center p-1.5 rounded-lg bg-white dark:bg-slate-800/80 border border-slate-100 dark:border-slate-700">
                            <div class="text-[8px] text-slate-400 uppercase font-semibold">Precision</div>
                            <div class="text-xs font-bold text-slate-700 dark:text-slate-300">±${device.accuracy ? device.accuracy.toFixed(1) : '5.0'}m</div>
                        </div>
                        <div class="text-center p-1.5 rounded-lg bg-white dark:bg-slate-800/80 border border-slate-100 dark:border-slate-700">
                            <div class="text-[8px] text-slate-400 uppercase font-semibold">Heading</div>
                            <div class="text-xs font-bold text-slate-700 dark:text-slate-300">${Math.round(device.bearing || 0)}°</div>
                        </div>
                    </div>

                    <!-- Micro Sensor Truth -->
                    <div class="p-3 bg-slate-50 dark:bg-slate-900/60 border-b border-slate-100 dark:border-slate-800">
                        <div class="text-[8px] font-bold uppercase tracking-wider text-sky-500 mb-1.5">🐜 GPS Ground Truth</div>
                        <div class="space-y-1 font-mono text-[9px]">
                            <div class="flex justify-between text-slate-500 dark:text-slate-400">
                                <span>Coordinates:</span>
                                <span class="text-slate-700 dark:text-slate-200 font-bold">${device.latitude ? `${Number(device.latitude).toFixed(4)}°, ${Number(device.longitude).toFixed(4)}°` : '--'}</span>
                            </div>
                            <div class="flex justify-between text-slate-500 dark:text-slate-400">
                                <span>Altitude:</span>
                                <span class="text-slate-700 dark:text-slate-200 font-bold">${device.altitude ? `${Number(device.altitude).toFixed(0)}m` : '0m'}</span>
                            </div>
                            <div class="flex justify-between text-slate-500 dark:text-slate-400">
                                <span>Signal:</span>
                                <span class="${device.isMockGps || device.isMock ? 'text-rose-400' : 'text-emerald-400'} font-bold">
                                    ${device.isMockGps || device.isMock ? '🚨 SPOOFED' : '🛡️ HARDWARE'}
                                </span>
                            </div>
                        </div>
                    </div>

                    <!-- Action Buttons Strip -->
                    <div class="grid grid-cols-4 gap-1 p-2.5">
                        <button class="flex flex-col items-center justify-center p-1.5 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30 transition-all"
                                onclick="window.MatrixApp.pokeDevice('${device.agentId}')" title="Dispatches silent high-priority FCM wake burst to shake device from Doze Mode">
                            <span class="text-sm">⚡</span>
                            <span class="text-[9px] font-bold mt-0.5">Wake</span>
                        </button>
                        <button class="flex flex-col items-center justify-center p-1.5 rounded-xl bg-slate-100 dark:bg-slate-800/80 hover:bg-sky-50 dark:hover:bg-sky-950/50 text-slate-700 dark:text-slate-200 hover:text-sky-600 dark:hover:text-sky-400 border border-slate-200 dark:border-slate-700 transition-all"
                                onclick="window.FBBridge.pokeDevice('${device.agentId}', 'RING_ALARM')" title="Triggers remote ring beacon alarm">
                            <span class="text-sm">🔊</span>
                            <span class="text-[9px] font-bold mt-0.5">Ring</span>
                        </button>
                        <button class="flex flex-col items-center justify-center p-1.5 rounded-xl bg-slate-100 dark:bg-slate-800/80 hover:bg-emerald-50 dark:hover:bg-emerald-950/50 text-slate-700 dark:text-slate-200 hover:text-emerald-600 dark:hover:text-emerald-400 border border-slate-200 dark:border-slate-700 transition-all"
                                onclick="window.MatrixApp.showHistory('${device.agentId}')">
                            <span class="text-sm">🗺️</span>
                            <span class="text-[9px] font-bold mt-0.5">Trail</span>
                        </button>
                        <button class="flex flex-col items-center justify-center p-1.5 rounded-xl bg-slate-100 dark:bg-slate-800/80 hover:bg-amber-50 dark:hover:bg-amber-950/50 text-slate-700 dark:text-slate-200 hover:text-amber-600 dark:hover:text-amber-400 border border-slate-200 dark:border-slate-700 transition-all"
                                onclick="window.MatrixApp.openEditProfile('${device.agentId}')">
                            <span class="text-sm">✏️</span>
                            <span class="text-[9px] font-bold mt-0.5">Edit</span>
                        </button>
                    </div>
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
                
                <!-- RIGHT: Leaflet Device Telemetry Card with Avatar (Positioned clear of zoom controls) -->
                <div class="pointer-events-auto leaflet-device-card-container mr-12 sm:mr-14">
                    ${device ? this.renderDeviceDetailCardHtml(device) : ''}
                </div>
            </div>
        `;
    }

    /**
     * Toggle device details panel visibility (global function for onclick handlers)
     * @param {string} agentId 
     */
    static toggleDetails(agentId) {
        const content = document.getElementById(`detail-content-${agentId}`);
        const btn = document.getElementById(`detail-toggle-${agentId}`);
        
        if (content && btn) {
            const isHidden = content.classList.contains('hidden');
            
            if (isHidden) {
                content.classList.remove('hidden');
                btn.textContent = '▼';
                btn.classList.add('bg-sky-500', 'text-white');
            } else {
                content.classList.add('hidden');
                btn.textContent = '▶';
                btn.classList.remove('bg-sky-500', 'text-white');
            }
        }
    }

    /**
     * Generates jButterfly tactical circular pin HTML with glowing halo, device name, and avatar
     * @param {import('../models/DeviceModel.js').DeviceModel} device 
     * @returns {string} HTML markup
     */
    static renderTacticalPinHtml(device) {
        if (!device) return '';
        const isGhost = Boolean(device.isGhostMode || device.ragStatus === 'GHOST' || device.sessionState === 'GHOST' || device.sessionState === 'UNINSTALLED');
        const themeColor = isGhost ? '#64748B' : (device.getThemeColor ? device.getThemeColor() : '#00E5FF');
        const avatarUrl = device.getAvatarUrl ? device.getAvatarUrl() : 'data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>';
        const isWithin = device.isWithinSearchRadius !== undefined ? device.isWithinSearchRadius : true;
        const bearing = Math.round(device.bearing || 0);

        // Standardized Centralized RAG Status Engine Integration (Agent 2 & 3)
        const unified = typeof device.getUnifiedStatus === 'function' ? device.getUnifiedStatus() : {
            color: isGhost ? '#64748B' : (device.ragColor || (device.isOnline ? '#22C55E' : '#EF4444')),
            dotClass: isGhost ? 'bg-slate-500' : (device.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-500')
        };
        const ragFontColor = isGhost ? '#94A3B8' : (unified.color || (device.isOnline ? '#22C55E' : '#EF4444'));

        return `
            <div class="matrix-marker-wrapper ${isWithin ? 'in-radar' : 'out-radar'} ${isGhost ? 'ghost-mode' : (device.isOnline ? 'online' : 'offline')} flex flex-col items-center select-none cursor-pointer">
                <!-- Top Name Callout Pill (Role Color Border & RAG Font Color) -->
                <div class="marker-callout-pill" style="border: 1.5px ${isGhost ? 'dashed' : 'solid'} ${themeColor}; background: rgba(8, 12, 22, 0.95); backdrop-filter: blur(8px); border-radius: 9999px; padding: 2px 8px; font-size: 10px; font-weight: 800; color: ${ragFontColor}; white-space: nowrap; box-shadow: 0 2px 6px rgba(0,0,0,0.5); margin-bottom: 2px; pointer-events: none; max-width: 120px; overflow: hidden; text-overflow: ellipsis;">
                    <span class="marker-name-text">${isGhost ? '👻 ' : ''}${device.name}</span>
                </div>

                <!-- Clean Direct Avatar Container (Strict 50% Circular Mask, No Black Box) -->
                <div class="marker-avatar-container relative w-10 h-10 rounded-full bg-transparent p-0 shadow-md transition-transform hover:scale-105" 
                     style="border: 2px ${isGhost ? 'dashed' : 'solid'} ${themeColor}; ${isGhost ? 'opacity: 0.75;' : ''}">
                    <img src="${avatarUrl}" class="w-full h-full rounded-full object-cover bg-transparent" alt="${device.name}"
                         style="border-radius: 50% !important; clip-path: circle(50% at 50% 50%) !important; -webkit-clip-path: circle(50% at 50% 50%) !important;"
                         onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><circle cx=%2250%22 cy=%2250%22 r=%2248%22 fill=%22%23080c14%22/><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                    
                    <!-- Live Status Dot (5-Stage Centralized RAG Status) -->
                    <span class="absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full border-2 border-slate-950 ${isGhost ? 'bg-slate-500' : unified.dotClass}"></span>
                </div>

                <!-- Motion Heading Chevron (Clean Directional Vector) -->
                ${!isGhost && device.speed > 0.8 ? `<div class="w-0 h-0 border-l-[4px] border-l-transparent border-r-[4px] border-r-transparent border-b-[6px] mt-0.5" style="transform: rotate(${bearing}deg); border-bottom-color: ${themeColor};"></div>` : ''}

                <!-- Pin Anchor Point -->
                <div class="w-0 h-0 border-l-[4px] border-l-transparent border-r-[4px] border-r-transparent border-t-[5px]" style="border-top-color: ${themeColor}; margin-top: -1px;"></div>
            </div>
        `;
    }
}

// SSOT Alias for backwards compatibility
export const GoogleFindHubView = jButterflyHubView;

// Register global toggle function
window.toggleDeviceDetails = (agentId) => {
    jButterflyHubView.toggleDetails(agentId);
};
