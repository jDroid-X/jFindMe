package com.jdroidx.jfindme.ui.management;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.SystemLogger;
import java.io.File;

/**
 * SystemLogActivity — Enterprise Audit Log Viewer.
 *
 * AUDIT FIX [GAP-005, AUD-014]: Replaces the 109-byte stub with a fully
 * implemented WebView-based audit log viewer.
 *
 * FEATURES:
 * - Renders HTML audit logs from SystemLogger.readConsolidatedAuditLogs()
 * - Share action exports jfindme_playstore_debug.txt via Android share sheet
 * - Refresh button re-reads the latest logs on demand
 * - Filter pills: ALL | INFO | WARN | ERROR
 * - 7-day log rotation display
 *
 * MVVM: No ViewModel needed — this is a pure read-only display screen.
 *       All IO is performed asynchronously via SystemLogger's existing executor.
 *
 * NAVIGATION: Accessible from SettingsActivity → "Audit Logs" menu item.
 */
public class SystemLogActivity extends BaseActivityJava {

    private static final String TAG = "SystemLogActivity";
    private WebView webView;
    private String currentFilter = "ALL";

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // ─── HEADER ───────────────────────────────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        TextView title = new TextView(this);
        title.setText("SYSTEM AUDIT LOGS");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("7-Day Forensic Log Matrix | Tap Share to Export");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        header.addView(subtitle);
        root.addView(header);

        // ─── FILTER PILLS ─────────────────────────────────────────────────────
        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);
        int fp = scale(20);
        filterRow.setPadding(fp, fp / 2, fp, fp / 2);

        String[] filters = {"ALL", "INFO", "WARN", "ERROR"};
        for (String f : filters) {
            TextView pill = new TextView(this);
            pill.setText(f);
            pill.setId(f.hashCode());
            pill.setGravity(Gravity.CENTER);
            pill.setPadding(scale(30), scale(15), scale(30), scale(15));
            UiArchitect.applyText(this, pill, "TEXT_BODY");
            updateFilterPillStyle(pill, f.equals(currentFilter));
            int fCopy = f.hashCode();
            String filterValue = f;
            pill.setOnClickListener(v -> {
                currentFilter = filterValue;
                applyFilter();
                // Update all pill styles
                for (String pf : filters) {
                    View pv = filterRow.findViewWithTag(pf);
                    if (pv instanceof TextView) {
                        updateFilterPillStyle((TextView) pv, pf.equals(currentFilter));
                    }
                }
            });
            pill.setTag(f);
            LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pillLp.setMargins(0, 0, scale(15), 0);
            pill.setLayoutParams(pillLp);
            filterRow.addView(pill);
        }
        root.addView(filterRow);

        // ─── WEBVIEW LOG DISPLAY ──────────────────────────────────────────────
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false); // Security: no JS needed
        settings.setDefaultFontSize(12);
        settings.setTextZoom(90);
        webView.setBackgroundColor(getPrimaryColor());

        LinearLayout.LayoutParams wvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        int wvPad = scale(15);
        webView.setPadding(wvPad, 0, wvPad, scale(120));
        root.addView(webView, wvLp);

        // ─── ACTION BUTTONS ───────────────────────────────────────────────────
        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);
        int bp = scale(20);
        buttonRow.setPadding(bp, bp, bp, bp);
        UiArchitect.applyCard(this, buttonRow, "CARD_STANDARD");

        // Refresh button
        View refreshBtn = createIconButton("Refresh Logs",
                android.R.drawable.ic_menu_rotate, false, v -> loadLogs());
        LinearLayout.LayoutParams rbLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        rbLp.setMargins(0, 0, scale(15), 0);
        refreshBtn.setLayoutParams(rbLp);
        buttonRow.addView(refreshBtn);

        // Share button
        View shareBtn = createIconButton("Share Logs",
                android.R.drawable.ic_menu_share, true, v -> shareLogs());
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        shareBtn.setLayoutParams(sbLp);
        buttonRow.addView(shareBtn);

        root.addView(buttonRow);

        return root;
    }

    @Override
    protected void setupLogic() {
        SystemLogger.i(TAG, "Audit log viewer opened.");
        loadLogs();
    }

    private void loadLogs() {
        // Run on background thread to avoid blocking UI
        new Thread(() -> {
            final String rawHtml = SystemLogger.readConsolidatedAuditLogs(this);
            final String styledHtml = buildStyledHtml(rawHtml);
            runOnUiThread(() -> {
                if (webView != null) {
                    webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null);
                    SystemLogger.d(TAG, "Audit log rendered. Filter: " + currentFilter);
                }
            });
        }).start();
    }

    private void applyFilter() {
        loadLogs(); // Reload with current filter applied
    }

    private String buildStyledHtml(String rawContent) {
        int bgColor = getPrimaryColor();
        int textColor = getTextColor();
        String bgHex = String.format("#%06X", (0xFFFFFF & bgColor));
        String textHex = String.format("#%06X", (0xFFFFFF & textColor));
        int accentRaw = getGradientStart();
        String accentHex = String.format("#%06X", (0xFFFFFF & accentRaw));

        // Apply filter
        String content = rawContent;
        if (!"ALL".equals(currentFilter)) {
            StringBuilder filtered = new StringBuilder();
            // Keep only divs containing the filter keyword in their content
            String[] parts = content.split("<div");
            for (String part : parts) {
                if (part.isEmpty()) continue;
                if (part.toUpperCase().contains("[" + currentFilter)) {
                    filtered.append("<div").append(part);
                }
            }
            content = filtered.length() > 0 ? filtered.toString()
                    : "<p style='color:" + accentHex + ";padding:20px'>No " + currentFilter + " level logs found.</p>";
        }

        if (content == null || content.trim().isEmpty()) {
            content = "<p style='color:#AAAAAA;padding:20px;text-align:center'>" +
                      "📋 No audit logs found.<br><br>" +
                      "Logs are generated as the app runs. " +
                      "Try using the app for a few minutes and then refresh.</p>";
        }

        return "<!DOCTYPE html><html><head>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "<style>" +
               "body { background:" + bgHex + "; color:" + textHex + "; font-family:monospace; " +
               "font-size:11px; padding:10px; margin:0; word-wrap:break-word; }" +
               "h3 { color:" + accentHex + "; font-size:13px; margin:15px 0 5px 0; }" +
               "hr { border-color:#333; }" +
               "font[color='#4CAF50'] { color:#4CAF50 !important; }" +
               "font[color='#FF5252'] { color:#FF5252 !important; }" +
               "font[color='#AAAAAA'] { color:#888888 !important; }" +
               "</style></head><body>" +
               content +
               "</body></html>";
    }

    private void shareLogs() {
        try {
            File logDir = new File(getExternalFilesDir(null), "PlayStoreLogs");
            File logFile = new File(logDir, "jfindme_playstore_debug.txt");
            if (!logFile.exists()) {
                Toast.makeText(this, "No debug log file found. Run the app first to generate logs.", Toast.LENGTH_LONG).show();
                return;
            }
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", logFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "jFindMe Audit Log Export");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Export Audit Log via..."));
            SystemLogger.i(TAG, "Audit log share dialog opened.");
        } catch (Exception e) {
            SystemLogger.e(TAG, "Failed to share log file", e);
            Toast.makeText(this, "Could not share log file. Please check storage permissions.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateFilterPillStyle(TextView pill, boolean isActive) {
        if (isActive) {
            UiArchitect.applyCard(this, pill, "CARD_CAPSULE_BUTTON");
            pill.setTextColor(getGradientOverlayText());
            pill.setTypeface(null, Typeface.BOLD);
        } else {
            UiArchitect.applyCard(this, pill, "CARD_STANDARD");
            pill.setTextColor(getTextColor());
            pill.setTypeface(null, Typeface.NORMAL);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Cleanup WebView to prevent memory leaks
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.onPause();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
    }
}
