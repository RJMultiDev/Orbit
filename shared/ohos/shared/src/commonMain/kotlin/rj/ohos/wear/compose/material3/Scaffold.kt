package rj.ohos.wear.compose.material3

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.ScrollState
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.BoxScope
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.rememberScrollState
import com.tencent.kuikly.compose.foundation.shape.RectangleShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 手表页面骨架，对齐 `androidx.wear.compose.material3.Scaffold`。
 *
 * 对应 androidx 签名：
 * ```
 * Scaffold(
 *     modifier: Modifier = Modifier,
 *     timeText: @Composable () -> Unit = { TimeText() },
 *     vignette: @Composable (VignettePosition) -> Unit = { Vignette(position = it) },
 *     positionIndicator: @Composable (ScaffoldDefaults.PositionIndicatorState) -> Unit = {
 *         PositionIndicator(scrollState = it.scrollState)
 *     },
 *     content: @Composable BoxScope.() -> Unit
 * )
 * ```
 *
 * 差异：
 * - **WindowInsets**：androidx 依赖 `contentWindowInsets` 做安全区避让；Kuikly 的
 *   `WindowInsets` 在 OHOS 上取值不可靠，这里用固定内边距
 *   [ScaffoldDefaults.contentPadding] 替代（默认左 16dp、右侧留出滚动指示条 32dp）。
 * - **vignette / timeText / positionIndicator 槽位**与 androidx 一致；[PositionIndicator]
 *   在 Kuikly 下退化为矩形条（见 [PositionIndicator]）。
 */
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    timeText: @Composable () -> Unit = { TimeText() },
    vignette: @Composable (VignettePosition) -> Unit = { Vignette(position = it) },
    positionIndicator: @Composable (ScaffoldDefaults.PositionIndicatorState) -> Unit = {
        PositionIndicator(scrollState = it.scrollState)
    },
    content: @Composable BoxScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxSize()) {
        // 主体内容
        Box(modifier = Modifier.fillMaxSize().padding(ScaffoldDefaults.contentPadding)) {
            content()
        }
        // 上下渐晕（保证圆屏上下文字可读性）
        vignette(VignettePosition.TopAndBottom)
        // 顶部时间
        Box(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            timeText()
        }
        // 右侧滚动指示条
        Box(modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd)) {
            positionIndicator(ScaffoldDefaults.PositionIndicatorState(scrollState = scrollState))
        }
    }
}

/**
 * Scaffold 默认值，对齐 `androidx.wear.compose.material3.ScaffoldDefaults`。
 *
 * 差异：androidx 的 `contentWindowInsets` 在此移除（Kuikly insets 不可靠），
 * 改为固定 [contentPadding]。
 */
object ScaffoldDefaults {

    /**
     * 滚动位置状态，对齐 `androidx.wear.compose.material3.ScaffoldDefaults.PositionIndicatorState`。
     * 由 [Scaffold] 内部持有 [rememberScrollState]；调用方也可自建 [ScrollState] 并
     * 通过 `positionIndicator` 槽位传入（与 androidx 用法一致）。
     */
    class PositionIndicatorState(
        val scrollState: ScrollState,
        val reversed: Boolean = false,
    )

    /** 内容固定内边距：左 16dp、右侧为滚动指示条让出 32dp。 */
    val contentPadding: PaddingValues = PaddingValues(16.dp, 0.dp, 32.dp, 0.dp)
}

/**
 * 渐晕位置，对齐 `androidx.wear.compose.material3.VignettePosition`。
 */
enum class VignettePosition { Top, Bottom, TopAndBottom }

/**
 * 边缘渐晕，对齐 `androidx.wear.compose.material3.Vignette`。
 *
 * 差异：androidx 默认的 `brushFactory` 基于屏幕形状/密度计算径向渐晕；Kuikly 只有
 * `Brush.verticalGradient`，这里实现为上下两端的线性渐变（见 [VignetteDefaults.brush]）。
 */
@Composable
fun Vignette(
    position: VignettePosition = VignettePosition.TopAndBottom,
    modifier: Modifier = Modifier,
    brushFactory: (VignettePosition) -> Brush = { pos -> VignetteDefaults.brush(pos) },
) {
    Box(modifier = modifier.fillMaxSize().background(brushFactory(position), RectangleShape))
}

/**
 * Vignette 默认值，对齐 `androidx.wear.compose.material3.VignetteDefaults`。
 */
object VignetteDefaults {
    /** 渐晕颜色（黑，40% 透明度，近似 androidx 默认）。 */
    val vignetteColor: Color = Color.Black.copy(alpha = 0.4f)

    /** 依据位置构造垂直线性渐变。 */
    fun brush(position: VignettePosition): Brush {
        val transparent = Color.Transparent
        return when (position) {
            VignettePosition.Top -> Brush.verticalGradient(listOf(vignetteColor, transparent))
            VignettePosition.Bottom -> Brush.verticalGradient(listOf(transparent, vignetteColor))
            VignettePosition.TopAndBottom ->
                Brush.verticalGradient(listOf(vignetteColor, transparent, vignetteColor))
        }
    }
}
