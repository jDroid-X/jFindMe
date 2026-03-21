package com.jdroidx.jfindme.ui.group

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.engine.sync.DevicePairingEngine
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * Acts as the precise architectural bridge between the UI (GroupActivity)
 * and the Core DevicePairingEngine. Resolves the missing execution logic natively.
 */
class GroupViewModel : ViewModel() {

    private val TAG = "GroupViewModel_Bridge"

    val pairingCode = MutableLiveData<String>()
    val deviceId = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()
    val isLinked = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()

    init {
        // Expose physical hardware ID for debug/tracking reference
        val hardwareId = FirebaseAuth.getInstance().currentUser?.uid ?: "U123"
        deviceId.value = hardwareId
        SystemLogger.i(TAG, "Hardware Identity Booted: $hardwareId")
    }

    /**
     * Executes the Generator role (Parent).
     * Secures a 6-digit grouping token and broadcasts it back to the Java UI Observer.
     */
    fun startNewPairingSession() {
        isLoading.value = true
        
        DevicePairingEngine.bootstrapNewGroup("Secure Family Hub") { _, generatedCode ->
            isLoading.postValue(false)
            if (generatedCode.isNotEmpty()) {
                pairingCode.postValue(generatedCode) // Java Activity will natively draw the QR Graphic via Writer
            } else {
                errorMessage.postValue("Server failure: Could not generate crypto token.")
            }
        }
    }

    /**
     * Executes the Joiner role (Child).
     * Pushes the manual text entry OR the scanned QR payload into the verifier.
     */
    fun linkToGroup(code: String) {
        val uppercaseCode = code.trim().uppercase()
        if (uppercaseCode.length != 6) {
            errorMessage.value = "Invalid Token Matrix."
            return
        }
        
        isLoading.value = true
        DevicePairingEngine.joinGroupViaToken(uppercaseCode) { success ->
            isLoading.postValue(false)
            if (success) {
                isLinked.postValue(true)
            } else {
                errorMessage.postValue("Authentication Failed. Expired or invalid Pairing Code.")
            }
        }
    }

    fun clearError() {
        errorMessage.value = null
    }
}
