package com.lamtap.jumper.ui

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lamtap.jumper.R
import com.lamtap.jumper.accessibility.JumperAccessibilityService
import com.lamtap.jumper.accessibility.RuleStore

class RulesFragment : Fragment() {
    private lateinit var listContainer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var tabRecommended: TextView
    private lateinit var tabBuiltin: TextView
    private var currentTab = Tab.RECOMMENDED
    private var searchText = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_rules, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listContainer = view.findViewById(R.id.rule_list_container)
        searchInput = view.findViewById(R.id.search_input)
        tabRecommended = view.findViewById(R.id.tab_recommended)
        tabBuiltin = view.findViewById(R.id.tab_builtin)

        view.findViewById<View>(R.id.rules_transfer_entry).setOnClickListener {
            startActivity(Intent(requireContext(), RuleTransferActivity::class.java))
        }
        view.findViewById<FloatingActionButton>(R.id.rules_add_button).setOnClickListener {
            startActivity(Intent(requireContext(), RuleEditorActivity::class.java))
        }

        tabRecommended.setOnClickListener {
            currentTab = Tab.RECOMMENDED
            updateTabStyles()
            renderRules()
        }
        tabBuiltin.setOnClickListener {
            currentTab = Tab.BUILTIN
            updateTabStyles()
            renderRules()
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchText = s?.toString()?.trim().orEmpty()
                renderRules()
            }
        })

        updateTabStyles()
    }

    override fun onResume() {
        super.onResume()
        renderRules()
    }

    private fun updateTabStyles() {
        val context = requireContext()
        if (currentTab == Tab.RECOMMENDED) {
            tabRecommended.background = ContextCompat.getDrawable(context, R.drawable.bg_section_card)
            tabRecommended.setTextColor(ContextCompat.getColor(context, R.color.tag_text))
            tabRecommended.setTypeface(null, Typeface.BOLD)

            tabBuiltin.background = null
            tabBuiltin.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            tabBuiltin.setTypeface(null, Typeface.NORMAL)
        } else {
            tabBuiltin.background = ContextCompat.getDrawable(context, R.drawable.bg_section_card)
            tabBuiltin.setTextColor(ContextCompat.getColor(context, R.color.tag_text))
            tabBuiltin.setTypeface(null, Typeface.BOLD)

            tabRecommended.background = null
            tabRecommended.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            tabRecommended.setTypeface(null, Typeface.NORMAL)
        }
    }

    private fun renderRules() {
        listContainer.removeAllViews()
        val context = requireContext()

        when (currentTab) {
            Tab.RECOMMENDED -> {
                val customRules = RuleStore.getAllRules(context)
                val filtered = if (searchText.isEmpty()) {
                    customRules
                } else {
                    customRules.filter { rule ->
                        rule.name.contains(searchText, ignoreCase = true) ||
                                rule.packageName.contains(searchText, ignoreCase = true) ||
                                rule.keywords.any { it.contains(searchText, ignoreCase = true) }
                    }
                }

                if (filtered.isEmpty()) {
                    listContainer.addView(buildEmptyRow("暂无自定义规则", "点击右下角 + 添加新规则"))
                } else {
                    filtered.forEach { rule ->
                        listContainer.addView(buildCustomRuleRow(rule))
                    }
                }
            }
            Tab.BUILTIN -> {
                val packageNames = resources.getStringArray(R.array.supported_packages)
                val keywords = resources.getStringArray(R.array.default_skip_keywords).toSet()
                val filtered = if (searchText.isEmpty()) {
                    packageNames.toList()
                } else {
                    packageNames.filter { pkg ->
                        PackageVisuals.displayName(pkg).contains(searchText, ignoreCase = true) ||
                                pkg.contains(searchText, ignoreCase = true) ||
                                keywords.any { it.contains(searchText, ignoreCase = true) }
                    }
                }

                filtered.forEachIndexed { index, packageName ->
                    listContainer.addView(buildBuiltinRuleRow(packageName, index < 4))
                }
            }
        }
    }

    private fun buildEmptyRow(title: String, subtitle: String): View {
        val context = requireContext()
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(40), dp(20), dp(40))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(TextView(context).apply {
                text = title
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = subtitle
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_tertiary))
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, 0)
            })
        }
    }

    private fun buildCustomRuleRow(rule: RuleStore.CustomRule): View {
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
            setOnClickListener {
                val intent = Intent(context, RuleEditorActivity::class.java)
                intent.putExtra(RuleEditorActivity.EXTRA_RULE_ID, rule.id)
                startActivity(intent)
            }
            addView(FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
                background = customRuleBackground()
                addView(TextView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    gravity = Gravity.CENTER
                    text = "+"
                    textSize = 20f
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
                    text = rule.name
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = rule.packageName
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                })
                addView(TextView(context).apply {
                    text = "关键词: ${rule.keywords.joinToString(", ")}"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.tag_text))
                })
            })
            addView(Switch(context).apply {
                isChecked = rule.enabled
                thumbDrawable = ContextCompat.getDrawable(context, R.drawable.switch_thumb)
                trackDrawable = ContextCompat.getDrawable(context, R.drawable.switch_track)
                setOnCheckedChangeListener { _, _ ->
                    val updated = rule.copy(enabled = isChecked)
                    RuleStore.addRule(context, updated)
                    JumperAccessibilityService.reloadRulesIfRunning()
                }
            })
        }
    }

    private fun buildBuiltinRuleRow(packageName: String, enabled: Boolean): View {
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

    private fun customRuleBackground(): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF6C72F3.toInt(), 0xFF8A90FF.toInt())).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(14).toFloat()
        }
    }

    private fun iconBackground(packageName: String): GradientDrawable {
        val palette = when (packageName) {
            "com.ss.android.ugc.aweme" -> intArrayOf(0xFF23253A.toInt(), 0xFF53597B.toInt())
            "com.ss.android.article.news" -> intArrayOf(0xFFFF6A3D.toInt(), 0xFFFF8C52.toInt())
            "com.tencent.news" -> intArrayOf(0xFF2BBF76.toInt(), 0xFF58D693.toInt())
            "com.qiyi.video" -> intArrayOf(0xFF37B54A.toInt(), 0xFF61D46B.toInt())
            "tv.danmaku.bili" -> intArrayOf(0xFF00A1D6.toInt(), 0xFF40BCD8.toInt())
            else -> intArrayOf(0xFF6C72F3.toInt(), 0xFF8A90FF.toInt())
        }
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, palette).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(14).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private enum class Tab {
        RECOMMENDED, BUILTIN
    }
}
