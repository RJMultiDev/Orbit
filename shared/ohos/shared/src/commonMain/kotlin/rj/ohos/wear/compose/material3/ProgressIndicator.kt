package rj.ohos.wear.compose.material3

import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.material3.CircularProgressIndicator as KuiklyCircularProgressIndicator
import com.tencent.kuikly.compose.material3.LinearProgressIndicator as KuiklyLinearProgressIndicator
import com.tencent.kuikly.compose.material3.MaterialTheme
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import androidx.compose.runtime.Composable

/**
 * 进度指示组件（对齐 `androidx.compose.material3` / `androidx.wear.compose.material3` 的
 * LinearProgressIndicator / CircularProgressIndicator API）。
 *
 * Kuikly 支撑情况：com.tencent.kuikly.compose.material3 已原生提供
 * [KuiklyLinearProgressIndicator] 与 [KuiklyCircularProgressIndicator]（含 determinate /
 * indeterminate 全部重载），此处直接包装，未做自绘。
 */

private const val ProgressDefaultStrokeWidth = 4.0f

/**
 * 进度指示默认参数。
 *
 * 对应 androidx：
 * - `androidx.compose.material3.ProgressIndicatorDefaults`
 * - `androidx.wear.compose.material3.ProgressIndicatorDefaults`
 */
object ProgressIndicatorDefaults {
    /** 圆形进度条描边宽度（dp），对应 androidx 的 CircularStrokeWidth。 */
    val CircularStrokeWidth: Dp = ProgressDefaultStrokeWidth.dp

    /** 线性进度条高度（dp）。 */
    val LinearStrokeWidth: Dp = ProgressDefaultStrokeWidth.dp

    /** 默认激活色。 */
    @Composable
    fun circularColor(): Color = MaterialTheme.colorScheme.primary

    /** 线性进度条默认激活色。 */
    @Composable
    fun linearColor(): Color = MaterialTheme.colorScheme.primary
}

/**
 * 圆形进度指示器（determinate，progress ∈ [0, 1]）。
 *
 * 对应 androidx：`androidx.compose.material3.CircularProgressIndicator(progress, modifier,
 * color, strokeWidth, trackColor, strokeCap)`；
 * wear m3 中与之对应的是
 * `androidx.wear.compose.material3.CircularProgressIndicator(progress, modifier, colors, strokeWidth)`。
 *
 * Kuikly 包装对象：`com.tencent.kuikly.compose.material3.CircularProgressIndicator(progress,
 * modifier, color, strokeWidth, trackColor, strokeCap)`。
 */
@Composable
fun CircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeCap: StrokeCap = StrokeCap.Butt,
) {
    KuiklyCircularProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        strokeWidth = strokeWidth,
        trackColor = trackColor,
        strokeCap = strokeCap,
    )
}

/**
 * 圆形进度指示器（indeterminate，自转动画）。
 *
 * 对应 androidx：`androidx.compose.material3.CircularProgressIndicator(modifier, color,
 * strokeWidth, trackColor, strokeCap)`。
 */
@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeCap: StrokeCap = StrokeCap.Butt,
) {
    KuiklyCircularProgressIndicator(
        modifier = modifier,
        color = color,
        strokeWidth = strokeWidth,
        trackColor = trackColor,
        strokeCap = strokeCap,
    )
}

/**
 * 线性进度指示器（determinate，progress ∈ [0, 1]）。
 *
 * 对应 androidx：`androidx.compose.material3.LinearProgressIndicator(progress, modifier,
 * color, trackColor, strokeCap)`；
 * wear m3 中对应
 * `androidx.wear.compose.material3.LinearProgressIndicator(progress, modifier, colors, strokeWidth)`。
 *
 * Kuikly 包装对象：`com.tencent.kuikly.compose.material3.LinearProgressIndicator(progress,
 * modifier, color, trackColor, strokeCap)`。
 */
@Composable
fun LinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeCap: StrokeCap = StrokeCap.Butt,
) {
    KuiklyLinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeCap = strokeCap,
    )
}

/**
 * 线性进度指示器（indeterminate，滑动动画）。
 *
 * 对应 androidx：`androidx.compose.material3.LinearProgressIndicator(modifier, color,
 * trackColor, strokeCap)`。
 */
@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeCap: StrokeCap = StrokeCap.Butt,
) {
    KuiklyLinearProgressIndicator(
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeCap = strokeCap,
    )
}

/**
 * 通用进度指示器（determinate 圆形）。
 *
 * 对应 androidx：`androidx.compose.material3.ProgressIndicator`（旧版 API，
 * 后更名为 CircularProgressIndicator）；wear m3 1.x 中的
 * `androidx.wear.compose.material3.ProgressIndicator` 亦为圆形指示器。
 *
 * 此 API 仅为迁移兼容而保留，内部委托给 [CircularProgressIndicator]。
 */
@Deprecated(
    message = "ProgressIndicator 已更名为 CircularProgressIndicator，请使用 CircularProgressIndicator",
    replaceWith = ReplaceWith("CircularProgressIndicator(progress, modifier, color, strokeWidth, trackColor, strokeCap)"),
)
@Composable
fun ProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeCap: StrokeCap = StrokeCap.Butt,
) {
    CircularProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        strokeWidth = strokeWidth,
        trackColor = trackColor,
        strokeCap = strokeCap,
    )
}

/**
 * 占位提示（非 API）：当 determinate 圆形进度需要整圆底轨但 Kuikly 不支持时使用。
 *
 * 当前 Kuikly 原生 CircularProgressIndicator 已支持 trackColor 底轨绘制，
 * 因此本项目未启用该占位实现。保留仅供文档对照。
 */
@Composable
private fun CircularProgressFallbackPlaceholder(
    progress: Float,
    modifier: Modifier,
    color: Color,
    strokeWidth: Dp,
    trackColor: Color,
) {
    Box(modifier = modifier) {
        Text(
            text = "TODO",
            color = color,
            fontSize = 10.sp,
            modifier = Modifier,
        )
        Text(
            text = "${(progress * 100).toInt()}%",
            color = trackColor,
            fontSize = 10.sp,
            modifier = Modifier,
        )
    }
}
