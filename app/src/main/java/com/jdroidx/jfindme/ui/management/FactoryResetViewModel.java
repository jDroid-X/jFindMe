package com.jdroidx.jfindme.ui.management;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.work.WorkManager;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.data.remote.FirebaseComm;
import com.jdroidx.jfindme.service.TrackingService;
import com.jdroidx.jfindme.utils.RegistryManager;
import com.jdroidx.jfindme.utils.SystemLogger;
import java.util.*;

/**
 * 🛰️ FACTORY RESET VIEWMODEL (L7 Waterfall Implementation)
 * Objective: Decouple system logic (Purge, Sync, Scan, Teardown) from the View layer.
 * V72: Epoch-Aware Selective Multi-Tier Purge, Graceful Teardown & Scoped Cloud Removal.
 */
public class FactoryResetViewModel extends AndroidViewModel {

    private final DatabaseHelper db;
    public final MutableLiveData<List<TableNode>> tableMatrix = new MutableLiveData<>();
    public final MutableLiveData<String> syncStatus = new MutableLiveData<>();
    public final MutableLiveData<Boolean> rebootTrigger = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public FactoryResetViewModel(Application app) {
        super(app);
        db = DatabaseHelper.getInstance(app);
        loadLocalMatrix();
    }

    public void loadLocalMatrix() {
        isLoading.postValue(true);
        new Thread(() -> {
            try {
                List<TableNode> nodes = new ArrayList<>();
                String[] tables = db.getAllTables();
                for (String t : tables) {
                    syncStatus.postValue("Scanning Table Cluster: " + t.toUpperCase());
                    Thread.sleep(50);
                    nodes.add(new TableNode(t, db.getRecordCount(t), db.getPagedData(t, 3, 0, null)));
                }
                tableMatrix.postValue(nodes);
                syncStatus.postValue("SYNC_COMPLETE");
            } catch (Exception e) {
                syncStatus.postValue("SCAN_FAILED: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void startCloudRecovery() {
        isLoading.postValue(true);
        syncStatus.postValue("INITIALIZING GLOBAL FORENSIC RECOVERY...");
        
        String authUid = FirebaseAuth.getInstance().getUid();
        if (authUid == null) {
            syncStatus.postValue("RECOVERY_FAILED: NO_AUTH");
            isLoading.postValue(false);
            return;
        }

        // 🧬 UNIVERSAL IDENTITY RESOLUTION (Local-First -> Cloud Fallback)
        FirebaseComm.resolveHardwareIdentity(authUid, agentId -> {
            if (agentId == null) {
                syncStatus.postValue("RECOVERY_OFFLINE: IDENTITY_NOT_FOUND");
                isLoading.postValue(false);
                return null;
            }

            syncStatus.postValue("RESOLVED AGENT ID: " + agentId);
            
            // 🚀 MASTER FORENSIC RETRIEVAL: Querying the L7 Unified Device Node
            FirebaseDatabase.getInstance().getReference("devices").child(agentId)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            syncStatus.postValue("PARSING CLOUD DOSSIER...");
                            
                            // Map Profile Tier
                            Map<String, String> profile = new HashMap<>();
                            com.google.firebase.database.DataSnapshot profSnap = snapshot.child("profile");
                            if (profSnap.exists()) {
                                for (com.google.firebase.database.DataSnapshot child : profSnap.getChildren()) {
                                    profile.put(child.getKey(), String.valueOf(child.getValue()));
                                }
                            }

                            // Map Identity Tier
                            Map<String, String> identity = new HashMap<>();
                            com.google.firebase.database.DataSnapshot idSnap = snapshot.child("Identity");
                            if (idSnap.exists()) {
                                for (com.google.firebase.database.DataSnapshot child : idSnap.getChildren()) {
                                    identity.put(child.getKey(), String.valueOf(child.getValue()));
                                }
                            }
                            
                            // Merge into a single map for the handshake
                            profile.putAll(identity);

                            String rawHw = android.provider.Settings.Secure.getString(getApplication().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                            String hardwareUid = "HW_" + (rawHw != null ? rawHw.toUpperCase() : "NODE");

                            db.commitHandshakeDossier(authUid, profile, "NODE", profile.get("name"), agentId, hardwareUid, "RECOVERY_SYNC", "CLOUD");
                            
                            syncStatus.postValue("RECOVERY_COMPLETE: DATA_RESTORED");
                            loadLocalMatrix();
                        } else {
                            syncStatus.postValue("RECOVERY_OFFLINE: NO_CLOUD_NODE");
                        }
                        isLoading.postValue(false);
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                        syncStatus.postValue("RECOVERY_FAILED: " + error.getMessage());
                        isLoading.postValue(false);
                    }
                });
            return null;
        });
    }

    /**
     * 🛡️ Scoped Master Purge: Wipes local database and tenant-owned Firebase branch safely.
     */
    public void executeMasterPurge() {
        isLoading.postValue(true);
        syncStatus.postValue("EXECUTING SCOPED SYSTEM PURGE...");
        
        new Thread(() -> {
            try {
                Context context = getApplication().getApplicationContext();
                String authUid = FirebaseAuth.getInstance().getUid();
                String agentId = RegistryManager.getAgentId(context);

                // 1. Wipe Tenant-Scoped Cloud Node
                if (agentId != null && !agentId.isEmpty()) {
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(agentId).removeValue();
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getHistoryArchiveRef().child(agentId).removeValue();
                    com.jdroidx.jfindme.data.remote.FirebaseComm.cleanupAbandonedPairingSessions(agentId);
                }
                if (authUid != null && !authUid.isEmpty()) {
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getLookupIndicesRef().child("auth_to_device").child(authUid).removeValue();
                }

                // 2. Wipe Local Database Tables
                db.factoryReset();

                // 3. Clear Registry Preferences
                context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).edit().clear().apply();

                syncStatus.postValue("PURGE_SUCCESS");
                rebootTrigger.postValue(true);
            } catch (Exception e) {
                SystemLogger.e("FactoryResetViewModel", "Purge failure", e);
                syncStatus.postValue("PURGE_FAILED: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    /**
     * 🛡️ Scoped Selective Table Purge
     */
    public void executeSelectivePurge(List<String> tables) {
        isLoading.postValue(true);
        syncStatus.postValue("EXECUTING SELECTIVE PURGE...");
        
        new Thread(() -> {
            try {
                Context context = getApplication().getApplicationContext();
                String agentId = RegistryManager.getAgentId(context);
                String authUid = FirebaseAuth.getInstance().getUid();

                for (String t : tables) {
                    // Truncate local table
                    db.truncateTable(t);

                    // Scoped cloud removal for specific entity tables
                    if (t.equalsIgnoreCase("location_event") && agentId != null) {
                        com.jdroidx.jfindme.data.remote.FirebaseComm.getHistoryArchiveRef().child(agentId).removeValue();
                    } else if (t.equalsIgnoreCase("user_profile") && agentId != null) {
                        com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(agentId).removeValue();
                    } else if (t.equalsIgnoreCase("safe_zones") && agentId != null) {
                        com.jdroidx.jfindme.data.remote.FirebaseComm.getSafeZonesRef(agentId).removeValue();
                    }
                }
                syncStatus.postValue("SELECTIVE_PURGE_SUCCESS");
                loadLocalMatrix(); // Force UI refresh
            } catch (Exception e) {
                syncStatus.postValue("PURGE_FAILED: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    /**
     * 🚀 PRE-UNINSTALL GRACEFUL TEARDOWN PROTOCOL
     * Dismantles all active background processes, unregisters OS geofences,
     * cancels WorkManager jobs, revokes cloud FCM token, and prepares node for clean removal.
     */
    public void executeFullSystemTeardown() {
        isLoading.postValue(true);
        syncStatus.postValue("INITIALIZING PRE-UNINSTALL TEARDOWN...");

        new Thread(() -> {
            try {
                Context context = getApplication().getApplicationContext();
                String agentId = RegistryManager.getAgentId(context);
                String authUid = FirebaseAuth.getInstance().getUid();

                // 1. Stop Foreground Tracking Service
                syncStatus.postValue("1/5: Halting Background Tracking Service...");
                try {
                    Intent serviceIntent = new Intent(context, TrackingService.class);
                    context.stopService(serviceIntent);
                } catch (Exception ignored) {}

                // 2. Cancel All WorkManager Tasks
                syncStatus.postValue("2/5: Cancelling Scheduled Workers...");
                try {
                    WorkManager.getInstance(context).cancelAllWork();
                } catch (Exception ignored) {}

                // 3. Remove Active OS Geofences
                syncStatus.postValue("3/5: Removing OS Geofence Registrations...");
                try {
                    Intent geofenceIntent = new Intent("com.jdroidx.jfindme.ACTION_GEOFENCE_EVENT");
                    android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                        context, 0, geofenceIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_MUTABLE
                    );
                    LocationServices.getGeofencingClient(context).removeGeofences(pendingIntent);
                } catch (Exception ignored) {}

                // 4. Revoke Cloud Session & FCM Token
                syncStatus.postValue("4/5: Revoking Cloud Push Tokens...");
                if (agentId != null && !agentId.isEmpty()) {
                    FirebaseDatabase.getInstance().getReference("devices").child(agentId).child("fcm_token").removeValue();
                    FirebaseDatabase.getInstance().getReference("devices").child(agentId).child("status").setValue("UNINSTALLED");
                    FirebaseComm.cleanupAbandonedPairingSessions(agentId);
                }

                // 5. Factory Reset Local Database & Preferences
                syncStatus.postValue("5/5: Clearing Local Ledger & Registry...");
                db.factoryReset();
                context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).edit().clear().apply();

                syncStatus.postValue("TEARDOWN_COMPLETE");
                rebootTrigger.postValue(true);
            } catch (Exception e) {
                SystemLogger.e("FactoryResetViewModel", "Teardown failed", e);
                syncStatus.postValue("TEARDOWN_FAILED: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public static class TableNode {
        public String name;
        public long count;
        public List<Map<String, String>> sample;

        public TableNode(String n, long c, List<Map<String, String>> s) {
            this.name = n;
            this.count = c;
            this.sample = s;
        }
    }
}
