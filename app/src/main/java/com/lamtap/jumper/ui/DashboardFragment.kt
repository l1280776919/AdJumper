package com.lamtap.jumper.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.lamtap.jumper.R
import com.lamtap.jumper.accessibility.JumperAccessibilityService
import com.lamtap.jumper.accessibility.RuleStore
import com.lamtap.jumper.stats.SkipStatsStore

class DashboardFragment : Fragment() {
    private lateinit var statusBadge: TextView
    private lateinit var statusText: TextView
    private lateinit var serviceStatusSwitch: Switch
    private lateinit var totalSkipsView: TextView
    private lateinit var activeDaysView: TextView
    private lateinit var successRateView: TextView
    private lateinit var recentRecordsContainer: LinearLayout
    private lateinit var packageContainer: FlexboxLayout
    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (isAdded) {
                renderStatus()
                refreshHandler.postDelayed(this, 3000)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusBadge = view.findViewById(R.id.status_badge)
        statusText = view.findViewById(R.id.status_text)
        serviceStatusSwitch = view.findViewById(R.id.service_status_switch)
        totalSkipsView = view.findViewById(R.id.total_skips_value)
        activeDaysView = view.findViewById(R.id.active_days_value)
        successRateView = view.findViewById(R.id.success_rate_value)
        recentRecordsContainer = view.findViewById(R.id.recent_records_container)
        packageContainer = view.findViewById(R.id.package_chip_group)

        view.findViewById<MaterialButton>(R.id.open_settings_button).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        serviceStatusSwitch.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        view.findViewById<MaterialButton>(R.id.permission_guide_button).setOnClickListener {
            startActivity(Intent(requireContext(), PermissionGuideActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        renderStatus()
        renderStats()
        renderPackages()
        refreshHandler.postDelayed(refreshRunnable, 3000)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun renderStatus() {
        val context = requireContext()
        val enabled = isServiceEnabled(context)
        statusText.text = if (enabled) {
            getString(R.string.main_status_enabled)
        } else {
            getString(R.string.main_status_disabled)
        }
        statusBadge.text = getString(
            if (enabled) R.string.main_status_badge_enabled else R.string.main_status_badge_disabled
        )
        statusBadge.setBackgroundColor(
            ContextCompat.getColor(context, if (enabled) R.color.white else R.color.blue_500)
        )
        statusBadge.alpha = if (enabled) 0.9f else 0.7f
        serviceStatusSwitch.isChecked = enabled
    }

    private fun renderStats() {
        val context = requireContext()
        val trackedPackages = resources.getStringArray(R.array.supported_packages).toList()
        val snapshot = SkipStatsStore.snapshot(context, trackedPackages)

        totalSkipsView.text = snapshot.todaySkips.toString()
        activeDaysView.text = snapshot.weeklySkips.toString()
        successRateView.text = getString(
            R.string.dashboard_success_rate_value,
            SkipStatsStore.getSuccessRate(context)
        )

        recentRecordsContainer.removeAllViews()
        val recentEntries = snapshot.recentEntries

        if (recentEntries.isEmpty()) {
            recentRecordsContainer.addView(
                buildRecentRecordRow(
                    title = getString(R.string.main_stats_empty),
                    subtitle = getString(R.string.dashboard_record_empty_subtitle),
                    packageName = "",
                    timestamp = 0,
                    showSuccess = false
                )
            )
            return
        }

        val grouped = recentEntries.groupBy { it.packageName }
            .mapValues { (_, entries) -> entries.size }
            .toList()
            .sortedByDescending { (_, count) -> count }
            .take(5)

        grouped.forEachIndexed { index, (packageName, count) ->
            val latestEntry = recentEntries.first { it.packageName == packageName }
            recentRecordsContainer.addView(
                buildRecentRecordRow(
                    title = PackageVisuals.displayName(packageName),
                    subtitle = getString(R.string.dashboard_record_hits, count),
                    packageName = packageName,
                    timestamp = latestEntry.timestamp,
                    showSuccess = true
                )
            )
        }
    }

    private fun renderPackages() {
        packageContainer.removeAllViews()
        val context = requireContext()
        val adSdkApps = InstalledApps.getAdSdkApps(context)
        val customPackages = UiPreferencesStore.getCustomPackages(context)
        val customApps = customPackages.map { pkg ->
            InstalledApps.AppInfo(pkg, InstalledApps.getAppLabel(context, pkg), false)
        }
        val allApps = (adSdkApps + customApps).distinctBy { it.packageName }.take(12)

        allApps.forEach { app ->
            packageContainer.addView(buildTag(app.label))
        }

        packageContainer.addView(buildAddTag())
    }

    private fun buildAddTag(): MaterialCardView {
        val context = requireContext()
        return MaterialCardView(context).apply {
            radius = dpToPx(14).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.tag_keyword_background))
            layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = dpToPx(8)
                bottomMargin = dpToPx(8)
            }
            isClickable = true
           setOnClickListener { showAddAppDialog() }
            addView(TextView(context).apply {
                this.text = "+ 添加"
                setTextColor(ContextCompat.getColor(context, R.color.tag_text))
                textSize = 13f
                setPadding(dpToPx(14), dpToPx(9), dpToPx(14), dpToPx(9))
            })
        }
    }

    private fun showAddAppDialog() {
        val context = requireContext()
        val installedApps = InstalledApps.getNonSystemApps(context)
        val customPackages = UiPreferencesStore.getCustomPackages(context).toSet()
        val availableApps = installedApps.filter { it.packageName !in customPackages }

        if (availableApps.isEmpty()) {
            Toast.makeText(context, "没有更多可添加的应用", Toast.LENGTH_SHORT).show()
            return
        }

        val appLabels = availableApps.take(50).map { "${it.label} (${it.packageName})" }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("选择要监控的应用")
            .setItems(appLabels) { _, which ->
                val app = availableApps.take(50)[which]
                UiPreferencesStore.addCustomPackage(context, app.packageName)
                JumperAccessibilityService.reloadRulesIfRunning()
                renderPackages()
                Toast.makeText(context, "已添加: ${app.label}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun buildTag(text: String): MaterialCardView {
        val context = requireContext()
        return MaterialCardView(context).apply {
            radius = dpToPx(14).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.tag_package_background))
            layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = dpToPx(8)
                bottomMargin = dpToPx(8)
            }
            addView(TextView(context).apply {
                this.text = text
                setTextColor(ContextCompat.getColor(context, R.color.tag_text))
                textSize = 13f
                setPadding(dpToPx(14), dpToPx(9), dpToPx(14), dpToPx(9))
            })
        }
    }

    private fun buildRecentRecordRow(
        title: String,
        subtitle: String,
        packageName: String,
        timestamp: Long,
        showSuccess: Boolean
    ): View {
        val context = requireContext()
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_search_bar)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
            addView(TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(38), dpToPx(38))
                background = buildAccentBackground(packageName)
                gravity = Gravity.CENTER
                text = if (packageName.isBlank()) "-" else PackageVisuals.badgeText(packageName)
                textSize = 15f
                setTextColor(ContextCompat.getColor(context, R.color.white))
                typeface = typeface
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dpToPx(12)
                }
                addView(TextView(context).apply {
                    text = title
                    textSize = 15f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = subtitle
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                })
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
                addView(TextView(context).apply {
                    text = formatTimestamp(timestamp)
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                })
                addView(TextView(context).apply {
                    text = if (showSuccess) {
                        getString(R.string.dashboard_record_status)
                    } else {
                        getString(R.string.logs_status_waiting)
                    }
                    textSize = 12f
                    setTextColor(
                        ContextCompat.getColor(
                            context,
                            if (showSuccess) R.color.success_text else R.color.text_secondary
                        )
                    )
                })
            })
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return "--:--"
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    private fun buildAccentBackground(packageName: String): GradientDrawable {
        val colors = when (packageName) {
            "com.ss.android.ugc.aweme" -> intArrayOf(0xFF23253A.toInt(), 0xFF53597B.toInt())
            "com.ss.android.article.news" -> intArrayOf(0xFFFF6A3D.toInt(), 0xFFFF8C52.toInt())
            "com.tencent.news" -> intArrayOf(0xFF2BBF76.toInt(), 0xFF58D693.toInt())
            "com.qiyi.video" -> intArrayOf(0xFF37B54A.toInt(), 0xFF61D46B.toInt())
            "tv.danmaku.bili" -> intArrayOf(0xFF00A1D6.toInt(), 0xFF40BCD8.toInt())
            "com.tencent.qqlive" -> intArrayOf(0xFF37B977.toInt(), 0xFF59D18F.toInt())
            "com.youku.phone" -> intArrayOf(0xFF2BBF76.toInt(), 0xFF58D693.toInt())
            "com.dragon.read" -> intArrayOf(0xFFFF8A4D.toInt(), 0xFFFFA866.toInt())
            "com.smile.gifmaker" -> intArrayOf(0xFFFF6A3D.toInt(), 0xFFFF8C52.toInt())
            "com.sina.weibo" -> intArrayOf(0xFFFF4500.toInt(), 0xFFFF6B35.toInt())
            else -> intArrayOf(0xFF6C72F3.toInt(), 0xFF8A90FF.toInt())
        }
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(14).toFloat()
        }
    }

    private fun dpToPx(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun isServiceEnabled(context: Context): Boolean {
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedComponent = ComponentName(context, JumperAccessibilityService::class.java)
        return enabledServices.any { serviceInfo ->
            serviceInfo.resolveInfo.serviceInfo?.let { serviceInfoData ->
                ComponentName(serviceInfoData.packageName, serviceInfoData.name) == expectedComponent
            } ?: false
        }
    }
}
