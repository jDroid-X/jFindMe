package com.jdroidx.jfindme.ui.map;

import android.content.Intent;
import android.os.Bundle;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * ============================================================
 * MapActivity2 — Canonical Redirect Bridge
 * ============================================================
 * Consolidates all map rendering into MapActivity (the Single Source of Truth).
 */
public class MapActivity2 extends BaseActivityJava {

    private static final String TAG = "MapActivity2[REDIR]";

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

        redirect.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(redirect);
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected android.view.View createLayout() {
        return new android.view.View(this);
    }

    @Override
    protected void setupLogic() {
        // Zero-logic redirect bridge to canonical MapActivity
    }
}
