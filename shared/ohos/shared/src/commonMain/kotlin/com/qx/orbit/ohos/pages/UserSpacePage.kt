package com.qx.orbit.ohos.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.components.UserAvatar
import com.qx.orbit.ohos.components.UserNameText
import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.data.UserSpace
import com.qx.orbit.ohos.data.VideoItem
import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
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
 * 用户空间页（对齐 Android UserSpaceScreen 核心布局：
 * 用户信息卡 + 投稿视频列表；动态/图文 tab 与横向分页暂不移植）。
 */
@Page("userSpace")
internal class UserSpacePage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            UserSpaceContent(client)
        }
    }
}

@Composable
internal fun UserSpaceContent(client: BiliClient) {
    val mid = PageArgs.getLong(AppRouter.KEY_MID)
    var user by remember { mutableStateOf<UserSpace?>(null) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(mid) {
        if (mid == 0L) {
            loading = false
            error = "缺少用户参数"
            return@LaunchedEffect
        }
        loading = true
        error = ""
        try {
            user = client.userSpace(mid)
            videos = client.userVideos(mid)
            if (user == null && error.isEmpty()) {
                error = "加载用户信息失败"
            }
        } catch (e: Throwable) {
            error = e.message ?: "加载失败"
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        if (error.isNotEmpty()) {
            Text(
                text = error,
                fontSize = 12.sp,
                color = OrbitColors.ErrorRed,
                modifier = Modifier.padding(10.dp),
            )
        }
        when {
            loading && user == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp),
                )
            }
            else -> {
                user?.let { UserSpaceHeader(it) }

                Text(
                    text = "发布的视频",
                    fontSize = 12.sp,
                    color = OrbitColors.Primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
                if (videos.isEmpty()) {
                    Text(
                        text = "暂无投稿",
                        fontSize = 12.sp,
                        color = OrbitColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                        for (item in videos) {
                            VideoItemRow(
                                item = item,
                                onClick = {
                                    PageArgs.set(AppRouter.KEY_BVID, item.bvid)
                                    RouterModule().openPage(AppRouter.PAGE_VIDEO_DETAIL, null)
                                },
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 用户信息卡：头像 / 昵称 / 签名 / 粉丝关注数 + 关注按钮。 */
@Composable
internal fun UserSpaceHeader(user: UserSpace) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UserAvatar(
            avatarUrl = user.face,
            officialRole = 0,
            modifier = Modifier.size(60.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        UserNameText(
            name = user.name.ifEmpty { "未知用户" },
            isVip = false,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${formatCount(user.follower)}粉丝  ${formatCount(user.following)}关注",
            fontSize = 11.sp,
            color = OrbitColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (user.sign.isNotEmpty()) {
            Text(
                text = user.sign,
                fontSize = 11.sp,
                color = OrbitColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 关注/取关：交互 TODO（B3 follow 扩展尚未就绪；UserSpace 暂无 isFollowed 状态，
        // acc/info 返回的 is_followed 可用于后续接线）
        Button(onClick = { /* TODO 关注/取关（调 B3 follow 扩展或 /x/relation/modify + csrf） */ }) {
            Text(text = "关注", fontSize = 12.sp)
        }
    }
}

/** 投稿视频行：标题 + 播放数，点击进 videoDetail。 */
@Composable
internal fun VideoItemRow(item: VideoItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OrbitColors.CardBackground)
            .padding(10.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = item.title,
            fontSize = 13.sp,
            color = OrbitColors.OnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${formatCount(item.view)}播放",
            fontSize = 11.sp,
            color = OrbitColors.TextSecondary,
        )
    }
}
