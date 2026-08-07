package rj.ohos.wear.compose

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import rj.ohos.wear.compose.navigation.Route
import rj.ohos.wear.compose.navigation.rememberWearNavigator

/**
 * `rj.ohos.wear.compose` 迁移示例（**不注册 @Page**，仅作 API 形态演示）。
 *
 * 本函数演示**已落地**的 navigation 迁移（W6）：
 * - [rememberWearNavigator] 获取导航器；
 * - `navigator.navigateTo(route)` 无参跳转、`navigator.navigate(route, params)` 带参跳转、
 *   `navigator.popBackStack()` 返回上一页——内部等价于
 *   `RouterModule().openPage(route, null)` + `PageArgs.set(...)` + `RouterModule().closePage()`。
 *
 * Scaffold / TimeText / Chip 属于 material3（W3/W4，尚未落地），目标形态见
 * [README](README.md)「使用示例」与下方注释块。
 */
@Composable
fun WearUsageExample() {
    // 页面入口处拿到导航器（initialRoute = 本页 @Page 注册名）
    val navigator = rememberWearNavigator(Route.HOME)

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(
            text = "当前路由：${navigator.currentBackStackEntry ?: "-"}",
            fontSize = 12.sp,
        )
        Button(onClick = { navigator.navigateTo(Route.SEARCH) }) {
            Text(text = "无参跳转：搜索", fontSize = 11.sp)
        }
        Button(
            onClick = {
                navigator.navigate(
                    Route.VIDEO_DETAIL,
                    mapOf(Route.KEY_BVID to "BV1xx411c7mD", Route.KEY_CID to 2L),
                )
            },
        ) {
            Text(text = "带参跳转：视频详情", fontSize = 11.sp)
        }
        Button(onClick = { navigator.popBackStack() }) {
            Text(text = "返回上一页", fontSize = 11.sp)
        }
    }
}

/*
 * ---- 目标形态（待 W3/W4 落地后可用）----
 * 页面改用 rj.ohos.wear.compose 的 Scaffold / TimeText / Chip 后大致如下：
 *
 * @Composable
 * fun WearHomeScaffold(navigator: WearNavigator) {
 *     Scaffold(
 *         timeText = { TimeText() },
 *         modifier = Modifier.fillMaxSize(),
 *     ) { contentPadding ->
 *         Column(modifier = Modifier.padding(contentPadding)) {
 *             Chip(
 *                 label = { Text("打开搜索") },
 *                 onClick = { navigator.navigateTo(Route.SEARCH) },
 *             )
 *         }
 *     }
 * }
 */
