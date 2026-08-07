package com.qx.orbit.ohos.util

/**
 * WBI / AppSign 签名（对齐 shared/core sign 包；mixinKey 计算含标准 64 位置换表）。
 */
object WbiKeys {
    private val MIXIN_KEY_ENC_TAB = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52,
    )

    fun calcMixinKey(imgUrl: String, subUrl: String): String {
        val imgKey = imgUrl.substringAfterLast('/').substringBefore('.')
        val subKey = subUrl.substringAfterLast('/').substringBefore('.')
        val raw = imgKey + subKey
        val sb = StringBuilder(raw.length)
        for (idx in MIXIN_KEY_ENC_TAB) {
            sb.append(raw[idx])
        }
        return sb.toString()
    }
}

class WbiSigner(private val mixinKey: () -> String, private val nowSeconds: () -> Long = { 0 }) {

    fun signParams(params: Map<String, String>): Map<String, String> {
        val key = mixinKey()
        val ts = nowSeconds().toString()
        val encoded = params.map { (k, v) ->
            "${UrlCodec.encodeUriComponent(k)}=${UrlCodec.encodeUriComponent(v)}"
        }.toMutableList()
        encoded.add("wts=$ts")
        encoded.sort()
        val wrid = Md5.hex(encoded.joinToString("&") + key)
        return params + mapOf("w_rid" to wrid, "wts" to ts)
    }

    fun signUrl(url: String): String {
        val key = mixinKey()
        val ts = nowSeconds().toString()
        val separator = if (url.contains('?')) "&" else "?"
        val queryPart = url.substringAfter('?', "")
        val entries = mutableListOf<String>()
        if (queryPart.isNotEmpty()) {
            for (piece in queryPart.split("&")) {
                val eq = piece.indexOf('=')
                if (eq > 0) {
                    val k = UrlCodec.encodeUriComponent(UrlCodec.percentDecode(piece.substring(0, eq)))
                    val v = UrlCodec.encodeUriComponent(UrlCodec.percentDecode(piece.substring(eq + 1)))
                    entries.add("$k=$v")
                } else {
                    entries.add(piece)
                }
            }
        }
        entries.add("wts=$ts")
        entries.sort()
        val wrid = Md5.hex(entries.joinToString("&") + key)
        return "$url${separator}w_rid=$wrid&wts=$ts"
    }
}

class AppSigner(
    private val appKey: String,
    private val appSecret: String,
    private val nowSeconds: () -> Long = { 0 },
) {
    fun sign(params: Map<String, String>): Map<String, String> {
        val withKey = params + mapOf(
            "appkey" to appKey,
            "ts" to nowSeconds().toString(),
        )
        val raw = withKey.keys.sorted().joinToString("&") { key ->
            "${UrlCodec.encodeFormComponent(key)}=${UrlCodec.encodeFormComponent(withKey.getValue(key))}"
        }
        return withKey + mapOf("sign" to Md5.hex(raw + appSecret))
    }
}
