package com.qx.orbit.core.danmaku

/**
 * 弹幕轨道分配与避让调度（确定性、可测的纯逻辑）。
 *
 * 模型（参考 DFM/DFMNext 的 DanmakusRetainer 轨道概念，简化为"行释放时间"模型）：
 * - 每类弹幕独立轨道池：滚动（SCROLL/REVERSE 共用）、顶部、底部，互不干扰；
 * - 每条弹幕占用一条轨道，占用期 = [timeMs, timeMs + durationMs)：
 *   - 滚动：duration = (轨道宽 + 估算文本宽) / 速度，即整条完全滑出屏幕所需时长；
 *   - 顶部/底部：固定停留时长 [fixedDurationMs]；
 * - `allowOverlap = false` 时做避让：新弹幕优先进入"释放时间 <= 当前时刻"的第一条轨道，
 *   否则开新轨；轨道数达 [maxLines] 后按 [overflowPolicy] 丢弃或选择最早释放轨道；
 * - `allowOverlap = true` 时不做避让，按轨道数轮转分配，永不丢弃；
 * - SPECIAL/CODE/BAS/UNKNOWN 不参与轨道调度，返回 lane = -1。
 *
 * 输入事件先按 (timeMs, sourceIndex) 稳定排序，输出与输入无依赖，结果确定可复现。
 * 轨道索引仅表示第几条轨道：SCROLL/TOP 自顶部起数，BOTTOM 自底部起数，由渲染端换算 y 坐标。
 */
class DanmakuLaneScheduler(
    /** 弹幕区宽度（px），用于计算滚动时长。 */
    val laneWidthPx: Int,
    /** 滚动速度（px/秒）。 */
    val speedPxPerSec: Float,
    /** 每类弹幕最大轨道数；<= 0 表示不限。 */
    val maxLines: Int = 0,
    /** 允许重叠：true 时不避让，轨道轮转分配。 */
    val allowOverlap: Boolean = true,
    /** 顶部/底部固定弹幕的停留时长（毫秒）。 */
    val fixedDurationMs: Long = 4_000L,
    /** 轨道占满时的处理策略。 */
    val overflowPolicy: LaneOverflowPolicy = LaneOverflowPolicy.DROP,
    /** 文本宽度估算（默认：字符数 × 字号）。 */
    val textWidthEstimator: (DanmakuEvent) -> Int = { e -> (e.text.length * e.fontSize).toInt() },
) {

    init {
        require(laneWidthPx > 0) { "laneWidthPx must be positive" }
        require(speedPxPerSec > 0f) { "speedPxPerSec must be positive" }
        require(fixedDurationMs > 0L) { "fixedDurationMs must be positive" }
    }

    /**
     * 调度一批事件，返回按时间序的 [LaneAssignment]。
     * `overflowPolicy == DROP` 且轨道占满时对应事件不出现在结果中。
     */
    fun schedule(events: List<DanmakuEvent>): List<LaneAssignment> {
        val sorted = events.sortedWith(compareBy<DanmakuEvent> { it.timeMs }.thenBy { it.sourceIndex })
        val scrollPool = LanePool(laneCount())
        val topPool = LanePool(laneCount())
        val bottomPool = LanePool(laneCount())
        val result = ArrayList<LaneAssignment>(sorted.size)
        for (e in sorted) {
            val laneClass = LaneClass.of(e.mode)
            if (laneClass == null) {
                result.add(LaneAssignment(e, NO_LANE))
                continue
            }
            val pool = when (laneClass) {
                LaneClass.SCROLL -> scrollPool
                LaneClass.TOP -> topPool
                LaneClass.BOTTOM -> bottomPool
            }
            val duration = if (laneClass == LaneClass.SCROLL) scrollDurationMs(e) else fixedDurationMs
            val lane = pool.assign(e.timeMs, duration, allowOverlap, overflowPolicy)
            if (lane >= 0) result.add(LaneAssignment(e, lane))
        }
        return result
    }

    private fun laneCount(): Int = if (maxLines <= 0) Int.MAX_VALUE else maxLines

    /** 滚动弹幕完全滑出屏幕所需时长（毫秒）。 */
    fun scrollDurationMs(e: DanmakuEvent): Long {
        val width = textWidthEstimator(e).coerceAtLeast(1)
        val totalPx = (laneWidthPx + width).toFloat()
        return (totalPx / speedPxPerSec * 1000f).toLong()
    }

    private class LanePool(private val capacity: Int) {

        private val releaseTimes = ArrayList<Long>()
        private var roundRobin = 0

        /** 返回分配的轨道索引；避让模式下无可分配轨道且策略为 DROP 时返回 -1。 */
        fun assign(
            startMs: Long,
            durationMs: Long,
            allowOverlap: Boolean,
            policy: LaneOverflowPolicy,
        ): Int {
            if (allowOverlap) {
                val lane = roundRobin % capacity
                roundRobin++
                ensure(lane)
                return lane
            }
            // 避让模式：优先复用已释放的轨道（按索引序，确定性强）
            for (i in releaseTimes.indices) {
                if (releaseTimes[i] <= startMs) {
                    releaseTimes[i] = startMs + durationMs
                    return i
                }
            }
            if (releaseTimes.size < capacity) {
                val lane = releaseTimes.size
                releaseTimes.add(startMs + durationMs)
                return lane
            }
            // 轨道已满
            return when (policy) {
                LaneOverflowPolicy.PICK_EARLIEST -> {
                    var best = 0
                    for (i in 1 until releaseTimes.size) {
                        if (releaseTimes[i] < releaseTimes[best]) best = i
                    }
                    releaseTimes[best] = startMs + durationMs
                    best
                }
                LaneOverflowPolicy.DROP -> -1
            }
        }

        private fun ensure(lane: Int) {
            while (releaseTimes.size <= lane) releaseTimes.add(Long.MIN_VALUE)
        }
    }

    companion object {
        /** 非轨道类弹幕（SPECIAL/CODE/BAS/UNKNOWN）的 lane 值。 */
        const val NO_LANE = -1
    }
}

/** 轨道占满时的处理策略。 */
enum class LaneOverflowPolicy {
    /** 丢弃该条弹幕（不出现在调度结果中）。 */
    DROP,

    /** 强行放入释放最早的轨道（可能产生轻微重叠）。 */
    PICK_EARLIEST,
}

/** 轨道类别。SCROLL/REVERSE 共用滚动轨道，FIX_TOP/FIX_BOTTOM 各自独立。 */
enum class LaneClass {
    SCROLL, TOP, BOTTOM;

    companion object {
        /** 返回弹幕对应的轨道类别；SPECIAL/CODE/BAS/UNKNOWN 返回 null（不参与轨道调度）。 */
        fun of(mode: DanmakuMode): LaneClass? = when (mode) {
            DanmakuMode.SCROLL, DanmakuMode.REVERSE -> SCROLL
            DanmakuMode.FIX_TOP -> TOP
            DanmakuMode.FIX_BOTTOM -> BOTTOM
            DanmakuMode.SPECIAL, DanmakuMode.CODE, DanmakuMode.BAS, DanmakuMode.UNKNOWN -> null
        }
    }
}

/** 调度结果：事件 + 分配的轨道索引（[DanmakuLaneScheduler.NO_LANE] 表示不参与轨道调度）。 */
data class LaneAssignment(val event: DanmakuEvent, val lane: Int)
