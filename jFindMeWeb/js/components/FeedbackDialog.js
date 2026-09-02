/**
 * jFindMe Matrix - Universal Enterprise Feedback & Modal Dialog System (Dynamic OOPS Architecture)
 * Closed-Loop interactive dialogs: Multi-Option Confirmations, Prompts with Live Validation,
 * Action Modals with Subtitles, Searchable Detail List Selectors, and Animated Floating Toast Notifications.
 * 100% Theme-Adaptive (Dark / Light) using Enterprise CSS Custom Property Tokens.
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
        confirmClass = "btn-action-primary",
        onConfirm = null,
        onCancel = null
    } = {}) {
        FeedbackDialog.closeModal();

        const modalOverlay = document.createElement("div");
        modalOverlay.id = "jfindme-feedback-modal";
        modalOverlay.className = "fixed inset-0 z-[9999] bg-black/75 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in font-sans select-none";

        modalOverlay.innerHTML = `
            <div class="card-theme border rounded-2xl max-w-md w-full p-4 shadow-2xl space-y-3 transform transition-all scale-100">
                <div class="flex items-start gap-3">
                    <div class="w-10 h-10 rounded-xl bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-xl shrink-0">
                        ${icon}
                    </div>
                    <div class="min-w-0 flex-1">
                        <h3 class="text-sm font-bold text-primary-theme tracking-wide">${title}</h3>
                        <p class="text-xs text-secondary-theme mt-1 leading-relaxed">${message}</p>
                    </div>
                </div>

                <div class="flex items-center justify-end gap-2 pt-2 border-t border-subtle-theme">
                    <button id="fb-dialog-cancel" class="btn-action-secondary text-xs">
                        ${cancelText}
                    </button>
                    <button id="fb-dialog-confirm" class="${confirmClass} text-xs">
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

        // Close on backdrop click
        modalOverlay.addEventListener("click", (e) => {
            if (e.target === modalOverlay) {
                FeedbackDialog.closeModal();
                if (onCancel) onCancel();
            }
        });
    }

    /**
     * Shows an interactive prompt dialog with input textbox and validation conditions
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
            <div class="card-theme border rounded-2xl max-w-md w-full p-4 shadow-2xl space-y-3">
                <div class="flex items-start gap-3">
                    <div class="w-10 h-10 rounded-xl bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-xl shrink-0">
                        ✏️
                    </div>
                    <div class="min-w-0 flex-1">
                        <h3 class="text-sm font-bold text-primary-theme tracking-wide">${title}</h3>
                        <p class="text-xs text-secondary-theme mt-1 leading-relaxed">${message}</p>
                    </div>
                </div>

                <div class="space-y-1">
                    <label class="text-[10px] uppercase font-mono text-muted-theme font-bold">${inputLabel}</label>
                    <input type="text" id="fb-prompt-input" value="${defaultValue}" placeholder="${placeholder}"
                           class="input-theme w-full px-3 py-2 text-xs font-mono">
                    <div id="fb-prompt-error" class="text-[10px] text-rose-500 font-mono hidden pt-0.5"></div>
                </div>

                <div class="flex items-center justify-end gap-2 pt-2 border-t border-subtle-theme">
                    <button id="fb-dialog-cancel" class="btn-action-secondary text-xs">
                        ${cancelText}
                    </button>
                    <button id="fb-dialog-submit" class="btn-action-primary text-xs">
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
     * Shows an action modal with multiple interactive option buttons & descriptions
     * @param {Object} options 
     */
    static showActionModal({
        title = "Select Operation",
        message = "Choose an action below to proceed:",
        icon = "⚡",
        actions = [],
        onSelect = null,
        onCancel = null
    } = {}) {
        FeedbackDialog.closeModal();

        const modalOverlay = document.createElement("div");
        modalOverlay.id = "jfindme-feedback-modal";
        modalOverlay.className = "fixed inset-0 z-[9999] bg-black/75 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in font-sans select-none";

        modalOverlay.innerHTML = `
            <div class="card-theme border rounded-2xl max-w-md w-full p-4 shadow-2xl space-y-3">
                <div class="flex items-start justify-between border-b border-subtle-theme pb-2.5">
                    <div class="flex items-center gap-2.5">
                        <div class="w-8 h-8 rounded-lg bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-base shrink-0">
                            ${icon}
                        </div>
                        <div>
                            <h3 class="text-sm font-bold text-primary-theme tracking-wide">${title}</h3>
                            <p class="text-xs text-secondary-theme">${message}</p>
                        </div>
                    </div>
                    <button id="fb-dialog-close" class="text-muted-theme hover:text-primary-theme text-sm font-bold p-1">✕</button>
                </div>

                <!-- Action Buttons with Option Descriptions -->
                <div class="space-y-2">
                    ${actions.map(act => {
                        const isDanger = act.variant === 'danger';
                        const isPrimary = act.variant === 'primary';
                        const borderClass = isDanger ? 'border-rose-500/30 hover:border-rose-500/60' : (isPrimary ? 'border-sky-500/40 hover:border-sky-500' : 'border-subtle-theme hover:border-sky-400');
                        return `
                            <button class="fb-action-btn w-full p-2.5 rounded-xl text-left transition-all border ${borderClass} bg-card-subtle hover:bg-card-hover flex items-center justify-between group"
                                    data-action-id="${act.id}">
                                <div class="flex items-center gap-2.5 min-w-0">
                                    <span class="text-lg shrink-0">${act.icon || '👉'}</span>
                                    <div class="min-w-0">
                                        <div class="text-xs font-bold text-primary-theme group-hover:text-sky-400 transition-colors truncate">
                                            ${act.label}
                                        </div>
                                        ${act.description ? `
                                            <div class="text-[10px] text-secondary-theme leading-tight mt-0.5 truncate">
                                                ${act.description}
                                            </div>
                                        ` : ''}
                                    </div>
                                </div>
                                ${act.badge ? `
                                    <span class="badge-status ${isDanger ? 'badge-danger' : 'badge-accent'} text-[9px] shrink-0 ml-2">
                                        ${act.badge}
                                    </span>
                                ` : ''}
                            </button>
                        `;
                    }).join('')}
                </div>

                <div class="flex justify-end pt-1 border-t border-subtle-theme">
                    <button id="fb-action-cancel" class="btn-action-secondary text-xs">
                        Cancel
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modalOverlay);

        document.querySelectorAll(".fb-action-btn").forEach(el => {
            el.addEventListener("click", () => {
                const actionId = el.getAttribute("data-action-id");
                FeedbackDialog.closeModal();
                if (onSelect) onSelect(actionId);
            });
        });

        document.getElementById("fb-dialog-close")?.addEventListener("click", () => {
            FeedbackDialog.closeModal();
            if (onCancel) onCancel();
        });

        document.getElementById("fb-action-cancel")?.addEventListener("click", () => {
            FeedbackDialog.closeModal();
            if (onCancel) onCancel();
        });
    }

    /**
     * Shows a searchable list modal with rich details, icons, and multi/single selection
     * @param {Object} options 
     */
    static showDetailListModal({
        title = "Select Option",
        message = "Choose an item from the list below:",
        items = [],
        multiSelect = false,
        confirmText = "Confirm Selection",
        onConfirm = null,
        onCancel = null
    } = {}) {
        FeedbackDialog.closeModal();

        const modalOverlay = document.createElement("div");
        modalOverlay.id = "jfindme-feedback-modal";
        modalOverlay.className = "fixed inset-0 z-[9999] bg-black/75 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in font-sans select-none";

        let selectedIds = new Set();

        modalOverlay.innerHTML = `
            <div class="card-theme border rounded-2xl max-w-lg w-full p-4 shadow-2xl space-y-3">
                <div class="flex items-start justify-between border-b border-subtle-theme pb-2.5">
                    <div>
                        <h3 class="text-sm font-bold text-primary-theme tracking-wide">${title}</h3>
                        <p class="text-xs text-secondary-theme mt-0.5">${message}</p>
                    </div>
                    <button id="fb-dialog-close" class="text-muted-theme hover:text-primary-theme text-sm font-bold p-1">✕</button>
                </div>

                <!-- Search Filter -->
                <div class="relative flex items-center">
                    <input type="text" id="fb-list-search" placeholder="🔍 Search list items..." 
                           class="input-theme w-full pl-7 pr-3 py-1.5 text-xs font-mono">
                    <svg class="w-3.5 h-3.5 absolute left-2 text-secondary-theme pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                    </svg>
                </div>

                <!-- List Box with Details -->
                <div id="fb-list-container" class="space-y-1.5 max-h-[320px] overflow-y-auto custom-scrollbar font-mono text-xs pr-0.5">
                    <!-- Rendered dynamically -->
                </div>

                <div class="flex items-center justify-between pt-2 border-t border-subtle-theme">
                    <span id="fb-selection-counter" class="text-[10px] text-muted-theme font-mono">
                        ${multiSelect ? '0 selected' : 'Click item to select'}
                    </span>
                    <div class="flex items-center gap-2">
                        <button id="fb-list-cancel" class="btn-action-secondary text-xs">
                            Cancel
                        </button>
                        ${multiSelect ? `
                            <button id="fb-list-confirm" class="btn-action-primary text-xs">
                                ${confirmText}
                            </button>
                        ` : ''}
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modalOverlay);

        const listContainer = document.getElementById("fb-list-container");
        const searchInput = document.getElementById("fb-list-search");
        const counterEl = document.getElementById("fb-selection-counter");

        const renderItems = (filterText = "") => {
            const q = filterText.toLowerCase();
            const filtered = items.filter(it => 
                (it.title && it.title.toLowerCase().includes(q)) ||
                (it.subtitle && it.subtitle.toLowerCase().includes(q)) ||
                (it.id && it.id.toLowerCase().includes(q))
            );

            if (filtered.length === 0) {
                listContainer.innerHTML = `<div class="text-[10px] text-muted-theme italic py-4 text-center">No matching items found.</div>`;
                return;
            }

            listContainer.innerHTML = filtered.map(item => {
                const isSelected = selectedIds.has(item.id);
                return `
                    <div class="fb-detail-item p-2 rounded-xl border ${isSelected ? 'border-sky-500 bg-sky-500/10' : 'border-subtle-theme bg-card-subtle hover:bg-card-hover'} cursor-pointer transition-all flex items-center justify-between select-none"
                         data-item-id="${item.id}">
                        <div class="flex items-center gap-2.5 min-w-0">
                            ${multiSelect ? `
                                <input type="checkbox" class="fb-chk rounded accent-sky-500 pointer-events-none" ${isSelected ? 'checked' : ''}>
                            ` : ''}
                            ${item.avatarUrl ? `
                                <img src="${item.avatarUrl}" class="w-7 h-7 rounded-full object-cover border border-subtle-theme">
                            ` : `
                                <span class="text-base">${item.icon || '📱'}</span>
                            `}
                            <div class="min-w-0">
                                <div class="text-xs font-bold text-primary-theme truncate">${item.title}</div>
                                <div class="text-[10px] text-secondary-theme truncate">${item.subtitle || ''}</div>
                            </div>
                        </div>
                        ${item.tag ? `
                            <span class="badge-status badge-accent text-[8px] shrink-0 ml-2">${item.tag}</span>
                        ` : ''}
                    </div>
                `;
            }).join('');

            // Bind click
            listContainer.querySelectorAll(".fb-detail-item").forEach(el => {
                el.addEventListener("click", () => {
                    const id = el.getAttribute("data-item-id");
                    if (multiSelect) {
                        if (selectedIds.has(id)) selectedIds.delete(id);
                        else selectedIds.add(id);
                        if (counterEl) counterEl.textContent = `${selectedIds.size} selected`;
                        renderItems(searchInput?.value || "");
                    } else {
                        FeedbackDialog.closeModal();
                        if (onConfirm) onConfirm([id]);
                    }
                });
            });
        };

        renderItems();

        searchInput?.addEventListener("input", (e) => {
            renderItems(e.target.value);
        });

        document.getElementById("fb-list-confirm")?.addEventListener("click", () => {
            FeedbackDialog.closeModal();
            if (onConfirm) onConfirm(Array.from(selectedIds));
        });

        document.getElementById("fb-dialog-close")?.addEventListener("click", () => {
            FeedbackDialog.closeModal();
            if (onCancel) onCancel();
        });

        document.getElementById("fb-list-cancel")?.addEventListener("click", () => {
            FeedbackDialog.closeModal();
            if (onCancel) onCancel();
        });
    }

    /**
     * Shows a lightweight animated floating Toast notification
     * @param {string} message 
     * @param {'success'|'warning'|'error'|'info'} type 
     * @param {number} duration 
     */
    static showToast(message, type = "success", duration = 3500) {
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
        toast.className = `flex items-center gap-2.5 px-3.5 py-2.5 rounded-xl border backdrop-blur-md shadow-xl text-xs font-medium transition-all duration-300 transform translate-y-2 opacity-0 pointer-events-auto ${bgStyles[type] || bgStyles.info}`;
        toast.innerHTML = `
            <span class="text-sm shrink-0">${icons[type] || 'ℹ️'}</span>
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
