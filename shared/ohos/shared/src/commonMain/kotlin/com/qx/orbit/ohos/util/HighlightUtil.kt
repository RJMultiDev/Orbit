package com.qx.orbit.ohos.util

import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.AnnotatedString
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.text.withStyle

/**
 * 解析 `<em class="keyword">...</em>` 高亮标签，返回关键词用高亮色着色的 AnnotatedString。
 *
 * 对应 Android presentation/util/HighlightUtil.kt 的 [parseHighlightedTitle]。
 * 默认高亮色用 OrbitColors.Primary（Android 默认取 MaterialTheme.colorScheme.primary，
 * Kuikly 主题无此读取 API，改为显式传色/默认主色）。
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

            // 标签前文本原样追加
            append(remaining.substring(0, matchResult.range.first))

            // 找闭合标签；缺失则余下部分原样追加
            val afterOpenTag = remaining.substring(matchResult.range.last + 1)
            val closeIdx = afterOpenTag.indexOf(emClose)
            if (closeIdx < 0) {
                append(remaining.substring(matchResult.range.first))
                break
            }

            // 关键词上高亮色
            val keyword = afterOpenTag.substring(0, closeIdx)
            withStyle(SpanStyle(color = highlightColor)) {
                append(keyword)
            }

            remaining = afterOpenTag.substring(closeIdx + emClose.length)
        }
    }
}
