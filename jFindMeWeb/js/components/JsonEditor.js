import { Store } from '../core/Store.js';
import { FBBridge } from '../services/FirebaseService.js';

/**
 * jFindMe Matrix Manager V3 - Precision JSON Editor
 * Features: Syntax Validation, Real-time Formatting, Cloud Sync Bridge
 */
export class JsonEditor {
    constructor() {
        this.activeRecord = null;
        this.activeField = null;
        this.activeData = null;
        this.init();
    }

    init() {
        // Create overlay container
        const overlay = document.createElement('div');
        overlay.id = 'json-editor-overlay';
        overlay.className = 'fixed inset-0 bg-black/90 backdrop-blur-2xl z-[1000] hidden flex items-center justify-center p-10 font-sans';
        overlay.innerHTML = `
            <div class="bg-[#0d1117] border border-accent/20 w-full max-w-2xl rounded-2xl shadow-[0_0_100px_rgba(0,0,0,0.8)] overflow-hidden flex flex-col max-h-[80vh]">
                <div class="p-4 bg-accent/10 border-b border-accent/20 flex justify-between items-center">
                    <div class="flex flex-col">
                        <span id="editor-title" class="text-accent font-black text-xs uppercase tracking-widest">Metadata Identity Probe</span>
                        <span id="editor-subtitle" class="text-[9px] text-white/40 font-mono">NODE: /undefined/undefined</span>
                    </div>
                    <button onclick="window.closeJsonEditor()" class="text-white/40 hover:text-white transition-colors">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M6 18L18 6M6 6l12 12"></path></svg>
                    </button>
                </div>
                
                <div class="flex-1 p-6 flex flex-col gap-4">
                    <div class="flex-1 relative">
                        <textarea id="json-textarea" 
                                class="w-full h-full bg-black/50 border border-white/5 rounded-xl p-6 font-mono text-sm text-success/90 focus:outline-accent/30 outline-none resize-none scrollbar-thin"
                                spellcheck="false"></textarea>
                        <div id="json-valid-indicator" class="absolute top-4 right-4 text-[10px] font-black px-2 py-0.5 rounded border">VALID_JSON</div>
                    </div>
                    
                    <div id="editor-error" class="bg-danger/10 border border-danger/30 text-danger text-[10px] p-3 rounded-lg font-bold uppercase tracking-widest hidden">
                        SYNTAX_ERROR: INVALID_IDENTITY_CLUSTER
                    </div>
                </div>

                <div class="p-4 bg-black/40 border-t border-white/5 flex justify-end gap-3">
                    <button onclick="window.closeJsonEditor()" class="px-6 py-2 text-xs font-bold text-white/40 hover:text-white transition-all uppercase tracking-widest">Abort</button>
                    <button id="btn-save-json" onclick="window.saveJsonEditor()" class="btn-primary px-8 py-2 text-xs font-bold uppercase tracking-widest shadow-lg shadow-accent/10">Synchronize Cluster</button>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);

        // Bind events
        const textarea = document.getElementById('json-textarea');
        textarea.oninput = () => this.validate();
        
        // Key bindings
        textarea.onkeydown = (e) => {
            if (e.key === 'Escape') window.closeJsonEditor();
            if (e.ctrlKey && e.key === 's') { e.preventDefault(); window.saveJsonEditor(); }
        };

        // Global Handlers
        window.openJsonEditor = (node, id, field) => this.open(node, id, field);
        window.closeJsonEditor = () => this.close();
        window.saveJsonEditor = () => this.save();
    }

    open(node, id, field) {
        this.activeRecord = { node, id };
        this.activeField = field;
        
        // --- LOOKUP FROM STORE (IMMUTABLE PROTECTION) ---
        const state = Store.getState();
        const record = state.data.records.find(r => r.id === id);
        const val = record ? record[field] : null;
        this.activeData = val;

        const overlay = document.getElementById('json-editor-overlay');
        const textarea = document.getElementById('json-textarea');
        const subtitle = document.getElementById('editor-subtitle');

        subtitle.textContent = `NODE: /${node}/${id} | FIELD: ${field}`;
        textarea.value = JSON.stringify(val || {}, null, 4);
        
        overlay.classList.remove('hidden');
        textarea.focus();
        this.validate();
    }

    close() {
        document.getElementById('json-editor-overlay').classList.add('hidden');
    }

    async save() {
        const textarea = document.getElementById('json-textarea');
        try {
            const updatedData = JSON.parse(textarea.value);
            const { node, id } = this.activeRecord;
            
            window.log(`METADATA: Deploying cluster update to /${node}/${id}...`, "system");
            
            await FBBridge.updateRecord(`/${node}/${id}/${this.activeField}`, updatedData);
            
            window.log(`SYNC_COMPLETE: Metadata identity synchronized.`, "success");
            this.close();
            
            // Reload grid
            window.appInstance.loadNode(node);
        } catch (err) {
            this.showError("COMMIT_FAILURE: " + err.message);
        }
    }

    validate() {
        const textarea = document.getElementById('json-textarea');
        const indicator = document.getElementById('json-valid-indicator');
        const saveBtn = document.getElementById('btn-save-json');
        
        try {
            JSON.parse(textarea.value);
            indicator.textContent = 'VALID_JSON';
            indicator.className = 'absolute top-4 right-4 text-[10px] font-black px-2 py-0.5 rounded border border-success/30 text-success bg-success/5';
            saveBtn.disabled = false;
            saveBtn.classList.remove('opacity-20', 'grayscale');
            this.hideError();
        } catch (err) {
            indicator.textContent = 'SYNTAX_ERR';
            indicator.className = 'absolute top-4 right-4 text-[10px] font-black px-2 py-0.5 rounded border border-danger/30 text-danger bg-danger/5';
            saveBtn.disabled = true;
            saveBtn.classList.add('opacity-20', 'grayscale');
        }
    }

    showError(msg) {
        const el = document.getElementById('editor-error');
        el.textContent = msg;
        el.classList.remove('hidden');
    }

    hideError() {
        document.getElementById('editor-error').classList.add('hidden');
    }
}
