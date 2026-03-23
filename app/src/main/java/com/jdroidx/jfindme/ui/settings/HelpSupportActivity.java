package com.jdroidx.jfindme.ui.settings;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise Grade Help & FAQ Documentation Matrix
 * Provides fully structured UI indexing with dynamic recursive search natively.
 */
public class HelpSupportActivity extends BaseActivityJava {
    private LinearLayout listContainer;
    private List<View> faqViews = new ArrayList<>();

    @Override
    protected View createLayout() {
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor());

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        TextView title = new TextView(this);
        title.setText("HELP & SUPPORT HUB");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Official Documentation & 24/7 Diagnostics Line");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        header.addView(subtitle);

        mainRoot.addView(header);
        
        // Body Wrapper
        ScrollView scrollView = new ScrollView(this);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(40, 40, 40, 100);

        // 1. Search Bar
        EditText searchBar = new EditText(this);
        searchBar.setHint("Search Knowledge Base (e.g., 'Pairing', 'Map')...");
        searchBar.setHintTextColor(getSecondaryTextColor());
        searchBar.setTextColor(getTextColor());
        searchBar.setTextSize(13);
        searchBar.setPadding(40, 40, 40, 40);
        applySolidCardGeometry(searchBar, getCardColor(), 25f);
        
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.setMargins(0, 0, 0, 40);
        wrapper.addView(searchBar, searchLp);

        // 🚀 QA Fix: Dynamically reads the Intent Hash to instantly surface Contextual Documentation exactly for the Screen they tapped from!
        String autoQuery = getIntent().getStringExtra("AUTO_SEARCH_QUERY");
        if (autoQuery != null && !autoQuery.isEmpty()) {
            String mappedQuery = autoQuery;
            if (autoQuery.equals("SystemLog")) mappedQuery = "System Audit Logs";
            else if (autoQuery.equals("Geofence")) mappedQuery = "Safe Zones";
            else if (autoQuery.equals("Privacy")) mappedQuery = "Privacy Locks";
            else if (autoQuery.equals("Settings") || autoQuery.equals("UiConfig")) mappedQuery = "Theme Engine";
            else if (autoQuery.equals("Profile") || autoQuery.equals("AvatarEditor") || autoQuery.equals("AvatarCrop")) mappedQuery = "Custom Avatars";
            else if (autoQuery.equals("FamilyFriends") || autoQuery.equals("PersonDetails")) mappedQuery = "Friends Roster";
            else if (autoQuery.equals("History")) mappedQuery = "Location History";
            else if (autoQuery.equals("Notif")) mappedQuery = "Panic Alerts";
            else if (autoQuery.equals("Auth")) mappedQuery = "App Setup";
            else if (autoQuery.equals("Group")) mappedQuery = "Device Pairing";
            
            searchBar.setText(mappedQuery);
        }

        // 2. Enterprise Support Contact Card
        LinearLayout contactCard = new LinearLayout(this);
        contactCard.setOrientation(LinearLayout.VERTICAL);
        contactCard.setPadding(40, 40, 40, 40);
        applySolidCardGeometry(contactCard, getCardColor(), 20f);
        
        TextView cTitle = new TextView(this);
        cTitle.setText("PRIORITY CONTACT DESK");
        cTitle.setTextColor(getGradientStart());
        cTitle.setTextSize(14);
        cTitle.setTypeface(null, Typeface.BOLD);
        contactCard.addView(cTitle);
        
        TextView cBody = new TextView(this);
        cBody.setText("Direct Line: 1-800-FINDME-CORP\nEmail: enterprise-support@jfindme.net\nHours: 24/7 Global Matrix Coverage");
        cBody.setTextColor(getTextColor());
        cBody.setTextSize(12);
        cBody.setPadding(0, 20, 0, 0);
        cBody.setLineSpacing(5f, 1f);
        contactCard.addView(cBody);

        LinearLayout.LayoutParams contactLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contactLp.setMargins(0, 0, 0, 40);
        wrapper.addView(contactCard, contactLp);

        // 3. Dynamic FAQ & Screen Guides List
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        
        buildHelpCards();

        for (View v : faqViews) {
            listContainer.addView(v);
        }
        
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterFaq(s.toString().toLowerCase()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        wrapper.addView(listContainer);
        scrollView.addView(wrapper);
        
        mainRoot.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return mainRoot;
    }

    private void filterFaq(String query) {
        for (View v : faqViews) {
            String textContent = (String) v.getTag();
            if (query.isEmpty() || textContent.contains(query)) {
                v.setVisibility(View.VISIBLE);
            } else {
                v.setVisibility(View.GONE);
            }
        }
    }

    private void buildHelpCards() {
        faqViews.add(createFaqCard("Screen: App Setup & Profile Login", 
            "Functionality: Secures your raw native identity and syncs Firebase Node parameters.\n\n" +
            "• Step 1: Open App. Provide Phone & Nickname securely.\n" +
            "• Step 2: Allow OS Hardware Permissions if prompted.\n" +
            "• Step 3: Advance automatically into the Main Dashboard Hub."
        ));

        faqViews.add(createFaqCard("Screen: Device Pairing & Family Sync", 
            "Functionality: Bridges physical devices into a unified P2P network architecture.\n\n" +
            "• Step 1: Tap 'Sync Devices' on the Dashboard grid.\n" +
            "• Step 2: The Host generates a cryptographic 6-digit Code (or QR Vector).\n" +
            "• Step 3: The Target device visually scans or types the code.\n" +
            "• Step 4: The System natively resolves dependencies and merges the Family Network Array."
        ));

        faqViews.add(createFaqCard("Screen: Full Live Map Engine", 
            "Functionality: Real-time telemetry tracking utilizing absolute GPS coordinates dynamically rendered.\n\n" +
            "• Step 1: Access the Map natively from the top Dashboard.\n" +
            "• Step 2: Observe Live Avatars. Tap any Pin to pull up exact Geolocation data.\n" +
            "• Step 3: Expand Map using Action Tiles to render unified Family trace paths."
        ));

        faqViews.add(createFaqCard("Screen: Safe Zones (Geofencing)", 
            "Functionality: Sets strict physical boundaries that actively monitor dependents.\n\n" +
            "• Step 1: Open Safe Zones via Dashboard Navigation.\n" +
            "• Step 2: Drag Map to drop the target Radius Anchor Pin.\n" +
            "• Step 3: Adjust the physical Radius (e.g., 200m) required to trigger Sirens.\n" +
            "• Step 4: Lock the Geofence into Firebase explicitly."
        ));

        faqViews.add(createFaqCard("Screen: Identity Protocol & Privacy Locks", 
            "Functionality: Hardware-level constraints preventing physical tracking abuse.\n\n" +
            "• Step 1: Open 'Privacy Controls' via Settings menu.\n" +
            "• Step 2: Toggle 'Background Scan' limits to throttle tracking when screen sleeps.\n" +
            "• Step 3: Run the 'FULL DIAGNOSTIC' sweep to verify Rooting or OS compromises natively."
        ));

        faqViews.add(createFaqCard("Screen: System Audit Logs", 
            "Functionality: Provides extreme transparency into background configuration caching.\n\n" +
            "• Step 1: Navigate to 'System Audits'.\n" +
            "• Step 2: Tap any Diagnostic Card (e.g., Battery Health, P2P Matrix).\n" +
            "• Step 3: View the 7-Day Storage Array generated on your physical drive.\n" +
            "• Step 4: Tap 'Full Report' to aggregate and export the live tracking logs."
        ));

        // 🚀 QA Fix: Added Missing 5 Core Architecture Screens
        faqViews.add(createFaqCard("Screen: Location History Logs", 
            "Functionality: Reviews the historical GPS trail and breadcrumbs of synced devices.\n\n" +
            "• Step 1: Open 'Location Logs' from the Dashboard Grid.\n" +
            "• Step 2: Select a target user from the dropdown.\n" +
            "• Step 3: Scroll through the timeline to review clustered tracking coordinates."
        ));

        faqViews.add(createFaqCard("Screen: Emergency Panic Alerts (SOS)", 
            "Functionality: Logs and routes critical SOS overrides to all connected members.\n\n" +
            "• Step 1: Open 'Panic Alerts' (Notifications) from the Dashboard Grid.\n" +
            "• Step 2: View the chronological history of all emergency sirens triggered.\n" +
            "• Step 3: Active SOS events bypass Doze mode natively."
        ));

        faqViews.add(createFaqCard("Screen: Profile & Custom Avatars", 
            "Functionality: Configures physical user identity parameters and map markers.\n\n" +
            "• Step 1: Access 'Me' or 'Profile' from the Bottom Navigation bar.\n" +
            "• Step 2: Edit your Display Name or tap your Profile Picture.\n" +
            "• Step 3: Use the Native Editor to crop and sync an image directly to the Map Engine."
        ));

        faqViews.add(createFaqCard("Screen: Family & Friends Roster", 
            "Functionality: Displays a categorized list of all actively linked devices.\n\n" +
            "• Step 1: Open 'Family and Friends' from the Navigation Drawer.\n" +
            "• Step 2: Observe online statuses labeled securely (e.g. 'Father', 'Mother').\n" +
            "• Step 3: Tap a member to configure exact specific granular permissions individually."
        ));

        faqViews.add(createFaqCard("Screen: Global Settings & Theme Engine", 
            "Functionality: Architecture hub for Application colors, Battery tuning, and Overrides.\n\n" +
            "• Step 1: Access 'Settings Hub' from the Map Dashboard.\n" +
            "• Step 2: Override the 'Tracking Resolution' interval or toggle 'Ghost Mode'.\n" +
            "• Step 3: Enter the 'GLOBAL UI ARCHITECT' to reprogram the gradient colors physically across the entire App."
        ));
    }

    private View createFaqCard(String titleStr, String bodyStr) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(40, 40, 40, 40);
        applySolidCardGeometry(row, getCardColor(), 20f);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 30);
        row.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(titleStr);
        tvTitle.setTextColor(getGradientStart()); 
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, Typeface.BOLD);
        row.addView(tvTitle);

        TextView tvBody = new TextView(this);
        tvBody.setText(bodyStr);
        tvBody.setTextColor(getSecondaryTextColor());
        tvBody.setTextSize(12);
        tvBody.setPadding(0, 20, 0, 0);
        tvBody.setLineSpacing(6f, 1f);
        row.addView(tvBody);

        row.setTag((titleStr + " " + bodyStr).toLowerCase());

        return row;
    }

    @Override
    protected void setupLogic() {}
}
