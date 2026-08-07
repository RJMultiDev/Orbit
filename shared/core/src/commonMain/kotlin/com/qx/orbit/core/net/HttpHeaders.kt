package com.qx.orbit.core.net

/**
 * 默认请求头与 Cookie 过滤/合并逻辑（纯算法）。
 *
 * 对齐 app 侧 HttpClient：
 * - [USER_AGENT]/[REFERER]/[defaultHeaders]：DefaultHeadersInterceptor；
 * - [filterSensitiveCookies]：CookieAddInterceptor（发送前剔除 B 站 WAF 不兼容字段）；
 * - [mergeCookies]：CookieSaveInterceptor（响应 Set-Cookie 合并进存储串）。
 */
object HttpHeaders {

    /** 对齐 DefaultHeadersInterceptor 的 UA（Chrome/122）。 */
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36"

    /** 对齐 DefaultHeadersInterceptor 的 Referer。 */
    const val REFERER = "https://www.bilibili.com/"

    /** 默认补全头（仅在请求未显式携带对应头时使用）。 */
    fun defaultHeaders(): Map<String, String> = mapOf(
        "User-Agent" to USER_AGENT,
        "Referer" to REFERER,
    )

    /** 与 B 站 WAF 不兼容的自定义字段，发送前过滤。 */
    private val SENSITIVE_COOKIE_KEYS = setOf("refresh_token", "access_token", "expires_in", "mid")

    /** 剔除敏感自定义字段，返回净化后的 Cookie 请求头。 */
    fun filterSensitiveCookies(rawCookie: String): String {
        val kept = Cookie.parseRequestHeader(rawCookie)
            .filter { (k, _) -> k !in SENSITIVE_COOKIE_KEYS }
        return kept.joinToString("; ") { "${it.first}=${it.second}" }
    }

    /**
     * 将响应中的 Set-Cookie 合并进现有 Cookie 串：
     * 覆盖同名 key（保留原顺序，新 key 追加在末尾）；
     * 显式声明 Domain 且不在 [allowedDomainSuffix] 内的跳过；未声明 Domain 的接受。
     */
    fun mergeCookies(
        existing: String,
        setCookies: List<String>,
        allowedDomainSuffix: String = "bilibili.com",
    ): String {
        val map = LinkedHashMap<String, String>()
        for ((k, v) in Cookie.parseRequestHeader(existing)) map[k] = v

        for (header in setCookies) {
            val cookie = Cookie.parseSetCookie(header) ?: continue
            if (!cookie.hostOnly && !Cookie.isDomainAllowed(cookie.domain, allowedDomainSuffix)) continue
            map[cookie.name] = cookie.value
        }
        return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }
}
