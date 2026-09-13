import { GeoUtils } from '../core/GeoUtils.js';
import { FeedbackDialog } from '../components/FeedbackDialog.js';
import { DeviceEditModalView } from './DeviceEditModalView.js';
import { DeviceController } from '../controllers/DeviceController.js';
import { TelemetryValidator } from '../core/TelemetryValidator.js';
import { UIControls } from '../components/UIControls.js';

/**
 * jFindMe Matrix - Dynamic Device User Oversight Matrix (OOPS Architecture)
 * Strict Relational Database Binding & Zero Hardcoding Policy:
 * All Master, Sub-Master, and Transactional Data points are strictly derived from relative Firebase RTDB entity structures.
 * Enforces Date/Time as Primary Filtering Criteria with Latest Telemetry on Top.
 */
export class DeviceUserView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.onSelectDevice = options.onSelectDevice || null;
        this.onNavigateMap = options.onNavigateMap || null;
        
        this.devices = [];
        this.selectedAgentId = null;
        this.searchQuery = "";
        this.timeWindowFilter = "all"; // 'all', 'live', 'today', '24h'
        this.isOrgExpanded = false;

        // Enterprise Telemetry Controls State (Date/Time, Switch, Toggle, Flipper)
        this.streamMode = "live"; // 'live' | 'history' (Toggle left/right)
        this.deadbandEnabled = true; // Switch (on/off)
        this.selectedDate = new Date().toISOString().slice(0, 10);
        this.selectedTimeWindow = "all";
        this.flipperIndex = 0;
        this.historyBreadcrumbs = [];
        this.expandedFixIds = new Set();
        this.availableHistoryDates = [];
        this.deviceController = new DeviceController();

        this.editModal = new DeviceEditModalView({
            onSave: async (agentId, payload) => {
                try {
                    const res = await (window.fetchApi ? window.fetchApi('/api/update-profile', {
                        method: 'POST',
                        body: JSON.stringify({ agentId, profileData: payload })
                    }) : fetch('/api/update-profile', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ agentId, profileData: payload })
                    }).then(r => r.json()));

                    if (res && res.error) {
                        throw new Error(res.error || 'Update failed');
                    }
                    UIControls.notify(`Profile & Circle updated for ${payload.name || agentId}`, 'success', { duration: 3500 });
                    if (window.showDeviceUser) window.showDeviceUser(agentId);
                } catch (err) {
                    UIControls.notify(`Failed to save profile: ${err.message}`, 'danger', { duration: 4500 });
                    throw err;
                }
            }
        });
    }

    /**
     * Smoothly updates live telemetry from background stream
     */
    updateData(devices = [], safeZones = []) {
        const activeEl = document.activeElement;
        const isEditing = activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA' || activeEl.tagName === 'SELECT');
        if (isEditing) {
            this.devices = DeviceController.sortByLatestFirst(devices);
            return;
        }
        this.render(devices, this.selectedAgentId, safeZones || this.safeZones);
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
        // Primary Rule: Date/Time with Latest Information on Top
        this.devices = DeviceController.sortByLatestFirst(uniqueDevices);
        this.safeZones = safeZones || [];

        if (this.devices.length > 0) {
            if (!this.selectedAgentId || !this.devices.some(d => d.agentId === this.selectedAgentId)) {
                this.selectedAgentId = initialSelectedAgentId || this.devices[0].agentId;
            }
        }

        const container = document.getElementById(this.containerId);
        if (!container) return;

        // Scope devices by temporal window first
        const timeScoped = DeviceController.filterByTimeWindow(this.devices, this.timeWindowFilter);

        // Filter devices by search query
        let filteredDevices = timeScoped;
        if (this.searchQuery && this.searchQuery.trim().length > 0) {
            const q = this.searchQuery.trim().toLowerCase();
            filteredDevices = timeScoped.filter(d => {
                return (d.name && d.name.toLowerCase().includes(q)) ||
                       (d.agentId && d.agentId.toLowerCase().includes(q)) ||
                       (d.model && d.model.toLowerCase().includes(q)) ||
                       (d.deviceName && d.deviceName.toLowerCase().includes(q)) ||
                       (d.role && d.role.toLowerCase().includes(q)) ||
                       (d.address && d.address.toLowerCase().includes(q));
            });
        }
        filteredDevices = DeviceController.sortByLatestFirst(filteredDevices);

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
            <div class="w-full h-full select-text overflow-y-auto custom-scrollbar p-1 font-sans space-y-1">
                
                <!-- COMPACT HEADER BAR (50% REDUCED PADDING) -->
                <div class="card-theme px-2 py-1 flex items-center justify-between shadow-sm">
                    <div class="flex items-center gap-2 min-w-0">
                        <div class="w-6 h-6 rounded-lg bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-sky-400 font-bold text-xs shrink-0">
                            📱
                        </div>
                        <div class="min-w-0">
                            <h2 class="text-[11px] font-bold tracking-wide text-primary-theme uppercase flex items-center gap-1.5">
                                <span>Device User Oversight Matrix</span>
                                <span class="badge-status badge-accent text-[7.5px] px-1 py-0.2">DYNAMIC SSOT</span>
                            </h2>
                            <p class="text-[9px] text-secondary-theme font-mono truncate select-text">
                                Master Identity: <span class="text-sky-400 font-bold">${selectedDev ? selectedDev.name : 'None'}</span> (<span class="copyable-badge text-sky-300 font-mono" data-copy="${selectedDev ? selectedDev.agentId : ''}" title="Click to copy Agent ID">${selectedDev ? selectedDev.agentId : 'N/A'}</span>)
                            </p>
                        </div>
                    </div>

                    <div class="flex items-center gap-1 shrink-0">
                        <button id="btn-du-dup-devices" class="btn-action-primary text-[10.5px] py-0.5 px-2 bg-gradient-to-r from-amber-600 to-amber-500 hover:from-amber-500 hover:to-amber-400 text-slate-950 font-extrabold flex items-center gap-1 shadow-sm border border-amber-400/50" title="Reconcile Duplicate Devices via Silicon SHA-256">
                            <span>🔄 Dup Device</span>
                        </button>
                        ${selectedDev ? `
                        <button id="btn-du-view-map" class="btn-action-primary text-[10.5px] py-0.5 px-2">
                            <span>🗺️ Locate on Map</span>
                        </button>
                        ` : ''}
                    </div>
                </div>

                <!-- 5-PANE MULTI-COLUMN SMART-FIT WORKSPACE (50% REDUCED GAP & PADDING) -->
                <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-1 items-start">
                    
                    <!-- PANE 1: MASTER DEVICE ROSTER & SEARCH BAR (SHOWS DEVICE NAME NOT ROLES) -->
                    <div class="card-theme p-1 flex flex-col space-y-1 shadow-sm">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-0.5">
                            <span class="text-[11px] font-black uppercase text-sky-400 tracking-wider">1. Device Roster</span>
                            <span class="badge-status badge-accent text-[8px] px-1 py-0.2">${filteredDevices.length}/${this.devices.length}</span>
                        </div>

                        <!-- Search Input (Smart Fit) -->
                        <div class="relative flex items-center">
                            <input type="text" id="du-search-input" 
                                   placeholder="🔍 Filter device, name, ID..." 
                                   value="${this.searchQuery}"
                                   class="input-theme w-full pl-5 pr-4 py-0.5 text-[10px] font-mono">
                            <svg class="w-2.5 h-2.5 absolute left-1.5 text-secondary-theme pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                            </svg>
                            ${this.searchQuery ? `
                                <button id="btn-du-clear-search" class="absolute right-1 text-muted-theme hover:text-primary-theme text-[10px] font-bold p-0.5">✕</button>
                            ` : ''}
                        </div>

                        <!-- TEMPORAL TIME WINDOW FILTER TOOLBAR (PANE 1 - ULTRA COMPACT SMART FIT) -->
                        <div class="grid grid-cols-4 gap-0.5 pt-0.5">
                            <button class="btn-du-time-filter text-[7.5px] font-bold py-0.5 px-0.5 rounded truncate text-center transition-all ${this.timeWindowFilter === 'all' ? 'btn-action-primary' : 'btn-action-secondary'}" data-time-filter="all">All (${this.devices.length})</button>
                            <button class="btn-du-time-filter text-[7.5px] font-bold py-0.5 px-0.5 rounded truncate text-center transition-all ${this.timeWindowFilter === 'live' ? 'btn-action-primary' : 'btn-action-secondary'}" data-time-filter="live">⚡Live</button>
                            <button class="btn-du-time-filter text-[7.5px] font-bold py-0.5 px-0.5 rounded truncate text-center transition-all ${this.timeWindowFilter === 'today' ? 'btn-action-primary' : 'btn-action-secondary'}" data-time-filter="today">Today</button>
                            <button class="btn-du-time-filter text-[7.5px] font-bold py-0.5 px-0.5 rounded truncate text-center transition-all ${this.timeWindowFilter === '24h' ? 'btn-action-primary' : 'btn-action-secondary'}" data-time-filter="24h">24h</button>
                        </div>

                        <!-- Roster List (Shows Device Name prominently, Zero Roles) -->
                        <div class="space-y-0.5 overflow-y-auto custom-scrollbar max-h-[540px] pr-0.5" id="du-roster-list">
                            ${filteredDevices.map(dev => {
                                const ragColor = dev.getRagColor();
                                const isSel = dev.agentId === this.selectedAgentId;
                                const recency = typeof dev.getRecencyTier === 'function' ? dev.getRecencyTier() : { badgeClass: 'badge-status badge-accent', label: 'TELEMETRY' };
                                const primaryDeviceName = dev.deviceName || dev.model || 'Android Device';

                                return `
                                    <div class="du-device-item cursor-pointer p-1 rounded-md border ${isSel ? 'border-sky-500 bg-sky-500/10 shadow-sm' : 'border-subtle-theme bg-card-subtle hover:bg-card-hover'} transition-all flex items-center gap-1.5 select-text"
                                         data-agent-id="${dev.agentId}">
                                        <div class="relative shrink-0">
                                            <img src="${dev.getAvatarUrl()}" alt="${primaryDeviceName}" 
                                                 class="w-6 h-6 rounded-full object-cover border shadow-sm" 
                                                 style="border-color: ${ragColor};">
                                            <span class="absolute bottom-0 right-0 w-1.5 h-1.5 rounded-full border border-surface" style="background-color: ${ragColor}"></span>
                                        </div>
                                        <div class="min-w-0 flex-1">
                                            <div class="flex items-center justify-between gap-1">
                                                <h4 class="text-[11px] font-bold truncate text-primary-theme min-w-0" title="${primaryDeviceName}">
                                                    ${primaryDeviceName}
                                                </h4>
                                                <span class="${recency.badgeClass} text-[7px] px-1 py-0.2 font-bold shrink-0">${recency.label}</span>
                                            </div>
                                            <div class="text-[8.5px] text-secondary-theme truncate font-mono flex items-center justify-between mt-0.5 select-text">
                                                <span class="truncate min-w-0 select-text">
                                                    <span class="text-sky-400 font-sans font-medium">${dev.name}</span> • <span class="copyable-badge font-mono hover:text-sky-300" data-copy="${dev.agentId}" title="Click to copy ID">ID:${dev.agentId}</span>
                                                </span>
                                                <span class="text-sky-400 font-sans shrink-0 ml-1 text-[8px]">${dev.getFormattedTimeDelta()}</span>
                                            </div>
                                        </div>
                                    </div>
                                `;
                            }).join('')}
                        </div>
                    </div>

                    ${selectedDev ? `
                    <!-- PANE 2: TIER 1 - MASTER DATA (HARDWARE SILICON SENTINEL & IDENTITY) -->
                    <div class="card-theme p-1 space-y-1 shadow-sm flex flex-col">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-0.5 shrink-0">
                            <div class="flex items-center gap-1 min-w-0">
                                <span class="text-[11px] font-black uppercase text-sky-400 tracking-wider">2. Master Data</span>
                                <span class="badge-status badge-accent text-[7.5px] px-1 py-0.2 font-mono">TIER 1</span>
                            </div>
                            <div class="flex items-center gap-1 shrink-0">
                                <button id="btn-edit-profile-dossier" class="px-1 py-0.2 rounded text-[8px] font-bold bg-sky-500/20 text-sky-400 hover:bg-sky-500/30 border border-sky-500/30 transition-all flex items-center gap-0.5 cursor-pointer">
                                    <span>✏️ Edit</span>
                                </button>
                                <button id="btn-toggle-org" class="text-[8px] text-sky-400 hover:underline font-mono font-bold">
                                    ${this.isOrgExpanded ? '[-] Hide' : '[+] Org'}
                                </button>
                            </div>
                        </div>

                        <div class="overflow-y-auto custom-scrollbar space-y-1 max-h-[540px] pr-0.5">
                            <!-- Tier 1: Master Hardware Anchor & Identity Card -->
                            <div class="surface-theme p-1 rounded-md space-y-0.5 text-xs font-mono border select-text">
                                <div class="text-[9px] font-bold text-sky-400 uppercase tracking-wider font-sans border-b border-subtle-theme pb-0.5 flex items-center justify-between">
                                    <span>🏢 Hardware & Identity</span>
                                    <span class="copyable-badge badge-status badge-accent text-[7.5px] px-1 py-0.2 select-text font-mono" data-copy="${selectedDev.agentId}" title="Click to copy Agent ID">${selectedDev.agentId} 📋</span>
                                </div>

                                <div class="flex items-center gap-1.5 pb-1 border-b border-subtle-theme">
                                    <img src="${selectedDev.getAvatarUrl()}" alt="${selectedDev.name}" class="w-7 h-7 rounded-full object-cover border-2 shadow-sm shrink-0" style="border-color: ${selectedDev.getThemeColor()};">
                                    <div class="min-w-0 flex-1 select-text">
                                        <h3 class="font-bold text-[11px] text-primary-theme truncate select-text">${selectedDev.name}</h3>
                                        <div class="text-[8.5px] text-sky-400 font-sans font-bold truncate">${selectedDev.roleFamily !== 'None' ? selectedDev.roleFamily : selectedDev.role}</div>
                                        <div class="text-[8px] text-secondary-theme truncate">${selectedDev.deviceName || selectedDev.model}</div>
                                    </div>
                                </div>

                                <div class="space-y-0.5 text-[9px] select-text">
                                    <div class="flex justify-between items-center gap-1 py-0.2 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0 text-[8.5px]">Agent ID:</span> 
                                        <span class="text-sky-400 font-bold truncate text-right flex-1 min-w-0 font-mono select-text copyable-badge" data-copy="${selectedDev.agentId}" title="Click to copy">${selectedDev.agentId}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.2 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0 text-[8.5px]">Hardware ID:</span> 
                                        <span class="text-primary-theme font-bold truncate text-right flex-1 min-w-0 font-mono select-text copyable-badge" data-copy="${selectedDev.deviceId}" title="Click to copy">${selectedDev.deviceId}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.2 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0 text-[8.5px]">Auth UID:</span> 
                                        <span class="text-primary-theme font-bold truncate text-right flex-1 min-w-0 font-mono select-text text-[8px]" title="${selectedDev.authUid || 'Unlinked'}">${selectedDev.authUid || (rawNode.Identity && rawNode.Identity.auth_uid) || (rawNode.device && rawNode.device.live && rawNode.device.live.auth_uid) || 'Unlinked'}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.2 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0 text-[8.5px]">Model:</span> 
                                        <span class="text-primary-theme font-bold truncate text-right flex-1 min-w-0 select-text" title="${selectedDev.brand} / ${selectedDev.model}">${selectedDev.brand} / ${selectedDev.model}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.2 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0 text-[8.5px]">Chipset:</span> 
                                        <span class="text-primary-theme font-mono truncate text-right flex-1 min-w-0 select-text" title="${selectedDev.board} / ${selectedDev.hardware}">${selectedDev.board} / ${selectedDev.hardware}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.2 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0 text-[8.5px]">Node:</span> 
                                        <span class="text-primary-theme font-mono truncate text-right flex-1 min-w-0 select-text">${selectedDev.device}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.2 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0 text-[8.5px]">IMEI 1:</span> 
                                        <span class="text-primary-theme font-mono truncate text-right flex-1 min-w-0 select-text copyable-badge" data-copy="${selectedDev.imei1}" title="Click to copy IMEI 1">${selectedDev.imei1}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.2 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0 text-[8.5px]">IMEI 2:</span> 
                                        <span class="text-primary-theme font-mono truncate text-right flex-1 min-w-0 select-text copyable-badge" data-copy="${selectedDev.imei2}" title="Click to copy IMEI 2">${selectedDev.imei2}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.2">
                                        <span class="text-secondary-theme shrink-0 text-[8.5px]">Silicon SHA:</span> 
                                        <span class="text-amber-400 font-bold font-mono truncate text-right flex-1 min-w-0 select-text text-[8px] copyable-badge" data-copy="${selectedDev.siliconHash}" title="Click to copy Silicon SHA">${selectedDev.siliconHash}</span>
                                    </div>
                                </div>
                            </div>

                            <!-- Expandable Org Data Panel -->
                            <div id="org-data-panel" class="surface-theme p-1 rounded-md space-y-0.5 text-xs font-mono border select-text ${this.isOrgExpanded ? 'block' : 'hidden'}">
                                <div class="text-[9px] font-bold text-sky-400 uppercase tracking-wider font-sans border-b border-subtle-theme pb-0.5 flex items-center justify-between">
                                    <span>🏢 Org & Context</span>
                                    <span class="text-[7.5px] text-muted-theme font-mono">ORG</span>
                                </div>
                                <div class="space-y-0.5 text-[9px]">
                                    <div class="flex justify-between py-0.2 border-b border-subtle-theme"><span class="text-secondary-theme">Org:</span> <span class="text-primary-theme font-bold truncate">${(rawNode.profile && rawNode.profile.orgName) || 'jFindMe Fleet'}</span></div>
                                    <div class="flex justify-between py-0.2 border-b border-subtle-theme"><span class="text-secondary-theme">Dept:</span> <span class="text-primary-theme truncate">${selectedDev.department || 'General'}</span></div>
                                    <div class="flex justify-between py-0.2 border-b border-subtle-theme"><span class="text-secondary-theme">Role:</span> <span class="text-primary-theme font-bold truncate">${selectedDev.role}</span></div>
                                    <div class="flex justify-between py-0.2"><span class="text-secondary-theme">Emergency:</span> <span class="${selectedDev.emergencyNote ? 'text-amber-500 font-bold' : 'text-muted-theme'} truncate">${selectedDev.emergencyNote || 'None'}</span></div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 3: TIER 2 - SUB-MASTER DATA (PROFILE, PERSONA & CONNECTIONS) -->
                    <div class="card-theme p-1 space-y-1 shadow-sm flex flex-col">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-0.5 shrink-0">
                            <div class="flex items-center gap-1 min-w-0">
                                <span class="text-[11px] font-black uppercase text-sky-400 tracking-wider">3. Sub-Master</span>
                                <span class="badge-status badge-accent text-[7.5px] px-1 py-0.2 font-mono">TIER 2</span>
                            </div>
                            <span class="badge-status badge-accent text-[8px] px-1 py-0.2 font-mono">📊 ${relCount} Linked</span>
                        </div>

                        <div class="space-y-1 overflow-y-auto custom-scrollbar max-h-[540px] pr-0.5">
                            <!-- Persona Details Card -->
                            <div class="surface-theme p-1 rounded-md space-y-0.5 text-xs font-mono border select-text">
                                <div class="text-[9px] font-bold text-sky-400 uppercase tracking-wider font-sans border-b border-subtle-theme pb-0.5">
                                    👤 Profile Persona
                                </div>
                                <div class="space-y-0.5 text-[9px]">
                                    <div class="flex justify-between py-0.2 border-b border-subtle-theme"><span class="text-secondary-theme">Name:</span> <span class="text-primary-theme font-bold truncate select-text">${selectedDev.name}</span></div>
                                    <div class="flex justify-between py-0.2 border-b border-subtle-theme"><span class="text-secondary-theme">Phone:</span> <span class="text-primary-theme font-mono truncate select-text copyable-badge" data-copy="${selectedDev.phone || ''}" title="Click to copy">${selectedDev.phone || 'Not Registered'}</span></div>
                                    <div class="flex justify-between py-0.2 border-b border-subtle-theme"><span class="text-secondary-theme">Email:</span> <span class="text-primary-theme truncate select-text copyable-badge" data-copy="${selectedDev.email || ''}" title="${selectedDev.email || ''}">${selectedDev.email || 'Not Registered'}</span></div>
                                    <div class="flex justify-between py-0.2 border-b border-subtle-theme"><span class="text-secondary-theme">Family Role:</span> <span class="text-amber-400 font-bold">${selectedDev.roleFamily}</span></div>
                                    <div class="flex justify-between py-0.2"><span class="text-secondary-theme">Friend Role:</span> <span class="text-emerald-400 font-bold">${selectedDev.roleFriend}</span></div>
                                </div>
                            </div>

                            <!-- Tree Container: Family & Friends Circle Connections -->
                            <div class="surface-theme p-1 rounded-md border space-y-0.5 font-mono select-text">
                                <div class="text-[9px] font-bold text-sky-400 uppercase tracking-wider font-sans border-b border-subtle-theme pb-0.5 flex items-center justify-between">
                                    <span>🤝 Circle Connections</span>
                                    <span class="text-[7.5px] text-muted-theme">RELATIONSHIPS</span>
                                </div>

                                <!-- Tree Branches -->
                                <div class="space-y-0.5">
                                    <!-- Family Branch -->
                                    <details class="group/family space-y-0.5" open>
                                        <summary class="cursor-pointer select-none text-[9px] font-bold text-amber-500 font-sans uppercase flex items-center gap-1 list-none hover:text-amber-400">
                                            <span class="inline-block transition-transform group-open/family:rotate-90 text-[8px] font-black">&gt;</span>
                                            <span>👨‍👩‍👧‍👦 Family Circle</span>
                                        </summary>
                                        <div class="space-y-0.5 pl-1.5 border-l border-amber-500/20 mt-0.5">
                                            ${this.renderTreeMembers(selectedDev, 'family')}
                                        </div>
                                    </details>

                                    <!-- Friends Branch -->
                                    <details class="group/friends space-y-0.5" open>
                                        <summary class="cursor-pointer select-none text-[9px] font-bold text-emerald-500 font-sans uppercase flex items-center gap-1 list-none hover:text-emerald-400">
                                            <span class="inline-block transition-transform group-open/friends:rotate-90 text-[8px] font-black">&gt;</span>
                                            <span>🤝 Friends Circle</span>
                                        </summary>
                                        <div class="space-y-0.5 pl-1.5 border-l border-emerald-500/20 mt-0.5">
                                            ${this.renderTreeMembers(selectedDev, 'friends')}
                                        </div>
                                    </details>
                                </div>
                            </div>

                            <!-- Interactive ACL Grants Panel -->
                            <div class="surface-theme p-1 rounded-md border space-y-0.5 font-sans text-xs select-text">
                                <div class="text-[9px] font-bold text-sky-400 uppercase tracking-wider font-sans border-b border-subtle-theme pb-0.5">
                                    🛡️ Access Controls (ACL)
                                </div>
                                <div class="space-y-0.5 pt-0.5">
                                    <div class="flex items-center justify-between p-0.5 px-1 rounded bg-card-subtle border border-subtle-theme">
                                        <span class="text-[9px] font-bold text-primary-theme">Live Tracking</span>
                                        <label class="switch-toggle switch-sm">
                                            <input type="checkbox" class="du-switch-acl" data-agent-id="${selectedDev.agentId}" data-flag="acl_live_tracking" ${selectedDev.acl.liveTracking ? 'checked' : ''}>
                                            <span class="switch-slider"></span>
                                        </label>
                                    </div>
                                    <div class="flex items-center justify-between p-0.5 px-1 rounded bg-card-subtle border border-subtle-theme">
                                        <span class="text-[9px] font-bold text-primary-theme">Geofence Alerts</span>
                                        <label class="switch-toggle switch-sm">
                                            <input type="checkbox" class="du-switch-acl" data-agent-id="${selectedDev.agentId}" data-flag="acl_geofence" ${selectedDev.acl.geofence ? 'checked' : ''}>
                                            <span class="switch-slider"></span>
                                        </label>
                                    </div>
                                    <div class="flex items-center justify-between p-0.5 px-1 rounded bg-card-subtle border border-subtle-theme">
                                        <span class="text-[9px] font-bold text-primary-theme">Route History</span>
                                        <label class="switch-toggle switch-sm">
                                            <input type="checkbox" class="du-switch-acl" data-agent-id="${selectedDev.agentId}" data-flag="acl_history_audit" ${selectedDev.acl.historyAudit ? 'checked' : ''}>
                                            <span class="switch-slider"></span>
                                        </label>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 4: TIER 2 & 3 - SAFEZONES & LIVE TELEMETRY DIAGNOSTICS -->
                    <div class="card-theme p-1 space-y-1 shadow-sm flex flex-col">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-0.5 shrink-0">
                            <div class="flex items-center gap-1 min-w-0">
                                <span class="text-[11px] font-black uppercase text-sky-400 tracking-wider">4. SafeZones</span>
                                <span class="badge-status badge-accent text-[7.5px] px-1 py-0.2 font-mono">TIER 2/3</span>
                            </div>
                            <span class="badge-status badge-accent text-[7.5px] px-1 py-0.2 font-mono">C:${createdZonesList.length} | A:${assignedZonesList.length}</span>
                        </div>

                        <div class="space-y-1 overflow-y-auto custom-scrollbar max-h-[540px] pr-0.5">
                            <!-- Section 1: Created Safe Zones -->
                            <div class="surface-theme p-1 rounded-md border space-y-0.5 select-text">
                                <div class="text-[9px] font-bold text-sky-400 uppercase font-sans flex items-center justify-between border-b border-subtle-theme pb-0.5">
                                    <span>Section 1: Created</span>
                                    <span class="text-[8px] font-mono text-muted-theme">(${createdZonesList.length})</span>
                                </div>
                                <div class="space-y-0.5 max-h-[110px] overflow-y-auto custom-scrollbar">
                                    ${this.renderCreatedSafeZonesHTML(selectedDev, createdZonesList)}
                                </div>
                            </div>

                            <!-- Section 2: Assigned Safe Zones -->
                            <div class="surface-theme p-1 rounded-md border space-y-0.5 select-text">
                                <div class="text-[9px] font-bold text-amber-500 uppercase font-sans flex items-center justify-between border-b border-subtle-theme pb-0.5">
                                    <span>Section 2: Assigned</span>
                                    <span class="text-[8px] font-mono text-muted-theme">(${assignedZonesList.length})</span>
                                </div>
                                <div class="space-y-0.5 max-h-[110px] overflow-y-auto custom-scrollbar">
                                    ${this.renderAssignedSafeZonesHTML(selectedDev, assignedZonesList)}
                                </div>
                            </div>

                            <!-- Live Telemetry & Battery Diagnostics (Tier 3) -->
                            <div class="surface-theme p-1 rounded-md border space-y-0.5 font-mono text-xs select-text">
                                <div class="text-[9px] font-bold text-emerald-400 uppercase font-sans border-b border-subtle-theme pb-0.5 flex items-center justify-between">
                                    <span>⚡ Live Hardware Pulse</span>
                                    <span class="badge-status ${selectedDev.isOnline ? 'badge-success' : 'badge-danger'} text-[7px] px-1 py-0.2">${selectedDev.isOnline ? 'ONLINE' : 'OFFLINE'}</span>
                                </div>
                                <div class="space-y-0.5 text-[9px]">
                                    <div class="flex justify-between py-0.2 border-b border-subtle-theme">
                                        <span class="text-secondary-theme">Battery:</span> 
                                        <span class="font-bold ${selectedDev.battery <= 20 ? 'text-rose-400' : 'text-emerald-400'}">${selectedDev.battery >= 0 ? `${selectedDev.battery}% ${selectedDev.isCharging ? '⚡' : ''}` : 'N/A'}</span>
                                    </div>
                                    <div class="flex justify-between py-0.2 border-b border-subtle-theme">
                                        <span class="text-secondary-theme">Network:</span> 
                                        <span class="text-primary-theme font-bold truncate">${selectedDev.networkType}</span>
                                    </div>
                                    <div class="flex justify-between py-0.2 border-b border-subtle-theme">
                                        <span class="text-secondary-theme">GPS:</span> 
                                        <span class="${selectedDev.isMockGps ? 'text-rose-400 font-bold' : 'text-emerald-400'}">${selectedDev.isMockGps ? '⚠️ Mock' : '✅ Genuine'}</span>
                                    </div>
                                    <div class="flex justify-between py-0.2">
                                        <span class="text-secondary-theme">Last Seen:</span> 
                                        <span class="text-sky-400 font-sans font-bold">${selectedDev.getFormattedTimeDelta()}</span>
                                    </div>
                                </div>
                            </div>

                            <!-- Transactional Geofence Notification Log (Tier 4) -->
                            <div class="surface-theme p-1 rounded-md border space-y-0.5 select-text">
                                <div class="text-[9px] font-bold text-secondary-theme uppercase font-sans border-b border-subtle-theme pb-0.5">Geofence Event Log</div>
                                <div class="space-y-0.5 text-[8px] font-mono max-h-[90px] overflow-y-auto custom-scrollbar">
                                    ${this.renderNotificationItems(selectedDev, rawNode)}
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 5: TIER 4 - TRANSACTIONAL LOCATION FIX & TIME-SERIES MATRIX -->
                    <div id="pane-5-container" class="card-theme p-1 space-y-1 shadow-sm flex flex-col">
                        ${this.renderPane5HTML(selectedDev, rawNode)}
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
            const isFriend = roleStr.includes('friend') || roleStr.includes('colleague') || roleStr.includes('peer') || roleStr.includes('buddy');
            return type === 'family' ? !isFriend : isFriend;
        });

        if (matchedNodes.length === 0) {
            return `<div class="text-[10px] text-muted-theme italic py-1 font-mono">No linked ${type} connections.</div>`;
        }

        return matchedNodes.map(([agentId, data]) => {
            const cleanKey = String(agentId).replace(/^HW_/, '').toUpperCase();
            const peerDev = this.devices.find(d => {
                const dClean = String(d.agentId).replace(/^HW_/, '').toUpperCase();
                return d.agentId === agentId || 
                       d.deviceId === agentId || 
                       dClean === cleanKey ||
                       (data.name && d.name && d.name.toLowerCase() === String(data.name).toLowerCase());
            });

            // Prioritize true human/friendly persona name over raw hardware IDs (e.g. "REDMI N7" instead of "2T929153")
            let peerName = "";
            if (peerDev && peerDev.name && peerDev.name !== peerDev.agentId && !peerDev.name.startsWith("HW_")) {
                peerName = peerDev.name;
            } else if (data.name && data.name !== agentId && !data.name.startsWith("HW_") && !this.devices.some(d => d.agentId === data.name)) {
                peerName = data.name;
            } else if (peerDev && (peerDev.deviceName || peerDev.model)) {
                peerName = peerDev.deviceName || peerDev.model;
            } else {
                peerName = data.name || (peerDev ? peerDev.name : agentId);
            }

            const peerAvatar = peerDev ? peerDev.getAvatarUrl() : `data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>`;
            const peerTargetId = peerDev ? peerDev.agentId : agentId;
            const peerRag = peerDev ? peerDev.getRagColor() : '#38bdf8';
            const peerSubtext = peerDev ? `${peerDev.deviceName || peerDev.model || 'Android'} • ${peerTargetId}` : `ID: ${peerTargetId}`;

            return `
                <div class="du-tree-node flex items-center justify-between p-1.5 rounded-md bg-card-subtle hover:bg-card-hover text-xs border border-subtle-theme cursor-pointer transition-all group select-text"
                     data-target-agent-id="${peerTargetId}">
                    <div class="flex items-center gap-1.5 min-w-0">
                        <img src="${peerAvatar}" class="w-4 h-4 rounded-full object-cover shrink-0 border" style="border-color: ${peerRag}">
                        <div class="min-w-0 flex-1">
                            <div class="truncate text-primary-theme font-bold text-xs select-text">${peerName}</div>
                            <div class="text-[8.5px] text-secondary-theme font-mono truncate select-text">${peerSubtext}</div>
                        </div>
                    </div>
                    <div class="flex items-center gap-1 shrink-0">
                        <span class="badge-status ${type === 'family' ? 'badge-accent' : 'badge-success'} text-[7.5px] font-bold uppercase">${data.role || (type === 'family' ? 'Family' : 'Friend')}</span>
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
            const zLat = z.lat || z.latitude || 0;
            const zLng = z.lng || z.longitude || 0;

            return `
                <div class="p-1.5 rounded-md bg-card-subtle border border-subtle-theme text-xs space-y-0.5 select-text">
                    <div class="flex items-center justify-between font-bold text-primary-theme">
                        <span class="truncate text-sky-400 font-bold select-text">${zName}</span>
                        <div class="flex items-center gap-1 shrink-0">
                            <span class="badge-status badge-accent text-[7.5px]">CREATED</span>
                            <button class="btn-locate-zone px-1 py-0.5 rounded text-[8px] font-bold bg-sky-500/20 text-sky-400 hover:bg-sky-500/30 border border-sky-500/30"
                                    data-lat="${zLat}" data-lng="${zLng}" data-name="${zName}" title="Locate on map radar">🎯</button>
                        </div>
                    </div>
                    <div class="text-[9.5px] text-secondary-theme font-mono select-text truncate">Assigned: <span class="text-amber-500 font-bold">${targets}</span> • ${zRadius}m</div>
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
            const zLat = z.lat || z.latitude || 0;
            const zLng = z.lng || z.longitude || 0;

            return `
                <div class="p-1.5 rounded-md bg-card-subtle border border-subtle-theme text-xs space-y-0.5 select-text">
                    <div class="flex items-center justify-between font-bold text-primary-theme">
                        <span class="truncate text-amber-500 font-bold select-text">${zName}</span>
                        <div class="flex items-center gap-1 shrink-0">
                            <span class="badge-status ${stateTag === 'OUT' ? 'badge-warning' : 'badge-success'} text-[7.5px]">${stateTag}</span>
                            <button class="btn-locate-zone px-1 py-0.5 rounded text-[8px] font-bold bg-sky-500/20 text-sky-400 hover:bg-sky-500/30 border border-sky-500/30"
                                    data-lat="${zLat}" data-lng="${zLng}" data-name="${zName}" title="Locate on map radar">🎯</button>
                        </div>
                    </div>
                    <div class="text-[9.5px] text-secondary-theme font-mono select-text truncate">Radius: ${zRadius}m • <span class="text-emerald-500 font-bold">${stateTag}</span></div>
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
            <div class="p-1.5 rounded-md bg-card-subtle border border-subtle-theme space-y-0.5 select-text">
                <div class="flex items-center justify-between font-bold ${lastTransState === 'IN' ? 'text-emerald-500' : 'text-amber-500'} text-[10px]">
                    <span class="truncate">${lastTransState === 'IN' ? '🟢 IN' : '🔴 OUT'} ${lastTransZone}</span>
                    <span class="text-[8px] text-muted-theme shrink-0">${lastTransTime || 'Recent'}</span>
                </div>
                <div class="text-secondary-theme text-[9px] truncate">State: ${lastTransState} perimeter</div>
            </div>
        `;
    }

    /**
     * Renders Pane 5 (Spatial Telemetry & History Matrix with Rich Interactive Controls)
     */
    renderPane5HTML(device, rawNode) {
        if (!device) return `<div class="text-xs text-muted-theme italic py-4">No device selected.</div>`;

        const points = this.getAggregatedTelemetryPoints(device, rawNode);
        const activeCount = points.length;

        return `
            <div class="flex items-center justify-between border-b border-subtle-theme pb-0.5 shrink-0">
                <div class="flex items-center gap-1 min-w-0">
                    <span class="text-[11px] font-black uppercase text-sky-400 tracking-wider">5. Telemetry</span>
                    <span class="badge-status badge-accent text-[7.5px] px-1 py-0.2">${activeCount} Pts</span>
                </div>
                
                <!-- Controls Right: Toggle (Left/Right) & Multi-Option Dialog Button -->
                <div class="flex items-center gap-1 shrink-0">
                    <!-- Toggle (Left/Right): Live vs History Stream -->
                    <div class="inline-flex p-0.2 rounded bg-surface border border-subtle-theme text-[8px] font-mono">
                        <button id="btn-toggle-stream-live" class="px-1 py-0.2 rounded font-bold transition-all ${this.streamMode === 'live' ? 'btn-action-primary' : 'text-secondary-theme hover:text-primary-theme'}">
                            📡 Live
                        </button>
                        <button id="btn-toggle-stream-history" class="px-1 py-0.2 rounded font-bold transition-all ${this.streamMode === 'history' ? 'btn-action-primary' : 'text-secondary-theme hover:text-primary-theme'}">
                            📜 Trail
                        </button>
                    </div>

                    <!-- Multi-Option Dialog Trigger Button -->
                    <button id="btn-telemetry-options" class="btn-action-secondary text-[8px] py-0.2 px-1 flex items-center gap-0.5" title="Telemetry options modal">
                        <span>⚡ Opts</span>
                    </button>
                </div>
            </div>

            <!-- Primary Filters: Date Preset Dropdown, Date Picker, Time Window, & Switch (Smart-Fit 2 Rows) -->
            <div class="surface-theme p-1 rounded-md border space-y-0.5 font-mono text-[9px]">
                <!-- Smart Fit Date / Time Filter Rows -->
                <div class="space-y-0.5">
                    ${this.availableHistoryDates.length > 0 ? `
                        <select id="du-telemetry-date-preset" class="input-theme w-full px-1 py-0.5 text-[8.5px] font-mono">
                            <option value="">Detected Days (${this.availableHistoryDates.length})</option>
                            ${this.availableHistoryDates.map(d => `
                                <option value="${d.date}" ${this.selectedDate === d.date ? 'selected' : ''}>
                                    ${d.date} (${d.count.toLocaleString()} fixes)
                                </option>
                            `).join('')}
                        </select>
                    ` : ''}

                    <!-- Row 1: Full-width Date Picker (Prevents overflow) -->
                    <input type="date" id="du-telemetry-date" 
                           value="${this.selectedDate}" 
                           class="input-theme w-full px-1.5 py-0.5 text-[9px] font-mono">

                    <!-- Row 2: Time Window Dropdown + Deadband Switch + Go Button -->
                    <div class="flex items-center justify-between gap-1 pt-0.5">
                        <select id="du-telemetry-time" class="input-theme flex-1 min-w-0 px-1 py-0.5 text-[8.5px] font-mono">
                            <option value="all" ${this.selectedTimeWindow === 'all' ? 'selected' : ''}>All Times</option>
                            <option value="1h" ${this.selectedTimeWindow === '1h' ? 'selected' : ''}>Past 1h</option>
                            <option value="6h" ${this.selectedTimeWindow === '6h' ? 'selected' : ''}>Past 6h</option>
                            <option value="morning" ${this.selectedTimeWindow === 'morning' ? 'selected' : ''}>06-12h</option>
                            <option value="evening" ${this.selectedTimeWindow === 'evening' ? 'selected' : ''}>18-24h</option>
                        </select>

                        <div class="flex items-center gap-1 shrink-0" title="Stationary noise deadband (<15m)">
                            <label class="switch-toggle switch-sm">
                                <input type="checkbox" id="switch-deadband-filter" ${this.deadbandEnabled ? 'checked' : ''}>
                                <span class="switch-slider"></span>
                            </label>
                            <span class="text-[7.5px] font-mono font-bold text-sky-400">15m</span>
                        </div>

                        <button id="btn-apply-date-filter" class="btn-action-primary text-[8px] py-0.5 px-2 shrink-0">
                            Go
                        </button>
                    </div>
                </div>
            </div>

            <!-- Flipper Card: Interactive Waypoint Stepper -->
            ${this.renderFlipperCard(device, points)}

            <!-- List Box with List Details (Dropdown Expandable Accordion) -->
            <div class="surface-theme p-1 rounded-md border flex-1 flex flex-col min-h-0 overflow-hidden space-y-0.5">
                <div class="flex items-center justify-between text-[9px] font-bold text-sky-400 uppercase font-sans border-b border-subtle-theme pb-0.5 shrink-0">
                    <span>Fix Records</span>
                    <span class="text-[7.5px] font-mono text-muted-theme">Latest on top</span>
                </div>
                <div id="du-telemetry-list-box" class="space-y-0.5 overflow-y-auto custom-scrollbar flex-1 pr-0.5 text-xs font-mono">
                    ${this.renderTelemetryListBox(points, device)}
                </div>
            </div>
        `;
    }

    /**
     * Resolves and downsamples telemetry points according to active Date/Time filters
     */
    getAggregatedTelemetryPoints(device, rawNode) {
        let points = [];

        // 1. Live Fix
        if (device && device.hasValidLocation) {
            points.push({
                id: 'live_fix',
                timestamp: typeof device.getEpochTimestamp === 'function' ? device.getEpochTimestamp() : Date.now(),
                formattedTime: device.getFormattedDateTime(),
                timeDelta: device.getFormattedTimeDelta(),
                latitude: device.latitude,
                longitude: device.longitude,
                coords: device.getFormattedCoords(),
                address: device.address || `${device.latitude.toFixed(5)}°, ${device.longitude.toFixed(5)}°`,
                speed: parseFloat(device.speed || 0),
                bearing: Math.round(device.bearing || 0),
                altitude: parseFloat(device.altitude || 0),
                accuracy: parseFloat(device.accuracy || 10),
                isLive: true
            });
        }

        // 2. Historical Points (from cached API or raw node)
        const sourceHistory = this.historyBreadcrumbs.length > 0 
            ? this.historyBreadcrumbs 
            : Object.values(rawNode.history || {});

        for (const [idx, h] of sourceHistory.entries()) {
            if (!h) continue;
            const lat = Number(h.latitude || h.lat || 0);
            const lng = Number(h.longitude || h.lng || 0);
            if (lat === 0 && lng === 0) continue;

            const t = Number(h.timestamp || h.time) || Date.now();
            const dateStr = new Date(t).toISOString().slice(0, 10);

            // Filter by Date if in history mode or if a specific date is chosen
            if (this.streamMode === 'history' && this.selectedDate && dateStr !== this.selectedDate) {
                continue;
            }

            // Filter by Time Window
            if (this.selectedTimeWindow !== 'all') {
                const hour = new Date(t).getHours();
                const ageHours = (Date.now() - t) / 3600000;
                if (this.selectedTimeWindow === '1h' && ageHours > 1) continue;
                if (this.selectedTimeWindow === '6h' && ageHours > 6) continue;
                if (this.selectedTimeWindow === 'morning' && (hour < 6 || hour >= 12)) continue;
                if (this.selectedTimeWindow === 'evening' && (hour < 18 || hour > 23)) continue;
            }

            points.push({
                id: h.id || `hist_${idx}_${t}`,
                timestamp: t,
                formattedTime: GeoUtils.formatDateTime(t),
                timeDelta: GeoUtils.formatRelativeTime(t),
                latitude: lat,
                longitude: lng,
                coords: GeoUtils.formatCoords(lat, lng),
                address: h.address || `${lat.toFixed(5)}°, ${lng.toFixed(5)}°`,
                speed: parseFloat(h.speed || 0),
                bearing: Math.round(h.bearing || 0),
                altitude: parseFloat(h.altitude || 0),
                accuracy: parseFloat(h.accuracy || 15),
                isLive: false
            });
        }

        // Apply Deadband Noise Filter (Suppress stationary points < 15m)
        if (this.deadbandEnabled && points.length > 1) {
            const chron = [...points].sort((a, b) => a.timestamp - b.timestamp);
            const deadbanded = [chron[0]];
            for (let i = 1; i < chron.length; i++) {
                const prev = deadbanded[deadbanded.length - 1];
                const curr = chron[i];
                const distM = TelemetryValidator.calculateDistanceMeters(prev.latitude, prev.longitude, curr.latitude, curr.longitude);
                const speedKmh = curr.speed > 50 ? curr.speed : curr.speed * 3.6;

                // Keep point only if user moved > 15m or moving speed > 1.5 km/h, or if it is the latest live point
                if (distM >= 15 || speedKmh >= 1.5 || curr.isLive) {
                    curr.distDeltaMeters = distM;
                    deadbanded.push(curr);
                }
            }
            points = deadbanded;
        }

        // Sort descending: Latest Telemetry On Top
        points.sort((a, b) => b.timestamp - a.timestamp);

        // Keep flipper index in bounds
        if (this.flipperIndex >= points.length) {
            this.flipperIndex = Math.max(0, points.length - 1);
        }

        return points;
    }

    /**
     * Renders Interactive Waypoint Flipper Card
     */
    renderFlipperCard(device, points) {
        if (points.length === 0) {
            return `
                <div class="surface-theme p-1 rounded-md border text-center text-[8.5px] text-muted-theme italic font-mono select-text">
                    No active telemetry fixes recorded for this criteria.
                </div>
            `;
        }

        const curr = points[this.flipperIndex] || points[0];
        const distDeltaStr = curr.distDeltaMeters ? ` • Δd: ${curr.distDeltaMeters.toFixed(0)}m` : '';

        return `
            <div class="surface-theme p-1 rounded-md border space-y-0.5 font-mono text-[9px] select-text">
                <div class="flex items-center justify-between border-b border-subtle-theme pb-0.5">
                    <div class="flex items-center gap-1 font-sans font-bold text-sky-400">
                        <span class="text-[9.5px]">🎯 FLIPPER</span>
                        <span class="badge-status badge-accent text-[7px] px-1 py-0.2">Pt ${this.flipperIndex + 1}/${points.length}</span>
                    </div>

                    <!-- Flipper Controls (Prev / Next Buttons & Locate) -->
                    <div class="flex items-center gap-0.5">
                        <button id="btn-flipper-prev" class="btn-action-secondary px-1 py-0.2 text-[8px] font-bold" ${this.flipperIndex <= 0 ? 'disabled style="opacity:0.4"' : ''}>
                            ◀
                        </button>
                        <button id="btn-flipper-next" class="btn-action-secondary px-1 py-0.2 text-[8px] font-bold" ${this.flipperIndex >= points.length - 1 ? 'disabled style="opacity:0.4"' : ''}>
                            ▶
                        </button>
                        <button id="btn-flipper-locate" class="px-1 py-0.2 rounded text-[8px] font-bold bg-sky-500/20 text-sky-400 hover:bg-sky-500/30 border border-sky-500/30 transition-all flex items-center cursor-pointer" title="Center on map radar">
                            <span>🎯</span>
                        </button>
                    </div>
                </div>

                <!-- Active Flipper Readout -->
                <div class="space-y-0.5 select-text">
                    <div class="flex items-center justify-between text-primary-theme font-bold text-[9.5px]">
                        <span>🕒 ${curr.formattedTime}</span>
                        <span class="${curr.isLive ? 'badge-status badge-success' : 'badge-status badge-accent'} text-[7px] px-1 py-0.2">${curr.isLive ? 'LIVE' : curr.timeDelta}</span>
                    </div>
                    <div class="text-sky-400 font-bold select-text font-mono flex items-center justify-between">
                        <span class="copyable-badge truncate text-[8.5px]" data-copy="${curr.coords}" title="Click to copy coordinates">📍 ${curr.coords} 📋</span>
                    </div>
                    <div class="text-secondary-theme text-[8px] truncate select-text" title="${curr.address}">🏠 ${curr.address}</div>
                    <div class="flex items-center justify-between text-[7.5px] text-muted-theme pt-0.5 border-t border-subtle-theme">
                        <span>${curr.speed.toFixed(1)} km/h • ${curr.bearing}°</span>
                        <span>±${curr.accuracy.toFixed(0)}m${distDeltaStr}</span>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Renders List Box with List Details (Dropdown Expandable Accordion)
     */
    renderTelemetryListBox(points, device) {
        if (points.length === 0) {
            return `<div class="text-[8.5px] text-muted-theme italic py-1 text-center">No location history matches the date/time filter.</div>`;
        }

        return points.map((item, index) => {
            const isExpanded = this.expandedFixIds.has(item.id);
            const isSelectedFlipper = (index === this.flipperIndex);

            return `
                <div class="p-1 rounded-md border ${isSelectedFlipper ? 'border-sky-500 bg-sky-500/10' : 'border-subtle-theme bg-card-subtle hover:bg-card-hover'} transition-all space-y-0.5 select-text">
                    <!-- Clickable Header Row -->
                    <div class="du-list-item-header flex items-center justify-between cursor-pointer" data-item-id="${item.id}" data-index="${index}">
                        <div class="flex items-center gap-1 min-w-0">
                            <span class="text-[8px] text-sky-400 font-bold">${isExpanded ? '▼' : '▶'}</span>
                            <span class="text-[9px] font-bold text-primary-theme truncate">🕒 ${item.formattedTime}</span>
                        </div>
                        <div class="flex items-center gap-1 shrink-0">
                            <span class="text-[8px] font-bold text-emerald-500">${item.speed.toFixed(1)} km/h</span>
                            <span class="badge-status ${item.isLive ? 'badge-success' : 'badge-accent'} text-[7px] px-1 py-0.2">${item.isLive ? 'LIVE' : item.timeDelta}</span>
                        </div>
                    </div>

                    <!-- Primary Location Summary Line -->
                    <div class="text-[8.5px] text-secondary-theme truncate pl-2 font-mono select-text flex items-center justify-between">
                        <span class="truncate copyable-badge" data-copy="${item.coords}" title="Click to copy">${item.coords}</span>
                        <span class="truncate ml-1 text-muted-theme text-[8px]">${item.address}</span>
                    </div>

                    <!-- Expandable Dropdown Details Box -->
                    ${isExpanded ? `
                        <div class="pl-2 pt-0.5 border-t border-subtle-theme text-[8px] space-y-0.5 font-mono text-secondary-theme bg-surface/40 p-1 rounded mt-0.5 select-text">
                            <div class="flex justify-between"><span>Latitude:</span> <span class="text-primary-theme font-bold select-text copyable-badge" data-copy="${item.latitude.toFixed(6)}">${item.latitude.toFixed(6)}°</span></div>
                            <div class="flex justify-between"><span>Longitude:</span> <span class="text-primary-theme font-bold select-text copyable-badge" data-copy="${item.longitude.toFixed(6)}">${item.longitude.toFixed(6)}°</span></div>
                            <div class="flex justify-between"><span>Speed & Heading:</span> <span class="text-primary-theme font-bold">${item.speed.toFixed(1)} km/h @ ${item.bearing}°</span></div>
                            <div class="flex justify-between"><span>Altitude & Accuracy:</span> <span class="text-primary-theme font-bold">${item.altitude.toFixed(1)}m (±${item.accuracy.toFixed(0)}m)</span></div>
                            <div class="flex justify-between"><span>Epoch Timestamp:</span> <span class="text-sky-400 font-bold select-text copyable-badge" data-copy="${item.timestamp}">${item.timestamp}</span></div>
                            <div class="flex justify-between"><span>SafeZone State:</span> <span class="text-emerald-400 font-bold">${device.safeZonesCount > 0 ? 'MONITORED' : 'NO_PERIMETER'}</span></div>
                        </div>
                    ` : ''}
                </div>
            `;
        }).join('');
    }

    /**
     * Asynchronously loads historical telemetry for the selected device
     */
    async loadHistoricalTelemetry(agentId) {
        if (!agentId) return;
        try {
            // Discover available dates if not already discovered
            if (this.availableHistoryDates.length === 0) {
                const dates = await this.deviceController.fetchHistoricalDates(agentId);
                this.availableHistoryDates = dates || [];
                if (this.availableHistoryDates.length > 0 && !this.selectedDate) {
                    this.selectedDate = this.availableHistoryDates[0].date;
                }
            }

            // Query history with date/time filters & deadbanding from backend
            const crumbs = await this.deviceController.fetchDeviceHistory(agentId, {
                startDate: this.selectedDate,
                endDate: this.selectedDate,
                deadband: this.deadbandEnabled,
                order: 'desc',
                limit: 500
            });

            this.historyBreadcrumbs = crumbs || [];
            this.flipperIndex = 0;
            this.updatePane5Content();

            FeedbackDialog.showSlideNotification({
                title: "Historical Trail Loaded",
                message: `Loaded ${this.historyBreadcrumbs.length} breadcrumb fixes for date ${this.selectedDate}.`,
                type: "success",
                duration: 4000
            });
        } catch (err) {
            console.error("[DU_VIEW] History load failure:", err);
            FeedbackDialog.showSlideNotification({
                title: "History Fetch Failed",
                message: err.message,
                type: "error"
            });
        }
    }

    /**
     * Dynamically updates only Pane 5 DOM without disturbing scroll or search state
     */
    updatePane5Content() {
        const pane5Container = document.getElementById("pane-5-container");
        const selectedDev = this.devices.find(d => d.agentId === this.selectedAgentId) || this.devices[0];
        if (!pane5Container || !selectedDev) return;

        const rawNode = selectedDev.raw || {};
        pane5Container.innerHTML = this.renderPane5HTML(selectedDev, rawNode);
        this.bindPane5Events(selectedDev, rawNode);
    }

    /**
     * Opens Rich Action Modal for Telemetry Operations (Replay, Downsample, Export, Purge)
     */
    openTelemetryOptionsModal(device, points) {
        if (!device) return;

        FeedbackDialog.showActionModal({
            title: `Telemetry Operations (${device.name})`,
            message: `Selected Date: ${this.selectedDate} • Active Points: ${points.length}`,
            icon: "⚡",
            actions: [
                { id: 'replay_trail', label: 'Replay Route on Live Map', description: 'Draw animated breadcrumb trail and launch playback HUD on radar', icon: '🗺️', variant: 'primary', badge: 'MAP' },
                { id: 'downsample_rdp', label: 'Apply RDP Downsampling', description: 'Compress collinear points by 80% to optimize render speed', icon: '📉', variant: 'secondary', badge: 'RDP' },
                { id: 'export_geojson', label: 'Export Telemetry (GeoJSON)', description: 'Download complete GIS track with time coordinates', icon: '📥', variant: 'secondary', badge: 'JSON' },
                { id: 'export_csv', label: 'Export Telemetry (CSV)', description: 'Download tabular spreadsheet for audit and analysis', icon: '📊', variant: 'secondary', badge: 'CSV' },
                { id: 'purge_history', label: 'Purge Old Telemetry (>30 Days)', description: 'Enforce retention policy and remove aged breadcrumbs', icon: '🧹', variant: 'danger', badge: 'AUDIT' }
            ],
            onSelect: async (actionId) => {
                if (actionId === 'replay_trail') {
                    if (window.MatrixApp && window.MatrixApp.showHistory) {
                        window.showLiveMap?.();
                        setTimeout(() => {
                            window.MatrixApp.showHistory(device.agentId);
                        }, 300);
                    } else if (this.onNavigateMap) {
                        this.onNavigateMap(device.agentId);
                    }
                    FeedbackDialog.showSlideNotification({
                        title: "Route Playback Launched",
                        message: `Rendering trail playback for ${device.name} on map.`,
                        type: "success"
                    });
                } else if (actionId === 'downsample_rdp') {
                    const origCount = points.length;
                    const compressed = TelemetryValidator.downsampleRDP(points, 0.0001);
                    this.historyBreadcrumbs = compressed;
                    this.updatePane5Content();

                    const saved = origCount - compressed.length;
                    const pct = origCount > 0 ? ((saved / origCount) * 100).toFixed(0) : 0;
                    FeedbackDialog.showSlideNotification({
                        title: "RDP Downsampling Applied",
                        message: `Reduced ${origCount} points to ${compressed.length} points (-${pct}% memory saved).`,
                        type: "success"
                    });
                } else if (actionId === 'export_geojson') {
                    const geojson = {
                        type: "FeatureCollection",
                        features: [{
                            type: "Feature",
                            geometry: {
                                type: "LineString",
                                coordinates: points.map(p => [p.longitude, p.latitude, p.altitude || 0])
                            },
                            properties: {
                                deviceName: device.name,
                                agentId: device.agentId,
                                date: this.selectedDate,
                                pointCount: points.length
                            }
                        }]
                    };
                    const blob = new Blob([JSON.stringify(geojson, null, 2)], { type: "application/json" });
                    const url = URL.createObjectURL(blob);
                    const a = document.createElement("a");
                    a.href = url;
                    a.download = `jFindMe_${device.agentId}_${this.selectedDate}.geojson`;
                    a.click();
                    URL.revokeObjectURL(url);
                    FeedbackDialog.showSlideNotification({ title: "Export Ready", message: "GeoJSON trail downloaded.", type: "info" });
                } else if (actionId === 'export_csv') {
                    const headers = "timestamp,readableTime,latitude,longitude,speed,bearing,altitude,accuracy,address\n";
                    const rows = points.map(p => `"${p.timestamp}","${p.formattedTime}",${p.latitude},${p.longitude},${p.speed},${p.bearing},${p.altitude},${p.accuracy},"${(p.address||'').replace(/"/g, '""')}"`).join("\n");
                    const blob = new Blob([headers + rows], { type: "text/csv" });
                    const url = URL.createObjectURL(blob);
                    const a = document.createElement("a");
                    a.href = url;
                    a.download = `jFindMe_${device.agentId}_${this.selectedDate}.csv`;
                    a.click();
                    URL.revokeObjectURL(url);
                    FeedbackDialog.showSlideNotification({ title: "Export Ready", message: "CSV spreadsheet downloaded.", type: "info" });
                } else if (actionId === 'purge_history') {
                    FeedbackDialog.showConfirm({
                        title: "Purge Historical Telemetry",
                        message: `Are you sure you want to permanently purge records older than 30 days for [${device.name}]?`,
                        confirmText: "Purge Old Records",
                        confirmClass: "btn-action-danger",
                        onConfirm: async () => {
                            try {
                                const res = await (window.fetchApi ? window.fetchApi('/api/history-purge', {
                                    method: 'POST',
                                    body: JSON.stringify({ agentId: device.agentId, daysOlderThan: 30 })
                                }) : fetch('/api/history-purge', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({ agentId: device.agentId, daysOlderThan: 30 })
                                }).then(r => r.json()));
                                const data = res || {};
                                FeedbackDialog.showSlideNotification({
                                    title: "Purge Completed",
                                    message: `Removed ${data.purgedCount || 0} expired telemetry records.`,
                                    type: "warning"
                                });
                                this.loadHistoricalTelemetry(device.agentId);
                            } catch (e) {
                                FeedbackDialog.showToast(`Purge failed: ${e.message}`, "error");
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Binds all Interactive Event Handlers specifically for Pane 5 Controls
     */
    bindPane5Events(selectedDev, rawNode) {
        if (!selectedDev) return;

        // 1. Toggle (Left/Right): Live vs History Stream Mode
        document.getElementById('btn-toggle-stream-live')?.addEventListener('click', () => {
            this.streamMode = 'live';
            this.updatePane5Content();
            FeedbackDialog.showSlideNotification({
                title: "Live Radar Stream",
                message: `Displaying real-time telemetry fix for ${selectedDev.name}.`,
                type: "info",
                duration: 3000
            });
        });

        document.getElementById('btn-toggle-stream-history')?.addEventListener('click', () => {
            this.streamMode = 'history';
            this.loadHistoricalTelemetry(selectedDev.agentId);
        });

        // 2. Switch (On/Off): Stationary Deadband Filter
        const switchDeadband = document.getElementById('switch-deadband-filter');
        if (switchDeadband) {
            switchDeadband.addEventListener('change', (e) => {
                this.deadbandEnabled = e.target.checked;
                this.updatePane5Content();
                FeedbackDialog.showSlideNotification({
                    title: "Deadband Noise Filter",
                    message: this.deadbandEnabled ? "Stationary filter ACTIVE: Suppressing jitter <15m." : "Raw Stream ACTIVE: Showing uncompressed fixes.",
                    type: this.deadbandEnabled ? "success" : "warning",
                    duration: 3500
                });
            });
        }

        // 3. Date & Time Window Pickers
        document.getElementById('du-telemetry-date-preset')?.addEventListener('change', (e) => {
            if (e.target.value) {
                this.selectedDate = e.target.value;
                const dateInput = document.getElementById('du-telemetry-date');
                if (dateInput) dateInput.value = e.target.value;
                this.loadHistoricalTelemetry(selectedDev.agentId);
            }
        });

        document.getElementById('btn-apply-date-filter')?.addEventListener('click', () => {
            const dateInput = document.getElementById('du-telemetry-date');
            const timeSelect = document.getElementById('du-telemetry-time');
            if (dateInput) this.selectedDate = dateInput.value;
            if (timeSelect) this.selectedTimeWindow = timeSelect.value;
            this.loadHistoricalTelemetry(selectedDev.agentId);
        });

        // 4. Flipper Steppers (Prev / Next / Locate)
        document.getElementById('btn-flipper-prev')?.addEventListener('click', () => {
            if (this.flipperIndex > 0) {
                this.flipperIndex--;
                this.updatePane5Content();
            } else {
                FeedbackDialog.showSlideNotification({
                    title: "Flipper Boundary",
                    message: "At the latest recorded waypoint fix.",
                    type: "info",
                    duration: 2000
                });
            }
        });

        document.getElementById('btn-flipper-next')?.addEventListener('click', () => {
            const points = this.getAggregatedTelemetryPoints(selectedDev, rawNode);
            if (this.flipperIndex < points.length - 1) {
                this.flipperIndex++;
                this.updatePane5Content();
            } else {
                FeedbackDialog.showSlideNotification({
                    title: "Flipper Boundary",
                    message: "Reached the earliest waypoint in selected window.",
                    type: "info",
                    duration: 2000
                });
            }
        });

        document.getElementById('btn-flipper-locate')?.addEventListener('click', () => {
            const points = this.getAggregatedTelemetryPoints(selectedDev, rawNode);
            const curr = points[this.flipperIndex] || points[0];
            if (curr) {
                if (window.showLiveMap) window.showLiveMap();
                setTimeout(() => {
                    if (window.MatrixApp && window.MatrixApp.focusDevice) {
                        window.MatrixApp.focusDevice(selectedDev.agentId);
                    }
                    if (window.appInstance?.mapController?.mapView?.map) {
                        window.appInstance.mapController.mapView.map.setView([curr.latitude, curr.longitude], 17);
                    }
                }, 300);
                FeedbackDialog.showSlideNotification({
                    title: "Radar Locked",
                    message: `Map centered on waypoint ${this.flipperIndex + 1}: ${curr.coords}`,
                    type: "success",
                    duration: 3000
                });
            }
        });

        // 5. List Box with Details (Dropdown Expand/Collapse)
        document.querySelectorAll('.du-list-item-header').forEach(el => {
            el.addEventListener('click', () => {
                const id = el.getAttribute('data-item-id');
                const idx = parseInt(el.getAttribute('data-index'), 10);
                if (id) {
                    if (this.expandedFixIds.has(id)) {
                        this.expandedFixIds.delete(id);
                    } else {
                        this.expandedFixIds.add(id);
                    }
                }
                if (!isNaN(idx)) {
                    this.flipperIndex = idx;
                }
                this.updatePane5Content();
            });
        });

        // 6. Options Modal Trigger
        document.getElementById('btn-telemetry-options')?.addEventListener('click', () => {
            const points = this.getAggregatedTelemetryPoints(selectedDev, rawNode);
            this.openTelemetryOptionsModal(selectedDev, points);
        });

        // 7. Clipboard Copy Handler for Pane 5
        document.querySelectorAll('#pane-5-container .copyable-badge, #pane-5-container [data-copy]').forEach(el => {
            el.addEventListener('click', (e) => {
                e.stopPropagation();
                const copyText = el.getAttribute('data-copy') || el.innerText.trim().replace(/📋/g, '').trim();
                const label = el.getAttribute('data-copy-label') || 'Telemetry Fix';
                DeviceUserView.copyToClipboard(copyText, label);
            });
        });
    }

    /**
     * Event Binders for Bidirectional Inter-relationship Filtering
     */
    bindEvents() {
        const selectedDev = this.devices.find(d => d.agentId === this.selectedAgentId) || this.devices[0];
        const rawNode = selectedDev ? (selectedDev.raw || {}) : {};

        // Bind Pane 5 dynamic controls
        this.bindPane5Events(selectedDev, rawNode);

        // 0. Temporal Filter Handler (Pane 1)
        document.querySelectorAll('.btn-du-time-filter').forEach(btn => {
            btn.addEventListener('click', () => {
                this.timeWindowFilter = btn.getAttribute('data-time-filter') || 'all';
                this.render(this.devices, this.selectedAgentId, this.safeZones);
            });
        });

        // 0b. Pane 2 ACL Tactile Switches
        document.querySelectorAll('.du-switch-acl').forEach(sw => {
            sw.addEventListener('change', async (e) => {
                const agentId = sw.getAttribute('data-agent-id');
                const flag = sw.getAttribute('data-flag');
                const val = sw.checked;
                try {
                    const res = await (window.fetchApi ? window.fetchApi('/api/device/toggle-acl', {
                        method: 'POST',
                        body: JSON.stringify({ agentId, flag, value: val })
                    }) : fetch('/api/device/toggle-acl', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ agentId, flag, value: val })
                    }).then(r => r.json()));
                    if (res && res.error) throw new Error(res.error || 'ACL update rejected');
                    FeedbackDialog.showSlideNotification({
                        title: 'Access Permission Updated',
                        message: `Flag [${flag}] set to ${val ? 'GRANTED' : 'DENIED'} for [${agentId}]`,
                        type: val ? 'success' : 'warning',
                        timeout: 3500
                    });
                } catch (err) {
                    FeedbackDialog.showSlideNotification({
                        title: 'Permission Update Failed',
                        message: err.message,
                        type: 'danger',
                        timeout: 3500
                    });
                    sw.checked = !val;
                }
            });
        });

        // 1. Roster Click Handler (With Complete State Isolation)
        document.querySelectorAll('.du-device-item').forEach(el => {
            el.addEventListener('click', (e) => {
                const agentId = el.getAttribute('data-agent-id');
                if (agentId && agentId !== this.selectedAgentId) {
                    this.selectedAgentId = agentId;
                    this.historyBreadcrumbs = [];
                    this.availableHistoryDates = [];
                    this.flipperIndex = 0;
                    this.expandedFixIds.clear();
                    this.render(this.devices, agentId, this.safeZones);
                    if (this.streamMode === 'history') {
                        this.loadHistoricalTelemetry(agentId);
                    }
                }
            });
        });

        // 2. Tree Node Click Handler (With Complete State Isolation)
        document.querySelectorAll('.du-tree-node').forEach(el => {
            el.addEventListener('click', (e) => {
                const targetAgentId = el.getAttribute('data-target-agent-id');
                if (targetAgentId && targetAgentId !== this.selectedAgentId) {
                    this.selectedAgentId = targetAgentId;
                    this.historyBreadcrumbs = [];
                    this.availableHistoryDates = [];
                    this.flipperIndex = 0;
                    this.expandedFixIds.clear();
                    this.render(this.devices, targetAgentId, this.safeZones);
                    if (this.streamMode === 'history') {
                        this.loadHistoricalTelemetry(targetAgentId);
                    }
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

        // 4b. Open Duplicate Device Reconciliation View
        document.getElementById('btn-du-dup-devices')?.addEventListener('click', () => {
            if (window.showDupDevice) {
                window.showDupDevice();
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
                            const resData = await (window.fetchApi ? window.fetchApi('/api/relationship/delete', {
                                method: 'POST',
                                body: JSON.stringify({ agentId: sourceId, targetAgentId: targetId })
                            }) : fetch('/api/relationship/delete', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ agentId: sourceId, targetAgentId: targetId })
                            }).then(r => r.json()));
                            if (resData && (resData.status === 'SUCCESS' || resData.status === 'DELETED')) {
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

        // 8. Create Safe Zone Modal Trigger
        document.getElementById('btn-du-create-zone')?.addEventListener('click', () => {
            if (window.MatrixApp && window.MatrixApp.openCreateSafeZone) {
                window.MatrixApp.openCreateSafeZone();
            }
        });

        // 9. Locate Safe Zone on Map Radar
        document.querySelectorAll('.btn-locate-zone').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const lat = parseFloat(btn.getAttribute('data-lat'));
                const lng = parseFloat(btn.getAttribute('data-lng'));
                const name = btn.getAttribute('data-name') || 'Safe Zone';
                if (!isNaN(lat) && !isNaN(lng) && lat !== 0 && lng !== 0) {
                    if (window.showLiveMap) window.showLiveMap();
                    setTimeout(() => {
                        if (window.appInstance?.mapController?.mapView?.map) {
                            window.appInstance.mapController.mapView.map.setView([lat, lng], 16);
                        }
                    }, 300);
                    FeedbackDialog.showSlideNotification({
                        title: "Safe Zone Located",
                        message: `Map centered on perimeter: ${name}`,
                        type: "info",
                        duration: 3000
                    });
                } else {
                    FeedbackDialog.showSlideNotification({
                        title: "No GPS Coordinates",
                        message: `Perimeter [${name}] has no valid center coordinates assigned.`,
                        type: "warning",
                        duration: 3000
                    });
                }
            });
        });

        // 10. Universal Clipboard Copy Handler (Cards, Badges & Tile Views)
        document.querySelectorAll('.copyable-badge, [data-copy]').forEach(el => {
            el.addEventListener('click', (e) => {
                e.stopPropagation();
                const copyText = el.getAttribute('data-copy') || el.innerText.trim().replace(/📋/g, '').trim();
                const label = el.getAttribute('data-copy-label') || 'Data';
                DeviceUserView.copyToClipboard(copyText, label);
            });
        });
    }

    /**
     * Universal Clipboard Helper (Supports async navigator.clipboard and fallback execCommand)
     */
    static copyToClipboard(text, label = 'Data') {
        if (!text) return;
        const str = String(text).trim();
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(str).then(() => {
                FeedbackDialog.showToast(`📋 Copied ${label}: ${str.length > 25 ? str.slice(0, 22) + '...' : str}`, 'info');
            }).catch(() => DeviceUserView.fallbackCopy(str, label));
        } else {
            DeviceUserView.fallbackCopy(str, label);
        }
    }

    static fallbackCopy(text, label = 'Data') {
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.left = '-9999px';
        document.body.appendChild(ta);
        ta.focus();
        ta.select();
        try {
            document.execCommand('copy');
            FeedbackDialog.showToast(`📋 Copied ${label}: ${text.length > 25 ? text.slice(0, 22) + '...' : text}`, 'info');
        } catch (e) {
            console.warn('Fallback copy failed', e);
        }
        document.body.removeChild(ta);
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
                            const resData = await (window.fetchApi ? window.fetchApi('/api/relationship/add', {
                                method: 'POST',
                                body: JSON.stringify({
                                    agentId: selectedDev.agentId,
                                    targetAgentId: targetDev.agentId,
                                    targetName: targetDev.name,
                                    role: chosenRole,
                                    type: branchType
                                })
                            }) : fetch('/api/relationship/add', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({
                                    agentId: selectedDev.agentId,
                                    targetAgentId: targetDev.agentId,
                                    targetName: targetDev.name,
                                    role: chosenRole,
                                    type: branchType
                                })
                            }).then(r => r.json()));

                            if (resData && (resData.status === 'SUCCESS' || resData.status === 'LINKED')) {
                                FeedbackDialog.showToast(`Connected [${selectedDev.name}] <-> [${targetDev.name}] as ${chosenRole}!`, "success");
                                if (window.showDeviceUser) window.showDeviceUser(selectedDev.agentId);
                            } else {
                                FeedbackDialog.showToast(`Binding notice: ${resData?.message || 'Operation processed.'}`, "info");
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

