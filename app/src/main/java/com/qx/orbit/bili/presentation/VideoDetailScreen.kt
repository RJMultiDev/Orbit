package com.qx.orbit.bili.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableIntStateOf
import com.qx.orbit.bili.data.api.UserInfoApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Dialog
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.gson.Gson
import com.qx.orbit.bili.R
import com.qx.orbit.bili.data.api.BilibiliIDConverter
import com.qx.orbit.bili.data.model.Emote
import com.qx.orbit.bili.data.model.PlayerData
import com.qx.orbit.bili.data.model.Reply
import com.qx.orbit.bili.data.model.VideoCard
import com.qx.orbit.bili.data.model.VideoInfo
import com.qx.orbit.bili.presentation.ui.components.RecommendVideoCard
import com.qx.orbit.bili.presentation.viewmodel.VideoDetailViewModel
import com.qx.orbit.bili.util.LinkResolver
import com.qx.orbit.bili.util.SharedPreferencesUtil
import com.qx.orbit.bili.util.formatCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.core.net.toUri

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoDetailScreen(navController: NavHostController, bvid: String, aid: Long, viewModel: VideoDetailViewModel = viewModel()) {
    LaunchedEffect(bvid, aid) {
        viewModel.loadData(bvid, aid)
    }

    val videoInfo by viewModel.videoInfo.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val replies by viewModel.replies.collectAsState()
    val replyCount by viewModel.replyCount.collectAsState()
    val relatedVideos by viewModel.relatedVideos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showCoinDialog by remember { mutableStateOf(false) }
    var showFavDialog by remember { mutableStateOf(false) }
    
    val emotes by viewModel.emotes.collectAsState()
    val isEmoteLoading by viewModel.isEmoteLoading.collectAsState()
    var showWriteReply by remember { mutableStateOf(false) }
    var replyTarget by remember { mutableStateOf<Reply?>(null) }
    val pagerState = rememberPagerState(pageCount = { 3 })
    
    // Focus requesters for rotary input for each page
    val focusRequesters = remember { List(3) { FocusRequester() } }
    
    LaunchedEffect(pagerState.currentPage) {
        try {
            focusRequesters[pagerState.currentPage].requestFocus()
        } catch (e: Exception) {
            // Ignore focus exceptions
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            AnimatedContent(
                targetState = isLoading && videoInfo == null,
                transitionSpec = { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) },
                label = "LoadingAnimation"
            ) { isInitialLoading ->
                if (isInitialLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                val context = LocalContext.current
                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                    when (page) {
                        0 -> VideoInfoPage(
                            videoInfo = videoInfo, 
                            tags = tags, 
                            focusRequester = focusRequesters[0],
                            onPlayClick = {
                                val info = videoInfo ?: return@VideoInfoPage
                                // Launch PlayerActivity
                                val qn = SharedPreferencesUtil.getInt("play_qn", 16)
                                val playerData = PlayerData(
                                    title = info.title,
                                    aid = aid,
                                    cid = info.cids.firstOrNull() ?: 0L,
                                    cids = info.cids,
                                    pagenames = info.pagenames,
                                    type = if (info.epid > 0) PlayerData.TYPE_BANGUMI else PlayerData.TYPE_VIDEO,
                                    qn = qn
                                )
                                val jsonStr = Gson().toJson(playerData)
                                val encodedJson = URLEncoder.encode(jsonStr, StandardCharsets.UTF_8.toString())
                                navController.navigate("player/$encodedJson")
                            },
                            onLikeClick = { viewModel.toggleLike() },
                            onCoinClick = { showCoinDialog = true },
                            onFavClick = { 
                                viewModel.loadFavoriteFolders()
                                showFavDialog = true
                            },
                            onUpClick = { mid ->
                                navController.navigate("user_space/$mid")
                            }
                        )
                        1 -> VideoCommentsPage(
                            replies = replies, 
                            replyCount = replyCount,
                            focusRequester = focusRequesters[1],
                            navController = navController,
                            onLoadMore = { viewModel.loadReplies() },
                            onClick = { reply ->
                                val json = Gson().toJson(reply)
                                val encoded = URLEncoder.encode(json, "UTF-8")
                                navController.navigate("reply_detail/$encoded")
                            },
                            onLikeClick = { reply -> viewModel.likeReply(reply.rpid, reply.liked) },
                            onReplyClick = { reply ->
                                replyTarget = reply
                                viewModel.loadEmotes()
                                showWriteReply = true
                            },
                            onSendCommentClick = {
                                replyTarget = null
                                viewModel.loadEmotes()
                                showWriteReply = true
                            }
                        )
                        2 -> VideoRelatedPage(
                            relatedVideos = relatedVideos, 
                            focusRequester = focusRequesters[2],
                            navController = navController
                        )
                    }
                }
                
                HorizontalPageIndicator(
                    pagerState = pagerState,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                )
            }
            }
        }
    }
    
    WriteReplyScreen(
        visible = showWriteReply,
        targetName = replyTarget?.sender?.name,
        emotes = emotes,
        onSend = { text ->
            viewModel.sendReply(
                text = text,
                root = replyTarget?.root?.takeIf { it > 0 } ?: replyTarget?.rpid ?: 0L,
                parent = replyTarget?.rpid ?: 0L,
                onSuccess = {
                    showWriteReply = false
                },
                onError = { error ->
                    showWriteReply = false
                }
            )
        },
        onClose = { showWriteReply = false }
    )
    
    var currentCoins by remember { mutableIntStateOf(-1) }
    LaunchedEffect(showCoinDialog) {
        if (showCoinDialog) {
            try {
                currentCoins = UserInfoApi.getCurrentUserCoin()
            } catch (e: Exception) {}
        }
    }
    
    Dialog(visible = showCoinDialog, onDismissRequest = { showCoinDialog = false }) {
        var selectedCoinIndex by remember { mutableIntStateOf(0) }
        val rowOffsetX by animateDpAsState(targetValue = if (selectedCoinIndex == 0) 45.dp else (-45).dp)
        
        val scaleBox1 by animateFloatAsState(targetValue = if (selectedCoinIndex == 0) 1f else 0.8f)
        val scaleBox2 by animateFloatAsState(targetValue = if (selectedCoinIndex == 1) 1f else 0.8f)
        
        val dragYDpAnim = remember { androidx.compose.animation.core.Animatable(0f) }
        val blockJumpAnim = remember { androidx.compose.animation.core.Animatable(0f) }
        var isHit by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val density = LocalDensity.current
        
        LaunchedEffect(isHit) {
            if (isHit) {
                blockJumpAnim.animateTo(-15f, tween(100))
                blockJumpAnim.animateTo(0f, tween(100))
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                if (!isHit) {
                                    if (dragAmount < -10) selectedCoinIndex = 1
                                    else if (dragAmount > 10) selectedCoinIndex = 0
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.offset(x = rowOffsetX),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp).offset(y = if (selectedCoinIndex == 0) blockJumpAnim.value.dp else 0.dp).graphicsLayer { scaleX = scaleBox1; scaleY = scaleBox1 }) {
                            Image(
                                painter = painterResource(R.drawable.ic_pay_coins_box),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp)
                            )
                            Image(
                                painter = painterResource(R.drawable.ic_coins_one),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp).offset(y = if (selectedCoinIndex == 1) blockJumpAnim.value.dp else 0.dp).graphicsLayer { scaleX = scaleBox2; scaleY = scaleBox2 }) {
                            Image(
                                painter = painterResource(R.drawable.ic_pay_coins_box),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp)
                            )
                            Image(
                                painter = painterResource(R.drawable.ic_coins_two),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Image(
                    painter = painterResource(if (selectedCoinIndex == 0) R.drawable.ic_22_mario else R.drawable.ic_22_gun_sister),
                    contentDescription = "2233",
                    modifier = Modifier
                        .size(72.dp)
                        .offset(y = dragYDpAnim.value.dp)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                if (!isHit) {
                                    isHit = true
                                    coroutineScope.launch {
                                        dragYDpAnim.animateTo(-30f, tween(150))
                                        viewModel.doCoin(selectedCoinIndex + 1)
                                        dragYDpAnim.animateTo(0f, tween(150))
                                        delay(100)
                                        showCoinDialog = false
                                    }
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (!isHit) {
                                        coroutineScope.launch {
                                            dragYDpAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                        }
                                    }
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    if (!isHit) {
                                        val dragDp = with(density) { dragAmount.toDp().value }
                                        coroutineScope.launch {
                                            val nextY = (dragYDpAnim.value + dragDp).coerceAtMost(0f)
                                            if (nextY <= -30f && dragYDpAnim.value > -30f) {
                                                isHit = true
                                                dragYDpAnim.snapTo(-30f)
                                                viewModel.doCoin(selectedCoinIndex + 1)
                                                dragYDpAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                                delay(100)
                                                showCoinDialog = false
                                            } else if (nextY > -30f) {
                                                dragYDpAnim.snapTo(nextY)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (currentCoins >= 0) "剩余硬币: $currentCoins" else "剩余硬币: 获取中...",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
    
    val folders by viewModel.favoriteFolders.collectAsState()
    Dialog(visible = showFavDialog, onDismissRequest = { showFavDialog = false }) {
            val listState = rememberTransformingLazyColumnState()
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                try { focusRequester.requestFocus() } catch (e: Exception) {}
            }
            ScreenScaffold(
                scrollState = listState,
                modifier = Modifier.focusRequester(focusRequester).focusable()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    if (folders == null) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (folders!!.isEmpty()) {
                        Text("暂无收藏夹", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
                    } else {
                        TransformingLazyColumn(state = listState, contentPadding = PaddingValues(16.dp)) {
                            item { ListHeader { Text("选择收藏夹", color = MaterialTheme.colorScheme.primary) } }
                            items(folders!!.size) { index ->
                                val folder = folders!![index]
                                Button(
                                    onClick = { 
                                        viewModel.doFavorite(folder.id)
                                        showFavDialog = false 
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (folder.isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = folder.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (folder.isFav) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${folder.mediaCount} 个视频",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (folder.isFav) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}
@Composable
fun VideoInfoPage(
    videoInfo: VideoInfo?, 
    tags: String, 
    focusRequester: FocusRequester,
    onPlayClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCoinClick: () -> Unit,
    onFavClick: () -> Unit,
    onUpClick: (Long) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val isRound = LocalConfiguration.current.isScreenRound

    ScreenScaffold(scrollState = listState, modifier = Modifier.focusRequester(focusRequester)) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
            if (videoInfo != null) {
                // 1. Cover Image
                item {
                    val secureCover = videoInfo.cover.replace("http://", "https://")
                    val coverUrl = if (secureCover.contains("@")) secureCover else "${secureCover}@480w_270h_1c.webp"
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                if (isRound) {
                                    with(transformationSpec) {
                                        applyContainerTransformation(scrollProgress)
                                    }
                                }
                            }
                            .fillMaxWidth(0.95f)
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPlayClick() }
                            
                    )
                }
                
                // 2. Title (slightly larger)
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = videoInfo.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                if (isRound) {
                                    with(transformationSpec) {
                                        applyContainerTransformation(scrollProgress)
                                    }
                                }
                            }
                            .fillMaxWidth()
                    )
                }
                
                // 3. UP Info Capsule
                item {
                    val upInfo = videoInfo.staff.firstOrNull()
                    if (upInfo != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                                .graphicsLayer {
                                    if (isRound) {
                                        with(transformationSpec) {
                                            applyContainerTransformation(scrollProgress)
                                        }
                                    }
                                }
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable(onClick = { onUpClick(upInfo.mid) })
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (upInfo.avatar.isNotEmpty()) {
                                AsyncImage(
                                    model = upInfo.avatar,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(8.dp))
                            
                            Column {
                                Text(
                                    text = "UP主",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = upInfo.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                
                // 4. Small Metadata
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                if (isRound) {
                                    with(transformationSpec) {
                                        applyContainerTransformation(scrollProgress)
                                    }
                                }
                            }
                            .fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Views",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = formatCount(videoInfo.stats?.view ?: 0),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "Danmaku",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = formatCount(videoInfo.stats?.danmaku ?: 0),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${videoInfo.timeDesc} · ${videoInfo.bvid}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
                
                // 5. Tags
                item {
                    if (tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tags,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .transformedHeight(this, transformationSpec)
                                .graphicsLayer {
                                    if (isRound) {
                                        with(transformationSpec) {
                                            applyContainerTransformation(scrollProgress)
                                        }
                                    }
                                }
                                .fillMaxWidth()
                        )
                    }
                }
                
                // 6. Play Button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onPlayClick,
                        modifier = Modifier
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                if (isRound) {
                                    with(transformationSpec) {
                                        applyContainerTransformation(scrollProgress)
                                    }
                                }
                            }
                            .fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("播放视频")
                    }
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
                // 7. Triple Actions
                item {
                    val isLiked = videoInfo.stats?.liked == true
                    val isCoined = (videoInfo.stats?.coined ?: 0) > 0
                    val isFav = videoInfo.stats?.favoured == true
                    
                    val activeColor = Color(0xFFfb8799)
                    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
                    
                    val likeInteractionSource = remember { MutableInteractionSource() }
                    val coinInteractionSource = remember { MutableInteractionSource() }
                    val favInteractionSource = remember { MutableInteractionSource() }
                    
                    ButtonGroup(
                        modifier = Modifier
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                if (isRound) {
                                    with(transformationSpec) {
                                        applyContainerTransformation(scrollProgress)
                                    }
                                }
                            }
                            .fillMaxWidth()
                    ) {
                        FilledIconButton(
                            onClick = onLikeClick,
                            interactionSource = likeInteractionSource,
                            modifier = Modifier.animateWidth(likeInteractionSource),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = if (isLiked) activeColor else inactiveColor
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(painterResource(id = R.drawable.icon_like_0), contentDescription = "Like")
                                Text(
                                    text = formatCount(videoInfo.stats?.like ?: 0),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                                )
                            }
                        }
                        
                        FilledIconButton(
                            onClick = onCoinClick,
                            interactionSource = coinInteractionSource,
                            modifier = Modifier.animateWidth(coinInteractionSource),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = if (isCoined) activeColor else inactiveColor
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(painterResource(id = R.drawable.icon_coin_0), contentDescription = "Coin")
                                Text(
                                    text = formatCount(videoInfo.stats?.coin ?: 0),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                                )
                            }
                        }
                        
                        FilledIconButton(
                            onClick = onFavClick,
                            interactionSource = favInteractionSource,
                            modifier = Modifier.animateWidth(favInteractionSource),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = if (isFav) activeColor else inactiveColor
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(painterResource(id = R.drawable.icon_fav_0), contentDescription = "Fav")
                                Text(
                                    text = formatCount(videoInfo.stats?.favorite ?: 0),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }

            }
        }
    }
}

@Composable
fun VideoCommentsPage(
    replies: List<Reply>, 
    replyCount: Int,
    focusRequester: FocusRequester,
    navController: NavHostController,
    onLoadMore: () -> Unit,
    onClick: (Reply) -> Unit,
    onLikeClick: (Reply) -> Unit,
    onReplyClick: (Reply) -> Unit,
    onSendCommentClick: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState, modifier = Modifier.focusRequester(focusRequester)) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader{
                    Text(
                        "评论(${formatCount(replyCount)})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Button(
                    onClick = onSendCommentClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("发布评论", style = MaterialTheme.typography.labelMedium)
                }
            }
            items(replies.size) { index ->
                if (index == replies.size - 1) {
                    LaunchedEffect(index) { onLoadMore() }
                }
                ReplyCard(
                    reply = replies[index],
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                    navController = navController,
                    onLikeClick = { onLikeClick(replies[index]) },
                    onClick = { onClick(replies[index]) },
                    onReplyClick = { onReplyClick(replies[index]) }
                )
            }
        }
    }
}

@Composable
fun ReplyCard(
    reply: Reply,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation,
    navController: NavHostController,
    onClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onReplyClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var linkClicked by remember { mutableStateOf(false) }
    var resolvedB23Links by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    LaunchedEffect(reply.message) {
        val b23Pattern = Regex("https?://b23\\.tv/\\S+", RegexOption.IGNORE_CASE)
        val b23Links = b23Pattern.findAll(reply.message).map { it.value }.toList()
        if (b23Links.isNotEmpty()) {
            val resolved = mutableMapOf<String, String>()
            for (link in b23Links) {
                withContext(Dispatchers.IO) {
                    LinkResolver.resolveB23Link(link)?.let { bv ->
                        resolved[link] = bv
                    }
                }
            }
            resolvedB23Links = resolved
        }
    }
    Card(
        onClick = { if (!linkClicked) onClick() else linkClicked = false },
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = reply.sender?.avatar,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.akari),
                    modifier = Modifier.size(24.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = reply.sender?.name ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val (richText, inlineContent) = parseRichText(reply.message, reply.emotes, resolvedB23Links)
            val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
            Text(
                text = richText,
                inlineContent = inlineContent,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val pos = textLayoutResult.value?.getOffsetForPosition(offset) ?: -1
                        if (pos >= 0) {
                            richText.getStringAnnotations(tag = "URL", start = pos, end = pos).firstOrNull()?.let { annotation ->
                                linkClicked = true
                                val url = annotation.item.removePrefix("url:")
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            richText.getStringAnnotations(tag = "VIDEO", start = pos, end = pos).firstOrNull()?.let { annotation ->
                                linkClicked = true
                                val videoId = annotation.item.removePrefix("video:")
                                val aid = if (videoId.startsWith("av", ignoreCase = true)) videoId.removePrefix("av").toLongOrNull() ?: 0L else 0L
                                val bvid = if (videoId.startsWith("bv", ignoreCase = true)) videoId else BilibiliIDConverter.aidToBv(aid)
                                if (aid > 0 || bvid.isNotEmpty()) {
                                    navController.navigate("detail/$bvid/$aid")
                                }
                            }
                        }
                    }
                },
                onTextLayout = { textLayoutResult.value = it }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                // Like Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick() }.padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icon_like_0),
                        contentDescription = "Like",
                        modifier = Modifier.size(14.dp),
                        tint = if (reply.liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${reply.likeCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reply.liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Reply Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onReplyClick() }.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Reply",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "回复(${reply.childCount})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun VideoRelatedPage(
    relatedVideos: List<VideoCard>, 
    focusRequester: FocusRequester,
    navController: NavHostController
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val context = LocalContext.current

    ScreenScaffold(scrollState = listState, modifier = Modifier.focusRequester(focusRequester)) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader {
                    Text(
                        "相关推荐",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            items(relatedVideos.size) { index ->
                RecommendVideoCard(
                    item = relatedVideos[index],
                    onClick = { 
                        navController.navigate("detail/${relatedVideos[index].bvid}/${relatedVideos[index].aid}")
                    },
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }
}

@Composable
fun parseRichText(
    text: String,
    emotes: Map<String, Emote>,
    resolvedB23Links: Map<String, String> = emptyMap()
): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val inlineContentMap = mutableMapOf<String, InlineTextContent>()
    
    var processedText = text
        .replace("<br>", "\n")
        .replace("<br/>", "\n")
        .replace("<br />", "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
    
    for ((b23Link, bv) in resolvedB23Links) {
        processedText = processedText.replace(b23Link, bv)
    }
    
    val urlPattern = "(https?://[^\\s<>()\\[\\]\"',;!?]+|www\\.[^\\s<>()\\[\\]\"',;!?]+)"
    val videoPattern = "(?i)(bv[A-Za-z0-9]+|av\\d+)"
    val fullPattern = Regex("($urlPattern|$videoPattern)")
    
    val annotatedString = buildAnnotatedString {
        if (emotes.isEmpty() && !processedText.contains(fullPattern)) {
            append(processedText)
            return@buildAnnotatedString
        }
        
        val parts = processedText.split(fullPattern)
        val matches = fullPattern.findAll(processedText).toList()
        
        for (i in parts.indices) {
            val part = parts[i]
            if (part.isNotEmpty()) {
                if (emotes.isNotEmpty()) {
                    val emotePattern = "\\[[^]]+]".toRegex()
                    var lastIdx = 0
                    for (emoteMatch in emotePattern.findAll(part)) {
                        val emoteKey = emoteMatch.value
                        val emote = emotes[emoteKey]
                        if (emote != null) {
                            append(part.substring(lastIdx, emoteMatch.range.first))
                            appendInlineContent(emoteKey, emoteKey)
                            if (!inlineContentMap.containsKey(emoteKey)) {
                                val sizeSp = (emote.size * 18).sp
                                inlineContentMap[emoteKey] = InlineTextContent(
                                    Placeholder(
                                        width = sizeSp,
                                        height = sizeSp,
                                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                    )
                                ) {
                                    AsyncImage(
                                        model = emote.url,
                                        contentDescription = emote.name,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            lastIdx = emoteMatch.range.last + 1
                        }
                    }
                    if (lastIdx < part.length) {
                        append(part.substring(lastIdx))
                    }
                } else {
                    append(part)
                }
            }
            
            if (i < matches.size) {
                val match = matches[i].value.trimEnd('.', ',', ';', ':', '!', '?')
                val isVideo = match.matches(Regex("(?i)(bv[A-Za-z0-9]+|av\\d+)"))
                val tag = if (isVideo) "VIDEO" else "URL"
                val annotation = if (isVideo) "video:$match" else "url:${if (match.startsWith("www.")) "https://$match" else match}"
                pushStringAnnotation(tag = tag, annotation = annotation)
                withStyle(SpanStyle(color = Color(0xFF4FC3F7), textDecoration = TextDecoration.Underline)) {
                    append(match)
                }
                pop()
            }
        }
    }
    return Pair(annotatedString, inlineContentMap)
}