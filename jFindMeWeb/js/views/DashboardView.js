import { GeoUtils } from '../core/GeoUtils.js';
import { DeviceCardView } from './DeviceCardView.js';

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
        this.devices = [];
        this.settings = null;
    }

    /**
     * Renders the complete interactive Dashboard
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices 
     * @param {import('../models/SettingsModel.js').SettingsModel} settings 
     */
    render(devices = [], settings = {}) {
        this.devices = devices;
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
        let displayList = [];
        let filterTitle = "";
        let filterBadge = "";

        if (this.activeFilter === "in_radius") {
            displayList = inRadiusDevices;
            filterTitle = `Devices Within ${radiusKm} km Window`;
            filterBadge = `🎯 In Radius (${inRadiusDevices.length})`;
        } else if (this.activeFilter === "outside_radius") {
            displayList = outOfRadiusDevices;
            filterTitle = `Devices Outside ${radiusKm} km Window`;
            filterBadge = `📡 Outside Radius (${outOfRadiusDevices.length})`;
        } else if (this.activeFilter === "online") {
            displayList = onlineDevices;
            filterTitle = "Active Online Telemetry Stream";
            filterBadge = `🟢 Online (${onlineDevices.length})`;
        } else if (this.activeFilter === "all") {
            displayList = this.devices;
            filterTitle = "All Registered Fleet Devices";
            filterBadge = `📋 Master Roster (${this.devices.length})`;
        } else if (this.activeFilter.startsWith("cluster_")) {
            const cluster = clusters.find(c => c.key === this.activeClusterKey);
            if (cluster) {
                displayList = cluster.list;
                filterTitle = cluster.label;
                filterBadge = `${cluster.icon} ${cluster.list.length} Devices`;
            }
        }

        container.innerHTML = `
            <div class="h-full bg-[#05070a] overflow-y-auto custom-scrollbar p-6 space-y-6 text-white select-none">
                
                <!-- TOP BAR: Station Context & Radius Configuration -->
                <div class="bg-gradient-to-r from-slate-900/90 via-slate-900/60 to-slate-950/90 backdrop-blur-xl border border-white/10 rounded-2xl p-4 shadow-xl flex flex-wrap items-center justify-between gap-4">
                    <div class="flex items-center gap-3.5">
                        <div class="w-11 h-11 rounded-xl bg-sky-500/20 border border-sky-500/40 flex items-center justify-center text-sky-400 text-xl shadow-lg">
                            📡
                        </div>
                        <div>
                            <div class="flex items-center gap-2">
                                <span class="text-xs font-black uppercase tracking-widest text-sky-400">Reference Location Station</span>
                                <span class="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 text-[10px] font-mono font-bold border border-emerald-500/30">MONITORING ACTIVE</span>
                            </div>
                            <h2 class="text-base font-bold text-white tracking-tight mt-0.5">${refAddress}</h2>
                            <p class="text-[11px] font-mono text-slate-400">
                                Coordinates: <span class="text-white">${refLat.toFixed(5)}° N, ${refLng.toFixed(5)}° E</span> • Active Radius Window: <span class="text-amber-300 font-bold">${radiusKm} km</span>
                            </p>
                        </div>
                    </div>

                    <!-- Quick Actions -->
                    <div class="flex items-center gap-2.5">
                        <button id="btn-dash-settings" class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 hover:text-white border border-white/10 text-xs font-semibold flex items-center gap-1.5 transition-all shadow-md">
                            <span>⚙️</span>
                            <span>Adjust Window (${radiusKm}km)</span>
                        </button>
                        <button id="btn-dash-refresh" class="px-3 py-1.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold flex items-center gap-1.5 transition-all shadow-lg shadow-sky-600/30">
                            <span>🔄</span>
                            <span>Refresh Live Fleet</span>
                        </button>
                    </div>
                </div>

                <!-- 4 PRIMARY METRIC CARDS (CLICKABLE FOR DRILL-DOWN) -->
                <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                    <!-- Card 1: Within Proximity Window -->
                    <div id="card-filter-in-radius" class="cursor-pointer bg-slate-900/80 hover:bg-slate-850 p-4 rounded-2xl border ${this.activeFilter === 'in_radius' ? 'border-sky-400 bg-sky-950/30 shadow-[0_0_20px_rgba(56,189,248,0.2)]' : 'border-white/10 hover:border-white/20'} transition-all flex flex-col justify-between space-y-3 group">
                        <div class="flex items-center justify-between">
                            <span class="text-[10px] font-mono uppercase tracking-wider text-sky-400 font-bold">Within ${radiusKm}km Window</span>
                            <span class="w-2.5 h-2.5 rounded-full bg-sky-400 animate-ping"></span>
                        </div>
                        <div>
                            <div class="text-3xl font-black text-white tracking-tight">${inRadiusDevices.length} <span class="text-xs font-medium text-slate-400">devices</span></div>
                            <div class="text-[11px] text-slate-400 mt-0.5">Fleet in active safety zone</div>
                        </div>
                        <div class="text-[10px] font-bold text-sky-400 group-hover:translate-x-1 transition-transform flex items-center gap-1">
                            <span>View Location Devices</span>
                            <span>→</span>
                        </div>
                    </div>

                    <!-- Card 2: Outside Proximity Window -->
                    <div id="card-filter-outside-radius" class="cursor-pointer bg-slate-900/80 hover:bg-slate-850 p-4 rounded-2xl border ${this.activeFilter === 'outside_radius' ? 'border-amber-400 bg-amber-950/30 shadow-[0_0_20px_rgba(251,191,36,0.2)]' : 'border-white/10 hover:border-white/20'} transition-all flex flex-col justify-between space-y-3 group">
                        <div class="flex items-center justify-between">
                            <span class="text-[10px] font-mono uppercase tracking-wider text-amber-400 font-bold">Outside ${radiusKm}km Window</span>
                            <span class="text-xs">📡</span>
                        </div>
                        <div>
                            <div class="text-3xl font-black text-white tracking-tight">${outOfRadiusDevices.length} <span class="text-xs font-medium text-slate-400">devices</span></div>
                            <div class="text-[11px] text-slate-400 mt-0.5">Remote / roaming devices</div>
                        </div>
                        <div class="text-[10px] font-bold text-amber-400 group-hover:translate-x-1 transition-transform flex items-center gap-1">
                            <span>View Remote Devices</span>
                            <span>→</span>
                        </div>
                    </div>

                    <!-- Card 3: Live Online Telemetry -->
                    <div id="card-filter-online" class="cursor-pointer bg-slate-900/80 hover:bg-slate-850 p-4 rounded-2xl border ${this.activeFilter === 'online' ? 'border-emerald-400 bg-emerald-950/30 shadow-[0_0_20px_rgba(52,211,153,0.2)]' : 'border-white/10 hover:border-white/20'} transition-all flex flex-col justify-between space-y-3 group">
                        <div class="flex items-center justify-between">
                            <span class="text-[10px] font-mono uppercase tracking-wider text-emerald-400 font-bold">Online Heartbeat</span>
                            <span class="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse"></span>
                        </div>
                        <div>
                            <div class="text-3xl font-black text-white tracking-tight">${onlineDevices.length} <span class="text-xs font-medium text-slate-400">streaming</span></div>
                            <div class="text-[11px] text-slate-400 mt-0.5">Active WebSocket telemetry</div>
                        </div>
                        <div class="text-[10px] font-bold text-emerald-400 group-hover:translate-x-1 transition-transform flex items-center gap-1">
                            <span>View Online Fleet</span>
                            <span>→</span>
                        </div>
                    </div>

                    <!-- Card 4: Master Total Fleet -->
                    <div id="card-filter-all" class="cursor-pointer bg-slate-900/80 hover:bg-slate-850 p-4 rounded-2xl border ${this.activeFilter === 'all' ? 'border-indigo-400 bg-indigo-950/30 shadow-[0_0_20px_rgba(129,140,248,0.2)]' : 'border-white/10 hover:border-white/20'} transition-all flex flex-col justify-between space-y-3 group">
                        <div class="flex items-center justify-between">
                            <span class="text-[10px] font-mono uppercase tracking-wider text-indigo-400 font-bold">Total Master Fleet</span>
                            <span class="text-xs">📋</span>
                        </div>
                        <div>
                            <div class="text-3xl font-black text-white tracking-tight">${this.devices.length} <span class="text-xs font-medium text-slate-400">total nodes</span></div>
                            <div class="text-[11px] text-slate-400 mt-0.5">All registered identities</div>
                        </div>
                        <div class="text-[10px] font-bold text-indigo-400 group-hover:translate-x-1 transition-transform flex items-center gap-1">
                            <span>View Master Roster</span>
                            <span>→</span>
                        </div>
                    </div>
                </div>

                <!-- MAIN WORKSPACE: LOCATION CLUSTERS + DRILL-DOWN DEVICES LIST & METADATA -->
                <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    
                    <!-- LEFT COLUMN: Spatial Clusters & Location Breakdown -->
                    <div class="space-y-4">
                        <div class="bg-slate-900/90 backdrop-blur-xl rounded-2xl border border-white/10 p-4 space-y-3">
                            <div class="flex items-center justify-between border-b border-white/10 pb-2.5">
                                <h3 class="text-xs font-black uppercase tracking-wider text-sky-400 flex items-center gap-1.5">
                                    <span>📍 Location Range Clusters</span>
                                </h3>
                                <span class="text-[10px] font-mono text-slate-400">${this.devices.length} Devices</span>
                            </div>

                            <!-- Cluster List -->
                            <div class="space-y-2">
                                ${clusters.map(c => `
                                    <div class="cluster-item cursor-pointer p-2.5 rounded-xl border ${this.activeClusterKey === c.key ? 'border-sky-400 bg-sky-950/40 text-white' : 'border-white/5 bg-black/30 hover:bg-white/5 text-slate-300'} transition-all flex items-center justify-between"
                                         data-cluster="${c.key}">
                                        <div class="flex items-center gap-2.5">
                                            <span class="text-base">${c.icon}</span>
                                            <div>
                                                <div class="text-xs font-bold leading-tight">${c.label}</div>
                                                <div class="text-[9px] text-slate-400 font-mono">${c.list.length} devices detected</div>
                                            </div>
                                        </div>
                                        <span class="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-white/10 border border-white/10">
                                            ${c.list.length}
                                        </span>
                                    </div>
                                `).join('')}
                            </div>
                        </div>

                        <!-- Live Map Navigation Card -->
                        <div class="bg-gradient-to-br from-sky-950/40 to-slate-900/90 rounded-2xl border border-sky-500/30 p-4 space-y-2 text-center">
                            <div class="text-2xl">🗺️</div>
                            <h4 class="text-xs font-bold text-white">Full Tactical Live Map Radar</h4>
                            <p class="text-[10px] text-slate-400 leading-relaxed">
                                View interactive real-time movement markers, heading bearings, and safe zone perimeters on the live map.
                            </p>
                            <button id="btn-dash-open-map" class="w-full py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold shadow-lg shadow-sky-600/30 transition-all mt-2">
                                Launch Live Map Radar →
                            </button>
                        </div>
                    </div>

                    <!-- RIGHT COLUMN (2 COLS): Drill-down Devices List & Detailed Metadata -->
                    <div class="lg:col-span-2 space-y-4">
                        <div class="bg-slate-900/90 backdrop-blur-xl rounded-2xl border border-white/10 p-5 space-y-4">
                            <!-- Section Title -->
                            <div class="flex items-center justify-between border-b border-white/10 pb-3">
                                <div>
                                    <h3 class="text-sm font-bold text-white flex items-center gap-2">
                                        <span>${filterTitle}</span>
                                    </h3>
                                    <p class="text-[10px] text-slate-400 mt-0.5">Click on any device card below to inspect full metadata & dossier</p>
                                </div>
                                <span class="px-2.5 py-1 rounded-full text-xs font-bold bg-sky-500/20 text-sky-300 border border-sky-500/30 font-mono">
                                    ${filterBadge}
                                </span>
                            </div>

                            <!-- Devices Grid -->
                            ${displayList.length === 0 ? `
                                <div class="py-12 text-center text-slate-500 text-xs italic">
                                    No devices matching the selected location criteria.
                                </div>
                            ` : `
                                <div class="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                                    ${displayList.map(dev => `
                                        <div class="device-summary-card bg-slate-950/80 hover:bg-slate-900 border border-white/10 hover:border-sky-500/50 rounded-2xl p-3.5 space-y-3 transition-all cursor-pointer group shadow-md"
                                             data-agent-id="${dev.agentId}">
                                            <!-- Top Row: Avatar + Name + Status -->
                                            <div class="flex items-center justify-between">
                                                <div class="flex items-center gap-2.5 min-w-0">
                                                    <div class="relative shrink-0">
                                                        <img src="${dev.getAvatarUrl()}" alt="${dev.name}" 
                                                             class="w-10 h-10 rounded-full object-cover border-2 shadow bg-transparent"
                                                             style="border-color: ${dev.getThemeColor()};"
                                                             onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                                                        <span class="absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-slate-900 ${dev.isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-500'}"></span>
                                                    </div>
                                                    <div class="min-w-0">
                                                        <h4 class="text-xs font-bold text-white group-hover:text-sky-300 transition-colors truncate">${dev.name}</h4>
                                                        <div class="text-[10px] text-slate-400 truncate">${dev.deviceName || dev.model || 'Android'} • <span class="font-mono text-sky-400 font-bold">${dev.agentId}</span></div>
                                                    </div>
                                                </div>

                                                <!-- Online / Offline Badge -->
                                                <span class="text-[8px] font-mono px-1.5 py-0.5 rounded font-bold uppercase ${dev.isOnline ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-slate-800 text-slate-400 border border-white/10'}">
                                                    ${dev.isOnline ? 'LIVE' : 'OFFLINE'}
                                                </span>
                                            </div>

                                            <!-- Distance & Telemetry Row -->
                                            <div class="flex items-center justify-between text-[10px] font-mono bg-black/40 rounded-xl px-2.5 py-1.5 border border-white/5">
                                                <div class="flex items-center gap-1.5">
                                                    <span class="${dev.isWithinSearchRadius ? 'text-sky-300' : 'text-amber-300'} font-bold">
                                                        ${dev.isWithinSearchRadius ? '🎯' : '📡'} ${dev.getFormattedDistance()}
                                                    </span>
                                                </div>
                                                <div class="flex items-center gap-2 text-slate-400">
                                                    <span>🔋 ${dev.battery >= 0 ? `${dev.battery}%` : '--'}</span>
                                                    <span>•</span>
                                                    <span>${dev.getFormattedSpeed()}</span>
                                                </div>
                                            </div>

                                            <!-- Street Address Snippet -->
                                            ${dev.address ? `
                                                <div class="text-[9px] text-slate-400 line-clamp-1 flex items-center gap-1">
                                                    <span>📍</span>
                                                    <span class="truncate">${dev.address}</span>
                                                </div>
                                            ` : ''}

                                            <!-- Action Triggers -->
                                            <div class="flex items-center gap-2 pt-1 border-t border-white/5">
                                                <button class="btn-inspect-dossier flex-1 py-1.5 rounded-lg bg-sky-500/20 hover:bg-sky-500/30 text-sky-300 border border-sky-500/30 text-[10px] font-bold transition-all"
                                                        data-agent-id="${dev.agentId}">
                                                    📄 Metadata Details
                                                </button>
                                                <button class="btn-inspect-map px-2.5 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white text-[10px] font-bold transition-all"
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

        // 5. Entire Device Card Click -> Metadata Dossier
        document.querySelectorAll('.device-summary-card').forEach(card => {
            card.addEventListener('click', () => {
                const agentId = card.getAttribute('data-agent-id');
                if (this.onShowDossier) this.onShowDossier(agentId);
            });
        });

        // 6. Settings & Refresh Triggers
        document.getElementById('btn-dash-settings')?.addEventListener('click', () => {
            if (this.onOpenSettings) this.onOpenSettings();
        });

        document.getElementById('btn-dash-refresh')?.addEventListener('click', () => {
            if (window.MatrixApp && window.MatrixApp.refreshNow) {
                window.MatrixApp.refreshNow().then(() => {
                    this.render(this.devices, this.settings);
                });
            }
        });

        document.getElementById('btn-dash-open-map')?.addEventListener('click', () => {
            if (window.showLiveMap) window.showLiveMap();
        });
    }
}
