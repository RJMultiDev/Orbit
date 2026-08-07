package com.qx.orbit.ohos.pages

import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.data.PlayControlApi
import com.qx.orbit.ohos.data.PlayUrl
import com.qx.orbit.ohos.data.VideoDetail
import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.CircularProgressIndicator
import com.tencent.kuikly.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 播放页（对齐 Android PlayerScreen 核心控制）。
 *
 * 职责边界：媒体播放由宿主 AVPlayer 承接，本页只负责业务取流、状态展示与
 * 控制意图收集，最终可播地址经 PageArgs["playUrl"] 暴露给宿主。
 *
 * 对接计划（C20）：
 * 1. 宿主侧实现 OrbitPlaybackModule（AVPlayer 封装）或在 ArkTS 中订阅
 *    PageArgs.getString("playUrl") 触发原生播放，本页负责在每次重取流后刷新该键。
 * 2. 播放/暂停、±10s seek、倍速、音量目前为页面内状态模拟（UI 先行），
 *    桥接后经 OrbitPlaybackModule 下发到 AVPlayer；进度改为读取真实播放位置。
 * 3. 画质（qn）与分 P 切换在本页重新取流后再次更新 "playUrl"。
 */
@Page("player")
internal class PlayerPage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            PlayerContent(client)
        }
    }
}

@Composable
internal fun PlayerContent(client: BiliClient) {
    val bvid = PageArgs.getString("bvid")
    val playApi = remember { PlayControlApi(client) }
    val scope = rememberCoroutineScope()

    var cid by remember { mutableStateOf(PageArgs.getLong("cid")) }
    var detail by remember { mutableStateOf<VideoDetail?>(null) }
    var playUrl by remember { mutableStateOf<PlayUrl?>(null) }
    var status by remember { mutableStateOf("加载中…") }
    var loading by remember { mutableStateOf(false) }

    // 以下为页面内模拟状态：桥接 AVPlayer 后由真实播放位置/播放器状态驱动。
    var isPlaying by remember { mutableStateOf(false) }
    var currentTimeMs by remember { mutableStateOf(0L) }
    var totalDurationMs by remember { mutableStateOf(0L) }
    var speed by remember { mutableStateOf(1.0f) }
    var volume by remember { mutableStateOf(50) }
    var currentQn by remember { mutableStateOf(DEFAULT_QN) }
    var pageIndex by remember { mutableStateOf(0) }

    suspend fun loadStream() {
        if (loading) return
        loading = true
        isPlaying = false
        status = "取流中…"
        val targetCid = cid.takeIf { it > 0 } ?: detail?.cid ?: 0L
        try {
            val pu = playApi.playUrl(bvid, targetCid, currentQn)
            if (pu == null) {
                status = "取流失败：返回空流"
                return
            }
            playUrl = pu
            totalDurationMs = pu.durationMs.takeIf { it > 0 } ?: (detail?.duration ?: 0L) * 1000
            currentTimeMs = 0
            val url = resolvePrimaryUrl(pu)
            PageArgs.set("playUrl", url)
            val effQn = pu.quality.takeIf { it > 0 } ?: currentQn
            status = "已就绪 · ${qualityLabel(effQn)} · ${formatTime(totalDurationMs)}"
        } catch (e: Throwable) {
            status = "取流失败：${e.message ?: "网络错误"}"
        } finally {
            loading = false
        }
    }

    fun seekBy(deltaMs: Long) {
        val total = totalDurationMs
        currentTimeMs = (currentTimeMs + deltaMs)
            .coerceIn(0L, if (total > 0) total else Long.MAX_VALUE)
        // TODO(播放桥接)：seek 下发至宿主 AVPlayer（OrbitPlaybackModule）
    }

    suspend fun switchQuality(qn: Int) {
        if (qn == currentQn) return
        currentQn = qn
        loadStream()
    }

    suspend fun switchPage(index: Int) {
        val pages = detail?.pages ?: return
        if (index !in pages.indices || index == pageIndex) return
        val p = pages[index]
        pageIndex = index
        cid = p.cid
        PageArgs.set("cid", p.cid)
        currentTimeMs = 0
        loadStream()
    }

    LaunchedEffect(bvid) {
        if (bvid.isEmpty()) {
            status = "缺少视频参数"
            return@LaunchedEffect
        }
        try {
            val d = client.videoDetail(bvid)
            detail = d
            if (cid <= 0) cid = d?.cid ?: 0L
            val idx = d?.pages?.indexOfFirst { it.cid == cid } ?: -1
            pageIndex = if (idx >= 0) idx else 0
        } catch (_: Throwable) {
            // 详情失败不阻断取流（cid 仍来自 PageArgs）
        }
        loadStream()
    }

    // 进度模拟：播放中每秒推进（按倍速）；桥接后改为读取 AVPlayer 位置。
    LaunchedEffect(isPlaying, playUrl) {
        while (isPlaying) {
            delay(1000)
            val step = (1000f * speed).toLong()
            val next = currentTimeMs + step
            if (totalDurationMs > 0 && next >= totalDurationMs) {
                currentTimeMs = totalDurationMs
                isPlaying = false
            } else {
                currentTimeMs = next
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(OrbitColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = status,
            fontSize = 12.sp,
            color = if (status.startsWith("取流失败") || status == "缺少视频参数") {
                OrbitColors.ErrorRed
            } else {
                OrbitColors.TextSecondary
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        )

        if (loading && playUrl == null) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 10.dp))
        }

        playUrl?.let { pu ->
            val progress = if (totalDurationMs > 0) {
                (currentTimeMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
            } else {
                0f
            }

            // 进度条（展示用；点击 seek 依赖手势 offset，先以 ±10s 按钮代替，TODO 手势）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(6.dp)
                    .background(OrbitColors.SurfaceContainerHigh),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(OrbitColors.BiliPink),
                )
            }

            Text(
                text = "${formatTime(currentTimeMs)} / ${formatTime(totalDurationMs)}",
                fontSize = 11.sp,
                color = OrbitColors.OnSurface,
                modifier = Modifier.padding(top = 2.dp),
            )

            // 播放控制：±10s / 播放暂停 / 倍速循环（0.75/1.0/1.25/1.5/2.0）
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { seekBy(-SEEK_STEP_MS) },
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                ) {
                    Text(text = "-10s", fontSize = 10.sp)
                }
                Button(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                ) {
                    Text(text = if (isPlaying) "暂停" else "播放", fontSize = 10.sp)
                }
                Button(
                    onClick = { seekBy(SEEK_STEP_MS) },
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                ) {
                    Text(text = "+10s", fontSize = 10.sp)
                }
                Button(
                    onClick = { speed = nextSpeed(speed) },
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                ) {
                    Text(text = speedLabel(speed), fontSize = 9.sp)
                }
            }

            // 音量控制（0-100 增减）
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { volume = (volume - VOLUME_STEP).coerceAtLeast(0) }) {
                    Text(text = "音量-", fontSize = 10.sp)
                }
                Text(text = "${volume}%", fontSize = 12.sp, color = OrbitColors.OnSurface)
                Button(onClick = { volume = (volume + VOLUME_STEP).coerceAtMost(100) }) {
                    Text(text = "音量+", fontSize = 10.sp)
                }
            }

            // 画质选择（qn 档位，重取流）
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for ((label, qn) in QUALITY_OPTIONS) {
                    SelectChip(
                        label = label,
                        selected = qn == currentQn,
                        onClick = { scope.launch { switchQuality(qn) } },
                    )
                }
            }

            // 分 P 选择（多 P 时展示；切换后走 PageArgs cid 重新取流）
            val pages = detail?.pages ?: emptyList()
            if (pages.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for ((i, _) in pages.withIndex()) {
                        SelectChip(
                            label = "P${i + 1}",
                            selected = i == pageIndex,
                            onClick = { scope.launch { switchPage(i) } },
                        )
                    }
                }
                detail?.pages?.getOrNull(pageIndex)?.let { p ->
                    Text(
                        text = "P${pageIndex + 1} ${p.part}",
                        fontSize = 9.sp,
                        color = OrbitColors.TextSecondary,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    )
                }
            }

            // 可播地址（供宿主 AVPlayer 接管）
            val url = resolvePrimaryUrl(pu)
            if (url.isNotEmpty()) {
                Text(
                    text = url,
                    fontSize = 8.sp,
                    color = OrbitColors.OnSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                )
            }
        }

        Button(
            onClick = { RouterModule().openPage("videoDetail", null) },
            modifier = Modifier.padding(top = 6.dp),
        ) {
            Text(text = "返回", fontSize = 12.sp)
        }
    }
}

/** 简洁档位选择块（画质 / 分 P 共用）。 */
@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 10.sp,
        color = if (selected) OrbitColors.BiliPink else OrbitColors.OnSurface,
        modifier = Modifier
            .background(if (selected) OrbitColors.PrimaryContainer else OrbitColors.CardBackground)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
    )
}

private val QUALITY_OPTIONS = listOf("360" to 16, "480" to 32, "720" to 64, "1080" to 80)

private const val DEFAULT_QN = 64
private const val SEEK_STEP_MS = 10_000L
private const val VOLUME_STEP = 10

private fun resolvePrimaryUrl(pu: PlayUrl): String =
    pu.durl.firstOrNull() ?: pu.dashVideo.firstOrNull()?.baseUrl ?: ""

private fun qualityLabel(qn: Int): String {
    for ((label, value) in QUALITY_OPTIONS) if (value == qn) return "${label}p"
    return "${qn}p"
}

private fun speedLabel(speed: Float): String {
    val s = when (speed) {
        0.75f -> "0.75"
        1.0f -> "1.0"
        1.25f -> "1.25"
        1.5f -> "1.5"
        2.0f -> "2.0"
        else -> speed.toString()
    }
    return "${s}x"
}

private fun nextSpeed(current: Float): Float {
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val idx = speeds.indexOf(current)
    return speeds[(idx + 1) % speeds.size]
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "${pad2(h)}:${pad2(m)}:${pad2(s)}" else "${pad2(m)}:${pad2(s)}"
}

private fun pad2(n: Long): String = if (n < 10) "0$n" else "$n"
