import { Store } from '../core/Store.js';
import { FBBridge } from '../services/FirebaseService.js';

/**
 * jFindMe Matrix Manager V3 - Multi-Join Forensic Architect
 * Features: Orthogonal Joins, Cardinality Markers (1:∞), Specific Field Mapping
 */
export class SchemaView {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.selectedUserId = null;
        this.fullData = { users: [], groups: [], locations: [], history: [], pairing_requests: [] };
    }

    async render() {
        this.container.innerHTML = `<div class="p-20 text-center animate-pulse text-accent uppercase tracking-widest font-bold text-lg">Mapping Multi-Join Relational Network...</div>`;
        
        try {
            const state = Store.getState();
            const collections = state.data.collections; // [{name, type}, ...]
            
            // Define intended relational nodes for the map
            const targetNodes = ['users', 'groups', 'locations', 'history', 'pairing_requests', 'avatars', 'sessions'];
            
            for(const nodeName of targetNodes) {
                const meta = collections.find(c => c.name === nodeName);
                if (meta) {
                    const data = await FBBridge.getNodeData(meta.name, 100, null, meta.type);
                    this.fullData[nodeName] = Object.entries(data || {}).map(([id, val]) => ({ 
                        id, 
                        _source: meta.type,
                        ...(typeof val === 'object' ? val : {val}) 
                    }));
                }
            }
            this.updateUI();
        } catch (err) {
            this.container.innerHTML = `<div class="p-20 text-center text-danger font-bold text-xl">Handshake Failure: ${err.message}</div>`;
        }
    }
    updateUI() {
        const filtered = this.getFilteredData();

        // Configuration: Define relational anchors with explicit Forensic Link Fields (FK)
        const nodes = [
            { id: 'users', label: 'Users HUB', linkField: 'id', fields: ['id', 'name', 'email', 'avatar_id', 'role', 'status'], data: filtered.users, x: 50, y: 70 },
            { id: 'pairing_requests', label: 'Pairing Proxy', linkField: 'fromUID', fields: ['id', 'fromUID', 'toEmail', 'status', 'timestamp'], data: filtered.pairing_requests, x: 350, y: 70 },
            { id: 'groups', label: 'Group Join', linkField: 'host', fields: ['id', 'name', 'host', 'members', 'description'], data: filtered.groups, x: 650, y: 70 },
            { id: 'locations', label: 'Telemetry', linkField: 'id', fields: ['id', 'lat', 'lng', 'timestamp', 'accuracy'], data: filtered.locations, x: 950, y: 70 },
            { id: 'history', label: 'Forensic', linkField: 'node_id', fields: ['id', 'timestamp', 'event_type', 'node_id'], data: filtered.history, x: 1250, y: 70 },
            { id: 'avatars', label: 'Asset Lib', linkField: 'id', fields: ['id', 'asset_url', 'type'], data: filtered.avatars || [], x: 1550, y: 70 }
        ];

        const getFieldY = (n, f) => {
            const node = nodes.find(x => x.id === n);
            const idx = node ? node.fields.indexOf(f) : 0;
            return (node ? node.y : 0) + 28 + (idx * 18) + 9;
        };

        // 1. ANCHOR USERS HUB (Nodes[0])
        const hubNode = nodes.shift();
        
        // 2. DYNAMICALY SORT REMAINING NODES BY DATA DENSITY (JOIN COUNT)
        const sortedNodes = nodes.sort((a, b) => b.data.length - a.data.length);
        
        // 3. RECONSTRUCT RATIONAL WATERFALL (HUB -> MAX -> MIN)
        const waterfallNodes = [hubNode, ...sortedNodes];

        this.container.innerHTML = `
            <style>
                .join-path { transition: all 0.3s ease; stroke-dasharray: 1000; stroke-dashoffset: 0; animation: dash 20s linear infinite; }
                @keyframes dash { from { stroke-dashoffset: 1000; } to { stroke-dashoffset: 0; } }
                .schema-node { transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1); filter: drop-shadow(0 0 15px rgba(0,0,0,0.5)); }
                .schema-node:hover { transform: scale(1.02); z-index: 50 !important; border-color: #f59e0b; }
            </style>
            <div class="query-builder flex flex-col h-full bg-main overflow-hidden text-white font-sans">
                <!-- ROW 1: MISSION CRITICAL RELATIONSHIP MAP -->
                <div class="h-[40%] border-b border-white/5 p-4 overflow-auto relative bg-[#05070a]" id="schema-row-1">
                    <div class="relative min-w-[1800px] min-h-[500px]">
                        <!-- Nodes -->
                        ${waterfallNodes.map((node, i) => `
                            <div class="schema-node border border-white/10 w-[210px] absolute z-20 bg-[#0d1117]/95 backdrop-blur-md rounded-md overflow-hidden" style="left: ${node.x}px; top: ${node.y}px">
                                <div class="schema-node-header flex justify-between items-center p-1.5 px-2 bg-white/5 border-b border-white/10">
                                    <div class="flex flex-col">
                                        <span class="font-bold text-accent tracking-wider uppercase text-[9px] leading-tight">${node.label}</span>
                                        <span class="text-[7px] text-white/20 font-mono uppercase tracking-tighter">FK: ${node.linkField}</span>
                                    </div>
                                    <span class="text-[9px] bg-white/10 px-1.5 py-0.5 rounded text-white/60 font-mono font-black">${node.data.length}</span>
                                </div>
                                <div class="p-1 px-1.5 bg-gradient-to-b from-transparent to-black/10">
                                    ${node.fields.map(f => `
                                        <div id="field-${node.id}-${f}" class="text-[11px] font-mono flex justify-between items-center group/field px-1.5 rounded hover:bg-white/5 transition-colors h-[18px]">
                                            <div class="flex items-center gap-2">
                                                <div class="w-1 h-1 rounded-full ${f === 'id' ? 'bg-accent' : 'bg-white/20'}"></div>
                                                <span class="${f === node.linkField ? 'font-black text-accentunderline italic' : 'text-white/60'}">${f}</span> 
                                            </div>
                                            <span class="text-[7px] opacity-10 font-bold">${f === node.linkField ? 'FK' : ''}</span>
                                        </div>`).join('')}
                                </div>
                            </div>
                        `).join('')}
                    </div>
                </div>

                <!-- MATRIX MANAGEMENT TOOLKIT -->
                <div class="bg-[#161b22] border-t border-b border-white/5 p-3 px-6 flex justify-between items-center">
                    <div class="flex items-center gap-6">
                        <div class="flex items-center gap-3">
                            <div class="w-1.5 h-1.5 bg-accent rounded-full animate-pulse shadow-[0_0_6px_#f59e0b]"></div>
                            <span class="text-[10px] font-black tracking-[0.4em] text-white/50 uppercase">Matrix Forensic Probe Waterfall</span>
                        </div>
                        <div class="h-6 w-px bg-white/5 mr-2"></div>
                        <input type="text" 
                               oninput="window.filterMatrixBySearch(this.value)" 
                               onkeypress="window.handleMatrixKey(event)"
                               placeholder="SEARCH_IDENTITY..." 
                               class="bg-black/40 border border-white/5 rounded px-8 py-1.5 text-[10px] font-mono w-64 outline-none focus:border-accent/40"
                               tabindex="1">
                    </div>
                    <button onclick="window.clearMatrixFilter()" 
                            class="bg-accent/10 text-accent text-[9px] font-black px-4 py-2 rounded border border-accent/20 hover:bg-accent/20"
                            tabindex="2">RESET_PROBE</button>
                </div>

                <!-- ROW 2: MATRIX IDENTITY PROBE (60%) -->
                <div class="h-[60%] flex flex-col bg-[#0a0c10]">
                    <div class="flex-1 overflow-auto bg-[#0d1117] scrollbar-thin">
                        <table class="w-full text-[13px] font-mono border-collapse table-auto">
                            <thead class="sticky top-0 bg-[#161b22] z-40 border-b border-white/10">
                                <tr>
                                    <th class="p-4 w-10 border-r border-white/5 text-center"><input type="checkbox" onclick="window.toggleAllMatrix(this.checked)" class="accent-accent" tabindex="3"></th>
                                    ${waterfallNodes.map((n, i) => `<th class="p-4 text-left font-black text-[10px] text-accent/60 uppercase tracking-widest border-r border-white/5 whitespace-nowrap">${n.label} <br/><span class="text-[8px] text-white/20 font-mono">LINK: ${n.linkField}</span></th>`).join('')}
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-white/5">
                                ${this.getMatrixRows(waterfallNodes)}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;

        window.clearMatrixFilter = () => { this.selectedUserId = null; this.matrixSearch = ""; this.updateUI(); };
        window.filterByMatrixUser = (id) => { this.selectedUserId = id; this.updateUI(); };
        window.filterMatrixBySearch = (term) => { this.matrixSearch = term.toLowerCase(); this.updateUI(); };
        window.handleMatrixKey = (e) => { if (e.key === 'Enter') this.updateUI(); };
    }

    renderJoinPath(x1, y1, x2, y2, isBus = false, busOffset = 40) {
        const color = "#f59e0b";
        const midX = x1 + (isBus ? busOffset : 30);
        
        const path = isBus 
            ? `M ${x1} ${y1} L ${midX} ${y1} L ${midX} ${y2+50} L ${x2-30} ${y2+50} L ${x2-30} ${y2} L ${x2} ${y2}`
            : `M ${x1} ${y1} L ${midX} ${y1} L ${midX} ${y2} L ${x2} ${y2}`;

        return `
            <g class="join-group active:opacity-100 opacity-60 transition-opacity">
                <!-- Outer Shadow/Glow Path -->
                <path d="${path}" stroke="${color}" stroke-width="6" fill="none" opacity="0.05" filter="url(#glow)" />
                <!-- Main Path -->
                <path d="${path}" stroke="${color}" stroke-width="2.5" fill="none" class="join-path" />
                
                <!-- Cardinality Connectors -->
                <circle cx="${x1}" cy="${y1}" r="3" fill="${color}" />
                <circle cx="${x2}" cy="${y2}" r="3" fill="${color}" />
                
                <!-- Indicators -->
                <rect x="${x1-15}" y="${y1-22}" width="30" height="15" rx="4" fill="black" stroke="${color}" stroke-width="1" />
                <text x="${x1}" y="${y1-11}" fill="${color}" font-size="10" font-weight="black" text-anchor="middle">1</text>
                
                <rect x="${x2-15}" y="${y2-22}" width="30" height="15" rx="4" fill="black" stroke="${color}" stroke-width="1" />
                <text x="${x2}" y="${y2-11}" fill="${color}" font-size="12" font-weight="black" text-anchor="middle">∞</text>
            </g>
        `;
    }

    getFilteredData() {
        let baseData = this.fullData;

        // 1. Identity Select Filter
        if (this.selectedUserId) {
            const uid = this.selectedUserId;
            baseData = {
                users: this.fullData.users.filter(u => u.id === uid),
                pairing_requests: this.fullData.pairing_requests.filter(p => p.fromUID === uid || p.id === uid),
                groups: this.fullData.groups.filter(g => g.host === uid || g.members?.includes?.(uid) || g.id === uid),
                locations: this.fullData.locations.filter(l => l.id === uid),
                history: this.fullData.history.filter(h => h.id === uid || h.node_id === uid)
            };
        }

        // 2. Fuzzy Matrix Search Filter
        if (this.matrixSearch) {
            const term = this.matrixSearch.toLowerCase();
            const fuzzyFilter = (arr) => arr.filter(item => JSON.stringify(item).toLowerCase().includes(term));
            
            return {
                users: fuzzyFilter(baseData.users),
                pairing_requests: fuzzyFilter(baseData.pairing_requests),
                groups: fuzzyFilter(baseData.groups),
                locations: fuzzyFilter(baseData.locations),
                history: fuzzyFilter(baseData.history)
            };
        }
        
        return baseData;
    }

    getMatrixRows(nodes) {
        const rowCount = Math.max(...nodes.map(n => n.data.length), 1);
        let html = '';
        for (let i = 0; i < rowCount; i++) {
            html += `<tr class="group hover:bg-white/[0.02] transition-colors">`;
            
            // LEAD CHECKBOX COLUMN
            const leadItem = nodes[0].data[i];
            html += `
                <td class="p-4 w-10 border-r border-white/5 text-center">
                    ${leadItem ? `<input type="checkbox" value="${leadItem.id}" class="matrix-check accent-accent opacity-20 group-hover:opacity-100 transition-opacity">` : ''}
                </td>`;

            nodes.forEach((n, colIdx) => {
                const item = n.data[i];
                const isUserCol = colIdx === 0;
                const isSelection = item && this.selectedUserId === item.id;
                
                html += `
                    <td class="p-3 px-5 border-r border-white/5 whitespace-nowrap ${isSelection ? 'bg-accent/5 ring-1 ring-inset ring-accent/30' : ''}">
                        ${item ? `
                            <div class="flex items-center gap-2">
                                <div class="w-1.5 h-1.5 rounded-full ${isSelection ? 'bg-accent animate-pulse' : 'bg-white/10'}"></div>
                                <span class="${isUserCol ? 'text-accent cursor-pointer hover:underline font-bold' : 'text-white/60'}" 
                                      onclick="${isUserCol ? `window.filterByMatrixUser('${item.id}')` : ''}">${item[n.linkField] || item.id}</span>
                            </div>
                        ` : '<span class="text-white/10">--</span>'}
                    </td>`;
            });
            html += `</tr>`;
        }
        return html;
    }
}
