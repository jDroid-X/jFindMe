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
     * Resolves the direct URL to an avatar image file
     * @param {string} avatarId 
     * @param {string} name 
     * @returns {string} Image URL or Data URI
     */
    static getAvatarUrl(avatarId = "", name = "") {
        if (!avatarId) {
            return this.generateInitialAvatarSvg(name);
        }

        const idStr = String(avatarId).trim();

        // Direct HTTP URL
        if (idStr.startsWith("http://") || idStr.startsWith("https://") || idStr.startsWith("data:image/")) {
            return idStr;
        }

        // Numeric match e.g. "1", "12", "avatar_custom_3" -> "3.png"
        let numMatch = idStr.match(/\d+/);
        if (numMatch) {
            let num = parseInt(numMatch[0]);
            if (num >= 1 && num <= 37) {
                return `/Avatar/${num}.png`;
            }
        }

        // Exact filename match
        if (idStr.endsWith(".png") || idStr.endsWith(".jpg") || idStr.endsWith(".webp")) {
            return `/Avatar/${idStr}`;
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
