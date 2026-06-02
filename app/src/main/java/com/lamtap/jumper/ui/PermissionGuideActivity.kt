package com.lamtap.jumper.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.lamtap.jumper.R
import com.lamtap.jumper.accessibility.JumperAccessibilityService

class PermissionGuideActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_guide)

        findViewById<TextView>(R.id.top_back).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.button_finish_guide).setOnClickListener { finish() }

        bindStep(
            stepId = R.id.step_service,
            index = "1",
            titleRes = R.string.guide_step_service_title,
            descRes = R.string.guide_step_service_desc,
            actionRes = R.string.guide_step_service_action
        ) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        bindStep(
            stepId = R.id.step_battery,
            index = "2",
            titleRes = R.string.guide_step_battery_title,
            descRes = R.string.guide_step_battery_desc,
            actionRes = R.string.guide_step_battery_action
        ) {
            startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        }
        bindStep(
            stepId = R.id.step_background,
            index = "3",
            titleRes = R.string.guide_step_overlay_title,
            descRes = R.string.guide_step_overlay_desc,
            actionRes = R.string.guide_step_overlay_action
        ) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            })
        }
        bindStep(
            stepId = R.id.step_notice,
            index = "4",
            titleRes = R.string.guide_step_notice_title,
            descRes = R.string.guide_step_notice_desc,
            actionRes = R.string.guide_step_notice_action
        ) {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        updateStepStatuses()
    }

    private fun updateStepStatuses() {
        updateStepStatus(R.id.step_service, isAccessibilityEnabled())
        updateStepStatus(R.id.step_battery, true)
        updateStepStatus(R.id.step_background, true)
        updateStepStatus(R.id.step_notice, true)
    }

    private fun updateStepStatus(stepId: Int, completed: Boolean) {
        val stepView = findViewById<View>(stepId)
        val indexText = stepView.findViewById<TextView>(R.id.step_index)
        if (completed) {
            indexText.text = "✓"
            indexText.setBackgroundResource(R.drawable.bg_status_chip_success)
        } else {
            indexText.text = stepView.tag?.toString() ?: "1"
            indexText.setBackgroundResource(R.drawable.bg_step_index)
        }
    }

    private fun bindStep(
        stepId: Int,
        index: String,
        titleRes: Int,
        descRes: Int,
        actionRes: Int,
        onClick: () -> Unit
    ) {
        val stepView = findViewById<View>(stepId)
        stepView.tag = index
        stepView.findViewById<TextView>(R.id.step_index).text = index
        stepView.findViewById<TextView>(R.id.step_title).setText(titleRes)
        stepView.findViewById<TextView>(R.id.step_desc).setText(descRes)
        stepView.findViewById<Button>(R.id.step_action).setText(actionRes)
        stepView.findViewById<Button>(R.id.step_action).setOnClickListener { onClick() }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val accessibilityManager =
            getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedComponent = ComponentName(this, JumperAccessibilityService::class.java)
        return enabledServices.any { serviceInfo ->
            serviceInfo.resolveInfo.serviceInfo?.let { serviceInfoData ->
                ComponentName(serviceInfoData.packageName, serviceInfoData.name) == expectedComponent
            } ?: false
        }
    }
}
