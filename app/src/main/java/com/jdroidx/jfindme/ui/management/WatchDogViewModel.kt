package com.jdroidx.jfindme.ui.management

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jdroidx.jfindme.data.local.DatabaseHelper
import kotlin.concurrent.thread

/**
 * 🛰️ WATCHDOG VIEWMODEL (L7 Forensic Matrix)
 * Objective: Reactive monitoring of the System Audit Ledger using the MVVM pattern.
 */
class WatchDogViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseHelper.getInstance(application)

    private val _logs = MutableLiveData<List<Map<String, Any>>>()
    val logs: LiveData<List<Map<String, Any>>> get() = _logs

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun loadLogs(level: String? = null, query: String? = null) {
        _isLoading.postValue(true)
        thread {
            val list = db.getSysLogs(level, query)
            _logs.postValue(list)
            _isLoading.postValue(false)
        }
    }

    fun purgeOldLogs() {
        thread {
            db.purgeSysLogs()
            loadLogs()
        }
    }

    fun clearAllLogs() {
        thread {
            db.clearAllSysLogs()
            loadLogs()
        }
    }
}
