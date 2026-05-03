package com.jdroidx.jfindme.ui.management;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.database.FirebaseDatabase;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import java.util.*;

/**
 * 🛰️ FACTORY RESET VIEWMODEL (L7 Waterfall Implementation)
 * Objective: Decouple system logic (Purge, Sync, Scan) from the View layer.
 * V42: Selective Multi-Tier Purge (Dual-Stack Removal).
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
                    // 🚀 AUDIT: Simulating Forensic Latency for Visibility (300ms)
                    Thread.sleep(300);
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
        
        String authUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (authUid == null) {
            syncStatus.postValue("RECOVERY_FAILED: NO_AUTH");
            isLoading.postValue(false);
            return;
        }

        // 🧬 V69.8: UNIVERSAL IDENTITY RESOLUTION (Local-First -> Cloud Fallback)
        com.jdroidx.jfindme.data.remote.FirebaseComm.resolveHardwareIdentity(authUid, agentId -> {
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
                            
                            // Merge into a single forensic map for the handshake
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

    public void executeMasterPurge() {
        isLoading.postValue(true);
        syncStatus.postValue("EXECUTING FULL SYSTEM PURGE...");
        
        new Thread(() -> {
            try {
                // Wipe Local
                db.factoryReset();
                // 📡 Wipe Cloud (Master Table Roots)
                String[] allMain = db.getAllTables();
                for (String t : allMain) {
                    FirebaseDatabase.getInstance().getReference().child(t).removeValue();
                }
                syncStatus.postValue("PURGE_SUCCESS");
                rebootTrigger.postValue(true);
            } catch (Exception e) {
                syncStatus.postValue("PURGE_FAILED: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void executeSelectivePurge(List<String> tables) {
        isLoading.postValue(true);
        syncStatus.postValue("EXECUTING SELECTIVE PURGE...");
        
        new Thread(() -> {
            try {
                for (String t : tables) {
                    // 🏗️ Dual-Stack Removal
                    db.truncateTable(t);
                    FirebaseDatabase.getInstance().getReference().child(t).removeValue();
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
