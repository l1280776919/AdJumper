package com.lamtap.jumper.stats

import android.content.Context

object SkipStatsStore {
    private const val PREFS_NAME = "jumper_skip_stats"
    private const val KEY_TOTAL_SKIPS = "total_skips"
    private const val KEY_LAST_PACKAGE = "last_package"
    private const val KEY_ACTIVE_DAYS = "active_days"
    private const val KEY_LAST_DAY = "last_day"

    fun recordSkip(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = currentDayToken()
        val storedDay = prefs.getString(KEY_LAST_DAY, null)
        val editor = prefs.edit()

        editor.putInt(KEY_TOTAL_SKIPS, prefs.getInt(KEY_TOTAL_SKIPS, 0) + 1)
        editor.putString(KEY_LAST_PACKAGE, packageName)

        if (storedDay != today) {
            editor.putInt(KEY_ACTIVE_DAYS, prefs.getInt(KEY_ACTIVE_DAYS, 0) + 1)
            editor.putString(KEY_LAST_DAY, today)
        }

        val packageKey = packageCountKey(packageName)
        editor.putInt(packageKey, prefs.getInt(packageKey, 0) + 1)
        editor.apply()
    }

    fun snapshot(context: Context, trackedPackages: List<String>): SkipStatsSnapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val perPackage = trackedPackages.associateWith { packageName ->
            prefs.getInt(packageCountKey(packageName), 0)
        }

        return SkipStatsSnapshot(
            totalSkips = prefs.getInt(KEY_TOTAL_SKIPS, 0),
            lastPackage = prefs.getString(KEY_LAST_PACKAGE, null),
            activeDays = prefs.getInt(KEY_ACTIVE_DAYS, 0),
            perPackageCounts = perPackage
        )
    }

    private fun packageCountKey(packageName: String): String {
        return "package_count_$packageName"
    }

    private fun currentDayToken(): String {
        val now = java.time.LocalDate.now()
        return now.toString()
    }
}

data class SkipStatsSnapshot(
    val totalSkips: Int,
    val lastPackage: String?,
    val activeDays: Int,
    val perPackageCounts: Map<String, Int>
)