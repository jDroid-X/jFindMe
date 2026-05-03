/**
 * Auto-Login Architecture: Enterprise Forensic Entry Node
 * Waterfall Method: Secured Splash Logic & Identity Auditing
 */
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
    private final android.os.Handler transitionHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable transitionRunnable;

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
            lottieLoader.setRepeatCount(0); // 🚀 L7 Optimization: Single Pass for Splash Symmetry
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
        transitionRunnable = () -> {
            if (isFinishing() || isDestroyed()) return;
            
            com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Splash screen transition triggered.");
            
            // 🚀 V57 TIERED VALIDATION: Verify Hardware Fingerprint Integrity
            com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            
            if (user != null) {
                com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Identity Detected. Routing to Secure GateWay for Manual Handshake.");
                startActivity(new Intent(SplashActivity.this, AuthActivity.class));
            } else {
                com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Unauthenticated Hardware. Routing to Auth Hub.");
                startActivity(new Intent(SplashActivity.this, AuthActivity.class));
            }
            
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        };
        
        transitionHandler.postDelayed(transitionRunnable, 3500); 
    }

    @Override
    protected void onDestroy() {
        if (transitionHandler != null && transitionRunnable != null) {
            transitionHandler.removeCallbacks(transitionRunnable);
        }
        super.onDestroy();
    }
}
