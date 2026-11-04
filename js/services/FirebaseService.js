/**
 * jFindMe Matrix Manager V3 - Firebase Bridge Service
 * Role: API Abstraction & Forensic Service-Level Connector
 */
export class FirebaseService {
    constructor() {
        const hasWindow = typeof window !== 'undefined';
        const host = (hasWindow && window.location && window.location.hostname) || '127.0.0.1';
        const port = (hasWindow && window.location && window.location.port) || '3000';
        const origin = (hasWindow && window.location && window.location.origin) || `http://${host}:${port}`;
        // Primary and resilient secondary fallback endpoints (auto-failover between 8080 and 3000)
        this.baseUrl = (hasWindow && window.API_BASE_URL) || origin;
        this.fallbackBaseUrl = this.baseUrl.includes('3000') ? `http://${host}:8080` : `http://${host}:3000`;
        this.inFlightRequests = new Map(); // Request coalescing map for GET requests
    }

    async fetchRaw(endpoint, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        
        // For GET requests, coalesce identical in-flight requests to eliminate race collisions
        if (method === 'GET') {
            if (this.inFlightRequests.has(endpoint)) {
                return this.inFlightRequests.get(endpoint);
            }
        }

        const fetchPromise = (async () => {
            try {
                const response = await fetch(`${this.baseUrl}${endpoint}`, { 
                    ...options,
                    headers: { 'Content-Type': 'application/json', ...options.headers }
                });
                if (!response.ok) throw new Error(`HTTP_${response.status}`);
                return await response.json();
            } catch (err) {
                // Secondary Fallback Attempt: If primary port (8080/3000) was unreachable, try alternate port
                try {
                    const fallbackRes = await fetch(`${this.fallbackBaseUrl}${endpoint}`, {
                        ...options,
                        headers: { 'Content-Type': 'application/json', ...options.headers }
                    });
                    if (fallbackRes.ok) {
                        return await fallbackRes.json();
                    }
                } catch (fallbackErr) {
                    // Ignore fallback error and log main error
                }
                console.error(`[BRIDGE_ERROR] ${endpoint}`, err);
                throw err;
            } finally {
                if (method === 'GET') {
                    this.inFlightRequests.delete(endpoint);
                }
            }
        })();

        if (method === 'GET') {
            this.inFlightRequests.set(endpoint, fetchPromise);
        }

        return fetchPromise;
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
    async getDevicesLive() {
        return this.fetchRaw('/api/devices-live');
    }

    async getSafeZones() {
        return this.fetchRaw('/api/safe-zones');
    }

    async updateProfile(agentId, profileData) {
        return this.fetchRaw('/api/update-profile', {
            method: 'POST',
            body: JSON.stringify({ agentId, profileData })
        });
    }

    async pokeDevice(agentId, action = 'RING_ALARM') {
        return this.fetchRaw('/api/poke-device', {
            method: 'POST',
            body: JSON.stringify({ agentId, action })
        });
    }

    async interrogateFleet(agentIds = null) {
        return this.fetchRaw('/api/fleet/interrogate-live', {
            method: 'POST',
            body: JSON.stringify({ agentIds, requester: 'Web_Live_Radar' })
        });
    }

    async toggleAcl(agentId, permission, state) {
        return this.fetchRaw('/api/device/toggle-acl', {
            method: 'POST',
            body: JSON.stringify({ agentId, permission, state })
        });
    }
}

export const FBBridge = new FirebaseService();
if (typeof window !== 'undefined') {
    window.FBBridge = FBBridge;
    window.fetchApi = (endpoint, options = {}) => FBBridge.fetchRaw(endpoint, options);
}
