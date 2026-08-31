const getApiBase = () => window.API_BASE_URL || (window.location.origin.startsWith('http') ? window.location.origin : 'http://127.0.0.1:3000');

export const Store = {
    state: {
        activeCollection: null,
        collections: [],
        records: [],
        selectedIds: [],
        searchTerm: "",
        auditLogs: [],
        syncStatus: 'SYNCED', // SYNCED, PENDING, ERROR
        user: { 
            role: 'ADMIN', 
            email: window.ENV?.ADMIN_EMAIL || 'jmeet2gether@gmail.com',
            auth_key: window.ENV?.MASTER_KEY_ID || localStorage.getItem('matrix_master_key') || 'Jeet@123Meet'
        }
    },

    listeners: [],

    subscribe(fn) {
        this.listeners.push(fn);
    },

    notify() {
        this.listeners.forEach(fn => fn(this.state));
    },

    async setCollection(name) {
        this.state.activeCollection = name;
        this.state.records = [];
        this.state.selectedIds = [];
        this.state.syncStatus = 'PENDING';
        this.notify();

        try {
            const res = await fetch(`${getApiBase()}/api/data?path=/${name}`);
            const rawData = await res.json();
            
            this.state.records = Object.entries(rawData || {}).map(([id, val]) => ({
                id,
                ...(typeof val === 'object' ? val : { value: val })
            }));
            
            this.state.syncStatus = 'SYNCED';
            this.notify();
        } catch (err) {
            this.state.syncStatus = 'ERROR';
            this.notify();
        }
    },

    toggleSelection(id) {
        const idx = this.state.selectedIds.indexOf(id);
        if (idx > -1) this.state.selectedIds.splice(idx, 1);
        else this.state.selectedIds.push(id);
        this.notify();
    },

    selectAll() {
        this.state.selectedIds = this.state.records.map(r => r.id);
        this.notify();
    },

    clearSelection() {
        this.state.selectedIds = [];
        this.notify();
    },

    async deleteSelected() {
        const count = this.state.selectedIds.length;
        if (!count || !confirm(`Permanently delete ${count} selected records?`)) return;

        this.state.syncStatus = 'PENDING';
        this.notify();

        try {
            for (const id of this.state.selectedIds) {
                await this.apiCall('POST', '/api/update', {
                    path: `/${this.state.activeCollection}/${id}`,
                    data: null
                });
            }
            this.state.records = this.state.records.filter(r => !this.state.selectedIds.includes(r.id));
            this.state.selectedIds = [];
            this.state.syncStatus = 'SYNCED';
        } catch (err) {
            this.state.syncStatus = 'ERROR';
        }
        this.notify();
    },

    async updateRecord(id, updates) {
        const previousState = { ...this.state.records.find(r => r.id === id) };
        this.state.records = this.state.records.map(r => r.id === id ? { ...r, ...updates } : r);
        this.state.syncStatus = 'PENDING';
        this.notify();

        try {
            await this.apiCall('POST', '/api/update', {
                path: `/${this.state.activeCollection}/${id}`,
                data: updates
            });
            this.state.syncStatus = 'SYNCED';
            this.addAuditLog('UPDATE', `/${this.state.activeCollection}/${id}`, previousState, updates);
        } catch (err) {
            this.state.syncStatus = 'ERROR';
        }
        this.notify();
    },

    async deleteRecord(id) {
        if (!confirm(`Are you sure you want to permanently delete record [${id}]?`)) return;
        
        const previousState = { ...this.state.records.find(r => r.id === id) };
        this.state.records = this.state.records.filter(r => r.id !== id);
        this.state.syncStatus = 'PENDING';
        this.notify();

        try {
            await this.apiCall('POST', '/api/update', {
                path: `/${this.state.activeCollection}/${id}`,
                data: null
            });
            this.state.syncStatus = 'SYNCED';
            this.addAuditLog('DELETE', `/${this.state.activeCollection}/${id}`, previousState, null);
        } catch (err) {
            this.state.syncStatus = 'ERROR';
        }
        this.notify();
    },

    setSearch(term) {
        this.state.searchTerm = term;
        this.notify();
    },

    async apiCall(method, endpoint, body) {
        const res = await fetch(`${getApiBase()}${endpoint}`, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!res.ok) throw new Error("API_ERROR");
        return res.json();
    },

    addAuditLog(action, path, oldVal, newVal) {
        const log = {
            id: Date.now(),
            admin: this.state.user.email,
            action,
            path,
            timestamp: new Date().toISOString()
        };
        this.state.auditLogs.unshift(log);
        fetch(`${getApiBase()}/api/update`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                path: `/system_audit/${log.id}`,
                data: log
            })
        });
    }
};
