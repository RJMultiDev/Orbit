package com.qx.orbit.ohos.util

/**
 * 圆屏自适应变换（对应 Android presentation/ui/components/AdaptiveTransforms.kt）。
 *
 * Android 版 `adaptiveTransformedHeight(scope, transformationSpec)` 在圆屏手表上对
 * Wear `TransformingLazyColumn` 列表项按滚动位置应用高度缩放（`transformedHeight`），
 * 方屏（如 Mi Watch S 系列）则透传不做变换，保证方屏列表项不变形。
 *
 * Kuikly 无 `TransformingLazyColumn` / `TransformationSpec` / `LocalScreenRound`
 * 等价物，此处保留「屏幕形状判定 + 条件应用变换」的纯逻辑：
 *  - [isRoundScreen]：屏幕形状标记，由平台层（ArkTS @ohos.display 的 isRound）在
 *    启动时注入；
 *  - 列表项缩放效果由调用方基于 [heightFactor] 自行施加（如 Modifier.size/height
 *    乘系数），与 Android 语义一致：圆屏生效、方屏恒为 1f。
 */
object AdaptiveTransforms {

    /** 是否圆屏。默认 false（方屏）；平台层启动时注入真实值。 */
    var isRoundScreen: Boolean = false

    /**
     * 列表项高度缩放系数：圆屏返回 [scale]，方屏恒为 1f（不缩放）。
     * 默认 [scale] 取 0.8f 仅为示意，实际值由调用方按 UI 复现度指定。
     */
    fun heightFactor(scale: Float = 0.8f): Float = if (isRoundScreen) scale else 1f

    /**
     * 通用条件门：圆屏返回原值（可作变换参数），方屏返回 null（调用方跳过变换）。
     * 对应 Android `if (isRound) SurfaceTransformation(spec) else null` 的取值逻辑。
     */
    fun <T> roundOnly(value: T): T? = if (isRoundScreen) value else null
}
