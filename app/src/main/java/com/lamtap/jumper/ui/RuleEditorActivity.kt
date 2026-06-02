package com.lamtap.jumper.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.lamtap.jumper.R
import com.lamtap.jumper.accessibility.JumperAccessibilityService
import com.lamtap.jumper.accessibility.RuleStore
import java.util.UUID

class RuleEditorActivity : AppCompatActivity() {
    private var ruleId: String? = null
    private var isEditMode = false
    private var delaySeconds = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_editor)

        ruleId = intent.getStringExtra(EXTRA_RULE_ID)
        isEditMode = ruleId != null

        findViewById<TextView>(R.id.top_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.top_action).setOnClickListener { saveRule() }

        // Priority buttons
        val priorityValue = findViewById<TextView>(R.id.priority_value)
        findViewById<TextView>(R.id.priority_minus).setOnClickListener {
            val current = priorityValue.text.toString().toIntOrNull() ?: 10
            if (current > 1) {
                priorityValue.text = (current - 1).toString()
            }
        }
        findViewById<TextView>(R.id.priority_plus).setOnClickListener {
            val current = priorityValue.text.toString().toIntOrNull() ?: 10
            if (current < 100) {
                priorityValue.text = (current + 1).toString()
            }
        }

        // Delay buttons
        val delayValue = findViewById<TextView>(R.id.delay_value)
        findViewById<TextView>(R.id.delay_minus).setOnClickListener {
            if (delaySeconds > 0) {
                delaySeconds -= 1
                delayValue.text = "$delaySeconds 秒"
            }
        }
        findViewById<TextView>(R.id.delay_plus).setOnClickListener {
            if (delaySeconds < 30) {
                delaySeconds += 1
                delayValue.text = "$delaySeconds 秒"
            }
        }

        // Action spinner
        val spinner = findViewById<Spinner>(R.id.spinner_action)
        val actions = listOf("点击匹配节点", "点击坐标中心", "仅记录不操作")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actions)

        if (isEditMode) {
            loadRule(ruleId!!)
        }
    }

    private fun loadRule(id: String) {
        val rules = RuleStore.getAllRules(this)
        val rule = rules.find { it.id == id } ?: return

        findViewById<EditText>(R.id.edit_rule_name).setText(rule.name)
        findViewById<EditText>(R.id.edit_package_name).setText(rule.packageName)
        findViewById<EditText>(R.id.edit_trigger_text).setText(rule.keywords.joinToString(","))
        findViewById<EditText>(R.id.edit_trigger_id).setText(rule.viewIds.joinToString(","))
        findViewById<EditText>(R.id.edit_trigger_desc).setText(rule.descriptions.joinToString(","))
        findViewById<Switch>(R.id.switch_enable).isChecked = rule.enabled
        findViewById<TextView>(R.id.priority_value).text = rule.priority.toString()

        // Delay
        delaySeconds = rule.delay
        findViewById<TextView>(R.id.delay_value).text = "$delaySeconds 秒"

        // Action spinner
        val spinner = findViewById<Spinner>(R.id.spinner_action)
        val actionIndex = when (rule.actionType) {
            "click" -> 0
            "coordinate" -> 1
            "log_only" -> 2
            else -> 0
        }
        spinner.setSelection(actionIndex)

        // Show delete button in edit mode
        findViewById<TextView>(R.id.top_action).text = "保存"
        val deleteBtn = findViewById<TextView>(R.id.top_delete)
        deleteBtn.visibility = android.view.View.VISIBLE
        deleteBtn.setOnClickListener { deleteRule() }
    }

    private fun saveRule() {
        val name = findViewById<EditText>(R.id.edit_rule_name).text.toString().trim()
        val packageName = findViewById<EditText>(R.id.edit_package_name).text.toString().trim()
        val triggerText = findViewById<EditText>(R.id.edit_trigger_text).text.toString().trim()
        val triggerId = findViewById<EditText>(R.id.edit_trigger_id).text.toString().trim()
        val triggerDesc = findViewById<EditText>(R.id.edit_trigger_desc).text.toString().trim()
        val enabled = findViewById<Switch>(R.id.switch_enable).isChecked
        val priorityText = findViewById<TextView>(R.id.priority_value).text.toString()
        val priority = priorityText.toIntOrNull() ?: 10
        val spinner = findViewById<Spinner>(R.id.spinner_action)
        val actionType = when (spinner.selectedItemPosition) {
            0 -> "click"
            1 -> "coordinate"
            2 -> "log_only"
            else -> "click"
        }

        if (packageName.isBlank()) {
            Toast.makeText(this, "请输入包名", Toast.LENGTH_SHORT).show()
            return
        }

        if (name.isBlank()) {
            Toast.makeText(this, "请输入规则名称", Toast.LENGTH_SHORT).show()
            return
        }

        val keywords = triggerText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val viewIds = triggerId.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val descriptions = triggerDesc.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

        if (keywords.isEmpty() && viewIds.isEmpty() && descriptions.isEmpty()) {
            Toast.makeText(this, "请输入至少一个触发条件", Toast.LENGTH_SHORT).show()
            return
        }

        val rule = RuleStore.CustomRule(
            id = ruleId ?: UUID.randomUUID().toString(),
            name = name,
            packageName = packageName,
            enabled = enabled,
            priority = priority,
            keywords = keywords,
            viewIds = viewIds,
            descriptions = descriptions.ifEmpty { keywords },
            actionType = actionType,
            delay = delaySeconds
        )

        RuleStore.addRule(this, rule)
        JumperAccessibilityService.reloadRulesIfRunning()
        Toast.makeText(this, "规则已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun deleteRule() {
        ruleId?.let {
            AlertDialog.Builder(this)
                .setTitle("删除规则")
                .setMessage("确定要删除这条规则吗？")
                .setPositiveButton("删除") { _, _ ->
                    RuleStore.removeRule(this, it)
                    JumperAccessibilityService.reloadRulesIfRunning()
                    Toast.makeText(this, "规则已删除", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    companion object {
        const val EXTRA_RULE_ID = "rule_id"
    }
}
