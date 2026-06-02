package com.lamtap.jumper.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.accessibility.AccessibilityManager
import com.lamtap.jumper.R
import com.lamtap.jumper.accessibility.JumperAccessibilityService

class JumperTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } else {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        if (isServiceEnabled()) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "跳广告 运行中"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "跳广告 已关闭"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = if (isServiceEnabled()) "运行中" else "已关闭"
        }
        tile.updateTile()
    }

    private fun isServiceEnabled(): Boolean {
        val accessibilityManager =
            getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedComponent = ComponentName(this, JumperAccessibilityService::class.java)
        return enabledServices.any { serviceInfo ->
            serviceInfo.resolveInfo.serviceInfo?.let { serviceInfoData ->
                ComponentName(serviceInfoData.packageName, serviceInfoData.name) == expectedComponent
            } ?: false
        }
    }
}
