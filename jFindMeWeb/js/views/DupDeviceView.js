import { UIControls } from '../components/UIControls.js';
import { FeedbackDialog } from '../components/FeedbackDialog.js';

/**
 * DupDeviceView — Dedicated Duplicate Device Reconciliation & Silicon Hardware Sentinel
 * Identifies devices whose Agent IDs changed post-OTA/system update due to signature mismatches
 * by verifying their immutable Physical Silicon SHA-256 Hardware Properties (Brand + Model + Board + Hardware + Device).
 * Enables 1-click atomic multi-tier merging into the new active device under 'Old_Device_ID'.
 */
export class DupDeviceView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.onBackToDeviceUser = options.onBackToDeviceUser || null;
        this.onNavigateMap = options.onNavigateMap || null;
        this.onMergeSuccess = options.onMergeSuccess || null;

        this.duplicates = [];
        this.isLoading = false;
        this.searchQuery = "";
        this.selectedPairId = null;
        this.isMerging = false;
    }

    /**
     * Loads duplicate pairs from backend API
     */
    async loadDuplicates() {
        this.isLoading = true;
        this.render();

        try {
            const res = await (window.fetchApi ? window.fetchApi('/api/duplicate-devices') : fetch('/api/duplicate-devices').then(r => r.json()));
            if (res && res.error) {
                throw new Error(res.message || res.error);
            }
            this.duplicates = res.duplicates || [];
            if (window.log) window.log(`[DUP_SENTINEL] Scanned database: ${this.duplicates.length} duplicate pair(s) identified via Silicon SHA-256.`, "success");
        } catch (err) {
            console.error("[DUP_VIEW] Failed to load duplicates:", err);
            UIControls.notify(`Duplicate scan failed: ${err.message}`, 'danger', { duration: 4500 });
            this.duplicates = [];
        } finally {
            this.isLoading = false;
            this.render();
        }
    }

    /**
     * Main Render Method
     */
    render() {
        const container = document.getElementById(this.containerId);
        if (!container) return;

        let filtered = this.duplicates;
        if (this.searchQuery && this.searchQuery.trim()) {
            const q = this.searchQuery.trim().toLowerCase();
            filtered = this.duplicates.filter(p => {
                const oldD = p.oldDevice || {};
                const newD = p.newDevice || {};
                return (oldD.name && oldD.name.toLowerCase().includes(q)) ||
                       (oldD.agentId && oldD.agentId.toLowerCase().includes(q)) ||
                       (newD.name && newD.name.toLowerCase().includes(q)) ||
                       (newD.agentId && newD.agentId.toLowerCase().includes(q)) ||
                       (p.siliconHash && p.siliconHash.toLowerCase().includes(q)) ||
                       (p.hardwareProperties?.model && p.hardwareProperties.model.toLowerCase().includes(q));
            });
        }

        container.innerHTML = `
            <div class="w-full h-full select-text overflow-y-auto custom-scrollbar p-3 font-sans space-y-3">
                
                <!-- TOP HEADER BAR -->
                <div class="card-theme px-3.5 py-2.5 flex flex-wrap items-center justify-between gap-3 shadow-md border-l-4 border-l-amber-500">
                    <div class="flex items-center gap-3 min-w-0">
                        <div class="w-9 h-9 rounded-xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-amber-400 font-bold text-lg shrink-0 shadow-inner">
                            🔄
                        </div>
                        <div class="min-w-0">
                            <div class="flex items-center gap-2 flex-wrap">
                                <h2 class="text-sm font-extrabold tracking-wide text-primary-theme uppercase">
                                    Duplicate Device Reconciliation Matrix
                                </h2>
                                <span class="badge-status badge-accent text-[9px] bg-amber-500/20 text-amber-300 border border-amber-500/30">
                                    SILICON SHA-256 SENTINEL
                                </span>
                                <span class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-sky-500/20 text-sky-400 border border-sky-500/30">
                                    ${this.duplicates.length} DUPLICATE PAIR${this.duplicates.length === 1 ? '' : 'S'}
                                </span>
                            </div>
                            <p class="text-[11px] text-secondary-theme font-mono truncate mt-0.5">
                                Identifies nodes whose Agent IDs changed post-OTA due to keystore signature mismatches and reconciles them into <span class="text-sky-400 font-bold">Old_Device_ID</span>.
                            </p>
                        </div>
                    </div>

                    <div class="flex items-center gap-2 shrink-0">
                        <button id="btn-dup-refresh" class="btn-action-secondary text-xs py-1.5 px-3 flex items-center gap-1.5">
                            <span>🔄 Scan Database</span>
                        </button>
                        <button id="btn-dup-back" class="btn-action-primary text-xs py-1.5 px-3.5 flex items-center gap-1.5 bg-slate-800 hover:bg-slate-700 border-slate-600">
                            <span>⬅️ Back to Device User</span>
                        </button>
                    </div>
                </div>

                <!-- MANUAL PAIR ADDITION SECTION (PRE-SCAN / TARGETED VERIFICATION) -->
                <div class="card-theme p-3 border border-sky-500/30 bg-sky-500/5 rounded-xl shadow-sm space-y-2.5">
                    <div class="flex items-center justify-between">
                        <div class="flex items-center gap-2">
                            <span class="text-sm">🎯</span>
                            <span class="text-xs font-bold text-primary-theme uppercase tracking-wider">Manual ID Pair Verification & Addition</span>
                            <span class="badge-status text-[8px] bg-sky-500/20 text-sky-300">TARGETED ANCHOR</span>
                        </div>
                        <span class="text-[10px] text-secondary-theme font-mono">Specify 2 IDs to verify hardware specs & merge</span>
                    </div>

                    <div class="grid grid-cols-1 sm:grid-cols-12 gap-2 items-center">
                        <div class="sm:col-span-4 relative">
                            <label class="block text-[9px] uppercase font-bold text-rose-400 mb-0.5">1. Pre-Update (Old Device ID) *</label>
                            <input type="text" id="manual-old-id" placeholder="e.g. 2B75E1E9" maxlength="12"
                                   class="input-theme w-full py-1 px-2.5 text-xs font-mono uppercase font-bold border-rose-500/40 focus:border-rose-400">
                        </div>

                        <div class="sm:col-span-1 flex items-center justify-center pt-3 text-secondary-theme font-bold text-sm">
                            ➔
                        </div>

                        <div class="sm:col-span-4 relative">
                            <label class="block text-[9px] uppercase font-bold text-emerald-400 mb-0.5">2. Post-Update (New Device ID) *</label>
                            <input type="text" id="manual-new-id" placeholder="e.g. 3C86F2A0" maxlength="12"
                                   class="input-theme w-full py-1 px-2.5 text-xs font-mono uppercase font-bold border-emerald-500/40 focus:border-emerald-400">
                        </div>

                        <div class="sm:col-span-3 pt-3 flex items-center gap-1.5">
                            <button id="btn-manual-verify-pair" class="btn-action-primary text-xs py-1.5 px-3 w-full bg-gradient-to-r from-sky-600 to-sky-500 hover:from-sky-500 hover:to-sky-400 font-bold flex items-center justify-center gap-1 shadow-sm">
                                <span>⚡ Verify & Add Pair</span>
                            </button>
                        </div>
                    </div>
                    <div id="manual-pair-error" class="hidden text-[10px] text-rose-400 font-mono font-medium p-1.5 rounded bg-rose-500/10 border border-rose-500/20"></div>
                </div>

                <!-- SEARCH & FILTER TOOLBAR -->
                <div class="card-theme p-2.5 flex items-center justify-between gap-3 shadow-sm">
                    <div class="relative flex-1 max-w-md">
                        <input type="text" id="dup-search-input" 
                               placeholder="🔍 Filter by Name, Hardware ID, Model, or Silicon Hash..." 
                               value="${this.searchQuery}"
                               class="input-theme w-full pl-8 pr-4 py-1.5 text-xs font-mono">
                        <svg class="w-3.5 h-3.5 absolute left-2.5 top-2.5 text-secondary-theme pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                        </svg>
                    </div>

                    <div class="text-xs font-mono text-secondary-theme flex items-center gap-2">
                        <span>Showing: <strong class="text-primary-theme">${filtered.length}</strong> of ${this.duplicates.length}</span>
                    </div>
                </div>

                <!-- MAIN WORKSPACE / DUPLICATE PAIRS LIST -->
                ${this.isLoading ? `
                    <div class="card-theme p-12 text-center flex flex-col items-center justify-center space-y-3">
                        <div class="w-8 h-8 border-3 border-amber-500 border-t-transparent rounded-full animate-spin"></div>
                        <p class="text-sm font-bold text-amber-400">Scanning Firebase Matrix for Silicon Hardware Fingerprint Matches...</p>
                        <p class="text-xs text-secondary-theme font-mono">Cross-verifying Brand, Model, Board, Hardware, Device & Build Fingerprint</p>
                    </div>
                ` : filtered.length === 0 ? `
                    <div class="card-theme p-12 text-center flex flex-col items-center justify-center space-y-3 border border-emerald-500/20 bg-emerald-500/5">
                        <div class="w-12 h-12 rounded-full bg-emerald-500/20 flex items-center justify-center text-emerald-400 text-2xl font-bold">
                            ✅
                        </div>
                        <h3 class="text-base font-bold text-emerald-400">No Duplicate Devices Detected</h3>
                        <p class="text-xs text-secondary-theme max-w-lg font-mono">
                            All registered hardware devices in Firebase RTDB have unique physical silicon hashes without orphaned pre-OTA signature instances.
                        </p>
                        <button id="btn-dup-refresh-empty" class="btn-action-primary text-xs py-1.5 px-4 mt-2">
                            🔄 Re-scan Database
                        </button>
                    </div>
                ` : `
                    <div class="space-y-3">
                        ${filtered.map((pair, idx) => this.renderPairCard(pair, idx)).join('')}
                    </div>
                `}

            </div>
        `;

        this.bindEvents();
    }

    /**
     * Renders a single Duplicate Pair Comparison Card
     */
    /**
     * Renders a single Duplicate Pair Comparison Card
     * Focused on moving old device details into Old_Device_ID under the new device.
     * Shows only main headings and summary items initially; full detailed headings expand on click.
     */
    renderPairCard(pair, index) {
        const oldD = pair.oldDevice || {};
        const newD = pair.newDevice || {};
        const hw = pair.hardwareProperties || {};
        const isSel = this.selectedPairId === pair.pairId;
        const headingsComp = pair.headingsComparison || { headings: [], totalHeadings: 0, syncedHeadings: 0, missingHeadings: 0, isFullySynced: false };
        const isMoved = Boolean(pair.isMoved || pair.matchType?.includes('MOVED') || newD.hasArchive);

        return `
            <div class="card-theme p-3.5 border ${isSel ? 'border-amber-500 ring-1 ring-amber-500/30' : 'border-subtle-theme'} shadow-md transition-all hover:border-amber-500/50 space-y-3">
                
                <!-- PAIR HEADER: SILICON FINGERPRINT & MOVE TARGET BANNER -->
                <div class="flex flex-wrap items-center justify-between gap-2 pb-2.5 border-b border-subtle-theme">
                    <div class="flex items-center gap-2.5 min-w-0">
                        <span class="w-6 h-6 rounded-full bg-amber-500/20 text-amber-300 flex items-center justify-center text-[10px] font-bold shrink-0">
                            #${index + 1}
                        </span>
                        <div class="min-w-0">
                            <div class="flex items-center gap-2 flex-wrap">
                                <span class="text-xs font-extrabold text-primary-theme uppercase tracking-wider">
                                    Hardware Anchor: ${hw.brand || 'Generic'} ${hw.model || 'Android'}
                                </span>
                                <span class="badge-status text-[8.5px] ${isMoved ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30' : 'bg-sky-500/15 text-sky-300 border border-sky-500/30'}">
                                    ${pair.matchType || 'SILICON_SHA256'}
                                </span>
                                <span class="badge-status text-[8.5px] ${isMoved ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40' : 'bg-amber-500/20 text-amber-300 border border-amber-500/40'} font-bold">
                                    ${isMoved ? '✅ MOVED INTO Old_Device_ID' : '📦 READY TO MOVE INTO Old_Device_ID'}
                                </span>
                            </div>
                            <div class="text-[9.5px] text-secondary-theme font-mono flex items-center gap-2 mt-0.5 select-text">
                                <span class="text-rose-400 font-bold font-mono select-text">${oldD.agentId}</span>
                                <span class="text-slate-400">➔ Move Details into ➔</span>
                                <span class="text-emerald-400 font-bold font-mono select-text">${newD.agentId} <span class="text-sky-300">(/Old_Device_ID/${oldD.agentId})</span></span>
                            </div>
                        </div>
                    </div>

                    <!-- MOVE / ARCHIVE ACTION BUTTON -->
                    <div class="flex items-center gap-2">
                        ${isMoved ? `
                            <button class="btn-action-primary text-xs py-1.5 px-3.5 bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-500 hover:to-emerald-400 text-slate-950 font-extrabold shadow-md flex items-center gap-1.5 btn-sync-all-headings"
                                    data-old-id="${oldD.agentId}" 
                                    data-new-id="${newD.agentId}">
                                <span>${headingsComp.isFullySynced ? '✅ Moved under Old_Device_ID (Preserved)' : '📦 Move Remaining Headings to Old_Device_ID'}</span>
                            </button>
                        ` : `
                            <button class="btn-action-primary text-xs py-1.5 px-3.5 bg-gradient-to-r from-amber-600 to-amber-500 hover:from-amber-500 hover:to-amber-400 text-slate-950 font-extrabold shadow-md flex items-center gap-1.5 btn-merge-action"
                                    data-old-id="${oldD.agentId}" 
                                    data-new-id="${newD.agentId}"
                                    data-pair-name="${newD.name || hw.model}">
                                <span>📦 Move All Details to ${newD.agentId} (Old_Device_ID)</span>
                            </button>
                        `}
                    </div>
                </div>

                <!-- MAIN ITEMS AT A GLANCE (SIDE-BY-SIDE COMPACT COLUMNS) -->
                <div class="grid grid-cols-1 md:grid-cols-2 gap-3 items-stretch">
                    
                    <!-- LEFT COLUMN: OLD DEPRECATED DEVICE SUMMARY -->
                    <div class="p-3 rounded-xl border border-rose-500/30 bg-rose-500/5 flex flex-col justify-between space-y-2">
                        <div class="space-y-2">
                            <div class="flex items-center justify-between">
                                <span class="badge-status text-[8.5px] bg-rose-500/20 text-rose-300 border border-rose-500/40 font-extrabold uppercase">
                                    🔴 SOURCE (OLD PRE-OTA DEVICE)
                                </span>
                                <span class="text-[9.5px] font-mono text-rose-300/80">
                                    Last Seen: ${oldD.lastSeenFormatted}
                                </span>
                            </div>

                            <div class="flex items-center gap-2.5">
                                <img src="${oldD.avatarUrl || '/assets/avatars/1.png'}" 
                                     alt="${oldD.name}" 
                                     class="w-10 h-10 rounded-full object-cover border-2 border-rose-500/60 shadow-sm shrink-0">
                                <div class="min-w-0 flex-1">
                                    <h4 class="text-xs font-bold text-primary-theme truncate flex items-center gap-1.5">
                                        <span>${oldD.name}</span>
                                        <span class="badge-status text-[8px] bg-white/10 text-slate-300 font-normal">${oldD.deviceName}</span>
                                    </h4>
                                    <div class="text-[10px] font-mono text-secondary-theme flex items-center gap-2 mt-0.5">
                                        <span class="text-rose-400 font-bold">ID: ${oldD.agentId}</span>
                                        <span>•</span>
                                        <span>OS: ${oldD.osVersion}</span>
                                    </div>
                                </div>
                            </div>

                            <!-- Hardware Dossier Specs (Grey Box) -->
                            ${this.renderHardwareDossierBox(oldD, hw)}

                            <!-- Main Summary Badges -->
                            <div class="grid grid-cols-3 gap-1 pt-1 text-center font-mono text-[9px]">
                                <div class="p-1 rounded bg-black/30 border border-white/5">
                                    <div class="text-slate-400 text-[8px]">History Fixes</div>
                                    <div class="font-bold text-amber-400">${oldD.historyCount || 0} pts</div>
                                </div>
                                <div class="p-1 rounded bg-black/30 border border-white/5">
                                    <div class="text-slate-400 text-[8px]">Circle Peers</div>
                                    <div class="font-bold text-sky-400">${oldD.relationshipsCount || 0} linked</div>
                                </div>
                                <div class="p-1 rounded bg-black/30 border border-white/5">
                                    <div class="text-slate-400 text-[8px]">Safe Zones</div>
                                    <div class="font-bold text-emerald-400">${oldD.safeZonesCount || 0} zones</div>
                                </div>
                            </div>
                        </div>

                        <div class="pt-1.5 border-t border-rose-500/20 text-[9.5px] text-rose-300/90 font-mono flex items-center justify-between">
                            <span>📦 Source Root: <code class="text-sky-400 font-bold">/devices/${oldD.agentId}</code></span>
                            <span class="badge-status text-[8px] bg-rose-500/20 text-rose-300">${isMoved ? 'Archived' : 'Active'}</span>
                        </div>
                    </div>

                    <!-- RIGHT COLUMN: NEW ACTIVE DEVICE SUMMARY -->
                    <div class="p-3 rounded-xl border border-emerald-500/30 bg-emerald-500/5 flex flex-col justify-between space-y-2">
                        <div class="space-y-2">
                            <div class="flex items-center justify-between">
                                <span class="badge-status text-[8.5px] bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 font-extrabold uppercase">
                                    🟢 TARGET (NEW ACTIVE DEVICE)
                                </span>
                                <span class="text-[9.5px] font-mono text-emerald-400 font-bold">
                                    ${newD.isLive ? '⚡ LIVE NOW' : `Last Seen: ${newD.lastSeenFormatted}`}
                                </span>
                            </div>

                            <div class="flex items-center gap-2.5">
                                <img src="${newD.avatarUrl || '/assets/avatars/1.png'}" 
                                     alt="${newD.name}" 
                                     class="w-10 h-10 rounded-full object-cover border-2 border-emerald-500 shadow-sm shrink-0">
                                <div class="min-w-0 flex-1">
                                    <h4 class="text-xs font-bold text-primary-theme truncate flex items-center gap-1.5">
                                        <span>${newD.name}</span>
                                        <span class="badge-status text-[8px] bg-emerald-500/20 text-emerald-300 font-normal">${newD.deviceName}</span>
                                    </h4>
                                    <div class="text-[10px] font-mono text-secondary-theme flex items-center gap-2 mt-0.5">
                                        <span class="text-emerald-400 font-bold">ID: ${newD.agentId}</span>
                                        <span>•</span>
                                        <span>Battery: <strong class="text-sky-400">${newD.batteryPct}%</strong></span>
                                    </div>
                                </div>
                            </div>

                            <!-- Hardware Dossier Specs (Grey Box) -->
                            ${this.renderHardwareDossierBox(newD, hw)}

                            <!-- Main Summary Badges -->
                            <div class="grid grid-cols-3 gap-1 pt-1 text-center font-mono text-[9px]">
                                <div class="p-1 rounded bg-black/30 border border-white/5">
                                    <div class="text-slate-400 text-[8px]">Old_Device_ID</div>
                                    <div class="font-bold text-sky-400">${headingsComp.syncedHeadings}/${headingsComp.totalHeadings} Headings</div>
                                </div>
                                <div class="p-1 rounded bg-black/30 border border-white/5">
                                    <div class="text-slate-400 text-[8px]">Preserved Peers</div>
                                    <div class="font-bold text-emerald-400">${newD.relationshipsCount || 0} peers</div>
                                </div>
                                <div class="p-1 rounded bg-black/30 border border-white/5">
                                    <div class="text-slate-400 text-[8px]">Archive Status</div>
                                    <div class="font-bold ${headingsComp.isFullySynced ? 'text-emerald-400' : 'text-amber-400'}">${headingsComp.isFullySynced ? '100% Synced' : 'Ready'}</div>
                                </div>
                            </div>
                        </div>

                        <div class="pt-1.5 border-t border-emerald-500/20 text-[9.5px] text-emerald-300/90 font-mono flex items-center justify-between">
                            <span>🎯 Archive Target: <strong class="text-emerald-400 font-bold">Old_Device_ID/${oldD.agentId}</strong></span>
                            <span class="badge-status text-[8px] bg-emerald-500/20 text-emerald-300">${headingsComp.syncedHeadings}/${headingsComp.totalHeadings} Headings</span>
                        </div>
                    </div>

                </div>

                <!-- EXPANDABLE HEADINGS DOSSIER (COLLAPSED BY DEFAULT, EXPANDS ONLY ON CLICK) -->
                <details class="group/headings-dossier mt-2 border border-sky-500/30 rounded-xl overflow-hidden">
                    <summary class="cursor-pointer font-bold text-xs text-sky-400 hover:text-sky-300 flex items-center justify-between p-2.5 bg-black/40 hover:bg-black/60 select-none transition-colors">
                        <div class="flex items-center gap-2 min-w-0">
                            <span class="text-sm">📋</span>
                            <span class="truncate">Inspect All 16 Headings under Old_Device_ID (${oldD.agentId} ➔ ${newD.agentId})</span>
                            <span class="badge-status text-[8.5px] ${headingsComp.isFullySynced ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40' : 'bg-amber-500/20 text-amber-300 border border-amber-500/40'} font-mono shrink-0">
                                ${headingsComp.syncedHeadings}/${headingsComp.totalHeadings} Preserved
                            </span>
                        </div>
                        <span class="text-[9px] text-secondary-theme font-mono group-open/headings-dossier:rotate-180 transition-transform duration-200">
                            ▼ Click to Expand / Collapse
                        </span>
                    </summary>

                    <div class="p-3 bg-black/25 space-y-3">
                        <!-- Side-by-Side Checklists for Old and New Device -->
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                            ${this.renderDeviceHeadingsChecklist(oldD, headingsComp, 'old', newD.agentId)}
                            ${this.renderDeviceHeadingsChecklist(newD, headingsComp, 'synced', oldD.agentId)}
                        </div>

                        <!-- UNIFIED HEADINGS SYNCHRONIZATION MATRIX -->
                        ${this.renderHeadingsSyncMatrix(oldD, newD, headingsComp)}
                    </div>
                </details>

                <!-- STATUS BANNER -->
                <div class="p-2 rounded-lg bg-black/30 border border-subtle-theme text-[10px] font-mono text-secondary-theme flex flex-wrap items-center justify-between gap-2">
                    <div class="flex items-center gap-2">
                        <span class="text-amber-400 font-bold">⚡ WORKFLOW:</span>
                        <span>${isMoved 
                            ? `Historical details from <code class="text-rose-400 font-bold">${oldD.agentId}</code> are preserved in <code class="text-emerald-400 font-bold">${newD.agentId}</code> under heading <code class="text-sky-400 font-bold">Old_Device_ID</code>.`
                            : `Click "Move All Details" to archive all telemetry, breadcrumbs (${oldD.historyCount || 0} fixes), profile, circles, and headings from <code class="text-rose-400 font-bold">${oldD.agentId}</code> into <code class="text-emerald-400 font-bold">${newD.agentId}</code> under <code class="text-sky-400 font-bold">Old_Device_ID</code>.`
                        }</span>
                    </div>
                    <div class="text-slate-400 italic">
                        Audit Log: Change_Log/Pairing_Devices
                    </div>
                </div>

            </div>
        `;
    }

    /**
     * Renders Device Headings Checklist for a specific side (Old Device or Old_Device_ID under New Device)
     * Displays headings with tickboxes. Collapsed by default; expands on click.
     * Both sides tick yes if they are sync in heading Old_device_Id (under new device).
     */
    renderDeviceHeadingsChecklist(dev, headingsComp, side = 'old', peerId = '') {
        const headings = headingsComp.headings || [];
        const isOld = side === 'old';
        const title = isOld 
            ? `📋 Source Headings (${headingsComp.syncedHeadings}/${headingsComp.totalHeadings} Moved)` 
            : `📦 Preserved in Old_Device_ID (${headingsComp.syncedHeadings}/${headingsComp.totalHeadings} Preserved)`;
        const borderColor = isOld ? 'border-rose-500/20' : 'border-emerald-500/20';
        const titleColor = isOld ? 'text-rose-300' : 'text-emerald-300';

        return `
            <div class="p-2 rounded-lg bg-black/25 dark:bg-black/40 border ${borderColor} space-y-1.5 font-mono text-[9.5px]">
                <details class="group/sidecheck">
                    <summary class="cursor-pointer flex items-center justify-between font-bold ${titleColor} hover:opacity-90 select-none py-0.5">
                        <div class="flex items-center gap-1.5">
                            <span class="transition-transform duration-200 group-open/sidecheck:rotate-90 text-[8px]">▶</span>
                            <span>${title}</span>
                        </div>
                        <span class="badge-status text-[8px] ${headingsComp.isFullySynced ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40' : 'bg-amber-500/20 text-amber-300 border border-amber-500/40'}">
                            ${headingsComp.isFullySynced ? '🟢 100% MOVED' : `⚠️ ${headingsComp.missingHeadings} PENDING`}
                        </span>
                    </summary>
                    <div class="pt-2 space-y-1 divide-y divide-white/5">
                        ${headings.map(h => {
                            // User rule: Both side tick yes if they are sync in heading Old_device_Id (under new device)
                            const isTicked = h.isBothSynced;
                            const fieldsCount = isOld ? h.oldRecordsCount : h.syncedRecordsCount;
                            const sampleText = isOld ? h.oldSample : h.syncedSample;
                            const previewJson = isOld ? h.oldPreview : h.syncedPreview;

                            return `
                                <details class="py-1 group/item">
                                    <summary class="cursor-pointer flex items-center justify-between gap-1 select-none hover:bg-white/5 px-1 py-0.5 rounded transition-colors">
                                        <div class="flex items-center gap-2 min-w-0">
                                            <input type="checkbox" 
                                                   ${isTicked ? 'checked' : ''} 
                                                   class="w-3.5 h-3.5 accent-emerald-500 rounded pointer-events-none" 
                                                   disabled>
                                            <span class="truncate font-semibold text-slate-200 flex items-center gap-1">
                                                <span>${h.icon}</span>
                                                <span>${h.key}</span>
                                            </span>
                                        </div>
                                        <div class="flex items-center gap-1.5 shrink-0 text-[8.5px]">
                                            <span class="text-secondary-theme">${fieldsCount} field(s)</span>
                                            <span class="${isTicked ? 'text-emerald-400 font-bold' : 'text-rose-400 font-bold'}">
                                                ${isTicked ? '✔ YES' : (isOld ? '✖ NOT MOVED' : '✖ MISSING')}
                                            </span>
                                            <span class="text-secondary-theme text-[7.5px] transition-transform duration-200 group-open/item:rotate-90">▶</span>
                                        </div>
                                    </summary>
                                    <div class="mt-1 pl-6 pr-1 py-1 text-[8.5px] bg-black/40 rounded border border-white/5 space-y-1 select-text">
                                        <div class="flex items-center justify-between text-secondary-theme italic">
                                            <span>${h.desc}</span>
                                            <span class="text-sky-300 font-bold font-mono">[${h.key}]</span>
                                        </div>
                                        ${sampleText ? `
                                            <div class="text-slate-300">
                                                <span class="text-slate-400">Child Keys:</span> 
                                                <code class="${isOld ? 'text-rose-300' : 'text-emerald-300'}">${sampleText}</code>
                                            </div>
                                        ` : `
                                            <div class="text-rose-400 italic">No sub-keys or records currently stored.</div>
                                        `}
                                        ${previewJson ? `
                                            <details class="mt-0.5">
                                                <summary class="cursor-pointer text-[8px] text-sky-400 hover:underline select-none">
                                                    🔍 View Raw Data Preview (${fieldsCount} entries)
                                                </summary>
                                                <pre class="mt-1 text-[8px] text-slate-300 max-h-24 overflow-y-auto p-1.5 bg-black/70 rounded border border-white/10 select-text font-mono whitespace-pre-wrap">${previewJson}</pre>
                                            </details>
                                        ` : ''}
                                    </div>
                                </details>
                            `;
                        }).join('')}
                    </div>
                </details>
            </div>
        `;
    }

    /**
     * Renders Master Headings Move & Parity Matrix (Old Device ↔ Old_Device_ID under New Device)
     * Displays all headings with dual tickboxes side-by-side.
     * Both sides tick yes if they are synced in Old_device_Id (under new device).
     */
    renderHeadingsSyncMatrix(oldD, newD, headingsComp) {
        const headings = headingsComp.headings || [];
        const syncedCount = headingsComp.syncedHeadings || 0;
        const totalCount = headingsComp.totalHeadings || 0;
        const missingCount = headingsComp.missingHeadings || 0;
        const isFullySynced = headingsComp.isFullySynced;

        return `
            <div class="p-3 rounded-xl bg-black/30 border border-sky-500/30 space-y-2.5 font-mono">
                
                <!-- MATRIX HEADER -->
                <div class="flex flex-wrap items-center justify-between gap-2 pb-2 border-b border-subtle-theme">
                    <div class="flex items-center gap-2">
                        <span class="text-base">📦</span>
                        <div>
                            <h4 class="text-xs font-bold text-primary-theme flex items-center gap-2">
                                <span>Headings Preservation Matrix</span>
                                <span class="text-secondary-theme font-normal text-[10px]">(${oldD.agentId} ➔ Old_Device_ID/${oldD.agentId})</span>
                            </h4>
                            <p class="text-[9.5px] text-secondary-theme">
                                Both sides tick yes (<span class="text-emerald-400 font-bold">✔ YES</span>) when heading is preserved under <code class="text-sky-400 font-bold">Old_Device_ID</code>
                            </p>
                        </div>
                    </div>

                    <div class="flex items-center gap-2">
                        <span class="badge-status text-[9px] ${isFullySynced ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40' : 'bg-amber-500/20 text-amber-300 border border-amber-500/40'} font-bold">
                            ${isFullySynced ? `🟢 ${syncedCount}/${totalCount} PRESERVED` : `⚠️ ${missingCount} PENDING IN Old_Device_ID`}
                        </span>
                        ${missingCount > 0 ? `
                            <button class="btn-action-primary text-[10px] py-1 px-3 bg-gradient-to-r from-amber-600 to-amber-500 hover:from-amber-500 hover:to-amber-400 text-slate-950 font-extrabold shadow-sm flex items-center gap-1 btn-sync-all-headings"
                                    data-old-id="${oldD.agentId}" 
                                    data-new-id="${newD.agentId}">
                                <span>📦 Move Remaining Headings to Old_Device_ID</span>
                            </button>
                        ` : `
                            <button class="btn-action-secondary text-[10px] py-1 px-2.5 text-emerald-300 border border-emerald-500/30 hover:bg-emerald-500/10 flex items-center gap-1 btn-sync-all-headings"
                                    data-old-id="${oldD.agentId}" 
                                    data-new-id="${newD.agentId}">
                                <span>🔄 Re-Verify Parity</span>
                            </button>
                        `}
                    </div>
                </div>

                <!-- TABLE / CHECKLIST GRID -->
                <div class="overflow-x-auto">
                    <table class="w-full text-left text-[9.5px] border-collapse">
                        <thead>
                            <tr class="border-b border-subtle-theme text-secondary-theme font-bold bg-white/5">
                                <th class="py-1.5 px-2 text-center w-16">Old Device<br><span class="text-[8px] font-normal text-rose-300">[✔ Tick]</span></th>
                                <th class="py-1.5 px-2">Heading Name & Description</th>
                                <th class="py-1.5 px-2 text-center w-24">Old_Device_ID<br><span class="text-[8px] font-normal text-emerald-300">[✔ Tick]</span></th>
                                <th class="py-1.5 px-2 text-center w-36">Preservation State</th>
                                <th class="py-1.5 px-2 text-right w-24">Action</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-subtle-theme/50">
                            ${headings.map((h, hIdx) => {
                                const isTicked = h.isBothSynced;
                                return `
                                    <tr class="hover:bg-white/5 transition-colors group/row">
                                        <!-- LEFT TICKBOX: OLD DEVICE -->
                                        <td class="py-1.5 px-2 text-center align-middle">
                                            <div class="flex items-center justify-center">
                                                <input type="checkbox" 
                                                       ${isTicked ? 'checked' : ''} 
                                                       class="w-4 h-4 accent-emerald-500 rounded pointer-events-none" 
                                                       title="${isTicked ? 'Preserved in Old_Device_ID' : 'Pending in Old_Device_ID'}"
                                                       disabled>
                                            </div>
                                        </td>

                                        <!-- HEADING INFO -->
                                        <td class="py-1.5 px-2 align-middle">
                                            <details class="group/matrixdetail">
                                                <summary class="cursor-pointer select-none flex items-center justify-between gap-1 hover:text-sky-300">
                                                    <div class="flex items-center gap-1.5">
                                                        <span class="text-xs">${h.icon}</span>
                                                        <span class="font-bold text-primary-theme">${h.label}</span>
                                                        <code class="text-sky-400 font-bold text-[8.5px] bg-sky-500/10 px-1 rounded">${h.key}</code>
                                                    </div>
                                                    <span class="text-[8px] text-secondary-theme group-open/matrixdetail:rotate-90 transition-transform">▶ Details</span>
                                                </summary>
                                                <div class="mt-1.5 p-2 bg-black/60 rounded border border-white/10 space-y-1.5 text-[8.5px] select-text">
                                                    <p class="text-secondary-theme italic">${h.desc}</p>
                                                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1 border-t border-white/10">
                                                        <div>
                                                            <span class="text-rose-300 font-bold">Source Old Node:</span> ${h.oldRecordsCount} fields
                                                            <div class="text-slate-400 text-[8px] truncate mt-0.5"><code>${h.oldSample || 'None'}</code></div>
                                                        </div>
                                                        <div>
                                                            <span class="text-emerald-300 font-bold">Old_Device_ID Archive:</span> ${h.syncedRecordsCount} fields
                                                            <div class="text-slate-400 text-[8px] truncate mt-0.5"><code>${h.syncedSample || 'None'}</code></div>
                                                        </div>
                                                    </div>
                                                    ${h.syncedPreview || h.oldPreview ? `
                                                        <pre class="mt-1 text-[7.5px] text-slate-300 max-h-20 overflow-y-auto p-1 bg-black/80 rounded border border-white/5 font-mono">${h.syncedPreview || h.oldPreview}</pre>
                                                    ` : ''}
                                                </div>
                                            </details>
                                        </td>

                                        <!-- RIGHT TICKBOX: OLD_DEVICE_ID UNDER NEW DEVICE -->
                                        <td class="py-1.5 px-2 text-center align-middle">
                                            <div class="flex items-center justify-center">
                                                <input type="checkbox" 
                                                       ${isTicked ? 'checked' : ''} 
                                                       class="w-4 h-4 accent-emerald-500 rounded pointer-events-none" 
                                                       title="${isTicked ? 'Preserved in Old_Device_ID' : 'Pending in Old_Device_ID'}"
                                                       disabled>
                                            </div>
                                        </td>

                                        <!-- SYNC STATE BADGE -->
                                        <td class="py-1.5 px-2 text-center align-middle">
                                            ${isTicked ? `
                                                <span class="badge-status text-[8px] bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 font-bold">
                                                    🟢 BOTH TICK YES
                                                </span>
                                            ` : `
                                                <span class="badge-status text-[8px] bg-rose-500/20 text-rose-300 border border-rose-500/40 font-bold">
                                                    🔴 PENDING IN ARCHIVE
                                                </span>
                                            `}
                                        </td>

                                        <!-- ACTIONS -->
                                        <td class="py-1.5 px-2 text-right align-middle">
                                            ${isTicked ? `
                                                <span class="text-emerald-400 text-[9px] font-bold">✔ Moved</span>
                                            ` : `
                                                <button class="btn-action-primary text-[8.5px] py-0.5 px-2 bg-amber-500 hover:bg-amber-400 text-slate-950 font-extrabold shadow-sm btn-sync-single-heading"
                                                        data-old-id="${oldD.agentId}" 
                                                        data-new-id="${newD.agentId}"
                                                        data-heading="${h.key}">
                                                    <span>📦 Move</span>
                                                </button>
                                            `}
                                        </td>
                                    </tr>
                                `;
                            }).join('')}
                        </tbody>
                    </table>
                </div>

            </div>
        `;
    }

    /**
     * Renders comprehensive Hardware Dossier Metadata Specs Box (Grey Box)
     * Displays Brand, Model, Board, Hardware, Device, and IMEI (Dual SIM 1 & 2)
     */
    renderHardwareDossierBox(dev, hw = {}) {
        const brand = (dev.brand && dev.brand !== 'Generic') ? dev.brand : (hw.brand || dev.brand || 'Generic');
        const model = (dev.model && dev.model !== 'Unknown') ? dev.model : (hw.model || dev.model || 'Unknown');
        const board = (dev.board && dev.board !== 'N/A') ? dev.board : (hw.board || dev.board || 'N/A');
        const hardware = (dev.hardware && dev.hardware !== 'N/A') ? dev.hardware : (hw.hardware || dev.hardware || 'N/A');
        const device = (dev.device && dev.device !== 'N/A') ? dev.device : (hw.device || dev.device || 'N/A');

        // Dual SIM IMEI resolution (SIM 1 & SIM 2)
        const imei1 = (dev.imei1 && dev.imei1 !== 'N/A') ? dev.imei1 : (hw.imei1 || (dev.imei && dev.imei !== 'N/A' ? dev.imei : (hw.imei || 'N/A')));
        const imei2 = dev.imei2 || hw.imei2 || '';
        const isDualSim = Boolean(dev.isDualSim || hw.isDualSim || imei2);

        return `
            <div class="mt-2.5 p-2 rounded-lg bg-black/25 dark:bg-black/40 border border-subtle-theme space-y-1.5 font-mono text-[9.5px]">
                <div class="grid grid-cols-2 sm:grid-cols-3 gap-x-2 gap-y-1 text-secondary-theme select-text">
                    <div class="truncate">Brand: <strong class="text-sky-400 font-bold">${brand}</strong></div>
                    <div class="truncate">Model: <strong class="text-primary-theme font-bold">${model}</strong></div>
                    <div class="truncate">Board: <strong class="text-slate-200">${board}</strong></div>
                    <div class="truncate">Hardware: <strong class="text-slate-200">${hardware}</strong></div>
                    <div class="truncate">Device: <strong class="text-slate-200">${device}</strong></div>
                    <div class="truncate">History: <strong class="text-amber-400 font-bold">${dev.historyCount || 0} pts</strong></div>
                </div>
                <!-- Dual SIM IMEI & Cellular Metadata -->
                <div class="pt-1 border-t border-subtle-theme grid grid-cols-1 sm:grid-cols-2 gap-x-2 gap-y-0.5 text-secondary-theme select-text">
                    <div class="truncate">SIM / Cellular: <strong class="text-emerald-400 font-bold">${dev.simStatus || (dev.phone ? `Active • ${dev.phone}` : 'Active')}</strong></div>
                    <div class="truncate">Network Routing: <strong class="text-sky-300 font-semibold">${dev.networkPreference || 'AUTO_ANY_ACTIVE'}</strong></div>
                    <div class="truncate">IMEI 1: <strong class="text-slate-200">${imei1}</strong></div>
                    <div class="truncate">IMEI 2: <strong class="text-slate-200">${isDualSim ? (imei2 || 'N/A') : (imei2 ? imei2 : 'Slot 2 (None)')}</strong></div>
                </div>
                ${dev.location?.address ? `
                    <div class="pt-0.5 border-t border-subtle-theme/60 text-[9px] text-secondary-theme truncate select-text">
                        📍 <span class="text-emerald-400 font-medium">${dev.location.address}</span>
                    </div>
                ` : ''}
            </div>
        `;
    }

    /**
     * Binds DOM events
     */
    bindEvents() {
        const btnRefresh = document.getElementById('btn-dup-refresh');
        if (btnRefresh) {
            btnRefresh.onclick = () => this.loadDuplicates();
        }

        const btnRefreshEmpty = document.getElementById('btn-dup-refresh-empty');
        if (btnRefreshEmpty) {
            btnRefreshEmpty.onclick = () => this.loadDuplicates();
        }

        const btnBack = document.getElementById('btn-dup-back');
        if (btnBack) {
            btnBack.onclick = () => {
                if (this.onBackToDeviceUser) this.onBackToDeviceUser();
                else if (window.showDeviceUser) window.showDeviceUser();
            };
        }

        const searchInput = document.getElementById('dup-search-input');
        if (searchInput) {
            searchInput.oninput = (e) => {
                this.searchQuery = e.target.value;
                this.render();
            };
        }

        // Manual Pair Verification Handler
        const btnManualVerify = document.getElementById('btn-manual-verify-pair');
        const oldInput = document.getElementById('manual-old-id');
        const newInput = document.getElementById('manual-new-id');
        const errBox = document.getElementById('manual-pair-error');

        if (btnManualVerify && oldInput && newInput) {
            btnManualVerify.onclick = async () => {
                const oldId = oldInput.value.trim().toUpperCase();
                const newId = newInput.value.trim().toUpperCase();

                if (errBox) errBox.classList.add('hidden');

                if (!oldId || !newId) {
                    if (errBox) {
                        errBox.textContent = "⚠️ Please enter both Old Device ID and New Device ID.";
                        errBox.classList.remove('hidden');
                    }
                    return;
                }

                if (oldId === newId) {
                    if (errBox) {
                        errBox.textContent = "⚠️ Old Device ID and New Device ID cannot be identical.";
                        errBox.classList.remove('hidden');
                    }
                    return;
                }

                btnManualVerify.disabled = true;
                btnManualVerify.innerHTML = `<span class="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></span> Verifying...`;

                try {
                    const res = await (window.fetchApi ? window.fetchApi('/api/verify-duplicate-pair', {
                        method: 'POST',
                        body: JSON.stringify({ oldAgentId: oldId, newAgentId: newId })
                    }) : fetch('/api/verify-duplicate-pair', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ oldAgentId: oldId, newAgentId: newId })
                    }).then(r => r.json()));

                    if (res && res.error) {
                        throw new Error(res.message || res.error);
                    }

                    if (res && res.pair) {
                        const existingIdx = this.duplicates.findIndex(p => p.pairId === res.pair.pairId);
                        if (existingIdx >= 0) {
                            this.duplicates[existingIdx] = res.pair;
                        } else {
                            this.duplicates.unshift(res.pair);
                        }
                        this.selectedPairId = res.pair.pairId;
                        UIControls.notify(`Verified device pair: ${oldId} ➔ ${newId}`, 'success', { duration: 3500 });
                        this.render();
                    }
                } catch (err) {
                    console.error("[DUP_VIEW] Verification failed:", err);
                    if (errBox) {
                        errBox.textContent = `❌ ${err.message}`;
                        errBox.classList.remove('hidden');
                    }
                    UIControls.notify(`Verification failed: ${err.message}`, 'danger', { duration: 4500 });
                } finally {
                    btnManualVerify.disabled = false;
                    btnManualVerify.innerHTML = `<span>⚡ Verify & Add Pair</span>`;
                }
            };
        }

        // Merge Action Buttons
        const mergeBtns = document.querySelectorAll('.btn-merge-action');
        mergeBtns.forEach(btn => {
            btn.onclick = (e) => {
                const oldId = btn.getAttribute('data-old-id');
                const newId = btn.getAttribute('data-new-id');
                const pairName = btn.getAttribute('data-pair-name');
                this.promptMergeConfirmation(oldId, newId, pairName);
            };
        });

        // Sync Single Heading Buttons
        const syncSingleBtns = document.querySelectorAll('.btn-sync-single-heading');
        syncSingleBtns.forEach(btn => {
            btn.onclick = (e) => {
                e.stopPropagation();
                const oldId = btn.getAttribute('data-old-id');
                const newId = btn.getAttribute('data-new-id');
                const heading = btn.getAttribute('data-heading');
                this.syncSingleHeading(oldId, newId, heading);
            };
        });

        // Sync All Missing Headings Buttons
        const syncAllBtns = document.querySelectorAll('.btn-sync-all-headings');
        syncAllBtns.forEach(btn => {
            btn.onclick = (e) => {
                e.stopPropagation();
                const oldId = btn.getAttribute('data-old-id');
                const newId = btn.getAttribute('data-new-id');
                this.syncAllMissingHeadings(oldId, newId);
            };
        });
    }

    /**
     * Opens modal confirmation challenge before executing atomic Firebase merge
     */
    promptMergeConfirmation(oldAgentId, newAgentId, deviceName) {
        if (this.isMerging) return;

        const modalHtml = `
            <div id="modal-dup-merge-backdrop" class="fixed inset-0 bg-black/80 backdrop-blur-sm z-[999] flex items-center justify-center p-4 animate-fadeIn">
                <div class="card-theme max-w-lg w-full p-5 border border-amber-500/50 shadow-2xl space-y-4">
                    
                    <div class="flex items-center gap-3 border-b border-subtle-theme pb-3">
                        <div class="w-10 h-10 rounded-xl bg-amber-500/20 border border-amber-500/40 flex items-center justify-center text-amber-400 text-xl font-bold">
                            📦
                        </div>
                        <div>
                            <h3 class="text-sm font-extrabold text-primary-theme uppercase tracking-wide">
                                Move Old Device Details into New Device
                            </h3>
                            <p class="text-xs text-secondary-theme font-mono">
                                Preserves all old headings & telemetry under <span class="text-sky-400 font-bold">Old_Device_ID</span>
                            </p>
                        </div>
                    </div>

                    <div class="space-y-2.5 text-xs font-mono">
                        <div class="p-3 rounded-lg bg-amber-500/10 border border-amber-500/30 space-y-1.5 text-amber-300">
                            <p class="font-bold">You are about to move all details for <strong>${deviceName}</strong>:</p>
                            <ul class="list-disc list-inside space-y-1 text-[11px] text-slate-300 pl-1">
                                <li>Source Old Node: <code class="text-rose-400 font-bold">${oldAgentId}</code> (Deprecated Pre-OTA ID)</li>
                                <li>Target Active Node: <code class="text-emerald-400 font-bold">${newAgentId}</code> (Active Hardware ID)</li>
                                <li>Archive Destination: <code class="text-sky-400 font-bold">devices/${newAgentId}/Old_Device_ID/${oldAgentId}</code></li>
                            </ul>
                        </div>

                        <div class="p-2.5 rounded-lg bg-black/30 border border-subtle-theme space-y-1 text-[10.5px] text-slate-400">
                            <p class="text-sky-400 font-bold uppercase">Atomic Move & Archive Operations:</p>
                            <p>1. Moves all 16 headings from <code class="text-slate-300">/devices/${oldAgentId}</code> into <code class="text-sky-400">/devices/${newAgentId}/Old_Device_ID/${oldAgentId}</code>.</p>
                            <p>2. Preserves all history breadcrumbs into <code class="text-slate-300">/History/${newAgentId}/history</code>.</p>
                            <p>3. Re-links circle peers and SafeZone assignments to active node <code class="text-emerald-400">${newAgentId}</code>.</p>
                            <p>4. Emits master forensic audit entry into <code class="text-slate-300">Change_Log/Pairing_Devices</code>.</p>
                            <p>5. Safely archives and decommissions the deprecated <code class="text-rose-400">/devices/${oldAgentId}</code> root node.</p>
                        </div>
                    </div>

                    <div class="flex items-center justify-end gap-2.5 pt-2 border-t border-subtle-theme">
                        <button id="btn-modal-merge-cancel" class="btn-action-secondary text-xs py-1.5 px-4">
                            Cancel
                        </button>
                        <button id="btn-modal-merge-confirm" class="btn-action-primary text-xs py-1.5 px-5 bg-gradient-to-r from-amber-600 to-amber-500 text-slate-950 font-extrabold shadow-md flex items-center gap-1.5">
                            <span>📦 Confirm & Move All Details</span>
                        </button>
                    </div>

                </div>
            </div>
        `;

        const existing = document.getElementById('modal-dup-merge-backdrop');
        if (existing) existing.remove();

        document.body.insertAdjacentHTML('beforeend', modalHtml);

        const btnCancel = document.getElementById('btn-modal-merge-cancel');
        if (btnCancel) {
            btnCancel.onclick = () => {
                const m = document.getElementById('modal-dup-merge-backdrop');
                if (m) m.remove();
            };
        }

        const btnConfirm = document.getElementById('btn-modal-merge-confirm');
        if (btnConfirm) {
            btnConfirm.onclick = async () => {
                btnConfirm.disabled = true;
                btnConfirm.innerHTML = `<span class="w-3 h-3 border-2 border-slate-950 border-t-transparent rounded-full animate-spin"></span> Merging...`;
                await this.executeMerge(oldAgentId, newAgentId);
                const m = document.getElementById('modal-dup-merge-backdrop');
                if (m) m.remove();
            };
        }
    }

    /**
     * Executes merge API call
     */
    async executeMerge(oldAgentId, newAgentId) {
        this.isMerging = true;
        try {
            UIControls.notify(`Initiating multi-tier merge: ${oldAgentId} -> ${newAgentId}...`, 'info', { duration: 3000 });

            const res = await (window.fetchApi ? window.fetchApi('/api/merge-duplicate-devices', {
                method: 'POST',
                body: JSON.stringify({ oldAgentId, newAgentId })
            }) : fetch('/api/merge-duplicate-devices', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ oldAgentId, newAgentId })
            }).then(r => r.json()));

            if (res && res.error) {
                throw new Error(res.message || res.error);
            }

            UIControls.notify(`✅ Duplicate device ${oldAgentId} successfully merged into ${newAgentId}! Old data preserved under Old_Device_ID.`, 'success', { duration: 5000 });
            if (window.log) window.log(`[MERGE_SUCCESS] Merged ${oldAgentId} into ${newAgentId}. Archive: ${res.archivePath}`, "success");

            if (this.onMergeSuccess) {
                this.onMergeSuccess(oldAgentId, newAgentId);
            }

            // Reload duplicates list
            await this.loadDuplicates();
        } catch (err) {
            console.error("[DUP_VIEW] Merge failed:", err);
            UIControls.notify(`Merge failed: ${err.message}`, 'danger', { duration: 5000 });
            if (window.log) window.log(`[MERGE_FAILED] ${err.message}`, "error");
        } finally {
            this.isMerging = false;
        }
    }

    /**
     * Synchronizes a single missing heading into Old_Device_ID
     */
    async syncSingleHeading(oldAgentId, newAgentId, headingKey) {
        try {
            UIControls.notify(`Synchronizing heading [${headingKey}] for ${oldAgentId}...`, 'info', { duration: 2500 });

            const res = await (window.fetchApi ? window.fetchApi('/api/sync-heading', {
                method: 'POST',
                body: JSON.stringify({ oldAgentId, newAgentId, headingKey })
            }) : fetch('/api/sync-heading', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ oldAgentId, newAgentId, headingKey })
            }).then(r => r.json()));

            if (res && res.error) {
                throw new Error(res.message || res.error);
            }

            UIControls.notify(`✅ Heading [${headingKey}] synchronized! Both sides ticked YES.`, 'success', { duration: 4000 });
            
            // Update cached pair headingsComparison if present
            const pairKey = `${oldAgentId}_TO_${newAgentId}`;
            const existingPair = this.duplicates.find(p => p.pairId === pairKey);
            if (existingPair && res.headingsComparison) {
                existingPair.headingsComparison = res.headingsComparison;
            }

            await this.loadDuplicates();
        } catch (err) {
            console.error(`[DUP_VIEW] Sync heading [${headingKey}] failed:`, err);
            UIControls.notify(`Failed to sync heading: ${err.message}`, 'danger', { duration: 4500 });
        }
    }

    /**
     * Synchronizes all missing headings into Old_Device_ID
     */
    async syncAllMissingHeadings(oldAgentId, newAgentId) {
        try {
            UIControls.notify(`Synchronizing all missing headings for ${oldAgentId} ➔ ${newAgentId}...`, 'info', { duration: 3000 });

            const res = await (window.fetchApi ? window.fetchApi('/api/sync-all-missing-headings', {
                method: 'POST',
                body: JSON.stringify({ oldAgentId, newAgentId })
            }) : fetch('/api/sync-all-missing-headings', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ oldAgentId, newAgentId })
            }).then(r => r.json()));

            if (res && res.error) {
                throw new Error(res.message || res.error);
            }

            UIControls.notify(`✅ All headings synchronized into Old_Device_ID/${oldAgentId}! Full Parity Achieved.`, 'success', { duration: 5000 });

            // Update cached pair
            const pairKey = `${oldAgentId}_TO_${newAgentId}`;
            const existingPair = this.duplicates.find(p => p.pairId === pairKey);
            if (existingPair && res.headingsComparison) {
                existingPair.headingsComparison = res.headingsComparison;
            }

            await this.loadDuplicates();
        } catch (err) {
            console.error(`[DUP_VIEW] Sync all headings failed:`, err);
            UIControls.notify(`Failed to sync all headings: ${err.message}`, 'danger', { duration: 5000 });
        }
    }
}
