package com.lamtap.jumper.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lamtap.jumper.R
import com.lamtap.jumper.stats.SkipStatsStore
import java.util.ArrayDeque

class JumperAccessibilityService : AccessibilityService() {
    private val packageRules by lazy { SkipRule.defaultRules(resources) }
    private var lastHandledPackage: String? = null
    private var lastHandledAtMillis: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "service connected, rules=${packageRules.size}")
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

        val clicked = performClick(match)
        val cost = SystemClock.elapsedRealtime() - startedAt
        if (clicked) {
            lastHandledPackage = packageName
            lastHandledAtMillis = now
            SkipStatsStore.recordSkip(this, packageName)
            Log.i(TAG, "skip success package=$packageName selector=${match.matchedBy} cost=${cost}ms")
        } else {
            Log.w(TAG, "skip click-failed package=$packageName selector=${match.matchedBy} cost=${cost}ms")
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

    private fun performClick(match: NodeMatch): Boolean {
        val clickableNode = findClickableAncestor(match.node)
        if (clickableNode != null && clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }

        val bounds = Rect()
        match.node.getBoundsInScreen(bounds)
        if (bounds.isEmpty) {
            return false
        }

        val path = Path().apply {
            moveTo(bounds.exactCenterX(), bounds.exactCenterY())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, GESTURE_DURATION_MS))
            .build()
        return dispatchGesture(gesture, null, null)
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

    private data class NodeMatch(
        val node: AccessibilityNodeInfo,
        val matchedBy: String
    )

    private data class SkipRule(
        val packageName: String,
        val keywords: Set<String>,
        val viewIds: Set<String>,
        val descriptions: Set<String>
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

        companion object {
            fun defaultRules(resources: android.content.res.Resources): Map<String, SkipRule> {
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
        }
    }

    companion object {
        private const val TAG = "JumperService"
        private const val EVENT_THROTTLE_MS = 600L
        private const val GESTURE_DURATION_MS = 40L
    }
}