package com.qx.orbit.ohos.util

/**
 * 数字/链接格式化（对应 Android util/FormatUtil.kt）。
 * 纯 Kotlin 实现，无 String.format / java.util 依赖。
 */

/**
 * 播放量等数字格式化：>=1 万显示 "x.x万"（四舍五入保留一位小数），否则原样。
 * 对应 Android `formatCount(Int)`（String.format("%.1f万") 的等价实现）。
 */
fun formatCount(count: Int): String = formatCount(count.toLong())

/** Long 重载（B 站接口 stat 字段多为 Long，对齐首页/详情现有用法）。 */
fun formatCount(count: Long): String {
    return if (count >= 10000) {
        // 等价 %.1f 的 round-half-up：tenths = round(count/10000 * 10)
        val tenths = (count * 10 + 5000) / 10000
        "${tenths / 10}.${tenths % 10}万"
    } else {
        count.toString()
    }
}

/**
 * 封面/头像 URL 修复：`//` 补全 https、http 升级为 https、avif 转 webp。
 * 对应 Android `String.fixCoverUrl()`。
 */
fun String.fixCoverUrl(): String {
    return when {
        this.startsWith("//") -> "https:$this"
        this.startsWith("http://") -> this.replaceFirst("http://", "https://")
        else -> this
    }.replace(".avif", ".webp")
}
