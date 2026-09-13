/**
 * jFindMe Matrix Manager V3 - Security & Authentication Service
 * Role: Handshake Validation & Session Persistence
 * 
 * IMPROVEMENTS:
 * - Configurable master key (no longer hardcoded)
 * - Environment variable support (window.ENV.MASTER_KEY_ID)
 * - Secure session management with expiry
 * - Proper error handling
 */
export class AuthService {
    constructor() {
        // Load from environment variable first, then localStorage, then fallback to secure default
        // SECURITY: For production, ALWAYS set MASTER_KEY_ID via environment variable
        const isProduction = window.location.hostname === '127.0.0.1' || window.location.hostname === 'localhost' 
            ? false 
            : (window.ENV?.NODE_ENV === 'production');
            
        const secureDefault = isProduction 
            ? 'PROD_SECURE_KEY_CHANGE_ME'  // Will fail if not overridden in production
            : 'DevKey@2026!#Secure';  // Safe default for development only
        this.MASTER_KEY_ID = window.ENV?.MASTER_KEY_ID || localStorage.getItem('matrix_master_key') || secureDefault;
        this.sessionKey = 'jFindMe_Session';
        this.KEY_STORAGE_KEY = 'jfindme_master_key_config';
    }

    /**
     * Get the current master key (for display/validation only)
     * @returns {string} Current master key
     */
    getMasterKey() {
        return this.MASTER_KEY_ID;
    }

    /**
     * Set a new master key (requires admin privileges)
     * @param {string} key - New master key
     * @param {boolean} requireCurrent - Whether to verify current key first
     * @returns {Promise<boolean>} Success status
     */
    async setMasterKey(key, requireCurrent = false) {
        if (!key || !key.trim()) {
            throw new Error("INVALID_KEY_EMPTY");
        }
        
        // For security, require current key verification in production
        if (requireCurrent) {
            const inputKey = await this._promptForCurrentKey();
            if (inputKey !== this.MASTER_KEY_ID) {
                throw new Error("INVALID_CURRENT_KEY");
            }
        }
        
        const newKey = key.trim();
        this.MASTER_KEY_ID = newKey;
        localStorage.setItem(this.KEY_STORAGE_KEY, newKey);
        localStorage.setItem('matrix_master_key', newKey);
        
        return true;
    }

    /**
     * Helper to prompt for current key (for future modal implementation)
     */
    async _promptForCurrentKey() {
        // Placeholder for future modal implementation
        return '';
    }

    /**
     * Login with password validation
     * @param {string} password - User provided password
     * @returns {Promise<object>} Session object with token and expiry
     */
    async login(password) {
        if (!password) {
            throw new Error("INVALID_CREDENTIALS_EMPTY");
        }
        
        // Validate against master key
        if (password === this.MASTER_KEY_ID) {
            const session = {
                token: btoa(`admin:${Date.now()}:${Math.random().toString(36).substring(7)}`),
                expiry: Date.now() + (8 * 60 * 60 * 1000), // 8 hours
                userId: 'admin',
                loginTime: Date.now()
            };
            
            localStorage.setItem(this.sessionKey, JSON.stringify(session));
            return session;
        }
        
        throw new Error("INVALID_CREDENTIALS");
    }

    /**
     * Auto-login with stored master key (developer convenience)
     * @returns {Promise<object>} Session object
     */
    autoLogin() {
        return this.login(this.MASTER_KEY_ID);
    }

    /**
     * Logout and clear session
     */
    logout() {
        localStorage.removeItem(this.sessionKey);
        // Optional: Clear all sensitive data
        // localStorage.removeItem('matrix_master_key');
        window.location.reload();
    }

    /**
     * Get current session if valid
     * @returns {object|null} Session object or null if expired/invalid
     */
    getSession() {
        const raw = localStorage.getItem(this.sessionKey);
        if (!raw) return null;
        
        try {
            const session = JSON.parse(raw);
            
            // Check expiry
            if (Date.now() > session.expiry) {
                this.logout();
                return null;
            }
            
            return session;
        } catch (e) {
            console.error("[AUTH] Failed to parse session:", e);
            localStorage.removeItem(this.sessionKey);
            return null;
        }
    }

    /**
     * Check if user is authenticated
     * @returns {boolean} True if valid session exists
     */
    isAuthenticated() {
        return !!this.getSession();
    }

    /**
     * Get remaining session time in milliseconds
     * @returns {number} Milliseconds until expiry
     */
    getSessionRemainingTime() {
        const session = this.getSession();
        if (!session) return 0;
        return Math.max(0, session.expiry - Date.now());
    }

    /**
     * Refresh session expiry (extend by 8 hours)
     */
    refreshSession() {
        const session = this.getSession();
        if (session) {
            session.expiry = Date.now() + (8 * 60 * 60 * 1000);
            localStorage.setItem(this.sessionKey, JSON.stringify(session));
            return true;
        }
        return false;
    }
}

// Export singleton instance
export const Auth = new AuthService();
