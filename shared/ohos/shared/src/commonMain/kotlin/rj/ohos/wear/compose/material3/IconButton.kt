package rj.ohos.wear.compose.material3

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.material3.ButtonDefaults as KuiklyButtonDefaults
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Shape

/**
 * 图标按钮默认值。
 *
 * 对应 androidx.wear.compose.material3.IconButtonDefaults 的简化实现。
 * Kuikly material3 无 IconButton，默认形状此处取 [CircleShape]。
 */
object IconButtonDefaults {

    /** 图标按钮默认形状。对应 IconButtonDefaults.shape（androidx 为圆形/胶囊圆角）。 */
    val shape: Shape = CircleShape

    /** 普通 IconButton 默认容器色（透明，无填充）。对应 IconButtonDefaults.iconButtonColors 的容器色。 */
    val containerColor: Color = Color.Transparent
}

/**
 * 图标按钮。
 *
 * 对应 androidx.wear.compose.material3.IconButton(onClick, modifier, enabled, shape, colors, border, interactionSource, content: () -> Unit)。
 * Kuikly material3 无 IconButton 组件，此处基于 [Box] + [clickable] 实现：
 * 点击区域由 [modifier] 及内容尺寸决定，无填充色，可配合 [Icon] 使用。
 */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * 填充图标按钮。
 *
 * 对应 androidx.wear.compose.material3.FilledIconButton(onClick, modifier, enabled, shape, colors, border, interactionSource, content: () -> Unit)。
 * 在 [IconButton] 基础上带圆形填充背景，默认容器色取 Kuikly 主题的填充按钮容器色。
 */
@Composable
fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = KuiklyButtonDefaults.buttonColors().containerColor,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(IconButtonDefaults.shape)
            .background(containerColor, IconButtonDefaults.shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
