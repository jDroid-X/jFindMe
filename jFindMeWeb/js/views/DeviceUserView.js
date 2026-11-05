import { GeoUtils } from '../core/GeoUtils.js';
import { FeedbackDialog } from '../components/FeedbackDialog.js';
import { DeviceEditModalView } from './DeviceEditModalView.js';
import { DeviceDeleteModalView } from './DeviceDeleteModalView.js';
import { DeviceController } from '../controllers/DeviceController.js';
import { TelemetryValidator } from '../core/TelemetryValidator.js';
import { UIControls } from '../components/UIControls.js';
import { DeviceModel } from '../models/DeviceModel.js';

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
        this.groupBy = "none"; // 'none' | 'group' | 'role' | 'status'
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

        this.showArchivedLog = false;
        this.archivedDevices = [];
        this.loadArchivedDevices();
        this.masterRelease = null;
        this.loadMasterRelease();

        this.deleteModal = new DeviceDeleteModalView({
            onComplete: async (mode, agentId) => {
                await this.loadArchivedDevices();
                this.devices = (this.devices || []).filter(d => (d.agentId || d.deviceId) !== agentId);
                if (window.appInstance && Array.isArray(window.appInstance.cachedDevices)) {
                    window.appInstance.cachedDevices = window.appInstance.cachedDevices.filter(d => (d.agentId || d.deviceId) !== agentId);
                    window.appInstance.updateSyncUI('SYNCED', window.appInstance.cachedDevices);
                }
                if (window.showDeviceUser) {
                    window.showDeviceUser();
                } else {
                    this.render(this.devices, null, this.safeZones);
                }
            }
        });

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
     * Fetch list of cold-storage archived devices
     */
    async loadArchivedDevices() {
        try {
            const res = await (window.fetchApi ? window.fetchApi('/api/archived-devices') : fetch('/api/archived-devices').then(r => r.json()));
            if (res && res.archivedDevices) {
                this.archivedDevices = res.archivedDevices;
            }
        } catch (err) {
            console.warn('[DeviceUserView] Failed to load archived devices:', err.message);
        }
    }

    /**
     * Fetch published Master Release version metadata
     */
    async loadMasterRelease() {
        try {
            const release = await this.deviceController.fetchSystemRelease();
            if (release) {
                const isDiff = !this.masterRelease || this.masterRelease.version_code !== release.version_code;
                this.masterRelease = release;
                if (isDiff && this.devices && this.devices.length > 0) {
                    this.render(this.devices, this.selectedAgentId, this.safeZones);
                }
            }
        } catch (err) {
            console.warn('[DeviceUserView] Failed to load master release:', err.message);
        }
    }

    /**
     * Restore an archived device back to active master registry
     */
    async restoreArchivedDevice(agentId, name = '') {
        if (!agentId) return;
        try {
            FeedbackDialog.showSlideNotification({
                title: 'Restoring Device',
                message: `Restoring [${name || agentId}] from archive database...`,
                type: 'info',
                timeout: 2500
            });
            const res = await (window.fetchApi ? window.fetchApi('/api/device/restore-archive', {
                method: 'POST',
                body: JSON.stringify({ agentId })
            }) : fetch('/api/device/restore-archive', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ agentId })
            }).then(r => r.json()));

            if (res && res.error) throw new Error(res.error || 'Restore failed');

            FeedbackDialog.showSlideNotification({
                title: 'Device Restored',
                message: `Device [${name || agentId}] restored to active radar and master roster!`,
                type: 'success',
                timeout: 4000
            });

            await this.loadArchivedDevices();
            if (window.showDeviceUser) {
                window.showDeviceUser(agentId);
            } else {
                this.render(this.devices, agentId, this.safeZones);
            }
        } catch (err) {
            FeedbackDialog.showSlideNotification({
                title: 'Restore Failed',
                message: err.message,
                type: 'danger',
                timeout: 4000
            });
        }
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
            if (initialSelectedAgentId && this.devices.some(d => d.agentId === initialSelectedAgentId)) {
                this.selectedAgentId = initialSelectedAgentId;
            } else if (!this.selectedAgentId || !this.devices.some(d => d.agentId === this.selectedAgentId)) {
                this.selectedAgentId = this.devices[0].agentId;
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

        // Resolve Versioning Data for Master Data Pane (Clean SSOT Extraction)
        const parsedInstalled = (typeof DeviceModel !== 'undefined' && DeviceModel.parseVersion) 
            ? DeviceModel.parseVersion(rawNode, selectedDev) 
            : { versionName: (selectedDev && selectedDev.versionName) ? String(selectedDev.versionName).replace(/^V/i, '') : '1.0.13', versionCode: (selectedDev && selectedDev.versionCode) || 86 };
        const installedVerName = parsedInstalled.versionName.replace(/^V/i, '');
        const installedVerCode = parsedInstalled.versionCode;

        // Dynamic Master Release SSOT: Use live fetched release or default to current production build
        const currentMaster = this.masterRelease || {
            version_name: '1.0.13',
            version_code: 89,
            download_url_direct: 'https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jbutterfly/jButterFly.apk',
            download_url_versioned: 'https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jbutterfly/jButterFly_V89.apk',
            download_url_github: 'https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jbutterfly/jButterFly.apk',
            package_size_formatted: '80.6 MB'
        };
        const masterVerName = (currentMaster.version_name || '1.0.13').replace(/^V/i, '');
        const masterVerCode = parseInt(currentMaster.version_code || 89, 10);
        const hasNewVersion = masterVerCode > installedVerCode;

        container.innerHTML = `
            <div class="w-full h-full select-text overflow-y-auto custom-scrollbar p-1 font-sans space-y-1">
                
                <!-- COMPACT HEADER BAR -->
                <div class="card-theme px-3 py-1.5 flex items-center justify-between shadow-sm border-b border-subtle-theme">
                    <div class="flex items-center gap-2.5 min-w-0">
                        <div class="w-7 h-7 rounded-lg bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-sky-400 font-bold text-sm shrink-0 shadow-inner">
                            📱
                        </div>
                        <div class="min-w-0">
                            <h2 class="text-[11.5px] font-bold tracking-wide text-primary-theme uppercase flex items-center gap-1.5">
                                <span>Device User Oversight Matrix</span>
                                <span class="badge-status badge-accent text-[7.5px] px-1.5 py-0.2">DYNAMIC SSOT</span>
                            </h2>
                            <p class="text-[9.5px] text-secondary-theme font-mono truncate select-text">
                                Master Identity: <span class="text-accent-theme font-bold">${selectedDev ? selectedDev.name : 'None'}</span> (<span class="copyable-badge text-accent-theme font-mono cursor-pointer" data-copy="${selectedDev ? selectedDev.agentId : ''}" title="Click to copy Agent ID">${selectedDev ? selectedDev.agentId : 'N/A'}</span>) • Node: <span class="text-primary-theme">${selectedDev ? (selectedDev.deviceName || selectedDev.model) : ''}</span>
                            </p>
                        </div>
                    </div>

                    <div class="flex items-center gap-1.5 shrink-0">
                        <button id="btn-du-dup-devices" class="btn-action-primary text-[10.5px] py-1 px-2.5 bg-gradient-to-r from-amber-600 to-amber-500 hover:from-amber-500 hover:to-amber-400 text-slate-950 font-extrabold flex items-center gap-1 shadow-sm border border-amber-400/50 cursor-pointer" title="Reconcile Duplicate Devices via Silicon SHA-256">
                            <span>🔄 Dup Device</span>
                        </button>
                        ${selectedDev ? `
                        <button id="btn-du-edit-device" class="btn-action-secondary text-[10.5px] py-1 px-2.5 bg-sky-600/20 hover:bg-sky-600/40 text-sky-300 font-extrabold flex items-center gap-1 shadow-sm border border-sky-400/50 cursor-pointer" title="Edit Profile Dossier (Avatar, Name, Persona, Circle, ACL)">
                            <span>✏️ Edit Profile</span>
                        </button>
                        ` : ''}
                        <button id="btn-du-delete-device" class="btn-action-danger text-[10.5px] py-1 px-2.5 bg-gradient-to-r from-rose-600 to-red-600 hover:from-rose-500 hover:to-red-500 text-white font-extrabold flex items-center gap-1 shadow-sm border border-rose-400/50 cursor-pointer" title="Delete Device Everywhere (Archive / CleanSlate)">
                            <span>🗑️ Delete</span>
                        </button>
                        ${selectedDev ? `
                        <button id="btn-du-view-map" class="btn-action-primary text-[10.5px] py-1 px-2.5 flex items-center gap-1">
                            <span>🗺️ Locate on Map</span>
                        </button>
                        ` : ''}
                    </div>
                </div>

                <!-- 5-PANE MULTI-COLUMN SMART-FIT WORKSPACE (NO SQUISHING / OVERLAPPING) -->
                <div class="flex flex-col xl:flex-row gap-2 items-stretch w-full overflow-x-auto custom-scrollbar pb-1">
                    
                    <!-- PANE 1: MASTER DEVICE ROSTER & SEARCH BAR (PERSON NAME PROMINENT, MODEL SUBTITLE) -->
                    <div class="card-theme p-2 flex flex-col space-y-1.5 shadow-sm w-full xl:w-[260px] xl:shrink-0">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-1">
                            <span class="text-[11px] font-black uppercase text-accent-theme tracking-wider">1. Device Roster</span>
                            <div class="flex items-center gap-1.5">
                                <label class="flex items-center gap-1 text-[8.5px] font-mono cursor-pointer text-secondary-theme hover:text-amber-500 select-none transition-colors" title="Toggle visibility of cold-storage archived devices">
                                    <input type="checkbox" id="du-check-archive-log" class="rounded border-subtle-theme text-amber-500 focus:ring-0 w-2.5 h-2.5 cursor-pointer" ${this.showArchivedLog ? 'checked' : ''}>
                                    <span class="${this.showArchivedLog ? 'text-warning-theme font-bold' : ''}">Archive (${this.archivedDevices ? this.archivedDevices.length : 0})</span>
                                </label>
                                <span class="badge-status badge-accent text-[8px] px-1.5 py-0.2">${filteredDevices.length}/${this.devices.length}</span>
                            </div>
                        </div>

                        <!-- Search Input (Smart Fit) -->
                        <div class="relative flex items-center">
                            <input type="text" id="du-search-input" 
                                   placeholder="🔍 Filter device, name, ID..." 
                                   value="${this.searchQuery}"
                                   class="input-theme w-full pl-6 pr-5 py-1 text-[10.5px] font-mono">
                            <svg class="w-3 h-3 absolute left-1.5 text-secondary-theme pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                            </svg>
                            ${this.searchQuery ? `
                                <button id="btn-du-clear-search" class="absolute right-1 text-muted-theme hover:text-primary-theme text-[10px] font-bold p-0.5">✕</button>
                            ` : ''}
                        </div>

                        <!-- TEMPORAL TIME WINDOW FILTER TOOLBAR -->
                        <div class="grid grid-cols-4 gap-1 pt-0.5">
                            <button class="btn-du-time-filter text-[8px] font-bold py-1 px-1 rounded truncate text-center transition-all ${this.timeWindowFilter === 'all' ? 'btn-action-primary' : 'btn-action-secondary'}" data-time-filter="all">All (${this.devices.length})</button>
                            <button class="btn-du-time-filter text-[8px] font-bold py-1 px-1 rounded truncate text-center transition-all ${this.timeWindowFilter === 'live' ? 'btn-action-primary' : 'btn-action-secondary'}" data-time-filter="live">⚡Live</button>
                            <button class="btn-du-time-filter text-[8px] font-bold py-1 px-1 rounded truncate text-center transition-all ${this.timeWindowFilter === 'today' ? 'btn-action-primary' : 'btn-action-secondary'}" data-time-filter="today">Today</button>
                            <button class="btn-du-time-filter text-[8px] font-bold py-1 px-1 rounded truncate text-center transition-all ${this.timeWindowFilter === '24h' ? 'btn-action-primary' : 'btn-action-secondary'}" data-time-filter="24h">24h</button>
                        </div>

                        <!-- DYNAMIC GROUP BY SELECTOR (OOPS Hierarchy) -->
                        <div class="flex items-center justify-between gap-1 pt-1 border-t border-subtle-theme px-0.5">
                            <span class="text-[8.5px] font-mono text-secondary-theme font-bold">Group By:</span>
                            <select id="du-group-by-select" class="input-theme py-0.5 px-1.5 text-[8.5px] font-mono rounded cursor-pointer">
                                <option value="none" ${this.groupBy === 'none' ? 'selected' : ''}>None (Flat)</option>
                                <option value="group" ${this.groupBy === 'group' ? 'selected' : ''}>Circle / Group</option>
                                <option value="role" ${this.groupBy === 'role' ? 'selected' : ''}>Role</option>
                                <option value="status" ${this.groupBy === 'status' ? 'selected' : ''}>RAG Status</option>
                            </select>
                        </div>

                        <!-- Roster List (Switched: Persona Name as Prominent Title, Device Name as Subtitle) -->
                        <div class="space-y-1 overflow-y-auto custom-scrollbar max-h-[calc(100vh-160px)] pr-0.5" id="du-roster-list">
                            ${this.renderRosterList(filteredDevices)}

                            ${this.showArchivedLog ? `
                                <div class="mt-2 pt-1 border-t-2 border-amber-500/40">
                                    <div class="flex items-center justify-between pb-1 px-0.5">
                                        <span class="text-[9.5px] font-black uppercase text-amber-500 tracking-wider flex items-center gap-1">
                                            <span>📦 Cold Storage Archive</span>
                                            <span class="badge-status badge-warning text-[7px] px-1 py-0.2">${this.archivedDevices.length}</span>
                                        </span>
                                        <span class="text-[8px] text-muted-theme font-mono">Isolated DB</span>
                                    </div>
                                    ${this.archivedDevices.length === 0 ? `
                                        <div class="text-[9px] text-muted-theme italic py-1 text-center bg-amber-500/5 rounded border border-amber-500/20">
                                            No devices in archive.
                                        </div>
                                    ` : `
                                        <div class="space-y-1">
                                            ${this.archivedDevices.map(arc => `
                                                <div class="p-1 rounded-md border border-amber-500/30 bg-amber-950/20 hover:bg-amber-900/30 transition-all flex items-center justify-between gap-1 text-xs">
                                                    <div class="min-w-0 flex-1">
                                                        <div class="flex items-center gap-1">
                                                            <span class="text-[10px] font-bold text-amber-400 truncate">${arc.name || arc.agentId}</span>
                                                            <span class="badge-status badge-warning text-[6.5px] px-1 py-0.2">ARCHIVED</span>
                                                        </div>
                                                        <div class="text-[8px] text-secondary-theme font-mono truncate">
                                                            ID: <span class="text-accent-theme font-bold">${arc.agentId}</span> • ${arc.archivedAt ? new Date(arc.archivedAt).toLocaleDateString() : 'Stored'}
                                                        </div>
                                                    </div>
                                                    <div class="flex items-center gap-1 shrink-0">
                                                        <button class="btn-restore-archive px-1.5 py-0.5 rounded text-[8px] font-bold bg-emerald-600/30 text-emerald-300 hover:bg-emerald-600/50 border border-emerald-500/40 cursor-pointer" data-agent-id="${arc.agentId}" data-name="${arc.name || arc.agentId}" title="Restore device back to active radar and master roster">
                                                            ♻️ Restore
                                                        </button>
                                                        <button class="btn-cleanslate-archive px-1.5 py-0.5 rounded text-[8px] font-bold bg-rose-600/30 text-rose-300 hover:bg-rose-600/50 border border-rose-500/40 cursor-pointer" data-agent-id="${arc.agentId}" data-name="${arc.name || arc.agentId}" title="CleanSlate permanent erasure">
                                                            ⚡
                                                        </button>
                                                    </div>
                                                </div>
                                            `).join('')}
                                        </div>
                                    `}
                                </div>
                            ` : ''}
                        </div>
                    </div>

                    ${selectedDev ? `
                    <!-- PANE 2: TIER 1 - MASTER DATA (HARDWARE SILICON SENTINEL & IDENTITY) -->
                    <div class="card-theme p-2 space-y-1.5 shadow-sm flex flex-col w-full xl:w-[275px] xl:shrink-0">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-1 shrink-0">
                            <div class="flex items-center gap-1.5 min-w-0">
                                <span class="text-[11px] font-black uppercase text-accent-theme tracking-wider">2. Master Data</span>
                                <span class="badge-status badge-accent text-[7.5px] px-1.5 py-0.2 font-mono">TIER 1</span>
                            </div>
                            <div class="flex items-center gap-1 shrink-0">
                                <button id="btn-edit-profile-dossier" class="px-2 py-0.5 rounded text-[8.5px] font-bold bg-sky-500/20 text-accent-theme hover:bg-sky-500/30 border border-sky-500/30 transition-all flex items-center gap-0.5 cursor-pointer" title="Edit Master & Sub-Master Profile Dossier">
                                    <span>✏️ Edit</span>
                                </button>
                                <button id="btn-toggle-org" class="text-[8.5px] text-accent-theme hover:underline font-mono font-bold">
                                    ${this.isOrgExpanded ? '[-] Hide' : '[+] Org'}
                                </button>
                            </div>
                        </div>

                        <div class="overflow-y-auto custom-scrollbar space-y-1.5 max-h-[calc(100vh-160px)] pr-0.5">
                            <!-- Tier 1: Master Hardware Anchor & Identity Card -->
                            <div class="surface-theme p-2 rounded-xl space-y-1 text-xs font-mono border select-text">
                                <div class="text-[9.5px] font-bold text-accent-theme uppercase tracking-wider font-sans border-b border-subtle-theme pb-1 flex items-center justify-between">
                                    <span>🏢 Hardware & Identity</span>
                                    <span class="copyable-badge badge-status badge-accent text-[7.5px] px-1.5 py-0.2 select-text font-mono cursor-pointer" data-copy="${selectedDev.agentId}" title="Click to copy Agent ID">${selectedDev.agentId} 📋</span>
                                </div>
                                ${(() => {
                                    const twins = this.devices.filter(d => (d.agentId !== selectedDev.agentId) && ((d.siliconHash && d.siliconHash === selectedDev.siliconHash) || (d.name && selectedDev.name && d.name.toLowerCase() === selectedDev.name.toLowerCase())));
                                    if (twins.length === 0) return '';
                                    const allCopies = [selectedDev, ...twins];
                                    const newestCopy = allCopies.reduce((prev, curr) => (curr.lastSeen > prev.lastSeen ? curr : prev), allCopies[0]);
                                    const isActive = (selectedDev.agentId === newestCopy.agentId);
                                    if (isActive) {
                                        return `
                                            <div class="p-1 px-1.5 rounded-lg bg-emerald-500/15 border border-emerald-500/30 text-[8.5px] font-mono text-emerald-300 flex items-center justify-between">
                                                <span>🟢 <strong>CURRENT ACTIVE NODE</strong> (Live GPS)</span>
                                                <span class="text-[8px] text-muted-theme">Twin Stale: ${twins.map(t => t.agentId).join(', ')}</span>
                                            </div>
                                        `;
                                    } else {
                                        return `
                                            <div class="p-1 px-1.5 rounded-lg bg-amber-500/15 border border-amber-500/30 text-[8.5px] font-mono text-amber-300 flex items-center justify-between">
                                                <span>⚠️ <strong>STALE DUPLICATE NODE</strong></span>
                                                <button class="du-switch-to-active text-sky-300 underline font-bold cursor-pointer hover:text-sky-200" data-target-id="${newestCopy.agentId}">
                                                    Switch to Active (${newestCopy.agentId}) ➔
                                                </button>
                                            </div>
                                        `;
                                    }
                                })()}

                                <div class="flex items-center gap-2 pb-1.5 border-b border-subtle-theme">
                                    <img src="${selectedDev.getAvatarUrl()}" alt="${selectedDev.name}" class="w-8 h-8 rounded-full object-cover border-2 shadow-sm shrink-0" style="border-color: ${selectedDev.getThemeColor ? selectedDev.getThemeColor() : '#38bdf8'};">
                                    <div class="min-w-0 flex-1 select-text">
                                        <h3 class="font-bold text-[11.5px] text-primary-theme truncate select-text">${selectedDev.name}</h3>
                                        <div class="text-[9px] text-accent-theme font-sans font-bold truncate">${selectedDev.roleFamily !== 'None' ? selectedDev.roleFamily : selectedDev.role}</div>
                                        <div class="text-[8.5px] text-secondary-theme truncate">${selectedDev.deviceName || selectedDev.model}</div>
                                    </div>
                                </div>

                                <div class="space-y-1 text-[9.5px] select-text">
                                    <div class="flex justify-between items-center gap-1 py-0.5 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0">Hardware ID:</span> 
                                        <span class="text-primary-theme font-bold truncate text-right flex-1 min-w-0 font-mono select-text copyable-badge cursor-pointer" data-copy="${selectedDev.deviceId}" title="Click to copy">${selectedDev.deviceId}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.5 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0">Auth UID:</span> 
                                        <span class="text-primary-theme font-bold truncate text-right flex-1 min-w-0 font-mono select-text text-[8.5px] copyable-badge cursor-pointer" data-copy="${selectedDev.authUid || ''}" title="${selectedDev.authUid || 'Unlinked'}">${selectedDev.authUid || (rawNode.Identity && rawNode.Identity.auth_uid) || (rawNode.device && rawNode.device.live && rawNode.device.live.auth_uid) || 'Unlinked'}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.5 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0">Model:</span> 
                                        <span class="text-primary-theme font-bold truncate text-right flex-1 min-w-0 select-text" title="${selectedDev.brand} / ${selectedDev.model}">${selectedDev.brand} / ${selectedDev.model}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.5 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0">Chipset:</span> 
                                        <span class="text-primary-theme font-mono truncate text-right flex-1 min-w-0 select-text" title="${selectedDev.board} / ${selectedDev.hardware}">${selectedDev.board} / ${selectedDev.hardware}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.5 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0">Node:</span> 
                                        <span class="text-primary-theme font-mono truncate text-right flex-1 min-w-0 select-text">${typeof selectedDev.device === 'string' ? selectedDev.device : (selectedDev.device?.name || 'generic_node')}</span>
                                    </div>
                                    <div class="du-sim-row flex justify-between items-center gap-1 py-0.5 border-b border-subtle-theme cursor-pointer group hover:bg-white/5 rounded px-0.5 transition-colors" title="Click to edit SIM, Phone, Carrier & Network Routing">
                                        <span class="text-secondary-theme shrink-0 flex items-center gap-1">
                                            <span>SIM / Cellular:</span>
                                            <span class="text-[7.5px] text-sky-400 opacity-0 group-hover:opacity-100 transition-opacity">✏️</span>
                                        </span> 
                                        <span class="text-emerald-400 font-mono font-bold truncate text-right flex-1 min-w-0 select-text text-[8.5px] flex items-center justify-end gap-1" title="${selectedDev.simStatus || 'Cellular Active'}">
                                            <span class="inline-block w-1.5 h-1.5 rounded-full ${selectedDev.hasSim ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'}"></span>
                                            <span>${selectedDev.simStatus || (selectedDev.phone ? `Active • ${selectedDev.phone}` : 'Active')}</span>
                                        </span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.5 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0">Routing Pref:</span>
                                        <span class="text-sky-300 font-mono font-semibold text-right flex-1 min-w-0 select-text text-[8.5px]">
                                            ${selectedDev.networkPreference === 'WIFI_ONLY' ? '📡 Wi-Fi Only' : (selectedDev.networkPreference === 'MOBILE_DATA_ONLY' ? '📶 Mobile Data Only' : '⚡ Auto (Mobile / Wi-Fi)')}
                                        </span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.5 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0">Active Route:</span>
                                        <span class="text-emerald-400 font-mono font-semibold text-right flex-1 min-w-0 select-text text-[8.5px]">
                                            ${selectedDev.activeRoute || 'N1_CELLULAR_FAST'}
                                        </span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.5 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0">Tower / RSSI:</span>
                                        <span class="text-sky-300 font-mono font-semibold text-right flex-1 min-w-0 select-text text-[8.5px]">
                                            ${selectedDev.cellularOperator || 'Cellular'} (${selectedDev.cellularSignalDbm != null ? selectedDev.cellularSignalDbm + ' dBm' : 'Normal'})
                                        </span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.5 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0">IMEI 1:</span> 
                                        <span class="text-primary-theme font-mono truncate text-right flex-1 min-w-0 select-text copyable-badge cursor-pointer" data-copy="${selectedDev.imei1}" title="${selectedDev.imei1.includes('Protected') ? 'Hardware IMEI protected by Android 10+ OS sandbox' : 'Click to copy IMEI 1'}">${selectedDev.imei1}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.5 border-b border-subtle-theme">
                                        <span class="text-secondary-theme shrink-0">IMEI 2:</span> 
                                        <span class="text-primary-theme font-mono truncate text-right flex-1 min-w-0 select-text copyable-badge cursor-pointer" data-copy="${selectedDev.imei2}" title="${selectedDev.imei2.includes('Protected') ? 'Hardware IMEI protected by Android 10+ OS sandbox' : 'Click to copy IMEI 2'}">${selectedDev.imei2}</span>
                                    </div>
                                    <div class="flex justify-between items-center gap-1 py-0.5">
                                        <span class="text-secondary-theme shrink-0">Silicon SHA:</span> 
                                        <span class="text-warning-theme font-bold font-mono truncate text-right flex-1 min-w-0 select-text text-[8.5px] copyable-badge cursor-pointer" data-copy="${selectedDev.siliconHash}" title="Click to copy Silicon SHA">${selectedDev.siliconHash}</span>
                                    </div>
                                    ${selectedDev.simDossier ? `
                                    <div class="border-t border-subtle-theme/60 pt-1 mt-1 space-y-0.5">
                                        <div class="text-[8.5px] font-bold text-emerald-400 uppercase tracking-wider flex items-center justify-between">
                                            <span>🛡️ SIM Dossier (Private)</span>
                                            <span class="text-[7.5px] px-1 py-0.2 rounded font-mono ${selectedDev.simDossier.slot_0?.anti_tamper_status === 'SIM_SWAPPED' ? 'bg-red-500/20 text-red-400 font-bold' : 'bg-emerald-500/20 text-emerald-400'}">
                                                ${selectedDev.simDossier.slot_0?.anti_tamper_status === 'SIM_SWAPPED' ? '🚨 TAMPERED' : '🟢 VERIFIED'}
                                            </span>
                                        </div>
                                        <div class="flex justify-between items-center gap-1 py-0.5">
                                            <span class="text-secondary-theme shrink-0 text-[8px]">SIM Serial:</span>
                                            <span class="text-primary-theme font-mono text-[8px] text-right truncate select-text">${selectedDev.simDossier.slot_0?.sim_serial_masked || '...'}</span>
                                        </div>
                                        ${selectedDev.simDossier.slot_0?.phone ? `
                                        <div class="flex justify-between items-center gap-1 py-0.5">
                                            <span class="text-secondary-theme shrink-0 text-[8px]">Bound Phone:</span>
                                            <span class="text-emerald-300 font-mono font-semibold text-[8px] text-right truncate select-text">${selectedDev.simDossier.slot_0.phone} ${selectedDev.simDossier.slot_0.is_guardian_bound ? '(Bound)' : ''}</span>
                                        </div>` : ''}
                                    </div>` : ''}

                                    <!-- [Variables]: oldArchives, oldKeys | [Constants]: None | [Dependencies]: DeviceUserView | [Integration Points]: Old_Device_ID Evolution SSOT -->
                                    ${(() => {
                                        const oldArchives = selectedDev.oldDeviceIdArchive || (rawNode && rawNode.Old_Device_ID) || {};
                                        const oldKeys = Object.keys(oldArchives);
                                        if (oldKeys.length === 0) return '';
                                        return `
                                            <div class="border-t border-amber-500/40 pt-1 mt-1 space-y-1">
                                                <div class="text-[8.5px] font-bold text-amber-400 uppercase tracking-wider flex items-center justify-between">
                                                    <span class="flex items-center gap-1">📦 Merged Predecessors (Old_Device_ID)</span>
                                                    <span class="badge-status badge-warning text-[7px] px-1 py-0.2">${oldKeys.length} Archived</span>
                                                </div>
                                                <div class="space-y-1">
                                                    ${oldKeys.map(oldId => {
                                                        const arch = oldArchives[oldId] || {};
                                                        const meta = arch._merge_metadata || {};
                                                        const oldIso = meta.merged_at_iso ? new Date(meta.merged_at_iso).toLocaleString() : '';
                                                        return `
                                                            <div class="p-1 px-1.5 rounded bg-amber-500/10 border border-amber-500/30 text-[8px] font-mono flex items-center justify-between">
                                                                <div>
                                                                    <span class="text-amber-300 font-bold">ID: ${oldId}</span>
                                                                    ${oldIso ? `<span class="text-muted-theme text-[7px] block">${oldIso}</span>` : ''}
                                                                </div>
                                                                <span class="badge-status badge-success text-[7px] px-1 py-0.2 font-bold">[✔] Preserved</span>
                                                            </div>
                                                        `;
                                                    }).join('')}
                                                </div>
                                            </div>
                                        `;
                                    })()}

                                    <!-- MASTER DATA FOOTER: INSTALLED VERSION & REMOTE OTA PUSH (STRICT DEVICE-DEPENDENT SSOT) -->
                                    <div class="border-t border-subtle-theme pt-1.5 mt-1.5 space-y-1">
                                        <div class="text-[9px] font-bold text-accent-theme uppercase tracking-wider font-sans flex items-center justify-between">
                                            <span class="flex items-center gap-1">📦 App Package & OTA</span>
                                            <span class="badge text-[7px] px-1.5 py-0.2 font-mono font-bold ${hasNewVersion ? 'badge-warning animate-pulse' : 'badge-status badge-accent'}">
                                                ${hasNewVersion ? 'UPDATE READY' : 'LATEST'}
                                            </span>
                                        </div>
                                        <div class="flex justify-between items-center gap-1 py-0.5 border-b border-subtle-theme">
                                            <span class="text-secondary-theme shrink-0">Installed:</span>
                                            <span class="text-primary-theme font-bold font-mono text-right truncate text-[8.5px] select-text">
                                                V${installedVerName} (Build ${installedVerCode})
                                            </span>
                                        </div>
                                        <div class="flex justify-between items-center gap-1 py-0.5 border-b border-subtle-theme">
                                            <span class="text-secondary-theme shrink-0">Package Specs:</span>
                                            <span class="text-secondary-theme font-mono text-right truncate text-[8px] select-text">
                                                ${selectedDev.packageSizeFormatted || '80.6 MB'} • Schema: <span class="text-accent-theme font-bold">${selectedDev.databaseSchema || 'V75'}</span>
                                            </span>
                                        </div>
                                        ${(selectedDev.otaPendingPush && selectedDev.otaPendingPush.status !== 'COMPLETED' && (selectedDev.otaPendingPush.version_code > installedVerCode)) ? `
                                        <div class="flex justify-between items-center gap-1 py-0.5 bg-amber-500/10 px-1 rounded border border-amber-500/30">
                                            <span class="text-amber-400 font-bold text-[7.5px] uppercase">Pending Dispatch:</span>
                                            <span class="text-amber-300 font-mono text-[7.5px] font-bold">Build ${selectedDev.otaPendingPush.version_code || ''} (${selectedDev.otaPendingPush.status || 'PENDING'})</span>
                                        </div>
                                        ` : ''}
                                        <div class="flex justify-between items-center gap-1 py-0.5">
                                            <span class="text-secondary-theme shrink-0">Available OTA:</span>
                                            <span class="font-bold font-mono text-right truncate text-[8.5px] select-text ${hasNewVersion ? 'text-accent-theme animate-pulse' : 'text-emerald-400'}">
                                                V${masterVerName} (Build ${masterVerCode}) [${currentMaster.package_size_formatted || '80.6 MB'}]
                                            </span>
                                        </div>
                                        <div class="pt-1 flex items-center gap-1.5">
                                            <button id="btn-push-ota-version" 
                                                    data-agent-id="${selectedDev.agentId}"
                                                    data-target-version="${masterVerName}"
                                                    data-target-code="${masterVerCode}"
                                                    data-installed-code="${installedVerCode}"
                                                    data-download-url="${currentMaster.download_url_versioned || currentMaster.download_url_github || currentMaster.download_url_media || currentMaster.download_url_drive || ''}"
                                                    class="flex-1 py-1.5 px-2 rounded-lg text-[9px] font-mono font-bold flex items-center justify-center gap-1.5 transition-all shadow-sm ${hasNewVersion 
                                                        ? 'bg-gradient-to-r from-amber-500/25 via-sky-500/25 to-emerald-500/25 border border-sky-400 text-sky-200 hover:brightness-125 hover:shadow-cyan-500/30 active:scale-95 cursor-pointer' 
                                                        : 'bg-subtle-theme/30 border border-subtle-theme text-secondary-theme hover:text-primary-theme active:scale-95 cursor-pointer'}"
                                                    title="${hasNewVersion ? 'Push and install this new version in device background over Mobile Data / Wi-Fi' : 'Re-push latest master build to device in background'}">
                                                <span class="text-[11px]">🚀</span>
                                                <span id="btn-push-ota-text">${hasNewVersion ? `Push & Install V${masterVerName} (Background)` : `Re-Push V${masterVerName} (Background)`}</span>
                                            </button>
                                            <button id="btn-open-ota-chooser"
                                                    data-agent-id="${selectedDev.agentId}"
                                                    class="py-1.5 px-2 rounded-lg text-[9px] font-mono border border-subtle-theme bg-subtle-theme/30 hover:bg-white/10 text-secondary-theme hover:text-primary-theme transition-all cursor-pointer"
                                                    title="Deployment Mode Options (Screen Mode vs Background Script)">
                                                ⚙️
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Expandable Org Data Panel -->
                            <div id="org-data-panel" class="surface-theme p-2 rounded-xl space-y-1 text-xs font-mono border select-text ${this.isOrgExpanded ? 'block' : 'hidden'}">
                                <div class="text-[9.5px] font-bold text-accent-theme uppercase tracking-wider font-sans border-b border-subtle-theme pb-1 flex items-center justify-between">
                                    <span>🏢 Org & Context</span>
                                    <span class="text-[8px] text-muted-theme font-mono">ORG</span>
                                </div>
                                <div class="space-y-1 text-[9.5px]">
                                    <div class="flex justify-between py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Org:</span> <span class="text-primary-theme font-bold truncate">${(rawNode.profile && rawNode.profile.orgName) || (rawNode.profile && rawNode.profile.org_name) || 'jFindMe Fleet'}</span></div>
                                    <div class="flex justify-between py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Dept:</span> <span class="text-primary-theme truncate">${selectedDev.department || 'General'}</span></div>
                                    <div class="flex justify-between py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Role:</span> <span class="text-primary-theme font-bold truncate">${selectedDev.role}</span></div>
                                    <div class="flex justify-between py-0.5"><span class="text-secondary-theme">Emergency:</span> <span class="${selectedDev.emergencyNote ? 'text-warning-theme font-bold' : 'text-muted-theme'} truncate">${selectedDev.emergencyNote || 'None'}</span></div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 3: TIER 2 - SUB-MASTER DATA (PROFILE, PERSONA & CIRCLE CONNECTIONS) -->
                    <div class="card-theme p-2 space-y-1.5 shadow-sm flex flex-col w-full xl:w-[280px] xl:shrink-0">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-1 shrink-0">
                            <div class="flex items-center gap-1.5 min-w-0">
                                <span class="text-[11px] font-black uppercase text-accent-theme tracking-wider">3. Sub-Master</span>
                                <span class="badge-status badge-accent text-[7.5px] px-1.5 py-0.2 font-mono">TIER 2</span>
                            </div>
                            <span class="badge-status badge-accent text-[8px] px-1.5 py-0.2 font-mono">📊 ${relCount} Linked</span>
                        </div>

                        <div class="space-y-1.5 overflow-y-auto custom-scrollbar max-h-[560px] pr-0.5">
                            <!-- Persona Details Card -->
                            <div class="surface-theme p-2 rounded-xl space-y-1 text-xs font-mono border select-text">
                                <div class="text-[9.5px] font-bold text-accent-theme uppercase tracking-wider font-sans border-b border-subtle-theme pb-1 flex items-center justify-between">
                                    <span>👤 Profile Persona</span>
                                    <button id="btn-du-edit-persona" class="px-1.5 py-0.5 rounded text-[8px] font-bold bg-sky-500/20 text-accent-theme hover:bg-sky-500/30 border border-sky-500/30 transition-all flex items-center gap-0.5 cursor-pointer" title="Edit Persona Profile">
                                        <span>✏️ Edit</span>
                                    </button>
                                </div>
                                <div class="space-y-1 text-[9.5px]">
                                    <div class="flex justify-between py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Name:</span> <span class="text-primary-theme font-bold truncate select-text">${selectedDev.name}</span></div>
                                    <div class="flex justify-between py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Phone:</span> <span class="text-primary-theme font-mono truncate select-text copyable-badge cursor-pointer" data-copy="${selectedDev.phone || ''}" title="Click to copy">${selectedDev.phone || 'Not Registered'}</span></div>
                                    <div class="flex justify-between py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Email:</span> <span class="text-primary-theme truncate select-text copyable-badge cursor-pointer" data-copy="${selectedDev.email || ''}" title="${selectedDev.email || ''}">${selectedDev.email || 'Not Registered'}</span></div>
                                    <div class="flex justify-between py-0.5 border-b border-subtle-theme"><span class="text-secondary-theme">Family Role:</span> <span class="text-warning-theme font-bold">${selectedDev.roleFamily}</span></div>
                                    <div class="flex justify-between py-0.5"><span class="text-secondary-theme">Friend Role:</span> <span class="text-success-theme font-bold">${selectedDev.roleFriend}</span></div>
                                </div>
                            </div>

                            <!-- Tree Container: Family & Friends Circle Connections -->
                            <div class="surface-theme p-2 rounded-xl border space-y-1 font-mono select-text">
                                <div class="text-[9.5px] font-bold text-accent-theme uppercase tracking-wider font-sans border-b border-subtle-theme pb-1 flex items-center justify-between">
                                    <span>🤝 Circle Connections</span>
                                    <div class="flex items-center gap-1">
                                        <button id="btn-du-add-link" class="px-1.5 py-0.5 rounded text-[8px] font-bold bg-sky-500/20 text-accent-theme hover:bg-sky-500/30 border border-sky-500/30 transition-all flex items-center gap-0.5 cursor-pointer" title="Connect a new Circle Member">
                                            <span>➕ Link</span>
                                        </button>
                                    </div>
                                </div>

                                <!-- Tree Branches -->
                                <div class="space-y-1">
                                    <!-- Family Branch -->
                                    <details class="group/family space-y-0.5" open>
                                        <summary class="cursor-pointer select-none text-[9.5px] font-bold text-amber-500 font-sans uppercase flex items-center gap-1 list-none hover:text-amber-400">
                                            <span class="inline-block transition-transform group-open/family:rotate-90 text-[8px] font-black">&gt;</span>
                                            <span>👨‍👩‍👧‍👦 Family Circle</span>
                                        </summary>
                                        <div class="space-y-1 pl-1.5 border-l border-amber-500/20 mt-1">
                                            ${this.renderTreeMembers(selectedDev, 'family')}
                                        </div>
                                    </details>

                                    <!-- Friends Branch -->
                                    <details class="group/friends space-y-0.5" open>
                                        <summary class="cursor-pointer select-none text-[9.5px] font-bold text-emerald-500 font-sans uppercase flex items-center gap-1 list-none hover:text-emerald-400">
                                            <span class="inline-block transition-transform group-open/friends:rotate-90 text-[8px] font-black">&gt;</span>
                                            <span>🤝 Friends Circle</span>
                                        </summary>
                                        <div class="space-y-1 pl-1.5 border-l border-emerald-500/20 mt-1">
                                            ${this.renderTreeMembers(selectedDev, 'friends')}
                                        </div>
                                    </details>
                                </div>
                            </div>

                            <!-- Interactive ACL Grants Panel -->
                            <div class="surface-theme p-2 rounded-xl border space-y-1 font-sans text-xs select-text">
                                <div class="text-[9.5px] font-bold text-accent-theme uppercase tracking-wider font-sans border-b border-subtle-theme pb-1">
                                    🛡️ Access Controls (ACL)
                                </div>
                                <div class="space-y-1 pt-0.5">
                                    <div class="flex items-center justify-between p-1 px-1.5 rounded-lg bg-card-subtle border border-subtle-theme">
                                        <span class="text-[9.5px] font-bold text-primary-theme">Live Tracking</span>
                                        <label class="switch-toggle switch-sm">
                                            <input type="checkbox" class="du-switch-acl" data-agent-id="${selectedDev.agentId}" data-flag="acl_live_tracking" ${selectedDev.acl.liveTracking ? 'checked' : ''}>
                                            <span class="switch-slider"></span>
                                        </label>
                                    </div>
                                    <div class="flex items-center justify-between p-1 px-1.5 rounded-lg bg-card-subtle border border-subtle-theme">
                                        <span class="text-[9.5px] font-bold text-primary-theme">Geofence Alerts</span>
                                        <label class="switch-toggle switch-sm">
                                            <input type="checkbox" class="du-switch-acl" data-agent-id="${selectedDev.agentId}" data-flag="acl_geofence" ${selectedDev.acl.geofence ? 'checked' : ''}>
                                            <span class="switch-slider"></span>
                                        </label>
                                    </div>
                                    <div class="flex items-center justify-between p-1 px-1.5 rounded-lg bg-card-subtle border border-subtle-theme">
                                        <span class="text-[9.5px] font-bold text-primary-theme">Route History</span>
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
                    <div class="card-theme p-2 space-y-1.5 shadow-sm flex flex-col w-full xl:w-[275px] xl:shrink-0">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-1 shrink-0">
                            <div class="flex items-center gap-1.5 min-w-0">
                                <span class="text-[11px] font-black uppercase text-accent-theme tracking-wider">4. SafeZones</span>
                                <span class="badge-status badge-accent text-[7.5px] px-1.5 py-0.2 font-mono">TIER 2/3</span>
                            </div>
                            <span class="badge-status badge-accent text-[7.5px] px-1.5 py-0.2 font-mono">C:${createdZonesList.length} | A:${assignedZonesList.length}</span>
                        </div>

                        <div class="space-y-1.5 overflow-y-auto custom-scrollbar max-h-[560px] pr-0.5">
                            <!-- Section 1: Created Safe Zones -->
                            <div class="surface-theme p-2 rounded-xl border space-y-1 select-text">
                                <div class="text-[9.5px] font-bold text-accent-theme uppercase font-sans flex items-center justify-between border-b border-subtle-theme pb-1">
                                    <span>Section 1: Created</span>
                                    <span class="text-[8.5px] font-mono text-muted-theme">(${createdZonesList.length})</span>
                                </div>
                                <div class="space-y-1 max-h-[120px] overflow-y-auto custom-scrollbar">
                                    ${this.renderCreatedSafeZonesHTML(selectedDev, createdZonesList)}
                                </div>
                            </div>

                            <!-- Section 2: Assigned Safe Zones -->
                            <div class="surface-theme p-2 rounded-xl border space-y-1 select-text">
                                <div class="text-[9.5px] font-bold text-warning-theme uppercase font-sans flex items-center justify-between border-b border-subtle-theme pb-1">
                                    <span>Section 2: Assigned</span>
                                    <span class="text-[8.5px] font-mono text-muted-theme">(${assignedZonesList.length})</span>
                                </div>
                                <div class="space-y-1 max-h-[120px] overflow-y-auto custom-scrollbar">
                                    ${this.renderAssignedSafeZonesHTML(selectedDev, assignedZonesList)}
                                </div>
                            </div>

                            <!-- Live Telemetry & Battery Diagnostics (Tier 3) -->
                            <div class="surface-theme p-2 rounded-xl border space-y-1 font-mono text-xs select-text">
                                <div class="text-[9.5px] font-bold text-success-theme uppercase font-sans border-b border-subtle-theme pb-1 flex items-center justify-between">
                                    <span>⚡ Live Hardware Pulse</span>
                                    <span class="badge-status ${selectedDev.isOnline ? 'badge-success' : 'badge-danger'} text-[7.5px] px-1.5 py-0.2">${selectedDev.isOnline ? 'ONLINE' : 'OFFLINE'}</span>
                                </div>
                                <div class="space-y-1 text-[9.5px]">
                                    <div class="flex justify-between py-0.5 border-b border-subtle-theme">
                                        <span class="text-secondary-theme">Battery:</span> 
                                        <span class="font-bold ${selectedDev.battery <= 20 ? 'text-danger-theme' : 'text-success-theme'}">${selectedDev.battery >= 0 ? `${selectedDev.battery}% ${selectedDev.isCharging ? '⚡' : ''}` : 'N/A'}</span>
                                    </div>
                                    <div class="flex justify-between py-0.5 border-b border-subtle-theme">
                                        <span class="text-secondary-theme">Network:</span> 
                                        <span class="text-primary-theme font-bold truncate">${selectedDev.networkType}</span>
                                    </div>
                                    <div class="flex justify-between py-0.5 border-b border-subtle-theme">
                                        <span class="text-secondary-theme">GPS:</span> 
                                        <span class="${selectedDev.isMockGps ? 'text-danger-theme font-bold' : 'text-success-theme'}">${selectedDev.isMockGps ? '⚠️ Mock' : '✅ Genuine'}</span>
                                    </div>
                                    <div class="flex justify-between py-0.5">
                                        <span class="text-secondary-theme">Last Seen:</span> 
                                        <span class="text-accent-theme font-sans font-bold">${selectedDev.getFormattedTimeDelta()}</span>
                                    </div>
                                </div>
                            </div>

                            <!-- Transactional Geofence Notification Log (Tier 4) -->
                            <div class="surface-theme p-2 rounded-xl border space-y-1 select-text">
                                <div class="text-[9.5px] font-bold text-secondary-theme uppercase font-sans border-b border-subtle-theme pb-1">Geofence Event Log</div>
                                <div class="space-y-1 text-[8.5px] font-mono max-h-[100px] overflow-y-auto custom-scrollbar">
                                    ${this.renderNotificationItems(selectedDev, rawNode)}
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- PANE 5: TIER 4 - TRANSACTIONAL LOCATION FIX & TIME-SERIES MATRIX -->
                    <div id="pane-5-container" class="card-theme p-2 space-y-1.5 shadow-sm flex flex-col w-full xl:w-[320px] xl:shrink-0">
                        ${this.renderPane5HTML(selectedDev, rawNode)}
                    </div>

                    ` : ''}

                </div>
            </div>
        `;

        this.bindEvents();

        // 🔗 SSOT Connection: Asynchronously fetch historical fixes from cloud database
        if (selectedDev && selectedDev.agentId && (this._lastLoadedHistoryAgentId !== selectedDev.agentId || this.historyBreadcrumbs.length === 0)) {
            this._lastLoadedHistoryAgentId = selectedDev.agentId;
            this.loadHistoricalTelemetry(selectedDev.agentId);
        }
    }

    /**
     * Renders Device Roster list with optional dynamic grouping (OOPS Group By Hierarchy)
     */
    renderRosterList(filteredDevices = []) {
        const renderItem = (dev) => {
            const ragColor = dev.getRagColor();
            const isSel = dev.agentId === this.selectedAgentId;
            const recency = typeof dev.getRecencyTier === 'function' ? dev.getRecencyTier() : { badgeClass: 'badge-status badge-accent', label: 'TELEMETRY' };
            const primaryDeviceName = (dev.brand && dev.model && dev.model !== 'Device') ? `${dev.brand} ${dev.model}` : (dev.deviceName || dev.model || 'Android Device');

            // Detect Duplicate Twin (same persona name or same silicon hash)
            const twins = this.devices.filter(d => (d.agentId !== dev.agentId) && ((d.siliconHash && d.siliconHash === dev.siliconHash) || (d.name && dev.name && d.name.toLowerCase() === dev.name.toLowerCase())));
            let twinBadge = '';
            if (twins.length > 0) {
                const allCopies = [dev, ...twins];
                const newestCopy = allCopies.reduce((prev, curr) => (curr.lastSeen > prev.lastSeen ? curr : prev), allCopies[0]);
                const isActive = (dev.agentId === newestCopy.agentId);
                twinBadge = isActive
                    ? `<span class="badge-status badge-success text-[7px] px-1 py-0.2 font-bold shrink-0">ACTIVE</span>`
                    : `<span class="badge-status badge-warning text-[7px] px-1 py-0.2 font-bold shrink-0">STALE (DUP)</span>`;
            }

            return `
                <div class="du-device-item cursor-pointer p-1.5 rounded-lg border ${isSel ? 'border-sky-500 bg-sky-500/10 shadow-sm' : 'border-subtle-theme bg-card-subtle hover:bg-card-hover'} transition-all flex items-center gap-2 select-text"
                     data-agent-id="${dev.agentId}">
                    <div class="relative shrink-0">
                        <img src="${dev.getAvatarUrl()}" alt="${dev.name}" 
                             class="w-7 h-7 rounded-full object-cover border-2 shadow-sm" 
                             style="border-color: ${ragColor};">
                        <span class="absolute bottom-0 right-0 w-2 h-2 rounded-full border border-surface" style="background-color: ${ragColor}"></span>
                    </div>
                    <div class="min-w-0 flex-1">
                        <div class="flex items-center justify-between gap-1">
                            <div class="flex items-center gap-1 min-w-0">
                                <h4 class="text-[11.5px] font-bold truncate text-primary-theme min-w-0" title="${dev.name}">
                                    ${dev.name}
                                </h4>
                                ${twinBadge}
                            </div>
                            <span class="${recency.badgeClass} text-[7.5px] px-1 py-0.2 font-bold shrink-0">${recency.label}</span>
                        </div>
                        <div class="text-[9px] text-secondary-theme truncate font-mono flex items-center justify-between mt-0.5 select-text">
                            <span class="truncate min-w-0 select-text">
                                <span class="text-accent-theme font-medium">${primaryDeviceName}</span> • <span class="copyable-badge font-mono hover:text-sky-300" data-copy="${dev.agentId}" title="Click to copy ID">ID:${dev.agentId}</span>
                            </span>
                            <span class="text-accent-theme font-sans shrink-0 ml-1 text-[8.5px]">${dev.getFormattedTimeDelta()}</span>
                        </div>
                    </div>
                </div>
            `;
        };

        if (this.groupBy === 'none') {
            return filteredDevices.map(renderItem).join('');
        }

        // Dynamic Grouping
        const groups = new Map();
        for (const dev of filteredDevices) {
            let groupKey = 'Default Circle';
            if (this.groupBy === 'group') {
                groupKey = dev.activeGroup || (dev.raw && dev.raw.active_group) || (dev.raw && dev.raw.Identity && dev.raw.Identity.active_group) || 'Default Circle';
            } else if (this.groupBy === 'role') {
                groupKey = (dev.roleFamily && dev.roleFamily !== 'None') ? dev.roleFamily : (dev.role || 'Member');
            } else if (this.groupBy === 'status') {
                groupKey = dev.isSos ? '🚨 EMERGENCY SOS' : (dev.isOnline ? '🟢 ACTIVE (LIVE)' : (dev.isStale ? '🔴 INACTIVE (>6H)' : '🟡 INACTIVE (<6H)'));
            }
            if (!groups.has(groupKey)) {
                groups.set(groupKey, []);
            }
            groups.get(groupKey).push(dev);
        }

        let html = '';
        for (const [groupName, groupDevs] of groups.entries()) {
            html += `
                <div class="du-roster-group mb-1">
                    <div class="flex items-center justify-between px-1.5 py-0.5 bg-surface-theme/80 border-b border-subtle-theme rounded text-[9px] font-bold text-accent-theme">
                        <span class="truncate">📁 ${groupName}</span>
                        <span class="badge-status badge-accent text-[7.5px] px-1 py-0.2">${groupDevs.length}</span>
                    </div>
                    <div class="space-y-1 mt-1 pl-1 border-l-2 border-accent-theme/20">
                        ${groupDevs.map(renderItem).join('')}
                    </div>
                </div>
            `;
        }
        return html;
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
        const relsMap = device.relationships || rawNode.relationships || {};
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
        // If device has no created or assigned safe zones, do not display unrelated transition logs!
        const createdCount = (this.safeZones || []).filter(z => z.creator_id === device.agentId || z.agentId === device.agentId || z.creatorId === device.agentId).length;
        const assignedCount = (this.safeZones || []).filter(z => {
            const assignedDevices = z.assigned_devices || z.devices || z.connectedDevices || [];
            return Array.isArray(assignedDevices) ? assignedDevices.includes(device.agentId) : (assignedDevices[device.agentId] !== undefined);
        }).length;

        if (createdCount === 0 && assignedCount === 0) {
            return `<div class="text-[10px] text-muted-theme italic py-1 font-mono">No active geofence perimeters assigned.</div>`;
        }

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

                        <div class="flex items-center gap-1 shrink-0" title="Stationary noise deadband">
                            <label class="switch-toggle switch-sm">
                                <input type="checkbox" id="switch-deadband-filter" ${this.deadbandEnabled ? 'checked' : ''}>
                                <span class="switch-slider"></span>
                            </label>
                            <span class="text-[7.5px] font-mono font-bold text-sky-400">${Math.round(window.app?.settingsModel?.deadbandNoiseMeters ?? 15)}m</span>
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

        // Apply Deadband Noise Filter (Suppress stationary points < deadbandThresholdM)
        const deadbandThresholdM = Math.max(0, parseFloat(window.app?.settingsModel?.deadbandNoiseMeters ?? 15));
        if (this.deadbandEnabled && points.length > 1) {
            const chron = [...points].sort((a, b) => a.timestamp - b.timestamp);
            const deadbanded = [chron[0]];
            for (let i = 1; i < chron.length; i++) {
                const prev = deadbanded[deadbanded.length - 1];
                const curr = chron[i];
                const distM = TelemetryValidator.calculateDistanceMeters(prev.latitude, prev.longitude, curr.latitude, curr.longitude);
                const speedKmh = curr.speed > 50 ? curr.speed : curr.speed * 3.6;

                // Keep point only if user moved > deadbandThresholdM or moving speed > 1.5 km/h, or if it is the latest live point
                if (distM >= deadbandThresholdM || speedKmh >= 1.5 || curr.isLive) {
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
                    ${(curr.address && /[a-zA-Z]/.test(curr.address) && !curr.address.includes(curr.latitude.toFixed(3))) ? `
                        <div class="text-secondary-theme text-[8px] truncate select-text" title="${curr.address}">🏠 ${curr.address}</div>
                    ` : ''}
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
            const hasItemNamedAddr = item.address && /[a-zA-Z]/.test(item.address) && !item.address.includes(item.latitude.toFixed(3));

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
                        ${hasItemNamedAddr ? `
                            <span class="truncate ml-1 text-muted-theme text-[8px]" title="${item.address}">🏠 ${item.address}</span>
                        ` : ''}
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

        UIControls.showRightSlideDrawer({
            title: `Telemetry: ${device.name}`,
            subtitle: `Date: ${this.selectedDate} • Points: ${points.length}`,
            icon: "⚡",
            fields: [
                { label: "Target Agent", value: device.agentId },
                { label: "Selected Date", value: this.selectedDate },
                { label: "Active Points", value: String(points.length) },
                { label: "RAG Status", value: device.ragLabel || "ACTIVE" },
                { label: "Assigned Circle", value: device.activeGroup || "Default Circle" },
                { label: "Battery Level", value: `${device.batteryPct || 100}%` }
            ],
            actions: [
                { id: 'replay_trail', label: 'Replay Trail', variant: 'primary' },
                { id: 'downsample_rdp', label: 'RDP Downsample', variant: 'secondary' },
                { id: 'export_geojson', label: 'GeoJSON', variant: 'secondary' },
                { id: 'export_csv', label: 'Export CSV', variant: 'secondary' },
                { id: 'purge_history', label: 'Purge (>30d)', variant: 'danger' }
            ],
            onAction: async (actionId) => {
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
     * SSOT Connector: Loads historical telemetry fixes from /api/device-history and updates panels
     * @param {string} agentId Canonical hardware silicon agent ID
     */
    async loadHistoricalTelemetry(agentId) {
        if (!agentId) return;
        try {
            const queryParams = new URLSearchParams({
                agentId: agentId,
                deadband: this.deadbandEnabled ? "true" : "false",
                limit: "1000"
            });
            if (this.selectedDate) {
                queryParams.set("startDate", this.selectedDate);
                queryParams.set("endDate", this.selectedDate);
            }
            if (this.selectedTimeWindow && this.selectedTimeWindow !== 'all') {
                if (this.selectedTimeWindow === '1h') queryParams.set("startTime", new Date(Date.now() - 3600000).toTimeString().slice(0, 5));
                else if (this.selectedTimeWindow === '6h') queryParams.set("startTime", new Date(Date.now() - 21600000).toTimeString().slice(0, 5));
                else if (this.selectedTimeWindow === 'morning') { queryParams.set("startTime", "06:00"); queryParams.set("endTime", "12:00"); }
                else if (this.selectedTimeWindow === 'evening') { queryParams.set("startTime", "18:00"); queryParams.set("endTime", "23:59"); }
            }

            const [histRes, datesRes] = await Promise.all([
                (window.fetchApi ? window.fetchApi(`/api/device-history?${queryParams.toString()}`) : fetch(`/api/device-history?${queryParams.toString()}`).then(r => r.json()).catch(() => null)),
                (window.fetchApi ? window.fetchApi(`/api/history-dates?agentId=${encodeURIComponent(agentId)}`) : fetch(`/api/history-dates?agentId=${encodeURIComponent(agentId)}`).then(r => r.json()).catch(() => null))
            ]);

            if (histRes && Array.isArray(histRes.breadcrumbs)) {
                this.historyBreadcrumbs = histRes.breadcrumbs;
            } else if (Array.isArray(histRes)) {
                this.historyBreadcrumbs = histRes;
            }

            if (datesRes && Array.isArray(datesRes.dates)) {
                this.availableHistoryDates = datesRes.dates;
            }

            this.updatePane5Content();
        } catch (err) {
            console.warn('[DeviceUserView] Failed to load SSOT telemetry:', err.message);
        }
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
                const centerWaypoint = (attempts = 0) => {
                    const mapInstance = window.appInstance?.mapController?.mapView?.map;
                    if (mapInstance && typeof mapInstance.setView === 'function') {
                        mapInstance.setView([curr.latitude, curr.longitude], 17);
                        if (window.MatrixApp && window.MatrixApp.focusDevice) {
                            window.MatrixApp.focusDevice(selectedDev.agentId);
                        }
                    } else if (attempts < 15) {
                        setTimeout(() => centerWaypoint(attempts + 1), 100);
                    }
                };
                centerWaypoint();
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
        // Switch to Active Twin Device Action
        document.querySelectorAll('.du-switch-to-active').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const targetId = btn.getAttribute('data-target-id');
                if (targetId && targetId !== this.selectedAgentId) {
                    this.selectedAgentId = targetId;
                    this.historyBreadcrumbs = [];
                    this.availableHistoryDates = [];
                    this.flipperIndex = 0;
                    this.expandedFixIds.clear();
                    this.render(this.devices, targetId, this.safeZones);
                    if (this.streamMode === 'history') {
                        this.loadHistoricalTelemetry(targetId);
                    }
                }
            });
        });

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

        // 4c. Open Decommission / Delete Everywhere Modal (Archive vs CleanSlate)
        document.getElementById('btn-du-delete-device')?.addEventListener('click', () => {
            const selectedDev = this.devices.find(d => d.agentId === this.selectedAgentId) || this.devices[0];
            this.deleteModal.show(selectedDev, this.devices);
        });

        // 4d. Archive Log Checkbox Toggle
        document.getElementById('du-check-archive-log')?.addEventListener('change', async (e) => {
            this.showArchivedLog = e.target.checked;
            if (this.showArchivedLog && (!this.archivedDevices || this.archivedDevices.length === 0)) {
                await this.loadArchivedDevices();
            }
            this.render(this.devices, this.selectedAgentId, this.safeZones);
        });

        // 4e. Restore Device from Archive Action
        document.querySelectorAll('.btn-restore-archive').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const agentId = btn.getAttribute('data-agent-id');
                const name = btn.getAttribute('data-name');
                await this.restoreArchivedDevice(agentId, name);
            });
        });

        // 4f. Direct CleanSlate for Archived Device
        document.querySelectorAll('.btn-cleanslate-archive').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const agentId = btn.getAttribute('data-agent-id');
                const name = btn.getAttribute('data-name');
                this.deleteModal.show({ agentId, name, deviceName: name }, this.devices, 'CLEANSLATE');
            });
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

        // 5a. Dynamic Group By Change
        document.getElementById('du-group-by-select')?.addEventListener('change', (e) => {
            this.groupBy = e.target.value;
            this.render(this.devices, this.selectedAgentId, this.safeZones);
        });

        // 5b. Edit Profile Action (Header Bar, Pane 2 Dossier, and Pane 3 Persona)
        const triggerEditProfile = () => {
            const selectedDev = this.devices.find(d => d.agentId === this.selectedAgentId) || this.devices[0];
            if (selectedDev) {
                this.editModal.show(selectedDev);
            }
        };
        document.getElementById('btn-edit-profile-dossier')?.addEventListener('click', triggerEditProfile);
        document.getElementById('btn-du-edit-device')?.addEventListener('click', triggerEditProfile);
        document.getElementById('btn-du-edit-persona')?.addEventListener('click', triggerEditProfile);
        document.querySelectorAll('.du-sim-row').forEach(row => row.addEventListener('click', triggerEditProfile));

        // 5c. Remote System OTA Push Action: Presents Option 1 (Screen & Web Control) vs Option 2 (Background Script)
        const btnPushOta = document.getElementById('btn-push-ota-version');
        if (btnPushOta) {
            btnPushOta.addEventListener('click', async (e) => {
                e.preventDefault();
                e.stopPropagation();

                const agentId = btnPushOta.getAttribute('data-agent-id') || this.selectedAgentId;
                if (!agentId) {
                    FeedbackDialog.showToast("Please select a device node first.", "warning");
                    return;
                }

                const targetVersion = btnPushOta.getAttribute('data-target-version') || '1.0.13';
                const targetCode = btnPushOta.getAttribute('data-target-code') || '87';
                const downloadUrl = btnPushOta.getAttribute('data-download-url') || '';
                const selectedDev = this.devices.find(d => d.agentId === agentId) || { agentId, name: agentId };

                UIControls.notify(`🚀 Pushing update V${targetVersion} to ${selectedDev.name || selectedDev.agentId} in background (Mobile Data / Wi-Fi)...`, 'info', { duration: 4000 });
                btnPushOta.disabled = true;
                btnPushOta.innerHTML = `<span class="text-[11px] animate-spin">⏳</span><span>Pushing Update...</span>`;

                try {
                    const res = await this.deviceController.pushDeviceUpdate(selectedDev.agentId, {
                        targetVersion,
                        targetCode,
                        downloadUrl,
                        mode: 'BACKGROUND_BEST_OPTION'
                    });
                    if (res && res.status === 'SUCCESS') {
                        UIControls.notify(`✅ Update V${targetVersion} pushed to ${selectedDev.name || selectedDev.agentId} over active internet! Background installer active.`, 'success', { duration: 6000 });
                        btnPushOta.classList.remove('from-amber-500/25', 'via-sky-500/25');
                        btnPushOta.classList.add('bg-emerald-600/30', 'text-emerald-300', 'border-emerald-500');
                        btnPushOta.innerHTML = `<span>🛡️</span><span>Script Running (Background Engine)</span>`;
                    } else {
                        throw new Error(res?.message || 'Failed to dispatch OTA');
                    }
                } catch (err) {
                    console.error('[DeviceUserView] Background OTA push failed:', err);
                    UIControls.notify(`Push failed: ${err.message}`, 'danger', { duration: 5000 });
                    btnPushOta.disabled = false;
                    btnPushOta.innerHTML = `<span>🚀</span><span>Retry Push & Install</span>`;
                }
            });
        }

        const btnOpenOtaChooser = document.getElementById('btn-open-ota-chooser');
        if (btnOpenOtaChooser) {
            btnOpenOtaChooser.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();

                const agentId = btnOpenOtaChooser.getAttribute('data-agent-id') || this.selectedAgentId;
                if (!agentId) {
                    FeedbackDialog.showToast("Please select a device node first.", "warning");
                    return;
                }

                const targetVersion = btnPushOta?.getAttribute('data-target-version') || (this.masterRelease?.version_name) || '1.0.13';
                const targetCode = btnPushOta?.getAttribute('data-target-code') || (this.masterRelease?.version_code) || '89';
                const downloadUrl = btnPushOta?.getAttribute('data-download-url') || (this.masterRelease?.download_url_versioned || this.masterRelease?.download_url_github) || '';
                const selectedDev = this.devices.find(d => d.agentId === agentId) || { agentId, name: agentId };

                const installedVer = (typeof DeviceModel !== 'undefined' && DeviceModel.parseVersion)
                    ? DeviceModel.parseVersion(selectedDev.raw || selectedDev, selectedDev)
                    : { versionName: (selectedDev.versionName || '1.0.13').replace(/^V/i, ''), versionCode: selectedDev.versionCode || 86 };
                const currentMaster = this.masterRelease || {
                    version_name: targetVersion,
                    version_code: targetCode,
                    package_size_formatted: '80.6 MB'
                };

                this.showOtaDeploymentChooser(selectedDev, {
                    targetVersion,
                    targetCode,
                    downloadUrl,
                    installedVerName: installedVer.versionName,
                    installedVerCode: installedVer.versionCode,
                    sizeFormatted: currentMaster.package_size_formatted || '80.6 MB'
                });
            });
        }


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
                    const centerZone = (attempts = 0) => {
                        const mapInstance = window.appInstance?.mapController?.mapView?.map;
                        if (mapInstance && typeof mapInstance.setView === 'function') {
                            mapInstance.setView([lat, lng], 16);
                        } else if (attempts < 15) {
                            setTimeout(() => centerZone(attempts + 1), 100);
                        }
                    };
                    centerZone();
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

    /**
     * 🚀 Displays Tactical Choice Modal: Option 1 (Screen & Web Control) vs Option 2 (Background Script)
     */
    showOtaDeploymentChooser(selectedDev, { targetVersion, targetCode, downloadUrl, installedVerName, installedVerCode, sizeFormatted }) {
        const modal = document.createElement('div');
        modal.id = 'ota-strategy-modal';
        modal.className = 'fixed inset-0 z-[1200] flex items-center justify-center bg-black/85 backdrop-blur-md p-4 animate-fade-in font-sans';

        modal.innerHTML = `
            <div class="bg-slate-900 border border-white/10 rounded-2xl w-full max-w-xl shadow-2xl overflow-hidden text-white flex flex-col max-h-[90vh]">
                <!-- Modal Header -->
                <div class="p-4 border-b border-white/10 flex items-center justify-between bg-slate-950/80">
                    <div class="flex items-center gap-2.5">
                        <div class="w-9 h-9 rounded-xl bg-gradient-to-tr from-sky-500/20 to-accent-theme/20 border border-sky-500/30 flex items-center justify-center text-lg">
                            🚀
                        </div>
                        <div>
                            <h3 class="text-sm font-black tracking-tight text-white uppercase font-sans">System OTA Deployment Matrix</h3>
                            <p class="text-[10px] text-slate-400 font-mono">Target Node: <span class="text-sky-300 font-bold">${selectedDev.name || selectedDev.agentId}</span> [${selectedDev.agentId}]</p>
                        </div>
                    </div>
                    <button id="modal-ota-close" class="text-slate-400 hover:text-white text-sm font-mono px-2 py-1 rounded-lg hover:bg-white/10 transition-colors">✕</button>
                </div>

                <!-- Build Comparison Bar -->
                <div class="bg-slate-950/50 px-4 py-2.5 border-b border-white/5 flex items-center justify-between text-[11px] font-mono">
                    <div class="flex items-center gap-2">
                        <span class="text-slate-400">Installed:</span>
                        <span class="text-slate-200 font-bold">V${installedVerName} (Build ${installedVerCode})</span>
                    </div>
                    <span class="text-sky-400 font-bold">➔</span>
                    <div class="flex items-center gap-2">
                        <span class="text-slate-400">Target Build:</span>
                        <span class="text-emerald-400 font-bold">V${targetVersion} (Build ${targetCode})</span>
                        <span class="text-[9px] px-1.5 py-0.5 rounded bg-sky-950 border border-sky-500/30 text-sky-300">${sizeFormatted}</span>
                    </div>
                </div>

                <!-- Strategy Chooser Cards -->
                <div class="p-4 space-y-3 overflow-y-auto font-sans">
                    <div class="text-[11px] text-slate-300 flex items-center justify-between">
                        <span>Choose deployment execution path for node <span class="font-bold text-sky-400 font-mono">[${selectedDev.agentId}]</span>:</span>
                        <span class="text-[9px] px-2 py-0.5 rounded font-mono font-bold bg-slate-800 border border-slate-700 text-sky-300">
                            Routing: ${selectedDev.networkPreference === 'WIFI_ONLY' ? '📡 Wi-Fi Only' : (selectedDev.networkPreference === 'MOBILE_DATA_ONLY' ? '📶 Mobile Data Only' : '⚡ Auto (Mobile / Wi-Fi)')}
                        </span>
                    </div>

                    <!-- Option 1 Card -->
                    <div id="card-option-1-interactive" class="group relative p-3.5 rounded-xl border border-sky-500/30 bg-gradient-to-r from-sky-950/40 via-slate-900 to-slate-950 hover:border-sky-400 hover:shadow-lg hover:shadow-sky-500/10 transition-all cursor-pointer">
                        <div class="flex items-start justify-between gap-3">
                            <div class="w-10 h-10 rounded-xl bg-sky-500/20 border border-sky-500/40 flex items-center justify-center text-xl shrink-0 mt-0.5">
                                🖥️
                            </div>
                            <div class="flex-1 min-w-0">
                                <div class="flex items-center gap-2 mb-1">
                                    <span class="text-xs font-black text-sky-300 uppercase tracking-wide">Option 1: Device Screen & Webpage Controlled</span>
                                    <span class="text-[8.5px] px-1.5 py-0.2 rounded font-mono font-bold bg-sky-500/20 text-sky-200 border border-sky-500/30">INTERACTIVE</span>
                                </div>
                                <p class="text-[10.5px] text-slate-300 leading-relaxed mb-2">
                                    Pops up the installation screen directly on the user's phone, and opens the live <span class="text-sky-300 font-semibold">Web Installation Controller</span> to monitor stages and send remote commands in real-time.
                                </p>
                                <div class="flex items-center gap-2 text-[9.5px] text-slate-400 font-mono">
                                    <span>• Live Stage Stepper</span>
                                    <span>• Re-Open Screen Button</span>
                                    <span>• Status Heartbeat</span>
                                    <span>• Session Abort</span>
                                </div>
                            </div>
                        </div>
                        <div class="mt-3 pt-2.5 border-t border-sky-500/20 flex justify-end">
                            <button id="btn-deploy-option-1" class="px-4 py-1.5 rounded-lg text-xs font-bold font-mono bg-sky-600 hover:bg-sky-500 active:scale-95 text-white shadow-md shadow-sky-600/30 transition-all flex items-center gap-1.5">
                                <span>📱</span>
                                <span>Deploy Option 1 (Open Screen & Web Control)</span>
                            </button>
                        </div>
                    </div>

                    <!-- Option 2 Card -->
                    <div id="card-option-2-background" class="group relative p-3.5 rounded-xl border border-emerald-500/30 bg-gradient-to-r from-emerald-950/40 via-slate-900 to-slate-950 hover:border-emerald-400 hover:shadow-lg hover:shadow-emerald-500/10 transition-all cursor-pointer">
                        <div class="flex items-start justify-between gap-3">
                            <div class="w-10 h-10 rounded-xl bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-xl shrink-0 mt-0.5">
                                ⚙️
                            </div>
                            <div class="flex-1 min-w-0">
                                <div class="flex items-center gap-2 mb-1">
                                    <span class="text-xs font-black text-emerald-300 uppercase tracking-wide">Option 2: Run Background Script (Best Option Selected)</span>
                                    <span class="text-[8.5px] px-1.5 py-0.2 rounded font-mono font-bold bg-emerald-500/20 text-emerald-200 border border-emerald-500/30">UNATTENDED</span>
                                </div>
                                <p class="text-[10.5px] text-slate-300 leading-relaxed mb-2">
                                    Executes background installation script on the device, automatically choosing the optimal engine: <span class="text-emerald-300 font-semibold">API 31+ Unattended Session</span>, <span class="text-emerald-300 font-semibold">Root PM Shell</span>, or <span class="text-emerald-300 font-semibold">Idle Worker</span> with zero screen interruption.
                                </p>
                                <div class="flex items-center gap-2 text-[9.5px] text-slate-400 font-mono">
                                    <span>• Autonomous Engine Selection</span>
                                    <span>• Zero User Interruption</span>
                                    <span>• Data Snapshot Guard</span>
                                </div>
                            </div>
                        </div>
                        <div class="mt-3 pt-2.5 border-t border-emerald-500/20 flex justify-end">
                            <button id="btn-deploy-option-2" class="px-4 py-1.5 rounded-lg text-xs font-bold font-mono bg-emerald-600 hover:bg-emerald-500 active:scale-95 text-white shadow-md shadow-emerald-600/30 transition-all flex items-center gap-1.5">
                                <span>🛡️</span>
                                <span>Deploy Option 2 (Run Background Script)</span>
                            </button>
                        </div>
                    </div>
                </div>

                <!-- Footer -->
                <div class="p-3 border-t border-white/10 bg-slate-950 flex items-center justify-between text-[10px] text-slate-400 font-mono">
                    <span>Closed-Loop OTA Security Sentinel • jDroid-X</span>
                    <button id="modal-ota-cancel" class="px-3 py-1.5 rounded-lg hover:bg-white/5 text-slate-400 hover:text-white transition-colors">
                        Cancel
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        const closeChooser = () => modal.remove();
        modal.querySelector('#modal-ota-close')?.addEventListener('click', closeChooser);
        modal.querySelector('#modal-ota-cancel')?.addEventListener('click', closeChooser);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeChooser();
        });

        // Trigger Option 1: Open Device Screen & Web Console
        const handleOption1 = async () => {
            closeChooser();
            UIControls.notify(`🚀 Dispatching Option 1 (Screen & Web Control) to ${selectedDev.agentId}...`, 'info', { duration: 3500 });
            try {
                const res = await this.deviceController.pushDeviceUpdate(selectedDev.agentId, {
                    targetVersion,
                    targetCode,
                    downloadUrl,
                    mode: 'INTERACTIVE_SCREEN'
                });
                if (res && res.status === 'SUCCESS') {
                    this.showWebControlledOtaConsole(selectedDev, { targetVersion, targetCode, downloadUrl });
                } else {
                    throw new Error(res?.message || 'Failed to dispatch OTA');
                }
            } catch (err) {
                console.error('[DeviceUserView] Option 1 push failed:', err);
                UIControls.notify(`Push failed: ${err.message}`, 'danger', { duration: 5000 });
            }
        };

        // Trigger Option 2: Run Background Script (Best Option Selected)
        const handleOption2 = async () => {
            closeChooser();
            UIControls.notify(`⚙️ Dispatching Option 2 (Background Script) to ${selectedDev.agentId}...`, 'info', { duration: 3500 });
            try {
                const res = await this.deviceController.pushDeviceUpdate(selectedDev.agentId, {
                    targetVersion,
                    targetCode,
                    downloadUrl,
                    mode: 'BACKGROUND_BEST_OPTION'
                });
                if (res && res.status === 'SUCCESS') {
                    UIControls.notify(`✅ Background installation script active on ${selectedDev.agentId}! Best option selected autonomously.`, 'success', { duration: 6000 });
                    const btn = document.getElementById('btn-push-ota-version');
                    if (btn) {
                        btn.classList.remove('from-amber-500/25', 'via-sky-500/25');
                        btn.classList.add('bg-emerald-600/30', 'text-emerald-300', 'border-emerald-500');
                        btn.innerHTML = `<span>🛡️</span><span>Script Running (Background Engine)</span>`;
                    }
                } else {
                    throw new Error(res?.message || 'Failed to dispatch OTA');
                }
            } catch (err) {
                console.error('[DeviceUserView] Option 2 push failed:', err);
                UIControls.notify(`Push failed: ${err.message}`, 'danger', { duration: 5000 });
            }
        };

        modal.querySelector('#btn-deploy-option-1')?.addEventListener('click', handleOption1);
        modal.querySelector('#card-option-1-interactive')?.addEventListener('click', (e) => {
            if (e.target.id !== 'btn-deploy-option-1') handleOption1();
        });

        modal.querySelector('#btn-deploy-option-2')?.addEventListener('click', handleOption2);
        modal.querySelector('#card-option-2-background')?.addEventListener('click', (e) => {
            if (e.target.id !== 'btn-deploy-option-2') handleOption2();
        });
    }

    /**
     * 🖥️ Option 1: Live Interactive Webpage OTA Controller & Progress Monitor
     */
    showWebControlledOtaConsole(selectedDev, { targetVersion, targetCode, downloadUrl }) {
        if (this.activeInstallPoll) {
            clearInterval(this.activeInstallPoll);
            this.activeInstallPoll = null;
        }

        const modal = document.createElement('div');
        modal.id = 'web-ota-console-modal';
        modal.className = 'fixed inset-0 z-[1250] flex items-center justify-center bg-black/90 backdrop-blur-md p-4 animate-fade-in font-sans';

        modal.innerHTML = `
            <div class="bg-slate-900 border border-sky-500/30 rounded-2xl w-full max-w-2xl shadow-2xl overflow-hidden text-white flex flex-col max-h-[92vh]">
                <!-- Console Header -->
                <div class="p-4 border-b border-white/10 flex items-center justify-between bg-slate-950">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-xl bg-sky-500/20 border border-sky-400/40 flex items-center justify-center text-xl animate-pulse">
                            🖥️
                        </div>
                        <div>
                            <div class="flex items-center gap-2">
                                <h3 class="text-sm font-black tracking-tight text-white uppercase font-sans">OTA Installation Control Console</h3>
                                <span id="console-live-badge" class="text-[8.5px] px-2 py-0.5 rounded font-mono font-bold bg-sky-500/20 text-sky-300 border border-sky-500/40 animate-pulse">LIVE CONTROL</span>
                            </div>
                            <p class="text-[10px] text-slate-400 font-mono">
                                Node: <span class="text-sky-300 font-bold">${selectedDev.name || selectedDev.agentId}</span> [${selectedDev.agentId}] • Target: <span class="text-emerald-400 font-bold">V${targetVersion} (Build ${targetCode})</span>
                            </p>
                        </div>
                    </div>
                    <button id="btn-console-close" class="text-slate-400 hover:text-white text-sm font-mono px-2 py-1 rounded-lg hover:bg-white/10 transition-colors">✕</button>
                </div>

                <!-- 5-Stage Visual Stepper -->
                <div class="px-6 py-4 bg-slate-950/70 border-b border-white/5">
                    <div class="text-[9.5px] font-bold text-slate-400 uppercase tracking-wider mb-2 font-mono flex justify-between">
                        <span>Pipeline Progression</span>
                        <span id="stepper-stage-title" class="text-sky-400">STAGE 1: PUSH DISPATCHED</span>
                    </div>
                    <div class="grid grid-cols-5 gap-2 text-center text-[9px] font-mono">
                        <div id="step-1" class="p-2 rounded-lg border border-sky-500/40 bg-sky-950/40 text-sky-200 flex flex-col items-center gap-1 transition-all">
                            <span class="text-sm">🚀</span>
                            <span class="font-bold">1. Dispatched</span>
                        </div>
                        <div id="step-2" class="p-2 rounded-lg border border-white/10 bg-slate-950/40 text-slate-400 flex flex-col items-center gap-1 transition-all">
                            <span class="text-sm">📥</span>
                            <span class="font-bold">2. Download</span>
                        </div>
                        <div id="step-3" class="p-2 rounded-lg border border-white/10 bg-slate-950/40 text-slate-400 flex flex-col items-center gap-1 transition-all">
                            <span class="text-sm">🔍</span>
                            <span class="font-bold">3. Verified</span>
                        </div>
                        <div id="step-4" class="p-2 rounded-lg border border-white/10 bg-slate-950/40 text-slate-400 flex flex-col items-center gap-1 transition-all">
                            <span class="text-sm">📱</span>
                            <span class="font-bold">4. Screen Active</span>
                        </div>
                        <div id="step-5" class="p-2 rounded-lg border border-white/10 bg-slate-950/40 text-slate-400 flex flex-col items-center gap-1 transition-all">
                            <span class="text-sm">✅</span>
                            <span class="font-bold">5. Installed</span>
                        </div>
                    </div>
                </div>

                <!-- Live Status & Feedback Box -->
                <div class="p-4 space-y-3 overflow-y-auto">
                    <div class="p-3.5 rounded-xl border border-white/10 bg-slate-950/80 space-y-2">
                        <div class="flex items-center justify-between text-[10.5px]">
                            <span class="text-slate-400 font-mono">Device State:</span>
                            <span id="console-status-text" class="font-mono font-bold text-sky-300">DISPATCHED</span>
                        </div>
                        <div class="flex items-center justify-between text-[10.5px]">
                            <span class="text-slate-400 font-mono">Current Engine / Mode:</span>
                            <span id="console-engine-text" class="font-mono font-bold text-emerald-400">INTERACTIVE_SCREEN</span>
                        </div>
                        <div class="flex items-center justify-between text-[10.5px]">
                            <span class="text-slate-400 font-mono">Last Device Message:</span>
                            <span id="console-message-text" class="font-mono text-slate-200 text-right truncate max-w-[320px]">Waiting for device pickup...</span>
                        </div>
                    </div>

                    <!-- Telemetry Event Stream Log -->
                    <div>
                        <div class="text-[9.5px] font-bold text-slate-400 uppercase tracking-wider mb-1 font-mono">
                            Live Stream Log:
                        </div>
                        <div id="console-log-box" class="h-28 bg-black/60 border border-white/10 rounded-xl p-2.5 font-mono text-[9px] text-slate-300 overflow-y-auto space-y-1">
                            <div class="text-sky-400">[SYSTEM] Operator initiated Option 1: Device Screen & Web Control.</div>
                            <div class="text-slate-400">[DISPATCH] Command packet delivered to Firebase RTDB and high-priority FCM channel.</div>
                        </div>
                    </div>
                </div>

                <!-- Webpage Operator Remote Control Actions Bar -->
                <div class="p-3.5 border-t border-white/10 bg-slate-950 flex flex-wrap items-center justify-between gap-2">
                    <div class="flex items-center gap-2">
                        <button id="btn-ctrl-reopen-screen" class="px-3 py-1.5 rounded-lg text-[10px] font-mono font-bold bg-sky-600/30 hover:bg-sky-600 border border-sky-500/40 text-sky-200 hover:text-white transition-all flex items-center gap-1.5 active:scale-95" title="Re-launch native installer screen if user backed out on phone">
                            <span>📱</span>
                            <span>Re-Open Screen on Device</span>
                        </button>
                        <button id="btn-ctrl-ping-status" class="px-3 py-1.5 rounded-lg text-[10px] font-mono font-bold bg-slate-800 hover:bg-slate-700 border border-white/10 text-slate-300 hover:text-white transition-all flex items-center gap-1.5 active:scale-95" title="Ping device for immediate heartbeat">
                            <span>🛰️</span>
                            <span>Ping Status</span>
                        </button>
                        <button id="btn-ctrl-switch-bg" class="px-3 py-1.5 rounded-lg text-[10px] font-mono font-bold bg-emerald-950/40 hover:bg-emerald-600/30 border border-emerald-500/40 text-emerald-300 transition-all flex items-center gap-1.5 active:scale-95" title="Switch to autonomous background script">
                            <span>⚙️</span>
                            <span>Switch to Background</span>
                        </button>
                    </div>
                    <div>
                        <button id="btn-ctrl-abort" class="px-3 py-1.5 rounded-lg text-[10px] font-mono font-bold bg-red-950/40 hover:bg-red-600/40 border border-red-500/40 text-red-300 hover:text-white transition-all flex items-center gap-1.5 active:scale-95">
                            <span>🛑</span>
                            <span>Abort Session</span>
                        </button>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        const appendLog = (msg, colorClass = 'text-slate-300') => {
            const logBox = modal.querySelector('#console-log-box');
            if (logBox) {
                const row = document.createElement('div');
                row.className = colorClass;
                const timeStr = new Date().toTimeString().slice(0, 8);
                row.textContent = `[${timeStr}] ${msg}`;
                logBox.appendChild(row);
                logBox.scrollTop = logBox.scrollHeight;
            }
        };

        const updateStepper = (stepIndex, title) => {
            modal.querySelector('#stepper-stage-title').textContent = title;
            for (let i = 1; i <= 5; i++) {
                const el = modal.querySelector(`#step-${i}`);
                if (!el) continue;
                if (i < stepIndex) {
                    el.className = 'p-2 rounded-lg border border-emerald-500/40 bg-emerald-950/40 text-emerald-200 flex flex-col items-center gap-1 transition-all';
                } else if (i === stepIndex) {
                    el.className = 'p-2 rounded-lg border border-sky-400 bg-sky-950/60 text-sky-100 flex flex-col items-center gap-1 transition-all shadow-md shadow-sky-500/20 animate-pulse';
                } else {
                    el.className = 'p-2 rounded-lg border border-white/10 bg-slate-950/40 text-slate-400 flex flex-col items-center gap-1 transition-all';
                }
            }
        };

        // Bind Web Controller Buttons
        modal.querySelector('#btn-ctrl-reopen-screen')?.addEventListener('click', async () => {
            appendLog('Operator triggered: Re-Open Screen on Device', 'text-sky-300');
            try {
                await this.deviceController.controlDeviceInstall(selectedDev.agentId, 'OPEN_INSTALLER_SCREEN');
                UIControls.notify('Dispatched re-open screen command to device!', 'info');
            } catch (e) {
                appendLog(`Re-open screen failed: ${e.message}`, 'text-red-400');
            }
        });

        modal.querySelector('#btn-ctrl-ping-status')?.addEventListener('click', async () => {
            appendLog('Operator triggered: Ping Status Heartbeat', 'text-slate-300');
            try {
                await this.deviceController.controlDeviceInstall(selectedDev.agentId, 'PING_STATUS');
                UIControls.notify('Ping dispatched to node!', 'info');
            } catch (e) {
                appendLog(`Ping failed: ${e.message}`, 'text-red-400');
            }
        });

        modal.querySelector('#btn-ctrl-switch-bg')?.addEventListener('click', async () => {
            appendLog('Operator switched to Option 2: Autonomous Background Script', 'text-emerald-300');
            try {
                await this.deviceController.controlDeviceInstall(selectedDev.agentId, 'SWITCH_TO_BACKGROUND');
                UIControls.notify('Switched to Background Script execution!', 'success');
            } catch (e) {
                appendLog(`Switch failed: ${e.message}`, 'text-red-400');
            }
        });

        modal.querySelector('#btn-ctrl-abort')?.addEventListener('click', async () => {
            appendLog('Operator requested: ABORT SESSION', 'text-red-400');
            try {
                await this.deviceController.controlDeviceInstall(selectedDev.agentId, 'ABORT_INSTALL');
                UIControls.notify('OTA session aborted.', 'warning');
                setTimeout(() => closeConsole(), 1200);
            } catch (e) {
                appendLog(`Abort failed: ${e.message}`, 'text-red-400');
            }
        });

        const closeConsole = () => {
            if (this.activeInstallPoll) {
                clearInterval(this.activeInstallPoll);
                this.activeInstallPoll = null;
            }
            modal.remove();
        };

        modal.querySelector('#btn-console-close')?.addEventListener('click', closeConsole);

        // Real-Time Polling Loop (Checks device progress every 2.5s)
        let lastReportedStatus = '';
        this.activeInstallPoll = setInterval(async () => {
            try {
                const data = await this.deviceController.getDeviceInstallStatus(selectedDev.agentId);
                if (!data) return;

                const status = data.installStatus;
                const stage = data.stage;
                const message = data.message;
                const chosenEngine = data.chosenEngine || data.mode;

                modal.querySelector('#console-status-text').textContent = status;
                modal.querySelector('#console-engine-text').textContent = chosenEngine;
                if (message) modal.querySelector('#console-message-text').textContent = message;

                if (status !== lastReportedStatus) {
                    lastReportedStatus = status;
                    appendLog(`Status Update: [${status}] - ${message || 'Processing stage...'}`, 'text-sky-200');

                    if (status === 'DISPATCHED' || status === 'PENDING_INSTALL') {
                        updateStepper(1, 'STAGE 1: PUSH DISPATCHED');
                    } else if (status === 'DOWNLOADING') {
                        updateStepper(2, 'STAGE 2: DOWNLOADING APK');
                    } else if (status === 'DOWNLOADED') {
                        updateStepper(3, 'STAGE 3: PACKAGE VERIFIED');
                    } else if (status === 'INSTALLER_SCREEN_ACTIVE' || status === 'USER_ACTION_REQUIRED') {
                        updateStepper(4, 'STAGE 4: SCREEN ACTIVE ON PHONE');
                        appendLog('📱 Native installer screen active on device! Awaiting confirmation.', 'text-amber-300');
                    } else if (status === 'SUCCESS_INSTALLED' || status === 'COMPLETED' || Number(data.installedCode) >= Number(targetCode)) {
                        updateStepper(5, 'STAGE 5: UPGRADE COMPLETE');
                        appendLog(`🎉 App updated to Build ${data.installedCode}! Session finished successfully.`, 'text-emerald-300 font-bold');
                        modal.querySelector('#console-live-badge').textContent = 'COMPLETED';
                        modal.querySelector('#console-live-badge').className = 'text-[8.5px] px-2 py-0.5 rounded font-mono font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/40';
                        clearInterval(this.activeInstallPoll);
                        this.activeInstallPoll = null;
                    } else if (status === 'INSTALL_FAILED' || status === 'DOWNLOAD_FAILED' || status === 'ABORTED_BY_OPERATOR') {
                        appendLog(`❌ Session halted: ${status} - ${message}`, 'text-red-400 font-bold');
                        modal.querySelector('#console-live-badge').textContent = 'HALTED';
                        modal.querySelector('#console-live-badge').className = 'text-[8.5px] px-2 py-0.5 rounded font-mono font-bold bg-red-500/20 text-red-300 border border-red-500/40';
                        clearInterval(this.activeInstallPoll);
                        this.activeInstallPoll = null;
                    }
                }
            } catch (err) {
                console.warn('[Console Poll Error]', err);
            }
        }, 2500);
    }
}


