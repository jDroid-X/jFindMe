/**
 * OfflineQueue.js - IndexedDB-based Offline Queue for GPS Telemetry
 * Handles device location updates when Firebase is unavailable
 */
import { FBBridge } from '../services/FirebaseService.js';

export class OfflineQueue {
    static DB_NAME = 'jfindme_offline_queue';
    static DB_VERSION = 1;
    static STORE_NAME = 'location_updates';
    
    static _db = null;

    /**
     * Initialize IndexedDB storage
     */
    static async init() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(this.DB_NAME, this.DB_VERSION);
            
            request.onerror = () => reject(request.error);
            request.onsuccess = () => {
                this._db = request.result;
                resolve(this._db);
            };
            
            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                
                // Create object store for pending updates
                if (!db.objectStoreNames.contains(this.STORE_NAME)) {
                    const store = db.createObjectStore(this.STORE_NAME, {
                        keyPath: 'id',
                        autoIncrement: true
                    });
                    store.createIndex('timestamp', 'timestamp', { unique: false });
                    store.createIndex('deviceId', 'deviceId', { unique: false });
                    store.createIndex('synced', 'synced', { unique: false });
                }
            };
        });
    }

    /**
     * Add location update to offline queue
     */
    static async queueLocationUpdate(deviceData) {
        if (!this._db) await this.init();
        
        const record = {
            deviceId: deviceData.agentId,
            data: deviceData,
            timestamp: Date.now(),
            synced: false
        };
        
        return new Promise((resolve, reject) => {
            const tx = this._db.transaction([this.STORE_NAME], 'readwrite');
            const store = tx.objectStore(this.STORE_NAME);
            const request = store.add(record);
            
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Get all unsynced records
     */
    static async getPendingUpdates() {
        if (!this._db) await this.init();
        
        return new Promise((resolve, reject) => {
            const tx = this._db.transaction([this.STORE_NAME], 'readonly');
            const store = tx.objectStore(this.STORE_NAME);
            const index = store.index('synced');
            const request = index.getAll(false);
            
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Sync queued updates to Firebase
     */
    static async syncAll() {
        const pending = await this.getPendingUpdates();
        
        if (pending.length === 0) {
            return { synced: 0, failed: 0 };
        }
        
        let synced = 0;
        let failed = 0;
        
        for (const record of pending) {
            try {
                await FBBridge.fetchRaw('/api/devices-live', {
                    method: 'POST',
                    body: JSON.stringify({
                        agentId: record.deviceId,
                        ...record.data
                    })
                });
                
                await this.markSynced(record.id);
                synced++;
            } catch (err) {
                console.warn('[OFFLINE_QUEUE] Sync failed for record:', record.id, err);
                failed++;
            }
        }
        
        return { synced, failed, total: pending.length };
    }

    /**
     * Mark record as synced and remove from queue
     */
    static async markSynced(id) {
        if (!this._db) return;
        
        return new Promise((resolve, reject) => {
            const tx = this._db.transaction([this.STORE_NAME], 'readwrite');
            const store = tx.objectStore(this.STORE_NAME);
            const request = store.delete(id);
            
            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Clear all queued updates
     */
    static async clearAll() {
        if (!this._db) return;
        
        return new Promise((resolve, reject) => {
            const tx = this._db.transaction([this.STORE_NAME], 'readwrite');
            const store = tx.objectStore(this.STORE_NAME);
            const request = store.clear();
            
            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Get queue statistics
     */
    static async getStats() {
        if (!this._db) await this.init();
        
        return new Promise((resolve, reject) => {
            const tx = this._db.transaction([this.STORE_NAME], 'readonly');
            const store = tx.objectStore(this.STORE_NAME);
            const request = store.count();
            
            request.onsuccess = () => resolve({ pending: request.result });
            request.onerror = () => reject(request.error);
        });
    }
}
