package com.qx.orbit.core.model.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArticleFromWireTest {

    @Test
    fun `ArticleInfo maps wire with author stats and image_urls banner`() {
        val article = ArticleInfo.fromWire(
            mapOf(
                "id" to 456789L,
                "title" to "示例专栏（脱敏）",
                "summary" to "专栏摘要（脱敏）",
                "image_urls" to listOf("https://example.com/banner.jpg"),
                "author" to mapOf("mid" to 8888L, "name" to "测试作者", "face" to "https://example.com/face.jpg"),
                "ctime" to 1750000000L,
                "stats" to mapOf("view" to 333, "like" to 22),
                "word_count" to 1200,
                "keywords" to "测试,脱敏",
                "content" to "正文内容（脱敏）"
            )
        )

        assertEquals(456789L, article.id)
        assertEquals("示例专栏（脱敏）", article.title)
        assertEquals("https://example.com/banner.jpg", article.banner)
        assertEquals("测试作者", article.upInfo?.name)
        assertEquals(333, article.stats?.view)
        assertEquals(1200, article.wordCount)
    }

    @Test
    fun `ArticleCard reads author_name and formats view`() {
        val card = ArticleCard.fromWire(
            mapOf(
                "title" to "搜索专栏（脱敏）",
                "id" to 456789L,
                "image_urls" to listOf("https://example.com/cover.jpg"),
                "author_name" to "测试作者",
                "view" to 99999
            )
        )
        assertEquals("搜索专栏（脱敏）", card.title)
        assertEquals("https://example.com/cover.jpg", card.cover)
        assertEquals("测试作者", card.upName)
        assertEquals("10.0万", card.view)
    }

    @Test
    fun `ArticleLine parses simple fields`() {
        val line = ArticleLine.fromWire(mapOf("type" to 1, "content" to "段落（脱敏）", "extra" to ""))
        assertEquals(1, line.type)
        assertEquals("段落（脱敏）", line.content)
    }
}
