import { Store } from '../core/Store.js';
import { FBBridge } from '../services/FirebaseService.js';

/**
 * jFindMe Matrix Manager V3 - Forensic DataGrid
 * Features: Multi-select, Bulk-Action, Real-time Sync, Analytical Telemetry
 */
export class DataGrid {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.currentPage = 1;
        this.pageSize = 15;
        this.SCHEMA_COLS = {
            'users': ['name', 'email', 'active_group', 'status', 'role', 'avatar_id'],
            'locations': ['lat', 'lng', 'batteryLevel', 'accuracy', 'timestamp'],
            'groups': ['name', 'host', 'members', 'description'],
            'history': ['timestamp', 'event_type', 'node_id', 'details'],
            'system_audit': ['admin_uid', 'action', 'target_path', 'ip', 'timestamp'],
            'system_settings': ['maintenance_mode', 'sync_interval_ms', 'global_broadcast'],
            'geofences': ['name', 'radius', 'lat', 'lng', 'status'],
            'avatars': ['asset_url', 'pixel_ratio', 'type', 'animations'],
            'sessions': ['hardware_id', 'last_login', 'ip', 'status'],
            'users_metadata': ['logs', 'preferences', 'last_seen']
        };

        Store.subscribe(() => {
            // Reset page if node changes
            const state = Store.getState();
            if (this.lastNode !== state.navigation.activeNode) {
                this.currentPage = 1;
                this.lastNode = state.navigation.activeNode;
            }
            this.render();
        });
    }

    render() {
        const state = Store.getState();
        let records = state.data.records || [];
        
        // --- RESTORED SEARCH FILTERING ---
        const searchTerm = (state.searchTerm || "").toLowerCase();
        if (searchTerm) {
            records = records.filter(r => 
                Object.values(r).some(v => String(v).toLowerCase().includes(searchTerm))
            );
        }
        
        // Show empty state if no records yet
        if (records.length === 0 && !state.navigation.activeNode) {
            this.container.innerHTML = `<div class="p-20 text-center text-secondary uppercase tracking-[1em] opacity-20">Awaiting Signal...</div>`;
            return;
        }
        const columns = new Set();
        
        // 1. Extract columns from live data
        records.forEach(r => Object.keys(r).forEach(k => { 
            if(k !== 'id' && !k.startsWith('_')) columns.add(k); 
        }));

        // 2. BACKFILL: Ensure 'Published Structure' is visible even if records are 0
        const activeNode = state.navigation.activeNode;
        if (activeNode && this.SCHEMA_COLS[activeNode]) {
            this.SCHEMA_COLS[activeNode].forEach(col => columns.add(col));
        }

        const colArray = Array.from(columns);

        // Fetch Background Stats for large nodes
        if (state.navigation.activeNode && !this._statsLoading) {
            this._statsLoading = true;
            const nodeType = window.appInstance.activeNodeType || 'rtdb';
            FBBridge.getNodeStats(state.navigation.activeNode, nodeType).then(res => {
                const el = document.getElementById('total-cloud-count');
                if (el) el.textContent = `/ ${res.total_records.toLocaleString()} TOTAL`;
                this._statsLoading = false;
            }).catch(() => this._statsLoading = false);
        }

        const selectionCount = state.data.selection.length;
        const lastSync = new Date().toLocaleTimeString();

        // --- SERVER-SIDE PAGINATION LOGIC ---
        // We use 5 records per page as requested by the user
        this.pageSize = 5; 
        const isHistory = state.navigation.activeNode === 'history' || (state.navigation.activeNode || '').startsWith('history/');
        
        // Use local page index for navigation
        const startIndex = (this.currentPage - 1) * this.pageSize;
        const pageRecords = records; // In server-side mode, Store.records ONLY contains the current page

        this.container.innerHTML = `
            <div class="data-grid-container flex flex-col h-full bg-main overflow-hidden">
                <!-- FORENSIC ANALYTICS BAR -->
                <div class="px-6 py-3 bg-black/40 border-b border-white/5 flex items-center justify-between">
                    <div class="flex items-center gap-6">
                        <div class="flex flex-col">
                            <span class="text-[9px] uppercase tracking-widest text-secondary font-bold flex items-center gap-2">
                                Node Inventory
                                <a href="https://console.firebase.google.com/u/0/project/${state.system.config.projectId}/database/realtime/data/${state.navigation.activeNode}" target="_blank" class="text-accent/40 hover:text-accent font-mono text-[8px] border border-accent/20 px-1 rounded bg-accent/5 transition-all">
                                    /${state.navigation.activeNode}
                                </a>
                            </span>
                            <span class="text-xl font-black text-accent">
                                ${records.length} 
                                <span id="total-cloud-count" class="text-[10px] text-white/30 font-normal">/ ... TOTAL</span>
                            </span>
                        </div>
                        <div class="h-8 w-[1px] bg-white/10"></div>
                        <div class="flex flex-col">
                            <span class="text-[9px] uppercase tracking-widest text-secondary font-bold flex items-center gap-2">
                                Structural Density
                                <span class="text-warning/40 font-mono text-[8px] border border-warning/20 px-1 rounded bg-warning/5 cursor-default">
                                    ${colArray.length}_DIMENSIONS
                                </span>
                            </span>
                            <span class="text-xl font-black text-warning">${colArray.length} <span class="text-[10px] text-white/30 font-normal">FIELDS</span></span>
                        </div>
                    </div>
                    
                    <div class="flex items-center gap-6">
                        <button onclick="window.appInstance.loadNode('${state.navigation.activeNode}', ${this.pageSize}, window.appInstance.activeNodeType)" class="flex items-center gap-2 p-2 px-4 bg-white/5 hover:bg-white/10 rounded-lg border border-white/10 text-secondary transition-all text-[10px] font-bold uppercase tracking-widest">
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
                            Reset_View
                        </button>

                        <div class="text-right">
                            <div class="text-[9px] uppercase tracking-widest text-secondary font-bold">Matrix Heartbeat</div>
                            <div class="text-[11px] font-mono text-success/80 animate-pulse">● LIVE SYNC: ${lastSync}</div>
                        </div>
                    </div>
                </div>

                <!-- MAIN DATA MATRIX -->
                <div class="flex-1 overflow-auto scrollbar-thin">
                    <table class="data-sheet w-full font-mono border-collapse">
                        <thead>
                            <tr class="bg-card/50 backdrop-blur-xl border-b border-border sticky top-0 z-20">
                                <th class="w-12 text-center p-4">
                                    <input type="checkbox" onchange="window.toggleAll(this.checked)">
                                </th>
                                <th class="text-left font-black uppercase tracking-widest text-[9px] text-accent/60 p-4 border-r border-white/5 w-24">ACTIONS</th>
                                <th class="text-left font-black uppercase tracking-widest text-[10px] text-accent/60 p-4 border-r border-white/5">Forensic ID</th>
                                ${colArray.map(col => `<th class="text-left font-black uppercase tracking-widest text-[10px] text-white/40 p-4 border-r border-white/5">${col}</th>`).join('')}
                            </tr>
                        </thead>
                        <tbody>
                            ${pageRecords.length > 0 ? pageRecords.map(row => `
                                <tr class="${state.data.selection.includes(row.id) ? 'bg-accent/10' : 'hover:bg-white/5'} transition-all border-b border-white/5">
                                    <td class="text-center p-3">
                                        <input type="checkbox" ${state.data.selection.includes(row.id) ? 'checked' : ''} onchange="window.toggleSelect('${row.id}')">
                                    </td>
                                    <td class="p-4 border-r border-white/5 whitespace-nowrap">
                                        <div class="flex items-center gap-4">
                                            <button onclick="window.appInstance.editor.open('${state.navigation.activeNode}', '${row.id}', ${JSON.stringify(row).replace(/"/g, '&quot;')})" 
                                                    class="text-accent/80 hover:text-accent hover:scale-125 transition-all" title="Edit Node">
                                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path></svg>
                                            </button>
                                            <button onclick="window.deleteNode('${row.id}')" class="text-red-500 hover:text-red-400 hover:scale-125 transition-all" title="Decommission Node">
                                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                                            </button>
                                        </div>
                                    </td>
                                    <td class="p-4 text-accent font-bold text-xs whitespace-nowrap border-r border-white/5">${row.id}</td>
                                    ${colArray.map(col => {
                                        const val = row[col];
                                        const isObject = val && typeof val === 'object';
                                        
                                        if (isObject) {
                                            return `
                                                <td class="p-4 border-r border-white/5">
                                                    <button 
                                                        onclick="window.openJsonEditor('${state.navigation.activeNode}', '${row.id}', '${col}')"
                                                        class="bg-accent/10 hover:bg-accent/20 text-accent text-[10px] px-2 py-0.5 rounded border border-accent/20 font-bold transition-all flex items-center gap-2"
                                                    >
                                                        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"></path></svg>
                                                        OBJECT_META
                                                    </button>
                                                </td>`;
                                        }

                                        const displayVal = val !== undefined ? val : '--';
                                        return `<td class="p-4 text-white/70 text-xs whitespace-nowrap border-r border-white/5">${displayVal}</td>`;
                                    }).join('')}
                                </tr>
                            `).join('') : `
                                <tr>
                                    <td colspan="${colArray.length + 3}" class="p-20 text-center">
                                        <div class="flex flex-col items-center gap-4 opacity-30">
                                            <svg class="w-12 h-12 text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M4 7V4a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v3M9 2v3M15 2v3M2 7h20v14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V7zM2 12h20M7 12v9M17 12v9"></path></svg>
                                            <div class="flex flex-col">
                                                <span class="text-xs font-black uppercase tracking-[0.3em]">No Active Nodes Found</span>
                                                <span class="text-[9px] font-mono text-secondary">Awaiting Cloud Provisioning... Structure Published.</span>
                                            </div>
                                            <button onclick="window.appInstance.loadNode('${state.navigation.activeNode}', ${this.pageSize}, window.appInstance.activeNodeType)" 
                                                    class="mt-4 px-4 py-2 border border-white/10 rounded hover:bg-white/5 transition-all text-[10px] uppercase font-bold text-accent/60">
                                                Retry Force Sync
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            `}
                        </tbody>
                    </table>
                </div>

                <!-- SERVER-SIDE PAGINATION CONTROLS -->
                <div class="px-6 py-3 bg-black/20 border-t border-white/5 flex items-center justify-between">
                    <div class="text-[10px] text-secondary font-bold tracking-widest">
                        BATCH SIZE: ${this.pageSize} | ${isHistory ? 'FORENSIC_STREAM_PAGINATION' : 'STANDARD_VIEW'}
                    </div>
                    <div class="flex items-center gap-2">
                        <button 
                            onclick="window.appInstance.loadPrevPage()"
                            ${this.currentPage === 1 ? 'disabled' : ''}
                            class="p-2 border border-white/10 rounded-lg hover:bg-white/5 disabled:opacity-20 transition-all flex items-center gap-2 text-[10px] uppercase font-black"
                        >
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M15 19l-7-7 7-7"></path></svg>
                            Prev
                        </button>
                        <div class="px-4 py-2 bg-accent/10 border border-accent/20 rounded-lg text-accent text-xs font-bold">
                            SECTOR ${this.currentPage}
                        </div>
                        <button 
                            onclick="window.appInstance.loadNextPage()"
                            ${pageRecords.length < this.pageSize ? 'disabled' : ''}
                            class="p-2 border border-white/10 rounded-lg hover:bg-white/5 disabled:opacity-20 transition-all flex items-center gap-2 text-[10px] uppercase font-black"
                        >
                            Next
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M9 5l7 7-7 7"></path></svg>
                        </button>
                    </div>
                </div>
            </div>
        `;

        // Bind Window Globals for Grid Interactivity
        window.gridInstance = this;
        window.toggleSelect = (id) => Store.dispatch('TOGGLE_SELECTION', id);
        window.toggleAll = (checked) => {
            records.forEach(r => {
                const isSelected = Store.getState().data.selection.includes(r.id);
                if (checked !== isSelected) Store.dispatch('TOGGLE_SELECTION', r.id);
            });
        };
        window.bulkDelete = async () => {
             const ids = Store.getState().data.selection;
             if (!confirm(`Engage Bulk Decommission on ${ids.length} nodes?`)) return;
             const nodeType = window.appInstance.activeNodeType || 'rtdb';
             for (const id of ids) {
                 await FBBridge.deleteRecord(`/${state.navigation.activeNode}/${id}`, nodeType);
             }
             Store.dispatch('SET_RECORDS', Store.getState().data.records.filter(r => !ids.includes(r.id)));
             Store.dispatch('TOGGLE_SELECTION', null);
             window.log(`BULK_DECOMMISSION: Purged ${ids.length} nodes from Cloud Matrix [${nodeType.toUpperCase()}].`, "success");
        };

        window.deleteNode = async (id) => {
            if (!confirm(`DECOMMISSION EXECUTABLE: Permanently erase forensic node [${id}]?`)) return;
            try {
                const nodeType = window.appInstance.activeNodeType || 'rtdb';
                await FBBridge.deleteRecord(`/${state.navigation.activeNode}/${id}`, nodeType);
                Store.dispatch('SET_RECORDS', Store.getState().data.records.filter(r => r.id !== id));
                window.log(`NODE_ERASED: [${id}] purged from [${state.navigation.activeNode}] [${nodeType.toUpperCase()}]`, "success");
            } catch (err) {
                window.log(`ERASE_FAILURE: ${err.message}`, "error");
            }
        };

        /**
         * NUCLEAR PURGE: Complete erasure of the entire active node.
         * Two-stage safety gate:
         *   1. Type-to-confirm the node name
         *   2. Final yes/no confirmation dialog
         */
        window.purgeEntireNode = async () => {
            const activeNode = Store.getState().navigation.activeNode;
            if (!activeNode) {
                window.log("PURGE_ABORTED: No active node selected.", "error");
                return;
            }

            // SAFETY GATE 1: Type-to-confirm
            const typed = prompt(
                `⚠️ NUCLEAR PURGE WARNING ⚠️\n\n` +
                `This will PERMANENTLY DELETE ALL DATA under:\n` +
                `   /${activeNode}\n\n` +
                `This action is IRREVERSIBLE.\n\n` +
                `Type the node name "${activeNode}" to confirm:`
            );

            if (!typed || typed.trim() !== activeNode) {
                window.log("PURGE_ABORTED: Confirmation mismatch — operation cancelled.", "warning");
                return;
            }

            // SAFETY GATE 2: Final confirmation
            if (!confirm(`FINAL CONFIRMATION:\n\nPurge ALL data from /${activeNode}?\n\nClick OK to execute.`)) {
                window.log("PURGE_ABORTED: User cancelled final confirmation.", "warning");
                return;
            }

            // Execute purge
            const btn = document.getElementById('btn-purge-all');
            if (btn) {
                btn.disabled = true;
                btn.innerHTML = `<svg class="w-3.5 h-3.5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg> PURGING...`;
            }

            try {
                const nodeType = window.appInstance.activeNodeType || 'rtdb';
                window.log(`NUCLEAR_PURGE: Initiating complete erasure of /${activeNode} [${nodeType.toUpperCase()}]...`, "warning");
                const result = await FBBridge.purgeNode(activeNode, nodeType);

                // Clear local state
                Store.dispatch('SET_RECORDS', []);

                window.log(
                    `PURGE_COMPLETE: /${activeNode} — ` +
                    `${result.purgedChildren} top-level nodes, ` +
                    `~${result.purgedRecordsEstimate} total records erased. ` +
                    `Timestamp: ${result.timestamp}`,
                    "success"
                );

                // Show terminal to display the audit trail
                document.getElementById('terminal').style.display = 'block';
            } catch (err) {
                window.log(`PURGE_FAILURE: ${err.message}`, "error");
            } finally {
                if (btn) {
                    btn.disabled = false;
                    btn.innerHTML = `<svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z"></path></svg> ☢ PURGE ALL DATA`;
                }
            }
        };
    }

    setPage(page) {
        this.currentPage = page;
        this.render();
    }
}
