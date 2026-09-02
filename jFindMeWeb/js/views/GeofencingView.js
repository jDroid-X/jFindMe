import { GeoUtils } from '../core/GeoUtils.js';

/**
 * jFindMe Matrix - Dedicated Geofencing 4-Pane Oversight Workspace (OOPS Architecture)
 * 1. Safezone Roster & Creator Tree View
 * 2. Devices Assigned to Selected Safe Zone
 * 3. IN/OUT Alert Event Log (Last 2+ Days Metadata)
 * 4. Global Device Safezone Matrix
 */
export class GeofencingView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.onNavigateMap = options.onNavigateMap || null;

        this.devices = [];
        this.safeZones = [];
        this.selectedZoneId = null;
        this.selectedDeviceId = null;
        this.searchQuery = "";
    }

    /**
     * Renders the 4-Pane Geofencing Workspace
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices 
     * @param {Array<Object>} safeZones 
     */
    render(devices = [], safeZones = []) {
        this.devices = devices || [];

        // Deduplicate safe zones by unique zoneId
        const seenZones = new Set();
        const uniqueZones = [];
        for (const z of (Array.isArray(safeZones) ? safeZones : [])) {
            const zId = z.zoneId || z.id;
            if (zId && !seenZones.has(zId)) {
                seenZones.add(zId);
                uniqueZones.push(z);
            }
        }
        this.safeZones = uniqueZones;

        if (this.safeZones.length > 0) {
            if (!this.selectedZoneId || !this.safeZones.some(z => z.zoneId === this.selectedZoneId)) {
                this.selectedZoneId = this.safeZones[0].zoneId;
            }
        }

        const container = document.getElementById(this.containerId);
        if (!container) return;

        // Filter safe zones by search query
        let filteredZones = this.safeZones;
        if (this.searchQuery && this.searchQuery.trim().length > 0) {
            const q = this.searchQuery.trim().toLowerCase();
            filteredZones = this.safeZones.filter(z => {
                return (z.zoneName && z.zoneName.toLowerCase().includes(q)) ||
                       (z.assignedName && z.assignedName.toLowerCase().includes(q)) ||
                       (z.address && z.address.toLowerCase().includes(q));
            });
        }

        // Selected Safe Zone Object
        const selectedZone = this.safeZones.find(z => z.zoneId === this.selectedZoneId) || this.safeZones[0];

        // Assigned Devices for Selected Zone
        const assignedDevices = this.getAssignedDevicesForZone(selectedZone);

        if (assignedDevices.length > 0 && (!this.selectedDeviceId || !assignedDevices.some(d => d.agentId === this.selectedDeviceId))) {
            this.selectedDeviceId = assignedDevices[0].agentId;
        }

        const selectedDev = assignedDevices.find(d => d.agentId === this.selectedDeviceId) || assignedDevices[0];

        container.innerHTML = `
            <div class="w-full h-full select-none overflow-y-auto custom-scrollbar p-4 font-sans space-y-3">
                
                <!-- COMPACT HEADER BAR -->
                <div class="card-theme px-4 py-2.5 flex items-center justify-between shadow-sm">
                    <div class="flex items-center gap-3 min-w-0">
                        <div class="w-8 h-8 rounded-lg bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-sky-400 text-base font-bold shrink-0">
                            🛡️
                        </div>
                        <div class="min-w-0">
                            <h2 class="text-xs font-bold tracking-wide text-primary-theme uppercase flex items-center gap-2">
                                <span>Geofencing Perimeter Oversight Matrix</span>
                                <span class="badge-status badge-accent text-[9px]">REAL-TIME PERIMETERS</span>
                            </h2>
                            <p class="text-[11px] text-secondary-theme font-mono truncate">
                                Selected Perimeter: <span class="text-sky-400 font-bold">${selectedZone ? selectedZone.zoneName : 'None'}</span> (${selectedZone ? selectedZone.radius + 'm Radius' : 'N/A'})
                            </p>
                        </div>
                    </div>

                    ${selectedZone ? `
                    <div class="flex items-center gap-2 shrink-0">
                        <button id="btn-gf-view-map" class="btn-action-primary text-xs">
                            <span>🗺️ Locate Perimeter on Map</span>
                        </button>
                    </div>
                    ` : ''}
                </div>

                <!-- 4-PANE MULTI-COLUMN SMART FIT WORKSPACE -->
                <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3 items-start">
                    
                    <!-- PANE 1: SAFEZONE NAME & CREATOR TREE VIEW -->
                    <div class="card-theme p-3 flex flex-col space-y-2.5 shadow-sm">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-2">
                            <span class="text-xs font-extrabold uppercase text-sky-400 tracking-wider">1. Safezone Tree</span>
                            <span class="badge-status badge-accent">${filteredZones.length} Zones</span>
                        </div>

                        <!-- Search Input -->
                        <div class="relative flex items-center">
                            <input type="text" id="gf-search-input" 
                                   placeholder="🔍 Search safe zone name..." 
                                   value="${this.searchQuery}"
                                   class="input-theme w-full pl-7 pr-6 py-1.5 text-xs font-mono">
                            <svg class="w-3.5 h-3.5 absolute left-2 text-secondary-theme pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                            </svg>
                            ${this.searchQuery ? `
                                <button id="btn-gf-clear-search" class="absolute right-1.5 text-muted-theme hover:text-primary-theme text-xs font-bold p-0.5">✕</button>
                            ` : ''}
                        </div>

                        <!-- Safezone Roster Tree List -->
                        <div class="space-y-1.5 overflow-y-auto custom-scrollbar max-h-[500px] font-mono pr-0.5">
                            ${this.renderSafeZoneTree(filteredZones)}
                        </div>
                    </div>

                    ${selectedZone ? `
                    <!-- PANE 2: DEVICES ASSIGNED TO SELECTED SAFEZONE -->
                    <div class="card-theme p-3 space-y-2.5 shadow-sm flex flex-col">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-2">
                            <span class="text-xs font-extrabold uppercase text-sky-400 tracking-wider">2. Assigned Devices</span>
                            <div class="flex items-center gap-1">
                                <button id="btn-gf-edit-targets" class="btn-action-secondary text-[10px] py-0.5 px-2">
                                    ⚙️ Targets
                                </button>
                                <button id="btn-gf-delete-zone" class="btn-action-secondary text-[10px] py-0.5 px-2 text-rose-500">
                                    🗑️ Delete
                                </button>
                                <span class="badge-status badge-accent">${assignedDevices.length}</span>
                            </div>
                        </div>

                        <div class="surface-theme p-2.5 rounded-xl border space-y-2 text-xs font-mono">
                            <div class="text-[10px] text-sky-400 font-sans uppercase font-bold border-b border-subtle-theme pb-1 flex justify-between">
                                <span>Target Roster: ${selectedZone.zoneName}</span>
                                <span class="text-secondary-theme font-normal">${selectedZone.radius}m</span>
                            </div>
                            <div class="space-y-1.5 overflow-y-auto custom-scrollbar max-h-[460px] pr-0.5">
                                ${this.renderAssignedDevicesList(assignedDevices)}
                            </div>
                        </div>
                    </div>

                    <!-- PANE 3: IN/OUT TRANSITION LOG (LAST 2+ DAYS METADATA) -->
                    <div class="card-theme p-3 space-y-2.5 shadow-sm flex flex-col">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-2">
                            <span class="text-xs font-extrabold uppercase text-sky-400 tracking-wider">3. IN/OUT Alert Log</span>
                            <span class="badge-status badge-accent">2+ DAYS</span>
                        </div>

                        <div class="surface-theme p-2.5 rounded-xl border space-y-2 min-h-[350px]">
                            <div class="text-[10px] text-sky-400 font-sans uppercase font-bold border-b border-subtle-theme pb-1 flex justify-between">
                                <span>${selectedDev ? selectedDev.name : 'Device'}</span>
                                <span class="text-secondary-theme font-mono truncate max-w-[100px]">${selectedZone.zoneName}</span>
                            </div>
                            <div class="space-y-1.5 max-h-[460px] overflow-y-auto custom-scrollbar text-xs font-mono pr-0.5">
                                ${this.renderInOutTransitionLogs(selectedZone, selectedDev)}
                            </div>
                        </div>
                    </div>

                    <!-- PANE 4: GLOBAL DEVICE SAFEZONE MATRIX -->
                    <div class="card-theme p-3 space-y-2.5 shadow-sm flex flex-col">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-2">
                            <span class="text-xs font-extrabold uppercase text-sky-400 tracking-wider">4. Global Matrix</span>
                            <span class="badge-status badge-accent">${this.safeZones.length} ZONES</span>
                        </div>

                        <div class="surface-theme p-2.5 rounded-xl border space-y-2 min-h-[350px]">
                            <div class="text-[10px] text-sky-400 font-sans uppercase font-bold border-b border-subtle-theme pb-1">
                                Safezone Cross-Matrix
                            </div>
                            <div class="space-y-2 max-h-[460px] overflow-y-auto custom-scrollbar text-xs font-mono pr-0.5">
                                ${this.renderGlobalDeviceSafeZoneMatrix()}
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
     * Helper: Formats timestamp to readable Date & Time string
     */
    formatDateTime(ts) {
        if (!ts) return "Recent";
        const dateObj = new Date(typeof ts === 'number' ? ts : (Date.parse(ts) || Date.now()));
        if (isNaN(dateObj.getTime())) return String(ts);
        return dateObj.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            hour12: true
        });
    }

    /**
     * Renders Pane 1: Detailed Safezone Cards (Sorted by Latest Timestamp First)
     */
    renderSafeZoneTree(zones) {
        if (zones.length === 0) {
            return `<div class="text-[10px] text-muted-theme italic py-4 text-center font-mono">No registered safe zones.</div>`;
        }

        const sorted = [...zones].sort((a, b) => {
            const timeA = typeof a.createdAt === 'number' ? a.createdAt : (Date.parse(a.createdAt) || 0);
            const timeB = typeof b.createdAt === 'number' ? b.createdAt : (Date.parse(b.createdAt) || 0);
            return timeB - timeA;
        });

        return sorted.map(z => {
            const creatorIdLower = String(z.creatorId || "").toLowerCase();
            const creatorDev = this.devices.find(d => {
                const devId = String(d.agentId || "").toLowerCase();
                const devHwId = String(d.deviceId || "").toLowerCase();
                const devName = String(d.name || "").toLowerCase();
                const devModel = String(d.model || "").toLowerCase();
                const devUserName = String((d.raw && d.raw.Identity && d.raw.Identity.owner_name) || "").toLowerCase();
                return creatorIdLower && (devId === creatorIdLower || devHwId === creatorIdLower || devName === creatorIdLower || devModel === creatorIdLower || devUserName === creatorIdLower);
            });
            const creatorName = creatorDev ? creatorDev.name : (z.creatorId || 'Fleet Admin');
            const createdTimeStr = this.formatDateTime(z.createdAt);
            const isSelected = z.zoneId === this.selectedZoneId;

            return `
                <div class="gf-zone-item cursor-pointer p-2.5 rounded-xl border ${isSelected ? 'border-sky-500 bg-sky-500/10 shadow-sm' : 'border-subtle-theme bg-card-subtle hover:bg-card-hover'} transition-all space-y-1.5 font-mono select-none"
                     data-zone-id="${z.zoneId}">
                    
                    <!-- Header Row: Icon + Zone Name + Radius Badge -->
                    <div class="flex items-center justify-between text-xs font-bold text-primary-theme">
                        <div class="flex items-center gap-1.5 min-w-0">
                            <span class="text-sky-400 text-sm">🛡️</span>
                            <span class="truncate text-sky-400 text-xs font-bold tracking-tight">${z.zoneName}</span>
                        </div>
                        <span class="badge-status badge-accent text-[8px] shrink-0">
                            ${z.radius}m
                        </span>
                    </div>

                    <!-- Line 2: Created By <Name> <Date Time> -->
                    <div class="text-[10px] text-secondary-theme leading-tight flex items-center gap-1">
                        <span class="text-muted-theme">By:</span>
                        <span class="text-amber-500 font-bold truncate">${creatorName}</span>
                        <span class="text-muted-theme">•</span>
                        <span class="text-[9px] text-muted-theme truncate">${createdTimeStr}</span>
                    </div>

                    <!-- Line 3: Address + Status Badge -->
                    <div class="text-[9px] text-secondary-theme border-t border-subtle-theme pt-1 flex items-start justify-between gap-2">
                        <div class="line-clamp-2 flex-1 leading-snug">
                            ${z.address || 'Local Coordinates'}
                        </div>
                        <span class="badge-status ${z.isActive ? 'badge-success' : 'badge-danger'} text-[7px] shrink-0">
                            ${z.isActive ? 'ACTIVE' : 'OFF'}
                        </span>
                    </div>
                </div>
            `;
        }).join('');
    }

    /**
     * Resolves Assigned Devices for Selected Safe Zone
     */
    getAssignedDevicesForZone(zone) {
        if (!zone) return [];
        
        const connList = (Array.isArray(zone.connectedDevices) ? zone.connectedDevices : []).map(c => String(c).toLowerCase());
        const targetStr = (zone.assignedName || "").toLowerCase();
        const creatorId = (zone.creatorId || "").toLowerCase();

        return this.devices.filter(d => {
            const devName = (d.name || "").toLowerCase();
            const devId = (d.agentId || "").toLowerCase();
            const devHwId = (d.deviceId || "").toLowerCase();
            const devModel = (d.model || "").toLowerCase();
            const devUserName = ((d.raw && d.raw.Identity && d.raw.Identity.owner_name) || "").toLowerCase();

            const matchConnected = connList.some(c => 
                c === devId || c === devHwId || c === devName || (devUserName && c === devUserName) || (devModel && c.includes(devModel))
            );
            if (matchConnected) return true;

            if (targetStr.length > 0) {
                if (targetStr.includes(devName) || targetStr.includes(devId) || targetStr.includes(devHwId) || 
                    (devUserName && targetStr.includes(devUserName)) || (devModel && targetStr.includes(devModel))) {
                    return true;
                }
            }

            if (creatorId && (creatorId === devId || creatorId === devHwId || creatorId === devName || (devUserName && creatorId === devUserName))) {
                return true;
            }

            return false;
        });
    }

    /**
     * Renders Pane 2: Assigned Devices List
     */
    renderAssignedDevicesList(devicesList) {
        if (devicesList.length === 0) {
            return `<div class="text-[10px] text-muted-theme italic py-2 text-center font-mono">No assigned devices for this safe zone.</div>`;
        }

        return devicesList.map(dev => `
            <div class="gf-device-item cursor-pointer p-2 rounded-xl border ${dev.agentId === this.selectedDeviceId ? 'border-sky-500 bg-sky-500/10 shadow-sm' : 'border-subtle-theme bg-card-subtle hover:bg-card-hover'} transition-all flex items-center justify-between"
                 data-agent-id="${dev.agentId}">
                <div class="flex items-center gap-2 min-w-0">
                    <img src="${dev.getAvatarUrl()}" class="w-6 h-6 rounded-full object-cover border" style="border-color: ${dev.getThemeColor()};">
                    <div class="min-w-0">
                        <div class="text-xs font-bold text-primary-theme truncate">${dev.name}</div>
                        <div class="text-[9px] text-secondary-theme font-mono truncate">${dev.agentId} • ${dev.model || 'Device'}</div>
                    </div>
                </div>
                <span class="badge-status badge-accent text-[8px]">ASSIGNED</span>
            </div>
        `).join('');
    }

    /**
     * Renders Pane 3: IN/OUT Transition Event Log
     */
    renderInOutTransitionLogs(zone, device) {
        if (!device) return `<div class="text-[10px] text-muted-theme italic py-2 text-center font-mono">No device selected.</div>`;

        const logs = [];
        const devName = (device.name || "").toLowerCase();
        const devId = (device.agentId || "").toLowerCase();
        const devHwId = (device.deviceId || "").toLowerCase();
        const devUserName = ((device.raw && device.raw.Identity && device.raw.Identity.owner_name) || "").toLowerCase();

        const matchesDevice = (targetStr) => {
            if (!targetStr) return false;
            const t = String(targetStr).toLowerCase();
            return t.includes(devName) || devName.includes(t) || t === devId || t === devHwId || (devUserName && t.includes(devUserName));
        };

        const rawNode = device.raw || {};
        const live = (rawNode.device && rawNode.device.live) || rawNode.live || {};
        const currentZoneState = live.current_safe_zone_state || "";
        const currentZoneId = (live.current_safe_zone_id || "").toLowerCase();
        const currentZoneName = (live.current_safe_zone_name || "").toLowerCase();
        const zoneNameLower = (zone.zoneName || "").toLowerCase();
        const zoneIdLower = (zone.zoneId || "").toLowerCase();

        if (currentZoneState && (currentZoneId === zoneIdLower || currentZoneName === zoneNameLower || currentZoneName.includes(zoneNameLower))) {
            const isEntered = currentZoneState === "IN" || currentZoneState.includes("ENTER");
            logs.push({
                datetime: live.last_geofence_transition_readable || device.getFormattedDateTime() || "Recent Live Fix",
                status: isEntered ? "ENTERED" : "EXITED",
                zone: zone.zoneName,
                duration: isEntered ? "Currently Inside Safe Zone" : "Outside Safe Zone",
                isLive: true
            });
        }

        if (zone.history && typeof zone.history === 'object') {
            Object.values(zone.history).forEach(histItem => {
                if (!histItem) return;
                
                if (typeof histItem === 'object') {
                    const targetName = histItem.target_name || histItem.avatar_name || histItem.userName || "";
                    const targetUid = histItem.target_uid || histItem.creator_uid || histItem.uid || "";
                    
                    if (matchesDevice(targetName) || matchesDevice(targetUid)) {
                        const statusTag = String(histItem.status || histItem.transition || histItem.event_type || "TRANSITION").toUpperCase();
                        const isEntered = statusTag.includes("ENTER") || statusTag.includes("IN");
                        logs.push({
                            datetime: histItem.datetime || histItem.readable_time || (histItem.timestamp ? new Date(histItem.timestamp).toLocaleString() : "Recorded Event"),
                            status: isEntered ? "ENTERED" : "EXITED",
                            zone: histItem.zone_name || zone.zoneName,
                            duration: isEntered ? "Inside Safe Zone" : "Outside Safe Zone",
                            isLive: false
                        });
                    }
                }
            });
        }

        if (logs.length === 0) {
            const isInside = live.current_safe_zone_state === "IN" && (currentZoneId === zoneIdLower || currentZoneName === zoneNameLower);
            logs.push({
                datetime: device.getFormattedDateTime() || "Active State",
                status: isInside ? "ENTERED" : "EXITED",
                zone: zone.zoneName,
                duration: isInside ? "Active Inside Perimeter" : "Active Outside Perimeter",
                isLive: true
            });
        }

        return logs.map(log => `
            <div class="p-2 rounded-lg bg-card-subtle border border-subtle-theme space-y-0.5 font-mono">
                <div class="flex items-center justify-between text-[10px]">
                    <span class="${log.status === 'ENTERED' ? 'text-emerald-500' : 'text-amber-500'} font-bold">
                        ${log.status === 'ENTERED' ? '🟢 ENTERED' : '🔴 EXITED'} ${log.zone}
                    </span>
                    <span class="text-muted-theme text-[9px]">
                        ${log.datetime}
                    </span>
                </div>
                <div class="text-[10px] text-secondary-theme">
                    Device: <span class="text-primary-theme font-bold">${device.name}</span> • ${log.duration}
                </div>
            </div>
        `).join('');
    }

    /**
     * Renders Pane 4: Global Device Safezone Matrix
     */
    renderGlobalDeviceSafeZoneMatrix() {
        return this.safeZones.map(z => `
            <div class="p-2.5 rounded-xl bg-card-subtle border border-subtle-theme space-y-1 font-mono">
                <div class="flex items-center justify-between text-xs font-bold text-sky-400">
                    <span>🛡️ ${z.zoneName}</span>
                    <span class="badge-status badge-accent text-[8px]">${z.radius}m</span>
                </div>
                <div class="text-[10px] text-secondary-theme">
                    Targets: <span class="text-amber-500 font-bold">${z.assignedName || 'All Connected'}</span>
                </div>
                <div class="text-[9px] text-muted-theme border-t border-subtle-theme pt-0.5 flex justify-between">
                    <span class="truncate">${z.address || 'Local Coordinates'}</span>
                    <span class="text-emerald-500 font-bold">ACTIVE</span>
                </div>
            </div>
        `).join('');
    }

    /**
     * Event Binders
     */
    bindEvents() {
        // 1. Safe Zone Click
        document.querySelectorAll('.gf-zone-item').forEach(el => {
            el.addEventListener('click', () => {
                const zoneId = el.getAttribute('data-zone-id');
                this.selectedZoneId = zoneId;
                this.render(this.devices, this.safeZones);
            });
        });

        // 2. Device Click
        document.querySelectorAll('.gf-device-item').forEach(el => {
            el.addEventListener('click', () => {
                const agentId = el.getAttribute('data-agent-id');
                this.selectedDeviceId = agentId;
                this.render(this.devices, this.safeZones);
            });
        });

        // 3. Search Bar
        const searchInput = document.getElementById('gf-search-input');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.searchQuery = e.target.value;
                this.render(this.devices, this.safeZones);
                const nextInput = document.getElementById('gf-search-input');
                if (nextInput) {
                    nextInput.focus();
                    nextInput.setSelectionRange(nextInput.value.length, nextInput.value.length);
                }
            });
        }

        document.getElementById('btn-gf-clear-search')?.addEventListener('click', () => {
            this.searchQuery = "";
            this.render(this.devices, this.safeZones);
        });

        // 4. Locate on Map
        document.getElementById('btn-gf-view-map')?.addEventListener('click', () => {
            if (this.onNavigateMap && this.selectedZoneId) {
                this.onNavigateMap(this.selectedZoneId);
            }
        });

        // 5. Edit SafeZone Targets Button
        document.getElementById('btn-gf-edit-targets')?.addEventListener('click', () => {
            this.openEditTargetsModal();
        });

        // 6. Delete SafeZone Button
        document.getElementById('btn-gf-delete-zone')?.addEventListener('click', () => {
            this.openDeleteZoneModal();
        });
    }

    openEditTargetsModal() {
        const selectedZone = this.safeZones.find(z => z.zoneId === this.selectedZoneId) || this.safeZones[0];
        if (!selectedZone) return;

        const assignedDevices = this.getAssignedDevicesForZone(selectedZone);
        const assignedIds = assignedDevices.map(d => d.agentId);

        const overlay = document.createElement('div');
        overlay.id = 'edit-targets-modal-overlay';
        overlay.className = 'fixed inset-0 bg-black/70 backdrop-blur-sm z-[999] flex items-center justify-center p-4';
        overlay.innerHTML = `
            <div class="card-theme p-5 w-full max-w-md shadow-2xl border space-y-4">
                <div class="flex items-center justify-between border-b border-subtle-theme pb-2.5">
                    <h3 class="text-sm font-bold text-primary-theme flex items-center gap-2">
                        <span>🛡️</span>
                        <span>Assign Targets: ${selectedZone.zoneName}</span>
                    </h3>
                    <button id="btn-close-targets-modal" class="text-muted-theme hover:text-primary-theme text-base font-bold">✕</button>
                </div>

                <div class="space-y-2 text-xs">
                    <div class="text-secondary-theme font-bold">Select Connected Devices to Monitor:</div>
                    <div class="space-y-1.5 max-h-56 overflow-y-auto custom-scrollbar p-1">
                        ${this.devices.map(d => `
                            <label class="flex items-center justify-between p-2 rounded-lg bg-card-subtle hover:bg-card-hover border border-subtle-theme cursor-pointer">
                                <div class="flex items-center gap-2">
                                    <input type="checkbox" value="${d.agentId}" class="target-device-chk" ${assignedIds.includes(d.agentId) ? 'checked' : ''}>
                                    <span class="text-primary-theme font-bold">${d.name}</span>
                                    <span class="text-secondary-theme text-[10px] font-mono">(${d.agentId})</span>
                                </div>
                                <span class="text-muted-theme text-[10px]">${d.model || 'Device'}</span>
                            </label>
                        `).join('')}
                    </div>
                </div>

                <div class="flex items-center justify-end gap-2 pt-2 border-t border-subtle-theme">
                    <button id="btn-cancel-targets-modal" class="btn-action-secondary text-xs">Cancel</button>
                    <button id="btn-save-targets-modal" class="btn-action-primary text-xs">Save Targets</button>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        const closeModal = () => overlay.remove();
        document.getElementById('btn-close-targets-modal')?.addEventListener('click', closeModal);
        document.getElementById('btn-cancel-targets-modal')?.addEventListener('click', closeModal);

        document.getElementById('btn-save-targets-modal')?.addEventListener('click', async () => {
            const checked = Array.from(document.querySelectorAll('.target-device-chk:checked')).map(el => el.value);
            const targetNames = this.devices.filter(d => checked.includes(d.agentId)).map(d => d.name).join(', ');

            try {
                const res = await fetch('/api/safezone/update-targets', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        zoneId: selectedZone.zoneId,
                        creatorUid: selectedZone.creatorId || 'Fleet_Admin',
                        assignedTargets: targetNames || 'All Devices',
                        connectedDevices: checked
                    })
                });
                const resData = await res.json();
                if (resData.status === 'UPDATED') {
                    if (window.log) window.log(`SAFEZONE_TARGETS: Updated perimeter [${selectedZone.zoneName}] targets.`, "success");
                    closeModal();
                    if (window.showGeofencing) window.showGeofencing();
                }
            } catch (err) {
                console.error("[GEOFENCING] Update targets failure:", err);
                closeModal();
            }
        });
    }

    openDeleteZoneModal() {
        const selectedZone = this.safeZones.find(z => z.zoneId === this.selectedZoneId) || this.safeZones[0];
        if (!selectedZone) return;

        if (!confirm(`Are you sure you want to delete safe zone [${selectedZone.zoneName}]?`)) return;

        fetch('/api/safezone/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                zoneId: selectedZone.zoneId,
                creatorUid: selectedZone.creatorId || 'Fleet_Admin'
            })
        })
        .then(r => r.json())
        .then(res => {
            if (res.status === 'DELETED') {
                if (window.log) window.log(`SAFEZONE_PURGE: Removed perimeter [${selectedZone.zoneName}]`, "warning");
                if (window.showGeofencing) window.showGeofencing();
            }
        })
        .catch(err => {
            console.error("[GEOFENCING] Delete zone error:", err);
        });
    }
}
