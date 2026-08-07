package com.qx.orbit.core.model.social

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReplyFromWireTest {

    @Test
    fun `Reply maps v2 reply wire fields`() {
        val reply = Reply.fromWire(
            mapOf(
                "rpid" to 888L,
                "oid" to 123456L,
                "root" to 0L,
                "parent" to 0L,
                "member" to mapOf(
                    "mid" to 9999L,
                    "uname" to "评论用户",
                    "face" to "https://example.com/avatar.jpg"
                ),
                "content" to mapOf("message" to "示例评论内容（脱敏）"),
                "pictures" to listOf(mapOf("img_src" to "https://example.com/pic1.jpg")),
                "like" to 42,
                "rcount" to 3,
                "up_action" to mapOf("like" to true, "reply" to false),
                "reply_control" to mapOf("is_up_top" to true, "is_up" to true),
                "replies" to listOf(
                    mapOf("rpid" to 889L, "content" to mapOf("message" to "子评论（脱敏）"))
                ),
                "emotes" to mapOf(
                    "[doge]" to mapOf("id" to 7, "url" to "https://example.com/emote.png", "name" to "doge")
                ),
                "members" to mapOf("888" to 9999L)
            )
        )

        assertEquals(888L, reply.rpid)
        assertEquals(123456L, reply.oid)
        assertEquals("示例评论内容（脱敏）", reply.message)
        assertEquals("评论用户", reply.sender?.name)
        assertEquals("https://example.com/avatar.jpg", reply.sender?.avatar)
        assertEquals(listOf("https://example.com/pic1.jpg"), reply.pictureList)
        assertEquals(42, reply.likeCount)
        assertEquals(3, reply.childCount)
        assertTrue(reply.upLiked)
        assertTrue(!reply.upReplied)
        assertTrue(reply.isTop)
        assertTrue(reply.isUp)
        assertEquals("子评论（脱敏）", reply.childMsgList.single().message)
        assertEquals("doge", reply.emotes["[doge]"]?.name)
        assertEquals(9999L, reply.members["888"])
    }

    @Test
    fun `Reply reads camelCase and direct message keys`() {
        val reply = Reply.fromWire(
            mapOf(
                "rpid" to 1L,
                "message" to "直接 message（脱敏）",
                "likeCount" to 5,
                "childCount" to 0,
                "isTop" to true,
                "isUp" to false
            )
        )
        assertEquals("直接 message（脱敏）", reply.message)
        assertEquals(5, reply.likeCount)
        assertTrue(reply.isTop)
        assertTrue(!reply.isUp)
    }

    @Test
    fun `ContentType resolves from code`() {
        assertEquals(ContentType.Video, ContentType.fromCode(1))
        assertEquals(ContentType.Live, ContentType.fromCode(8))
        assertEquals(ContentType.Course, ContentType.fromCode(33))
        assertEquals(ContentType.Video, ContentType.fromCode(9999)) // 未知名回落 Video
    }
}
