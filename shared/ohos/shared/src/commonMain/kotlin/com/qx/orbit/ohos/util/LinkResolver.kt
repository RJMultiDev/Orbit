package com.qx.orbit.ohos.util

/**
 * b23.tv 短链解析（对应 Android util/LinkResolver.kt）。
 *
 * Android 用 OkHttp 跟随重定向取得最终 URL，再从 URL 中抽出 BV 号；
 * Kuikly commonMain 无 OkHttp，网络跟随由平台层注入 [setRedirectFollower]
 * （ArkTS @ohos.net.http / Kuikly NetworkModule 桥），本文件保持纯算法。
 */
object LinkResolver {

    private val BV_REGEX = Regex("BV[A-Za-z0-9]+", RegexOption.IGNORE_CASE)

    /** 短链跟随实现注入点：输入短链，返回跟随重定向后的最终 URL（失败返回 null）。 */
    private var redirectFollower: (String) -> String? = { null }

    /** 注入平台短链跟随实现（启动时由宿主设置，如 NetworkModule 桥）。 */
    fun setRedirectFollower(follower: (String) -> String?) {
        redirectFollower = follower
    }

    /** 从（最终）URL 中提取 BV 号；无则返回 null。 */
    fun extractBv(finalUrl: String): String? = BV_REGEX.find(finalUrl)?.value

    /**
     * 解析 b23.tv 短链为 BV 号：跟随重定向取最终 URL 后抽 BV。
     * 未注入 follower 或跟随失败时返回 null（调用方可回退到原文本展示）。
     */
    fun resolveB23Link(url: String): String? {
        val finalUrl = try {
            redirectFollower(url)
        } catch (_: Throwable) {
            null
        } ?: return null
        return extractBv(finalUrl)
    }
}
