/**
 * jFindMe Matrix Manager V3 - Immutable State Engine
 * Pattern: One-Way Data Flow (Redux-Lite)
 */
export class StateStore {
    constructor() {
        this.state = {
            auth: { isAuthenticated: false, user: null, sessionToken: null },
            navigation: { activeTab: 'dashboard', activeNode: null },
            data: { records: [], collections: [], selection: [] },
            system: { syncStatus: 'IDLE', lastSync: null, errors: [], config: { projectId: null, databaseURL: null } }
        };
        this.listeners = [];
    }

    getState() { return JSON.parse(JSON.stringify(this.state)); }

    subscribe(fn) {
        this.listeners.push(fn);
        return () => this.listeners = this.listeners.filter(l => l !== fn);
    }

    dispatch(action, payload) {
        console.debug(`[STORE_ACTION] ${action}`, payload);
        
        switch(action) {
            case 'SET_NAV':
                // payload: { activeTab, activeNode, activeView }
                this.state.navigation = { ...this.state.navigation, ...payload };
                break;
            case 'SET_AUTH':
                this.state.auth = { ...this.state.auth, ...payload };
                break;
            case 'SET_COLLECTIONS':
                this.state.data.collections = payload;
                break;
            case 'SET_CONFIG':
                this.state.system.config = payload;
                break;
            case 'SET_RECORDS':
                this.state.data.records = payload;
                this.state.data.selection = [];
                break;
            case 'TOGGLE_SELECTION':
                const idx = this.state.data.selection.indexOf(payload);
                if (idx > -1) this.state.data.selection.splice(idx, 1);
                else this.state.data.selection.push(payload);
                break;
            case 'SET_SYNC_STATUS':
                this.state.system.syncStatus = payload;
                this.state.system.lastSync = new Date().toISOString();
                break;
            case 'SET_ACTIVE_NODE':
                this.state.navigation.activeNode = payload;
                this.state.navigation.activeTab = 'dashboard';
                break;
            case 'SET_SEARCH':
                this.state.searchTerm = payload;
                break;
            case 'LOG_ERROR':
                this.state.system.errors.push({ t: Date.now(), m: payload });
                break;
        }

        this.notify();
    }

    notify() {
        const stateClone = this.getState();
        this.listeners.forEach(fn => fn(stateClone));
    }
}

export const Store = new StateStore();
