package com.qx.orbit.core.danmaku

/**
 * B 站弹幕协议解析纯函数。
 *
 * 纯 Kotlin 实现，commonMain 不依赖 android、java 平台 API（无 org.xmlpull / org.json），
 * XML 与 JSON 均使用内置的最小解析器，输出 [DanmakuEvent]。
 *
 * ## 输入规约
 *
 * ### XML（B 站弹幕文件）
 * 结构为 `<i>` 根节点下平铺的 `<d p="...">text</d>`，`p` 属性为逗号分隔：
 * `time,mode,fontSize,color,sentAtSec,pool,userHash,rowId[,...]`
 * - time：出现时间，秒（浮点）→ 毫秒
 * - mode：见 [DanmakuMode.fromRaw]
 * - color：十进制 RGB，按现有 app 语义补全 alpha（`or 0xFF000000`）
 * - 后续字段忽略
 *
 * ### JSON
 * 顶层为数组，支持两种条目：
 * - 对象：`{"p": "<同上 p 属性>", "m": "text"}`（兼容 DFM JsonDanmakuParser）
 * - 数组：`[time,mode,fontSize,color,sentAtSec,pool,userHash,rowId,text]`
 */
object DanmakuParser {

    /**
     * 解析 B 站弹幕 XML 文本。
     * 无法解析、空文本、非法时间（负数/非数字）的 `<d>` 条目会被跳过；返回空列表不抛异常。
     */
    fun parseXml(xml: String): List<DanmakuEvent> {
        val events = ArrayList<DanmakuEvent>()
        val n = xml.length
        var i = 0
        var seq = 0
        while (i < n) {
            val lt = xml.indexOf('<', i)
            if (lt < 0 || lt + 1 >= n) break
            when (xml[lt + 1]) {
                '!', '?' -> {
                    // 注释 / CDATA / DOCTYPE / 处理指令，直接跳过到 '>'
                    val gt = xml.indexOf('>', lt + 2)
                    if (gt < 0) break
                    i = gt + 1
                }
                '/' -> {
                    // 闭合标签
                    val gt = xml.indexOf('>', lt + 2)
                    if (gt < 0) break
                    i = gt + 1
                }
                else -> {
                    val gt = xml.indexOf('>', lt + 1)
                    if (gt < 0) break
                    val openTag = xml.substring(lt + 1, gt)
                    val name = openTag.takeWhile { it != ' ' && it != '\t' && it != '\n' && it != '\r' && it != '/' }
                    if (name == "d") {
                        val selfClosing = openTag.trimEnd().endsWith('/')
                        val contentEnd = if (selfClosing) gt else xml.indexOf("</d>", gt + 1)
                        if (contentEnd < 0) break
                        val text = if (selfClosing) "" else xml.substring(gt + 1, contentEnd)
                        val pValue = parseAttributes(openTag)["p"]
                        if (pValue != null) {
                            parsePAttribute(pValue, decodeContent(text), seq)?.let { events.add(it) }
                        }
                        i = if (selfClosing) gt + 1 else contentEnd + 4
                        seq++
                    } else {
                        i = gt + 1
                    }
                }
            }
        }
        return events
    }

    /**
     * 解析 B 站弹幕 JSON 文本（规约见 [DanmakuParser] 文档）。
     * 非法 JSON 或空输入返回空列表，不抛异常。
     */
    fun parseJson(json: String): List<DanmakuEvent> {
        val root = MinimalJsonParser(json).parse() ?: return emptyList()
        val items = (root as? JsonValue.Arr)?.items ?: return emptyList()
        val events = ArrayList<DanmakuEvent>(items.size)
        for ((index, item) in items.withIndex()) {
            when (item) {
                is JsonValue.Obj -> {
                    val p = (item.entries["p"] as? JsonValue.Str)?.value ?: continue
                    val text = (item.entries["m"] as? JsonValue.Str)?.value
                        ?: (item.entries["text"] as? JsonValue.Str)?.value
                        ?: ""
                    parsePAttribute(p, text, index)?.let { events.add(it) }
                }
                is JsonValue.Arr -> {
                    // [time,mode,fontSize,color,sentAtSec,pool,userHash,rowId,text]
                    val timeSec = item.items.getOrNull(0)?.asFloatOrNull() ?: continue
                    val timeMs = (timeSec * 1000f).toLong()
                    if (timeMs < 0) continue
                    val rawMode = item.items.getOrNull(1)?.asLongOrNull()?.toInt() ?: 1
                    val fontSize = item.items.getOrNull(2)?.asFloatOrNull() ?: 25f
                    val rawColor = item.items.getOrNull(3)?.asLongOrNull() ?: 0xFFFFFFL
                    val text = item.items.getOrNull(8)?.asStringOrNull().orEmpty()
                    if (text.isBlank()) continue
                    events.add(
                        DanmakuEvent(
                            timeMs = timeMs,
                            mode = DanmakuMode.fromRaw(rawMode),
                            text = text,
                            color = rawColor or 0xFF000000L,
                            fontSize = fontSize,
                            sentAtSec = item.items.getOrNull(4)?.asLongOrNull() ?: 0L,
                            pool = item.items.getOrNull(5)?.asLongOrNull()?.toInt() ?: 0,
                            userHash = item.items.getOrNull(6)?.asStringOrNull().orEmpty(),
                            rowId = item.items.getOrNull(7)?.asLongOrNull() ?: 0L,
                            sourceIndex = index,
                        )
                    )
                }
                else -> Unit
            }
        }
        return events
    }

    /**
     * 解析 `p` 属性字符串（XML 与 JSON 对象形式的公共规约）。
     * 非法输入（字段数 < 2、time 非数字或负数）返回 null。
     */
    fun parsePAttribute(p: String, text: String, sourceIndex: Int = 0): DanmakuEvent? {
        val parts = p.split(',')
        if (parts.size < 2) return null
        val timeSec = parts[0].trim().toFloatOrNull() ?: return null
        val timeMs = (timeSec * 1000f).toLong()
        if (timeMs < 0) return null
        if (text.isBlank()) return null
        val rawMode = parts[1].trim().toIntOrNull() ?: 1
        val fontSize = parts.getOrNull(2)?.trim()?.toFloatOrNull() ?: 25f
        val rawColor = parts.getOrNull(3)?.trim()?.toLongOrNull() ?: 0xFFFFFFL
        return DanmakuEvent(
            timeMs = timeMs,
            mode = DanmakuMode.fromRaw(rawMode),
            text = text,
            color = rawColor or 0xFF000000L,
            fontSize = fontSize,
            sentAtSec = parts.getOrNull(4)?.trim()?.toLongOrNull() ?: 0L,
            pool = parts.getOrNull(5)?.trim()?.toIntOrNull() ?: 0,
            userHash = parts.getOrNull(6)?.trim().orEmpty(),
            rowId = parts.getOrNull(7)?.trim()?.toLongOrNull() ?: 0L,
            sourceIndex = sourceIndex,
        )
    }

    /** 解析 `<tag attr="value" ...>` 标签内的属性。 */
    private fun parseAttributes(tag: String): Map<String, String> {
        val result = HashMap<String, String>()
        var i = 0
        val n = tag.length
        while (i < n) {
            while (i < n && tag[i].isWhitespace()) i++
            if (i >= n || tag[i] == '/') break
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
                    result[name] = tag.substring(valueStart, i)
                    if (i < n) i++
                } else {
                    val valueStart = i
                    while (i < n && !tag[i].isWhitespace() && tag[i] != '/') i++
                    result[name] = tag.substring(valueStart, i)
                }
            } else if (name.isNotEmpty()) {
                result[name] = ""
            }
        }
        return result
    }

    /**
     * 解码弹幕文本内容：CDATA 原样保留（不解码实体），CDATA 之外解码 XML 实体
     * （&amp; &lt; &gt; &quot; &apos; 与 &#123; / &#x1F600; 数字实体）。单遍处理，不做二次解码。
     */
    private fun decodeContent(input: String): String {
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
                val decodedChar = if (semi > i) {
                    val entity = input.substring(i + 1, semi)
                    when (entity) {
                        "amp" -> '&'
                        "lt" -> '<'
                        "gt" -> '>'
                        "quot" -> '"'
                        "apos" -> '\''
                        else -> null
                    }
                } else null
                if (decodedChar != null) {
                    sb.append(decodedChar)
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
}

// ---------------------------------------------------------------------------
// 最小 JSON 解析器（commonMain 无 org.json，自实现确定性 JSON 解析）
// ---------------------------------------------------------------------------

internal sealed class JsonValue {
    object Null : JsonValue()
    data class Bool(val value: Boolean) : JsonValue()
    data class Number(val value: String) : JsonValue()
    data class Str(val value: String) : JsonValue()
    data class Arr(val items: List<JsonValue>) : JsonValue()
    data class Obj(val entries: Map<String, JsonValue>) : JsonValue()
}

internal fun JsonValue.asFloatOrNull(): Float? = when (this) {
    is JsonValue.Number -> value.toFloatOrNull()
    is JsonValue.Str -> value.toFloatOrNull()
    else -> null
}

internal fun JsonValue.asLongOrNull(): Long? = when (this) {
    is JsonValue.Number -> value.toLongOrNull()
    is JsonValue.Str -> value.toLongOrNull()
    is JsonValue.Bool -> if (value) 1L else 0L
    else -> null
}

internal fun JsonValue.asStringOrNull(): String? = when (this) {
    is JsonValue.Str -> value
    is JsonValue.Number -> value
    is JsonValue.Bool -> value.toString()
    else -> null
}

/**
 * 递归下降最小 JSON 解析器。
 * 顶层要求消费完整输入，否则视为非法返回 null；对象/数组内部对结构错误宽松处理。
 */
internal class MinimalJsonParser(private val s: String) {

    private var pos = 0

    fun parse(): JsonValue? {
        skipWhitespace()
        val value = parseValue() ?: return null
        skipWhitespace()
        return if (pos == s.length) value else null
    }

    private fun parseValue(): JsonValue? {
        if (pos >= s.length) return null
        return when (s[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.Str(parseString())
            't' -> { if (expect("true")) JsonValue.Bool(true) else null }
            'f' -> { if (expect("false")) JsonValue.Bool(false) else null }
            'n' -> { if (expect("null")) JsonValue.Null else null }
            else -> parseNumber()
        }
    }

    private fun parseObject(): JsonValue {
        pos++ // '{'
        val entries = LinkedHashMap<String, JsonValue>()
        skipWhitespace()
        if (pos < s.length && s[pos] == '}') {
            pos++
            return JsonValue.Obj(entries)
        }
        while (pos < s.length) {
            skipWhitespace()
            if (pos >= s.length || s[pos] != '"') break
            val key = parseString()
            skipWhitespace()
            if (pos < s.length && s[pos] == ':') pos++
            skipWhitespace()
            entries[key] = parseValue() ?: JsonValue.Null
            skipWhitespace()
            when {
                pos < s.length && s[pos] == ',' -> pos++
                pos < s.length && s[pos] == '}' -> { pos++; return JsonValue.Obj(entries) }
                else -> break
            }
        }
        return JsonValue.Obj(entries)
    }

    private fun parseArray(): JsonValue {
        pos++ // '['
        val items = ArrayList<JsonValue>()
        skipWhitespace()
        if (pos < s.length && s[pos] == ']') {
            pos++
            return JsonValue.Arr(items)
        }
        while (pos < s.length) {
            skipWhitespace()
            val value = parseValue() ?: break
            items.add(value)
            skipWhitespace()
            when {
                pos < s.length && s[pos] == ',' -> pos++
                pos < s.length && s[pos] == ']' -> { pos++; return JsonValue.Arr(items) }
                else -> break
            }
        }
        return JsonValue.Arr(items)
    }

    private fun parseString(): String {
        pos++ // 起始引号
        val sb = StringBuilder()
        while (pos < s.length) {
            val c = s[pos]
            when {
                c == '"' -> { pos++; return sb.toString() }
                c == '\\' -> {
                    pos++
                    if (pos >= s.length) break
                    when (val e = s[pos]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (pos + 4 < s.length) {
                                val hex = s.substring(pos + 1, pos + 5)
                                val code = hex.toIntOrNull(16)
                                if (code != null) {
                                    sb.append(code.toChar())
                                    pos += 4
                                } else {
                                    sb.append(e)
                                }
                            } else {
                                sb.append(e)
                            }
                        }
                        else -> sb.append(e)
                    }
                    pos++
                }
                else -> { sb.append(c); pos++ }
            }
        }
        return sb.toString()
    }

    private fun parseNumber(): JsonValue {
        val start = pos
        if (pos < s.length && (s[pos] == '-' || s[pos] == '+')) pos++
        while (pos < s.length) {
            val c = s[pos]
            if (!c.isDigit() && c != '.' && c != 'e' && c != 'E' && c != '-' && c != '+') break
            pos++
        }
        if (pos == start) {
            pos = s.length // 非法数字，强制使顶层 parse 失败
            return JsonValue.Number("0")
        }
        return JsonValue.Number(s.substring(start, pos))
    }

    private fun expect(literal: String): Boolean {
        if (s.startsWith(literal, pos)) {
            pos += literal.length
            return true
        }
        pos = s.length // 不匹配，强制使顶层 parse 失败
        return false
    }

    private fun skipWhitespace() {
        while (pos < s.length && s[pos].isWhitespace()) pos++
    }
}
