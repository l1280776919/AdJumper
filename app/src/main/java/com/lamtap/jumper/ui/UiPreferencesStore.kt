package com.lamtap.jumper.ui

import android.content.Context

object UiPreferencesStore {
    private const val PREFS_NAME = "jumper_ui_prefs"
    private const val KEY_PRIVACY_ACCEPTED = "privacy_accepted"

    fun isPrivacyAccepted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PRIVACY_ACCEPTED, false)
    }

    fun markPrivacyAccepted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PRIVACY_ACCEPTED, true)
            .apply()
    }
}