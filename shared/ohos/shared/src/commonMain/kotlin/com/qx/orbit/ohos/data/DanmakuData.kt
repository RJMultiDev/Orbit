package com.qx.orbit.ohos.data

import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * B 站 VOD 弹幕：拉取 + XML 最小解析 + 屏蔽规则。
 *
 * 纯 Kotlin 实现（commonMain，无 android/java 平台 API）。语义对齐：
 * - shared/core danmaku/DanmakuParser：p 属性规约（`t,type,size,color,sentAtSec,pool,userHash,rowId`）、
 *   实体解码（CDATA 原样 + 命名/数字实体）、非法条目跳过不抛异常；
 * - 弹幕模型复用 data/PlayerDataApi.kt 的 [DanmakuItem]（同包唯一定义，避免重名冲突）。
 * 轨道调度（DanmakuLaneScheduler 思路）在 components/DanmakuOverlay.kt 侧实现。
 */

/** B 站弹幕列表接口（type=1 返回 XML；type=0/省略为 protobuf）。 */
private const val DM_LIST_URL = "https://api.bilibili.com/x/v1/dm/list.so"

/**
 * 拉取并解析视频弹幕，返回按 XML 文档顺序排列的列表。
 *
 * - XML：最小解析 `<d p="t,type,size,color,...">text</d>`；
 * - protobuf（二进制）响应暂不支持，返回空列表（TODO：protobuf 解码）；
 * - 网络失败 / 空响应 / 无法解析均返回空列表，不抛异常。
 */
internal suspend fun BiliClient.danmaku(cid: Long): List<DanmakuItem> =
    suspendCancellableCoroutine { cont ->
        val headers = JSONObject()
        headers.put("User-Agent", BiliClient.UA)
        headers.put("Referer", BiliClient.REFERER)
        NetworkModule().httpRequestBinary(
            "$DM_LIST_URL?oid=$cid&type=1",
            false,
            JSONObject(),
            headers,
            null,
            15000,
        ) { bytes, success, _, _ ->
            cont.resume(parseDanmakuBody(if (success) bytes else null))
        }
    }

private fun parseDanmakuBody(bytes: ByteArray?): List<DanmakuItem> {
    if (bytes == null) return emptyList()
    val body = utf8Decode(bytes)
    // XML 以 '<' 开头；其余（protobuf/压缩流）暂不解析。
    if (!body.trimStart().startsWith("<")) return emptyList()
    return parseDanmakuXml(body)
}

/** UTF-8 字节 → 字符串（K/N 无 java String API，手动解码，对齐 Md5/UrlCodec/PlayerDataApi 做法）。 */
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

// ---------------------------------------------------------------------------
// XML 最小解析器（commonMain 无 org.xmlpull，自实现，思路同 core DanmakuParser.parseXml）
// ---------------------------------------------------------------------------

private fun parseDanmakuXml(xml: String): List<DanmakuItem> {
    val result = ArrayList<DanmakuItem>()
    var i = 0
    val n = xml.length
    while (i < n) {
        val open = xml.indexOf("<d", i)
        if (open < 0 || open + 2 >= n) break
        val gt = xml.indexOf('>', open + 2)
        if (gt < 0) break
        val tag = xml.substring(open + 1, gt)
        val selfClosing = tag.trimEnd().endsWith('/')
        val close = if (selfClosing) gt else xml.indexOf("</d>", gt + 1)
        if (close < 0) break
        val text = if (selfClosing) "" else xml.substring(gt + 1, close)
        val p = parseAttr(tag, "p")
        if (p != null) {
            parsePValue(p, decodeEntities(text))?.let { result.add(it) }
        }
        i = if (selfClosing) gt + 1 else close + 4
    }
    return result
}

/**
 * 解析 p 属性。非法条目（字段 < 2、time 非数字或负数、空文本）返回 null。
 * color 为十进制 RGB，按 app 语义补全 alpha（`or 0xFF000000`）。
 */
private fun parsePValue(p: String, text: String): DanmakuItem? {
    if (text.isBlank()) return null
    val parts = p.split(',')
    if (parts.size < 2) return null
    val time = parts[0].trim().toDoubleOrNull() ?: return null
    if (time < 0) return null
    val mode = parts[1].trim().toIntOrNull() ?: 1
    val fontSize = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 25
    val rawColor = parts.getOrNull(3)?.trim()?.toLongOrNull() ?: 0xFFFFFFL
    return DanmakuItem(
        time = time,
        mode = mode,
        fontSize = fontSize,
        color = rawColor or 0xFF000000L,
        text = text,
    )
}

/** 从 `<tag attr="value" ...>` 标签文本中取指定属性值；不存在返回 null。 */
private fun parseAttr(tag: String, target: String): String? {
    var i = 0
    val n = tag.length
    while (i < n) {
        while (i < n && tag[i].isWhitespace()) i++
        val nameStart = i
        while (i < n && tag[i] != '=' && !tag[i].isWhitespace() && tag[i] != '/') i++
        if (i == nameStart) break
        val name = tag.substring(nameStart, i)
        while (i < n && tag[i].isWhitespace()) i++
        if (i < n && tag[i] == '=') {
            i++
            while (i < n && tag[i].isWhitespace()) i++
            if (i < n && (tag[i] == '"' || tag[i] == '\'')) {
                val quote = tag[i]
                val valueStart = ++i
                while (i < n && tag[i] != quote) i++
                val value = tag.substring(valueStart, i)
                if (name == target) return value
                if (i < n) i++
            }
        }
        if (name == target) return null
    }
    return null
}

/**
 * 解码弹幕文本：CDATA 原样保留，其余解码 XML 实体
 * （&amp; &lt; &gt; &quot; &apos; 与 &#123; / &#x1F600; 数字实体）。单遍处理，不做二次解码。
 */
private fun decodeEntities(input: String): String {
    if (input.indexOf('&') < 0 && input.indexOf("<![CDATA[") < 0) return input
    val sb = StringBuilder(input.length)
    var i = 0
    val n = input.length
    while (i < n) {
        if (input.startsWith("<![CDATA[", i)) {
            val end = input.indexOf("]]>", i + 9)
            if (end < 0) {
                sb.append(input, i + 9, n)
                break
            }
            sb.append(input, i + 9, end)
            i = end + 3
        } else if (input[i] == '&') {
            val semi = input.indexOf(';', i + 1)
            val named = if (semi > i) {
                when (val entity = input.substring(i + 1, semi)) {
                    "amp" -> '&'
                    "lt" -> '<'
                    "gt" -> '>'
                    "quot" -> '"'
                    "apos" -> '\''
                    else -> null
                }
            } else null
            if (named != null) {
                sb.append(named)
                i = semi + 1
                continue
            }
            val code = if (semi > i) {
                val entity = input.substring(i + 1, semi)
                when {
                    entity.startsWith("#x") || entity.startsWith("#X") -> entity.substring(2).toIntOrNull(16)
                    entity.startsWith("#") -> entity.substring(1).toIntOrNull()
                    else -> null
                }
            } else null
            if (code != null) {
                appendCodePoint(sb, code)
                i = semi + 1
                continue
            }
            sb.append('&')
            i++
        } else {
            sb.append(input[i])
            i++
        }
    }
    return sb.toString()
}

/** 将 Unicode 码点写入 [sb]；补充平面（> U+FFFF）按 UTF-16 代理对编码。 */
private fun appendCodePoint(sb: StringBuilder, code: Int) {
    if (code in 0x10000..0x10FFFF) {
        val c = code - 0x10000
        sb.append((0xD800 + (c shr 10)).toChar())
        sb.append((0xDC00 + (c and 0x3FF)).toChar())
    } else {
        sb.append(code.toChar())
    }
}

// ---------------------------------------------------------------------------
// 屏蔽规则（纯 Kotlin 移植自 shared/core danmaku/DanmakuFilter.kt）
// ---------------------------------------------------------------------------

/** 弹幕屏蔽配置（默认不屏蔽任何内容）。 */
data class DanmakuFilterConfig(
    /** 关键词屏蔽：文本包含任一关键词即屏蔽。 */
    val blockKeywords: List<String> = emptyList(),
    /** 类型屏蔽：B 站 mode 值集合（1/4/5/6/7/8/9）。 */
    val blockModes: Set<Int> = emptySet(),
    /** 高级弹幕开关：false 时 mode 7/8 降级为滚动（对齐 app `player_danmaku_advanced_enable`）。 */
    val advancedDanmakuEnabled: Boolean = true,
) {
    companion object {
        /** 不屏蔽任何弹幕。 */
        val NONE = DanmakuFilterConfig()
    }
}

/** 弹幕屏蔽引擎：先做类型变换（高级降级），再做关键词/类型屏蔽。 */
object DanmakuFilter {
    fun filter(items: List<DanmakuItem>, config: DanmakuFilterConfig = DanmakuFilterConfig.NONE): List<DanmakuItem> {
        if (config.blockKeywords.isEmpty() && config.blockModes.isEmpty() && config.advancedDanmakuEnabled) {
            return items
        }
        val result = ArrayList<DanmakuItem>(items.size)
        for (item in items) {
            val mode = transformMode(item.mode, config)
            if (blocked(item.text, mode, config)) continue
            result.add(if (mode == item.mode) item else item.copy(mode = mode))
        }
        return result
    }

    /** 高级弹幕开关关闭时把 7/8 降级为滚动（对齐 core DanmakuFilterEngine.transformMode）。 */
    private fun transformMode(mode: Int, config: DanmakuFilterConfig): Int =
        if (!config.advancedDanmakuEnabled && (mode == 7 || mode == 8)) 1 else mode

    private fun blocked(text: String, mode: Int, config: DanmakuFilterConfig): Boolean {
        if (mode in config.blockModes) return true
        for (keyword in config.blockKeywords) {
            if (text.contains(keyword)) return true
        }
        return false
    }
}
