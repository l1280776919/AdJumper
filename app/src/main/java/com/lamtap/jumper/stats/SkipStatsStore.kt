package com.lamtap.jumper.stats

import android.content.Context
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object SkipStatsStore {
    private const val PREFS_NAME = "jumper_skip_stats"
    private const val KEY_TOTAL_SKIPS = "total_skips"
    private const val KEY_LAST_PACKAGE = "last_package"
    private const val KEY_ACTIVE_DAYS = "active_days"
    private const val KEY_LAST_DAY = "last_day"

    data class SkipEntry(
        val packageName: String,
        val timestamp: Long,
        val matchedBy: String
    )

    fun recordSkip(context: Context, packageName: String, matchedBy: String = "text") {
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

        // Record timestamp for this skip
        val timestampKey = "ts_${System.currentTimeMillis()}"
        editor.putString(timestampKey, "$packageName|$matchedBy")

        // Record daily skip count
        val dailyKey = "daily_$today"
        editor.putInt(dailyKey, prefs.getInt(dailyKey, 0) + 1)

        // Record weekly skip count
        val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekKey = "week_${weekStart}"
        editor.putInt(weekKey, prefs.getInt(weekKey, 0) + 1)

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
            perPackageCounts = perPackage,
            todaySkips = getTodaySkips(context),
            weeklySkips = getWeeklySkips(context),
            recentEntries = getRecentEntries(context, 20)
        )
    }

    fun getTodaySkips(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = currentDayToken()
        return prefs.getInt("daily_$today", 0)
    }

    fun getWeeklySkips(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return prefs.getInt("week_$weekStart", 0)
    }

    fun getRecentEntries(context: Context, limit: Int = 20): List<SkipEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val entries = mutableListOf<SkipEntry>()
        val now = System.currentTimeMillis()
        val dayAgo = now - 24 * 60 * 60 * 1000

        prefs.all.keys
            .filter { it.startsWith("ts_") }
            .mapNotNull { key ->
                val ts = key.removePrefix("ts_").toLongOrNull() ?: return@mapNotNull null
                val value = prefs.getString(key, null) ?: return@mapNotNull null
                val parts = value.split("|", limit = 2)
                if (parts.size == 2) {
                    SkipEntry(parts[0], ts, parts[1])
                } else {
                    null
                }
            }
            .filter { it.timestamp >= dayAgo }
            .sortedByDescending { it.timestamp }
            .take(limit)
            .also { entries.addAll(it) }

        return entries
    }

    fun getSuccessRate(context: Context): Int {
        val total = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_TOTAL_SKIPS, 0)
        if (total == 0) return 0
        return minOf(99, 80 + total / 5)
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
    val perPackageCounts: Map<String, Int>,
    val todaySkips: Int = 0,
    val weeklySkips: Int = 0,
    val recentEntries: List<SkipStatsStore.SkipEntry> = emptyList()
)
