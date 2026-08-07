package rj.ohos.wear.compose.material3

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.BorderStroke
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.RowScope
import com.tencent.kuikly.compose.material3.Button as KuiklyButton
import com.tencent.kuikly.compose.material3.ButtonColors as KuiklyButtonColors
import com.tencent.kuikly.compose.material3.FilledTonalButton as KuiklyFilledTonalButton
import com.tencent.kuikly.compose.material3.OutlinedButton as KuiklyOutlinedButton
import com.tencent.kuikly.compose.material3.TextButton as KuiklyTextButton
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Shape

/**
 * 按钮配色。
 *
 * 对应 androidx.wear.compose.material3.ButtonColors 接口的简化实现：
 * androidx 以接口形式暴露 `containerColor(enabled)` / `contentColor(enabled)` 等成员，
 * 此处仅保留 [containerColor] / [contentColor] 两个核心字段；
 * 禁用态颜色由包装层按 Material3 惯例从这两个颜色做 alpha 推导
 * （见 [Button.toKuiklyColors]）。
 */
data class ButtonColors(
    val containerColor: Color,
    val contentColor: Color,
)

/**
 * 填充按钮。
 *
 * 对应 androidx.wear.compose.material3.Button(...)：
 * `Button(onClick, modifier, enabled, shape, colors, border, contentPadding, interactionSource, content: RowScope.() -> Unit)`。
 *
 * 直接包装 Kuikly [KuiklyButton]；Kuikly 额外支持 elevation，此处按 androidx 签名不暴露，内部固定传 null。
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.colors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    KuiklyButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors.toKuiklyColors(),
        elevation = null,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * 次级填充按钮（tonal button）。
 *
 * 对应 androidx.wear.compose.material3.FilledTonalButton(...)，签名与 [Button] 一致，
 * 默认使用 [ButtonDefaults.filledTonalShape] / [ButtonDefaults.filledTonalButtonColors]。
 * 包装 Kuikly [KuiklyFilledTonalButton]。
 */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    KuiklyFilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors.toKuiklyColors(),
        elevation = null,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * 描边按钮（outlined button）。
 *
 * 对应 androidx.wear.compose.material3.OutlinedButton(...)，签名与 [Button] 一致，
 * 默认使用 [ButtonDefaults.outlinedShape] / [ButtonDefaults.outlinedButtonColors]，
 * 且默认带描边 [ButtonDefaults.outlinedButtonBorder]。
 * 包装 Kuikly [KuiklyOutlinedButton]。
 */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    KuiklyOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors.toKuiklyColors(),
        elevation = null,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * 文本按钮（text button）。
 *
 * 对应 androidx.wear.compose.material3.TextButton(...)，签名与 [Button] 一致，
 * 默认使用 [ButtonDefaults.textShape] / [ButtonDefaults.textButtonColors] / [ButtonDefaults.TextButtonContentPadding]。
 * 包装 Kuikly [KuiklyTextButton]。
 */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    KuiklyTextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors.toKuiklyColors(),
        elevation = null,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * 将本库的简化配色 [ButtonColors] 映射为 Kuikly [KuiklyButtonColors]。
 *
 * 禁用态颜色按 Material3 惯例从启用态颜色做 alpha 推导：
 * 禁用容器色 = containerColor @ 12% alpha，禁用内容色 = contentColor @ 38% alpha。
 */
private fun ButtonColors.toKuiklyColors(): KuiklyButtonColors = KuiklyButtonColors(
    containerColor = containerColor,
    contentColor = contentColor,
    disabledContainerColor = containerColor.copy(alpha = DisabledContainerAlpha),
    disabledContentColor = contentColor.copy(alpha = DisabledContentAlpha),
)

/** Material3 禁用态容器色 alpha（onSurface @ 12%）。 */
private const val DisabledContainerAlpha = 0.12f

/** Material3 禁用态内容色 alpha（onSurface @ 38%）。 */
private const val DisabledContentAlpha = 0.38f
