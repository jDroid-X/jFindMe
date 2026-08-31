/**
 * jFindMe Matrix Manager V3 - Security & Authentication Service
 * Role: Handshake Validation & Session Persistence
 */
export class AuthService {
    constructor() {
        this.MASTER_KEY_ID = window.ENV?.MASTER_KEY_ID || localStorage.getItem('matrix_master_key') || 'Jeet@123Meet';
        this.sessionKey = 'jFindMe_Session';
    }

    setMasterKey(key) {
        if (key && key.trim()) {
            this.MASTER_KEY_ID = key.trim();
            localStorage.setItem('matrix_master_key', this.MASTER_KEY_ID);
        }
    }

    async login(password) {
        if (password === this.MASTER_KEY_ID) {
            const session = {
                token: btoa(`admin:${Date.now()}`),
                expiry: Date.now() + (8 * 60 * 60 * 1000)
            };
            localStorage.setItem(this.sessionKey, JSON.stringify(session));
            return session;
        }
        throw new Error("INVALID_CREDENTIALS");
    }

    autoLogin() {
        return this.login(this.MASTER_KEY_ID);
    }

    logout() {
        localStorage.removeItem(this.sessionKey);
        window.location.reload();
    }

    getSession() {
        const raw = localStorage.getItem(this.sessionKey);
        if (!raw) return null;
        const session = JSON.parse(raw);
        if (Date.now() > session.expiry) {
            this.logout();
            return null;
        }
        return session;
    }

    isAuthenticated() {
        return !!this.getSession();
    }
}

export const Auth = new AuthService();
