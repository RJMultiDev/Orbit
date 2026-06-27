package com.qx.orbit.bili.presentation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.qx.orbit.bili.R
import com.qx.orbit.bili.data.model.Opus
import com.qx.orbit.bili.data.model.OpusParagraph
import com.qx.orbit.bili.presentation.theme.BiliPink
import com.qx.orbit.bili.presentation.viewmodel.OpusDetailViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch

@Composable
fun OpusDetailScreen(
    opusId: Long,
    navController: NavHostController,
    viewModel: OpusDetailViewModel = viewModel()
) {
    val opus by viewModel.opus.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val focusRequesters = remember { List(2) { FocusRequester() } }

    LaunchedEffect(opusId) {
        viewModel.loadOpus(opusId)
    }

    LaunchedEffect(pagerState.currentPage) {
        try {
            focusRequesters[pagerState.currentPage].requestFocus()
        } catch (e: Exception) {
            // Ignore focus exceptions when screen is not yet fully composed
        }
    }

    ScreenScaffold {
        if (opus == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val item = opus!!
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> OpusContentPage(item, viewModel, navController, focusRequesters[0])
                        1 -> OpusCommentsPage(viewModel, navController, focusRequesters[1])
                    }
                }
                HorizontalPageIndicator(
                    pagerState = pagerState,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun OpusContentPage(
    item: Opus,
    viewModel: OpusDetailViewModel,
    navController: NavHostController,
    focusRequester: FocusRequester
) {
    val context = LocalContext.current
    val listState = rememberTransformingLazyColumnState()
    val behavior = RotaryScrollableDefaults.behavior(listState)
    var showImageDialog by remember { mutableStateOf<String?>(null) }
    
    val likeInteractionSource = remember { MutableInteractionSource() }
    val favInteractionSource = remember { MutableInteractionSource() }

    if (showImageDialog != null) {
        ImageViewerDialog(
            imageUrl = showImageDialog!!,
            onDismiss = { showImageDialog = null }
        )
    }

    TransformingLazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .rotaryScrollable(behavior, focusRequester),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp)
    ) {
        if (item.title.isNotEmpty()) {
            item {
                androidx.compose.material3.Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        }
        
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        item.upInfo?.let {
                            navController.navigate("user_space/${it.mid}")
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val avatarUrl = item.upInfo?.avatar ?: ""
                val fixedAvatarUrl = when {
                    avatarUrl.startsWith("//") -> "https:$avatarUrl"
                    avatarUrl.startsWith("http://") -> avatarUrl.replaceFirst("http://", "https://")
                    else -> avatarUrl
                }
                AsyncImage(
                    model = ImageRequest.Builder(context).data(fixedAvatarUrl).crossfade(true).build(),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = item.upInfo?.name ?: "", style = MaterialTheme.typography.titleSmall)
                    Text(text = item.pubTime, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
        
        if (item.topImages.isNotEmpty()) {
            item {
                Column {
                    item.topImages.forEach { imgUrl ->
                        val fixedUrl = when {
                            imgUrl.startsWith("//") -> "https:$imgUrl"
                            imgUrl.startsWith("http://") -> imgUrl.replaceFirst("http://", "https://")
                            else -> imgUrl
                        }
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(fixedUrl).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showImageDialog = fixedUrl },
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }

        item.paragraphs?.forEach { paragraph ->
            item {
                when (paragraph.type) {
                    OpusParagraph.TYPE_TEXT, OpusParagraph.TYPE_HEADING -> {
                        val inlineContentMap = mutableMapOf<String, androidx.compose.foundation.text.InlineTextContent>()
                        val annotatedStr = buildAnnotatedString {
                            paragraph.textNodes.forEach { node ->
                                val weight = if (node.bold || paragraph.type == OpusParagraph.TYPE_HEADING) FontWeight.Bold else FontWeight.Normal
                                val fontStyle = if (node.italic) FontStyle.Italic else FontStyle.Normal
                                val textColor = if (node.color != null) {
                                    try { Color(android.graphics.Color.parseColor(node.color)) } catch (e: Exception) { Color.Unspecified }
                                } else Color.Unspecified
                                
                                val start = length
                                
                                if (node.emoteUrl != null) {
                                    appendInlineContent(node.text, node.text)
                                    if (!inlineContentMap.containsKey(node.text)) {
                                        val sizeSp = (node.emoteSize * 18).sp
                                        inlineContentMap[node.text] = InlineTextContent(
                                            Placeholder(
                                                width = sizeSp,
                                                height = sizeSp,
                                                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                            )
                                        ) {
                                            AsyncImage(
                                                model = node.emoteUrl,
                                                contentDescription = node.text,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                } else {
                                    withStyle(SpanStyle(fontWeight = weight, fontStyle = fontStyle, color = textColor)) {
                                        append(node.text)
                                    }
                                }
                                
                                if (node.jumpUrl != null) {
                                    addStringAnnotation("URL", node.jumpUrl, start, length)
                                }
                                if (node.memberId != null) {
                                    addStringAnnotation("USER", node.memberId.toString(), start, length)
                                }
                            }
                        }
                        
                        val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
                        androidx.compose.material3.Text(
                            text = annotatedStr,
                            inlineContent = inlineContentMap,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified),
                            modifier = Modifier.padding(vertical = 4.dp).pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val pos = textLayoutResult.value?.getOffsetForPosition(offset) ?: -1
                                    if (pos >= 0) {
                                        val urlAnn = annotatedStr.getStringAnnotations("URL", pos, pos).firstOrNull()
                                        if (urlAnn != null) {
                                            // TODO: handle url jump
                                        }
                                        val userAnn = annotatedStr.getStringAnnotations("USER", pos, pos).firstOrNull()
                                        if (userAnn != null) {
                                            navController.navigate("user_space/${userAnn.item}")
                                        }
                                    }
                                }
                            },
                            onTextLayout = { textLayoutResult.value = it }
                        )
                        Unit
                    }
                    OpusParagraph.TYPE_PIC -> {
                        Column {
                            paragraph.pics.forEach { imgUrl ->
                                val fixedUrl = when {
                                    imgUrl.startsWith("//") -> "https:$imgUrl"
                                    imgUrl.startsWith("http://") -> imgUrl.replaceFirst("http://", "https://")
                                    else -> imgUrl
                                }
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(fixedUrl).crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showImageDialog = fixedUrl },
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }
                    }
                    OpusParagraph.TYPE_DIVIDER -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.DarkGray))
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        item {
            ButtonGroup(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                FilledIconButton(
                    onClick = { viewModel.toggleLike() },
                    interactionSource = likeInteractionSource,
                    modifier = Modifier.animateWidth(likeInteractionSource),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = if (item.stats?.liked == true) BiliPink else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(R.drawable.icon_like_0), null)
                        Text(
                            text = com.qx.orbit.bili.util.formatCount(item.stats?.like ?: 0),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                        )
                    }
                }
                FilledIconButton(
                    onClick = { /* TODO */ },
                    interactionSource = favInteractionSource,
                    modifier = Modifier.animateWidth(favInteractionSource),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(R.drawable.icon_fav_0), null)
                        Text(
                            text = "收藏",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun OpusCommentsPage(
    viewModel: OpusDetailViewModel,
    navController: NavHostController,
    focusRequester: FocusRequester
) {
    val replies by viewModel.replies.collectAsState()
    val isReplyLoading by viewModel.isReplyLoading.collectAsState()
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val behavior = RotaryScrollableDefaults.behavior(listState)
    val coroutineScope = rememberCoroutineScope()

    TransformingLazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .rotaryScrollable(behavior, focusRequester),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp)
    ) {
        item {
            ListHeader {
                Text("评论区")
            }
        }
        item {
            Button(
                onClick = { /* TODO: Open send comment dialog */ },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text("发送评论", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        items(replies.size) { index ->
            if (index == replies.size - 3) {
                LaunchedEffect(index) { viewModel.loadReplies() }
            }
            ReplyCard(
                reply = replies[index],
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
                navController = navController,
                onLikeClick = { viewModel.likeReply(replies[index].rpid) }
            )
        }
        if (isReplyLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}
