package com.qx.orbit.core.net

/**
 * 纯算法的 Cookie 模型与规则（RFC 6265 子集）。
 *
 * 覆盖 Set-Cookie 解析、Cookie 请求头序列化、domain/path/secure/httpOnly 匹配规则；
 * 不涉及存储、过期时间计算（max-age/expires 不参与匹配）。
 */
data class Cookie(
    val name: String,
    val value: String,
    /** 规范化后的 domain（小写、无前导 `.`）。host-only cookie 在 [bindHost] 前为空串。 */
    val domain: String = "",
    /** 是否来自未显式声明 Domain 属性的 Set-Cookie（host-only cookie）。 */
    val hostOnly: Boolean = true,
    val path: String = "/",
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
) {
    /** 请求头形式。 */
    fun toRequestHeader(): String = "$name=$value"

    companion object {

        /**
         * 解析单个 Set-Cookie 头（RFC 6265 子集）。
         * 属性名不区分大小写；Domain 去前导 `.` 并转小写；Path 非 `/` 开头时回退为 `/`。
         * 解析失败（空输入、无 `=`、空 name）返回 null，对齐 app 侧 `eqIdx <= 0` 的跳过逻辑。
         */
        fun parseSetCookie(header: String): Cookie? {
            val parts = header.split(';')
            val nameValue = parts[0].trim()
            val eq = nameValue.indexOf('=')
            if (eq <= 0) return null
            val name = nameValue.substring(0, eq)
            val value = nameValue.substring(eq + 1)

            var domain = ""
            var hostOnly = true
            var path = "/"
            var secure = false
            var httpOnly = false

            for (i in 1 until parts.size) {
                val attr = parts[i].trim()
                if (attr.isEmpty()) continue
                val eqIdx = attr.indexOf('=')
                val key = if (eqIdx < 0) attr else attr.substring(0, eqIdx)
                val attrValue = if (eqIdx < 0) null else attr.substring(eqIdx + 1)
                when (key.lowercase()) {
                    "domain" -> {
                        domain = (attrValue ?: "").trim().removePrefix(".").lowercase()
                        hostOnly = false
                    }
                    "path" -> {
                        val p = (attrValue ?: "").trim()
                        if (p.startsWith("/")) path = p
                    }
                    "secure" -> secure = true
                    "httponly" -> httpOnly = true
                }
            }
            return Cookie(name, value, domain, hostOnly, path, secure, httpOnly)
        }

        /**
         * 解析 Cookie 请求头：`k=v; k=v`。value 可含 `=`（仅按首个 `=` 分割）；
         * 跳过无 `=` 或空 name 的片段。
         */
        fun parseRequestHeader(header: String): List<Pair<String, String>> {
            if (header.isEmpty()) return emptyList()
            val result = mutableListOf<Pair<String, String>>()
            for (part in header.split(';')) {
                val trimmed = part.trim()
                if (trimmed.isEmpty()) continue
                val eq = trimmed.indexOf('=')
                if (eq <= 0) continue
                result += trimmed.substring(0, eq) to trimmed.substring(eq + 1)
            }
            return result
        }

        /** 序列化请求头：`k=v; k=v`。 */
        fun toRequestHeader(cookies: List<Cookie>): String =
            cookies.joinToString("; ") { it.toRequestHeader() }

        /** host-only cookie 在存储时应绑定为来源 host（RFC 6265 5.3），以便后续匹配。 */
        fun bindHost(cookie: Cookie, host: String): Cookie {
            if (!cookie.hostOnly) return cookie
            return cookie.copy(domain = host.lowercase())
        }

        /** RFC 6265 5.1.3 domain-match：host == domain，或 host 是 domain 的 `.<domain>` 子域。 */
        fun domainMatches(cookieDomain: String, host: String): Boolean {
            val d = cookieDomain.lowercase()
            val h = host.lowercase()
            return h == d || h.endsWith(".$d")
        }

        /** RFC 6265 5.1.4 path-match。 */
        fun pathMatches(cookiePath: String, requestPath: String): Boolean {
            val cp = cookiePath.ifEmpty { "/" }
            val rp = requestPath.ifEmpty { "/" }
            if (!rp.startsWith(cp)) return false
            if (rp.length == cp.length) return true
            if (cp.endsWith("/")) return true
            return rp[cp.length] == '/'
        }

        /** 该 cookie 是否可随 `host`/`path` 的请求发送（RFC 6265 5.4）。 */
        fun canSendTo(cookie: Cookie, host: String, path: String, secureConnection: Boolean): Boolean {
            if (cookie.secure && !secureConnection) return false
            val hostOk = if (cookie.hostOnly) {
                cookie.domain.isNotEmpty() && cookie.domain.equals(host, ignoreCase = true)
            } else {
                domainMatches(cookie.domain, host)
            }
            if (!hostOk) return false
            return pathMatches(cookie.path, path)
        }

        /** domain 是否落在允许的后缀内（如 `bilibili.com`），对齐 app 侧 `endsWith("bilibili.com")`。 */
        fun isDomainAllowed(cookieDomain: String, allowedSuffix: String): Boolean {
            val d = cookieDomain.lowercase()
            val s = allowedSuffix.lowercase()
            return d == s || d.endsWith(".$s")
        }
    }
}
