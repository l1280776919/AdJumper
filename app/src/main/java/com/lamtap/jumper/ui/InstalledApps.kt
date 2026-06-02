package com.lamtap.jumper.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object InstalledApps {

    data class AppInfo(
        val packageName: String,
        val label: String,
        val isSystemApp: Boolean
    )

    fun getNonSystemApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    label = app.loadLabel(pm).toString(),
                    isSystemApp = false
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun getAdSdkApps(context: Context): List<AppInfo> {
        val adSdkPackages = setOf(
            "com.ss.android.ugc.aweme",
            "com.ss.android.article.news",
            "com.tencent.news",
            "com.qiyi.video",
            "tv.danmaku.bili",
            "com.tencent.qqlive",
            "com.youku.phone",
            "com.dragon.read",
            "com.smile.gifmaker",
            "com.sina.weibo",
            "com.sina.news",
            "com.jingdong.app.mall",
            "com.taobao.taobao",
            "com.eg.android.AlipayGphone",
            "com.tencent.mm",
            "com.tencent.mobileqq",
            "com.baidu.searchbox",
            "com.baidu.BaiduMap",
            "com.baidu.netdisk",
            "com.netease.cloudmusic",
            "com.kugou.android",
            "com.kuwo.player",
            "com.autonavi.minimap",
            "com.baidu.duer.superapp",
            "com.zhihu.android",
            "com.douban.frodo",
            "com.toutiao.news",
            "com.hunantv.imgo.activity",
            "com.moji.mjweather",
            "com.cainiao.wireless"
        )

        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val installedPackages = installed.map { it.packageName }.toSet()

        return adSdkPackages
            .filter { it in installedPackages }
            .map { pkg ->
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    AppInfo(
                        packageName = pkg,
                        label = appInfo.loadLabel(pm).toString(),
                        isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                    )
                } catch (e: Exception) {
                    AppInfo(pkg, pkg.substringAfterLast('.'), false)
                }
            }
            .filter { !it.isSystemApp }
            .sortedBy { it.label.lowercase() }
    }

    fun getAppLabel(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appInfo.loadLabel(pm).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    fun isSystemApp(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        } catch (e: Exception) {
            false
        }
    }
}
