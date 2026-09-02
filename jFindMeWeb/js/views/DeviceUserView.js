import { GeoUtils } from '../core/GeoUtils.js';

/**
 * jFindMe Matrix - Device User 5-Pane Inspection & Management Workspace (OOPS Architecture)
 * Panes:
 * 1. Device Selection & Search Roster
 * 2. Profile & Expandable/Collapsible Org Data
 * 3. Family & Friends Connection Tree View
 * 4. Safe Zones & Notification History Audit
 * 5. Location Event & Spatial Movement Log
 */
export class DeviceUserView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.onSelectDevice = options.onSelectDevice || null;
        this.onNavigateMap = options.onNavigateMap || null;
        
        this.devices = [];
        this.selectedAgentId = null;
        this.searchQuery = "";
        this.isOrgExpanded = true;
    }

    /**
     * Renders the 5-Pane Device User Interface
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices 
     * @param {string} initialSelectedAgentId 
     */
    render(devices = [], initialSelectedAgentId = null) {
        this.devices = devices;
        if (devices.length > 0 && !this.selectedAgentId) {
            this.selectedAgentId = initialSelectedAgentId || devices[0].agentId;
        }

        const container = document.getElementById(this.containerId);
        if (!container) return;

        // Filter devices by search query
        let filteredDevices = this.devices;
        if (this.searchQuery && this.searchQuery.trim().length > 0) {
            const q = this.searchQuery.trim().toLowerCase();
            filteredDevices = this.devices.filter(d => {
                return (d.name && d.name.toLowerCase().includes(q)) ||
                       (d.agentId && d.agentId.toLowerCase().includes(q)) ||
                       (d.model && d.model.toLowerCase().includes(q)) ||
                       (d.deviceName && d.deviceName.toLowerCase().includes(q)) ||
                       (d.role && d.role.toLowerCase().includes(q)) ||
                       (d.address && d.address.toLowerCase().includes(q));
            });
        }

        // Selected Device Object
        const selectedDev = this.devices.find(d => d.agentId === this.selectedAgentId) || this.devices[0];

        container.innerHTML = `
            <div class="h-full bg-[#05070a] overflow-hidden flex flex-col text-white select-none">
                
                <!-- HEADER BAR -->
                <div class="bg-slate-900/90 border-b border-white/10 px-5 py-3 flex items-center justify-between shadow-xl shrink-0">
                    <div class="flex items-center gap-3">
                        <div class="w-9 h-9 rounded-xl bg-sky-500/20 border border-sky-500/40 flex items-center justify-center text-sky-400 font-bold text-lg">
                            📱
                        </div>
                        <div>
                            <h2 class="text-sm font-black tracking-tight text-white uppercase flex items-center gap-2">
                                <span>Device User Oversight Matrix</span>
                                <span class="px-2 py-0.5 rounded-full bg-sky-500/20 text-sky-300 text-[10px] font-mono border border-sky-500/30">5-PANE OOPS</span>
                            </h2>
                            <p class="text-[10px] text-slate-400 font-mono">
                                Selected Node: <span class="text-sky-400 font-bold">${selectedDev ? selectedDev.name : 'None'}</span> (${selectedDev ? selectedDev.agentId : 'N/A'})
                            </p>
                        </div>
                    </div>

                    ${selectedDev ? `
                    <div class="flex items-center gap-2">
                        <button id="btn-du-view-map" class="px-3.5 py-1.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold transition-all flex items-center gap-1.5 shadow-lg shadow-sky-600/30">
                            <span>🗺️ Locate on Map</span>
                        </button>
                    </div>
                    ` : ''}
                </div>

                <!-- 5-PANE WORKSPACE LAYOUT (Scrollable Grid) -->
                <div class="flex-1 overflow-y-auto custom-scrollbar p-4">
                    <div class="grid grid-cols-1 lg:grid-cols-5 gap-4 min-h-[600px]">
                        
                        <!-- PANE 1: DEVICE SELECTION ROSTER & SEARCH BAR -->
                        <div class="bg-slate-900/90 rounded-2xl border border-white/10 p-3.5 flex flex-col space-y-3 shadow-lg">
                            <div class="flex items-center justify-between border-b border-white/10 pb-2">
                                <span class="text-xs font-black uppercase text-sky-400 tracking-wider">1. Select Device</span>
                                <span class="px-2 py-0.5 rounded-full bg-white/10 text-[10px] font-mono font-bold">${filteredDevices.length}/${this.devices.length}</span>
                            </div>

                            <!-- Search Input -->
                            <div class="relative flex items-center">
                                <input type="text" id="du-search-input" 
                                       placeholder="🔍 Search name, ID, role..." 
                                       value="${this.searchQuery}"
                                       class="w-full bg-slate-950 border border-white/10 rounded-xl pl-8 pr-7 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-sky-500 font-mono">
                                <svg class="w-3.5 h-3.5 absolute left-2.5 text-slate-500 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                                </svg>
                                ${this.searchQuery ? `
                                    <button id="btn-du-clear-search" class="absolute right-2 text-slate-400 hover:text-white text-xs font-bold p-1">✕</button>
                                ` : ''}
                            </div>

                            <!-- Device List -->
                            <div class="flex-1 space-y-2 overflow-y-auto custom-scrollbar max-h-[500px]">
                                ${filteredDevices.map(dev => `
                                    <div class="du-device-item cursor-pointer p-2.5 rounded-xl border ${dev.agentId === this.selectedAgentId ? 'border-sky-400 bg-sky-950/40 text-white shadow-md' : 'border-white/5 bg-black/40 hover:bg-white/5 text-slate-300'} transition-all flex items-center gap-2.5"
                                         data-agent-id="${dev.agentId}">
                                        <div class="relative shrink-0">
                                            <img src="${dev.getAvatarUrl()}" alt="${dev.name}" 
                                                 class="w-8 h-8 rounded-full object-cover border" 
                                                 style="border-color: ${dev.getThemeColor()};">
                                            <span class="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full border border-slate-900 ${dev.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-500'}"></span>
                                        </div>
                                        <div class="min-w-0 flex-1">
                                            <div class="flex items-center justify-between">
                                                <h4 class="text-xs font-bold truncate">${dev.name}</h4>
                                                <span class="text-[8px] font-mono font-bold px-1 rounded ${dev.isOnline ? 'bg-emerald-500/20 text-emerald-400' : 'bg-slate-800 text-slate-400'}">${dev.isOnline ? 'LIVE' : 'OFF'}</span>
                                            </div>
                                            <div class="text-[9px] text-slate-400 truncate font-mono">${dev.deviceName || dev.model} • ${dev.agentId}</div>
                                        </div>
                                    </div>
                                `).join('')}
                            </div>
                        </div>

                        ${selectedDev ? `
                        <!-- PANE 2: PROFILE & EXPANDABLE ORG DATA -->
                        <div class="bg-slate-900/90 rounded-2xl border border-white/10 p-3.5 space-y-3 shadow-lg">
                            <div class="flex items-center justify-between border-b border-white/10 pb-2">
                                <span class="text-xs font-black uppercase text-sky-400 tracking-wider">2. Profile & Org Data</span>
                                <button id="btn-toggle-org" class="text-[10px] text-sky-400 hover:underline font-mono">
                                    ${this.isOrgExpanded ? 'Collapse Org [-]' : 'Expand Org [+]'}
                                </button>
                            </div>

                            <!-- Profile Card -->
                            <div class="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2 text-xs font-mono">
                                <div class="flex items-center gap-3 pb-2 border-b border-white/5">
                                    <img src="${selectedDev.getAvatarUrl()}" alt="${selectedDev.name}" class="w-12 h-12 rounded-full object-cover border-2 shadow" style="border-color: ${selectedDev.getThemeColor()};">
                                    <div>
                                        <h3 class="font-bold text-sm text-white">${selectedDev.name}</h3>
                                        <div class="text-[10px] text-sky-400 font-sans font-bold">${selectedDev.roleFamily !== 'None' ? selectedDev.roleFamily : selectedDev.role}</div>
                                        <div class="text-[9px] text-slate-400">ID: ${selectedDev.agentId}</div>
                                    </div>
                                </div>

                                <div class="space-y-1.5 pt-1 text-[11px]">
                                    <div class="flex justify-between"><span class="text-slate-400">Hardware HW:</span> <span class="text-slate-200 font-bold">${selectedDev.deviceId}</span></div>
                                    <div class="flex justify-between"><span class="text-slate-400">Model:</span> <span class="text-slate-200">${selectedDev.model || 'Android Device'}</span></div>
                                    <div class="flex justify-between"><span class="text-slate-400">Phone:</span> <span class="text-slate-200">${selectedDev.phone || 'N/A'}</span></div>
                                    <div class="flex justify-between"><span class="text-slate-400">Email:</span> <span class="text-slate-200 truncate max-w-[120px]">${selectedDev.email || 'N/A'}</span></div>
                                    <div class="flex justify-between"><span class="text-slate-400">Emergency:</span> <span class="text-amber-300">${selectedDev.emergencyNote || 'None'}</span></div>
                                </div>
                            </div>

                            <!-- Expandable Org Data -->
                            <div id="org-data-panel" class="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2 text-xs font-mono ${this.isOrgExpanded ? 'block' : 'hidden'}">
                                <div class="text-[10px] font-bold text-sky-400 uppercase tracking-wider font-sans border-b border-white/5 pb-1">🏢 Organization Data & ACL</div>
                                <div class="space-y-1 text-[10px]">
                                    <div class="flex justify-between"><span class="text-slate-400">Department:</span> <span class="text-white">${selectedDev.department || 'General Fleet'}</span></div>
                                    <div class="flex justify-between"><span class="text-slate-400">Role Title:</span> <span class="text-white">${selectedDev.role}</span></div>
                                    <div class="pt-1 text-[9px] text-slate-400 uppercase font-sans font-bold">Permissions:</div>
                                    <div class="grid grid-cols-2 gap-1 text-[9px]">
                                        <span class="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">Live Tracking</span>
                                        <span class="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">Geofence</span>
                                        <span class="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">History Audit</span>
                                        <span class="px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 border border-white/10">Ghost Override</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- PANE 3: FAMILY & FRIENDS TREE VIEW -->
                        <div class="bg-slate-900/90 rounded-2xl border border-white/10 p-3.5 space-y-3 shadow-lg">
                            <div class="flex items-center justify-between border-b border-white/10 pb-2">
                                <span class="text-xs font-black uppercase text-sky-400 tracking-wider">3. Connections Tree</span>
                                <span class="px-2 py-0.5 rounded-full bg-white/10 text-[10px] font-mono">TREE VIEW</span>
                            </div>

                            <!-- Connection Tree Container -->
                            <div class="bg-black/50 p-3 rounded-xl border border-white/5 space-y-3 min-h-[350px] font-mono">
                                <!-- Root Node -->
                                <div class="flex items-center gap-2 p-2 rounded-xl bg-sky-950/60 border border-sky-500/40 text-white">
                                    <span class="text-base">👑</span>
                                    <div>
                                        <div class="text-xs font-bold">${selectedDev.name}</div>
                                        <div class="text-[9px] text-sky-300">Root Node (${selectedDev.agentId})</div>
                                    </div>
                                </div>

                                <!-- Tree Branches -->
                                <div class="pl-4 border-l-2 border-sky-500/30 space-y-3">
                                    <!-- Branch 1: Family -->
                                    <div class="space-y-1.5">
                                        <div class="text-[10px] font-bold text-amber-300 font-sans uppercase flex items-center gap-1">
                                            <span>👨‍👩‍👧 Family Group</span>
                                        </div>
                                        <div class="space-y-1 pl-2">
                                            ${this.renderTreeMembers(selectedDev, 'family')}
                                        </div>
                                    </div>

                                    <!-- Branch 2: Friends -->
                                    <div class="space-y-1.5">
                                        <div class="text-[10px] font-bold text-emerald-300 font-sans uppercase flex items-center gap-1">
                                            <span>🤝 Friends Group</span>
                                        </div>
                                        <div class="space-y-1 pl-2">
                                            ${this.renderTreeMembers(selectedDev, 'friends')}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- PANE 4: SAFEZONES & NOTIFICATIONS -->
                        <div class="bg-slate-900/90 rounded-2xl border border-white/10 p-3.5 space-y-3 shadow-lg">
                            <div class="flex items-center justify-between border-b border-white/10 pb-2">
                                <span class="text-xs font-black uppercase text-sky-400 tracking-wider">4. Safezones & Alerts</span>
                                <span class="px-2 py-0.5 rounded-full bg-white/10 text-[10px] font-mono">${selectedDev.safeZonesCount} Zones</span>
                            </div>

                            <div class="space-y-3">
                                <!-- Safezones Card -->
                                <div class="bg-black/50 p-2.5 rounded-xl border border-white/5 space-y-1.5">
                                    <div class="text-[10px] font-bold text-sky-400 uppercase font-sans">Assigned Perimeters</div>
                                    <div class="space-y-1 max-h-[160px] overflow-y-auto custom-scrollbar">
                                        ${this.renderSafeZoneItems(selectedDev)}
                                    </div>
                                </div>

                                <!-- Geofence Notifications Stream -->
                                <div class="bg-black/50 p-2.5 rounded-xl border border-white/5 space-y-1.5">
                                    <div class="text-[10px] font-bold text-amber-400 uppercase font-sans">Notification History</div>
                                    <div class="space-y-1 text-[9px] font-mono max-h-[220px] overflow-y-auto custom-scrollbar">
                                        ${this.renderNotificationItems(selectedDev)}
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- PANE 5: LOCATION HISTORY LOG -->
                        <div class="bg-slate-900/90 rounded-2xl border border-white/10 p-3.5 space-y-3 shadow-lg">
                            <div class="flex items-center justify-between border-b border-white/10 pb-2">
                                <span class="text-xs font-black uppercase text-sky-400 tracking-wider">5. Location Log</span>
                                <span class="px-2 py-0.5 rounded-full bg-white/10 text-[10px] font-mono">SPATIAL</span>
                            </div>

                            <div class="bg-black/50 p-2.5 rounded-xl border border-white/5 space-y-2 min-h-[380px]">
                                <div class="text-[10px] font-bold text-sky-400 uppercase font-sans border-b border-white/5 pb-1">Recorded Fixes & Trails</div>
                                <div class="space-y-2 max-h-[480px] overflow-y-auto custom-scrollbar text-xs font-mono">
                                    ${this.renderLocationLogItems(selectedDev)}
                                </div>
                            </div>
                        </div>

                        ` : ''}

                    </div>
                </div>
            </div>
        `;

        this.bindEvents();
    }

    renderTreeMembers(device, type) {
        // Find other devices in the fleet to render tree connections
        const peers = this.devices.filter(d => d.agentId !== device.agentId);
        if (peers.length === 0) {
            return `<div class="text-[9px] text-slate-500 italic">No connected ${type} nodes</div>`;
        }

        return peers.slice(0, 3).map(p => `
            <div class="flex items-center justify-between p-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-[10px] border border-white/5 cursor-pointer"
                 onclick="window.showDeviceUser('${p.agentId}')">
                <div class="flex items-center gap-1.5 min-w-0">
                    <img src="${p.getAvatarUrl()}" class="w-4 h-4 rounded-full object-cover">
                    <span class="truncate text-white font-bold">${p.name}</span>
                </div>
                <span class="text-[8px] font-mono text-slate-400 font-bold">${type === 'family' ? (p.roleFamily !== 'None' ? p.roleFamily : 'Family') : 'Friend'}</span>
            </div>
        `).join('');
    }

    renderSafeZoneItems(device) {
        return `
            <div class="p-2 rounded-lg bg-sky-950/30 border border-sky-500/20 text-[10px] flex items-center justify-between">
                <div>
                    <div class="font-bold text-white">Vibgyor School Zone</div>
                    <div class="text-[8px] text-slate-400 font-mono">Radius: 100m • Lat: 18.5592° N</div>
                </div>
                <span class="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 text-[8px] font-bold">ACTIVE</span>
            </div>
            <div class="p-2 rounded-lg bg-white/5 border border-white/5 text-[10px] flex items-center justify-between">
                <div>
                    <div class="font-bold text-white">Home Safe Zone</div>
                    <div class="text-[8px] text-slate-400 font-mono">Radius: 200m • Lat: 18.5788° N</div>
                </div>
                <span class="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 text-[8px] font-bold">ACTIVE</span>
            </div>
        `;
    }

    renderNotificationItems(device) {
        return `
            <div class="p-1.5 rounded bg-white/5 border border-white/5 space-y-0.5">
                <div class="flex items-center justify-between text-sky-300 font-bold">
                    <span>🟢 ENTERED Vibgyor</span>
                    <span class="text-[8px] text-slate-400">${device.getFormattedLastSeen()}</span>
                </div>
                <div class="text-slate-300 text-[8px]">Device ${device.name} entered safe zone perimeter.</div>
            </div>
            <div class="p-1.5 rounded bg-white/5 border border-white/5 space-y-0.5">
                <div class="flex items-center justify-between text-amber-300 font-bold">
                    <span>🔴 EXITED Home Zone</span>
                    <span class="text-[8px] text-slate-400">Yesterday</span>
                </div>
                <div class="text-slate-300 text-[8px]">Device ${device.name} exited safe zone perimeter.</div>
            </div>
        `;
    }

    renderLocationLogItems(device) {
        if (!device.hasValidLocation) {
            return `<div class="text-[10px] text-slate-500 italic py-4 text-center">No active GPS location fixes recorded.</div>`;
        }

        return `
            <div class="p-2 rounded-xl bg-sky-950/40 border border-sky-500/30 space-y-1">
                <div class="flex items-center justify-between text-[9px] text-sky-300 font-bold">
                    <span>🕒 ${device.getFormattedDateTime()}</span>
                    <span class="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 text-[8px]">LATEST</span>
                </div>
                <div class="text-white text-[11px] font-bold">${device.getFormattedCoords()}</div>
                <div class="text-[9px] text-slate-300 leading-snug">📍 ${device.address || 'No street address recorded.'}</div>
                <div class="flex items-center justify-between text-[8px] text-slate-400 pt-0.5 border-t border-white/5">
                    <span>Speed: ${device.getFormattedSpeed()}</span>
                    <span>Bearing: ${Math.round(device.bearing || 0)}°</span>
                </div>
            </div>
        `;
    }

    bindEvents() {
        // Device Item Selection
        document.querySelectorAll('.du-device-item').forEach(el => {
            el.addEventListener('click', () => {
                const agentId = el.getAttribute('data-agent-id');
                this.selectedAgentId = agentId;
                this.render(this.devices, agentId);
            });
        });

        // Search Input
        const searchInput = document.getElementById('du-search-input');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.searchQuery = e.target.value;
                this.render(this.devices, this.selectedAgentId);
                const nextInput = document.getElementById('du-search-input');
                if (nextInput) {
                    nextInput.focus();
                    nextInput.setSelectionRange(nextInput.value.length, nextInput.value.length);
                }
            });
        }

        document.getElementById('btn-du-clear-search')?.addEventListener('click', () => {
            this.searchQuery = "";
            this.render(this.devices, this.selectedAgentId);
        });

        // Org Expansion Toggle
        document.getElementById('btn-toggle-org')?.addEventListener('click', () => {
            this.isOrgExpanded = !this.isOrgExpanded;
            this.render(this.devices, this.selectedAgentId);
        });

        // Locate on Map Button
        document.getElementById('btn-du-view-map')?.addEventListener('click', () => {
            if (this.onNavigateMap && this.selectedAgentId) {
                this.onNavigateMap(this.selectedAgentId);
            }
        });
    }
}
