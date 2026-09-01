/**
 * jFindMe Matrix - Universal Modal UI & Tactical Toast Engine (OOPS Architecture)
 * Provides standard modal dialogs with options (Confirm, Prompt, Custom Dialogs) and toast notifications.
 */
export class ModalUI {
    /**
     * Displays a confirmation dialog with options
     * @param {string} title 
     * @param {string} message 
     * @param {object} options { confirmText, cancelText, isDanger, icon }
     * @returns {Promise<boolean>}
     */
    static confirm(title, message, options = {}) {
        return new Promise((resolve) => {
            const confirmText = options.confirmText || "Confirm";
            const cancelText = options.cancelText || "Cancel";
            const isDanger = Boolean(options.isDanger);
            const icon = options.icon || (isDanger ? "⚠️" : "❓");

            const modal = document.createElement('div');
            modal.className = 'fixed inset-0 z-[1200] flex items-center justify-center bg-black/85 backdrop-blur-md p-4 animate-fade-in font-sans';

            modal.innerHTML = `
                <div class="bg-slate-900 border border-white/10 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden text-white flex flex-col">
                    <div class="p-5 border-b border-white/10 flex items-center gap-3 bg-slate-950/80">
                        <div class="w-10 h-10 rounded-xl ${isDanger ? 'bg-red-500/20 text-red-400 border border-red-500/30' : 'bg-sky-500/20 text-sky-400 border border-sky-500/30'} flex items-center justify-center text-lg">
                            ${icon}
                        </div>
                        <div>
                            <h3 class="text-sm font-black tracking-tight text-white uppercase">${title}</h3>
                        </div>
                    </div>

                    <div class="p-5 text-xs text-slate-300 leading-relaxed font-sans">
                        ${message}
                    </div>

                    <div class="p-4 border-t border-white/10 bg-slate-950 flex items-center justify-end gap-3">
                        <button id="modal-btn-cancel" class="px-4 py-2 rounded-xl text-xs font-bold text-slate-400 hover:text-white hover:bg-white/5 transition-all">
                            ${cancelText}
                        </button>
                        <button id="modal-btn-confirm" class="px-5 py-2 rounded-xl text-xs font-black uppercase tracking-wider transition-all shadow-lg ${isDanger ? 'bg-red-600 hover:bg-red-500 text-white shadow-red-600/30' : 'bg-sky-600 hover:bg-sky-500 text-white shadow-sky-600/30'}">
                            ${confirmText}
                        </button>
                    </div>
                </div>
            `;

            document.body.appendChild(modal);

            const cancelBtn = modal.querySelector('#modal-btn-cancel');
            const confirmBtn = modal.querySelector('#modal-btn-confirm');

            const cleanup = (result) => {
                modal.remove();
                resolve(result);
            };

            cancelBtn.addEventListener('click', () => cleanup(false));
            confirmBtn.addEventListener('click', () => cleanup(true));
            modal.addEventListener('click', (e) => {
                if (e.target === modal) cleanup(false);
            });
        });
    }

    /**
     * Shows a tactical toast notification on the top-right
     * @param {string} message 
     * @param {"success"|"error"|"warning"|"info"} type 
     * @param {number} durationMs 
     */
    static toast(message, type = "info", durationMs = 4000) {
        let container = document.getElementById('matrix-toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'matrix-toast-container';
            container.className = 'fixed top-5 right-5 z-[2000] flex flex-col gap-2 pointer-events-none max-w-sm font-sans';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = `matrix-toast pointer-events-auto p-3.5 rounded-xl border backdrop-blur-xl shadow-2xl flex items-center gap-3 text-xs font-medium text-white animate-fade-in ${
            type === 'success' ? 'bg-emerald-950/90 border-emerald-500/40 text-emerald-100' :
            type === 'error' ? 'bg-red-950/90 border-red-500/40 text-red-100' :
            type === 'warning' ? 'bg-amber-950/90 border-amber-500/40 text-amber-100' :
            'bg-slate-950/90 border-sky-500/40 text-sky-100'
        }`;

        const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : type === 'warning' ? '⚠️' : 'ℹ️';

        toast.innerHTML = `
            <span class="text-base">${icon}</span>
            <div class="flex-1">${message}</div>
            <button class="text-slate-400 hover:text-white text-xs font-mono ml-2">✕</button>
        `;

        const closeBtn = toast.querySelector('button');
        closeBtn.addEventListener('click', () => toast.remove());

        container.appendChild(toast);

        setTimeout(() => {
            if (toast.parentNode) {
                toast.classList.add('opacity-0', 'transition-opacity');
                setTimeout(() => toast.remove(), 300);
            }
        }, durationMs);
    }
}
