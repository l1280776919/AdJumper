package com.lamtap.jumper.ui

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lamtap.jumper.R

class RulesFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_rules, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val listContainer = view.findViewById<LinearLayout>(R.id.rule_list_container)
        listContainer.removeAllViews()
        view.findViewById<View>(R.id.rules_transfer_entry).setOnClickListener {
            startActivity(Intent(requireContext(), RuleTransferActivity::class.java))
        }
        view.findViewById<FloatingActionButton>(R.id.rules_add_button).setOnClickListener {
            startActivity(Intent(requireContext(), RuleEditorActivity::class.java))
        }
        resources.getStringArray(R.array.supported_packages).take(6).forEachIndexed { index, packageName ->
            listContainer.addView(buildRuleRow(packageName, index < 4))
        }
    }

    private fun buildRuleRow(packageName: String, enabled: Boolean): View {
        val context = requireContext()
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_search_bar)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
            addView(FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
                background = iconBackground(packageName)
                addView(TextView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    gravity = Gravity.CENTER
                    text = PackageVisuals.badgeText(packageName)
                    textSize = 16f
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
                    text = PackageVisuals.displayName(packageName)
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = packageName
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                })
                addView(TextView(context).apply {
                    text = context.getString(
                        R.string.rules_priority_format,
                        if (enabled) 10 else 6
                    )
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.tag_text))
                })
            })
            addView(Switch(context).apply {
                isChecked = enabled
                thumbDrawable = ContextCompat.getDrawable(context, R.drawable.switch_thumb)
                trackDrawable = ContextCompat.getDrawable(context, R.drawable.switch_track)
            })
        }
    }

    private fun iconBackground(packageName: String): GradientDrawable {
        val palette = when (packageName) {
            "com.ss.android.ugc.aweme" -> intArrayOf(0xFF23253A.toInt(), 0xFF53597B.toInt())
            "com.ss.android.article.news" -> intArrayOf(0xFFFF6A3D.toInt(), 0xFFFF8C52.toInt())
            "com.tencent.news" -> intArrayOf(0xFF2BBF76.toInt(), 0xFF58D693.toInt())
            "com.qiyi.video" -> intArrayOf(0xFF37B54A.toInt(), 0xFF61D46B.toInt())
            else -> intArrayOf(0xFF6C72F3.toInt(), 0xFF8B90FF.toInt())
        }
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, palette).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(14).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}