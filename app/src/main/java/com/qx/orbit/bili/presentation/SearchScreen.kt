package com.qx.orbit.bili.presentation
import com.qx.orbit.bili.presentation.component.WysTimeText

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import kotlin.math.roundToInt
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.compose.foundation.focusable
import androidx.wear.compose.material3.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.Text as Material3Text
import androidx.compose.material3.Text as Material3Text
import coil.compose.AsyncImage
import com.qx.orbit.bili.data.model.*
import com.qx.orbit.bili.presentation.ui.components.RecommendVideoCard
import com.qx.orbit.bili.presentation.util.parseHighlightedTitle
import com.qx.orbit.bili.presentation.viewmodel.SearchTab
import com.qx.orbit.bili.presentation.viewmodel.SearchViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun SearchInputScreen(navController: NavHostController) {
    var searchText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                searchText = spokenText
                val encodedQuery = URLEncoder.encode(searchText, StandardCharsets.UTF_8.toString())
                navController.navigate("search_result/$encodedQuery")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .focusRequester(focusRequester),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = {
                    Material3Text(
                        "输入搜索词",
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.background,
                    unfocusedBorderColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            FilledIconButton(
                onClick = {
                    if (searchText.isNotBlank()) {
                        val encodedQuery = URLEncoder.encode(searchText, StandardCharsets.UTF_8.toString())
                        navController.navigate("search_result/$encodedQuery")
                    }
                },
                modifier = Modifier.size(IconButtonDefaults.DefaultButtonSize),
                enabled = searchText.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Optional: Voice Search Button
        Button(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                }
                launcher.launch(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("语音搜索", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultScreen(viewModel: SearchViewModel, query: String, navController: NavHostController) {
    val currentTab by viewModel.currentTab.collectAsState()
    val resultsMap by viewModel.results.collectAsState()
    val isLoadingMap by viewModel.isLoading.collectAsState()
    val errorMessageMap by viewModel.errorMessage.collectAsState()
    var showTabMenu by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = SearchTab.entries.indexOf(currentTab),
        pageCount = { SearchTab.entries.size }
    )

    LaunchedEffect(query) {
        viewModel.performSearch(query)
    }

    LaunchedEffect(pagerState.currentPage) {
        val tab = SearchTab.entries[pagerState.currentPage]
        viewModel.switchTab(tab)
    }

    LaunchedEffect(currentTab) {
        val tabIndex = SearchTab.entries.indexOf(currentTab)
        if (pagerState.currentPage != tabIndex) {
            pagerState.animateScrollToPage(tabIndex)
        }
    }

    val focusRequesters = remember { List(4) { FocusRequester() } }
    LaunchedEffect(showTabMenu, currentTab) {
        if (!showTabMenu) {
            val tabIndex = SearchTab.entries.indexOf(currentTab)
            try { focusRequesters[tabIndex].requestFocus() } catch (e: Exception) {}
        }
    }

    var actualTitleHeightPx by remember { mutableFloatStateOf(0f) }
    var titleOffset by remember { mutableFloatStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (actualTitleHeightPx > 0f) {
                    titleOffset = (titleOffset + delta).coerceIn(-actualTitleHeightPx, 0f)
                }
                return Offset.Zero
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val isFocusedPage = pagerState.currentPage == page
            LaunchedEffect(isFocusedPage) {
                if (isFocusedPage) {
                    try { focusRequesters[page].requestFocus() } catch (e: Exception) {}
                }
            }
            
            val tab = SearchTab.entries[page]
            val results = resultsMap[tab] ?: emptyList()
            val isLoading = isLoadingMap[tab] ?: false
            val errorMessage = errorMessageMap[tab]

            val listState = rememberTransformingLazyColumnState()
            val transformationSpec = rememberTransformationSpec()
            ScreenScaffold(
                timeText = { WysTimeText() },
                scrollState = listState,
                modifier = Modifier.fillMaxSize().focusRequester(focusRequesters[page])
            ) { contentPadding ->
                TransformingLazyColumn(
                    state = listState,
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Spacer(modifier = Modifier.height(36.dp))
                    }

                    itemsIndexed(results) { index, item ->
                        if (index >= results.size - 3 && !isLoading) {
                            LaunchedEffect(index) {
                                viewModel.loadMore(tab)
                            }
                        }

                        // Reduced gap
                        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp).transformedHeight(this@itemsIndexed, transformationSpec)) {
                            when (item) {
                                is VideoCard -> RecommendVideoCard(
                                    item = item, 
                                    onClick = {
                                        navController.navigate("detail/${item.bvid}/${item.aid}")
                                    },
                                    transformation = SurfaceTransformation(transformationSpec)
                                )
                                is LiveRoom -> LiveRoomCard(
                                    item = item, 
                                    onClick = {
                                        // navController.navigate("live_room/${item.roomid}")
                                    },
                                    transformation = SurfaceTransformation(transformationSpec)
                                )
                                is UserInfo -> UserInfoCard(
                                    item = item, 
                                    onClick = {
                                        navController.navigate("user_space/${item.mid}")
                                    },
                                    transformation = SurfaceTransformation(transformationSpec)
                                )
                                is ArticleCard -> ArticleCardItem(
                                    item = item, 
                                    onClick = {
                                        // navController.navigate("article/${item.id}")
                                    },
                                    transformation = SurfaceTransformation(transformationSpec)
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    if (errorMessage != null && results.isEmpty()) {
                        item {
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                        }
                    }
                    
                    if (results.isEmpty() && !isLoading) {
                        item {
                            Text(text = "没有找到相关结果", color = Color.Gray, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
        
        // Floating Menu Button
        val localDensity = LocalDensity.current
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, titleOffset.roundToInt()) }
                .padding(top = 12.dp)
                .zIndex(2f)
                .onGloballyPositioned { coordinates ->
                    actualTitleHeightPx = coordinates.size.height.toFloat() + with(localDensity) { 12.dp.toPx() }
                }
        ) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { showTabMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentDisplayTab = SearchTab.entries.getOrNull(pagerState.currentPage) ?: currentTab
                    Text(
                        text = currentDisplayTab.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "切换",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Tab Menu Overlay
        AnimatedVisibility(
            visible = showTabMenu,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.fillMaxSize().zIndex(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                val menuListState = rememberTransformingLazyColumnState()
                val menuFocusRequester = remember { FocusRequester() }
                val menuTransformationSpec = rememberTransformationSpec()

                LaunchedEffect(showTabMenu) {
                    if (showTabMenu) {
                        try { menuFocusRequester.requestFocus() } catch (_: Exception) {}
                    }
                }

                ScreenScaffold(
                    timeText = { WysTimeText() },
                    scrollState = menuListState,
                    modifier = Modifier.focusRequester(menuFocusRequester)
                ) { contentPadding ->
                    TransformingLazyColumn(
                        state = menuListState,
                        contentPadding = contentPadding,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, menuTransformationSpec)
                                    .graphicsLayer {
                                        with(menuTransformationSpec) {
                                            applyContainerTransformation(scrollProgress)
                                        }
                                    }
                            ) {
                                ListHeader(
                                    modifier = Modifier.fillMaxWidth().clickable { showTabMenu = false }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Close Menu", tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(1.dp))
                                        Text(text = "收起", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        
                        items(SearchTab.entries.size) { index ->
                            val tab = SearchTab.entries[index]
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, menuTransformationSpec)
                                    .graphicsLayer {
                                        with(menuTransformationSpec) {
                                            applyContainerTransformation(scrollProgress)
                                        }
                                    }
                            ) {
                                val isSelected = currentTab == tab
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clip(CircleShape)
                                        .height(48.dp)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                                        .clickable {
                                            viewModel.switchTab(tab)
                                            showTabMenu = false
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = tab.title,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
fun LiveRoomCard(
    item: LiveRoom, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null
) {
    val coverUrl = item.user_cover.ifEmpty { item.cover }.ifEmpty { item.keyframe }
    val finalCover = if (coverUrl.contains("@")) coverUrl else "${coverUrl}@480w_270h_1c.webp"

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(110.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp),
        transformation = transformation
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(finalCover)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    val titleText = if (item.title.contains("<em")) {
                        parseHighlightedTitle(item.title)
                    } else {
                        AnnotatedString(item.title)
                    }
                    Text(
                        text = titleText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.uname,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🔴 ${item.online}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun UserInfoCard(
    item: UserInfo, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null
) {
    Button(
        onClick = onClick, 
        modifier = modifier.fillMaxWidth(), 
        transformation = transformation,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.primary,
            secondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        icon = {
            AsyncImage(
                model = item.avatar,
                contentDescription = item.name,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        },
        label = {
            Text(
                text = item.name,
                modifier = Modifier.basicMarquee(),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                fontWeight = FontWeight.Bold
            )
        },
        secondaryLabel = {
            Text(
                text = "粉丝: ${item.fans}",
                modifier = Modifier.basicMarquee(),
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    )
}

@Composable
fun ArticleCardItem(
    item: ArticleCard, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null
) {
    val coverUrl = if (item.cover.contains("@")) item.cover else "${item.cover}@480w_270h_1c.webp"

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(110.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp),
        transformation = transformation
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    val titleText = if (item.title.contains("<em")) {
                        parseHighlightedTitle(item.title)
                    } else {
                        AnnotatedString(item.title)
                    }
                    Text(
                        text = titleText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.upName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.view,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
