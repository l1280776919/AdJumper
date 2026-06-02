package com.lamtap.jumper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lamtap.jumper.ui.DashboardFragment
import com.lamtap.jumper.ui.LogsFragment
import com.lamtap.jumper.ui.RulesFragment
import com.lamtap.jumper.ui.SettingsFragment
import com.lamtap.jumper.ui.UiPreferencesStore

class MainActivity : AppCompatActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val contentContainer = findViewById<View>(R.id.content_container)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_container)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            contentContainer.setPadding(
                contentContainer.paddingLeft,
                systemBars.top,
                contentContainer.paddingRight,
                0
            )

            bottomNavigation.setPadding(
                bottomNavigation.paddingLeft,
                0,
                bottomNavigation.paddingRight,
                systemBars.bottom
            )

            insets
        }

        bottomNavigation.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_UNLABELED
        bottomNavigation.isItemHorizontalTranslationEnabled = false

        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> DashboardFragment()
                R.id.nav_rules -> RulesFragment()
                R.id.nav_logs -> LogsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> DashboardFragment()
            }
            showFragment(fragment)
            true
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
            maybeShowPrivacyDialog()
            requestNotificationPermission()
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_container, fragment)
            .commit()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun maybeShowPrivacyDialog() {
        if (UiPreferencesStore.isPrivacyAccepted(this)) {
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_privacy, null, false)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<Button>(R.id.button_exit).setOnClickListener {
            dialog.dismiss()
            finish()
        }
        dialogView.findViewById<Button>(R.id.button_accept).setOnClickListener {
            dialog.dismiss()
                UiPreferencesStore.markPrivacyAccepted(this)
        }

        dialog.show()
    }
}
