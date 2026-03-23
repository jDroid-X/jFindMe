package com.jdroidx.jfindme.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * SystemLogger: Enterprise-grade logging with standardized tagging and color-coded simulation.
 * Follows standard programming protocols for system logs.
 */
object SystemLogger {
    private const val GLOBAL_TAG = "jFindMe_SysLog"
    private const val MAX_LOG_AGE_DAYS = 7L

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (com.jdroidx.jfindme.BuildConfig.DEBUG) {
            Log.d("$GLOBAL_TAG:$tag", msg)
        }
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        if (com.jdroidx.jfindme.BuildConfig.DEBUG) {
            Log.i("$GLOBAL_TAG:$tag", msg)
        }
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        if (com.jdroidx.jfindme.BuildConfig.DEBUG) {
            Log.w("$GLOBAL_TAG:$tag", msg)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        if (com.jdroidx.jfindme.BuildConfig.DEBUG) {
            Log.e("$GLOBAL_TAG:$tag", msg, throwable)
        }
    }

    // --- 🚀 PHASE 5: ENTERPRISE PHYSICAL TEXT FILE PROTOCOL ---
    
    @JvmStatic
    fun writeAuditDiagnostic(context: Context, header: String, content: String, isRedAlert: Boolean) {
        try {
            val logDir = File(context.getExternalFilesDir(null), "AuditLogs_7DayMatrix")
            if (!logDir.exists()) logDir.mkdirs()
            purgeLegacyLogs(logDir)

            val dfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dfTime = SimpleDateFormat("HH:mm:ss", Locale.US)
            val now = Date()

            val fileName = "AuditLog_${dfDate.format(now)}.html"
            val targetFile = File(logDir, fileName)
            
            val colorHex = if (isRedAlert) "#FF5252" else "#4CAF50"
            val statusHtml = if (isRedAlert) "<b><font color=\"$colorHex\">[FATAL ALERT DETECTED]</font></b>" else "<b><font color=\"$colorHex\">[SYSTEM SECURE]</font></b>"
            
            val safeContent = content.replace("\n", "<br>")
            
            val htmlEntry = """
                <div style="margin-bottom: 30px;">
                   <small><font color="#AAAAAA">[LOG TIMESTAMP: ${dfDate.format(now)} ${dfTime.format(now)}]</font></small><br>
                   <b><u>[DIAGNOSTIC CATEGORY: $header]</u></b><br>
                   $statusHtml<br><br>
                   <font color="$colorHex">$safeContent</font><br><br>
                   <hr color="#555555" size="1">
                </div>
            """.trimIndent() + "\n"

            val writer = FileWriter(targetFile, true)
            writer.append(htmlEntry)
            writer.flush()
            writer.close()
            
            Log.i("DiskLogger", "Secured Trace to Archive: $fileName")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    @JvmStatic
    fun readConsolidatedAuditLogs(context: Context): String {
        val logDir = File(context.getExternalFilesDir(null), "AuditLogs_7DayMatrix")
        if (!logDir.exists()) return "<b><font color='#FF5252'>[ERROR] Native Directory Missing or Orphaned.</font></b>"
        
        val files = logDir.listFiles()?.filter { it.name.startsWith("AuditLog_") && it.name.endsWith(".html") }?.sortedByDescending { it.name }
        if (files.isNullOrEmpty()) return "<b><font color='#AAAAAA'>[ARCHIVE EMPTY] No Enterprise Logs Generated.</font></b>"

        val masterBuilder = StringBuilder()
        for (f in files) {
            try {
                masterBuilder.append("<h3><font color='#FFFFFF'>FILE SIGNATURE: ${f.name}</font></h3><br>")
                masterBuilder.append(f.readText())
            } catch (e: Exception) {}
        }
        return masterBuilder.toString()
    }

    private fun purgeLegacyLogs(logDir: File) {
        val files = logDir.listFiles() ?: return
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(MAX_LOG_AGE_DAYS)
        for (file in files) {
            if (file.lastModified() < cutoffTime) {
                file.delete()
                Log.w("DiskLogger", "Purged expired 7-Day Asset: ${file.name}")
            }
        }
    }
}
