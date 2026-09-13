package com.jdroidx.jbutterfly.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

/**
 * SystemLogger: Enterprise-grade logging with standardized tagging and asynchronous file append.
 */
object SystemLogger {
    private const val GLOBAL_TAG = "jButterFly_SysLog"
    private var appContext: Context? = null
    private val logExecutor = Executors.newSingleThreadExecutor()

    @JvmStatic
    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            i("SystemLogger", "jButterFly Logger initialized. Hardware identity sentinel online.")
        }
    }

    private fun writeDiagnosticLine(level: String, tag: String, msg: String, throwable: Throwable? = null) {
        val ctx = appContext ?: return
        logExecutor.execute {
            try {
                val logDir = File(ctx.getExternalFilesDir(null), "DiagnosticLogs")
                if (!logDir.exists()) logDir.mkdirs()
                val dfDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                val time = dfDate.format(Date())
                val targetFile = File(logDir, "jbutterfly_diagnostic.txt")

                if (targetFile.exists() && targetFile.length() > 2 * 1024 * 1024) {
                    val rotated = File(logDir, "jbutterfly_diagnostic_old.txt")
                    if (rotated.exists()) rotated.delete()
                    targetFile.renameTo(rotated)
                }

                FileWriter(targetFile, true).use { writer ->
                    writer.append("[$time] [$level] [$tag] $msg\n")
                    if (throwable != null) {
                        writer.append(Log.getStackTraceString(throwable))
                        writer.append("\n")
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    @JvmStatic
    fun v(tag: String, msg: String) {
        Log.v(GLOBAL_TAG, "[$tag] $msg")
        writeDiagnosticLine("VERBOSE", tag, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        Log.d(GLOBAL_TAG, "[$tag] $msg")
        writeDiagnosticLine("DEBUG", tag, msg)
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        Log.i(GLOBAL_TAG, "[$tag] $msg")
        writeDiagnosticLine("INFO", tag, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        Log.w(GLOBAL_TAG, "[$tag] $msg")
        writeDiagnosticLine("WARN", tag, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        Log.e(GLOBAL_TAG, "[$tag] $msg", tr)
        writeDiagnosticLine("ERROR", tag, msg, tr)
    }
}
