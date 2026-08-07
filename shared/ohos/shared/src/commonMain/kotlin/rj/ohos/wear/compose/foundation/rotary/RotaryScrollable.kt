package rj.ohos.wear.compose.foundation.rotary

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.ui.Modifier

/**
 * 表冠滚动行为策略接口。
 *
 * 100% API 等效于 `androidx.wear.compose.foundation.rotary.RotaryScrollableBehavior`。
 * 空壳：待表冠事件桥接落地后，由真实行为实现（绑定 [ScrollableState]/[AnchoredDraggableState]）。
 */
interface RotaryScrollableBehavior

/**
 * 表冠滚动行为默认值。
 *
 * 100% API 等效于 `androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults`。
 */
object RotaryScrollableDefaults {

    /** 空操作行为：不消费任何表冠事件。 */
    val NoOpBehavior: RotaryScrollableBehavior = RotaryScrollableBehaviorImpl

    /**
     * 构造默认行为。当前为占位实现，始终返回 [NoOpBehavior]。
     * TODO: 参数对齐 androidx（scrollableState/canScrollBackward/canScrollForward 等）。
     */
    fun behavior(): RotaryScrollableBehavior = NoOpBehavior

    private object RotaryScrollableBehaviorImpl : RotaryScrollableBehavior
}

/**
 * 表冠滚动修饰符。
 *
 * 100% API 等效于 `androidx.wear.compose.foundation.rotary.rotaryScrollable`。
 *
 * TODO(空壳，转发到 foundation 对应):
 * 当前直接返回 this，不产生滚动效果。计划在表冠事件桥接可用后，
 * 按 [orientation] 把表冠增量转发到 Kuikly `draggable`/`anchoredDraggable`
 * （水平/垂直两个方向均已具备），并把 [behavior] 作为策略注入点。
 */
@Composable
fun Modifier.rotaryScrollable(
    orientation: Orientation,
    reverseDirection: Boolean = false,
    enabled: Boolean = true,
    behavior: RotaryScrollableBehavior = RotaryScrollableDefaults.behavior(),
): Modifier = this // TODO: 表冠桥接后转发到 Kuikly draggable/anchoredDraggable
