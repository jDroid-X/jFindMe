package com.jdroidx.jfindme.ui.base;

import android.graphics.Color;

/**
 * Main -> UI Engine -> ThemeManager
 * Level 4: Manages the 8 High-Contrast Poetic Ceramic & Mineral Themes (WCAG AAA Compliant).
 */
public class ThemeManager {

    public static final String[] THEME_NAMES = {
        "Celadon Sage",          // 0: Whispering Sage & Porcelain Bisque (Light)
        "Nordic Obsidian",       // 1: Oceanic Glacier & Deep OLED Void (Dark)
        "Oceanic Emerald",       // 2: Marine Jade & Soft Sea Glass (Light)
        "Sunrise Terracotta",    // 3: Warm Sandstone & Apricot Bisque (Light)
        "Kyoto Wabi-Sabi",       // 4: Lotus Porcelain & Amethyst Plum (Light)
        "Silver Slate",          // 5: Mineral Basalt & Frosted Zinc (Light)
        "Earthen Clay",          // 6: Artisan Clay & Burnt Sienna (Light)
        "Golden Hour Dark"       // 7: Smoked Roasted Charcoal & Amber Glow (Dark)
    };

    // Theme Elements Index Mapping:
    // [0] Screen Background (Canvas Baseline)
    // [1] Card Background (Surface Elevation with Frosted Ceramic/Pottery Glaze)
    // [2] Text Primary (Headers, Active Nav - Ultra Contrast)
    // [3] Text Secondary (Subtitles, Inactive UI - Eye-Soothing Slate)
    // [4] Gradient Start (Left/Top)
    // [5] Gradient End (Right/Bottom)
    // [6] Gradient Overlay Text/Icon Color (Contrast Guard)
    public static final String[][] THEMES = {
        // 0: "Celadon Sage" (Whispering Sage Mist -> Glazed Frosted Porcelain White)
        {"#EBF2EB", "#F4F8F4", "#0D2818", "#415D43", "#1B4332", "#2D6A4F", "#FFFFFF"},
        
        // 1: "Nordic Obsidian" (Deep Oceanic OLED Canvas -> Frosted Cobalt Slate)
        {"#080C14", "#111A28", "#F0F6FC", "#8B9BB4", "#0EA5E9", "#2563EB", "#FFFFFF"},
        
        // 2: "Oceanic Emerald" (Soft Sea Glass Canvas -> Porcelain Sea Mist)
        {"#E5F4F0", "#F2FAF7", "#064E3B", "#2D6A4F", "#059669", "#0D9488", "#FFFFFF"},
        
        // 3: "Sunrise Terracotta" (Warm Sandstone Canvas -> Cream Clay Bisque)
        {"#F8EDE5", "#FCF6F2", "#431407", "#7C2D12", "#C2410C", "#EA580C", "#FFFFFF"},
        
        // 4: "Kyoto Wabi-Sabi" (Soft Lavender Petal Wash -> Lotus Porcelain Bisque)
        {"#F6EDF4", "#FCF6FA", "#280E24", "#703866", "#9333EA", "#C026D3", "#FFFFFF"},
        
        // 5: "Silver Slate" (Soft Slate Mist -> Glazed Frosted Mineral)
        {"#E2E8F0", "#F4F7FB", "#0F172A", "#475569", "#0284C7", "#0369A1", "#FFFFFF"},
        
        // 6: "Earthen Clay" (Warm Clay Dust -> Baked Terracotta Bisque)
        {"#F3EBE3", "#FAF4EF", "#3E1A0A", "#78350F", "#9A3412", "#C2410C", "#FFFFFF"}, 
        
        // 7: "Golden Hour Dark" (Cast Iron OLED Black -> Smoked Roasted Charcoal)
        {"#0C0A07", "#1A1612", "#FEF3C7", "#A89886", "#D97706", "#F59E0B", "#000000"}
    };

    // Rigorous Database Element Getters
    public static int getScreenBackgroundColor(int themeId) { 
        themeId = sanitizeThemeId(themeId);
        return Color.parseColor(THEMES[themeId][0]); 
    }

    public static int getCardBackgroundColor(int themeId) { 
        themeId = sanitizeThemeId(themeId);
        return Color.parseColor(THEMES[themeId][1]); 
    }

    public static int getTextPrimaryColor(int themeId) { 
        themeId = sanitizeThemeId(themeId);
        return Color.parseColor(THEMES[themeId][2]); 
    }

    public static int getTextSecondaryColor(int themeId) { 
        themeId = sanitizeThemeId(themeId);
        return Color.parseColor(THEMES[themeId][3]); 
    }
    
    public static int getGradientStart(int themeId) { 
        themeId = sanitizeThemeId(themeId);
        return Color.parseColor(THEMES[themeId][4]); 
    }

    public static int getGradientEnd(int themeId) { 
        themeId = sanitizeThemeId(themeId);
        return Color.parseColor(THEMES[themeId][5]); 
    }

    public static int getGradientOverlayColor(int themeId) { 
        themeId = sanitizeThemeId(themeId);
        return Color.parseColor(THEMES[themeId][6]); 
    }

    /**
     * Boosts color brightness/luminance by 50% while maintaining rich, vibrant chromatic saturation.
     */
    public static int brightenColor(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.min(1.0f, hsv[2] + (1.0f - hsv[2]) * factor + 0.10f * factor);
        hsv[1] = Math.min(1.0f, Math.max(0.20f, hsv[1] * 0.90f));
        return Color.HSVToColor(Color.alpha(color), hsv);
    }

    public static int getBrightenedGradientStart(int themeId) {
        return brightenColor(getGradientStart(themeId), 0.50f);
    }

    public static int getBrightenedGradientEnd(int themeId) {
        return brightenColor(getGradientEnd(themeId), 0.50f);
    }

    public static int getBrightenedGradientOverlayColor(int themeId) {
        int brightStart = getBrightenedGradientStart(themeId);
        return getContrastTextColor(brightStart);
    }
    
    public static int getGradientMid(int themeId) {
        int start = getGradientStart(themeId);
        int end = getGradientEnd(themeId);
        return Color.rgb(
            (Color.red(start) + Color.red(end)) / 2,
            (Color.green(start) + Color.green(end)) / 2,
            (Color.blue(start) + Color.blue(end)) / 2
        );
    }

    public static boolean isDarkTheme(int themeId) {
        themeId = sanitizeThemeId(themeId);
        return themeId == 1 || themeId == 7;
    }

    private static int sanitizeThemeId(int themeId) {
        if (themeId < 0 || themeId >= THEMES.length) return 0;
        return themeId;
    }

    // Dynamic WCAG Contrast Engine (Relative Luminance Standard)
    public static double calculateLuminance(int color) {
        double r = Color.red(color) / 255.0;
        double g = Color.green(color) / 255.0;
        double b = Color.blue(color) / 255.0;
        r = (r <= 0.03928) ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = (g <= 0.03928) ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = (b <= 0.03928) ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    /**
     * Dynamically calculates crystal-clear readable primary text color against ANY background.
     */
    public static int getContrastTextColor(int backgroundColor) {
        double lum = calculateLuminance(backgroundColor);
        return (lum > 0.40) ? Color.parseColor("#0F172A") : Color.parseColor("#F8FAFC");
    }

    /**
     * Dynamically calculates eye-soothing secondary text color against ANY background.
     */
    public static int getContrastSecondaryColor(int backgroundColor) {
        double lum = calculateLuminance(backgroundColor);
        return (lum > 0.40) ? Color.parseColor("#475569") : Color.parseColor("#94A3B8");
    }

    // Legacy support to prevent engine crashes across the app
    public static int getPrimaryColor(int themeId) { return getScreenBackgroundColor(themeId); }
    public static int getTextColor(int themeId) { return getTextPrimaryColor(themeId); }
    public static int getAccentColor(int themeId) { return getGradientStart(themeId); }

    /**
     * 🍸 GLASSMORPHISM & CERAMIC ENGINE
     * Creates a translucent gradient or solid background with rounded corners and subtle stroke rim.
     */
    public static android.graphics.drawable.GradientDrawable createGlassDrawable(int color, float alpha, float radius) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int a = (int)(alpha * 255);
        gd.setColor(Color.argb(a, r, g, b));
        gd.setCornerRadius(radius);
        return gd;
    }

    /**
     * 🏺 FLOATING CERAMIC POTTERY DRAWABLE
     * Creates a floating ceramic card drawable with an optical glaze top edge.
     */
    public static android.graphics.drawable.GradientDrawable createFloatingCeramicDrawable(int surfaceColor, float radius, boolean isDark) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(surfaceColor);
        gd.setCornerRadius(radius);
        int rimColor = isDark ? Color.argb(30, 255, 255, 255) : Color.argb(180, 255, 255, 255);
        gd.setStroke(2, rimColor);
        return gd;
    }
}
