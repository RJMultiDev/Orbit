package com.qx.orbit.ohos.pages

import com.qx.orbit.ohos.data.BangumiDetail
import com.qx.orbit.ohos.data.BangumiEp
import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.data.bangumiInfo
import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
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
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.CircularProgressIndicator
import com.tencent.kuikly.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule

/**
 * 番剧详情页（对齐 Android BangumiDetailScreen 核心布局的简化版）。
 * 参数：PageArgs["season_id"]（seasonId），由搜索结果/追番列表跳入。
 */
@Page("bangumi")
internal class BangumiPage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            BangumiContent(client)
        }
    }
}

@Composable
internal fun BangumiContent(client: BiliClient) {
    val seasonId = PageArgs.getLong("season_id")
    var detail by remember { mutableStateOf<BangumiDetail?>(null) }
    var error by remember { mutableStateOf("") }
    // 追番状态：当前仅本地切换；真实状态/接口见 data/BangumiApi.kt 的 TODO
    var followed by remember { mutableStateOf(false) }

    LaunchedEffect(seasonId) {
        if (seasonId <= 0L) {
            error = "缺少番剧参数"
            return@LaunchedEffect
        }
        try {
            detail = client.bangumiInfo(seasonId)
        } catch (e: Throwable) {
            error = e.message ?: "加载失败"
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        val d = detail
        if (d == null) {
            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    fontSize = 12.sp,
                    color = OrbitColors.ErrorRed,
                    modifier = Modifier.padding(10.dp),
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.padding(10.dp))
            }
            return@Column
        }

        // 季番标题
        Text(
            text = d.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = OrbitColors.OnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        )

        // 封面占位 + 评分
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(96.dp)
                    .background(OrbitColors.SurfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                val coverUrl = coverUrlOf(d.cover)
                if (coverUrl.isEmpty()) {
                    Text(text = "无封面", fontSize = 10.sp, color = OrbitColors.TextSecondary)
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(coverUrl),
                        contentDescription = "封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = if (d.score > 0f) "评分 ${formatScore(d.score)}" else "暂无评分",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (d.score > 0f) OrbitColors.Primary else OrbitColors.TextSecondary,
                )
                Text(
                    text = "共 ${d.eps.size} 话",
                    fontSize = 11.sp,
                    color = OrbitColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        // 简介（可读性截断；展开/收起后续可做）
        if (d.desc.isNotEmpty()) {
            Text(
                text = d.desc,
                fontSize = 11.sp,
                color = OrbitColors.OnSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        // 追番/追剧（状态切换，接口 TODO）
        Button(
            onClick = { followed = !followed },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(text = if (followed) "已追番" else "追番", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 正片剧集列表
        Text(
            text = "正片",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OrbitColors.OnSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
        for (ep in d.eps) {
            BangumiEpRow(ep = ep, onClick = {
                // 进播放页：正片多为 pgc 无 bvid，携带 cid + season/ep 供取流
                PageArgs.set("bvid", ep.bvid)
                PageArgs.set("cid", ep.cid)
                PageArgs.set("season_id", seasonId)
                PageArgs.set("ep", ep.id)
                RouterModule().openPage("player", null)
            })
        }
    }
}

@Composable
private fun BangumiEpRow(ep: BangumiEp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.CardBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ep.title,
            fontSize = 12.sp,
            color = OrbitColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        val duration = formatDuration(ep.duration)
        if (duration.isNotEmpty()) {
            Text(
                text = duration,
                fontSize = 10.sp,
                color = OrbitColors.TextSecondary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** 封面 URL 修复（// 或 http 补全 https）并附 3:4 处理后缀。 */
private fun coverUrlOf(raw: String): String {
    if (raw.isEmpty()) return ""
    val secure = when {
        raw.startsWith("//") -> "https:$raw"
        raw.startsWith("http://") -> raw.replaceFirst("http://", "https://")
        else -> raw
    }
    return if (secure.contains("@")) secure else "${secure}@240w_320h_1c.webp"
}

/** 评分保留一位小数（避免 String.format）。 */
private fun formatScore(score: Float): String {
    if (score <= 0f) return "暂无评分"
    val tenths = (score * 10f + 0.5f).toInt()
    return "${tenths / 10}.${tenths % 10}"
}

/**
 * 时长格式化为 mm:ss / h:mm:ss。
 * pgc 剧集 duration 口径在秒/毫秒间不一致，>= 60_000 视为毫秒归一为秒。
 */
private fun formatDuration(duration: Long): String {
    if (duration <= 0L) return ""
    val seconds = if (duration >= 60_000L) duration / 1000L else duration
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "${h}:${pad2(m)}:${pad2(s)}" else "${m}:${pad2(s)}"
}

private fun pad2(v: Long): String = if (v < 10) "0$v" else "$v"
