package com.jdroidx.jbutterfly.ui.management;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.android.gms.location.LocationServices;
import com.jdroidx.jbutterfly.data.local.DatabaseHelper;
import com.jdroidx.jbutterfly.data.model.SafeZoneModel;
import com.jdroidx.jbutterfly.data.model.UserLocation;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.utils.DeviceIdentityHelper;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class GeofenceActivity extends Activity {

    private LinearLayout listContainer;
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        detectCurrentLocation();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        // Header
        LinearLayout header = UiArchitect.createHeaderBar(this, "SafeZone Geofence Defense", v -> finish());
        root.addView(header);

        // Action Toolbar
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        int pad = UiArchitect.dpToPx(this, 16);
        bar.setPadding(pad, pad, pad, UiArchitect.dpToPx(this, 8));

        TextView tvSub = new TextView(this);
        tvSub.setText("Perimeter monitoring & entry/exit alerts");
        tvSub.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvSub.setTextSize(UiArchitect.spToPx(this, 12f));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        bar.addView(tvSub, subLp);

        Button btnAdd = UiArchitect.createPrimaryButton(this, "+ NEW SAFEZONE", v -> showSafeZoneDialog(null));
        bar.addView(btnAdd);

        root.addView(bar);

        // Scrollable List Container
        ScrollView scroll = new ScrollView(this);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(pad, 0, pad, UiArchitect.dpToPx(this, 30));
        scroll.addView(listContainer);

        root.addView(scroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
        refreshZones();
    }

    @SuppressLint("MissingPermission")
    private void detectCurrentLocation() {
        String myAgentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        UserLocation last = db.getLatestLocationForAgent(myAgentId);
        if (last != null && (last.latitude != 0.0 || last.longitude != 0.0)) {
            currentLat = last.latitude;
            currentLng = last.longitude;
        }

        try {
            LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    currentLat = loc.getLatitude();
                    currentLng = loc.getLongitude();
                }
            });
        } catch (Exception ignored) {}
    }

    private void refreshZones() {
        listContainer.removeAllViews();
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        List<SafeZoneModel> zones = db.getAllSafeZones();

        if (zones.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("🛡️ No active SafeZones established.\nCreate a boundary (e.g. Home, School, Office) to receive instant entry/exit alert notifications.");
            tvEmpty.setTextColor(ThemeManager.getSecondaryTextColor(this));
            tvEmpty.setTextSize(UiArchitect.spToPx(this, 13f));
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(0, UiArchitect.dpToPx(this, 60), 0, 0);
            listContainer.addView(tvEmpty);
            return;
        }

        for (SafeZoneModel zone : zones) {
            listContainer.addView(createZoneCard(zone));
        }
    }

    private View createZoneCard(SafeZoneModel zone) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = UiArchitect.dpToPx(this, 16);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getSurfaceColor(this));
        bg.setStroke(UiArchitect.dpToPx(this, 1), ThemeManager.getBorderColor(this));
        bg.setCornerRadius(UiArchitect.dpToPx(this, 12));
        card.setBackground(bg);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.bottomMargin = UiArchitect.dpToPx(this, 12);
        card.setLayoutParams(cardLp);

        // Title and Radius Pill
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText("🛡️ " + zone.name);
        tvName.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvName.setTextSize(UiArchitect.spToPx(this, 15f));
        tvName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        topRow.addView(tvName, nameLp);

        TextView tvRadius = UiArchitect.createStatusPill(this, (int) zone.radiusMeters + "m RADIUS", "#3B82F6");
        topRow.addView(tvRadius);
        card.addView(topRow);

        // Coordinates & Alerts
        TextView tvCoords = new TextView(this);
        tvCoords.setText("Center: " + String.format(Locale.US, "%.5f", zone.latitude) + ", " + String.format(Locale.US, "%.5f", zone.longitude));
        tvCoords.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvCoords.setTextSize(UiArchitect.spToPx(this, 11f));
        tvCoords.setPadding(0, UiArchitect.dpToPx(this, 6), 0, UiArchitect.dpToPx(this, 4));
        card.addView(tvCoords);

        TextView tvAlerts = new TextView(this);
        tvAlerts.setText("Triggers: " + (zone.notifyEntry ? "🟢 Entry Alert" : "⚪ Entry Off") + " • " + (zone.notifyExit ? "🔴 Exit Alert" : "⚪ Exit Off"));
        tvAlerts.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvAlerts.setTextSize(UiArchitect.spToPx(this, 11f));
        card.addView(tvAlerts);

        // Action Buttons: Edit and Delete
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, UiArchitect.dpToPx(this, 10), 0, 0);

        Button btnEdit = UiArchitect.createSecondaryButton(this, "✏️ EDIT", v -> showSafeZoneDialog(zone));
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        editLp.rightMargin = UiArchitect.dpToPx(this, 8);
        btnRow.addView(btnEdit, editLp);

        Button btnDelete = UiArchitect.createDestructiveButton(this, "✕ REMOVE", v -> {
            UiArchitect.showConfirmationDialog(this,
                "Delete SafeZone",
                "Delete the \"" + zone.name + "\" SafeZone perimeter?",
                "DELETE", "CANCEL",
                () -> {
                    DatabaseHelper.getInstance(this).deleteSafeZone(zone.zoneId);
                    refreshZones();
                    UiArchitect.showSlideNotificationBanner(this, "SafeZone Removed", zone.name + " deleted", true, 2500, null, null);
                }, null);
        });
        btnRow.addView(btnDelete, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        card.addView(btnRow);
        return card;
    }

    private void showSafeZoneDialog(SafeZoneModel existingZone) {
        boolean isEdit = existingZone != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEdit ? "Edit SafeZone Perimeter" : "Create SafeZone Perimeter");

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = UiArchitect.dpToPx(this, 20);
        form.setPadding(pad, pad, pad, pad);

        EditText etName = UiArchitect.createInputField(this, "Zone Name (e.g. Home, School, Office)", InputType.TYPE_CLASS_TEXT);
        if (isEdit) etName.setText(existingZone.name);
        form.addView(etName);

        // Center Coordinates
        double defLat = isEdit ? existingZone.latitude : (currentLat != 0.0 ? currentLat : 0.0);
        double defLng = isEdit ? existingZone.longitude : (currentLng != 0.0 ? currentLng : 0.0);

        LinearLayout latLngRow = new LinearLayout(this);
        latLngRow.setOrientation(LinearLayout.HORIZONTAL);
        latLngRow.setPadding(0, UiArchitect.dpToPx(this, 8), 0, 0);

        EditText etLat = UiArchitect.createInputField(this, "Latitude (-90 to 90)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        etLat.setText(String.format(Locale.US, "%.5f", defLat));
        LinearLayout.LayoutParams latLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        latLp.rightMargin = UiArchitect.dpToPx(this, 6);
        latLngRow.addView(etLat, latLp);

        EditText etLng = UiArchitect.createInputField(this, "Longitude (-180 to 180)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        etLng.setText(String.format(Locale.US, "%.5f", defLng));
        latLngRow.addView(etLng, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        form.addView(latLngRow);

        Button btnUseGps = UiArchitect.createSecondaryButton(this, "📍 USE CURRENT GPS CENTER", v -> {
            detectCurrentLocation();
            etLat.setText(String.format(Locale.US, "%.5f", currentLat));
            etLng.setText(String.format(Locale.US, "%.5f", currentLng));
            Toast.makeText(this, "Coordinates updated to current GPS fix.", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams gpsLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        gpsLp.topMargin = UiArchitect.dpToPx(this, 6);
        form.addView(btnUseGps, gpsLp);

        EditText etRadius = UiArchitect.createInputField(this, "Radius in meters (50 - 5000)", InputType.TYPE_CLASS_NUMBER);
        etRadius.setText(isEdit ? String.valueOf((int) existingZone.radiusMeters) : "200");
        LinearLayout.LayoutParams radLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        radLp.topMargin = UiArchitect.dpToPx(this, 10);
        form.addView(etRadius, radLp);

        CheckBox cbEntry = new CheckBox(this);
        cbEntry.setText("Alert upon Entry (Arrival)");
        cbEntry.setChecked(isEdit ? existingZone.notifyEntry : true);
        cbEntry.setTextColor(ThemeManager.getPrimaryTextColor(this));
        form.addView(cbEntry);

        CheckBox cbExit = new CheckBox(this);
        cbExit.setText("Alert upon Exit (Departure)");
        cbExit.setChecked(isEdit ? existingZone.notifyExit : true);
        cbExit.setTextColor(ThemeManager.getPrimaryTextColor(this));
        form.addView(cbExit);

        builder.setView(form);
        builder.setPositiveButton(isEdit ? "UPDATE SAFEZONE" : "SAVE SAFEZONE", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) name = "SafeZone Perimeter";

            double lat = 0.0;
            double lng = 0.0;
            try {
                lat = Double.parseDouble(etLat.getText().toString().trim());
                lng = Double.parseDouble(etLng.getText().toString().trim());
            } catch (Exception e) {
                Toast.makeText(this, "Invalid Coordinates entered.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) {
                Toast.makeText(this, "Coordinates out of bounds (-90..90, -180..180).", Toast.LENGTH_LONG).show();
                return;
            }

            double rad = 200.0;
            try {
                rad = Double.parseDouble(etRadius.getText().toString().trim());
            } catch (Exception ignored) {}

            if (rad < 50.0 || rad > 5000.0) {
                Toast.makeText(this, "Radius must be between 50m and 5000m.", Toast.LENGTH_LONG).show();
                return;
            }

            String zoneId = isEdit ? existingZone.zoneId : UUID.randomUUID().toString();
            SafeZoneModel zone = new SafeZoneModel(
                zoneId,
                name,
                lat, lng,
                rad,
                cbEntry.isChecked(),
                cbExit.isChecked(),
                isEdit ? existingZone.createdAt : System.currentTimeMillis()
            );

            DatabaseHelper.getInstance(this).insertOrUpdateSafeZone(zone);
            refreshZones();
            UiArchitect.showSlideNotificationBanner(this,
                isEdit ? "SafeZone Updated" : "SafeZone Established",
                name + " (" + (int) rad + "m perimeter)",
                false, 3000, null, null
            );
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }
}
