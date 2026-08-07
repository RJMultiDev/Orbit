package rj.ohos.wear.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.components.formatHm
import com.qx.orbit.ohos.util.TimeUtil
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.RowScope
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.wrapContentWidth
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.shadow
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Shape
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 手表顶部时间组件，对齐 `androidx.wear.compose.material3.TimeText`。
 *
 * 对应 androidx 签名：
 * ```
 * TimeText(
 *     modifier: Modifier = Modifier,
 *     start: @Composable RowScope.() -> Unit = {},
 *     timeText: @Composable RowScope.() -> Unit = { TimeTextDefaults.TimeText() },
 *     end: @Composable RowScope.() -> Unit = {},
 *     timeSource: TimeSource = TimeSource.SystemTimeSource,
 *     timeTextStyle: TextStyle = TimeTextDefaults.timeTextStyle,
 *     containerColor: Color = TimeTextDefaults.primaryContainerColor,
 *     contentColor: Color = TimeTextDefaults.primaryContentColor,
 *     shadow: Boolean = TimeTextDefaults.shadow,
 *     shadowElevation: Dp = TimeTextDefaults.shadowElevation
 * )
 * ```
 *
 * 差异：
 * - **timeText 参数位置**：androidx 中 timeText 声明在 timeSource 之前，默认实现无法引用
 *   timeSource/timeTextStyle/contentColor；本移植将其置于参数末尾，使默认实现可以直接使用这些参数。
 *   命名参数调用与 androidx 完全兼容。
 * - **时间来源**：androidx 的 [TimeSource] 返回 `ZonedDateTime`；本移植为纯 Kotlin 无
 *   `java.time`，[TimeSource] 返回 `nowMillis(): Long`（毫秒），默认 [TimeSource.SystemTimeSource]
 *   基于 [TimeUtil.nowMillis]。
 * - **时间格式**：默认显示 "HH:mm"，走 `com.qx.orbit.ohos.components.formatHm`（Kuikly
 *   CalendarModule 桥，设备本地时区），禁止 `java.text.SimpleDateFormat`。
 * - **刷新频率**：androidx 在分钟边界刷新；本移植同样对齐到下一分钟边界（KDoc 要求每分钟刷新）。
 * - **容器**：androidx 用 `contentWindowInsets`/安全区做避让；Kuikly 的 `WindowInsets` 在 OHOS
 *   上无可靠取值，这里用固定圆角胶囊 + 内边距替代（见 [TimeTextDefaults.shape]/[TimeTextDefaults.paddingValues]）。
 */
@Composable
fun TimeText(
    modifier: Modifier = Modifier,
    start: @Composable RowScope.() -> Unit = {},
    end: @Composable RowScope.() -> Unit = {},
    timeSource: TimeSource = TimeSource.SystemTimeSource,
    timeTextStyle: TextStyle = TimeTextDefaults.timeTextStyle,
    containerColor: Color = TimeTextDefaults.primaryContainerColor,
    contentColor: Color = TimeTextDefaults.primaryContentColor,
    shadow: Boolean = TimeTextDefaults.shadow,
    shadowElevation: Dp = TimeTextDefaults.shadowElevation,
    timeText: @Composable RowScope.() -> Unit = {
        TimeTextDefaults.TimeText(
            timeSource = timeSource,
            timeTextStyle = timeTextStyle,
            contentColor = contentColor,
            modifier = Modifier.weight(1f),
        )
    },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (shadow) Modifier.shadow(shadowElevation, TimeTextDefaults.shape) else Modifier)
            .background(containerColor, TimeTextDefaults.shape)
            .padding(TimeTextDefaults.paddingValues),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        start()
        timeText()
        end()
    }
}

/**
 * 时间来源。对齐 `androidx.wear.compose.material3.TimeSource`。
 *
 * 差异：androidx 的 `TimeSource.now()` 返回 `ZonedDateTime`（依赖 java.time）；本移植
 * `nowMillis()` 返回 Unix 毫秒时间戳，默认实现基于 [TimeUtil.nowMillis]（posix gettimeofday）。
 */
fun interface TimeSource {
    /** 当前时间（Unix 毫秒）。 */
    fun nowMillis(): Long

    companion object {
        /** 系统时间源：Kuikly/OHOS 无 java.time，走 [TimeUtil.nowMillis]。 */
        val SystemTimeSource: TimeSource = TimeSource { TimeUtil.nowMillis() }
    }
}

/**
 * TimeText 默认值，对齐 `androidx.wear.compose.material3.TimeTextDefaults`。
 *
 * 差异：
 * - androidx 的 `primaryContainerColor`/`primaryContentColor` 读取 MaterialTheme 色彩；
 *   这里为自包含的暗色常量（与 com.qx.orbit.ohos.theme.OrbitColors 的暗色面板一致），
 *   不依赖调用方主题。
 * - `timeTextStyle` 为 11sp 粗体（近似 androidx labelSmall + 白字），
 *   Kuikly 无 `Typography.labelSmall` 的直接等价、直接给样式常量。
 * - `shadow` 默认 true（androidx 一致），OHOS 上阴影能力取决于 Kuikly 运行时，降级为无阴影。
 */
object TimeTextDefaults {

    /** 时间文本样式。 */
    val timeTextStyle: TextStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
    )

    /** 容器底色（对齐 OrbitColors.SurfaceContainer 的暗色面板）。 */
    val primaryContainerColor: Color = Color(0xFF261D1F)

    /** 时间文字颜色。 */
    val primaryContentColor: Color = Color(0xFFEFDFE1)

    /** 是否绘制阴影（androidx 默认 true）。 */
    val shadow: Boolean = true

    /** 阴影高度。 */
    val shadowElevation: Dp = 4.dp

    /** 胶囊形容器（50% 圆角）。 */
    val shape: Shape = RoundedCornerShape(50f)

    /** 固定内边距（替代 androidx 的 insets 避让）。 */
    val paddingValues: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 4.dp)

    /** 一分钟的毫秒数。 */
    internal const val MinuteMillis: Long = 60_000L

    /**
     * 默认时间文本槽位，对齐 `androidx.wear.compose.material3.TimeTextDefaults.TimeText`。
     *
     * 每分钟对齐到分钟边界刷新：先立即显示当前时间，随后 `delay` 到下一个整分再更新。
     */
    @Composable
    fun TimeText(
        timeSource: TimeSource = TimeSource.SystemTimeSource,
        timeTextStyle: TextStyle = TimeTextDefaults.timeTextStyle,
        contentColor: Color = TimeTextDefaults.primaryContentColor,
        modifier: Modifier = Modifier,
    ) {
        var nowMillis by remember { mutableStateOf(timeSource.nowMillis()) }
        LaunchedEffect(timeSource) {
            while (true) {
                nowMillis = timeSource.nowMillis()
                val nextMinuteBoundary = (nowMillis / MinuteMillis + 1) * MinuteMillis
                delay(nextMinuteBoundary - nowMillis)
            }
        }
        Text(
            text = formatHm(nowMillis),
            style = timeTextStyle,
            color = contentColor,
            modifier = modifier.wrapContentWidth(Alignment.CenterHorizontally),
        )
    }
}
