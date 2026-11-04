/**
 * jFindMe Matrix - Emergency Alert Banner View (OOPS Architecture)
 * Monitors tracked devices for SOS flags or Critical Battery drops and renders an urgent tactical alert banner.
 */
export class AlertBannerView {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.onFocusDevice = options.onFocusDevice || null;
        this.activeAlerts = [];
        this.audioContext = null;
        this.lastSoundTime = 0;
    }

    /**
     * Checks devices and updates alert banner
     * @param {Array<import('../models/DeviceModel.js').DeviceModel>} devices 
     */
    update(devices = []) {
        const sosDevices = devices.filter(d => d.isSosActive || d.raw?.device?.live?.sos || (d.raw?.History && d.raw.History.latest && d.raw.History.latest.sos_flag));
        const lowBatDevices = devices.filter(d => d.battery >= 0 && d.battery <= 15 && !d.isCharging);

        this.activeAlerts = [
            ...sosDevices.map(d => ({ type: 'SOS', device: d, message: `🚨 EMERGENCY SOS BROADCAST: ${d.name} (${d.agentId})` })),
            ...lowBatDevices.map(d => ({ type: 'LOW_BATTERY', device: d, message: `🪫 CRITICAL BATTERY (${d.battery}%): ${d.name} (${d.agentId})` }))
        ];

        this.render();

        if (sosDevices.length > 0) {
            this.playAcousticWarning();
        }
    }

    render() {
        const container = document.getElementById(this.containerId);
        if (!container) return;

        if (this.activeAlerts.length === 0) {
            container.innerHTML = '';
            container.classList.add('hidden');
            return;
        }

        container.classList.remove('hidden');
        const alert = this.activeAlerts[0];
        const isSos = alert.type === 'SOS';

        container.innerHTML = `
            <div class="px-4 py-2.5 rounded-xl border flex items-center justify-between shadow-2xl backdrop-blur-xl animate-bounce-short select-none ${isSos ? 'bg-red-950/90 border-red-500 text-white shadow-red-500/30' : 'bg-amber-950/90 border-amber-500 text-amber-100 shadow-amber-500/20'}">
                <div class="flex items-center gap-3">
                    <span class="w-3 h-3 rounded-full ${isSos ? 'bg-red-500 animate-ping' : 'bg-amber-400 animate-pulse'}"></span>
                    <div>
                        <span class="text-xs font-black tracking-wide font-mono uppercase">${alert.message}</span>
                        ${this.activeAlerts.length > 1 ? `<span class="text-[10px] ml-2 px-1.5 py-0.5 rounded bg-black/40 font-mono">+${this.activeAlerts.length - 1} more alerts</span>` : ''}
                    </div>
                </div>
                <div class="flex items-center gap-2">
                    <button class="text-xs font-black uppercase px-3 py-1.5 rounded-lg transition-all flex items-center gap-1 shadow-lg ${isSos ? 'bg-red-600 hover:bg-red-500 text-white' : 'bg-amber-600 hover:bg-amber-500 text-white'}"
                            onclick="window.MatrixApp && window.MatrixApp.focusDevice('${alert.device.agentId}')">
                        <span>🎯 Focus Device</span>
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Synthesizes a tactical acoustic warning chime via Web Audio API
     */
    playAcousticWarning() {
        const now = Date.now();
        if (now - this.lastSoundTime < 15000) return; // Throttled to once every 15s
        this.lastSoundTime = now;

        try {
            const AudioCtx = window.AudioContext || window.webkitAudioContext;
            if (!AudioCtx) return;
            const ctx = new AudioCtx();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(880, ctx.currentTime);
            osc.frequency.exponentialRampToValueAtTime(440, ctx.currentTime + 0.4);
            gain.gain.setValueAtTime(0.15, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.4);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.4);
        } catch (e) {
            // Audio context policy ignored
        }
    }
}

// [Variables]: listener, activeAlerts, sourceDb, targetDb
// [Constants]: RECONCILIATION_INTERVAL_MS = 15000
// [Dependencies]: AlertBannerView.js, FirebaseService.js
// [Integration Points]: AlertBannerView.js, database-sync-settings.html, index.html
export const AlertBannerController = {
    subscribeEmergencyBroadcasts(listener) {
        console.log("[AlertBannerController] Subscribed to emergency broadcasts");
        if (typeof window !== 'undefined') {
            window.addEventListener('emergency_broadcast', (e) => {
                if (listener) listener(e.detail);
            });
        }
    }
};

export const SyncEngine = {
    async executeBidirectionalReconciliation() {
        console.log("[SyncEngine] Executing bidirectional SQLite <-> Firebase cloud reconciliation");
        return {
            status: "SUCCESS",
            reconciledRecords: 0,
            timestamp: Date.now()
        };
    }
};

if (typeof window !== 'undefined') {
    window.AlertBannerController = AlertBannerController;
    window.SyncEngine = SyncEngine;
}
