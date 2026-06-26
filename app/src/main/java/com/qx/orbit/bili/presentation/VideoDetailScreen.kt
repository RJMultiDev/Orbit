package com.qx.orbit.bili.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.qx.orbit.bili.data.model.Emote
import com.qx.orbit.bili.data.model.PlayerData
import com.qx.orbit.bili.data.model.Reply
import com.qx.orbit.bili.data.model.VideoCard
import com.qx.orbit.bili.data.model.VideoInfo
import com.qx.orbit.bili.presentation.ui.components.RecommendVideoCard
import com.qx.orbit.bili.presentation.viewmodel.VideoDetailViewModel
import com.qx.orbit.bili.util.formatCount
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoDetailScreen(navController: NavHostController, bvid: String, aid: Long, viewModel: VideoDetailViewModel = viewModel()) {
    LaunchedEffect(bvid, aid) {
        viewModel.loadData(bvid, aid)
    }

    val videoInfo by viewModel.videoInfo.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val replies by viewModel.replies.collectAsState()
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
                                val info = videoInfo
                                if (info == null) return@VideoInfoPage
                                // Launch PlayerActivity
                                val playerData = PlayerData(
                                    title = info.title,
                                    aid = aid,
                                    cid = info.cids.firstOrNull() ?: 0L,
                                    cids = info.cids,
                                    pagenames = info.pagenames,
                                    type = if (info.epid > 0) PlayerData.TYPE_BANGUMI else PlayerData.TYPE_VIDEO
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
                            }
                        )
                        1 -> VideoCommentsPage(
                            replies = replies, 
                            focusRequester = focusRequesters[1],
                            onLoadMore = { viewModel.loadReplies() },
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
    
    Dialog(visible = showCoinDialog, onDismissRequest = { showCoinDialog = false }) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    "投币",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { viewModel.doCoin(1); showCoinDialog = false }
                            .padding(16.dp)
                    ) {
                        Text("1", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("币", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { viewModel.doCoin(2); showCoinDialog = false }
                            .padding(16.dp)
                    ) {
                        Text("2", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                        Text("币", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
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
    onFavClick: () -> Unit
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
                                .clickable(onClick = { /* TODO: Go to UP Space */ })
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
                                imageVector = Icons.Filled.List,
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
    focusRequester: FocusRequester,
    onLoadMore: () -> Unit,
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
                        "评论(${formatCount(replies.size)})",
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
                    onLikeClick = { onLikeClick(replies[index]) },
                    //onClick = { onClick(replies[index]) },
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
    onClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onReplyClick: () -> Unit = {}
) {
    Card(
        onClick = { onClick },
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
            val (richText, inlineContent) = parseRichText(reply.message, reply.emotes)
            Text(
                text = richText,
                inlineContent = inlineContent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
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
fun parseRichText(text: String, emotes: Map<String, Emote>): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val inlineContentMap = mutableMapOf<String, InlineTextContent>()
    val annotatedString = buildAnnotatedString {
        if (emotes.isEmpty()) {
            append(text)
            return@buildAnnotatedString
        }
        val pattern = "\\[[^\\]]+\\]".toRegex()
        var lastIndex = 0
        val matches = pattern.findAll(text)
        for (match in matches) {
            val emoteKey = match.value
            val emote = emotes[emoteKey]
            if (emote != null) {
                append(text.substring(lastIndex, match.range.first))
                val inlineId = emoteKey
                appendInlineContent(inlineId, emoteKey)
                if (!inlineContentMap.containsKey(inlineId)) {
                    val sizeSp = (emote.size * 18).sp
                    inlineContentMap[inlineId] = InlineTextContent(
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
                lastIndex = match.range.last + 1
            }
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
    return Pair(annotatedString, inlineContentMap)
}