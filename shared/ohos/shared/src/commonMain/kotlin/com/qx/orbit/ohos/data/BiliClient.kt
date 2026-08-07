package com.qx.orbit.ohos.data

import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.util.UrlCodec
import com.qx.orbit.ohos.util.WbiKeys
import com.qx.orbit.ohos.util.WbiSigner
import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * B 站业务客户端：WBI/AppSign 签名在 Kotlin 侧完成，HTTP 经 Kuikly 内置
 * NetworkModule 桥到 ArkTS（@ohos.net.http 真实网络），Cookie 经凭证模块存取。
 */
class BiliClient(private val credential: OrbitCredentialModule) {

    private val network = NetworkModule()
    private var mixinKey: String = ""

    companion object {
        const val UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36"
        const val REFERER = "https://www.bilibili.com/"
        const val APP_KEY = "dfca71928277209b"
        const val APP_SECRET = "b5475a8825547a4fc26c7d518eaaa02e"
        private const val HOST = "https://api.bilibili.com"
    }

    private val wbiSigner: WbiSigner get() = WbiSigner({ mixinKey }, { 0 })

    // ---- 底层请求（suspend 包装 NetworkModule） ----

    internal suspend fun request(
        url: String,
        params: Map<String, String> = emptyMap(),
        isPost: Boolean = false,
    ): JSONObject? = suspendCancellableCoroutine { cont ->
        val headers = JSONObject()
        headers.put("User-Agent", UA)
        headers.put("Referer", REFERER)
        val cookieParam = JSONObject()
        credential.getCookie { cookie ->
            if (cookie.isNotEmpty()) {
                cookieParam.put("Cookie", cookie)
            }
            val body = JSONObject()
            params.forEach { (k, v) -> body.put(k, v) }
            val fullUrl = if (params.isEmpty()) url else {
                val q = params.entries.sortedBy { it.key }
                    .joinToString("&") { (k, v) -> "${UrlCodec.encodeUriComponent(k)}=${UrlCodec.encodeUriComponent(v)}" }
                if (url.contains('?')) "$url&$q" else "$url?$q"
            }
            network.httpRequest(
                fullUrl,
                false,
                if (isPost) body else JSONObject(),
                headers,
                null,
                15000,
            ) { data, isSuccess, errorMsg, resp ->
                if (isSuccess && data != null) {
                    // 回存 Set-Cookie
                    val headerFields = resp.headerFields
                    if (headerFields != null) {
                        val setCookies = headerFields.optString("set-cookie")
                        if (setCookies.isNotEmpty()) {
                            credential.setCookie(mergeCookies(cookie, setCookies))
                        }
                    }
                    cont.resume(data)
                } else {
                    cont.resume(null)
                }
            }
        }
    }

    private fun mergeCookies(existing: String, setCookieHeader: String): String {
        val map = linkedMapOf<String, String>()
        if (existing.isNotEmpty()) {
            for (part in existing.split(";")) {
                val eq = part.indexOf('=')
                if (eq > 0) map[part.substring(0, eq).trim()] = part.substring(eq + 1).trim()
            }
        }
        for (part in setCookieHeader.split(",")) {
            val eq = part.indexOf('=')
            if (eq <= 0) continue
            val name = part.substring(0, eq).trim()
            val value = part.substring(eq + 1).substringBefore(';').trim()
            map[name] = value
        }
        return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    // ---- 认证 ----

    suspend fun ensureWbiKey() {
        if (mixinKey.isNotEmpty()) return
        val resp = request("$HOST/x/web-interface/nav")
        val img = resp?.optJSONObject("data")?.optJSONObject("wbi_img") ?: return
        mixinKey = WbiKeys.calcMixinKey(
            img.optString("img_url"),
            img.optString("sub_url"),
        )
    }

    suspend fun nav(): NavUser? {
        val resp = request("$HOST/x/web-interface/nav")
        return resp?.optJSONObject("data")?.let { parseNavUser(it) }
    }

    suspend fun createQr(): QrLogin? {
        val resp = request("https://passport.bilibili.com/x/passport-login/web/qrcode/generate")
        return resp?.optJSONObject("data")?.let { parseQrLogin(it) }
    }

    /** 轮询扫码：返回 poll code（0=成功 86038=过期 86090=已扫 86101=未扫）。 */
    suspend fun pollQr(qrcodeKey: String): Int {
        val resp = request(
            "https://passport.bilibili.com/x/passport-login/web/qrcode/poll",
            mapOf("qrcode_key" to qrcodeKey),
        )
        return resp?.optInt("data", -1) ?: -1
    }

    // ---- 内容 ----

    suspend fun recommend(): List<RecommendCard> {
        ensureWbiKey()
        val signed = wbiSigner.signParams(mapOf("ps" to "20", "fresh_idx" to "0"))
        val resp = request("$HOST/x/web-interface/wbi/index/top/feed/rcmd", signed)
        val arr = resp?.optJSONObject("data")?.optJSONArray("item") ?: return emptyList()
        val list = mutableListOf<RecommendCard>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { list.add(parseRecommendCard(it)) }
        }
        return list
    }

    suspend fun videoDetail(bvid: String): VideoDetail? {
        val resp = request("$HOST/x/web-interface/view", mapOf("bvid" to bvid))
        return resp?.optJSONObject("data")?.let { parseVideoDetail(it) }
    }

    suspend fun playUrl(bvid: String, cid: Long): PlayUrl? {
        ensureWbiKey()
        val signed = wbiSigner.signParams(
            mapOf(
                "bvid" to bvid,
                "cid" to cid.toString(),
                "qn" to "64",
                "fnval" to "16",
                "fourk" to "1",
            )
        )
        val resp = request("$HOST/x/player/wbi/playurl", signed)
        return resp?.optJSONObject("data")?.let { parsePlayUrl(it) }
    }

    suspend fun logout() {
        credential.clear()
    }
}
