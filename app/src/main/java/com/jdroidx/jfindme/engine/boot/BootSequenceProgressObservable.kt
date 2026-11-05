package com.jdroidx.jfindme.engine.boot

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 🚀 BootSequenceProgressObservable: Enterprise Reactive Observer for Boot Stages.
 * Coordinates 0% -> 100% Boot Progress, Hardware Attestation, Migration & Navigation Routing.
 *
 * [Variables]: listeners, currentProgress, currentPhase, mainHandler
 * [Constants]: PHASE_INIT, PHASE_SILICON_ATTESTATION, PHASE_STATE_MIGRATION, PHASE_CLOUD_SYNC, PHASE_NAVIGATION_READY
 * [Dependencies]: android.os.Handler, android.os.Looper, java.util.concurrent.CopyOnWriteArrayList
 * [Integration Points]: SplashActivity.java, BaseActivityJava.java, EnterpriseUnitTestSuite.kt
 */
object BootSequenceProgressObservable {

    const val PHASE_INIT = "INIT"
    const val PHASE_SILICON_ATTESTATION = "SILICON_ATTESTATION"
    const val PHASE_STATE_MIGRATION = "STATE_MIGRATION"
    const val PHASE_CLOUD_SYNC = "CLOUD_SYNC"
    const val PHASE_NAVIGATION_READY = "NAVIGATION_READY"

    interface BootProgressListener {
        fun onProgressUpdate(phase: String, progressPercent: Int)
        fun onBootCompleted()
    }

    private val listeners = CopyOnWriteArrayList<BootProgressListener>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var currentProgress: Int = 0
        private set

    @Volatile
    var currentPhase: String = PHASE_INIT
        private set

    fun registerListener(listener: BootProgressListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            listener.onProgressUpdate(currentPhase, currentProgress)
        }
    }

    fun unregisterListener(listener: BootProgressListener) {
        listeners.remove(listener)
    }

    fun updateProgress(phase: String, progressPercent: Int) {
        currentPhase = phase
        currentProgress = progressPercent.coerceIn(0, 100)

        mainHandler.post {
            for (l in listeners) {
                l.onProgressUpdate(currentPhase, currentProgress)
            }
            if (currentProgress >= 100) {
                for (l in listeners) {
                    l.onBootCompleted()
                }
            }
        }
    }

    fun reset() {
        currentProgress = 0
        currentPhase = PHASE_INIT
    }
}

