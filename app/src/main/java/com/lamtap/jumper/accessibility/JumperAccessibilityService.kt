package com.lamtap.jumper.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.lamtap.jumper.R
import com.lamtap.jumper.stats.SkipStatsStore
import com.lamtap.jumper.ui.UiPreferencesStore
import java.util.ArrayDeque

data class SkipRule(
    val packageName: String,
    val keywords: Set<String>,
    val viewIds: Set<String>,
    val descriptions: Set<String>,
    val actionType: String = "click",
    val delay: Int = 0
) {
    fun matchReason(node: AccessibilityNodeInfo): String? {
        val textValue = node.text?.toString()?.trim().orEmpty()
        if (textValue.isNotEmpty() && keywords.any { textValue.contains(it, ignoreCase = true) }) {
            return "text"
        }

        val descriptionValue = node.contentDescription?.toString()?.trim().orEmpty()
        if (descriptionValue.isNotEmpty() && descriptions.any {
                descriptionValue.contains(it, ignoreCase = true)
            }
        ) {
            return "desc"
        }

        val viewIdValue = node.viewIdResourceName?.trim().orEmpty()
        if (viewIdValue.isNotEmpty() && viewIds.any { viewIdValue.contains(it, ignoreCase = true) }) {
            return "id"
        }

        return null
    }
}

class JumperAccessibilityService : AccessibilityService() {
    private var packageRules = emptyMap<String, SkipRule>()
    private var lastHandledPackage: String? = null
    private var lastHandledAtMillis: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        createNotificationChannel()
        startForegroundNotification()
        reloadRules()
        Log.i(TAG, "service connected, rules=${packageRules.size}")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "跳广告服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "保持无障碍服务在后台运行"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("跳广告")
            .setContentText("服务运行中，正在监控启动广告")
            .setSmallIcon(R.drawable.ic_jumper_mark)
            .setOngoing(true)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun reloadRules() {
        val defaults = loadDefaultRules()
        val custom = RuleStore.getEnabledRules(this)
        val customPackages = UiPreferencesStore.getCustomPackages(this)
        val merged = RuleStore.mergeWithDefaults(defaults, custom).toMutableMap()
        customPackages.forEach { pkg ->
            if (pkg !in merged) {
                val keywords = resources.getStringArray(R.array.default_skip_keywords).toSet()
                merged[pkg] = SkipRule(
                    packageName = pkg,
                    keywords = keywords,
                    viewIds = setOf("skip", "close", "countdown"),
                    descriptions = keywords + setOf("close")
                )
            }
        }
        packageRules = merged
        Log.i(TAG, "rules reloaded: ${packageRules.size} packages (custom=${customPackages.size})")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            return
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        if (isSystemPackage(packageName)) return
        val rule = packageRules[packageName] ?: return

        val now = SystemClock.elapsedRealtime()
        if (packageName == lastHandledPackage && now - lastHandledAtMillis < EVENT_THROTTLE_MS) {
            return
        }

        val root = rootInActiveWindow ?: run {
            Log.d(TAG, "skip root=null package=$packageName")
            return
        }

        val startedAt = SystemClock.elapsedRealtime()
        val match = findMatchingNode(root, rule) ?: run {
            Log.d(TAG, "skip no-match package=$packageName")
            return
        }

        if (rule.delay > 0) {
            android.os.Handler(mainLooper).postDelayed({
                val clicked = performClick(match, rule)
                val cost = SystemClock.elapsedRealtime() - startedAt + rule.delay * 1000L
                recordResult(packageName, match.matchedBy, clicked, cost)
            }, rule.delay * 1000L)
        } else {
            val clicked = performClick(match, rule)
            val cost = SystemClock.elapsedRealtime() - startedAt
            recordResult(packageName, match.matchedBy, clicked, cost)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "service interrupted")
    }

    private fun findMatchingNode(root: AccessibilityNodeInfo, rule: SkipRule): NodeMatch? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val matchedBy = rule.matchReason(node)
            if (matchedBy != null) {
                return NodeMatch(node, matchedBy)
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::addLast)
            }
        }

        return null
    }

    private fun recordResult(packageName: String, matchedBy: String, clicked: Boolean, cost: Long) {
        if (clicked) {
            lastHandledPackage = packageName
            lastHandledAtMillis = SystemClock.elapsedRealtime()
            SkipStatsStore.recordSkip(this, packageName, matchedBy)
            Log.i(TAG, "skip success package=$packageName selector=$matchedBy cost=${cost}ms")
        } else {
            Log.w(TAG, "skip click-failed package=$packageName selector=$matchedBy cost=${cost}ms")
        }
    }

    private fun performClick(match: NodeMatch, rule: SkipRule): Boolean {
        when (rule.actionType) {
            "log_only" -> {
                Log.i(TAG, "log-only match package=${match.node.toString()} selector=${match.matchedBy}")
                return true
            }
            "coordinate" -> {
                val bounds = Rect()
                match.node.getBoundsInScreen(bounds)
                if (bounds.isEmpty) return false
                val path = Path().apply {
                    moveTo(bounds.exactCenterX(), bounds.exactCenterY())
                }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, GESTURE_DURATION_MS))
                    .build()
                return dispatchGesture(gesture, null, null)
            }
            else -> {
                val clickableNode = findClickableAncestor(match.node)
                if (clickableNode != null && clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                val bounds = Rect()
                match.node.getBoundsInScreen(bounds)
                if (bounds.isEmpty) return false
                val path = Path().apply {
                    moveTo(bounds.exactCenterX(), bounds.exactCenterY())
                }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, GESTURE_DURATION_MS))
                    .build()
                return dispatchGesture(gesture, null, null)
            }
        }
    }

    private fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable && current.isVisibleToUser) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private val systemPackages = setOf(
        "android", "com.android.systemui", "com.android.settings",
        "com.android.launcher", "com.android.launcher3",
        "com.android.incallui", "com.android.phone",
        "com.android.dialer", "com.android.contacts",
        "com.android.mms", "com.android.calendar",
        "com.android.camera", "com.android.gallery3d",
        "com.android.filemanager", "com.android.documentsui",
        "com.android.vending", "com.android.chrome",
        "com.google.android.gms", "com.google.android.gsf",
        "com.qualcomm.qti.telephonyservice",
        "com.android.server.telecom",
        "com.miui.home", "com.miui.securitycenter",
        "com.miui.permissionmanager", "com.miui.packageinstaller",
        "com.xiaomi.market", "com.xiaomi.mipicks",
        "com.huawei.systemmanager", "com.huawei.android.launcher"
    )

    private fun isSystemPackage(packageName: String): Boolean {
        if (packageName in systemPackages) return true
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
        } catch (e: Exception) {
            false
        }
    }

    private data class NodeMatch(
        val node: AccessibilityNodeInfo,
        val matchedBy: String
    )

    private fun loadDefaultRules(): Map<String, SkipRule> {
        val packageNames = resources.getStringArray(R.array.supported_packages)
        val keywords = resources.getStringArray(R.array.default_skip_keywords).toSet()
        return packageNames.associateWith { packageName ->
            SkipRule(
                packageName = packageName,
                keywords = keywords,
                viewIds = setOf("skip", "close", "countdown"),
                descriptions = keywords + setOf("close")
            )
        }
    }

    companion object {
        private const val TAG = "JumperService"
        private const val EVENT_THROTTLE_MS = 600L
        private const val GESTURE_DURATION_MS = 40L
        private const val CHANNEL_ID = "jumper_service_channel"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var instance: JumperAccessibilityService? = null
            private set

        fun reloadRulesIfRunning() {
            instance?.reloadRules()
        }
    }
}
