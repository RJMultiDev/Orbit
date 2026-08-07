package rj.ohos.wear.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.ui.Modifier

/**
 * [WearNavHost] 对齐 `androidx.wear.compose.navigation.SwipeDismissableNavHost` 的
 * **路由声明表**壳。
 *
 * 重要：Kuikly 的页面注册由 `@Page("route")` + KSP 完成（每个页面是独立的
 * ComposeContainer，见 `com.qx.orbit.ohos.pages` 各 `*Page.kt`），真正的跨页面切换
 * 由 `RouterModule().openPage` 触发。因此本类**不负责实际页面渲染**，只提供声明式的
 * route 表（[WearNavHostScope.route]）并暴露当前 [WearNavigator] 供页面内导航。
 *
 * 典型用法：
 * ```
 * @Composable
 * fun HomeContent(navigator: WearNavigator) {
 *     WearNavHost(startDestination = Route.HOME) {
 *         route(Route.HOME) {
 *             // 首页子路由内容；navigator 即当前导航器
 *         }
 *         route(Route.SEARCH) {
 *             // 搜索子路由内容（当 navigator.currentBackStackEntry == "search" 时渲染）
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun WearNavHost(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: WearNavController = rememberWearNavController(startDestination),
    content: @Composable WearNavHostScope.() -> Unit,
) {
    val scope = remember(startDestination, navController) { WearNavHostScope(navController.navigator) }
    Box(modifier = modifier) {
        scope.content()
    }
}

/**
 * [WearNavHost] 的声明表作用域。
 *
 * 通过 `WearNavHost { ... }` 的 content 接收者获得；[navigator] 用于发起导航。
 */
class WearNavHostScope internal constructor(
    /** 当前导航器：页面内可直接 `navigator.navigateTo(...)` / `navigator.popBackStack()`。 */
    val navigator: WearNavigator,
) {

    /**
     * 声明一条路由。当 [navigator] 的名义当前路由等于 [route] 时渲染 [content]，
     * 对齐 `SwipeDismissableNavHost { composable(route) { ... } }` 的形态。
     *
     * 实际页面注册仍以 @Page + KSP 为准；本函数用于单容器内的子路由切换或作为
     * 路由清单文档。
     */
    @Composable
    fun route(route: String, content: @Composable WearNavHostScope.() -> Unit) {
        if (route == navigator.currentBackStackEntry) {
            content()
        }
    }
}
