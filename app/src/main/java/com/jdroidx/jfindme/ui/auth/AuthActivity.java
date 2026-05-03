package com.jdroidx.jfindme.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import com.jdroidx.jfindme.R;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.map.MapActivity;
import com.jdroidx.jfindme.utils.SystemLogger;
import java.util.concurrent.atomic.AtomicBoolean;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

/**
 * 🏛️ AuthActivity: ButterFly Secure GateWay (L7 Enterprise Stable)
 * RESTORED: Matches 11:53 PM Production Snapshot.
 * Features: Butterfly Header, jButterFly Auth Card, Multi-Action Grid.
 */
public class AuthActivity extends BaseActivityJava {

    private AuthViewModel viewModel;
    private final String TAG = "AuthActivity_Secure";
    private EditText etNick, etEmail, etPass;
    private EditText etLat, etLng, etAddr;
    private TextView tvLocationBaseline;
    private AtomicBoolean isFinalizing = new AtomicBoolean(false);

    @Override
    protected View createLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getPrimaryColor()); // 🚀 QA Fix: Master Theme Sync Override

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // --- 🦋 HEADER: ButterFly Secure GateWay ---
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        
        android.graphics.drawable.GradientDrawable headBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{getGradientStart(), getGradientEnd()}
        );
        header.setBackground(headBg);
        header.setPadding(0, scale(60), 0, scale(40));

        // Butterfly Animation Engine
        LottieAnimationView lottie = new LottieAnimationView(this);
        int rawId = getResources().getIdentifier("butterfly_angel", "raw", getPackageName());
        if (rawId != 0) {
            lottie.setAnimation(rawId);
            lottie.setRepeatCount(LottieDrawable.INFINITE);
            lottie.playAnimation();
        } else {
            lottie.setImageResource(android.R.drawable.ic_menu_compass); // Standard Fallback
        }
        int lSize = scale(220);
        header.addView(lottie, new LinearLayout.LayoutParams(lSize, (int)(lSize * 0.7f)));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("ButterFly\nSecure\nGateWay");
        tvTitle.setTextColor(getGradientOverlayText());
        tvTitle.setTextSize(scaleText(22f, true));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setLineSpacing(0, 0.9f);
        header.addView(tvTitle);
        root.addView(header);

        // --- 🏛️ AUTH CARD: jButterFly Authentication ---
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int cPad = getResponsivePaddingQuota();
        card.setPadding(cPad, cPad, cPad, cPad);
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(cPad, -scale(30), cPad, cPad); // Overlap header slightly for premium look
        card.setLayoutParams(cardLp);
        UiArchitect.applyCard(this, card, "CARD_STANDARD");
        card.setBackground(getSolidDrawable(Color.WHITE, 40f));

        TextView tvCardTitle = new TextView(this);
        tvCardTitle.setText("jButterFly Authentication");
        tvCardTitle.setTextColor(getTextColor());
        tvCardTitle.setTextSize(scaleText(18f, true));
        tvCardTitle.setTypeface(null, Typeface.BOLD);
        card.addView(tvCardTitle);

        TextView tvCardSub = new TextView(this);
        tvCardSub.setText("Secure gateway access for active agents.");
        tvCardSub.setTextColor(getSecondaryTextColor());
        tvCardSub.setTextSize(scaleText(12f));
        tvCardSub.setPadding(0, 0, 0, scale(20));
        card.addView(tvCardSub);

        // --- INPUT MATRIX ---
        etNick = createField("Identity Alias", android.R.drawable.ic_menu_myplaces, false);
        card.addView(etNick);

        etEmail = createField("Enterprise Email", android.R.drawable.ic_dialog_email, false);
        card.addView(etEmail);

        etPass = createField("Secure Password", android.R.drawable.ic_lock_idle_lock, true);
        card.addView(etPass);

        // 🧬 V63.6: Resurrected L7 Geolocation Matrix (Baseline Theme Sync)
        LinearLayout geoRow = new LinearLayout(this);
        geoRow.setOrientation(LinearLayout.HORIZONTAL);
        
        etLat = createField("Latitude", android.R.drawable.ic_menu_compass, false);
        etLng = createField("Longitude", android.R.drawable.ic_menu_compass, false);
        
        // 🚀 L7 Calibration: Explicit side-by-side weight distribution
        LinearLayout.LayoutParams geoChildLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        etLat.setLayoutParams(geoChildLp);
        etLng.setLayoutParams(geoChildLp);
        
        geoRow.addView(etLat);
        View geoSpacerNode = new View(this); 
        geoRow.addView(geoSpacerNode, new LinearLayout.LayoutParams(scale(15), 1));
        geoRow.addView(etLng);
        
        // 🧬 Forensic Margin Fix: Standardized L7 Baseline
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, scale(20)); // RESTORED BASELINE MARGIN
        geoRow.setLayoutParams(rowLp);
        card.addView(geoRow);

        etAddr = createField("Current Node Address", android.R.drawable.ic_menu_mapmode, false);
        card.addView(etAddr);

        tvLocationBaseline = new TextView(this);
        tvLocationBaseline.setText("📍 Current Node: Waiting for GNSS...");
        tvLocationBaseline.setTextColor(getSecondaryTextColor());
        tvLocationBaseline.setTextSize(scaleText(11f));
        tvLocationBaseline.setPadding(scale(20), 0, 0, scale(20));
        card.addView(tvLocationBaseline);

        // --- MAIN ACTION: AUTHENTICATE SYSTEM ---
        View btnAuth = createMainButton("AUTHENTICATE SYSTEM", v -> {
            String nick = etNick.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            if (nick.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                showAutoSetupDialog();
                return;
            }
            double lat = 0.0;
            double lng = 0.0;
            try {
                lat = Double.parseDouble(etLat.getText().toString());
                lng = Double.parseDouble(etLng.getText().toString());
            } catch (Exception e) {}
            String addr = etAddr.getText().toString();

            com.jdroidx.jfindme.utils.AngelLoader.show(this);
            viewModel.login(email, pass, nick, true, lat, lng, addr);
        });
        card.addView(btnAuth);

        // --- BOTTOM ROW: Action Trio ---
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, scale(20), 0, 0);
        
        row.addView(createSmallButton("AUTO SETUP", v -> performAutoSetup()));
        row.addView(createSmallButton("SYNC DEVICE", v -> {
            String nick = etNick.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            if (nick.isEmpty() || email.isEmpty()) {
                showAutoSetupDialog();
            } else {
                viewModel.triggerManualStart(); // 🚀 Force Sync/Start
                startActivity(new Intent(this, com.jdroidx.jfindme.ui.group.NetworkGatewayActivity.class));
            }
        }));

        // 🧬 V63.2: L7 Full Library Cycle - Safe Recreate Integration
        row.addView(createSmallButton("UI THEME", v -> {
            int current = prefs.getInt("theme_id", 0);
            prefs.edit().putInt("theme_id", (current + 1) % 8).apply();
            safeRecreate(); // 🛡️ Forensic Stability Patch: Bypasses BinderProxy crash
        }));

        card.addView(row);
        root.addView(card);

        scroll.addView(root);
        return scroll;
    }

    private EditText createField(String hint, int iconRes, boolean isPassword) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(Color.LTGRAY);
        et.setTextColor(getTextColor());
        et.setPadding(35, 30, 35, 30);
        et.setCompoundDrawablePadding(25);
        et.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, isPassword ? android.R.drawable.ic_menu_view : 0, 0);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            et.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(getGradientStart()));
        }

        if (isPassword) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#F0F0F0"));
        bg.setStroke(3, Color.parseColor("#E0E0E0"));
        bg.setCornerRadius(20f);
        et.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, scale(15));
        et.setLayoutParams(lp);
        return et;
    }

    private View createMainButton(String text, View.OnClickListener listener) {
        return createEnterpriseButton(text, 18f, 140, true, listener);
    }

    private View createSmallButton(String text, View.OnClickListener listener) {
        return createEnterpriseButton(text, 14f, 110, false, listener);
    }

    /**
     * 🚀 Master Unified Button Factory (L7 Architect Matrix)
     * Consolidates redundant GradientDrawable and LayoutParams logic.
     */
    private View createEnterpriseButton(String text, float tSize, int hQuo, boolean isMain, View.OnClickListener listener) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setGravity(Gravity.CENTER);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setTextSize(scaleText(tSize));
        btn.setPadding(isMain ? 30 : 10, 0, isMain ? 30 : 10, 0);
        btn.setOnClickListener(listener);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            isMain ? new int[]{getGradientStart(), getGradientEnd()} : new int[]{getGradientMid(), getGradientEnd()}
        );
        gd.setCornerRadius(isMain ? 20f : 15f);
        btn.setBackground(gd);
        if (isMain) btn.setElevation(15f);

        int bHeight = scale(hQuo);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(isMain ? ViewGroup.LayoutParams.MATCH_PARENT : 0, bHeight, isMain ? 0 : 1.0f);
        lp.setMargins(isMain ? 0 : 5, isMain ? scale(20) : 0, isMain ? 0 : 5, 0);
        btn.setLayoutParams(lp);

        if (isMain) {
            btn.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_play, 0, 0, 0);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                btn.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(getGradientOverlayText()));
            }
        }

        return btn;
    }

    private void performAutoSetup() {
        String hwId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        if (hwId == null || hwId.length() < 8) hwId = "AGENT_MASTER_NODE_L7";
        String sig = (hwId.length() >= 8) ? (hwId.substring(0, 4) + hwId.substring(hwId.length() - 4)).toLowerCase() : hwId.toLowerCase();
        
        if (etNick != null) etNick.setText("Agent_" + sig.toUpperCase());
        if (etEmail != null) etEmail.setText("node_" + sig + "@jfindme.net");
        if (etPass != null) etPass.setText("PassCode" + sig + "!");
        
        // 🚀 QA Fix: Resolve Geographic Anchor for Registration Field
        try {
            android.location.LocationManager lm = (android.location.LocationManager) getSystemService(android.content.Context.LOCATION_SERVICE);
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
                return;
            }
            
            // 🧬 Multi-Provider Probe Sequence (V66.2 Optimization)
            android.location.Location last = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            if (last == null) last = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
            if (last == null) last = lm.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER);
            
            if (last != null) {
                updateGeoMatrix(last);
            } else {
                // 📡 Active Burst: Request fresh update if last known is null (Cold Start)
                SystemLogger.i(TAG, "Cold Start: Requesting fresh GNSS/Network baseline.");
                if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                    lm.requestSingleUpdate(android.location.LocationManager.NETWORK_PROVIDER, loc -> updateGeoMatrix(loc), null);
                } else if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    lm.requestSingleUpdate(android.location.LocationManager.GPS_PROVIDER, loc -> updateGeoMatrix(loc), null);
                }
            }
        } catch (Exception e) {
            if (tvLocationBaseline != null) tvLocationBaseline.setText("📍 Location Sync Denied");
            SystemLogger.e(TAG, "Auto-Setup Location Probe Failure", e);
        }

        SystemLogger.i(TAG, "Forensic Auto-Setup Executed. Identity Matrix Synced.");
        Toast.makeText(this, "IDENTITY MATRIX SYNCED", Toast.LENGTH_SHORT).show();
    }

    private void showAutoSetupDialog() {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle("AUTHENTICATION INTERCEPT")
            .setMessage("Forensic credentials not detected. Would you like to execute AUTO SETUP to instantly sync default local node details?")
            .setPositiveButton("AUTO SETUP", (d, w) -> performAutoSetup())
            .setNegativeButton("CANCEL", null)
            .show();
        applyDialogTheme(dialog);
    }

    @Override
    protected void onSaveCurrentState(Bundle outState) {
        if (etNick != null) outState.putString("SAVED_NICK", etNick.getText().toString());
        if (etEmail != null) outState.putString("SAVED_EMAIL", etEmail.getText().toString());
        if (etPass != null) outState.putString("SAVED_PASS", etPass.getText().toString());
    }

    @Override
    protected void onUiRebuilt(Bundle savedState) {
        if (savedState != null) {
            String nick = savedState.getString("SAVED_NICK", "");
            String email = savedState.getString("SAVED_EMAIL", "");
            String pass = savedState.getString("SAVED_PASS", "");
            if (etNick != null) etNick.setText(nick);
            if (etEmail != null) etEmail.setText(email);
            if (etPass != null) etPass.setText(pass);
        }
    }

    @Override
    protected void setupLogic() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 🧬 V63.5: Reactive Geolocation Auto-Sync (Display as per current location)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            try {
                if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
                } else {
                    probeLocation();
                }
            } catch (Exception e) {
                SystemLogger.e(TAG, "Forensic Location Probe Failed", e);
            }
        }, 2000); // 2.0s Debounce for GNSS stability on fresh install

        viewModel.getUser().observe(this, user -> {
            if (user != null && !isFinalizing.get()) {
                // 💡 UI Update: Show a "START ENGINE" cue if already logged in
                SystemLogger.i(TAG, "Identity Detected. Awaiting Manual Start Trigger.");
            }
        });

        viewModel.isManualStartTriggered().observe(this, triggered -> {
            if (Boolean.TRUE.equals(triggered) && !isFinalizing.get()) {
                com.google.firebase.auth.FirebaseUser user = viewModel.getUser().getValue();
                if (user != null) {
                    finalizeLogin(user, user.getUid());
                }
            }
        });

        viewModel.isLoading().observe(this, loading -> {
            if (isDestroyed() || isFinishing()) return;
            if (loading) com.jdroidx.jfindme.utils.AngelLoader.show(this);
            else com.jdroidx.jfindme.utils.AngelLoader.hide(this);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (isDestroyed() || isFinishing()) return;
            if (error != null) {
                Toast.makeText(this, "AUTHENTICATION FAILURE: " + error, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            probeLocation();
        }
    }

    private void probeLocation() {
        try {
            android.location.LocationManager lm = (android.location.LocationManager) getSystemService(android.content.Context.LOCATION_SERVICE);
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.location.Location last = lm.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER);
                if (last == null) last = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                if (last == null) last = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
                
                if (last != null) {
                    updateGeoMatrix(last);
                } else {
                    // Force a single update if no last known location is available
                    lm.requestSingleUpdate(android.location.LocationManager.NETWORK_PROVIDER, loc -> updateGeoMatrix(loc), null);
                }
            }
        } catch (Exception e) {
            SystemLogger.e(TAG, "Proactive Probe Failed", e);
        }
    }

    private void finalizeLogin(com.google.firebase.auth.FirebaseUser user, String uid) {
        if (isFinalizing.getAndSet(true)) return;
        
        // 🚀 QA Fix: Reset Nudge Bypass on Login Finalization
        getSharedPreferences("system_registry", MODE_PRIVATE).edit().putBoolean("profile_bypass_nudge", false).apply();
        
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "AUTHENTICATING NODE...");
        
        SystemLogger.i(TAG, "Auth Handshake Finalized. Launching Portal...");
        prefs.edit().putString("auth_uid", uid).apply();

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!isDestroyed() && !isFinishing()) {
                // 🚀 QA Fix: Identity Matrix Integrity Gate
                // Check if profile is complete OR if the user chose to 'Save Later' (Bypass Nudge)
                com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
                String agentId = getSharedPreferences("system_registry", MODE_PRIVATE).getString("agent_id", "");
                boolean bypassNudge = getSharedPreferences("system_registry", MODE_PRIVATE).getBoolean("profile_bypass_nudge", false);
                
                if (!bypassNudge && (agentId.isEmpty() || !db.isProfileComplete(agentId))) {
                    SystemLogger.w(TAG, "Incomplete Profile Detected. Redirecting to Identity Hub.");
                    startActivity(new Intent(AuthActivity.this, com.jdroidx.jfindme.ui.settings.ProfileActivity.class));
                } else {
                    SystemLogger.i(TAG, "Profile Verified or Bypassed. Granting Dashboard Access.");
                    startActivity(new Intent(AuthActivity.this, com.jdroidx.jfindme.ui.map.MapActivity.class));
                }
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        }, 800);
    }

    /**
     * 🛰️ V63.5: Multi-Stage Geolocation Resolution Engine
     * Populates Latitude, Longitude, and Address based on the active Node Fix.
     */
    private void updateGeoMatrix(android.location.Location loc) {
        if (loc == null || isDestroyed() || isFinishing()) return;
        
        String latStr = String.format(java.util.Locale.US, "%.6f", loc.getLatitude());
        String lngStr = String.format(java.util.Locale.US, "%.6f", loc.getLongitude());
        
        if (etLat != null) etLat.setText(latStr);
        if (etLng != null) etLng.setText(lngStr);
        
        String locStr = String.format(java.util.Locale.US, "📍 Node Fixed: %s, %s", latStr, lngStr);
        if (tvLocationBaseline != null) tvLocationBaseline.setText(locStr);

        // 🧬 Reverse Geocode: Resolve physical address for etAddr
        if (android.location.Geocoder.isPresent()) {
            new Thread(() -> {
                try {
                    android.location.Geocoder geo = new android.location.Geocoder(this);
                    java.util.List<android.location.Address> addrs = geo.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                    if (addrs != null && !addrs.isEmpty()) {
                        String fullAddr = addrs.get(0).getAddressLine(0);
                        String city = addrs.get(0).getLocality();
                        runOnUiThread(() -> {
                            if (etAddr != null) etAddr.setText(fullAddr);
                            if (tvLocationBaseline != null) tvLocationBaseline.setText(locStr + " [" + city + "]");
                        });
                    }
                } catch (Exception ignored) {}
            }).start();
        }
    }
}
