package com.jdroidx.jfindme.utils

import android.util.Log

/**
 * SystemLogger: Enterprise-grade logging with standardized tagging and color-coded simulation.
 * Follows standard programming protocols for system logs.
 */
object SystemLogger {
    private const val GLOBAL_TAG = "jFindMe_SysLog"

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
}
