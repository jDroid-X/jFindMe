package com.jdroidx.jfindme

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.local.tables.GroupMemberTable
import com.jdroidx.jfindme.data.local.tables.UserIdentityTable
import com.jdroidx.jfindme.ui.base.SecurityDialogManager
import com.jdroidx.jfindme.utils.DeviceIdentityHelper
import com.jdroidx.jfindme.utils.RegistryManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * [Variables] context, db, selfDevId, targetPeerId, childNodeId, activeGroup
 * [Constants] TAG = "UnlinkLiveVerificationTest", CHILD_RIDDHI = "4379A761"
 * [Dependencies] InstrumentationRegistry, DatabaseHelper, SecurityDialogManager, RegistryManager, DeviceIdentityHelper
 * [Integration Points] SQLite user_identity, group_members, SharedPreferences system_registry, Firebase commands
 *
 * Dedicated live verification of symmetrical uncoupling and blast-radius containment:
 * 1. Password verification for admin@1234 (case-insensitive)
 * 2. Pre-unlink baseline integrity check (Child Riddhi 4379A761, Pushpa, Innovant)
 * 3. Standard Unlink execution targeting peer Note 7 (2T929153)
 * 4. Post-unlink verification: Target unlinked and decommissioned; child node 100% intact!
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UnlinkLiveVerificationTest {

    private lateinit var context: Context
    private lateinit var db: DatabaseHelper
    private lateinit var selfDevId: String
    private val targetPeerId = "2T929153"
    private val childNodeId = "4379A761"
    private var activeGroup: String = ""

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull("Context must not be null", context)
        db = DatabaseHelper.getInstance(context)
        assertNotNull("DatabaseHelper must not be null", db)

        selfDevId = DeviceIdentityHelper.getOrGenerateAgentId(context)
        assertTrue("Self device ID must not be blank", selfDevId.isNotBlank())

        activeGroup = RegistryManager.getEffectiveActiveGroup(context)
        assertTrue("Active group must not be blank", activeGroup.isNotBlank())
    }

    @Test
    fun test01_VerifyMasterPurgePasswordCaseInsensitive() {
        // Test case 1: admin@1234
        val resLower = RegistryManager.verifyAndUpgradePurgePhrase(context, "admin@1234")
        assertTrue("admin@1234 must be verified successfully", resLower)

        // Test case 2: Admin@1234 (mixed case)
        val resMixed = RegistryManager.verifyAndUpgradePurgePhrase(context, "Admin@1234")
        assertTrue("Admin@1234 must be verified successfully case-insensitively", resMixed)

        // Test case 3: ADMIN@1234 (all caps)
        val resUpper = RegistryManager.verifyAndUpgradePurgePhrase(context, "ADMIN@1234")
        assertTrue("ADMIN@1234 must be verified successfully case-insensitively", resUpper)

        // Test case 4: invalid password
        val resWrong = RegistryManager.verifyAndUpgradePurgePhrase(context, "wrongpass999")
        assertFalse("wrongpass999 must be rejected", resWrong)
    }

    @Test
    fun test02_VerifyPreUnlinkBaselineAndChildNodeIntegrity() {
        // Verify child node Riddhi is intact and active
        val childId = db.resolveInternalId(childNodeId)
        assertTrue("Child node Riddhi 4379A761 must exist in database", childId != -1L)

        // Verify child node is not deleted
        assertFalse("Child node Riddhi must NEVER be marked deleted (Gate 1 Minor Protection)", db.isDeviceDeleted(childNodeId, activeGroup))

        // Verify host device is active
        assertFalse("Host device must NEVER be marked deleted", db.isDeviceDeleted(selfDevId, activeGroup))
    }

    @Test
    fun test03_ExecuteStandardUnlinkAndVerifyScopingContainment() {
        // Execute Standard Unlink targeting targetPeerId (2T929153)
        SecurityDialogManager.performStandardUnlink(context, targetPeerId, "REDMI N7", activeGroup)

        // Verify targetPeerId is now marked deleted in SQLite
        assertTrue("Target peer 2T929153 must be reported deleted by isDeviceDeleted", db.isDeviceDeleted(targetPeerId, activeGroup))

        // Verify targetPeerId is decommissioned in RegistryManager
        assertTrue("Target peer 2T929153 must be registered in decommissioned sentinel", RegistryManager.isAgentIdDecommissioned(context, targetPeerId))

        // Verify targetPeerId is NOT in linked_devices_csv
        val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        val csv = prefs.getString("linked_devices_csv", "") ?: ""
        assertFalse("Target peer 2T929153 must be purged from linked_devices_csv", csv.contains("$targetPeerId,"))

        // 🛡️ CRITICAL BLAST RADIUS CONTAINMENT CHECK:
        // Child node Riddhi (4379A761) MUST REMAIN 100% INTACT!
        assertFalse("Protected Child Node Riddhi 4379A761 must NOT be deleted after unlinking peer!", db.isDeviceDeleted(childNodeId, activeGroup))
        assertFalse("Protected Child Node Riddhi 4379A761 must NOT be decommissioned!", RegistryManager.isAgentIdDecommissioned(context, childNodeId))

        // Host device Note 5 Pro MUST REMAIN 100% INTACT!
        assertFalse("Host device 2C811042 must NOT be deleted after unlinking peer!", db.isDeviceDeleted(selfDevId, activeGroup))
    }
}
