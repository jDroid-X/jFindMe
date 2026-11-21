package com.jdroidx.jfindme

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable
import com.jdroidx.jfindme.data.local.tables.DevicePairingTable
import com.jdroidx.jfindme.data.repository.PairingRepository
import com.jdroidx.jfindme.utils.DeviceIdentityHelper
import com.jdroidx.jfindme.utils.RegistryManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * [Variables] context, db, selfDevId, devBId, devCId, childNodeId, myGrp
 * [Constants] TAG = "DeviceSharingMasterMeshLiveTest", TIMEOUT_SEC = 15L
 * [Dependencies] InstrumentationRegistry, DatabaseHelper, PairingRepository, DeviceIdentityHelper, RegistryManager
 * [Integration Points] FamilyFriendsActivity, MapActivity, Firebase RTDB, SQLite device_sharing, device_pairing
 *
 * Master 3-Cycle Live Verification of Device Sharing & Peer Unlink Flow:
 * Device A (Redmi Note 7: 2T929153) selects Device B (Redmi Note 5: 2C811042)
 * to connect to Device C (Pushpa: F8CAFC67) with role "Friend" (Normal Friend).
 * Verifies 3 consecutive full cycles: Connect -> Verify -> Remove -> Verify.
 * Enforces Gate 1 Minor Protection (Child node Riddhi 4379A761 100% intact).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DeviceSharingMasterMeshLiveTest {

    private lateinit var context: Context
    private lateinit var db: DatabaseHelper
    private lateinit var selfDevId: String
    private val devBId = "2C811042" // Redmi Note 5 Pro
    private val devCId = "F8CAFC67" // Pushpa (Redmi Note 10 Pro)
    private val childNodeId = "4379A761" // Riddhi (Ward / Minor)
    private var myGrp: String = ""

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull("Context must not be null", context)
        db = DatabaseHelper.getInstance(context)
        assertNotNull("DatabaseHelper must not be null", db)

        selfDevId = DeviceIdentityHelper.getOrGenerateAgentId(context)
        assertTrue("Self device ID must not be blank", selfDevId.isNotBlank())

        myGrp = RegistryManager.getEffectiveActiveGroup(context)
        assertTrue("Active group must not be blank", myGrp.isNotBlank())

        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        println("🔍 DIAGNOSTIC: FirebaseAuth currentUser = ${currentUser?.uid}, email = ${currentUser?.email}")
    }

    private fun executeConnectAndVerify(cycleNumber: Int) {
        println("🔄 Starting Device Sharing Cycle $cycleNumber: Connecting Dev B ($devBId) ↔ Dev C ($devCId) [Role: Friend]")
        val latch = CountDownLatch(1)
        var connectSuccess = false
        var failureReason: String? = null

        PairingRepository.executeClosedLoopDeviceSharing(
            context = context,
            hostGroupId = myGrp,
            devAId = devBId,
            devBId = devCId,
            roleA = "Friend",
            roleB = "Friend",
            callback = object : PairingRepository.DeviceSharingSyncCallback {
                override fun onSyncDispatched(syncSessionId: String) {
                    println("  📡 Cycle $cycleNumber: Sync session dispatched: $syncSessionId")
                }

                override fun onSyncComplete(devAId: String, devBId: String, latencyMs: Long) {
                    println("  ✅ Cycle $cycleNumber: Sync complete ($devAId ↔ $devBId) in ${latencyMs}ms")
                    connectSuccess = true
                    latch.countDown()
                }

                override fun onSyncTimeout(diagnosticReason: String) {
                    println("  ⚠️ Cycle $cycleNumber: Sync timed out: $diagnosticReason")
                    failureReason = diagnosticReason
                    latch.countDown()
                }

                override fun onSyncFailed(errorMessage: String) {
                    println("  ❌ Cycle $cycleNumber: Sync failed: $errorMessage")
                    failureReason = errorMessage
                    latch.countDown()
                }
            }
        )

        val completed = latch.await(15, TimeUnit.SECONDS)
        assertTrue("Cycle $cycleNumber: Sharing operation must complete within SLA", completed)
        assertTrue("Cycle $cycleNumber: Sharing must succeed: $failureReason", connectSuccess)

        // Verify SQLite Database Records
        val shareId = "DSH_${devBId}_${devCId}"
        val record = db.getDeviceSharingRecord(shareId)
        assertNotNull("Cycle $cycleNumber: Device sharing record must exist in SQLite", record)
        assertEquals("Cycle $cycleNumber: Status must be ACTIVE", DeviceSharingTransactionTable.STAGE_ACTIVE, record?.currentStatus)
        assertTrue("Cycle $cycleNumber: Share record must be active", record?.isActive == true)

        // Verify Gate 1: Minor Protection (Child node Riddhi must be untouched)
        assertFalse("Gate 1: Child node Riddhi must never be marked deleted", db.isDeviceDeleted(childNodeId, myGrp))
        println("  🛡️ Cycle $cycleNumber: Gate 1 passed (Child node $childNodeId is 100% intact)")
    }

    private fun executeUnlinkAndVerify(cycleNumber: Int) {
        println("🗑️ Cycle $cycleNumber: Removing Dev B ($devBId) from Dev C ($devCId)")
        val latch = CountDownLatch(1)
        var unlinkSuccess = false
        var unlinkErr: String? = null

        PairingRepository.unlinkPeerDevices(
            context = context,
            hostGroupId = myGrp,
            devAId = devBId,
            devBId = devCId,
            callback = { success, err ->
                unlinkSuccess = success
                unlinkErr = err
                latch.countDown()
            }
        )

        val completed = latch.await(15, TimeUnit.SECONDS)
        assertTrue("Cycle $cycleNumber: Unlink operation must complete within SLA", completed)
        assertTrue("Cycle $cycleNumber: Unlink must succeed: $unlinkErr", unlinkSuccess)

        // Verify SQLite status transition to REVOKED
        val shareId = "DSH_${devBId}_${devCId}"
        val record = db.getDeviceSharingRecord(shareId)
        assertNotNull("Cycle $cycleNumber: Record must exist for audit", record)
        assertEquals("Cycle $cycleNumber: Status must transition to REVOKED", DeviceSharingTransactionTable.STAGE_REVOKED, record?.currentStatus)
        assertFalse("Cycle $cycleNumber: Record must be marked inactive", record?.isActive == true)

        // Verify Gate 1: Minor Protection
        assertFalse("Gate 1: Child node Riddhi must never be marked deleted", db.isDeviceDeleted(childNodeId, myGrp))
        println("  ✅ Cycle $cycleNumber: Device B successfully removed from Device C. Gate 1 intact.")
    }

    @Test
    fun test01_PreFlightInitialCleanState() {
        // If B and C are already linked, unlink first as instructed
        val connectedToB = db.getConnectedDeviceIds(devBId)
        if (connectedToB.contains(devCId.uppercase())) {
            println("🧹 Pre-flight: Dev B and Dev C already connected. Cleaning up before test...")
            executeUnlinkAndVerify(0)
        } else {
            println("✨ Pre-flight: Dev B and Dev C are not connected. Ready for testing.")
        }
    }

    @Test
    fun test02_Cycle1_ConnectAndRemove() {
        executeConnectAndVerify(1)
        executeUnlinkAndVerify(1)
    }

    @Test
    fun test03_Cycle2_ConnectAndRemove() {
        executeConnectAndVerify(2)
        executeUnlinkAndVerify(2)
    }

    @Test
    fun test04_Cycle3_ConnectAndRemoveFinal() {
        executeConnectAndVerify(3)
        executeUnlinkAndVerify(3)
        println("🎉 ALL 3 CONSECUTIVE CYCLES OF DEVICE SHARING & REMOVAL PASSED PERFECTLY!")
    }
}
