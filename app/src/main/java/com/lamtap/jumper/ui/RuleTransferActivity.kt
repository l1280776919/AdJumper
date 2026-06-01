package com.lamtap.jumper.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lamtap.jumper.R

class RuleTransferActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_transfer)

        findViewById<android.widget.TextView>(R.id.top_back).setOnClickListener { finish() }

        val clickIds = listOf(
            R.id.card_import_clipboard,
            R.id.card_import_file,
            R.id.card_export_file,
            R.id.card_export_clipboard,
            R.id.card_restore_builtin
        )
        clickIds.forEach { viewId ->
            findViewById<android.view.View>(viewId).setOnClickListener {
                Toast.makeText(this, R.string.transfer_title, Toast.LENGTH_SHORT).show()
            }
        }
    }
}