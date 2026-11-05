package com.jdroidx.jfindme.engine.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.*
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * 🏃 ActivityRecognitionSentinel: Hardware Motion Co-Processor Engine.
 *
 * Uses Android's low-power hardware activity recognition to detect physical movement:
 * - STILL (Stationary on desk/pocket) -> Powers down GPS chip (0 mA satellite drain).
 * - IN_VEHICLE (Driving on roads) -> Spins up High-Accuracy road tracking (10-15s).
 * - WALKING / BICYCLE -> Moderate tracking duty cycle (20-30s).
 */
object ActivityRecognitionSentinel {

    private const val TAG = "ActivityRecognitionSentinel"
    private const val REQUEST_CODE = 2002

    enum class MotionState {
        STILL,
        IN_VEHICLE,
        ON_BICYCLE,
        WALKING,
        RUNNING,
        UNKNOWN
    }

    private var currentMotionState = MotionState.UNKNOWN
    private val stateChangeListeners = mutableListOf<(MotionState) -> Unit>()
    private var isTrackingActive = false

    fun getMotionState(): MotionState = currentMotionState

    fun addListener(listener: (MotionState) -> Unit) {
        if (!stateChangeListeners.contains(listener)) {
            stateChangeListeners.add(listener)
            listener(currentMotionState)
        }
    }

    fun removeListener(listener: (MotionState) -> Unit) {
        stateChangeListeners.remove(listener)
    }

    @SuppressLint("MissingPermission")
    fun startMonitoring(context: Context) {
        if (isTrackingActive) return

        val client = ActivityRecognition.getClient(context)
        val pendingIntent = getPendingIntent(context)

        try {
            val transitions = listOf(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_BICYCLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )

            val request = ActivityTransitionRequest(transitions)
            client.requestActivityTransitionUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    isTrackingActive = true
                    SystemLogger.i(TAG, "Hardware Activity Recognition Transitions Active.")
                }
                .addOnFailureListener {
                    // Fallback to periodic activity updates (30s)
                    client.requestActivityUpdates(30000L, pendingIntent)
                        .addOnSuccessListener {
                            isTrackingActive = true
                            SystemLogger.i(TAG, "Hardware Activity Recognition Periodic Updates Active (Fallback).")
                        }
                        .addOnFailureListener { err ->
                            SystemLogger.w(TAG, "ActivityRecognition unavailable on this device: ${err.message}")
                        }
                }
        } catch (e: Exception) {
            SystemLogger.w(TAG, "Failed to start ActivityRecognition: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopMonitoring(context: Context) {
        if (!isTrackingActive) return
        try {
            val client = ActivityRecognition.getClient(context)
            val pendingIntent = getPendingIntent(context)
            client.removeActivityTransitionUpdates(pendingIntent)
            client.removeActivityUpdates(pendingIntent)
            isTrackingActive = false
            SystemLogger.i(TAG, "ActivityRecognition stopped.")
        } catch (e: Exception) {
            SystemLogger.w(TAG, "Error stopping ActivityRecognition: ${e.message}")
        }
    }

    internal fun onActivityTransitionReceived(state: MotionState) {
        if (currentMotionState == state) return
        val previous = currentMotionState
        currentMotionState = state
        SystemLogger.i(TAG, "🏃 Motion Transition: $previous -> $state")
        stateChangeListeners.forEach { listener ->
            try { listener(state) } catch (e: Exception) { SystemLogger.e(TAG, "Listener error", e) }
        }
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ActivityTransitionReceiver::class.java).apply {
            action = "com.jdroidx.jfindme.ACTION_PROCESS_ACTIVITY_TRANSITIONS"
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    /**
     * BroadcastReceiver to handle OS motion callbacks.
     */
    class ActivityTransitionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null) return

            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent)
                result?.transitionEvents?.forEach { event ->
                    val state = when (event.activityType) {
                        DetectedActivity.STILL -> MotionState.STILL
                        DetectedActivity.IN_VEHICLE -> MotionState.IN_VEHICLE
                        DetectedActivity.ON_BICYCLE -> MotionState.ON_BICYCLE
                        DetectedActivity.WALKING -> MotionState.WALKING
                        DetectedActivity.RUNNING -> MotionState.RUNNING
                        else -> MotionState.UNKNOWN
                    }
                    if (state != MotionState.UNKNOWN) {
                        onActivityTransitionReceived(state)
                    }
                }
            } else if (ActivityRecognitionResult.hasResult(intent)) {
                val result = ActivityRecognitionResult.extractResult(intent)
                val mostProbable = result?.mostProbableActivity
                if (mostProbable != null && mostProbable.confidence >= 60) {
                    val state = when (mostProbable.type) {
                        DetectedActivity.STILL -> MotionState.STILL
                        DetectedActivity.IN_VEHICLE -> MotionState.IN_VEHICLE
                        DetectedActivity.ON_BICYCLE -> MotionState.ON_BICYCLE
                        DetectedActivity.WALKING -> MotionState.WALKING
                        DetectedActivity.RUNNING -> MotionState.RUNNING
                        else -> MotionState.UNKNOWN
                    }
                    if (state != MotionState.UNKNOWN) {
                        onActivityTransitionReceived(state)
                    }
                }
            }
        }
    }
}
