package com.jdroidx.jfindme.engine.data

import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import android.content.Context
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * SpatialProcessor: Bridge to Python-based Data Engine (Py).
 * Level 4: AnalyticsEngine integration via Chaquopy.
 */
object SpatialProcessor {
    private const val TAG = "SpatialProcessor"

    @JvmStatic
    fun init(context: Context) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
            SystemLogger.i(TAG, "Python Data Engine Initialized.")
        }
    }

    /**
     * Calls data_processor.py -> calculate_distance
     */
    @JvmStatic
    fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return try {
            val py = Python.getInstance()
            val module = py.getModule("data_processor")
            module.callAttr("calculate_distance", lat1, lon1, lat2, lon2).toDouble()
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Python distance calculation failed", e)
            0.0
        }
    }

    /**
     * Calls data_processor.py -> predict_next_point
     */
    @JvmStatic
    fun predictPath(path: List<Map<String, Any>>): Map<String, Double>? {
        return try {
            val py = Python.getInstance()
            val module = py.getModule("data_processor")
            val pyResult = module.callAttr("predict_next_point", path)
            
            if (pyResult == null) return null
            
            val result = pyResult.asMap()
            val prediction = mutableMapOf<String, Double>()
            result.forEach { (k, v) ->
                prediction[k.toString()] = v.toDouble()
            }
            prediction
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Python path prediction failed", e)
            null
        }
    }
}
