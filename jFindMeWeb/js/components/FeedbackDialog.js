/**
 * jFindMe Matrix - Universal Enterprise Feedback & Modal Dialog System (OOPS Architecture)
 * Closed-Loop interactive dialogs: Confirmations, Prompts, List Selectors, and Toast Notifications
 */
export class FeedbackDialog {
    /**
     * Shows an interactive confirmation dialog with options
     * @param {Object} options 
     */
    static showConfirm({
        title = "Confirmation Required",
        message = "Are you sure you want to proceed with this operation?",
        icon = "⚠️",
        confirmText = "Confirm",
        cancelText = "Cancel",
        confirmClass = "bg-sky-600 hover:bg-sky-500 text-white",
        onConfirm = null,
        onCancel = null
    } = {}) {
        FeedbackDialog.closeModal();

        const modalOverlay = document.createElement("div");
        modalOverlay.id = "jfindme-feedback-modal";
        modalOverlay.className = "fixed inset-0 z-[9999] bg-black/75 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in font-sans select-none";

        modalOverlay.innerHTML = `
            <div class="bg-slate-900 border border-white/15 rounded-2xl max-w-md w-full p-4 shadow-2xl space-y-3 transform transition-all scale-100 text-slate-200">
                <div class="flex items-start gap-3">
                    <div class="w-10 h-10 rounded-xl bg-sky-500/20 border border-sky-500/30 flex items-center justify-center text-xl shrink-0">
                        ${icon}
                    </div>
                    <div class="min-w-0 flex-1">
                        <h3 class="text-sm font-bold text-white tracking-wide">${title}</h3>
                        <p class="text-xs text-slate-300 mt-1 leading-relaxed">${message}</p>
                    </div>
                </div>

                <div class="flex items-center justify-end gap-2 pt-2 border-t border-white/10">
                    <button id="fb-dialog-cancel" class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium transition-all">
                        ${cancelText}
                    </button>
                    <button id="fb-dialog-confirm" class="px-4 py-1.5 rounded-xl ${confirmClass} text-xs font-bold transition-all shadow-md">
                        ${confirmText}
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modalOverlay);

        document.getElementById("fb-dialog-cancel")?.addEventListener("click", () => {
            FeedbackDialog.closeModal();
            if (onCancel) onCancel();
        });

        document.getElementById("fb-dialog-confirm")?.addEventListener("click", () => {
            FeedbackDialog.closeModal();
            if (onConfirm) onConfirm();
        });
    }

    /**
     * Shows an interactive prompt dialog with input textbox and validation
     * @param {Object} options 
     */
    static showPrompt({
        title = "Input Required",
        message = "Please enter the required information:",
        inputLabel = "Value",
        defaultValue = "",
        placeholder = "Type here...",
        confirmText = "Submit",
        cancelText = "Cancel",
        validate = null,
        onConfirm = null,
        onCancel = null
    } = {}) {
        FeedbackDialog.closeModal();

        const modalOverlay = document.createElement("div");
        modalOverlay.id = "jfindme-feedback-modal";
        modalOverlay.className = "fixed inset-0 z-[9999] bg-black/75 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in font-sans select-none";

        modalOverlay.innerHTML = `
            <div class="bg-slate-900 border border-white/15 rounded-2xl max-w-md w-full p-4 shadow-2xl space-y-3 text-slate-200">
                <div class="flex items-start gap-3">
                    <div class="w-10 h-10 rounded-xl bg-sky-500/20 border border-sky-500/30 flex items-center justify-center text-xl shrink-0">
                        ✏️
                    </div>
                    <div class="min-w-0 flex-1">
                        <h3 class="text-sm font-bold text-white tracking-wide">${title}</h3>
                        <p class="text-xs text-slate-300 mt-1 leading-relaxed">${message}</p>
                    </div>
                </div>

                <div class="space-y-1">
                    <label class="text-[10px] uppercase font-mono text-slate-400 font-bold">${inputLabel}</label>
                    <input type="text" id="fb-prompt-input" value="${defaultValue}" placeholder="${placeholder}"
                           class="w-full bg-slate-950 border border-white/15 rounded-xl px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-sky-500 font-mono">
                    <div id="fb-prompt-error" class="text-[10px] text-rose-400 font-mono hidden pt-0.5"></div>
                </div>

                <div class="flex items-center justify-end gap-2 pt-2 border-t border-white/10">
                    <button id="fb-dialog-cancel" class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium transition-all">
                        ${cancelText}
                    </button>
                    <button id="fb-dialog-submit" class="px-4 py-1.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold transition-all shadow-md">
                        ${confirmText}
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modalOverlay);

        const inputEl = document.getElementById("fb-prompt-input");
        const errorEl = document.getElementById("fb-prompt-error");
        inputEl?.focus();

        const handleSubmit = () => {
            const val = inputEl ? inputEl.value.trim() : "";
            if (validate) {
                const validationRes = validate(val);
                if (validationRes !== true) {
                    if (errorEl) {
                        errorEl.textContent = typeof validationRes === "string" ? validationRes : "Invalid input value.";
                        errorEl.classList.remove("hidden");
                    }
                    return;
                }
            }
            FeedbackDialog.closeModal();
            if (onConfirm) onConfirm(val);
        };

        document.getElementById("fb-dialog-submit")?.addEventListener("click", handleSubmit);
        inputEl?.addEventListener("keydown", (e) => {
            if (e.key === "Enter") handleSubmit();
            if (e.key === "Escape") {
                FeedbackDialog.closeModal();
                if (onCancel) onCancel();
            }
        });

        document.getElementById("fb-dialog-cancel")?.addEventListener("click", () => {
            FeedbackDialog.closeModal();
            if (onCancel) onCancel();
        });
    }

    /**
     * Shows an interactive list selection modal with rich details
     * @param {Object} options 
     */
    static showListSelection({
        title = "Select Option",
        message = "Choose an item from the list below:",
        items = [],
        onSelect = null,
        onCancel = null
    } = {}) {
        FeedbackDialog.closeModal();

        const modalOverlay = document.createElement("div");
        modalOverlay.id = "jfindme-feedback-modal";
        modalOverlay.className = "fixed inset-0 z-[9999] bg-black/75 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in font-sans select-none";

        modalOverlay.innerHTML = `
            <div class="bg-slate-900 border border-white/15 rounded-2xl max-w-md w-full p-4 shadow-2xl space-y-3 text-slate-200">
                <div class="flex items-start justify-between border-b border-white/10 pb-2">
                    <div>
                        <h3 class="text-sm font-bold text-white tracking-wide">${title}</h3>
                        <p class="text-xs text-slate-400 mt-0.5">${message}</p>
                    </div>
                    <button id="fb-dialog-close" class="text-slate-400 hover:text-white text-sm font-bold p-1">✕</button>
                </div>

                <!-- List Box with Details -->
                <div class="space-y-1.5 max-h-[300px] overflow-y-auto custom-scrollbar font-mono text-xs">
                    ${items.map(item => `
                        <div class="fb-list-item p-2 rounded-xl bg-black/40 hover:bg-sky-950/60 border border-white/5 hover:border-sky-500/40 cursor-pointer transition-all flex items-center justify-between"
                             data-item-id="${item.id}">
                            <div class="flex items-center gap-2 min-w-0">
                                <span class="text-base">${item.icon || '📱'}</span>
                                <div class="min-w-0">
                                    <div class="text-xs font-bold text-white truncate">${item.title}</div>
                                    <div class="text-[9px] text-slate-400 truncate">${item.subtitle || ''}</div>
                                </div>
                            </div>
                            ${item.tag ? `
                                <span class="text-[8px] font-mono px-1.5 py-0.5 rounded bg-sky-500/20 text-sky-300 border border-sky-500/30">${item.tag}</span>
                            ` : ''}
                        </div>
                    `).join('')}
                </div>
            </div>
        `;

        document.body.appendChild(modalOverlay);

        document.querySelectorAll(".fb-list-item").forEach(el => {
            el.addEventListener("click", () => {
                const itemId = el.getAttribute("data-item-id");
                FeedbackDialog.closeModal();
                if (onSelect) onSelect(itemId);
            });
        });

        document.getElementById("fb-dialog-close")?.addEventListener("click", () => {
            FeedbackDialog.closeModal();
            if (onCancel) onCancel();
        });
    }

    /**
     * Shows a lightweight animated Toast notification
     * @param {string} message 
     * @param {'success'|'warning'|'error'|'info'} type 
     * @param {number} duration 
     */
    static showToast(message, type = "success", duration = 3000) {
        let toastContainer = document.getElementById("jfindme-toast-container");
        if (!toastContainer) {
            toastContainer = document.createElement("div");
            toastContainer.id = "jfindme-toast-container";
            toastContainer.className = "fixed bottom-5 right-5 z-[10000] space-y-2 pointer-events-none font-sans";
            document.body.appendChild(toastContainer);
        }

        const icons = {
            success: "✅",
            warning: "⚠️",
            error: "❌",
            info: "ℹ️"
        };

        const bgStyles = {
            success: "bg-emerald-950/95 border-emerald-500/40 text-emerald-200 shadow-emerald-500/20",
            warning: "bg-amber-950/95 border-amber-500/40 text-amber-200 shadow-amber-500/20",
            error: "bg-rose-950/95 border-rose-500/40 text-rose-200 shadow-rose-500/20",
            info: "bg-sky-950/95 border-sky-500/40 text-sky-200 shadow-sky-500/20"
        };

        const toast = document.createElement("div");
        toast.className = `flex items-center gap-2.5 px-3.5 py-2 rounded-xl border backdrop-blur-md shadow-lg text-xs font-medium transition-all duration-300 transform translate-y-2 opacity-0 pointer-events-auto ${bgStyles[type] || bgStyles.info}`;
        toast.innerHTML = `
            <span>${icons[type] || 'ℹ️'}</span>
            <span class="leading-snug">${message}</span>
        `;

        toastContainer.appendChild(toast);

        // Animate in
        requestAnimationFrame(() => {
            toast.classList.remove("translate-y-2", "opacity-0");
        });

        // Dismiss after duration
        setTimeout(() => {
            toast.classList.add("translate-y-2", "opacity-0");
            setTimeout(() => {
                toast.remove();
            }, 300);
        }, duration);
    }

    static closeModal() {
        const modal = document.getElementById("jfindme-feedback-modal");
        if (modal) modal.remove();
    }
}
