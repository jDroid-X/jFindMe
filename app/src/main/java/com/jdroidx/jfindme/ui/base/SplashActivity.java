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
        
        ImageView logoNode = new ImageView(this);
        
        // Inject physical resource safely matching copied asset naming
        int resId = getResources().getIdentifier("logo_m2g", "drawable", getPackageName());
        if (resId != 0) {
            logoNode.setImageResource(resId);
        } else {
            // Fallback purely for IDE inspection logic tests
            logoNode.setImageResource(android.R.drawable.ic_dialog_map);
        }
        
        FrameLayout.LayoutParams logoParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        logoParams.gravity = Gravity.CENTER;
        
        // Standard geometric constraint maintaining aspect payload without stretching
        logoNode.setPadding(80, 80, 80, 80);
        logoNode.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        root.addView(logoNode, logoParams);
        setContentView(root);

        // UI Feedback Glide
        // Generates a smooth breathing pop animation scaling from 0.8x out to 1.0x dynamically
        ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                logoNode,
                PropertyValuesHolder.ofFloat("scaleX", 0.7f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 0.7f, 1f)
        );
        scaleDown.setDuration(1200); // 1.2 physical seconds
        scaleDown.start();

        // Android Main Thread Handler locking the Splash UI natively
        getWindow().getDecorView().postDelayed(() -> {
            
            // 🧠 QA Fix: Explicitly check Cloud Authentication State BEFORE routing to AuthActivity!
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            
            if (user != null) {
                // User is fully authenticated remotely. Check their local Hardware routing path!
                boolean isReturningVeteran = getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE).getBoolean("profile_onboarded", false);
                
                if (isReturningVeteran) {
                    startActivity(new Intent(SplashActivity.this, com.jdroidx.jfindme.ui.map.MapActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, com.jdroidx.jfindme.ui.settings.ProfileActivity.class));
                }
            } else {
                // Spin up the physical Auth Engine
                startActivity(new Intent(SplashActivity.this, AuthActivity.class));
            }
            
            finish();
            
            // Push native transition animations smoothing the UI hop
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 2200); // Wait 2.2 Seconds
    }
}
