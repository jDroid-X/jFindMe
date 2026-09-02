import { GeoUtils } from '../core/GeoUtils.js';

/**
 * jFindMe Matrix - Device User 5-Pane Inspection & Management Workspace (OOPS Architecture)
 * Hierarchical Data Flow:
 * - Master Data: User Identity & Profile (Name, Avatar, Auth UID, Phone, Email, Role)
 * - Sub-Master Data: Hardware Device (Agent ID, Device ID, Model, IMEI, Battery)
 * - Sub-Master Relational: Family & Friends Connection Tree (Linked Nodes)
 * - Sub-Master Perimeters & Transactional Alerts: Assigned Safe Zones & Geofence Logs
 * - Transactional Data: Location History & Fixes
 */
export class DeviceUserView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.onSelectDevice = options.onSelectDevice || null;
        this.onNavigateMap = options.onNavigateMap || null;
        
        this.devices = [];
        this.selectedAgentId = null;
        this.searchQuery = "";
        this.isOrgExpanded = false; // Default collapsed for 50% compact height
    }

    /**
     * Renders the 5-Pane Device User Interface
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices 
     * @param {string} initialSelectedAgentId 
     */
    render(devices = [], initialSelectedAgentId = null) {
        this.devices = devices;
        if (devices.length > 0) {
            if (!this.selectedAgentId || !this.devices.some(d => d.agentId === this.selectedAgentId)) {
                this.selectedAgentId = initialSelectedAgentId || devices[0].agentId;
            }
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

        // Selected Device Object (Master & Sub-Master Source)
        const selectedDev = this.devices.find(d => d.agentId === this.selectedAgentId) || this.devices[0];
        const rawNode = selectedDev ? (selectedDev.raw || {}) : {};

        container.innerHTML = `
            <div class="w-full min-h-screen bg-[#05070a] text-slate-200 select-none overflow-y-auto custom-scrollbar p-2 font-sans">
                
                <!-- COMPACT HEADER BAR (50% reduced padding) -->
                <div class="bg-slate-900/90 border border-white/10 rounded-xl px-3 py-1.5 mb-2 flex items-center justify-between shadow-lg">
                    <div class="flex items-center gap-2">
                        <div class="w-7 h-7 rounded-lg bg-sky-500/20 border border-sky-500/40 flex items-center justify-center text-sky-400 font-bold text-sm">
                            📱
                        </div>
                        <div>
                            <h2 class="text-xs font-black tracking-wide text-white uppercase flex items-center gap-2">
                                <span>Device User Oversight Matrix</span>
                                <span class="px-1.5 py-0.2 rounded bg-sky-500/20 text-sky-300 text-[9px] font-mono border border-sky-500/30">MASTER-RELATIONAL</span>
                            </h2>
                            <p class="text-[9px] text-slate-400 font-mono">
                                Selected Master Node: <span class="text-sky-400 font-bold">${selectedDev ? selectedDev.name : 'None'}</span> (${selectedDev ? selectedDev.agentId : 'N/A'})
                            </p>
                        </div>
                    </div>

                    ${selectedDev ? `
                    <div class="flex items-center gap-1.5">
                        <button id="btn-du-view-map" class="px-2.5 py-1 rounded-lg bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold transition-all flex items-center gap-1 shadow-md shadow-sky-600/20">
                            <span>🗺️ Locate on Map</span>
                        </button>
                    </div>
                    ` : ''}
                </div>

                <!-- 5-PANE MULTI-COLUMN WORKSPACE (Responsive & Page Scrollable) -->
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-2">
                    
                    <!-- PANE 1: MASTER DEVICE ROSTER & SEARCH BAR -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 flex flex-col space-y-2 shadow-md">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1">
                            <span class="text-xs font-black uppercase text-sky-400 tracking-wider">1. Master Roster</span>
                            <span class="px-1.5 py-0.2 rounded bg-white/10 text-[9px] font-mono font-bold">${filteredDevices.length}/${this.devices.length}</span>
                        </div>

                        <!-- Search Input -->
                        <div class="relative flex items-center">
                            <input type="text" id="du-search-input" 
                                   placeholder="🔍 Search name, ID, role..." 
                                   value="${this.searchQuery}"
                                   class="w-full bg-slate-950 border border-white/10 rounded-lg pl-7 pr-6 py-1 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-sky-500 font-mono">
                            <svg class="w-3 h-3 absolute left-2 text-slate-500 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                            </svg>
                            ${this.searchQuery ? `
                                <button id="btn-du-clear-search" class="absolute right-1.5 text-slate-400 hover:text-white text-xs font-bold p-0.5">✕</button>
                            ` : ''}
                        </div>

                        <!-- Device Roster List -->
                        <div class="space-y-1 overflow-y-auto custom-scrollbar max-h-[450px]">
                            ${filteredDevices.map(dev => `
                                <div class="du-device-item cursor-pointer p-1.5 rounded-lg border ${dev.agentId === this.selectedAgentId ? 'border-sky-400 bg-sky-950/50 text-white shadow' : 'border-white/5 bg-black/40 hover:bg-white/5 text-slate-300'} transition-all flex items-center gap-2"
                                     data-agent-id="${dev.agentId}">
                                    <div class="relative shrink-0">
                                        <img src="${dev.getAvatarUrl()}" alt="${dev.name}" 
                                             class="w-7 h-7 rounded-full object-cover border" 
                                             style="border-color: ${dev.getThemeColor()};">
                                        <span class="absolute bottom-0 right-0 w-2 h-2 rounded-full border border-slate-900 ${dev.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-500'}"></span>
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
                    <!-- PANE 2: MASTER & SUB-MASTER PROFILE DATA (EXPANDABLE ORG) -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 space-y-2 shadow-md">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1">
                            <span class="text-xs font-black uppercase text-sky-400 tracking-wider">2. Master Profile</span>
                            <button id="btn-toggle-org" class="text-[9px] text-sky-400 hover:underline font-mono">
                                ${this.isOrgExpanded ? 'Collapse Org [-]' : 'Expand Org [+]'}
                            </button>
                        </div>

                        <!-- Master User Identity Card -->
                        <div class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-1.5 text-xs font-mono">
                            <div class="flex items-center gap-2 pb-1.5 border-b border-white/5">
                                <img src="${selectedDev.getAvatarUrl()}" alt="${selectedDev.name}" class="w-9 h-9 rounded-full object-cover border shadow" style="border-color: ${selectedDev.getThemeColor()};">
                                <div class="min-w-0 flex-1">
                                    <h3 class="font-bold text-xs text-white truncate">${selectedDev.name}</h3>
                                    <div class="text-[9px] text-sky-400 font-sans font-bold">${selectedDev.roleFamily !== 'None' ? selectedDev.roleFamily : selectedDev.role}</div>
                                    <div class="text-[8px] text-slate-400">Agent ID: ${selectedDev.agentId}</div>
                                </div>
                            </div>

                            <div class="space-y-1 text-[10px]">
                                <div class="flex justify-between"><span class="text-slate-400">Auth UID:</span> <span class="text-slate-200 font-bold truncate max-w-[110px]">${(rawNode.Identity && rawNode.Identity.auth_uid) || selectedDev.agentId}</span></div>
                                <div class="flex justify-between"><span class="text-slate-400">Hardware ID:</span> <span class="text-slate-200 font-bold truncate max-w-[110px]">${selectedDev.deviceId}</span></div>
                                <div class="flex justify-between"><span class="text-slate-400">Device Model:</span> <span class="text-white">${selectedDev.model || 'Android'}</span></div>
                                <div class="flex justify-between"><span class="text-slate-400">Phone:</span> <span class="text-slate-200">${selectedDev.phone || 'N/A'}</span></div>
                                <div class="flex justify-between"><span class="text-slate-400">Email:</span> <span class="text-slate-200 truncate max-w-[110px]">${selectedDev.email || 'N/A'}</span></div>
                                <div class="flex justify-between"><span class="text-slate-400">Emergency Note:</span> <span class="text-amber-300 font-bold">${selectedDev.emergencyNote || 'None'}</span></div>
                            </div>
                        </div>

                        <!-- Sub-Master Expandable Org Data -->
                        <div id="org-data-panel" class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-1.5 text-xs font-mono ${this.isOrgExpanded ? 'block' : 'hidden'}">
                            <div class="text-[9px] font-bold text-sky-400 uppercase tracking-wider font-sans border-b border-white/5 pb-0.5">🏢 Org & ACL Metadata</div>
                            <div class="space-y-0.5 text-[9px]">
                                <div class="flex justify-between"><span class="text-slate-400">Organization:</span> <span class="text-white">${(rawNode.profile && rawNode.profile.orgName) || 'jFindMe Matrix'}</span></div>
                                <div class="flex justify-between"><span class="text-slate-400">Department:</span> <span class="text-white">${selectedDev.department || 'Fleet Ops'}</span></div>
                                <div class="flex justify-between"><span class="text-slate-400">Role Title:</span> <span class="text-white">${selectedDev.role}</span></div>
                                <div class="pt-0.5 text-[8px] text-slate-400 uppercase font-sans font-bold">ACL Grants:</div>
                                <div class="grid grid-cols-2 gap-1 text-[8px]">
                                    <span class="px-1 py-0.2 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">Live Track</span>
                                    <span class="px-1 py-0.2 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">Geofence</span>
                                    <span class="px-1 py-0.2 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">History</span>
                                    <span class="px-1 py-0.2 rounded bg-slate-800 text-slate-400 border border-white/10">Ghost</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 3: SUB-MASTER RELATIONAL TREE VIEW (INTER-RELATIONSHIP FILTERING) -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 space-y-2 shadow-md">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1">
                            <span class="text-xs font-black uppercase text-sky-400 tracking-wider">3. Connections Tree</span>
                            <span class="px-1.5 py-0.2 rounded bg-white/10 text-[9px] font-mono">TREE VIEW</span>
                        </div>

                        <!-- Connection Tree Container -->
                        <div class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-2 min-h-[250px] font-mono">
                            <!-- Root Selected Device Node -->
                            <div class="flex items-center gap-1.5 p-1.5 rounded-lg bg-sky-950/70 border border-sky-500/40 text-white">
                                <span class="text-xs">👑</span>
                                <div class="min-w-0 flex-1">
                                    <div class="text-xs font-bold truncate">${selectedDev.name}</div>
                                    <div class="text-[8px] text-sky-300">Root Node (${selectedDev.agentId})</div>
                                </div>
                            </div>

                            <!-- Tree Branches with Bidirectional Filtering -->
                            <div class="pl-2 border-l-2 border-sky-500/30 space-y-2">
                                <!-- Family Branch -->
                                <div class="space-y-1">
                                    <div class="text-[9px] font-bold text-amber-300 font-sans uppercase flex items-center gap-1">
                                        <span>👨‍👩‍👧 Family Connections</span>
                                    </div>
                                    <div class="space-y-1 pl-1">
                                        ${this.renderTreeMembers(selectedDev, 'family')}
                                    </div>
                                </div>

                                <!-- Friends Branch -->
                                <div class="space-y-1">
                                    <div class="text-[9px] font-bold text-emerald-300 font-sans uppercase flex items-center gap-1">
                                        <span>🤝 Friends Connections</span>
                                    </div>
                                    <div class="space-y-1 pl-1">
                                        ${this.renderTreeMembers(selectedDev, 'friends')}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 4: SUB-MASTER SAFEZONES & TRANSACTIONAL ALERTS -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 space-y-2 shadow-md">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1">
                            <span class="text-xs font-black uppercase text-sky-400 tracking-wider">4. Safezones & Alerts</span>
                            <span class="px-1.5 py-0.2 rounded bg-white/10 text-[9px] font-mono">${selectedDev.safeZonesCount} Zones</span>
                        </div>

                        <div class="space-y-2">
                            <!-- Assigned Safe Zones -->
                            <div class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-1">
                                <div class="text-[9px] font-bold text-sky-400 uppercase font-sans">Assigned Perimeters</div>
                                <div class="space-y-1 max-h-[140px] overflow-y-auto custom-scrollbar">
                                    ${this.renderSafeZoneItems(selectedDev, rawNode)}
                                </div>
                            </div>

                            <!-- Transactional Geofence Notification History -->
                            <div class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-1">
                                <div class="text-[9px] font-bold text-amber-400 uppercase font-sans">Geofence Event Audit</div>
                                <div class="space-y-1 text-[8px] font-mono max-h-[160px] overflow-y-auto custom-scrollbar">
                                    ${this.renderNotificationItems(selectedDev, rawNode)}
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 5: TRANSACTIONAL LOCATION LOG -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 space-y-2 shadow-md">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1">
                            <span class="text-xs font-black uppercase text-sky-400 tracking-wider">5. Spatial Fix Log</span>
                            <span class="px-1.5 py-0.2 rounded bg-white/10 text-[9px] font-mono">TRANSACTIONAL</span>
                        </div>

                        <div class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-1.5 min-h-[300px]">
                            <div class="text-[9px] font-bold text-sky-400 uppercase font-sans border-b border-white/5 pb-0.5">Recorded Fixes & Telemetry</div>
                            <div class="space-y-1.5 max-h-[420px] overflow-y-auto custom-scrollbar text-xs font-mono">
                                ${this.renderLocationLogItems(selectedDev, rawNode)}
                            </div>
                        </div>
                    </div>

                    ` : ''}

                </div>
            </div>
        `;

        this.bindEvents();
    }

    /**
     * Renders Tree Members with Bidirectional Click Filtering
     */
    renderTreeMembers(device, type) {
        const rawNode = device.raw || {};
        const relsMap = rawNode.relationships || {};
        const relEntries = Object.entries(relsMap);

        // Filter relationships by type
        let matchedNodes = relEntries.filter(([k, v]) => {
            const roleStr = (v.role || "").toLowerCase();
            return type === 'family' ? (roleStr.includes('spouse') || roleStr.includes('child') || roleStr.includes('parent') || roleStr.includes('family')) : roleStr.includes('friend');
        });

        // Fallback: search overall fleet for matching devices if raw relationships map is empty
        if (matchedNodes.length === 0) {
            const fleetPeers = this.devices.filter(d => d.agentId !== device.agentId);
            if (type === 'family') {
                matchedNodes = fleetPeers.filter(d => d.roleFamily !== 'None').map(d => [d.agentId, { name: d.name, role: d.roleFamily }]);
            } else {
                matchedNodes = fleetPeers.filter(d => d.roleFriend !== 'None').map(d => [d.agentId, { name: d.name, role: d.roleFriend }]);
            }
        }

        if (matchedNodes.length === 0) {
            return `<div class="text-[8px] text-slate-500 italic">No registered ${type} links</div>`;
        }

        return matchedNodes.slice(0, 4).map(([agentId, data]) => {
            const peerDev = this.devices.find(d => d.agentId === agentId || d.deviceId === agentId || d.name === data.name);
            const peerName = data.name || (peerDev ? peerDev.name : agentId);
            const peerAvatar = peerDev ? peerDev.getAvatarUrl() : `data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>`;
            const peerTargetId = peerDev ? peerDev.agentId : agentId;

            return `
                <div class="du-tree-node flex items-center justify-between p-1 rounded bg-white/5 hover:bg-sky-950/60 hover:border-sky-500/40 text-[9px] border border-white/5 cursor-pointer transition-all"
                     data-target-agent-id="${peerTargetId}">
                    <div class="flex items-center gap-1 min-w-0">
                        <img src="${peerAvatar}" class="w-4 h-4 rounded-full object-cover shrink-0">
                        <span class="truncate text-white font-bold">${peerName}</span>
                    </div>
                    <span class="text-[8px] font-mono text-sky-300 font-bold px-1 rounded bg-sky-500/10">${data.role || 'Member'}</span>
                </div>
            `;
        }).join('');
    }

    /**
     * Renders Safe Zone Items from Sub-Master Node Data
     */
    renderSafeZoneItems(device, rawNode) {
        const zonesMap = rawNode.safe_zones || rawNode.Safehouse || {};
        const zoneEntries = Object.entries(zonesMap);

        if (zoneEntries.length === 0) {
            return `
                <div class="p-1.5 rounded bg-white/5 border border-white/5 text-[9px] space-y-0.5">
                    <div class="flex items-center justify-between font-bold text-white">
                        <span>Vibgyor Safe Zone</span>
                        <span class="px-1 py-0.2 rounded bg-emerald-500/20 text-emerald-400 text-[8px]">ACTIVE</span>
                    </div>
                    <div class="text-[8px] text-slate-400 font-mono">Radius: 100m • Lat: 18.5592° N, Lng: 73.8843° E</div>
                </div>
            `;
        }

        return zoneEntries.map(([id, z]) => {
            const zName = z.zone_name || z.name || id;
            const zRadius = z.radius || 100;
            const zLat = (z.lat || z.latitude || 0).toFixed(4);
            const zLng = (z.lng || z.longitude || 0).toFixed(4);
            const stateTag = z.current_state || "ACTIVE";

            return `
                <div class="p-1.5 rounded bg-sky-950/30 border border-sky-500/20 text-[9px] space-y-0.5">
                    <div class="flex items-center justify-between font-bold text-white">
                        <span class="truncate">${zName}</span>
                        <span class="px-1 py-0.2 rounded ${stateTag === 'OUT' ? 'bg-amber-500/20 text-amber-300' : 'bg-emerald-500/20 text-emerald-400'} text-[8px] font-bold">${stateTag}</span>
                    </div>
                    <div class="text-[8px] text-slate-400 font-mono">Radius: ${zRadius}m • ${zLat}°, ${zLng}°</div>
                </div>
            `;
        }).join('');
    }

    /**
     * Renders Geofence Notification Items from Transactional Data
     */
    renderNotificationItems(device, rawNode) {
        const live = (rawNode.device && rawNode.device.live) || rawNode.live || {};
        const lastTransState = live.current_safe_zone_state || "OUT";
        const lastTransZone = live.current_safe_zone_name || "Vibgyor Zone";
        const lastTransTime = live.last_geofence_transition_readable || device.getFormattedLastSeen();

        return `
            <div class="p-1.5 rounded bg-white/5 border border-white/5 space-y-0.5">
                <div class="flex items-center justify-between font-bold ${lastTransState === 'IN' ? 'text-emerald-400' : 'text-amber-400'}">
                    <span>${lastTransState === 'IN' ? '🟢 ENTERED' : '🔴 EXITED'} ${lastTransZone}</span>
                    <span class="text-[8px] text-slate-400">${lastTransTime}</span>
                </div>
                <div class="text-slate-300 text-[8px]">Device ${device.name} transition logged: ${lastTransState} perimeter.</div>
            </div>
        `;
    }

    /**
     * Renders Location History Items from Transactional Data
     */
    renderLocationLogItems(device, rawNode) {
        if (!device.hasValidLocation) {
            return `<div class="text-[9px] text-slate-500 italic py-2 text-center">No GPS coordinates registered.</div>`;
        }

        return `
            <div class="p-1.5 rounded-lg bg-sky-950/40 border border-sky-500/30 space-y-1">
                <div class="flex items-center justify-between text-[8px] text-sky-300 font-bold">
                    <span>🕒 ${device.getFormattedDateTime()}</span>
                    <span class="px-1 py-0.2 rounded bg-emerald-500/20 text-emerald-400 text-[8px]">LIVE FIX</span>
                </div>
                <div class="text-white text-xs font-bold">${device.getFormattedCoords()}</div>
                <div class="text-[9px] text-slate-200 leading-snug">📍 ${device.address || 'Environ 2 Block-A, Tingre Nagar, Pune, Maharashtra 411015'}</div>
                <div class="flex items-center justify-between text-[8px] text-slate-400 pt-0.5 border-t border-white/5">
                    <span>Speed: ${device.getFormattedSpeed()}</span>
                    <span>Heading: ${Math.round(device.bearing || 0)}°</span>
                </div>
            </div>
        `;
    }

    /**
     * Event Binders for Higher-to-Lower and Lower-to-Higher Bidirectional Filtering
     */
    bindEvents() {
        // 1. Pane 1: Device Selection Click Handler
        document.querySelectorAll('.du-device-item').forEach(el => {
            el.addEventListener('click', () => {
                const agentId = el.getAttribute('data-agent-id');
                this.selectedAgentId = agentId;
                this.render(this.devices, agentId);
            });
        });

        // 2. Pane 3: Tree View Node Bidirectional Inter-relationship Click Handler (Lower to Higher Filter)
        document.querySelectorAll('.du-tree-node').forEach(el => {
            el.addEventListener('click', () => {
                const targetAgentId = el.getAttribute('data-target-agent-id');
                if (targetAgentId) {
                    this.selectedAgentId = targetAgentId;
                    this.render(this.devices, targetAgentId);
                }
            });
        });

        // 3. Search Input Filtering
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

        // 4. Org Expansion Toggle
        document.getElementById('btn-toggle-org')?.addEventListener('click', () => {
            this.isOrgExpanded = !this.isOrgExpanded;
            this.render(this.devices, this.selectedAgentId);
        });

        // 5. Locate on Map Button
        document.getElementById('btn-du-view-map')?.addEventListener('click', () => {
            if (this.onNavigateMap && this.selectedAgentId) {
                this.onNavigateMap(this.selectedAgentId);
            }
        });
    }
}
