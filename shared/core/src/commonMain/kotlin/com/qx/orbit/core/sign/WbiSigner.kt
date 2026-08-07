package com.qx.orbit.core.sign

import com.qx.orbit.core.net.UrlQueryBuilder

/**
 * WBI 签名纯算法，逐字节对齐 app 侧 `data/api/WbiSigner.kt`。
 *
 * 编码语义与 Android 完全一致：
 * `URLEncoder.encode(value)` 后再做 `+`→`%20`、`%21`→`!`、`%27`→`'`、
 * `%28`→`(`、`%29`→`)`、`%7E`→`~`、`%2A`→`*` 的替换，
 * 最终未转义字符集为 `A-Z a-z 0-9 - _ . ~ ! * ' ( )`，其余按 UTF-8 百分号编码（大写十六进制）。
 * 注：Java URLEncoder 本就不转义 `*`，因此 Android 的 `%2A`→`*` 替换实际是空操作，
 * 最终 `*` 保持未转义（与 bilibili 参考实现 `*`→`%2A` 不同，golden 以 Android 输出为准）。
 *
 * 算法流程（与 Android 一致）：
 * 1. 对每个参数分别做 encodeUriComponent 后拼成 `k=v`；
 * 2. 追加 `wts=<epochSecond>`；
 * 3. 对编码后的整条 `k=v` 字符串按字典序排序后 `&` 拼接；
 * 4. `w_rid = md5(sorted + mixinKey)`（mixinKey 直接拼接，不做编码）。
 */
class WbiSigner(private val config: SignerConfig) {

    /**
     * 对参数表签名，返回原参数 + `w_rid` / `wts` 的新表（不修改入参）。
     * 与 Android `signParams` 一致：输入参数即使自带 `wts`/`w_rid` 也会被覆盖。
     */
    fun signParams(params: Map<String, String>): Map<String, String> {
        val mixinKey = config.wbiKeyProvider.mixinKey()
        val ts = config.clock.epochSecond().toString()
        val encodedEntries = params.map { (k, v) ->
            "${encodeUriComponent(k)}=${encodeUriComponent(v)}"
        }
        val sortedParams = (encodedEntries + "wts=$ts").sorted().joinToString("&")
        val wrid = config.md5.hex(sortedParams + mixinKey)
        return params + mapOf("w_rid" to wrid, "wts" to ts)
    }

    /**
     * 对带 query 的 URL 签名：解析 query → 百分号解码 → 重新 encodeUriComponent →
     * 追加 `wts` → 排序 → 计算 `w_rid`，最后追加 `&w_rid=...&wts=...`。
     * 与 Android `signUrl` 一致：分隔符由原 URL 是否含 `?` 决定。
     */
    fun signUrl(url: String): String {
        val mixinKey = config.wbiKeyProvider.mixinKey()
        val ts = config.clock.epochSecond()
        val separator = if (url.contains("?")) "&" else "?"
        val queryPart = url.substringAfter("?", "")
        val entries = if (queryPart.isEmpty()) {
            emptyList()
        } else {
            queryPart.split("&").map { piece ->
                val kv = piece.split("=", limit = 2)
                if (kv.size == 2) {
                    val k = encodeUriComponent(percentDecode(kv[0]))
                    val v = encodeUriComponent(percentDecode(kv[1]))
                    "$k=$v"
                } else {
                    // 没有 `=` 的片段原样保留（与 Android 一致，不再编码）
                    piece
                }
            }
        }
        val sortedParams = (entries + "wts=$ts").sorted().joinToString("&")
        val wrid = config.md5.hex(sortedParams + mixinKey)
        return "$url$separator" + "w_rid=$wrid&wts=$ts"
    }

    private fun encodeUriComponent(value: String): String =
        UrlQueryBuilder.encodeUriComponent(value)
}
