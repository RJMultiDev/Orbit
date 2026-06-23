package com.qx.orbit.bili.presentation.player

import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.basicMarquee
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import com.qx.orbit.bili.R
import com.qx.orbit.bili.data.api.PlayerApi
import com.qx.orbit.bili.data.model.PlayerData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import master.flame.danmaku.ui.widget.DanmakuView
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.parser.android.BiliProtobufDanmakuParser
import com.qx.orbit.bili.data.api.DanmakuApi

@Composable
fun PlayerScreen(
    initialData: PlayerData,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var playerData by remember { mutableStateOf(initialData) }
    var showDanmaku by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val mediaPlayer = remember { IjkMediaPlayer() }
    val danmakuView = remember { DanmakuView(context) }
    var surfaceHolder by remember { mutableStateOf<SurfaceHolder?>(null) }
    var videoWidth by remember { mutableFloatStateOf(16f) }
    var videoHeight by remember { mutableFloatStateOf(9f) }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(isPlaying, isPrepared) {
        if (isPrepared) {
            if (isPlaying) danmakuView.resume() else danmakuView.pause()
        }
        while(isPlaying && isPrepared) {
            currentProgress = mediaPlayer.currentPosition
            delay(1000)
        }
    }

    LaunchedEffect(showDanmaku) {
        if (showDanmaku) danmakuView.show() else danmakuView.hide()
    }

    LaunchedEffect(playerData.cid) {
        isLoading = true
        try {
            if (!com.qx.orbit.bili.data.remote.CookieManager.getCookie().contains("buvid3")) {
                com.qx.orbit.bili.data.api.CookiesApi.checkCookies()
            }
            
            val danmakuSegment = DanmakuApi.getVideoDanmakuSegment(playerData.aid, playerData.cid, 1)
            val parser = BiliProtobufDanmakuParser()
            if (danmakuSegment != null) {
                parser.setDanmakuSegments(listOf(danmakuSegment))
            }
            val danmakuContext = DanmakuContext.create().apply {
                setDuplicateMergingEnabled(false)
                setScaleTextSize(0.8f)
                setDanmakuTransparency(0.4f)
            }
            danmakuView.prepare(parser, danmakuContext)
            danmakuView.enableDanmakuDrawingCache(true)

            val result = PlayerApi.getVideo(playerData)
            playerData = result
            if (result.videoUrl.isNotEmpty()) {
                mediaPlayer.reset()
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "allowed_extensions", "ALL")
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", "file,http,https,tcp,tls,crypto")
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user_agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "headers", "Referer: https://www.bilibili.com")
                mediaPlayer.dataSource = result.videoUrl
                surfaceHolder?.let { mediaPlayer.setDisplay(it) }
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener {
                    isPrepared = true
                    isLoading = false
                    totalDuration = it.duration
                    it.start()
                    isPlaying = true
                    danmakuView.start()
                    
                    // Report heartbeat on start
                    scope.launch {
                        try {
                            com.qx.orbit.bili.data.api.HeartbeatApi.reportHeartbeat(
                                aid = playerData.aid,
                                bvid = playerData.bvid,
                                cid = playerData.cid,
                                playedTime = 0
                            )
                        } catch (e: Exception) {}
                    }
                }
                mediaPlayer.setOnVideoSizeChangedListener { _, width, height, _, _ ->
                    if (width > 0 && height > 0) {
                        videoWidth = width.toFloat()
                        videoHeight = height.toFloat()
                    }
                }
                mediaPlayer.setOnErrorListener { _, what, extra ->
                    errorMessage = "播放器错误: $what"
                    true
                }
            } else {
                errorMessage = "无法获取视频地址"
                isLoading = false
            }
        } catch (e: Exception) {
            errorMessage = "网络请求失败"
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        val startTs = System.currentTimeMillis() / 1000
        onDispose {
            var currentPosSeconds = 0L
            try {
                currentPosSeconds = mediaPlayer.currentPosition / 1000
            } catch (e: Exception) {}

            kotlinx.coroutines.GlobalScope.launch {
                try {
                    com.qx.orbit.bili.data.api.HistoryApi.reportHistory(playerData.aid, playerData.cid, currentPosSeconds)
                    com.qx.orbit.bili.data.api.HeartbeatApi.reportHeartbeat(
                        aid = playerData.aid,
                        bvid = playerData.bvid,
                        cid = playerData.cid,
                        playedTime = currentPosSeconds,
                        startTs = startTs
                    )
                } catch (e: Exception) {}
            }
            mediaPlayer.release()
            danmakuView.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = {
                        if (isPlaying) {
                            mediaPlayer.pause()
                            danmakuView.pause()
                            isPlaying = false
                            // Report progress on pause
                            scope.launch {
                                try {
                                    val pos = mediaPlayer.currentPosition / 1000
                                    com.qx.orbit.bili.data.api.HistoryApi.reportHistory(playerData.aid, playerData.cid, pos)
                                    com.qx.orbit.bili.data.api.HeartbeatApi.reportHeartbeat(
                                        aid = playerData.aid,
                                        bvid = playerData.bvid,
                                        cid = playerData.cid,
                                        playedTime = pos
                                    )
                                } catch (_: Exception) {}
                            }
                        } else {
                            mediaPlayer.start()
                            danmakuView.resume()
                            isPlaying = true
                        }
                        showControls = true
                    }
                )
            }
            .onRotaryScrollEvent {
                if (isPrepared) {
                    val delta = it.verticalScrollPixels
                    val newProgress = (mediaPlayer.currentPosition + delta * 100).toLong().coerceIn(0L, totalDuration)
                    mediaPlayer.seekTo(newProgress)
                    danmakuView.seekTo(newProgress)
                    currentProgress = newProgress
                    showControls = true
                }
                true
            }
    ) {
        // Video Surface
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(h: SurfaceHolder) {
                                surfaceHolder = h
                                if (isPrepared) mediaPlayer.setDisplay(h)
                            }
                            override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, height: Int) {}
                            override fun surfaceDestroyed(h: SurfaceHolder) {
                                surfaceHolder = null
                                mediaPlayer.setDisplay(null)
                            }
                        })
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(videoWidth / videoHeight)
            )
        }

        AndroidView(
            factory = { danmakuView },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        errorMessage?.let {
            Text(
                text = it,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0x88000000))
                    .padding(8.dp)
            )
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
            ) {
                Text(
                    text = playerData.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp, start = 24.dp, end = 24.dp)
                        .basicMarquee()
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showDanmaku = !showDanmaku }) {
                            Text(
                                text = "弹",
                                color = if (showDanmaku) Color.White else Color.Gray,
                                fontSize = 16.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(onClick = {
                            if (isPlaying) {
                                mediaPlayer.pause()
                                danmakuView.pause()
                            } else {
                                mediaPlayer.start()
                                danmakuView.resume()
                            }
                            isPlaying = !isPlaying
                        }) {
                            Icon(
                                painter = painterResource(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play),
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${formatTime(currentProgress)} / ${formatTime(totalDuration)}",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d", m, s)
}
