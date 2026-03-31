package com.jdroidx.jfindme.ui.base;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

/**
 * 👑 ANGEL LOAD: Enterprise Progress Transition Node
 * Handles the "Butterfly Angel" lottie sequence across System Stage migrations.
 * Ensures the UX doesn't feel like a raw crash/restart during re-architecting.
 */
public class AngelLoad {

    public static View createLoader(Context context, String message) {
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Color.parseColor("#E60F1A2A")); // Semi-transparent enterprise dark blue
        root.setClickable(true);
        root.setFocusable(true);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);

        int rawId = context.getResources().getIdentifier("butterfly_angel", "raw", context.getPackageName());
        if (rawId != 0) {
            LottieAnimationView lottie = new LottieAnimationView(context);
            lottie.setAnimation(rawId);
            lottie.setRepeatCount(LottieDrawable.INFINITE);
            lottie.playAnimation();

            int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, context.getResources().getDisplayMetrics());
            container.addView(lottie, new LinearLayout.LayoutParams(size, size));

            ObjectAnimator.ofFloat(lottie, "alpha", 0f, 1f).setDuration(800).start();
        }

        if (message != null) {
            TextView tv = new TextView(context);
            tv.setText(message);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(14f);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, 40, 0, 0);
            container.addView(tv);
            
            ObjectAnimator.ofFloat(tv, "alpha", 0f, 1f).setDuration(1200).start();
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        root.addView(container, lp);

        return root;
    }

    public static void engage(BaseActivityJava activity, Runnable onComplete) {
        engage(activity, "RE-INDEXING SYSTEM STAGE...", onComplete);
    }

    public static void engage(BaseActivityJava activity, String statusMessage, Runnable onComplete) {
        ViewGroup root = (ViewGroup) activity.getWindow().getDecorView();
        View loader = createLoader(activity, statusMessage);
        root.addView(loader, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Simulate "Architectural Re-indexing" transition
        activity.getWindow().getDecorView().postDelayed(() -> {
            root.removeView(loader);
            if (onComplete != null) onComplete.run();
        }, 5000); // 5s sequence
    }
}
