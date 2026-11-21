package com.jdroidx.jfindme

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrantPermissionsInstrumentedTest {
    @Test
    fun grantAllPermissions() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val pkg = "com.jdroidx.jfindme"
        val perms = listOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_PHONE_STATE"
        )
        for (p in perms) {
            try {
                uiAutomation.grantRuntimePermission(pkg, p)
            } catch (e: Exception) {
                println("Could not grant $p: ${e.message}")
            }
        }
    }
}
