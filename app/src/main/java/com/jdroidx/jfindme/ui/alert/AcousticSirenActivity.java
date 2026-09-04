package com.jdroidx.jfindme.ui.alert;

import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.jdroidx.jfindme.engine.alert.AcousticBeaconEngine;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * AcousticSirenActivity: Full-screen emergency alarm screen triggered remotely by a guardian.
 * Wakes up device, bypasses lockscreen, and allows one-tap dismissal.
 */
public class AcousticSirenActivity extends AppCompatActivity {

    private static final String TAG = "AcousticSirenActivity";
    private CountDownTimer countDownTimer;
    private TextView tvCountdown;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Turn screen on and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String initiator = getIntent().getStringExtra("INITIATOR_NAME");
        if (initiator == null || initiator.isEmpty()) initiator = "Family Guardian";
        int durationSec = getIntent().getIntExtra("DURATION_SEC", 30);

        setContentView(buildLayout(initiator, durationSec));
    }

    private View buildLayout(String initiator, int durationSec) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#0F172A")); // Deep Slate / Emergency Night
        root.setPadding(48, 48, 48, 48);

        // Emergency Flashing Icon Card
        LinearLayout iconCard = new LinearLayout(this);
        iconCard.setOrientation(LinearLayout.VERTICAL);
        iconCard.setGravity(Gravity.CENTER);
        int cardPad = 32;
        iconCard.setPadding(cardPad, cardPad, cardPad, cardPad);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.OVAL);
        cardBg.setColor(Color.parseColor("#DC2626")); // Crimson Red
        cardBg.setStroke(8, Color.parseColor("#FCA5A5"));
        iconCard.setBackground(cardBg);

        ImageView sirenIcon = new ImageView(this);
        sirenIcon.setImageResource(android.R.drawable.ic_lock_idle_alarm);
        sirenIcon.setColorFilter(Color.WHITE);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(140, 140);
        iconCard.addView(sirenIcon, iconParams);

        root.addView(iconCard);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("ACOUSTIC SIREN ACTIVE");
        tvTitle.setTextColor(Color.parseColor("#F87171"));
        tvTitle.setTextSize(22f);
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = 36;
        root.addView(tvTitle, titleParams);

        // Subtitle / Initiator
        TextView tvSub = new TextView(this);
        tvSub.setText("Remotely triggered by " + initiator + "\nPlaying at 100% volume to locate device");
        tvSub.setTextColor(Color.parseColor("#CBD5E1"));
        tvSub.setTextSize(14f);
        tvSub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subParams.topMargin = 16;
        root.addView(tvSub, subParams);

        // Countdown Timer View
        tvCountdown = new TextView(this);
        tvCountdown.setText("Auto-silencing in " + durationSec + "s");
        tvCountdown.setTextColor(Color.parseColor("#94A3B8"));
        tvCountdown.setTextSize(13f);
        tvCountdown.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams cdParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cdParams.topMargin = 24;
        root.addView(tvCountdown, cdParams);

        // Start Countdown Timer
        countDownTimer = new CountDownTimer(durationSec * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (tvCountdown != null) {
                    tvCountdown.setText("Auto-silencing in " + (millisUntilFinished / 1000) + "s");
                }
            }

            @Override
            public void onFinish() {
                finishAndDismiss();
            }
        }.start();

        // 1-Tap Dismiss Card (No standard Button widget per UI rules)
        LinearLayout btnDismiss = new LinearLayout(this);
        btnDismiss.setOrientation(LinearLayout.HORIZONTAL);
        btnDismiss.setGravity(Gravity.CENTER);
        btnDismiss.setPadding(32, 28, 32, 28);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(24f);
        btnBg.setColor(Color.parseColor("#22C55E")); // Emerald Green
        btnDismiss.setBackground(btnBg);

        ImageView checkIcon = new ImageView(this);
        checkIcon.setImageResource(android.R.drawable.checkbox_on_background);
        checkIcon.setColorFilter(Color.WHITE);
        btnDismiss.addView(checkIcon, new LinearLayout.LayoutParams(48, 48));

        TextView tvDismiss = new TextView(this);
        tvDismiss.setText("  SILENCE & DISMISS");
        tvDismiss.setTextColor(Color.WHITE);
        tvDismiss.setTextSize(16f);
        tvDismiss.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btnDismiss.addView(tvDismiss);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnParams.topMargin = 48;
        btnParams.leftMargin = 32;
        btnParams.rightMargin = 32;
        btnDismiss.setLayoutParams(btnParams);

        btnDismiss.setOnClickListener(v -> finishAndDismiss());

        root.addView(btnDismiss);

        return root;
    }

    private void finishAndDismiss() {
        SystemLogger.i(TAG, "Dismissing acoustic siren from UI.");
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        AcousticBeaconEngine.stopSiren(this);
        AcousticBeaconEngine.stopVibration(this);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        AcousticBeaconEngine.stopSiren(this);
        AcousticBeaconEngine.stopVibration(this);
        super.onDestroy();
    }
}
