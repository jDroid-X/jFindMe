/**
 * Auto-Login Architecture: Enterprise Forensic Entry Node
 * Waterfall Method: Secured Splash Logic & Identity Auditing
 */
package com.jdroidx.jfindme.ui.base;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.jdroidx.jfindme.ui.auth.AuthActivity;
import com.jdroidx.jfindme.ui.map.MapActivity;
import com.jdroidx.jfindme.ui.settings.ProfileActivity;

/**
 * Enterprise Application Entry Node: Native Custom Splash Screen
 * Securely blocks drawing UI artifacts displaying 'M2G GLOBAL' branding
 * while dynamically fading into the AuthActivity.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends Activity {

    private final String TAG = "SplashActivity";
    private final android.os.Handler transitionHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable transitionRunnable;
    private boolean isSyncInProgress = false;
    private int syncRetries = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Build Native Fullscreen Canvas Layer
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#0F1A2A")); // Dark sophisticated blue matching the logo theme
        
        // 🦋 Premium Lottie Hero Injection: The Waving Wings Matrix
        int rawId = getResources().getIdentifier("butterfly_angel", "raw", getPackageName());
        if (rawId != 0) {
            com.airbnb.lottie.LottieAnimationView lottieLoader = new com.airbnb.lottie.LottieAnimationView(this);
            lottieLoader.setAnimation(rawId);
            lottieLoader.setRepeatCount(com.airbnb.lottie.LottieDrawable.INFINITE);
            lottieLoader.setRenderMode(com.airbnb.lottie.RenderMode.HARDWARE);
            lottieLoader.enableMergePathsForKitKatAndAbove(true);
            lottieLoader.setSpeed(1.25f);
            lottieLoader.playAnimation();
            
            // 🚀 MASTER SCALING INTERCEPTOR: Single Source of Truth
            int heroSize = UiArchitect.getResponsiveIconSize(this, "ICON_HERO");
            
            FrameLayout.LayoutParams butterflyParams = new FrameLayout.LayoutParams(heroSize, heroSize);
            butterflyParams.gravity = Gravity.CENTER; 
            root.addView(lottieLoader, butterflyParams);
            
            // Clean silky alpha entrance without matrix/scale distortion fighting Lottie's vector frames
            lottieLoader.setAlpha(0f);
            lottieLoader.animate().alpha(1f).setDuration(300).start();
        }

        setContentView(root);

        // Start Firebase sync if user is logged in to recover profile data seamlessly
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            startBackgroundProfileSync(user.getUid());
        }

        // Android Main Thread Handler locking the Splash UI natively
        transitionRunnable = () -> {
            if (isFinishing() || isDestroyed()) return;
            
            if (isSyncInProgress && syncRetries < 5) {
                syncRetries++;
                com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Profile sync still in progress. Delaying transition retry: " + syncRetries);
                transitionHandler.postDelayed(transitionRunnable, 500);
                return;
            }
            isSyncInProgress = false; // 🛡️ Offline-First Fallback: Release sync latch and continue with local SQLite cache
            
            com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Splash screen transition triggered.");
            
            // 🚀 V57 TIERED VALIDATION: Verify Hardware Fingerprint Integrity
            com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
            com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            
            Intent nextIntent;
            if (currentUser != null) {
                com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Identity Detected. Verifying Forensic Integrity.");
                
                String agentId = db.resolveAgentIdLocally(currentUser.getUid());
                if (agentId == null) agentId = ""; // Fallback for fresh installation
                
                boolean bypassNudge = getSharedPreferences("system_registry", MODE_PRIVATE).getBoolean("profile_bypass_nudge", false);
                
                if (!bypassNudge && (agentId.isEmpty() || !db.isProfileComplete(agentId))) {
                    com.jdroidx.jfindme.utils.SystemLogger.w(TAG, "Incomplete Profile Detected. Redirecting to Identity Hub.");
                    nextIntent = new Intent(SplashActivity.this, ProfileActivity.class);
                } else {
                    com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Session Verified. Granting Dashboard Access.");
                    nextIntent = new Intent(SplashActivity.this, MapActivity.class);
                }
            } else {
                com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Unauthenticated Hardware. Routing to Auth Hub.");
                nextIntent = new Intent(SplashActivity.this, AuthActivity.class);
            }
            
            nextIntent.putExtra("EXTRA_FROM_SPLASH", true);
            startActivity(nextIntent);
            
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        };
        
        transitionHandler.postDelayed(transitionRunnable, 3500); 
    }

    private void startBackgroundProfileSync(String authUid) {
        isSyncInProgress = true;
        com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Starting background profile recovery for UID: " + authUid);
        com.jdroidx.jfindme.data.remote.FirebaseComm.purgeLegacyAuthUidNodes();
        
        com.jdroidx.jfindme.data.remote.FirebaseComm.getDevicesRef()
            .orderByChild("Identity/auth_uid")
            .equalTo(authUid)
            .limitToFirst(1)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    for (com.google.firebase.database.DataSnapshot child : task.getResult().getChildren()) {
                        String agentId = child.getKey();
                        if (agentId != null && com.jdroidx.jfindme.data.remote.FirebaseComm.isAgentIdValid(agentId)) {
                            com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Resolved agent ID from cloud: " + agentId);
                            fetchCloudProfile(authUid, agentId);
                            return;
                        }
                    }
                }
                com.jdroidx.jfindme.utils.SystemLogger.w(TAG, "Could not resolve agent ID from cloud on startup.");
                isSyncInProgress = false;
            });
    }

    private void fetchCloudProfile(String authUid, String agentId) {
        com.jdroidx.jfindme.data.remote.FirebaseComm.getDevicesRef()
            .child(agentId)
            .child("profile")
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    com.google.firebase.database.DataSnapshot profileSnap = task.getResult();
                    
                    // Parse values
                    String name = profileSnap.child("name").getValue(String.class);
                    if (name == null) name = profileSnap.child("fullName").getValue(String.class);
                    if (name == null) name = profileSnap.child("userName").getValue(String.class);
                    
                    String phone = profileSnap.child("phone").getValue(String.class);
                    String dob = profileSnap.child("dob").getValue(String.class);
                    String gender = profileSnap.child("gender").getValue(String.class);
                    String country = profileSnap.child("country").getValue(String.class);
                    String state = profileSnap.child("state").getValue(String.class);
                    String city = profileSnap.child("city").getValue(String.class);
                    String pincode = profileSnap.child("pincode").getValue(String.class);
                    String address = profileSnap.child("address").getValue(String.class);
                    String emergencyNote = profileSnap.child("emergency_note").getValue(String.class);
                    if (emergencyNote == null) emergencyNote = profileSnap.child("emergencyNote").getValue(String.class);
                    
                    String orgName = profileSnap.child("org_name").getValue(String.class);
                    if (orgName == null) orgName = profileSnap.child("orgName").getValue(String.class);
                    String deptName = profileSnap.child("dept_name").getValue(String.class);
                    if (deptName == null) deptName = profileSnap.child("deptName").getValue(String.class);
                    
                    String rawActiveGroup = profileSnap.child("active_group").getValue(String.class);
                    String activeGroup = rawActiveGroup;
                    if (activeGroup == null || activeGroup.trim().isEmpty() || "DEFAULT_GROUP".equals(activeGroup)) {
                        activeGroup = "GRP_" + agentId;
                        com.jdroidx.jfindme.data.remote.FirebaseComm.INSTANCE.getDevicesRef().child(agentId).child("profile").child("active_group").setValue(activeGroup);
                        com.jdroidx.jfindme.data.remote.FirebaseComm.INSTANCE.getRelationshipsRef().child(activeGroup).child("members").child(agentId).child("role").setValue("OWNER");
                    }
                    
                    String avatarId = profileSnap.child("avatar_id").getValue(String.class);
                    if (avatarId == null) avatarId = "1";
                    
                    // Map profile
                    java.util.Map<String, String> profileData = new java.util.HashMap<>();
                    profileData.put("name", name != null ? name : agentId);
                    profileData.put("phone", phone != null ? phone : "");
                    profileData.put("dob", dob != null ? dob : "");
                    profileData.put("gender", gender != null ? gender : "");
                    profileData.put("country", country != null ? country : "");
                    profileData.put("state", state != null ? state : "");
                    profileData.put("city", city != null ? city : "");
                    profileData.put("pincode", pincode != null ? pincode : "");
                    profileData.put("address", address != null ? address : "");
                    profileData.put("emergencyNote", emergencyNote != null ? emergencyNote : "");
                    profileData.put("avatar_id", avatarId);
                    profileData.put("active_group", activeGroup);
                    
                    java.util.Map<String, String> orgData = new java.util.HashMap<>();
                    orgData.put("orgName", orgName != null ? orgName : "");
                    orgData.put("deptName", deptName != null ? deptName : "");
                    orgData.put("userAddress", address != null ? address : "");
                    
                    java.util.Map<String, String> groupData = new java.util.HashMap<>();
                    groupData.put("group_id", activeGroup);
                    groupData.put("group_name", "Primary Circle");
                    
                    // Save to SQLite
                    com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(SplashActivity.this);
                    String canonicalAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(SplashActivity.this);
                    String hardwareUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(SplashActivity.this);
                    String androidId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getRawAndroidId(SplashActivity.this);
                    
                    db.executeTieredHandshake(
                        authUid, canonicalAgentId, hardwareUid, androidId,
                        profileData, orgData, null, groupData, null, null, null,
                        0.0, 0.0, null, "ADMIN"
                    );
                    db.pruneZombieNodes(agentId, android.os.Build.MODEL);
                    db.applyTtlDecay(7);
                    
                    // Save locally to SharedPreferences `"system_registry"`
                    android.content.SharedPreferences prefs = getSharedPreferences("system_registry", MODE_PRIVATE);
                    prefs.edit()
                         .putString("agent_id", agentId)
                         .putString("user_nickname", name != null ? name : agentId)
                         .putString("user_phone", phone != null ? phone : "")
                         .putString("user_dob", dob != null ? dob : "")
                         .putString("user_gender", gender != null ? gender : "")
                         .putString("user_country", country != null ? country : "")
                         .putString("user_state", state != null ? state : "")
                         .putString("user_city", city != null ? city : "")
                         .putString("user_pincode", pincode != null ? pincode : "")
                         .apply();
                    
                    com.jdroidx.jfindme.utils.SystemLogger.i(TAG, "Startup profile recovery successfully saved locally!");
                } else {
                    com.jdroidx.jfindme.utils.SystemLogger.w(TAG, "No profile exists in cloud for agent ID: " + agentId);
                }
                isSyncInProgress = false;
            });
    }

    @Override
    protected void onDestroy() {
        if (transitionHandler != null && transitionRunnable != null) {
            transitionHandler.removeCallbacks(transitionRunnable);
        }
        super.onDestroy();
    }
}
