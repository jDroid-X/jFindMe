/**
 * jFindMe Matrix - Map Configuration & High-Performance Tile Providers
 * 100% Free, Zero API Keys, Zero Watermarks, Worldwide High-DPI Zoom Support.
 */
export const MapConfig = {
    // Clean, High-DPI Tile Providers with Worldwide High-Zoom Support
    TILE_PROVIDERS: {
        google_clean: {
            name: "Crisp Vector Roadmap (High-DPI)",
            url: "https://mt{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
            subdomains: "0123",
            maxZoom: 22,
            maxNativeZoom: 20,
            attribution: '&copy; Map Data'
        },
        satellite: {
            name: "Satellite & Hybrid HD Layer",
            url: "https://mt{s}.google.com/vt/lyrs=y&x={x}&y={y}&z={z}",
            subdomains: "0123",
            maxZoom: 22,
            maxNativeZoom: 20,
            attribution: '&copy; Satellite Imagery'
        },
        carto_voyager: {
            name: "CartoDB Voyager (Vector Clean)",
            url: "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png",
            subdomains: "abcd",
            maxZoom: 20,
            maxNativeZoom: 19,
            attribution: '&copy; <a href="https://carto.com/">CARTO</a> & OpenStreetMap'
        },
        carto_dark: {
            name: "Dark Matrix (Tactical Dark)",
            url: "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png",
            subdomains: "abcd",
            maxZoom: 20,
            maxNativeZoom: 19,
            attribution: '&copy; <a href="https://carto.com/">CARTO</a> & OpenStreetMap'
        },
        osm: {
            name: "OpenStreetMap Standard",
            url: "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
            subdomains: "abc",
            maxZoom: 19,
            maxNativeZoom: 19,
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        },
        topo: {
            name: "Topographic Terrain",
            url: "https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png",
            subdomains: "abc",
            maxZoom: 17,
            maxNativeZoom: 17,
            attribution: '&copy; <a href="https://opentopomap.org">OpenTopoMap</a>'
        }
    }
};
