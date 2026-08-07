package com.qx.orbit.core.model.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserAccountFromWireTest {

    @Test
    fun `UserInfo reads uname face fans aliases and nested vip live_room`() {
        val user = UserInfo.fromWire(
            mapOf(
                "mid" to 8888L,
                "uname" to "测试用户甲",
                "face" to "https://example.com/avatar.jpg",
                "sign" to "测试签名（脱敏）",
                "fans" to 123,
                "level" to 4,
                "following" to 10,
                "followed" to true,
                "vip" to mapOf("vipStatus" to 1, "vipType" to 2),
                "official" to mapOf("type" to 1),
                "live_room" to mapOf("roomid" to 12345L, "title" to "测试直播间（脱敏）", "online" to 8)
            )
        )

        assertEquals(8888L, user.mid)
        assertEquals("测试用户甲", user.name)
        assertEquals("https://example.com/avatar.jpg", user.avatar)
        assertEquals(123, user.fans)
        assertEquals(1, user.vip_role)
        assertEquals(2, user.official) // official_verify.type==1 -> 2
        assertEquals(12345L, user.live_room?.roomid)
        assertEquals("测试直播间（脱敏）", user.live_room?.title)
    }

    @Test
    fun `UserInfo reads vip_role int and official role directly`() {
        val user = UserInfo.fromWire(
            mapOf(
                "name" to "测试用户乙",
                "vip_role" to 1,
                "official" to 2
            )
        )
        assertEquals(1, user.vip_role)
        assertEquals(2, user.official)
    }

    @Test
    fun `LiveRoom reads room_id and area aliases`() {
        val room = LiveRoom.fromWire(
            mapOf(
                "room_id" to 2222L,
                "short_id" to 0,
                "uid" to 8888L,
                "title" to "测试直播间（脱敏）",
                "area_v2_name" to "虚拟主播",
                "live_status" to 1
            )
        )
        assertEquals(2222L, room.roomid)
        assertEquals("虚拟主播", room.area_name)
        assertEquals(1, room.live_status)
    }

    @Test
    fun `Account models parse from wire`() {
        val login = LoginRecord.fromWire(
            mapOf("mid" to 1L, "deviceName" to "测试手表", "loginType" to "qrcode", "loginTime" to "2026-08-07 10:00", "location" to "测试地", "ip" to "0.0.0.0")
        )
        assertEquals("测试手表", login.deviceName)

        val exp = ExpLog.fromWire(mapOf("delta" to 5, "time" to "2026-08-07", "reason" to "测试经验"))
        assertEquals(5, exp.delta)

        val coin = CoinLog.fromWire(mapOf("time" to "2026-08-07", "delta" to -1, "reason" to "测试扣币"))
        assertEquals(-1, coin.delta)

        val medal = MedalInfo.fromWire(
            mapOf("target_id" to 999L, "level" to 3, "medal_name" to "测试勋章", "medal_id" to 42L, "wearing_status" to 1)
        )
        assertEquals("测试勋章", medal.medal_name)
        assertEquals(1, medal.wearing_status)

        val tag = FollowTag.fromWire(mapOf("tagid" to 5, "name" to "测试标签", "count" to 2))
        assertEquals(5, tag.tagid)

        val vip = VipDetailInfo.fromWire(
            mapOf(
                "isVip" to true, "level" to 6, "vipStatus" to 1, "vipType" to 2,
                "vipDueDate" to 1760000000000L,
                "privilegeList" to listOf(mapOf("type" to 1, "state" to 1, "expireTime" to 1760000000000L))
            )
        )
        assertTrue(vip.isVip)
        assertEquals(1, vip.privilegeList.single().state)
    }

    @Test
    fun `NavInfo parses login state and nested level vip`() {
        val nav = NavInfo.fromWire(
            mapOf(
                "isLogin" to true,
                "mid" to 8888L,
                "uname" to "测试用户甲",
                "face" to "https://example.com/avatar.jpg",
                "level_info" to mapOf("current_level" to 4, "current_exp" to 100, "next_exp" to 200),
                "vip" to mapOf("vipType" to 2, "vipStatus" to 1, "nickname_color" to "#FF0000")
            )
        )
        assertTrue(nav.isLogin)
        assertEquals("测试用户甲", nav.uname)
        assertEquals(4, nav.level_info?.current_level)
        assertEquals(2, nav.vip?.vipType)
        assertEquals("#FF0000", nav.vip?.nickname_color)
    }

    @Test
    fun `NavInfo stays nullable when fields absent`() {
        val nav = NavInfo.fromWire(emptyMap())
        assertTrue(!nav.isLogin)
        assertNull(nav.uname)
        assertNull(nav.level_info)
    }
}
