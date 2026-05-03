import { 
    signInWithEmailAndPassword, 
    onAuthStateChanged 
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import { auth } from "../config/firebase-config.js";
import { dbProvider } from "../models/DatabaseProvider.js";

import { renderer } from "../views/DataRenderer.js";
import { modalUI } from "../views/ModalUI.js";

/**
 * Controller: MainController
 * Purpose: Orchestrates the application logic and event flow.
 */
class MainController {
    constructor() {
        this.currentNode = null;
        this.currentData = null;
        this.errorLogs = []; // Stores encountered system errors
        this.terminal = document.getElementById('terminal-output');
        this.refreshBtn = document.getElementById('refresh-db-btn');
        this.addBtn = document.getElementById('add-record-btn');
        this.searchBox = document.getElementById('node-search');
        this.errorNav = document.getElementById('nav-error-logs');
        this.loginOverlay = document.getElementById('login-overlay');
        this.uidBadge = document.getElementById('current-uid');
        
        this._initAuth();
        this._initListeners();
    }

    async bootstrap() {
        // Only run bootstrap if authenticated
        if (!auth.currentUser) return;

        this.log("Bootstrapping Application Workspace...", "system");
        this.uidBadge.textContent = auth.currentUser.uid;

        // --- DRY RUN SIMULATION START ---
        this.log("PERMISSION_DENIED: Unauthorized access to system/security_rules", "error");
        // --- DRY RUN SIMULATION END ---
        try {
            const rootNodes = await dbProvider.getRootNodes();
            renderer.renderNodeList(rootNodes, (node) => this.selectNode(node));
            this.log(`Successfully mapped ${rootNodes.length} database nodes.`, "success");
        } catch (err) {
            this.log(`Mapping Failed: ${err.message}`, "error");
        }
    }

    _initAuth() {
        onAuthStateChanged(auth, (user) => {
            if (user) {
                this.loginOverlay.classList.add('hidden');
                this.bootstrap();
            } else {
                this.loginOverlay.classList.remove('hidden');
                this.uidBadge.textContent = "ANONYMOUS";
            }
        });
    }

    _initListeners() {
        // Refresh Button
        this.refreshBtn.addEventListener('click', () => {
            if (this.currentNode) this.selectNode(this.currentNode);
        });

        // Add Record Logic
        this.addBtn.addEventListener('click', () => {
            if (!this.currentNode) {
                this.log("ERROR: Select a node first to add a record.", "error");
                return;
            }
            this.log(`INITIALIZING_NEW_RECORD for [/${this.currentNode}]`, "warning");
            modalUI.show(`CREATE NEW [${this.currentNode}]`, { }, async (newData) => {
                await dbProvider.createRecord(`/${this.currentNode}`, newData);
                this.log(`Successfully appended record to ${this.currentNode}`, "success");
            });
        });

        // Search Filter
        this.searchBox.addEventListener('input', (e) => {
            const term = e.target.value.toLowerCase();
            this.filterData(term);
        });

        // Error Log Navigation
        this.errorNav.addEventListener('click', () => {
            this.currentNode = "ERROR_LOGS";
            document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
            this.errorNav.classList.add('active');
            renderer.renderErrorLogs(this.errorLogs);
        });

        // Login Handler
        document.getElementById('login-btn').addEventListener('click', async () => {
            const email = document.getElementById('login-email').value;
            const pass = document.getElementById('login-password').value;
            this.log(`ATTEMPTING_AUTH as [${email}]...`, "warning");
            try {
                await signInWithEmailAndPassword(auth, email, pass);
                this.log("AUTH_SUCCESS: Connected to Cloud Cluster.", "success");
            } catch (err) {
                this.log(`AUTH_FAILED: ${err.message}`, "error");
                alert("INVALID_CREDENTIALS: ACCESS_DENIED");
            }
        });

        // Clear terminal
        document.getElementById('clear-terminal').addEventListener('click', () => {
            this.terminal.innerHTML = '';
        });
    }

    /**
     * Select a specific database node
     * @param {string} node - Name of the node
     */
    selectNode(node) {
        this.currentNode = node;
        this.log(`Subscribing to node stream: [/${node}]`, "system");
        
        dbProvider.subscribeToNode(`/${node}`, (data) => {
            this.currentData = data;
            renderer.renderTable(data, `/${node}`);
        });
    }

    /**
     * Handle global edit action from DataRenderer
     */
    async handleEdit(path, originalDataStr) {
        this.log(`EDIT_RECORD requested for path: ${path}`, "warning");
        try {
            const data = JSON.parse(originalDataStr);
            modalUI.show(`EDIT ${path}`, data, async (updatedData) => {
                await dbProvider.updateRecord(path, updatedData);
                this.log(`Successfully committed changes to ${path}`, "success");
            });
        } catch (err) {
            this.log(`Edit failed: ${err.message}`, "error");
        }
    }

    /**
     * Handle global delete action
     */
    async handleDelete(path) {
        if (confirm(`CRITICAL_ACTION: Confirm permanent deletion of ${path}?`)) {
            try {
                await dbProvider.deleteRecord(path);
                this.log(`Record purged from cloud: ${path}`, "success");
            } catch (err) {
                this.log(`Purge failed: ${err.message}`, "error");
            }
        }
    }

    /**
     * Search/Filter records
     */
    filterData(term) {
        if (!this.currentData) return;
        const filtered = {};
        Object.entries(this.currentData).forEach(([id, val]) => {
            const contents = JSON.stringify({ id, val }).toLowerCase();
            if (contents.includes(term)) filtered[id] = val;
        });
        renderer.renderTable(filtered, `/${this.currentNode}`);
    }

    /**
     * Central Logger (Forensic Terminal)
     * @param {string} msg 
     * @param {string} type - system | error | success | warning
     */
    log(msg, type = "system") {
        const timestamp = new Date().toLocaleTimeString();
        if (type === "error") {
            this.errorLogs.push({ ts: timestamp, msg: msg, type: type });
        }
        const entry = document.createElement('div');
        entry.className = `log-entry ${type}`;
        entry.textContent = `[${timestamp}] ${msg}`;
        this.terminal.append(entry);
        this.terminal.scrollTop = this.terminal.scrollHeight;
    }
}

export const app = new MainController();
window.app = app; // Expose to global for click handlers in renderer
