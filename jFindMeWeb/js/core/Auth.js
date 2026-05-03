/**
 * jFindMe Matrix Manager V3 - Security & Authentication Service
 * Role: Handshake Validation & Session Persistence
 */
export class AuthService {
    constructor() {
        this.MASTER_KEY_ID = 'Jeet@123Meet';
        this.sessionKey = 'jFindMe_Session';
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
