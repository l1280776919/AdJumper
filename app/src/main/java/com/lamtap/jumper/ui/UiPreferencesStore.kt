package com.lamtap.jumper.ui

import android.content.Context
import org.json.JSONArray

object UiPreferencesStore {
    private const val PREFS_NAME = "jumper_ui_prefs"
    private const val KEY_PRIVACY_ACCEPTED = "privacy_accepted"
    private const val KEY_AUTO_START = "auto_start"
    private const val KEY_NOTIFICATION = "notification"
    private const val KEY_POWER_SAVE = "power_save"
    private const val KEY_CUSTOM_PACKAGES = "custom_packages"

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

    fun isAutoStart(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_START, true)
    }

    fun setAutoStart(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }

    fun isNotification(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATION, true)
    }

    fun setNotification(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NOTIFICATION, enabled).apply()
    }

    fun isPowerSave(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_POWER_SAVE, false)
    }

    fun setPowerSave(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_POWER_SAVE, enabled).apply()
    }

    fun getCustomPackages(context: Context): List<String> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_PACKAGES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addCustomPackage(context: Context, packageName: String) {
        val current = getCustomPackages(context).toMutableList()
        if (packageName !in current) {
            current.add(packageName)
            saveCustomPackages(context, current)
        }
    }

    fun removeCustomPackage(context: Context, packageName: String) {
        val current = getCustomPackages(context).toMutableList()
        current.remove(packageName)
        saveCustomPackages(context, current)
    }

    private fun saveCustomPackages(context: Context, packages: List<String>) {
        val arr = JSONArray()
        packages.forEach { arr.put(it) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CUSTOM_PACKAGES, arr.toString()).apply()
    }
}
