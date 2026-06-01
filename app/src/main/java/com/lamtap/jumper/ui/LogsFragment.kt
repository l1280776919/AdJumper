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
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_logs, container, false)
    }

    override fun onResume() {
        super.onResume()
        val logContainer = requireView().findViewById<LinearLayout>(R.id.log_list_container)
        logContainer.removeAllViews()
        val snapshot = SkipStatsStore.snapshot(
            requireContext(),
            resources.getStringArray(R.array.supported_packages).toList()
        )

        val entries = snapshot.perPackageCounts
            .filterValues { it > 0 }
            .toList()
            .sortedByDescending { (_, count) -> count }
            .take(8)

        if (entries.isEmpty()) {
            logContainer.addView(
                buildLogRow(
                    title = getString(R.string.main_stats_empty),
                    subtitle = getString(R.string.logs_empty_hint),
                    packageName = "",
                    index = 0,
                    success = false
                )
            )
            return
        }

        entries.forEachIndexed { index, (packageName, count) ->
            logContainer.addView(
                buildLogRow(
                    title = PackageVisuals.displayName(packageName),
                    subtitle = getString(R.string.logs_item_format, count),
                    packageName = packageName,
                    index = index,
                    success = true
                )
            )
        }
    }

    private fun buildLogRow(
        title: String,
        subtitle: String,
        packageName: String,
        index: Int,
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
                background = iconBackground(index)
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
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
                addView(TextView(context).apply {
                    text = getString(R.string.dashboard_record_time_format, 14 - index, 8 + index * 5)
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

    private fun iconBackground(index: Int): GradientDrawable {
        val palette = listOf(
            intArrayOf(0xFF23253A.toInt(), 0xFF53597B.toInt()),
            intArrayOf(0xFFFF7E45.toInt(), 0xFFFFA061.toInt()),
            intArrayOf(0xFF38B86D.toInt(), 0xFF61D18C.toInt()),
            intArrayOf(0xFF3C8BFF.toInt(), 0xFF62AAFF.toInt()),
            intArrayOf(0xFF6C72F3.toInt(), 0xFF8A90FF.toInt())
        )[index % 5]
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, palette).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(14).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}