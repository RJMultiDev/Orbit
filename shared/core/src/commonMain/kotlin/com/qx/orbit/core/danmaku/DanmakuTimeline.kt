package com.qx.orbit.core.danmaku

/**
 * 弹幕时间轴：按时间点切片/构建索引的纯逻辑。
 *
 * 构建时对输入做稳定排序（timeMs 升序，相等时按 [DanmakuEvent.sourceIndex]），
 * 之后通过二分查找在 O(log n) 内完成任意时间窗切片。
 */
class DanmakuTimeline private constructor(
    /** 按 timeMs 升序、同刻按 sourceIndex 稳定排序的事件。 */
    private val events: List<DanmakuEvent>,
) {

    val size: Int get() = events.size

    /** 首条弹幕时间；空时间轴为 0。 */
    val startTimeMs: Long get() = events.firstOrNull()?.timeMs ?: 0L

    /** 末条弹幕时间；空时间轴为 0。 */
    val endTimeMs: Long get() = events.lastOrNull()?.timeMs ?: 0L

    fun isEmpty(): Boolean = events.isEmpty()

    /** 取全部事件（已按时间排序）。 */
    fun all(): List<DanmakuEvent> = events

    /**
     * 返回 [startMs, endMs) 内的弹幕（半开区间），按时间顺序。
     * `endMs <= startMs` 或区间无弹幕时返回空列表。
     */
    fun slice(startMs: Long, endMs: Long): List<DanmakuEvent> {
        if (events.isEmpty() || endMs <= startMs) return emptyList()
        val lo = lowerBound(startMs)
        val hi = lowerBound(endMs)
        if (lo >= hi) return emptyList()
        return events.subList(lo, hi)
    }

    /** 以 [centerMs] 为中心、半宽 [halfWidthMs] 的时间窗内的弹幕。 */
    fun window(centerMs: Long, halfWidthMs: Long): List<DanmakuEvent> =
        slice(centerMs - halfWidthMs, centerMs + halfWidthMs)

    /**
     * 按固定段长切分索引：返回「段起点(ms) → 该段事件的下标区间 [startIndex, endIndex)」。
     * 空时间轴或 [segmentSizeMs] <= 0 时返回空 Map。返回的区间可直接用于 [all] 取子集。
     */
    fun segments(segmentSizeMs: Long): Map<Long, IntRange> {
        if (events.isEmpty() || segmentSizeMs <= 0) return emptyMap()
        val result = LinkedHashMap<Long, IntRange>()
        var i = 0
        while (i < events.size) {
            val segStart = events[i].timeMs / segmentSizeMs * segmentSizeMs
            var j = i + 1
            while (j < events.size && events[j].timeMs / segmentSizeMs * segmentSizeMs == segStart) j++
            result[segStart] = i until j
            i = j
        }
        return result
    }

    /** 首个 timeMs >= 入参的下标（lower_bound）。 */
    private fun lowerBound(timeMs: Long): Int {
        var lo = 0
        var hi = events.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (events[mid].timeMs < timeMs) lo = mid + 1 else hi = mid
        }
        return lo
    }

    companion object {
        /**
         * 由任意无序输入构建时间轴。排序稳定且确定：timeMs 升序，同刻保持输入顺序。
         */
        fun build(events: Iterable<DanmakuEvent>): DanmakuTimeline {
            val sorted = events
                .toList()
                .sortedWith(compareBy<DanmakuEvent> { it.timeMs }.thenBy { it.sourceIndex })
            return DanmakuTimeline(sorted)
        }
    }
}
