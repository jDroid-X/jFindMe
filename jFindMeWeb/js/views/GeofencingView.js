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
        this.devices = devices;
        this.safeZones = safeZones;

        if (safeZones.length > 0) {
            if (!this.selectedZoneId || !safeZones.some(z => z.zoneId === this.selectedZoneId)) {
                this.selectedZoneId = safeZones[0].zoneId;
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
            <div class="w-full min-h-screen bg-[#05070a] text-slate-200 select-none overflow-y-auto custom-scrollbar p-2 font-sans">
                
                <!-- COMPACT HEADER BAR (Simple Non-Bold Text) -->
                <div class="bg-slate-900/90 border border-white/10 rounded-xl px-3 py-1.5 mb-2 flex items-center justify-between shadow-lg">
                    <div class="flex items-center gap-2">
                        <div class="w-7 h-7 rounded-lg bg-sky-500/20 border border-sky-500/40 flex items-center justify-center text-sky-400 text-sm font-medium">
                            🛡️
                        </div>
                        <div>
                            <h2 class="text-xs font-medium tracking-wide text-white uppercase flex items-center gap-2">
                                <span>Geofencing Perimeter Oversight Matrix</span>
                                <span class="px-1.5 py-0.2 rounded bg-sky-500/20 text-sky-300 text-[9px] font-mono border border-sky-500/30 font-normal">REAL-TIME PERIMETERS</span>
                            </h2>
                            <p class="text-[9px] text-slate-400 font-mono font-normal">
                                Selected Perimeter: <span class="text-sky-400 font-medium">${selectedZone ? selectedZone.zoneName : 'None'}</span> (${selectedZone ? selectedZone.radius + 'm Radius' : 'N/A'})
                            </p>
                        </div>
                    </div>

                    ${selectedZone ? `
                    <div class="flex items-center gap-1.5">
                        <button id="btn-gf-view-map" class="px-2.5 py-1 rounded-lg bg-sky-600 hover:bg-sky-500 text-white text-xs font-medium transition-all flex items-center gap-1 shadow-md shadow-sky-600/20">
                            <span>🗺️ Locate Perimeter on Map</span>
                        </button>
                    </div>
                    ` : ''}
                </div>

                <!-- 4-PANE MULTI-COLUMN WORKSPACE -->
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-2">
                    
                    <!-- PANE 1: SAFEZONE NAME & CREATOR TREE VIEW -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 flex flex-col space-y-2 shadow-md">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1">
                            <span class="text-xs font-medium uppercase text-sky-400 tracking-wider">1. Safezone Tree</span>
                            <span class="px-1.5 py-0.2 rounded bg-white/10 text-[9px] font-mono font-normal">${filteredZones.length} Zones</span>
                        </div>

                        <!-- Search Input -->
                        <div class="relative flex items-center">
                            <input type="text" id="gf-search-input" 
                                   placeholder="🔍 Search safe zone name..." 
                                   value="${this.searchQuery}"
                                   class="w-full bg-slate-950 border border-white/10 rounded-lg pl-7 pr-6 py-1 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-sky-500 font-mono font-normal">
                            <svg class="w-3 h-3 absolute left-2 text-slate-500 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                            </svg>
                            ${this.searchQuery ? `
                                <button id="btn-gf-clear-search" class="absolute right-1.5 text-slate-400 hover:text-white text-xs font-medium p-0.5">✕</button>
                            ` : ''}
                        </div>

                        <!-- Safezone Roster Tree List -->
                        <div class="space-y-1.5 overflow-y-auto custom-scrollbar max-h-[480px] font-mono">
                            ${this.renderSafeZoneTree(filteredZones)}
                        </div>
                    </div>

                    ${selectedZone ? `
                    <!-- PANE 2: DEVICES ASSIGNED TO SELECTED SAFEZONE -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 space-y-2 shadow-md">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1">
                            <span class="text-xs font-medium uppercase text-sky-400 tracking-wider">2. Assigned Devices</span>
                            <div class="flex items-center gap-1">
                                <button id="btn-gf-edit-targets" class="px-1.5 py-0.5 rounded bg-sky-600/80 hover:bg-sky-500 text-white text-[9px] font-bold font-mono transition-all">
                                    ⚙️ Targets
                                </button>
                                <button id="btn-gf-delete-zone" class="px-1.5 py-0.5 rounded bg-rose-600/80 hover:bg-rose-500 text-white text-[9px] font-bold font-mono transition-all">
                                    🗑️ Delete
                                </button>
                                <span class="px-1.5 py-0.2 rounded bg-white/10 text-[9px] font-mono font-normal">${assignedDevices.length} Devices</span>
                            </div>
                        </div>

                        <div class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-1.5 text-xs font-mono">
                            <div class="text-[9px] text-sky-400 font-sans uppercase font-medium border-b border-white/5 pb-0.5 flex justify-between">
                                <span>Target Roster: ${selectedZone.zoneName}</span>
                                <span class="text-slate-400 font-normal">Radius: ${selectedZone.radius}m</span>
                            </div>
                            <div class="space-y-1 overflow-y-auto custom-scrollbar max-h-[440px]">
                                ${this.renderAssignedDevicesList(assignedDevices)}
                            </div>
                        </div>
                    </div>

                    <!-- PANE 3: IN/OUT TRANSITION LOG (LAST 2+ DAYS METADATA) -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 space-y-2 shadow-md">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1">
                            <span class="text-xs font-medium uppercase text-sky-400 tracking-wider">3. IN/OUT Alert Log</span>
                            <span class="px-1.5 py-0.2 rounded bg-white/10 text-[9px] font-mono font-normal">2+ DAYS</span>
                        </div>

                        <div class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-1.5 min-h-[350px]">
                            <div class="text-[9px] text-sky-400 font-sans uppercase font-medium border-b border-white/5 pb-0.5 flex justify-between">
                                <span>Transitions: ${selectedDev ? selectedDev.name : 'Device'}</span>
                                <span class="text-slate-400 font-normal font-mono">${selectedZone.zoneName}</span>
                            </div>
                            <div class="space-y-1.5 max-h-[440px] overflow-y-auto custom-scrollbar text-xs font-mono">
                                ${this.renderInOutTransitionLogs(selectedZone, selectedDev)}
                            </div>
                        </div>
                    </div>

                    <!-- PANE 4: GLOBAL DEVICE SAFEZONE MATRIX -->
                    <div class="bg-slate-900/90 rounded-xl border border-white/10 p-2 space-y-2 shadow-md">
                        <div class="flex items-center justify-between border-b border-white/10 pb-1">
                            <span class="text-xs font-medium uppercase text-sky-400 tracking-wider">4. Global Matrix</span>
                            <span class="px-1.5 py-0.2 rounded bg-white/10 text-[9px] font-mono font-normal">ALL ZONES</span>
                        </div>

                        <div class="bg-black/50 p-2 rounded-lg border border-white/5 space-y-2 min-h-[350px]">
                            <div class="text-[9px] text-sky-400 font-sans uppercase font-medium border-b border-white/5 pb-0.5">
                                Device Safezone Cross-Matrix
                            </div>
                            <div class="space-y-2 max-h-[440px] overflow-y-auto custom-scrollbar text-xs font-mono">
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
            second: '2-digit',
            hour12: true
        });
    }

    /**
     * Renders Pane 1: Detailed Safezone Cards (Sorted by Latest Timestamp First)
     * Format matches image layout:
     * [ 🛡️ Zone Name ]                     [ XXm Radius ]
     * Created by: <Creator Name> • <Date Time>
     * Address: <Location Address>              [ ACTIVE ]
     */
    renderSafeZoneTree(zones) {
        if (zones.length === 0) {
            return `<div class="text-[9px] text-slate-500 italic py-4 text-center font-mono">No registered safe zones.</div>`;
        }

        // Sort zones by latest creation timestamp first
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
                <div class="gf-zone-item cursor-pointer p-2.5 rounded-xl border ${isSelected ? 'border-sky-400 bg-sky-950/40 text-white shadow-lg shadow-sky-500/10' : 'border-white/10 bg-slate-950/80 hover:bg-slate-900 text-slate-300'} transition-all space-y-1.5 font-mono select-none"
                     data-zone-id="${z.zoneId}">
                    
                    <!-- Header Row: Icon + Zone Name + Radius Badge -->
                    <div class="flex items-center justify-between text-xs font-bold text-white">
                        <div class="flex items-center gap-1.5 min-w-0">
                            <span class="text-sky-400 text-sm">🛡️</span>
                            <span class="truncate text-sky-300 text-sm font-bold tracking-tight">${z.zoneName}</span>
                        </div>
                        <span class="px-2 py-0.5 rounded-lg bg-sky-500/20 text-sky-300 border border-sky-500/30 text-[9px] font-bold shrink-0">
                            ${z.radius}m Radius
                        </span>
                    </div>

                    <!-- Line 2: Created By <Name> <Date Time> -->
                    <div class="text-[9px] text-slate-300 leading-tight flex items-center gap-1">
                        <span class="text-slate-400">Created by:</span>
                        <span class="text-amber-300 font-bold">${creatorName}</span>
                        <span class="text-slate-500">•</span>
                        <span class="text-slate-400 text-[8px]">${createdTimeStr}</span>
                    </div>

                    <!-- Line 3: Address + Status Badge -->
                    <div class="text-[8px] text-slate-400 border-t border-white/5 pt-1 flex items-start justify-between gap-2">
                        <div class="line-clamp-2 text-slate-300 flex-1 leading-snug">
                            <span class="text-slate-400">Address:</span> ${z.address || 'Local Memory Coordinates'}
                        </div>
                        <span class="px-1.5 py-0.2 rounded font-bold uppercase tracking-wider text-[8px] shrink-0 ${z.isActive ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-slate-800 text-slate-500 border border-white/10'}">
                            ${z.isActive ? 'ACTIVE' : 'OFF'}
                        </span>
                    </div>
                </div>
            `;
        }).join('');
    }

    /**
     * Resolves Assigned Devices for Selected Safe Zone (Strict Subset Filtering: Pane 1 -> Pane 2)
     * Matches connected_devices array, assigned targets string, or creator device with alias support (e.g. REDMI N7 PRO / BE5A234A / Riddhi).
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

            // Check if device matches any alias in connectedDevices list
            const matchConnected = connList.some(c => 
                c === devId || c === devHwId || c === devName || (devUserName && c === devUserName) || (devModel && c.includes(devModel))
            );
            if (matchConnected) return true;

            // Check match in assigned targets string (e.g. "REDMI 5, REDMI N7 PRO" or "Riddhi, PUSHPA")
            if (targetStr.length > 0) {
                if (targetStr.includes(devName) || targetStr.includes(devId) || targetStr.includes(devHwId) || 
                    (devUserName && targetStr.includes(devUserName)) || (devModel && targetStr.includes(devModel))) {
                    return true;
                }
            }

            // Creator check: Creator device is implicitly linked to their created perimeter
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
            return `<div class="text-[9px] text-slate-500 italic py-2 text-center font-mono">No assigned devices for this safe zone.</div>`;
        }

        return devicesList.map(dev => `
            <div class="gf-device-item cursor-pointer p-1.5 rounded-lg border ${dev.agentId === this.selectedDeviceId ? 'border-sky-400 bg-sky-950/50 text-white shadow' : 'border-white/5 bg-black/40 hover:bg-white/5 text-slate-300'} transition-all flex items-center justify-between"
                 data-agent-id="${dev.agentId}">
                <div class="flex items-center gap-1.5 min-w-0">
                    <img src="${dev.getAvatarUrl()}" class="w-6 h-6 rounded-full object-cover border" style="border-color: ${dev.getThemeColor()};">
                    <div class="min-w-0">
                        <div class="text-xs font-medium text-white truncate">${dev.name}</div>
                        <div class="text-[8px] text-slate-400 font-mono">${dev.agentId} • ${dev.model || 'Device'}</div>
                    </div>
                </div>
                <span class="text-[8px] font-mono px-1 rounded bg-sky-500/20 text-sky-300">ASSIGNED</span>
            </div>
        `).join('');
    }

    /**
     * Renders Pane 3: IN/OUT Transition Event Log (Strict Subset Filtering: Pane 2 -> Pane 3)
     * Queries live transition state + history ledger for the selected device in the selected zone.
     */
    renderInOutTransitionLogs(zone, device) {
        if (!device) return `<div class="text-[9px] text-slate-500 italic py-2 text-center font-mono">No device selected.</div>`;

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

        // 1. Live Current SafeZone State from selected device node
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
                duration: isEntered ? "Currently Inside Safe Zone Perimeter" : "Outside Safe Zone Perimeter",
                isLive: true
            });
        }

        // 2. Dynamic Extraction from zone.history (Firebase RTDB history & notification nodes)
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
                            duration: isEntered ? "Inside Safe Zone Perimeter" : "Outside Safe Zone Perimeter",
                            isLive: false
                        });
                    }
                } else if (typeof histItem === 'string') {
                    const itemLower = histItem.toLowerCase();
                    if (matchesDevice(itemLower)) {
                        const isEntered = itemLower.includes('entered') || itemLower.includes('enter') || itemLower.includes(' in ');
                        const isExited = itemLower.includes('exited') || itemLower.includes('exit') || itemLower.includes(' out ');
                        const timeMatch = histItem.match(/on (.*?) at (.*)/i) || histItem.match(/(?:at|on)\s+(.*)/i);
                        const datetimeStr = timeMatch ? `${timeMatch[1]} ${timeMatch[2] || ''}`.trim() : histItem;

                        logs.push({
                            datetime: datetimeStr || "Recorded Event",
                            status: isEntered ? "ENTERED" : (isExited ? "EXITED" : "TRANSITION"),
                            zone: zone.zoneName,
                            duration: isEntered ? "Inside Safe Zone Perimeter" : "Outside Safe Zone Perimeter",
                            isLive: false
                        });
                    }
                }
            });
        }

        // Fallback: If device is assigned to this perimeter, synthesize current state fix log
        if (logs.length === 0) {
            const isInside = live.current_safe_zone_state === "IN" && (currentZoneId === zoneIdLower || currentZoneName === zoneNameLower);
            logs.push({
                datetime: device.getFormattedDateTime() || "Current Active State",
                status: isInside ? "ENTERED" : "EXITED",
                zone: zone.zoneName,
                duration: isInside ? "Active Inside Perimeter" : "Active Outside Perimeter",
                isLive: true
            });
        }

        return logs.map(log => `
            <div class="p-1.5 rounded-lg ${log.isLive ? 'bg-sky-950/40 border-sky-500/40' : 'bg-white/5 border-white/5'} border space-y-0.5 font-mono">
                <div class="flex items-center justify-between text-[9px] font-normal">
                    <span class="${log.status === 'ENTERED' ? 'text-emerald-400' : 'text-amber-400'} font-bold">
                        ${log.status === 'ENTERED' ? '🟢 ENTERED' : '🔴 EXITED'} ${log.zone}
                    </span>
                    <span class="text-slate-400 text-[8px] flex items-center gap-1">
                        ${log.isLive ? '<span class="px-1 bg-emerald-500/20 text-emerald-400 rounded text-[7px]">LIVE</span>' : ''}
                        <span>${log.datetime}</span>
                    </span>
                </div>
                <div class="text-[8px] text-slate-300">
                    Device: <span class="text-white font-bold">${device.name}</span> • ${log.duration}
                </div>
            </div>
        `).join('');
    }

    /**
     * Renders Pane 4: Global Device Safezone Matrix
     */
    renderGlobalDeviceSafeZoneMatrix() {
        return this.safeZones.map(z => `
            <div class="p-2 rounded-lg bg-sky-950/30 border border-sky-500/20 space-y-1 font-mono">
                <div class="flex items-center justify-between text-xs font-medium text-sky-300">
                    <span>🛡️ ${z.zoneName}</span>
                    <span class="text-[8px] px-1 rounded bg-sky-500/20 text-sky-300">${z.radius}m Radius</span>
                </div>
                <div class="text-[9px] text-slate-300">
                    Assigned Targets: <span class="text-amber-300">${z.assignedName || 'All Devices'}</span>
                </div>
                <div class="text-[8px] text-slate-400 border-t border-white/5 pt-0.5 flex justify-between">
                    <span>Address: ${z.address || 'Local Memory'}</span>
                    <span class="text-emerald-400">ACTIVE</span>
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
        overlay.id = 'gf-edit-targets-modal';
        overlay.className = 'fixed inset-0 bg-black/80 backdrop-blur-md z-[999] flex items-center justify-center p-4';
        overlay.innerHTML = `
            <div class="bg-slate-900 border border-sky-500/30 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4 relative">
                <div class="flex items-center justify-between border-b border-white/10 pb-3">
                    <div class="flex items-center gap-2">
                        <span class="text-xl">⚙️</span>
                        <div>
                            <h3 class="text-sm font-bold text-white uppercase tracking-wider">Assign Target Devices</h3>
                            <p class="text-[10px] text-sky-400 font-mono">SafeZone: ${selectedZone.zoneName} (${selectedZone.zoneId})</p>
                        </div>
                    </div>
                    <button id="btn-close-target-modal" class="text-slate-400 hover:text-white font-bold text-sm">✕</button>
                </div>

                <div class="space-y-2 text-xs font-sans">
                    <label class="block text-[10px] uppercase font-bold text-slate-400">Select Devices Bound To Perimeter:</label>
                    <div class="space-y-1.5 max-h-[220px] overflow-y-auto custom-scrollbar p-2 bg-slate-950 rounded-xl border border-white/10">
                        ${this.devices.map(d => {
                            const isChecked = assignedIds.includes(d.agentId);
                            return `
                                <label class="flex items-center justify-between p-2 rounded-lg bg-white/5 hover:bg-sky-950/40 cursor-pointer border border-white/5">
                                    <div class="flex items-center gap-2 min-w-0">
                                        <input type="checkbox" value="${d.name}" class="target-dev-check accent-sky-500 scale-110" ${isChecked ? 'checked' : ''}>
                                        <img src="${d.getAvatarUrl()}" class="w-6 h-6 rounded-full object-cover">
                                        <div class="truncate">
                                            <div class="text-white font-bold text-xs">${d.name}</div>
                                            <div class="text-[9px] text-slate-400 font-mono">${d.model} • ${d.agentId}</div>
                                        </div>
                                    </div>
                                    <span class="text-[8px] font-mono text-sky-400 font-bold">${d.role}</span>
                                </label>
                            `;
                        }).join('')}
                    </div>
                </div>

                <div class="flex items-center justify-end gap-2 pt-2 border-t border-white/10">
                    <button id="btn-cancel-targets" class="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-bold transition-all">Cancel</button>
                    <button id="btn-save-targets" class="px-4 py-2 rounded-lg bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold transition-all flex items-center gap-1 shadow-lg shadow-sky-600/20">
                        <span>Save Targets</span>
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        const close = () => overlay.remove();
        document.getElementById('btn-close-target-modal').onclick = close;
        document.getElementById('btn-cancel-targets').onclick = close;

        document.getElementById('btn-save-targets').onclick = async () => {
            const selectedNames = Array.from(document.querySelectorAll('.target-dev-check:checked')).map(c => c.value);
            const assignedTargetsStr = selectedNames.length > 0 ? selectedNames.join(', ') : "Connected Devices";

            try {
                const res = await fetch('/api/safezone/update-targets', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        creatorUid: selectedZone.creatorId,
                        zoneId: selectedZone.zoneId,
                        assignedTargets: assignedTargetsStr
                    })
                });
                const result = await res.json();
                if (result.status === 'SUCCESS') {
                    if (window.log) window.log(`SAFEZONE_TARGETS_UPDATED: ${selectedZone.zoneName} -> "${assignedTargetsStr}"`, "success");
                    close();
                    if (window.showGeofencing) window.showGeofencing();
                } else {
                    alert(`Target Update Failed: ${result.error}`);
                }
            } catch (err) {
                alert(`Network Failure: ${err.message}`);
            }
        };
    }

    openDeleteZoneModal() {
        const selectedZone = this.safeZones.find(z => z.zoneId === this.selectedZoneId) || this.safeZones[0];
        if (!selectedZone) return;

        if (!confirm(`CONFIRM DESTRUCTION: Permanently decommission SafeZone [${selectedZone.zoneName}] (${selectedZone.zoneId})?`)) return;

        fetch('/api/safezone/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                creatorUid: selectedZone.creatorId,
                zoneId: selectedZone.zoneId
            })
        })
        .then(r => r.json())
        .then(res => {
            if (res.status === 'SUCCESS') {
                if (window.log) window.log(`SAFEZONE_DECOMMISSIONED: ${selectedZone.zoneName}`, "success");
                if (window.showGeofencing) window.showGeofencing();
            } else {
                alert(`Decommission Failed: ${res.error}`);
            }
        })
        .catch(err => alert(`Network Failure: ${err.message}`));
    }
}
