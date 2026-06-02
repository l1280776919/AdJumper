package com.lamtap.jumper.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.lamtap.jumper.R
import com.lamtap.jumper.accessibility.JumperAccessibilityService
import com.lamtap.jumper.accessibility.RuleStore

class RuleTransferActivity : AppCompatActivity() {
    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importFromFile(it) }
    }

    private val exportFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { exportToFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_transfer)

        findViewById<TextView>(R.id.top_back).setOnClickListener { finish() }

        findViewById<android.view.View>(R.id.card_import_clipboard).setOnClickListener {
            importFromClipboard()
        }
        findViewById<android.view.View>(R.id.card_import_file).setOnClickListener {
            importFileLauncher.launch(arrayOf("application/json", "text/plain"))
        }
        findViewById<android.view.View>(R.id.card_export_file).setOnClickListener {
            exportFileLauncher.launch("jumper_rules.json")
        }
        findViewById<android.view.View>(R.id.card_export_clipboard).setOnClickListener {
            exportToClipboard()
        }
        findViewById<android.view.View>(R.id.card_restore_builtin).setOnClickListener {
            restoreBuiltin()
        }
    }

    private fun importFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip ?: run {
            Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show()
            return
        }
        val text = clip.getItemAt(0)?.text?.toString() ?: run {
            Toast.makeText(this, "剪贴板内容为空", Toast.LENGTH_SHORT).show()
            return
        }
        if (RuleStore.importFromJson(this, text)) {
            JumperAccessibilityService.reloadRulesIfRunning()
            Toast.makeText(this, "规则导入成功", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "JSON 格式错误，导入失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importFromFile(uri: Uri) {
        try {
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return
            if (RuleStore.importFromJson(this, json)) {
                JumperAccessibilityService.reloadRulesIfRunning()
                Toast.makeText(this, "规则导入成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "JSON 格式错误，导入失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "文件读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportToClipboard() {
        val json = RuleStore.exportJson(this)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("jumper_rules", json))
        Toast.makeText(this, "规则已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun exportToFile(uri: Uri) {
        try {
            val json = RuleStore.exportJson(this)
            contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            Toast.makeText(this, "规则已导出到文件", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "文件写入失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreBuiltin() {
        RuleStore.clearAllRules(this)
        JumperAccessibilityService.reloadRulesIfRunning()
        Toast.makeText(this, "已恢复默认规则", Toast.LENGTH_SHORT).show()
    }
}
