package rj.ohos.wear.compose.foundation

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.Modifier

/**
 * 表冠滚动事件。
 *
 * 100% API 等效于 `androidx.wear.compose.foundation.RotaryScrollEvent`：
 * [delta] 为本次表冠旋转产生的滚动增量（像素/逻辑单位），正方向约定与 Android 一致。
 */
data class RotaryScrollEvent(val delta: Float)

/**
 * 表冠滚动回调修饰符。
 *
 * 100% API 等效于 `androidx.wear.compose.foundation.onRotaryScrollEvent`。
 *
 * TODO(ArkTS 表冠事件桥接计划):
 * Kuikly compose 当前未暴露表冠（Digital Crown）输入流，暂无对应 Modifier。
 * 计划经 ArkTS 侧 `@ohos.multimodalInput` 表冠事件桥接到 Kuikly 容器后，
 * 将旋转增量派发到 [callback] 并按其返回值决定是否消费事件。
 * 当前为空壳实现：直接返回 this，回调不会被调用。
 */
@Composable
fun Modifier.onRotaryScrollEvent(
    callback: (RotaryScrollEvent) -> Boolean,
): Modifier = this // TODO: ArkTS 表冠事件桥接落地后接入真实事件流
