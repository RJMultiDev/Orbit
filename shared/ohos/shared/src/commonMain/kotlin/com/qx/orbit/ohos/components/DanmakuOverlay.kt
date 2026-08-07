package com.qx.orbit.ohos.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.data.DanmakuFilter
import com.qx.orbit.ohos.data.DanmakuFilterConfig
import com.qx.orbit.ohos.data.DanmakuItem
import com.tencent.kuikly.compose.foundation.layout.BoxWithConstraints
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clipToBounds
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.TimeSource

/**
 * 弹幕渲染层：固定高度 Box 内按播放时钟线性右→左滚动。
 *
 * 对齐 Android util/danmaku/base/DanmakuPlayer 的语义（VOD 场景）：
 * - [playing] = pause/resume：false 时冻结时钟（内部时钟停止推进）；
 * - [positionMs] = seek/校准：变化时内部时钟复位到该值；
 * - 弹幕按时间过滤（`timeMs <= 时钟 < timeMs + 滚动时长` 才渲染），
 *   位置为播放时间的线性函数，seek 后自动跳到对应位置。
 *
 * 轨道分配参考 shared/core danmaku/DanmakuLaneScheduler：固定 [laneCount] 条滚动轨道，
 * 轨道满即丢弃（DROP 策略），按「轨道释放时间」避让，保证同轨不重叠。
 *
 * 滚动动画为简化实现：播放时钟驱动 recomposition 逐帧重算 Offset
 * （`Modifier.offset { IntOffset(xPx, yPx) }`，Kuikly 已确认支持）。
 * TODO(待验证)：Kuikly 帧回调能力 / 高频 recomposition 在真机手表的开销；
 * 后续可改为 Canvas/XComponent 单层渲染。
 */
@Composable
fun DanmakuOverlay(
    danmaku: List<DanmakuItem>,
    positionMs: Long,
    playing: Boolean,
    modifier: Modifier = Modifier,
    filterConfig: DanmakuFilterConfig = DanmakuFilterConfig.NONE,
    laneCount: Int = DEFAULT_LANE_COUNT,
    speedDpPerSec: Float = DEFAULT_SPEED_DP_PER_SEC,
    laneHeight: Dp = DEFAULT_LANE_HEIGHT_DP,
) {
    val overlayHeight = (laneHeight.value * laneCount).dp

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(overlayHeight).clipToBounds(),
    ) {
        val laneWidthDp = maxWidth

        // 屏蔽 + 时间稳定排序 + 固定行轨道分配（与播放时钟无关，缓存之）
        val filtered = remember(danmaku, filterConfig) { DanmakuFilter.filter(danmaku, filterConfig) }
        val prepared = remember(filtered, laneCount, laneWidthDp) {
            prepareDanmaku(filtered, laneCount, laneWidthDp.value, speedDpPerSec)
        }

        // 播放时钟：playing 推进 / 停止冻结；positionMs 变化（seek/外部驱动）即校准。
        var displayMs by remember { mutableStateOf(positionMs) }
        LaunchedEffect(playing, positionMs) {
            displayMs = positionMs
            if (playing) {
                while (true) {
                    val tick = TimeSource.Monotonic.markNow()
                    delay(FRAME_INTERVAL_MS)
                    displayMs += tick.elapsedNow().inWholeMilliseconds
                }
            }
        }

        // 当前时刻活跃弹幕：`timeMs <= now < timeMs + durationMs` 才可见。
        val now = displayMs
        for (entry in prepared) {
            val timeMs = entry.timeMs
            if (now < timeMs || now >= timeMs + entry.durationMs) continue
            val lane = entry.lane
            if (lane < 0) continue

            val elapsedSec = (now - timeMs).toFloat() / 1000f
            val xDp = laneWidthDp - (elapsedSec * speedDpPerSec).dp
            val yDp = (laneHeight.value * lane).dp
            key(entry.item) {
                Text(
                    text = entry.item.text,
                    fontSize = (entry.item.fontSize / 2).sp,
                    // 模型 color 可能是十进制 RGB（无 alpha）或已补全 ARGB，统一置为不透明。
                    color = Color((entry.item.color and 0x00FFFFFFL) or 0xFF000000L),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset { IntOffset(xDp.toPx().roundToInt(), yDp.toPx().roundToInt()) },
                )
            }
        }
    }
}

/** 默认滚动轨道数。 */
private const val DEFAULT_LANE_COUNT = 4
/** 默认滚动速度（dp/秒）。 */
private const val DEFAULT_SPEED_DP_PER_SEC = 60f
/** 默认轨道高度（dp）。 */
private val DEFAULT_LANE_HEIGHT_DP = 16.dp
/** 内部时钟节拍（毫秒，约 60fps）。 */
private const val FRAME_INTERVAL_MS = 16L

/** 轨道分配结果：弹幕 + 轨道索引 + 进入/滚动时长（毫秒）。 */
private data class PreparedDanmaku(
    val item: DanmakuItem,
    val lane: Int,
    val timeMs: Long,
    val durationMs: Long,
)

/**
 * 固定行数轨道分配（移植 shared/core DanmakuLaneScheduler.LanePool 的避让分配）：
 * - 按 (time, 输入序) 稳定排序；
 * - 每条弹幕占用一条轨道，占用期 = [timeMs, timeMs + durationMs)；
 * - 优先复用「释放时间 <= 当前时刻」的第一条轨道，否则开新轨；
 * - 轨道满（[laneCount]）即丢弃该条（DROP 策略，不参与渲染）。
 * 文本宽度估算：字符数 × 字号（dp 单位，与渲染滚动速度同尺度）。
 */
private fun prepareDanmaku(
    items: List<DanmakuItem>,
    laneCount: Int,
    laneWidthDp: Float,
    speedDpPerSec: Float,
): List<PreparedDanmaku> {
    val sorted = items.mapIndexed { index, item -> item to index }
        .sortedWith(compareBy({ it.first.time }, { it.second }))

    val laneRelease = ArrayList<Long>(laneCount)
    val result = ArrayList<PreparedDanmaku>(sorted.size)
    for ((item, _) in sorted) {
        val timeMs = (item.time * 1000.0).toLong()
        val textWidthDp = (item.fontSize * item.text.length).coerceAtLeast(1)
        // 完全滑出屏幕所需时长：Dp 尺度的 (轨道宽 + 文本宽) / 速度
        val durationMs = (((laneWidthDp + textWidthDp) / speedDpPerSec) * 1000f).toLong()

        var lane = -1
        for (l in laneRelease.indices) {
            if (laneRelease[l] <= timeMs) {
                lane = l
                break
            }
        }
        if (lane < 0 && laneRelease.size < laneCount) {
            lane = laneRelease.size
            laneRelease.add(0L)
        }
        if (lane >= 0) {
            laneRelease[lane] = timeMs + durationMs
            result.add(PreparedDanmaku(item, lane, timeMs, durationMs))
        }
    }
    return result
}
