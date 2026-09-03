/**
 * UIControls.js - Universal Enterprise Interactive Component Suite
 * Dynamic OOPS-based MVC / MVVM UI Architecture
 * Features:
 *  - Switch (On/Off Animated Toggle)
 *  - Segmented Toggle (Left/Right / Multi-State)
 *  - 3D Card Flipper (Macro Executive Front ↔ Micro Sensor "The Ant" Back)
 *  - ListBox with List Details & Expandable Accordion Dropdown
 *  - Multi-Option Modal Dialog Box (Prompt, Confirm, Action List)
 *  - Slide In/Out Toast Notification Stack (Bottom-Right)
 *  - Form Validation Engine (Inline Tooltips & Dynamic Error Flags)
 */

export class UIControls {
    /**
     * Creates an animated On/Off Switch component HTML & binds event listener
     * @param {Object} options { id, label, checked, onChange, activeText, inactiveText, className }
     * @returns {string} HTML string
     */
    static createSwitch(options = {}) {
        const {
            id = `switch-${Math.random().toString(36).substr(2, 9)}`,
            label = '',
            checked = false,
            activeText = 'ON',
            inactiveText = 'OFF',
            className = ''
        } = options;

        return `
            <label class="ui-switch-wrapper inline-flex items-center gap-2.5 cursor-pointer select-none ${className}" for="${id}">
                ${label ? `<span class="text-xs font-semibold text-slate-300">${label}</span>` : ''}
                <div class="relative">
                    <input type="checkbox" id="${id}" class="sr-only ui-switch-input" ${checked ? 'checked' : ''} data-switch-id="${id}">
                    <div class="ui-switch-bg w-10 h-5 bg-slate-800 rounded-full transition-colors border border-slate-700 shadow-inner"></div>
                    <div class="ui-switch-dot absolute left-0.5 top-0.5 bg-white w-4 h-4 rounded-full transition-transform shadow-md flex items-center justify-center text-[8px] font-bold text-slate-900">
                    </div>
                </div>
                <span class="ui-switch-state text-[10px] font-mono text-slate-400" id="${id}-state-text">${checked ? activeText : inactiveText}</span>
            </label>
        `;
    }

    /**
     * Binds all switch elements within a root container
     * @param {HTMLElement} rootEl 
     * @param {Function} callback 
     */
    static bindSwitches(rootEl, callback) {
        if (!rootEl) return;
        rootEl.querySelectorAll('.ui-switch-input').forEach(input => {
            input.onchange = (e) => {
                const checked = e.target.checked;
                const id = e.target.id;
                const stateText = rootEl.querySelector(`#${id}-state-text`);
                if (stateText) {
                    stateText.textContent = checked ? 'ON' : 'OFF';
                }
                if (callback) callback(id, checked, e);
            };
        });
    }

    /**
     * Creates a Segmented Toggle Button Group (Left/Right or Multi-Option)
     * @param {Object} options { id, options: [{ value, label, icon }], selectedValue, className }
     * @returns {string} HTML string
     */
    static createSegmentedToggle(options = {}) {
        const {
            id = `toggle-${Math.random().toString(36).substr(2, 9)}`,
            options: items = [],
            selectedValue = '',
            className = ''
        } = options;

        return `
            <div class="ui-segmented-toggle inline-flex p-1 bg-slate-900/90 rounded-xl border border-slate-800 shadow-inner ${className}" id="${id}" data-toggle-group="${id}">
                ${items.map(item => {
                    const isSelected = item.value === selectedValue;
                    return `
                        <button type="button" 
                            class="ui-toggle-btn px-3 py-1.5 rounded-lg text-xs font-mono font-semibold transition-all flex items-center gap-1.5 ${isSelected ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30 active-toggle' : 'text-slate-400 hover:text-slate-200'}"
                            data-value="${item.value}"
                            data-group="${id}">
                            ${item.icon ? `<span>${item.icon}</span>` : ''}
                            <span>${item.label}</span>
                        </button>
                    `;
                }).join('')}
            </div>
        `;
    }

    /**
     * Binds Segmented Toggles within container
     */
    static bindSegmentedToggles(rootEl, callback) {
        if (!rootEl) return;
        rootEl.querySelectorAll('.ui-segmented-toggle').forEach(group => {
            group.querySelectorAll('.ui-toggle-btn').forEach(btn => {
                btn.onclick = (e) => {
                    const val = btn.dataset.value;
                    const groupId = group.dataset.toggleGroup;
                    group.querySelectorAll('.ui-toggle-btn').forEach(b => {
                        b.classList.remove('bg-indigo-600', 'text-white', 'shadow-md', 'shadow-indigo-600/30', 'active-toggle');
                        b.classList.add('text-slate-400');
                    });
                    btn.classList.add('bg-indigo-600', 'text-white', 'shadow-md', 'shadow-indigo-600/30', 'active-toggle');
                    btn.classList.remove('text-slate-400');
                    if (callback) callback(groupId, val, e);
                };
            });
        });
    }

    /**
     * Creates a 3D Flipper Card component
     * Front: Macro Executive Info | Back: "The Ant" Micro Sensor Telemetry
     */
    static createFlipperCard(cardId, frontHtml, backHtml, options = {}) {
        return `
            <div class="ui-flipper-perspective w-full" id="${cardId}-wrapper">
                <div class="ui-flipper-inner relative w-full transition-transform duration-500 transform-style-3d" id="${cardId}">
                    <!-- Front Card: Macro View -->
                    <div class="ui-flipper-front w-full backface-hidden">
                        ${frontHtml}
                        <button type="button" class="ui-flip-trigger absolute top-3 right-3 px-2 py-1 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-[10px] font-mono text-amber-400 border border-slate-700/80 flex items-center gap-1 shadow z-10"
                            data-flip-target="${cardId}" title="Flip to Micro Sensor Telemetry ('The Ant')">
                            <span>🐜</span>
                            <span>Sensor Details ⮌</span>
                        </button>
                    </div>

                    <!-- Back Card: Micro Sensor View -->
                    <div class="ui-flipper-back w-full backface-hidden rotate-y-180 absolute top-0 left-0">
                        ${backHtml}
                        <button type="button" class="ui-flip-trigger absolute top-3 right-3 px-2 py-1 rounded-lg bg-indigo-900/80 hover:bg-indigo-800 text-[10px] font-mono text-indigo-200 border border-indigo-700/80 flex items-center gap-1 shadow z-10"
                            data-flip-target="${cardId}" title="Flip to Macro Dossier ('The Eagle')">
                            <span>🦅</span>
                            <span>Macro Overview ⮌</span>
                        </button>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Binds all flip triggers
     */
    static bindFlippers(rootEl) {
        if (!rootEl) return;
        rootEl.querySelectorAll('.ui-flip-trigger').forEach(btn => {
            btn.onclick = (e) => {
                e.stopPropagation();
                const targetId = btn.dataset.flipTarget;
                const card = rootEl.querySelector(`#${targetId}`);
                if (card) {
                    card.classList.toggle('flipped');
                }
            };
        });
    }

    /**
     * Creates a Searchable Expandable ListBox with nested metadata dropdown
     */
    static createListBox(options = {}) {
        const {
            id = `listbox-${Math.random().toString(36).substr(2, 9)}`,
            label = 'Select Item',
            placeholder = 'Search items...',
            items = [], // [{ id, title, subtitle, badge, icon, details: [] }]
            selectedValue = null,
            className = ''
        } = options;

        const selectedItem = items.find(i => i.id === selectedValue) || items[0];

        return `
            <div class="ui-listbox-container relative w-full ${className}" id="${id}" data-listbox-id="${id}">
                ${label ? `<label class="block text-xs font-semibold text-slate-300 mb-1.5">${label}</label>` : ''}
                <div class="ui-listbox-trigger w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white font-mono flex items-center justify-between cursor-pointer hover:border-slate-500 shadow-sm">
                    <div class="flex items-center gap-2 truncate">
                        <span>${selectedItem?.icon || '📁'}</span>
                        <span class="font-bold text-white truncate" id="${id}-selected-title">${selectedItem?.title || 'Choose Option'}</span>
                        ${selectedItem?.badge ? `<span class="px-1.5 py-0.2 rounded text-[9px] bg-slate-800 text-slate-400 font-mono">${selectedItem.badge}</span>` : ''}
                    </div>
                    <span class="text-slate-400 text-xs">▼</span>
                </div>

                <!-- Dropdown Menu -->
                <div class="ui-listbox-dropdown hidden absolute top-full left-0 right-0 mt-1 bg-slate-900 border border-slate-700 rounded-xl shadow-2xl z-50 overflow-hidden backdrop-blur-xl">
                    <div class="p-2 border-b border-slate-800">
                        <input type="text" class="ui-listbox-search w-full px-2.5 py-1.5 bg-slate-950 border border-slate-800 rounded-lg text-xs text-white font-mono placeholder-slate-500 focus:outline-none focus:border-indigo-500"
                            placeholder="${placeholder}">
                    </div>
                    <div class="ui-listbox-items max-h-60 overflow-y-auto p-1 space-y-1 custom-scrollbar">
                        ${items.map(item => `
                            <div class="ui-listbox-item px-3 py-2 rounded-lg hover:bg-slate-800/80 cursor-pointer flex flex-col gap-1 transition ${item.id === selectedValue ? 'bg-indigo-600/20 border border-indigo-500/30' : ''}"
                                data-id="${item.id}" data-title="${item.title}">
                                <div class="flex items-center justify-between">
                                    <div class="flex items-center gap-2">
                                        <span class="text-sm">${item.icon || '📍'}</span>
                                        <span class="text-xs font-bold font-mono text-white">${item.title}</span>
                                    </div>
                                    ${item.badge ? `<span class="px-1.5 py-0.5 rounded text-[9px] font-mono bg-slate-800 text-slate-300">${item.badge}</span>` : ''}
                                </div>
                                ${item.subtitle ? `<div class="text-[10px] text-slate-400 font-sans">${item.subtitle}</div>` : ''}
                            </div>
                        `).join('')}
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Binds ListBoxes
     */
    static bindListBoxes(rootEl, callback) {
        if (!rootEl) return;
        rootEl.querySelectorAll('.ui-listbox-container').forEach(container => {
            const trigger = container.querySelector('.ui-listbox-trigger');
            const dropdown = container.querySelector('.ui-listbox-dropdown');
            const search = container.querySelector('.ui-listbox-search');
            const items = container.querySelectorAll('.ui-listbox-item');

            if (trigger && dropdown) {
                trigger.onclick = (e) => {
                    e.stopPropagation();
                    dropdown.classList.toggle('hidden');
                    if (!dropdown.classList.contains('hidden') && search) {
                        search.focus();
                    }
                };

                if (search) {
                    search.oninput = (e) => {
                        const q = e.target.value.toLowerCase();
                        items.forEach(it => {
                            const text = it.textContent.toLowerCase();
                            it.style.display = text.includes(q) ? 'flex' : 'none';
                        });
                    };
                }

                items.forEach(item => {
                    item.onclick = (e) => {
                        e.stopPropagation();
                        const id = item.dataset.id;
                        const title = item.dataset.title;
                        const titleEl = container.querySelector(`#${container.dataset.listboxId}-selected-title`);
                        if (titleEl) titleEl.textContent = title;
                        dropdown.classList.add('hidden');
                        if (callback) callback(container.dataset.listboxId, id, title);
                    };
                });
            }
        });

        document.addEventListener('click', () => {
            document.querySelectorAll('.ui-listbox-dropdown').forEach(d => d.classList.add('hidden'));
        });
    }

    /**
     * Shows a Multi-Option Custom Dialog Modal
     * @param {Object} options { title, subtitle, icon, contentHtml, buttons: [{ text, role, class, onClick }], danger }
     */
    static showDialog(options = {}) {
        const {
            title = 'Confirm Action',
            subtitle = '',
            icon = 'ℹ️',
            contentHtml = '',
            buttons = [
                { text: 'Cancel', role: 'cancel', class: 'bg-slate-800 text-slate-300 hover:bg-slate-700' },
                { text: 'Confirm', role: 'confirm', class: 'bg-indigo-600 text-white hover:bg-indigo-500' }
            ],
            danger = false
        } = options;

        const modalId = `dialog-${Date.now()}`;
        const overlay = document.createElement('div');
        overlay.id = modalId;
        overlay.className = 'ui-dialog-overlay fixed inset-0 bg-black/70 backdrop-blur-md flex items-center justify-center p-4 z-[9999] animate-fade-in select-none';

        overlay.innerHTML = `
            <div class="ui-dialog-box w-full max-w-md bg-slate-900 border ${danger ? 'border-rose-500/50 shadow-rose-900/30' : 'border-slate-800 shadow-2xl shadow-indigo-950/40'} rounded-2xl p-6 text-slate-100 flex flex-col gap-4 font-sans animate-scale-up">
                <div class="flex items-start gap-3.5">
                    <div class="w-12 h-12 rounded-xl ${danger ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30' : 'bg-indigo-600/20 text-indigo-400 border border-indigo-500/30'} flex items-center justify-center text-2xl shrink-0">
                        ${icon}
                    </div>
                    <div class="flex-1 min-w-0">
                        <h3 class="text-base font-bold text-white font-mono tracking-tight">${title}</h3>
                        ${subtitle ? `<p class="text-xs text-slate-400 font-sans mt-0.5">${subtitle}</p>` : ''}
                    </div>
                </div>

                ${contentHtml ? `<div class="ui-dialog-content text-xs text-slate-300 bg-slate-950/60 p-3.5 rounded-xl border border-slate-800/80 font-mono">${contentHtml}</div>` : ''}

                <div class="flex items-center justify-end gap-2.5 pt-2 border-t border-slate-800/80 mt-1">
                    ${buttons.map((btn, idx) => `
                        <button type="button" class="ui-dialog-btn px-4 py-2 rounded-xl text-xs font-mono font-bold transition-all ${btn.class || 'bg-slate-800 text-slate-300'}"
                            data-btn-idx="${idx}" data-role="${btn.role || 'action'}">
                            ${btn.text}
                        </button>
                    `).join('')}
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        const close = () => {
            overlay.classList.add('animate-fade-out');
            setTimeout(() => overlay.remove(), 200);
        };

        overlay.querySelectorAll('.ui-dialog-btn').forEach(btnEl => {
            btnEl.onclick = async () => {
                const idx = parseInt(btnEl.dataset.btnIdx);
                const btnConfig = buttons[idx];
                if (btnConfig && typeof btnConfig.onClick === 'function') {
                    const result = await btnConfig.onClick();
                    if (result !== false) close();
                } else {
                    close();
                }
            };
        });

        overlay.onclick = (e) => {
            if (e.target === overlay) close();
        };

        return { close };
    }

    /**
     * Slide-In/Slide-Out Toast Notification Engine (Bottom-Right Stack)
     * @param {string} message 
     * @param {string} type 'success' | 'warning' | 'danger' | 'info'
     * @param {Object} options { title, duration, actionText, onAction }
     */
    static notify(message, type = 'info', options = {}) {
        const {
            title = '',
            duration = 4500,
            actionText = '',
            onAction = null
        } = options;

        let container = document.getElementById('ui-toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'ui-toast-container';
            container.className = 'fixed bottom-5 right-5 z-[99999] flex flex-col gap-2.5 max-w-sm w-full pointer-events-none select-none';
            document.body.appendChild(container);
        }

        const toastId = `toast-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`;
        const toast = document.createElement('div');
        toast.id = toastId;

        const themes = {
            success: { bg: 'bg-slate-900 border-emerald-500/50 shadow-emerald-950/40 text-emerald-300', icon: '✅', bar: 'bg-emerald-500' },
            warning: { bg: 'bg-slate-900 border-amber-500/50 shadow-amber-950/40 text-amber-300', icon: '⚠️', bar: 'bg-amber-500' },
            danger: { bg: 'bg-slate-900 border-rose-500/50 shadow-rose-950/40 text-rose-300', icon: '🚨', bar: 'bg-rose-500' },
            info: { bg: 'bg-slate-900 border-indigo-500/50 shadow-indigo-950/40 text-indigo-300', icon: 'ℹ️', bar: 'bg-indigo-500' }
        };

        const theme = themes[type] || themes.info;

        toast.className = `ui-toast-card pointer-events-auto p-3.5 rounded-2xl border shadow-2xl backdrop-blur-xl flex flex-col gap-2 ${theme.bg} transform translate-x-full transition-all duration-300 ease-out font-sans`;
        toast.innerHTML = `
            <div class="flex items-start justify-between gap-3">
                <div class="flex items-start gap-2.5 min-w-0">
                    <span class="text-base shrink-0">${theme.icon}</span>
                    <div class="flex-1 min-w-0">
                        ${title ? `<div class="text-xs font-bold font-mono text-white">${title}</div>` : ''}
                        <div class="text-xs text-slate-300 leading-snug">${message}</div>
                    </div>
                </div>
                <button class="ui-toast-close text-slate-500 hover:text-white text-xs font-bold leading-none p-1">✕</button>
            </div>
            ${actionText ? `
                <div class="flex justify-end">
                    <button class="ui-toast-action px-2.5 py-1 rounded-lg bg-white/10 hover:bg-white/20 text-[10px] font-mono font-bold text-white transition">
                        ${actionText}
                    </button>
                </div>
            ` : ''}
            <div class="ui-toast-progress w-full h-0.5 bg-slate-800 rounded-full overflow-hidden">
                <div class="ui-toast-progress-bar h-full ${theme.bar} transition-all" style="width: 100%;"></div>
            </div>
        `;

        container.appendChild(toast);

        // Slide in
        requestAnimationFrame(() => {
            toast.classList.remove('translate-x-full');
            toast.classList.add('translate-x-0');
        });

        const dismiss = () => {
            toast.classList.add('translate-x-full', 'opacity-0');
            setTimeout(() => toast.remove(), 300);
        };

        toast.querySelector('.ui-toast-close').onclick = dismiss;

        if (actionText && onAction) {
            toast.querySelector('.ui-toast-action').onclick = () => {
                onAction();
                dismiss();
            };
        }

        // Animate progress bar & auto dismiss
        const bar = toast.querySelector('.ui-toast-progress-bar');
        if (bar && duration > 0) {
            bar.style.transitionDuration = `${duration}ms`;
            requestAnimationFrame(() => {
                bar.style.width = '0%';
            });
            setTimeout(dismiss, duration);
        }

        return { dismiss };
    }

    /**
     * Form Validation Helper Methods
     */
    static Validator = {
        isValidEmail: (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(email).trim()),
        isValidPhone: (phone) => /^[+0-9\s-]{7,18}$/.test(String(phone).trim()),
        isValidCoord: (lat, lng) => !isNaN(lat) && !isNaN(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180,
        isValidRadius: (radius) => !isNaN(radius) && radius >= 10 && radius <= 50000,
        isNotEmpty: (str) => typeof str === 'string' && str.trim().length > 0,
        hasMinLength: (str, min) => typeof str === 'string' && str.trim().length >= min
    };
}
