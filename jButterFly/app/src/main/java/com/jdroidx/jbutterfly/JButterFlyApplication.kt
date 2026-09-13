package com.jdroidx.jbutterfly

import android.app.Application
import com.google.firebase.FirebaseApp
import com.jdroidx.jbutterfly.data.local.DatabaseHelper
import com.jdroidx.jbutterfly.ui.base.ThemeManager
import com.jdroidx.jbutterfly.utils.DeviceIdentityHelper
import com.jdroidx.jbutterfly.utils.SystemLogger

class JButterFlyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize System Logger
        SystemLogger.init(this)
        SystemLogger.i("JButterFlyApplication", "Application launching. Initializing hardware mesh...")

        // 2. Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            SystemLogger.i("JButterFlyApplication", "Firebase successfully initialized.")
        } catch (e: Exception) {
            SystemLogger.e("JButterFlyApplication", "Firebase initialization error", e)
        }

        // 3. Initialize SQLite Database Engine (SSOT)
        DatabaseHelper.getInstance(this)

        // 4. Hardware Identity Sentinel Bootstrap
        val agentId = DeviceIdentityHelper.getOrGenerateAgentId(this)
        val hwKey = DeviceIdentityHelper.getRawHardwareKey(this)
        SystemLogger.i("JButterFlyApplication", "Hardware Identity anchored -> Agent ID: $agentId, Key: $hwKey")

        // 5. Apply Theme
        ThemeManager.applyTheme(this)
    }
}
