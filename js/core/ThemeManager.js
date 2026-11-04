/**
 * jFindMe Matrix - Theme Manager (Light & Dark Theme Switcher)
 * Persistent UI theme state management with smooth DOM updates
 */
export class ThemeManager {
    static THEME_KEY = "jfindme_theme_preference";

    static init() {
        const savedTheme = localStorage.getItem(ThemeManager.THEME_KEY) || "dark";
        ThemeManager.applyTheme(savedTheme);
    }

    static getCurrentTheme() {
        return localStorage.getItem(ThemeManager.THEME_KEY) || "dark";
    }

    static toggleTheme() {
        const current = ThemeManager.getCurrentTheme();
        const nextTheme = current === "dark" ? "light" : "dark";
        ThemeManager.applyTheme(nextTheme);
        return nextTheme;
    }

    static applyTheme(theme) {
        localStorage.setItem(ThemeManager.THEME_KEY, theme);
        const root = document.documentElement;

        if (theme === "light") {
            root.classList.add("light-theme");
            root.classList.remove("dark");
        } else {
            root.classList.remove("light-theme");
            root.classList.add("dark");
        }

        // Update Theme Button Label / Icon if present
        const btn = document.getElementById("btn-theme-toggle");
        if (btn) {
            btn.innerHTML = theme === "light" ? "🌙 Dark" : "☀️ Light";
            btn.title = `Switch to ${theme === "light" ? "Dark" : "Light"} Mode`;
        }

        // Broadcast global theme change event across all active MVC controllers & map canvases
        window.dispatchEvent(new CustomEvent('themeChanged', { detail: { theme } }));
    }
}
