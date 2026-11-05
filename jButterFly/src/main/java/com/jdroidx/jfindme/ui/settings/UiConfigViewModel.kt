package com.jdroidx.jfindme.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jdroidx.jfindme.ui.base.DashboardTileTable
import com.jdroidx.jfindme.ui.base.EnterpriseCardTable
import com.jdroidx.jfindme.ui.base.HeaderTable
import com.jdroidx.jfindme.ui.base.IconTable

class UiConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("system_registry", 0)

    private val _themeId = MutableLiveData<Int>()
    val themeId: LiveData<Int> = _themeId

    private val _customRadius = MutableLiveData<Float>()
    val customRadius: LiveData<Float> = _customRadius

    private val _customPadding = MutableLiveData<Int>()
    val customPadding: LiveData<Int> = _customPadding

    private val _iconScale = MutableLiveData<Int>()
    val iconScale: LiveData<Int> = _iconScale

    // ≡ƒÜÇ ENTERPRISE MATRIX WATERFALL: Surfacing internal static registries for UI Observation
    val dashboardTiles = MutableLiveData(DashboardTileTable.getRegistry())
    val enterpriseCards = MutableLiveData(EnterpriseCardTable.getRegistry())
    val iconMatrix = MutableLiveData(IconTable.getRegistry())
    val headerMatrix = MutableLiveData(HeaderTable.getRegistry())
    val inputMatrix = MutableLiveData(com.jdroidx.jfindme.ui.base.FormInputTable.getRegistry())
    val actionButtons = MutableLiveData(com.jdroidx.jfindme.ui.base.ActionButtonTable.getRegistry())
    val navMatrix = MutableLiveData(com.jdroidx.jfindme.ui.base.BottomNavTable.getRegistry())
    val listMatrix = MutableLiveData(com.jdroidx.jfindme.ui.base.ListItemTable.getRegistry())
    val marqueeMatrix = MutableLiveData(com.jdroidx.jfindme.ui.base.MarqueeTable.getRegistry())
    val statusMatrix = MutableLiveData(com.jdroidx.jfindme.ui.base.StatusPillTable.getRegistry())

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _themeId.value = prefs.getInt("theme_id", 0)
        _customRadius.value = prefs.getFloat("custom_geometry_radius", 30f)
        _customPadding.value = prefs.getInt("custom_ui_padding", 40)
        _iconScale.value = prefs.getInt("custom_icon_bounds", 140)
    }

    fun updateTheme(index: Int) {
        prefs.edit().putInt("theme_id", index).apply()
        _themeId.value = index
        updateConfigHash()
    }

    fun updateRadius(radius: Float) {
        prefs.edit().putFloat("custom_geometry_radius", radius).apply()
        _customRadius.value = radius
        updateConfigHash()
    }

    fun updatePadding(padding: Int) {
        prefs.edit().putInt("custom_ui_padding", padding).apply()
        _customPadding.value = padding
        updateConfigHash()
    }

    fun updateIconScale(scale: Int) {
        prefs.edit().putInt("custom_icon_bounds", scale).apply()
        _iconScale.value = scale
        updateConfigHash()
    }
    
    fun updateStage(index: Int) {
        prefs.edit().putInt("stage_override", index).apply()
        updateConfigHash()
    }
    
    private fun updateConfigHash() {
        prefs.edit().putLong("ui_config_hash", System.currentTimeMillis()).apply()
    }
    
    fun factoryReset() {
        prefs.edit().clear().apply()
        updateConfigHash()
    }
}
