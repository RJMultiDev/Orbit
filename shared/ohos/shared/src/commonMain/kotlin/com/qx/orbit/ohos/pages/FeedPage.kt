package com.qx.orbit.ohos.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.components.UserAvatar
import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.data.DynamicItem
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
import com.tencent.kuikly.compose.material3.CircularProgressIndicator
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule

/**
 * 动态 Feed 页（对齐 Android DynamicFeedScreen 核心布局：
 * 顶部用户信息/搜索入口 + 动态卡片列表）。
 */
@Page("feed")
internal class FeedPage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            FeedContent(client, credential)
        }
    }
}

@Composable
internal fun FeedContent(client: BiliClient, credential: OrbitCredentialModule) {
    var items by remember { mutableStateOf<List<DynamicItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var loggedIn by remember { mutableStateOf(false) }
    var uname by remember { mutableStateOf("") }
    var userFace by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val nav = client.nav()
        loggedIn = nav?.isLogin == true
        uname = nav?.uname ?: ""
        userFace = nav?.face ?: ""
        credential.setLoggedIn(loggedIn)
        loading = true
        error = ""
        try {
            items = client.dynamicFeed()
        } catch (e: Throwable) {
            error = e.message ?: "网络错误"
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        // 顶部用户信息区（头像/昵称/登录状态）+ 搜索入口
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                modifier = Modifier.size(28.dp),
                avatarUrl = userFace,
                officialRole = 0,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (loggedIn && uname.isNotEmpty()) uname else "未登录",
                fontSize = 11.sp,
                color = OrbitColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!loggedIn) {
                Button(onClick = { RouterModule().openPage("login", null) }) {
                    Text(text = "登录", fontSize = 11.sp)
                }
            }
            Button(onClick = { RouterModule().openPage("search", null) }) {
                Text(text = "搜索", fontSize = 11.sp)
            }
        }

        if (error.isNotEmpty()) {
            Text(
                text = error,
                fontSize = 12.sp,
                color = OrbitColors.ErrorRed,
                modifier = Modifier.padding(8.dp),
            )
        }

        when {
            loading && items.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            items.isEmpty() -> {
                Text(
                    text = "暂无动态",
                    fontSize = 12.sp,
                    color = OrbitColors.TextSecondary,
                    modifier = Modifier.padding(16.dp),
                )
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                ) {
                    for (item in items) {
                        DynamicCardRow(item = item, onClick = {
                            PageArgs.set("dynamicId", item.id)
                            RouterModule().openPage("dynamicDetail", null)
                        })
                    }
                }
            }
        }
    }
}

/**
 * 动态卡片（对齐 Android components/DynamicCard.kt 核心字段：
 * 发布者昵称+时间 / 正文 / 图片区占位 / 赞·评论·转发计数）。
 */
@Composable
internal fun DynamicCardRow(item: DynamicItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.CardBackground)
            .padding(10.dp)
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.userName,
                fontSize = 11.sp,
                color = OrbitColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = item.time,
                fontSize = 10.sp,
                color = OrbitColors.TextSecondary,
            )
        }

        if (item.text.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            // TODO: parseRichText 高级富文本（表情/@成员/话题/链接）占位，先纯文本渲染
            Text(
                text = item.text,
                fontSize = 12.sp,
                color = OrbitColors.OnSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 图片区：DRAW/OPUS 为图文动态；Kuikly 卡片内网络图（coil3）未完全确认，先用 Box 占位
        if (item.type == "DYNAMIC_TYPE_DRAW" || item.type == "DYNAMIC_TYPE_OPUS") {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(OrbitColors.SurfaceContainerHigh, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "图片",
                    fontSize = 10.sp,
                    color = OrbitColors.TextSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "赞 ${formatCount(item.likeCount)}",
                fontSize = 10.sp,
                color = OrbitColors.TextSecondary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "评论 ${formatCount(item.commentCount)}",
                fontSize = 10.sp,
                color = OrbitColors.TextSecondary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "转发 ${formatCount(item.forwardCount)}",
                fontSize = 10.sp,
                color = OrbitColors.TextSecondary,
            )
        }
    }
}
