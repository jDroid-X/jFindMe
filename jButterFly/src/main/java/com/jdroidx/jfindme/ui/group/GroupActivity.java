package com.jdroidx.jfindme.ui.group;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * ============================================================
 * DEPRECATED: GroupActivity — Thin Redirect Shell
 * ============================================================
 * PRIME DIRECTIVE FIX: Duplicate code audit identified that this class
 * was a ~105 KB clone of NetworkGatewayActivity with 17 duplicate method
 * blocks causing critical divergences (DB-01 to DB-05, DUP-01 to DUP-17).
 *
 * This class is now a ZERO-LOGIC REDIRECT. All business logic,
 * UI, Firebase sync, and pairing flows have been consolidated into
 * NetworkGatewayActivity (the canonical screen).
 *
 * All AndroidManifest, navigation, and deep-link references to
 * GroupActivity are preserved — this wrapper transparently redirects
 * any incoming intent (including extras) to NetworkGatewayActivity.
 *
 * DO NOT add any logic back to this file.
 * ============================================================
 */
public class GroupActivity extends BaseActivityJava {

    private static final String TAG = "GroupActivity[REDIRECT]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemLogger.i(TAG, "Redirecting to canonical NetworkGatewayActivity.");
        redirectToNetworkGateway();
    }

    /**
     * Transparently forwards this activity's Intent to NetworkGatewayActivity,
     * preserving all extras and flags sent by the original caller.
     */
    private void redirectToNetworkGateway() {
        Intent original = getIntent();
        Intent redirect = new Intent(this, NetworkGatewayActivity.class);

        // Forward all extras from the original intent (e.g., LAUNCH_MODE=RECOVERY)
        if (original != null && original.getExtras() != null) {
            redirect.putExtras(original.getExtras());
        }

        // Preserve the original action if set
        if (original != null && original.getAction() != null) {
            redirect.setAction(original.getAction());
        }

        // Use FLAG_ACTIVITY_REORDER_TO_FRONT to avoid stack duplication
        redirect.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        startActivity(redirect);
        // Finish self immediately — no UI is ever shown in this wrapper
        finish();
        // Disable enter/exit animations for a seamless transparent redirect
        overridePendingTransition(0, 0);
    }

    // -----------------------------------------------------------------------
    // Required abstract method implementations — NO UI is built here.
    // BaseActivityJava calls createLayout() before onCreate redirect fires,
    // so we must return null safely.
    // -----------------------------------------------------------------------

    @Override
    protected View createLayout() {
        // 🔧 FIX ISSUE-02: Must return a non-null View — BaseActivityJava calls setContentView(createLayout())
        // before our redirect fires. A null return causes NullPointerException on API < 30.
        // This View is never actually shown; the redirect fires immediately in onCreate().
        return new View(this);
    }

    @Override
    protected void setupLogic() {
        // Intentionally empty — all logic is in NetworkGatewayActivity
    }
}
