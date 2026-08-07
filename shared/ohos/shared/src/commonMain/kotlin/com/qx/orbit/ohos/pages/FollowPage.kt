package com.qx.orbit.ohos.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.components.UserAvatar
import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.data.FollowUser
import com.qx.orbit.ohos.data.followList
import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.CircularProgressIndicator
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule

/**
 * 关注列表页（对齐 Android 关注用户列表：头像/昵称/签名，分页加载）。
 * 入口 @Page("follow")，mid 经 PageArgs.set("mid", mid) 透传。
 */
@Page("follow")
internal class FollowPage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            FollowContent(client)
        }
    }
}

@Composable
internal fun FollowContent(client: BiliClient) {
    val mid = PageArgs.getLong("mid")
    var users by remember { mutableStateOf<List<FollowUser>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var hasMore by remember { mutableStateOf(true) }
    var loadMoreTick by remember { mutableStateOf(0) }

    // 首屏加载
    LaunchedEffect(mid) {
        if (mid <= 0) {
            error = "缺少用户参数"
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = ""
        hasMore = true
        try {
            users = client.followList(mid, 1)
            page = 1
        } catch (e: Throwable) {
            error = e.message ?: "加载失败"
        }
        loading = false
    }

    // 加载更多：点击按钮累加 loadMoreTick 触发，page 递增
    LaunchedEffect(loadMoreTick) {
        if (loadMoreTick == 0) return@LaunchedEffect
        if (loadingMore || loading) return@LaunchedEffect
        loadingMore = true
        error = ""
        try {
            val more = client.followList(mid, page + 1)
            if (more.isEmpty()) {
                hasMore = false
            } else {
                users = users + more
                page += 1
            }
        } catch (e: Throwable) {
            error = e.message ?: "加载失败"
        }
        loadingMore = false
    }

    Column(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        if (error.isNotEmpty()) {
            Text(
                text = error,
                fontSize = 12.sp,
                color = OrbitColors.ErrorRed,
                modifier = Modifier.padding(8.dp),
            )
        }
        when {
            loading && users.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            users.isEmpty() -> {
                Text(
                    text = "暂无关注",
                    fontSize = 12.sp,
                    color = OrbitColors.TextSecondary,
                    modifier = Modifier.padding(10.dp),
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                    for (user in users) {
                        FollowUserRow(user = user, onClick = {
                            PageArgs.set("mid", user.mid)
                            RouterModule().openPage("userSpace", null)
                        })
                    }
                    if (hasMore) {
                        if (loadingMore) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            Button(
                                onClick = {
                                    if (!loading && !loadingMore) loadMoreTick += 1
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            ) {
                                Text(text = "加载更多", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FollowUserRow(user: FollowUser, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.CardBackground)
            .padding(10.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            avatarUrl = user.face,
            officialRole = 0,
            modifier = Modifier.size(36.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                fontSize = 13.sp,
                color = OrbitColors.OnSurface,
                maxLines = 1,
            )
            if (user.sign.isNotEmpty()) {
                Text(
                    text = user.sign,
                    fontSize = 11.sp,
                    color = OrbitColors.TextSecondary,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
