package com.jdroidx.jbutterfly.ui.base;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatDelegate;
import com.jdroidx.jbutterfly.utils.RegistryManager;

public class ThemeManager {

    public static final String THEME_SYSTEM = "SYSTEM";
    public static final String THEME_LIGHT = "LIGHT";
    public static final String THEME_DARK = "DARK";

    public static void applyTheme(Context context) {
        String mode = RegistryManager.getString(context, RegistryManager.KEY_THEME_MODE, THEME_SYSTEM);
        switch (mode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static boolean isDarkMode(Context context) {
        String mode = RegistryManager.getString(context, RegistryManager.KEY_THEME_MODE, THEME_SYSTEM);
        if (THEME_DARK.equals(mode)) return true;
        if (THEME_LIGHT.equals(mode)) return false;
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void toggleTheme(Context context) {
        boolean dark = isDarkMode(context);
        String next = dark ? THEME_LIGHT : THEME_DARK;
        RegistryManager.putString(context, RegistryManager.KEY_THEME_MODE, next);
        applyTheme(context);
    }

    // Dynamic Color Getters based on OS & Custom Theme
    public static int getBackgroundColor(Context context) {
        return isDarkMode(context) ? Color.parseColor("#080C14") : Color.parseColor("#F8FAFC");
    }

    public static int getSurfaceColor(Context context) {
        return isDarkMode(context) ? Color.parseColor("#111827") : Color.parseColor("#FFFFFF");
    }

    public static int getCardColor(Context context) {
        return isDarkMode(context) ? Color.parseColor("#1F2937") : Color.parseColor("#F1F5F9");
    }

    public static int getBorderColor(Context context) {
        return isDarkMode(context) ? Color.parseColor("#374151") : Color.parseColor("#CBD5E1");
    }

    public static int getPrimaryTextColor(Context context) {
        return isDarkMode(context) ? Color.parseColor("#FFFFFF") : Color.parseColor("#0F172A");
    }

    public static int getSecondaryTextColor(Context context) {
        return isDarkMode(context) ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");
    }

    public static int getBrandColor(Context context) {
        return Color.parseColor("#3B82F6"); // Butterfly Blue
    }
}
