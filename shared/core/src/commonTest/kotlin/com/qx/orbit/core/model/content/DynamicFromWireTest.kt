package com.qx.orbit.core.model.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DynamicFromWireTest {

    @Test
    fun `Dynamic parses feed item with emotes members and nested forward`() {
        val dynamic = Dynamic.fromWire(
            mapOf(
                "dynamic_id" to "7000000000",
                "type" to "DYNAMIC_TYPE_VIDEO",
                "comment_id" to 500L,
                "comment_type" to 17,
                "title" to "测试动态标题（脱敏）",
                "content" to "动态正文（脱敏）",
                "pubTime" to "2分钟前",
                "stats" to mapOf("view" to 10, "like" to 5),
                "canDelete" to true,
                "cover" to "https://example.com/cover.jpg",
                "bvid" to "BV1TEST123",
                "archiveTitle" to "测试视频（脱敏）",
                "liveState" to 0,
                "emotes" to mapOf(
                    "[测试表情]" to mapOf("id" to 1, "packageId" to 2, "name" to "笑", "url" to "https://example.com/emo.png")
                ),
                "members" to mapOf("测试用户" to 8888L),
                "dynamic_forward" to mapOf("dynamic_id" to "7000000001", "type" to "DYNAMIC_TYPE_TEXT", "content" to "转发内容（脱敏）")
            )
        )

        assertEquals("7000000000", dynamic.dynamicId)
        assertEquals("测试动态标题（脱敏）", dynamic.title)
        assertEquals(5, dynamic.stats?.like)
        assertTrue(dynamic.canDelete)
        assertEquals("[测试表情]", dynamic.emotes.keys.single())
        assertEquals("笑", dynamic.emotes.values.single().name)
        assertEquals(8888L, dynamic.members["测试用户"])
        assertEquals("转发内容（脱敏）", dynamic.dynamic_forward?.content)
    }

    @Test
    fun `Dynamic keeps major_object raw passthrough`() {
        val major = mapOf("aid" to 123456L, "title" to "大卡（脱敏）")
        val dynamic = Dynamic.fromWire(mapOf("dynamic_id" to "1", "major_type" to "MAJOR_TYPE_ARCHIVE", "major_object" to major))
        assertEquals("MAJOR_TYPE_ARCHIVE", dynamic.major_type)
        assertEquals(major, dynamic.major_object)
    }

    @Test
    fun `Opus parses paragraphs array and userInfo alias`() {
        val opus = Opus.fromWire(
            mapOf(
                "id" to 9001L,
                "type" to 1,
                "commentId" to 77L,
                "title" to "Opus 标题（脱敏）",
                "content" to "Opus 正文（脱敏）",
                "pubTime" to "1小时前",
                "userInfo" to mapOf("mid" to 8888L, "name" to "测试作者"),
                "stats" to mapOf("view" to 3),
                "paragraphs" to listOf(
                    mapOf("type" to 1, "textNodes" to listOf(mapOf("text" to "第一段（脱敏）", "bold" to true)))
                )
            )
        )

        assertEquals(9001L, opus.id)
        assertEquals("测试作者", opus.upInfo?.name)
        val paragraph = assertNotNull(opus.paragraphs).single()
        assertEquals(1, paragraph.type)
        assertTrue(paragraph.textNodes.single().bold)
        assertEquals("第一段（脱敏）", paragraph.textNodes.single().text)
    }

    @Test
    fun `OpusTextNode handles nullable fields`() {
        val node = OpusTextNode.fromWire(mapOf("text" to "正文", "jumpUrl" to "https://example.com", "fontSize" to 20))
        assertEquals("https://example.com", node.jumpUrl)
        assertEquals(20, node.fontSize)
        assertNull(node.memberId)
        assertNull(node.color)

        val withMember = OpusTextNode.fromWire(mapOf("text" to "正文", "member_id" to 42L))
        assertEquals(42L, withMember.memberId)
    }

    @Test
    fun `OpusCard and Emote parse from wire`() {
        val card = OpusCard.fromWire(mapOf("title" to "Opus卡片（脱敏）", "id" to 1L, "view" to "999"))
        assertEquals("Opus卡片（脱敏）", card.title)
        assertEquals("999", card.view)

        val emote = Emote.fromWire(mapOf("id" to 3, "package_id" to 4, "name" to "大哭", "url" to "https://example.com/emo2.png"))
        assertEquals(4, emote.packageId)
        assertEquals("大哭", emote.name)
    }
}
