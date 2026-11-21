package com.jdroidx.jfindme.engine.power

import android.content.Context
import android.os.PowerManager
import com.jdroidx.jfindme.utils.SystemLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * ⚡ KERNEL WAKELOCK GATEWAY (OOPS-based Hardware Power Abstraction)
 * ================================================================
 * Centralized, leak-proof PowerManager.WakeLock governor.
 * Eliminates scattered raw WakeLock instantiations, ensures deterministic timeout
 * ceilings, prevents battery exhaustion on Xiaomi/Redmi silicon, and guards
 * against fatal "WakeLock underflow" exceptions during OS deep doze cycles.
 *
 * Implements Prime Directive 4: Dynamic OOPS-based MVC standard, call-by-reference.
 *
 * [Variables] appContext, powerManager, activeLocksMap, lockMutex
 * [Constants] DEFAULT_BURST_TIMEOUT_MS = 10000L, MAX_ALLOWABLE_TIMEOUT_MS = 30000L,
 *             TAG_PREFIX = "jFindMe::KernelLock::"
 * [Dependencies] android.os.PowerManager, PowerManager.WakeLock, SystemLogger
 * [Integration Points] TrackingService, FCMService, StealthAlarmSentinelEngine, WatchdogAlarmReceiver
 */
object KernelWakeLockGateway {

    private const val TAG = "KernelWakeLockGateway"
    private const val TAG_PREFIX = "jFindMe::KernelLock::"
    const val DEFAULT_BURST_TIMEOUT_MS = 10000L
    const val MAX_ALLOWABLE_TIMEOUT_MS = 30000L

    // Thread-safe map tracking active locks by tag to prevent duplicate multi-allocations
    private val activeLocks = ConcurrentHashMap<String, PowerManager.WakeLock>()

    /**
     * Executes the given high-priority block under a hardware CPU WakeLock.
     * Guarantees safe, deterministic release in a finally block even if an exception occurs.
     */
    @JvmStatic
    inline fun <T> withWakeLock(
        context: Context,
        tag: String,
        timeoutMs: Long = DEFAULT_BURST_TIMEOUT_MS,
        block: () -> T
    ): T {
        val lock = acquireBurstWakeLock(context, tag, timeoutMs)
        return try {
            block()
        } finally {
            releaseWakeLock(tag)
        }
    }

    /**
     * Safely acquires a timed partial CPU WakeLock with an enforced ceiling.
     */
    @Synchronized
    @JvmStatic
    fun acquireBurstWakeLock(
        context: Context,
        tag: String,
        timeoutMs: Long = DEFAULT_BURST_TIMEOUT_MS
    ): PowerManager.WakeLock? {
        val fullTag = if (tag.startsWith(TAG_PREFIX)) tag else TAG_PREFIX + tag
        val safeTimeout = timeoutMs.coerceIn(1000L, MAX_ALLOWABLE_TIMEOUT_MS)

        try {
            // Check if existing lock is held under this tag
            val existing = activeLocks[fullTag]
            if (existing != null && existing.isHeld) {
                SystemLogger.d(TAG, "WakeLock already active for tag: $fullTag. Re-arming timeout: ${safeTimeout}ms")
                existing.acquire(safeTimeout)
                return existing
            }

            val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm == null) {
                SystemLogger.w(TAG, "PowerManager unavailable; cannot acquire WakeLock for $fullTag")
                return null
            }

            val newLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, fullTag).apply {
                setReferenceCounted(false)
                acquire(safeTimeout)
            }

            activeLocks[fullTag] = newLock
            SystemLogger.d(TAG, "⚡ Acquired timed WakeLock [$fullTag] for ${safeTimeout}ms")
            return newLock

        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed to acquire WakeLock for $fullTag: ${e.message}")
            return null
        }
    }

    /**
     * Safely releases a WakeLock by tag, guarding against underflow exceptions.
     */
    @Synchronized
    @JvmStatic
    fun releaseWakeLock(tag: String) {
        val fullTag = if (tag.startsWith(TAG_PREFIX)) tag else TAG_PREFIX + tag
        try {
            val lock = activeLocks.remove(fullTag)
            if (lock != null && lock.isHeld) {
                lock.release()
                SystemLogger.d(TAG, "🛑 Released WakeLock [$fullTag]")
            }
        } catch (e: RuntimeException) {
            SystemLogger.w(TAG, "Safe catch on WakeLock release [$fullTag]: ${e.message}")
        }
    }

    /**
     * Diagnostic: Returns count of currently registered active hardware locks.
     */
    @JvmStatic
    fun getActiveLockCount(): Int = activeLocks.size

    /**
     * Clean shutdown: Releases all remaining active locks.
     */
    @Synchronized
    @JvmStatic
    fun releaseAll() {
        for ((tag, lock) in activeLocks) {
            try {
                if (lock.isHeld) lock.release()
            } catch (ignored: Exception) {}
        }
        activeLocks.clear()
        SystemLogger.i(TAG, "All active kernel WakeLocks cleanly deallocated.")
    }
}
