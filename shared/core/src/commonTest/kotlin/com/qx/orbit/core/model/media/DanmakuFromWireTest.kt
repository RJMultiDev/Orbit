package com.qx.orbit.core.model.media

import kotlin.test.Test
import kotlin.test.assertEquals

class DanmakuFromWireTest {

    @Test
    fun `DanmakuElem maps seg reply fields`() {
        val reply = DmSegMobileReply.fromWire(
            mapOf(
                "elems" to listOf(
                    mapOf(
                        "id" to 1001L,
                        "progress" to 5000,
                        "mode" to 1,
                        "fontsize" to 25,
                        "color" to 16777215,
                        "midHash" to "a1b2c3",
                        "content" to "示例弹幕（脱敏）",
                        "ctime" to 1700000000L,
                        "weight" to 0,
                        "action" to "none",
                        "pool" to 0,
                        "idStr" to "1001",
                        "attr" to 0,
                        "animation" to "none"
                    )
                )
            )
        )

        val elem = reply.elems.single()
        assertEquals(1001L, elem.id)
        assertEquals(5000, elem.progress)
        assertEquals(1, elem.mode)
        assertEquals(25, elem.fontsize)
        assertEquals(16777215, elem.color)
        assertEquals("a1b2c3", elem.midHash)
        assertEquals("示例弹幕（脱敏）", elem.content)
        assertEquals(1700000000L, elem.ctime)
        assertEquals("1001", elem.idStr)
    }

    @Test
    fun `DanmakuElem keeps mode and fontsize defaults when keys absent`() {
        val elem = DanmakuElem.fromWire(mapOf("content" to "无参弹幕"))
        assertEquals(1, elem.mode)
        assertEquals(25, elem.fontsize)
        assertEquals("无参弹幕", elem.content)
    }
}
