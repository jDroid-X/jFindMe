import { 
    ref, 
    onValue, 
    set, 
    update, 
    remove, 
    push, 
    get 
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-database.js";
import { database } from "../config/firebase-config.js";

/**
 * Model: DatabaseProvider
 * Purpose: Handles all direct interactions with Firebase RTDB.
 */
class DatabaseProvider {
    constructor() {
        this.db = database;
    }

    /**
     * Get root nodes of the database for navigation
     */
    async getRootNodes() {
        const rootRef = ref(this.db, "/");
        const snapshot = await get(rootRef);
        if (snapshot.exists()) {
            return Object.keys(snapshot.val());
        }
        return [];
    }

    /**
     * Subscribe to a specific data node
     * @param {string} path - Firebase path
     * @param {function} callback - Update handler
     */
    subscribeToNode(path, callback) {
        const nodeRef = ref(this.db, path);
        return onValue(nodeRef, (snapshot) => {
            callback(snapshot.val());
        });
    }

    /**
     * Update a specific record
     * @param {string} path - Firebase path
     * @param {object} data - New data
     */
    async updateRecord(path, data) {
        const nodeRef = ref(this.db, path);
        return await update(nodeRef, data);
    }

    /**
     * Create a new record
     * @param {string} path - Parent path
     * @param {object} data - New record data
     */
    async createRecord(path, data) {
        const nodeRef = ref(this.db, path);
        const newRecordRef = push(nodeRef);
        return await set(newRecordRef, data);
    }

    /**
     * Delete a record
     * @param {string} path - Firebase path
     */
    async deleteRecord(path) {
        const nodeRef = ref(this.db, path);
        return await remove(nodeRef);
    }
}

export const dbProvider = new DatabaseProvider();
