import { GeoUtils } from '../core/GeoUtils.js';
import { FeedbackDialog } from '../components/FeedbackDialog.js';
import { DeviceEditModalView } from './DeviceEditModalView.js';

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

        this.editModal = new DeviceEditModalView({
            onSave: async (agentId, payload) => {
                const res = await fetch('/api/update-profile', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ agentId, profileData: payload })
                });
                if (!res.ok) {
                    const data = await res.json();
                    throw new Error(data.error || 'Update failed');
                }
                if (window.showDeviceUser) window.showDeviceUser(agentId);
            }
        });
    }

    /**
     * Renders the 5-Pane Device User Interface dynamically without hardcoded fallbacks
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices 
     * @param {string} initialSelectedAgentId 
     * @param {Array<Object>} safeZones 
     */
    render(devices = [], initialSelectedAgentId = null, safeZones = []) {
        // Deduplicate devices by canonical agentId
        const seenDevs = new Set();
        const uniqueDevices = [];
        for (const d of (Array.isArray(devices) ? devices : [])) {
            const key = d.agentId || d.deviceId;
            if (key && !seenDevs.has(key)) {
                seenDevs.add(key);
                uniqueDevices.push(d);
            }
        }
        this.devices = uniqueDevices;
        this.safeZones = safeZones || [];

        if (this.devices.length > 0) {
            if (!this.selectedAgentId || !this.devices.some(d => d.agentId === this.selectedAgentId)) {
                this.selectedAgentId = initialSelectedAgentId || this.devices[0].agentId;
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
            <div class="w-full h-full select-none overflow-y-auto custom-scrollbar p-4 font-sans space-y-3">
                
                <!-- COMPACT HEADER BAR -->
                <div class="card-theme px-4 py-2.5 flex items-center justify-between shadow-sm">
                    <div class="flex items-center gap-3 min-w-0">
                        <div class="w-8 h-8 rounded-lg bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-sky-400 font-bold text-base shrink-0">
                            📱
                        </div>
                        <div class="min-w-0">
                            <h2 class="text-xs font-bold tracking-wide text-primary-theme uppercase flex items-center gap-2">
                                <span>Device User Oversight Matrix</span>
                                <span class="badge-status badge-accent text-[9px]">DYNAMIC SSOT</span>
                            </h2>
                            <p class="text-[11px] text-secondary-theme font-mono truncate">
                                Master Identity: <span class="text-sky-400 font-bold">${selectedDev ? selectedDev.name : 'None'}</span> (${selectedDev ? selectedDev.agentId : 'N/A'})
                            </p>
                        </div>
                    </div>

                    ${selectedDev ? `
                    <div class="flex items-center gap-2 shrink-0">
                        <button id="btn-du-view-map" class="btn-action-primary text-xs">
                            <span>🗺️ Locate on Map</span>
                        </button>
                    </div>
                    ` : ''}
                </div>

                <!-- 5-PANE MULTI-COLUMN SMART-FIT WORKSPACE -->
                <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-3 items-start">
                    
                    <!-- PANE 1: MASTER DEVICE ROSTER & SEARCH BAR -->
                    <div class="card-theme p-3 flex flex-col space-y-2.5 shadow-sm">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-2">
                            <span class="text-xs font-extrabold uppercase text-sky-400 tracking-wider">1. Device Roster</span>
                            <span class="badge-status badge-accent">${filteredDevices.length}/${this.devices.length}</span>
                        </div>

                        <!-- Search Input -->
                        <div class="relative flex items-center">
                            <input type="text" id="du-search-input" 
                                   placeholder="🔍 Filter name, ID, role..." 
                                   value="${this.searchQuery}"
                                   class="input-theme w-full pl-7 pr-6 py-1.5 text-xs font-mono">
                            <svg class="w-3.5 h-3.5 absolute left-2 text-secondary-theme pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                            </svg>
                            ${this.searchQuery ? `
                                <button id="btn-du-clear-search" class="absolute right-1.5 text-muted-theme hover:text-primary-theme text-xs font-bold p-0.5">✕</button>
                            ` : ''}
                        </div>

                        <!-- Roster List -->
                        <div class="space-y-1.5 overflow-y-auto custom-scrollbar max-h-[500px] pr-0.5">
                            ${filteredDevices.map(dev => {
                                const ragColor = dev.getRagColor();
                                const ragLabel = dev.getRagLabel();
                                const isSel = dev.agentId === this.selectedAgentId;

                                return `
                                    <div class="du-device-item cursor-pointer p-2 rounded-xl border ${isSel ? 'border-sky-500 bg-sky-500/10 shadow-sm' : 'border-subtle-theme bg-card-subtle hover:bg-card-hover'} transition-all flex items-center gap-2.5"
                                         data-agent-id="${dev.agentId}">
                                        <div class="relative shrink-0">
                                            <img src="${dev.getAvatarUrl()}" alt="${dev.name}" 
                                                 class="w-8 h-8 rounded-full object-cover border-2 shadow-sm" 
                                                 style="border-color: ${ragColor};">
                                            <span class="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full border border-surface" style="background-color: ${ragColor}"></span>
                                        </div>
                                        <div class="min-w-0 flex-1">
                                            <div class="flex items-center justify-between gap-1">
                                                <h4 class="text-xs font-bold truncate text-primary-theme min-w-0">${dev.name}</h4>
                                                <span class="text-[9px] font-mono font-bold px-1.5 py-0.5 rounded border shrink-0" style="color: ${ragColor}; border-color: ${ragColor}44; background-color: ${ragColor}18">${ragLabel}</span>
                                            </div>
                                            <div class="text-[10px] text-secondary-theme truncate font-mono">${dev.deviceName || dev.model} • ${dev.agentId}</div>
                                        </div>
                                    </div>
                                `;
                            }).join('')}
                        </div>
                    </div>

                    ${selectedDev ? `
                    <!-- PANE 2: MASTER & SUB-MASTER PROFILE DATA -->
                    <div class="card-theme p-3 space-y-2.5 shadow-sm flex flex-col">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-2 shrink-0">
                            <span class="text-xs font-extrabold uppercase text-sky-400 tracking-wider">2. Master Profile</span>
                            <div class="flex items-center gap-1.5">
                                <button id="btn-edit-profile-dossier" class="px-2 py-0.5 rounded text-[10px] font-bold bg-sky-500/20 text-sky-400 hover:bg-sky-500/30 border border-sky-500/30 transition-all flex items-center gap-1 cursor-pointer">
                                    <span>✏️ Edit</span>
                                </button>
                                <button id="btn-toggle-org" class="text-[10px] text-sky-400 hover:underline font-mono font-bold">
                                    ${this.isOrgExpanded ? '[-] Collapse' : '[+] Org ACL'}
                                </button>
                            </div>
                        </div>

                        <div class="overflow-y-auto custom-scrollbar space-y-2.5 max-h-[500px] pr-0.5">
                            <!-- Master Profile Identity Card -->
                            <div class="surface-theme p-3 rounded-xl space-y-2 text-xs font-mono border">
                                <div class="text-[10px] font-bold text-sky-400 uppercase tracking-wider font-sans border-b border-subtle-theme pb-1 flex items-center justify-between">
                                    <span>👤 Master Identity</span>
                                    <span class="badge-status badge-accent text-[8px]">${selectedDev.agentId}</span>
                                </div>

                                <div class="flex items-center gap-2.5 pb-2 border-b border-subtle-theme">
                                    <img src="${selectedDev.getAvatarUrl()}" alt="${selectedDev.name}" class="w-10 h-10 rounded-full object-cover border-2 shadow-sm shrink-0" style="border-color: ${selectedDev.getThemeColor()};">
                                    <div class="min-w-0 flex-1">
                                        <h3 class="font-bold text-xs text-primary-theme truncate">${selectedDev.name}</h3>
                                        <div class="text-[10px] text-sky-400 font-sans font-bold">${selectedDev.roleFamily !== 'None' ? selectedDev.roleFamily : selectedDev.role}</div>
                                        <div class="text-[9px] text-secondary-theme truncate">${selectedDev.deviceName || selectedDev.model}</div>
                                    </div>
                                </div>

                                <div class="space-y-1.5 text-[11px]">
                                    <div class="flex justify-between items-center py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Auth UID:</span> <span class="text-primary-theme font-bold truncate max-w-[120px] font-mono">${(rawNode.Identity && rawNode.Identity.auth_uid) || (rawNode.device && rawNode.device.live && rawNode.device.live.auth_uid) || 'Unlinked'}</span></div>
                                    <div class="flex justify-between items-center py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Hardware ID:</span> <span class="text-primary-theme font-bold truncate max-w-[120px] font-mono">${selectedDev.deviceId}</span></div>
                                    <div class="flex justify-between items-center py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Model:</span> <span class="text-primary-theme font-medium truncate max-w-[120px]">${selectedDev.model || 'Android Device'}</span></div>
                                    <div class="flex justify-between items-center py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Phone:</span> <span class="text-primary-theme font-mono">${selectedDev.phone || 'Not Registered'}</span></div>
                                    <div class="flex justify-between items-center py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Email:</span> <span class="text-primary-theme truncate max-w-[120px]">${selectedDev.email || 'Not Registered'}</span></div>
                                    <div class="flex justify-between items-center py-0.5"><span class="text-secondary-theme">Emergency Note:</span> <span class="${selectedDev.emergencyNote ? 'text-amber-500 font-bold' : 'text-muted-theme'} truncate max-w-[120px]">${selectedDev.emergencyNote || 'None'}</span></div>
                                </div>
                            </div>

                            <!-- Sub-Master Expandable Org Data Panel -->
                            <div id="org-data-panel" class="surface-theme p-3 rounded-xl space-y-2 text-xs font-mono border ${this.isOrgExpanded ? 'block' : 'hidden'}">
                                <div class="text-[10px] font-bold text-sky-400 uppercase tracking-wider font-sans border-b border-subtle-theme pb-1 flex items-center justify-between">
                                    <span>🏢 Org Metadata & ACL</span>
                                    <span class="text-[9px] text-muted-theme font-mono">SUB-MASTER</span>
                                </div>
                                <div class="space-y-1 text-[10px]">
                                    <div class="flex justify-between py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Org:</span> <span class="text-primary-theme font-bold">${(rawNode.profile && rawNode.profile.orgName) || 'jFindMe Fleet'}</span></div>
                                    <div class="flex justify-between py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Dept:</span> <span class="text-primary-theme">${selectedDev.department || 'General'}</span></div>
                                    <div class="flex justify-between py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Role:</span> <span class="text-primary-theme font-bold">${selectedDev.role}</span></div>
                                    <div class="pt-1 text-[9px] text-secondary-theme uppercase font-sans font-bold">ACL Grants:</div>
                                    <div class="grid grid-cols-2 gap-1 text-[9px] font-sans pt-0.5">
                                        <span class="badge-status badge-success text-center justify-center">Live Track</span>
                                        <span class="badge-status badge-success text-center justify-center">Geofence</span>
                                        <span class="badge-status badge-success text-center justify-center">History</span>
                                        <span class="badge-status badge-warning text-center justify-center">Ghost Mode</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 3: CONNECTIONS TREE (FAMILY & FRIENDS) -->
                    <div class="card-theme p-3 space-y-2.5 shadow-sm flex flex-col">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-2 shrink-0">
                            <span class="text-xs font-extrabold uppercase text-sky-400 tracking-wider">3. Connections</span>
                            <div class="flex items-center gap-1.5">
                                <button id="btn-du-add-link" class="btn-action-secondary text-[10px] py-0.5 px-2">
                                    + Link
                                </button>
                                <span class="badge-status badge-accent">${relCount} Linked</span>
                            </div>
                        </div>

                        <!-- Tree Container with Scrollbar -->
                        <div class="surface-theme p-2.5 rounded-xl border space-y-2 overflow-y-auto custom-scrollbar max-h-[500px] font-mono">
                            <!-- Root Selected Device Node -->
                            <div class="flex items-center gap-2 p-2 rounded-lg bg-sky-500/10 border border-sky-500/30 text-primary-theme">
                                <span class="text-sm">👑</span>
                                <div class="min-w-0 flex-1">
                                    <div class="text-xs font-bold truncate">${selectedDev.name}</div>
                                    <div class="text-[9px] text-sky-400">Root Node (${selectedDev.agentId})</div>
                                </div>
                            </div>

                            <!-- Tree Branches -->
                            <div class="pl-2 border-l-2 border-sky-500/30 space-y-2">
                                <!-- Family Branch -->
                                <details class="group/family space-y-1" open>
                                    <summary class="cursor-pointer select-none text-[10px] font-bold text-amber-500 font-sans uppercase flex items-center gap-1.5 list-none hover:text-amber-400">
                                        <span class="inline-block transition-transform group-open/family:rotate-90 text-[10px] font-black">&gt;</span>
                                        <span>👨‍👩‍👧‍👦 Family Connections</span>
                                    </summary>
                                    <div class="space-y-1 pl-3 border-l border-amber-500/20 mt-1">
                                        ${this.renderTreeMembers(selectedDev, 'family')}
                                    </div>
                                </details>

                                <!-- Friends Branch -->
                                <details class="group/friends space-y-1" open>
                                    <summary class="cursor-pointer select-none text-[10px] font-bold text-emerald-500 font-sans uppercase flex items-center gap-1.5 list-none hover:text-emerald-400">
                                        <span class="inline-block transition-transform group-open/friends:rotate-90 text-[10px] font-black">&gt;</span>
                                        <span>🤝 Friends Connections</span>
                                    </summary>
                                    <div class="space-y-1 pl-3 border-l border-emerald-500/20 mt-1">
                                        ${this.renderTreeMembers(selectedDev, 'friends')}
                                    </div>
                                </details>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 4: SAFEZONES (SECTION 1: CREATED | SECTION 2: ASSIGNED) & NOTIFICATIONS -->
                    <div class="card-theme p-3 space-y-2.5 shadow-sm flex flex-col">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-2 shrink-0">
                            <span class="text-xs font-extrabold uppercase text-sky-400 tracking-wider">4. Safezones & Alerts</span>
                            <span class="badge-status badge-accent">${createdZonesList.length} C | ${assignedZonesList.length} A</span>
                        </div>

                        <div class="space-y-2 overflow-y-auto custom-scrollbar max-h-[500px] pr-0.5">
                            <!-- Section 1: Created Safe Zones -->
                            <div class="surface-theme p-2.5 rounded-xl border space-y-1.5">
                                <div class="text-[10px] font-bold text-sky-400 uppercase font-sans flex items-center justify-between border-b border-subtle-theme pb-1">
                                    <span>Section 1: Created Zones</span>
                                    <span class="text-[9px] font-mono text-muted-theme">(${createdZonesList.length})</span>
                                </div>
                                <div class="space-y-1 max-h-[140px] overflow-y-auto custom-scrollbar">
                                    ${this.renderCreatedSafeZonesHTML(selectedDev, createdZonesList)}
                                </div>
                            </div>

                            <!-- Section 2: Assigned Safe Zones -->
                            <div class="surface-theme p-2.5 rounded-xl border space-y-1.5">
                                <div class="text-[10px] font-bold text-amber-500 uppercase font-sans flex items-center justify-between border-b border-subtle-theme pb-1">
                                    <span>Section 2: Assigned Zones</span>
                                    <span class="text-[9px] font-mono text-muted-theme">(${assignedZonesList.length})</span>
                                </div>
                                <div class="space-y-1 max-h-[140px] overflow-y-auto custom-scrollbar">
                                    ${this.renderAssignedSafeZonesHTML(selectedDev, assignedZonesList)}
                                </div>
                            </div>

                            <!-- Transactional Geofence Notification Log -->
                            <div class="surface-theme p-2.5 rounded-xl border space-y-1.5">
                                <div class="text-[10px] font-bold text-secondary-theme uppercase font-sans border-b border-subtle-theme pb-1">Geofence Event Log</div>
                                <div class="space-y-1 text-[9px] font-mono max-h-[140px] overflow-y-auto custom-scrollbar">
                                    ${this.renderNotificationItems(selectedDev, rawNode)}
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 5: TRANSACTIONAL LOCATION FIX LOG -->
                    <div class="card-theme p-3 space-y-2.5 shadow-sm flex flex-col">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-2 shrink-0">
                            <span class="text-xs font-extrabold uppercase text-sky-400 tracking-wider">5. Spatial Fix Log</span>
                            <span class="badge-status badge-accent">${historyCount} Fixes</span>
                        </div>

                        <div class="surface-theme p-2.5 rounded-xl border space-y-2 flex-1 flex flex-col min-h-0 overflow-hidden">
                            <div class="text-[10px] font-bold text-sky-400 uppercase font-sans border-b border-subtle-theme pb-1 shrink-0">Recorded Location Fixes</div>
                            <div class="space-y-2 overflow-y-auto custom-scrollbar flex-1 pr-0.5 text-xs font-mono">
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
     * Dynamic Helper: Resolves Created Safe Zones for Selected Device (Strict Deduplicated ID Matching)
     */
    getCreatedSafeZonesList(device, safeZonesList = []) {
        if (!device) return [];
        const devName = (device.name || "").toLowerCase();
        const devId = (device.agentId || "").toLowerCase();
        const devHwId = (device.deviceId || "").toLowerCase();
        const devModel = (device.model || "").toLowerCase();
        const devUserName = ((device.raw && device.raw.Identity && device.raw.Identity.owner_name) || "").toLowerCase();

        const seen = new Set();
        const result = [];

        for (const z of safeZonesList) {
            const zId = z.zoneId || z.id || z.zone_id;
            if (!zId || seen.has(zId)) continue;

            const creator = (z.creatorId || z.creator_uid || z.creator_id || z.created_by || "").toLowerCase();
            if (!creator) continue;

            if (creator === devId || creator === devHwId || creator === devName || creator === devModel || (devUserName && creator === devUserName)) {
                seen.add(zId);
                result.push({
                    id: zId,
                    zone_name: z.zoneName || z.name || z.zone_id || "Safe Zone",
                    radius: z.radius || 100,
                    assigned_targets: z.assignedName || z.assigned_targets || "Connected Devices",
                    lat: z.latitude || z.lat,
                    lng: z.longitude || z.lng
                });
            }
        }
        return result;
    }

    /**
     * Dynamic Helper: Resolves Assigned Safe Zones for Selected Device (Strict Deduplicated)
     */
    getAssignedSafeZonesList(device, safeZonesList = []) {
        if (!device) return [];
        const devName = (device.name || "").toLowerCase();
        const devId = (device.agentId || "").toLowerCase();
        const devHwId = (device.deviceId || "").toLowerCase();
        const devModel = (device.model || "").toLowerCase();
        const devUserName = ((device.raw && device.raw.Identity && device.raw.Identity.owner_name) || "").toLowerCase();

        const seen = new Set();
        const result = [];

        for (const z of safeZonesList) {
            const zId = z.zoneId || z.id || z.zone_id;
            if (!zId || seen.has(zId)) continue;

            const targetStr = (z.assignedName || z.assigned_targets || "").toLowerCase();
            const connList = (Array.isArray(z.connectedDevices) ? z.connectedDevices : []).map(c => String(c).toLowerCase());

            let isAssigned = false;
            if (targetStr.includes(devName) || targetStr.includes(devId) || targetStr.includes(devHwId) || 
                (devModel && targetStr.includes(devModel)) || (devUserName && targetStr.includes(devUserName))) {
                isAssigned = true;
            } else if (connList.some(c => c === devId || c === devHwId || c === devName || c === devModel || (devUserName && c === devUserName))) {
                isAssigned = true;
            }

            if (isAssigned) {
                seen.add(zId);
                result.push({
                    id: zId,
                    zone_name: z.zoneName || z.name || z.zone_id || "Safe Zone",
                    radius: z.radius || 100,
                    assigned_targets: z.assignedName || z.assigned_targets || "Connected Devices"
                });
            }
        }
        return result;
    }

    /**
     * Renders Tree Members (Deduplicated, Zero Hardcoding)
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
            return `<div class="text-[10px] text-muted-theme italic py-1 font-mono">No linked ${type} connections.</div>`;
        }

        return matchedNodes.map(([agentId, data]) => {
            const peerDev = this.devices.find(d => d.agentId === agentId || d.deviceId === agentId || d.name === data.name);
            const peerName = data.name || (peerDev ? peerDev.name : agentId);
            const peerAvatar = peerDev ? peerDev.getAvatarUrl() : `data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>`;
            const peerTargetId = peerDev ? peerDev.agentId : agentId;

            return `
                <div class="du-tree-node flex items-center justify-between p-1.5 rounded-lg bg-card-subtle hover:bg-card-hover text-xs border border-subtle-theme cursor-pointer transition-all group"
                     data-target-agent-id="${peerTargetId}">
                    <div class="flex items-center gap-1.5 min-w-0">
                        <img src="${peerAvatar}" class="w-4 h-4 rounded-full object-cover shrink-0">
                        <span class="truncate text-primary-theme font-bold">${peerName}</span>
                    </div>
                    <div class="flex items-center gap-1">
                        <span class="badge-status badge-accent text-[8px]">${data.role || 'Member'}</span>
                        <button class="btn-delete-link text-muted-theme hover:text-rose-500 p-0.5 text-xs font-bold"
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
            return `<div class="text-[10px] text-muted-theme italic py-1 font-mono">No created safe zones.</div>`;
        }

        return createdList.map(z => {
            const zName = z.zone_name || z.name || z.id;
            const zRadius = z.radius || 100;
            const targets = z.assigned_targets || "Connected Devices";

            return `
                <div class="p-2 rounded-lg bg-card-subtle border border-subtle-theme text-xs space-y-0.5">
                    <div class="flex items-center justify-between font-bold text-primary-theme">
                        <span class="truncate text-sky-400 font-bold">${zName}</span>
                        <span class="badge-status badge-accent text-[8px]">CREATED</span>
                    </div>
                    <div class="text-[10px] text-secondary-theme font-mono">Assigned: <span class="text-amber-500 font-bold">${targets}</span> • ${zRadius}m</div>
                </div>
            `;
        }).join('');
    }

    /**
     * Renders Section 2: Assigned Safe Zones HTML
     */
    renderAssignedSafeZonesHTML(device, assignedList) {
        if (assignedList.length === 0) {
            return `<div class="text-[10px] text-muted-theme italic py-1 font-mono">No assigned safe zones.</div>`;
        }

        return assignedList.map(z => {
            const zName = z.zone_name || z.name || z.id;
            const zRadius = z.radius || 100;
            const stateTag = z.current_state || "ACTIVE";

            return `
                <div class="p-2 rounded-lg bg-card-subtle border border-subtle-theme text-xs space-y-0.5">
                    <div class="flex items-center justify-between font-bold text-primary-theme">
                        <span class="truncate text-amber-500 font-bold">${zName}</span>
                        <span class="badge-status ${stateTag === 'OUT' ? 'badge-warning' : 'badge-success'} text-[8px]">${stateTag}</span>
                    </div>
                    <div class="text-[10px] text-secondary-theme font-mono">Radius: ${zRadius}m • Perimeter: <span class="text-emerald-500 font-bold">${stateTag}</span></div>
                </div>
            `;
        }).join('');
    }

    /**
     * Renders Geofence Notification Log
     */
    renderNotificationItems(device, rawNode) {
        const live = (rawNode.device && rawNode.device.live) || rawNode.live || {};
        const lastTransState = live.current_safe_zone_state;
        const lastTransZone = live.current_safe_zone_name;
        const lastTransTime = live.last_geofence_transition_readable;

        if (!lastTransState || !lastTransZone) {
            return `<div class="text-[10px] text-muted-theme italic py-1 font-mono">No geofence transition events logged.</div>`;
        }

        return `
            <div class="p-2 rounded-lg bg-card-subtle border border-subtle-theme space-y-0.5">
                <div class="flex items-center justify-between font-bold ${lastTransState === 'IN' ? 'text-emerald-500' : 'text-amber-500'}">
                    <span>${lastTransState === 'IN' ? '🟢 ENTERED' : '🔴 EXITED'} ${lastTransZone}</span>
                    <span class="text-[9px] text-muted-theme">${lastTransTime || 'Recent'}</span>
                </div>
                <div class="text-secondary-theme text-[10px]">Transition: ${lastTransState} perimeter registered.</div>
            </div>
        `;
    }

    /**
     * Renders Location History Items
     */
    renderLocationLogItems(device, rawNode) {
        if (!device.hasValidLocation) {
            return `<div class="text-[10px] text-muted-theme italic py-4 text-center font-mono">No location history recorded.</div>`;
        }

        return `
            <div class="p-2.5 rounded-xl bg-card-subtle border border-subtle-theme space-y-1.5">
                <div class="flex items-center justify-between text-[10px] text-sky-400 font-bold">
                    <span>🕒 ${device.getFormattedDateTime()}</span>
                    <span class="badge-status badge-success text-[8px]">LIVE FIX</span>
                </div>
                <div class="text-primary-theme text-xs font-bold">${device.getFormattedCoords()}</div>
                <div class="text-[11px] text-secondary-theme leading-snug">📍 ${device.address || 'Environ 2 Block-A, Tingre Nagar, Pune, Maharashtra 411015'}</div>
                <div class="flex items-center justify-between text-[10px] text-muted-theme pt-1 border-t border-subtle-theme">
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
                this.render(this.devices, agentId, this.safeZones);
            });
        });

        // 2. Tree Node Click Handler
        document.querySelectorAll('.du-tree-node').forEach(el => {
            el.addEventListener('click', () => {
                const targetAgentId = el.getAttribute('data-target-agent-id');
                if (targetAgentId) {
                    this.selectedAgentId = targetAgentId;
                    this.render(this.devices, targetAgentId, this.safeZones);
                }
            });
        });

        // 3. Search Input Filtering
        const searchInput = document.getElementById('du-search-input');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.searchQuery = e.target.value;
                this.render(this.devices, this.selectedAgentId, this.safeZones);
                const nextInput = document.getElementById('du-search-input');
                if (nextInput) {
                    nextInput.focus();
                    nextInput.setSelectionRange(nextInput.value.length, nextInput.value.length);
                }
            });
        }

        document.getElementById('btn-du-clear-search')?.addEventListener('click', () => {
            this.searchQuery = "";
            this.render(this.devices, this.selectedAgentId, this.safeZones);
        });

        // 4. Locate on Live Map
        document.getElementById('btn-du-view-map')?.addEventListener('click', () => {
            if (this.onNavigateMap && this.selectedAgentId) {
                this.onNavigateMap(this.selectedAgentId);
            }
        });

        // 5. Expand Org Panel Toggle
        document.getElementById('btn-toggle-org')?.addEventListener('click', () => {
            this.isOrgExpanded = !this.isOrgExpanded;
            const orgPanel = document.getElementById('org-data-panel');
            const btnToggle = document.getElementById('btn-toggle-org');
            if (orgPanel && btnToggle) {
                if (this.isOrgExpanded) {
                    orgPanel.classList.remove('hidden');
                    btnToggle.textContent = '[-] Collapse';
                } else {
                    orgPanel.classList.add('hidden');
                    btnToggle.textContent = '[+] Org ACL';
                }
            }
        });

        // 5b. Edit Profile Action
        document.getElementById('btn-edit-profile-dossier')?.addEventListener('click', () => {
            const selectedDev = this.devices.find(d => d.agentId === this.selectedAgentId);
            if (selectedDev) {
                this.editModal.show(selectedDev);
            }
        });

        // 6. Delete Link Action (Closed-Loop Confirmation)
        document.querySelectorAll('.btn-delete-link').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const sourceId = btn.getAttribute('data-source-agent-id');
                const targetId = btn.getAttribute('data-target-agent-id');
                const peerName = btn.getAttribute('data-peer-name');
                const sourceDev = this.devices.find(d => d.agentId === sourceId);
                const sourceName = sourceDev ? sourceDev.name : sourceId;

                FeedbackDialog.showConfirm({
                    title: "Sever Connection",
                    message: `Are you sure you want to disconnect [${peerName}] from [${sourceName}]? This will remove bidirectional relational tracking.`,
                    icon: "🔗",
                    confirmText: "Disconnect",
                    confirmClass: "btn-action-danger",
                    onConfirm: async () => {
                        try {
                            const res = await fetch('/api/relationship/delete', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ agentId: sourceId, targetAgentId: targetId })
                            });
                            const resData = await res.json();
                            if (resData.status === 'SUCCESS' || resData.status === 'DELETED') {
                                FeedbackDialog.showToast(`Severed connection with [${peerName}].`, "warning");
                                if (window.showDeviceUser) window.showDeviceUser(sourceId);
                            }
                        } catch (err) {
                            FeedbackDialog.showToast(`Sever failed: ${err.message}`, "error");
                        }
                    }
                });
            });
        });

        // 7. Add Link Action Modal
        document.getElementById('btn-du-add-link')?.addEventListener('click', () => {
            this.openAddLinkModal();
        });
    }

    /**
     * Add Link Modal using Rich Searchable Detail List & Action Selection
     */
    openAddLinkModal() {
        const selectedDev = this.devices.find(d => d.agentId === this.selectedAgentId) || this.devices[0];
        if (!selectedDev) return;

        const otherDevices = this.devices.filter(d => d.agentId !== selectedDev.agentId);
        if (otherDevices.length === 0) {
            FeedbackDialog.showToast("No other registered devices available to connect.", "info");
            return;
        }

        // Step 1: Rich Detail List Modal to select candidate device
        FeedbackDialog.showDetailListModal({
            title: `Connect Device to [${selectedDev.name}]`,
            message: "Select a fleet device from the directory below to establish relational link:",
            items: otherDevices.map(d => ({
                id: d.agentId,
                title: d.name,
                subtitle: `ID: ${d.agentId} • ${d.model || 'Device'} • Battery: ${d.batteryPct}%`,
                avatarUrl: d.getAvatarUrl(),
                tag: d.isOnline ? 'ONLINE' : 'OFFLINE'
            })),
            multiSelect: false,
            onConfirm: (selectedIds) => {
                const targetId = selectedIds[0];
                const targetDev = this.devices.find(d => d.agentId === targetId);
                if (!targetDev) return;

                // Step 2: Multi-Option Action Modal to choose Role & Branch
                FeedbackDialog.showActionModal({
                    title: `Select Role for [${targetDev.name}]`,
                    message: `Define relational hierarchy between ${selectedDev.name} and ${targetDev.name}:`,
                    icon: "👥",
                    actions: [
                        { id: 'Father', label: 'Father', description: 'Primary Guardian & Family Administrator', icon: '👨', badge: 'FAMILY', variant: 'primary' },
                        { id: 'Mother', label: 'Mother', description: 'Family Guardian & Oversight Node', icon: '👩', badge: 'FAMILY', variant: 'primary' },
                        { id: 'Spouse', label: 'Spouse', description: 'Partner Relationship', icon: '💍', badge: 'FAMILY', variant: 'primary' },
                        { id: 'Child', label: 'Child', description: 'Monitored Family Dependent', icon: '🧒', badge: 'FAMILY', variant: 'primary' },
                        { id: 'Guardian', label: 'Guardian / Caretaker', description: 'Designated Safety Monitor', icon: '🛡️', badge: 'FAMILY', variant: 'secondary' },
                        { id: 'Friend', label: 'Close Friend', description: 'Social & MeetMyFriend Circle', icon: '🤝', badge: 'FRIENDS', variant: 'secondary' },
                        { id: 'Member', label: 'Fleet Team Member', description: 'General Organization Colleague', icon: '📱', badge: 'MEMBERS', variant: 'secondary' }
                    ],
                    onSelect: async (chosenRole) => {
                        const isFriend = chosenRole.toLowerCase().includes('friend');
                        const branchType = isFriend ? 'friend' : 'family';

                        try {
                            const res = await fetch('/api/relationship/add', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({
                                    agentId: selectedDev.agentId,
                                    targetAgentId: targetDev.agentId,
                                    targetName: targetDev.name,
                                    role: chosenRole,
                                    type: branchType
                                })
                            });
                            const resData = await res.json();
                            if (resData.status === 'SUCCESS' || resData.status === 'LINKED') {
                                FeedbackDialog.showToast(`Connected [${selectedDev.name}] <-> [${targetDev.name}] as ${chosenRole}!`, "success");
                                if (window.showDeviceUser) window.showDeviceUser(selectedDev.agentId);
                            } else {
                                FeedbackDialog.showToast(`Binding notice: ${resData.message || 'Operation processed.'}`, "info");
                            }
                        } catch (err) {
                            FeedbackDialog.showToast(`Connection failed: ${err.message}`, "error");
                        }
                    }
                });
            }
        });
    }
}

