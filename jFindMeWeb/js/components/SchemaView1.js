import { Store } from '../core/Store.js';
import { FBBridge } from '../services/FirebaseService.js';
import { SchemaView } from './SchemaView.js';

/**
 * jFindMe Matrix Manager V3 - Hierarchical Database Architect V2
 * Arrangement: Relational Vertical Waterfall (1:Many Hierarchy)
 */
export class SchemaView1 extends SchemaView {
    updateUI() {
        const filtered = this.getFilteredData() || { users: [], groups: [], locations: [], history: [], pairing_requests: [] };
        
        // Configuration: Define relational anchors with explicit Forensic Link Fields (FK)
        const nodeConfigs = [
            { id: 'users', label: 'Identity Hub [HUB]', linkField: 'id', fields: ['id', 'name', 'email', 'avatar_id', 'role', 'status'], data: filtered.users || [], x: 650, y: 50 },
            { id: 'pairing_requests', label: 'Pairing [LEFT]', linkField: 'fromUID', fields: ['id', 'fromUID', 'toEmail', 'status', 'timestamp'], data: filtered.pairing_requests || [], x: 250, y: 20 },
            { id: 'groups', label: 'Group Join [LEFT]', linkField: 'host', fields: ['id', 'name', 'host', 'members', 'description'], data: filtered.groups || [], x: 250, y: 220 },
            { id: 'locations', label: 'Telemetry [RIGHT]', linkField: 'id', fields: ['id', 'lat', 'lng', 'timestamp', 'accuracy'], data: filtered.locations || [], x: 1050, y: 20 },
            { id: 'history', label: 'Forensic [RIGHT]', linkField: 'id', fields: ['id', 'timestamp', 'event_type', 'node_id'], data: filtered.history || [], x: 1050, y: 220 }
        ];

        // Dynamic Relational Waterfall: Sort by join density for the Matrix Probe
        const sortedMatrixNodes = [...nodeConfigs].sort((a, b) => b.data.length - a.data.length);

        const getFieldY = (nodeId, fieldName) => {
            const node = nodeConfigs.find(n => n.id === nodeId);
            if (!node) return 0;
            const fieldIdx = node.fields.indexOf(fieldName);
            return node.y + 28 + (Math.max(fieldIdx, 0) * 18) + 9;
        };

        this.container.innerHTML = `
            <style>
                .join-path { transition: all 0.3s ease; stroke: #f59e0b; stroke-opacity: 0.3; stroke-width: 1.5; fill: none; }
                .anchor-point { fill: #f59e0b; }
                .schema-node { border: 1px solid #333; box-shadow: 2px 2px 10px rgba(0,0,0,0.5); background: #0d1117; transition: border-color 0.2s; }
                .schema-node:hover { border-color: #f59e0b; }
                .schema-node-header { background: #1a1f26; border-bottom: 1px solid #30363d; color: #8b949e; }
                .field-row { height: 18px; font-family: 'JetBrains Mono', monospace; font-size: 10px; }
                .matrix-table th { border-color: rgba(255,255,255,0.05); }
                .matrix-table td { border-color: rgba(255,255,255,0.05); height: 32px; }
            </style>
            <div class="query-builder flex flex-col min-h-full bg-main overflow-y-auto text-white font-sans scrollbar-thin">
                <!-- ROW 1: COMPACT RELATIONSHIP MATRIX (MAXIMIZED) -->
                <div class="h-auto border-b border-white/5 p-4 overflow-x-auto relative bg-[#05070a] scrollbar-thin" id="schema-row-1">
                    <div class="relative min-w-[1500px] min-h-[450px]">
                        ${nodeConfigs.map((node) => `
                            <div class="schema-node w-[200px] absolute z-20 rounded-sm overflow-hidden" style="left: ${node.x}px; top: ${node.y}px">
                                <div class="schema-node-header p-1.5 px-3 flex justify-between items-center text-[10px] font-black uppercase tracking-widest">
                                    <span>${node.label}</span>
                                    <span class="text-accent underline">${node.data.length}</span>
                                </div>
                                <div class="p-1 px-1.5 bg-black/40">
                                    ${node.fields.map(f => {
                                        const isPK = f === 'id';
                                        const isFK = f === node.linkField && !isPK;
                                        return `
                                        <div id="field-v2-${node.id}-${f}" class="field-row flex justify-between items-center px-2 hover:bg-white/5 ${isPK ? 'font-black text-accent' : (isFK ? 'text-warning' : 'text-white/40')}">
                                            <span>${f}</span> 
                                            <span class="text-[7px] opacity-40 font-bold">${isPK ? 'PK' : (isFK ? 'FK' : '')}</span>
                                        </div>`;
                                    }).join('')}
                                </div>
                            </div>
                        `).join('')}

                        
                        <svg class="absolute inset-0 w-full h-full pointer-events-none z-10 overflow-visible">
                            ${this.renderClassicJoin(nodeConfigs[1].x+200, getFieldY('pairing_requests', 'fromUID'), nodeConfigs[0].x, getFieldY('users', 'id'), '∞', '1')}
                            ${this.renderClassicJoin(nodeConfigs[2].x+200, getFieldY('groups', 'host'), nodeConfigs[0].x, getFieldY('users', 'id'), '∞', '1')}
                            ${this.renderClassicJoin(nodeConfigs[0].x+200, getFieldY('users', 'id'), nodeConfigs[3].x, getFieldY('locations', 'id'), '1', '∞')}
                            ${this.renderClassicJoin(nodeConfigs[0].x+200, getFieldY('users', 'id'), nodeConfigs[4].x, getFieldY('history', 'id'), '1', '∞')}
                        </svg>
                    </div>
                </div>

                <!-- MATRIX MANAGEMENT TOOLKIT (V2 COMPRESSION) -->
                <div class="bg-[#161b22] border-t border-b border-white/5 p-2 px-6 flex justify-between items-center">
                    <div class="flex items-center gap-6">
                        <div class="flex items-center gap-2">
                            <div class="w-2 h-2 bg-accent rounded-full animate-pulse shadow-[0_0_8px_#f59e0b]"></div>
                            <div class="flex flex-col">
                                <span class="text-[9px] font-black tracking-[0.4em] text-white/50 uppercase">Matrix V2 Architect</span>
                                <span class="text-[7px] text-white/20 font-mono">Waterfall Density Engine Active</span>
                            </div>
                        </div>
                        <div class="h-6 w-px bg-white/5 mx-2"></div>
                        <div class="relative group">
                            <input type="text" 
                                   value="${this.matrixSearch || ''}"
                                   oninput="window.filterMatrixBySearch(this.value)"
                                   placeholder="SEARCH_FORENSIC_NODES..." 
                                   class="bg-black/60 border border-white/10 rounded px-10 py-2 text-[10px] font-mono w-72 focus:border-accent/40 transition-all outline-none uppercase placeholder:text-white/10">
                            <svg class="w-4 h-4 absolute left-3 top-2 text-white/20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                        </div>
                    </div>
                    <div class="flex items-center gap-3">
                        <button onclick="window.appInstance.schema1.render()" class="flex items-center gap-2 p-1.5 px-3 bg-white/5 hover:bg-white/10 rounded border border-white/10 text-secondary transition-all text-[9px] font-bold uppercase tracking-widest">
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
                            Ref_Matrix (Sync)
                        </button>
                        <button onclick="window.bulkExportMatrix()" class="flex items-center gap-2 p-1.5 px-3 bg-white/5 hover:bg-white/10 rounded border border-white/10 text-secondary transition-all text-[9px] font-bold uppercase tracking-widest">
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M4 16v1a2 2 0 002 2h12a2 2 0 002-2v-1M16 8l-4-4m0 0L8 8m4-4v12"></path></svg>
                            Arch_JSON
                        </button>
                        <button onclick="window.bulkDeleteMatrix()" class="flex items-center gap-2 p-1.5 px-3 bg-danger/10 hover:bg-danger/20 rounded border border-danger/20 text-danger transition-all text-[9px] font-bold uppercase tracking-widest">
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                            DECOMMISSION
                        </button>
                        <div class="h-6 w-px bg-white/10 mx-2"></div>
                        <button onclick="window.clearMatrixFilter()" class="bg-accent/10 hover:bg-accent/20 text-accent text-[9px] font-black px-4 py-2 rounded transition-all uppercase tracking-widest border border-accent/20">RESET_PROBE</button>
                    </div>
                </div>

                <!-- ROW 2: MATRIX IDENTITY PROBE (DYNAMIC) -->
                <div class="flex-1 min-h-[400px] flex flex-col bg-[#0a0c10]">
                    <div class="flex-1 overflow-auto bg-[#0d1117] scrollbar-thin">
                        <table class="w-full text-[11px] font-mono border-collapse table-auto matrix-table">
                            <thead class="sticky top-0 bg-[#161b22] z-40 border-b border-white/10">
                                <tr>
                                    <th class="p-3 w-10 border-r text-center">
                                        <input type="checkbox" onclick="window.toggleAllMatrix(this.checked)" class="accent-accent scale-110">
                                    </th>
                                    <th class="p-3 px-6 text-left text-accent/40 uppercase tracking-[0.2em] font-black text-[9px] border-r w-32">NODE_TYPE</th>
                                    <th class="p-3 px-6 text-left text-accent/40 uppercase tracking-[0.2em] font-black text-[9px] border-r w-64">IDENTITY_ID</th>
                                    <th class="p-3 px-6 text-left text-accent/40 uppercase tracking-[0.2em] font-black text-[9px] border-r">MASTER_LINK (FK)</th>
                                    <th class="p-3 px-6 text-left text-accent/40 uppercase tracking-[0.2em] font-black text-[9px]">ACTIONS</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-white/5">
                                ${this.getMatrixRows(sortedMatrixNodes)}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;

        // --- RELATIONAL PROBE HANDLERS (V2 BINDING) ---
        window.clearMatrixFilter = () => { this.selectedUserId = null; this.matrixSearch = ""; this.updateUI(); };
        window.filterByMatrixUser = (id) => { 
            this.selectedUserId = id; 
            this.updateUI(); 
            window.log(`MATRIX_V2: Relational probe engaged for [${id}]`, "warning");
        };
        window.filterMatrixBySearch = (term) => {
            this.matrixSearch = term.toLowerCase();
            this.updateUI();
        };
        window.toggleAllMatrix = (checked) => {
            document.querySelectorAll('.matrix-check').forEach(c => c.checked = checked);
        };
        
        window.bulkDeleteMatrix = async () => {
            const selected = Array.from(document.querySelectorAll('.matrix-check:checked')).map(c => c.value);
            if (selected.length === 0) return window.log("MATRIX_V2: No nodes selected for decommissioning.", "system");
            
            if (confirm(`DECOMMISSION EXECUTABLE: Permanently erase ${selected.length} forensic nodes?`)) {
                window.log(`FORENSIC_ABORT: Initiating mass decommissioning...`, "error");
                for (const id of selected) {
                    try {
                        const activeNode = Store.state.navigation.activeNode || 'users'; 
                        await FBBridge.deleteRecord(`${activeNode}/${id}`);
                        window.log(`NODE_ERASED: [${id}]`, "success");
                    } catch (err) {
                        window.log(`ERASE_FAILURE: [${id}] -> ${err.message}`, "error");
                    }
                }
                this.render(); // Refresh data
            }
        };

        window.bulkExportMatrix = () => {
            const data = sortedMatrixNodes.reduce((acc, node) => {
                acc[node.id] = node.data;
                return acc;
            }, {});
            
            const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `jFindMe_Forensic_Export_${new Date().getTime()}.json`;
            a.click();
            window.log("FORENSIC_ARCHIVE: Matrix dump generated successfully.", "success");
        };
    }

    getMatrixRows(nodes) {
        let html = '';
        nodes.forEach(n => {
            n.data.forEach(item => {
                const isSelection = this.selectedUserId === item.id;
                const nodeColor = n.id === 'users' ? '#f59e0b' : (n.id === 'groups' ? '#3b82f6' : '#10b981');
                
                html += `
                    <tr class="group hover:bg-white/[0.02] transition-colors ${isSelection ? 'bg-accent/5 ring-1 ring-inset ring-accent/20' : ''}">
                        <td class="p-3 text-center border-r border-white/5">
                            <input type="checkbox" value="${item.id}" class="matrix-check accent-accent opacity-20 group-hover:opacity-100 transition-opacity">
                        </td>
                        <td class="p-3 px-6 border-r border-white/5 w-32">
                            <div class="flex items-center gap-2">
                                <div class="w-1.5 h-1.5 rounded-full" style="background: ${nodeColor}"></div>
                                <span class="text-[8px] font-black uppercase tracking-widest text-white/30">${n.id}</span>
                            </div>
                        </td>
                        <td class="p-3 px-6 border-r border-white/5 w-64">
                            <span class="text-[11px] font-bold ${n.id === 'users' ? 'text-accent hover:underline cursor-pointer' : 'text-white/60'}"
                                  onclick="${n.id === 'users' ? `window.filterByMatrixUser('${item.id}')` : ''}">${item.id}</span>
                        </td>
                        <td class="p-3 px-6 border-r border-white/5">
                            <span class="text-[10px] font-mono text-white/40 italic">${item[n.linkField] || '--'}</span>
                        </td>
                        <td class="p-3 px-6 flex items-center gap-4">
                            <button onclick="window.appInstance.editor.open('${n.id}', '${item.id}', ${JSON.stringify(item).replace(/"/g, '&quot;')})" 
                                    class="text-accent/60 hover:text-accent transition-colors" title="Edit Node">
                                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path></svg>
                            </button>
                            <button onclick="window.log('PROBE: Inspecting node [${item.id}]', 'system')" class="text-white/20 hover:text-white transition-colors">
                                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M15 12a3 3 0 11-6 0 3 3 0 016 0zM2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                            </button>
                            <button onclick="window.bulkDeleteMatrix('${n.id}', '${item.id}')" class="text-danger/40 hover:text-danger transition-colors" title="Decommission Node">
                                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                            </button>
                        </td>
                    </tr>
                `;
            });
        });
        return html || '<tr><td colspan="5" class="p-20 text-center text-white/10 uppercase tracking-[0.4em] font-black">Null Matrix Data State</td></tr>';
    }

    renderClassicJoin(x1, y1, x2, y2, label1, label2) {
        const path = `M ${x1} ${y1} L ${x2} ${y2}`;
        return `
            <g class="classic-join group">
                <path d="${path}" class="join-path" />
                <circle cx="${x1}" cy="${y1}" r="3" class="anchor-point" />
                <circle cx="${x2}" cy="${y2}" r="3" class="anchor-point" />
                <text x="${x1+10}" y="${y1-5}" fill="#f59e0b" font-weight="black" font-size="11" font-family="Arial" class="opacity-80">${label1}</text>
                <text x="${x2-18}" y="${y2-5}" fill="#f59e0b" font-weight="black" font-size="11" font-family="Arial" class="opacity-80">${label2}</text>
            </g>
        `;
    }
}
