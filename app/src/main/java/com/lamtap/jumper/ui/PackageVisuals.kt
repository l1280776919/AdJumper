package com.lamtap.jumper.ui

object PackageVisuals {
    fun displayName(packageName: String): String {
        return when (packageName) {
            "com.ss.android.ugc.aweme" -> "抖音"
            "com.ss.android.article.news" -> "今日头条"
            "com.tencent.news" -> "腾讯新闻"
            "com.qiyi.video" -> "爱奇艺"
            "tv.danmaku.bili" -> "哔哩哔哩"
            "com.tencent.qqlive" -> "腾讯视频"
            "com.youku.phone" -> "优酷视频"
            "com.dragon.read" -> "番茄免费小说"
            "com.smile.gifmaker" -> "快手"
            "com.sina.weibo" -> "微博"
            else -> packageName.substringAfterLast('.')
        }
    }

    fun badgeText(packageName: String): String {
        return when (packageName) {
            "com.ss.android.ugc.aweme" -> "抖"
            "com.ss.android.article.news" -> "头"
            "com.tencent.news" -> "新"
            "com.qiyi.video" -> "艺"
            "tv.danmaku.bili" -> "哔"
            "com.tencent.qqlive" -> "视"
            "com.youku.phone" -> "优"
            "com.dragon.read" -> "番"
            "com.smile.gifmaker" -> "快"
            "com.sina.weibo" -> "微"
            else -> packageName.substringAfterLast('.').take(1).uppercase()
        }
    }
}