package com.qx.orbit.ohos.data

import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.util.TimeUtil
import com.qx.orbit.ohos.util.UrlCodec
import com.qx.orbit.ohos.util.WbiKeys
import com.qx.orbit.ohos.util.WbiSigner
import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** 弹幕条目（对齐 Android util/danmaku/base 的 DanmakuItem，取 XML `<d p="..">` 语义）。 */
data class DanmakuItem(
    val time: Double,   // 出现时间（秒）
    val mode: Int,      // 弹幕类型：1滚动 2滚动(旧) 3逆向 4底端 5顶端 6逆向 7高级 8代码
    val fontSize: Int,  // 字号（px；xml 未填时默认 25）
    val color: Long,    // 十进制 RGB（如 16777215 = 白）
    val text: String,   // 弹幕内容（已做 XML 反转义）
)

/**
 * 播放/弹幕数据层（独占 data/PlayerDataApi.kt；对应 Android PlayerApi / HeartbeatApi / DanmakuApi）。
 *
 * - [playUrl]：普通视频走 WBI `/x/player/wbi/playurl`（bvid+cid）；番剧走 `/pgc/player/web/playurl`
 *   （season_type+bvid+cid）。fnval=16 返回 DASH（dash.video/audio），fnval=1 返回 MP4/FLV（durl）。
 * - [danmaku]：`/x/v1/dm/list.so`（XML 或 protobuf），当前实现按 XML 解析（最小解析器提取
 *   `<d p="时间,模式,字号,颜色,池,用户hash,弹幕id">text</d>`）。
 * - [heartbeat]：`/x/click-interface/web/heartbeat` POST，需 cookie 中 bili_jct（CSRF），
 *   `w_*` 参数 WBI 签名。
 *
 * 底层请求差异：弹幕/心跳需要原始字节与真正的 POST 表单体，BiliClient.request 只返回
 * JSONObject 且其 isPost 固定走 GET，故本类自持 NetworkModule 走 httpRequestBinary。
 */
class PlayerDataApi(private val client: BiliClient) {

    private val network = NetworkModule()
    private val credential = OrbitCredentialModule()

    private var mixinKey: String = ""
    private val signer: WbiSigner
        get() = WbiSigner({ mixinKey }, { TimeUtil.nowMillis() / 1000 })
    private val aidCache = mutableMapOf<String, Long>()

    private suspend fun ensureWbiKey() {
        if (mixinKey.isNotEmpty()) return
        val resp = client.request("$HOST/x/web-interface/nav")
        val img = resp?.optJSONObject("data")?.optJSONObject("wbi_img") ?: return
        mixinKey = WbiKeys.calcMixinKey(
            img.optString("img_url"),
            img.optString("sub_url"),
        )
    }

    /** 读取当前 Cookie 串（原生侧按模块名共享同一存储）。 */
    private suspend fun readCookie(): String = suspendCancellableCoroutine { cont ->
        credential.getCookie { cookie -> cont.resume(cookie) }
    }

    // ---- 取流 ----

    /**
     * 取播放地址。qn 映射：16=360P、32=480P、64=720P、80=1080P。
     *
     * @param fnval 16=DASH（响应含 dash.video/audio），1=MP4/FLV（响应含 durl）
     * @param fourk 是否允许 4K（1/0）
     * @param seasonType 非 null 时走 PGC 接口（番剧/影视），组合为 bvid+cid+season_type
     * @return 解析后的 [PlayUrl]；失败或返回空流时为 null
     */
    suspend fun playUrl(
        bvid: String,
        cid: Long,
        qn: Int = 64,
        fnval: Int = 16,
        fourk: Int = 1,
        seasonType: Int? = null,
    ): PlayUrl? {
        val base = linkedMapOf(
            "cid" to cid.toString(),
            "qn" to qn.toString(),
            "fnval" to fnval.toString(),
            "fnver" to "0",
            "fourk" to fourk.toString(),
        )
        if (fnval == 1) base["high_quality"] = "1"

        if (seasonType != null) {
            // PGC 番剧/影视：bvid+cid+season_type，无需 WBI（对齐 Android getBangumi）
            val pgcParams = base + mapOf(
                "bvid" to bvid,
                "season_type" to seasonType.toString(),
            )
            return client.request("$HOST/pgc/player/web/playurl", pgcParams)
                ?.optJSONObject("data")
                ?.let { parsePlayUrl(it) }
        }

        // 普通视频：WBI 签名
        ensureWbiKey()
        val signed = signer.signParams(
            base + mapOf(
                "bvid" to bvid,
                "platform" to (if (fnval == 1) "html5" else "pc"),
            )
        )
        return client.request("$HOST/x/player/wbi/playurl", signed)
            ?.optJSONObject("data")
            ?.let { parsePlayUrl(it) }
    }

    // ---- 弹幕 ----

    /**
     * 弹幕列表。oid 为弹幕池 id（视频场景下 = cid），接口 `/x/v1/dm/list.so`。
     * 若实测返回 protobuf 二进制，当前返回空列表（protobuf 解析待按实测定补充）。
     */
    suspend fun danmaku(cid: Long, oid: Long = cid): List<DanmakuItem> {
        val url = "$HOST/x/v1/dm/list.so?oid=$oid&type=1"
        val bytes = rawRequest(url, isPost = false, body = byteArrayOf()) ?: return emptyList()
        return parseDanmakuResponse(bytes)
    }

    // ---- 心跳 ----

    /**
     * 播放心跳上报（对齐 Android HeartbeatApi）。未登录（无 bili_jct）时静默跳过。
     *
     * @param playedTime 已播放时长（秒）
     * @param realTime 上报时刻的真实时间戳（秒）；缺省回退为 playedTime（对齐 Android 默认）
     * @param aid 可显式传入；缺省时内部经 videoDetail 解析并缓存（避免每次心跳都查详情）
     */
    suspend fun heartbeat(
        bvid: String,
        cid: Long,
        playedTime: Long,
        realTime: Long = playedTime,
        aid: Long? = null,
    ) {
        val cookie = readCookie()
        val csrf = cookieValue(cookie, "bili_jct")
        if (csrf.isEmpty()) return
        val mid = cookieValue(cookie, "DedeUserID").toLongOrNull() ?: 0L
        val resolvedAid = aid ?: resolveAid(bvid) ?: return
        val startTs = TimeUtil.nowMillis() / 1000

        ensureWbiKey()
        val signed = signer.signParams(
            linkedMapOf(
                "w_start_ts" to startTs.toString(),
                "w_mid" to mid.toString(),
                "w_aid" to resolvedAid.toString(),
                "w_dt" to "2",
                "w_realtime" to realTime.coerceAtLeast(0).toString(),
                "w_played_time" to playedTime.toString(),
                "w_real_played_time" to playedTime.toString(),
                "w_video_duration" to "0",
                "w_last_play_progress_time" to playedTime.toString(),
                "web_location" to "1315873",
            )
        )
        val query = signed.entries.sortedBy { it.key }.joinToString("&") { (k, v) ->
            "${UrlCodec.encodeUriComponent(k)}=${UrlCodec.encodeUriComponent(v)}"
        }
        val url = "$HOST/x/click-interface/web/heartbeat?$query"
        val form = linkedMapOf(
            "aid" to resolvedAid.toString(),
            "bvid" to bvid,
            "cid" to cid.toString(),
            "mid" to mid.toString(),
            "csrf" to csrf,
            "played_time" to playedTime.toString(),
            "realtime" to realTime.toString(),
            "start_ts" to startTs.toString(),
            "type" to "3",
            "dt" to "2",
            "video_duration" to "0",
            "last_play_progress_time" to playedTime.toString(),
            "play_type" to "0",
        )
        rawRequest(url, isPost = true, body = formEncode(form))
    }

    private suspend fun resolveAid(bvid: String): Long? {
        aidCache[bvid]?.let { return it }
        val aid = client.videoDetail(bvid)?.aid ?: return null
        aidCache[bvid] = aid
        return aid
    }

    // ---- 底层原始请求（二进制） ----

    /**
     * 原始 HTTP 请求（GET/POST），返回响应字节。
     * 弹幕 XML、心跳表单都需要原始响应/真 POST，BiliClient.request 的 JSONObject 通道无法承载。
     */
    private suspend fun rawRequest(url: String, isPost: Boolean, body: ByteArray): ByteArray? =
        suspendCancellableCoroutine { cont ->
            val headers = JSONObject()
            headers.put("User-Agent", BiliClient.UA)
            headers.put("Referer", BiliClient.REFERER)
            if (isPost) headers.put("Content-Type", "application/x-www-form-urlencoded")
            credential.getCookie { cookie ->
                if (cookie.isNotEmpty()) headers.put("Cookie", cookie)
                network.httpRequestBinary(
                    url,
                    isPost,
                    body,
                    JSONObject(),
                    headers,
                    null,
                    15000,
                ) { data, success, _, _ ->
                    if (success && data.isNotEmpty()) cont.resume(data) else cont.resume(null)
                }
            }
        }

    private fun formEncode(form: Map<String, String>): ByteArray =
        utf8Encode(form.entries.joinToString("&") { (k, v) ->
            "${UrlCodec.encodeFormComponent(k)}=${UrlCodec.encodeFormComponent(v)}"
        })

    private companion object {
        const val HOST = "https://api.bilibili.com"
    }
}

// ---- 弹幕解析（最小 XML；p 属性按逗号分割对齐 DanmakuParser/DFM 语义） ----

private fun parseDanmakuResponse(bytes: ByteArray): List<DanmakuItem> {
    if (bytes.isEmpty()) return emptyList()
    // XML 声明/根节点以 '<' 开头；否则视为 protobuf 或未知格式，暂不解析
    if (bytes[0] == '<'.code.toByte()) {
        return parseDanmakuXml(utf8Decode(bytes))
    }
    return emptyList()
}

private fun parseDanmakuXml(xml: String): List<DanmakuItem> {
    val items = mutableListOf<DanmakuItem>()
    var idx = 0
    while (idx < xml.length) {
        val open = xml.indexOf("<d", idx)
        if (open < 0) break
        val pAttr = xml.indexOf("p=\"", open)
        if (pAttr < 0) break
        val pStart = pAttr + 3
        val pEnd = xml.indexOf('"', pStart)
        if (pEnd < 0) break
        val p = xml.substring(pStart, pEnd)
        val textStart = xml.indexOf('>', pEnd)
        if (textStart < 0) break
        val contentStart = textStart + 1
        val close = xml.indexOf("</d>", contentStart)
        if (close < 0) break
        parseDanmakuP(p, xml.substring(contentStart, close))?.let { items.add(it) }
        idx = close + 4
    }
    return items
}

/** p 属性字段：[0]时间(秒) [1]模式 [2]字号 [3]颜色(十进制) [4]弹幕池 [5]用户hash [6]弹幕id */
private fun parseDanmakuP(p: String, rawText: String): DanmakuItem? {
    val parts = p.split(",")
    if (parts.size < 4) return null
    val time = parts[0].toDoubleOrNull() ?: return null
    val mode = parts.getOrNull(1)?.toIntOrNull() ?: 1
    val fontSize = parts.getOrNull(2)?.toIntOrNull() ?: 25
    val color = parts.getOrNull(3)?.toLongOrNull() ?: 0L
    return DanmakuItem(
        time = time,
        mode = mode,
        fontSize = fontSize,
        color = color,
        text = xmlUnescape(rawText),
    )
}

private fun xmlUnescape(s: String): String {
    var out = s
    out = out.replace("&lt;", "<")
    out = out.replace("&gt;", ">")
    out = out.replace("&quot;", "\"")
    out = out.replace("&apos;", "'")
    out = out.replace("&amp;", "&")
    return out
}

// ---- Cookie / 编解码工具 ----

private fun cookieValue(cookie: String, name: String): String {
    for (part in cookie.split(";")) {
        val eq = part.indexOf('=')
        if (eq > 0 && part.substring(0, eq).trim() == name) {
            return part.substring(eq + 1).trim()
        }
    }
    return ""
}

/** 字符串 → UTF-8 字节（K/N 无 java String API，手动编码，对齐 Md5/UrlCodec 做法）。 */
private fun utf8Encode(s: String): ByteArray {
    val out = ArrayList<Byte>(s.length)
    var i = 0
    while (i < s.length) {
        val c = s[i].code
        var cp = c
        if (c >= 0xd800 && c <= 0xdbff && i + 1 < s.length) {
            val low = s[i + 1].code
            if (low >= 0xdc00 && low <= 0xdfff) {
                cp = 0x10000 + ((c - 0xd800) shl 10) + (low - 0xdc00)
                i++
            }
        }
        when {
            cp < 0x80 -> out.add(cp.toByte())
            cp < 0x800 -> {
                out.add((0xc0 or (cp shr 6)).toByte())
                out.add((0x80 or (cp and 0x3f)).toByte())
            }
            cp < 0x10000 -> {
                out.add((0xe0 or (cp shr 12)).toByte())
                out.add((0x80 or ((cp shr 6) and 0x3f)).toByte())
                out.add((0x80 or (cp and 0x3f)).toByte())
            }
            else -> {
                out.add((0xf0 or (cp shr 18)).toByte())
                out.add((0x80 or ((cp shr 12) and 0x3f)).toByte())
                out.add((0x80 or ((cp shr 6) and 0x3f)).toByte())
                out.add((0x80 or (cp and 0x3f)).toByte())
            }
        }
        i++
    }
    return out.toByteArray()
}

/** UTF-8 字节 → 字符串（手动解码，对齐 UrlCodec.bytesToUtf8 做法）。 */
private fun utf8Decode(bytes: ByteArray): String {
    val sb = StringBuilder(bytes.size)
    var i = 0
    while (i < bytes.size) {
        val b = bytes[i].toInt() and 0xff
        when {
            b < 0x80 -> {
                sb.append(b.toChar())
                i++
            }
            b and 0xe0 == 0xc0 && i + 1 < bytes.size -> {
                sb.append(((b and 0x1f) shl 6 or (bytes[i + 1].toInt() and 0x3f)).toChar())
                i += 2
            }
            b and 0xf0 == 0xe0 && i + 2 < bytes.size -> {
                sb.append(
                    ((b and 0x0f) shl 12 or
                        ((bytes[i + 1].toInt() and 0x3f) shl 6) or
                        (bytes[i + 2].toInt() and 0x3f)).toChar()
                )
                i += 3
            }
            b and 0xf8 == 0xf0 && i + 3 < bytes.size -> {
                val cp = (b and 0x07) shl 18 or
                    ((bytes[i + 1].toInt() and 0x3f) shl 12) or
                    ((bytes[i + 2].toInt() and 0x3f) shl 6) or
                    (bytes[i + 3].toInt() and 0x3f)
                if (cp < 0x10000) {
                    sb.append(cp.toChar())
                } else {
                    val v = cp - 0x10000
                    sb.append((0xd800 + (v ushr 10)).toChar())
                    sb.append((0xdc00 + (v and 0x3ff)).toChar())
                }
                i += 4
            }
            else -> {
                sb.append(b.toChar())
                i++
            }
        }
    }
    return sb.toString()
}
