package com.qx.orbit.core.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DanmakuTimelineTest {

    private fun ev(timeMs: Long, text: String = "t", index: Int = 0) =
        DanmakuEvent(timeMs = timeMs, mode = DanmakuMode.SCROLL, text = text, color = 0xFFFFFFFFL, sourceIndex = index)

    @Test
    fun build_sortsByTime() {
        val timeline = DanmakuTimeline.build(listOf(ev(3000, "c"), ev(1000, "a"), ev(2000, "b")))
        assertEquals(listOf("a", "b", "c"), timeline.all().map { it.text })
    }

    @Test
    fun build_stableForEqualTimes() {
        val timeline = DanmakuTimeline.build(listOf(ev(1000, "b", index = 1), ev(1000, "a", index = 0)))
        // 同刻按 sourceIndex 稳定排序
        assertEquals(listOf("a", "b"), timeline.all().map { it.text })
    }

    @Test
    fun slice_halfOpenRange() {
        val timeline = DanmakuTimeline.build(listOf(ev(1000), ev(2000), ev(3000)))
        assertEquals(listOf(2000L), timeline.slice(1500, 2500).map { it.timeMs })
        // 半开区间：[2000, 3000) 包含 2000，不包含 3000
        assertEquals(listOf(2000L), timeline.slice(2000, 3000).map { it.timeMs })
        assertEquals(listOf(2000L, 3000L), timeline.slice(2000, 3001).map { it.timeMs })
    }

    @Test
    fun slice_emptyWhenNoOverlap() {
        val timeline = DanmakuTimeline.build(listOf(ev(1000)))
        assertTrue(timeline.slice(500, 999).isEmpty())
        assertTrue(timeline.slice(1001, 2000).isEmpty())
        assertTrue(timeline.slice(2000, 1000).isEmpty())
    }

    @Test
    fun window_centeredRange() {
        val timeline = DanmakuTimeline.build(listOf(ev(1000), ev(2000), ev(3000)))
        assertEquals(listOf(2000L, 3000L), timeline.window(2500, 1000).map { it.timeMs })
    }

    @Test
    fun segments_consumeAllEvents() {
        val timeline = DanmakuTimeline.build(
            listOf(ev(1000), ev(1500), ev(61000), ev(65500), ev(120000)),
        )
        val segments = timeline.segments(60_000)
        assertEquals(setOf(0L, 60_000L, 120_000L), segments.keys)
        assertEquals(0 until 2, segments[0L])
        assertEquals(2 until 4, segments[60_000L])
        assertEquals(4 until 5, segments[120_000L])
        // 所有段拼起来等于全部事件
        val rebuilt = segments.values.flatMap { timeline.all().subList(it.first, it.last + 1) }
        assertEquals(5, rebuilt.size)
    }

    @Test
    fun segments_invalidInput() {
        val timeline = DanmakuTimeline.build(listOf(ev(1000)))
        assertTrue(timeline.segments(0).isEmpty())
        assertTrue(timeline.segments(-100).isEmpty())
        assertTrue(DanmakuTimeline.build(emptyList()).segments(60_000).isEmpty())
    }

    @Test
    fun emptyTimeline() {
        val timeline = DanmakuTimeline.build(emptyList())
        assertTrue(timeline.isEmpty())
        assertEquals(0, timeline.size)
        assertEquals(0L, timeline.startTimeMs)
        assertEquals(0L, timeline.endTimeMs)
        assertTrue(timeline.slice(0, 1000).isEmpty())
        assertTrue(timeline.all().isEmpty())
    }

    @Test
    fun bounds() {
        val timeline = DanmakuTimeline.build(listOf(ev(1000), ev(2000), ev(3000)))
        assertEquals(1000L, timeline.startTimeMs)
        assertEquals(3000L, timeline.endTimeMs)
    }
}
