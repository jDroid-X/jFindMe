import { FeedbackDialog } from '../components/FeedbackDialog.js';

/**
 * jFindMe Matrix - Tactical Device List Sidebar View (OOPS Architecture)
 * Renders the tactical sidebar device roster with multi-filter chips, sorting criteria,
 * search with clear button, and quick-action triggers.
 */
export class DeviceListView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.onSelectDevice = options.onSelectDevice || null;
        this.onFilterChange = options.onFilterChange || null;
        this.currentFilter = "all"; // 'all', 'within_radius', 'online', 'alert', 'outside'
        this.currentSort = "distance"; // 'distance', 'battery', 'last_seen', 'name'
        this.searchQuery = "";
    }

    /**
     * Renders the complete device list roster
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices 
     * @param {number} radiusKm 
     */
    render(devices = [], radiusKm = 10) {
        const container = document.getElementById(this.containerId);
        if (!container) return;

        // Filter and sort devices
        const filtered = this.filterAndSortDevices(devices);
        const inRadiusCount = devices.filter(d => d.isWithinSearchRadius).length;
        const onlineCount = devices.filter(d => d.isOnline).length;
        const alertCount = devices.filter(d => (d.battery >= 0 && d.battery <= 15) || d.isSosActive).length;

        container.innerHTML = `
            <div class="flex flex-col h-full bg-slate-950/90 backdrop-blur-2xl border-r border-white/10 select-none">
                <!-- Search & Header Bar -->
                <div class="p-3.5 border-b border-white/10 space-y-2.5">
                    <div class="flex items-center justify-between">
                        <div class="flex items-center gap-2">
                            <span class="text-xs font-black tracking-widest text-sky-400 uppercase">Live Fleet</span>
                            <span class="px-2 py-0.5 rounded-full bg-sky-500/20 text-sky-400 text-[10px] font-mono font-bold">${devices.length}</span>
                        </div>
                        <div class="text-[10px] text-slate-400 font-mono">
                            <span class="text-emerald-400 font-bold">${inRadiusCount}</span> in ${radiusKm}km
                        </div>
                    </div>

                    <!-- Search Input with Clear Button -->
                    <div class="relative flex items-center">
                        <input type="text" id="device-search-input" 
                               placeholder="Filter by name, ID, role, address..." 
                               value="${this.searchQuery}"
                               class="w-full bg-slate-900/90 border border-white/10 rounded-xl pl-8 pr-7 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-sky-500 transition-all font-mono">
                        <svg class="w-3.5 h-3.5 absolute left-2.5 text-slate-500 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                        </svg>
                        ${this.searchQuery ? `
                            <button id="btn-clear-search" class="absolute right-2 text-slate-400 hover:text-white text-xs font-bold p-1">✕</button>
                        ` : ''}
                    </div>

                    <!-- Sort & Controls Row -->
                    <div class="flex items-center justify-between text-[10px] font-mono text-slate-400">
                        <span>SORT BY:</span>
                        <select id="device-sort-select" class="bg-slate-900 border border-white/10 rounded px-2 py-1 text-slate-300 focus:outline-none focus:border-sky-500">
                            <option value="distance" ${this.currentSort === 'distance' ? 'selected' : ''}>Distance (Closest First)</option>
                            <option value="battery" ${this.currentSort === 'battery' ? 'selected' : ''}>Battery Level (High to Low)</option>
                            <option value="last_seen" ${this.currentSort === 'last_seen' ? 'selected' : ''}>Recent Activity</option>
                            <option value="name" ${this.currentSort === 'name' ? 'selected' : ''}>Alphabetical</option>
                        </select>
                    </div>

                    <!-- Filter Chips -->
                    <div class="flex items-center gap-1.5 overflow-x-auto no-scrollbar pt-0.5">
                        <button class="filter-chip ${this.currentFilter === 'all' ? 'active' : ''}" data-filter="all">
                            All (${devices.length})
                        </button>
                        <button class="filter-chip ${this.currentFilter === 'within_radius' ? 'active' : ''}" data-filter="within_radius">
                            🎯 ${radiusKm}km (${inRadiusCount})
                        </button>
                        <button class="filter-chip ${this.currentFilter === 'online' ? 'active' : ''}" data-filter="online">
                            🟢 Online (${onlineCount})
                        </button>
                        ${alertCount > 0 ? `
                        <button class="filter-chip ${this.currentFilter === 'alert' ? 'active' : ''} text-red-400 border-red-500/30" data-filter="alert">
                            🚨 Alert (${alertCount})
                        </button>
                        ` : ''}
                    </div>
                </div>

                <!-- Device List Body -->
                <div class="flex-1 overflow-y-auto p-2 space-y-1.5 custom-scrollbar" id="device-roster-items">
                    ${filtered.length === 0 ? `
                        <div class="text-center py-14 text-slate-500 text-xs px-4">
                            <div class="w-10 h-10 mx-auto mb-3 rounded-full bg-slate-900 flex items-center justify-center text-slate-600 text-lg">📡</div>
                            <div class="font-bold text-slate-400">No matching devices</div>
                            <div class="text-[10px] text-slate-500 mt-1">Try clearing search or changing the filter chip.</div>
                        </div>
                    ` : filtered.map(d => this.renderDeviceItem(d)).join('')}
                </div>

                <!-- Footer Stats & Safe Zones Counter -->
                <div class="p-3 border-t border-white/10 bg-black/40 text-[10px] text-slate-400 flex items-center justify-between font-mono">
                    <span class="flex items-center gap-1.5">
                        <span class="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
                        Radar Active
                    </span>
                    <span>Displaying ${filtered.length}/${devices.length}</span>
                </div>
            </div>
        `;

        this.bindEvents(container, devices);
    }

    /**
     * Renders a single device item row
     * @param {import('../models/DeviceModel.js').DeviceModel} device 
     * @returns {string}
     */
    renderDeviceItem(device) {
        const themeColor = device.getThemeColor();
        const avatarUrl = device.getAvatarUrl();
        const batteryMeta = device.getBatteryMeta();
        const isWithin = device.isWithinSearchRadius;
        const hasAlert = (device.battery >= 0 && device.battery <= 15 && !device.isCharging) || device.isSosActive;

        return `
            <div class="device-card-item group p-2.5 rounded-xl border border-white/5 bg-slate-900/40 hover:bg-slate-800/80 hover:border-sky-500/40 transition-all cursor-pointer relative overflow-hidden"
                 data-agent-id="${device.agentId}">
                <!-- In-radius accent bar -->
                <div class="absolute left-0 top-0 bottom-0 w-1 ${hasAlert ? 'bg-red-500' : isWithin ? 'bg-sky-400' : 'bg-slate-700'}"></div>
                
                <div class="flex items-center gap-2.5 pl-1.5">
                    <!-- Avatar & Online Pulse (Transparent Background) -->
                    <div class="relative shrink-0">
                        <img src="${avatarUrl}" alt="${device.name}"
                             class="w-10 h-10 rounded-full object-cover border shadow-sm bg-transparent"
                             style="border-color: ${themeColor};"
                             onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2245%22>👤</text></svg>'">
                        <span class="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full border border-slate-900 ${device.isOnline ? 'bg-emerald-500' : 'bg-slate-600'}"></span>
                    </div>

                    <!-- Info Details -->
                    <div class="flex-1 min-w-0">
                        <div class="flex items-center justify-between gap-1">
                            <h4 class="text-xs font-bold text-white truncate group-hover:text-sky-300 transition-colors">${device.name}</h4>
                            <div class="flex items-center gap-1.5">
                                <span class="text-[10px] font-mono font-bold ${isWithin ? 'text-sky-400' : 'text-slate-500'}">
                                    ${device.getFormattedDistance()}
                                </span>
                                <label class="switch-toggle switch-sm shrink-0" title="Toggle Live Tracking Stream" onclick="event.stopPropagation()">
                                    <input type="checkbox" class="list-switch-stream" data-agent-id="${device.agentId}" ${device.raw?.acl?.acl_live_tracking === 'DENIED' ? '' : 'checked'}>
                                    <span class="switch-slider"></span>
                                </label>
                            </div>
                        </div>

                        <div class="flex items-center justify-between text-[10px] text-slate-400 mt-0.5 font-mono">
                            <span class="truncate max-w-[110px] text-sky-400/80">${device.roleFamily !== 'None' ? device.roleFamily : device.agentId}</span>
                            <span class="flex items-center gap-1 font-bold" style="color: ${batteryMeta.color}">
                                ${batteryMeta.icon} ${batteryMeta.text}
                            </span>
                        </div>

                        <div class="flex items-center justify-between text-[9px] text-slate-500 mt-1 font-mono">
                            <span>${device.getFormattedSpeed()}</span>
                            <span>${device.getFormattedLastSeen()}</span>
                        </div>
                    </div>
                </div>

                <!-- Hover Quick Actions Bar -->
                <div class="mt-2 pt-2 border-t border-white/5 flex items-center justify-end gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button class="px-2 py-0.5 rounded bg-sky-600/30 hover:bg-sky-600 text-sky-300 hover:text-white text-[9px] font-bold font-mono transition-all"
                            onclick="event.stopPropagation(); window.MatrixApp.focusDevice('${device.agentId}')">
                        🎯 Focus
                    </button>
                    <button class="px-2 py-0.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-[9px] font-mono transition-all"
                            onclick="event.stopPropagation(); window.MatrixApp.showHistory('${device.agentId}')">
                        📍 Trail
                    </button>
                    <button class="px-2 py-0.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-[9px] font-mono transition-all"
                            onclick="event.stopPropagation(); window.MatrixApp.showDossier('${device.agentId}')">
                        🔍 Dossier
                    </button>
                    <button class="px-2 py-0.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-[9px] font-mono transition-all"
                            onclick="event.stopPropagation(); window.MatrixApp.openEditProfile('${device.agentId}')">
                        ✏️ Edit
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Filters and sorts devices according to search text, filter chips, and sort criteria
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices 
     * @returns {Array<import('../models/DeviceModel.js').DeviceModel>}
     */
    filterAndSortDevices(devices) {
        let list = [...devices];

        // 1. Filter Chips
        if (this.currentFilter === 'within_radius') {
            list = list.filter(d => d.isWithinSearchRadius);
        } else if (this.currentFilter === 'online') {
            list = list.filter(d => d.isOnline);
        } else if (this.currentFilter === 'alert') {
            list = list.filter(d => (d.battery >= 0 && d.battery <= 15 && !d.isCharging) || d.isSosActive);
        } else if (this.currentFilter === 'outside') {
            list = list.filter(d => !d.isWithinSearchRadius);
        }

        // 2. Search Query
        if (this.searchQuery) {
            const q = this.searchQuery.toLowerCase().trim();
            list = list.filter(d => 
                (d.name && d.name.toLowerCase().includes(q)) ||
                (d.agentId && d.agentId.toLowerCase().includes(q)) ||
                (d.deviceId && d.deviceId.toLowerCase().includes(q)) ||
                (d.roleFamily && d.roleFamily.toLowerCase().includes(q)) ||
                (d.role && d.role.toLowerCase().includes(q)) ||
                (d.address && d.address.toLowerCase().includes(q)) ||
                (d.department && d.department.toLowerCase().includes(q))
            );
        }

        // 3. Sorting
        list.sort((a, b) => {
            if (this.currentSort === 'distance') {
                if (a.isWithinSearchRadius && !b.isWithinSearchRadius) return -1;
                if (!a.isWithinSearchRadius && b.isWithinSearchRadius) return 1;
                return a.distanceFromRefKm - b.distanceFromRefKm;
            } else if (this.currentSort === 'battery') {
                return b.battery - a.battery;
            } else if (this.currentSort === 'last_seen') {
                return (Number(b.lastSeen) || 0) - (Number(a.lastSeen) || 0);
            } else if (this.currentSort === 'name') {
                return a.name.localeCompare(b.name);
            }
            return 0;
        });

        return list;
    }

    /**
     * Binds DOM events
     * @param {HTMLElement} container 
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices 
     */
    bindEvents(container, devices) {
        // Search Input
        const searchInput = container.querySelector('#device-search-input');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.searchQuery = e.target.value;
                this.render(devices);
                const newInput = document.getElementById('device-search-input');
                if (newInput) {
                    newInput.focus();
                    newInput.setSelectionRange(newInput.value.length, newInput.value.length);
                }
            });
        }

        // Clear Search Button
        const clearBtn = container.querySelector('#btn-clear-search');
        if (clearBtn) {
            clearBtn.addEventListener('click', () => {
                this.searchQuery = '';
                this.render(devices);
            });
        }

        // Sort Select Dropdown
        const sortSelect = container.querySelector('#device-sort-select');
        if (sortSelect) {
            sortSelect.addEventListener('change', (e) => {
                this.currentSort = e.target.value;
                this.render(devices);
            });
        }

        // Filter Chips
        container.querySelectorAll('.filter-chip').forEach(btn => {
            btn.addEventListener('click', () => {
                this.currentFilter = btn.dataset.filter;
                this.render(devices);
                if (this.onFilterChange) this.onFilterChange(this.currentFilter);
            });
        });

        // Device Item Click
        container.querySelectorAll('.device-card-item').forEach(card => {
            card.addEventListener('click', () => {
                const agentId = card.dataset.agentId;
                const device = devices.find(d => d.agentId === agentId);
                if (device && this.onSelectDevice) {
                    this.onSelectDevice(device);
                }
            });
        });

        // Tactile Switch Handlers for Stream Toggle
        container.querySelectorAll('.list-switch-stream').forEach(sw => {
            sw.addEventListener('click', (e) => e.stopPropagation());
            sw.addEventListener('change', async (e) => {
                e.stopPropagation();
                const agentId = sw.getAttribute('data-agent-id');
                const val = sw.checked;
                try {
                    const res = await fetch('/api/device/toggle-acl', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ agentId, flag: 'acl_live_tracking', value: val })
                    });
                    if (!res.ok) throw new Error('Failed to update live stream ACL');
                    FeedbackDialog.showSlideNotification({
                        title: 'Telemetry Stream Toggled',
                        message: `Device [${agentId}] live stream set to ${val ? 'ACTIVE' : 'MUTED'}`,
                        type: val ? 'success' : 'warning',
                        timeout: 3500
                    });
                } catch (err) {
                    FeedbackDialog.showSlideNotification({
                        title: 'Stream Toggle Failed',
                        message: err.message,
                        type: 'danger',
                        timeout: 3500
                    });
                    sw.checked = !val;
                }
            });
        });
    }
}
