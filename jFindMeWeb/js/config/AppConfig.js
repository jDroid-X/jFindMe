/**
 * jFindMe Matrix - Master Application Configuration (SSOT)
 * Centralizes all API keys, network endpoints, role colors, thresholds, and defaults.
 * ZERO HARDCODED CONSTANTS in application logic.
 */
export const AppConfig = {
    APP_NAME: "jFindMe Matrix Radar",
    VERSION: "3.0.0",
    
    // Google Maps JavaScript API Credentials
    GOOGLE_MAPS_API_KEY: "AIzaSyBFhlNKDNCu37_ZjkABUglfvZW9Iwm0qPs",
    GOOGLE_MAPS_LIBRARIES: ["places", "geometry", "drawing", "visualization"],

    // Backend Proxy Endpoints
    API_ENDPOINTS: {
        DISCOVERY: "/api/discovery",
        DEVICES_LIVE: "/api/devices-live",
        SAFE_ZONES: "/api/safe-zones",
        SAVE_SAFE_ZONE: "/api/save-safe-zone",
        UPDATE_PROFILE: "/api/update-profile",
        REVERSE_GEOCODE: "/api/reverse-geocode",
        POKE_DEVICE: "/api/poke-device",
        DEVICE_HISTORY: "/api/device-history",
        UPDATE_DATA: "/api/update",
        DELETE_DATA: "/api/delete",
        PURGE_NODE: "/api/purge-node",
        STATS: "/api/stats",
        PING: "/api/ping"
    },

    // Map Defaults
    MAP_DEFAULTS: {
        CENTER: { lat: 18.5788, lng: 73.8887 }, // Centered on Live Device Fleet
        ZOOM: 16,
        SEARCH_RADIUS_KM: 10.0, // 10 km default
        ENGINE: "leaflet",      // 'leaflet' (Clean Google/OSM Map - Zero API Key Restrictions)
        THEME: "google_clean",
        REFRESH_INTERVAL_SEC: 5,
        MAX_TRAIL_POINTS: 100
    },

    // Role-Based Neon Ring Colors (Aligned with Android AvatarCalloutGenerator)
    ROLE_COLORS: {
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
    },

    // Battery Warning Thresholds
    BATTERY_THRESHOLDS: {
        CRITICAL: 15,
        LOW: 30,
        MODERATE: 60
    },

    TOTAL_AVATARS_COUNT: 37
};
