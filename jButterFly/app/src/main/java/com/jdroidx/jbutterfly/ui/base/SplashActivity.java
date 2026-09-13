package com.jdroidx.jbutterfly.ui.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.RenderMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.jdroidx.jbutterfly.R;
import com.jdroidx.jbutterfly.ui.auth.AuthActivity;
import com.jdroidx.jbutterfly.ui.map.MapActivity;
import com.jdroidx.jbutterfly.utils.DeviceIdentityHelper;
import com.jdroidx.jbutterfly.utils.RegistryManager;
import com.jdroidx.jbutterfly.utils.SystemLogger;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends Activity {

    private static final String TAG = "SplashActivity";
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        SystemLogger.init(this);
        SystemLogger.i(TAG, "Splash screen initializing...");

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#080C14"));

        // Lottie Vector Wings Hero Animation
        int rawId = getResources().getIdentifier("butterfly_angel", "raw", getPackageName());
        if (rawId != 0) {
            LottieAnimationView lottie = new LottieAnimationView(this);
            lottie.setAnimation(rawId);
            lottie.setRepeatCount(LottieDrawable.INFINITE);
            lottie.setRenderMode(RenderMode.HARDWARE);
            lottie.setSpeed(1.2f);
            lottie.playAnimation();

            int size = UiArchitect.dpToPx(this, 180);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
            lp.gravity = Gravity.CENTER;
            root.addView(lottie, lp);
        }

        // Subtitle tag
        TextView tagLine = new TextView(this);
        tagLine.setText("jButterFly • Enterprise Spatial Radar");
        tagLine.setTextColor(Color.parseColor("#94A3B8"));
        tagLine.setTextSize(UiArchitect.spToPx(this, 12f));
        tagLine.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams tagLp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        tagLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        tagLp.bottomMargin = UiArchitect.dpToPx(this, 40);
        root.addView(tagLine, tagLp);

        setContentView(root);

        // Hardware Identity Resolution in Background Thread
        new Thread(() -> {
            String agentId = DeviceIdentityHelper.getOrGenerateAgentId(SplashActivity.this);
            String hwKey = DeviceIdentityHelper.getRawHardwareKey(SplashActivity.this);
            SystemLogger.i(TAG, "Hardware Key anchored: " + hwKey + " -> Agent ID: " + agentId);
        }).start();

        // Smooth transition timer
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;

            boolean loggedIn = RegistryManager.getBoolean(this, RegistryManager.KEY_IS_LOGGED_IN, false);
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            Intent next;
            if (loggedIn || currentUser != null) {
                next = new Intent(SplashActivity.this, MapActivity.class);
            } else {
                next = new Intent(SplashActivity.this, AuthActivity.class);
            }
            startActivity(next);
            finish();
        }, 1500);
    }
}
