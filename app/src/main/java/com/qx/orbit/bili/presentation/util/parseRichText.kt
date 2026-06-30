package com.qx.orbit.bili.presentation.util

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import coil.compose.AsyncImage
import com.qx.orbit.bili.data.model.Emote
import kotlin.collections.iterator

@Composable
fun parseRichText(
    text: String,
    emotes: Map<String, Emote>,
    members: Map<String, Long> = emptyMap(),
    resolvedB23Links: Map<String, String> = emptyMap()
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

    val annotatedString = buildAnnotatedString {
        if (emotes.isEmpty() && members.isEmpty() && !processedText.contains(fullPattern)) {
            append(processedText)
            return@buildAnnotatedString
        }

        val parts = processedText.split(fullPattern)
        val matches = fullPattern.findAll(processedText).toList()

        for (i in parts.indices) {
            val part = parts[i]
            if (part.isNotEmpty()) {
                if (emotes.isNotEmpty() || members.isNotEmpty()) {
                    val tokenPattern = Regex("\\[[^]]+]|@([\\w\\u4e00-\\u9fa5_-]+)")
                    var lastIdx = 0
                    for (match in tokenPattern.findAll(part)) {
                        val token = match.value

                        if (token.startsWith("[")) {
                            // Emote
                            val emote = emotes[token]
                            if (emote != null) {
                                append(part.substring(lastIdx, match.range.first))
                                appendInlineContent(token, token)
                                if (!inlineContentMap.containsKey(token)) {
                                    val sizeSp = (emote.size * 18).sp
                                    inlineContentMap[token] = InlineTextContent(
                                        Placeholder(
                                            width = sizeSp,
                                            height = sizeSp,
                                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                        )
                                    ) {
                                        AsyncImage(
                                            model = emote.url,
                                            contentDescription = emote.name,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                lastIdx = match.range.last + 1
                            }
                        } else if (token.startsWith("@")) {
                            // Mention
                            val name = match.groupValues[1]
                            val mid = members[name]
                            if (mid != null) {
                                append(part.substring(lastIdx, match.range.first))
                                pushStringAnnotation(tag = "USER", annotation = mid.toString())
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append(token)
                                }
                                pop()
                                lastIdx = match.range.last + 1
                            }
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