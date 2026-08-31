package com.jdroidx.jfindme.engine.alert

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.jdroidx.jfindme.ui.alert.AcousticSirenActivity
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * AcousticBeaconEngine: Handles acoustic sirens and remote ringing with DND bypass.
 * Rings device at maximum volume on AudioManager.STREAM_ALARM even on silent mode.
 */
object AcousticBeaconEngine {

    private const val TAG = "AcousticBeaconEngine"
    private var mediaPlayer: MediaPlayer? = null
    private var originalVolume: Int = -1
    private var isSirenActive = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoStopRunnable: Runnable? = null

    @Synchronized
    @JvmStatic
    fun isRinging(): Boolean = isSirenActive

    /**
     * Trigger Acoustic Siren on this device.
     * @param context Application/Service context
     * @param durationSec Duration in seconds before auto-silence (default 30s)
     * @param initiatorName Name of guardian who triggered the siren
     */
    @Synchronized
    @JvmStatic
    fun startSiren(context: Context, durationSec: Int = 30, initiatorName: String = "Guardian") {
        if (isSirenActive) {
            SystemLogger.d(TAG, "Siren is already active. Refreshing timer.")
            autoStopRunnable?.let { mainHandler.removeCallbacks(it) }
            scheduleAutoStop(context, durationSec)
            return
        }

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

            // Force max volume on Alarm Stream (bypasses silent/vibrate mode)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            // Get default alarm sound or fallback to ringtone
            var alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            isSirenActive = true
            SystemLogger.i(TAG, "🔊 Acoustic Siren ACTIVATED at 100% volume for ${durationSec}s by $initiatorName")

            // Start vibration pulse
            triggerContinuousVibration(context)

            // Launch full-screen interactive overlay
            val sirenIntent = Intent(context, AcousticSirenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("INITIATOR_NAME", initiatorName)
                putExtra("DURATION_SEC", durationSec)
            }
            context.startActivity(sirenIntent)

            scheduleAutoStop(context, durationSec)

        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed to activate Acoustic Siren", e)
            stopSiren(context)
        }
    }

    private fun scheduleAutoStop(context: Context, durationSec: Int) {
        val runnable = Runnable {
            SystemLogger.i(TAG, "Acoustic Siren auto-timeout reached (${durationSec}s). Silencing.")
            stopSiren(context)
        }
        autoStopRunnable = runnable
        mainHandler.postDelayed(runnable, durationSec * 1000L)
    }

    /**
     * Stop and silence the acoustic siren.
     */
    @Synchronized
    @JvmStatic
    fun stopSiren(context: Context) {
        if (!isSirenActive) return

        try {
            autoStopRunnable?.let {
                mainHandler.removeCallbacks(it)
                autoStopRunnable = null
            }

            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null

            // Restore original volume if available
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (originalVolume >= 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
                originalVolume = -1
            }

            // Stop vibration
            stopVibration(context)

            isSirenActive = false
            SystemLogger.i(TAG, "🔇 Acoustic Siren STOPPED.")

        } catch (e: Exception) {
            SystemLogger.e(TAG, "Error stopping siren", e)
        }
    }

    private fun triggerContinuousVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 800, 400, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 means repeat
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            SystemLogger.w(TAG, "Could not trigger vibration: ${e.message}")
        }
    }

    private fun stopVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.cancel()
        } catch (e: Exception) { }
    }
}
