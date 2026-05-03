package com.jdroidx.jfindme.utils;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

/**
 * 🦋 Premium Enterprise UX: The Guardian Angel Loading Matrix
 * Subliminally calms the user during network bottlenecks replacing the stressful generic spinning wheel.
 * Renders Lottie Vectors dynamically. If the raw JSON isn't available, gracefully falls back to OS defaults.
 */
public class AngelLoader {
    private static final String TAG_NODE = "ANGEL_LOTTIE_NODE";
    private static final String TAG_TEXT = "ANGEL_SCRIPT_TEXT";

    /**
     * Injects a perfectly centered, touch-blocking transparent dim layer onto the entire Activity.
     * Fires the beautiful Butterfly animation if the file exists.
     */
    public static void show(Activity context) {
        if (context == null || context.isFinishing() || context.isDestroyed()) return;
        
        ViewGroup root = (ViewGroup) context.getWindow().getDecorView().findViewById(android.R.id.content);
        if (root.findViewWithTag(TAG_NODE) != null) return; // Prevent duplicate instantiation!
        
        FrameLayout overlay = new FrameLayout(context);
        overlay.setTag(TAG_NODE);
        overlay.setBackgroundColor(Color.parseColor("#AA000000")); // Deep 66% protective tint shadowing background data
        overlay.setClickable(true); // Prevents users from mashing buttons while tasks render
        overlay.setFocusable(true);
        
        // Accessibility Support (Screen Readers announce loading to visually impaired)
        overlay.setContentDescription("Loading sequence Active. Please hold.");
        
        // 🚀 QA Responsive Scaling & Centering: Screen size conversion
        int boxSize = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
        
        // Formulate a Unified Content Wrapper! This guarantees the Text and Loader are always perfectly centered as a Group.
        android.widget.LinearLayout contentGroup = new android.widget.LinearLayout(context);
        contentGroup.setOrientation(android.widget.LinearLayout.VERTICAL);
        contentGroup.setGravity(Gravity.CENTER);
        
        FrameLayout.LayoutParams groupLp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        groupLp.gravity = Gravity.CENTER; // 🚀 Mathematically Center the entire block into the exact middle of the physical screen
        contentGroup.setLayoutParams(groupLp);

        // Dynamically request the Lottie Vector (Zero Crash Safety Architecture)
        int rawId = context.getResources().getIdentifier("butterfly_angel", "raw", context.getPackageName());
        
        if (rawId != 0) {
            LottieAnimationView lottie = new LottieAnimationView(context);
            lottie.setAnimation(rawId);
            lottie.setRepeatCount(LottieDrawable.INFINITE);
            lottie.playAnimation(); // Start the waving vector loop smoothly!
            
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(boxSize, boxSize);
            lp.gravity = Gravity.CENTER;
            contentGroup.addView(lottie, lp);
        } else {
            // Elegant OS Fallback in case JSON compilation was forgotten by the developer
            ProgressBar fallback = new ProgressBar(context);
            fallback.getIndeterminateDrawable().setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
            
            int fallbackSize = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics());
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(fallbackSize, fallbackSize);
            lp.gravity = Gravity.CENTER;
            contentGroup.addView(fallback, lp);
        }
        
        // 🚀 QA Fix: Enterprise Script CardView layered exactly below the Loader
        android.widget.LinearLayout statusCard = new android.widget.LinearLayout(context);
        statusCard.setOrientation(android.widget.LinearLayout.VERTICAL);
        
        int padH = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 20, context.getResources().getDisplayMetrics());
        int padV = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 10, context.getResources().getDisplayMetrics());
        statusCard.setPadding(padH, padV, padH, padV);
        statusCard.setGravity(Gravity.CENTER);
        
        android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
        cardBg.setColor(Color.parseColor("#80000000")); // Sharp translucent glass finish
        cardBg.setStroke(2, Color.parseColor("#40FFFFFF"));
        cardBg.setCornerRadius(30f);
        statusCard.setBackground(cardBg);
        
        android.widget.TextView scriptText = new android.widget.TextView(context);
        scriptText.setTag(TAG_TEXT);
        scriptText.setText("Securing Cloud Matrix...");
        scriptText.setTextColor(Color.WHITE);
        scriptText.setTextSize(14f);
        scriptText.setTypeface(null, android.graphics.Typeface.BOLD);
        scriptText.setGravity(Gravity.CENTER);
        statusCard.addView(scriptText);
        
        android.widget.LinearLayout.LayoutParams statusLp = new android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.gravity = Gravity.CENTER;
        statusLp.topMargin = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 15, context.getResources().getDisplayMetrics()); // Graceful buffer
        contentGroup.addView(statusCard, statusLp);

        // 🚀 V53 Telemetry: Version Matrix display below butterfly
        android.widget.TextView verText = new android.widget.TextView(context);
        verText.setText("Db: 53 | L7: v1.0.8 | Core: 2.4.1 | UI: M3");
        verText.setTextColor(Color.parseColor("#80FFFFFF"));
        verText.setTextSize(9f);
        verText.setGravity(Gravity.CENTER);
        android.widget.LinearLayout.LayoutParams verLp = new android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        verLp.topMargin = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
        contentGroup.addView(verText, verLp);
        
        overlay.addView(contentGroup);
        
        // Animate overlay Alpha exactly like a premium movie fade
        overlay.setAlpha(0f);
        root.addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.animate().alpha(1f).setDuration(300).start();
    }
    
    /**
     * Instantly dismantles the animation engine safely and natively.
     */
    public static void hide(Activity context) {
        if (context == null || context.isFinishing() || context.isDestroyed()) return;
        
        ViewGroup root = (ViewGroup) context.getWindow().getDecorView().findViewById(android.R.id.content);
        View cacheRemove = root.findViewWithTag(TAG_NODE);
        
        if (cacheRemove != null) {
            cacheRemove.animate().alpha(0f).setDuration(250).withEndAction(() -> {
                if (cacheRemove.getParent() != null) {
                    ((ViewGroup) cacheRemove.getParent()).removeView(cacheRemove);
                }
            }).start();
        }
    }
    
    /**
     * 🚀 QA Upgrade: Dynamically updates the script stream rendered directly beneath the Animated Object.
     */
    public static void setStatus(Activity context, String statusMessage) {
        if (context == null || context.isFinishing() || context.isDestroyed()) return;
        
        ViewGroup root = (ViewGroup) context.getWindow().getDecorView().findViewById(android.R.id.content);
        View rawText = root.findViewWithTag(TAG_TEXT);
        if (rawText instanceof android.widget.TextView) {
            ((android.widget.TextView) rawText).setText(statusMessage);
        }
    }
}
