package com.lamtap.jumper.accessibility

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object RuleStore {
    private const val PREFS_NAME = "jumper_rules"
    private const val KEY_RULES = "custom_rules"

    data class CustomRule(
        val id: String,
        val name: String,
        val packageName: String,
        val enabled: Boolean = true,
        val priority: Int = 10,
        val keywords: Set<String> = emptySet(),
        val viewIds: Set<String> = emptySet(),
        val descriptions: Set<String> = emptySet(),
        val actionType: String = "click",
        val delay: Int = 0
    )

    fun getAllRules(context: Context): List<CustomRule> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RULES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i -> parseRule(array.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getEnabledRules(context: Context): List<CustomRule> {
        return getAllRules(context).filter { it.enabled }
    }

    fun getRulesByPackage(context: Context, packageName: String): List<CustomRule> {
        return getAllRules(context).filter { it.packageName == packageName }
    }

    fun addRule(context: Context, rule: CustomRule) {
        val rules = getAllRules(context).toMutableList()
        val existingIndex = rules.indexOfFirst { it.id == rule.id }
        if (existingIndex >= 0) {
            rules[existingIndex] = rule
        } else {
            rules.add(rule)
        }
        saveRules(context, rules)
    }

    fun removeRule(context: Context, ruleId: String) {
        val rules = getAllRules(context).toMutableList()
        rules.removeAll { it.id == ruleId }
        saveRules(context, rules)
    }

    fun clearAllRules(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_RULES).apply()
    }

    fun importRules(context: Context, rules: List<CustomRule>) {
        val existing = getAllRules(context).toMutableList()
        val existingIds = existing.map { it.id }.toSet()
        rules.forEach { rule ->
            if (rule.id in existingIds) {
                val idx = existing.indexOfFirst { it.id == rule.id }
                if (idx >= 0) existing[idx] = rule
            } else {
                existing.add(rule)
            }
        }
        saveRules(context, existing)
    }

    fun exportJson(context: Context): String {
        val rules = getAllRules(context)
        val array = JSONArray()
        rules.forEach { rule ->
            val obj = JSONObject().apply {
                put("id", rule.id)
                put("name", rule.name)
                put("packageName", rule.packageName)
                put("enabled", rule.enabled)
                put("priority", rule.priority)
                put("keywords", JSONArray(rule.keywords.toList()))
                put("viewIds", JSONArray(rule.viewIds.toList()))
                put("descriptions", JSONArray(rule.descriptions.toList()))
                put("actionType", rule.actionType)
                put("delay", rule.delay)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    fun importFromJson(context: Context, json: String): Boolean {
        return try {
            val array = JSONArray(json)
            val rules = (0 until array.length()).map { i -> parseRule(array.getJSONObject(i)) }
            importRules(context, rules)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun mergeWithDefaults(
        defaultRules: Map<String, SkipRule>,
        customRules: List<CustomRule>
    ): Map<String, SkipRule> {
        val merged = defaultRules.toMutableMap()
        customRules.filter { it.enabled }.forEach { custom ->
            val existing = merged[custom.packageName]
            if (existing != null) {
                merged[custom.packageName] = existing.copy(
                    keywords = existing.keywords + custom.keywords,
                    viewIds = existing.viewIds + custom.viewIds,
                    descriptions = existing.descriptions + custom.descriptions,
                    actionType = custom.actionType,
                    delay = custom.delay
                )
            } else {
                merged[custom.packageName] = SkipRule(
                    packageName = custom.packageName,
                    keywords = custom.keywords,
                    viewIds = custom.viewIds,
                    descriptions = custom.descriptions,
                    actionType = custom.actionType,
                    delay = custom.delay
                )
            }
        }
        return merged
    }

    private fun parseRule(obj: JSONObject): CustomRule {
        return CustomRule(
            id = obj.optString("id", ""),
            name = obj.optString("name", ""),
            packageName = obj.optString("packageName", ""),
            enabled = obj.optBoolean("enabled", true),
            priority = obj.optInt("priority", 10),
            keywords = jsonArrayToStringSet(obj.optJSONArray("keywords")),
            viewIds = jsonArrayToStringSet(obj.optJSONArray("viewIds")),
            descriptions = jsonArrayToStringSet(obj.optJSONArray("descriptions")),
            actionType = obj.optString("actionType", "click"),
            delay = obj.optInt("delay", 0)
        )
    }

    private fun jsonArrayToStringSet(arr: JSONArray?): Set<String> {
        if (arr == null) return emptySet()
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    private fun saveRules(context: Context, rules: List<CustomRule>) {
        val array = JSONArray()
        rules.forEach { rule ->
            val obj = JSONObject().apply {
                put("id", rule.id)
                put("name", rule.name)
                put("packageName", rule.packageName)
                put("enabled", rule.enabled)
                put("priority", rule.priority)
                put("keywords", JSONArray(rule.keywords.toList()))
                put("viewIds", JSONArray(rule.viewIds.toList()))
                put("descriptions", JSONArray(rule.descriptions.toList()))
                put("actionType", rule.actionType)
                put("delay", rule.delay)
            }
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_RULES, array.toString()).apply()
    }
}
