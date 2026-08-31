package com.jdroidx.jfindme.ui.management

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.engine.EngineRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🛰️ WATCHDOG VIEWMODEL (L7 Forensic Matrix)
 *
 * AUDIT FIX [AUD-012, AUD-006]:
 * - Replaced raw thread{} with viewModelScope.launch(Dispatchers.IO)
 * - Added forensic table audit logic to pull it off the main UI thread.
 */
class WatchDogViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseHelper.getInstance(application)

    private val _logs = MutableLiveData<List<Map<String, Any>>>()
    val logs: LiveData<List<Map<String, Any>>> get() = _logs

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    // Data class for the Audit Matrix
    data class TableStatus(val name: String, val count: Long, val columns: List<String>)
    
    private val _tableStats = MutableLiveData<List<TableStatus>>()
    val tableStats: LiveData<List<TableStatus>> get() = _tableStats
    
    private val _engineStatus = MutableLiveData<String>()
    val engineStatus: LiveData<String> get() = _engineStatus

    fun loadLogs(level: String? = null, query: String? = null) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val list = db.getSysLogs(level, query)
            withContext(Dispatchers.Main) {
                _logs.value = list
                _isLoading.value = false
            }
        }
    }

    fun purgeOldLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            db.purgeSysLogs()
            loadLogs()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            db.clearAllSysLogs()
            loadLogs()
        }
    }
    
    /**
     * Performs a full background sweep of all 14 tables and reads the EngineRegistry heartbeat.
     */
    fun performForensicSweep(tablesToAudit: List<String>) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val stats = mutableListOf<TableStatus>()
            for (table in tablesToAudit) {
                val count = db.getRecordCount(table)
                val columns = db.getTableColumns(table)
                stats.add(TableStatus(table, count, columns))
            }
            val engineReport = EngineRegistry.getStatusReport()
            
            withContext(Dispatchers.Main) {
                _tableStats.value = stats
                _engineStatus.value = engineReport
                _isLoading.value = false
            }
        }
    }
}
