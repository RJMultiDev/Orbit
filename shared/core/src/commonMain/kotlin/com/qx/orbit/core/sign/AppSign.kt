package com.qx.orbit.core.sign

import com.qx.orbit.core.net.UrlQueryBuilder

/**
 * AppSign 纯算法，逐字节对齐 app 侧 `data/sign/AppSignUtil.kt`。
 *
 * 与 WBI 不同：这里的编码是 application/x-www-form-urlencoded（Java URLEncoder）语义，
 * 空格 → `+`，未转义字符集为 `A-Z a-z 0-9 - _ . *`；`~` 编码为 `%7E`，`! ' ( )` 均会编码。
 *
 * 算法流程（与 Android 一致）：
 * 1. 追加 `appkey` 与 `ts=<epochSecond>`；
 * 2. 对**原始 key** 排序；
 * 3. 每对 key/value 用 URLEncoder 语义编码后拼成 `k=v`，`&` 连接；
 * 4. `sign = md5(raw + appSecret)`（appSecret 直接拼接，不做编码）。
 */
class AppSign(
    private val appKey: String,
    private val appSecret: String,
    private val clock: Clock,
    private val md5: Md5,
) {

    /**
     * 对参数表签名，返回原参数 + `appkey` / `ts` / `sign` 的新表（不修改入参）。
     * 入参若自带 `appkey` / `ts` 会被覆盖（对齐 Android 的原地写入）。
     */
    fun sign(params: Map<String, String>): Map<String, String> {
        val withKey = params + mapOf(
            "appkey" to appKey,
            "ts" to clock.epochSecond().toString(),
        )
        val sortedKeys = withKey.keys.sorted()
        val raw = sortedKeys.joinToString("&") { key ->
            encodeForm(key) + "=" + encodeForm(withKey.getValue(key))
        }
        val sign = md5.hex(raw + appSecret)
        return withKey + mapOf("sign" to sign)
    }

    private fun encodeForm(value: String): String =
        UrlQueryBuilder.encodeFormComponent(value)
}
