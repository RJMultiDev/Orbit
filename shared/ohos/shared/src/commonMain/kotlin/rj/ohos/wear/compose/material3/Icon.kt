package rj.ohos.wear.compose.material3

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.TextUnit
import com.tencent.kuikly.compose.ui.unit.sp

/**
 * 图标占位组件。
 *
 * 对应 androidx.compose.material3.Icon(imageVector: ImageVector, contentDescription: String?, modifier, tint)。
 * Kuikly material3 无 ImageVector / Icon 组件，因此用 [Text] 渲染 [glyph]（单字符 / emoji）作为占位图标。
 *
 * [contentDescription] 保留以维持与 androidx 相同的参数形态；当前实现不渲染该描述（占位，待无障碍接入）。
 */
@Composable
fun Icon(
    /** 占位图标文本（单字符 / emoji）。androidx 对应 imageVector 参数，此处因无 ImageVector 类型而用文本替代。 */
    glyph: String,
    /** 无障碍内容描述，对应 androidx 的 contentDescription 参数。 */
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    fontSize: TextUnit = 24.sp,
) {
    Text(
        text = glyph,
        color = tint,
        fontSize = fontSize,
        maxLines = 1,
        modifier = modifier,
    )
}
