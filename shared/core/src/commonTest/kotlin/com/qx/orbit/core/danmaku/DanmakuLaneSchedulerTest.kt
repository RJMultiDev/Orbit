package com.qx.orbit.core.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DanmakuLaneSchedulerTest {

    private fun ev(timeMs: Long, mode: DanmakuMode = DanmakuMode.SCROLL, text: String = "abc", index: Int = 0) =
        DanmakuEvent(
            timeMs = timeMs,
            mode = mode,
            text = text,
            color = 0xFFFFFFFFL,
            fontSize = 25f,
            sourceIndex = index,
        )

    private fun scheduler(
        maxLines: Int = 0,
        allowOverlap: Boolean = false,
        policy: LaneOverflowPolicy = LaneOverflowPolicy.DROP,
        fixedDurationMs: Long = 4_000L,
    ) = DanmakuLaneScheduler(
        laneWidthPx = 1000,
        speedPxPerSec = 600f,
        maxLines = maxLines,
        allowOverlap = allowOverlap,
        overflowPolicy = policy,
        fixedDurationMs = fixedDurationMs,
    )

    @Test
    fun scrollDuration_usesWidthSpeedModel() {
        // (1000 + 3*25) / 600 px/秒 = 1.7916... 秒 → 1791ms
        assertEquals(1791L, scheduler().scrollDurationMs(ev(0)))
    }

    @Test
    fun schedule_isDeterministic() {
        val s = scheduler(maxLines = 2)
        val events = listOf(ev(0), ev(1000), ev(2000), ev(4000))
        assertEquals(s.schedule(events), s.schedule(events))
    }

    @Test
    fun avoidance_reusesLaneAfterRelease() {
        val s = scheduler(allowOverlap = false)
        // "abc" 时长 1791ms；t=0 占 lane0 至 1791，t=1000 时仍未释放 → 开新轨
        val result = s.schedule(listOf(ev(0, index = 0), ev(1000, index = 1), ev(2000, index = 2)))
        assertEquals(listOf(0, 1, 0), result.map { it.lane })
        assertEquals(listOf(0L, 1000L, 2000L), result.map { it.event.timeMs })
    }

    @Test
    fun overflowDrop_dropsWhenFull() {
        val s = scheduler(maxLines = 1, allowOverlap = false, policy = LaneOverflowPolicy.DROP)
        // t=1000 时 lane0 未释放且容量已满 → 丢弃
        val result = s.schedule(listOf(ev(0), ev(1000), ev(2000)))
        assertEquals(listOf(0L, 2000L), result.map { it.event.timeMs })
        assertEquals(listOf(0, 0), result.map { it.lane })
    }

    @Test
    fun overflowPickEarliest_alwaysAssigns() {
        val s = scheduler(maxLines = 1, allowOverlap = false, policy = LaneOverflowPolicy.PICK_EARLIEST)
        val result = s.schedule(listOf(ev(0), ev(1000), ev(2000)))
        assertEquals(3, result.size)
        assertTrue(result.all { it.lane == 0 })
    }

    @Test
    fun allowOverlap_roundRobinNeverDrops() {
        val s = scheduler(maxLines = 2, allowOverlap = true)
        val result = s.schedule(listOf(ev(0), ev(1), ev(2)))
        assertEquals(3, result.size)
        assertEquals(listOf(0, 1, 0), result.map { it.lane })
    }

    @Test
    fun unlimitedLines_opensNewLanes() {
        val s = scheduler(allowOverlap = false)
        // 不限行数：每条忙碌轨道都不复用，新开轨道
        val result = s.schedule(listOf(ev(0), ev(500), ev(1000)))
        assertEquals(listOf(0, 1, 2), result.map { it.lane })
    }

    @Test
    fun fixedTopAndBottom_useFixedDurationAndSeparatePools() {
        val s = scheduler(allowOverlap = false)
        val top0 = ev(0, mode = DanmakuMode.FIX_TOP, index = 0)
        val top1 = ev(3000, mode = DanmakuMode.FIX_TOP, index = 1)
        val top2 = ev(4000, mode = DanmakuMode.FIX_TOP, index = 2)
        val scroll = ev(0, mode = DanmakuMode.SCROLL, index = 3)

        val result = s.schedule(listOf(top0, top1, top2, scroll))
        // top：t=0 lane0（占用至 4000），t=3000 未释放 → lane1，t=4000 释放 → lane0
        // scroll 独立轨道池 → lane0
        assertEquals(
            listOf(
                LaneAssignment(top0, 0),
                LaneAssignment(scroll, 0),
                LaneAssignment(top1, 1),
                LaneAssignment(top2, 0),
            ),
            result,
        )
    }

    @Test
    fun reverseSharesScrollPool() {
        val s = scheduler(allowOverlap = false)
        val scroll = ev(0, mode = DanmakuMode.SCROLL, index = 0)
        val reverse = ev(1000, mode = DanmakuMode.REVERSE, index = 1)
        // REVERSE 与 SCROLL 共用滚动轨道：t=1000 时 lane0 未释放 → lane1
        val result = s.schedule(listOf(scroll, reverse))
        assertEquals(listOf(0, 1), result.map { it.lane })
    }

    @Test
    fun specialNotScheduled() {
        val s = scheduler()
        val result = s.schedule(listOf(ev(0, mode = DanmakuMode.SPECIAL), ev(1, mode = DanmakuMode.CODE)))
        assertEquals(2, result.size)
        assertTrue(result.all { it.lane == DanmakuLaneScheduler.NO_LANE })
    }

    @Test
    fun schedule_sortsInputByTime() {
        val s = scheduler(allowOverlap = false)
        val result = s.schedule(listOf(ev(2000, index = 2), ev(0, index = 0), ev(1000, index = 1)))
        assertEquals(listOf(0L, 1000L, 2000L), result.map { it.event.timeMs })
    }

    @Test
    fun invalidParamsRejected() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            DanmakuLaneScheduler(laneWidthPx = 0, speedPxPerSec = 600f)
        }
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            DanmakuLaneScheduler(laneWidthPx = 1000, speedPxPerSec = 0f)
        }
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            DanmakuLaneScheduler(laneWidthPx = 1000, speedPxPerSec = 600f, fixedDurationMs = 0L)
        }
    }
}
