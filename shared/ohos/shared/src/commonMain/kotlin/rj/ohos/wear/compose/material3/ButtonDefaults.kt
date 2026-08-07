package rj.ohos.wear.compose.material3

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.BorderStroke
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.material3.ButtonDefaults as KuiklyButtonDefaults
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Shape
import com.tencent.kuikly.compose.ui.unit.Dp

/**
 * 按钮默认值。
 *
 * 对应 androidx.wear.compose.material3.ButtonDefaults 的简化实现：
 * 形状、内边距、尺寸等直接透传 Kuikly [KuiklyButtonDefaults] 的对应常量；
 * 颜色相关返回本库的 [ButtonColors]（containerColor/contentColor 简单 data class）。
 */
object ButtonDefaults {

    /**
     * 默认按钮配色。
     *
     * 对应 androidx.wear.compose.material3.ButtonDefaults.buttonColors(...)，
     * 但按任务约定命名为 [colors]。无参时取 Kuikly 主题默认的填充按钮色。
     */
    @Composable
    fun colors(
        containerColor: Color = KuiklyButtonDefaults.buttonColors().containerColor,
        contentColor: Color = KuiklyButtonDefaults.buttonColors().contentColor,
    ): ButtonColors = ButtonColors(containerColor, contentColor)

    /** androidx 兼容别名，等价于 [colors]。对应 ButtonDefaults.buttonColors()。 */
    @Composable
    fun buttonColors(
        containerColor: Color = KuiklyButtonDefaults.buttonColors().containerColor,
        contentColor: Color = KuiklyButtonDefaults.buttonColors().contentColor,
    ): ButtonColors = colors(containerColor, contentColor)

    /** FilledTonalButton 默认配色。对应 ButtonDefaults.filledTonalButtonColors()。 */
    @Composable
    fun filledTonalButtonColors(
        containerColor: Color = KuiklyButtonDefaults.filledTonalButtonColors().containerColor,
        contentColor: Color = KuiklyButtonDefaults.filledTonalButtonColors().contentColor,
    ): ButtonColors = ButtonColors(containerColor, contentColor)

    /** OutlinedButton 默认配色。对应 ButtonDefaults.outlinedButtonColors()。 */
    @Composable
    fun outlinedButtonColors(
        containerColor: Color = KuiklyButtonDefaults.outlinedButtonColors().containerColor,
        contentColor: Color = KuiklyButtonDefaults.outlinedButtonColors().contentColor,
    ): ButtonColors = ButtonColors(containerColor, contentColor)

    /** TextButton 默认配色。对应 ButtonDefaults.textButtonColors()。 */
    @Composable
    fun textButtonColors(
        containerColor: Color = KuiklyButtonDefaults.textButtonColors().containerColor,
        contentColor: Color = KuiklyButtonDefaults.textButtonColors().contentColor,
    ): ButtonColors = ButtonColors(containerColor, contentColor)

    /** 填充按钮默认形状。对应 ButtonDefaults.shape。 */
    val shape: Shape
        @Composable get() = KuiklyButtonDefaults.shape

    /** FilledTonalButton 默认形状。对应 ButtonDefaults.filledTonalShape。 */
    val filledTonalShape: Shape
        @Composable get() = KuiklyButtonDefaults.filledTonalShape

    /** OutlinedButton 默认形状。对应 ButtonDefaults.outlinedShape。 */
    val outlinedShape: Shape
        @Composable get() = KuiklyButtonDefaults.outlinedShape

    /** TextButton 默认形状。对应 ButtonDefaults.textShape。 */
    val textShape: Shape
        @Composable get() = KuiklyButtonDefaults.textShape

    /** OutlinedButton 默认描边。对应 ButtonDefaults.outlinedButtonBorder(enabled)。 */
    @Composable
    fun outlinedButtonBorder(enabled: Boolean = true): BorderStroke =
        KuiklyButtonDefaults.outlinedButtonBorder(enabled)

    /** 按钮默认内边距。对应 ButtonDefaults.ContentPadding。 */
    val ContentPadding: PaddingValues get() = KuiklyButtonDefaults.ContentPadding

    /** 带图标按钮默认内边距。对应 ButtonDefaults.ButtonWithIconContentPadding。 */
    val ButtonWithIconContentPadding: PaddingValues get() = KuiklyButtonDefaults.ButtonWithIconContentPadding

    /** TextButton 默认内边距。对应 ButtonDefaults.TextButtonContentPadding。 */
    val TextButtonContentPadding: PaddingValues get() = KuiklyButtonDefaults.TextButtonContentPadding

    /** 带图标 TextButton 默认内边距。对应 ButtonDefaults.TextButtonWithIconContentPadding。 */
    val TextButtonWithIconContentPadding: PaddingValues get() = KuiklyButtonDefaults.TextButtonWithIconContentPadding

    /** 按钮最小宽度。对应 ButtonDefaults.MinWidth。 */
    val MinWidth: Dp get() = KuiklyButtonDefaults.MinWidth

    /** 按钮最小高度。对应 ButtonDefaults.MinHeight。 */
    val MinHeight: Dp get() = KuiklyButtonDefaults.MinHeight

    /** 按钮图标尺寸。对应 ButtonDefaults.IconSize。 */
    val IconSize: Dp get() = KuiklyButtonDefaults.IconSize

    /** 按钮图标与文字间距。对应 ButtonDefaults.IconSpacing。 */
    val IconSpacing: Dp get() = KuiklyButtonDefaults.IconSpacing
}
