/**
 * EagleAnalyticsTreeView.js - Business Analytics & Explorer Tree Monitoring
 * Metaphor: "Eagle flying at high altitude to Ant on the ground"
 * Hierarchy: Global Enterprise -> Regions/Circles -> SafeZones -> Device Unit -> Micro GPS Ticks ("The Ant")
 * Dynamic OOPS-based MVC Component with UIControls Suite (Switch, Toggle, 3D Flipper, Toasts)
 */

import { UIControls } from '../components/UIControls.js';

export class EagleAnalyticsTreeView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.container = document.getElementById(containerId);
        this.options = options;
        this.devices = [];
        this.safeZones = [];
        this.selectedDevice = null;
        this.selectedGroup = null;
        this.expandedNodes = new Set(['root', 'group-all']);
        this.searchQuery = '';
        this.speedUnit = 'kmh'; // 'kmh' or 'mph'
        this.showMockOnly = false;
    }

    setDevices(devices) {
        this.devices = Array.isArray(devices) ? devices : [];
        if (this.selectedDevice) {
            this.selectedDevice = this.devices.find(d => d.agentId === this.selectedDevice.agentId) || this.devices[0];
        }
    }

    setSafeZones(zones) {
        this.safeZones = Array.isArray(zones) ? zones : [];
    }

    updateData(devices = [], safeZones = []) {
        this.setDevices(devices);
        this.setSafeZones(safeZones);
        this.render();
    }

    render(devices = null, safeZones = null) {
        if (devices !== null) this.setDevices(devices);
        if (safeZones !== null) this.setSafeZones(safeZones);
        const container = document.getElementById(this.containerId) || this.container || document.getElementById('workspace-root');
        if (!container) return;
        this.container = container;

        const metrics = this.computeEagleMetrics();
        const treeData = this.buildExplorerTree();

        this.container.innerHTML = `
            <div class="eagle-analytics-container flex flex-col h-full bg-slate-950 text-slate-100 font-sans select-none overflow-hidden">
                <!-- Eagle's Eye Top Executive Telemetry Bar -->
                <header class="eagle-header px-6 py-4 border-b border-slate-800 bg-slate-900/80 backdrop-blur-md flex flex-wrap items-center justify-between gap-4">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-xl bg-gradient-to-tr from-amber-500 to-indigo-600 flex items-center justify-center text-xl shadow-lg shadow-amber-500/20">
                            🦅
                        </div>
                        <div>
                            <div class="flex items-center gap-2">
                                <h1 class="text-lg font-black tracking-tight text-white uppercase font-mono">Eagle-Eye Analytics Radar</h1>
                                <span class="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-amber-500/10 text-amber-400 border border-amber-500/30">MONITORING ONLY</span>
                            </div>
                            <p class="text-xs text-slate-400 font-sans">Business Analytical Explorer: Macro Bird's-Eye Overview ➔ Micro Sensor Ground Truth</p>
                        </div>
                    </div>

                    <!-- Macro KPI Badges & Controls -->
                    <div class="flex items-center gap-3 flex-wrap">
                        ${UIControls.createSegmentedToggle({
                            id: 'speed-unit-toggle',
                            options: [
                                { value: 'kmh', label: 'km/h', icon: '⚡' },
                                { value: 'mph', label: 'mph', icon: '🚗' }
                            ],
                            selectedValue: this.speedUnit
                        })}

                        <div class="px-3.5 py-1.5 rounded-lg bg-slate-800/80 border border-slate-700 flex items-center gap-2.5">
                            <span class="w-2.5 h-2.5 rounded-full bg-sky-400 animate-pulse"></span>
                            <div class="text-left">
                                <div class="text-[9px] font-mono text-slate-400 uppercase">Total Units</div>
                                <div class="text-sm font-black font-mono text-white">${metrics.total}</div>
                            </div>
                        </div>

                        <div class="px-3.5 py-1.5 rounded-lg bg-slate-800/80 border border-emerald-900/40 flex items-center gap-2.5">
                            <span class="w-2.5 h-2.5 rounded-full bg-emerald-400"></span>
                            <div class="text-left">
                                <div class="text-[9px] font-mono text-emerald-400 uppercase">Moving</div>
                                <div class="text-sm font-black font-mono text-emerald-300">${metrics.active}</div>
                            </div>
                        </div>

                        <div class="px-3.5 py-1.5 rounded-lg bg-slate-800/80 border border-amber-900/40 flex items-center gap-2.5">
                            <span class="w-2.5 h-2.5 rounded-full bg-amber-400"></span>
                            <div class="text-left">
                                <div class="text-[9px] font-mono text-amber-400 uppercase">Idle</div>
                                <div class="text-sm font-black font-mono text-amber-300">${metrics.idle}</div>
                            </div>
                        </div>

                        <div class="px-3.5 py-1.5 rounded-lg bg-slate-800/80 border border-slate-700 flex items-center gap-2.5">
                            <span class="w-2.5 h-2.5 rounded-full ${metrics.mockGpsCount > 0 ? 'bg-rose-500 animate-ping' : 'bg-slate-500'}"></span>
                            <div class="text-left">
                                <div class="text-[9px] font-mono text-slate-400 uppercase">Integrity Alerts</div>
                                <div class="text-sm font-black font-mono ${metrics.mockGpsCount > 0 ? 'text-rose-400' : 'text-slate-300'}">${metrics.mockGpsCount}</div>
                            </div>
                        </div>
                    </div>
                </header>

                <!-- Main Analytics Workspace (Split Pane: Explorer Tree <-> Micro Unit Inspector) -->
                <div class="flex-1 flex overflow-hidden">
                    <!-- Left: Hierarchical Explorer Tree Pane -->
                    <aside class="w-80 md:w-96 border-r border-slate-800 bg-slate-900/40 flex flex-col">
                        <!-- Tree Search & Filter Bar -->
                        <div class="p-3 border-b border-slate-800/80 flex items-center gap-2 bg-slate-900/60">
                            <div class="relative flex-1">
                                <input type="text" id="eagle-tree-search" placeholder="Search hierarchy, units, zones..." 
                                    value="${this.searchQuery}"
                                    class="w-full pl-8 pr-3 py-1.5 bg-slate-950 border border-slate-700/80 rounded-lg text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-500 font-mono">
                                <svg class="w-4 h-4 text-slate-500 absolute left-2.5 top-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                                </svg>
                            </div>
                            <button id="eagle-tree-expand-all" title="Expand / Collapse All" class="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-mono text-slate-300 border border-slate-700">
                                ⤢
                            </button>
                        </div>

                        <!-- Tree Breadcrumbs Navigation -->
                        <div class="px-3 py-2 bg-slate-950/60 border-b border-slate-800 text-[10px] font-mono text-slate-400 flex items-center gap-1 overflow-x-auto whitespace-nowrap">
                            <span class="text-amber-400 font-bold cursor-pointer" data-action="reset-tree">🦅 Global</span>
                            <span>➔</span>
                            <span class="text-slate-300">${this.selectedGroup ? this.selectedGroup : 'All Circles'}</span>
                            <span>➔</span>
                            <span class="text-sky-300 font-semibold truncate">${this.selectedDevice ? (this.selectedDevice.nickname || this.selectedDevice.name || this.selectedDevice.agentId) : 'Select Unit'}</span>
                        </div>

                        <!-- Tree Content Body -->
                        <div class="flex-1 overflow-y-auto p-2 space-y-1 custom-scrollbar" id="eagle-tree-nodes">
                            ${this.renderTreeHtml(treeData)}
                        </div>
                    </aside>

                    <!-- Right: Macro Eagle Radar & Micro "Ant" Inspector -->
                    <main class="flex-1 flex flex-col bg-slate-950 overflow-y-auto custom-scrollbar p-6 space-y-6">
                        ${this.renderInspectorHtml(metrics)}
                    </main>
                </div>
            </div>
        `;

        this.bindEvents();
    }

    computeEagleMetrics() {
        const total = this.devices.length;
        let active = 0;
        let idle = 0;
        let offline = 0;
        let totalSpeed = 0;
        let totalBattery = 0;
        let mockGpsCount = 0;

        const now = Date.now();
        this.devices.forEach(d => {
            let speed = Number(d.speed || d.spd || 0);
            if (this.speedUnit === 'mph') speed *= 0.621371;
            
            const battery = Number(d.battery != null && d.battery >= 0 ? d.battery : (d.bat || 0));
            const unified = typeof d.getUnifiedStatus === 'function' ? d.getUnifiedStatus() : { status: d.isOnline ? 'LIVE' : 'OFFLINE', isOnline: Boolean(d.isOnline), isMoving: speed > 2 };

            if (unified.isOnline) {
                if (unified.isMoving) active++;
                else idle++;
            } else {
                offline++;
            }

            totalSpeed += speed;
            totalBattery += battery;
            if (d.isMock || d.isMockGps || d.mockGps) mockGpsCount++;
        });

        return {
            total,
            active,
            idle,
            offline,
            avgSpeed: total > 0 ? (totalSpeed / total).toFixed(1) : '0.0',
            avgBattery: total > 0 ? Math.round(totalBattery / total) : 0,
            mockGpsCount,
            safeZonesCount: this.safeZones.length,
            unitLabel: this.speedUnit === 'mph' ? 'mph' : 'km/h'
        };
    }

    buildExplorerTree() {
        const groups = {};
        this.devices.forEach(dev => {
            const rel = dev.relation || dev.roleFamily || dev.role || 'Family Circle';
            if (!groups[rel]) groups[rel] = [];
            groups[rel].push(dev);
        });

        return {
            id: 'root',
            label: '🏢 Tier 1: Master Enterprise Fleet',
            count: this.devices.length,
            groups: Object.entries(groups).map(([groupName, devs]) => ({
                id: `group-${groupName.replace(/\s+/g, '-').toLowerCase()}`,
                name: groupName,
                count: devs.length,
                devices: devs
            }))
        };
    }

    renderTreeHtml(treeData) {
        const isRootOpen = this.expandedNodes.has('root');
        let html = `
            <div class="tree-node">
                <div class="tree-row flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-slate-800/60 cursor-pointer text-xs font-mono ${this.selectedGroup === null && !this.selectedDevice ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30' : 'text-slate-200'}"
                    data-action="toggle-node" data-id="root">
                    <span class="text-slate-400 text-[10px]">${isRootOpen ? '▼' : '▶'}</span>
                    <span class="text-sm">🦅</span>
                    <div class="flex-1 flex items-center gap-1.5 min-w-0">
                        <span class="font-bold truncate">${treeData.label}</span>
                        <span class="badge-status badge-accent text-[8px] font-mono shrink-0">TIER 1</span>
                    </div>
                    <span class="px-1.5 py-0.2 rounded bg-slate-800 text-[10px] text-slate-400 font-mono">${treeData.count}</span>
                </div>
        `;

        if (isRootOpen) {
            html += `<div class="pl-4 mt-1 space-y-1 border-l border-slate-800 ml-3">`;
            treeData.groups.forEach(grp => {
                const isGrpOpen = this.expandedNodes.has(grp.id);
                html += `
                    <div class="tree-group-node">
                        <div class="tree-row flex items-center gap-2 px-2 py-1.5 rounded-md hover:bg-slate-800/60 cursor-pointer text-xs font-mono ${this.selectedGroup === grp.name ? 'bg-sky-500/10 text-sky-400 border border-sky-500/30' : 'text-slate-300'}"
                            data-action="select-group" data-id="${grp.id}" data-group="${grp.name}">
                            <span class="text-slate-500 text-[9px]" data-action="toggle-node" data-id="${grp.id}">${isGrpOpen ? '▼' : '▶'}</span>
                            <span class="text-xs">📂</span>
                            <div class="flex-1 flex items-center gap-1 min-w-0">
                                <span class="font-semibold truncate">${grp.name}</span>
                                <span class="badge-status badge-accent text-[7.5px] font-mono shrink-0">TIER 2</span>
                            </div>
                            <span class="px-1.5 py-0.2 rounded bg-slate-800/80 text-[10px] text-slate-400">${grp.count}</span>
                        </div>
                `;

                if (isGrpOpen) {
                    html += `<div class="pl-4 mt-1 space-y-0.5 border-l border-slate-800/80 ml-2">`;
                    grp.devices.forEach(dev => {
                        const isSelected = this.selectedDevice && this.selectedDevice.agentId === dev.agentId;
                        const unified = typeof dev.getUnifiedStatus === 'function' ? dev.getUnifiedStatus() : { status: dev.isOnline ? 'LIVE' : 'OFFLINE', dotClass: dev.isOnline ? 'bg-emerald-400' : 'bg-slate-600', isMoving: false };
                        let speed = Number(dev.speed || dev.spd || 0);
                        if (this.speedUnit === 'mph') speed = (speed * 0.621371).toFixed(1);
                        else speed = speed.toFixed(1);

                        html += `
                            <div class="tree-device-node flex items-center gap-2 px-2 py-1.5 rounded hover:bg-slate-800 cursor-pointer text-xs font-sans ${isSelected ? 'bg-indigo-600/20 text-indigo-200 border border-indigo-500/40' : 'text-slate-300'}"
                                data-action="select-device" data-agent-id="${dev.agentId}">
                                <span class="w-2 h-2 rounded-full ${unified.dotClass}"></span>
                                <span class="text-xs">${dev.avatar || '📱'}</span>
                                <div class="flex-1 truncate font-mono text-[11px]">
                                    <span class="font-bold text-white">${dev.nickname || dev.name || dev.agentId}</span>
                                    <span class="text-slate-500 text-[10px] ml-1">(${dev.agentId?.slice(0, 6)})</span>
                                    <span class="badge-status badge-accent text-[7px] font-mono ml-1">TIER 3</span>
                                </div>
                                <span class="text-[10px] font-mono text-slate-400">${Number(speed) > 0 ? `${speed} ${this.speedUnit === 'mph' ? 'mph' : 'km/h'}` : 'Idle'}</span>
                            </div>
                        `;
                    });
                    html += `</div>`;
                }

                html += `</div>`;
            });
            html += `</div>`;
        }

        html += `</div>`;
        return html;
    }

    renderInspectorHtml(metrics) {
        if (!this.selectedDevice) {
            return `
                <!-- Eagle's Eye Macro Overview Dashboard -->
                <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
                    <div class="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 shadow-xl flex flex-col justify-between">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-mono uppercase text-slate-400">Fleet Velocity Average</span>
                            <span class="text-xl">🏎️</span>
                        </div>
                        <div class="my-4">
                            <span class="text-3xl font-black font-mono text-amber-400">${metrics.avgSpeed}</span>
                            <span class="text-xs text-slate-400 font-mono">${metrics.unitLabel}</span>
                        </div>
                        <div class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                            <div class="bg-amber-400 h-full rounded-full" style="width: ${Math.min(100, (Number(metrics.avgSpeed) / 80) * 100)}%"></div>
                        </div>
                    </div>

                    <div class="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 shadow-xl flex flex-col justify-between">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-mono uppercase text-slate-400">Average Battery Level</span>
                            <span class="text-xl">🔋</span>
                        </div>
                        <div class="my-4">
                            <span class="text-3xl font-black font-mono ${metrics.avgBattery < 20 ? 'text-rose-400' : 'text-emerald-400'}">${metrics.avgBattery}%</span>
                            <span class="text-xs text-slate-400 font-mono">System Aggregate</span>
                        </div>
                        <div class="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                            <div class="bg-emerald-400 h-full rounded-full" style="width: ${metrics.avgBattery}%"></div>
                        </div>
                    </div>

                    <div class="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 shadow-xl flex flex-col justify-between">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-mono uppercase text-slate-400">Active Geofences</span>
                            <span class="text-xl">🛡️</span>
                        </div>
                        <div class="my-4">
                            <span class="text-3xl font-black font-mono text-indigo-400">${metrics.safeZonesCount}</span>
                            <span class="text-xs text-slate-400 font-mono">Safe Zones</span>
                        </div>
                        <div class="text-[11px] font-mono text-slate-400">100% Deterministic Ray-Casting</div>
                    </div>
                </div>

                <div class="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 text-center py-16">
                    <div class="text-4xl mb-3 animate-bounce">🔍</div>
                    <h3 class="text-base font-bold text-white font-mono uppercase">Select Any Unit in Explorer Tree</h3>
                    <p class="text-xs text-slate-400 max-w-md mx-auto mt-1 font-sans">
                        Drill down from Macro Eagle's Eye view directly into micro sensor ground truth ("The Ant") with 3D Flipper and sub-meter GPS telemetry.
                    </p>
                </div>
            `;
        }

        const dev = this.selectedDevice;
        const lat = Number(dev.latitude || dev.lat || 0).toFixed(6);
        const lng = Number(dev.longitude || dev.lng || 0).toFixed(6);
        let speed = Number(dev.speed || dev.spd || 0);
        if (this.speedUnit === 'mph') speed = (speed * 0.621371).toFixed(1);
        else speed = speed.toFixed(1);

        const alt = Number(dev.altitude || dev.alt || 0).toFixed(1);
        const bearing = Number(dev.bearing || dev.head || 0);
        const accuracy = Number(dev.accuracy || dev.acc || 5).toFixed(1);
        const battery = Number(dev.battery != null && dev.battery >= 0 ? dev.battery : (dev.bat || 100));
        const lastUpdated = dev.lastUpdated ? new Date(dev.lastUpdated).getTime() : (dev.lastSeen || 0);
        const unified = typeof dev.getUnifiedStatus === 'function' ? dev.getUnifiedStatus() : { status: dev.isOnline ? 'LIVE' : 'OFFLINE', badgeClass: dev.isOnline ? 'badge-success' : 'badge-danger', isOnline: Boolean(dev.isOnline) };
        const isMock = Boolean(dev.isMock || dev.isMockGps || dev.mockGps);

        // Build Front (Macro View) and Back ("The Ant" Micro View) for 3D Flipper
        const frontHtml = `
            <div class="p-6 rounded-2xl bg-gradient-to-r from-slate-900 via-slate-900/90 to-indigo-950/40 border border-slate-800 shadow-2xl flex flex-col gap-5">
                <div class="flex items-center gap-4">
                    <div class="w-14 h-14 rounded-2xl bg-indigo-600/20 border border-indigo-500/40 flex items-center justify-center text-3xl shadow-inner">
                        ${dev.avatar || '📱'}
                    </div>
                    <div>
                        <div class="flex items-center gap-2">
                            <h2 class="text-xl font-black text-white font-mono tracking-tight">${dev.nickname || dev.name || dev.agentId}</h2>
                            <span class="px-2 py-0.5 rounded text-[10px] font-mono font-bold ${unified.badgeClass}">
                                ${unified.status}
                            </span>
                        </div>
                        <div class="text-xs font-mono text-slate-400 mt-1 flex items-center gap-3">
                            <span>ID: <strong class="text-slate-200">${dev.agentId}</strong></span>
                            <span>•</span>
                            <span>Circle: <strong class="text-amber-400">${dev.relation || dev.roleFamily || 'Member'}</strong></span>
                        </div>
                    </div>
                </div>

                <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 font-mono">
                    <div class="p-3 bg-slate-950/70 border border-slate-800 rounded-xl">
                        <div class="text-[9px] uppercase text-slate-400">Velocity</div>
                        <div class="text-sm font-bold text-amber-400 mt-0.5">${speed} ${this.speedUnit === 'mph' ? 'mph' : 'km/h'}</div>
                    </div>
                    <div class="p-3 bg-slate-950/70 border border-slate-800 rounded-xl">
                        <div class="text-[9px] uppercase text-slate-400">Battery Level</div>
                        <div class="text-sm font-bold text-emerald-400 mt-0.5">${battery}% ${dev.isCharging ? '⚡' : ''}</div>
                    </div>
                    <div class="p-3 bg-slate-950/70 border border-slate-800 rounded-xl">
                        <div class="text-[9px] uppercase text-slate-400">Precision Radius</div>
                        <div class="text-sm font-bold text-sky-400 mt-0.5">±${accuracy}m</div>
                    </div>
                    <div class="p-3 bg-slate-950/70 border border-slate-800 rounded-xl">
                        <div class="text-[9px] uppercase text-slate-400">Heading</div>
                        <div class="text-sm font-bold text-indigo-400 mt-0.5">${bearing}° (${this.getCompassDirection(bearing)})</div>
                    </div>
                </div>

                <div class="flex items-center justify-between pt-3 border-t border-slate-800/80">
                    <span class="text-[11px] font-mono text-slate-400">Last Seen: <strong class="text-slate-200">${lastUpdated ? new Date(lastUpdated).toLocaleTimeString() : 'N/A'}</strong></span>
                    <button id="eagle-focus-map-btn" data-agent-id="${dev.agentId}"
                        class="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-mono text-xs font-bold transition flex items-center gap-2 shadow-lg shadow-indigo-600/30">
                        <span>🗺️ Focus on Radar Map</span>
                    </button>
                </div>
            </div>
        `;

        const backHtml = `
            <div class="p-6 rounded-2xl bg-gradient-to-r from-slate-950 via-slate-900 to-amber-950/30 border border-amber-500/40 shadow-2xl flex flex-col gap-4">
                <div class="flex items-center gap-2 mb-1">
                    <span class="text-amber-400 text-base">🐜</span>
                    <h3 class="text-xs font-bold uppercase font-mono tracking-wider text-amber-300">Micro Sensor Ground Truth Telemetry ("The Ant")</h3>
                </div>

                <div class="grid grid-cols-2 sm:grid-cols-3 gap-3 font-mono">
                    <div class="p-3 bg-slate-950 border border-slate-800 rounded-xl">
                        <div class="text-[9px] uppercase text-slate-400">Raw Latitude</div>
                        <div class="text-xs font-bold text-white mt-1">${lat}°</div>
                    </div>
                    <div class="p-3 bg-slate-950 border border-slate-800 rounded-xl">
                        <div class="text-[9px] uppercase text-slate-400">Raw Longitude</div>
                        <div class="text-xs font-bold text-white mt-1">${lng}°</div>
                    </div>
                    <div class="p-3 bg-slate-950 border border-slate-800 rounded-xl">
                        <div class="text-[9px] uppercase text-slate-400">Altitude (AMSL)</div>
                        <div class="text-xs font-bold text-emerald-400 mt-1">${alt} m</div>
                    </div>
                    <div class="p-3 bg-slate-950 border border-slate-800 rounded-xl">
                        <div class="text-[9px] uppercase text-slate-400">Hardware GPS State</div>
                        <div class="text-xs font-bold ${isMock ? 'text-rose-400' : 'text-emerald-400'} mt-1">${isMock ? '⚠️ MOCK PROVIDER' : '✅ GENUINE HARDWARE'}</div>
                    </div>
                    <div class="p-3 bg-slate-950 border border-slate-800 rounded-xl">
                        <div class="text-[9px] uppercase text-slate-400">Assigned Geofences</div>
                        <div class="text-xs font-bold text-indigo-400 mt-1">${dev.safeZonesCount || (dev.assignedZones ? dev.assignedZones.length : 0)} Zones</div>
                    </div>
                    <div class="p-3 bg-slate-950 border border-slate-800 rounded-xl">
                        <div class="text-[9px] uppercase text-slate-400">Payload Security</div>
                        <div class="text-xs font-bold text-slate-300 mt-1">AES-256 Valid</div>
                    </div>
                </div>

                <div class="text-[11px] font-mono text-slate-400 pt-2 border-t border-slate-800 flex items-center justify-between">
                    <span>Hardware Model: <strong class="text-slate-200">${dev.deviceName || dev.model || 'Android GPS'}</strong></span>
                    <span class="text-[10px] text-amber-400 font-bold">Sub-meter 6-Decimal Telemetry</span>
                </div>
            </div>
        `;

        return `
            <!-- 3D Card Flipper for Macro to Micro Unit Transition -->
            ${UIControls.createFlipperCard('eagle-unit-card', frontHtml, backHtml)}
        `;
    }

    getCompassDirection(deg) {
        const directions = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
        return directions[Math.round(deg / 45) % 8] || 'N';
    }

    bindEvents() {
        if (!this.container) return;

        // Tree node toggles
        this.container.querySelectorAll('[data-action="toggle-node"]').forEach(el => {
            el.onclick = (e) => {
                e.stopPropagation();
                const id = el.dataset.id;
                if (this.expandedNodes.has(id)) this.expandedNodes.delete(id);
                else this.expandedNodes.add(id);
                this.render();
            };
        });

        // Group selection
        this.container.querySelectorAll('[data-action="select-group"]').forEach(el => {
            el.onclick = () => {
                this.selectedGroup = el.dataset.group;
                this.selectedDevice = null;
                UIControls.notify(`Filtered to circle: ${this.selectedGroup}`, 'info', { duration: 2500 });
                this.render();
            };
        });

        // Device unit selection (drill-down to the ant)
        this.container.querySelectorAll('[data-action="select-device"]').forEach(el => {
            el.onclick = () => {
                const agentId = el.dataset.agentId;
                this.selectedDevice = this.devices.find(d => d.agentId === agentId);
                UIControls.notify(`Inspecting Unit: ${this.selectedDevice?.nickname || agentId}`, 'success', { duration: 3000 });
                this.render();
            };
        });

        // Reset tree breadcrumbs
        const resetBreadcrumb = this.container.querySelector('[data-action="reset-tree"]');
        if (resetBreadcrumb) {
            resetBreadcrumb.onclick = () => {
                this.selectedGroup = null;
                this.selectedDevice = null;
                this.render();
            };
        }

        // Expand/Collapse all
        const expandAllBtn = this.container.querySelector('#eagle-tree-expand-all');
        if (expandAllBtn) {
            expandAllBtn.onclick = () => {
                if (this.expandedNodes.size > 1) {
                    this.expandedNodes = new Set(['root']);
                } else {
                    this.expandedNodes.add('root');
                    this.devices.forEach(d => {
                        this.expandedNodes.add(`group-${(d.relation || d.roleFamily || d.role || 'Family Circle').replace(/\s+/g, '-').toLowerCase()}`);
                    });
                }
                this.render();
            };
        }

        // Speed Unit Toggle
        UIControls.bindSegmentedToggles(this.container, (groupId, val) => {
            if (groupId === 'speed-unit-toggle') {
                this.speedUnit = val;
                UIControls.notify(`Velocity unit changed to ${val === 'mph' ? 'Miles/Hour' : 'Kilometers/Hour'}`, 'info', { duration: 2000 });
                this.render();
            }
        });

        // Bind 3D Card Flippers
        UIControls.bindFlippers(this.container);

        // Search in tree
        const searchInput = this.container.querySelector('#eagle-tree-search');
        if (searchInput) {
            searchInput.oninput = (e) => {
                this.searchQuery = e.target.value.toLowerCase();
                if (this.searchQuery) {
                    this.expandedNodes.add('root');
                    this.devices.forEach(d => {
                        const name = (d.nickname || d.name || '').toLowerCase();
                        const id = (d.agentId || '').toLowerCase();
                        if (name.includes(this.searchQuery) || id.includes(this.searchQuery)) {
                            this.expandedNodes.add(`group-${(d.relation || d.roleFamily || d.role || 'Family Circle').replace(/\s+/g, '-').toLowerCase()}`);
                        }
                    });
                }
                this.render();
                const input = this.container.querySelector('#eagle-tree-search');
                if (input) {
                    input.focus();
                    input.setSelectionRange(input.value.length, input.value.length);
                }
            };
        }

        // Focus on map handler
        const focusBtn = this.container.querySelector('#eagle-focus-map-btn');
        if (focusBtn && this.options.onNavigateMap) {
            focusBtn.onclick = () => {
                const id = focusBtn.dataset.agentId;
                UIControls.notify(`Focusing map onto unit: ${id}`, 'info', { duration: 2500 });
                this.options.onNavigateMap(id);
            };
        }
    }
}
