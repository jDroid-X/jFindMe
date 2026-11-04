/**
 * jFindMe Matrix - Avatar & Asset Service
 * Responsibility: Resolves and serves avatar pictures, dynamic SVG avatars, and role badges
 */
export class AvatarService {
    // Role Color Palette Matrix (matches Android AvatarCalloutGenerator standard)
    static ROLE_COLORS = {
        SELF: "#00E5FF",      // Cyberpunk Cyan
        CHILD: "#FF6D00",     // Deep Neon Orange
        DEPENDENT: "#FF6D00",
        SON: "#FF6D00",
        DAUGHTER: "#FF6D00",
        SPOUSE: "#FF1744",    // Neon Crimson Red
        PARTNER: "#FF1744",
        WIFE: "#FF1744",
        HUSBAND: "#FF1744",
        GUARDIAN: "#FFD700",  // Core Guardian Gold
        PARENT: "#FFD700",
        MOTHER: "#FFD700",
        FATHER: "#FFD700",
        FRIEND: "#A855F7",    // Vivid Purple Neon
        PEER: "#A855F7",
        GROUP: "#A855F7",
        DEFAULT: "#38BDF8"    // Sky Blue
    };

    /**
     * Resolves the role color hex
     * @param {string} roleStr 
     * @param {boolean} isSelf 
     * @returns {string} Hex color
     */
    static getRoleColor(roleStr = "", isSelf = false) {
        if (isSelf) return this.ROLE_COLORS.SELF;
        const normalized = (roleStr || "").toUpperCase().trim();
        return this.ROLE_COLORS[normalized] || this.ROLE_COLORS.DEFAULT;
    }

    /**
     * Resolves the direct URL to an avatar image file (Supporting preset IDs, direct URLs, and Base64 payloads)
     * @param {string} avatarId 
     * @param {string} name 
     * @param {string} avatarData Optional Base64 JPEG data URI or raw base64 string
     * @returns {string} Image URL or Data URI
     */
    static getAvatarUrl(avatarId = "", name = "", avatarData = "") {
        // 1. Direct Base64 Binary Payload Provided
        if (avatarData && typeof avatarData === "string") {
            const trimmedData = avatarData.trim();
            if (trimmedData.startsWith("data:image/")) {
                return trimmedData;
            }
            if (trimmedData.length > 50) {
                return `data:image/jpeg;base64,${trimmedData}`;
            }
        }

        if (!avatarId) {
            return this.generateInitialAvatarSvg(name);
        }

        const idStr = String(avatarId).trim();

        // 2. Direct HTTP URL or Data URI
        if (idStr.startsWith("http://") || idStr.startsWith("https://") || idStr.startsWith("data:image/")) {
            return idStr;
        }

        // 3. Android Internal Path check (e.g. /data/user/0/.../remote_...png) - sanitize to prevent 404
        if (idStr.includes("/data/user/") || idStr.includes("/files/avatars/")) {
            return this.generateInitialAvatarSvg(name);
        }

        // 4. Numeric match e.g. "1", "12", "avatar_custom_3" -> "3.png" (Full 1..37 range matching assets/avatars/)
        let numMatch = idStr.match(/\d+/);
        if (numMatch) {
            let num = parseInt(numMatch[0]);
            let clamped = ((num - 1) % 37) + 1;
            return `/assets/avatars/${clamped}.png`;
        }

        // 5. Exact simple filename match (e.g. "1.png", "butterfly.png")
        if (!idStr.includes("/") && (idStr.endsWith(".png") || idStr.endsWith(".jpg") || idStr.endsWith(".webp"))) {
            return `/assets/avatars/${idStr}`;
        }

        // 6. Custom device selfie (AV_{agentId}_{seq}) fallback without Base64 payload -> deterministic fallback in 1..37
        if (idStr.startsWith("AV_")) {
            const hashNum = Math.abs(idStr.split("").reduce((acc, c) => ((acc << 5) - acc) + c.charCodeAt(0), 0)) % 37 + 1;
            return `/assets/avatars/${hashNum}.png`;
        }

        // 7. Custom photo without Base64 payload fallback
        if (idStr === "avatar_custom_photo") {
            return this.generateInitialAvatarSvg(name);
        }

        return this.generateInitialAvatarSvg(name);
    }

    /**
     * Generates a sleek, high-DPI dynamic SVG avatar with user initials
     * @param {string} name 
     * @param {string} roleColor 
     * @returns {string} Data URI
     */
    static generateInitialAvatarSvg(name = "Device", roleColor = "#38BDF8") {
        const initials = (name || "D")
            .split(" ")
            .map(w => w[0])
            .filter(Boolean)
            .slice(0, 2)
            .join("")
            .toUpperCase() || "D";

        const svg = `
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
                <circle cx="50" cy="50" r="46" fill="none" stroke="${roleColor}" stroke-width="4" />
                <text x="50%" y="54%" dominant-baseline="middle" text-anchor="middle" fill="#ffffff" 
                      font-family="system-ui, -apple-system, sans-serif" font-size="36" font-weight="800" letter-spacing="1">
                    ${initials}
                </text>
            </svg>
        `.trim();

        return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
    }
}
