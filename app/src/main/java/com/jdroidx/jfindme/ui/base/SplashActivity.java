package com.jdroidx.jfindme.ui.base;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.jdroidx.jfindme.ui.auth.AuthActivity;
import com.jdroidx.jfindme.ui.map.MapActivity;

/**
 * Enterprise Application Entry Node: Native Custom Splash Screen
 * Securely blocks drawing UI artifacts displaying 'M2G GLOBAL' branding
 * while dynamically fading into the AuthActivity.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends Activity {

    private final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Build Native Fullscreen Canvas Layer
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#0F1A2A")); // Dark sophisticated blue matching the logo theme
        
        // 🦋 Premium Lottie Hero Injection: The Waving Wings Matrix
        int rawId = getResources().getIdentifier("butterfly_angel", "raw", getPackageName());
        if (rawId != 0) {
            com.airbnb.lottie.LottieAnimationView lottieLoader = new com.airbnb.lottie.LottieAnimationView(this);
            lottieLoader.setAnimation(rawId);
            lottieLoader.setRepeatCount(com.airbnb.lottie.LottieDrawable.INFINITE);
            lottieLoader.playAnimation(); // Smooth 60FPS Waving Wings!
            
            // 🚀 MASTER SCALING INTERCEPTOR: Single Source of Truth
            int heroSize = UiArchitect.getResponsiveIconSize(this, "ICON_HERO");
            
            FrameLayout.LayoutParams butterflyParams = new FrameLayout.LayoutParams(heroSize, heroSize);
            butterflyParams.gravity = Gravity.CENTER; 
            root.addView(lottieLoader, butterflyParams);
            
            // Render cinematic breathing pop against the dark canvas
            ObjectAnimator lottiePop = ObjectAnimator.ofPropertyValuesHolder(
                    lottieLoader,
                    android.animation.PropertyValuesHolder.ofFloat("alpha", 0f, 1f),
                    android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.6f, 1.0f),
                    android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.6f, 1.0f)
            );
            lottiePop.setDuration(2200).start();
        }

        setContentView(root);

        // Android Main Thread Handler locking the Splash UI natively
        getWindow().getDecorView().postDelayed(() -> {
            
            com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Splash screen transition triggered.");
            // 🧠 QA Fix: Explicitly check Cloud Authentication State BEFORE routing to AuthActivity!
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            
            // 🚀 Day Before Yesterday: Conditional Dashboard Routing Protocol
            if (user != null) {
                com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Authenticated Node Detected. Bypassing Gateway.");
                startActivity(new Intent(SplashActivity.this, MapActivity.class));
            } else {
                com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Unauthenticated Hardware. Routing to Auth Hub.");
                startActivity(new Intent(SplashActivity.this, AuthActivity.class));
            }
            
            finish();
            
            // Push native transition animations smoothing the UI hop
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 5000); // 🦋 Butterfly Angel Guard: Extended to 5s cinematic bridge
    }
}
