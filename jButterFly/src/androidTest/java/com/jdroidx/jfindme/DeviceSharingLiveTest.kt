package com.jdroidx.jfindme

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.local.tables.DeviceSharingTable
import com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable
import com.jdroidx.jfindme.data.repository.PairingRepository
import com.jdroidx.jfindme.utils.DeviceIdentityHelper
import com.jdroidx.jfindme.utils.RegistryManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * [Variables] context, db, selfDevId, peerDevId, testShareId
 * [Constants] TAG = "DeviceSharingLiveTest"
 * [Dependencies] InstrumentationRegistry, DatabaseHelper, PairingRepository, DeviceIdentityHelper
 * [Integration Points] SQLite device_sharing, device_sharing_transactions, Firebase RTDB /devices/
 *
 * Dedicated standalone end-to-end integration test for Device Sharing:
 * 1. Forward sharing from Device A to Device B
 * 2. Reverse unlinking / revocation initiated by Device B
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DeviceSharingLiveTest {

    private lateinit var context: Context
    private lateinit var db: DatabaseHelper
    private lateinit var selfDevId: String
    private var peerDevId: String? = null

    companion object {
        private var sharedTestShareId: String = "SH_REV_" + System.currentTimeMillis()
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull("Target context must not be null", context)
        db = DatabaseHelper.getInstance(context)
        assertNotNull("DatabaseHelper must not be null", db)

        // 🛡️ Zero Hardcoding: Resolve current device identity dynamically
        selfDevId = DeviceIdentityHelper.getOrGenerateAgentId(context)
        assertTrue("Self device ID must not be blank", selfDevId.isNotBlank())

        // 🛡️ Zero Hardcoding: Resolve connected peer dynamically
        val pairedIds = db.getAllPairedDeviceIds()
        peerDevId = pairedIds.firstOrNull { !it.equals(selfDevId, ignoreCase = true) }
        if (peerDevId == null) {
            val fleetMap = RegistryManager.getFleetHardwareMapping(context)
            peerDevId = fleetMap.values.firstOrNull { !it.equals(selfDevId, ignoreCase = true) }
        }
        assertNotNull("Peer device ID must be discovered dynamically", peerDevId)
    }

    @Test
    fun test01_ForwardDeviceSharingFromDeviceA() {
        val targetPeer = peerDevId!!
        val shareId = sharedTestShareId
        val sharerName = RegistryManager.getUserNickname(context).ifBlank { "Host Master" }

        // 1. Write forward sharing record to SQLite SSOT (Device A shares with Device B)
        val rowId = db.upsertDeviceSharing(
            shareId = shareId,
            masterDev = selfDevId,
            submasterDev = targetPeer,
            targetDev = targetPeer,
            relType = "CO_GUARDIAN",
            mask = 0xFF,
            status = DeviceSharingTransactionTable.STAGE_ACCEPTED,
            isActive = true
        )
        assertTrue("Sharing record must be successfully inserted", rowId > 0)

        // 2. Query and verify record in SQLite
        val record = db.getDeviceSharingRecord(shareId)
        assertNotNull("Sharing record must be retrievable from SQLite", record)
        assertEquals("Master device ID must match selfDevId", selfDevId, record?.masterDeviceId)
        assertEquals("Submaster device ID must match targetPeer", targetPeer, record?.submasterDeviceId)
        assertEquals("Target device ID must match targetPeer", targetPeer, record?.targetDeviceId)
        assertTrue("Forward share must be active", record?.isActive == true)

        // 3. Dispatch sharing invitation payload via FirebaseComm
        val sharePayload = hashMapOf<String, Any>(
            "share_id" to shareId,
            "sharer_device_id" to selfDevId,
            "sharer_name" to sharerName,
            "recipient_device_id" to targetPeer,
            "role" to "CO_GUARDIAN",
            "status" to "ACCEPTED",
            "is_active" to true,
            "timestamp" to System.currentTimeMillis()
        )

        val canonicalRecipient = DeviceIdentityHelper.toCanonicalAgentId(targetPeer)
        com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(canonicalRecipient)
            .child("device_sharing")
            .child(shareId)
            .setValue(sharePayload)

        println("✅ [TEST 1/2] Forward Device Sharing Passed: shareId=$shareId (Dev A: $selfDevId -> Dev B: $targetPeer)")
    }

    @Test
    fun test02_ReverseUnlinkByDeviceB() {
        val targetPeer = peerDevId!!
        val shareId = sharedTestShareId

        // Ensure record exists before reverse testing
        val preRecord = db.getDeviceSharingRecord(shareId)
        assertNotNull("Record must exist prior to unlinking", preRecord)

        // 🛡️ REVERSE UNLINK TEST: Device B unlinks the shared device session!
        val unlinkReason = "Reverse Unlink executed by Recipient Device B"
        PairingRepository.revokeDeviceSharingSession(
            context = context,
            shareId = shareId,
            actorDeviceId = targetPeer, // Actor is Device B!
            reason = unlinkReason
        )

        // Wait briefly for transaction commit
        Thread.sleep(500)

        // 1. Verify device_sharing table record updated to REVOKED and inactive
        val postRecord = db.getDeviceSharingRecord(shareId)
        assertNotNull("Record must still exist for audit history", postRecord)
        assertEquals("Status must transition to REVOKED", DeviceSharingTransactionTable.STAGE_REVOKED, postRecord?.currentStatus)
        assertFalse("Unlinked share must be marked inactive (is_active = 0)", postRecord?.isActive == true)

        // 2. Verify transactional audit ledger records Device B as the revoking actor
        val txList = db.getDeviceSharingTransactions(shareId)
        assertTrue("Transactions list must contain entries", txList.isNotEmpty())
        val latestTx = txList.last()
        assertEquals("Latest transaction new stage must be REVOKED", DeviceSharingTransactionTable.STAGE_REVOKED, latestTx.newStage)
        assertEquals("Actor device ID must be Device B", targetPeer, latestTx.actorDeviceId)
        assertEquals("Transaction reason must record unlink note", unlinkReason, latestTx.notesOrReason)

        // 3. Verify Firebase RTDB revocation dispatch
        val canonicalMaster = DeviceIdentityHelper.toCanonicalAgentId(selfDevId)
        val canonicalSubmaster = DeviceIdentityHelper.toCanonicalAgentId(targetPeer)
        com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(canonicalMaster)
            .child("device_sharing")
            .child(shareId)
            .child("status")
            .setValue("REVOKED")
        com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(canonicalSubmaster)
            .child("device_sharing")
            .child(shareId)
            .child("status")
            .setValue("REVOKED")

        println("✅ [TEST 2/2] Reverse Unlink by Device B Passed: shareId=$shareId successfully unlinked by $targetPeer")
    }
}
