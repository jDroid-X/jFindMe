/**
 * Matrix Manager V2 - high-performance Data Grid
 * Features: Auto-schema detection, Inline editing, Status tracking.
 */
import { Store } from './Store.js';

export class DataGrid {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        Store.subscribe(state => this.render(state));
    }

    render(state) {
        if (!state.activeCollection) {
            this.container.innerHTML = `<div class="p-8 text-center text-secondary">Select a collection to manage data.</div>`;
            return;
        }

        let records = state.records;
        
        // --- 1. SEARCH FILTERING ---
        const searchTerm = (state.searchTerm || "").toLowerCase();
        if (searchTerm) {
            records = records.filter(r => 
                Object.values(r).some(v => String(v).toLowerCase().includes(searchTerm))
            );
        }

        if (records.length === 0 && state.syncStatus === 'SYNCED') {
            this.container.innerHTML = `
                <div class="p-8 text-center space-y-4">
                    <div class="text-secondary text-lg">Zero records match strategy.</div>
                    <button class="btn-primary" onclick="window.addNewRecord()">+ Add New Node</button>
                </div>`;
            return;
        }

        // Auto-detect columns
        const columns = new Set();
        records.forEach(r => Object.keys(r).forEach(k => {
            if (k !== 'id') columns.add(k);
        }));
        const colArray = Array.from(columns);

        const allSelected = records.length > 0 && records.every(r => state.selectedIds.includes(r.id));
        const selectionCount = state.selectedIds.length;

        let html = `
            <div class="px-6 py-4 flex justify-between items-center border-b border-border bg-card/50 backdrop-blur-md sticky top-0 z-20">
                <div class="flex items-center gap-4">
                    <div class="w-10 h-10 bg-accent/20 rounded-lg flex items-center justify-center text-accent">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M4 7V4a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v3M9 2v3M15 2v3M2 7h20v14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V7zM2 12h20M7 12v9M17 12v9"></path></svg>
                    </div>
                    <div>
                        <h2 class="text-xl font-bold tracking-tight">/${state.activeCollection}</h2>
                        <div class="text-[10px] text-secondary uppercase font-bold tracking-widest">Active Forensic Node</div>
                    </div>
                </div>

                <div class="flex gap-4 items-center">
                    ${selectionCount > 0 ? `
                        <div class="flex items-center gap-2 bg-danger/10 border border-danger/20 p-1 px-3 rounded-lg animate-pulse">
                            <span class="text-[10px] font-bold text-danger">${selectionCount} SELECTED</span>
                            <button class="p-1 hover:bg-danger/20 rounded text-danger" title="Delete Bulk" onclick="Store.deleteSelected()">
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16"></path></svg>
                            </button>
                            <button class="text-secondary hover:text-white p-1" onclick="Store.clearSelection()">✕</button>
                        </div>
                    ` : ''}

                    <div class="relative">
                        <input 
                            type="text" 
                            placeholder="Search strategy..." 
                            class="bg-main border border-border rounded-lg pl-10 pr-4 py-2 text-xs focus:outline-accent outline-none w-48"
                            value="${state.searchTerm || ''}"
                            oninput="window.handleSearch(this.value)"
                        >
                        <svg class="w-3 h-3 text-secondary absolute left-3 top-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                    </div>

                    <div class="flex gap-1">
                        <button class="p-2 hover:bg-white/5 rounded-lg text-secondary" title="Refresh Node" onclick="Store.setCollection('${state.activeCollection}')">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
                        </button>
                        <button class="btn-primary flex items-center gap-2 text-xs p-2 px-4 h-9" onclick="window.addNewRecord()">
                            + Add Node
                        </button>
                    </div>
                </div>
            </div>

            <div class="data-grid-container px-6 pb-20">
                <table class="data-sheet">
                    <thead>
                        <tr>
                            <th class="col-check">
                                <input type="checkbox" ${allSelected ? 'checked' : ''} onclick="this.checked ? Store.selectAll() : Store.clearSelection()">
                            </th>
                            <th class="uppercase text-[10px] tracking-widest bg-card">ID</th>
                            ${colArray.map(col => `<th class="uppercase text-[10px] tracking-widest">${col}</th>`).join('')}
                            <th class="w-20 text-center">ACTIONS</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${records.map(row => `
                            <tr class="${state.selectedIds.includes(row.id) ? 'bg-accent/5' : ''}">
                                <td class="col-check">
                                    <input type="checkbox" ${state.selectedIds.includes(row.id) ? 'checked' : ''} onchange="Store.toggleSelection('${row.id}')">
                                </td>
                                <td class="font-mono text-[10px] text-accent">${row.id}</td>
                                ${colArray.map(col => {
                                    const val = row[col];
                                    const isObject = typeof val === 'object' && val !== null;
                                    const displayVal = isObject ? '{...}' : (val !== undefined ? val : '-');
                                    
                                    return `
                                        <td 
                                            class="editable-cell ${isObject ? 'italic text-accent cursor-pointer' : ''}" 
                                            contenteditable="${!isObject}" 
                                            onblur="!${isObject} && window.handleCellEdit('${row.id}', '${col}', this.innerText)"
                                            onclick="${isObject} ? window.openJsonEditor('${row.id}', '${col}') : null"
                                        >
                                            ${displayVal}
                                        </td>
                                    `;
                                }).join('')}
                                <td class="text-center">
                                    <button class="text-danger opacity-20 hover:opacity-100 p-2 hover:bg-danger/10 rounded-lg transition-all" onclick="window.deleteRecord('${row.id}')">
                                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16"></path></svg>
                                    </button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;

        this.container.innerHTML = html;
    }
}

// Global handler for inline edits
window.handleCellEdit = (id, field, value) => {
    Store.updateRecord(id, { [field]: value });
};
