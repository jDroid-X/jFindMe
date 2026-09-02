import { GeoUtils } from '../core/GeoUtils.js';
import { DeviceCardView } from './DeviceCardView.js';
import { FeedbackDialog } from '../components/FeedbackDialog.js';

/**
 * jFindMe Matrix - Enterprise Dashboard View (OOPS Architecture)
 * Renders live proximity window metrics, station coordinates, location-grouped device clusters,
 * and interactive drill-down lists with instant metadata & dossier views.
 */
export class DashboardView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.onSelectDevice = options.onSelectDevice || null;
        this.onShowDossier = options.onShowDossier || null;
        this.onOpenSettings = options.onOpenSettings || null;
        this.onNavigateMap = options.onNavigateMap || null;
        
        this.activeFilter = "in_radius"; // 'in_radius', 'outside_radius', 'online', 'all', 'cluster_X'
        this.activeClusterKey = null;
        this.searchQuery = "";
        this.devices = [];
        this.settings = null;
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
        this.devices = uniqueDevices;
        this.settings = settings;

        const container = document.getElementById(this.containerId);
        if (!container) return;

        const refLat = parseFloat(settings.referenceLat || 18.5788);
        const refLng = parseFloat(settings.referenceLng || 73.8887);
        const radiusKm = parseFloat(settings.searchRadiusKm || 10.0);
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

        // Metrics calculations
        const inRadiusDevices = this.devices.filter(d => d.isWithinSearchRadius);
        const outOfRadiusDevices = this.devices.filter(d => d.hasValidLocation && !d.isWithinSearchRadius);
        const onlineDevices = this.devices.filter(d => d.isOnline);
        const noGpsDevices = this.devices.filter(d => !d.hasValidLocation);

        // Location Clusters / Distance Buckets
        const clusters = [
            { key: "c_imm", label: "Immediate Proximity (< 1 km)", icon: "🎯", color: "emerald", list: this.devices.filter(d => d.hasValidLocation && d.distanceFromRefKm < 1) },
            { key: "c_close", label: "Close Range (1 - 5 km)", icon: "📍", color: "sky", list: this.devices.filter(d => d.hasValidLocation && d.distanceFromRefKm >= 1 && d.distanceFromRefKm < 5) },
            { key: "c_mid", label: "Mid Range (5 - 10 km)", icon: "🚗", color: "amber", list: this.devices.filter(d => d.hasValidLocation && d.distanceFromRefKm >= 5 && d.distanceFromRefKm <= radiusKm) },
            { key: "c_far", label: `Outside Radius (> ${radiusKm} km)`, icon: "📡", color: "rose", list: this.devices.filter(d => d.hasValidLocation && d.distanceFromRefKm > radiusKm) },
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
            baseList = this.devices;
            filterTitle = "All Registered Fleet Devices";
            filterBadge = `📋 Master Roster (${this.devices.length})`;
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

        container.innerHTML = `
            <div class="h-full overflow-y-auto custom-scrollbar p-6 space-y-6 select-none font-sans">
                
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
                                    <span class="badge-status badge-accent text-xs">
                                        ${filterBadge}
                                    </span>
                                </div>

                                <!-- Device Search Bar -->
                                <div class="relative flex items-center">
                                    <input type="text" id="dash-device-search-input" 
                                           placeholder="🔍 Search device by name, ID, hardware, role, address..." 
                                           value="${this.searchQuery}"
                                           class="input-theme w-full pl-9 pr-8 py-2 text-xs font-mono">
                                    <svg class="w-4 h-4 absolute left-3 text-secondary-theme pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                                    </svg>
                                    ${this.searchQuery ? `
                                        <button id="btn-dash-clear-search" class="absolute right-2.5 text-muted-theme hover:text-primary-theme text-xs font-bold p-1">✕</button>
                                    ` : ''}
                                </div>
                            </div>

                            <!-- Devices Grid -->
                            ${displayList.length === 0 ? `
                                <div class="py-12 text-center text-muted-theme text-xs italic space-y-1">
                                    <div>No devices found matching your search or location filter.</div>
                                    ${this.searchQuery ? `<div class="text-xs text-sky-400 cursor-pointer underline mt-1" onclick="document.getElementById('dash-device-search-input').value=''; document.getElementById('dash-device-search-input').dispatchEvent(new Event('input'));">Clear search filter</div>` : ''}
                                </div>
                            ` : `
                                <div class="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                                    ${displayList.map(dev => `
                                        <div class="device-summary-card surface-theme hover:border-sky-500/50 rounded-2xl p-3.5 space-y-3 transition-all cursor-pointer group shadow-sm border"
                                             data-agent-id="${dev.agentId}">
                                            <!-- Top Row: Avatar + Name + Status -->
                                            <div class="flex items-center justify-between">
                                                <div class="flex items-center gap-2.5 min-w-0">
                                                    <div class="relative shrink-0">
                                                        <img src="${dev.getAvatarUrl()}" alt="${dev.name}" 
                                                             class="w-10 h-10 rounded-full object-cover border-2 shadow-sm"
                                                             style="border-color: ${dev.getThemeColor()};"
                                                             onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                                                        <span class="absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-surface ${dev.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400'}"></span>
                                                    </div>
                                                    <div class="min-w-0">
                                                        <h4 class="text-xs font-bold text-primary-theme group-hover:text-sky-400 transition-colors truncate">${dev.name}</h4>
                                                        <div class="text-[10px] text-secondary-theme truncate">${dev.deviceName || dev.model || 'Android'} • <span class="font-mono text-sky-400 font-bold">${dev.agentId}</span></div>
                                                    </div>
                                                </div>

                                                <!-- Online / Offline Badge -->
                                                <span class="badge-status ${dev.isOnline ? 'badge-success' : 'badge-danger'}">
                                                    ${dev.isOnline ? 'LIVE' : 'OFFLINE'}
                                                </span>
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
                                            <div class="bg-card-subtle rounded-xl p-2 space-y-1 border border-subtle-theme">
                                                <div class="flex items-center justify-between text-[10px] font-mono text-sky-400 font-bold border-b border-subtle-theme pb-1">
                                                    <span class="flex items-center gap-1">🕒 <span>${dev.getFormattedDateTime()}</span></span>
                                                    <span class="text-secondary-theme font-normal">${dev.getFormattedTimeDelta()}</span>
                                                </div>
                                                <div class="text-[11px] text-secondary-theme line-clamp-2 flex items-start gap-1 pt-0.5">
                                                    <span class="text-muted-theme">📍</span>
                                                    <span class="truncate">${dev.address || 'No street address recorded'}</span>
                                                </div>
                                            </div>

                                            <!-- Action Triggers -->
                                            <div class="flex items-center gap-2 pt-1 border-t border-subtle-theme">
                                                <button class="btn-inspect-dossier flex-1 py-1.5 rounded-lg bg-sky-500/15 hover:bg-sky-500/25 text-sky-400 border border-sky-500/30 text-xs font-bold transition-all"
                                                        data-agent-id="${dev.agentId}">
                                                    📄 Details
                                                </button>
                                                <button class="btn-poke-dash px-2.5 py-1.5 rounded-lg bg-amber-500/15 hover:bg-amber-500/25 text-amber-500 border border-amber-500/30 text-xs font-bold transition-all"
                                                        data-agent-id="${dev.agentId}" title="Send Telemetry Poke Signal">
                                                    🔊 Poke
                                                </button>
                                                <button class="btn-inspect-map px-2.5 py-1.5 rounded-lg bg-card-subtle hover:bg-card-hover text-primary-theme text-xs font-bold border border-subtle-theme transition-all"
                                                        data-agent-id="${dev.agentId}" title="Focus on Live Map">
                                                    🗺️ Map
                                                </button>
                                            </div>
                                        </div>
                                    `).join('')}
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
                    fetch('/api/poke-device', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ agentId, pokedBy: 'Dashboard_Console' })
                    })
                    .then(r => r.json())
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
    }
}
