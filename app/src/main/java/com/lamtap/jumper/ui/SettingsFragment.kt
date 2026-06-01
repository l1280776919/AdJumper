package com.lamtap.jumper.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.lamtap.jumper.R
import com.lamtap.jumper.accessibility.JumperAccessibilityService

class SettingsFragment : Fragment() {
    private lateinit var autoStartSwitch: Switch
    private lateinit var notificationSwitch: Switch
    private lateinit var powerSaveCardSwitch: Switch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        autoStartSwitch = view.findViewById(R.id.switch_auto_start)
        notificationSwitch = view.findViewById(R.id.switch_notification)
        powerSaveCardSwitch = view.findViewById(R.id.switch_power_save_card)

        autoStartSwitch.isChecked = true
        notificationSwitch.isChecked = true
        powerSaveCardSwitch.isChecked = false

        powerSaveCardSwitch.setOnCheckedChangeListener { _, _ -> renderStatuses() }
        autoStartSwitch.setOnCheckedChangeListener { _, _ -> renderStatuses() }
        notificationSwitch.setOnCheckedChangeListener { _, _ -> renderStatuses() }

        view.findViewById<MaterialButton>(R.id.button_view_logs).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.content_container, LogsFragment())
                .commit()
            activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.nav_logs
        }

        bindStaticStatusRow(
            containerId = R.id.settings_status_row_accessibility,
            labelRes = R.string.settings_service_accessibility
        )
        bindStaticStatusRow(
            containerId = R.id.settings_status_row_foreground,
            labelRes = R.string.settings_service_foreground
        )
        bindStaticStatusRow(
            containerId = R.id.settings_status_row_battery,
            labelRes = R.string.settings_service_battery
        )
        bindStaticStatusRow(
            containerId = R.id.settings_status_row_background,
            labelRes = R.string.settings_service_background
        )

        renderStatuses()
    }

    override fun onResume() {
        super.onResume()
        renderStatuses()
    }

    private fun renderStatuses() {
        val context = requireContext()
        val accessibilityEnabled = isServiceEnabled(context)

        renderStatusBadge(
            textView = requireView().findViewById(R.id.status_service_running),
            enabled = accessibilityEnabled,
            enabledText = getString(R.string.settings_status_running),
            disabledText = getString(R.string.settings_status_pending)
        )
        renderStatusBadge(
            textView = requireView().findViewById(R.id.status_accessibility),
            enabled = accessibilityEnabled,
            enabledText = getString(R.string.settings_status_enabled),
            disabledText = getString(R.string.settings_status_pending)
        )
        renderStatusBadge(
            textView = requireView().findViewById(R.id.status_foreground),
            enabled = notificationSwitch.isChecked,
            enabledText = getString(R.string.settings_status_running),
            disabledText = getString(R.string.settings_status_disabled)
        )
        renderStatusBadge(
            textView = requireView().findViewById(R.id.status_battery),
            enabled = !powerSaveCardSwitch.isChecked,
            enabledText = getString(R.string.settings_status_optimized),
            disabledText = getString(R.string.settings_status_pending)
        )
        renderStatusBadge(
            textView = requireView().findViewById(R.id.status_background),
            enabled = autoStartSwitch.isChecked,
            enabledText = getString(R.string.settings_status_enabled),
            disabledText = getString(R.string.settings_status_disabled)
        )
    }

    private fun bindStaticStatusRow(containerId: Int, labelRes: Int) {
        val row = requireView().findViewById<View>(containerId)
        row.findViewById<TextView>(R.id.status_label).setText(labelRes)
    }

    private fun renderStatusBadge(
        textView: TextView,
        enabled: Boolean,
        enabledText: String,
        disabledText: String
    ) {
        textView.text = if (enabled) enabledText else disabledText
        textView.background = badgeBackground(enabled)
        textView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (enabled) R.color.success_text else R.color.text_secondary
            )
        )
    }

    private fun badgeBackground(enabled: Boolean): Drawable? {
        return ContextCompat.getDrawable(
            requireContext(),
            if (enabled) R.drawable.bg_status_chip_success else R.drawable.bg_status_chip_neutral
        )
    }

    private fun isServiceEnabled(context: Context): Boolean {
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
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