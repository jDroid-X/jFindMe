package com.jdroidx.jfindme.ui.map;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * ============================================================
 * DEPRECATED: LiveMapDashboardActivity — Thin Redirect Shell
 * ============================================================
 * PRIME DIRECTIVE FIX: Duplicate code audit identified that this class
 * was a partial clone of MapActivity with duplicate layout and logic constraints.
 *
 * This class is now a ZERO-LOGIC REDIRECT. All business logic,
 * UI, and Firebase mapping flows have been consolidated into
 * MapActivity (the canonical spatial hub).
 *
 * All AndroidManifest, navigation, and deep-link references to
 * LiveMapDashboardActivity are preserved — this wrapper transparently redirects
 * any incoming intent (including extras) to MapActivity.
 *
 * DO NOT add any logic back to this file.
 * ============================================================
 */
public class LiveMapDashboardActivity extends BaseActivityJava {

    private static final String TAG = "LiveMapDashboard[REDIR]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemLogger.i(TAG, "Redirecting to canonical MapActivity.");
        redirectToCanonicalMap();
    }

    private void redirectToCanonicalMap() {
        Intent original = getIntent();
        Intent redirect = new Intent(this, MapActivity.class);

        if (original != null && original.getExtras() != null) {
            redirect.putExtras(original.getExtras());
        }

        if (original != null && original.getAction() != null) {
            redirect.setAction(original.getAction());
        }

        redirect.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        startActivity(redirect);
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected View createLayout() {
        return new View(this);
    }

    @Override
    protected void setupLogic() {
        // Intentionally empty
    }
}
