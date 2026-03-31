package com.jdroidx.jfindme.ui.group

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jdroidx.jfindme.data.repository.PairingRepository
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * Acts as the precise architectural bridge between the UI (GroupActivity)
 * and the Core DevicePairingEngine. Resolves the missing execution logic natively.
 */
class GroupViewModel : ViewModel() {

    private val TAG = "GroupViewModel_Bridge"

    val syncStatus = MutableLiveData<String>()
    val pairingCode = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()

    fun injectGenesisNode(uid: String, callback: (String) -> Unit) {
        isLoading.value = true
        syncStatus.value = "ALLOCATING NEW HOST GROUP..."
        PairingRepository.executeGenesisNode(uid) { newGroupId ->
            isLoading.postValue(false)
            syncStatus.postValue("ROOT HUB ESTABLISHED: $newGroupId")
            callback(newGroupId)
        }
    }

    fun approveFamilyTarget(hostUid: String, hostGroupId: String, targetUid: String, role: String, callback: () -> Unit) {
        isLoading.value = true
        syncStatus.value = "MERGING TARGET INTO FAMILY ARRAY..."
        PairingRepository.executeFamilyHandshake(hostUid, hostGroupId, targetUid, role) {
            isLoading.postValue(false)
            syncStatus.postValue("FAMILY HANDSHAKE COMPLETE.")
            callback()
        }
    }

    fun approveInsulatedFriend(hostUid: String, targetUid: String, callback: () -> Unit) {
        isLoading.value = true
        syncStatus.value = "ESTABLISHING P2P FRIEND LINK..."
        PairingRepository.executeFriendHandshake(hostUid, targetUid) {
            isLoading.postValue(false)
            syncStatus.postValue("INSULATED FRIEND LINK ACTIVE.")
            callback()
        }
    }

    fun clearError() {
        errorMessage.value = null
    }
}
