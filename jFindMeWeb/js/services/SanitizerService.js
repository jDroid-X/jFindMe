/**
 * ==============================================================================
 * SANITIZER SERVICE & HIGH-PERFORMANCE DATA GOVERNANCE SENTINEL
 * Codename: jDroid-X | Architecture: OOPS MVC Model Interceptor
 * ==============================================================================
 * 
 * [Variables]:
 *   - _sanitizedCache (Map): In-memory O(1) LRU/Memoization cache for device identities
 * 
 * [Constants]:
 *   - HARDWARE_ID_REGEX: Canonical 8-character uppercase alphanumeric regex
 *   - CANONICAL_ROLES: Strict Role-Based Access Control (RBAC) set
 * 
 * [Dependencies]:
 *   - None (Zero runtime dependencies, ultra-lightweight)
 * 
 * [Integration Points]:
 *   - DeviceModel.js (Tier 1 & Tier 2 data binding)
 *   - Firebase Realtime Database Data Adapters
 * ==============================================================================
 */

export class SanitizerService {
    static HARDWARE_ID_REGEX = /^[A-Z0-9]{8}$/;
    
    static CANONICAL_ROLES = new Set([
        'ADMIN',
        'PARENT',
        'SPOUSE',
        'CHILD',
        'GUARDIAN',
        'FRIEND',
        'MEMBER',
        'OWNER'
    ]);

    static _sanitizedCache = new Map();

    /**
     * Validates whether a given string adheres to the canonical 8-character silicon hardware identity
     * @param {string} id 
     * @returns {boolean}
     */
    static isValidHardwareId(id) {
        if (!id || typeof id !== 'string') return false;
        return this.HARDWARE_ID_REGEX.test(id.trim());
    }

    /**
     * Sanitizes and extracts a canonical hardware ID from raw or legacy concatenated strings
     * Example: "2T921R129153" -> returns valid 8-char or clean fallback
     * @param {string} rawId 
     * @param {string} fallbackId 
     * @returns {string}
     */
    static sanitizeHardwareId(rawId, fallbackId = "") {
        if (!rawId || typeof rawId !== 'string') return fallbackId;
        const clean = rawId.trim();
        if (this.isValidHardwareId(clean)) return clean;

        // Memoized resolution
        if (this._sanitizedCache.has(clean)) {
            return this._sanitizedCache.get(clean);
        }

        // Detect 16-char concatenated IDs (e.g. F8CA6628E835FC67)
        if (clean.length === 16 && /^[A-Z0-9]{16}$/.test(clean)) {
            const prefix = clean.substring(0, 8);
            this._sanitizedCache.set(clean, prefix);
            return prefix;
        }

        this._sanitizedCache.set(clean, fallbackId || clean);
        return fallbackId || clean;
    }

    /**
     * Sanitizes the connected_id pairing bridge
     * Ensures member nodes always resolve to a clean 8-character peer or circle anchor
     * @param {string} connectedId 
     * @param {string} activeGroup 
     * @returns {string}
     */
    static sanitizeConnectedId(connectedId, activeGroup = "") {
        if (!connectedId || connectedId === "N/A" || connectedId === "null" || connectedId === "undefined") {
            return activeGroup ? activeGroup.replace("GRP_", "") : "";
        }
        const clean = connectedId.trim();
        if (clean.startsWith("GRP_")) return clean;
        if (this.isValidHardwareId(clean)) return clean;
        return this.sanitizeHardwareId(clean, "");
    }

    /**
     * Validates and standardizes circle membership roles
     * @param {string} role 
     * @param {string} defaultRole 
     * @returns {string}
     */
    static sanitizeRole(role, defaultRole = "MEMBER") {
        if (!role || typeof role !== 'string') return defaultRole;
        const upper = role.trim().toUpperCase();
        if (this.CANONICAL_ROLES.has(upper)) return upper;
        
        // Aliases mapping
        if (upper === 'MOTHER' || upper === 'FATHER') return 'PARENT';
        if (upper === 'DAUGHTER' || upper === 'SON') return 'CHILD';
        if (upper === 'WIFE' || upper === 'HUSBAND') return 'SPOUSE';

        return defaultRole;
    }

    /**
     * Harmonizes device display names and filters out stale legacy placeholders
     * @param {string} name 
     * @param {string} model 
     * @param {string} deviceId 
     * @returns {string}
     */
    static sanitizeDeviceName(name, model = "", deviceId = "") {
        if (!name || typeof name !== 'string') return model || `Device_${deviceId}`;
        const trimmed = name.trim();
        // [Variables]: name, model, deviceId, trimmed | [Constants]: None | [Dependencies]: None | [Integration Points]: DeviceModel.js
        // Zero Hardcoded Device Policy (Program Mapping Master SSOT)
        if (trimmed === "Cloud Roster Mirror") {
            return model || (deviceId ? `Device_${deviceId}` : "Active Node");
        }
        return trimmed;
    }

    /**
     * Flushes the memory cache when database reloads
     */
    static clearCache() {
        this._sanitizedCache.clear();
    }
}
