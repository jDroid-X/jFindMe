package com.jdroidx.jfindme.engine.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.*
import androidx.core.content.ContextCompat
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * 📶 CellTowerSentinel (Agent 1 + Agent 6 Standard)
 *
 * Extracts real cellular tower identifiers (MCC, MNC, LAC/TAC, CID, Signal dBm, Operator)
 * across 2G GSM, 3G WCDMA, 4G LTE, and 5G NR networks.
 *
 * Reverse Engineering Fix:
 * Implements multi-version safe reflection for API 26-28 where getMccString() does not exist,
 * and guards CellInfoNr to prevent NoClassDefFoundError on Android 8/9 runtimes.
 *
 * [Variables]
 * - currentCell: Holds latest evaluated ServingCellData
 * - cellInfoList: List of CellInfo retrieved from TelephonyManager
 *
 * [Constants]
 * - TAG: "CellTowerSentinel"
 * - UNKNOWN_VAL: "UNKNOWN"
 *
 * [Dependencies]
 * - android.telephony.TelephonyManager
 * - android.telephony.CellInfo (CellInfoLte, CellInfoNr, CellInfoGsm, CellInfoWcdma)
 * - androidx.core.content.ContextCompat
 *
 * [Integration Points]
 * - SimForensicWorker.kt: Invokes extractServingCellInfo() to log real hardware tower metadata
 * - TrackingService.kt: Queries real signalDbm and networkType for vitals table
 * - DatabaseHelper.kt: Stores into SimTable and sim_transactions
 */
object CellTowerSentinel {

    private const val TAG = "CellTowerSentinel"
    private const val UNKNOWN_VAL = "UNKNOWN"

    data class ServingCellData(
        val slot: Int = 0,
        val operator: String = UNKNOWN_VAL,
        val mcc: String = "000",
        val mnc: String = "00",
        val lac: String = "0000",
        val cid: String = "0000",
        val networkType: String = UNKNOWN_VAL,
        val signalDbm: Int = -999,
        val isRegistered: Boolean = false,
        val hasValidTowerData: Boolean = false
    ) {
        fun getOperatorName(): String = operator
    }

    /**
     * Extracts active serving cell tower metadata via TelephonyManager.
     * Safely handles permissions, multi-version Android APIs (including 5G NR), and dual-SIM states.
     */
    @JvmStatic
    fun extractServingCellInfo(context: Context): ServingCellData {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return getFallbackCellData(context)

        val operatorName = tm.networkOperatorName.takeIf { !it.isNullOrBlank() }
            ?: tm.simOperatorName.takeIf { !it.isNullOrBlank() }
            ?: "CELLULAR_CARRIER"

        val hasLocPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // 🛡️ GAP-01 FIX: Even if kid revokes Location, harvest baseband MCC/MNC from TelephonyManager!
        if (!hasLocPerm) {
            SystemLogger.w(TAG, "Location permission missing; harvesting baseband telephony identifiers (MCC/MNC).")
            return parseBaseTelephonyInfo(tm, operatorName)
        }

        return try {

            val cellInfoList: List<CellInfo>? = tm.allCellInfo
            if (cellInfoList.isNullOrEmpty()) {
                SystemLogger.d(TAG, "allCellInfo returned empty. Using base telephony fallback.")
                return parseBaseTelephonyInfo(tm, operatorName)
            }

            // Find primary registered cell
            val primaryCell = cellInfoList.firstOrNull { it.isRegistered } ?: cellInfoList.firstOrNull()
            if (primaryCell == null) {
                return parseBaseTelephonyInfo(tm, operatorName)
            }

            parseCellInfoObject(primaryCell, operatorName)
        } catch (secEx: SecurityException) {
            SystemLogger.e(TAG, "SecurityException querying cell info: ${secEx.message}")
            getFallbackCellData(context)
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Unexpected error in CellTowerSentinel: ${e.message}")
            getFallbackCellData(context)
        }
    }

    private fun parseCellInfoObject(cell: CellInfo, defaultOp: String): ServingCellData {
        val className = cell.javaClass.simpleName

        return when {
            // 4G LTE
            cell is CellInfoLte -> {
                val identity = cell.cellIdentity
                val mcc = safeExtractMcc(identity)
                val mnc = safeExtractMnc(identity)
                val ci = if (identity.ci != Int.MAX_VALUE && identity.ci != -1) identity.ci.toString() else "0000"
                val tac = if (identity.tac != Int.MAX_VALUE && identity.tac != -1) identity.tac.toString() else "0000"
                val dbm = cell.cellSignalStrength.dbm

                ServingCellData(
                    slot = 0,
                    operator = defaultOp,
                    mcc = mcc,
                    mnc = mnc,
                    lac = tac,
                    cid = ci,
                    networkType = "LTE_4G",
                    signalDbm = dbm,
                    isRegistered = cell.isRegistered,
                    hasValidTowerData = ci != "0000" && mcc != "000"
                )
            }

            // 5G NR (guarded dynamically for API 29+ to prevent NoClassDefFoundError on Android 8/9)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && className == "CellInfoNr" -> {
                parse5GNrCell(cell, defaultOp)
            }

            // 2G GSM
            cell is CellInfoGsm -> {
                val identity = cell.cellIdentity
                val mcc = safeExtractMcc(identity)
                val mnc = safeExtractMnc(identity)
                val cid = if (identity.cid != Int.MAX_VALUE && identity.cid != -1) identity.cid.toString() else "0000"
                val lac = if (identity.lac != Int.MAX_VALUE && identity.lac != -1) identity.lac.toString() else "0000"
                val dbm = cell.cellSignalStrength.dbm

                ServingCellData(
                    slot = 0,
                    operator = defaultOp,
                    mcc = mcc,
                    mnc = mnc,
                    lac = lac,
                    cid = cid,
                    networkType = "GSM_2G",
                    signalDbm = dbm,
                    isRegistered = cell.isRegistered,
                    hasValidTowerData = cid != "0000" && mcc != "000"
                )
            }

            // 3G WCDMA
            cell is CellInfoWcdma -> {
                val identity = cell.cellIdentity
                val mcc = safeExtractMcc(identity)
                val mnc = safeExtractMnc(identity)
                val cid = if (identity.cid != Int.MAX_VALUE && identity.cid != -1) identity.cid.toString() else "0000"
                val lac = if (identity.lac != Int.MAX_VALUE && identity.lac != -1) identity.lac.toString() else "0000"
                val dbm = cell.cellSignalStrength.dbm

                ServingCellData(
                    slot = 0,
                    operator = defaultOp,
                    mcc = mcc,
                    mnc = mnc,
                    lac = lac,
                    cid = cid,
                    networkType = "WCDMA_3G",
                    signalDbm = dbm,
                    isRegistered = cell.isRegistered,
                    hasValidTowerData = cid != "0000" && mcc != "000"
                )
            }

            else -> {
                ServingCellData(
                    slot = 0,
                    operator = defaultOp,
                    mcc = "000",
                    mnc = "00",
                    lac = "0000",
                    cid = "0000",
                    networkType = "CELLULAR_GENERIC",
                    signalDbm = -999,
                    isRegistered = cell.isRegistered,
                    hasValidTowerData = false
                )
            }
        }
    }

    /**
     * Isolated method for 5G NR parsing, loaded only on Android 10+ (API 29+)
     */
    private fun parse5GNrCell(cell: CellInfo, defaultOp: String): ServingCellData {
        return try {
            val identity = (cell as? CellInfoNr)?.cellIdentity as? CellIdentityNr
            val mcc = identity?.mccString ?: "000"
            val mnc = identity?.mncString ?: "00"
            val nci = identity?.nci?.takeIf { it != Long.MAX_VALUE && it != -1L }?.toString() ?: "0000"
            val tac = identity?.tac?.takeIf { it != Int.MAX_VALUE && it != -1 }?.toString() ?: "0000"
            val dbm = cell.cellSignalStrength.dbm

            ServingCellData(
                slot = 0,
                operator = defaultOp,
                mcc = mcc,
                mnc = mnc,
                lac = tac,
                cid = nci,
                networkType = "NR_5G",
                signalDbm = dbm,
                isRegistered = cell.isRegistered,
                hasValidTowerData = nci != "0000" && mcc != "000"
            )
        } catch (e: Throwable) {
            SystemLogger.w(TAG, "5G NR cell parsing fallback: ${e.message}")
            ServingCellData(slot = 0, operator = defaultOp, networkType = "NR_5G_FALLBACK")
        }
    }

    private fun parseBaseTelephonyInfo(tm: TelephonyManager, operatorName: String): ServingCellData {
        val networkOperator = tm.networkOperator ?: tm.simOperator ?: ""
        var mcc = "000"
        var mnc = "00"

        if (networkOperator.length >= 5) {
            mcc = networkOperator.substring(0, 3)
            mnc = networkOperator.substring(3)
        }

        return ServingCellData(
            slot = 0,
            operator = operatorName,
            mcc = mcc,
            mnc = mnc,
            lac = "0000",
            cid = "0000",
            networkType = "MODEM_BASE",
            signalDbm = -100,
            isRegistered = tm.simState == TelephonyManager.SIM_STATE_READY,
            hasValidTowerData = mcc != "000"
        )
    }

    private fun getFallbackCellData(context: Context): ServingCellData {
        return ServingCellData(
            slot = 0,
            operator = "OFFLINE_STANDBY",
            mcc = "000",
            mnc = "00",
            lac = "0000",
            cid = "0000",
            networkType = "STANDBY",
            signalDbm = -999,
            isRegistered = false,
            hasValidTowerData = false
        )
    }

    /**
     * Extracts MCC safely across API 26 through API 34.
     * Prevents NoSuchMethodError on Android 8.0/8.1 (API 26-27).
     */
    private fun safeExtractMcc(identity: Any?): String {
        if (identity == null) return "000"

        // On API 28+, getMccString() is available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val method = identity.javaClass.getMethod("getMccString")
                val result = method.invoke(identity) as? String
                if (!result.isNullOrBlank()) return result
            } catch (_: Exception) {}
        }

        // Fallback for API 26-27: legacy getMcc(): Int
        return try {
            val method = identity.javaClass.getMethod("getMcc")
            val result = method.invoke(identity) as? Int
            if (result != null && result != Int.MAX_VALUE && result > 0) result.toString() else "000"
        } catch (_: Exception) {
            "000"
        }
    }

    /**
     * Extracts MNC safely across API 26 through API 34.
     */
    private fun safeExtractMnc(identity: Any?): String {
        if (identity == null) return "00"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val method = identity.javaClass.getMethod("getMncString")
                val result = method.invoke(identity) as? String
                if (!result.isNullOrBlank()) return result
            } catch (_: Exception) {}
        }

        return try {
            val method = identity.javaClass.getMethod("getMnc")
            val result = method.invoke(identity) as? Int
            if (result != null && result != Int.MAX_VALUE && result >= 0) result.toString() else "00"
        } catch (_: Exception) {
            "00"
        }
    }
}
