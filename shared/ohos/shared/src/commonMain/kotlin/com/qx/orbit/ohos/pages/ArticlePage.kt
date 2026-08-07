package com.qx.orbit.ohos.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.components.formatBiliTime
import com.qx.orbit.ohos.data.ArticleDetail
import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.data.articleView
import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * 专栏文章详情页（对齐 Android ArticleDetailScreen 的 ArticleContentPage，精简为纯文本）。
 */
@Page("article")
internal class ArticlePage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            ArticleContent(client)
        }
    }
}

@Composable
internal fun ArticleContent(client: BiliClient) {
    val id = PageArgs.getLong("id")
    var detail by remember { mutableStateOf<ArticleDetail?>(null) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(id) {
        if (id <= 0L) {
            error = "缺少文章参数"
            return@LaunchedEffect
        }
        try {
            detail = client.articleView(id)
        } catch (e: Throwable) {
            error = e.message ?: "加载失败"
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        val d = detail
        if (d != null) {
            // 标题
            Text(
                text = d.title,
                fontSize = 14.sp,
                color = OrbitColors.OnSurface,
                modifier = Modifier.fillMaxWidth().padding(10.dp),
            )
            // 作者行 + 发布时间
            Text(
                text = if (d.publishTime > 0) "${d.authorName} · ${formatBiliTime(d.publishTime)}" else d.authorName,
                fontSize = 11.sp,
                color = OrbitColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            // 正文：纯文本渲染；富文本/图片暂以 TODO 占位
            // TODO: 解析 content 中的 <img>/<h1-6>/<a>/<blockquote> 为富文本段（对齐 Android parseArticleHtml）
            Text(
                text = articlePlainText(d.content),
                fontSize = 12.sp,
                color = OrbitColors.OnSurface,
                modifier = Modifier.fillMaxWidth().padding(10.dp),
            )
            // 点赞/收藏/评论数行
            Text(
                text = "${formatCount(d.likeCount)}赞 · ${formatCount(d.favoriteCount)}收藏 · " +
                    "${formatCount(d.commentCount)}评论",
                fontSize = 11.sp,
                color = OrbitColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        } else {
            Text(
                text = if (error.isNotEmpty()) error else "加载中…",
                fontSize = 12.sp,
                color = if (error.isNotEmpty()) OrbitColors.ErrorRed else OrbitColors.TextSecondary,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

/** 文章 HTML 转纯文本：去标签、解实体、压缩连续换行。 */
private fun articlePlainText(html: String): String {
    if (html.isBlank()) return ""
    val tagStripper = Regex("""<[^>]+>""")
    val entityMap = mapOf(
        "&amp;" to "&", "&lt;" to "<", "&gt;" to ">",
        "&quot;" to "\"", "&#39;" to "'", "&nbsp;" to " ",
    )
    var text = tagStripper.replace(html, "")
    for ((entity, char) in entityMap) text = text.replace(entity, char)
    return text.replace(Regex("""\n{3,}"""), "\n\n").trim()
}
