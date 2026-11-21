package com.jdroidx.jfindme.data.remote

import com.google.android.gms.tasks.Task
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.utils.SystemLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * 🛰️ FIREBASE ATOMIC GATEWAY (OOPS-based Master Cloud Protocol & Concurrency Abstraction)
 * ======================================================================================
 * High-reliability gateway governing all mutations and real-time streams to Firebase RTDB.
 * Eliminates GAP-04 (Whole-Object Cloud Write Race Condition) by enforcing atomic map-based
 * updateChildren operations instead of destructive whole-tree .setValue() calls.
 *
 * Additionally resolves GAP-07 (Unbounded Real-Time Listener Accumulation) by tracking
 * active listeners in a thread-safe registry and offering deterministic unbinding.
 *
 * Implements Prime Directive 4: Dynamic OOPS-based MVC standard, call-by-reference.
 *
 * [Variables] activeValueListeners, activeChildListeners, listenerRefs, defaultDatabase
 * [Constants] TAG = "FirebaseAtomicGateway", MAX_BATCH_RETRIES = 3, BACKOFF_BASE_MS = 250L
 * [Dependencies] FirebaseDatabase, DatabaseReference, ValueEventListener, ChildEventListener, Task, SystemLogger
 * [Integration Points] FirebaseComm, TrackingService, GroupViewModel, GeofenceViewModel, MapViewModel
 */
object FirebaseAtomicGateway {

    private const val TAG = "FirebaseAtomicGateway"

    // Thread-safe listener registries for auto-cleanup and lifecycle governance
    private val activeValueListeners = ConcurrentHashMap<String, ValueEventListener>()
    private val activeChildListeners = ConcurrentHashMap<String, ChildEventListener>()
    private val listenerRefs = ConcurrentHashMap<String, DatabaseReference>()

    private val dbInstance: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    fun interface AtomicCompletionListener {
        fun onComplete(success: Boolean, errorMsg: String?)
    }

    /**
     * Executes atomic multi-path update across one or more nested Firebase database nodes.
     * Prevents race conditions where concurrent clients clobber adjacent sibling fields.
     *
     * @param rootRef The root DatabaseReference or base path
     * @param updates Flat path-to-value map (e.g., "devices/DEV1/status" -> "ONLINE")
     * @param callback Optional completion listener
     */
    @JvmStatic
    fun updateChildrenAtomic(
        rootRef: DatabaseReference,
        updates: Map<String, Any?>,
        callback: AtomicCompletionListener? = null
    ) {
        if (updates.isEmpty()) {
            callback?.onComplete(true, null)
            return
        }

        rootRef.updateChildren(updates)
            .addOnSuccessListener {
                SystemLogger.d(TAG, "Atomic update succeeded on [${rootRef.key}] with ${updates.size} fields.")
                callback?.onComplete(true, null)
            }
            .addOnFailureListener { e ->
                SystemLogger.e(TAG, "Atomic update failed on [${rootRef.key}]: ${e.message}")
                callback?.onComplete(false, e.message)
            }
    }

    /**
     * Atomically mutates a single leaf node value while preserving sibling state.
     */
    @JvmStatic
    fun updateLeafAtomic(
        ref: DatabaseReference,
        key: String,
        value: Any?,
        callback: AtomicCompletionListener? = null
    ) {
        val payload = mapOf(key to value)
        updateChildrenAtomic(ref, payload, callback)
    }

    /**
     * Registers a managed, tag-bound ValueEventListener that can be deterministically evicted.
     */
    @Synchronized
    @JvmStatic
    fun registerManagedValueListener(
        tag: String,
        ref: DatabaseReference,
        listener: ValueEventListener
    ) {
        // Unbind any previous listener holding this tag to prevent socket stacking
        unregisterListener(tag)

        activeValueListeners[tag] = listener
        listenerRefs[tag] = ref
        ref.addValueEventListener(listener)
        SystemLogger.d(TAG, "📡 Registered managed ValueEventListener [tag: $tag] on [${ref.key}]")
    }

    /**
     * Registers a managed, tag-bound ChildEventListener that can be deterministically evicted.
     */
    @Synchronized
    @JvmStatic
    fun registerManagedChildListener(
        tag: String,
        ref: DatabaseReference,
        listener: ChildEventListener
    ) {
        unregisterListener(tag)

        activeChildListeners[tag] = listener
        listenerRefs[tag] = ref
        ref.addChildEventListener(listener)
        SystemLogger.d(TAG, "📡 Registered managed ChildEventListener [tag: $tag] on [${ref.key}]")
    }

    /**
     * Unregisters and detaches any active listener bound to the given tag.
     */
    @Synchronized
    @JvmStatic
    fun unregisterListener(tag: String) {
        val ref = listenerRefs.remove(tag)

        val valueListener = activeValueListeners.remove(tag)
        if (ref != null && valueListener != null) {
            try {
                ref.removeEventListener(valueListener)
                SystemLogger.d(TAG, "🛑 Detached ValueEventListener [tag: $tag]")
            } catch (e: Exception) {
                SystemLogger.w(TAG, "Safe catch detaching ValueEventListener [$tag]: ${e.message}")
            }
        }

        val childListener = activeChildListeners.remove(tag)
        if (ref != null && childListener != null) {
            try {
                ref.removeEventListener(childListener)
                SystemLogger.d(TAG, "🛑 Detached ChildEventListener [tag: $tag]")
            } catch (e: Exception) {
                SystemLogger.w(TAG, "Safe catch detaching ChildEventListener [$tag]: ${e.message}")
            }
        }
    }

    /**
     * Diagnostic: Returns count of currently active cloud real-time listeners.
     */
    @JvmStatic
    fun getActiveListenerCount(): Int = activeValueListeners.size + activeChildListeners.size

    /**
     * Clean shutdown: Detaches all registered listeners across the application.
     */
    @Synchronized
    @JvmStatic
    fun unregisterAllListeners() {
        val allTags = HashSet<String>().apply {
            addAll(activeValueListeners.keys)
            addAll(activeChildListeners.keys)
        }
        for (tag in allTags) {
            unregisterListener(tag)
        }
        SystemLogger.i(TAG, "All managed Firebase RTDB listeners cleanly detached.")
    }
}
