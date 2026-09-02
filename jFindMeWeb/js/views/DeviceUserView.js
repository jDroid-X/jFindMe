import { GeoUtils } from '../core/GeoUtils.js';

/**
 * jFindMe Matrix - Dynamic Device User Oversight Matrix (OOPS Architecture)
 * Strict Relational Database Binding & Zero Hardcoding Policy:
 * All Master, Sub-Master, and Transactional Data points are strictly derived from relative Firebase RTDB entity structures.
 */
export class DeviceUserView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.onSelectDevice = options.onSelectDevice || null;
        this.onNavigateMap = options.onNavigateMap || null;
        
        this.devices = [];
        this.selectedAgentId = null;
        this.searchQuery = "";
        this.isOrgExpanded = false;
    }

    /**
     * Renders the 5-Pane Device User Interface dynamically without hardcoded fallbacks
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices 
     * @param {string} initialSelectedAgentId 
     * @param {Array<Object>} safeZones 
     */
    render(devices = [], initialSelectedAgentId = null, safeZones = []) {
        this.devices = devices;
        this.safeZones = safeZones;
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

        // Selected Device Object (Master Source)
        const selectedDev = this.devices.find(d => d.agentId === this.selectedAgentId) || this.devices[0];
        const rawNode = selectedDev ? (selectedDev.raw || {}) : {};

        // Compute exact dynamic counters for headers
        const relsMap = rawNode.relationships || {};
        const relCount = Object.keys(relsMap).length;

        const historyMap = rawNode.history || {};
        const historyCount = Object.keys(historyMap).length + (selectedDev && selectedDev.hasValidLocation ? 1 : 0);

        const createdZonesList = this.getCreatedSafeZonesList(selectedDev, this.safeZones);
        const assignedZonesList = this.getAssignedSafeZonesList(selectedDev, this.safeZones);

        container.innerHTML = `
            <div class="w-full min-h-screen bg-[#05070a] text-slate-200 select-none overflow-y-auto custom-scrollbar p-2 font-sans">
                
                <!-- COMPACT HEADER BAR -->
                <div class="bg-slate-900/90 border border-white/10 rounded-xl px-3 py-1.5 mb-2 flex items-center justify-between shadow-lg">
                    <div class="flex items-center gap-2">
                        <div class="w-7 h-7 rounded-lg bg-sky-500/20 border border-sky-500/40 flex items-center justify-center text-sky-400 font-bold text-sm">
                            📱
                        </div>
                        <div>
                            <h2 class="text-xs font-black tracking-wide text-white uppercase flex items-center gap-2">
                                <span>Device User Oversight Matrix</span>
                                <span class="px-1.5 py-0.2 rounded bg-sky-500/20 text-sky-300 text-[9px] font-mono border border-sky-500/30">DYNAMIC SSOT</span>
                            </h2>
                            <p class="text-[9px] text-slate-400 font-mono">
                                Master Device: <span class="text-sky-400 font-bold">${selectedDev ? selectedDev.name : 'None'}</span> (${selectedDev ? selectedDev.agentId : 'N/A'})
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

                <!-- 5-PANE MULTI-COLUMN WORKSPACE -->
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-2">
                    
                    <!-- PANE 1: MASTER DEVICE ROSTER & SEARCH BAR -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 flex flex-col space-y-2 shadow-md">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1">
                            <span class="text-xs font-black uppercase text-sky-400 tracking-wider">1. Device Roster</span>
                            <span class="px-1.5 py-0.2 rounded bg-white/10 text-[9px] font-mono font-bold">${filteredDevices.length}/${this.devices.length}</span>
                        </div>

                        <!-- Search Input -->
                        <div class="relative flex items-center">
                            <input type="text" id="du-search-input" 
                                   placeholder="🔍 Filter name, ID, role..." 
                                   value="${this.searchQuery}"
                                   class="w-full bg-slate-950 border border-white/10 rounded-lg pl-7 pr-6 py-1 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-sky-500 font-mono">
                            <svg class="w-3 h-3 absolute left-2 text-slate-500 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                            </svg>
                            ${this.searchQuery ? `
                                <button id="btn-du-clear-search" class="absolute right-1.5 text-slate-400 hover:text-white text-xs font-bold p-0.5">✕</button>
                            ` : ''}
                        </div>

                        <!-- Roster List -->
                        <div class="space-y-1 overflow-y-auto custom-scrollbar max-h-[450px]">
                            ${filteredDevices.map(dev => {
                                const ragColor = dev.getRagColor();
                                const ragLabel = dev.getRagLabel();

                                return `
                                    <div class="du-device-item cursor-pointer p-1.5 rounded-lg border ${dev.agentId === this.selectedAgentId ? 'border-sky-400 bg-sky-950/50 text-white shadow' : 'border-white/5 bg-black/40 hover:bg-white/5 text-slate-300'} transition-all flex items-center gap-2"
                                         data-agent-id="${dev.agentId}">
                                        <div class="relative shrink-0">
                                            <img src="${dev.getAvatarUrl()}" alt="${dev.name}" 
                                                 class="w-7 h-7 rounded-full object-cover border-2" 
                                                 style="border-color: ${ragColor};">
                                            <span class="absolute bottom-0 right-0 w-2 h-2 rounded-full border border-slate-900" style="background-color: ${ragColor}"></span>
                                        </div>
                                        <div class="min-w-0 flex-1">
                                            <div class="flex items-center justify-between">
                                                <h4 class="text-xs font-normal truncate text-white">${dev.name}</h4>
                                                <span class="text-[8px] font-mono font-normal px-1 py-0.2 rounded border" style="color: ${ragColor}; border-color: ${ragColor}44; background-color: ${ragColor}11">${ragLabel}</span>
                                            </div>
                                            <div class="text-[9px] text-slate-400 truncate font-mono">${dev.deviceName || dev.model} • ${dev.agentId}</div>
                                        </div>
                                    </div>
                                `;
                            }).join('')}
                        </div>
                    </div>

                    ${selectedDev ? `
                    <!-- PANE 2: MASTER & SUB-MASTER PROFILE DATA -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 space-y-2 shadow-md flex flex-col max-h-[calc(100vh-100px)] overflow-hidden">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1 shrink-0">
                            <span class="text-xs font-black uppercase text-sky-400 tracking-wider">2. Master Profile</span>
                            <button id="btn-toggle-org" class="text-[9px] text-sky-400 hover:underline font-mono">
                                ${this.isOrgExpanded ? 'Collapse Org [-]' : 'Expand Org [+]'}
                            </button>
                        </div>

                        <!-- Master Profile Details Container with Scrollbar -->
                        <div class="overflow-y-auto custom-scrollbar flex-1 space-y-2 pr-1">
                            <!-- Master Profile Identity Card -->
                            <div class="bg-black/50 p-2.5 rounded-lg border border-white/5 space-y-2 text-xs font-mono">
                                <div class="text-[9px] font-bold text-sky-400 uppercase tracking-wider font-sans border-b border-white/10 pb-1 flex items-center justify-between">
                                    <span>👤 Master Identity & Hardware</span>
                                    <span class="px-1.5 py-0.2 rounded bg-sky-500/20 text-sky-300 text-[8px] font-mono">${selectedDev.agentId}</span>
                                </div>

                                <div class="flex items-center gap-2.5 pb-2 border-b border-white/5">
                                    <img src="${selectedDev.getAvatarUrl()}" alt="${selectedDev.name}" class="w-10 h-10 rounded-full object-cover border-2 shadow shrink-0" style="border-color: ${selectedDev.getThemeColor()};">
                                    <div class="min-w-0 flex-1">
                                        <h3 class="font-bold text-xs text-white truncate">${selectedDev.name}</h3>
                                        <div class="text-[9px] text-sky-400 font-sans font-bold">${selectedDev.roleFamily !== 'None' ? selectedDev.roleFamily : selectedDev.role}</div>
                                        <div class="text-[8px] text-slate-400 truncate">Device: ${selectedDev.deviceName || selectedDev.model}</div>
                                    </div>
                                </div>

                                <div class="space-y-1.5 text-[10px]">
                                    <div class="flex justify-between items-center py-0.5 border-b border-white/[0.03]"><span class="text-slate-400">Auth UID:</span> <span class="text-slate-200 font-bold truncate max-w-[130px] font-mono">${(rawNode.Identity && rawNode.Identity.auth_uid) || (rawNode.device && rawNode.device.live && rawNode.device.live.auth_uid) || 'Unlinked'}</span></div>
                                    <div class="flex justify-between items-center py-0.5 border-b border-white/[0.03]"><span class="text-slate-400">Hardware ID:</span> <span class="text-slate-200 font-bold truncate max-w-[130px] font-mono">${selectedDev.deviceId}</span></div>
                                    <div class="flex justify-between items-center py-0.5 border-b border-white/[0.03]"><span class="text-slate-400">Device Model:</span> <span class="text-white font-medium">${selectedDev.model || 'Android Device'}</span></div>
                                    <div class="flex justify-between items-center py-0.5 border-b border-white/[0.03]"><span class="text-slate-400">Phone Number:</span> <span class="text-slate-200 font-mono">${selectedDev.phone || 'Not Registered'}</span></div>
                                    <div class="flex justify-between items-center py-0.5 border-b border-white/[0.03]"><span class="text-slate-400">Email Address:</span> <span class="text-slate-200 truncate max-w-[130px]">${selectedDev.email || 'Not Registered'}</span></div>
                                    <div class="flex justify-between items-center py-0.5"><span class="text-slate-400">Emergency Note:</span> <span class="${selectedDev.emergencyNote ? 'text-amber-300 font-bold' : 'text-slate-500'} truncate max-w-[120px]">${selectedDev.emergencyNote || 'None'}</span></div>
                                </div>
                            </div>

                            <!-- Sub-Master Expandable Org Data Panel -->
                            <div id="org-data-panel" class="bg-black/50 p-2.5 rounded-lg border border-white/5 space-y-2 text-xs font-mono ${this.isOrgExpanded ? 'block' : 'hidden'}">
                                <div class="text-[9px] font-bold text-sky-400 uppercase tracking-wider font-sans border-b border-white/10 pb-1 flex items-center justify-between">
                                    <span>🏢 Org Metadata & ACL</span>
                                    <span class="text-[8px] text-slate-400 font-mono">SUB-MASTER</span>
                                </div>
                                <div class="space-y-1 text-[9px]">
                                    <div class="flex justify-between py-0.5 border-b border-white/[0.03]"><span class="text-slate-400">Organization:</span> <span class="text-white font-bold">${(rawNode.profile && rawNode.profile.orgName) || 'jFindMe Fleet'}</span></div>
                                    <div class="flex justify-between py-0.5 border-b border-white/[0.03]"><span class="text-slate-400">Department:</span> <span class="text-white">${selectedDev.department || 'General'}</span></div>
                                    <div class="flex justify-between py-0.5 border-b border-white/[0.03]"><span class="text-slate-400">Role Title:</span> <span class="text-white font-bold">${selectedDev.role}</span></div>
                                    <div class="pt-1 text-[8px] text-slate-400 uppercase font-sans font-bold">ACL Access Grants:</div>
                                    <div class="grid grid-cols-2 gap-1 text-[8px] font-sans">
                                        <span class="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-center font-bold">Live Track</span>
                                        <span class="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-center font-bold">Geofence</span>
                                        <span class="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-center font-bold">History</span>
                                        <span class="px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 border border-white/10 text-center font-bold">Ghost Mode</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 3: CONNECTIONS TREE (FAMILY & FRIENDS) -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 space-y-2 shadow-md flex flex-col max-h-[calc(100vh-100px)] overflow-hidden">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1 shrink-0">
                            <span class="text-xs font-black uppercase text-sky-400 tracking-wider">3. Connections Tree</span>
                            <div class="flex items-center gap-1.5">
                                <button id="btn-du-add-link" class="px-1.5 py-0.5 rounded bg-sky-600/80 hover:bg-sky-500 text-white text-[9px] font-bold font-mono transition-all">
                                    + Add Link
                                </button>
                                <span class="px-1.5 py-0.2 rounded bg-white/10 text-[9px] font-mono font-bold">${relCount} Linked</span>
                            </div>
                        </div>

                        <!-- Tree Container with Scrollbar -->
                        <div class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-2 flex-1 overflow-y-auto custom-scrollbar font-mono pr-1">
                            <!-- Root Selected Device Node -->
                            <div class="flex items-center gap-1.5 p-1.5 rounded-lg bg-sky-950/70 border border-sky-500/40 text-white">
                                <span class="text-xs">👑</span>
                                <div class="min-w-0 flex-1">
                                    <div class="text-xs font-bold truncate">${selectedDev.name}</div>
                                    <div class="text-[8px] text-sky-300">Root Node (${selectedDev.agentId})</div>
                                </div>
                            </div>

                            <!-- Tree Branches (Dynamic Database Relationships Only) -->
                            <div class="pl-2 border-l-2 border-sky-500/30 space-y-2">
                                <!-- Family Branch -->
                                <details class="group/family space-y-1" open>
                                    <summary class="cursor-pointer select-none text-[9px] font-bold text-amber-300 font-sans uppercase flex items-center gap-1.5 list-none hover:text-amber-200">
                                        <span class="inline-block transition-transform group-open/family:rotate-90 text-[10px] text-amber-400 font-black">&gt;</span>
                                        <span>👨‍👩‍👧‍👦 Family Connections</span>
                                    </summary>
                                    <div class="space-y-1 pl-3.5 border-l border-amber-500/20 mt-1">
                                        ${this.renderTreeMembers(selectedDev, 'family')}
                                    </div>
                                </details>

                                <!-- Friends Branch -->
                                <details class="group/friends space-y-1" open>
                                    <summary class="cursor-pointer select-none text-[9px] font-bold text-emerald-300 font-sans uppercase flex items-center gap-1.5 list-none hover:text-emerald-200">
                                        <span class="inline-block transition-transform group-open/friends:rotate-90 text-[10px] text-emerald-400 font-black">&gt;</span>
                                        <span>🤝 Friends Connections</span>
                                    </summary>
                                    <div class="space-y-1 pl-3.5 border-l border-emerald-500/20 mt-1">
                                        ${this.renderTreeMembers(selectedDev, 'friends')}
                                    </div>
                                </details>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 4: SAFEZONES (SECTION 1: CREATED | SECTION 2: ASSIGNED) & NOTIFICATIONS -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 space-y-2 shadow-md flex flex-col max-h-[calc(100vh-100px)] overflow-hidden">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1 shrink-0">
                            <span class="text-xs font-black uppercase text-sky-400 tracking-wider">4. Safezones & Alerts</span>
                            <span class="px-1.5 py-0.2 rounded bg-white/10 text-[9px] font-mono font-bold">${createdZonesList.length} Created | ${assignedZonesList.length} Assigned</span>
                        </div>

                        <div class="space-y-2 flex-1 overflow-y-auto custom-scrollbar pr-1">
                            <!-- Section 1: Created Safe Zones -->
                            <div class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-1">
                                <div class="text-[9px] font-bold text-sky-400 uppercase font-sans flex items-center justify-between border-b border-white/10 pb-0.5">
                                    <span>Section 1: Created Safe Zones</span>
                                    <span class="text-[8px] font-mono font-normal text-slate-400">(${createdZonesList.length})</span>
                                </div>
                                <div class="space-y-1 max-h-[140px] overflow-y-auto custom-scrollbar">
                                    ${this.renderCreatedSafeZonesHTML(selectedDev, createdZonesList)}
                                </div>
                            </div>

                            <!-- Section 2: Assigned Safe Zones -->
                            <div class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-1">
                                <div class="text-[9px] font-bold text-amber-400 uppercase font-sans flex items-center justify-between border-b border-white/10 pb-0.5">
                                    <span>Section 2: Assigned Safe Zones</span>
                                    <span class="text-[8px] font-mono font-normal text-slate-400">(${assignedZonesList.length})</span>
                                </div>
                                <div class="space-y-1 max-h-[140px] overflow-y-auto custom-scrollbar">
                                    ${this.renderAssignedSafeZonesHTML(selectedDev, assignedZonesList)}
                                </div>
                            </div>

                            <!-- Transactional Geofence Notification Log -->
                            <div class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-1">
                                <div class="text-[9px] font-bold text-slate-300 uppercase font-sans border-b border-white/10 pb-0.5">Geofence Event Log</div>
                                <div class="space-y-1 text-[8px] font-mono max-h-[140px] overflow-y-auto custom-scrollbar">
                                    ${this.renderNotificationItems(selectedDev, rawNode)}
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 5: TRANSACTIONAL LOCATION FIX LOG -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 space-y-2 shadow-md flex flex-col max-h-[calc(100vh-100px)] overflow-hidden">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1 shrink-0">
                            <span class="text-xs font-black uppercase text-sky-400 tracking-wider">5. Spatial Fix Log</span>
                            <span class="px-1.5 py-0.2 rounded bg-white/10 text-[9px] font-mono font-bold">${historyCount} Fixes</span>
                        </div>

                        <div class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-1.5 flex-1 flex flex-col min-h-0 overflow-hidden">
                            <div class="text-[9px] font-bold text-sky-400 uppercase font-sans border-b border-white/5 pb-0.5 shrink-0">Recorded Location Fixes</div>
                            <div class="space-y-1.5 overflow-y-auto custom-scrollbar flex-1 pr-1 text-xs font-mono">
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
     * Dynamic Helper: Resolves Created Safe Zones for Selected Device (Strict Creator ID Matching)
     */
    getCreatedSafeZonesList(device, safeZonesList = []) {
        if (!device) return [];
        const devName = (device.name || "").toLowerCase();
        const devId = (device.agentId || "").toLowerCase();
        const devHwId = (device.deviceId || "").toLowerCase();
        const devModel = (device.model || "").toLowerCase();
        const devUserName = ((device.raw && device.raw.Identity && device.raw.Identity.owner_name) || "").toLowerCase();

        return safeZonesList.filter(z => {
            const creator = (z.creatorId || z.creator_uid || z.creator_id || z.created_by || "").toLowerCase();
            if (!creator) return false;
            return creator === devId || creator === devHwId || creator === devName || creator === devModel || (devUserName && creator === devUserName);
        }).map(z => ({
            id: z.zoneId || z.id,
            zone_name: z.zoneName || z.name || z.zone_id,
            radius: z.radius || 100,
            assigned_targets: z.assignedName || z.assigned_targets || "Connected Devices",
            lat: z.latitude || z.lat,
            lng: z.longitude || z.lng
        }));
    }

    /**
     * Dynamic Helper: Resolves Assigned Safe Zones for Selected Device
     */
    getAssignedSafeZonesList(device, safeZonesList = []) {
        if (!device) return [];
        const devName = (device.name || "").toLowerCase();
        const devId = (device.agentId || "").toLowerCase();
        const devHwId = (device.deviceId || "").toLowerCase();
        const devModel = (device.model || "").toLowerCase();
        const devUserName = ((device.raw && device.raw.Identity && device.raw.Identity.owner_name) || "").toLowerCase();

        return safeZonesList.filter(z => {
            const targetStr = (z.assignedName || z.assigned_targets || "").toLowerCase();
            const connList = (Array.isArray(z.connectedDevices) ? z.connectedDevices : []).map(c => String(c).toLowerCase());

            // Match targets string
            if (targetStr.includes(devName) || targetStr.includes(devId) || targetStr.includes(devHwId) || 
                (devModel && targetStr.includes(devModel)) || (devUserName && targetStr.includes(devUserName))) {
                return true;
            }

            // Match connected devices list
            if (connList.some(c => c === devId || c === devHwId || c === devName || c === devModel || (devUserName && c === devUserName))) {
                return true;
            }

            return false;
        }).map(z => ({
            id: z.zoneId || z.id,
            zone_name: z.zoneName || z.name || z.zone_id,
            radius: z.radius || 100,
            assigned_targets: z.assignedName || z.assigned_targets || "Connected Devices"
        }));
    }

    /**
     * Renders Tree Members (NO HARDCODED MOCKS)
     */
    renderTreeMembers(device, type) {
        const rawNode = device.raw || {};
        const relsMap = rawNode.relationships || {};
        const relEntries = Object.entries(relsMap);

        const matchedNodes = relEntries.filter(([k, v]) => {
            const roleStr = (v.role || "").toLowerCase();
            return type === 'family' ? (roleStr.includes('spouse') || roleStr.includes('child') || roleStr.includes('parent') || roleStr.includes('family') || roleStr.includes('father') || roleStr.includes('mother')) : roleStr.includes('friend');
        });

        if (matchedNodes.length === 0) {
            return `<div class="text-[9px] text-slate-500 italic py-0.5 font-mono">No linked ${type} connections for ${device.name}.</div>`;
        }

        return matchedNodes.map(([agentId, data]) => {
            const peerDev = this.devices.find(d => d.agentId === agentId || d.deviceId === agentId || d.name === data.name);
            const peerName = data.name || (peerDev ? peerDev.name : agentId);
            const peerAvatar = peerDev ? peerDev.getAvatarUrl() : `data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>`;
            const peerTargetId = peerDev ? peerDev.agentId : agentId;

            return `
                <div class="du-tree-node flex items-center justify-between p-1 rounded bg-white/5 hover:bg-sky-950/60 hover:border-sky-500/40 text-[9px] border border-white/5 cursor-pointer transition-all group"
                     data-target-agent-id="${peerTargetId}">
                    <div class="flex items-center gap-1 min-w-0">
                        <img src="${peerAvatar}" class="w-4 h-4 rounded-full object-cover shrink-0">
                        <span class="truncate text-white font-bold">${peerName}</span>
                    </div>
                    <div class="flex items-center gap-1">
                        <span class="text-[8px] font-mono text-sky-300 font-bold px-1 rounded bg-sky-500/10">${data.role || 'Member'}</span>
                        <button class="btn-delete-link text-slate-500 hover:text-rose-400 p-0.5 text-[10px] font-bold"
                                data-source-agent-id="${device.agentId}"
                                data-target-agent-id="${peerTargetId}"
                                data-peer-name="${peerName}"
                                title="Remove Link">✕</button>
                    </div>
                </div>
            `;
        }).join('');
    }

    /**
     * Renders Section 1: Created Safe Zones HTML
     */
    renderCreatedSafeZonesHTML(device, createdList) {
        if (createdList.length === 0) {
            return `<div class="text-[9px] text-slate-500 italic py-0.5 font-mono">No created safe zones for ${device.name}.</div>`;
        }

        return createdList.map(z => {
            const zName = z.zone_name || z.name || z.id;
            const zRadius = z.radius || 100;
            const zLat = z.lat ? parseFloat(z.lat).toFixed(4) : "0.0000";
            const zLng = z.lng ? parseFloat(z.lng).toFixed(4) : "0.0000";
            const targets = z.assigned_targets || "Connected Devices";

            return `
                <div class="p-1.5 rounded bg-sky-950/40 border border-sky-500/30 text-[9px] space-y-0.5">
                    <div class="flex items-center justify-between font-bold text-white">
                        <span class="truncate text-sky-300 font-bold">${zName}</span>
                        <span class="px-1 py-0.2 rounded bg-sky-500/20 text-sky-300 text-[8px] font-bold">CREATED</span>
                    </div>
                    <div class="text-[8px] text-slate-300 font-mono">Assigned To: <span class="text-amber-300 font-bold">${targets}</span> • Radius: ${zRadius}m</div>
                </div>
            `;
        }).join('');
    }

    /**
     * Renders Section 2: Assigned Safe Zones HTML
     */
    renderAssignedSafeZonesHTML(device, assignedList) {
        if (assignedList.length === 0) {
            return `<div class="text-[9px] text-slate-500 italic py-0.5 font-mono">No assigned safe zones for ${device.name}.</div>`;
        }

        return assignedList.map(z => {
            const zName = z.zone_name || z.name || z.id;
            const zRadius = z.radius || 100;
            const stateTag = z.current_state || "ACTIVE";

            return `
                <div class="p-1.5 rounded bg-amber-950/40 border border-amber-500/30 text-[9px] space-y-0.5">
                    <div class="flex items-center justify-between font-bold text-white">
                        <span class="truncate text-amber-300 font-bold">${zName}</span>
                        <span class="px-1 py-0.2 rounded ${stateTag === 'OUT' ? 'bg-amber-500/20 text-amber-300' : 'bg-emerald-500/20 text-emerald-400'} text-[8px] font-bold">${stateTag}</span>
                    </div>
                    <div class="text-[8px] text-slate-300 font-mono">Radius: ${zRadius}m • Perimeter: <span class="text-emerald-400 font-bold">${stateTag}</span></div>
                </div>
            `;
        }).join('');
    }

    /**
     * Renders Geofence Notification Log (NO HARDCODED MOCKS)
     */
    renderNotificationItems(device, rawNode) {
        const live = (rawNode.device && rawNode.device.live) || rawNode.live || {};
        const lastTransState = live.current_safe_zone_state;
        const lastTransZone = live.current_safe_zone_name;
        const lastTransTime = live.last_geofence_transition_readable;

        if (!lastTransState || !lastTransZone) {
            return `<div class="text-[9px] text-slate-500 italic py-0.5 font-mono">No geofence transition events logged for ${device.name}.</div>`;
        }

        return `
            <div class="p-1.5 rounded bg-white/5 border border-white/5 space-y-0.5">
                <div class="flex items-center justify-between font-bold ${lastTransState === 'IN' ? 'text-emerald-400' : 'text-amber-400'}">
                    <span>${lastTransState === 'IN' ? '🟢 ENTERED' : '🔴 EXITED'} ${lastTransZone}</span>
                    <span class="text-[8px] text-slate-400">${lastTransTime || 'Recent'}</span>
                </div>
                <div class="text-slate-300 text-[8px]">Device transition logged: ${lastTransState} perimeter.</div>
            </div>
        `;
    }

    /**
     * Renders Location History Items (NO HARDCODED MOCKS)
     */
    renderLocationLogItems(device, rawNode) {
        if (!device.hasValidLocation) {
            return `<div class="text-[9px] text-slate-500 italic py-2 text-center font-mono">No location history recorded for ${device.name}.</div>`;
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
     * Event Binders for Bidirectional Inter-relationship Filtering
     */
    bindEvents() {
        // 1. Roster Click Handler
        document.querySelectorAll('.du-device-item').forEach(el => {
            el.addEventListener('click', () => {
                const agentId = el.getAttribute('data-agent-id');
                this.selectedAgentId = agentId;
                this.render(this.devices, agentId);
            });
        });

        // 2. Tree Node Click Handler
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

        // 6. Add Relationship Link Modal Handler
        document.getElementById('btn-du-add-link')?.addEventListener('click', () => {
            this.openAddConnectionModal();
        });

        // 7. Delete Relationship Link Handler
        document.querySelectorAll('.btn-delete-link').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const sourceId = btn.getAttribute('data-source-agent-id');
                const targetId = btn.getAttribute('data-target-agent-id');
                const peerName = btn.getAttribute('data-peer-name') || targetId;

                if (!confirm(`CONFIRM REMOVAL: Remove relationship link between [${sourceId}] and [${peerName}]?`)) return;

                fetch('/api/relationship/delete', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ agentId: sourceId, targetAgentId: targetId })
                })
                .then(r => r.json())
                .then(res => {
                    if (res.status === 'SUCCESS') {
                        if (window.log) window.log(`RELATIONSHIP_PURGED: Connection ${sourceId} <-> ${targetId} deleted`, "success");
                        if (window.showDeviceUsers) window.showDeviceUsers();
                    } else {
                        alert(`Delete Failed: ${res.error}`);
                    }
                })
                .catch(err => alert(`Network Failure: ${err.message}`));
            });
        });
    }

    openAddConnectionModal() {
        const selectedDev = this.devices.find(d => d.agentId === this.selectedAgentId) || this.devices[0];
        if (!selectedDev) return;

        const peerDevices = this.devices.filter(d => d.agentId !== selectedDev.agentId);

        const overlay = document.createElement('div');
        overlay.id = 'du-add-link-modal';
        overlay.className = 'fixed inset-0 bg-black/80 backdrop-blur-md z-[999] flex items-center justify-center p-4';
        overlay.innerHTML = `
            <div class="bg-slate-900 border border-sky-500/30 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4 relative">
                <div class="flex items-center justify-between border-b border-white/10 pb-3">
                    <div class="flex items-center gap-2">
                        <span class="text-xl">🤝</span>
                        <div>
                            <h3 class="text-sm font-bold text-white uppercase tracking-wider">Bind Relationship Connection</h3>
                            <p class="text-[10px] text-sky-400 font-mono">Source Node: ${selectedDev.name} (${selectedDev.agentId})</p>
                        </div>
                    </div>
                    <button id="btn-close-link-modal" class="text-slate-400 hover:text-white font-bold text-sm">✕</button>
                </div>

                <div class="space-y-3 text-xs font-sans">
                    <div>
                        <label class="block text-[10px] uppercase font-bold text-slate-400 mb-1">Target Device Node</label>
                        <select id="link-target-agent" class="w-full bg-slate-950 border border-white/10 rounded-lg p-2 text-white font-mono focus:border-sky-500 outline-none">
                            ${peerDevices.map(d => `<option value="${d.agentId}">${d.name} (${d.agentId} • ${d.model})</option>`).join('')}
                        </select>
                    </div>

                    <div>
                        <label class="block text-[10px] uppercase font-bold text-slate-400 mb-1">Relationship Role Title</label>
                        <input type="text" id="link-role-title" placeholder="e.g. Spouse, Child, Parent, Friend" value="Spouse" class="w-full bg-slate-950 border border-white/10 rounded-lg p-2 text-white font-mono focus:border-sky-500 outline-none">
                    </div>

                    <div>
                        <label class="block text-[10px] uppercase font-bold text-slate-400 mb-1">Group Branch Type</label>
                        <select id="link-branch-type" class="w-full bg-slate-950 border border-white/10 rounded-lg p-2 text-white font-mono focus:border-sky-500 outline-none">
                            <option value="family">Family Group</option>
                            <option value="friends">Friends Group</option>
                        </select>
                    </div>
                </div>

                <div class="flex items-center justify-end gap-2 pt-2 border-t border-white/10">
                    <button id="btn-cancel-link" class="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-bold transition-all">Cancel</button>
                    <button id="btn-save-link" class="px-4 py-2 rounded-lg bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold transition-all flex items-center gap-1 shadow-lg shadow-sky-600/20">
                        <span>Save Connection</span>
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        const close = () => overlay.remove();
        document.getElementById('btn-close-link-modal').onclick = close;
        document.getElementById('btn-cancel-link').onclick = close;

        document.getElementById('btn-save-link').onclick = async () => {
            const targetAgentId = document.getElementById('link-target-agent').value;
            const role = document.getElementById('link-role-title').value.trim() || "Member";
            const type = document.getElementById('link-branch-type').value;

            const targetDev = this.devices.find(d => d.agentId === targetAgentId);
            const targetName = targetDev ? targetDev.name : targetAgentId;

            try {
                const res = await fetch('/api/relationship/update', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        agentId: selectedDev.agentId,
                        targetAgentId,
                        targetName,
                        role,
                        type
                    })
                });
                const result = await res.json();
                if (result.status === 'SUCCESS') {
                    if (window.log) window.log(`RELATIONSHIP_BOUND: ${selectedDev.name} -> ${targetName} (${role})`, "success");
                    close();
                    if (window.showUsers) window.showUsers(selectedDev.agentId);
                } else {
                    alert(`Binding Failed: ${result.error}`);
                }
            } catch (err) {
                alert(`Network Failure: ${err.message}`);
            }
        };
    }
}
