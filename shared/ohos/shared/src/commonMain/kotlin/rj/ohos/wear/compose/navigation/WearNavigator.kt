package rj.ohos.wear.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.qx.orbit.ohos.pages.PageArgs
import com.tencent.kuikly.core.module.RouterModule

/**
 * [WearNavigator] 是 `androidx.navigation.NavController` / `androidx.wear.compose.navigation`
 * 的 Kuikly 迁移封装。
 *
 * Kuikly 的页面栈由宿主与 `@Page` + KSP 注册表管理，真正的跨页切换就是
 * `RouterModule().openPage(route, null)`（返回上一页是 `closePage()`）。因此：
 *
 * - [navigate] / [navigateTo] 转发 `openPage`，并把参数经 [PageArgs]（进程内共享 map）
 *   写入，与现有 `pages/AppRouter.kt` 的传参约定完全一致；
 * - [popBackStack] 转发 `closePage()`；
 * - [currentBackStackEntry] / [previousBackStackEntry] 是 **名义字段**（记录路由名），
 *   Kuikly 不暴露可订阅的回栈流，它们不是响应式的——跨容器切换后每个 @Page 容器
 *   各自 `rememberWearNavigator(自己的路由)` 即可获知"我是谁"。
 *
 * 以 data class 提供，便于 `copy(currentBackStackEntry = ...)` 在单个容器内表达
 * "当前子路由"（配合 [WearNavHost] 的 route 声明表使用）。
 */
data class WearNavigator(
    /** 当前路由名（@Page 注册名；名义值，非响应式）。 */
    var currentBackStackEntry: String? = null,
    /** 上一个路由名（名义值；[navigate] 时由当前值平移得到）。 */
    var previousBackStackEntry: String? = null,
) {

    /** 携带参数导航：先写 [PageArgs]，再 `RouterModule().openPage(route, null)`。 */
    fun navigate(route: String, params: Map<String, Any?> = emptyMap()) {
        previousBackStackEntry = currentBackStackEntry
        currentBackStackEntry = route
        params.forEach { (k, v) -> PageArgs.set(k, v) }
        RouterModule().openPage(route, null)
    }

    /** 无参导航：`RouterModule().openPage(route, null)`。 */
    fun navigateTo(route: String) {
        previousBackStackEntry = currentBackStackEntry
        currentBackStackEntry = route
        RouterModule().openPage(route, null)
    }

    /** 返回上一页：`RouterModule().closePage()`，并把名义回栈同步回退。 */
    fun popBackStack() {
        currentBackStackEntry = previousBackStackEntry
        previousBackStackEntry = null
        RouterModule().closePage()
    }
}

/**
 * 记住一个以 [initialRoute] 为当前路由的 [WearNavigator]。
 *
 * 页面级用法：`val navigator = rememberWearNavigator(Route.HOME)`，
 * 随后 `navigator.navigateTo(...)` / `navigator.popBackStack()`。
 */
@Composable
fun rememberWearNavigator(initialRoute: String): WearNavigator {
    return remember(initialRoute) { WearNavigator(currentBackStackEntry = initialRoute) }
}
