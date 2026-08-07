package com.qx.orbit.ohos.pages

import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.data.FavoriteFolder
import com.qx.orbit.ohos.data.VideoItem
import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
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
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule

/**
 * 我的收藏夹列表页（对齐 Android FavoriteFoldersScreen 核心布局）。
 */
@Page("favorites")
internal class FavoritesPage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            FavoritesContent(client)
        }
    }
}

/**
 * 收藏夹详情页（对齐 Android FavoriteDetailScreen 精简版）。
 */
@Page("favoriteDetail")
internal class FavoriteDetailPage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            FavoriteDetailContent(client)
        }
    }
}

@Composable
internal fun FavoritesContent(client: BiliClient) {
    var folders by remember { mutableStateOf<List<FavoriteFolder>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            folders = client.favoriteFolders()
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
                modifier = Modifier.padding(8.dp),
            )
        }
        when {
            loading && folders.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            folders.isEmpty() -> {
                Text(
                    text = "暂无收藏夹",
                    fontSize = 12.sp,
                    color = OrbitColors.TextSecondary,
                    modifier = Modifier.padding(10.dp),
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                    for (folder in folders) {
                        FavoriteFolderRow(folder = folder, onClick = {
                            PageArgs.set("fid", folder.id)
                            RouterModule().openPage("favoriteDetail", null)
                        })
                    }
                }
            }
        }
    }
}

@Composable
internal fun FavoriteFolderRow(folder: FavoriteFolder, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.CardBackground)
            .padding(10.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = folder.title,
            fontSize = 13.sp,
            color = OrbitColors.OnSurface,
            maxLines = 1,
        )
        Text(
            text = "${folder.mediaCount}个内容",
            fontSize = 11.sp,
            color = OrbitColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
internal fun FavoriteDetailContent(client: BiliClient) {
    val fid = PageArgs.getLong("fid")
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(fid) {
        if (fid <= 0) {
            error = "缺少收藏夹参数"
            loading = false
            return@LaunchedEffect
        }
        try {
            videos = client.favoriteDetail(fid)
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
                modifier = Modifier.padding(8.dp),
            )
        }
        when {
            loading && videos.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            videos.isEmpty() -> {
                Text(
                    text = "暂无内容",
                    fontSize = 12.sp,
                    color = OrbitColors.TextSecondary,
                    modifier = Modifier.padding(10.dp),
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                    for (video in videos) {
                        FavoriteVideoRow(video = video, onClick = {
                            PageArgs.set("bvid", video.bvid)
                            RouterModule().openPage("videoDetail", null)
                        })
                    }
                }
            }
        }
    }
}

@Composable
internal fun FavoriteVideoRow(video: VideoItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.CardBackground)
            .padding(10.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = video.title,
            fontSize = 13.sp,
            color = OrbitColors.OnSurface,
            maxLines = 2,
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(
                text = "${formatCount(video.view)}播放",
                fontSize = 11.sp,
                color = OrbitColors.TextSecondary,
            )
        }
    }
}
