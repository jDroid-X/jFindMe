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
            'devices': ['name', 'role', 'model', 'status', 'battery', 'address', 'latitude', 'longitude', 'app_version', 'last_seen'],
            'Geofencing': ['zone_name', 'status', 'radius', 'lat', 'lng', 'creator_uid'],
            'safe_zones': ['zone_name', 'status', 'radius', 'lat', 'lng', 'creator_uid'],
            'relationships': ['group_name', 'owner_id', 'members', 'created_at'],
            'Version_Update': ['version_name', 'version_code', 'release_date', 'download_url', 'is_mandatory'],
            'History': ['timestamp', 'latitude', 'longitude', 'speed', 'address'],
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

        Store.subscribe((state) => {
            // Guard: Do not re-render or overwrite workspace if another view (Live Map, Dashboard, Device User, Geofencing) is active!
            if (window.appInstance && window.appInstance.currentView && 
                window.appInstance.currentView !== 'datagrid' && 
                window.appInstance.currentView !== 'audit') {
                return;
            }
            // Reset page if node changes
            if (this.lastNode !== state.navigation.activeNode) {
                this.currentPage = 1;
                this.lastNode = state.navigation.activeNode;
            }
            this.render();
        });
    }

    render() {
        // Guard: Prevent rendering DataGrid if active view is neither 'datagrid' nor 'audit'
        if (window.appInstance && window.appInstance.currentView && 
            window.appInstance.currentView !== 'datagrid' && 
            window.appInstance.currentView !== 'audit') {
            return;
        }
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
                    
                    <div class="flex items-center gap-3">
                        <div class="flex items-center gap-1 bg-white/5 border border-white/10 rounded-lg px-2 py-1 text-[10px] font-mono text-slate-300">
                            <span class="text-slate-500 font-bold uppercase">Size:</span>
                            <select onchange="window.dataGridInstance && window.dataGridInstance.setPageSize(this.value)" class="bg-transparent text-accent font-bold outline-none cursor-pointer">
                                <option value="5" ${this.pageSize === 5 ? 'selected' : ''} class="bg-slate-900 text-white">5</option>
                                <option value="10" ${this.pageSize === 10 ? 'selected' : ''} class="bg-slate-900 text-white">10</option>
                                <option value="25" ${this.pageSize === 25 ? 'selected' : ''} class="bg-slate-900 text-white">25</option>
                                <option value="50" ${this.pageSize === 50 ? 'selected' : ''} class="bg-slate-900 text-white">50</option>
                            </select>
                        </div>
                        <button onclick="window.dataGridInstance && window.dataGridInstance.showAddRecordModal()" class="flex items-center gap-1.5 p-2 px-3 bg-emerald-600/30 hover:bg-emerald-600 text-emerald-300 hover:text-white rounded-lg border border-emerald-500/30 transition-all text-[10px] font-bold uppercase tracking-wider">
                            <span>➕ Add Record</span>
                        </button>
                        <button onclick="window.dataGridInstance && window.dataGridInstance.exportCsv()" class="flex items-center gap-1.5 p-2 px-3 bg-white/5 hover:bg-white/10 rounded-lg border border-white/10 text-secondary hover:text-white transition-all text-[10px] font-bold uppercase tracking-wider">
                            <span>📥 CSV</span>
                        </button>
                        <button onclick="window.dataGridInstance && window.dataGridInstance.exportJson()" class="flex items-center gap-1.5 p-2 px-3 bg-white/5 hover:bg-white/10 rounded-lg border border-white/10 text-secondary hover:text-white transition-all text-[10px] font-bold uppercase tracking-wider">
                            <span>📥 JSON</span>
                        </button>
                        <button onclick="window.appInstance.loadNode('${state.navigation.activeNode}', ${this.pageSize}, window.appInstance.activeNodeType)" class="flex items-center gap-2 p-2 px-3 bg-white/5 hover:bg-white/10 rounded-lg border border-white/10 text-secondary transition-all text-[10px] font-bold uppercase tracking-widest">
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
                            Refresh
                        </button>

                        <div class="text-right pl-3 border-l border-white/10">
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
                                            const keys = Object.keys(val);
                                            const countLabel = Array.isArray(val) ? `[${keys.length} items]` : `{${keys.length} fields}`;
                                            return `
                                                <td class="p-3 border-r border-white/5 align-top">
                                                    <details class="group/tree inline-block">
                                                        <summary class="cursor-pointer select-none text-[10px] font-mono font-bold text-sky-400 hover:text-sky-300 inline-flex items-center gap-1.5 list-none bg-sky-950/40 px-2 py-0.5 rounded border border-sky-500/20">
                                                            <span class="inline-block transition-transform group-open/tree:rotate-90 text-[10px] text-sky-400 font-black">&gt;</span>
                                                            <span class="text-sky-300 font-mono text-[10px]">${countLabel}</span>
                                                        </summary>
                                                        <div class="mt-1.5 pl-2.5 border-l border-sky-500/30 space-y-1 bg-black/60 p-2 rounded max-h-48 overflow-y-auto custom-scrollbar">
                                                            ${keys.map(k => {
                                                                const subVal = val[k];
                                                                const isSubObj = subVal && typeof subVal === 'object';
                                                                const subDisplay = isSubObj ? (Array.isArray(subVal) ? `[${Object.keys(subVal).length} items]` : `{${Object.keys(subVal).length} fields}`) : subVal;
                                                                return `<div class="text-[9px] font-mono text-slate-300 flex items-start gap-1">
                                                                    <span class="text-sky-400 font-bold shrink-0">${k}:</span> 
                                                                    <span class="text-white/90 break-all">${subDisplay !== undefined ? subDisplay : '--'}</span>
                                                                </div>`;
                                                            }).join('')}
                                                        </div>
                                                    </details>
                                                </td>`;
                                        }

                                        const displayVal = val !== undefined && val !== null ? String(val) : '--';
                                        return `<td class="p-4 text-white/80 text-xs whitespace-nowrap border-r border-white/5 font-mono">${displayVal}</td>`;
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

        window.dataGridInstance = this;
    }

    exportCsv() {
        const state = Store.getState();
        const records = state.data.records || [];
        if (records.length === 0) {
            alert("No records to export.");
            return;
        }

        const columns = new Set();
        records.forEach(r => Object.keys(r).forEach(k => { if (!k.startsWith('_')) columns.add(k); }));
        const colList = Array.from(columns);

        let csv = colList.map(c => `"${c}"`).join(",") + "\n";
        records.forEach(row => {
            csv += colList.map(col => {
                const val = row[col] !== undefined ? String(row[col]).replace(/"/g, '""') : "";
                return `"${val}"`;
            }).join(",") + "\n";
        });

        const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.setAttribute("href", url);
        link.setAttribute("download", `${state.navigation.activeNode || "export"}_${Date.now()}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        if (window.log) window.log(`EXPORT_CSV: Exported ${records.length} records to CSV.`, "success");
    }

    exportJson() {
        const state = Store.getState();
        const records = state.data.records || [];
        if (records.length === 0) {
            alert("No records to export.");
            return;
        }

        const jsonStr = JSON.stringify(records, null, 2);
        const blob = new Blob([jsonStr], { type: "application/json" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.setAttribute("href", url);
        link.setAttribute("download", `${state.navigation.activeNode || "export"}_${Date.now()}.json`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        if (window.log) window.log(`EXPORT_JSON: Exported ${records.length} records to JSON.`, "success");
    }

    showAddRecordModal() {
        const state = Store.getState();
        const activeNode = state.navigation.activeNode;
        if (!activeNode) return;

        const records = state.data.records || [];
        const columns = new Set();
        records.forEach(r => Object.keys(r).forEach(k => { if (k !== 'id' && !k.startsWith('_')) columns.add(k); }));
        if (this.SCHEMA_COLS[activeNode]) {
            this.SCHEMA_COLS[activeNode].forEach(c => columns.add(c));
        }
        const colList = Array.from(columns);

        const modal = document.createElement('div');
        modal.className = 'fixed inset-0 z-[1100] flex items-center justify-center bg-black/85 backdrop-blur-md p-4 animate-fade-in font-sans';

        modal.innerHTML = `
            <div class="bg-slate-900 border border-white/10 rounded-2xl w-full max-w-xl shadow-2xl overflow-hidden text-white flex flex-col max-h-[90vh]">
                <div class="p-5 border-b border-white/10 flex items-center justify-between bg-slate-950/80">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400 text-lg">➕</div>
                        <div>
                            <h3 class="text-base font-black tracking-tight text-white uppercase">Add New Record</h3>
                            <p class="text-xs text-slate-400">Node: /${activeNode}</p>
                        </div>
                    </div>
                    <button id="btn-close-add-rec" class="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-white/5 transition-all text-xl font-mono">✕</button>
                </div>

                <div class="p-6 overflow-y-auto space-y-4 flex-1 custom-scrollbar">
                    <div>
                        <label class="text-[11px] font-bold uppercase tracking-wider text-sky-400 block mb-1">Record Key / Document ID *</label>
                        <input type="text" id="add-rec-id" placeholder="e.g. REC_${Date.now()}" value="REC_${Date.now()}"
                               class="w-full bg-slate-950 border border-white/10 rounded-lg p-2.5 text-xs text-white font-mono focus:border-sky-500 focus:outline-none">
                    </div>

                    <div class="space-y-3 pt-2 border-t border-white/5">
                        <span class="text-[11px] font-bold uppercase tracking-wider text-slate-300 block">Record Attributes</span>
                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-60 overflow-y-auto custom-scrollbar p-2 bg-black/40 rounded-xl border border-white/5">
                            ${colList.map(col => `
                                <div>
                                    <label class="text-[10px] text-slate-400 uppercase font-mono block mb-1 truncate">${col}</label>
                                    <input type="text" class="add-rec-field w-full bg-slate-950 border border-white/10 rounded-lg p-2 text-xs text-white focus:border-sky-500 focus:outline-none font-mono" data-key="${col}">
                                </div>
                            `).join('')}
                        </div>
                    </div>
                </div>

                <div class="p-4 border-t border-white/10 bg-slate-950 flex items-center justify-end gap-3">
                    <button id="btn-cancel-add-rec" class="px-4 py-2 rounded-xl text-xs font-bold text-slate-400 hover:text-white hover:bg-white/5 transition-all">Cancel</button>
                    <button id="btn-save-add-rec" class="px-6 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-black uppercase tracking-wider transition-all shadow-lg shadow-emerald-600/30">
                        Create Record
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        const close = () => modal.remove();
        modal.querySelector('#btn-close-add-rec').addEventListener('click', close);
        modal.querySelector('#btn-cancel-add-rec').addEventListener('click', close);

        modal.querySelector('#btn-save-add-rec').addEventListener('click', async () => {
            const id = modal.querySelector('#add-rec-id').value.trim();
            if (!id) {
                alert("Please specify a Record Key/ID.");
                return;
            }

            const data = {};
            modal.querySelectorAll('.add-rec-field').forEach(input => {
                const k = input.dataset.key;
                const v = input.value.trim();
                if (v !== "") {
                    // Try parsing numbers / booleans
                    if (!isNaN(v) && v !== "") data[k] = Number(v);
                    else if (v.toLowerCase() === "true") data[k] = true;
                    else if (v.toLowerCase() === "false") data[k] = false;
                    else data[k] = v;
                }
            });

            try {
                const nodeType = window.appInstance.activeNodeType || 'rtdb';
                await FBBridge.saveRecord(activeNode, id, data, nodeType);
                if (window.log) window.log(`RECORD_CREATED: Created [${id}] under /${activeNode}.`, "success");
                close();
                window.appInstance.loadNode(activeNode, this.pageSize, nodeType);
            } catch (e) {
                alert("Failed to create record: " + e.message);
            }
        });
    }

    setPageSize(size) {
        const val = parseInt(size, 10);
        if (!isNaN(val) && val > 0) {
            this.pageSize = val;
            const state = Store.getState();
            if (state.navigation.activeNode && window.appInstance) {
                window.appInstance.loadNode(state.navigation.activeNode, this.pageSize, window.appInstance.activeNodeType);
            }
        }
    }

    setPage(page) {
        this.currentPage = page;
        this.render();
    }
}
