package com.qx.orbit.core.model.social

import kotlin.test.Test
import kotlin.test.assertEquals

class MessageFromWireTest {

    @Test
    fun `MessageCard maps message box item`() {
        val card = MessageCard.fromWire(
            mapOf(
                "id" to 100L,
                "user" to listOf(
                    mapOf("mid" to 9999L, "uname" to "消息用户", "face" to "https://example.com/m.jpg")
                ),
                "timestamp" to 1700000000L,
                "title" to "回复了你",
                "content" to "示例消息内容（脱敏）",
                "video" to mapOf(
                    "title" to "被回复视频（脱敏）",
                    "author" to "测试UP主",
                    "aid" to 123456L,
                    "bvid" to "BV1TEST456"
                ),
                "reply" to mapOf("rpid" to 777L, "content" to mapOf("message" to "被回复的评论（脱敏）")),
                "subject_id" to 555L,
                "business_id" to 2,
                "item_type" to "reply",
                "get_type" to 0,
                "source_id" to 1L,
                "root_id" to 2L,
                "target_id" to 3L
            )
        )

        assertEquals(100L, card.id)
        assertEquals("消息用户", card.user.single().name)
        assertEquals(1700000000L, card.timeStamp)
        assertEquals("回复了你", card.timeDesc)
        assertEquals("示例消息内容（脱敏）", card.content)
        assertEquals("被回复视频（脱敏）", card.videoCard?.title)
        assertEquals("BV1TEST456", card.videoCard?.bvid)
        assertEquals(777L, card.replyInfo?.rpid)
        assertEquals("被回复的评论（脱敏）", card.replyInfo?.message)
        assertEquals(555L, card.subjectId)
        assertEquals(2, card.businessId)
        assertEquals("reply", card.itemType)
        assertEquals(0, card.getType)
    }

    @Test
    fun `MessageCard Cursor parses paging`() {
        val cursor = MessageCard.Cursor.fromWire(mapOf("is_end" to false, "id" to 90L, "time" to 1700000000L))
        assertEquals(false, cursor.is_end)
        assertEquals(90L, cursor.id)
        assertEquals(1700000000L, cursor.time)
    }

    @Test
    fun `PrivateMessage keeps opaque content and message identity`() {
        val pm = PrivateMessage.fromWire(
            mapOf(
                "type" to PrivateMessage.TYPE_TEXT,
                "timestamp" to 1700000000L,
                "uid" to 9999L,
                "name" to "私信用户",
                "msg_id" to 300L,
                "msg_seqno" to 1L,
                "content" to mapOf("content" to "你好（脱敏）")
            )
        )

        assertEquals(PrivateMessage.TYPE_TEXT, pm.type)
        assertEquals(1700000000L, pm.timestamp)
        assertEquals(9999L, pm.uid)
        assertEquals(300L, pm.msgId)
        assertEquals(1L, pm.msgSeqno)
        assertEquals("你好（脱敏）", (pm.content as? Map<*, *>)?.get("content"))
    }

    @Test
    fun `MessageSettingItem defaults switch type and options`() {
        val item = MessageSettingItem.fromWire(mapOf("key" to "dm_show", "title" to "显示弹幕", "value" to true))
        assertEquals(MessageSettingItem.TYPE_SWITCH, item.type)
        assertEquals(true, item.value)
        assertEquals(null, item.options)

        val choose = MessageSettingItem.fromWire(mapOf("key" to "font", "type" to 1, "options" to listOf("小", "中", "大")))
        assertEquals(MessageSettingItem.TYPE_CHOOSE, choose.type)
        assertEquals(listOf("小", "中", "大"), choose.options)
    }
}
