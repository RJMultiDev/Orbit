package com.qx.orbit.ohos.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.components.RoundToast
import com.qx.orbit.ohos.components.RoundToastHost
import com.qx.orbit.ohos.components.UserAvatar
import com.qx.orbit.ohos.components.UserNameText
import com.qx.orbit.ohos.components.formatCount
import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.data.VideoDetail
import com.qx.orbit.ohos.data.VideoDetailApi
import com.qx.orbit.ohos.data.favoriteFolders
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
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.ButtonDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import kotlinx.coroutines.launch

/**
 * 视频详情页（对齐 Android VideoDetailScreen 的 VideoInfoPage 核心布局）。
 *
 * 区块顺序：标题 → UP 主行（头像/昵称/关注）→ 数据行（播放/弹幕/赞/币/藏）→
 * 分 P 列表（可点选，PageArgs.set("cid", …)）→ 简介（点击展开/收起）→
 * 评论区预览（TODO）→ 操作行（播放/点赞/投币/收藏/稍后再看）。
 */
@Page("videoDetail")
internal class VideoDetailPage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            VideoDetailContent(client, credential)
        }
    }
}

@Composable
internal fun VideoDetailContent(client: BiliClient, credential: OrbitCredentialModule) {
    val bvid = PageArgs.getString("bvid")
    var detail by remember { mutableStateOf<VideoDetail?>(null) }
    var error by remember { mutableStateOf("") }
    var selectedCid by remember { mutableStateOf(PageArgs.getLong("cid")) }
    var descExpanded by remember { mutableStateOf(false) }
    var liked by remember { mutableStateOf(false) }
    var following by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(bvid) {
        if (bvid.isEmpty()) {
            error = "缺少视频参数"
            return@LaunchedEffect
        }
        try {
            val d = client.videoDetail(bvid)
            detail = d
            // 分 P 默认选中上次传入的 cid（来自首页卡片），无效则回退首 P
            if (d != null && (selectedCid <= 0 || d.pages.none { it.cid == selectedCid })) {
                selectedCid = d.cid
                PageArgs.set("cid", d.cid)
            }
        } catch (e: Throwable) {
            error = e.message ?: "加载失败"
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        val d = detail
        if (d != null) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ---- 1. 标题 ----
                Text(
                    text = d.title,
                    fontSize = 14.sp,
                    color = OrbitColors.OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 8.dp),
                )

                // ---- 2. UP 主行：头像 / 名字 / 关注按钮 ----
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UserAvatar(
                        avatarUrl = d.owner.face,
                        officialRole = 0,
                        modifier = Modifier.size(30.dp),
                        // TODO: 点击进用户空间页（AppRouter.PAGE_USER_SPACE 路由尚未注册 @Page）
                        onClick = {
                            RoundToast.show("用户空间页待接入（TODO）")
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        UserNameText(
                            name = d.owner.name.ifEmpty { "未知UP主" },
                            isVip = false,
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "UP主 · mid ${d.owner.mid}",
                            fontSize = 9.sp,
                            color = OrbitColors.TextSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val r = VideoDetailApi.follow(client, credential, d.owner.mid, !following)
                                if (r.success) following = !following
                                RoundToast.show(r.message)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (following) OrbitColors.SurfaceContainer else OrbitColors.Primary,
                            contentColor = if (following) OrbitColors.Primary else OrbitColors.OnPrimary,
                        ),
                    ) {
                        Text(text = if (following) "已关注" else "关注", fontSize = 10.sp)
                    }
                }

                // ---- 3. 数据行：播放 / 弹幕 / 点赞 / 投币 / 收藏 ----
                Text(
                    text = "${formatCount(d.stat.view)}播放 · ${formatCount(d.stat.danmaku)}弹幕",
                    fontSize = 11.sp,
                    color = OrbitColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                )
                Text(
                    text = "${formatCount(d.stat.like)}赞 · ${formatCount(d.stat.coin)}币 · ${formatCount(d.stat.favorite)}藏",
                    fontSize = 11.sp,
                    color = OrbitColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                )

                // ---- 4. 分 P 列表（可点选，选中后同步 PageArgs.cid） ----
                if (d.pages.size > 1) {
                    Text(
                        text = "分P",
                        fontSize = 11.sp,
                        color = OrbitColors.Primary,
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp),
                    )
                    for (p in d.pages) {
                        val selected = p.cid == selectedCid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                                .clickable(onClick = {
                                    selectedCid = p.cid
                                    PageArgs.set("cid", p.cid)
                                }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "P${p.page}",
                                fontSize = 10.sp,
                                color = if (selected) OrbitColors.Primary else OrbitColors.TextSecondary,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = p.part,
                                fontSize = 10.sp,
                                color = if (selected) OrbitColors.OnSurface else OrbitColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatDuration(p.duration),
                                fontSize = 9.sp,
                                color = OrbitColors.TextSecondary,
                            )
                        }
                    }
                }

                // ---- 5. 简介（点击展开/收起） ----
                Text(
                    text = "简介",
                    fontSize = 11.sp,
                    color = OrbitColors.Primary,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp),
                )
                Text(
                    text = d.desc.ifEmpty { "暂无简介" },
                    fontSize = 11.sp,
                    color = OrbitColors.OnSurface,
                    maxLines = if (descExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { descExpanded = !descExpanded })
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                )
                if (d.desc.length > 48) {
                    Text(
                        text = if (descExpanded) "收起" else "展开",
                        fontSize = 10.sp,
                        color = OrbitColors.Primary,
                        modifier = Modifier
                            .clickable(onClick = { descExpanded = !descExpanded })
                            .padding(start = 10.dp, top = 1.dp),
                    )
                }

                // ---- 6. 评论区预览（前 5 条待接入） ----
                Text(
                    text = "评论 ${formatCount(d.stat.reply)} · 预览待接入（TODO）",
                    fontSize = 11.sp,
                    color = OrbitColors.TextSecondary,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp),
                )
                // TODO: 评论区预览 —— shared/core InteractionRepository.replies(oid=aid, page=1, type=1)
                //       尚未桥接进 ohos 模块（BiliClient 无 replies()）。桥接后在此渲染前 5 条，
                //       展示为「评论」区块；当前仅显示 stat.reply 总数。

                // ---- 7. 操作行：播放 / 点赞 / 投币 / 收藏 / 稍后再看 ----
                Button(
                    onClick = {
                        PageArgs.set("bvid", d.bvid)
                        PageArgs.set("cid", selectedCid)
                        RouterModule().openPage("player", null)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrbitColors.Primary,
                        contentColor = OrbitColors.OnPrimary,
                    ),
                ) {
                    Text(text = "播放", fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                    DetailActionButton(
                        label = "点赞 ${formatCount(d.stat.like)}",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                val r = VideoDetailApi.like(client, credential, d.aid, !liked)
                                if (r.success) {
                                    liked = !liked
                                    detail = detail?.copy(
                                        stat = detail!!.stat.copy(
                                            like = (detail!!.stat.like + if (liked) 1 else -1).coerceAtLeast(0),
                                        )
                                    )
                                }
                                RoundToast.show(r.message)
                            }
                        },
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    DetailActionButton(
                        label = "投币 ${formatCount(d.stat.coin)}",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                val r = VideoDetailApi.coin(client, credential, d.aid, 1)
                                if (r.success) {
                                    detail = detail?.copy(
                                        stat = detail!!.stat.copy(coin = detail!!.stat.coin + 1),
                                    )
                                }
                                RoundToast.show(r.message)
                            }
                        },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    DetailActionButton(
                        label = "收藏 ${formatCount(d.stat.favorite)}",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                // 收藏需选收藏夹：先取默认收藏夹（现有 favoriteFolders()），无则提示
                                val folders = client.favoriteFolders()
                                val folder = folders.firstOrNull()
                                if (folder == null) {
                                    RoundToast.show("暂无收藏夹或未登录")
                                    return@launch
                                }
                                val r = VideoDetailApi.favorite(client, credential, d.aid, folder.id)
                                if (r.success) {
                                    detail = detail?.copy(
                                        stat = detail!!.stat.copy(favorite = detail!!.stat.favorite + 1),
                                    )
                                }
                                RoundToast.show(r.message)
                            }
                        },
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    DetailActionButton(
                        label = "稍后再看",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                val r = VideoDetailApi.watchLater(client, credential, d.aid)
                                RoundToast.show(r.message)
                            }
                        },
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                Text(
                    text = if (error.isNotEmpty()) error else "加载中…",
                    fontSize = 12.sp,
                    color = if (error.isNotEmpty()) OrbitColors.ErrorRed else OrbitColors.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        RoundToastHost()
    }
}

/** 次级操作按钮（点赞/投币/收藏/稍后再看）：暗底胶囊，短标签。 */
@Composable
private fun DetailActionButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = OrbitColors.SurfaceContainer,
            contentColor = OrbitColors.OnSurface,
        ),
    ) {
        Text(text = label, fontSize = 10.sp, maxLines = 1)
    }
}

/** 秒 → "M:SS" / "H:MM:SS"（替代 Android StringUtil.toTime，避免 java String.format）。 */
private fun formatDuration(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    fun two(n: Long): String = if (n < 10) "0$n" else "$n"
    return if (h > 0) "$h:${two(m)}:${two(sec)}" else "${two(m)}:${two(sec)}"
}
