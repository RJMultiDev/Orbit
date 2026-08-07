package com.qx.orbit.ohos.components

import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.text.InlineTextContent
import com.tencent.kuikly.compose.foundation.text.appendInlineContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Shadow
import com.tencent.kuikly.compose.ui.text.AnnotatedString
import com.tencent.kuikly.compose.ui.text.Placeholder
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.text.style.TextDecoration
import com.tencent.kuikly.compose.ui.text.withStyle
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.module.CalendarModule
import com.tencent.kuikly.core.module.ICalendar

/**
 * 表情模型（对应 Android data/model/Emote.kt 的 Emote，去掉了序列化/Parcealable 相关）。
 */
data class Emote(
    val name: String = "",
    val url: String = "",
    val size: Int = 0,
)

// ---------------------------------------------------------------------------
// HighlightUtil / parseRichText
// ---------------------------------------------------------------------------

/**
 * 解析 `<em class="keyword">...</em>` 高亮标签，返回关键词用主色着色的 AnnotatedString。
 *
 * 对应 Android presentation/util/HighlightUtil.kt 的 [parseHighlightedTitle]。
 * 默认高亮色用 OrbitColors.Primary（Android 默认 MaterialTheme.colorScheme.primary）。
 */
fun parseHighlightedTitle(raw: String, highlightColor: Color = OrbitColors.Primary): AnnotatedString {
    return buildAnnotatedString {
        var remaining = raw
        val emOpenRegex = Regex("""<em[^>]*>""")
        val emClose = "</em>"

        while (remaining.isNotEmpty()) {
            val matchResult = emOpenRegex.find(remaining)
            if (matchResult == null) {
                append(remaining)
                break
            }

            val beforeEm = remaining.substring(0, matchResult.range.first)
            append(beforeEm)

            val afterOpenTag = remaining.substring(matchResult.range.last + 1)
            val closeIdx = afterOpenTag.indexOf(emClose)
            if (closeIdx < 0) {
                append(remaining.substring(matchResult.range.first))
                break
            }

            val keyword = afterOpenTag.substring(0, closeIdx)
            withStyle(SpanStyle(color = highlightColor)) {
                append(keyword)
            }

            remaining = afterOpenTag.substring(closeIdx + emClose.length)
        }
    }
}

/**
 * 富文本解析：把 B 站动态/评论正文中的 `<br>`、HTML 实体、[表情]、
 * `@成员`、`#话题#`、URL 与 BV/av 号解析为带样式的 AnnotatedString + 行内表情内容。
 *
 * 对应 Android presentation/util/parseRichText.kt。
 *
 * 注意：
 * - 表情图渲染依赖 Kuikly `InlineTextContent`（行内图片），`Image` 走 coil3
 *   `rememberAsyncImagePainter`；若真机上行内图片渲染异常，可退化为纯文本。
 * - @成员 / #话题# / URL / 视频号 均写入 StringAnnotation（USER / TOPIC / URL / VIDEO），
 *   点击跳转逻辑由调用方处理（与 Android 一致）。
 */
fun parseRichText(
    text: String,
    emotes: Map<String, Emote>,
    members: Map<String, Long> = emptyMap(),
    resolvedB23Links: Map<String, String> = emptyMap(),
): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val inlineContentMap = mutableMapOf<String, InlineTextContent>()

    var processedText = text
        .replace("<br>", "\n")
        .replace("<br/>", "\n")
        .replace("<br />", "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")

    for ((b23Link, bv) in resolvedB23Links) {
        processedText = processedText.replace(b23Link, bv)
    }

    val urlPattern = "(https?://[^\\s<>()\\[\\]\"',;!?，。！？、：；“”（）【】《》]+|www\\.[^\\s<>()\\[\\]\"',;!?，。！？、：；“”（）【】《》]+)"
    val videoPattern = "(?i)(bv[A-Za-z0-9]+|av\\d+)"
    val fullPattern = Regex("($urlPattern|$videoPattern)")
    val topicPattern = Regex("#([^#\\n]+)#")

    val annotatedString = buildAnnotatedString {
        if (emotes.isEmpty() && members.isEmpty() && !topicPattern.containsMatchIn(processedText) && !processedText.contains(fullPattern)) {
            append(processedText)
            return@buildAnnotatedString
        }

        val parts = processedText.split(fullPattern)
        val matches = fullPattern.findAll(processedText).toList()

        for (i in parts.indices) {
            val part = parts[i]
            if (part.isNotEmpty()) {
                if (emotes.isNotEmpty() || members.isNotEmpty() || topicPattern.containsMatchIn(part)) {
                    val tokenPattern = Regex("\\[[^]]+]|@([\\w\\u4e00-\\u9fa5_-]+)|#([^#\\n]+)#")
                    var lastIdx = 0
                    for (match in tokenPattern.findAll(part)) {
                        val token = match.value

                        if (token.startsWith("[")) {
                            // 表情
                            val emote = emotes[token]
                            if (emote != null) {
                                append(part.substring(lastIdx, match.range.first))
                                appendInlineContent(token, token)
                                if (!inlineContentMap.containsKey(token)) {
                                    val sizeSp = (emote.size * 18).sp
                                    inlineContentMap[token] = InlineTextContent(
                                        placeholder = Placeholder(width = sizeSp, height = sizeSp),
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(emote.url),
                                            contentDescription = emote.name,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                                lastIdx = match.range.last + 1
                            }
                        } else if (token.startsWith("@")) {
                            // @成员
                            val name = match.groupValues[1]
                            val mid = members[name]
                            if (mid != null) {
                                append(part.substring(lastIdx, match.range.first))
                                pushStringAnnotation(tag = "USER", annotation = mid.toString())
                                withStyle(style = SpanStyle(color = OrbitColors.Primary)) {
                                    append(token)
                                }
                                pop()
                                lastIdx = match.range.last + 1
                            }
                        } else if (token.startsWith("#")) {
                            // #话题#
                            val topic = match.groupValues[2]
                            append(part.substring(lastIdx, match.range.first))
                            pushStringAnnotation(tag = "TOPIC", annotation = topic)
                            withStyle(style = SpanStyle(color = OrbitColors.Primary)) {
                                append(token)
                            }
                            pop()
                            lastIdx = match.range.last + 1
                        }
                    }
                    if (lastIdx < part.length) {
                        append(part.substring(lastIdx))
                    }
                } else {
                    append(part)
                }
            }

            if (i < matches.size) {
                val match = matches[i].value.trimEnd('.', ',', ';', ':', '!', '?')
                val isVideo = match.matches(Regex("(?i)(bv[A-Za-z0-9]+|av\\d+)"))
                val tag = if (isVideo) "VIDEO" else "URL"
                val annotation = if (isVideo) "video:$match" else "url:${if (match.startsWith("www.")) "https://$match" else match}"
                pushStringAnnotation(tag = tag, annotation = annotation)
                withStyle(SpanStyle(color = Color(0xFF4FC3F7), textDecoration = TextDecoration.Underline)) {
                    append(match)
                }
                pop()
            }
        }
    }
    return Pair(annotatedString, inlineContentMap)
}

// ---------------------------------------------------------------------------
// TimeUtil / FormatUtil 移植
// ---------------------------------------------------------------------------

/**
 * 当前系统时间（毫秒）。桥接 Kuikly `DateTime.currentTimestamp()`；
 * 该 API 在部分平台返回秒，这里按量级归一化为毫秒，避免时间单位不一致。
 */
fun currentTimeMillis(): Long {
    val t = DateTime.currentTimestamp()
    return if (t < 100_000_000_000L) t * 1000L else t
}

/** 格式化毫秒时间戳为 "HH:mm"（设备本地时区，经 ArkTS CalendarModule 桥）。 */
fun formatHm(millis: Long): String {
    return runCatching { CalendarModule().formatTime(millis, "HH:mm") }.getOrDefault("")
}

/**
 * B 站相对时间格式化。
 *
 * 对应 Android util/TimeUtil.kt 的 [formatBiliTime]；由 Java 的 Instant/LocalDateTime
 * 替换为 Kuikly `CalendarModule`（ArkTS 日历桥，设备本地时区）。
 */
fun formatBiliTime(timestampSeconds: Long): String {
    val targetMillis = timestampSeconds * 1000L
    val now = currentTimeMillis()
    val cal = CalendarModule()
    val targetCal = cal.newCalendarInstance(targetMillis)
    val nowCal = cal.newCalendarInstance(now)

    val diffMillis = now - targetMillis
    val diffMinutes = diffMillis / (60 * 1000)
    val diffHours = diffMillis / (60 * 60 * 1000)

    val dayDiff = nowCal.get(ICalendar.Field.DAY_OF_YEAR) - targetCal.get(ICalendar.Field.DAY_OF_YEAR) +
            (nowCal.get(ICalendar.Field.YEAR) - targetCal.get(ICalendar.Field.YEAR)) * 365

    return if (nowCal.get(ICalendar.Field.YEAR) == targetCal.get(ICalendar.Field.YEAR)) {
        if (nowCal.get(ICalendar.Field.DAY_OF_YEAR) == targetCal.get(ICalendar.Field.DAY_OF_YEAR)) {
            when {
                diffMinutes < 1L -> "刚刚"
                diffMinutes < 60L -> "${diffMinutes}分钟前"
                else -> "${diffHours}小时前"
            }
        } else if (dayDiff == 1) {
            "昨天 ${cal.formatTime(targetMillis, "HH:mm")}"
        } else if (dayDiff in 2..7) {
            "${dayDiff}天前"
        } else {
            cal.formatTime(targetMillis, "M月d日")
        }
    } else {
        cal.formatTime(targetMillis, "yyyy年M月d日")
    }
}

/**
 * 数字格式化：>=1 万显示 "x.x万"（四舍五入保留一位小数），否则原样。
 *
 * 对应 Android util/FormatUtil.kt 的 [formatCount]（String.format("%.1f万") 的等价实现，
 * 避免 java.lang.String.format）。同时提供 Int/Long 两个重载。
 */
fun formatCount(count: Int): String = formatCount(count.toLong())

fun formatCount(count: Long): String {
    return if (count >= 10000) {
        val tenths = (count * 10 + 5000) / 10000
        "${tenths / 10}.${tenths % 10}万"
    } else {
        count.toString()
    }
}

/**
 * 封面/头像 URL 修复：`//` 补全 https、http 升级为 https、avif 转 webp。
 *
 * 对应 Android util/FormatUtil.kt 的 [fixCoverUrl]。
 */
fun String.fixCoverUrl(): String {
    return when {
        this.startsWith("//") -> "https:$this"
        this.startsWith("http://") -> this.replaceFirst("http://", "https://")
        else -> this
    }.replace(".avif", ".webp")
}

// ---------------------------------------------------------------------------
// 文字阴影辅助（RecommendVideoCard 复用）
// ---------------------------------------------------------------------------

/**
 * 给文字加黑色描影（对应 Android RecommendVideoCard 中
 * `TextStyle.copy(shadow = Shadow(黑, Offset(2,2), 4f))` 的等价实现，
 * 因为 Kuikly material3 Text 无 style.shadow 参数，改由 SpanStyle 施加）。
 */
fun shadowedText(
    text: String,
    shadowColor: Color = Color.Black,
    offset: Offset = Offset(2f, 2f),
    blur: Float = 4f,
): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(shadow = Shadow(shadowColor, offset, blur))) {
        append(text)
    }
}

fun shadowedText(
    text: AnnotatedString,
    shadowColor: Color = Color.Black,
    offset: Offset = Offset(2f, 2f),
    blur: Float = 4f,
): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(shadow = Shadow(shadowColor, offset, blur))) {
        append(text)
    }
}
