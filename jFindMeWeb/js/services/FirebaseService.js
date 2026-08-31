/**
 * jFindMe Matrix Manager V3 - Firebase Bridge Service
 * Role: API Abstraction & Forensic Service-Level Connector
 */
export class FirebaseService {
    constructor() {
        this.baseUrl = window.API_BASE_URL || (window.location.origin.startsWith('http') ? window.location.origin : "http://127.0.0.1:3000");
    }

    async fetchRaw(endpoint, options = {}) {
        if (this.currentController) {
            this.currentController.abort();
        }
        this.currentController = new AbortController();
        
        try {
            const response = await fetch(`${this.baseUrl}${endpoint}`, { 
                ...options,
                signal: this.currentController.signal,
                headers: { 'Content-Type': 'application/json', ...options.headers }
            });
            if (!response.ok) throw new Error(`HTTP_${response.status}`);
            return await response.json();
        } catch (err) {
            if (err.name === 'AbortError') return null;
            console.error(`[BRIDGE_ERROR] ${endpoint}`, err);
            throw err;
        } finally {
            this.currentController = null;
        }
    }

    async getDiscovery() {
        return this.fetchRaw('/api/discovery');
    }

    /**
     * Fetches a batch of data from the proxy.
     * @param {string} path - Firebase node path
     * @param {number} limit - Number of records to fetch
     * @param {string} startAt - The key to start fetching from (optional)
     * @param {string} type - 'rtdb' or 'firestore'
     */
    async getNodeData(path, limit = 2000, startAt = null, type = 'rtdb') {
        let url = `/api/data?path=${path === '/' ? '' : path}&limit=${limit}&type=${type}`;
        if (startAt) url += `&startAt=${encodeURIComponent(startAt)}`;
        return this.fetchRaw(url);
    }

    async getNodeStats(path, type = 'rtdb') {
        return this.fetchRaw(`/api/stats?path=${path === '/' ? '' : path}&type=${type}`);
    }

    async getHistoryByUID(uid) {
        return this.fetchRaw(`/api/data?path=History/${uid}/history`);
    }

    async deleteRecord(path, type = 'rtdb') {
        return this.fetchRaw('/api/delete', {
            method: 'POST',
            body: JSON.stringify({ path, type })
        });
    }

    /**
     * NUCLEAR PURGE: Completely erases all data under a root-level node.
     * Uses a dedicated controller so navigation won't abort the operation.
     * @param {string} node - The root node name (e.g., "history")
     * @param {string} type - 'rtdb' or 'firestore'
     * @returns {Promise<{status, node, purgedChildren, purgedRecordsEstimate, timestamp}>}
     */
    async purgeNode(node, type = 'rtdb') {
        const controller = new AbortController();
        try {
            const response = await fetch(`${this.baseUrl}/api/purge-node`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ node, type }),
                signal: controller.signal
            });
            if (!response.ok) {
                const err = await response.json();
                throw new Error(err.error || `HTTP_${response.status}`);
            }
            return await response.json();
        } catch (err) {
            console.error(`[BRIDGE_ERROR] purge-node/${node}`, err);
            throw err;
        }
    }
}

export const FBBridge = new FirebaseService();
