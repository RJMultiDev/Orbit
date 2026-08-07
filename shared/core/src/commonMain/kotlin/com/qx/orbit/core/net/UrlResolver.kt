package com.qx.orbit.core.net

/**
 * URL 解析与重定向策略（纯算法，不依赖 java.net.URI）。
 *
 * 对齐 app 侧 HttpClient.RedirectInterceptor：OkHttp `followRedirects(false)` 后手动跟随，
 * 特殊处理 b23.tv 短链——除非 http→https 同主机升级，否则不跟随（避免在手表端卡死短链）。
 */
object UrlResolver {

    /** 重定向跳数上限。 */
    const val DEFAULT_MAX_REDIRECTS = 10

    /** OkHttp Response.isRedirect 等价：300/301/302/303/307/308。 */
    fun isRedirectStatus(statusCode: Int): Boolean =
        statusCode == 300 || statusCode == 301 || statusCode == 302 ||
            statusCode == 303 || statusCode == 307 || statusCode == 308

    /**
     * 对齐 RedirectInterceptor 的 isSslRedirect：
     * 请求本身非 https、Location 为 https、且请求主机与 Location 主机一致（均忽略大小写）。
     */
    fun isSslRedirect(requestIsHttps: Boolean, requestHost: String, location: String): Boolean {
        if (requestIsHttps) return false
        val scheme = schemeOf(location) ?: return false
        if (!scheme.equals("https", ignoreCase = true)) return false
        val host = hostOf(location) ?: return false
        return host.equals(requestHost, ignoreCase = true)
    }

    /**
     * 综合判定是否跟随重定向；跟随则返回解析后的绝对 URL，否则返回 null。
     * 约束顺序：Location 非空 → 状态码为 3xx → 未超跳数上限 → b23.tv 非 SSL 升级拦截 → 解析。
     */
    fun resolveRedirect(
        requestUrl: String,
        requestIsHttps: Boolean,
        statusCode: Int,
        location: String?,
        hopsTaken: Int = 0,
        maxHops: Int = DEFAULT_MAX_REDIRECTS,
    ): String? {
        if (location.isNullOrEmpty()) return null
        if (!isRedirectStatus(statusCode)) return null
        if (hopsTaken >= maxHops) return null

        val requestHost = hostOf(requestUrl) ?: return null
        val sslUpgrade = isSslRedirect(requestIsHttps, requestHost, location)
        if (requestHost.equals("b23.tv", ignoreCase = true) && !sslUpgrade) return null
        return resolveUrl(requestUrl, location)
    }

    /** 提取 scheme（`scheme://` 前缀，RFC 3986 scheme 语法），无则返回 null。 */
    fun schemeOf(url: String): String? {
        val idx = url.indexOf("://")
        if (idx <= 0) return null
        val scheme = url.substring(0, idx)
        if (scheme.isEmpty() || !scheme[0].isLetter()) return null
        for (c in scheme) {
            if (!(c.isLetterOrDigit() || c == '+' || c == '-' || c == '.')) return null
        }
        return scheme.lowercase()
    }

    /** 提取 host（authority 中 `:` 端口之前、小写），无则返回 null。 */
    fun hostOf(url: String): String? {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd < 0) return null
        var start = schemeEnd + 3
        var end = start
        while (end < url.length) {
            val c = url[end]
            if (c == '/' || c == '?' || c == '#' || c == ':') break
            end++
        }
        if (end == start) return null
        return url.substring(start, end).lowercase()
    }

    /**
     * RFC 3986 相对 URL 解析：把 [location] 相对 [baseUrl] 解析为绝对 URL。
     * 支持：绝对 URL、`//host/path`（scheme-relative）、`/path`、`?query`、`#frag`、普通相对路径（含 `../` 去点）。
     */
    fun resolveUrl(baseUrl: String, location: String): String? {
        if (location.isEmpty()) return baseUrl
        if (schemeOf(location) != null) return location

        val base = parseUrl(baseUrl) ?: return null

        val sb = StringBuilder()
        sb.append(base.scheme).append("://").append(base.authority)

        if (location.startsWith("//")) {
            return "${base.scheme}:$location"
        }
        if (location.startsWith("?")) {
            sb.append(base.path)
            sb.append(location)
            return sb.toString()
        }
        if (location.startsWith("#")) {
            sb.append(base.path)
            if (base.query != null) sb.append('?').append(base.query)
            sb.append(location)
            return sb.toString()
        }

        val targetPath = if (location.startsWith("/")) {
            removeDotSegments(location)
        } else {
            removeDotSegments(mergePaths(base.path, location))
        }
        sb.append(targetPath)
        return sb.toString()
    }

    /** 合并 base 目录与相对引用：`/dir/page` + `x` → `/dir/x`。 */
    private fun mergePaths(basePath: String, ref: String): String {
        if (basePath.isEmpty() || !basePath.contains('/')) return "/$ref"
        val dir = basePath.substringBeforeLast('/', "")
        return if (dir.isEmpty()) "/$ref" else "$dir/$ref"
    }

    /** RFC 3986 5.2.4 remove_dot_segments。 */
    private fun removeDotSegments(input: String): String {
        var path = input
        val output = StringBuilder()
        while (path.isNotEmpty()) {
            when {
                path.startsWith("../") -> path = path.substring(3)
                path.startsWith("./") -> path = path.substring(2)
                path.startsWith("/./") -> path = path.substring(2)
                path == "/." -> path = "/"
                path.startsWith("/../") -> {
                    path = path.substring(3)
                    removeLastSegment(output)
                }
                path == "/.." -> {
                    path = "/"
                    removeLastSegment(output)
                }
                path == "." || path == ".." -> path = ""
                else -> {
                    val idx = if (path.startsWith("/")) path.indexOf('/', 1) else path.indexOf('/')
                    if (idx < 0) {
                        output.append(path)
                        path = ""
                    } else {
                        output.append(path, 0, idx)
                        path = path.substring(idx)
                    }
                }
            }
        }
        return output.toString()
    }

    private fun removeLastSegment(sb: StringBuilder) {
        // RFC 3986 5.2.4：删掉最后一段及段前的最后一个 '/'（output="/a" → ""，而非 "/"）
        val lastSlash = sb.lastIndexOf("/")
        if (lastSlash >= 0) sb.setLength(lastSlash)
    }

    private data class UrlParts(
        val scheme: String,
        val authority: String,
        val path: String,
        val query: String?,
    )

    private fun parseUrl(url: String): UrlParts? {
        val scheme = schemeOf(url) ?: return null
        val afterScheme = url.substring(scheme.length + 3)
        val noFrag = afterScheme.substringBefore('#')
        val queryIdx = noFrag.indexOf('?')
        val authorityAndPath = if (queryIdx >= 0) noFrag.substring(0, queryIdx) else noFrag
        val query = if (queryIdx >= 0) noFrag.substring(queryIdx + 1) else null

        val pathStart = authorityAndPath.indexOf('/')
        val authority = if (pathStart >= 0) authorityAndPath.substring(0, pathStart) else authorityAndPath
        val path = if (pathStart >= 0) authorityAndPath.substring(pathStart) else "/"
        if (authority.isEmpty()) return null
        return UrlParts(scheme, authority, path, query)
    }
}
