import { GeoUtils } from '../core/GeoUtils.js';
import { DeviceCardView } from './DeviceCardView.js';
import { DeviceController } from '../controllers/DeviceController.js';
import { FeedbackDialog } from '../components/FeedbackDialog.js';

/**
 * jFindMe Matrix - Enterprise Dashboard View (OOPS Architecture)
 * Renders live proximity window metrics, station coordinates, location-grouped device clusters,
 * and interactive drill-down lists with instant metadata & dossier views.
 * Strictly enforces Date/Time as primary filter with Latest Telemetry on Top.
 */
export class DashboardView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.onSelectDevice = options.onSelectDevice || null;
        this.onShowDossier = options.onShowDossier || null;
        this.onOpenSettings = options.onOpenSettings || null;
        this.onNavigateMap = options.onNavigateMap || null;
        
        this.activeFilter = "all";       // 'all', 'in_radius', 'outside_radius', 'online', 'cluster_X'
        this.timeWindowFilter = "all";   // 'all', 'live', 'today', '24h'
        this.viewMode = "grid";          // 'grid' (3D Flip Cards) or 'table' (Data Sheet)
        this.soundAlerts = true;
        this.showPerimeterCircle = true;
        this.activeClusterKey = null;
        this.searchQuery = "";
        this.devices = [];
        this.settings = null;
    }

    /**
     * Smoothly updates live telemetry from background stream
     */
    updateData(devices = [], settings = {}) {
        // If a search query is currently focused in input, do not interrupt typing
        const activeEl = document.activeElement;
        const isTyping = activeEl && (activeEl.id === 'dash-search-input' || activeEl.tagName === 'INPUT');
        if (isTyping) {
            this.devices = DeviceController.sortByLatestFirst(devices);
            return;
        }
        this.render(devices, settings || this.settings);
    }

    /**
     * Renders the complete interactive Dashboard
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices 
     * @param {import('../models/SettingsModel.js').SettingsModel} settings 
     */
    render(devices = [], settings = {}) {
        // Closed-Loop Deduplication by Canonical Identifier
        const seenIds = new Set();
        const uniqueDevices = [];
        for (const d of (Array.isArray(devices) ? devices : [])) {
            const key = d.agentId || d.deviceId;
            if (key && !seenIds.has(key)) {
                seenIds.add(key);
                uniqueDevices.push(d);
            }
        }
        // Primary Rule: Date/Time with Latest Information on Top
        this.devices = DeviceController.sortByLatestFirst(uniqueDevices);
        this.settings = settings;

        const container = document.getElementById(this.containerId);
        if (!container) return;

        // Auto-resolve reference coordinates: fallback to fleet centroid or default to Pune
        let refLat = settings.referenceLat !== undefined ? settings.referenceLat : 18.5788;
        let refLng = settings.referenceLng !== undefined ? settings.referenceLng : 73.8887;
        const radiusKm = settings.searchRadiusKm || 10;
        const refAddress = settings.referenceAddress || "Central Operator Station";

        // Compute dynamic distance metrics for all devices
        this.devices.forEach(d => {
            if (d.hasValidLocation) {
                d.distanceFromRefKm = GeoUtils.calculateDistanceKm(refLat, refLng, d.latitude, d.longitude);
                d.isWithinSearchRadius = d.distanceFromRefKm <= radiusKm;
            } else {
                d.distanceFromRefKm = Infinity;
                d.isWithinSearchRadius = false;
            }
        });

        // Scope devices by temporal window first
        const timeScopedDevices = DeviceController.filterByTimeWindow(this.devices, this.timeWindowFilter);

        // Metrics calculations
        const inRadiusDevices = timeScopedDevices.filter(d => d.isWithinSearchRadius);
        const outOfRadiusDevices = timeScopedDevices.filter(d => d.hasValidLocation && !d.isWithinSearchRadius);
        const onlineDevices = timeScopedDevices.filter(d => d.isOnline);
        const noGpsDevices = timeScopedDevices.filter(d => !d.hasValidLocation);

        // Location Clusters / Distance Buckets
        const clusters = [
            { key: "c_imm", label: "Immediate Proximity (< 1 km)", icon: "🎯", color: "emerald", list: timeScopedDevices.filter(d => d.hasValidLocation && d.distanceFromRefKm < 1) },
            { key: "c_close", label: "Close Range (1 - 5 km)", icon: "📍", color: "sky", list: timeScopedDevices.filter(d => d.hasValidLocation && d.distanceFromRefKm >= 1 && d.distanceFromRefKm < 5) },
            { key: "c_mid", label: "Mid Range (5 - 10 km)", icon: "🚗", color: "amber", list: timeScopedDevices.filter(d => d.hasValidLocation && d.distanceFromRefKm >= 5 && d.distanceFromRefKm <= radiusKm) },
            { key: "c_far", label: `Outside Radius (> ${radiusKm} km)`, icon: "📡", color: "rose", list: timeScopedDevices.filter(d => d.hasValidLocation && d.distanceFromRefKm > radiusKm) },
            { key: "c_nogps", label: "No Active GPS Signal", icon: "⚠️", color: "slate", list: noGpsDevices }
        ];

        // Resolve currently selected device list to display in the drill-down panel
        let baseList = [];
        let filterTitle = "";
        let filterBadge = "";

        if (this.activeFilter === "in_radius") {
            baseList = inRadiusDevices;
            filterTitle = `Devices Within ${radiusKm} km Window`;
            filterBadge = `🎯 In Radius (${inRadiusDevices.length})`;
        } else if (this.activeFilter === "outside_radius") {
            baseList = outOfRadiusDevices;
            filterTitle = `Devices Outside ${radiusKm} km Window`;
            filterBadge = `📡 Outside Radius (${outOfRadiusDevices.length})`;
        } else if (this.activeFilter === "online") {
            baseList = onlineDevices;
            filterTitle = "Active Online Telemetry Stream";
            filterBadge = `🟢 Online (${onlineDevices.length})`;
        } else if (this.activeFilter === "all") {
            baseList = timeScopedDevices;
            filterTitle = "Fleet Roster (Time-Scoped)";
            filterBadge = `📋 Filtered (${timeScopedDevices.length}/${this.devices.length})`;
        } else if (this.activeFilter.startsWith("cluster_")) {
            const cluster = clusters.find(c => c.key === this.activeClusterKey);
            if (cluster) {
                baseList = cluster.list;
                filterTitle = cluster.label;
                filterBadge = `${cluster.icon} ${cluster.list.length} Devices`;
            }
        }

        // Apply real-time search query filtering
        let displayList = baseList;
        if (this.searchQuery && this.searchQuery.trim().length > 0) {
            const q = this.searchQuery.trim().toLowerCase();
            displayList = baseList.filter(d => {
                return (d.name && d.name.toLowerCase().includes(q)) ||
                       (d.agentId && d.agentId.toLowerCase().includes(q)) ||
                       (d.model && d.model.toLowerCase().includes(q)) ||
                       (d.deviceName && d.deviceName.toLowerCase().includes(q)) ||
                       (d.role && d.role.toLowerCase().includes(q)) ||
                       (d.roleFamily && d.roleFamily.toLowerCase().includes(q)) ||
                       (d.address && d.address.toLowerCase().includes(q));
            });
        }

        // Enforce Latest Information on Top
        displayList = DeviceController.sortByLatestFirst(displayList);

        container.innerHTML = `
            <div class="h-full overflow-y-auto custom-scrollbar p-4 space-y-4 select-text font-sans">
                
                <!-- TOP BAR: Station Context & Radius Configuration -->
                <div class="card-theme p-4 flex flex-wrap items-center justify-between gap-4">
                    <div class="flex items-center gap-3.5">
                        <div class="w-11 h-11 rounded-xl bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-sky-400 text-xl shadow-md shrink-0">
                            📡
                        </div>
                        <div>
                            <div class="flex items-center gap-2">
                                <span class="text-xs font-extrabold uppercase tracking-widest text-sky-400">Reference Location Station</span>
                                <span class="badge-status badge-success">MONITORING ACTIVE</span>
                            </div>
                            <h2 class="text-base font-bold text-primary-theme tracking-tight mt-0.5">${refAddress}</h2>
                            <p class="text-xs font-mono text-secondary-theme">
                                Coordinates: <span class="text-primary-theme font-bold">${refLat.toFixed(5)}° N, ${refLng.toFixed(5)}° E</span> • Active Window: <span class="text-amber-500 font-bold">${radiusKm} km</span>
                            </p>
                        </div>
                    </div>

                    <!-- Quick Actions -->
                    <div class="flex items-center gap-2.5">
                        <button id="btn-dash-settings" class="btn-action-secondary text-xs">
                            <span>⚙️</span>
                            <span>Radius & Station</span>
                        </button>
                        <button id="btn-dash-refresh" class="btn-action-primary text-xs">
                            <span>🔄</span>
                            <span>Refresh Live Fleet</span>
                        </button>
                    </div>
                </div>

                <!-- 4 PRIMARY METRIC CARDS (INTERACTIVE FILTERS) -->
                <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                    <!-- Card 1: Within Search Radius -->
                    <div id="card-filter-in-radius" class="card-theme p-4 cursor-pointer transition-all flex flex-col justify-between space-y-3 group ${this.activeFilter === 'in_radius' ? '!border-sky-500 shadow-md ring-2 ring-sky-500/20' : ''}">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-mono uppercase tracking-wider text-sky-400 font-bold">Within Search Radius</span>
                            <span class="text-base">🎯</span>
                        </div>
                        <div>
                            <div class="flex items-baseline gap-2">
                                <span class="text-3xl font-black text-primary-theme tracking-tight">${inRadiusDevices.length}</span>
                                <span class="text-xs font-semibold text-muted-theme">/ ${this.devices.length} TOTAL</span>
                            </div>
                            <div class="text-xs text-secondary-theme mt-1">Inside active <span class="text-sky-400 font-bold">${radiusKm}km</span> perimeter</div>
                        </div>
                        <div class="text-xs font-bold text-sky-400 group-hover:translate-x-1 transition-transform flex items-center gap-1">
                            <span>Inspect In-Radius Fleet</span>
                            <span>→</span>
                        </div>
                    </div>

                    <!-- Card 2: Outside Search Radius -->
                    <div id="card-filter-outside-radius" class="card-theme p-4 cursor-pointer transition-all flex flex-col justify-between space-y-3 group ${this.activeFilter === 'outside_radius' ? '!border-amber-500 shadow-md ring-2 ring-amber-500/20' : ''}">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-mono uppercase tracking-wider text-amber-500 font-bold">Outside Radius Window</span>
                            <span class="text-base">📡</span>
                        </div>
                        <div>
                            <div class="flex items-baseline gap-2">
                                <span class="text-3xl font-black text-primary-theme tracking-tight">${outOfRadiusDevices.length}</span>
                                <span class="text-xs font-semibold text-muted-theme">NODES</span>
                            </div>
                            <div class="text-xs text-secondary-theme mt-1">Beyond <span class="text-amber-500 font-bold">${radiusKm}km</span> range</div>
                        </div>
                        <div class="text-xs font-bold text-amber-500 group-hover:translate-x-1 transition-transform flex items-center gap-1">
                            <span>Inspect Long-Range Nodes</span>
                            <span>→</span>
                        </div>
                    </div>

                    <!-- Card 3: Active Online Signal -->
                    <div id="card-filter-online" class="card-theme p-4 cursor-pointer transition-all flex flex-col justify-between space-y-3 group ${this.activeFilter === 'online' ? '!border-emerald-500 shadow-md ring-2 ring-emerald-500/20' : ''}">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-mono uppercase tracking-wider text-emerald-500 font-bold">Active Online Pulse</span>
                            <span class="text-base">🟢</span>
                        </div>
                        <div>
                            <div class="flex items-baseline gap-2">
                                <span class="text-3xl font-black text-emerald-500 tracking-tight">${onlineDevices.length}</span>
                                <span class="text-xs font-semibold text-muted-theme">ACTIVE</span>
                            </div>
                            <div class="text-xs text-secondary-theme mt-1">Real-time GPS stream active</div>
                        </div>
                        <div class="text-xs font-bold text-emerald-500 group-hover:translate-x-1 transition-transform flex items-center gap-1">
                            <span>View Active Nodes</span>
                            <span>→</span>
                        </div>
                    </div>

                    <!-- Card 4: Master Total Fleet -->
                    <div id="card-filter-all" class="card-theme p-4 cursor-pointer transition-all flex flex-col justify-between space-y-3 group ${this.activeFilter === 'all' ? '!border-indigo-500 shadow-md ring-2 ring-indigo-500/20' : ''}">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-mono uppercase tracking-wider text-indigo-400 font-bold">Total Master Fleet</span>
                            <span class="text-base">📋</span>
                        </div>
                        <div>
                            <div class="flex items-baseline gap-2">
                                <span class="text-3xl font-black text-primary-theme tracking-tight">${this.devices.length}</span>
                                <span class="text-xs font-semibold text-muted-theme">IDENTITIES</span>
                            </div>
                            <div class="text-xs text-secondary-theme mt-1">All unique registered devices</div>
                        </div>
                        <div class="text-xs font-bold text-indigo-400 group-hover:translate-x-1 transition-transform flex items-center gap-1">
                            <span>View Master Roster</span>
                            <span>→</span>
                        </div>
                    </div>
                </div>

                <!-- MAIN WORKSPACE: LOCATION CLUSTERS + DRILL-DOWN DEVICES LIST & METADATA -->
                <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    
                    <!-- LEFT COLUMN: Spatial Clusters & Location Breakdown -->
                    <div class="space-y-4">
                        <div class="card-theme p-4 space-y-3">
                            <div class="flex items-center justify-between border-b border-subtle-theme pb-2.5">
                                <h3 class="text-xs font-black uppercase tracking-wider text-sky-400 flex items-center gap-1.5">
                                    <span>📍 Location Range Clusters</span>
                                </h3>
                                <span class="text-xs font-mono text-muted-theme">${this.devices.length} Devices</span>
                            </div>

                            <!-- Cluster List -->
                            <div class="space-y-2">
                                ${clusters.map(c => `
                                    <div class="cluster-item cursor-pointer p-2.5 rounded-xl border ${this.activeClusterKey === c.key ? 'border-sky-500 bg-sky-500/10 font-bold' : 'border-subtle-theme bg-card-subtle hover:bg-card-hover'} transition-all flex items-center justify-between"
                                         data-cluster="${c.key}">
                                        <div class="flex items-center gap-2.5 min-w-0">
                                            <span class="text-base shrink-0">${c.icon}</span>
                                            <div class="min-w-0">
                                                <div class="text-xs font-bold text-primary-theme truncate">${c.label}</div>
                                                <div class="text-[10px] text-secondary-theme font-mono">${c.list.length} devices detected</div>
                                            </div>
                                        </div>
                                        <span class="badge-status badge-accent shrink-0 ml-2">
                                            ${c.list.length}
                                        </span>
                                    </div>
                                `).join('')}
                            </div>
                        </div>

                        <!-- Live Map Navigation Card -->
                        <div class="card-theme p-4 space-y-2 text-center border-sky-500/30">
                            <div class="text-3xl">🗺️</div>
                            <h4 class="text-sm font-bold text-primary-theme">Full Tactical Live Map Radar</h4>
                            <p class="text-xs text-secondary-theme leading-relaxed">
                                View interactive real-time movement markers, heading bearings, and safe zone perimeters on the live radar map.
                            </p>
                            <button id="btn-dash-open-map" class="btn-action-primary w-full justify-center mt-2 py-2">
                                Launch Live Map Radar →
                            </button>
                        </div>
                    </div>

                    <!-- RIGHT COLUMN (2 COLS): Drill-down Devices List & Detailed Metadata -->
                    <div class="lg:col-span-2 space-y-4">
                        <div class="card-theme p-5 space-y-4">
                            <!-- Section Title & Search Bar -->
                            <div class="space-y-3 border-b border-subtle-theme pb-3">
                                <div class="flex items-center justify-between flex-wrap gap-2">
                                    <div>
                                        <h3 class="text-sm font-bold text-primary-theme flex items-center gap-2">
                                            <span>${filterTitle}</span>
                                        </h3>
                                        <p class="text-xs text-muted-theme mt-0.5">Click on any device card below to inspect full metadata & dossier</p>
                                    </div>
                                    <div class="flex items-center gap-3">
                                        <!-- SEGMENTED VIEW TOGGLE (LEFT / RIGHT) -->
                                        <div class="toggle-segment-group">
                                            <button id="btn-toggle-view-grid" class="toggle-segment-btn ${this.viewMode === 'grid' ? 'active' : ''}" title="Card Grid with 3D Flip Diagnostics">
                                                <span>🎴 Cards</span>
                                            </button>
                                            <button id="btn-toggle-view-table" class="toggle-segment-btn ${this.viewMode === 'table' ? 'active' : ''}" title="Dense Forensic Data Sheet Table">
                                                <span>📋 Table</span>
                                            </button>
                                        </div>
                                        <span class="badge-status badge-accent text-xs">
                                            ${filterBadge}
                                        </span>
                                    </div>
                                </div>

                                <!-- Device Search Bar & Tactile Switches -->
                                <div class="flex items-center gap-3 flex-wrap">
                                    <div class="relative flex-1 min-w-[240px]">
                                        <input type="text" id="dash-device-search-input" 
                                               placeholder="🔍 Search device by name, ID, hardware, role, address..." 
                                               value="${this.searchQuery}"
                                               class="input-theme w-full pl-9 pr-8 py-2 text-xs font-mono">
                                        <svg class="w-4 h-4 absolute left-3 top-2.5 text-secondary-theme pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                                        </svg>
                                        ${this.searchQuery ? `
                                            <button id="btn-dash-clear-search" class="absolute right-2.5 top-2 text-muted-theme hover:text-primary-theme text-xs font-bold p-1">✕</button>
                                        ` : ''}
                                    </div>

                                    <!-- TACTILE SWITCHES (ON / OFF) -->
                                    <div class="flex items-center gap-3 shrink-0 bg-card-subtle px-3 py-1.5 rounded-xl border border-subtle-theme">
                                        <label class="switch-toggle switch-sm" title="Toggle Acoustic Audio Warnings">
                                            <input type="checkbox" id="dash-switch-sound" ${this.soundAlerts ? 'checked' : ''}>
                                            <span class="switch-slider"></span>
                                            <span class="text-[11px] text-secondary-theme font-bold">🔊 Audio Alert</span>
                                        </label>
                                        <label class="switch-toggle switch-sm" title="Toggle 5km Radar Boundary Circle">
                                            <input type="checkbox" id="dash-switch-perimeter" ${this.showPerimeterCircle ? 'checked' : ''}>
                                            <span class="switch-slider"></span>
                                            <span class="text-[11px] text-secondary-theme font-bold">🎯 Radar Circle</span>
                                        </label>
                                    </div>
                                </div>

                                <!-- TEMPORAL TIME WINDOW FILTER TOOLBAR -->
                                <div class="flex items-center justify-between flex-wrap gap-2 pt-1">
                                    <div class="flex items-center gap-1.5 flex-wrap">
                                        <span class="text-[10px] font-mono font-bold uppercase text-muted-theme flex items-center gap-1">
                                            <span>🕒</span>
                                            <span>Time Window:</span>
                                        </span>
                                        <button class="btn-time-filter text-[11px] font-bold px-2.5 py-1 rounded-lg transition-all ${this.timeWindowFilter === 'all' ? 'btn-action-primary' : 'btn-action-secondary'}" data-time-filter="all">
                                            All (${this.devices.length})
                                        </button>
                                        <button class="btn-time-filter text-[11px] font-bold px-2.5 py-1 rounded-lg transition-all ${this.timeWindowFilter === 'live' ? 'btn-action-primary' : 'btn-action-secondary'}" data-time-filter="live">
                                            ⚡ Live &lt;15m (${DeviceController.filterByTimeWindow(this.devices, 'live').length})
                                        </button>
                                        <button class="btn-time-filter text-[11px] font-bold px-2.5 py-1 rounded-lg transition-all ${this.timeWindowFilter === 'today' ? 'btn-action-primary' : 'btn-action-secondary'}" data-time-filter="today">
                                            📅 Today (${DeviceController.filterByTimeWindow(this.devices, 'today').length})
                                        </button>
                                        <button class="btn-time-filter text-[11px] font-bold px-2.5 py-1 rounded-lg transition-all ${this.timeWindowFilter === '24h' ? 'btn-action-primary' : 'btn-action-secondary'}" data-time-filter="24h">
                                            🕒 24 Hours (${DeviceController.filterByTimeWindow(this.devices, '24h').length})
                                        </button>
                                    </div>
                                    <span class="badge-status badge-accent text-[9px] font-mono">
                                        ⚡ Latest Telemetry On Top ↓
                                    </span>
                                </div>
                            </div>

                            <!-- Display List Content: Grid (3D Flip Cards) vs Data Sheet Table -->
                            ${displayList.length === 0 ? `
                                <div class="py-12 text-center text-muted-theme text-xs italic space-y-1">
                                    <div>No devices found matching your temporal or location criteria.</div>
                                    ${this.searchQuery ? `<div class="text-xs text-sky-400 cursor-pointer underline mt-1" onclick="document.getElementById('dash-device-search-input').value=''; document.getElementById('dash-device-search-input').dispatchEvent(new Event('input'));">Clear search filter</div>` : ''}
                                </div>
                            ` : this.viewMode === 'table' ? `
                                <!-- DENSE FORENSIC DATA SHEET TABLE -->
                                <div class="overflow-x-auto custom-scrollbar border border-subtle-theme rounded-2xl shadow-sm bg-card-subtle">
                                    <table class="data-sheet">
                                        <thead>
                                            <tr>
                                                <th>Status</th>
                                                <th>Identity / Model</th>
                                                <th>Agent ID</th>
                                                <th>Recency</th>
                                                <th>Last Seen</th>
                                                <th>Coordinates</th>
                                                <th>Speed</th>
                                                <th>Battery</th>
                                                <th class="text-right">Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            ${displayList.map(dev => {
                                                const unified = typeof dev.getUnifiedStatus === 'function' ? dev.getUnifiedStatus() : { status: dev.isOnline ? 'LIVE' : 'OFFLINE', badgeClass: dev.isOnline ? 'badge-success' : 'badge-danger', dotClass: dev.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400', label: dev.isOnline ? 'LIVE' : 'OFFLINE' };
                                                return `
                                                    <tr class="hover:bg-card-hover cursor-pointer" data-agent-id="${dev.agentId}">
                                                        <td>
                                                            <span class="${unified.badgeClass} text-[9px] font-bold">
                                                                ${unified.status}
                                                            </span>
                                                        </td>
                                                        <td>
                                                            <div class="flex items-center gap-2">
                                                                <img src="${dev.getAvatarUrl()}" class="w-6 h-6 rounded-full object-cover border border-subtle-theme" alt="${dev.name}">
                                                                <div class="font-bold text-primary-theme text-xs">${dev.name}</div>
                                                            </div>
                                                        </td>
                                                        <td class="font-mono text-sky-400 font-bold text-xs">${dev.agentId}</td>
                                                        <td><span class="${unified.badgeClass} text-[8px] font-bold">${unified.label}</span></td>
                                                        <td class="font-mono text-xs text-secondary-theme">${dev.getFormattedDateTime()}</td>
                                                        <td class="font-mono text-xs">${dev.getFormattedCoords()}</td>
                                                        <td class="font-mono text-xs">${dev.getFormattedSpeed()}</td>
                                                        <td class="font-mono text-xs font-bold ${dev.battery <= 20 ? 'text-red-400' : 'text-emerald-400'}">${dev.battery >= 0 ? `${dev.battery}%` : '--'}</td>
                                                        <td class="text-right">
                                                            <div class="flex items-center justify-end gap-1">
                                                                <button class="btn-inspect-dossier btn-action-secondary text-[10px] py-1 px-2" data-agent-id="${dev.agentId}">📄</button>
                                                                <button class="btn-poke-dash btn-action-secondary text-[10px] py-1 px-2 text-amber-500" data-agent-id="${dev.agentId}">🔊</button>
                                                                <button class="btn-inspect-map btn-action-primary text-[10px] py-1 px-2" data-agent-id="${dev.agentId}">🗺️</button>
                                                            </div>
                                                        </td>
                                                    </tr>
                                                `;
                                            }).join('')}
                                        </tbody>
                                    </table>
                                </div>
                            ` : `
                                <!-- 3D FLIP CARD GRID -->
                                <div class="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                                    ${displayList.map(dev => {
                                        const unified = typeof dev.getUnifiedStatus === 'function' ? dev.getUnifiedStatus() : { status: dev.isOnline ? 'LIVE' : 'OFFLINE', badgeClass: dev.isOnline ? 'badge-success' : 'badge-danger', dotClass: dev.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400', label: dev.isOnline ? 'LIVE' : 'OFFLINE' };
                                        return `
                                        <div class="flip-card-container" id="flip-container-${dev.agentId}">
                                            <div class="flip-card-inner surface-theme hover:border-sky-500/50 rounded-2xl p-3.5 space-y-3 transition-all cursor-pointer group shadow-sm border" id="flip-card-${dev.agentId}" data-agent-id="${dev.agentId}">
                                                <!-- FRONT OF CARD -->
                                                <div class="flip-card-front space-y-3">
                                                    <!-- Top Row: Avatar + Name + Status -->
                                                    <div class="flex items-center justify-between">
                                                        <div class="flex items-center gap-2.5 min-w-0">
                                                            <div class="relative shrink-0">
                                                                <img src="${dev.getAvatarUrl()}" alt="${dev.name}" 
                                                                     class="w-10 h-10 rounded-full object-cover border-2 shadow-sm"
                                                                     style="border-color: ${dev.getThemeColor()};"
                                                                     onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                                                                <span class="absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-surface ${unified.dotClass}"></span>
                                                            </div>
                                                            <div class="min-w-0">
                                                                <h4 class="text-xs font-bold text-primary-theme group-hover:text-sky-400 transition-colors truncate">${dev.name}</h4>
                                                                <div class="text-[10px] text-secondary-theme truncate">${dev.deviceName || dev.model || 'Android'} • <span class="font-mono text-sky-400 font-bold copyable-badge" data-copy="${dev.agentId}" title="Click to copy ID">${dev.agentId}</span></div>
                                                            </div>
                                                        </div>

                                                        <!-- Recency & Online Status Badges -->
                                                        <div class="flex items-center gap-1.5 shrink-0">
                                                            <span class="${unified.badgeClass} text-[9px] font-bold">
                                                                ${unified.label}
                                                            </span>
                                                        </div>
                                                    </div>

                                                    <!-- Distance & Telemetry Row -->
                                                    <div class="flex items-center justify-between text-xs font-mono bg-card-subtle rounded-xl px-2.5 py-1.5 border border-subtle-theme">
                                                        <div class="flex items-center gap-1.5">
                                                            <span class="${dev.isWithinSearchRadius ? 'text-sky-400' : 'text-amber-500'} font-bold">
                                                                ${dev.isWithinSearchRadius ? '🎯' : '📡'} ${dev.getFormattedDistance()}
                                                            </span>
                                                        </div>
                                                        <div class="flex items-center gap-2 text-secondary-theme text-[11px]">
                                                            <span>🔋 ${dev.battery >= 0 ? `${dev.battery}%` : '--'}</span>
                                                            <span>•</span>
                                                            <span>${dev.getFormattedSpeed()}</span>
                                                        </div>
                                                    </div>

                                                    <!-- Location & Timestamp Metadata -->
                                                    <div class="bg-card-subtle rounded-xl p-2 space-y-1 border border-subtle-theme select-text">
                                                        <div class="flex items-center justify-between text-[10px] font-mono text-sky-400 font-bold border-b border-subtle-theme pb-1">
                                                            <span class="flex items-center gap-1">🕒 <span class="copyable-badge font-mono" data-copy="${dev.getFormattedDateTime()}" title="Click to copy time">${dev.getFormattedDateTime()}</span></span>
                                                            <span class="text-secondary-theme font-normal">${dev.getFormattedTimeDelta()}</span>
                                                        </div>
                                                        <div class="text-[11px] text-secondary-theme line-clamp-2 flex items-start gap-1 pt-0.5">
                                                            <span class="text-muted-theme">📍</span>
                                                            <span class="truncate copyable-badge" data-copy="${dev.address || dev.getFormattedCoords()}" title="Click to copy address/coords">${dev.address || 'No street address recorded'}</span>
                                                        </div>
                                                    </div>

                                                    <!-- Action Triggers with FLIPPER Button -->
                                                    <div class="flex items-center gap-1.5 pt-1 border-t border-subtle-theme">
                                                        <button class="btn-inspect-dossier flex-1 py-1.5 rounded-lg bg-sky-500/15 hover:bg-sky-500/25 text-sky-400 border border-sky-500/30 text-xs font-bold transition-all"
                                                                data-agent-id="${dev.agentId}">
                                                            📄 Details
                                                        </button>
                                                        <button class="btn-poke-dash px-2 py-1.5 rounded-lg bg-amber-500/15 hover:bg-amber-500/25 text-amber-500 border border-amber-500/30 text-xs font-bold transition-all"
                                                                data-agent-id="${dev.agentId}" title="Send Telemetry Poke Signal">
                                                            🔊 Poke
                                                        </button>
                                                        <button class="btn-inspect-map px-2 py-1.5 rounded-lg bg-card-subtle hover:bg-card-hover text-primary-theme text-xs font-bold border border-subtle-theme transition-all"
                                                                data-agent-id="${dev.agentId}" title="Focus on Live Map">
                                                            🗺️ Map
                                                        </button>
                                                        <button class="btn-flip-card px-2 py-1.5 rounded-lg bg-indigo-500/15 hover:bg-indigo-500/25 text-indigo-400 border border-indigo-500/30 text-xs font-bold transition-all"
                                                                data-agent-id="${dev.agentId}" title="3D Flip for Hardware Diagnostics">
                                                            🔄 Flip
                                                        </button>
                                                    </div>
                                                </div>

                                                <!-- BACK OF CARD (TIER 1 HARDWARE & TIER 3 SENSOR DIAGNOSTICS) -->
                                                <div class="flip-card-back p-3 bg-surface border border-indigo-500/40 rounded-2xl shadow-xl space-y-2 select-text">
                                                    <div class="flex items-center justify-between border-b border-subtle-theme pb-1.5">
                                                        <div class="flex items-center gap-1.5 min-w-0">
                                                            <span class="text-indigo-400 font-bold text-xs truncate">⚡ HARDWARE SILICON SENTINEL</span>
                                                            <span class="badge-status badge-accent text-[8px] font-mono shrink-0 copyable-badge" data-copy="${dev.agentId}" title="Click to copy">${dev.agentId}</span>
                                                        </div>
                                                        <button class="btn-flip-back text-xs text-muted-theme hover:text-indigo-400 font-bold px-1.5 py-0.5 rounded border border-subtle-theme transition-all shrink-0" data-agent-id="${dev.agentId}">
                                                            ↩️ Front
                                                        </button>
                                                    </div>

                                                    <!-- Tier 1 Silicon Specs -->
                                                    <div class="space-y-1 text-[9.5px] font-mono bg-card-subtle p-2 rounded-xl border border-subtle-theme select-text">
                                                        <div class="flex justify-between py-0.5 border-b border-subtle-theme">
                                                            <span class="text-secondary-theme">Hardware Model:</span>
                                                            <span class="text-primary-theme font-bold truncate">${dev.brand} ${dev.model} (${dev.device})</span>
                                                        </div>
                                                        <div class="flex justify-between py-0.5 border-b border-subtle-theme">
                                                            <span class="text-secondary-theme">Board / Chip:</span>
                                                            <span class="text-primary-theme truncate">${dev.board} / ${dev.hardware}</span>
                                                        </div>
                                                        <div class="flex justify-between py-0.5 border-b border-subtle-theme">
                                                            <span class="text-secondary-theme">IMEI 1 / 2:</span>
                                                            <span class="text-primary-theme truncate copyable-badge" data-copy="${dev.imei1}">${dev.imei1} / ${dev.imei2}</span>
                                                        </div>
                                                        <div class="flex justify-between py-0.5">
                                                            <span class="text-secondary-theme">Silicon SHA:</span>
                                                            <span class="text-amber-400 font-bold truncate text-[8.5px] copyable-badge" data-copy="${dev.siliconHash}" title="Click to copy Silicon SHA">${dev.siliconHash}</span>
                                                        </div>
                                                    </div>

                                                    <!-- Live Sensor Grid (Tier 3) -->
                                                    <div class="grid grid-cols-2 gap-1.5 text-[9.5px] font-mono">
                                                        <div class="bg-card-subtle p-1.5 rounded-lg border border-subtle-theme">
                                                            <div class="text-muted-theme uppercase text-[8px]">Altitude</div>
                                                            <div class="font-bold text-primary-theme">${dev.altitude ? `${dev.altitude}m MSL` : '182m MSL'}</div>
                                                        </div>
                                                        <div class="bg-card-subtle p-1.5 rounded-lg border border-subtle-theme">
                                                            <div class="text-muted-theme uppercase text-[8px]">Heading Bearing</div>
                                                            <div class="font-bold text-primary-theme">${Math.round(dev.bearing || 0)}° North</div>
                                                        </div>
                                                        <div class="bg-card-subtle p-1.5 rounded-lg border border-subtle-theme">
                                                            <div class="text-muted-theme uppercase text-[8px]">GPS Precision</div>
                                                            <div class="font-bold text-emerald-400">±${(dev.accuracy || 3.8).toFixed(1)}m HDOP</div>
                                                        </div>
                                                        <div class="bg-card-subtle p-1.5 rounded-lg border border-subtle-theme">
                                                            <div class="text-muted-theme uppercase text-[8px]">Provider State</div>
                                                            <div class="font-bold ${dev.isMockGps ? 'text-rose-400' : 'text-emerald-400'}">${dev.isMockGps ? '⚠️ MOCK GPS' : '🛡️ GENUINE HW'}</div>
                                                        </div>
                                                    </div>

                                                    <!-- Direct Switch: Live Telemetry Stream -->
                                                    <div class="flex items-center justify-between p-1.5 rounded-xl bg-card-subtle border border-subtle-theme">
                                                        <span class="text-[10.5px] font-bold text-primary-theme">Telemetry Stream</span>
                                                        <label class="switch-toggle switch-sm">
                                                            <input type="checkbox" class="dash-switch-acl" data-agent-id="${dev.agentId}" data-flag="acl_live_tracking" checked>
                                                            <span class="switch-slider"></span>
                                                        </label>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                        `;
                                    }).join('')}
                                </div>
                            `}
                        </div>
                    </div>
                </div>
            </div>
        `;

        this.bindEvents();
    }

    /**
     * Binds interactive click handlers for drill-down cards and modal inspections
     */
    bindEvents() {
        // 1. Metric Cards Filter
        const inCard = document.getElementById('card-filter-in-radius');
        inCard?.addEventListener('click', () => {
            this.activeFilter = "in_radius";
            this.activeClusterKey = null;
            this.render(this.devices, this.settings);
        });

        const outCard = document.getElementById('card-filter-outside-radius');
        outCard?.addEventListener('click', () => {
            this.activeFilter = "outside_radius";
            this.activeClusterKey = null;
            this.render(this.devices, this.settings);
        });

        const onCard = document.getElementById('card-filter-online');
        onCard?.addEventListener('click', () => {
            this.activeFilter = "online";
            this.activeClusterKey = null;
            this.render(this.devices, this.settings);
        });

        const allCard = document.getElementById('card-filter-all');
        allCard?.addEventListener('click', () => {
            this.activeFilter = "all";
            this.activeClusterKey = null;
            this.render(this.devices, this.settings);
        });

        // 1b. Temporal Time Window Filter Buttons
        document.querySelectorAll('.btn-time-filter').forEach(btn => {
            btn.addEventListener('click', () => {
                this.timeWindowFilter = btn.getAttribute('data-time-filter') || 'all';
                this.render(this.devices, this.settings);
            });
        });

        // 1c. Segmented View Toggle (Grid / Table)
        document.getElementById('btn-toggle-view-grid')?.addEventListener('click', () => {
            this.viewMode = 'grid';
            this.render(this.devices, this.settings);
        });
        document.getElementById('btn-toggle-view-table')?.addEventListener('click', () => {
            this.viewMode = 'table';
            this.render(this.devices, this.settings);
        });

        // 1d. Tactile Operational Switches (Audio Alert & Radar Circle)
        document.getElementById('dash-switch-sound')?.addEventListener('change', (e) => {
            this.soundAlerts = e.target.checked;
            FeedbackDialog.showSlideNotification({
                title: 'Acoustic Alert State',
                message: `Audio breach chimes ${this.soundAlerts ? 'ACTIVATED' : 'MUTED'}`,
                type: this.soundAlerts ? 'success' : 'warning',
                timeout: 3000
            });
        });

        document.getElementById('dash-switch-perimeter')?.addEventListener('change', (e) => {
            this.showPerimeterCircle = e.target.checked;
            FeedbackDialog.showSlideNotification({
                title: 'Perimeter Radar Ring',
                message: `5km Station Boundary circle ${this.showPerimeterCircle ? 'VISIBLE' : 'HIDDEN'}`,
                type: 'info',
                timeout: 3000
            });
        });

        // 1e. 3D Card Flipper Triggers
        document.querySelectorAll('.btn-flip-card').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const id = btn.getAttribute('data-agent-id');
                const card = document.getElementById(`flip-card-${id}`);
                card?.classList.add('is-flipped');
            });
        });

        document.querySelectorAll('.btn-flip-back').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const id = btn.getAttribute('data-agent-id');
                const card = document.getElementById(`flip-card-${id}`);
                card?.classList.remove('is-flipped');
            });
        });

        // 1f. Card Diagnostics ACL Quick Switch
        document.querySelectorAll('.dash-switch-acl').forEach(sw => {
            sw.addEventListener('click', (e) => e.stopPropagation());
            sw.addEventListener('change', async (e) => {
                e.stopPropagation();
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
                    }));
                    if (!res || res.error) throw new Error(res?.error || 'Failed to update ACL flag');
                    FeedbackDialog.showSlideNotification({
                        title: 'Telemetry Access Updated',
                        message: `Telemetry stream for [${agentId}] set to ${val ? 'ACTIVE' : 'SUSPENDED'}`,
                        type: val ? 'success' : 'warning',
                        timeout: 3500
                    });
                } catch (err) {
                    FeedbackDialog.showSlideNotification({
                        title: 'Access Update Error',
                        message: err.message,
                        type: 'danger',
                        timeout: 3500
                    });
                    sw.checked = !val;
                }
            });
        });

        // 2. Spatial Cluster Items
        document.querySelectorAll('.cluster-item').forEach(el => {
            el.addEventListener('click', () => {
                const clusterKey = el.getAttribute('data-cluster');
                this.activeFilter = `cluster_${clusterKey}`;
                this.activeClusterKey = clusterKey;
                this.render(this.devices, this.settings);
            });
        });

        // 3. Metadata Dossier Button Triggers
        document.querySelectorAll('.btn-inspect-dossier').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const agentId = btn.getAttribute('data-agent-id');
                if (this.onShowDossier) this.onShowDossier(agentId);
            });
        });

        // 4. Map Jump Button Triggers
        document.querySelectorAll('.btn-inspect-map').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const agentId = btn.getAttribute('data-agent-id');
                if (this.onNavigateMap) this.onNavigateMap(agentId);
            });
        });

        // 4b. Poke Telemetry Button Triggers
        document.querySelectorAll('.btn-poke-dash').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const agentId = btn.getAttribute('data-agent-id');
                const targetDev = this.devices.find(d => d.agentId === agentId);
                const targetName = targetDev ? targetDev.name : agentId;

                if (agentId) {
                    const pokePromise = window.FBBridge?.pokeDevice 
                        ? window.FBBridge.pokeDevice(agentId, 'Dashboard_Console')
                        : (window.fetchApi || fetch)('/api/poke-device', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ agentId, pokedBy: 'Dashboard_Console' })
                        }).then(r => r.json ? r.json() : r);

                    pokePromise
                    .then(res => {
                        FeedbackDialog.showToast(`⚡ Poke ping sent to [${targetName}]!`, "success");
                        if (window.log) window.log(`POKE_SUCCESS: Poke sent to [${agentId}]`, "success");
                    })
                    .catch(err => {
                        FeedbackDialog.showToast(`Poke failed: ${err.message}`, "error");
                        if (window.log) window.log(`POKE_FAILED: ${err.message}`, "error");
                    });
                }
            });
        });

        // 5. Direct Card Click -> Select Device & Focus Map
        document.querySelectorAll('.device-summary-card').forEach(card => {
            card.addEventListener('click', (e) => {
                if (e.target.closest('button')) return;
                const agentId = card.getAttribute('data-agent-id');
                if (this.onNavigateMap) this.onNavigateMap(agentId);
            });
        });

        // 6. Search Bar Real-Time Filter
        const searchInput = document.getElementById('dash-device-search-input');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.searchQuery = e.target.value;
                this.render(this.devices, this.settings);
                const nextInput = document.getElementById('dash-device-search-input');
                if (nextInput) {
                    nextInput.focus();
                    nextInput.setSelectionRange(nextInput.value.length, nextInput.value.length);
                }
            });
        }

        document.getElementById('btn-dash-clear-search')?.addEventListener('click', () => {
            this.searchQuery = "";
            this.render(this.devices, this.settings);
        });

        // 7. Settings Modal Trigger
        document.getElementById('btn-dash-settings')?.addEventListener('click', () => {
            if (this.onOpenSettings) this.onOpenSettings();
        });

        // 8. Refresh Fleet Trigger
        document.getElementById('btn-dash-refresh')?.addEventListener('click', async () => {
            if (window.MatrixApp && window.MatrixApp.refreshNow) {
                await window.MatrixApp.refreshNow();
            }
            if (window.showDashboard) {
                window.showDashboard();
            }
            FeedbackDialog.showToast("Fleet proximity & telemetry refreshed.", "info");
        });

        // 9. Launch Live Map Button
        document.getElementById('btn-dash-open-map')?.addEventListener('click', () => {
            if (window.showLiveMap) window.showLiveMap();
        });

        // 10. Universal Clipboard Copy for Dashboard Cards & Tiles
        document.querySelectorAll('.copyable-badge, [data-copy]').forEach(el => {
            el.addEventListener('click', (e) => {
                e.stopPropagation();
                const copyText = el.getAttribute('data-copy') || el.innerText.trim().replace(/📋/g, '').trim();
                const label = el.getAttribute('data-copy-label') || 'Data';
                if (copyText && navigator.clipboard?.writeText) {
                    navigator.clipboard.writeText(copyText).then(() => {
                        FeedbackDialog.showToast(`📋 Copied ${label}: ${copyText}`, 'info');
                    }).catch(() => {});
                }
            });
        });
    }
}
