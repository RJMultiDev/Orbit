package com.qx.orbit.ohos.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.components.UserAvatar
import com.qx.orbit.ohos.components.UserNameText
import com.qx.orbit.ohos.components.parseRichText
import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.data.DynamicDetail
import com.qx.orbit.ohos.data.dynamicDetail
import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * 动态详情页（对齐 Android DynamicDetailScreen 核心布局：作者行/正文/图片占位/
 * 点赞评论转发 + 含视频时的「查看视频」入口）。
 */
@Page(AppRouter.PAGE_DYNAMIC_DETAIL)
internal class DynamicDetailPage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            DynamicDetailContent(client)
        }
    }
}

@Composable
internal fun DynamicDetailContent(client: BiliClient) {
    val id = PageArgs.getString("id")
    var detail by remember { mutableStateOf<DynamicDetail?>(null) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(id) {
        if (id.isEmpty()) {
            error = "缺少动态参数"
            return@LaunchedEffect
        }
        try {
            detail = client.dynamicDetail(id)
        } catch (e: Throwable) {
            error = e.message ?: "加载失败"
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        val d = detail
        if (d != null) {
            // 作者行：头像 + 昵称 + 类型副标 + 时间
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UserAvatar(
                    avatarUrl = d.userFace,
                    officialRole = 0,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    UserNameText(
                        name = d.userName.ifEmpty { "未知用户" },
                        isVip = false,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = when (d.type) {
                            "DYNAMIC_TYPE_AV" -> "投稿了视频"
                            "DYNAMIC_TYPE_LIVE_RCMD" -> "直播了"
                            else -> "发布了动态"
                        },
                        fontSize = 10.sp,
                        color = OrbitColors.TextSecondary,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = d.time,
                    fontSize = 10.sp,
                    color = OrbitColors.TextSecondary,
                )
            }

            // 正文（富文本：@/#/URL/BV 高亮；表情 inline 图暂不渲染）
            if (d.text.isNotEmpty()) {
                val (richText, _) = parseRichText(d.text, emptyMap())
                Text(
                    text = richText,
                    fontSize = 12.sp,
                    color = OrbitColors.OnSurface,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            // 图片区占位（模型暂无图片字段，待接入 draw/opus 图片列表）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .height(120.dp)
                    .background(OrbitColors.CardBackground, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "图片区（待实现）", fontSize = 11.sp, color = OrbitColors.TextSecondary)
            }

            // 含视频：进 videoDetail
            if (d.bvid.isNotEmpty()) {
                Button(
                    onClick = {
                        PageArgs.set("bvid", d.bvid)
                        RouterModule().openPage("videoDetail", null)
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(text = "查看视频", fontSize = 12.sp)
                }
            }

            // 点赞 / 评论 / 转发
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DynamicStat(label = "赞", count = d.likeCount) { /* TODO: 接入点赞动作 */ }
                Spacer(modifier = Modifier.width(14.dp))
                DynamicStat(label = "评论", count = d.commentCount) { /* TODO: 接入评论回复 */ }
                Spacer(modifier = Modifier.width(14.dp))
                DynamicStat(label = "转发", count = d.forwardCount) { /* TODO: 接入转发 */ }
            }
        } else {
            Text(
                text = if (error.isNotEmpty()) error else "加载中…",
                fontSize = 12.sp,
                color = if (error.isNotEmpty()) OrbitColors.ErrorRed else OrbitColors.TextSecondary,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

/** 点赞/评论/转发按钮：文本标签 + 计数（Kuikly 无 Icon 组件，以文字替代）。 */
@Composable
private fun DynamicStat(label: String, count: Long, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp),
    ) {
        Text(text = label, fontSize = 10.sp, color = OrbitColors.TextSecondary)
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = formatCount(count), fontSize = 10.sp, color = OrbitColors.TextSecondary)
    }
}
