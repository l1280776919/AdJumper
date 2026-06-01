package com.lamtap.jumper.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lamtap.jumper.R

class RuleEditorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_editor)

        findViewById<TextView>(R.id.top_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.top_action).setOnClickListener { finish() }
    }
}