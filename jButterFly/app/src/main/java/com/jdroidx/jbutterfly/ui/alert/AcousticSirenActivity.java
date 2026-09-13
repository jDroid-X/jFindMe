package com.jdroidx.jbutterfly.ui.alert;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.jdroidx.jbutterfly.R;
import com.jdroidx.jbutterfly.utils.SystemLogger;

public class AcousticSirenActivity extends Activity {

    private static final String TAG = "AcousticSirenActivity";
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Turn screen on and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            @SuppressWarnings("deprecation")
            int legacyFlags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                              WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                              WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            getWindow().addFlags(legacyFlags);
        }

        String senderId = getIntent().getStringExtra("SENDER_ID");
        if (senderId == null || senderId.isEmpty()) senderId = "COMMAND_GUARDIAN";

        buildUi(senderId);
        startAlarmAndVibration();
    }

    private void buildUi(String senderId) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#7F1D1D")); // Emergency Crimson
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.butterfly);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(240, 240);
        iconParams.bottomMargin = 32;
        root.addView(icon, iconParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("EMERGENCY ACOUSTIC BUZZ");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(22f);
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        root.addView(tvTitle);

        TextView tvSender = new TextView(this);
        tvSender.setText("Triggered by Sentinel: " + senderId + "\nLocating this device now...");
        tvSender.setTextColor(Color.parseColor("#FECACA"));
        tvSender.setTextSize(14f);
        tvSender.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams senderParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        senderParams.topMargin = 16;
        senderParams.bottomMargin = 48;
        root.addView(tvSender, senderParams);

        Button btnStop = new Button(this);
        btnStop.setText("SILENCE SIREN");
        btnStop.setTextColor(Color.parseColor("#7F1D1D"));
        btnStop.setBackgroundColor(Color.WHITE);
        btnStop.setPadding(48, 24, 48, 24);
        btnStop.setTextSize(16f);
        btnStop.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btnStop.setOnClickListener(v -> finish());

        root.addView(btnStop);
        setContentView(root);
    }

    private void startAlarmAndVibration() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            );
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.os.VibratorManager vm = (android.os.VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    vibrator = vm.getDefaultVibrator();
                }
            }
            if (vibrator == null) {
                @SuppressWarnings("deprecation")
                Vibrator legacyVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator = legacyVibrator;
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 600, 200, 600, 200, 800};
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            }
        } catch (Exception e) {
            SystemLogger.e(TAG, "Error initiating acoustic alarm", e);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            if (vibrator != null) {
                vibrator.cancel();
            }
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}
