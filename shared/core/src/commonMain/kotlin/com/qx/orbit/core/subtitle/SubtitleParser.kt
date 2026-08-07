package com.qx.orbit.core.subtitle

/** 一条字幕条目（B 站字幕 JSON body 元素），时间单位：秒。 */
data class SubtitleCue(
    val content: String,
    val from: Double,
    val to: Double
)

/**
 * 字幕解析与时间轴定位纯逻辑（commonMain 不依赖 Gson/Android）。
 *
 * - [parseBody]：解析 B 站字幕 JSON（`{"body":[{"from":…,"to":…,"content":…},…]}`），
 *   语义对齐 `PlayerViewModel#parseSubtitleJson`（content/from/to 取 Number/Double）。
 * - [SubtitleTimeline]：时间轴游标定位，语义对齐 `PlayerViewModel#updateCurrentSubtitle`：
 *   顺序播放时游标递增；回退 seek 时二分查找 `to > timeSec` 的首个下标。
 */
object SubtitleParser {

    /**
     * 解析 B 站字幕 JSON。支持根为 `{"body":[…]}`（服务端标准格式）或直接为数组。
     * 解析失败/无 body 时返回空列表。
     */
    fun parseBody(json: String): List<SubtitleCue> {
        if (json.isBlank()) return emptyList()
        val root = try { MiniJson.parse(json) } catch (_: Exception) { return emptyList() }
        val entries: List<*> = when (root) {
            is List<*> -> root
            is Map<*, *> -> root["body"] as? List<*>
            else -> null
        } ?: return emptyList()

        return entries.mapNotNull { entry ->
            val m = entry as? Map<*, *> ?: return@mapNotNull null
            SubtitleCue(
                content = (m["content"] as? String) ?: "",
                from = (m["from"] as? Number)?.toDouble() ?: 0.0,
                to = (m["to"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }

    /**
     * 极小 JSON 解析器，仅覆盖本 schema 需要的值类型。
     * 结果结构：Map<String, Any?> / List<Any?> / String / Double / Boolean / null。
     */
    private object MiniJson {
        fun parse(text: String): Any? {
            val p = Parser(text)
            val v = p.value()
            p.ws()
            return v
        }

        private class Parser(private val s: String) {
            private var i = 0

            fun ws() {
                while (i < s.length && s[i].isWhitespace()) i++
            }

            fun value(): Any? {
                ws()
                return when {
                    i >= s.length -> null
                    s[i] == '{' -> obj()
                    s[i] == '[' -> arr()
                    s[i] == '"' -> str()
                    s[i] == 't' || s[i] == 'f' -> bool()
                    s[i] == 'n' -> { i += 4; null }
                    else -> num()
                }
            }

            private fun obj(): Map<String, Any?> {
                val m = LinkedHashMap<String, Any?>()
                i++ // '{'
                ws()
                if (i < s.length && s[i] == '}') { i++; return m }
                while (true) {
                    ws()
                    val key = str()
                    ws()
                    if (i < s.length && s[i] == ':') i++
                    m[key] = value()
                    ws()
                    when {
                        i < s.length && s[i] == ',' -> { i++; continue }
                        i < s.length && s[i] == '}' -> { i++; break }
                        else -> break
                    }
                }
                return m
            }

            private fun arr(): List<Any?> {
                val l = mutableListOf<Any?>()
                i++ // '['
                ws()
                if (i < s.length && s[i] == ']') { i++; return l }
                while (true) {
                    l.add(value())
                    ws()
                    when {
                        i < s.length && s[i] == ',' -> { i++; continue }
                        i < s.length && s[i] == ']' -> { i++; break }
                        else -> break
                    }
                }
                return l
            }

            private fun str(): String {
                i++ // '"'
                val sb = StringBuilder()
                while (i < s.length) {
                    when (val c = s[i]) {
                        '"' -> { i++; return sb.toString() }
                        '\\' -> {
                            i++
                            if (i >= s.length) break
                            when (val e = s[i]) {
                                '"' -> sb.append('"')
                                '\\' -> sb.append('\\')
                                '/' -> sb.append('/')
                                'b' -> sb.append('\b')
                                'f' -> sb.append('\u000C')
                                'n' -> sb.append('\n')
                                'r' -> sb.append('\r')
                                't' -> sb.append('\t')
                                'u' -> {
                                    if (i + 4 < s.length) {
                                        sb.append(s.substring(i + 1, i + 5).toInt(16).toChar())
                                        i += 4
                                    }
                                }
                                else -> sb.append(e)
                            }
                            i++
                        }
                        else -> { sb.append(c); i++ }
                    }
                }
                return sb.toString()
            }

            private fun bool(): Boolean =
                if (s.startsWith("true", i)) { i += 4; true } else { i += 5; false }

            private fun num(): Double {
                val start = i
                if (i < s.length && (s[i] == '-' || s[i] == '+')) i++
                while (i < s.length &&
                    (s[i].isDigit() || s[i] == '.' || s[i] == 'e' || s[i] == 'E' || s[i] == '-' || s[i] == '+')
                ) i++
                return s.substring(start, i).toDouble()
            }
        }
    }
}

/**
 * 字幕时间轴。构造时按 from 升序排序；保持与 `updateCurrentSubtitle` 相同的游标语义，
 * 支持顺序推进与回退 seek（二分）。
 */
class SubtitleTimeline(cues: List<SubtitleCue>) {

    private val sorted: List<SubtitleCue> = cues.sortedWith(compareBy({ it.from }, { it.to }))

    /** 当前游标（下一个候选字幕的下标）。 */
    private var cursor = 0

    /** 全部 cue，按 from 升序。 */
    val cues: List<SubtitleCue> get() = sorted

    /** 重置游标（对应 `setSubtitles` 时的 cursor=0）。 */
    fun reset() {
        cursor = 0
    }

    /** 给定播放进度（毫秒），返回当前命中的字幕内容；无则 null。 */
    fun textAt(progressMs: Long): String? = cueAt(progressMs)?.content

    /**
     * 给定播放进度（毫秒），返回当前命中的 cue；无则 null。
     * 时间换算：progressMs / 1000.0 为秒；命中条件 `from <= t < to`。
     */
    fun cueAt(progressMs: Long): SubtitleCue? {
        if (sorted.isEmpty()) return null
        val timeSec = progressMs / 1000.0
        if (cursor !in sorted.indices || timeSec < sorted[cursor].from) {
            // 回退或首次定位：二分找首个 to > timeSec 的下标
            var low = 0
            var high = sorted.size
            while (low < high) {
                val mid = (low + high) ushr 1
                if (sorted[mid].to <= timeSec) low = mid + 1 else high = mid
            }
            cursor = low.coerceAtMost(sorted.lastIndex)
        } else {
            // 顺序推进：跨过已结束的 cue
            while (cursor < sorted.lastIndex && timeSec >= sorted[cursor].to) cursor++
        }
        val candidate = sorted[cursor]
        return candidate.takeIf { timeSec >= it.from && timeSec < it.to }
    }
}
