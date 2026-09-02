import { GeoUtils } from '../core/GeoUtils.js';
import { FeedbackDialog } from '../components/FeedbackDialog.js';

/**
 * jFindMe Matrix - History Breadcrumb Trail & Playback View (OOPS Architecture)
 * Renders route breadcrumbs on the map and provides an interactive playback scrubber bar with speed controls.
 * 100% Theme-Adaptive with user notifications.
 */
export class HistoryTrailView {
    constructor(mapView, options = {}) {
        this.mapView = mapView;
        this.onClose = options.onClose || null;
        this.breadcrumbs = [];
        this.currentIndex = 0;
        this.isPlaying = false;
        this.playSpeed = 1; // 1x, 2x, 5x
        this.playbackTimer = null;
        this.activeMarker = null;
        this.controlPanelElement = null;
    }

    /**
     * Loads breadcrumbs and activates the playback HUD
     * @param {string} deviceName 
     * @param {Array<{latitude: number, longitude: number, timestamp: any, speed: number, battery: number, address: string}>} breadcrumbs 
     */
    showTrail(deviceName, breadcrumbs = []) {
        this.clear();
        if (!breadcrumbs || breadcrumbs.length === 0) {
            FeedbackDialog.showToast(`No historical breadcrumbs recorded for ${deviceName}.`, "info");
            return;
        }

        this.breadcrumbs = breadcrumbs;
        this.currentIndex = breadcrumbs.length - 1;

        // Render full polyline on MapView
        this.mapView.renderHistoryTrail(breadcrumbs);

        // Compute total trail distance
        let totalDistanceKm = 0;
        for (let i = 0; i < breadcrumbs.length - 1; i++) {
            totalDistanceKm += GeoUtils.calculateDistanceKm(
                breadcrumbs[i].latitude, breadcrumbs[i].longitude,
                breadcrumbs[i+1].latitude, breadcrumbs[i+1].longitude
            );
        }

        // Render Floating Playback HUD on bottom of map
        this.renderPlaybackHud(deviceName, totalDistanceKm);
    }

    renderPlaybackHud(deviceName, totalDistanceKm) {
        this.closeHud();

        const hud = document.createElement('div');
        hud.id = 'trail-playback-hud';
        hud.className = 'absolute bottom-6 left-1/2 transform -translate-x-1/2 z-[950] card-theme shadow-2xl p-3.5 border rounded-2xl text-primary-theme w-[92%] max-w-xl animate-fade-in font-sans select-none';

        const lastCrumb = this.breadcrumbs[this.currentIndex];

        hud.innerHTML = `
            <div class="space-y-2.5">
                <!-- Header -->
                <div class="flex items-center justify-between border-b border-subtle-theme pb-2">
                    <div class="flex items-center gap-2 min-w-0">
                        <span class="w-2.5 h-2.5 rounded-full bg-sky-400 animate-ping shrink-0"></span>
                        <h4 class="text-xs font-black tracking-wider text-sky-400 uppercase truncate">Route Trail: ${deviceName}</h4>
                        <span class="text-[10px] font-mono text-muted-theme truncate">(${this.breadcrumbs.length} Pts • ${GeoUtils.formatDistance(totalDistanceKm)})</span>
                    </div>
                    <button id="btn-close-trail-hud" class="btn-action-secondary text-[10px] py-0.5 px-2 shrink-0">
                        ✕ Close Trail
                    </button>
                </div>

                <!-- Active Waypoint Readout -->
                <div class="flex items-center justify-between text-xs font-mono surface-theme px-3 py-1.5 rounded-xl border">
                    <div class="flex items-center gap-1.5">
                        <span class="text-muted-theme">Time:</span>
                        <span id="hud-time" class="font-bold text-primary-theme">${new Date(Number(lastCrumb.timestamp) || Date.now()).toLocaleTimeString()}</span>
                    </div>
                    <div class="flex items-center gap-1.5">
                        <span class="text-muted-theme">Speed:</span>
                        <span id="hud-speed" class="font-bold text-emerald-500">${(lastCrumb.speed * 3.6).toFixed(1)} km/h</span>
                    </div>
                    <div class="flex items-center gap-1.5">
                        <span class="text-muted-theme">Point:</span>
                        <span id="hud-point" class="font-bold text-sky-400">${this.currentIndex + 1}/${this.breadcrumbs.length}</span>
                    </div>
                </div>

                <!-- Scrubber Slider -->
                <div class="flex items-center gap-2.5">
                    <button id="btn-play-pause" class="btn-action-primary w-8 h-8 rounded-xl flex items-center justify-center text-sm font-bold shrink-0 p-0">
                        ▶
                    </button>
                    <input type="range" id="hud-scrubber" min="0" max="${this.breadcrumbs.length - 1}" value="${this.currentIndex}"
                           class="w-full h-2 bg-canvas rounded-lg appearance-none cursor-pointer accent-sky-500">
                    <button id="btn-speed-toggle" class="btn-action-secondary px-2 py-0.5 text-[10px] font-mono font-bold shrink-0">
                        1x
                    </button>
                </div>
            </div>
        `;

        const mapWorkspace = document.getElementById('map-workspace');
        if (mapWorkspace) {
            mapWorkspace.appendChild(hud);
            this.controlPanelElement = hud;
            this.bindHudEvents(hud);
        }
    }

    bindHudEvents(hud) {
        const closeBtn = hud.querySelector('#btn-close-trail-hud');
        const playBtn = hud.querySelector('#btn-play-pause');
        const scrubber = hud.querySelector('#hud-scrubber');
        const speedBtn = hud.querySelector('#btn-speed-toggle');

        closeBtn.addEventListener('click', () => this.clear());

        scrubber.addEventListener('input', (e) => {
            this.pause();
            this.seekTo(parseInt(e.target.value));
        });

        playBtn.addEventListener('click', () => {
            if (this.isPlaying) this.pause();
            else this.play();
        });

        speedBtn.addEventListener('click', () => {
            if (this.playSpeed === 1) this.playSpeed = 2;
            else if (this.playSpeed === 2) this.playSpeed = 5;
            else this.playSpeed = 1;
            speedBtn.textContent = `${this.playSpeed}x`;
            if (this.isPlaying) {
                this.pause();
                this.play();
            }
        });
    }

    play() {
        if (this.currentIndex >= this.breadcrumbs.length - 1) {
            this.currentIndex = 0;
        }
        this.isPlaying = true;
        const playBtn = this.controlPanelElement?.querySelector('#btn-play-pause');
        if (playBtn) playBtn.textContent = '⏸';

        const stepMs = Math.max(100, Math.floor(1000 / this.playSpeed));
        this.playbackTimer = setInterval(() => {
            if (this.currentIndex < this.breadcrumbs.length - 1) {
                this.seekTo(this.currentIndex + 1);
            } else {
                this.pause();
            }
        }, stepMs);
    }

    pause() {
        this.isPlaying = false;
        if (this.playbackTimer) {
            clearInterval(this.playbackTimer);
            this.playbackTimer = null;
        }
        const playBtn = this.controlPanelElement?.querySelector('#btn-play-pause');
        if (playBtn) playBtn.textContent = '▶';
    }

    seekTo(index) {
        if (index < 0 || index >= this.breadcrumbs.length) return;
        this.currentIndex = index;
        const crumb = this.breadcrumbs[index];

        // Update HUD
        const scrubber = this.controlPanelElement?.querySelector('#hud-scrubber');
        const timeReadout = this.controlPanelElement?.querySelector('#hud-time');
        const speedReadout = this.controlPanelElement?.querySelector('#hud-speed');
        const pointReadout = this.controlPanelElement?.querySelector('#hud-point');

        if (scrubber) scrubber.value = index;
        if (timeReadout) timeReadout.textContent = new Date(Number(crumb.timestamp) || Date.now()).toLocaleTimeString();
        if (speedReadout) speedReadout.textContent = `${(crumb.speed * 3.6).toFixed(1)} km/h`;
        if (pointReadout) pointReadout.textContent = `${index + 1}/${this.breadcrumbs.length}`;

        // Pan map smoothly to point
        if (this.mapView && this.mapView.map) {
            this.mapView.map.panTo([crumb.latitude, crumb.longitude], { animate: true, duration: 0.3 });
        }
    }

    closeHud() {
        if (this.controlPanelElement) {
            this.controlPanelElement.remove();
            this.controlPanelElement = null;
        }
    }

    clear() {
        this.pause();
        this.closeHud();
        this.breadcrumbs = [];
        if (this.mapView) {
            this.mapView.clearHistoryTrail();
        }
        if (this.onClose) this.onClose();
    }
}
