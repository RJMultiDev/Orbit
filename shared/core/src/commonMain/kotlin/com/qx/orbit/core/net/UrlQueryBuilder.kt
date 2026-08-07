package com.qx.orbit.core.net

/**
 * query/form 参数编码与拼接（纯算法，无平台依赖）。
 *
 * [encodeUriComponent] 语义对齐 app 侧 WbiSigner.encodeUriComponent：
 * Java URLEncoder 输出再经 `+`→`%20`、`%21`→`!`、`%27`→`'`、`%28`→`(`、
 * `%29`→`)`、`%7E`→`~`、`%2A`→`*` 替换，最终未转义字符集为
 * `A-Z a-z 0-9 - _ . ~ ! * ' ( )`；其余字节按 UTF-8 百分号编码（大写十六进制）。
 */
object UrlQueryBuilder {

    private const val HEX = "0123456789ABCDEF"

    /** Wbi 语义的 URI 组件编码：空格编码为 `%20`。 */
    fun encodeUriComponent(value: String): String {
        if (value.isEmpty()) return ""
        val bytes = value.encodeToByteArray()
        val out = StringBuilder(bytes.size * 3)
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (isUriUnreserved(c)) {
                out.append(c.toChar())
            } else {
                out.append('%')
                out.append(HEX[c ushr 4])
                out.append(HEX[c and 0x0F])
            }
        }
        return out.toString()
    }

    private fun isUriUnreserved(c: Int): Boolean =
        (c in 'a'.code..'z'.code) || (c in 'A'.code..'Z'.code) || (c in '0'.code..'9'.code) ||
            c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code ||
            c == '!'.code || c == '\''.code || c == '('.code || c == ')'.code || c == '*'.code

    /** application/x-www-form-urlencoded 组件编码：空格编码为 `+`（对齐 Java URLEncoder 的 form 语义）。 */
    fun encodeFormComponent(value: String): String {
        if (value.isEmpty()) return ""
        val bytes = value.encodeToByteArray()
        val out = StringBuilder(bytes.size * 3)
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (c == ' '.code) {
                out.append('+')
            } else if (isFormUnreserved(c)) {
                out.append(c.toChar())
            } else {
                out.append('%')
                out.append(HEX[c ushr 4])
                out.append(HEX[c and 0x0F])
            }
        }
        return out.toString()
    }

    /** Java URLEncoder 的未转义字符集（不含 `~`，form 语义）。 */
    private fun isFormUnreserved(c: Int): Boolean =
        (c in 'a'.code..'z'.code) || (c in 'A'.code..'Z'.code) || (c in '0'.code..'9'.code) ||
            c == '-'.code || c == '_'.code || c == '.'.code || c == '*'.code

    /**
     * 由参数 map 构建 query string。每个 `k=v` 都经 [encodeUriComponent] 编码；
     * [sort] 为 true 时对**编码后**的整条 `k=v` 排序（对齐 WbiSigner 的 wts/w_rid 排序语义）。
     */
    fun build(params: Map<String, String>, sort: Boolean = false): String {
        val entries = params.map { (k, v) -> "${encodeUriComponent(k)}=${encodeUriComponent(v)}" }
        val ordered = if (sort) entries.sorted() else entries
        return ordered.joinToString("&")
    }

    /** 将 query 追加到 URL：已有 `?` 用 `&` 连接，否则用 `?`。 */
    fun appendQuery(url: String, query: String): String {
        if (query.isEmpty()) return url
        return if (url.contains("?")) "$url&$query" else "$url?$query"
    }
}
