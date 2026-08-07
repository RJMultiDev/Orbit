package com.qx.orbit.ohos.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.components.fixCoverUrl
import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.data.LiveRoom
import com.qx.orbit.ohos.data.livePlayUrl
import com.qx.orbit.ohos.data.liveRoomInfo
import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule

/**
 * 直播详情页（对齐 Android LiveDetailScreen 的 LiveInfoPage 核心布局）。
 */
@Page("live")
internal class LivePage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            LiveContent(client)
        }
    }
}

@Composable
internal fun LiveContent(client: BiliClient) {
    val roomId = PageArgs.getLong("roomId")
    var room by remember { mutableStateOf<LiveRoom?>(null) }
    var error by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }

    LaunchedEffect(roomId) {
        if (roomId <= 0L) {
            error = "缺少直播间参数"
            return@LaunchedEffect
        }
        try {
            val info = client.liveRoomInfo(roomId)
            if (info == null) {
                error = "直播间不存在或已下播"
            } else {
                // 补齐取流地址（getInfoByRoom 自带 playUrl 时不再额外请求）
                val streamUrl = if (info.playUrl.isNotEmpty()) info.playUrl
                    else client.livePlayUrl(roomId) ?: ""
                room = info.copy(playUrl = streamUrl)
            }
        } catch (e: Throwable) {
            error = e.message ?: "加载失败"
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        val r = room
        if (r != null) {
            // 封面：coil3 网络图；空封面以色块占位
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrbitColors.CardBackground),
                contentAlignment = Alignment.Center,
            ) {
                if (r.cover.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(r.cover.fixCoverUrl()),
                        contentDescription = r.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = "直播间",
                        fontSize = 12.sp,
                        color = OrbitColors.TextSecondary,
                    )
                }
            }
            // 标题
            Text(
                text = r.title,
                fontSize = 14.sp,
                color = OrbitColors.OnSurface,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            )
            // 主播名
            Text(
                text = "主播：${r.uname}",
                fontSize = 11.sp,
                color = OrbitColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
            // 在线人数
            Text(
                text = "${formatCount(r.online)}人观看",
                fontSize = 11.sp,
                color = OrbitColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            if (hint.isNotEmpty()) {
                Text(
                    text = hint,
                    fontSize = 11.sp,
                    color = OrbitColors.ErrorRed,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            // 进入直播间：liveUrl 经 PageArgs 传 player 页（原生 AVPlayer 桥接后续接管）
            Button(
                onClick = {
                    val url = r.playUrl
                    if (url.isEmpty()) {
                        hint = "取流失败：暂无可用播放地址（AVPlayer 原生桥接入后由 player 页播放）"
                    } else {
                        PageArgs.set("liveUrl", url)
                        PageArgs.set("liveTitle", r.title)
                        RouterModule().openPage("player", null)
                    }
                },
                modifier = Modifier.padding(10.dp),
            ) {
                Text(text = "进入直播间", fontSize = 12.sp)
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
