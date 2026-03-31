package com.jdroidx.jfindme.ui.base;

import android.graphics.Color;

/**
 * Main -> UI Engine -> ThemeManager
 * Level 4: Manages the 7 High-Contrast Enterprise Themes.
 */
public class ThemeManager {

    // Theme Elements Index Mapping:
    // [0] Screen Background
    // [1] Card Background (Cards, Drawers, Floating Views)
    // [2] Text Primary (Headers, Active Nav)
    // [3] Text Secondary (Subtitles, Inactive UI)
    // [4] Gradient Start (Left/Top)
    // [5] Gradient End (Right/Bottom)
    // [6] Gradient Overlay Text/Icon Color (Contrast Guard)
    public static final String[][] THEMES = {
        // [Enterprise Registry - Hardened 5% Opacity Override] 
        
        // 0: "FamiSafe Violet"
        {"#F5F6F8", "#0D6A11CB", "#1F1F1F", "#757591", "#D500F9", "#6A11CB", "#FFFFFF"},
        
        // 1: "Twilight Midnight"
        {"#0F2027", "#0D2C5364", "#F5F5F5", "#9E9E9E", "#00DEFF", "#2C5364", "#1F1F1F"},
        
        // 2: "Oceanic Teal"
        {"#F0F4F4", "#0D11998E", "#1F1F1F", "#758F8B", "#00E676", "#11998E", "#1F1F1F"},
        
        // 3: "Sunrise Orange"
        {"#FFF3E0", "#0DDD2C00", "#1F1F1F", "#8F7B75", "#FF9100", "#DD2C00", "#1F1F1F"},
        
        // 4: "Bubblegum Pop"
        {"#FFF0F5", "#0D89216B", "#1F1F1F", "#8F7589", "#F50057", "#89216B", "#FFFFFF"},
        
        // 5: "Silver Chrome"
        {"#E0E0E0", "#0D414345", "#1F1F1F", "#78909C", "#40C4FF", "#414345", "#1F1F1F"},
        
        // 6: "Copper Antique" (Re-engineered from Electric Azure)
        {"#FDF5E6", "#0DCB6D51", "#1F1F1F", "#756555", "#CB6D51", "#5D3110", "#FFFFFF"}, 
        
        // 7: "Golden Hour" [RE-ENGINEERED WITH CUSTOM GOLD]
        {"#FFF8E1", "#0DD0A203", "#000000", "#424242", "#F5BF03", "#D0A203", "#000000"}
    };

    // Rigorous Database Element Getters
    public static int getScreenBackgroundColor(int themeId) { return Color.parseColor(THEMES[themeId][0]); }
    public static int getCardBackgroundColor(int themeId) { return Color.parseColor(THEMES[themeId][1]); }
    public static int getTextPrimaryColor(int themeId) { return Color.parseColor(THEMES[themeId][2]); }
    public static int getTextSecondaryColor(int themeId) { return Color.parseColor(THEMES[themeId][3]); }
    
    public static int getGradientStart(int themeId) { return Color.parseColor(THEMES[themeId][4]); }
    public static int getGradientEnd(int themeId) { return Color.parseColor(THEMES[themeId][5]); }
    public static int getGradientOverlayColor(int themeId) { return Color.parseColor(THEMES[themeId][6]); }
    
    public static int getGradientMid(int themeId) {
        int start = getGradientStart(themeId);
        int end = getGradientEnd(themeId);
        return Color.rgb(
            (Color.red(start) + Color.red(end)) / 2,
            (Color.green(start) + Color.green(end)) / 2,
            (Color.blue(start) + Color.blue(end)) / 2
        );
    }

    // Legacy support to prevent engine crashes across the app during the rollout
    public static int getPrimaryColor(int themeId) { return getScreenBackgroundColor(themeId); }
    public static int getTextColor(int themeId) { return getTextPrimaryColor(themeId); }
    public static int getAccentColor(int themeId) { return getGradientStart(themeId); }
}
