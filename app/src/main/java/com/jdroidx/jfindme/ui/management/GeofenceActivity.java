package com.jdroidx.jfindme.ui.management;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.jdroidx.jfindme.engine.location.GeofenceHelper;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Main -> UI Engine -> ManagementCluster -> GeofenceActivity (L4)
 * Rules: 100% Java Programmatic, Spatial Interaction.
 * Integration: Google Geofencing API Orchestrator.
 */
public class GeofenceActivity extends BaseActivityJava {

    private final String TAG = "GeofenceActivity";
    private GeofenceHelper geofenceHelper;
    private static final int GEOFENCE_PERMISSION_CODE = 200;

    @Override
    protected View createLayout() {
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor());

        // 🚀 Premium Linear Gradient Hero Banner
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(50, 80, 50, 60);
        applyLinearGradient(header, 0f);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(this);
        title.setText("SAFE ZONES");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topRow.addView(title);
        header.addView(topRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Location Fencing & Geographical Boundary Sync");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, 10, 0, 0);
        header.addView(subtitle);

        mainRoot.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(40, 40, 40, 100);

        // --- LAYER 1: ACTIVE BOUNDARIES ---
        content.addView(createSectionHeader("ACTIVE BOUNDARIES"));
        
        content.addView(createGeofenceItem("Home Zone", "Radius: 200m", Color.parseColor("#4CAF50"), v -> {
            geofenceHelper.removeGeofence("Home Zone");
            Toast.makeText(this, "Home Zone Removed", Toast.LENGTH_SHORT).show();
        }));
        
        content.addView(createGeofenceItem("School Zone", "Radius: 500m", getGradientStart(), v -> {
            geofenceHelper.removeGeofence("School Zone");
            Toast.makeText(this, "School Zone Removed", Toast.LENGTH_SHORT).show();
        }));

        // --- LAYER 2: CONFIGURATION ACTIONS ---
        content.addView(createSectionHeader("GEOFENCE ORCHESTRATOR"));
        
        LinearLayout actionCard = new LinearLayout(this);
        actionCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(actionCard, getCardColor(), 30f);
        actionCard.addView(createIconButton("CREATE HOME GEOFENCE", android.R.drawable.ic_menu_add, v -> {
            checkPermissionsAndAdd();
        }));
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, 40);
        content.addView(actionCard, cardLp);

        scroll.addView(content);
        mainRoot.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return mainRoot;
    }

    private void checkPermissionsAndAdd() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
             ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            
            String[] permissions = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q ?
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION} :
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            
            ActivityCompat.requestPermissions(this, permissions, GEOFENCE_PERMISSION_CODE);
        } else {
            // Mock adding a geofence at current location for testing
            geofenceHelper.addGeofence("Home Zone", 18.5204, 73.8567, 200f);
            Toast.makeText(this, "Geofence 'Home Zone' Activated", Toast.LENGTH_SHORT).show();
        }
    }

    private View createGeofenceItem(String name, String detail, int color, View.OnClickListener removeListener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        applySolidCardGeometry(item, getCardColor(), 30f); // Make internal cards!
        
        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        itemLp.setMargins(0, 15, 0, 20);
        item.setLayoutParams(itemLp);
        
        View indicator = new View(this);
        indicator.setBackgroundColor(color);
        item.addView(indicator, new LinearLayout.LayoutParams(15, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(40, 40, 40, 40);
        text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(getTextColor());
        tvName.setTextSize(16);
        tvName.setTypeface(null, Typeface.BOLD);
        text.addView(tvName);

        TextView tvDetail = new TextView(this);
        tvDetail.setText(detail);
        tvDetail.setTextColor(getSecondaryTextColor()); // Premium color tie!
        tvDetail.setTextSize(12);
        tvDetail.setPadding(0, 10, 0, 0);
        text.addView(tvDetail);

        item.addView(text);

        ImageView delete = new ImageView(this);
        delete.setImageResource(android.R.drawable.ic_menu_delete);
        delete.setColorFilter(Color.parseColor("#F44336")); // Stronger Android Native Red for contrast
        delete.setOnClickListener(removeListener);
        delete.setPadding(20, 20, 20, 20);
        item.addView(delete, new LinearLayout.LayoutParams(110, 110));

        return item;
    }

    private View createSectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart()); // Native Theme Bind
        header.setTextSize(11);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(10, 40, 0, 20);
        return header;
    }

    @Override
    protected void setupLogic() {
        geofenceHelper = new GeofenceHelper(this);
        SystemLogger.i(TAG, "Geofence Orchestrator standby.");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GEOFENCE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkPermissionsAndAdd();
        }
    }
}
