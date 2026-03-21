package com.jdroidx.jfindme.ui.map;

import android.content.res.Configuration;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Main -> UI Engine -> MapScreen -> MapActivityJava
 * Rules: Java Programmatic, No Buttons (Icons Only), Dual-Pane Landscape, 7 Themes.
 */
public class MapActivityJava extends BaseActivityJava {

    private boolean isDualPaneActive = true;
    private static final int ID_MAP_PANE = View.generateViewId();
    private static final int ID_CONTROL_PANE = View.generateViewId();

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(isLandscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // Level 4: Pane 1 - Map Visualization
        FrameLayout mapPane = new FrameLayout(this);
        mapPane.setId(ID_MAP_PANE);
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, isDualPaneActive ? 0.7f : 1.0f);
        
        if (!isLandscape) {
            mapParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.6f);
        }
        root.addView(mapPane, mapParams);

        // Level 4: Pane 2 - Control/Data Panel (Python Integrated)
        if (isLandscape && isDualPaneActive) {
            root.addView(createControlPanel(), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.3f));
        } else if (!isLandscape) {
            root.addView(createControlPanel(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.4f));
        }

        return root;
    }

    private View createControlPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setId(ID_CONTROL_PANE);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(40, 40, 40, 40);
        panel.setGravity(Gravity.TOP);

        // Toggle Icons for Landscape Mode (Requirement: Single/Double Screen)
        if (isLandscape) {
            View toggleBtn = createIconButton(isDualPaneActive ? "SINGLE SCREEN" : "DOUBLE SCREEN", 
                android.R.drawable.ic_menu_revert, v -> {
                isDualPaneActive = !isDualPaneActive;
                recreate(); // Dynamic UI Refresh
            });
            panel.addView(toggleBtn);
        }

        View sosBtn = createIconButton("SEND SOS ALERT", android.R.drawable.ic_dialog_alert, v -> {
            SystemLogger.INSTANCE.w("MapUI", "SOS Event Broadcasted to Python Data Engine");
        });
        panel.addView(sosBtn);

        return panel;
    }

    @Override
    protected void setupLogic() {
        SystemLogger.INSTANCE.i("MapUI", "Map Logic Initialized with Theme: " + currentThemeId);
        // Integration with MarkerClusterer and LocationEngine via ViewModel
    }
}
