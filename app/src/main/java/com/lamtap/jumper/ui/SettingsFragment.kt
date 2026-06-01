package com.lamtap.jumper.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.lamtap.jumper.R

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Switch>(R.id.switch_auto_start).isChecked = true
        view.findViewById<Switch>(R.id.switch_notification).isChecked = true
        view.findViewById<Switch>(R.id.switch_power_save).isChecked = false
    }
}