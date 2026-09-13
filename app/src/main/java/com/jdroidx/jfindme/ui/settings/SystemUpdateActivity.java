package com.jdroidx.jfindme.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import com.jdroidx.jfindme.BuildConfig;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.AngelLoader;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

/**
 * 👑 GLOBAL UI ARCHITECT: System Update Center
 * Waterfall MVVM Architecture Implementation (L7 Standard)
 * Manages secure binary synchronization and system identification.
 */
public class SystemUpdateActivity extends BaseActivityJava {

    private SystemUpdateViewModel viewModel;
    private EditText emailField;
    private EditText driveLinkField;
    private EditText githubLinkField;
    private android.widget.CheckBox silentUpdateCheck;
    private TextView tvCurrentVersion;
    private TextView tvLastUpdate;
    private TextView tvLastCheck;
    private TextView tvLastDownload;
    private TextView tvUpdateAvailable;
    private TextView tvIsUpdatedStatus;
    private TextView tvLastFailedInstall;

    @Override
    protected void setupLogic() {
        // Initialize Waterfall MVVM Engine
        viewModel = new ViewModelProvider(this).get(SystemUpdateViewModel.class);
        viewModel.initData(this);

        // --- OBSERVERS: Synchronization Hub ---
        
        viewModel.getGmailAccount().observe(this, email -> {
            if (emailField != null) emailField.setText(email);
        });

        viewModel.getDriveLink().observe(this, link -> {
            if (driveLinkField != null) driveLinkField.setText(link);
        });

        viewModel.getGithubLink().observe(this, link -> {
            if (githubLinkField != null) githubLinkField.setText(link);
        });

        viewModel.isSilentMode().observe(this, isEnabled -> {
            if (silentUpdateCheck != null) {
                silentUpdateCheck.setOnCheckedChangeListener(null);
                silentUpdateCheck.setChecked(isEnabled);
                silentUpdateCheck.setOnCheckedChangeListener((bv, checked) -> viewModel.toggleSilentMode(this, checked));
            }
        });

        viewModel.getLoadingStatus().observe(this, status -> {
            if (status != null) AngelLoader.setStatus(this, status);
        });

        viewModel.isLoading().observe(this, loading -> {
            if (loading) AngelLoader.show(this);
            else AngelLoader.hide(this);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                // Parse Waterfall Error Code (SYNC_FAILED: ...)
                String[] parts = error.split(":");
                String title = parts.length > 0 ? parts[0].replace("_", " ") : "Execution Error";
                String msg = parts.length > 1 ? parts[1] : error;
                showForensicDialog(title, msg);
                viewModel.clearError();
            }
        });

        viewModel.getInstallPackageUrl().observe(this, url -> {
            if (url != null) {
                downloadAndInstallApk(url);
                viewModel.resetInstallPackageUrl();
            }
        });

        viewModel.getCheckResultDialog().observe(this, pair -> {
            if (pair != null) {
                showForensicDialog(pair.getFirst(), pair.getSecond());
                viewModel.dismissCheckResultDialog();
            }
        });

        // Forensic Update Observers
        viewModel.getCurrentVersionInfo().observe(this, info -> {
            if (tvCurrentVersion != null) tvCurrentVersion.setText("📱 CURRENT INSTALLED VERSION: " + info);
        });

        viewModel.getLastUpdateInfo().observe(this, info -> {
            if (tvLastUpdate != null) tvLastUpdate.setText("🕒 LAST APP UPDATE / INSTALL: " + info);
        });

        viewModel.getLastCheckInfo().observe(this, info -> {
            if (tvLastCheck != null) tvLastCheck.setText("🌐 LAST CHECKED ONLINE: " + info);
        });

        viewModel.getUpdateAvailableInfo().observe(this, info -> {
            if (tvUpdateAvailable != null) tvUpdateAvailable.setText("📦 UPDATE AVAILABLE: " + info);
        });

        viewModel.getLastDownloadInfo().observe(this, info -> {
            if (tvLastDownload != null) tvLastDownload.setText("📥 LAST DOWNLOADED PACKAGE: " + info);
        });

        viewModel.getUpdateStatusInfo().observe(this, info -> {
            if (tvIsUpdatedStatus != null) tvIsUpdatedStatus.setText("🛡️ SYSTEM STATUS: " + info);
        });

        viewModel.getLastFailedInstallInfo().observe(this, info -> {
            if (tvLastFailedInstall != null) {
                tvLastFailedInstall.setText("❌ LAST FAILED INSTALL: " + info);
                if (info != null && !info.startsWith("None")) {
                    tvLastFailedInstall.setTextColor(Color.parseColor("#FF5252"));
                } else {
                    tvLastFailedInstall.setTextColor(getSecondaryTextColor());
                }
            }
        });

        // 🛡️ HUMAN-IN-THE-LOOP Installation Prompt Observer
        viewModel.getShowInstallPrompt().observe(this, promptData -> {
            if (promptData != null) {
                showInstallGuidanceModal(promptData);
            }
        });
    }

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // --- HEADER: L7 Waterfall Branding ---
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        TextView title = new TextView(this);
        title.setText("SYSTEM UPDATE CENTER");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Secure Binary Synchronization Protocol");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        subtitle.setPadding(0, scale(10), 0, 0);
        header.addView(subtitle);

        root.addView(header);

        // --- MATRIX: Core Input Architecture ---
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        scroll.setClipToPadding(false);

        LinearLayout master = new LinearLayout(this);
        master.setOrientation(LinearLayout.VERTICAL);
        int quota = getResponsivePaddingQuota();
        master.setPadding(quota, quota, quota, scale(60));

        // Label-and-Box Strategy (Google Account)
        master.addView(createLabel("REGISTERED GOOGLE ACCOUNT (*)"));
        emailField = createEnterpriseField("Reading hardware id...", android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setEnabled(true);
        emailField.setFocusable(true);
        emailField.setFocusableInTouchMode(false);
        emailField.setCursorVisible(false);
        emailField.setKeyListener(null);
        master.addView(wrapField(emailField));

        // Label-and-Box Strategy (Binary Source with Interactive Edit / Custom Link)
        LinearLayout linkHeader = new LinearLayout(this);
        linkHeader.setOrientation(LinearLayout.HORIZONTAL);
        linkHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView linkLbl = createLabel("BINARY SOURCE LINK (*)");
        linkHeader.addView(linkLbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView btnEditLink = new TextView(this);
        btnEditLink.setText("EDIT / PASTE LINK");
        btnEditLink.setTextColor(getGradientStart());
        btnEditLink.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, btnEditLink, "TEXT_MINI");
        btnEditLink.setPadding(scale(15), scale(10), scale(15), scale(10));
        btnEditLink.setClickable(true);
        btnEditLink.setFocusable(true);
        btnEditLink.setOnClickListener(v -> showEditLinkDialog());
        linkHeader.addView(btnEditLink);

        master.addView(linkHeader);

        driveLinkField = createEnterpriseField("Constructing link...", android.text.InputType.TYPE_TEXT_VARIATION_URI);
        driveLinkField.setEnabled(true);
        driveLinkField.setFocusable(true);
        driveLinkField.setFocusableInTouchMode(false);
        driveLinkField.setCursorVisible(false);
        driveLinkField.setKeyListener(null);
        master.addView(wrapField(driveLinkField));

        // Label-and-Box Strategy (GitHub Source with Direct Browser & Install Actions)
        LinearLayout ghHeader = new LinearLayout(this);
        ghHeader.setOrientation(LinearLayout.HORIZONTAL);
        ghHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView ghLbl = createLabel("GITHUB REPOSITORY DISTRIBUTION (*)");
        ghHeader.addView(ghLbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView btnDirectGh = new TextView(this);
        btnDirectGh.setText("📥 DIRECT DOWNLOAD");
        btnDirectGh.setTextColor(getGradientStart());
        btnDirectGh.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, btnDirectGh, "TEXT_MINI");
        btnDirectGh.setPadding(scale(10), scale(10), scale(10), scale(10));
        btnDirectGh.setClickable(true);
        btnDirectGh.setFocusable(true);
        btnDirectGh.setOnClickListener(v -> viewModel.installGithubApk(this));
        ghHeader.addView(btnDirectGh);

        TextView btnEditGh = new TextView(this);
        btnEditGh.setText("EDIT");
        btnEditGh.setTextColor(getGradientStart());
        btnEditGh.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, btnEditGh, "TEXT_MINI");
        btnEditGh.setPadding(scale(10), scale(10), scale(10), scale(10));
        btnEditGh.setClickable(true);
        btnEditGh.setFocusable(true);
        btnEditGh.setOnClickListener(v -> showEditGithubLinkDialog());
        ghHeader.addView(btnEditGh);

        master.addView(ghHeader);

        githubLinkField = createEnterpriseField("Constructing GitHub link...", android.text.InputType.TYPE_TEXT_VARIATION_URI);
        githubLinkField.setEnabled(true);
        githubLinkField.setFocusable(true);
        githubLinkField.setFocusableInTouchMode(false);
        githubLinkField.setCursorVisible(false);
        githubLinkField.setKeyListener(null);
        master.addView(wrapField(githubLinkField));

        // Interaction Toggle: Silent Update
        master.addView(createSilentUpdateNode());

        master.addView(createSpacer(50));

        // --- ACTIONS: Compact Integrated Row (3.0 Span) ---
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setWeightSum(3.0f);

        // Direct in-app install without browser
        View btnInstall = createSmallButton("DOWNLOAD & INSTALL", android.R.drawable.ic_menu_save, true, v -> viewModel.installGithubApk(this));
        View btnUpdate = createSmallButton("CHECK UPDATE", android.R.drawable.ic_popup_sync, true, v -> viewModel.executeUpdate(this));
        View btnCancel = createSmallButton("CLOSE", android.R.drawable.ic_menu_close_clear_cancel, false, v -> finish());

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        actionRow.addView(btnInstall, btnLp);

        LinearLayout.LayoutParams midLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        midLp.setMargins(scale(10), 0, scale(10), 0);
        actionRow.addView(btnUpdate, midLp);
        
        actionRow.addView(btnCancel, btnLp);

        master.addView(actionRow);

        // --- FOOTER: Dynamic Design Balance Matrix ---
        master.addView(createArchitecturalFooter());

        scroll.addView(master);
        root.addView(scroll);

        return root;
    }

    private void showVersionSelectionModal() {
        String curVer = "V" + BuildConfig.VERSION_NAME + " (Build " + BuildConfig.VERSION_CODE + ")";

        UiArchitect.OptionItem[] versionOptions = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("⚡ LATEST PRODUCTION BUILD (Universal)", "Full architecture parity (Multi-Marker, SafeZone Radar, Roster Link)", android.R.drawable.ic_menu_save),
            new UiArchitect.OptionItem("📥 GITHUB DISTRIBUTION (jButterfly main)", "Fetch latest production release directly from GitHub raw branch", android.R.drawable.ic_menu_set_as),
            new UiArchitect.OptionItem("☁️ GOOGLE DRIVE CLOUD MIRROR", "Download mirrored build package from cloud Google Drive repository", android.R.drawable.ic_menu_upload),
            new UiArchitect.OptionItem("🏷️ CHOOSE SPECIFIC VERSION / CUSTOM URL", "Enter custom version tag or custom APK binary link to download", android.R.drawable.ic_menu_edit),
            new UiArchitect.OptionItem("🛡️ ARCHITECTURE / SIGNATURE CONFLICT GUIDE", "How to resolve 'App not installed' when switching Debug and Release builds", android.R.drawable.ic_menu_info_details)
        };

        UiArchitect.showRichOptionsDialog(this, "📦 SELECT VERSION TO UPDATE / INSTALL", versionOptions, (index, label) -> {
            switch (index) {
                case 0: // Latest Universal
                    viewModel.executeInstall(this);
                    break;
                case 1: // GitHub
                    viewModel.installGithubApk(this);
                    break;
                case 2: // Google Drive
                    viewModel.executeInstall(this);
                    break;
                case 3: // Custom Version URL
                    showEditLinkDialog();
                    break;
                case 4: // Architecture Guide
                    showArchitectureHelpDialog();
                    break;
            }
        });
    }

    private void showArchitectureHelpDialog() {
        UiArchitect.showConfirmationDialog(this,
            "🛡️ ARCHITECTURE & SIGNATURE UPDATE GUIDE",
            "Why did 'App not installed' happen in previous versions?\n\n" +
            "1. SIGNATURE MISMATCH: If your phone currently runs a DEBUG build (from Android Studio USB) and you try to install a RELEASE build from GitHub, Android OS security blocks the update.\n\n" +
            "2. TO FIX THIS ISSUE:\n" +
            "   • If updating from GitHub Release -> Uninstall existing Debug app once, then install the Release APK.\n" +
            "   • Future Release-to-Release online updates will install smoothly in 1-click!\n\n" +
            "3. PERMISSION: Ensure 'Install Unknown Apps' is enabled in Android Settings.",
            "OPEN GITHUB RELEASES", "GOT IT",
            () -> viewModel.openGithubInBrowser(this),
            null
        );
    }

    private void showEditLinkDialog() {
        String current = viewModel.getDriveLink().getValue();
        UiArchitect.showInputPromptDialog(this,
            "🔗 CUSTOM BINARY SOURCE LINK",
            "Enter or paste direct APK download URL...",
            current != null ? current : "",
            newLink -> {
                if (newLink != null && !newLink.trim().isEmpty()) {
                    boolean success = viewModel.updateCustomDriveLink(this, newLink.trim());
                    if (success) {
                        Toast.makeText(this, "Binary Source Link Updated.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void showEditGithubLinkDialog() {
        String current = viewModel.getGithubLink().getValue();
        UiArchitect.showInputPromptDialog(this,
            "🔗 CUSTOM GITHUB REPOSITORY LINK",
            "Enter or paste GitHub repository URL...",
            current != null ? current : "",
            newLink -> {
                if (newLink != null && !newLink.trim().isEmpty()) {
                    boolean success = viewModel.updateCustomGithubLink(this, newLink.trim());
                    if (success) {
                        Toast.makeText(this, "GitHub Repository Link Updated.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void showInstallGuidanceModal(SystemUpdateViewModel.InstallPromptData data) {
        String ver = data != null ? data.getTargetVersion() : "Latest";
        String sz = data != null ? data.getPackageSizeEst() : "";

        UiArchitect.OptionItem[] updateOptions = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("⚡ INSTALL NOW (Direct Download)", "Download and launch package installer immediately", android.R.drawable.ic_menu_save),
            new UiArchitect.OptionItem("📥 Install from GitHub (jButterfly)", "Fetch latest release binary from GitHub repository", android.R.drawable.ic_menu_set_as),
            new UiArchitect.OptionItem("📥 Install from Google Drive Mirror", "Download APK from cloud Google Drive storage", android.R.drawable.ic_menu_upload),
            new UiArchitect.OptionItem("🌐 Open GitHub in Web Browser", "Visit releases page in your mobile browser", android.R.drawable.ic_menu_view),
            new UiArchitect.OptionItem("⏱️ REMIND ME LATER", "Postpone update and remind on next cycle", android.R.drawable.ic_menu_recent_history)
        };

        UiArchitect.showRichOptionsDialog(this, "📦 SYSTEM UPDATE V" + ver + " (" + sz + ")", updateOptions, (index, label) -> {
            viewModel.dismissInstallPrompt();
            switch (index) {
                case 0: // Direct install
                    viewModel.executeInstall(this);
                    break;
                case 1: // GitHub
                    viewModel.installGithubApk(this);
                    break;
                case 2: // Google Drive
                    viewModel.executeInstall(this);
                    break;
                case 3: // Browser
                    viewModel.openGithubInBrowser(this);
                    break;
                case 4: // Later
                    Toast.makeText(this, "Update postponed. You will be reminded on next cycle.", Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private View createSilentUpdateNode() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(scale(20), scale(20), scale(20), scale(20));
        
        android.graphics.drawable.GradientDrawable glass = new android.graphics.drawable.GradientDrawable();
        glass.setCornerRadius(30f);
        glass.setColor(Color.parseColor("#1A" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        glass.setStroke(2, getGradientStart());
        row.setBackground(glass);

        row.setFocusable(true);
        row.setClickable(true);
        applyAccessibility(row, "Toggle Silent Update Mode");
        row.setOnClickListener(v -> {
            if (silentUpdateCheck != null) {
                silentUpdateCheck.toggle();
            }
        });

        TextView label = new TextView(this);
        label.setText("SILENT UPDATE");
        label.setTextColor(getTextColor());
        UiArchitect.applyText(this, label, "TEXT_TITLE");
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        row.addView(label);

        silentUpdateCheck = new android.widget.CheckBox(this);
        silentUpdateCheck.setButtonTintList(android.content.res.ColorStateList.valueOf(getGradientStart()));
        silentUpdateCheck.setFocusable(false);
        silentUpdateCheck.setClickable(false);
        row.addView(silentUpdateCheck);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, scale(15), 0, scale(10));
        row.setLayoutParams(lp);
        
        // Wrap everything in a vertically aligned container for strict Waterfall consistency
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        LinearLayout cardMeta = new LinearLayout(this);
        cardMeta.setOrientation(LinearLayout.VERTICAL);
        cardMeta.setPadding(scale(15), scale(15), scale(15), scale(15));
        android.graphics.drawable.GradientDrawable metaBg = new android.graphics.drawable.GradientDrawable();
        metaBg.setCornerRadius(24f);
        metaBg.setColor(Color.parseColor("#15" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        metaBg.setStroke(2, Color.parseColor("#30" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        cardMeta.setBackground(metaBg);

        tvCurrentVersion = new TextView(this);
        tvCurrentVersion.setPadding(scale(5), scale(3), 0, scale(3));
        tvCurrentVersion.setTextColor(getTextColor());
        tvCurrentVersion.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvCurrentVersion, "TEXT_MINI");
        cardMeta.addView(tvCurrentVersion);

        tvLastUpdate = new TextView(this);
        tvLastUpdate.setPadding(scale(5), scale(3), 0, scale(3));
        tvLastUpdate.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvLastUpdate, "TEXT_MINI");
        cardMeta.addView(tvLastUpdate);

        tvLastCheck = new TextView(this);
        tvLastCheck.setPadding(scale(5), scale(3), 0, scale(3));
        tvLastCheck.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvLastCheck, "TEXT_MINI");
        cardMeta.addView(tvLastCheck);

        tvLastDownload = new TextView(this);
        tvLastDownload.setPadding(scale(5), scale(3), 0, scale(3));
        tvLastDownload.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvLastDownload, "TEXT_MINI");
        cardMeta.addView(tvLastDownload);

        tvUpdateAvailable = new TextView(this);
        tvUpdateAvailable.setPadding(scale(5), scale(3), 0, scale(3));
        tvUpdateAvailable.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvUpdateAvailable, "TEXT_MINI");
        cardMeta.addView(tvUpdateAvailable);

        tvIsUpdatedStatus = new TextView(this);
        tvIsUpdatedStatus.setPadding(scale(5), scale(3), 0, scale(3));
        tvIsUpdatedStatus.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvIsUpdatedStatus, "TEXT_MINI");
        cardMeta.addView(tvIsUpdatedStatus);

        tvLastFailedInstall = new TextView(this);
        tvLastFailedInstall.setPadding(scale(5), scale(3), 0, scale(3));
        tvLastFailedInstall.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvLastFailedInstall, "TEXT_MINI");
        cardMeta.addView(tvLastFailedInstall);

        wrapper.addView(cardMeta);
        wrapper.addView(row);
        
        return wrapper;
    }

    private View createSmallButton(String text, int iconRes, boolean isPrimary, View.OnClickListener listener) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, scale(20), 0, scale(20));
        btn.setClickable(true);
        btn.setFocusable(true);
        btn.setOnClickListener(listener);

        UiArchitect.applyCardWithOverride(this, btn, isPrimary ? "CARD_CAPSULE_BUTTON" : "CARD_STANDARD");

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(isPrimary ? getGradientOverlayText() : getTextColor());
        UiArchitect.applyText(this, tv, "BUTTON_TEXT");
        tv.setGravity(Gravity.CENTER);
        btn.addView(tv);

        applyAccessibility(btn, text);
        return btn;
    }

    private View createArchitecturalFooter() {
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, scale(40), 0, scale(40));

        int lottieId = getResources().getIdentifier("butterfly_angel", "raw", getPackageName());
        if (lottieId != 0) {
            LottieAnimationView lottie = new LottieAnimationView(this);
            lottie.setAnimation(lottieId);
            lottie.setRepeatCount(LottieDrawable.INFINITE);
            lottie.playAnimation();
            int bSize = scale(180);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(bSize, bSize);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            footer.addView(lottie, lp);
        }

        TextView name = new TextView(this);
        name.setText("jFindMe Tracking Engine");
        name.setTextColor(getTextColor());
        name.setGravity(Gravity.CENTER_HORIZONTAL);
        UiArchitect.applyText(this, name, "TEXT_BODY");
        name.setAlpha(0.9f);
        footer.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Enterprise Version Stack (Dynamically mapped from BuildConfig & System Modules)
        LinearLayout.LayoutParams verLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footer.addView(createVersionLine("ENTERPRISE BUILD: V" + BuildConfig.VERSION_NAME + " (CODE " + BuildConfig.VERSION_CODE + ")"), verLp);
        footer.addView(createVersionLine("DATABASE SCHEMA: V" + DatabaseHelper.DATABASE_VERSION + " (EPOCH-AWARE)"), verLp);
        footer.addView(createVersionLine("UI ARCHITECT STAGE: " + UiArchitect.getStage(this).name()), verLp);
        footer.addView(createVersionLine("OS ENVIRONMENT: ANDROID " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"), verLp);

        return footer;
    }

    private View createVersionLine(String text) {
        TextView ver = new TextView(this);
        ver.setText(text);
        ver.setTextColor(getSecondaryTextColor());
        ver.setGravity(Gravity.CENTER_HORIZONTAL);
        UiArchitect.applyText(this, ver, "TEXT_MINI");
        ver.setAlpha(0.6f);
        ver.setPadding(0, scale(2), 0, 0);
        return ver;
    }

    private TextView createLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(getGradientStart());
        UiArchitect.applyText(this, label, "TEXT_LABEL");
        label.setPadding(scale(10), scale(15), 0, scale(8));
        return label;
    }

    private EditText createEnterpriseField(String hint, int inputType) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setTextColor(getTextColor());
        et.setHintTextColor(getSecondaryTextColor());
        et.setBackgroundColor(Color.TRANSPARENT);
        et.setPadding(scale(25), scale(20), scale(25), scale(20));
        UiArchitect.applyText(this, et, "TEXT_BODY");
        return et;
    }

    private View wrapField(View field) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable glass = new android.graphics.drawable.GradientDrawable();
        glass.setCornerRadius(30f);
        glass.setColor(getCardColor());
        glass.setStroke(2, Color.parseColor("#33" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        wrap.setBackground(glass);
        wrap.addView(field);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, scale(15));
        wrap.setLayoutParams(lp);
        
        return wrap;
    }

    private View createSpacer(int dpSize) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, scale(dpSize)));
        return v;
    }

    private void downloadAndInstallApk(String url) {
        if (url == null || url.trim().isEmpty()) return;
        com.jdroidx.jfindme.utils.ApkInstallerHelper.INSTANCE.downloadAndInstall(this, url, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Package downloaded. Launching installer...", Toast.LENGTH_SHORT).show();
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.jdroidx.jfindme.utils.ApkInstallerHelper.INSTANCE.resumePendingInstall(this);
    }

    @Override
    protected void onSaveCurrentState(android.os.Bundle outState) {
        if (outState == null) return;
        if (driveLinkField != null) {
            outState.putString("SAVED_SOURCE_LINK", driveLinkField.getText().toString());
        }
        if (githubLinkField != null) {
            outState.putString("SAVED_GITHUB_LINK", githubLinkField.getText().toString());
        }
    }

    @Override
    protected void onUiRebuilt(android.os.Bundle savedState) {
        if (savedState == null) return;
        String link = savedState.getString("SAVED_SOURCE_LINK");
        if (driveLinkField != null && link != null && !link.isEmpty()) {
            driveLinkField.setText(link);
        }
        String ghLink = savedState.getString("SAVED_GITHUB_LINK");
        if (githubLinkField != null && ghLink != null && !ghLink.isEmpty()) {
            githubLinkField.setText(ghLink);
        }
    }
}
