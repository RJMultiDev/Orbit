package com.qx.orbit.ohos.util

import com.qx.orbit.ohos.components.Emote
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.text.InlineTextContent
import com.tencent.kuikly.compose.foundation.text.appendInlineContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.AnnotatedString
import com.tencent.kuikly.compose.ui.text.Placeholder
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.text.style.TextDecoration
import com.tencent.kuikly.compose.ui.text.withStyle
import com.tencent.kuikly.compose.ui.unit.sp

/**
 * 富文本解析：把 B 站动态/评论正文中的 `<br>`、HTML 实体、[表情]、
 * `@成员`、`#话题#`、URL 与 BV/av 号解析为带样式的 AnnotatedString + 行内表情内容。
 *
 * 对应 Android presentation/util/parseRichText.kt。
 *
 * 注意：
 * - 表情模型复用 components/Utils.kt 的 [Emote]；表情图渲染依赖 Kuikly
 *   `InlineTextContent`（行内图片），`Image` 走 coil3 `rememberAsyncImagePainter`；
 *   若真机上行内图片渲染异常，可退化为纯文本。
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
