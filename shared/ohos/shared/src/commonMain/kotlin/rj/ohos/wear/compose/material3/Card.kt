package rj.ohos.wear.compose.material3

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.BorderStroke
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ColumnScope
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Card as KuiklyCard
import com.tencent.kuikly.compose.material3.CardColors as KuiklyCardColors
import com.tencent.kuikly.compose.material3.CardDefaults as KuiklyCardDefaults
import com.tencent.kuikly.compose.material3.CardElevation as KuiklyCardElevation
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Shape
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp

/** 对应 androidx.wear.compose.material3.CardColors。 */
typealias CardColors = KuiklyCardColors

/** 对应 androidx.wear.compose.material3.CardElevation。 */
typealias CardElevation = KuiklyCardElevation

/**
 * Card 默认值，对齐 androidx.wear.compose.material3.CardDefaults。
 *
 * Kuikly material3 无独立 CardDefaults.ContentPadding，此处补齐；
 * 颜色默认取 Orbit 暗色主题的 surfaceContainerLow / onSurface（与
 * com.qx.orbit.ohos.theme.OrbitColors 同值，库内不自依赖业务包）。
 */
object CardDefaults {
    /** androidx.wear.compose.material3.CardDefaults.shape == RoundedCornerShape(12.dp) */
    val shape: Shape = RoundedCornerShape(12.dp)

    /** androidx.wear.compose.material3.CardDefaults.ContentPadding == PaddingValues(12.dp) */
    val ContentPadding: PaddingValues = PaddingValues(12.dp)

    /**
     * 对应 androidx.wear.compose.material3.CardDefaults.cardColors()。
     * 默认容器色 #FF22191B（surfaceContainerLow）、内容色 #FFEFDFE1（onSurface）。
     */
    fun cardColors(
        containerColor: Color = Color(0xFF22191B),
        contentColor: Color = Color(0xFFEFDFE1),
        disabledContainerColor: Color = containerColor.copy(alpha = 0.38f),
        disabledContentColor: Color = contentColor.copy(alpha = 0.38f),
    ): CardColors = KuiklyCardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    /**
     * 对应 androidx.wear.compose.material3.CardDefaults.cardElevation()。
     * Kuikly 不支持 elevation 绘制（仅传参存储），默认全部为 0.dp。
     */
    fun cardElevation(
        defaultElevation: Dp = 0.dp,
        pressedElevation: Dp = 0.dp,
        focusedElevation: Dp = 0.dp,
        hoveredElevation: Dp = 0.dp,
        draggedElevation: Dp = 0.dp,
        disabledElevation: Dp = 0.dp,
    ): CardElevation = KuiklyCardDefaults.cardElevation(
        defaultElevation = defaultElevation,
        pressedElevation = pressedElevation,
        focusedElevation = focusedElevation,
        hoveredElevation = hoveredElevation,
        draggedElevation = draggedElevation,
        disabledElevation = disabledElevation,
    )
}

/**
 * 可点击卡片，对齐 androidx.wear.compose.material3.Card。
 *
 * 包装 Kuikly material3 Card（已含 clickable 交互）。Kuikly 版无
 * [contentPadding] 参数，故在内部再包一层带 padding 的 Column 模拟
 * wear m3 的 CardDefaults.ContentPadding 语义（内容在圆角背景内缩进）。
 *
 * @param onClick 点击回调。
 * @param contentPadding 内容内边距（默认 12.dp，与 wear m3 一致）。
 */
@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    KuiklyCard(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            content()
        }
    }
}

/**
 * 不可点击卡片，对齐 androidx.wear.compose.material3.Card 的无 onClick 重载。
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    KuiklyCard(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            content()
        }
    }
}
