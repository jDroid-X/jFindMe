import { Store } from './core/Store.js';
import { Auth } from './core/Auth.js';
import { FBBridge } from './services/FirebaseService.js';
import { DataGrid } from './components/DataGrid.js';
import { SchemaView } from './components/SchemaView.js';
import { SchemaView1 } from './components/SchemaView1.js';
import { JsonEditor } from './components/JsonEditor.js';
import { MapController } from './controllers/MapController.js';
import { DashboardView } from './views/DashboardView.js';
import { SettingsModel } from './models/SettingsModel.js';
import { DeviceModel } from './models/DeviceModel.js';
import { DeviceCardView } from './views/DeviceCardView.js';
import { MapSettingsView } from './views/MapSettingsView.js';
import { DeviceUserView } from './views/DeviceUserView.js';
import { GeofencingView } from './views/GeofencingView.js';
import { EagleAnalyticsTreeView } from './views/EagleAnalyticsTreeView.js';
import { ThemeManager } from './core/ThemeManager.js';
import { FeedbackDialog } from './components/FeedbackDialog.js';
import { UIControls } from './components/UIControls.js';

/**
 * jFindMe Matrix Manager V3 - Main Orchestrator
 * Responsibility: Component Lifecycle & Bridge Management
 */
class App {
    constructor() {
        this.currentView = 'live-map';
        this.initComponents();
        this.initAuth();
        this.bindGlobalHandlers();
    }

    initComponents() {
        this.grid = new DataGrid('workspace-root');
        this.schema = new SchemaView('workspace-root');
        this.schema1 = new SchemaView1('workspace-root');
        this.editor = new JsonEditor();
        this.settingsModel = new SettingsModel();
        this.settingsView = new MapSettingsView({
            onSave: (updated) => {
                this.settingsModel = updated;
                if (window.log) window.log(`SETTINGS_UPDATE: Search Radius set to ${updated.searchRadiusKm}km`, "success");
                if (document.getElementById('nav-dashboard')?.classList.contains('active')) {
                    window.showDashboard();
                }
            }
        });
        this.dashboardView = new DashboardView('workspace-root', {
            onShowDossier: (agentId) => {
                const dev = this.cachedDevices?.find(d => d.agentId === agentId);
                if (dev) {
                    DeviceCardView.showDossierModal(dev, {
                        onPoke: (id) => this.mapController?.pokeDevice(id),
                        onEdit: (id) => this.mapController?.openEditProfileModal(id),
                        onHistory: (id) => {
                            window.showLiveMap();
                            setTimeout(() => this.mapController?.showDeviceHistoryTrail(id), 400);
                        }
                    });
                }
            },
            onOpenSettings: () => {
                this.settingsView.show(this.settingsModel);
            },
            onNavigateMap: (agentId) => {
                window.showLiveMap();
                setTimeout(() => {
                    const dev = this.mapController?.deviceController.getDeviceById(agentId);
                    if (dev) this.mapController?.handleDeviceSelected(dev);
                }, 400);
            }
        });

        this.deviceUserView = new DeviceUserView('workspace-root', {
            onNavigateMap: (agentId) => {
                window.showLiveMap();
                setTimeout(() => {
                    const dev = this.mapController?.deviceController.getDeviceById(agentId);
                    if (dev) this.mapController?.handleDeviceSelected(dev);
                }, 400);
            }
        });

        this.geofencingView = new GeofencingView('workspace-root', {
            onNavigateMap: (zoneId) => {
                window.showLiveMap();
                setTimeout(() => {
                    if (this.mapController) this.mapController.fitAll();
                }, 400);
            }
        });

        this.eagleAnalyticsView = new EagleAnalyticsTreeView('workspace-root', {
            onNavigateMap: (agentId) => {
                window.showLiveMap();
                setTimeout(() => {
                    const dev = this.mapController?.deviceController.getDeviceById(agentId);
                    if (dev) this.mapController?.handleDeviceSelected(dev);
                }, 400);
            }
        });
        
        // Theme & Modal Dialog Systems
        ThemeManager.init();
        window.toggleTheme = () => ThemeManager.toggleTheme();
        window.FeedbackDialog = FeedbackDialog;
        window.UIControls = UIControls;
        window.notify = (msg, type = 'info', opt = {}) => UIControls.notify(msg, type, opt);

        // Global Logging Engine (Descending Order: Latest First)
        window.log = (msg, type = 'system') => {
            const body = document.getElementById('terminal-body') || document.getElementById('terminal');
            if (!body) return;
            const line = document.createElement('div');
            line.className = `log-line log-${type} animate-fade-in border-b border-white/5 pb-1 mb-1`;
            line.setAttribute('data-log-type', type);
            line.textContent = `[${new Date().toLocaleTimeString()}] ${msg}`;
            
            // Prepend to show latest first
            body.prepend(line);
        };

        window.filterLogs = (type) => {
            const body = document.getElementById('terminal-body');
            if (!body) return;
            const lines = body.querySelectorAll('.log-line');
            lines.forEach(l => {
                const logType = l.getAttribute('data-log-type') || 'system';
                if (type === 'all' || logType === type) {
                    l.style.display = 'block';
                } else {
                    l.style.display = 'none';
                }
            });
        };

        window.clearTerminalLogs = () => {
            const body = document.getElementById('terminal-body');
            if (body) body.innerHTML = '<div class="text-secondary opacity-50 mb-2">--- TERMINAL LOGS CLEARED ---</div>';
        };

        window.toggleTerminalExpand = () => {
            const term = document.getElementById('terminal');
            if (!term) return;
            if (term.classList.contains('h-[250px]')) {
                term.classList.remove('h-[250px]');
                term.classList.add('h-[450px]');
            } else {
                term.classList.remove('h-[450px]');
                term.classList.add('h-[250px]');
            }
        };
    }

    async initAuth() {
        if (!Auth.isAuthenticated()) {
            window.log("AUTH_BYPASS: Deploying Developer Auto-Handshake...", "warning");
            try {
                await Auth.autoLogin();
                this.bootstrap();
            } catch (err) {
                this.showLoginOverlay();
            }
        } else {
            this.bootstrap();
        }
    }

    showLoginOverlay() {
        const overlay = document.createElement('div');
        overlay.id = 'login-overlay';
        overlay.className = 'fixed inset-0 bg-black/90 backdrop-blur-2xl z-[999] flex items-center justify-center';
        overlay.innerHTML = `
            <div class="bg-card p-10 rounded-2xl border border-accent/20 w-96 shadow-2xl relative overflow-hidden group">
                <div class="absolute inset-0 bg-accent/5 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none"></div>
                <div class="text-center mb-8">
                    <div class="w-16 h-16 bg-accent-glow rounded-2xl flex items-center justify-center text-accent mx-auto mb-4 border border-accent/30">
                        <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                    </div>
                    <h2 class="text-2xl font-bold tracking-tight mb-2 text-white">Identity Hub</h2>
                    <p class="text-secondary text-sm">Deploy Administrative Handshake</p>
                </div>
                <div class="space-y-4">
                    <input type="password" id="admin-pass" placeholder="Enter Master Key ID" 
                           class="w-full bg-black/50 border border-border rounded-xl p-4 text-center focus:outline-accent outline-none font-mono tracking-widest">
                    <button id="btn-auth" class="w-full btn-primary h-14 text-sm font-bold uppercase tracking-widest shadow-xl shadow-accent/10">Authorize Session</button>
                    <div id="auth-err" class="text-danger text-[10px] text-center font-bold tracking-widest hidden uppercase">ACCESS_DENIED_INVALID_MATRIX_KEY</div>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);

        document.getElementById('btn-auth').onclick = async () => {
            const pass = document.getElementById('admin-pass').value;
            try {
                await Auth.login(pass);
                overlay.classList.add('opacity-0', 'scale-110');
                setTimeout(() => {
                    overlay.remove();
                    this.bootstrap();
                }, 400);
            } catch (err) {
                document.getElementById('auth-err').classList.remove('hidden');
                document.getElementById('admin-pass').classList.add('border-danger/50');
            }
        };

        // Support Enter key
        document.getElementById('admin-pass').onkeypress = (e) => {
            if (e.key === 'Enter') document.getElementById('btn-auth').click();
        };
    }

    async bootstrap() {
        window.log("MATRIX_BOOT: Establishing Cloud Matrix Bridge...", "system");
        
        try {
            // 1. Verify Proxy Availability & Discover Nodes
            const data = await FBBridge.getDiscovery();
            
            // 2. Map Multi-Cloud Nodes (RTDB + Firestore)
            const rtdbNodes = (data.rtdb || []).filter(k => !['system_audit', 'config', 'discovery'].includes(k))
                                                .map(name => ({ name, type: 'rtdb' }));
            
            const firestoreNodes = (data.firestore || []).map(name => ({ name, type: 'firestore' }));
            
            const collections = [...rtdbNodes, ...firestoreNodes];
            
            Store.dispatch('SET_COLLECTIONS', collections);
            Store.dispatch('SET_CONFIG', { projectId: data.projectId, databaseURL: data.databaseURL });
            
            window.log(`BRIDGE_STABLE: Mapped ${collections.length} Cloud Nodes from [${data.projectId}].`, "success");

            // Update Header UI
            const label = document.getElementById('project-id-label');
            if (label) {
                label.textContent = data.projectId || "FIREBASE_MATRIX";
                label.className = "px-3 py-1 rounded-full bg-success/20 text-success text-[10px] font-black tracking-tighter uppercase";
            }
            
            this.renderSidebar(collections);

            // Default Route: Launch Flagship Live Map Radar
            window.showLiveMap();
            
            this.startHeartbeat();

            // After successful retrieval, hide terminal unless we are in Audit mode
            document.getElementById('terminal').style.display = 'none';
        } catch (err) {
            window.log(`BRIDGE_FAILURE: ${err.message}`, "error");
            window.log("ACTION: Verify 'service-account.json' and 'jFireBase.bat' state.", "warning");
        }
    }

    startHeartbeat() {
        if (this.heartbeatTimer) clearInterval(this.heartbeatTimer);
        this.heartbeatTimer = setInterval(async () => {
            if (this._syncing) return;
            
            // Guard: Only poll raw collection node if currently viewing the DataGrid tab
            if (this.currentView !== 'datagrid') return;

            const state = Store.getState();
            const activeNode = state.navigation.activeNode;
            
            if (activeNode && document.visibilityState === 'visible' && !document.querySelector('.schema-row-1')) {
                this._syncing = true;
                try {
                    const limit = this.grid.pageSize || 5;
                    const marker = this.pageMarkers ? this.pageMarkers[this.grid.currentPage - 1] : null;
                    const nodeType = this.activeNodeType || 'rtdb';
                    
                    const data = await FBBridge.getNodeData(activeNode, limit + (marker ? 1 : 0), marker, nodeType);
                    let normalized = this.normalizeRecords(activeNode, data);

                    if (marker && normalized.length > 0 && normalized[0].id === marker) {
                        normalized.shift();
                    }
                    
                    // Optimized Parity Check: Length first, then deep compare
                    if (normalized.length !== state.data.records.length || 
                        JSON.stringify(normalized) !== JSON.stringify(state.data.records)) {
                        Store.dispatch('SET_RECORDS', normalized);
                        this.updateSyncUI('SYNCED');
                    }
                } catch (err) {
                    console.warn("HEARTBEAT_SKIP: Signal weak.");
                    this.updateSyncUI('ERROR');
                } finally {
                    this._syncing = false;
                }
            }
        }, 15000); 
    }

    renderSidebar(collections) {
        const list = document.getElementById('collection-list');
        if (!list) return;
        
        list.innerHTML = collections.map(col => {
            const { name, type } = col;
            const config = Store.getState().system.config;
            const fbUrl = type === 'rtdb' 
                ? `https://console.firebase.google.com/u/0/project/${config.projectId}/database/realtime/data/${name}`
                : `https://console.firebase.google.com/u/0/project/${config.projectId}/firestore/data/~2F${name}`;
            
            const typeTag = type === 'rtdb' ? 'RTDB' : 'FS';
            const tagColor = type === 'rtdb' ? 'text-accent/60' : 'text-success/60';

            return `
                <div class="nav-item group flex items-center justify-between" data-node="${name}" data-type="${type}" onclick="window.loadCollection('${name}', this, '${type}')">
                    <div class="flex items-center gap-2">
                        <span class="text-[7px] font-bold px-1 rounded border border-white/10 ${tagColor}">${typeTag}</span>
                        <span class="truncate">${name}</span>
                    </div>
                    <a href="${fbUrl}" target="_blank" onclick="event.stopPropagation()" class="opacity-0 group-hover:opacity-40 hover:!opacity-100 transition-all p-1" title="Open in Firebase Console">
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"></path></svg>
                    </a>
                </div>
            `;
        }).join('');
    }

    async loadNode(name, limit = 5, type = 'rtdb') {
        Store.dispatch('SET_ACTIVE_NODE', name);
        this.activeNodeType = type; // Persist type for heartbeat
        this.updateSyncUI('PENDING');
        localStorage.setItem('jFindMe_LastNode', name);
        
        // Reset Pagination
        this.pageMarkers = [null]; // First page starts at null
        this.grid.currentPage = 1;
        
        // Update Title & Cloud Path
        const header = document.querySelector('header h1');
        if (header) header.textContent = name.charAt(0).toUpperCase() + name.slice(1);
        
        const link = document.getElementById('cloud-path-link');
        const config = Store.getState().system.config;
        if (link) {
            const fbUrl = type === 'rtdb' 
                ? `https://console.firebase.google.com/u/0/project/${config.projectId}/database/realtime/data/${name}`
                : `https://console.firebase.google.com/u/0/project/${config.projectId}/firestore/data/~2F${name}`;
            link.href = fbUrl;
            link.textContent = `SOURCE: ${type.toUpperCase()} | PATH: /${name}`;
        }
        
        this.showProgress();
        try {
            window.log(`PAGINATION_INIT: Requesting first cluster of [${name}] from ${type.toUpperCase()} | Size: ${limit}`, "system");
            const data = await FBBridge.getNodeData(name, limit, null, type);
            
            if (!data && FBBridge.currentController?.signal.aborted) return;

            const normalized = this.normalizeRecords(name, data);

            // Store the next page marker (the ID of the last record)
            if (normalized.length === limit) {
                this.pageMarkers[1] = normalized[normalized.length - 1].id;
            }

            Store.dispatch('SET_RECORDS', normalized);
            this.updateSyncUI('SYNCED');
            window.log(`NODE_SYNC: Resolved cluster [${name}] | Records: ${normalized.length}`, "success");
        } catch (err) {
            if (err.name !== 'AbortError') {
                this.updateSyncUI('ERROR');
                window.log(`SYNC_FAILURE: Failed to resolve [${name}]`, "error");
            }
        } finally {
            this.hideProgress();
        }
    }

    async loadNextPage() {
        const state = Store.getState();
        const activeNode = state.navigation.activeNode;
        const limit = this.grid.pageSize || 5;
        const currentRecords = state.data.records;
        
        if (currentRecords.length < limit) return; // No more data

        const lastId = currentRecords[currentRecords.length - 1].id;
        this.showProgress();
        
        try {
            const nodeType = this.activeNodeType || 'rtdb';
            window.log(`EXTENDING_STREAM: Fetching next ${limit} nodes after [${lastId}] from ${nodeType.toUpperCase()}...`, "system");
            // To avoid overlapping the last element, we fetch limit + 1 and skip the first one
            const data = await FBBridge.getNodeData(activeNode, limit + 1, lastId, nodeType);
            
            let normalized = this.normalizeRecords(activeNode, data);

            // Remove the anchor (first element) as it overlaps with previous page
            if (normalized.length > 0 && normalized[0].id === lastId) {
                normalized.shift();
            }

            if (normalized.length === 0) {
                window.log("END_OF_MATRIX: No subsequent records found.", "warning");
                return;
            }

            this.grid.currentPage++;
            this.pageMarkers[this.grid.currentPage - 1] = lastId; // Save marker for previous

            Store.dispatch('SET_RECORDS', normalized);
            this.updateSyncUI('SYNCED');
            window.log(`PAGE_LOAD_COMPLETE: Viewing Sector ${this.grid.currentPage}`, "success");
        } catch (err) {
            window.log(`PAGE_LOAD_ERROR: ${err.message}`, "error");
        } finally {
            this.hideProgress();
        }
    }

    async loadPrevPage() {
        if (this.grid.currentPage <= 1) return;

        const state = Store.getState();
        const activeNode = state.navigation.activeNode;
        const limit = this.grid.pageSize || 5;
        this.grid.currentPage--;
        
        // Get the marker for the desired page
        // Page 1 -> marker 0 (null)
        // Page 2 -> marker 1 ...
        const marker = this.pageMarkers[this.grid.currentPage - 1];
        
        this.showProgress();
        try {
            const nodeType = this.activeNodeType || 'rtdb';
            window.log(`REVERTING_STREAM: Fetching Sector ${this.grid.currentPage} from ${nodeType.toUpperCase()}...`, "system");
            const data = await FBBridge.getNodeData(activeNode, limit + (marker ? 1 : 0), marker, nodeType);
            
            let normalized = this.normalizeRecords(activeNode, data);

            // If we used a marker, skip the first record
            if (marker && normalized.length > 0 && normalized[0].id === marker) {
                normalized.shift();
            }

            Store.dispatch('SET_RECORDS', normalized);
            this.updateSyncUI('SYNCED');
            window.log(`PAGE_LOAD_COMPLETE: Restored Sector ${this.grid.currentPage}`, "success");
        } catch (err) {
            window.log(`PAGE_LOAD_ERROR: ${err.message}`, "error");
        } finally {
            this.hideProgress();
        }
    }

    normalizeRecords(nodeName, data) {
        return Object.entries(data || {}).map(([id, val]) => {
            const isObj = val && typeof val === 'object';
            const keyCount = isObj ? Object.keys(val).length : 0;
            
            // Memory Shield: If object density > 50 keys, do NOT spread.
            // This prevents the DOM from hanging when a single UID node contains thousands of history events.
            if (isObj && keyCount > 50) {
                return { 
                    id, 
                    [nodeName + '_cluster']: `(UNPACK_SHIELDED) ${keyCount} records`,
                    _cluster_data: val, 
                    _is_cluster: true, 
                    _cluster_size: keyCount 
                };
            }
            
            return {
                id,
                ...(isObj ? val : { [nodeName + '_value']: val })
            };
        });
    }

    updateSyncUI(status) {
        const dot = document.getElementById('sync-pulsar-dot');
        const label = document.getElementById('sync-pulsar-label');
        if (!dot || !label) return;

        switch(status) {
            case 'PENDING':
                dot.className = "w-1.5 h-1.5 rounded-full bg-warning animate-pulse shadow-[0_0_8px_#f59e0b]";
                label.textContent = "SYNC_PENDING";
                label.className = "text-[8px] font-black tracking-[0.2em] text-warning uppercase";
                break;
            case 'SYNCED':
                dot.className = "w-1.5 h-1.5 rounded-full bg-success shadow-[0_0_8px_#22c55e]";
                label.textContent = "SYNC_ACTIVE";
                label.className = "text-[8px] font-black tracking-[0.2em] text-success uppercase";
                break;
            case 'ERROR':
                dot.className = "w-1.5 h-1.5 rounded-full bg-danger";
                label.textContent = "SYNC_FAILURE";
                label.className = "text-[8px] font-black tracking-[0.2em] text-danger uppercase";
                break;
            default:
                dot.className = "w-1.5 h-1.5 rounded-full bg-white/10";
                label.textContent = "SYNC_IDLE";
                label.className = "text-[8px] font-black tracking-[0.2em] text-white/30 uppercase";
        }
    }

    showProgress() {
        const wrap = document.getElementById('forensic-progress');
        const bar = document.getElementById('progress-bar');
        if (!wrap || !bar) return;

        wrap.classList.remove('hidden');
        bar.style.width = '10%';
        
        let progress = 10;
        let startTime = Date.now();
        
        if (this.progressInterval) clearInterval(this.progressInterval);
        this.progressInterval = setInterval(() => {
            if (progress < 90) {
                progress += (90 - progress) * 0.1;
                bar.style.width = `${progress}%`;
            }
        }, 300);
    }

    hideProgress() {
        const wrap = document.getElementById('forensic-progress');
        const bar = document.getElementById('progress-bar');
        if (!wrap || !bar) return;

        if (this.progressInterval) clearInterval(this.progressInterval);
        bar.style.width = '100%';
        setTimeout(() => {
            wrap.classList.add('hidden');
            bar.style.width = '0';
        }, 200);
    }

    bindGlobalHandlers() {
        window.appInstance = this;

        window.showLiveMap = () => {
            this.currentView = 'live-map';
            Store.dispatch('SET_NAV', { activeTab: 'live-map', activeView: 'live-map', activeNode: null });
            document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
            document.getElementById('nav-live-map')?.classList.add('active');
            
            const header = document.querySelector('header h1');
            if (header) header.textContent = "Live Map Radar (10 km Proximity Matrix)";
            
            const pathLink = document.getElementById('cloud-path-link');
            if (pathLink) {
                pathLink.textContent = "SOURCE: RTDB | PATH: /devices (Tactical Live Map)";
                pathLink.href = "#";
            }
            
            const terminal = document.getElementById('terminal');
            if (terminal) terminal.style.display = 'none';

            const workspace = document.getElementById('workspace-root');
            if (!workspace) return;

            workspace.innerHTML = `
                <div id="map-workspace" class="flex h-full w-full overflow-hidden relative font-sans">
                    <!-- Center Map Canvas (Full Viewport) -->
                    <div id="map-canvas" class="w-full h-full absolute inset-0 z-0"></div>

                    <!-- TOP EMERGENCY ALERT BANNER CONTAINER -->
                    <div id="map-alert-banner-container" class="absolute top-16 left-1/2 transform -translate-x-1/2 z-[920] w-[90%] max-w-xl hidden"></div>

                    <!-- Floating Top Center Action Toolbar (Theme-Adaptive) -->
                    <div class="absolute top-2.5 left-1/2 transform -translate-x-1/2 z-20 flex items-center gap-1 select-none bg-white/95 dark:bg-slate-950/90 backdrop-blur-md p-0.5 rounded-xl border border-slate-200 dark:border-white/10 shadow-xl pointer-events-auto">
                        <button class="px-2 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 font-bold text-[10px] transition-all flex items-center gap-1 border border-slate-200/60 dark:border-white/5" onclick="window.MatrixApp.openCreateSafeZone()">
                            <span>🛡️</span>
                            <span>+ Safe Zone</span>
                        </button>
                        <button class="px-2 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 font-bold text-[10px] transition-all flex items-center gap-1 border border-slate-200/60 dark:border-white/5" onclick="window.MatrixApp.openSettings()">
                            <span>⚙️</span>
                            <span>Settings</span>
                        </button>
                        <button class="px-2 py-1 rounded-lg bg-sky-600 hover:bg-sky-500 text-white font-bold text-[10px] transition-all flex items-center gap-1 shadow-sm" onclick="window.MatrixApp.fitAll()">
                            <span>🎯</span>
                            <span>Center Fleet</span>
                        </button>
                        <button class="px-2 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 font-bold text-[10px] transition-all flex items-center gap-1 border border-slate-200/60 dark:border-white/5" onclick="window.MatrixApp.refreshNow()">
                            <span>🔄</span>
                            <span>Refresh</span>
                        </button>
                    </div>

                    <!-- Floating Left Selection Matrix & Right Leaflet Device Card Container -->
                    <div id="device-sidebar" class="absolute inset-0 pointer-events-none z-20 pt-3 pb-3"></div>

                    <!-- Bottom-Left Realtime Coordinate Tracker -->
                    <div class="absolute bottom-4 left-4 z-20 bg-white/90 backdrop-blur-md px-3 py-1.5 rounded-xl border border-slate-200 shadow-md text-[10px] font-mono text-slate-600 flex items-center gap-2 select-none">
                        <span class="text-sky-600 font-bold">CURSOR:</span>
                        <span id="map-cursor-coords" class="text-slate-800 font-bold">--°, --°</span>
                    </div>
                </div>
            `;

            if (this.mapController) {
                this.mapController.destroy();
            }
            this.mapController = new MapController('map-canvas', 'device-sidebar');
            this.mapController.init();

            window.MatrixApp = {
                focusDevice: (agentId) => {
                    if (!this.mapController) {
                        window.showLiveMap();
                        setTimeout(() => {
                            const dev = this.mapController?.deviceController?.getDeviceById(agentId);
                            if (dev) this.mapController?.handleDeviceSelected(dev);
                        }, 300);
                        return;
                    }
                    const dev = this.mapController.deviceController?.getDeviceById(agentId);
                    if (dev) this.mapController.handleDeviceSelected(dev);
                },
                toggleDeviceSelection: (agentId) => {
                    if (this.mapController) this.mapController.toggleDeviceSelection(agentId);
                },
                selectAllDevices: () => {
                    if (this.mapController) this.mapController.selectAllDevices();
                },
                showDossier: (agentId) => {
                    if (this.mapController) this.mapController.showDeviceDossier(agentId);
                },
                openEditProfile: (agentId) => {
                    if (this.mapController) this.mapController.openEditProfileModal(agentId);
                },
                openCreateSafeZone: () => {
                    if (this.mapController) this.mapController.openCreateSafeZoneModal();
                },
                toggleTraffic: () => {
                    if (this.mapController) this.mapController.toggleTraffic();
                },
                pokeDevice: (agentId) => {
                    if (this.mapController) this.mapController.pokeDevice(agentId);
                },
                showHistory: (agentId) => {
                    if (this.mapController) this.mapController.showDeviceHistoryTrail(agentId);
                },
                openSettings: () => {
                    if (this.mapController) this.mapController.settingsController?.openSettingsModal();
                },
                focusStation: () => {
                    if (this.mapController) this.mapController.focusStation();
                },
                fitAll: () => {
                    if (this.mapController) this.mapController.fitAll();
                },
                toggleFullscreen: () => {
                    if (this.mapController) this.mapController.toggleFullscreen();
                },
                refreshNow: async () => {
                    if (this.mapController) {
                        window.log?.("RADAR_SYNC: Requesting instantaneous device refresh...", "system");
                        await Promise.all([
                            this.mapController.deviceController?.fetchLiveDevices?.(),
                            this.mapController.safeZoneController?.fetchSafeZones?.()
                        ]);
                        window.log?.("RADAR_SYNC: Devices & Geofences synchronized.", "success");
                    }
                }
            };
        };
        
        window.loadCollection = (name, el, type = 'rtdb') => {
            this.currentView = 'datagrid';
            document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
            if (el) {
                el.classList.add('active');
            } else {
                const target = document.querySelector(`.nav-item[data-node="${name}"]`);
                target?.classList.add('active');
            }
            this.loadNode(name, 5, type);
        };
        
        window.showDashboard = async () => {
            this.currentView = 'dashboard';
            Store.dispatch('SET_NAV', { activeTab: 'dashboard', activeView: 'dashboard', activeNode: null });
            document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
            document.getElementById('nav-dashboard')?.classList.add('active');
            
            const header = document.querySelector('header h1');
            if (header) header.textContent = "Fleet Proximity & Location Dashboard";

            const pathLink = document.getElementById('cloud-path-link');
            if (pathLink) {
                pathLink.textContent = "SOURCE: RTDB | PATH: /devices-live";
                pathLink.href = "#";
            }
            
            const terminal = document.getElementById('terminal');
            if (terminal) terminal.style.display = 'none';

            // Fetch latest live devices and active settings
            try {
                this.settingsModel.load();
                const res = await fetch('/api/devices-live');
                const rawData = await res.json();
                const rawDevices = Array.isArray(rawData) ? rawData : (rawData.devices || []);
                this.cachedDevices = rawDevices.map(d => new DeviceModel(d));
                this.dashboardView.render(this.cachedDevices, this.settingsModel);
            } catch (err) {
                console.error("[DASHBOARD] Failed to load devices:", err);
                this.dashboardView.render([], this.settingsModel);
            }
        };

        window.showDeviceUser = window.showUsers = async (agentId = null) => {
            this.currentView = 'device-user';
            Store.dispatch('SET_NAV', { activeTab: 'device-user', activeView: 'device-user', activeNode: null });
            document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
            document.getElementById('nav-users')?.classList.add('active');
            
            const header = document.querySelector('header h1');
            if (header) header.textContent = "Device User Oversight Matrix";

            const pathLink = document.getElementById('cloud-path-link');
            if (pathLink) {
                pathLink.textContent = "SOURCE: RTDB | PATH: /devices + /Geofencing";
                pathLink.href = "#";
            }
            
            const terminal = document.getElementById('terminal');
            if (terminal) terminal.style.display = 'none';

            try {
                const [devRes, zoneRes] = await Promise.all([
                    fetch('/api/devices-live').then(r => r.json()),
                    fetch('/api/safe-zones').then(r => r.json())
                ]);
                const devicesPayload = devRes.devices || devRes;
                this.cachedDevices = (Array.isArray(devicesPayload) ? devicesPayload : []).map(d => new DeviceModel(d));
                const safeZones = zoneRes.safeZones || [];
                this.deviceUserView.render(this.cachedDevices, agentId, safeZones);
            } catch (err) {
                console.error("[DEVICE_USER] Failed to load devices:", err);
                this.deviceUserView.render([], agentId, []);
            }
        };

        window.showGeofencing = async () => {
            this.currentView = 'geofencing';
            Store.dispatch('SET_NAV', { activeTab: 'geofencing', activeView: 'geofencing', activeNode: null });
            document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
            document.getElementById('nav-geofencing')?.classList.add('active');
            
            const header = document.querySelector('header h1');
            if (header) header.textContent = "Geofencing Perimeter Oversight Matrix";

            const pathLink = document.getElementById('cloud-path-link');
            if (pathLink) {
                pathLink.textContent = "SOURCE: RTDB | PATH: /Geofencing";
                pathLink.href = "#";
            }
            
            const terminal = document.getElementById('terminal');
            if (terminal) terminal.style.display = 'none';

            try {
                const [devRes, zoneRes] = await Promise.all([
                    fetch('/api/devices-live').then(r => r.json()),
                    fetch('/api/safe-zones').then(r => r.json())
                ]);
                const rawDevices = devRes.devices || devRes;
                this.cachedDevices = (Array.isArray(rawDevices) ? rawDevices : []).map(d => new DeviceModel(d));
                const safeZones = zoneRes.safeZones || [];
                this.geofencingView.render(this.cachedDevices, safeZones);
            } catch (err) {
                console.error("[GEOFENCING] Failed to load zones:", err);
                this.geofencingView.render([], []);
            }
        };

        window.showEagleAnalytics = async () => {
            this.currentView = 'eagle-analytics';
            Store.dispatch('SET_NAV', { activeTab: 'eagle-analytics', activeView: 'eagle-analytics', activeNode: null });
            document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
            document.getElementById('nav-eagle-analytics')?.classList.add('active');
            
            const header = document.querySelector('header h1');
            if (header) header.textContent = "Eagle-Eye Analytics Radar (Macro to Micro Explorer)";

            const pathLink = document.getElementById('cloud-path-link');
            if (pathLink) {
                pathLink.textContent = "SOURCE: RTDB + GIS ENGINE | PATH: /analytics-tree";
                pathLink.href = "#";
            }
            
            const terminal = document.getElementById('terminal');
            if (terminal) terminal.style.display = 'none';

            try {
                const [devRes, zoneRes] = await Promise.all([
                    fetch('/api/devices-live').then(r => r.json()),
                    fetch('/api/safe-zones').then(r => r.json())
                ]);
                const devicesPayload = devRes.devices || devRes;
                this.cachedDevices = (Array.isArray(devicesPayload) ? devicesPayload : []).map(d => new DeviceModel(d));
                const safeZones = zoneRes.safeZones || [];
                
                this.eagleAnalyticsView.setDevices(this.cachedDevices);
                this.eagleAnalyticsView.setSafeZones(safeZones);
                this.eagleAnalyticsView.render();
            } catch (err) {
                console.error("[EAGLE_ANALYTICS] Failed to load data:", err);
                this.eagleAnalyticsView.setDevices([]);
                this.eagleAnalyticsView.setSafeZones([]);
                this.eagleAnalyticsView.render();
            }
        };

        window.showAudit = () => {
            this.currentView = 'audit';
            Store.dispatch('SET_NAV', { activeTab: 'audit', activeView: 'audit', activeNode: null });
            document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
            document.getElementById('nav-audit').classList.add('active');
            document.querySelector('header h1').textContent = "Forensic Audit Ledger";
            
            const pathLink = document.getElementById('cloud-path-link');
            if (pathLink) {
                pathLink.textContent = "SOURCE: RTDB | PATH: /system_audit";
                pathLink.href = "#";
            }
            
            document.getElementById('terminal').style.display = 'block';
            this.loadNode('system_audit');
        };

        window.showSchema = () => {
            this.currentView = 'schema';
            document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
            document.getElementById('btn-schema').classList.add('active');
            document.querySelector('header h1').textContent = "Database Schema Architect";
            document.getElementById('terminal').style.display = 'none';
            this.schema.render();
        };

        window.showArchitectV2 = () => {
            this.currentView = 'architect';
            Store.dispatch('SET_NAV', { activeTab: 'architect', activeView: 'architect', activeNode: null });
            document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
            document.getElementById('nav-architect')?.classList.add('active');
            document.querySelector('header h1').textContent = "Database Architect V2 (Hierarchical)";
            
            const pathLink = document.getElementById('cloud-path-link');
            if (pathLink) {
                pathLink.textContent = "SOURCE: RTDB + FS | PATH: /SchemaMatrix";
                pathLink.href = "#";
            }
            
            document.getElementById('terminal').style.display = 'none';
            this.schema1.render();
        };

        window.handleSearch = (term) => Store.dispatch('SET_SEARCH', term);
        window.logout = () => Auth.logout();

        window.generateForensicReport = async () => {
             const state = Store.getState();
             const selectedIds = state.data.selection || []; // Check for grid selection
             
             // V2 Relational Registry
             const v2Nodes = ['users', 'groups', 'locations', 'history', 'pairing_requests'];
             
             const reportWindow = window.open('', '_blank');
             if (!reportWindow) return window.log("REPORT_BLOCKED", "error");
             
             reportWindow.document.write(`<body style="background:#05070a;color:#f59e0b;padding:50px;font-family:sans-serif;"><h2>EXECUTING_RELATIONAL_FILTER_PROBE...</h2><div id="logs" style="font-size:10px;color:rgba(255,255,255,0.3)"></div></body>`);
             const log = (msg) => { reportWindow.document.getElementById('logs').innerHTML += `<div>${msg}</div>`; };

             try {
                 log("V2_WATERFALL: Initializing multi-node handshake...");
                 const resultData = {};
                 
                 await Promise.all(v2Nodes.map(async (key) => {
                     log(`FETCHING [${key}] (RTDB)...`);
                     const d = await FBBridge.getNodeData(key, 2000, null, 'rtdb');
                     resultData[key] = d || {};
                 }));

                 // 1.5 Fetch COLD DATA (Firestore)
                 const fsNodes = ['avatars', 'sessions', 'cells'];
                 await Promise.all(fsNodes.map(async (key) => {
                     log(`FETCHING [${key}] (FIRESTORE)...`);
                     const d = await FBBridge.getNodeData(key, 1000, null, 'firestore');
                     resultData[key] = d || {};
                 }));

                 // 2. FILTERING_LAYER: If users are selected, only audit those personas. Otherwise audit all.
                 let usersToAudit = resultData['users'] || {};
                 if (selectedIds.length > 0) {
                     log(`FILTER_ACTIVE: Isolating ${selectedIds.length} Target Identity(s)...`);
                     usersToAudit = Object.keys(usersToAudit)
                         .filter(uid => selectedIds.includes(uid))
                         .reduce((obj, key) => { obj[key] = usersToAudit[key]; return obj; }, {});
                 }

                 await Promise.all(Object.entries(usersToAudit).map(async ([uid, userData]) => {
                     log(`SURGICAL_PROBE [history/${uid}]...`);
                     const h = await FBBridge.getNodeData(`history/${uid}`, 1000, null, 'rtdb');
                     if (!resultData['history']) resultData['history'] = {};
                     // Merge targeted history into the relational matrix
                     resultData['history'][uid] = h || {};
                 }));

                 let reportHtml = `
                 <!DOCTYPE html>
                 <html>
                 <head>
                     <title>V2_RELATIONAL_AUDIT</title>
                     <script src="https://cdn.tailwindcss.com"></script>
                     <style>
                         @import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono&family=Outfit:wght@900&display=swap');
                         body { background: #05070a; color: #fff; font-family: sans-serif; }
                         .persona-card { border-left: 2px solid #f59e0b; background: rgba(255,255,255,0.01); margin-bottom: 80px; padding: 40px; border-radius: 0 10px 10px 0; border: 1px solid rgba(255,255,255,0.03); border-left-width: 4px; }
                         .step-tag { background: #f59e0b; color: #000; font-weight: 900; padding: 2px 8px; font-size: 8px; border-radius: 2px; }
                     </style>
                 </head>
                 <body class="p-16">
                     <div class="max-w-7xl mx-auto">
                         <div class="mb-20 border-b border-white/10 pb-10">
                             <h1 class="text-6xl font-black italic tracking-tighter uppercase mb-2">Forensic_Audit</h1>
                             <div class="flex items-center gap-4 text-[10px] font-bold text-white/20 uppercase tracking-[0.5em]">
                                 <span>Waterfall_Relational_v2.0</span>
                                 <span>|</span>
                                 <span>Filtered_Personas: ${Object.keys(usersToAudit).length}</span>
                             </div>
                         </div>
                 `;

                 Object.entries(usersToAudit).forEach(([uid, userData]) => {
                     // Flatten nested history for this specific user
                     const userHistory = resultData['history'] ? resultData['history'][uid] : {};

                     reportHtml += `
                         <div class="persona-card">
                             <div class="mb-12">
                                 <span class="step-tag mb-2 inline-block uppercase">Hub_Identity</span>
                                 <h2 class="text-4xl font-black text-accent uppercase tracking-tighter">${userData.name || uid}</h2>
                                 <p class="text-[10px] font-mono text-white/30 lowercase mt-1">${uid}</p>
                             </div>
                             
                             <!-- RELATIONAL TABLE STACK -->
                             <div class="space-y-12">
                                 <section>
                                     <h3 class="text-[10px] font-black text-white/20 uppercase tracking-widest mb-4">TABLE 1: Identity_Metadata</h3>
                                     ${this.renderMiniTable(userData)}
                                 </section>

                                 <section>
                                     <h3 class="text-[10px] font-black text-white/20 uppercase tracking-widest mb-4">TABLE 2: Pairing_Network (Join: fromUID)</h3>
                                     ${this.renderRelationTable(resultData['pairing_requests'], uid, 'fromUID', 'pairing_requests')}
                                 </section>

                                 <section>
                                     <h3 class="text-[10px] font-black text-white/20 uppercase tracking-widest mb-4">TABLE 3: Group_Associations (Join: host)</h3>
                                     ${this.renderRelationTable(resultData['groups'], uid, 'host', 'groups')}
                                 </section>

                                 <section>
                                     <h3 class="text-[10px] font-black text-white/20 uppercase tracking-widest mb-4">TABLE 4: Realtime_Telemetry</h3>
                                     ${this.renderRelationTable(resultData['locations'], uid, null, 'locations')}
                                 </section>

                                 <section>
                                     <h3 class="text-[10px] font-black text-white/20 uppercase tracking-widest mb-4">TABLE 5: Event_History_Logs (Targeted_Probe)</h3>
                                     ${this.renderRelationTable(userHistory, uid, null, `history/${uid}`)}
                                 </section>
                             </div>
                         </div>
                     `;
                 });

                 reportHtml += `</div></body></html>`;
                 reportWindow.document.open();
                 reportWindow.document.write(reportHtml);
                 reportWindow.document.close();
             } catch (err) {
                 reportWindow.document.write(`<div style="color:red;padding:40px;">AUDIT_ENGINE_FAULT: ${err.message}</div>`);
             }
        };

        // Cross-Window Forensic Tool: Decommission nodes from the Dossier View
        window.deleteDossierNode = async (node, id) => {
            if (!confirm(`PERSONA_AUDIT_ACTION: Permanently decommission node [${id}] from node [${node}]?`)) return;
            try {
                await FBBridge.deleteRecord(`/${node}/${id}`);
                window.log(`DOSSIER_ACTION: Node [${id}] decommissioned successfully.`, "success");
                // The actual row in the child window will be hidden by the child script or the user can refresh
            } catch (err) {
                window.log(`DOSSIER_FAILURE: ${err.message}`, "error");
            }
        };
    }

    renderMiniTable(data) {
        const entries = Object.entries(data);
        return `
            <div class="mb-4 overflow-x-auto shadow-lg rounded-lg border border-white/10 bg-black/40">
                <table class="w-full text-left text-[11px] border-collapse">
                    <thead class="bg-black/60">
                        <tr>${entries.map(([k]) => `<th class="p-2 border border-white/5 text-accent font-black uppercase text-[9px]">${k}</th>`).join('')}</tr>
                    </thead>
                    <tbody>
                        <tr>${entries.map(([,v]) => `<td class="p-2 border border-white/5 text-white/70 mono whitespace-nowrap">${typeof v === 'object' ? 'JSON' : v}</td>`).join('')}</tr>
                    </tbody>
                </table>
            </div>
        `;
    }

    renderRelationTable(fullData, value, field = null, nodeKey = null) {
        // Universal Relational Matcher: Targeted field check OR fuzzy key scan
        const matches = Object.entries(fullData || {}).filter(([k, v]) => {
            if (k === value) return true;
            if (field && v[field] === value) return true;
            const fuzzyKeys = ['uid', 'userId', 'user_id', 'id'];
            return fuzzyKeys.some(sk => v[sk] === value);
        });

        if (matches.length === 0) return '<div class="text-[9px] text-white/20 italic mb-4 ml-4">-- NO_RELATIONAL_DATA_FOUND --</div>';
        
        const headers = Array.from(new Set(matches.flatMap(([, v]) => Object.keys(v))));
        return `
            <div class="mb-4 overflow-x-auto shadow-lg rounded-lg border border-white/10 bg-black/20">
                <table class="w-full text-left text-[11px] border-collapse">
                    <thead class="bg-black/40">
                        <tr>
                            <th class="p-2 border border-white/5 text-white/40 font-black uppercase text-[9px]">ACTIONS</th>
                            <th class="p-2 border border-white/5 text-white/40 font-black uppercase text-[9px]">REF_ID</th>
                            ${headers.map(h => `<th class="p-2 border border-white/5 text-white/40 font-black uppercase text-[9px]">${h}</th>`).join('')}
                        </tr>
                    </thead>
                    <tbody>
                        ${matches.map(([id, val]) => `
                            <tr id="row-${nodeKey}-${id}">
                                <td class="p-2 border border-white/5 whitespace-nowrap">
                                    <button onclick="window.opener.deleteDossierNode('${nodeKey}', '${id}')" class="text-red-500 hover:text-red-400 font-bold px-1" title="DECOMMISSION_FROM_CLOUD">
                                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                                    </button>
                                </td>
                                <td class="p-2 border border-white/5 font-bold text-accent/60">${id}</td>
                                ${headers.map(h => `<td class="p-2 border border-white/5 text-white/60 mono whitespace-nowrap">${val[h] !== undefined ? (typeof val[h] === 'object' ? 'JSON' : val[h]) : '--'}</td>`).join('')}
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    }

    renderAnalytics() {
        const collections = Store.getState().data.collections;
        const workspace = document.getElementById('workspace-root');
        
        workspace.innerHTML = `
            <div class="p-8 h-full bg-[#05070a] overflow-auto scrollbar-thin">
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
                    ${collections.map(node => `
                        <div class="bg-gradient-to-br from-[#121820] to-[#0a0c12] p-6 rounded-xl border border-white/5 shadow-2xl relative overflow-hidden group hover:border-accent/30 transition-all cursor-pointer" 
                             onclick="window.loadCollection('${node}')">
                            <div class="absolute -right-4 -top-4 w-24 h-24 bg-accent/5 rounded-full blur-3xl group-hover:bg-accent/10 transition-all"></div>
                            <div class="flex flex-col gap-1 relative z-10">
                                <span class="text-[10px] font-black tracking-[0.3em] text-accent uppercase">${node}</span>
                                <div class="flex items-center gap-3">
                                    <span class="text-3xl font-black text-white">...</span>
                                    <span class="text-[10px] text-white/20 font-mono italic">FETCHING_SIGNAL</span>
                                </div>
                                <div class="mt-4 w-full h-1 bg-white/5 rounded-full overflow-hidden">
                                    <div class="w-2/3 h-full bg-accent/20"></div>
                                </div>
                            </div>
                        </div>
                    `).join('')}
                </div>
                
                <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    <div class="lg:col-span-2 bg-[#0d1117] rounded-xl border border-white/5 p-6 h-[400px]">
                        <div class="flex justify-between items-center mb-6">
                            <span class="text-[11px] font-black text-white/40 tracking-widest uppercase">Forensic Traffic Stream</span>
                            <div class="flex gap-2">
                                <div class="w-2 h-2 rounded-full bg-success"></div>
                                <div class="w-2 h-2 rounded-full bg-white/10"></div>
                            </div>
                        </div>
                        <div class="h-64 flex items-end gap-3 px-4">
                            ${Array(12).fill(0).map(() => `<div class="flex-1 bg-accent/10 hover:bg-accent/40 transition-all rounded-t-sm" style="height: ${Math.random() * 80 + 20}%"></div>`).join('')}
                        </div>
                    </div>
                    <div class="bg-[#0d1117] rounded-xl border border-white/5 p-6 flex flex-col gap-6">
                         <span class="text-[11px] font-black text-white/40 tracking-widest uppercase">Cluster Integrity</span>
                         <div class="flex-1 flex items-center justify-center">
                            <div class="w-32 h-32 rounded-full border-8 border-accent/20 border-t-accent animate-spin-slow"></div>
                         </div>
                         <div class="text-center text-[10px] text-white/30 font-mono tracking-tighter">L7 SECURITY COMPLIANT ENGINE</div>
                    </div>
                </div>
            </div>
        `;
    }
}

// Global Exposure for UI attributes
window.Store = Store;

// Launch Enterprise Matrix (Singleton Guard)
if (!window.appInstance) {
    window.appInstance = new App();
    console.log("[SYSTEM] App Singleton Initialized.");
} else {
    console.warn("[SYSTEM] Prevented redundant App instantiation.");
}
