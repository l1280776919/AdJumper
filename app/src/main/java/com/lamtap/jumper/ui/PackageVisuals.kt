package com.lamtap.jumper.ui

import android.content.Context

object PackageVisuals {
    private val knownApps = mapOf(
        "com.ss.android.ugc.aweme" to "抖音",
        "com.ss.android.article.news" to "今日头条",
        "com.tencent.news" to "腾讯新闻",
        "com.qiyi.video" to "爱奇艺",
        "tv.danmaku.bili" to "哔哩哔哩",
        "com.tencent.qqlive" to "腾讯视频",
        "com.youku.phone" to "优酷视频",
        "com.dragon.read" to "番茄免费小说",
        "com.smile.gifmaker" to "快手",
        "com.sina.weibo" to "微博",
        "com.sina.news" to "新浪新闻",
        "com.jingdong.app.mall" to "京东",
        "com.taobao.taobao" to "淘宝",
        "com.eg.android.AlipayGphone" to "支付宝",
        "com.tencent.mm" to "微信",
        "com.tencent.mobileqq" to "QQ",
        "com.baidu.searchbox" to "百度",
        "com.baidu.BaiduMap" to "百度地图",
        "com.baidu.netdisk" to "百度网盘",
        "com.netease.cloudmusic" to "网易云音乐",
        "com.kugou.android" to "酷狗音乐",
        "com.kuwo.player" to "酷我音乐",
        "com.autonavi.minimap" to "高德地图",
        "com.zhihu.android" to "知乎",
        "com.douban.frodo" to "豆瓣",
        "com.toutiao.news" to "今日头条",
        "com.hunantv.imgo.activity" to "芒果TV",
        "com.moji.mjweather" to "墨迹天气",
        "com.cainiao.wireless" to "菜鸟"
    )

    private val badges = mapOf(
        "com.ss.android.ugc.aweme" to "抖",
        "com.ss.android.article.news" to "头",
        "com.tencent.news" to "新",
        "com.qiyi.video" to "艺",
        "tv.danmaku.bili" to "哔",
        "com.tencent.qqlive" to "视",
        "com.youku.phone" to "优",
        "com.dragon.read" to "番",
        "com.smile.gifmaker" to "快",
        "com.sina.weibo" to "微",
        "com.sina.news" to "浪",
        "com.jingdong.app.mall" to "京",
        "com.taobao.taobao" to "淘",
        "com.eg.android.AlipayGphone" to "支",
        "com.tencent.mm" to "微",
        "com.tencent.mobileqq" to "Q",
        "com.baidu.searchbox" to "百",
        "com.baidu.BaiduMap" to "地",
        "com.baidu.netdisk" to "盘",
        "com.netease.cloudmusic" to "网",
        "com.kugou.android" to "酷",
        "com.kuwo.player" to "酷",
        "com.autonavi.minimap" to "高",
        "com.zhihu.android" to "知",
        "com.douban.frodo" to "豆",
        "com.toutiao.news" to "头",
        "com.hunantv.imgo.activity" to "芒",
        "com.moji.mjweather" to "墨",
        "com.cainiao.wireless" to "菜"
    )

    fun displayName(packageName: String): String {
        return knownApps[packageName] ?: packageName.substringAfterLast('.')
    }

    fun displayName(context: Context, packageName: String): String {
        knownApps[packageName]?.let { return it }
        return InstalledApps.getAppLabel(context, packageName)
    }

    fun badgeText(packageName: String): String {
        return badges[packageName] ?: packageName.substringAfterLast('.').take(1).uppercase()
    }
}
