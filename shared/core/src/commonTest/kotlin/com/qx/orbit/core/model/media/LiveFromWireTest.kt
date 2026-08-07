package com.qx.orbit.core.model.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LiveFromWireTest {

    @Test
    fun `LiveInfo maps anchor room and play info`() {
        val info = LiveInfo.fromWire(
            mapOf(
                "userInfo" to mapOf(
                    "mid" to 9999L,
                    "uname" to "测试主播",
                    "face" to "https://example.com/face.jpg"
                ),
                "liveRoom" to mapOf(
                    "roomid" to 12345L,
                    "title" to "示例直播间（脱敏）",
                    "online" to 66,
                    "live_status" to 1,
                    "area_name" to "手游"
                ),
                "livePlayInfo" to mapOf(
                    "room_id" to 12345L,
                    "uid" to 9999L,
                    "live_status" to 1,
                    "is_portrait" to true,
                    "live_time" to 1700000000L,
                    "playurl_info" to mapOf(
                        "playurl" to mapOf("cid" to 555L)
                    )
                )
            )
        )

        assertEquals("测试主播", info.userInfo?.name)
        assertEquals("示例直播间（脱敏）", info.liveRoom?.title)
        assertEquals(66, info.liveRoom?.online)
        assertEquals("手游", info.liveRoom?.area_name)

        val play = assertNotNull(info.livePlayInfo)
        assertEquals(12345L, play.roomid)
        assertTrue(play.isPortrait)
        assertEquals(1, play.live_status)
        assertEquals(1700000000L, play.live_time)
        assertEquals(555L, play.playurl_info?.playurl?.cid)
    }

    @Test
    fun `LivePlayInfo accepts roomid alias and absent playurl`() {
        val play = LivePlayInfo.fromWire(mapOf("roomid" to 12345L, "live_status" to 1))
        assertEquals(12345L, play.roomid)
        assertEquals(1, play.live_status)
        assertEquals(null, play.playurl_info)
    }
}
