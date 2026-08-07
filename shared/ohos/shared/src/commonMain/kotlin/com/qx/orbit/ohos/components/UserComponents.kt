package com.qx.orbit.ohos.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.resources.DrawableResource
import com.tencent.kuikly.compose.resources.InternalResourceApi
import com.tencent.kuikly.compose.resources.painterResource
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.TextUnit
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 视频卡片数据（对应 Android data/model/Video.kt 的 VideoCard）。
 */
data class VideoCard(
    val title: String = "",
    val upName: String = "",
    val view: String = "",
    val cover: String = "",
    val type: String = "video",
    val aid: Long = 0,
    val bvid: String = "",
    val cid: Long = 0,
    val mid: Long = 0,
    val seasonId: Long = 0,
    val progressPercent: Float? = null,
    val kid: String = "",
)

/**
 * 用户头像（圆形，可选直播彩圈、官方/企业认证角标、VIP 角标、点击回调）。
 *
 * 对应 Android components/UserAvatar.kt。差异：
 * - 网络图走 Kuikly coil3 `rememberAsyncImagePainter`（Android 为 coil AsyncImage）；
 * - 占位图用「人」字替代 material-icons Person（Kuikly 无 Icon 组件）；
 * - 认证/VIP 角标按同名 drawable 引用（`painterResource(DrawableResource(...))`），
 *   资源需在 HarmonyOS entry 侧按此名字提供，缺失时仅不显示角标（TODO）。
 */
@Composable
@OptIn(InternalResourceApi::class)
fun UserAvatar(
    modifier: Modifier = Modifier,
    avatarUrl: String,
    officialRole: Int,
    isVip: Boolean = false,
    isLive: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val boxModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Box(modifier = boxModifier) {
        val imageModifier = Modifier
            .fillMaxSize()
            .then(
                if (isLive) Modifier.padding(2.dp).border(
                    width = 2.dp,
                    brush = Brush.linearGradient(colors = listOf(OrbitColors.BiliPink, Color(0xFFFF85A1))),
                    shape = CircleShape,
                ).padding(2.dp) else Modifier
            )
            .clip(CircleShape)

        if (avatarUrl.isNotEmpty()) {
            val baseAvatarUrl = avatarUrl.fixCoverUrl()
            val fixedAvatarUrl = if (baseAvatarUrl.contains("@")) baseAvatarUrl else "${baseAvatarUrl}@150w_150h_1c.webp"

            Image(
                painter = rememberAsyncImagePainter(fixedAvatarUrl),
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
            )
        } else {
            Box(
                modifier = imageModifier.background(OrbitColors.SurfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "人",
                    fontSize = 12.sp,
                    color = OrbitColors.OnSurfaceVariant,
                )
            }
        }

        if (officialRole == 1) {
            Image(
                painter = painterResource(DrawableResource("ic_certification_official")),
                contentDescription = "Official Verification",
                modifier = Modifier
                    .fillMaxSize(0.35f)
                    .align(Alignment.BottomEnd),
            )
        } else if (officialRole >= 2) {
            Image(
                painter = painterResource(DrawableResource("ic_certification_enterprise")),
                contentDescription = "Enterprise Verification",
                modifier = Modifier
                    .fillMaxSize(0.35f)
                    .align(Alignment.BottomEnd),
            )
        } else if (isVip) {
            Image(
                painter = painterResource(DrawableResource("icon_vip_badge_normal")),
                contentDescription = "VIP",
                modifier = Modifier
                    .fillMaxSize(0.35f)
                    .align(Alignment.BottomEnd),
            )
        }
    }
}

/**
 * 用户等级图标（Lv0~Lv6 + 高级 Lv6）。
 *
 * 对应 Android components/LevelIcon.kt；drawable 名与原版一致，
 * 通过 `painterResource(DrawableResource(name))` 按名引用，资源待 HarmonyOS
 * entry 侧补齐（TODO）。
 */
@Composable
@OptIn(InternalResourceApi::class)
fun LevelIcon(
    level: Int,
    isSenior: Boolean,
    modifier: Modifier = Modifier,
) {
    val drawableName = when {
        isSenior -> "ic_lv6_senior"
        level == 1 -> "ic_lv1_v2"
        level == 2 -> "ic_lv2_v2"
        level == 3 -> "ic_lv3_v2"
        level == 4 -> "ic_lv4_v2"
        level == 5 -> "ic_lv5_v2"
        level == 6 -> "ic_lv6_v2"
        else -> "ic_lv0_v2"
    }

    Image(
        painter = painterResource(DrawableResource(drawableName)),
        contentDescription = "Level $level",
        modifier = modifier.height(20.dp),
    )
}

/**
 * 用户昵称文本：VIP 昵称用 B 站粉，可配置颜色/字号/行数（单行省略）。
 *
 * 对应 Android components/UserNameText.kt。原版带 `style: TextStyle`（默认
 * bodySmall）与 overflow 参数，这里以 fontSize 承载字号，overflow 固定 Ellipsis。
 */
@Composable
fun UserNameText(
    name: String,
    isVip: Boolean,
    modifier: Modifier = Modifier,
    color: Color = OrbitColors.OnSurface,
    maxLines: Int = 1,
    fontSize: TextUnit = 11.sp,
) {
    Text(
        text = name,
        color = if (isVip) OrbitColors.BiliPink else color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        fontSize = fontSize,
    )
}

/**
 * 手表顶部时间（HH:mm，粗体，半透明白字，每秒刷新）。
 *
 * 对应 Android components/WysTimeText.kt 的简化移植：
 * - 仅保留「方形屏顶部居中」分支；圆形屏的 CurvedLayout 弧线时间不做（Kuikly 无
 *   对应 API，TODO）；
 * - 原版读取 SharedPreferences「ui_immersive_time」开关，此处暂恒显（TODO）；
 * - 时间格式化走 Kuikly CalendarModule（ArkTS 桥，设备本地时区）。
 */
@Composable
fun WysTimeText(modifier: Modifier = Modifier) {
    var timeString by remember { mutableStateOf(formatHm(currentTimeMillis())) }
    LaunchedEffect(Unit) {
        while (true) {
            timeString = formatHm(currentTimeMillis())
            delay(1_000)
        }
    }

    Box(
        modifier = modifier.fillMaxSize().padding(top = 4.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = timeString,
            color = OrbitColors.OnBackground.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * 推荐视频卡片（封面 + 暗色渐变遮罩 + 标题 + UP 主 + 播放数 + 可选进度条）。
 *
 * 对应 Android components/RecommendVideoCard.kt。差异：
 * - 用 material3 Card（Kuikly 版）承载，shape 16dp 圆角，无 contentPadding；
 * - 播放数前的 PlayArrow 图标用文本「▶」替代（Kuikly 无 Icon 组件）；
 * - 标题/副文字阴影由 SpanStyle 施加（见 Utils.shadowedText），对齐原版 TextStyle.shadow；
 * - 未接标题富文本高亮时直接显示纯文本（`<em>` 由 parseHighlightedTitle 处理）。
 */
@Composable
fun RecommendVideoCard(
    item: VideoCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(110.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 封面（B 站 16:9 处理后缀）
            if (item.cover.isNotEmpty()) {
                val coverUrl = if (item.cover.contains("@")) item.cover else "${item.cover}@480w_270h_1c.webp"
                Image(
                    painter = rememberAsyncImagePainter(coverUrl),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // 暗色渐变遮罩（保证文字可读）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.8f),
                            ),
                        )
                    ),
            )

            // 文字内容（上标题 / 下 UP 主+播放数）
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    val title = if (item.title.contains("<em")) {
                        shadowedText(parseHighlightedTitle(item.title))
                    } else {
                        shadowedText(item.title)
                    }
                    Text(
                        text = title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp,
                        color = Color.White,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = shadowedText(item.upName),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (item.view.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.type != "bangumi") {
                                Text(
                                    text = "▶",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            Text(
                                text = shadowedText(item.view),
                                fontSize = 10.sp,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            // 底部进度条
            item.progressPercent?.let { percent ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.White.copy(alpha = 0.3f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percent.coerceIn(0f, 1f))
                            .fillMaxSize()
                            .background(OrbitColors.Primary),
                    )
                }
            }
        }
    }
}
