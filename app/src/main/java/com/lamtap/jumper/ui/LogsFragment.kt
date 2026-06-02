package com.lamtap.jumper.ui

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.lamtap.jumper.R
import com.lamtap.jumper.stats.SkipStatsStore

class LogsFragment : Fragment() {
    private var currentFilter = Filter.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_logs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFilters(view)
    }

    override fun onResume() {
        super.onResume()
        renderLogs()
    }

    private fun setupFilters(view: View) {
        val filterAll = view.findViewById<TextView>(R.id.filter_all)
        val filterSuccess = view.findViewById<TextView>(R.id.filter_success)
        val filterPending = view.findViewById<TextView>(R.id.filter_pending)

        filterAll.setOnClickListener {
            currentFilter = Filter.ALL
            updateFilterStyles(filterAll, filterSuccess, filterPending)
            renderLogs()
        }
        filterSuccess.setOnClickListener {
            currentFilter = Filter.SUCCESS
            updateFilterStyles(filterAll, filterSuccess, filterPending)
            renderLogs()
        }
        filterPending.setOnClickListener {
            currentFilter = Filter.PENDING
            updateFilterStyles(filterAll, filterSuccess, filterPending)
            renderLogs()
        }

        updateFilterStyles(filterAll, filterSuccess, filterPending)
    }

    private fun updateFilterStyles(
        selected: TextView,
        vararg others: TextView
    ) {
        selected.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_section_card)
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.tag_text))
        selected.setTypeface(null, Typeface.BOLD)

        others.forEach { tv ->
            tv.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_search_bar)
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            tv.setTypeface(null, Typeface.NORMAL)
        }
    }

    private fun renderLogs() {
        val logContainer = requireView().findViewById<LinearLayout>(R.id.log_list_container)
        logContainer.removeAllViews()
        val context = requireContext()
        val entries = SkipStatsStore.getRecentEntries(context, 50)
        val supportedPackages = resources.getStringArray(R.array.supported_packages).toList()

        when (currentFilter) {
            Filter.ALL, Filter.SUCCESS -> {
                if (entries.isEmpty()) {
                    logContainer.addView(
                        buildLogRow(
                            title = getString(R.string.main_stats_empty),
                            subtitle = getString(R.string.logs_empty_hint),
                            packageName = "",
                            timestamp = 0,
                            matchedBy = "",
                            success = false
                        )
                    )
                    return
                }

                val grouped = entries.groupBy { it.packageName }
                    .mapValues { (_, entries) -> entries.size }
                    .toList()
                    .sortedByDescending { (_, count) -> count }

                grouped.forEachIndexed { index, (packageName, count) ->
                    val latestEntry = entries.first { it.packageName == packageName }
                    logContainer.addView(
                        buildLogRow(
                            title = PackageVisuals.displayName(packageName),
                            subtitle = getString(R.string.logs_item_format, count),
                            packageName = packageName,
                            timestamp = latestEntry.timestamp,
                            matchedBy = latestEntry.matchedBy,
                            success = true
                        )
                    )
                }
            }
            Filter.PENDING -> {
                val triggeredPackages = entries.map { it.packageName }.toSet()
                val pendingPackages = supportedPackages.filter { it !in triggeredPackages }

                if (pendingPackages.isEmpty()) {
                    logContainer.addView(
                        buildLogRow(
                            title = "全部已触发",
                            subtitle = "所有支持的应用都已成功跳过广告",
                            packageName = "",
                            timestamp = 0,
                            matchedBy = "",
                            success = true
                        )
                    )
                    return
                }

                pendingPackages.forEach { packageName ->
                    logContainer.addView(
                        buildLogRow(
                            title = PackageVisuals.displayName(packageName),
                            subtitle = "等待首次触发",
                            packageName = packageName,
                            timestamp = 0,
                            matchedBy = "",
                            success = false
                        )
                    )
                }
            }
        }
    }

    private fun buildLogRow(
        title: String,
        subtitle: String,
        packageName: String,
        timestamp: Long,
        matchedBy: String,
        success: Boolean
    ): View {
        val context = requireContext()
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_search_bar)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
            addView(FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                background = iconBackground(packageName)
                addView(TextView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    gravity = Gravity.CENTER
                    text = if (packageName.isBlank()) "-" else PackageVisuals.badgeText(packageName)
                    textSize = 15f
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    setTypeface(typeface, Typeface.BOLD)
                })
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(12)
                }
                addView(TextView(context).apply {
                    text = title
                    textSize = 15f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = subtitle
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                })
                if (matchedBy.isNotEmpty()) {
                    addView(TextView(context).apply {
                        text = "匹配方式: $matchedBy"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(context, R.color.text_tertiary))
                    })
                }
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
                    text = if (success) getString(R.string.logs_status_success) else getString(R.string.logs_status_waiting)
                    textSize = 12f
                    setTextColor(
                        ContextCompat.getColor(
                            context,
                            if (success) R.color.success_text else R.color.text_secondary
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

    private fun iconBackground(packageName: String): GradientDrawable {
        val colors = when (packageName) {
            "com.ss.android.ugc.aweme" -> intArrayOf(0xFF23253A.toInt(), 0xFF53597B.toInt())
            "com.ss.android.article.news" -> intArrayOf(0xFFFF7E45.toInt(), 0xFFFFA061.toInt())
            "com.tencent.news" -> intArrayOf(0xFF38B86D.toInt(), 0xFF61D18C.toInt())
            "com.qiyi.video" -> intArrayOf(0xFF3C8BFF.toInt(), 0xFF62AAFF.toInt())
            "tv.danmaku.bili" -> intArrayOf(0xFF00A1D6.toInt(), 0xFF40BCD8.toInt())
            "com.tencent.qqlive" -> intArrayOf(0xFF6C72F3.toInt(), 0xFF8A90FF.toInt())
            "com.youku.phone" -> intArrayOf(0xFF38B86D.toInt(), 0xFF61D18C.toInt())
            "com.dragon.read" -> intArrayOf(0xFFFF7E45.toInt(), 0xFFFFA061.toInt())
            "com.smile.gifmaker" -> intArrayOf(0xFFFF7E45.toInt(), 0xFFFFA061.toInt())
            "com.sina.weibo" -> intArrayOf(0xFFFF4500.toInt(), 0xFFFF6B35.toInt())
            else -> intArrayOf(0xFF6C72F3.toInt(), 0xFF8A90FF.toInt())
        }
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(14).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private enum class Filter {
        ALL, SUCCESS, PENDING
    }
}
