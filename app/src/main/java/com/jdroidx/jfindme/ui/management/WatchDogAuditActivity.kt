package com.jdroidx.jfindme.ui.management

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.jdroidx.jfindme.ui.base.BaseActivityJava
import com.jdroidx.jfindme.ui.base.UiArchitect

/**
 * 🛰️ WATCHDOG AUDIT: V57 5-Tier Forensic Dashboard
 * 
 * AUDIT FIX [AUD-012, AUD-006]: Re-wired to use WatchDogViewModel to pull DB queries 
 * off the main UI thread.
 * AUDIT FIX [AUD-020]: Replaced hardcoded textSize=14f with UiArchitect.applyText()
 */
class WatchDogAuditActivity : BaseActivityJava() {

    private lateinit var viewModel: WatchDogViewModel
    private lateinit var statsContainer: LinearLayout
    private lateinit var summaryContainer: LinearLayout
    private lateinit var engineContainer: LinearLayout
    private val TAG = "WatchDogAudit"

    // Grouping mapping for UI display
    private val tierMapping = mapOf(
        "TIER 1: IDENTITY CORE" to listOf("user_identity", "user_profile", "organization", "user_org_map"),
        "TIER 2: DEVICE HARDWARE" to listOf("device", "device_pairing", "sim"),
        "TIER 3: RELATIONSHIP GRAPH" to listOf("groups", "group_members"),
        "TIER 4: ANALYTICAL EVENTS" to listOf("location_event", "safe_zone", "geofence_event"),
        "TIER 5: SYSTEM LEDGERS" to listOf("sync_queue", "sys_log")
    )

    override fun createLayout(): View {
        viewModel = ViewModelProvider(this)[WatchDogViewModel::class.java]

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(primaryColor)
        }

        // --- GLOBAL HEADER ---
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            applyGlobalHeaderGeometry(this)
        }

        val title = TextView(this).apply {
            text = "FORENSIC MATRIX AUDIT"
            setTextColor(gradientOverlayText)
            UiArchitect.applyText(this@WatchDogAuditActivity, this, "TEXT_HEADER")
        }
        header.addView(title)

        val subtitle = TextView(this).apply {
            text = "V57 14-Table Normalized Relational Verifier"
            setTextColor(gradientOverlayText)
            alpha = 0.8f
            UiArchitect.applyText(this@WatchDogAuditActivity, this, "TEXT_SUB")
        }
        header.addView(subtitle)
        root.addView(header)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setPadding(0, 0, 0, scale(100))
        }
        
        val mainContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        summaryContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        mainContent.addView(summaryContainer)

        engineContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val q = responsivePaddingQuota
            setPadding(q, 0, q, 0)
        }
        mainContent.addView(engineContainer)

        statsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val q = responsivePaddingQuota
            setPadding(q, q, q, q)
        }
        mainContent.addView(statsContainer)

        scroll.addView(mainContent)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        return root
    }

    override fun setupLogic() {
        // Observe ViewModel data
        viewModel.tableStats.observe(this) { stats ->
            renderAuditMatrix(stats)
            renderSummaryCard(stats)
        }
        
        viewModel.engineStatus.observe(this) { engineReport ->
            renderEngineReport(engineReport)
        }

        // Request forensic sweep of all tables mapped in the tiers
        val allTables = tierMapping.values.flatten()
        viewModel.performForensicSweep(allTables)
    }

    private fun renderSummaryCard(stats: List<WatchDogViewModel.TableStatus>) {
        summaryContainer.removeAllViews()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val p = scale(30)
            setPadding(p, p, p, p)
            val lp = LinearLayout.LayoutParams(-1, -2)
            val q = responsivePaddingQuota
            lp.setMargins(q, scale(20), q, scale(10))
            layoutParams = lp
            UiArchitect.applyCard(this@WatchDogAuditActivity, this, "CARD_STANDARD")
            setBackground(getSolidDrawable(Color.argb(30, 0, 0, 0), 20f))
        }

        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_dialog_info)
            setColorFilter(gradientStart)
        }
        card.addView(icon, scale(60), scale(60))

        val textStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(scale(30), 0, 0, 0)
        }
        
        val identityStatus = stats.find { it.name == "user_identity" }
        val healthy = (identityStatus?.count ?: 0) > 0

        val healthTitle = TextView(this).apply {
            text = if (healthy) "MATRIX NOMINAL" else "IDENTITY GAP DETECTED"
            setTextColor(if (healthy) Color.parseColor("#2E7D32") else Color.RED)
            UiArchitect.applyText(this@WatchDogAuditActivity, this, "TEXT_BODY") // AUDIT FIX [AUD-020]
            typeface = Typeface.DEFAULT_BOLD
        }
        textStack.addView(healthTitle)

        val healthSub = TextView(this).apply {
            text = "Total Tables Active: ${stats.size} | Forensic Sync: OK"
            setTextColor(Color.DKGRAY)
            UiArchitect.applyText(this@WatchDogAuditActivity, this, "TEXT_CAPTION")
        }
        textStack.addView(healthSub)
        card.addView(textStack)

        summaryContainer.addView(card)
    }

    private fun renderEngineReport(engineReport: String) {
        engineContainer.removeAllViews()
        
        val groupLabel = TextView(this).apply {
            text = "TIER 0: ENGINE REGISTRY HEARTBEAT"
            setTextColor(gradientStart)
            typeface = Typeface.DEFAULT_BOLD
            UiArchitect.applyText(this@WatchDogAuditActivity, this, "TEXT_BODY")
            setPadding(scale(10), scale(40), 0, scale(10))
            letterSpacing = 0.1f
        }
        engineContainer.addView(groupLabel)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = scale(25)
            setPadding(p, p, p, p)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 0, 0, scale(15))
            layoutParams = lp
            UiArchitect.applyCard(this@WatchDogAuditActivity, this, "CARD_STANDARD")
        }

        val reportTv = TextView(this).apply {
            text = engineReport
            setTextColor(Color.BLACK)
            typeface = Typeface.MONOSPACE
            UiArchitect.applyText(this@WatchDogAuditActivity, this, "TEXT_BODY")
        }
        card.addView(reportTv)
        engineContainer.addView(card)
    }

    private fun renderAuditMatrix(stats: List<WatchDogViewModel.TableStatus>) {
        statsContainer.removeAllViews()

        for ((tierName, tablesInTier) in tierMapping) {
            val groupLabel = TextView(this).apply {
                text = tierName
                setTextColor(gradientStart)
                typeface = Typeface.DEFAULT_BOLD
                UiArchitect.applyText(this@WatchDogAuditActivity, this, "TEXT_BODY")
                setPadding(scale(10), scale(40), 0, scale(10))
                letterSpacing = 0.1f
            }
            statsContainer.addView(groupLabel)

            for (tableName in tablesInTier) {
                val tableStat = stats.find { it.name == tableName }
                if (tableStat != null) {
                    statsContainer.addView(createTableAuditCard(tableStat))
                }
            }
        }
    }

    private fun createTableAuditCard(stat: WatchDogViewModel.TableStatus): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = scale(25)
            setPadding(p, p, p, p)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 0, 0, scale(15))
            layoutParams = lp
            UiArchitect.applyCard(this@WatchDogAuditActivity, this, "CARD_STANDARD")
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val statusIcon = TextView(this).apply {
            text = if (stat.count > 0) "✅" else "⚠️"
            UiArchitect.applyText(this@WatchDogAuditActivity, this, "TEXT_BODY")
            setPadding(0, 0, scale(20), 0)
        }
        row.addView(statusIcon)

        val nameTv = TextView(this).apply {
            text = stat.name.uppercase()
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            UiArchitect.applyText(this@WatchDogAuditActivity, this, "TEXT_BODY")
        }
        row.addView(nameTv)

        val countTv = TextView(this).apply {
            text = "${stat.count}"
            setTextColor(if (stat.count > 0) Color.parseColor("#1B5E20") else Color.GRAY)
            typeface = Typeface.MONOSPACE
            UiArchitect.applyText(this@WatchDogAuditActivity, this, "TEXT_BODY")
            setPadding(scale(20), 0, 0, 0)
        }
        row.addView(countTv)
        card.addView(row)

        val colTv = TextView(this).apply {
            val preview = stat.columns.take(4).joinToString(", ") + (if (stat.columns.size > 4) "..." else "")
            text = "PARITY: ${stat.columns.size} FIELDS | $preview"
            setTextColor(Color.GRAY)
            UiArchitect.applyText(this@WatchDogAuditActivity, this, "TEXT_CAPTION")
            setPadding(scale(55), scale(10), 0, 0)
        }
        card.addView(colTv)

        return card
    }
}
