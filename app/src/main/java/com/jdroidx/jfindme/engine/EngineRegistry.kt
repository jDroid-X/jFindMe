package com.jdroidx.jfindme.engine

import com.jdroidx.jfindme.utils.SystemLogger

/**
 * EngineRegistry — Central Engine Lifecycle State Machine.
 *
 * AUDIT FIX [AUD-011]: LocationEngine had no pause/resume heartbeat mode.
 * All engines were either fully running or fully stopped — no intermediate
 * "stay alive at minimal cost" mode for when screens are backgrounded.
 *
 * ARCHITECTURE:
 * Each engine registers itself with a unique ID and reports its current mode.
 * Screens (Activities/Fragments) call activate()/pause()/deactivate() as
 * their lifecycle changes. Engines observe their own mode via callbacks.
 *
 * ENGINE MODES:
 * ─ ACTIVE     : Full-speed execution (screen visible, data actively needed)
 * ─ HEARTBEAT  : Minimal-cost periodic pulse (screen hidden, engine must stay alive)
 * ─ DORMANT    : Fully suspended (feature not in use, zero CPU/battery cost)
 *
 * HEARTBEAT INTERVALS (defaults — engines can override):
 * ─ LocationEngine  : 300,000ms (5 min) heartbeat pulse
 * ─ MapRenderEngine : 30,000ms  (30 sec) position refresh
 * ─ GeofenceEngine  : 1,800,000ms (30 min) zone status check
 * ─ FirebaseSyncEngine : 120,000ms (2 min) connectivity check
 *
 * USAGE:
 *   // In Activity.onResume():
 *   EngineRegistry.activate("LocationEngine")
 *   // In Activity.onPause():
 *   EngineRegistry.heartbeat("LocationEngine")
 *   // In Activity.onStop():
 *   EngineRegistry.deactivate("LocationEngine")
 */
object EngineRegistry {

    private const val TAG = "EngineRegistry"

    // ─── ENGINE IDs ───────────────────────────────────────────────────────────
    const val ENGINE_LOCATION        = "LocationEngine"
    const val ENGINE_MAP_RENDER      = "MapRenderEngine"
    const val ENGINE_HEATMAP         = "HeatmapEngine"
    const val ENGINE_GEOFENCE        = "GeofenceEngine"
    const val ENGINE_AVATAR_SYNC     = "AvatarSyncEngine"
    const val ENGINE_FIREBASE_SYNC   = "FirebaseSyncEngine"
    const val ENGINE_OFFLINE_SYNC    = "OfflineSyncEngine"
    const val ENGINE_PAIRING         = "DevicePairingEngine"
    const val ENGINE_SECURITY        = "SecurityEngine"
    const val ENGINE_SPATIAL         = "SpatialProcessor"

    // ─── MODE ENUM ────────────────────────────────────────────────────────────
    enum class EngineMode {
        ACTIVE,     // Full speed — screen visible
        HEARTBEAT,  // Minimal cost — screen hidden but engine must stay alive
        DORMANT     // Fully suspended — feature not in use
    }

    // ─── STATE STORAGE ────────────────────────────────────────────────────────
    private val engineStates = mutableMapOf<String, EngineMode>()
    private val modeChangeCallbacks = mutableMapOf<String, MutableList<(EngineMode) -> Unit>>()

    // ─── DEFAULT HEARTBEAT INTERVALS ─────────────────────────────────────────
    private val defaultHeartbeatIntervals = mapOf(
        ENGINE_LOCATION      to 300_000L,   // 5 minutes
        ENGINE_MAP_RENDER    to 30_000L,    // 30 seconds
        ENGINE_HEATMAP       to 0L,         // N/A — purely on-demand
        ENGINE_GEOFENCE      to 1_800_000L, // 30 minutes (OS-managed anyway)
        ENGINE_AVATAR_SYNC   to 0L,         // N/A — event-driven
        ENGINE_FIREBASE_SYNC to 120_000L,   // 2 minutes
        ENGINE_OFFLINE_SYNC  to 0L,         // N/A — queue-driven
        ENGINE_PAIRING       to 0L,         // N/A — event-driven
        ENGINE_SECURITY      to 0L,         // N/A — boot-time only
        ENGINE_SPATIAL       to 0L          // N/A — on-demand calculation
    )

    // ─── STATE TRANSITIONS ────────────────────────────────────────────────────

    /**
     * Transitions an engine to ACTIVE mode (full speed).
     * Call from Activity.onResume() or when the feature screen becomes visible.
     */
    fun activate(engineId: String) {
        val previous = engineStates[engineId] ?: EngineMode.DORMANT
        if (previous == EngineMode.ACTIVE) return // Already active, no-op
        engineStates[engineId] = EngineMode.ACTIVE
        SystemLogger.d(TAG, "[$engineId] → ACTIVE (was: $previous)")
        notifyCallbacks(engineId, EngineMode.ACTIVE)
    }

    /**
     * Transitions an engine to HEARTBEAT mode (minimal-cost pulse).
     * Call from Activity.onPause() when screen is hidden but data should stay live.
     */
    fun heartbeat(engineId: String) {
        val previous = engineStates[engineId] ?: EngineMode.DORMANT
        if (previous == EngineMode.HEARTBEAT) return
        engineStates[engineId] = EngineMode.HEARTBEAT
        val interval = defaultHeartbeatIntervals[engineId] ?: 0L
        SystemLogger.d(TAG, "[$engineId] → HEARTBEAT (interval: ${interval}ms, was: $previous)")
        notifyCallbacks(engineId, EngineMode.HEARTBEAT)
    }

    /**
     * Transitions an engine to DORMANT mode (fully suspended).
     * Call from Activity.onStop() or when a feature is explicitly disabled.
     */
    fun deactivate(engineId: String) {
        val previous = engineStates[engineId] ?: EngineMode.DORMANT
        if (previous == EngineMode.DORMANT) return
        engineStates[engineId] = EngineMode.DORMANT
        SystemLogger.d(TAG, "[$engineId] → DORMANT (was: $previous)")
        notifyCallbacks(engineId, EngineMode.DORMANT)
    }

    /**
     * Returns the current mode for a given engine.
     * Engines can self-check their mode before performing work.
     */
    fun getMode(engineId: String): EngineMode = engineStates[engineId] ?: EngineMode.DORMANT

    /**
     * Returns the default heartbeat interval for an engine in milliseconds.
     * Returns 0 if the engine is event-driven (no polling heartbeat).
     */
    fun getHeartbeatInterval(engineId: String): Long = defaultHeartbeatIntervals[engineId] ?: 0L

    /**
     * Returns a human-readable status report of all registered engine states.
     * Use in WatchDogAuditActivity or SystemLogActivity diagnostics.
     */
    fun getStatusReport(): String {
        if (engineStates.isEmpty()) return "No engines registered."
        val sb = StringBuilder()
        engineStates.forEach { (id, mode) ->
            val icon = when (mode) {
                EngineMode.ACTIVE    -> "🟢"
                EngineMode.HEARTBEAT -> "🟡"
                EngineMode.DORMANT   -> "⚫"
            }
            sb.appendLine("$icon $id → $mode")
        }
        return sb.toString()
    }

    // ─── CALLBACK SYSTEM ──────────────────────────────────────────────────────

    /**
     * Registers a callback to be notified when an engine's mode changes.
     * Engines should register here to adapt their polling intervals.
     *
     * @param engineId  The engine to observe
     * @param callback  Lambda receiving the new EngineMode
     */
    fun observeMode(engineId: String, callback: (EngineMode) -> Unit) {
        modeChangeCallbacks.getOrPut(engineId) { mutableListOf() }.add(callback)
    }

    /**
     * Removes all callbacks registered for a given engine.
     * Call from Engine.onDestroy() or ViewModel.onCleared() to prevent leaks.
     */
    fun removeObserver(engineId: String) {
        modeChangeCallbacks.remove(engineId)
    }

    private fun notifyCallbacks(engineId: String, mode: EngineMode) {
        modeChangeCallbacks[engineId]?.forEach { callback ->
            try { callback(mode) } catch (e: Exception) {
                SystemLogger.e(TAG, "Callback error for engine: $engineId", e)
            }
        }
    }

    // ─── BATCH OPERATIONS ─────────────────────────────────────────────────────

    /**
     * Called when the app goes to background (onStop at Application level).
     * Transitions all ACTIVE engines to HEARTBEAT.
     */
    fun onAppBackgrounded() {
        engineStates.keys.toList().forEach { id ->
            if (engineStates[id] == EngineMode.ACTIVE) heartbeat(id)
        }
        SystemLogger.i(TAG, "App backgrounded — all active engines → HEARTBEAT")
    }

    /**
     * Called when the app returns to foreground.
     * Engines that were in HEARTBEAT mode are promoted back to ACTIVE.
     */
    fun onAppForegrounded() {
        engineStates.keys.toList().forEach { id ->
            if (engineStates[id] == EngineMode.HEARTBEAT) activate(id)
        }
        SystemLogger.i(TAG, "App foregrounded — all heartbeat engines → ACTIVE")
    }
}
