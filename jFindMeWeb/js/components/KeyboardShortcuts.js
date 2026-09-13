/**
 * KeyboardShortcuts.js - Enterprise Keyboard Navigation System
 * Features: Global shortcuts, context-aware bindings, visual hints
 */

export class KeyboardShortcuts {
    static _bindings = new Map();
    static _activeContext = 'default';
    static _disabled = false;

    /**
     * Initialize keyboard shortcuts system
     */
    static init() {
        document.addEventListener('keydown', (e) => {
            if (this._disabled) return;
            
            // Don't trigger when typing in inputs
            const tag = document.activeElement?.tagName;
            if (tag === 'INPUT' || tag === 'TEXTAREA' || document.activeElement?.isContentEditable) {
                // Allow Ctrl+C, Ctrl+V, Ctrl+A, Ctrl+X for clipboard
                if (e.ctrlKey && ['c', 'v', 'a', 'x'].includes(e.key.toLowerCase())) {
                    return;
                }
                return;
            }

            this._handleKey(e);
        });

        console.log('[KEYBOARD] Shortcuts initialized');
    }

    /**
     * Register a keyboard shortcut
     * @param {string} key - Key combination (e.g., 'ctrl+k', 'alt+1')
     * @param {Function} callback - Handler function
     * @param {string} context - Context name (default, map, dashboard, etc.)
     */
    static register(key, callback, context = 'default') {
        const normalizedKey = key.toLowerCase().replace(/\s+/g, '+').split('+');
        const bindingKey = normalizedKey.join('-');
        
        if (!this._bindings.has(context)) {
            this._bindings.set(context, new Map());
        }
        
        this._bindings.get(context).set(bindingKey, {
            callback,
            key: normalizedKey
        });
    }

    /**
     * Set active context (changes available shortcuts)
     * @param {string} context 
     */
    static setActiveContext(context) {
        this._activeContext = context;
        console.log(`[KEYBOARD] Context changed to: ${context}`);
    }

    /**
     * Handle keydown event
     * @param {KeyboardEvent} e 
     */
    static _handleKey(e) {
        const bindings = this._bindings.get(this._activeContext) || new Map();
        
        // Check all contexts if no specific context match
        let allBindings = new Map();
        this._bindings.forEach((ctxBindings, ctxName) => {
            ctxBindings.forEach((binding, key) => {
                allBindings.set(key, { ...binding, context: ctxName });
            });
        });

        for (const [key, binding] of allBindings) {
            if (this._matchesKeyBinding(e, key, binding.key)) {
                e.preventDefault();
                binding.callback(e);
                return;
            }
        }
    }

    /**
     * Check if event matches key binding
     */
    static _matchesKeyBinding(e, pressedKey, bindingKeys) {
        const [key, ...modifiers] = bindingKeys;
        
        // Check modifiers
        if (modifiers.includes('ctrl') !== e.ctrlKey) return false;
        if (modifiers.includes('alt') !== e.altKey) return false;
        if (modifiers.includes('shift') !== e.shiftKey) return false;
        
        // Check key
        return e.key.toLowerCase() === key.toLowerCase();
    }

    /**
     * Get tooltip text for shortcut
     */
    static getTooltip(key) {
        return key.toUpperCase().replace(/-/g, '+');
    }
}

// Default shortcuts registration
KeyboardShortcuts.register('escape', () => {
    // Close any open modals
    const modals = document.querySelectorAll('.fixed.inset-0');
    modals.forEach(m => m.remove());
}, 'global');

KeyboardShortcuts.register('ctrl+k', () => {
    // Focus search
    const search = document.getElementById('dash-search-input') || 
                   document.querySelector('input[type="text"]');
    if (search) search.focus();
}, 'global');

KeyboardShortcuts.register('1', () => window.showLiveMap?.(), 'navigation');
KeyboardShortcuts.register('2', () => window.showDashboard?.(), 'navigation');
KeyboardShortcuts.register('3', () => window.showDeviceUser?.(), 'navigation');
KeyboardShortcuts.register('4', () => window.showGeofencing?.(), 'navigation');
KeyboardShortcuts.register('5', () => window.showArchitectV2?.(), 'navigation');

KeyboardShortcuts.register('space', (e) => {
    // Play/pause history
    const hud = document.getElementById('trail-playback-hud');
    if (hud) {
        const playBtn = hud.querySelector('[data-action="play"]');
        if (playBtn) playBtn.click();
    }
}, 'map');

export default KeyboardShortcuts;
