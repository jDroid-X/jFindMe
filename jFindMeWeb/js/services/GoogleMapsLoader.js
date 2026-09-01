import { AppConfig } from '../config/AppConfig.js';

/**
 * jFindMe Matrix - Google Maps JavaScript API Asynchronous Loader
 * Dynamically loads Google Maps JS SDK with libraries (places, geometry, drawing, visualization)
 */
export class GoogleMapsLoader {
    static isLoaded = false;
    static isLoading = false;
    static loadPromise = null;

    /**
     * Loads the Google Maps SDK
     * @returns {Promise<typeof google.maps>}
     */
    static load() {
        if (this.isLoaded && window.google && window.google.maps) {
            return Promise.resolve(window.google.maps);
        }

        if (this.loadPromise) {
            return this.loadPromise;
        }

        this.isLoading = true;
        this.loadPromise = new Promise((resolve, reject) => {
            if (window.google && window.google.maps) {
                this.isLoaded = true;
                this.isLoading = false;
                return resolve(window.google.maps);
            }

            const callbackName = `__googleMapsCallback_${Date.now()}`;
            window[callbackName] = () => {
                this.isLoaded = true;
                this.isLoading = false;
                delete window[callbackName];
                console.log("[GOOGLE_MAPS] SDK loaded successfully.");
                resolve(window.google.maps);
            };

            const libraries = AppConfig.GOOGLE_MAPS_LIBRARIES.join(",");
            const script = document.createElement("script");
            script.src = `https://maps.googleapis.com/maps/api/js?key=${AppConfig.GOOGLE_MAPS_API_KEY}&libraries=${libraries}&callback=${callbackName}&v=weekly`;
            script.async = true;
            script.defer = true;
            script.onerror = (err) => {
                this.isLoading = false;
                this.loadPromise = null;
                console.warn("[GOOGLE_MAPS] SDK load error, fallback available:", err);
                reject(new Error("GOOGLE_MAPS_SDK_LOAD_FAILED"));
            };

            document.head.appendChild(script);
        });

        return this.loadPromise;
    }
}
