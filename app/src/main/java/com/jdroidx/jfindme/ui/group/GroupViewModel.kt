package com.jdroidx.jfindme.ui.group

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.jdroidx.jfindme.data.local.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 🚀 Enterprise Identity & Grouping ViewModel (L7 Global Architect)
 * Acts as the precise architectural bridge between the UI (GroupActivity)
 * and the Core Identity Matrix. Manages the 5-second Butterfly waterfall.
 */
class GroupViewModel : ViewModel() {

    private val TAG = "GroupViewModel_Bridge"

    val syncStatus = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()
    val rosterData = MutableLiveData<List<Map<String, Any>>>()
    val signalReceived = MutableLiveData<Boolean>()

    /**
     * 🚀 Forensic Role Change Waterfall: Orchestrates the 5s delay and SSOT sync.
     */
    fun updateForensicIdentity(context: android.content.Context, uid: String, myGrp: String, newRole: String, isFriend: Boolean, onComplete: Runnable) {
        isLoading.value = true
        syncStatus.value = "FINALIZING IDENTITY MATRIX..."
        
        // 📚 1. Local Database Settlement (SSOT)
        val db = DatabaseHelper.getInstance(context)
        db.updateRosterGrouping(uid, newRole, isFriend)
        
        // 🦋 2. Mandatory Butterfly Guard (5 Seconds)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            
            // 🛰️ 3. Cloud Propagation (Waterfall)
            val ref = FirebaseDatabase.getInstance().reference
            
            // Step A: Update Group Membership Role
            ref.child("groups").child(myGrp).child("members").child(uid).child("role")
                .setValue(newRole)
                .addOnCompleteListener { task ->
                    
                    // Step B: Inject Real-time Signal (WhatsApp-style)
                    ref.child("users").child(uid).child("signals").child("refresh_roster")
                        .setValue(System.currentTimeMillis())
                        .addOnCompleteListener {
                            isLoading.postValue(false)
                            syncStatus.postValue("NETWORK PARITY SECURED: $newRole")
                            refreshLocalRoster(db)
                            onComplete.run()
                        }
                }
        }, 5000)
    }

    fun refreshLocalRoster(db: DatabaseHelper) {
        viewModelScope.launch(Dispatchers.IO) {
            val roster = db.getOfflineRoster()
            rosterData.postValue(roster)
        }
    }

    fun setupSignalListener(myUid: String) {
        FirebaseDatabase.getInstance().reference
            .child("users").child(myUid).child("signals").child("refresh_roster")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    if (snap.exists()) signalReceived.postValue(true)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
