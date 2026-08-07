package com.qx.orbit.core.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuFilterTest {

    private fun ev(timeMs: Long, mode: DanmakuMode = DanmakuMode.SCROLL, text: String = "t", userHash: String = "") =
        DanmakuEvent(timeMs = timeMs, mode = mode, text = text, color = 0xFFFFFFFFL, userHash = userHash)

    @Test
    fun blockTextContains() {
        val config = DanmakuFilterConfig(blockTextRules = listOf(TextRule(TextRuleType.CONTAINS, "广告")))
        val result = DanmakuFilterEngine.filter(listOf(ev(1000, text = "打广告的滚"), ev(2000, text = "正常弹幕")), config)
        assertEquals(listOf("正常弹幕"), result.map { it.text })
    }

    @Test
    fun blockTextEquals() {
        val config = DanmakuFilterConfig(blockTextRules = listOf(TextRule(TextRuleType.EQUALS, "举报")))
        val result = DanmakuFilterEngine.filter(
            listOf(ev(1000, text = "举报"), ev(2000, text = "举报一下")), config,
        )
        assertEquals(listOf("举报一下"), result.map { it.text })
    }

    @Test
    fun blockTextRegex() {
        val config = DanmakuFilterConfig(blockTextRules = listOf(TextRule(TextRuleType.REGEX, "\\d{4,}")))
        val result = DanmakuFilterEngine.filter(listOf(ev(1000, text = "QQ 12345678"), ev(2000, text = "无数字")), config)
        assertEquals(listOf("无数字"), result.map { it.text })
    }

    @Test
    fun invalidRegexDoesNotCrash() {
        val config = DanmakuFilterConfig(blockTextRules = listOf(TextRule(TextRuleType.REGEX, "[")))
        val result = DanmakuFilterEngine.filter(listOf(ev(1000, text = "任意文本")), config)
        assertEquals(1, result.size)
    }

    @Test
    fun blockModes() {
        val config = DanmakuFilterConfig(blockModes = setOf(DanmakuMode.FIX_TOP))
        val result = DanmakuFilterEngine.filter(
            listOf(
                ev(1000, mode = DanmakuMode.FIX_TOP),
                ev(2000, mode = DanmakuMode.SCROLL),
            ),
            config,
        )
        assertEquals(1, result.size)
        assertEquals(DanmakuMode.SCROLL, result[0].mode)
    }

    @Test
    fun blockUserHashes() {
        val config = DanmakuFilterConfig(blockUserHashes = setOf("spam01"))
        val result = DanmakuFilterEngine.filter(
            listOf(ev(1000, userHash = "spam01"), ev(2000, userHash = "normal")), config,
        )
        assertEquals(listOf("normal"), result.map { it.userHash })
    }

    @Test
    fun advancedDisabledDegradesSpecialAndCodeToScroll() {
        val config = DanmakuFilterConfig(advancedDanmakuEnabled = false)
        val result = DanmakuFilterEngine.filter(
            listOf(
                ev(1000, mode = DanmakuMode.SPECIAL),
                ev(2000, mode = DanmakuMode.CODE),
                ev(3000, mode = DanmakuMode.FIX_TOP),
            ),
            config,
        )
        // 降级而非屏蔽：三条都保留，7/8 变为 SCROLL
        assertEquals(3, result.size)
        assertEquals(DanmakuMode.SCROLL, result[0].mode)
        assertEquals(DanmakuMode.SCROLL, result[1].mode)
        assertEquals(DanmakuMode.FIX_TOP, result[2].mode)
    }

    @Test
    fun specialInvisibleDropsSpecial() {
        val config = DanmakuFilterConfig(specialDanmakuVisible = false)
        val result = DanmakuFilterEngine.filter(
            listOf(ev(1000, mode = DanmakuMode.SPECIAL), ev(2000, mode = DanmakuMode.SCROLL)), config,
        )
        assertEquals(1, result.size)
        assertEquals(DanmakuMode.SCROLL, result[0].mode)
    }

    @Test
    fun forceR2LConvertsNonSpecial() {
        val config = DanmakuFilterConfig(forceR2L = true)
        val result = DanmakuFilterEngine.filter(
            listOf(
                ev(1000, mode = DanmakuMode.FIX_TOP),
                ev(2000, mode = DanmakuMode.REVERSE),
                ev(3000, mode = DanmakuMode.SPECIAL),
            ),
            config,
        )
        assertEquals(DanmakuMode.SCROLL, result[0].mode)
        assertEquals(DanmakuMode.SCROLL, result[1].mode)
        // SPECIAL 不受 forceR2L 影响
        assertEquals(DanmakuMode.SPECIAL, result[2].mode)
    }

    @Test
    fun duplicateMergingWithinWindow() {
        val config = DanmakuFilterConfig(duplicateMergingEnabled = true, duplicateWindowMs = 10_000)
        val result = DanmakuFilterEngine.filter(
            listOf(
                ev(1000, text = "重复"),
                ev(3000, text = "重复"),   // 3s 后，窗口内 → 合并
                ev(5000, text = "不同"),
                ev(12_000, text = "重复"),  // 11s 后，窗口外 → 保留
            ),
            config,
        )
        assertEquals(listOf(1000L, 5000L, 12000L), result.map { it.timeMs })
    }

    @Test
    fun duplicateMergingDisabledKeepsAll() {
        val config = DanmakuFilterConfig(duplicateMergingEnabled = false)
        val result = DanmakuFilterEngine.filter(listOf(ev(1000, text = "重复"), ev(3000, text = "重复")), config)
        assertEquals(2, result.size)
    }

    @Test
    fun defaultConfigKeepsEverything() {
        val events = listOf(
            ev(1000, mode = DanmakuMode.SPECIAL),
            ev(2000, mode = DanmakuMode.FIX_TOP),
            ev(3000, mode = DanmakuMode.SCROLL, text = "任何文本", userHash = "u"),
        )
        val result = DanmakuFilterEngine.filter(events, DanmakuFilterConfig())
        assertEquals(events, result)
    }

    @Test
    fun shouldBlockEmptyUserHashNotMatched() {
        val config = DanmakuFilterConfig(blockUserHashes = setOf("u"))
        // 空 userHash 不参与用户屏蔽（避免误伤所有匿名弹幕）
        assertFalse(DanmakuFilterEngine.shouldBlock(ev(1000, userHash = ""), config))
        assertTrue(DanmakuFilterEngine.shouldBlock(ev(1000, userHash = "u"), config))
    }
}
