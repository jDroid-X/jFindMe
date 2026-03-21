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
        // 0: "FamiSafe Violet" (Neon Violet Glow)
        {"#F5F6F8", "#FFFFFF", "#1F1F1F", "#757575", "#D500F9", "#6A11CB", "#FFFFFF"},
        
        // 1: "Twilight Midnight" (Neon Cyber Blue Glow)
        {"#0F2027", "#1A2536", "#F5F5F5", "#9E9E9E", "#00DEFF", "#2C5364", "#1F1F1F"},
        
        // 2: "Oceanic Teal" (Neon Mint/Teal Glow)
        {"#F0F4F4", "#FFFFFF", "#1F1F1F", "#757575", "#00E676", "#11998E", "#1F1F1F"},
        
        // 3: "Sunrise Orange" (Neon Fiery Orange Glow)
        {"#FFF4F0", "#FFFFFF", "#1F1F1F", "#757575", "#FF3D00", "#FF4B2B", "#FFFFFF"},
        
        // 4: "Bubblegum Pop" (Neon Magenta Glow)
        {"#FFF0F5", "#FFFFFF", "#1F1F1F", "#757575", "#F50057", "#89216B", "#FFFFFF"},
        
        // 5: "Silver Chrome" (Neon Silver-Ice Glow)
        {"#E0E0E0", "#FFFFFF", "#1F1F1F", "#757575", "#40C4FF", "#414345", "#1F1F1F"},
        
        // 6: "Electric Azure" (Neon Azure/Cyan Glow)
        {"#F0F8FF", "#FFFFFF", "#1F1F1F", "#757575", "#00B0FF", "#92FE9D", "#1F1F1F"}, 
        
        // 7: "Golden Hour" (Neon Gold/Yellow Glow)
        {"#FFF8E1", "#FFFFFF", "#1F1F1F", "#757575", "#FFEA00", "#F2C94C", "#1F1F1F"}
    };

    // Rigorous Database Element Getters
    public static int getScreenBackgroundColor(int themeId) { return Color.parseColor(THEMES[themeId][0]); }
    public static int getCardBackgroundColor(int themeId) { return Color.parseColor(THEMES[themeId][1]); }
    public static int getTextPrimaryColor(int themeId) { return Color.parseColor(THEMES[themeId][2]); }
    public static int getTextSecondaryColor(int themeId) { return Color.parseColor(THEMES[themeId][3]); }
    
    public static int getGradientStart(int themeId) { return Color.parseColor(THEMES[themeId][4]); }
    public static int getGradientEnd(int themeId) { return Color.parseColor(THEMES[themeId][5]); }
    public static int getGradientOverlayColor(int themeId) { return Color.parseColor(THEMES[themeId][6]); }
    
    // Legacy support to prevent engine crashes across the app during the rollout
    public static int getPrimaryColor(int themeId) { return getScreenBackgroundColor(themeId); }
    public static int getTextColor(int themeId) { return getTextPrimaryColor(themeId); }
    public static int getAccentColor(int themeId) { return getGradientStart(themeId); }
}
