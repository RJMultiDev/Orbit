package com.qx.orbit.ohos.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.components.RecommendVideoCard
import com.qx.orbit.ohos.components.RoundToast
import com.qx.orbit.ohos.components.RoundToastHost
import com.qx.orbit.ohos.components.VideoCard
import com.qx.orbit.ohos.components.fixCoverUrl
import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.data.HistoryItem
import com.qx.orbit.ohos.data.VideoItem
import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
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
import kotlinx.coroutines.launch

/**
 * 历史记录页（对齐 Android HistoryScreen 的简化版：列表卡片 + 点击进视频详情，
 * 清空走 TODO 接口）。布局复用共享列表骨架 [VideoListScreen]。
 */
@Page("history")
internal class HistoryPage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            HistoryContent(client)
        }
    }
}

/**
 * 稍后再看页（对齐 Android WatchLaterScreen 的简化版：列表卡片 + 点击进视频详情，
 * 删除走 TODO 接口）。
 */
@Page("watchLater")
internal class WatchLaterPage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            WatchLaterContent(client)
        }
    }
}

@Composable
internal fun HistoryContent(client: BiliClient) {
    var items by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        error = ""
        try {
            items = client.history()
        } catch (e: Throwable) {
            error = e.message ?: "加载失败"
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    VideoListScreen(
        header = "历史记录",
        emptyText = "暂无历史记录",
        loading = loading,
        error = error,
        rows = items.map { it.toVideoCard() },
        onOpen = { bvid ->
            PageArgs.set("bvid", bvid)
            RouterModule().openPage("videoDetail", null)
        },
        onRetry = { scope.launch { load() } },
        headerAction = {
            Button(onClick = { RoundToast.show("清空历史接口待接入（TODO）") }) {
                Text(text = "清空", fontSize = 11.sp)
            }
        },
    )
}

@Composable
internal fun WatchLaterContent(client: BiliClient) {
    var items by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        error = ""
        try {
            items = client.watchLater()
        } catch (e: Throwable) {
            error = e.message ?: "加载失败"
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    VideoListScreen(
        header = "稍后再看",
        emptyText = "暂无稍后再看",
        loading = loading,
        error = error,
        rows = items.map { it.toVideoCard() },
        onOpen = { bvid ->
            PageArgs.set("bvid", bvid)
            RouterModule().openPage("videoDetail", null)
        },
        onRetry = { scope.launch { load() } },
        rowAction = { card ->
            RoundToast.show("删除接口待接入（TODO: ${card.bvid}）")
        },
    )
}

/**
 * 共享的视频列表骨架：标题行（可选头部操作）+ 空/加载/错误/列表四态。
 *
 * @param rowAction 非空时每张卡片下方追加一行操作按钮（如稍后再看的「删除」）。
 */
@Composable
private fun VideoListScreen(
    header: String,
    emptyText: String,
    loading: Boolean,
    error: String,
    rows: List<VideoCard>,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    headerAction: (@Composable () -> Unit)? = null,
    rowAction: ((VideoCard) -> Unit)? = null,
) {
    Box(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = header,
                    fontSize = 14.sp,
                    color = OrbitColors.Primary,
                    modifier = Modifier.weight(1f),
                )
                if (headerAction != null) headerAction()
            }

            when {
                loading && rows.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error.isNotEmpty() && rows.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = error, fontSize = 12.sp, color = OrbitColors.ErrorRed)
                        Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                            Text(text = "重试", fontSize = 12.sp)
                        }
                    }
                }
                rows.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = emptyText, fontSize = 12.sp, color = OrbitColors.TextSecondary)
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                        for (card in rows) {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                RecommendVideoCard(item = card, onClick = { onOpen(card.bvid) })
                                if (rowAction != null) {
                                    Button(
                                        onClick = { rowAction(card) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    ) {
                                        Text(text = "删除", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        RoundToastHost()
    }
}

// ---- 模型 → 卡片（VideoCard）映射 ----

/** 历史条目卡片：副文本 = 已看完/看到 HH:MM:SS/xx播放；无 duration 故进度条不渲染（TODO）。 */
private fun HistoryItem.toVideoCard(): VideoCard {
    val viewText = when {
        progress < 0 -> "已看完"
        progress > 0 -> "看到${formatDuration(progress)}"
        view > 0 -> "${formatCount(view)}播放"
        else -> ""
    }
    return VideoCard(
        title = title,
        upName = uname,
        view = viewText,
        cover = pic.fixCoverUrl(),
        type = "video",
        bvid = bvid,
        progressPercent = null,
    )
}

/** 稍后再看条目卡片：副文本 = xx播放 · 时长。 */
private fun VideoItem.toVideoCard(): VideoCard {
    val viewText = buildString {
        if (view > 0) append("${formatCount(view)}播放")
        if (duration > 0) {
            if (isNotEmpty()) append(" · ")
            append(formatDuration(duration))
        }
    }
    return VideoCard(
        title = title,
        upName = "",
        view = viewText,
        cover = pic.fixCoverUrl(),
        type = "video",
        bvid = bvid,
        progressPercent = null,
    )
}

/** 秒 → "H:MM:SS" 或 "M:SS"（替代 Android StringUtil.toTime，避免 java String.format）。 */
private fun formatDuration(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    val two = { n: Long -> if (n < 10) "0$n" else "$n" }
    return if (h > 0) "$h:${two(m)}:${two(sec)}" else "${two(m)}:${two(sec)}"
}
