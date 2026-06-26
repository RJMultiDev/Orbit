package com.qx.orbit.bili.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import com.qx.orbit.bili.presentation.component.WysTimeText
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material3.ButtonDefaults
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.qx.orbit.bili.data.model.PlayerData
import com.qx.orbit.bili.data.model.Reply
import com.qx.orbit.bili.data.model.VideoCard
import com.qx.orbit.bili.data.remote.CookieManager
import com.qx.orbit.bili.presentation.player.PlayerScreen
import com.qx.orbit.bili.presentation.theme.OrbitTheme
import com.qx.orbit.bili.presentation.ui.components.RecommendVideoCard
import com.qx.orbit.bili.presentation.viewmodel.MainViewModel
import com.qx.orbit.bili.presentation.viewmodel.TabMode
import com.qx.orbit.bili.presentation.viewmodel.VideoDetailViewModel
import com.qx.orbit.bili.presentation.viewmodel.SearchViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.init(this)
        com.qx.orbit.bili.util.SharedPreferencesUtil.init(this)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp(viewModel: MainViewModel = viewModel()) {
    OrbitTheme {
        val navController = rememberSwipeDismissableNavController()
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route
        val isSwipeEnabled = currentRoute?.startsWith("player/") != true
        
        AppScaffold(timeText = { WysTimeText() }) {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "home",
                userSwipeEnabled = isSwipeEnabled,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
            composable("home") {
                HomeScreen(viewModel, navController)
            }
            composable(
                "detail/{bvid}/{aid}",
                arguments = listOf(
                    navArgument("bvid") { type = NavType.StringType },
                    navArgument("aid") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val bvid = backStackEntry.arguments?.getString("bvid") ?: ""
                val aid = backStackEntry.arguments?.getLong("aid") ?: 0L
                VideoDetailScreen(navController = navController, bvid = bvid, aid = aid)
            }
                composable("search") {
                    SearchInputScreen(navController = navController)
                }
                composable("search_result/{query}") { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("query") ?: ""
                    val searchViewModel: SearchViewModel = viewModel()
                    SearchResultScreen(viewModel = searchViewModel, query = query, navController = navController)
                }
                composable(
                    "reply_detail/{replyJson}",
                    arguments = listOf(
                        navArgument("replyJson") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val json = backStackEntry.arguments?.getString("replyJson") ?: ""
                    val decodedJson = URLDecoder.decode(json, StandardCharsets.UTF_8.toString())
                    val reply = Gson().fromJson(decodedJson, Reply::class.java)
                    val replyDetailViewModel: com.qx.orbit.bili.presentation.viewmodel.ReplyDetailViewModel = viewModel()
                    if (reply != null) {
                        ReplyDetailScreen(reply = reply, viewModel = replyDetailViewModel, navController = navController)
                    }
                }
                composable("login") {
                    LoginScreen(
                        navController = navController,
                        onLoginSuccess = {
                            viewModel.fetchNavInfo()
                            viewModel.loadMore(reset = true)
                        }
                    )
                }
            composable(
                "player/{playerDataJson}",
                arguments = listOf(
                    navArgument("playerDataJson") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val json = backStackEntry.arguments?.getString("playerDataJson") ?: ""
                val decodedJson = URLDecoder.decode(json, StandardCharsets.UTF_8.toString())
                val playerData = Gson().fromJson(decodedJson, PlayerData::class.java) ?: PlayerData(aid = 0L)
                PlayerScreen(initialData = playerData, onBack = { navController.popBackStack() })
            }
            composable(
                "user_space/{mid}",
                arguments = listOf(
                    navArgument("mid") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val mid = backStackEntry.arguments?.getLong("mid") ?: 0L
                val userSpaceViewModel: com.qx.orbit.bili.presentation.viewmodel.UserSpaceViewModel = viewModel()
                UserSpaceScreen(mid = mid, viewModel = userSpaceViewModel, navController = navController)
            }
            composable("settings_main") {
                com.qx.orbit.bili.presentation.settings.SettingsScreen(navController = navController)
            }
            composable("settings_terminal_player") {
                com.qx.orbit.bili.presentation.settings.SettingTerminalPlayerScreen(navController = navController)
            }
            composable("settings_ui") {
                com.qx.orbit.bili.presentation.settings.SettingUIScreen(navController = navController)
            }
        }
    }
    }
}

@Composable
fun HomeScreen(viewModel: MainViewModel, navController: NavHostController) {
    val currentTab by viewModel.currentTab.collectAsState()
    val videoList by viewModel.videoList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val navInfo by viewModel.navInfo.collectAsState()
    var showTabMenu by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(showTabMenu) {
        if (!showTabMenu) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus exceptions
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RecommendScreen(
                currentTab = currentTab,
                videoList = videoList,
                isLoading = isLoading,
                errorMessage = errorMessage,
                focusRequester = focusRequester,
                navController = navController,
                onLoadMore = { viewModel.loadMore() },
                onTabClick = { showTabMenu = true }
            )

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
                            if (navInfo == null || !navInfo!!.isLogin) {
                                viewModel.fetchNavInfo()
                            }
                            try { menuFocusRequester.requestFocus() } catch (_: Exception) {}
                        }
                    }

                    ScreenScaffold(
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
                                            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(1.dp))
                                            Text(text = "菜单", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
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
                                    if (navInfo != null && navInfo!!.isLogin) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(CircleShape)
                                                .height(48.dp)
                                                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                                                .clickable {
                                                    showTabMenu = false
                                                    navController.navigate("user_space/${navInfo!!.mid}")
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            AsyncImage(
                                                model = navInfo!!.face,
                                                contentDescription = "Avatar",
                                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(navInfo!!.uname ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                showTabMenu = false
                                                navController.navigate("login")
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "登录")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("登录")
                                        }
                                    }
                                }
                            }
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
                                    Button(
                                        onClick = {
                                            showTabMenu = false
                                            navController.navigate("search")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = "搜索")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("搜索")
                                    }
                                }
                            }
                            items(TabMode.entries.size) { index ->
                                val tab = TabMode.entries[index]
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
                                    Button(
                                        onClick = {
                                            viewModel.switchTab(tab)
                                            showTabMenu = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (tab == TabMode.RECOMMEND) {
                                            Icon(imageVector = Icons.Default.Star, contentDescription = tab.title)
                                        } else {
                                            Icon(imageVector = Icons.Default.ThumbUp, contentDescription = tab.title)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(tab.title)
                                    }
                                }
                            }
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
                                    Button(
                                        onClick = {
                                            showTabMenu = false
                                            navController.navigate("settings_main")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Settings, contentDescription = "设置")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("设置")
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
    }
}

@Composable
fun RecommendScreen(
    currentTab: TabMode,
    videoList: List<VideoCard>,
    isLoading: Boolean,
    errorMessage: String?,
    focusRequester: FocusRequester,
    navController: NavHostController,
    onLoadMore: () -> Unit,
    onTabClick: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    val titleHeight = 36.dp // Used to pad the list top so first item is visible
    
    // Dynamically measure actual title area height for complete hiding
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
    
    val layoutDirection = LocalLayoutDirection.current

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).nestedScroll(nestedScrollConnection)) {
        ScreenScaffold(
            scrollState = listState,
            modifier = Modifier.focusRequester(focusRequester)
        ) { contentPadding ->
            TransformingLazyColumn(
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + titleHeight,
                    bottom = contentPadding.calculateBottomPadding(),
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection)
                ),
                state = listState
            ) {
                items(videoList.size) { index ->
                    if (index == videoList.size - 1 && !isLoading) {
                        LaunchedEffect(index) {
                            onLoadMore()
                        }
                    }
                    
                    RecommendVideoCard(
                        item = videoList[index],
                        onClick = {
                            navController.navigate("detail/${videoList[index].bvid}/${videoList[index].aid}")
                        },
                        modifier = Modifier.transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (errorMessage != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clickable { onLoadMore() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "加载失败: $errorMessage\n点击重试", 
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center, 
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Floating Title Area
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, titleOffset.roundToInt()) }
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.background, Color.Transparent)
                    )
                )
                .onSizeChanged { size -> 
                    actualTitleHeightPx = size.height.toFloat() 
                }
                .padding(top = 18.dp, bottom = 12.dp)
                .clickable { onTabClick() },
            contentAlignment = Alignment.TopCenter
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(1.dp))
                Text(text = currentTab.title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    // Empty preview since ViewModel is injected
}
