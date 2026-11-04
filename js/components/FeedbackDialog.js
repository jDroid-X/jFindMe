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

    /**
     * Shows an Enterprise Slide-in/out Notification from the Right Bottom with Interactive Action Options
     * @param {Object} options
     * @param {string} options.title
     * @param {string} options.message
     * @param {'success'|'warning'|'error'|'info'} [options.type='info']
     * @param {Array<{id: string, label: string, variant?: 'primary'|'secondary'|'danger'}>} [options.actions=[]]
     * @param {number} [options.duration=6500] 0 to keep open until closed
     * @param {Function} [options.onAction]
     * @param {Function} [options.onClose]
     */
    static showSlideNotification({
        title = "System Notification",
        message = "",
        type = "info",
        actions = [],
        duration = 6500,
        onAction = null,
        onClose = null
    } = {}) {
        let slideContainer = document.getElementById("jfindme-slide-notif-container");
        if (!slideContainer) {
            slideContainer = document.createElement("div");
            slideContainer.id = "jfindme-slide-notif-container";
            slideContainer.className = "fixed bottom-5 right-5 z-[10001] flex flex-col gap-2.5 max-w-sm w-[90vw] pointer-events-none font-sans";
            document.body.appendChild(slideContainer);
        }

        const icons = {
            success: "🛡️",
            warning: "⚠️",
            error: "🚨",
            info: "📡"
        };

        const themeStyles = {
            success: "border-emerald-500/40 bg-emerald-950/90 text-emerald-100 shadow-emerald-500/20",
            warning: "border-amber-500/40 bg-amber-950/90 text-amber-100 shadow-amber-500/20",
            error: "border-rose-500/40 bg-rose-950/90 text-rose-100 shadow-rose-500/20",
            info: "border-sky-500/40 bg-sky-950/90 text-sky-100 shadow-sky-500/20"
        };

        const notifCard = document.createElement("div");
        notifCard.className = `p-3.5 rounded-2xl border backdrop-blur-xl shadow-2xl transition-all duration-400 ease-out transform translate-x-full opacity-0 pointer-events-auto select-none space-y-2.5 ${themeStyles[type] || themeStyles.info}`;

        notifCard.innerHTML = `
            <div class="flex items-start justify-between gap-2">
                <div class="flex items-center gap-2 min-w-0">
                    <span class="text-base shrink-0">${icons[type] || '📡'}</span>
                    <h4 class="text-xs font-bold tracking-wide truncate uppercase">${title}</h4>
                </div>
                <button class="btn-slide-close text-xs text-muted-theme hover:text-white font-bold p-0.5 transition-colors">✕</button>
            </div>
            <p class="text-[11px] leading-relaxed opacity-90">${message}</p>
            ${actions.length > 0 ? `
                <div class="flex items-center justify-end gap-1.5 pt-1.5 border-t border-white/10">
                    ${actions.map(act => {
                        const isPrimary = act.variant === 'primary';
                        const isDanger = act.variant === 'danger';
                        const btnClass = isPrimary 
                            ? 'btn-action-primary text-[10px] py-1 px-2.5 shadow-sm' 
                            : (isDanger ? 'btn-action-danger text-[10px] py-1 px-2.5' : 'btn-action-secondary text-[10px] py-1 px-2');
                        return `<button class="btn-slide-action ${btnClass}" data-action-id="${act.id}">${act.label}</button>`;
                    }).join('')}
                </div>
            ` : ''}
        `;

        slideContainer.appendChild(notifCard);

        const dismiss = () => {
            notifCard.classList.remove("translate-x-0", "opacity-100");
            notifCard.classList.add("translate-x-full", "opacity-0");
            setTimeout(() => {
                notifCard.remove();
                if (onClose) onClose();
            }, 400);
        };

        // Slide In Animation
        requestAnimationFrame(() => {
            notifCard.classList.remove("translate-x-full", "opacity-0");
            notifCard.classList.add("translate-x-0", "opacity-100");
        });

        notifCard.querySelector(".btn-slide-close")?.addEventListener("click", dismiss);

        notifCard.querySelectorAll(".btn-slide-action").forEach(btn => {
            btn.addEventListener("click", () => {
                const actId = btn.getAttribute("data-action-id");
                dismiss();
                if (onAction) onAction(actId);
            });
        });

        if (duration > 0) {
            setTimeout(dismiss, duration);
        }
    }

    /**
     * Renders an interactive custom rich dropdown with avatars, subtitles, search, and badges
     * @param {Object} options
     */
    static createRichDropdown({
        container,
        items = [],
        selectedId = null,
        placeholder = "Select Option...",
        onSelect = null
    }) {
        if (!container) return;
        container.classList.add("relative", "select-none");
        
        let currentItem = items.find(i => i.id === selectedId) || null;
        
        container.innerHTML = `
            <div class="rich-dropdown-trigger input-theme p-2 text-xs flex items-center justify-between cursor-pointer">
                <div class="flex items-center gap-2 min-w-0">
                    ${currentItem ? `
                        ${currentItem.avatarUrl ? `<img src="${currentItem.avatarUrl}" class="w-5 h-5 rounded-full object-cover">` : `<span class="text-sm">${currentItem.icon || '📱'}</span>`}
                        <span class="font-bold text-primary-theme truncate">${currentItem.title}</span>
                        ${currentItem.tag ? `<span class="badge-status badge-accent text-[8px]">${currentItem.tag}</span>` : ''}
                    ` : `<span class="text-muted-theme">${placeholder}</span>`}
                </div>
                <span class="text-muted-theme text-xs ml-1">▼</span>
            </div>
            <div class="rich-dropdown-menu hidden absolute top-full left-0 right-0 z-50 mt-1 card-theme p-2 shadow-2xl border rounded-xl space-y-1.5 max-h-56 overflow-y-auto custom-scrollbar">
                <input type="text" class="rich-dropdown-search input-theme w-full px-2 py-1 text-xs font-mono" placeholder="🔍 Filter list...">
                <div class="rich-dropdown-items space-y-1"></div>
            </div>
        `;

        const trigger = container.querySelector(".rich-dropdown-trigger");
        const menu = container.querySelector(".rich-dropdown-menu");
        const search = container.querySelector(".rich-dropdown-search");
        const listWrapper = container.querySelector(".rich-dropdown-items");

        const renderItems = (filter = "") => {
            const q = filter.toLowerCase();
            const filtered = items.filter(it => !q || it.title.toLowerCase().includes(q) || (it.subtitle && it.subtitle.toLowerCase().includes(q)));
            if (filtered.length === 0) {
                listWrapper.innerHTML = `<div class="text-[10px] text-muted-theme italic p-2 text-center">No options found.</div>`;
                return;
            }
            listWrapper.innerHTML = filtered.map(it => `
                <div class="rich-dropdown-item p-1.5 rounded-lg hover:bg-card-hover cursor-pointer flex items-center justify-between transition-all ${it.id === selectedId ? 'bg-sky-500/15 border border-sky-500/30' : ''}" data-id="${it.id}">
                    <div class="flex items-center gap-2 min-w-0">
                        ${it.avatarUrl ? `<img src="${it.avatarUrl}" class="w-5 h-5 rounded-full object-cover">` : `<span class="text-sm">${it.icon || '📱'}</span>`}
                        <div class="min-w-0">
                            <div class="text-xs font-bold text-primary-theme truncate">${it.title}</div>
                            ${it.subtitle ? `<div class="text-[9px] text-secondary-theme font-mono truncate">${it.subtitle}</div>` : ''}
                        </div>
                    </div>
                    ${it.tag ? `<span class="badge-status badge-accent text-[8px]">${it.tag}</span>` : ''}
                </div>
            `).join('');

            listWrapper.querySelectorAll(".rich-dropdown-item").forEach(el => {
                el.addEventListener("click", () => {
                    const id = el.getAttribute("data-id");
                    selectedId = id;
                    currentItem = items.find(i => i.id === id);
                    menu.classList.add("hidden");
                    FeedbackDialog.createRichDropdown({ container, items, selectedId, placeholder, onSelect });
                    if (onSelect) onSelect(id, currentItem);
                });
            });
        };

        trigger.addEventListener("click", (e) => {
            e.stopPropagation();
            menu.classList.toggle("hidden");
            if (!menu.classList.contains("hidden")) {
                renderItems();
                search.focus();
            }
        });

        search.addEventListener("input", (e) => renderItems(e.target.value));

        document.addEventListener("click", (e) => {
            if (!container.contains(e.target)) menu.classList.add("hidden");
        });
    }

    static closeModal() {
        const modal = document.getElementById("jfindme-feedback-modal");
        if (modal) modal.remove();
    }

    /**
     * Shows an animated slide-in notification from bottom-right with support for actions
     * @param {Object} options
     */
    static showSlideNotification({
        title = "Notification",
        message = "",
        type = "info", // info, success, warning, error
        icon = "ℹ️",
        actions = [],
        onAction = null,
        duration = 5000
    } = {}) {
        // Remove existing notifications to prevent clutter (max 5)
        const stack = document.querySelector('.slide-notification-container');
        if (stack && stack.children.length >= 5) {
            const first = stack.firstChild;
            first.classList.remove('slide-in');
            first.classList.add('slide-out');
            setTimeout(() => first.remove(), 300);
        }

        const colors = {
            info: 'border-sky-500',
            success: 'border-emerald-500',
            warning: 'border-amber-500',
            error: 'border-rose-500'
        };

        const notification = document.createElement('div');
        notification.className = `slide-notification-item border ${colors[type] || colors.info}`;
        notification.innerHTML = `
            <div class="p-3">
                <div class="flex items-start gap-2">
                    <span class="text-base shrink-0">${icon}</span>
                    <div class="min-w-0 flex-1">
                        <h4 class="text-xs font-bold truncate">${title}</h4>
                        ${message ? `<p class="text-[10px] text-secondary-theme mt-0.5 line-clamp-2">${message}</p>` : ''}
                    </div>
                    <button class="close-btn text-muted-theme hover:text-primary-theme text-xs font-bold shrink-0 ml-1">×</button>
                </div>
                ${actions.length > 0 ? `
                    <div class="flex gap-2 mt-2 pt-2 border-t border-subtle-theme">
                        ${actions.map(act => `
                            <button class="btn-action-${act.variant === 'primary' ? 'primary' : 'secondary'} text-[10px] px-2 py-1" data-action="${act.id}">${act.label}</button>
                        `).join('')}
                    </div>
                ` : ''}
            </div>
        `;

        // Action handlers
        notification.querySelectorAll('[data-action]').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const actionId = btn.getAttribute('data-action');
                notification.classList.remove('slide-in');
                notification.classList.add('slide-out');
                setTimeout(() => notification.remove(), 300);
                if (onAction) onAction(actionId);
            });
        });

        // Close handler
        notification.querySelector('.close-btn').addEventListener('click', () => {
            notification.classList.remove('slide-in');
            notification.classList.add('slide-out');
            setTimeout(() => notification.remove(), 300);
        });

        // Append to stack
        if (!stack) {
            const container = document.createElement('div');
            container.className = 'slide-notification-container';
            document.body.appendChild(container);
            container.appendChild(notification);
        } else {
            stack.appendChild(notification);
        }

        // Animate in
        requestAnimationFrame(() => {
            notification.classList.add('slide-in');
        });

        // Auto-remove after duration
        const timer = setTimeout(() => {
            if (notification.parentNode) {
                notification.classList.remove('slide-in');
                notification.classList.add('slide-out');
                setTimeout(() => notification.remove(), 300);
            }
        }, duration);

        // Pause auto-close on hover
        notification.addEventListener('mouseenter', () => clearTimeout(timer));
        notification.addEventListener('mouseleave', () => {
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.classList.remove('slide-in');
                    notification.classList.add('slide-out');
                    setTimeout(() => notification.remove(), 300);
                }
            }, duration);
        });
    }
}
