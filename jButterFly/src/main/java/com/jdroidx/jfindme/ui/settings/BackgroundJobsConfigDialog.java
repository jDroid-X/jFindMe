package com.jdroidx.jfindme.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jdroidx.jfindme.service.BackgroundJobManager;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.ThemeManager;
import com.jdroidx.jfindme.ui.base.UiArchitect;

import java.util.List;

/**
 * 🛠️ BackgroundJobsConfigDialog: Interactive Management Console for all
 * Background Engine Jobs, Sentinels, and Workers.
 *
 * Human-in-the-loop features:
 * 1. Live toggle to pause / activate any background worker.
 * 2. Instant manual execution trigger ("⚡ RUN NOW") with feedback.
 * 3. Predefined & custom frequency selection with battery-impact advisories.
 * 4. Real-time "Last Executed" and "Status" telemetry indicators.
 */
public class BackgroundJobsConfigDialog {

    private static int resolveThemeId(Context context) {
        return context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getInt("theme_id", 0);
    }

    public static void show(BaseActivityJava activity) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        int themeId = resolveThemeId(activity);
        int screenBg = ThemeManager.getScreenBackgroundColor(themeId);
        int textPrimary = ThemeManager.getTextPrimaryColor(themeId);
        int textSecondary = ThemeManager.getTextSecondaryColor(themeId);
        int gradStart = ThemeManager.getGradientStart(themeId);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(screenBg);
        int pad = activity.scale(35);
        root.setPadding(pad, pad, pad, pad);

        // --- HEADER ---
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, activity.scale(20));

        TextView title = new TextView(activity);
        title.setText("⚙️ BACKGROUND JOBS & SENTINELS");
        title.setTextColor(gradStart);
        title.setTextSize(UiArchitect.getResponsiveTextSize(activity, "TEXT_HEADER"));
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        headerRow.addView(title);

        ImageView closeBtn = new ImageView(activity);
        closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        closeBtn.setColorFilter(textPrimary);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        headerRow.addView(closeBtn, new LinearLayout.LayoutParams(activity.scale(70), activity.scale(70)));

        root.addView(headerRow);

        TextView subTitle = new TextView(activity);
        subTitle.setText("Configure telemetry frequencies, trigger manual sweeps, or pause engine sentinels.");
        subTitle.setTextColor(textSecondary);
        subTitle.setTextSize(UiArchitect.getResponsiveTextSize(activity, "TEXT_BODY"));
        subTitle.setPadding(0, 0, 0, activity.scale(25));
        root.addView(subTitle);

        // --- LIST SCROLLER ---
        ScrollView scroll = new ScrollView(activity);
        LinearLayout jobListContainer = new LinearLayout(activity);
        jobListContainer.setOrientation(LinearLayout.VERTICAL);

        renderJobList(activity, jobListContainer, dialog);

        scroll.addView(jobListContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        dialog.setContentView(root);
        dialog.show();

        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.95);
            lp.height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.88);
            dialog.getWindow().setAttributes(lp);
        }
    }

    private static void renderJobList(BaseActivityJava activity, LinearLayout container, Dialog parentDialog) {
        container.removeAllViews();
        List<BackgroundJobManager.JobMetadata> jobs = BackgroundJobManager.INSTANCE.getAllJobs(activity);

        int themeId = resolveThemeId(activity);
        int screenBg = ThemeManager.getScreenBackgroundColor(themeId);
        int cardBg = ThemeManager.getCardBackgroundColor(themeId);
        int textPrimary = ThemeManager.getTextPrimaryColor(themeId);
        int textSecondary = ThemeManager.getTextSecondaryColor(themeId);
        int gradStart = ThemeManager.getGradientStart(themeId);
        int gradOverlay = ThemeManager.getGradientOverlayColor(themeId);

        String lastCat = "";
        for (BackgroundJobManager.JobMetadata job : jobs) {
            if (!job.getCategory().equals(lastCat)) {
                lastCat = job.getCategory();
                TextView catHeader = new TextView(activity);
                catHeader.setText(lastCat);
                catHeader.setTextColor(gradStart);
                catHeader.setTextSize(UiArchitect.getResponsiveTextSize(activity, "TEXT_TITLE"));
                catHeader.setTypeface(null, Typeface.BOLD);
                catHeader.setPadding(0, activity.scale(20), 0, activity.scale(10));
                container.addView(catHeader);
            }

            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(activity.getSolidDrawable(cardBg, activity.scaleRadius(16)));
            int cPad = activity.scale(25);
            card.setPadding(cPad, cPad, cPad, cPad);

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, 0, 0, activity.scale(20));
            card.setLayoutParams(cardLp);

            // Job Title + Switch + Status Pill
            LinearLayout topRow = new LinearLayout(activity);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView jobTitle = new TextView(activity);
            jobTitle.setText(job.getTitle());
            jobTitle.setTextColor(textPrimary);
            jobTitle.setTextSize(UiArchitect.getResponsiveTextSize(activity, "TEXT_TITLE"));
            jobTitle.setTypeface(null, Typeface.BOLD);
            jobTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            topRow.addView(jobTitle);

            Switch toggleSwitch = new Switch(activity);
            toggleSwitch.setChecked(job.isEnabled());
            toggleSwitch.setOnClickListener(v -> {
                boolean targetState = toggleSwitch.isChecked();
                if (!targetState && (job.getJobId().equals("JOB_TRACKING_SERVICE") || job.getJobId().equals("JOB_LOCATION_WATCHDOG"))) {
                    new AlertDialog.Builder(activity)
                            .setTitle("⚠️ Pause Critical Engine Sentinel?")
                            .setMessage("Pausing \"" + job.getTitle() + "\" may cause live location updates to halt or allow the operating system to terminate background tracking. Are you sure?")
                            .setPositiveButton("PAUSE JOB", (d, w) -> {
                                BackgroundJobManager.INSTANCE.toggleJobEnabled(activity, job.getJobId(), false);
                                Toast.makeText(activity, job.getTitle() + " PAUSED", Toast.LENGTH_SHORT).show();
                                renderJobList(activity, container, parentDialog);
                            })
                            .setNegativeButton("KEEP ACTIVE", (d, w) -> {
                                toggleSwitch.setChecked(true);
                            })
                            .setOnCancelListener(d -> toggleSwitch.setChecked(true))
                            .show();
                } else {
                    BackgroundJobManager.INSTANCE.toggleJobEnabled(activity, job.getJobId(), targetState);
                    Toast.makeText(activity, job.getTitle() + (targetState ? " ACTIVATED" : " PAUSED"), Toast.LENGTH_SHORT).show();
                    renderJobList(activity, container, parentDialog);
                }
            });
            topRow.addView(toggleSwitch);

            card.addView(topRow);

            // Description
            TextView desc = new TextView(activity);
            desc.setText(job.getDescription());
            desc.setTextColor(textSecondary);
            desc.setTextSize(UiArchitect.getResponsiveTextSize(activity, "TEXT_BODY"));
            desc.setPadding(0, activity.scale(8), 0, activity.scale(10));
            card.addView(desc);

            // Telemetry Status Details (Frequency & Last Executed)
            LinearLayout statsRow = new LinearLayout(activity);
            statsRow.setOrientation(LinearLayout.HORIZONTAL);
            statsRow.setGravity(Gravity.CENTER_VERTICAL);
            statsRow.setPadding(0, 0, 0, activity.scale(15));

            TextView freqLabel = new TextView(activity);
            freqLabel.setText("⏱️ Interval: " + job.getCurrentFrequencyLabel());
            freqLabel.setTextColor(textPrimary);
            freqLabel.setTextSize(UiArchitect.getResponsiveTextSize(activity, "TEXT_LABEL"));
            freqLabel.setTypeface(null, Typeface.BOLD);
            freqLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            statsRow.addView(freqLabel);

            TextView lastRunLabel = new TextView(activity);
            lastRunLabel.setText("Last Run: " + job.getLastRunFormatted());
            lastRunLabel.setTextColor(textSecondary);
            lastRunLabel.setTextSize(UiArchitect.getResponsiveTextSize(activity, "TEXT_LABEL"));
            statsRow.addView(lastRunLabel);

            card.addView(statsRow);

            // Action Buttons: RUN NOW ⚡ & EDIT FREQUENCY ✏️
            LinearLayout btnRow = new LinearLayout(activity);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(Gravity.CENTER_VERTICAL);

            // 1. Run Now Action
            TextView runNowBtn = new TextView(activity);
            runNowBtn.setText("⚡ RUN NOW");
            runNowBtn.setTextColor(gradOverlay);
            runNowBtn.setTextSize(UiArchitect.getResponsiveTextSize(activity, "TEXT_LABEL"));
            runNowBtn.setTypeface(null, Typeface.BOLD);
            runNowBtn.setGravity(Gravity.CENTER);
            runNowBtn.setBackground(activity.getSolidDrawable(gradStart, activity.scaleRadius(10)));
            int bPad = activity.scale(14);
            runNowBtn.setPadding(bPad * 2, bPad, bPad * 2, bPad);
            runNowBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            runNowBtn.setOnClickListener(v -> {
                runNowBtn.setEnabled(false);
                runNowBtn.setText("RUNNING...");
                BackgroundJobManager.INSTANCE.triggerJobNow(activity, job.getJobId(), (success, message) -> {
                    activity.runOnUiThread(() -> {
                        runNowBtn.setEnabled(true);
                        runNowBtn.setText("⚡ RUN NOW");
                        new AlertDialog.Builder(activity)
                                .setTitle(success ? "✅ Execution Triggered" : "⚠️ Notice")
                                .setMessage(message)
                                .setPositiveButton("OK", (d, w) -> renderJobList(activity, container, parentDialog))
                                .show();
                    });
                    return null;
                });
            });
            btnRow.addView(runNowBtn);

            View spacer = new View(activity);
            btnRow.addView(spacer, new LinearLayout.LayoutParams(activity.scale(15), 1));

            // 2. Edit Frequency Action
            TextView editBtn = new TextView(activity);
            editBtn.setText("✏️ FREQUENCY");
            editBtn.setTextColor(textPrimary);
            editBtn.setTextSize(UiArchitect.getResponsiveTextSize(activity, "TEXT_LABEL"));
            editBtn.setTypeface(null, Typeface.BOLD);
            editBtn.setGravity(Gravity.CENTER);
            editBtn.setBackground(activity.getSolidDrawable(screenBg, activity.scaleRadius(10)));
            editBtn.setPadding(bPad * 2, bPad, bPad * 2, bPad);
            editBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            editBtn.setOnClickListener(v -> showFrequencySelectionDialog(activity, job, () -> renderJobList(activity, container, parentDialog)));
            btnRow.addView(editBtn);

            card.addView(btnRow);
            container.addView(card);
        }
    }

    private static void showFrequencySelectionDialog(BaseActivityJava activity, BackgroundJobManager.JobMetadata job, Runnable onUpdated) {
        int optionCount = job.getFrequencyOptions().size() + 1; // +1 for "Custom Interval..."
        String[] optionNames = new String[optionCount];
        long[] optionValues = new long[optionCount];
        int selectedIndex = 0;

        for (int i = 0; i < job.getFrequencyOptions().size(); i++) {
            optionNames[i] = job.getFrequencyOptions().get(i).getFirst();
            optionValues[i] = job.getFrequencyOptions().get(i).getSecond();
            if (optionValues[i] == job.getCurrentIntervalRaw()) {
                selectedIndex = i;
            }
        }
        optionNames[optionCount - 1] = "⚙️ Custom Interval (Manual Entry)...";
        optionValues[optionCount - 1] = -1L;

        new AlertDialog.Builder(activity)
                .setTitle("⏱️ Frequency: " + job.getTitle())
                .setSingleChoiceItems(optionNames, selectedIndex, (dialog, which) -> {
                    long chosenInterval = optionValues[which];
                    dialog.dismiss();

                    if (chosenInterval == -1L) {
                        showCustomIntervalInputDialog(activity, job, onUpdated);
                    } else {
                        // Check for battery impact warning on high frequencies
                        if (chosenInterval <= 7000L && job.getJobId().equals("JOB_TRACKING_SERVICE")) {
                            new AlertDialog.Builder(activity)
                                    .setTitle("⚠️ High Battery Consumption Notice")
                                    .setMessage("Running live GPS tracking at high speed (" + optionNames[which] + ") will increase battery drain significantly. Continue?")
                                    .setPositiveButton("ENABLE", (d, w) -> {
                                        BackgroundJobManager.INSTANCE.updateJobFrequency(activity, job.getJobId(), chosenInterval);
                                        Toast.makeText(activity, "Frequency set to " + optionNames[which], Toast.LENGTH_SHORT).show();
                                        if (onUpdated != null) onUpdated.run();
                                    })
                                    .setNegativeButton("CANCEL", null)
                                    .show();
                        } else {
                            BackgroundJobManager.INSTANCE.updateJobFrequency(activity, job.getJobId(), chosenInterval);
                            Toast.makeText(activity, "Frequency set to " + optionNames[which], Toast.LENGTH_SHORT).show();
                            if (onUpdated != null) onUpdated.run();
                        }
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private static void showCustomIntervalInputDialog(BaseActivityJava activity, BackgroundJobManager.JobMetadata job, Runnable onUpdated) {
        String unit = "seconds";
        long multiplier = 1000L;
        long minVal = 1L;
        long maxVal = 86400L;

        if (job.getJobId().equals("JOB_OFFLINE_SYNC") || job.getJobId().equals("JOB_LOCATION_WATCHDOG") || job.getJobId().equals("JOB_SIM_FORENSIC")) {
            unit = "minutes (min 15 min for WorkManager)";
            multiplier = 1L;
            minVal = 15L;
            maxVal = 1440L;
        } else if (job.getJobId().equals("JOB_SILENT_UPDATE")) {
            unit = "hours";
            multiplier = 1L;
            minVal = 1L;
            maxVal = 72L;
        } else if (job.getJobId().equals("JOB_PRESENCE_PULSE")) {
            unit = "seconds";
            multiplier = 1L;
            minVal = 10L;
            maxVal = 600L;
        }

        final long finalMultiplier = multiplier;
        final long finalMin = minVal;
        final long finalMax = maxVal;
        final String finalUnit = unit;

        EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter " + unit + " (" + minVal + " - " + maxVal + ")");
        input.setPadding(activity.scale(30), activity.scale(20), activity.scale(30), activity.scale(20));

        new AlertDialog.Builder(activity)
                .setTitle("⚙️ Custom Interval (" + job.getTitle() + ")")
                .setMessage("Enter the custom running frequency in " + finalUnit + ":")
                .setView(input)
                .setPositiveButton("SAVE", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) {
                        Toast.makeText(activity, "Interval value cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        long entered = Long.parseLong(text);
                        if (entered < finalMin || entered > finalMax) {
                            Toast.makeText(activity, "Value must be between " + finalMin + " and " + finalMax + " " + finalUnit, Toast.LENGTH_LONG).show();
                            return;
                        }
                        long calculatedRaw = entered * finalMultiplier;
                        BackgroundJobManager.INSTANCE.updateJobFrequency(activity, job.getJobId(), calculatedRaw);
                        Toast.makeText(activity, "Custom frequency set to " + entered + " " + finalUnit, Toast.LENGTH_SHORT).show();
                        if (onUpdated != null) onUpdated.run();
                    } catch (NumberFormatException e) {
                        Toast.makeText(activity, "Invalid number entered.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }
}
