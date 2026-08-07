package com.qx.orbit.core.model.social

import kotlin.test.Test
import kotlin.test.assertEquals

class ElectricFromWireTest {

    @Test
    fun `ElectricPanel maps user list with vip info`() {
        val panel = ElectricPanel.fromWire(
            mapOf(
                "count" to 2,
                "total_count" to 2,
                "total" to 200,
                "special_day" to 0,
                "list" to listOf(
                    mapOf(
                        "uname" to "充电用户",
                        "avatar" to "https://example.com/avatar.jpg",
                        "mid" to 9999L,
                        "pay_mid" to 9999L,
                        "rank" to 1,
                        "trend_type" to 0,
                        "message" to "支持一下（脱敏）",
                        "msg_hidden" to 0,
                        "vip_info" to mapOf(
                            "vipDueMsec" to 1700000000000L,
                            "vipStatus" to 1,
                            "vipType" to 2
                        )
                    )
                )
            )
        )

        assertEquals(2, panel.count)
        assertEquals(2, panel.total_count)
        assertEquals(200, panel.total)

        val user = panel.list.single()
        assertEquals("充电用户", user.uname)
        assertEquals("https://example.com/avatar.jpg", user.avatar)
        assertEquals(9999L, user.mid)
        assertEquals(9999L, user.pay_mid)
        assertEquals(1, user.rank)
        assertEquals("支持一下（脱敏）", user.message)
        assertEquals(2, user.vip_info?.vipType)
        assertEquals(1, user.vip_info?.vipStatus)
        assertEquals(1700000000000L, user.vip_info?.vipDueMsec)
    }

    @Test
    fun `ElectricUser accepts snake_case aliases`() {
        val user = ElectricUser.fromWire(
            mapOf(
                "uname" to "充电用户乙",
                "face" to "https://example.com/face.jpg",
                "vip_info" to mapOf("vip_due_msec" to 0L, "vip_status" to 1, "vip_type" to 1)
            )
        )
        assertEquals("充电用户乙", user.uname)
        assertEquals("https://example.com/face.jpg", user.avatar)
        assertEquals(1, user.vip_info?.vipType)
    }
}
