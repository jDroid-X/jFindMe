import { FeedbackDialog } from '../components/FeedbackDialog.js';
import { UIControls } from '../components/UIControls.js';

/**
 * jFindMe Matrix - Device Decommission & Erasure Modal View (Dynamic OOPS Architecture)
 * Provides two distinct decommission workflows:
 * 1. ARCHIVE: Staged cold-storage isolation into /database_archive/devices/{agentId}.
 *             Device is removed from active radar and rosters, hidden until "Archive Log" is checked.
 * 2. CLEANSLATE: Scans all database tables and cloud indices, outputs a structured Log Report Table,
 *                and permanently drops all records across 9 distinct systems upon confirmation.
 */
export class DeviceDeleteModalView {
    constructor(options = {}) {
        this.onComplete = options.onComplete || null;
        this.modalElement = null;
        this.selectedDevice = null;
        this.allDevices = [];
        this.currentMode = 'ARCHIVE'; // 'ARCHIVE' | 'CLEANSLATE'
        this.previewData = null;
        this.isLoadingPreview = false;
        this.isExecuting = false;
        this.isCleanSlateConfirmed = false;
    }

    /**
     * Open the Decommission Modal
     * @param {Object} targetDevice
     * @param {Array} allDevices
     * @param {string} initialMode
     */
    show(targetDevice, allDevices = [], initialMode = 'ARCHIVE') {
        this.close();

        this.selectedDevice = targetDevice || (allDevices && allDevices[0]) || null;
        this.allDevices = allDevices || [];
        this.currentMode = initialMode;
        this.previewData = null;
        this.isLoadingPreview = false;
        this.isExecuting = false;
        this.isCleanSlateConfirmed = false;

        if (!this.selectedDevice) {
            UIControls.notify('No device selected for decommission.', 'warning');
            return;
        }

        const modal = document.createElement('div');
        modal.id = 'device-delete-everywhere-modal';
        // Agent 3 Mandate: Right-Side Slide-Out Drawer with full viewport height & backdrop dismissal
        modal.className = 'fixed inset-0 z-[1200] flex justify-end bg-black/85 backdrop-blur-md animate-fade-in font-sans overflow-hidden';

        this.modalElement = modal;
        document.body.appendChild(modal);

        this._escapeHandler = (e) => {
            if (e.key === 'Escape') this.close();
        };
        window.addEventListener('keydown', this._escapeHandler);

        this.renderModalContent();

        // If CleanSlate is initial mode, fetch preview immediately
        if (this.currentMode === 'CLEANSLATE') {
            this.fetchPreviewReport();
        }
    }

    /**
     * Render or refresh modal DOM
     */
    renderModalContent() {
        if (!this.modalElement || !this.selectedDevice) return;

        const dev = this.selectedDevice;
        const agentId = dev.agentId || dev.deviceId || 'UNKNOWN';
        const displayName = dev.name || dev.deviceName || dev.profile?.name || agentId;
        const model = dev.model || dev.device?.model || 'Android Device';
        const avatarUrl = typeof dev.getAvatarUrl === 'function' ? dev.getAvatarUrl() : '/assets/avatars/1.png';
        const ragColor = typeof dev.getRagColor === 'function' ? dev.getRagColor() : '#22C55E';

        this.modalElement.innerHTML = `
            <div class="card-theme border-l border-subtle-theme w-full max-w-2xl h-full shadow-2xl overflow-hidden text-primary-theme flex flex-col transform transition-transform duration-300 translate-x-0">
                
                <!-- HEADER BAR -->
                <div class="p-3.5 border-b border-subtle-theme flex items-center justify-between surface-theme shrink-0">
                    <div class="flex items-center gap-2.5">
                        <div class="w-9 h-9 rounded-xl ${this.currentMode === 'CLEANSLATE' ? 'bg-rose-500/20 text-rose-400 border border-rose-500/40' : 'bg-amber-500/20 text-amber-400 border border-amber-500/40'} flex items-center justify-center text-lg font-bold">
                            ${this.currentMode === 'CLEANSLATE' ? '⚡' : '📦'}
                        </div>
                        <div>
                            <h3 class="text-sm font-extrabold tracking-tight text-primary-theme uppercase flex items-center gap-2">
                                <span>Decommission Device Everywhere</span>
                                <span class="badge-status ${this.currentMode === 'CLEANSLATE' ? 'badge-danger' : 'badge-warning'} text-[8px] px-1.5 py-0.2">
                                    ${this.currentMode === 'CLEANSLATE' ? 'PERMANENT ERASURE' : 'COLD-STORAGE ARCHIVE'}
                                </span>
                            </h3>
                            <p class="text-[11px] text-secondary-theme font-mono">
                                Multi-Table Lifecycle Controller • Closed-Loop Database Synchronization
                            </p>
                        </div>
                    </div>
                    <button id="btn-close-decom-modal" class="text-muted-theme hover:text-primary-theme p-1.5 rounded-lg text-lg font-mono transition-colors">
                        ✕
                    </button>
                </div>

                <!-- BODY -->
                <div class="p-4 overflow-y-auto space-y-4 flex-1 custom-scrollbar">

                    <!-- TARGET DEVICE DOSSIER STRIP -->
                    <div class="p-3 rounded-xl surface-theme border border-subtle-theme flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-inner">
                        <div class="flex items-center gap-3">
                            <div class="relative shrink-0">
                                <img src="${avatarUrl}" alt="${displayName}" class="w-11 h-11 rounded-full object-cover border-2 shadow-sm" style="border-color: ${ragColor}">
                                <span class="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full border border-surface" style="background-color: ${ragColor}"></span>
                            </div>
                            <div class="min-w-0">
                                <div class="flex items-center gap-2">
                                    <h4 class="text-sm font-extrabold text-primary-theme truncate">${displayName}</h4>
                                    <span class="badge-status badge-accent text-[8px] px-1 py-0.2">${model}</span>
                                </div>
                                <div class="text-[10px] text-secondary-theme font-mono mt-0.5 flex flex-wrap items-center gap-2">
                                    <span>Agent ID: <strong class="text-sky-400 font-bold">${agentId}</strong></span>
                                    <span>•</span>
                                    <span>Hardware: <span class="font-mono text-slate-300">${dev.deviceId || agentId}</span></span>
                                </div>
                            </div>
                        </div>

                        ${this.allDevices.length > 1 ? `
                        <div class="shrink-0 flex items-center gap-1.5">
                            <label class="text-[9px] font-mono text-secondary-theme uppercase">Switch Node:</label>
                            <select id="decom-target-device-select" class="input-theme py-1 px-2 text-[10px] font-mono rounded-lg border-subtle-theme bg-surface">
                                ${this.allDevices.map(d => `
                                    <option value="${d.agentId}" ${d.agentId === agentId ? 'selected' : ''}>
                                        ${d.name || d.agentId} (${d.agentId})
                                    </option>
                                `).join('')}
                            </select>
                        </div>
                        ` : ''}
                    </div>

                    <!-- DECOMMISSION MODE RADIO SELECTOR -->
                    <div class="space-y-1.5">
                        <label class="text-[10px] font-black uppercase tracking-wider text-sky-400 block font-mono">
                            Select Decommissioning Pipeline:
                        </label>
                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            
                            <!-- OPTION 1: ARCHIVE -->
                            <label class="relative flex flex-col p-3 rounded-xl border-2 cursor-pointer transition-all ${this.currentMode === 'ARCHIVE' ? 'border-amber-500 bg-amber-500/10 shadow-lg shadow-amber-500/10' : 'border-subtle-theme surface-theme hover:border-amber-400/40'}">
                                <div class="flex items-start justify-between">
                                    <div class="flex items-center gap-2">
                                        <input type="radio" name="decom-radio-mode" value="ARCHIVE" ${this.currentMode === 'ARCHIVE' ? 'checked' : ''} class="w-4 h-4 text-amber-500 focus:ring-0 cursor-pointer">
                                        <span class="text-xs font-black text-amber-400 uppercase">📦 Archive Database</span>
                                    </div>
                                    <span class="badge-status badge-warning text-[7px] px-1 py-0.2">SAFE & REVERSIBLE</span>
                                </div>
                                <p class="text-[10px] text-secondary-theme mt-2 leading-relaxed">
                                    Backs up entire device dossier, GPS historical tracks, and safe zones into the dedicated <strong class="text-amber-300">/database_archive</strong> bucket.
                                </p>
                                <div class="mt-2 text-[8.5px] font-mono text-amber-300/80 bg-amber-500/10 p-1.5 rounded border border-amber-500/20">
                                    🔒 Device is removed from active radar & map. Stays completely hidden until <strong>[✓] Archive Log</strong> is checked.
                                </div>
                            </label>

                            <!-- OPTION 2: CLEANSLATE -->
                            <label class="relative flex flex-col p-3 rounded-xl border-2 cursor-pointer transition-all ${this.currentMode === 'CLEANSLATE' ? 'border-rose-500 bg-rose-500/10 shadow-lg shadow-rose-500/10' : 'border-subtle-theme surface-theme hover:border-rose-400/40'}">
                                <div class="flex items-start justify-between">
                                    <div class="flex items-center gap-2">
                                        <input type="radio" name="decom-radio-mode" value="CLEANSLATE" ${this.currentMode === 'CLEANSLATE' ? 'checked' : ''} class="w-4 h-4 text-rose-500 focus:ring-0 cursor-pointer">
                                        <span class="text-xs font-black text-rose-400 uppercase">⚡ CleanSlate Deletion</span>
                                    </div>
                                    <span class="badge-status badge-danger text-[7px] px-1 py-0.2">PERMANENT DELETION</span>
                                </div>
                                <p class="text-[10px] text-secondary-theme mt-2 leading-relaxed">
                                    Scans and permanently purges device references across all 9 database tables, inverted lookup indices, safe zones, and telemetry streams.
                                </p>
                                <div class="mt-2 text-[8.5px] font-mono text-rose-300/80 bg-rose-500/10 p-1.5 rounded border border-rose-500/20">
                                    ⚠️ Shows comprehensive database tables audit log report below. Irreversible with zero recovery.
                                </div>
                            </label>

                        </div>
                    </div>

                    <!-- DYNAMIC WORKFLOW SECTION -->
                    ${this.currentMode === 'ARCHIVE' ? this.renderArchiveWorkflowSection(agentId, displayName) : this.renderCleanSlateWorkflowSection(agentId, displayName)}

                </div>

                <!-- FOOTER ACTIONS -->
                <div class="p-3.5 border-t border-subtle-theme flex items-center justify-between surface-theme shrink-0">
                    <button id="btn-cancel-decom" class="btn-action-secondary text-xs py-1.5 px-3">
                        ✕ Cancel
                    </button>

                    <div class="flex items-center gap-2">
                        ${this.currentMode === 'ARCHIVE' ? `
                            <button id="btn-confirm-archive" class="btn-action-primary text-xs py-2 px-4 bg-gradient-to-r from-amber-600 to-amber-500 hover:from-amber-500 hover:to-amber-400 text-slate-950 font-black shadow-md flex items-center gap-1.5 cursor-pointer" ${this.isExecuting ? 'disabled' : ''}>
                                ${this.isExecuting ? '<span class="animate-spin text-sm">🔄</span> Saving to Archive...' : '<span>📦 Move to Archive Database</span>'}
                            </button>
                        ` : `
                            <button id="btn-confirm-cleanslate" class="btn-action-danger text-xs py-2 px-5 ${this.isCleanSlateConfirmed ? 'bg-gradient-to-r from-rose-600 to-red-600 hover:from-rose-500 hover:to-red-500 text-white font-black shadow-lg shadow-rose-900/30' : 'bg-rose-950/40 hover:bg-rose-900/60 text-rose-300 border border-rose-500/40 font-bold'} flex items-center gap-2 transition-all cursor-pointer">
                                ${this.isExecuting ? '<span class="animate-spin text-sm">🔄</span> Purging Tables...' : '<span>⚠️ Confirm Permanent Delete</span>'}
                            </button>
                        `}
                    </div>
                </div>

            </div>
        `;

        this.bindModalEvents();
    }

    /**
     * Render Archive Workflow Staging Details
     */
    renderArchiveWorkflowSection(agentId, displayName) {
        return `
            <div class="p-3 rounded-xl border border-amber-500/30 bg-amber-500/5 space-y-2.5 animate-fade-in">
                <div class="flex items-center justify-between border-b border-amber-500/20 pb-1.5">
                    <span class="text-[11px] font-black uppercase text-amber-400 tracking-wider flex items-center gap-1.5">
                        <span>📦 Cold Storage Archive Staging Target</span>
                    </span>
                    <span class="badge-status badge-warning text-[7.5px] px-1.5 py-0.2">ISOLATED DATABASE</span>
                </div>

                <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[10px] font-mono">
                    <div class="p-2 rounded-lg bg-surface/60 border border-subtle-theme space-y-1">
                        <span class="text-secondary-theme text-[9px] block">Target Cloud Entity:</span>
                        <span class="text-amber-300 font-bold block truncate">/database_archive/devices/${agentId}</span>
                        <span class="text-muted-theme text-[8.5px] block">Includes profile, telemetry metadata, and full history breadcrumbs.</span>
                    </div>

                    <div class="p-2 rounded-lg bg-surface/60 border border-subtle-theme space-y-1">
                        <span class="text-secondary-theme text-[9px] block">Index Summary Ledger:</span>
                        <span class="text-amber-300 font-bold block truncate">/database_archive/summary/${agentId}</span>
                        <span class="text-muted-theme text-[8.5px] block">Feeds the fast-access Archive Log toggle in the master roster.</span>
                    </div>
                </div>

                <div class="text-[9.5px] text-secondary-theme leading-relaxed bg-amber-950/20 p-2 rounded-lg border border-amber-500/20 flex items-start gap-2">
                    <span class="text-amber-400 text-sm">ℹ️</span>
                    <div>
                        <strong>Visibility Assurance:</strong> Moving <strong>${displayName}</strong> to archive immediately detaches it from active radar viewports and live polling. It will remain completely hidden from everyone until you check the <strong class="text-amber-400">[✓] Archive Log</strong> checkbox in the device roster.
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Render CleanSlate Workflow Log Report Table
     */
    renderCleanSlateWorkflowSection(agentId, displayName) {
        if (this.isLoadingPreview) {
            return `
                <div class="p-8 rounded-xl border border-subtle-theme surface-theme flex flex-col items-center justify-center space-y-3 text-center animate-fade-in">
                    <div class="w-8 h-8 rounded-full border-2 border-rose-500 border-t-transparent animate-spin"></div>
                    <span class="text-xs font-mono font-bold text-rose-400">Scanning all 9 Database Tables & Cloud Roots for [${agentId}]...</span>
                    <span class="text-[10px] text-secondary-theme font-mono">Auditing RTDB master registries, GPS tracks, indices, geofences, and remote purge command dispatches.</span>
                </div>
            `;
        }

        if (!this.previewData || !this.previewData.targets) {
            return `
                <div class="p-4 rounded-xl border border-rose-500/30 bg-rose-500/10 text-center space-y-2 animate-fade-in">
                    <span class="text-xs font-bold text-rose-400 block">Database Scan Report Pending</span>
                    <button id="btn-re-scan-cleanslate" class="btn-action-danger text-[10px] py-1 px-3">
                        🔍 Scan Database Tables Now
                    </button>
                </div>
            `;
        }

        const targets = this.previewData.targets || [];
        const totalRecords = targets.reduce((sum, t) => sum + (t.recordsCount || 1), 0);

        return `
            <div class="space-y-3 animate-fade-in font-sans">
                <!-- Log Report Header Summary -->
                <div class="flex flex-col sm:flex-row sm:items-center justify-between p-3 rounded-xl bg-rose-500/10 dark:bg-rose-500/15 border border-rose-500/30 gap-2">
                    <div class="flex items-center gap-2.5">
                        <span class="text-xl text-rose-500 font-bold">⚡</span>
                        <div>
                            <span class="text-xs font-black text-rose-600 dark:text-rose-400 uppercase tracking-tight">Database Deletion Log Report</span>
                            <p class="text-[11px] text-secondary-theme font-mono">
                                References identified across <strong>${targets.length} database tables / cloud entities</strong> (<span class="text-rose-600 dark:text-rose-400 font-bold">${totalRecords} records</span> targeted for drop).
                            </p>
                        </div>
                    </div>
                    <span class="badge-status badge-danger text-[9px] px-2.5 py-1 font-bold font-mono tracking-wider self-start sm:self-center">
                        IRREVERSIBLE
                    </span>
                </div>

                <!-- Structured Database Tables Log Report (High Contrast & Clear Readability) -->
                <div class="border border-subtle-theme rounded-xl overflow-hidden shadow-md surface-theme">
                    <div class="overflow-x-auto max-h-72 custom-scrollbar">
                        <table class="w-full text-left border-collapse text-xs font-mono">
                            <thead class="bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 uppercase text-[10px] font-black border-b border-subtle-theme sticky top-0 z-10 shadow-sm">
                                <tr>
                                    <th class="p-3 w-1/4">Database & Table</th>
                                    <th class="p-3 w-5/12">Cloud Target Path (RTDB Online SSOT)</th>
                                    <th class="p-3 text-center w-20">Records</th>
                                    <th class="p-3">Category & Operational Impact</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-subtle-theme">
                                ${targets.map((t, idx) => {
                                    const isRtdb = (t.system || '').toLowerCase().includes('firebase');
                                    const sysBadge = isRtdb 
                                        ? 'bg-sky-500/15 text-sky-700 dark:text-sky-300 border border-sky-500/30' 
                                        : 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 border border-emerald-500/30';
                                    return `
                                        <tr class="transition-colors hover:bg-rose-50/70 dark:hover:bg-rose-950/25 ${idx % 2 === 0 ? 'bg-white/80 dark:bg-slate-900/40' : 'bg-slate-50/60 dark:bg-slate-900/70'}">
                                            <td class="p-3 align-top font-bold text-slate-900 dark:text-slate-100">
                                                <div class="flex items-center gap-1.5 flex-wrap">
                                                    <span class="text-rose-500 font-bold">•</span>
                                                    <span class="text-xs font-extrabold tracking-tight">${t.table}</span>
                                                </div>
                                                <span class="inline-block mt-1 text-[9px] font-mono px-1.5 py-0.5 rounded ${sysBadge}">
                                                    ${t.system}
                                                </span>
                                            </td>
                                            <td class="p-3 align-top">
                                                <div class="bg-slate-100 dark:bg-slate-950/80 px-2 py-1 rounded border border-subtle-theme text-[11px] font-mono text-sky-700 dark:text-sky-300 break-all select-all font-semibold" title="Full database path">
                                                    ${t.path}
                                                </div>
                                            </td>
                                            <td class="p-3 align-top text-center">
                                                <span class="inline-block px-2 py-0.5 rounded-full text-xs font-bold font-mono bg-rose-500/15 text-rose-600 dark:text-rose-400 border border-rose-500/30">
                                                    ${t.recordsCount}
                                                </span>
                                            </td>
                                            <td class="p-3 align-top text-slate-700 dark:text-slate-300 text-[11px] font-sans leading-relaxed">
                                                ${t.description}
                                            </td>
                                        </tr>
                                    `;
                                }).join('')}
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Confirmation Ticbox Container with Visual Highlight State -->
                <div id="cleanslate-confirm-box" class="p-3 rounded-xl bg-slate-50 dark:bg-slate-900/60 border-2 border-rose-500/40 flex items-start sm:items-center gap-3 transition-all shadow-sm">
                    <input type="checkbox" id="check-confirm-cleanslate" ${this.isCleanSlateConfirmed ? 'checked' : ''} class="w-5 h-5 rounded text-rose-600 focus:ring-0 cursor-pointer mt-0.5 sm:mt-0 shrink-0">
                    <label for="check-confirm-cleanslate" class="text-xs text-primary-theme font-medium cursor-pointer select-none leading-relaxed">
                        I have audited the database log report above and confirm the irreversible permanent deletion of <strong class="text-rose-600 dark:text-rose-400 underline decoration-rose-500/50">${displayName} (${agentId})</strong> across all systems.
                    </label>
                </div>
            </div>
        `;
    }

    /**
     * Bind dynamic listeners
     */
    bindModalEvents() {
        if (!this.modalElement) return;

        // Close Buttons
        this.modalElement.querySelector('#btn-close-decom-modal')?.addEventListener('click', () => this.close());
        this.modalElement.querySelector('#btn-cancel-decom')?.addEventListener('click', () => this.close());

        // Target Device Select Switch
        const selectEl = this.modalElement.querySelector('#decom-target-device-select');
        if (selectEl) {
            selectEl.addEventListener('change', (e) => {
                const targetId = e.target.value;
                const newDev = this.allDevices.find(d => d.agentId === targetId);
                if (newDev) {
                    this.selectedDevice = newDev;
                    this.previewData = null;
                    this.isCleanSlateConfirmed = false;
                    this.renderModalContent();
                    if (this.currentMode === 'CLEANSLATE') {
                        this.fetchPreviewReport();
                    }
                }
            });
        }

        // Mode Radio Buttons
        this.modalElement.querySelectorAll('input[name="decom-radio-mode"]').forEach(radio => {
            radio.addEventListener('change', (e) => {
                this.currentMode = e.target.value;
                this.isCleanSlateConfirmed = false;
                this.renderModalContent();
                if (this.currentMode === 'CLEANSLATE' && !this.previewData) {
                    this.fetchPreviewReport();
                }
            });
        });

        // CleanSlate Rescan
        this.modalElement.querySelector('#btn-re-scan-cleanslate')?.addEventListener('click', () => {
            this.fetchPreviewReport();
        });

        // Confirmation Checkbox (CleanSlate)
        const confirmCheck = this.modalElement.querySelector('#check-confirm-cleanslate');
        const confirmBtn = this.modalElement.querySelector('#btn-confirm-cleanslate');
        if (confirmCheck) {
            confirmCheck.addEventListener('change', (e) => {
                this.isCleanSlateConfirmed = e.target.checked;
                if (confirmBtn) {
                    if (this.isCleanSlateConfirmed) {
                        confirmBtn.className = "btn-action-danger text-xs py-2 px-5 bg-gradient-to-r from-rose-600 to-red-600 hover:from-rose-500 hover:to-red-500 text-white font-black shadow-lg shadow-rose-900/30 flex items-center gap-2 transition-all cursor-pointer transform hover:scale-[1.02]";
                    } else {
                        confirmBtn.className = "btn-action-danger text-xs py-2 px-5 bg-rose-950/40 hover:bg-rose-900/60 text-rose-300 border border-rose-500/40 font-bold flex items-center gap-2 transition-all cursor-pointer";
                    }
                }
            });
        }

        // Confirm Archive Execution
        this.modalElement.querySelector('#btn-confirm-archive')?.addEventListener('click', () => {
            this.executeDecommission('ARCHIVE');
        });

        // Confirm CleanSlate Execution (with active verification feedback)
        this.modalElement.querySelector('#btn-confirm-cleanslate')?.addEventListener('click', (e) => {
            e.preventDefault();
            if (this.isExecuting) return;

            if (!this.previewData || !this.previewData.targets) {
                UIControls.notify('Please wait for the database scan to complete before confirming deletion.', 'warning');
                return;
            }

            if (!this.isCleanSlateConfirmed) {
                const checkContainer = this.modalElement.querySelector('#cleanslate-confirm-box');
                if (checkContainer) {
                    checkContainer.classList.add('ring-4', 'ring-rose-500', 'bg-rose-500/20');
                    setTimeout(() => {
                        checkContainer?.classList.remove('ring-4', 'ring-rose-500', 'bg-rose-500/20');
                    }, 1400);
                }
                FeedbackDialog.showSlideNotification({
                    title: 'Verification Required',
                    message: `Please tick the verification checkbox: "I have audited the database log report..." before executing permanent deletion.`,
                    type: 'warning',
                    timeout: 4500
                });
                return;
            }

            this.executeDecommission('CLEANSLATE');
        });
    }

    /**
     * Fetch Database Tables Log Report via Preview API
     */
    async fetchPreviewReport() {
        if (!this.selectedDevice) return;
        const agentId = this.selectedDevice.agentId;
        this.isLoadingPreview = true;
        this.renderModalContent();

        try {
            const res = await (window.fetchApi ? window.fetchApi('/api/device/delete-everywhere', {
                method: 'POST',
                body: JSON.stringify({ agentId, mode: 'CLEANSLATE', preview: true })
            }) : fetch('/api/device/delete-everywhere', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ agentId, mode: 'CLEANSLATE', preview: true })
            }).then(r => r.json()));

            this.previewData = res;
        } catch (err) {
            UIControls.notify(`Database scan failed: ${err.message}`, 'danger');
            this.previewData = null;
        } finally {
            this.isLoadingPreview = false;
            this.renderModalContent();
        }
    }

    /**
     * Execute Permanent Decommission or Archive Staging
     */
    async executeDecommission(mode) {
        if (!this.selectedDevice || this.isExecuting) return;
        const agentId = this.selectedDevice.agentId;
        const displayName = this.selectedDevice.name || agentId;

        this.isExecuting = true;
        this.renderModalContent();

        try {
            const res = await (window.fetchApi ? window.fetchApi('/api/device/delete-everywhere', {
                method: 'POST',
                body: JSON.stringify({ agentId, mode, preview: false })
            }) : fetch('/api/device/delete-everywhere', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ agentId, mode, preview: false })
            }).then(r => r.json()));

            if (res && res.error) {
                throw new Error(res.error || res.message || 'Operation failed');
            }

            if (mode === 'ARCHIVE') {
                FeedbackDialog.showSlideNotification({
                    title: 'Device Archived',
                    message: `[${displayName}] moved to archive database. It is hidden from live radar until 'Archive Log' is checked.`,
                    type: 'warning',
                    timeout: 4500
                });
            } else {
                FeedbackDialog.showSlideNotification({
                    title: 'CleanSlate Purge Complete',
                    message: `[${displayName}] (${agentId}) was permanently erased from all database tables and cloud indices.`,
                    type: 'success',
                    timeout: 4500
                });
            }

            this.close();

            if (typeof this.onComplete === 'function') {
                await this.onComplete(mode, agentId);
            }

        } catch (err) {
            this.isExecuting = false;
            this.renderModalContent();
            FeedbackDialog.showSlideNotification({
                title: 'Decommission Failed',
                message: err.message,
                type: 'danger',
                timeout: 5000
            });
        }
    }

    /**
     * Close and destroy modal
     */
    close() {
        if (this._escapeHandler) {
            window.removeEventListener('keydown', this._escapeHandler);
            this._escapeHandler = null;
        }
        if (this.modalElement) {
            this.modalElement.remove();
            this.modalElement = null;
        }
    }
}
