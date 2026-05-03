import { Store } from './Store.js';
import { DataGrid } from './DataGrid.js';
import { SchemaView } from './SchemaView.js';

class App {
    constructor() {
        // Global Logging Engine
        window.log = (msg, type = 'system') => {
            const terminal = document.getElementById('terminal');
            if (!terminal) return;
            const line = document.createElement('div');
            line.className = `log-line log-${type}`;
            line.textContent = `[${new Date().toLocaleTimeString()}] ${msg}`;
            terminal.appendChild(line);
            terminal.scrollTop = terminal.scrollHeight;
        };

        this.grid = new DataGrid('workspace-root');
        this.schema = new SchemaView('workspace-root');
        
        // Expose to window for sidebar triggers
        window.appInstance = this;
        this.init();
    }

    async init() {
        window.log("SYSTEM: Establishing Matrix Bridge...", "system");
        let retryCount = 0;
        const maxRetries = 10;

        const connect = async () => {
            try {
                // 1. Discovery Handshake - Use 127.0.0.1 for IPv4 stability
                const configRes = await fetch("http://127.0.0.1:3000/api/config");
                const config = await configRes.json();
                
                window.log(`HANDSHAKE: Cluster matched [${config.projectId}]`, "success");
                document.getElementById('project-id-label').classList.replace('bg-accent-glow', 'bg-success/20');
                document.getElementById('project-id-label').classList.replace('text-accent', 'text-success');

                // 2. Load Initial Collections
                const dataRes = await fetch("http://127.0.0.1:3000/api/data?path=/");
                if (!dataRes.ok) throw new Error("UPSTREAM_OFFLINE");
                
                const data = await dataRes.json();
                const collections = Object.keys(data || {}).filter(k => k !== 'system_audit');
                
                this.renderSidebar(collections);
                window.log(`DATA_READY: Mapped ${collections.length} nodes.`, "success");

                // --- AUTO-ENGAGEMENT FIX ---
                // Automatically open the first node to clear the loading screen
                if (collections.length > 0) {
                    setTimeout(() => {
                        const firstItem = document.querySelector('#collection-list .nav-item');
                        window.loadCollection(collections[0], firstItem);
                    }, 500);
                }
            } catch (err) {
                if (retryCount < maxRetries) {
                    retryCount++;
                    window.log(`RETRY_${retryCount}: Bridge pending...`, "warning");
                    // Exponential backoff
                    setTimeout(connect, 3000 * retryCount);
                } else {
                    window.log(`CRITICAL: Bridge Terminated. Check jFireBase.bat`, "error");
                }
            }
        };

        connect();

        // 3. Setup Audit Tab Listener
        document.getElementById('nav-audit').addEventListener('click', () => {
            this.setActiveNavItem('nav-audit');
            this.renderAuditLogs();
        });
    }

    renderSidebar(collections) {
        const sidebar = document.getElementById('collection-list');
        if (!collections.length) {
            sidebar.innerHTML = '<div class="text-xs text-secondary px-4">No nodes found.</div>';
            return;
        }

        sidebar.innerHTML = collections.map(name => `
            <div class="nav-item group" onclick="window.loadCollection('${name}', this)">
                <div class="w-1.5 h-1.5 rounded-full bg-border group-hover:bg-accent transition-colors mr-3"></div>
                <span class="flex-1">${name}</span>
                <span class="text-[9px] opacity-0 group-hover:opacity-40 transition-opacity">EXPLORE</span>
            </div>
        `).join('');
    }

    setActiveNavItem(el) {
        document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
        if (typeof el === 'string') {
            document.getElementById(el).classList.add('active');
        } else {
            el.classList.add('active');
        }
    }

    renderAuditLogs() {
        const root = document.getElementById('workspace-root');
        const logs = Store.state.auditLogs;
        
        root.innerHTML = `
            <div class="p-8">
                <h2 class="text-2xl font-bold mb-6">Forensic Audit Ledger</h2>
                <div class="space-y-4">
                    ${logs.length > 0 ? logs.map(log => `
                        <div class="p-4 bg-card border border-border rounded-lg flex justify-between items-center">
                            <div>
                                <span class="text-accent font-mono text-sm">${log.action}</span>
                                <span class="mx-2 text-secondary">at</span>
                                <span class="text-primary">${log.path}</span>
                            </div>
                            <div class="text-right">
                                <div class="text-xs text-secondary">${log.timestamp}</div>
                                <div class="text-xs font-bold text-success">${log.admin}</div>
                            </div>
                        </div>
                    `).join('') : '<div class="text-secondary">No activity recorded in this session.</div>'}
                </div>
            </div>
        `;
    }
}

// Global exposure for event handlers
window.loadCollection = (name, el) => {
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
    el.classList.add('active');
    Store.setCollection(name);
};

window.handleSearch = (term) => {
    // Debounce search
    clearTimeout(window.searchTimer);
    window.searchTimer = setTimeout(() => {
        Store.setSearch(term);
    }, 300);
};

window.deleteRecord = (id) => {
    Store.deleteRecord(id);
};

window.addNewRecord = () => {
    const id = prompt("Enter Unique ID for new record:");
    if (id) {
        Store.updateRecord(id, { created_at: Date.now(), status: "NEW" });
    }
};

// --- BULK ACTION REGISTRY ---
window.toggleSelection = (id) => Store.toggleSelection(id);
window.selectAll = () => Store.selectAll();
window.clearSelection = () => Store.clearSelection();
window.deleteSelected = () => Store.deleteSelected();
window.Store = Store; // For direct access from attributes

window.showSchema = () => {
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
    document.getElementById('btn-schema').classList.add('active');
    window.appInstance.schema.render();
};

window.adjustZoom = (delta, reset = false) => {
    window.appInstance.schema.setZoom(delta, reset);
};

// Start the Dashboard
new App();
